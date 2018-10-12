package com.cai.coin;

import com.cai.ai.RobotPlayer;

public class CoinPlayer extends RobotPlayer {

	private static final long serialVersionUID = 1L;
	// 是否进入游戏
	private transient boolean isEnter;

	//原始输赢
	private transient int winCoin;
	//最大能输
	private transient int resultWinCoin1;
	//最终输赢
	private transient int resultWinCoin2;

	private transient int baseCoin;
	private transient boolean isBankruptcy;
	private transient boolean isCoinBankruptcy;
	private transient long coinPlayTime;
	private transient long createTime;

	public boolean isAuto() {
		return super.isAuto() || !this.isOnline();
	}

	public boolean isEnter() {
		return isEnter;
	}

	public void setEnter(boolean isEnter) {
		this.isEnter = isEnter;
	}

	public void reset() {
		setChannel(null);
	}

	public int getWinCoin() {
		return winCoin;
	}

	public int setWinCoin(int winCoin) {
		this.winCoin = winCoin;

		long ownCoin = getMoney();
		resultWinCoin1 = winCoin;
		int result = (int) (ownCoin + winCoin);
		if (result < 0) {
			resultWinCoin1 = (int) -ownCoin;
		}
		return resultWinCoin1;
	}

	public int getResultWinCoin1() {
		return resultWinCoin1;
	}

	public int getResultWinCoin2() {
		return resultWinCoin2;
	}

	public void setResultWinCoin2(int resultWinCoin2) {
		this.resultWinCoin2 = resultWinCoin2;
	}

	public long getResultOwnCoin() {
		long result = getMoney() + resultWinCoin2;
		setMoney(result);
		return result;
	}

	public int getBaseCoin() {
		return baseCoin;
	}

	public void setBaseCoin(int baseCoin) {
		this.baseCoin = baseCoin;
	}

	public boolean isBankruptcy() {
		return isBankruptcy;
	}

	public void setBankruptcy(boolean isBankruptcy) {
		this.isBankruptcy = isBankruptcy;
	}

	public boolean isCoinBankruptcy() {
		return isCoinBankruptcy;
	}

	public void setCoinBankruptcy(boolean isCoinBankruptcy) {
		this.isCoinBankruptcy = isCoinBankruptcy;
	}

	public long getCoinPlayTime() {
		return coinPlayTime;
	}

	public void setCoinPlayTime(long coinPlayTime) {
		this.coinPlayTime = coinPlayTime;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

}
