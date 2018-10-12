package com.cai.tasks;

import com.cai.coin.CoinTable;
import com.cai.common.base.BaseTask;

public class CoinGameStartTask extends BaseTask {
	
	private CoinTable table;
	
	public CoinGameStartTask(CoinTable table){
		this.table = table;
	}

	@Override
	public void execute() {
		table.gameStart();
	}

	@Override
	public String getTaskName() {
		return null;
	}

}
