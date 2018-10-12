package com.lingyu.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class SessionInterceptor extends HandlerInterceptorAdapter {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		// 1、请求到登录页面 放行
		RpcContext.setThreadLocalObjects(request, response);
		return true;
//		if (StringUtils.lastIndexOf(request.getServletPath(), "/") == 0 && request.getServletPath().indexOf("center.do") == -1) {
//			return true;
//		}
//
//		// 2、TODO 比如退出、首页等页面无需登录，即此处要放行 允许游客的请求
//
//		// 3、如果用户已经登录 放行
//		if (request.getSession().getAttribute(Constant.USER_KEY) != null) {
//			// 更好的实现方式的使用cookie
//
//			return true;
//		}
//		// 4、非法请求 即这些请求需要登录后才能访问
//		// 重定向到登录页面
//		response.sendRedirect(request.getContextPath());
	}

	public void postHandle(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse, Object obj, ModelAndView modelandview)
			throws Exception {
		RpcContext.clearThreadLocalObjects();
	}
}
