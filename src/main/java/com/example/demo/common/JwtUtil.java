package com.example.demo.common;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
    /**
     * JWT工具类（主要包含token生成、检验、刷新）
     *          还解析用户名、角色、用户id，便于后续拓展
     */

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expire;

    private SecretKey getSigningKey() {
        /*
        获取密钥
        * @return 密钥
         */
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    public String generateToken(String username, String role, long userId) {
        /*
        生成token
        * @param username 用户名
        * @param role 角色
        * @param userId 用户id
        * @return token
         */
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expire);

        return Jwts.builder()
                .subject(username)
                .claim("role",role)
                .claim("userId",userId)
                .issuedAt(now)
                .expiration(expireTime)
                .signWith(getSigningKey())
                .compact();
    }
    public Claims getClaimsFromToken(String token) {
        /*
        从token中获取claims
        * @param token
        * @return claims
         */
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }
    public String getUsernameFromToken(String token) {
        /*
        从token中获取用户名
        * @param token
        * @return 用户名
         */
        return getClaimsFromToken(token).getSubject();
    }
    public String getRoleFromToken(String token) {
        /*
        从token中获取角色
        * @param token
        * @return 角色
         */
        return (String) getClaimsFromToken(token).get("role");
    }
    public Long getUserIdFromToken(String token) {
        /*
        从token中获取用户id
        * @param token
        * @return 用户id
         */
        return (Long) getClaimsFromToken(token).get("userId");
    }

    /**
     * 验证 token 并返回 Claims（一次解析，避免重复解析 3 次）
     * @return 解析成功返回 Claims，失败返回 null
     */
    public Claims validateAndParseToken(String token) {
        try {
            return getClaimsFromToken(token);
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException | MalformedJwtException | io.jsonwebtoken.security.SignatureException e) {
            log.warn("Token 格式或签名异常: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token 参数非法: {}", e.getMessage());
        }
        return null;
    }

    public boolean validateToken(String token) {
        return validateAndParseToken(token) != null;
    }
}
