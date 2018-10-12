/**
 * 
 */
package com.cai.game.yyox;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.cai.game.nn.NNGameLogic;



public class YYOXGameLogic {

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
	public int get_card_type(int card_date[],int card_count, int game_rule_index){
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
		int  king_count=0,ten_count=0;
		int sum = 0;
		int four_count = 0;
		for(int i=0;i<card_count;i++)
		{
			four_count = 1;
			int temp_real_value = get_real_card_value(card_date[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_date[j])))
					four_count++;
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
			if(get_card_value(card_date[i])>=5)
				sum  = 11;
			sum += get_card_value(card_date[i]);
			
		}
		
		if(sum <= 10)
			return  GameConstants.YYOX_WUXIAONIU;
		if(four_count == 4)
			return GameConstants.YYOX_BOOM;
		
	
		if((king_count == 5)) return GameConstants.YYOX_JING_HUA_OX;
		if((king_count == 4)&&(ten_count == 1)) return GameConstants.YYOX_JING_HUA_OX;
		
		int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
		sum=0;
		for (int i=0;i<card_count;i++)
		{
			temp_value[i]=get_card_value(card_date[i]);
			sum+=temp_value[i];
		}

		for (int i=0;i<card_count-1;i++)
		{
			for (int j=i+1;j<card_count;j++)
			{
				if((sum-temp_value[i]-temp_value[j])%10==0)
				{
					return ((temp_value[i]+temp_value[j])>10)?(temp_value[i]+temp_value[j]-10):(temp_value[i]+temp_value[j]);
				}
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
		int times = 1;
		
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
		if(times <=7) times = 1;
		else if(times ==8)times = 2;
		else if(times ==9)times = 3;
		else if(times ==10)times = 4;
		else if(times == GameConstants.YYOX_WUXIAONIU) times = 8;
		else if(times == GameConstants.YYOX_BOOM) times = 7;
		else if(times == GameConstants.YYOX_JING_HUA_OX) times = 6;
		else if(times == GameConstants.YYOX_YING_HUA_OX) times = 5;
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
		for(int i = 0; i< GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			temp_card_data[i] = card_date[i];
			card_real_value_count[get_real_card_value(temp_card_data[i])]++;
		}
		ox_sort_card_list(temp_card_data,card_count);
		int  king_count=0,ten_count=0;
		int sum = 0;
		int four_count = 0;
		int temp_real_value = 0;
		for(int i=0;i<card_count;i++)
		{
			four_count = 1;
			temp_real_value = get_real_card_value(card_date[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_date[j])))
					four_count++;
			
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
			if(get_card_value(card_date[i])>=5)
				sum  = 11;
			sum += get_card_value(card_date[i]);
			
		}
		if(sum <= 10 )
			 return true;
		if(four_count == 4)
			return true;
		if(king_count==GameConstants.OX_MAX_CARD_COUNT ) return true;
		if(king_count==GameConstants.OX_MAX_CARD_COUNT-1&& ten_count == 1) return true;
		
		//查找牛牛
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
		
		//查找牛牛
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
		int next_type=get_card_type(next_date,card_count,game_rule_index);
		int first_type=get_card_type(first_data,card_count,game_rule_index);

		if(next_type != first_type) return first_type>next_type;

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
	

		int next_max_value=get_real_card_value(next_temp[0]);
		int first_max_value=get_real_card_value(first_temp[0]);
		if(next_max_value!=first_max_value)return first_max_value>next_max_value;
		

		//比较颜色
		return get_card_color(first_temp[0]) > get_card_color(next_temp[0]);

	}
/////////////////////////////////////////////////////////////////////OX-code/////////////////////////
	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
	}
}
