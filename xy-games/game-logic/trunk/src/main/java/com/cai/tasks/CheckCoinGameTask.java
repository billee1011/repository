package com.cai.tasks;

import com.cai.coin.CoinService;
import com.cai.common.base.BaseTask;

public class CheckCoinGameTask extends BaseTask {
	
	private int coinIndex;
	
	public CheckCoinGameTask(int coinIndex){
		this.coinIndex = coinIndex;
	}

	@Override
	public void execute() {
		CoinService.INTANCE().checkGameState(coinIndex);
	}

	@Override
	public String getTaskName() {
		return null;
	}

}
