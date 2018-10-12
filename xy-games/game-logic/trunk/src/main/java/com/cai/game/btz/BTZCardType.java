package com.cai.game.btz;

import com.cai.common.constant.game.BTZConstants;
import com.cai.common.util.FvMask;

public enum BTZCardType {
	SINGLE(1) {
		@Override
		public int getRate(BTZTable table, int[] card_data) {
			if ((card_data[0] == 0x35 && card_data[1] == 0x29) || (card_data[1] == 0x35 && card_data[0] == 0x29)) {
				return 2;
			}
			
			if ((card_data[0] == 0x11 && card_data[1] == 0x29) || (card_data[1] == 0x11 && card_data[0] == 0x29)) {
				return 2;
			}
			// TODO Auto-generated method stub
			return 1;
		}
	},
	DOUBLE(2) {
		@Override
		public int getRate(BTZTable table, int[] card_data) {
			// TODO Auto-generated method stub
			return 3;
		}
	},

	YAO_JI(3) {
		@Override
		public int getRate(BTZTable table, int[] card_data) {
			// TODO Auto-generated method stub
			return 4;
		}
	},
	TWO_EIGHT(4) {
		@Override
		public int getRate(BTZTable table, int[] card_data) {
			// TODO Auto-generated method stub
			return 5;
		}
	},;

	private final int value;

	BTZCardType(int value) {
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
	public abstract int getRate(BTZTable table, int[] card_data);

	public static BTZCardType getType(int[] card_datas, int rule) {
		
		if (card_datas[0] == 0x35 && card_datas[1] == 0x35) {
			return YAO_JI;
		}
		
		if (card_datas[0] == 0x11 && card_datas[1] == 0x11) {
			return YAO_JI;
		}

		if (FvMask.has_any(rule, FvMask.mask(BTZConstants.BTZ_RULE_TWO_EIGHT))) {
			if ((card_datas[0] == 0x22 && card_datas[1] == 0x28 || card_datas[1] == 0x22 && card_datas[0] == 0x28)) {
				return TWO_EIGHT;
			}
		}

		return card_datas[0] == card_datas[1] ? DOUBLE : SINGLE;
	}

}
