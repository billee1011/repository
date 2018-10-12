package com.cai.game.pdk.handler.sxsdpdk;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EGameType;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.PDKAutoOutCardRunnable;
import com.cai.game.ddz.handler.lps2ddz.DDZ_LPS2_Table;
import com.cai.game.pdk.PDKTable;
import com.cai.game.pdk.handler.PDKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pdk.PdkRsp.RoomInfoPdk;
import protobuf.clazz.pdk_all.PdkRsp.TableResponse_PDK_Error;

public class PDKHandlerOutCardOperate_SXSD extends PDKHandlerOutCardOperate<PDK_SXSD_Table> {
	@Override
	public void exe(PDK_SXSD_Table table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			no_out_card(table);
			return;
		}

		table._logic.sort_card_date_list_SXSD(_out_cards_data, _out_card_count);
		int cbCardType = adjust_out_card_right(table);
		if (cbCardType == GameConstants.PDK_CT_ERROR) {
			table.log_info(
					"_out_cards_data:" + Arrays.toString(_out_cards_data) + "_out_card_count:" + _out_card_count);
			table.log_info("desc:" + this._desc);
			return;
		}
		
		if (table._auto_out_card_scheduled != null) {
			table._auto_out_card_scheduled.cancel(false);
			table._auto_out_card_scheduled = null;
		}
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		for (int i = 0; i < table.get_hand_card_count_max(); i++) {
			table.GRR._cur_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
			table.GRR._cur_change_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
		}
		table._logic.sort_card_date_list_by_type_SXSD(this._out_cards_data, this._out_card_count, cbCardType);

