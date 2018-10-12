package com.cai.game.yyqf.handler;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_YYQF;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.YyqfNotOutCardRunnable;
import com.cai.game.yyqf.YYQFGameLogic;
import com.cai.game.yyqf.YYQFTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.yyqf.YYQFRsp.TableResponseYYQF;

public class YYQFHandlerOutCardOperate extends YYQFHandler {

	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] _out_cards_data = new int[GameConstants.MAX_PDK_COUNT_JD]; // 出牌扑克
	public int _out_card_count = 0;
	public int _out_type;
	public boolean isEnd = false;

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
	public void exe(YYQFTable table) {
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
		int cbCardType = check_out_card_right(table, next_player);
		if (cbCardType == Constants_YYQF.ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
			return;
		} else if (cbCardType >= 7 && cbCardType <= 12 && table.has_rule(Constants_YYQF.YYQF_RULE_BOOM_ALWAYS_GETSCORE)) {
			int boom = cbCardType - 6;
			int score = 0;
			if (table.has_rule(Constants_YYQF.YYQF_RULE_XI_FEN_ADD)) {
				score = boom * 100;
			} else if (table.has_rule(Constants_YYQF.YYQF_RULE_XI_FEN_MULTI)) {
				score = (1 << (boom - 1)) * 100;
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.roundBoomScore[i] -= score;
				// table.boomScore[i] -= score;
			}
			table.roundBoomScore[_out_card_player] += score * 3;
			// table.boomScore[_out_card_player] += score * 3;
		}

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
		table._turn_out_player = _out_card_player;
		table._turn_out_card_count = this._out_card_count;
		table._turn_out_card_type = cbCardType;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;

		int index = 1;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.player_rank[i] != 0) {
				index++;
			}
		}

		if (0 == table.GRR._card_count[_out_card_player]) {
			table.player_rank[_out_card_player] = index;
			if (index == 1) {
				table.firstPlayer = _out_card_player;
			}
		}

		if (index >= table.getTablePlayerNumber()) { // 都出完了只剩1个玩家
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
		table.exe_add_discard(_out_card_player, _out_card_count, _out_cards_data, false, 1);

		if (index >= table.getTablePlayerNumber()) {
			isEnd = true;
			table.showLastCard();
			int delay = 3;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), this._out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		} else {
			autoNoOutCard(next_player, table);
		}
	}

	private void autoNoOutCard(int nextPlayer, YYQFTable table) {
		if (nextPlayer < 0 || table.player_rank[nextPlayer] > 0) {
			return;
		}

		List<Integer> cards = table._logic.getOutCard(table.GRR._cards_data[nextPlayer], table.GRR._card_count[nextPlayer], table._turn_out_card_data,
				table._turn_out_card_count, table._turn_out_card_type, table.has_rule(Constants_YYQF.YYQF_RULE_RETAIN_SIX_SEVEN));
		if (cards.isEmpty()) {
			GameSchedule.put(new YyqfNotOutCardRunnable(table.getRoom_id(), nextPlayer, table), 700, TimeUnit.MILLISECONDS);
		}
	}

	private int getNextPlayer(int out_card_player, YYQFTable table) {
		int index = 0;
		int nextPlayer = out_card_player;
		while (index < table.getTablePlayerNumber()) {
			index++;
			nextPlayer = (nextPlayer + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			if (table.player_rank[nextPlayer] == 0) {
				return nextPlayer;
			}
		}

		return GameConstants.INVALID_SEAT;
	}

	/**
	 * 玩家不出
	 * 
	 * @param table
	 */
	public void no_out_card(YYQFTable table) {
		if (table._turn_out_player == GameConstants.INVALID_SEAT) {
			return;
		}

		if (table._logic.searchOutCard(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player], table._turn_out_card_data,
				table._turn_out_card_count, table._turn_out_card_type, table.has_rule(Constants_YYQF.YYQF_RULE_RETAIN_SIX_SEVEN))) {
			return;
		}

		table.GRR._cur_round_count[_out_card_player] = 0;
		table.GRR._cur_round_pass[_out_card_player] = 1;

		for (int i = 0; i < table.handCardCount; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
		}
		table._prev_palyer = _out_card_player;

		if (table._current_player == table._out_card_player) {
			return;
		}

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
			next_player = (next_player + 1) % table.getTablePlayerNumber();

			// 上次出牌的玩家，没有人吃掉他的牌，又轮到他，说明他这轮已经结束了，分数给他。。他也出完牌了，让他的下家随意出牌
			if (table._out_card_player == next_player && table.player_rank[next_player] != 0) {
				isNewTurn = true;
			}

			if (table.player_rank[next_player] == 0) {
				break;
			}
		}

		table._current_player = next_player;
		if (table._current_player == table._out_card_player) {
			// 炸弹分
			int cbCardType = table._logic.getCardType(table._turn_out_card_data, table._turn_out_card_count, table._turn_out_card_data,
					table.has_rule(Constants_YYQF.YYQF_RULE_RETAIN_SIX_SEVEN));
			if (cbCardType == Constants_YYQF.ERROR) {
				return;
			}
			isNewTurn = true;
		}
		if (isNewTurn) {
			// 出完一圈牌 /7炸及以上的炸弹可得喜分
			if (table._turn_out_card_type >= 7 && table._turn_out_card_type <= 12 && !table.has_rule(Constants_YYQF.YYQF_RULE_BOOM_ALWAYS_GETSCORE)) {
				int boom = table._turn_out_card_type - 6;
				int score = 0;
				if (table.has_rule(Constants_YYQF.YYQF_RULE_XI_FEN_ADD)) {
					score = boom * 100;
				} else if (table.has_rule(Constants_YYQF.YYQF_RULE_XI_FEN_MULTI)) {
					score = (1 << (boom - 1)) * 100;
				}
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.roundBoomScore[i] -= score;
					// table.boomScore[i] -= score;
				}
				table.roundBoomScore[table._turn_out_player] += score * 3;
				// table.boomScore[table._turn_out_player] += score * 3;
			}
			table._turn_out_card_count = 0;
			table._turn_out_card_type = Constants_YYQF.ERROR;
			for (int i = 0; i < this._out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_round_pass, 0);
		}

		int overCount = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.player_rank[i] != 0) {
				overCount++;
			}
		}

		if (overCount >= table.getTablePlayerNumber() - 1) {
			table._current_player = -1;

			if (isNewTurn) {
				int score = 0;
				for (int card : table.score_card) {
					int value = YYQFGameLogic.get_card_value(card);
					if (value == 5) {
						score += 5;
					} else {
						score += 10;
					}
				}
				// 计算本轮赢家的牌
				if (score > 0) {
					table.round_score[table._turn_out_player] += score;
					table._game_score[table._turn_out_player] += score;
				}
				table.score_card.clear();
			}

			isEnd = true;
			table.showLastCard();
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
			return;
		} else {
			table.outCardTime = System.currentTimeMillis() + 15000;
			table.operate_player_status();
			if (!isNewTurn) {
				autoNoOutCard(next_player, table);
			}
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, _out_cards_data, Constants_YYQF.PASS, GameConstants.INVALID_SEAT, isNewTurn);
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
	public int check_out_card_right(YYQFTable table, int player) {
		int cbCardType = table._logic.getCardType(this._out_cards_data, this._out_card_count, this._out_cards_data,
				table.has_rule(Constants_YYQF.YYQF_RULE_RETAIN_SIX_SEVEN));
		if (cbCardType == Constants_YYQF.ERROR) {
			return cbCardType;
		}

		if (table._turn_out_card_count > 0) {
			if (!table._logic.compareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count, _out_card_count,
					table.has_rule(Constants_YYQF.YYQF_RULE_RETAIN_SIX_SEVEN))) {
				return Constants_YYQF.ERROR;
			}
		}

		// 下家只剩一张牌了 出单牌必须是最大的
		// if (player != this._out_card_player && player != -1 && cbCardType ==
		// Constants_YYQF.SINGLE && table.GRR._card_count[player] == 1) {
		// if (YYQFGameLogic.get_card_value(this._out_cards_data[0]) !=
		// table._logic.getMaxSingleCardValue(table.GRR._cards_data[this._out_card_player],
		// table.GRR._card_count[this._out_card_player])) {
		// return Constants_YYQF.ERROR;
		// }
		// }

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player], this._out_cards_data,
				this._out_card_count)) {
			return Constants_YYQF.ERROR;
		}

		return cbCardType;
	}

	@Override
	public boolean handler_player_be_in_room(YYQFTable table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(Constants_YYQF.RESPONSE_YYQF_RECONNECT_DATA);

		TableResponseYYQF.Builder tableResponse_YYQF = TableResponseYYQF.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse_YYQF.setRoomInfo(table.getRoomInfoQF());

		if (table.GRR != null) {
			tableResponse_YYQF.setBankerPlayer(table.GRR._banker_player);
			tableResponse_YYQF.setCurrentPlayer(table._current_player);
			tableResponse_YYQF.setPrevPlayer(table._prev_palyer);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponse_YYQF.addOutCardsCount(table.GRR._cur_round_count[i]);
				tableResponse_YYQF.addPlayerPass(table.GRR._cur_round_pass[i]);
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

				tableResponse_YYQF.addCardsData(i, cards);
				tableResponse_YYQF.addPlayerYou(table.player_rank[i]);
				tableResponse_YYQF.addPlayerScores(table._game_score[i]);
				tableResponse_YYQF.addRoundScores(table.round_score[i]);
				tableResponse_YYQF.addRoundBoomScores(table.roundBoomScore[i]);
				tableResponse_YYQF.addBoomScores(table.boomScore[i]);
			}
			tableResponse_YYQF.setOutCardTime(table.outCardTime - System.currentTimeMillis());
		}

		for (int i = 0; i < Constants_YYQF.BASE_CARD_COUNT; i++) {
			tableResponse_YYQF.addBaseCardsData(table.baseCards[i]);
		}
		tableResponse_YYQF.setBaseCardsCount(Constants_YYQF.BASE_CARD_COUNT);
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse_YYQF.addPrCardsData(table._turn_out_card_data[i]);
		}
		tableResponse_YYQF.setPrCardsCount(table._turn_out_card_count);
		tableResponse_YYQF.setPrOutCardType(table._turn_out_card_type);
		tableResponse_YYQF.setPrOutCardPlayer(table._turn_out_player);
		tableResponse_YYQF.addAllScoreCard(table.score_card);
		tableResponse_YYQF.setPrCardsCount(table._turn_out_card_count);

		tableResponse_YYQF.setPrOutCardPlayer(table._turn_out_player);
		tableResponse_YYQF.setIsEnd(isEnd);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_YYQF));
		table.send_response_to_player(seat_index, roomResponse);

		if (table._current_player == seat_index) {
			PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		}

		if (table.GRR != null) {
			table.operate_player_cards();
		}

		return true;
	}

}
