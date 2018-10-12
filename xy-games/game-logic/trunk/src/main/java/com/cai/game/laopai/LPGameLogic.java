package com.cai.game.laopai;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.handler.yyzxz.MJHandlerDispatchCard_YYZXZ.XiaZiCardResult;

public class LPGameLogic {
	private static Logger logger = Logger.getLogger(LPGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	private int _ci; // 洛阳杠次 次牌
	private int _wang_ba; // 王霸牌

	public LPGameLogic() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_LAOPAI_COUNT];
		_lai_gen = 0;
		_ding_gui = 0;
		_ci = -1;
		_wang_ba = 0;
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
		int cbTempCardData[] = new int[GameConstants.MAX_LAOPAI_COUNT];

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
		int cbTempCardData[] = new int[GameConstants.MAX_LAOPAI_COUNT];

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
		for (int i = 0; i < GameConstants.MAX_INDEX_LAOPAI; i++)
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
		if (card_index < 0 || card_index >= GameConstants.MAX_INDEX_LAOPAI) {
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
					cards_index[j]++;
				}
				return false;
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
		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX_LAOPAI];
		for (int i = 0; i < GameConstants.MAX_INDEX_LAOPAI; i++) {
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

	// 吃牌判断
	public int check_chi(int cards_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 过滤判断
		if ( is_magic_card(cur_card))
			return GameConstants.WIK_NULL;

		// 变量定义
		int excursion[] = new int[] { 0, 1, 2 };
		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 吃牌判断
		int eat_type = 0, first_index = 0;
		int cur_card_index = switch_to_card_index(cur_card);
		int max_value_inde=6;
		for (int i = 0; i < 3; i++) {
			int value_index = cur_card_index % 9;
			if(cur_card_index > 27){
				max_value_inde=0;
			}
			if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= max_value_inde)) {
				// 吃牌判断
				first_index = cur_card_index - excursion[i];

				if (this._magic_card_count > 0) {
					for (int m = 0; m < this._magic_card_count; m++) {
						// 吃牌不能包含有王霸
						if (get_magic_card_index(m) != GameConstants.MAX_INDEX_LAOPAI && get_magic_card_index(m) >= first_index
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
				mj_count = GameConstants.MAX_FENG_LAOPAI;
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

	// 分析扑克
	public boolean analyse_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray, boolean has_feng) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		// 变量定义
		int cbKindItemCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount - 2) / 3;

		// 单吊判断
		if (cbLessKindItem == 0) {
			// 牌眼判断 长沙麻将需要二、五、八做将，比如二万、五条、八筒等。如果是某些牌型则将可以为任意一对。
			int mj_count = GameConstants.MAX_ZI_LAOPAI;
			if (has_feng) {
				mj_count = GameConstants.MAX_FENG_LAOPAI;
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
		int cbMagicCardIndex[] = new int[GameConstants.MAX_INDEX_LAOPAI];
		for (int i = 0; i < GameConstants.MAX_INDEX_LAOPAI; i++) {
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
			int mj_count = GameConstants.MAX_ZI_LAOPAI;
			if (has_feng) {
				mj_count = GameConstants.MAX_FENG_LAOPAI;
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
				if ((i < (GameConstants.MAX_FENG_LAOPAI - 2)) && ((i % 9) < 7)) {
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
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX_LAOPAI];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3,4 };
			KindItem pKindItem[] = new KindItem[5];
			for (int i = 0; i < 5; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_ZI_LAOPAI;
				if (has_feng) {
					mj_count = GameConstants.MAX_FENG_LAOPAI;
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
		if (player_action == GameConstants.WIK_YAO_YI_SE) {
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
			return GameConstants.MAX_INDEX_LAOPAI;
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
		if (card_index >= GameConstants.MAX_INDEX_LAOPAI) {
			return GameConstants.MAX_INDEX_LAOPAI;
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

		if (LPTable.DEBUG_CARDS_MODE) {
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
		for (int i = 0; i < GameConstants.MAX_INDEX_LAOPAI; i++) {
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
		if (cbCardCount == 0 || cbCardCount > GameConstants.MAX_LAOPAI_COUNT)
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

		public KindItem() {

		}
	}

	// 分析子项
	public static class AnalyseItem {
		public int cbCardEye;//// 牌眼扑克
		public boolean bMagicEye;// 牌眼是否是王霸
		public int cbWeaveKind[] = new int[5];// 组合类型
		public int cbCenterCard[] = new int[5];// 中心扑克
		public int cbCardData[][] = new int[5][4]; // 实际扑克
	}
}
