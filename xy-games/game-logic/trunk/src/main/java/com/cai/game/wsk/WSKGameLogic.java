package com.cai.game.wsk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

public class WSKGameLogic {
	// 分析结构, 修改成内部公共类，以便继承 WSKGameLogic 可以使用该类
	public class tagAnalyseResult {
		public int cbBlockCount[] = new int[12]; // 扑克数目
		public int cbCardData[][] = new int[12][GameConstants.WSK_MAX_COUNT]; // 扑克数据

		public tagAnalyseResult() {
			Arrays.fill(cbBlockCount, 0);
			for (int i = 0; i < 12; i++) {
				Arrays.fill(cbCardData[i], 0);
			}
		}

		public void Reset() {
			Arrays.fill(cbBlockCount, 0);
			for (int i = 0; i < 12; i++) {
				Arrays.fill(cbCardData[i], 0);
			}
		}
	};

	private static Logger logger = Logger.getLogger(WSKGameLogic.class);
	public Map<Integer, Integer> ruleMap = new HashMap<>();

	public WSKGameLogic() {

	}

	/**
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param isLast
	 *            是否最后一手牌
	 * @return
	 */
	public int GetCardType(int cbCardData[], int cbCardCount, boolean isLast) {
		return 0;
	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		return GetCardType(cbCardData, cbCardCount, false);
	}

	public int get_three_link_count(int cbCardData[], int cbCardCount, int type) {
		return 0;
	}

	// 获取类型
	public int GetCardType_WSK(int cbCardData[], int cbCardCount) {
		// 简单牌型
		switch (cbCardCount) {
		case 0: // 空牌
		{
			return GameConstants.WSK_CT_ERROR;
		}
		case 1: // 单牌
		{
			return GameConstants.WSK_CT_SINGLE;
		}
		case 2: // 两张
		{
			if (GetCardValue(cbCardData[0]) == GetCardValue(cbCardData[1])) {
				if (GetCardValue(cbCardData[0]) != 0x0E && GetCardValue(cbCardData[0]) != 0x0F)
					return GameConstants.WSK_CT_DOUBLE; // 一般对
				else if (GetCardValue(cbCardData[0]) == 0x0E)
					return GameConstants.WSK_CT_KING_ST; // 纯小王
				else if (GetCardValue(cbCardData[0]) == 0x0F)
					return GameConstants.WSK_CT_KING_SK; // 纯大王
			} else if ((GetCardValue(cbCardData[0]) == 0x0E && GetCardValue(cbCardData[1]) == 0x0F)
					|| (GetCardValue(cbCardData[0]) == 0x0F && GetCardValue(cbCardData[1]) == 0x0E))
				return GameConstants.WSK_CT_KING_DC; // 大小王
			else
				return GameConstants.WSK_CT_ERROR;
		}
		case 3: // 三牌
		{
			if (GetCardValue(cbCardData[0]) == GetCardValue(cbCardData[2]))
				return GameConstants.WSK_CT_BOMB_3;
			if (GetCardValue(cbCardData[0]) == 13 && GetCardValue(cbCardData[1]) == 10
					&& GetCardValue(cbCardData[2]) == 5) {
				if (GetCardColor(cbCardData[0]) == GetCardColor(cbCardData[1])
						&& GetCardColor(cbCardData[0]) == GetCardColor(cbCardData[2]))
					return GameConstants.WSK_CT_510K_SC; // 同色510K
				return GameConstants.WSK_CT_510K_DC; // 普通510K
			}
			if ((GetCardValue(cbCardData[0]) == 0x0E || GetCardValue(cbCardData[0]) == 0x0F)
					&& (GetCardValue(cbCardData[1]) == 0x0E || GetCardValue(cbCardData[1]) == 0x0F)
					&& (GetCardValue(cbCardData[2]) == 0x0E || GetCardValue(cbCardData[2]) == 0x0F))
				return GameConstants.WSK_CT_KING_THREE; // 3王
		}
		}

		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbCardData, cbCardCount, AnalyseResult);

