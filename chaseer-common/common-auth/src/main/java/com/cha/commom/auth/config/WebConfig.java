package com.cha.common.auth.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.same.SaSameUtil;
import com.cha.common.auth.aspect.PermissionAspect;
import com.cha.common.auth.filter.MobileUserFilter;
import com.cha.common.auth.filter.TenancyIdWebFilter;
import com.cha.common.auth.service.SmsService;
import com.cha.common.auth.service.impl.AliyunSmsServiceImpl;
import com.cha.common.core.constant.SecurityConstant;
import com.cha.common.redis.util.RedisUtils;
import com.cha.common.web.result.R;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author ztc
 * @since 2023-08-25
 */
@Slf4j
@Configuration
@ConditionalOnClass(Filter.class)
@EnableConfigurationProperties(GatewayRequestWhiteListProperties.class)
public class WebConfig {

    @Bean
    public Filter saServletFilter(GatewayRequestWhiteListProperties properties) {
        List<String> ignoreUrls = Optional.ofNullable(properties.getIgnoreUrls()).orElse(Collections.emptyList());
        String actuatorPath = "/actuator/**";
        String nacosPath = "/**/cha-system/**";
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico")
                // 接口文档的请求不拦截，方便本地开发调式
                .addExclude("/doc.html")
                .addExclude("/**/swagger-resources/**")
                .addExclude("/**/api-docs/**")
                .addExclude("/**/css/**")
                .addExclude("/**/js/**")
                .addExclude(actuatorPath)
                .addExclude(nacosPath)
                .addExclude(ignoreUrls.toArray(new String[ignoreUrls.size()]))
                .setAuth(obj -> {
                    //校验身份凭证，确认是网关过来的请求，该token由网关生成
                    String token = SaHolder.getRequest().getHeader(SecurityConstant.GATEWAY_TOKEN);
                    SaSameUtil.checkToken(token);
                }).setError(e -> {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    HttpServletRequest request = attributes.getRequest();
                    HttpServletResponse response = attributes.getResponse();
                    String path = request.getRequestURI();
                    log.warn("request is not from gateway,path={},errorMsg={}", path, e.getMessage());
                    response.setContentType("application/json; charset=utf-8");
                    response.setStatus(401);
                    R<Void> r = new R();
                    r.setCode("4000");
                    r.setMsg("非法的请求");
                    return JSON.toJSONString(r);
                });
    }

    @Bean
    public Filter tenancyIdWebFilter() {
        return new TenancyIdWebFilter();
    }

    @Bean
    public SmsService aliyunSmsService(RedisUtils redisUtils) {
        return new AliyunSmsServiceImpl(redisUtils);
    }

    @Bean
    public PermissionAspect permissionAspect(SmsService smsService) {
        return new PermissionAspect(smsService);
    }

    @Bean
    public Filter mobileUserFilter() {
        return new MobileUserFilter();
    }

}
