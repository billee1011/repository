package com.cai.core;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.cai.handler.LogicRoomHandler;
import com.cai.net.core.ClientHandler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;

public final class RequestInvoker implements Runnable {
	
	private static Logger logger = Logger.getLogger(RequestInvoker.class);
	
	
	private RequestWrapper wrapper;
	private ChannelHandlerContext context;

	public RequestInvoker(RequestWrapper wrapper,ChannelHandlerContext ctx) {
		this.wrapper = wrapper;
		this.context = ctx;
	}

	@SuppressWarnings("unchecked")
	public void run() {
		
		try {
			long startTime = System.currentTimeMillis();
			MDC.clear();
			Class handlerClaz = wrapper.getHandlerClass();
			if(handlerClaz == null){
				logger.error("error handlerClaz == null");
			}
			//转到各个处理器中处理
			ClientHandler handler = (ClientHandler) handlerClaz.newInstance();
			if(handler == null){
				logger.error("error ClientHandler == null");
			}
			handler.init(wrapper);
			//所有请求日志
//			MDC.put("logType","all_request");
//			logger.info("{}",wrapper.toString());
//			MDC.remove("logType");
			
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
					
					Request.Builder requestBuilder = Request.newBuilder();
					requestBuilder.setRequestType(Request.RequestType.PROXY);
					requestBuilder.setProxId(responseWrapper.getProx_id());
					requestBuilder.setProxSeesionId(responseWrapper.getProx_session_id());
					//requestBuilder.setProxId(value);
					requestBuilder.setExtension(Protocol.response, res);
					if(SystemConfig.gameDebug==1){
						System.out.println("逻辑计算服Encoder<========="+res);
					}
//					if(SysParamDict.getInstance().getIsOpenLog()) {
//						logger.info(res.toString());
//					}
					
					try {
						ChannelFuture wf = this.context.channel().writeAndFlush(requestBuilder.build());
						wf.addListener(new ChannelFutureListener()
						{
							public void operationComplete(ChannelFuture future) throws Exception
							{
								if (!future.isSuccess())
								{
									logger.error("server write response error,request id is: " + wrapper.getRequest());
								}
							}
						});
					} catch (Exception e) {
						logger.error("error",e);
					}
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