		// 炸弹类型
		if ((cbCardCount == 4) && (cbCardData[0] == 0x4F) && (cbCardData[3] == 0x4E))
			return GameConstants.WSK_CT_KING_FOUR;// 4王
		if ((cbCardCount == 4) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.WSK_CT_BOMB_4;
		if ((cbCardCount == 5) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.WSK_CT_BOMB_5;
		if ((cbCardCount == 6) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.WSK_CT_BOMB_6;
		if ((cbCardCount == 7) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.WSK_CT_BOMB_7;
		if ((cbCardCount == 8) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.WSK_CT_BOMB_8;

		// 顺子类型
		if ((cbCardCount >= 3) && (AnalyseResult.cbBlockCount[0] == cbCardCount)) {
			// 扑克属性
			int cbSignedCount = AnalyseResult.cbBlockCount[0];
			int cbCardColor = GetCardColor(AnalyseResult.cbCardData[0], cbSignedCount);
			if (IsStructureLink(AnalyseResult.cbCardData[0], cbSignedCount, 1))
				return GameConstants.WSK_CT_SINGLE_LINK;
		}

		// 对连类型
		if ((cbCardCount >= 6) && ((AnalyseResult.cbBlockCount[1] * 2) == cbCardCount)) {
			int cbDoubleCount = AnalyseResult.cbBlockCount[1] * 2;
			if (IsStructureLink(AnalyseResult.cbCardData[1], cbDoubleCount, 2))
				return GameConstants.WSK_CT_DOUBLE_LINK;
		}
		return GameConstants.WSK_CT_ERROR;
	}

	// 获取类型
	public int GetCardType_DMZ(int cbCardData[], int cbCardCount) {
		// 简单牌型
		switch (cbCardCount) {
		case 0: // 空牌
		{
			return GameConstants.DMZ_CT_ERROR;
		}
		case 1: // 单牌
		{
			return GameConstants.DMZ_CT_SINGLE;
		}
		case 2: // 两张
		{
			if (GetCardValue(cbCardData[0]) == GetCardValue(cbCardData[1])) {
				if (GetCardValue(cbCardData[0]) != 0x0E && GetCardValue(cbCardData[0]) != 0x0F)
					return GameConstants.DMZ_CT_DOUBLE; // 一般对
				else if (GetCardValue(cbCardData[0]) == 0x0E)
					return GameConstants.DMZ_CT_KING_ST; // 纯小王
				else if (GetCardValue(cbCardData[0]) == 0x0F)
					return GameConstants.DMZ_CT_KING_SK; // 纯大王
			} else if ((GetCardValue(cbCardData[0]) == 0x0E && GetCardValue(cbCardData[1]) == 0x0F)
					|| (GetCardValue(cbCardData[0]) == 0x0F && GetCardValue(cbCardData[1]) == 0x0E))
				return GameConstants.DMZ_CT_KING_DC; // 大小王
			else
				return GameConstants.DMZ_CT_ERROR;
		}
		case 3: // 三牌
		{
			if (GetCardValue(cbCardData[0]) == GetCardValue(cbCardData[2]))
				return GameConstants.DMZ_CT_THREE;
			if (GetCardValue(cbCardData[0]) == 13 && GetCardValue(cbCardData[1]) == 10
					&& GetCardValue(cbCardData[2]) == 5) {
				if (GetCardColor(cbCardData[0]) == GetCardColor(cbCardData[1])
						&& GetCardColor(cbCardData[0]) == GetCardColor(cbCardData[2]))
					return GameConstants.DMZ_CT_510K_SC; // 同色510K
				return GameConstants.DMZ_CT_510K_DC; // 普通510K
			}
			if ((GetCardValue(cbCardData[0]) == 0x0E || GetCardValue(cbCardData[0]) == 0x0F)
					&& (GetCardValue(cbCardData[1]) == 0x0E || GetCardValue(cbCardData[1]) == 0x0F)
					&& (GetCardValue(cbCardData[2]) == 0x0E || GetCardValue(cbCardData[2]) == 0x0F))
				return GameConstants.DMZ_CT_KING_THREE; // 3王
		}
		}

		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbCardData, cbCardCount, AnalyseResult);

		if ((cbCardCount == 4) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.DMZ_CT_FOUR;
		// 炸弹类型
		if ((cbCardCount == 4) && (cbCardData[0] == 0x4F) && (cbCardData[3] == 0x4E))
			return GameConstants.DMZ_CT_KING_FOUR;// 4王
		if ((cbCardCount == 5) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.DMZ_CT_BOMB_5;
		if ((cbCardCount == 6) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.DMZ_CT_BOMB_6;
		if ((cbCardCount == 7) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.DMZ_CT_BOMB_7;
		if ((cbCardCount == 8) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants.DMZ_CT_BOMB_8;

		// //顺子类型
		// if ((cbCardCount>=3)&&(AnalyseResult.cbBlockCount[0]==cbCardCount))
		// {
		// //扑克属性
		// int cbSignedCount=AnalyseResult.cbBlockCount[0];
		// int
		// cbCardColor=GetCardColor(AnalyseResult.cbCardData[0],cbSignedCount);
		// if ( IsStructureLink(AnalyseResult.cbCardData[0],cbSignedCount,1) )
		// return GameConstants.DMZ_CT_SINGLE_LINK;
		// }

		// 对连类型
		if (AnalyseResult.cbBlockCount[1] * 2 == cbCardCount) {
			int cbDoubleCount = AnalyseResult.cbBlockCount[1] * 2;
			if (IsStructureLink(AnalyseResult.cbCardData[1], cbDoubleCount, 2))
				return GameConstants.DMZ_CT_DOUBLE_LINK;
		}
		// 三连类型
		if (AnalyseResult.cbBlockCount[2] * 3 == cbCardCount) {
			int cbThreeCount = AnalyseResult.cbBlockCount[2] * 3;
			if (IsStructureLink(AnalyseResult.cbCardData[2], cbThreeCount, 3))
				return GameConstants.DMZ_CT_THREE_LINK;
		}
		// 四连类型
		if (AnalyseResult.cbBlockCount[3] * 4 == cbCardCount) {
			int cbFourCount = AnalyseResult.cbBlockCount[3] * 4;
			if (IsStructureLink(AnalyseResult.cbCardData[3], cbFourCount, 4))
				return GameConstants.DMZ_CT_FOUR_LINK;
		}
		return GameConstants.DMZ_CT_ERROR;
	}

	// 是否连牌
	public boolean IsStructureLink(int cbCardData[], int cbCardCount, int cbCellCount) {
		// 数目判断
		if ((cbCardCount % cbCellCount) != 0)
			return false;

		// 构造扑克
		int cbCardDataTemp[] = new int[GameConstants.WSK_MAX_COUNT];
		for (int i = 0; i < cbCardCount; i++) {
			cbCardDataTemp[i] = cbCardData[i];
		}

		SortCardList(cbCardDataTemp, cbCardCount, GameConstants.WSK_ST_ORDER);
		if (GetCardLogicValue(cbCardDataTemp[0]) > 15)
			return false;

		// 扑克排序
		SortCardList(cbCardDataTemp, cbCardCount, GameConstants.WSK_ST_ORDER);

		// 变量定义
		int cbBlockCount = cbCardCount / cbCellCount;
		int cbFirstValue = GetCardLogicValue(cbCardDataTemp[0]);

		// 无效过虑
		if (cbFirstValue > 15)
			return false;

		// 扑克搜索
		for (int i = 1; i < cbBlockCount; i++) {
			// 扑克数值
			int cbCardValue = GetCardLogicValue(cbCardDataTemp[i * cbCellCount]);

			// 特殊过虑
			if ((cbCardValue == 2) && (cbFirstValue == 14))
				continue;

			// 连牌判断
			if (cbFirstValue != (cbCardValue + i))
				return false;
		}

		// 结果判断
		return true;
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		// 排序过虑
		if (cbCardCount == 0)
			return;
		if (cbSortType == GameConstants.WSK_ST_CUSTOM)
			return;

		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			int index[] = new int[GameConstants.WSK_MAX_INDEX];
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
			}
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[index[i]] > card_index.card_index[index[j]]) {
						int temp = index[j];
						index[j] = index[i];
						index[i] = temp;
					} else if (card_index.card_index[index[i]] == card_index.card_index[index[j]]) {
						if (index[i] > index[j]) {
							int temp = index[j];
							index[j] = index[i];
							index[i] = temp;
						}
					}
				}
			}

			int sort_num = 0;
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					cbCardData[sort_num++] = card_index.card_data[index[i]][j];
				}
			}
			return;
		}
		// 转换数值
		int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
		for (int i = 0; i < cbCardCount; i++) {
			switch (cbSortType) {
			case GameConstants.WSK_ST_ORDER: // 等级排序
			{
				cbSortValue[i] = GetCardLogicValue(cbCardData[i]);
				break;
			}
			case GameConstants.WSK_ST_VALUE: // 数值排序
			{
				cbSortValue[i] = GetCardValue(cbCardData[i]);
				break;
			}
			case GameConstants.WSK_ST_COLOR: // 花色排序
			{
				cbSortValue[i] = GetCardColor(cbCardData[i]) + GetCardLogicValue(cbCardData[i]);
				break;
			}
			}
		}

		// 排序操作
		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if ((cbSortValue[i] < cbSortValue[i + 1])
						|| ((cbSortValue[i] == cbSortValue[i + 1]) && (cbCardData[i] < cbCardData[i + 1]))) {
					// 设置标志
					bSorted = false;

					// 扑克数据
					cbSwitchData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbSwitchData;

					// 排序权位
					cbSwitchData = cbSortValue[i];
					cbSortValue[i] = cbSortValue[i + 1];
					cbSortValue[i + 1] = cbSwitchData;
				}
			}
			cbLast--;
		} while (bSorted == false);

