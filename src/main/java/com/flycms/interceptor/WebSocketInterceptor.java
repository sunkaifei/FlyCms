package com.flycms.interceptor;

import java.util.Map;

import javax.servlet.http.HttpSession;

import com.flycms.constant.Const;
import com.flycms.module.user.model.User;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;


/**
 * 此类用来获取登录用户信息并交由websocket管理
 */
public class WebSocketInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse arg1, WebSocketHandler arg2,
			Map<String, Object> attributes) throws Exception {
		// 将ServerHttpRequest转换成request请求相关的类，用来获取request域中的用户信息
		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
			HttpSession session = servletRequest.getServletRequest().getSession(false);
			if (session != null) {
				// 使用userName区分WebSocketHandler，以便定向发送消息
				User user = (User) session.getAttribute(Const.SESSION_USER);
				//System.out.println("=======当前登录用户=======" + user.getUserId());
				attributes.put("accountId", user.getUserId());
				attributes.put("sessionid", session.getId());
			}else {
				return false;
			}
		}
		//System.out.println("连接到我了");
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest arg0, ServerHttpResponse arg1, WebSocketHandler arg2, Exception arg3) {
		// TODO Auto-generated method stub

	}

}
