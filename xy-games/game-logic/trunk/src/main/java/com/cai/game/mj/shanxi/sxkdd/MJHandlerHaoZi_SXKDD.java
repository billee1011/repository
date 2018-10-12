package com.cai.game.mj.shanxi.sxkdd;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_SXKDD;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GeneralRevomeMiddleCardRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerHaoZi_SXKDD extends AbstractMJHandler<MJTable_SXKDD> {
	protected int _da_dian_card;
	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable_SXKDD table) {
		_da_dian_card = getRandomCard(table);

		if (table.DEBUG_CARDS_MODE) {
			//_da_dian_card = 0x05;
		}
		
		if (table.DEBUG_MAGIC_CARD) {
			// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
			_da_dian_card = table.magic_card_decidor;
			table.DEBUG_MAGIC_CARD = false;
		}

	/*	table._send_card_count++;
		--table.GRR._left_card_count;*/

		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI },
				GameConstants.INVALID_SEAT);
		
		int index = CalculateMasterCard(table, _da_dian_card);
		table._logic.add_magic_card_index(index);
		table.GRR._especial_card_count = 1;
		table.GRR._especial_show_cards[0] = _da_dian_card;

		GameSchedule.put(new GeneralRevomeMiddleCardRunnable(table.getRoom_id(), _banker), 3, TimeUnit.SECONDS);
	}
	
	public int getRandomCard(MJTable_SXKDD table){
		int card = table._repertory_card[RandomUtil.getRandomNumber(table._all_card_len-1)];
		if(table.has_rule(GameConstants_SXKDD.GAME_RULE_MJ_SXKDD_FEN_HAO_ZI)){
			if(card < 0x31){
				return getRandomCard(table);
			}
		}
		return card;
	}
	
	public int CalculateMasterCard(MJTable_SXKDD table, int card) {
		int index = table._logic.switch_to_card_index(card);
		int card_num = table._logic.get_card_value(card);
 		int corlor = table._logic.get_card_color(card);
		int next_index = index;
		if (card_num > 0 && card_num < 9 && corlor < 3) { // 9的循环是
			next_index = index + 1;
		} else if (card_num == 9 && corlor < 3) {
			next_index = index - 8;
		} else if (index >= table._logic.switch_to_card_index(0x31)
				&& index <= table._logic.switch_to_card_index(0x33)) {
//			if (index == table._logic.switch_to_card_index(0x33)) {
//				next_index = index - 1;
//			} else if (index == table._logic.switch_to_card_index(0x31)) {
//				next_index = index + 2;
//			} else {
				next_index = index + 1;
//			}
		}else if (index == table._logic.switch_to_card_index(0x34)) {
			next_index = index - 3;
		}else if (index >= table._logic.switch_to_card_index(0x35)
				&& index < table._logic.switch_to_card_index(0x37)) {
			next_index = index + 1;
		} else if (index == table._logic.switch_to_card_index(0x37)) {
			next_index = index - 2;
		}

		return next_index;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_SXKDD table, int seat_index) {
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
				int_array.addItem(table.changeCard(table.GRR._discard_cards[i][j]));
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._weave_items[i][j].center_card))) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
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
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		table.changeCard(cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[_da_dian_card], seat_index);

		return true;
	}

}
