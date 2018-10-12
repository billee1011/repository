/**
 * 
 */
package com.cai.game.sdh;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_XT;
import com.cai.common.constant.game.sdh.SDHConstants_YYBS;
import com.cai.common.util.RandomUtil;

public class SDHGameLogic {

	// 常主数值
	public int m_cbNTValue; // 常主数值
	public int m_cbMainColor; // 主牌花色
	public int m_cbMainValue; // 主牌数值
	public int maxCard[][]; // 记录每个玩家每个花色最大的牌 用来作甩牌判断
	public final int mCbSortVolue[] = { 0, 20, 60, 120, 400 }; // 方片、梅花、红桃、黑桃、主牌

	public SDHGameLogic(int m_cbNTValue, int m_cbMainColor, int m_cbMainValue) {
		// 初始化数据
		this.m_cbNTValue = m_cbNTValue;
		this.m_cbMainColor = m_cbMainColor;
		this.m_cbMainValue = m_cbMainValue;
	}

	public int getMain6() {
		int card = 0;
		switch (m_cbMainColor) {
		case 0:
			card = 0x06;
			break;
		case 1:
			card = 0x16;
			break;
		case 2:
			card = 0x26;
			break;
		case 3:
			card = 0x36;
			break;
		}
		return card;
	}

	/**
	 * 获取出牌的牌型(有限制、最多4张牌)
	 * 
	 * @param seatIndex
	 * @param playerCount
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int getOutCardType(SDHTable table, int seatIndex, int playerCount, int cbCardData[], int cbCardCount) {
		switch (cbCardCount) {
		case 1: // 单牌
			return SDHConstants_XT.SDH_CT_SINGLE;
		case 2: // 对子 或者 甩牌
			if (cbCardData[0] == cbCardData[1]) {
				return SDHConstants_XT.SDH_CT_SAME_2;
			}
			return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 3); // 甩牌判断
		case 4: // 拖牌 或者甩牌
			sortCardList(cbCardData, cbCardCount); // 排序
			if (getRealCard(cbCardData[0]) == getRealCard(cbCardData[1])
					&& getRealCard(cbCardData[2]) == getRealCard(cbCardData[3])) { // 拖拉机
				int firstColor = getCardColor(cbCardData[0]), lastColor = getCardColor(cbCardData[3]);
				int firstValue = getCardLogicValue(cbCardData[0]), lastValue = getCardLogicValue(cbCardData[3]);
				if (firstColor == SDHConstants_XT.SDH_COLOR_MAIN) { // 大小王
					if (firstValue == 2 && (lastColor == firstColor && lastValue == 14)) { // 大王小王
						return SDHConstants_XT.SDH_CT_SAME_4;
					} else if (firstValue == 14 && (lastColor == m_cbMainColor && lastValue == m_cbMainValue)) { // 小王主7
						return SDHConstants_XT.SDH_CT_SAME_4;
					}
					return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 3); // 甩牌判断
				} else if (firstValue == m_cbMainValue || firstValue == m_cbNTValue) { // 主花色/主值/常主值
					if (firstValue == lastValue && firstColor == m_cbMainColor && lastColor != m_cbMainColor) { // 正7副7
						return SDHConstants_XT.SDH_CT_SAME_4;
					} else if (firstValue == m_cbNTValue && firstColor != m_cbMainColor && lastColor == m_cbMainColor
							&& lastValue == 14) { // 主A副2
						return SDHConstants_XT.SDH_CT_SAME_4;
					} else if (firstValue == m_cbMainValue && firstColor != m_cbMainColor && lastColor == m_cbMainColor
							&& lastValue == m_cbNTValue) { // 主2副7
						return SDHConstants_XT.SDH_CT_SAME_4;
					}
					return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 3); // 甩牌判断
				} else if (firstValue == lastValue + 1
						|| (firstValue == m_cbMainValue + 1 && m_cbMainValue == lastValue + 1)) {
					return SDHConstants_XT.SDH_CT_SAME_4;
				}
				judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 3); // 甩牌判断
			}
			return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 3); // 甩牌判断
		default:
			return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 3); // 甩牌判断
		}
	}

	/**
	 * 获取出牌的牌型(无限制、可以随意出)
	 * 
	 * @param table
	 * @param seatIndex
	 * @param playerCount
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int getOutCardTypeWithOutLimit(SDHTable table, int seatIndex, int playerCount, int cbCardData[],
			int cbCardCount) {
		switch (cbCardCount) {
		case 1: // 单牌
			return SDHConstants_XT.SDH_CT_SINGLE;
		case 2: // 对子 或者 甩牌
			if (getRealCard(cbCardData[0]) == getRealCard(cbCardData[1])) {
				return SDHConstants_XT.SDH_CT_SAME_2;
			}
			return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 0); // 甩牌判断
		case 4: // 拖牌 或者甩牌
			sortCardList(cbCardData, cbCardCount); // 排序
			if (getRealCard(cbCardData[0]) == getRealCard(cbCardData[1])
					&& getRealCard(cbCardData[2]) == getRealCard(cbCardData[3])
					&& isSameColor(cbCardData[0], cbCardData[3])) { // 拖拉机
				int firstColor = getCardColor(cbCardData[0]), lastColor = getCardColor(cbCardData[3]);
				int firstValue = getCardLogicValue(cbCardData[0]), lastValue = getCardLogicValue(cbCardData[3]);
				if (firstColor == SDHConstants_XT.SDH_COLOR_MAIN) { // 大小王
					if (firstValue == 2 && (lastColor == firstColor && lastValue == 14)) { // 大王小王
						return SDHConstants_XT.SDH_CT_SAME_4;
					} else if (firstValue == 14 && (lastColor == m_cbMainColor && lastValue == m_cbMainValue)) { // 小王主7
						return SDHConstants_XT.SDH_CT_SAME_4;
					}
					return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 0); // 甩牌判断
				} else if (firstValue == m_cbMainValue || firstValue == m_cbNTValue) { // 主花色/主值/常主值
					if (firstValue == lastValue && firstColor == m_cbMainColor && lastColor != m_cbMainColor) { // 正7副7
						return SDHConstants_XT.SDH_CT_SAME_4;
					} else if (firstValue == m_cbNTValue && firstColor != m_cbMainColor && lastColor == m_cbMainColor
							&& lastValue == 14) { // 主A副2
						return SDHConstants_XT.SDH_CT_SAME_4;
					} else if (firstValue == m_cbMainValue && firstColor != m_cbMainColor && lastColor == m_cbMainColor
							&& lastValue == m_cbNTValue) { // 主2副7
						return SDHConstants_XT.SDH_CT_SAME_4;
					}
					return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 0); // 甩牌判断
				} else if(table._game_type_index == GameConstants.GAME_TYPE_YI_YANG_BA_SHI){//益阳巴十及三打哈拖拉机判断区分
					if(judgeTractor_yybs(cbCardData, cbCardCount) == SDHConstants_XT.SDH_CT_SAME_4){
						return SDHConstants_XT.SDH_CT_SAME_4;
					}
				} else if (firstValue == lastValue + 1
						|| (firstValue == m_cbMainValue + 1 && m_cbMainValue == lastValue + 2)) {
					return SDHConstants_XT.SDH_CT_SAME_4;
				}
				judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 0); // 甩牌判断
			}
			return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 0); // 甩牌判断
		default:
			if (cbCardCount % 2 == 0) {
				// WalkerGeek 益阳巴十及三打哈拖拉机判断区分
				int type = SDHConstants_XT.SDH_CT_ERROR;
				if (table._game_type_index == GameConstants.GAME_TYPE_YI_YANG_BA_SHI) {
					type = judgeTractor_yybs(cbCardData, cbCardCount); // 拖拉机判断
				} else {
					type = judgeTractor(cbCardData, cbCardCount); // 拖拉机判断
				}
				if (type != SDHConstants_XT.SDH_CT_ERROR) {
					return type;
				}
			}
			return judgeThrowCards(table, seatIndex, playerCount, cbCardData, cbCardCount, 0); // 甩牌判断
		}
	}

	/**
	 * 判断是否同花色
	 * 
	 * @param firstColor
	 * @param lastColor
	 * @return
	 */
	public boolean isSameColor(int firstCard, int lastCard) {
		int firstColor = getCardColor(firstCard), lastColor = getCardColor(lastCard);
		if ((isTheMain(firstCard) || firstColor == m_cbMainColor)
				&& (isTheMain(lastCard) || lastColor == m_cbMainColor)) {
			return true;
		}
		if (firstColor == lastColor) {
			return true;
		}
		return false;
	}

