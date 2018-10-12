package com.cai.game.schcpdss.handler.dssms;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.schcpdss.handler.SCHCPDSSHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class SCHCPHandlerDaYiPiao_DSSMS extends SCHCPDSSHandler<SCHCPDSSTable_MS> {
	
	private static Logger logger = Logger.getLogger(SCHCPHandlerDaYiPiao_DSSMS.class);

	@Override
	public void exe(SCHCPDSSTable_MS table) {

		table._game_status = GameConstants.GS_MJ_PIAO;// 设置状态
		// TODO Auto-generated method stub
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		roomResponse.setGameStatus(table._game_status);
		table.load_room_info_data(roomResponse);
		roomResponse.setBiaoyan(table._cur_banker);//庄家显示
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if(i == table._cur_banker && table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_PIAO)==1)
			{
				table._playerStatus[i]._is_pao_qiang = true;
				table._player_result.pao[i] = 1;
			}
			else if(i == table._cur_banker && table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_X_PIAO)==1)
				table._player_result.pao[i] = -1;
			else if(i != table._cur_banker && table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_X_PIAO)==1)
				table._player_result.pao[i] = -2;
			else 
				table._player_result.pao[i] = -1;
//			table.istrustee[i]=false;
		}
		table.operate_player_data();
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber()&& table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_PIAO)==1; i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
			roomResponse.setPaoMin(0);
			roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_DSS_MS);
			roomResponse.setPaoDes("最多飘2个");
			table.send_response_to_player(i, roomResponse);
		}
		if(table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_X_PIAO)==1){
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				roomResponse.setTarget(table._cur_banker);
				roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
				roomResponse.setPaoMin(0);
				roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_DSS_MS);
				roomResponse.setPaoDes("最多飘2个");
				table.send_response_to_player(table._cur_banker, roomResponse);
			}
			
		}
	}

	public boolean handler_pao_qiang(SCHCPDSSTable_MS table, int seat_index, int pao, int qiang) {
		if(table._game_status != GameConstants.GS_MJ_PIAO)
			return true;
		table._playerStatus[seat_index]._is_pao_qiang = true;
		
		if (pao < 0 || pao > GameConstants.PAO_MAX_COUNT_DSS_MS) {
			pao=0;
		}
		if(table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_X_PIAO)==1&& seat_index == table._cur_banker){
			if(pao == 0)
			{
				for(int i = 0; i< table.getTablePlayerNumber();i++){
					table._player_result.pao[i] = pao;
					table._player_result.haspiao[seat_index]=1;
					table.operate_player_data();
				}
				table.game_start_cp();
				return true;
			}
			else{
				
				table._player_result.pao[seat_index] = pao;
				table._player_result.haspiao[seat_index]=1;
				table.operate_player_data();
				
				
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
				table.load_room_info_data(roomResponse);
				roomResponse.setBiaoyan(table._cur_banker);//庄家显示
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if(i != table._cur_banker && table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_X_PIAO)==1)
						table._player_result.pao[i] = -1;
				}
				for (int i = 0; i < table.getTablePlayerNumber()&& table.getRuleValue(GameConstants.GAME_RULE_DSS_MS_BANKER_X_PIAO)==1; i++) {
					roomResponse.setTarget(i);
					roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
					if(i == table._cur_banker )
						continue;
					roomResponse.setPaoMin(0);
					roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_DSS_MS);
					roomResponse.setPaoDes("最多飘2个");
					table.send_response_to_player(i, roomResponse);
				}

			}
		}
		int p = table._player_result.pao[seat_index];

		table._player_result.pao[seat_index] = pao;
		table._player_result.haspiao[seat_index]=1;
		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}
		table.game_start_cp();
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(SCHCPDSSTable_MS table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setGameStatus(table._game_status);
		
		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		// 游戏变量
		tableResponse.setBankerPlayer(table._cur_banker);

		// tableResponse.setCurrentPlayer(seat_index);
		// tableResponse.setCellScore(0);

		// 状态变量
		// tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		// tableResponse.setOutCardData(0);
		// tableResponse.setOutCardPlayer(0);

		/**
		 * for (int i = 0; i < MJtable.getTablePlayerNumber(); i++) {
		 * tableResponse.addTrustee(false);// 是否托管 // 剩余牌数
		 * tableResponse.addDiscardCount(table.GRR._discard_count[i]);
		 * Int32ArrayResponse.Builder int_array =
		 * Int32ArrayResponse.newBuilder(); for (int j = 0; j < 55; j++) {
		 * int_array.addItem(table.GRR._discard_cards[i][j]); }
		 * tableResponse.addDiscardCards(int_array);
		 * 
		 * // 组合扑克 tableResponse.addWeaveCount(table.GRR._weave_count[i]);
		 * WeaveItemResponseArrayResponse.Builder weaveItem_array =
		 * WeaveItemResponseArrayResponse.newBuilder(); for (int j = 0; j <
		 * MJGameConstants.MAX_WEAVE; j++) { WeaveItemResponse.Builder
		 * weaveItem_item = WeaveItemResponse.newBuilder();
		 * weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
		 * weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
		 * weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
		 * weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
		 * weaveItem_array.addWeaveItem(weaveItem_item); }
		 * tableResponse.addWeaveItemArray(weaveItem_array);
		 * 
		 * // tableResponse.addWinnerOrder(0);
		 * 
		 * // 牌
		 * tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		 * }
		 * 
		 * // 数据 tableResponse.setSendCardData(0); int hand_cards[] = new
		 * int[MJGameConstants.MAX_COUNT]; int hand_card_count =
		 * table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index],
		 * hand_cards);
		 * 
		 * 
		 * for (int i = 0; i < MJGameConstants.MAX_COUNT; i++) {
		 * tableResponse.addCardsData(hand_cards[i]); }
		 **/
		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// TODO Auto-generated method stub

		this.player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(SCHCPDSSTable_MS table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		roomResponse.setBiaoyan(table._cur_banker);//庄家显示
		table.load_room_info_data(roomResponse);
		// 发送数据
		table.istrustee[seat_index]=false;
		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[i]
		if (table._shang_zhuang_player == GameConstants.INVALID_SEAT) {
			roomResponse.setPaoMin(0);
		} else {
			if (seat_index == table._shang_zhuang_player) {
				roomResponse.setPaoMin(table._player_result.pao[seat_index]);
			} else {
				roomResponse.setPaoMin(0);
			}
		}

		roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_DSS_MS);
		roomResponse.setPaoDes("飘2个");
		table.send_response_to_player(seat_index, roomResponse);

		// table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
