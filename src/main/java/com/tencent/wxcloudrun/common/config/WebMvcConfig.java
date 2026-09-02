package com.tencent.wxcloudrun.common.config;

import com.tencent.wxcloudrun.common.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 AuthInterceptor。
 * 登录 /wx/login 放行（不需要 token），其他 /api/** 都要鉴权。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuthInterceptor authInterceptor;

  public WebMvcConfig(@Autowired AuthInterceptor authInterceptor) {
    this.authInterceptor = authInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/user/wx/login",     // 登录本身不需要 token
            "/api/payment/wx/notify", // 微信支付回调（微信服务器调用，无 token）
            "/error",                 // Spring Boot 默认错误页
            "/static/**",             // 模板静态资源
            "/"                       // 首页（健康检查用）
        );
  }
}
