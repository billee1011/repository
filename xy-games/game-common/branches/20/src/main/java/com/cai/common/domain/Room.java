package com.cai.common.domain;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.google.protobuf.ByteString;

import protobuf.clazz.Protocol.LocationInfor;

/**
 * 房间
 * 
 * @author run
 *
 */
public abstract class Room implements Serializable {

	public Logger logger = LoggerFactory.getLogger(getClass());

	private static final long serialVersionUID = -1070175849016765655L;

	private int room_id;
	
	private int game_id;
	/**
	 * 房间里的人
	 */
	private Player[] _players = { null, null, null, null };// new RoomPlayer[4];

	/**
	 * 创建时间
	 */
	private long create_time;

	/**
	 * 创建人
	 */
	private long room_owner_account_id;

	private String room_owner_name;

	private long _record_id;

	/**
	 * 最后刷新的时间(通信)
	 */
	private long last_flush_time;

	private ReentrantLock roomLock = new ReentrantLock();
	
	/**
	 * 房间类型枚举
	 */
	private RoomType roomType;
	
	public enum RoomType{
		MJ,FLS
	}

	public Room(RoomType roomType) {
		this.roomType = roomType;
		create_time = System.currentTimeMillis();
		last_flush_time = create_time;
	}

	// =================================================================
	abstract public boolean process_release_room();

	abstract public boolean process_flush_time();
	
	/**
	 * 强制结算
	 * 
	 * @return
	 */
	public abstract boolean force_account();
	
	
	/**
	 * 最大数目 手牌
	 * @return
	 */
	public int getMaxCount() {
		if(roomType==RoomType.MJ) {
			return GameConstants.MAX_COUNT;
		}else if(roomType==RoomType.FLS) {
			return GameConstants.MAX_FLS_COUNT;
		}
		return 0;
	}

	public long getCreate_time() {
		return create_time;
	}

	public void setCreate_time(long create_time) {
		this.create_time = create_time;
	}

	public long getRoom_owner_account_id() {
		return room_owner_account_id;
	}

	public void setRoom_owner_account_id(long room_owner_account_id) {
		this.room_owner_account_id = room_owner_account_id;
	}

	public int getRoom_id() {
		return room_id;
	}

	public void setRoom_id(int room_id) {
		this.room_id = room_id;
	}

	public Player[] get_players() {
		return _players;
	}

	public void set_players(Player[] _players) {
		this._players = _players;
	}

	public int getPlayerCount() {
		int count = 0;
		for (Player player : _players) {
			if (player == null)
				continue;
			count++;
		}
		return count;
	}

	public long get_record_id() {
		return _record_id;
	}

	public void set_record_id(long _record_id) {
		this._record_id = _record_id;
	}

	public String getRoom_owner_name() {
		return room_owner_name;
	}

	public void setRoom_owner_name(String room_owner_name) {
		this.room_owner_name = room_owner_name;
	}

	public long getLast_flush_time() {
		return last_flush_time;
	}

	public void setLast_flush_time(long last_flush_time) {
		this.last_flush_time = last_flush_time;
	}

	public ReentrantLock getRoomLock() {
		return roomLock;
	}

	public void setRoomLock(ReentrantLock roomLock) {
		this.roomLock = roomLock;
	}

	/****************************** 以下方法需要在所有的table中改一下 ****************************************************************************/
	/**
	 * @param player
	 * @return
	 */
	public abstract boolean handler_reconnect_room(Player player);

	/**
	 * @param get_seat_index
	 * @param operateCode
	 * @param operateCard
	 * @return
	 */
	public abstract boolean handler_operate_card(int get_seat_index, int operateCode, int operateCard);

	/**
	 * @param account_id
	 * @return
	 */
	public abstract Player get_player(long account_id);

	/**
	 * @param get_seat_index
	 * @param operateCard
	 * @return
	 */
	public abstract boolean handler_player_out_card(int get_seat_index, int operateCard);

	/**
	 * @param get_seat_index
	 * @return
	 */
	public abstract boolean handler_player_be_in_room(int get_seat_index);

	/**
	 * @param get_seat_index
	 * @return
	 */
	public abstract boolean handler_player_ready(int get_seat_index);

	/**
	 * @param player
	 * @param operateCode
	 * @return
	 */
	public abstract boolean handler_release_room(Player player, int operateCode);

	/**
	 * @param player
	 * @return
	 */
	public abstract boolean handler_enter_room(Player player);

	/**
	 * @param player
	 * @param audioChat
	 * @param audioSize
	 * @param audioLen
	 * @return
	 */
	public abstract boolean handler_audio_chat(Player player, ByteString audioChat, int audioSize, float audioLen);

	/**
	 * @param player
	 * @param emjoyId
	 * @return
	 */
	public abstract boolean handler_emjoy_chat(Player player, int emjoyId);

	/**
	 * @param player
	 * @param pao
	 * @param qiang
	 * @return
	 */
	public abstract boolean handler_requst_pao_qiang(Player player, int pao, int qiang);

	/**
	 * @param player
	 * @param locationInfor
	 * @return
	 */
	public abstract boolean handler_requst_location(Player player, LocationInfor locationInfor);

	/**
	 * @param player
	 */
	public abstract boolean handler_player_offline(Player player);

	/**
	 * @param game_type_index
	 * @param game_rule_index
	 * @param game_round
	 */
	public abstract void init_table(int game_type_index, int game_rule_index, int game_round);

	/**
	 * @param player
	 */
	public abstract boolean handler_create_room(Player player);
	
	
	public abstract boolean dispatch_card_data(int cur_player, int type, boolean tail);

	/**
	 * @param _seat_index
	 * @param _reason
	 */
	public abstract boolean handler_game_finish(int _seat_index, int _reason) ;

	/**
	 * @param _seat_index
	 * @param _type
	 * @param _tail
	 */
	public abstract boolean runnable_dispatch_last_card_data(int _seat_index, int _type, boolean _tail);

	/**
	 * @param _seat_index
	 * @param _type
	 */
	public abstract void runnable_remove_out_cards(int _seat_index, int _type) ;

	/**
	 * @param _seat_index
	 * @param _card_count
	 * @param _card_data
	 * @param _send_client
	 */
	public abstract void runnable_add_discard(int _seat_index, int _card_count, int[] _card_data, boolean _send_client);

	/**
	 * 
	 */
	public abstract boolean exe_finish() ;

	public int getGame_id() {
		return game_id;
	}

	public void setGame_id(int game_id) {
		this.game_id = game_id;
	}

	public abstract int getTablePlayerNumber();

	/**
	 * @param player
	 * @param openThree
	 * @return
	 */
	public abstract boolean handler_requst_open_less(Player player, boolean openThree);
}
