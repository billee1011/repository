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
import com.cai.future.runnable.PDKAutoYinBaoRunnable;
import com.cai.game.pdk.handler.PDKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pdk.PdkRsp.RoomInfoPdk;
import protobuf.clazz.pdk_all.PdkRsp.TableResponse_PDK_Error;



public class PDKHandlerYinBaoOperate_SXSD extends PDKHandlerOutCardOperate<PDK_SXSD_Table> {
	
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] _out_cards_data =  new int[GameConstants.MAX_PDK_COUNT_JD]; // 出牌扑克
	public int  _out_card_count =  0;
	public int _out_type;
	
	public void reset_status(int seat_index,int cards[],int card_count,int is_out){
		_out_card_player = seat_index;
		_out_cards_data =  new int[card_count]; 
		for(int i = 0; i<card_count;i++)
		{
			_out_cards_data[i] = cards[i];
		}
		_out_card_count = card_count;
		_out_type=is_out;
	}
	
	@Override
	public void exe(PDK_SXSD_Table table) {

		if(_out_card_count != 4){
			return ;
		}
		
		if (_out_type != GameConstants.PDK_CT_BOMB_CARD) {
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
		
		if (table._auto_yibao_scheduled != null) {
			table._auto_yibao_scheduled.cancel(false);
			table._auto_yibao_scheduled = null;
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
		table._logic.sort_card_date_list_by_type_SXSD(this._out_cards_data, this._out_card_count, GameConstants.PDK_CT_BOMB_CARD);
		// 保存玩家出牌次数
		table._out_card_times[_out_card_player]++;
		table.GRR._cur_card_type[_out_card_player] = _out_type;
		table._turn_out__player = _out_card_player;
		table._turn_out_card_count = this._out_card_count;
		table._turn_out_card_type = _out_type;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;
		
		int next_player = GameConstants.INVALID_SEAT;
		
		//引爆状态必须先打完所有的炸弹
		if(table._logic.GetAllBomCard_SXSD(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player]) > 0){
			next_player = _out_card_player;
		}else{
			for(int i = 0;i < table.getTablePlayerNumber();i++){
				next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				if(next_player == table.GRR._banker_player){
					//table._handler = table._handler_out_card_operate;
					break;
				}
				if(table._logic.GetAllBomCard_SXSD(table.GRR._cards_data[next_player], 
						table.GRR._card_count[next_player]) > 0){
					break;
				}
			}
			
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
		table.operate_out_card_yinbao(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT);

		// 玩家出完牌
		if (0 == table.GRR._card_count[_out_card_player]) {
			int delay = 1;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		}

		int auto_out_card_dely = 1000;

		int boom_card[] = new int[table.get_hand_card_count_max()];
		if (table._logic.get_auto_boom_card(table.GRR._cards_data[next_player], table.GRR._card_count[next_player], boom_card)) {
			table._auto_yibao_scheduled = GameSchedule.put(
					new PDKAutoYinBaoRunnable(table.getRoom_id(), next_player, table, GameConstants.PDK_CT_BOMB_CARD,boom_card,4),
					auto_out_card_dely, TimeUnit.MILLISECONDS);
			return;
		} else {
			table._current_player = table.GRR._banker_player;
			//重置
			table._turn_out_card_count = 0;
			table._turn_out_card_type = GameConstants.PDK_CT_ERROR;
			for (int i = 0; i < this._out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;

			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_card_type, GameConstants.PDK_CT_ERROR);
			table.operate_out_card(table._current_player, 0, null,GameConstants.PDK_CT_PASS, GameConstants.INVALID_SEAT);
			
			int can_out_card_data[] = new int[table.get_hand_card_count_max()];
			if (table._current_player == table.GRR._banker_player) {
				adjustAutoOutCard(table, table.GRR._banker_player);
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
			return;
		}
	}
	
	// 判断玩家出牌合法
	public int adjust_out_card_right(PDK_SXSD_Table table) {
		int cbCardType = table._logic.GetCardType_SXSD(this._out_cards_data, this._out_card_count, this._out_cards_data);
		if (cbCardType == GameConstants.PDK_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		}
		return cbCardType;
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