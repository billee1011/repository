package com.cai.game.mj.handler.hunanshaoyan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_ShaoYang extends MJHandlerGang<MJTable_ShaoYang> {
	@Override
	public void exe(MJTable_ShaoYang table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].chi_hu_round_valid();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		// WalkerGeek 抢杠胡玩法修改,暗杠不能抢（抢杠玩家胡十三幺除外）
		boolean can_gang = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == _seat_index)
				continue;
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				cbCardIndexTemp[j] = table.GRR._cards_index[i][j];
			}
			// 插入扑克
			if (_center_card != GameConstants.INVALID_VALUE) {
				cbCardIndexTemp[table._logic.switch_to_card_index(_center_card)]++;
			}
			if (table._logic.isShiSanYao(cbCardIndexTemp, table.GRR._weave_items[i], table.GRR._weave_count[i])) {
				can_gang = false;
			}

		}
		if ((GameConstants.GANG_TYPE_AN_GANG == _type) && can_gang) {
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
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}

	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_ShaoYang table, int seat_index, int operate_code, int operate_card) {
		// 抢杠胡

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

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
			table.process_chi_hu_player_operate(seat_index, _center_card, true);
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		// 清理这个玩家状态
		table._playerStatus[seat_index].clean_action();
		table.change_player_status(seat_index, GameConstants.INVALID_VALUE);
		table.operate_player_action(seat_index, true);

		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}
		
		operate_card = _center_card;

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				table._cur_banker = jie_pao_count > 1 ? _seat_index : i;
				table.GRR._chi_hu_card[i][0] = _center_card;
				table.full_pay_player_score(i, _seat_index, _center_card);

				// 记录
				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_provide_player]++;
				if (table.GRR._chi_hu_rights[i].da_hu_count > 0) {
					table._player_result.da_hu_jie_pao[i]++;
					table._player_result.da_hu_dian_pao[_seat_index]++;
				} else {
					table._player_result.xiao_hu_jie_pao[i]++;
					table._player_result.xiao_hu_dian_pao[_seat_index]++;
				}
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		} else {
			// 选择了不胡发牌给杠的玩家
			this.exe_gang(table);
		}

		return true;
	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	@Override
	protected boolean exe_gang(MJTable_ShaoYang table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			// 暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 别人打的牌

			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			// 删掉出来的那张牌
			// table.operate_out_card(_provide_player, 0,
			// null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					// _provide_player =
					// table.GRR._weave_items[_seat_index][i].provide_player;//
					// 找到放碰到人
					_provide_player = _seat_index;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}

		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].type= _type;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			}
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		// 检查听牌
		table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(table._playerStatus[_seat_index]._hu_cards,
				table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}

		///////////////////////////////////////////////////// 算分
		// 杆的人是庄家
		boolean zhuang_gang = (table.GRR._banker_player == _seat_index ? true : false);
		// 是否是庄家放杆
		boolean zhuang_fang_gang = (table.GRR._banker_player == _provide_player ? true : false);

		// 是否庄家加底
		boolean jia_di = table.has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI);
		// 是否杆跑
		boolean gang_pao = table.has_rule(GameConstants.GAME_RULE_HENAN_GANG_PAO);

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				int score = GameConstants.CELL_SCORE;
				score++;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
			}
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = 2;
			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {

			int score = table.getTablePlayerNumber() - 1;

			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = score;// m_pGameServiceOption->lCellScore*2;//配置参数乘
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -score;// -m_pGameServiceOption->lCellScore*2;//配置参数乘
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = score;
			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				int score = GameConstants.CELL_SCORE;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
			}
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = 1;
			table._player_result.ming_gang_count[_seat_index]++;
		}

		// 从后面发一张牌给玩家
		table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_GANG_KAI, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShaoYang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

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
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			// if(_self){
			// if(i == _seat_index){
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
			// }else{
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			// }
			// }else{
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			// }

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;// 将癞子转下
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}
}
