package com.laigeoffer.pmhub.base.security.feign;

import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.utils.ServletUtils;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.base.core.utils.ip.IpUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * feign 请求拦截器
 *
 * @author canghe
 */
@Component
//- 实现 Feign 的 RequestInterceptor 接口，拦截所有 Feign 客户端请求
public class FeignRequestInterceptor implements RequestInterceptor {
    @Override
    //- apply()：Feign 拦截器核心方法，每次 Feign 调用前执行
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes requestAttributes = ServletUtils.getRequestAttributes();
        if (requestAttributes == null || requestAttributes.getRequest() == null) {
            return;
        }
        HttpServletRequest httpServletRequest = requestAttributes.getRequest();
        //getHeaders()：提取原始请求的所有请求头
        Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
        // 传递用户信息请求头，防止丢失
        String userId = headers.get(SecurityConstants.DETAILS_USER_ID);
        if (StringUtils.isNotEmpty(userId)) {
            // requestTemplate.header()这是 Feign 的 API，用来给即将发出的 HTTP 请求添加请求头。
            requestTemplate.header(SecurityConstants.DETAILS_USER_ID, userId);
        }
        String userKey = headers.get(SecurityConstants.USER_KEY);
        if (StringUtils.isNotEmpty(userKey)) {
            requestTemplate.header(SecurityConstants.USER_KEY, userKey);
        }
        String userName = headers.get(SecurityConstants.DETAILS_USERNAME);
        if (StringUtils.isNotEmpty(userName)) {
            requestTemplate.header(SecurityConstants.DETAILS_USERNAME, userName);
        }
        // 继续传递认证 token（如果有）
        String authentication = headers.get(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(authentication)) {
            requestTemplate.header(SecurityConstants.AUTHORIZATION_HEADER, authentication);
        }


        /**
         *  配置客户端IP
         *   "X-Forwarded-For"
         *   这是一个标准的 HTTP 请求头，用于记录客户端真实 IP。
         *   为什么需要它？因为微服务架构中，请求会经过多层：
         *   用户 (IP: 203.0.113.5)
         *       ↓
         *   [网关/Nginx]
         *       ↓
         *   [服务 A]
         *       ↓ (Feign 调用)
         *   [服务 B] ← 这里拿到的远程地址是服务 A 的 IP，不是用户真实 IP
         *   如果没有 X-Forwarded-For，服务 B 看到的只是服务 A 的 IP（比如 192.168.1.100），拿不到用户的真实 IP。
         *
         *   简单来说：这行代码让微服务之间传递"谁在访问"的信息，避免 IP 在链路中丢失
         */
        requestTemplate.header("X-Forwarded-For", IpUtils.getIpAddr(httpServletRequest));
    }
}
