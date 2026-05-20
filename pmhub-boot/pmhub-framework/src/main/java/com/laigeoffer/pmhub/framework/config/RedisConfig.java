package com.laigeoffer.pmhub.framework.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis配置
 * @EnableCaching,它的主要作用是开启 Spring 对基于注解的缓存管理支持。
 * 添加了 @EnableCaching 后，Spring 在启动时会在后台执行以下操作：
 *      1. 扫描：检查所有的 Spring Bean，看它们的方法上是否带有缓存注解。
 *      2. 代理（AOP）：只要发现带有缓存注解的类或方法，Spring 就会利用 AOP（面向切面编程）为该类生成一个代理对象。
 *      3. 拦截：当你调用这个方法时，实际调用的是代理对象。代理对象会“拦截”你的调用，去底层的 CacheManager（比如 Redis、Caffeine 或本地内存）中检查是否有缓存数据。
 *          如果有，直接返回缓存结果（不执行真实方法）；如果没有，则执行真实方法，并将返回结果存入缓存。
 * 通常，它会被放在 @Configuration 配置类或者 Spring Boot 的启动类上。
 */
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {   // 继承CachingConfigurerSupport， 提供缓存配置的默认实现，继承该类后，Spring Boot 会自动将 RedisTemplate 注册到 Spring 容器中。
    @Bean
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        //template是Redis操作模板，用于执行Redis命令，下面的一堆代码都是在设置RedisTemplate的序列化方式
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 限流 Lua 脚本 Bean
     * @return
     */
    @Bean
    public DefaultRedisScript<Long> limitScript() {
        // DefaultRedisScript<Long>是Spring Data Redis 的 Lua 脚本封装类，<Long> 是泛型返回值
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        //设置 Lua 脚本的文本内容（就是 limitScriptText() 方法返回的字符串）
        redisScript.setScriptText(limitScriptText());
        //告诉 Spring 把 Lua 脚本的返回值转为 Long 类型
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 限流脚本
     */
    private String limitScriptText() {
        return "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local time = tonumber(ARGV[2])\n" +
                "local current = redis.call('get', key);\n" +
                "if current and tonumber(current) > count then\n" +
                "    return tonumber(current);\n" +
                "end\n" +
                "current = redis.call('incr', key)\n" +
                "if tonumber(current) == 1 then\n" +
                "    redis.call('expire', key, time)\n" +
                "end\n" +
                "return tonumber(current);";
    }
}
