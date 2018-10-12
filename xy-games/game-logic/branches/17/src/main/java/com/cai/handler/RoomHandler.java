package com.cai.handler;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.mj.MJTable;
import com.cai.net.core.ClientHandler;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.RoomRequest;

/**
 * 房间处理
 * @author run
 *
 */
public class RoomHandler extends ClientHandler<RoomRequest>{
	
	private static Logger logger = Logger.getLogger(RoomHandler.class);

	@Override
	public void onRequest() throws Exception {
		
		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(topRequest.getProxyAccountId());
		if(player==null)
			return;
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(player.getRoom_id());
		if(room==null)
			return;
		
		ReentrantLock lock = room.getRoomLock();
		
		try {
			lock.lock();
			//刷新时间
			room.process_flush_time();
			
			int type = request.getType();
			
			//MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room.getRoom_id());
			
			//table.get_state_machine().on_message(player.getAccount_id(), request);
			
			if(type == MsgConstants.REQUST_PLAYER_READY){
				//准备
				handler_player_ready(room.getRoom_id(),player.getAccount_id(),request);
			}
			else if(type == MsgConstants.REQUST_PLAYER_OUT_CARD){
				handler_player_out_card(room.getRoom_id(),player.getAccount_id(),request);
			}else if(type == MsgConstants.REQUST_PLAYER_OPERATE){
				handler_player_operate_card(room.getRoom_id(),player.getAccount_id(),request);
			}else if(type == MsgConstants.REQUST_PLAYER_RELEASE_ROOM){
				handler_release_room(room.getRoom_id(),player.getAccount_id(),request);
			}else if(type == MsgConstants.REQUST_PLAYER_BE_IN_ROOM){
				handler_requst_player_be_in_room(room.getRoom_id(),player.getAccount_id(),request);
			}else if(type == MsgConstants.REQUST_AUDIO_CHAT){
				handler_requst_audio_chat(room.getRoom_id(),player.getAccount_id(),request);
			}else if(type == MsgConstants.REQUST_EMJOY_CHAT){
				handler_requst_emjoy_chat(room.getRoom_id(),player.getAccount_id(),request);
			}
			
		} finally {
			lock.unlock();
		}
		
		
	}
	
	//准备
	private boolean handler_player_ready(int room_id,long account_id, RoomRequest room_rq){
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		Player player = table.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		return table.handler_player_ready(player.get_seat_index());
	}
	
	
	private boolean handler_requst_player_be_in_room(int room_id,long account_id, RoomRequest room_rq){
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		Player player = table.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		return table.handler_player_be_in_room(player.get_seat_index());
	}
	
	private boolean handler_player_out_card(int room_id,long account_id, RoomRequest room_rq){
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		Player player = table.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		return table.handler_player_out_card(player.get_seat_index(), room_rq.getOperateCard());
	}
	
	private boolean handler_player_operate_card(int room_id,long account_id, RoomRequest room_rq){
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		Player player = table.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		return table.handler_operate_card(player.get_seat_index(), room_rq.getOperateCode(),room_rq.getOperateCard());
	}
	
	private boolean handler_release_room(int room_id,long account_id, RoomRequest room_rq){
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		Player player = table.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		boolean r =  table.handler_release_room(player, room_rq.getOperateCode());
		
//		if(r==true){
//			PlayerServiceImpl.getInstance().quitRoomId(account_id);
//			
//			PlayerServiceImpl.getInstance().getRoomMap().remove(room_id);
//		}
		
		return true;
	}
	
	private boolean handler_requst_audio_chat(int room_id,long account_id, RoomRequest room_rq){
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		Player player = table.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		boolean r =  table.handler_audio_chat(player, room_rq.getAudioChat(),room_rq.getAudioSize(),room_rq.getAudioLen());
		
		
		return true;
	}
	
	private boolean handler_requst_emjoy_chat(int room_id,long account_id, RoomRequest room_rq){
		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		
		if(table==null){
			return false;
		}
		
		Player player = table.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		boolean r =  table.handler_emjoy_chat(player, room_rq.getEmjoyId());
		
		
		return true;
	}
	
	
}