	/**
	 * 甩牌判断 只有主牌能甩牌 且最小的一张都要比其他玩家最大的牌大
	 * 
	 * @param seatIndex
	 * @param playerCount
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cardLimit
	 *            甩牌数量上限 (0表示没有上限)
	 * @return
	 */
	public int judgeThrowCards(SDHTable table, int seatIndex, int playerCount, int cbCardData[], int cbCardCount,
			int cardLimit) {
		if (table._game_type_index == GameConstants.GAME_TYPE_YI_YANG_BA_SHI) {
			return judgeThrowCards_yybs(table, seatIndex, playerCount, cbCardData, cbCardCount, cardLimit);
		} 
		
		if (cardLimit > 0 && cbCardCount > cardLimit) { // 甩牌数量上限限制
			return SDHConstants_XT.SDH_CT_ERROR;
		}
		int firstValue = getCardValue(cbCardData[0]);
		int firstColor = getCardColor(cbCardData[0]);

		// 第一张牌主花色
		if (firstColor == SDHConstants_XT.SDH_COLOR_MAIN || firstColor == m_cbMainColor || firstValue == m_cbMainValue
				|| firstValue == m_cbNTValue) {
			firstColor = SDHConstants_XT.SDH_COLOR_MAIN;
		}
		for (int i = 1; i < cbCardCount; i++) {
			int value = getCardValue(cbCardData[i]);
			int color = getCardColor(cbCardData[i]);

			// 主花色则所有牌都要是主牌
			if (firstColor == SDHConstants_XT.SDH_COLOR_MAIN) {
				if (!(color == SDHConstants_XT.SDH_COLOR_MAIN || color == m_cbMainColor || value == m_cbMainValue
						|| value == m_cbNTValue)) {
					return SDHConstants_XT.SDH_CT_ERROR;
				}
			} else { // 非主花色时 其他牌都应与第一张牌同花色
				if (firstColor != color) {
					return SDHConstants_XT.SDH_CT_ERROR;
				}
			}
		}
		sortCardList(cbCardData, cbCardCount);

		for (int i = 0; i < playerCount; i++) {
			if (i != seatIndex) {
				// 主牌判断 最小的牌不小于其他玩家相同花色的最大的牌
				if (firstColor == SDHConstants_XT.SDH_COLOR_MAIN) {
					if (compareCardData(getRealCard(cbCardData[cbCardCount - 1]),
							getRealCard(maxCard[i][firstColor]))) {
						return SDHConstants_XT.SDH_CT_ERROR;
					}
					continue;
				}
				if (maxCard[i][SDHConstants_XT.SDH_COLOR_MAIN] > 0) { // 有主牌就不能甩
					return SDHConstants_XT.SDH_CT_ERROR;
				}

				// 非主牌判断 没有该花色 且主牌数量少于出牌数量 则可以甩牌
				if (maxCard[i][firstColor] == 0) {
					int countMain = 0;
					for (int j = 0; j < SDHConstants_XT.SDH_ONE_COLOR_COUNT + 4; j++) {
						countMain += table.cardsValues[i][SDHConstants_XT.SDH_COLOR_MAIN][j];
					}
					if (countMain < cbCardCount) {
						continue;
					}
					return SDHConstants_XT.SDH_CT_ERROR;
				}
				if (compareCardData(getRealCard(cbCardData[cbCardCount - 1]), getRealCard(maxCard[i][firstColor]))) {
					return SDHConstants_XT.SDH_CT_ERROR;
				}
			}
		}

		return SDHConstants_XT.SDH_CT_THROW_CARD;
	}
	
	
	/**
	 * 甩牌判断 只有主牌能甩牌 且最小的一张都要比其他玩家最大的牌大
	 * 
	 * @param seatIndex
	 * @param playerCount
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cardLimit
	 *            甩牌数量上限 (0表示没有上限)
	 * @return
	 */
	public int judgeThrowCards_yybs(SDHTable table, int seatIndex, int playerCount, int cbCardData[], int cbCardCount,
			int cardLimit) {
		if (cardLimit > 0 && cbCardCount > cardLimit) { // 甩牌数量上限限制
			return SDHConstants_YYBS.SDH_CT_ERROR;
		}
		
		if (table.hasFriend ) { //一打三模式才能甩牌 
			return SDHConstants_YYBS.SDH_CT_ERROR_THROW_FRIEND;
		}
		if(table._cur_banker != seatIndex){//庄家才能甩牌
			return SDHConstants_YYBS.SDH_CT_ERROR_THROW_ZHUAN;
		}
		
		int firstValue = getCardValue(cbCardData[0]);
		int firstColor = getCardColor(cbCardData[0]);

		// 第一张牌主花色
		if (firstColor == SDHConstants_YYBS.SDH_COLOR_MAIN || firstColor == m_cbMainColor || firstValue == m_cbMainValue
				|| firstValue == m_cbNTValue) {
			firstColor = SDHConstants_YYBS.SDH_COLOR_MAIN;
		}
		for (int i = 1; i < cbCardCount; i++) {
			int value = getCardValue(cbCardData[i]);
			int color = getCardColor(cbCardData[i]);

			// 主花色则所有牌都要是主牌
			if (firstColor == SDHConstants_YYBS.SDH_COLOR_MAIN) {
				if (!(color == SDHConstants_YYBS.SDH_COLOR_MAIN || color == m_cbMainColor || value == m_cbMainValue
						|| value == m_cbNTValue)) {
					return SDHConstants_YYBS.SDH_CT_ERROR;
				}
			} else { // 非主花色时 其他牌都应与第一张牌同花色
				if (firstColor != color) {
					return SDHConstants_YYBS.SDH_CT_ERROR;
				}
			}
		}
		sortCardList(cbCardData, cbCardCount);

		for (int i = 0; i < playerCount; i++) {
			if (i != seatIndex) {
				// 主牌判断 最小的牌不小于其他玩家相同花色的最大的牌
				if (firstColor == SDHConstants_YYBS.SDH_COLOR_MAIN) {
					if (compareCardData(getRealCard(cbCardData[cbCardCount - 1]),
							getRealCard(maxCard[i][firstColor]))) {
						return SDHConstants_YYBS.SDH_CT_ERROR_THROW;
					}
					continue;
				}
				if (maxCard[i][SDHConstants_YYBS.SDH_COLOR_MAIN] > 0) { // 有主牌就不能甩
					return SDHConstants_YYBS.SDH_CT_ERROR_THROW;
				}

				// 非主牌判断 没有该花色 且主牌数量少于出牌数量 则可以甩牌
				if (maxCard[i][firstColor] == 0) {
					int countMain = 0;
					for (int j = 0; j < SDHConstants_YYBS.SDH_ONE_COLOR_COUNT + 4; j++) {
						countMain += table.cardsValues[i][SDHConstants_YYBS.SDH_COLOR_MAIN][j];
					}
					if (countMain < cbCardCount) {
						continue;
					}
					return SDHConstants_YYBS.SDH_CT_ERROR_THROW;
				}
				if (compareCardData(getRealCard(cbCardData[cbCardCount - 1]), getRealCard(maxCard[i][firstColor]))) {
					return SDHConstants_YYBS.SDH_CT_ERROR_THROW;
				}
			}
		}

		return SDHConstants_YYBS.SDH_CT_THROW_CARD;
	}

