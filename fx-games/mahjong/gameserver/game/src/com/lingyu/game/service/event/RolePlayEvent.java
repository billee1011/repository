package com.lingyu.game.service.event;

import java.util.ArrayList;
import java.util.List;

import com.lingyu.common.constant.SystemConstant;
import com.lingyu.game.service.mahjong.MahjongEventHandler;

/**
 * 角色打牌事件
 * @author wangning
 * @date 2016年12月23日 上午11:30:14
 */
@Event
public class RolePlayEvent extends AbEvent {
	/** 事件处理顺序 */
	private static List<HandlerWrapper> pipeline = new ArrayList<HandlerWrapper>();

	public void subscribe() {
//		pipeline.add(this.createHandler(SystemConstant.GROUP_PUBLIC, MahjongEventHandler.class));
	}

	@Override
	/* 获取监听管理器列表 */
	protected List<HandlerWrapper> getHandlerPipeline() {
		return pipeline;
	}
	
	
	//
	public static void publish() {
		
	}
}
