package com.cai.game.mj.shanxi.ningwu;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_SXNW;
import com.cai.common.domain.ChiHuRight;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardBaoTing_ShanXi_NINGWU extends AbstractMJHandler<MJTable_ShanXi_NINGWU> {
	public int _out_card_player = GameConstants.INVALID_SEAT;
	public int _out_card_data = GameConstants.INVALID_VALUE;

	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
	}

	@Override
	public void exe(MJTable_ShanXi_NINGWU table) {

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table.operate_player_action(_out_card_player, true);

		// 设置为报听状态
		table._playerStatus[_out_card_player].set_card_status(GameConstants.CARD_STATUS_BAO_TING);

		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		// 效果
		table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
				new long[] { GameConstants.WIK_BAO_TING }, 1, GameConstants.INVALID_SEAT);

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);

		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		// 这个牌就是报听后要出的那张牌出牌(客户端要做特别的处理，将这张牌扑倒，其余玩家不能吃，碰，杠，胡等操作)
		int _hide_out_card_data = _out_card_data + GameConstants_SXNW.CARD_ESPECIAL_TYPE_HIDE;
		table.operate_out_card(_out_card_player, 1, new int[] { _hide_out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);

		// 判断出牌的人能不能听牌
		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(
				table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player], _out_card_player);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0 && table._playerStatus[_out_card_player].is_bao_ting()) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		table.operate_player_action(_out_card_player, true);

		table.exe_add_discard(_out_card_player, 1,
				new int[] { _out_card_data + GameConstants_SXNW.CARD_ESPECIAL_TYPE_HIDE }, false,
				GameConstants.DELAY_SEND_CARD_DELAY);

		// 引用权位
		ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];
		chr.bao_ting_index = table.GRR._discard_count[_out_card_player];
		chr.bao_ting_card = _out_card_data;

		// 因为你出的这张牌 是安全牌 没人会操作 直接开始发牌
		table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
	}

	// 报听后打的牌都是安全牌 所以不会进这个方法
	@Override
	public boolean handler_operate_card(MJTable_ShanXi_NINGWU table, int seat_index, int operate_code,
			int operate_card) {
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShanXi_NINGWU table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(table._playerStatus[i].is_bao_ting());

			tableResponse.addDiscardCount(table.GRR._discard_count[i]);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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
		int cards[] = new int[GameConstants.MAX_COUNT];
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		// int real_card =
		// _out_card_data+GameConstants_SXNW.CARD_ESPECIAL_TYPE_HIDE;
		// table.operate_out_card(_out_card_player, 1, new int[] { real_card },
		// GameConstants.OUT_CARD_TYPE_XIA_ZI,
		// seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].is_bao_ting()) {
				table._playerStatus[i].set_card_status(GameConstants.CARD_STATUS_BAO_TING);
			}
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}

}
