package com.intelligenthealthcare.auth.infrastructure.jwt;

import com.intelligenthealthcare.auth.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 使用 HS256 对称签名；签名密钥来自配置 {@code app.jwt.secret}，需足够长且生产环境务必备份轮换。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.expirationMs();
    }

    /** subject 为患者主键，供 {@link com.intelligenthealthcare.auth.infrastructure.web.JwtAuthenticationFilter} 回查库。 */
    public String createToken(long patientId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(Long.toString(patientId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验；失败时抛出不检查异常，由调用方或过滤器处理。
     */
    public long parsePatientIdOrThrow(String token) {
        String sub = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(sub);
    }
}
