package com.cai.game.mj.jilin.chuangchun;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandleLiang_ChangChun extends AbstractMJHandler<MjTable_ChangChun> {
	protected int _seat_index;
	protected int _center_card;
	protected int _action;
	protected int _type;
	protected int _index;
	

	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, int index, boolean d) {
		_seat_index = seat_index;
		_center_card = center_card;
		_action = action;
		_type = type;
		_index = index;
	}

	@Override
	public void exe(MjTable_ChangChun table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}
		table._current_player = _seat_index;

		exe_linag(table);

	}

	protected boolean exe_linag(MjTable_ChangChun table) {
		boolean is_xuan_feng = false;
		int cbWeaveIndex = -1;
		cbWeaveIndex = table.GRR._weave_count[_seat_index];
		table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		
		for(int i = 0;i < table.m_liangCardResult.cbCardCount;i++){
			if(i == _index){
				if(table.m_liangCardResult.type[i] == GameConstants.GANG_TYPE_XUAN_FENG_DAN){
					is_xuan_feng = true;
				}
				int cbRemovecard[] = new int[4];
				for(int j = 0; j < table.m_liangCardResult.indexcount[i];j++){
					cbRemovecard[j] = table.m_liangCardResult.cbCardData[i][j];
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[j] = table.m_liangCardResult.cbCardData[i][j];
				}
				
				if (!table._logic.remove_cards_by_index(table.GRR._cards_index[_seat_index], cbRemovecard, table.m_liangCardResult.indexcount[i])) {
					table.log_player_error(_seat_index, "亮牌删除出错");
					return false;
				}
				table.GRR._weave_items[_seat_index][cbWeaveIndex].type = table.m_liangCardResult.type[i];
				break;
			}
		}
		
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
			for (int n = 0; n < table.GRR._weave_items[_seat_index][i].weave_card.length; n++) {
				if (table.GRR._weave_items[_seat_index][i].weave_card[n] == 0) {
					continue;
				}
				weaves[i].weave_card[n] = table.GRR._weave_items[_seat_index][i].weave_card[n];
			}
	
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		
		
		PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
		currentPlayerStatus.reset();
		
		//旋风杠需要补牌
		if(is_xuan_feng){
			table.exe_dispatch_card(_seat_index, GameConstants.DISPATCH_CARD_TYPE_GANG, 0);
			return true;
		}
		// 亮
		if (table.GRR._left_card_count > 0 && table.GRR._discard_count[_seat_index] == 0) {
			table.m_liangCardResult.cbCardCount = 0;
			table.m_liangCardResult.ResetData();
			int cbActionMask = table._logic.analyse_liang_card(table.GRR._cards_index[_seat_index], table.m_liangCardResult,
					table.has_rule(GameConstants.GAME_RULE_SAN_FENG_CC), table.has_rule(GameConstants.GAME_RULE_FEI_DAO_CC),
					table.GRR._weave_items[_seat_index],table.GRR._weave_count[_seat_index]);
			if (0 != cbActionMask) {
				currentPlayerStatus.add_action(GameConstants.WIK_LIANG);
				for (int i = 0; i < table.m_liangCardResult.cbCardCount; i++) {
					currentPlayerStatus.add_liang_card_cc(table.m_liangCardResult.cbCardData[i], _seat_index,table.m_liangCardResult.type[i],i);
				}
			}
		}
		
		//补特殊杠
		if((table.GRR._discard_count[_seat_index] == 0 && _seat_index != table.GRR._banker_player) || table.GRR._discard_count[_seat_index] != 0){
			table.m_BuCardResult.ResetData();
			int cbActionMask = table._logic.analyse_teshu_gang_cc(table.GRR._cards_index[_seat_index],table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index],table.m_BuCardResult,table.has_rule(GameConstants.GAME_RULE_FEI_DAO_CC));
			if (0 != cbActionMask) {
				currentPlayerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
				for (int i = 0; i < table.m_BuCardResult.cbCardCount; i++) {
					currentPlayerStatus.add_bu_card_cc(table.m_BuCardResult.cbCardData[i], _seat_index,table.m_BuCardResult.card[i],table.m_BuCardResult.type[i]);
				}
			}
		}
		
		if (currentPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
		

		return false;
	}
	
	@Override
	public boolean handler_player_out_card(MjTable_ChangChun table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (_type == GameConstants.DISPATCH_CARD_TYPE_GANG) {
			table.exe_out_card(_seat_index, card, GameConstants.DISPATCH_CARD_TYPE_GANG);
		} else {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}
	
	@Override
	public boolean handler_operate_card(MjTable_ChangChun table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_LIANG: {
			for (int i = 0; i < table.m_liangCardResult.cbCardCount; i++) {
				//索引
				if (operate_card == i) {
					table.exe_liang(seat_index, seat_index, 0, operate_code, table.m_liangCardResult.type[i], operate_card, false);
					return true;
				}
			}
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MjTable_ChangChun table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setIsGoldRoom(table.is_sys());

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
		tableResponse.setSendCardData(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
	            if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
	                // 癞子
	                int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				for (int n = 0; n < table.GRR._weave_items[i][j].weave_card.length; n++) {
					if (table.GRR._weave_items[i][j].weave_card[n] == 0) {
						continue;
					}
					weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[n]);
				}
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].type);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		//癞子标记
		for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(hand_cards[j])) {
            	hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }
		
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
