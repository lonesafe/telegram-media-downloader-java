package com.tgdownloader.dto;

/**
 * 认证响应 DTO - 无 Lombok 版本
 */
public class AuthResponse {

    private String token;
    private String username;
    private long expiresIn;

    public AuthResponse(String token, String username, long expiresIn) {
        this.token = token;
        this.username = username;
        this.expiresIn = expiresIn;
    }

    public AuthResponse(String token) {
        this.token = token;
        this.username = null;
        this.expiresIn = 86400000L;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
