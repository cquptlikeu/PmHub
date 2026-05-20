package com.laigeoffer.pmhub.gateway.handler;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.laigeoffer.pmhub.base.core.utils.ServletUtils;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * 自定义限流异常处理
 *
 * @author canghe
 */
public class SentinelFallbackHandler implements WebExceptionHandler
{
    private Mono<Void> writeResponse(ServerResponse response, ServerWebExchange exchange)
    {
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), "请求超过最大数，请稍候再试");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex)
    {
        //检查响应是否已提交（避免重复写入）
        if (exchange.getResponse().isCommitted())
        {
            return Mono.error(ex);
        }
        //检查异常是否为限流异常
        if (!BlockException.isBlockException(ex))
        {
            return Mono.error(ex);
        }
        //调用 GatewayCallbackManager 获取限流处理器
        //返回统一错误消息
        return handleBlockedRequest(exchange, ex).flatMap(response -> writeResponse(response, exchange));
    }

    private Mono<ServerResponse> handleBlockedRequest(ServerWebExchange exchange, Throwable throwable)
    {
        return GatewayCallbackManager.getBlockHandler().handleRequest(exchange, throwable);
    }
}
