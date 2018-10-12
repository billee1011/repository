package com.cai.game.wsk.gzhbzp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;


class AnalyseIndexResult {
	public int card_index[] = new int[GameConstants.WSK_TC_MAX_INDEX];
	public int card_data[][] = new int[GameConstants.WSK_TC_MAX_INDEX][GameConstants.WSK_MAX_COUNT];

	public AnalyseIndexResult() {
		for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}

	public void reset() {
		for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}
};

public class GameLogic_GZHBZP {
	public final int TRANSFER_TYPE_MIX_WSK = 101; // 转换后的杂五十K的牌型
	public final int TRANSFER_TYPE_PURE_WSK = 102; // 转换后的纯五十K的牌型
	public final int TRANSFER_TYPE_BOMB = 103; // 转换后的炸弹的牌型

	public Map<Integer, Integer> ruleMap = new HashMap<>();

	public GameLogic_GZHBZP() {
	}
	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color_one_bit(card);
		return (cbValue >= 1) && (cbValue <= 16) && (cbColor <= 4);
	}

	public int is_all_single(int cbCardData[],int cbCardCount){
		int value = 15;
		int i = 0; 
		while(this.get_card_logic_value(cbCardData[i]) == value){
			i++;
			value--;
			if(i == cbCardCount)
				break;
		}
		if(i<cbCardCount)
			return GameConstants.BZP_GZH_ERROR;
		return GameConstants.BZP_GZH_ALL_SINGLE;
	}
	public int get_card_type(int cbCardData[],int cbCardCount){ 
		int type = GameConstants.BZP_GZH_ERROR;
		if(cbCardData[0] < 0)
			return type;
		int temp_card[] = new int[cbCardCount];
		for(int i = 0; i< cbCardCount; i++){
			temp_card[i] = cbCardData[i];
		}
		this.sort_out_card_list(temp_card, cbCardCount);
		if(cbCardCount == 13){
			type = is_all_single(temp_card,cbCardCount);
			if(type != GameConstants.BZP_GZH_ERROR)
				return type;
		}
		AnalyseIndexResult card_index = new AnalyseIndexResult();
		store_card_data(temp_card, cbCardCount, card_index);
		for(int index = 4; index > 0 ;index --)
		{
			int flag_count = 0;
			int max_value = 0;
			for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX; i++) {
				if(flag_count >0&&card_index.card_index[i] != index&&i != 11)
				    break;	
				if (card_index.card_index[i] == index)
				{
					flag_count++;
					max_value = this.switch_index_to_value(i);
				}
			}
			if(index == 4&& flag_count > 2)
				type = GameConstants.BZP_GZH_GUN_LONG;
			else if(index == 4 && flag_count == 1)
				type = GameConstants.BZP_GZH_BOMB;
			if(index == 3&& flag_count >2)
				type = GameConstants.BZP_GZH_PLANE;
			if(index == 3 && flag_count == 1)
				type = GameConstants.BZP_GZH_THREE;
			if(index == 2 && flag_count > 2)
				type = GameConstants.BZP_GZH_DOUBLE_LINK;
			if(index == 2 && flag_count == 1)
				type = GameConstants.BZP_GZH_DOUBLE;
			if(index == 1 && flag_count > 2)
				type = GameConstants.BZP_GZH_SINGLE_LINK;
			if(index == 1 && flag_count == 1)
				type = GameConstants.BZP_GZH_SINGLE;
			if(type == GameConstants.BZP_GZH_GUN_LONG)
			{
				type = type|(flag_count<<4)|max_value;
				break;
			}
			else if(type != 0 && flag_count*index == cbCardCount){
				type = type|(flag_count<<4)|max_value;
				break;
			}
			else 
				type = GameConstants.BZP_GZH_ERROR;
				
			
			
			
		}
		return type;
	}
	public int get_card_max_type(int cbCardData[],int cbCardCount,int card_type){ 
		int type = GameConstants.BZP_GZH_ERROR;
		if(card_type ==  GameConstants.BZP_GZH_ERROR)
			return type;
		int temp_card[] = new int[cbCardCount];
		for(int i = 0; i< cbCardCount; i++){
			temp_card[i] = cbCardData[i];
		}
		this.sort_out_card_list(temp_card, cbCardCount);
		if(cbCardCount == 13){
			type = is_all_single(temp_card,cbCardCount);
		}
		{
			AnalyseIndexResult card_index = new AnalyseIndexResult();
			store_card_data(temp_card, cbCardCount, card_index);
			int i = GameConstants.WSK_TC_MAX_INDEX-3;
			for(int index = 4; index > 0 ;)
			{
				
				if(i == -1)
				{
					i = GameConstants.WSK_TC_MAX_INDEX-3;
					index --;
				}
				int flag_count = 0;
				int max_value = 0;
				type = 0;
				for (; i>=0; i--) {
					if(flag_count >0&&(card_index.card_index[i] < index|| i == 11))
					    break;	
					
					if (card_index.card_index[i] >= index)
					{
						if(flag_count == 0)
						{
							max_value = this.switch_index_to_value(i);
						}
						flag_count++;
						
					}
				}

				if(index == 4&& flag_count > 2)
					type = GameConstants.BZP_GZH_GUN_LONG;
				else if(index == 4 && flag_count >=1)
				{
					flag_count = 1;
					type = GameConstants.BZP_GZH_BOMB;
				}
				if(index == 3&& flag_count >2)
					type = GameConstants.BZP_GZH_PLANE;
				else if(index == 3 && flag_count >=1)
				{
					flag_count = 1;
					type = GameConstants.BZP_GZH_THREE;
				}
				if(index == 2 && flag_count > 2)
					type = GameConstants.BZP_GZH_DOUBLE_LINK;
				else if(index == 2 && flag_count >= 1)
				{
					flag_count = 1;
					type = GameConstants.BZP_GZH_DOUBLE;
				}
				if(index == 1 && flag_count > 2)
					type = GameConstants.BZP_GZH_SINGLE_LINK;
				if(index == 1 && flag_count >= 1&&flag_count <3)
				{
					flag_count = 1;
					type = GameConstants.BZP_GZH_SINGLE;
				}
			   if(type != 0 ){
					type = type|(flag_count<<4)|max_value;
				}
				if(type < GameConstants.BZP_GZH_THREE&&card_type < GameConstants.BZP_GZH_THREE)
				{
					if((type > 0)&&(type&GameConstants.BZP_GZH_SINGLE) != 0 && (card_type&GameConstants.BZP_GZH_SINGLE) != 0)
					{
					    int type_value = card_type&0xf;
						if(type > card_type && max_value > type_value)
							return type;
					}
					if((type > 0)&&(type&GameConstants.BZP_GZH_DOUBLE) != 0 && (card_type&GameConstants.BZP_GZH_DOUBLE) != 0)
					{
						if(type > card_type)
							return type;
					}
					if((type > 0)&&(type&GameConstants.BZP_GZH_SINGLE_LINK) != 0 && ((card_type&GameConstants.BZP_GZH_SINGLE) != 0)||(card_type&GameConstants.BZP_GZH_SINGLE_LINK) != 0)
					{
						int card_value = card_type &0x0f;
						int card_flag_count = (card_type &0xf0)>>4;
						if(max_value > card_value && flag_count >=card_flag_count)
							return type;
					}
				}
				else{
					if(type > card_type)
						return type;
				}
				
			
			}
			
			
		}
		return GameConstants.BZP_GZH_ERROR;
	}
	 /* 全局搜索哪些牌能压上家出的牌
	 * 
	 * @param hand_card_data
	 * @param cbHandCardCount
	 * @param cbOutCardData
	 * @param out_card_count
	 * @param can_out_cards
	 * @return
	 */
	public int search_can_out_cards(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count, int can_out_cards[]) {
		int cbHandCardData[] = Arrays.copyOf(hand_card_data, cbHandCardCount);

		int can_out_card_count = 0;

		// 标记能出的牌，不管手牌里是否有两张一样的牌，最后轮询手牌，进行转换
		Set<Integer> set = new HashSet<>();
		int[] tmpHandCardData = Arrays.copyOf(cbHandCardData, cbHandCardCount);
		int tmpHandCardCount = cbHandCardCount;

		AnalyseIndexResult analyseIndexResult = new AnalyseIndexResult();
		store_card_data(tmpHandCardData, tmpHandCardCount, analyseIndexResult);
		
		int cbFirstType  = this.get_card_type(cbOutCardData, out_card_count);
		int cbNextType = get_card_max_type(hand_card_data,cbHandCardCount,cbFirstType);

		if(cbNextType > cbFirstType)
		{
			can_out_cards[0] = hand_card_data[0];
			can_out_card_count++;
		}
		return can_out_card_count;
	}

	/**
	 * 将手牌进行排序
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbSortType
	 */
	public void sort_card_list_before_card_change(int cbCardData[], int cbCardCount, int cbSortType) {
		if (cbCardCount == 0)
			return;


		int zheng_card_num = 0;

		int fu_card_num = 0;

		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			AnalyseIndexResult card_index = new AnalyseIndexResult();
			store_card_data(cbCardData, cbCardCount, card_index);

			int card_five_count = card_index.card_index[2];
			int card_ten_count = card_index.card_index[7];
			int card_thirteen_count = card_index.card_index[10];

			int[] cards_five = card_index.card_data[2];
			int[] cards_ten = card_index.card_data[7];
			int[] cards_thirteen = card_index.card_data[10];

			

			sort_card_list_before_card_change(cbCardData, cbCardCount, GameConstants.WSK_ST_ORDER);

			card_index.reset();

			store_card_data(cbCardData, cbCardCount, card_index);

			int index[] = new int[GameConstants.WSK_TC_MAX_INDEX];
			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
			}

			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = i - 1; j >= 0; j--) {
					int count_i = card_index.card_index[index[i]];
					int count_j = card_index.card_index[index[j]];

					if (count_i > count_j) {
						int temp = index[j];
						index[j] = index[i];
						index[i] = temp;
					} else if (count_i == count_j) {
						if (index[i] > index[j]) {
							int temp = index[j];
							index[j] = index[i];
							index[i] = temp;
						}
					}
				}
			}

			int sort_num = 0;

			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				int count = card_index.card_index[index[i]];
				if (count < 4) {
					for (int j = 0; j < count; j++) {
						cbCardData[sort_num++] = card_index.card_data[index[i]][j];
					}
				}
			}

			sort_card_list_before_card_change(cbCardData, sort_num, GameConstants.WSK_ST_ORDER);



			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				int count = card_index.card_index[index[i]];
				if (count >= 4) {
					for (int j = 0; j < count; j++) {
						cbCardData[sort_num++] = card_index.card_data[index[i]][j];
					}
				}
			}


			return;
		} else if (cbSortType == GameConstants.WSK_ST_ORDER) {
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < cbCardCount; i++) {
				cbSortValue[i] = get_card_logic_value(cbCardData[i]);
			}

			boolean bSorted = true;
			int cbSwitchData = 0, cbLast = cbCardCount - 1;

			do {
				bSorted = true;
				for (int i = 0; i < cbLast; i++) {
					if ((cbSortValue[i] > cbSortValue[i + 1]) || ((cbSortValue[i] == cbSortValue[i + 1]) && (cbCardData[i] > cbCardData[i + 1]))) {
						bSorted = false;

						cbSwitchData = cbCardData[i];
						cbCardData[i] = cbCardData[i + 1];
						cbCardData[i + 1] = cbSwitchData;

						cbSwitchData = cbSortValue[i];
						cbSortValue[i] = cbSortValue[i + 1];
						cbSortValue[i + 1] = cbSwitchData;
					}
				}
				cbLast--;
			} while (bSorted == false);

		

			return;
		}
	}


	/**
	 * 将打出的牌进行排序
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbSortType
	 */
	public void sort_out_card_list(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 0)
			return;

		int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
		for (int i = 0; i < cbCardCount; i++) {
				cbSortValue[i] = get_card_logic_value(cbCardData[i]);
		}

		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;

		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if ((cbSortValue[i] < cbSortValue[i + 1])
						|| ((cbSortValue[i] == cbSortValue[i + 1]) && ((cbCardData[i] & 0xFF) < (cbCardData[i + 1] & 0xFF)))) {
					bSorted = false;

					cbSwitchData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbSwitchData;

					cbSwitchData = cbSortValue[i];
					cbSortValue[i] = cbSortValue[i + 1];
					cbSortValue[i + 1] = cbSwitchData;
				}
			}
			cbLast--;
		} while (bSorted == false);

		return;
	}

	public boolean remove_card(int cbRemoveCard[], int cbRemoveCount, int cbCardData[], int cbCardCount) {
		int cbDeleteCount = 0, cbTempCardData[] = new int[GameConstants.WSK_MAX_COUNT];
		if (cbCardCount > cbTempCardData.length)
			return false;
		for (int i = 0; i < cbCardCount; i++) {
			cbTempCardData[i] = cbCardData[i];
		}

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

		int cbCardPos = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbTempCardData[i] != 0)
				cbCardData[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

    public int switch_s_to_c(int card_type){
    	int type = (card_type>>8)<<8;
    	switch(type){
    	case GameConstants.BZP_GZH_SINGLE:
    		return GameConstants.BZP_GZH_SINGLE_SZ;
    	case GameConstants.BZP_GZH_DOUBLE:
    		return GameConstants.BZP_GZH_DOUBLE_SZ;
    	case GameConstants.BZP_GZH_SINGLE_LINK:
    		return GameConstants.BZP_GZH_SINGLE_LINK_SZ;
    	case GameConstants.BZP_GZH_THREE:
    		return GameConstants.BZP_GZH_THREE_SZ;
    	case GameConstants.BZP_GZH_THREE_SZ:
    		return GameConstants.BZP_GZH_SINGLE_SZ;
    	case GameConstants.BZP_GZH_DOUBLE_LINK:
    		return GameConstants.BZP_GZH_DOUBLE_LINK_SZ;
    	case GameConstants.BZP_GZH_BOMB:
    		return GameConstants.BZP_GZH_BOMB_SZ;
    	case GameConstants.BZP_GZH_PLANE:
    		return GameConstants.BZP_GZH_PLANE_SZ;
    	case GameConstants.BZP_GZH_GUN_LONG:
    		return GameConstants.BZP_GZH_GUN_LONG_SZ;
    	case GameConstants.BZP_GZH_ALL_SINGLE:
    		return GameConstants.BZP_GZH_ALL_SINGLE_SZ;	
    	}
    	return GameConstants.BZP_GZH_PASS;
    }
	/**
	 * 比较两个玩家出的牌的大小。牌数据已经用make_change_card进行合理的转换。
	 * 
	 * @param cbFirstCard
	 * @param cbNextCard
	 * @param cbFirstCount
	 * @param cbNextCount
	 * @return
	 */
	public boolean compare_card(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		int cbNextType = get_card_type(cbNextCard, cbNextCount);
		int cbFirstType = get_card_type(cbFirstCard, cbFirstCount);
		if(cbFirstType == GameConstants.BZP_GZH_ERROR && cbNextType !=  GameConstants.BZP_GZH_ERROR)
			return true;
		if(cbFirstType < GameConstants.BZP_GZH_THREE&&cbNextType < GameConstants.BZP_GZH_THREE)
		{
			if(cbFirstCount!=cbNextCount)
				return false;
			if((cbFirstType&GameConstants.BZP_GZH_SINGLE) != 0 && (cbNextType&GameConstants.BZP_GZH_SINGLE) != 0)
			{
				if(cbFirstType>cbNextType)
					return false;
				else 
					return true;
			}
			if((cbFirstType&GameConstants.BZP_GZH_DOUBLE) != 0 && (cbNextType&GameConstants.BZP_GZH_DOUBLE) != 0)
			{
				if(cbFirstType>cbNextType)
					return false;
				else 
					return true;
			}
			if((cbFirstType&GameConstants.BZP_GZH_SINGLE_LINK) != 0 && (cbNextType&GameConstants.BZP_GZH_SINGLE_LINK) != 0)
			{
				if(cbFirstType>cbNextType)
					return false;
				else 
					return true;
			}
			return false;
			
		}
		
		if(cbFirstType>cbNextType)
			return false;
		else 
			return true;
	}

	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);
	}

	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);
	}

	/**
	 * 获取牌数据里的分值，5对应5分，10或K对应10分
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int get_card_score(int cbCardData[], int cbCardCount) {
		int score = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (get_card_logic_value(cbCardData[i]) == Constants_GZHBZP.CARD_FIVE) {
				score += 5;
			} else if (get_card_logic_value(cbCardData[i]) == Constants_GZHBZP.CARD_TEN
					|| get_card_logic_value(cbCardData[i]) == Constants_GZHBZP.CARD_THIRTEEN) {
				score += 10;
			}
		}
		return score;
	}

	public int get_card_value(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_VALUE;
	}

	public int get_card_color(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_COLOR;
	}

	public int get_card_color_one_bit(int cbCardData) {
		return (cbCardData & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	/**
	 * 获取十六进制牌型的逻辑牌值。 A的十六进制为0x01，逻辑牌值为1+13。 2的十六进制为0x02，逻辑牌值为2+13。
	 * 小王的十六进制为0x4E，逻辑牌值为14+2。大王的十六进制为0x4F，逻辑牌值为15+2。花牌的十六进制为0x5F，逻辑牌值为15+3。
	 * 
	 * @param CardData
	 * @return
	 */
	public int get_card_logic_value(int CardData) {
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);


		return (cbCardValue == 2 || cbCardValue == 1) ? (cbCardValue + 13) : cbCardValue;
	}


	/**
	 * 获取花牌的张数
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int get_flower_count(int cbCardData[], int cbCardCount) {
		AnalyseIndexResult card_index = new AnalyseIndexResult();

		store_card_data(cbCardData, cbCardCount, card_index);

		return card_index.card_index[15];
	}

	/**
	 * 牌进行转换之后，获取大小王和花牌的张数
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param AnalyseIndexResult
	 * @return
	 */
	public int get_wang_count_after_card_change(int cbCardData[], int cbCardCount, AnalyseIndexResult AnalyseIndexResult) {
		int count = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] > Constants_GZHBZP.SPECIAL_CARD_TYPE)
				count++;
		}
		count += AnalyseIndexResult.card_index[13] + AnalyseIndexResult.card_index[14] + AnalyseIndexResult.card_index[15];
		return count;
	}

	/**
	 * 保存中间分析结果的十六进制牌值数据，和每种牌的张数。
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param analyseIndexResult
	 */
	public void store_card_data(int cbCardData[], int cbCardCount, AnalyseIndexResult analyseIndexResult) {
		for (int i = 0; i < cbCardCount; i++) {
			int index = switch_card_to_index(cbCardData[i]);

			if (index < 0)
				continue;

			int count = analyseIndexResult.card_index[index];

			analyseIndexResult.card_data[index][count] = cbCardData[i];

			analyseIndexResult.card_index[index]++;
		}
	}

	/**
	 * 根据十六进制牌值，获取到实际的牌值索引。先获取到逻辑牌值，再获取到时间的索引。
	 * 3-K的索引为0-10，A为11，2为12，小王为13，大王为14，花牌为15。
	 * 
	 * @param card
	 * @return
	 */
	public int switch_card_to_index(int card) {
		int index = get_card_logic_value(card) - 3;
		return index;
	}

	/**
	 * 将牌的索引转换成牌值（统一转成方块0x0*）。 索引0-10对应3-K。 索引11对应14（A）。 索引12对应15（2）。不会转大小王和花牌
	 * 
	 * @param index
	 * @return
	 */
	public int switch_index_to_value(int index) {
		return index + 3;
	}

}