		// 保存玩家炸弹个数
		if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
			if(!table.m_yin_bao_stata){
				table._boom_num[_out_card_player]++;
				table._all_boom_num[_out_card_player]++;
				table.send_boom_info();
			}
		}
		
		// 保存上一操作玩家
		table._prev_palyer = _out_card_player;
		table._out_card_player = _out_card_player;
		// 保存该轮出牌信息
		table.GRR._cur_round_pass[_out_card_player] = 0;
		table.GRR._cur_round_count[_out_card_player] = this._out_card_count;
		table._history_out_count[_out_card_player][table._out_card_times[_out_card_player]] = _out_card_count;
		for (int i = 0; i < this._out_card_count; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = this._out_cards_data[i];
			table.GRR._cur_change_round_data[_out_card_player][i] = this._out_cards_data[i];
			// 保存该次出牌数据
			table._turn_out_card_data[i] = this._out_cards_data[i];
			table._history_out_card[_out_card_player][table._out_card_times[_out_card_player]][i] = this._out_cards_data[i];
		}
		// 保存玩家出牌次数
		table._out_card_times[_out_card_player]++;
		table.GRR._cur_card_type[_out_card_player] = cbCardType;
		table._turn_out__player = _out_card_player;
		table._turn_out_card_count = this._out_card_count;
		table._turn_out_card_type = cbCardType;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;


		int next_player = GameConstants.INVALID_SEAT;
		//引爆状态必须先打完所有的炸弹
		if(table.m_yin_bao_stata && table._logic.GetAllBomCard_SXSD(table.GRR._cards_data[_out_card_player], 
				table.GRR._card_count[_out_card_player]) > 0){
			next_player = _out_card_player;
		}else{
			next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		}
		
		// 清空接下去出牌玩家出牌数据
		table._current_player = next_player;
		table.GRR._cur_round_count[table._current_player] = 0;
		table.GRR._cur_round_pass[table._current_player] = 0;
		for (int j = 0; j < this._out_card_count; j++) {
			table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
		}

		if (0 == table.GRR._card_count[_out_card_player]) {
			table._current_player = GameConstants.INVALID_SEAT;
		}
		// 刷新玩家手牌
		table.operate_player_cards();
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT);

		// 玩家出完牌
		if (0 == table.GRR._card_count[_out_card_player]) {
			int delay = 1;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		}

		int auto_out_card_dely = 500;

		int can_out_card_data[] = new int[table.get_hand_card_count_max()];
		if (0 == table._logic.Player_Can_out_card_SXSD(table.GRR._cards_data[next_player],
				table.GRR._card_count[next_player], table._turn_out_card_data, table._turn_out_card_count,
				can_out_card_data)) {

			table._auto_out_card_scheduled = GameSchedule.put(
					new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, GameConstants.PDK_CT_PASS),
					auto_out_card_dely, TimeUnit.MILLISECONDS);
			return;
		} else {
			int card_type = table._logic.adjustAutoOutCard_SXSD(table.GRR._cards_data[next_player],
					table.GRR._card_count[next_player]);
			if (card_type != GameConstants.PDK_CT_ERROR) {
				if (table._logic.CompareCard_SXSD(table._turn_out_card_data, table.GRR._cards_data[next_player],
						table._turn_out_card_count, table.GRR._card_count[next_player])) {
					table._auto_out_card_scheduled = GameSchedule.put(
							new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, card_type),
							auto_out_card_dely, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	@Override
	public boolean handler_player_be_in_room(PDK_SXSD_Table table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_RECONNECT_DATA);

		TableResponse_PDK_Error.Builder tableResponse_pdk = TableResponse_PDK_Error.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_pdk);
		RoomInfoPdk.Builder room_info = table.getRoomInfoPdk();
		tableResponse_pdk.setRoomInfo(room_info);

		tableResponse_pdk.setBankerPlayer(table.GRR._banker_player);
		tableResponse_pdk.setCurrentPlayer(table._current_player);
		tableResponse_pdk.setPrevPlayer(table._prev_palyer);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_pdk.addOutCardsCount(table.GRR._cur_round_count[i]);
			tableResponse_pdk.addPlayerPass(table.GRR._cur_round_pass[i]);
			Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
				if (table.GRR._cur_round_count[i] > 0) {
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
					out_change_cards.addItem(table.GRR._cur_round_data[i][j]);
				}
			}
			if (table.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD_SC)) {
				tableResponse_pdk.addCardCount(table.GRR._card_count[i]);
			} else {
				if (i == seat_index) {
					tableResponse_pdk.addCardCount(table.GRR._card_count[i]);
				} else {
					tableResponse_pdk.addCardCount(-1);
				}
			}
			tableResponse_pdk.addCardType(table.GRR._cur_card_type[i]);
			tableResponse_pdk.addOutCardsData(i, out_cards);
			tableResponse_pdk.addChangeCardsData(out_change_cards);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._card_count[i]; j++) {
				cards_card.addItem(GameConstants.INVALID_CARD);
			}
			tableResponse_pdk.addCardsData(i, cards_card);

		}

		if (table._current_player == seat_index) {
			int can_out_card_data[] = new int[table.get_hand_card_count_max()];
			int can_out_card_count = table._logic.Player_Can_out_card_SXSD(table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player], table._turn_out_card_data, table._turn_out_card_count,
					can_out_card_data);
			for (int i = 0; i < can_out_card_count; i++) {
				tableResponse_pdk.addUserCanOutData(can_out_card_data[i]);
			}
			tableResponse_pdk.setUserCanOutCount(can_out_card_count);
		}

		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		tableResponse_pdk.setCardsData(seat_index, cards_card);
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (table._turn_out_card_count > 0) {
				tableResponse_pdk.addPrCardsData(table._turn_out_card_data[i]);
				tableResponse_pdk.addPrChangeCardsData(table._turn_out_card_data[i]);
			}
		}
		tableResponse_pdk.setPrCardsCount(table._turn_out_card_count);
		tableResponse_pdk.setPrOutCardType(table._turn_out_card_type);
		tableResponse_pdk.setPrOutCardPlayer(table._turn_out__player);
		if (table._turn_out_card_count == 0) {
			tableResponse_pdk.setIsFirstOut(1);
		} else {
			tableResponse_pdk.setIsFirstOut(0);
		}
		if (table.matchId == 0) {
			tableResponse_pdk.setDisplayTime(10);
		} else {
			tableResponse_pdk.setDisplayTime(SysParamServerDict.getInstance()
					.getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(8).getVal1() / 1000);
		}
		tableResponse_pdk.setMagicCard(GameConstants.INVALID_CARD);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_pdk));
		table.send_response_to_player(seat_index, roomResponse);
		
		table.send_boom_info();

		return true;
	}

	// 玩家不出
	public void no_out_card(PDK_SXSD_Table table) {
		int can_out_card_data[] = new int[table.get_hand_card_count_max()];
		if (0 < table._logic.Player_Can_out_card_SXSD(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], table._turn_out_card_data, table._turn_out_card_count,
				can_out_card_data) && table.has_rule(GameConstants.GAME_RULE_YING_PAI_SXSD)) {

			return;
		}
		
		table.GRR._cur_round_count[_out_card_player] = 0;
		table.GRR._cur_round_pass[_out_card_player] = 1;

		table._prev_palyer = _out_card_player;

		if (table._turn_out_card_count == 0) {
			return;
		}
		
		//引爆状态，必须出完所有炸弹
		if(table.m_yin_bao_stata && table._out_card_times[_out_card_player] == 0){
			if(!table.yin_bao(_out_card_player,_out_cards_data,_out_card_count,-1)){
				table.send_error_notify(_out_card_player, 2, "必须先出完所有炸弹！");
				return ;
			}
		}
		
		
		//下家最后一张牌，单张必须出最大
		if (mustmax(table)) {
			table.send_error_notify(_out_card_player, 2, "请出最大牌!");
			return ;
		}
		
		//22比见33
		if(table.has_rule(GameConstants.GAME_RULE_22_TO_33_SXSD)){
			if(table.c22_to_c33(table._turn_out_card_data,table._turn_out_card_count,_out_cards_data,_out_card_count,2,_out_card_player)){
				table.send_error_notify(_out_card_player, 2, "22必见33!");
				return ;
			}
		}
		
		//222比见333
		if(table.has_rule(GameConstants.GAME_RULE_222_TO_333_SXSD)){
			if(table._logic.c222_to_c333(table._turn_out_card_data,table._turn_out_card_count,_out_cards_data,_out_card_count,
					table.GRR._cards_data[_out_card_player],table.GRR._card_count[_out_card_player])){
				table.send_error_notify(_out_card_player, 2, "222必见333!");
				return ;
			}
		}
		
		//小炸弹比见大炸弹
		if(table.has_rule(GameConstants.GAME_RULE_SMALL_TO_BIG_SXSD)){
			int turn_type = table._logic.GetCardType_SXSD(table._turn_out_card_data, table._turn_out_card_count, table._turn_out_card_data);
			if(turn_type == GameConstants.PDK_CT_BOMB_CARD && table._logic.small_to_big(turn_type, -1,table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player],table._turn_out_card_data[0])){
				table.send_error_notify(_out_card_player, 2, "小炸弹必见大炸弹");
				return ;
			}
		}
		
		// 判断下一个玩家
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;
		if (table._current_player == table._out_card_player) {
			// 炸弹分
			int cbCardType = table._logic.GetCardType_SXSD(table._turn_out_card_data, table._turn_out_card_count,
					table._turn_out_card_data);
			if (cbCardType == GameConstants.PDK_CT_ERROR) {
				return;
			}
			if (cbCardType == GameConstants.PDK_CT_BOMB_CARD ) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == table._out_card_player) {
						continue;
					}
					if (table.has_rule(GameConstants.GAME_RULE_AWARD_10_SXSD)) {
						table._bomb_cell_score = 10;
					} else if(table.has_rule(GameConstants.GAME_RULE_AWARD_5_SXSD)){
						table._bomb_cell_score = 5;
					}else if(table.has_rule(GameConstants.GAME_RULE_AWARD_2_SXSD)){
						table._bomb_cell_score = 2;
					}
					table._out_bomb_score[i] -= table._bomb_cell_score;

					table._out_bomb_score[table._out_card_player] += table._bomb_cell_score;
					if (table.matchId == 0) {
						//table._player_result.game_score[i] -= table._bomb_cell_score;
						//table._player_result.game_score[table._out_card_player] += table._bomb_cell_score;
					}

				}
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(table._game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				table.load_player_info_data(roomResponse2);
				table.send_response_to_room(roomResponse2);
			}
			// 出完一圈牌
			table._turn_out_card_count = 0;
			table._turn_out_card_type = GameConstants.PDK_CT_ERROR;
			for (int i = 0; i < this._out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;

			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_card_type, GameConstants.PDK_CT_ERROR);
		}
		table.GRR._cur_round_count[table._current_player] = 0;
		table.GRR._cur_round_pass[table._current_player] = 0;
		for (int j = 0; j < this._out_card_count; j++) {
			table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, _out_cards_data, GameConstants.PDK_CT_PASS,
				GameConstants.INVALID_SEAT);

		if (table._auto_out_card_scheduled != null) {
			table._auto_out_card_scheduled.cancel(false);
			table._auto_out_card_scheduled = null;
		}
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		for (int i = 0; i < table.get_hand_card_count_max(); i++) {
			table.GRR._cur_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
			table.GRR._cur_change_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
		}
		int auto_out_card_dely = 500;
		if (table._current_player == table._out_card_player) {
			adjustAutoOutCard(table, table._out_card_player);
		} else {
			if (0 == table._logic.Player_Can_out_card_SXSD(table.GRR._cards_data[next_player],
					table.GRR._card_count[next_player], table._turn_out_card_data, table._turn_out_card_count,
					can_out_card_data)) {

				table._auto_out_card_scheduled = GameSchedule.put(
						new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, GameConstants.PDK_CT_PASS),
						auto_out_card_dely, TimeUnit.MILLISECONDS);
				return;
			} else {
				int card_type = table._logic.adjustAutoOutCard_SXSD(table.GRR._cards_data[next_player],
						table.GRR._card_count[next_player]);
				if (card_type != GameConstants.PDK_CT_ERROR) {
					if (table._logic.CompareCard_SXSD(table._turn_out_card_data, table.GRR._cards_data[next_player],
							table._turn_out_card_count, table.GRR._card_count[next_player])) {
						table._auto_out_card_scheduled = GameSchedule.put(
								new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, card_type),
								auto_out_card_dely, TimeUnit.MILLISECONDS);
					}
				}
			}
		}
	}

	// 判断玩家出牌合法
	public int adjust_out_card_right(PDK_SXSD_Table table) {
		int cbCardType = table._logic.GetCardType_SXSD(this._out_cards_data, this._out_card_count, this._out_cards_data);
		if (cbCardType == GameConstants.PDK_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		}
		if (table._turn_out_card_count > 0) {
			if (!table._logic.CompareCard_SXSD(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
				return GameConstants.PDK_CT_ERROR;
			}
		}

		// 红桃4必须先出,有4炸必须引爆
		if (table._out_card_times[_out_card_player] == 0 && _out_card_player == table.GRR._banker_player) {
			if (!table.can_frist_out(_out_card_player,_out_cards_data,_out_card_count)) {
				return GameConstants.PDK_CT_ERROR;
			}
		} 
		
		//引爆状态，必须出完所有炸弹
		if(table.m_yin_bao_stata && table._out_card_times[_out_card_player] == 0){
			if(!table.yin_bao(_out_card_player,_out_cards_data,_out_card_count,cbCardType)){
				table.send_error_notify(_out_card_player, 2, "必须先出完所有炸弹！");
				return GameConstants.PDK_CT_ERROR;
			}
		}
		
		//最后一手牌不能出炸弹
		if(table.GRR._card_count[_out_card_player] == 4 && cbCardType == GameConstants.PDK_CT_BOMB_CARD){
			table.send_error_notify(_out_card_player, 2, "最后一手牌不能出炸弹！");
			return GameConstants.PDK_CT_ERROR;
		}
		
		//不是引爆状态下，炸弹不能首出
		if(!table.m_yin_bao_stata && cbCardType == GameConstants.PDK_CT_BOMB_CARD && table._turn_out_card_count == 0){
			table.send_error_notify(_out_card_player, 2, "炸弹不能空投！");
			return GameConstants.PDK_CT_ERROR;
		}
		
		//下家最后一张牌，单张必须出最大
		if (mustmax(table)) {
			table.send_error_notify(_out_card_player, 2, "单张请出最大牌!");
			return GameConstants.PDK_CT_ERROR;
		}
		
		//22比见33
		if(table.has_rule(GameConstants.GAME_RULE_22_TO_33_SXSD)){
			if(table.c22_to_c33(table._turn_out_card_data,table._turn_out_card_count,_out_cards_data,_out_card_count,2,_out_card_player)){
				table.send_error_notify(_out_card_player, 2, "22比见33!");
				return GameConstants.PDK_CT_ERROR;
			}
		}
		
		//222比见333
		if(table.has_rule(GameConstants.GAME_RULE_222_TO_333_SXSD)){
			if(table._logic.c222_to_c333(table._turn_out_card_data,table._turn_out_card_count,_out_cards_data,
					_out_card_count,table.GRR._cards_data[_out_card_player],table.GRR._card_count[_out_card_player])){
				table.send_error_notify(_out_card_player, 2, "222比见333!");
				return GameConstants.PDK_CT_ERROR;
			}
		}
		
		//小炸弹比见大炸弹
		if(table.has_rule(GameConstants.GAME_RULE_SMALL_TO_BIG_SXSD)){
			int turn_type = table._logic.GetCardType_SXSD(table._turn_out_card_data, table._turn_out_card_count, table._turn_out_card_data);
			if(turn_type == GameConstants.PDK_CT_BOMB_CARD && table._logic.small_to_big(turn_type, cbCardType,table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player],table._turn_out_card_data[0])){
				table.send_error_notify(_out_card_player, 2, "小炸弹比见大炸弹");
				return GameConstants.PDK_CT_ERROR;
			}
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		}
		return cbCardType;
	}

	// 必须出最大牌
	public boolean mustmax(PDK_SXSD_Table table) {
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		if (table.GRR._card_count[next_player] == 1 && _out_card_count == 1) {
			return table._logic.fang_zou_bao_pei_SXSD(table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player], _out_cards_data);
		}
		return false;
	}

	// 自动出牌
	public void adjustAutoOutCard(PDK_SXSD_Table table, int seat_index) {
		int auto_out_card_dely = 1;
		if (table.matchId != 0) {
			auto_out_card_dely = 2;
		}
		int card_type = table._logic.adjustAutoOutCard_SXSD(table.GRR._cards_data[seat_index],
				table.GRR._card_count[seat_index]);
		if (card_type != GameConstants.PDK_CT_ERROR) {
			table._auto_out_card_scheduled = GameSchedule.put(
					new PDKAutoOutCardRunnable(table.getRoom_id(), seat_index, table, card_type), auto_out_card_dely,
					TimeUnit.SECONDS);
		}
	}
	
}