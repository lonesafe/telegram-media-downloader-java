package com.tgdownloader.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 *
 * 用于实时推送下载进度到前端
 *
 * 工作机制：
 * 1. 启用 STOMP 消息代理（Simple Text Oriented Messaging Protocol）
 * 2. 服务端可向 /topic/xxx 推送消息，前端订阅该主题接收
 * 3. 前端向 /app/xxx 发送消息，服务端处理
 *
 * 前端连接示例：
 *   const socket = new SockJS('/ws');
 *   const stompClient = Stomp.over(socket);
 *   stompClient.subscribe('/topic/download-progress', (message) => {
 *       console.log(JSON.parse(message.body));
 *   });
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     *
     * @param config 消息代理注册器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理
        // 客户端订阅 /topic/xxx 接收服务端推送的消息
        config.enableSimpleBroker("/topic");
        // 客户端发送消息的前缀（服务端通过 @MessageMapping 处理）
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册 STOMP 端点（WebSocket 连接入口）
     *
     * @param registry STOMP 端点注册器
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点，前端通过 /ws 建立连接
        // withSockJS() 启用 SockJS 降级（浏览器不支持 WebSocket 时回退到轮询）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
