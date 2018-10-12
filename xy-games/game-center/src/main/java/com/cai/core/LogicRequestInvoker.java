package com.cai.core;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;

public final class LogicRequestInvoker implements Runnable {
	Logger logger = LoggerFactory.getLogger(LogicRequestInvoker.class);
	private ChannelHandlerContext context;
	private int session_id;
	private byte[] array;
	
	public final static AtomicLong inMessages = new AtomicLong();
	
	public LogicRequestInvoker(byte[] array, ChannelHandlerContext context,int session_id) {
		super();
		this.array = array;
		this.context = context;
		this.session_id = session_id;
	}

	public void run() {
//		RequestWrapper request = new RequestWrapper(session_id, array);
//		
//		// TODO 验证什么的在这个线程里写
//		int cmd = request.getCmd();
//		
////		//日志
////		StringBuilder bf = new StringBuilder();
////		bf.append("===>收到逻辑服指令:" + Integer.toHexString(request.getCmd()));
////		bf.append("  详情:"+ToStringBuilder.reflectionToString(request));
////		System.out.println(bf.toString()); 
//		
//		//System.out.println("当前处理逻辑消息数量:" + inMessages.getAndIncrement());
//		
//		if (cmd >= MsgCmdConstant.MSG_GROUP_SYSTEM_MAINTAIN_START && cmd < MsgCmdConstant.MSG_GROUP_SYSTEM_MAINTAIN_END){
//			
//		
//			switch (cmd) {
//			
//			//心跳包
//			case MsgCmdConstant.MSG_HEART_BEATING:{
//				HeartBeatingAckMsg heartBeatingAckMsg = new HeartBeatingAckMsg();
//				heartBeatingAckMsg.msgSessinID=CoderUtil.makeID(context.channel().hashCode());
//				ByteBuf buf = Unpooled.buffer();
//				buf = buf.order(ByteOrder.LITTLE_ENDIAN);
//				
//				buf.markWriterIndex();
//				buf.writeInt(0);
//				buf.writeInt(0);
//				heartBeatingAckMsg.wserializer(buf);
//				int length = buf.writerIndex();
//				buf.resetWriterIndex();
//				buf.writeInt(length-8);
//				buf.writerIndex(length);
//				
//				 byte[] array = new byte[buf.readableBytes()];
//			     buf.getBytes(0, array);
//			     
//				RequestWrapper requestWrapper = new RequestWrapper(context.channel().hashCode(),array);
//				ClientServiceImpl.getInstance().sendMsg(requestWrapper);
//				break;
//			}
//			
//			default:
//				logger.error("收到逻辑服指令：没有发现实现方法:cmd="+Integer.toHexString(cmd));
//				break;
//			}
//		}else{
//			
//			//发送给客户端
//			
//			
//			int channelid =  CoderUtil.getMsgSessiondChannelId(request.getMsg_session_id());
//			Session session = SessionServiceImpl.getInstance().getSession(channelid);
//			if(session!=null){
//				session.getChannel().writeAndFlush(request);
//			}
////			if(TcpGameServerHandler.tmp!=null)
////				TcpGameServerHandler.tmp.channel().writeAndFlush(request);
//		}
		
		
//		

	}
	
	


}
