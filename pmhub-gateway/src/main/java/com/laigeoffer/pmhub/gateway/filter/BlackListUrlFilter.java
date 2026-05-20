package com.laigeoffer.pmhub.gateway.filter;

import com.laigeoffer.pmhub.base.core.utils.ServletUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 黑名单过滤器
 * Nacos 配置中心
 *       ↓
 *   读取配置字符串: ["/api/black1/**", "/api/black2/**"]
 *       ↓
 *   调用 BlackListUrlFilter.Config.setBlacklistUrl()
 *       ↓
 *   转换为正则: ["/api/black1/(.*?)", "/api/black2/(.*?)"]
 *       ↓
 *   编译为 Pattern 对象: [Pattern1, Pattern2]
 *
 *   3. 请求到达时的匹配过程
 *
 *   请求: GET /api/black1/delete
 *       ↓
 *   BlackListUrlFilter.apply() 被调用
 *       ↓
 *   config.matchBlacklist("/api/black1/delete")
 *       ↓
 *   Pattern("/api/black1/(.*?)").matcher("/api/black1/delete").find()
 *       ↓
 *   返回 true (匹配成功)
 *       ↓
 *   返回 "请求地址不允许访问"
 * @author canghe
 */
@Component
public class BlackListUrlFilter extends AbstractGatewayFilterFactory<BlackListUrlFilter.Config>
{
    @Override
    public GatewayFilter apply(Config config)
    {
        return (exchange, chain) -> {

            String url = exchange.getRequest().getURI().getPath();
            if (config.matchBlacklist(url))
            {
                return ServletUtils.webFluxResponseWriter(exchange.getResponse(), "请求地址不允许访问");
            }

            return chain.filter(exchange);
        };
    }

    public BlackListUrlFilter()
    {
        super(Config.class);
    }

    //静态内部类，类被加载的时候就初始化了
    public static class Config
    {
        // 存储原始配置的黑名单 URL 列表
        private List<String> blacklistUrl;

        // 存储预编译的正则表达式 Pattern 对象
        private List<Pattern> blacklistUrlPattern = new ArrayList<>();

        public boolean matchBlacklist(String url)
        {
            return !blacklistUrlPattern.isEmpty() && blacklistUrlPattern.stream().anyMatch(p -> p.matcher(url).find());
        }

        public List<String> getBlacklistUrl()
        {
            return blacklistUrl;
        }

        // 作用：将用户配置的黑名单 URL 字符串转换为预编译的正则表达式
        public void setBlacklistUrl(List<String> blacklistUrl)
        {
            this.blacklistUrl = blacklistUrl;
            this.blacklistUrlPattern.clear();
            this.blacklistUrl.forEach(url -> {
                this.blacklistUrlPattern.add(Pattern.compile(url.replaceAll("\\*\\*", "(.*?)"), Pattern.CASE_INSENSITIVE));
            });
        }
    }

}
