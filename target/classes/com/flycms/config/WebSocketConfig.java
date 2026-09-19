package com.flycms.config;

import com.flycms.interceptor.WebSocketInterceptor;
import com.flycms.module.websocket.service.WebSocketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:14 2018/7/8
 */

@Configuration
@EnableWebSocket
public class WebSocketConfig extends WebMvcConfigurationSupport implements WebSocketConfigurer {

	
	@Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(WebSocketService(), "/webSocketServer.action").addInterceptors(new WebSocketInterceptor()).setAllowedOrigins("*");
        registry.addHandler(WebSocketService(), "/ricky-websocket").addInterceptors(new WebSocketInterceptor()).withSockJS();
    }

    @Bean
    public WebSocketHandler WebSocketService() {
        return new WebSocketService();
    }

}