package com.cai.game.mj.hubei.huangshi;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangShi;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerXiaoJieSuan_HuangShi extends AbstractMJHandler<Table_HuangShi> {
	@Override
	public void exe(Table_HuangShi table) {
	}

	@Override
	public boolean handler_player_be_in_room(Table_HuangShi table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 设置骰子点数
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
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

		table.send_response_to_player(seat_index, table.saved_room_response);

		return true;
	}
}
