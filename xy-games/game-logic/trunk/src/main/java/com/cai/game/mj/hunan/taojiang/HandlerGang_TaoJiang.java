package com.cai.game.mj.hunan.taojiang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
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

public class HandlerGang_TaoJiang extends MJHandlerGang<Table_TaoJiang> {

	@Override
	public void exe(Table_TaoJiang table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._current_player = _seat_index;

		table.seat_index_when_win = _seat_index;

		// 正常抓牌之后，都算过圈了
		table.score_when_abandoned_jie_pao[_seat_index] = 0;

		table._provide_card = _center_card;

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.can_qiang_gang[_seat_index] = true;

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		int gang_card_index = table._logic.switch_to_card_index(_center_card);

		int weave_index = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			weave_index = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			weave_index = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			// 明杠需要删除废弃牌堆里别人打出来的那张牌，客户端操作
			table.operate_remove_discard(_provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 对已经落地的牌进行轮询
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int center_card = table.GRR._weave_items[_seat_index][i].center_card;
				if (center_card == _center_card && weave_kind == GameConstants.WIK_PENG) {
					weave_index = i;
					break;
				}
			}
		}

		if (-1 == weave_index) { // 杠牌出错
			table.log_player_error(_seat_index, "杠牌出错");
			return;
		}

		// 处理杠牌的落地牌显示和存储
		table.GRR._weave_items[_seat_index][weave_index].public_card = _p ? 1 : 0;
		table.GRR._weave_items[_seat_index][weave_index].center_card = _center_card;
		table.GRR._weave_items[_seat_index][weave_index].weave_kind = _action;

		if (GameConstants.GANG_TYPE_ADD_GANG != _type) {
			table.GRR._weave_items[_seat_index][weave_index].provide_player = _provide_player;
		}

		table._current_player = _seat_index;

		// 删掉手里杠了那张牌，不管是明杠还是暗杠
		table.GRR._cards_index[_seat_index][gang_card_index] = 0;

		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		// 客户端进行手牌和落地牌刷新
		int[] hand_cards = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], hand_cards);
		// 处理王牌和定王牌
		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
		}

		table.operate_player_cards(_seat_index, hand_card_count, hand_cards, weave_count, weaves);

		// TODO 显示听牌数据
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

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) && !table.has_rule(Constants_TaoJiang.GAME_RULE_AN_GANG_KE_QIANG)) {
			table.exe_gang_xuan_mei(_seat_index, table.get_xuan_mei_count());
			return;
		}

		boolean bAroseAction = false;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			bAroseAction = table.estimate_an_gang_respond(_seat_index, _center_card);
		} else {
			bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);
		}

		if (bAroseAction == false) {
			table.exe_gang_xuan_mei(_seat_index, table.get_xuan_mei_count());
		} else {
			PlayerStatus playerStatus = null;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index) {
					continue;
				}

				playerStatus = table._playerStatus[i];

				if (playerStatus.has_action()) {
					if (table.is_match() || table.isClubMatch() || table.isCoinRoom()) {
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					} else {
						if (table.is_bao_ting[i] || (table.istrustee[i] && table.is_gang_tuo_guan[i])) {
							if (playerStatus.has_chi_hu() || playerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
								table.operate_player_action(i, false);
								table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							} else {
								table.operate_player_action(i, true);
								table.exe_jian_pao_hu(i, GameConstants.WIK_NULL, _center_card);
							}
						} else if (table.istrustee[i]) {
							if (playerStatus.has_chi_hu()) {
								table.operate_player_action(i, true);
								table.exe_jian_pao_hu(i, GameConstants.WIK_CHI_HU, _center_card);
							} else {
								table.operate_player_action(i, true);
								table.exe_jian_pao_hu(i, GameConstants.WIK_NULL, _center_card);
							}
						} else {
							table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							table.operate_player_action(i, false);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_TaoJiang table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_chi_hu()) {
				table._playerStatus[seat_index].chi_hu_round_invalid();

				// 能抢杠胡不胡
				table.can_qiang_gang[_seat_index] = false;
			}
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
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
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

		int target_card = _center_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table.exe_gang_xuan_mei(_seat_index, table.get_xuan_mei_count());

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.card_type_when_win = Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}

			table._cur_banker = target_player;

			table.GRR._chi_hu_card[target_player][0] = target_card;

			table.set_niao_card(target_player);

			table.process_chi_hu_player_operate(target_player, target_card, false);
			table.process_chi_hu_player_score(target_player, _seat_index, _center_card, false);

			if (table.get_da_hu_count(table.GRR._chi_hu_rights[_seat_index]) > 0) {
				table._player_result.da_hu_jie_pao[target_player]++;
				table._player_result.da_hu_dian_pao[_seat_index]++;
			} else {
				table._player_result.xiao_hu_jie_pao[target_player]++;
				table._player_result.xiao_hu_dian_pao[_seat_index]++;
			}

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), table.DELAY_GAME_FINISH,
					TimeUnit.MILLISECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_TaoJiang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 离地王牌还有多少张
		if (table.distance_to_ding_wang_card > 0) {
			roomResponse.setOperateLen(table.distance_to_ding_wang_card);
		}

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table.GRR._discard_cards[i][j] == table.joker_card_1 || table.GRR._discard_cards[i][j] == table.joker_card_2) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
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

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// TODO 显示听牌数据
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	protected boolean exe_gang(Table_TaoJiang table) {
		return true;
	}
}
