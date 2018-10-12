package com.lingyu.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.facade.RpcBrokerService;
import com.lingyu.admin.manager.OperationLogManager;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.entity.User;
import com.lingyu.common.http.MethodWrapper;

public class SlaveRpcBrokerServlet extends HttpServlet {
	private static final Logger logger = LogManager.getLogger(SlaveRpcBrokerServlet.class);
	private static final long serialVersionUID = 1L;
	private OperationLogManager operationLogManager = AdminServerContext.getBean(OperationLogManager.class);
	private RpcBrokerService rpcBrokerService = RpcBrokerService.getInstance();

	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.processReuqest(request, response);
	}

	protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		this.processReuqest(request, response);

	}

	protected void processReuqest(HttpServletRequest req, HttpServletResponse resp) {
		try {
			RpcContext.setThreadLocalObjects(req, resp);
			try {
				req.setCharacterEncoding("UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage(), e);
			}
			String functionName = req.getParameter("funName");// 方法名
			
			MethodWrapper wrapper = rpcBrokerService.matcherMethod(functionName);
			if (wrapper != null) {
				Class<?> clazz = wrapper.getParamClazz();
				Object paramObject = null;
				String param = req.getParameter("param");
				// 可能有锚点导致URL请求后带上#sadvvsdf这种符号，需要去掉
				param = StringUtils.substringBefore(param, "#");
				// logger.info("param={}", param);
				try {
					paramObject = JSON.parseObject(param, clazz);
				} catch (Exception e) {
					logger.error(String.format("Error processRequest: , fun=%s", functionName), e);

				}
				// 需要登录
				Object result = null;
				User user = SessionUtil.getCurrentUser();
				if (user == null && (!StringUtils.equals(functionName, "login"))) {// 未登录
					
					logger.warn("operation need login:{} ", functionName);
				} else {
					result = rpcBrokerService.call(wrapper, paramObject);
					if (user == null) {
						user = SessionUtil.getCurrentUser();
					}
					logger.info("fun={},userName={}", functionName, user.getName());
//					operationLogManager.createLog(user, functionName, param);

				}
				String text = JSON.toJSONString(result);
				try {
					resp.setCharacterEncoding("UTF-8");
					resp.getWriter().write(text);
					resp.getWriter().flush();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}

			} else {
				throw new ServiceException("current function " + functionName + " is not defined");
			}

		} finally {
			RpcContext.clearThreadLocalObjects();
		}

	}
}
