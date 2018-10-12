/**
 * 
 */
package com.cai.game.nn;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;


public class NNGameLogic {

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
	public int switch_ox(int ox_value)
	{
		switch(ox_value)
		{
		case GameConstants.OX_FIVE_KING_VALUE:
			return GameConstants.OX_FIVE_KING;
		case GameConstants.OX_SHUN_ZI_VALUE:
			return GameConstants.OX_SHUN_ZI;
		case GameConstants.OX_TONG_HUA_VALUE:
			return GameConstants.OX_TONG_HUA;
		case GameConstants.OX_HU_LU_VALUE:
			return GameConstants.OX_HU_LU;
		case GameConstants.OX_BOOM_VALUE:
			return GameConstants.OX_BOOM;
		case GameConstants.OX_WUXIAONIU_VALUE:
			return GameConstants.OX_WUXIAONIU;
		case GameConstants.OX_TONG_HUA_XHUN_VALUE:
			return GameConstants.OX_TONG_HUA_XHUN;
		default:
			return ox_value;
				
		}
	}
    /***
	 * 获取牛牛类型
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_card_type(int card_date[],int card_count, int game_rule_index,int game_type_index){
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
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_TONG_HUA_SHUN)))
		{
			
			if((same_color==true)&&(line_card == true)) return GameConstants.OX_TONG_HUA_XHUN_VALUE;
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
		if(game_type_index == GameConstants.GAME_TYPE_JDOX_YY)
		{
			if(sum <= 10)
				return GameConstants.OX_WUXIAONIU_NIU;
			if(four_count == 4)
				return GameConstants.OX_BOOM_NIU;
			if(king_count==GameConstants.OX_MAX_CARD_COUNT) return GameConstants.OX_KING_NIU;
			else if(king_count==GameConstants.OX_MAX_CARD_COUNT-1 && ten_count==1) return GameConstants.OX_YING_NIU;
			
		}
		else{
			if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_OX_WUXIAONIU))){
				if(sum <= 10)
					return  GameConstants.OX_WUXIAONIU_VALUE;
			}
			if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_OX_BOOM))){
				if(four_count == 4)
					return GameConstants.OX_BOOM_VALUE;
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
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_HU_LU_NIU)))
		{
			if(san_zhang == true && two_zhang == true)
			{
				return GameConstants.OX_HU_LU_VALUE;
			}
		}
			if((same_color==true)&& FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_TONG_HUA_NIU))) return GameConstants.OX_TONG_HUA_VALUE;
			if((line_card == true)&&FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_SHUN_ZI_NIU))) return GameConstants.OX_SHUN_ZI_VALUE;
		
		if(game_type_index != GameConstants.GAME_TYPE_JDOX_YY)
		{
			if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_OX_WUHUANIU))){
				if(king_count==GameConstants.OX_MAX_CARD_COUNT) return GameConstants.OX_FIVE_KING_VALUE;
				
			}
		}
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
	public int get_times_one(int card_date[],int card_count,int game_rule_index,int game_type_index){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,game_rule_index,game_type_index);
		if(times < 7) times = 1;
		else if(times ==7)times = 2;
		else if(times ==8)times = 2;
		else if(times ==9)times = 3;
		else if(times ==10 )times = 4;
		else if(times == GameConstants.OX_SHUN_ZI_VALUE) times = 5;
		else if(times == GameConstants.OX_TONG_HUA_VALUE) times = 6;
		else if(times == GameConstants.OX_HU_LU_VALUE) times = 7;
		else if(times ==GameConstants.OX_FIVE_KING_VALUE)times = 5;
		else if(times ==GameConstants.OX_BOOM_VALUE)times = 8;
		else if(times ==GameConstants.OX_WUXIAONIU_VALUE)times = 8;
		else if(times ==GameConstants.OX_TONG_HUA_XHUN_VALUE)times = 10;
		return times;
	}
	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_mul(int card_date[],int card_count,int game_rule_index,int game_type_index){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,game_rule_index,game_type_index);
		if(times < 8) times = 1;
		else if(times ==8)times = 2;
		else if(times ==9)times = 3;
		else if(times ==10 )times = 4;
		else if(times ==GameConstants.OX_YING_NIU) times = 5;
		else if(times == GameConstants.OX_KING_NIU) times = 6;
		else if(times == GameConstants.OX_BOOM_NIU) times = 8;
		else if(times == GameConstants.OX_WUXIAONIU_NIU) times = 8;
		return times;
	}
	/***
	 * 获取牛牛倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_ping(int card_date[],int card_count,int game_rule_index,int game_type_index){
		int times = 1;
		return times;
	}
	public int get_times_two(int card_date[],int card_count,int game_rule_index,int game_type_index){
		int times = 0;
		if(card_count != GameConstants.OX_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,game_rule_index,game_type_index);
		if(times <= 7) times = 1;
		else if(times ==8)times = 2;
		else if(times ==9)times = 2;
		else if(times ==10 )times = 3;
		else if(times == GameConstants.OX_SHUN_ZI_VALUE) times = 5;
		else if(times == GameConstants.OX_TONG_HUA_VALUE) times = 6;
		else if(times == GameConstants.OX_HU_LU_VALUE) times = 7;
		else if(times ==GameConstants.OX_FIVE_KING_VALUE)times = 5;
		else if(times ==GameConstants.OX_BOOM_VALUE)times = 8;
		else if(times ==GameConstants.OX_WUXIAONIU_VALUE)times = 8;
		else if(times ==GameConstants.OX_TONG_HUA_XHUN_VALUE)times = 10;
		return times;
	}
	/***
	 * 获取牛值
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public boolean get_ox_card(int card_date[],int card_count,int game_rule_index,int game_type_index){
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
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_TONG_HUA_SHUN)))
		{
			if((same_color==true)&&(line_card == true)) return true;
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
		if(game_type_index == GameConstants.GAME_TYPE_JDOX_YY)
		{
			if(sum <= 10)
				return true;
			if(four_count == 4)
				return true;
			if(king_count==GameConstants.OX_MAX_CARD_COUNT) return true;
			else if(king_count==GameConstants.OX_MAX_CARD_COUNT-1 && ten_count==1) return true;
			
		}
		else{
			if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_OX_WUXIAONIU))){
				if(sum <= 10)
					return  true;
			}
			if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_OX_BOOM))){
				if(four_count == 4)
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
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_HU_LU_NIU)))
		{
			if(san_zhang==true && two_zhang == true)
			{
				return true;
			}
		}
		if((same_color==true)&&FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_TONG_HUA_NIU))) return true;
		if((line_card == true)&&FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_SHUN_ZI_NIU))) return true;
		if(game_type_index != GameConstants.GAME_TYPE_JDOX_YY)
		{
			if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_OX_WUHUANIU))){
				if(king_count==GameConstants.OX_MAX_CARD_COUNT) return true;
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
	public boolean compare_card(int first_data[], int next_date[], int card_count,boolean first_ox,boolean next_ox
			,int game_rule_index, int game_type_index)
	{
		if(first_ox!=next_ox){
			if(first_ox == true)
				return true;
			if(next_ox == true)
				return false;
		}

		//比较牛大小
	
		//获取点数
		int next_type=get_card_type(next_date,card_count,game_rule_index,game_type_index);
		int first_type=get_card_type(first_data,card_count,game_rule_index,game_type_index);

		//点数判断
		if (first_type!=next_type) return (first_type>next_type);
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_ZHUANG_WIN))){
			if(get_real_card_value(first_type) < get_real_card_value(next_type))
				return false;
			else
				return true;
				
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

		if(next_type == GameConstants.OX_BOOM_VALUE)
		{
			int frist_boom_value = get_boom_value(first_temp,card_count);
			int next_boom_value = get_boom_value(next_temp,card_count);
			if(next_boom_value!=frist_boom_value)return frist_boom_value>next_boom_value;
		}
		if(next_type == GameConstants.OX_HU_LU_VALUE)
		{
			int first_hu_lu_value = get_hu_lu_value(first_data,card_count);
			int next_hu_lu_value = get_hu_lu_value(next_date,card_count);
			if(next_hu_lu_value!=first_hu_lu_value ) return first_hu_lu_value>next_hu_lu_value;
			
		}

		int next_max_value=get_real_card_value(next_temp[0]);
		int first_max_value=get_real_card_value(first_temp[0]);
		int first_color = get_card_color(first_temp[0]);
		int next_color = get_card_color(next_temp[0]);
		if(next_type == GameConstants.OX_SHUN_ZI_VALUE||next_type == GameConstants.OX_TONG_HUA_XHUN_VALUE)
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
		
		
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_HUA_SE_COMPARE))){
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
