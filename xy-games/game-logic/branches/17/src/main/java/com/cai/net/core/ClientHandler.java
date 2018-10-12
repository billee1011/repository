package com.cai.net.core;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.cai.core.RequestWrapper;
import com.cai.core.ResponseWrapper;
import com.cai.domain.Session;
import com.cai.service.AbstractService;
import com.cai.service.ServiceManager;
import com.google.common.collect.Lists;
import com.google.protobuf.GeneratedMessage;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;

public abstract class ClientHandler<T extends GeneratedMessage> {

	public Logger logger = LoggerFactory.getLogger(getClass());
	
	protected T request;
	
	protected List<ResponseWrapper> responseList;
	
	protected Session session;
	
	protected boolean isRealTime = false;

	protected static final String RESPONSE_TYPE = "responseType";

	protected static final String REQUEST_TYPE = "requestType";
	
	protected Request topRequest;
	
	
	public void init(RequestWrapper _request) throws Exception
	{
		this.request = (T) _request.getRequest();
		this.session = _request.getSession();
		this.responseList = Lists.newArrayList();
		this.topRequest = _request.getTopRequest();
		inLog(_request);
	}

	public long getSessionId()
	{
		return this.session.sessionId;
	}

	public boolean isRealTime()
	{
		return isRealTime;
	}

	public void execute() throws Exception
	{
		 final ReentrantLock mainLock = this.session.getMainLock();
			mainLock.lock();
		 try 
		 {
			 onRequest();
		 } 
		 finally 
		 {
	         mainLock.unlock();
	     }
	}

	public abstract void onRequest() throws Exception;

	/**
	 * 发送回执消息给当前用户的队列
	 * 
	 * @param response
	 * @throws Exception
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public void send(Response response)
	{
		try
		{
			ResponseWrapper responseWrapper = buildResponseWrapper(response,topRequest);
			responseList.add(responseWrapper);
		}
		catch (Exception e)
		{
			logger.error("Error in Send message", e);
		}
	}

	private void outLog(Response response) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	{
		if (!getClass().getSimpleName().equalsIgnoreCase("HeartBeatClientHandler"))
		{
			MDC.put(RESPONSE_TYPE, response.getClass().getSimpleName());
			//logger.debug("<=={}", Bean2MapUtil.convertBean(response));			
		}
	}

	private void inLog(RequestWrapper _request) throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException
	{
		if (_request.getRequestType() != RequestType.HEAR && this.session != null)
		{
			MDC.put(REQUEST_TYPE, _request.getRequest().getClass().getSimpleName());
			//logger.debug("==>{}", Bean2MapUtil.convertBean(_request.getRequest()));			
		}
	}

	public void log(String type, String message) throws IntrospectionException, IllegalAccessException,
			InvocationTargetException
	{
		MDC.put("logType", type);
		MDC.remove(REQUEST_TYPE);
		logger.info("{}", message);
	}

	private ResponseWrapper buildResponseWrapper(Response response,Request topRequest)
	{
		int responseType = response.getResponseType().getNumber();
		ResponseWrapper responseWrapper = new ResponseWrapper(responseType, response,topRequest);
		return responseWrapper;
	}


	public void afterHandlerProcces()
	{
		AbstractService service = ServiceManager.getInstance().getServicesByteOrder(2);// PlayerService
		Object object = service.afterHandlerProcces(session.userID);
		if (service !=null && object != null)
		{
			if (object instanceof List)
			{
				List<Response> list = (List<Response>) object;
				for (Response tbase : list)
				{
					if(tbase != null)
					{
						send((Response) tbase);
					}
					else
					{
						System.out.println(tbase);
					}
				}
			}
			else
			{
				send((Response) object);
			}
		}
	}
	public List<ResponseWrapper> getResponseList()
	{		
		return responseList;
	}
	
	
	
	
}
