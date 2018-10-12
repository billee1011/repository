package com.cai.game.sdh.handler;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.SDHClearRoundRunnable;
import com.cai.game.sdh.SDHConstants;
import com.cai.game.sdh.SDHTable;
import com.cai.game.sdh.SDHUtil;
import com.google.common.collect.Lists;

public class SDHHandlerOutCardOperate<T extends SDHTable> extends SDHHandler<T> {

	private static Logger logger = Logger.getLogger(SDHHandlerOutCardOperate.class);

	private boolean success = true;

	public int outCardPlayer = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] outCardsData = new int[SDHConstants.SDH_CARD_COUNT]; // 出牌扑克
	public int outCardCount = 0;

	public int[][] outCardsDatas; // 当前轮出牌信息
	public int outNumber; // 当前轮出牌人数
	public int firstType; // 首出的类型
	public int firstCount; // 首出牌数

	@Override
	public void exe(SDHTable table) {
		table._current_player = outCardPlayer;
		if (outCardCount <= 0) {
			logger.error("出牌数量必须多于一张" + outCardPlayer);
			success = false;
			return;
		}

		if (0 == outNumber) { // 首出玩家出牌校验与记录
			table.currentFirst = outCardPlayer;
			outCardsDatas = new int[table.getTablePlayerNumber()][outCardCount]; // 初始化数据

			// 记录首出牌型
			firstType = table._logic.getOutCardType(table, outCardPlayer, table.getTablePlayerNumber(), outCardsData, outCardCount);
			if (firstType == SDHConstants.SDH_CT_ERROR) {
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

		int winnerSeatIndex = table._logic.compareCardArray(table, table.getTablePlayerNumber(), outCardsDatas, outCardCount, table.currentFirst);
		// 记录出牌信息
		recordCard(outCardPlayer, table);
		// 显示出牌
		table.operate_out_card(outCardPlayer, outCardCount, outCardsData, firstType, GameConstants.INVALID_SEAT, winnerSeatIndex);
		// 收起该玩家的效果通知
		table.operate_effect_action(table._current_player, GameConstants.Effect_Action_Other, 1, new long[] { SDHConstants.Player_Status_OUT_CARDS }, 1,
				table._current_player);

		// 设置下一个出牌用户
		int nextSeat = (outCardPlayer + 1) % table.getTablePlayerNumber();
		// 当前轮所有玩家都已经出完
		if (outNumber == table.getTablePlayerNumber()) {
			int winScore = table._logic.calculationScore(table, outCardsDatas, outCardCount);

			recordScore(winnerSeatIndex, table);
			if (winnerSeatIndex != table._banker_select) { // 闲家大
				if (winScore > 0) {
					table.allScore += winScore; // 总得分
					table.playerScores[winnerSeatIndex] += winScore; // 玩家个人得分
					table.refreshScore(winnerSeatIndex);
				}
			} else { // 庄家大
				if (winScore > 0) {
					table.playerScores[winnerSeatIndex] += winScore;
					table.refreshScore(winnerSeatIndex);
				}
			}

			outNumber = 0;
			table.outRound++;
			table.firstOutColor = SDHConstants.SDH_ERROR_NUMBER;
			table.firstPlayer = SDHConstants.SDH_ERROR_NUMBER;
			this.firstType = SDHConstants.SDH_ERROR_NUMBER;
			// 出牌数等于手牌数 即表示出完牌了
			if (outCardCount == table.GRR._card_count[outCardPlayer]) {
				int type = table._logic.getOutCardType(table, winnerSeatIndex, table.getTablePlayerNumber(), outCardsDatas[winnerSeatIndex], outCardCount);
				int color = table._logic.getCardLogicColor(outCardsDatas[winnerSeatIndex][0]);
				if (!table.has_rule(GameConstants.GAME_RULE_SDH_QUDIAO6)) {
					if (color == SDHConstants.SDH_COLOR_MAIN) { // 如果是主牌 则可看底牌
						int difen = table._logic.calculationScore(table, table.diPai, SDHConstants.SDH_DIPAI_COUNT);
						if (difen > 0 && winnerSeatIndex != table._banker_select) {
							if (type == 2 || type == 4) {
								table.allScore += difen * outCardCount;
							} else {
								table.allScore += difen;
							}
						}
					}
				}
				// 结算
				table._handler_finish.exe(table);
				table.reconnectOutCards = SDHConstants.SDH_ERROR_NUMBER; // 清除本轮出牌状态
				return;
			}
			nextSeat = winnerSeatIndex;
			table.reconnectOutCards = SDHConstants.SDH_ERROR_NUMBER; // 清除本轮出牌状态
			GameSchedule.put(new SDHClearRoundRunnable(table.getRoom_id(), nextSeat, 1), 1000, TimeUnit.MILLISECONDS);

			// 通知所有玩家看分牌、回看
			List<Integer> list = Lists.newArrayList();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table.outRound > 0) {
					list.add(SDHConstants.Player_Status_RELOOK);
				}
				if (!table.has_rule(GameConstants.GAME_RULE_SDH_QUDIAO6)) {
					list.add(SDHConstants.Player_Status_LOOK_DIPAI);
				}
				if (table.scoreCardsCount[i] > 0) {
					list.add(SDHConstants.Player_Status_LOOK_SCORE);
				}
				long[] operate = SDHUtil.listToArray(list);
				table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, operate.length, operate, 1, i);
				list.clear();
			}
		}

		int delay = 0;
		if (table.firstPlayer == SDHConstants.SDH_ERROR_NUMBER) {
			delay = 1000;
		}
		GameSchedule.put(new SDHClearRoundRunnable(table.getRoom_id(), nextSeat, 2), delay, TimeUnit.MILLISECONDS);

		// 删除手牌
		table._logic.removeCardsByData(table.GRR._cards_data[outCardPlayer], table.GRR._card_count[outCardPlayer], outCardsData, outCardCount, 0);
		table.GRR._card_count[outCardPlayer] -= outCardCount;
		table.refreshPlayerCards(outCardPlayer);
		if (firstType == SDHConstants.SDH_ERROR_NUMBER) {
			outCardsDatas = null;
			table.recordMaxCard();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) { // 刷新所有玩家手牌
				table.refreshPlayerCards(i);
			}
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
	public boolean validateOtherPlayerOutCards(SDHTable table) {
		int colorCount = 0; // 当前玩家拥有首出玩家花色卡牌数据
		int duiziCount = 0; // 当前玩家拥有首出玩家花色卡牌对子数据
		for (int i = 0; i < SDHConstants.SDH_ONE_COLOR_COUNT + 4; i++) {
			// 非主牌不计算2、7
			if (table.firstOutColor != SDHConstants.SDH_COLOR_MAIN && (i == table._logic.m_cbNTValue || i == table._logic.m_cbMainValue)) {
				continue;
			} // 主牌上常主或者主值牌数量多于2张, 则寻找其他花色牌对子
			if ((i == table._logic.m_cbNTValue || i == table._logic.m_cbMainValue)
					&& table.cardsValues[outCardPlayer][table.firstOutColor][i] >= SDHConstants.SDH_PACK_COUNT) {
				for (int j = 0; j < SDHConstants.SDH_COLOR_COUNT; j++) {
					if (table.cardsValues[outCardPlayer][j][i] == SDHConstants.SDH_PACK_COUNT) {
						duiziCount++;
					}
				}
				colorCount += table.cardsValues[outCardPlayer][table.firstOutColor][i];
				continue;
			}
			if (table.cardsValues[outCardPlayer][table.firstOutColor][i] >= SDHConstants.SDH_PACK_COUNT) {
				duiziCount++;
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
		if (firstType == SDHConstants.SDH_CT_SAME_2 || firstType == SDHConstants.SDH_CT_SAME_4) { // 对子或拖拉机
			table._logic.sortCardList(outCardsData, outCardCount);
			int outDuiziCount = 0;
			if (outCardsData[0] == outCardsData[1]) {
				outDuiziCount++;
			}
			if (outCardCount == 4 && (outCardsData[2] == outCardsData[3] || outCardsData[2] == outCardsData[1])) {
				outDuiziCount++;
			}
			int shouldOutDuiziCount = duiziCount >= firstType / 2 ? firstType / 2 : duiziCount;
			if (outDuiziCount < shouldOutDuiziCount) { // 有对必须出对
				return false;
			}
		}

		return true;
	}

	/**
	 * 记录出牌
	 * 
	 * @param seatIndex
	 * @param table
	 */
	public void recordCard(int seatIndex, SDHTable table) {
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
	public void recordScore(int winnerIndex, SDHTable table) {
		int scoreCount = table.scoreCardsCount[winnerIndex];
		int currentSocreCount = 0;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			for (int j = 0; j < outCardCount; j++) {
				int value = table._logic.getCardValue(outCardsDatas[i][j]);
				if (0 == value % 5 || 13 == value) { // 5/10/K 得分
					table.scoreCards[winnerIndex][currentSocreCount + scoreCount] = outCardsDatas[i][j];
					currentSocreCount++;
				}
			}
		}
		table.scoreCardsCount[winnerIndex] += currentSocreCount;
	}

	public boolean isSuccess() {
		return success;
	}

}
