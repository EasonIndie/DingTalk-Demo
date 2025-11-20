package com.example.dingding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Web配置类
 *
 * @author system
 * @version 1.0.0
 */
@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 设置请求工厂，支持请求体缓存
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(
            new SimpleClientHttpRequestFactory()
        );
        restTemplate.setRequestFactory(factory);

        // 设置消息转换器
        restTemplate.getMessageConverters().clear();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        // 添加拦截器
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        // 可以添加日志拦截器等
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }
}