package com.laigeoffer.pmhub.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**核心问题：Spring Cloud Gateway 中 Body 流只能读取一次
 * Spring WebFlux 是 Spring Cloud Gateway 的底层基石与核心引擎
 *在 Spring WebFlux (Spring Cloud Gateway 底层) 中，HTTP 请求的 Body 是基于 Flux<DataBuffer> 的数据流。数据流的特点是一旦被读取，就会被消费掉，无法再次读取。
 *
 * 获取body请求数据（解决流不能重复读取问题）
 * 在处理验证码或者记录日志时，系统往往需要读取 HTTP 请求里的 Body 内容。
 * 但在标准的原生框架中，请求的 Body 就像流水一样，读了一次就没了（流不能重复读取）。
 * 这个过滤器的作用通常是把请求的内容缓存下来，方便后面的流程（比如验证码校验）再次读取。
 * @author canghe
 */
@Component
public class CacheRequestFilter extends AbstractGatewayFilterFactory<CacheRequestFilter.Config>
{
    public CacheRequestFilter()
    {
        super(Config.class);
    }

    @Override
    public String name()
    {
        return "CacheRequestFilter";
    }

    //apply方法是用来创建过滤器实例的，返回的过滤器对象会被添加到 GatewayFilterChain 中，并依次执行。
    @Override
    public GatewayFilter apply(Config config)
    {
        CacheRequestGatewayFilter cacheRequestGatewayFilter = new CacheRequestGatewayFilter();
        Integer order = config.getOrder();
        if (order == null)
        {
            return cacheRequestGatewayFilter;
        }
        return new OrderedGatewayFilter(cacheRequestGatewayFilter, order);
    }

    public static class CacheRequestGatewayFilter implements GatewayFilter
    {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
        {
            // GET DELETE 不过滤
            HttpMethod method = exchange.getRequest().getMethod();
            //：GET 和 DELETE 请求通常没有请求体（Body）
            if (method == null || method == HttpMethod.GET || method == HttpMethod.DELETE)
            {
                return chain.filter(exchange);
            }
            return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, (serverHttpRequest) -> {
                // 如果请求已被缓存（serverHttpRequest == exchange.getRequest()），直接放行
                if (serverHttpRequest == exchange.getRequest())
                {
                    return chain.filter(exchange);
                }
                //；否则使用缓存后的请求继续流程
                return chain.filter(exchange.mutate().request(serverHttpRequest).build());
            });
        }
    }

    @Override
    public List<String> shortcutFieldOrder()
    {
        return Collections.singletonList("order");
    }

    static class Config
    {
        private Integer order;

        public Integer getOrder()
        {
            return order;
        }

        public void setOrder(Integer order)
        {
            this.order = order;
        }
    }
}