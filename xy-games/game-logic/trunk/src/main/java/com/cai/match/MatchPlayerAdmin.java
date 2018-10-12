package com.cai.match;

public class MatchPlayerAdmin extends MatchPlayer{

	private static final long serialVersionUID = 1L;
	
	private transient boolean isObserver; //是否进入旁观
	private transient int obRoomId; //旁观房间ID
	
	public MatchPlayerAdmin(long adminId){
		setAccount_id(adminId);
	}

	public boolean isObserver() {
		return isObserver;
	}

	public void setObserver(boolean isObserver) {
		this.isObserver = isObserver;
	}

	public int getObRoomId() {
		return obRoomId;
	}

	public void setObRoomId(int obRoomId) {
		this.obRoomId = obRoomId;
	}

}
