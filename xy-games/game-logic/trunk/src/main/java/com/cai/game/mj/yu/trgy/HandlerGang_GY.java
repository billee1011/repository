package com.cai.game.mj.yu.trgy;

import java.util.concurrent.TimeUnit;

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

public class HandlerGang_GY extends MJHandlerGang<Table_GY> {

	protected int _seat_index;
	protected int _provide_player;
	protected int _center_card;
	protected int _action;
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;
	protected int _type;

	public int get_type() {
		return _type;
	}

	public void set_type(int _type) {
		this._type = _type;
	}

	public HandlerGang_GY() {
	}

	@Override
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		if (GameConstants_MYGY.GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		_self = self;
		_double = d;
	}

	@Override
	public void exe(Table_GY table) {

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants_MYGY.INVALID_VALUE);
		}

		table.player_mo_first[_seat_index] = false;

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.operate_effect_action(_seat_index, GameConstants_MYGY.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants_MYGY.INVALID_SEAT);

		if ((GameConstants_MYGY.GANG_TYPE_AN_GANG == _type) || (GameConstants_MYGY.GANG_TYPE_JIE_GANG == _type)) {
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
				table.change_player_status(i, GameConstants_MYGY.Player_Status_OPR_CARD);
				table.operate_player_action(i, false);
			}
		}
	}

	@Override
	public boolean exe_gang(Table_GY table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		table.re_pao_gang_type = _type;

		if (GameConstants_MYGY.GANG_TYPE_AN_GANG == _type) {
			// 暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			if (table.is_ji_card(_center_card)) {
				for (int i = 0; i < 4; i++)
					table.out_ji_pai[_seat_index][table.out_ji_pai_count[_seat_index]++] = _center_card;
			}
		} else if (GameConstants_MYGY.GANG_TYPE_JIE_GANG == _type) {
			// 别人打的牌

			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			if (table.is_ji_card(_center_card)) {
				if (_center_card == GameConstants_MYGY.BA_TONG_CARD) {
					table.responsibility_ji[table.responsibility_ji_count][1].setLeft(_provide_player);
					table.responsibility_ji[table.responsibility_ji_count++][1].setRight(_seat_index);
					table._player_result.duanmen[_provide_player] = 1;
					table.operate_effect_action(_seat_index, GameConstants_MYGY.EFFECT_ACTION_TYPE_ACTION, 1,
							new long[] { GameConstants_MYGY.WIK_ZE_REN_JI_WU }, 1, GameConstants_MYGY.INVALID_SEAT);
				} else {
					table.responsibility_ji[table.responsibility_ji_count][0].setLeft(_provide_player);
					table.responsibility_ji[table.responsibility_ji_count++][0].setRight(_seat_index);
					table._player_result.haspiao[_provide_player] = 1;
					table.operate_effect_action(_seat_index, GameConstants_MYGY.EFFECT_ACTION_TYPE_ACTION, 1,
							new long[] { GameConstants_MYGY.WIK_ZE_REN_JI }, 1, GameConstants_MYGY.INVALID_SEAT);
				}
				for (int i = 0; i < 4; i++)
					table.out_ji_pai[_seat_index][table.out_ji_pai_count[_seat_index]++] = _center_card;
				table.operate_player_data();
			}
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

		} else if (GameConstants_MYGY.GANG_TYPE_ADD_GANG == _type || _type == GameConstants_MYGY.GAME_TYPE_TONG_GANG) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants_MYGY.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					_provide_player = _seat_index;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}

			if (table.is_ji_card(_center_card)) {
				table.out_ji_pai[_seat_index][table.out_ji_pai_count[_seat_index]++] = _center_card;
			}
		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants_MYGY.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards, _seat_index);

		WeaveItem weaves[] = new WeaveItem[GameConstants_MYGY.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants_MYGY.WEAVE_SHOW_DIRECT;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		table.player_GangScore_type[_seat_index][cbGangIndex] = _type;
		if (GameConstants_MYGY.GANG_TYPE_AN_GANG == _type) {
			int score = GameConstants_MYGY.CELL_SCORE * 3;
			// table.player_show_desc_11[_seat_index][2] += score;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants_MYGY.GANG_TYPE_JIE_GANG == _type) {

			int score = GameConstants_MYGY.CELL_SCORE * 3;

			// table.player_show_desc_11[_seat_index][3] += score;
			// table.player_show_desc_11[_provide_player][3] -= score;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = score;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -score;

			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants_MYGY.GANG_TYPE_ADD_GANG == _type) {// 放碰的人给分
			int score = 3;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				if (_double) {
					table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
					table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
				}
			}
			if (_double)
				table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants_MYGY.GAME_TYPE_TONG_GANG == _type) {// 放碰的人给分
			if (_double)
				table._player_result.ming_gang_count[_seat_index]++;
		}

		// 从后面发一张牌给玩家
		table.exe_dispatch_card(_seat_index, _type, 0);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_GY table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants_MYGY.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants_MYGY.WIK_CHI_HU || operate_code == GameConstants_MYGY.WIK_MUSIT_CHI_HU) {
			if (table.pao_hu_first == -1) {
				table.pao_hu_first = seat_index;
			}
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);

			table.GRR._win_order[seat_index] = 1; // 用来计算和处理吃三比消散

			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, _center_card, false);
			// 客户端播放放炮动画
			table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_MYGY.CHR_FANG_PAO);
		} else if (operate_code == GameConstants_MYGY.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants_MYGY.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_MYGY.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_chi_hu()) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

		int target_player = seat_index;
		@SuppressWarnings("unused")
		int target_action = operate_code;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action);
				}

				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		operate_card = _center_card;

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}
			jie_pao_count++;
		}

		table.show_index_score[seat_index][GameConstants_MYGY.INDEX_1_HT_DIAN_PAO] -= 1;
		if (jie_pao_count == 2) {
			table.show_index_score[seat_index][GameConstants_MYGY.INDEX_1_HT_DIAN_PAO_TWO] -= 1;
		} else if (jie_pao_count == 3) {
			table.show_index_score[seat_index][GameConstants_MYGY.INDEX_1_HT_DIAN_PAO_TWO] -= 1;
			table.show_index_score[seat_index][GameConstants_MYGY.INDEX_1_HT_DIAN_PAO_THREE] -= 1;
		}

		if (jie_pao_count > 0) {

			table.shao[_seat_index] = true; // 烧鸡烧杠咯

			table.exe_select_magic();
			table.process_ji_fen();
			table.process_reponsibility_ji_fen();

			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(operate_card)]--;

			// 连庄玩法 加连庄数
			// if (table.has_rule(GameConstants_MYGY.GAME_RULE_CONTINUE_BANKER)
			// ||
			// table._game_type_index == GameConstants_MYGY.GAME_TYPE_GY_EDG
			// || table._game_type_index == GameConstants_MYGY.GAME_TYPE_GT_SDG)
			// {
			// if (table._cur_banker == table.old_banker) {
			// table.continue_banker_count++;
			// } else {
			// table.continue_banker_count = 0;
			// }
			// }
			if (jie_pao_count > 2) {
				table._cur_banker = _seat_index;
			} else {
				table._cur_banker = table.pao_hu_first;
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				table.GRR._chi_hu_card[i][0] = operate_card;

				table.process_chi_hu_player_score(i, _seat_index, operate_card, true);

				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_seat_index]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants_MYGY.Game_End_NORMAL),
					GameConstants_MYGY.GAME_FINISH_DELAY, TimeUnit.SECONDS);
		} else {
			this.exe_gang(table);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_GY table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

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

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants_MYGY.CARD_ESPECIAL_TYPE_WANG_BA);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants_MYGY.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants_MYGY.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_MYGY.MAX_COUNT];
		table.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards, seat_index);

		for (int i = 0; i < GameConstants_MYGY.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
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

			table.operate_effect_action(_seat_index, GameConstants_MYGY.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}
}