	public int judgeTractor(int cbCardData[], int cbCardCount) {
		sortCardList(cbCardData, cbCardCount);
		int len = cbCardCount / 2;
		int cardNumber[] = new int[len];
		int k = 0;
		int logicColor = getCardLogicColor(getRealCard(cbCardData[0]));
		for (int i = 0; i < cbCardCount; i += 2) {
			if (logicColor != getCardLogicColor(getRealCard(cbCardData[i]))) {
				return SDHConstants_XT.SDH_CT_ERROR;
			}
			if (getRealCard(cbCardData[i]) != getRealCard(cbCardData[i + 1])) { // 不是对子
				return SDHConstants_XT.SDH_CT_ERROR;
			}
			cardNumber[k++] = getTractorValue(getRealCard(cbCardData[i]));
		}
		for (int i = 0; i < len; i++) {
			int airplanceLen = 1;
			for (int j = i + 1; j < len; j++) {
				if (cardNumber[j] != (cardNumber[i] - airplanceLen)) { // 不连续
					break;
				}
				if (cardNumber[j] <= 8 && cardNumber[i] > 8) { // 主牌跟副牌不能连
					break;
				}
				airplanceLen++;
			}
			if (airplanceLen == len) {
				return SDHConstants_XT.SDH_CT_SAME_4;
			}
			i += airplanceLen - 1;
		}

		return SDHConstants_XT.SDH_CT_ERROR;
	}

