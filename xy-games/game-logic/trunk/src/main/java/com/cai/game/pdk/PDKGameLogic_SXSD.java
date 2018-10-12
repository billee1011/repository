/**
 * 
 */
package com.cai.game.pdk;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.cai.game.pdk.data.tagAnalyseIndexResult;


public class PDKGameLogic_SXSD extends PDKGameLogic{

	/**
	 * 
	 */
	protected final Logger logger = LoggerFactory.getLogger(PDKGameLogic_SXSD.class);
	private int cbIndexCount = 5;
	//public int _game_rule_index; // 游戏规则
	//public int _game_type_index;
	//public int _laizi = GameConstants.INVALID_CARD;// 癞子牌数据
	//public boolean _is_boom;

	public PDKGameLogic_SXSD() {

	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		int cbTempData[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			cbTempData[i] = cbCardData[i];
		}
		this.sort_card_date_list(cbTempData, cbCardCount);
		int nlaizicount = this.GetLaiZiCount(cbRealData, cbCardCount);
		// 简单牌型
		switch (cbCardCount) {
		case 0: // 空牌
		{
			return GameConstants.PDK_CT_ERROR;
		}
		case 1: // 单牌
		{
			return GameConstants.PDK_CT_SINGLE;
		}
		case 2: // 对牌
		{
			// 牌型判断
			if (GetCardLogicValue(cbTempData[0]) == GetCardLogicValue(cbTempData[1]))
				return GameConstants.PDK_CT_DOUBLE;

			return GameConstants.PDK_CT_ERROR;
		}
		case 4: // 天王炸
		{
			boolean bMissileCard = true;
			for (int cbCardIdx = 0; cbCardIdx < cbCardCount; ++cbCardIdx) {
				if (this.get_card_color(cbTempData[cbCardIdx]) != 0x40) {
					bMissileCard = false;
					break;
				}
			}
			if (bMissileCard)
				return GameConstants.PDK_CT_MISSILE_CARD;
		}
		}

		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbTempData, cbCardCount, AnalyseResult);

