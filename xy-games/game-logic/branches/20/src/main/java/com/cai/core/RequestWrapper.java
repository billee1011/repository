package com.cai.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.domain.Session;
import com.cai.net.core.ClientHandler;
import com.cai.net.util.RequestClassHandlerBinding;
import com.google.protobuf.GeneratedMessage;

import io.netty.channel.Channel;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

public class RequestWrapper {

	private static Logger logger = LoggerFactory.getLogger(RequestWrapper.class);
	/**
	 * 上级类(源请求)
	 */
	private Request topRequest;
	
	/**
	 * 里面的扩展,真正业务用到的
	 */
	private GeneratedMessage request;

	private Channel channel;

	private long startTime;

	private RequestType requestType;

	private Session session;

	private RequestClassHandlerBinding<ClientHandler> binding;

	public RequestWrapper(Request topRequest, Channel channel, RequestClassHandlerBinding<ClientHandler> binding, Session session) {
		
		this.topRequest = topRequest;
		
		
		if(binding.getFieldDescriptor()==null){
			
			logger.error(requestType.getNumber()+"找不到扩展!!");
		}else{
			this.request = (GeneratedMessage)topRequest.getField(binding.getFieldDescriptor());
		}
		
		if(this.request==null){
			logger.error("RequestWrapper request is null!!!!!!!!!");
		}
		
		this.channel = channel;
		this.binding = binding;
		this.session = session;
		this.startTime = System.currentTimeMillis();
		this.requestType = topRequest.getRequestType();
	}

	public GeneratedMessage getRequest() {
		return request;
	}

	public void setRequest(GeneratedMessage request) {
		this.request = request;
	}

	public RequestType getRequestType() {
		return requestType;
	}

	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Class getHandlerClass() {
		return binding.getHandlerClass();
	}

	public Request getTopRequest() {
		return topRequest;
	}

	public void setTopRequest(Request topRequest) {
		this.topRequest = topRequest;
	}

}
