package com.example.dingding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 钉钉数据表单实例详情获取Demo
 *
 * @author system
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务调度功能
public class DingdingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DingdingDemoApplication.class, args);
        System.out.println("钉钉Demo启动成功！");
    }
}