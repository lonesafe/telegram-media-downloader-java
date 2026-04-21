package com.tgdownloader.controller;

import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.dto.AuthRequest;
import com.tgdownloader.dto.AuthResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 *
 * 当前配置为无认证模式（SecurityConfig permitAll），
 * 此 Controller 直接返回成功，不进行实际验证
 *
 * 如需启用 JWT 认证，使用 JwtAuthenticationFilter
 *
 * @see com.tgdownloader.config.SecurityConfig 安全配置
 * @see com.tgdownloader.config.jwt.JwtAuthenticationFilter JWT 过滤器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 登录
     *
     * 当前实现：直接返回成功令牌，不验证用户名密码
     *
     * @param request 认证请求（用户名/密码）
     * @return 认证响应（包含 token）
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest request) {
        // 直接返回成功，无需验证
        return ApiResponse.success(new AuthResponse("token-not-required", "admin", 86400000L));
    }

    /**
     * 登出
     *
     * 当前实现：直接返回成功（无状态）
     *
     * @return 空成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
