package com.laigeoffer.pmhub.base.security.feign;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FeignRequestInterceptorTest {

    private final FeignRequestInterceptor feignRequestInterceptor = new FeignRequestInterceptor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldIgnoreMissingServletRequestContext() {
        RequestTemplate requestTemplate = new RequestTemplate();

        assertDoesNotThrow(() -> feignRequestInterceptor.apply(requestTemplate));
    }

    @Test
    void shouldAllowServletRequestContextWhenItExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("user_id", "100");
        request.addHeader("user_key", "uk-1");
        request.addHeader("username", "tester");
        request.addHeader("authorization", "Bearer demo");
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate requestTemplate = new RequestTemplate();

        assertDoesNotThrow(() -> feignRequestInterceptor.apply(requestTemplate));
    }
}
