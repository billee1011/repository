package com.cai.ai;

import com.cai.common.base.BaseTask;
import com.cai.game.AbstractRoom;
import protobuf.clazz.Protocol.RoomResponse;

public class AiHandleTask<T extends AbstractRoom> extends BaseTask{
	
	private AbstractAi<T> handler;
	private T t;
	private RobotPlayer player;
	private RoomResponse rsp;
	
	public AiHandleTask(AbstractAi<T> handler,T t,RobotPlayer player,RoomResponse rsp){
		this.handler = handler;
		this.t = t;
		this.player = player;
		this.rsp = rsp;
	}

	@Override
	public void execute() {
		handler.beforeExe(t,player,rsp);
	}

	@Override
	public String getTaskName() {
		return "AiHandleTask";
	}

}
