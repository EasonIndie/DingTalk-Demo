package com.example.dingding.service.impl;

import com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceResponseBody;
import com.aliyun.tea.TeaException;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiV2DepartmentListsubRequest;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.request.OapiV2UserListRequest;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiV2UserListResponse;
import com.example.dingding.config.Constants;
import com.example.dingding.config.DingdingConfig;
import com.example.dingding.service.DingTalkOAService;
import com.example.dingding.dto.DepartmentDTO;
import com.example.dingding.entity.*;
import com.example.dingding.service.*;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 钉钉办公自动化服务实现类
 * 提供钉钉数据同步相关功能的具体实现
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class DingTalkOAServiceImpl implements DingTalkOAService {

    private static final String TOKEN_CACHE_KEY = "dingding:token:access_token";
    //private static final Long ROOT_DEPT_ID = 14479368L; // 根部门ID
    private static final Long ROOT_DEPT_ID = 1L; // 根部门ID

    @Autowired
    private DingdingConfig dingdingConfig;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IProcessInstanceService processInstanceService;

    @Autowired
    private IFormComponentValueService formComponentValueService;

    @Autowired
    private IOperationRecordService operationRecordService;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private ISyncRecordService syncRecordService;

    // 钉钉Workflow API客户端（懒加载）
    private volatile com.aliyun.dingtalkworkflow_1_0.Client workflowClient;

    /**
     * 时间范围内部类
     */
    private static class TimeRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            return "TimeRange{" +
                    "startTime=" + startTime +
                    ", endTime=" + endTime +
                    '}';
        }
    }

    @Override
    public int syncUserIds() {
        log.info("开始同步钉钉用户ID数据");

        try {
            // 1. 获取有效的access_token
            String accessToken = getValidAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                log.error("获取access_token失败，无法进行数据同步");
                return 0;
            }

            // 2. 递归获取所有部门ID并缓存
            Set<Long> allDeptIds = getAllDepartmentIds(accessToken);
            if (CollectionUtils.isEmpty(allDeptIds)) {
                log.error("获取部门ID列表为空");
                return 0;
            }

            cacheDepartmentIds(allDeptIds);
            log.info("成功获取并缓存了{}个部门ID", allDeptIds.size());

            // 3. 遍历所有部门获取用户ID并缓存
            Set<String> allUserIds = new HashSet<>();
            for (Long deptId : allDeptIds) {
                Set<String> userIds = getUserIdsByDepartment(deptId, accessToken);
                if (!CollectionUtils.isEmpty(userIds)) {
                    allUserIds.addAll(userIds);
                    log.debug("部门{}获取到{}个用户ID", deptId, userIds.size());
                }
            }

            // 4. 缓存所有用户ID
            cacheUserIds(allUserIds);

            log.info("同步完成，总共缓存了{}个用户ID", allUserIds.size());
            return allUserIds.size();

        } catch (Exception e) {
            log.error("同步用户ID数据时发生异常", e);
            return 0;
        }
    }

    @Override
    public void syncOALSS(LocalDateTime startTime) {
        try {
            //从缓存获取用户id
            log.info("开始从Redis缓存获取用户ID列表");
            Set<Object> userIds = redisTemplate.opsForSet().members(Constants.KEY_ALL_USER_IDS);
            if (CollectionUtils.isEmpty(userIds)) {
                log.info("缓存中没有找到用户ID，先调用syncUserIds()方法同步数据");
                syncUserIds();
            }else {
                log.info("从缓存中获取到{}个用户ID:", userIds.size());
                userIds = redisTemplate.opsForSet().members(Constants.KEY_ALL_USER_IDS);
            }
            //固定userIds 单个用户用来测试
            //userIds = Stream.of("2001051333950200").collect(Collectors.toSet());
            for(String formId :Constants.FORM_MAP.keySet()){
                //获取表单实例id
                for (Object userId : userIds) {
                    List<String> instanceIds = getFormInstantIds(formId, String.valueOf(userId), startTime);
                    if (!CollectionUtils.isEmpty(instanceIds)) {
                        log.info("表单ID: {}, 用户ID: {}, 从时间{}开始获取到{}个实例ID", formId, userId, startTime, instanceIds.size());
                        //通过实例id获取表单数据并保存
                        syncProcessInstanceDetails(instanceIds, formId);
                    } else {
                        log.info("表单ID: {}, 用户ID: {}, 从时间{}开始未获取到实例ID", formId, userId, startTime);
                    }
                }
            }


        } catch (Exception e) {
            log.error("从缓存获取用户ID时发生异常", e);
        }
    }

    /**
     * 拆分时间范围为不超过3个月的多个时间段
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段列表
     */
    private List<TimeRange> splitTimeRanges(LocalDateTime startTime, LocalDateTime endTime) {
        List<TimeRange> ranges = new ArrayList<>();
        LocalDateTime currentStart = startTime;

        while (currentStart.isBefore(endTime)) {
            LocalDateTime currentEnd = currentStart.plusMonths(3);
            if (currentEnd.isAfter(endTime)) {
                currentEnd = endTime;
            }

            ranges.add(new TimeRange(currentStart, currentEnd));
            currentStart = currentEnd;

            // 安全防护：最多拆分50个时间段，防止异常情况
            if (ranges.size() > Constants.DEFAULT_MAX_TIME_SPLITS) {
                log.warn("时间拆分超过{}个时间段，强制停止拆分", Constants.DEFAULT_MAX_TIME_SPLITS);
                break;
            }
        }

        log.info("时间范围拆分完成：{} -> {}，共拆分为{}个时间段",
                startTime, endTime, ranges.size());

        return ranges;
    }

    /**
     * 获取钉钉Workflow API客户端（懒加载，线程安全）
     * @return Workflow客户端实例
     */
    private com.aliyun.dingtalkworkflow_1_0.Client getWorkflowClient() {
        if (workflowClient == null) {
            synchronized (this) {
                if (workflowClient == null) {
                    try {
                        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
                        config.protocol = "https";
                        config.regionId = "central";
                        workflowClient = new com.aliyun.dingtalkworkflow_1_0.Client(config);
                        log.debug("成功初始化钉钉Workflow API客户端");
                    } catch (Exception e) {
                        log.error("初始化钉钉Workflow API客户端失败", e);
                        throw new RuntimeException("初始化钉钉Workflow API客户端失败", e);
                    }
                }
            }
        }
        return workflowClient;
    }

    /**
     * 获取指定时间范围内的表单实例ID（单个时间范围，不超过3个月）
     * @param formId 表单ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 实例ID列表
     */
    private List<String> getFormInstantIdsForRange(String formId, String userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            log.info("开始获取表单实例ID，表单ID: {}, 用户ID: {}, 查询时间: {}", formId, userId, startTime);

            // 1. 获取有效的access_token
            String accessToken = getValidAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                log.error("获取access_token失败，无法获取表单实例ID");
                return Collections.emptyList();
            }

            // 2. 获取Workflow客户端（单例，避免重复初始化）
            com.aliyun.dingtalkworkflow_1_0.Client client = getWorkflowClient();

            // 3. 构建请求头
            com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsHeaders headers =
                new com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsHeaders();
            headers.xAcsDingtalkAccessToken = accessToken;

            // 4. 根据传入的时间参数设置时间范围
            long endTimeMillis = endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long startTimeMillis = startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            log.info("查询时间范围: {} - {}",
                    new java.util.Date(startTimeMillis), new java.util.Date(endTimeMillis));

            // 5. 用于存储所有获取到的实例ID
            List<String> allInstanceIds = new ArrayList<>();
            Long nextToken = 0L; // 首次调用使用0
            int pageCount = 0; // 记录分页次数
            final Long maxResults = 20L; // 每页最大记录数

            // 6. 循环获取所有分页数据
            while (true) {
                pageCount++;
                log.debug("开始获取第{}页数据，nextToken: {}", pageCount, nextToken);

                // 构建请求体
                com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsRequest request =
                    new com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsRequest()
                        .setStartTime(startTimeMillis)
                        .setEndTime(endTimeMillis)
                        .setProcessCode(formId)  // 使用传入的表单ID
                        .setNextToken(nextToken)
                        .setMaxResults(maxResults)
                        .setUserIds(java.util.Arrays.asList(userId)); // 使用传入的用户ID

                // 调用API获取结果
                com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsResponse response =
                    client.listProcessInstanceIdsWithOptions(request, headers, new com.aliyun.teautil.models.RuntimeOptions());

                // 处理响应结果
                if (response != null && response.getBody() != null) {
                    com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsResponseBody body = response.getBody();

                    if (body != null && body.getResult() != null) {
                        // 解析result中的数据
                        com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsResponseBody.ListProcessInstanceIdsResponseBodyResult result = body.getResult();
                        if (result != null) {
                            // 获取当前页的实例ID列表
                            List<String> currentPageIds = result.getList();
                            if (currentPageIds != null && !currentPageIds.isEmpty()) {
                                allInstanceIds.addAll(currentPageIds);
                                log.info("第{}页获取到{}个实例ID，累计总数: {}",
                                        pageCount, currentPageIds.size(), allInstanceIds.size());
                            }
                            // 检查是否还有下一页
                            String nextToken1 = result.getNextToken();
                            if (nextToken1 == null) {
                                log.info("已获取到所有数据，共{}页，总计{}个实例ID",
                                        pageCount, allInstanceIds.size());
                                break; // 没有下一页，退出循环
                            }else {
                                nextToken = Long.valueOf(nextToken1);
                            }
                        } else {
                            log.warn("第{}页结果为空，结束查询", pageCount);
                            break;
                        }
                    } else {
                        log.warn("第{}页响应体中的result为空，结束查询", pageCount);
                        break;
                    }
                } else {
                    log.warn("第{}页响应为空，结束查询", pageCount);
                    break;
                }

                // 防止无限循环，设置最大分页数限制
                if (pageCount > 1000) {
                    log.warn("分页查询超过1000页，强制结束，当前总数: {}", allInstanceIds.size());
                    break;
                }
            }

            log.info("表单实例ID获取完成，表单ID: {}, 用户ID: {}, 总页数: {}, 总实例数: {}",
                    formId, userId, pageCount, allInstanceIds.size());
            log.debug("表单实例ID列表: {}", allInstanceIds);

            return allInstanceIds;

        } catch (TeaException err) {
            log.error("获取表单实例ID时发生TeaException，表单ID: {}, 用户ID: {}, 错误代码: {}, 错误信息: {}",
                    formId, userId, err.getCode(), err.getMessage());

            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                log.error("TeaException详细信息 - Code: {}, Message: {}", err.code, err.message);
            }

        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            log.error("获取表单实例ID时发生异常，表单ID: {}, 用户ID: {}", formId, userId, err);

            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                log.error("异常详细信息 - Code: {}, Message: {}", err.code, err.message);
            }
        }

        // 失败时返回空列表
        log.warn("获取表单实例ID失败，返回空列表，表单ID: {}, 用户ID: {}", formId, userId);
        return Collections.emptyList();
    }

    /**
     * 处理超过3个月时间范围的表单实例ID查询
     * @param formId 表单ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 实例ID列表（去重后）
     */
    private List<String> getFormInstantIdsWithSplit(String formId, String userId,
                                                  LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 拆分时间范围
            List<TimeRange> timeRanges = splitTimeRanges(startTime, endTime);
            Set<String> allInstanceIds = new HashSet<>();
            int totalSplitCount = timeRanges.size();

            log.info("开始处理{}个时间段的查询，表单ID: {}, 用户ID: {}", totalSplitCount, formId, userId);

            for (int i = 0; i < timeRanges.size(); i++) {
                TimeRange range = timeRanges.get(i);
                try {
                    log.info("处理第{}/{}个时间段: {}", i + 1, totalSplitCount, range);

                    // 调用单个时间范围的查询
                    List<String> rangeInstanceIds = getFormInstantIdsForRange(
                            formId, userId, range.getStartTime(), range.getEndTime());

                    if (!CollectionUtils.isEmpty(rangeInstanceIds)) {
                        allInstanceIds.addAll(rangeInstanceIds);
                        log.info("第{}个时间段获取到{}个实例ID，累计总数: {}",
                                i + 1, rangeInstanceIds.size(), allInstanceIds.size());
                    } else {
                        log.info("第{}个时间段未获取到实例ID", i + 1);
                    }

                    // 添加API调用间隔，避免触发频率限制
                    if (i < timeRanges.size() - 1) {
                        try {
                            Thread.sleep(dingdingConfig.getApi().getApiCallInterval());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("API调用间隔被中断", e);
                            break;
                        }
                    }

                } catch (Exception e) {
                    log.error("处理时间段{}时发生异常: {}", range, e.getMessage(), e);
                    // 继续处理其他时间段
                }
            }

            List<String> result = new ArrayList<>(allInstanceIds);
            log.info("多时间段查询完成，表单ID: {}, 用户ID: {}, 总时间段数: {}, 最终实例数: {}",
                    formId, userId, totalSplitCount, result.size());

            return result;

        } catch (Exception e) {
            log.error("处理多时间段查询时发生异常，表单ID: {}, 用户ID: {}", formId, userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取表单实例ID的主方法，支持时间拆分和结果合并
     * @param formId 表单ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间（可选，不传时默认为当前时间）
     * @return 实例ID列表
     */
    private List<String> getFormInstantIds(String formId, String userId,
                                         LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 参数验证
            if (startTime == null) {
                throw new IllegalArgumentException("startTime不能为空");
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
                log.debug("endTime为空，使用当前时间作为默认值: {}", endTime);
            }
            if (startTime.isAfter(endTime)) {
                throw new IllegalArgumentException("startTime不能晚于endTime");
            }

            log.info("开始查询表单实例ID，表单ID: {}, 用户ID: {}, 时间范围: {} - {}",
                    formId, userId, startTime, endTime);

            // 检查时间间隔是否超过3个月
            LocalDateTime threeMonthsLater = startTime.plusMonths(3);
            if (threeMonthsLater.isAfter(endTime) || threeMonthsLater.isEqual(endTime)) {
                // 不超过3个月，直接调用原逻辑
                log.debug("时间间隔不超过3个月，直接查询");
                return getFormInstantIdsForRange(formId, userId, startTime, endTime);
            } else {
                // 超过3个月，拆分时间范围
                log.info("时间间隔超过3个月，将拆分为多个时间段查询");
                return getFormInstantIdsWithSplit(formId, userId, startTime, endTime);
            }

        } catch (Exception e) {
            log.error("获取表单实例ID失败，表单ID: {}, 用户ID: {}", formId, userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取表单实例ID（向后兼容方法，endTime默认为当前时间）
     * @param formId 表单ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @return 实例ID列表
     */
    private List<String> getFormInstantIds(String formId, String userId, LocalDateTime startTime) {
        return getFormInstantIds(formId, userId, startTime, LocalDateTime.now());
    }

    /**
     * 获取有效的access_token
     * 优先从Redis缓存获取，如果不存在或过期则重新获取
     *
     * @return access_token
     */
    private String getValidAccessToken() {
        try {
            // 先从Redis缓存获取
            String cachedToken = (String) redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
            if (StringUtils.hasText(cachedToken)) {
                log.debug("从Redis缓存获取到access_token");
                return cachedToken;
            }

            // 缓存不存在，重新获取
            log.info("缓存中无access_token，开始重新获取");
            return refreshTokenFromDingTalk();

        } catch (Exception e) {
            log.error("获取access_token时发生异常", e);
            return null;
        }
    }

    /**
     * 从钉钉服务器重新获取access_token
     *
     * @return 新的access_token
     */
    private String refreshTokenFromDingTalk() {
        try {
            String url = dingdingConfig.getApi().getBaseUrl() + dingdingConfig.getApi().getTokenUrl();
            DingTalkClient client = new DefaultDingTalkClient(url);
            OapiGettokenRequest request = new OapiGettokenRequest();
            request.setAppkey(dingdingConfig.getApp().getAppKey());
            request.setAppsecret(dingdingConfig.getApp().getAppSecret());

            OapiGettokenResponse response = client.execute(request);

            if (response.isSuccess() && response.getAccessToken() != null) {
                String accessToken = response.getAccessToken();

                // 缓存到Redis，设置过期时间
                long expireTime = dingdingConfig.getToken().getCacheDuration(); // 转换为秒
                redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, accessToken, expireTime, TimeUnit.SECONDS);

                log.info("成功获取并缓存access_token，过期时间{}秒", expireTime);
                return accessToken;
            } else {
                log.error("获取access_token失败：{}", response.getErrmsg());
                return null;
            }

        } catch (ApiException e) {
            log.error("调用钉钉获取access_token API失败", e);
            return null;
        }
    }

    /**
     * 递归获取所有部门ID
     *
     * @param accessToken 访问令牌
     * @return 所有部门ID集合
     */
    private Set<Long> getAllDepartmentIds(String accessToken) {
        Set<Long> allDeptIds = new HashSet<>();
        Set<Long> processedDeptIds = new HashSet<>();

        try {
            // 从根部门开始递归
            recursionGetDepartmentIds(ROOT_DEPT_ID, accessToken, allDeptIds, processedDeptIds);
        } catch (Exception e) {
            log.error("递归获取部门ID时发生异常", e);
        }

        return allDeptIds;
    }

    /**
     * 递归获取部门ID的核心方法
     *
     * @param deptId 当前部门ID
     * @param accessToken 访问令牌
     * @param allDeptIds 所有部门ID集合
     * @param processedDeptIds 已处理的部门ID集合（防止重复处理）
     */
    private void recursionGetDepartmentIds(Long deptId, String accessToken, Set<Long> allDeptIds, Set<Long> processedDeptIds) {
        if (deptId == null || processedDeptIds.contains(deptId)) {
            return;
        }

        processedDeptIds.add(deptId);
        allDeptIds.add(deptId);

        try {
            String url = dingdingConfig.getApi().getBaseUrl() + dingdingConfig.getApi().getDepartmentListUrl();
            DingTalkClient client = new DefaultDingTalkClient(url);
            OapiV2DepartmentListsubRequest request = new OapiV2DepartmentListsubRequest();
            request.setDeptId(deptId);

            OapiV2DepartmentListsubResponse response = client.execute(request, accessToken);

            if (response.isSuccess() && response.getResult() != null) {
                List<OapiV2DepartmentListsubResponse.DeptBaseResponse> departments = response.getResult();
                if (!CollectionUtils.isEmpty(departments)) {
                    for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : departments) {
                        if (dept != null && dept.getDeptId() != null) {
                            // 递归处理子部门
                            recursionGetDepartmentIds(dept.getDeptId(), accessToken, allDeptIds, processedDeptIds);
                        }
                    }
                }
            } else {
                log.warn("获取部门{}的子部门列表失败：{}", deptId, response.getErrmsg());
            }

        } catch (ApiException e) {
            log.error("调用钉钉获取部门列表API失败，deptId: {}", deptId, e);
        }
    }

    /**
     * 获取指定部门的用户ID列表
     *
     * @param deptId 部门ID
     * @param accessToken 访问令牌
     * @return 用户ID集合
     */
    private Set<String> getUserIdsByDepartment(Long deptId, String accessToken) {
        Set<String> userIds = new HashSet<>();
        Long cursor = 0L;
        Long size = 50L; // 每页大小

        try {
            while (true) {
                String url = dingdingConfig.getApi().getBaseUrl() + dingdingConfig.getApi().getUserListUrl();
                DingTalkClient client = new DefaultDingTalkClient(url);
                OapiV2UserListRequest request = new OapiV2UserListRequest();
                request.setDeptId(deptId);
                request.setCursor(cursor);
                request.setSize(size);

                OapiV2UserListResponse response = client.execute(request, accessToken);

                if (response.isSuccess() && response.getResult() != null) {
                    OapiV2UserListResponse.PageResult pageResult = response.getResult();

                    List<OapiV2UserListResponse.ListUserResponse> users = pageResult.getList();
                    if (!CollectionUtils.isEmpty(users)) {
                        for (OapiV2UserListResponse.ListUserResponse user : users) {
                            if (user != null && StringUtils.hasText(user.getUserid())) {
                                userIds.add(user.getUserid());
                            }
                        }
                    }

                    // 检查是否还有下一页
                    if (pageResult.getHasMore() == null || !pageResult.getHasMore()) {
                        break;
                    }

                    cursor = pageResult.getNextCursor();
                } else {
                    log.warn("获取部门{}的用户列表失败：{}", deptId, response.getErrmsg());
                    break;
                }
            }

        } catch (ApiException e) {
            log.error("调用钉钉获取用户列表API失败，deptId: {}", deptId, e);
        }

        return userIds;
    }

    /**
     * 缓存部门ID到Redis
     *
     * @param deptIds 部门ID集合
     */
    private void cacheDepartmentIds(Set<Long> deptIds) {
        try {
            if (!CollectionUtils.isEmpty(deptIds)) {
                // 清空现有缓存
                redisTemplate.delete(Constants.KEY_ALL_DEPARTMENT_IDS);
                // 缓存新的部门ID集合
                redisTemplate.opsForSet().add(Constants.KEY_ALL_DEPARTMENT_IDS, deptIds.toArray());
                // 设置过期时间
                redisTemplate.expire(Constants.KEY_ALL_DEPARTMENT_IDS, Constants.CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
                log.debug("成功缓存{}个部门ID到Redis", deptIds.size());
            }
        } catch (Exception e) {
            log.error("缓存部门ID到Redis时发生异常", e);
        }
    }

    /**
     * 缓存用户ID到Redis
     *
     * @param userIds 用户ID集合
     */
    private void cacheUserIds(Set<String> userIds) {
        try {
            if (!CollectionUtils.isEmpty(userIds)) {
                // 清空现有缓存
                redisTemplate.delete(Constants.KEY_ALL_USER_IDS);
                // 缓存新的用户ID集合
                redisTemplate.opsForSet().add(Constants.KEY_ALL_USER_IDS, userIds.toArray());
                // 设置过期时间
                redisTemplate.expire(Constants.KEY_ALL_USER_IDS, Constants.CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
                log.debug("成功缓存{}个用户ID到Redis", userIds.size());
            }
        } catch (Exception e) {
            log.error("缓存用户ID到Redis时发生异常", e);
        }
    }

    /**
     * 同步流程实例详情
     *
     * @param instanceIds 实例ID列表
     * @param formId 表单ID（用作processCode）
     */
    private void syncProcessInstanceDetails(List<String> instanceIds, String formId) {
        if (CollectionUtils.isEmpty(instanceIds)) {
            return;
        }

        // 记录同步开始
        SyncRecord syncRecord = syncRecordService.startSync("PROCESS_DETAILS", instanceIds.size());
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        try {
            for (String instanceId : instanceIds) {
                try {
                    // 检查是否已存在
                    ProcessInstance existingInstance = processInstanceService.getByProcessInstanceId(instanceId);
                    if (existingInstance != null) {
                        log.debug("流程实例已存在，跳过: {}", instanceId);
                        successCount++;
                        continue;
                    }

                    // 获取流程实例详情
                    ProcessInstanceDetails details = getProcessInstanceDetails(instanceId, formId);
                    if (details != null) {
                        // 保存数据
                        saveProcessInstanceDetails(details);
                        successCount++;
                        log.debug("成功保存流程实例详情: {}", instanceId);
                    } else {
                        failCount++;
                        errorMessages.append("获取流程实例详情失败: ").append(instanceId).append("; ");
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("处理流程实例失败: ").append(instanceId).append(", 错误: ").append(e.getMessage()).append("; ");
                    log.error("处理流程实例失败: {}", instanceId, e);
                }
            }

            // 记录同步完成
            if (failCount == 0) {
                syncRecordService.completeSync(syncRecord, successCount);
            } else {
                syncRecordService.failSync(syncRecord, successCount, errorMessages.toString());
            }

            log.info("同步流程实例详情完成，总数: {}, 成功: {}, 失败: {}", instanceIds.size(), successCount, failCount);

        } catch (Exception e) {
            syncRecordService.failSync(syncRecord, successCount, "同步过程中发生异常: " + e.getMessage());
            log.error("同步流程实例详情时发生异常", e);
        }
    }

    /**
     * 获取流程实例详情
     *
     * @param instanceId 实例ID
     * @param formId 表单ID（用作processCode）
     * @return 流程实例详情
     */
    private ProcessInstanceDetails getProcessInstanceDetails(String instanceId, String formId) {
        try {
            String accessToken = getValidAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                log.error("获取access_token失败，无法获取流程实例详情");
                return null;
            }

            com.aliyun.dingtalkworkflow_1_0.Client client = getWorkflowClient();

            // 构建请求头
            com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceHeaders headers =
                new com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceHeaders();
            headers.xAcsDingtalkAccessToken = accessToken;

            // 构建请求体
            com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceRequest request =
                new com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceRequest()
                    .setProcessInstanceId(instanceId);

            // 调用API
            com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceResponse response =
                client.getProcessInstanceWithOptions(request, headers, new com.aliyun.teautil.models.RuntimeOptions());

            if (response != null && response.getBody() != null && response.getBody().getResult() != null) {
                return parseProcessInstanceDetails(response.getBody().getResult(), instanceId, formId);
            }

        } catch (Exception e) {
            log.error("获取流程实例详情失败: {}", instanceId, e);
        }

        return null;
    }

    /**
     * 解析流程实例详情
     */
    private ProcessInstanceDetails parseProcessInstanceDetails(GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult result, String instanceId, String formId) {
        ProcessInstanceDetails details = new ProcessInstanceDetails();

        try {
            // 解析主流程实例信息
            ProcessInstance processInstance = new ProcessInstance();

            // 设置流程实例ID（必需字段）
            processInstance.setProcessInstanceId(instanceId);

            // 设置流程模板ID - 使用传入的formId作为processCode
            processInstance.setProcessCode(formId);

            // 根据钉钉SDK实体类获取其他字段
            processInstance.setTitle(result.getTitle());
            processInstance.setBusinessId(result.getBusinessId());
            processInstance.setOriginatorUserid(result.getOriginatorUserId());
            processInstance.setOriginatorDeptId(result.getOriginatorDeptId());
            processInstance.setOriginatorDeptName(result.getOriginatorDeptName());
            processInstance.setStatus(result.getStatus());
            processInstance.setResult(result.getResult());
            processInstance.setCreateTime(parseDateTime(result.getCreateTime()));
            processInstance.setFinishTime(parseDateTime(result.getFinishTime()));
            processInstance.setBizAction(result.getBizAction());
            processInstance.setBizData(result.getBizData());

            // 解析附属实例和抄送用户
            if (result.getAttachedProcessInstanceIds() != null) {
                processInstance.setAttachedProcessInstanceIds(result.getAttachedProcessInstanceIds());
            }
            if (result.getCcUserIds() != null) {
                processInstance.setCcUserids(result.getCcUserIds());
            }

            details.setProcessInstance(processInstance);

            // 解析表单组件值
            if (result.getFormComponentValues() != null && !result.getFormComponentValues().isEmpty()) {
                List<FormComponentValue> formComponentValues = new ArrayList<>();

                for (com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultFormComponentValues component : result.getFormComponentValues()) {
                    FormComponentValue fcv = new FormComponentValue();

                    // 从请求参数中获取processInstanceId
                    fcv.setProcessInstanceId(instanceId);
                    fcv.setComponentId(component.getId());
                    fcv.setComponentName(component.getName());
                    fcv.setComponentType(component.getComponentType());
                    fcv.setValue(component.getValue());
                    fcv.setExtValue(component.getExtValue());
                    fcv.setBizAlias(component.getBizAlias());

                    // 设置字段分类
                    fcv.setFieldCategory(determineFieldCategory(component.getName()));
                    fcv.setIsKeyField(isKeyField(component.getName()));

                    formComponentValues.add(fcv);
                }
                details.setFormComponentValues(formComponentValues);
            }

            // 解析操作记录
            if (result.getOperationRecords() != null && !result.getOperationRecords().isEmpty()) {
                List<OperationRecord> operationRecords = new ArrayList<>();

                for (com.aliyun.dingtalkworkflow_1_0.models.GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultOperationRecords record : result.getOperationRecords()) {
                    OperationRecord or = new OperationRecord();

                    or.setProcessInstanceId(instanceId);
                    or.setActivityId(record.getActivityId());
                    or.setOperationDate(parseDateTime(record.getDate()));
                    or.setUserId(record.getUserId());
                    or.setShowName(record.getShowName());
                    or.setOperationType(record.getType());
                    or.setResult(record.getResult());
                    or.setRemark(record.getRemark());

                    // 解析图片附件
                    if (record.getImages() != null && !record.getImages().isEmpty()) {
                        // images字段可能是String列表或其他格式，根据实际SDK返回调整
                        try {
                            or.setImages(new ArrayList<>(record.getImages()));
                        } catch (Exception e) {
                            // 如果解析失败，使用空列表
                            or.setImages(new ArrayList<>());
                        }
                    }

                    operationRecords.add(or);
                }
                details.setOperationRecords(operationRecords);
            }

            // tasks不需要解析保存，根据要求跳过
            details.setTasks(new ArrayList<>());

        } catch (Exception e) {
            log.error("解析流程实例详情失败", e);
            // 返回空的details对象，避免null pointer
            details.setProcessInstance(new ProcessInstance());
            details.setFormComponentValues(new ArrayList<>());
            details.setOperationRecords(new ArrayList<>());
            details.setTasks(new ArrayList<>());
        }

        return details;
    }

    /**
     * 保存流程实例详情
     */
    private void saveProcessInstanceDetails(ProcessInstanceDetails details) {
        // 保存主流程实例
        processInstanceService.saveOrUpdate(details.getProcessInstance());

        // 批量保存表单组件值
        if (details.getFormComponentValues() != null && !details.getFormComponentValues().isEmpty()) {
            formComponentValueService.saveBatch(details.getFormComponentValues());
        }

        // 批量保存操作记录
        if (details.getOperationRecords() != null && !details.getOperationRecords().isEmpty()) {
            operationRecordService.saveBatch(details.getOperationRecords());
        }

        // 根据要求，tasks不需要解析和保存
        log.debug("跳过tasks保存，按业务要求不需要存储");
    }

    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (!StringUtils.hasText(dateTimeStr)) {
            return null;
        }
        try {
            // 处理钉钉返回的时间格式 (2025-11-19T11:11Z)
            return LocalDateTime.parse(dateTimeStr.replace("Z", ""));
        } catch (Exception e) {
            log.warn("解析日期时间失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 确定字段分类
     */
    private String determineFieldCategory(String componentName) {
        if (componentName == null) return "其他";

        if (componentName.contains("姓名") || componentName.contains("部门") || componentName.contains("申报日期")) {
            return "基础信息";
        } else if (componentName.contains("改善") || componentName.contains("建议") || componentName.contains("提案")) {
            return "项目详情";
        } else if (componentName.contains("完成") || componentName.contains("实施")) {
            return "完成情况";
        } else {
            return "其他";
        }
    }

    /**
     * 判断是否关键字段
     */
    private Boolean isKeyField(String componentName) {
        if (componentName == null) return false;

        return componentName.contains("等级") ||
               componentName.contains("经济效益") ||
               componentName.contains("类别") ||
               componentName.contains("改善");
    }

    /**
     * 流程实例详情内部类
     */
    private static class ProcessInstanceDetails {
        private ProcessInstance processInstance;
        private List<FormComponentValue> formComponentValues;
        private List<OperationRecord> operationRecords;
        private List<Task> tasks;

        // getters and setters
        public ProcessInstance getProcessInstance() { return processInstance; }
        public void setProcessInstance(ProcessInstance processInstance) { this.processInstance = processInstance; }
        public List<FormComponentValue> getFormComponentValues() { return formComponentValues; }
        public void setFormComponentValues(List<FormComponentValue> formComponentValues) { this.formComponentValues = formComponentValues; }
        public List<OperationRecord> getOperationRecords() { return operationRecords; }
        public void setOperationRecords(List<OperationRecord> operationRecords) { this.operationRecords = operationRecords; }
        public List<Task> getTasks() { return tasks; }
        public void setTasks(List<Task> tasks) { this.tasks = tasks; }
    }

    @Override
    public List<DepartmentDTO> getAllDepartmentsWithDetails() {
        log.info("开始获取钉钉所有部门的详细信息");

        try {
            // 1. 获取有效的access_token
            String accessToken = getValidAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                log.error("获取access_token失败，无法获取部门信息");
                return Collections.emptyList();
            }

            // 2. 递归获取所有部门信息
            List<DepartmentDTO> allDepartments = new ArrayList<>();
            recursionGetDepartmentsWithDetails(ROOT_DEPT_ID, accessToken, allDepartments, new HashSet<>());

            log.info("成功获取{}个部门的详细信息", allDepartments.size());
            return allDepartments;

        } catch (Exception e) {
            log.error("获取钉钉部门详细信息时发生异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 递归获取部门详细信息的核心方法
     * 改进版：直接利用获取子部门API返回的信息
     *
     * @param deptId 当前部门ID
     * @param accessToken 访问令牌
     * @param allDepartments 所有部门信息集合
     * @param processedDeptIds 已处理的部门ID集合（防止重复处理）
     */
    private void recursionGetDepartmentsWithDetails(Long deptId, String accessToken,
                                                   List<DepartmentDTO> allDepartments,
                                                   Set<Long> processedDeptIds) {
        if (deptId == null || processedDeptIds.contains(deptId)) {
            return;
        }

        processedDeptIds.add(deptId);

        try {
            // 根部门特殊处理：钉钉API中根部门(1)不返回详情，需要特殊处理
            if (ROOT_DEPT_ID.equals(deptId)) {
                DepartmentDTO rootDept = new DepartmentDTO()
                        .setDeptId(ROOT_DEPT_ID)
                        .setParentId(null)
                        .setName("根部门");
                allDepartments.add(rootDept);
                log.debug("添加根部门: ID={}, Name={}", ROOT_DEPT_ID, "根部门");
            }

            // 获取子部门列表，这里会返回子部门的详细信息
            String url = dingdingConfig.getApi().getBaseUrl() + dingdingConfig.getApi().getDepartmentListUrl();
            DingTalkClient client = new DefaultDingTalkClient(url);
            OapiV2DepartmentListsubRequest request = new OapiV2DepartmentListsubRequest();
            request.setDeptId(deptId);

            OapiV2DepartmentListsubResponse response = client.execute(request, accessToken);

            if (response.isSuccess() && response.getResult() != null) {
                List<OapiV2DepartmentListsubResponse.DeptBaseResponse> departments = response.getResult();
                if (!CollectionUtils.isEmpty(departments)) {
                    for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : departments) {
                        if (dept != null && dept.getDeptId() != null) {
                            // 转换为DTO并添加到列表
                            DepartmentDTO deptDto = DepartmentDTO.fromDingTalkResponse(dept);
                            if (deptDto != null) {
                                allDepartments.add(deptDto);
                                log.debug("添加部门: ID={}, Name={}, ParentId={}",
                                         deptDto.getDeptId(), deptDto.getName(), deptDto.getParentId());
                            }

                            // 递归处理子部门
                            recursionGetDepartmentsWithDetails(dept.getDeptId(), accessToken, allDepartments, processedDeptIds);
                        }
                    }
                }
            } else {
                log.warn("获取部门{}的子部门列表失败：{}", deptId, response.getErrmsg());
            }

        } catch (ApiException e) {
            log.error("调用钉钉获取部门详情API失败，deptId: {}", deptId, e);
        }
    }
}