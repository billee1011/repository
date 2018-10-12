package com.cai.game.dzd.handler;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.dzd.DZDConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.dzd.DZDGameLogic;
import com.cai.game.dzd.DZDTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dzd.DzdRsp.TableResponse_DZD;

public class DZDHandlerOutCardOperate extends DZDHandler {

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
	public void exe(DZDTable table) {
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		// 玩家不出
		if (_out_type == 0) {
			no_out_card(table);
			return;
		}

		// 清空接下去出牌玩家出牌数据
		int next_player = getNextPlayer(_out_card_player, table);
		int cbCardType = adjust_out_card_right(table, next_player);
		if (cbCardType == DZDConstants.ERROR) {
			table.send_error_notify(table.getCampSeat(_out_card_player), 2, "请选择正确的牌型");
			return;
		}
		if (table._out_card_scheduled != null) {
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
		table.GRR._card_count[table.getCampSeat(_out_card_player)] -= this._out_card_count;
		int index = 1;
		int indexA = 0, indexB = 0; // 甲乙两方出完的人数

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.player_you[i] != 0) {
				index++;
				if (0 == i % 2) {
					indexA++;
				} else {
					indexB++;
				}
			}
		}

		if (0 == table.GRR._card_count[table.getCampSeat(_out_card_player)]) {
			table.player_you[_out_card_player] = index; // 这里记录的是实际座位的游数
		}

		if (index >= table.getTablePlayerNumber() || indexA >= 2 || indexB >= 2) {
			// 都出完了只剩1个玩家 或者甲乙两方有一方两个玩家都出完了
			next_player = -1;
		}
		table._current_player = next_player;
		if (next_player != -1) {
			table.GRR._cur_round_count[table._current_player] = 0;
			table.GRR._cur_round_pass[table._current_player] = 0;
			for (int j = 0; j < this._out_card_count; j++) {
				table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
			}
			table.outCardTime = System.currentTimeMillis() + 15000;
		}

		// 刷新玩家手牌
		table.operate_player_cards();
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data, table._turn_out_card_type,
				GameConstants.INVALID_SEAT, next_player == GameConstants.INVALID_SEAT);
		// 这里不需要加入废牌堆
