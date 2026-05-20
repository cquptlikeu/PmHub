package com.laigeoffer.pmhub.api.system;

import com.laigeoffer.pmhub.api.system.domain.dto.SysUserDTO;
import com.laigeoffer.pmhub.api.system.factory.UserFeginFallbackFactory;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.constant.ServiceNameConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.base.core.core.domain.vo.SysUserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author canghe
 * @description 用户服务
 * @create 2024-04-24-22:38
 *
 * FeignClient：声明一个 Feign 客户端，用于调用其他服务
 * contextId：Feign 客户端的唯一标识，区分多个 Feign 客户端，用于 Spring 容器内部管理 Bean
 * value ： 目标微服务名称，从 ServiceNameConstants 常量类获取，，用于服务调用
 * fallbackFactory： 服务降级处理工厂，当目标服务不可用时返回兜底数据
 */
@FeignClient(contextId = "userFeignService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = UserFeginFallbackFactory.class)
public interface UserFeignService {



    /**
     * 根据用户名获取当前用户信息
     * 调用这个方法的时候，，Feign 会自动帮你发起一个 HTTP 请求：
     * userFeignService.info(username, SecurityConstants.INNER);
     *
     *  Feign 实际发出的 HTTP 请求
     *  GET /system/user/info/laigeoffer
     *  请求头里包括Header: from-source: inner
     *
     *  pmhub-system 服务的 Controller 接收请求
     *  它收到请求之后，会用@InnerAuth注解做安全验证，只接收内部调用的请求
     *
     */
    @GetMapping("/system/user/info/{username}")
    R<LoginUser> info(@PathVariable("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据 userId 获取用户信息
     */
    @GetMapping("/system/user/getInfoByUserId/{userId}")
    R<LoginUser> getInfoByUserId(@PathVariable("userId") Long userId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据条件获取用户列表
     */
    @PostMapping("/system/user/listOfInner")
    R<List<SysUserVO>> listOfInner(@RequestBody SysUserDTO sysUserDTO, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);


    /**
     * 注册用户信息
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PostMapping("/system/user/register")
    R<Boolean> registerUserInfo(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
