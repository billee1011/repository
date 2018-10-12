/**
 * 
 */
package com.cai.game.schcpdss;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;

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
	int cbMulCardData[] = new int[GameConstants.MAX_GXZP_COUNT];
	int cbFourCardData[] = new int[GameConstants.MAX_GXZP_COUNT]; // 四张扑克
	int cbThreeCardData[] = new int[GameConstants.MAX_GXZP_COUNT]; // 三张扑克
	int cbDoubleCardData[] = new int[GameConstants.MAX_GXZP_COUNT]; // 两张扑克
	int cbSingleCardData[] = new int[GameConstants.MAX_GXZP_COUNT]; // 单张扑克

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

enum ECPType {
	CP_20, CP_12, CP_04, CP_13, CP_05, CP_50, CP_06, CP_15, CP_42, CP_43, CP_07, CP_16, CP_D8, CP_F8, CP_80, CP_45, CP_09, CP_0A, CP_46, CP_0B, CP_66
}
//// 胡牌信息
// class HuCardInfo{
// public int card_eye;
// public int hu_xi_count;
// public int weave_count;
// public WeaveItem weave_item[] = new WeaveItem[10];
// }

public class SCHCPDSSGameLogic {

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	public SCHCPDSSGameLogic() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_GXZP_COUNT];
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

	public int get_card_value_cp(int card) {
		if (0xd == ((card & GameConstants.LOGIC_MASK_COLOR) >> 4) || 0xf == ((card & GameConstants.LOGIC_MASK_COLOR) >> 4)) {
			return 8;
		}
		int value_1 = card & GameConstants.LOGIC_MASK_VALUE;
		int value_2 = (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
		return value_1 + value_2;
	}

	public int[] sortCard(int[] cards, int cards_count) {
		int[] array = Arrays.copyOf(cards, cards_count);
		int temp = 0;
		for (int i = 0; i < array.length; i++) {// 趟数
			for (int j = 0; j < array.length - i - 1; j++) {// 比较次数
				if (array[j] > array[j + 1]) {
					temp = array[j];
					array[j] = array[j + 1];
					array[j + 1] = temp;
				}
			}
		}
		return array;
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
		return _magic_card_index[index];// MJGameConstants.MAX_CP_INDEX;
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
		int cbTempCardData[] = new int[GameConstants.MAX_GXZP_COUNT];

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

	public int check_nei_hua(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.DSS_WIK_PENG : GameConstants.DSS_WIK_NULL;
	}

	// 碰牌判断
	public int check_che(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.DSS_WIK_PENG : GameConstants.DSS_WIK_NULL;
	}

	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.DSS_MAX_CP_COUNT];

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
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++)
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
		if (card_index < 0 || card_index >= GameConstants.MAX_CP_INDEX) {
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
					cards_index[j]++;
				}
				return false;
			}
		}

		return true;
	}

	public int get_hong_pai_count(int weave_kind, int center_card) {
		switch (weave_kind) {
		case GameConstants.DSS_WIK_PENG: {
			if (this.get_hong_dot(center_card) != 0)
				return 3;
			break;
		}
		case GameConstants.DSS_WIK_LEFT:
		case GameConstants.DSS_WIK_RIGHT:
		case GameConstants.DSS_WIK_CENTER: {
			int hong_count = 0;
			if (this.get_hong_dot(center_card) != 0)
				hong_count++;
			int other_card = this.get_kind_card(center_card, weave_kind);
			if (this.get_hong_dot(other_card) != 0)
				hong_count++;
			return hong_count;
		}

		}
		return 0;
	}

	public int get_hei_pai_count(int weave_kind, int center_card) {
		switch (weave_kind) {
		case GameConstants.DSS_WIK_PENG: {
			if (this.get_hong_dot(center_card) == 0)
				return 3;
			break;
		}
		case GameConstants.DSS_WIK_LEFT:
		case GameConstants.DSS_WIK_RIGHT:
		case GameConstants.DSS_WIK_CENTER: {
			int hei_count = 0;
			if (this.get_hong_dot(center_card) == 0)
				hei_count++;
			int other_card = this.get_kind_card(center_card, weave_kind);
			if (this.get_hong_dot(other_card) == 0)
				hei_count++;
			return hei_count;
		}

		}
		return 0;
	}

	public int get_hong_pai_count_qlhf(int weave_kind, int center_card) {
		switch (weave_kind) {
		case GameConstants.DSS_WIK_DH_FOUR: {
			if (this.get_hong_dot(center_card) != 0)
				return 4;
			break;
		}
		case GameConstants.DSS_WIK_DH_THREE:
		case GameConstants.DSS_WIK_PENG: {
			if (this.get_hong_dot(center_card) != 0)
				return 3;
			break;
		}
		case GameConstants.DSS_WIK_DH_TWO: {
			if (this.get_hong_dot(center_card) != 0)
				return 2;
			break;
		}
		case GameConstants.DSS_WIK_DH_ONE: {
			if (this.get_hong_dot(center_card) != 0)
				return 1;
			break;
		}
		case GameConstants.DSS_WIK_LEFT:
		case GameConstants.DSS_WIK_RIGHT:
		case GameConstants.DSS_WIK_CENTER: {
			int hong_count = 0;
			if (this.get_hong_dot(center_card) != 0)
				hong_count++;
			int other_card = this.get_kind_card(center_card, weave_kind);
			if (this.get_hong_dot(other_card) != 0)
				hong_count++;
			return hong_count;
		}

		}
		return 0;
	}

	public int get_hei_pai_count_qlhf(int weave_kind, int center_card) {
		switch (weave_kind) {
		case GameConstants.DSS_WIK_DH_FOUR: {
			if (this.get_hong_dot(center_card) == 0)
				return 4;
			break;
		}
		case GameConstants.DSS_WIK_PENG:
		case GameConstants.DSS_WIK_DH_THREE: {
			if (this.get_hong_dot(center_card) == 0)
				return 3;
			break;
		}
		case GameConstants.DSS_WIK_DH_TWO: {
			if (this.get_hong_dot(center_card) == 0)
				return 2;
			break;
		}
		case GameConstants.DSS_WIK_DH_ONE: {
			if (this.get_hong_dot(center_card) == 0)
				return 1;
			break;
		}
		case GameConstants.DSS_WIK_LEFT:
		case GameConstants.DSS_WIK_RIGHT:
		case GameConstants.DSS_WIK_CENTER: {
			int hei_count = 0;
			if (this.get_hong_dot(center_card) == 0)
				hei_count++;
			int other_card = this.get_kind_card(center_card, weave_kind);
			if (this.get_hong_dot(other_card) == 0)
				hei_count++;
			return hei_count;
		}

		}
		return 0;
	}

	public int get_hong_dian_shu(int weave_kind, int center_card) {
		switch (weave_kind) {
		case GameConstants.DSS_WIK_PENG: {
			if (this.get_hong_dot(center_card) != 0)
				return this.get_hong_dot(center_card) * 3;
			break;
		}
		case GameConstants.DSS_WIK_LEFT:
		case GameConstants.DSS_WIK_RIGHT:
		case GameConstants.DSS_WIK_CENTER: {
			int hong_count = 0;
			if (this.get_hong_dot(center_card) != 0)
				hong_count += this.get_hong_dot(center_card);
			int other_card = this.get_kind_card(center_card, weave_kind);
			if (this.get_hong_dot(other_card) != 0)
				hong_count += this.get_hong_dot(other_card);
			return hong_count;
		}

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
			case GameConstants.WIK_TUO:
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

	public int get_analyse_tuo_shu(int cbWeaveKind, int center_card) {
		switch (cbWeaveKind) {
		case GameConstants.DSS_WIK_DH_FOUR: {
			int times = 1;
			if (get_hei_dot(center_card) == 0)
				times = 2;
			return 10 * 4 * times;
		}
		case GameConstants.DSS_WIK_PENG: {
			int times = 1;
			if (get_hei_dot(center_card) == 0)
				times = 2;
			int count = get_hong_dot(center_card) * 3 * times;
			if (count < 10)
				return 10;
			else
				return count;
		}
		case GameConstants.DSS_WIK_DH_THREE: {
			int times = 1;
			if (get_hei_dot(center_card) == 0)
				times = 2;
			return 10 * 3 * times;
		}
		case GameConstants.DSS_WIK_DH_TWO: {
			int times = 1;
			if (get_hei_dot(center_card) == 0)
				times = 2;
			return 10 * 2 * times;
		}
		case GameConstants.DSS_WIK_DH_ONE: {
			int times = 1;
			if (get_hei_dot(center_card) == 0)
				times = 2;
			return 10 * 1 * times;
		}
		case GameConstants.DSS_WIK_LEFT:
		case GameConstants.DSS_WIK_CENTER:
		case GameConstants.DSS_WIK_RIGHT: {
			int times = 1;
			if (get_hei_dot(center_card) == 0)
				times = 2;
			int hong_tuo = 0;
			hong_tuo += get_hong_dot(center_card) * times;
			times = 1;
			if (get_hei_dot(this.get_kind_card(center_card, cbWeaveKind)) == 0)
				times = 2;
			hong_tuo += get_hong_dot(this.get_kind_card(center_card, cbWeaveKind)) * times;
			return hong_tuo;
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

	// 提牌判断
	public int get_action_ti_Card(int cards_index[], int ti_cards_index[]) {
		int ti_card_count = 0;
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			if (cards_index[i] == 4) {
				ti_cards_index[ti_card_count++] = cards_index[i];
			}
		}
		return ti_card_count;
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
		if (is_magic_card(card)) {
			b_hei = false;
		}
		return b_hei;
	}

	public int calculate_weave_hei_pai(WeaveItem weave_item) {
		int count = 0;
		switch (weave_item.weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
		case GameConstants.WIK_TUO:
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

	public int get_times_cards(int card) {
		int count = 0;
		switch (card) {
		case 0x66: // 天
		case 0x20: // 地
		case 0x80: // 人牌
		case 0x13: // 和牌
		case 0x50: // 幺
			count++;

		}
		return count;
	}

	public int calculate_weave_hong_pai(WeaveItem weave_item) {
		int count = 0;
		switch (weave_item.weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_PAO:
		case GameConstants.WIK_TUO:
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
		case GameConstants.WIK_TUO:
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
		case GameConstants.WIK_TUO:
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

	public int calculate_tuan_yuan_count(WeaveItem hu_weave_item[], int hu_weave_count) {
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

		int cbMagicCardIndex[] = new int[GameConstants.MAX_CP_INDEX];
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
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

		int mj_count = GameConstants.MAX_CP_INDEX;
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
			if ((i < (GameConstants.MAX_CP_INDEX - 2)) && ((i % 3) == 0)) {
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
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
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

	public int get_four_count(WeaveItem weaveItem[], int cbWeaveCount, boolean is_jsg) {
		int count = 0;
		int cards_index[] = new int[GameConstants.MAX_CP_INDEX];
		int peng_index[] = new int[GameConstants.DSS_MAX_CP_INDEX];
		for (int i = 0; i < cbWeaveCount; i++) {
			switch (weaveItem[i].weave_kind) {
			case GameConstants.DSS_WIK_PENG: {
				cards_index[this.switch_to_card_index(weaveItem[i].center_card)] += 3;
				peng_index[this.switch_to_card_index(weaveItem[i].center_card)] += 1;
				break;
			}
			case GameConstants.DSS_WIK_LEFT:
			case GameConstants.DSS_WIK_CENTER:
			case GameConstants.DSS_WIK_RIGHT: {
				cards_index[this.switch_to_card_index(weaveItem[i].center_card)]++;
				cards_index[this.switch_to_card_index(this.get_kind_card(weaveItem[i].center_card, weaveItem[i].weave_kind))]++;
				break;
			}
			case GameConstants.DSS_WIK_DH_FOUR: {
				cards_index[this.switch_to_card_index(weaveItem[i].center_card)] = 4;
				break;
			}
			case GameConstants.DSS_WIK_DH_THREE: {
				cards_index[this.switch_to_card_index(weaveItem[i].center_card)] = 3;
				break;
			}
			case GameConstants.DSS_WIK_DH_TWO: {
				cards_index[this.switch_to_card_index(weaveItem[i].center_card)] = 2;
				break;
			}
			case GameConstants.DSS_WIK_DH_ONE: {
				cards_index[this.switch_to_card_index(weaveItem[i].center_card)] = 1;
				break;
			}
			}
		}
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			if (cards_index[i] == 4) {
				count++;
				if (get_times_cards(this.switch_to_card_data(i)) > 0 && peng_index[i] != 0 && is_jsg == true) {
					count++;
				}
			}
		}
		return count;
	}

	// 分析扑克
	public boolean analyse_card_qihf(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index, int cur_card,
			List<AnalyseItem> analyseItemArray, boolean dispatch) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);

		// 需求判断
		if (cbCardCount == 0)
			return false;
		// 拆分分析
		AnalyseItem analyseItem = new AnalyseItem();
		int array_count = cbWeaveCount;
		int temp_cards_index[] = new int[GameConstants.DSS_MAX_CP_INDEX];
		for (int i = 0; i < GameConstants.DSS_MAX_CP_INDEX; i++) {
			int cards[] = new int[3];
			Arrays.fill(cards, 0);
			int card_count = this.switch_to_value_to_card(switch_to_card_value(switch_to_card_data(i)), cards);
			int count = 0;
			for (int c = 0; c < card_count; c++) {
				count += cards_index[this.switch_to_card_index(cards[c])];
			}
			if (cards_index[i] == 3 && count != 3) {
				analyseItem.cbWeaveKind[array_count] = GameConstants.DZ_WIK_PENG;
				analyseItem.cbCenterCard[array_count] = this.switch_to_card_data(i);
				analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(GameConstants.DZ_WIK_SHA_QIANG, this.switch_to_card_data(i));
				temp_cards_index[i] = 0;
			} else {
				temp_cards_index[i] = cards_index[i];
			}
		}

		for (int i = 2; i <= 10; i++) {
			int cards[] = new int[3];
			Arrays.fill(cards, 0);
			int card_count = this.switch_to_value_to_card(i, cards);
			if (i == 7) {
				cbCardCount = get_card_count_by_index(temp_cards_index);
				if (cbCardCount % 3 != 0)
					break;
				for (int j = 0; j < card_count; j++) {
					if (temp_cards_index[this.switch_to_card_index(cards[j])] >= 3) {
						--temp_cards_index[this.switch_to_card_index(cards[j])];
						--temp_cards_index[this.switch_to_card_index(cards[j])];
						--temp_cards_index[this.switch_to_card_index(cards[j])];
						analyseItem.cbWeaveKind[array_count] = GameConstants.DZ_WIK_PENG;
						analyseItem.cbCenterCard[array_count] = cards[j];
						analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(GameConstants.DZ_WIK_SHA_QIANG, cards[j]);
					}
					if (temp_cards_index[this.switch_to_card_index(cards[j])] > 1) {
						for (int k = j + 1; k < card_count; k++) {
							if (temp_cards_index[this.switch_to_card_index(cards[k])] > 0) {
								--temp_cards_index[this.switch_to_card_index(cards[j])];
								--temp_cards_index[this.switch_to_card_index(cards[j])];
								--temp_cards_index[this.switch_to_card_index(cards[k])];
								int action = this.get_chi_action(cards[j], cards[k], cards[j]);
								analyseItem.cbWeaveKind[array_count] = action;
								analyseItem.cbCenterCard[array_count] = cards[j];
								analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, cards[j]);
								break;
							}
						}
					}
					if (temp_cards_index[this.switch_to_card_index(cards[j])] > 0) {
						for (int k = j + 1; k < card_count; k++) {
							if (temp_cards_index[this.switch_to_card_index(cards[k])] > 1) {
								--temp_cards_index[this.switch_to_card_index(cards[j])];
								--temp_cards_index[this.switch_to_card_index(cards[k])];
								--temp_cards_index[this.switch_to_card_index(cards[k])];
								int action = this.get_chi_action(cards[j], cards[k], cards[k]);
								analyseItem.cbWeaveKind[array_count] = action;
								analyseItem.cbCenterCard[array_count] = cards[j];
								analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, cards[j]);
								break;
							}
						}
					}
				}
				if (temp_cards_index[this.switch_to_card_index(cards[0])] == 1 && temp_cards_index[this.switch_to_card_index(cards[1])] == 1
						&& temp_cards_index[this.switch_to_card_index(cards[2])] == 1) {
					--temp_cards_index[this.switch_to_card_index(cards[0])];
					--temp_cards_index[this.switch_to_card_index(cards[1])];
					--temp_cards_index[this.switch_to_card_index(cards[2])];
					int action = this.get_chi_action(cards[0], cards[1], cards[2]);
					analyseItem.cbWeaveKind[array_count] = action;
					analyseItem.cbCenterCard[array_count] = cards[0];
					analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, cards[0]);
				}
				continue;
			}
			int cards1[] = new int[3];
			Arrays.fill(cards1, 0);
			int card_count1 = this.switch_to_value_to_card(14 - i, cards1);
			for (int j = 0; j < card_count; j++) {
				if (temp_cards_index[this.switch_to_card_index(cards[j])] == 0)
					continue;
				if (temp_cards_index[this.switch_to_card_index(cards[j])] == 1) {
					int k = 0;
					for (; k < card_count1; k++) {
						if (temp_cards_index[this.switch_to_card_index(cards1[k])] > 1) {
							--temp_cards_index[this.switch_to_card_index(cards[j])];
							--temp_cards_index[this.switch_to_card_index(cards1[k])];
							--temp_cards_index[this.switch_to_card_index(cards1[k])];
							int action = this.get_chi_action(cards[j], cards1[k], cards1[k]);
							analyseItem.cbWeaveKind[array_count] = action;
							analyseItem.cbCenterCard[array_count] = cards[j];
							analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, cards[j]);
							break;
						}
					}
				}
				if (temp_cards_index[this.switch_to_card_index(cards[j])] == 2) {
					int k = 0;
					for (; k < card_count1; k++) {
						if (temp_cards_index[this.switch_to_card_index(cards1[k])] > 0) {
							for (int kk = k + 1; kk < card_count1; kk++) {
								if (temp_cards_index[this.switch_to_card_index(cards1[kk])] > 0
										&& temp_cards_index[switch_to_card_index(cards1[k])] > temp_cards_index[switch_to_card_index(cards1[kk])]) {
									k = kk;
								}
							}
							--temp_cards_index[this.switch_to_card_index(cards[j])];
							--temp_cards_index[this.switch_to_card_index(cards[j])];
							--temp_cards_index[this.switch_to_card_index(cards1[k])];
							int action = this.get_chi_action(cards[j], cards1[k], cards1[k]);
							analyseItem.cbWeaveKind[array_count] = action;
							analyseItem.cbCenterCard[array_count] = cards[j];
							analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, cards[j]);

							break;
						}
					}
				}

				if (temp_cards_index[this.switch_to_card_index(cards[j])] == 3) {
					int k = 0;
					for (; k < card_count1; k++) {
						if (temp_cards_index[this.switch_to_card_index(cards1[k])] > 1) {
							--temp_cards_index[this.switch_to_card_index(cards[j])];
							--temp_cards_index[this.switch_to_card_index(cards1[k])];
							--temp_cards_index[this.switch_to_card_index(cards1[k])];
							int action = this.get_chi_action(cards[j], cards1[k], cards1[k]);
							analyseItem.cbWeaveKind[array_count] = action;
							analyseItem.cbCenterCard[array_count] = cards[j];
							analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, cards[j]);
							break;
						}
					}
				}
				if (temp_cards_index[this.switch_to_card_index(cards[j])] == 3) {
					--temp_cards_index[this.switch_to_card_index(cards[j])];
					--temp_cards_index[this.switch_to_card_index(cards[j])];
					--temp_cards_index[this.switch_to_card_index(cards[j])];
					analyseItem.cbWeaveKind[array_count] = GameConstants.DZ_WIK_PENG;
					analyseItem.cbCenterCard[array_count] = cards[j];
					analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(GameConstants.DZ_WIK_SHA_QIANG, cards[j]);

					break;
				}
			}
			// for (int j = 0; j < card_count; j++) {
			// if (temp_cards_index[this.switch_to_card_index(cards[j])] == 0)
			// continue;
			// if (temp_cards_index[this.switch_to_card_index(cards[j])] > 1) {
			// int k = 0;
			// for (; k < card_count1; k++) {
			// if (temp_cards_index[this.switch_to_card_index(cards1[k])] > 0) {
			// for (int kk = k + 1; kk < card_count1; kk++) {
			// if (temp_cards_index[this.switch_to_card_index(cards1[kk])] > 0
			// && temp_cards_index[switch_to_card_index(cards1[k])] >
			// temp_cards_index[switch_to_card_index(cards1[kk])]) {
			// k = kk;
			// }
			// }
			// --temp_cards_index[this.switch_to_card_index(cards[j])];
			// --temp_cards_index[this.switch_to_card_index(cards[j])];
			// --temp_cards_index[this.switch_to_card_index(cards1[k])];
			// int action = this.get_chi_action(cards[j], cards1[k], cards1[k]);
			// analyseItem.cbWeaveKind[array_count] = action;
			// analyseItem.cbCenterCard[array_count] = cards[j];
			// analyseItem.hu_xi[array_count++] =
			// this.get_analyse_tuo_shu(action, cards[j]);
			//
			// break;
			// } else if (temp_cards_index[this.switch_to_card_index(cards[j])]
			// == 3) {
			// --temp_cards_index[this.switch_to_card_index(cards[j])];
			// --temp_cards_index[this.switch_to_card_index(cards[j])];
			// --temp_cards_index[this.switch_to_card_index(cards[j])];
			// analyseItem.cbWeaveKind[array_count] = GameConstants.DZ_WIK_PENG;
			// analyseItem.cbCenterCard[array_count] = cards[j];
			// analyseItem.hu_xi[array_count++] =
			// this.get_analyse_tuo_shu(GameConstants.DZ_WIK_SHA_QIANG,
			// cards[j]);
			//
			// break;
			// }
			// }
			// }
			// }
		}
		cbCardCount = get_card_count_by_index(temp_cards_index);
		if (cbCardCount != 0)
			return false;
		for (int i = 0; i < cbWeaveCount; i++) {
			analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
			analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
			analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
		}
		analyseItemArray.add(analyseItem);
		return (analyseItemArray.size() > 0 ? true : false);
	}

	public int get_chi_action(int card, int temp_card, int second_card) {
		switch (card) {
		case 0x20:
		case 0x0b:
		case 0x12:
		case 0x66: {
			if (second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else
				return GameConstants.DZ_WIK_DUI_LEFT;

		}
		case 0x04:
		case 0x13: {
			if (temp_card == 0x0a && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else if (temp_card == 0x0a)
				return GameConstants.DZ_WIK_DUI_LEFT;
			if (temp_card == 0x46 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_CENTER;
			else if (temp_card == 0x46)
				return GameConstants.DZ_WIK_DUI_CENTER;
		}
		case 0x05:
		case 0x50: {
			if (temp_card == 0x45 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else if (temp_card == 0x45)
				return GameConstants.DZ_WIK_DUI_LEFT;
			if (temp_card == 0x09 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_CENTER;
			else if (temp_card == 0x09)
				return GameConstants.DZ_WIK_DUI_CENTER;
		}
		case 0x06:
		case 0x15:
		case 0x42: {
			if (temp_card == 0xd8 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else if (temp_card == 0xd8)
				return GameConstants.DZ_WIK_DUI_LEFT;
			if (temp_card == 0xf8 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_CENTER;
			else if (temp_card == 0xf8)
				return GameConstants.DZ_WIK_DUI_CENTER;
			if (temp_card == 0x80 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_RIGHT;
			else if (temp_card == 0x80)
				return GameConstants.DZ_WIK_DUI_RIGHT;
		}
		case 0x43:
		case 0x07:
		case 0x16: {
			if (temp_card != second_card && temp_card != card && second_card != card)
				return GameConstants.DZ_WIK_OTHER;
			else if (temp_card == 0x43 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else if (temp_card == 0x43)
				return GameConstants.DZ_WIK_DUI_LEFT;
			if (temp_card == 0x07 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_CENTER;
			else if (temp_card == 0x07)
				return GameConstants.DZ_WIK_DUI_CENTER;
			if (temp_card == 0x16 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_RIGHT;
			else if (temp_card == 0x16)
				return GameConstants.DZ_WIK_DUI_RIGHT;
		}
		case 0xd8:
		case 0xf8:
		case 0x80: {
			if (temp_card == 0x06 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else if (temp_card == 0x06)
				return GameConstants.DZ_WIK_DUI_LEFT;
			if (temp_card == 0x15 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_CENTER;
			else if (temp_card == 0x15)
				return GameConstants.DZ_WIK_DUI_CENTER;
			if (temp_card == 0x42 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_RIGHT;
			else if (temp_card == 0x42)
				return GameConstants.DZ_WIK_DUI_RIGHT;

		}
		case 0x45:
		case 0x09: {
			if (temp_card == 0x05 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else if (temp_card == 0x05)
				return GameConstants.DZ_WIK_DUI_LEFT;
			if (temp_card == 0x50 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_CENTER;
			else if (temp_card == 0x50)
				return GameConstants.DZ_WIK_DUI_CENTER;
		}
		case 0x0a:
		case 0x46: {
			if (temp_card == 0x04 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_LEFT;
			else if (temp_card == 0x04)
				return GameConstants.DZ_WIK_DUI_LEFT;
			if (temp_card == 0x13 && second_card == card)
				return GameConstants.DZ_WIK_SINGLE_CENTER;
			else if (temp_card == 0x13)
				return GameConstants.DZ_WIK_DUI_CENTER;
		}
		}
		return GameConstants.DZ_WIK_NULL;

	}

	// 分析扑克
	public boolean analyse_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index, int cur_card,
			List<AnalyseItem> analyseItemArray, boolean dispatch) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);

		// 需求判断
		if (cbCardCount == 0)
			return false;
		int temp_cards_index[] = new int[GameConstants.MAX_CP_INDEX];
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			temp_cards_index[i] = cards_index[i];
		}
		// 拆分分析
		AnalyseItem analyseItem = new AnalyseItem();
		int array_count = cbWeaveCount;
		for (int i = 2; i <= 7; i++) {
			int cards[] = new int[3];
			Arrays.fill(cards, 0);
			int card_count = this.switch_to_value_to_card(i, cards);
			if (i == 7) {
				cbCardCount = get_card_count_by_index(temp_cards_index);
				if (cbCardCount % 2 != 0)
					break;
				int s_card_count = 0;
				int s_card[] = new int[2];
				for (int j = 0; j < card_count; j++) {

					while (temp_cards_index[this.switch_to_card_index(cards[j])] > 0) {
						--temp_cards_index[this.switch_to_card_index(cards[j])];
						s_card[s_card_count++] = cards[j];
						while (temp_cards_index[this.switch_to_card_index(cards[j])] == 0) {

							j++;
							if (j == card_count)
								break;

						}
						if (j == card_count)
							break;
						--temp_cards_index[this.switch_to_card_index(cards[j])];
						s_card[s_card_count++] = cards[j];
						if (s_card_count == 2) {
							int action = this.get_chi_action(s_card[0], s_card[1]);
							analyseItem.cbWeaveKind[array_count] = action;
							analyseItem.cbCenterCard[array_count] = s_card[0];
							analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, s_card[0]);
							s_card_count = 0;
						}
					}
				}
				continue;
			}
			int cards1[] = new int[3];
			Arrays.fill(cards1, 0);
			int card_count1 = this.switch_to_value_to_card(14 - i, cards1);
			for (int j = 0; j < card_count; j++) {
				if (temp_cards_index[this.switch_to_card_index(cards[j])] == 0)
					continue;
				for (int k = 0; k < card_count1; k++) {
					while (temp_cards_index[this.switch_to_card_index(cards1[k])] > 0 && temp_cards_index[this.switch_to_card_index(cards[j])] > 0) {
						--temp_cards_index[this.switch_to_card_index(cards[j])];
						--temp_cards_index[this.switch_to_card_index(cards1[k])];
						int action = this.get_chi_action(cards[j], cards1[k]);
						if (array_count == 9)
							break;
						analyseItem.cbWeaveKind[array_count] = action;
						analyseItem.cbCenterCard[array_count] = cards[j];
						analyseItem.hu_xi[array_count++] = this.get_analyse_tuo_shu(action, cards[j]);
						if (temp_cards_index[this.switch_to_card_index(cards[j])] == 0)
							break;
					}
					if (temp_cards_index[this.switch_to_card_index(cards[j])] == 0)
						break;

				}
			}
		}
		cbCardCount = get_card_count_by_index(temp_cards_index);
		if (cbCardCount != 0)
			return false;
		for (int i = 0; i < cbWeaveCount; i++) {
			analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
			analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
			analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
		}
		analyseItemArray.add(analyseItem);
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

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.DSS_WIK_ZI_MO) {
			return 50;
		}
		// 吃胡牌等级
		if (player_action == GameConstants.DSS_WIK_CHI_HU) {
			return 40;
		}

		// 偷牌等级
		if (player_action == GameConstants.DSS_WIK_PENG) {
			return 30;
		}
		if (player_action == GameConstants.DSS_WIK_DH_FOUR) {
			return 30;
		}
		if (player_action == GameConstants.DSS_WIK_DH_THREE) {
			return 30;
		}
		if (player_action == GameConstants.DSS_WIK_DH_TWO) {
			return 30;
		}
		if (player_action == GameConstants.DSS_WIK_DH_ONE) {
			return 30;
		}
		if (player_action == GameConstants.DSS_WIK_BAO_TING) {
			return 30;
		}

		// 上牌等级
		if (player_action == GameConstants.DSS_WIK_RIGHT || player_action == GameConstants.DSS_WIK_CENTER
				|| player_action == GameConstants.DSS_WIK_LEFT) {
			return 10;
		}

		return 0;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int MAX_CP_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (MAX_CP_INDEX < index) {
				MAX_CP_INDEX = index;
			}

		}

		return MAX_CP_INDEX;
	}

	public int get_chi_hu_action_rank_hh(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu = 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu = 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu = 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HEI)).is_empty()) {
			wFanShu = 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
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

	public int get_chi_hu_action_rank_phz_chd(int seat_index, int da_pai_count, int xiao_pai_count, int tuan_yuan_count, int huang_zhang_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu += 3 + (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu += 8;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DA_HU)).is_empty()) {
			wFanShu += 8 + (da_pai_count - 18);
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()) {
			wFanShu += 10 + (xiao_pai_count - 16);
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUANG_FAN)).is_empty()) {
			wFanShu += 1 + huang_zhang_count;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TUAN_YUAN)).is_empty()) {
			wFanShu += 8 * tuan_yuan_count;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI)).is_empty()) {
			wFanShu += 8;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_hgw(int seat_index, int da_pai_count, int xiao_pai_count, int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu += 2 + (hong_pai_count - 10);
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu += 5;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
			wFanShu += 3;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_thk(int seat_index, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu += 5;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu += 5;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_lhq_oho_hd(int seat_index, ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu *= 2;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu *= 2;
		}
		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_lhq(int seat_index, ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu *= 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu *= 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu *= 5;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
			wFanShu *= 2;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_lhq_hy(int seat_index, ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu *= 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu *= 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu *= 5;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_lhq_oho_hy(int seat_index, ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu *= 2;
		}
		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_lhq_qd(int seat_index, ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu *= 2;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu *= 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu *= 5;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_lhq_oho_qd(int seat_index, ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu *= 2;
		}
		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_phz_xt(int seat_index, int da_pai_count, int xiao_pai_count, int tuan_yuan_count, int huang_zhang_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu += 4 + (hong_pai_count - 13);
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_JIA_DIAN_HU)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu += 5;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu += 5;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DA_HU)).is_empty()) {
			wFanShu += 8 + (da_pai_count - 18);
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()) {
			wFanShu += 6 + (xiao_pai_count - 16);
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()) {
			wFanShu += 5;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		if (switch_to_card_index(card) == -1)
			return false;
		return true;
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		int index = -1;
		switch (card) {
		case 0x20:
			return ECPType.CP_20.ordinal();
		case 0x12:
			return ECPType.CP_12.ordinal();
		case 0x04:
			return ECPType.CP_04.ordinal();
		case 0x13:
			return ECPType.CP_13.ordinal();
		case 0x05:
			return ECPType.CP_05.ordinal();
		case 0x50:
			return ECPType.CP_50.ordinal();
		case 0x06:
			return ECPType.CP_06.ordinal();
		case 0x15:
			return ECPType.CP_15.ordinal();
		case 0x42:
			return ECPType.CP_42.ordinal();
		case 0x43:
			return ECPType.CP_43.ordinal();
		case 0x07:
			return ECPType.CP_07.ordinal();
		case 0x16:
			return ECPType.CP_16.ordinal();
		case 0xD8:
			return ECPType.CP_D8.ordinal();
		case 0xF8:
			return ECPType.CP_F8.ordinal();
		case 0x80:
			return ECPType.CP_80.ordinal();
		case 0x45:
			return ECPType.CP_45.ordinal();
		case 0x09:
			return ECPType.CP_09.ordinal();

		case 0x0A:
			return ECPType.CP_0A.ordinal();
		case 0x46:
			return ECPType.CP_46.ordinal();
		case 0x0B:
			return ECPType.CP_0B.ordinal();
		case 0x66:
			return ECPType.CP_66.ordinal();
		}
		return index;
	}

	public int switch_to_value_to_card(int card_dot, int cards[]) {
		int count = 0;
		switch (card_dot) {
		case 2: {
			cards[count++] = 0x20;
			break;
		}
		case 3: {
			cards[count++] = 0x12;
			break;
		}
		case 4: {
			cards[count++] = 0x04;
			cards[count++] = 0x13;
			break;
		}
		case 5: {
			cards[count++] = 0x05;
			cards[count++] = 0x50;
			break;
		}
		case 6: {
			cards[count++] = 0x06;
			cards[count++] = 0x15;
			cards[count++] = 0x42;
			break;
		}
		case 7: {
			cards[count++] = 0x43;
			cards[count++] = 0x07;
			cards[count++] = 0x16;
			break;
		}
		case 8: {
			cards[count++] = 0xd8;
			cards[count++] = 0xf8;
			cards[count++] = 0x80;
			break;
		}
		case 9: {
			cards[count++] = 0x45;
			cards[count++] = 0x09;
			break;
		}
		case 10: {
			cards[count++] = 0x0a;
			cards[count++] = 0x46;
			break;
		}
		case 11: {
			cards[count++] = 0x0b;
			break;
		}
		case 12: {
			cards[count++] = 0x66;
			break;
		}
		}
		return count;
	}

	public int get_chi_action(int card, int temp_card) {
		switch (card) {
		case 0x20:
		case 0x0b:
		case 0x12:
		case 0x66: {
			return GameConstants.DSS_WIK_LEFT;

		}
		case 0x04:
		case 0x13: {
			if (temp_card == 0x0a)
				return GameConstants.DSS_WIK_LEFT;
			if (temp_card == 0x46)
				return GameConstants.DSS_WIK_CENTER;
		}
		case 0x05:
		case 0x50: {
			if (temp_card == 0x45)
				return GameConstants.DSS_WIK_LEFT;
			if (temp_card == 0x09)
				return GameConstants.DSS_WIK_CENTER;
		}
		case 0x06:
		case 0x15:
		case 0x42: {
			if (temp_card == 0xd8)
				return GameConstants.DSS_WIK_LEFT;
			if (temp_card == 0xf8)
				return GameConstants.DSS_WIK_CENTER;
			if (temp_card == 0x80)
				return GameConstants.DSS_WIK_RIGHT;
		}
		case 0x43:
		case 0x07:
		case 0x16: {
			if (temp_card == 0x43)
				return GameConstants.DSS_WIK_LEFT;
			if (temp_card == 0x07)
				return GameConstants.DSS_WIK_CENTER;
			if (temp_card == 0x16)
				return GameConstants.DSS_WIK_RIGHT;
		}
		case 0xd8:
		case 0xf8:
		case 0x80: {
			if (temp_card == 0x06)
				return GameConstants.DSS_WIK_LEFT;
			if (temp_card == 0x15)
				return GameConstants.DSS_WIK_CENTER;
			if (temp_card == 0x42)
				return GameConstants.DSS_WIK_RIGHT;
		}
		case 0x45:
		case 0x09: {
			if (temp_card == 0x05)
				return GameConstants.DSS_WIK_LEFT;
			if (temp_card == 0x50)
				return GameConstants.DSS_WIK_CENTER;
		}
		case 0x0a:
		case 0x46: {
			if (temp_card == 0x04)
				return GameConstants.DSS_WIK_LEFT;
			if (temp_card == 0x13)
				return GameConstants.DSS_WIK_CENTER;
		}
		}
		return GameConstants.DSS_WIK_NULL;

	}

	public int get_kind_card(int card, int type) {
		switch (card) {
		case 0x20: {
			return 0x66;
		}
		case 0x0b: {
			return 0x12;
		}
		case 0x12: {
			return 0x0b;
		}
		case 0x66: {
			return 0x20;

		}
		case 0x04:
		case 0x13: {
			if (type == GameConstants.DSS_WIK_LEFT)
				return 0x0a;
			if (type == GameConstants.DSS_WIK_CENTER)
				return 0x46;
		}
		case 0x05:
		case 0x50: {
			if (type == GameConstants.DSS_WIK_LEFT)
				return 0x45;
			if (type == GameConstants.DSS_WIK_CENTER)
				return 0x09;
		}
		case 0x06:
		case 0x15:
		case 0x42: {
			if (type == GameConstants.DSS_WIK_LEFT)
				return 0xd8;
			if (type == GameConstants.DSS_WIK_CENTER)
				return 0xf8;
			if (type == GameConstants.DSS_WIK_RIGHT)
				return 0x80;
		}
		case 0x43:
		case 0x07:
		case 0x16: {
			if (type == GameConstants.DSS_WIK_LEFT)
				return 0x43;
			if (type == GameConstants.DSS_WIK_CENTER)
				return 0x07;
			if (type == GameConstants.DSS_WIK_RIGHT)
				return 0x16;
		}
		case 0xd8:
		case 0xf8:
		case 0x80: {
			if (type == GameConstants.DSS_WIK_LEFT)
				return 0x06;
			if (type == GameConstants.DSS_WIK_CENTER)
				return 0x15;
			if (type == GameConstants.DSS_WIK_RIGHT)
				return 0x42;
		}
		case 0x45:
		case 0x09: {
			if (type == GameConstants.DSS_WIK_LEFT)
				return 0x05;
			if (type == GameConstants.DSS_WIK_CENTER)
				return 0x50;
		}
		case 0x0a:
		case 0x46: {
			if (type == GameConstants.DSS_WIK_LEFT)
				return 0x04;
			if (type == GameConstants.DSS_WIK_CENTER)
				return 0x13;
		}
		}
		return GameConstants.DSS_WIK_NULL;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data(int card_index) {
		switch (card_index) {
		case 0:
			return 0x20;
		case 1:
			return 0x12;
		case 2:
			return 0x04;
		case 3:
			return 0x13;
		case 4:
			return 0x05;
		case 5:
			return 0x50;
		case 6:
			return 0x06;
		case 7:
			return 0x15;
		case 8:
			return 0x42;
		case 9:
			return 0x43;
		case 10:
			return 0x07;
		case 11:
			return 0x16;
		case 12:
			return 0xD8;
		case 13:
			return 0xF8;
		case 14:
			return 0x80;
		case 15:
			return 0x45;
		case 16:
			return 0x09;
		case 17:
			return 0x0A;
		case 18:
			return 0x46;
		case 19:
			return 0x0B;
		case 20:
			return 0x66;
		}
		return 0;
	}

	public int get_hong_dot(int card) {
		int dot = (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
		if (dot > 12)
			dot = 0;
		return dot;
	}

	public int get_hei_dot(int card) {
		int dot = card & GameConstants.LOGIC_MASK_VALUE;
		return dot;
	}

	public int get_dot(int card) {
		int dot = get_hong_dot(card) + get_hei_dot(card);
		return dot;
	}

	public int switch_to_card_value(int card) {
		if (card == 0xD8 || card == 0xF8 || card == 0x80) {
			return 8;
		}
		int dot = (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
		int dot1 = card & GameConstants.LOGIC_MASK_VALUE;
		return dot + dot1;
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

	public int switch_to_cards_first_data(int cards_data[], int start_index, int card_count, int cards_frist_data[]) {
		for (int i = 0; i < card_count; i++) {
			if (i < 3)
				cards_frist_data[i] = cards_data[start_index + i];
			else
				cards_frist_data[i] = GameConstants.BLACK_CARD;
		}
		return card_count;
	}

	public int switch_to_cards_first_data_QLHF(int cards_data[], int start_index, int card_count, int cards_frist_data[]) {
		for (int i = 0; i < card_count; i++) {
			cards_frist_data[i] = cards_data[start_index + i];
		}
		return card_count;
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
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	public static void main(String[] args) {
		// 插入扑克

	}

	public static class AnalyseItem {
		public int cbCardEye;//// 牌眼扑克
		public boolean bMagicEye;// 牌眼是否是王霸
		public int cbWeaveKind[] = new int[9];// 组合类型
		public int cbCenterCard[] = new int[9];// 中心扑克
		public int cbCardData[][] = new int[9][4]; // 实际扑克
		public int hu_xi[] = new int[9];// 计算胡息

		public int cbPoint;// 组合牌的最佳点数;

		public boolean curCardEye;// 当前摸的牌是否是牌眼
		public boolean isShuangDui;// 牌眼 true双对--判断碰碰胡
		public int eyeKind;// 牌眼 组合类型
		public int eyeCenterCard;// 牌眼 中心扑克
		public int cbHuXiCount; // 胡息
	}

}
