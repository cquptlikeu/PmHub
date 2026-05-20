package com.laigeoffer.pmhub.base.security.aspect;

import com.laigeoffer.pmhub.base.security.annotation.RequiresLogin;
import com.laigeoffer.pmhub.base.security.annotation.RequiresPermissions;
import com.laigeoffer.pmhub.base.security.annotation.RequiresRoles;
import com.laigeoffer.pmhub.base.security.auth.AuthUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 基于 Spring Aop 的注解鉴权
 * 
 * @author kong
 */
@Aspect
@Component
public class PreAuthorizeAspect
{
    /**
     * 构建
     */
    public PreAuthorizeAspect()
    {
    }

    /**
     * 定义AOP签名 (切入所有使用鉴权注解的方法)
     * POINTCUT_SIGN 是一个切入点表达式字符串，它定义了 AOP 需要拦截哪些方法。具体来说，它会拦截标注了以下三个注解之一的所有方法：
     * 使用 @annotation() 语法匹配带有指定注解的方法
     * 使用 || 逻辑或运算符，表示只要方法上有这三个注解中的任何一个，就会被拦截
     */
    public static final String POINTCUT_SIGN = " @annotation(com.laigeoffer.pmhub.base.security.annotation.RequiresLogin) || "
            + "@annotation(com.laigeoffer.pmhub.base.security.annotation.RequiresPermissions) || "
            + "@annotation(com.laigeoffer.pmhub.base.security.annotation.RequiresRoles)";

    /**
     * 声明AOP签名
     * 将表达式应用到 pointcut() 方法上，以后提到的 pointcut() 方法，表示该方法将作为切入点方法。
     */
    @Pointcut(POINTCUT_SIGN)
    public void pointcut()
    {
    }

    /**
     * 环绕切入
     * 当被拦截的方法执行时，会先执行 around() 方法
     * 在方法执行前进行权限校验（调用 checkMethodAnnotation()）
     * 校验通过后，才执行原方法的逻辑（joinPoint.proceed()）
     * @param joinPoint 切面对象
     * @return 底层方法执行后的返回值
     * @throws Throwable 底层方法抛出的异常
     */
    @Around("pointcut()")
    // joinPoint：连接点，代表着被 AOP 拦截的那个方法执行时的"现场信息"。包含了被拦截方法的完整上下文信息：
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        // 注解鉴权
        // joinPoint.getSignature()：获取当前被拦截方法的签名对象
        // 因为切入点匹配的是方法级别的注解，所以可以确定是方法签名
        //在编程中，方法签名指的是能够唯一标识一个方法的信息集合，包括：
        //✅ 方法名
        //✅ 参数类型和顺序
        //✅ 返回值类型
        //✅ 所在的类
        //✅ 方法上的注解
        //注意：在 Java 中，方法签名通常不包括参数名（因为编译后会丢失），但 Spring 的 MethodSignature 可以通过反射获取参数名。
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //signature.getMethod() 拿到的是一个 java.lang.reflect.Method 对象，也就是 Java 反射中的方法对象。
        //Method 对象是 Java 反射机制的核心类之一，它代表了一个方法的完整信息，包括：
        //方法名
        //参数类型
        //返回值类型
        //修饰符（public、private 等）
        //方法上的所有注解 ← 这是当前代码最关心的
        //异常声明
        //泛型信息等
        checkMethodAnnotation(signature.getMethod());
        try
        {
            // 执行原有逻辑
            Object obj = joinPoint.proceed();
            return obj;
        }
        catch (Throwable e)
        {
            throw e;
        }
    }

    /**
     * 对一个Method对象进行注解检查
     */
    public void checkMethodAnnotation(Method method)
    {
        // 校验 @RequiresLogin 注解，检查这个方法上是否标注了 @RequiresLogin 注解
        RequiresLogin requiresLogin = method.getAnnotation(RequiresLogin.class);
        if (requiresLogin != null)
        {
            AuthUtil.checkLogin();
        }

        // 校验 @RequiresRoles 注解
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
        if (requiresRoles != null)
        {
            AuthUtil.checkRole(requiresRoles);
        }

        // 校验 @RequiresPermissions 注解
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);
        if (requiresPermissions != null)
        {
            AuthUtil.checkPermi(requiresPermissions);
        }
    }
}
