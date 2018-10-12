package com.cai.game.mj;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.handler.yyzxz.MJHandlerDispatchCard_YYZXZ.XiaZiCardResult;

public class MJGameLogic {
	private static Logger logger = Logger.getLogger(MJGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	private int _ci; // 洛阳杠次 次牌

	public MJGameLogic() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_COUNT];
		_lai_gen = 0;
		_ding_gui = 0;
		_ci = 0;
	}

	public void clean_magic_cards() {
		_magic_card_count = 0;
	}

	// 获取数值
	public int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public static int get_card_color(int card) {
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

	public void add_ci_card(int card) {
		_ci = card;
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

	public boolean is_ci_card(int card) {
		if (_ci == card) {
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
					for (int m = 0; m < this._magic_card_count; i++) {
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

	// 岳阳抓虾子 碰牌判断
	public int check_peng_yyzxz(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		int cbColor = get_card_color(cur_card);// (card&MJGameConstants.LOGIC_MASK_COLOR);

		// 万子不能碰牌
		if (cbColor == 1) {
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
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
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
				return cbActionMask;
			}
			if (cards_index[i] > 0) {
				cbActionMask = GameConstants.WIK_XIA_ZI_BU;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;//
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
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave, int markColor) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4 && (markColor == -1 || get_card_color(switch_to_card_data(i)) == markColor)) {
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
	public int analyse_gang_card_all_lygc(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave, int peng_card) {
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
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1
							&& WeaveItem[i].center_card != peng_card) { // 缓杠
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
	public int analyse_gang_card_all_xc(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
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

	public int analyse_gang_card_cs(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
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
	public int analyse_gang_card_all_xthh(MJTable table, int seat_index, int cards_index[], WeaveItem WeaveItem[],
			int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
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
	public int analyse_gang_by_card_hand_card(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
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
				gangCardResult.type[i] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
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
	public int analyse_xiao_by_dispacth_card(MJTable table, int seat_index, int cards_index[], int card,
			WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
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
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card,
			ChiHuRight chiHuRight, boolean has_feng) {
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
			}
		}

		if (eye != 1) {
			return GameConstants.WIK_NULL;
		}
		return GameConstants.CHR_HUNAN_DA_DUI_HU;
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
			if (nGenCount > 1) {
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

	public int is_qi_xiao_dui_he_nan_zhou_kou(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int cur_card) {
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
	public boolean analyse_card_henanxy(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card,
			List<AnalyseItem> analyseItemArray, boolean has_feng) {
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
				if (cards_index[i] == 2
						|| (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
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
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i
								: get_magic_card_index(0);
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
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
								this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1], this.is_magic_index(i + 2) ? 0
										: cbMagicCardIndex[i + 2] };

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
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_CENTER;
								} else if (switch_to_card_index(cur_card) == i + 2) {
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_RIGHT;
								} else {
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
								}
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
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card,
									analyseItem.cbCardData[i]);
						}

						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(
									pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(
									pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(
									pKindItem[i].cbValidIndex[2]);
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
	//中五
	public int is_zhongwu_henan_xy(List<AnalyseItem> analyseItemArray) {

		// 胡牌判断
		int cbCardCount = 0;

		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(0);
			for (int j = 0; j < analyseItem.cbWeaveKind.length; j++) {
				if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_CENTER) != 0 
						&& get_card_value(analyseItem.cbCenterCard[j]) == 5){
					return GameConstants.CHR_HENAN_XY_ZHONGWU;
				}
				if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_LEFT) != 0 
						&& get_card_value(analyseItem.cbCenterCard[j]) == 4){
					return GameConstants.CHR_HENAN_XY_ZHONGWU;
				}
					
			}

		}
		return GameConstants.WIK_NULL;
	}
	//连六
	public int is_lianliu_henan_xy( List<AnalyseItem> analyseItemArray) {

		// 胡牌判断
		int cbCardCount = 0;

		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			int firstcard=GameConstants.INVALID_CARD;
			int lastcard=GameConstants.INVALID_CARD;
			AnalyseItem analyseItem = analyseItemArray.get(0);
			for (int j = 0; j < analyseItem.cbWeaveKind.length; j++) {
				if(firstcard == GameConstants.INVALID_CARD){
					if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_LEFT) != 0)
						firstcard = analyseItem.cbCenterCard[j];
				}else{
					if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_LEFT) != 0){
						lastcard = analyseItem.cbCenterCard[j];
						if(lastcard-firstcard!=3){
							firstcard=lastcard;
							lastcard=GameConstants.INVALID_CARD;
						}else{
							return GameConstants.CHR_HENAN_XY_LIANLIU;
						}
					}
					else if((analyseItem.cbWeaveKind[j] & GameConstants.WIK_CENTER) != 0){
						lastcard = analyseItem.cbCenterCard[j];
						if(lastcard-firstcard!=2){
							firstcard=lastcard;
							lastcard=GameConstants.INVALID_CARD;
						}else{
							return GameConstants.CHR_HENAN_XY_LIANLIU;
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
			int cbValue = get_card_value(switch_to_card_data(analyseItem.cbCardEye));

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

	// 大对子,碰碰胡
	public boolean is_pengpeng_hu(AnalyseItem analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
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
			if ((pAnalyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0) {
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
		int cbValue = get_card_value(cur_card);

		// 单牌统计
		if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
			return false;
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

	// 分析扑克--东南西北可以自由组合
	public boolean analyse_card_feng_chi(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			List<AnalyseItem> analyseItemArray) {
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
				if (cards_index[i] == 2
						|| (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
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
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i
								: get_magic_card_index(0);
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
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
								this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1], this.is_magic_index(i + 2) ? 0
										: cbMagicCardIndex[i + 2] };

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
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
								this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1], this.is_magic_index(i + 2) ? 0
										: cbMagicCardIndex[i + 2] };

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
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
								this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1], this.is_magic_index(i + 2) ? 0
										: cbMagicCardIndex[i + 2] };

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
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card,
									analyseItem.cbCardData[i]);
						}

						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(
									pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(
									pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(
									pKindItem[i].cbValidIndex[2]);
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

	// 分析麻将--东南西北、中发白可以自由组合
	public boolean analyse_card_feng_chi_zfb(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			List<AnalyseItem> analyseItemArray, ChiHuRight chiHuRight, boolean hupai) {
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
				if (cards_index[i] == 2
						|| (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
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
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i
								: get_magic_card_index(0);
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
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
								this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1], this.is_magic_index(i + 2) ? 0
										: cbMagicCardIndex[i + 2] };

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
							int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + sub_index_array[1]]
									+ cbMagicCardIndex[i + sub_index_array[2]];
							chi_count += cbMagicCardCount;
							if (chi_count >= 3) {
								int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
										this.is_magic_index(i + sub_index_array[1]) ? 0
												: cbMagicCardIndex[i + sub_index_array[1]],
										this.is_magic_index(i + sub_index_array[2]) ? 0
												: cbMagicCardIndex[i + sub_index_array[2]] };

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
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
								this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1], this.is_magic_index(i + 2) ? 0
										: cbMagicCardIndex[i + 2] };

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
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card,
									analyseItem.cbCardData[i]);
						}

						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(
									pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(
									pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(
									pKindItem[i].cbValidIndex[2]);
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
	public boolean analyse_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			List<AnalyseItem> analyseItemArray, boolean has_feng) {
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
				if (cards_index[i] == 2
						|| (this.is_magic_index(i) == false && magic_count(cards_index) + cards_index[i] == 2)) {
					// 变量定义
					AnalyseItem analyseItem = new AnalyseItem();

					// 设置结果
					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}
					
					if (magic_count(cards_index) == 2) { // 如果是两张癞子牌，目前只针对红中癞子258将的情况
						analyseItem.cbCardEye = switch_to_card_data(this.get_magic_card_index(0));
						analyseItem.bMagicEye = true;
					} else {
						analyseItem.cbCardEye = switch_to_card_data(i);
						if (cards_index[i] < 2 || this.is_magic_index(i) == true)
							analyseItem.bMagicEye = true;
						else
							analyseItem.bMagicEye = false;
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
						kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i
								: get_magic_card_index(0);
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
						int cbIndex[] = { this.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
								this.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1], this.is_magic_index(i + 2) ? 0
										: cbMagicCardIndex[i + 2] };

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
						if (this.magic_count(cbCardIndexTemp) == 2) { // 如果将牌是一对癞子牌，只针对于红中癞子玩法
							cbCardEye = switch_to_card_data(this.get_magic_card_index(0));
							bMagicEye = true;
							break;
						} else if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);// 牌眼
							if (this.is_magic_index(i))
								bMagicEye = true;
							break;
						} else if ((cbCardIndexTemp[i] + this.magic_count(cbCardIndexTemp) == 2)) {
							cbCardEye = switch_to_card_data(i);
							bMagicEye = true;
							break; // 这里不break跳出循环的话，会导致将牌判断出问题，红中癞子是OK的，如果是混子，而且混子的索引比牌眼的索引靠前，后期还是要根据情况调整
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
							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card,
									analyseItem.cbCardData[i]);
						}

						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(
									pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(
									pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(
									pKindItem[i].cbValidIndex[2]);
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
	 * 落地牌 是否暗 杠 明杠
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_an_gang(WeaveItem weaveItems[], int weaveCount) {
		boolean isAnGang = true;
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind != GameConstants.WIK_GANG
					&& weaveItems[i].weave_kind != GameConstants.WIK_BU_ZHNAG)) {
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

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
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

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 15;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER
				|| player_action == GameConstants.WIK_LEFT) {
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
	public int get_chi_hu_action_rank_hz(ChiHuRight chiHuRight) {
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
			wFanShu = 6;// 没人两分,被抢杠的人全包
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

	public int get_chi_hu_action_rank_henan_xc(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
				|| !(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()
				|| !(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
			wFanShu += 4;
		}
		// 断2门及以上加一番
		if (chiHuRight.duanmen_count > 1) {
			wFanShu += 1;
		}
		// 风牌组合加番数,只能计算手上的
		wFanShu += chiHuRight.baifeng_count;
		wFanShu += chiHuRight.heifeng_count;

		// if
		// (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HZ_FENGMO_1)).is_empty()
		// ){
		// wFanShu+=1;
		// }else if
		// (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HZ_FENGMO_2)).is_empty()
		// ){
		// wFanShu+=2;
		// }

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
			logger.error("get_weave_card:invalid cbWeaveKind");
		}
		}

		return 0;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);// (card&MJGameConstants.LOGIC_MASK_VALUE);
		int cbColor = get_card_color(card);// (card&MJGameConstants.LOGIC_MASK_COLOR);
		return (((cbValue >= 1) && (cbValue <= 9) && (cbColor <= 2))
				|| ((cbValue >= 1) && (cbValue <= 9) && (cbColor == 3))
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
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
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

	public static void main(String[] args) {

	}

	public boolean checkWanZiByIndex(int[] cards_index) {
		// TODO Auto-generated method stub
		for (int i = 0; i < cards_index.length; i++) {
			int color = get_card_color(switch_to_card_data(i));
			if (color == 0) {
				return true;
			}

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

	// 类型子项
	public static class KindItem {
		public int cbWeaveKind;// 组合类型
		public int cbCenterCard;// 中心扑克
		public int cbCardIndex[] = new int[3];// 扑克索引
		public int cbValidIndex[] = new int[3];// 实际扑克索引

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
	}
}
