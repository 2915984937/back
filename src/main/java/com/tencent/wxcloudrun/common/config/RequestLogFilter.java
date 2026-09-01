package com.tencent.wxcloudrun.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求日志 Filter：每个请求进来时用红色打印 方法 + URI + 耗时。
 */
@Component
public class RequestLogFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_RED   = "\u001B[31m";
  private static final String ANSI_CYAN  = "\u001B[36m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW= "\u001B[33m";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String method = req.getMethod();
    String uri = req.getRequestURI();
    String query = req.getQueryString();
    String fullPath = query == null ? uri : uri + "?" + query;

    long start = System.currentTimeMillis();
    try {
      chain.doFilter(request, response);
    } finally {
      long cost = System.currentTimeMillis() - start;
      String color = cost > 2000 ? ANSI_RED
                   : cost > 500  ? ANSI_YELLOW
                   : ANSI_GREEN;
      // 红色打印接口
      log.info("{}>>> {} {}{} → {}ms{}",
          ANSI_RED, method, fullPath, ANSI_RESET,
          color + cost + ANSI_RESET, ANSI_RESET);
    }
  }
}