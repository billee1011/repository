package com.lingyu.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.entity.User;

public class PrivilegeInterceptor extends HandlerInterceptorAdapter {
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		// 处理privilege Annotation，实现方法级权限控制
		HandlerMethod method = (HandlerMethod) handler;
		Privilege privilege = method.getMethodAnnotation(Privilege.class);
		// 如果为空在表示该方法不需要进行权限验证
		if (privilege == null) {
			return true;
		}
		if (privilege.login()) {
			// 3、如果用户已经登录
			User user = SessionUtil.getCurrentUser();
			if (user != null) {
				// 检测是否有这个权限
				int privilegeId = privilege.value();
				if (privilegeId > 0) {
					boolean isAuthorize = user.isAuthorize(privilege.value());
					if (isAuthorize) {
						return true;
					} else {
						// TODO 指向无权的页面
						// response.sendRedirect(request.getContextPath() +
						// "/business/nopermission.html");
						response.sendRedirect(request.getContextPath() + "/authorize.do");
						return false;
					}
				} else {
					return true;
				}

			} else {
				// 4、非法请求 即这些请求需要登录后才能访问
				// 重定向到登录页面
				response.sendRedirect(request.getContextPath());
				return false;
			}
		}
		return true;
	}
}
