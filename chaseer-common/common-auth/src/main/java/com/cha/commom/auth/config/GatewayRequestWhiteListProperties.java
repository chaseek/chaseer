package com.cha.commom.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author ztc
 * @since 2024-10-30
 */
@Data
@ConfigurationProperties(prefix = "gateway.request")
public class GatewayRequestWhiteListProperties {

    /**
     * 非网关请求放行接口
     */
    private List<String> ignoreUrls;

}
