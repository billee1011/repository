package com.cai.game.util;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.MJGameLogic;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;

public class AnalyseCardUtil {
	private static final Random random = new Random();

	/**
	 * 胡牌分析算法，基于查表法，针对万条筒风牌，可以带癞子，癞子牌最多8张。在服务端启动时，利用
	 * TableManager.getInstance().load()方法，将表格数据装载到TableManager的私有静态域manager里。
	 * 然后使用AnalyseCardUtil.getInstance().analyse_win_by_cards_index()进行胡牌分析。
	 * 
	 * @param hand_cards_index
	 *            手牌数据数组，容量为34，单个值为0-4，所有牌加到一起最多13张。测试时固定为14张。
	 * @param cur_card_index
	 *            当前牌的索引，范围为0-33。
	 * @param magic_cards_index
	 *            鬼牌数据，一个二维数组，实际可用大小为total_magic_card_indexs，里面 的整型值表示，鬼牌的索引。
	 * @param total_magic_card_indexs
	 *            一共有多少个鬼牌索引，可以是0、1或者2。
	 * @return true，如果能胡牌；false，如果不能胡。
	 */
	public static boolean analyse_win_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}
		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	public static boolean analyse_win_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index, int total_magic_card_indexs,
			boolean isZimo) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		boolean is_magic = false;
		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			if (cur_card_index == magic_cards_index[i]) {
				is_magic = true;
			}
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}
		// 不是自摸宝牌只能是本身牌
		if (!isZimo && is_magic) {
			tmp_hand_cards_index[cur_card_index]++;
			total_magic--;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/** 永州麻将转用 */
	public static boolean analyse_win_by_cards_index_yz(int[] hand_cards_index, int cur_card_index, int magic_cards_index, boolean is_peng_hu) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = tmp_hand_cards_index[magic_cards_index];
		tmp_hand_cards_index[magic_cards_index] = tmp_hand_cards_index[31];// 红中的索引
		tmp_hand_cards_index[31] = 0;// 红中的索引

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, is_peng_hu, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * @param hand_cards_index
	 *            手牌数据数组，容量为34，单个值为0-4，所有牌加到一起最多13张。测试时固定为14张。
	 * @param cur_card_index
	 *            当前牌的索引，范围为0-33。
	 * @param magic_cards_index
	 *            鬼牌数据，一个二维数组，实际可用大小为total_magic_card_indexs，里面 的整型值表示，鬼牌的索引。
	 * @param total_magic_card_indexs
	 *            一共有多少个鬼牌索引，可以是0、1或者2。
	 * @param max_lai_zi_count
	 *            做多使用的赖子数
	 * @return true，如果能胡牌；false，如果不能胡。
	 * 
	 */
	public static boolean analyse_win_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index, int total_magic_card_indexs,
			int max_lai_zi_count) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		boolean flag_break = false;
		for (int i = 0; i < total_magic_card_indexs && max_lai_zi_count != 0; i++) {
			int hand_magic_count = tmp_hand_cards_index[magic_cards_index[i]];
			for (int k = 0; k < hand_magic_count; k++) {
				total_magic++;
				tmp_hand_cards_index[magic_cards_index[i]]--;

				if (total_magic == max_lai_zi_count) {
					flag_break = true;
					break;
				}
			}
			if (flag_break) {
				break;
			}
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 幺久，所有组成的顺子、刻子、将牌里都包含1或9
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_win_yao_jiu(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index, int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table_yao_jiu(tmp_hand_cards_index, total_magic, p_table))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 幺久，所有组成的顺子、刻子、将牌里都包含1或9
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_win_yao_jiu_ytdgk(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[magic_cards_index[0]] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table_yao_jiu(tmp_hand_cards_index, total_magic, p_table))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	public static boolean analyse_win_by_cards_index_4_hand_cards_index_length(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[hand_cards_index.length];
		for (int i = 0; i < hand_cards_index.length; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < hand_cards_index.length)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 桃江麻将，别人出王牌时，胡牌分析时不能当成癞子牌
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_win_by_cards_index_taojiang(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[magic_cards_index[0]] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	public static boolean analyse_win_by_cards_index_da_lian(int[] hand_cards_index, int cur_card_index, int total_magic, boolean is_check_peng_hu) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;
		if (total_magic == 1)
			tmp_hand_cards_index[cur_card_index]--;

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, is_check_peng_hu, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 山西吕梁麻将，别人出王牌时，胡牌分析时当前牌不当成癞子牌
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_win_by_cards_index_shanxill(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[cur_card_index] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 胡卡张判断
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static int[] analyse_ckeck_ka_bian_diao(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index, int total_magic_card_indexs,
			boolean flag) {
		boolean f = false;
		for (int i = 0; i < magic_cards_index.length; i++) {
			// 癞子判断
			if (cur_card_index == magic_cards_index[i]) {
				f = true;
			}
		}
		int action = GameConstants.WIK_NULL;
		if (f) {// 癞子当所有牌
			for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE - GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
				int card = switch_to_card_data(i);

				int val = get_card_value(card);
				if (val == 1 || val == 9) {
					continue;
				}

				if (val == 2 && flag) {
					continue;
				}
				action = checke_ka_bian(hand_cards_index, i, magic_cards_index, total_magic_card_indexs);
				if (action != GameConstants.WIK_NULL) {
					return new int[] { action, i };
				}
			}
		} else {
			action = checke_ka_bian(hand_cards_index, cur_card_index, magic_cards_index, total_magic_card_indexs);
		}

		return new int[] { action, cur_card_index };
	}

	// 获取数值
	public static int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public static int switch_to_card_data(int card_index) {
		if (card_index >= GameConstants.MAX_INDEX) {
			return GameConstants.MAX_INDEX;
		}
		return ((card_index / 9) << 4) | (card_index % 9 + 1);
	}

	/**
	 * 校验胡牌
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @param type
	 * @return
	 */
	public static int checke_ka_bian(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index, int total_magic_card_indexs) {
		int type = 0;
		// 剔除顺子后的牌组能胡牌,则满足卡需求
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		int needMagic = 0;
		int total_magic = 0;
		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (tmp_hand_cards_index[cur_card_index + 1] > 0) {
			tmp_hand_cards_index[cur_card_index + 1]--;
		} else {
			needMagic++;
		}
		if (tmp_hand_cards_index[cur_card_index - 1] > 0) {
			tmp_hand_cards_index[cur_card_index - 1]--;
		} else {
			needMagic++;
		}
		if (needMagic > 0 && total_magic < needMagic) {
			return GameConstants.WIK_NULL;
		} else {
			for (int i = 0; i < total_magic_card_indexs; i++) {
				int can_count = total_magic - needMagic;
				if (i > 0) {
					can_count -= 4;
				}
				if (can_count > 4) {
					can_count = 4;
				}
				total_magic += tmp_hand_cards_index[magic_cards_index[i]];
				tmp_hand_cards_index[magic_cards_index[i]] = can_count;
			}
		}

		boolean can_win = analyse_win_by_cards_index(tmp_hand_cards_index, GameConstants.INVALID_CARD, magic_cards_index, total_magic_card_indexs);
		if (can_win) {
			type = 1;
		}
		return type;
	}

	/**
	 * 癞子成句判断，先计算王牌数，并清空手里的王牌，再把王牌当普通牌加到手里
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_lai_zi_cheng_ju_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE + 2];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		boolean can_win = false;
		// TODO: 从手牌里减掉当前牌(癞子牌)做为牌眼、刻子、左吃、中吃、右吃，如果任意一种都可以胡牌，那就能癞子成句
		if (tmp_hand_cards_index[cur_card_index] >= 2) {
			if (tmp_hand_cards_index[cur_card_index] == 3) {
				tmp_hand_cards_index[cur_card_index] -= 3;
				can_win = analyse_win_by_cards_index(tmp_hand_cards_index, GameUtilConstants.MAX_CARD_TYPE, magic_cards_index,
						total_magic_card_indexs);
				tmp_hand_cards_index[cur_card_index] += 3;

				if (can_win)
					return true;
			}

			tmp_hand_cards_index[cur_card_index] -= 2;
			can_win = analyse_win_by_cards_index(tmp_hand_cards_index, GameUtilConstants.MAX_CARD_TYPE, magic_cards_index, total_magic_card_indexs);
			tmp_hand_cards_index[cur_card_index] += 2;

			if (can_win)
				return true;
		} else {
			if (cur_card_index < GameUtilConstants.MAX_CARD_TYPE) { // 风牌不能吃
				boolean can_eat_left = false;
				boolean can_eat_center = false;
				boolean can_eat_right = false;

				if (cur_card_index % 9 == 0) { // 牌值为1，只能左吃
					can_eat_left = true;
				} else if (cur_card_index % 9 == 8) { // 牌值为9，只能右吃
					can_eat_right = true;
				} else if (cur_card_index % 9 == 1) { // 牌值为2，可以左吃或中吃
					can_eat_left = true;
					can_eat_center = true;
				} else if (cur_card_index % 9 == 7) { // 牌值为8，可以中吃或右吃
					can_eat_center = true;
					can_eat_right = true;
				} else { // 牌值为3-7，可以左吃、中吃、右吃
					can_eat_left = true;
					can_eat_center = true;
					can_eat_right = true;
				}

				can_win = analyse_left_center_right_eat(tmp_hand_cards_index, cur_card_index, magic_cards_index, total_magic_card_indexs,
						can_eat_left, can_eat_center, can_eat_right);
			}
		}

		return false;
	}

	private static boolean analyse_left_center_right_eat(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs, boolean can_eat_left, boolean can_eat_center, boolean can_eat_right) {
		boolean can_win = false;

		if (can_eat_left) {
			if (hand_cards_index[cur_card_index] > 0 && hand_cards_index[cur_card_index + 1] > 0 && hand_cards_index[cur_card_index + 2] > 0) {
				hand_cards_index[cur_card_index]--;
				hand_cards_index[cur_card_index + 1]--;
				hand_cards_index[cur_card_index + 2]--;

				can_win = analyse_win_by_cards_index(hand_cards_index, GameUtilConstants.MAX_CARD_TYPE, magic_cards_index, total_magic_card_indexs);

				if (can_win)
					return can_win;

				hand_cards_index[cur_card_index]++;
				hand_cards_index[cur_card_index + 1]++;
				hand_cards_index[cur_card_index + 2]++;
			}
		}

		if (can_eat_center) {
			if (hand_cards_index[cur_card_index - 1] > 0 && hand_cards_index[cur_card_index] > 0 && hand_cards_index[cur_card_index + 1] > 0) {
				hand_cards_index[cur_card_index - 1]--;
				hand_cards_index[cur_card_index]--;
				hand_cards_index[cur_card_index + 1]--;

				can_win = analyse_win_by_cards_index(hand_cards_index, GameUtilConstants.MAX_CARD_TYPE, magic_cards_index, total_magic_card_indexs);

				if (can_win)
					return can_win;

				hand_cards_index[cur_card_index - 1]++;
				hand_cards_index[cur_card_index]++;
				hand_cards_index[cur_card_index + 1]++;
			}
		}

		if (can_eat_right) {
			if (hand_cards_index[cur_card_index - 2] > 0 && hand_cards_index[cur_card_index - 1] > 0 && hand_cards_index[cur_card_index] > 0) {
				hand_cards_index[cur_card_index - 2]--;
				hand_cards_index[cur_card_index - 1]--;
				hand_cards_index[cur_card_index]--;

				can_win = analyse_win_by_cards_index(hand_cards_index, GameUtilConstants.MAX_CARD_TYPE, magic_cards_index, total_magic_card_indexs);

				if (can_win)
					return can_win;

				hand_cards_index[cur_card_index - 2]++;
				hand_cards_index[cur_card_index - 1]++;
				hand_cards_index[cur_card_index]++;
			}
		}

		return can_win;
	}

	/**
	 * 判断手牌是不是碰碰胡
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_peng_hu_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, true, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 判断手牌是不是碰碰胡
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_peng_hu_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs, boolean zimo) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		boolean is_magic = false;
		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			if (cur_card_index == magic_cards_index[i]) {
				is_magic = true;
			}
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}
		// 不是自摸宝牌只能是本身牌
		if (!zimo && is_magic) {
			tmp_hand_cards_index[cur_card_index]++;
			total_magic--;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, true, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 判断手牌是不是碰碰胡
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_peng_hu_by_cards_index_ytdgk(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[magic_cards_index[0]] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, true, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 判断手牌是不是碰碰胡
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_peng_hu_by_cards_index_ydcutlaizi(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}
		// 别家打出了癞子 需要转换成实际牌值计算 这里要减掉一个癞子
		total_magic--;

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, true, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 判断手牌是不是碰碰胡
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_peng_hu_by_cards_index_for_huizhou(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameConstants.MAX_INDEX)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, true, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 桃江麻将，别人打王牌时，判断手牌是不是碰碰胡
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_peng_hu_by_cards_index_taojiang(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[magic_cards_index[0]] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, true, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 判断胡牌时，手牌是否能258做将
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_258_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, true, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}
	
	/**
	 * 判断胡牌时，手牌是否能258做将(玩家打出的癞子牌还原成本身)
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_258_by_cards_index_zhuzhou(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}
		
		if (total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[cur_card_index] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, true, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 桃江麻将，别人出王牌时，判断胡牌时，手牌是否能258做将
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_taojiang_258_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[magic_cards_index[0]] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, true, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 风牌能吃时的胡牌分析算法，东南西北或中发白的任意三种不同的牌，可以自由组合，也可以是刻子组合
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_feng_chi_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, true))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 风牌能吃时的胡牌分析算法，东南西北或中发白的任意三种不同的牌，可以自由组合，也可以是刻子组合
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_feng_chi_by_cards_index_hd(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table_hd(tmp_hand_cards_index, total_magic, p_table, false, false, true))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 风牌能吃时的胡牌分析算法，东南西北或中发白的任意三种不同的牌，可以自由组合，也可以是刻子组合
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_feng_chi_by_cards_index_hd(int[] cards_index, int total_magic) {
		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table_hd(cards_index, total_magic, p_table, false, false, true))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 风牌能吃时的胡牌分析算法，东南西北或中发白的任意三种不同的牌，可以自由组合，也可以是刻子组合
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_feng_chi_by_cards_index_yd(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs, boolean isZimo) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		int total_magic = 0;
		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE) {
			tmp_hand_cards_index[cur_card_index]++;
		}

		boolean is_magic = false;
		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			if (cur_card_index == magic_cards_index[i]) {
				is_magic = true;
			}
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		// 不是自摸宝牌只能是本身牌
		if (!isZimo && is_magic) {
			tmp_hand_cards_index[cur_card_index]++;
			total_magic--;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, true))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}
	
	//欢乐扣点点专用
	public static boolean analyse_win_by_cards_index_hlkdd(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs, boolean check_feng_chi, boolean reduce_magic) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}
		if(reduce_magic){
			total_magic -= 1;
			tmp_hand_cards_index[magic_cards_index[0]] = 1;
		}
		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, check_feng_chi))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 风牌能吃时的胡牌分析算法，东南西北或中发白的任意三种不同的牌，可以自由组合，也可以是刻子组合 花牌装换成包牌本身值
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_feng_chi_by_cards_index_ruijin(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs, int max_card_index, boolean hsa_jia_bao, boolean isZimo) {

		int[] tmp_hand_cards_index = new int[max_card_index];
		for (int i = 0; i < max_card_index; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < max_card_index)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		boolean is_magic = false;
		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			if (cur_card_index == magic_cards_index[i]) {
				is_magic = true;
			}
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		// 不是自摸宝牌只能是本身牌
		if (!isZimo && is_magic) {
			tmp_hand_cards_index[cur_card_index]++;
			total_magic--;
		}

		if (hsa_jia_bao) {
			// 花牌还原成万能牌本身
			int total_hua = 0;
			for (int i = 0; i < 4; i++) {
				int index = GameConstants.MAX_ZI_FENG + i;
				total_hua += tmp_hand_cards_index[index];
				tmp_hand_cards_index[index] = 0;
			}
			tmp_hand_cards_index[magic_cards_index[0]] += total_hua;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, true))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 有东风令时，风牌能吃时的胡牌分析算法， 东南西、东南北、东西北或中发白进行组合，也可以是刻子组合
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_feng_chi_dfl_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table_feng_chi_dfl(tmp_hand_cards_index, total_magic, p_table, false, false, true))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 吉县1928夹胡牌分析
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @return
	 */
	public static boolean analyse_ji_xian_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table_ji_xian(tmp_hand_cards_index, total_magic, p_table))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	// ================================== 南昌麻将胡牌算法重写
	// start=================================================//

	/**
	 * 风牌能吃时的胡牌分析算法，东南西北或中发白的任意三种不同的牌，可以自由组合，也可以是刻子组合
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @param needDel
	 *            是否需要剔除发牌癞子
	 * @return
	 */
	public static boolean analyse_feng_chi_by_cards_index(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs, boolean needDel) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (needDel && total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[cur_card_index] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, false, false, true))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	/**
	 * 判断手牌是不是碰碰胡
	 * 
	 * @param hand_cards_index
	 * @param cur_card_index
	 * @param magic_cards_index
	 * @param total_magic_card_indexs
	 * @param needDel
	 *            是否需要剔除发牌癞子
	 * @return
	 */
	public static boolean analyse_peng_hu_by_cards_index_nc(int[] hand_cards_index, int cur_card_index, int[] magic_cards_index,
			int total_magic_card_indexs, boolean needDel) {
		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = hand_cards_index[i];
		}

		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;

		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		if (needDel && total_magic_card_indexs > 0) {
			// 别人打的牌是癞子牌，胡牌分析时不能当成癞子牌
			total_magic -= 1;
			tmp_hand_cards_index[cur_card_index] = 1;
		}

		PossibilityItemTable p_table = new PossibilityItemTable();

		if (false == check_table(tmp_hand_cards_index, total_magic, p_table, true, false, false))
			return false;

		return check_all_divide_plan(p_table, total_magic);
	}

	// ================================== 南昌麻将胡牌算法重写
	// end=================================================//

	private static boolean check_table_feng_chi_dfl(int[] cards_index, int total_magic, PossibilityItemTable p_table, boolean check_peng_hu,
			boolean check_258, boolean check_feng_chi) {
		int wan_start_index = 0; // 0
		int wan_end_index = GameUtilConstants.MAX_CARD_VALUE * 1 - 1; // 8
		int tiao_start_index = GameUtilConstants.MAX_CARD_VALUE * 1; // 9
		int tiao_end_index = GameUtilConstants.MAX_CARD_VALUE * 2 - 1; // 17
		int tong_start_index = GameUtilConstants.MAX_CARD_VALUE * 2; // 18
		int tong_end_index = GameUtilConstants.MAX_CARD_VALUE * 3 - 1; // 26
		int feng_start_index = GameUtilConstants.MAX_CARD_VALUE * 3; // 27
		int feng_end_index = GameUtilConstants.MAX_CARD_TYPE - 1; // 33

		if (false == check_table_sub_feng_chi_dfl(cards_index, total_magic, wan_start_index, wan_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub_feng_chi_dfl(cards_index, total_magic, tiao_start_index, tiao_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub_feng_chi_dfl(cards_index, total_magic, tong_start_index, tong_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub_feng_chi_dfl(cards_index, total_magic, feng_start_index, feng_end_index, false, p_table, check_peng_hu,
				check_258, check_feng_chi))
			return false;

		return true;
	}

	private static boolean check_table(int[] cards_index, int total_magic, PossibilityItemTable p_table, boolean check_peng_hu, boolean check_258,
			boolean check_feng_chi) {
		int wan_start_index = 0; // 0
		int wan_end_index = GameUtilConstants.MAX_CARD_VALUE * 1 - 1; // 8
		int tiao_start_index = GameUtilConstants.MAX_CARD_VALUE * 1; // 9
		int tiao_end_index = GameUtilConstants.MAX_CARD_VALUE * 2 - 1; // 17
		int tong_start_index = GameUtilConstants.MAX_CARD_VALUE * 2; // 18
		int tong_end_index = GameUtilConstants.MAX_CARD_VALUE * 3 - 1; // 26
		int feng_start_index = GameUtilConstants.MAX_CARD_VALUE * 3; // 27
		int feng_end_index = GameUtilConstants.MAX_CARD_TYPE - 1; // 33

		if (false == check_table_sub(cards_index, total_magic, wan_start_index, wan_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub(cards_index, total_magic, tiao_start_index, tiao_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub(cards_index, total_magic, tong_start_index, tong_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub(cards_index, total_magic, feng_start_index, feng_end_index, false, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		return true;
	}

	private static boolean check_table_hd(int[] cards_index, int total_magic, PossibilityItemTable p_table, boolean check_peng_hu, boolean check_258,
			boolean check_feng_chi) {
		int wan_start_index = 0; // 0
		int wan_end_index = GameUtilConstants.MAX_CARD_VALUE * 1 - 1; // 8
		int tiao_start_index = GameUtilConstants.MAX_CARD_VALUE * 1; // 9
		int tiao_end_index = GameUtilConstants.MAX_CARD_VALUE * 2 - 1; // 17
		int tong_start_index = GameUtilConstants.MAX_CARD_VALUE * 2; // 18
		int tong_end_index = GameUtilConstants.MAX_CARD_VALUE * 3 - 1; // 26
		int feng_start_index = GameUtilConstants.MAX_CARD_VALUE * 3; // 27
		int feng_end_index = GameUtilConstants.MAX_CARD_TYPE - 1; // 33

		if (false == check_table_sub(cards_index, total_magic, wan_start_index, wan_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub(cards_index, total_magic, tiao_start_index, tiao_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub(cards_index, total_magic, tong_start_index, tong_end_index, true, p_table, check_peng_hu, check_258,
				check_feng_chi))
			return false;

		if (false == check_table_sub_hd(cards_index, total_magic, feng_start_index, feng_end_index, false, p_table))
			return false;

		return true;
	}

	private static boolean check_table_ji_xian(int[] cards_index, int total_magic, PossibilityItemTable p_table) {
		int wan_start_index = 0; // 0
		int wan_end_index = GameUtilConstants.MAX_CARD_VALUE * 1 - 1; // 8
		int tiao_start_index = GameUtilConstants.MAX_CARD_VALUE * 1; // 9
		int tiao_end_index = GameUtilConstants.MAX_CARD_VALUE * 2 - 1; // 17
		int tong_start_index = GameUtilConstants.MAX_CARD_VALUE * 2; // 18
		int tong_end_index = GameUtilConstants.MAX_CARD_VALUE * 3 - 1; // 26
		int feng_start_index = GameUtilConstants.MAX_CARD_VALUE * 3; // 27
		int feng_end_index = GameUtilConstants.MAX_CARD_TYPE - 1; // 33

		if (false == check_table_sub_ji_xian(cards_index, total_magic, wan_start_index, wan_end_index, true, p_table))
			return false;

		if (false == check_table_sub_ji_xian(cards_index, total_magic, tiao_start_index, tiao_end_index, true, p_table))
			return false;

		if (false == check_table_sub_ji_xian(cards_index, total_magic, tong_start_index, tong_end_index, true, p_table))
			return false;

		if (false == check_table_sub_ji_xian(cards_index, total_magic, feng_start_index, feng_end_index, false, p_table))
			return false;

		return true;
	}

	private static boolean check_table_yao_jiu(int[] cards_index, int total_magic, PossibilityItemTable p_table) {
		int wan_start_index = 0; // 0
		int wan_end_index = GameUtilConstants.MAX_CARD_VALUE * 1 - 1; // 8
		int tiao_start_index = GameUtilConstants.MAX_CARD_VALUE * 1; // 9
		int tiao_end_index = GameUtilConstants.MAX_CARD_VALUE * 2 - 1; // 17
		int tong_start_index = GameUtilConstants.MAX_CARD_VALUE * 2; // 18
		int tong_end_index = GameUtilConstants.MAX_CARD_VALUE * 3 - 1; // 26

		if (false == check_table_sub_yao_jiu(cards_index, total_magic, wan_start_index, wan_end_index, p_table))
			return false;

		if (false == check_table_sub_yao_jiu(cards_index, total_magic, tiao_start_index, tiao_end_index, p_table))
			return false;

		if (false == check_table_sub_yao_jiu(cards_index, total_magic, tong_start_index, tong_end_index, p_table))
			return false;

		return true;
	}

	private static boolean check_table_sub_feng_chi_dfl(int[] cards_index, int total_magic, int start_index, int end_index, boolean is_wan_tiao_tong,
			PossibilityItemTable p_table, boolean check_peng_hu, boolean check_258, boolean check_feng_chi) {
		int key = 0;
		int card_count = 0;

		for (int i = start_index; i <= end_index; i++) {
			key = key * 10 + cards_index[i];
			card_count += cards_index[i];
		}

		if (card_count > 0) {
			if (false == check_all_possibility_feng_chi_dfl(total_magic, card_count, key, is_wan_tiao_tong, p_table, check_peng_hu, check_258,
					check_feng_chi))
				return false;
		}

		return true;
	}

	private static boolean check_table_sub(int[] cards_index, int total_magic, int start_index, int end_index, boolean is_wan_tiao_tong,
			PossibilityItemTable p_table, boolean check_peng_hu, boolean check_258, boolean check_feng_chi) {
		int key = 0;
		int card_count = 0;

		for (int i = start_index; i <= end_index; i++) {
			key = key * 10 + cards_index[i];
			card_count += cards_index[i];
		}

		if (card_count > 0) {
			if (false == check_all_possibility(total_magic, card_count, key, is_wan_tiao_tong, p_table, check_peng_hu, check_258, check_feng_chi))
				return false;
		}

		return true;
	}

	private static boolean check_table_sub_hd(int[] cards_index, int total_magic, int start_index, int end_index, boolean is_wan_tiao_tong,
			PossibilityItemTable p_table) {
		int key = 0;
		int card_count = 0;

		for (int i = start_index; i <= end_index; i++) {
			key = key * 10 + cards_index[i];
			card_count += cards_index[i];
		}

		if (card_count > 0) {
			if (false == check_all_possibility_hd(total_magic, card_count, key, p_table))
				return false;
		}

		return true;
	}

	private static boolean check_table_sub_ji_xian(int[] cards_index, int total_magic, int start_index, int end_index, boolean is_wan_tiao_tong,
			PossibilityItemTable p_table) {
		int key = 0;
		int card_count = 0;

		for (int i = start_index; i <= end_index; i++) {
			key = key * 10 + cards_index[i];
			card_count += cards_index[i];
		}

		if (card_count > 0) {
			if (false == check_all_possibility_ji_xian(total_magic, card_count, key, is_wan_tiao_tong, p_table))
				return false;
		}

		return true;
	}

	private static boolean check_table_sub_yao_jiu(int[] cards_index, int total_magic, int start_index, int end_index, PossibilityItemTable p_table) {
		int key = 0;
		int card_count = 0;

		for (int i = start_index; i <= end_index; i++) {
			key = key * 10 + cards_index[i];
			card_count += cards_index[i];
		}

		if (card_count > 0) {
			if (false == check_all_possibility_yao_jiu(total_magic, card_count, key, p_table))
				return false;
		}

		return true;
	}

	private static boolean check_all_possibility_feng_chi_dfl(int total_magic, int card_count, int key, boolean is_wan_tiao_tong,
			PossibilityItemTable p_table, boolean check_peng_hu, boolean check_258, boolean check_feng_chi) {
		boolean has_founded = false;
		int index = p_table.index;

		int left_card = 0;
		boolean has_eye = false;

		for (int magic_needed = 0; magic_needed <= total_magic; magic_needed++) {
			left_card = (card_count + magic_needed) % 3;
			has_eye = false;

			if (1 == left_card)
				continue;

			if (2 == left_card)
				has_eye = true;

			if (has_founded || TableManager.getInstance().contains_feng_chi_dfl(key, magic_needed, has_eye, is_wan_tiao_tong, check_peng_hu,
					check_258, check_feng_chi)) {
				int win_counter = p_table.sub_win_counter[index];

				PossibilityItem tmp_item = p_table.possibility_array[index][win_counter];
				tmp_item.has_eye = has_eye;
				tmp_item.magic_needed = magic_needed;

				p_table.sub_win_counter[index]++;

				has_founded = true;
			}
		}

		if (p_table.sub_win_counter[index] <= 0)
			return false;

		p_table.index++;

		return true;
	}

	private static boolean check_all_possibility(int total_magic, int card_count, int key, boolean is_wan_tiao_tong, PossibilityItemTable p_table,
			boolean check_peng_hu, boolean check_258, boolean check_feng_chi) {
		boolean has_founded = false;
		int index = p_table.index;

		int left_card = 0;
		boolean has_eye = false;

		for (int magic_needed = 0; magic_needed <= total_magic; magic_needed++) {
			left_card = (card_count + magic_needed) % 3;
			has_eye = false;

			if (1 == left_card)
				continue;

			if (2 == left_card)
				has_eye = true;

			if (has_founded
					|| TableManager.getInstance().contains(key, magic_needed, has_eye, is_wan_tiao_tong, check_peng_hu, check_258, check_feng_chi)) {
				int win_counter = p_table.sub_win_counter[index];

				PossibilityItem tmp_item = p_table.possibility_array[index][win_counter];
				tmp_item.has_eye = has_eye;
				tmp_item.magic_needed = magic_needed;

				p_table.sub_win_counter[index]++;

				has_founded = true;
			}
		}

		if (p_table.sub_win_counter[index] <= 0)
			return false;

		p_table.index++;

		return true;
	}

	private static boolean check_all_possibility_hd(int total_magic, int card_count, int key, PossibilityItemTable p_table) {
		boolean has_founded = false;
		int index = p_table.index;

		int left_card = 0;
		boolean has_eye = false;

		for (int magic_needed = 0; magic_needed <= total_magic; magic_needed++) {
			left_card = (card_count + magic_needed) % 3;
			has_eye = false;

			if (1 == left_card)
				continue;

			if (2 == left_card)
				has_eye = true;

			if (has_founded || TableManager.getInstance().contains_hd(key, magic_needed, has_eye)) {
				int win_counter = p_table.sub_win_counter[index];

				PossibilityItem tmp_item = p_table.possibility_array[index][win_counter];
				tmp_item.has_eye = has_eye;
				tmp_item.magic_needed = magic_needed;

				p_table.sub_win_counter[index]++;

				has_founded = true;
			}
		}

		if (p_table.sub_win_counter[index] <= 0)
			return false;

		p_table.index++;

		return true;
	}

	private static boolean check_all_possibility_ji_xian(int total_magic, int card_count, int key, boolean is_wan_tiao_tong,
			PossibilityItemTable p_table) {
		boolean has_founded = false;
		int index = p_table.index;

		int left_card = 0;
		boolean has_eye = false;

		for (int magic_needed = 0; magic_needed <= total_magic; magic_needed++) {
			left_card = (card_count + magic_needed) % 3;
			has_eye = false;

			if (1 == left_card)
				continue;

			if (2 == left_card)
				has_eye = true;

			if (has_founded || TableManager.getInstance().contains_ji_xian(key, magic_needed, has_eye, is_wan_tiao_tong)) {
				int win_counter = p_table.sub_win_counter[index];

				PossibilityItem tmp_item = p_table.possibility_array[index][win_counter];
				tmp_item.has_eye = has_eye;
				tmp_item.magic_needed = magic_needed;

				p_table.sub_win_counter[index]++;

				has_founded = true;
			}
		}

		if (p_table.sub_win_counter[index] <= 0)
			return false;

		p_table.index++;

		return true;
	}

	private static boolean check_all_possibility_yao_jiu(int total_magic, int card_count, int key, PossibilityItemTable p_table) {
		boolean has_founded = false;
		int index = p_table.index;

		int left_card = 0;
		boolean has_eye = false;

		for (int magic_needed = 0; magic_needed <= total_magic; magic_needed++) {
			left_card = (card_count + magic_needed) % 3;
			has_eye = false;

			if (1 == left_card)
				continue;

			if (2 == left_card)
				has_eye = true;

			if (has_founded || TableManager.getInstance().contains_yao_jiu(key, magic_needed, has_eye)) {
				int win_counter = p_table.sub_win_counter[index];

				PossibilityItem tmp_item = p_table.possibility_array[index][win_counter];
				tmp_item.has_eye = has_eye;
				tmp_item.magic_needed = magic_needed;

				p_table.sub_win_counter[index]++;

				has_founded = true;
			}
		}

		if (p_table.sub_win_counter[index] <= 0)
			return false;

		p_table.index++;

		return true;
	}

	private static boolean check_all_divide_plan(PossibilityItemTable p_table, int total_magic) {
		// 手里的牌全是鬼牌
		if (0 == p_table.index) {
			return (total_magic >= 2);
		}

		// 只有一种花色的牌
		if (p_table.index == 1)
			return true;

		// 尝试组合所有花色，能组合则能胡
		for (int i = 0; i < p_table.sub_win_counter[0]; i++) {
			PossibilityItem item = p_table.possibility_array[0][i];

			boolean has_eye = item.has_eye;

			int magic_left = total_magic - item.magic_needed;

			if (check_all_divide_plan_sub(p_table, has_eye, magic_left, 1, p_table.index))
				return true;
		}

		return false;
	}

	private static boolean check_all_divide_plan_sub(PossibilityItemTable p_table, boolean has_eye, int magic_left, int start_index, int end_index) {
		for (int i = 0; i < p_table.sub_win_counter[start_index]; i++) {
			PossibilityItem item = p_table.possibility_array[start_index][i];

			if (has_eye && item.has_eye)
				continue;

			if (magic_left < item.magic_needed)
				continue;

			if (start_index < end_index - 1) {
				if (check_all_divide_plan_sub(p_table, has_eye || item.has_eye, magic_left - item.magic_needed, start_index + 1, p_table.index))
					return true;

				continue;
			}

			if ((false == has_eye) && (false == item.has_eye) && (item.magic_needed + 2 > magic_left))
				continue;

			return true;
		}

		return false;
	}

	protected static void test_one() {
		TableManager.getInstance().load();

		int[] cards_index = { 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		int[] magic_cards_index = { 0, 0 };

		boolean lai_zi_cheng_ju = analyse_lai_zi_cheng_ju_by_cards_index(cards_index, 0, magic_cards_index, 1);

		System.out.println(lai_zi_cheng_ju);
	}

	protected static void test(int loops) {
		TableManager.getInstance().load();
		MJGameLogic logic = new MJGameLogic(MJType.DEFAULT);
		PrintWriter stdout = null;

		try {
			stdout = new PrintWriter("tbl_mj/old_new_compare.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		stdout.println("AnalyseCardUtil testing ...");
		int win_count = 0;
		int lose_count = 0;

		long timer = System.currentTimeMillis();

		// int[] cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		int[] cards_index = new int[GameConstants.MAX_INDEX];
		int card_count = 0;
		int random_index = 0;
		int random_magic_card_count = 0;
		boolean can_win = false;
		boolean old_can_win = false;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		for (int i = 0; i < loops; i++) {
			card_count = 0;
			for (int j = 0; j < GameUtilConstants.MAX_CARD_TYPE; j++) {
				cards_index[j] = 0;
			}

			while (card_count < GameUtilConstants.MAX_HAND_CARD_COUNT) {
				random_index = random.nextInt(GameUtilConstants.MAX_CARD_TYPE);

				if (cards_index[random_index] < GameUtilConstants.CARD_COUNT_PER_CARD) {
					cards_index[random_index]++;
					card_count++;
				}
			}

			random_magic_card_count = random.nextInt(GameUtilConstants.MAX_MAGIC_INDEX_COUNT);
			for (int x = 0; x < random_magic_card_count; x++) {
				random_index = random.nextInt(GameUtilConstants.MAX_CARD_TYPE);
				magic_cards_index[x] = random_index;
			}

			long tmp_timer = System.currentTimeMillis();
			can_win = analyse_258_by_cards_index(cards_index, GameUtilConstants.MAX_CARD_TYPE, magic_cards_index, random_magic_card_count);
			long time_lag_new = System.currentTimeMillis() - tmp_timer;

			// 构造必要的初始化数据
			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
			WeaveItem[] weaveItem = new WeaveItem[1];
			weaveItem[0] = new WeaveItem();
			logic.clean_magic_cards();
			for (int y = 0; y < random_magic_card_count; y++)
				logic.add_magic_card_index(magic_cards_index[y]);

			tmp_timer = System.currentTimeMillis();
			old_can_win = logic.analyse_card_(cards_index, weaveItem, 0, analyseItemArray, true);
			long time_lag_old = System.currentTimeMillis() - tmp_timer;

			if (time_lag_new > 10 || time_lag_old > 10) {
				stdout.println("第" + i + "次随机比对，新算法耗时：" + time_lag_new + "毫秒");
				stdout.println("第" + i + "次随机比对，旧算法耗时：" + time_lag_old + "毫秒");
			}

			if (can_win != old_can_win) {
				stdout.println("比对失败，" + "新算法：" + can_win + "旧算法" + old_can_win);
				print_cards(cards_index, magic_cards_index, stdout);
				stdout.println();
			}

			if (can_win)
				win_count++;
			else
				lose_count++;
		}

		stdout.println(loops + "次随机测试，用时" + (System.currentTimeMillis() - timer) + "毫秒，" + "赢牌" + win_count + "次，" + "输牌" + lose_count + "次。");
		stdout.println("AnalyseCardUtil testing finished.");

		if (stdout != null)
			stdout.close();
	}

	protected static void print_cards(int[] cards_index, int[] magic_cards_index, PrintWriter stdout) {
		int wan_start_index = 0; // 0
		int wan_end_index = GameUtilConstants.MAX_CARD_VALUE * 1 - 1; // 8
		int tiao_start_index = GameUtilConstants.MAX_CARD_VALUE * 1; // 9
		int tiao_end_index = GameUtilConstants.MAX_CARD_VALUE * 2 - 1; // 17
		int tong_start_index = GameUtilConstants.MAX_CARD_VALUE * 2; // 18
		int tong_end_index = GameUtilConstants.MAX_CARD_VALUE * 3 - 1; // 26
		int feng_start_index = GameUtilConstants.MAX_CARD_VALUE * 3; // 27
		int feng_end_index = GameUtilConstants.MAX_CARD_TYPE - 1; // 33

		for (int i = wan_start_index; i <= wan_end_index; i++) {
			stdout.printf("%2d ", cards_index[i]);
		}
		stdout.println();

		for (int i = tiao_start_index; i <= tiao_end_index; i++) {
			stdout.printf("%2d ", cards_index[i]);
		}
		stdout.println();

		for (int i = tong_start_index; i <= tong_end_index; i++) {
			stdout.printf("%2d ", cards_index[i]);
		}
		stdout.println();

		for (int i = feng_start_index; i <= feng_end_index; i++) {
			stdout.printf("%2d ", cards_index[i]);
		}
		stdout.println();

		for (int i = 0; i < GameUtilConstants.MAX_MAGIC_INDEX_COUNT; i++) {
			stdout.printf("%2d ", magic_cards_index[i]);
		}
		stdout.println();
		stdout.println("------------------");
	}

	public static void main(String[] args) {
		// int loops = 10000;
		// test(loops);
		test_one();
	}
}

class PossibilityItem {
	public boolean has_eye;
	public int magic_needed;

	public PossibilityItem() {
		has_eye = false;
		magic_needed = 0;
	}
}

class PossibilityItemTable {
	PossibilityItem[][] possibility_array = new PossibilityItem[GameUtilConstants.CARD_COLOR_COUNT][GameUtilConstants.MAX_COMBINE_COUNT];

	public int index;
	public int[] sub_win_counter;

	public PossibilityItemTable() {
		for (int i = 0; i < GameUtilConstants.CARD_COLOR_COUNT; i++) {
			for (int j = 0; j < GameUtilConstants.MAX_COMBINE_COUNT; j++) {
				possibility_array[i][j] = new PossibilityItem();
			}
		}

		index = 0;
		sub_win_counter = new int[] { 0, 0, 0, 0 };
	}
}
