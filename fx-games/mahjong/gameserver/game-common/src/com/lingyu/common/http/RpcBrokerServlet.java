package com.lingyu.common.http;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lingyu.common.core.ServiceException;
import com.lingyu.msg.http.HttpMsg;

public class RpcBrokerServlet {
	private static final Logger logger = LogManager.getLogger(RpcBrokerServlet.class);

	public static RpcBrokerServlet getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder {
		private static final RpcBrokerServlet INSTANCE = new RpcBrokerServlet();
	}

	protected final void doGet(HttpRequest req, FullHttpResponse resp) {

		this.processRequest(req, resp);

	}

	protected final void doPost(HttpRequest req, FullHttpResponse resp) {

		this.processRequest(req, resp);

	}

	protected void processRequest(HttpRequest req, FullHttpResponse resp) {
		
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.getUri());
		logger.debug("uri={}",req.getUri());
		
		Map<String, List<String>> store = queryStringDecoder.parameters();
		List<String> list = store.get("funName");
		if (CollectionUtils.isNotEmpty(list)) {
			String functionName = list.get(0);// 方法名
			RpcBrokerService rpcBrokerService = RpcBrokerService.getInstance();
			MethodWrapper wrapper = rpcBrokerService.matcherMethod(functionName);
			if (wrapper != null) {
				Class<?> clazz = wrapper.getParamClazz();
				Object paramObject = null;
				String param = store.get("param").get(0);
				try {
					paramObject = JSON.parseObject(param, clazz);
				} catch (Exception e) {
					logger.error(String.format("Error processRequest: , fun=%s", functionName), e);

				}
				HttpMsg result = rpcBrokerService.call(wrapper, paramObject);
				logger.info("fun={},param={}", functionName, param);
				String text = JSON.toJSONString(result, SerializerFeature.WriteClassName);
				resp.content().writeBytes(text.getBytes());

			} else {
				throw new ServiceException("current function " + functionName + " is not defined");
			}
		} else {
			logger.warn("请求不合法 url={}", req.getUri());
		}

		// http://192.168.1.21:10086/getdata?funName=test&param={%22name%22:%22bird%22}
		// {funName=[loginname], param=[{"bird",1}]}

	}
}
