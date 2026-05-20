package com.laigeoffer.pmhub.base.security.annotation;

import java.lang.annotation.*;

/**
 * 内部认证注解
 * 
 * @author canghe
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InnerAuth
{
    /**
     * 是否校验用户信息
     *  isUser 是 @InnerAuth 注解的一个布尔类型参数，默认值为 false，此时默认不校验用户信息，仅验证接口权限，请求来源是 "inner" 即可
     *
     *
     * 为什么需要这个参数？
     *
     *   - 某些接口不需要用户上下文：比如一些公共的配置查询，只需要验证是内部调用即可
     *   - 某些接口需要用户信息：比如根据 userId 查询用户详情，必须确保调用时携带了用户上下文
     *
     *   - isUser = false（默认）：只验证请求来源是否为 "inner"，不校验用户上下文
     *   - isUser = true：除了验证来源，还要求请求携带用户信息（userId、username）
     */
    boolean isUser() default false;
}