	/**
	 * 卡牌数值转换 大王、小王、主7、副7、主2、副2(22-17) 主A-5(16-9) 副A-5(8-1)
	 * 
	 * @param cardData
	 * @return
	 */
	public int getTractorValue(int cardData) {
		int color = getCardColor(cardData);
		int value = getCardValue(cardData);
		if (color == 4) { // 大小王
			return 20 + value; // 小王value = 1 排序值为21
		} else if (value == m_cbNTValue) { // 常主2
			if (color == m_cbMainColor) {
				return 18;
			} else {
				return 17;
			}
		} else if (value == m_cbMainValue) { // 常主7
			if (color == m_cbMainColor) {
				return 20;
			} else {
				return 19;
			}
		} else {
			int v = getCardLogicValue(cardData);
			if (v == 5) {
				v = 1;
			} else {
				v = v - 6;
			}
			if (color == m_cbMainColor) {
				v += 8;
			}
			return v;
		}
	}

	public int judgeTractor_yybs(int cbCardData[], int cbCardCount) {
		sortCardList(cbCardData, cbCardCount);
		int len = cbCardCount / 2;
		int cardNumber[] = new int[len];
		int k = 0;
		int logicColor = getCardLogicColor(getRealCard(cbCardData[0]));
		for (int i = 0; i < cbCardCount; i += 2) {
			if (logicColor != getCardLogicColor(getRealCard(cbCardData[i]))) {
				return SDHConstants_XT.SDH_CT_ERROR;
			}
			if (getRealCard(cbCardData[i]) != getRealCard(cbCardData[i + 1])) { // 不是对子
				return SDHConstants_XT.SDH_CT_ERROR;
			}
			cardNumber[k++] = getTractorValue_yybs(getRealCard(cbCardData[i]));
		}
		for (int i = 0; i < len; i++) {
			int airplanceLen = 1;
			for (int j = i + 1; j < len; j++) {
				if (cardNumber[j] != (cardNumber[i] - airplanceLen)) { // 不连续
					break;
				}
				if (cardNumber[j] <= 9 && cardNumber[i] > 9) { // 主牌跟副牌不能连
																// 9为定制分界线
					break;
				}
				airplanceLen++;
			}
			if (airplanceLen == len) {
				return SDHConstants_XT.SDH_CT_SAME_4;
			}
			i += airplanceLen - 1;
		}

		return SDHConstants_XT.SDH_CT_ERROR;
	}

