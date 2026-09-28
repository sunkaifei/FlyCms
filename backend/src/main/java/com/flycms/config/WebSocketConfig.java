package com.flycms.config;

import com.flycms.interceptor.WebSocketInterceptor;
import com.flycms.module.websocket.service.WebSocketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
/**
 * WebSocket 配置。
 *
 * <p>阶段 K3/G3 修正：原先本类与 {@code WebMvcConfig} <b>同时</b>继承
 * {@code WebMvcConfigurationSupport}，导致两件坏事——
 * ① 两处都继承了超类的 {@code @Bean localeResolver}，注册时互相冲突
 *   （{@code BeanDefinitionOverrideException}，Bean 覆盖在 Boot 2.1+ 默认关闭）；
 * ② 任何一处继承都会关闭 Spring Boot 的 {@code WebMvcAutoConfiguration}。
 * 本类只需要 {@link WebSocketConfigurer} 能力，故去掉无谓的继承。
 */

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	
	@Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 历史 Bug：setAllowedOrigins("*") 允许任意站点建立 WebSocket 连接。
        // 改为按前缀匹配白名单：仅本机任意端口（开发联调）与生产域名可连。
        String[] allowedOriginPatterns = {
                "http://localhost:*", "https://localhost:*",
                "http://127.0.0.1:*", "https://127.0.0.1:*",
                "http://www.28844.com", "https://www.28844.com",
                "http://28844.com", "https://28844.com"
        };
        registry.addHandler(WebSocketService(), "/webSocketServer.action")
                .addInterceptors(new WebSocketInterceptor())
                .setAllowedOriginPatterns(allowedOriginPatterns);
        registry.addHandler(WebSocketService(), "/ricky-websocket")
                .addInterceptors(new WebSocketInterceptor())
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .withSockJS();
    }

    @Bean
    public WebSocketHandler WebSocketService() {
        return new WebSocketService();
    }

}