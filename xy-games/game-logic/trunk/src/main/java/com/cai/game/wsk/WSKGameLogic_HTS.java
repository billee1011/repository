package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

public class WSKGameLogic_HTS extends WSKGameLogic {
	public int _boom_count = 6;
	public boolean hts_zuo_fei = false;//黑桃三作废
	
	protected static final int CARD_HONG_TAO_3 = 0x23;//红桃三
	protected static final int CARD_HEI_TAO_3 = 0x33;//黑桃三

	public WSKGameLogic_HTS() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		if(cbCardCount == 0){
			return GameConstants.HTS_CT_PASS;
		}
		
		if(!hts_zuo_fei && cbCardCount > 1){
			for(int i = 0;i < cbCardCount;i++){
				if(cbCardData[i] == CARD_HEI_TAO_3){
					return GameConstants.HTS_CT_ERROR;
				}
			}
		}
		
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == cbCardCount) {
			// 所有牌都为同一种牌
			if (cbCardCount == 1) {
				if(cbCardData[0] == CARD_HEI_TAO_3 && !hts_zuo_fei){
					return GameConstants.HTS_CT_HTS;
				}else{
					return GameConstants.HTS_CT_SINGLE;
				}
				
			}
			if (cbCardCount == 2) {

				return GameConstants.HTS_CT_DOUBLE;
			}
			if(cbCardCount == 3){

				return GameConstants.HTS_CT_THREE;
			}
			
			if (cbCardCount > 3) {
				//黑桃三不能当炸弹出
				return GameConstants.HTS_CT_BOOM;
			}
			return GameConstants.HTS_CT_ERROR;
		}

		if (cbCardCount == 3) {
			// 510K
			int one_card_value = this.GetCardLogicValue(cbCardData[0]);
			int two_card_value = this.GetCardLogicValue(cbCardData[1]);
			int three_card_value = this.GetCardLogicValue(cbCardData[2]);
			if ((one_card_value == 5 && two_card_value == 10 && three_card_value == 13) ||
				(one_card_value == 13 && two_card_value == 10 && three_card_value == 5)) {
				int color = this.GetCardColor(cbCardData[0]);
				for (int i = 1; i < cbCardCount; i++) {
					if (GetCardColor(cbCardData[i]) != color) {
						return GameConstants.HTS_CT_ZHA_WSK;
					}
				}
				return GameConstants.HTS_CT_ZHENG_WSK;
			}
		}
		
		if(cbCardCount == 4){
			// 三带一
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 3) {
					return GameConstants.WSK_GF_CT_THREE;
				}
			}
		}
		
		//三代二
		if (cbCardCount == 5) {
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] >= 3) {
					return GameConstants.HTS_CT_THREE_TAKE_TWO;
				}
			}
		}
		
		// 连对
		if (this.is_link(card_index, 2, cbCardCount / 2)) {
			return GameConstants.HTS_CT_LINK_DOUBLE;
		}
		
		//飞机
		if (is_plane(card_index, cbCardData, cbCardCount) == 1) {
			return GameConstants.HTS_CT_PLANE;
		}else if(is_plane(card_index, cbCardData, cbCardCount) == 0){
			return GameConstants.HTS_CT_PLANE_LOSS;
		}
		return GameConstants.HTS_CT_ERROR;
	}
	
	// 是否连
	public boolean is_link(tagAnalyseIndexResult_WSK card_data_index, int link_num, int link_count_num) {
		int pai_count = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX /*- 3*/; i++) {
			pai_count += card_data_index.card_index[i];
		}
		int num = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX /*- 3*/; i++) {
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
	
	// 飞机 0飞机缺翅膀 1飞机
	public int is_plane(tagAnalyseIndexResult_WSK card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount < 6) {
			return -1;
		}
		int num = 0;
		for (int i = GameConstants.WSK_MAX_INDEX - 1/*4*/; i >= 0; i--) {
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
	
	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,int link_three_count) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子、三代、。。。
		if (cbNextType >= GameConstants.HTS_CT_ZHA_WSK && cbFirstType < GameConstants.HTS_CT_ZHA_WSK)
			return true;
		if (cbNextType < GameConstants.HTS_CT_ZHA_WSK && cbFirstType >= GameConstants.HTS_CT_ZHA_WSK) {
			return false;
		}
		if (cbNextType >= GameConstants.HTS_CT_ZHA_WSK && cbFirstType >= GameConstants.HTS_CT_ZHA_WSK) {
			if (cbNextType == cbFirstType) {
				if (cbNextType == GameConstants.HTS_CT_ZHENG_WSK) {
					return cbNextCard[0] > cbFirstCard[0];
				} else {
					return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
				}
			} else {
				return cbNextType > cbFirstType;
			}
		}

		tagAnalyseIndexResult_WSK next_card_index = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK first_card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
		AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
		switch (cbFirstType) {
		case GameConstants.HTS_CT_SINGLE:
		case GameConstants.HTS_CT_DOUBLE: {
			if (cbFirstType != cbNextType) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case GameConstants.HTS_CT_LINK_DOUBLE: {
			if (cbFirstType != cbNextType || cbFirstCount != cbNextCount) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case GameConstants.HTS_CT_PLANE:
		case GameConstants.HTS_CT_PLANE_LOSS: {
			if (cbNextType != GameConstants.HTS_CT_PLANE && cbNextType != GameConstants.HTS_CT_PLANE_LOSS) {
				return false;
			}
			if (link_three_count * 5 < cbNextCount) {
				return false;
			}
			int first_Type_index1 = get_plane_max_index(first_card_index, cbFirstCard, cbFirstCount, link_three_count);
			int cbNextType_index1 = get_plane_max_index(next_card_index, cbNextCard, cbNextCount, link_three_count);
			return cbNextType_index1 > first_Type_index1;
		}
 
		case GameConstants.HTS_CT_THREE:
		case GameConstants.HTS_CT_THREE_TAKE_TWO:{
			if (cbNextType != GameConstants.HTS_CT_THREE_TAKE_TWO && cbNextType != GameConstants.HTS_CT_THREE) {
				return false;
			}
			int next_index = -1;
			int first_index = -1;
			for (int i = 0; i < cbNextCount; i++) {
				if (next_index == -1 && next_card_index.card_index[this.switch_card_to_idnex(cbNextCard[i])] >= 3) {
					next_index = this.switch_card_to_idnex(cbNextCard[i]);
				}

			}
			for (int i = 0; i < cbFirstCount; i++) {
				if (first_index == -1 && first_card_index.card_index[this.switch_card_to_idnex(cbFirstCard[i])] >= 3) {
					first_index = this.switch_card_to_idnex(cbFirstCard[i]);
				}
			}
			return next_index > first_index;
		}
		}
		return false;
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		// 排序过虑
		if (cbCardCount == 0)
			return;
		
		//数值理牌
		if (cbSortType == GameConstants.WSK_ST_VALUE) {
			// 转换数值
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < cbCardCount; i++) {
				if(cbCardData[i] == CARD_HEI_TAO_3){
					if(!this.hts_zuo_fei){
						cbSortValue[i] = 20;
					}else{
						cbSortValue[i] = GetCardLogicValue(cbCardData[i]);
					}
				}else{
					cbSortValue[i] = GetCardLogicValue(cbCardData[i]);
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
		
		//510k理牌
		if (cbSortType == GameConstants.WSK_ST_510K){
			int card_510K[] = new int[cbCardCount];
			int num_510K = 0;
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			// 510K排前面
			for (int j = 0; j < card_index.card_index[10]; j++) {
				card_510K[num_510K++] = card_index.card_data[10][j];
			}
			for (int j = 0; j < card_index.card_index[7]; j++) {
				card_510K[num_510K++] = card_index.card_data[7][j];
			}
			for (int j = 0; j < card_index.card_index[2]; j++) {
				card_510K[num_510K++] = card_index.card_data[2][j];
			}
			
			int cbnot510card[] = new int[GameConstants.WSK_MAX_COUNT];
			int not_510K = 0;
			for(int i = 0; i < cbCardCount; i++ ){
				int card_value = GetCardLogicValue(cbCardData[i]);
				if(card_value == 5 || card_value == 10 || card_value == 13)
					continue;
				cbnot510card[not_510K++] = cbCardData[i];
			}
			
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < not_510K; i++) {
				int card_value = GetCardLogicValue(cbnot510card[i]);
				if(cbnot510card[i] == CARD_HEI_TAO_3){
					if(!this.hts_zuo_fei){
						cbSortValue[i] = 20;
					}else{
						cbSortValue[i] = card_value;
					}
				}else{
					cbSortValue[i] = card_value;
				}
				
			}
			// 排序操作
			boolean bSorted = true;
			int cbSwitchData = 0, cbLast = not_510K - 1;
			do {
				bSorted = true;
				for (int i = 0; i < cbLast; i++) {
					if ((cbSortValue[i] < cbSortValue[i + 1])
							|| ((cbSortValue[i] == cbSortValue[i + 1]) && (cbnot510card[i] < cbnot510card[i + 1]))) {
						// 设置标志
						bSorted = false;

						// 扑克数据
						cbSwitchData = cbnot510card[i];
						cbnot510card[i] = cbnot510card[i + 1];
						cbnot510card[i + 1] = cbSwitchData;

						// 排序权位
						cbSwitchData = cbSortValue[i];
						cbSortValue[i] = cbSortValue[i + 1];
						cbSortValue[i + 1] = cbSwitchData;
					}
				}
				cbLast--;
			} while (bSorted == false);
			
			//先加510k
			for(int i = 0; i < num_510K;i++){
				cbCardData[i] = card_510K[i];
			}
			//在加非510k
			for(int i = 0; i < not_510K;i++){
				cbCardData[i+num_510K] = cbnot510card[i];
			}
			return;
		}


		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			int card_510K[] = new int[cbCardCount];
			int num_510K = 0;
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			// 510K排前面
			for (int j = 0; j < card_index.card_index[10]; j++) {
				card_510K[num_510K++] = card_index.card_data[10][j];
			}
			for (int j = 0; j < card_index.card_index[7]; j++) {
				card_510K[num_510K++] = card_index.card_data[7][j];
			}
			for (int j = 0; j < card_index.card_index[2]; j++) {
				card_510K[num_510K++] = card_index.card_data[2][j];
			}
			card_index.card_index[10] = 0;
			card_index.card_index[7] = 0;
			card_index.card_index[2] = 0;
			int index[] = new int[GameConstants.WSK_MAX_INDEX];
			for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
				index[i] = i;
			}
			for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
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
			// 王牌后面
			int sort_num = 0;

			for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					cbCardData[sort_num++] = card_index.card_data[index[i]][j];
				}
			}
			// 510K牌最后
			for (int i = 0; i < num_510K; i++) {
				cbCardData[sort_num++] = card_510K[i];
			}
			for (int j = 0; j < card_index.card_index[14]; j++) {
				cbCardData[sort_num++] = card_index.card_data[14][j];
			}
			for (int j = 0; j < card_index.card_index[13]; j++) {
				cbCardData[sort_num++] = card_index.card_data[13][j];
			}
			return;
		} else if (cbSortType == GameConstants.WSK_ST_ORDER) {
			int card_510K[] = new int[cbCardCount];
			int num_510K = 0;
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			// 510K排前面
			for (int j = 0; j < card_index.card_index[10]; j++) {
				card_510K[num_510K++] = card_index.card_data[10][j];
			}
			for (int j = 0; j < card_index.card_index[7]; j++) {
				card_510K[num_510K++] = card_index.card_data[7][j];
			}
			for (int j = 0; j < card_index.card_index[2]; j++) {
				card_510K[num_510K++] = card_index.card_data[2][j];
			}
			card_index.card_index[10] = 0;
			card_index.card_index[7] = 0;
			card_index.card_index[2] = 0;
			int sort_num = 0;
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
				for (int j = 0; j < card_index.card_index[i]; j++) {
					cbCardData[sort_num++] = card_index.card_data[i][j];
				}
			}
			// 510K牌最后
			for (int i = 0; i < num_510K; i++) {
				cbCardData[sort_num++] = card_510K[i];
			}
			// 王牌后面
			for (int j = 0; j < card_index.card_index[13]; j++) {
				cbCardData[sort_num++] = card_index.card_data[13][j];
			}
			for (int j = 0; j < card_index.card_index[14]; j++) {
				cbCardData[sort_num++] = card_index.card_data[14][j];
			}
		} else {
			// 转换数值
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < cbCardCount; i++) {
				cbSortValue[i] = GetCardLogicValue(cbCardData[i]);

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
		}

		return;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count,int three_link_count) {
		int turn_card_type = this.GetCardType(turn_card_data, turn_card_count);

		switch (turn_card_type) {
		case GameConstants.HTS_CT_SINGLE: 
		case GameConstants.HTS_CT_DOUBLE:{
			return search_out_single_double(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
							tip_out_count, all_tip_count);
		}
		case GameConstants.HTS_CT_LINK_DOUBLE:{
			return search_out_card_double_link(cbCardData, cbCardCount, turn_card_data, turn_card_count,tip_out_card, 
					tip_out_count,all_tip_count);
		}
		case GameConstants.HTS_CT_THREE:
		case GameConstants.HTS_CT_THREE_TAKE_TWO:{
			 
			return search_out_card_three(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, all_tip_count);
		}
		case GameConstants.HTS_CT_PLANE:
		case GameConstants.HTS_CT_PLANE_LOSS:{
			return search_out_card_plane(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					three_link_count, tip_out_card, tip_out_count, all_tip_count);
		}
		case GameConstants.HTS_CT_ZHA_WSK: {
			return search_out_card_false_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.HTS_CT_ZHENG_WSK: {
			return search_out_card_real_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.HTS_CT_BOOM: {
			return search_out_card_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.HTS_CT_ERROR: {
			return search_out_error(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		}
		return all_tip_count;
	}

	public int search_out_card_double_single(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] >= 3) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] >= turn_card_count && index > turn_index) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}

	public int search_out_card_boom(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] > turn_card_count) {
				return 1;
			} else if (hand_card_idnex.card_index[index] == turn_card_count && index > turn_index) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}

	public int search_out_card_single_link(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < turn_card_count;) {
			int index = switch_card_to_idnex(turn_card_data[i]);
			if (index < turn_index) {
				turn_index = index;
			}
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}
		}
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] >= 3) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] >= 1 && index > turn_index) {
				int prv_index = index;
				for (int j = i + hand_card_idnex.card_index[index]; j < cbCardCount;) {
					int other_index = this.switch_card_to_idnex(cbCardData[j]);
					if (hand_card_idnex.card_index[other_index] >= 3) {
						return 1;
					}
					if (hand_card_idnex.card_index[other_index] > 0 && prv_index == other_index - 1) {
						prv_index = other_index;
						if ((prv_index - index) + 1 >= turn_card_count) {
							return 1;
						}
					} else {
						break;
					}
					if (hand_card_idnex.card_index[other_index] > 0) {
						j += hand_card_idnex.card_index[other_index];
					} else {
						j++;
					}
				}
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}
	public int search_out_card_double_link(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (card_index.card_index[i] > 0) {
				turn_index = i;
				break;
			}
		}
		// 不拆炸弹
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX ; i++) {
			if (hand_card_idnex.card_index[i] >= 2 && hand_card_idnex.card_index[i] < 4 && i > turn_index) {
				for (int j = i + 1; j < GameConstants.WSK_MAX_INDEX; j++) {
					if (hand_card_idnex.card_index[j] >= 2 && hand_card_idnex.card_index[j] < 4) {
						if ((j - i) + 1 == turn_card_count / 2) {
							for (int x = j; x >= i; x--) {
								for (int y = 0; y < 2; y++) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[x][y];
								}
							}
							all_tip_count++;
							break;
						}
					} else {
						break;
					}
				}
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		return all_tip_count;
	}


	public int search_out_card_double_link(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < turn_card_count;) {
			int index = switch_card_to_idnex(turn_card_data[i]);
			if (index < turn_index) {
				turn_index = index;
			}
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}
		}
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] >= 3) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] >= 2 && index > turn_index) {
				int prv_index = index;
				for (int j = i + hand_card_idnex.card_index[index]; j < cbCardCount;) {
					int other_index = this.switch_card_to_idnex(cbCardData[j]);
					if (hand_card_idnex.card_index[other_index] >= 3) {
						return 1;
					}
					if (hand_card_idnex.card_index[other_index] >= 2 && prv_index == other_index - 1) {
						prv_index = other_index;
						if ((prv_index - index) + 1 >= turn_card_count / 2) {
							return 1;
						}
					}
					if (hand_card_idnex.card_index[other_index] > 0) {
						j += hand_card_idnex.card_index[other_index];
					} else {
						j++;
					}
				}
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}
	
	public int search_out_card_three(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < turn_card_count;) {
			int index = switch_card_to_idnex(turn_card_data[i]);
			if (card_index.card_index[index] >= 3) {
				turn_index = index;
				break;
			}
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}
		}
		// 不拆炸弹
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (hand_card_idnex.card_index[i] == 3 && i > turn_index) {
				int take_count = 0;
				boolean have_hts = false;
				for (int index_num = 1; index_num < 4; index_num++) {
					for (int take_index = 0; take_index < GameConstants.WSK_MAX_INDEX; take_index++) {
						if (take_count >= 2) {
							break;
						}
						if (take_index != i && hand_card_idnex.card_index[take_index] == index_num) {
							for (int y = 0; y < hand_card_idnex.card_index[take_index]; y++) {
								if(hand_card_idnex.card_data[take_index][y] == CARD_HEI_TAO_3 && !this.hts_zuo_fei){
									have_hts = true;
									continue;
								}
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[take_index][y];
								take_count++;
								if (take_count >= 2) {
									break;
								}
							}
						}

					}
				}
				//有带的或者最后一手少带才可以提起
				if(take_count >= 2 || (cbCardCount < 5 && !have_hts)){
					for (int y = 0; y < hand_card_idnex.card_index[i]; y++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][y];
					}
					all_tip_count++;
				}else{
					tip_out_count[all_tip_count] = 0;
					for (int y = 0; y < take_count; y++) {
						
						tip_out_card[all_tip_count][y] = 0;
					}
				}
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		return all_tip_count;
	}
	
	public int search_out_card_plane(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int three_link_count, int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];

		// 找出飞机的最大值
		int turn_index = -1;
		for (int i = GameConstants.WSK_MAX_INDEX - 1/*4*/; i >= 0; i--) {

			if (card_index.card_index[i] >= 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] >= 3) {
						if ((i - j) + 1 == three_link_count) {
							turn_index = i;
							break;
						}
					}
				}
			}
			if (turn_index != -1) {
				break;
			}
		}
		// 搜索飞机
		for (int i = GameConstants.WSK_MAX_INDEX - 1/*4*/; i >= 0; i--) {

			if (cbCardCount > three_link_count * 5) {
				// 如果牌还够的情况下，先不考虑拆炸弹
				if (hand_card_idnex.card_index[i] == 3 && i > turn_index) {
					for (int j = i - 1; j >= 0; j--) {
						if (hand_card_idnex.card_index[j] == 3) {
							if ((i - j) + 1 == three_link_count) {
								int take_count = 0;
								for (int index_num = 1; index_num < 4; index_num++) {
									for (int take_index = 0; take_index < GameConstants.WSK_MAX_INDEX; take_index++) {
										if (take_index < j || take_index > i) {
											if (hand_card_idnex.card_index[take_index] == index_num) {
												for (int y = 0; y < hand_card_idnex.card_index[take_index]; y++) {
													if(hand_card_idnex.card_data[take_index][y] == CARD_HEI_TAO_3 && !this.hts_zuo_fei)
														continue;
													tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[take_index][y];
													take_count++;
													if (take_count == three_link_count * 2) {
														break;
													}
												}
											}
											if (take_count == three_link_count * 2) {
												break;
											}
										}
									}
									if (take_count == three_link_count * 2) {
										break;
									}
								}
								if (take_count == three_link_count * 2) {
									for (int x = i; x >= j; x--) {
										for (int y = 0; y < hand_card_idnex.card_index[x]; y++) {
											tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[x][y];
										}
									}
									all_tip_count++;
								}

								break;
							} /*else {
								i = j + 1;
								break;
							}*/
						}else {
							i = j + 1;
							break;
						}
					}
				}
			} else {
				// 如果牌还够的情况下，先不考虑拆炸弹
				if (hand_card_idnex.card_index[i] >= 3 && i > turn_index) {
					for (int j = i - 1; j >= 0; j--) {
						if (hand_card_idnex.card_index[j] >= 3) {
							if ((i - j) + 1 == three_link_count) {
								for (int take_index = 0; take_index < GameConstants.WSK_MAX_INDEX; take_index++) {
									if (take_index < j || take_index > i) {
										for (int y = 0; y < hand_card_idnex.card_index[take_index]; y++) {
											if(hand_card_idnex.card_data[take_index][y] == CARD_HEI_TAO_3 && !this.hts_zuo_fei)
												continue;
											tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[take_index][y];
										}
									}
								}
								for (int x = i; x >= j; x--) {
									for (int y = 0; y < hand_card_idnex.card_index[x]; y++) {
										tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[x][y];
									}
								}
								all_tip_count++;

								break;
							}
						} else {
							i = j + 1;
							break;
						}
					}
				}
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}
	
	public int search_out_card_real_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		if (card_index.card_index[13] + card_index.card_index[14] >= 2) {
			return 1;
		}

		// 正510K黑桃直接返回
		if (this.GetCardColor(cbCardData[0]) == 3) {
			return 0;
		}
		if (card_index.card_index[2] <= 0 || card_index.card_index[7] <= 0 || card_index.card_index[10] <= 0) {
			return 0;
		}

		int color = this.GetCardColor(cbCardData[0]);
		for (int color_temp = color + 1; color_temp < 4; color_temp++) {
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < card_index.card_index[2]; i++) {
				if (this.GetCardColor(card_index.card_data[2][i]) == color_temp) {
					is_five = true;
					break;
				}
			}
			for (int i = 0; i < card_index.card_index[7]; i++) {
				if (this.GetCardColor(card_index.card_data[7][i]) == color_temp) {
					is_ten = true;
					break;
				}
			}
			for (int i = 0; i < card_index.card_index[10]; i++) {
				if (this.GetCardColor(card_index.card_data[10][i]) == color_temp) {
					is_k = true;
					break;
				}
			}

			if (is_five && is_ten && is_k) {
				return 1;
			}
		}

		return 0;
	}

	public int search_out_card_false_510K(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		if (card_index.card_index[13] + card_index.card_index[14] >= 2) {
			return 1;
		}
		if (card_index.card_index[2] <= 0 || card_index.card_index[7] <= 0 || card_index.card_index[10] <= 0) {
			return 0;
		}

		for (int color = 0; color < 4; color++) {
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < card_index.card_index[2]; i++) {
				if (this.GetCardColor(card_index.card_data[2][i]) == color) {
					is_five = true;
				}
			}
			for (int i = 0; i < card_index.card_index[7]; i++) {
				if (this.GetCardColor(card_index.card_data[7][i]) == color) {
					is_ten = true;
				}
			}
			for (int i = 0; i < card_index.card_index[10]; i++) {
				if (this.GetCardColor(card_index.card_data[10][i]) == color) {
					is_k = true;
				}
			}
			if (is_five && is_ten && is_k) {
				return 1;
			}
		}

		return 0;
	}
	
	public int search_out_error(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		for (int count = 1; count <= 5; count++) {
			for (int i = 0; i < cbCardCount;) {
				int index = this.switch_card_to_idnex(cbCardData[i]);
				if (hand_card_idnex.card_index[index] == count) {
					for (int y = 0; y < hand_card_idnex.card_index[index]; y++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][y];
					}
					all_tip_count++;
				}
				if (hand_card_idnex.card_index[index] > 0) {
					i += hand_card_idnex.card_index[index];
				} else {
					i++;
				}
			}
		}

		// 搜索炸弹
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_boom(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		// 不带王的炸弹
		for (int add_boom_count = 0; add_boom_count <= 12 - turn_card_count; add_boom_count++) {
			for (int i = 0; i < cbCardCount;) {
				int index = this.switch_card_to_idnex(cbCardData[i]);
				if (add_boom_count == 0) {
					if (hand_card_idnex.card_index[index] == turn_card_count && index > turn_index) {
						for (int j = 0; j < hand_card_idnex.card_index[index]; j++) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][j];
						}
						all_tip_count++;
					}
				} else {
					if (hand_card_idnex.card_index[index] == turn_card_count + add_boom_count) {
						for (int j = 0; j < hand_card_idnex.card_index[index]; j++) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][j];
						}
						all_tip_count++;
					}
				}
				if (hand_card_idnex.card_index[index] > 0) {
					i += hand_card_idnex.card_index[index];
				} else {
					i++;
				}
			}
		}

		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 12 - turn_card_count; add_boom_count++) {
				for (int i = 0; i < cbCardCount;) {
					int index = this.switch_card_to_idnex(cbCardData[i]);
					if (hand_card_idnex.card_index[index] >= _boom_count - 1) {
						if (add_boom_count == 0) {
							if (hand_card_idnex.card_index[index] + count == turn_card_count && index > turn_index) {
								for (int j = 0; j < hand_card_idnex.card_index[index]; j++) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][j];
								}

								int use_wang = 0;
								for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
									use_wang++;
								}
								for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
									use_wang++;
								}
								all_tip_count++;
							}
						} else {
							if (hand_card_idnex.card_index[index] + count == turn_card_count + add_boom_count) {
								for (int j = 0; j < hand_card_idnex.card_index[index]; j++) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][j];
								}
								int use_wang = 0;
								for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
									use_wang++;
								}
								for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
									use_wang++;
								}
								all_tip_count++;
							}
						}
					}

					if (hand_card_idnex.card_index[index] > 0) {
						i += hand_card_idnex.card_index[index];
					} else {
						i++;
					}
				}
			}
		}
		
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);

		return all_tip_count;
	}

	public int search_out_card_tong_hua(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
			if (hand_card_idnex.card_index[i] == 3 && i > turn_index) {

				for (int x = 0; x < hand_card_idnex.card_index[i] - 3; x++) {
					int color_count = 0;
					int color = this.GetCardColor(hand_card_idnex.card_data[i][x]);
					for (int j = 0; j < hand_card_idnex.card_index[i] - 3; j++) {
						if (GetCardColor(hand_card_idnex.card_data[i][j]) == color) {
							color_count++;
						}
					}
					if (color_count >= 3) {
						for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
							if (GetCardColor(hand_card_idnex.card_data[i][j]) == color) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
							}
						}
						all_tip_count++;
						break;
					}
				}

			}
		}
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_real_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {

		int turn_color = GetCardColor(turn_card_data[0]);
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		// 没有510K的情况
		if (hand_card_idnex.card_index[2] <= 0 || hand_card_idnex.card_index[7] <= 0
				|| hand_card_idnex.card_index[10] <= 0) {

			all_tip_count += search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
			
			if(!this.hts_zuo_fei)
				all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
			
			return all_tip_count;
		}
		
		for (int color = 0; color < 4; color++) {
			if(color < turn_color)
				continue;
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color) {
					is_five = true;
					break;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color) {
					is_ten = true;
					break;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color) {
					is_k = true;
					break;
				}
			}

			if (is_five && is_ten && is_k) {
				for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[7][j]) == color && color > turn_color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
						break;
					}
				}
				for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[10][j]) == color && color > turn_color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
						break;
					}
				}
				for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[2][j]) == color && color > turn_color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
						break;
					}
				}
				all_tip_count++;
			}
		}
		
		
		all_tip_count += search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		return all_tip_count;
	}

	public int search_out_card_false_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		// 没有510K的情况
		if (hand_card_idnex.card_index[2] <= 0 || hand_card_idnex.card_index[7] <= 0
				|| hand_card_idnex.card_index[10] <= 0) {

			all_tip_count += search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
			
			if(!this.hts_zuo_fei)
				all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
			
			return all_tip_count;
		}
		// 有510K的情况下
		// 纯510K
		for (int color = 0; color < 4; color++) {
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color) {
					is_five = true;
					break;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color) {
					is_ten = true;
					break;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color) {
					is_k = true;
					break;
				}
			}

			if (is_five && is_ten && is_k) {
				for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[7][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
						break;
					}
				}
				for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[10][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
						break;
					}
				}
				for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[2][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
						break;
					}
				}
				all_tip_count++;
			}
		}
		
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);

		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		return all_tip_count;
	}

	public int search_out_card_tong_zhang(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);

		for (int add_count = 0; add_count < _boom_count - turn_card_count; add_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (hand_card_idnex.card_index[i] == turn_card_count + add_count && i > turn_index) {
					if (turn_card_count >= 3 && i < GameConstants.WSK_MAX_INDEX - 2) {
						boolean is_tonghua = false;
						for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
							int color = this.GetCardColor(hand_card_idnex.card_data[i][j]);
							int count = 1;
							for (int x = j + 1; x < hand_card_idnex.card_index[i]; x++) {
								if (this.GetCardColor(hand_card_idnex.card_data[i][x]) == color) {
									count++;
								}
							}
							if (count >= 3) {
								is_tonghua = true;
								break;
							}
						}
						if (!is_tonghua) {
							for (int j = 0; j < turn_card_count; j++) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
							}
							all_tip_count++;
						}
					} else {
						for (int j = 0; j < turn_card_count; j++) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
						}
						all_tip_count++;
					}

				}
			}
		}

		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		//搜索黑桃三
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}
	
	public int search_out_single_double(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		// 不拆炸弹
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (hand_card_idnex.card_index[i] >= turn_card_count && hand_card_idnex.card_index[i] < 4
					&& i > turn_index) {
				for (int y = 0; y < turn_card_count; y++) {
					tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][y];
				}
				all_tip_count++;
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		//搜索黑桃三
		if(!this.hts_zuo_fei)
			all_tip_count = search_hei_tao_san(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		
		return all_tip_count;
	}

	public int search_510k(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			// 510K
			for (int color_five = 0; color_five < 4; color_five++) {
				for (int color_ten = 0; color_ten < 4; color_ten++) {
					for (int color_k = 0; color_k < 4; color_k++) {
						boolean is_five = false;
						boolean is_ten = false;
						boolean is_k = false;
						for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color_five) {
								is_five = true;
								break;
							}
						}
						for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color_ten) {
								is_ten = true;
								break;
							}
						}
						for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color_k) {
								is_k = true;
								break;
							}
						}

						if (is_five && is_ten && is_k) {
							for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[10][j]) == color_k) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
									break;
								}
							}
							for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[7][j]) == color_ten) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
									break;
								}
							}
							for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[2][j]) == color_five) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
									break;
								}
							}

							all_tip_count++;
						}

					}
				}
			}
		}
		return all_tip_count;
	}

	public int search_boom(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
		// 不带王的炸弹
		for (int add_boom_count = 0; add_boom_count <= 8 - 4; add_boom_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (hand_card_idnex.card_index[i] == 4 + add_boom_count) {
					for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
					}
					all_tip_count++;
				}
			}
		}
		// 王炸
		if (hand_card_idnex.card_index[14] + hand_card_idnex.card_index[13] == 4) {
			for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
			}
			for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
			}
			all_tip_count++;
		}
		// 带王的炸弹
		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 12 - 4; add_boom_count++) {
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
					if (hand_card_idnex.card_index[i] >= 4) {
						if (hand_card_idnex.card_index[i] + count == 4 + add_boom_count) {
							for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
							}
							int use_wang = 0;
							for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
								if (use_wang >= count) {
									break;
								}
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
								use_wang++;
							}
							for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
								if (use_wang >= count) {
									break;
								}
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
								use_wang++;
							}
							all_tip_count++;
						}
					}
				}
			}
		}
		return all_tip_count;
	}
	
	public void sort_card_date_list_by_type(int card_date[], int card_count, int type, int three_link_count) {
		switch (type) {
		case GameConstants.HTS_CT_THREE:
		case GameConstants.HTS_CT_THREE_TAKE_TWO:{
			tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			int count = 0;
			for (int i = 0; i < card_count; i++) {
				int index = this.switch_card_to_idnex(card_date[i]);
				if (card_data_index.card_index[index] >= 3) {
					for (int j = 0; j < card_data_index.card_index[index]; j++) {
						card_date[count++] = card_data_index.card_data[index][j];
					}
					break;
				}
			}
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (card_data_index.card_index[i] < 3) {
					for (int j = 0; j < card_data_index.card_index[i]; j++) {
						card_date[count++] = card_data_index.card_data[i][j];
					}
				}
			}
			return;
		}
		case GameConstants.HTS_CT_PLANE:
		case GameConstants.HTS_CT_PLANE_LOSS: {
			tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			int count = 0;
			for (int i = 0; i < card_count; i++) {
				int index = this.switch_card_to_idnex(card_date[i]);
				if (card_data_index.card_index[index] >= 3) {
					for (int j = 0; j < 3; j++) {
						card_date[count++] = card_data_index.card_data[index][j];
						//card_data_index.card_data[index][j] = 0;
					}
					if(card_data_index.card_index[index] == 4){
						card_data_index.card_data[index][0] = card_data_index.card_data[index][3];
					}
					card_data_index.card_index[index] -= 3;
					
				}
			}
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (card_data_index.card_index[i] > 0) {
					for (int j = 0; j < card_data_index.card_index[i]; j++) {
						card_date[count++] = card_data_index.card_data[i][j];
					}
				}
			}
			return ;
		}
		}
	}

	
	public int search_hei_tao_san(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count){
		for(int i = 0;i < cbCardCount;i++){
			if(cbCardData[i] == 0x33){
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = 0x33;
				all_tip_count++;
			}
		}
		return all_tip_count;
	}
	
	public int get_three_link_count(int cbCardData[], int cbCardCount, int type) {
		tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_data_index);
		if (type == GameConstants.HTS_CT_PLANE_LOSS) {
			for (int i = GameConstants.WSK_MAX_INDEX - 1/*4*/; i >= 0; i--) {
				// 三个2不能当做飞机
				if (card_data_index.card_index[i] >= 3) {
					int link_num = 1;
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							link_num++;
						} else {
							if (link_num > 1 && link_num * 5 > cbCardCount) {
								return link_num;
							}
						}
						if (j == 0) {
							if (link_num > 1 && link_num * 5 > cbCardCount) {
								return link_num;
							}
						}
					}
				}
			}
		} else {
			for (int i = GameConstants.WSK_MAX_INDEX - 1/*4*/; i >= 0; i--) {
				// 三个2不能当做飞机
				if (card_data_index.card_index[i] >= 3) {
					int link_num = 1;
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							link_num++;
							if (link_num * 5 == cbCardCount) {
								return link_num;
							}
						}
						if (j == 0) {
							if (link_num > 1 && link_num * 5 == cbCardCount) {
								return link_num;
							}
						}
					}
				}
			}
		}
		return 0;
	}
	
	public int get_plane_max_index(tagAnalyseIndexResult_WSK card_data_index, int cbCardData[], int cbCardCount,
			int three_link_count) {
		for (int i = GameConstants.WSK_MAX_INDEX - 1/*4*/; i >= 0; i--) {
			// 三个2不能当做飞机
			if (card_data_index.card_index[i] >= 3) {
				int link_num = 1;
				for (int j = i - 1; j >= 0; j--) {
					if (card_data_index.card_index[j] >= 3) {
						link_num++;
						if (link_num == three_link_count) {
							return i;
						}
					} else {
						break;
					}
				}
			}
		}

		return -1;
	}
	

}
