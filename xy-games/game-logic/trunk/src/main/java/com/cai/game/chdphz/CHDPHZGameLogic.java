/**
 * 
 */
package com.cai.game.chdphz;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_YZCHZ;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.hh.handler.yyzhz.GameConstants_YYZHZ;

//类型子项
class KindItem {
	public int cbWeaveKind;// 组合类型
	public int cbCenterCard;// 中心扑克
	public int cbCardIndex[] = new int[3];// 扑克索引
	public int cbValidIndex[] = new int[3];// 实际扑克索引

	public KindItem() {

	}
}

class LouWeaveItem {
	public int nWeaveKind; // 组合类型
	public int nLouWeaveKind[][] = new int[50][2];
	public int nCount;

	public LouWeaveItem() {

	}
}

// 吃牌信息
class ChiCardInfo {
	int cbChiKind; // 吃牌类型
	int cbCenterCard; // 中心扑克
	int cbResultCount; // 结果数目
	int cbCardData[][] = new int[3][3]; // 吃牌组合

	public ChiCardInfo() {

	}
};

// 分析结构
class tagAnalyseResult {

	int cbMulCount; // 多张数目总和
	int cbFourCount; // 四张数目
	int cbThreeCount; // 三张数目
	int cbDoubleCount; // 两张数目
	int cbSingleCount; // 单张数目
	int cbMulCardData[] = new int[GameConstants.MAX_HH_COUNT];
	int cbFourCardData[] = new int[GameConstants.MAX_HH_COUNT]; // 四张扑克
	int cbThreeCardData[] = new int[GameConstants.MAX_HH_COUNT]; // 三张扑克
	int cbDoubleCardData[] = new int[GameConstants.MAX_HH_COUNT]; // 两张扑克
	int cbSingleCardData[] = new int[GameConstants.MAX_HH_COUNT]; // 单张扑克

	public tagAnalyseResult() {
		cbFourCount = 0;
		cbThreeCount = 0;
		cbDoubleCount = 0;
		cbMulCount = 0;
		Arrays.fill(cbMulCardData, 0);
		Arrays.fill(cbFourCardData, 0);
		Arrays.fill(cbThreeCardData, 0);
		Arrays.fill(cbDoubleCardData, 0);

	}

	public void Reset() {

		cbFourCount = 0;
		cbThreeCount = 0;
		cbDoubleCount = 0;
		Arrays.fill(cbMulCardData, 0);
		Arrays.fill(cbFourCardData, 0);
		Arrays.fill(cbThreeCardData, 0);
		Arrays.fill(cbDoubleCardData, 0);
	}
};

//// 胡牌信息
// class HuCardInfo{
// public int card_eye;
// public int hu_xi_count;
// public int weave_count;
// public WeaveItem weave_item[] = new WeaveItem[10];
// }

public class CHDPHZGameLogic {

