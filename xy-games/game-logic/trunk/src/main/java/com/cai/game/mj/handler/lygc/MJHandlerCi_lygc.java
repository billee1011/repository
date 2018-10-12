package com.cai.game.mj.handler.lygc;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.RevomeCiMiddleCardRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerCi_lygc extends MJHandler {
	protected int _da_dian_card;
	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable table) {
		_da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		if (table.DEBUG_CARDS_MODE) {
			_da_dian_card = 0x27;
		}
		
		if (table.DEBUG_MAGIC_CARD) {
			// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
			_da_dian_card = table.magic_card_decidor;
			table.DEBUG_MAGIC_CARD = false;
		}

		// 不次风规则
		if (table.has_rule(GameConstants.GAME_RULE_HENAN_BUCIFENG)) {
			if (table._logic.get_card_color(_da_dian_card) == 3) {
				int cbCardIndexTemp[] = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				for (int i = 0; i < GameConstants.CARD_COUNT_DAI_FENG_LZ; i++) {
					cbCardIndexTemp[i] = table._repertory_card[i];
				}

				int index = table._all_card_len - table.GRR._left_card_count + 1;
				int change_card = GameConstants.INVALID_SEAT;
				for (int j = index; j < GameConstants.CARD_COUNT_DAI_FENG_LZ; j++) {
					int card = cbCardIndexTemp[j];
					if (table._logic.get_card_color(card) != 3) {
						index = j;
						change_card = card;
						break;
					}
				}

				if (change_card != GameConstants.INVALID_SEAT) {
					cbCardIndexTemp[index] = _da_dian_card;
					cbCardIndexTemp[table._all_card_len - table.GRR._left_card_count] = change_card;

					table._repertory_card = cbCardIndexTemp;
					_da_dian_card = change_card;
				}
			}
		}

		table._send_card_count++;
		--table.GRR._left_card_count;

		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_CI },
				GameConstants.INVALID_SEAT);

		table._logic.add_ci_card(table._logic.switch_to_card_index(_da_dian_card));

		table.GRR._especial_card_count = 1;
		table.GRR._especial_show_cards[0] = _da_dian_card;

		GameSchedule.put(new RevomeCiMiddleCardRunnable(table.getRoom_id(), _banker), 3, TimeUnit.SECONDS);
	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

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
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._discard_cards[i][j]))) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}

			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._weave_items[i][j].center_card))) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_PENG) {
					for (int k = 0; k < table.getTablePlayerNumber(); k++) {
						for (int m = 0; m < table.GRR._discard_count[k]; m++) {
							if (table.GRR._weave_items[i][j].center_card == table.GRR._discard_cards[k][m]) {
								weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
							}
						}
					}
				}

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
				}

				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_ci_card(table._logic.switch_to_card_index(cards[j]))) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[_da_dian_card], seat_index);

		return true;
	}

}
