/**
 * 
 */
package com.cai.game.phz;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.phz.data.AnalyseItem;
import com.cai.game.phz.data.LouWeaveItem;
import com.cai.game.phz.handler.yiyangwhz.YiYangWHZTable;
import com.google.common.collect.Lists;



public class PHZGameLogicYIYANG extends PHZGameLogic{

	private static Logger logger = Logger.getLogger(PHZGameLogicYIYANG.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	public PHZGameLogicYIYANG() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_HH_COUNT];
		_lai_gen = 0;
		_ding_gui = 0;
	}

	public void clean_magic_cards() {
		_magic_card_count = 0;
	}

	// 获取数值
	public int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public void add_magic_card_index(int index) {
		_magic_card_index[_magic_card_count] = index;
		_magic_card_count++;
	}

	public void add_lai_gen_card(int card) {
		_lai_gen = card;
	}

	public void add_ding_gui_card(int card) {
		_ding_gui = card;
	}

	public boolean is_magic_card(int card) {
		for (int i = 0; i < _magic_card_count; i++) {
			if (_magic_card_index[i] == switch_to_card_index(card)) {
				return true;
			}
		}
		return false;
	}

	public boolean is_magic_index(int index) {
		for (int i = 0; i < _magic_card_count; i++) {
			if (_magic_card_index[i] == index) {
				return true;
			}
		}
		return false;
	}

	public boolean is_lai_gen_card(int card) {
		if (_lai_gen == card) {
			return true;
		}
		return false;
	}

	public boolean is_ding_gui_card(int card) {
		if (_ding_gui == card) {
			return true;
		}
		return false;
	}

	public int magic_count(int cards_index[]) {
		int count = 0;
		for (int i = 0; i < _magic_card_count; i++) {
			count += cards_index[_magic_card_index[i]];
		}
		return count;
	}

	public int get_magic_card_index(int index) {
		// m_cbMagicIndex
		return _magic_card_index[index];// MJGameConstants.MAX_HH_INDEX;
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
			bPosition = (int) (RandomUtil.getRandomNumber() % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);

	}

	// 删除扑克 by data
	public boolean remove_card_by_data(int cards[], int card_data) {
		int card_count = cards.length;

		if (card_count == 0) {
			return false;
		}

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.MAX_HH_COUNT];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < card_count; i++) {
			if (card_data == cbTempCardData[i]) {
				cbDeleteCount++;
				cbTempCardData[i] = 0;
				break;
			}
		}

		// 成功判断
		if (cbDeleteCount != 1) {
			return false;
		}

		// 清理扑克
		for (int i = 0; i < card_count; i++) {
			cards[i] = 0;
		}
		int cbCardPos = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;

	}

	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.MAX_HH_COUNT];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < remove_count; i++) {
			for (int j = 0; j < card_count; j++) {
				if (remove_cards[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}

		// 成功判断
		if (cbDeleteCount != remove_count) {
			return false;
		}

		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

	// 牌数数目
	public int get_card_count_by_index(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}

	/***
	 * 删除扑克 索引
	 * 
	 * @param cards_index
	 * @param card
	 * @return
	 */
	public boolean remove_card_by_index(int cards_index[], int card) {
		// 效验扑克
		int card_index = switch_to_card_index(card);
		if (card_index < 0 || card_index >= GameConstants.MAX_HH_INDEX) {
			return false;
		}

		if (cards_index[card_index] == 0) {
			return false;
		}

		// 删除扑克
		cards_index[card_index]--;
		return true;
	}

	// 删除扑克
	public boolean remove_cards_by_index(int cards_index[], int cards[], int card_count) {
		// 删除扑克
		for (int i = 0; i < card_count; i++) {
			if (remove_card_by_index(cards_index, cards[i]) == false) {
				// 还原删除
				for (int j = 0; j < i; j++) {
					cards_index[j]++;
				}
				return false;
			}
		}

		return true;
	}

	//吃牌判断
	public int get_action_chi_card(int cards_index[],int cur_card ,ChiCardInfo chi_card_info[])
	{
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		//变量定义
		int chi_card_count=0;
		int cur_index=switch_to_card_index(cur_card);

		//三牌判断
		if (cards_index[cur_index]>=3) return chi_card_count;

		//大小搭吃
		int reverse_index=(cur_index+10)%GameConstants.MAX_HH_INDEX;
		if ((cards_index[cur_index]>=1)&&(cards_index[reverse_index]>=1)&&(cards_index[reverse_index]<3))
		{
			//构造扑克
			int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
			for(int i = 0; i<GameConstants.MAX_HH_INDEX;i++)
			{
				temp_card_index[i] = cards_index[i];
			}

			//删除扑克
			temp_card_index[cur_index]--;
			temp_card_index[reverse_index]--;

			//提取判断
			int  cbResultCount=1;
			while (temp_card_index[cur_index]>0)
			{
				int result[]=chi_card_info[chi_card_count].cbCardData[cbResultCount];
				if (take_out_chi_card(temp_card_index,cur_card,result)!=GameConstants.WIK_NULL) cbResultCount++;
				else break;
			}

			//设置结果
			if (temp_card_index[cur_index]==0)
			{
				chi_card_info[chi_card_count].cbCenterCard=cur_card;
				chi_card_info[chi_card_count].cbResultCount=cbResultCount;
				chi_card_info[chi_card_count].cbCardData[0][0]=cur_card;
				chi_card_info[chi_card_count].cbCardData[0][1]=cur_card;
				chi_card_info[chi_card_count].cbCardData[0][2]=switch_to_card_data(reverse_index);
				chi_card_info[chi_card_count++].cbChiKind=(get_card_color(cur_card)==0x00)?GameConstants.WIK_XXD:GameConstants.WIK_DDX;
			}
		}

		//大小搭吃
		if (cards_index[reverse_index]==2)
		{
			//构造扑克
			int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
			for(int i = 0; i<GameConstants.MAX_HH_INDEX;i++)
			{
				temp_card_index[i] = cards_index[i];
			}

			//删除扑克
			temp_card_index[reverse_index]-=2;

			//提取判断
			int cbResultCount=1;
			while (temp_card_index[cur_index]>0)
			{
				int result[]=chi_card_info[chi_card_count].cbCardData[cbResultCount];
				if (take_out_chi_card(temp_card_index,cur_card,result)!=GameConstants.WIK_NULL) cbResultCount++;
				else break;
			}

			//设置结果
			if (temp_card_index[cur_index]==0)
			{
				chi_card_info[chi_card_count].cbCenterCard=cur_card;
				chi_card_info[chi_card_count].cbResultCount=cbResultCount;
				chi_card_info[chi_card_count].cbCardData[0][0]=cur_card;
				chi_card_info[chi_card_count].cbCardData[0][1]=switch_to_card_data(reverse_index);
				chi_card_info[chi_card_count].cbCardData[0][2]=switch_to_card_data(reverse_index);
				chi_card_info[chi_card_count++].cbChiKind=(get_card_color(cur_card)==0x00)?GameConstants.WIK_DDX:GameConstants.WIK_XXD;
			}
		}

		//二七十吃
		int card_value=get_card_value(cur_card);
		if ((card_value==0x02)||(card_value==0x07)||(card_value==0x0A))
		{
			//变量定义
			int excursion[]={1,6,9};
			int incept_index=(get_card_color(cur_card)==0x00)?0:10;

			//类型判断
			int i = 0;
			for (;i<excursion.length;i++)
			{
				int temp_index=incept_index+excursion[i];
				if ((temp_index!=cur_index)&&((cards_index[cur_index]==0)||(cards_index[cur_index]==3)||(cards_index[cur_index]==4))) break;
			}

			//提取判断
			if (i==excursion.length)
			{
				//构造扑克
				int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
				for(int  j = 0; i<GameConstants.MAX_HH_INDEX;j++)
				{
					temp_card_index[j] = cards_index[j];
				}

				//删除扑克
				for (int j=0;j<excursion.length;j++)
				{
					int  index=incept_index+excursion[j];
					if (index!=cur_index) temp_card_index[index]--;
				}

				//提取判断
				int cbResultCount=1;
				while (temp_card_index[cur_index]>0)
				{
					int result[]=chi_card_info[chi_card_count].cbCardData[cbResultCount];
					if (take_out_chi_card(temp_card_index,cur_card,result)!=GameConstants.WIK_EQS) cbResultCount++;
					else break;
				}

				//设置结果
				if (temp_card_index[cur_index]==0)
				{
					chi_card_info[chi_card_count].cbChiKind=GameConstants.WIK_EQS;
					chi_card_info[chi_card_count].cbCenterCard=cur_card;
					chi_card_info[chi_card_count].cbResultCount=cbResultCount;
					chi_card_info[chi_card_count].cbCardData[0][0]=switch_to_card_data(incept_index+excursion[0]);
					chi_card_info[chi_card_count].cbCardData[0][1]=switch_to_card_data(incept_index+excursion[1]);
					chi_card_info[chi_card_count++].cbCardData[0][2]=switch_to_card_data(incept_index+excursion[2]);
				}
			}
		}

		//顺子类型
		int excursion[]={0,1,2};
		for (int i=0;i<excursion.length;i++)
		{
			int value_index=cur_index%10;
			if ((value_index>=excursion[i])&&((value_index-excursion[i])<=7))
			{
				//索引定义
				int first_index=cur_index-excursion[i];

				//吃牌判断
				int j=0;
				for (;j<3;j++)
				{
					int cbIndex=first_index+j;
					if ((cbIndex!=cur_index)&&((cards_index[cbIndex]==0)||(cards_index[cbIndex]==3)||(cards_index[cbIndex]==4))) break;
				}

				//提取判断
				if (j==excursion.length)
				{
					//构造扑克
					int temp_card_index[] = new int[GameConstants.MAX_HH_INDEX];
					for(int  k = 0; i<GameConstants.MAX_HH_INDEX;k++)
					{
						temp_card_index[k] = cards_index[k];
					}

					//删除扑克
					for (int  k=0;k<3;k++)
					{
						int  temp_index=first_index+k;
						if (temp_index!=cur_index) temp_card_index[temp_index]--;
					}

					//提取判断
					int cbResultCount=1;
					while (temp_card_index[cur_index]>0)
					{
						int result[]=chi_card_info[chi_card_count].cbCardData[cbResultCount];
						if (take_out_chi_card(temp_card_index,cur_card,result)!=GameConstants.WIK_NULL) cbResultCount++;
						else break;
					}

					//设置结果
					if (temp_card_index[cur_index]==0)
					{
						int cbChiKind[]={GameConstants.WIK_LEFT,GameConstants.WIK_CENTER,GameConstants.WIK_RIGHT};
						chi_card_info[chi_card_count].cbChiKind=cbChiKind[i];
						chi_card_info[chi_card_count].cbCenterCard=cur_card;
						chi_card_info[chi_card_count].cbResultCount=cbResultCount;
						chi_card_info[chi_card_count].cbCardData[0][0]=switch_to_card_data(first_index);
						chi_card_info[chi_card_count].cbCardData[0][1]=switch_to_card_data(first_index+1);
						chi_card_info[chi_card_count++].cbCardData[0][2]=switch_to_card_data(first_index+2);
					}
				}
			}
		}

		return chi_card_count;
	}
	// 吃牌判断
	public int take_out_chi_card(int cards_index[], int cur_card,int  result_card[]) {
    	// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		//变量定义
		int first_index=0;
		int cur_index=switch_to_card_data(cur_card);

		//大小搭吃
		int reverse_index=(cur_index+10)%GameConstants.MAX_HH_INDEX;
		if ((cards_index[cur_index]>=2)&&(cards_index[reverse_index]>=1)&&(cards_index[reverse_index]<3))
		{
			//删除扑克
			cards_index[cur_index]--;
			cards_index[cur_index]--;
			cards_index[reverse_index]--;

			//设置结果
			result_card[0]=cur_card;
			result_card[1]=cur_card;
			result_card[2]=switch_to_card_data(reverse_index);

			return (get_card_color(cur_card)==0x00)?GameConstants.WIK_XXD:GameConstants.WIK_DDX;
		}

		//大小搭吃
		if (cards_index[reverse_index]==2)
		{
			//删除扑克
			cards_index[cur_index]--;
			cards_index[reverse_index]-=2;

			//设置结果
			result_card[0]=cur_card;
			result_card[1]=switch_to_card_data(reverse_index);
			result_card[2]=switch_to_card_data(reverse_index);

			return (get_card_color(cur_card)==0x00)?GameConstants.WIK_DDX:GameConstants.WIK_XXD;
		}

		//二七十吃
		int card_value=get_card_value(cur_card);
		if ((card_value==0x02)||(card_value==0x07)||(card_value==0x0A))
		{
			//变量定义
			int excursion[]={1,6,9};
			int incept_index=(get_card_color(cur_card)==0x00)?0:10;

			//类型判断
			int i = 0;
			for (;i<excursion.length;i++)
			{
				int temp_index=incept_index+excursion[i];
				if ((cards_index[temp_index]==0)||(cards_index[temp_index]==3)||(cards_index[temp_index]==4)) break;
			}

			//成功判断
			if (i==excursion.length)
			{
				//删除扑克
				cards_index[incept_index+excursion[0]]--;
				cards_index[incept_index+excursion[1]]--;
				cards_index[incept_index+excursion[2]]--;

				//设置结果
				result_card[0]=switch_to_card_data(incept_index+excursion[0]);
				result_card[1]=switch_to_card_data(incept_index+excursion[0]);
				result_card[2]=switch_to_card_data(incept_index+excursion[0]);

				return GameConstants.WIK_EQS;
			}
		}

		//顺子判断
		int excursion[]={0,1,2};
		for (int i=0;i<excursion.length;i++)
		{
			int cbValueIndex=cur_index%10;
			if ((cbValueIndex>=excursion[i])&&((cbValueIndex-excursion[i])<=7))
			{
				//吃牌判断
				first_index=cur_index-excursion[i];
				if ((cards_index[first_index]==0)||(cards_index[first_index]==3)||(cards_index[first_index]==4)) continue;
				if ((cards_index[first_index+1]==0)||(cards_index[first_index+1]==3)||(cards_index[first_index+1]==4)) continue;
				if ((cards_index[first_index+2]==0)||(cards_index[first_index+2]==3)||(cards_index[first_index+2]==4)) continue;

				//删除扑克
				cards_index[first_index]--;
				cards_index[first_index+1]--;
				cards_index[first_index+2]--;

				//设置结果
				result_card[0]=switch_to_card_data(first_index);
				result_card[1]=switch_to_card_data(first_index+1);
				result_card[2]=switch_to_card_data(first_index+2);

				int chi_kind[]={GameConstants.WIK_LEFT,GameConstants.WIK_CENTER,GameConstants.WIK_RIGHT};
				return chi_kind[i];
			}
		}

		return GameConstants.WIK_NULL;
	}
	public int  check_chi_phz(int cards_index[], int cur_card,int type_count[],int type_eat_count[],boolean yws_type){
    	int eat_type = 0;
    	int count = 0;
    	int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		//三牌判断 
		if(cards_index[cur_card_index]>=3)
			return eat_type; 
		//大小搭吃
		int  reverse_index = (cur_card_index+10)%GameConstants.MAX_HH_INDEX;
		if((cards_index[cur_card_index]>=1)&&(cards_index[reverse_index]>=1)&&(cards_index[reverse_index] < 3)){
			
			
			int action = (get_card_color(cur_card) == 0x00)?GameConstants.WIK_XXD:GameConstants.WIK_DDX;
			eat_type |=  action;
			type_count[count++] = action;
		}
		//大小搭吃
		if(cards_index[reverse_index] == 2){
			
			int action = (get_card_color(cur_card) == 0x00)?GameConstants.WIK_DDX:GameConstants.WIK_XXD;
			eat_type |=  action;
			type_count[count++] = action; 
		}
		//二七十吃
		int card_value = get_card_value(cur_card);
		if((card_value == 2)||(card_value == 7)||(card_value == 10)){
			int excursion[] = {1,6,9};
			int acceptIndex = (get_card_color(cur_card)==0)?0:10;
			int i = 0;
			for(; i<excursion.length;i++)
			{
				int index = acceptIndex + excursion[i];
				if((index != cur_card_index)&&((cards_index[index] == 0)||(cards_index[index] >= 3)))break;
			}
			if(i==excursion.length)
			{
 
				eat_type |= GameConstants.WIK_EQS;	
				type_count[count++] = GameConstants.WIK_EQS;
			}	
		}
		//一五十吃
		if(yws_type == true){
			if((card_value == 1)||(card_value == 5)||(card_value == 10)){
				int excursion[] = {0,4,9};
				int acceptIndex = (get_card_color(cur_card)==0)?0:10;
				int i = 0;
				for(; i<excursion.length;i++)
				{
					int index = acceptIndex + excursion[i];
					if((index != cur_card_index)&&((cards_index[index] == 0)||(cards_index[index] >= 3)))break;
				}
				if(i==excursion.length)
				{
	 
					eat_type |= GameConstants.WIK_YWS;	
					type_count[count++] = GameConstants.WIK_YWS;
				}	
			}
		}
		//顺子吃
		
		int excursion[] = {0,1,2};

		
		for(int i = 0; i<excursion.length;i++)
		{
			int value_index = cur_card_index%10;
			if((value_index >=excursion[i])&&(value_index - excursion[i] <=7))
			{
				int first_index  = cur_card_index - excursion[i];
				if((cur_card_index !=first_index )&&((cards_index[first_index] == 0)||(cards_index[first_index] == 3)||(cards_index[first_index] == 4))) continue;
				if((cur_card_index !=first_index+1 )&&((cards_index[first_index+1] == 0)||(cards_index[first_index+1] == 3)||(cards_index[first_index+1] == 4))) continue;
				if((cur_card_index !=first_index+2 )&&((cards_index[first_index+2] == 0)||(cards_index[first_index+2] == 3)||(cards_index[first_index+2] == 4))) continue;

				int chi_kind[] = {GameConstants.WIK_LEFT,GameConstants.WIK_CENTER,GameConstants.WIK_RIGHT};
				eat_type |= chi_kind[i];	
				type_count[count++] = chi_kind[i];
			}
		}
			
		type_eat_count[0] = count;
    	return eat_type ;
    	
	}
	public int  check_chi(int cards_index[], int cur_card,int type_count[],int type_eat_count[],List<can_chi_index> can_not_chi){
    	int eat_type = 0;
    	int count = 0;
    	int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引

		//二七十吃
		int card_value = get_card_value(cur_card);
		if((card_value == 2)||(card_value == 7)||(card_value == 10)){
			int excursion[] = {1,6,9};
			int acceptIndex = (get_card_color(cur_card)==0)?0:10;
			
			boolean is_can_not_chi=false;
			if(can_not_chi !=null ){
				for(int i=0;i<can_not_chi.size();i++){
					can_chi_index object=can_not_chi.get(i);
					int number=0;
					for(int j=0; j<excursion.length;j++)
					{
						int index = acceptIndex + excursion[j];
						if(index != cur_card_index){
							if(object._one_index == index || object._two_index == index && object._is_pass){
								number++;
							}
						}
					}
					if(number == 2){
						is_can_not_chi=true;
					}
					if(is_can_not_chi){
						break;
					}	
				}
			}

			if(!is_can_not_chi){
				int i = 0;
				for(; i<excursion.length;i++)
				{
					int index = acceptIndex + excursion[i];
					if((index != cur_card_index)&&((cards_index[index] == 0)))break;
				}
				if(i==excursion.length)
				{
	 
					eat_type |= GameConstants.WIK_EQS;	
					type_count[count++] = GameConstants.WIK_EQS;

				}	
			}
			
		
		}
		//顺子吃
		
		int excursion[] = {0,1,2};


		


		for(int i = 0; i<excursion.length;i++)
		{
			int value_index = cur_card_index%10;
			if((value_index >=excursion[i])&&(value_index - excursion[i] <=7))
			{
				int first_index  = cur_card_index - excursion[i];
				if((cur_card_index !=first_index )&&((cards_index[first_index] == 0))) continue;
				if((cur_card_index !=first_index+1 )&&((cards_index[first_index+1] == 0))) continue;
				if((cur_card_index !=first_index+2 )&&((cards_index[first_index+2] == 0))) continue;
				boolean is_can_not_chi=false;
				if(can_not_chi !=null ){
					for(int can_not_index=0;can_not_index<can_not_chi.size();can_not_index++){
						can_chi_index object=can_not_chi.get(can_not_index);
						if(first_index == cur_card_index-2){
							if(object._one_index == cur_card_index-2 && object._two_index == cur_card_index-1 && object._is_pass){
								is_can_not_chi=true;
							}
						}
						if(first_index == cur_card_index-1){
							if(object._one_index == cur_card_index-1 && object._two_index == cur_card_index+1 && object._is_pass){
								is_can_not_chi=true;
							}
						}
						if(first_index == cur_card_index){
							if(object._one_index == cur_card_index+1 && object._two_index == cur_card_index+2 && object._is_pass){
								is_can_not_chi=true;
							}
						}
						if(is_can_not_chi){
							break;
						}	
					}
				}
				if(!is_can_not_chi){
					int chi_kind[] = {GameConstants.WIK_LEFT,GameConstants.WIK_CENTER,GameConstants.WIK_RIGHT};
					eat_type |= chi_kind[i];	
					type_count[count++] = chi_kind[i];
				}

			}	
			
		}


		type_eat_count[0] = count;
    	return eat_type ;
    	
	}
	
	public int  check_chi_YIYANG(int cards_index[], int cur_card,int type_count[],int type_eat_count[],List<can_chi_index> can_not_chi){
    	int eat_type = 0;
    	int count = 0;
    	int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引

		//二七十吃
		int card_value = get_card_value(cur_card);
		if((card_value == 2)||(card_value == 7)||(card_value == 10)){
			int excursion[] = {1,6,9};
			int acceptIndex = (get_card_color(cur_card)==0)?0:10;
			
			boolean is_can_not_chi=false;
			if(can_not_chi !=null ){
				for(int i=0;i<can_not_chi.size();i++){
					can_chi_index object=can_not_chi.get(i);
					int number=0;
					for(int j=0; j<excursion.length;j++)
					{
						int index = acceptIndex + excursion[j];
						if(index != cur_card_index){
							if((object._one_index == index || object._two_index == index)){
								number++;
							}
						}
					}
					if(number == 2){
						is_can_not_chi=true;
					}
					if(is_can_not_chi){
						break;
					}	
				}
			}

			if(!is_can_not_chi){
				int i = 0;
				for(; i<excursion.length;i++)
				{
					int index = acceptIndex + excursion[i];
					if((index != cur_card_index)&&((cards_index[index] == 0)))break;
				}
				if(i==excursion.length)
				{
	 
					eat_type |= GameConstants.WIK_EQS;	
					type_count[count++] = GameConstants.WIK_EQS;

				}	
			}
			
		
		}
		//顺子吃
		
		int excursion[] = {0,1,2};


		


		for(int i = 0; i<excursion.length;i++)
		{
			int value_index = cur_card_index%10;
			if((value_index >=excursion[i])&&(value_index - excursion[i] <=7))
			{
				int first_index  = cur_card_index - excursion[i];
				if((cur_card_index !=first_index )&&((cards_index[first_index] == 0))) continue;
				if((cur_card_index !=first_index+1 )&&((cards_index[first_index+1] == 0))) continue;
				if((cur_card_index !=first_index+2 )&&((cards_index[first_index+2] == 0))) continue;
				boolean is_can_not_chi=false;
				if(can_not_chi !=null ){
					for(int can_not_index=0;can_not_index<can_not_chi.size();can_not_index++){
						can_chi_index object=can_not_chi.get(can_not_index);
						if(first_index == cur_card_index-2){
							if(object._one_index == cur_card_index-2 && object._two_index == cur_card_index-1){
								is_can_not_chi=true;
							}
						}
						if(first_index ==  cur_card_index-1){
							if(object._one_index == cur_card_index-1 && object._two_index == cur_card_index+1){
								is_can_not_chi=true;
							}
						}
						if(first_index == cur_card_index){
							if(object._one_index == cur_card_index+1 && object._two_index == cur_card_index+2){
								is_can_not_chi=true;
							}
						}
						if(is_can_not_chi){
							break;
						}	
					}
				}
				if(!is_can_not_chi){
					int chi_kind[] = {GameConstants.WIK_LEFT,GameConstants.WIK_CENTER,GameConstants.WIK_RIGHT};
					eat_type |= chi_kind[i];	
					type_count[count++] = chi_kind[i];
				}

			}	
			
		}


		type_eat_count[0] = count;
    	return eat_type ;
    	
	}
	// 碰牌判断
	public int check_peng(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}
	// 碰牌判断
	public int check_peng_yywhz(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}
    public int check_wai_yywhz(int card_index[],int cur_card)
    {
    	// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_YYWHZ_WAI : GameConstants.WIK_NULL;
    }
    public int check_liu_nei_yywhz(int card_index[],WeaveItem WeaveItem[], int cbWeaveCount,int cur_card,GangCardResult gangCardResult)
    {
    	int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (card_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_YYWHZ_LIU_NEI;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.WIK_YYWHZ_LIU_NEI;
				}
		}
		return cbActionMask;
    }
    public int check_liu_wai_yywhz(int card_index[],WeaveItem WeaveItem[], int cbWeaveCount,int cur_card,GangCardResult gangCardResult,boolean is_dispath)
    {
    	int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_YYWHZ_WAI) {
				if (WeaveItem[i].center_card == cur_card) {
					cbActionMask |= GameConstants.WIK_YYWHZ_LIU_WAI;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.WIK_YYWHZ_LIU_WAI;
				}
				if(!is_dispath){
					for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
						if(this.switch_to_card_data(j) == WeaveItem[i].center_card && card_index[j] > 0){
							cbActionMask |= GameConstants.WIK_YYWHZ_LIU_WAI;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.isPublic[index] = 1;// 明刚
							gangCardResult.type[index] = GameConstants.WIK_YYWHZ_LIU_WAI;
						}
					}	
				}

			}
			
		}
		if(is_dispath){
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if(card_index[j] == 3 && this.switch_to_card_data(j) == cur_card){
					cbActionMask |= GameConstants.WIK_YYWHZ_LIU_WAI;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = cur_card;
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.WIK_YYWHZ_LIU_WAI;
				}
			}
		}
		return cbActionMask;
    }
    public int check_piao_yiyangwhz(int card_index[],WeaveItem WeaveItem[], int cbWeaveCount,int cur_card,GangCardResult gangCardResult,
    		int seat_index,int provider,boolean is_dispath)
    {
    	int cbActionMask = GameConstants.WIK_NULL;
    	if(seat_index == provider){
    		// 手上杠牌
    		for (int i = 0; i < cbWeaveCount; i++) {
    			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
    				if (WeaveItem[i].center_card == cur_card) {
    					cbActionMask |= GameConstants.WIK_YIYANGWHZ_PIAO;

    					int index = gangCardResult.cbCardCount++;
    					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
    					gangCardResult.isPublic[index] = 1;// 明刚
    					gangCardResult.type[index] = GameConstants.WIK_YIYANGWHZ_PIAO;
    				}
    				if(!is_dispath){
    					for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
        					if(this.switch_to_card_data(j) == WeaveItem[i].center_card && card_index[j] > 0){
        						cbActionMask |= GameConstants.WIK_YIYANGWHZ_PIAO;

        						int index = gangCardResult.cbCardCount++;
        						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
        						gangCardResult.isPublic[index] = 1;// 明刚
        						gangCardResult.type[index] = GameConstants.WIK_YIYANGWHZ_PIAO;
        					}
        				}	
    				}
    				
    			}
    		}
    	}

		if(seat_index != provider){
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if(card_index[j] == 3 && this.switch_to_card_data(j) == cur_card){
					cbActionMask |= GameConstants.WIK_YIYANGWHZ_PIAO;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = cur_card;
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.WIK_YIYANGWHZ_PIAO;
				}
			}
		}
		return cbActionMask;
    }
    
    public int check_qing_nei_yiyangwhz(int card_index[],WeaveItem WeaveItem[], int cbWeaveCount,int cur_card,GangCardResult gangCardResult)
    {
    	int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (card_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_YIYANGWHZ_QING_NEI;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.WIK_YIYANGWHZ_QING_NEI;
				}
		}
		return cbActionMask;
    }
    public int check_qing_wai_yiyangwhz(int card_index[],WeaveItem WeaveItem[], int cbWeaveCount,int cur_card,GangCardResult gangCardResult,boolean is_dispath)
    {
    	int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_YIYANGWHZ_WAI) {
				if (WeaveItem[i].center_card == cur_card) {
					cbActionMask |= GameConstants.WIK_YIYANGWHZ_QING_WAI;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.WIK_YIYANGWHZ_QING_WAI;
				}
				if(!is_dispath){
					for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
						if(this.switch_to_card_data(j) == WeaveItem[i].center_card && card_index[j] > 0){
							cbActionMask |= GameConstants.WIK_YIYANGWHZ_QING_WAI;

							int index = gangCardResult.cbCardCount++;
							gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
							gangCardResult.isPublic[index] = 1;// 明刚
							gangCardResult.type[index] = GameConstants.WIK_YIYANGWHZ_QING_WAI;
						}
					}	
				}

			}
			
		}
		if(is_dispath){
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if(card_index[j] == 3 && this.switch_to_card_data(j) == cur_card){
					cbActionMask |= GameConstants.WIK_YIYANGWHZ_QING_WAI;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = cur_card;
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.WIK_YIYANGWHZ_QING_WAI;
				}
			}
		}
		return cbActionMask;
    }
    public boolean check_si_shou(int type,int seat_index,int cur_card,YiYangWHZTable table){
		if(type != GameConstants.WIK_PENG){
		   	int eat_type = 0;
	    	int count = 0;
	    	int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引

			Map<Integer,List<can_chi_index>> user_map=table.map_can_chi_index.get(seat_index);
			List<can_chi_index> can_not_chi;
			if(user_map != null){
				can_not_chi = user_map.get(cur_card_index);
			}else{
				can_not_chi=Lists.newArrayList(); 
			}
			int one_index=0;
			int two_index=0;
			if(type == GameConstants.WIK_LEFT){
				one_index=cur_card_index+1;
				two_index=cur_card_index+2;
			}else if(type == GameConstants.WIK_CENTER){
				one_index=cur_card_index-1;
				two_index=cur_card_index+1;
			}else if(type == GameConstants.WIK_RIGHT){
				one_index=cur_card_index-2;
				two_index=cur_card_index-1;
			}else if(type == GameConstants.WIK_EQS){
				if(cur_card_index%10 == 1){
					one_index=cur_card_index+5;
					two_index=cur_card_index+8;
				}else if(cur_card_index%10 == 6){
					one_index=cur_card_index-5;
					two_index=cur_card_index+3;
				}else{
					one_index=cur_card_index-8;
					two_index=cur_card_index-3;
				}
				
			}
			//二七十吃
			int card_value = get_card_value(cur_card);
			if((card_value == 2)||(card_value == 7)||(card_value == 10)){
				boolean is_can_not_chi=false;
				if(can_not_chi !=null ){
					for(int i=0;i<can_not_chi.size();i++){
						can_chi_index object=can_not_chi.get(i);
						int number=0;
						if((object._one_index == one_index || object._two_index == two_index) && !object._is_pass){
							number++;
						}
						if(number == 2){
							return true;
						}	
					}
				}
			}
			//顺子吃


			


			if(can_not_chi !=null ){
				for(int can_not_index=0;can_not_index<can_not_chi.size();can_not_index++){
					can_chi_index object=can_not_chi.get(can_not_index);
					if((object._one_index == one_index && object._two_index == two_index)&& !object._is_pass){
							return true;
					}
				}
			}
		}else{
			int peng_index = 0;
			for (; peng_index < table._cannot_peng_count[seat_index]; peng_index++) {
				if (table._cannot_peng[seat_index][peng_index] == cur_card) {
					return true;
				}
			}
		}
    	return false;
    }
    public int get_weave_hu_xi_yywhz_nxghz(WeaveItem weave_item){
    	switch(weave_item.weave_kind)
    	{
    	case GameConstants.WIK_YYWHZ_WAI:
    		return 4;
    	case GameConstants.WIK_YYWHZ_KAN:
    		return 3;
    	case GameConstants.WIK_YYWHZ_LIU_WAI:
    	case GameConstants.WIK_YYWHZ_LIU_NEI:
    		return 5;
    	case GameConstants.WIK_PENG:
    		return 1;
    	case GameConstants.WIK_EQS:
    		return 1;
    	}
    	return 0;
    }
    public int get_weave_hu_xi_yywhz_yywzh(WeaveItem weave_item){
    	switch(weave_item.weave_kind)
    	{
    	case GameConstants.WIK_YYWHZ_WAI:
    		return 4;
    	case GameConstants.WIK_YYWHZ_KAN:
    		return 3;
    	case GameConstants.WIK_YYWHZ_LIU_WAI:
    	case GameConstants.WIK_YYWHZ_LIU_NEI:
    		return 5;
    	case GameConstants.WIK_PENG:
    		return 1;
    	case GameConstants.WIK_EQS:
    	case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
    		return 1;

    		
    	}
    	return 0;
    }
    public int get_weave_hu_xi_yiyangwhz_yywzh(WeaveItem weave_item){
    	switch(weave_item.weave_kind)
    	{
    	case GameConstants.WIK_YYWHZ_WAI:
    		return 4;
    	case GameConstants.WIK_YYWHZ_KAN:
    		return 3;
    	case GameConstants.WIK_YIYANGWHZ_QING_NEI:
    	case GameConstants.WIK_YIYANGWHZ_QING_WAI:
    		return 4;
    	case GameConstants.WIK_YIYANGWHZ_PIAO:
    	case GameConstants.WIK_PENG:
    		return 1;
    	case GameConstants.WIK_EQS:
    	case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
    		return 1;

    		
    	}
    	return 0;
    }
    public void ming_index_temp(int cbMingIndexTemp[],WeaveItem weaveItems[],int  weaveCount,boolean zimo,int cur_card)
    {
    	if(zimo == false)
    	{
    		if(cur_card != 0)
    			cbMingIndexTemp[switch_to_card_index(cur_card)] = 1;
    	}
    	for(int i = 0; i< weaveCount;i++)
    	{
    		switch(weaveItems[i].weave_kind)
        	{
        	case GameConstants.WIK_PENG:
        		cbMingIndexTemp[switch_to_card_index(weaveItems[i].center_card)] = 1;
        		break;
        	case GameConstants.WIK_LEFT:
	    	{
	    		int card_index = this.switch_to_card_index(weaveItems[i].center_card);
	            for(int j =  card_index ; j<=card_index+2;j++ )
	            {
	            	cbMingIndexTemp[j] ++;
	            }
	    		break;
	    	}
	    	case GameConstants.WIK_CENTER:
	    	{
	    		int card_index = this.switch_to_card_index(weaveItems[i].center_card);
	            for(int j = card_index - 1 ; j<=card_index+1;j++ )
	            {
	            	cbMingIndexTemp[j] ++;
	            }
	    		break;
	    	}
	    	case GameConstants.WIK_RIGHT:
	    	{
	    		int card_index = this.switch_to_card_index(weaveItems[i].center_card);
	            for(int j = card_index - 2 ; j<=card_index;j++ )
	            {
	            	cbMingIndexTemp[j] ++;
	            }
	    		break;
	    	}
        		
        	}
    	}
    	return ;
    }
    public int calculate_dui_zi_hu_count(AnalyseItem analyseItem)
    {
    	int dui_zi_count = 0;
    	for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch(analyseItem.cbWeaveKind[j])
			{
			case GameConstants.WIK_KAN:
	      	case GameConstants.WIK_WEI:
	    	case GameConstants.WIK_CHOU_WEI:
	    	case GameConstants.WIK_PENG:
	    	{
	    		dui_zi_count++;
	    		break;
	    	}   
			}
    	}
    	if(analyseItem.cbCardEye!=0)
    	{
    		dui_zi_count++;
    	}
    	return dui_zi_count;
    }
    public boolean is_hua_man_yuan(AnalyseItem analyseItem){
    	boolean is_hua_man_yuan  = false;
    	int hong_zi_count = 0;
    	for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch(analyseItem.cbWeaveKind[j])
			{
			case GameConstants.WIK_KAN:
	      	case GameConstants.WIK_WEI:
	    	case GameConstants.WIK_CHOU_WEI:
	    	case GameConstants.WIK_PENG:
	    	{	if(color_hei(analyseItem.cbCenterCard[j]))
	    			hong_zi_count++;
	    			break;
	    	}
	    	case GameConstants.WIK_LEFT:
	    	{
    			if(color_hei(analyseItem.cbCenterCard[j])==false)
    				hong_zi_count++;
    	
    			if(color_hei(analyseItem.cbCenterCard[j]+1)==false)
	    			hong_zi_count++;
	    		
    			if(color_hei(analyseItem.cbCenterCard[j]+2)==false)
	    			hong_zi_count++;
			    break;
	    	} 
	    	case GameConstants.WIK_CENTER:
	    	{
	    		if(color_hei(analyseItem.cbCenterCard[j])==false)
	    			hong_zi_count++;
	    	
	    			if(color_hei(analyseItem.cbCenterCard[j]+1)==false)
		    			hong_zi_count++;
		    		
	    			if(color_hei(analyseItem.cbCenterCard[j]-1)==false)
		    			hong_zi_count++;
				    break;
	    	} 
	      	case GameConstants.WIK_RIGHT:
	    	{
	    		if(color_hei(analyseItem.cbCenterCard[j])==false)
	    			hong_zi_count++;
	    	
    			if(color_hei(analyseItem.cbCenterCard[j]-1)==false)
	    			hong_zi_count++;
	    		
    			if(color_hei(analyseItem.cbCenterCard[j]-2)==false)
	    			hong_zi_count++;
			    break;
	    	} 
			}
	
    	}
    	if(analyseItem.cbCardEye!=0)
    	{
    		hong_zi_count++;
    	}
    	if(hong_zi_count == 7)
    		is_hua_man_yuan = true;
    	
    	return is_hua_man_yuan;
    	   	
    }
    public void analyse_item_to_card(AnalyseItem analyseItem,int cbAnalyseIndexTemp[])
    {
    	for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch(analyseItem.cbWeaveKind[j])
			{
			case GameConstants.WIK_KAN:
	      	case GameConstants.WIK_WEI:
	      	case GameConstants.WIK_XIAO:
	      	case GameConstants.WIK_CHOU_XIAO:
	    	case GameConstants.WIK_CHOU_WEI:
	    	case GameConstants.WIK_PENG:
	    	{
	    		int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
	    		cbAnalyseIndexTemp[card_index] +=3;
	    		break;
	    	}

	    	case GameConstants.WIK_LEFT:
	    	{
	    		int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
	            for(int i =  card_index ; i<=card_index+2;i++ )
	            {
	            	cbAnalyseIndexTemp[i] ++;
	            }
	    		break;
	    	}
	    	case GameConstants.WIK_CENTER:
	    	{
	    		int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
	            for(int i = card_index - 1 ; i<=card_index+1;i++ )
	            {
	            	cbAnalyseIndexTemp[i] ++;
	            }
	    		break;
	    	}
	    	case GameConstants.WIK_RIGHT:
	    	{
	    		int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
	            for(int i = card_index - 2 ; i<=card_index;i++ )
	            {
	            	cbAnalyseIndexTemp[i] ++;
	            }
	    		break;
	    	}	    
			}
    	}
    	if(analyseItem.cbCardEye!=0)
    	{
    		int card_index = this.switch_to_card_index(analyseItem.cbCardEye);
    		cbAnalyseIndexTemp[card_index] +=2;
    	}
    	return ;
    }
    public boolean is_zhuo_hu(int cbAnalyseIndexTemp[])
    {
    	int ying_hu_count = 0;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
			{
				ying_hu_count ++;
			}
    		
    	}
       if(ying_hu_count ==2)
    	   return true;
       else
    	   return false;
    }
    public boolean is_jie_mei_zhuo_hu(int cbAnalyseIndexTemp[])
    {

     	boolean  is_jie_mei = false;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
    			{
    				if(i>0&&cbAnalyseIndexTemp[i-1]==4)
    					is_jie_mei = true;	
    			}
    		
    	}
       if(is_jie_mei  == true)
    	   return true;
       else
    	   return false;
    	
    }
    public boolean is_san_luan_zhuo_hu(int cbAnalyseIndexTemp[])
    {
    	int ying_hu_count = 0;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
			{
				ying_hu_count ++;
			}
    		
    	}
       if(ying_hu_count ==3)
    	   return true;
       else
    	   return false;
   
    }
    public boolean is_jie_mei_zhuo_dai_tuo_hu(int cbAnalyseIndexTemp[])
    {

    	int ying_hu_count = 0;
    	boolean  is_jie_mei = false;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
    			{
    				ying_hu_count ++;
    				if(i>0&&cbAnalyseIndexTemp[i-1]==4)
    					is_jie_mei = true;	
    			}
    		
    	}
       if(is_jie_mei  == true &&ying_hu_count == 3)
    	   return true;
       else
    	   return false;
    }
    public boolean is_die_shun_zhuo_hu(int cbAnalyseIndexTemp[])
    {

     	boolean  is_die_shun = false;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
    			{
    				if(i>1&&cbAnalyseIndexTemp[i-1]==4&&cbAnalyseIndexTemp[i-2]==4)
    					is_die_shun = true;	
    			}
    		
    	}
       if(is_die_shun  == true)
    	   return true;
       else
    	   return false;
    	
    }
    public boolean is_die_shun_zhuo_dai_tuo_hu(int cbAnalyseIndexTemp[])
    {

    	int ying_hu_count = 0;
    	boolean  is_die_shun = false;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
    			{
    				ying_hu_count ++;
    				if(i>1&&cbAnalyseIndexTemp[i-1]==4&&cbAnalyseIndexTemp[i-2]==4)
    					is_die_shun = true;	
    			}
    		
    	}
       if(is_die_shun  == true &&ying_hu_count == 4)
    	   return true;
       else
    	   return false;
    }
    public boolean is_si_luan_zhuo_hu(int cbAnalyseIndexTemp[])
    {
    	int ying_hu_count = 0;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
			{
				ying_hu_count ++;
			}
    		
    	}
       if(ying_hu_count ==4)
    	   return true;
       else
    	   return false;
   
    }
    public boolean is_zhen_ba_peng_tou(int cbAnalyseIndexTemp[])
    {

     	boolean  is_zhen_ba = false;

		if(cbAnalyseIndexTemp[7] == 4&&cbAnalyseIndexTemp[17] == 4)
		{
		
				is_zhen_ba = true;	
		}
    		
    	
       if(is_zhen_ba  == true)
    	   return true;
       else
    	   return false;
    }
    public boolean is_jia_ba_peng_tou(int cbAnalyseIndexTemp[])
    {

     	boolean  is_jia_ba = false;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(i!= 7&&i<10&&cbAnalyseIndexTemp[i] == 4&&cbAnalyseIndexTemp[i+10] == 4)
			{
			
    			is_jia_ba = true;	
    			break;
			}
    		
    	}
       if(is_jia_ba  == true)
    	   return true;
       else
    	   return false;
    }
    public boolean is_long_bai_wei(int cbAnalyseIndexTemp[])
    {
    	boolean  is_long_bai_wei = false;

		if(cbAnalyseIndexTemp[0] == 4&&cbAnalyseIndexTemp[10] == 4)
		{
		
			is_long_bai_wei = true;	
		}
		if(cbAnalyseIndexTemp[9] == 4&&cbAnalyseIndexTemp[19] == 4)
		{
		
			is_long_bai_wei = true;	
		}
    		
    	
       if(is_long_bai_wei  == true)
    	   return true;
       else
    	   return false;
    }
    public int calculate_ying_hu_count (int cbAnalyseIndexTemp[] )
    {
    	int ying_hu_count = 0;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbAnalyseIndexTemp[i] == 4)
    			if(color_hei(switch_to_card_data(i)) == false)
    			{
    				ying_hu_count ++;
    			}
    		
    	}
       return ying_hu_count;
    	
    }
    public boolean piao_dui(AnalyseItem analyseItem)
    {
    	for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch(analyseItem.cbWeaveKind[j])
			{
	    	case GameConstants.WIK_PENG:
	    	{
	    		return true;
	    		
	    	}

	    	
			}
    	}
    	return  false;
    }
    public boolean is_ji_ding(int cur_card,AnalyseItem analyseItem,int weaveCount)
    {
    	if(cur_card != analyseItem.cbCardEye)
    		return false;
    	for (int j = 0; j < 6-weaveCount; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch(analyseItem.cbWeaveKind[j])
			{
			case GameConstants.WIK_LEFT:
			{	if(analyseItem.cbCenterCard[j]+1 == cur_card )
	    			return true;
			}
	    	case GameConstants.WIK_CENTER:
	    	{
	    		if(analyseItem.cbCenterCard[j] == cur_card )
	    			return true;
	    		
	    	}
	    	case GameConstants.WIK_RIGHT:
	    	{
	    		if(analyseItem.cbCenterCard[j]-1 == cur_card )
	    			return true;
	    		
	    	}

	    	
			}
    	}
    	return false;
    }
    public boolean is_bei_kao_bei(int _hu_cards[],int count, int cur_card,AnalyseItem analyseItem, int weaveCount)
    {
    	int  bei_kao_bei = 0;
    	int temp_cur_card = (cur_card+16)%32;
    	for(int i = 0; i< count;i++)
    	{
    		if(_hu_cards[i] == cur_card||_hu_cards[i] == temp_cur_card)
    			bei_kao_bei++;
    	}
    	if(bei_kao_bei>=2)
    		return true;
    	return false;
    }
    public boolean is_shou_qian_shou(int _hu_cards[],int count, int cur_card,AnalyseItem analyseItem, int weaveCount)
    {
    	int  shou_qian_shou = 0;
    	for(int i = 0; i< count;i++)
    	{
    		if(_hu_cards[i] == cur_card&&_hu_cards[i+1] ==cur_card+1 )
    		{
    			shou_qian_shou++;
    			break;
    		}
    	}
    	if(shou_qian_shou>=2)
    		return true;
    	return false;
    }
    public boolean is_feng_bai_wei(int _hu_cards[],int count, int cur_card,AnalyseItem analyseItem, int weaveCount)
    {
    	int  feng_bai_wei = 0;
    	for(int i = 0; i< count;i++)
    	{
    		if(_hu_cards[i] == cur_card&&_hu_cards[i+1] ==cur_card+1 )
    		{
    			feng_bai_wei++;
    			break;
    		}
    	}
    	if(feng_bai_wei>=2)
    		return true;
    	return false;
    }
    public int get_all_hu_xi_weave(WeaveItem weave_items[], int weave_count, int cbMingIndexTemp[]){
    	int hu_xi = 0;
    	for(int i = 0; i< GameConstants.MAX_HH_INDEX; i++)
    	{
    		if(cbMingIndexTemp[i] == 4)
    			if(color_hei(switch_to_card_data(i)) == true)
    					hu_xi += 5;
    			else
    					hu_xi += 7;
    	
    	}
    	for (int j = 0; j < weave_count; j++) {
			if (weave_items[j].weave_kind == GameConstants.WIK_NULL)
				break;
			switch(weave_items[j].weave_kind)
			{
			case GameConstants.WIK_KAN:
	      	case GameConstants.WIK_WEI:
	    	case GameConstants.WIK_CHOU_WEI:
	    	{	
	    		if(color_hei(weave_items[j].center_card) == true)
    			{
    				hu_xi += 3;
    				
    			}
    			else
    			{
    				hu_xi += 4;
    			}
	    		break;
	    	}
	    	case GameConstants.WIK_PENG:
	    	{
	    		if(color_hei(weave_items[j].center_card) == true)
    			{
    				hu_xi += 2;
    			}
    			else
    			{
    				hu_xi += 4;
    			}
	    		break;
	    	}
			}
    	}
    	return hu_xi;
    	
    }

   
    public int get_xing_pai_count(WeaveItem weave_item[],int weave_count,int card ){
    	int count = 0;
    	for(int i = 0; i< weave_count;i++)
    	{
    		switch(weave_item[i].weave_kind)
        	{
        	case GameConstants.WIK_TI_LONG:
        	case GameConstants.WIK_AN_LONG:
        	case GameConstants.WIK_PAO:
        		count += (weave_item[i].center_card == card)?4:0;
        		break;
        	case GameConstants.WIK_SAO:
        	case GameConstants.WIK_CHOU_SAO:
        	case GameConstants.WIK_KAN:
          	case GameConstants.WIK_WEI:
          	case GameConstants.WIK_XIAO:
          	case GameConstants.WIK_CHOU_XIAO:
        	case GameConstants.WIK_CHOU_WEI:
        	case GameConstants.WIK_PENG:
        		count+= (weave_item[i].center_card == card)?3:0;
        		break;
        	case GameConstants.WIK_EQS:{
        	   if( weave_item[i].center_card < 16)
        	    switch(card){
        		case 0x02:
        		case 0x07:
        		case 0x0a:
        		
        			count+= 1;
        			break;
        	    }
        	   if( weave_item[i].center_card > 16)
           	    switch(card){
        		case 0x12:     		
        		case 0x17:	
        		case 0x1a:
        			
        			count+= 1;
        			break;			
        		}
        	   break;
        		
        	}
        	case GameConstants.WIK_YWS:{
    		   if( weave_item[i].center_card < 16)
           	    switch(card){
        		case 0x01:
        		case 0x05:
        		case 0x0a:
        			count+= 1;
        			break;
           	    }
    		   if( weave_item[i].center_card > 16)
              	switch(card){
        		case 0x11:
        		case 0x15:      		
        		case 0x1a:
        			count+= 1;
        			break;
        		}
    		   break;
        	}
        		
        	case GameConstants.WIK_LEFT:
        	{
        		if((weave_item[i].center_card == card)
        		||(weave_item[i].center_card+1 == card)
        		||(weave_item[i].center_card+2 == card))
        		{
        			count +=1;
        		}
        		break;
        	}
        	case GameConstants.WIK_CENTER:
        	{
        		if((weave_item[i].center_card == card)
                		||(weave_item[i].center_card+1 == card)
                		||(weave_item[i].center_card-1 == card))
                		{
                			count +=1;
                		}
                			
        		break;
        	}
        	case GameConstants.WIK_RIGHT:
        	{
        		if((weave_item[i].center_card == card)
                		||(weave_item[i].center_card-1 == card)
                		||(weave_item[i].center_card-2 == card))
                		{
                			count +=1;
                		}
                			
        		break;
        	}
        	case GameConstants.WIK_DDX:
        	{
        		if(weave_item[i].center_card>16)
        		{
        			if(weave_item[i].center_card == card)
        				count +=2;
        			else if(weave_item[i].center_card - 16 == card)
        				count +=1;
        		}
        		else{
        			if(weave_item[i].center_card<16)
            		{
            			if(weave_item[i].center_card == card)
            				count +=1;
            			else if(weave_item[i].center_card + 16 == card)
            				count +=2;
            		}
        		}
        		break;
        	}
        	case GameConstants.WIK_XXD:
        	{
        		if(weave_item[i].center_card>16)
        		{
        			if(weave_item[i].center_card == card)
        				count +=1;
        			else if(weave_item[i].center_card - 16 == card)
        				count +=2;
        		}
        		else{
        			if(weave_item[i].center_card<16)
            		{
            			if(weave_item[i].center_card == card)
            				count +=2;
            			else if(weave_item[i].center_card + 16 == card)
            				count +=1;
            		}
        		}
        		break;
        	}
        	case GameConstants.WIK_DUI_ZI:{
        		if(weave_item[i].center_card == card)
    				count +=2;
        	}
        		
        	}
    	}
    	return count ;
    }
    
    public int get_analyse_hu_xi(int cbWeaveKind ,int center_card){
    	switch(cbWeaveKind)
    	{
    	case GameConstants.WIK_TI_LONG:
    	case GameConstants.WIK_AN_LONG:
    	case GameConstants.WIK_AN_LONG_LIANG:
    		return (get_card_color(center_card)!=0)?12:9;
    	case GameConstants.WIK_PAO:
    		return (get_card_color(center_card)!=0)?9:6;
    	case GameConstants.WIK_SAO:
    	case GameConstants.WIK_CHOU_SAO:
    	case GameConstants.WIK_KAN:
      	case GameConstants.WIK_WEI:
      	case GameConstants.WIK_XIAO:
      	case GameConstants.WIK_CHOU_XIAO:
    	case GameConstants.WIK_CHOU_WEI:
    		return (get_card_color(center_card)!=0)?6:3;
    	case GameConstants.WIK_PENG:
    		return (get_card_color(center_card)!=0)?3:1;
    	case GameConstants.WIK_EQS:
    		return (get_card_color(center_card)!=0)?6:3;
    	case GameConstants.WIK_YWS:
    		return (get_card_color(center_card)!=0)?6:3;	
    	case GameConstants.WIK_LEFT:
    	{
    		int card_value = get_card_value(center_card);
    		if(card_value == 1)
    		{
    			return (get_card_color(center_card)!=0)?6:3;
    		}
    		break;
    	}
    	case GameConstants.WIK_CENTER:
    	{
    		int card_value = get_card_value(center_card);
    		if(card_value == 2)
    		{
    			return (get_card_color(center_card)!=0)?6:3;
    		}
    		break;
    	}
    	case GameConstants.WIK_RIGHT:
    	{
    		int card_value = get_card_value(center_card);
    		if(card_value == 3)
    		{
    			return (get_card_color(center_card)!=0)?6:3;
    		}
    		break;
    	}
    		
    	}
    	return 0;
    }
	/**
	 * 杠牌判断 别人打的牌自己能不能杠
	 * 
	 * @param card_index
	 *            当前牌型
	 * @param cur_card
	 *            出的牌
	 * @return
	 */
	public int estimate_pao_card_out_card(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_PAO : GameConstants.WIK_NULL;
	}
	/**
	 * 跑牌分析
	 */
	public int analyse_first_pao_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_PAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗龙
				gangCardResult.type[index] = GameConstants.PAO_TYPE_AN_LONG;
			
			}
		}

		return cbActionMask; 
	}
	/**
	 * 跑牌分析
	 */
	public int analyse_pao_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_PAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 提龙
				gangCardResult.type[index] = GameConstants.PAO_TYPE_TI_MINE_LONG;
				
			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_PAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明杠
						gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
					}
				}
				if (WeaveItem[i].weave_kind == GameConstants.WIK_SAO) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_PAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明杠
						gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_SAO_LONG;
					}
				}
			}
		}

		return cbActionMask;
	}
	//提牌判断
    public int get_action_ti_Card(int cards_index[], int  ti_cards_index[])
    {
    	int ti_card_count=0;
    	for(int i = 0; i < GameConstants.MAX_HH_INDEX; i++){
    		if (cards_index[i] == 4) {
    			ti_cards_index[ti_card_count++] = cards_index[i];
    		}
    	}
    	return ti_card_count;
    }

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index--手牌
	 * @param WeaveItem
	 *            --落地牌
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆）
	 * @return
	 */
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					}
				}
			}
		}

		return cbActionMask;
	}

	public int analyse_gang_card_hh(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上有3张
		if (cards_index[this.switch_to_card_index(card)] == 3) {
			cbActionMask |= GameConstants.WIK_PAO;
			int index = gangCardResult.cbCardCount++;
			gangCardResult.cbCardData[index] = card;
			gangCardResult.isPublic[index] = 0;// 明刚
			gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			return cbActionMask;
		}

		if (check_weave) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (WeaveItem[i].center_card == card) {
						cbActionMask |= GameConstants.WIK_PAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
						break;
					}
				}
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card_hand_card(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[i] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		return cbActionMask;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		// 组合杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_GANG;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明杠
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					break;
				}
			}
		}

		return cbActionMask;
	}
	// 跑牌分析 ，分析自己抓的牌是跑还是提
	public int analyse_pao_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_PAO;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.PAO_TYPE_TI_MINE_LONG;
			}
		}

		// 组合杠牌
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_PAO;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明杠
					gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
					break;
				}
			}
			else if (WeaveItem[i].weave_kind == GameConstants.WIK_SAO) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_PAO;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;// 明杠
					gangCardResult.type[index] = GameConstants.PAO_TYPE_MINE_SAO_LONG;
					break;
				}
			}
		}

		return cbActionMask;
	}
	// 是否单吊
	public boolean is_dan_diao(int cards_index[], int cur_card) {
		// 单牌数目
		// int cbReplaceCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			// 单牌统计
			if (cbCardCount == 2) {
				bDuizi = true;
			}
			nTaltal += cbCardCount;
		}

		if (bDuizi && nTaltal == 2) {
			return true;
		}
		return false;

	}
	/**
	 * 计算牌数量
	 * @param analyseItem
	 * @return
	 */
	public int calculate_all_pai_count(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YYWHZ_LIU_NEI:
			case GameConstants.WIK_YYWHZ_LIU_WAI:
				  count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				count +=3;
				break;
			}
				
		}
		if(analyseItem.curCardEye!=false)
			count +=2;
		if(analyseItem.cbMenEye[0] != 0){
			count +=2;
		}
		return count;
		
	}
	public int calculate_all_pai_count_yiyang(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YIYANGWHZ_QING_NEI:
			case GameConstants.WIK_YIYANGWHZ_QING_WAI:
			case GameConstants.WIK_YIYANGWHZ_PIAO:
				  count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				count +=3;
				break;
			}
				
		}
		if(analyseItem.curCardEye!=false)
			count +=2;
		if(analyseItem.cbMenEye[0] != 0){
			count +=2;
		}
		return count;
		
	}
	public boolean color_hei(int card)
	{
		boolean b_hei = false;
		int value = get_card_value(card);
		switch(value)
		{
		case 2:
		case 7:
		case 10:
			b_hei = false;
			break;
		default:
			b_hei=true;
		}
		return b_hei;
	}
	public int calculate_hong_pai_count(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YYWHZ_LIU_NEI:
			case GameConstants.WIK_YYWHZ_LIU_WAI:
				if(color_hei(analyseItem.cbCenterCard[i])==false)
					count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(analyseItem.cbCenterCard[i])==false)
					count +=3;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3; j++ )
				{
					if(color_hei(analyseItem.cbCardData[i][j])==false)
						count +=1;
					 
				}
				break;
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				count +=3;

				break;
			}
				
		}
		if((analyseItem.curCardEye!=false)&&(color_hei(analyseItem.cbCardEye)==false)){
			count +=2;
		}
		if(analyseItem.cbMenEye[0] != 0){
			if(color_hei(analyseItem.cbMenEye[0]) == false){
				count++;
			}
			if(color_hei(analyseItem.cbMenEye[1]) == false){
				count++;
			}
		}
		return count;
	}
	public int calculate_hong_pai_count_yiyang(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			int center_card=analyseItem.cbCenterCard[i];
			if(center_card>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
				center_card-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YIYANGWHZ_QING_NEI:
			case GameConstants.WIK_YIYANGWHZ_QING_WAI:
			case GameConstants.WIK_YIYANGWHZ_PIAO:
				if(color_hei(center_card)==false)
					count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(center_card)==false)
					count +=3;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3; j++ )
				{
					if(color_hei(analyseItem.cbCardData[i][j])==false)
						count +=1;
					 
				}
				break;
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				count +=3;

				break;
			}
				
		}
		
		int cbCardEye=analyseItem.cbCardEye;
		int cbMenEye[]=new int[2];
		cbMenEye[0]=analyseItem.cbMenEye[0];
		cbMenEye[1]=analyseItem.cbMenEye[1];
		if(cbCardEye>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbCardEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[0]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[0]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[1]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[1]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if((analyseItem.curCardEye!=false)&&(color_hei(cbCardEye)==false)){
			count +=2;
		}
		if(cbMenEye[0] != 0){
			if(color_hei(cbMenEye[0]) == false){
				count++;
			}
			if(color_hei(cbMenEye[1]) == false){
				count++;
			}
		}
		return count;
	}
	
	public int calculate_hong_pai_count(WeaveItem weave_item[],int weave_count)
	{
		int count = 0;
		for(int i = 0; i< weave_count;i++)
		{
			switch(weave_item[i].weave_kind){
			case GameConstants.WIK_YYWHZ_LIU_NEI:
			case GameConstants.WIK_YYWHZ_LIU_WAI:
				if(color_hei(weave_item[i].center_card)==false)
					count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(weave_item[i].center_card)==false)
					count +=3;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3; j++ )
				{
					if(color_hei(weave_item[i].weave_card[j])==false)
						count +=1;
					 
				}
				break;
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				count +=3;

				break;
			case GameConstants.WIK_DUI_ZI:
				if(color_hei(weave_item[i].center_card)==false){
					count +=2;
				}
				break;
			case GameConstants.WIK_YYWHZ_MENZI:
			case GameConstants.WIK_YYWHZ_MENZI_GUANG:
				for(int j=0;j<2;j++){
					if(color_hei(weave_item[i].weave_card[j])==false){
						count +=1;
					}
				}
				
				break;
			}
				
		}
			
		return count;
		
		
	}
	public int calculate_hong_pai_count_yiyang(WeaveItem weave_item[],int weave_count)
	{
		int count = 0;
		for(int i = 0; i< weave_count;i++)
		{
			int center_card=weave_item[i].center_card;
			if(center_card>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
				center_card-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
			switch(weave_item[i].weave_kind){
			case GameConstants.WIK_YIYANGWHZ_QING_NEI:
			case GameConstants.WIK_YIYANGWHZ_QING_WAI:
			case GameConstants.WIK_YIYANGWHZ_PIAO:
				if(color_hei(center_card)==false)
					count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(center_card)==false)
					count +=3;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3; j++ )
				{
					if(color_hei(weave_item[i].weave_card[j])==false)
						count +=1;
					 
				}
				break;
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				count +=3;

				break;
			case GameConstants.WIK_DUI_ZI:
			case GameConstants.WIK_DAN_PENG:
			case GameConstants.WIK_DAN_WAI:
				if(color_hei(center_card)==false){
					count +=2;
				}
				break;
			case GameConstants.WIK_YYWHZ_MENZI:
			case GameConstants.WIK_YYWHZ_MENZI_GUANG:
				for(int j=0;j<2;j++){
					if(color_hei(weave_item[i].weave_card[j])==false){
						count +=1;
					}
				}
				
				break;
			}
				
		}
			
		return count;
		
		
	}
	public int calculate_hei_pai_count(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			int cbCenterCard=analyseItem.cbCenterCard[i];
			if(cbCenterCard>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
				cbCenterCard-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YYWHZ_LIU_NEI:
			case GameConstants.WIK_YYWHZ_LIU_WAI:
				if(color_hei(cbCenterCard)==true)
					count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(cbCenterCard)==true)
					count +=3;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3;j++ )
				{
					if(color_hei(analyseItem.cbCardData[i][j])==true)
						count +=1;
				
				}

				break;
			}
				
		}
		if((analyseItem.curCardEye!=false)&&(color_hei(analyseItem.cbCardEye)==true)){
			count +=2;
		}
		if(analyseItem.cbMenEye[0] != 0){
			if(color_hei(analyseItem.cbMenEye[0]) == true){
				count++;
			}
			if(color_hei(analyseItem.cbMenEye[1]) == true){
				count++;
			}
		}
		return count;
		
	}
	public int calculate_hei_pai_count_yiyang(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			int cbCenterCard=analyseItem.cbCenterCard[i];
			if(cbCenterCard>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
				cbCenterCard-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YIYANGWHZ_QING_NEI:
			case GameConstants.WIK_YIYANGWHZ_QING_WAI:
			case GameConstants.WIK_YIYANGWHZ_PIAO:
				if(color_hei(cbCenterCard)==true)
					count +=4;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(cbCenterCard)==true)
					count +=3;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3;j++ )
				{
					if(color_hei(analyseItem.cbCardData[i][j])==true)
						count +=1;
				
				}

				break;
			}
				
		}
		int cbCardEye=analyseItem.cbCardEye;
		int cbMenEye[]=new int[2];
		cbMenEye[0]=analyseItem.cbMenEye[0];
		cbMenEye[1]=analyseItem.cbMenEye[1];
		if(cbCardEye>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbCardEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[0]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[0]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[1]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[1]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if((analyseItem.curCardEye!=false)&&(color_hei(cbCardEye)==true)){
			count +=2;
		}
		if(cbMenEye[0] != 0){
			if(color_hei(cbMenEye[0]) == true){
				count++;
			}
			if(color_hei(cbMenEye[1]) == true){
				count++;
			}
		}
		return count;
		
	}
	public int calculate_weave_hei_pai(WeaveItem weave_item){
		int count = 0;
		switch(weave_item.weave_kind){
		case GameConstants.WIK_YYWHZ_LIU_NEI:
		case GameConstants.WIK_YYWHZ_LIU_WAI:
			if(color_hei(weave_item.center_card)==true)
				count +=4;
			  break;
		case GameConstants.WIK_YYWHZ_WAI:	
		case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_PENG:
			if(color_hei(weave_item.center_card)==true)
				count +=3;
			  break;
		case GameConstants.WIK_LEFT:
			if(color_hei(weave_item.center_card)==true)
				count +=1;
			if(color_hei(weave_item.center_card+1)==true)
				count +=1;
			if(color_hei(weave_item.center_card+2)==true)
				count +=1;
			  break;
		case GameConstants.WIK_CENTER:
			if(color_hei(weave_item.center_card)==true)
				count +=1;
			if(color_hei(weave_item.center_card+1)==true)
				count +=1;
			if(color_hei(weave_item.center_card-1)==true)
				count +=1;
			  break;
		case GameConstants.WIK_RIGHT:
			if(color_hei(weave_item.center_card)==true)
				count +=1;
			if(color_hei(weave_item.center_card-1)==true)
				count +=1;
			if(color_hei(weave_item.center_card-2)==true)
				count +=1;
			  break;
		
		}
		return count;
		
	}
	public int calculate_weave_hong_pai(WeaveItem weave_item){
		int count = 0;
		switch(weave_item.weave_kind){
		case GameConstants.WIK_YYWHZ_LIU_NEI:
		case GameConstants.WIK_YYWHZ_LIU_WAI:
			if(color_hei(weave_item.center_card)==false)
				count +=4;
			  break;
		case GameConstants.WIK_YYWHZ_WAI:	
		case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_PENG:
			if(color_hei(weave_item.center_card)==false)
				count +=3;
			  break;
		case GameConstants.WIK_LEFT:
			if(color_hei(weave_item.center_card)==false)
				count +=1;
			if(color_hei(weave_item.center_card+1)==false)
				count +=1;
			if(color_hei(weave_item.center_card+2)==false)
				count +=1;
			  break;
		case GameConstants.WIK_CENTER:
			if(color_hei(weave_item.center_card)==false)
				count +=1;
			if(color_hei(weave_item.center_card+1)==false)
				count +=1;
			if(color_hei(weave_item.center_card-1)==false)
				count +=1;
			  break;
		case GameConstants.WIK_RIGHT:
			if(color_hei(weave_item.center_card)==false)
				count +=1;
			if(color_hei(weave_item.center_card-1)==false)
				count +=1;
			if(color_hei(weave_item.center_card-2)==false)
				count +=1;
			  break;
		case GameConstants.WIK_EQS:
				count+=3;
			 break;
		
		}
		return count;
		
	}
	public int calculate_da_pai_count(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			count+=get_da_card(analyseItem.cbWeaveKind[i],analyseItem.cbCenterCard[i]);
		}
		if((analyseItem.curCardEye!=false)&&(analyseItem.cbCardEye>16)){
			count +=2;
		}
		if(analyseItem.cbMenEye[0] != 0){
			if(analyseItem.cbMenEye[0]>16){
				count +=2;
			}
		}
		return count;
		
	}
	public int calculate_xiao_pai_count(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			count+=get_xiao_card(analyseItem.cbWeaveKind[i],analyseItem.cbCenterCard[i]);
		}
		
		if((analyseItem.curCardEye!=false)&&(analyseItem.cbCardEye<16)){
			count +=2;
		}
		if(analyseItem.cbMenEye[0] != 0){
			if(analyseItem.cbMenEye[0]<16){
				count +=2;
			}
		}
		return count;
		
	}
	public int get_da_card(int weave_kind,int center_card){
		int count = 0;
		switch(weave_kind){
		case GameConstants.WIK_YYWHZ_LIU_NEI:
		case GameConstants.WIK_YYWHZ_LIU_WAI:
			if(center_card > 16)
				count +=3;
			  break;
		case GameConstants.WIK_YYWHZ_WAI:	
		case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_YYWHZ_SHUNZI:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			if(center_card > 16)
				count +=3;
			  break;
		
		}
		return count;
	}
	public int get_xiao_card(int weave_kind,int center_card){
		int count = 0;
		switch(weave_kind){
		case GameConstants.WIK_YYWHZ_LIU_NEI:
		case GameConstants.WIK_YYWHZ_LIU_WAI:
			if(center_card <16)
				count +=3;
			  break;
		case GameConstants.WIK_YYWHZ_WAI:	
		case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_YYWHZ_SHUNZI:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			if(center_card <16)
				count +=3;
			  break;
		}
		return count;
	}
	
	
	public int calculate_da_pai_count_yiyang(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			count+=get_da_card_yiyang(analyseItem.cbWeaveKind[i],analyseItem.cbCenterCard[i]);
		}
		
		int card_eye=analyseItem.cbCardEye;
		int cbMenEye=analyseItem.cbMenEye[0];
		if(card_eye > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			card_eye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if((analyseItem.curCardEye!=false)&&(card_eye>16)){
			count +=2;
		}
		if(cbMenEye != 0){
			if(cbMenEye>16){
				count +=2;
			}
		}
		return count;
		
	}
	public int calculate_xiao_pai_count_yiyang(AnalyseItem analyseItem)
	{
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			count+=get_xiao_card_yiyang(analyseItem.cbWeaveKind[i],analyseItem.cbCenterCard[i]);
		}
		
		
		int card_eye=analyseItem.cbCardEye;
		int cbMenEye=analyseItem.cbMenEye[0];
		if(card_eye > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			card_eye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if((analyseItem.curCardEye!=false)&&(card_eye<16)){
			count +=2;
		}
		if(cbMenEye != 0){
			if(cbMenEye<16){
				count +=2;
			}
		}
		return count;
		
	}
	public int get_da_card_yiyang(int weave_kind,int center_card){
		int count = 0;
		if(center_card > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			center_card-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		switch(weave_kind){
		case GameConstants.WIK_YIYANGWHZ_QING_NEI:
		case GameConstants.WIK_YIYANGWHZ_QING_WAI:
		case GameConstants.WIK_YIYANGWHZ_PIAO:
			if(center_card > 16)
				count +=3;
			  break;
		case GameConstants.WIK_YYWHZ_WAI:	
		case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_YYWHZ_SHUNZI:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			if(center_card > 16)
				count +=3;
			  break;
		
		}
		return count;
	}
	public int get_xiao_card_yiyang(int weave_kind,int center_card){
		int count = 0;
		if(center_card > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			center_card-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		switch(weave_kind){
		case GameConstants.WIK_YIYANGWHZ_QING_NEI:
		case GameConstants.WIK_YIYANGWHZ_QING_WAI:
		case GameConstants.WIK_YIYANGWHZ_PIAO:
			if(center_card <16)
				count +=3;
			  break;
		case GameConstants.WIK_YYWHZ_WAI:	
		case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_YYWHZ_SHUNZI:
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			if(center_card <16)
				count +=3;
			  break;
		}
		return count;
	}
	public boolean is_hanghangxi(AnalyseItem analyseItem){
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++){
			if(analyseItem.cbWeaveKind[i] == GameConstants.WIK_LEFT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_RIGHT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_CENTER 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_YYWHZ_SHUNZI){
				return false;
			}
		}
		if(analyseItem.cbMenEye[0]%16 == 2 || analyseItem.cbMenEye[0]%16 == 7 || analyseItem.cbMenEye[0]%16 == 10){
			if(analyseItem.cbMenEye[1]%16 == 2 || analyseItem.cbMenEye[1]%16 == 7 || analyseItem.cbMenEye[1]%16 == 10){
				return true;
    		}
		}
		
		if(analyseItem.cbCardEye!=0){
			return true;
		}
		return false;
	}
	public boolean is_duizixi(AnalyseItem analyseItem){
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++){
			if(analyseItem.cbWeaveKind[i] == GameConstants.WIK_LEFT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_RIGHT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_CENTER 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_YYWHZ_SHUNZI
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_YYWHZ_SHUNZI_EQS
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_EQS){
				return false;
			}
		}
		if(analyseItem.cbMenEye[0] != 0){
			return false;
		}
		return true;
	}

	public boolean is_hanghangxi_yiyang(AnalyseItem analyseItem){
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++){
			if(analyseItem.cbWeaveKind[i] == GameConstants.WIK_LEFT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_RIGHT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_CENTER 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_YYWHZ_SHUNZI){
				return false;
			}
		}
		
		int cbCardEye=analyseItem.cbCardEye;
		int cbMenEye[]=new int[2];
		cbMenEye[0]=analyseItem.cbMenEye[0];
		cbMenEye[1]=analyseItem.cbMenEye[1];
		if(cbCardEye>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbCardEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[0]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[0]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[1]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[1]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[0]%16 == 2 || cbMenEye[0]%16 == 7 || cbMenEye[0]%16 == 10){
			if(cbMenEye[1]%16 == 2 || cbMenEye[1]%16 == 7 || cbMenEye[1]%16 == 10){
				return true;
    		}
		}
		
		if(cbCardEye!=0){
			return true;
		}
		return false;
	}
	public boolean is_duizixi_yiyang(AnalyseItem analyseItem){
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++){
			if(analyseItem.cbWeaveKind[i] == GameConstants.WIK_LEFT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_RIGHT 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_CENTER 
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_YYWHZ_SHUNZI
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_YYWHZ_SHUNZI_EQS
				|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_EQS){
				return false;
			}
		}
		
		
		if(analyseItem.cbMenEye[0] != 0){
			return false;
		}
		return true;
	}
	public int is_ji_piao(AnalyseItem analyseItem){
		int count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			int cbCenterCard=analyseItem.cbCenterCard[i];
			if(cbCenterCard>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
				cbCenterCard-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YIYANGWHZ_QING_NEI:
			case GameConstants.WIK_YIYANGWHZ_QING_WAI:
			case GameConstants.WIK_YIYANGWHZ_PIAO:
				if(color_hei(cbCenterCard)==false)
					count ++;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(cbCenterCard)==false)
					count ++;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3; j++ )
				{
					if(color_hei(analyseItem.cbCardData[i][j])==false){
						return 0;
					}
					 
				}
				break;
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				count ++;

				break;
			}
				
		}
		int cbCardEye=analyseItem.cbCardEye;
		int cbMenEye[]=new int[2];
		cbMenEye[0]=analyseItem.cbMenEye[0];
		cbMenEye[1]=analyseItem.cbMenEye[1];
		if(cbCardEye>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbCardEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[0]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[0]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[1]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[1]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if((analyseItem.curCardEye!=false)&&(color_hei(cbCardEye)==false)){
			count ++;
		}
		if(cbMenEye[0] != 0){
			if(color_hei(cbMenEye[0]) == false && color_hei(cbMenEye[1]) == false){
				count ++;
			}else if(color_hei(cbMenEye[0]) == false || color_hei(cbMenEye[1]) == false){
				return 0;
			}
		}
		return count;
	}
	public boolean is_yin(AnalyseItem analyseItem){
		int hong_count = 0;
		int ott_count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			int cbCenterCard=analyseItem.cbCenterCard[i];
			if(cbCenterCard>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
				cbCenterCard-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YIYANGWHZ_QING_NEI:
			case GameConstants.WIK_YIYANGWHZ_QING_WAI:
			case GameConstants.WIK_YIYANGWHZ_PIAO:
				if(color_hei(cbCenterCard)==false)
					hong_count ++;
				  break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(cbCenterCard)==false)
					hong_count ++;
				  break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				for(int j = 0; j<3; j++ ){
					if(color_hei(analyseItem.cbCardData[i][j])==false){
						ott_count++;
					}
						 
				}
				break;
			case GameConstants.WIK_YYWHZ_SHUNZI_EQS:
			case GameConstants.WIK_EQS:
				hong_count ++;

				break;
			}
				
		}
		int cbCardEye=analyseItem.cbCardEye;
		int cbMenEye[]=new int[2];
		cbMenEye[0]=analyseItem.cbMenEye[0];
		cbMenEye[1]=analyseItem.cbMenEye[1];
		if(cbCardEye>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbCardEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[0]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[0]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[1]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[1]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if((analyseItem.curCardEye!=false)&&(color_hei(cbCardEye)==false)){
			hong_count ++;
		}
		if(cbMenEye[0] != 0){
			if(color_hei(cbMenEye[0]) == false && color_hei(cbMenEye[1]) == false){
				hong_count ++;
			}else if(color_hei(cbMenEye[1]) == false ||  color_hei(cbMenEye[0]) == false){
				ott_count++;
			}
		}
		if(hong_count == 1 && ott_count == 1){
			return true;
		}
		return false;
	}
	
	public boolean is_hua_huzi(AnalyseItem analyseItem){

		int hong_count = 0;
		int ott_count = 0;
		for(int i = 0; i< analyseItem.cbWeaveKind.length;i++)
		{
			int cbCenterCard=analyseItem.cbCenterCard[i];
			if(cbCenterCard>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
				cbCenterCard-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
			switch(analyseItem.cbWeaveKind[i]){
			case GameConstants.WIK_YIYANGWHZ_QING_NEI:
			case GameConstants.WIK_YIYANGWHZ_QING_WAI:
			case GameConstants.WIK_YIYANGWHZ_PIAO:
				if(color_hei(cbCenterCard)){
					return false;
				}
				break;
			case GameConstants.WIK_YYWHZ_WAI:	
			case GameConstants.WIK_YYWHZ_KAN:
			case GameConstants.WIK_PENG:
				if(color_hei(cbCenterCard)){
					return false;
				}
				break;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_YYWHZ_SHUNZI:
				boolean is_hong=false;
				for(int j = 0; j<3; j++ ){
					if(color_hei(analyseItem.cbCardData[i][j])==false){
						is_hong=true;
					}
					 
				}
				if(!is_hong){
					return false;
				}
				break;

			}
				
		}
		int cbCardEye = analyseItem.cbCardEye;
		int cbMenEye[]=new int[2];

		cbMenEye[0]=analyseItem.cbMenEye[0];
		cbMenEye[1]=analyseItem.cbMenEye[1];
		if(cbCardEye > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbCardEye-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[0]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[0]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if(cbMenEye[1]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
			cbMenEye[1]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
		}
		if((analyseItem.curCardEye!=false)&&(color_hei(cbCardEye))){
			return false;
		}
		if(cbMenEye[0] != 0){
			if(color_hei(cbMenEye[0]) && color_hei(cbMenEye[1])){
				return false;
			}
		}
		return true;
	}
	/**
	 * 落地牌 是否满足
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_pengpeng_hu_down(WeaveItem weaveItems[], int weaveCount) {
		boolean isPengPengHu = true;
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind != GameConstants.WIK_PENG
					&& weaveItems[i].weave_kind != GameConstants.WIK_GANG
					&& weaveItems[i].weave_kind != GameConstants.WIK_ZHAO)) {
				isPengPengHu = false;
				break;
			}
		}
		return isPengPengHu;
	}

	/**
	 * 落地牌 是否暗 杠 明杠
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_an_gang(WeaveItem weaveItems[], int weaveCount) {
		boolean isAnGang = true;
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind != GameConstants.WIK_GANG
					&& weaveItems[i].weave_kind != GameConstants.WIK_ZHAO)) {
				isAnGang = false;
				break;
			}
			if (weaveItems[i].getPublic_card() == 1) {
				isAnGang = false;
				break;
			}
		}
		return isAnGang;
	}

	/**
	 * 判断牌眼是否有成句
	 * 
	 * @param cards_index
	 * @return
	 */
	public boolean isChengJu(int cards_index[], AnalyseItem analyseItem) {

		int cbMagicCardIndex[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int num = cbMagicCardIndex[i] = cards_index[i];
			if (num > 0) {
				for (int j = 0; j < num; j++) {
//					for (int k = 0; k < 4; k++) {
//						if (analyseItem.cbCardEye[k] == 0) {
//							analyseItem.cbCardEye[k] = switch_to_card_data(i);// 复制牌眼
//							break;
//						}
//					}
				}
			}
		}

		int mj_count = GameConstants.MAX_HH_INDEX;
		for (int i = 0; i < mj_count; i++) {
			// 同牌判断
			if (cbMagicCardIndex[i] == 3) {
				// if (analyseItem != null) {
				// analyseItem.cbWeaveKind[analyseItem.cbWeaveKind.length - 1] =
				// GameConstants.WIK_PENG;
				// analyseItem.cbCenterCard[analyseItem.cbWeaveKind.length - 1]
				// = switch_to_card_data(i);
				// analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][0]
				// = switch_to_card_data(i);
				// analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][1]
				// = switch_to_card_data(i);
				// analyseItem.cbCardData[analyseItem.cbWeaveKind.length - 1][2]
				// = switch_to_card_data(i);
				// }
				analyseItem.eyeKind = GameConstants.WIK_PENG;
				analyseItem.eyeCenterCard = switch_to_card_data(i);
				return true;
			} // 同牌判断 end
				// 连牌判断
			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && ((i % 3) == 0)) {
				// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
				int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] + cbMagicCardIndex[i + 2];
				if (chi_count >= 3) {
					if (cbMagicCardIndex[i] >= 1 && cbMagicCardIndex[i + 1] >= 1 && cbMagicCardIndex[i + 2] >= 1) {
						// if (analyseItem != null) {
						// analyseItem.cbWeaveKind[analyseItem.cbWeaveKind.length
						// - 1] = GameConstants.WIK_LEFT;
						// analyseItem.cbCenterCard[analyseItem.cbWeaveKind.length
						// - 1] = switch_to_card_data(i);
						// analyseItem.cbCardData[analyseItem.cbWeaveKind.length
						// - 1][0] = switch_to_card_data(i);
						// analyseItem.cbCardData[analyseItem.cbWeaveKind.length
						// - 1][1] = switch_to_card_data(i + 1);
						// analyseItem.cbCardData[analyseItem.cbWeaveKind.length
						// - 1][2] = switch_to_card_data(i + 2);
						// }
						analyseItem.eyeKind = GameConstants.WIK_LEFT;
						analyseItem.eyeCenterCard = switch_to_card_data(i);
						return true;
					}
				}
			} // 连牌判断 end
		}
		return false;
	}

	/**
	 * 判断牌眼是否成双
	 * 
	 * @param cards_index
	 * @param cardEyes
	 * @return
	 */
	public boolean isChengShuang(int cards_index[], AnalyseItem analyseItem) {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		boolean isShuang = true;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int num = cards_index[i];

			if (num > 0) {

//				for (int j = 0; j < num; j++) {
//					for (int k = 0; k < 4; k++) {
//						if (analyseItem.cbCardEye[k] == 0) {
//							analyseItem.cbCardEye[k] = switch_to_card_data(i);// 复制牌眼
//							break;
//						}
//					}
//				}

				if (num != 2) {
					isShuang = false;// 判断牌眼是否 双对
				}

				int card_data = switch_to_card_data(i);
				int color = get_card_color(card_data);
				Integer value = map.get(color);
				if (value == null)
					value = 0;
				map.put(color, value + num);

			}
		}
		analyseItem.isShuangDui = isShuang;
