package com.cai.game.btz;

import com.cai.common.constant.game.BTZConstants;

public class TTZTable extends BTZTable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TTZTable(int game_rule_index) {
		super(game_rule_index);
	}

	
	@Override
	protected int[] getCard(){
		return BTZConstants.TUO_TONG_ZI;
	}
}
