package com.cai.tasks;

import com.cai.coin.CoinService;
import com.cai.coin.CoinTable;
import com.cai.common.base.BaseTask;
import com.cai.game.AbstractRoom;

public class CoinGameOverTask extends BaseTask {
	
	private CoinTable table;
	private AbstractRoom room;
	
	public CoinGameOverTask(AbstractRoom room,CoinTable table){
		this.table = table;
		this.room = room;
	}

	@Override
	public void execute() {
		if(table == null){
			return;
		}
		int coinIndex = table.gameOver(room);
		CoinService.INTANCE().cancelStateTask(coinIndex);
	}

	@Override
	public String getTaskName() {
		return null;
	}

}