	/**
	 * 卡牌数值转换 大王、小王、主10、副10、主2、副2(24-19) 主A-5(18-10) 副A-5(9-1) 益阳巴十
	 * 
	 * @param cardData
	 * @return
	 */
	public int getTractorValue_yybs(int cardData) {
		int color = getCardColor(cardData);
		int value = getCardValue(cardData);
		if (color == 4) { // 大小王
			return 22 + value; // 小王value = 1 排序值为21
		} else if (value == m_cbNTValue) { // 常主2
			if (color == m_cbMainColor) {
				return 20;
			} else {
				return 19;
			}
		} else if (value == m_cbMainValue) { // 常主7
			if (color == m_cbMainColor) {
				return 22;
			} else {
				return 21;
			}
		} else {
			int v = getCardLogicValue(cardData);
			int l = 4;
			if (v >= 11) {
				l = 5;
			}
			v = v - l;
			if (color == m_cbMainColor) {
				v += 9;
			}
			return v;
		}
	}

	public int[] getTractorNumber(int[] cardDatas, int cardCount) {
		sortCardList(cardDatas, cardCount);
		int result[] = new int[13];

		int cardNumber[] = new int[cardCount];
		for (int i = 0; i < cardCount; i++) {
			cardNumber[i] = getTractorValue(cardDatas[i]);
		}
		for (int i = 0; i < cardCount; i++) {
			int airplanceLen = 1;
			for (int j = i + 1; j < cardCount; j++) {
				if (cardNumber[j] != (cardNumber[i] - airplanceLen)) { // 不连续
					break;
				}
				if (cardNumber[j] <= 8 && cardNumber[i] > 8) { // 主牌跟副牌不能连
					break;
				}
				airplanceLen++;
			}
			result[airplanceLen]++;
			i += airplanceLen - 1;
		}

		return result;
	}
	
	public int[] getTractorNumber_yybs(int[] cardDatas, int cardCount) {
		sortCardList(cardDatas, cardCount);
		int result[] = new int[13];

		int cardNumber[] = new int[cardCount];
		for (int i = 0; i < cardCount; i++) {
			cardNumber[i] = getTractorValue_yybs(cardDatas[i]);
		}
		
		for (int i = 0; i < cardCount; i++) {
			int airplanceLen = 1;
			for (int j = i + 1; j < cardCount; j++) {
				if (cardNumber[j] != (cardNumber[i] - airplanceLen)) { // 不连续
					break;
				}
				if (cardNumber[j] <= 9 && cardNumber[i] > 9) { // 主牌跟副牌不能连
																// 9为定制分界线
					break;
				}
				airplanceLen++;
			}
			result[airplanceLen]++;
			i += airplanceLen - 1;
		}
		return result;
	}

	/**
	 * 
	 * @param firstCard
	 *            第一张卡牌的数据
	 * @param nextCardValue
	 *            第二张牌的值
	 * @return
	 */
	public boolean compareSameColorCardData(int firstCard, int nextCardValue) {
		return nextCardValue > getCardLogicValue(firstCard);
	}

	/**
	 * 判断出的是否是同类型的牌 主牌可以不同花色
	 * 
	 * @param cbCardData
	 *            扑克数组
	 * @param cbCardCount
	 *            扑克长度
	 * @return 花色 0-4 方-黑、主
	 */
	public int getCardLogicColor(int cbCardData[], int cbCardCount) {
		// 获取花色
		int cbFirstColor = getCardColor(cbCardData[0]);
		int cbSecondColor = getCardColor(cbCardData[cbCardCount - 1]);

		return cbFirstColor == cbSecondColor ? cbFirstColor : SDHConstants_XT.LOGIC_MASK_COLOR;
	}

	public int getCardLogicColor(int cardData) {
		int color = getCardColor(cardData);
		int value = getCardValue(cardData);

		if (color == m_cbMainColor || value == m_cbMainValue || value == m_cbNTValue) {
			return SDHConstants_XT.SDH_COLOR_MAIN;
		}

		return color;
	}

