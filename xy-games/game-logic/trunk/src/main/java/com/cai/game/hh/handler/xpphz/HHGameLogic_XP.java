/**
 * 
 */
package com.cai.game.hh.handler.xpphz;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;
import com.cai.game.hh.HHGameLogic;

public class HHGameLogic_XP extends HHGameLogic {

	public HHGameLogic_XP() {
	}

	public void clean_magic_cards() {
	}

	// 扑克转换
	public int switch_to_cards_index(int cards_data[], int start_index, int card_count, int cards_index[]) {
		// 转换扑克
		for (int i = 0; i < card_count; i++) {
			cards_index[switch_to_card_index(cards_data[start_index + i])]++;
		}
		return card_count;
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		if (is_valid_card(card) == false) {
			return Constants_XPPHZ.MAX_HH_INDEX;
		}
		int color = get_card_color_orig(card);
		int value = get_card_value(card);
		int index = color * 10 + value - 1;
		return index;
	}

	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int i = 0; i < Constants_XPPHZ.MAX_HH_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	// 删除扑克
	public boolean remove_cards_by_index(int cards_index[], int cards[], int card_count) {
		// 删除扑克
		for (int i = 0; i < card_count; i++) {
			// 先删除小的 再删除大的 如果都没有 就还原
			if (remove_card_by_index(cards_index, toLowCard(cards[i])) == false) {
				if (remove_card_by_index(cards_index, toUpCard(cards[i])) == false) {
					// 还原删除
					for (int j = 0; j < i; j++) {
						cards_index[j]++;
					}
					return false;
				}
			}
		}

		return true;
	}

	public int get_card_color_orig(int card) {
		return ((card & GameConstants.LOGIC_MASK_COLOR) >> 4);
	}

	// 获取花色
	public int get_card_color(int card) {
		return ((card & GameConstants.LOGIC_MASK_COLOR) >> 4) % 2;
	}

