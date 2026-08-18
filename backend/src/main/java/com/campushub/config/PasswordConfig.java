package com.campushub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密配置：BCryptPasswordEncoder
 *
 * BCrypt 特点（面试常考）：
 * 1. 单向哈希，不可逆——数据库泄露也无法还原明文
 * 2. 自带随机盐，同一密码每次加密结果都不同
 * 3. 加密耗时可控（cost 参数），天然抗暴力破解
 */
@Configuration
public class PasswordConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
