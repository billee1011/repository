package com.cai.game.mj.xtdgk;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_XTDGK;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 杠牌
 * 
 *
 * @author WalkerGeek 
 * date: 2018年4月12日 下午9:28:16 <br/>
 */
public class MJHandlerGang_XTDGK extends MJHandlerGang<MJTable_XTDGK> {
	private boolean isFrist = true;

	@Override
	public void exe(MJTable_XTDGK table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}
		isFrist = true;
		
		// 可以胡了
		table._playerStatus[_seat_index].chi_hu_round_valid();
		//解除过手胡
		table.clear_jie_pao_hu_fan(_seat_index);

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { getEffectByActionType(_type) }, 1, GameConstants.INVALID_SEAT);

		// 暗杆 接杆不能抢
		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);
			return;
		}

		// 检查对这个杠有没有 胡
		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

		if (bAroseAction == false) {
			this.exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}

	}

	/***
	 * 用户操作 --抢杠胡
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_XTDGK table, int seat_index, int operate_code, int operate_card) {
		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		// 没有这个操作动作
		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU)){
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);
		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
			// 记录过胡的时候，牌型的番数，变大了，本圈才能接炮
			table.fan_shu_when_abandoned_jie_pao[seat_index] = table.fan_shu_when_jie_pao_hu[seat_index];
			if(table._player_result.is_bao_hu(seat_index)){
				table.need_clear[seat_index ] = false;
			} 
		}
		
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,new long[] { GameConstants.WIK_NULL }, 1);
			// 胡牌失效
			table.GRR._chi_hu_rights[seat_index].set_valid(false);
			// 这一轮就不能吃胡了没过牌之前都不能胡
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
			table.chang_zhang(seat_index,false);
			table.ISHUVaild(seat_index,operate_card);
			// 效果
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
			/*if(isFrist){
				isFrist = false;
				//添加放炮动画
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[]{GameConstants_XTDGK.CHR_FANG_PAO}, 1,
						GameConstants.INVALID_SEAT);
			}*/
		}
		
		
		
		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		// 修改网络导致吃碰错误 9.26 
		int target_card = _center_card;

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			exe_gang(table);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			
			int jie_pao_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false) || table.getISHU(i)) {
					continue;
				}
				jie_pao_count++;
			}
			
			if (jie_pao_count > 0) {
				//手牌删除
				int card_index = table._logic.switch_to_card_index(_center_card);
				table.GRR._cards_index[_seat_index][card_index]--;
				// 刷新放杠玩家手牌
				int cards[] = new int[GameConstants_XTDGK.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, 0, null);
				
				int zhuang = GameConstants.INVALID_SEAT;
				if (jie_pao_count == table.getTablePlayerNumber()-1) {
					zhuang = _seat_index;
					table.chang_zhang(zhuang,true);
				} 
				
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false) || table.getISHU(i)) {
						continue;
					}
					table.qiang_gang_hu[i] ++;
					
					table.ISHUVaild(i);
					
					table.process_chi_hu_player_operate(i, target_card, false);
					table.process_chi_hu_player_score(i, _seat_index, target_card, false);

					// 记录
					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_seat_index]++;
				}

				int next_player = table.getNextPalyerIndex(_seat_index);
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

			}
			
			
			
			return true;
		}
		}

		return true;

	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	@Override
	protected boolean exe_gang(MJTable_XTDGK table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

			table.zhi_gang_count[_seat_index]++;
			table.dian_gang_count[_provide_player]++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}

			// 及时雨，弯杠不直接杠，不计算分
//			if (_center_card != table._send_card_data) {
//				table.GRR._weave_items[_seat_index][cbWeaveIndex].is_vavild = false;
//			} else {
				table.wan_gang_count[_seat_index]++;
//			}
		}
		
		table.gang_pai_weave_index = cbWeaveIndex;

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].type = _type; // 记录暗杠-明杠-弯杠的类型

		// 回头杠时，提供者不更新，暗杠明杠才更新
		if (GameConstants.GANG_TYPE_ADD_GANG != _type) {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		}

		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == _seat_index)
				continue;

			if (table.getISHU(i))
				continue;

			table.GRR._weave_items[_seat_index][cbWeaveIndex].gang_gei_fen_valid[i] = true;
		}

		table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_GANG_KAI, 0);

		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(MJTable_XTDGK table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			tableResponse.addTrustee(false);// 是否托管
			//比赛场托管状态取玩家当前托管状态
            if(table.is_match()){
            	tableResponse.addTrustee(table.istrustee[i]);// 是否托管
            }
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		table.roomResponseShowHuCard(seat_index);
		return true;
	}
	
	
	public int getEffectByActionType(int type){
		if (type == GameConstants.GANG_TYPE_AN_GANG){
			return GameConstants_XTDGK.WIK_AN_GANG;
		}else if (type == GameConstants.GANG_TYPE_JIE_GANG){
			return GameConstants_XTDGK.WIK_JIE_GANG;
		}else if (type == GameConstants.GANG_TYPE_ADD_GANG){
			return GameConstants_XTDGK.WIK_ADD_GANG;
		}
		return 0;
	}
}
