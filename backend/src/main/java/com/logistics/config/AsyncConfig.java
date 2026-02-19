package com.logistics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置
 * 启用@Async注解支持
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