	/**
	 * 两张牌是否相连
	 * 
	 * @param cbFirstCard
	 *            第一张牌
	 * @param cbNextCard
	 *            后二张牌
	 * @return
	 */
	public boolean isLineValue(int cbFirstCard, int cbNextCard) {
		return getCardLogicValue(cbNextCard) == (getCardLogicValue(cbFirstCard) + 1);
	}

	/**
	 * 对比所有玩家出牌的大小
	 * 
	 * @param playerCount
	 * @param cbOutCardData
	 *            出牌扑克
	 * @param cbCardCount
	 *            出牌数量
	 * @param wFirstIndex
	 *            首家出牌位置
	 * @return
	 */
	public int compareCardArray(SDHTable table, int playerCount, int cbOutCardData[][], int cbCardCount,
			int wFirstIndex) {
		// 变量定义
		int wWinnerIndex = wFirstIndex;
		int cbCardType = getOutCardType(table, wFirstIndex, playerCount, cbOutCardData[wFirstIndex], cbCardCount);

		// 对比扑克
		switch (cbCardType) {
		case SDHConstants_XT.SDH_CT_SINGLE: // 单牌类型
		case SDHConstants_XT.SDH_CT_SAME_2: // 对牌类型
		case SDHConstants_XT.SDH_CT_SAME_4: // 四牌类型
			int loop = 1;
			do {
				int wUserIndex = (wFirstIndex + loop++) % playerCount;
				// 牌型不一致、跳过
				if (getOutCardType(table, wUserIndex, playerCount, cbOutCardData[wUserIndex],
						cbCardCount) != cbCardType) {
					continue;
				}
				if (compareCardData(cbOutCardData[wWinnerIndex][0], cbOutCardData[wUserIndex][0])) {
					wWinnerIndex = wUserIndex;
				}
			} while (loop < playerCount);
			return wWinnerIndex;
		case SDHConstants_XT.SDH_CT_THROW_CARD: // 甩牌类型 首出玩家最大
			return wWinnerIndex;
		}

		return wFirstIndex;
	}

	public int compareCardArrayWithOutLimit(SDHTable table, int playerCount, int cbOutCardData[][], int cbCardCount,
			int wFirstIndex) {
		// 变量定义
		int wWinnerIndex = wFirstIndex;
		int cbCardType = getOutCardTypeWithOutLimit(table, wFirstIndex, playerCount, cbOutCardData[wFirstIndex],
				cbCardCount);

		// 对比扑克
		switch (cbCardType) {
		case SDHConstants_XT.SDH_CT_SINGLE: // 单牌类型
		case SDHConstants_XT.SDH_CT_SAME_2: // 对牌类型
		case SDHConstants_XT.SDH_CT_SAME_4: // 四牌类型
			int loop = 1;
			do {
				int wUserIndex = (wFirstIndex + loop++) % playerCount;
				// 牌型不一致、跳过
				if (getOutCardTypeWithOutLimit(table, wUserIndex, playerCount, cbOutCardData[wUserIndex],
						cbCardCount) != cbCardType) {
					continue;
				}
				if (compareCardData(cbOutCardData[wWinnerIndex][0], cbOutCardData[wUserIndex][0])) {
					wWinnerIndex = wUserIndex;
				}
			} while (loop < playerCount);
			return wWinnerIndex;
		case SDHConstants_XT.SDH_CT_THROW_CARD: // 甩牌类型 首出玩家最大
			return wWinnerIndex;
		}

		return wFirstIndex;
	}

	/**
	 * 对比两组扑克大小
	 * 
	 * @param cbFirstCardData
	 *            第一组
	 * @param cbNextCardData
	 *            下一组
	 * @return
	 */
	public boolean compareCardData(int cbFirstCardData, int cbNextCardData) {
		if (cbNextCardData <= 0) {
			return false;
		}
		// 获取花色
		int cbLogicColorNext = getCardLogicColor(cbNextCardData);
		int cbLogicColorFirst = getCardLogicColor(cbFirstCardData);
		// 花色一致
		if (cbLogicColorNext == cbLogicColorFirst) {
			// 且是主牌
			if (cbLogicColorFirst == SDHConstants_XT.SDH_COLOR_MAIN) {
				return getMainCardValue(cbFirstCardData) < getMainCardValue(cbNextCardData);
			}
			return getCardSortOrder(cbNextCardData) > getCardSortOrder(cbFirstCardData);
		}
		// 花色不一致 则主牌大
		return cbLogicColorNext == SDHConstants_XT.SDH_COLOR_MAIN;
	}

	public int getMainCardValue(int card) {
		int value = getCardValue(card);
		int color = getCardColor(card);

		if (color == SDHConstants_XT.SDH_COLOR_MAIN) {
			value += 24;
		} else if (value == 1) {
			value += 13;
		} else if (value == m_cbNTValue || value == m_cbMainValue) {
			value += 13;
			if (getCardColor(card) == m_cbMainColor) {
				value += 1;
			}
		}

		return value;
	}

