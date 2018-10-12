package com.cai.game.mj.hubei.huangshi;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangShi;
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

public class HandlerGang_HuangShi extends MJHandlerGang<Table_HuangShi> {
	protected int _seat_index;
	protected int _provide_player;
	protected int _center_card;
	protected int _action;
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;
	protected int _type;

	public HandlerGang_HuangShi() {
	}

	@Override
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		_self = self;
		_double = d;
	}

	@Override
	public void exe(Table_HuangShi table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		// 正常抓牌之后，都算过圈了
		table.score_when_abandoned_jie_pao[_seat_index] = 0;

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type) || (GameConstants.GANG_TYPE_ADD_GANG == _type)
				|| (GameConstants.WIK_LAI_ZI_PI_ZI_GANG == _type)) {
			this.exe_gang(table);
			return;
		}

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

	public boolean exe_gang(Table_HuangShi table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.GRR._gang_score[_seat_index].an_gang_count++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

			table.GRR._gang_score[_seat_index].ming_gang_count++;
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

			table.GRR._gang_score[_seat_index].ming_gang_count++;
		}

		if (GameConstants.GANG_TYPE_AN_GANG == _type || GameConstants.GANG_TYPE_JIE_GANG == _type || GameConstants.GANG_TYPE_ADD_GANG == _type) {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;

			if (GameConstants.GANG_TYPE_ADD_GANG != _type) {
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
			}
		}

		table._current_player = _seat_index;

		if (GameConstants.GANG_TYPE_AN_GANG == _type || GameConstants.GANG_TYPE_JIE_GANG == _type || GameConstants.GANG_TYPE_ADD_GANG == _type) {
			table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		}
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		boolean can_fa_cai_gang = table.has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_huangshi(table.GRR._cards_index[_seat_index], cards, can_fa_cai_gang);

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else if (cards[i] == Constants_HuangShi.HONG_ZHONG_CARD) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
			} else if (cards[i] == Constants_HuangShi.FA_CAI_CARD && can_fa_cai_gang) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
			}
		}

		if (GameConstants.WIK_LAI_ZI_PI_ZI_GANG == _type) {
			table.GRR._lai_zi_pi_zi_gang[_seat_index][table.GRR._player_niao_count[_seat_index]++] = _center_card;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

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

		if (GameConstants.GANG_TYPE_AN_GANG == _type)
			table._player_result.an_gang_count[_seat_index]++;
		else if (GameConstants.GANG_TYPE_JIE_GANG == _type)
			table._player_result.ming_gang_count[_seat_index]++;
		else if (GameConstants.GANG_TYPE_ADD_GANG == _type)
			table._player_result.ming_gang_count[_seat_index]++;

		// TODO 注意，对鄂州晃晃而言，cbGangIndex可能会超过4
		// int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		// if (GameConstants.GANG_TYPE_AN_GANG == _type) {
		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// if (i == _seat_index)
		// continue;
		//
		// table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -=
		// GameConstants.CELL_SCORE;
		// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index]
		// +=
		// GameConstants.CELL_SCORE;
		// }
		//
		// table._player_result.an_gang_count[_seat_index]++;
		// } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
		// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index]
		// +=
		// GameConstants.CELL_SCORE;
		// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player]
		// -=
		// GameConstants.CELL_SCORE;
		//
		// table._player_result.ming_gang_count[_seat_index]++;
		// } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
		// int provide_index =
		// table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player;
		//
		// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index]
		// +=
		// GameConstants.CELL_SCORE;
		// table.GRR._gang_score[_seat_index].scores[cbGangIndex][provide_index]
		// -=
		// GameConstants.CELL_SCORE;
		//
		// table._player_result.ming_gang_count[_seat_index]++;
		// }

		int tmp_player_fan_shu = table.get_player_fan_shu(_seat_index);
		if (table.player_fan_shu[_seat_index] != tmp_player_fan_shu) {
			table.operate_player_info();
			table.player_fan_shu[_seat_index] = tmp_player_fan_shu;

			// 每次牌桌上有番变动，都重新获取一次听牌数据
			for (int p = 0; p < table.getTablePlayerNumber(); p++) {
				table._playerStatus[p]._hu_card_count = table.get_ting_card(table._playerStatus[p]._hu_cards, table.GRR._cards_index[p],
						table.GRR._weave_items[p], table.GRR._weave_count[p], p);
				int tmp_ting_cards[] = table._playerStatus[p]._hu_cards;
				int tmp_ting_count = table._playerStatus[p]._hu_card_count;

				if (tmp_ting_count > 0) {
					table.operate_chi_hu_cards(p, tmp_ting_count, tmp_ting_cards);

					// 牌桌上番变动之后，如果有人有听牌数据了，显示‘自动胡牌’按钮
					table.operate_auto_win_card(p, true);
				} else {
					tmp_ting_cards[0] = 0;
					table.operate_chi_hu_cards(p, 1, tmp_ting_cards);
				}
			}
		}

		table.exe_dispatch_card(_seat_index, _type, 0);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_HuangShi table, int seat_index, int operate_code, int operate_card) {
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

		if (operate_code == GameConstants.WIK_CHI_HU) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);

			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
		} else if (operate_code == GameConstants.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_chi_hu()) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
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
			exe_gang(table);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}

			table._cur_banker = target_player;

			table.GRR._chi_hu_card[target_player][0] = target_card;

			table.process_chi_hu_player_operate(target_player, target_card, false);
			table.process_chi_hu_player_score(target_player, _seat_index, _center_card, false);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_HuangShi table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 设置骰子点数
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		// roomResponse.setTarget(seat_index);
		// roomResponse.setScoreType(table.get_player_fan_shu(seat_index));
		// table.send_response_to_other(seat_index, roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		boolean can_fa_cai_gang = table.has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int real_card = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (real_card == Constants_HuangShi.HONG_ZHONG_CARD) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_GANG;
				} else if (real_card == Constants_HuangShi.FA_CAI_CARD && can_fa_cai_gang) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_GANG;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG && table.GRR._weave_items[i][j].public_card == 0
						&& i != seat_index) {
					weaveItem_item.setCenterCard(0);

					for (int x = 0; x < 4; x++) {
						weaveItem_item.addWeaveCard(-1);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

					int[] weave_cards = new int[4];
					int count = table._logic.get_weave_card_huangshi(table.GRR._weave_items[i][j].weave_kind,
							table.GRR._weave_items[i][j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (table._logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}
				}

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data_huangshi(table.GRR._cards_index[seat_index], hand_cards, can_fa_cai_gang);

		for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
			if (table._logic.is_magic_card(hand_cards[j])) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else if (hand_cards[j] == Constants_HuangShi.HONG_ZHONG_CARD) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
			} else if (hand_cards[j] == Constants_HuangShi.FA_CAI_CARD && can_fa_cai_gang) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
			}
			tableResponse.addCardsData(hand_cards[j]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
			table.process_chi_hu_player_operate_reconnect(seat_index, _center_card, false); // 效果
		} else {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}

			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(Table_HuangShi table, int seat_index) {
		return false;
	}
}
