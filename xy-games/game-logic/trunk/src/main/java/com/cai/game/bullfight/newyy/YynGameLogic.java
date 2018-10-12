package com.cai.game.bullfight.newyy;

import java.util.Arrays;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;

public class YynGameLogic {
	/**
	 * 金牌牛的特殊值
	 */
	private static final int OX_ESPECIAL_VALUE = 100;

	public int get_card_value(int card) {
		int card_value = card & GameConstants.LOGIC_MASK_VALUE;
		return card_value > 10 ? 10 : card_value;
	}

	public int get_real_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public void random_card_data(int return_cards[], final int src_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = src_cards[i];
		}
		random_cards(card_data, return_cards, card_count);
	}

	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);
	}

	public int getRuleValue(Map<Integer, Integer> ruleMaps, int game_rule) {
		if (!ruleMaps.containsKey(game_rule)) {
			return 0;
		}
		return ruleMaps.get(game_rule);
	}

	/***
	 * 获取牛牛类型
	 * 
	 * @param card_data
	 * @param card_count
	 * @return
	 */
	public int get_card_type(int card_data[], int card_count, Map<Integer, Integer> ruleMaps) {
		if (card_count != GameConstants.OX_MAX_CARD_COUNT)
			return 0;

		int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);

		for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
			temp_card_data[i] = card_data[i];
			card_real_value_count[get_real_card_value(temp_card_data[i])]++;
		}

		ox_sort_card_list(temp_card_data, card_count);

		boolean same_color = true;
		boolean line_card = true;
		@SuppressWarnings("unused")
		boolean special = false;

		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_real_card_value(temp_card_data[0]);

		if (first_value == 13) {
			// 牌牌型过后，如果第一张牌是K，最后一张牌是A；将最后一张A换成-‘花色’*16+9-的牌值
			if (get_real_card_value(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
				temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1] = get_card_color(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) * 16 + 9;
				special = true;
			}
		}

		for (int i = 1; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
			if (get_card_color(temp_card_data[i]) != first_color) {
				same_color = false;
			}
			if (get_real_card_value(temp_card_data[i]) != first_value - i) {
				line_card = false;
			}
			if (same_color == false && line_card == false) {
				break;
			}
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_TONG_HUA_SHUN) == 1) {
			// 同花顺 10倍
			if ((same_color == true) && (line_card == true)) {
				return GameConstants.YY_OX_VALUE_TONG_HUA_SHUN;
			}
		}

		int four_count = 0;
		for (int i = 0; i < card_count; i++) {
			four_count = 1;

			int temp_real_value = get_real_card_value(card_data[i]);

			for (int j = 0; j < card_count; j++) {
				if ((i != j) && (temp_real_value == get_real_card_value(card_data[j]))) {
					four_count++;
				}
			}

			if (four_count == 4) {
				break;
			}
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_OX_BOOM) == 1) {
			// 炸弹 8倍
			if (four_count == 4) {
				return GameConstants.YY_OX_VALUE_ZHA_DAN;
			}
		}

		int sum = 0;
		boolean all_card_less_than_five = true;

		for (int i = 0; i < card_count; i++) {
			if (get_card_value(card_data[i]) > 5) {
				all_card_less_than_five = false;
			}
			sum += get_card_value(card_data[i]);
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_SI_SHI_DA) == 1) {
			// 40大 7倍
			if (sum >= 40) {
				return GameConstants.YY_OX_VALUE_SI_SHI_DA;
			}
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_OX_WUXIAONIU) == 1) {
			// 10小 7倍
			if (sum <= 10 && all_card_less_than_five) {
				return GameConstants.YY_OX_VALUE_SHI_XIAO;
			}
		}

		boolean san_zhang = false;
		boolean two_zhang = false;
		int san_zhang_card_value = 0;
		for (int i = 0; i < 14; i++) {
			if (card_real_value_count[i] == 3) {
				san_zhang = true;
				san_zhang_card_value = i;
			}
			if (card_real_value_count[i] == 2) {
				two_zhang = true;
			}
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_HU_LU_NIU) == 1) {
			// 葫芦 6倍
			if (san_zhang == true && two_zhang == true) {
				return GameConstants.YY_OX_VALUE_HU_LU;
			}
		}

		if ((same_color == true) && getRuleValue(ruleMaps, GameConstants.GAME_RULE_TONG_HUA_NIU) == 1) {
			// 同花 5倍
			return GameConstants.YY_OX_VALUE_TONG_HUA;
		}

		if ((line_card == true) && getRuleValue(ruleMaps, GameConstants.GAME_RULE_SHUN_ZI_NIU) == 1) {
			// 顺子 5倍
			return GameConstants.YY_OX_VALUE_SHUN_ZI;
		}

		int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		sum = 0;
		for (int i = 0; i < card_count; i++) {
			temp_value[i] = get_card_value(card_data[i]);
			sum += temp_value[i];
		}

		// 存储最大的牌型
		int max_card_value = 0;

		for (int i = 0; i < card_count; i++) {
			for (int j = i + 1; j < card_count; j++) {
				if ((sum - temp_value[i] - temp_value[j]) % 10 == 0) {
					// 如果5张牌里，减掉2张牌的值，剩余的3张牌的牌值是10的倍数
					int tmp_sum = temp_value[i] + temp_value[j];
					int tmp_max_card_value = (tmp_sum > 10) ? (tmp_sum - 10) : tmp_sum;

					if (max_card_value < tmp_max_card_value) {
						max_card_value = tmp_max_card_value;
					}
				}
			}
		}

		// 存储最大金牌牛的牌型
		int jin_pai_niu = 0;

		if (san_zhang == true) {
			for (int i = 0; i < card_count; i++) {
				if (temp_value[i] != san_zhang_card_value) {
					jin_pai_niu += temp_value[i];
				}
			}

			jin_pai_niu = jin_pai_niu > 10 ? jin_pai_niu - 10 : jin_pai_niu;
		}

		if (jin_pai_niu >= max_card_value && jin_pai_niu > 0) {
			return jin_pai_niu + OX_ESPECIAL_VALUE;
		} else if (max_card_value > 0) {
			return max_card_value;
		}

		return 0;
	}

	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_data
	 * @param card_count
	 * @return
	 */
	public int get_times_one(int card_data[], int card_count, Map<Integer, Integer> ruleMaps) {
		int card_times = 0;

		if (card_count != GameConstants.OX_MAX_CARD_COUNT)
			return 0;

		card_times = get_card_type(card_data, card_count, ruleMaps);

		if (card_times > OX_ESPECIAL_VALUE) {
			card_times -= OX_ESPECIAL_VALUE;
		}

		if (card_times < 7) {
			card_times = 1;
		} else if (card_times == 7) {
			if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_NIU_QI_WU_BEI_SHU) == 1) {
				card_times = 1;
			} else {
				card_times = 2;
			}
		} else if (card_times == 8) {
			card_times = 2;
		} else if (card_times == 9) {
			if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_NIU_JIU_LIANG_BEI) == 1) {
				card_times = 2;
			} else {
				card_times = 3;
			}
		} else if (card_times == 10) {
			if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_NIU_NIU_SAN_BEI) == 1) {
				card_times = 3;
			} else {
				card_times = 4;
			}
		} else if (card_times == GameConstants.YY_OX_VALUE_SHUN_ZI) {
			card_times = 5;
		} else if (card_times == GameConstants.YY_OX_VALUE_TONG_HUA) {
			card_times = 5;
		} else if (card_times == GameConstants.YY_OX_VALUE_HU_LU) {
			card_times = 6;
		} else if (card_times == GameConstants.YY_OX_VALUE_SHI_XIAO) {
			card_times = 7;
		} else if (card_times == GameConstants.YY_OX_VALUE_SI_SHI_DA) {
			card_times = 7;
		} else if (card_times == GameConstants.YY_OX_VALUE_ZHA_DAN) {
			card_times = 8;
		} else if (card_times == GameConstants.YY_OX_VALUE_TONG_HUA_SHUN) {
			card_times = 10;
		}

		return card_times;
	}

	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_data
	 * @param card_count
	 * @return
	 */
	public int get_times_two(int card_data[], int card_count, Map<Integer, Integer> ruleMaps) {
		int card_times = 0;

		if (card_count != GameConstants.OX_MAX_CARD_COUNT)
			return 0;

		card_times = get_card_type(card_data, card_count, ruleMaps);

		if (card_times > OX_ESPECIAL_VALUE) {
			card_times -= OX_ESPECIAL_VALUE;
		}

		if (card_times < 7) {
			card_times = 1;
		} else if (card_times == 7) {
			if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_NIU_QI_WU_BEI_SHU) == 1) {
				card_times = 1;
			} else {
				card_times = 2;
			}
		} else if (card_times == 8) {
			card_times = 2;
		} else if (card_times == 9) {
			if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_NIU_JIU_LIANG_BEI) == 1) {
				card_times = 2;
			} else {
				card_times = 3;
			}
		} else if (card_times == 10) {
			if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_NIU_NIU_SAN_BEI) == 1) {
				card_times = 3;
			} else {
				card_times = 4;
			}
		} else if (card_times == GameConstants.YY_OX_VALUE_SHUN_ZI) {
			card_times = 5;
		} else if (card_times == GameConstants.YY_OX_VALUE_TONG_HUA) {
			card_times = 5;
		} else if (card_times == GameConstants.YY_OX_VALUE_HU_LU) {
			card_times = 6;
		} else if (card_times == GameConstants.YY_OX_VALUE_SHI_XIAO) {
			card_times = 7;
		} else if (card_times == GameConstants.YY_OX_VALUE_SI_SHI_DA) {
			card_times = 7;
		} else if (card_times == GameConstants.YY_OX_VALUE_ZHA_DAN) {
			card_times = 8;
		} else if (card_times == GameConstants.YY_OX_VALUE_TONG_HUA_SHUN) {
			card_times = 10;
		}

		return card_times;
	}

	/***
	 * 获取牛值
	 * 
	 * @param card_data
	 * @param card_count
	 * @return
	 */
	public boolean get_ox_card(int card_data[], int card_count, Map<Integer, Integer> ruleMaps) {
		if (card_count != GameConstants.OX_MAX_CARD_COUNT)
			return false;

		int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);

		for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
			temp_card_data[i] = card_data[i];
			card_real_value_count[get_real_card_value(temp_card_data[i])]++;
		}

		ox_sort_card_list(temp_card_data, card_count);

		boolean same_color = true;
		boolean line_card = true;

		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_real_card_value(temp_card_data[0]);

		if (first_value == 13) {
			if (get_real_card_value(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
				temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1] = get_card_color(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) * 16 + 9;
			}
		}

		for (int i = 1; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
			if (get_card_color(temp_card_data[i]) != first_color)
				same_color = false;
			if (get_real_card_value(temp_card_data[i]) != first_value - i)
				line_card = false;
			if (same_color == false && line_card == false)
				break;
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_TONG_HUA_SHUN) == 1) {
			// 同花顺 10倍
			if ((same_color == true) && (line_card == true))
				return true;
		}

		int four_count = 0;
		for (int i = 0; i < card_count; i++) {
			four_count = 1;
			int temp_real_value = get_real_card_value(card_data[i]);
			for (int j = 0; j < card_count; j++) {
				if ((i != j) && (temp_real_value == get_real_card_value(card_data[j])))
					four_count++;
			}
			if (four_count == 4)
				break;
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_OX_BOOM) == 1) {
			// 炸弹 8倍
			if (four_count == 4)
				return true;
		}

		boolean san_zhang = false;
		boolean two_zhang = false;
		int san_zhang_card_value = 0;
		for (int i = 0; i < 14; i++) {
			if (card_real_value_count[i] == 3) {
				san_zhang = true;
				san_zhang_card_value = i;
			}
			if (card_real_value_count[i] == 2) {
				two_zhang = true;
			}
		}

		int sum = 0;
		boolean all_card_less_than_five = true;

		for (int i = 0; i < card_count; i++) {
			if (get_card_value(card_data[i]) > 5) {
				all_card_less_than_five = false;
			}
			sum += get_card_value(card_data[i]);
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_SI_SHI_DA) == 1) {
			// 40大 7倍
			if (sum >= 40)
				return true;
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_OX_WUXIAONIU) == 1) {
			// 10小 7倍
			if (sum <= 10 && all_card_less_than_five)
				return true;
		}

		if (getRuleValue(ruleMaps, GameConstants.GAME_RULE_HU_LU_NIU) == 1) {
			// 葫芦 6倍
			if (san_zhang == true && two_zhang == true) {
				return true;
			}
		}

		if ((same_color == true) && getRuleValue(ruleMaps, GameConstants.GAME_RULE_TONG_HUA_NIU) == 1) {
			// 同花 5倍
			return true;
		}

		if ((line_card == true) && getRuleValue(ruleMaps, GameConstants.GAME_RULE_SHUN_ZI_NIU) == 1) {
			// 顺子 5倍
			return true;
		}

		// 设置变量
		int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int temp_card[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		for (int i = 0; i < card_count; i++) {
			temp_card[i] = card_data[i];
		}

		sum = 0;
		for (int i = 0; i < card_count; i++) {
			temp_value[i] = get_card_value(card_data[i]);
			sum += temp_value[i];
		}

		// 存储最大的牌型
		int max_card_value = 0;

		for (int i = 0; i < card_count; i++) {
			for (int j = i + 1; j < card_count; j++) {
				if ((sum - temp_value[i] - temp_value[j]) % 10 == 0) {
					// 如果5张牌里，减掉2张牌的值，剩余的3张牌的牌值是10的倍数
					int tmp_sum = temp_value[i] + temp_value[j];
					int tmp_max_card_value = (tmp_sum > 10) ? (tmp_sum - 10) : tmp_sum;

					if (max_card_value < tmp_max_card_value) {
						max_card_value = tmp_max_card_value;
					}
				}
			}
		}

		// 存储最大金牌牛的牌型
		int jin_pai_niu = 0;

		if (san_zhang == true) {
			for (int i = 0; i < card_count; i++) {
				if (temp_value[i] != san_zhang_card_value) {
					jin_pai_niu += temp_value[i];
				}
			}

			jin_pai_niu = jin_pai_niu > 10 ? jin_pai_niu - 10 : jin_pai_niu;
		}

		if (max_card_value > jin_pai_niu) {
			// 查找牛牛
			for (int i = 0; i < card_count; i++) {
				for (int j = i + 1; j < card_count; j++) {
					if ((sum - temp_value[i] - temp_value[j]) % 10 == 0) {
						// 如果有牛，将牌数据重新排列一下，之前是按牌值大小排列的，现在是前面3张是10的整数倍的三张牌值，后面两张是牛几的2张牌
						int count = 0;
						for (int k = 0; k < card_count; k++) {
							if (k != i && k != j) {
								card_data[count++] = card_data[k];
							}
						}

						card_data[count++] = temp_card[i];
						card_data[count++] = temp_card[j];

						return true;
					}
				}
			}
		} else {
			// 查找金牌牛，并重新排列牌值数据
			if (san_zhang == true) {
				int count = 0;

				for (int i = 0; i < card_count; i++) {
					if (temp_value[i] == san_zhang_card_value) {
						card_data[count++] = temp_card[i];
					}
				}

				for (int i = 0; i < card_count; i++) {
					if (temp_value[i] != san_zhang_card_value) {
						card_data[count++] = temp_card[i];
					}
				}

				return true;
			}
		}

		return false;
	}

	/***
	 * 排列扑克
	 * 
	 * @param card_data
	 * @param card_count
	 * @return
	 */
	public void ox_sort_card_list(int card_data[], int card_count) {
		// 转换数值
		int logic_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		for (int i = 0; i < card_count; i++) {
			logic_value[i] = get_real_card_value(card_data[i]);
		}

		// 排序操作
		boolean sorted = true;
		int temp_date, last = card_count - 1;
		do {
			sorted = true;
			for (int i = 0; i < last; i++) {
				if ((logic_value[i] < logic_value[i + 1]) || ((logic_value[i] == logic_value[i + 1]) && (card_data[i] < card_data[i + 1]))) {
					// 交换位置
					temp_date = card_data[i];
					card_data[i] = card_data[i + 1];
					card_data[i + 1] = temp_date;
					temp_date = logic_value[i];
					logic_value[i] = logic_value[i + 1];
					logic_value[i + 1] = temp_date;
					sorted = false;
				}
			}
			last--;
		} while (sorted == false);

		return;
	}

	public int get_boom_value(int card_data[], int card_count) {
		for (int i = 0; i < card_count; i++) {
			int four_count = 1;
			int temp_real_value = get_real_card_value(card_data[i]);
			for (int j = 0; j < card_count; j++) {
				if ((i != j) && (temp_real_value == get_real_card_value(card_data[j]))) {
					four_count++;
				}
			}

			if (four_count == 4) {
				return temp_real_value;
			}
		}
		return 0;
	}

	public int get_hu_lu_value(int card_data[], int card_count) {
		int value = 0;
		for (int i = 0; i < card_count; i++) {
			int four_count = 1;
			int temp_real_value = this.get_real_card_value(card_data[i]);
			for (int j = 0; j < card_count; j++) {
				if ((i != j) && (temp_real_value == get_real_card_value(card_data[j])))
					four_count++;
			}

			if (four_count > 2) {
				return temp_real_value;
			}
		}
		return value;
	}

	/***
	 * 对比扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @param card_date
	 * @param card_count
	 * @param card_count
	 * @return
	 */
	public int compare_card(int first_data[], int next_data[], int card_count, boolean first_ox, boolean next_ox, Map<Integer, Integer> ruleMaps) {
		if (first_ox != next_ox) {
			if (first_ox == true)
				return 1;
			if (next_ox == true)
				return -1;
		}

		int next_type = get_card_type(next_data, card_count, ruleMaps);
		int first_type = get_card_type(first_data, card_count, ruleMaps);

		if (first_type > OX_ESPECIAL_VALUE && next_type > OX_ESPECIAL_VALUE) {
			if (first_type > next_type) {
				return 1;
			} else if (first_type < next_type) {
				return -1;
			} else if (first_type == next_type) {
				int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
				int card_real_value_count[] = new int[14];

				for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
					temp_card_data[i] = first_data[i];
					card_real_value_count[get_real_card_value(temp_card_data[i])]++;
				}

				int first_card_value = 0;
				for (int i = 0; i < 14; i++) {
					if (card_real_value_count[i] == 3) {
						first_card_value = i;
					}
				}

				Arrays.fill(temp_card_data, 0);
				Arrays.fill(card_real_value_count, 0);

				for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
					temp_card_data[i] = next_data[i];
					card_real_value_count[get_real_card_value(temp_card_data[i])]++;
				}

				int next_card_value = 0;
				for (int i = 0; i < 14; i++) {
					if (card_real_value_count[i] == 3) {
						next_card_value = i;
					}
				}

				if (first_card_value > next_card_value) {
					return 1;
				} else if (first_card_value < next_card_value) {
					return -1;
				}
			}
		} else if (first_type > OX_ESPECIAL_VALUE && next_type < GameConstants.YY_OX_VALUE_SHUN_ZI) {
			if (first_type - OX_ESPECIAL_VALUE >= next_type) {
				return 1;
			} else {
				return -1;
			}
		} else if (first_type < GameConstants.YY_OX_VALUE_SHUN_ZI && next_type > OX_ESPECIAL_VALUE) {
			if (next_type - OX_ESPECIAL_VALUE >= first_type) {
				return -1;
			} else {
				return 1;
			}
		} else if (first_type < OX_ESPECIAL_VALUE && next_type < OX_ESPECIAL_VALUE) {
			if (first_type > next_type)
				return 1;
			else if (first_type < next_type)
				return -1;
		}

		first_type = first_type > OX_ESPECIAL_VALUE ? first_type - OX_ESPECIAL_VALUE : first_type;
		next_type = next_type > OX_ESPECIAL_VALUE ? next_type - OX_ESPECIAL_VALUE : next_type;

		if (first_type > next_type)
			return 1;
		else if (first_type < next_type)
			return -1;

		if (first_type == 0 && next_type == 0) {
			// 判断有没有村长牌型
			for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
				if (first_data[i] == 0x39) {
					return 1;
				}
			}
			for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
				if (next_data[i] == 0x39) {
					return -1;
				}
			}
		}

		int first_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int next_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		for (int i = 0; i < card_count; i++) {
			first_temp[i] = first_data[i];
			next_temp[i] = next_data[i];
		}

		ox_sort_card_list(first_temp, card_count);
		ox_sort_card_list(next_temp, card_count);

		if (next_type == GameConstants.YY_OX_VALUE_ZHA_DAN) {
			int frist_boom_value = get_boom_value(first_temp, card_count);
			int next_boom_value = get_boom_value(next_temp, card_count);
			if (next_boom_value != frist_boom_value)
				return frist_boom_value > next_boom_value ? 1 : -1;
		}

		if (next_type == GameConstants.YY_OX_VALUE_HU_LU) {
			int first_hu_lu_value = get_hu_lu_value(first_data, card_count);
			int next_hu_lu_value = get_hu_lu_value(next_data, card_count);
			if (next_hu_lu_value != first_hu_lu_value)
				return first_hu_lu_value > next_hu_lu_value ? 1 : -1;
		}

		int next_max_value = get_real_card_value(next_temp[0]);
		int first_max_value = get_real_card_value(first_temp[0]);

		int first_color = get_card_color(first_temp[0]);
		int next_color = get_card_color(next_temp[0]);

		if (next_type == GameConstants.OX_SHUN_ZI_VALUE || next_type == GameConstants.OX_TONG_HUA_XHUN_VALUE) {
			if (next_max_value == 13 && get_real_card_value(next_temp[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
				next_max_value = 14;
				next_color = get_card_color(next_temp[GameConstants.OX_MAX_CARD_COUNT - 1]);
			}
			if (first_max_value == 13 && get_real_card_value(first_temp[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
				first_max_value = 14;
				first_color = get_card_color(first_temp[GameConstants.OX_MAX_CARD_COUNT - 1]);
			}
		}

		if (next_max_value > first_max_value)
			return -1;
		else if (next_max_value < first_max_value)
			return 1;

		if (first_color > next_color)
			return 1;
		else
			return -1;

	}

	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
	}
}