	/**
	 * 洗牌
	 * 
	 * @param return_cards
	 *            洗牌结果
	 * @param cards
	 *            原始牌
	 */
	public void randomCardData(int return_cards[], final int cards[]) {
		int cardCount = return_cards.length;
		int card_data[] = new int[cardCount];
		for (int i = 0; i < cardCount; i++) {
			card_data[i] = cards[i];
		}
		randomCards(card_data, return_cards, cardCount);
	}

	// 混乱准备
	private static void randomCards(int card_data[], int return_cards[], int cardCount) {
		// 混乱扑克
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (cardCount - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[cardCount - bRandCount];
		} while (bRandCount < cardCount);

	}

	/***
	 * 排列扑克
	 * 
	 * @param card_date
	 * @param cardCount
	 * @return
	 */
	public int[] sortCardList(int cbCardData[], int cbCardCount) {
		int cbSortValue[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			cbSortValue[i] = getCardSortOrder(cbCardData[i]);
		}

		// 排序操作
		int bLast = cbCardCount - 1;
		do {
			for (int i = 0; i < bLast; i++) {
				if ((cbSortValue[i] < cbSortValue[i + 1])
						|| ((cbSortValue[i] == cbSortValue[i + 1]) && (cbCardData[i] < cbCardData[i + 1]))) {
					// 交换位置
					int temp = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = temp;

					temp = cbSortValue[i];
					cbSortValue[i] = cbSortValue[i + 1];
					cbSortValue[i + 1] = temp;
				}
			}
			bLast--;
		} while (bLast > 0);

		return cbCardData;
	}

	public int getCardSortOrder(int cardData) {
		int tmpCardData = cardData & 0x0FF;
		// 逻辑数值
		int cbCardColor = getCardColor(tmpCardData);
		int cbCardValue = getCardValue(tmpCardData);

		if (cbCardColor == SDHConstants_XT.SDH_COLOR_MAIN) {
			return 70 * 10 + tmpCardData;
		}
		int sort[] = { 0, 70, 70 * 2, 70 * 3 };
		if (m_cbMainColor > 0 && m_cbMainColor < 3) {
			if (m_cbMainColor < 2) {
				int temp = sort[m_cbMainColor];
				sort[m_cbMainColor] = sort[m_cbMainColor + 2];
				sort[m_cbMainColor + 2] = temp;
			} else {
				int temp = sort[m_cbMainColor];
				sort[m_cbMainColor] = sort[m_cbMainColor - 2];
				sort[m_cbMainColor - 2] = temp;
			}
		}
		// 主值
		if (cbCardValue == m_cbMainValue) {
			int value = 70 * 8 + tmpCardData;
			if (cbCardColor == m_cbMainColor) {
				value += 70;
			} else if (m_cbMainColor > 0 && m_cbMainColor < 3) {
				if (m_cbMainColor == 1 && cbCardColor == 3) { // 梅花主
					value -= 32;
				} else if (m_cbMainColor == 2 && cbCardColor == 0) { // 红桃主
					value += 32;
				}
			}
			return value;
		}
		// 常主
		if (cbCardValue == m_cbNTValue) {
			int cbSortValue = 70 * 6;
			if (cbCardColor == m_cbMainColor) {
				cbSortValue += 70;
			} else if (m_cbMainColor > 0 && m_cbMainColor < 3) {
				if (m_cbMainColor == 1 && cbCardColor == 3) { // 梅花主
					tmpCardData -= 32;
				} else if (m_cbMainColor == 2 && cbCardColor == 0) { // 红桃主
					tmpCardData += 32;
				}
			}
			return cbSortValue + tmpCardData;
		}
		// 主花色
		if (cbCardColor == m_cbMainColor) {
			int value = 70 * 4 + tmpCardData;
			if (cbCardValue == 1) {
				value += 13;
			}
			return value;
		}

		tmpCardData += sort[cbCardColor];
		if (cbCardValue == 1) {
			return tmpCardData + 13;
		}

		return tmpCardData;
	}

	/**
	 * 获取扑克值
	 * 
	 * @param cardData
	 * @return
	 */
	public int getCardLogicValue(int cardData) {
		int bCardValue = getCardValue(cardData);
		return (bCardValue == 1) ? (bCardValue + 13) : bCardValue;
	}

	public int getRealCard(int cardData) {
		return cardData & 0x0FF;
	}

