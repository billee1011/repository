package com.cai.game.mj.handler.henanxc;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.ay.MJHandlerOutCardBaoTing;

/**
 * 报听
 * @author Administrator
 *
 */
public class MJHandlerOutCardBaoTing_XC extends MJHandlerOutCardBaoTing {
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _type;
	
	
	public void reset_status(int seat_index,int card,int type){
		_out_card_player = seat_index;
		_out_card_data = card;
		_type= type;
	}
	
	@Override
	public void exe(MJTable table) {
		// TODO Auto-generated method stub
		for(int i=0; i < GameConstants.GAME_PLAYER; i++){
			table._playerStatus[i].clean_action();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
			//table._playerStatus[i].clean_status();
		}
		
		table.operate_player_action(_out_card_player, true);
		
		//设置为报听状态
		table._playerStatus[_out_card_player].set_card_status(GameConstants.CARD_STATUS_BAO_TING);
		
		
		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;
		
		// 用户切换
		int next_player =  (_out_card_player + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
		table._current_player = next_player;
		
		//效果
		table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_BAO_TING}, 1, GameConstants.INVALID_SEAT);
		
		//刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		
		//刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		for(int j=0; j < hand_card_count; j++){
			if( table._logic.is_magic_card(cards[j])){
				cards[j]+=GameConstants.CARD_ESPECIAL_TYPE_HUN;
			}
		}
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);
		
		
		//出牌
//		table.operate_out_card_bao_ting(_out_card_player,1,new int[]{_out_card_data+GameConstants.CARD_ESPECIAL_TYPE_BAO_TING},GameConstants.OUT_CARD_TYPE_MID,GameConstants.INVALID_SEAT);
		
		//听的牌，保存，下次不用计算
		int ting_count = table._playerStatus[_out_card_player]._hu_out_card_count;
		for(int i=0; i < ting_count; i++){
			int out_card = table._playerStatus[_out_card_player]._hu_out_card_ting[i];
			if(out_card == _out_card_data){
				int tc = table._playerStatus[_out_card_player]._hu_card_count = table._playerStatus[_out_card_player]._hu_out_card_ting_count[i] ;
				for(int j=0; j <tc;j++){
					table._playerStatus[_out_card_player]._hu_cards[j] = table._playerStatus[_out_card_player]._hu_out_cards[i][j];
				}
			}
		}
		//table.operate_chi_hu_cards(_out_card_player, table._playerStatus[_out_card_player]._hu_card_count, table._playerStatus[_out_card_player]._hu_cards);
		//效果
		
		// 引用权位
		ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];

		chr.bao_ting_index=table.GRR._discard_count[_out_card_player];
		chr.bao_ting_card = _out_card_data;
		
		table.exe_add_discard( _out_card_player,  1, new int[]{_out_card_data},false,GameConstants.DELAY_SEND_CARD_DELAY);
		
		
		//发牌
		table.exe_dispatch_card(next_player,GameConstants.WIK_NULL,GameConstants.DELAY_SEND_CARD_DELAY);
		
	}

}
