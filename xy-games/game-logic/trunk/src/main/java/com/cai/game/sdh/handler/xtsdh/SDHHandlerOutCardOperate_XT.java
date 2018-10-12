package com.cai.game.sdh.handler.xtsdh;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_XT;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.SDHClearRoundRunnable;
import com.cai.future.runnable.SDHGameFinishRunnable;
import com.cai.game.sdh.SDHUtil;
import com.cai.game.sdh.handler.SDHHandler;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sdh.SdhRsp.RoomPlayerResponseSdh;
import protobuf.clazz.sdh.SdhRsp.TableResponseSdh;

public class SDHHandlerOutCardOperate_XT<T extends SDHTable_XT> extends SDHHandler<T> {

	private static Logger logger = Logger.getLogger(SDHHandlerOutCardOperate_XT.class);

	private boolean success = true;

	public int outCardPlayer = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] outCardsData = new int[SDHConstants_XT.SDH_CARD_COUNT_REMOVE6]; // 出牌扑克
	public int outCardCount = 0;

	public int[][] outCardsDatas; // 当前轮出牌信息
	public int outNumber; // 当前轮出牌人数
	public int firstType = SDHConstants_XT.SDH_ERROR_NUMBER; // 首出的类型
	public int firstCount; // 首出牌数
	public int status = 0;

	@Override
	public void exe(SDHTable_XT table) {
		table._current_player = outCardPlayer;
		if (outCardCount <= 0) {
			logger.error("出牌数量必须多于一张" + outCardPlayer);
			success = false;
			return;
		}
		if (table._current_player != table.nextPlayer) {
			logger.error("不是该玩家出牌" + outCardPlayer);
			success = false;
			return;
		}
		if (table.GRR == null) {
			logger.error("SDHHandlerOutCardOperate_XT GRR为空 不能继续刷新手牌");
			return ;
		}
		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = SDHUtil.copyArray(table.GRR._cards_data[outCardPlayer], 0, table.GRR._card_count[outCardPlayer]);
		// 置零扑克
		for (int i = 0; i < outCardCount; i++) {
			for (int j = 0; j < table.GRR._card_count[outCardPlayer]; j++) {
				if (outCardsData[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					break;
				}
			}
		}
		// 判断
		if (cbDeleteCount != outCardCount) {
			table.send_error_notify(outCardPlayer, 2, "您 出牌不对"+outCardsData[0]);
			return ;
		}


		if (0 == outNumber) { // 首出玩家出牌校验与记录
			table.currentFirst = outCardPlayer;
			outCardsDatas = new int[table.getTablePlayerNumber()][outCardCount]; // 初始化数据

			// 记录首出牌型
			firstType = table._logic.getOutCardTypeWithOutLimit(table, outCardPlayer, table.getTablePlayerNumber(), outCardsData, outCardCount);
			if (firstType == SDHConstants_XT.SDH_CT_ERROR) {
				logger.error("首出错误的牌型" + outCardPlayer);
				success = false;
				return;
			}
			// 记录首出花色
			table.firstOutColor = table._logic.getCardLogicColor(outCardsData[0]);
			table.firstPlayer = outCardPlayer;
			firstCount = outCardCount;
		} else { // 校验非首出玩家出牌正确性
			if (outCardCount != firstCount) {
				logger.error("出牌数量不对" + outCardPlayer);
				success = false;
				return;
			}
			if (!validateOtherPlayerOutCards(table)) {
				logger.error("玩家出牌类型不对" + outCardPlayer);
				success = false;
				return;
			}
		}
		table.GRR._cur_round_count[outCardPlayer] = outCardCount;
		table.GRR._cur_round_data[outCardPlayer] = Arrays.copyOf(outCardsData, outCardCount);
		outCardsDatas[outCardPlayer] = Arrays.copyOf(outCardsData, outCardCount);
		outNumber++;
		table.reconnectOutCards = 1; // 记录当前轮是否已经有出牌

		int winnerSeatIndex = table._logic.compareCardArrayWithOutLimit(table, table.getTablePlayerNumber(), outCardsDatas, outCardCount, table.currentFirst);
		// 记录出牌信息
		recordCard(outCardPlayer, table);
		// 显示出牌
		table.operate_out_card(outCardPlayer, outCardCount, outCardsData, firstType, GameConstants.INVALID_SEAT, winnerSeatIndex);
		// 收起该玩家的效果通知
		table.operate_effect_action(table._current_player, GameConstants.Effect_Action_Other, 1, new long[] { SDHConstants_XT.Player_Status_OUT_CARDS }, 1,
				table._current_player);

		// 设置下一个出牌用户
		int nextSeat = (outCardPlayer + 1) % table.getTablePlayerNumber();
		int winScore = 0;
		// 当前轮所有玩家都已经出完
		if (outNumber == table.getTablePlayerNumber()) {
			winScore = table._logic.calculationScore(table, outCardsDatas, outCardCount);

			recordScore(winnerSeatIndex, table);
			if (winnerSeatIndex != table._banker_select) { // 闲家大
				if (winScore > 0) {
					table.allScore += winScore; // 总得分
					table.playerScores[winnerSeatIndex] += winScore; // 玩家个人得分
				}
				if (table.allScore - 70 >= table._di_fen
						|| (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_YI_DANG_REN_SHU) && table.allScore - 40 >= table._di_fen && table.rate == 1)) { // 大倒直接结束
					table.refresh_player_score(winnerSeatIndex, winnerSeatIndex == table._banker_select ? 0 : winScore);
					GameSchedule.put(new SDHGameFinishRunnable(table.getRoom_id()), 1200, TimeUnit.MILLISECONDS);
					return;
				}
			} else { // 庄家大
				if (winScore > 0) {
					table.playerScores[winnerSeatIndex] += winScore;
				}
			}

			outNumber = 0;
			table.outRound++;
			table.firstOutColor = SDHConstants_XT.SDH_ERROR_NUMBER;
			table.firstPlayer = SDHConstants_XT.SDH_ERROR_NUMBER;
			this.firstType = SDHConstants_XT.SDH_ERROR_NUMBER;
			// 出牌数等于手牌数 即表示出完牌了
			if (outCardCount == table.GRR._card_count[outCardPlayer]) {
				int type = table._logic.getOutCardTypeWithOutLimit(table, winnerSeatIndex, table.getTablePlayerNumber(), outCardsDatas[winnerSeatIndex],
						outCardCount);
				int color = table._logic.getCardLogicColor(outCardsDatas[winnerSeatIndex][0]);
				if (color == SDHConstants_XT.SDH_COLOR_MAIN) { // 如果是主牌 则可看底牌
					int difen = table._logic.calculationScore(table, table.diPai, SDHConstants_XT.SDH_DIPAI_COUNT);
					if (difen > 0 && winnerSeatIndex != table._banker_select) {
						if (type == 2 || type == 4) {
							table.allScore += difen * outCardCount;
							winScore += difen * outCardCount;
						} else {
							table.allScore += difen;
							winScore += difen;
						}
					}
				}
				// 结算
				table.refresh_player_score(winnerSeatIndex, winnerSeatIndex == table._banker_select ? 0 : winScore);
				GameSchedule.put(new SDHGameFinishRunnable(table.getRoom_id()), 1200, TimeUnit.MILLISECONDS);
				table.reconnectOutCards = SDHConstants_XT.SDH_ERROR_NUMBER; // 清除本轮出牌状态
				return;
			}
			nextSeat = winnerSeatIndex;
			table.reconnectOutCards = SDHConstants_XT.SDH_ERROR_NUMBER; // 清除本轮出牌状态
			GameSchedule.put(new SDHClearRoundRunnable(table.getRoom_id(), nextSeat, 1), 1000, TimeUnit.MILLISECONDS);

			// 通知所有玩家看分牌、回看
			List<Integer> list = Lists.newArrayList();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table.outRound > 0) {
					list.add(SDHConstants_XT.Player_Status_RELOOK);
				}
				if (!table.has_rule(GameConstants.GAME_RULE_SDH_QUDIAO6)) {
					list.add(SDHConstants_XT.Player_Status_LOOK_DIPAI);
				}
				if (table.freeScoreCardsCount > 0) {
					list.add(SDHConstants_XT.Player_Status_LOOK_SCORE);
				}
				long[] operate = SDHUtil.listToArray(list);
				table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, operate.length, operate, 1, i);
				list.clear();
			}
		}

		status = 0;
		table.nextPlayer = nextSeat;
		if (table.judgeAllhadNoMain() && !table.bankerHasOut && this.outCardPlayer == table._cur_banker) {
			table.bankerHasOut = true;
		}
		if (table.judgeAllhadNoMain() && table.bankerHasOut && table._banker_select != this.outCardPlayer
				&& table.guard[this.outCardPlayer] == SDHConstants_XT.SDH_ERROR_NUMBER && table.GRR._card_count[outCardPlayer] > outCardCount) {
			table.sendGuardInfo(this.outCardPlayer);
			status = 1;
		} else {
			int delay = 0;
			if (table.firstPlayer == SDHConstants_XT.SDH_ERROR_NUMBER) {
				delay = 1000;
			}
			GameSchedule.put(new SDHClearRoundRunnable(table.getRoom_id(), nextSeat, 2), delay, TimeUnit.MILLISECONDS);
		}
		//移除手牌

		table._logic.removeCardsByData(table.GRR._cards_data[outCardPlayer], table.GRR._card_count[outCardPlayer], outCardsData, outCardCount, 0);	
		table.GRR._card_count[outCardPlayer] -= outCardCount;
		
		table.refreshPlayerCards(outCardPlayer);
		table.hasOutCard = true;

		if (table.firstPlayer == SDHConstants_XT.SDH_ERROR_NUMBER) {
			table.recordMaxCard();
			table.refresh_player_score(winnerSeatIndex, winnerSeatIndex == table._banker_select ? 0 : winScore);
		}

	}

	public void resetStatus(int seatIndex, int cards[], int cardCount) {
		success = true;
		outCardPlayer = seatIndex;
		for (int i = 0; i < cardCount; i++) {
			outCardsData[i] = cards[i];
		}
		outCardCount = cardCount;
	}

	/**
	 * 出牌合法性校验
	 * 
	 * @param table
	 * @return
	 */
	public boolean validateOtherPlayerOutCards(SDHTable_XT table) {
		int colorCount = 0; // 当前玩家拥有首出玩家花色卡牌数据
		int duiziCount = 0; // 当前玩家拥有首出玩家花色卡牌对子数据
		int duiziValue[] = new int[13];
		for (int i = 0; i < SDHConstants_XT.SDH_ONE_COLOR_COUNT + 4; i++) {
			// 非主牌不计算2、7
			if (table.firstOutColor != SDHConstants_XT.SDH_COLOR_MAIN && (i == table._logic.m_cbNTValue || i == table._logic.m_cbMainValue)) {
				continue;
			} // 主牌上常主或者主值牌数量多于2张, 则寻找其他花色牌对子
			if ((i == table._logic.m_cbNTValue || i == table._logic.m_cbMainValue)
					&& table.cardsValues[outCardPlayer][table.firstOutColor][i] >= SDHConstants_XT.SDH_PACK_COUNT) {
				for (int j = 0; j < SDHConstants_XT.SDH_COLOR_COUNT; j++) {
					if (table.cardsValues[outCardPlayer][j][i] == SDHConstants_XT.SDH_PACK_COUNT) {
						duiziValue[duiziCount] = j * 16 + i;
						duiziCount++;
					}
				}
				colorCount += table.cardsValues[outCardPlayer][table.firstOutColor][i];
				continue;
			}
			if (table.cardsValues[outCardPlayer][table.firstOutColor][i] >= SDHConstants_XT.SDH_PACK_COUNT) {
				if (table.firstOutColor == SDHConstants_XT.SDH_COLOR_MAIN && i < 13) {
					duiziValue[duiziCount] = table._logic.m_cbMainColor * 16 + i;
					duiziCount++;
				} else {
					duiziValue[duiziCount] = table.firstOutColor * 16 + i;
					if (i > 14) { // 如果是大小王 则也要减去大小王的逻辑值
						duiziValue[duiziCount] -= 14;
					} else if (i > 13) { // 如果是A 则要减去A的逻辑值
						if (table.firstOutColor == SDHConstants_XT.SDH_COLOR_MAIN) {
							duiziValue[duiziCount] = table._logic.m_cbMainColor * 16 + i - 13;
						} else {
							duiziValue[duiziCount] -= 13;
						}
					}
					duiziCount++;
				}
			}
			colorCount += table.cardsValues[outCardPlayer][table.firstOutColor][i];
		}
		int colorOutCount = 0;
		for (int i = 0; i < outCardCount; i++) {
			if (table._logic.getCardLogicColor(outCardsData[i]) == table.firstOutColor) {
				colorOutCount++;
			}
		}
		// 当前花色数量不少于首出花色 则必须出首出花色卡牌的相应数量 反之则必须出完该花色
		int shouldOutNumber = colorCount >= firstCount ? firstCount : colorCount;
		if (colorOutCount < shouldOutNumber) {
			return false;
		}
		int outDuiziValue[] = new int[13];
		if (firstType == SDHConstants_XT.SDH_CT_SAME_2 || firstType == SDHConstants_XT.SDH_CT_SAME_4) { // 对子或拖拉机
			table._logic.sortCardList(outCardsData, outCardCount);
			int outDuiziCount = 0;
			for (int i = 0; i < outCardCount; i++) {
				for (int j = i + 1; j < outCardCount; j++) {
					if (table._logic.getRealCard(outCardsData[i]) == table._logic.getRealCard(outCardsData[j])) {
						outDuiziValue[outDuiziCount] = table._logic.getRealCard(outCardsData[i]);
						outDuiziCount++;
						break;
					}
				}
			}
			int shouldOutDuiziCount = duiziCount >= outCardCount / 2 ? outCardCount / 2 : duiziCount;
			if (outDuiziCount < shouldOutDuiziCount) { // 有对必须出对
				return false;
			}
			// 以下是判断首家出拖拉机 之后三个玩家有拖拉机必须出拖拉机 
			// 2018年3月26日 代理要求去掉这个限制
//			if (firstType == SDHConstants_XT.SDH_CT_SAME_4 && duiziCount > shouldOutDuiziCount) {
//				int outSameCount[] = table._logic.getTractorNumber(outDuiziValue, outDuiziCount);
//
//				int sameCount[] = table._logic.getTractorNumber(duiziValue, duiziCount);
//				int len = firstCount / 2;
//				if (outSameCount[len] <= 0) {
//					for (int i = len; i < 13; i++) {
//						if (sameCount[i] > 0) {
//							return false;
//						}
//					}
//				}
//				for (int i = len - 1; i > 0; i--) {
//					if (sameCount[i] > 0 && outSameCount[i] < 0) {
//						return false;
//					}
//				}
//			}
		}

		return true;
	}

	/**
	 * 记录出牌
	 * 
	 * @param seatIndex
	 * @param table
	 */
	public void recordCard(int seatIndex, SDHTable_XT table) {
		int outCount = table.outCardsCount[seatIndex];
		for (int i = 0; i < outCardCount; i++) {
			table.outCards[seatIndex][i + outCount] = outCardsData[i];
		}
		table.outCardsCount[outCardPlayer] += outCardCount;
	}

	/**
	 * 记录得分
	 * 
	 * @param winnerIndex
	 * @param table
	 */
	public void recordScore(int winnerIndex, SDHTable_XT table) {
		int scoreCount = table.freeScoreCardsCount;
		int currentSocreCount = 0;

		if (winnerIndex != table._cur_banker) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < outCardCount; j++) {
					int value = table._logic.getCardValue(outCardsDatas[i][j]);
					if ((0 == value % 5 || 13 == value) && value > 0) { // 5/10/K
																		// 得分
						table.freeScoreCards[currentSocreCount + scoreCount] = outCardsDatas[i][j];
						currentSocreCount++;
					}
				}
			}
			table.freeScoreCardsCount += currentSocreCount;
		}
	}

	@Override
	public boolean handler_player_be_in_room(SDHTable_XT table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SDHConstants_XT.RESPONSE_RECONNECT_DATA);

		TableResponseSdh.Builder tableResponseSdh = TableResponseSdh.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		int min = table.outCardsCount[0];
		int max = table.outCardsCount[0];
		if (table.GRR != null) {
			for (int i = 1; i < table.getTablePlayerNumber(); i++) {
				min = min > table.outCardsCount[i] ? table.outCardsCount[i] : min;
				max = max < table.outCardsCount[i] ? table.outCardsCount[i] : max;
			}
			tableResponseSdh.setCurrentPlayer(table.nextPlayer);
			for (int i = 0; i < SDHConstants_XT.SDH_DIPAI_COUNT; i++) {
				tableResponseSdh.addDiCardsData(seat_index == table._cur_banker ? table.diPai[i] : -2);
			}
			tableResponseSdh.setDiCardCount(SDHConstants_XT.SDH_DIPAI_COUNT);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponseSdh.addTrustee(table.isTrutess(i));
				tableResponseSdh.addOutCardsCount(table.GRR._cur_round_count[i]);
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
				tableResponseSdh.addCardsData(i, cards);
				tableResponseSdh.addCallBankerScore(table.callScore[i]);
				// 回看、分牌
				RoomPlayerResponseSdh.Builder player = RoomPlayerResponseSdh.newBuilder();
				for (int j = 0; j < min; j++) {
					player.addOutCardsData(table.outCards[i][j]);
				}
				player.setOutCardsCount(min);
				for (int j = 0; j < table.freeScoreCardsCount; j++) {
					player.addScoreCardsData(table.freeScoreCards[j]);
				}
				player.setScoreCardsCount(table.freeScoreCardsCount);
				for (int z = 0; z < table.getTablePlayerNumber(); z++) {
					Int32ArrayResponse.Builder value = Int32ArrayResponse.newBuilder();
					for (int j = 0; j <= SDHConstants_XT.SDH_COLOR_COUNT; j++) {
						value.addItem(table._logic.maxCard[z][j]);
					}
					player.addMaxCardXt(value);
				}
				for (int j = 0; j <= SDHConstants_XT.SDH_COLOR_COUNT; j++) {
					player.addMaxCard(table._logic.maxCard[i][j]);
				}
				player.setHasMain(table.hasMain[i]);
				if (!table.hasMain[i] && table.guard[i] == -1) {
					player.setGuard(5);
				} else {
					player.setGuard(table.guard[i]);
				}
				if (this.firstType != SDHConstants_XT.SDH_ERROR_NUMBER && this.firstCount != 0 && table.firstPlayer != -1) { // 当前轮已经有人出牌了
					for (int j = 0; j < this.outCardsDatas[i].length; j++) {
						player.addCurOutCardsData(this.outCardsDatas[i][j]);
					}
					player.setCurOutCardsCount(this.outCardsDatas[i].length);
				}
				tableResponseSdh.addPlayers(player);
			}
			if (this.firstType != SDHConstants_XT.SDH_ERROR_NUMBER && this.firstCount != 0 && table.firstPlayer != -1) { // 当前轮已经有人出牌了
				tableResponseSdh.setBigPlayerSeat(
						table._logic.compareCardArrayWithOutLimit(table, table.getTablePlayerNumber(), this.outCardsDatas, this.firstCount, table.firstPlayer));
			} else {
				tableResponseSdh.setBigPlayerSeat(-1);
			}

			tableResponseSdh.setStall(table.stall);
			tableResponseSdh.setRate(table.rate);
			tableResponseSdh.setDifen(table._di_fen);
			tableResponseSdh.setScore(table.allScore);
			tableResponseSdh.setBankerPlayer(table._cur_banker);
			tableResponseSdh.setMainColor(table._logic.m_cbMainColor);
			tableResponseSdh.setGameStatus(table._game_status);
			tableResponseSdh.setFirstOutPlayer(table.firstPlayer);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponseSdh));
		table.send_response_to_player(seat_index, roomResponse);

		if (table.firstOutColor != SDHConstants_XT.SDH_ERROR_NUMBER && table.firstPlayer != SDHConstants_XT.SDH_ERROR_NUMBER) {
			table.operate_out_card_type();
		}

		int endTime = (int) (SDHConstants_XT.SDH_OPERATOR_TIME - (System.currentTimeMillis() - table.beginTime) / 1000);
		if (seat_index == table.nextPlayer && status == 0 && table.GRR._card_count[seat_index] > 0) {
			if (max > table.outCardsCount[table.nextPlayer]) {
				table.operate_effect_action(table.nextPlayer, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { SDHConstants_XT.Player_Status_OUT_CARDS }, endTime, table.nextPlayer);
			} else if (max == min) {
				table.operate_effect_action(table.nextPlayer, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { SDHConstants_XT.Player_Status_OUT_CARDS }, endTime, table.nextPlayer);
			}
		}
		table.showPlayerOperate(table.nextPlayer, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_OUT_CARDS },
				endTime, seat_index);

		// 通知所有玩家看分牌、回看
		List<Integer> list = Lists.newArrayList();
		if (table.outRound > 0) {
			list.add(SDHConstants_XT.Player_Status_RELOOK);
		}
		list.add(SDHConstants_XT.Player_Status_LOOK_DIPAI);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.freeScoreCardsCount > 0) {
				list.add(SDHConstants_XT.Player_Status_LOOK_SCORE);
			}
			long[] operate = SDHUtil.listToArray(list);
			table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, operate.length, operate, -1, i);
		}

		if (this.outCardPlayer == seat_index && status == 1) {
			if (table.judgeAllhadNoMain() && table._banker_select != outCardPlayer) {
				if (table.guard[outCardPlayer] == SDHConstants_XT.SDH_ERROR_NUMBER) {
					table.sendGuardInfo(outCardPlayer);
				}
			}
		}

		return true;
	}

	public boolean isSuccess() {
		return success;
	}

	/**
	 * 获取自动出牌
	 * 
	 * @return
	 */
	public List<Integer> getOutCard(SDHTable_XT table, int seatIndex) {
		List<Integer> result = Lists.newArrayList();

		if (this.firstType == SDHConstants_XT.SDH_ERROR_NUMBER) { // 首出 出最小的牌
			result.add(table.GRR._cards_data[seatIndex][table.GRR._card_count[seatIndex] - 1]);
		} else {
			int count = 0;
			switch (this.firstType) {
			case SDHConstants_XT.SDH_CT_SINGLE:
				for (int i = table.GRR._card_count[seatIndex] - 1; i >= 0; i--) {
					int card = table.GRR._cards_data[seatIndex][i];
					if (table._logic.getCardLogicColor(card) == table.firstOutColor) {
						result.add(card);
						break;
					}
				}
				if (result.size() == 0) {
					result.add(table.GRR._cards_data[seatIndex][table.GRR._card_count[seatIndex] - 1]);
				}
				break;
			case SDHConstants_XT.SDH_CT_SAME_2:
			case SDHConstants_XT.SDH_CT_SAME_4:
				count = 0;
				for (int i = 0; i < SDHConstants_XT.SDH_ONE_COLOR_COUNT + 4; i++) {
					if (count == this.firstCount) {
						break;
					}
					// 非主牌不计算2、7
					if (table.firstOutColor != SDHConstants_XT.SDH_COLOR_MAIN && (i == table._logic.m_cbNTValue || i == table._logic.m_cbMainValue)) {
						continue;
					} // 主牌上常主或者主值牌数量多于2张, 则寻找其他花色牌对子
					if ((i == table._logic.m_cbNTValue || i == table._logic.m_cbMainValue)
							&& table.cardsValues[seatIndex][table.firstOutColor][i] >= SDHConstants_XT.SDH_PACK_COUNT) {
						for (int j = 0; j < SDHConstants_XT.SDH_COLOR_COUNT; j++) {
							if (table.cardsValues[seatIndex][j][i] == SDHConstants_XT.SDH_PACK_COUNT) {
								result.add(table.firstOutColor * 16 + (i > 13 ? i - 13 : i));
								count += 2;
							}
						}
						continue;
					}
					if (table.cardsValues[outCardPlayer][table.firstOutColor][i] >= SDHConstants_XT.SDH_PACK_COUNT) {
						result.add(table.firstOutColor * 16 + (i > 13 ? i - 13 : i));
						count += 2;
					}
				}
				if (count < this.firstCount) {
					for (int i = table.GRR._card_count[seatIndex] - 1; i >= 0; i--) {
						int card = table.GRR._cards_data[seatIndex][i];
						if (table._logic.getCardLogicColor(card) != table.firstOutColor) {
							result.add(card);
							count++;
							if (count == this.firstCount) {
								break;
							}
						}
					}
				}
				break;
			case SDHConstants_XT.SDH_CT_THROW_CARD:
				count = 0;
				for (int i = table.GRR._card_count[seatIndex] - 1; i >= 0; i--) {
					int card = table.GRR._cards_data[seatIndex][i];
					if (table._logic.getCardLogicColor(card) == table.firstOutColor) {
						result.add(card);
						count++;
						if (count == this.firstCount) {
							break;
						}
					}
				}
				if (count < this.firstCount) {
					for (int i = table.GRR._card_count[seatIndex] - 1; i >= 0; i--) {
						int card = table.GRR._cards_data[seatIndex][i];
						if (table._logic.getCardLogicColor(card) != table.firstOutColor) {
							result.add(card);
							count++;
							if (count == this.firstCount) {
								break;
							}
						}
					}
				}
				break;
			}
		}

		return result;
	}

}
