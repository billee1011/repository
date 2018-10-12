package com.cai.common.domain;

import java.io.Serializable;

public class PrxoyPlayerRoomModel implements Serializable{
	
	/**
	 * 房间id
	 */
	private int room_id;

	/**
	 * 创建者id
	 */
	private long create_account_id;
	
	public int getRoom_id() {
		return room_id;
	}

	public void setRoom_id(int room_id) {
		this.room_id = room_id;
	}

	public long getCreate_account_id() {
		return create_account_id;
	}

	public void setCreate_account_id(long create_account_id) {
		this.create_account_id = create_account_id;
	}
	
	

}
