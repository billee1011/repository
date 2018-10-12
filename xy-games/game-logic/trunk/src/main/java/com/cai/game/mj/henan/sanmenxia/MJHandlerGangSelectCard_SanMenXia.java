package com.cai.game.mj.henan.sanmenxia;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGangSelectCard_SanMenXia extends AbstractMJHandler<MJTable_SanMenXia> {

	private int _seat_index = GameConstants.INVALID_SEAT;

	public void reset_status(int seat_index) {
		_seat_index = seat_index;
	}
	@Override
	public void exe(MJTable_SanMenXia table) {
		
		table._playerStatus[_seat_index]._is_nao_zhuang = false;
		table.operate_player_gang_get_card(_seat_index);
	}

	public boolean handler_selectcard(MJTable_SanMenXia table, int seat_index, int card) {
		if(table.GRR._especial_show_cards[1] != card && table.GRR._especial_show_cards[2] != card){
			return false;
		}
		if(table._playerStatus[seat_index]._is_nao_zhuang){
			return false;
		}
		table.select_gang_card = card;
		
		int gang_total_count = 0;
		
		int newgangcard[] = new int[2];
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            gang_total_count += table.GRR._gang_score[i].gang_count;
        }
        newgangcard[0] = card == table.GRR._especial_show_cards[1] ? table.GRR._especial_show_cards[2] : table.GRR._especial_show_cards[1];
        newgangcard[1] = table._repertory_card[table._all_card_len - 2 - gang_total_count];
        
        if (table.DEBUG_MAGIC_CARD && 2 + gang_total_count == table.hunweizi) {
        	newgangcard[1] = table.magic_card_decidor;
        }
        
		table.GRR._especial_show_cards[1] = newgangcard[0];
		table.GRR._especial_show_cards[2] = newgangcard[1];
		if(table.has_rule(GameConstants.GAME_RULE_SHANG_HUN_SMX) || table.has_rule(GameConstants.GAME_RULE_YUAN_HUN_SMX)){
			//int igang = gang_total_count/2;
			//int iIndex = table.GRR._especial_show_cards[4];
			if(gang_total_count + 2 > table.hunweizi){
				if(card == table.GRR._especial_show_cards[0]){
					table.GRR._especial_show_cards[4] = -2;
				}
			}
			if(gang_total_count >=  (table.GRR._especial_show_cards[4]*2) && table.GRR._especial_show_cards[4] != -2){
				int addindex = ((gang_total_count - table.GRR._especial_show_cards[4]*2) / 2) + 1;
				table.GRR._especial_show_cards[4] += addindex;
				
			}
			/*else{
				table.GRR._especial_show_cards[4] = iIndex - igang;
			}*/
			//table.GRR._especial_show_cards[4] = iIndex - igang;
			//table.GRR._especial_show_cards[4] = gang_total_count < (iIndex*2 - 1) ? table.GRR._especial_show_cards[4] : -1;
			table.GRR._especial_show_cards[5] = gang_total_count;
		}
		table.operate_show_card(table._cur_banker, GameConstants.Show_Gang_Card, 2, newgangcard,GameConstants.INVALID_SEAT);
		
		table._playerStatus[seat_index]._is_nao_zhuang = true;
		table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_GANG_KAI, 0);
		
 		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_SanMenXia table, int seat_index) {
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

		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
		
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}

			}
			tableResponse.addDiscardCards(int_array);

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

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		
		//发杠后拿牌
		if(seat_index == _seat_index)
			table.operate_player_gang_get_card(seat_index);
		
		return true;
	}

}
