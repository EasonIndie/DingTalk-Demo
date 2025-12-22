package com.example.dingding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 钉钉应用配置类
 * 支持多应用配置，每个应用有独立的配置和权限
 *
 * @author system
 * @version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "dingding")
@Data
public class DingdingConfig {

    /**
     * 应用基础配置
     */
    private App app;

    /**
     * API配置
     */
    private Api api;

    /**
     * Token管理配置
     */
    private Token token;

    @Data
    public static class App {
        /**
         * 应用名称（用于标识不同应用）
         */
        private String name = "HIS云平台";

        /**
         * 钉钉应用的AppKey
         */
        private String appKey;

        /**
         * 钉钉应用的AppSecret
         */
        private String appSecret;

        /**
         * 企业ID
         */
        private String corpId;

        /**
         * 应用类型（内部应用/第三方应用）
         */
        private String appType = "INTERNAL";


    }
    @Data
    public static class Api {
        /**
         * 钉钉API基础地址
         */
        private String baseUrl = "https://oapi.dingtalk.com";

        /**
         * 获取access_token的API
         */
        private String tokenUrl = "/gettoken";

        /**
         * 获取部门列表的API
         */
        private String departmentListUrl = "/topapi/v2/department/listsub";

        /**
         * 获取部门下用户id
         */
        private String listUserid = "/topapi/user/listid";

        /**
         * 获取部门下用户详情
         */
        private String departmentGet = "/topapi/v2/department/get";

        /**
         * 获取部门用户列表
         */
        private String userListUrl = "/topapi/v2/user/list";

        /**
         * 获取用户详细信息API
         */
        private String userDetailUrl = "/topapi/v2/user/getbyuserid";

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 10000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 15000;

        /**
         * API查询时间跨度限制（天）
         */
        private int maxTimeRangeDays = 90;

        /**
         * API调用间隔时间（毫秒）
         */
        private long apiCallInterval = 20;

        /**
         * 最大时间拆分数量
         */
        private int maxTimeSplits = 50;

    }
    @Data
    public static class Token {
        /**
         * access_token缓存时间（毫秒），钉钉token有效期7200秒
         */
        private long cacheDuration = 7200;

        /**
         * token获取失败重试次数
         */
        private int retryTimes = 3;

    }


}