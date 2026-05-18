package com.tgdownloader.service;

import it.tdlight.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Web 认证供应器：处理 Telegram 用户登录全流程
 *
 * 同时实现：
 * - AuthenticationSupplier&lt;AuthenticationData&gt;：提供手机号（通过 get()）
 * - ClientInteraction：处理验证码/密码请求（通过 onParameterRequest()）
 *
 * 认证流程：
 * 1. TelegramClientService.connect() → clientBuilder.build(phoneAuthSupplier)
 *    TDLib 初始化后进入 AuthorizationStateWaitPhoneNumber
 * 2. Controller → setPhone(phone) → PhoneAuthSupplier.get() 返回 AuthenticationData
 *    TDLib 收到手机号后发送验证码，进入 AuthorizationStateWaitCode
 * 3. Controller → setCode(code) → onParameterRequest(ASK_CODE) 返回验证码
 *    TDLib 验证通过（或需要两步密码 → AuthorizationStateWaitPassword）
 * 4. Controller → setPassword(password) → onParameterRequest(ASK_PASSWORD) 返回密码
 *    TDLib 登录完成，进入 AuthorizationStateReady
 *
 * 自动登录：
 * 如果 tdlib_db/ 会话目录存在，TDLib 会自动恢复会话，直接进入 AuthorizationStateReady，
 * 不会调用 get()。如果 get() 仍被调用（会话已过期等），会检查会话目录，
 * 存在则立即返回 null 让 TDLib 自行处理。
 */
public class PhoneAuthSupplier
        implements AuthenticationSupplier<AuthenticationData>,
                   ClientInteraction {

    private static final Logger log = LoggerFactory.getLogger(PhoneAuthSupplier.class);

    // ==================== 认证数据存储 ====================

    private final AtomicReference<String> phoneRef = new AtomicReference<>();
    private final AtomicReference<String> codeRef = new AtomicReference<>();
    private final AtomicReference<String> passwordRef = new AtomicReference<>();
    private final AtomicReference<String> firstNameRef = new AtomicReference<>();
    private final AtomicReference<String> lastNameRef = new AtomicReference<>();
    private final long TIMEOUT_MS = 5 * 60 * 1000; // 5 分钟超时
    private volatile boolean closed = false;

    // ==================== Controller 调用的方法 ====================

    /** 设置手机号（供 Controller 调用） */
    public void setPhone(String phone) {
        synchronized (phoneRef) {
            phoneRef.set(phone);
            phoneRef.notifyAll();
        }
    }

    /** 设置验证码（供 Controller 调用） */
    public void setCode(String code) {
        synchronized (codeRef) {
            codeRef.set(code);
            codeRef.notifyAll();
        }
    }

    /** 设置两步验证密码（供 Controller 调用） */
    public void setPassword(String password) {
        synchronized (passwordRef) {
            passwordRef.set(password);
            passwordRef.notifyAll();
        }
    }

    // ==================== AuthenticationSupplier 实现 ====================

    /**
     * TDLib 在 AuthorizationStateWaitPhoneNumber 时调用此方法获取手机号。
     *
     * 自动登录：如果有已保存的有效会话，TDLib 不会调用此方法，直接进入 AuthorizationStateReady。
     * 只有当需要用户输入手机号时才会调用此方法，此时阻塞等待前端传入手机号。
     */
    @Override
    public CompletableFuture<AuthenticationData> get() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("[PhoneAuth] TDLib 请求手机号，等待用户输入...");
            String phone = waitFor(phoneRef, "手机号");
            log.info("[PhoneAuth] 提供手机号: {}", maskPhone(phone));
            return (AuthenticationData) new WebAuthData(phone);
        });
    }

    // ==================== ClientInteraction 实现 ====================

    /**
     * TDLib 在需要验证码、密码等参数时调用此方法。
     * 根据 InputParameter 类型阻塞等待对应的用户输入。
     */
    @Override
    public CompletableFuture<String> onParameterRequest(InputParameter parameter, ParameterInfo info) {
        log.info("[PhoneAuth] TDLib 请求参数: {} (info={})", parameter,
                info == null ? "null" : info.getClass().getSimpleName());

        switch (parameter) {
            case ASK_CODE:
                return waitForInput(codeRef, "验证码");

            case ASK_PASSWORD:
                return waitForInput(passwordRef, "两步密码");

            case ASK_FIRST_NAME:
                // 注册新账号时需要名字，默认给空串让 TDLib 用默认值
                return CompletableFuture.completedFuture("");

            case ASK_LAST_NAME:
                return CompletableFuture.completedFuture("");

            case ASK_EMAIL_ADDRESS:
                return waitForInput(new AtomicReference<>(), "邮箱地址");

            case ASK_EMAIL_CODE:
                return waitForInput(new AtomicReference<>(), "邮箱验证码");

            case NOTIFY_LINK:
                // 通知用户点击链接（如 Terms of Service）
                if (info instanceof ParameterInfoNotifyLink) {
                    String link = ((ParameterInfoNotifyLink) info).getLink();
                    log.info("[PhoneAuth] 请访问链接: {}", link);
                }
                return CompletableFuture.completedFuture("");

            case TERMS_OF_SERVICE:
                // 同意服务条款
                return CompletableFuture.completedFuture("");

            default:
                log.warn("[PhoneAuth] 未知参数类型: {}，返回空串", parameter);
                return CompletableFuture.completedFuture("");
        }
    }

    // ==================== 等待输入的通用方法 ====================

    private CompletableFuture<String> waitForInput(AtomicReference<String> ref, String name) {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                String value = waitFor(ref, name);
                future.complete(value);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future.orTimeout(5, TimeUnit.MINUTES);
    }

    private String waitFor(AtomicReference<String> ref, String name) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (ref.get() == null && !closed) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                throw new RuntimeException("等待" + name + "超时（5分钟）");
            }
            try {
                synchronized (ref) {
                    ref.wait(Math.min(remaining, 1000));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待" + name + "被中断");
            }
        }
        String value = ref.get();
        if (value == null) {
            throw new RuntimeException(name + "未提供（已关闭）");
        }
        return value;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public void close() {
        closed = true;
        synchronized (phoneRef) { phoneRef.notifyAll(); }
        synchronized (codeRef) { codeRef.notifyAll(); }
        synchronized (passwordRef) { passwordRef.notifyAll(); }
        synchronized (firstNameRef) { firstNameRef.notifyAll(); }
        synchronized (lastNameRef) { lastNameRef.notifyAll(); }
    }

    // ==================== AuthenticationData 实现 ====================

    /**
     * 承载手机号的 AuthenticationData 实现
     *
     * 参考 AuthenticationSupplier.user(phone) 的内部实现 AuthenticationDataImpl，
     * 它也是类似方式：构造一个 isBot=false, isQrCode=false, phoneNumber=xxx 的对象。
     * 由于 AuthenticationDataImpl 是 package-private 无法直接使用，
     * 所以这里自己实现 AuthenticationData 接口。
     */
    private static class WebAuthData implements AuthenticationData {
        private final String phoneNumber;

        WebAuthData(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        @Override
        public boolean isQrCode() {
            return false;
        }

        @Override
        public boolean isBot() {
            return false;
        }

        @Override
        public String getUserPhoneNumber() {
            return phoneNumber;
        }

        @Override
        public String getBotToken() {
            throw new UnsupportedOperationException("This is a user authentication, not a bot");
        }
    }
}
