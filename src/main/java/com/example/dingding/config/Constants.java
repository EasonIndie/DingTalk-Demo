package com.example.dingding.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    // 部门相关缓存键
    public static String KEY_ALL_DEPARTMENT_IDS = "dingding:department:all_departmentids"; // 所有部门ID集合

    // 用户相关缓存键
    public static String KEY_ALL_USER_IDS = "dingding:user:all_userids"; // 所有用户ID集合

    // 数据过期时间（秒）- 24小时
    public static long CACHE_EXPIRE_TIME = 86400;
    //表单map
    public static final Map<String,String> FORM_MAP ;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("PROC-FD7C69D9-67AA-4C09-8DB1-3D1A40FC8679", "合理化建议【提给别的部门，提这个，无积分】");
        map.put("PROC-7C4DF150-6853-49B3-B5E3-742E37CBFCC1", "信息中心专用【 精益改善提案】");
        FORM_MAP = Collections.unmodifiableMap(map);
    }
}
