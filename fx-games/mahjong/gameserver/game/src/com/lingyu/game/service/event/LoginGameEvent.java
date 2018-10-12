package com.lingyu.game.service.event;

import java.util.ArrayList;
import java.util.List;

import com.lingyu.common.constant.SystemConstant;
import com.lingyu.game.service.mail.MailEventHandler;
import com.lingyu.game.service.role.RoleEventHandler;

/**
 * 创建角色登陆事件
 */
@Event
public class LoginGameEvent extends AbEvent {
	/** 事件处理顺序 */
	private static List<HandlerWrapper> pipeline = new ArrayList<HandlerWrapper>();

	public void subscribe() {
		pipeline.add(this.createHandler(SystemConstant.GROUP_BUS_CACHE, RoleEventHandler.class));
		pipeline.add(this.createHandler(SystemConstant.GROUP_BUS_CACHE, MailEventHandler.class));
	}

	// 事件独有属性

	private String ip;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Override
	/* 获取监听管理器列表 */
	protected List<HandlerWrapper> getHandlerPipeline() {
		return pipeline;
	}

	//
	public static void publish(long roleId, String ip) {
		LoginGameEvent event = new LoginGameEvent();
		event.roleId = roleId;
		event.setIp(ip);
		event.dispatch();
	}
}
