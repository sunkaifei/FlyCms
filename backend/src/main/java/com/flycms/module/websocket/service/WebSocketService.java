package com.flycms.module.websocket.service;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
@Service
public class WebSocketService extends TextWebSocketHandler  {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final CopyOnWriteArraySet<WebSocketSession> users = new CopyOnWriteArraySet<>();

    // 用户进入系统监听
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println(session.getAttributes().get("accountId")+"监听页面登录用户==============="+session.getId());
    	
    	System.out.println("成功进入了系统。。。");
        users.add(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception  {
    	for (WebSocketSession user : users) {
    		System.out.println(user.getAttributes().get("accountId")+"====2====发送消息给指定的用户=========");
        }
    	System.out.println("======当前在线用户========="+users.size());
        super.handleTextMessage(session, message);
        session.sendMessage(message);
    }
    
   

    // 后台错误信息处理方法
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    	 if(session.isOpen()){session.close();}
         logger.debug("websocket connection closed......");
         users.remove(session);
    }

    // 用户退出后的处理，不如退出之后，要将用户信息从websocket的session中remove掉，这样用户就处于离线状态了，也不会占用系统资源
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        if (session.isOpen()) {
            session.close();
        }
        users.remove(session);
        System.out.println("安全退出了系统");
        System.out.println("======当前剩余在线用户========="+users.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 给所有的用户发送消息
     */
    public void sendMessagesToUsers(TextMessage message) {
        
    	for (WebSocketSession user : users) {
            try {
                // isOpen()在线就发送
                if (user.isOpen()) {
                    user.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送消息给指定的用户
     */
    public void sendMessageToUser(String userId, TextMessage message) {
    	for (WebSocketSession user : users) {
    		System.out.println(user.getAttributes().get("accountId")+"========发送消息给指定的用户========="+userId);
    		if (user.getAttributes().get("accountId").equals(userId)) {
                try {
                    // isOpen()在线就发送
                    if (user.isOpen()) {
                        user.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
