package com.example.dingding.service.impl;

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
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private static final Long ROOT_DEPT_ID = 1L; // 根部门ID

    @Autowired
    private DingdingConfig dingdingConfig;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
    public void syncOALSS() {
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
            userIds = Stream.of("2001051333950200").collect(Collectors.toSet());
            for(String formId :Constants.FORM_MAP.keySet()){
                //获取表单实例id
                for (Object userId : userIds) {
                    getFormInstantIds(formId, String.valueOf(userId));
                }
            }


        } catch (Exception e) {
            log.error("从缓存获取用户ID时发生异常", e);
        }
    }

    private void getFormInstantIds(String formId, String s) {
        com.aliyun.dingtalkworkflow_1_0.Client client = Sample.createClient();
        com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsHeaders listProcessInstanceIdsHeaders = new com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsHeaders();
        listProcessInstanceIdsHeaders.xAcsDingtalkAccessToken = "<your access token>";
        com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsRequest listProcessInstanceIdsRequest = new com.aliyun.dingtalkworkflow_1_0.models.ListProcessInstanceIdsRequest()
                .setStartTime(1762735507000L)
                .setEndTime(1763599507000L)
                .setProcessCode("PROC-FD7C69D9-67AA-4C09-8DB1-3D1A40FC8679")
                .setNextToken(0L)
                .setMaxResults(20L)
                .setUserIds(java.util.Arrays.asList(
                        "2001051333950200"
                ));
        try {
            client.listProcessInstanceIdsWithOptions(listProcessInstanceIdsRequest, listProcessInstanceIdsHeaders, new com.aliyun.teautil.models.RuntimeOptions());
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }

        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }

        }
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
                long expireTime = dingdingConfig.getToken().getCacheDuration() / 1000; // 转换为秒
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
}