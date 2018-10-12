package com.cai.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.domain.Session;
import com.cai.net.core.ClientHandler;
import com.cai.net.server.GameSocketServer;
import com.cai.service.MongoDBServiceImpl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.Response;

public final class RequestInvoker implements Runnable {
	Logger logger = LoggerFactory.getLogger(RequestInvoker.class);
	
	private RequestWrapper wrapper;
	private ChannelHandlerContext context;

	public RequestInvoker(RequestWrapper wrapper,ChannelHandlerContext ctx) {
		this.wrapper = wrapper;
		this.context = ctx;
	}

	public void run() {
		
		try {
			long startTime = System.currentTimeMillis();
			MDC.clear();
			Class handlerClaz = wrapper.getHandlerClass();
			//转到各个处理器中处理
			ClientHandler handler = (ClientHandler) handlerClaz.newInstance();
			handler.init(wrapper);
//			//所有请求日志
//			MDC.put("logType","all_request");
//			//logger.info("{}",wrapper.toString());
//			MDC.remove("logType");
//			
			//执行
			handler.execute();
			
			if(wrapper.getRequestType() != RequestType.HEAR)
			{
				handler.afterHandlerProcces();
			}
			
			List<ResponseWrapper> responseWrapperList = handler.getResponseList();

			if(responseWrapperList.size()!=0)
			{
				
				
				for(ResponseWrapper responseWrapper : responseWrapperList){
					Response res = responseWrapper.getResponse();
					
					if(SystemConfig.gameDebug==1){
						if(res.getResponseType()!=ResponseType.HEAR)
						System.out.println("转发服Encoder<========="+res.toByteArray().length+"b\n"+res);
					}
					
					
					ChannelFuture wf = this.context.channel().writeAndFlush(res);
					wf.addListener(new ChannelFutureListener()
					{
						public void operationComplete(ChannelFuture future) throws Exception
						{
							if (!future.isSuccess())
							{
								logger.warn("转发服给客户端消息失败: request:" + wrapper.getRequest().getParserForType() + ",response:"+res.getResponseType());
							}
						}
					});
					
					
					//日志
					Session session  = wrapper.getSession();
					Long account_id = null;
					Account account = session.getAccount();
					if(false&&account!=null && wrapper.getTopRequest().getRequestType()!=RequestType.HEAR){
						account_id = session.getAccount().getAccount_id();
						String ip = session.getClientIP();
						StringBuffer buf = new StringBuffer();
//						buf.append(res.toByteArray().length).append("B")
						buf.append("|sessionId:").append(session.getSessionId())
						.append("|accountId:"+account.getAccount_id()).append("|")
						.append("转发服消息|")
						.append(res.toString());
						long v1 = res.getResponseType().getNumber();
						MongoDBServiceImpl.getInstance().response_log(account_id, ELogType.response,buf.toString(), v1, null, ip,account.getRoom_id());
					}
					//
				}
				
			}
			
			long hanlderProccesTime = System.currentTimeMillis() - startTime;
			if(hanlderProccesTime > 2000)
			{
				StringBuilder buf = new StringBuilder();
				buf.append("Slowly process [").append(handlerClaz.getSimpleName()).append(", elapsed time : ").append(hanlderProccesTime).append(", request=[")
				.append(wrapper.getRequest()).append("]");
				logger.warn(buf.toString());
			}
			
			
			//日志,开发测试
			if(false&&wrapper.getTopRequest().getRequestType()!=RequestType.HEAR){
				Session session  = wrapper.getSession();
				Long account_id = null;
				String ip = null;
				Account account = session.getAccount();
				if(account!=null){
					account_id = session.getAccount().getAccount_id();
					ip = session.getClientIP();
					StringBuffer buf = new StringBuffer();
//					buf.append(wrapper.getTopRequest().toByteArray().length).append("B")
					buf.append("|sessionId:").append(session.getSessionId())
					.append("|accountId:"+account.getAccount_id()).append("|")
					.append(wrapper.getTopRequest().toString());
					long v1 = wrapper.getTopRequest().getRequestType().getNumber();
					MongoDBServiceImpl.getInstance().request_log(account_id, ELogType.request,buf.toString(), v1, hanlderProccesTime, ip,account.getRoom_id());
				}
			}
			
			
			
//			if(request.getRequestType() != 1 && SystemMonitorManager.getInstance().isStart_monitor())
//			{
//				GameMonitorEvent gameMonitorEvent=new GameMonitorEvent();
//				gameMonitorEvent.setGameMonitorEventType((byte)1);
//				gameMonitorEvent.setType(request.getRequestType());
//				gameMonitorEvent.setReciveProccesTime(reciveProccesTime);
//				gameMonitorEvent.setHanlderProccesTime(hanlderProccesTime);
//				SystemMonitorManager.getInstance().addGameMonitorEvent(gameMonitorEvent);
//			}
			
		
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("error,request:"+wrapper.getRequest(),e);
		}finally{
			MDC.clear();
		}
		
		
	}

	public RequestWrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(RequestWrapper wrapper) {
		this.wrapper = wrapper;
	}
	

}
