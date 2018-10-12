package com.cai.game.mj.yu.kwx.TwoD.xg;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
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

public class HandlerGang_KWX_XG extends MJHandlerGang<Table_KWX_XG_2D> {

	protected int _seat_index;
	protected int _provide_player;
	protected int _center_card;
	protected int _action;
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;
	protected int _type;

	public HandlerGang_KWX_XG() {
	}

	@Override
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		if (GameConstants_KWX.GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		_self = self;
		_double = d;
	}

	@Override
	public void exe(Table_KWX_XG_2D table) {

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants_KWX.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.operate_effect_action(_seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants_KWX.INVALID_SEAT);

		if ((GameConstants_KWX.GANG_TYPE_AN_GANG == _type) || (GameConstants_KWX.GANG_TYPE_JIE_GANG == _type)) {
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
					table.change_player_status(i, GameConstants_KWX.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	public boolean exe_gang(Table_KWX_XG_2D table) {
		if (_double) {
			table.player_continue_gang[_seat_index]++;
		}

		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants_KWX.GANG_TYPE_AN_GANG == _type) {
			boolean liang_gang = false;
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants_KWX.WIK_LIANG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					liang_gang = true;
					break;
				}
			}

			if (!liang_gang) {
				// 暗杠
				// 设置变量
				cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_count[_seat_index]++;
			}
		} else if (GameConstants_KWX.GANG_TYPE_JIE_GANG == _type) {
			boolean liang_gang = false;
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants_KWX.WIK_LIANG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					liang_gang = true;
					break;
				}
			}

			if (!liang_gang) {
				// 别人打的牌
				cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_count[_seat_index]++;
			}
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants_KWX.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants_KWX.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
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
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card = new int[] { _center_card, _center_card, _center_card, _center_card };

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		WeaveItem weaves[] = new WeaveItem[GameConstants_KWX.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants_KWX.WEAVE_SHOW_DIRECT;
			weaves[i].weave_card = table.GRR._weave_items[_seat_index][i].weave_card;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		if (table.player_liang[_seat_index] == 1) {
			int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[_seat_index], table.GRR._cards_index[_seat_index].length);
			table.liangShowCard(table, _seat_index, 0, temp_cards_index);
		}

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants_KWX.GANG_TYPE_AN_GANG == _type) {
			int score = GameConstants_KWX.CELL_SCORE + GameConstants_KWX.CELL_SCORE;
			if (table.player_continue_gang[_seat_index] > 1) {
				score *= 1 << (table.player_continue_gang[_seat_index] - 1);
			}

			score *= table.get_di_fen();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				table._player_result.biaoyan[i] -= score;
				table._player_result.biaoyan[_seat_index] += score;
				table._player_result.ziba[i] -= score;
				table._player_result.ziba[_seat_index] += score;
				// table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] =
				// -score;// 暗杠，其他玩家扣分
				// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index]
				// += score;// 一共加分
				table._player_result.game_score[i] -= score;
				table._player_result.game_score[_seat_index] += score;
			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants_KWX.GANG_TYPE_JIE_GANG == _type) {

			int score = GameConstants_KWX.CELL_SCORE + GameConstants_KWX.CELL_SCORE;
			if (table.player_continue_gang[_seat_index] > 1) {
				score *= 1 << (table.player_continue_gang[_seat_index] - 1);
			}
			score *= table.get_di_fen();

			table._player_result.biaoyan[_provide_player] -= score;
			table._player_result.biaoyan[_seat_index] += score;
			table._player_result.ziba[_provide_player] -= score;
			table._player_result.ziba[_seat_index] += score;
			table._player_result.game_score[_provide_player] -= score;
			table._player_result.game_score[_seat_index] += score;
			// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index]
			// = score;
			// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player]
			// = -score;

			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants_KWX.GANG_TYPE_ADD_GANG == _type) {// 放碰的人给分
			int score = GameConstants_KWX.CELL_SCORE;
			if (table.player_continue_gang[_seat_index] > 1) {
				score *= 1 << (table.player_continue_gang[_seat_index] - 1);
			}
			score *= table.get_di_fen();

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				table._player_result.biaoyan[i] -= score;
				table._player_result.biaoyan[_seat_index] += score;
				table._player_result.game_score[i] -= score;
				table._player_result.ziba[i] -= score;
				table._player_result.ziba[_seat_index] += score;
				table._player_result.game_score[_seat_index] += score;
				// table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] =
				// -score;// 暗杠，其他玩家扣分
				// table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index]
				// += score;// 一共加分
			}
			table._player_result.ming_gang_count[_seat_index]++;
		}

		table.operate_player_data();
		for (int p = -0; p < table.getTablePlayerNumber(); p++) {
			table._player_result.biaoyan[p] = 0;
		}
		// 从后面发一张牌给玩家
		table.exe_dispatch_card(_seat_index, _type, 0);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_KWX_XG_2D table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants_KWX.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants_KWX.WIK_CHI_HU) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);

			table.GRR._win_order[seat_index] = 1; // 用来计算和处理吃三比的消散

			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
			// 客户端播放放炮动画
			table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_KWX.CHR_FANG_PAO);
		} else if (operate_code == GameConstants_KWX.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_KWX.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_chi_hu()) {
				table.pass_hu_fan[seat_index] = table.pass_hu_fan[seat_index] > table.get_fan(seat_index, _seat_index,
						table.GRR._chi_hu_rights[seat_index], true) ? table.pass_hu_fan[seat_index]
								: table.get_fan(seat_index, _seat_index, table.GRR._chi_hu_rights[seat_index], true);
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

		if (jie_pao_count > 0) {

			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(operate_card)]--;

			// table.set_niao_card(_seat_index, 0, true, 0);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				if (jie_pao_count > 1) {
					table._cur_banker = _seat_index;
				} else {
					table._cur_banker = i;
				}
				if ((_type == GameConstants_KWX.GANG_TYPE_AN_GANG || _type == GameConstants_KWX.GANG_TYPE_ADD_GANG
						|| _type == GameConstants_KWX.GANG_TYPE_JIE_GANG) && table.player_continue_gang[_seat_index] > 1) {
					if (table.player_continue_gang[_seat_index] == 2) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG);
					}
					if (table.player_continue_gang[_seat_index] == 3) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG);
					}
					if (table.player_continue_gang[_seat_index] == 4) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG_GANG);
					}
				}
				table.GRR._chi_hu_card[i][0] = operate_card;

				table.process_chi_hu_player_score(i, _seat_index, operate_card, false);

				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_seat_index]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants_KWX.Game_End_NORMAL), 0, TimeUnit.SECONDS);
		} else {
			this.exe_gang(table);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_KWX_XG_2D table, int seat_index) {
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
				int real_card = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants_KWX.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants_KWX.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants_KWX.WIK_GANG && table.GRR._weave_items[i][j].public_card == 0
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
							weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}
				}

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (table.player_liang[i] == 1) {
				int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[i], table.GRR._cards_index[i].length);
				int hand_card_count = table.liangShowCard(table, i, 0, temp_cards_index);
				tableResponse.addCardCount(hand_card_count);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		table.filterHandCards(seat_index, hand_cards, hand_card_count);
		for (int i = 0; i < hand_card_count; i++) {
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

			table.operate_effect_action(_seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		if (table.player_liang[seat_index] == 1) {
			int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[seat_index], table.GRR._cards_index[seat_index].length);
			table.liangShowCard(table, seat_index, 0, temp_cards_index);
			table.handler_be_in_room_chu_zi(seat_index);
		}
		return true;
	}
}