	// 获取数值
	public int getCardValue(int card) {
		return card & SDHConstants_XT.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int getCardColor(int card) {
		return (card & SDHConstants_XT.LOGIC_MASK_COLOR) >> 4;
	}

	/**
	 * 删除扑克
	 * 
	 * @param cards
	 *            卡牌
	 * @param cardCount
	 *            数量
	 * @param removeCards
	 *            待删除卡牌
	 * @param removeCount
	 *            数量
	 * @param nStartPos
	 *            开始位置
	 * @return
	 */
	public boolean removeCardsByData(int cards[], int cardCount, int removeCards[], int removeCount, int nStartPos) {
		// 检验数据
		if (cardCount < removeCount) {
			return false;
		}

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = SDHUtil.copyArray(cards, nStartPos, cardCount);
		// 置零扑克
		for (int i = nStartPos; i < removeCount; i++) {
			for (int j = nStartPos; j < cardCount; j++) {
				if (removeCards[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}
		// 判断
		if (cbDeleteCount != removeCount) {
			return false;
		}
		// 清理扑克
		int cbCardPos = 0;
		for (int i = nStartPos; i < cardCount; i++) {
			if (cbTempCardData[i] != 0) {
				cards[cbCardPos++] = cbTempCardData[i];
			}
		}

		return true;
	}

	/**
	 * 计算一轮出牌的得分
	 * 
	 * @param outCardsDatas
	 * @param outCardCount
	 * @return
	 */
	public int calculationScore(SDHTable table, int[][] outCardsDatas, int outCardCount) {
		int score = 0;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			score += calculationScore(table, outCardsDatas[i], outCardCount);
		}

		return score;
	}

	public int calculationScore(SDHTable table, int[] outCardsDatas, int outCardCount) {
		int score = 0;

		for (int j = 0; j < outCardCount; j++) {
			int value = getCardValue(outCardsDatas[j]); // 5 10 K 加分
			if (0 == value % 5 || 13 == value) {
				score += value == 5 ? 5 : 10;
			}
		}

		return score;
	}

	public void switch_cards_to_index(int seatIndex, int[] cards, int cardsCount, int[][] cardsIndex) {
		for (int i = 0; i < SDHConstants_XT.SDH_COLOR_COUNT + 1; i++) {
			Arrays.fill(cardsIndex[i], 0); // 每一次之后都要清空数据 防止重复统计
		}
		Arrays.fill(maxCard[seatIndex], 0);
		for (int i = 0; i < cardsCount; i++) {
			int color = getCardColor(cards[i]);
			int value = getCardValue(cards[i]);

			if (color == 4) { // 大小王
				value += 14;
			} else if (1 == value) {
				value += 13;
			}
			cardsIndex[color][value]++;
			if (color == m_cbMainColor || value == m_cbMainValue || value == m_cbNTValue) {
				if (color != 4) {
					cardsIndex[SDHConstants_XT.SDH_COLOR_MAIN][value]++;
				}
			}
		}
		// 记录每个花色的最大牌
		int[] copyCards = Arrays.copyOf(cards, cardsCount);
		// int colorIndex = SDHConstants_XT.SDH_COLOR_MAIN;

		sortCardList(copyCards, cardsCount);

		for (int i = 0; i < cardsCount; i++) {
			int logicColor = getCardLogicColor(copyCards[i]);

			if (maxCard[seatIndex][logicColor] != 0) {
				continue;
			}
			maxCard[seatIndex][logicColor] = copyCards[i];
		}
	}

	/**
	 * 判断一张牌是否正主
	 * 
	 * @param card
	 * @return true: 是
	 */
	public boolean isTheMain(int card) {
		int value = getCardLogicValue(card);
		int color = getCardColor(card);

		if (color == SDHConstants_XT.SDH_COLOR_MAIN || value == m_cbMainValue || value == m_cbNTValue) {
			return true;
		}
		return false;
	}

	public boolean hasTheMain(int card) {
		int value = getCardLogicValue(card);
		int color = getCardColor(card);

		if (color == SDHConstants_XT.SDH_COLOR_MAIN || value == m_cbMainValue || value == m_cbNTValue
				|| color == m_cbMainColor) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		SDHGameLogic logic = new SDHGameLogic(2, 1, 10);

		// int[] cbCardData = { 0x1d, 0x1d, 0x11, 0x11, 0x02, 0x02, 0x12, 0x12,
		// 0x07, 0x07, 0x17, 0x17, 0x41, 0x41, 0x42, 0x42 };
		// int[] cardDatas = { 0x12, 0x15, 0x18, 0x19, 0x2a, 0x2b, 0x1c, 0x1d,
		// 0x11, };
		// int sameCount[] = logic.getTractorNumber(cardDatas,
		// cardDatas.length);
		// for (int i = 0; i < 13; i++) {
		// System.out.println(i + "连拖拉机" + sameCount[i]);
		// }
		int cards[] = { 0x2A, 0x2A, 0x28, 0x08 };
		System.out.println(logic.judgeTractor_yybs(cards, cards.length));
		for (int i = 0; i < cards.length; i++) {
			System.out.println(logic.getCardLogicValue(cards[i]));
		}
	}

}
