/**
 * 
 */
package com.cai.game.jdb;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Player;

public class GameRecordInfo {
	public int cur_round;
	public int cards_data[][];
	public int jetton_player[][];
	public int end_score[];
	public int area_status[];
	public int banker_seat;
	public Player player[];
	
	public GameRecordInfo(int max_index,int max_card,int max_area) {
		cards_data = new int[max_index][max_card];
		jetton_player = new int[max_index][max_area];
		for(int i =0 ;i<max_index;i++)
		{
			cards_data[i] = new int[max_card];
			jetton_player[i] = new int[max_area];
		}
		end_score = new int[max_index];
		area_status  = new int[max_index];
		banker_seat = GameConstants.INVALID_SEAT;
		player = new Player[max_index];
			
	}

}
