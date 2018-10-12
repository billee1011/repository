/**
 * 
 */
package com.cai.game.huaihuaox;

import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.nn.NNGameLogic;



public class HUAIHUAOXGameLogic {

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
		boolean special =false;
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_real_card_value(temp_card_data[0]);
		if(first_value == 13)
		{
			if(get_real_card_value(temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1])==1)
			{
				temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1] = get_card_color(temp_card_data[GameConstants.OX_MAX_CARD_COUNT-1])*16+9;
				special = true;
			}
			
		}
		for(int i = 1; i < GameConstants.OX_MAX_CARD_COUNT;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			if(get_real_card_value(temp_card_data[i])!=first_value-i) line_card = false;
			if(same_color == false && line_card == false)
				break;
		}
		if(this.getRuleValue(ruleMaps,GameConstants.GAME_RULE_HHUA_THSH) == 1)
		{
			
			if((same_color==true)&&(line_card == true)) return GameConstants.OX_HHUA_TONG_HUA_XHUN_VALUE;
		}

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
		
			if(this.getRuleValue(ruleMaps,GameConstants.GAME_RULE_HHUA_WXOX)==1){
				if(sum <= 10)
					return  GameConstants.OX_HHUA_WUXIAONIU_VALUE;
			}
			if(this.getRuleValue(ruleMaps,GameConstants.GAME_RULE_HHUA_BOOM)==1){
				if(four_count == 4)
					return GameConstants.OX_HHUA_BOOM_VALUE;
			}
		
		
		boolean san_zhang =false;
		boolean two_zhang = false;
		for(int i = 0; i< 14;i++)
		{
			if(card_real_value_count[i] == 3)
				san_zhang = true;
			if(card_real_value_count[i] ==2)
			   two_zhang = true;
		}
		if(this.getRuleValue(ruleMaps,GameConstants.GAME_RULE_HHUA_HULU)==1)
		{
			if(san_zhang == true && two_zhang == true)
			{
				return GameConstants.OX_HHUA_HU_LU_VALUE;
			}
		}
		if(king_count==5&&this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_WHJOX)==1)return GameConstants.OX_HHUA_JIN_VALUE;
		if((same_color==true)&& this.getRuleValue(ruleMaps,GameConstants.GAME_RULE_HHUA_THOX)==1) return GameConstants.OX_HHUA_TONG_HUA_VALUE;
		if(king_count==4&&ten_count==1&&this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_WHYOX)==1)return GameConstants.OX_HHUA_YIN_VALUE;
		if((line_card == true)&&this.getRuleValue(ruleMaps,GameConstants.GAME_RULE_HHUA_SHZI)==1) return GameConstants.OX_HHUA_SHUN_ZI_VALUE;

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
	public int get_times_one(int card_date[],int card_count,Map<Integer, Integer> ruleMaps){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,ruleMaps);
		if(times < 7) times = 1;
		else if(times ==7)times = 2;
		else if(times ==8)times = 2;
		else if(times ==9)times = 3;
		else if(times ==10 )times = 4;
		else if(times == GameConstants.OX_HHUA_SHUN_ZI_VALUE) times = 5;
		else if(times == GameConstants.OX_HHUA_YIN_VALUE) times = 5;
		else if(times == GameConstants.OX_HHUA_TONG_HUA_VALUE) times = 6;
		else if(times == GameConstants.OX_HHUA_JIN_VALUE) times = 6;
		else if(times == GameConstants.OX_HHUA_HU_LU_VALUE) times = 7;
		else if(times ==GameConstants.OX_HHUA_BOOM_VALUE)times = 8;
		else if(times ==GameConstants.OX_HHUA_WUXIAONIU_VALUE)times = 10;
		else if(times ==GameConstants.OX_HHUA_TONG_HUA_XHUN_VALUE)times = 15;
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
		if(times <= 7) times = 1;
		else if(times ==8)times = 2;
		else if(times ==9)times = 2;
		else if(times ==10 )times = 3;
		else if(times == GameConstants.OX_HHUA_SHUN_ZI_VALUE) times = 5;
		else if(times == GameConstants.OX_HHUA_YIN_VALUE) times = 5;
		else if(times == GameConstants.OX_HHUA_TONG_HUA_VALUE) times = 6;
		else if(times == GameConstants.OX_HHUA_JIN_VALUE) times = 6;
		else if(times == GameConstants.OX_HHUA_HU_LU_VALUE) times = 7;
		else if(times ==GameConstants.OX_HHUA_BOOM_VALUE)times = 8;
		else if(times ==GameConstants.OX_HHUA_WUXIAONIU_VALUE)times = 10;
		else if(times ==GameConstants.OX_HHUA_TONG_HUA_XHUN_VALUE)times = 15;
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
		if(getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_THSH)==1)
		{
			if((same_color==true)&&(line_card == true)){
				ox_sort_card_list(card_date,card_count);
				return true;
			}
		}
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
			else if(get_card_value(card_date[i])==10)
			{
				ten_count++;
			}
			if(get_card_value(card_date[i])>=5)
				sum  = 11;
			sum += get_card_value(card_date[i]);
			
		}
	
		if(getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_WXOX)==1){
			if(sum <= 10)
			{
				ox_sort_card_list(card_date,card_count);
				return  true;
			}
		}
		if(getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_BOOM)==1){
			if(four_count == 4)
			{
				ox_sort_card_list(card_date,card_count);
				return true;
			}
		}
		
		boolean san_zhang =false;
		boolean two_zhang = false;
		for(int i = 0; i< 14;i++)
		{
			if(card_real_value_count[i] == 3)
				san_zhang = true;
			if(card_real_value_count[i] ==2)
			   two_zhang = true;
		}
		if(getRuleValue(ruleMaps,  GameConstants.GAME_RULE_HHUA_HULU)==1)
		{
			if(san_zhang==true && two_zhang == true)
			{
				ox_sort_card_list(card_date,card_count);
				return true;
			}
		}
		if(king_count==5&&this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_WHJOX)==1){
			
			ox_sort_card_list(card_date,card_count);
			return true;
		}
		if((same_color==true)&&this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_THOX)==1){ 
			ox_sort_card_list(card_date,card_count);
			return true;
		
		}
		if(king_count==4&&ten_count == 1&&this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_WHYOX)==1){
			ox_sort_card_list(card_date,card_count);
			return true;
		}
		if((line_card == true)&&getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_SHZI)==1){
			ox_sort_card_list(card_date,card_count);
			return true;
		}
		if(this.getRuleValue(ruleMaps,GameConstants.GAME_RULE_OX_WUHUANIU)==1){
			if(king_count==GameConstants.OX_MAX_CARD_COUNT) 
			{
				ox_sort_card_list(card_date,card_count);
				return true;
				
			}
			
		}
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
					int temp_cards[] = new int[2];
					temp_cards[0] = temp_card[i];
					temp_cards[1] = temp_card[j];
					ox_sort_card_list(temp_cards,2);
					ox_sort_card_list(card_date,3);
					card_date[count++] = temp_cards[0];
					card_date[count++] = temp_cards[1];
					return true;
				}
			}
		}
		ox_sort_card_list(card_date,card_count);
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

		int value = 0;	
		for(int i=0;i<card_count;i++)
		{
			int four_count = 1;
			int temp_real_value = this.get_real_card_value(card_data[i]);
			for(int j = 0; j<card_count;j++){
				if((i != j)&&(temp_real_value == get_real_card_value(card_data[j])))
					four_count++;
				
			}
			
			if(four_count > 2)
			{
				return temp_real_value;
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
	public int  compare_card(int first_data[], int next_date[], int card_count,boolean first_ox,boolean next_ox
			,Map<Integer, Integer> ruleMaps)
	{
		if(first_ox!=next_ox){
			if(first_ox == true)
				return 1;
			if(next_ox == true)
				return -1;
		}

		//比较牛大小
	
		//获取点数
		int next_type=get_card_type(next_date,card_count,ruleMaps);
		int first_type=get_card_type(first_data,card_count,ruleMaps);

		//点数判断
		 if (first_type>next_type)
				return 1;
		else if(first_type<next_type)
			return -1;
		if(this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_EQUAL_PING)==1 )
		{
			if(first_type==next_type)
				return 0;
			else if (first_type>next_type)
					return 1;
			else return -1;
		}
		
		
		if(this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_ZHUANG_WIN)==1){
			if(get_real_card_value(first_type) < get_real_card_value(next_type))
				return -1;
			else
				return 1;
				
		}
		

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
		if(next_type == GameConstants.OX_HHUA_BOOM_VALUE)
		{
			int frist_boom_value = get_boom_value(first_temp,card_count);
			int next_boom_value = get_boom_value(next_temp,card_count);
			if(frist_boom_value>next_boom_value)
				return 1;
			else  return -1;
		}
		if(next_type == GameConstants.OX_HHUA_HU_LU_VALUE)
		{
			int first_hu_lu_value = get_hu_lu_value(first_data,card_count);
			int next_hu_lu_value = get_hu_lu_value(next_date,card_count);
			if(first_hu_lu_value>next_hu_lu_value)
				return 1;
			else 
				return -1;
			
		}

		int next_max_value=get_real_card_value(next_temp[0]);
		int first_max_value=get_real_card_value(first_temp[0]);
		int first_color = get_card_color(first_temp[0]);
		int next_color = get_card_color(next_temp[0]);
		if(next_type == GameConstants.OX_HHUA_SHUN_ZI_VALUE||next_type == GameConstants.OX_HHUA_TONG_HUA_XHUN_VALUE)
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
		if(next_max_value>first_max_value)
			return -1;
		else if(next_max_value<first_max_value) return 1;
		
		
		
		if(this.getRuleValue(ruleMaps, GameConstants.GAME_RULE_HHUA_HUA_SE_COMPARE)==1){
			if(first_color > next_color)
				return 1;
			else  return -1;
			
		}

		//比较颜色
		if(first_color > next_color)
			return 1;
		else  return -1;

	}
/////////////////////////////////////////////////////////////////////OX-code/////////////////////////
	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
	}
}
