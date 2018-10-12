package com.cai.common.domain;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 房间
 * @author run
 *
 */
public abstract class Room implements Serializable{

	private static final long serialVersionUID = -1070175849016765655L;
	
	private int room_id;
	/**
	 * 房间里的人
	 */
	private Player[] _players = {null,null,null,null};//new RoomPlayer[4];
	
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

	
	public Room(){
		create_time = System.currentTimeMillis();
		last_flush_time = create_time;
	}
	
	//=================================================================
	abstract public boolean process_release_room();
	abstract public boolean process_flush_time();
	/**
	 * 强制结算
	 * @return
	 */
	public abstract boolean force_account();
	
	
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
	
	
	
}
