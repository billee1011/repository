package com.cai.hp.table;

import java.util.HashMap;
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

public class NiuGameLogic {

	private static Logger logger = Logger.getLogger(NiuGameLogic.class);

	public NiuGameLogic() {

	}

	public static final int MAX_COUNT = 5;

	public static final int OX_VALUE0 = 0; // 混合牌型
	public static final int OX_THREE_SAME = 12; // 三条牌型
	public static final int OX_FOUR_SAME = 13; // 四条牌型
	public static final int OX_FOURKING = 14; // 天王牌型
	public static final int OX_FIVEKING = 15; // 天王牌型

	// 获取类型
	public int getCardType(int cbCardData[], int cbCardCount) {
		assert (cbCardCount == MAX_COUNT);

		int bKingCount = 0, bTenCount = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (get_card_value(cbCardData[i]) > 10) {
				bKingCount++;
			} else if (get_card_value(cbCardData[i]) == 10) {
				bTenCount++;
			}
		}
		if (bKingCount == MAX_COUNT)
			return OX_FIVEKING;
		else if (bKingCount == MAX_COUNT - 1 && bTenCount == 1)
			return OX_FOURKING;

		int bTemp[] = new int[MAX_COUNT];
		int bSum = 0;
		for (int i = 0; i < cbCardCount; i++) {
			bTemp[i] = get_card_value(cbCardData[i]);
			bSum += bTemp[i];
		}

		for (int i = 0; i < cbCardCount - 1; i++) {
			for (int j = i + 1; j < cbCardCount; j++) {
				if ((bSum - bTemp[i] - bTemp[j]) % 10 == 0) {
					return ((bTemp[i] + bTemp[j]) > 10) ? (bTemp[i] + bTemp[j] - 10) : (bTemp[i] + bTemp[j]);
				}
			}
		}

