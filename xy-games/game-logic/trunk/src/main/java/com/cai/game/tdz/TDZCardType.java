package com.cai.game.tdz;

public enum TDZCardType {

	SINGLE(1) {
		@Override
		public int getRate(TDZTable table, int[] card_data) {
			return 1;
		}
	},
	DOUBLE(2) {
		@Override
		public int getRate(TDZTable table, int[] card_data) {
			return 2;
		}
	},;

	private final int value;

	TDZCardType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}

	/**
	 * 获得赢牌比例
	 * 
	 * @param table
	 * @return
	 */
	public abstract int getRate(TDZTable table, int[] card_data);

	public static TDZCardType getType(int[] card_datas, int rule) {
		return card_datas[0] == card_datas[1] ? DOUBLE : SINGLE;
	}

}
