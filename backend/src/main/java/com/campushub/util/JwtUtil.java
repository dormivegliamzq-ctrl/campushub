package com.campushub.util;

import com.campushub.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：签发与解析
 *
 * 结构（三段，点号分隔，Base64 编码）：
 *   Header.Payload.Signature
 *   - Header：算法声明（HS256）
 *   - Payload：业务数据（这里放 userId + username + 过期时间）
 *   - Signature：用密钥对前两段做签名，保证内容不被篡改
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties props;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // 用配置的密钥字符串构造 HS256 签名密钥
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 token */
    public String generateToken(Long userId, String username) {
        long expireMs = props.getExpireHours() * 3600_000L;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))   // subject：用户ID
                .claim("username", username)       // 自定义字段：用户名
                .issuedAt(now)                     // 签发时间
                .expiration(new Date(now.getTime() + expireMs)) // 过期时间
                .signWith(key)                     // HS256 签名
                .compact();
    }

    /** 解析并校验 token；签名不对 / 已过期会抛 JwtException，由调用方处理 */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
