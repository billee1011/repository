package com.cai.tasks;

import com.cai.common.base.BaseTask;
import com.cai.match.MatchTable;

public class MatchEnterTimeOutTask extends BaseTask {
	
	private MatchTable table;
	
	public MatchEnterTimeOutTask(MatchTable table){
		this.table = table;
	}

	@Override
	public void execute() {
		table.checkEnterTimeOut();
	}

	@Override
	public String getTaskName() {
		return "MatchEnterTimeOutTask";
	}

}
