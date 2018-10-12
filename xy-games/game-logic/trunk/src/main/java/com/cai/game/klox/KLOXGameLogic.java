/**
 * 
 */
package com.cai.game.klox;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.nn.NNGameLogic;



public class KLOXGameLogic {

private static Logger logger = Logger.getLogger(NNGameLogic.class);

	
	// 获取数值
	public int get_card_value(int card) {
		int card_value =  card & GameConstants.LOGIC_MASK_VALUE;
		return card_value>10?10:card_value;
	}
	// 获取数值
	public int get_real_card_value(int card) {
		
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
		case 1:
			 des += "牛一";
				break;
		case 2:
			 des += "牛二";
				break;
		case 3:
			 des += "牛三";
				break;
		case 4:
			 des += "牛四";
				break;
		case 5:
			 des += "牛五";
				break;
		case 6:
			des += "牛六";
				break;
		case 7:
			 des += "牛七";
				break;
		case 8:
			 des += "牛八";
				break;
		case 9:
			 des += "牛九";
				break;
		case 10:
			 des += "牛牛";
				break;
		case GameConstants.FKN_SHUNZI:
			 des += "顺子";
				break;
		case GameConstants.FKN_TONGHUA:
			 des += "同花";
				break;
		case GameConstants.FKN_HULU:
			 des += "葫芦";
				break;
		case GameConstants.FKN_WUXIAONIU:
			 des += "五小牛";
				break;
		case GameConstants.FKN_WUHUANIU:
			 des += "五花牛";
				break;
		case GameConstants.FKN_BOOM:
			 des += "炸弹牛";
				break;
		case GameConstants.FKN_TONGHUASHUN:
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
	public int get_card_type(int card_date[],int card_count, int game_rule_index){
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;

		int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);
		int da_count = 0;
		int xiao_count = 0;
		int count = 0;
		for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			if(this.get_card_color(card_date[i]) == 4)
			{
				if(this.get_real_card_value(card_date[i]) == 14)
				{
					xiao_count++;
				}
				else 
					da_count ++;
			}
			else
				temp_card_data[count++] = card_date[i];
			if(this.get_card_color(card_date[i]) != 4)
				card_real_value_count[get_real_card_value(card_date[i])]++;
		}
		ox_sort_card_list(temp_card_data,count);
		boolean same_color = true;
		boolean line_card = true;
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_real_card_value(temp_card_data[0]);
		int temp_da_count = da_count;
		int temp_xiao_count = xiao_count;
		int value_count = 1;
		if(first_value+da_count+xiao_count>= 13)
		{
			if(get_real_card_value(temp_card_data[count-1])==1)
			{
				temp_card_data[count-1] = get_card_color(temp_card_data[count-1])*16+9;
			}
			
		}
		for(int i = 1; i< count ; i++)
		{
			if(get_real_card_value(temp_card_data[i])!=first_value-value_count) line_card = false;
			if(get_real_card_value(temp_card_data[i])>first_value-value_count &&line_card == false)
				break;
			if(get_real_card_value(temp_card_data[i])+temp_da_count+temp_xiao_count < first_value-value_count&& line_card == false)
				break;
		   if(temp_da_count > 0&&line_card == false)
		    {
		    	temp_da_count = 0;
		    	line_card = true;
		    	if(get_real_card_value(temp_card_data[i]) != 0)
		    		value_count++;
		    }
		    if(temp_xiao_count > 0&&line_card == false)
		    {
		    	temp_xiao_count = 0;
		    	if(get_real_card_value(temp_card_data[i]) != 0)
		    		value_count++;
		    	line_card = true;
		    	
		    }
		    if(get_real_card_value(temp_card_data[i]) == 0&&line_card ==false)
		    {
		    	line_card = true;
		    }
		    if(line_card == false)
		    	break;
		    value_count++;
		}
		for(int i = 1; i < count;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			
			if(same_color == false )
				break;
		}
		if((same_color==true)&&(line_card == true)){
//			if(da_count == 1)
//				return GameConstants.KLOX_DA_THSH;
//			else if(xiao_count == 1)
//				return GameConstants.KLOX_XIAO_THSH;
//			else 
				return GameConstants.KLOX_THSH;
		}
		int  king_count=0,ten_count=0;
		int sum = 0;
		int four_count = 0;
		int temp_count = 0;
		for(int i=0;i<card_count;i++)
		{
			temp_count = 1;
			int temp_real_value = get_real_card_value(card_date[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_date[j])))
					temp_count++;
			}
			if(temp_count > four_count)
				four_count = temp_count;

			if(temp_count == 4)
				break;
		}
		for(int i=0;i<card_count;i++)
		{
			if(get_real_card_value(card_date[i])>10)
			{
				king_count++;
			}
			else if(get_real_card_value(card_date[i])==10)
			{
				ten_count++;
			}
			if(get_card_value(card_date[i])>=5&&get_card_color(card_date[i])!=4)
				sum  = 11;
			if(get_card_color(card_date[i]) == 4)
				sum +=1;
			else
				sum += get_card_value(card_date[i]);
			
		}
		if(four_count + da_count + xiao_count >= 4)
		{
//			if(four_count == 4)
//				return GameConstants.KLOX_BOOM;
//			else if(da_count == 1)
//				return GameConstants.KLOX_DA_BOOM;
//			else if(xiao_count == 1)
//				return GameConstants.KLOX_XIAO_BOOM;
//			else
				return GameConstants.KLOX_BOOM;
		}
	
		if(king_count == GameConstants.OX_MAX_CARD_COUNT)
		{
			if(da_count == 1)
				return GameConstants.KLOX_DA_WU_HUA_NIU;
			else if(xiao_count == 1)
				return GameConstants.KLOX_XIAO_WU_HUA_NIU;
			else 
				 return GameConstants.KLOX_WU_HUA_NIU;
		}
	
			
		if(sum <= 10 && da_count == 1)
			return  GameConstants.KLOX_DA_WXOX;
		if(sum <= 10 && xiao_count == 1)
			return  GameConstants.KLOX_XIAO_WXOX;
		if(sum <= 10)
			return  GameConstants.KLOX_WXOX;
		
		
		boolean is_two = false;
		boolean is_three = false;
		int card_real_count = 0;
		for(int i = 0; i< 14;i++)
		{
			if(card_real_value_count[i] ==2)
				is_two = true;
			if(card_real_value_count[i] == 3)
				is_three = true;
			if(card_real_value_count[i] > 0)
				
				card_real_count++;
		}
		if(card_real_count <= 2)
		{
//			if(da_count == 1)
//				return  GameConstants.KLOX_DA_HU_LU;
//			else if(xiao_count == 1)
//				return  GameConstants.KLOX_XIAO_HU_LU;
			return GameConstants.KLOX_HU_LU;
		}
		if(is_two ==  true&&is_three == true)
		{
			return GameConstants.KLOX_HU_LU;
		}
	
		
	
		if((same_color==true)) 
		{
			if(da_count == 1)
				return  GameConstants.KLOX_DA_TONG_HUA;
			else if(xiao_count == 1)
				return  GameConstants.KLOX_XIAO_TONG_HUA;
			else
				return  GameConstants.KLOX_TONG_HUA;
		}
		if((line_card == true))
		{
//			if(da_count == 1)
//				return  GameConstants.KLOX_DA_SHUN_ZI;
//			else if(xiao_count == 1)
//				return  GameConstants.KLOX_XIAO_SHUN_ZI;
//			else
				return  GameConstants.KLOX_SHUN_ZI;
		}
	    count = 0;
		for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			
			if(this.get_card_color(card_date[i]) != 4)
				temp_card_data[count++] = card_date[i];
			if(this.get_card_color(card_date[i]) != 4)
				card_real_value_count[get_real_card_value(card_date[i])]++;
		}

		int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		sum=0;
		for (int i=0;i<count;i++)
		{
			temp_value[i]=get_card_value(temp_card_data[i]);
			if(get_card_color(temp_card_data[i]) == 4)
				temp_value[i] = 10;
			sum+=temp_value[i];
		}
		if(da_count == 1 && xiao_count == 1)
			return  GameConstants.KLOX_DA_TEN;

		if(da_count + xiao_count == 0)
		{
			for (int i=0;i<count-1;i++)
			{
				for (int j=i+1;j<count;j++)
				{
					if((sum-temp_value[i]-temp_value[j])%10==0)
					{	
						return ((temp_value[i]+temp_value[j])>10)?((temp_value[i]+temp_value[j])-10)*3:(temp_value[i]+temp_value[j])*3;
					}
				}
			}
		}
		if(da_count + xiao_count != 0)
		{
			boolean flag = false;
			int value = 0;
			int temp_card_count= 0;
			//查找牛牛
			for (int i=0;i<count;i++)
			{
				if((sum-temp_value[i])%10==0)
				{
					value = 10;
					if(da_count == 1)	
						return value*3 - 1;
						
					else if(xiao_count == 1)
						return value*3 - 2;

					
				}
				
			}
			for(int i = 0; i< count - 1; i++)
			{
				
				for(int j = i+1; j<count; j++)
				{
					if((temp_value[i] + temp_value[j])%10 == 0)
					{
						value = 10;
						flag = true;
						break;
					}
					if(value < (temp_value[i] + temp_value[j])%10)
					{
						value = (temp_value[i] + temp_value[j])%10;
					}
					
				}
				if(flag == true)
				{
					break;
				}
				
			}
			if(da_count == 1)
			{
				return value*3 - 1;
			}
			else if(xiao_count == 1)
			{
				return value*3 - 2;
			}
		}
		return GameConstants.OX_VALUE0;
	}
	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_one(int card_date[],int card_count,int game_rule_index){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,game_rule_index);
		switch(times)
		{
		case GameConstants.KLOX_VALUE0: // 混合牌型 无牛
		case GameConstants.KLOX_XIAO_ONE:// 小牛1
		case GameConstants.KLOX_DA_ONE:// 大牛1
		case GameConstants.KLOX_ONE: // 牛1
		case GameConstants.KLOX_XIAO_TWO:// 小牛2
		case GameConstants.KLOX_DA_TWO://大牛2
		case GameConstants.KLOX_TWO: // 牛2
		case GameConstants.KLOX_XIAO_THREE:// 小牛3
		case GameConstants.KLOX_DA_THREE: // 大牛3
		case GameConstants.KLOX_THREE: // 牛3
		case GameConstants.KLOX_XIAO_FOUR:// 小牛4
		case GameConstants.KLOX_DA_FOUR: // 大牛4
		case GameConstants.KLOX_FOUR: // 牛4
		case GameConstants.KLOX_XIAO_FIVE:// 小牛5
		case GameConstants.KLOX_DA_FIVE: // 大牛5
		case GameConstants.KLOX_FIVE: // 牛5
		case GameConstants.KLOX_XIAO_SIX:// 小牛6
		case GameConstants.KLOX_DA_SIX: // 大牛6
		case GameConstants.KLOX_SIX: // 牛6
		case GameConstants.KLOX_XIAO_SEVEN:// 小牛7
		case GameConstants.KLOX_DA_SEVEN: // 大牛7
		case GameConstants.KLOX_SEVEN: // 牛7
			return 1;
		case GameConstants.KLOX_XIAO_EIGHT:// 小牛8
		case GameConstants.KLOX_DA_EIGHT:// 大牛8
		case GameConstants.KLOX_EIGHT: // 牛8
			return 2;
		case GameConstants.KLOX_XIAO_NINE:// 小牛9
		case GameConstants.KLOX_DA_NINE:// 大牛9
		case GameConstants.KLOX_NINE:// 牛9
			return 2;
		case GameConstants.KLOX_XIAO_TEN:// 小牛10
		case GameConstants.KLOX_DA_TEN:// 大牛10
		case GameConstants.KLOX_TEN:// 牛10
			return 3;
		case GameConstants.KLOX_XIAO_SHUN_ZI:// 小顺子牛 
		case GameConstants.KLOX_DA_SHUN_ZI:// 大顺子 牛
		case GameConstants.KLOX_SHUN_ZI:// 顺子
		case GameConstants.KLOX_XIAO_TONG_HUA:// 小同花牌
		case GameConstants.KLOX_DA_TONG_HUA: // 大同花牛
		case GameConstants.KLOX_TONG_HUA: // 同花
		case GameConstants.KLOX_XIAO_HU_LU:// 小葫芦牛
		case GameConstants.KLOX_DA_HU_LU: // 大葫芦牛
		case GameConstants.KLOX_HU_LU: // 葫芦牛
		case GameConstants.KLOX_XIAO_WXOX:// 小五小牛
		case GameConstants.KLOX_DA_WXOX:// 大五小牛
		case GameConstants.KLOX_WXOX: // 五小牛
		case GameConstants.KLOX_XIAO_WU_HUA_NIU:// 小五花牛
		case GameConstants.KLOX_DA_WU_HUA_NIU: // 大五花牛
		case GameConstants.KLOX_WU_HUA_NIU: // 五花牛
		case GameConstants.KLOX_XIAO_BOOM:// 小炸弹牛
		case GameConstants.KLOX_DA_BOOM: // 大炸弹牛
		case GameConstants.KLOX_BOOM:// 炸弹
		case GameConstants.KLOX_XIAO_THSH:// 小同花顺
		case GameConstants.KLOX_DA_THSH: //大同花顺
		case GameConstants.KLOX_THSH:// 同花顺
			return 5;
		}
		return times;
	}

	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_two(int card_date[],int card_count,int game_rule_index){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,game_rule_index);
		switch(times)
		{
		case GameConstants.KLOX_VALUE0: // 混合牌型 无牛
		case GameConstants.KLOX_XIAO_ONE:// 小牛1
		case GameConstants.KLOX_DA_ONE:// 大牛1
		case GameConstants.KLOX_ONE: // 牛1
			return 1;
		case GameConstants.KLOX_XIAO_TWO:// 小牛2
		case GameConstants.KLOX_DA_TWO://大牛2
		case GameConstants.KLOX_TWO: // 牛2
			return 2;
		case GameConstants.KLOX_XIAO_THREE:// 小牛3
		case GameConstants.KLOX_DA_THREE: // 大牛3
		case GameConstants.KLOX_THREE: // 牛3
			return 3;
		case GameConstants.KLOX_XIAO_FOUR:// 小牛4
		case GameConstants.KLOX_DA_FOUR: // 大牛4
		case GameConstants.KLOX_FOUR: // 牛4
			return 4;
		case GameConstants.KLOX_XIAO_FIVE:// 小牛5
		case GameConstants.KLOX_DA_FIVE: // 大牛5
		case GameConstants.KLOX_FIVE: // 牛5
			return 5;
		case GameConstants.KLOX_XIAO_SIX:// 小牛6
		case GameConstants.KLOX_DA_SIX: // 大牛6
		case GameConstants.KLOX_SIX: // 牛6
			return 6;
		case GameConstants.KLOX_XIAO_SEVEN:// 小牛7
		case GameConstants.KLOX_DA_SEVEN: // 大牛7
		case GameConstants.KLOX_SEVEN: // 牛7
			return 7;
		case GameConstants.KLOX_XIAO_EIGHT:// 小牛8
		case GameConstants.KLOX_DA_EIGHT:// 大牛8
		case GameConstants.KLOX_EIGHT: // 牛8
			return 8;
		case GameConstants.KLOX_XIAO_NINE:// 小牛9
		case GameConstants.KLOX_DA_NINE:// 大牛9
		case GameConstants.KLOX_NINE:// 牛9
			return 9;
		case GameConstants.KLOX_XIAO_TEN:// 小牛10
		case GameConstants.KLOX_DA_TEN:// 大牛10
		case GameConstants.KLOX_TEN:// 牛10
			return 10;
		case GameConstants.KLOX_XIAO_SHUN_ZI:// 小顺子牛 
		case GameConstants.KLOX_DA_SHUN_ZI:// 大顺子 牛
		case GameConstants.KLOX_SHUN_ZI:// 顺子
			return 15;
		case GameConstants.KLOX_XIAO_TONG_HUA:// 小同花牌
		case GameConstants.KLOX_DA_TONG_HUA: // 大同花牛
		case GameConstants.KLOX_TONG_HUA: // 同花
			return 16;
		case GameConstants.KLOX_XIAO_HU_LU:// 小葫芦牛
		case GameConstants.KLOX_DA_HU_LU: // 大葫芦牛
		case GameConstants.KLOX_HU_LU: // 葫芦牛
			return 17;
		case GameConstants.KLOX_XIAO_WXOX:// 小五小牛
		case GameConstants.KLOX_DA_WXOX:// 大五小牛
		case GameConstants.KLOX_WXOX: // 五小牛
			return 18;
		case GameConstants.KLOX_XIAO_WU_HUA_NIU:// 小五花牛
		case GameConstants.KLOX_DA_WU_HUA_NIU: // 大五花牛
		case GameConstants.KLOX_WU_HUA_NIU: // 五花牛
			return 19;
		case GameConstants.KLOX_XIAO_BOOM:// 小炸弹牛
		case GameConstants.KLOX_DA_BOOM: // 大炸弹牛
		case GameConstants.KLOX_BOOM:// 炸弹
			return 20;
		case GameConstants.KLOX_XIAO_THSH:// 小同花顺
		case GameConstants.KLOX_DA_THSH: //大同花顺
		case GameConstants.KLOX_THSH:// 同花顺
			return 25;
		}
		return times;
	}

	/***
	 * 获取牛值
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public boolean get_ox_card(int card_date[],int card_count,int game_rule_index){
 		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return false;
		int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);
		int da_count = 0;
		int xiao_count = 0;
		int count = 0;
		for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			if(this.get_card_color(card_date[i]) == 4)
			{
				if(this.get_real_card_value(card_date[i]) == 14)
				{
					xiao_count++;
				}
				else 
					da_count ++;
			}
			else
				temp_card_data[count++] = card_date[i];
			if(this.get_card_color(card_date[i]) != 4)
				card_real_value_count[get_real_card_value(card_date[i])]++;
		}
		ox_sort_card_list(temp_card_data,card_count);
		boolean same_color = true;
		boolean line_card = true;
		int temp_shu_card[] = new int [GameConstants.OX_MAX_CARD_COUNT];
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_real_card_value(temp_card_data[0]);
		int flag_card = 0;
		int shun_card_count = 0;
		if(first_value+da_count+xiao_count>= 13)
		{
			if(get_real_card_value(temp_card_data[count-1])==1)
			{
				temp_shu_card[shun_card_count++] =  get_card_color(temp_card_data[count-1])*16+14;
				flag_card = temp_card_data[count-1];
			}	
		}
		for(int i = 0; i<count;i++)
		{
			
			if(temp_card_data[i] == flag_card)
				continue;
			temp_shu_card[shun_card_count++] = temp_card_data[i];
		}
		int shun_card[]  = new int [GameConstants.OX_MAX_CARD_COUNT];
		first_color = get_card_color(temp_shu_card[0]);
		first_value = get_real_card_value(temp_shu_card[0]);
		int temp_da_count = da_count;
		int temp_xiao_count = xiao_count;	
		shun_card[0] = temp_shu_card[0];
		int value_count = 1;
		
		
		
		for(int i = 1; i< count ; i++)
		{
			if(get_real_card_value(temp_shu_card[i])!=first_value-value_count) line_card = false;
			if(get_real_card_value(temp_shu_card[i])>first_value-value_count &&line_card == false)
				break;
			if(get_real_card_value(temp_shu_card[i])+temp_da_count+temp_xiao_count < first_value-value_count&& line_card == false)
				break;
		    if(temp_xiao_count > 0&&line_card == false)
		    {
		    	temp_xiao_count = 0;
		    	if(get_real_card_value(temp_shu_card[i]) != 0)
		    	{
		    		shun_card[value_count] = 0x4E;
		    		value_count++;
		    	}
		    	line_card = true;
		    	
		    }
		   if(temp_da_count > 0&&line_card == false)
		    {
		    	temp_da_count = 0;
		    	line_card = true;
		    	if(get_real_card_value(temp_shu_card[i]) != 0)
		    	{
		    		shun_card[value_count] = 0x4F;
		    		value_count++;
		    	}
		    		
		    }
		
		    if(get_real_card_value(temp_shu_card[i]) == 0&&line_card ==false)
		    {
		    	line_card = true;
		    }
		    if(line_card == false)
		    	break;
		    shun_card[value_count] = temp_shu_card[i];
		    value_count++;
		}
		for(int i = 1; i < count;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			
			if(same_color == false )
				break;
		}
		if((same_color==true)&&(line_card == true)){
			int shu_count = 0;
			if(get_real_card_value(shun_card[0])+temp_da_count+temp_xiao_count <= 14){
				if(temp_da_count != 0)
				{
					card_date[shu_count++] = 0x4F;
					temp_da_count = 0;
				}
				if(temp_xiao_count != 0)
				{
					card_date[shu_count++] = 0x4E;
					temp_xiao_count = 0;
				}	
			}
			else if(get_real_card_value(shun_card[0])+temp_xiao_count<=14)
			{
				if(temp_xiao_count != 0)
				{
					card_date[shu_count++] = 0x4E;
					temp_xiao_count = 0;
				}
					
			}
			else if(get_real_card_value(shun_card[0])+temp_da_count<=14)
			{
				if(temp_da_count != 0)
				{
					card_date[shu_count++] = 0x4F;
					temp_da_count = 0;
				}
			}
		
			for(int i = 0; i<value_count;i++)
			{
				if(i == 0 && get_real_card_value(shun_card[i]) == 14 )
					card_date[shu_count++] = temp_card_data[GameConstants.OX_MAX_CARD_COUNT-da_count-xiao_count-1];
				else
					card_date[shu_count++] = shun_card[i];
			}
			if(temp_xiao_count + temp_da_count != 0)
			{
				if(temp_xiao_count != 0)
					card_date[shu_count++] = 0x4E;
				else
					card_date[shu_count++] = 0x4F;
			}
			 return true;
		}
		int  king_count=0,ten_count=0;
		int sum = 0;
		int four_count = 0;
		int temp_four_count = 0;
		for(int i=0;i<card_count;i++)
		{
			temp_four_count = 1;
			int temp_real_value = get_real_card_value(card_date[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_date[j])))
					temp_four_count++;
			}
			if(temp_four_count>four_count)
				four_count = temp_four_count;
			if(temp_four_count == 4)
			{
				break;
			}
			
		}
		for(int i=0;i<card_count;i++)
		{
			if(get_real_card_value(card_date[i])>10)
			{
				king_count++;
			}
			else if(get_real_card_value(card_date[i])==10)
			{
				ten_count++;
			}
			if(get_card_value(card_date[i])>=5&&get_card_color(card_date[i])!=4)
				sum  = 11;
			if(get_card_color(card_date[i]) == 4)
				sum +=1;
			else
				sum += get_card_value(card_date[i]);
			
		}
		
		if(four_count+da_count+xiao_count >= 4)
			return true;
	
		
		if(king_count   == GameConstants.OX_MAX_CARD_COUNT)
			return true;
	
			
		if(sum <= 10 && da_count == 1)
			return  true;
		if(sum <= 10 && xiao_count == 1)
			return  true;
		if(sum <= 10)
			return  true;
		
		boolean is_two = false;
		boolean is_three = false;
		int card_real_count = 0;
		for(int i = 0; i< 14;i++)
		{
			if(card_real_value_count[i] ==2)
				is_two = true;
			if(card_real_value_count[i] == 3)
				is_three = true;
			if(card_real_value_count[i] > 0)
				
				card_real_count++;
		}
		if(card_real_count <= 2)
		{
			if(da_count == 1)
				return  true;
			else if(xiao_count == 1)
				return  true;
		}
		if(is_two ==  true&&is_three == true)
		{
			return true;
		}
	
	
	
		if((same_color==true)) 
		{
				return true;
		}
		if((line_card == true))
		{
			int shu_count = 0;
			if(get_real_card_value(shun_card[0])+temp_da_count+temp_xiao_count <= 14){
				if(temp_da_count != 0)
				{
					card_date[shu_count++] = 0x4F;
					temp_da_count = 0;
				}
				if(temp_xiao_count != 0)
				{
					card_date[shu_count++] = 0x4E;
					temp_xiao_count = 0;
				}
				
				
			}
			else if(get_real_card_value(shun_card[0])+temp_xiao_count<=14)
			{
				if(temp_xiao_count != 0)
				{
					card_date[shu_count++] = 0x4E;
					temp_xiao_count = 0;
				}
					
			}
			else if(get_real_card_value(shun_card[0])+temp_da_count<=14)
			{
				if(temp_da_count != 0)
				{
					card_date[shu_count++] = 0x4F;
					temp_da_count = 0;
				}
			}
		
			for(int i = 0; i<value_count;i++)
			{
				if(i == 0 && get_real_card_value(shun_card[i]) == 14&&get_card_color(shun_card[i])!=4 )
					card_date[shu_count++] = temp_card_data[GameConstants.OX_MAX_CARD_COUNT-da_count-xiao_count-1];
				else
					card_date[shu_count++] = shun_card[i];
			}
			if(temp_xiao_count + temp_da_count != 0)
			{
				if(temp_xiao_count != 0)
					card_date[shu_count++] = 0x4E;
				else
					card_date[shu_count++] = 0x4F;
			}
			return true;
		}
	    count = 0;
		for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			
			if(this.get_card_color(card_date[i]) != 4)
				temp_card_data[count++] = card_date[i];
			if(this.get_card_color(card_date[i]) != 4)
				card_real_value_count[get_real_card_value(card_date[i])]++;
		}

		int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		sum=0;

		for (int i=0;i<count;i++)
		{
			temp_value[i]=get_card_value(temp_card_data[i]);
			sum+=temp_value[i];
		}

		if(da_count == 1 && xiao_count == 1)
		{
			card_date[0] = 0x4F;
			card_date[1] = temp_card_data[0];
			card_date[2] = temp_card_data[1];
			card_date[3] = temp_card_data[2];
			card_date[4] = 0x4E;
			return true;
		}
		if(da_count == 1||xiao_count == 1)
		{
			boolean flag = false;
			int value = 0;
			int temp_card_count= 0;
			//查找牛牛
			for (int i=0;i<count;i++)
			{
				if((sum-temp_value[i])%10==0)
				{
					
					for (int k=0;k<count;k++)
					{
						if(i != k)
							card_date[temp_card_count++] = temp_card_data[k];
						
					}

					card_date[temp_card_count++] = temp_card_data[i];
					if(da_count == 1)
						card_date[temp_card_count] = 0x4F;
					if(xiao_count == 1)
						card_date[temp_card_count] = 0x4E;

					return true;
				}
				
			}
			for(int i = 0; i< count - 1; i++)
			{
				
				for(int j = i+1; j<count; j++)
				{
					if((temp_value[i] + temp_value[j])%10 == 0)
					{
						card_date[4] = temp_card_data[i];
						card_date[3] = temp_card_data[j];
						flag = true;
						break;
					}
					if(value < (temp_value[i] + temp_value[j])%10)
					{
						value = (temp_value[i] + temp_value[j])%10;
						card_date[4] = temp_card_data[i];
						card_date[3] = temp_card_data[j];
					}
					
				}
				if(flag == true)
				{
					break;
				}
				
			}
		
			int temp_count = 0;
			for(int j = 0; j< count ;j++)
			{
				if(card_date[3] == temp_card_data[j])
					continue;
				if(card_date[4] == temp_card_data[j])
					continue;
				card_date[temp_count ++] = temp_card_data[j];
			}
			if(da_count == 1)
				card_date[temp_count] = 0x4F;
			else 
				card_date[temp_count] = 0x4E;
			
			
			
			return true;
		}
		
	
		//查找牛牛
		//设置变量
		int temp_card[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		for(int i = 0; i< card_count;i++)
		{
			temp_card[i] = card_date[i];
		}
		 sum=0;
		for (int i=0;i<card_count;i++)
		{
			temp_value[i]=get_card_value(card_date[i]);
			sum+=temp_value[i];
		}
		
		//查找牛牛
		for (int i=0;i<card_count-1;i++)
		{
			for (int j=i+1;j<card_count;j++)
			{
				if((sum-temp_value[i]-temp_value[j])%10==0)
				{
					count=0;
					for (int k=0;k<card_count;k++)
					{
						if(k!=i && k!=j)
						{
							card_date[count++] = card_date[k];
						}
					}

					card_date[count++] = temp_card[i];
					card_date[count++] = temp_card[j];

					return true;
				}
			}
		}
		return false;
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
			int temp_real_value = this.get_real_card_value(card_data[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_data[j])))
					four_count++;
				
			}
			
			if(four_count >= 2)
			{
				return temp_real_value;
			}
		}
		return 0;
	}
	public int  get_hu_lu_value(int card_data[],int card_count)
	{
		int da_count = 0;
		int xiao_count = 0;
		int value = 0;
		int temp_index[] = new int[14];		
		for(int i = 0; i < card_count ; i++)
		{
			if(card_data[i] == 0x4E)
				xiao_count = 1;
			if(card_data[i] == 0x4F)
				da_count = 1;
			if(get_card_color(card_data[i]) == 4)
				continue;
			temp_index[this.get_real_card_value(card_data[i])]++;
		}
		for(int i = 13;i>=0;i--)
		{
			if(temp_index[i]+da_count+xiao_count == 3)
			{
				value = i;
				break;
			}
		}
		return value;
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
	public boolean compare_card(int first_data[], int next_data[], int card_count,boolean first_ox,boolean next_ox
			,int game_rule_index)
	{
		if(first_ox!=next_ox){
			if(first_ox == true)
				return true;
			if(next_ox == true)
				return false;
		}

		//比较牛大小
	
		//获取点数
		int next_type=get_card_type(next_data,card_count,game_rule_index);
		int first_type=get_card_type(first_data,card_count,game_rule_index);


		//点数判断
		if (first_type!=next_type) return (first_type>next_type);

		//排序大小
		int  first_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int  next_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		for(int i = 0; i< card_count;i++){
			first_temp[i] = first_data[i];
			next_temp[i] = next_data[i];
		}

		ox_sort_card_list(first_temp,card_count);
		ox_sort_card_list(next_temp,card_count);

		//比较数值
	

		int next_max_value=get_real_card_value(next_temp[0]);
		int first_max_value=get_real_card_value(first_temp[0]);
		int first_color = get_card_color(first_temp[0]);
		int next_color = get_card_color(next_temp[0]);
		if(next_type == GameConstants.KLOX_BOOM)
		{
			int frist_boom_value = get_boom_value(first_temp,card_count);
			int next_boom_value = get_boom_value(next_temp,card_count);
			if(next_boom_value!=frist_boom_value)return frist_boom_value>next_boom_value;
		}
		if(next_type == GameConstants.KLOX_HU_LU)
		{
			int first_hu_lu_value = get_hu_lu_value(first_data,card_count);
			int next_hu_lu_value = get_hu_lu_value(next_data,card_count);
			if(next_hu_lu_value!=first_hu_lu_value ) return first_hu_lu_value>next_hu_lu_value;
			else{
				int first_da_count = 0;
				int first_xiao_count = 0;
				int first_count = 0;
				int first_temp_color = -3;
				for(int i = 0 ;i < card_count ;i++)
				{
					if(this.get_real_card_value(first_data[i]) == first_hu_lu_value)
					{
						first_count ++;
						if(first_temp_color < get_real_card_value(first_data[i]))
							first_temp_color = get_card_color(first_data[i]);
					}
					if(first_data[i] == 0x4F)
						first_da_count = 1;
					if(first_data[i] == 0x4E)
						first_xiao_count = 1;		
				}
				if(first_count != 3)
				{
					if(first_da_count == 1)
						first_temp_color = -1;
					else if(first_xiao_count == 1)
						first_temp_color = -2;
				}
				int next_da_count = 0;
				int next_xiao_count = 0;
				int next_count = 0;
				int next_temp_color = -3;
				for(int i = 0 ;i < card_count ;i++)
				{
					if(this.get_real_card_value(next_data[i]) == next_hu_lu_value)
					{
						next_count ++;
						if(next_temp_color < get_real_card_value(next_data[i]))
							next_temp_color = get_card_color(next_data[i]);
					}
					if(next_data[i] == 0x4F)
						next_da_count = 1;
					if(next_data[i] == 0x4E)
						next_xiao_count = 1;		
				}
				if(next_count != 3)
				{
					if(next_da_count == 1)
						next_temp_color = -1;
					else if(next_xiao_count == 1)
						next_temp_color = -2;
				}
				return first_temp_color > next_temp_color;
			}
		}
		if(next_type == GameConstants.KLOX_SHUN_ZI
		||next_type == GameConstants.KLOX_THSH)
		{
//			if(next_max_value == 13 && get_real_card_value(next_temp[GameConstants.OX_MAX_CARD_COUNT-1])==1)
//			{
//				next_max_value =  14;
//				next_color = get_card_color(next_temp[GameConstants.OX_MAX_CARD_COUNT-1]);
//			}
//			if(first_max_value == 13 && get_real_card_value(first_temp[GameConstants.OX_MAX_CARD_COUNT-1])==1)
//			{
//				first_max_value =  14;
//				first_color = get_card_color(first_temp[GameConstants.OX_MAX_CARD_COUNT-1]);
//			}
			int first_shun_max_value = 0;
			int next_shun_max_value = 0;
			int first_da_count = 0;
			int first_xiao_count = 0; 
			int next_da_count = 0;
			int next_xiao_count = 0;
			int first_shun_color = 0;
			int next_shun_color = 0;
			int first_index = 0;
			int next_index = 0;
			for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
			{
				if(get_real_card_value(first_data[i]) == 15)
				{
					first_da_count = 1;
					continue;
				}
				else if(get_real_card_value(first_data[i]) ==14)
				{
					first_xiao_count = 1;
					continue;
				}
				first_shun_max_value = get_real_card_value(first_data[i])==1?14:get_real_card_value(first_data[i]);
				first_shun_max_value =  first_shun_max_value+ first_da_count + first_xiao_count;
				if(first_da_count!=0)
					first_shun_color = -1;
				else if(first_xiao_count != 0)
					first_shun_color = -2;
				else first_shun_color = this.get_card_color(first_data[i]);
				first_index = i;
				break;
			}
			for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
			{
				if(get_real_card_value(next_data[i]) == 15)
				{
					next_da_count = 1;
					continue;
				}
				else if(get_real_card_value(next_data[i]) ==14)
				{
					next_xiao_count = 1;
					continue;
				}
				next_shun_max_value = get_real_card_value(next_data[i])==1?14:get_real_card_value(next_data[i]);
				
				next_shun_max_value =  next_shun_max_value+ next_da_count + next_xiao_count;
				if(next_da_count!=0)
					next_shun_color = -1;
				else if(next_xiao_count != 0)
					next_shun_color = -2;
				else next_shun_color = this.get_card_color(next_data[i]);
				next_index = i;
				break;
					
			}
			if(first_shun_max_value!=next_shun_max_value)return first_shun_max_value>next_shun_max_value;
			if(first_shun_color == next_shun_color)
			{
				first_da_count = 0;
				first_xiao_count = 0;
				next_xiao_count = 0;
				next_da_count = 0;
				for(int i = first_index; i< GameConstants.OX_MAX_CARD_COUNT;i++)
				{
					if(get_real_card_value(first_data[i]) == 15)
					{
						first_da_count = 1;
						continue;
					}
					else if(get_real_card_value(first_data[i]) ==14)
					{
						first_xiao_count = 1;
						continue;
					}
					first_shun_max_value = get_real_card_value(first_data[i])+ first_da_count + first_xiao_count;
					if(first_da_count!=0)
						first_shun_color = -1;
					else if(first_xiao_count != 0)
						first_shun_color = -2;
					else first_shun_color = get_card_color(first_data[i]);
					break;
						
				}
				for(int i = next_index; i< GameConstants.OX_MAX_CARD_COUNT;i++)
				{
					if(get_real_card_value(next_data[i]) == 15)
					{
						next_da_count = 1;
						continue;
					}
					else if(get_real_card_value(next_data[i]) ==14)
					{
						next_xiao_count = 1;
						continue;
					}
					next_shun_max_value =  get_real_card_value(next_data[i])+ next_da_count + next_xiao_count;
					if(next_da_count!=0)
						next_shun_color = -1;
					else if(next_xiao_count != 0)
						next_shun_color = -2;
					else next_shun_color = get_card_color(next_data[i]);
					break;
						
				}
				if(first_shun_max_value!=next_shun_max_value)return first_shun_max_value>next_shun_max_value;
			}
			
			return first_shun_color > next_shun_color;
		}
		if(next_max_value!=first_max_value)return first_max_value>next_max_value;	
		if(first_color == next_color)
		{
			next_max_value=get_real_card_value(next_temp[1]);
			first_max_value=get_real_card_value(first_temp[1]);
			first_color = get_card_color(first_temp[1]);
			next_color = get_card_color(next_temp[1]);
			if(next_type == GameConstants.KLOX_SHUN_ZI||next_type == GameConstants.KLOX_DA_SHUN_ZI||next_type == GameConstants.KLOX_XIAO_SHUN_ZI
			||next_type == GameConstants.KLOX_TONG_HUA||next_type == GameConstants.KLOX_DA_TONG_HUA||next_type == GameConstants.KLOX_XIAO_TONG_HUA)
			{
				if(next_max_value == 13 && get_real_card_value(next_temp[GameConstants.OX_MAX_CARD_COUNT-1])==1)
				{
					next_max_value =  14;
					next_color = get_card_color(next_temp[0]);
				}
				if(first_max_value == 13 && get_real_card_value(first_temp[GameConstants.OX_MAX_CARD_COUNT-1])==1)
				{
					first_max_value =  14;
					first_color = get_card_color(first_temp[0]);
				}
			}
			if(next_max_value!=first_max_value)return first_max_value>next_max_value;
			return first_color > next_color;
			
		}
		//比较颜色
		return first_color > next_color;

	}
/////////////////////////////////////////////////////////////////////OX-code/////////////////////////
	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
	}
}
