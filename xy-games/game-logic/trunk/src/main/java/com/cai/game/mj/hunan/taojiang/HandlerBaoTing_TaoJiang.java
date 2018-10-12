package com.cai.game.mj.hunan.taojiang;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerBaoTing_TaoJiang extends AbstractMJHandler<Table_TaoJiang> {

	/**
	 * 1 = 庄家起手14张，出牌之前；2 = 庄家起手14张，并打出第一张牌之后；
	 */
	protected int _type;
	protected int _seat_index;
	protected int _card_data;

	/**
	 * 报听的时候，类型是什么，座位好是多少，出的牌是什么
	 * 
	 * @param _seat_index
	 * @param _card_data
	 */
	public void reset_status(int _type, int _seat_index, int _card_data) {
		this._type = _type;
		this._seat_index = _seat_index;
		this._card_data = _card_data;
	}

	@Override
	public void exe(Table_TaoJiang table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_weave();
		}

		boolean bAroseAction = false;

		// 每个能听牌的闲家在客户端弹出‘报听’的界面
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == table.GRR._banker_player) {
				continue;
			}

			// 能听牌的才在客户端弹出‘报听’的界面, 并且地牌张数小于3
			if (table.qi_shou_ting[i] && table.GRR._cards_index[i][table.ding_wang_card_index] < 3) {
				table._playerStatus[i].add_action(GameConstants.WIK_BAO_TING);
				table._playerStatus[i].add_bao_ting(GameConstants.INVALID_VALUE, GameConstants.WIK_BAO_TING, i);

				bAroseAction = true;
			}
		}

		if (bAroseAction == false) {
			if (_type == 1) {
				table.exe_dispatch_card(table._cur_banker, GameConstants.WIK_BAO_TING, 0);
			} else {
				table.exe_out_card(_seat_index, _card_data, GameConstants.WIK_BAO_TING);
			}
		} else {
			// TODO 先注释掉
			// table.operate_effect_action(_seat_index,
			// GameConstants.Effect_Action_Other,
			// 1,
			// new long[] { GameConstants.WIK_BAO_TING }, 2, _seat_index);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				PlayerStatus playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					table.operate_player_action(i, false);
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
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (operate_code == GameConstants.WIK_BAO_TING) {
			// 播报听动画
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BAO_TING }, 1,
					GameConstants.INVALID_SEAT);

			table.is_bao_ting[seat_index] = true;

			// TODO 报听之后，相当于自动托管，摸什么牌打什么牌。直到胡牌
			table.istrustee[seat_index] = true;

			table.operate_player_info();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_action_by_code(GameConstants.WIK_BAO_TING)))
				return false;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		// TODO 先注释掉
		// table.operate_effect_action(_seat_index,
		// GameConstants.EFFECT_ACTION_CACEL_OUT_CARD, 1,
		// new long[] { GameConstants.WIK_BAO_TING }, 2, _seat_index);

		if (_type == 1) {
			table.exe_dispatch_card(table._cur_banker, GameConstants.WIK_BAO_TING, 0);
		} else {
			table.exe_out_card(_seat_index, _card_data, GameConstants.WIK_BAO_TING);
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
		tableResponse.setCurrentPlayer(table.GRR._banker_player);
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

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, table._send_card_data);
		}

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

		// 处理王牌
		int real_card = table._send_card_data;
		if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}

		// 客户端显示玩家抓牌
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

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
}
