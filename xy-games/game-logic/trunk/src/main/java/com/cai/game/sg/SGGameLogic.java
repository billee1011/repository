/**
 * 
 */
package com.cai.game.sg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.cai.game.nn.NNGameLogic;



public class SGGameLogic {

private static Logger logger = Logger.getLogger(NNGameLogic.class);

	
	// 获取数值
	public int get_card_value(int card) {
		int card_value =  card & GameConstants.LOGIC_MASK_VALUE;
		return card_value;
	}
	// 获取数值
	public int get_card_point(int card){
		int card_value =  card & GameConstants.LOGIC_MASK_VALUE;
		int card_point = (card_value>=10)?0:card_value;
		return card_point;
	}
	//获取牌点
	public int get_card_list_point(int  card_data[], int card_count)
	{
		//变量定义
		int  card_point=0;
		//获取牌点
		for (int  i=0;i<card_count;i++)
		{
			card_point=(get_card_point(card_data[i])+card_point)%10;
		}
		return card_point;
	}
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

	public String get_card_sg_value(int ox_value)
	{
		String des ="";
		switch(ox_value)
		{
		case 0: 
			des += "零点";
				break;
		case 1:
			 des += "一点";
				break;
		case 2:
			 des += "两点";
				break;
		case 3:
			 des += "三点";
				break;
		case 4:
			 des += "四点";
				break;
		case 5:
			 des += "五点";
				break;
		case 6:
			des += "六点";
				break;
		case 7:
			 des += "七点";
				break;
		case 8:
			 des += "八点";
				break;
		case 9:
			 des += "九点";
				break;
		case GameConstants.SW_HUN_SG:
			des += "混三公";
			break;
		case GameConstants.SW_LEI_SG:
			des += "雷公";
			break;
		case GameConstants.SW_XIAO_SG:
			des += "小三公";
			break;
		case GameConstants.SW_DA_SG:
			des += "大三公";
			break;
		case GameConstants.SW_BS_SG:
			des += "爆三";
			break;
		case GameConstants.SW_TG_SG:
			des += "天公";
			break;
		}
		return des;
	}
    /***
	 * 获取三公类型
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_card_type(int card_data[],int card_count){
		if(card_count != GameConstants.SG_MAX_CARD_COUNT) return 0;
		if(this.is_tian_gong(card_data, card_count))
		{
			return GameConstants.SW_TG_SG;
		}
		else if(this.is_bao_san_gong(card_data, card_count))
		{
			return GameConstants.SW_BS_SG;
		}
		else if(this.is_da_san_gong(card_data, card_count))
		{
			return GameConstants.SW_DA_SG;
		}
		else if(this.is_xiao_san_gong(card_data, card_count))
		{
			return GameConstants.SW_XIAO_SG;
		}
		else if(this.is_lei_gong(card_data, card_count))
		{
			return GameConstants.SW_LEI_SG;
		}
		else if(this.is_hun_san_gong(card_data, card_count))
		{
			return GameConstants.SW_HUN_SG;
		}
		else 
			return this.get_card_list_point(card_data, card_count);
		
	}
    /***
	 * 获取三公类型
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_card_jh_type(int card_data[],int card_count){
		if(card_count != GameConstants.SG_MAX_CARD_COUNT) return 0;
		if(this.is_bao_zi(card_data, card_count))
		{
			return GameConstants.SG_BAO_ZI;
		}
		else if(this.is_tong_hua_shun(card_data, card_count))
		{
			return GameConstants.SG_TONG_HUA_SHUN;
		}
		else if(this.is_tong_hua(card_data, card_count))
		{
			return GameConstants.SG_TONG_HUA;
		}
		else if(this.is_shun_zi(card_data, card_count))
		{
			return GameConstants.SG_SHUN_ZI;
		}
		else if(this.is_dui_zi(card_data, card_count))
		{
			return GameConstants.SG_DUI_ZI;
		}
		else 
			return GameConstants.SG_SAN_PAI;
		
	}
	/***
	 * 获取三公比金花倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_one(int card_data[],int card_count,int game_rule_index){
		int times = 0;
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_SG_NO_TIMES)))
			return 1;
		if(card_count != GameConstants.SG_MAX_CARD_COUNT) return 0;
		times = get_card_jh_type(card_data, card_count);
		if(times == GameConstants.SG_SAN_PAI ) times = 1;
		else if(times == GameConstants.SG_DUI_ZI)times = 2;
		else if(times == GameConstants.SG_SHUN_ZI)times = 3;
		else if(times == GameConstants.SG_TONG_HUA)times = 4;
		else if(times == GameConstants.SG_TONG_HUA_SHUN )times = 5;
		else if(times == GameConstants.SG_BAO_ZI) times = 6;
		return times;
	}

	/***
	 * 获取三公倍数
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public int get_times_two(int card_data[],int card_count,int game_rule_index){
		int times = 0;
		if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_SG_NO_TIMES)))
				return 1;
		if(card_count != GameConstants.SG_MAX_CARD_COUNT) return 0;
		times = get_card_type(card_data, card_count);
		if(times <= 6) times = 1;
		else if(times == 7) times = 2;
		else if(times == 8) times = 3;
		else if(times == 9) times = 4;
		else if(times == GameConstants.SW_HUN_SG) times = 5;
		else if(times == GameConstants.SW_LEI_SG) times = 7;
		else if(times == GameConstants.SW_XIAO_SG) times = 7;
		else if(times == GameConstants.SW_DA_SG) times = 9;
		else if(times == GameConstants.SW_BS_SG) times = 9;
		else if(times == GameConstants.SW_TG_SG) times = 9;
		return times;
	}


    public int get_card_logic_value(int card)
    {
    	int card_value = get_card_value(card);
    	if(card_value == 1)
    		card_value  = 14;
    	if(get_card_color(card) == 4)
    		card_value +=1;
    	return card_value;
		
    }
	/***
	 * 	//排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	//排序
	public void sort_card_list(int  card_data[], int  card_count)
	{
		if(card_count == 0)
			return;

		for(int  i=0; i<card_count-1; i++)
		{
			for(int  j=i+1; j<card_count; j++)
			{
				if(get_card_logic_value(card_data[i]) < get_card_logic_value(card_data[j]))
				{
					//交换位置
					int  cbTemp = card_data[i];
					card_data[i] = card_data[j];
					card_data[j] = cbTemp;
				}
			}
		}
	}
	
	public int calculate_gong_pai_count(int cards_data[], int card_count)
	{
		int count = 0;
		for(int i = 0; i<card_count ;i++ )
		{
			if(this.get_card_value(cards_data[i])>10)
			{
				count ++;
			}
		}
		return count;
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
	public boolean compare_card(int first_data[], int next_data[], int card_count,int game_rule_index)
	{
	
		int first_card[] = new int[3];
		int next_card[] = new int[3];
		Arrays.fill(first_card, 0);
		Arrays.fill(next_card, 0);
		for(int i = 0;  i<card_count;i++)
		{
			first_card[i] = first_data[i];
			next_card[i] = next_data[i];
		}
		this.sort_card_list(first_card, card_count);
		this.sort_card_list(next_card, card_count);
		int first_type = this.get_card_type(first_card, card_count);
		int next_type = this.get_card_type(next_card, card_count);	
		if(first_type == next_type)
		{
			if(FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_SG_DS_TONG_G_WIN)))
			{
				int frist_count  = this.calculate_gong_pai_count(first_card, card_count);
				int next_count = this.calculate_gong_pai_count(next_card, card_count);
				if(frist_count != next_count)
					return frist_count > next_count;
			}
			if(first_type == GameConstants.SW_DA_SG)
			{
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				return first_value>next_value;
			}
			else if(first_type == GameConstants.SW_XIAO_SG) //222>AAA
			{
				int first_value = this.get_card_value(first_card[0]);
				int next_value = this.get_card_value(next_card[0]);
				return first_value>next_value;
			}
			else if(first_type==GameConstants.SW_LEI_SG)
			{
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				return first_value>next_value;
			}
			else if(first_type==GameConstants.SW_HUN_SG)
			{
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				if(first_value==next_value)
				{
					int first_color = this.get_card_color(first_card[0]);
					int next_color = this.get_card_color(next_card[0]);
					return first_color>next_color;
				}
				else 
					return  first_value>next_value;
			}
			else 
			{
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				if(first_value==next_value)
				{
					int first_color = this.get_card_color(first_card[0]);
					int next_color = this.get_card_color(next_card[0]);
					return first_color>next_color;
				}
				else 
					return  first_value>next_value;
			}
		}
		else{
			return first_type > next_type;
		}

	}
	public int get_dui_zi(int card_data[],int card_count)
	{
		int card_value = 0;
		if(card_count != 3)
			return 0;
		int card_index[] = new int[16];
		Arrays.fill(card_index, 0);
		
		for(int  i=0; i<card_count; i++)
		{
			card_index[get_card_value(card_data[i])]++;
			if(card_index[get_card_value(card_data[i])] == 2)
			{
				card_value = card_data[i];
				break;
			}
		}
		return card_value;
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
	public boolean compare_card_jhb(int first_data[], int next_data[], int card_count)
	{
	
		int first_card[] = new int[3];
		int next_card[] = new int[3];
		Arrays.fill(first_card, 0);
		Arrays.fill(next_card, 0);
		for(int i = 0;  i<card_count;i++)
		{
			first_card[i] = first_data[i];
			next_card[i] = next_data[i];
		}
		this.sort_card_list(first_card, card_count);
		this.sort_card_list(next_card, card_count);
		int first_type = this.get_card_jh_type(first_card, card_count);
		int next_type = this.get_card_jh_type(next_card, card_count);
		if(first_type == next_type)
		{
			if(first_type == GameConstants.SG_TONG_HUA_SHUN)
			{
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				if(first_value==next_value)
				{
					int first_color = this.get_card_color(first_card[0]);
					int next_color = this.get_card_color(next_card[0]);
					return first_color>next_color;
				}
				else 
					return  first_value>next_value;
			}
			else if(first_type==GameConstants.SG_TONG_HUA)
			{
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				if(first_value==next_value)
				{
					int first_color = this.get_card_color(first_card[0]);
					int next_color = this.get_card_color(next_card[0]);
					return first_color>next_color;
				}
				else 
					return  first_value>next_value;
			} 
			else if(first_type == GameConstants.SG_DUI_ZI)
			{
				int first_dui_zi = this.get_dui_zi(first_card, card_count);
				int next_dui_zi = this.get_dui_zi(next_card, card_count);
				if(this.get_card_logic_value(first_dui_zi) != this.get_card_logic_value(next_dui_zi))
				{
					return this.get_card_logic_value(first_dui_zi) >this.get_card_logic_value(next_dui_zi);
				}
				
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				if(first_value==next_value)
				{
					int first_color = this.get_card_color(first_card[0]);
					int next_color = this.get_card_color(next_card[0]);
					return first_color>next_color;
				}
				else 
					return  first_value>next_value;
			}
			else
			{
				int first_value = this.get_card_logic_value(first_card[0]);
				int next_value = this.get_card_logic_value(next_card[0]);
				if(first_value==next_value)
				{
					int first_color = this.get_card_color(first_card[0]);
					int next_color = this.get_card_color(next_card[0]);
					return first_color>next_color;
				}
				else 
					return  first_value>next_value;
			}
		}
		else{
			return first_type > next_type;
		}

	}

	//是否是三混公
	public boolean is_hun_san_gong(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		for(int  i=0; i<card_count; i++)
		{
			if(get_card_value(card_data[i]) < 0x0B)
				return false;
		}

		return true;
	}
	//是否是雷公
	public boolean is_lei_gong(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		int wang_count = 0;
		for(int  i=0; i<card_count; i++)
		{
			if(get_card_value(card_data[i]) < 0x0B)
			{
				return false;
			}
			if(get_card_color(card_data[i]) == 4)
			{
				wang_count ++;
			}
				
		}
		if(wang_count == 1)
			return true;
		return false;
	}
	//小三公
	public boolean is_xiao_san_gong(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		int card_value = get_card_value(card_data[0]);
		if(card_value == 3)
			return false;
		if(card_value >10)
			return false;
		int sum_count = 0;
		for(int  i=1; i<card_count; i++)
		{
			if(card_value == get_card_value(card_data[i]))
				sum_count ++;
			
				
		}
		if(sum_count == 2)
			return true;
		return false;
	}
	//大三公
	public boolean is_da_san_gong(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		int card_value = get_card_value(card_data[0]);
		if(card_value <11 || card_value>13)
			return false;
		int sum_count = 0;
		for(int  i=1; i<card_count; i++)
		{
			if(card_value == get_card_value(card_data[i]))
				sum_count ++;				
		}
		if(sum_count == 2)
			return true;
		return false;
	}
	//爆三
	public boolean is_bao_san_gong(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		int card_value = get_card_value(card_data[0]);
		if(card_value != 3)
			return false;
		int sum_count = 0;
		for(int  i=1; i<card_count; i++)
		{
			if(card_value == get_card_value(card_data[i]))
				sum_count ++;				
		}
		if(sum_count == 2)
			return true;
		return false;
	}
	//是否是雷公
	public boolean is_tian_gong(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		int wang_count = 0;
		for(int  i=0; i<card_count; i++)
		{
			if(get_card_value(card_data[i]) < 0x0B)
			{
				return false;
			}
			if(get_card_color(card_data[i]) == 4)
			{
				wang_count ++;
			}
				
		}
		if(wang_count == 2)
			return true;
		return false;
	}
	//豹子
	public boolean is_bao_zi(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		int card_value = get_card_value(card_data[0]);
		int sum_count = 0;
		int card_index[] = new int[16];
		Arrays.fill(card_index, 0);
		
		for(int  i=0; i<card_count; i++)
		{
			card_index[get_card_value(card_data[i])]++;
			if(card_index[get_card_value(card_data[i])] > 1)
				sum_count = card_index[get_card_value(card_data[i])];
		}
		if(sum_count == 3)
			return true;
		return false;
	}
	//同花顺
	public boolean is_tong_hua_shun(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		int temp_card_data[] = new int[GameConstants.SG_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);
		for(int i = 0; i< GameConstants.SG_MAX_CARD_COUNT;i++)
		{
			temp_card_data[i] = card_data[i];
		}
		this.sort_card_list(temp_card_data,card_count);
		boolean same_color = true;
		boolean line_card = true;
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_card_logic_value(temp_card_data[0]);
		for(int i = 1; i < GameConstants.SG_MAX_CARD_COUNT;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			if(get_card_logic_value(temp_card_data[i])!=first_value-i) line_card = false;
			if(same_color == false && line_card == false)
				break;
		}
		if((same_color==true)&&(line_card == true)) return true;
		return false;
	}
	//同花
	public boolean is_tong_hua(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		int temp_card_data[] = new int[GameConstants.SG_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);
		for(int i = 0; i< GameConstants.SG_MAX_CARD_COUNT;i++)
		{
			temp_card_data[i] = card_data[i];
		}
		this.sort_card_list(temp_card_data,card_count);
		boolean same_color = true;
		boolean line_card = true;
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_card_logic_value(temp_card_data[0]);
		for(int i = 1; i < GameConstants.SG_MAX_CARD_COUNT;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			if(get_card_logic_value(temp_card_data[i])!=first_value-i) line_card = false;
			if(same_color == false && line_card == false)
				break;
		}
		if((same_color==true)) return true;
		return false;
	}
	//顺子
	public boolean is_shun_zi(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		int temp_card_data[] = new int[GameConstants.SG_MAX_CARD_COUNT];
		int card_real_value_count[] = new int[14];
		Arrays.fill(card_real_value_count, 0);
		for(int i = 0; i< GameConstants.SG_MAX_CARD_COUNT;i++)
		{
			temp_card_data[i] = card_data[i];
		}
		this.sort_card_list(temp_card_data,card_count);
		boolean same_color = true;
		boolean line_card = true;
		int first_color = get_card_color(temp_card_data[0]);
		int first_value = get_card_logic_value(temp_card_data[0]);
		for(int i = 1; i < GameConstants.SG_MAX_CARD_COUNT;i++)
		{
			if(get_card_color(temp_card_data[i])!=first_color) same_color = false;
			if(get_card_logic_value(temp_card_data[i])!=first_value-i) line_card = false;
			if(same_color == false && line_card == false)
				break;
		}
		if((line_card == true)) return true;
		return false;
	}
	//对子
	public boolean is_dui_zi(int  card_data[], int  card_count)
	{
		if(card_count != 3)
			return false;
		
		int sum_count = 0;
		int card_index[] = new int[16];
		Arrays.fill(card_index, 0);
		
		for(int  i=0; i<card_count; i++)
		{
			card_index[get_card_value(card_data[i])]++;
			if(card_index[get_card_value(card_data[i])] > 1)
				sum_count = card_index[get_card_value(card_data[i])];
		}
		if(sum_count == 2)
			return true;
		return false;
	}
/////////////////////////////////////////////////////////////////////OX-code/////////////////////////
	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 15) && (cbColor <= 1);
	}
}
