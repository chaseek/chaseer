package com.cha.common.web.config;

import cn.dev33.satoken.same.SaSameUtil;
import com.cha.common.core.constant.SecurityConstant;
import com.cha.common.core.context.TenancyContext;
import com.cha.common.core.context.ThreadContext;
import com.cha.common.core.exception.ADKException;
import com.cha.common.core.utils.LogEvent;
import com.cha.common.web.result.R;
import com.alibaba.fastjson.JSON;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Objects;

/**
 * @author songChaoHua
 * @Description Feign配置类
 * @date 2023/2/13 15:33
 */
@Configuration
public class FeignConfig {

    public final static String TRANSFER_ENCODING = "transfer-encoding";
    public final static String SERVICE_NAME = "serviceName";

    @Value("${spring.application.name:}")
    private String applicationName;

    /**
     * 让DispatcherServlet向子线程传递RequestContext
     *
     * @param servlet servlet
     * @return 注册bean
     */
    @Bean
    public ServletRegistrationBean<DispatcherServlet> dispatcherRegistration(DispatcherServlet servlet) {
        servlet.setThreadContextInheritable(true);
        RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true);
        return new ServletRegistrationBean<>(servlet, "/**");
    }

    /**
     * 解决RequestContextHolder.getRequestAttributes()为空的问题
     *
     * @return
     */
    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    /**
     * 覆写拦截器，在feign发送请求前取出原来的header并转发
     *
     * @return 拦截器
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            Object gatewayToken = ThreadContext.getContext(SecurityConstant.GATEWAY_TOKEN);
            if (gatewayToken == null) {
                ThreadContext.addContext(SecurityConstant.GATEWAY_TOKEN, SaSameUtil.getToken());
            }
            //token单独传递，防止异步线程主线程销毁request导致拿不到token
            template.header(SecurityConstant.HEADER_TOKEN, ThreadContext.getContext(SecurityConstant.HEADER_TOKEN, String.class))
                    .header(SecurityConstant.GATEWAY_TOKEN, ThreadContext.getContext(SecurityConstant.GATEWAY_TOKEN, String.class))
                    .header(SecurityConstant.APP_ID, ThreadContext.getContext(SecurityConstant.APP_ID, String.class))
                    .header(SecurityConstant.TENANCY_ID, Objects.toString(TenancyContext.getTenancyId(), null))
                    .header(SecurityConstant.UUID, ThreadContext.getContext(SecurityConstant.UUID, String.class))
                    .header(SERVICE_NAME, applicationName);

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            //获取请求头
            Enumeration<String> headerNames;
            try {
                headerNames = request.getHeaderNames();
            } catch (Exception e) {
                return;
            }
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name;
                    String values;
                    try {
                        name = headerNames.nextElement();
                        values = request.getHeader(name);
                    } catch (Exception e) {
                        continue;
                    }
                    //将请求头保存到模板中
                    if (name.equalsIgnoreCase(SERVICE_NAME)) {
                        continue;
                    }
                    if (TRANSFER_ENCODING.equalsIgnoreCase(name)) {
                        continue;
                    }
                    template.header(name, values);
                }
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    public class FeignErrorDecoder extends ErrorDecoder.Default {

        @Override
        public Exception decode(String methodKey, Response response) {
            Exception exception = super.decode(methodKey, response);
            if (exception instanceof RetryableException) {
                return exception;
            }
            String message = exception.getMessage();
            if (message == null) {
                return exception;
            }
            try {
                int start = message.lastIndexOf("{");
                int end = message.lastIndexOf("}");
                if (start != -1 && end != -1 && end > start) {
                    String json = message.substring(start, end + 1);
                    R r = JSON.parseObject(json, R.class);
                    if (r != null && r.getMsg() != null && r.getCode() != null && !R.isSuccess(r)) {
                        LogEvent.warn("微服务调用业务异常，methodKey={}，errorMsg={}", methodKey, message);
                        exception = new ADKException(r.getCode(), r.getMsg());
                    }
                }
            } catch (Exception e) {
            }
            return exception;
        }

    }

}
