package com.tgdownloader.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 令牌服务
 *
 * 负责 JWT 令牌的生成、验证和 Claims 提取
 *
 * 使用 HMAC-SHA256 签名，密钥从配置文件读取
 *
 * @see JwtAuthenticationFilter 用于在请求中提取和验证 JWT
 */
@Service
public class JwtService {

    /** JWT 签名密钥（Base64 编码） */
    @Value("${jwt.secret}")
    private String secretKey;

    /** JWT 有效期（毫秒） */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ==================== Claims 提取 ====================

    /**
     * 从令牌中提取用户名（Subject）
     *
     * @param token JWT 令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中提取任意 Claims
     *
     * @param token          JWT 令牌
     * @param claimsResolver Claims 提取函数
     * @param <T>            返回类型
     * @return 提取的 Claims 值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ==================== 令牌生成 ====================

    /**
     * 为 UserDetails 生成令牌
     *
     * @param userDetails Spring Security 用户详情
     * @return JWT 令牌字符串
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * 为 UserDetails 生成带额外声明的令牌
     *
     * @param extraClaims    额外声明（键值对）
     * @param userDetails Spring Security 用户详情
     * @return JWT 令牌字符串
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails.getUsername(), jwtExpiration);
    }

    /**
     * 为用户名生成令牌（简化版）
     *
     * @param username 用户名
     * @return JWT 令牌字符串
     */
    public String generateToken(String username) {
        return buildToken(new HashMap<>(), username, jwtExpiration);
    }

    /**
     * 构建 JWT 令牌
     *
     * @param extraClaims 额外声明
     * @param subject     主题（用户名）
     * @param expiration  有效期（毫秒）
     * @return JWT 令牌字符串
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    // ==================== 令牌验证 ====================

    /**
     * 验证令牌是否有效
     *
     * 验证条件：
     * 1. 令牌中的用户名与提供的 UserDetails 用户名一致
     * 2. 令牌未过期
     *
     * @param token       JWT 令牌
     * @param userDetails Spring Security 用户详情
     * @return true=有效，false=无效
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * 检查令牌是否已过期
     *
     * @param token JWT 令牌
     * @return true=已过期，false=未过期
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 提取令牌过期时间
     *
     * @param token JWT 令牌
     * @return 过期时间
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ==================== 密钥管理 ====================

    /**
     * 从 Base64 密钥生成签名用的 SecretKey
     *
     * @return HMAC-SHA256 签名密钥
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 解析令牌获取所有 Claims
     *
     * @param token JWT 令牌
     * @return Claims 对象
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