		return OX_VALUE0;
	}

	// 获取倍数
	public int getTimes(int cbCardData[], int cbCardCount) {
		if (cbCardCount != MAX_COUNT)
			return 0;

		int bTimes = getCardType(cbCardData, MAX_COUNT);
		if (bTimes < 7)
			return 1;
		else if (bTimes == 7)
			return 2;
		else if (bTimes == 8)
			return 3;
		else if (bTimes == 9)
			return 4;
		else if (bTimes == 10)
			return 5;
		else if (bTimes == OX_FOURKING)
			return 5;
		else if (bTimes == OX_FIVEKING)
			return 5;

		return 0;
	}

	// 获取牛牛
	public boolean getOxCard(int cbCardData[], int cbCardCount) {
		assert (cbCardCount == MAX_COUNT);

		// 设置变量
		int bTemp[] = new int[MAX_COUNT];
		int bTempData[] = new int[MAX_COUNT];
		for (int i = 0; i < MAX_COUNT; i++) {
			bTempData[i] = cbCardData[i];
		}

		int bSum = 0;
		for (int i = 0; i < cbCardCount; i++) {
			bTemp[i] = get_card_color(cbCardData[i]);
			bSum += bTemp[i];
		}

		// 查找牛牛
		for (int i = 0; i < cbCardCount - 1; i++) {
			for (int j = i + 1; j < cbCardCount; j++) {
				if ((bSum - bTemp[i] - bTemp[j]) % 10 == 0) {
					int bCount = 0;
					for (int k = 0; k < cbCardCount; k++) {
						if (k != i && k != j) {
							cbCardData[bCount++] = bTempData[k];
						}
					}
					if (bCount == 3) {
						cbCardData[bCount++] = bTempData[i];
						cbCardData[bCount++] = bTempData[j];

						return true;
					}
					
				}
			}
		}

		return false;
	}

	// 获取整数
	boolean isIntValue(int cbCardData[], int cbCardCount) {
		int sum = 0;
		for (int i = 0; i < cbCardCount; i++) {
			sum += getCardLogicValue(cbCardData[i]);
		}
		assert (sum > 0);
		return (sum % 10 == 0);
	}

	// 获取数值
	public int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	// 逻辑数值
	public int getCardLogicValue(int cbCardData) {
		// 扑克属性
		int bCardColor = get_card_color(cbCardData);
		int bCardValue = get_card_value(cbCardData);
		// 转换数值
		return (bCardValue > 10) ? (10) : bCardValue;
	}

	// 排列扑克
	public void sortCardList(int cbCardData[], int cbCardCount) {
		// 转换数值
		int cbLogicValue[] = new int[MAX_COUNT];
		for (int i = 0; i < cbCardCount; i++)
			cbLogicValue[i] = get_card_value(cbCardData[i]);

		// 排序操作
		boolean bSorted = true;
		int cbTempData, bLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < bLast; i++) {
				if ((cbLogicValue[i] < cbLogicValue[i + 1])
						|| ((cbLogicValue[i] == cbLogicValue[i + 1]) && (cbCardData[i] < cbCardData[i + 1]))) {
					// 交换位置
					cbTempData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbTempData;
					cbTempData = cbLogicValue[i];
					cbLogicValue[i] = cbLogicValue[i + 1];
					cbLogicValue[i + 1] = cbTempData;
					bSorted = false;
				}
			}
			bLast--;
		} while (bSorted == false);

		return;
	}

	// 对比扑克
	public boolean compareCard(int cbFirstData[], int cbNextData[], int cbCardCount, boolean FirstOX, boolean NextOX) {
		if (FirstOX != NextOX)
			return false;

		// 比较牛大小
		if (FirstOX) {
			// 获取点数
			int cbNextType = getCardType(cbNextData, cbCardCount);
			int cbFirstType = getCardType(cbFirstData, cbCardCount);

			// 点数判断
			if (cbFirstType != cbNextType)
				return (cbFirstType > cbNextType);
		}

		// 排序大小
		int bFirstTemp[] = new int[MAX_COUNT];
		int bNextTemp[] = new int[MAX_COUNT];

		for (int i = 0; i < MAX_COUNT; i++) {
			bFirstTemp[i] = cbFirstData[i];
		}
		for (int i = 0; i < MAX_COUNT; i++) {
			bNextTemp[i] = cbNextData[i];
		}

		sortCardList(bFirstTemp, cbCardCount);
		sortCardList(bNextTemp, cbCardCount);

		// 比较数值
		int cbNextMaxValue = get_card_value(bNextTemp[0]);
		int cbFirstMaxValue = get_card_value(bFirstTemp[0]);
		if (cbNextMaxValue != cbFirstMaxValue)
			return cbFirstMaxValue > cbNextMaxValue;

		// 比较颜色
		return get_card_color(bFirstTemp[0]) > get_card_color(bNextTemp[0]);

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
		boolean same = true;
		int lastCard = 0;
		int num = 0;
		for (int eye : analyseItem.cbCardEye) {
			if (eye == 0)
				continue;
			if (lastCard == 0) {
				lastCard = eye;
			}
			if (eye != lastCard) {
				same = false;
			}
			num++;
		}
		return analyseItem.isShuangDui || (same && num == 4);// 2对子
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
	 * 落地牌 是否暗 杠 明杠
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_an_gang(WeaveItem weaveItems[], int weaveCount) {
		boolean isAnGang = true;
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind != GameConstants.WIK_GANG
					&& weaveItems[i].weave_kind != GameConstants.WIK_ZHAO)) {
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

		int cbMagicCardIndex[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			int num = cbMagicCardIndex[i] = cards_index[i];
			if (num > 0) {
				for (int j = 0; j < num; j++) {
					for (int k = 0; k < 4; k++) {
						if (analyseItem.cbCardEye[k] == 0) {
							analyseItem.cbCardEye[k] = switch_to_card_data(i);// 复制牌眼
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
			if ((i < (GameConstants.MAX_FLS_INDEX - 2)) && ((i % 3) == 0)) {
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
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			int num = cards_index[i];

			if (num > 0) {

				for (int j = 0; j < num; j++) {
					for (int k = 0; k < 4; k++) {
						if (analyseItem.cbCardEye[k] == 0) {
							analyseItem.cbCardEye[k] = switch_to_card_data(i);// 复制牌眼
							break;
						}
					}
				}

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

	public boolean isInCardEye(AnalyseItem analyseItem, int cur_card) {
		boolean isEyes = false;
		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
			if (analyseItem.cbCardEye[i] == cur_card) {
				isEyes = true;
				break;
			}
		}
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
		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
			if (analyseItem.cbCardEye[i] == cur_card) {
				isEyes = true;
				break;
			}
		}

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

		// if
		// (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GANGSHANGHUA)).is_empty()
		// && !(chiHuRight.opr_and(GameConstants.CHR_FLS_MENQING)).is_empty())
		// {//门前 杠上花 5分
		// wFanShu = 5;
		// }

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
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {

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
	}

}
