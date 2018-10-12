package com.cai.tasks;

import com.cai.common.base.BaseTask;
import com.cai.match.MatchTable;
import com.cai.service.MatchTableService;

public class MatchSendEnterTask extends BaseTask {
	
	private MatchTable table;
	
	public MatchSendEnterTask(MatchTable table){
		this.table = table;
	}

	@Override
	public void execute() {
		if(table == null){
			return;
		}
		boolean result = table.sendEnterMsg();
		if(result){
			MatchTableService.getInstance().sendEnterMessage(table);
		}
	}

	@Override
	public String getTaskName() {
		return "MatchSendEnterTask";
	}

}
