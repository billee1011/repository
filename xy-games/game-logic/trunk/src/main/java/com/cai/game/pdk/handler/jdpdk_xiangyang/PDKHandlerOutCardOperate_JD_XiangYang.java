package com.cai.game.pdk.handler.jdpdk_xiangyang;

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
import com.cai.game.pdk.PDKTable;
import com.cai.game.pdk.handler.PDKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pdk.PdkRsp.RoomInfoPdk;
import protobuf.clazz.pdk_xy.PdkRsp.TableResponse_PDK_xy;

public class PDKHandlerOutCardOperate_JD_XiangYang extends PDKHandlerOutCardOperate {
	@Override
	public void exe(PDKTable table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			no_out_card(table);
			return;
		}

		table._logic.sort_card_date_list(_out_cards_data, _out_card_count);
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
		if (table.has_rule(GameConstants.GAME_RULE_FANG_ZOU_BAOPEI)) {
			if (fang_zou_bao_pei(table)) {
				// table._bao_pei_palyer=table._turn_out__player;
			}
		}
		table._logic.sort_card_date_list_by_type(this._out_cards_data, this._out_card_count, cbCardType);

		// 保存玩家炸弹个数
		if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
			table._boom_num[_out_card_player]++;
			table._all_boom_num[_out_card_player]++;
		}

		// 保存上一操作玩家
		table._prev_palyer = _out_card_player;
		table._out_card_player = _out_card_player;
		// 保存该轮出牌信息
		table.GRR._cur_round_pass[_out_card_player] = 0;
		table.GRR._cur_round_count[_out_card_player] = this._out_card_count;
		table._history_out_count[_out_card_player][table._out_card_times[_out_card_player]] = _out_card_count;
		// 保存玩家出牌次数

		for (int i = 0; i < this._out_card_count; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = this._out_cards_data[i];
			table.GRR._cur_change_round_data[_out_card_player][i] = this._out_cards_data[i];
			table._history_out_card[_out_card_player][table._out_card_times[_out_card_player]][i] = this._out_cards_data[i];
			// 保存该次出牌数据
			table._turn_out_card_data[i] = this._out_cards_data[i];
		}
		table.GRR._cur_card_type[_out_card_player] = cbCardType;
		table._turn_out__player = _out_card_player;
		table._turn_out_card_count = this._out_card_count;
		table._turn_out_card_type = cbCardType;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;
		table._out_card_times[_out_card_player]++;
		// 刷新手牌
		int cards[] = new int[table.get_hand_card_count_max()];

		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.get_players()[next_player] != null) {
				break;
			}
			next_player = (next_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		}
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

		if (0 == table.GRR._card_count[_out_card_player]) {
			int delay = 1;
			if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
				// int boom_score=10;
				// if(table._hong_tao_palyer == table._out_card_player &&
				// table.has_rule(GameConstants.GAME_RULE_ZHADAN_ZHAI)){
				// boom_score*=2;AO
				// }
				// for(int i=0;i<table.getTablePlayerNumber();i++){
				// if(i == table._out_card_player){
				// continue;
				// }
				// if(table._hong_tao_palyer == i &&
				// table.has_rule(GameConstants.GAME_RULE_ZHADAN_ZHAIAO)){
				// table._out_bomb_score[i]-=2*boom_score;
				// table._player_result.game_score[i]-=2*boom_score;
				// table._out_bomb_score[table._out_card_player]+=2*boom_score;
				// table._player_result.game_score[table._out_card_player]+=2*boom_score;
				// }else{
				// table._out_bomb_score[i]-=boom_score;
				// table._player_result.game_score[i]-=boom_score;
				// table._out_bomb_score[table._out_card_player]+=boom_score;
				// table._player_result.game_score[table._out_card_player]+=boom_score;
				// }
				// }

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == table._out_card_player) {
						continue;
					}
					if (table.get_players()[i] == null) {
						continue;
					}
					if (table._hong_tao_palyer == i && table.has_rule(GameConstants.GAME_RULE_ZHADAN_ZHAIAO)
							&& table.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
						table._out_bomb_score_zhaniao[i] -= table._bomb_cell_score;
						table._out_bomb_score_zhaniao[table._out_card_player] += table._bomb_cell_score;
					} else {
						if (table._hong_tao_palyer == table._out_card_player
								&& table.has_rule(GameConstants.GAME_RULE_ZHADAN_ZHAIAO)
								&& table.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
							table._out_bomb_score_zhaniao[i] -= table._bomb_cell_score;
							table._out_bomb_score_zhaniao[table._out_card_player] += table._bomb_cell_score;
						}
					}
					table._out_bomb_score[i] -= table._bomb_cell_score;

					table._out_bomb_score[table._out_card_player] += table._bomb_cell_score;
					if (!table.is_match()) {
						table._player_result.game_score[i] -= table._bomb_cell_score;
						table._player_result.game_score[table._out_card_player] += table._bomb_cell_score;
					}

				}

				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(table._game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				table.load_player_info_data(roomResponse2);
				table.send_response_to_room(roomResponse2);

			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		}

		int auto_out_card_dely = 1000;

		if (table.has_rule(GameConstants.GAME_RULE_KUAI_SU)) {
			auto_out_card_dely = 500;
		}

		int can_out_card_data[] = new int[table.get_hand_card_count_max()];
		if (table._logic.Player_Can_out_card(table.GRR._cards_data[next_player], table.GRR._card_count[next_player],
				table._turn_out_card_data, table._turn_out_card_count, can_out_card_data) == 0) {

			table._auto_out_card_scheduled = GameSchedule.put(
					new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, GameConstants.PDK_CT_PASS),
					auto_out_card_dely, TimeUnit.MILLISECONDS);
			return;
		} else {
			int card_type = table._logic.adjustAutoOutCard(table.GRR._cards_data[next_player],
					table.GRR._card_count[next_player]);
			if (card_type != GameConstants.PDK_CT_ERROR) {
				if (table._logic.CompareCard(table._turn_out_card_data, table.GRR._cards_data[next_player],
						table._turn_out_card_count, table.GRR._card_count[next_player])) {
					table._auto_out_card_scheduled = GameSchedule.put(
							new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, card_type),
							auto_out_card_dely, TimeUnit.MILLISECONDS);
				}
			}
		}

		// if(table.istrustee[table._current_player]){
		// int card_data[]=new int[16];
		// int
		// out_card_count=table._logic.Ai_Out_Card(table.GRR._cards_data[table._current_player],
		// table.GRR._card_count[table._current_player],
		// table._turn_out_card_data, table._turn_out_card_count,
		// card_data,table);
		// if(out_card_count != 0){
		// table._logic.sort_card_date_list(card_data, out_card_count);
		// table._handler_out_card_operate.reset_status(table._current_player,card_data,out_card_count,1,"");
		// table._handler_out_card_operate.exe(table);
		// }
		// }
	}

	@Override
	public boolean handler_player_be_in_room(PDKTable table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_RECONNECT_DATA);

		TableResponse_PDK_xy.Builder tableResponse_pdk = TableResponse_PDK_xy.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_pdk);
		RoomInfoPdk.Builder room_info = table.getRoomInfoPdk();
		tableResponse_pdk.setRoomInfo(room_info);
		if (table.GRR != null) {
			tableResponse_pdk.setBankerPlayer(table.GRR._banker_player);
			tableResponse_pdk.setCurrentPlayer(table._current_player);
			tableResponse_pdk.setPrevPlayer(table._prev_palyer);

			if (table._game_status == GameConstants.GS_MJ_PLAY) {
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
					if (table.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)) {
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
					if (table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_ONE)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_TWO)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_THREE)) {
						tableResponse_pdk.addSeatPiaoScore(table._piao_fen[i]);
					} else {
						tableResponse_pdk.addSeatPiaoScore(-2);
					}

					if (table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_ONE)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_TWO)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_THREE)) {
						tableResponse_pdk.addOpreatePiaoScore(-2);
					}

				}

				if (table._current_player == seat_index) {
					int can_out_card_data[] = new int[table.get_hand_card_count_max()];
					int can_out_card_count = table._logic.Player_Can_out_card(
							table.GRR._cards_data[table._current_player], table.GRR._card_count[table._current_player],
							table._turn_out_card_data, table._turn_out_card_count, can_out_card_data);
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
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					tableResponse_pdk.addSeatPiaoScore(table._piao_fen_select[i]);
				}
			}

		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_pdk));
		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	// 玩家不出
	public void no_out_card(PDKTable table) {
		int can_out_card_data[] = new int[table.get_hand_card_count_max()];
		if (table.has_rule(GameConstants.GAME_RULE_BI_XU_GUAN)
				&& table._logic.Player_Can_out_card(table.GRR._cards_data[_out_card_player],
						table.GRR._card_count[_out_card_player], table._turn_out_card_data, table._turn_out_card_count,
						can_out_card_data) != 0) {

			return;
		}
		table.GRR._cur_round_count[_out_card_player] = 0;
		table.GRR._cur_round_pass[_out_card_player] = 1;

		table._prev_palyer = _out_card_player;

		if (table._turn_out_card_count == 0) {
			return;
		}
		// 判断下一个玩家
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.get_players()[next_player] != null) {
				break;
			}
			next_player = (next_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		}
		table._current_player = next_player;
		if (table._current_player == table._out_card_player) {
			// 炸弹分
			int cbCardType = table._logic.GetCardType(table._turn_out_card_data, table._turn_out_card_count,
					table._turn_out_card_data);
			if (cbCardType == GameConstants.PDK_CT_ERROR) {
				return;
			}
			if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == table._out_card_player) {
						continue;
					}
					if (table.get_players()[i] == null) {
						continue;
					}
					if (table._hong_tao_palyer == i && table.has_rule(GameConstants.GAME_RULE_ZHADAN_ZHAIAO)
							&& table.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
						table._out_bomb_score_zhaniao[i] -= table._bomb_cell_score;
						table._out_bomb_score_zhaniao[table._out_card_player] += table._bomb_cell_score;
					} else {
						if (table._hong_tao_palyer == table._out_card_player
								&& table.has_rule(GameConstants.GAME_RULE_ZHADAN_ZHAIAO)
								&& table.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
							table._out_bomb_score_zhaniao[i] -= table._bomb_cell_score;
							table._out_bomb_score_zhaniao[table._out_card_player] += table._bomb_cell_score;
						}
					}
					table._out_bomb_score[i] -= table._bomb_cell_score;

					table._out_bomb_score[table._out_card_player] += table._bomb_cell_score;
					if (!table.is_match()) {
						table._player_result.game_score[i] -= table._bomb_cell_score;
						table._player_result.game_score[table._out_card_player] += table._bomb_cell_score;
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
			Arrays.fill(table.GRR._cur_round_pass, 0);
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
		int auto_out_card_dely = 1;

		if (table._current_player == table._out_card_player) {
			adjustAutoOutCard(table, table._out_card_player);
		} else {
			if (table._logic.Player_Can_out_card(table.GRR._cards_data[next_player], table.GRR._card_count[next_player],
					table._turn_out_card_data, table._turn_out_card_count, can_out_card_data) == 0) {

				table._auto_out_card_scheduled = GameSchedule.put(
						new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, GameConstants.PDK_CT_PASS),
						auto_out_card_dely, TimeUnit.SECONDS);
				return;
			} else {
				int card_type = table._logic.adjustAutoOutCard(table.GRR._cards_data[next_player],
						table.GRR._card_count[next_player]);
				if (card_type != GameConstants.PDK_CT_ERROR) {
					if (table._logic.CompareCard(table._turn_out_card_data, table.GRR._cards_data[next_player],
							table._turn_out_card_count, table.GRR._card_count[next_player])) {
						table._auto_out_card_scheduled = GameSchedule.put(
								new PDKAutoOutCardRunnable(table.getRoom_id(), next_player, table, card_type),
								auto_out_card_dely, TimeUnit.SECONDS);
					}
				}

				// if(table.istrustee[table._current_player]){
				// int card_data[]=new int[16];
				// int
				// out_card_count=table._logic.Ai_Out_Card(table.GRR._cards_data[table._current_player],
				// table.GRR._card_count[table._current_player],
				// table._turn_out_card_data, table._turn_out_card_count,
				// card_data,table);
				// if(out_card_count != 0){
				// table._logic.sort_card_date_list(card_data, out_card_count);
				// table._handler_out_card_operate.reset_status(table._current_player,card_data,out_card_count,1,"");
				// table._handler_out_card_operate.exe(table);
				// }
				// }
			}
		}
	}

	// 判断玩家出牌合法
	public int adjust_out_card_right(PDKTable table) {
		int cbCardType = table._logic.GetCardType(this._out_cards_data, this._out_card_count, this._out_cards_data);
		if (cbCardType == GameConstants.PDK_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return cbCardType;
		}
		// 三带一只有在剩4张牌时才能出
		if (table._turn_out_card_count == 0) {
			if (cbCardType == GameConstants.PDK_CT_THREE_TAKE_ONE) {
				if (table.GRR._card_count[this._out_card_player] != 4
						|| !table.has_rule(GameConstants.GAME_RULE_THREE_LOST_CHU)) {
					table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
					return GameConstants.PDK_CT_ERROR;
				}
			}
			if (cbCardType == GameConstants.PDK_CT_THREE) {
				if (table.GRR._card_count[this._out_card_player] != 3
						|| !table.has_rule(GameConstants.GAME_RULE_THREE_LOST_CHU)) {
					table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
					return GameConstants.PDK_CT_ERROR;
				}
			}
			if (cbCardType == GameConstants.PDK_CT_PLANE_LOST) {
				if (this._out_card_count != table.GRR._card_count[this._out_card_player]
						|| !table.has_rule(GameConstants.GAME_RULE_PLANE_LOST_NENG_CHU)) {
					table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
					return GameConstants.PDK_CT_ERROR;
				}
			}
		}

		if (table.has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)) {
			if (cbCardType != GameConstants.PDK_CT_BOMB_CARD) {
				for (int i = 0; i < _out_card_count; i++) {
					int card_count = 0;
					for (int j = 0; j < table.GRR._card_count[this._out_card_player]; j++) {
						if (table._logic.GetCardLogicValue(_out_cards_data[i]) == table._logic
								.GetCardLogicValue(table.GRR._cards_data[_out_card_player][j])) {
							card_count++;
						}
					}
					if (table.has_rule(GameConstants.GAME_RULE_KKK_BOOM)) {
						if (card_count >= 3 && cbCardType != GameConstants.PDK_CT_BOMB_CARD
								&& table._logic.get_card_value(_out_cards_data[i]) == 1) {
							table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
							return GameConstants.PDK_CT_ERROR;
						}
					}
					if (card_count >= 4 && cbCardType != GameConstants.PDK_CT_BOMB_CARD) {
						table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
						return GameConstants.PDK_CT_ERROR;
					}
				}
			}
		}
		// 四带
		if (cbCardType == GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO
				&& !table.has_rule(GameConstants.GAME_RULE_FOUR_DAI_TWO)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		} else if (cbCardType == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE
				&& !table.has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		}

		if (table._turn_out_card_count > 0) {
			if (!table._logic.CompareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
				return GameConstants.PDK_CT_ERROR;
			}
		}

		int playernumber = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.get_players()[i] == null) {
				continue;
			}
			playernumber++;
		}
		// 黑桃必须先出
		if (!table.has_rule(GameConstants.GAME_RULE_TWO_PLAY)
				&& table.has_rule(GameConstants.GAME_RULE_SHOU_JU_HEITAO_SAN) && table.GRR._cur_round == 0
				&& table._out_card_times[_out_card_player] == 0 && _out_card_player == table.GRR._banker_player
				&& playernumber == table.getTablePlayerNumber()) {
			for (int i = 1; i <= _out_card_count; i++) {
				if (_out_cards_data[_out_card_count - i] == 0x33) {
					break;
				}
				if (i == _out_card_count) {
					table.send_error_notify(_out_card_player, 2, "首出请出黑桃3!");
					return GameConstants.PDK_CT_ERROR;
				}
			}
		} else if ((table.has_rule(GameConstants.GAME_RULE_TWO_PLAY) || playernumber != table.getTablePlayerNumber())
				&& table.GRR._cur_round == 0 && table._out_card_times[_out_card_player] == 0
				&& _out_card_player == table.GRR._banker_player) {
			if (!table.has_rule(GameConstants.GAME_RULE_RAND_SHOU_CHU)
					|| playernumber != table.getTablePlayerNumber()) {
				for (int i = 1; i <= _out_card_count; i++) {
					if (_out_cards_data[_out_card_count
							- i] == table.GRR._cards_data[_out_card_player][table.GRR._card_count[_out_card_player]
									- 1]) {
						break;
					}
					if (i == _out_card_count) {
						table.send_error_notify(_out_card_player, 2, "首出请出最小牌!");
						return GameConstants.PDK_CT_ERROR;
					}
				}
			}

		}
		if (mustmax(table)) {
			table.send_error_notify(_out_card_player, 2, "单张请出最大牌!");
			return GameConstants.PDK_CT_ERROR;
		}
		// if(fang_zou_bao_pei(table)){
		// return GameConstants.PDK_CT_ERROR;
		// }
		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		}

		if (cbCardType == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE) {
			cbCardType = GameConstants.PDK_CT_THREE_TAKE_TWO;
		}
		return cbCardType;
	}

	// 放走包赔
	public boolean fang_zou_bao_pei(PDKTable table) {
		if (table.GRR._card_count[_out_card_player] == 1 && _out_card_count == 1 && table._turn_out_card_count == 1
				&& ((_out_card_player + table.getTablePlayerNumber() + 2)
						% table.getTablePlayerNumber()) == table._turn_out__player) {
			// 还原上家牌型
			int cards_data_temp[] = new int[table.GRR._card_count[table._turn_out__player] + 1];
			for (int i = 0; i < table.GRR._card_count[table._turn_out__player]; i++) {
				cards_data_temp[i] = table.GRR._cards_data[table._turn_out__player][i];
			}
			cards_data_temp[table.GRR._card_count[table._turn_out__player]] = table._turn_out_card_data[0];

			table._logic.sort_card_date_list(cards_data_temp, table.GRR._card_count[table._turn_out__player] + 1);
			return table._logic.fang_zou_bao_pei(cards_data_temp, table.GRR._card_count[table._turn_out__player] + 1,
					table._turn_out_card_data);
		}
		return false;
	}

	// 必须出最大牌
	public boolean mustmax(PDKTable table) {
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.get_players()[next_player] == null) {
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		if (table.GRR._card_count[next_player] == 1 && _out_card_count == 1) {
			return table._logic.fang_zou_bao_pei(table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player], _out_cards_data);
		}
		return false;
	}

	// 自动出牌
	public void adjustAutoOutCard(PDKTable table, int seat_index) {
		int auto_out_card_dely = 1;
		if (table.matchId != 0) {
			auto_out_card_dely = 2;
		}
		int card_type = table._logic.adjustAutoOutCard(table.GRR._cards_data[seat_index],
				table.GRR._card_count[seat_index]);
		if (card_type != GameConstants.PDK_CT_ERROR) {
			table._auto_out_card_scheduled = GameSchedule.put(
					new PDKAutoOutCardRunnable(table.getRoom_id(), seat_index, table, card_type), auto_out_card_dely,
					TimeUnit.SECONDS);
		} else {
			// if(table.istrustee[seat_index]){
			// int card_data[]=new int[16];
			// int
			// out_card_count=table._logic.Ai_Out_Card(table.GRR._cards_data[seat_index],
			// table.GRR._card_count[seat_index], table._turn_out_card_data,
			// table._turn_out_card_count, card_data,table);
			// if(out_card_count != 0){
			// table._logic.sort_card_date_list(card_data, out_card_count);
			// table._handler_out_card_operate.reset_status(seat_index,card_data,out_card_count,1,"");
			// table._handler.exe(table);
			// }
			// }

		}
	}
}