		if (has_rule(GameConstants.GAME_RULE_BOOM) && has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)) {
			if (cbCardCount > 4 && AnalyseResult.cbFourCount > 0) {
				return GameConstants.PDK_CT_ERROR;
			}
		}
		// 炸弹判断
		if (4 <= cbCardCount && cbCardCount <= 8) {
			// 牌型判断
			if ((AnalyseResult.cbFourCount == 1) && (cbCardCount == 4)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else if (nlaizicount == 4) {
					return GameConstants.PDK_CT_MAGIC_BOOM;
				} else {
					return GameConstants.PDK_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbFiveCount == 1) && (cbCardCount == 5)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
			if ((AnalyseResult.cbSixCount == 1) && (cbCardCount == 6)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
			if ((AnalyseResult.cbSevenCount == 1) && (cbCardCount == 7)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
			if ((AnalyseResult.cbEightCount == 1) && (cbCardCount == 8)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
		}
		if (AnalyseResult.cbFourCount == 1) {
			if (has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
				if (cbCardCount == 5) {
					return GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE;
				} else if (cbCardCount == 6) {
					return GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO;
				} else if (cbCardCount == 7) {
					return GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE;
				}
			}

		}

		/////////////////////////////////////////////////////////////////////////////////

		/////////////////////////////////////////////////////////////////////////////////

		// 三牌判断
		if (AnalyseResult.cbThreeCount > 0) {
			if (AnalyseResult.cbThreeCount == 1 && cbCardCount == 5) {
				for (int i = 0; i < AnalyseResult.cbSignedCount; i++) {
					if (get_card_value(AnalyseResult.cbSignedCardData[i]) == get_card_value(
							AnalyseResult.cbThreeCardData[0])) {
						return GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE;
					}
				}
			}
			// 三条类型
			if (AnalyseResult.cbThreeCount == 1 && cbCardCount == 3)
				return GameConstants.PDK_CT_THREE;
			if (AnalyseResult.cbThreeCount * 5 == cbCardCount && cbCardCount == 5)
				return GameConstants.PDK_CT_THREE_TAKE_TWO;
			if ((AnalyseResult.cbThreeCount * 4 == cbCardCount) && (cbCardCount == 4))
				return GameConstants.PDK_CT_THREE_TAKE_ONE;

			// 连牌判断
			if (AnalyseResult.cbThreeCount > 1) {
				// 变量定义
				int CardData = AnalyseResult.cbThreeCardData[0];
				int cbFirstLogicValue = GetCardLogicValue(CardData);
				int nLink_Three_Count = 0;

				// 错误过虑
				if (cbFirstLogicValue >= 15)
					return GameConstants.PDK_CT_ERROR;

				// 连牌判断
				int value_add = 0;
				for (int i = 0; i < AnalyseResult.cbThreeCount; i++) {
					if (nLink_Three_Count * 5 == cbCardCount) {
						return GameConstants.PDK_CT_PLANE;
					} else if (nLink_Three_Count * 5 > cbCardCount) {
						return GameConstants.PDK_CT_PLANE_LOST;
					}
					int CardDatatemp = AnalyseResult.cbThreeCardData[i * 3];
					if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + value_add)) {
						if (nLink_Three_Count * 5 == cbCardCount) {
							return GameConstants.PDK_CT_PLANE;
						}
						cbFirstLogicValue = GetCardLogicValue(AnalyseResult.cbThreeCardData[nLink_Three_Count * 3]);
						nLink_Three_Count = 1;
						value_add = 1;
						continue;
					}
					value_add++;
					nLink_Three_Count++;
				}
				if (nLink_Three_Count * 5 == cbCardCount) {
					return GameConstants.PDK_CT_PLANE;
				}
				if (nLink_Three_Count <= 1) {
					return GameConstants.PDK_CT_ERROR;
				}
				if (nLink_Three_Count * 5 > cbCardCount && nLink_Three_Count * 3 <= cbCardCount) {
					return GameConstants.PDK_CT_PLANE_LOST;
				}
			}

			// 牌形判断
			if (AnalyseResult.cbFourCount > 0) {
				int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
				switch_to_card_index(cbCardData, cbCardCount, card_index);
				int link_count = 1;
				for (int i = 0; i < GameConstants.PDK_MAX_INDEX - 2; i++) {
					if (card_index[i] > 0) {
						if ((card_index[i] == 4 || card_index[i] == 3)
								&& (card_index[i + 1] == 4 || card_index[i + 1] == 3)) {
							link_count++;
							if (link_count >= 2) {
								if (link_count * 5 == cbCardCount) {
									return GameConstants.PDK_CT_PLANE;
								} else if (link_count * 5 > cbCardCount) {
									return GameConstants.PDK_CT_PLANE_LOST;
								}

							}
						}
					}

				}
			}

			return GameConstants.PDK_CT_ERROR;
		}

		// 两张类型
		if (AnalyseResult.cbDoubleCount >= 2) {
			// 变量定义
			int CardData = AnalyseResult.cbDoubleCardData[0];
			int cbFirstLogicValue = GetCardLogicValue(CardData);

			// 错误过虑
			if (cbFirstLogicValue >= 15)
				return GameConstants.PDK_CT_ERROR;

			// 连牌判断
			for (int i = 1; i < AnalyseResult.cbDoubleCount; i++) {
				int CardDatatemp = AnalyseResult.cbDoubleCardData[i * 2];
				if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + i))
					return GameConstants.PDK_CT_ERROR;
			}

			// 二连判断
			if ((AnalyseResult.cbDoubleCount * 2) == cbCardCount)
				return GameConstants.PDK_CT_DOUBLE_LINE;

			return GameConstants.PDK_CT_ERROR;
		}

		// 单张判断
		if ((AnalyseResult.cbSignedCount >= 5) && (AnalyseResult.cbSignedCount == cbCardCount)) {
			// 变量定义
			int CardData = AnalyseResult.cbSignedCardData[0];
			int cbFirstLogicValue = GetCardLogicValue(CardData);

			// 错误过虑
			if (cbFirstLogicValue >= 15)
				return GameConstants.PDK_CT_ERROR;

			// 连牌判断
			for (int i = 1; i < AnalyseResult.cbSignedCount; i++) {
				int CardDatatemp = AnalyseResult.cbSignedCardData[i];
				if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + i))
					return GameConstants.PDK_CT_ERROR;
			}

			CardData = AnalyseResult.cbSignedCardData[0];
			int cbFirstColor = get_card_color(CardData);
			for (int i = 1; i < AnalyseResult.cbSignedCount; i++) {
				int CardDatatemp = AnalyseResult.cbSignedCardData[i];
				int cbNextColor = get_card_color(CardDatatemp);
				if (cbFirstColor != cbNextColor)
					return GameConstants.PDK_CT_SINGLE_LINE;
			}
			if (has_rule(GameConstants.GAME_RULE_LIANGFU_COUNT)) {
				return GameConstants.PDK_CT_HONG_HUA_SHUN;
			} else {
				return GameConstants.PDK_CT_SINGLE_LINE;
			}
		}

		return GameConstants.PDK_CT_ERROR;
	}
	
	// 陕西三代获取类型
	public int GetCardType_SXSD(int cbCardData[], int cbCardCount, int cbRealData[]) {
		int cbTempData[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			cbTempData[i] = cbCardData[i];
		}
		this.sort_card_date_list_SXSD(cbTempData, cbCardCount);
		int nlaizicount = this.GetLaiZiCount(cbRealData, cbCardCount);
		// 简单牌型
		switch (cbCardCount) {
		case 0: // 空牌
		{
			return GameConstants.PDK_CT_ERROR;
		}
		case 1: // 单牌
		{
			return GameConstants.PDK_CT_SINGLE;
		}
		case 2: // 对牌
		{
			// 牌型判断
			if (GetCardLogicValue_SXSD(cbTempData[0]) == GetCardLogicValue_SXSD(cbTempData[1]))
				return GameConstants.PDK_CT_DOUBLE;

			return GameConstants.PDK_CT_ERROR;
		}
		case 4: // 天王炸
		{
			boolean bMissileCard = true;
			for (int cbCardIdx = 0; cbCardIdx < cbCardCount; ++cbCardIdx) {
				if (this.get_card_color(cbTempData[cbCardIdx]) != 0x40) {
					bMissileCard = false;
					break;
				}
			}
			if (bMissileCard)
				return GameConstants.PDK_CT_MISSILE_CARD;
		}
		}

		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData_SXSD(cbTempData, cbCardCount, AnalyseResult);

		if (has_rule(GameConstants.GAME_RULE_BOOM) && has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)) {
			if (cbCardCount > 4 && AnalyseResult.cbFourCount > 0) {
				return GameConstants.PDK_CT_ERROR;
			}
		}
		// 炸弹判断
		if (4 <= cbCardCount && cbCardCount <= 8) {
			// 牌型判断
			if ((AnalyseResult.cbFourCount == 1) && (cbCardCount == 4)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else if (nlaizicount == 4) {
					return GameConstants.PDK_CT_MAGIC_BOOM;
				} else {
					return GameConstants.PDK_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbFiveCount == 1) && (cbCardCount == 5)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
			if ((AnalyseResult.cbSixCount == 1) && (cbCardCount == 6)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
			if ((AnalyseResult.cbSevenCount == 1) && (cbCardCount == 7)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
			if ((AnalyseResult.cbEightCount == 1) && (cbCardCount == 8)) {
				if (nlaizicount == 0) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_SINGLE;
				}
			}
		}
		if (AnalyseResult.cbFourCount == 1) {
			if(cbCardCount >= 5)
				return GameConstants.PDK_CT_ERROR; 
		}

		/////////////////////////////////////////////////////////////////////////////////

		/////////////////////////////////////////////////////////////////////////////////

		// 三牌判断
		if (AnalyseResult.cbThreeCount > 0) {
			// 三条类型
			if (AnalyseResult.cbThreeCount == 1 && cbCardCount == 3)
				return GameConstants.PDK_CT_ERROR;
				//return GameConstants.PDK_CT_THREE;
			if (AnalyseResult.cbThreeCount * 5 == cbCardCount && cbCardCount == 5){
				
				if(this.has_rule(GameConstants.GAME_RULE_SD_NOT_TAKE_3_SXSD)){
					if(AnalyseResult.cbDoubleCount == 1 && GetCardLogicValue_SXSD(AnalyseResult.cbDoubleCardData[0]) == 16){
						return GameConstants.PDK_CT_ERROR;  
					}else if(GetCardLogicValue_SXSD(AnalyseResult.cbSignedCardData[0]) == 16 || 
							GetCardLogicValue_SXSD(AnalyseResult.cbSignedCardData[1]) == 16){
						return GameConstants.PDK_CT_ERROR;
					}
				}
				
				if(AnalyseResult.cbDoubleCount == 1){
					return GameConstants.PDK_CT_THREE_TAKE_DOUBLE;
				}else{
					return GameConstants.PDK_CT_THREE_TAKE_TWO;
				}
			}
				
			if ((AnalyseResult.cbThreeCount * 4 == cbCardCount) && (cbCardCount == 4))
				return GameConstants.PDK_CT_ERROR;
				//return GameConstants.PDK_CT_THREE_TAKE_ONE;

			// 飞机
			if (AnalyseResult.cbThreeCount > 1) {
				
				if(!this.has_rule(GameConstants.GAME_RULE_TAKE_FLY_SXSD))
					return GameConstants.PDK_CT_ERROR;
				
				//飞机必须带4张
				if(cbCardCount != 10 || AnalyseResult.cbThreeCount < 2){
					return GameConstants.PDK_CT_ERROR;
				}
				// 变量定义
				int CardData = AnalyseResult.cbThreeCardData[0];
				int cbFirstLogicValue = GetCardLogicValue_SXSD(CardData);
				int cbSecondLogicValue = GetCardLogicValue_SXSD(AnalyseResult.cbThreeCardData[3]);

				//过滤1，2，3
				if(AnalyseResult.cbThreeCount == 3){
					int cbthirdlyLogicValue = GetCardLogicValue_SXSD(AnalyseResult.cbThreeCardData[6]);
					if(cbFirstLogicValue != cbSecondLogicValue && cbSecondLogicValue == cbthirdlyLogicValue+1){
						if (cbSecondLogicValue >= 14)
							return GameConstants.PDK_CT_ERROR;
					}
				}else{
					if (cbFirstLogicValue >= 14)
						return GameConstants.PDK_CT_ERROR;
				}
				
				

				
				if(this.has_rule(GameConstants.GAME_RULE_SD_NOT_TAKE_3_SXSD)){
					if(AnalyseResult.cbDoubleCount > 0){
						for(int i = 0;i < AnalyseResult.cbDoubleCardData.length;i++){
							if(GetCardLogicValue_SXSD(AnalyseResult.cbDoubleCardData[i]) == 16){
								return GameConstants.PDK_CT_ERROR;
							}
						}
					}
					if(AnalyseResult.cbSignedCount > 0){
						for(int i = 0;i < AnalyseResult.cbSignedCount;i++){
							if(GetCardLogicValue_SXSD(AnalyseResult.cbSignedCardData[i]) == 16){
								return GameConstants.PDK_CT_ERROR;
							}
						}
					}
					if(AnalyseResult.cbThreeCount == 3){
						for(int i = 0;i < AnalyseResult.cbThreeCardData.length;i++){
							if(GetCardLogicValue_SXSD(AnalyseResult.cbSignedCardData[i]) == 16){
								return GameConstants.PDK_CT_ERROR;
							}
						}
					}
				}
				
				if(AnalyseResult.cbThreeCount == 3){
					int cbthirdlyLogicValue = GetCardLogicValue_SXSD(AnalyseResult.cbThreeCardData[6]);
					if(cbFirstLogicValue != cbSecondLogicValue+1 && cbSecondLogicValue != cbthirdlyLogicValue+1){
						return GameConstants.PDK_CT_ERROR;
					}
				}else{
					if(cbFirstLogicValue != cbSecondLogicValue+1 ){
						return GameConstants.PDK_CT_ERROR;
					}
				}

				return GameConstants.PDK_CT_PLANE;
			}

			return GameConstants.PDK_CT_ERROR;
		}

		// 两张类型
		if (AnalyseResult.cbDoubleCount >= 3) {
			// 变量定义
			int CardData = AnalyseResult.cbDoubleCardData[0]; 
			int cbFirstLogicValue = GetCardLogicValue_SXSD(CardData);

			// 错误过虑
			if (cbFirstLogicValue >= 14)
				return GameConstants.PDK_CT_ERROR;

			// 连牌判断
			for (int i = 1; i < AnalyseResult.cbDoubleCount; i++) {
				int CardDatatemp = AnalyseResult.cbDoubleCardData[i * 2];
				if (cbFirstLogicValue != (GetCardLogicValue_SXSD(CardDatatemp) + i))
					return GameConstants.PDK_CT_ERROR;
			}

			// 二连判断
			if ((AnalyseResult.cbDoubleCount * 2) == cbCardCount)
				return GameConstants.PDK_CT_DOUBLE_LINE;

			return GameConstants.PDK_CT_ERROR;
		}

		int shunzicount = 5;
		if(this.has_rule(GameConstants.GAME_RULE_FOUR_SXSD)){
			shunzicount = 4;
		}
		// 单张判断
		if ((AnalyseResult.cbSignedCount >= shunzicount) && (AnalyseResult.cbSignedCount == cbCardCount)) {
			// 变量定义
			int CardData = AnalyseResult.cbSignedCardData[0];
			int cbFirstLogicValue = GetCardLogicValue_SXSD(CardData);

			// 错误过虑
			if (cbFirstLogicValue >= 14)
				return GameConstants.PDK_CT_ERROR;

			// 连牌判断
			for (int i = 1; i < AnalyseResult.cbSignedCount; i++) {
				int CardDatatemp = AnalyseResult.cbSignedCardData[i];
				if (cbFirstLogicValue != (GetCardLogicValue_SXSD(CardDatatemp) + i))
					return GameConstants.PDK_CT_ERROR;
			}

			CardData = AnalyseResult.cbSignedCardData[0];
			int cbFirstColor = get_card_color(CardData);
			for (int i = 1; i < AnalyseResult.cbSignedCount; i++) {
				int CardDatatemp = AnalyseResult.cbSignedCardData[i];
				int cbNextColor = get_card_color(CardDatatemp);
				if (cbFirstColor != cbNextColor)
					return GameConstants.PDK_CT_SINGLE_LINE;
			}
			if (has_rule(GameConstants.GAME_RULE_LIANGFU_COUNT)) {
				return GameConstants.PDK_CT_HONG_HUA_SHUN;
			} else {
				return GameConstants.PDK_CT_SINGLE_LINE;
			}
		}

		return GameConstants.PDK_CT_ERROR;
	}

	// 分析扑克
	public void AnalysebCardData(int cbCardData[], int cbCardCount, tagAnalyseResult AnalyseResult) {
		// 设置结果
		AnalyseResult.Reset();

		// 扑克分析
		for (int i = 0; i < cbCardCount; i++) {
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
			switch (cbSameCount) {
			case 1: // 单张
			{
				int cbIndex = AnalyseResult.cbSignedCount++;
				AnalyseResult.cbSignedCardData[cbIndex * cbSameCount] = cbCardData[i];
				break;
			}
			case 2: // 两张
			{
				int cbIndex = AnalyseResult.cbDoubleCount++;
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				break;
			}
			case 3: // 三张
			{
				int cbIndex = AnalyseResult.cbThreeCount++;
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				break;
			}
			case 4: // 四张
			{
				if (cbCardCount == 4 || cbCardCount == 6 || cbCardCount == 7
						|| has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)) {
					int cbIndex = AnalyseResult.cbFourCount++;
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
				}
				break;
			}
			case 5: // 五张
			{
				if (cbCardCount == 5) {
					int cbIndex = AnalyseResult.cbFiveCount++;
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
				}

				break;
			}
			case 6: // 六张
			{
				if (cbCardCount == 6) {
					int cbIndex = AnalyseResult.cbSixCount++;
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
				}

				break;
			}
			case 7: // 七张
			{
				if (cbCardCount == 7) {
					int cbIndex = AnalyseResult.cbSevenCount++;
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 6];
				}

				break;
			}
			case 8: // 八张
			{
				if (cbCardCount == 8) {
					int cbIndex = AnalyseResult.cbEightCount++;
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 7] = cbCardData[i + 7];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 6];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 7];
				}

				break;
			}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return;
	}
	
	// 分析扑克
	public void AnalysebCardData_SXSD(int cbCardData[], int cbCardCount, tagAnalyseResult AnalyseResult) {
		// 设置结果
		AnalyseResult.Reset();

		// 扑克分析
		for (int i = 0; i < cbCardCount; i++) {
			// 变量定义
			int cbSameCount = 1, cbCardValueTemp = 0;
			int cbLogicValue = GetCardLogicValue_SXSD(cbCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue_SXSD(cbCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			// 设置结果
			switch (cbSameCount) {
			case 1: // 单张
			{
				int cbIndex = AnalyseResult.cbSignedCount++;
				AnalyseResult.cbSignedCardData[cbIndex * cbSameCount] = cbCardData[i];
				break;
			}
			case 2: // 两张
			{
				int cbIndex = AnalyseResult.cbDoubleCount++;
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				break;
			}
			case 3: // 三张
			{
				int cbIndex = AnalyseResult.cbThreeCount++;
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				break;
			}
			case 4: // 四张
			{
				//if (cbCardCount == 4 || cbCardCount == 6 || cbCardCount == 7) {
					int cbIndex = AnalyseResult.cbFourCount++;
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				//} else {
				//	int cbIndex = AnalyseResult.cbThreeCount++;
				//	AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1)] = cbCardData[i];
				//	AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1) + 1] = cbCardData[i + 1];
				//	AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1) + 2] = cbCardData[i + 2];
				//	int cbSingleIndex = AnalyseResult.cbSignedCount++;
				//	AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
				//}
				break;
			}
			case 5: // 五张
			{
				if (cbCardCount == 5) {
					int cbIndex = AnalyseResult.cbFiveCount++;
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
				}

				break;
			}
			case 6: // 六张
			{
				if (cbCardCount == 6) {
					int cbIndex = AnalyseResult.cbSixCount++;
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
				}

				break;
			}
			case 7: // 七张
			{
				if (cbCardCount == 7) {
					int cbIndex = AnalyseResult.cbSevenCount++;
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 6];
				}

				break;
			}
			case 8: // 八张
			{
				if (cbCardCount == 8) {
					int cbIndex = AnalyseResult.cbEightCount++;
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 7] = cbCardData[i + 7];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 6];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 7];
				}

				break;
			}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return;
	}

	// 分析扑克
	public void AnalysebAllCardData(int cbCardData[], int cbCardCount, tagAnalyseResult AnalyseResult) {
		// 设置结果
		AnalyseResult.Reset();

		// 扑克分析
		for (int i = 0; i < cbCardCount; i++) {
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
			switch (cbSameCount) {
			case 1: // 单张
			{
				int cbIndex = AnalyseResult.cbSignedCount++;
				AnalyseResult.cbSignedCardData[cbIndex * cbSameCount] = cbCardData[i];
				break;
			}
			case 2: // 两张
			{
				int cbIndex = AnalyseResult.cbDoubleCount++;
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				break;
			}
			case 3: // 三张
			{
				int cbIndex = AnalyseResult.cbThreeCount++;
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				break;
			}
			case 4: // 四张
			{
				int cbIndex = AnalyseResult.cbFourCount++;
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				break;
			}
			case 5: // 五张
			{
				if (cbCardCount == 5) {
					int cbIndex = AnalyseResult.cbFiveCount++;
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
				}

				break;
			}
			case 6: // 六张
			{
				if (cbCardCount == 6) {
					int cbIndex = AnalyseResult.cbSixCount++;
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
				}

				break;
			}
			case 7: // 七张
			{
				if (cbCardCount == 7) {
					int cbIndex = AnalyseResult.cbSevenCount++;
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
					AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 6];
				}

				break;
			}
			case 8: // 八张
			{
				if (cbCardCount == 8) {
					int cbIndex = AnalyseResult.cbEightCount++;
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
					AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 7] = cbCardData[i + 7];
				} else {
					int cbIndex = AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) + 2] = cbCardData[i + 2];
					int cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 3];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 4];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 5];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 6];
					cbSingleIndex = AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i + 7];
				}

				break;
			}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return;
	}

	// 对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = GetCardType(cbNextCard, cbNextCount, cbNextCard);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount, cbFirstCard);
		if (cbNextType == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE)
			cbNextType = GameConstants.PDK_CT_THREE_TAKE_TWO;
		if (cbFirstType == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE)
			cbFirstType = GameConstants.PDK_CT_THREE_TAKE_TWO;
		// 类型判断
		if (cbNextType == GameConstants.PDK_CT_ERROR)
			return false;
		if (cbNextType == GameConstants.PDK_CT_MISSILE_CARD)
			return true;
		if (cbFirstType == GameConstants.PDK_CT_MISSILE_CARD)
			return false;

		// 炸弹判断
		if ((cbFirstType != GameConstants.PDK_CT_BOMB_CARD) && (cbNextType == GameConstants.PDK_CT_BOMB_CARD))
			return true;
		if ((cbFirstType == GameConstants.PDK_CT_BOMB_CARD) && (cbNextType != GameConstants.PDK_CT_BOMB_CARD))
			return false;

		// 规则判断
		if ((cbFirstType != cbNextType) && cbNextType == GameConstants.PDK_CT_HONG_HUA_SHUN
				&& cbFirstType == GameConstants.PDK_CT_SINGLE_LINE)
			return true;
		if ((cbNextType == GameConstants.PDK_CT_THREE_TAKE_ONE || cbNextType == GameConstants.PDK_CT_THREE)
				&& cbFirstType == GameConstants.PDK_CT_THREE_TAKE_TWO
				&& this.has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		} else if (cbFirstType == GameConstants.PDK_CT_PLANE && cbNextType == GameConstants.PDK_CT_PLANE_LOST
				&& this.has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
			if (cbNextCount > cbFirstCount) {
				return false;
			}
			// 分析扑克
			tagAnalyseResult firstResult = new tagAnalyseResult();
			tagAnalyseResult nextResult = new tagAnalyseResult();
			AnalysebCardData(cbFirstCard, cbFirstCount, firstResult);
			AnalysebCardData(cbNextCard, cbNextCount, nextResult);
			if (cbFirstCount / 5 <= nextResult.cbThreeCount + nextResult.cbFourCount) {
				// 获取数值
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			} else {
				return false;
			}
		} else if ((cbFirstType != cbNextType)
				|| (cbFirstType != GameConstants.PDK_CT_BOMB_CARD && cbFirstCount != cbNextCount))
			return false;

		// 开始对比
		switch (cbNextType) {
		case GameConstants.PDK_CT_SINGLE:
		case GameConstants.PDK_CT_DOUBLE:
		case GameConstants.PDK_CT_THREE:
		case GameConstants.PDK_CT_SINGLE_LINE:
		case GameConstants.PDK_CT_DOUBLE_LINE:
		case GameConstants.PDK_CT_PLANE: {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:
		case GameConstants.PDK_CT_THREE_TAKE_TWO:
		case GameConstants.PDK_CT_THREE_TAKE_ONE: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCard, cbNextCount, NextResult);
			AnalysebCardData(cbFirstCard, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(NextResult.cbThreeCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue(FirstResult.cbThreeCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCard, cbNextCount, NextResult);
			AnalysebCardData(cbFirstCard, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(NextResult.cbFourCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue(FirstResult.cbFourCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_BOMB_CARD: {
			// 数目判断
			if (cbNextCount != cbFirstCount)
				return cbNextCount > cbFirstCount;

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		}

		return false;
	}
	
	// 对比扑克
	public boolean CompareCard_SXSD(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = GetCardType_SXSD(cbNextCard, cbNextCount, cbNextCard);
		int cbFirstType = GetCardType_SXSD(cbFirstCard, cbFirstCount, cbFirstCard);
		
		// 类型判断
		if (cbNextType == GameConstants.PDK_CT_ERROR)
			return false;
		if (cbNextType == GameConstants.PDK_CT_MISSILE_CARD)
			return true;
		if (cbFirstType == GameConstants.PDK_CT_MISSILE_CARD)
			return false;

		// 炸弹判断
		if ((cbFirstType != GameConstants.PDK_CT_BOMB_CARD) && (cbNextType == GameConstants.PDK_CT_BOMB_CARD))
			return true;
		if ((cbFirstType == GameConstants.PDK_CT_BOMB_CARD) && (cbNextType != GameConstants.PDK_CT_BOMB_CARD))
			return false;

		//特殊玩法
		if(cbNextType == GameConstants.PDK_CT_THREE_TAKE_DOUBLE && cbFirstType == GameConstants.PDK_CT_THREE_TAKE_TWO){
			int cbNextLogicValue = GetCardLogicValue_SXSD(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue_SXSD(cbFirstCard[0]);
			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue; 	
		}else if(cbFirstType == GameConstants.PDK_CT_THREE_TAKE_DOUBLE && cbNextType == GameConstants.PDK_CT_THREE_TAKE_TWO){
			if(this.has_rule(GameConstants.GAME_RULE_SDE_WIN_SDD_SXSD)){
				int cbNextLogicValue = GetCardLogicValue_SXSD(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue_SXSD(cbFirstCard[0]);
				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue; 
			}else{
				return false;
			}
		}else if ((cbFirstType != cbNextType)|| (cbFirstType != GameConstants.PDK_CT_BOMB_CARD && cbFirstCount != cbNextCount)){
			return false;
		}

		// 以下是牌型相同的比较
		switch (cbNextType) {
		case GameConstants.PDK_CT_SINGLE:
		case GameConstants.PDK_CT_DOUBLE:
		case GameConstants.PDK_CT_THREE:
		case GameConstants.PDK_CT_SINGLE_LINE:
		case GameConstants.PDK_CT_DOUBLE_LINE:
		case GameConstants.PDK_CT_PLANE: {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue_SXSD(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue_SXSD(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:
		case GameConstants.PDK_CT_THREE_TAKE_TWO:
		case GameConstants.PDK_CT_THREE_TAKE_ONE: 
		case GameConstants.PDK_CT_THREE_TAKE_DOUBLE: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData_SXSD(cbNextCard, cbNextCount, NextResult);
			AnalysebCardData_SXSD(cbFirstCard, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue_SXSD(NextResult.cbThreeCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue_SXSD(FirstResult.cbThreeCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData_SXSD(cbNextCard, cbNextCount, NextResult);
			AnalysebCardData_SXSD(cbFirstCard, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue_SXSD(NextResult.cbFourCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue_SXSD(FirstResult.cbFourCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_BOMB_CARD: {
			// 数目判断
			if (cbNextCount != cbFirstCount)
				return cbNextCount > cbFirstCount;

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue_SXSD(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue_SXSD(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		}

		return false;
	}

	// 判断是否有压牌
	// 出牌搜索
	public boolean SearchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {

		// 获取出牌类型
		int card_type = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);
		if (card_type == GameConstants.PDK_CT_MISSILE_CARD)
			return false;

		if (is_mj_type(GameConstants.GAME_TYPE_PDK_SW) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL)) {
			if (has_rule(GameConstants.GAME_RULE_BOOM)) {
				// 搜索炸弹
				if (SearchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
					return true;
				}
			}
		} else {
			// 搜索炸弹
			if (SearchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
				return true;
			}
		}

		if (card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE) {
			// 搜索四带三
			if (SearchFourTakeThree(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
				return true;
			}
		}
		// 搜索顺子
		if (card_type == GameConstants.PDK_CT_SINGLE_LINE) {
			return SearchSingleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_DOUBLE_LINE) {
			return SearchDoubleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_PLANE) {
			return SearchThreeLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_SINGLE) {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_DOUBLE) {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_THREE || card_type == GameConstants.PDK_CT_THREE_TAKE_ONE
				|| card_type == GameConstants.PDK_CT_THREE_TAKE_TWO
				|| card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE) {
			return SearchThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}

		// int cbFirstType=GetCardType(cbFirstCard,cbFirstCount);
		return false;
	}

	// 搜索三张
	public boolean SearchThreeCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (cbHandCardCount < cbTurnCardCount && !has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE))
			return false;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCard[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCard[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 3 && GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	// 搜索对子
	public boolean SearchDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCard[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCard[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 2 && GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	// 搜索单张
	public boolean SearchSingleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		for (int i = 0; i < cbHandCardCount; ++i) {
			if (GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
		}

		return false;
	}

	// 搜索飞机
	public boolean SearchThreeLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (cbHandCardCount < cbTurnCardCount && !has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE))
			return false;
		// 连牌判断
		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i)
			if (GetCardLogicValue(cbTmpCard[i]) < 15) {
				cbFirstCard = i;
				break;
			}

		int cbLeftCardCount = cbHandCardCount - cbFirstCard;
		boolean bFindThreeLine = true;
		int cbThreeLineCount = 0;
		int cbThreeLineCard[] = new int[20];
		// 开始判断
		while (cbLeftCardCount >= cbTurnCardCount / 5 * 3 && bFindThreeLine) {
			int cbLastCard = cbTmpCard[cbFirstCard];
			int cbSameCount = 1;
			cbThreeLineCount = 0;
			bFindThreeLine = false;
			for (int i = cbFirstCard + 1; i < cbLeftCardCount + cbFirstCard; ++i) {
				// 搜索同牌
				while (i < cbLeftCardCount + cbFirstCard
						&& get_card_value(cbLastCard) == get_card_value(cbTmpCard[i])) {
					++cbSameCount;
					++i;
				}
				if (i >= cbLeftCardCount + cbFirstCard) {
					break;
				}

				int cbLastThreeCardValue = 0;
				if (cbThreeLineCount > 0)
					cbLastThreeCardValue = GetCardLogicValue(cbThreeLineCard[cbThreeLineCount - 1]);

				// 重新开始
				if ((cbSameCount < 3
						|| (cbThreeLineCount > 0 && (cbLastThreeCardValue - GetCardLogicValue(cbLastCard)) != 1))
						&& i <= cbLeftCardCount + cbFirstCard) {
					if (cbThreeLineCount >= cbTurnCardCount / 5 * 3)
						break;

					if (cbSameCount >= 3)
						i -= 3;
					cbLastCard = cbTmpCard[i];
					cbThreeLineCount = 0;
				}
				// 保存数据
				else if (cbSameCount >= 3) {
					cbThreeLineCard[cbThreeLineCount] = cbTmpCard[i - cbSameCount];
					cbThreeLineCard[cbThreeLineCount + 1] = cbTmpCard[i - cbSameCount + 1];
					cbThreeLineCard[cbThreeLineCount + 2] = cbTmpCard[i - cbSameCount + 2];
					cbThreeLineCount += 3;

					// 结尾判断
					if (i == (cbLeftCardCount + cbFirstCard - 3))
						if ((GetCardLogicValue(cbLastCard) - GetCardLogicValue(cbTmpCard[i])) == 1
								&& (GetCardLogicValue(cbTmpCard[i]) == GetCardLogicValue(cbTmpCard[i + 1]))
								&& (GetCardLogicValue(cbTmpCard[i]) == GetCardLogicValue(cbTmpCard[i + 2]))) {
							cbThreeLineCard[cbThreeLineCount] = cbTmpCard[i];
							cbThreeLineCard[cbThreeLineCount + 1] = cbTmpCard[i + 1];
							cbThreeLineCard[cbThreeLineCount + 2] = cbTmpCard[i + 2];
							cbThreeLineCount += 3;
							break;
						}

				}

				cbLastCard = cbTmpCard[i];
				cbSameCount = 1;
			}

			// 保存数据
			if (cbThreeLineCount >= cbTurnCardCount / 5 * 3) {
				if (GetCardLogicValue(cbThreeLineCard[0]) > GetCardLogicValue(cbTurnCardData[0])) {
					return true;
				}
			}
		}
		return false;
	}

	// 搜索连对
	public boolean SearchDoubleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		// 连牌判断
		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i)
			if (GetCardLogicValue(cbTmpCard[i]) < 15) {
				cbFirstCard = i;
				break;
			}

		int cbLeftCardCount = cbHandCardCount - cbFirstCard;
		boolean bFindDoubleLine = true;
		int cbDoubleLineCount = 0;
		int cbDoubleLineCard[] = new int[24];
		// 开始判断
		while (cbLeftCardCount >= cbTurnCardCount && bFindDoubleLine) {
			int cbLastCard = cbTmpCard[cbFirstCard];
			int cbSameCount = 1;
			cbDoubleLineCount = 0;
			bFindDoubleLine = false;
			for (int i = cbFirstCard + 1; i < cbLeftCardCount + cbFirstCard; ++i) {
				// 搜索同牌
				while (get_card_value(cbLastCard) == get_card_value(cbTmpCard[i])
						&& i < cbLeftCardCount + cbFirstCard) {
					++cbSameCount;
					if (i == cbLeftCardCount + cbFirstCard - 1)
						break;
					++i;
				}

				int cbLastDoubleCardValue = 0;
				if (cbDoubleLineCount > 0)
					cbLastDoubleCardValue = GetCardLogicValue(cbDoubleLineCard[cbDoubleLineCount - 1]);
				// 重新开始
				if ((cbSameCount < 1
						|| (cbDoubleLineCount > 0 && (cbLastDoubleCardValue - GetCardLogicValue(cbLastCard)) != 1))
						&& i <= cbLeftCardCount + cbFirstCard) {
					if (cbDoubleLineCount >= cbTurnCardCount)
						break;

					if (cbSameCount >= 2)
						i -= cbSameCount;

					cbLastCard = cbTmpCard[i];
					cbDoubleLineCount = 0;
				}
				// 保存数据
				else if (cbSameCount >= 2) {
					if (i - cbSameCount >= 0) {
						cbDoubleLineCard[cbDoubleLineCount] = cbTmpCard[i - cbSameCount];
						cbDoubleLineCard[cbDoubleLineCount + 1] = cbTmpCard[i - cbSameCount + 1];
						cbDoubleLineCount += 2;
						// 结尾判断
						if (i == (cbLeftCardCount + cbFirstCard - 2)) {
							if ((GetCardLogicValue(cbLastCard) - GetCardLogicValue(cbTmpCard[i])) == 1
									&& (GetCardLogicValue(cbTmpCard[i]) == GetCardLogicValue(cbTmpCard[i + 1]))) {
								cbDoubleLineCard[cbDoubleLineCount] = cbTmpCard[i];
								cbDoubleLineCard[cbDoubleLineCount + 1] = cbTmpCard[i + 1];
								cbDoubleLineCount += 2;
								break;
							}
						}
					} else {
						break;
					}

				}

				cbLastCard = cbTmpCard[i];
				cbSameCount = 1;
			}

			// 保存数据
			if (cbDoubleLineCount >= cbTurnCardCount) {
				if (GetCardLogicValue(cbDoubleLineCard[0]) > GetCardLogicValue(cbTurnCardData[0])) {
					return true;
				}
			}
		}
		return false;
	}

	// 分析顺子
	public boolean SearchSingleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		this.sort_card_date_list(cbTmpCard, cbHandCardCount);
		int cbLineCardCount = 0;

		// 数据校验
		if (cbHandCardCount < cbTurnCardCount)
			return false;

		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i) {
			int value = GetCardLogicValue(cbTmpCard[i]);
			if (value < 15) {
				cbFirstCard = i;
				break;
			}
		}

		int cbSingleLineCard[] = new int[12];
		int cbSingleLineCount = 0;
		int cbLeftCardCount = cbHandCardCount;
		boolean bFindSingleLine = true;

		// 连牌判断
		while (cbLeftCardCount >= cbTurnCardCount && bFindSingleLine) {
			cbSingleLineCount = 1;
			bFindSingleLine = false;
			int cbLastCard = cbTmpCard[cbFirstCard];
			cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[cbFirstCard];
			for (int i = cbFirstCard + 1; i < cbLeftCardCount; i++) {
				int cbCardData = cbTmpCard[i];

				// 连续判断
				if (1 != (GetCardLogicValue(cbLastCard) - GetCardLogicValue(cbCardData))
						&& get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbTmpCard[i];
					if (cbSingleLineCount < cbTurnCardCount) {
						cbSingleLineCount = 1;
						cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[i];
						continue;
					} else
						break;
				}
				// 同牌判断
				else if (get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbCardData;
					cbSingleLineCard[cbSingleLineCount] = cbCardData;
					++cbSingleLineCount;
				}
			}

			// 保存数据
			if (cbSingleLineCount >= cbTurnCardCount) {
				if (GetCardLogicValue(cbTurnCardData[0]) < GetCardLogicValue(cbSingleLineCard[0])) {
					return true;
				}
			}
		}
		return false;
	}

	// 分析炸弹
	public boolean SearchBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;

		if (cbHandCardCount < 2)
			return false;

		// 双王炸弹
		if (0x4F == cbTmpCardData[0] && 0x4F == cbTmpCardData[1] && 0x4E == cbTmpCardData[2]
				&& 0x4E == cbTmpCardData[3]) {
			return true;
		}
		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}
			if (cbSameCount >= 4 && cbSameCount > cbTurnCardCount) {
				return true;
			} else if (cbSameCount >= 4) {
				int cbBomCardData[] = new int[cbSameCount];
				for (int j = 0; j < cbSameCount; j++) {
					cbBomCardData[j] = cbTmpCardData[i + j];
				}
				if (CompareCard(cbTurnCardData, cbBomCardData, cbTurnCardCount, cbSameCount)) {
					return true;
				}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	// 分析炸弹
	public boolean SearchFourTakeThree(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;

		if (cbHandCardCount < 7)
			return false;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}
			if (cbSameCount >= 4) {
				int cbBomCardData[] = new int[cbSameCount];
				for (int j = 0; j < cbSameCount; j++) {
					cbBomCardData[j] = cbTmpCardData[i + j];
				}
				if (GetCardLogicValue(cbBomCardData[0]) > GetCardLogicValue(cbTurnCardData[0])) {
					return true;
				}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
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

	// 混乱准备
	private static void random_cards_good(int card_data[], int return_cards[], int card_count) {
		// 混乱扑克
		int bRandCount = 0, bPosition = 0, brand_once_count = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			brand_once_count = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (2)) + 1;
			if (bPosition + brand_once_count > card_count - bRandCount) {
				brand_once_count = card_count - bRandCount - bPosition;
			}
			for (int i = 0; i < brand_once_count; i++) {
				return_cards[bRandCount++] = card_data[bPosition + i];
				card_data[bPosition + i] = card_data[card_count - bRandCount];
			}

		} while (bRandCount < card_count);

	}

	/***
	 * //排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public void sort_card_date_list(int card_date[], int card_count) {
		// 转换数值
		int logic_value[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			logic_value[i] = GetCardLogicValue(card_date[i]);
		}

		// 排序操作
		boolean sorted = true;
		int temp_date, last = card_count - 1;
		int nLaiZicount = this.GetLaiZiCount(card_date, card_count);
		int index = 0;

		if (nLaiZicount > 0) {
			for (int i = 0; i < last + 1; i++) {
				if (logic_value[i] == GetCardLogicValue(this._laizi)) {
					temp_date = card_date[i];
					card_date[i] = card_date[index];
					card_date[index] = temp_date;
					temp_date = logic_value[i];
					logic_value[i] = logic_value[index];
					logic_value[index] = temp_date;
					index++;
				}

			}
		}

		do {
			sorted = true;

			for (int i = index; i < last; i++) {
				if ((logic_value[i] < logic_value[i + 1])
						|| ((logic_value[i] == logic_value[i + 1]) && (card_date[i] < card_date[i + 1]))) {
					// 交换位置
					temp_date = card_date[i];
					card_date[i] = card_date[i + 1];
					card_date[i + 1] = temp_date;
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
	
	/***
	 * //陕西三代排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public void sort_card_date_list_SXSD(int card_date[], int card_count) {
		// 转换数值
		int logic_value[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			logic_value[i] = GetCardLogicValue_SXSD(card_date[i]);
		}

		// 排序操作
		boolean sorted = true;
		int temp_date, last = card_count - 1;
		int nLaiZicount = this.GetLaiZiCount(card_date, card_count);
		int index = 0;

		if (nLaiZicount > 0) {
			for (int i = 0; i < last + 1; i++) {
				if (logic_value[i] == GetCardLogicValue_SXSD(this._laizi)) {
					temp_date = card_date[i];
					card_date[i] = card_date[index];
					card_date[index] = temp_date;
					temp_date = logic_value[i];
					logic_value[i] = logic_value[index];
					logic_value[index] = temp_date;
					index++;
				}

			}
		}

		do {
			sorted = true;

			for (int i = index; i < last; i++) {
				if ((logic_value[i] < logic_value[i + 1])
						|| ((logic_value[i] == logic_value[i + 1]) && (card_date[i] < card_date[i + 1]))) {
					// 交换位置
					temp_date = card_date[i];
					card_date[i] = card_date[i + 1];
					card_date[i + 1] = temp_date;
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


	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		this.sort_card_date_list(card_date, card_count);
		tagAnalyseResult Result = new tagAnalyseResult();
		AnalysebCardData(card_date, card_count, Result);

		int index = 0;
		if (type == GameConstants.PDK_CT_SINGLE || type == GameConstants.PDK_CT_SINGLE_LINE
				|| type == GameConstants.PDK_CT_HONG_HUA_SHUN) {
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
		} else if (type == GameConstants.PDK_CT_DOUBLE || type == GameConstants.PDK_CT_DOUBLE_LINE) {
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.PDK_CT_THREE || type == GameConstants.PDK_CT_THREE_TAKE_ONE
				|| type == GameConstants.PDK_CT_THREE_TAKE_TWO || type == GameConstants.PDK_CT_PLANE
				|| type == GameConstants.PDK_CT_PLANE_LOST) {
			// 连牌判断
			int value_add = 0;
			int CardData = Result.cbThreeCardData[0];
			int cbFirstLogicValue = GetCardLogicValue(CardData);
			int nLink_Three_Count = 0;
			int threeindex = 0;
			for (int i = 0; i < Result.cbThreeCount; i++) {
				if (nLink_Three_Count * 5 >= card_count) {
					break;
				}
				int CardDatatemp = Result.cbThreeCardData[i * 3];
				if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + value_add)) {
					if (nLink_Three_Count * 5 == card_count) {

						break;
					}
					cbFirstLogicValue = GetCardLogicValue(Result.cbThreeCardData[nLink_Three_Count * 3]);
					nLink_Three_Count = 1;
					value_add = 1;
					threeindex = i;
					continue;
				}
				value_add++;
				nLink_Three_Count++;

			}
			for (int i = threeindex; i < Result.cbThreeCount; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < threeindex; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE || type == GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO
				|| type == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE) {
			for (int i = 0; i < Result.cbFourCount; i++) {
				for (int j = 0; j < 4; j++) {
					card_date[index++] = Result.cbFourCardData[i * 4 + j];
				}
			}
			for (int i = 0; i < Result.cbThreeCount; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.PDK_CT_BOMB_CARD) {
			for (int i = 0; i < Result.cbEightCount; i++) {
				for (int j = 0; j < 8; j++) {
					card_date[index++] = Result.cbEightCardData[i * 8 + j];
				}
			}
			for (int i = 0; i < Result.cbSevenCount; i++) {
				for (int j = 0; j < 7; j++) {
					card_date[index++] = Result.cbSevenCardData[i * 7 + j];
				}
			}
			for (int i = 0; i < Result.cbSixCount; i++) {
				for (int j = 0; j < 6; j++) {
					card_date[index++] = Result.cbSixCardData[i * 6 + j];
				}
			}
			for (int i = 0; i < Result.cbFiveCount; i++) {
				for (int j = 0; j < 5; j++) {
					card_date[index++] = Result.cbFiveCardData[i * 5 + j];
				}
			}
		}

		return;
	}
	
	public void sort_card_date_list_by_type_SXSD(int card_date[], int card_count, int type) {
		this.sort_card_date_list_SXSD(card_date, card_count);
		tagAnalyseResult Result = new tagAnalyseResult();
		AnalysebCardData_SXSD(card_date, card_count, Result);

		int index = 0;
		if (type == GameConstants.PDK_CT_SINGLE || type == GameConstants.PDK_CT_SINGLE_LINE
				|| type == GameConstants.PDK_CT_HONG_HUA_SHUN) {
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
		} else if (type == GameConstants.PDK_CT_DOUBLE || type == GameConstants.PDK_CT_DOUBLE_LINE) {
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.PDK_CT_THREE || type == GameConstants.PDK_CT_THREE_TAKE_ONE
				|| type == GameConstants.PDK_CT_THREE_TAKE_TWO || type == GameConstants.PDK_CT_PLANE
				|| type == GameConstants.PDK_CT_PLANE_LOST || type == GameConstants.PDK_CT_THREE_TAKE_DOUBLE) {
			// 连牌判断
			int value_add = 0;
			int CardData = Result.cbThreeCardData[0];
			int cbFirstLogicValue = GetCardLogicValue_SXSD(CardData);
			int nLink_Three_Count = 0;
			int threeindex = 0;
			for (int i = 0; i < Result.cbThreeCount; i++) {
				if (nLink_Three_Count * 5 >= card_count) {
					break;
				}
				int CardDatatemp = Result.cbThreeCardData[i * 3];
				if (cbFirstLogicValue != (GetCardLogicValue_SXSD(CardDatatemp) + value_add)) {
					if (nLink_Three_Count * 5 == card_count) {

						break;
					}
					cbFirstLogicValue = GetCardLogicValue_SXSD(Result.cbThreeCardData[nLink_Three_Count * 3]);
					nLink_Three_Count = 1;
					value_add = 1;
					threeindex = i;
					continue;
				}
				value_add++;
				nLink_Three_Count++;

			}
			for (int i = threeindex; i < Result.cbThreeCount; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < threeindex; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE || type == GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO
				|| type == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE) {
			for (int i = 0; i < Result.cbFourCount; i++) {
				for (int j = 0; j < 4; j++) {
					card_date[index++] = Result.cbFourCardData[i * 4 + j];
				}
			}
			for (int i = 0; i < Result.cbThreeCount; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.PDK_CT_BOMB_CARD) {
			for (int i = 0; i < Result.cbEightCount; i++) {
				for (int j = 0; j < 8; j++) {
					card_date[index++] = Result.cbEightCardData[i * 8 + j];
				}
			}
			for (int i = 0; i < Result.cbSevenCount; i++) {
				for (int j = 0; j < 7; j++) {
					card_date[index++] = Result.cbSevenCardData[i * 7 + j];
				}
			}
			for (int i = 0; i < Result.cbSixCount; i++) {
				for (int j = 0; j < 6; j++) {
					card_date[index++] = Result.cbSixCardData[i * 6 + j];
				}
			}
			for (int i = 0; i < Result.cbFiveCount; i++) {
				for (int j = 0; j < 5; j++) {
					card_date[index++] = Result.cbFiveCardData[i * 5 + j];
				}
			}
		}

		return;
	}


	public int GetCardLogicValue(int CardData) {
		if (CardData == 0) {
			return -1;
		}
		// 扑克属性
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		// 转换数值
		if (cbCardColor == 0x04)
			return cbCardValue + 2;
		return (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
	}
	
	public int GetCardLogicValue_SXSD(int CardData) {
		if (CardData == 0) {
			return -1;
		}
		// 扑克属性
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		// 转换数值
		if (cbCardColor == 0x04)
			return cbCardValue + 2;
		return (cbCardValue <= 3) ? (cbCardValue + 13) : cbCardValue;
	}

	// 获取数值
	public int get_card_value(int card) {
		if (card == 0) {
			return -1;
		}
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		if (card == 0) {
			return -1;
		}
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[card_count];

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

	// 放走包赔
	public boolean fang_zou_bao_pei(int cbCardData[], int cbCardCount, int cbOutCardData[]) {

		if (GetCardLogicValue(cbCardData[0]) == GetCardLogicValue(cbOutCardData[0])) {
			return false;
		}

		if (GetAllBomCard(cbCardData, cbCardCount) > 0) {
			return true;
		}
		if (GetAllLineCard(cbCardData, cbCardCount) > 0) {
			return true;
		}
		if (GetAllThreeCard(cbCardData, cbCardCount) > 0) {
			return true;
		}
		if (GetAllDoubleCard(cbCardData, cbCardCount) > 0) {
			return true;
		}

		// 分析扑克
		if (GetCardLogicValue(cbCardData[0]) == GetCardLogicValue(cbOutCardData[0])) {
			return false;
		} else {
			return true;
		}

	}
	

	// 放走包赔
	public boolean fang_zou_bao_pei_SXSD(int cbCardData[], int cbCardCount, int cbOutCardData[]) {

		if (GetCardLogicValue_SXSD(cbCardData[0]) == GetCardLogicValue_SXSD(cbOutCardData[0])) {
			return false;
		}

		if (GetAllBomCard_SXSD(cbCardData, cbCardCount) > 0) {
			return true;
		}
		if (GetAllLineCard_SXSD(cbCardData, cbCardCount) > 0) {
			return true;
		}
		if (GetAllThreeCard_SXSD(cbCardData, cbCardCount) > 0) {
			return true;
		}
		if (GetAllDoubleCard_SXSD(cbCardData, cbCardCount) > 0) {
			return true;
		}

		// 分析扑克
		if (GetCardLogicValue_SXSD(cbCardData[0]) == GetCardLogicValue_SXSD(cbOutCardData[0])) {
			return false;
		} else {
			return true;
		}

	}

	// 获取炸弹
	public int GetAllBomCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;

		if (cbHandCardCount < 2)
			return 0;

		// 双王炸弹
		if (0x4F == cbTmpCardData[0] && 0x4E == cbTmpCardData[1]) {
			cbBomCardCount += 2;
		}

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (4 == cbSameCount) {
				cbBomCardCount += 4;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbBomCardCount;
	}
	
	// 获取炸弹
	public int GetAllBomCard_SXSD(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;

		if (cbHandCardCount < 2)
			return 0;

		// 双王炸弹
		if (0x4F == cbTmpCardData[0] && 0x4E == cbTmpCardData[1]) {
			cbBomCardCount += 2;
		}

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue_SXSD(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue_SXSD(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (4 == cbSameCount) {
				cbBomCardCount += 4;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbBomCardCount;
	}

	// 获取顺子
	public int GetAllLineCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		int cbLineCardCount = 0;

		// 数据校验
		if (cbHandCardCount < 5)
			return 0;

		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i)
			if (GetCardLogicValue(cbTmpCard[i]) < 15) {
				cbFirstCard = i;
				break;
			}

		int cbSingleLineCard[] = new int[12];
		int cbSingleLineCount = 0;
		int cbLeftCardCount = cbHandCardCount;
		boolean bFindSingleLine = true;

		// 连牌判断
		while (cbLeftCardCount >= 5 && bFindSingleLine) {
			cbSingleLineCount = 1;
			bFindSingleLine = false;
			int cbLastCard = cbTmpCard[cbFirstCard];
			cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[cbFirstCard];
			for (int i = cbFirstCard + 1; i < cbLeftCardCount; i++) {
				int cbCardData = cbTmpCard[i];

				// 连续判断
				if (1 != (GetCardLogicValue(cbLastCard) - GetCardLogicValue(cbCardData))
						&& get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbTmpCard[i];
					if (cbSingleLineCount < 5) {
						cbSingleLineCount = 1;
						cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[i];
						continue;
					} else
						break;
				}
				// 同牌判断
				else if (get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbCardData;
					cbSingleLineCard[cbSingleLineCount] = cbCardData;
					++cbSingleLineCount;
				}
			}

			// 保存数据
			if (cbSingleLineCount >= 5) {
				this.remove_cards_by_data(cbTmpCard, cbLeftCardCount, cbSingleLineCard, cbSingleLineCount);
				cbLineCardCount += cbSingleLineCount;
				cbLeftCardCount -= cbSingleLineCount;
				bFindSingleLine = true;
			}
		}
		return cbLineCardCount;
	}
	
	// 获取顺子
	public int GetAllLineCard_SXSD(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		int cbLineCardCount = 0;

		// 数据校验
		if (cbHandCardCount < 5)
			return 0;

		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i)
			if (GetCardLogicValue_SXSD(cbTmpCard[i]) < 15) {
				cbFirstCard = i;
				break;
			}

		int cbSingleLineCard[] = new int[12];
		int cbSingleLineCount = 0;
		int cbLeftCardCount = cbHandCardCount;
		boolean bFindSingleLine = true;

		// 连牌判断
		while (cbLeftCardCount >= 5 && bFindSingleLine) {
			cbSingleLineCount = 1;
			bFindSingleLine = false;
			int cbLastCard = cbTmpCard[cbFirstCard];
			cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[cbFirstCard];
			for (int i = cbFirstCard + 1; i < cbLeftCardCount; i++) {
				int cbCardData = cbTmpCard[i];

				// 连续判断
				if (1 != (GetCardLogicValue_SXSD(cbLastCard) - GetCardLogicValue_SXSD(cbCardData))
						&& get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbTmpCard[i];
					if (cbSingleLineCount < 5) {
						cbSingleLineCount = 1;
						cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[i];
						continue;
					} else
						break;
				}
				// 同牌判断
				else if (get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbCardData;
					cbSingleLineCard[cbSingleLineCount] = cbCardData;
					++cbSingleLineCount;
				}
			}

			// 保存数据
			if (cbSingleLineCount >= 5) {
				this.remove_cards_by_data(cbTmpCard, cbLeftCardCount, cbSingleLineCard, cbSingleLineCount);
				cbLineCardCount += cbSingleLineCount;
				cbLeftCardCount -= cbSingleLineCount;
				bFindSingleLine = true;
			}
		}
		return cbLineCardCount;
	}


	// 获取三条
	public int GetAllThreeCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbThreeCardCount = 0;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 3) {
				cbThreeCardCount += 3;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbThreeCardCount;
	}
	
	// 获取三条
	public int GetAllThreeCard_SXSD(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbThreeCardCount = 0;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue_SXSD(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue_SXSD(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 3) {
				cbThreeCardCount += 3;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbThreeCardCount;
	}


	// 分析对子
	public int GetAllDoubleCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbDoubleCardCount = 0;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 2) {
				cbDoubleCardCount += 2;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbDoubleCardCount;
	}
	
	// 分析对子
	public int GetAllDoubleCard_SXSD(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbDoubleCardCount = 0;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue_SXSD(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue_SXSD(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 2) {
				cbDoubleCardCount += 2;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbDoubleCardCount;
	}


	// 机器人算法
	public int Ai_Out_Card(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[], PDKTable table) {
		int card_count = 0;
		int card_type = GetCardType(cbOutCardData, out_card_count, cbOutCardData);
		// 分析扑克
		tagAnalyseResult CardDataResult = new tagAnalyseResult();
		AnalysebAllCardData(cbHandCardData, cbHandCardCount, CardDataResult);

		// 第一局首出
		if (!table.has_rule(GameConstants.GAME_RULE_TWO_PLAY)
				&& table.has_rule(GameConstants.GAME_RULE_SHOU_JU_HEITAO_SAN) && table.GRR._cur_round == 0
				&& table._out_card_times[table._current_player] == 0
				&& table._current_player == table.GRR._banker_player) {
			int count = 0;
			for (int i = cbHandCardCount - 1; i >= 0; i--) {
				if (GetCardLogicValue(cbHandCardData[i]) == 3) {
					count++;
				}

			}
			if (count == 1) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				return card_count;
			} else if (count == 2) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 2];
				return card_count;
			} else if (count == 3) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 2];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 3];
				for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
					card_data[card_count++] = CardDataResult.cbSignedCardData[j];
					if (card_count == 5) {
						return card_count;
					}
				}
				for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
					card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
					if (card_count == 5) {
						return card_count;
					}
					card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
					if (card_count == 5) {
						return card_count;
					}
				}
				for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
					card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
					if (card_count == 5) {
						return card_count;
					}
					card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
					if (card_count == 5) {
						return card_count;
					}
				}
				return card_count;
			} else if (count == 4) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 2];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 3];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 4];
				return card_count;
			}
		} else if (table.has_rule(GameConstants.GAME_RULE_TWO_PLAY) && table.GRR._cur_round == 0
				&& table._out_card_times[table._current_player] == 0
				&& table._current_player == table.GRR._banker_player) {
			int count = 0;
			int value = GetCardLogicValue(cbHandCardData[cbHandCardCount - 1]);
			for (int i = cbHandCardCount - 1; i >= 0; i--) {
				if (GetCardLogicValue(cbHandCardData[i]) == value) {
					count++;
				}

			}
			if (count == 1) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				return card_count;
			} else if (count == 2) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 2];
				return card_count;
			} else if (count == 3) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 2];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 3];
				for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
					card_data[card_count++] = CardDataResult.cbSignedCardData[j];
					if (card_count == 5) {
						return card_count;
					}
				}
				for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
					card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
					if (card_count == 5) {
						return card_count;
					}
					card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
					if (card_count == 5) {
						return card_count;
					}
				}
				for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
					card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
					if (card_count == 5) {
						return card_count;
					}
					card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
					if (card_count == 5) {
						return card_count;
					}
				}
				return card_count;
			} else if (count == 4) {
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 1];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 2];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 3];
				card_data[card_count++] = cbHandCardData[cbHandCardCount - 4];
				return card_count;
			}
		}

		if (out_card_count != 0) {
			// 单牌
			if (card_type == GameConstants.PDK_CT_SINGLE) {
				card_count = 0;
				int nextplayer = (table._current_player + 1) % table.getTablePlayerNumber();
				if (table.GRR._card_count[nextplayer] != 1) {
					for (int i = CardDataResult.cbSignedCount - 1; i >= 0; i--) {
						if (GetCardLogicValue(CardDataResult.cbSignedCardData[i]) > GetCardLogicValue(
								cbOutCardData[0])) {
							card_data[card_count++] = CardDataResult.cbSignedCardData[i];
							return card_count;
						}
					}
					for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 2) {
						if (GetCardLogicValue(CardDataResult.cbDoubleCardData[i]) > GetCardLogicValue(
								cbOutCardData[0])) {
							card_data[card_count++] = CardDataResult.cbDoubleCardData[i];
							return card_count;
						}
					}
					for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i = i - 3) {
						if (GetCardLogicValue(CardDataResult.cbThreeCardData[i]) > GetCardLogicValue(
								cbOutCardData[0])) {
							card_data[card_count++] = CardDataResult.cbThreeCardData[i];
							return card_count;
						}
					}
				} else {
					card_data[card_count++] = cbHandCardData[0];
					return card_count;
				}

			}
			// 对子
			if (card_type == GameConstants.PDK_CT_DOUBLE) {
				card_count = 0;
				for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 1) {
					if (GetCardLogicValue(CardDataResult.cbDoubleCardData[i * 2]) > GetCardLogicValue(
							cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2];
						card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2 + 1];
						return card_count;
					}
				}
				for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i = i - 1) {
					if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3]) > GetCardLogicValue(
							cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
						return card_count;
					}
				}
			}
			if (card_type == GameConstants.PDK_CT_DOUBLE_LINE) {
				card_count = 0;
				for (int i = cbHandCardCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(cbHandCardData[i]) == 15) {
						continue;
					}
					if (i >= out_card_count - 1
							&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(
									cbOutCardData[out_card_count - 1])
							&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])) {
						int count = 1;
						int index = i - 2;
						for (; index > 0; index--) {
							if (GetCardLogicValue(cbHandCardData[index]) == 15) {
								continue;
							}
							if (GetCardLogicValue(cbHandCardData[i]) + count == GetCardLogicValue(cbHandCardData[index])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 1])) {
								count++;
								index--;
							}
						}
						if (count >= out_card_count / 2) {
							card_data[card_count++] = cbHandCardData[i];
							card_data[card_count++] = cbHandCardData[i - 1];
							for (int j = i; j >= index; j--) {
								if (GetCardLogicValue(cbHandCardData[j - 2])
										- 1 == GetCardLogicValue(cbHandCardData[j])) {
									card_data[card_count++] = cbHandCardData[j - 2];
									card_data[card_count++] = cbHandCardData[j - 3];
									count--;
									j--;
									if (count == 1) {
										return out_card_count;
									}
								}
							}

						}
						i--;
					}
				}
			}
			// 三带二
			if (card_type == GameConstants.PDK_CT_THREE_TAKE_TWO
					|| card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE) {
				card_count = 0;
				for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3]) > GetCardLogicValue(
							cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 2];
						for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbSignedCardData[j];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
							if (i == j) {
								continue;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
					}
				}
			}

			if (card_type == GameConstants.PDK_CT_PLANE) {
				card_count = 0;
				for (int i = CardDataResult.cbThreeCount - 1; i > 0; i--) {
					if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3]) > GetCardLogicValue(
							cbOutCardData[0])) {
						int count = 1;
						for (int j = i - 1; j >= count - 1; j--) {
							if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3])
									+ count == GetCardLogicValue(CardDataResult.cbThreeCardData[j * 3])) {
								count++;
								if (count == out_card_count / 5) {
									for (int x = i; x >= j; x--) {
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3];
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 1];
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 2];
									}

									for (int x = CardDataResult.cbSignedCount - 1; x >= 0; x--) {
										card_data[card_count++] = CardDataResult.cbSignedCardData[x];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
									for (int x = CardDataResult.cbDoubleCount - 1; x >= 0; x--) {
										card_data[card_count++] = CardDataResult.cbDoubleCardData[x * 2];
										if (card_count == out_card_count) {
											return card_count;
										}
										card_data[card_count++] = CardDataResult.cbDoubleCardData[x * 2 + 1];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
									for (int x = CardDataResult.cbThreeCount - 1; x >= 0; x--) {
										if (x >= i && x <= j) {
											continue;
										}
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3];
										if (card_count == out_card_count) {
											return card_count;
										}
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 1];
										if (card_count == out_card_count) {
											return card_count;
										}
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 2];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
								}
							}
						}
					}

				}
			}
			// 顺子
			if (card_type == GameConstants.PDK_CT_SINGLE_LINE) {
				card_count = 0;
				for (int i = 0; i < cbHandCardCount; i++) {
					if (i <= cbHandCardCount - out_card_count
							&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
						if (GetCardLogicValue(cbHandCardData[i]) == 15) {
							continue;
						}
						int count = 1;
						int index = i + 1;
						for (; index < cbHandCardCount; index++) {
							if (GetCardLogicValue(cbHandCardData[index]) == 15) {
								continue;
							}
							if (GetCardLogicValue(cbHandCardData[i])
									- count == GetCardLogicValue(cbHandCardData[index])) {
								count++;
							}
						}
						if (count >= out_card_count) {
							card_data[card_count++] = cbHandCardData[i];
							for (int j = i + 1; j < index; j++) {
								if (GetCardLogicValue(cbHandCardData[j - 1])
										- 1 == GetCardLogicValue(cbHandCardData[j])) {
									card_data[card_count++] = cbHandCardData[j];
									count--;
									if (count == 1) {
										return out_card_count;
									}
								}
							}

						}
					}
				}
			}

			card_count = 0;
			for (int i = CardDataResult.cbFourCount - 1; i >= 0; i = i - 1) {
				if (card_type == GameConstants.PDK_CT_BOMB_CARD) {
					if (GetCardLogicValue(CardDataResult.cbFourCardData[i * 4]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4];
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 1];
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 2];
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 3];
						return card_count;
					}
				} else {
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 1];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 2];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 3];
					return card_count;
				}

			}
		} else {
			int nextplayer = (table._current_player + 1) % table.getTablePlayerNumber();
			if (table.GRR._card_count[nextplayer] != 1) {
				if (GetCardLogicValue(cbHandCardData[0]) == 2
						|| (GetCardLogicValue(cbHandCardData[0]) == 1 && GetCardLogicValue(cbHandCardData[1]) == 1)) {
					if (CardDataResult.cbSignedCount > 2) {
						card_data[card_count++] = CardDataResult.cbSignedCardData[CardDataResult.cbSignedCount - 1];
						return card_count;
					}
				}

				// 三带二
				if (CardDataResult.cbThreeCount > 0) {
					for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i--) {
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 2];

						for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbSignedCardData[j];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
							if (i == j) {
								continue;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
					}
				}
				// 连对
				for (int i = cbHandCardCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(cbHandCardData[i]) == 15) {
						continue;
					}
					if (i >= 4 - 1
							&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])) {
						int count = 1;
						int index = i - 2;
						for (; index > 0; index--) {
							if (GetCardLogicValue(cbHandCardData[index]) == 15) {
								continue;
							}
							if (GetCardLogicValue(cbHandCardData[i]) + count == GetCardLogicValue(cbHandCardData[index])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 1])) {
								count++;
								index--;
							}
						}
						if (count >= 2) {
							card_data[card_count++] = cbHandCardData[i];
							card_data[card_count++] = cbHandCardData[i - 1];
							for (int j = i; j >= index; j--) {
								if (GetCardLogicValue(cbHandCardData[j - 2])
										- 1 == GetCardLogicValue(cbHandCardData[j])) {
									card_data[card_count++] = cbHandCardData[j - 2];
									card_data[card_count++] = cbHandCardData[j - 3];
									count--;
									j--;
									if (count == 1) {
										return card_count;
									}
								}
							}

						}
						i--;
					}
				}

				// 对子
				for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 1) {
					card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2];
					card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2 + 1];
					return card_count;
				}
				// 单张
				for (int i = CardDataResult.cbSignedCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSignedCardData[i];
					return card_count;
				}

				for (int i = CardDataResult.cbFourCount - 1; i >= 0; i = i - 1) {
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 1];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 2];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 3];
					return card_count;
				}
			} else {
				// 三带二
				if (CardDataResult.cbThreeCount > 0) {
					for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i--) {
						if (this.GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3]) >= 12) {
							continue;
						}

						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 2];

						for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbSignedCardData[j];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
							if (i == j) {
								continue;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
					}
				}
				// 连对
				for (int i = cbHandCardCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(cbHandCardData[i]) == 15) {
						continue;
					}
					if (i >= 4 - 1
							&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])) {
						int count = 1;
						int index = i - 2;
						for (; index > 0; index--) {
							if (GetCardLogicValue(cbHandCardData[index]) == 15) {
								continue;
							}
							if (GetCardLogicValue(cbHandCardData[i]) + count == GetCardLogicValue(cbHandCardData[index])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 1])) {
								count++;
								index--;
							}
						}
						if (count >= 2) {
							card_data[card_count++] = cbHandCardData[i];
							card_data[card_count++] = cbHandCardData[i - 1];
							for (int j = i; j >= index; j--) {
								if (GetCardLogicValue(cbHandCardData[j - 2])
										- 1 == GetCardLogicValue(cbHandCardData[j])) {
									card_data[card_count++] = cbHandCardData[j - 2];
									card_data[card_count++] = cbHandCardData[j - 3];
									count--;
									j--;
									if (count == 1) {
										return card_count;
									}
								}
							}

						}
						i--;
					}
				}
				// 对子
				for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 1) {
					if (this.GetCardLogicValue(CardDataResult.cbDoubleCardData[i * 2]) < 11) {
						card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2];
						card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2 + 1];
						return card_count;
					}

				}
				// 单张
				for (int i = 0; i < CardDataResult.cbSignedCount; i++) {
					card_data[card_count++] = cbHandCardData[0];
					return card_count;
				}
				// 对子
				for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 1) {
					card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2];
					card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2 + 1];
					return card_count;
				}

				// 三带二
				if (CardDataResult.cbThreeCount > 0) {
					for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i--) {
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 2];

						for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbSignedCardData[j];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
							if (i == j) {
								continue;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
					}
				}

				for (int i = CardDataResult.cbFourCount - 1; i >= 0; i = i - 1) {
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 1];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 2];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 3];
					return card_count;
				}
			}

		}

		return card_count;
	}

	// 获取能出牌数据
	public int Player_Can_out_card_SXSD(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[]) {
		int cbHandCardData[] = new int[GameConstants.PDK_MAX_COUNT];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbHandCardData[i] = hand_card_data[i];
		}
		int card_count = 0;
		int boom_count = 0;
		int card_type = GetCardType_SXSD(cbOutCardData, out_card_count, cbOutCardData);
		
		tagAnalyseResult OutCardResult = new tagAnalyseResult();
		if(card_type == GameConstants.PDK_CT_THREE_TAKE_ONE || 
				card_type == GameConstants.PDK_CT_THREE_TAKE_TWO || 
				card_type == GameConstants.PDK_CT_THREE_TAKE_DOUBLE){

			AnalysebCardData_SXSD(cbOutCardData, out_card_count, OutCardResult);
		}
		// 分析扑克
		tagAnalyseResult HandDataResult = new tagAnalyseResult();
		AnalysebCardData_SXSD(cbHandCardData, cbHandCardCount, HandDataResult);
		
		if (has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI) && card_type != GameConstants.PDK_CT_BOMB_CARD) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (i < cbHandCardCount - 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])) {
					card_data[card_count++] = cbHandCardData[i];
					card_data[card_count++] = cbHandCardData[i + 1];
					card_data[card_count++] = cbHandCardData[i + 2];
					card_data[card_count++] = cbHandCardData[i + 3];

					cbHandCardData[i] = 0;
					cbHandCardData[i + 1] = 0;
					cbHandCardData[i + 2] = 0;
					cbHandCardData[i + 3] = 0;
					i += 3;
					boom_count++;
				}
			}
		}
		// 单牌
		if (card_type == GameConstants.PDK_CT_SINGLE) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(cbOutCardData[0])) {
					card_data[card_count++] = cbHandCardData[i];
				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						i += 3;
					}
				}
			}
			return card_count;
		}
		// 对子
		if (card_type == GameConstants.PDK_CT_DOUBLE) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}

				if (i < cbHandCardCount - 1
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(cbOutCardData[0])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])) {
					card_data[card_count++] = cbHandCardData[i];
					card_data[card_count++] = cbHandCardData[i + 1];
					i += 1;
				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						i += 3;
					}
				}
			}
			return card_count;
		}
		// 三带二
		if (card_type == GameConstants.PDK_CT_THREE_TAKE_TWO ){
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i < cbHandCardCount - 2
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(OutCardResult.cbThreeCardData[0])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])) {
					if (boom_count == 0) {
						if (cbHandCardCount >= out_card_count) {
							int have_3 = 0;
							int temphandcount = cbHandCardCount;
							if(this.has_rule(GameConstants.GAME_RULE_SD_NOT_TAKE_3_SXSD)){
								for(int k = 0;k < cbHandCardCount;k++){
									if(GetCardLogicValue_SXSD(cbHandCardData[k]) == 16){
										have_3++;
									}
								}
								if(have_3 > 0 && GetCardLogicValue_SXSD(cbHandCardData[i]) != 16){
									temphandcount -= have_3;
								}
							}
							if(temphandcount < out_card_count){
								return 0;
							}
							for (int j = 0; j < cbHandCardCount; j++) {
								if (cbHandCardData[j] != 0) {
									card_data[card_count++] = cbHandCardData[j];
								}
							}
							return card_count;
						} else {
							if (i < cbHandCardCount - 3
									&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
									&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
									&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(
											cbHandCardData[i + 3])) {
								if (!(!has_rule(GameConstants.GAME_RULE_BOOM)
										&& (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
												|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL)))
										&& cbHandCardData[i] != 0) {
									card_data[card_count++] = cbHandCardData[i];
									card_data[card_count++] = cbHandCardData[i + 1];
									card_data[card_count++] = cbHandCardData[i + 2];
									card_data[card_count++] = cbHandCardData[i + 3];
									cbHandCardData[i] = 0;
									cbHandCardData[i + 1] = 0;
									cbHandCardData[i + 2] = 0;
									cbHandCardData[i + 3] = 0;
									i += 3;
								}
								if ( GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(OutCardResult.cbThreeCardData[0])){
									for (int j = 0; j < cbHandCardCount; j++) {
										if (cbHandCardData[j] != 0) {
											card_data[card_count++] = cbHandCardData[j];
										}
									}
									return cbHandCardCount;
								}
							}
						}

					} else {
						if (cbHandCardCount < 5 + 4 * boom_count) {
							return card_count;
						} else {
							if (cbHandCardCount >= out_card_count) {
								for (int j = 0; j < cbHandCardCount; j++) {
									if (cbHandCardData[j] != 0) {
										card_data[card_count++] = cbHandCardData[j];
									}

								}
							} else {
								return 0;
							}

						}
					}

				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						cbHandCardData[i] = 0;
						cbHandCardData[i + 1] = 0;
						cbHandCardData[i + 2] = 0;
						cbHandCardData[i + 3] = 0;
						i += 3;
					}
					if ( GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(OutCardResult.cbThreeCardData[0])){
						for (int j = 0; j < cbHandCardCount; j++) {
							if (cbHandCardData[j] != 0) {
								card_data[card_count++] = cbHandCardData[j];
							}
						}
						return cbHandCardCount;
					}
				}
			}
			return card_count;
		}
		
		// 三带对
		if (card_type == GameConstants.PDK_CT_THREE_TAKE_DOUBLE ){
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i < cbHandCardCount - 2
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(OutCardResult.cbThreeCardData[0])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])) {
					if (boom_count == 0) {
						if (cbHandCardCount >= out_card_count) {
							int have_3 = 0;
							int temphandcount = cbHandCardCount;
							if(this.has_rule(GameConstants.GAME_RULE_SDE_WIN_SDD_SXSD)){
								if(this.has_rule(GameConstants.GAME_RULE_SD_NOT_TAKE_3_SXSD)){
									for(int k = 0;k < cbHandCardCount;k++){
										if(GetCardLogicValue_SXSD(cbHandCardData[k]) == 16){
											have_3++;
										}
									}
									if(have_3 > 0 && GetCardLogicValue_SXSD(cbHandCardData[i]) != 16){
										temphandcount -= have_3;
									}
								}
								if(temphandcount < out_card_count){
									return 0;
								}
							}else{
								if(HandDataResult.cbDoubleCount == 1 && this.has_rule(GameConstants.GAME_RULE_SD_NOT_TAKE_3_SXSD) &&
										GetCardLogicValue_SXSD(HandDataResult.cbDoubleCardData[0]) == 16){
									return 0;
								}
								if(this.has_rule(GameConstants.GAME_RULE_SD_NOT_TAKE_3_SXSD)&& HandDataResult.cbDoubleCount == 0 &&
										HandDataResult.cbThreeCount == 1 && HandDataResult.cbThreeCardData[0] != cbHandCardData[i] && 
										GetCardLogicValue_SXSD(HandDataResult.cbThreeCardData[0]) == 16){
									return 0;
								}
							}
							
							for (int j = 0; j < cbHandCardCount; j++) {
								if (cbHandCardData[j] != 0) {
									card_data[card_count++] = cbHandCardData[j];
								}
							}
							return card_count;
						} else {
							if (i < cbHandCardCount - 3
									&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
									&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
									&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(
											cbHandCardData[i + 3])) {
								if (!(!has_rule(GameConstants.GAME_RULE_BOOM)
										&& (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
												|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL)))
										&& cbHandCardData[i] != 0) {
									card_data[card_count++] = cbHandCardData[i];
									card_data[card_count++] = cbHandCardData[i + 1];
									card_data[card_count++] = cbHandCardData[i + 2];
									card_data[card_count++] = cbHandCardData[i + 3];
									cbHandCardData[i] = 0;
									cbHandCardData[i + 1] = 0;
									cbHandCardData[i + 2] = 0;
									cbHandCardData[i + 3] = 0;
									i += 3;
								}
								if ( GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(OutCardResult.cbThreeCardData[0])){
									for (int j = 0; j < cbHandCardCount; j++) {
										if (cbHandCardData[j] != 0) {
											card_data[card_count++] = cbHandCardData[j];
										}
									}
									return cbHandCardCount;
								}
							}
						}

					} else {
						if (cbHandCardCount < 5 + 4 * boom_count) {
							return card_count;
						} else {
							if (cbHandCardCount >= out_card_count) {
								for (int j = 0; j < cbHandCardCount; j++) {
									if (cbHandCardData[j] != 0) {
										card_data[card_count++] = cbHandCardData[j];
									}

								}
							} else {
								return 0;
							}

						}
					}

				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						cbHandCardData[i] = 0;
						cbHandCardData[i + 1] = 0;
						cbHandCardData[i + 2] = 0;
						cbHandCardData[i + 3] = 0;
						i += 3;
					}
					if ( GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(OutCardResult.cbThreeCardData[0])){
						for (int j = 0; j < cbHandCardCount; j++) {
							if (cbHandCardData[j] != 0) {
								card_data[card_count++] = cbHandCardData[j];
							}
						}
						return cbHandCardCount;
					}
				}
			}
			return card_count;
		}
		// 单张顺子
		if (card_type == GameConstants.PDK_CT_SINGLE_LINE) {
			for (int i = cbHandCardCount - 1; i >= 0; i--) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i >= out_card_count - 1 && GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(
						cbOutCardData[out_card_count - 1])) {
					int count = 1;
					int index = i - 1;
					for (; index >= 0; index--) {
						if (GetCardLogicValue_SXSD(cbHandCardData[index]) == 14) {
							break;
						}
						if (GetCardLogicValue_SXSD(cbHandCardData[i]) + count == GetCardLogicValue_SXSD(cbHandCardData[index])) {
							count++;
						}
						if (GetCardLogicValue_SXSD(cbHandCardData[index]) < 0) {
							break;
						}
						if (GetCardLogicValue_SXSD(cbHandCardData[index]) - GetCardLogicValue_SXSD(cbHandCardData[i]) > count) {
							break;
						}
					}
					if (count >= out_card_count) {
						card_data[card_count++] = cbHandCardData[i];
						for (int j = i - 1; j > index; j--) {
							card_data[card_count++] = cbHandCardData[j];
						}

						i = index + 1;
					} else {
						if (i >= 4 && GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 1])
								&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 2])
								&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 3])) {
							if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
									|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
								card_data[card_count++] = cbHandCardData[i];
								card_data[card_count++] = cbHandCardData[i - 1];
								card_data[card_count++] = cbHandCardData[i - 2];
								card_data[card_count++] = cbHandCardData[i - 3];
								i -= 3;
							}
						}
					}
				} else if (i >= 3 && GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i - 1];
						card_data[card_count++] = cbHandCardData[i - 2];
						card_data[card_count++] = cbHandCardData[i - 3];
						i -= 3;
					}
				}
			}
			return card_count;
		}
		// 对子顺子
		if (card_type == GameConstants.PDK_CT_DOUBLE_LINE) {
			for (int i = cbHandCardCount - 1; i >= 0; i--) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}

				if (i > 0 && GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(cbOutCardData[out_card_count - 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 1])) {
					int count = 1;
					int index = i - 2;
					if (i > 1 && GetCardLogicValue_SXSD(cbHandCardData[i - 1]) == GetCardLogicValue_SXSD(cbHandCardData[i - 2])) {
						index = i - 3;
						if (i > 2 && GetCardLogicValue_SXSD(cbHandCardData[i - 2]) == GetCardLogicValue_SXSD(
								cbHandCardData[i - 3])) {
							index = i - 4;
						}
					}
					for (; index > 0; index--) {
						if (GetCardLogicValue_SXSD(cbHandCardData[index]) == 16 ||
								GetCardLogicValue_SXSD(cbHandCardData[index]) == 15 || 
								GetCardLogicValue_SXSD(cbHandCardData[index]) == 14) {
							break;
						}
						if (GetCardLogicValue_SXSD(cbHandCardData[i]) + count == GetCardLogicValue_SXSD(cbHandCardData[index])
								&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
										cbHandCardData[index - 1])) {
							if (index > 1
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index - 1])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) != GetCardLogicValue_SXSD(
											cbHandCardData[index - 2])) {
								index--;
							} else if (index > 2
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index - 1])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index - 2])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) != GetCardLogicValue_SXSD(
											cbHandCardData[index - 3])) {
								index -= 2;
							} else if (index > 2
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index - 1])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index - 2])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index - 3])) {
								index -= 3;
							} else {
								index--;
							}
							count++;
						} else {
							break;
						}
					}
					if (count * 2 >= out_card_count) {
						card_data[card_count++] = cbHandCardData[i];
						for (int j = i - 1; j > index; j--) {
							card_data[card_count++] = cbHandCardData[j];
						}
						i = index + 1;
					} else {
						if (i >= 3 && GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 1])
								&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 2])
								&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 3])) {
							if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
									|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
								card_data[card_count++] = cbHandCardData[i];
								card_data[card_count++] = cbHandCardData[i - 1];
								card_data[card_count++] = cbHandCardData[i - 2];
								card_data[card_count++] = cbHandCardData[i - 3];
								cbHandCardData[i] = 0;
								cbHandCardData[i + 1] = 0;
								cbHandCardData[i + 2] = 0;
								cbHandCardData[i + 3] = 0;
								i -= 3;
							}
						} else {
							i--;
						}

					}
				} else if (i >= 3 && GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i - 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i - 1];
						card_data[card_count++] = cbHandCardData[i - 2];
						card_data[card_count++] = cbHandCardData[i - 3];
						cbHandCardData[i] = 0;
						cbHandCardData[i + 1] = 0;
						cbHandCardData[i + 2] = 0;
						cbHandCardData[i + 3] = 0;
						i -= 3;
					}
				}
			}
			return card_count;
		}

		// 飞机
		if (card_type == GameConstants.PDK_CT_PLANE) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i <= cbHandCardCount - out_card_count / 5 * 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(cbOutCardData[0])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])) {
					int count = 1;
					int index = i + 3;
					for (; index < cbHandCardCount; index++) {
						if (GetCardLogicValue_SXSD(cbHandCardData[i]) - count == GetCardLogicValue_SXSD(cbHandCardData[index])
								&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
										cbHandCardData[index + 1])
								&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
										cbHandCardData[index + 2])) {
							if (GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(cbHandCardData[index + 1])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index + 2])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) != GetCardLogicValue_SXSD(
											cbHandCardData[index + 3])) {
								index += 2;
							} else if (GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
									cbHandCardData[index + 1])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index + 2])
									&& GetCardLogicValue_SXSD(cbHandCardData[index]) == GetCardLogicValue_SXSD(
											cbHandCardData[index + 3])) {
								index += 4;
							}
							count++;
						}
					}
					if (count >= out_card_count / 5) {
						for (int j = 0; j < cbHandCardCount; j++) {
							if (cbHandCardData[j] != 0) {
								card_data[card_count++] = cbHandCardData[j];
							}
						}
						return cbHandCardCount;
					} else {
						if (GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
								&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
								&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])) {
							if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
									|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
								card_data[card_count++] = cbHandCardData[i];
								card_data[card_count++] = cbHandCardData[i + 1];
								card_data[card_count++] = cbHandCardData[i + 2];
								card_data[card_count++] = cbHandCardData[i + 3];
								cbHandCardData[i] = 0;
								cbHandCardData[i + 1] = 0;
								cbHandCardData[i + 2] = 0;
								cbHandCardData[i + 3] = 0;
								i += 3;
								continue;
							}
						}
						i += 1;
					}
				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						cbHandCardData[i] = 0;
						cbHandCardData[i + 1] = 0;
						cbHandCardData[i + 2] = 0;
						cbHandCardData[i + 3] = 0;
						i += 3;
					}
				}
			}
			return card_count;
		}
		// 炸弹
		if (card_type == GameConstants.PDK_CT_BOMB_CARD) {

			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i < cbHandCardCount - 3
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 1])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 2])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) == GetCardLogicValue_SXSD(cbHandCardData[i + 3])
						&& GetCardLogicValue_SXSD(cbHandCardData[i]) > GetCardLogicValue_SXSD(cbOutCardData[0])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						i += 3;
					}
				}
			}
			return card_count;
		}
		return card_count;
	}
	
	// 获取能出牌数据
	public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[]) {
		int cbHandCardData[] = new int[GameConstants.PDK_MAX_COUNT];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbHandCardData[i] = hand_card_data[i];
		}
		int card_count = 0;
		int boom_count = 0;
		int card_type = GetCardType(cbOutCardData, out_card_count, cbOutCardData);
		// 分析扑克
		tagAnalyseResult CardDataResult = new tagAnalyseResult();
		AnalysebCardData(cbHandCardData, cbHandCardCount, CardDataResult);
		if (has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI) && card_type != GameConstants.PDK_CT_BOMB_CARD) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])) {
					card_data[card_count++] = cbHandCardData[i];
					card_data[card_count++] = cbHandCardData[i + 1];
					card_data[card_count++] = cbHandCardData[i + 2];
					card_data[card_count++] = cbHandCardData[i + 3];

					cbHandCardData[i] = 0;
					cbHandCardData[i + 1] = 0;
					cbHandCardData[i + 2] = 0;
					cbHandCardData[i + 3] = 0;
					i += 3;
					boom_count++;
				}
			}
		}
		// 单牌
		if (card_type == GameConstants.PDK_CT_SINGLE) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
					card_data[card_count++] = cbHandCardData[i];
				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						i += 3;
					}
				}
			}
			return card_count;
		}
		// 对子
		if (card_type == GameConstants.PDK_CT_DOUBLE) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}

				if (i < cbHandCardCount - 1
						&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])) {
					card_data[card_count++] = cbHandCardData[i];
					card_data[card_count++] = cbHandCardData[i + 1];
					i += 1;
				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						i += 3;
					}
				}
			}
			return card_count;
		}
		// 三带二
		if (card_type == GameConstants.PDK_CT_THREE_TAKE_TWO || card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE) {
			if (has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)) {
				if (cbHandCardCount - boom_count * 4 < out_card_count) {
					return card_count;
				}
			}
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i < cbHandCardCount - 2
						&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])) {
					if (boom_count == 0) {
						if (has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
							for (int j = 0; j < cbHandCardCount; j++) {
								if (cbHandCardData[j] != 0) {
									card_data[card_count++] = cbHandCardData[j];
								}
							}
							return card_count;
						} else if (cbHandCardCount >= out_card_count) {
							for (int j = 0; j < cbHandCardCount; j++) {
								if (cbHandCardData[j] != 0) {
									card_data[card_count++] = cbHandCardData[j];
								}
							}
							return card_count;
						} else {
							if (i < cbHandCardCount - 3
									&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
									&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
									&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(
											cbHandCardData[i + 3])) {
								if (!(!has_rule(GameConstants.GAME_RULE_BOOM)
										&& (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
												|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL)))
										&& cbHandCardData[i] != 0) {
									card_data[card_count++] = cbHandCardData[i];
									card_data[card_count++] = cbHandCardData[i + 1];
									card_data[card_count++] = cbHandCardData[i + 2];
									card_data[card_count++] = cbHandCardData[i + 3];
									cbHandCardData[i] = 0;
									cbHandCardData[i + 1] = 0;
									cbHandCardData[i + 2] = 0;
									cbHandCardData[i + 3] = 0;
									i += 3;
								}
								if (!has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)
										&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
									for (int j = 0; j < cbHandCardCount; j++) {
										if (cbHandCardData[j] != 0) {
											card_data[card_count++] = cbHandCardData[j];
										}
									}
									return cbHandCardCount;
								}
							}
						}

					} else {
						if (cbHandCardCount < 5 + 4 * boom_count) {
							return card_count;
						} else {
							if (has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
								for (int j = 0; j < cbHandCardCount; j++) {
									if (cbHandCardData[j] != 0) {
										card_data[card_count++] = cbHandCardData[j];
									}

								}
							} else if (cbHandCardCount >= out_card_count) {
								for (int j = 0; j < cbHandCardCount; j++) {
									if (cbHandCardData[j] != 0) {
										card_data[card_count++] = cbHandCardData[j];
									}

								}
							} else {
								return 0;
							}

						}
					}

				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						cbHandCardData[i] = 0;
						cbHandCardData[i + 1] = 0;
						cbHandCardData[i + 2] = 0;
						cbHandCardData[i + 3] = 0;
						i += 3;
					}
					if (!has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)
							&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
						for (int j = 0; j < cbHandCardCount; j++) {
							if (cbHandCardData[j] != 0) {
								card_data[card_count++] = cbHandCardData[j];
							}
						}
						return cbHandCardCount;
					}
				}
			}
			return card_count;
		}
		// 单张顺子
		if (card_type == GameConstants.PDK_CT_SINGLE_LINE) {
			for (int i = cbHandCardCount - 1; i >= 0; i--) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i >= out_card_count - 1 && GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(
						cbOutCardData[out_card_count - 1])) {
					int count = 1;
					int index = i - 1;
					for (; index >= 0; index--) {
						if (GetCardLogicValue(cbHandCardData[index]) == 15) {
							break;
						}
						if (GetCardLogicValue(cbHandCardData[i]) + count == GetCardLogicValue(cbHandCardData[index])) {
							count++;
						}
						if (GetCardLogicValue(cbHandCardData[index]) < 0) {
							break;
						}
						if (GetCardLogicValue(cbHandCardData[index]) - GetCardLogicValue(cbHandCardData[i]) > count) {
							break;
						}
					}
					if (count >= out_card_count) {
						card_data[card_count++] = cbHandCardData[i];
						for (int j = i - 1; j > index; j--) {
							card_data[card_count++] = cbHandCardData[j];
						}

						i = index + 1;
					} else {
						if (i >= 4 && GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])
								&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 2])
								&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 3])) {
							if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
									|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
								card_data[card_count++] = cbHandCardData[i];
								card_data[card_count++] = cbHandCardData[i - 1];
								card_data[card_count++] = cbHandCardData[i - 2];
								card_data[card_count++] = cbHandCardData[i - 3];
								i -= 3;
							}
						}
					}
				} else if (i >= 3 && GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i - 1];
						card_data[card_count++] = cbHandCardData[i - 2];
						card_data[card_count++] = cbHandCardData[i - 3];
						i -= 3;
					}
				}
			}
			return card_count;
		}
		// 对子顺子
		if (card_type == GameConstants.PDK_CT_DOUBLE_LINE) {
			for (int i = cbHandCardCount - 1; i >= 0; i--) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}

				if (i > 0 && GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[out_card_count - 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])) {
					int count = 1;
					int index = i - 2;
					if (i > 1 && GetCardLogicValue(cbHandCardData[i - 1]) == GetCardLogicValue(cbHandCardData[i - 2])) {
						index = i - 3;
						if (i > 2 && GetCardLogicValue(cbHandCardData[i - 2]) == GetCardLogicValue(
								cbHandCardData[i - 3])) {
							index = i - 4;
						}
					}
					for (; index > 0; index--) {
						if (GetCardLogicValue(cbHandCardData[index]) == 15) {
							break;
						}
						if (GetCardLogicValue(cbHandCardData[i]) + count == GetCardLogicValue(cbHandCardData[index])
								&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
										cbHandCardData[index - 1])) {
							if (index > 1
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 1])
									&& GetCardLogicValue(cbHandCardData[index]) != GetCardLogicValue(
											cbHandCardData[index - 2])) {
								index--;
							} else if (index > 2
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 1])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 2])
									&& GetCardLogicValue(cbHandCardData[index]) != GetCardLogicValue(
											cbHandCardData[index - 3])) {
								index -= 2;
							} else if (index > 2
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 1])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 2])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index - 3])) {
								index -= 3;
							} else {
								index--;
							}
							count++;
						} else {
							break;
						}
					}
					if (count * 2 >= out_card_count) {
						card_data[card_count++] = cbHandCardData[i];
						for (int j = i - 1; j > index; j--) {
							card_data[card_count++] = cbHandCardData[j];
						}
						i = index + 1;
					} else {
						if (i >= 3 && GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])
								&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 2])
								&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 3])) {
							if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
									|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
								card_data[card_count++] = cbHandCardData[i];
								card_data[card_count++] = cbHandCardData[i - 1];
								card_data[card_count++] = cbHandCardData[i - 2];
								card_data[card_count++] = cbHandCardData[i - 3];
								cbHandCardData[i] = 0;
								cbHandCardData[i + 1] = 0;
								cbHandCardData[i + 2] = 0;
								cbHandCardData[i + 3] = 0;
								i -= 3;
							}
						} else {
							i--;
						}

					}
				} else if (i >= 3 && GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i - 1];
						card_data[card_count++] = cbHandCardData[i - 2];
						card_data[card_count++] = cbHandCardData[i - 3];
						cbHandCardData[i] = 0;
						cbHandCardData[i + 1] = 0;
						cbHandCardData[i + 2] = 0;
						cbHandCardData[i + 3] = 0;
						i -= 3;
					}
				}
			}
			return card_count;
		}

		// 飞机
		if (card_type == GameConstants.PDK_CT_PLANE) {
			if (has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)) {
				if (cbHandCardCount - boom_count * 4 < out_card_count) {
					return card_count;
				}
			}
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i <= cbHandCardCount - out_card_count / 5 * 3
						&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])) {
					int count = 1;
					int index = i + 3;
					for (; index < cbHandCardCount; index++) {
						if (GetCardLogicValue(cbHandCardData[i]) - count == GetCardLogicValue(cbHandCardData[index])
								&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
										cbHandCardData[index + 1])
								&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
										cbHandCardData[index + 2])) {
							if (GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(cbHandCardData[index + 1])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index + 2])
									&& GetCardLogicValue(cbHandCardData[index]) != GetCardLogicValue(
											cbHandCardData[index + 3])) {
								index += 2;
							} else if (GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
									cbHandCardData[index + 1])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index + 2])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(
											cbHandCardData[index + 3])) {
								index += 4;
							}
							count++;
						}
					}
					if (count >= out_card_count / 5) {
						for (int j = 0; j < cbHandCardCount; j++) {
							if (cbHandCardData[j] != 0) {
								card_data[card_count++] = cbHandCardData[j];
							}
						}
						return cbHandCardCount;
					} else {
						if (GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
								&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
								&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])) {
							if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
									|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
								card_data[card_count++] = cbHandCardData[i];
								card_data[card_count++] = cbHandCardData[i + 1];
								card_data[card_count++] = cbHandCardData[i + 2];
								card_data[card_count++] = cbHandCardData[i + 3];
								cbHandCardData[i] = 0;
								cbHandCardData[i + 1] = 0;
								cbHandCardData[i + 2] = 0;
								cbHandCardData[i + 3] = 0;
								i += 3;
								continue;
							}
						}
						i += 1;
					}
				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						cbHandCardData[i] = 0;
						cbHandCardData[i + 1] = 0;
						cbHandCardData[i + 2] = 0;
						cbHandCardData[i + 3] = 0;
						i += 3;
					}
				}
			}
			return card_count;
		}
		if (card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE) {
			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])
						&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
					if (out_card_count <= cbHandCardCount) {
						for (int j = 0; j < cbHandCardCount; j++) {
							card_data[j] = cbHandCardData[j];
						}
						return cbHandCardCount;
					}
				} else if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						i += 3;
					}
				}
			}
			return card_count;
		}
		// 炸弹
		if (card_type == GameConstants.PDK_CT_BOMB_CARD) {

			for (int i = 0; i < cbHandCardCount; i++) {
				if (cbHandCardData[i] <= 0) {
					continue;
				}
				if (i < cbHandCardCount - 3
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 1])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 2])
						&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i + 3])
						&& GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
					if (!(!has_rule(GameConstants.GAME_RULE_BOOM) && (is_mj_type(GameConstants.GAME_TYPE_PDK_SW)
							|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL))) && cbHandCardData[i] != 0) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i + 1];
						card_data[card_count++] = cbHandCardData[i + 2];
						card_data[card_count++] = cbHandCardData[i + 3];
						i += 3;
					}
				}
			}
			return card_count;
		}
		return card_count;
	}

	// 赖子数目
	public int GetLaiZiCount(int cbHandCardData[], int cbHandCardCount) {
		if (_laizi == GameConstants.INVALID_CARD) {
			return 0;
		}
		int bLaiZiCount = 0;
		for (int i = 0; i < cbHandCardCount; i++) {
			if (GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(_laizi))
				bLaiZiCount++;
		}

		return bLaiZiCount;
	}

	public void GetCardTypeLaizi(int cbCardData[], int cbCardCount, tagOutCardTypeResult out_card_type_result) {
		int bLaiZiCount = GetLaiZiCount(cbCardData, cbCardCount);

		int tempCardData[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			tempCardData[i] = cbCardData[i];
		}
		for (int i = 0; i < cbCardCount; i++) {
			if (GetCardLogicValue(cbCardData[i]) == GetCardLogicValue(this._laizi)) {
				for (int valueone = 1; valueone < 14; valueone++) {
					// 一个癞子变牌
					tempCardData[i] = (0x0F - valueone) % 13 + 1;
					if (bLaiZiCount >= 2) {
						for (int j = i + 1; j < cbCardCount; j++) {
							if (GetCardLogicValue(cbCardData[j]) == GetCardLogicValue(this._laizi)) {
								for (int valuetwo = 1; valuetwo < 14; valuetwo++) {
									// 两个癞子变牌
									tempCardData[j] = (0x0F - valuetwo) % 13 + 1;
									if (bLaiZiCount >= 3) {
										for (int x = j + 1; x < cbCardCount; x++) {
											if (GetCardLogicValue(cbCardData[x]) == GetCardLogicValue(this._laizi)) {
												for (int valuethree = 1; valuethree < 14; valuethree++) {
													// 三个癞子变牌
													tempCardData[x] = (0x0F - valuethree) % 13 + 1;
													if (bLaiZiCount >= 4) {
														for (int y = x + 1; y < cbCardCount; y++) {
															if (GetCardLogicValue(cbCardData[y]) == GetCardLogicValue(
																	this._laizi)) {
																for (int valuefour = 1; valuefour < 14; valuefour++) {
																	// 四个癞子变牌
																	tempCardData[y] = (0x0F - valuefour) % 13 + 1;
																	int card_type = this.GetCardType(tempCardData,
																			cbCardCount, cbCardData);
																	if (cbCardCount == 4) {
																		// 四癞子炸弹直接返回
																		int count = out_card_type_result.cbCardTypeCount;
																		out_card_type_result.cbCardType[count] = GameConstants.PDK_CT_MAGIC_BOOM;
																		for (int card_index = 0; card_index < cbCardCount; card_index++) {
																			out_card_type_result.cbCardData[count][card_index] = cbCardData[card_index];
																		}
																		out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
																		out_card_type_result.cbCardTypeCount++;
																		return;
																	}
																	if (cbCardCount > 4
																			&& card_type == GameConstants.PDK_CT_BOMB_CARD) {
																		continue;
																	}
																	if (card_type != GameConstants.PDK_CT_ERROR) {
																		int count = out_card_type_result.cbCardTypeCount;
																		out_card_type_result.cbCardType[count] = card_type;
																		for (int card_index = 0; card_index < cbCardCount; card_index++) {
																			out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
																		}
																		out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
																		out_card_type_result.cbCardTypeCount++;
																	}
																}
															}
														}
													}
													int card_type = this.GetCardType(tempCardData, cbCardCount,
															cbCardData);
													if (cbCardCount > 4
															&& card_type == GameConstants.PDK_CT_BOMB_CARD) {
														continue;
													}
													if (card_type != GameConstants.PDK_CT_ERROR) {
														int count = out_card_type_result.cbCardTypeCount;
														out_card_type_result.cbCardType[count] = card_type;
														for (int card_index = 0; card_index < cbCardCount; card_index++) {
															out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
														}
														out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
														out_card_type_result.cbCardTypeCount++;
													}
												}
											}
										}
									}
									int card_type = this.GetCardType(tempCardData, cbCardCount, cbCardData);
									if (cbCardCount > 4 && card_type == GameConstants.PDK_CT_BOMB_CARD) {
										continue;
									}
									if (card_type != GameConstants.PDK_CT_ERROR) {
										int count = out_card_type_result.cbCardTypeCount;
										out_card_type_result.cbCardType[count] = card_type;
										for (int card_index = 0; card_index < cbCardCount; card_index++) {
											out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
										}
										out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
										out_card_type_result.cbCardTypeCount++;
									}
								}
							}
						}
					}
					int card_type = this.GetCardType(tempCardData, cbCardCount, cbCardData);
					if (cbCardCount > 4 && card_type == GameConstants.PDK_CT_BOMB_CARD) {
						continue;
					}
					if (card_type != GameConstants.PDK_CT_ERROR) {
						int count = out_card_type_result.cbCardTypeCount;
						out_card_type_result.cbCardType[count] = card_type;
						for (int card_index = 0; card_index < cbCardCount; card_index++) {
							out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
						}
						out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
						out_card_type_result.cbCardTypeCount++;
					}
				}
			}
		}

	}

	// 对比扑克
	public boolean CompareCardLaizi(int cbFirstCard[], int cbNextCard[], int cbFirstRealCard[], int cbNextRealCard[],
			int cbFirstCount, int cbNextCount, int card_type) {
		int cbFirstCardTemp[] = new int[cbFirstCount];
		int cbNextCardTemp[] = new int[cbNextCount];
		for (int j = 0; j < cbFirstCount; j++) {
			cbFirstCardTemp[j] = cbFirstCard[j];
		}
		for (int j = 0; j < cbNextCount; j++) {
			cbNextCardTemp[j] = cbNextCard[j];
		}

		int cbNextType = GetCardType(cbFirstCardTemp, cbFirstCount, cbFirstRealCard);
		int cbFirstType = GetCardType(cbNextCardTemp, cbNextCount, cbNextRealCard);

		// 类型判断
		if (cbNextType == GameConstants.PDK_CT_ERROR)
			return false;
		// 其中一方有炸弹判断
		if ((cbFirstType >= GameConstants.PDK_CT_RUAN_BOMB || cbNextType >= GameConstants.PDK_CT_RUAN_BOMB)
				&& cbFirstType != cbNextType) {
			return cbFirstType > cbNextCount;
		}
		// 规则判断
		if ((cbFirstType != cbNextType) && cbNextType == GameConstants.PDK_CT_HONG_HUA_SHUN
				&& cbFirstType == GameConstants.PDK_CT_SINGLE_LINE)
			return true;
		if ((cbFirstType != cbNextType)
				|| (cbFirstType != GameConstants.PDK_CT_BOMB_CARD && cbFirstCount != cbNextCount))
			return false;

		// 开始对比
		switch (cbNextType) {
		case GameConstants.PDK_CT_SINGLE:
		case GameConstants.PDK_CT_DOUBLE:
		case GameConstants.PDK_CT_THREE:
		case GameConstants.PDK_CT_SINGLE_LINE:
		case GameConstants.PDK_CT_DOUBLE_LINE:
		case GameConstants.PDK_CT_PLANE: {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCardTemp[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCardTemp[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:
		case GameConstants.PDK_CT_THREE_TAKE_TWO:
		case GameConstants.PDK_CT_THREE_TAKE_ONE: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCardTemp, cbNextCount, NextResult);
			AnalysebCardData(cbFirstCardTemp, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(NextResult.cbThreeCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue(FirstResult.cbThreeCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCardTemp, cbNextCount, NextResult);
			AnalysebCardData(cbFirstCardTemp, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(NextResult.cbFourCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue(FirstResult.cbFourCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_BOMB_CARD:
		case GameConstants.PDK_CT_RUAN_BOMB: {
			// 数目判断
			if (cbNextCount != cbFirstCount)
				return cbNextCount > cbFirstCount;

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCardTemp[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCardTemp[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		}

		return false;
	}

	public int get_change_data(int cbCardData[], int cbChangeData[], int cbCardCount, int card_type) {

		tagOutCardTypeResult out_card_type_result = new tagOutCardTypeResult();
		GetCardTypeLaizi(cbCardData, cbCardCount, out_card_type_result);
		for (int i = 0; i < out_card_type_result.cbCardTypeCount; i++) {
			if (card_type == GameConstants.PDK_CT_ERROR) {
				if (out_card_type_result.cbCardType[0] > GameConstants.PDK_CT_PASS) {
					for (int card_index = 0; card_index < out_card_type_result.cbEachHandCardCount[i]; card_index++) {
						cbChangeData[card_index] = out_card_type_result.cbCardData[i][card_index];
						return card_type;
					}
				}
			} else {
				if (out_card_type_result.cbCardType[i] == card_type) {
					for (int card_index = 0; card_index < out_card_type_result.cbEachHandCardCount[i]; card_index++) {
						cbChangeData[card_index] = out_card_type_result.cbCardData[i][card_index];
					}
					return card_type;
				}
			}
		}
		for (int i = 0; i < cbCardCount; i++) {
			cbChangeData[i] = cbCardData[i];
		}
		return this.GetCardType(cbCardData, cbCardCount, cbChangeData);

	}

	// 分析炸弹
	public boolean SearchBoomCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, int cbTurnRealCardData[], int card_type) {

		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;
		int nLaiziCount = this.GetLaiZiCount(cbHandCardData, cbHandCardCount);

		if (cbHandCardCount < 2)
			return false;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}
			if (cbSameCount + nLaiziCount >= 4) {
				int cbBomCardData[] = new int[cbSameCount + nLaiziCount];
				for (int j = 0; j < cbSameCount; j++) {
					cbBomCardData[j] = cbTmpCardData[i + j];
				}
				for (int j = cbSameCount; j < cbSameCount + nLaiziCount; j++) {
					cbBomCardData[j] = this._laizi;
				}
				if (CompareCardLaizi(cbTurnCardData, cbBomCardData, cbTurnRealCardData, cbBomCardData, cbTurnCardCount,
						cbSameCount + nLaiziCount, card_type)) {
					return true;
				}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	// 判断是否有压牌
	// 出牌搜索
	public boolean SearchOutCardLaiZi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, int cbChangeData[]) {

		// 获取出牌类型
		int card_type = GetCardType(cbChangeData, cbTurnCardCount, cbTurnCardData);
		if (card_type == GameConstants.PDK_CT_MAGIC_BOOM)
			return false;
		if (GetLaiZiCount(cbHandCardData, cbHandCardCount) == 4)
			return true;
		// 搜索炸弹
		if (SearchBoomCardLaizi(cbHandCardData, cbHandCardCount, cbChangeData, cbTurnCardCount, cbTurnCardData,
				card_type)) {
			return true;
		}
		// 搜索顺子
		if (card_type == GameConstants.PDK_CT_SINGLE_LINE) {
			return SearchSingleLineCardLaiZi(cbHandCardData, cbHandCardCount, cbChangeData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_DOUBLE_LINE) {
			return SearchDoubleLineCardLaizi(cbHandCardData, cbHandCardCount, cbChangeData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_PLANE) {
			return SearchThreeLineCardLaizi(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_SINGLE) {
			return SearchSingleCardLaizi(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_DOUBLE) {
			return SearchDoubleCardLaizi(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.PDK_CT_THREE || card_type == GameConstants.PDK_CT_THREE_TAKE_ONE
				|| card_type == GameConstants.PDK_CT_THREE_TAKE_TWO
				|| card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE) {
			return SearchThreeCardLaizi(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}

		// int cbFirstType=GetCardType(cbFirstCard,cbFirstCount);
		return false;
	}

	// 分析顺子
	public boolean SearchSingleLineCardLaiZi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		int nLaiziCount = this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		// 数据校验
		if (cbHandCardCount < cbTurnCardCount)
			return false;
		for (int i = 0; i < cbHandCardCount; i++) {
			if (GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)) {
				for (int valueone = 1; valueone < 14; valueone++) {
					// 一个癞子变牌
					cbTmpCard[i] = (0x0F - valueone) % 13 + 1;
					if (nLaiziCount >= 2) {
						for (int j = i + 1; j < cbHandCardCount; j++) {
							if (GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)) {
								for (int valuetwo = 1; valuetwo < 14; valuetwo++) {
									// 两个癞子变牌
									cbTmpCard[j] = (0x0F - valuetwo) % 13 + 1;
									if (nLaiziCount >= 3) {
										for (int x = j + 1; x < cbHandCardCount; x++) {
											if (GetCardLogicValue(cbHandCardData[x]) == GetCardLogicValue(
													this._laizi)) {
												for (int valuethree = 1; valuethree < 14; valuethree++) {
													// 三个癞子变牌
													cbTmpCard[x] = (0x0F - valuethree) % 13 + 1;
													if (nLaiziCount >= 4) {
														for (int y = x + 1; y < cbHandCardCount; y++) {
															if (GetCardLogicValue(
																	cbHandCardData[y]) == GetCardLogicValue(
																			this._laizi)) {
																for (int valuefour = 1; valuefour < 14; valuefour++) {
																	// 四个癞子变牌
																	cbTmpCard[y] = (0x0F - valuefour) % 13 + 1;
																	if (SearchSingleLineCard(cbTmpCard, cbHandCardCount,
																			cbTurnCardData, cbTurnCardCount)) {
																		return true;
																	}
																}
															}
														}
													}
													if (SearchSingleLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData,
															cbTurnCardCount)) {
														return true;
													}
												}
											}
										}
									}
									if (SearchSingleLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData,
											cbTurnCardCount)) {
										return true;
									}
								}
							}
						}
					}
					if (SearchSingleLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// 搜索连对
	public boolean SearchDoubleLineCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		int nLaiziCount = this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		// 数据校验
		if (cbHandCardCount < cbTurnCardCount)
			return false;
		for (int i = 0; i < cbHandCardCount; i++) {
			if (GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)) {
				for (int valueone = 1; valueone < 14; valueone++) {
					// 一个癞子变牌
					cbTmpCard[i] = (0x0F - valueone) % 13 + 1;
					if (nLaiziCount >= 2) {
						for (int j = i + 1; j < cbHandCardCount; j++) {
							if (GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)) {
								for (int valuetwo = 1; valuetwo < 14; valuetwo++) {
									// 两个癞子变牌
									cbTmpCard[j] = (0x0F - valuetwo) % 13 + 1;
									if (nLaiziCount >= 3) {
										for (int x = j + 1; x < cbHandCardCount; x++) {
											if (GetCardLogicValue(cbHandCardData[x]) == GetCardLogicValue(
													this._laizi)) {
												for (int valuethree = 1; valuethree < 14; valuethree++) {
													// 三个癞子变牌
													cbTmpCard[x] = (0x0F - valuethree) % 13 + 1;
													if (nLaiziCount >= 4) {
														for (int y = x + 1; y < cbHandCardCount; y++) {
															if (GetCardLogicValue(
																	cbHandCardData[y]) == GetCardLogicValue(
																			this._laizi)) {
																for (int valuefour = 1; valuefour < 14; valuefour++) {
																	// 四个癞子变牌
																	cbTmpCard[y] = (0x0F - valuefour) % 13 + 1;
																	if (SearchDoubleLineCard(cbTmpCard, cbHandCardCount,
																			cbTurnCardData, cbTurnCardCount)) {
																		return true;
																	}
																}
															}
														}
													}
													if (SearchDoubleLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData,
															cbTurnCardCount)) {
														return true;
													}
												}
											}
										}
									}
									if (SearchDoubleLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData,
											cbTurnCardCount)) {
										return true;
									}
								}
							}
						}
					}
					if (SearchDoubleLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
						return true;
					}
				}
			}
		}

		return false;

	}

	// 搜索飞机
	public boolean SearchThreeLineCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		int nLaiziCount = this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		// 数据校验
		if (cbHandCardCount < cbTurnCardCount)
			return false;
		for (int i = 0; i < cbHandCardCount; i++) {
			if (GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)) {
				for (int valueone = 1; valueone < 14; valueone++) {
					// 一个癞子变牌
					cbTmpCard[i] = (0x0F - valueone) % 13 + 1;
					if (nLaiziCount >= 2) {
						for (int j = i + 1; j < cbHandCardCount; j++) {
							if (GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)) {
								for (int valuetwo = 1; valuetwo < 14; valuetwo++) {
									// 两个癞子变牌
									cbTmpCard[j] = (0x0F - valuetwo) % 13 + 1;
									if (nLaiziCount >= 3) {
										for (int x = j + 1; x < cbHandCardCount; x++) {
											if (GetCardLogicValue(cbHandCardData[x]) == GetCardLogicValue(
													this._laizi)) {
												for (int valuethree = 1; valuethree < 14; valuethree++) {
													// 三个癞子变牌
													cbTmpCard[x] = (0x0F - valuethree) % 13 + 1;
													if (nLaiziCount >= 4) {
														for (int y = x + 1; y < cbHandCardCount; y++) {
															if (GetCardLogicValue(
																	cbHandCardData[y]) == GetCardLogicValue(
																			this._laizi)) {
																for (int valuefour = 1; valuefour < 14; valuefour++) {
																	// 四个癞子变牌
																	cbTmpCard[y] = (0x0F - valuefour) % 13 + 1;
																	if (SearchThreeLineCard(cbTmpCard, cbHandCardCount,
																			cbTurnCardData, cbTurnCardCount)) {
																		return true;
																	}
																}
															}
														}
													}
													if (SearchThreeLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData,
															cbTurnCardCount)) {
														return true;
													}
												}
											}
										}
									}
									if (SearchThreeLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData,
											cbTurnCardCount)) {
										return true;
									}
								}
							}
						}
					}
					if (SearchThreeLineCard(cbTmpCard, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// 搜索单张
	public boolean SearchSingleCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (GetCardLogicValue(cbTurnCardData[0]) >= 15) {
			return false;
		} else {
			int nLaiziCount = this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
			if (nLaiziCount > 0) {
				return true;
			}
		}
		for (int i = 0; i < cbHandCardCount; ++i) {
			if (GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
		}

		return false;
	}

	// 搜索对子
	public boolean SearchDoubleCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		int nLaiziCount = this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		if (GetCardLogicValue(cbTurnCardData[0]) >= 15) {
			return false;
		} else {
			if (nLaiziCount >= 2) {
				return true;
			}
		}
		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCard[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCard[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount + nLaiziCount >= 2
					&& GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	// 搜索三张
	public boolean SearchThreeCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		int nLaiziCount = this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		// 数据校验
		if (cbHandCardCount < cbTurnCardCount)
			return false;
		for (int i = 0; i < cbHandCardCount; i++) {
			if (GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)) {
				for (int valueone = 1; valueone < 14; valueone++) {
					// 一个癞子变牌
					cbTmpCard[i] = (0x0F - valueone) % 13 + 1;
					if (nLaiziCount >= 2) {
						for (int j = i + 1; j < cbHandCardCount; j++) {
							if (GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)) {
								for (int valuetwo = 1; valuetwo < 14; valuetwo++) {
									// 两个癞子变牌
									cbTmpCard[j] = (0x0F - valuetwo) % 13 + 1;
									if (nLaiziCount >= 3) {
										for (int x = j + 1; x < cbHandCardCount; x++) {
											if (GetCardLogicValue(cbHandCardData[x]) == GetCardLogicValue(
													this._laizi)) {
												for (int valuethree = 1; valuethree < 14; valuethree++) {
													// 三个癞子变牌
													cbTmpCard[x] = (0x0F - valuethree) % 13 + 1;
													if (nLaiziCount >= 4) {
														for (int y = x + 1; y < cbHandCardCount; y++) {
															if (GetCardLogicValue(
																	cbHandCardData[y]) == GetCardLogicValue(
																			this._laizi)) {
																for (int valuefour = 1; valuefour < 14; valuefour++) {
																	// 四个癞子变牌
																	cbTmpCard[y] = (0x0F - valuefour) % 13 + 1;
																	if (SearchThreeCard(cbTmpCard, cbHandCardCount,
																			cbTurnCardData, cbTurnCardCount)) {
																		return true;
																	}
																}
															}
														}
													}
													if (SearchThreeCard(cbTmpCard, cbHandCardCount, cbTurnCardData,
															cbTurnCardCount)) {
														return true;
													}
												}
											}
										}
									}
									if (SearchThreeCard(cbTmpCard, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
										return true;
									}
								}
							}
						}
					}
					if (SearchThreeCard(cbTmpCard, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public int adjustAutoOutCard(int cbHandCardData[], int cbHandCardCount) {
		int cardtype = GetCardType(cbHandCardData, cbHandCardCount, cbHandCardData);
		if (cardtype != GameConstants.PDK_CT_ERROR) {
			int cbTmpCardData[] = new int[cbHandCardCount];
			for (int i = 0; i < cbHandCardCount; i++) {
				cbTmpCardData[i] = cbHandCardData[i];
			}

			int cbBomCardCount = 0;

			if (cbHandCardCount < 2)
				return cardtype;

			// 双王炸弹
			if (0x4F == cbTmpCardData[0] && 0x4F == cbTmpCardData[1] && 0x4E == cbTmpCardData[2]
					&& 0x4E == cbTmpCardData[3]) {
				return GameConstants.PDK_CT_ERROR;
			}
			// 扑克分析
			for (int i = 0; i < cbHandCardCount; i++) {
				// 变量定义
				int cbSameCount = 1;
				int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

				// 搜索同牌
				for (int j = i + 1; j < cbHandCardCount; j++) {
					// 获取扑克
					if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
						break;

					// 设置变量
					cbSameCount++;
				}
				if (is_mj_type(GameConstants.GAME_TYPE_PDK_SW) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL)) {
					if (cbSameCount >= 4 && has_rule(GameConstants.GAME_RULE_BOOM) && cbHandCardCount != cbSameCount) {
						return GameConstants.PDK_CT_ERROR;
					}
				} else {
					if (cbSameCount >= 4 && cbHandCardCount != cbSameCount) {
						return GameConstants.PDK_CT_ERROR;
					}
				}
				if (cbSameCount >= 4 && cbHandCardCount != cbSameCount
						&& !has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
					return GameConstants.PDK_CT_ERROR;
				}

				// 设置索引
				i += cbSameCount - 1;
			}
			return cardtype;
		}
		return GameConstants.PDK_CT_ERROR;
	}
	
	public int adjustAutoOutCard_SXSD(int cbHandCardData[], int cbHandCardCount) {
		int cardtype = GetCardType_SXSD(cbHandCardData, cbHandCardCount, cbHandCardData);
		if (cardtype != GameConstants.PDK_CT_ERROR) {
			int cbTmpCardData[] = new int[cbHandCardCount];
			for (int i = 0; i < cbHandCardCount; i++) {
				cbTmpCardData[i] = cbHandCardData[i];
			}


			if (cbHandCardCount < 2)
				return cardtype;

			// 双王炸弹
			if (0x4F == cbTmpCardData[0] && 0x4F == cbTmpCardData[1] && 0x4E == cbTmpCardData[2]
					&& 0x4E == cbTmpCardData[3]) {
				return GameConstants.PDK_CT_ERROR;
			}
			// 扑克分析
			for (int i = 0; i < cbHandCardCount; i++) {
				// 变量定义
				int cbSameCount = 1;
				int cbLogicValue = GetCardLogicValue_SXSD(cbTmpCardData[i]);

				// 搜索同牌
				for (int j = i + 1; j < cbHandCardCount; j++) {
					// 获取扑克
					if (GetCardLogicValue_SXSD(cbTmpCardData[j]) != cbLogicValue)
						break;

					// 设置变量
					cbSameCount++;
				}
				if (is_mj_type(GameConstants.GAME_TYPE_PDK_SW) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL)) {
					if (cbSameCount >= 4 && has_rule(GameConstants.GAME_RULE_BOOM) && cbHandCardCount != cbSameCount) {
						return GameConstants.PDK_CT_ERROR;
					}
				} else {
					if (cbSameCount >= 4 && cbHandCardCount != cbSameCount) {
						return GameConstants.PDK_CT_ERROR;
					}
				}
				if (cbSameCount >= 4 && cbHandCardCount != cbSameCount
						&& !has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
					return GameConstants.PDK_CT_ERROR;
				}

				// 设置索引
				i += cbSameCount - 1;
			}
			return cardtype;
		}
		return GameConstants.PDK_CT_ERROR;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount, tagAnalyseIndexResult AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	public void switch_to_card_index(int card_data[], int card_count, int card_index[]) {
		for (int i = 0; i < card_count; i++) {
			int index = GetCardLogicValue(card_data[i]);
			card_index[index - 3]++;
		}
	}
	public void switch_to_card_index_SXSD(int card_data[], int card_count, int card_index[]) {
		for (int i = 0; i < card_count; i++) {
			int index = GetCardLogicValue_SXSD(card_data[i]);
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

	//public boolean has_rule(int cbRule) {
	//	return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	//}

	//public boolean is_mj_type(int type) {
	//	return _game_type_index == type;
	//}

	public void get_card_type_award(PDKTable table) {

	}
	
	//222必见333
	public boolean c222_to_c333(int [] turn_card,int turn_count,int [] out_card,int out_count,int []hand_card,int hand_count){
		int turn_type = GetCardType_SXSD(turn_card, turn_count, turn_card);
		int out_type = GetCardType_SXSD(out_card, out_count, out_card);
		if(turn_type != GameConstants.PDK_CT_THREE_TAKE_TWO && turn_type != GameConstants.PDK_CT_THREE_TAKE_DOUBLE){
			return false;
		}
		if(out_type == GameConstants.PDK_CT_THREE_TAKE_TWO && out_type == GameConstants.PDK_CT_THREE_TAKE_DOUBLE){
			return false;
		}
		int have_2 = 0;
		for(int i = 0;i < turn_count;i++){
			if(GetCardLogicValue_SXSD(turn_card[i]) == 15)
				have_2++;
		}
		if(have_2 != 3 )
			return false;
		
		if(out_type == GameConstants.PDK_CT_BOMB_CARD || out_type == GameConstants.PDK_CT_ERROR){
			tagAnalyseResult handResult = new tagAnalyseResult();
			AnalysebCardData_SXSD(hand_card, hand_count, handResult);
			boolean have_3 = false;
			if(handResult.cbFourCount != 0){
				for(int i = 0;i < handResult.cbFourCount;i++){
					if(GetCardLogicValue_SXSD(handResult.cbFourCardData[i]) == 16){
						have_3 = true;
					}
				}
			}
			if(handResult.cbThreeCount != 0){
				for(int i = 0;i < handResult.cbThreeCount;i++){
					if(GetCardLogicValue_SXSD(handResult.cbThreeCardData[i]) == 16){
						have_3 = true;
					}
				}
			}
			if(have_3){
				if(handResult.cbDoubleCount != 0){
					return true;
				}
				if(turn_type == GameConstants.PDK_CT_THREE_TAKE_TWO){
					if(hand_count >= turn_count){
						return true;
					}
				}else if(turn_type == GameConstants.PDK_CT_THREE_TAKE_DOUBLE){
					if(this.has_rule(GameConstants.GAME_RULE_SDE_WIN_SDD_SXSD)){
						if(hand_count >= turn_count){
							return true;
						}
					}else{
						if(handResult.cbDoubleCount != 0){
							return true;
						}
					}
				}
			}
		}
		
		
		return false;
	}
	
	//小炸弹比见大炸弹
	public boolean small_to_big(int turn_type,int out_type,int []hand_card,int card_count,int turn_value){
		int temp_card[] = new int[card_count];
		int temp_count = card_count;
		for(int i = 0;i < card_count;i++){
			temp_card[i] = hand_card[i];
		}
		if(turn_type != GameConstants.PDK_CT_BOMB_CARD){
			return false;
		}
		if(turn_type == GameConstants.PDK_CT_BOMB_CARD && out_type == GameConstants.PDK_CT_BOMB_CARD){
			return false;
		}
		//有炸弹不出
		tagAnalyseResult handResult = new tagAnalyseResult();
		AnalysebCardData_SXSD(temp_card, temp_count, handResult);
		if(handResult.cbFourCount > 0){
			int max_vale = 0;
			for(int i = 0;i < handResult.cbFourCount;i++){
				int vaule = GetCardLogicValue_SXSD(handResult.cbFourCardData[i]);
				max_vale = max_vale < vaule ? vaule : max_vale;
			}
			if(max_vale > GetCardLogicValue_SXSD(turn_value)){
				return true;
			}
		}
		
		
		
		return false;
	}
	
	//获取自动引爆的牌
	public boolean get_auto_boom_card(int []card_data,int card_count,int []boom_card){
		
		tagAnalyseResult handResult = new tagAnalyseResult();
		AnalysebCardData_SXSD(card_data, card_count, handResult);
		if(handResult.cbFourCount != 0){
			for(int i = 0;i < 4;i++){
				boom_card[i] = handResult.cbFourCardData[i];
			}
			return true;
		}
		return false;
	}

}
