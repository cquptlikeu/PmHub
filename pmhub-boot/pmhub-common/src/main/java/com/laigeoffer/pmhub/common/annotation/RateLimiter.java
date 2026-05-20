package com.laigeoffer.pmhub.common.annotation;

import com.laigeoffer.pmhub.common.enums.LimitType;
import com.laigeoffer.pmhub.common.constant.CacheConstants;

import java.lang.annotation.*;

/**
 * 限流注解
 * @Target和@Retention都是元注解（用来修饰注解的注解），定义了 @RateLimiter 的使用规则
 *  1. @Target(ElementType.METHOD)，意思：此注解只能用在方法上
 *  2. @Retention(RetentionPolicy.RUNTIME)，意思：此注解会保留到运行时，可以通过反射读取。
 *       为什么 @RateLimiter 必须是 RUNTIME？
 *          因为限流功能是通过 AOP 切面在运行时实现的：
 *          代码执行 → AOP 拦截 → 反射读取 @RateLimiter 注解 → 执行限流逻辑
 *          如果改成 CLASS 或 SOURCE，运行时就读不到注解了，限流功能会失效。
 * @author canghe
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
//这是 Java 中定义注解（Annotation）的语法，@interface：Java 关键字，专门用于定义注解类型
public @interface RateLimiter {
    /**
     * 限流key
     */
    public String key() default CacheConstants.RATE_LIMIT_KEY;

    /**
     * 限流时间,单位秒
     */
    public int time() default 60;

    /**
     * 限流次数
     */
    public int count() default 100;

    /**
     * 限流类型
     */
    public LimitType limitType() default LimitType.DEFAULT;
}