//		for (Entry<Integer, Integer> entry : map.entrySet()) {
//			if (entry.getValue() == 0)
//				continue;
//			if (entry.getValue() != 2 && entry.getValue() != 4) {
//				return false;
//			}
//		}
		return true;
	}

	/**
	 * 只处理牌眼 4张的情况
	 * 
	 * @param cards_index
	 * @param cardEyes
	 * @return
	 */
	public boolean isYankou(int cards_index[], AnalyseItem analyseItem) {

		int cbCardCount = get_card_count_by_index(cards_index);

		if (cbCardCount == 1)
			return true;

		if (cbCardCount != 4) {
			return false;
		}

		boolean chengshuang = isChengShuang(cards_index, analyseItem);

		if (chengshuang)
			return true;

		boolean chengju = isChengJu(cards_index, analyseItem);

		return chengju;
	}

	
	public boolean isInCardEye(AnalyseItem analyseItem, int cur_card) {
		boolean isEyes = false;
//		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
//			if (analyseItem.cbCardEye[i] == cur_card) {
//				isEyes = true;
//				break;
//			}
//		}
		return isEyes;
	}
	
	/**
	 * 当前摸牌 是不是牌眼
	 * 
	 * @param cardEyes
	 * @param cur_card
	 * @return
	 */
	private boolean isCurCardEye(AnalyseItem analyseItem, int cur_card) {
		boolean isEyes = false;
//		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
//			if (analyseItem.cbCardEye[i] == cur_card) {
//				isEyes = true;
//				break;
//			}
//		}

		if ((analyseItem.eyeKind == GameConstants.WIK_LEFT || analyseItem.eyeKind == GameConstants.WIK_PENG) && isEyes
				&& isCurCard(cur_card, analyseItem.eyeCenterCard)) {// 牌眼一张
																	// 单调才是满天飞
			return true;
		}
		return false;
	}

	private boolean isCurCard(int cur_card, int eyeCenterCard) {
		if (cur_card == eyeCenterCard)
			return false;

		int index = switch_to_card_index(cur_card);

		int eyeindex = switch_to_card_index(eyeCenterCard);

		if (index == eyeindex + 1 || index == eyeindex + 2)
			return false;

		return true;
	}

	// 分析扑克
	public boolean analyse_card_yywhz(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index,int cur_card,
				List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[],boolean yws_type) {
			// 计算数目
			int cbCardCount = get_card_count_by_index(cards_index);
			hu_xi[0] = 0;
	


			// 需求判断
			if(cbCardCount == 0)
				return false;
			int cbLessKindItem = (cbCardCount ) / 3;
			boolean bNeedCardEye=((cbCardCount+1)%3==0);
			if(cbCardCount%3==1)
				return false;
		
			// 单吊判断
			if ((cbLessKindItem == 0)&&(bNeedCardEye == true)) {
				for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++){
					if(cards_index[i]==2){
						
						// 变量定义
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem.curCardEye = true;
						analyseItem.cbCardEye = cur_card+GameConstants.CARD_ESPECIAL_TYPE_NIAO;

						int count = 0;
						// 设置结果
						for (int j = 0; j < cbWeaveCount; j++) {
							analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
							analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
							analyseItem.hu_xi[j] = weaveItem[j].hu_xi;
							hu_xi[0] += analyseItem.hu_xi[j] ;
							get_weave_card_yywhz(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
							count ++;
						}

						analyseItem.cbCardEye = switch_to_card_data(i);
						if (cards_index[i] < 2 || this.is_magic_index(i) == true)
							analyseItem.bMagicEye = true;
						else
							analyseItem.bMagicEye = false;

						// 插入结果
						analyseItemArray.add(analyseItem);

					}else if(cards_index[i] > 0){
						int cbMenEye[]=new int[2];
						for(int j=0;j<2;j++){
							cbMenEye[j]=0;
						}
						//顺子判断
						if((i<(GameConstants.MAX_HH_INDEX-2))&&(cards_index[i]>0)&&((i%10)<=7)){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+1] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+1);
									break;
								}
								if((cards_index[i+2] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+2);
									break;
								}
							}
						}
						
						//顺子判断
						if(cards_index[i]>0&&(i%10)==8){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+1] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+1);
									break;
								}
								if((cards_index[i-1] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i-1);
									break;
								}
							}
						}
						
						if(cards_index[i]>0&&(i%10)==1){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+5] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+5);
									break;
								}
								if((cards_index[i+8] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+8);
									break;
								}
							}
						}
						if(cards_index[i]>0&&(i%10)==6){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+3] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+3);
									break;
								}
							}
						}
						if(cbMenEye[0]!=0){
							// 变量定义
							AnalyseItem analyseItem = new AnalyseItem();

							int count = 0;
							// 设置结果
							for (int j = 0; j < cbWeaveCount; j++) {
								analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
								analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
								analyseItem.hu_xi[j] = weaveItem[j].hu_xi;
								hu_xi[0] += analyseItem.hu_xi[j] ;
								get_weave_card_yywhz(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
								count ++;
							}


							if(cbMenEye[0]!=0){
								analyseItem.cbMenEye[0]=cbMenEye[0];
								analyseItem.cbMenEye[1]=cbMenEye[1];
							}
							// 插入结果
							analyseItemArray.add(analyseItem);
						}
						
						

					}
				}
				
				return (analyseItemArray.size() > 0 ? true : false);
			} // 单吊判断 end


			// 变量定义
			int cbKindItemCount = 0;
			KindItem kindItem[] = new KindItem[76];
			for (int i = 0; i < kindItem.length; i++) {
				kindItem[i] = new KindItem();
			}

			for (int i = 0; i < kindItem.length; i++) {
				kindItem[i] = new KindItem();
			}
			
			
			// 拆分分析
			
			for(int i = 0; i<GameConstants.MAX_HH_INDEX; i++){
				if(cards_index[i]== 0) continue;
				int card_date  = switch_to_card_data(i);
				if(cards_index[i] >= 3){
					if((cur_card == switch_to_card_data(i))){
						if( seat_index != provider_index){
							if(cards_index[i] >= 4){
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i;
								kindItem[cbKindItemCount].cbCardIndex[2] = i;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i;
								kindItem[cbKindItemCount].cbValidIndex[2] = i;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_KAN;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								cbKindItemCount++;
							}else{
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i;
								kindItem[cbKindItemCount].cbCardIndex[2] = i;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i;
								kindItem[cbKindItemCount].cbValidIndex[2] = i;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								cbKindItemCount++;
							}
							
						}else if(cards_index[i] >= 3){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i;
							kindItem[cbKindItemCount].cbCardIndex[2] = i;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i;
							kindItem[cbKindItemCount].cbValidIndex[2] = i;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_WAI;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}else{
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i;
						kindItem[cbKindItemCount].cbCardIndex[2] = i;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i;
						kindItem[cbKindItemCount].cbValidIndex[2] = i;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_KAN;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}

				}

				//顺子判断
				
				if((i<(GameConstants.MAX_HH_INDEX-2))&&(cards_index[i]>0)&&((i%10)<=7)){
					for(int j = 1;j <= cards_index[i];j++){
						if((cards_index[i+1] >= j)&&(cards_index[i+2] >= j) 
						&& (cur_card == switch_to_card_data(i) || cur_card == switch_to_card_data(i+1) || cur_card == switch_to_card_data(i+2))){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+2;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+1;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+2;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
							if(cards_index[switch_to_card_index(cur_card)] >= 2){
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i+1;
								kindItem[cbKindItemCount].cbCardIndex[2] = i+2;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i+1;
								kindItem[cbKindItemCount].cbValidIndex[2] = i+2;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								cbKindItemCount++;
							}
						}else if((cards_index[i+1] >= j)&&(cards_index[i+2] >= j)){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+2;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+1;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+2;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}
				}
				
				if((card_date&GameConstants.LOGIC_MASK_VALUE) == 0x02){
					for(int j = 1;j <= cards_index[i];j++){
						if(cards_index[i+5] >= j && cards_index[i+8] >= j
						&& (cur_card == switch_to_card_data(i) || cur_card == switch_to_card_data(i+5) || cur_card == switch_to_card_data(i+8))){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+5;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+8;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+5;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+8;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
							if(cards_index[switch_to_card_index(cur_card)] >= 2){
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i+5;
								kindItem[cbKindItemCount].cbCardIndex[2] = i+8;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i+5;
								kindItem[cbKindItemCount].cbValidIndex[2] = i+8;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI_EQS;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								cbKindItemCount++;
							}
						}else if(cards_index[i+5] >= j && cards_index[i+8] >= j){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+5;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+8;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+5;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+8;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI_EQS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}

				}

			}
			// 组合分析
			
			if (cbKindItemCount >= cbLessKindItem) {
				// 变量定义
				int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

				int cbIndex[] = new int[] { 0, 1, 2, 3, 4 ,5, 6};
				KindItem pKindItem[] = new KindItem[cbIndex.length];
				for (int i = 0; i < cbIndex.length; i++) {
					pKindItem[i] = new KindItem();
				}
				// 把剩余需要判断的组合开始分析 组合
				// 开始组合
				do {
					int hh_count = GameConstants.MAX_HH_INDEX;
					// 设置变量
					for (int i = 0; i < hh_count; i++) {
						cbCardIndexTemp[i] = cards_index[i];
					}
					for (int i = 0; i < cbLessKindItem; i++) {
						pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
						pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
						for (int j = 0; j < 3; j++) {
							pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
							pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
						}
					}
					if(cbIndex[1] == 4 && cbIndex[2] == 5 && cbIndex[3] == 6&& cbIndex[4] == 8&& cbIndex[5] == 10){
						int a=0;
						a++;
					}
					// 数量判断
					boolean bEnoughCard = true;
					for (int i = 0; i < cbLessKindItem * 3; i++) {
						// 存在判断
						int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];
						
						if(cbCardIndexTemp[cbCardIndex] == 0)
						{
							bEnoughCard = false;
							break;
						} else
							cbCardIndexTemp[cbCardIndex]--;
					}
					
					// 胡牌判断
					if (bEnoughCard == true) {
						
						int cbCardEye = 0;
						int cbMenEye[]=new int[2];
						for(int i=0;i<2;i++){
							cbMenEye[i]=0;
						}
						if(bNeedCardEye == true){
							for (int i = 0; i < hh_count; i++) {
								if (cbCardIndexTemp[i] == 2) {
									cbCardEye = switch_to_card_data(i);// 牌眼				
									break;
								} 
								//顺子判断
								if((i<(GameConstants.MAX_HH_INDEX-2))&&(cbCardIndexTemp[i]>0)&&((i%10)<=7)){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+1] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+1);
											break;
										}
										if((cbCardIndexTemp[i+2] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+2);
											break;
										}
									}
								}
								
								//顺子判断
								if(cbCardIndexTemp[i]>0&&(i%10)==8){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+1] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+1);
											break;
										}
										if((cbCardIndexTemp[i-1] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i-1);
											break;
										}
									}
								}
								
								if(cbCardIndexTemp[i]>0&&(i%10)==1){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+5] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+5);
											break;
										}
										if((cbCardIndexTemp[i+8] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+8);
											break;
										}
									}
								}
								if(cbCardIndexTemp[i]>0&&(i%10)==6){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+3] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+3);
											break;
										}
									}
								}
								
							}
						}
						hu_xi[0] = 0;
						if ((bNeedCardEye == false) || (cbCardEye != 0) || cbMenEye[0]!=0) {
							// 牌眼判断
							AnalyseItem analyseItem = new AnalyseItem();
							// 设置组合
							int count = 0;
							for (int i = 0; i < cbWeaveCount; i++) {
								analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
								analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
								analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
								get_weave_card_yywhz(weaveItem[i].weave_kind, weaveItem[i].center_card,
										analyseItem.cbCardData[i]);
								count ++;
							}
							// 设置牌型
							for (int i = 0; i < cbLessKindItem; i++) {
								analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
								analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
								WeaveItem weave_item = new WeaveItem();
								weave_item.weave_kind  = pKindItem[i].cbWeaveKind;
								weave_item.center_card = pKindItem[i].cbCenterCard;
								analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(
										pKindItem[i].cbValidIndex[0]);
								analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(
										pKindItem[i].cbValidIndex[1]);
								analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(
										pKindItem[i].cbValidIndex[2]);
								count ++;
								
							}
							// 插入结果


							if(cbCardEye != 0 )
							{
								analyseItem.curCardEye = true;
								analyseItem.cbCardEye = cbCardEye;

							}
							if(cbMenEye[0]!=0){
								analyseItem.cbMenEye[0]=cbMenEye[0];
								analyseItem.cbMenEye[1]=cbMenEye[1];
							}
							// 插入结果
							analyseItemArray.add(analyseItem);
						}
					}

					// 设置索引
					if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
						int i = cbLessKindItem - 1;
						for (; i > 0; i--) {
							if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
								int cbNewIndex = cbIndex[i - 1];
								for (int j = (i - 1); j < cbLessKindItem; j++)
									cbIndex[j] = cbNewIndex + j - i + 2;
								break;
							}
						}
						if (i == 0)
							break;
					} else
						cbIndex[cbLessKindItem - 1]++;
				} while (true);
			}
			
			
			return (analyseItemArray.size() > 0 ? true : false);
	}
	// 分析扑克
	public boolean analyse_card_yiyangwhz(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provider_index,int cur_card,
				List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[],boolean yws_type) {
			// 计算数目
			int cbCardCount = get_card_count_by_index(cards_index);
			hu_xi[0] = 0;
	


			// 需求判断
			if(cbCardCount == 0)
				return false;
			int cbLessKindItem = (cbCardCount ) / 3;
			boolean bNeedCardEye=((cbCardCount+1)%3==0);
			if(cbCardCount%3==1)
				return false;
		
			// 单吊判断
			if ((cbLessKindItem == 0)&&(bNeedCardEye == true)) {
				for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++){
					if(cards_index[i]==2){
						
						// 变量定义
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem.curCardEye = true;
						

						int count = 0;
						// 设置结果
						for (int j = 0; j < cbWeaveCount; j++) {
							analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
							analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
							analyseItem.hu_xi[j] = weaveItem[j].hu_xi;
							hu_xi[0] += analyseItem.hu_xi[j] ;
							get_weave_card_yiyangwhz(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
							count ++;
						}

						analyseItem.cbCardEye = switch_to_card_data(i);
						if (cards_index[i] < 2 || this.is_magic_index(i) == true)
							analyseItem.bMagicEye = true;
						else
							analyseItem.bMagicEye = false;

						if(analyseItem.cbCardEye == cur_card){
							analyseItem.cbCardEye+=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
						}
						// 插入结果
						analyseItemArray.add(analyseItem);

					}else if(cards_index[i] > 0){
						int cbMenEye[]=new int[2];
						for(int j=0;j<2;j++){
							cbMenEye[j]=0;
						}
						//顺子判断
						if((i<(GameConstants.MAX_HH_INDEX-2))&&(cards_index[i]>0)&&((i%10)<=7)){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+1] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+1);
									break;
								}
								if((cards_index[i+2] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+2);
									break;
								}
							}
						}
						
						//顺子判断
						if(cards_index[i]>0&&(i%10)==8){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+1] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+1);
									break;
								}
								if((cards_index[i-1] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i-1);
									break;
								}
							}
						}
						
						if(cards_index[i]>0&&(i%10)==1){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+5] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+5);
									break;
								}
								if((cards_index[i+8] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+8);
									break;
								}
							}
						}
						if(cards_index[i]>0&&(i%10)==6){
							for(int j = 1;j <= cards_index[i];j++){
								if((cards_index[i+3] >= j)){
									cbMenEye[0]=switch_to_card_data(i);
									cbMenEye[1]=switch_to_card_data(i+3);
									break;
								}
							}
						}
						if(cbMenEye[0]!=0){
							// 变量定义
							AnalyseItem analyseItem = new AnalyseItem();

							if(cbMenEye[0] == cur_card){
								cbMenEye[0]+=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
							}
							if(cbMenEye[1] == cur_card){
								cbMenEye[1]+=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
							}
							int count = 0;
							// 设置结果
							for (int j = 0; j < cbWeaveCount; j++) {
								analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
								analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
								analyseItem.hu_xi[j] = weaveItem[j].hu_xi;
								hu_xi[0] += analyseItem.hu_xi[j] ;
								get_weave_card_yiyangwhz(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
								count ++;
							}

							if(cbMenEye[0]!=0){
								analyseItem.cbMenEye[0]=cbMenEye[0];
								analyseItem.cbMenEye[1]=cbMenEye[1];
							}
							// 插入结果
							analyseItemArray.add(analyseItem);
						}

					}
				}
				
				return (analyseItemArray.size() > 0 ? true : false);
			} // 单吊判断 end


			// 变量定义
			int cbKindItemCount = 0;
			KindItem kindItem[] = new KindItem[76];
			for (int i = 0; i < kindItem.length; i++) {
				kindItem[i] = new KindItem();
			}
			
			
			// 拆分分析
			
			for(int i = 0; i<GameConstants.MAX_HH_INDEX; i++){
				if(cards_index[i]== 0) continue;
				int card_date  = switch_to_card_data(i);
				if(cards_index[i] >= 3){
					if((cur_card == switch_to_card_data(i))){
						if( seat_index != provider_index){
							if(cards_index[i] >= 4){
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i;
								kindItem[cbKindItemCount].cbCardIndex[2] = i;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i;
								kindItem[cbKindItemCount].cbValidIndex[2] = i;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_KAN;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								cbKindItemCount++;
							}else{
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i;
								kindItem[cbKindItemCount].cbCardIndex[2] = i;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i;
								kindItem[cbKindItemCount].cbValidIndex[2] = i;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								kindItem[cbKindItemCount].cbCenterCard +=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
								cbKindItemCount++;
							}
							
						}else if(cards_index[i] >= 3){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i;
							kindItem[cbKindItemCount].cbCardIndex[2] = i;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i;
							kindItem[cbKindItemCount].cbValidIndex[2] = i;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_WAI;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbCenterCard +=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
							cbKindItemCount++;
						}
					}else{
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i;
						kindItem[cbKindItemCount].cbCardIndex[2] = i;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i;
						kindItem[cbKindItemCount].cbValidIndex[2] = i;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_KAN;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}

				}

				//顺子判断
				
				if((i<(GameConstants.MAX_HH_INDEX-2))&&(cards_index[i]>0)&&((i%10)<=7)){
					for(int j = 1;j <= cards_index[i];j++){
						if((cards_index[i+1] >= j)&&(cards_index[i+2] >= j) 
						&& (cur_card == switch_to_card_data(i) || cur_card == switch_to_card_data(i+1) || cur_card == switch_to_card_data(i+2))){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+2;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+1;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+2;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbCenterCard +=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
							cbKindItemCount++;
							if(cards_index[switch_to_card_index(cur_card)] >= 2){
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i+1;
								kindItem[cbKindItemCount].cbCardIndex[2] = i+2;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i+1;
								kindItem[cbKindItemCount].cbValidIndex[2] = i+2;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								cbKindItemCount++;
							}
						}else if((cards_index[i+1] >= j)&&(cards_index[i+2] >= j)){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+2;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+1;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+2;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}
				}
				
				if((card_date&GameConstants.LOGIC_MASK_VALUE) == 0x02){
					for(int j = 1;j <= cards_index[i];j++){
						if(cards_index[i+5] >= j && cards_index[i+8] >= j
						&& (cur_card == switch_to_card_data(i) || cur_card == switch_to_card_data(i+5) || cur_card == switch_to_card_data(i+8))){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+5;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+8;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+5;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+8;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbCenterCard +=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
							cbKindItemCount++;
							if(cards_index[switch_to_card_index(cur_card)] >= 2){
								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i+5;
								kindItem[cbKindItemCount].cbCardIndex[2] = i+8;
								kindItem[cbKindItemCount].cbValidIndex[0] = i;
								kindItem[cbKindItemCount].cbValidIndex[1] = i+5;
								kindItem[cbKindItemCount].cbValidIndex[2] = i+8;
								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI_EQS;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
								cbKindItemCount++;
							}
						}else if(cards_index[i+5] >= j && cards_index[i+8] >= j){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i+5;
							kindItem[cbKindItemCount].cbCardIndex[2] = i+8;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i+5;
							kindItem[cbKindItemCount].cbValidIndex[2] = i+8;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YYWHZ_SHUNZI_EQS;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}

				}

			}
			// 组合分析
			
			if (cbKindItemCount >= cbLessKindItem) {
				// 变量定义
				int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

				int cbIndex[] = new int[] { 0, 1, 2, 3, 4 ,5, 6};
				KindItem pKindItem[] = new KindItem[cbIndex.length];
				for (int i = 0; i < cbIndex.length; i++) {
					pKindItem[i] = new KindItem();
				}
				// 把剩余需要判断的组合开始分析 组合
				// 开始组合
				do {
					int hh_count = GameConstants.MAX_HH_INDEX;
					// 设置变量
					for (int i = 0; i < hh_count; i++) {
						cbCardIndexTemp[i] = cards_index[i];
					}
					for (int i = 0; i < cbLessKindItem; i++) {
						pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
						pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
						for (int j = 0; j < 3; j++) {
							pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
							pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
						}
					}
					if(cbIndex[1] == 4 && cbIndex[2] == 5 && cbIndex[3] == 6&& cbIndex[4] == 8&& cbIndex[5] == 10){
						int a=0;
						a++;
					}
					// 数量判断
					boolean bEnoughCard = true;
					for (int i = 0; i < cbLessKindItem * 3; i++) {
						// 存在判断
						int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];
						
						if(cbCardIndexTemp[cbCardIndex] == 0)
						{
							bEnoughCard = false;
							break;
						} else
							cbCardIndexTemp[cbCardIndex]--;
					}
					
					// 胡牌判断
					if (bEnoughCard == true) {
						
						int cbCardEye = 0;
						int cbMenEye[]=new int[2];
						for(int i=0;i<2;i++){
							cbMenEye[i]=0;
						}
						if(bNeedCardEye == true){
							for (int i = 0; i < hh_count; i++) {
								if (cbCardIndexTemp[i] == 2) {
									cbCardEye = switch_to_card_data(i);// 牌眼	
									if(cur_card == cbCardEye){
										cbCardEye+=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
									}
									break;
								} 
								//顺子判断
								if((i<(GameConstants.MAX_HH_INDEX-2))&&(cbCardIndexTemp[i]>0)&&((i%10)<=7)){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+1] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+1);
											break;
										}
										if((cbCardIndexTemp[i+2] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+2);
											break;
										}
									}
								}
								
								//顺子判断
								if(cbCardIndexTemp[i]>0&&(i%10)==8){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+1] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+1);
											break;
										}
										if((cbCardIndexTemp[i-1] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i-1);
											break;
										}
									}
								}
								
								if(cbCardIndexTemp[i]>0&&(i%10)==1){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+5] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+5);
											break;
										}
										if((cbCardIndexTemp[i+8] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+8);
											break;
										}
									}
								}
								if(cbCardIndexTemp[i]>0&&(i%10)==6){
									for(int j = 1;j <= cbCardIndexTemp[i];j++){
										if((cbCardIndexTemp[i+3] >= j)){
											cbMenEye[0]=switch_to_card_data(i);
											cbMenEye[1]=switch_to_card_data(i+3);
											break;
										}
									}
								}
								
							}
						}
						hu_xi[0] = 0;
						if ((bNeedCardEye == false) || (cbCardEye != 0) || cbMenEye[0]!=0) {
							// 牌眼判断
							AnalyseItem analyseItem = new AnalyseItem();
							// 设置组合
							int count = 0;
							for (int i = 0; i < cbWeaveCount; i++) {
								analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
								analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
								analyseItem.hu_xi[i] = weaveItem[i].hu_xi;
								get_weave_card_yiyangwhz(weaveItem[i].weave_kind, weaveItem[i].center_card,
										analyseItem.cbCardData[i]);
								count ++;
							}
							// 设置牌型
							for (int i = 0; i < cbLessKindItem; i++) {
								analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
								analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
								WeaveItem weave_item = new WeaveItem();
								weave_item.weave_kind  = pKindItem[i].cbWeaveKind;
								weave_item.center_card = pKindItem[i].cbCenterCard;
								analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(
										pKindItem[i].cbValidIndex[0]);
								analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(
										pKindItem[i].cbValidIndex[1]);
								analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(
										pKindItem[i].cbValidIndex[2]);
								count ++;
								
							}
							// 插入结果


							if(cbCardEye != 0 )
							{
								analyseItem.curCardEye = true;
								analyseItem.cbCardEye = cbCardEye;

							}
							
							
							//歪胡
							boolean is_wai_hu=false;
							for(int i=0;i<7;i++){
								if(analyseItem.cbCenterCard[i]>GameConstants.CARD_ESPECIAL_TYPE_NIAO && analyseItem.cbWeaveKind[i] == GameConstants.WIK_YYWHZ_WAI){	
									for(int j=0;j<7;j++){
										if(i==j){
											continue;
										}
										if(analyseItem.cbCenterCard[j]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
											if(GameConstants.WIK_LEFT == analyseItem.cbWeaveKind[j]){
												analyseItem.cbWeaveKind[j]=GameConstants.WIK_YYWHZ_SHUNZI;
											}
											if(GameConstants.WIK_EQS == analyseItem.cbWeaveKind[j]){
												analyseItem.cbWeaveKind[j]=GameConstants.WIK_YYWHZ_SHUNZI_EQS;
											}
											analyseItem.cbCenterCard[j]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
										}
									}
									is_wai_hu=true;
									break;
								}
							}
							
							boolean is_cur_data=false;
							if(!is_wai_hu){
								for(int i=0;i<7;i++){
									if(analyseItem.cbCenterCard[i]>GameConstants.CARD_ESPECIAL_TYPE_NIAO && !is_cur_data){	
										is_cur_data=true;
									}else if(analyseItem.cbCenterCard[i]>GameConstants.CARD_ESPECIAL_TYPE_NIAO && is_cur_data){
										if(GameConstants.WIK_LEFT == analyseItem.cbWeaveKind[i]){
											analyseItem.cbWeaveKind[i]=GameConstants.WIK_YYWHZ_SHUNZI;
										}
										if(GameConstants.WIK_EQS == analyseItem.cbWeaveKind[i]){
											analyseItem.cbWeaveKind[i]=GameConstants.WIK_YYWHZ_SHUNZI_EQS;
										}
										analyseItem.cbCenterCard[i]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
									}
								}
							}
							
							//单吊对子，单吊门子
							if(cbMenEye[0]!=0 && !is_wai_hu){
								if(cbMenEye[0] == cur_card){
									cbMenEye[0]+=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
								}
								if(cbMenEye[1] == cur_card){
									cbMenEye[1]+=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
								}
								analyseItem.cbMenEye[0]=cbMenEye[0];
								analyseItem.cbMenEye[1]=cbMenEye[1];
							}
							if(cbCardEye > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
								for(int i=0;i<7;i++){
									if(analyseItem.cbCenterCard[i]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
										if(GameConstants.WIK_LEFT == analyseItem.cbWeaveKind[i]){
											analyseItem.cbWeaveKind[i]=GameConstants.WIK_YYWHZ_SHUNZI;
										}
										if(GameConstants.WIK_EQS == analyseItem.cbWeaveKind[i]){
											analyseItem.cbWeaveKind[i]=GameConstants.WIK_YYWHZ_SHUNZI_EQS;
										}
										analyseItem.cbCenterCard[i]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
									}
								}
							}
							if(cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO
							|| cbMenEye[1]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
								for(int i=0;i<7;i++){
									if(analyseItem.cbCenterCard[i]>GameConstants.CARD_ESPECIAL_TYPE_NIAO){
										if(GameConstants.WIK_LEFT == analyseItem.cbWeaveKind[i]){
											analyseItem.cbWeaveKind[i]=GameConstants.WIK_YYWHZ_SHUNZI;
										}
										if(GameConstants.WIK_EQS == analyseItem.cbWeaveKind[i]){
											analyseItem.cbWeaveKind[i]=GameConstants.WIK_YYWHZ_SHUNZI_EQS;
										}
										analyseItem.cbCenterCard[i]-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
									}
								}
							}
							
							analyseItem.cbMenEye[0]=cbMenEye[0];
							analyseItem.cbMenEye[1]=cbMenEye[1];
							
							// 插入结果
							analyseItemArray.add(analyseItem);
						}
					}

					// 设置索引
					if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
						int i = cbLessKindItem - 1;
						for (; i > 0; i--) {
							if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
								int cbNewIndex = cbIndex[i - 1];
								for (int j = (i - 1); j < cbLessKindItem; j++)
									cbIndex[j] = cbNewIndex + j - i + 2;
								break;
							}
						}
						if (i == 0)
							break;
					} else
						cbIndex[cbLessKindItem - 1]++;
				} while (true);
			}
			
			
			return (analyseItemArray.size() > 0 ? true : false);
	}
	
	public boolean wudui(int cards_index[]){
		int dui_count=0;
		for(int i=0;i<GameConstants.MAX_HH_INDEX;i++){
			if(cards_index[i] == 2){
				dui_count++;
			}
			if(cards_index[i] == 3){
				dui_count++;
			}
			if(cards_index[i] == 4){
				dui_count+=2;
			}
		}
		if(dui_count == 0 ){
			return true;
		}
		return false;
	}
	public boolean shidui(int cards_index[]){
		int dui_count=0;
		for(int i=0;i<GameConstants.MAX_HH_INDEX;i++){
			if(cards_index[i] == 2){
				dui_count++;
			}
			if(cards_index[i] == 3){
				dui_count++;
			}
			if(cards_index[i] == 4){
				dui_count+=2;
			}
		}
		if(dui_count == 10){
			return true;
		}
		return false;
	}
	public boolean yidui(int cards_index[]){
		int dui_count=0;
		int liu_count=0;
		for(int i=0;i<GameConstants.MAX_HH_INDEX;i++){
			if(cards_index[i] == 2){
				dui_count++;
			}
			if(cards_index[i] == 3){
				dui_count++;
			}
			if(cards_index[i] == 4){
				dui_count+=2;
				liu_count++;
			}
		}
		if(dui_count == 2 && liu_count == 1){
			dui_count=1;
		}
		if(dui_count == 1){
			return true;
		}
		return false;
	}
	public boolean jiudui(int cards_index[]){
		int dui_count=0;
		for(int i=0;i<GameConstants.MAX_HH_INDEX;i++){
			if(cards_index[i] == 2){
				dui_count++;
			}
			if(cards_index[i] == 3){
				dui_count++;
			}
			if(cards_index[i] == 4){
				dui_count+=2;
			}
		}
		if( dui_count == 9){
			return true;
		}
		return false;
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		//	溜等级 
		if(player_action == GameConstants.WIK_YYWHZ_LIU_WAI )
		{
			return 50;
		}
		// 溜等级 
		if (player_action == GameConstants.WIK_YYWHZ_LIU_NEI) {
			return 50;
		}
		
		//歪操作
		if (player_action == GameConstants.WIK_YYWHZ_WAI) {
			return 40;
		}
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 30;
		}
		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER
				|| player_action == GameConstants.WIK_LEFT||player_action == GameConstants.WIK_XXD
				|| player_action == GameConstants.WIK_DDX || player_action == GameConstants.WIK_EQS|| player_action == GameConstants.WIK_YWS) {
			return 10;
		}


		return 0;
	}
	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank_yiyang(int player_action,int seat_index,int provider) {
		//	溜等级 
		if(player_action == GameConstants.WIK_YIYANGWHZ_QING_NEI )
		{
			return 50;
		}
		// 溜等级 
		if (player_action == GameConstants.WIK_YIYANGWHZ_QING_WAI) {
			return 50;
		}
		// 溜等级 
		if (player_action == GameConstants.WIK_YIYANGWHZ_PIAO  ) {
			if(seat_index == provider){
				return 40;
			}else{
				return 20;
			}
			
		}
		
		//歪操作
		if (player_action == GameConstants.WIK_YYWHZ_WAI) {
			return 40;
		}
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 30;
		}
		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER
				|| player_action == GameConstants.WIK_LEFT||player_action == GameConstants.WIK_XXD
				|| player_action == GameConstants.WIK_DDX || player_action == GameConstants.WIK_EQS|| player_action == GameConstants.WIK_YWS) {
			return 10;
		}


		return 0;
	}
	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int MAX_HH_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (MAX_HH_INDEX < index) {
				MAX_HH_INDEX = index;
			}

		}

		return MAX_HH_INDEX;
	}
	// 获取动作序列最高等级
	public int get_action_list_rank_yiyang(int action_count, int action[],int seat_idnex,int provider) {
		int MAX_HH_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank_yiyang(action[i],seat_idnex,provider);
			if (MAX_HH_INDEX < index) {
				MAX_HH_INDEX = index;
			}

		}

		return MAX_HH_INDEX;
	}
	public int get_chi_hu_action_rank_hh(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		if(!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()){
			wFanShu = 2;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()){
			wFanShu = 4;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()){
			wFanShu = 3;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ONE_HEI)).is_empty()){
			wFanShu = 3;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()){
			wFanShu = 5;
		}
	
		return wFanShu;
	}
	public int get_chi_hu_ying_xi_dzb_wmq(int seat_index,ChiHuRight chiHuRight)
	{
		int fanshu = 0;
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()){
			fanshu += 10;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()){
			fanshu += 6;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()){
			fanshu += 8;
		}
		if(fanshu == 0)
			fanshu = 1;
		return fanshu;
	}
	public int get_chi_hu_action_rank_dzb_wmq(int seat_index,int da_pai_count, int xiao_pai_count, int ying_hu_count,int chun_ying_count,int hong_pai_count,ChiHuRight chiHuRight) {
		int hu_xi = 0;
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()){
			hu_xi += 30+30*(hong_pai_count-10);
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()){
			hu_xi += ying_hu_count*30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()){
			hu_xi += chun_ying_count*150;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()){
			hu_xi += 200;
		}
	
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()){
			hu_xi += 40;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()){
			hu_xi += 80;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()){
			hu_xi += 120;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()){
			hu_xi += 300;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()){
			hu_xi += 450;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()){
			hu_xi += 300;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()){
			hu_xi += 300;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()){
			hu_xi += 200;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()){
			hu_xi += 100;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
	
		return hu_xi;
	}

	public int get_chi_hu_ying_xi_xzb_wmq(int seat_index,ChiHuRight chiHuRight)
	{
		int fanshu = 0;
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()){
			fanshu += 10;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()){
			fanshu += 6;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()){
			fanshu += 8;
		}
		if(fanshu == 0)
			fanshu = 1;
		return fanshu;
	}
	public int get_chi_hu_action_rank_xzb_wmq(int seat_index,int da_pai_count, int xiao_pai_count, int ying_hu_count,int chun_ying_count,int hong_pai_count,ChiHuRight chiHuRight) {
		int hu_xi = 0;
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()){
			hu_xi += 30+30*(hong_pai_count-10);
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()){
			hu_xi += ying_hu_count*30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()){
			hu_xi += chun_ying_count*150;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()){
			hu_xi += 200;
		}
	
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()){
			hu_xi += 40;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()){
			hu_xi += 80;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()){
			hu_xi += 120;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()){
			hu_xi += 300;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()){
			hu_xi += 450;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()){
			hu_xi += 300;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()){
			hu_xi += 300;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()){
			hu_xi += 200;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()){
			hu_xi += 100;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()){
			hu_xi += 100;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_LDH_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		return hu_xi;
	}

	public int get_chi_hu_ying_xi_qmt_wmq(int seat_index,ChiHuRight chiHuRight)
	{
		int fanshu = 0;
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()){
			fanshu += 10;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()){
			fanshu += 6;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()){
			fanshu += 8;
		}
		if(fanshu == 0)
			fanshu = 1;
		return fanshu;
	}
	public int get_chi_hu_action_rank_qmt_wmq(int seat_index,int da_pai_count, int xiao_pai_count, int ying_hu_count,int chun_ying_count,int hong_pai_count,ChiHuRight chiHuRight) {
		int hu_xi = 0;
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()){
			hu_xi += 30+30*(hong_pai_count-10);
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()){
			hu_xi += ying_hu_count*30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()){
			hu_xi += chun_ying_count*150;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()){
			hu_xi += 200;
		}
	
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()){
			hu_xi += 40;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()){
			hu_xi += 80;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()){
			hu_xi += 120;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()){
			hu_xi += 300;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()){
			hu_xi += 450;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()){
			hu_xi += 300;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()){
			hu_xi += 300;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()){
			hu_xi += 200;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()){
			hu_xi += 100;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_LDH_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_JIU_DUI_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_SBD_TIAN_HU)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_BIAN_KAN_HU)).is_empty()){
			hu_xi += 30;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BKB_WMQ)).is_empty()){
			hu_xi += 100;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_KA_HU_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_ZHA_DAN_WMQ)).is_empty()){
			hu_xi += 150;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_FBW_WMQ)).is_empty()){
			hu_xi += 50;
		}
		
	
		return hu_xi;
	}
	public int get_chi_hu_ying_xi_lmt_wmq(int seat_index,ChiHuRight chiHuRight)
	{
		int fanshu = 0;
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()){
			fanshu += 4;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()){
			fanshu += 6;
		}
	
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()){
			fanshu += 2;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()){
			fanshu += 3;
		}
		if(fanshu == 0)
			fanshu = 1;
		return fanshu;
	}
	public int get_chi_hu_action_rank_lmt_wmq(int seat_index,int da_pai_count, int xiao_pai_count, int ying_hu_count,int chun_ying_count,int hong_pai_count,ChiHuRight chiHuRight) {
		int hu_xi = 0;
		
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()){
			hu_xi += 10;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()){
			hu_xi += 10+10*(hong_pai_count-10);
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()){
			hu_xi += 50;
		}
		if(!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()){
			hu_xi += 50;
		}	
		return hu_xi;
	}
	
	public int get_chi_hu_action_rank_phz_chd(int seat_index,int da_pai_count, int xiao_pai_count, int tuan_yuan_count,int huang_zhang_count,int hong_pai_count,ChiHuRight chiHuRight) {
		int wFanShu = 0;
		
		if(!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()){
			wFanShu += 3+(hong_pai_count-10);
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()){
			wFanShu += 4;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()){
			wFanShu += 8;
		}
	
		if(!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_DA_HU)).is_empty()){
			wFanShu += 8+(da_pai_count-18);
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()){
			wFanShu += 10+(xiao_pai_count-16);
		}
	
		if(!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()){
			wFanShu += 8;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()){
			wFanShu += 8;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_HUANG_FAN)).is_empty()){
			wFanShu += 1+huang_zhang_count;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_TUAN_YUAN)).is_empty()){
			wFanShu += 8*tuan_yuan_count;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI)).is_empty()){
			wFanShu += 8;
		}
	
		if(wFanShu == 0)
			wFanShu = 1;
	
		return wFanShu;
	}
	public int get_chi_hu_action_rank_thk(int seat_index,ChiHuRight chiHuRight) {
		int wFanShu = 0;
		
		if(!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()){
			wFanShu += 2;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()){
			wFanShu += 5;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()){
			wFanShu += 3;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()){
			wFanShu += 5;
		}
	
		if(wFanShu == 0)
			wFanShu = 1;
	
		return wFanShu;
	}
	
	public int get_chi_hu_action_rank_lhq(int seat_index,ChiHuRight chiHuRight) {
		int wFanShu = 1;
		
		if(!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()){
			wFanShu *= 2;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()){
			wFanShu *= 4;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()){
			wFanShu *= 3;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()){
			wFanShu *= 5;
		}
	
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()){
			wFanShu *= 2;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()){
			wFanShu *= 2;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()){
			wFanShu *= 2;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()){
			wFanShu *= 2;
		}
	
		if(wFanShu == 0)
			wFanShu = 1;
	
		return wFanShu;
	}
	public int get_chi_hu_action_rank_phz_xt(int seat_index,int da_pai_count, int xiao_pai_count, int tuan_yuan_count,int huang_zhang_count,int hong_pai_count,ChiHuRight chiHuRight) {
		int wFanShu = 0;
		
		if(!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()){
			wFanShu += 4;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()){
			wFanShu += 4+(hong_pai_count-13);
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_JIA_DIAN_HU)).is_empty()){
			wFanShu += 4;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()){
			wFanShu += 5;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()){
			wFanShu += 5;
		}
	
		if(!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()){
			wFanShu += 2;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()){
			wFanShu += 6;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_DA_HU)).is_empty()){
			wFanShu += 8+(da_pai_count-18);
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()){
			wFanShu += 6+(xiao_pai_count-16);
		}
	
		if(!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()){
			wFanShu += 4;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()){
			wFanShu += 5;
		}
	
		if(wFanShu == 0)
			wFanShu = 1;
	
		return wFanShu;
	}
	// 获取组合
	public int get_weave_card_yywhz(int cbWeaveKind, int cbCenterCard, int cbCardBuffer[]) {
		// 组合扑克
		switch (cbWeaveKind) {
		case GameConstants.WIK_LEFT: // 左吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard + 1;
			cbCardBuffer[2] = cbCenterCard + 2;

			return 3;
		}
		case GameConstants.WIK_RIGHT: // 右吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard - 2;

			return 3;
		}
		case GameConstants.WIK_CENTER: // 中吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard + 1;

			return 3;
		}
		case GameConstants.WIK_EQS://吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch(cur_card_value)
			{
			case 2:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard+5;
				cbCardBuffer[2] = cbCenterCard+8;
				break;
			}
			case 7:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-5;
				cbCardBuffer[2] = cbCenterCard+3;
				break;
			}
			case 10:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-8;
				cbCardBuffer[2] = cbCenterCard-3;
				break;
			}
			
			}
			return 3;
		}
		case GameConstants.WIK_YWS://吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch(cur_card_value)
			{
			case 1:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard+4;
				cbCardBuffer[2] = cbCenterCard+9;
				break;
			}
			case 5:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-4;
				cbCardBuffer[2] = cbCenterCard+5;
				break;
			}
			case 10:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-9;
				cbCardBuffer[2] = cbCenterCard-5;
				break;
			}
			
			}
			return 3;
		}
    	case GameConstants.WIK_YYWHZ_WAI:
    	case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_PENG: // 碰牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case GameConstants.WIK_YYWHZ_LIU_WAI:
		case GameConstants.WIK_YYWHZ_LIU_NEI:
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		default: {
			//logger.error("get_weave_card:invalid cbWeaveKind" + cbWeaveKind);
		}
		}

		return 0;
	}
	// 获取组合
	public int get_weave_card_yiyangwhz(int cbWeaveKind, int cbCenterCard, int cbCardBuffer[]) {
		// 组合扑克
		switch (cbWeaveKind) {
		case GameConstants.WIK_LEFT: // 左吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard + 1;
			cbCardBuffer[2] = cbCenterCard + 2;

			return 3;
		}
		case GameConstants.WIK_RIGHT: // 右吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard - 2;

			return 3;
		}
		case GameConstants.WIK_CENTER: // 中吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard + 1;

			return 3;
		}
		case GameConstants.WIK_EQS://吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch(cur_card_value)
			{
			case 2:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard+5;
				cbCardBuffer[2] = cbCenterCard+8;
				break;
			}
			case 7:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-5;
				cbCardBuffer[2] = cbCenterCard+3;
				break;
			}
			case 10:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-8;
				cbCardBuffer[2] = cbCenterCard-3;
				break;
			}
			
			}
			return 3;
		}
		case GameConstants.WIK_YWS://吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch(cur_card_value)
			{
			case 1:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard+4;
				cbCardBuffer[2] = cbCenterCard+9;
				break;
			}
			case 5:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-4;
				cbCardBuffer[2] = cbCenterCard+5;
				break;
			}
			case 10:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-9;
				cbCardBuffer[2] = cbCenterCard-5;
				break;
			}
			
			}
			return 3;
		}
    	case GameConstants.WIK_YYWHZ_WAI:
    	case GameConstants.WIK_YYWHZ_KAN:
		case GameConstants.WIK_PENG: // 碰牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case GameConstants.WIK_YIYANGWHZ_QING_NEI:
		case GameConstants.WIK_YIYANGWHZ_QING_WAI:
		case GameConstants.WIK_YIYANGWHZ_PIAO:
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		default: {
			//logger.error("get_weave_card:invalid cbWeaveKind" + cbWeaveKind);
		}
		}

		return 0;
	}
	// 获取组合
	public int get_weave_card(int cbWeaveKind, int cbCenterCard, int cbCardBuffer[]) {
		// 组合扑克
		switch (cbWeaveKind) {
		case GameConstants.WIK_LEFT: // 左吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard + 1;
			cbCardBuffer[2] = cbCenterCard + 2;

			return 3;
		}
		case GameConstants.WIK_RIGHT: // 右吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard - 2;

			return 3;
		}
		case GameConstants.WIK_CENTER: // 中吃类型
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard - 1;
			cbCardBuffer[2] = cbCenterCard + 1;

			return 3;
		}
		case GameConstants.WIK_XXD://吃小
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard+16;

			return 3;
		}
		case GameConstants.WIK_DDX://吃大
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard-16;

			return 3;
		}
		case GameConstants.WIK_EQS://吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch(cur_card_value)
			{
			case 2:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard+5;
				cbCardBuffer[2] = cbCenterCard+8;
				break;
			}
			case 7:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-5;
				cbCardBuffer[2] = cbCenterCard+3;
				break;
			}
			case 10:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-8;
				cbCardBuffer[2] = cbCenterCard-3;
				break;
			}
			
			}
			return 3;
		}
		case GameConstants.WIK_YWS://吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch(cur_card_value)
			{
			case 1:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard+4;
				cbCardBuffer[2] = cbCenterCard+9;
				break;
			}
			case 5:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-4;
				cbCardBuffer[2] = cbCenterCard+5;
				break;
			}
			case 10:
			{
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard-9;
				cbCardBuffer[2] = cbCenterCard-5;
				break;
			}
			
			}
			return 3;
		}
    	case GameConstants.WIK_KAN:
		case GameConstants.WIK_SAO:  //扫牌操作
		case GameConstants.WIK_PENG: // 碰牌操作
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
      	case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case GameConstants.WIK_PAO:
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		case GameConstants.WIK_MENG_XIAO: // 杠牌操作
		case GameConstants.WIK_DIAN_XIAO:
		case GameConstants.WIK_HUI_TOU_XIAO: {
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		default: {
			//logger.error("get_weave_card:invalid cbWeaveKind" + cbWeaveKind);
		}
		}

		return 0;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants.MAX_HH_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = color * 10 + value - 1;
		return index;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data(int card_index) {
		if (card_index >= GameConstants.MAX_HH_INDEX) {
			return GameConstants.MAX_HH_INDEX;
		}
		return ((card_index / 10) << 4) | (card_index % 10 + 1);
	}

	// 扑克转换
	public int switch_to_cards_index(int cards_data[], int start_index, int card_count, int cards_index[]) {
		// 设置变量
		// 转换扑克
		for (int i = 0; i < card_count; i++) {
			cards_index[switch_to_card_index(cards_data[start_index + i])]++;
		}
		return card_count;
	}

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int m = 0; m < this._magic_card_count; m++) {
			for (int i = 0; i < cards_index[this._magic_card_index[m]]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(this._magic_card_index[m]);
			}
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (this.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}

	// 排序,根据牌值排序
	public boolean sort_card_list(int card_data[]) {
		int cbCardCount = card_data.length;
		// 数目过虑
		if (cbCardCount == 0 || cbCardCount > GameConstants.MAX_HH_COUNT)
			return false;

		// 排序操作
		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if (card_data[i] > card_data[i + 1]) {
					// 设置标志
					bSorted = false;

					// 扑克数据
					cbSwitchData = card_data[i];
					card_data[i] = card_data[i + 1];
					card_data[i + 1] = cbSwitchData;
				}
			}
			cbLast--;
		} while (bSorted == false);

		return true;
	}

	public static void main(String[] args) {
		// 插入扑克
		PHZGameLogicYIYANG logic = new PHZGameLogicYIYANG();
		int index = logic.switch_to_card_index(24);

		for (int i = 0; i < GameConstants.CARD_DATA_FLS_LX.length; i++)
			System.out.println(GameConstants.CARD_DATA_FLS_LX[i]);
	}

}
