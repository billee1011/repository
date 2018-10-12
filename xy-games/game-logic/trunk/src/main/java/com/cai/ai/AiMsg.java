package com.cai.ai;

public class AiMsg {
	
	private AbstractAi<?> ai;
	private int gameType;
	private int[] gameIds;
	private int[] exceptGameIds;
	private int[] msgIds;
	private int roomType;
	
	public AiMsg(AbstractAi<?> ai,int gameType,int[] gameIds,int[] exceptGameIds,int[] msgIds,int roomType){
		this.ai = ai;
		this.gameType = gameType;
		this.gameIds = gameIds;
		this.exceptGameIds = exceptGameIds;
		this.msgIds = msgIds;
		this.roomType = roomType;
	}

	public AbstractAi<?> getAi() {
		return ai;
	}

	public int getGameType() {
		return gameType;
	}

	public int[] getGameIds() {
		return gameIds;
	}

	public int[] getExceptGameIds() {
		return exceptGameIds;
	}

	public int[] getMsgIds() {
		return msgIds;
	}

	public int getRoomType() {
		return roomType;
	}

	public void setRoomType(int roomType) {
		this.roomType = roomType;
	}

}
