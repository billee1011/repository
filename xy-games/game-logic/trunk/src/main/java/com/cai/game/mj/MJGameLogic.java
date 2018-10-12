package com.cai.game.mj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.constant.game.Constants_HuangShi;
import com.cai.common.constant.game.GameConstants_HY258;
import com.cai.common.constant.game.GameConstants_ZYZJ;
import com.cai.common.domain.BuCardResult;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.LiangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.handler.yyzxz.MJHandlerDispatchCard_YYZXZ.XiaZiCardResult;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.tasks.AnalyseMJCardTask;

public class MJGameLogic {

	// 东
	public static final int INDEX_DONG_FENG = 27;
	// 西
	public static final int INDEX_XI_FENG = 28;
	// 南
	public static final int INDEX_NAN_FENG = 29;
	// 北
	public static final int INDEX_BEI_FENG = 30;
	// 中
	public static final int INDEX_ZHONG_FENG = 31;
	// 发
	public static final int INDEX_FA_FENG = 32;
	// 白
	public static final int INDEX_BAI_FENG = 33;

	// 组合重复4次
	public static final int MULTI_FOUR = 4;
	// 组合重复3次
	public static final int MULTI_THREE = 3;
	// 组合重复2次
	public static final int MULTI_TWO = 2;
	// 组合重复1次
	public static final int MULTI_ONE = 1;

	// 风吃，东西南
	public static final int FENG_DXN = 1;
	// 风吃，东西北
	public static final int FENG_DXB = 2;
	// 风吃，东南北
	public static final int FENG_DNB = 3;
	// 风吃，西南北
	public static final int FENG_XNB = 4;
	// 风吃，中发白
	public static final int FENG_ZFB = 5;

	// 牌数目，4张
	public static final int CC_FOUR = 4;
	// 牌数目，3张
	public static final int CC_THREE = 3;
	// 牌数目，2张
	public static final int CC_TWO = 2;
	// 牌数目，1张
	public static final int CC_ONE = 1;

	// 1万索引
	public static final int WAN_FIRST_INDEX = 0;
	// 9万索引
	public static final int WAN_LAST_INDEX = 8;
	// 1条索引
	public static final int TIAO_FIRST_INDEX = 9;
	// 9条索引
	public static final int TIAO_LAST_INDEX = 17;
	// 1筒索引
	public static final int TONG_FIRST_INDEX = 18;
	// 9筒索引
	public static final int TONG_LAST_INDEX = 26;

	private static Logger logger = Logger.getLogger(MJGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _da_gen;
	private int _ding_gui;

	private int _ci; // 洛阳杠次 次牌
	private int _wang_ba; // 王霸牌

	private int _hua_card_index[];
	private int _hua_count;
	private boolean has_jia_bao;

	public void has_jia_bao_valid() {
		has_jia_bao = true;
	}

	public boolean get_has_jia_bao() {
		return has_jia_bao;
	}

	/**
	 * 麻将类型
	 */
	private final MJType mjType;

	public MJGameLogic(MJType mjType) {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_COUNT];
		_lai_gen = 0;
		_da_gen = 0;
		_ding_gui = 0;
		_ci = -1;
		_wang_ba = 0;
		_hua_count = 0;
		has_jia_bao = false;
		_hua_card_index = new int[GameConstants.MAX_COUNT];
		this.mjType = mjType;
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

	public void add_da_gen_card(int card) {
		_da_gen = card;
	}

	public void add_ding_gui_card(int card) {
		_ding_gui = card;
	}

	public void add_ci_card(int card) {
		_ci = card;
	}

	public void add_wang_ba_card(int card) {
		_wang_ba = card;
	}

	public boolean is_magic_card(int card) {
		for (int i = 0; i < _magic_card_count; i++) {
			if (_magic_card_index[i] == switch_to_card_index(card)) {
				return true;
			}
		}
		return false;
	}

	// 花牌判断
	public boolean is_hua_card(int card) {
		for (int i = 0; i < _hua_count; i++) {
			if (_hua_card_index[i] == switch_to_card_index(card)) {
				return true;
			}
		}
		return false;
	}

	// 判断手牌存在花牌
	public boolean is_hua_card_array(int card_index[]) {
		for (int i = 0; i < _hua_count; i++) {
			if (card_index[_hua_card_index[i]] > 0) {
				return true;
			}
		}
		return false;
	}

	public int rt_Hua_Index(int card_index[]) {
		for (int i = 0; i < _hua_count; i++) {
			if (card_index[_hua_card_index[i]] > 0) {
				return _hua_card_index[i];
			}
		}
		return GameConstants.INVALID_SEAT;
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

	public boolean is_da_gen_card(int card) {
		if (_da_gen == card) {
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

	public boolean is_ci_card(int card) {
		if (_ci == card) {
			return true;
		}
		return false;
	}

	public boolean is_wang_ba_card(int card) {
		if (_wang_ba == card) {
			return true;
		}
		return false;
	}

	public int get_ci_card_index() {
		return _ci;
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
		return _magic_card_index[index];// MJGameConstants.MAX_INDEX;
	}

	public int get_magic_card_count() {
		return _magic_card_count;
	}

	public int[] get_all_magic_card_index() {
		return Arrays.copyOf(_magic_card_index, _magic_card_count);
	}

	public void set_magic_card_count(int _magic_card_count) {
		this._magic_card_count = _magic_card_count;
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

	// 删除扑克 by data
	public boolean remove_card_by_data(int cards[], int card_data) {
		int card_count = cards.length;

		if (card_count == 0) {
			return false;
		}

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.MAX_COUNT];

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
		int cbTempCardData[] = new int[GameConstants.MAX_COUNT];

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
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}

	/**
	 * 获取特定索引值的牌数据
	 * 
	 * @param cards_index
	 * @param start_index
	 * @param end_index
	 * @return
	 */
	public int get_card_count(int[] cards_index, int start_index, int end_index) {
		int card_count = 0;
		for (int i = start_index; i < end_index; i++)
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
		if (card_index < 0 || card_index >= GameConstants.MAX_INDEX) {
			return false;
		}

		if (cards_index[card_index] <= 0) {
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
					cards_index[switch_to_card_index(cards[j])]++;
				}
				return false;
			}
		}

		return true;
	}

	public boolean remove_cards_by_index_lsdy(int cards_index[], int lsdy_cards_index[], int cards[], int card_count) {
		int removeCount = 0;

		int tmpCount = 0;
		Set<Integer> indexSet = new HashSet<>();
		for (int i = 0; i < card_count; i++) {
			int index = switch_to_card_index(cards[i]);
			if (!indexSet.contains(index)) {
				tmpCount += cards_index[index];
				indexSet.add(index);
			}
		}

		for (int i = 0; i < tmpCount && i < card_count; i++) {
			if (remove_card_by_index(cards_index, cards[i]) == false) {
				for (int j = 0; j < i; j++) {
					cards_index[switch_to_card_index(cards[j])]++;
				}
				return false;
			} else {
				removeCount++;
			}
		}

		if (removeCount < card_count) {
			for (int x = removeCount; x < card_count; x++) {
				if (remove_card_by_index(lsdy_cards_index, cards[x]) == false) {
					for (int y = removeCount; y < x; y++) {
						lsdy_cards_index[switch_to_card_index(cards[y])]++;
					}
					return false;
				}
			}
		}

		return true;
	}

	public int check_chi_xiang_tan(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		// 如果是风牌或者王牌
		if (get_card_color(cur_card) > 2 || is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		// 构造数据
		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		// 插入扑克
		tmp_cards_index[switch_to_card_index(cur_card)]++;

		int eat_type = GameConstants.WIK_NULL;

		int first_card_index = 0;

		int cur_card_index = switch_to_card_index(cur_card);

		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 轮询判断，可以有多种吃法，首先判断左吃，然后判断中吃，最后判断右吃
		for (int i = 0; i < 3; i++) {

			int card_value = get_card_value(cur_card);

			// 牌值判断，比如左吃时，牌值可以是1,2...7；右吃时，牌值可以是3,4...9
			if ((card_value >= (i + 1)) && (card_value <= 7 + i)) {

				// 吃牌时，第一张牌的实际索引值，按照左吃、中吃、右吃的顺序来
				// 比如左吃时，第一张牌的索引就是当前的牌，中吃的时候就是当前牌的前面一张牌
				// 为什么要用索引呢？因为传进来的数据是索引数据，哈哈哈
				first_card_index = cur_card_index - i;

				if (this._magic_card_count > 0) {
					// 如果有王牌，实际上湘潭麻将总会有王牌

					// 无论是左吃，还是中吃，还是右吃，三张牌的任意一张都不能是癞子
					if (is_magic_index(first_card_index) || is_magic_index(first_card_index + 1) || is_magic_index(first_card_index + 2)) {
						continue;
					}

					// 直接判断，第一张牌，第二牌，和第三张牌存不存在
					if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
							&& tmp_cards_index[first_card_index + 2] != 0) {
						// 使用或运算符的原因是，底层判断是否有多个吃的时候，用的位运算
						eat_type |= eat_type_check[i];
					}
				} else {
					// 如果不是王牌的玩法，直接判断，第一张牌，第二牌，和第三张牌存不存在
					if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
							&& tmp_cards_index[first_card_index + 2] != 0) {
						// 使用或运算符的原因是，底层判断是否有多个吃的时候，用的位运算
						eat_type |= eat_type_check[i];
					}
				}
			}
		}

		return eat_type;
	}

	public int check_chi_ezhou(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		// 如果是风牌或者王牌
		if (get_card_color(cur_card) > 2 || is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		// 构造数据
		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		// 插入扑克
		tmp_cards_index[switch_to_card_index(cur_card)]++;

		int eat_type = GameConstants.WIK_NULL;

		int first_card_index = 0;

		int cur_card_index = switch_to_card_index(cur_card);

		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 轮询判断，可以有多种吃法，首先判断左吃，然后判断中吃，最后判断右吃
		for (int i = 0; i < 3; i++) {

			int card_value = get_card_value(cur_card);

			// 牌值判断，比如左吃时，牌值可以是1,2...7；右吃时，牌值可以是3,4...9
			if ((card_value >= (i + 1)) && (card_value <= 7 + i)) {

				// 吃牌时，第一张牌的实际索引值，按照左吃、中吃、右吃的顺序来
				// 比如左吃时，第一张牌的索引就是当前的牌，中吃的时候就是当前牌的前面一张牌
				// 为什么要用索引呢？因为传进来的数据是索引数据，哈哈哈
				first_card_index = cur_card_index - i;

				// 如果不是王牌的玩法，直接判断，第一张牌，第二牌，和第三张牌存不存在
				if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
						&& tmp_cards_index[first_card_index + 2] != 0) {
					// 使用或运算符的原因是，底层判断是否有多个吃的时候，用的位运算
					eat_type |= eat_type_check[i];
				}
			}
		}

		return eat_type;
	}

	/**
	 * 赣州冲关吃牌判断
	 * 
	 * @param cards_index
	 * @param cur_card
	 * @return
	 */
	public int check_chi_gzcg(int[] cards_index, int cur_card) {
		if (!is_valid_card(cur_card)) { // 如果牌值非法
			return GameConstants.WIK_NULL;
		}

		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}
		// 插入扑克
		tmp_cards_index[switch_to_card_index(cur_card)]++;
		int eat_type = GameConstants.WIK_NULL;
		int first_card_index = 0;
		int cur_card_index = switch_to_card_index(cur_card);
		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		if (get_card_color(cur_card) == 3) { // 如果是风牌
			int card_value = get_card_value(cur_card);
			if (card_value > 4) { // 中发白
				int number = 0;
				for (int i = 0; i < 3; i++) {
					number += tmp_cards_index[switch_to_card_index(0x35) + i] > 0 ? 1 : 0;
				}
				if (number > 2) {
					eat_type |= eat_type_check[0];
				}
			} else { // 东南西北
				int number = 0;
				for (int i = 0; i < 4; i++) {
					number += tmp_cards_index[switch_to_card_index(0x31) + i] > 0 ? 1 : 0;
				}
				if (number > 3) {
					eat_type |= eat_type_check[0];
					eat_type |= eat_type_check[1];
					eat_type |= eat_type_check[2];
				} else if (number > 2) {
					eat_type |= eat_type_check[0];
				}
			}
		} else {
			for (int i = 0; i < 3; i++) {
				int card_value = get_card_value(cur_card);
				if ((card_value >= (i + 1)) && (card_value <= 7 + i)) {
					first_card_index = cur_card_index - i;
					if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
							&& tmp_cards_index[first_card_index + 2] != 0) {
						eat_type |= eat_type_check[i];
					}
				}
			}
		}

