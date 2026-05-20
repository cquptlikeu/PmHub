package com.laigeoffer.pmhub.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 放行白名单配置
 * 1. gateway的yml文件中了 Nacos Config 地址 192.168.100.128:8848
 * 2. 应用启动时会通过yml配置文件中读取shared-configs（共享配置），从 Nacos 配置中心加载 application-dev.yml
 * 在 Nacos 配置中心的配置文件中配置了白名单有哪些，有下面类似的配置
 *   security:
 *     ignore:
 *       whites:
 * IgnoreWhiteProperties 这个类就能够通过 @ConfigurationProperties 注解，将 Nacos 配置文件中的配置映射到这个类中。
 * @author canghe
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreWhiteProperties
{
    /**
     * 放行白名单配置，网关不校验此处的白名单
     */
    private List<String> whites = new ArrayList<>();

    public List<String> getWhites()
    {
        return whites;
    }

    public void setWhites(List<String> whites)
    {
        this.whites = whites;
    }
}