//		table.exe_add_discard(_out_card_player, _out_card_count, _out_cards_data, false, 1);

		if (index >= table.getTablePlayerNumber() || indexA >= 2 || indexB >= 2) {
			int overCount = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (0 != table.player_you[i]) {
					overCount++;
				}
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (0 == table.player_you[i]) {
					table.player_you[i] = ++overCount;
				}
			}

			int delay = 1;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			return;
		} else {
			autoNoOutCard(next_player, table);
		}
	}

	private void autoNoOutCard(int nextPlayer, DZDTable table) {
		if (nextPlayer < 0 || table.player_you[nextPlayer] > 0) {
			return;
		}
//		int real = table.getCampSeat(nextPlayer);
//		List<Integer> cards = table._logic.getOutCard(table.GRR._cards_data[real], table.GRR._card_count[real], table._turn_out_card_data,
//				table._turn_out_card_count, table._turn_out_card_type);
//		if (cards.isEmpty()) {
//			GameSchedule.put(new DzdNotOutCardRunnable(table.getRoom_id(), nextPlayer, table), 700, TimeUnit.MILLISECONDS);
//		} else {
//			table.handler_operate_out_card_mul(nextPlayer, cards, cards.size(), 2, "");
//		}
	}

	private int getNextPlayer(int out_card_player, DZDTable table) {
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
	public boolean handler_player_be_in_room(DZDTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DZDConstants.RESPONSE_DZD_RECONNECT_DATA);

		TableResponse_DZD.Builder tableResponse_DZD = TableResponse_DZD.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse_DZD.setRoomInfo(table.getRoomInfoDzd());

		if (table.GRR != null) {
			tableResponse_DZD.setCurrentPlayer(table._current_player);
			tableResponse_DZD.setPrevPlayer(table._prev_palyer);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponse_DZD.addCardsData(i, Int32ArrayResponse.newBuilder());
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponse_DZD.addOutCardsCount(table.GRR._cur_round_count[i]);
				tableResponse_DZD.addPlayerPass(table.GRR._cur_round_pass[i]);
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (i == seat_index) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				}

				tableResponse_DZD.setCardsData(table.get_players()[i].get_seat_index(), cards);
				tableResponse_DZD.addPlayerYou(table.player_you[i]);
				tableResponse_DZD.addRoundScores(table.round_score[i]);
				tableResponse_DZD.addPlayerScore(table._game_score[table.map.get(table.get_players()[table.getSeatByIndex(i)].getAccount_id())]);
				for (int ij = 0; ij < 2; ij++) {
					tableResponse_DZD.addEdgeScore(table.abScore[ij]);
				}
			}
			tableResponse_DZD.setOutCardTime(table.outCardTime - System.currentTimeMillis());
		}

		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse_DZD.addPrCardsData(table._turn_out_card_data[i]);
		}
		tableResponse_DZD.addAllScoreCard(table.score_card);
		tableResponse_DZD.setPrCardsCount(table._turn_out_card_count);
		tableResponse_DZD.setIsFirstOut(table.isNew ? 1 : 0);

		tableResponse_DZD.setPrOutCardPlayer(table._turn_out__player);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_DZD));
		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR != null) {
			table.operate_player_cards();
		}

		return true;
	}

	/**
	 * 玩家不出
	 * 
	 * @param table
	 */
	public void no_out_card(DZDTable table) {
		if (table._turn_out__player == GameConstants.INVALID_SEAT) {
			return;
		}
		if (this._out_type != 0 && table._logic.searchOutCard(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],
				table._turn_out_card_data, table._turn_out_card_count, table._turn_out_card_type)) {
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

		if (table._out_card_scheduled != null) {
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
				next_player = (next_player + 2) % table.getTablePlayerNumber();
				isNewTurn = true;
			}
			if (table.player_you[next_player] == 0) {
				break;
			}
		}

		table._current_player = next_player;
		if (table._current_player == table._out_card_player) {
			// 炸弹分
			int cbCardType = table._logic.getCardType(table._turn_out_card_data, table._turn_out_card_count, table._turn_out_card_data);
			if (cbCardType == DZDConstants.ERROR) {
				return;
			}
			isNewTurn = true;
		}
		if (isNewTurn) {
			// 出完一圈牌
			table._turn_out_card_count = 0;
			table._turn_out_card_type = DZDConstants.ERROR;
			for (int i = 0; i < this._out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_round_pass, 0);
		}

		int overCount = 0;
		int overA = 0, overB = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.player_you[i] != 0) {
				overCount++;
				if (i % 2 == 0) {
					overA++;
				} else {
					overB++;
				}
			}
		}

		if (overCount >= table.getTablePlayerNumber() - 1 || overA >= 2 || overB >= 2) {
			int delay = 1;
			table._current_player = -1;

			int score = 0;
			for (int card : table.score_card) {
				int value = DZDGameLogic.get_card_value(card);
				if (value == 5) {
					score += 5;
				} else {
					score += 10;
				}
			}
			// 计算本轮赢家的牌
			if (score > 0) {
				table.round_score[table._turn_out__player] += score;
				table.abScore[table._turn_out__player % 2] += score;
				// table._game_score[table._turn_out__player] += score;
			}
			table.score_card.clear();

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (0 == table.player_you[i]) {
					table.player_you[i] = ++overCount;
				}
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			return;
		} else {
			table.outCardTime = System.currentTimeMillis() + 15000;
			table.operate_player_status();
			if (!isNewTurn) {
				autoNoOutCard(next_player, table);
			}
		}
		// 显示出牌
		table.operate_out_card(_out_card_player, 0, _out_cards_data, DZDConstants.PASS, GameConstants.INVALID_SEAT, isNewTurn);

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
	public int adjust_out_card_right(DZDTable table, int player) {
		int cbCardType = table._logic.getCardType(this._out_cards_data, this._out_card_count, this._out_cards_data);
		if (cbCardType == DZDConstants.ERROR) {
			return cbCardType;
		}

		if (table._turn_out_card_count > 0) {
			if (!table._logic.compareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count, _out_card_count)) {
				return DZDConstants.ERROR;
			}
		}

		int real = table.getCampSeat(_out_card_player);
		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[real], table.GRR._card_count[real], this._out_cards_data, this._out_card_count)) {
			return DZDConstants.ERROR;
		}

		return cbCardType;
	}

}
