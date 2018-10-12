package com.cai.tasks;

import com.cai.common.base.BaseTask;
import com.cai.game.AbstractRoom;
import com.cai.match.MatchTable;

public class MatchUpdateRoomInfoTask extends BaseTask {
	
	private MatchTable table;
	private AbstractRoom room;
	
	public MatchUpdateRoomInfoTask(MatchTable table,AbstractRoom room){
		this.table = table;
		this.room = room;
	}

	@Override
	public void execute() {
		table.gameUpdate(room);
	}

	@Override
	public String getTaskName() {
		return "MatchGameOverTask";
	}

}
