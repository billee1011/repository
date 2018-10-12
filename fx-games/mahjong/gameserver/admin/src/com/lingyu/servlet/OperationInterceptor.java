package com.lingyu.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.alibaba.fastjson.JSON;
import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.manager.OperationLogManager;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.entity.User;

public class OperationInterceptor extends HandlerInterceptorAdapter {
	private static final Logger logger = LogManager.getLogger(OperationInterceptor.class);
	private OperationLogManager operationLogManager = AdminServerContext.getBean(OperationLogManager.class);

	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String funName = request.getServletPath();
		String param = JSON.toJSONString(request.getParameterMap());
		logger.debug("funcName={},value={}", funName, param);
		return true;
	}

	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		String method = request.getMethod();
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
//			if (handlerMethod.getBean() instanceof ExportFileController) {
//				return;
//			}
//			if (handlerMethod.getBean() instanceof AnnounceController) {
//				return;
//			}
		}
		if (StringUtils.equals(method, RequestMethod.POST.name())) {
			User user = SessionUtil.getCurrentUser();
			if (user != null) {
				String funName = request.getServletPath();
				String param = JSON.toJSONString(request.getParameterMap());
				logger.debug("user={},funcName={},value={}", user.getNickName(), funName, param);
				if (param.getBytes().length <= 1024) {
					operationLogManager.createLog(user, funName, param);
				}
			}
		}
	}

}
