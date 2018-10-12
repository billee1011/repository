/**
 * 
 */
package com.cai.game.jdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.cai.game.nn.NNGameLogic;



public class JDBGameLogic {

private static Logger logger = Logger.getLogger(NNGameLogic.class);

	
	// 获取数值
	public int get_card_value(int card) {
		int card_value =  card & GameConstants.LOGIC_MASK_VALUE;
		return card_value>10?10:card_value;
	}
	public int get_real_card_value(int card){
		return  card & GameConstants.LOGIC_MASK_VALUE;
	}
	// 获取数值
	public int get_logic_value(int card) {
		int  card_value =  card & GameConstants.LOGIC_MASK_VALUE;
		int card_color =(card & GameConstants.LOGIC_MASK_COLOR)>> 4;
		if(card_value == 10)
			card_value = 0;
		if(card_color == 4)
			card_value = 5;
		else 
			card_value *= 10;
		return card_value;
	}
	// 获取花色
	public int get_card_color(int card) {
		int card_color = (card & GameConstants.LOGIC_MASK_COLOR)>> 4;
		if(card_color==4)
			return 4;
		return card_color%2;
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
	/**
	 * 
	 */
	public boolean  is_dui_zi(int card_data[],int card_count)
	{
		boolean is_dui_zi = false;
		if(this.get_logic_value(card_data[0]) != this.get_logic_value(card_data[1]))
			return is_dui_zi;
		if(this.get_card_color(card_data[0]) != this.get_card_color(card_data[1]))
			return is_dui_zi;
		is_dui_zi = true;
		return is_dui_zi;
		
	}
    /***
	 * 获取牛牛类型
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_card_type(int card_data[],int card_count, int game_rule_index){
		if(card_count != GameConstants.JDB_CARD_COUNT) return GameConstants.JDB_NULL;

		if(is_dui_zi(card_data,card_count))
		{
			return  GameConstants.JDB_DUI_ZI; 
			
		}
		int first_dot = this.get_logic_value(card_data[0]);
		int next_dot = this.get_logic_value(card_data[1]);
		switch((first_dot+next_dot)%100)
		{
		case 0:
			return GameConstants.JDB_ZERO;
		case 5:
			return GameConstants.JDB_HALF;
		case 10:
			return GameConstants.JDB_ONE;
		case 15:
			return GameConstants.JDB_ONE_HALF;
		case 20:
			return GameConstants.JDB_TWO;
		case 25:
			return GameConstants.JDB_TWO_HALF;
		case 30:
			return GameConstants.JDB_THREE;
		case 35:
			return GameConstants.JDB_THREE_HALF;
		case 40:
			return GameConstants.JDB_FOUR;
		case 45:
			return GameConstants.JDB_FOUR_HALF;
		case 50:
			return GameConstants.JDB_FIVE;
		case 55:
			return GameConstants.JDB_FIVE_HALF;
		case 60:
			return GameConstants.JDB_SIX;
		case 65:
			return GameConstants.JDB_SIX_HALF;
		case 70:
			return GameConstants.JDB_SEVEN;
		case 75:
			return GameConstants.JDB_SEVEN_HALF;
		case 80:
			return GameConstants.JDB_EIGHT;
		case 85:
			return GameConstants.JDB_EIGHT_HALF;
		case 90:
			return GameConstants.JDB_NINE;
		case 95:
			return GameConstants.JDB_NINE_HALF;
			
		
		}
		
		return GameConstants.JDB_NULL;
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
		if(card_count != GameConstants.JDB_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,game_rule_index);
		if(times < 7) times = 1;
		else if(times ==7)times = 2;
		else if(times ==8)times = 2;
		else if(times ==9)times = 2;
		else if(times ==10 )times = 3;
		else times = 5;
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
		if(card_count != GameConstants.JDB_CARD_COUNT) return 0;
		times = get_card_type(card_date, card_count,game_rule_index);
		if(times <= 1) times = 1;
		else if(times ==2 ) times = 2;
		else if(times == 3) times = 3;
		else if(times == 4) times = 4;
		else if(times == 5) times = 5;
		else if(times == 6) times = 6;
		else if(times == 7) times = 7;
		else if(times == 8) times = 8;
		else if(times == 9) times = 9;
		else if(times == 10) times = 10;
		else if(times == GameConstants.FKN_SHUNZI) times = 15;
		else if(times == GameConstants.FKN_TONGHUA) times = 16;
		else if(times == GameConstants.FKN_HULU) times = 17;
		else if(times == GameConstants.FKN_WUXIAONIU) times = 18;
		else if(times == GameConstants.FKN_WUHUANIU) times = 19;
		else if(times == GameConstants.FKN_BOOM)  times = 20;
		else if(times == GameConstants.FKN_TONGHUASHUN) times = 25;
		return times;
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
		if(card_count != GameConstants.JDB_CARD_COUNT) return false;
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
	//对比扑克  0算赢 1算输2算平
	public int  compare_card(int first_data[], int next_date[], int card_count ,int game_rule_index)
	{
		
		//比较牛大小
	
		//获取点数
		int next_type=get_card_type(next_date,card_count,game_rule_index);
		int first_type=get_card_type(first_data,card_count,game_rule_index);

		if(next_type == first_type)
			return GameConstants.JDB_CALCULATE_PING;
		if(first_type > next_type)
			return GameConstants.JDB_CALCULATE_WIN;
		else 
			return GameConstants.JDB_CALCULATE_LOST;
		

		//比较颜色

	}
/////////////////////////////////////////////////////////////////////OX-code/////////////////////////
	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
	}
}
