package com.cai.game.hongershi;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;

public class HongErShiGameLogic {

	public int switch_to_card_data(int card_index) {
		if (card_index == 52) {
			return HongErShiConstants.MAGIC_CARD_KING;
		}
		return ((card_index / 13) << 4) | (card_index % 13 + 1);
	}

	public int switch_to_card_index(int card) {
		if (card == HongErShiConstants.MAGIC_CARD_KING) {
			return 52;
		}

		if (is_valid_card(card) == false) {
			return GameConstants.MAX_HH_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = color * 13 + value - 1;
		return index;
	}

	public int switch_to_card_index_red_black(int card) {
		if (card == HongErShiConstants.MAGIC_CARD_KING) {
			return 52;
		}

		int color = get_card_color(card) % 2;
		int value = get_card_value(card);
		int index = color * 13 + value - 1;
		return index;
	}

	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 13) && (cbColor <= 3);
	}

	// 获取数值
	public int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public boolean color_hei(int card) {
		return ((card & GameConstants.LOGIC_MASK_COLOR) >> 4) % 2 == 0;
	}

	// 洗牌
	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);
	}

	// 混乱准备
	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		// 混乱扑克
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount);
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);
	}

	// 扑克转换
	public int switch_to_cards_index_value(int cards_data[], int start_index, int card_count, int cards_index[]) {
		// 设置变量 转换扑克
		for (int i = 0; i < card_count; i++) {
			if (cards_data[i] == 0) {
				continue;
			}
			if (cards_data[start_index + i] == HongErShiConstants.MAGIC_CARD_KING) {
				cards_index[13]++;
			} else {
				cards_index[get_card_value(cards_data[start_index + i]) - 1]++;
			}
		}
		return card_count;
	}

	// 扑克转换
	public int switch_to_cards_index_real(int cards_data[], int start_index, int card_count, int cards_index[]) {
		// 设置变量
		// 转换扑克
		for (int i = 0; i < card_count; i++) {
			cards_index[switch_to_card_index(cards_data[start_index + i])]++;
		}
		return card_count;
	}

	public int check_chi(int[] cards, int card_count, int card) {
		for (int i = 0; i < card_count; i++) {
			int sum = get_card_value(cards[i]) + get_card_value(card);
			if (sum == 14) {
				return HongErShiConstants.WIK_CHI;
			}
		}
		return GameConstants.WIK_NULL;
	}

	public int check_peng(int[] cards, int card_count, int card) {
		int[] cards_value_index = new int[14];
		switch_to_cards_index_value(cards, 0, card_count, cards_value_index);

		if (card == HongErShiConstants.MAGIC_CARD_KING) {
			return GameConstants.WIK_NULL;
		}
		if (cards_value_index[get_card_value(card) - 1] > 1) {
			return GameConstants.WIK_PENG;
		}

		return GameConstants.WIK_NULL;
	}

	public int check_gang(int[] cards, int card_count, int card) {
		int[] cards_value_index = new int[14];
		switch_to_cards_index_value(cards, 0, card_count, cards_value_index);
		if (card == HongErShiConstants.MAGIC_CARD_KING) {
			return GameConstants.WIK_NULL;
		}

		if (cards_value_index[get_card_value(card)] == 3) {
			return GameConstants.WIK_GANG;
		}

		return GameConstants.WIK_NULL;
	}

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int i = 0; i < HongErShiConstants.MAX_CARD_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data_by_card(int cards_card[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int i = 0; i < cards_card.length; i++) {
			if (cards_card[i] != 0) {
				cards_data[cbPosition++] = cards_card[i];
			}
		}
		return cbPosition;
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == HongErShiConstants.WIK_ZI_MO) {
			return 50;
		}
		// 吃胡牌等级
		if (player_action == HongErShiConstants.WIK_CHI_HU) {
			return 40;
		}

		// 地胡牌等级
		if (player_action == HongErShiConstants.WIK_CHI_HU) {
			return 40;
		}

		// 杠牌等级
		if (player_action == HongErShiConstants.WIK_GANG) {
			return 30;
		}

		// 碰牌等级
		if (player_action == HongErShiConstants.WIK_PENG || player_action == HongErShiConstants.WIK_AN_PENG) {
			return 20;
		}

		// 上牌等级
		if (player_action == HongErShiConstants.WIK_CHI) {
			return 10;
		}

		return 0;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int MAX_HH_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (MAX_HH_INDEX < index) {
				MAX_HH_INDEX = index;
			}
		}

		return MAX_HH_INDEX;
	}

	// 牌数数目
	public int get_card_count_by_index(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}

	/***
	 * 删除扑克 索引
	 * 
	 * @param cards_index
	 * @param card
	 * @return
	 */
	public boolean remove_card_by_index(int cards_index[], int card) {
		// 效验扑克
		int card_index = switch_to_card_index(card);
		if (card_index < 0 || card_index > HongErShiConstants.MAX_CARD_INDEX) {
			return false;
		}

		if (cards_index[card_index] == 0) {
			return false;
		}

		// 删除扑克
		cards_index[card_index]--;
		return true;
	}

	public int remove_card_by_card_value(int[] cards, int cards_count, int remove_card, int[] data) {

		int remove_count = 0;
		for (int i = 0; i < cards_count; i++) {
			if (get_card_value(cards[i]) == get_card_value(remove_card)) {
				data[remove_count++] = cards[i];
				cards[i] = 0;
			}
			if (data.length == remove_count) {
				break;
			}
		}

		int count = 0;
		for (int i = 0; i < cards_count; i++) {
			if (cards[i] != 0) {
				cards[count++] = cards[i];
			}
		}
		return count;
	}

	// 删除扑克
	public boolean remove_cards_by_index(int cards_index[], int cards[], int card_count) {
		// 删除扑克
		for (int i = 0; i < card_count; i++) {
			if (remove_card_by_index(cards_index, cards[i]) == false) {
				// 还原删除
				for (int j = 0; j < i; j++) {
					cards_index[j]++;
				}
				return false;
			}
		}

		return true;
	}

	public int remove_cards_by_cards(int[] cards_data, int card_count, int[] cards, int re_card_count) {
		int[] temp_cards_data = Arrays.copyOf(cards_data, card_count);

		for (int i = 0; i < re_card_count; i++) {
			if (!remove_card_by_card(temp_cards_data, cards[i], card_count)) {
				return -1;
			}
		}

		int count = 0;
		for (int i = 0; i < card_count; i++) {
			if (temp_cards_data[i] != 0) {
				cards_data[count++] = temp_cards_data[i];
			}
		}
		return count;
	}

	public boolean remove_card_by_card(int[] cards_data, int card, int card_count) {
		for (int i = 0; i < card_count; i++) {
			if (cards_data[i] == card) {
				cards_data[i] = 0;
				return true;
			}
		}
		return false;
	}

	public boolean judgeKing(int card) {
		return card == 0x4F;
	}

	public int countKingNumber(int cardsData[], int cardsCount) {
		int result = 0;

		for (int i = 0; i < cardsCount; i++) {
			if (judgeKing(cardsData[i])) {
				result++;
			}
		}

		return result;
	}

	public int countSevenNumber(int cardsData[], int cardsCount) {
		int result = 0;

		for (int i = 0; i < cardsCount; i++) {
			if (get_card_value(cardsData[i]) == HongErShiConstants.MAGIC_SEVEN_VALUE) {
				result++;
			}
		}

		return result;
	}

	/**
	 * 检查手牌中数量大于2的卡牌
	 * 
	 * @param is
	 * @param i
	 * @return
	 */
	public void checkLgThree(int[] cardsData, int cardsCount, int[] cardsFour, int[] cardsThree, int[] count) {
		int cardsIndex[] = new int[14];
		switch_to_cards_index_value(cardsData, 0, cardsCount, cardsIndex);

		for (int i = 0; i < 13; i++) {
			if (cardsIndex[i] > 3) {
				cardsFour[count[0]++] = switch_to_card_data(i);
			} else if (cardsIndex[i] > 2) {
				cardsThree[count[1]++] = switch_to_card_data(i);
			}
		}
	}

	public boolean remove_all_card_by_card(int[] cardsData, int cardCount, int count, int card) {
		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = Arrays.copyOf(cardsData, cardCount);
		// 置零扑克
		for (int i = 0; i < cardCount; i++) {
			if (card == cbTempCardData[i]) {
				cbDeleteCount++;
				cbTempCardData[i] = 0;
				break;
			}
		}
		if (cbDeleteCount != count) {
			return false;
		}
		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < cardCount; i++) {
			if (cbTempCardData[i] != 0) {
				cardsData[cbCardPos++] = cbTempCardData[i];
			}
		}

		return true;
	}

	public void switch_to_card_data(int[] cardsData, int cardsCount, int[] cardsIndex) {
		for (int i = 0; i < cardsCount; i++) {
			cardsIndex[switch_to_card_index_red_black(cardsData[i])]++;
		}
	}

	public void countCardsCount(WeaveItem[] weaveItems, int count, int[] cardsIndex) {
		for (int i = 0; i < count; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 2; j++) {
					cardsIndex[switch_to_card_index_red_black(weaveItems[i].weave_card[j])]++;
				}
				break;
			case GameConstants.WIK_PENG:
				for (int j = 0; j < 3; j++) {
					cardsIndex[switch_to_card_index_red_black(weaveItems[i].weave_card[j])]++;
				}
				break;
			case GameConstants.WIK_AN_GANG:
			case GameConstants.WIK_GANG:
				for (int j = 0; j < 4; j++) {
					cardsIndex[switch_to_card_index_red_black(weaveItems[i].weave_card[j])]++;
				}
			}
		}
	}

}
