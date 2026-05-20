package com.laigeoffer.pmhub.framework.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.TimeZone;

/**
 * 程序注解配置
 *
 * @author canghe
 */
@Configuration    //标记这是一个 Spring 配置类，相当于 XML 配置文件，Spring 会扫描并加载其中定义的 Bean。
// 启用 AspectJ 自动代理（AOP 功能）
// exposeProxy = true：表示通过aop框架暴露该代理对象,AopContext能够访问，作用：当同一个类中的方法互相调用时，仍能让 AOP 切面生效（通过 AopContext.currentProxy() 获取代理对象）
@EnableAspectJAutoProxy(exposeProxy = true)
// 指定要扫描的Mapper类的包的路径
@MapperScan("com.laigeoffer.pmhub.**.mapper")
public class ApplicationConfig {
    /**
     * 时区配置
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
    }
}
