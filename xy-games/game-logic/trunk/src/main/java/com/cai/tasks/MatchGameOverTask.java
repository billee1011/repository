package com.cai.tasks;

import com.cai.common.base.BaseTask;
import com.cai.game.AbstractRoom;
import com.cai.match.MatchTable;

public class MatchGameOverTask extends BaseTask {
	
	private MatchTable table;
	private AbstractRoom room;
	
	public MatchGameOverTask(MatchTable table,AbstractRoom room){
		this.table = table;
		this.room = room;
	}

	@Override
	public void execute() {
		table.gameOver(room);
	}

	@Override
	public String getTaskName() {
		return "MatchGameOverTask";
	}

}