	private static Logger logger = Logger.getLogger(CHDPHZGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	public CHDPHZGameLogic() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_HH_COUNT];
		_lai_gen = 0;
		_ding_gui = 0;
	}

	public void clean_magic_cards() {
		_magic_card_count = 0;
	}

	// 获取数值
	public int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public void add_magic_card_index(int index) {
		_magic_card_index[_magic_card_count] = index;
		_magic_card_count++;
	}

	public void add_lai_gen_card(int card) {
		_lai_gen = card;
	}

	public void add_ding_gui_card(int card) {
		_ding_gui = card;
	}

	public boolean is_magic_card(int card) {
		for (int i = 0; i < _magic_card_count; i++) {
			if (_magic_card_index[i] == switch_to_card_index(card)) {
				return true;
			}
		}
		return false;
	}

	public boolean is_magic_index(int index) {
		for (int i = 0; i < _magic_card_count; i++) {
			if (_magic_card_index[i] == index) {
				return true;
			}
		}
		return false;
	}

	public boolean is_lai_gen_card(int card) {
		if (_lai_gen == card) {
			return true;
		}
		return false;
	}

	public boolean is_ding_gui_card(int card) {
		if (_ding_gui == card) {
			return true;
		}
		return false;
	}

	public int magic_count(int cards_index[]) {
		int count = 0;
		for (int i = 0; i < _magic_card_count; i++) {
			count += cards_index[_magic_card_index[i]];
		}
		return count;
	}

	public int get_magic_card_index(int index) {
		// m_cbMagicIndex
		return _magic_card_index[index];// MJGameConstants.MAX_HH_INDEX;
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
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);

	}

	// 删除扑克 by data
	public boolean remove_card_by_data(int cards[], int card_data) {
		int card_count = cards.length;

		if (card_count == 0) {
			return false;
		}

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.MAX_HH_COUNT];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < card_count; i++) {
			if (card_data == cbTempCardData[i]) {
				cbDeleteCount++;
				cbTempCardData[i] = 0;
				break;
			}
		}

		// 成功判断
		if (cbDeleteCount != 1) {
			return false;
		}

		// 清理扑克
		for (int i = 0; i < card_count; i++) {
			cards[i] = 0;
		}
		int cbCardPos = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;

	}

	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.MAX_HH_COUNT];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < remove_count; i++) {
			for (int j = 0; j < card_count; j++) {
				if (remove_cards[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}

		// 成功判断
		if (cbDeleteCount != remove_count) {
			return false;
		}

		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

	// 牌数数目
	public int get_card_count_by_index(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}

	// 牌数数目岳阳捉红字
	public int get_card_count_by_index_yyzhz(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}

	public int get_card_count_by_index_yzchz(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++)
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
		if (card_index < 0 || card_index >= GameConstants.MAX_HH_INDEX) {
			return false;
		}

		if (cards_index[card_index] == 0) {
			return false;
		}

		// 删除扑克
		cards_index[card_index]--;
		return true;
	}

	/***
	 * 删除扑克 索引(岳阳捉红字)
	 * 
	 * @param cards_index
	 * @param card
	 * @return
	 */
	public boolean remove_card_by_index_yyzhz(int cards_index[], int card) {
		// 效验扑克
		int card_index = switch_to_card_index(card);
		if (card_index < 0 || card_index >= GameConstants_YYZHZ.MAX_YYZHZ_INDEX) {
			return false;
		}

		if (cards_index[card_index] == 0) {
			return false;
		}

		// 删除扑克
		cards_index[card_index]--;
		return true;
	}

	// 删除扑克
	public boolean remove_cards_by_index(int cards_index[], int cards[], int card_count) {
		// 删除扑克
		for (int i = 0; i < card_count; i++) {
			if (remove_card_by_index(cards_index, cards[i]) == false) {
				// 还原删除
				for (int j = 0; j < i; j++) {
					cards_index[this.switch_to_card_index(cards[j])]++;
				}
				return false;
			}
		}

		return true;
	}

	// 吃牌判断
	public int get_action_chi_card(int cards_index[], int cur_card, ChiCardInfo chi_card_info[]) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int chi_card_count = 0;
		int cur_index = switch_to_card_index(cur_card);

		// 三牌判断
		if (cards_index[cur_index] >= 3)
			return chi_card_count;

		// 大小搭吃
		int reverse_index = (cur_index + 10) % GameConstants.MAX_HH_INDEX;
		if ((cards_index[cur_index] >= 1) && (cards_index[reverse_index] >= 1) && (cards_index[reverse_index] < 3)) {
			// 构造扑克
			int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				temp_card_index[i] = cards_index[i];
			}

			// 删除扑克
			temp_card_index[cur_index]--;
			temp_card_index[reverse_index]--;

			// 提取判断
			int cbResultCount = 1;
			while (temp_card_index[cur_index] > 0) {
				int result[] = chi_card_info[chi_card_count].cbCardData[cbResultCount];
				if (take_out_chi_card(temp_card_index, cur_card, result) != GameConstants.WIK_NULL)
					cbResultCount++;
				else
					break;
			}

			// 设置结果
			if (temp_card_index[cur_index] == 0) {
				chi_card_info[chi_card_count].cbCenterCard = cur_card;
				chi_card_info[chi_card_count].cbResultCount = cbResultCount;
				chi_card_info[chi_card_count].cbCardData[0][0] = cur_card;
				chi_card_info[chi_card_count].cbCardData[0][1] = cur_card;
				chi_card_info[chi_card_count].cbCardData[0][2] = switch_to_card_data(reverse_index);
				chi_card_info[chi_card_count++].cbChiKind = (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
			}
		}

		// 大小搭吃
		if (cards_index[reverse_index] == 2) {
			// 构造扑克
			int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				temp_card_index[i] = cards_index[i];
			}

			// 删除扑克
			temp_card_index[reverse_index] -= 2;

			// 提取判断
			int cbResultCount = 1;
			while (temp_card_index[cur_index] > 0) {
				int result[] = chi_card_info[chi_card_count].cbCardData[cbResultCount];
				if (take_out_chi_card(temp_card_index, cur_card, result) != GameConstants.WIK_NULL)
					cbResultCount++;
				else
					break;
			}

			// 设置结果
			if (temp_card_index[cur_index] == 0) {
				chi_card_info[chi_card_count].cbCenterCard = cur_card;
				chi_card_info[chi_card_count].cbResultCount = cbResultCount;
				chi_card_info[chi_card_count].cbCardData[0][0] = cur_card;
				chi_card_info[chi_card_count].cbCardData[0][1] = switch_to_card_data(reverse_index);
				chi_card_info[chi_card_count].cbCardData[0][2] = switch_to_card_data(reverse_index);
				chi_card_info[chi_card_count++].cbChiKind = (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
			}
		}

		// 二七十吃
		int card_value = get_card_value(cur_card);
		if ((card_value == 0x02) || (card_value == 0x07) || (card_value == 0x0A)) {
			// 变量定义
			int excursion[] = { 1, 6, 9 };
			int incept_index = (get_card_color(cur_card) == 0x00) ? 0 : 10;

			// 类型判断
			int i = 0;
			for (; i < excursion.length; i++) {
				int temp_index = incept_index + excursion[i];
				if ((temp_index != cur_index) && ((cards_index[cur_index] == 0) || (cards_index[cur_index] == 3) || (cards_index[cur_index] == 4)))
					break;
			}

			// 提取判断
			if (i == excursion.length) {
				// 构造扑克
				int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
				for (int j = 0; i < GameConstants.MAX_HH_INDEX; j++) {
					temp_card_index[j] = cards_index[j];
				}

				// 删除扑克
				for (int j = 0; j < excursion.length; j++) {
					int index = incept_index + excursion[j];
					if (index != cur_index)
						temp_card_index[index]--;
				}

				// 提取判断
				int cbResultCount = 1;
				while (temp_card_index[cur_index] > 0) {
					int result[] = chi_card_info[chi_card_count].cbCardData[cbResultCount];
					if (take_out_chi_card(temp_card_index, cur_card, result) != GameConstants.WIK_EQS)
						cbResultCount++;
					else
						break;
				}

				// 设置结果
				if (temp_card_index[cur_index] == 0) {
					chi_card_info[chi_card_count].cbChiKind = GameConstants.WIK_EQS;
					chi_card_info[chi_card_count].cbCenterCard = cur_card;
					chi_card_info[chi_card_count].cbResultCount = cbResultCount;
					chi_card_info[chi_card_count].cbCardData[0][0] = switch_to_card_data(incept_index + excursion[0]);
					chi_card_info[chi_card_count].cbCardData[0][1] = switch_to_card_data(incept_index + excursion[1]);
					chi_card_info[chi_card_count++].cbCardData[0][2] = switch_to_card_data(incept_index + excursion[2]);
				}
			}
		}

		// 顺子类型
		int excursion[] = { 0, 1, 2 };
		for (int i = 0; i < excursion.length; i++) {
			int value_index = cur_index % 10;
			if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= 7)) {
				// 索引定义
				int first_index = cur_index - excursion[i];

				// 吃牌判断
				int j = 0;
				for (; j < 3; j++) {
					int cbIndex = first_index + j;
					if ((cbIndex != cur_index) && ((cards_index[cbIndex] == 0) || (cards_index[cbIndex] == 3) || (cards_index[cbIndex] == 4)))
						break;
				}

				// 提取判断
				if (j == excursion.length) {
					// 构造扑克
					int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
					for (int k = 0; i < GameConstants.MAX_HH_INDEX; k++) {
						temp_card_index[k] = cards_index[k];
					}

					// 删除扑克
					for (int k = 0; k < 3; k++) {
						int temp_index = first_index + k;
						if (temp_index != cur_index)
							temp_card_index[temp_index]--;
					}

					// 提取判断
					int cbResultCount = 1;
					while (temp_card_index[cur_index] > 0) {
						int result[] = chi_card_info[chi_card_count].cbCardData[cbResultCount];
						if (take_out_chi_card(temp_card_index, cur_card, result) != GameConstants.WIK_NULL)
							cbResultCount++;
						else
							break;
					}

					// 设置结果
					if (temp_card_index[cur_index] == 0) {
						int cbChiKind[] = { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
						chi_card_info[chi_card_count].cbChiKind = cbChiKind[i];
						chi_card_info[chi_card_count].cbCenterCard = cur_card;
						chi_card_info[chi_card_count].cbResultCount = cbResultCount;
						chi_card_info[chi_card_count].cbCardData[0][0] = switch_to_card_data(first_index);
						chi_card_info[chi_card_count].cbCardData[0][1] = switch_to_card_data(first_index + 1);
						chi_card_info[chi_card_count++].cbCardData[0][2] = switch_to_card_data(first_index + 2);
					}
				}
			}
		}

		return chi_card_count;
	}

	// 吃牌判断
	public int take_out_chi_card(int cards_index[], int cur_card, int result_card[]) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int first_index = 0;
		int cur_index = switch_to_card_data(cur_card);

		// 大小搭吃
		int reverse_index = (cur_index + 10) % GameConstants.MAX_HH_INDEX;
		if ((cards_index[cur_index] >= 2) && (cards_index[reverse_index] >= 1) && (cards_index[reverse_index] < 3)) {
			// 删除扑克
			cards_index[cur_index]--;
			cards_index[cur_index]--;
			cards_index[reverse_index]--;

			// 设置结果
			result_card[0] = cur_card;
			result_card[1] = cur_card;
			result_card[2] = switch_to_card_data(reverse_index);

			return (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
		}

		// 大小搭吃
		if (cards_index[reverse_index] == 2) {
			// 删除扑克
			cards_index[cur_index]--;
			cards_index[reverse_index] -= 2;

			// 设置结果
			result_card[0] = cur_card;
			result_card[1] = switch_to_card_data(reverse_index);
			result_card[2] = switch_to_card_data(reverse_index);

			return (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
		}

		// 二七十吃
		int card_value = get_card_value(cur_card);
		if ((card_value == 0x02) || (card_value == 0x07) || (card_value == 0x0A)) {
			// 变量定义
			int excursion[] = { 1, 6, 9 };
			int incept_index = (get_card_color(cur_card) == 0x00) ? 0 : 10;

			// 类型判断
			int i = 0;
			for (; i < excursion.length; i++) {
				int temp_index = incept_index + excursion[i];
				if ((cards_index[temp_index] == 0) || (cards_index[temp_index] == 3) || (cards_index[temp_index] == 4))
					break;
			}

			// 成功判断
			if (i == excursion.length) {
				// 删除扑克
				cards_index[incept_index + excursion[0]]--;
				cards_index[incept_index + excursion[1]]--;
				cards_index[incept_index + excursion[2]]--;

				// 设置结果
				result_card[0] = switch_to_card_data(incept_index + excursion[0]);
				result_card[1] = switch_to_card_data(incept_index + excursion[0]);
				result_card[2] = switch_to_card_data(incept_index + excursion[0]);

				return GameConstants.WIK_EQS;
			}
		}

		// 顺子判断
		int excursion[] = { 0, 1, 2 };
		for (int i = 0; i < excursion.length; i++) {
			int cbValueIndex = cur_index % 10;
			if ((cbValueIndex >= excursion[i]) && ((cbValueIndex - excursion[i]) <= 7)) {
				// 吃牌判断
				first_index = cur_index - excursion[i];
				if ((cards_index[first_index] == 0) || (cards_index[first_index] == 3) || (cards_index[first_index] == 4))
					continue;
				if ((cards_index[first_index + 1] == 0) || (cards_index[first_index + 1] == 3) || (cards_index[first_index + 1] == 4))
					continue;
				if ((cards_index[first_index + 2] == 0) || (cards_index[first_index + 2] == 3) || (cards_index[first_index + 2] == 4))
					continue;

				// 删除扑克
				cards_index[first_index]--;
				cards_index[first_index + 1]--;
				cards_index[first_index + 2]--;

				// 设置结果
				result_card[0] = switch_to_card_data(first_index);
				result_card[1] = switch_to_card_data(first_index + 1);
				result_card[2] = switch_to_card_data(first_index + 2);

				int chi_kind[] = { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
				return chi_kind[i];
			}
		}

		return GameConstants.WIK_NULL;
	}

	public int check_chi_phz(int cards_index[], int cur_card, int type_count[], int type_eat_count[], boolean yws_type) {
		int eat_type = 0;
		int count = 0;
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		// 三牌判断
		if (cards_index[cur_card_index] >= 3)
			return eat_type;
		// 大小搭吃
		int reverse_index = (cur_card_index + 10) % GameConstants.MAX_HH_INDEX;
		if ((cards_index[cur_card_index] >= 1) && (cards_index[reverse_index] >= 1) && (cards_index[reverse_index] < 3)) {

			int action = (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
			eat_type |= action;
			type_count[count++] = action;
		}
		// 大小搭吃
		if (cards_index[reverse_index] == 2) {

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
				if ((index != cur_card_index) && ((cards_index[index] == 0) || (cards_index[index] >= 3)))
					break;
			}
			if (i == excursion.length) {

				eat_type |= GameConstants.WIK_EQS;
				type_count[count++] = GameConstants.WIK_EQS;
			}
		}
		// 一五十吃
		if (yws_type == true) {
			if ((card_value == 1) || (card_value == 5) || (card_value == 10)) {
				int excursion[] = { 0, 4, 9 };
				int acceptIndex = (get_card_color(cur_card) == 0) ? 0 : 10;
				int i = 0;
				for (; i < excursion.length; i++) {
					int index = acceptIndex + excursion[i];
					if ((index != cur_card_index) && ((cards_index[index] == 0) || (cards_index[index] >= 3)))
						break;
				}
				if (i == excursion.length) {

					eat_type |= GameConstants.WIK_YWS;
					type_count[count++] = GameConstants.WIK_YWS;
				}
			}
		}
		// 顺子吃

		int excursion[] = { 0, 1, 2 };

		for (int i = 0; i < excursion.length; i++) {
			int value_index = cur_card_index % 10;
			if ((value_index >= excursion[i]) && (value_index - excursion[i] <= 7)) {
				int first_index = cur_card_index - excursion[i];
				if ((cur_card_index != first_index)
						&& ((cards_index[first_index] == 0) || (cards_index[first_index] == 3) || (cards_index[first_index] == 4)))
					continue;
				if ((cur_card_index != first_index + 1)
						&& ((cards_index[first_index + 1] == 0) || (cards_index[first_index + 1] == 3) || (cards_index[first_index + 1] == 4)))
					continue;
				if ((cur_card_index != first_index + 2)
						&& ((cards_index[first_index + 2] == 0) || (cards_index[first_index + 2] == 3) || (cards_index[first_index + 2] == 4)))
					continue;

				int chi_kind[] = { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
				eat_type |= chi_kind[i];
				type_count[count++] = chi_kind[i];
			}
		}

		type_eat_count[0] = count;
		return eat_type;

	}

	public int check_chi(int cards_index[], int cur_card, int type_count[], int type_eat_count[]) {
		int eat_type = 0;
		int count = 0;
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		// 三牌判断
		if (cards_index[cur_card_index] >= 3)
			return eat_type;
		// 大小搭吃
		int reverse_index = (cur_card_index + 10) % GameConstants.MAX_HH_INDEX;
		if ((cards_index[cur_card_index] >= 1) && (cards_index[reverse_index] >= 1) && (cards_index[reverse_index] < 3)) {

			int action = (get_card_color(cur_card) == 0x00) ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
			eat_type |= action;
			type_count[count++] = action;
		}
		// 大小搭吃
		if (cards_index[reverse_index] == 2) {

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
				if ((index != cur_card_index) && ((cards_index[index] == 0) || (cards_index[index] >= 3)))
					break;
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
				if ((cur_card_index != first_index)
						&& ((cards_index[first_index] == 0) || (cards_index[first_index] == 3) || (cards_index[first_index] == 4)))
					continue;
				if ((cur_card_index != first_index + 1)
						&& ((cards_index[first_index + 1] == 0) || (cards_index[first_index + 1] == 3) || (cards_index[first_index + 1] == 4)))
					continue;
				if ((cur_card_index != first_index + 2)
						&& ((cards_index[first_index + 2] == 0) || (cards_index[first_index + 2] == 3) || (cards_index[first_index + 2] == 4)))
					continue;

				int chi_kind[] = { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
				eat_type |= chi_kind[i];
				type_count[count++] = chi_kind[i];
			}

		}

		type_eat_count[0] = count;
		return eat_type;

	}

	/**
	 * 岳阳捉红字
	 * 
	 * @param cards_index
	 * @param cur_card
	 * @param type_count
	 * @param type_eat_count
	 * @return
	 */
	public int check_chi_yyzhz(int cards_index[], int cur_card, int type_count[], int type_eat_count[]) {
		int eat_type = 0;
		int count = 0;
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		/*
		 * // 三牌判断 if (cards_index[cur_card_index] >= 3) return eat_type;
		 */
		/*
		 * // 大小搭吃 int reverse_index = (cur_card_index + 10) %
		 * GameConstants.MAX_HH_INDEX; if ((cards_index[cur_card_index] >= 1) &&
		 * (cards_index[reverse_index] >= 1) && (cards_index[reverse_index] <
		 * 3)) {
		 * 
		 * int action = (get_card_color(cur_card) == 0x00) ?
		 * GameConstants.WIK_XXD : GameConstants.WIK_DDX; eat_type |= action;
		 * type_count[count++] = action; } // 大小搭吃 if
		 * (cards_index[reverse_index] == 2) {
		 * 
		 * int action = (get_card_color(cur_card) == 0x00) ?
		 * GameConstants.WIK_DDX : GameConstants.WIK_XXD; eat_type |= action;
		 * type_count[count++] = action; }
		 */
		// 二七十吃
		int card_value = get_card_value(cur_card);
		if ((card_value == 2) || (card_value == 7) || (card_value == 10)) {
			int excursion[] = { 1, 6, 9 };
			int acceptIndex = (get_card_color(cur_card) == 0) ? 0 : 10;
			int i = 0;
			for (; i < excursion.length; i++) {
				int index = acceptIndex + excursion[i];
				if ((index != cur_card_index) && ((cards_index[index] == 0) || (cards_index[index] >= 3)))
					break;
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
				if ((cur_card_index != first_index)
						&& ((cards_index[first_index] == 0) || (cards_index[first_index] == 3) || (cards_index[first_index] == 4)))
					continue;
				if ((cur_card_index != first_index + 1)
						&& ((cards_index[first_index + 1] == 0) || (cards_index[first_index + 1] == 3) || (cards_index[first_index + 1] == 4)))
					continue;
				if ((cur_card_index != first_index + 2)
						&& ((cards_index[first_index + 2] == 0) || (cards_index[first_index + 2] == 3) || (cards_index[first_index + 2] == 4)))
					continue;

				int chi_kind[] = { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
				eat_type |= chi_kind[i];
				type_count[count++] = chi_kind[i];
			}

		}

		type_eat_count[0] = count;
		return eat_type;

	}

	public int check_chi_wmq(int cards_index[], int cur_card) {
		int eat_type = 0;
		int count = 0;
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		// 顺子吃

		int excursion[] = { 0, 1, 2 };

		for (int i = 0; i < excursion.length; i++) {
			int value_index = cur_card_index % 10;
			if ((value_index >= excursion[i]) && (value_index - excursion[i] <= 7)) {
				int first_index = cur_card_index - excursion[i];
				if ((cur_card_index != first_index) && ((cards_index[first_index] == 0)))
					continue;
				if ((cur_card_index != first_index + 1) && ((cards_index[first_index + 1] == 0)))
					continue;
				if ((cur_card_index != first_index + 2) && ((cards_index[first_index + 2] == 0)))
					continue;

				int chi_kind[] = { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
				eat_type |= chi_kind[i];
			}

		}

		return eat_type;

	}

	// 检查二七十
	public int check_erqishi(int cards_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		int cur_card_index = switch_to_card_index(cur_card);
		switch (get_card_value(cur_card)) {
		case 2: {
			if (cards_index[cur_card_index + 5] >= 1 && cards_index[cur_card_index + 8] >= 1)
				return GameConstants.WIK_EQS;
			break;
		}
		case 7: {
			if (cards_index[cur_card_index - 5] >= 1 && cards_index[cur_card_index + 3] >= 1)
				return GameConstants.WIK_EQS;
			break;
		}
		case 10: {
			if (cards_index[cur_card_index - 8] >= 1 && cards_index[cur_card_index - 3] >= 1)
				return GameConstants.WIK_EQS;
			break;
		}
		}
		return 0;
	}

	/**
	 * 十八小
	 * 
	 * @param card_index
	 * @return
	 */
	public boolean is_shi_ba_xiao(AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (get_card_color(analyseItem.cbCenterCard[i]) == 0)
					count += 4;
				continue;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (get_card_color(analyseItem.cbCenterCard[i]) == 0)
					count += 3;
				continue;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_YWS:
			case GameConstants.WIK_EQS:
				for (int j = 0; j < 3; j++) {
					if (get_card_color(analyseItem.cbCardData[i][j]) == 0)
						count += 1;
				}
				continue;
			}
		}

		if ((analyseItem.curCardEye != false) && (get_card_color(analyseItem.cbCardEye) == 0))
			count += 2;
		return count >= 18;
	}

	public boolean check_lou_weave_phz(int cards_index[], int cur_card, int action, int type_count[], int type_eat_count,
			LouWeaveItem lou_weave_item[], int sub_lou_index, int card_count, boolean yws_type) {
		boolean bAction = false;
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return bAction;
		}
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		int cur_card_color = get_card_color(cur_card);
		int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			temp_cards_index[i] = cards_index[i];
		}
		lou_weave_item[sub_lou_index].nWeaveKind = action;

		int lou_index = 0;
		int temp_type_count[] = new int[10];
		int temp_eat_count[] = new int[1];
		int temp_action = check_chi_phz(temp_cards_index, cur_card, temp_type_count, temp_eat_count, yws_type);

		if ((temp_cards_index[cur_card_index] == 0) && (card_count > 2)) {
			for (int i = 0; i < temp_eat_count[0]; i++) {

				switch (temp_type_count[i]) {
				case GameConstants.WIK_LEFT:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_LEFT;
					bAction = true;
					break;
				case GameConstants.WIK_CENTER:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_CENTER;
					bAction = true;
					break;
				case GameConstants.WIK_RIGHT:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_RIGHT;
					bAction = true;
					break;
				case GameConstants.WIK_DDX:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_DDX;
					bAction = true;
					break;
				case GameConstants.WIK_XXD:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_XXD;
					bAction = true;
					break;
				case GameConstants.WIK_EQS:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_EQS;
					bAction = true;
					break;
				case GameConstants.WIK_YWS:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_YWS;
					bAction = true;
					break;
				}
			}
		}
		if (temp_cards_index[cur_card_index] == 1) {

			for (int i = 0; i < temp_eat_count[0]; i++) {
				int sec_type_count[] = new int[10];
				int sec_eat_count[] = new int[1];

				switch (temp_type_count[i]) {
				case GameConstants.WIK_LEFT: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index + 1]--;
					temp_cards_index[cur_card_index + 2]--;
					check_chi_phz(temp_cards_index, cur_card, sec_type_count, sec_eat_count, yws_type);
					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_LEFT;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						case GameConstants.WIK_YWS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_YWS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 1]++;
					temp_cards_index[cur_card_index + 2]++;

					break;
				}
				case GameConstants.WIK_CENTER: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index + 1]--;
					temp_cards_index[cur_card_index - 1]--;
					check_chi_phz(temp_cards_index, cur_card, sec_type_count, sec_eat_count, yws_type);
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_CENTER;
					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_CENTER;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						case GameConstants.WIK_YWS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_YWS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 1]++;
					temp_cards_index[cur_card_index - 1]++;

					break;
				}
				case GameConstants.WIK_RIGHT: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index - 1]--;
					temp_cards_index[cur_card_index - 2]--;
					check_chi_phz(temp_cards_index, cur_card, sec_type_count, sec_eat_count, yws_type);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_RIGHT;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						case GameConstants.WIK_YWS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_YWS;
							break;
						}

					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index - 1]++;
					temp_cards_index[cur_card_index - 2]++;

					break;

				}
				case GameConstants.WIK_DDX: {

					if (cur_card_color == 0) {
						if (card_count < 6)
							break;
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 10]--;
						temp_cards_index[cur_card_index + 10]--;
					} else {
						if (card_count < 3)
							break;
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_DDX;
						bAction = true;
						lou_index++;
						break;

					}
					check_chi_phz(temp_cards_index, cur_card, sec_type_count, sec_eat_count, yws_type);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_DDX;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						case GameConstants.WIK_YWS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_YWS;
							bAction = true;
							break;
						}
					}

					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 10]++;
					temp_cards_index[cur_card_index + 10]++;

					break;

				}
				case GameConstants.WIK_XXD: {
					if (cur_card_color == 0) {
						if (card_count < 3)
							break;
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_XXD;
						bAction = true;
						lou_index++;
						break;
					} else {
						if (card_count < 6)
							break;
						temp_cards_index[cur_card_index - 10]--;
						temp_cards_index[cur_card_index - 10]--;
						temp_cards_index[cur_card_index]--;
					}
					check_chi_phz(temp_cards_index, cur_card, sec_type_count, sec_eat_count, yws_type);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_XXD;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						case GameConstants.WIK_YWS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_YWS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index - 10]++;
					temp_cards_index[cur_card_index - 10]++;
					temp_cards_index[cur_card_index]++;

					break;

				}
				case GameConstants.WIK_EQS: {
					if (card_count < 6)
						break;
					int index[] = { 1, 6, 9 };
					int temp_index = ((cur_card_color == 1) ? 10 : 0);
					temp_cards_index[temp_index + index[0]]--;
					temp_cards_index[temp_index + index[1]]--;
					temp_cards_index[temp_index + index[2]]--;
					check_chi_phz(temp_cards_index, cur_card, sec_type_count, sec_eat_count, yws_type);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_EQS;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						case GameConstants.WIK_YWS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_YWS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[temp_index + index[0]]++;
					temp_cards_index[temp_index + index[1]]++;
					temp_cards_index[temp_index + index[2]]++;

					break;

				}
				case GameConstants.WIK_YWS: {
					if (card_count < 6)
						break;
					int index[] = { 0, 4, 9 };
					int temp_index = ((cur_card_color == 1) ? 10 : 0);
					temp_cards_index[temp_index + index[0]]--;
					temp_cards_index[temp_index + index[1]]--;
					temp_cards_index[temp_index + index[2]]--;
					check_chi_phz(temp_cards_index, cur_card, sec_type_count, sec_eat_count, yws_type);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_YWS;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						case GameConstants.WIK_YWS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_YWS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[temp_index + index[0]]++;
					temp_cards_index[temp_index + index[1]]++;
					temp_cards_index[temp_index + index[2]]++;

					break;
				}
				}
			}

		}

		lou_weave_item[sub_lou_index].nCount = lou_index;
		return bAction;
	}

	public boolean check_lou_weave(int cards_index[], int cur_card, int action, int type_count[], int type_eat_count, LouWeaveItem lou_weave_item[],
			int sub_lou_index, int card_count) {
		boolean bAction = false;
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return bAction;
		}
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		int cur_card_color = get_card_color(cur_card);
		int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			temp_cards_index[i] = cards_index[i];
		}
		lou_weave_item[sub_lou_index].nWeaveKind = action;

		int lou_index = 0;
		int temp_type_count[] = new int[10];
		int temp_eat_count[] = new int[1];
		int temp_action = check_chi(temp_cards_index, cur_card, temp_type_count, temp_eat_count);

		if ((temp_cards_index[cur_card_index] == 0) && (card_count > 2)) {
			for (int i = 0; i < temp_eat_count[0]; i++) {

				switch (temp_type_count[i]) {
				case GameConstants.WIK_LEFT:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_LEFT;
					bAction = true;
					break;
				case GameConstants.WIK_CENTER:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_CENTER;
					bAction = true;
					break;
				case GameConstants.WIK_RIGHT:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_RIGHT;
					bAction = true;
					break;
				case GameConstants.WIK_DDX:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_DDX;
					bAction = true;
					break;
				case GameConstants.WIK_XXD:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_XXD;
					bAction = true;
					break;
				case GameConstants.WIK_EQS:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_EQS;
					bAction = true;
					break;
				}
			}
		}
		if (temp_cards_index[cur_card_index] == 1) {

			for (int i = 0; i < temp_eat_count[0]; i++) {
				int sec_type_count[] = new int[10];
				int sec_eat_count[] = new int[1];

				switch (temp_type_count[i]) {
				case GameConstants.WIK_LEFT: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index + 1]--;
					temp_cards_index[cur_card_index + 2]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);
					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_LEFT;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 1]++;
					temp_cards_index[cur_card_index + 2]++;

					break;
				}
				case GameConstants.WIK_CENTER: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index + 1]--;
					temp_cards_index[cur_card_index - 1]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_CENTER;
					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_CENTER;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 1]++;
					temp_cards_index[cur_card_index - 1]++;

					break;
				}
				case GameConstants.WIK_RIGHT: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index - 1]--;
					temp_cards_index[cur_card_index - 2]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_RIGHT;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}

					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index - 1]++;
					temp_cards_index[cur_card_index - 2]++;

					break;

				}
				case GameConstants.WIK_DDX: {

					if (cur_card_color == 0) {
						if (card_count < 6)
							break;
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 10]--;
						temp_cards_index[cur_card_index + 10]--;
					} else {
						if (card_count < 3)
							break;
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_DDX;
						bAction = true;
						lou_index++;
						break;

					}
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_DDX;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}

					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 10]++;
					temp_cards_index[cur_card_index + 10]++;

					break;

				}
				case GameConstants.WIK_XXD: {
					if (cur_card_color == 0) {
						if (card_count < 3)
							break;
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_XXD;
						bAction = true;
						lou_index++;
						break;
					} else {
						if (card_count < 6)
							break;
						temp_cards_index[cur_card_index - 10]--;
						temp_cards_index[cur_card_index - 10]--;
						temp_cards_index[cur_card_index]--;
					}
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_XXD;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index - 10]++;
					temp_cards_index[cur_card_index - 10]++;
					temp_cards_index[cur_card_index]++;

					break;

				}
				case GameConstants.WIK_EQS: {
					if (card_count < 6)
						break;
					int index[] = { 1, 6, 9 };
					int temp_index = ((cur_card_color == 1) ? 10 : 0);
					temp_cards_index[temp_index + index[0]]--;
					temp_cards_index[temp_index + index[1]]--;
					temp_cards_index[temp_index + index[2]]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_EQS;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[temp_index + index[0]]++;
					temp_cards_index[temp_index + index[1]]++;
					temp_cards_index[temp_index + index[2]]++;

					break;

				}
				}
			}

		}

		lou_weave_item[sub_lou_index].nCount = lou_index;
		return bAction;
	}

	/**
	 * 岳阳捉红字落牌判断
	 * 
	 * @param cards_index
	 * @param cur_card
	 * @param action
	 * @param type_count
	 * @param type_eat_count
	 * @param lou_weave_item
	 * @param sub_lou_index
	 * @param card_count
	 * @return
	 */
	public boolean check_lou_weave_yyzhz(int cards_index[], int cur_card, int action, int type_count[], int type_eat_count,
			LouWeaveItem lou_weave_item[], int sub_lou_index, int card_count) {
		boolean bAction = false;
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return bAction;
		}
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		int cur_card_color = get_card_color(cur_card);
		int temp_cards_index[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			temp_cards_index[i] = cards_index[i];
		}
		lou_weave_item[sub_lou_index].nWeaveKind = action;

		int lou_index = 0;
		int temp_type_count[] = new int[10];
		int temp_eat_count[] = new int[1];
		int temp_action = check_chi_yyzhz(temp_cards_index, cur_card, temp_type_count, temp_eat_count);

		if ((temp_cards_index[cur_card_index] == 0) && (card_count > 2)) {
			for (int i = 0; i < temp_eat_count[0]; i++) {

				switch (temp_type_count[i]) {
				case GameConstants.WIK_LEFT:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_LEFT;
					bAction = true;
					break;
				case GameConstants.WIK_CENTER:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_CENTER;
					bAction = true;
					break;
				case GameConstants.WIK_RIGHT:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_RIGHT;
					bAction = true;
					break;
				case GameConstants.WIK_DDX:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_DDX;
					bAction = true;
					break;
				case GameConstants.WIK_XXD:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_XXD;
					bAction = true;
					break;
				case GameConstants.WIK_EQS:
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = GameConstants.WIK_EQS;
					bAction = true;
					break;
				}
			}
		}
		if (temp_cards_index[cur_card_index] >= 1) {

			for (int i = 0; i < temp_eat_count[0]; i++) {
				int sec_type_count[] = new int[10];
				int sec_eat_count[] = new int[1];

				switch (temp_type_count[i]) {
				case GameConstants.WIK_LEFT: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index + 1]--;
					temp_cards_index[cur_card_index + 2]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);
					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_LEFT;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 1]++;
					temp_cards_index[cur_card_index + 2]++;

					break;
				}
				case GameConstants.WIK_CENTER: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index + 1]--;
					temp_cards_index[cur_card_index - 1]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);
					lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_CENTER;
					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_CENTER;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 1]++;
					temp_cards_index[cur_card_index - 1]++;

					break;
				}
				case GameConstants.WIK_RIGHT: {
					if (card_count < 6)
						break;
					temp_cards_index[cur_card_index]--;
					temp_cards_index[cur_card_index - 1]--;
					temp_cards_index[cur_card_index - 2]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_RIGHT;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}

					}
					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index - 1]++;
					temp_cards_index[cur_card_index - 2]++;

					break;

				}
				case GameConstants.WIK_DDX: {

					if (cur_card_color == 0) {
						if (card_count < 6)
							break;
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 10]--;
						temp_cards_index[cur_card_index + 10]--;
					} else {
						if (card_count < 3)
							break;
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_DDX;
						bAction = true;
						lou_index++;
						break;

					}
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_DDX;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}

					temp_cards_index[cur_card_index]++;
					temp_cards_index[cur_card_index + 10]++;
					temp_cards_index[cur_card_index + 10]++;

					break;

				}
				case GameConstants.WIK_XXD: {
					if (cur_card_color == 0) {
						if (card_count < 3)
							break;
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_XXD;
						bAction = true;
						lou_index++;
						break;
					} else {
						if (card_count < 6)
							break;
						temp_cards_index[cur_card_index - 10]--;
						temp_cards_index[cur_card_index - 10]--;
						temp_cards_index[cur_card_index]--;
					}
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_XXD;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[cur_card_index - 10]++;
					temp_cards_index[cur_card_index - 10]++;
					temp_cards_index[cur_card_index]++;

					break;

				}
				case GameConstants.WIK_EQS: {
					if (card_count < 6)
						break;
					int index[] = { 1, 6, 9 };
					int temp_index = ((cur_card_color == 1) ? 10 : 0);
					temp_cards_index[temp_index + index[0]]--;
					temp_cards_index[temp_index + index[1]]--;
					temp_cards_index[temp_index + index[2]]--;
					check_chi(temp_cards_index, cur_card, sec_type_count, sec_eat_count);

					for (int j = 0; j < sec_eat_count[0]; j++) {
						lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index][0] = GameConstants.WIK_EQS;
						switch (sec_type_count[j]) {
						case GameConstants.WIK_LEFT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_LEFT;
							bAction = true;
							break;
						case GameConstants.WIK_CENTER:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_CENTER;
							bAction = true;
							break;
						case GameConstants.WIK_RIGHT:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_RIGHT;
							bAction = true;
							break;
						case GameConstants.WIK_DDX:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_DDX;
							bAction = true;
							break;
						case GameConstants.WIK_XXD:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_XXD;
							bAction = true;
							break;
						case GameConstants.WIK_EQS:
							lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][1] = GameConstants.WIK_EQS;
							bAction = true;
							break;
						}
					}
					temp_cards_index[temp_index + index[0]]++;
					temp_cards_index[temp_index + index[1]]++;
					temp_cards_index[temp_index + index[2]]++;

					break;

				}
				}
			}

		}

		lou_weave_item[sub_lou_index].nCount = lou_index;
		return bAction;
	}

	// 碰牌判断
	public int check_peng(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	// 碰牌判断
	public int check_peng_wmq(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	// 跑牌判断
	public int check_pao(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_PAO : GameConstants.WIK_NULL;
	}

	public int check_sao(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 2) ? GameConstants.WIK_SAO : GameConstants.WIK_NULL;
	}

	public int check_wei_wmq(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_WEI : GameConstants.WIK_NULL;
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

	public boolean is_man_yuan_hua(WeaveItem hu_weave_items[], int hu_weave_count) {
		boolean is_hua_man_yuan = false;
		int hong_zi_count = 0;
		for (int j = 0; j < hu_weave_count; j++) {
			if (hu_weave_items[j].weave_kind == GameConstants.WIK_NULL)
				break;
			switch (hu_weave_items[j].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_PENG: {
				if (color_hei(hu_weave_items[j].center_card) == false)
					hong_zi_count++;
				break;
			}
			case GameConstants.WIK_LEFT: {
				if (color_hei(hu_weave_items[j].center_card) == false)
					hong_zi_count++;

				if (color_hei(hu_weave_items[j].center_card + 1) == false)
					hong_zi_count++;

				if (color_hei(hu_weave_items[j].center_card + 2) == false)
					hong_zi_count++;
				break;
			}
			case GameConstants.WIK_CENTER: {
				if (color_hei(hu_weave_items[j].center_card) == false)
					hong_zi_count++;

				if (color_hei(hu_weave_items[j].center_card + 1) == false)
					hong_zi_count++;

				if (color_hei(hu_weave_items[j].center_card - 1) == false)
					hong_zi_count++;
				break;
			}
			case GameConstants.WIK_RIGHT: {
				if (color_hei(hu_weave_items[j].center_card) == false)
					hong_zi_count++;

				if (color_hei(hu_weave_items[j].center_card - 1) == false)
					hong_zi_count++;

				if (color_hei(hu_weave_items[j].center_card - 2) == false)
					hong_zi_count++;
				break;
			}
			case GameConstants.WIK_DUI_ZI: {
				if (color_hei(hu_weave_items[j].center_card) == false)
					hong_zi_count++;
				break;
			}
			}

		}

		if (hong_zi_count == 7)
			is_hua_man_yuan = true;

		return is_hua_man_yuan;

	}

	public void ming_index_temp(int cbMingIndexTemp[], WeaveItem weaveItems[], int weaveCount, boolean zimo, int cur_card) {
		if (zimo == false) {
			if (cur_card != 0)
				cbMingIndexTemp[switch_to_card_index(cur_card)] = 1;
		}
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_PENG:
				cbMingIndexTemp[switch_to_card_index(weaveItems[i].center_card)] = 1;
				break;
			case GameConstants.WIK_LEFT: {
				int card_index = this.switch_to_card_index(weaveItems[i].center_card);
				for (int j = card_index; j <= card_index + 2; j++) {
					cbMingIndexTemp[j]++;
				}
				break;
			}
			case GameConstants.WIK_CENTER: {
				int card_index = this.switch_to_card_index(weaveItems[i].center_card);
				for (int j = card_index - 1; j <= card_index + 1; j++) {
					cbMingIndexTemp[j]++;
				}
				break;
			}
			case GameConstants.WIK_RIGHT: {
				int card_index = this.switch_to_card_index(weaveItems[i].center_card);
				for (int j = card_index - 2; j <= card_index; j++) {
					cbMingIndexTemp[j]++;
				}
				break;
			}

			}
		}
		return;
	}

	public int calculate_dui_zi_hu_count(AnalyseItem analyseItem) {
		int dui_zi_count = 0;
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch (analyseItem.cbWeaveKind[j]) {
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_PENG: {
				dui_zi_count++;
				break;
			}
			}
		}
		if (analyseItem.cbCardEye != 0) {
			dui_zi_count++;
		}
		return dui_zi_count;
	}

	public void analyse_item_to_card(AnalyseItem analyseItem, int cbAnalyseIndexTemp[]) {
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch (analyseItem.cbWeaveKind[j]) {
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_PENG: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				cbAnalyseIndexTemp[card_index] += 3;
				break;
			}

			case GameConstants.WIK_LEFT: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index; i <= card_index + 2; i++) {
					cbAnalyseIndexTemp[i]++;
				}
				break;
			}
			case GameConstants.WIK_CENTER: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index - 1; i <= card_index + 1; i++) {
					cbAnalyseIndexTemp[i]++;
				}
				break;
			}
			case GameConstants.WIK_RIGHT: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index - 2; i <= card_index; i++) {
					cbAnalyseIndexTemp[i]++;
				}
				break;
			}
			}
		}
		if (analyseItem.cbCardEye != 0) {
			int card_index = this.switch_to_card_index(analyseItem.cbCardEye);
			cbAnalyseIndexTemp[card_index] += 2;
		}
		return;
	}

	public int get_all_hu_xi_weave(WeaveItem weave_items[], int weave_count, int cbMingIndexTemp[]) {
		int hu_xi = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cbMingIndexTemp[i] == 4)
				if (color_hei(switch_to_card_data(i)) == true)
					hu_xi += 5;
				else
					hu_xi += 7;

		}
		for (int j = 0; j < weave_count; j++) {
			if (weave_items[j].weave_kind == GameConstants.WIK_NULL)
				break;
			switch (weave_items[j].weave_kind) {
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI: {
				if (color_hei(weave_items[j].center_card) == true) {
					hu_xi += 3;

				} else {
					hu_xi += 4;
				}
				break;
			}
			case GameConstants.WIK_PENG: {
				if (color_hei(weave_items[j].center_card) == true) {
					hu_xi += 2;
				} else {
					hu_xi += 4;
				}
				break;
			}
			}
		}
		return hu_xi;

	}

	public int get_all_hu_xi_awq(AnalyseItem analyseItem, int cbAnalyseIndexTemp[], int cbMingIndexTemp[]) {
		int hu_xi = 0;
		boolean zhuan_wan = false;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cbAnalyseIndexTemp[i] == 4)
				if (color_hei(switch_to_card_data(i)) == true) {
					if (cbMingIndexTemp[i] >= 1)
						hu_xi += 5;
					else
						hu_xi += 6;
				} else {
					if (cbMingIndexTemp[i] >= 1)
						hu_xi += 7;
					else
						hu_xi += 8;
				}
		}
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch (analyseItem.cbWeaveKind[j]) {
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI: {
				if (color_hei(analyseItem.cbCenterCard[j]) == true) {
					hu_xi += 3;

				} else {
					hu_xi += 4;
				}
				break;
			}
			case GameConstants.WIK_PENG: {
				if (color_hei(analyseItem.cbCenterCard[j]) == true) {
					hu_xi += 2;
				} else {
					hu_xi += 3;
				}
				break;
			}

			case GameConstants.WIK_LEFT: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index; i <= card_index + 2; i++) {
					if ((this.switch_to_card_data(i) == analyseItem.cbCardEye) && (cbAnalyseIndexTemp[i] != 4)) {
						if (color_hei(analyseItem.cbCardEye) == true) {
							hu_xi += 2;
							zhuan_wan = true;
						} else {
							hu_xi += 3;
							zhuan_wan = true;
						}
						break;
					}
				}
				break;
			}
			case GameConstants.WIK_CENTER: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index - 1; i < +card_index + 1; i++) {
					if ((this.switch_to_card_data(i) == analyseItem.cbCardEye) && (cbAnalyseIndexTemp[i] != 4)) {
						if (color_hei(analyseItem.cbCardEye) == true) {
							hu_xi += 2;
							zhuan_wan = true;
						} else {
							hu_xi += 3;
							zhuan_wan = true;
						}
						break;
					}
				}
				break;
			}
			case GameConstants.WIK_RIGHT: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index - 2; i <= card_index; i++) {
					if ((this.switch_to_card_data(i) == analyseItem.cbCardEye) && (cbAnalyseIndexTemp[i] != 4)) {
						if (color_hei(analyseItem.cbCardEye) == true) {
							hu_xi += 2;
							zhuan_wan = true;
						} else {
							hu_xi += 3;
							zhuan_wan = true;
						}
						break;
					}
				}
				break;
			}
			}

		}
		if (zhuan_wan == false && color_hei(analyseItem.cbCardEye) == false)
			hu_xi += 2;
		return hu_xi;
	}

	public int get_weave_hu_xi_wmq(WeaveItem weave_item) {
		switch (weave_item.weave_kind) {
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			return (this.color_hei(weave_item.center_card) == false) ? 4 : 3;
		case GameConstants.WIK_PENG:
			return (this.color_hei(weave_item.center_card) == false) ? 3 : 2;
		}
		return 0;
	}

	public int get_xing_pai_count(WeaveItem weave_item[], int weave_count, int card) {
		int count = 0;
		for (int i = 0; i < weave_count; i++) {
			switch (weave_item[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				count += (weave_item[i].center_card == card) ? 4 : 0;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_PENG:
				count += (weave_item[i].center_card == card) ? 3 : 0;
				break;
			case GameConstants.WIK_EQS: {
				if (weave_item[i].center_card < 16)
					switch (card) {
					case 0x02:
					case 0x07:
					case 0x0a:

						count += 1;
						break;
					}
				if (weave_item[i].center_card > 16)
					switch (card) {
					case 0x12:
					case 0x17:
					case 0x1a:

						count += 1;
						break;
					}
				break;

			}
			case GameConstants.WIK_YWS: {
				if (weave_item[i].center_card < 16)
					switch (card) {
					case 0x01:
					case 0x05:
					case 0x0a:
						count += 1;
						break;
					}
				if (weave_item[i].center_card > 16)
					switch (card) {
					case 0x11:
					case 0x15:
					case 0x1a:
						count += 1;
						break;
					}
				break;
			}

			case GameConstants.WIK_LEFT: {
				if ((weave_item[i].center_card == card) || (weave_item[i].center_card + 1 == card) || (weave_item[i].center_card + 2 == card)) {
					count += 1;
				}
				break;
			}
			case GameConstants.WIK_CENTER: {
				if ((weave_item[i].center_card == card) || (weave_item[i].center_card + 1 == card) || (weave_item[i].center_card - 1 == card)) {
					count += 1;
				}

				break;
			}
			case GameConstants.WIK_RIGHT: {
				if ((weave_item[i].center_card == card) || (weave_item[i].center_card - 1 == card) || (weave_item[i].center_card - 2 == card)) {
					count += 1;
				}

				break;
			}
			case GameConstants.WIK_DDX: {
				if (weave_item[i].center_card > 16) {
					if (weave_item[i].center_card == card)
						count += 2;
					else if (weave_item[i].center_card - 16 == card)
						count += 1;
				} else {
					if (weave_item[i].center_card < 16) {
						if (weave_item[i].center_card == card)
							count += 1;
						else if (weave_item[i].center_card + 16 == card)
							count += 2;
					}
				}
				break;
			}
			case GameConstants.WIK_XXD: {
				if (weave_item[i].center_card > 16) {
					if (weave_item[i].center_card == card)
						count += 1;
					else if (weave_item[i].center_card - 16 == card)
						count += 2;
				} else {
					if (weave_item[i].center_card < 16) {
						if (weave_item[i].center_card == card)
							count += 2;
						else if (weave_item[i].center_card + 16 == card)
							count += 1;
					}
				}
				break;
			}
			case GameConstants.WIK_DUI_ZI: {
				if (weave_item[i].center_card == card)
					count += 2;
			}

			}
		}
		return count;
	}

	public int get_analyse_hu_xi(int cbWeaveKind, int center_card) {
		switch (cbWeaveKind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_AN_LONG_LIANG:
			return (get_card_color(center_card) != 0) ? 12 : 9;
		case GameConstants.WIK_PAO:
			return (get_card_color(center_card) != 0) ? 9 : 6;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			return (get_card_color(center_card) != 0) ? 6 : 3;
		case GameConstants.WIK_PENG:
			return (get_card_color(center_card) != 0) ? 3 : 1;
		case GameConstants.WIK_EQS:
			return (get_card_color(center_card) != 0) ? 6 : 3;
		case GameConstants.WIK_YWS:
			return (get_card_color(center_card) != 0) ? 6 : 3;
		case GameConstants.WIK_LEFT: {
			int card_value = get_card_value(center_card);
			if (card_value == 1) {
				return (get_card_color(center_card) != 0) ? 6 : 3;
			}
			break;
		}
		case GameConstants.WIK_CENTER: {
			int card_value = get_card_value(center_card);
			if (card_value == 2) {
				return (get_card_color(center_card) != 0) ? 6 : 3;
			}
			break;
		}
		case GameConstants.WIK_RIGHT: {
			int card_value = get_card_value(center_card);
			if (card_value == 3) {
				return (get_card_color(center_card) != 0) ? 6 : 3;
			}
			break;
		}

		}
		return 0;
	}

	/**
	 * 杠牌判断 别人打的牌自己能不能杠
	 * 
	 * @param card_index
	 *            当前牌型
	 * @param cur_card
	 *            出的牌
	 * @return
	 */
	public int estimate_pao_card_out_card(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_PAO : GameConstants.WIK_NULL;
	}

	/**
	 * 跑牌分析
	 */
	public int analyse_first_pao_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_PAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗龙
				gangCardResult.type[index] = GameConstants.PAO_TYPE_AN_LONG;

			}
		}

		return cbActionMask;
	}

	/**
	 * 跑牌分析
	 */
	public int analyse_pao_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_PAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 提龙
				gangCardResult.type[index] = GameConstants.PAO_TYPE_TI_MINE_LONG;

			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_PAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明杠
						gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
					}
				}
				if (WeaveItem[i].weave_kind == GameConstants.WIK_SAO) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_PAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明杠
						gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_SAO_LONG;
					}
				}
			}
		}

		return cbActionMask;
	}

	// 提牌判断
	public int get_action_ti_Card(int cards_index[], int ti_cards_index[]) {
		int ti_card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 4) {
				ti_cards_index[ti_card_count++] = cards_index[i];
			}
		}
		return ti_card_count;
	}

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index--手牌
	 * @param WeaveItem
	 *            --落地牌
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆）
	 * @return
	 */
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_card_hh(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上有3张
		if (cards_index[this.switch_to_card_index(card)] == 3) {
			cbActionMask |= GameConstants.WIK_PAO;
			int index = gangCardResult.cbCardCount++;
			gangCardResult.cbCardData[index] = card;
			gangCardResult.isPublic[index] = 0;// 明刚
			gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			return cbActionMask;
		}

		if (check_weave) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (WeaveItem[i].center_card == card) {
						cbActionMask |= GameConstants.WIK_PAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
						break;
					}
				}
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card_hand_card(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[i] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		// 组合杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_GANG;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明杠
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					break;
				}
			}
		}

		return cbActionMask;
	}

	// 跑牌分析 ，分析自己抓的牌是跑还是提
	public int analyse_pao_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_PAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.PAO_TYPE_TI_MINE_LONG;
			}
		}

		// 组合杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_PAO;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明杠
					gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
					break;
				}
			} else if (WeaveItem[i].weave_kind == GameConstants.WIK_SAO) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_PAO;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明杠
					gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_SAO_LONG;
					break;
				}
			}
		}

		return cbActionMask;
	}

	// 是否单吊
	public boolean is_dan_diao(WeaveItem weaveItem[], int weave_count, int cards_index[], int cur_card) {

		// 四碰判断
		if (weave_count != 4) {
			return false;
		}
		for (int i = 0; i < weave_count; i++) {
			if (weaveItem[i].weave_kind != GameConstants_YYZHZ.WIK_PENG) {
				return false;
			}
		}

		// 单吊判读
		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		boolean has_magic = false;
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			if (cbCardCount == 0) {
				continue;
			}
			if (is_magic_index(i)) {
				has_magic = true;
			}
			if (cbCardCount == 2) {
				bDuizi = true;
			}
			nTaltal += cbCardCount;
		}

		if (bDuizi && nTaltal == 2) {
			return true;
		} else if (has_magic && nTaltal == 2) {
			return true;
		}
		return false;

	}

	/**
	 * 计算牌数量
	 * 
	 * @param analyseItem
	 * @return
	 */
	public int calculate_all_pai_count(AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				count += 4;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_EQS:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_YWS:

				count += 3;
				break;
			}

		}
		if (analyseItem.curCardEye != false)
			count += 2;
		return count;

	}

	public boolean color_hei(int card) {
		boolean b_hei = false;
		int value = get_card_value(card);
		switch (value) {
		case 2:
		case 7:
		case 10:
			b_hei = false;
			break;
		default:
			b_hei = true;
		}

		return b_hei;
	}

	/**
	 * 岳阳捉红字
	 * 
	 * @param card
	 * @return
	 */
	public boolean color_hei_yyzhz(int card) {
		boolean b_hei = false;
		int value = get_card_value(card);
		switch (value) {
		case 2:
		case 7:
		case 10:
			b_hei = false;
			break;
		default:
			b_hei = true;
		}
		// WalkerGeek 王牌为红色

		if (get_card_color(card) == 2) {
			b_hei = false;
		}
		return b_hei;
	}

	public int calculate_hong_pai_count(AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (color_hei(analyseItem.cbCenterCard[i]) == false)
					count += 4;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (color_hei(analyseItem.cbCenterCard[i]) == false)
					count += 3;
				break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_YWS:
				for (int j = 0; j < 3; j++) {
					if (color_hei(analyseItem.cbCardData[i][j]) == false)
						count += 1;

				}
				break;
			case GameConstants.WIK_EQS:
				count += 3;

				break;
			}

		}
		if ((analyseItem.curCardEye != false) && (color_hei(analyseItem.cbCardEye) == false))

			count += 2;
		return count;

	}

	public int calculate_hei_pai_count(AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (color_hei(analyseItem.cbCenterCard[i]) == true)
					count += 4;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (color_hei(analyseItem.cbCenterCard[i]) == true)
					count += 3;
				break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_YWS:
				for (int j = 0; j < 3; j++) {
					if (color_hei(analyseItem.cbCardData[i][j]) == true)
						count += 1;

				}

				break;
			}

		}
		if ((analyseItem.curCardEye != false) && (color_hei(analyseItem.cbCardEye) == true))

			count += 2;
		return count;

	}

	public int calculate_weave_hei_pai(WeaveItem weave_item) {
		int count = 0;
		switch (weave_item.weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
			if (color_hei(weave_item.center_card) == true)
				count += 4;
			break;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			if (color_hei(weave_item.center_card) == true)
				count += 3;
			break;
		case GameConstants.WIK_LEFT:
			if (color_hei(weave_item.center_card) == true)
				count += 1;
			if (color_hei(weave_item.center_card + 1) == true)
				count += 1;
			if (color_hei(weave_item.center_card + 2) == true)
				count += 1;
			break;
		case GameConstants.WIK_CENTER:
			if (color_hei(weave_item.center_card) == true)
				count += 1;
			if (color_hei(weave_item.center_card + 1) == true)
				count += 1;
			if (color_hei(weave_item.center_card - 1) == true)
				count += 1;
			break;
		case GameConstants.WIK_RIGHT:
			if (color_hei(weave_item.center_card) == true)
				count += 1;
			if (color_hei(weave_item.center_card - 1) == true)
				count += 1;
			if (color_hei(weave_item.center_card - 2) == true)
				count += 1;
			break;
		case GameConstants.WIK_YWS:
			count += 2;
			break;

		}
		return count;

	}

	public int calculate_weave_hong_pai(WeaveItem weave_item) {
		int count = 0;
		switch (weave_item.weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
			if (color_hei(weave_item.center_card) == false)
				count += 4;
			break;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			if (color_hei(weave_item.center_card) == false)
				count += 3;
			break;
		case GameConstants.WIK_LEFT:
			if (color_hei(weave_item.center_card) == false)
				count += 1;
			if (color_hei(weave_item.center_card + 1) == false)
				count += 1;
			if (color_hei(weave_item.center_card + 2) == false)
				count += 1;
			break;
		case GameConstants.WIK_CENTER:
			if (color_hei(weave_item.center_card) == false)
				count += 1;
			if (color_hei(weave_item.center_card + 1) == false)
				count += 1;
			if (color_hei(weave_item.center_card - 1) == false)
				count += 1;
			break;
		case GameConstants.WIK_RIGHT:
			if (color_hei(weave_item.center_card) == false)
				count += 1;
			if (color_hei(weave_item.center_card - 1) == false)
				count += 1;
			if (color_hei(weave_item.center_card - 2) == false)
				count += 1;
			break;
		case GameConstants.WIK_EQS:
			count += 3;
			break;
		case GameConstants.WIK_YWS:
			count += 1;
			break;

		}
		return count;

	}

	public int get_da_card(int weave_kind, int center_card) {
		int count = 0;
		switch (weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
			if (center_card > 16)
				count += 4;
			break;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_PENG:

		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			if (center_card > 16)
				count += 3;
			break;
		case GameConstants.WIK_XXD:
			count += 1;
			break;
		case GameConstants.WIK_DDX:
			count += 2;
			break;
		case GameConstants.WIK_LEFT:
			if (center_card > 16)
				count += 1;
			if (center_card + 1 > 16)
				count += 1;
			if (center_card + 2 > 16)
				count += 1;
			break;
		case GameConstants.WIK_CENTER:
			if (center_card > 16)
				count += 1;
			if (center_card + 1 > 16)
				count += 1;
			if (center_card - 1 > 16)
				count += 1;
			break;
		case GameConstants.WIK_RIGHT:
			if (center_card > 16)
				count += 1;
			if (center_card - 1 > 16)
				count += 1;
			if (center_card - 2 > 16)
				count += 1;
			break;
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YWS:
			if (center_card > 16)
				count += 3;
			break;
		case GameConstants.WIK_DUI_ZI:
			if (center_card > 16)
				count += 2;
			break;

		}
		return count;
	}

	public int get_xiao_card(int weave_kind, int center_card) {
		int count = 0;
		switch (weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
			if (center_card < 16)
				count += 4;
			break;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			if (center_card < 16)
				count += 3;
			break;
		case GameConstants.WIK_XXD:
			count += 2;
			break;
		case GameConstants.WIK_DDX:
			count += 1;
			break;
		case GameConstants.WIK_LEFT:
			if (center_card < 16)
				count += 1;
			if (center_card + 1 < 16)
				count += 1;
			if (center_card + 2 < 16)
				count += 1;
			break;
		case GameConstants.WIK_CENTER:
			if (center_card < 16)
				count += 1;
			if (center_card + 1 < 16)
				count += 1;
			if (center_card - 1 < 16)
				count += 1;
			break;
		case GameConstants.WIK_RIGHT:
			if (center_card < 16)
				count += 1;
			if (center_card - 1 < 16)
				count += 1;
			if (center_card - 2 < 16)
				count += 1;
			break;
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YWS:
			if (center_card < 16)
				count += 3;
			break;
		case GameConstants.WIK_DUI_ZI:
			if (center_card < 16)
				count += 2;
			break;

		}
		return count;
	}

	public int get_ying(int weave_kind, int center_card) {
		switch (weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
			if (color_hei(center_card) == false)
				return 1;
		}
		return 0;
	}

	public int get_piao(int weave_kind, int center_card) {
		int count = 0;
		switch (weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
			if (color_hei(center_card) == false)
				return 1;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			if (color_hei(center_card) == false)
				return 1;
		case GameConstants.WIK_XXD:
			if (color_hei(center_card) == false)
				return 1;
		case GameConstants.WIK_DDX:
			if (color_hei(center_card) == false)
				return 1;
		case GameConstants.WIK_LEFT: {
			int hong_count = 0;
			if (color_hei(center_card) == false)
				hong_count++;
			if (color_hei(center_card + 1) == false)
				hong_count++;
			if (color_hei(center_card + 1) == false)
				hong_count++;
			if (hong_count > 0)
				return 0;
			else
				return 2;
		}
		case GameConstants.WIK_CENTER: {
			int hong_count = 0;
			if (color_hei(center_card - 1) == false)
				hong_count++;
			if (color_hei(center_card) == false)
				hong_count++;
			if (color_hei(center_card + 1) == false)
				hong_count++;
			if (hong_count > 0)
				return 0;
			else
				return 2;
		}
		case GameConstants.WIK_RIGHT: {
			int hong_count = 0;
			if (color_hei(center_card) == false)
				hong_count++;
			if (color_hei(center_card - 1) == false)
				hong_count++;
			if (color_hei(center_card - 2) == false)
				hong_count++;
			if (hong_count > 0)
				return 0;
			else
				return 2;
		}
		case GameConstants.WIK_EQS:
			if (color_hei(center_card) == false)
				return 1;
		case GameConstants.WIK_YWS:
			if (color_hei(center_card) == false)
				return 2;
			break;
		case GameConstants.WIK_DUI_ZI:
			if (color_hei(center_card) == false)
				return 1;
		}
		return count;
	}

	public int calculate_tuan_count(WeaveItem hu_weave_item[], int hu_weave_count) {
		int count = 0;
		for (int i = 0; i < hu_weave_count - 1; i++) {
			if (!(hu_weave_item[i].weave_kind == GameConstants.WIK_PAO || hu_weave_item[i].weave_kind == GameConstants.WIK_TI_LONG
					|| hu_weave_item[i].weave_kind == GameConstants.WIK_AN_LONG))
				continue;
			for (int j = i + 1; j < hu_weave_count; j++) {
				if (!(hu_weave_item[j].weave_kind == GameConstants.WIK_PAO || hu_weave_item[j].weave_kind == GameConstants.WIK_TI_LONG
						|| hu_weave_item[j].weave_kind == GameConstants.WIK_AN_LONG))
					continue;
				if (get_card_value(hu_weave_item[i].center_card) == get_card_value(hu_weave_item[j].center_card))
					count++;
			}
		}
		return count;
	}

	public boolean calculate_shun(WeaveItem hu_weave_item[], int hu_weave_count) {
		int card[] = new int[7];
		Arrays.fill(card, 0);
		int index = 0;
		boolean flag = false;
		for (int i = 0; i < hu_weave_count - 1; i++) {
			if (!(hu_weave_item[i].weave_kind == GameConstants.WIK_PAO || hu_weave_item[i].weave_kind == GameConstants.WIK_TI_LONG
					|| hu_weave_item[i].weave_kind == GameConstants.WIK_AN_LONG))
				continue;
			for (int j = i + 1; j < hu_weave_count; j++) {
				if (!(hu_weave_item[j].weave_kind == GameConstants.WIK_PAO || hu_weave_item[j].weave_kind == GameConstants.WIK_TI_LONG
						|| hu_weave_item[j].weave_kind == GameConstants.WIK_AN_LONG))
					continue;
				if (index == 0)
					card[index++] = hu_weave_item[i].center_card;
				else if (card[index - 1] > hu_weave_item[i].center_card) {
					int temp = index - 1;
					card[index] = card[temp];
					card[temp] = hu_weave_item[i].center_card;
					index++;
				}
			}
		}
		if (index < 2)
			return false;
		for (int i = 0; i < index - 1; i++) {
			if (card[i] + 1 == card[i + 1]) {
				return true;
			}
		}

		Arrays.fill(card, 0);
		index = 0;
		for (int i = 0; i < hu_weave_count - 1; i++) {
			if (!(hu_weave_item[i].weave_kind == GameConstants.WIK_WEI || hu_weave_item[i].weave_kind == GameConstants.WIK_PENG
					|| hu_weave_item[i].weave_kind == GameConstants.WIK_CHOU_WEI || hu_weave_item[i].weave_kind == GameConstants.WIK_KAN))
				continue;
			if (index == 0)
				card[index++] = hu_weave_item[i].center_card;
			else if (card[index - 1] > hu_weave_item[i].center_card) {
				int temp = index - 1;
				card[index] = card[temp];
				card[temp] = hu_weave_item[i].center_card;
				index++;
			}

		}
		if (index < 3)
			return false;
		for (int i = 0; i < index - 2; i++) {
			if (card[i] + 1 == card[i + 1] && card[i] + 2 == card[i + 2]) {
				return true;
			}
		}

		return false;
	}

	public boolean is_calculate_tuan_yuan(WeaveItem hu_weave_item[], int hu_weave_count) {
		int count = 0;
		int card_value[] = new int[6];
		for (int i = 0; i < hu_weave_count - 1; i++) {
			if (!(hu_weave_item[i].weave_kind == GameConstants.WIK_PAO || hu_weave_item[i].weave_kind == GameConstants.WIK_TI_LONG
					|| hu_weave_item[i].weave_kind == GameConstants.WIK_AN_LONG))
				continue;
			for (int j = i + 1; j < hu_weave_count; j++) {
				if (!(hu_weave_item[j].weave_kind == GameConstants.WIK_PAO || hu_weave_item[j].weave_kind == GameConstants.WIK_TI_LONG
						|| hu_weave_item[j].weave_kind == GameConstants.WIK_AN_LONG))
					continue;
				if (get_card_value(hu_weave_item[i].center_card) == get_card_value(hu_weave_item[j].center_card)) {
					card_value[count] = get_card_value(hu_weave_item[i].center_card);
					int temp = count;
					for (int k = count - 1; k > 0; k++) {
						if (card_value[temp] < card_value[k]) {
							int value_temp = card_value[k];
							card_value[k] = card_value[temp];
							card_value[temp] = value_temp;
						}
					}
					count++;
				}
			}
		}
		for (int i = 1; i < count; i++) {
			if (card_value[i - 1] + 1 == card_value[i])
				return true;
		}
		return false;
	}

	public int is_hong(WeaveItem hu_weave_item[], int hu_weave_count) {
		boolean da_er = false;
		boolean da_qi = false;
		boolean da_shi = false;
		boolean xiao_er = false;
		boolean xiao_qi = false;
		boolean xiao_shi = false;
		for (int i = 0; i < hu_weave_count; i++) {
			switch (hu_weave_item[i].weave_kind) {
			case GameConstants.WIK_PAO:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_KAN: {
				if (get_card_value(hu_weave_item[i].center_card) == 2) {
					if (get_card_color(hu_weave_item[i].center_card) == 1)
						da_er = true;
					else
						xiao_er = true;

				}
				if (get_card_value(hu_weave_item[i].center_card) == 7) {
					if (get_card_color(hu_weave_item[i].center_card) == 1)
						da_qi = true;
					else
						xiao_qi = true;

				}
				if (get_card_value(hu_weave_item[i].center_card) == 10) {
					if (get_card_color(hu_weave_item[i].center_card) == 1)
						da_shi = true;
					else
						xiao_shi = true;

				}
			}
			}
		}
		if (da_er & da_qi & da_shi & xiao_er & xiao_qi & xiao_shi)
			return 3;
		if (da_er & da_qi & da_shi)
			return 2;
		if (xiao_er & xiao_qi & xiao_shi)
			return 1;

		return 0;
	}

	public boolean is_bei_kao_bei(int _hu_cards[], int count, int cur_card, AnalyseItem analyseItem, int weaveCount) {
		int bei_kao_bei = 0;
		for (int j = 0; j < 6 - weaveCount; j++) {
			if (analyseItem.cbWeaveKind[j + weaveCount] == GameConstants.WIK_NULL)
				break;
			switch (analyseItem.cbWeaveKind[j + weaveCount]) {
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_CHOU_WEI: {
				if (analyseItem.cbCenterCard[j + weaveCount] == cur_card)
					bei_kao_bei++;
				break;
			}
			}
		}
		if (analyseItem.cbCardEye % 16 == cur_card % 16)
			bei_kao_bei++;
		if (bei_kao_bei >= 2)
			return true;
		return false;
	}

	public boolean calculate_pengpeng_count(WeaveItem hu_weave_item[], int hu_weave_count) {

		for (int i = 0; i < hu_weave_count; i++) {
			switch (hu_weave_item[i].weave_kind) {

			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_EQS:
			case GameConstants.WIK_YWS:
				return false;
			}
		}
		return true;
	}

	public boolean is_pengpeng_hu(AnalyseItem analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i] & (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		boolean same = true;
		int lastCard = 0;
		int num = 0;
		// for (int eye : analyseItem.cbCardEye) {
		// if(eye==0) continue;
		// if(lastCard==0) {
		// lastCard = eye;
		// }
		// if(eye!=lastCard) {
		// same= false;
		// }
		// num++;
		// }
		return analyseItem.isShuangDui || (same && num == 4);// 2对子
	}

	// 大对子,碰碰胡
	public boolean is_pengpeng_hu_yyzhz(AnalyseItem analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i] & (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		return true;
	}

	public boolean is_ju_ju_hong(AnalyseItem analyseItem) {
		boolean flag = false;
		// 牌眼是红色
		if (color_hei_yyzhz(analyseItem.cbCardEye)) {
			return flag;
		}
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i] & (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) == 0) {
				int hong_count = 0;
				for (int j = 0; j < analyseItem.cbCardData[i].length; j++) {
					int card_data = analyseItem.cbCardData[i][j];
					if (switch_to_card_index_yyzhz(card_data) == 0) {
						continue;
					}
					if (color_hei_yyzhz(card_data)) {
						hong_count++;
					}
				}
				if (hong_count == 1) {
					return flag;
				}
			}
		}
		return true;
	}

	/**
	 * 落地牌 是否满足
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_pengpeng_hu_down(WeaveItem weaveItems[], int weaveCount) {
		boolean isPengPengHu = true;
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind != GameConstants.WIK_PENG && weaveItems[i].weave_kind != GameConstants.WIK_GANG
					&& weaveItems[i].weave_kind != GameConstants.WIK_ZHAO)) {
				isPengPengHu = false;
				break;
			}
		}
		return isPengPengHu;
	}

	/**
	 * 落地牌 是否暗 杠 明杠
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_an_gang(WeaveItem weaveItems[], int weaveCount) {
		boolean isAnGang = true;
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind != GameConstants.WIK_GANG && weaveItems[i].weave_kind != GameConstants.WIK_ZHAO)) {
				isAnGang = false;
				break;
			}
			if (weaveItems[i].getPublic_card() == 1) {
				isAnGang = false;
				break;
			}
		}
		return isAnGang;
	}

	/**
	 * 判断牌眼是否有成句
	 * 
	 * @param cards_index
	 * @return
	 */
	public boolean isChengJu(int cards_index[], AnalyseItem analyseItem) {

		int cbMagicCardIndex[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int num = cbMagicCardIndex[i] = cards_index[i];
			if (num > 0) {
				for (int j = 0; j < num; j++) {
					// for (int k = 0; k < 4; k++) {
					// if (analyseItem.cbCardEye[k] == 0) {
					// analyseItem.cbCardEye[k] = switch_to_card_data(i);// 复制牌眼
					// break;
					// }
					// }
				}
			}
		}

		int mj_count = GameConstants.MAX_HH_INDEX;
		for (int i = 0; i < mj_count; i++) {
			// 同牌判断
			if (cbMagicCardIndex[i] == 3) {
				// if (analyseItem != null) {
				// analyseItem.cbWeaveKind[analyseItem.cbWeaveKind.length - 1] =
				// GameConstants.WIK_PENG;
				// analyseItem.cbCenterCard[analyseItem.cbWeaveKind.length - 1]
				// = switch_to_card_data(i);
				// analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][0]
				// = switch_to_card_data(i);
				// analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][1]
				// = switch_to_card_data(i);
				// analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][2]
				// = switch_to_card_data(i);
				// }
				analyseItem.eyeKind = GameConstants.WIK_PENG;
				analyseItem.eyeCenterCard = switch_to_card_data(i);
				return true;
			} // 同牌判断 end
				// 连牌判断
			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && ((i % 3) == 0)) {
				// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
				int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] + cbMagicCardIndex[i + 2];
				if (chi_count >= 3) {
					if (cbMagicCardIndex[i] >= 1 && cbMagicCardIndex[i + 1] >= 1 && cbMagicCardIndex[i + 2] >= 1) {
						// if (analyseItem != null) {
						// analyseItem.cbWeaveKind[analyseItem.cbWeaveKind.length
						// - 1] = GameConstants.WIK_LEFT;
						// analyseItem.cbCenterCard[analyseItem.cbWeaveKind.length
						// - 1] = switch_to_card_data(i);
						// analyseItem.cbCardData[analyseItem.cbWeaveKind.length
						// - 1][0] = switch_to_card_data(i);
						// analyseItem.cbCardData[analyseItem.cbWeaveKind.length
						// - 1][1] = switch_to_card_data(i + 1);
						// analyseItem.cbCardData[analyseItem.cbWeaveKind.length
						// - 1][2] = switch_to_card_data(i + 2);
						// }
						analyseItem.eyeKind = GameConstants.WIK_LEFT;
						analyseItem.eyeCenterCard = switch_to_card_data(i);
						return true;
					}
				}
			} // 连牌判断 end
		}
		return false;
	}

	/**
	 * 判断牌眼是否成双
	 * 
	 * @param cards_index
	 * @param cardEyes
	 * @return
	 */
	public boolean isChengShuang(int cards_index[], AnalyseItem analyseItem) {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		boolean isShuang = true;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int num = cards_index[i];

			if (num > 0) {

				// for (int j = 0; j < num; j++) {
				// for (int k = 0; k < 4; k++) {
				// if (analyseItem.cbCardEye[k] == 0) {
				// analyseItem.cbCardEye[k] = switch_to_card_data(i);// 复制牌眼
				// break;
				// }
				// }
				// }

				if (num != 2) {
					isShuang = false;// 判断牌眼是否 双对
				}

				int card_data = switch_to_card_data(i);
				int color = get_card_color(card_data);
				Integer value = map.get(color);
				if (value == null)
					value = 0;
				map.put(color, value + num);

			}
		}
		analyseItem.isShuangDui = isShuang;
		// for (Entry<Integer, Integer> entry : map.entrySet()) {
		// if (entry.getValue() == 0)
		// continue;
		// if (entry.getValue() != 2 && entry.getValue() != 4) {
		// return false;
		// }
		// }
		return true;
	}

	/**
	 * 只处理牌眼 4张的情况
	 * 
	 * @param cards_index
	 * @param cardEyes
	 * @return
	 */
	public boolean isYankou(int cards_index[], AnalyseItem analyseItem) {

		int cbCardCount = get_card_count_by_index(cards_index);

		if (cbCardCount == 1)
			return true;

		if (cbCardCount != 4) {
			return false;
		}

		boolean chengshuang = isChengShuang(cards_index, analyseItem);

		if (chengshuang)
			return true;

		boolean chengju = isChengJu(cards_index, analyseItem);

		return chengju;
	}

	public boolean isInCardEye(AnalyseItem analyseItem, int cur_card) {
		boolean isEyes = false;
		// for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
		// if (analyseItem.cbCardEye[i] == cur_card) {
		// isEyes = true;
		// break;
		// }
		// }
		return isEyes;
	}

	/**
	 * 当前摸牌 是不是牌眼
	 * 
	 * @param cardEyes
	 * @param cur_card
	 * @return
	 */
	private boolean isCurCardEye(AnalyseItem analyseItem, int cur_card) {
		boolean isEyes = false;
		// for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
		// if (analyseItem.cbCardEye[i] == cur_card) {
		// isEyes = true;
		// break;
		// }
		// }

		if ((analyseItem.eyeKind == GameConstants.WIK_LEFT || analyseItem.eyeKind == GameConstants.WIK_PENG) && isEyes
				&& isCurCard(cur_card, analyseItem.eyeCenterCard)) {// 牌眼一张
																	// 单调才是满天飞
			return true;
		}
		return false;
	}

	private boolean isCurCard(int cur_card, int eyeCenterCard) {
		if (cur_card == eyeCenterCard)
			return false;

		int index = switch_to_card_index(cur_card);

		int eyeindex = switch_to_card_index(eyeCenterCard);

		if (index == eyeindex + 1 || index == eyeindex + 2)
			return false;

		return true;
	}

	// 分析扑克
	public boolean analyse_card_fphz(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index, int cur_card,
			List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[], boolean yws_type) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		hu_xi[0] = 0;
		// 跑胡判断
		WeaveItem pao_WeaveItem[] = new WeaveItem[7];
		for (int i = 0; i < 7; i++) {
			pao_WeaveItem[i] = new WeaveItem();
		}
		int pao_count = 0;
		boolean b_pao = false;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 4) {
				cards_index[i] = 0;
				pao_WeaveItem[pao_count].center_card = switch_to_card_data(i);
				if ((seat_index == provider_index) || (switch_to_card_index(cur_card) != i))
					pao_WeaveItem[pao_count].weave_kind = GameConstants.WIK_TI_LONG;
				else
					pao_WeaveItem[pao_count].weave_kind = GameConstants.WIK_PAO;
				b_pao = true;
				cbCardCount -= 4;
				pao_count++;

			}
		}

		// 需求判断
		if (cbCardCount == 0)
			return false;
		int cbLessKindItem = (cbCardCount) / 3;
		boolean bNeedCardEye = ((cbCardCount + 1) % 3 == 0);
		if (cbCardCount % 3 == 1)
			return false;

		// 单吊判断
		if ((cbLessKindItem == 0) && (bNeedCardEye == true)) {
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (cards_index[i] == 2) {

					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem.curCardEye = true;
					analyseItem.cbCardEye = cur_card;

					int count = 0;
					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						analyseItem.hu_xi[j] = get_weave_hu_xi(weaveItem[j]);
						// if((cur_card ==
						// weaveItem[j].center_card)&&((weaveItem[j].weave_kind
						// == GameConstants.WIK_PENG)
						// ||(weaveItem[j].weave_kind ==
						// GameConstants.WIK_SAO))){
						// WeaveItem weave_item = new WeaveItem();
						// weave_item.center_card = weaveItem[j].center_card;
						// weave_item.weave_kind = GameConstants.WIK_PAO;
						// analyseItem.cbWeaveKind[j] = weave_item.weave_kind;
						// analyseItem.cbCenterCard[j] = weave_item.center_card;
						// analyseItem.hu_xi[j] = get_weave_hu_xi(weave_item);
						// }
						hu_xi[0] += analyseItem.hu_xi[j];
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
						count++;
					}
					if (b_pao == true) {
						for (int k = 0; k < pao_count; k++) {
							analyseItem.cbWeaveKind[count] = pao_WeaveItem[k].weave_kind;
							analyseItem.cbCenterCard[count] = pao_WeaveItem[k].center_card;
							analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem[k]);
							hu_xi[0] += analyseItem.hu_xi[count];
							get_weave_card(pao_WeaveItem[k].weave_kind, pao_WeaveItem[k].center_card, analyseItem.cbCardData[count]);
							count++;
						}
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;
					// if(hu_xi[0]<15)
					// return false;
					// 插入结果
					analyseItemArray.add(analyseItem);

				}
			}

			return (analyseItemArray.size() > 0 ? true : false);
		} // 单吊判断 end

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[76];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		// 拆分分析
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			int card_date = switch_to_card_data(i);
			if (cards_index[i] == 3) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = i;
				kindItem[cbKindItemCount].cbValidIndex[2] = i;
				if ((cur_card == switch_to_card_data(i)) && (seat_index != provider_index))
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
				else
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
			// 大小搭吃
			if (((cards_index[i] == 2) || ((i == switch_to_card_index(cur_card)) && (cards_index[i] == 3)))
					&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] >= 1) && ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] < 3)
							|| (((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card)))) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = i;
				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbWeaveKind = i >= 10 ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
			// 大小搭吃
			if ((cards_index[i] >= 1)
					&& ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 2)
							|| ((((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card))
									&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 3)))
					&& ((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbWeaveKind = i > 10 ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

				cbKindItemCount++;
			}
			if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x02) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
							&& ((cards_index[i + 5] >= j) && ((cards_index[i + 5] < 3) || ((i + 5) == switch_to_card_index(cur_card))))
							&& ((cards_index[i + 8] >= j) && ((cards_index[i + 8] < 3) || ((i + 8) == switch_to_card_index(cur_card))))) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 5;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 8;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 5;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 8;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (yws_type == true) {
				if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x01) {
					for (int j = 1; j <= cards_index[i]; j++) {
						if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
								&& ((cards_index[i + 4] >= j) && ((cards_index[i + 4] < 3) || ((i + 4) == switch_to_card_index(cur_card))))
								&& ((cards_index[i + 9] >= j) && ((cards_index[i + 9] < 3) || ((i + 9) == switch_to_card_index(cur_card))))) {
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 4;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 9;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i + 4;
							kindItem[cbKindItemCount].cbValidIndex[2] = i + 9;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YWS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}
				}
			}
			// 顺子判断
			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && (cards_index[i] > 0) && ((i % 10) <= 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
							&& (cards_index[i + 1] >= j && (cards_index[i + 1] < 3 || ((i + 1) == switch_to_card_index(cur_card))))
							&& (cards_index[i + 2] >= j && (cards_index[i + 2] < 3 || ((i + 2) == switch_to_card_index(cur_card))))) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析

		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6 };
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}
			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int hh_count = GameConstants.MAX_HH_INDEX;
				// 设置变量
				for (int i = 0; i < hh_count; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {

					int cbCardEye = 0;
					if (bNeedCardEye == true) {
						for (int i = 0; i < hh_count; i++) {
							if (cbCardIndexTemp[i] == 2) {
								cbCardEye = switch_to_card_data(i);// 牌眼
								break;
							}
						}
					}
					hu_xi[0] = 0;
					if ((bNeedCardEye == false) || (cbCardEye != 0)) {
						// 牌眼判断
						AnalyseItem analyseItem = new AnalyseItem();
						// 设置组合
						int count = 0;
						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
							hu_xi[0] += weaveItem[i].hu_xi;
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
							count++;
						}
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							WeaveItem weave_item = new WeaveItem();
							weave_item.weave_kind = pKindItem[i].cbWeaveKind;
							weave_item.center_card = pKindItem[i].cbCenterCard;
							analyseItem.hu_xi[i + cbWeaveCount] = get_weave_hu_xi(weave_item);
							hu_xi[0] += analyseItem.hu_xi[i + cbWeaveCount];
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
							count++;

						}

						if (b_pao == true) {
							for (int k = 0; k < pao_count; k++) {
								analyseItem.cbWeaveKind[count] = pao_WeaveItem[k].weave_kind;
								analyseItem.cbCenterCard[count] = pao_WeaveItem[k].center_card;
								analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem[k]);
								hu_xi[0] += analyseItem.hu_xi[count];
								get_weave_card(pao_WeaveItem[k].weave_kind, pao_WeaveItem[k].center_card, analyseItem.cbCardData[count]);
								count++;
							}
						}
						// if(hu_xi[0]<15)
						// return false;
						// 插入结果

						if (cbCardEye != 0) {
							analyseItem.curCardEye = true;
							analyseItem.cbCardEye = cbCardEye;

						}
						// 插入结果
						analyseItemArray.add(analyseItem);
					}
				}

				// 设置索引
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0)
						break;
				} else
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}

	// 分析扑克
	public boolean analyse_card_wmq(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index, int cur_card,
			List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[], boolean yws_type) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		hu_xi[0] = 0;

		// 需求判断
		if (cbCardCount == 0)
			return false;
		int cbLessKindItem = (cbCardCount) / 3;
		boolean bNeedCardEye = ((cbCardCount + 1) % 3 == 0);
		if (cbCardCount % 3 == 1)
			return false;

		// 单吊判断
		if ((cbLessKindItem == 0) && (bNeedCardEye == true)) {
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (cards_index[i] == 2) {

					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem.curCardEye = true;
					analyseItem.cbCardEye = cur_card;

					int count = 0;
					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						analyseItem.hu_xi[j] = weaveItem[j].hu_xi;
						hu_xi[0] += analyseItem.hu_xi[j];
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
						count++;
					}

					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

				}
			}

			return (analyseItemArray.size() > 0 ? true : false);
		} // 单吊判断 end

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[76];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		// 拆分分析
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			int card_date = switch_to_card_data(i);
			if (cards_index[i] >= 3) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = i;
				kindItem[cbKindItemCount].cbValidIndex[2] = i;
				if ((cur_card == switch_to_card_data(i)) && (seat_index != provider_index))
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
				else
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}

			// 顺子判断
			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && (cards_index[i] > 0) && ((i % 10) <= 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if ((cards_index[i + 1] >= j) && (cards_index[i + 2] >= j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析

		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6 };
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}
			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int hh_count = GameConstants.MAX_HH_INDEX;
				// 设置变量
				for (int i = 0; i < hh_count; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {

					int cbCardEye = 0;
					if (bNeedCardEye == true) {
						for (int i = 0; i < hh_count; i++) {
							if (cbCardIndexTemp[i] == 2) {
								cbCardEye = switch_to_card_data(i);// 牌眼
								break;
							}
						}
					}
					hu_xi[0] = 0;
					if ((bNeedCardEye == false) || (cbCardEye != 0)) {
						// 牌眼判断
						AnalyseItem analyseItem = new AnalyseItem();
						// 设置组合
						int count = 0;
						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
							count++;
						}
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							WeaveItem weave_item = new WeaveItem();
							weave_item.weave_kind = pKindItem[i].cbWeaveKind;
							weave_item.center_card = pKindItem[i].cbCenterCard;
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
							count++;

						}
						// 插入结果

						if (cbCardEye != 0) {
							analyseItem.curCardEye = true;
							analyseItem.cbCardEye = cbCardEye;

						}
						// 插入结果
						analyseItemArray.add(analyseItem);
					}
				}

				// 设置索引
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0)
						break;
				} else
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}
    public int  analyse_weave(int cards_index[],int seat_index,int provider_index,int cur_card,boolean yws_type,int weave_kind[][],int weave_kind_count[],KindItem kindItem[]){
    	int cbKindItemCount = 0;
    	
    	// 拆分分析
    			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
    				if (cards_index[i] == 0)
    					continue;
    				int card_date = switch_to_card_data(i);
    				if (cards_index[i] == 3) {
    					kindItem[cbKindItemCount].cbCardIndex[0] = i;
    					kindItem[cbKindItemCount].cbCardIndex[1] = i;
    					kindItem[cbKindItemCount].cbCardIndex[2] = i;
    					kindItem[cbKindItemCount].cbValidIndex[0] = i;
    					kindItem[cbKindItemCount].cbValidIndex[1] = i;
    					kindItem[cbKindItemCount].cbValidIndex[2] = i;
    					if ((cur_card == switch_to_card_data(i)) && (seat_index != provider_index))
    						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
    					else
    						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
    					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
    					weave_kind[i][weave_kind_count[i]++] = kindItem[cbKindItemCount].cbWeaveKind;
    					cbKindItemCount++;
    				}
    				// 大小搭吃
    				if ((i < 10) && ((cards_index[i] == 2) || ((i == switch_to_card_index(cur_card)) && (cards_index[i] == 3)))
    						&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] >= 1) && ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] < 3)
    								|| (((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card)))) {
    					kindItem[cbKindItemCount].cbCardIndex[0] = i;
    					kindItem[cbKindItemCount].cbCardIndex[1] = i;
    					kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
    					kindItem[cbKindItemCount].cbValidIndex[0] = i;
    					kindItem[cbKindItemCount].cbValidIndex[1] = i;
    					kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
    					kindItem[cbKindItemCount].cbWeaveKind = i >= 10 ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
    					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
    				
    					weave_kind[i][weave_kind_count[i]++] = kindItem[cbKindItemCount].cbWeaveKind;
    					weave_kind[(i + 10) % GameConstants.MAX_HH_INDEX][weave_kind_count[(i + 10) % GameConstants.MAX_HH_INDEX]++] = kindItem[cbKindItemCount].cbWeaveKind;
    					cbKindItemCount++;
    				}
    				// 大小搭吃
    				if ((i < 10) && (cards_index[i] >= 1)
    						&& ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 2)
    								|| ((((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card))
    										&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 3)))
    						&& ((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))) {
    					kindItem[cbKindItemCount].cbCardIndex[0] = i;
    					kindItem[cbKindItemCount].cbCardIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
    					kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
    					kindItem[cbKindItemCount].cbValidIndex[0] = i;
    					kindItem[cbKindItemCount].cbValidIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
    					kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
    					kindItem[cbKindItemCount].cbWeaveKind = i > 10 ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
    					weave_kind[i][weave_kind_count[i]++] = kindItem[cbKindItemCount].cbWeaveKind;
    					weave_kind[(i + 10) % GameConstants.MAX_HH_INDEX][weave_kind_count[(i + 10) % GameConstants.MAX_HH_INDEX]++] = kindItem[cbKindItemCount].cbWeaveKind;
    					
    					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

    					cbKindItemCount++;
    				}
    				if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x02) {
    					for (int j = 1; j <= cards_index[i]; j++) {
    						if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
    								&& ((cards_index[i + 5] >= j) && ((cards_index[i + 5] < 3) || ((i + 5) == switch_to_card_index(cur_card))))
    								&& ((cards_index[i + 8] >= j) && ((cards_index[i + 8] < 3) || ((i + 8) == switch_to_card_index(cur_card))))) {
    							kindItem[cbKindItemCount].cbCardIndex[0] = i;
    							kindItem[cbKindItemCount].cbCardIndex[1] = i + 5;
    							kindItem[cbKindItemCount].cbCardIndex[2] = i + 8;
    							kindItem[cbKindItemCount].cbValidIndex[0] = i;
    							kindItem[cbKindItemCount].cbValidIndex[1] = i + 5;
    							kindItem[cbKindItemCount].cbValidIndex[2] = i + 8;
    							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
    							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
    							for(int k= 0; k < 3; k++)
    							{
    								weave_kind[kindItem[cbKindItemCount].cbCardIndex[k]][weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++] = kindItem[cbKindItemCount].cbWeaveKind;
    							}
    							cbKindItemCount++;
    						}
    					}
    				}
    				if (yws_type == true) {
    					if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x01) {
    						for (int j = 1; j <= cards_index[i]; j++) {
    							if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
    									&& ((cards_index[i + 4] >= j) && ((cards_index[i + 4] < 3) || ((i + 4) == switch_to_card_index(cur_card))))
    									&& ((cards_index[i + 9] >= j) && ((cards_index[i + 9] < 3) || ((i + 9) == switch_to_card_index(cur_card))))) {
    								kindItem[cbKindItemCount].cbCardIndex[0] = i;
    								kindItem[cbKindItemCount].cbCardIndex[1] = i + 4;
    								kindItem[cbKindItemCount].cbCardIndex[2] = i + 9;
    								kindItem[cbKindItemCount].cbValidIndex[0] = i;
    								kindItem[cbKindItemCount].cbValidIndex[1] = i + 4;
    								kindItem[cbKindItemCount].cbValidIndex[2] = i + 9;
    								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YWS;
    								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
    								for(int k= 0; k < 3; k++)
        							{
        								weave_kind[kindItem[cbKindItemCount].cbCardIndex[k]][weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++] = kindItem[cbKindItemCount].cbWeaveKind;
        							}
    								cbKindItemCount++;
    							}
    						}
    					}
    				}
    				// 顺子判断
    				if ((i < (GameConstants.MAX_HH_INDEX - 2)) && (cards_index[i] > 0) && ((i % 10) <= 7)) {
    					for (int j = 1; j <= cards_index[i]; j++) {

    						if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
    								&& (cards_index[i + 1] >= j && (cards_index[i + 1] < 3 || ((i + 1) == switch_to_card_index(cur_card))))
    								&& (cards_index[i + 2] >= j && (cards_index[i + 2] < 3 || ((i + 2) == switch_to_card_index(cur_card))))) {
    							kindItem[cbKindItemCount].cbCardIndex[0] = i;
    							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
    							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
    							kindItem[cbKindItemCount].cbValidIndex[0] = i;
    							kindItem[cbKindItemCount].cbValidIndex[1] = i + 1;
    							kindItem[cbKindItemCount].cbValidIndex[2] = i + 2;
    							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
    							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
    							int kind[] = new int[]{GameConstants.WIK_LEFT,GameConstants.WIK_CENTER,GameConstants.WIK_RIGHT};
    							for(int k= 0; k < 3; k++)
    							{
    								weave_kind[kindItem[cbKindItemCount].cbCardIndex[k]][weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++] = kind[k];
    							}
    							cbKindItemCount++;
    						}
    					}
    				}

    			}
    	
    	return cbKindItemCount;
    	
    }
	// 分析扑克
	public boolean analyse_card_phz(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index, int cur_card,
			List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[], boolean yws_type) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		hu_xi[0] = 0;
		// 跑胡判断
		WeaveItem pao_WeaveItem[] = new WeaveItem[7];
		for (int i = 0; i < 7; i++) {
			pao_WeaveItem[i] = new WeaveItem();
		}
		boolean b_pao = false;
		int pao_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 4) {
				cards_index[i] = 0;
				pao_WeaveItem[pao_count].center_card = switch_to_card_data(i);
				if ((seat_index == provider_index) || (switch_to_card_index(cur_card) != i))
					pao_WeaveItem[pao_count].weave_kind = GameConstants.WIK_TI_LONG;
				else
					pao_WeaveItem[pao_count].weave_kind = GameConstants.WIK_PAO;
				b_pao = true;
				cbCardCount -= 4;
				pao_count++;

			}
		}

		// 需求判断
		if (cbCardCount == 0)
			return false;
		int cbLessKindItem = (cbCardCount) / 3;
		boolean bNeedCardEye = ((cbCardCount + 1) % 3 == 0);
		if (cbCardCount % 3 == 1)
			return false;

		// 单吊判断
		if ((cbLessKindItem == 0) && (bNeedCardEye == true)) {
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (cards_index[i] == 2) {

					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem.curCardEye = true;
					analyseItem.cbCardEye = cur_card;

					int count = 0;
					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						analyseItem.hu_xi[j] = get_weave_hu_xi(weaveItem[j]);
						// if((cur_card ==
						// weaveItem[j].center_card)&&((weaveItem[j].weave_kind
						// == GameConstants.WIK_PENG)
						// ||(weaveItem[j].weave_kind ==
						// GameConstants.WIK_SAO))){
						// WeaveItem weave_item = new WeaveItem();
						// weave_item.center_card = weaveItem[j].center_card;
						// weave_item.weave_kind = GameConstants.WIK_PAO;
						// analyseItem.cbWeaveKind[j] = weave_item.weave_kind;
						// analyseItem.cbCenterCard[j] = weave_item.center_card;
						// analyseItem.hu_xi[j] = get_weave_hu_xi(weave_item);
						// }
						hu_xi[0] += analyseItem.hu_xi[j];
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
						count++;
					}
					if (b_pao == true) {
						for (int k = 0; k < pao_count; k++) {
							analyseItem.cbWeaveKind[count] = pao_WeaveItem[k].weave_kind;
							analyseItem.cbCenterCard[count] = pao_WeaveItem[k].center_card;
							analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem[k]);
							hu_xi[0] += analyseItem.hu_xi[count];
							get_weave_card(pao_WeaveItem[k].weave_kind, pao_WeaveItem[k].center_card, analyseItem.cbCardData[count]);
							count++;
						}
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;
					// if(hu_xi[0]<15)
					// return false;
					// 插入结果
					analyseItemArray.add(analyseItem);

				}
			}

			return (analyseItemArray.size() > 0 ? true : false);
		} // 单吊判断 end

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[76];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		// 拆分分析
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			int card_date = switch_to_card_data(i);
			if (cards_index[i] == 3) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = i;
				kindItem[cbKindItemCount].cbValidIndex[2] = i;
				if ((cur_card == switch_to_card_data(i)) && (seat_index != provider_index))
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
				else
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
			// 大小搭吃
			if ((i < 10) && ((cards_index[i] == 2) || ((i == switch_to_card_index(cur_card)) && (cards_index[i] == 3)))
					&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] >= 1) && ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] < 3)
							|| (((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card)))) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = i;
				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbWeaveKind = i >= 10 ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
			// 大小搭吃
			if ((i < 10) && (cards_index[i] >= 1)
					&& ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 2)
							|| ((((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card))
									&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 3)))
					&& ((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbWeaveKind = i > 10 ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

				cbKindItemCount++;
			}
			if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x02) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
							&& ((cards_index[i + 5] >= j) && ((cards_index[i + 5] < 3) || ((i + 5) == switch_to_card_index(cur_card))))
							&& ((cards_index[i + 8] >= j) && ((cards_index[i + 8] < 3) || ((i + 8) == switch_to_card_index(cur_card))))) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 5;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 8;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 5;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 8;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (yws_type == true) {
				if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x01) {
					for (int j = 1; j <= cards_index[i]; j++) {
						if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
								&& ((cards_index[i + 4] >= j) && ((cards_index[i + 4] < 3) || ((i + 4) == switch_to_card_index(cur_card))))
								&& ((cards_index[i + 9] >= j) && ((cards_index[i + 9] < 3) || ((i + 9) == switch_to_card_index(cur_card))))) {
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 4;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 9;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i + 4;
							kindItem[cbKindItemCount].cbValidIndex[2] = i + 9;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YWS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}
				}
			}
			// 顺子判断
			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && (cards_index[i] > 0) && ((i % 10) <= 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
							&& (cards_index[i + 1] >= j && (cards_index[i + 1] < 3 || ((i + 1) == switch_to_card_index(cur_card))))
							&& (cards_index[i + 2] >= j && (cards_index[i + 2] < 3 || ((i + 2) == switch_to_card_index(cur_card))))) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析

		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6 };
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}
			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int hh_count = GameConstants.MAX_HH_INDEX;
				// 设置变量
				for (int i = 0; i < hh_count; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {

					int cbCardEye = 0;
					if (bNeedCardEye == true) {
						for (int i = 0; i < hh_count; i++) {
							if (cbCardIndexTemp[i] == 2) {
								cbCardEye = switch_to_card_data(i);// 牌眼
								break;
							}
						}
					}
					hu_xi[0] = 0;
					if ((bNeedCardEye == false) || (cbCardEye != 0)) {
						// 牌眼判断
						AnalyseItem analyseItem = new AnalyseItem();
						// 设置组合
						int count = 0;
						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
							hu_xi[0] += weaveItem[i].hu_xi;
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
							count++;
						}
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							WeaveItem weave_item = new WeaveItem();
							weave_item.weave_kind = pKindItem[i].cbWeaveKind;
							weave_item.center_card = pKindItem[i].cbCenterCard;
							analyseItem.hu_xi[i + cbWeaveCount] = get_weave_hu_xi(weave_item);
							hu_xi[0] += analyseItem.hu_xi[i + cbWeaveCount];
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
							count++;

						}

						if (b_pao == true) {
							for (int k = 0; k < pao_count; k++) {
								analyseItem.cbWeaveKind[count] = pao_WeaveItem[k].weave_kind;
								analyseItem.cbCenterCard[count] = pao_WeaveItem[k].center_card;
								analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem[k]);
								hu_xi[0] += analyseItem.hu_xi[count];
								get_weave_card(pao_WeaveItem[k].weave_kind, pao_WeaveItem[k].center_card, analyseItem.cbCardData[count]);
								count++;
							}
						}
						// if(hu_xi[0]<15)
						// return false;
						// 插入结果

						if (cbCardEye != 0) {
							analyseItem.curCardEye = true;
							analyseItem.cbCardEye = cbCardEye;

						}
						// 插入结果
						analyseItemArray.add(analyseItem);
					}
				}

				// 设置索引
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0)
						break;
				} else
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}

	protected int get_magic_count(int[] cards_index) {
		// int count = 0;
		//
		// int[] hand_cards = new int[Constants_YongZhou.MAX_CARD_INDEX];
		// int hand_card_count = this.switch_to_cards_data(cards_index,
		// hand_cards);
		//
		// for (int i = 0; i < hand_card_count; i++) {
		// if (hand_cards[i] == 0x21)
		// count++;
		// }
		//
		// return count;

		return cards_index[Constants_YongZhou.MAX_CARD_INDEX - 1];
	}

	public boolean analyse_card_yzchz(int src_cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index,
			int cur_card, List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[]) {
		int[] cards_index = new int[Constants_YongZhou.MAX_CARD_INDEX];
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++) { // 这里必须要进行替换
			cards_index[i] = src_cards_index[i];
		}
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		hu_xi[0] = 0;
		// 跑胡判断
		WeaveItem pao_WeaveItem = new WeaveItem();
		WeaveItem[] kan_WeaveItems = new WeaveItem[7]; // 对象数组只是些引用，记得初始化，不然就加断言进行判断，null时直接抛出异常

		for (int i = 0; i < 7; i++) {
			kan_WeaveItems[i] = new WeaveItem();
		}

		int kan_count = 0;
		boolean b_pao = false;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 4) {
				cards_index[i] = 0;
				src_cards_index[i] = 0; // 注意这行代码别省
				pao_WeaveItem.center_card = switch_to_card_data(i);
				if ((seat_index == provider_index) || (switch_to_card_index(cur_card) != i))
					pao_WeaveItem.weave_kind = GameConstants.WIK_TI_LONG;
				else
					pao_WeaveItem.weave_kind = GameConstants.WIK_PAO;
				b_pao = true;
				cbCardCount -= 4;

			}
			if (cards_index[i] == 3) { // 将坎剔除掉
				if ((seat_index == provider_index) || (switch_to_card_index(cur_card) != i)) {
					cards_index[i] = 0;
					kan_WeaveItems[kan_count].center_card = switch_to_card_data(i);
					kan_WeaveItems[kan_count].weave_kind = GameConstants.WIK_KAN;
					kan_count++;
					cbCardCount -= 3;
				}
			}
		}

		// 需求判断
		if (cbCardCount == 0)
			return false;
		int cbLessKindItem = (cbCardCount) / 3;
		boolean bNeedCardEye = ((cbCardCount + 1) % 3 == 0);
		if (cbCardCount % 3 == 1)
			return false;

		int magic_count = this.get_magic_count(cards_index);

		// 单吊判断
		if ((cbLessKindItem == 0) && (bNeedCardEye == true)) {
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (cards_index[i] == 2 || magic_count == 2 || (cards_index[i] == 1 && magic_count == 1)) {

					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem.curCardEye = true;

					int count = 0;
					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						analyseItem.hu_xi[j] = get_weave_hu_xi(weaveItem[j]);
						hu_xi[0] += analyseItem.hu_xi[j];
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
						count++;
					}
					if (b_pao == true) {
						analyseItem.cbWeaveKind[count] = pao_WeaveItem.weave_kind;
						analyseItem.cbCenterCard[count] = pao_WeaveItem.center_card;
						analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem);
						hu_xi[0] += analyseItem.hu_xi[count];
						get_weave_card(pao_WeaveItem.weave_kind, pao_WeaveItem.center_card, analyseItem.cbCardData[count]);
						count++;
					}
					for (int j = 0; j < kan_count; j++) {
						analyseItem.cbWeaveKind[count] = kan_WeaveItems[j].weave_kind;
						analyseItem.cbCenterCard[count] = kan_WeaveItems[j].center_card;
						analyseItem.hu_xi[count] = get_weave_hu_xi(kan_WeaveItems[j]);
						hu_xi[0] += analyseItem.hu_xi[count];
						get_weave_card(kan_WeaveItems[j].weave_kind, kan_WeaveItems[j].center_card, analyseItem.cbCardData[count]);
						count++;
					}
					if (magic_count == 2) {
						analyseItem.bMagicEye = true;
						analyseItem.cbCardEye = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
					} else {
						analyseItem.cbCardEye = switch_to_card_data(i);
					}

					if (magic_count == 1) {
						analyseItem.cbCardData[count - 1][0] = switch_to_card_data(i);
						analyseItem.cbCardData[count - 1][1] = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
					} else if (magic_count == 2) {
						analyseItem.cbCardData[count - 1][0] = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
						analyseItem.cbCardData[count - 1][1] = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
					}

					// 插入结果
					analyseItemArray.add(analyseItem);

				}
			}

			return (analyseItemArray.size() > 0 ? true : false);
		} // 单吊判断 end

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[76];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		// 拆分分析
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			if (cards_index[i] + magic_count >= 3) { // 碰、坎
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				kindItem[cbKindItemCount].cbValidIndex[0] = cards_index[i] > 0 ? i : Constants_YZCHZ.MAGIC_CARD_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[1] = cards_index[i] > 1 ? i : Constants_YZCHZ.MAGIC_CARD_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[2] = cards_index[i] > 2 ? i : Constants_YZCHZ.MAGIC_CARD_INDEX;
				if ((cur_card == switch_to_card_data(i)) && (seat_index != provider_index))
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
				else
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
			if (true) { // 大小搭吃
				int tmp_i = (i + 10) % GameConstants.MAX_HH_INDEX;
				int chi_count = cards_index[i] + cards_index[tmp_i] + magic_count;
				if (chi_count >= 3) {
					int cbIndex[] = { (cards_index[i] % 2 == 0) ? (cards_index[i] / 2) : ((cards_index[i] + 1) / 2), cards_index[i] / 2,
							cards_index[tmp_i] };
					int nMagicCountTemp = magic_count;
					int cbValidIndex[] = new int[3];// 实际的牌

					while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
						for (int j = 0; j < cbIndex.length; j++) {
							if (cbIndex[j] > 0) {
								cbIndex[j]--;
								cbValidIndex[j] = (j == 2 ? tmp_i : i);
							} else {
								nMagicCountTemp--;
								cbValidIndex[j] = Constants_YZCHZ.MAGIC_CARD_INDEX;
							}
						}
						if (nMagicCountTemp >= 0) {
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i;
							kindItem[cbKindItemCount].cbCardIndex[2] = tmp_i;
							for (int x = 0; x < 3; x++) {
								kindItem[cbKindItemCount].cbValidIndex[x] = cbValidIndex[x];
							}
							kindItem[cbKindItemCount].cbWeaveKind = i >= 10 ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						} else {
							break;
						}
					}
				}
			}
			if (true) { // 大小搭吃
				int tmp_i = (i + 10) % GameConstants.MAX_HH_INDEX;
				int chi_count = cards_index[i] + cards_index[tmp_i] + magic_count;
				if (chi_count >= 3) {
					int cbIndex[] = { cards_index[i], (cards_index[tmp_i] % 2 == 0) ? (cards_index[tmp_i] / 2) : ((cards_index[tmp_i] + 1) / 2),
							cards_index[tmp_i] / 2 };
					int nMagicCountTemp = magic_count;
					int cbValidIndex[] = new int[3];// 实际的牌

					while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
						for (int j = 0; j < cbIndex.length; j++) {
							if (cbIndex[j] > 0) {
								cbIndex[j]--;
								cbValidIndex[j] = (j == 0 ? i : tmp_i);
							} else {
								nMagicCountTemp--;
								cbValidIndex[j] = Constants_YZCHZ.MAGIC_CARD_INDEX;
							}
						}
						if (nMagicCountTemp >= 0) {
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = tmp_i;
							kindItem[cbKindItemCount].cbCardIndex[2] = tmp_i;
							for (int x = 0; x < 3; x++) {
								kindItem[cbKindItemCount].cbValidIndex[x] = cbValidIndex[x];
							}
							kindItem[cbKindItemCount].cbWeaveKind = tmp_i >= 10 ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(tmp_i);
							cbKindItemCount++;
						} else {
							break;
						}
					}
				}
			}
			if (i == 1 || i == 11) { // 二七十吃
				int chi_count = cards_index[i] + cards_index[i + 5] + cards_index[i + 8] + magic_count;
				if (chi_count >= 3) {
					int cbIndex[] = { cards_index[i], cards_index[i + 5], cards_index[i + 8] };
					int nMagicCountTemp = magic_count;
					int cbValidIndex[] = new int[3];// 实际的牌

					while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
						for (int j = 0; j < cbIndex.length; j++) {
							if (cbIndex[j] > 0) {
								cbIndex[j]--;
								cbValidIndex[j] = i + (j == 0 ? 0 : (j == 1 ? 5 : 8));
							} else {
								nMagicCountTemp--;
								cbValidIndex[j] = Constants_YZCHZ.MAGIC_CARD_INDEX;
							}
						}
						if (nMagicCountTemp >= 0) {
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 5;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 8;
							for (int x = 0; x < 3; x++) {
								kindItem[cbKindItemCount].cbValidIndex[x] = cbValidIndex[x];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						} else {
							break;
						}
					}
				}
			}
			// 顺子判断
			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && ((i % 10) <= 7)) {
				// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
				int chi_count = cards_index[i] + cards_index[i + 1] + cards_index[i + 2] + magic_count;
				if (chi_count >= 3) {
					int cbIndex[] = { cards_index[i], cards_index[i + 1], cards_index[i + 2] };
					int nMagicCountTemp = magic_count;
					int cbValidIndex[] = new int[3];// 实际的牌

					while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
						for (int j = 0; j < cbIndex.length; j++) {
							if (cbIndex[j] > 0) {
								cbIndex[j]--;
								cbValidIndex[j] = i + j;
							} else {
								nMagicCountTemp--;
								cbValidIndex[j] = Constants_YZCHZ.MAGIC_CARD_INDEX;
							}
						}
						if (nMagicCountTemp >= 0) {
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
							for (int x = 0; x < 3; x++) {
								kindItem[cbKindItemCount].cbValidIndex[x] = cbValidIndex[x];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						} else {
							break;
						}
					}
				}
			}
		}
		// 组合分析

		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[Constants_YongZhou.MAX_CARD_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6 };
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}
			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int hh_count = Constants_YongZhou.MAX_CARD_INDEX;
				// 设置变量
				for (int i = 0; i < hh_count; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						if (magic_count > 0) {
							if (cbCardIndexTemp[Constants_YZCHZ.MAGIC_CARD_INDEX] > 0) {
								pKindItem[i / 3].cbValidIndex[i % 3] = Constants_YZCHZ.MAGIC_CARD_INDEX;
								cbCardIndexTemp[Constants_YZCHZ.MAGIC_CARD_INDEX]--;
								break;
							}
						} else {
							bEnoughCard = false;
							break;
						}
					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {

					int cbCardEye = 0;
					if (bNeedCardEye == true) {
						for (int i = 0; i < hh_count; i++) {
							if (cbCardIndexTemp[i] == 2 || magic_count == 2 || (cbCardIndexTemp[i] == 1 && magic_count == 1)) {
								if (magic_count == 2) {
									cbCardEye = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
								} else {
									cbCardEye = switch_to_card_data(i);
								}
								break;
							}
						}
					}
					hu_xi[0] = 0;
					if ((bNeedCardEye == false) || (cbCardEye != 0)) {
						// 牌眼判断
						AnalyseItem analyseItem = new AnalyseItem();
						// 设置组合
						int count = 0;
						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
							hu_xi[0] += weaveItem[i].hu_xi;
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
							count++;
						}
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							WeaveItem weave_item = new WeaveItem();
							weave_item.weave_kind = pKindItem[i].cbWeaveKind;
							weave_item.center_card = pKindItem[i].cbCenterCard;
							analyseItem.hu_xi[i + cbWeaveCount] = get_weave_hu_xi(weave_item);
							hu_xi[0] += analyseItem.hu_xi[i + cbWeaveCount];
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
							count++;

						}

						if (b_pao == true) {
							analyseItem.cbWeaveKind[count] = pao_WeaveItem.weave_kind;
							analyseItem.cbCenterCard[count] = pao_WeaveItem.center_card;
							analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem);
							hu_xi[0] += analyseItem.hu_xi[count];
							get_weave_card(pao_WeaveItem.weave_kind, pao_WeaveItem.center_card, analyseItem.cbCardData[count]);
							count++;
						}

						for (int j = 0; j < kan_count; j++) {
							analyseItem.cbWeaveKind[count] = kan_WeaveItems[j].weave_kind;
							analyseItem.cbCenterCard[count] = kan_WeaveItems[j].center_card;
							analyseItem.hu_xi[count] = get_weave_hu_xi(kan_WeaveItems[j]);
							hu_xi[0] += analyseItem.hu_xi[count];
							get_weave_card(kan_WeaveItems[j].weave_kind, kan_WeaveItems[j].center_card, analyseItem.cbCardData[count]);
							count++;
						}

						if (cbCardEye != 0) {
							analyseItem.curCardEye = true;
							analyseItem.cbCardEye = cbCardEye;

							if (magic_count == 1) {
								analyseItem.cbCardData[count - 1][0] = cbCardEye;
								analyseItem.cbCardData[count - 1][1] = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
							} else if (magic_count == 2) {
								analyseItem.cbCardData[count - 1][0] = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
								analyseItem.cbCardData[count - 1][1] = switch_to_card_data(Constants_YZCHZ.MAGIC_CARD_INDEX);
							}
						}
						// 插入结果
						analyseItemArray.add(analyseItem);
					}
				}

				// 设置索引
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0)
						break;
				} else
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}

	// 分析扑克
	public boolean analyse_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index, int cur_card,
			List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[]) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		hu_xi[0] = 0;
		// 跑胡判断
		WeaveItem pao_WeaveItem[] = new WeaveItem[7];
		for (int i = 0; i < 7; i++) {
			pao_WeaveItem[i] = new WeaveItem();
		}
		boolean b_pao = false;
		int pao_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 4) {
				cards_index[i] = 0;
				pao_WeaveItem[pao_count].center_card = switch_to_card_data(i);
				if ((seat_index == provider_index) || (switch_to_card_index(cur_card) != i))
					pao_WeaveItem[pao_count].weave_kind = GameConstants.WIK_TI_LONG;
				else
					pao_WeaveItem[pao_count].weave_kind = GameConstants.WIK_PAO;
				b_pao = true;
				cbCardCount -= 4;
				pao_count++;

			}
		}

		// 需求判断
		if (cbCardCount == 0)
			return false;
		int cbLessKindItem = (cbCardCount) / 3;
		boolean bNeedCardEye = ((cbCardCount + 1) % 3 == 0);
		if (cbCardCount % 3 == 1)
			return false;
		// 变量定义
		int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
		for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++)
			temp_card_index[i] = cards_index[i];
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[76];
		int weave_kind[][] = new int[GameConstants.MAX_HH_INDEX][8];
		int weave_kind_count[] = new int[GameConstants.MAX_HH_INDEX];
		do{
			for (int i = 0; i < kindItem.length; i++) {
				kindItem[i] = new KindItem();
			}
			for(int i = 0; i<GameConstants.MAX_HH_INDEX;i++)
			{
				Arrays.fill(weave_kind[i], 0);
				weave_kind_count[i] = 0;
			}
			cbKindItemCount = this.analyse_weave(temp_card_index, seat_index, provider_index, cur_card, false, weave_kind, weave_kind_count, kindItem);
			boolean flag = false;
			for(int i = 0; i<GameConstants.MAX_HH_INDEX;i++){
				if(weave_kind_count[i] == 1&&temp_card_index[i]!=2)
				{
					int remove_card[] = new int[3];
					int remove_card_count = this.get_weave_card( weave_kind[i][0],  switch_to_card_data(i) , remove_card);
					if(this.remove_cards_by_index(temp_card_index, remove_card, remove_card_count)==true)
					{
						pao_WeaveItem[pao_count].center_card = switch_to_card_data(i);
						pao_WeaveItem[pao_count].weave_kind = weave_kind[i][0];
						b_pao = true;
						pao_count++;
						cbLessKindItem--;
						flag = true;
					}
				}
			}
			if(flag == false)
				break;
		}while(true);
		// 单吊判断
		if ((cbLessKindItem == 0) && (bNeedCardEye == true)) {
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (temp_card_index[i] == 2) {

					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem.curCardEye = true;
					analyseItem.cbCardEye = cur_card;

					int count = 0;
					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						analyseItem.hu_xi[j] = get_weave_hu_xi(weaveItem[j]);
						// if((cur_card ==
						// weaveItem[j].center_card)&&((weaveItem[j].weave_kind
						// == GameConstants.WIK_PENG)
						// ||(weaveItem[j].weave_kind ==
						// GameConstants.WIK_KAN))){
						// WeaveItem weave_item = new WeaveItem();
						// weave_item.center_card = weaveItem[j].center_card;
						// weave_item.weave_kind = GameConstants.WIK_PAO;
						// analyseItem.cbWeaveKind[j] = weave_item.weave_kind;
						// analyseItem.cbCenterCard[j] = weave_item.center_card;
						// analyseItem.hu_xi[j] = get_weave_hu_xi(weave_item);
						// }
						hu_xi[0] += analyseItem.hu_xi[j];
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
						count++;
					}
					if (b_pao == true) {
						for (int k = 0; k < pao_count; k++) {
							analyseItem.cbWeaveKind[count] = pao_WeaveItem[k].weave_kind;
							analyseItem.cbCenterCard[count] = pao_WeaveItem[k].center_card;
							analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem[k]);
							hu_xi[0] += analyseItem.hu_xi[count];
							get_weave_card(pao_WeaveItem[k].weave_kind, pao_WeaveItem[k].center_card, analyseItem.cbCardData[count]);
							count++;
						}
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (temp_card_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

				}
			}
		
			return (analyseItemArray.size() > 0 ? true : false);
		} // 单吊判断 end
		if(this.get_card_count_by_index(temp_card_index)==0&&cbCardCount % 3==0)
		{
			int count = 0;
			// 设置结果
			AnalyseItem analyseItem = new AnalyseItem();
			for (int j = 0; j < cbWeaveCount; j++) {
				analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
				analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
				analyseItem.hu_xi[j] = get_weave_hu_xi(weaveItem[j]);
				// if((cur_card ==
				// weaveItem[j].center_card)&&((weaveItem[j].weave_kind
				// == GameConstants.WIK_PENG)
				// ||(weaveItem[j].weave_kind ==
				// GameConstants.WIK_KAN))){
				// WeaveItem weave_item = new WeaveItem();
				// weave_item.center_card = weaveItem[j].center_card;
				// weave_item.weave_kind = GameConstants.WIK_PAO;
				// analyseItem.cbWeaveKind[j] = weave_item.weave_kind;
				// analyseItem.cbCenterCard[j] = weave_item.center_card;
				// analyseItem.hu_xi[j] = get_weave_hu_xi(weave_item);
				// }
				hu_xi[0] += analyseItem.hu_xi[j];
				get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
				count++;
			}
			if (b_pao == true) {
				for (int k = 0; k < pao_count; k++) {
					analyseItem.cbWeaveKind[count] = pao_WeaveItem[k].weave_kind;
					analyseItem.cbCenterCard[count] = pao_WeaveItem[k].center_card;
					analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem[k]);
					hu_xi[0] += analyseItem.hu_xi[count];
					get_weave_card(pao_WeaveItem[k].weave_kind, pao_WeaveItem[k].center_card, analyseItem.cbCardData[count]);
					count++;
				}
			}
			// 插入结果
			analyseItemArray.add(analyseItem);
			return (analyseItemArray.size() > 0 ? true : false);
		}
	
		
		