	public int check_chi(int cards_index[], int cur_card, int type_count[], int type_eat_count[]) {
		int eat_type = 0;
		int count = 0;
		int cur_card_index = switch_to_card_index(cur_card) % 20; // 当前牌索引

		int tempIndex[] = new int[20];
		for (int i = 0; i < 20; i++) {
			tempIndex[i] = cards_index[i];
			if (cards_index.length > 20) {
				tempIndex[i] += cards_index[i + 20];
			}
		}
		// 三牌判断
		if (tempIndex[cur_card_index] >= 3) {
			return eat_type;
		}
		// 大小搭吃
		int reverse_index = (cur_card_index + 10) % GameConstants.MAX_HH_INDEX;
		if ((tempIndex[cur_card_index] >= 1) && (tempIndex[reverse_index] >= 1) && (tempIndex[reverse_index] < 3)) {
			int action = (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
			eat_type |= action;
			type_count[count++] = action;
		}
		// 大小搭吃
		if (tempIndex[reverse_index] == 2) {
			int action = (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
			eat_type |= action;
			type_count[count++] = action;
		}
		// 二七十吃
		int card_value = get_card_value(cur_card);
		if ((card_value == 2) || (card_value == 7) || (card_value == 10)) {
			int excursion[] = { 1, 6, 9 };
			int acceptIndex = (get_card_color(cur_card) == 0) ? 0 : 10;
			int i = 0;
			for (; i < excursion.length; i++) {
				int index = acceptIndex + excursion[i];
				if ((index != cur_card_index) && ((tempIndex[index] == 0) || (tempIndex[index] >= 3))) {
					break;
				}
			}
			if (i == excursion.length) {
				eat_type |= GameConstants.WIK_EQS;
				type_count[count++] = GameConstants.WIK_EQS;
			}
		}
		// 顺子吃

		int excursion[] = { 0, 1, 2 };
		for (int i = 0; i < excursion.length; i++) {
			int value_index = cur_card_index % 10;
			if ((value_index >= excursion[i]) && (value_index - excursion[i] <= 7)) {
				int first_index = cur_card_index - excursion[i];
				if ((cur_card_index != first_index) && ((tempIndex[first_index] == 0) || (tempIndex[first_index] == 3) || (tempIndex[first_index] == 4))) {
					continue;
				}
				if ((cur_card_index != first_index + 1)
						&& ((tempIndex[first_index + 1] == 0) || (tempIndex[first_index + 1] == 3) || (tempIndex[first_index + 1] == 4))) {
					continue;
				}
				if ((cur_card_index != first_index + 2)
						&& ((tempIndex[first_index + 2] == 0) || (tempIndex[first_index + 2] == 3) || (tempIndex[first_index + 2] == 4))) {
					continue;
				}

				int chi_kind[] = { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
				eat_type |= chi_kind[i];
				type_count[count++] = chi_kind[i];
			}

		}

		type_eat_count[0] = count;
		return eat_type;

	}

	public int switch_to_card_data(int card_index) {
		if (card_index >= Constants_XPPHZ.MAX_HH_INDEX) {
			return Constants_XPPHZ.MAX_HH_INDEX;
		}
		return ((card_index / 10) << 4) | (card_index % 10 + 1);
	}

	// 牌数数目
	public int get_card_count_by_index(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			card_count += cards_index[i];
		}

		return card_count;
	}

	public int get_weave_hu_xi(WeaveItem weave_item) {
		switch (weave_item.weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_AN_LONG_LIANG:
			return (get_card_color(weave_item.center_card) != 0) ? 12 : 9;
		case GameConstants.WIK_PAO:
			return (get_card_color(weave_item.center_card) != 0) ? 9 : 6;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			return (get_card_color(weave_item.center_card) != 0) ? 6 : 3;
		case GameConstants.WIK_PENG:
			return (get_card_color(weave_item.center_card) != 0) ? 3 : 1;
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YWS:
			return (get_card_color(weave_item.center_card) != 0) ? 6 : 3;
		case GameConstants.WIK_LEFT: {
			int card_value = get_card_value(weave_item.center_card);
			if (card_value == 1) {
				return (get_card_color(weave_item.center_card) != 0) ? 6 : 3;
			}
			break;
		}
		case GameConstants.WIK_CENTER: {
			int card_value = get_card_value(weave_item.center_card);
			if (card_value == 2) {
				return (get_card_color(weave_item.center_card) != 0) ? 6 : 3;
			}
			break;
		}
		case GameConstants.WIK_RIGHT: {
			int card_value = get_card_value(weave_item.center_card);
			if (card_value == 3) {
				return (get_card_color(weave_item.center_card) != 0) ? 6 : 3;
			}
			break;
		}

		}
		return 0;
	}

	public int estimate_pao_card_out_card(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return ((card_index[switch_to_card_index(cur_card) % 20] + card_index[switch_to_card_index(cur_card) % 20 + 20]) == 3) ? GameConstants.WIK_PAO
				: GameConstants.WIK_NULL;
	}

	public int check_sao(int card_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		int index = switch_to_card_index(toLowCard(cur_card));
		// 扫牌判断
		return ((card_index[index] + card_index[index + 20]) == 2) ? GameConstants.WIK_SAO : GameConstants.WIK_NULL;
	}

	public int check_peng(int card_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		int index = switch_to_card_index(toLowCard(cur_card));
		// 碰牌判断
		return ((card_index[index] + card_index[index + 20]) == 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	// 跑牌判断
	public int check_pao(int card_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		int index = switch_to_card_index(toLowCard(cur_card));
		// 碰牌判断
		return ((card_index[index] + card_index[index + 20]) == 3) ? GameConstants.WIK_PAO : GameConstants.WIK_NULL;
	}

	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color_orig(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 3);
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
		if (card_index < 0 || card_index >= Constants_XPPHZ.MAX_HH_INDEX) {
			return false;
		}

		if (cards_index[card_index] == 0) {
			return false;
		}
		// 删除扑克
		cards_index[card_index]--;
		return true;
	}

	public static void main(String[] args) {
		HHGameLogic_XP logic_XP = new HHGameLogic_XP();
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// int cards_data[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x21, 0x22, 0x23,
		// 0x24, 0x25, 0x01, 0x02, 0x03, 0x04, 0x25, 0x21, 0x12, 0x13, 0x14,
		// 0x15 };
		int cards_data[] = { 0x21, 0x22, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x11, 0x11, 0x13, 0x14, 0x11 };
		int[] cards_index = new int[40];
		logic_XP.switch_to_cards_index(cards_data, 0, 20, cards_index);
		cards_index[logic_XP.switch_to_card_index(0x15)]++;

		int calcIndex[] = new int[20];
		for (int i = 0; i < 20; i++) {
			calcIndex[i] += cards_index[i] + cards_index[i + 20];
		}
		int[] hu_xi = new int[1];
		hu_xi[0] = 0;
		System.out.println(logic_XP.analyse_card(calcIndex, null, 0, 0, 0, 0x15, analyseItemArray, false, hu_xi));
	}

	/**
	 * 比较两张卡是否相等
	 * 
	 * @param weave_card
	 * @param card_data
	 * @return
	 */
	public boolean compareCard(int first, int next) {
		int firstColor = get_card_color_orig(first);
		int nextColor = get_card_color_orig(next);

		if (firstColor % 2 == nextColor % 2) { // 颜色一致
			return get_card_value(first) == get_card_value(next);
		}
		return false;
	}

	public int toLowCard(int card) {
		return card % 32;
	}

	public int toUpCard(int card) {
		return (card % 32) + 32;
	}

}
