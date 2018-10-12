package com.cai.game.hh.handler.yyzhz;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.hh.handler.HHHandlerChiPeng;

public class PHZHandlerChiPeng_YYZHZ extends HHHandlerChiPeng<HHTable_YYZHZ> {

	

	@Override
	public void exe(HHTable_YYZHZ table) {
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		
		// 设置用户
		table._current_player = _seat_index;

		// 效果
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
				| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS | GameConstants.WIK_YWS;
		if (_lou_card == -1 || (eat_type & _action) == 0){
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },1, GameConstants.INVALID_SEAT);
		}else{
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,new long[] { GameConstants.WIK_LOU }, 1, GameConstants.INVALID_SEAT);
		}
		
		if (_type == GameConstants.CHI_PENG_TYPE_OUT_CARD) {
			table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}
		if (_type == GameConstants.CHI_PENG_TYPE_DISPATCH) {
			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT, false);
		}

		// 刷新手牌包括组合
		int cards[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_lai(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);

		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		//if (table._ti_two_long[_seat_index] == false) {
		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		table.operate_player_status();
		
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	@Override
	public boolean handler_operate_card(HHTable_YYZHZ table, int seat_index, int operate_code, int operate_card,
			int lou_pai) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("PHZHandlerChiPeng_YYZHZ:" + operate_code);
			return false;
		}
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
		}
		if (seat_index != _seat_index) {
			table.log_info("PHZHandlerChiPeng_YYZHZ 不是当前玩家操作");
			return false;
		}

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		return true;
	}
	
	
	/***
	 * //用户出牌--吃碰之后的出牌
	 */
	@Override
	public boolean handler_player_out_card(HHTable_YYZHZ table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		// 错误断言
		if (table._logic.is_valid_card(card) == false) {
			table.log_info("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_info("出牌,没到出牌");
			return false;
		}

		// if (card == MJGameConstants.ZZ_MAGIC_CARD &&
		// table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
		// table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
		// table.log_info("癞子牌不能出癞子");
		// return false;
		// }

		// 删除扑克
		if (table._logic.remove_card_by_index_yyzhz(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_info("出牌删除出错");
			return false;
		}

		// 出牌--切换到出牌handler
		table.exe_out_card(_seat_index, card, _action);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HHTable_YYZHZ table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
