package com.laigeoffer.pmhub.gateway.config;


import com.laigeoffer.pmhub.gateway.handler.SentinelFallbackHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 网关限流配置
 * 核心职责： 当网关触发 Sentinel 限流规则时，提供一个统一的降级响应处理器。
 * @author canghe
 */
@Configuration
public class GatewayConfig
{
    @Bean
    // @Order(Ordered.HIGHEST_PRECEDENCE) - 设置最高优先级，确保限流降级处理器最先被调用
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelFallbackHandler sentinelGatewayExceptionHandler()
    {
        return new SentinelFallbackHandler();
    }
}