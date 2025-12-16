package com.example.dingding.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    //表单map
    public static final Map<String,String> FORM_MAP ;
    static {
        Map<String, String> map = new HashMap<>();
//        map.put("PROC-48361881-52EB-4714-B94D-0CFA0C4CF553", "精益改善提案【先改再提，自己内部能解决】");
//        map.put("PROC-7C4DF150-6853-49B3-B5E3-742E37CBFCC1", "信息中心专用【 精益改善提案】");
        map.put("PROC-241823B3-6CC4-488F-B9CD-6AFB2AE17845", "测试精益");
        FORM_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * 3个月的毫秒数，用于时间拆分计算
     */
    public static final long THREE_MONTHS_MILLIS = 90L * 24 * 60 * 60 * 1000;

    /**
     * 默认API调用间隔（毫秒）
     */
    public static final long DEFAULT_API_CALL_INTERVAL = 100L;

    /**
     * 默认最大时间拆分数量
     */
    public static final int DEFAULT_MAX_TIME_SPLITS = 50;
}