		return;
	}

	// 删除扑克
	public boolean RemoveCard(int cbRemoveCard[], int cbRemoveCount, int cbCardData[], int cbCardCount) {
		// 定义变量
		int cbDeleteCount = 0, cbTempCardData[] = new int[cbCardCount];
		if (cbCardCount > cbTempCardData.length)
			return false;
		for (int i = 0; i < cbCardCount; i++) {
			cbTempCardData[i] = cbCardData[i];
		}

		// 置零扑克
		for (int i = 0; i < cbRemoveCount; i++) {
			for (int j = 0; j < cbCardCount; j++) {
				if (cbRemoveCard[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}
		if (cbDeleteCount != cbRemoveCount)
			return false;

		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbTempCardData[i] != 0)
				cbCardData[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType_WSK(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType_WSK(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子和单龙
		if (cbNextType >= GameConstants.WSK_CT_BOMB_3 && cbFirstType <= GameConstants.WSK_CT_SINGLE_LINK)
			return true;

		// 第二类型牌
		if (cbNextType >= GameConstants.WSK_CT_BOMB_3 && cbFirstType != GameConstants.WSK_CT_DOUBLE_LINK) {
			if (cbNextType != cbFirstType) // 不同类型直接比较
				return cbNextType > cbFirstType;
			else if (cbNextType == GameConstants.WSK_CT_510K_SC) // 类型相同且是纯510K时，需要比较花色
				return GetCardColor(cbNextCard[0]) > GetCardColor(cbFirstCard[0]);
			else
				return GetCardLogicValue(cbNextCard[cbNextCount - 1]) > GetCardLogicValue(
						cbFirstCard[cbFirstCount - 1]);
		}

		// 第一类牌型的不同类型比较
		if (cbFirstType != cbNextType) {
			if (cbFirstType == GameConstants.WSK_CT_DOUBLE_LINK) {
				if (cbNextType >= GameConstants.WSK_CT_BOMB_4)
					return true;
				else
					return false;
			}
			// 特殊处理 数目不同不比较

			if (cbNextCount != cbFirstCount)
				return false;
			else
				return cbNextType > cbFirstType;
		}

		// 相同类型
		switch (cbFirstType) {
		case GameConstants.WSK_CT_SINGLE: // 单牌类型
		case GameConstants.WSK_CT_DOUBLE: // 对子类型
		{
			// 变量定义
			int cbConsultNext = GetCardLogicValue(cbNextCard[0]);
			int cbConsultFirst = GetCardLogicValue(cbFirstCard[0]);

			return cbConsultNext > cbConsultFirst;
		}
		case GameConstants.WSK_CT_SINGLE_LINK: // 单连类型
		case GameConstants.WSK_CT_DOUBLE_LINK: // 对连类型
		{
			if (cbNextCount != cbFirstCount)
				return false;
			return CompareCardByValue(cbFirstCard, cbNextCard, cbFirstCount, cbNextCount);
		}
		}

		return false;
	}

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type, int three_link_count) {
	}

	// 对比扑克
	public boolean CompareCard_DMZ(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType_DMZ(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType_DMZ(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子和单龙,连对
		if (cbNextType >= GameConstants.DMZ_CT_510K_DC && cbFirstType <= GameConstants.DMZ_CT_FOUR_LINK)
			return true;

		// 炸弹比较
		if (cbFirstType >= GameConstants.DMZ_CT_510K_DC && cbNextType >= GameConstants.DMZ_CT_510K_DC) {
			if (cbNextType > cbFirstType) {
				return true;
			} else if (cbFirstType == cbNextType) {
				if (cbFirstType == GameConstants.DMZ_CT_510K_SC) {
					// 变量定义
					int cbConsultNext = GetCardColor(cbNextCard[0]);
					int cbConsultFirst = GetCardColor(cbFirstCard[0]);

					return cbConsultNext > cbConsultFirst;
				} else {
					// 变量定义
					int cbConsultNext = GetCardLogicValue(cbNextCard[0]);
					int cbConsultFirst = GetCardLogicValue(cbFirstCard[0]);

					return cbConsultNext > cbConsultFirst;
				}

			} else {
				return false;
			}
		}

		if (cbNextCount != cbFirstCount)
			return false;
		if (cbFirstType != cbNextType) {
			return false;
		}

		// 相同类型
		switch (cbFirstType) {
		case GameConstants.DMZ_CT_SINGLE: // 单牌类型
		case GameConstants.DMZ_CT_DOUBLE: // 对子类型
		case GameConstants.DMZ_CT_THREE: // 三张类型
		case GameConstants.DMZ_CT_FOUR: // 四张类型
		{
			// 变量定义
			int cbConsultNext = GetCardLogicValue(cbNextCard[0]);
			int cbConsultFirst = GetCardLogicValue(cbFirstCard[0]);

			return cbConsultNext > cbConsultFirst;
		}
		case GameConstants.DMZ_CT_SINGLE_LINK: // 单连类型
		case GameConstants.DMZ_CT_DOUBLE_LINK: // 对连类型
		case GameConstants.DMZ_CT_THREE_LINK: // 三连类型
		case GameConstants.DMZ_CT_FOUR_LINK: // 四连类型
		{
			// 变量定义
			int cbConsultNext = GetCardLogicValue(cbNextCard[0]);
			int cbConsultFirst = GetCardLogicValue(cbFirstCard[0]);

			return cbConsultNext > cbConsultFirst;
		}
		}

		return false;
	}

	// 对比扑克
	public boolean CompareCardByValue(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 变量定义
		boolean bHaveTwoNext = false;
		int cbConsultNext[] = new int[2];
		for (int i = 0; i < 2; i++) {
			cbConsultNext[i] = 0x00;
		}

		// 参照扑克
		for (int i = 0; i < cbNextCount; i++) {
			// 获取数值
			int cbConsultValue = GetCardValue(cbNextCard[i]);

			// 设置变量
			if ((bHaveTwoNext == false) && (cbConsultValue == 0x02))
				bHaveTwoNext = true;

			// 设置扑克
			if (cbConsultValue == 0x01) {
				if (14 > cbConsultNext[0])
					cbConsultNext[0] = 14;
				if (cbConsultValue > cbConsultNext[1])
					cbConsultNext[1] = cbConsultValue;
			} else {
				if (cbConsultValue > cbConsultNext[0])
					cbConsultNext[0] = cbConsultValue;
				if (cbConsultValue > cbConsultNext[1])
					cbConsultNext[1] = cbConsultValue;
			}
		}

		// 变量定义
		boolean bHaveTwoFirst = false;
		int cbConsultFirst[] = new int[2];
		for (int i = 0; i < 2; i++) {
			cbConsultFirst[i] = 0x00;
		}

		// 参照扑克
		for (int i = 0; i < cbFirstCount; i++) {
			// 获取数值
			int cbConsultValue = GetCardValue(cbFirstCard[i]);

			// 设置变量
			if ((bHaveTwoFirst == false) && (cbConsultValue == 0x02))
				bHaveTwoFirst = true;

			// 设置扑克
			if (cbConsultValue == 0x01) {
				if (14 > cbConsultFirst[0])
					cbConsultFirst[0] = 14;
				if (cbConsultValue > cbConsultFirst[1])
					cbConsultFirst[1] = cbConsultValue;
			} else {
				if (cbConsultValue > cbConsultFirst[0])
					cbConsultFirst[0] = cbConsultValue;
				if (cbConsultValue > cbConsultFirst[1])
					cbConsultFirst[1] = cbConsultValue;
			}
		}

		// 对比扑克
		int cbResultNext = (bHaveTwoNext == false) ? cbConsultNext[0] : cbConsultNext[1];
		int cbResultFirst = (bHaveTwoFirst == false) ? cbConsultFirst[0] : cbConsultFirst[1];

		return cbResultNext > cbResultFirst;
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

	// 洗牌
	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);

	}

	public int GetCardScore(int cbCardData[], int cbCardCount) {
		int score = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardLogicValue(cbCardData[i]) == 5) {
				score += 5;
			} else if (this.GetCardLogicValue(cbCardData[i]) == 10 || this.GetCardLogicValue(cbCardData[i]) == 13) {
				score += 10;
			}
		}
		return score;
	}

	// 获取数值
	public int GetCardValue(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int GetCardColor(int cbCardData) {
		return (cbCardData & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	// 获取花色
	public int GetCardColor(int cbCardData[], int cbCardCount) {
		// 效验参数
		if (cbCardCount == 0)
			return 0xF0;

		// 首牌花色
		int cbCardColor = GetCardColor(cbCardData[0]);

		// 花色判断
		for (int i = 0; i < cbCardCount; i++) {
			if (GetCardColor(cbCardData[i]) != cbCardColor)
				return 0xF0;
		}

		return cbCardColor;
	}

	// 分析扑克
	public void AnalysebCardData(int cbCardData[], int cbCardCount, tagAnalyseResult AnalyseResult) {
		// 设置结果
		AnalyseResult.Reset();

		// 扑克分析
		// for (BYTE i=0;i<cbCardCount;i++)
		for (int i = 0; i < cbCardCount;) {
			// 变量定义
			int cbSameCount = 1, cbCardValueTemp = 0;
			int cbLogicValue = GetCardLogicValue(cbCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			// 设置结果
			int cbIndex = AnalyseResult.cbBlockCount[cbSameCount - 1]++;
			for (int j = 0; j < cbSameCount; j++) {
				AnalyseResult.cbCardData[cbSameCount - 1][cbIndex * cbSameCount + j] = cbCardData[i + j];
			}

			// 设置索引
			i += cbSameCount;
			// i+=cbSameCount-1;
		}

		return;
	}

	// 是否连
	/*
	 * card_data_index 牌数据 link_num 几连
	 */
	public boolean is_link(tagAnalyseIndexResult_WSK card_data_index, int link_num, int link_count_num) {
		int pai_count = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
			pai_count += card_data_index.card_index[i];
		}
		int num = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
			if (card_data_index.card_index[i] == 0) {
				if (num == 0) {
					continue;
				} else {
					if (num >= link_count_num && (num * link_num == pai_count)) {
						return true;
					} else {
						return false;
					}
				}
			}

			if (card_data_index.card_index[i] == link_num) {
				num++;
			} else {
				return false;
			}
		}
		if (num >= link_count_num) {
			return true;
		} else {
			return false;
		}
	}

	// 是否连 123456
	/*
	 * card_data_index 牌数据 link_num 几连
	 */
	public boolean is_link_other(int cbCardData[], int cbCardCount, int link_num, int link_count_num) {
		tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_data_index);
		int pai_count = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			pai_count += card_data_index.card_index[i];
		}
		if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] >= link_num) {
			if (card_data_index.card_index[10] > 0) {
				return false;
			}
			int num = 0;
			for (int i = 1; i < 13; i++) {
				int index = this.switch_card_to_idnex(i);
				if (card_data_index.card_index[index] == 0) {
					if (num == 0) {
						continue;
					} else {
						if (num >= link_count_num && (num * link_num == pai_count)) {
							return true;
						} else {
							return false;
						}
					}
				}

				if (card_data_index.card_index[index] == link_num) {
					num++;
				} else {
					return false;
				}
			}
			if (num >= link_count_num) {
				return true;
			} else {
				return false;
			}
		} else {
			int num = 0;
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
				if (card_data_index.card_index[i] == 0) {
					if (num == 0) {
						continue;
					} else {
						if (num >= link_count_num && (num * link_num == pai_count)) {
							return true;
						} else {
							return false;
						}
					}
				}

				if (card_data_index.card_index[i] == link_num) {
					num++;
				} else {
					return false;
				}
			}
			if (num >= link_count_num) {
				return true;
			} else {
				return false;
			}
		}

	}

	// 飞机 0飞机缺翅膀 1飞机
	public int is_plane(tagAnalyseIndexResult_WSK card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount < 6) {
			return -1;
		}
		int num = 0;
		for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
			// 三个2不能当做飞机
			if (card_data_index.card_index[i] >= 3) {
				int link_num = 1;
				for (int j = i - 1; j >= 0; j--) {
					if (card_data_index.card_index[j] >= 3) {
						link_num++;
						if (link_num * 5 == cbCardCount) {
							return 1;
						} else if (link_num * 5 > cbCardCount) {
							return 0;
						}
					} else {
						i = j + 1;
						break;
					}
				}
			}
		}

		return -1;
	}

	// 飞机 0飞机缺翅膀 1飞机
	public int is_plane_other(tagAnalyseIndexResult_WSK card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount < 6) {
			return -1;
		}
		int num = 0;
		if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0) {
			for (int i = 13; i >= 1; i--) {
				int index = this.switch_card_to_idnex(i);
				if (card_data_index.card_index[index] >= 3) {
					int link_num = 1;
					for (int j = i - 1; j >= 1; j--) {
						int otherindex = this.switch_card_to_idnex(j);
						if (card_data_index.card_index[otherindex] >= 3) {
							link_num++;
							if (link_num * 5 == cbCardCount) {
								return 1;
							} else if (link_num * 5 > cbCardCount) {
								return 0;
							}
						} else {
							i = j - 1;
							break;
						}
					}
				}
			}
		} else {
			for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
				// 三个2不能当做飞机
				if (card_data_index.card_index[i] >= 3) {
					int link_num = 1;
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] >= 3) {
							link_num++;
							if (link_num * 5 == cbCardCount) {
								return 1;
							} else if (link_num * 5 > cbCardCount) {
								return 0;
							}
						} else {
							i = j + 1;
							break;
						}
					}
				}
			}
		}

		return -1;
	}

	// 飞机 0飞机缺翅膀 1飞机
	public int get_plane_max_index(tagAnalyseIndexResult_WSK card_data_index, int cbCardData[], int cbCardCount,
			int three_link_count) {
		if (cbCardCount < 6) {
			return -1;
		}
		int num = 0;
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			// 三个2不能当做飞机
			if (card_data_index.card_index[index] >= 3 && index != 12) {
				int link_num = 1;
				int next_index = index;
				for (int j = i + card_data_index.card_index[index]; j < cbCardCount; j++) {
					int other_index = this.switch_card_to_idnex(cbCardData[j]);
					if (card_data_index.card_index[other_index] >= 3 && other_index != 12) {
						if (other_index != next_index + 1) {
							break;
						}
						next_index = other_index;
						link_num++;
						if (link_num * 5 == cbCardCount) {
							return next_index;
						} else if (link_num * 5 > cbCardCount) {
							return next_index;
						}
					}
					if (card_data_index.card_index[other_index] > 0) {
						j += card_data_index.card_index[other_index];
					} else {
						j++;
					}
				}
			}
			if (card_data_index.card_index[index] > 0) {
				i += card_data_index.card_index[index];
			} else {
				i++;
			}
		}

		return -1;
	}

	public int get_plane_count(tagAnalyseIndexResult_WSK card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount <= 6) {
			return -1;
		}
		int num = 0;
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			// 三个2不能当做飞机
			if (card_data_index.card_index[index] >= 3 && index != 12) {
				int link_num = 1;
				int next_index = index;
				for (int j = i + card_data_index.card_index[index]; j < cbCardCount; j++) {
					int other_index = this.switch_card_to_idnex(cbCardData[j]);
					if (card_data_index.card_index[other_index] >= 3 && other_index != 12) {
						if (other_index != next_index + 1) {
							break;
						}
						next_index = other_index;
						link_num++;
						if (link_num * 5 == cbCardCount) {
							return link_num;
						} else if (link_num * 5 > cbCardCount) {
							return link_num;
						}
					}
					if (card_data_index.card_index[other_index] > 0) {
						j += card_data_index.card_index[other_index];
					} else {
						j++;
					}
				}
			}
			if (card_data_index.card_index[index] > 0) {
				i += card_data_index.card_index[index];
			} else {
				i++;
			}
		}

		return -1;
	}

	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = GetCardColor(CardData);
		int cbCardValue = GetCardValue(CardData);

		// 转换数值
		if (cbCardColor == 4)
			return cbCardValue + 2;
		return (cbCardValue == 2 || cbCardValue == 1) ? (cbCardValue + 13) : cbCardValue;
	}

	public int GetCardXianScore(int cbCardData[], int cbCardCount, int card_type) {
		int score = 0;
		return score;
	}

	public int GetHandCardXianScore(int cbCardData[], int cbCardCount) {
		return GetHandCardXianScore(cbCardData, cbCardCount, 0);
	}

	/**
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param sheng_dang_biaozhi
	 *            升档标志
	 * @return
	 */
	public int GetHandCardXianScore(int cbCardData[], int cbCardCount, int sheng_dang_biaozhi) {
		int score = 0;
		return score;
	}

	public int Get_Wang_Count(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		return Get_Wang_Count(card_index);
	}

	public int Get_Wang_Count(tagAnalyseIndexResult_WSK AnalyseIndexResult) {
		return AnalyseIndexResult.card_index[13] + AnalyseIndexResult.card_index[14];
	}

	public int Get_510K_Count(int cbCardData[], int cbCardCount) {
		boolean is_wu = false;
		boolean is_shi = false;
		boolean is_K = false;
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardValue(cbCardData[i]) == 5) {
				is_wu = true;
			}
			if (this.GetCardValue(cbCardData[i]) == 10) {
				is_shi = true;
			}
			if (this.GetCardValue(cbCardData[i]) == 13) {
				is_K = true;
			}
		}
		if (is_wu && is_shi && is_K) {
			return 1;
		}
		return 0;
	}

	public boolean have_card_num(int cbCardData[], int cbCardCount, int num) {
		tagAnalyseIndexResult_WSK card_card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_card_index);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (card_card_index.card_index[i] >= num) {
				return true;
			}
		}
		return false;
	}

	public int get_card_num_count(int cbCardData[], int cbCardCount, int num) {
		int count = 0;
		tagAnalyseIndexResult_WSK card_card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_card_index);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (card_card_index.card_index[i] == num) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 获取手上某张牌的数量
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param pai
	 * @return
	 */
	public int have_pai_num(int cbCardData[], int cbCardCount, int pai) {
		int num = 0;
		tagAnalyseIndexResult_WSK card_card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_card_index);
		num = card_card_index.card_index[pai];
		return num;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_WSK AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.WSK_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] == 0) {
				continue;
			}
			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	public void make_change_card(int cbCardData[], int cbCardCount, int cbReadData[], int type) {

	}

	public int get_liang_pai(int card_data[], int cbCardCount) {
		return 0;
	}

	public void switch_to_card_index(int card_data[], int card_count, int card_index[]) {
		for (int i = 0; i < card_count; i++) {
			int index = GetCardLogicValue(card_data[i]);
			card_index[index - 3]++;
		}
	}

	public int switch_idnex_to_data(int index) {
		return index + 3;
	}

	public int switch_card_to_idnex(int card) {
		int index = GetCardLogicValue(card) - 3;
		return index;
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

	// 对比扑克
	/**
	 * 
	 * @param cbFirstCard
	 * @param cbNextCard
	 * @param cbFirstCount
	 * @param cbNextCount
	 * @param pre_is_Last
	 *            上一手牌
	 * @param next_is_Last
	 *            下一手牌
	 * @return
	 */
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,
			boolean pre_is_Last, boolean next_is_Last, int three_link_count) {
		return false;
	}

	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,
			boolean pre_is_Last, boolean next_is_Last) {
		return false;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		return 1;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int three_link_count) {
		return 1;
	}
}
