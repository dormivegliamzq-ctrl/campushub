package com.campushub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项，绑定 application.yml 中的 jwt.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（HS256 要求至少 32 字节） */
    private String secret;

    /** 过期时间（小时） */
    private long expireHours = 24;
}
