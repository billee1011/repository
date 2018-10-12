/**
 * 
 */
package com.cai.game.shidianban;

import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.nn.NNGameLogic;



public class SDBGameLogic {

private static Logger logger = Logger.getLogger(NNGameLogic.class);

	
	// 获取数值
	public int get_card_value(int card) {
		int card_value =  card & GameConstants.LOGIC_MASK_VALUE;
		return card_value>10?5:card_value*10;
	}
	// 获取数值
	public int get_real_card_value(int card) {
		
		return card & GameConstants.LOGIC_MASK_VALUE;
	}
	// 获取数值
	public int get_logic_card_value(int card) {
		if((card & GameConstants.LOGIC_MASK_VALUE) == 1)
			return 14;
		return card & GameConstants.LOGIC_MASK_VALUE;
	}
	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
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
	public int getRuleValue(Map<Integer, Integer> ruleMaps,int game_rule) {
		if (!ruleMaps.containsKey(game_rule)) {
			return 0;
		}
		return ruleMaps.get(game_rule);
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
	// 牌数数目
	public int get_card_count_by_index(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}
	public String get_card_ox_value(int ox_value)
	{
		String des ="";
		switch(ox_value)
		{
		case 0: 
			des += "无牛";
				break;
		case GameConstants.CZWXOX_JIA_ONE:
		case GameConstants.CZWXOX_ZHEN_ONE:
			 des += "牛一";
				break;
		case GameConstants.CZWXOX_JIA_TWO:
		case GameConstants.CZWXOX_ZHEN_TWO:
			 des += "牛二";
				break;
		case GameConstants.CZWXOX_JIA_THREE:
		case GameConstants.CZWXOX_ZHEN_THREE:
			 des += "牛三";
				break;
		case GameConstants.CZWXOX_JIA_FOUR:
		case GameConstants.CZWXOX_ZHEN_FOUR:
			 des += "牛四";
				break;
		case GameConstants.CZWXOX_JIA_FIVE:
		case GameConstants.CZWXOX_ZHEN_FIVE:
			 des += "牛五";
				break;
		case GameConstants.CZWXOX_JIA_SIX:
		case GameConstants.CZWXOX_ZHEN_SIX:
			des += "牛六";
				break;
		case GameConstants.CZWXOX_JIA_SEVEN:
		case GameConstants.CZWXOX_ZHEN_SEVEN:
			 des += "牛七";
				break;
		case GameConstants.CZWXOX_JIA_EIGHT:
		case GameConstants.CZWXOX_ZHEN_EIGHT:
			 des += "牛八";
				break;
		case GameConstants.CZWXOX_JIA_NINE:
		case GameConstants.CZWXOX_ZHEN_NINE:
			 des += "牛九";
				break;
		case GameConstants.CZWXOX_JIA_TEN:
		case GameConstants.CZWXOX_ZHEN_TEN:
			 des += "牛牛";
				break;
		case GameConstants.CZWXOX_SHUN_ZI:
			 des += "顺子";
				break;
		case GameConstants.CZWXOX_TONG_HUA:
			 des += "同花";
				break;
		case GameConstants.CZWXOX_HU_LU:
			 des += "葫芦";
				break;
		case GameConstants.CZWXOX_WXL:
			 des += "五小牛";
				break;
		case GameConstants.CZWXOX_WDL:
			 des += "五大牛";
				break;
		case GameConstants.CZWXOX_BOOM:
			 des += "炸弹牛";
				break;
		case GameConstants.CZWXOX_TONG_HUA_SHUN:
			 des += "同花顺";
				break;
 
		}
		return des;
	}
    /***
	 * 获取牛牛类型
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_card_type(int card_data[],int card_count){
        
		int card_value = GameConstants.OX_VALUE0;
		for(int i = 0 ;i < card_count; i++)
		{
			card_value += this.get_card_value(card_data[i]);
		
		}
		if(card_value > 105)
			card_value = 120;
		return card_value;
	}


	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times(int card_date[],int card_count){
		int times = 0;
		times = get_card_type(card_date, card_count);
		if(times == 105)
			return 2;
		else 
			return 1;
	}

	/***
	 * //获取整数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public boolean  IsIntValue(int card_date[],int card_count)
	{
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return false;
		int sum=0;
		for(int i=0;i<card_count;i++)
		{
			sum+=get_card_value(card_date[i]);
		}

		return (sum%10==0);
	}


	/***
	 * 	//排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public void ox_sort_card_list(int card_date[],int card_count)
	{
		//转换数值
		int logic_value[] = new int [GameConstants.OX_MAX_CARD_COUNT];
		for (int i=0;i<card_count;i++) logic_value[i]=get_real_card_value(card_date[i]);

		//排序操作
		boolean sorted=true;
		int temp_date,last=card_count-1;
		do
		{
			sorted=true;
			for (int i=0;i<last;i++)
			{
				if ((logic_value[i]<logic_value[i+1])||
					((logic_value[i]==logic_value[i+1])&&(card_date[i]<card_date[i+1])))
				{
					//交换位置
					temp_date=card_date[i];
					card_date[i]=card_date[i+1];
					card_date[i+1]=temp_date;
					temp_date=logic_value[i];
					logic_value[i]=logic_value[i+1];
					logic_value[i+1]=temp_date;
					sorted=false;
				}	
			}
			last--;
		} while(sorted==false);

		return;
	}
	
	public int  get_boom_value(int card_data[],int card_count)
	{
		for(int i=0;i<card_count;i++)
		{
			int four_count = 1;
			int temp_real_value = this.get_logic_card_value(card_data[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_logic_card_value(card_data[j])))
					four_count++;
				
			}
			
			if(four_count == 4)
			{
				return temp_real_value;
			}
		}
		return 0;
	}
	/***
	 * 	//对比扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @param card_date
	 * @param card_count
	 * @param card_count
	 * @return
	 */
	//对比扑克
	public int  compare_card(int first_data[], int next_date[], int first_count,int next_count)
	{	
		//获取点数
		int next_type=get_card_type(next_date,next_count);
		int first_type=get_card_type(first_data,first_count);
		//点数判断
		if(first_type >105 && next_type >105)	
			return 1;
		if(first_type <= 105 && next_type <= 105)
			if (first_type>=next_type)
				return 1;
			else 
				return -1;
		if(first_type > 105 && next_type <= 105)	
			return -1;
		
		if(first_type<=105 && next_type > 105)
			return 1;
		
		 
		if (first_type>=next_type)
			return 1;
		else 
			return -1;

	}
/////////////////////////////////////////////////////////////////////OX-code/////////////////////////
	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
	}
}
