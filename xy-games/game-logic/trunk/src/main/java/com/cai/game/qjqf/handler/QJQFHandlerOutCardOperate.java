package com.cai.game.qjqf.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.QJQFConstants;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.QJQFNotOutCardRunnable;
import com.cai.game.qjqf.QJQFGameLogic;
import com.cai.game.qjqf.QJQFTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.qjqf.QJQFRsp.GameBigBoom_QJQF;
import protobuf.clazz.qjqf.QJQFRsp.TableResponse_QJQF;

public class QJQFHandlerOutCardOperate extends QJQFHandler {

	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] _out_cards_data = new int[GameConstants.MAX_PDK_COUNT_JD]; // 出牌扑克
	public int _out_card_count = 0;
	public int _out_type;

	protected int _current_player = GameConstants.INVALID_SEAT;

	public void reset_status(int seat_index, int cards[], int card_count, int is_out) {
		_out_card_player = seat_index;
		_out_cards_data = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			_out_cards_data[i] = cards[i];
		}
		_out_card_count = card_count;
		_out_type = is_out;
	}

	@Override
	public void exe(QJQFTable table) {
		// TODO Auto-generated method stub
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			no_out_card(table);

			return;
		}

		// 清空接下去出牌玩家出牌数据
		int next_player = getNextPlayer(_out_card_player, table);
		int cbCardType = adjust_out_card_right(table, next_player);
		if (cbCardType == QJQFConstants.ERROR) {
			return;
		}
		table.cancelShedule(table.ID_TIMER_AOTU_OUT_CARD);
		table.cancelShedule(table.ID_TIMER_TRUSTEE);

		// if (table.has_rule(GameConstants.GAME_RULE_FANG_ZOU_BAOPEI)) {
		// if (fang_zou_bao_pei(table)) {
		// table._bao_pei_palyer = table._turn_out__player;
		// }
		// }
		// if(!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
		// table.GRR._card_count[_out_card_player], this._out_cards_data,
		// this._out_card_count)){
		// return;
		// }

		// // 保存玩家炸弹个数
		// if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
		// table._boom_num[_out_card_player]++;
		// table._all_boom_num[_out_card_player]++;
		// }
		if (table._out_card_scheduled != null) {
			// table._out_card_scheduled.cancel(false);
			table._out_card_scheduled = null;
		}

		// 保存玩家出牌次数
		table._out_card_times[_out_card_player]++;
		// 保存上一操作玩家
		table._prev_palyer = _out_card_player;
		table._out_card_player = _out_card_player;
		// 保存该轮出牌信息
		table.GRR._cur_round_pass[_out_card_player] = 0;
		table.GRR._cur_round_count[_out_card_player] = this._out_card_count;
		for (int i = 0; i < this._out_card_count; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = this._out_cards_data[i];
			// 保存该次出牌数据
			table._turn_out_card_data[i] = this._out_cards_data[i];
		}
		table._turn_out__player = _out_card_player;
		table._turn_out_card_count = this._out_card_count;
		table._turn_out_card_type = cbCardType;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;
		int index = 1;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.player_you[i] != 0) {
				index++;
			}
		}

		if (0 == table.GRR._card_count[_out_card_player]) {
			table.player_you[_out_card_player] = index;
		}

		if (index >= table.getTablePlayerNumber()) {
			// 都出完了只剩1个玩家
			next_player = -1;
		}
		table._current_player = next_player;
		if (next_player != -1) {
			table.GRR._cur_round_count[table._current_player] = 0;
			table.GRR._cur_round_pass[table._current_player] = 0;
			for (int j = 0; j < this._out_card_count; j++) {
				table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
			}
			SysParamModel sysParamModel1104 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(table.getGame_id()).get(1104);
			table.outCardTime = System.currentTimeMillis() + sysParamModel1104.getVal1();
			// table._out_card_scheduled = GameSchedule.put(new
			// OutCardRunnable(table.getRoom_id(), next_player,
			// ++table.out_index), 15000,
			// TimeUnit.MILLISECONDS);
		}

		// 刷新玩家手牌
		table.operate_player_cards();
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, next_player == GameConstants.INVALID_SEAT);
		table.exe_add_discard(_out_card_player, _out_card_count, _out_cards_data, false, 1);

		if (table._turn_out_card_type >= QJQFConstants.BOM_FIVE_TEN_K && table.has_rule(QJQFConstants.QJQF_RULE_ZHA)
				&& table.big_boom_index == -1) {
			boolean isBigBoom = true;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == table._turn_out__player) {
					continue;
				}
				List<Integer> list = table._logic.getOutCard(table.GRR._cards_data[i], table.GRR._card_count[i],
						table._turn_out_card_data, table._turn_out_card_count, false);
				if (list.size() > 0) {
					isBigBoom = false;
					break;
				}
			}

			if (isBigBoom) {
				table.big_boom_index = table._turn_out__player;

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				GameBigBoom_QJQF.Builder b = GameBigBoom_QJQF.newBuilder();
				b.setBigBoomIndex(table.big_boom_index);
				roomResponse.setType(QJQFConstants.RESPONSE_QJQF_BIG_BOOM);//
				for (int i = 0; i < table.round_score.length; i++) {
					if (table.big_boom_index == i) {
						table.round_score[i] += 30;
						table._game_score[i] += 30;
						b.addUpdateScore(30);

					} else {
						table.round_score[i] -= 10;
						table._game_score[i] -= 10;
						b.addUpdateScore(-10);
					}
					b.addScore(table._game_score[i]);
				}
				roomResponse.setCommResponse(PBUtil.toByteString(b));
				table.send_response_to_room(roomResponse);
			}

		}

		if (index >= table.getTablePlayerNumber()) {
			int delay = 1;
			table._current_player = GameConstants.INVALID_SEAT;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		} else {
			autoNoOutCard(next_player, table);
		}
	}

	private void autoNoOutCard(int nextPlayer, QJQFTable table) {
		if (nextPlayer < 0 || table.player_you[nextPlayer] > 0) {
			return;
		}
		List<Integer> cards = new ArrayList<>();
		int only_card_count_player = (nextPlayer + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[only_card_count_player] == 0) {
				only_card_count_player = (only_card_count_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		if (table.GRR._card_count[only_card_count_player] == 1) {
			cards = table._logic.getOutCard(table.GRR._cards_data[nextPlayer], table.GRR._card_count[nextPlayer],
					table._turn_out_card_data, table._turn_out_card_count, true);
		} else {
			cards = table._logic.getOutCard(table.GRR._cards_data[nextPlayer], table.GRR._card_count[nextPlayer],
					table._turn_out_card_data, table._turn_out_card_count, false);
		}

		if (cards.isEmpty()) {
			GameSchedule.put(new QJQFNotOutCardRunnable(table.getRoom_id(), nextPlayer, table), 700,
					TimeUnit.MILLISECONDS);
		} else {
			if (table.has_rule(QJQFConstants.QJQF_TRUSTEE)) {
				if (table.istrustee[nextPlayer]) {
					SheduleArgs args = SheduleArgs.newArgs();
					args.set("seat_index", nextPlayer);
					args.set("count", cards.size());

					for (int i = 0; i < cards.size(); i++) {
						args.set("card_value" + i, cards.get(i));
					}
					table.schedule(table.ID_TIMER_AOTU_OUT_CARD, args, 700);
				} else {
					SysParamModel sysParamModel1104 = SysParamDict.getInstance()
							.getSysParamModelDictionaryByGameId(table.getGame_id()).get(1104);
					SheduleArgs args = SheduleArgs.newArgs();
					args.set("seat_index", nextPlayer);
					table.schedule(table.ID_TIMER_TRUSTEE, args, sysParamModel1104.getVal1());

					SheduleArgs args_other = SheduleArgs.newArgs();
					args_other.set("seat_index", nextPlayer);
					args_other.set("count", cards.size());
					for (int i = 0; i < cards.size(); i++) {
						args_other.set("card_value" + i, cards.get(i));
					}
					table.schedule(table.ID_TIMER_AOTU_OUT_CARD, args_other, sysParamModel1104.getVal1());

				}
			}

		}
	}

	private int getNextPlayer(int out_card_player, QJQFTable table) {
		int index = 0;
		int nextPlayer = out_card_player;
		while (index < table.getTablePlayerNumber()) {
			index++;
			nextPlayer = (nextPlayer + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			if (table.player_you[nextPlayer] == 0) {
				return nextPlayer;
			}
		}

		return GameConstants.INVALID_SEAT;

	}

	@Override
	public boolean handler_player_be_in_room(QJQFTable table, int seat_index) {

		SysParamModel sysParamModel1104 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(table.getGame_id()).get(1104);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(QJQFConstants.RESPONSE_QJQF_RECONNECT_DATA);

		TableResponse_QJQF.Builder tableResponse_QJQF = TableResponse_QJQF.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse_QJQF.setRoomInfo(table.getRoomInfoPdk());

		if (table.GRR != null) {
			tableResponse_QJQF.setBankerPlayer(table.GRR._banker_player);
			tableResponse_QJQF.setCurrentPlayer(table._current_player);
			System.out.println("==========千分断线重连================" + table._current_player);
			tableResponse_QJQF.setPrevPlayer(table._prev_palyer);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponse_QJQF.addOutCardsCount(table.GRR._cur_round_count[i]);
				tableResponse_QJQF.addPlayerPass(table.GRR._cur_round_pass[i]);
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
				if (i == seat_index) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				}
				for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
				}
				tableResponse_QJQF.addOutCardsData(out_cards);
				tableResponse_QJQF.addCardsData(i, cards);
				tableResponse_QJQF.addPlayerYou(table.player_you[i]);
				tableResponse_QJQF.addPlayerScores(table._game_score[i]);
				tableResponse_QJQF.addRoundScores(table.round_score[i]);
			}
			if (table.has_rule(QJQFConstants.QJQF_TRUSTEE)) {
				tableResponse_QJQF.setOutCardTime(sysParamModel1104.getVal1() / 1000);
				tableResponse_QJQF.setDisplayTime(sysParamModel1104.getVal1() / 1000);
			} else {
				tableResponse_QJQF.setOutCardTime(15);
				tableResponse_QJQF.setDisplayTime(15);
			}

		}

		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse_QJQF.addPrCardsData(table._turn_out_card_data[i]);
		}
		tableResponse_QJQF.addAllScoreCard(table.score_card);
		tableResponse_QJQF.setPrCardsCount(table._turn_out_card_count);
		tableResponse_QJQF.setPrOutCardPlayer(table._turn_out__player);
		//
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_QJQF));
		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR != null) {
			// table.operate_player_cards();
		}

		return true;
	}

	// 玩家不出
	public void no_out_card(QJQFTable table) {

		if (table._turn_out__player == GameConstants.INVALID_SEAT) {
			return;
		}

		if (table._logic.searchOutCard(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],
				table._turn_out_card_data, table._turn_out_card_count)) {
			return;
		}

		table.GRR._cur_round_count[_out_card_player] = 0;
		table.GRR._cur_round_pass[_out_card_player] = 1;

		for (int i = 0; i < table.get_hand_card_count_max(); i++) {
			table.GRR._cur_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
		}
		table._prev_palyer = _out_card_player;

		if (table._current_player == table._out_card_player) {
			return;
		}
		table.cancelShedule(table.ID_TIMER_AOTU_OUT_CARD);
		if (table._out_card_scheduled != null) {
			// table._out_card_scheduled.cancel(false);
			table._out_card_scheduled = null;
		}
		// 判断下一个玩家
		int index = 0;
		boolean isNewTurn = false;
		int next_player = _out_card_player;
		while (index < table.getTablePlayerNumber()) {
			index++;
			next_player = (next_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			// 上次出牌的玩家，没有人吃掉他的牌，又轮到他，说明他这轮已经结束了，分数给他。。他也出完牌了，让他的下家随意出牌
			if (table._out_card_player == next_player && table.player_you[next_player] != 0) {
				isNewTurn = true;
			}

			if (table.player_you[next_player] == 0) {
				break;
			}
		}

		table._current_player = next_player;
		if (table._current_player == table._out_card_player) {
			// 炸弹分
			int cbCardType = table._logic.getCardType(table._turn_out_card_data, table._turn_out_card_count,
					table._turn_out_card_data);
			if (cbCardType == QJQFConstants.ERROR) {
				return;
			}

			isNewTurn = true;
		}
		if (isNewTurn) {
			// 出完一圈牌
			table._turn_out_card_count = 0;
			table._turn_out_card_type = QJQFConstants.ERROR;
			for (int i = 0; i < this._out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_round_pass, 0);
		}

		int overCount = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.player_you[i] != 0) {
				overCount++;
			}
		}

		if (overCount >= table.getTablePlayerNumber() - 1) {
			int delay = 1;
			table._current_player = -1;

			if (isNewTurn) {
				int score = 0;
				for (int card : table.score_card) {
					int value = QJQFGameLogic.get_card_value(card);
					if (value == 5) {
						score += 5;
					} else {
						score += 10;
					}
				}
				// 计算本轮赢家的牌
				if (score > 0) {
					table.round_score[table._turn_out__player] += score;
					table._game_score[table._turn_out__player] += score;
				}
				table.score_card.clear();
			}

			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		} else {
			table.outCardTime = System.currentTimeMillis() + 15000;
			table.operate_player_status();
			if (!isNewTurn) {
				autoNoOutCard(next_player, table);
			} else {
				if (table.has_rule(QJQFConstants.QJQF_TRUSTEE)) {
					if (table.istrustee[next_player]) {
						List<Integer> cards = new ArrayList<>();
						int only_card_count_player = (next_player + 1) % table.getTablePlayerNumber();
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							if (table.GRR._card_count[only_card_count_player] == 0) {
								only_card_count_player = (only_card_count_player + 1) % table.getTablePlayerNumber();
							} else {
								break;
							}
						}
						if (table.GRR._card_count[only_card_count_player] == 1) {
							cards = table._logic.getOutCard(table.GRR._cards_data[next_player],
									table.GRR._card_count[next_player], table._turn_out_card_data,
									table._turn_out_card_count, true);
						} else {
							cards = table._logic.getOutCard(table.GRR._cards_data[next_player],
									table.GRR._card_count[next_player], table._turn_out_card_data,
									table._turn_out_card_count, false);
						}
						SheduleArgs args = SheduleArgs.newArgs();
						args.set("seat_index", next_player);
						args.set("count", cards.size());
						for (int i = 0; i < cards.size(); i++) {
							args.set("card_value" + i, cards.get(i));
						}
						table.schedule(table.ID_TIMER_AOTU_OUT_CARD, args, 1000);
					} else {
						SysParamModel sysParamModel1104 = SysParamDict.getInstance()
								.getSysParamModelDictionaryByGameId(table.getGame_id()).get(1104);
						SheduleArgs args = SheduleArgs.newArgs();
						args.set("seat_index", next_player);
						table.schedule(table.ID_TIMER_TRUSTEE, args, sysParamModel1104.getVal1());

					}
				}

			}

			// GameSchedule.put(new OutCardRunnable(table.getRoom_id(),
			// next_player, ++table.out_index), 15000, TimeUnit.MILLISECONDS);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, _out_cards_data, QJQFConstants.PASS, GameConstants.INVALID_SEAT,
				isNewTurn);

		// 通知客户端出牌用户
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == table._current_player) {
				continue;
			}
			table._playerStatus[i].set_status(GameConstants.Player_Status_NULL);
		}
		table._playerStatus[table._current_player].set_status(GameConstants.Player_Status_OUT_CARD);
	}

	// 判断玩家出牌合法
	public int adjust_out_card_right(QJQFTable table, int player) {
		int cbCardType = table._logic.getCardType(this._out_cards_data, this._out_card_count, this._out_cards_data);
		if (cbCardType == QJQFConstants.ERROR) {
			return cbCardType;
		}

		if (table._turn_out_card_count > 0) {
			if (!table._logic.compareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				return QJQFConstants.ERROR;
			}
		}

		if (player != this._out_card_player && player != -1 && cbCardType == QJQFConstants.SINGLE
				&& table.GRR._card_count[player] == 1) {
			int value = table._logic.GetCardLogicValue(table.GRR._cards_data[this._out_card_player][0]);
			for (int i = 1; i < table.GRR._card_count[_out_card_player]; i++) {
				int temp = table._logic.GetCardLogicValue(table.GRR._cards_data[this._out_card_player][i]);
				if (temp > value) {
					value = temp;
				}
			}
			if (value != table._logic.GetCardLogicValue(this._out_cards_data[0])) {
				return QJQFConstants.ERROR;
			}
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)) {
			return QJQFConstants.ERROR;
		}

		return cbCardType;
	}

	// 放走包赔
	public boolean fang_zou_bao_pei(QJQFTable table) {
		if (table.GRR._card_count[_out_card_player] == 1 && _out_card_count == 1 && table._turn_out_card_count == 1
				&& ((_out_card_player + table.getTablePlayerNumber() + 2)
						% table.getTablePlayerNumber()) == table._turn_out__player) {
			// 还原上家牌型
			int cards_data_temp[] = new int[table.GRR._card_count[table._turn_out__player] + 1];
			for (int i = 0; i < table.GRR._card_count[table._turn_out__player]; i++) {
				cards_data_temp[i] = table.GRR._cards_data[table._turn_out__player][i];
			}
			cards_data_temp[table.GRR._card_count[table._turn_out__player]] = table._turn_out_card_data[0];

			table._logic.sort_card_date_list(cards_data_temp, table.GRR._card_count[table._turn_out__player] + 1,
					table._score_type[table._turn_out__player]);
			return table._logic.fang_zou_bao_pei(cards_data_temp, table.GRR._card_count[table._turn_out__player] + 1,
					table._turn_out_card_data);
		}
		return false;
	}

}