//		// 拆分分析
//		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
//			if (cards_index[i] == 0)
//				continue;
//			int card_date = switch_to_card_data(i);
//			if (cards_index[i] == 3) {
//				kindItem[cbKindItemCount].cbCardIndex[0] = i;
//				kindItem[cbKindItemCount].cbCardIndex[1] = i;
//				kindItem[cbKindItemCount].cbCardIndex[2] = i;
//				kindItem[cbKindItemCount].cbValidIndex[0] = i;
//				kindItem[cbKindItemCount].cbValidIndex[1] = i;
//				kindItem[cbKindItemCount].cbValidIndex[2] = i;
//				if ((cur_card == switch_to_card_data(i)) && (seat_index != provider_index))
//					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
//				else
//					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
//				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
//				cbKindItemCount++;
//			}
//			// 大小搭吃
//			if ((i < 10) && ((cards_index[i] == 2) || ((i == switch_to_card_index(cur_card)) && (cards_index[i] == 3)))
//					&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] >= 1) && ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] < 3)
//							|| (((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card)))) {
//				kindItem[cbKindItemCount].cbCardIndex[0] = i;
//				kindItem[cbKindItemCount].cbCardIndex[1] = i;
//				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
//				kindItem[cbKindItemCount].cbValidIndex[0] = i;
//				kindItem[cbKindItemCount].cbValidIndex[1] = i;
//				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
//				kindItem[cbKindItemCount].cbWeaveKind = i >= 10 ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
//				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
//				cbKindItemCount++;
//			}
//			// 大小搭吃
//			if ((i < 10) && (cards_index[i] >= 1)
//					&& ((cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 2)
//							|| ((((i + 10) % GameConstants.MAX_HH_INDEX) == switch_to_card_index(cur_card))
//									&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 3)))
//					&& ((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))) {
//				kindItem[cbKindItemCount].cbCardIndex[0] = i;
//				kindItem[cbKindItemCount].cbCardIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
//				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
//				kindItem[cbKindItemCount].cbValidIndex[0] = i;
//				kindItem[cbKindItemCount].cbValidIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
//				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
//				kindItem[cbKindItemCount].cbWeaveKind = i > 10 ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
//				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
//
//				cbKindItemCount++;
//			}
//			if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x02) {
//				for (int j = 1; j <= cards_index[i]; j++) {
//					if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
//							&& ((cards_index[i + 5] >= j) && ((cards_index[i + 5] < 3) || ((i + 5) == switch_to_card_index(cur_card))))
//							&& ((cards_index[i + 8] >= j) && ((cards_index[i + 8] < 3) || ((i + 8) == switch_to_card_index(cur_card))))) {
//						kindItem[cbKindItemCount].cbCardIndex[0] = i;
//						kindItem[cbKindItemCount].cbCardIndex[1] = i + 5;
//						kindItem[cbKindItemCount].cbCardIndex[2] = i + 8;
//						kindItem[cbKindItemCount].cbValidIndex[0] = i;
//						kindItem[cbKindItemCount].cbValidIndex[1] = i + 5;
//						kindItem[cbKindItemCount].cbValidIndex[2] = i + 8;
//						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
//						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
//						cbKindItemCount++;
//					}
//				}
//			}
//			// 顺子判断
//			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && (cards_index[i] > 0) && ((i % 10) <= 7)) {
//				for (int j = 1; j <= cards_index[i]; j++) {
//
//					if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
//							&& (cards_index[i + 1] >= j && (cards_index[i + 1] < 3 || ((i + 1) == switch_to_card_index(cur_card))))
//							&& (cards_index[i + 2] >= j && (cards_index[i + 2] < 3 || ((i + 2) == switch_to_card_index(cur_card))))) {
//						kindItem[cbKindItemCount].cbCardIndex[0] = i;
//						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
//						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
//						kindItem[cbKindItemCount].cbValidIndex[0] = i;
//						kindItem[cbKindItemCount].cbValidIndex[1] = i + 1;
//						kindItem[cbKindItemCount].cbValidIndex[2] = i + 2;
//						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
//						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
//						cbKindItemCount++;
//					}
//				}
//			}
//
//		}
		// 组合分析

		if (cbKindItemCount >= cbLessKindItem&&cbLessKindItem>=1&&cbKindItemCount>=1) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6 };
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}
			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int hh_count = GameConstants.MAX_HH_INDEX;
				// 设置变量
				for (int i = 0; i < hh_count; i++) {
					cbCardIndexTemp[i] = temp_card_index[i];
				}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {

					int cbCardEye = 0;
					if (bNeedCardEye == true) {
						for (int i = 0; i < hh_count; i++) {
							if (cbCardIndexTemp[i] == 2) {
								cbCardEye = switch_to_card_data(i);// 牌眼
								break;
							}
						}
					}
					hu_xi[0] = 0;
					if ((bNeedCardEye == false) || (cbCardEye != 0)) {
						// 牌眼判断
						AnalyseItem analyseItem = new AnalyseItem();
						// 设置组合
						int count = 0;
						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
							hu_xi[0] += weaveItem[i].hu_xi;
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
							count++;
						}
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							WeaveItem weave_item = new WeaveItem();
							weave_item.weave_kind = pKindItem[i].cbWeaveKind;
							weave_item.center_card = pKindItem[i].cbCenterCard;
							analyseItem.hu_xi[i + cbWeaveCount] = get_weave_hu_xi(weave_item);
							hu_xi[0] += analyseItem.hu_xi[i + cbWeaveCount];
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
							count++;

						}

						if (b_pao == true) {
							for (int k = 0; k < pao_count; k++) {
								analyseItem.cbWeaveKind[count] = pao_WeaveItem[k].weave_kind;
								analyseItem.cbCenterCard[count] = pao_WeaveItem[k].center_card;
								analyseItem.hu_xi[count] = get_weave_hu_xi(pao_WeaveItem[k]);
								hu_xi[0] += analyseItem.hu_xi[count];
								get_weave_card(pao_WeaveItem[k].weave_kind, pao_WeaveItem[k].center_card, analyseItem.cbCardData[count]);
								count++;
							}
						}
						// if(hu_xi[0]<15)
						// return false;
						// 插入结果

						if (cbCardEye != 0) {
							analyseItem.curCardEye = true;
							analyseItem.cbCardEye = cbCardEye;

						}
						// 插入结果
						analyseItemArray.add(analyseItem);
					}
				}

				// 设置索引
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0)
						break;
				} else
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}
		// 跑等级
		if (player_action == GameConstants.WIK_PAO) {
			return 40;
		}
		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 地胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}

		// 补张牌等级
		if (player_action == GameConstants.WIK_BU_ZHNAG) {
			return 30;
		}

		// 招牌等级
		if (player_action == GameConstants.WIK_ZHAO) {
			return 30;
		}

		// 笑
		if (player_action == GameConstants.WIK_MENG_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_DIAN_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_HUI_TOU_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_XIAO_CHAO_TIAN) {
			return 30;
		}
		if (player_action == GameConstants.WIK_DA_CHAO_TIAN) {
			return 30;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT
				|| player_action == GameConstants.WIK_XXD || player_action == GameConstants.WIK_DDX || player_action == GameConstants.WIK_EQS
				|| player_action == GameConstants.WIK_YWS) {
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

	public int get_chi_hu_action_rank_hh(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu = 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu = 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu = 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HEI)).is_empty()) {
			wFanShu = 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu = 5;
		}

		return wFanShu;
	}

	public int get_chi_hu_ying_xi_dzb_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 8;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_dzb_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 30 + 30 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()) {
			hu_xi += ying_hu_count * 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()) {
			hu_xi += chun_ying_count * 150;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			hu_xi += 200;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()) {
			hu_xi += 40;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()) {
			hu_xi += 80;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 120;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()) {
			hu_xi += 450;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()) {
			hu_xi += 300;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()) {
			hu_xi += 200;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}

		return hu_xi;
	}

	public int get_chi_hu_ying_xi_xzb_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 8;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_xzb_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 30 + 30 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()) {
			hu_xi += ying_hu_count * 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()) {
			hu_xi += chun_ying_count * 150;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			hu_xi += 200;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()) {
			hu_xi += 40;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()) {
			hu_xi += 80;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 120;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()) {
			hu_xi += 450;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()) {
			hu_xi += 300;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()) {
			hu_xi += 200;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LDH_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		return hu_xi;
	}

	public int get_chi_hu_ying_xi_qmt_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 8;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_qmt_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 30 + 30 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()) {
			hu_xi += ying_hu_count * 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()) {
			hu_xi += chun_ying_count * 150;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			hu_xi += 200;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()) {
			hu_xi += 40;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()) {
			hu_xi += 80;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 120;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()) {
			hu_xi += 450;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()) {
			hu_xi += 300;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()) {
			hu_xi += 200;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LDH_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIU_DUI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SBD_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BIAN_KAN_HU)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BKB_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_HU_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHA_DAN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_FBW_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		return hu_xi;
	}

	public int get_chi_hu_ying_xi_lmt_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			fanshu += 6;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 3;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_lmt_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 10 + 10 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		return hu_xi;
	}

	public int get_chi_hu_action_rank_dhd_chd(int seat_index, int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 3 + (hong_pai_count - 10);
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_dhd_two_chd(int seat_index, int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 2 + (hong_pai_count - 10);
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 5;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_phz_chd(int seat_index, int da_pai_count, int xiao_pai_count, int tuan_yuan_count, int huang_zhang_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 3 + (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_THIRTEEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 8;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TING_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_HU_CHD)).is_empty()) {
			wFanShu += 8 + (da_pai_count - 18);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_HU_CHD)).is_empty()) {
			wFanShu += 10 + (xiao_pai_count - 16);
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHUA_HOU_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TUAN_CHD)).is_empty()) {
			wFanShu += 8 * tuan_yuan_count;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_lIU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_CHD)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TUAN_YUAN_CHD)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MTH_DA_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MTH_XIAO_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_FAN_TIAN_CHD)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_DENG_CHD)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_GAI_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_QI_CHD)).is_empty()) {
			wFanShu += 3;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_phz_lba(int seat_index, int da_pai_count, int xiao_pai_count, int tuan_yuan_count, int huang_zhang_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 3 + (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 5;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_HU_CHD)).is_empty()) {
			wFanShu += 6 + (da_pai_count - 18);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_HU_CHD)).is_empty()) {
			wFanShu += 8 + (xiao_pai_count - 16);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TUAN_CHD)).is_empty()) {
			wFanShu += 8 * tuan_yuan_count;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_lIU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_GAI_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_QI_CHD)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_PIAO_CHD)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHUANG_PIAO_CHD)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_CHD)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHUN_CHD)).is_empty()) {
			wFanShu += 8;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	// 获取组合
	public int get_weave_card(int cbWeaveKind, int cbCenterCard, int cbCardBuffer[]) {
		// 组合扑克
		switch (cbWeaveKind) {
		case GameConstants.WIK_LEFT: // 左吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard + 1;
			cbCardBuffer[2] = cbCenterCard + 2;

			return 3;
		}
		case GameConstants.WIK_RIGHT: // 右吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard - 2;

			return 3;
		}
		case GameConstants.WIK_CENTER: // 中吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard + 1;

			return 3;
		}
		case GameConstants.WIK_XXD:// 吃小
		{
			// 设置变量
			if (cbCenterCard > 16)
				cbCenterCard = cbCenterCard - 16;
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard + 16;

			return 3;
		}
		case GameConstants.WIK_DDX:// 吃大
		{
			// 设置变量
			if (cbCenterCard < 16)
				cbCenterCard = cbCenterCard + 16;
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard - 16;

			return 3;
		}
		case GameConstants.WIK_EQS:// 吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch (cur_card_value) {
			case 2: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard + 5;
				cbCardBuffer[2] = cbCenterCard + 8;
				break;
			}
			case 7: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 5;
				cbCardBuffer[2] = cbCenterCard + 3;
				break;
			}
			case 10: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 8;
				cbCardBuffer[2] = cbCenterCard - 3;
				break;
			}

			}
			return 3;
		}
		case GameConstants.WIK_YWS:// 吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch (cur_card_value) {
			case 1: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard + 4;
				cbCardBuffer[2] = cbCenterCard + 9;
				break;
			}
			case 5: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 4;
				cbCardBuffer[2] = cbCenterCard + 5;
				break;
			}
			case 10: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 9;
				cbCardBuffer[2] = cbCenterCard - 5;
				break;
			}

			}
			return 3;
		}
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_SAO: // 扫牌操作
		case GameConstants.WIK_PENG: // 碰牌操作
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI: {
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case GameConstants.WIK_PAO:
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		case GameConstants.WIK_MENG_XIAO: // 杠牌操作
		case GameConstants.WIK_DIAN_XIAO:
		case GameConstants.WIK_HUI_TOU_XIAO: {
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		default: {
			// logger.error("get_weave_card:invalid cbWeaveKind" + cbWeaveKind);
		}
		}

		return 0;
	}

	public boolean is_zha_dan_tian_hu(int cards_index[], int card_count) {
		for (int i = 0; i < card_count; i++) {
			if (cards_index[i] != 4)
				continue;
			if (color_hei(this.switch_to_card_data(i)) == true)
				continue;
			for (int j = 0; j < 5; ++j) {
				if (j == 2)
					continue;
				if (j + i - 2 < 0 || j + i - 2 > 19)
					continue;

				if (0 != cards_index[j + i - 2])
					return false;
			}
			return true;
		}
		return false;
	}

	public boolean is_not_ten_xi(int cards_index[], int card_count) {
		tagAnalyseResult anaylse_card = new tagAnalyseResult();
		tagAnalyseResult anaylse_hong_card = new tagAnalyseResult();
		tagAnalyseResult anaylse_heicard = new tagAnalyseResult();
		for (int i = 0; i < card_count; i++) {
			if (cards_index[i] == 2) {
				anaylse_card.cbDoubleCardData[anaylse_card.cbDoubleCount++] = i;
				anaylse_card.cbMulCardData[anaylse_card.cbMulCount++] = i;
				if (color_hei(this.switch_to_card_data(i)) == true) {
					anaylse_heicard.cbDoubleCardData[anaylse_heicard.cbDoubleCount++] = i;
					anaylse_heicard.cbMulCardData[anaylse_heicard.cbMulCount++] = i;
				} else {
					anaylse_hong_card.cbDoubleCardData[anaylse_hong_card.cbDoubleCount++] = i;
					anaylse_hong_card.cbMulCardData[anaylse_hong_card.cbMulCount++] = i;
				}
			}
			if (cards_index[i] == 3) {
				anaylse_card.cbThreeCardData[anaylse_card.cbThreeCount++] = i;
				anaylse_card.cbMulCardData[anaylse_card.cbMulCount++] = i;
				if (color_hei(this.switch_to_card_data(i)) == true) {
					anaylse_heicard.cbThreeCardData[anaylse_heicard.cbThreeCount++] = i;
					anaylse_heicard.cbMulCardData[anaylse_heicard.cbMulCount++] = i;
				} else {
					anaylse_hong_card.cbThreeCardData[anaylse_hong_card.cbThreeCount++] = i;
					anaylse_hong_card.cbMulCardData[anaylse_hong_card.cbMulCount++] = i;
				}
			}
			if (cards_index[i] == 4) {
				anaylse_card.cbFourCardData[anaylse_card.cbFourCount++] = i;
				anaylse_card.cbMulCardData[anaylse_card.cbMulCount++] = i;
				if (color_hei(this.switch_to_card_data(i)) == true) {
					anaylse_heicard.cbFourCardData[anaylse_heicard.cbFourCount++] = i;
					anaylse_heicard.cbMulCardData[anaylse_heicard.cbMulCount++] = i;
				} else {
					anaylse_hong_card.cbFourCardData[anaylse_hong_card.cbFourCount++] = i;
					anaylse_hong_card.cbMulCardData[anaylse_hong_card.cbMulCount++] = i;
				}
			}
		}

		if (anaylse_card.cbMulCount == 0) {
			return true;
		}
		switch (anaylse_card.cbMulCount) {
		case 1: {
			// 只有一个黑组合，绝对是无息
			if (anaylse_heicard.cbMulCount == 1) {
				return true;
			}
			if (this.check_chi_wmq(cards_index, this.switch_to_card_data(anaylse_hong_card.cbMulCardData[0])) == GameConstants.WIK_NULL) {
				return true;
			}
			if (this.check_chi_wmq(cards_index, this.switch_to_card_data(anaylse_hong_card.cbMulCardData[0])) != GameConstants.WIK_NULL) {
				return false;
			}
			if (anaylse_card.cbDoubleCount < 1) {
				for (int i = 0; i < 5; ++i) {
					if (i == 2)
						continue;
					if (anaylse_card.cbMulCardData[0] + i - 2 < 0 || anaylse_card.cbMulCardData[0] + i - 2 > 19)
						continue;

					if (0 != cards_index[anaylse_card.cbMulCardData[0] + i - 2])
						return false;
				}
			}
			return true;
		}
		case 2: {
			// 一个红对子，一个黑对子， 19-4 = 15 一个还有一个红字， 就可以 凑成红字 可以 红转弯，所以一定是有10息的 3+4+3
			// = 10
			if (anaylse_heicard.cbMulCount == 2) {
				int tuan_yuan = 0;
				for (int i = 0; i < anaylse_heicard.cbMulCount; i++) {
					if (cards_index[anaylse_heicard.cbMulCardData[i]] == 4) {
						tuan_yuan++;
					} else if (cards_index[anaylse_heicard.cbMulCardData[i]] == 3) {
						for (int j = 0; j < 5; ++j) {
							if (j == 2)
								continue;
							if (anaylse_heicard.cbMulCardData[0] + j - 2 < 0 || anaylse_heicard.cbMulCardData[0] + j - 2 > 19)
								continue;

							if (0 != cards_index[anaylse_heicard.cbMulCardData[0] + j - 2]) {
								tuan_yuan++;
								break;
							}
						}
					} else if (check_chi_wmq(cards_index, this.switch_to_card_data(anaylse_heicard.cbMulCardData[i])) != GameConstants.WIK_NULL) {
						tuan_yuan++;
					}
				}
				// 只要 有一个团员就一定是 满足10息 5+3+3 并且如果 不能团员 ，3（偎）+3（偎）+3（加红字转弯） = 9
				// ，是一定没有10息的
				if (tuan_yuan >= 1) {
					return false;

				}
				return true;

			}
		}
		}
		return false;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 2);
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants.MAX_HH_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = color * 10 + value - 1;
		return index;
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index_yyzhz(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants_YYZHZ.MAX_YYZHZ_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = color * 10 + value - 1;
		return index;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data(int card_index) {
		if (card_index >= GameConstants.MAX_HH_INDEX) {
			return GameConstants.MAX_HH_INDEX;
		}
		return ((card_index / 10) << 4) | (card_index % 10 + 1);
	}

	// 扑克转换
	public int switch_to_cards_index(int cards_data[], int start_index, int card_count, int cards_index[]) {
		// 设置变量
		// 转换扑克
		for (int i = 0; i < card_count; i++) {
			cards_index[switch_to_card_index(cards_data[start_index + i])]++;
		}
		return card_count;
	}

	public int switch_to_cards_data_yzchz(int cards_index[], int cards_data[]) {
		int cbPosition = 0;
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = switch_to_card_data_yyzhz(i);
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
	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int m = 0; m < this._magic_card_count; m++) {
			for (int i = 0; i < cards_index[this._magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(this._magic_card_index[m]);
			}
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = switch_to_card_data_yyzhz(i);
				}
			}
		}
		return cbPosition;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据(王牌)
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data_yyzhz(int card_index) {
		if (card_index >= GameConstants_YYZHZ.MAX_YYZHZ_INDEX) {
			return GameConstants_YYZHZ.MAX_YYZHZ_INDEX;
		}
		return ((card_index / 10) << 4) | (card_index % 10 + 1);
	}

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据(王牌)
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data_lai(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int m = 0; m < this._magic_card_count; m++) {
			for (int i = 0; i < cards_index[this._magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data_yyzhz(this._magic_card_index[m]);
			}
		}
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = switch_to_card_data_yyzhz(i);
				}
			}
		}
		return cbPosition;
	}

	// 排序,根据牌值排序
	public boolean sort_card_list(int card_data[]) {
		int cbCardCount = card_data.length;
		// 数目过虑
		if (cbCardCount == 0 || cbCardCount > GameConstants.MAX_HH_COUNT)
			return false;

		// 排序操作
		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if (card_data[i] > card_data[i + 1]) {
					// 设置标志
					bSorted = false;

					// 扑克数据
					cbSwitchData = card_data[i];
					card_data[i] = card_data[i + 1];
					card_data[i + 1] = cbSwitchData;
				}
			}
			cbLast--;
		} while (bSorted == false);

		return true;
	}

	public static void main(String[] args) {
		// 插入扑克
		CHDPHZGameLogic logic = new CHDPHZGameLogic();
		int index = logic.switch_to_card_index(24);

		for (int i = 0; i < GameConstants.CARD_DATA_FLS_LX.length; i++)
			System.out.println(GameConstants.CARD_DATA_FLS_LX[i]);
	}

	public static class AnalyseItem {
		public int cbCardEye;//// 牌眼扑克
		public boolean bMagicEye;// 牌眼是否是王霸
		public int cbWeaveKind[] = new int[7];// 组合类型
		public int cbCenterCard[] = new int[7];// 中心扑克
		public int cbCardData[][] = new int[7][4]; // 实际扑克
		public int hu_xi[] = new int[7];// 计算胡息

		public int cbPoint;// 组合牌的最佳点数;

		public boolean curCardEye;// 当前摸的牌是否是牌眼
		public boolean isShuangDui;// 牌眼 true双对--判断碰碰胡
		public int eyeKind;// 牌眼 组合类型
		public int eyeCenterCard;// 牌眼 中心扑克
		public int cbHuXiCount; // 胡息
	}

	// 分析扑克
	public boolean analyse_card_yyzhz(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray, int seat_index,
			int provider_index, int cur_card) {
		// 计算数目
		int cbCardCount = get_card_count_by_index_yyzhz(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;
		int count = GameConstants_YYZHZ.MAX_YYZHZ_INDEX;

		// 单吊判断
		if (cbLessKindItem == 0) {
			for (int i = 0; i < count; i++) {
				if ((cards_index[i] == 2) || (magic_count(cards_index) + cards_index[i] == 2)) { // 如果牌为2张或者癞子牌加上当前牌为2张
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}

					if (magic_count(cards_index) == 2) { // 如果是两张癞子
						analyseItem.cbCardEye = switch_to_card_data_yyzhz(this.get_magic_card_index(0));
						analyseItem.bMagicEye = true;
					} else if (is_magic_index(i)) { // 如果是癞子索引
						continue;
					} else { // 如果是两张普通牌或者是一张癞子加普通牌并且牌眼是普通牌
						analyseItem.cbCardEye = switch_to_card_data_yyzhz(i);
					}

					// 插入结果
					analyseItemArray.add(analyseItem);
					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[count];
		for (int i = 0; i < count; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[76];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			for (int i = 0; i < count; i++) {
				if (cards_index[i] == 0)
					continue;
				int card_date = switch_to_card_data_yyzhz(i);

				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					if ((cur_card == switch_to_card_data_yyzhz(i)) && (seat_index != provider_index)) {
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					} else {
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
					}
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data_yyzhz(i);
					kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 0 ? i : get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[1] = cbMagicCardIndex[i] > 1 ? i : get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[2] = cbMagicCardIndex[i] > 2 ? i : get_magic_card_index(0);
					cbKindItemCount++;
					if (cbMagicCardIndex[i] + cbMagicCardCount >= 6)// 再加一个
					{
						if (cbKindItemCount >= kindItem.length) {
							return false;
						}
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i;
						kindItem[cbKindItemCount].cbCardIndex[2] = i;
						if ((cur_card == switch_to_card_data_yyzhz(i)) && (seat_index != provider_index)) {
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						} else {
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_KAN;
						}
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data_yyzhz(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 二七十判断
				if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x02) {
					for (int j = 1; j <= cards_index[i]; j++) {
						if (((cards_index[i] < 3) || (i == switch_to_card_index(cur_card)))
								&& ((cards_index[i + 5] >= j) && ((cards_index[i + 5] < 3) || ((i + 5) == switch_to_card_index(cur_card))))
								&& ((cards_index[i + 8] >= j) && ((cards_index[i + 8] < 3) || ((i + 8) == switch_to_card_index(cur_card))))) {
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 5;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 8;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i + 5;
							kindItem[cbKindItemCount].cbValidIndex[2] = i + 8;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data_yyzhz(i);
							cbKindItemCount++;
						}
					}
				}

				// 连牌判断
				if ((i < (count - 2)) && ((i % 10) < 7)) {
					// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
					int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] + cbMagicCardIndex[i + 2];
					chi_count += cbMagicCardCount;
					if (chi_count >= 3) {
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i], this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1],
								this.is_magic_index(i + 2) ? 0 : cbMagicCardIndex[i + 2] };

						int nMagicCountTemp = cbMagicCardCount;

						int cbValidIndex[] = new int[3];// 实际的牌
						while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
							for (int j = 0; j < cbIndex.length; j++) {
								if (cbIndex[j] > 0) {
									cbIndex[j]--;
									cbValidIndex[j] = i + j;
								} else {
									nMagicCountTemp--;
									cbValidIndex[j] = get_magic_card_index(0);
								}
							}
							if (nMagicCountTemp >= 0) {
								if (cbKindItemCount >= kindItem.length) {
									return false;
								}
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
								kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data_yyzhz(i);
								for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
									kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
								}
								cbKindItemCount++;
							} else {
								break;
							}
						}
					}
				} // 连牌判断 end
			}
		} // if (cbCardCount>=3) end

		// 组合分析
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[count];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				// 设置变量
				for (int i = 0; i < count; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];
					if (cbCardIndexTemp[cbCardIndex] == 0) {
						if (this.magic_count(cbCardIndexTemp) > 0) {
							for (int m = 0; m < this._magic_card_count; m++) {
								if (cbCardIndexTemp[this._magic_card_index[m]] > 0) {
									pKindItem[i / 3].cbValidIndex[i % 3] = this._magic_card_index[m];
									cbCardIndexTemp[this._magic_card_index[m]]--;
									break;
								}
							}
						} else {
							bEnoughCard = false;
							break;
						}

					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {
					// 牌眼判断
					int cbCardEye = 0;
					boolean bMagicEye = false;
					for (int i = 0; i < count; i++) { // 只有将牌是癞子时，bMagicEye才为真，其他情况都已非癞子牌做牌眼
						if (this.magic_count(cbCardIndexTemp) == 2) { // 如果将牌是一对癞子牌
							cbCardEye = switch_to_card_data_yyzhz(this.get_magic_card_index(0));
							bMagicEye = true;
							break;
						} else if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data_yyzhz(i);// 牌眼
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							// 如果是一个癞子牌和一个其他牌，就不能将癞子作为牌眼，不然将牌判断会判断不了
							// 因为索引的先后问题，如果癞子牌索引靠前，牌眼就是不正确的
							if (is_magic_index(i)) {
								continue;
							} else {
								cbCardEye = switch_to_card_data_yyzhz(i);
								break; // 这里不break跳出循环的话，会导致将牌判断出问题
							}
						}
					}

					// 组合类型
					if (cbCardEye != 0) {
						// 变量定义
						AnalyseItem analyseItem = new AnalyseItem();

						// 设置组合
						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
						}

						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data_yyzhz(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data_yyzhz(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data_yyzhz(pKindItem[i].cbValidIndex[2]);
						}

						// 设置牌眼
						analyseItem.cbCardEye = cbCardEye;
						analyseItem.bMagicEye = bMagicEye;

						// 插入结果
						analyseItemArray.add(analyseItem);
					}
				}

				// 设置索引
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0)
						break;
				} else
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}
		return (analyseItemArray.size() > 0 ? true : false);
	}

	/**
	 * 岳阳捉红字板板胡判断
	 * 
	 * @param card_index
	 * @return
	 */
	public boolean checkBanBanHU(int card_index[]) {
		int hei_pai_count = 0;
		boolean flag = false;
		for (int j = 0; j < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; j++) {
			if (color_hei_yyzhz(switch_to_card_data(j))) {
				hei_pai_count += card_index[j];
			}
		}

		if (hei_pai_count == 14) {
			flag = true;
		}
		return flag;
	}

	public int is_qi_xiao_dui_yyzhz(int cards_index[]) {
		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 计算单牌
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (this._magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == get_magic_card_index(m))
						continue;

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (this._magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cards_index[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		return GameConstants.CHR_HENAN_QI_XIAO_DUI;

	}

	/**
	 * 一挂匾判断
	 * 
	 * @param card_index
	 * @return
	 */
	public boolean check_yi_gua_bian(int card_index[]) {
		boolean flag = false;
		int card_type[] = new int[] { 0, 0, 0 };
		int card_count = 0;
		int mac_card = 0;
		for (int i = 0; i < card_index.length; i++) {
			if (card_index[i] == 0) {
				continue;
			}
			if (is_magic_card(switch_to_card_data(i))) {
				mac_card++;
				continue;
			}
			card_count += card_index[i];

			if (card_count > 2) {
				return false;
			}
			// mac_card[car_count] =
		}

		return flag;
	}

}
