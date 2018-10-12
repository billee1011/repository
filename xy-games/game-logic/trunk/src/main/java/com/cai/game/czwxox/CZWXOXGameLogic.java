/**
 * 
 */
package com.cai.game.czwxox;

import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.nn.NNGameLogic;



public class CZWXOXGameLogic {

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
	public int get_card_type(int card_date[],int card_count, Map<Integer, Integer> ruleMaps){
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;

		int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);
		for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			temp_card_data[i] = card_date[i];
			card_real_value_count[get_real_card_value(temp_card_data[i])]++;
		}
		ox_sort_card_list(temp_card_data,card_count);
		boolean same_color = true;
		boolean line_card = true;
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_real_card_value(temp_card_data[0]);
		if(first_value == 13)
		{
			if(get_real_card_value(temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1])==1)
			{
				temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1] = get_card_color(temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1])*16+9;
			}
			
		}
		for(int i = 1; i < GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			if(get_real_card_value(temp_card_data[i])!=first_value-i) line_card = false;
			if(same_color == false && line_card == false)
				break;
		}
		if((line_card == false)&&(first_value == 14))
		{
			line_card = true;
			first_value =  6;
			for(int i = 1; i < GameConstants.OX_MAX_CARD_COUNT;i++)
			{
				if(get_real_card_value(temp_card_data[i])!=first_value-i)
				{
					line_card = false;
					break;
				}
			
			}
		}
		if((same_color==true)&&(line_card == true)&& getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_THSH)==1) return GameConstants.CZWXOX_TONG_HUA_SHUN;
		int  king_count=0,ten_count=0;
		int sum = 0;
		int sum_wdox = 0;
		int four_count = 0;
		int sum_other = 0;
		int record_index = -1;
		for(int i=0;i<card_count;i++)
		{
			four_count = 1;
			int temp_real_value = get_real_card_value(card_date[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_date[j])))
					four_count++;
				
			}
			if(four_count >= 3)
			{
				record_index = i;
			}
			if(four_count == 4)
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
			if(getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_FIVE_WXN)!=1){
				if(get_card_value(card_date[i])>=5)
					sum  = 11;
			}
			sum += get_card_value(card_date[i]);
			sum_wdox += get_card_value(card_date[i]);
			
		}
		
		
		
		
			
		if(sum_wdox>=40&&king_count == 0&&ten_count == 0&& getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_WDL)==1)
			return GameConstants.CZWXOX_WDL;
		if(sum <= 10&& getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_WXL)==1)
			return  GameConstants.CZWXOX_WXL;
		if(four_count == 4&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_BOOM)==1)
			return GameConstants.CZWXOX_BOOM;
		
		boolean is_two = false;
		boolean is_three = false;
		for(int i = 0; i< 14;i++)
		{
			if(card_real_value_count[i] ==2)
				is_two = true;
			if(card_real_value_count[i] == 3)
				is_three = true;
			
		}
	
		if(is_two ==  true&&is_three == true&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_HULU)==1)
		{
			return GameConstants.CZWXOX_HU_LU;
		}
		if((same_color==true)&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_THOX)==1) return GameConstants.CZWXOX_TONG_HUA;
		if((king_count == 5)&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_WHOX)==1) return GameConstants.CZWXOX_WU_HUA_NIU;
		if((line_card == true)&& getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_SHZI)==1) return GameConstants.CZWXOX_SHUN_ZI;
	 
	
		int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		sum=0;
		for (int i=0;i<card_count;i++)
		{
			temp_value[i]=get_card_value(card_date[i]);
			sum+=temp_value[i];
		}
		int count = 0;
		boolean is_frist_sum = false;
		for (int i=0;i<card_count-1;i++)
		{
			boolean flag = false;
			for (int j=i+1;j<card_count;j++)
			{
				if((sum-temp_value[i]-temp_value[j])%10==0)
				{
					count = ((temp_value[i]+temp_value[j])>10)?(temp_value[i]+temp_value[j]-10):(temp_value[i]+temp_value[j]);
					flag = true;
					break;
				}
			}
			if(flag == true)
			{
				is_frist_sum = true;
				break;
			}
		}
		boolean is_next_sum = false;
		if(record_index != -1)
		{	
			int temp_real_value = get_real_card_value(card_date[record_index]);
			int count_index = 0;
			for(int j = 0; j<card_count;j++)
			{
				if(temp_real_value!=get_real_card_value(card_date[j]) || count_index == 3)
					sum_other += get_card_value(card_date[j]);
				else 
					count_index ++;
				if(count_index == 3)
					is_next_sum = true;
			}
			if(sum_other%10 == 0)
				sum_other = 10;
			else
				sum_other %= 10;
		}
		if(count < sum_other &&  getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_STYC)==1&&is_next_sum== true)
		{
			
			return 2*sum_other - 1;
		}
		else if(is_frist_sum == true){
			return 2*count;
		}
		return 0;
	}
	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_one(int card_date[],int card_count,Map<Integer, Integer> ruleMaps){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,ruleMaps);
		if(times < 7*2-1) times = 1;
		else if(times ==7*2||times ==7*2-1)times = 2;
		else if(times ==8*2||times ==8*2-1)times = 2;
		else if(times ==9*2||times ==9*2-1)times = 3;
		else if(times ==10*2||times ==10*2-1)times = 4;
		else if(times ==GameConstants.CZWXOX_TONG_HUA_SHUN) times = 10;
		else if(times == GameConstants.CZWXOX_WDL) times = 8;
		else if(times == GameConstants.CZWXOX_WXL) times = 8;
		else if(times == GameConstants.CZWXOX_BOOM) times = 8;
		else if(times == GameConstants.CZWXOX_HU_LU) times = 7;
		else if(times == GameConstants.CZWXOX_TONG_HUA) times = 6;
		else if(times == GameConstants.CZWXOX_WU_HUA_NIU) times = 5;
		else if(times == GameConstants.CZWXOX_SHUN_ZI) times = 5;
		return times;
	}

	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_two(int card_date[],int card_count,Map<Integer, Integer> ruleMaps){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,ruleMaps);
		if(times <=7*2) times = 1;
		else if(times ==8*2||times ==8*2-1)times = 2;
		else if(times ==9*2||times ==9*2-1)times = 2;
		else if(times ==10*2||times ==10*2-1)times = 3;
		else if(times ==GameConstants.CZWXOX_TONG_HUA_SHUN) times = 10;
		else if(times == GameConstants.CZWXOX_WDL) times = 8;
		else if(times == GameConstants.CZWXOX_WXL) times = 8;
		else if(times == GameConstants.CZWXOX_BOOM) times = 8;
		else if(times == GameConstants.CZWXOX_HU_LU) times = 7;
		else if(times == GameConstants.CZWXOX_TONG_HUA) times = 6;
		else if(times == GameConstants.CZWXOX_WU_HUA_NIU) times = 5;
		else if(times == GameConstants.CZWXOX_SHUN_ZI) times = 5;
		return times;
	}
	/***
	 * 获取牛值
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public boolean get_ox_card(int card_date[],int card_count,Map<Integer, Integer> ruleMaps){
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return false;
		int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);
		for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			temp_card_data[i] = card_date[i];
			card_real_value_count[get_real_card_value(temp_card_data[i])]++;
		}
		ox_sort_card_list(temp_card_data,card_count);
		boolean same_color = true;
		boolean line_card = true;
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_real_card_value(temp_card_data[0]);
		if(first_value == 13)
		{
			if(get_real_card_value(temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1])==1)
			{
				temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1] = get_card_color(temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1])*16+9;
			}
			
		}
		for(int i = 1; i < GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			if(get_real_card_value(temp_card_data[i])!=first_value-i) line_card = false;
			if(same_color == false && line_card == false)
				break;
		}
		if((same_color==true)&&(line_card == true) &&  getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_THSH)==1) return true;
		int  king_count=0,ten_count=0;
		int sum = 0;
		int sum_wdox = 0;
		int four_count = 0;
		int temp_real_value = 0;
		int record_index = -1;
		for(int i=0;i<card_count;i++)
		{
			four_count = 1;
			temp_real_value = get_real_card_value(card_date[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_date[j])))
					four_count++;
			
			}
			if(four_count >= 3)
				record_index = i;
			if(four_count == 4)
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
			if(getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_FIVE_WXN)!=1){
				if(get_card_value(card_date[i])>=5)
					sum  = 11;
			}
			
			sum += get_card_value(card_date[i]);
			sum_wdox += get_card_value(card_date[i]);
			
		}
		
			if(four_count == 4&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_BOOM)==1)
				return true;
		
		
			if(king_count==GameConstants.OX_MAX_CARD_COUNT &&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_WHOX)==1) return true;;
			
		
		
			if(sum <= 10 && getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_WXL)==1)
				 return true;
			if(sum_wdox >= 40 && king_count == 0&&ten_count == 0&& getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_WDL)==1)
				 return true;
		boolean is_two = false;
		boolean is_three = false;
		for(int i = 0; i< 14;i++)
		{
			if(card_real_value_count[i] ==2)
				is_two = true;
			if(card_real_value_count[i] == 3)
				is_three = true;
			
		}
	
		if(is_two ==  true&&is_three == true&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_HULU)==1)
		{
			return true;
		}
		if((same_color==true)&&  getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_THOX)==1) return true;
		if((king_count == 5)&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_WHOX)==1) return true;
		if((line_card == true)&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_SHZI)==1 )return true;
		

		
		
		//设置变量
		int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
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
		int first_card[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int next_card[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		//查找牛牛
		int first_sum = 0;
		boolean is_first_sum = false;
		for (int i=0;i<card_count-1;i++)
		{
			for (int j=i+1;j<card_count;j++)
			{
				if((sum-temp_value[i]-temp_value[j])%10==0)
				{
					int count=0;
					for (int k=0;k<card_count;k++)
					{
						if(k!=i && k!=j)
						{
							first_card[count++] = card_date[k];
						}
					}
					first_card[count++] = temp_card[i];
					first_card[count++] = temp_card[j];
					first_sum = this.get_card_value(temp_card[i]) + this.get_card_value(temp_card[j]); 
					if(first_sum%10 == 0)
						first_sum = 10;
					else
						first_sum %= 10;
					is_first_sum =true;
					break;
					
				}
			}
		}
		int next_sum = 0;
		boolean is_next_sum = false;
		if(record_index != -1&&getRuleValue(ruleMaps,GameConstants.GAME_RULE_WX_STYC)==1)
		{	int count = 0;
			int count_end = GameConstants.OX_MAX_CARD_COUNT-1;
			temp_real_value = get_real_card_value(card_date[record_index]);
			for(int i = 0; i<GameConstants.OX_MAX_CARD_COUNT;i++ )
			{
				if(temp_real_value == get_real_card_value(temp_card[i])&&count != 3)
				{
					next_card[count++] = temp_card[i];
				}
				else
				{
					next_sum += get_card_value(temp_card[i]);
					next_card[count_end--] = temp_card[i];
					if(next_sum%10 == 0)
						next_sum = 10;
					else
						next_sum %= 10;
					
				}
				if(count == 3)
				{
					is_next_sum = true;
				}
			}
		}
		if(next_sum >first_sum && is_next_sum == true)
		{
			for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
				card_date[i] = next_card[i];
			return true;
		}
		else if(is_first_sum == true)
		{
			for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
				card_date[i] = first_card[i];
			return true;
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
			
			if(four_count >2)
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
	public boolean compare_card(int first_data[], int next_date[], int card_count,boolean first_ox,boolean next_ox
			,Map<Integer, Integer> ruleMaps)
	{
		if(first_ox!=next_ox){
			if(first_ox == true)
				return true;
			if(next_ox == true)
				return false;
		}

		//比较牛大小
	
		//获取点数
		int next_type=get_card_type(next_date,card_count,ruleMaps);
		int first_type=get_card_type(first_data,card_count,ruleMaps);

		//点数判断
		if(this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_WX_STYC)==1
				&& this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_WX_STYC_HS) == 1)
		{
			int temp_first_type = first_type;
			int temp_next_type = next_type;
			if(first_type <GameConstants.CZWXOX_SHUN_ZI && first_type > GameConstants.CZWXOX_VALUE0)
				temp_first_type = (first_type +1)/2;
			if(next_type <GameConstants.CZWXOX_SHUN_ZI && next_type > GameConstants.CZWXOX_VALUE0)
				temp_next_type = (next_type +1)/2;
			if(temp_first_type == temp_next_type && first_type <GameConstants.CZWXOX_SHUN_ZI && first_type > GameConstants.CZWXOX_VALUE0)
			{
				//排序大小
				int  first_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
				int  next_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
				for(int i = 0; i< card_count;i++){
					first_temp[i] = first_data[i];
					next_temp[i] = next_date[i];
				}

				ox_sort_card_list(first_temp,card_count);
				ox_sort_card_list(next_temp,card_count);
				int first_color = get_card_color(first_temp[0]);
				int next_color = get_card_color(next_temp[0]);
				int next_max_value=this.get_real_card_value(next_temp[0]);
				int first_max_value=this.get_real_card_value(first_temp[0]);
				if(next_max_value!=first_max_value)return first_max_value>next_max_value;
				return first_color > next_color;
			}
			
		}
		if (first_type!=next_type) return (first_type>next_type);
	
		

		//排序大小
		int  first_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		int  next_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		for(int i = 0; i< card_count;i++){
			first_temp[i] = first_data[i];
			next_temp[i] = next_date[i];
		}

		ox_sort_card_list(first_temp,card_count);
		ox_sort_card_list(next_temp,card_count);

		//比较数值
		if(next_type == GameConstants.CZWXOX_BOOM||next_type == GameConstants.CZWXOX_HU_LU)
		{
			int frist_boom_value = get_boom_value(first_temp,card_count);
			int next_boom_value = get_boom_value(next_temp,card_count);
			if(next_boom_value!=frist_boom_value)return frist_boom_value>next_boom_value;
		}

		int next_max_value=this.get_real_card_value(next_temp[0]);
		int first_max_value=this.get_real_card_value(first_temp[0]);
		
		int first_color = get_card_color(first_temp[0]);
		int next_color = get_card_color(next_temp[0]);
		if(next_type == GameConstants.CZWXOX_SHUN_ZI||next_type == GameConstants.CZWXOX_TONG_HUA_SHUN)
		{
			if(next_max_value == 13 && get_real_card_value(next_temp[GameConstants.OX_MAX_CARD_COUNT-1])==1)
			{
				next_max_value =  14;
				next_color = get_card_color(next_temp[GameConstants.OX_MAX_CARD_COUNT-1]);
			}
			if(first_max_value == 13 && get_real_card_value(first_temp[GameConstants.OX_MAX_CARD_COUNT-1])==1)
			{
				first_max_value =  14;
				first_color = get_card_color(first_temp[GameConstants.OX_MAX_CARD_COUNT-1]);
			}
		}
		if(next_max_value!=first_max_value)return first_max_value>next_max_value;
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
