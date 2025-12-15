package com.cha.commom.auth.config;

import cn.dev33.satoken.config.SaTokenConfig;
import com.cha.common.auth.util.AppUtil;
import com.cha.common.auth.util.TenancyUtil;
import com.cha.common.core.constant.SecurityConstant;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author ztc
 * @since 2023-03-12
 */
@Configuration
public class DefaultAuthConfig {

    /**
     * 可以使用yaml替换以下默认配置
     *
     * @return
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "sa-token")
    public SaTokenConfig saTokenConfiguration() {
        SaTokenConfig saTokenConfig = new SaTokenConfig();
        //# token名称 (同时也是cookie名称)
        saTokenConfig.setTokenName(SecurityConstant.HEADER_TOKEN);
        //# token有效期，单位秒，-1代表永不过期
        saTokenConfig.setTimeout(3600 * 24L);
        //# token临时有效期 (指定时间内无操作就视为token过期)，单位秒
        saTokenConfig.setActivityTimeout(-1L);
        //# 是否允许同一账号并发登录 (为false时新登录挤掉旧登录)
        // 20240407 不允许账号并发登录
        saTokenConfig.setIsConcurrent(Boolean.FALSE);
        //# 在多人登录同一账号时，是否共用一个token (为false时每次登录新建一个token)
        saTokenConfig.setIsShare(Boolean.FALSE);
        //同一账号最大登录数量，-1代表不限，超出数量，之前的token会失效
        saTokenConfig.setMaxLoginCount(3);
        //# token风格
        saTokenConfig.setTokenStyle("uuid");
        //# 是否输出操作日志
        saTokenConfig.setIsLog(Boolean.FALSE);
        //# 是否从cookie中读取token
        saTokenConfig.setIsReadCookie(Boolean.FALSE);
        //# 是否从head中读取token
        saTokenConfig.setIsReadHeader(Boolean.TRUE);
        saTokenConfig.setIsPrint(Boolean.FALSE);
        return saTokenConfig;
    }

    @Bean
    public AppUtil appUtil() {
        return new AppUtil();
    }

    @Bean
    public TenancyUtil tenancyUtil() {
        return new TenancyUtil();
    }

}
