package com.cai.tasks;

import com.cai.common.base.BaseTask;
import com.cai.game.AbstractRoom;
import com.cai.match.MatchTable;

public class MatchGameObserverTask extends BaseTask {
	
	private MatchTable table;
	private AbstractRoom room;
	private long accountId;
	private boolean isEnter;
	
	public MatchGameObserverTask(MatchTable table,AbstractRoom room,long accountId,boolean isEnter){
		this.table = table;
		this.room = room;
		this.accountId = accountId;
		this.isEnter = isEnter;
	}

	@Override
	public void execute() {
		table.gameObserver(room,accountId,isEnter);
	}

	@Override
	public String getTaskName() {
		return "MatchGameOverTask";
	}

}
