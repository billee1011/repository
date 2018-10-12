package com.lingyu.game.service.event;

import java.util.ArrayList;
import java.util.List;

import com.lingyu.game.service.mahjong.MahjongEventHandler;

/**
 * 麻将积分事件
 * @author wangning
 * @date 2016年12月27日 下午5:58:57
 */
@Event
public class ChessJifenEvent extends AbEvent {
	/** 事件处理顺序 */
	private static List<HandlerWrapper> pipeline = new ArrayList<HandlerWrapper>();

	public void subscribe() {
		pipeline.add(this.createHandler(MahjongEventHandler.class));
	}

	@Override
	/* 获取监听管理器列表 */
	protected List<HandlerWrapper> getHandlerPipeline() {
		return pipeline;
	}
	
	// 房间号
	private int roomNum;
	// 标签类型
	private int signType;
	// 减分的索引
	private int loseIndex;
	
	
	public static void publish(long roleId, int roomNum, int signType, int loseIndex) {
		ChessJifenEvent event = new ChessJifenEvent();
		event.roleId = roleId;
		event.roomNum = roomNum;
		event.signType = signType;
		event.loseIndex = loseIndex;
		event.dispatch();
	}

	public int getSignType() {
		return signType;
	}

	public void setSignType(int signType) {
		this.signType = signType;
	}

	public int getRoomNum() {
		return roomNum;
	}

	public void setRoomNum(int roomNum) {
		this.roomNum = roomNum;
	}

	public int getLoseIndex() {
		return loseIndex;
	}

	public void setLoseIndex(int loseIndex) {
		this.loseIndex = loseIndex;
	}
}
