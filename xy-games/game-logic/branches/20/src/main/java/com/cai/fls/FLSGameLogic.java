/**
 * 
 */
package com.cai.fls;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
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

// 分析子项
class AnalyseItem {
	public int cbCardEye[] = new int[4];//// 牌眼扑克
	public boolean bMagicEye;// 牌眼是否是王霸
	public int cbWeaveKind[] = new int[6];// 组合类型
	public int cbCenterCard[] = new int[6];// 中心扑克
	public int cbCardData[][] = new int[6][4]; // 实际扑克

	public int cbPoint;// 组合牌的最佳点数;

	public boolean curCardEye;// 当前摸的牌是否是牌眼
	public boolean isShuangDui;// 牌眼 true双对--判断碰碰胡
	public int eyeKind;// 牌眼 组合类型
	public int eyeCenterCard;// 牌眼 中心扑克
}

public class FLSGameLogic {

	private static Logger logger = Logger.getLogger(FLSGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	public FLSGameLogic() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_FLS_COUNT];
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
		return _magic_card_index[index];// MJGameConstants.MAX_FLS_INDEX;
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
		int cbTempCardData[] = new int[GameConstants.MAX_FLS_COUNT];

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
		int cbTempCardData[] = new int[GameConstants.MAX_FLS_COUNT];

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
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++)
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
		if (card_index < 0 || card_index >= GameConstants.MAX_FLS_INDEX) {
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

		// // 过滤判断
		// if (cur_card >= 0x31 || is_magic_card(cur_card))
		// return GameConstants.WIK_NULL;

		// 变量定义
		int excursion[] = new int[] { 0, 1, 2 };
		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 吃牌判断
		int eat_type = 0, first_index = 0;
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		for (int i = 0; i < 3; i++) {
			int value_index = cur_card_index % 3;
			if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= 0)) {
				// 吃牌判断
				first_index = cur_card_index - excursion[i];

				if (this._magic_card_count > 0) {
					for (int m = 0; m < this._magic_card_count; i++) {
						// 吃牌不能包含有王霸
						if (get_magic_card_index(m) != GameConstants.MAX_FLS_INDEX
								&& get_magic_card_index(m) >= first_index && get_magic_card_index(m) <= first_index + 2)
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
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
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

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
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
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
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

	public int analyse_gang_card_fls(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
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

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card_hand_card(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
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
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
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

	// 是否单吊
	public boolean is_dan_diao(int cards_index[], int cur_card) {
		// 单牌数目
		// int cbReplaceCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
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

	/**
	 * 手牌是否满足
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_pengpeng_hu(AnalyseItem analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		int num = 0;
		for (int eye : analyseItem.cbCardEye) {
			if (eye == 0)
				continue;
			num++;
		}
		return num == 1 || analyseItem.isShuangDui;// 单一张 或者 2对子
	}

	/**
	 * 落地牌 是否满足
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_pengpeng_hu_down(WeaveItem weaveItems[]) {
		boolean isPengPengHu = true;
		for (int i = 0; i < weaveItems.length; i++) {
			if ((weaveItems[i].weave_kind != GameConstants.WIK_PENG
					&& weaveItems[i].weave_kind != GameConstants.WIK_GANG
					&& weaveItems[i].weave_kind != GameConstants.WIK_ZHAO)) {
				isPengPengHu = false;
				break;
			}
		}
		return isPengPengHu;
	}
	

	/**
	 * 判断牌眼是否有成句
	 * @param cards_index
	 * @return
	 */
	public boolean isChengJu(int cards_index[], AnalyseItem analyseItem) {

		int cbMagicCardIndex[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			int num = cbMagicCardIndex[i] = cards_index[i];
			if(num>0) {
				for(int j=0;j<num;j++) {
					for(int k=0;k<4;k++) {
						if(analyseItem.cbCardEye[k]==0) {
							analyseItem.cbCardEye[k]=switch_to_card_data(i);//复制牌眼
							break;
						}
					}
				}
			}
		}

		int mj_count = GameConstants.MAX_FLS_INDEX;
		for (int i = 0; i < mj_count; i++) {
			// 同牌判断
			if (cbMagicCardIndex[i] == 3) {
//				if (analyseItem != null) {
//					analyseItem.cbWeaveKind[analyseItem.cbWeaveKind.length - 1] = GameConstants.WIK_PENG;
//					analyseItem.cbCenterCard[analyseItem.cbWeaveKind.length - 1] = switch_to_card_data(i);
//					analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][0] = switch_to_card_data(i);
//					analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][1] = switch_to_card_data(i);
//					analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][2] = switch_to_card_data(i);
//				}
				analyseItem.eyeKind=GameConstants.WIK_PENG;
				analyseItem.eyeCenterCard=switch_to_card_data(i);
				return true;
			} // 同牌判断 end
				// 连牌判断
			if ((i < (GameConstants.MAX_FLS_INDEX - 2)) && ((i % 3) == 0)) {
				// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
				int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] + cbMagicCardIndex[i + 2];
				if (chi_count >= 3) {
					if (cbMagicCardIndex[i] >= 1 && cbMagicCardIndex[i + 1] >= 1 && cbMagicCardIndex[i + 2] >= 1) {
//						if (analyseItem != null) {
//							analyseItem.cbWeaveKind[analyseItem.cbWeaveKind.length - 1] = GameConstants.WIK_LEFT;
//							analyseItem.cbCenterCard[analyseItem.cbWeaveKind.length - 1] = switch_to_card_data(i);
//							analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][0] = switch_to_card_data(i);
//							analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][1] = switch_to_card_data(i + 1);
//							analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][2] = switch_to_card_data(i + 2);
//						}
						analyseItem.eyeKind=GameConstants.WIK_LEFT;
						analyseItem.eyeCenterCard=switch_to_card_data(i);
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
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			int num = cards_index[i];

			if (num > 0) {
				
				for(int j=0;j<num;j++) {
					for(int k=0;k<4;k++) {
						if(analyseItem.cbCardEye[k]==0) {
							analyseItem.cbCardEye[k]=switch_to_card_data(i);//复制牌眼
							break;
						}
					}
				}

				if (num == 2) {
					analyseItem.isShuangDui = true;// 判断牌眼是否 双对
				} else {
					analyseItem.isShuangDui = false;
				}

				int card_data = switch_to_card_data(i);
				int color = get_card_color(card_data);
				Integer value = map.get(color);
				if (value == null)
					value = 0;
				map.put(color, value + num);

			}
		}
		for (Entry<Integer, Integer> entry : map.entrySet()) {
			if (entry.getValue() == 0)
				continue;
			if (entry.getValue() != 2 && entry.getValue() != 4) {
				return false;
			}
		}
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

	/**
	 * 当前摸牌 是不是牌眼
	 * 
	 * @param cardEyes
	 * @param cur_card
	 * @return
	 */
	private boolean isCurCardEye(AnalyseItem analyseItem, int cur_card) {
		boolean isEyes = false;
		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
			if (analyseItem.cbCardEye[i] == cur_card) {
				isEyes = true;
				break;
			}
		}
		if ((analyseItem.eyeKind==GameConstants.WIK_LEFT || analyseItem.eyeKind==GameConstants.WIK_PENG) && isEyes) {// 牌眼一张 单调才是满天飞
			return true;
		}
		return false;
	}

	// 分析扑克
	public boolean analyse_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			List<AnalyseItem> analyseItemArray, boolean has_feng, int cur_card) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);

		// 需求判断
		int cbLessKindItem = (cbCardCount - 1) / 3;

		if (cbCardCount == 1) {// 满天飞1张牌 胡牌的情况
			return true;
		}

		// 单吊判断
		if (cbLessKindItem <= 1) {
			int mj_count = GameConstants.MAX_FLS_INDEX;
			AnalyseItem analyseItem = new AnalyseItem();
			if (cbWeaveCount == 0 && cbCardCount == 3) {// 庄家对家胡牌--3张牌 胡牌的情况

				boolean chengju = isChengJu(cards_index, analyseItem);
				if (chengju) {
					// 插入结果
					analyseItemArray.add(analyseItem);
					return true;
				}
			}

			if (isYankou(cards_index, analyseItem)) {
				// 变量定义
				// 设置结果
				// for (int j = 0; j < cbWeaveCount; j++) {
				// analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
				// analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
				// get_weave_card(weaveItem[j].weave_kind,
				// weaveItem[j].center_card, analyseItem.cbCardData[j]);
				// }
				analyseItem.curCardEye = isCurCardEye(analyseItem, cur_card);
				// 插入结果
				analyseItemArray.add(analyseItem);
				return true;
			}

			return false;
		} // 单吊判断 end

		// 拆分分析
		int cbMagicCardIndex[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			cbMagicCardIndex[i] = cards_index[i];
		}
		int cbMagicCardCount = this.magic_count(cbMagicCardIndex);
		for (int i = 0; i < this._magic_card_count; i++) {
			if (cbMagicCardIndex[get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[75];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_FLS_INDEX;
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
				if ((i < (GameConstants.MAX_FLS_INDEX - 2)) && ((i % 3) == 0)) {
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
		cbLessKindItem = cbLessKindItem - 1;
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_FLS_INDEX];

			// 变量定义
			// int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5 };
			int cbIndex[] = new int[] { 0, 1, 2, 3, 4 };
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.MAX_FLS_INDEX;
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
					AnalyseItem analyseItem = new AnalyseItem();
					if (isYankou(cbCardIndexTemp, analyseItem)) {

						// 设置组合
						// for (int i = 0; i < cbWeaveCount; i++) {//赋值落地组合
						// analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
						// analyseItem.cbCenterCard[i] =
						// weaveItem[i].center_card;
						// get_weave_card(weaveItem[i].weave_kind,
						// weaveItem[i].center_card,
						// analyseItem.cbCardData[i]);
						// }

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

						analyseItem.curCardEye = isCurCardEye(analyseItem, cur_card);
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
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER
				|| player_action == GameConstants.WIK_LEFT) {
			return 10;
		}

		return 0;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int MAX_FLS_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (MAX_FLS_INDEX < index) {
				MAX_FLS_INDEX = index;
			}

		}

		return MAX_FLS_INDEX;
	}

	public int get_chi_hu_action_rank_fls(ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_MENQING)).is_empty()
				&& !(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			// 门清自摸3分（接炮算小胡，放炮者出1分）；
			wFanShu = 3;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_PENGPENGHU)).is_empty()) {
			wFanShu = 3;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GANGSHANGHUA)).is_empty()) {
			wFanShu = 4;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GANGSHANGPAO)).is_empty()) {
			wFanShu = 4;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HAIDI)).is_empty()
				&& !(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu = 4;// 最后4张牌的自摸胡牌都算海底捞
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GUNGUN)).is_empty()) {
			wFanShu = 5;
		}

		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
				wFanShu = 2;
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_SHU_FAN)).is_empty()) {
				wFanShu = 1;
			}

		}
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
		case GameConstants.WIK_ZHAO: // 招操作
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
			logger.error("get_weave_card:invalid cbWeaveKind" + cbWeaveKind);
		}
		}

		return 0;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 3) && (cbColor <= 7);
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants.MAX_FLS_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = color * 3 + value - 1;
		return index;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data(int card_index) {
		if (card_index >= GameConstants.MAX_FLS_INDEX) {
			return GameConstants.MAX_FLS_INDEX;
		}
		return ((card_index / 3) << 4) | (card_index % 3 + 1);
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
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
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
		if (cbCardCount == 0 || cbCardCount > GameConstants.MAX_FLS_COUNT)
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
		FLSGameLogic logic = new FLSGameLogic();
		int index = logic.switch_to_card_index(24);

		for (int i = 0; i < GameConstants.CARD_DATA_FLS_LX.length; i++)
			System.out.println(GameConstants.CARD_DATA_FLS_LX[i]);
	}

}
