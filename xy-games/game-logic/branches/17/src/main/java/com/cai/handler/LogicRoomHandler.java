package com.cai.handler;

import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.domain.Player;
import com.cai.mj.MJTable;
import com.cai.net.core.ClientHandler;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * 房间
 * 
 * @author run
 *
 */
public class LogicRoomHandler extends ClientHandler<LogicRoomRequest> {

	/**
	 * 创建房间
	 */
	private static final int CRATE_ROOM = 1;
	
	/**
	 * 加入房间
	 */
	private static final int JOIN_ROOM = 2;
	
	/**
	 * 重连
	 */
	private static final int RESET_CONNECT = 3;
	
	/**
	 * 下线
	 */
	private static final int OFFLINE = 4;

	@Override
	public void onRequest() throws Exception {

		LogicRoomRequest r = request;
		//System.out.println("=====");

		int type = r.getType();
		
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();
		
		int room_id = r.getRoomId();
		
		
		if(type == CRATE_ROOM){
			

			handler_player_create_room(r);

	
		}else if(type == JOIN_ROOM){

			
			MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if(table==null)
				return;
			
			ReentrantLock lock = table.getRoomLock();
			try{
				lock.lock();
				handler_player_enter_room(r);
			}finally{
				lock.unlock();
			}
			
			
		}
		
		else if(type == RESET_CONNECT){
			
			
			MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(r.getRoomId());
			
			//判断房间是否在内存中
			if(table==null){
				//删除
				PlayerServiceImpl.getInstance().delRoomId(r.getRoomId());
				
				// 返回消息
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(3);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.ROOM);
				responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
				send(responseBuilder.build());
				
				return;
			}
			
			ReentrantLock lock = table.getRoomLock();
			try{
				lock.lock();
				handler_player_reconnect_room(r);
			}finally{
				lock.unlock();
			}
			
			
//			System.out.println("重连的房间间:"+ room_id);
//			System.out.println("重连接的玩家信息:" + logicRoomAccountItemRequest);
			
		}
		
		else if(type == OFFLINE){
			long account_id = request.getAccountId();
			//System.out.println("收到下线消息 "+account_id);
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(account_id);
			if(player==null)
				return;
			
			player.setOnline(false);
			//TODO  下线后的操作
			
			room_id = player.getRoom_id();
			MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if(table==null){
				return;
			}
			
			ReentrantLock lock = table.getRoomLock();
			try{
				lock.lock();
				//刷新时间
				table.process_flush_time();
				
				table.handler_player_offline(player);
			}finally{
				lock.unlock();
			}
			
			
		}

	}
	
	
	private boolean handler_player_create_room(LogicRoomRequest r){
		//TODO ...
		
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();
		
		int room_id = r.getRoomId();
		
		
		int game_type_index = room_rq.getGameTypeIndex();
		int game_rule_index = room_rq.getGameRuleIndex();
		int game_round = room_rq.getGameRound();
		
		//测试牌局
		MJTable table = new MJTable();
		table.setRoom_id(r.getRoomId());
		table.setCreate_time(System.currentTimeMillis()/1000L);
		table.setRoom_owner_account_id(logicRoomAccountItemRequest.getAccountId());
		table.setRoom_owner_name(logicRoomAccountItemRequest.getNickName());
		
		
		table.init_table(game_type_index, game_rule_index, game_round);
		//boolean start = 
		
		PlayerServiceImpl.getInstance().getRoomMap().put(table.getRoom_id(), table);

		
		//初始化player,同时放入缓存
		Player player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, room_id, session);
		
		table.handler_create_room(player);
		
		return true;

	}
	
	
	private boolean handler_player_enter_room(LogicRoomRequest r){
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();
		
		int room_id = r.getRoomId();
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		//刷新时间
		table.process_flush_time();
				
		//初始化player
		Player player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, room_id, session);
		
		
		boolean flag =  table.handler_enter_room(player);
		if(flag){
			
			
			
		}

		return false;
		
	}
	
	private boolean handler_player_reconnect_room(LogicRoomRequest r){
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		
		int room_id = r.getRoomId();
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		//刷新时间
		table.process_flush_time();
				
		Player player = table.get_player(logicRoomAccountItemRequest.getAccountId());
		if(player==null){
			return false;
		}
		//重定位转发服的相关位置
		player.setChannel(session.getChannel());
		player.setProxy_index(logicRoomAccountItemRequest.getProxyIndex());
		player.setProxy_session_id(logicRoomAccountItemRequest.getProxySessionId());
		player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
		player.setOnline(true);
		return table.handler_reconnect_room(player);
		
	}

}