		return eat_type;
	}

	public int check_peng_couyise(int[] cards_index, int cur_card, int pi_zi) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		int peng_type = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		int cur_card_index = switch_to_card_index(cur_card);

		// TODO 癞子可以参与碰，但是癞子和痞子不能一起参与碰
		if (cards_index[cur_card_index] >= 2)
			peng_type |= GameConstants.WIK_PENG;

		if (cards_index[cur_card_index] >= 1 && magic_count >= 1)
			peng_type |= GameConstants.WIK_SUO_PENG_1;

		if (magic_count >= 2)
			peng_type |= GameConstants.WIK_SUO_PENG_2;

		return peng_type;
	}

	public int check_peng_with_suo_pai(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		int peng_type = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		int cur_card_index = switch_to_card_index(cur_card);

		if (cards_index[cur_card_index] >= 2)
			peng_type |= GameConstants.WIK_PENG;

		if (cards_index[cur_card_index] >= 1 && magic_count >= 1)
			peng_type |= GameConstants.WIK_SUO_PENG_1;

		if (magic_count >= 2)
			peng_type |= GameConstants.WIK_SUO_PENG_2;

		return peng_type;
	}

	public int check_peng_with_suo_pai_luzhougui(int[] cards_index, int cur_card, boolean can_ruan_peng) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		int peng_type = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		int cur_card_index = switch_to_card_index(cur_card);

		if (cards_index[cur_card_index] >= 2)
			peng_type |= GameConstants.WIK_PENG;

		if (cards_index[cur_card_index] >= 1 && magic_count >= 1 && can_ruan_peng)
			peng_type |= GameConstants.WIK_SUO_PENG_1;

		return peng_type;
	}

	public int check_chi_ignore_magic(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		// 如果是风牌或者王牌
		if (get_card_color(cur_card) > 2 || is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		// 构造数据
		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		// 插入扑克
		tmp_cards_index[switch_to_card_index(cur_card)]++;

		int eat_type = GameConstants.WIK_NULL;

		int first_card_index = 0;

		int cur_card_index = switch_to_card_index(cur_card);

		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 轮询判断，可以有多种吃法，首先判断左吃，然后判断中吃，最后判断右吃
		for (int i = 0; i < 3; i++) {

			int card_value = get_card_value(cur_card);

			// 牌值判断，比如左吃时，牌值可以是1,2...7；右吃时，牌值可以是3,4...9
			if ((card_value >= (i + 1)) && (card_value <= 7 + i)) {

				// 吃牌时，第一张牌的实际索引值，按照左吃、中吃、右吃的顺序来
				// 比如左吃时，第一张牌的索引就是当前的牌，中吃的时候就是当前牌的前面一张牌
				// 为什么要用索引呢？因为传进来的数据是索引数据，哈哈哈
				first_card_index = cur_card_index - i;

				// 如果不是王牌的玩法，直接判断，第一张牌，第二牌，和第三张牌存不存在
				if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
						&& tmp_cards_index[first_card_index + 2] != 0) {
					// 使用或运算符的原因是，底层判断是否有多个吃的时候，用的位运算
					eat_type |= eat_type_check[i];
				}
			}
		}

		return eat_type;
	}

	public int check_chi_tao_jiang(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		// 如果是风牌
		if (get_card_color(cur_card) > 2)
			return GameConstants.WIK_NULL;

		// 构造数据
		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		// 插入扑克
		tmp_cards_index[switch_to_card_index(cur_card)]++;

		int eat_type = GameConstants.WIK_NULL;

		int first_card_index = 0;

		int cur_card_index = switch_to_card_index(cur_card);

		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 轮询判断，可以有多种吃法，首先判断左吃，然后判断中吃，最后判断右吃
		for (int i = 0; i < 3; i++) {

			int card_value = get_card_value(cur_card);

			// 牌值判断，比如左吃时，牌值可以是1,2...7；右吃时，牌值可以是3,4...9
			if ((card_value >= (i + 1)) && (card_value <= 7 + i)) {

				// 吃牌时，第一张牌的实际索引值，按照左吃、中吃、右吃的顺序来
				// 比如左吃时，第一张牌的索引就是当前的牌，中吃的时候就是当前牌的前面一张牌
				// 为什么要用索引呢？因为传进来的数据是索引数据，哈哈哈
				first_card_index = cur_card_index - i;

				// 如果不是王牌的玩法，直接判断，第一张牌，第二牌，和第三张牌存不存在
				if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
						&& tmp_cards_index[first_card_index + 2] != 0) {
					// 使用或运算符的原因是，底层判断是否有多个吃的时候，用的位运算
					eat_type |= eat_type_check[i];
				}
			}
		}

		return eat_type;
	}

	// 吃牌判断
	public int check_chi(int cards_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 过滤判断
		if (cur_card >= 0x31 || is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		// 变量定义
		int excursion[] = new int[] { 0, 1, 2 };
		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 吃牌判断
		int eat_type = 0, first_index = 0;
		int cur_card_index = switch_to_card_index(cur_card);
		for (int i = 0; i < 3; i++) {
			int value_index = cur_card_index % 9;
			if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= 6)) {
				// 吃牌判断
				first_index = cur_card_index - excursion[i];

				if (this._magic_card_count > 0) {
					for (int m = 0; m < this._magic_card_count; m++) {
						// 吃牌不能包含有王霸
						if (get_magic_card_index(m) != GameConstants.MAX_INDEX && get_magic_card_index(m) >= first_index
								&& get_magic_card_index(m) <= first_index + 2)
							continue;

						if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
							continue;
						if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
							continue;
						if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
							continue;

						// 设置类型
						eat_type |= eat_type_check[i];
					}

				} else {
					if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
						continue;
					if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
						continue;
					if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
						continue;

					// 设置类型
					eat_type |= eat_type_check[i];

				}

				// //吃牌不能包含有王霸
				// if( get_magic_card_index() != MJGameConstants.MAX_INDEX &&
				// get_magic_card_index() >= first_index &&
				// get_magic_card_index() <= first_index+2 ) continue;
				//
				// if
				// ((cur_card_index!=first_index)&&(cards_index[first_index]==0))
				// continue;
				// if
				// ((cur_card_index!=(first_index+1))&&(cards_index[first_index+1]==0))
				// continue;
				// if
				// ((cur_card_index!=(first_index+2))&&(cards_index[first_index+2]==0))
				// continue;
				//
				// //设置类型
				// eat_type|=eat_type_check[i];
			}
		}

		return eat_type;
	}

	public int check_chi_shuang_wang(int cards_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card >= 0x31 || is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		int excursion[] = new int[] { 0, 1, 2 };
		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		int eat_type = 0, first_index = 0;
		int cur_card_index = switch_to_card_index(cur_card);
		for (int i = 0; i < 3; i++) {
			int value_index = cur_card_index % 9;
			if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= 6)) {
				first_index = cur_card_index - excursion[i];

				if (_magic_card_count == 1) {
					if (get_magic_card_index(0) != GameConstants.MAX_INDEX && get_magic_card_index(0) >= first_index
							&& get_magic_card_index(0) <= first_index + 2)
						continue;

					if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
						continue;
					if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
						continue;
					if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
						continue;

					eat_type |= eat_type_check[i];
				} else if (_magic_card_count == 2) {
					if (get_magic_card_index(0) != GameConstants.MAX_INDEX && get_magic_card_index(0) >= first_index
							&& get_magic_card_index(0) <= first_index + 2)
						continue;

					if (get_magic_card_index(1) != GameConstants.MAX_INDEX && get_magic_card_index(1) >= first_index
							&& get_magic_card_index(1) <= first_index + 2)
						continue;

					if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
						continue;
					if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
						continue;
					if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
						continue;

					eat_type |= eat_type_check[i];
				} else {
					if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
						continue;
					if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
						continue;
					if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
						continue;

					eat_type |= eat_type_check[i];
				}
			}
		}

		return eat_type;
	}

	public int check_chi_with_suo_pai(int cards_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card >= 0x31 || is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		int excursion[] = new int[] { 0, 1, 2 };

		int eat_type = GameConstants.WIK_NULL;
		int first_index = 0;
		int cur_card_index = switch_to_card_index(cur_card);

		int magic_count = magic_count(cards_index);

		if (magic_count >= 1) {
			for (int i = 0; i < 3; i++) {
				int value_index = cur_card_index % 9;
				if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= 6)) {
					// 吃牌判断
					first_index = cur_card_index - excursion[i];

					if (i == 0) { // 左吃
						if (cards_index[first_index + 2] > 0)
							eat_type |= GameConstants.WIK_SUO_CHI_LEFT_1;
						if (cards_index[first_index + 1] > 0)
							eat_type |= GameConstants.WIK_SUO_CHI_LEFT_2;
					} else if (i == 1) { // 中吃
						if (cards_index[first_index + 1] > 0)
							eat_type |= GameConstants.WIK_SUO_CHI_CENTER_1;
						if (cards_index[first_index - 1] > 0)
							eat_type |= GameConstants.WIK_SUO_CHI_CENTER_2;
					} else if (i == 2) { // 右吃
						if (cards_index[first_index - 1] > 0)
							eat_type |= GameConstants.WIK_SUO_CHI_RIGHT_1;
						if (cards_index[first_index - 2] > 0)
							eat_type |= GameConstants.WIK_SUO_CHI_RIGHT_2;
					}
					if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
						continue;
					if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
						continue;
					if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
						continue;
				}
			}
		}

		return eat_type;
	}

	// 碰牌判断
	public int check_peng(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 过滤判断
		// if ( is_magic_card(cur_card) )
		// return MJGameConstants.WIK_NULL;

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	public int check_peng_lsdy(int card_index[], int lsdy_cards_index[], int cur_card) {
		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = card_index[i] + lsdy_cards_index[i];
		}

		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		return (tmp_cards_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	/**
	 * 鄂州凑一色，碰牌判断，包括：锁碰，即一个正常牌和一个癞子牌去碰牌；正常的碰，即两个正常的牌去碰牌。
	 * 
	 * @param card_index
	 * @param cur_card
	 * @return
	 */
	public int check_suo_peng(int card_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		if (card_index[switch_to_card_index(cur_card)] >= 2)
			return GameConstants.WIK_PENG;
		else
			return GameConstants.WIK_NULL;
	}

	// 碰牌判断
	public int check_peng_hy(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 过滤判断
		if (is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	// 湖南郴州麻将碰牌判断
	public int check_peng_hncz(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		int cbColor = get_card_color(cur_card);
		// 红中不能碰牌
		if (cbColor == 3) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	// 岳阳抓虾子 碰牌判断
	public int check_peng_yyzxz(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		int cbColor = get_card_color(cur_card);// (card&MJGameConstants.LOGIC_MASK_COLOR);

		// 万子不能碰牌
		if (cbColor == 0) {
			return GameConstants.WIK_NULL;
		}

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	/**
	 * 仙桃晃晃 //碰牌判断
	 * 
	 * @param card_index
	 * @param cur_card
	 * @return
	 */
	public int check_peng_xthh(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		if (card_index[switch_to_card_index(cur_card)] >= 2) {
			if (this.is_lai_gen_card(cur_card) == false) {
				// 不是赖根
				return GameConstants.WIK_PENG;
			}
		}
		return GameConstants.WIK_NULL;
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
	public int estimate_gang_card_out_card(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 过滤判断EstimateGangCard
		// if ( is_magic_card(cur_card) )
		// return MJGameConstants.WIK_NULL;

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
	}

	public int estimate_gang_card_out_card_lsdy(int card_index[], int lsdy_cards_index[], int cur_card) {
		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = card_index[i] + lsdy_cards_index[i];
		}

		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		return (tmp_cards_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
	}

	/**
	 * 癞子牌可以参与接杠
	 * 
	 * @param card_index
	 * @param cur_card
	 * @return
	 */
	public int estimate_gang_card_with_suo_pai(int cards_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		int cbActionMask = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		int cur_card_index = switch_to_card_index(cur_card);

		if (cards_index[cur_card_index] >= 3)
			cbActionMask |= GameConstants.WIK_GANG;
		else if (cards_index[cur_card_index] >= 2 && magic_count >= 1)
			cbActionMask |= GameConstants.WIK_SUO_GANG_1;
		else if (cards_index[cur_card_index] >= 1 && magic_count >= 2)
			cbActionMask |= GameConstants.WIK_SUO_GANG_2;
		else if (magic_count >= 3)
			cbActionMask |= GameConstants.WIK_SUO_GANG_3;

		return cbActionMask;
	}

	public int estimate_gang_card_with_suo_pai_luzhougui(int cards_index[], int cur_card, boolean can_ruan_peng) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		int cbActionMask = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		int cur_card_index = switch_to_card_index(cur_card);

		if (cards_index[cur_card_index] >= 3)
			cbActionMask |= GameConstants.WIK_GANG;
		else if (cards_index[cur_card_index] >= 2 && magic_count >= 1 && can_ruan_peng)
			cbActionMask |= GameConstants.WIK_SUO_GANG_1;
		else if (cards_index[cur_card_index] >= 1 && magic_count >= 2 && can_ruan_peng)
			cbActionMask |= GameConstants.WIK_SUO_GANG_2;

		return cbActionMask;
	}

	/**
	 * 杠牌判断 别人打的牌自己能不能杠 红中,王霸不能杠
	 * 
	 * @param card_index
	 *            当前牌型
	 * @param cur_card
	 *            出的牌
	 * @return
	 */
	public int estimate_gang_card_out_card_hy(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 过滤判断EstimateGangCard
		if (is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
	}

	/**
	 * 湖南郴州麻将判断能不能杠
	 * 
	 * @param card_index
	 * @param cur_card
	 * @return
	 */
	public int estimate_gang_card_out_card_hncz(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		int cbColor = get_card_color(cur_card);
		// 红中不能碰牌
		if (cbColor == 3) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
	}

	/**
	 * 仙桃晃晃 //杠牌判断 别人打的牌自己能不能杠
	 * 
	 * @param card_index
	 * @param cur_card
	 * @return
	 */
	public int estimate_gang_card_out_card_xthh(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		if (card_index[switch_to_card_index(cur_card)] == 3) {
			return GameConstants.WIK_DIAN_XIAO;
		} else if (card_index[switch_to_card_index(cur_card)] == 2) {
			if (this.is_lai_gen_card(cur_card)) {
				return GameConstants.WIK_XIAO_CHAO_TIAN;// 小朝天
			}
		}

		return GameConstants.WIK_NULL;
	}

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆）
	 * @return
	 */
	public int analyse_gang_card_jszz(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave) {
		return this.analyse_gang_card_jszz(cards_index, WeaveItem, cbWeaveCount, gangCardResult, check_weave, -1);
	}

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆）
	 * @return
	 */
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
		return this.analyse_gang_card_all(cards_index, WeaveItem, cbWeaveCount, gangCardResult, check_weave, -1);
	}

	/**
	 * 检查有没虾牌。万子就是虾牌
	 * 
	 * @param xiaziAction
	 * @return
	 */
	public int analyse_xia_card_all(int cards_index[], XiaZiCardResult gangCardResult, int xiaziAction) {
		// 设置变量
		int cbActionMask = xiaziAction;

		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			// 如果不是万牌，后面就都不是万牌
			if (get_card_color(switch_to_card_data(i)) != 0) {
				break;
			}
			if (cards_index[i] > 0) {
				cbActionMask = GameConstants.WIK_XIA_ZI_BU;
				int cardData = switch_to_card_data(i);
				for (int j = 0; j < cards_index[i]; j++) {
					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = cardData;
					gangCardResult.isPublic[index] = 0;//
				}

				// gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		return cbActionMask;
	}

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_gang_card_jszz(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int markColor) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
			if (cards_index[i] == 3 && is_da_gen_card(this.switch_to_card_data(i))) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int markColor) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4 && (markColor == -1 || get_card_color(switch_to_card_data(i)) != markColor)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))(乡宁)
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_gang_card_XN(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int markColor) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (i == get_magic_card_index(0))
				continue;
			if (cards_index[i] == 4 && (markColor == -1 || get_card_color(switch_to_card_data(i)) != markColor)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_gang_card_all_yd(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int markColor) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4 && (markColor == -1 || get_card_color(switch_to_card_data(i)) != markColor)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					int card_data = WeaveItem[i].center_card;
					if (card_data > GameConstants.CARD_ESPECIAL_TYPE_BAO) { // 瑞金麻将宝牌
						card_data -= GameConstants.CARD_ESPECIAL_TYPE_BAO;
					}
					if (cards_index[switch_to_card_index(card_data)] == 1) {
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

	public int analyse_gang_card_all_xzdd(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int[] passed_gang_cards, int passed_gang_count) {
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						boolean can_add_gang = true;
						for (int j = 0; j < passed_gang_count; j++) {
							if (WeaveItem[i].center_card == passed_gang_cards[j]) {
								can_add_gang = false;
							}
						}

						if (can_add_gang) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.isPublic[index] = 1;// 明刚
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_card_all_xlch(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[]) {
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					int cIndex = switch_to_card_index(WeaveItem[i].center_card);

					if (cards_index[cIndex] == 1 && cards_abandoned_gang[cIndex] == 0) {
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

	public int analyse_gang_card_all_luzhougui(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int[] passed_gang_cards, int passed_gang_count) {
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (i == switch_to_card_index(0x35))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						boolean can_add_gang = true;
						for (int j = 0; j < passed_gang_count; j++) {
							if (WeaveItem[i].center_card == passed_gang_cards[j]) {
								can_add_gang = false;
							}
						}

						if (can_add_gang) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.isPublic[index] = 1;// 明刚
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_card_all_new_zz(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int markColor) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (is_magic_index(i))
				continue;
			if (cards_index[i] == 4 && (markColor == -1 || get_card_color(switch_to_card_data(i)) != markColor)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_gang_card_hy(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int markColor, int cards_abandoned_gang[]) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (is_magic_card(this.switch_to_card_data(i)))
				continue;
			if (cards_index[i] == 4 && (markColor == -1 || get_card_color(switch_to_card_data(i)) != markColor)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || this.is_magic_index(j) || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
							continue;
						} else {
							if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;// 明杠
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_gang_card_myg(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int markColor, int cards_abandoned_gang[]) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			// if (is_magic_card(this.switch_to_card_data(i)))
			// continue;
			if (cards_index[i] == 4 && (markColor == -1 || get_card_color(switch_to_card_data(i)) != markColor)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
							continue;
						} else {
							if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;// 明杠
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	/**
	 * 亮牌分析 (分析手中的牌是否有亮牌)
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_liang_card(int cards_index[], LiangCardResult liangCardResult, boolean has_sanfeng, boolean has_fei_dan,
			WeaveItem[] weaveItems, int weave_count) {
		boolean has_yao_dan = false;
		boolean has_jiu_dan = false;
		boolean has_xi_dan = false;
		boolean has_san_feng_dan = false;
		boolean has_xuan_feng_dan = false;
		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].type == GameConstants.GANG_TYPE_YAO_DAN) {
				has_yao_dan = true;
			}
			if (weaveItems[i].type == GameConstants.GANG_TYPE_JIU_DAN) {
				has_jiu_dan = true;
			}
			if (weaveItems[i].type == GameConstants.GANG_TYPE_XI_DAN) {
				has_xi_dan = true;
			}
			if (weaveItems[i].type == GameConstants.GANG_TYPE_SAN_FENG_DAN) {
				has_san_feng_dan = true;
			}
			if (weaveItems[i].type == GameConstants.GANG_TYPE_XUAN_FENG_DAN) {
				has_xuan_feng_dan = true;
			}

		}
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;
		// 幺蛋
		if (cards_index[this.switch_to_card_index(0x01)] != 0 && !has_yao_dan) {
			cbActionMask = GameConstants.WIK_LIANG;
			int index = liangCardResult.cbCardCount++;
			liangCardResult.cbCardData[index][0] = 0x01;
			liangCardResult.indexcount[index] = 1;
			liangCardResult.type[index] = GameConstants.GANG_TYPE_YAO_DAN;
		}
		if (cards_index[this.switch_to_card_index(0x11)] != 0 && !has_yao_dan) {
			cbActionMask = GameConstants.WIK_LIANG;
			int index = liangCardResult.cbCardCount++;
			liangCardResult.cbCardData[index][0] = 0x11;
			liangCardResult.indexcount[index] = 1;
			liangCardResult.type[index] = GameConstants.GANG_TYPE_YAO_DAN;
		}
		if (cards_index[this.switch_to_card_index(0x21)] != 0 && !has_yao_dan) {
			cbActionMask = GameConstants.WIK_LIANG;
			int index = liangCardResult.cbCardCount++;
			liangCardResult.cbCardData[index][0] = 0x21;
			liangCardResult.indexcount[index] = 1;
			liangCardResult.type[index] = GameConstants.GANG_TYPE_YAO_DAN;
		}
		// 九蛋
		if (cards_index[this.switch_to_card_index(0x09)] != 0 && !has_jiu_dan) {
			cbActionMask = GameConstants.WIK_LIANG;
			int index = liangCardResult.cbCardCount++;
			liangCardResult.cbCardData[index][0] = 0x09;
			liangCardResult.indexcount[index] = 1;
			liangCardResult.type[index] = GameConstants.GANG_TYPE_JIU_DAN;
		}
		if (cards_index[this.switch_to_card_index(0x19)] != 0 && !has_jiu_dan) {
			cbActionMask = GameConstants.WIK_LIANG;
			int index = liangCardResult.cbCardCount++;
			liangCardResult.cbCardData[index][0] = 0x19;
			liangCardResult.indexcount[index] = 1;
			liangCardResult.type[index] = GameConstants.GANG_TYPE_JIU_DAN;
		}
		if (cards_index[this.switch_to_card_index(0x29)] != 0 && !has_jiu_dan) {
			cbActionMask = GameConstants.WIK_LIANG;
			int index = liangCardResult.cbCardCount++;
			liangCardResult.cbCardData[index][0] = 0x29;
			liangCardResult.indexcount[index] = 1;
			liangCardResult.type[index] = GameConstants.GANG_TYPE_JIU_DAN;
		}
		// 喜蛋
		if (cards_index[this.switch_to_card_index(0x35)] != 0 && cards_index[this.switch_to_card_index(0x36)] != 0
				&& cards_index[this.switch_to_card_index(0x37)] != 0 && !has_xi_dan) {
			cbActionMask = GameConstants.WIK_LIANG;
			int index = liangCardResult.cbCardCount++;
			liangCardResult.cbCardData[index][0] = 0x35;
			liangCardResult.cbCardData[index][1] = 0x36;
			liangCardResult.cbCardData[index][2] = 0x37;
			liangCardResult.indexcount[index] = 3;
			liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;
		}
		// 旋风蛋
		if (!has_sanfeng && !has_xuan_feng_dan) {
			if (cards_index[this.switch_to_card_index(0x31)] != 0 && cards_index[this.switch_to_card_index(0x32)] != 0
					&& cards_index[this.switch_to_card_index(0x33)] != 0 && cards_index[this.switch_to_card_index(0x34)] != 0) {
				cbActionMask = GameConstants.WIK_LIANG;
				int index = liangCardResult.cbCardCount++;
				liangCardResult.cbCardData[index][0] = 0x31;
				liangCardResult.cbCardData[index][1] = 0x32;
				liangCardResult.cbCardData[index][2] = 0x33;
				liangCardResult.cbCardData[index][3] = 0x34;
				liangCardResult.indexcount[index] = 4;
				liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
			}
		}

		// 三风蛋
		if (has_sanfeng && !has_san_feng_dan) {
			int cbfengcount = 0;
			cbfengcount = cards_index[this.switch_to_card_index(0x31)] != 0 ? ++cbfengcount : cbfengcount;
			cbfengcount = cards_index[this.switch_to_card_index(0x32)] != 0 ? ++cbfengcount : cbfengcount;
			cbfengcount = cards_index[this.switch_to_card_index(0x33)] != 0 ? ++cbfengcount : cbfengcount;
			cbfengcount = cards_index[this.switch_to_card_index(0x34)] != 0 ? ++cbfengcount : cbfengcount;
			if (cbfengcount == 3) {
				boolean bdf = false;
				boolean bnf = false;
				boolean bxf = false;
				boolean bbf = false;
				bdf = cards_index[this.switch_to_card_index(0x31)] != 0 ? true : false;
				bnf = cards_index[this.switch_to_card_index(0x32)] != 0 ? true : false;
				bxf = cards_index[this.switch_to_card_index(0x33)] != 0 ? true : false;
				bbf = cards_index[this.switch_to_card_index(0x34)] != 0 ? true : false;
				cbActionMask = GameConstants.WIK_LIANG;
				int index = liangCardResult.cbCardCount++;
				int aa = 0;
				if (bdf) {
					liangCardResult.cbCardData[index][aa++] = 0x31;
				}
				if (bnf) {
					liangCardResult.cbCardData[index][aa++] = 0x32;
				}
				if (bxf) {
					liangCardResult.cbCardData[index][aa++] = 0x33;
				}
				if (bbf) {
					liangCardResult.cbCardData[index][aa++] = 0x34;
				}
				liangCardResult.indexcount[index] = 3;
				liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;

			}
			if (cbfengcount == 4) {
				cbActionMask = GameConstants.WIK_LIANG;
				int index = liangCardResult.cbCardCount++;
				liangCardResult.cbCardData[index][0] = 0x31;
				liangCardResult.cbCardData[index][1] = 0x32;
				liangCardResult.cbCardData[index][2] = 0x33;
				liangCardResult.indexcount[index] = 3;
				liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;

				index = liangCardResult.cbCardCount++;
				liangCardResult.cbCardData[index][0] = 0x31;
				liangCardResult.cbCardData[index][1] = 0x32;
				liangCardResult.cbCardData[index][2] = 0x34;
				liangCardResult.indexcount[index] = 3;
				liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;

				index = liangCardResult.cbCardCount++;
				liangCardResult.cbCardData[index][0] = 0x31;
				liangCardResult.cbCardData[index][1] = 0x33;
				liangCardResult.cbCardData[index][2] = 0x34;
				liangCardResult.indexcount[index] = 3;
				liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;

				index = liangCardResult.cbCardCount++;
				liangCardResult.cbCardData[index][0] = 0x32;
				liangCardResult.cbCardData[index][1] = 0x33;
				liangCardResult.cbCardData[index][2] = 0x34;
				liangCardResult.indexcount[index] = 3;
				liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;
			}

		}
		// 小鸡飞蛋
		if (has_fei_dan) {
			boolean bdf = false;
			boolean bnf = false;
			boolean bxf = false;
			boolean bbf = false;
			boolean bhz = false;
			boolean blf = false;
			boolean bbb = false;
			int xi_count = 0;
			int feng_count = 0;
			bdf = cards_index[this.switch_to_card_index(0x31)] != 0 ? true : false;
			bnf = cards_index[this.switch_to_card_index(0x32)] != 0 ? true : false;
			bxf = cards_index[this.switch_to_card_index(0x33)] != 0 ? true : false;
			bbf = cards_index[this.switch_to_card_index(0x34)] != 0 ? true : false;
			bhz = cards_index[this.switch_to_card_index(0x35)] != 0 ? true : false;
			blf = cards_index[this.switch_to_card_index(0x36)] != 0 ? true : false;
			bbb = cards_index[this.switch_to_card_index(0x37)] != 0 ? true : false;
			xi_count = bhz == true ? ++xi_count : xi_count;
			xi_count = blf == true ? ++xi_count : xi_count;
			xi_count = bbb == true ? ++xi_count : xi_count;
			feng_count = bdf == true ? ++feng_count : feng_count;
			feng_count = bnf == true ? ++feng_count : feng_count;
			feng_count = bxf == true ? ++feng_count : feng_count;
			feng_count = bbf == true ? ++feng_count : feng_count;
			switch (cards_index[this.switch_to_card_index(0x01)]) {
			case 1: {
				// 飞喜蛋
				if (!has_xi_dan) {
					if (xi_count == 2) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						int aa = 0;
						if (bhz) {
							liangCardResult.cbCardData[index][aa++] = 0x35;
						}
						if (blf) {
							liangCardResult.cbCardData[index][aa++] = 0x36;
						}
						if (bbb) {
							liangCardResult.cbCardData[index][aa++] = 0x37;
						}
						liangCardResult.cbCardData[index][aa] = 0x01;
						liangCardResult.indexcount[index] = 3;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;
					}
					if (xi_count == 3) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						liangCardResult.cbCardData[index][0] = 0x35;
						liangCardResult.cbCardData[index][1] = 0x36;
						liangCardResult.cbCardData[index][2] = 0x01;
						liangCardResult.indexcount[index] = 3;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;

						index = liangCardResult.cbCardCount++;
						liangCardResult.cbCardData[index][0] = 0x35;
						liangCardResult.cbCardData[index][1] = 0x37;
						liangCardResult.cbCardData[index][2] = 0x01;
						liangCardResult.indexcount[index] = 3;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;

						index = liangCardResult.cbCardCount++;
						liangCardResult.cbCardData[index][0] = 0x36;
						liangCardResult.cbCardData[index][1] = 0x37;
						liangCardResult.cbCardData[index][2] = 0x01;
						liangCardResult.indexcount[index] = 3;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;
					}
				}

				// 飞三风
				if (has_sanfeng && !has_san_feng_dan) {
					if (feng_count == 2) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						int aa = 0;
						if (bdf) {
							liangCardResult.cbCardData[index][aa++] = 0x31;
						}
						if (bnf) {
							liangCardResult.cbCardData[index][aa++] = 0x32;
						}
						if (bxf) {
							liangCardResult.cbCardData[index][aa++] = 0x33;
						}
						if (bbf) {
							liangCardResult.cbCardData[index][aa++] = 0x34;
						}
						liangCardResult.cbCardData[index][aa] = 0x01;
						liangCardResult.indexcount[index] = 3;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;
					}
				}
				// 飞旋风
				if (feng_count == 3 && !has_xuan_feng_dan) {
					cbActionMask = GameConstants.WIK_LIANG;
					int index = liangCardResult.cbCardCount++;
					int aa = 0;
					if (bdf) {
						liangCardResult.cbCardData[index][aa++] = 0x31;
					}
					if (bnf) {
						liangCardResult.cbCardData[index][aa++] = 0x32;
					}
					if (bxf) {
						liangCardResult.cbCardData[index][aa++] = 0x33;
					}
					if (bbf) {
						liangCardResult.cbCardData[index][aa++] = 0x34;
					}
					liangCardResult.cbCardData[index][aa] = 0x01;
					liangCardResult.indexcount[index] = 4;
					liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
				}
				break;
			}
			case 2: {
				if (!has_xi_dan) {
					if (xi_count == 1) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						if (bhz) {
							liangCardResult.cbCardData[index][0] = 0x35;
						}
						if (blf) {
							liangCardResult.cbCardData[index][0] = 0x36;
						}
						if (bbb) {
							liangCardResult.cbCardData[index][0] = 0x37;
						}
						liangCardResult.cbCardData[index][1] = 0x01;
						liangCardResult.cbCardData[index][2] = 0x01;
						liangCardResult.indexcount[index] = 3;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;
					}
					if (xi_count >= 2) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						if (bhz) {
							liangCardResult.cbCardData[index][0] = 0x35;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.indexcount[index] = 3;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;
						}
						if (blf) {
							liangCardResult.cbCardData[index][0] = 0x36;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.indexcount[index] = 3;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;
						}
						if (bbb) {
							liangCardResult.cbCardData[index][0] = 0x37;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.indexcount[index] = 3;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_XI_DAN;
						}
					}
				}

				// 三风蛋
				if (has_sanfeng && !has_san_feng_dan) {
					if (feng_count == 1) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						if (bdf) {
							liangCardResult.cbCardData[index][0] = 0x31;
						}
						if (bnf) {
							liangCardResult.cbCardData[index][0] = 0x32;
						}
						if (bxf) {
							liangCardResult.cbCardData[index][0] = 0x33;
						}
						if (bbf) {
							liangCardResult.cbCardData[index][0] = 0x34;
						}
						liangCardResult.cbCardData[index][1] = 0x01;
						liangCardResult.cbCardData[index][2] = 0x01;
						liangCardResult.indexcount[index] = 3;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;
					}
					if (feng_count >= 2) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						if (bdf) {
							liangCardResult.cbCardData[index][0] = 0x31;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.indexcount[index] = 3;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;
						}
						if (bnf) {
							liangCardResult.cbCardData[index][0] = 0x32;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.indexcount[index] = 3;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;
						}
						if (bxf) {
							liangCardResult.cbCardData[index][0] = 0x33;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.indexcount[index] = 3;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;
						}
						if (bbf) {
							liangCardResult.cbCardData[index][0] = 0x34;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.indexcount[index] = 3;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_SAN_FENG_DAN;
						}
					}
				}
				// 旋风蛋
				if (feng_count == 2 && !has_xuan_feng_dan) {
					cbActionMask = GameConstants.WIK_LIANG;
					int index = liangCardResult.cbCardCount++;
					int aa = 0;
					if (bdf) {
						liangCardResult.cbCardData[index][aa++] = 0x31;
					}
					if (bnf) {
						liangCardResult.cbCardData[index][aa++] = 0x32;
					}
					if (bxf) {
						liangCardResult.cbCardData[index][aa++] = 0x33;
					}
					if (bbf) {
						liangCardResult.cbCardData[index][aa++] = 0x34;
					}
					liangCardResult.cbCardData[index][2] = 0x01;
					liangCardResult.cbCardData[index][3] = 0x01;
					liangCardResult.indexcount[index] = 4;
					liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
				}
				break;
			}
			case 3: {
				if (!has_xuan_feng_dan) {
					if (feng_count == 1) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						int aa = 0;
						if (bdf) {
							liangCardResult.cbCardData[index][aa++] = 0x31;
						}
						if (bnf) {
							liangCardResult.cbCardData[index][aa++] = 0x32;
						}
						if (bxf) {
							liangCardResult.cbCardData[index][aa++] = 0x33;
						}
						if (bbf) {
							liangCardResult.cbCardData[index][aa++] = 0x34;
						}
						liangCardResult.cbCardData[index][1] = 0x01;
						liangCardResult.cbCardData[index][2] = 0x01;
						liangCardResult.cbCardData[index][3] = 0x01;
						liangCardResult.indexcount[index] = 4;
						liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
					}
					if (feng_count >= 2) {
						cbActionMask = GameConstants.WIK_LIANG;
						int index = liangCardResult.cbCardCount++;
						if (bdf) {
							liangCardResult.cbCardData[index][0] = 0x31;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.cbCardData[index][3] = 0x01;
							liangCardResult.indexcount[index] = 4;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
						}
						if (bnf) {
							liangCardResult.cbCardData[index][0] = 0x32;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.cbCardData[index][3] = 0x01;
							liangCardResult.indexcount[index] = 4;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
						}
						if (bxf) {
							liangCardResult.cbCardData[index][0] = 0x33;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.cbCardData[index][3] = 0x01;
							liangCardResult.indexcount[index] = 4;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
						}
						if (bbf) {
							liangCardResult.cbCardData[index][0] = 0x34;
							liangCardResult.cbCardData[index][1] = 0x01;
							liangCardResult.cbCardData[index][2] = 0x01;
							liangCardResult.cbCardData[index][3] = 0x01;
							liangCardResult.indexcount[index] = 4;
							liangCardResult.type[index] = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
						}
					}
				}
				// 三个幺鸡
				if ((has_sanfeng && !has_san_feng_dan) || !has_xi_dan) {
					cbActionMask = GameConstants.WIK_LIANG;
					int index = liangCardResult.cbCardCount++;
					liangCardResult.cbCardData[index][0] = 0x01;
					liangCardResult.cbCardData[index][1] = 0x01;
					liangCardResult.cbCardData[index][2] = 0x01;
					liangCardResult.indexcount[index] = 3;
					liangCardResult.type[index] = GameConstants.GANG_TYPE_XIAO_JI_FEI_DAN;
				}
				break;
			}
			case 4: {
				break;
			}
			}
		}

		return cbActionMask;
	}

	// 分析特殊杠长春麻将
	public int analyse_teshu_gang_cc(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, BuCardResult bucardesult, boolean bfeidan) {
		// 风牌
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			int icardcount = cards_index[i];
			if (icardcount > 0) {
				// 风蛋
				if (switch_to_card_data(i) == 0x31 || switch_to_card_data(i) == 0x32 || switch_to_card_data(i) == 0x33
						|| switch_to_card_data(i) == 0x34) {
					for (int j = 0; j < cbWeaveCount; j++) {
						if (WeaveItem[j].type == GameConstants.GANG_TYPE_SAN_FENG_DAN || WeaveItem[j].type == GameConstants.GANG_TYPE_XUAN_FENG_DAN
								|| WeaveItem[j].type == GameConstants.GANG_TYPE_XIAO_JI_FEI_DAN) {
							cbActionMask = GameConstants.WIK_BU_ZHNAG;
							int index = bucardesult.cbCardCount++;
							for (int k = 0; k < WeaveItem[j].weave_card.length; k++) {
								bucardesult.cbCardData[index][k] = WeaveItem[j].weave_card[k];
							}
							bucardesult.type[index] = WeaveItem[j].type;
							bucardesult.card[index] = switch_to_card_data(i);
						}
					}
				}
				// 喜蛋
				if (switch_to_card_data(i) == 0x35 || switch_to_card_data(i) == 0x36 || switch_to_card_data(i) == 0x37) {
					for (int j = 0; j < cbWeaveCount; j++) {
						if (WeaveItem[j].type == GameConstants.GANG_TYPE_XI_DAN || WeaveItem[j].type == GameConstants.GANG_TYPE_XIAO_JI_FEI_DAN) {
							cbActionMask = GameConstants.WIK_BU_ZHNAG;
							int index = bucardesult.cbCardCount++;
							for (int k = 0; k < WeaveItem[j].weave_card.length; k++) {
								bucardesult.cbCardData[index][k] = WeaveItem[j].weave_card[k];
							}
							bucardesult.type[index] = WeaveItem[j].type;
							bucardesult.card[index] = switch_to_card_data(i);
						}
					}
				}
				// 幺蛋
				if (switch_to_card_data(i) == 0x01 || switch_to_card_data(i) == 0x21) {
					for (int j = 0; j < cbWeaveCount; j++) {
						if (WeaveItem[j].type == GameConstants.GANG_TYPE_YAO_DAN) {
							cbActionMask = GameConstants.WIK_BU_ZHNAG;
							int index = bucardesult.cbCardCount++;
							for (int k = 0; k < WeaveItem[j].weave_card.length; k++) {
								bucardesult.cbCardData[index][k] = WeaveItem[j].weave_card[k];
							}
							bucardesult.type[index] = WeaveItem[j].type;
							bucardesult.card[index] = switch_to_card_data(i);
						}
					}
				}
				// 九蛋
				if (switch_to_card_data(i) == 0x09 || switch_to_card_data(i) == 0x19 || switch_to_card_data(i) == 0x29) {
					for (int j = 0; j < cbWeaveCount; j++) {
						if (WeaveItem[j].type == GameConstants.GANG_TYPE_JIU_DAN) {
							cbActionMask = GameConstants.WIK_BU_ZHNAG;
							int index = bucardesult.cbCardCount++;
							for (int k = 0; k < WeaveItem[j].weave_card.length; k++) {
								bucardesult.cbCardData[index][k] = WeaveItem[j].weave_card[k];
							}
							bucardesult.type[index] = WeaveItem[j].type;
							bucardesult.card[index] = switch_to_card_data(i);
						}
					}
				}
				// 飞蛋
				if (switch_to_card_data(i) == 0x11) {
					if (bfeidan) {
						for (int j = 0; j < cbWeaveCount; j++) {
							if (WeaveItem[j].type == GameConstants.GANG_TYPE_JIU_DAN || WeaveItem[j].type == GameConstants.GANG_TYPE_SAN_FENG_DAN
									|| WeaveItem[j].type == GameConstants.GANG_TYPE_XUAN_FENG_DAN
									|| WeaveItem[j].type == GameConstants.GANG_TYPE_YAO_DAN || WeaveItem[j].type == GameConstants.GANG_TYPE_XI_DAN) {
								cbActionMask = GameConstants.WIK_BU_ZHNAG;
								int index = bucardesult.cbCardCount++;
								for (int k = 0; k < WeaveItem[j].weave_card.length; k++) {
									bucardesult.cbCardData[index][k] = WeaveItem[j].weave_card[k];
								}
								bucardesult.type[index] = WeaveItem[j].type;
								bucardesult.card[index] = switch_to_card_data(i);
							}
						}
					} else {
						for (int j = 0; j < cbWeaveCount; j++) {
							if (WeaveItem[j].type == GameConstants.GANG_TYPE_YAO_DAN) {
								cbActionMask = GameConstants.WIK_BU_ZHNAG;
								int index = bucardesult.cbCardCount++;
								for (int k = 0; k < WeaveItem[j].weave_card.length; k++) {
									bucardesult.cbCardData[index][k] = WeaveItem[j].weave_card[k];
								}
								bucardesult.type[index] = WeaveItem[j].type;
								bucardesult.card[index] = switch_to_card_data(i);
							}
						}
					}
				}
			}
		}
		return cbActionMask;
	}

	/**
	 * 洛阳杠次杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆）
	 * @param peng_card
	 *            本轮碰的牌不进行杆检测，缓杠
	 * @return
	 */
	public int analyse_gang_card_all_lygc(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int peng_card) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1 && WeaveItem[i].center_card != peng_card) { // 缓杠
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

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆)),还要剔除这个暗杠是不是混子
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆）
	 * @return
	 */
	public int analyse_gang_card_all_xc(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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

	public int analyse_gang_card_cs(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上有3张
		if (cards_index[this.switch_to_card_index(card)] == 3) {
			cbActionMask |= GameConstants.WIK_GANG;
			int index = gangCardResult.cbCardCount++;
			gangCardResult.cbCardData[index] = card;
			gangCardResult.isPublic[index] = 0;// 明刚
			gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			return cbActionMask;
		}

		// 组合杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_GANG;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					break;
				}
			}
		}

		return cbActionMask;
	}

	/**
	 * 仙桃晃晃 //杠牌分析
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 * @return
	 */
	public int analyse_gang_card_all_xthh(MJTable table, int seat_index, int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (table.isIndiscardGang(seat_index, switch_to_card_data(i))) {
				continue;
			}
			if (this.is_magic_index(i))
				continue;

			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_MENG_XIAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.WIK_MENG_XIAO;
			} else if ((cards_index[i] == 3) && (this.is_lai_gen_card(this.switch_to_card_data(i)))) {
				cbActionMask |= GameConstants.WIK_DA_CHAO_TIAN;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.WIK_DA_CHAO_TIAN;

			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {

						if (table.isIndiscardGang(seat_index, WeaveItem[i].center_card)) {
							continue;
						}

						cbActionMask |= GameConstants.WIK_HUI_TOU_XIAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants.WIK_HUI_TOU_XIAO;
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
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_by_card_hand_card_hu_bei(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			int da_dian_card) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (is_magic_index(i))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					break;
				}
			}
		}

		return cbActionMask;
	}

	// 醴陵杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card_ll(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					break;
				}
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card_hong_zhong(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					break;
				}
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_exclude_magic_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
					if (cards_index[j] != 1 || this.is_magic_index(j)) { // 癞子不能杠，少于一张牌也直接过滤
						continue;
					} else {
						if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.isPublic[index] = 1;// 明刚
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
							break;
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_exclude_magic_card_lsdy(int cards_index[], int lsdy_cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i] + lsdy_cards_index[i];
		}

		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (tmp_cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
					if (tmp_cards_index[j] != 1 || this.is_magic_index(j)) { // 癞子不能杠，少于一张牌也直接过滤
						continue;
					} else {
						if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.isPublic[index] = 1;// 明刚
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
							break;
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	// 可以杠癞子
	public int analyse_gang_exclude_magic_card_yd(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			/*
			 * if (this.is_magic_index(i)) continue;
			 */
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
					if (cards_index[j] != 1) { // 癞子不能杠，少于一张牌也直接过滤
						continue;
					} else {
						if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.isPublic[index] = 1;// 明刚
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
							break;
						}
					}
				}
			}
		}

		return cbActionMask;

	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_hand_card_exclude_magic_card(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[]) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue; // 红中不能杠
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || this.is_magic_index(j) || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
							continue;
						} else {
							if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;// 明刚
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	// 遵义有癞子的碰
	public int check_peng_with_laizi_zyzj(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		int peng_type = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		int cur_card_index = switch_to_card_index(cur_card);

		if (cards_index[cur_card_index] >= 2)
			peng_type |= GameConstants.WIK_PENG;

		if (cards_index[cur_card_index] >= 1 && magic_count >= 1)
			peng_type |= GameConstants.WIK_SUO_PENG_1;

		if (magic_count >= 2)
			peng_type |= GameConstants.WIK_SUO_PENG_2;

		return peng_type;
	}

	// 有癞子的点杠，碰杠，遵义捉鸡
	public int estimate_gang_card_with_laizi_zyzj(int cards_index[], int cur_card) {
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		int cbActionMask = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		int cur_card_index = switch_to_card_index(cur_card);

		if (cards_index[cur_card_index] >= 3)
			cbActionMask |= GameConstants.WIK_GANG;
		if (cards_index[cur_card_index] >= 2 && magic_count >= 1)
			cbActionMask |= GameConstants.WIK_SUO_GANG_1;
		if (cards_index[cur_card_index] >= 1 && magic_count >= 2)
			cbActionMask |= GameConstants.WIK_SUO_GANG_2;
		if (magic_count >= 3)
			cbActionMask |= GameConstants.WIK_SUO_GANG_3;

		return cbActionMask;
	}

	// 遵义捉鸡有癞子的杠牌
	public int analyse_gang_with_laizi_zyzj(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int[] passed_gang_cards, int passed_gang_count, boolean hasRuleRuanGang) {
		int cbActionMask = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (is_magic_index(i))
				continue;

			int card = switch_to_card_data(i);

			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 3 && magic_count >= 1) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_1;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 2 && magic_count >= 2) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_2;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 1 && magic_count >= 3) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_3;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_3;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (hasRuleRuanGang) {
			if (check_weave) {
				for (int i = 0; i < cbWeaveCount; i++) {
					boolean can_add_gang = true;

					int wkind = WeaveItem[i].weave_kind;
					int card = WeaveItem[i].center_card;

					if (wkind == GameConstants.WIK_PENG || wkind == GameConstants.WIK_SUO_PENG_1 || wkind == GameConstants.WIK_SUO_PENG_2) {
						for (int k = 0; k < passed_gang_count; k++) {
							if (WeaveItem[i].center_card == passed_gang_cards[k]) {
								can_add_gang = false;
							}
						}
					}

					if (can_add_gang) {
						if (wkind == GameConstants.WIK_PENG || wkind == GameConstants.WIK_SUO_PENG_1 || wkind == GameConstants.WIK_SUO_PENG_2) {
							boolean founded = false;

							for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
								if (cards_index[j] == 0 || is_magic_index(j)) {
									continue;
								} else {
									if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
										founded = true;

										int index = gangCardResult.cbCardCount++;
										gangCardResult.cbCardData[index] = card;
										gangCardResult.realOperateCard[index] = card;
										gangCardResult.isPublic[index] = 1;

										if (wkind == GameConstants.WIK_PENG) {
											cbActionMask |= GameConstants.WIK_GANG;
											gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
										} else if (wkind == GameConstants.WIK_SUO_PENG_1) {
											cbActionMask |= GameConstants.WIK_SUO_GANG_1;
											gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;
										} else if (wkind == GameConstants.WIK_SUO_PENG_2) {
											cbActionMask |= GameConstants.WIK_SUO_GANG_2;
											gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;
										}

										gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
										break;
									}
								}
							}

							if (wkind == GameConstants.WIK_PENG && !founded) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = switch_to_card_data(get_magic_card_index(0));
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_1;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}

							if (wkind == GameConstants.WIK_SUO_PENG_1 && !founded) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = switch_to_card_data(get_magic_card_index(0));
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_2;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}

							if (wkind == GameConstants.WIK_SUO_PENG_2 && !founded) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = switch_to_card_data(get_magic_card_index(0));
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_3;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_3;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}
						}
					}
				}
			}
		} else {
			if (check_weave) {
				for (int i = 0; i < cbWeaveCount; i++) {
					if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
						int card = WeaveItem[i].center_card;

						if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = card;
							gangCardResult.realOperateCard[index] = card;
							gangCardResult.isPublic[index] = 1;

							gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_luhe(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[]) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue; // 红中不能杠
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || this.is_magic_index(j) || cards_abandoned_gang[j] != 0 // 红中不能杠，能接杠但是不杠的牌也过滤掉
								|| !WeaveItem[i].isCan_add_gang()) { // 已过杠
							continue;
						} else {
							if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;// 明刚
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_hong_zhong_all_hu_bei(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int da_dian_card) {
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// 四红中或4癞子不能暗杠
			if (is_magic_index(i) || i == Constants_EZ.HZ_INDEX)
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || is_magic_index(j)) {
							continue;
						} else {
							if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_hong_zhong_all_huangshi(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int da_dian_card) {
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// 四红中或4癞子不能暗杠
			if (is_magic_index(i) || i == Constants_HuangShi.HONG_ZHONG_INDEX || i == Constants_HuangShi.FA_CAI_INDEX)
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || is_magic_index(j)) {
							continue;
						} else {
							if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_hu_bei_with_suo_pai(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int da_dian_card) {
		int cbActionMask = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
				if (is_magic_index(i))
					cbActionMask |= GameConstants.WIK_SUO_GANG_4;
				else
					cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] == 3 && magic_count >= 1) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_1;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] == 2 && magic_count >= 2) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_2;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] == 1 && magic_count >= 3) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_3;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG || WeaveItem[i].weave_kind == GameConstants.WIK_SUO_PENG_1
						|| WeaveItem[i].weave_kind == GameConstants.WIK_SUO_PENG_2) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || is_magic_index(j)) {
							continue;
						} else {
							if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
								if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG)
									cbActionMask |= GameConstants.WIK_GANG;
								else if (WeaveItem[i].weave_kind == GameConstants.WIK_SUO_PENG_1)
									cbActionMask |= GameConstants.WIK_SUO_GANG_1;
								else if (WeaveItem[i].weave_kind == GameConstants.WIK_SUO_PENG_2)
									cbActionMask |= GameConstants.WIK_SUO_GANG_2;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_with_suo_pai(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int[] passed_gang_cards, int passed_gang_count, boolean hasRuleRuanGang) {
		int cbActionMask = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);
		int magicCard = switch_to_card_data(get_magic_card_index(0));

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (is_magic_index(i))
				continue;

			int card = switch_to_card_data(i);

			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 3 && magic_count >= 1) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_1;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 2 && magic_count >= 2) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_2;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 1 && magic_count >= 3) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_3;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_3;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (hasRuleRuanGang) {
			if (check_weave) {
				for (int i = 0; i < cbWeaveCount; i++) {
					boolean can_add_gang = true;

					int wkind = WeaveItem[i].weave_kind;
					int card = WeaveItem[i].center_card;

					if (wkind == GameConstants.WIK_PENG || wkind == GameConstants.WIK_SUO_PENG_1 || wkind == GameConstants.WIK_SUO_PENG_2) {
						for (int k = 0; k < passed_gang_count; k++) {
							if (WeaveItem[i].center_card == passed_gang_cards[k]) {
								can_add_gang = false;
							}
						}
					}

					if (can_add_gang) {
						if (wkind == GameConstants.WIK_PENG || wkind == GameConstants.WIK_SUO_PENG_1 || wkind == GameConstants.WIK_SUO_PENG_2) {
							boolean founded = false;

							for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
								if (cards_index[j] == 0 || is_magic_index(j)) {
									continue;
								} else {
									if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
										founded = true;

										int index = gangCardResult.cbCardCount++;
										gangCardResult.cbCardData[index] = card;
										gangCardResult.realOperateCard[index] = card;
										gangCardResult.isPublic[index] = 1;

										if (wkind == GameConstants.WIK_PENG) {
											cbActionMask |= GameConstants.WIK_GANG;
											gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
										} else if (wkind == GameConstants.WIK_SUO_PENG_1) {
											cbActionMask |= GameConstants.WIK_SUO_GANG_1;
											gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;
										} else if (wkind == GameConstants.WIK_SUO_PENG_2) {
											cbActionMask |= GameConstants.WIK_SUO_GANG_2;
											gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;
										}

										gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
										break;
									}
								}
							}

							if (wkind == GameConstants.WIK_PENG && !founded) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = switch_to_card_data(get_magic_card_index(0));
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_1;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}

							if (wkind == GameConstants.WIK_SUO_PENG_1 && !founded) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = switch_to_card_data(get_magic_card_index(0));
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_2;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}

							if (wkind == GameConstants.WIK_SUO_PENG_2 && !founded) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = switch_to_card_data(get_magic_card_index(0));
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_3;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_3;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}
						}
					}
				}
			}
		} else {
			if (check_weave) {
				for (int i = 0; i < cbWeaveCount; i++) {
					if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
						int card = WeaveItem[i].center_card;

						if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = card;
							gangCardResult.realOperateCard[index] = card;
							gangCardResult.isPublic[index] = 1;

							gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_with_suo_pai_luzhougui(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int[] passed_gang_cards, int passed_gang_count, boolean hasRuleRuanGang, boolean can_ruan_peng) {
		int cbActionMask = GameConstants.WIK_NULL;

		int magic_count = magic_count(cards_index);
		int magicCard = switch_to_card_data(get_magic_card_index(0));

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (i == switch_to_card_index(0x35))
				continue;

			int card = switch_to_card_data(i);

			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 3 && magic_count >= 1 && can_ruan_peng) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_1;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 2 && magic_count >= 2 && can_ruan_peng) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_2;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			} else if (cards_index[i] >= 1 && magic_count >= 3 && can_ruan_peng) {
				cbActionMask |= GameConstants.WIK_SUO_GANG_3;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = card;
				gangCardResult.realOperateCard[index] = card;
				gangCardResult.isPublic[index] = 0;
				gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_3;
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (hasRuleRuanGang) {
			if (check_weave) {
				for (int i = 0; i < cbWeaveCount; i++) {
					boolean can_add_gang = true;

					int wkind = WeaveItem[i].weave_kind;
					int card = WeaveItem[i].center_card;

					if (wkind == GameConstants.WIK_PENG || wkind == GameConstants.WIK_SUO_PENG_1 || wkind == GameConstants.WIK_SUO_PENG_2) {
						for (int k = 0; k < passed_gang_count; k++) {
							if (WeaveItem[i].center_card == passed_gang_cards[k]) {
								can_add_gang = false;
							}
						}
					}

					if (can_add_gang) {
						if (wkind == GameConstants.WIK_PENG || wkind == GameConstants.WIK_SUO_PENG_1 || wkind == GameConstants.WIK_SUO_PENG_2) {
							boolean founded = false;

							for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
								if (cards_index[j] == 0 || is_magic_index(j)) {
									continue;
								} else {
									if (WeaveItem[i].center_card == this.switch_to_card_data(j)) {
										founded = true;

										int index = gangCardResult.cbCardCount++;
										gangCardResult.cbCardData[index] = card;
										gangCardResult.realOperateCard[index] = card;
										gangCardResult.isPublic[index] = 1;

										if (wkind == GameConstants.WIK_PENG) {
											cbActionMask |= GameConstants.WIK_GANG;
											gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
										} else if (wkind == GameConstants.WIK_SUO_PENG_1 && can_ruan_peng) {
											cbActionMask |= GameConstants.WIK_SUO_GANG_1;
											gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;
										} else if (wkind == GameConstants.WIK_SUO_PENG_2 && can_ruan_peng) {
											cbActionMask |= GameConstants.WIK_SUO_GANG_2;
											gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;
										}

										gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
										break;
									}
								}
							}

							if (wkind == GameConstants.WIK_PENG && !founded && can_ruan_peng) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = card;
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_1;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_1;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}

							if (wkind == GameConstants.WIK_SUO_PENG_1 && !founded && can_ruan_peng) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = card;
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_2;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_2;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}

							if (wkind == GameConstants.WIK_SUO_PENG_2 && !founded && can_ruan_peng) {
								if (magic_count > 0) {
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = card;
									gangCardResult.realOperateCard[index] = card;
									gangCardResult.isPublic[index] = 1;

									cbActionMask |= GameConstants.WIK_SUO_GANG_3;
									gangCardResult.detailActionType[index] = GameConstants.WIK_SUO_GANG_3;

									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								}
							}
						}
					}
				}
			}
		} else {
			if (check_weave) {
				for (int i = 0; i < cbWeaveCount; i++) {
					if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
						if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
							cbActionMask |= GameConstants.WIK_GANG;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.detailActionType[index] = GameConstants.WIK_GANG;
							gangCardResult.isPublic[index] = 1;
							gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	/**
	 * 晃晃麻将 //笑牌分析 自己摸起来的牌能不能杠
	 * 
	 * @param cards_index
	 * @param card
	 * @param lai_gen
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @return
	 */
	public int analyse_xiao_by_dispacth_card(MJTable table, int seat_index, int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上有没有3个赖根
		if (cards_index[this.switch_to_card_index(_lai_gen)] == 3) {
			if (!table.isIndiscardGang(seat_index, _lai_gen)) {
				cbActionMask |= GameConstants.WIK_DA_CHAO_TIAN;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _lai_gen;
				gangCardResult.isPublic[index] = 0;// 大朝天
				gangCardResult.type[index] = GameConstants.WIK_DA_CHAO_TIAN;
			}
		}

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (table.isIndiscardGang(seat_index, switch_to_card_data(i))) {
				continue;
			}
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_MENG_XIAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 闷笑
				gangCardResult.type[index] = GameConstants.WIK_MENG_XIAO;
			}
		}

		// 组合杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (table.isIndiscardGang(seat_index, WeaveItem[i].center_card)) {
					continue;
				}
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_HUI_TOU_XIAO;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 回头笑
					gangCardResult.type[index] = GameConstants.WIK_HUI_TOU_XIAO;
					break;
				}
			}
		}

		return cbActionMask;
	}

	// 吃胡分析
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, ChiHuRight chiHuRight,
			boolean has_feng) {
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 设置变量
		analyseItemArray.clear();
		chiHuRight.set_empty();

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		/*
		 * // 特殊番型
		 */
		// 七小对牌
		long qxd = is_qi_xiao_dui(cards_index, weaveItem, cbWeaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL)
			chiHuRight.opr_or(qxd);

		// 带幺
		// if( IsDaiYao(pAnalyseItem) )
		// ChiHuRight |= CHR_DAI_YAO;
		// 将将胡
		if (is_jiangjiang_hu(cards_index, weaveItem, cbWeaveCount, cur_card))
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);

		if (!chiHuRight.is_empty())
			cbChiHuKind = GameConstants.WIK_CHI_HU;

		// 插入扑克
		if (cur_card != 0)
			cbCardIndexTemp[switch_to_card_index(cur_card)]++;

		// 分析扑克
		analyse_card(cbCardIndexTemp, weaveItem, weaveItem.length, analyseItemArray, has_feng);

		// 胡牌分析
		if (analyseItemArray.size() > 0) {
			//
			cbChiHuKind = GameConstants.WIK_CHI_HU;

			// 牌型分析
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);

				/*
				 * // 判断番型
				 */
				// 碰碰和
				if (is_pengpeng_hu(analyseItem))
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);

			}
		}

		// 素番
		if (cbChiHuKind == GameConstants.WIK_CHI_HU && chiHuRight.is_empty())
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		if (cbChiHuKind == GameConstants.WIK_CHI_HU) {
			// 清一色牌
			if (is_qing_yi_se(cards_index, weaveItem, cbWeaveCount, cur_card))
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		return cbChiHuKind;
	}

	// 是否听牌
	// public boolean is_ting_card( int cards_index[], WeaveItem weaveItem[]
	// ,int cbWeaveCount )
	// {
	// int handcount = this.get_card_count_by_index(cards_index);
	// if(handcount == 1){
	// //全求人
	// return true;
	// }
	// //复制数据
	// int cbCardIndexTemp[] = new int[MJGameConstants.MAX_INDEX];
	// for(int i=0; i < MJGameConstants.MAX_INDEX; i++){
	// cbCardIndexTemp[i] = cards_index[i];
	// }
	//
	// ChiHuRight chr = new ChiHuRight();
	// for( int i = 0; i < MJGameConstants.MAX_INDEX-7; i++ )
	// {
	// int cbCurrentCard = switch_to_card_data( i );
	// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card(
	// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr ) )
	// return true;
	// }
	// return false;
	// }

	// //株洲是否听牌
	// public boolean is_zhuzhou_ting_card(int cards_index[], WeaveItem
	// weaveItem[], int cbWeaveCount) {
	// int handcount = this.get_card_count_by_index(cards_index);
	// if (handcount == 1) {
	// // 全求人
	// return true;
	// }
	//
	// // 复制数据
	// int cbCardIndexTemp[] = new int[MJGameConstants.MAX_INDEX];
	// for (int i = 0; i < MJGameConstants.MAX_INDEX; i++) {
	// cbCardIndexTemp[i] = cards_index[i];
	// }
	//
	// ChiHuRight chr = new ChiHuRight();
	// for (int i = 0; i < MJGameConstants.MAX_INDEX - 7; i++) {
	// int cbCurrentCard = switch_to_card_data(i);
	// if (MJGameConstants.WIK_CHI_HU ==
	// analyse_chi_hu_card_zhuzhou(cbCardIndexTemp, weaveItem, cbWeaveCount,
	// cbCurrentCard, chr, true))
	// return true;
	// }
	// return false;
	// }

	// // 解析吃胡 株洲玩法
	// public int analyse_chi_hu_card_zhuzhou(int cards_index[], WeaveItem
	// weaveItem[], int weaveCount, int cur_card, ChiHuRight chiHuRight, boolean
	// bSelfSendCard) {
	// // 变量定义
	// // int cbChiHuKind = MJGameConstants.WIK_NULL;
	// // 设置变量
	// List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
	// chiHuRight.set_empty();
	//
	// // 构造扑克
	// int cbCardIndexTemp[] = new int[MJGameConstants.MAX_INDEX];
	// for (int i = 0; i < MJGameConstants.MAX_INDEX; i++) {
	// cbCardIndexTemp[i] = cards_index[i];
	// }
	// // cbCurrentCard一定不为0 !!!!!!!!!
	// if (cur_card == 0)
	// return MJGameConstants.WIK_NULL;
	//
	// // 插入扑克
	// cbCardIndexTemp[switch_to_card_index(cur_card)]++;
	//
	// // 分析扑克
	// boolean bValue = analyse_card(cbCardIndexTemp, weaveItem, weaveCount,
	// analyseItemArray);
	// if (bSelfSendCard) {
	// chiHuRight.opr_or(MJGameConstants.CHR_ZI_MO);
	// } else {
	// chiHuRight.opr_or(MJGameConstants.CHR_SHU_FAN);
	// }
	//
	// boolean hu = false;
	//
	// // 胡牌分析
	// if (bValue == false) {
	// // 不能胡的情况,有可能是七小对
	// // 七小对牌 豪华七小对
	// long qxd = is_qi_xiao_dui(cards_index, weaveItem, weaveCount, cur_card);
	// if (qxd != MJGameConstants.WIK_NULL) {
	// chiHuRight.opr_or(qxd);
	// hu = true;
	// return MJGameConstants.WIK_CHI_HU;
	// }
	//
	//
	// if (hu == false) {
	// chiHuRight.set_empty();
	// return MJGameConstants.WIK_NULL;
	// }
	// }
	//
	// /*
	// * // 特殊番型
	// */
	//
	// // 将将胡
	// if (is_jiangjiang_hu(cards_index, weaveItem, weaveCount, cur_card)) {
	//// chiHuRight.opr_or(MJGameConstants.CHR_HUNAN_JIANGJIANG_HU);
	// hu = true;
	// }
	// // 全求人
	// if (is_dan_diao(cards_index, cur_card)) {// weaveCount == 4 &&
	//// chiHuRight.opr_or(MJGameConstants.CHR_QUAN_QIU_REN);
	// hu = true;
	// }
	//
	// // 清一色牌
	// if (is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
	// chiHuRight.opr_or(MJGameConstants.CHR_QING_YI_SE);
	// hu = true;
	// }
	//
	// // 牌型分析
	// for (int i = 0; i < analyseItemArray.size(); i++) {
	// // 变量定义
	// AnalyseItem analyseItem = analyseItemArray.get(i);
	// /*
	// * // 判断番型
	// */
	// // 碰碰和
	// if (is_pengpeng_hu(analyseItem)) {
	// chiHuRight.opr_or(MJGameConstants.CHR_PENGPENG_HU);
	// hu = true;
	// }
	//
	// }
	//
	// if (hu == true) {
	// // 有大胡
	// return MJGameConstants.WIK_CHI_HU;
	// }
	//
	// // 胡牌分析 有没有258
	// for (int i = 0; i < analyseItemArray.size(); i++) {
	// // 变量定义
	// AnalyseItem pAnalyseItem = analyseItemArray.get(i);
	// int cbCardValue = get_card_value(pAnalyseItem.cbCardEye);
	// if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8) {
	// continue;
	// }
	//
	// hu = true;
	// return MJGameConstants.WIK_CHI_HU;
	// }
	// chiHuRight.set_empty();
	// return MJGameConstants.WIK_NULL;
	// }
	//

	// 解析吃胡 长沙玩法
	// public int analyse_chi_hu_card_cs(int cards_index[], WeaveItem
	// weaveItem[], int weaveCount, int cur_card,
	// ChiHuRight chiHuRight, boolean bSelfSendCard) {
	// // 变量定义
	// //int cbChiHuKind = MJGameConstants.WIK_NULL;
	// // 设置变量
	// List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
	// chiHuRight.set_empty();
	//
	// // 构造扑克
	// int cbCardIndexTemp[] = new int[MJGameConstants.MAX_INDEX];
	// for (int i = 0; i < MJGameConstants.MAX_INDEX; i++) {
	// cbCardIndexTemp[i] = cards_index[i];
	// }
	// // cbCurrentCard一定不为0 !!!!!!!!!
	// if (cur_card == 0)
	// return MJGameConstants.WIK_NULL;
	//
	// // 插入扑克
	// cbCardIndexTemp[switch_to_card_index(cur_card)]++;
	//
	//
	// // 分析扑克
	// boolean bValue = analyse_card(cbCardIndexTemp, weaveItem, weaveCount,
	// analyseItemArray);
	// if(bSelfSendCard){
	// chiHuRight.opr_or(MJGameConstants.CHR_ZI_MO);
	// }else{
	// chiHuRight.opr_or(MJGameConstants.CHR_SHU_FAN);
	// }
	//
	// boolean hu=false;
	//
	// // 胡牌分析
	// if (bValue==false) {
	// //不能胡的情况,有可能是七小对
	// // 七小对牌 豪华七小对
	// long qxd = is_qi_xiao_dui(cards_index,weaveItem,weaveCount,cur_card);
	// if(qxd!=MJGameConstants.WIK_NULL ) {
	// chiHuRight.opr_or(qxd);
	// hu = true;
	// }
	//
	// //将将胡
	// if (is_jiangjiang_hu(cards_index, weaveItem, weaveCount, cur_card)) {
	// chiHuRight.opr_or(MJGameConstants.CHR_HUNAN_JIANGJIANG_HU);
	// hu = true;
	// }
	//
	// if(hu==false){
	// chiHuRight.set_empty();
	// return MJGameConstants.WIK_NULL;
	// }
	// }
	//
	// /*
	// * // 特殊番型
	// */
	//
	// //全求人
	// if (is_dan_diao(cards_index, cur_card)) {//weaveCount == 4 &&
	// chiHuRight.opr_or(MJGameConstants.CHR_QUAN_QIU_REN);
	// hu = true;
	// }
	//
	// // 清一色牌
	// if (is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)){
	// chiHuRight.opr_or(MJGameConstants.CHR_QING_YI_SE);
	// hu = true;
	// }
	//
	//
	// // 牌型分析
	// for (int i = 0; i < analyseItemArray.size(); i++) {
	// // 变量定义
	// AnalyseItem analyseItem = analyseItemArray.get(i);
	// /*
	// * // 判断番型
	// */
	// // 碰碰和
	// if (is_pengpeng_hu(analyseItem)){
	// chiHuRight.opr_or(MJGameConstants.CHR_PENGPENG_HU);
	// hu = true;
	// }
	//
	// }
	//
	// if (hu == true){
	// //有大胡
	// return MJGameConstants.WIK_CHI_HU;
	// }
	//
	// // 胡牌分析 有没有258
	// for (int i=0;i<analyseItemArray.size();i++)
	// {
	// //变量定义
	// AnalyseItem pAnalyseItem=analyseItemArray.get(i);
	// int cbCardValue = get_card_value(pAnalyseItem.cbCardEye);
	// if( cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8 )
	// {
	// continue;
	// }
	//
	// hu = true;
	// return MJGameConstants.WIK_CHI_HU;
	// }
	// chiHuRight.set_empty();
	// return MJGameConstants.WIK_NULL;
	// }

	// 是否花猪
	public boolean is_hua_zhu(int cards_index[], WeaveItem weaveItem[]) {
		int cbColor[] = new int[] { 0, 0, 0 };
		int cbWeaveCount = weaveItem.length;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				int cbCardColor = get_card_color(switch_to_card_data(i));
				cbColor[cbCardColor]++;

				i = (i / 9 + 1) * 9 - 1;
			}
		}
		for (int i = 0; i < cbWeaveCount; i++) {
			int cbCardColor = get_card_color(switch_to_card_data(weaveItem[i].center_card));
			cbColor[cbCardColor]++;
		}
		// 缺一门就不是花猪
		for (int i = 0; i < cbColor.length; i++)
			if (cbColor[i] == 0)
				return false;

		return true;

	}

	// 是否单吊
	public boolean is_dan_diao(int cards_index[], int cur_card) {
		// 单牌数目
		// int cbReplaceCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 王牌过滤
			// if( i == get_magic_card_index() ) continue;

			// 单牌统计
			if (cbCardCount == 2) {
				bDuizi = true;
			}
			nTaltal += cbCardCount;
		}

		if (bDuizi && nTaltal == 2) {
			return true;
		}
		return false;

	}

	// 是否单吊
	public boolean is_dan_diao_lai(int cards_index[], int cur_card) {
		// 单牌数目
		// int cbReplaceCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		boolean has_lai = false;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 王牌过滤
			if (is_magic_index(i) && cbCardCount > 0) {
				has_lai = true;
			}

			// 单牌统计
			if (cbCardCount == 2) {
				bDuizi = true;
			}
			nTaltal += cbCardCount;
		}

		if (bDuizi && nTaltal == 2) {
			return true;
		} else if (has_lai && nTaltal == 2) {
			return true;
		}
		return false;

	}

	// 是否单吊
	public boolean is_dan_diao＿hy(int cards_index[], int cur_card) {
		// 单牌数目
		// int cbReplaceCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		boolean has_magic_card_1 = false;
		boolean has_magic_card_2 = false;
		boolean has_258 = false;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			int card = switch_to_card_data(i);

			// 单牌统计
			if (this.is_magic_card(card) && cbCardCount == 2) {
				has_magic_card_2 = true;
			} else if (this.is_magic_card(card) && cbCardCount == 1) {
				has_magic_card_1 = true;
			} else if (cbCardCount == 2) {
				bDuizi = true;
			}
			int cbCardValue = get_card_value(card);
			if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
				has_258 = true;
			}
			nTaltal += cbCardCount;
		}

		if (((has_magic_card_2) || (has_258 && has_magic_card_1) || (has_258 && bDuizi)) && nTaltal == 2) {
			return true;
		}
		return false;

	}

	/*
	 * public static void main(String[] args) { int[] ww = new
	 * int[GameConstants.MAX_INDEX];
	 * 
	 * MJGameLogic l = new MJGameLogic(); for (int i = 0; i < ww.length; i++) {
	 * if(i == l.switch_to_card_index(0x35)){ ww[i]++; }
	 * System.out.println("i="+i +":"+ww[i] ); }
	 * l.add_magic_card_index(l.switch_to_card_index(0x35));
	 * System.out.println(l.is_dan_diao＿hy(ww,0x35)); }
	 */

	public int is_feng_is_se(int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int cur_card) {
		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			int index = switch_to_card_index(weaveItem.center_card);
			if (index < GameConstants.MAX_ZI) {
				return GameConstants.WIK_NULL;
			}
		}

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cbCardIndexTemp[i] > 0) {
				return GameConstants.WIK_NULL;
			}
		}

		if (is_da_dui_hu_ShaoYang(cards_index, weaveItems, cbWeaveCount, cur_card) == GameConstants.WIK_NULL) {
			return GameConstants.WIK_NULL;
		}

		return GameConstants.CHR_HUNAN_FENG_YI_SE;
	}

	public int is_men_qing(WeaveItem weaveItems[], int cbWeaveCount) {

		if (cbWeaveCount == 0) {
			return GameConstants.CHR_HUNAN_MEN_QING;
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			if (weaveItem.weave_kind != GameConstants.WIK_GANG) {
				return GameConstants.WIK_NULL;
			}
			if (weaveItem.public_card == 1) {
				return GameConstants.WIK_NULL;
			}
		}

		return GameConstants.CHR_HUNAN_MEN_QING;
	}

	public int is_men_qing_hainan(WeaveItem weaveItems[], int cbWeaveCount) {

		if (cbWeaveCount == 0) {
			return GameConstants.CHR_HUNAN_MEN_QING;
		}
		return GameConstants.WIK_NULL;
	}

	public boolean is_men_qing_b(WeaveItem weaveItems[], int cbWeaveCount) {
		if (cbWeaveCount == 0) {
			return true;
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			if (weaveItem.weave_kind != GameConstants.WIK_GANG) {
				return false;
			}
			if (weaveItem.public_card == 1) {
				return false;
			}
		}

		return true;
	}

	// 邵阳 - 大对胡
	public int is_da_dui_hu_ShaoYang(int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int cur_card) {
		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			if (weaveItem.weave_kind != GameConstants.WIK_PENG && weaveItem.weave_kind != GameConstants.WIK_GANG) {
				return GameConstants.WIK_NULL;
			}
		}
		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int eye = 0;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			// 单牌统计
			if (cbCardCount == 3) {
				continue;
			} else {
				if (cbCardCount == 2) {
					eye++;
				}
				if (cbCardCount == 1) {
					return GameConstants.WIK_NULL;
				}
				if (cbCardCount == 4) {
					return GameConstants.WIK_NULL;
				}
			}
		}

		if (eye != 1) {
			return GameConstants.WIK_NULL;
		}
		return GameConstants.CHR_HUNAN_DA_DUI_HU;
	}

	public boolean check_hubei_ying_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return false;

		return true;
	}

	public boolean check_hubei_ying_qi_xiao_dui_qi_shou(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return false;

		return true;
	}

	public boolean check_hubei_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return false;
			}
		} else {
			if (cbReplaceCount > 0)
				return false;
		}

		return true;
	}

	public boolean check_hubei_qi_xiao_dui_qi_shou(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int magic_card_count = get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return false;
			}
		} else {
			if (cbReplaceCount > 0)
				return false;
		}

		return true;
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

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
			// 王牌过滤
			// if( i == get_magic_card_index() ) continue;
			//
			// //单牌统计
			// if( cbCardCount == 1 || cbCardCount == 3 ) cbReplaceCount++;
			//
			// if (cbCardCount == 4 )
			// {
			// nGenCount++;
			// }
		}

		// 王牌不够
		if (this._magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
			// //王牌不够
			// if( get_magic_card_index() != MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > cbCardIndexTemp[get_magic_card_index()] ||
			// get_magic_card_index() == MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > 0 )
			// return MJGameConstants.WIK_NULL;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				// 双豪华七小对
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}

	}

	// 七小对，遵义捉鸡
	public int is_qi_xiao_dui_zyzj(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

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
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			return GameConstants_ZYZJ.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_ZYZJ.CHR_XIAO_QI_DUI;
		}

	}

	/**
	 * 双鬼或者上下王的七小对判断，比较特殊，之前的is_qi_xiao_dui方法里，_magic_card_count=2的时候，单牌会计算两次
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param cur_card
	 * @return
	 */
	public int sg_is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_magic_card_count == 1) {
				if (i == get_magic_card_index(0))
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			} else if (_magic_card_count == 2) {
				if (i == get_magic_card_index(0) || i == get_magic_card_index(1))
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
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

		if (_magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}

	}

	public boolean check_hubei_hun_qi(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		if (weave_count != 0)
			return false;

		int cbReplaceCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (_magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					if (i == get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;
			}
		}

		if (_magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cards_index[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return false;
			}
		} else {
			if (cbReplaceCount > 0)
				return false;
		}

		return true;
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。(衡阳)
	public int is_qi_xiao_dui_hy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;
		int laiZi_count = 0;
		int three_num = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;
		int[] cards = new int[14];
		int hand_card_count = switch_to_cards_data(cbCardIndexTemp, cards);
		if (hand_card_count != 14)
			return GameConstants.WIK_NULL;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (this._magic_card_count > 0) {
				boolean flag = false;
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == get_magic_card_index(m)) {
						// 王牌数量统计
						laiZi_count += cbCardCount;
						flag = true;
						continue;
					}
				}
				if (flag) {
					continue;
				}
			}
			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 3) {
				three_num++;
			}

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (this._magic_card_count > 0) {
			if (cbReplaceCount > laiZi_count) {
				return GameConstants.WIK_NULL;
			}
			// 四张牌数量
			nGenCount += three_num;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				// 双豪华七小对
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}

	}

	public int is_qi_xiao_dui_henan_xc(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0 || this.get_card_count_by_index(cards_index) < 12)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_ZI_FENG];
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		// int cbCurrentIndex = switch_to_card_index(cur_card);
		// cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			if (cbCardCount == 0) {
				continue;
			}
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
			// 王牌过滤
			// if( i == get_magic_card_index() ) continue;
			//
			// //单牌统计
			// if( cbCardCount == 1 || cbCardCount == 3 ) cbReplaceCount++;
			//
			// if (cbCardCount == 4 )
			// {
			// nGenCount++;
			// }
		}

		// 王牌不够
		if (this._magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
			// //王牌不够
			// if( get_magic_card_index() != MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > cbCardIndexTemp[get_magic_card_index()] ||
			// get_magic_card_index() == MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > 0 )
			// return MJGameConstants.WIK_NULL;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount > 1) {
				// 双豪华七小对
				return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
			}
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}

	}

	public int is_qi_xiao_dui_he_nan_zhou_kou(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

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
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		/**
		 * if (nGenCount > 0) { if (nGenCount > 1) { // 双豪华七小对 return
		 * GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI; } return
		 * GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI; } else { return
		 * GameConstants.CHR_HENAN_QI_XIAO_DUI; }
		 **/

		return GameConstants.CHR_HENAN_QI_XIAO_DUI;

	}

	/////////////////////////////////// 河南信阳
	// 分析扑克
	public boolean analyse_card_henanxy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, List<AnalyseItem> analyseItemArray,
			boolean has_feng) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 效验参数
			if (((cbCardCount == 2) && (cbWeaveCount == 4)) == false) {
				return false;
			}
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2 || (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								if (switch_to_card_index(cur_card) == i + 1) {
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i + 1);
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_CENTER;
								} else if (switch_to_card_index(cur_card) == i + 2) {
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i + 2);
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_RIGHT;
								} else {
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
								}

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
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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

	// 门清
	public int is_men_qing_henanxy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount == 0)
			return GameConstants.CHR_HENAN_XY_MENQING;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG
					|| (weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card != 0)) {

				return GameConstants.WIK_NULL;
			}
		}

		return GameConstants.CHR_HENAN_XY_MENQING;
	}

	// 门清
	public int is_men_qing_henan_sq(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount == 0)
			return GameConstants.CHR_HENAN_XY_MENQING;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG || weaveItem[i].weave_kind == GameConstants.WIK_GANG) {

				return GameConstants.WIK_NULL;
			}
		}

		return GameConstants.CHR_HENAN_XY_MENQING;
	}

	// 门清
	public int is_men_qing_henan_hy258(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount == 0)
			return GameConstants_HY258.CHR_HUNAN_HY258_MEN_QING;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT || weaveItem[i].weave_kind == GameConstants.WIK_PENG
					|| weaveItem[i].weave_kind == GameConstants.WIK_BU_ZHNAG
					|| (weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card != 0)) {

				return GameConstants.WIK_NULL;
			}
		}

		return GameConstants_HY258.CHR_HUNAN_HY258_MEN_QING;
	}

	// 门清 吉林松原
	public int is_men_qing_jilin_sy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount == 0)
			return GameConstants.CHR_HENAN_XY_MENQING;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT || weaveItem[i].weave_kind == GameConstants.WIK_PENG
					|| weaveItem[i].weave_kind == GameConstants.WIK_BU_ZHNAG
					|| (weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card != 0)) {

				return GameConstants.WIK_NULL;
			}
		}

		return GameConstants.CHR_HENAN_XY_MENQING;
	}

	// 门清 吉林长春
	public int is_men_qing_jilin_cc(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean bxiadanzhanli) {

		// 组合判断
		if (cbWeaveCount == 0)
			return GameConstants.CHR_HENAN_XY_MENQING;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT || weaveItem[i].weave_kind == GameConstants.WIK_PENG
					|| (weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card != 0)) {

				return GameConstants.WIK_NULL;
			}
			if (!bxiadanzhanli) {
				if (weaveItem[i].weave_kind == GameConstants.WIK_LIANG || weaveItem[i].weave_kind == GameConstants.WIK_BU_ZHNAG) {
					return GameConstants.WIK_NULL;
				}

			}

		}

		return GameConstants.CHR_HENAN_XY_MENQING;
	}

	// 吉林麻将是否有幺九
	public boolean is_have_yaojiu_jilincc(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_ZI_FENG];
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LIANG
					&& (weaveItem[i].type == GameConstants.GANG_TYPE_YAO_DAN || weaveItem[i].type == GameConstants.GANG_TYPE_YAO_DAN)) {
				return true;
			}
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG || weaveItem[i].weave_kind == GameConstants.WIK_GANG) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
			} else if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
				cbCardIndexTemp[index + 1] += 1;
				cbCardIndexTemp[index + 2] += 1;
			} else if (weaveItem[i].weave_kind == GameConstants.WIK_CENTER) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
				cbCardIndexTemp[index + 1] += 1;
				cbCardIndexTemp[index - 1] += 1;
			} else if (weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
				cbCardIndexTemp[index - 1] += 1;
				cbCardIndexTemp[index - 2] += 1;
			}
		}
		if (cbCardIndexTemp[switch_to_card_index(0x01)] != 0 || cbCardIndexTemp[switch_to_card_index(0x09)] != 0 // 一九万
				|| cbCardIndexTemp[switch_to_card_index(0x11)] != 0 || cbCardIndexTemp[switch_to_card_index(0x19)] != 0// 一九条
				|| cbCardIndexTemp[switch_to_card_index(0x21)] != 0 || cbCardIndexTemp[switch_to_card_index(0x29)] != 0// 一九桶
				|| cbCardIndexTemp[switch_to_card_index(0x31)] != 0 || cbCardIndexTemp[switch_to_card_index(0x32)] != 0// 风牌
				|| cbCardIndexTemp[switch_to_card_index(0x32)] != 0 || cbCardIndexTemp[switch_to_card_index(0x33)] != 0// 风牌
				|| cbCardIndexTemp[switch_to_card_index(0x34)] != 0 || cbCardIndexTemp[switch_to_card_index(0x35)] != 0
				|| cbCardIndexTemp[switch_to_card_index(0x36)] != 0) {// 中发白
			return true;
		}
		return false;
	}

	// 吉林麻将平胡平胡条件判断
	public boolean ping_hu_tiao_jian_cc(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_ZI_FENG];
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LIANG) {
				return true;
			}
		}
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] >= 3)
				return true;
			if (i > 27 && cbCardIndexTemp[i] >= 2) {
				return true;
			}
		}

		return false;
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui_sy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

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
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount > 2) {
				return GameConstants.CHR_HUNAN_THREE_HAOHUA_QI_YI_SE;
			}
			if (nGenCount == 2) {
				// 双豪华七小对
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}

	}

	// 松原麻将是否有幺九
	public boolean is_have_yaojiu_jilinsy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_ZI_FENG];
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG || weaveItem[i].weave_kind == GameConstants.WIK_GANG) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
			} else if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
				cbCardIndexTemp[index + 1] += 1;
				cbCardIndexTemp[index + 2] += 1;
			} else if (weaveItem[i].weave_kind == GameConstants.WIK_CENTER) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
				cbCardIndexTemp[index + 1] += 1;
				cbCardIndexTemp[index - 1] += 1;
			} else if (weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {
				int index = switch_to_card_index(weaveItem[i].center_card);
				cbCardIndexTemp[index] += 1;
				cbCardIndexTemp[index - 1] += 1;
				cbCardIndexTemp[index - 2] += 1;
			}
		}
		if (cbCardIndexTemp[switch_to_card_index(0x01)] != 0 || cbCardIndexTemp[switch_to_card_index(0x09)] != 0 // 一九万
				|| cbCardIndexTemp[switch_to_card_index(0x11)] != 0 || cbCardIndexTemp[switch_to_card_index(0x19)] != 0// 一九条
				|| cbCardIndexTemp[switch_to_card_index(0x21)] != 0 || cbCardIndexTemp[switch_to_card_index(0x29)] != 0// 一九桶
				|| cbCardIndexTemp[switch_to_card_index(0x35)] != 0) {// 红中
			return true;
		}
		return false;
	}

	// 松原清一色牌
	public boolean is_qing_yi_se_sy(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		// 胡牌判断
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (i >= 33)
				continue;
			if (cards_index[i] != 0) {
				// 花色判断
				if (cbCardColor != 0xFF)
					return false;
				// 设置花色
				cbCardColor = (switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);
				// 设置索引
				i = (i / 9 + 1) * 9 - 1;
			}
		}

		// 如果手上只有王霸
		if (cbCardColor == 0xFF) {
			// 检查组合
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		if ((cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor && !is_magic_card(cur_card))
			return false;

		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if (cbCenterCard == 0x35)
				continue;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	/**
	 * ⑤夹子
	 */
	public boolean is_jia_zi_sy(int cards_index[], int card, boolean bsan_qi) {
		int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
		if (get_card_color(card) == 3)
			return false;
		int value = get_card_value(card);
		if (value == 1 || value == 9)
			return false;
		if (bsan_qi) {
			if (value == 3) {
				if (copyOf[switch_to_card_index(card - 1)] != 0 && copyOf[switch_to_card_index(card - 2)] != 0) {
					copyOf[switch_to_card_index(card - 1)] -= 1;
					copyOf[switch_to_card_index(card - 2)] -= 1;
					boolean hu;
					hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
					if (hu)
						return hu;
					copyOf[switch_to_card_index(card - 1)] += 1;
					copyOf[switch_to_card_index(card - 2)] += 1;
				}
			} else if (value == 7) {
				if (copyOf[switch_to_card_index(card + 1)] != 0 && copyOf[switch_to_card_index(card + 2)] != 0) {
					copyOf[switch_to_card_index(card + 1)] -= 1;
					copyOf[switch_to_card_index(card + 2)] -= 1;
					boolean hu;
					hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
					if (hu)
						return hu;
					copyOf[switch_to_card_index(card + 1)] += 1;
					copyOf[switch_to_card_index(card + 2)] += 1;
				}
			}
		}

		if (copyOf[switch_to_card_index(card - 1)] == 0 || copyOf[switch_to_card_index(card + 1)] == 0)
			return false;
		copyOf[switch_to_card_index(card - 1)] -= 1;
		copyOf[switch_to_card_index(card + 1)] -= 1;

		boolean hu;
		hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
		return hu;
	}

	// 八张
	public int is_bazhang_henan_xy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 胡牌判断
		int cbCardCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_ZI];
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			int index = switch_to_card_index(weaveItem[i].center_card);
			if (index >= GameConstants.MAX_ZI) {
				continue;
			}
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				cbCardIndexTemp[index] += 3;
			}
			if (weaveItem[i].weave_kind == GameConstants.WIK_GANG) {
				cbCardIndexTemp[index] += 4;
			}

		}
		// 组合判断
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (i % 9 == 0) {
				cbCardCount = 0;
			}
			if (cbCardIndexTemp[i] != 0)
				cbCardCount += cbCardIndexTemp[i];

			if (cbCardCount >= 8) {
				return GameConstants.CHR_HENAN_XY_BAZHANG;
			}
		}

		return GameConstants.WIK_NULL;
	}

	// 夹子
	public int is_jaizi_henan_xy(int cards_index[], int cur_card, List<AnalyseItem> analyseItemArray) {

		// 胡牌判断
		int cbCardCount = 0;

		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(0);

			for (int j = 0; j < analyseItem.cbWeaveKind.length; j++) {
				if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_CENTER) != 0)
					return GameConstants.CHR_HENAN_XY_JIAZI;
			}

		}
		return GameConstants.WIK_NULL;
	}

	// 中五
	public int is_zhongwu_henan_xy(List<AnalyseItem> analyseItemArray) {

		// 胡牌判断
		int cbCardCount = 0;

		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(0);
			for (int j = 0; j < analyseItem.cbWeaveKind.length; j++) {
				if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_CENTER) != 0 && get_card_value(analyseItem.cbCenterCard[j]) == 5) {
					return GameConstants.CHR_HENAN_XY_ZHONGWU;
				}
				if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_LEFT) != 0 && get_card_value(analyseItem.cbCenterCard[j]) == 4) {
					return GameConstants.CHR_HENAN_XY_ZHONGWU;
				}

			}

		}
		return GameConstants.WIK_NULL;
	}

	// 连六
	public int is_lianliu_henan_xy(List<AnalyseItem> analyseItemArray) {

		// 胡牌判断
		int cbCardCount = 0;

		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			int firstcard = GameConstants.INVALID_CARD;
			int lastcard = GameConstants.INVALID_CARD;
			AnalyseItem analyseItem = analyseItemArray.get(0);
			for (int j = 0; j < analyseItem.cbWeaveKind.length; j++) {
				if (firstcard == GameConstants.INVALID_CARD) {
					if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_LEFT) != 0)
						firstcard = analyseItem.cbCenterCard[j];
				} else {
					if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_LEFT) != 0) {
						lastcard = analyseItem.cbCenterCard[j];
						if (lastcard - firstcard != 3) {
							firstcard = lastcard;
							lastcard = GameConstants.INVALID_CARD;
						} else {
							if (this.get_card_value(firstcard) == 1 || this.get_card_value(firstcard) == 4) {
								return GameConstants.CHR_HENAN_XY_LIANLIU;
							}
						}
					} else if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_CENTER) != 0) {
						lastcard = analyseItem.cbCenterCard[j];
						if (lastcard - firstcard != 2) {
							firstcard = lastcard;
							lastcard = GameConstants.INVALID_CARD;
						} else {
							if (this.get_card_value(firstcard) == 1 || this.get_card_value(firstcard) == 4) {
								return GameConstants.CHR_HENAN_XY_LIANLIU;
							}

						}
					}
				}

			}

		}
		return GameConstants.WIK_NULL;
	}

	// 独赢
	public int is_duying_henan_xy(int cards_index[], int cur_card, List<AnalyseItem> analyseItemArray) {
		// 计算单牌
		// for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
		// // 变量定义
		// if(cards_index[i] == 2 && cur_card == switch_to_card_index(i)){
		// return GameConstants.CHR_HENAN_XY_DUYING;
		// }
		//
		// }
		if (analyseItemArray.size() == 0) {
			return GameConstants.WIK_NULL;
		}
		AnalyseItem analyseItem = analyseItemArray.get(0);

		if (analyseItem.cbCardEye == cur_card) {
			return GameConstants.CHR_HENAN_XY_DUYING;
		}
		return GameConstants.WIK_NULL;
	}

	// 断门
	public int is_duanmen_henan_xy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		int cbCardIndexTemp[] = new int[GameConstants.MAX_ZI_FENG];
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			int index = switch_to_card_index(weaveItem[i].center_card);
			if (index >= GameConstants.MAX_ZI_FENG) {
				continue;
			}
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				cbCardIndexTemp[index] += 3;
			}
			if (weaveItem[i].weave_kind == GameConstants.WIK_GANG) {
				cbCardIndexTemp[index] += 4;
			}

		}

		boolean bfeng = false;
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] != 0) {
				bfeng = true;
			}
		}

		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cbCardIndexTemp[i] != 0) {
				i = (i / 9 + 1) * 9 - 1;
				count++;
			}
		}
		if (count > 2) {
			return GameConstants.WIK_NULL;
		} else if (count == 2 || count == 0) {
			if (bfeng) {
				return GameConstants.CHR_HENAN_XY_HUNQUE;
			} else {
				return GameConstants.CHR_HENAN_XY_QINGQUE;
			}
		} else {
			return GameConstants.CHR_HENAN_XY_QINGYISE;
		}
	}

	// 三七赢
	public int is_sanqiying_henan_xy(int cur_card) {
		int cbValue = get_card_value(cur_card);
		if (cbValue == 3 || cbValue == 7) {
			return GameConstants.CHR_HENAN_XY_SANQIYING;
		}
		return GameConstants.WIK_NULL;
	}

	// 三七将
	public int is_sanqijiang_henan_xy(List<AnalyseItem> analyseItemArray) {
		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(0);
			int cbValue = get_card_value(analyseItem.cbCardEye);

			if (cbValue == 3 || cbValue == 7) {
				return GameConstants.CHR_HENAN_XY_SANQIJIANG;
			}

		}
		return GameConstants.WIK_NULL;
	}

	// // 河南信阳七小对
	public int is_qi_xiao_dui_henan_xy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			int cbCardCount = cards_index[i];
			// 王牌过滤

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				return GameConstants.WIK_NULL;

		}
		return GameConstants.CHR_HENAN_XY_QI_XIAO_DUI;
	}

	// // 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui_henan(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int nGenCount = 0;

		// 临时数据
		// int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		// for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
		// cbCardIndexTemp[i] = cards_index[i];
		// }

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];
			// 王牌过滤

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				return GameConstants.WIK_NULL;

			if (cbCardCount == 4) {
				nGenCount++;
			}

		}
		if (nGenCount > 0) {
			// if (nGenCount > 1) {
			// // 双豪华七小对
			// return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			// }
			return GameConstants.CHR_HENAN_HH_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HENAN_QI_XIAO_DUI;
		}
	}

	// // 七小对牌 七小对：胡牌时，手上任意七对牌。
	public long is_qi_xiao_dui_yifeng(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int nGenCount = 0;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				return GameConstants.WIK_NULL;

			if (cbCardCount == 4) {
				nGenCount++;
			}

		}
		if (nGenCount > 0) {
			if (nGenCount > 2) {
				return GameConstants.CHR_SANHAO_QI_XIAO_YF;
			} else if (nGenCount > 1) {
				return GameConstants.CHR_SHUANGHAO_QI_XIAO_YF;
			}
			return GameConstants.CHR_HAOHUA_QI_XIAO_YF;
		}
		return GameConstants.CHR_QI_XIAO_DUI_YF;
	}

	// // 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui_ll(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

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
				count += cbCardIndexTemp[get_magic_card_index(m)];
			}
			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			/*
			 * if (nGenCount >= 2) { // 双豪华七小对 return
			 * GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI; }
			 */
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}
	}

	// 大对子,碰碰胡
	public boolean is_pengpeng_hu(AnalyseItem analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i] & (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		return true;
	}

	// 十三夭牌
	// public boolean is_shi_san_yao(int cards_index[], WeaveItem weaveItem[],
	// int cbWeaveCount)
	// {
	// //组合判断
	// if (cbWeaveCount!=0) return false;
	//
	// //扑克判断
	// boolean bCardEye=false;
	//
	// //一九判断
	// for (BYTE i=0;i<27;i+=9)
	// {
	// //无效判断
	// if (cards_index[i]==0) return false;
	// if (cards_index[i+8]==0) return false;
	//
	// //牌眼判断
	// if ((bCardEye==false)&&(cbCardIndex[i]==2)) bCardEye=true;
	// if ((bCardEye==false)&&(cbCardIndex[i+8]==2)) bCardEye=true;
	// }
	//
	// //番子判断
	// for (BYTE i=27;i<MAX_INDEX;i++)
	// {
	// if (cbCardIndex[i]==0) return false;
	// if ((bCardEye==false)&&(cbCardIndex[i]==2)) bCardEye=true;
	// }
	//
	// //牌眼判断
	// if (bCardEye==false) return false;
	//
	// return true;
	// }

	// 都是19
	public boolean is_yao_jiu(int cards_index[], WeaveItem weaveItem[], int weaveCount) {

		int cbValue = 0;
		int cl = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {

				return false;
			}
			cbValue = get_card_value(weaveItem[i].center_card);
			cl = get_card_color(weaveItem[i].center_card);

			// 风牌过滤
			if (cl > 2) {
				continue;
			}

			// 单牌统计
			if ((cbValue != 1) && (cbValue != 9)) {
				return false;
			}

		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if ((i == 0) || (i == 8) || (i == 9) || (i == 17) || (i == 18) || (i == 26)) {
				continue;
			}

			if (cards_index[i] > 0) {
				return false;
			}

		}
		return true;
	}

	/**
	 * 幺九判断
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @return
	 */
	public boolean is_yao_jiu_xtdgk(int cards_index[], WeaveItem weaveItem[], int weaveCount) {

		int cbValue = 0;
		int cl = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {

				return false;
			}
			cbValue = get_card_value(weaveItem[i].center_card);
			cl = get_card_color(weaveItem[i].center_card);

			// 风牌过滤
			if (cl > 2) {
				continue;
			}

			// 单牌统计
			if ((cbValue != 1) && (cbValue != 9)) {
				return false;
			}

		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if ((i == 0) || (i == 8) || (i == 9) || (i == 17) || (i == 18) || (i == 26)) {
				continue;
			}

			if (cards_index[i] > 0 || cards_index[i] == 1) {
				return false;
			}

		}
		return true;
	}

	/**
	 * 混一色
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @return
	 */
	public boolean is_hun_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int color_s = 0xff;
		int cbValue = 0;
		int cl = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {

				if (color_s != 0xff) {

				}
				return false;
			}
			cbValue = get_card_value(weaveItem[i].center_card);
			cl = get_card_color(weaveItem[i].center_card);

			// 风牌过滤
			if (cl > 2) {
				continue;
			}

			// 单牌统计
			if ((cbValue != 1) && (cbValue != 9)) {
				return false;
			}

		}

		return true;
	}

	/**
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @return
	 */
	public boolean is_que_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}
			if (is_magic_index(i))
				continue;
			int card = switch_to_card_data(i);
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = get_card_color(card);
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		if ((cbQueYiMenColor[0] == 1) || (cbQueYiMenColor[1] == 1) || (cbQueYiMenColor[2] == 1)) {
			return true;
		}
		return false;

	}

	public boolean isShiSanLan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[get_magic_card_index(0)] = 0;

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				// if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
				// return false;
				// }
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}

		return true;
	}

	// 江西于都麻将十三烂(仅限自摸情况 炮胡情况请拷贝修改)
	public boolean isShiSanLanYD(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[get_magic_card_index(0)] = 0;

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0 || is_magic_index(j)) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}

		return true;
	}

	// 江西于都麻将七星十三烂(仅限自摸情况 炮胡情况请拷贝修改) 兼容癞子 十三烂的基础上必须东南西北中发白都有一张
	public boolean isQXShiSanLan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int magic_count = magic_count(cbCardIndexTemp);

		cbCardIndexTemp[get_magic_card_index(0)] = 0;

		int need_laizi = 0;

		for (int i = 0; i < 7; i++) {
			int index = switch_to_card_index(0x31) + i;
			if (cbCardIndexTemp[index] != 1) {
				need_laizi++;
			}

		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0/* ||is_magic_index(j) */) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0 || is_magic_index(i)) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}
		if (magic_count >= need_laizi) {
			return true;
		} else {
			return false;
		}
	}

	// 宜丰麻将十三烂
	public boolean isShiSanYF(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		// cbCardIndexTemp[get_magic_card_index(0)] = 0;

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0 || is_magic_index(j)) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}

		return true;
	}

	// 宜丰麻将七星十三烂 十三烂的基础上必须东南西北中发白都有一张
	public boolean isQXShiSanLanYF(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		// cbCardIndexTemp[get_magic_card_index(0)] = 0;

		for (int i = 0; i < 7; i++) {
			int index = switch_to_card_index(0x31) + i;
			if (cbCardIndexTemp[index] != 1) {
				return false;
			}

		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0 || is_magic_index(j)) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0 || is_magic_index(i)) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}

		return true;
	}

	// 全求人
	public boolean isQuanQiuRen(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card) {
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[weaveCount].type == GameConstants.GANG_TYPE_AN_GANG) {
				return false;
			}

		}
		if (!is_dan_diao_lai(cards_index, cur_card)) {
			return false;
		}
		return true;
	}

	/**
	 * 瑞金十三烂
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public boolean isShiSanLanRJ(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[get_magic_card_index(0)] = 0;

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0 && !is_magic_index(j + 1)) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0 && !is_magic_index(j + 2)) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1 && !is_magic_index(i)) {
				return false;
			}
		}

		return true;
	}

	// 十三夭牌
	public boolean isShiSanYao(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		// 组合判断
		if (weaveCount != 0)
			return false;

		// 扑克判断
		boolean bCardEye = false;

		// 一九判断
		for (int i = 0; i < 27; i += 9) {
			// 无效判断
			if (cards_index[i] == 0)
				return false;
			if (cards_index[i + 8] == 0)
				return false;

			// 牌眼判断
			if ((bCardEye == false) && (cards_index[i] == 2))
				bCardEye = true;
			if ((bCardEye == false) && (cards_index[i + 8] == 2))
				bCardEye = true;
		}

		// 番子判断
		for (int i = 27; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (cards_index[i] == 0)
				return false;
			if ((bCardEye == false) && (cards_index[i] == 2))
				bCardEye = true;
		}

		// 牌眼判断
		if (bCardEye == false)
			return false;

		return true;
	}

	/**
	 * 获取花色门数
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @return
	 */
	public int get_se_count(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}
			if (is_magic_index(i))
				continue;
			int card = switch_to_card_data(i);
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = get_card_color(card);
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		int count = 0;
		for (int i = 0; i < 3; i++) {
			if (cbQueYiMenColor[i] == 0) {
				count += 1;
			}
		}
		return count;
	}

	public int get_se_count_ying(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}

			int card = switch_to_card_data(i);

			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = get_card_color(card);

			if (cbCardColor > 2)
				continue;

			cbQueYiMenColor[cbCardColor] = 0;
		}

		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}

		int magic_card_index = get_magic_card_index(0);
		int magic_card = switch_to_card_data(magic_card_index);
		int magic_card_color = get_card_color(magic_card);

		if (magic_card_color <= 2) {
			if (exist_suo_pai(weaveItem, weaveCount)) {
				cbQueYiMenColor[magic_card_color] = 0;
			}
		}

		int count = 0;
		for (int i = 0; i < 3; i++) {
			if (cbQueYiMenColor[i] == 0) {
				count += 1;
			}
		}

		return count;
	}

	public boolean exist_eat_hubei(WeaveItem weaveItem[], int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)
				return true;
		}

		return false;
	}

	public boolean exist_eat(WeaveItem weaveItem[], int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)
				return true;
		}

		return false;
	}

	public boolean exist_suo_pai(WeaveItem weaveItem[], int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_PENG_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_PENG_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_GANG_1 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_GANG_2
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_GANG_3 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_GANG_4)
				return true;
		}

		return false;
	}

	/**
	 * 获取花色
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @return
	 */
	public int[] get_pai_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}
			if (is_magic_index(i))
				continue;
			int card = switch_to_card_data(i);
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = get_card_color(card);
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		return cbQueYiMenColor;
	}

	public boolean check_hubei_couyise(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		if (get_se_count(cards_index, weave_items, weave_count) > 1)
			return false;

		if (has_feng_pai(cards_index, weave_items, weave_count) == false)
			return false;

		return true;
	}

	public boolean check_hubei_ying_couyise(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		if (get_se_count_ying(cards_index, weave_items, weave_count) > 1)
			return false;

		if (has_feng_pai_ying(cards_index, weave_items, weave_count) == false)
			return false;

		return true;
	}

	public boolean check_hubei_feng_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			int color = get_card_color(weave_items[i].center_card);
			if (color < 3)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (is_magic_index(i))
				continue;

			if (cards_index[i] > 0)
				return false;
		}

		return true;
	}

	public boolean check_hubei_ying_feng_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int magic_card_index = get_magic_card_index(0);
		int magic_card = switch_to_card_data(magic_card_index);
		int magic_card_color = get_card_color(magic_card);

		if (magic_card_color <= 2) {
			if (exist_suo_pai(weave_items, weave_count)) {
				return false;
			}
		}

		for (int i = 0; i < weave_count; i++) {
			int color = get_card_color(weave_items[i].center_card);
			if (color < 3)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] > 0)
				return false;
		}

		return true;
	}

	public boolean has_feng_pai(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			int color = get_card_color(weave_items[i].center_card);
			if (color > 2)
				return true;
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (is_magic_index(i))
				continue;

			if (cards_index[i] > 0)
				return true;
		}

		return false;
	}

	public boolean has_feng_pai_ying(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int magic_card_index = get_magic_card_index(0);
		int magic_card = switch_to_card_data(magic_card_index);
		int magic_card_color = get_card_color(magic_card);

		if (magic_card_color == 3) {
			if (exist_suo_pai(weave_items, weave_count)) {
				return true;
			}
		}

		for (int i = 0; i < weave_count; i++) {
			int color = get_card_color(weave_items[i].center_card);
			if (color > 2)
				return true;
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] > 0)
				return true;
		}

		return false;
	}

	/**
	 * 全风
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @return
	 */
	public boolean is_quan_feng(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cl = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {

				return false;
			}
			cl = get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cl < 3) {
				return false;
			}
		}
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] > 0) {
				return false;
			}
		}
		return true;
	}

	// 将对
	public boolean is_jiang_dui(AnalyseItem pAnalyseItem) {
		// 是否大对子
		if (!is_pengpeng_hu(pAnalyseItem))
			return false;

		// 检查牌眼
		int cbCardValue = get_card_value(pAnalyseItem.cbCardEye);
		if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8)
			return false;

		for (int i = 0; i < pAnalyseItem.cbWeaveKind.length; i++) {
			if ((pAnalyseItem.cbWeaveKind[i] & (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0) {
				int j = 0;
				for (; j < 3; j++) {
					cbCardValue = get_card_value(pAnalyseItem.cbCardData[i][j]);
					if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8)
						break;
				}
				if (j == 3)
					return false;
			} else {
				cbCardValue = get_card_value(pAnalyseItem.cbCenterCard[i]);
				if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8)
					return false;
			}
		}
		return true;
	}

	// 将将胡：玩家手上每一张牌都为 2 、 5 、 8 ，可碰。
	public boolean is_jiangjiang_hu(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		int cbValue = 0;

		if (is_valid_card(cur_card)) {
			cbValue = get_card_value(cur_card);

			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			cbValue = get_card_value(switch_to_card_data(i));

			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {

				return false;
			}
			cbValue = get_card_value(weaveItem[i].center_card);
			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}

		}
		return true;
	}

	// 将将胡：玩家手上每一张牌都为 2 、 5 、 8 ，可碰。
	public boolean is_jiangjiang_hu_qi_shou(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			int cbValue = get_card_value(switch_to_card_data(i));

			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {

				return false;
			}
			int cbValue = get_card_value(weaveItem[i].center_card);
			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}

		}
		return true;
	}

	public boolean is_jiangjiang_hu_qishou(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			int cbValue = get_card_value(switch_to_card_data(i));

			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {

				return false;
			}
			int cbValue = get_card_value(weaveItem[i].center_card);
			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}

		}
		return true;
	}

	public boolean check_hubei_ying_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			int cbValue = get_card_value(switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		if (exist_eat_hubei(weave_items, weave_count))
			return false;

		int magic_card_index = get_magic_card_index(0);
		int magic_card = switch_to_card_data(magic_card_index);
		int magic_card_color = get_card_color(magic_card);
		int magic_card_value = get_card_value(magic_card);

		int magic_count = magic_count(cards_index);

		if (exist_suo_pai(weave_items, weave_count) || magic_count > 0) {
			if (magic_card_color > 2)
				return false;

			if ((magic_card_value != 2) && (magic_card_value != 5) && (magic_card_value != 8)) {
				return false;
			}
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int color = get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 双王牌时的硬将一色
	 * 
	 * @param cards_index
	 * @param weave_items
	 * @param weave_count
	 * @return
	 */
	public boolean check_sg_ying_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			int cbValue = get_card_value(switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		if (exist_eat_hubei(weave_items, weave_count))
			return false;

		if (_magic_card_count == 1) {
			int magic_card_index = get_magic_card_index(0);
			int magic_card = switch_to_card_data(magic_card_index);
			int magic_card_color = get_card_color(magic_card);
			int magic_card_value = get_card_value(magic_card);

			int magic_count = magic_count(cards_index);

			if (exist_suo_pai(weave_items, weave_count) || magic_count > 0) {
				if (magic_card_color > 2)
					return false;

				if ((magic_card_value != 2) && (magic_card_value != 5) && (magic_card_value != 8)) {
					return false;
				}
			}
		} else if (_magic_card_count == 2) {
			int magic_card_index_1 = get_magic_card_index(0);
			int magic_card_index_2 = get_magic_card_index(1);

			int magic_card_1 = switch_to_card_data(magic_card_index_1);
			int magic_card_color_1 = get_card_color(magic_card_1);
			int magic_card_value_1 = get_card_value(magic_card_1);

			int magic_card_2 = switch_to_card_data(magic_card_index_2);
			int magic_card_color_2 = get_card_color(magic_card_2);
			int magic_card_value_2 = get_card_value(magic_card_2);

			int magic_count = magic_count(cards_index);

			if (exist_suo_pai(weave_items, weave_count) || magic_count > 0) {
				if (magic_card_color_1 > 2 || magic_card_color_2 > 2)
					return false;

				if (cards_index[magic_card_index_1] > 0) {
					if ((magic_card_value_1 != 2) && (magic_card_value_1 != 5) && (magic_card_value_1 != 8)) {
						return false;
					}
				}

				if (cards_index[magic_card_index_2] > 0) {
					if ((magic_card_value_2 != 2) && (magic_card_value_2 != 5) && (magic_card_value_2 != 8)) {
						return false;
					}
				}
			}
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int color = get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		return true;
	}

	public boolean check_hubei_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (is_magic_index(i))
				continue;

			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			if (is_magic_index(i))
				continue;

			int cbValue = get_card_value(switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		if (exist_eat_hubei(weave_items, weave_count))
			return false;

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int color = get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}

		}

		return true;
	}

	// 桃江麻将，别人打的王牌，只能癞子还原
	public boolean check_taojiang_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count, int cur_card) {
		int tmp_card_value = get_card_value(cur_card);
		if (tmp_card_value != 2 && tmp_card_value != 5 && tmp_card_value != 8) {
			return false;
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (is_magic_index(i))
				continue;

			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			if (is_magic_index(i))
				continue;

			int cbValue = get_card_value(switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		if (exist_eat_hubei(weave_items, weave_count))
			return false;

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int color = get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}

		}

		return true;
	}

	// 清一色牌
	public boolean is_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		// 胡牌判断
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] != 0) {
				// 花色判断
				if (cbCardColor != 0xFF)
					return false;

				// 设置花色
				cbCardColor = (switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				// 设置索引
				i = (i / 9 + 1) * 9 - 1;
			}
		}

		// 如果手上只有王霸
		if (cbCardColor == 0xFF) {
			// 检查组合
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		if (is_valid_card(cur_card))
			if ((cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor && !is_magic_card(cur_card))
				return false;

		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	// 字一色牌
	public boolean is_zi_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		// 胡牌判断
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] != 0) {
				// 花色判断
				if (cbCardColor != 0xFF)
					return false;

				// 设置花色
				cbCardColor = (switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				// 设置索引
				i = (i / 9 + 1) * 9 - 1;
			}
		}

		// 如果手上只有王霸
		if (cbCardColor == 0xFF) {
			// 检查组合
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		if ((cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor && !is_magic_card(cur_card))
			return false;

		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return cbCardColor >> 4 == 3;
	}

	// 一条龙
	public boolean is_yi_tiao_long(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {

		int cbCardColor = 0xFF;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] != 0) {
				if (cbCardColor == 0xFF) {
					cbCardColor = get_card_color(cards_index[i]);
				} else if (cbCardColor != get_card_color(cards_index[i])) {
					return false;
				}
			}
		}

		int cbCardValue = 0xFF;
		// 判断1到九
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] != 0) {
				if (cbCardValue == 0xFF) {
					cbCardValue = get_card_value(cards_index[i]);
					continue;
				} else {
					if (cbCardValue == get_card_value(cards_index[i])) {
						continue;
					} else {
						if (cbCardValue + 1 != get_card_value(cards_index[i]))
							return false;
					}

				}

			}
		}
		return true;
	}

	public boolean is_qing_yi_se_qishou(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] != 0) {
				// 花色判断
				if (cbCardColor != 0xFF)
					return false;

				// 设置花色
				cbCardColor = (switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				// 设置索引
				i = (i / 9 + 1) * 9 - 1;
			}
		}

		// 如果手上只有王霸
		if (cbCardColor == 0xFF) {
			// 检查组合
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	public boolean check_hubei_qing_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (is_magic_index(i))
				continue;

			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weave_items[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int cbCenterCard = weave_items[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	// 桃江麻将，别人打王牌时，判断是不是清一色
	public boolean check_taojiang_qing_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count, int cur_card) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (is_magic_index(i))
				continue;

			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weave_items[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int cbCenterCard = weave_items[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		if ((cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor) {
			return false;
		}

		return true;
	}

	/**
	 * 湖北子麻将，硬清一色判断
	 * 
	 * @param cards_index
	 * @param weave_items
	 * @param weave_count
	 * @return
	 */
	public boolean check_hubei_ying_qing_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weave_items[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int cbCenterCard = weave_items[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	// 分析扑克--东南西北可以自由组合
	public boolean analyse_card_feng_chi(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray) {
		// 计算数目
		boolean has_feng = true;
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 效验参数
			if (((cbCardCount == 2) && (cbWeaveCount == 4)) == false) {
				return false;
			}
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2 || (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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

				// 连牌判断
				if ((i > GameConstants.MAX_ZI && i < (GameConstants.MAX_ZI_FENG - 2))) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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

				// 连牌判断
				if ((i > GameConstants.MAX_ZI + 5 && i < GameConstants.MAX_ZI_FENG)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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

	// 卡张
	public int is_kazhang_henan_py(int cards_index[], int cur_card, List<AnalyseItem> analyseItemArray) {

		// 胡牌判断
		int cbCardCount = 0;

		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(0);

			for (int j = 0; j < analyseItem.cbWeaveKind.length; j++) {
				if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_CENTER) != 0)
					return GameConstants.CHR_HENAN_XY_JIAZI;
			}

		}
		return GameConstants.WIK_NULL;
	}

	// 分析麻将--东南西北、中发白可以自由组合
	public boolean analyse_card_henanpy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray,
			ChiHuRight chiHuRight, boolean yaojiupu, boolean fengpu, boolean jiangpu, boolean has_feng) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;
		// 单吊判断
		if (cbLessKindItem == 0) {
			// 效验参数
			if (((cbCardCount == 2) && (cbWeaveCount == 4)) == false) {
				return false;
			}
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2 || (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}
		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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

				// 连牌判断
				if ((i >= GameConstants.MAX_ZI && i < (GameConstants.MAX_FENG - 2)) && fengpu) {
					// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合

					int sub_index = 4;
					if (i == 28)
						sub_index = 3;
					int i_index[] = { 0, 1, 2, 3 };
					int sub_index_array[] = new int[3];
					sub_index_array[0] = i_index[0];
					for (int a = 1; a < sub_index - 1; a++) {
						sub_index_array[1] = i_index[a];
						for (int b = a + 1; b < sub_index; b++) {
							sub_index_array[2] = i_index[b];
							int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + sub_index_array[1]] + cbMagicCardIndex[i + sub_index_array[2]];
							chi_count += cbMagicCardCount;
							if (chi_count >= 3) {
								int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
										this.is_magic_index(i + sub_index_array[1]) ? 0 : cbMagicCardIndex[i + sub_index_array[1]],
										this.is_magic_index(i + sub_index_array[2]) ? 0 : cbMagicCardIndex[i + sub_index_array[2]] };

								int nMagicCountTemp = cbMagicCardCount;

								int cbValidIndex[] = new int[3];// 实际的牌
								while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {

											cbIndex[j]--;
											cbValidIndex[j] = i + sub_index_array[j];

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
										kindItem[cbKindItemCount].cbCardIndex[1] = i + sub_index_array[1];
										kindItem[cbKindItemCount].cbCardIndex[2] = i + sub_index_array[2];
										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
										for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
											kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
										}
										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}
					}

				} // 连牌判断 end
					// if ((i > GameConstants.MAX_FENG && i <
					// (GameConstants.MAX_ZI_FENG - 2))) {
				if (i == 31 && jiangpu) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
									kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
								}
								cbKindItemCount++;
							} else {
								break;
							}
						}
					}
				}

			}
		} // if (cbCardCount>=3) end

		// 幺九扑
		if (yaojiupu) {
			// 119
			for (int j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++) {
					if (i == j) {
						continue;
					}
					if (cbMagicCardIndex[i * 9] > 0 && cbMagicCardIndex[j * 9] > 0) {
						for (int x = 0; x < 3; x++) {
							int count = 0;
							while (cbMagicCardIndex[i * 9] != count && cbMagicCardIndex[8 + (x * 9)] != count && cbMagicCardIndex[j * 9] != count) {
								kindItem[cbKindItemCount].cbCardIndex[0] = i * 9;
								kindItem[cbKindItemCount].cbCardIndex[1] = 8 + (x * 9);
								kindItem[cbKindItemCount].cbCardIndex[2] = j * 9;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i * 9);
								kindItem[cbKindItemCount].cbValidIndex[0] = i * 9;
								kindItem[cbKindItemCount].cbValidIndex[1] = 8 + (x * 9);
								kindItem[cbKindItemCount].cbValidIndex[2] = j * 9;
								cbKindItemCount++;
								count++;
							}
						}
					}
				}
			}
			// 199
			for (int j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++) {
					if (i == j) {
						continue;
					}
					if (cbMagicCardIndex[8 + (i * 9)] > 0 && cbMagicCardIndex[8 + (j * 9)] > 0) {
						for (int x = 0; x < 3; x++) {
							int count = 0;
							while (cbMagicCardIndex[x * 9] != count && cbMagicCardIndex[8 + (i * 9)] != count
									&& cbMagicCardIndex[8 + (j * 9)] != count) {
								kindItem[cbKindItemCount].cbCardIndex[0] = x * 9;
								kindItem[cbKindItemCount].cbCardIndex[1] = 8 + (i * 9);
								kindItem[cbKindItemCount].cbCardIndex[2] = 8 + (j * 9);
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(x * 9);
								kindItem[cbKindItemCount].cbValidIndex[0] = x * 9;
								kindItem[cbKindItemCount].cbValidIndex[1] = 8 + (i * 9);
								kindItem[cbKindItemCount].cbValidIndex[2] = 8 + (j * 9);
								cbKindItemCount++;
								count++;
							}
						}

					}
				}
			}
		}

		// 组合分析
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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

	public int analyse_weave_pu_yang(int cbMagicCardIndex[], int weave_kind[][], int weave_kind_count[], int weave_kind_card[][][],
			KindItem kindItem[], boolean yaojiupu, boolean fengpu, boolean jiangpu, boolean has_feng) {
		int cbKindItemCount = 0;

		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		int mj_count = GameConstants.MAX_ZI;
		if (has_feng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		}

		for (int i = 0; i < mj_count; i++) {
			if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;

				kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

				kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 0 ? i : get_magic_card_index(0);
				kindItem[cbKindItemCount].cbValidIndex[1] = cbMagicCardIndex[i] > 1 ? i : get_magic_card_index(0);
				kindItem[cbKindItemCount].cbValidIndex[2] = cbMagicCardIndex[i] > 2 ? i : get_magic_card_index(0);

				weave_kind[i][weave_kind_count[i]] = kindItem[cbKindItemCount].cbWeaveKind;
				weave_kind_card[i][weave_kind_count[i]][0] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[0]);
				weave_kind_card[i][weave_kind_count[i]][1] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[1]);
				weave_kind_card[i][weave_kind_count[i]][2] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[2]);
				weave_kind_count[i]++;

				cbKindItemCount++;

				if (cbMagicCardIndex[i] + cbMagicCardCount >= 6) {
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;

					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

					kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);

					weave_kind[i][weave_kind_count[i]] = kindItem[cbKindItemCount].cbWeaveKind;
					weave_kind_card[i][weave_kind_count[i]][0] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[0]);
					weave_kind_card[i][weave_kind_count[i]][1] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[1]);
					weave_kind_card[i][weave_kind_count[i]][2] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[2]);
					weave_kind_count[i]++;

					cbKindItemCount++;
				}
			}

			if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
				int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] + cbMagicCardIndex[i + 2];
				chi_count += cbMagicCardCount;

				if (chi_count >= 3) {
					int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i], this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1],
							this.is_magic_index(i + 2) ? 0 : cbMagicCardIndex[i + 2] };

					int nMagicCountTemp = cbMagicCardCount;

					int cbValidIndex[] = new int[3];

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
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;

							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

							for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
								kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
							}

							int kind[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
							for (int k = 0; k < 3; k++) {
								int index = kindItem[cbKindItemCount].cbCardIndex[k];
								int count = weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]];
								weave_kind[index][count] = kind[k];
								weave_kind_card[index][count][0] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[0]);
								weave_kind_card[index][count][1] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[1]);
								weave_kind_card[index][count][2] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[2]);
								weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++;
							}

							cbKindItemCount++;
						} else {
							break;
						}
					}
				}
			}

			if ((i >= GameConstants.MAX_ZI && i < (GameConstants.MAX_FENG - 2)) && fengpu) {
				int sub_index = 4;
				if (i == 28)
					sub_index = 3;

				int i_index[] = { 0, 1, 2, 3 };
				int sub_index_array[] = new int[3];
				sub_index_array[0] = i_index[0];

				for (int a = 1; a < sub_index - 1; a++) {
					sub_index_array[1] = i_index[a];

					for (int b = a + 1; b < sub_index; b++) {
						sub_index_array[2] = i_index[b];

						int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + sub_index_array[1]] + cbMagicCardIndex[i + sub_index_array[2]];
						chi_count += cbMagicCardCount;

						if (chi_count >= 3) {
							int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
									this.is_magic_index(i + sub_index_array[1]) ? 0 : cbMagicCardIndex[i + sub_index_array[1]],
									this.is_magic_index(i + sub_index_array[2]) ? 0 : cbMagicCardIndex[i + sub_index_array[2]] };

							int nMagicCountTemp = cbMagicCardCount;

							int cbValidIndex[] = new int[3];

							while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;
										cbValidIndex[j] = i + sub_index_array[j];
									} else {
										nMagicCountTemp--;
										cbValidIndex[j] = get_magic_card_index(0);
									}
								}

								if (nMagicCountTemp >= 0) {
									kindItem[cbKindItemCount].cbCardIndex[0] = i;
									kindItem[cbKindItemCount].cbCardIndex[1] = i + sub_index_array[1];
									kindItem[cbKindItemCount].cbCardIndex[2] = i + sub_index_array[2];

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
										kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
									}

									int kind[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
									for (int k = 0; k < 3; k++) {
										int index = kindItem[cbKindItemCount].cbCardIndex[k];
										int count = weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]];
										weave_kind[index][count] = kind[k];
										weave_kind_card[index][count][0] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[0]);
										weave_kind_card[index][count][1] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[1]);
										weave_kind_card[index][count][2] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[2]);
										weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++;
									}

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}
				}
			}

			if (i == 31 && jiangpu) {
				int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] + cbMagicCardIndex[i + 2];
				chi_count += cbMagicCardCount;

				if (chi_count >= 3) {
					int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i], this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1],
							this.is_magic_index(i + 2) ? 0 : cbMagicCardIndex[i + 2] };

					int nMagicCountTemp = cbMagicCardCount;

					int cbValidIndex[] = new int[3];

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
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;

							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

							for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
								kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
							}

							int kind[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };
							for (int k = 0; k < 3; k++) {
								int index = kindItem[cbKindItemCount].cbCardIndex[k];
								int count = weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]];
								weave_kind[index][count] = kind[k];
								weave_kind_card[index][count][0] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[0]);
								weave_kind_card[index][count][1] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[1]);
								weave_kind_card[index][count][2] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[2]);
								weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++;
							}

							cbKindItemCount++;
						} else {
							break;
						}
					}
				}
			}
		}

		if (yaojiupu) {
			for (int j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++) {
					if (i == j) {
						continue;
					}

					if (cbMagicCardIndex[i * 9] > 0 && cbMagicCardIndex[j * 9] > 0) {
						for (int x = 0; x < 3; x++) {
							int count = 0;

							while (cbMagicCardIndex[i * 9] != count && cbMagicCardIndex[8 + (x * 9)] != count && cbMagicCardIndex[j * 9] != count) {
								kindItem[cbKindItemCount].cbCardIndex[0] = i * 9;
								kindItem[cbKindItemCount].cbCardIndex[1] = 8 + (x * 9);
								kindItem[cbKindItemCount].cbCardIndex[2] = j * 9;

								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i * 9);

								kindItem[cbKindItemCount].cbValidIndex[0] = i * 9;
								kindItem[cbKindItemCount].cbValidIndex[1] = 8 + (x * 9);
								kindItem[cbKindItemCount].cbValidIndex[2] = j * 9;

								for (int k = 0; k < 3; k++) {
									int index = kindItem[cbKindItemCount].cbCardIndex[k];
									int tmpCount = weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]];
									weave_kind[index][tmpCount] = GameConstants.WIK_YAO_JIU;
									weave_kind_card[index][tmpCount][0] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[0]);
									weave_kind_card[index][tmpCount][1] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[1]);
									weave_kind_card[index][tmpCount][2] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[2]);
									weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++;
								}

								cbKindItemCount++;

								count++;
							}
						}
					}
				}
			}

			for (int j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++) {
					if (i == j) {
						continue;
					}

					if (cbMagicCardIndex[8 + (i * 9)] > 0 && cbMagicCardIndex[8 + (j * 9)] > 0) {
						for (int x = 0; x < 3; x++) {
							int count = 0;

							while (cbMagicCardIndex[x * 9] != count && cbMagicCardIndex[8 + (i * 9)] != count
									&& cbMagicCardIndex[8 + (j * 9)] != count) {
								kindItem[cbKindItemCount].cbCardIndex[0] = x * 9;
								kindItem[cbKindItemCount].cbCardIndex[1] = 8 + (i * 9);
								kindItem[cbKindItemCount].cbCardIndex[2] = 8 + (j * 9);

								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(x * 9);

								kindItem[cbKindItemCount].cbValidIndex[0] = x * 9;
								kindItem[cbKindItemCount].cbValidIndex[1] = 8 + (i * 9);
								kindItem[cbKindItemCount].cbValidIndex[2] = 8 + (j * 9);

								for (int k = 0; k < 3; k++) {
									int index = kindItem[cbKindItemCount].cbCardIndex[k];
									int tmpCount = weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]];
									weave_kind[index][tmpCount] = GameConstants.WIK_YAO_JIU;
									weave_kind_card[index][tmpCount][0] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[0]);
									weave_kind_card[index][tmpCount][1] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[1]);
									weave_kind_card[index][tmpCount][2] = switch_to_card_data(kindItem[cbKindItemCount].cbCardIndex[2]);
									weave_kind_count[kindItem[cbKindItemCount].cbCardIndex[k]]++;
								}

								cbKindItemCount++;

								count++;
							}
						}
					}
				}
			}
		}

		return cbKindItemCount;
	}

	public boolean analyse_card_henanpy_new(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray,
			ChiHuRight chiHuRight, boolean yaojiupu, boolean fengpu, boolean jiangpu, boolean has_feng) {
		int cbCardCount = get_card_count_by_index(cards_index);
		int cbKindItemCount = 0;
		int cbLessKindItem = (cbCardCount - 2) / 3;

		WeaveItem extraWeaveItem[] = new WeaveItem[4];
		for (int i = 0; i < 4; i++)
			extraWeaveItem[i] = new WeaveItem();

		boolean hasExtra = false;
		int extraCount = 0;

		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}

		KindItem kindItem[] = new KindItem[100];
		int weave_kind[][] = new int[GameConstants.MAX_INDEX][30];
		int weave_kind_count[] = new int[GameConstants.MAX_INDEX];
		int weave_kind_card[][][] = new int[GameConstants.MAX_INDEX][30][3];

		do {
			for (int i = 0; i < kindItem.length; i++) {
				kindItem[i] = new KindItem();
			}

			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				Arrays.fill(weave_kind[i], 0);
			}
			Arrays.fill(weave_kind_count, 0);

			cbKindItemCount = analyse_weave_pu_yang(cbMagicCardIndex, weave_kind, weave_kind_count, weave_kind_card, kindItem, yaojiupu, fengpu,
					jiangpu, has_feng);

			boolean flag = false;

			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (weave_kind_count[i] == 1) {
					extraWeaveItem[extraCount].center_card = switch_to_card_data(i);
					extraWeaveItem[extraCount].weave_kind = weave_kind[i][0];
					extraWeaveItem[extraCount].weave_card = Arrays.copyOf(weave_kind_card[i][0], weave_kind_card[i][0].length);

					if (remove_cards_by_index(cbMagicCardIndex, extraWeaveItem[extraCount].weave_card, extraWeaveItem[extraCount].weave_card.length) == true) {
						hasExtra = true;
						extraCount++;
						cbLessKindItem--;
						flag = true;
						break;
					}
				}
			}

			if (flag == false)
				break;
		} while (true);

		if (cbLessKindItem == 0) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cbMagicCardIndex[i] == 2 || (this.is_magic_index(i) == false && magic_count(cbMagicCardIndex) + cbMagicCardIndex[i] == 2)) {
					AnalyseItem analyseItem = new AnalyseItem();

					int count = 0;

					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
						count++;
					}

					if (hasExtra) {
						for (int k = 0; k < extraCount; k++) {
							analyseItem.cbWeaveKind[count] = extraWeaveItem[k].weave_kind;
							analyseItem.cbCenterCard[count] = extraWeaveItem[k].center_card;
							analyseItem.cbCardData[count] = Arrays.copyOf(extraWeaveItem[k].weave_card, extraWeaveItem[k].weave_card.length);
							count++;
						}
					}

					analyseItem.cbCardEye = switch_to_card_data(i);

					if (cbMagicCardIndex[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		}

		if (cbKindItemCount >= cbLessKindItem) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}

				for (int i = 0; i < mj_count; i++) {
					cbCardIndexTemp[i] = cbMagicCardIndex[i];
				}

				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}
				}

				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
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

				if (bEnoughCard == true) {
					int cbCardEye = 0;
					boolean bMagicEye = false;
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
						}
					}

					if (cbCardEye != 0) {
						AnalyseItem analyseItem = new AnalyseItem();

						int count = 0;

						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
							count++;
						}

						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
							count++;
						}

						if (hasExtra) {
							for (int k = 0; k < extraCount; k++) {
								analyseItem.cbWeaveKind[count] = extraWeaveItem[k].weave_kind;
								analyseItem.cbCenterCard[count] = extraWeaveItem[k].center_card;
								analyseItem.cbCardData[count] = Arrays.copyOf(extraWeaveItem[k].weave_card, extraWeaveItem[k].weave_card.length);
								count++;
							}
						}

						analyseItem.cbCardEye = cbCardEye;
						analyseItem.bMagicEye = bMagicEye;

						analyseItemArray.add(analyseItem);
					}
				}

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

	// 分析麻将--东南西北、中发白可以自由组合
	public boolean analyse_card_feng_chi_zfb(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray,
			ChiHuRight chiHuRight, boolean hupai) {
		// 计算数目
		boolean has_feng = true;
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;
		// 单吊判断
		if (cbLessKindItem == 0) {
			// 效验参数
			if (((cbCardCount == 2) && (cbWeaveCount == 4)) == false) {
				return false;
			}
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2 || (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}
		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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

				// 连牌判断
				if ((i >= GameConstants.MAX_ZI && i < (GameConstants.MAX_FENG - 2))) {
					// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合

					int sub_index = 4;
					if (i == 28)
						sub_index = 3;
					int i_index[] = { 0, 1, 2, 3 };
					int sub_index_array[] = new int[3];
					sub_index_array[0] = i_index[0];
					for (int a = 1; a < sub_index - 1; a++) {
						sub_index_array[1] = i_index[a];
						for (int b = a + 1; b < sub_index; b++) {
							sub_index_array[2] = i_index[b];
							int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + sub_index_array[1]] + cbMagicCardIndex[i + sub_index_array[2]];
							chi_count += cbMagicCardCount;
							if (chi_count >= 3) {
								int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
										this.is_magic_index(i + sub_index_array[1]) ? 0 : cbMagicCardIndex[i + sub_index_array[1]],
										this.is_magic_index(i + sub_index_array[2]) ? 0 : cbMagicCardIndex[i + sub_index_array[2]] };

								int nMagicCountTemp = cbMagicCardCount;

								int cbValidIndex[] = new int[3];// 实际的牌
								while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {

											cbIndex[j]--;
											cbValidIndex[j] = i + sub_index_array[j];

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
										kindItem[cbKindItemCount].cbCardIndex[1] = i + sub_index_array[1];
										kindItem[cbKindItemCount].cbCardIndex[2] = i + sub_index_array[2];
										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
										for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
											kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
										}
										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}
					}

				} // 连牌判断 end
					// if ((i > GameConstants.MAX_FENG && i <
					// (GameConstants.MAX_ZI_FENG - 2))) {
				if (i == 31) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
									kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
								}
								cbKindItemCount++;
							} else {
								break;
							}
						}
					}
				}

			}
		} // if (cbCardCount>=3) end

		// 组合分析
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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
	// public void analyse_fengzu_count(int cbMagicCardIndex[],ChiHuRight
	// chiHuRight){
	// for(int i=GameConstants.MAX_ZI;i< GameConstants.MAX_ZI_FENG;i++){
	// int cbMagicCardCount = 0;
	// //算黑风
	// if(i == GameConstants.MAX_ZI){
	// // 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
	// int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
	// this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1],
	// this.is_magic_index(i + 2) ? 0
	// : cbMagicCardIndex[i + 2], this.is_magic_index(i + 2) ? 0:
	// cbMagicCardIndex[i + 3] };
	// //如果东跟北都为0,就不用算了
	// if(cbIndex[0]==0&&cbIndex[0]==0){
	// i += 3;
	// continue;
	// }
	// //如果有9个风牌，就不去管白风了
	// if((cbIndex[0]==3||cbIndex[3]==3)&&cbIndex[1]==3&&cbIndex[2]==3){
	// chiHuRight.heifeng_count = 3;
	// return;
	// }
	// if(cbIndex[0]>0&&cbIndex[1]>0&&cbIndex[2]>0){
	// chiHuRight.heifeng_count++;
	// cbIndex[0]--;
	// cbIndex[1]--;
	// cbIndex[2]--;
	// if(cbIndex[0]>0&&cbIndex[1]>0&&cbIndex[2]>0){
	// chiHuRight.heifeng_count++;
	// cbIndex[0]--;
	// cbIndex[1]--;
	// cbIndex[2]--;
	// }else{
	// if(cbIndex[1]>0&&cbIndex[2]>0&&cbIndex[3]>0){
	// chiHuRight.heifeng_count++;
	// cbIndex[0]--;
	// cbIndex[1]--;
	// cbIndex[2]--;
	// }
	// }
	// }else{
	// if(cbIndex[1]>0&&cbIndex[2]>0&&cbIndex[3]>0){
	// chiHuRight.heifeng_count++;
	// cbIndex[0]--;
	// cbIndex[1]--;
	// cbIndex[2]--;
	// if(cbIndex[1]>0&&cbIndex[2]>0&&cbIndex[3]>0){
	// chiHuRight.heifeng_count++;
	// cbIndex[0]--;
	// cbIndex[1]--;
	// cbIndex[2]--;
	// }
	// }
	// }
	// i+=3;
	// }
	// //算白风
	// if(i==31){
	// // 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
	// int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] +
	// cbMagicCardIndex[i + 2];
	// chi_count += cbMagicCardCount;
	// if (chi_count >= 3) {
	// int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
	// this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1],
	// this.is_magic_index(i + 2) ? 0
	// : cbMagicCardIndex[i + 2] };
	// if(cbIndex[0]==3&&cbIndex[1]==3&&cbIndex[2]==3){
	// chiHuRight.baifeng_count=3;
	// return;
	// }
	// if(cbIndex[0]>0&&cbIndex[1]>0&&cbIndex[2]>0){
	// chiHuRight.baifeng_count++;
	// cbIndex[0]--;
	// cbIndex[1]--;
	// cbIndex[2]--;
	// if(cbIndex[0]>0&&cbIndex[1]>0&&cbIndex[2]>0){
	// chiHuRight.baifeng_count++;
	// }
	// }
	// }
	//
	// }
	// }
	// }

	// 分析扑克
	public boolean analyse_card_henan_zhou_kou(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray,
			boolean has_feng) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 效验参数，要注释掉下面的两行，不然杠换章检测的时候回出错，cbWeaveCount是3 -_-
			/**
			 * if (((cbCardCount == 2) && (cbWeaveCount == 4)) == false) {
			 * return false; }
			 **/
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
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
						analyseItem.cbCardEye = switch_to_card_data(this.get_magic_card_index(0));
						analyseItem.bMagicEye = true;
					} else if (is_magic_index(i)) { // 如果是癞子索引
						continue;
					} else { // 如果是两张普通牌或者是一张癞子加普通牌并且牌眼是普通牌
						analyseItem.cbCardEye = switch_to_card_data(i);
					}

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) { // 只有将牌是癞子时，bMagicEye才为真，其他情况都已非癞子牌做牌眼
						if (this.magic_count(cbCardIndexTemp) == 2) { // 如果将牌是一对癞子牌
							cbCardEye = switch_to_card_data(this.get_magic_card_index(0));
							bMagicEye = true;
							break;
						} else if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							// 如果是一个癞子牌和一个其他牌，就不能将癞子作为牌眼，不然将牌判断会判断不了
							// 因为索引的先后问题，如果癞子牌索引靠前，牌眼就是不正确的
							if (is_magic_index(i)) {
								continue;
							} else {
								cbCardEye = switch_to_card_data(i);
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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

	// 分析扑克
	public boolean analyse_card_henan_hy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray,
			boolean has_feng) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 效验参数，要注释掉下面的两行，不然杠换章检测的时候回出错，cbWeaveCount是3 -_-
			/**
			 * if (((cbCardCount == 2) && (cbWeaveCount == 4)) == false) {
			 * return false; }
			 **/
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
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
						analyseItem.cbCardEye = switch_to_card_data(this.get_magic_card_index(0));
						analyseItem.bMagicEye = true;
					} else if (is_magic_index(i)) { // 如果是癞子索引
						continue;
					} else { // 如果是两张普通牌或者是一张癞子加普通牌并且牌眼是普通牌
						analyseItem.cbCardEye = switch_to_card_data(i);
					}

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) { // 只有将牌是癞子时，bMagicEye才为真，其他情况都已非癞子牌做牌眼
						if (this.magic_count(cbCardIndexTemp) == 2) { // 如果将牌是一对癞子牌
							cbCardEye = switch_to_card_data(this.get_magic_card_index(0));
							bMagicEye = true;
							break;
						} else if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							// 如果是一个癞子牌和一个其他牌，就不能将癞子作为牌眼，不然将牌判断会判断不了
							// 因为索引的先后问题，如果癞子牌索引靠前，牌眼就是不正确的
							if (is_magic_index(i)) {
								continue;
							} else {
								cbCardEye = switch_to_card_data(i);
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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
	 * 测试-分析牌型
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param analyseItemArray
	 * @param has_feng
	 */
	private void test_analyse_card(int cards_index[], boolean expectResult) {
		// 获取癞子牌数据
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = get_magic_card_index(i);
		}

		Runnable task = new AnalyseMJCardTask(cards_index, GameUtilConstants.MAX_CARD_TYPE, magic_cards_index, magic_card_count,
				getMjType().getValue(), expectResult);
	}

	public boolean analyse_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray, boolean has_feng) {
		boolean rst = this.analyse_card_(cards_index, weaveItem, cbWeaveCount, analyseItemArray, has_feng);

		test_analyse_card(cards_index, rst);

		return rst;
	}

	// 分析扑克
	public boolean analyse_card_(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray, boolean has_feng) {

		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2 || (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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

	// 分析扑克
	public boolean analyse_card_zhi_chi(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray, int card) {

		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI_FENG;
			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2 || (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI_FENG;
			for (int i = 0; i < mj_count; i++) {
				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
		}

		// 组合分析
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI_FENG;
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
						}

						// 设置牌眼
						analyseItem.cbCardEye = cbCardEye;
						analyseItem.bMagicEye = bMagicEye;

						// 插入结果
						if (cbCardEye != 0x35 && cbCardEye != 0x36 && cbCardEye != 0x37 && cbCardEye != card) {
							analyseItemArray.add(analyseItem);
						}
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
	public boolean analyse_card_js_zz(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray,
			boolean has_feng) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);

					return true;
				}
			}
			for (int i = 0; i < mj_count; i++) {
				if (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					analyseItem.cbCardEye = switch_to_card_data(i);
					if (cards_index[i] < 2 || this.is_magic_index(i) == true)
						analyseItem.bMagicEye = true;
					else
						analyseItem.bMagicEye = false;

					// 插入结果
					analyseItemArray.add(analyseItem);
					return true;
				}
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI;
			if (has_feng) {
				mj_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i : get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[1] = get_magic_card_index(0);
						kindItem[cbKindItemCount].cbValidIndex[2] = get_magic_card_index(0);
						cbKindItemCount++;
					}
				} // 同牌判断 end

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
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
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
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
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3 };
			KindItem pKindItem[] = new KindItem[4];
			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI;
				if (has_feng) {
					mj_count = GameConstants.MAX_ZI_FENG;
				}
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2) && !this.is_magic_index(i)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
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
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
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

	/// 癞子作为变牌
	public boolean is_laizi_hu(List<AnalyseItem> analyseItemArray, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = switch_to_cards_data(cards_index, cards);
		for (int j = 0; j < analyseItemArray.size(); j++) {
			AnalyseItem analyseItem = analyseItemArray.get(j);
			int magic_count = this.magic_count(cards_index);
			if (magic_count == 0) {
				return false;
			}
			boolean laizi = false;
			for (int x = 0; x < 4; x++) {
				if (analyseItem.cbWeaveKind[x] == GameConstants.WIK_PENG) {
					if (analyseItem.cbCardData[x][0] != analyseItem.cbCardData[x][0] || analyseItem.cbCardData[x][1] != analyseItem.cbCardData[x][0]
							|| analyseItem.cbCardData[x][2] != analyseItem.cbCardData[x][0]) {
						laizi = true;
						break;
					} else {
						if (analyseItem.cbCardData[x][0] == this.switch_to_card_data(this.get_magic_card_index(0))) {
							magic_count--;
						}
						if (analyseItem.cbCardData[x][1] == this.switch_to_card_data(this.get_magic_card_index(0))) {
							magic_count--;
						}
						if (analyseItem.cbCardData[x][2] == this.switch_to_card_data(this.get_magic_card_index(0))) {
							magic_count--;
						}
					}
				} else if (analyseItem.cbWeaveKind[x] == GameConstants.WIK_LEFT) {
					if (analyseItem.cbCardData[x][0] != analyseItem.cbCardData[x][0]
							|| analyseItem.cbCardData[x][1] != analyseItem.cbCardData[x][0] + 1
							|| analyseItem.cbCardData[x][2] != analyseItem.cbCardData[x][0] + 2) {
						laizi = true;
						break;
					} else {
						if (analyseItem.cbCardData[x][0] == this.switch_to_card_data(this.get_magic_card_index(0))) {
							magic_count--;
						}
						if (analyseItem.cbCardData[x][1] == this.switch_to_card_data(this.get_magic_card_index(0))) {
							magic_count--;
						}
						if (analyseItem.cbCardData[x][2] == this.switch_to_card_data(this.get_magic_card_index(0))) {
							magic_count--;
						}
					}
				}
			}
			if (!laizi) {
				if (analyseItem.bMagicEye && magic_count == 2) {
					return false;
				} else if (!analyseItem.bMagicEye) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean is_hu_lai_gen_an_gang(List<AnalyseItem> analyseItemArray, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		for (int j = 0; j < analyseItemArray.size(); j++) {
			for (int x = 0; x < 4; x++) {
				AnalyseItem analyseItem = analyseItemArray.get(j);
				if (analyseItem.cbWeaveKind[x] == GameConstants.WIK_PENG) {
					if (this.is_da_gen_card(analyseItem.cbCenterCard[x])) {
						return true;
					}
				}
			}
		}
		return false;
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
			if ((weaveItems[i].weave_kind != GameConstants.WIK_GANG && weaveItems[i].weave_kind != GameConstants.WIK_BU_ZHNAG)) {
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
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 地胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}
		if (player_action == GameConstants.WIK_XIAO_HU) {
			return 39;
		}

		// 硬扣等级
		if (player_action == GameConstants.WIK_YING_KUO) {
			return 35;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}
		if (player_action == GameConstants.WIK_SUO_GANG_1) {
			return 28;
		}
		if (player_action == GameConstants.WIK_SUO_GANG_2) {
			return 28;
		}
		if (player_action == GameConstants.WIK_SUO_GANG_3) {
			return 28;
		}

		// 补
		if (player_action == GameConstants.WIK_XIA_ZI_BU) {
			return 30;
		}

		// 补张牌等级
		if (player_action == GameConstants.WIK_BU_ZHNAG) {
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
		if (player_action == GameConstants.WIK_YAO_YI_SE) {
			return 30;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}
		if (player_action == GameConstants.WIK_SUO_PENG_1) {
			return 18;
		}
		if (player_action == GameConstants.WIK_SUO_PENG_2) {
			return 18;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 15;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT) {
			return 10;
		}

		return 0;
	}

	public int get_action_rank_luzhougui(int player_action, boolean duan_gang) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 80;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 70;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 60;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 50;
		}

		if (player_action == GameConstants.WIK_SUO_GANG_1) {
			return 30;
		}
		if (player_action == GameConstants.WIK_SUO_GANG_2) {
			return 30;
		}
		if (player_action == GameConstants.WIK_SUO_GANG_3) {
			return 30;
		}

		if (duan_gang) {
			if (player_action == GameConstants.WIK_SUO_PENG_1) {
				return 40;
			}
			if (player_action == GameConstants.WIK_SUO_PENG_2) {
				return 40;
			}
		}

		if (!duan_gang) {
			if (player_action == GameConstants.WIK_SUO_PENG_1) {
				return 30;
			}
			if (player_action == GameConstants.WIK_SUO_PENG_2) {
				return 30;
			}
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 20;
		}

		return 0;
	}

	/**
	 * 获取操作的优先等级 松原麻将
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank_sy(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 地胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		if (player_action == GameConstants.WIK_CHI_TING_LEFT || player_action == GameConstants.WIK_CHI_TING_CENTER
				|| player_action == GameConstants.WIK_CHI_TING_RIGHT || player_action == GameConstants.WIK_PENG_TING) {
			return 35;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}

		// 补
		if (player_action == GameConstants.WIK_XIA_ZI_BU) {
			return 30;
		}

		// 补张牌等级
		if (player_action == GameConstants.WIK_BU_ZHNAG) {
			return 30;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 15;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT) {
			return 10;
		}

		return 0;
	}

	public int get_action_rank_xc(int player_action) {
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}

		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 10;
		}

		return 0;
	}

	public int get_action_rank_couyise(int player_action, boolean is_quan_zi_hu) {
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		if (player_action == GameConstants.WIK_CHI_HU && is_quan_zi_hu == false) {
			return 45;
		}

		if (player_action == GameConstants.WIK_CHI_HU && is_quan_zi_hu == true) {
			return 40;
		}

		if (player_action == GameConstants.WIK_GANG || player_action == GameConstants.WIK_SUO_GANG_1 || player_action == GameConstants.WIK_SUO_GANG_2
				|| player_action == GameConstants.WIK_SUO_GANG_3 || player_action == GameConstants.WIK_SUO_GANG_4) {
			return 30;
		}

		if (player_action == GameConstants.WIK_PENG || player_action == GameConstants.WIK_SUO_PENG_1
				|| player_action == GameConstants.WIK_SUO_PENG_2) {
			return 20;
		}

		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT
				|| player_action == GameConstants.WIK_SUO_CHI_LEFT_1 || player_action == GameConstants.WIK_SUO_CHI_LEFT_2
				|| player_action == GameConstants.WIK_SUO_CHI_CENTER_1 || player_action == GameConstants.WIK_SUO_CHI_CENTER_2
				|| player_action == GameConstants.WIK_SUO_CHI_RIGHT_1 || player_action == GameConstants.WIK_SUO_CHI_RIGHT_2) {
			return 10;
		}

		return 0;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (max_index < index) {
				max_index = index;
			}
		}

		return max_index;
	}

	public int get_action_list_rank_luzhougui(int action_count, WeaveItem[] weaveItems, boolean duan_gang) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank_luzhougui(weaveItems[i].weave_kind, duan_gang);
			if (max_index < index) {
				max_index = index;
			}
		}

		return max_index;
	}

	// 获取动作序列最高等级 松原麻将
	public int get_action_list_rank_sy(int action_count, int action[]) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (max_index < index) {
				max_index = index;
			}
		}

		return max_index;
	}

	public int get_action_list_rank_xc(int action_count, int action[]) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank_xc(action[i]);
			if (max_index < index) {
				max_index = index;
			}
		}

		return max_index;
	}

	// 获取动作序列最高等级
	public int get_max_rank_action(int action_count, int action[], int notWik) {
		int max_index = 0;
		int result_action = GameConstants.WIK_NULL;
		for (int i = 0; i < action_count; i++) {
			if (notWik != GameConstants.WIK_NULL) {
				if (notWik == action[i]) {
					continue;
				}
			}
			int index = get_action_rank(action[i]);

			if (max_index < index) {
				max_index = index;
				result_action = action[i];
			}

		}

		return result_action;
	}

	public int get_action_list_rank_couyise(int action_count, int action[], boolean is_quan_zi_hu) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank_couyise(action[i], is_quan_zi_hu);
			if (max_index < index) {
				max_index = index;
			}

		}

		return max_index;
	}

	// 胡牌等级
	public int get_chi_hu_action_rank(ChiHuRight chiHuRight) {
		return 0;
	}

	// 转转麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_zz(ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
			// 抢杠胡
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
			// 素胡
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu = 2;
		}

		// 转转没有杠开
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_GANG_KAI)).is_empty()){
		// //杠开
		// wFanShu *= 2;
		// }

		return wFanShu;
	}

	// hz麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_hz(ChiHuRight chiHuRight, int palyerNum) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
			// 素胡
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu = 2;
		}

		// 转转没有杠开
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_GANG_KAI)).is_empty()){
		// //杠开
		// wFanShu *= 2;
		// }

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
			// 抢杠胡
			wFanShu = 2 * (palyerNum - 1);// 每人两分,被抢杠的人全包 playerNum 牌桌人数,兼容三人场算分
		}
		return wFanShu;
	}

	// hz麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_ll(ChiHuRight chiHuRight, int palyerNum) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
			// 素胡
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu = 2;
		}

		// 转转没有杠开
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_GANG_KAI)).is_empty()){
		// //杠开
		// wFanShu *= 2;
		// }

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
			// 抢杠胡
			wFanShu = 1;// 每人两分,被抢杠的人全包 playerNum 牌桌人数,兼容三人场算分
		}
		return wFanShu;
	}

	// hz麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_lxcg(ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DADOU)).is_empty()) {
			wFanShu = 3;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
			// 素胡
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIADOU)).is_empty()) {
			wFanShu = 2;
		}
		return wFanShu;
	}

	// hz麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_hnhz(ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
			// 素胡
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu = 2;
		}

		// 转转没有杠开
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_GANG_KAI)).is_empty()){
		// //杠开
		// wFanShu *= 2;
		// }

		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU)).is_empty()) {
			// 抢杠胡
			wFanShu = 6;// 没人两分,被抢杠的人全包
		}
		return wFanShu;
	}

	// 湖南郴州麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_hncz(ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
			// 抢杠胡
			wFanShu = 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
			// 素胡
			wFanShu = 1;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu = 2;
		}
		return wFanShu;
	}

	/**
	 * 双鬼
	 * 
	 * @param chiHuRight
	 * @return
	 */
	public int get_chi_hu_action_rank_sg(ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu = 2;
		}

		return wFanShu;
	}

	/**
	 * 
	 * @param chiHuRight
	 * @return
	 */
	public int get_chi_hu_action_rank_xthh(ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_HEI_MO)).is_empty()) {
			wFanShu = 4;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RUAN_MO)).is_empty()) {
			wFanShu = 2;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_ZHUO_CHONG)).is_empty()) {
			wFanShu = 2;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RE_CHONG)).is_empty()) {
			wFanShu = 2;// 加杠钱
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_QIANG_GANG_HU)).is_empty()) {
			wFanShu = 3;
		}

		return wFanShu;
	}

	public int get_chi_hu_action_rank_ay(ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_DAN_DIAO)).is_empty()) {
			wFanShu += 1;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_KA_ZHANG)).is_empty()) {
			wFanShu += 1;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_BIAN_ZHANG)).is_empty()) {
			wFanShu += 1;
		}

		return wFanShu;
	}

	public int get_chi_hu_action_rank_lz(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		//
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_HENAN_DAN_DIAO)).is_empty()){
		// wFanShu += 1;
		// }else
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_HENAN_KA_ZHANG)).is_empty()){
		// wFanShu += 1;
		// }else
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_HENAN_BIAN_ZHANG)).is_empty()){
		// wFanShu += 1;
		// }

		return wFanShu;
	}

	public int get_chi_hu_action_rank_henan(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		//
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_HENAN_DAN_DIAO)).is_empty()){
		// wFanShu += 1;
		// }else
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_HENAN_KA_ZHANG)).is_empty()){
		// wFanShu += 1;
		// }else
		// if(!(chiHuRight.opr_and(MJGameConstants.CHR_HENAN_BIAN_ZHANG)).is_empty()){
		// wFanShu += 1;
		// }

		return wFanShu;
	}

	public int get_chi_hu_action_rank_he_nan_zhou_kou(ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu += 1;
		}

		return wFanShu;
	}

	public int get_chi_hu_action_rank_henan_lh(ChiHuRight chiHuRight) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu += 1;
		}

		return wFanShu;
	}

	public int get_chi_hu_action_rank_henanxy(ChiHuRight chiHuRight) {
		int wFanShu = 2;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_MENQING)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_BAZHANG)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_JIAZI)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_DUYING)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_QINGQUE)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_HUNQUE)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_QINGYISE)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_SANQIYING)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_SANQIJIANG)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_ZHONGWU)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_XY_LIANLIU)).is_empty()) {
			wFanShu += 1;
		}

		return wFanShu;
	}

	// 株洲麻将胡牌动作 优先级
	// 株洲麻将胡牌动作 优先级 --乘法 //碰碰胡 七小对 > 清一色 > 海底 ,杆上花 杆上炮 > 门清
	public int get_chi_hu_action_rank_zhuzhou_mutip(ChiHuRight chiHuRight) {

		int bigNumber = 0;
		// 大胡
		int wFanShu = 0;

		// 碰碰胡 七小对
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_258_JIANG)).is_empty()) {
				wFanShu += 4;
			} else {
				wFanShu += 3;
			}
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty())
			wFanShu += 4;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty())
			wFanShu += 8;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty())
			wFanShu += 16;

		boolean hasPengOrQi = wFanShu > 0 ? true : false;// 是否有 碰碰胡 七小对 一种
															// 上面牌型互斥 只会有一种
		if (hasPengOrQi)
			bigNumber++;

		boolean haQingyiSe = false;
		// 清一色
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			if (hasPengOrQi) {
				wFanShu = (wFanShu + 4) * 2;// 分数 == 牌型相加×2
				bigNumber++;
			} else {
				wFanShu += 4;
			}
			haQingyiSe = true;
			bigNumber++;
		}

		// 海底 ,杆上花 杆上炮 --也只会有一种 4分
		boolean hasGangOrHaiDi = isHasHaiDiOrGang(chiHuRight);
		if (hasGangOrHaiDi) {
			bigNumber++;
			if (hasPengOrQi || haQingyiSe) {// 是有一种大胡
				wFanShu = (wFanShu + 4) * 2;
			} else {
				wFanShu += 4;
			}
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
			if (bigNumber == 0) {
				wFanShu = 2;
			} else if (bigNumber == 1) {
				wFanShu *= 2;
			} else {
				wFanShu *= 2;
			}
		}

		// 小胡
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_DA_SI_XI)).is_empty())
		// wFanShu += 1;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_BAN_BAN_HU)).is_empty())//
		// 8000
		// wFanShu += 1;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_LIU_LIU_SHUN)).is_empty())
		// wFanShu += 1;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_QUE_YI_SE)).is_empty())//
		// 10000
		// wFanShu += 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty())
			wFanShu += 8;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty())
			wFanShu += 4;

		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty())
				wFanShu = 1;
		}
		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty())
				wFanShu = 1;
		}

		if (isGangPao(chiHuRight)) {
			wFanShu *= 3;
		}
		return wFanShu;
	}

	private boolean isHasHaiDiOrGang(ChiHuRight chiHuRight) {
		boolean has = false;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty())
			has = true;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty())
			has = true;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty())
			has = true;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_QIANG_GANG_HU)).is_empty())
		// wFanShu += 6;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty())
			has = true;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_QUAN_QIU_REN)).is_empty())
		// wFanShu += 6;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty())
			has = true;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty())
			has = true;
		return has;
	}

	private boolean isGangPao(ChiHuRight chiHuRight) {
		boolean has = false;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty())
			has = true;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty())
			has = true;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty())
			has = true;
		return has;
	}

	// 株洲麻将胡牌动作 优先级 --加法算分
	public int get_chi_hu_action_rank_zhuzhou(ChiHuRight chiHuRight) {
		// 大胡
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_258_JIANG)).is_empty()) {
				wFanShu += 4;
			} else {
				wFanShu += 3;
			}
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty())
			wFanShu += 4;

		// if (!(chiHuRight.opr_and(MJGameConstants.CHR_MEN_QING)).is_empty()) {
		// if(wFanShu==0){
		// wFanShu += 2;
		// }else{
		// wFanShu=wFanShu*2;//碰碰胡 清一色 *2
		// }
		// }

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty())
			wFanShu += 2;

		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty())
		// wFanShu += 6;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty())
			wFanShu += 8;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty())
			wFanShu += 4;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty())
			wFanShu += 4;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty())
			wFanShu += 4;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty())
			wFanShu += 4;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty())
			wFanShu += 8;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty())
			wFanShu += 4;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty())
			wFanShu += 6;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty())
			wFanShu += 4;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty())
			wFanShu += 6;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty())
			wFanShu += 16;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty())
			wFanShu += 4;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty())
			wFanShu += 4;

		// 小胡
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_DA_SI_XI)).is_empty())
		// wFanShu += 1;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_BAN_BAN_HU)).is_empty())//
		// 8000
		// wFanShu += 1;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_LIU_LIU_SHUN)).is_empty())
		// wFanShu += 1;
		// if
		// (!(chiHuRight.opr_and(MJGameConstants.CHR_XIAO_QUE_YI_SE)).is_empty())//
		// 10000
		// wFanShu += 1;

		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty())
				wFanShu = 1;
		}
		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty())
				wFanShu = 1;
		}

		if (isGangPao(chiHuRight)) {
			wFanShu *= 3;
		}
		return wFanShu;
	}

	// 长沙麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_cs(ChiHuRight chiHuRight) {
		// 大胡
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_PENGPENG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_JIANGJIANG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QING_YI_SE))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAI_DI_LAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAI_DI_PAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QI_XIAO_DUI))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 12;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI))
				wFanShu += 12;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_GANG_KAI))
				wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QIANG_GANG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_GANG_SHANG_PAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QUAN_QIU_REN))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 18;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI))
				wFanShu += 18;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
			wFanShu += 12;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI))
				wFanShu += 12;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 12;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO))
				wFanShu += 12;
		}

		// 小胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
			wFanShu += 1;

		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
				wFanShu = 1;
				if (chiHuRight.is_mul(GameConstants.CHR_SHU_FAN)) {
					wFanShu = 2;
				}
			}

		}
		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
				wFanShu = 1;
				if (chiHuRight.is_mul(GameConstants.CHR_ZI_MO)) {
					wFanShu = 2;
				}
			}

		}

		return wFanShu;
	}

	// 长沙麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_yytdh(ChiHuRight chiHuRight) {
		// 大胡
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_PENGPENG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_JIANGJIANG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QING_YI_SE))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAI_DI_LAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAI_DI_PAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QI_XIAO_DUI))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 12;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI))
				wFanShu += 12;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_GANG_KAI))
				wFanShu += 6;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QIANG_GANG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_GANG_SHANG_PAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QUAN_QIU_REN))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 18;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI))
				wFanShu += 18;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO))
				wFanShu += 6;
		}

		// 小胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
			wFanShu += 1;

		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
				wFanShu = 1;
				if (chiHuRight.is_mul(GameConstants.CHR_SHU_FAN)) {
					wFanShu = 2;
				}
			}

		}
		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
				wFanShu = 1;
				if (chiHuRight.is_mul(GameConstants.CHR_ZI_MO)) {
					wFanShu = 2;
				}
			}

		}

		return wFanShu;
	}

	// 长沙麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_cs_lx(ChiHuRight chiHuRight) {
		// 大胡
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			wFanShu += 6;

		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 12;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
			wFanShu += 6;
		}
		// if
		// (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty())
		// {
		// wFanShu += 6;
		// if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QIANG_GANG_HU))
		// wFanShu += 6;
		// }

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 18;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 6;
		}

		// 小胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_JING_TONG_YU_NV)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_SAN_TON)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_YI_ZHI_HUA)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BU_BU_GAO)).is_empty())
			wFanShu += 1;
		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
				wFanShu = 1;
			}

		}
		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
				wFanShu = 1;
			}

		}

		return wFanShu;
	}

	// 长沙麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_hy258(ChiHuRight chiHuRight, int fen) {
		// 大胡
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_PENGPENG_HU))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_JIANGJIANG_HU))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QING_YI_SE))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAI_DI_LAO))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAI_DI_PAO))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QI_XIAO_DUI))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 12 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI))
				wFanShu += 12 / fen;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_GANG_KAI))
				wFanShu += 6 / fen;
		}
		/*
		 * if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).
		 * is_empty()) { wFanShu += 6 / fen; if
		 * (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QIANG_GANG_HU)) wFanShu +=
		 * 6 / fen; }
		 */

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_GANG_SHANG_PAO))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
			wFanShu += 6 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_QUAN_QIU_REN))
				wFanShu += 6 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 18 / fen;
			if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI))
				wFanShu += 18 / fen;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
			wFanShu += 12 / fen;
			/*
			 * if (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI))
			 * wFanShu += 6/ fen;
			 */
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 12 / fen;
			/*
			 * if
			 * (chiHuRight.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)
			 * ) wFanShu += 6 / fen;
			 */
		}

		// 小胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
			wFanShu += 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
			wFanShu += 1;
		boolean flag = false;
		if (!(chiHuRight.opr_and(GameConstants_HY258.CHR_HUNAN_HY258_MEN_QING)).is_empty()) {
			wFanShu += 1;
			if (chiHuRight.is_mul(GameConstants_HY258.CHR_HUNAN_HY258_MEN_QING))
				wFanShu += 1;
			flag = true;
		}

		if ((chiHuRight.single_da_hu == 0 && wFanShu == 1 && flag) || wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
				wFanShu += 1;
			}

		}
		if ((chiHuRight.single_da_hu == 0 && wFanShu == 1 && flag) || wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
				wFanShu += 1;
			}

		}

		return wFanShu;
	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		// MAX_NIAO_CARD
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = this.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count_yytdh(int cards_data[], int card_num) {
		// MAX_NIAO_CARD
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (!is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = this.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}

	public int get_pick_niao_count_new_hz(int cards_data[], int card_num) {
		if (cards_data.length > 0) {
			if (cards_data[0] == 0x35) {
				return 10;
			} else {
				return get_card_value(cards_data[0]);
			}
		}

		return 0;
	}

	/**
	 * 抓金鸟，每次抓鸟 都不满足1 5 9 直到抓完 算金鸟,金鸟算全中
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_jin_niao(int cards_data[], int card_num) {
		int cbPickNum = 0;// 中的鸟的数量
		int noPickNum = 0;// 没中鸟的数量
		for (int i = 0; i < card_num; i++) {
			if (is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = this.get_card_value(cards_data[i]);
			if (nValue != 1 || nValue != 5 || nValue != 9) {
				noPickNum++;
			} else {
				cbPickNum++;
			}

		}

		if (card_num == noPickNum) {
			return card_num;
		}

		return cbPickNum;
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
		case GameConstants.WIK_PENG: // 碰牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		case GameConstants.WIK_BU_ZHNAG: // 杠牌操作
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
		case GameConstants.WIK_XIAO_CHAO_TIAN: // 杠牌操作
		case GameConstants.WIK_DA_CHAO_TIAN: {
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		default: {
			// logger.error("get_weave_card:invalid cbWeaveKind");
		}
		}

		return 0;
	}

	// 获取组合
	public int get_weave_card_huangshi(int cbWeaveKind, int cbCenterCard, int cbCardBuffer[]) {
		// 组合扑克
		switch (cbWeaveKind) {
		case GameConstants.WIK_SHOW_CARD: // 左吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard + 1;
			cbCardBuffer[2] = cbCenterCard + 2;

			return 3;
		}
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
		case GameConstants.WIK_PENG: // 碰牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		case GameConstants.WIK_BU_ZHNAG: // 杠牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		case GameConstants.WIK_DIAN_XIAO:
		case GameConstants.WIK_HUI_TOU_XIAO: {
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		case GameConstants.WIK_XIAO_CHAO_TIAN: // 杠牌操作
		case GameConstants.WIK_DA_CHAO_TIAN: {
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		default: {
			// logger.error("get_weave_card:invalid cbWeaveKind");
		}
		}

		return 0;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);// (card&MJGameConstants.LOGIC_MASK_VALUE);
		int cbColor = get_card_color(card);// (card&MJGameConstants.LOGIC_MASK_COLOR);
		return (((cbValue >= 1) && (cbValue <= 9) && (cbColor <= 2)) || ((cbValue >= 1) && (cbValue <= 9) && (cbColor == 3))
				|| ((cbValue >= 1) && (cbValue <= 6) && (cbColor == 4)));
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants.MAX_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = color * 9 + value - 1;
		return index;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data(int card_index) {
		if (card_index >= GameConstants.MAX_INDEX) {
			return GameConstants.MAX_INDEX;
		}
		return ((card_index / 9) << 4) | (card_index % 9 + 1);
	}

	// 扑克转换
	public int switch_to_cards_index(int cards_data[], int start_index, int card_count, int cards_index[]) {
		// 设置变量
		// 转换扑克
		for (int i = 0; i < card_count; i++) {
			cards_index[switch_to_card_index(cards_data[start_index + i])]++;
		}

		if (MJTable.DEBUG_CARDS_MODE) {
			// 调试模式
			// for (int i=0;i<card_count;i++)
			// {
			// cards_index[switch_to_card_index(cards_data[start_index+i])]--;
			// }
			// cards_index[switch_to_card_index(0x05)]++;
			// cards_index[switch_to_card_index(0x22)]++;
		}

		return card_count;
	}

	/**
	 * 河南武陟麻将，亮四打一玩法的时候，转换一定量的牌数据为牌索引
	 */
	public int switch_to_cards_index_lsdy(int cards_data[], int start_index, int card_count, int cards_index[], int[] lsdyIndex) {
		for (int i = 0; i < 4; i++) {
			lsdyIndex[switch_to_card_index(cards_data[start_index + i])]++;
		}
		for (int i = 4; i < card_count; i++) {
			cards_index[switch_to_card_index(cards_data[start_index + i])]++;
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

		boolean errorFlag = false;
		for (int i = 0; i < GameConstants.MAX_INDEX && errorFlag == false; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
					if (cbPosition == 14) {
						errorFlag = true;
						break;
					}
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	public int switch_to_cards_data_luzhougui(int cards_index[], int cards_data[], int pai_se) {
		int cbPosition = 0;

		for (int i = 0; i < cards_index[switch_to_card_index(0x35)]; i++) {
			cards_data[cbPosition++] = 0x35;
		}

		if (pai_se == 1) {
			for (int i = 9; i < 18; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 18; i < 27; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 27; i < 34; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 0; i < 9; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
		} else if (pai_se == 2) {
			for (int i = 0; i < 9; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 18; i < 27; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 27; i < 34; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 9; i < 18; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
		} else if (pai_se == 3) {
			for (int i = 0; i < 9; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 9; i < 18; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 27; i < 34; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 18; i < 27; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
		} else if (pai_se == 0) {
			boolean errorFlag = false;
			for (int i = 0; i < GameConstants.MAX_ZI_FENG && errorFlag == false; i++) {
				if (i == switch_to_card_index(0x35))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
						if (cbPosition == 14) {
							errorFlag = true;
							break;
						}
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
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
	 * @param max_index最大手牌值
	 * @return
	 */
	public int switch_to_cards_data(int cards_index[], int cards_data[], int max_index) {
		// 转换扑克
		int cbPosition = 0;

		for (int m = 0; m < this._magic_card_count; m++) {
			for (int i = 0; i < cards_index[this._magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(this._magic_card_index[m]);
			}
		}
		boolean errorFlag = false;
		for (int i = 0; i < max_index && errorFlag == false; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
					if (cbPosition == 14) {
						errorFlag = true;
						break;
					}
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	public int switch_to_cards_data_sichuan(int cards_index[], int cards_data[], int pai_se) {
		int cbPosition = 0;

		for (int m = 0; m < _magic_card_count; m++) {
			for (int i = 0; i < cards_index[_magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(_magic_card_index[m]) + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		if (pai_se == 1) {
			for (int i = 9; i < 18; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 18; i < 27; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 27; i < 34; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 0; i < 9; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
		} else if (pai_se == 2) {
			for (int i = 0; i < 9; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 18; i < 27; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 27; i < 34; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 9; i < 18; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
		} else if (pai_se == 3) {
			for (int i = 0; i < 9; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 9; i < 18; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 27; i < 34; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
			for (int i = 18; i < 27; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i]; j++) {
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
		} else if (pai_se == 0) {
			boolean errorFlag = false;
			for (int i = 0; i < GameConstants.MAX_ZI_FENG && errorFlag == false; i++) {
				if (is_magic_index(i))
					continue;
				if (cards_index[i] > 0) {
					for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
						if (cbPosition == 14) {
							errorFlag = true;
							break;
						}
						cards_data[cbPosition++] = switch_to_card_data(i);
					}
				}
			}
		}

		return cbPosition;
	}

	public int switch_to_cards_data_gzcg(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;

		boolean errorFlag = false;
		for (int i = 0; i < GameConstants.MAX_INDEX && errorFlag == false; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
					if (cbPosition == 14) {
						errorFlag = true;
						break;
					}
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	public int switch_to_cards_data_ezhou(int cards_index[], int cards_data[]) {
		int cbPosition = 0;

		for (int i = 0; i < cards_index[Constants_EZ.HZ_INDEX]; i++)
			cards_data[cbPosition++] = Constants_EZ.HZ_CARD;

		for (int m = 0; m < this._magic_card_count; m++) {
			for (int i = 0; i < cards_index[this._magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(this._magic_card_index[m]);
			}
		}

		boolean errorFlag = false;
		for (int i = 0; i < GameConstants.MAX_INDEX && errorFlag == false; i++) {
			if (this.is_magic_index(i) || i == Constants_EZ.HZ_INDEX)
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
					if (cbPosition == 14) {
						errorFlag = true;
						break;
					}
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	/**
	 * 湖北黄石麻将，牌型数据转换，癞子、红中往前面靠，如果发财可以杠，也要往前靠
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @param can_fa_cai_gang
	 * @return
	 */
	public int switch_to_cards_data_huangshi(int cards_index[], int cards_data[], boolean can_fa_cai_gang) {
		int cbPosition = 0;

		for (int m = 0; m < _magic_card_count; m++) {
			for (int i = 0; i < cards_index[_magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(_magic_card_index[m]);
			}
		}

		for (int i = 0; i < cards_index[Constants_HuangShi.HONG_ZHONG_INDEX]; i++)
			cards_data[cbPosition++] = Constants_HuangShi.HONG_ZHONG_CARD;

		if (can_fa_cai_gang) {
			if (_magic_card_index[0] != Constants_HuangShi.FA_CAI_INDEX) {
				for (int i = 0; i < cards_index[Constants_HuangShi.FA_CAI_INDEX]; i++)
					cards_data[cbPosition++] = Constants_HuangShi.FA_CAI_CARD;
			}
		}

		boolean errorFlag = false;
		for (int i = 0; i < GameConstants.MAX_INDEX && errorFlag == false; i++) {
			if (is_magic_index(i) || i == Constants_HuangShi.HONG_ZHONG_INDEX)
				continue;

			if (can_fa_cai_gang && i == Constants_HuangShi.FA_CAI_INDEX)
				continue;

			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
					if (cbPosition == 14) {
						errorFlag = true;
						break;
					}
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}

		return cbPosition;
	}

	public int switch_to_cards_data_couyise(int cards_index[], int cards_data[], int pi_zi) {
		int cbPosition = 0;

		for (int m = 0; m < _magic_card_count; m++) {
			for (int i = 0; i < cards_index[_magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(_magic_card_index[m]);
			}
		}

		int pi_zi_index = switch_to_card_index(pi_zi);

		if (pi_zi_index >= 0 && pi_zi_index < GameConstants.MAX_INDEX)
			for (int i = 0; i < cards_index[pi_zi_index]; i++)
				cards_data[cbPosition++] = pi_zi;

		for (int i = 0; i < cards_index[Constants_EZ.HZ_INDEX]; i++)
			cards_data[cbPosition++] = Constants_EZ.HZ_CARD;

		boolean errorFlag = false;
		for (int i = 0; i < GameConstants.MAX_INDEX && errorFlag == false; i++) {
			if (this.is_magic_index(i) || i == Constants_EZ.HZ_INDEX || i == pi_zi_index)
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
					if (cbPosition == 14) {
						errorFlag = true;
						break;
					}
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	// 排序,根据牌值排序
	public boolean sort_card_list(int card_data[]) {
		int cbCardCount = card_data.length;
		// 数目过虑
		if (cbCardCount == 0 || cbCardCount > GameConstants.MAX_COUNT)
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

	public boolean checkWanZiByIndex(int[] cards_index) {
		// TODO Auto-generated method stub
		for (int i = 0; i < cards_index.length; i++) {
			if (cards_index[i] == 0)
				continue;
			if (get_card_color(switch_to_card_data(i)) == 0)
				return true;
		}
		return false;
	}

	/**
	 * 检查有没万子
	 * 
	 * @param card
	 * @return
	 */
	public boolean checkWanZi(int card) {
		int color = get_card_color(card);

		return color == 0;
	}

	public int queryMinIndex(int[] card) {
		if (card.length == 0) {
			return 0;
		}
		int min = card[0];
		int minIndex = 0;
		for (int i = 1; i < card.length; i++) {
			if (card[i] == 0) {
				continue;
			}
			if (min > card[i]) {
				min = card[i];
				minIndex = i;
			}
		}
		return minIndex;
	}

	// 找到value适合插入的位置
	public int findInsertIndex(int[] sortArray, int value, int left, int right) {
		int middleIndex = (right - left) / 2 + left;
		middleIndex = middleIndex >= sortArray.length ? sortArray.length - 1 : middleIndex;
		int middleValue = sortArray[middleIndex];
		if (right - left < 2) {
			if (value > sortArray[right - 1])
				return right;
			else if (value > sortArray[left]) {
				return left + 1;
			} else {
				return left;
			}
		} else if (middleValue < value) {
			return findInsertIndex(sortArray, value, middleIndex + 1, right);
		} else if (middleValue > value) {
			return findInsertIndex(sortArray, value, left, middleIndex);
		} else {
			return middleIndex + 1;
		}
	}

	// 类型子项
	public static class KindItem {
		public int cbWeaveKind;// 组合类型
		public int cbCenterCard;// 中心扑克
		public int cbCardIndex[] = new int[3];// 扑克索引
		public int cbValidIndex[] = new int[3];// 实际扑克索引
		// 子组合类型。比如，weaveKind是风吃的时候，可以有好几种不同的风吃组合，用来确认时间的组合牌数据。
		public int cbSubWeaveKind;
		// 子组合的重复数目。比如，weaveKind是风吃的时候，最多可以有4个重复的‘东西南’。
		// 比如，19牌型的组合，最多可以重复2次
		public int cbMultiCount;

		public KindItem() {

		}
	}

	// 分析子项
	public static class AnalyseItem {
		public int cbCardEye;//// 牌眼扑克
		public boolean bMagicEye;// 牌眼是否是王霸
		public int cbWeaveKind[] = new int[4];// 组合类型
		public int cbCenterCard[] = new int[4];// 中心扑克
		public int cbCardData[][] = new int[4][4]; // 实际扑克
		// 子组合类型。比如，weaveKind是风吃的时候，可以有好几种不同的风吃组合，用来确认时间的组合牌数据。
		public int cbSubWeaveKind[] = new int[4];
		// 子组合的重复数目。比如，weaveKind是风吃的时候，最多可以有4个重复的‘东西南’
		// 比如，19牌型的组合，最多可以重复2次
		public int cbMultiCount[] = new int[4];
	}

	/**
	 * @return the _hua_card_index
	 */
	public int[] get_hua_card_index() {
		return _hua_card_index;
	}

	public void add_hua_card_index(int card_index) {
		_hua_card_index[_hua_count] = card_index;
		_hua_count++;
	}

	public int get_hua_count() {
		return _hua_count;
	}

	public void clean_hua_index() {
		_hua_count = 0;
	}

	public MJType getMjType() {
		return mjType;
	}

	/**
	 * 瑞金吃牌判断
	 * 
	 * @param cards_index
	 * @param cur_card
	 * @return
	 */
	public Map<Integer, List<Integer>> check_chi_ruijin(int seat_index, int[] cards_index, int cur_card, boolean has_jia_bao) {
		if (!is_valid_card(cur_card)) { // 如果牌值非法
			return null;
		}
		if (is_magic_card(cur_card)) { // 如果牌值非法
			return null;
		}

		Map<Integer, List<Integer>> result_map = new HashMap<Integer, List<Integer>>();

		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}
		// 插入扑克
		tmp_cards_index[switch_to_card_index(cur_card)]++;

		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		if (!has_jia_bao && switch_to_card_index(cur_card) >= GameConstants.MAX_ZI_FENG) { // 没有假宝的花牌不可吃
			return null;
		} else if (has_jia_bao && switch_to_card_index(cur_card) >= GameConstants.MAX_ZI_FENG) {
			// 有假宝的花牌吃处理
			int chang_card = switch_to_card_data(get_magic_card_index(0));

			if (get_card_color(chang_card) == 3) { // 如果是风牌
				check_chi_fen(result_map, chang_card, 0, eat_type_check, tmp_cards_index, cur_card);
			} else {
				for (int i = 0; i < eat_type_check.length; i++) {
					check_chi_basics(result_map, chang_card, i, eat_type_check, tmp_cards_index, cur_card);
				}
			}
		} else {
			if (get_card_color(cur_card) == 3) { // 如果是风牌
				check_chi_fen(result_map, cur_card, 0, eat_type_check, tmp_cards_index, GameConstants.INVALID_CARD);
			} else {
				for (int i = 0; i < eat_type_check.length; i++) {
					check_chi_basics(result_map, cur_card, i, eat_type_check, tmp_cards_index, GameConstants.INVALID_CARD);
					// 有花牌作代替万能牌本身值
					if (has_jia_bao) {
						check_chi_basics_jia_bao(result_map, cur_card, i, eat_type_check, tmp_cards_index);
					}
				}
			}
		}
		return result_map;
	}

	/**
	 * 组合正常万条筒吃牌
	 * 
	 * @param result_map
	 *            保存吃牌集合
	 * @param cur_card
	 *            检测牌
	 * @param time
	 *            吃类型index
	 * @param eat_type_check
	 *            吃类型
	 * @param tmp_cards_index
	 *            手牌
	 */
	public void check_chi_basics(Map<Integer, List<Integer>> result_map, int cur_card, int time, int[] eat_type_check, int[] tmp_cards_index,
			int hua_card) {

		int card_value = get_card_value(cur_card);
		int card_index = switch_to_card_index(cur_card);
		if ((card_value >= (time + 1)) && (card_value <= 7 + time)) {
			int first_card_index = card_index - time;
			if (check_chi_card_enough(tmp_cards_index, first_card_index, card_index)) {
				int first_card = cur_card;
				if (hua_card != GameConstants.INVALID_CARD) {
					first_card = hua_card;
				}

				// 或运算把吃碰中心牌放进int值
				int center_card = 0 | first_card;
				// 游标index,确定下次放入牌值位置
				int index = 1;

				for (int j = first_card_index; j < (first_card_index + 3); j++) {
					if (j == card_index) {
						continue;
					}
					int[] rtArr = build_center_card(center_card, switch_to_card_data(j), index);
					index = rtArr[0];
					center_card = rtArr[1];
				}
				buildResultMap(result_map, center_card, eat_type_check, time);
			}
		}
	}

	/**
	 * 构建吃牌组合
	 * 
	 * @param result_map
	 * @param center_card
	 * @param eat_type_check
	 * @param time
	 */
	public void buildResultMap(Map<Integer, List<Integer>> result_map, int center_card, int[] eat_type_check, int time) {
		List<Integer> center_card_list = result_map.get(eat_type_check[time]);
		if (center_card_list == null) {
			center_card_list = new ArrayList<Integer>();
		}
		center_card_list.add(center_card);
		result_map.put(eat_type_check[time], center_card_list);
	}

	/**
	 * 组合正常万条筒吃牌(花牌作代替万能牌本身值)
	 * 
	 * @param result_map
	 *            保存吃牌集合
	 * @param cur_card
	 *            检测牌
	 * @param time
	 *            吃类型index
	 * @param eat_type_check
	 *            吃类型
	 * @param tmp_cards_index
	 *            手牌
	 */
	public void check_chi_basics_jia_bao(Map<Integer, List<Integer>> result_map, int cur_card, int time, int[] eat_type_check,
			int[] tmp_cards_index) {

		int card_value = get_card_value(cur_card);
		int card_index = switch_to_card_index(cur_card);
		if ((card_value >= (time + 1)) && (card_value <= 7 + time)) {
			int first_card_index = card_index - time;
			int[] center_card_arr = new int[3];
			int card_num = 0;
			boolean bao_chi = false; // 存在宝吃

			// 可吃正常牌
			for (int k = first_card_index; k < (first_card_index + 3); k++) {
				if (is_magic_index(k)) {
					bao_chi = true;
				}
				if (k != get_magic_card_index(0) && tmp_cards_index[k] != 0) {
					center_card_arr[card_num] = k;
					card_num++;
				}
			}

			if (bao_chi && card_num >= 2) {
				List<Integer> hua_index_list = new ArrayList<Integer>();

				for (int i = GameConstants.MAX_ZI_FENG; i < (GameConstants.MAX_ZI_FENG + 4); i++) {
					if (tmp_cards_index[i] > 0) {
						hua_index_list.add(i);
					}
				}

				// 玩家手上是否存在花牌
				if (hua_index_list.size() > 0) {
					// 或运算把吃碰中心牌放进int值
					int center_card = 0 | cur_card;
					// 游标index,确定下次放入牌值位置
					int index = 1;
					for (int k = 0; k < card_num; k++) {
						if (center_card_arr[k] != card_index && center_card_arr[k] != 0) {

							int[] rtArr = build_center_card(center_card, switch_to_card_data(center_card_arr[k]), index);
							index = rtArr[0];
							center_card = rtArr[1];
							// 加入花牌
							for (Integer integer : hua_index_list) {
								int center_card_hua = center_card;
								int[] rtArr1 = build_center_card(center_card_hua, switch_to_card_data(integer), index);
								center_card_hua = rtArr1[1];
								buildResultMap(result_map, center_card_hua, eat_type_check, time);
							}
						}
					}

				}
			}
		}
	}

	/**
	 * 判断吃牌是否足够
	 * 
	 * @param tmp_cards_index
	 * @param first_card_index
	 * @return
	 */
	public boolean check_chi_card_enough(int[] tmp_cards_index, int first_card_index, int card_index) {
		for (int i = 0; i < 3; i++) {
			int check_index = first_card_index + i;
			if (check_index == card_index) {
				continue;
			}
			if (tmp_cards_index[check_index] == 0 || is_magic_index(check_index)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 组合中心牌
	 * 
	 * @param center_card
	 * @param card
	 * @param index
	 * @return
	 */
	public int[] build_center_card(int center_card, int card, int index) {
		int coler = get_card_color(card);
		int value = get_card_value(card);
		center_card = (int) (center_card + coler * Math.pow(16, index + 2) + value * Math.pow(16, index + 1));
		return new int[] { index += 2, center_card };
	}

	/**
	 * 杆牌判断,花牌可以杠
	 * 
	 * @param card_index
	 * @param cur_card
	 * @return
	 */
	public int estimate_gang_card_out_card_hua(int card_index[], int cur_card, List<Integer> gang_list) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		if (is_magic_card(cur_card)) {
			return GameConstants.WIK_NULL;
		}

		int _card_index = switch_to_card_index(cur_card);

		int has_magic = 0;
		/*
		 * if ((is_magic_card(cur_card) || _card_index >=
		 * GameConstants.MAX_ZI_FENG) && has_jia_bao) { has_magic = 1; } else if
		 * (is_magic_card(cur_card) && !has_jia_bao) { has_magic = 2; }
		 */
		if (_card_index >= GameConstants.MAX_ZI_FENG && has_jia_bao) {
			has_magic = 2;
		}

		if (has_magic == 1) {
			// 花牌数量
			List<Integer> huaCard = new ArrayList<Integer>();
			for (int i = 0; i < 4; i++) {
				int tmp_index = switch_to_card_index(0x38) + i;
				if (card_index[tmp_index] > 0) {
					huaCard.add(switch_to_card_data(tmp_index));
				}
			}
			// 万能牌数量
			int card_count = card_index[get_magic_card_index(0)];
			int center_card = 0 | cur_card;
			if (card_count + huaCard.size() >= 3) {
				int magic_card = switch_to_card_data(get_magic_card_index(0));
				int coler = get_card_color(magic_card);
				int value = get_card_value(magic_card);
				if (card_count > 0) {
					huaCard.add(magic_card);
				}

				// 花牌组合的杠
				int index = 1;
				for (int i = 0; i < huaCard.size() - 2; i++) {
					int[] rtArr = build_center_card(center_card, huaCard.get(i), index);
					int index2 = rtArr[0];
					int center_card1 = rtArr[1];
					for (int j = i + 1; j < huaCard.size() - 1; j++) {
						int[] rtArr1 = build_center_card(center_card1, huaCard.get(j), index2);
						int center_card_hua = rtArr1[1];
						int index3 = rtArr1[0];
						for (int k = j + 1; k < huaCard.size(); k++) {
							int[] rtArr2 = build_center_card(center_card_hua, huaCard.get(k), index3);
							gang_list.add(rtArr2[1]);
						}
					}
				}

				// 两个万能牌和一个花牌的杠
				if (card_count == 2 && huaCard.size() > 0) {
					int index2 = 1;
					int center_crd_two = center_card;
					for (int i = 0; i < 2; i++) {
						center_crd_two = (int) (center_crd_two + coler * Math.pow(16, index2 + 2) + value * Math.pow(16, index2 + 1));
						index2 += 2;
					}
					for (int j = 0; j < huaCard.size(); j++) {
						if (magic_card == huaCard.get(j)) {
							continue;
						}
						int[] rtArr1 = build_center_card(center_crd_two, huaCard.get(j), index2);
						int center_card_hua = rtArr1[1];
						gang_list.add(center_card_hua);
					}
				}

				// 全部是万能牌的杠
				if (card_count == 3) {
					for (int i = 0; i < card_count; i++) {
						center_card = (int) (center_card + coler * Math.pow(16, index + 2) + value * Math.pow(16, index + 1));
						index += 2;
					}
					gang_list.add(center_card);
				}
				return GameConstants.WIK_GANG;
			} else {
				return GameConstants.WIK_NULL;
			}

		} else if (has_magic == 2) {
			// 杠判断
			List<Integer> card_list = new ArrayList<Integer>();

			if (_card_index >= GameConstants.MAX_ZI_FENG) {
				for (int i = 0; i < 4; i++) {
					int index = GameConstants.MAX_ZI_FENG + i;
					if (_card_index != index && card_index[index] > 0) {
						card_list.add(switch_to_card_data(index));
					}
				}
			}

			if (card_list.size() == 3) {
				int center_card = 0 | cur_card;
				int index = 1;
				for (int i = 0; i < card_list.size() - 1; i++) {
					int[] rtArr = build_center_card(center_card, card_list.get(i), index);
					index = rtArr[0];
					center_card = rtArr[1];
				}
				gang_list.add(center_card);
				return GameConstants.WIK_GANG;
			} else {
				return GameConstants.WIK_NULL;
			}
		} else {
			return card_index[switch_to_card_index(cur_card)] == 3 ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
		}
	}

	/**
	 * 碰牌判断,花牌可以碰
	 * 
	 * @param card_index
	 * @param cur_card
	 * @return
	 */
	public int check_peng_hua(int card_index[], int cur_card, List<Integer> center_card_list) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		if (is_magic_card(cur_card)) {
			return GameConstants.WIK_NULL;
		}

		int _card_index = switch_to_card_index(cur_card);

		int has_magic = 0;
		/*
		 * if ((is_magic_card(cur_card) || _card_index >=
		 * GameConstants.MAX_ZI_FENG) && has_jia_bao) { has_magic = 1; } else if
		 * (is_magic_card(cur_card) && !has_jia_bao) { has_magic = 2; }
		 */
		if (_card_index >= GameConstants.MAX_ZI_FENG && has_jia_bao) {
			has_magic = 2;
		}

		if (has_magic == 1) {
			// 花牌数量
			List<Integer> huaCard = new ArrayList<Integer>();
			for (int i = 0; i < 4; i++) {
				int tmp_index = switch_to_card_index(0x38) + i;
				if (card_index[tmp_index] > 0) {
					huaCard.add(switch_to_card_data(tmp_index));
				}
			}
			// 万能牌数量
			int card_count = card_index[get_magic_card_index(0)];
			int center_card = 0 | cur_card;
			if (card_count + huaCard.size() >= 2) {
				int magic_card = switch_to_card_data(get_magic_card_index(0));
				if (card_count > 0) {
					huaCard.add(magic_card);
				}

				// 花牌组合的碰
				int index = 1;
				for (int i = 0; i < huaCard.size() - 1; i++) {
					int[] rtArr = build_center_card(center_card, huaCard.get(i), index);
					int index2 = rtArr[0];
					int center_card1 = rtArr[1];
					for (int j = i + 1; j < huaCard.size(); j++) {
						int[] rtArr1 = build_center_card(center_card1, huaCard.get(j), index2);
						int center_card_hua = rtArr1[1];
						center_card_list.add(center_card_hua);
					}
				}
				// 有两个万能牌的碰
				if (card_count == 2) {
					int coler = get_card_color(magic_card);
					int value = get_card_value(magic_card);
					center_card = (int) (center_card + coler * Math.pow(16, index + 2) + value * Math.pow(16, index + 1));
					index += 2;
					center_card = (int) (center_card + coler * Math.pow(16, index + 2) + value * Math.pow(16, index + 1));
					index += 2;
					center_card_list.add(center_card);
				}
				return GameConstants.WIK_PENG;
			} else {
				return GameConstants.WIK_NULL;
			}

		} else if (has_magic == 2) {
			// 碰牌判断
			List<Integer> card_list = new ArrayList<Integer>();

			if (_card_index >= GameConstants.MAX_ZI_FENG) {
				for (int i = 0; i < 4; i++) {
					int index = GameConstants.MAX_ZI_FENG + i;
					if (_card_index != index && card_index[index] > 0) {
						card_list.add(switch_to_card_data(index));
					}
				}
			}

			if (card_list.size() >= 2) {
				int center_card = 0 | cur_card;
				int index = 1;
				for (int i = 0; i < card_list.size() - 1; i++) {
					int[] rtArr = build_center_card(center_card, card_list.get(i), index);
					int index2 = rtArr[0];
					int center_card1 = rtArr[1];
					for (int j = i + 1; j < card_list.size(); j++) {
						int[] rtArr1 = build_center_card(center_card1, card_list.get(j), index2);
						int center_card2 = rtArr1[1];
						center_card_list.add(center_card2);
					}
				}
				return GameConstants.WIK_PENG;
			} else {
				return GameConstants.WIK_NULL;
			}
		} else {
			return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
		}

	}

	public void check_chi_fen(Map<Integer, List<Integer>> result_map, int cur_card, int time, int[] eat_type_check, int[] tmp_cards_index,
			int hua_card) {
		int card_value = get_card_value(cur_card);
		int card_index = switch_to_card_index(cur_card);

		int start_index = 0;// 检测牌开始位置
		int count = 0; // 检测次数

		if (card_value > 4) { // 中发白
			start_index = switch_to_card_index(0x35);
			count = 3;
		} else { // 东南西北
			start_index = switch_to_card_index(0x31);
			count = 4;
		}

		// 判断手牌值 筛选牌值
		int cards[] = new int[count];
		for (int i = 0; i < count; i++) {
			cards[i] = -1;
		}
		int number = 0;
		boolean has_hua = false;
		for (int i = 0; i < count; i++) {
			if (is_magic_index(start_index + i) && get_has_jia_bao()) {
				has_hua = true;
			}
			if (!is_magic_index(start_index + i) && (start_index + i) != card_index && tmp_cards_index[start_index + i] > 0) {
				number++;
				cards[i] = switch_to_card_data(start_index + i);
			}
		}

		List<Integer> huaArr = new ArrayList<Integer>();

		if (has_hua) {
			for (int i = 0; i < 4; i++) {
				int tmp_index = switch_to_card_index(0x38) + i;
				if (tmp_cards_index[tmp_index] > 0) {
					huaArr.add(switch_to_card_data(tmp_index));
				}
			}
		}

		// 判断是是否够组合
		if (number >= 2) {
			int first_card = cur_card;
			if (hua_card != GameConstants.INVALID_CARD) {
				first_card = hua_card;
			}
			Integer center_card = 0 | first_card;
			int index = 1;

			for (int i = 0; i < (count - 1); i++) {
				int card = cards[i];
				if (card == -1) {
					continue;
				}
				int[] rtArr = build_center_card(center_card, card, index);
				int next_index = rtArr[0];
				int temCenterCardFirst = rtArr[1];

				for (int j = i + 1; j < count; j++) {
					int card1 = cards[j];
					if (card1 == -1) {
						continue;
					}
					int[] rtArr1 = build_center_card(temCenterCardFirst, card1, next_index);
					int temCenterCardTwo = rtArr1[1];
					buildResultMap(result_map, temCenterCardTwo, eat_type_check, time);
				}
			}
		}

		int count1 = number;

		// 有花牌的情况
		if (hua_card == GameConstants.INVALID_CARD && huaArr.size() > 0 && (count1 + (huaArr.size() > 0 ? 1 : 0)) >= 2 && has_jia_bao) {
			int first_card = cur_card;
			int center_card = 0 | first_card;
			int index = 1;
			for (int i = 0; i < count; i++) {
				if (is_magic_card(cur_card)) {
					continue;
				}
				int card = cards[i];
				if (card == -1 || is_magic_card(card)) {
					continue;
				}
				int[] rtArr = build_center_card(center_card, card, index);
				int temCenterCardFirst = rtArr[1];
				int next_index = rtArr[0];
				for (Integer integer : huaArr) {
					int[] rtArr1 = build_center_card(temCenterCardFirst, integer, next_index);
					int temCenterCardTwo = rtArr1[1];
					buildResultMap(result_map, temCenterCardTwo, eat_type_check, time);
				}
			}
		}
	}

	/**
	 * 一条龙牌型
	 * 
	 * @param cards_index
	 * @param weave_count
	 * @return
	 */
	public boolean is_yi_tiao_long(int[] cards_index, int weave_count) {
		if (weave_count > 1) {
			return false;
		}

		int cbCardIndexTemp[] = Arrays.copyOf(cards_index, cards_index.length);

		boolean has_enough_cards = true;
		for (int i = 0; i < 9; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, new int[] {}, 0);
			if (can_win) {
				return true;
			}
		}

		cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);

		has_enough_cards = true;
		for (int i = 9; i < 18; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, new int[] {}, 0);
			if (can_win) {
				return true;
			}
		}

		cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);

		has_enough_cards = true;
		for (int i = 18; i < 27; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, new int[] {}, 0);
			if (can_win) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 一条龙牌型，能风吃
	 * 
	 * @param cards_index
	 * @param weave_count
	 * @return
	 */
	public boolean is_yi_tiao_long_fc(int[] cards_index, int weave_count) {
		if (weave_count > 1) {
			return false;
		}

		int cbCardIndexTemp[] = Arrays.copyOf(cards_index, cards_index.length);

		boolean has_enough_cards = true;
		for (int i = 0; i < 9; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, -1, new int[] {}, 0);
			if (can_win) {
				return true;
			}
		}

		cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);

		has_enough_cards = true;
		for (int i = 9; i < 18; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, -1, new int[] {}, 0);
			if (can_win) {
				return true;
			}
		}

		cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);

		has_enough_cards = true;
		for (int i = 18; i < 27; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, -1, new int[] {}, 0);
			if (can_win) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 获取下一张牌
	 * 
	 * @param iCard
	 * @return
	 */
	public int get_next_card(int iCard) {
		int cur_value = get_card_value(iCard);
		int cur_color = get_card_color(iCard);
		int iNextCard = 0;
		int itemp = 0;
		if (cur_color < 3) {
			if (cur_value < 9) {
				itemp = cur_value + 1;
			} else {
				itemp = 1;
			}
		} else {
			if (cur_value == 4) {
				itemp = 1;
			} else if (cur_value == 7) {
				itemp = 5;
			} else {
				itemp = cur_value + 1;
			}
		}
		iNextCard = (cur_color << 4) + itemp;
		return iNextCard;
	}

}
