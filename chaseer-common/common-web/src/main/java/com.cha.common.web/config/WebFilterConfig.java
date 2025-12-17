package com.cha.common.web.config;

import com.cha.common.web.filter.RequestContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ztc
 * @since 2023-03-17
 */
@Configuration
public class WebFilterConfig {

    @Bean
    public RequestContextFilter chaRequestContextFilter() {
        return new RequestContextFilter();
    }

}
