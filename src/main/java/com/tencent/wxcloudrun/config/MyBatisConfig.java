package com.tencent.wxcloudrun.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis mapper 扫描配置，独立成 @Configuration 而不是写在启动类上。
 * 这样运行时由 @SpringBootApplication 组件扫描加载（行为不变），
 * 而 @WebMvcTest 这类切片测试不会加载它，从而无需依赖数据库即可启动测试上下文。
 */
@Configuration
@MapperScan(basePackages = {"com.tencent.wxcloudrun.dao"})
public class MyBatisConfig {
}