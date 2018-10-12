package com.cai.core;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Player;
import com.cai.common.util.ThreadUtil;
import com.cai.net.core.ClientHandler;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

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

	public RequestInvoker(RequestWrapper wrapper, ChannelHandlerContext ctx) {
		this.wrapper = wrapper;
		this.context = ctx;
	}

	@SuppressWarnings("unchecked")
	public void run() {

		try {
			long startTime = System.currentTimeMillis();
			MDC.clear();
			Class handlerClaz = wrapper.getHandlerClass();
			if (handlerClaz == null) {
				logger.error("error handlerClaz == null");
			}
			// 转到各个处理器中处理
			ClientHandler handler = (ClientHandler) handlerClaz.newInstance();
			if (handler == null) {
				logger.error("error ClientHandler == null");
			}
			handler.init(wrapper);
			// 所有请求日志
			// MDC.put("logType","all_request");
			// logger.info("{}",wrapper.toString());
			// MDC.remove("logType");

			// 执行
			handler.execute();

			if (wrapper.getRequestType() != RequestType.HEAR) {
//				handler.afterHandlerProcces();
			}

			long hanlderProccesTime = System.currentTimeMillis() - startTime;
			if (hanlderProccesTime > 200) {
				StringBuilder buf = new StringBuilder();
				buf.append("Slowly process [").append(handlerClaz.getSimpleName()).append(", elapsed time : ").append(hanlderProccesTime)
						.append(", request=[").append(wrapper.getRequest()).append("]");

				Request topRequest = wrapper.getTopRequest();
				if (null != topRequest) {
					Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(topRequest.getProxSeesionId());
					buf.append(String.format("playerid:%d,roomid:%d", topRequest.getProxSeesionId(), null != player ? player.getRoom_id() : -1));
				}
				String str = buf.append(Thread.currentThread().getName()+Thread.currentThread().getId()).toString();
				logger.warn(str);
				
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.roomLogicSlow,"", 0L, str,0);
			}
			// if(request.getRequestType() != 1 &&
			// SystemMonitorManager.getInstance().isStart_monitor())
			// {
			// GameMonitorEvent gameMonitorEvent=new GameMonitorEvent();
			// gameMonitorEvent.setGameMonitorEventType((byte)1);
			// gameMonitorEvent.setType(request.getRequestType());
			// gameMonitorEvent.setReciveProccesTime(reciveProccesTime);
			// gameMonitorEvent.setHanlderProccesTime(hanlderProccesTime);
			// SystemMonitorManager.getInstance().addGameMonitorEvent(gameMonitorEvent);
			// }

		} catch (Exception e) {
			logger.error("error,request:" + wrapper.getRequest(), e);
		} finally {
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
