/**
 * 
 */
package com.cai.game.shisanzhang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;

//分析结构
class tagAnalyseData
{
	int							bOneCount ;								//单张数目
	int							bTwoCount ;								//两张数目 
	int							bThreeCount ;							//三张数目
	int							bFourCount ;							//四张数目
	int							bFiveCount ;							//五张数目
	int							bOneFirst[]=new int[13];							//单牌位置
	int							bTwoFirst[]=new int[13];							//对牌位置
	int							bThreeFirst[]=new int[13];						//三条位置
	int							bFourFirst[]=new int[13];							//四张位置
	boolean							bStraight;								//是否顺子
	public tagAnalyseData(){
		bOneCount=0;
		bTwoCount=0;
		bThreeCount=0;
		bFourCount=0;
		bFiveCount=0;
		Arrays.fill(bOneFirst,0);
		Arrays.fill(bTwoFirst,0);
		Arrays.fill(bThreeFirst,0);
		Arrays.fill(bFourFirst,0);
		bStraight=false;
	}
	public void Reset(){
		bOneCount=0;
		bTwoCount=0;
		bThreeCount=0;
		bFourCount=0;
		bFiveCount=0;
		Arrays.fill(bOneFirst,0);
		Arrays.fill(bTwoFirst,0);
		Arrays.fill(bThreeFirst,0);
		Arrays.fill(bFourFirst,0);
		bStraight=false;
	}
};
//////////////////////////////////////////////////////////////////////////
class search_all_card_type{
	int card_type[]=new int[4000];
	int card_count[]=new int[4000];
	int type_card_data[][]=new int[4000][5];
	int card_type_count=0;
	
	public search_all_card_type(){
		card_type_count=0;
		Arrays.fill(card_type,GameConstants.SSZ_CT_INVALID);
		Arrays.fill(card_count,0);
		for(int i=0;i<4000;i++){
			for(int j=0;j<5;j++){
				type_card_data[i][j]=GameConstants.INVALID_CARD;
			}
		}
	}
	public void reset(){
		card_type_count=0;
		Arrays.fill(card_type,GameConstants.SSZ_CT_INVALID);
		Arrays.fill(card_count,0);
		for(int i=0;i<4000;i++){
			for(int j=0;j<5;j++){
				type_card_data[i][j]=GameConstants.INVALID_CARD;
			}
		}
	}
}


//////////////////////////////////////////////////////////////////////////
//排列类型
enum enSortCardType
{
	enDescend ,																//降序类型 
	enAscend ,																//升序类型
	enColor																	//花色类型
};


public class SSZGameLogic {
	public int _game_rule_index; // 游戏规则
	public int _game_type_index;
	
	public int VALUE_SINGLE=1;//乌龙
	public int VALUE_ONE_DOUBLE=2;//一对
	public int VALUE_TWO_DOUBLE =3;//两对
	public int VALUE_THREE=4;//三条
	public int VALUE_LINK=5;//顺子
	public int VALUE_CHONG_SAN=6;//冲三
	public int VALUE_FLUSH=6;//同花
	public int VALUE_THREE_DEOUBLE=7;//葫芦
	public int VALUE_ZHONGDUN_HULU=8;//中墩葫芦
	public int VALUE_FOUR_ONE=8;//铁枝
	public int VALUE_STRAIGHT_FLUSH=9;//同花顺
	public SSZGameLogic() {

	}
	public int GetSpecialCard_type(int bCardData[], int bCardCount){
		if(_game_type_index == GameConstants.GAME_TYPE_SSZ_JD){
			if(is_zhizun_qing_long(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_ZHI_ZHUN_QING_LONG;
			}
			if(is_yitiao_long(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_YI_TIAO_LONG;
			}
			if(is_shier_huang_zu(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SHI_ER_HUANG_ZU;
			}
			if(is_san_tong_hua_shun(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_TONG_HUA_SHUN;
			}
			if(is_san_fen_tian_xia(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_FEN_TIAN_XIA;
			}
			if(is_quan_da(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_QUAN_DA;
			}
			if(is_quan_xiao_jd(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_QUAN_XIAO;
			}
			if(is_cou_yi_se(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_COU_YI_SE;
			}
			if(is_si_tao_san_tiao(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SI_TAO_SAN_TIAO;
			}
			if(is_wu_dui_san_tiao(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_WU_DUI_SAN_TIAO;
			}
			if(is_liu_dui_ban(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_LIU_DUI_BAN;
			}
			if(is_san_shun_zi(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_SHUN_ZI;
			}
			if(is_san_tong_hua(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_TONG_HUA;
			}
		}else{
			if(is_zhizun_qing_long(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_ZHI_ZHUN_QING_LONG;
			}
			if(is_ji_hua(bCardData,bCardCount,12)){
				return GameConstants.SSZ_CT_SHI_ER_HUA;
			}
			if(is_quan_hei(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_QUAN_HEI;
			}
			if(is_quan_hong(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_QUNA_HONG;
			}
			if(is_san_tong_hua_shun(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_TONG_HUA_SHUN;
			}
			if(is_san_fen_tian_xia(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_FEN_TIAN_XIA;
			}
			if(is_yitiao_long(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_YI_TIAO_LONG;
			}
			if(is_quan_hong_yi_dian_hei(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_QUAN_HONG_YI_DIAN_HEI;
			}
			if(is_quan_hei_yi_dian_hong(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_QUAN_HEI_YI_DIAN_HONG;
			}
			if(is_si_tao_san_tiao(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SI_TAO_SAN_TIAO;
			}
			if(is_ji_hua(bCardData,bCardCount,11)){
				return GameConstants.SSZ_CT_SHI_YI_HUA;
			}
			if(is_ji_qi(bCardData,bCardCount,11)){
				return GameConstants.SSZ_CT_SHI_YI_QI;
			}
			if(is_ji_hua(bCardData,bCardCount,10)){
				return GameConstants.SSZ_CT_SHI_HUA;
			}
			if(is_ji_qi(bCardData,bCardCount,10)){
				return GameConstants.SSZ_CT_SHI_QI;
			}
			if(is_wu_dui_san_tiao(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_WU_DUI_SAN_TIAO;
			}
			if(is_ji_hua(bCardData,bCardCount,9)){
				return GameConstants.SSZ_CT_JIU_HUA;
			}
			if(is_ji_qi(bCardData,bCardCount,9)){
				return GameConstants.SSZ_CT_JIU_QI;
			}
			if(is_ji_hua(bCardData,bCardCount,8)){
				return GameConstants.SSZ_CT_BA_HUA;
			}
			if(is_ji_qi(bCardData,bCardCount,8)){
				return GameConstants.SSZ_CT_BA_QI;
			}
			if(is_ji_qi(bCardData,bCardCount,7)){
				return GameConstants.SSZ_CT_QI_QI;
			}
			if(is_ji_qi(bCardData,bCardCount,6)){
				return GameConstants.SSZ_CT_LIU_QI;
			}
			if(is_quan_xiao_zz(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_QUAN_XIAO;
			}
			if(is_liu_dui_ban(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_LIU_DUI_BAN;
			}
			if(is_si_dui_yi_tong(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SI_DUI_YI_TONG;
			}
			if(is_si_dui_yi_shun(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SI_DUI_YI_SHUN;
			}
			if(is_du_si(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_DU_SI;
			}
			if(is_san_tong_hua(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_TONG_HUA;
			}
			if(is_san_shun_zi(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_SAN_SHUN_ZI;
			}
			if(is_ban_xiao(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_BAN_XIAO;
			}
			if(is_du_san(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_DU_SAN;
			}
			if(is_du_dui(bCardData,bCardCount)){
				return GameConstants.SSZ_CT_DU_DUI;
			}
		}
		return GameConstants.SSZ_CT_INVALID;
	}
	//获取类型
	public int GetCardType(int bCardData[], int bCardCount)
	{
		SortCardList(bCardData, bCardCount, enSortCardType.enDescend);
		//数据校验
		if(bCardCount!=3 && bCardCount!=5 &&  bCardCount!=13) return GameConstants.SSZ_CT_INVALID ;

		tagAnalyseData AnalyseData = new tagAnalyseData();


		AnalyseCard(bCardData , bCardCount , AnalyseData) ;

		//开始分析
		switch (bCardCount)
		{
		case 3:	//三条类型
			{
				if(_game_type_index == GameConstants.GAME_TYPE_SSZ_JD || _game_type_index == GameConstants.GAME_TYPE_SSZ_ZZ
				|| _game_type_index == GameConstants.GAME_TYPE_SSZ_XUPU|| _game_type_index == GameConstants.GAME_TYPE_SSZ_CZ){
					//单牌类型
					if(3==AnalyseData.bOneCount) return GameConstants.SSZ_CT_SINGLE ;
					
					//对带一张
					if(1==AnalyseData.bTwoCount && 1==AnalyseData.bOneCount) return GameConstants.SSZ_CT_ONE_DOUBLE ;

					//三张牌型
					if(1==AnalyseData.bThreeCount) return GameConstants.SSZ_CT_THREE ;

					//错误类型
					return GameConstants.SSZ_CT_INVALID ;
				}else{
					//对带一张
					if(1==AnalyseData.bTwoCount && 1==AnalyseData.bOneCount) return GameConstants.SSZ_CT_ONE_DOUBLE ;

					//三张牌型
					if(1==AnalyseData.bThreeCount) return GameConstants.SSZ_CT_THREE ;
					
					boolean bFlus=false;
					boolean bLink=false;
					if(get_card_color(bCardData[0]) == get_card_color(bCardData[1])
					 &&get_card_color(bCardData[1]) == get_card_color(bCardData[2])){
							bFlus=true;
					}
					if(GetCardLogicValue(bCardData[0]) == GetCardLogicValue(bCardData[1])+1
					&&GetCardLogicValue(bCardData[1]) == GetCardLogicValue(bCardData[2])+1){
						bLink=true;
					}
					
					if(get_card_value(bCardData[0])+1 == GetCardLogicValue(bCardData[2])
						&&GetCardLogicValue(bCardData[1]) == GetCardLogicValue(bCardData[2])+1){
						bLink=true;
					}
					if(bFlus && !bLink){
						return GameConstants.SSZ_CT_FIVE_FLUSH ;
					}else if(!bFlus && bLink){
						return GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_BACK_A ;
					}else if(bFlus && bLink){
						return GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH ;
					}else{
						return GameConstants.SSZ_CT_SINGLE ;
					}
				}
				
			}
		case 5:	//五张牌型
			{
				boolean bFlushNoA	    = false , 
					 bFlushFirstA	= false ,
					 bFlushBackA	= false ;

				//A连在后
				if(14==GetCardLogicValue(bCardData[0]) && 10==GetCardLogicValue(bCardData[4]))
					bFlushBackA = true ;
				else
					bFlushNoA = true ;
				for(int i=0 ; i<4 ; ++i)
					if(1!=GetCardLogicValue(bCardData[i])-GetCardLogicValue(bCardData[i+1])) 
					{
						bFlushBackA = false ;
						bFlushNoA   = false ;
					}
				//A连在前
				if(false==bFlushBackA && false==bFlushNoA && 14==GetCardLogicValue(bCardData[0]))
				{
					bFlushFirstA = true ;
					for(int i=1 ; i<4 ; ++i) if(1!=GetCardLogicValue(bCardData[i])-GetCardLogicValue(bCardData[i+1])) bFlushFirstA = false ;
					if(2!=GetCardLogicValue(bCardData[4])) bFlushFirstA = false ;
				}

				//同花五牌
				if(false==bFlushBackA && false==bFlushNoA && false==bFlushFirstA)
				{
					if(true==AnalyseData.bStraight) return GameConstants.SSZ_CT_FIVE_FLUSH ;
				}
				else if(true==bFlushNoA)
				{
					//杂顺类型
					if(false==AnalyseData.bStraight) return GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_NO_A;
					//同花顺牌
					else							 return GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH ;
				}
				else if(true==bFlushFirstA)
				{
					//杂顺类型
					if(false==AnalyseData.bStraight) return GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_FIRST_A;
					//同花顺牌
					else							 return GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A ;
				}
				else if(true==bFlushBackA)
				{
					//杂顺类型
					if(false==AnalyseData.bStraight) return GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_BACK_A;
					//同花顺牌
					else							 return GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH ;
				}

				//四带单张
				if(1==AnalyseData.bFourCount && 1==AnalyseData.bOneCount) return	 GameConstants.SSZ_CT_FIVE_FOUR_ONE ;

				//三条一对
				if(1==AnalyseData.bThreeCount && 1==AnalyseData.bTwoCount) return	 GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE ;

				//三条带单
				if(1==AnalyseData.bThreeCount && 2==AnalyseData.bOneCount) return	 GameConstants.SSZ_CT_THREE ;


				//两对牌型
				if(2==AnalyseData.bTwoCount && 1==AnalyseData.bOneCount) return		 GameConstants.SSZ_CT_FIVE_TWO_DOUBLE ;


				//只有一对
				if(1==AnalyseData.bTwoCount && 3==AnalyseData.bOneCount) return		 GameConstants.SSZ_CT_ONE_DOUBLE ;

				//单牌类型
				if(5==AnalyseData.bOneCount && false==AnalyseData.bStraight) return  GameConstants.SSZ_CT_SINGLE ;

				//错误类型
				return GameConstants.SSZ_CT_INVALID;
			}
		case 8:{
			//错误类型
			return GameConstants.SSZ_CT_INVALID;
		}
		}

		return GameConstants.SSZ_CT_INVALID;
	}
	public int get_type_value(int type,int dao_index){
		int value=0;
		switch(type){
		case GameConstants.SSZ_CT_SINGLE:{
			value=VALUE_SINGLE;
			break;
		}
		case GameConstants.SSZ_CT_ONE_DOUBLE:{
			value=VALUE_ONE_DOUBLE;
			break;
		}
		case GameConstants.SSZ_CT_FIVE_TWO_DOUBLE:{
			value=VALUE_TWO_DOUBLE;
			break;
		}
		case GameConstants.SSZ_CT_THREE:{
			if(dao_index == 1){
				value=VALUE_CHONG_SAN;
			}else{
				value=VALUE_THREE;
			}
			
			break;
		}
		case GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_FIRST_A:
		case GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_NO_A:
		case GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_BACK_A:{
			value=VALUE_LINK;
			break;
		}
		case GameConstants.SSZ_CT_FIVE_FLUSH:{
			value=VALUE_FLUSH;
			break;
		}
		case GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE:{
			if(dao_index == 2){
				value=VALUE_ZHONGDUN_HULU;
			}else{
				value=VALUE_THREE_DEOUBLE;
			}
			
			break;
		}
		case GameConstants.SSZ_CT_FIVE_FOUR_ONE:{
			value=VALUE_FOUR_ONE;
			break;
		}
		case GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A:
		case GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH:{
			value=VALUE_STRAIGHT_FLUSH;
			break;
		}
		}
		return value;
	}
	public int get_special_score_jd(int type){
		int score=0;
		switch(type){
		case GameConstants.SSZ_CT_SAN_TONG_HUA:{
			score=3;
			break;
		}
		case GameConstants.SSZ_CT_SAN_SHUN_ZI:{
			score=3;
			break;
		}
		case GameConstants.SSZ_CT_LIU_DUI_BAN:{
			score=4;
			break;
		}
		case GameConstants.SSZ_CT_WU_DUI_SAN_TIAO:{
			score=5;
			break;
		}
		case GameConstants.SSZ_CT_SI_TAO_SAN_TIAO:{
			score=6;
			break;
		}
		case GameConstants.SSZ_CT_COU_YI_SE:{
			score=10;
			break;
		}
		case GameConstants.SSZ_CT_QUAN_XIAO:{
			score=12;
			break;
		}
		case GameConstants.SSZ_CT_QUAN_DA:{
			score=15;
			break;
		}
		case GameConstants.SSZ_CT_SAN_FEN_TIAN_XIA:{
			score=20;
			break;
		}
		case GameConstants.SSZ_CT_SAN_TONG_HUA_SHUN:{
			score=22;
			break;
		}
		case GameConstants.SSZ_CT_SHI_ER_HUANG_ZU:{
			score=24;
			break;
		}
		case GameConstants.SSZ_CT_YI_TIAO_LONG:{
			score=26;
			break;
		}
		case GameConstants.SSZ_CT_ZHI_ZHUN_QING_LONG:{
			score=108;
			break;
		}
		}
		return score;
	}
	public int get_special_score_zz(int type){
		int score=0;
		switch(type){
		case GameConstants.SSZ_CT_DU_DUI:
		case GameConstants.SSZ_CT_DU_SAN:
		case GameConstants.SSZ_CT_BAN_XIAO:
		case GameConstants.SSZ_CT_SAN_TONG_HUA:
		case GameConstants.SSZ_CT_SAN_SHUN_ZI:{
			score=3;
			break;
		}
		case GameConstants.SSZ_CT_DU_SI:
		case GameConstants.SSZ_CT_SI_DUI_YI_TONG:
		case GameConstants.SSZ_CT_SI_DUI_YI_SHUN:{
			score=4;
			break;
		}
		case GameConstants.SSZ_CT_LIU_DUI_BAN:
		case GameConstants.SSZ_CT_QUAN_XIAO:
		case GameConstants.SSZ_CT_LIU_QI:{
			score=6;
			break;
		}
		case GameConstants.SSZ_CT_QI_QI:{
			score=7;
			break;
		}
		case GameConstants.SSZ_CT_BA_QI:
		case GameConstants.SSZ_CT_BA_HUA:{
			score=8;
			break;
		}
		case GameConstants.SSZ_CT_JIU_QI:
		case GameConstants.SSZ_CT_JIU_HUA:
		case GameConstants.SSZ_CT_WU_DUI_SAN_TIAO:{
			score=9;
			break;
		}
		case GameConstants.SSZ_CT_SHI_QI:
		case GameConstants.SSZ_CT_SHI_HUA:{
			score=10;
			break;
		}
		case GameConstants.SSZ_CT_SHI_YI_QI:
		case GameConstants.SSZ_CT_SHI_YI_HUA:{
			score=11;
			break;
		}
		case GameConstants.SSZ_CT_SI_TAO_SAN_TIAO:{
			score=12;
			break;
		}
		case GameConstants.SSZ_CT_QUAN_HONG_YI_DIAN_HEI:
		case GameConstants.SSZ_CT_QUAN_HEI_YI_DIAN_HONG:{
			score=13;
			break;
		}
		case GameConstants.SSZ_CT_SAN_FEN_TIAN_XIA:
		case GameConstants.SSZ_CT_SAN_TONG_HUA_SHUN:
		case GameConstants.SSZ_CT_QUAN_HEI:
		case GameConstants.SSZ_CT_YI_TIAO_LONG:
		case GameConstants.SSZ_CT_QUNA_HONG:{
			score=26;
			break;
		}
		case GameConstants.SSZ_CT_SHI_ER_HUA:{
			score=36;
			break;
		}
		case GameConstants.SSZ_CT_ZHI_ZHUN_QING_LONG:{
			score=108;
			break;
		}
		}
		return score;
	}
	//推荐方案
	public int search_all_type(int bCardData[], int bCardCount,int tui_jian_card[][],int type[][]){
		//判断特殊牌型
		int finial_type_count=0;
		int init_type=0;
		int special_type=GetSpecialCard_type(bCardData, bCardCount);
		if(special_type !=GameConstants.SSZ_CT_INVALID){
			for(int i=0;i<bCardCount;i++){
				tui_jian_card[0][i]=bCardData[i];
			}
			type[0][0]=special_type;
			finial_type_count++;
			init_type++;
		}

		search_all_card_type allt_type=new search_all_card_type();
		

		/*找出所有牌型
		 * 
		 * 
		 * */
		search_tong_hua_shun(bCardData,bCardCount,allt_type);

		search_tie_zhi(bCardData,bCardCount,allt_type);
		search_hulu(bCardData,bCardCount,allt_type);
		search_tong_hua(bCardData,bCardCount,allt_type);
		search_shun_zi(bCardData,bCardCount,allt_type);
		search_san_tiao(bCardData,bCardCount,allt_type,false);
		search_liang_dui(bCardData,bCardCount,allt_type);
		search_dui_zi(bCardData,bCardCount,allt_type);
		search_wu_long(bCardData,bCardCount,allt_type,false);
		////权值判断
		int one_type[]=new int[4000];
		int one_card[][]=new int[4000][3];
		Arrays.fill(one_type,0);
		for(int x=0;x<4000;x++){
			Arrays.fill(one_card[x],0);
		}
		int index_two[]=new int[4000];
		int index_three[]=new int[4000];
		int type_count=0;
		for(int i=0;i<allt_type.card_type_count;i++){
			if(allt_type.card_count[i]!=5){
				continue;
			}
			int cb_card_data_temp_one[]=new int[bCardCount];
			int card_count_temp_one=bCardCount;
			for(int index=0;index<bCardCount;index++){
				cb_card_data_temp_one[index]=bCardData[index];
			}
			if(GetCardLogicValue(allt_type.type_card_data[i][0]) == 5
			&& GetCardLogicValue(allt_type.type_card_data[i][1]) == 5
			&& GetCardLogicValue(allt_type.type_card_data[i][2]) == 3
			&& GetCardLogicValue(allt_type.type_card_data[i][3]) == 3){
				int a=3;
				a++;
			}
			if(this.remove_cards_by_data(cb_card_data_temp_one, card_count_temp_one, allt_type.type_card_data[i], allt_type.card_count[i])){
				card_count_temp_one-=allt_type.card_count[i];
				for(int j=0;j<allt_type.card_type_count;j++){
					if(allt_type.card_count[j]!=5 || i==j){
						continue;
					}
					int cb_card_data_temp_two[]=new int[card_count_temp_one];
					int card_count_temp_two=card_count_temp_one;
					for(int index=0;index<card_count_temp_two;index++){
						cb_card_data_temp_two[index]=cb_card_data_temp_one[index];
					}
					if(allt_type.card_type[j]>allt_type.card_type[i]){
							continue;
					}else if(allt_type.card_type[j]==allt_type.card_type[i]){
						int success=1;
						for(int index=0;index<5;index++){
							if(GetCardLogicValue(allt_type.type_card_data[i][index])<GetCardLogicValue(allt_type.type_card_data[j][index])){
								success=-1;
								break;
							}else if(GetCardLogicValue(allt_type.type_card_data[i][index])>GetCardLogicValue(allt_type.type_card_data[j][index])){
								success=1;
								break;
							}else{
								success=0;
							}
						}
						if(success == 0){
							for(int index=0;index<5;index++){
								if(allt_type.type_card_data[i][index]<allt_type.type_card_data[j][index]){
									success=-1;
									break;
								}else{
									success=1;
									break;
								}
							}
						}
						if(success == -1){
							continue;
						}
					}
					if(this.remove_cards_by_data(cb_card_data_temp_two, card_count_temp_two, allt_type.type_card_data[j], allt_type.card_count[j])){
						card_count_temp_two-=allt_type.card_count[j];
						int three_type=this.GetCardType(cb_card_data_temp_two, card_count_temp_two);
						if(three_type>allt_type.card_type[j]){
							continue;
						}
						if(three_type==allt_type.card_type[j]){
							if(three_type == GameConstants.SSZ_CT_ONE_DOUBLE){
								tagAnalyseData AnalyseData = new tagAnalyseData();
								AnalyseCard(cb_card_data_temp_two , 3 , AnalyseData);
								if(GetCardLogicValue(cb_card_data_temp_two[AnalyseData.bTwoFirst[0]])>GetCardLogicValue(allt_type.type_card_data[j][0])){
									continue;
								}
							}else{
								if(GetCardLogicValue(allt_type.type_card_data[j][0])<GetCardLogicValue(cb_card_data_temp_two[0])){
									continue;
								}
							}
						}

						if(type_count<4000){
							index_two[type_count]=j;
							index_three[type_count]=i;
							one_type[type_count]=three_type;
							for(int card_index=0;card_index<card_count_temp_two;card_index++){
								one_card[type_count][card_index]=cb_card_data_temp_two[card_index];
							}
							
							type_count++;
						}
					}
				}
			}
			
		}
		
		//头道最大
		int finial_one_type[]=new int[3];
		int finial_two_type[]=new int[3];
		int finial_three_type[]=new int[3];
		int finial_one_card[][]=new int[3][3];
		int finial_two_card[][]=new int[3][5];
		int finial_three_card[][]=new int[3][5];
		int finial_index[]=new int[3];
		boolean has_find=false;
		Arrays.fill(finial_one_type,0);
		Arrays.fill(finial_two_type,0);
		Arrays.fill(finial_three_type,0);
		Arrays.fill(finial_index,-1);
		for(int x=0;x<3;x++){
			Arrays.fill(finial_one_card[x],GameConstants.INVALID_CARD);
			Arrays.fill(finial_two_card[x],GameConstants.INVALID_CARD);
			Arrays.fill(finial_three_card[x],GameConstants.INVALID_CARD);
		}		
		for(int i=0;i<3;i++){
			finial_type_count=search_AI_paixing(allt_type,one_card,finial_one_type,finial_two_type,finial_three_type,finial_one_card,finial_two_card
					,finial_three_card,finial_index,finial_type_count,one_type,index_two,index_three,type_count,i);	
			if(finial_type_count == 3){
				break;
			}
		}
			

		
		
		for(int i=init_type;i<finial_type_count;i++){
			int one_count=3;
			int two_count=5;
			int three_count=5;
			for(int j=0;j<one_count;j++){
				tui_jian_card[i][j]=finial_one_card[i][j];
			}
			for(int j=0;j<two_count;j++){
				tui_jian_card[i][one_count+j]=finial_two_card[i][j];
			}
			for(int j=0;j<three_count;j++){
				tui_jian_card[i][one_count+two_count+j]=finial_three_card[i][j];
			}
			if(finial_one_type[i] == GameConstants.SSZ_CT_THREE){
				finial_one_type[i]=GameConstants.SSZ_CT_CHONG_SAN;
			}
			if(finial_two_type[i] == GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE){
				finial_two_type[i]=GameConstants.SSZ_CT_ZHOGNDUN_HULU;
			}
			type[i][0]=finial_one_type[i];
			type[i][1]=finial_two_type[i];
			type[i][2]=finial_three_type[i];
		}
		return finial_type_count;
	}
	public int search_AI_paixing(search_all_card_type allt_type,int one_card[][],int finial_one_type[],int finial_two_type[],
			int finial_three_type[],int finial_one_card[][],int finial_two_card[][],int finial_three_card[][],int finial_index[],
			int finial_type_count,int one_type[],int index_two[],int index_three[],int type_count,int card_type_order){
		boolean is_find=false;
		int init_type_count=finial_type_count;
		for(int i=0;i<type_count;i++){
			boolean is_have=false;
			for(int j=0;j<init_type_count;j++){
				if(allt_type.card_type[index_three[i]] == finial_three_type[j]){
					is_have=true;
					break;
				}
			}
			if(!is_have){
				finial_one_type[init_type_count]=one_type[i];
				finial_two_type[init_type_count]=allt_type.card_type[index_two[i]];
				finial_three_type[init_type_count]=allt_type.card_type[index_three[i]];
				for(int y=0;y<3;y++){
					finial_one_card[init_type_count][y]=one_card[i][y];
				}
				for(int y=0;y<5;y++){
					finial_two_card[init_type_count][y]=allt_type.type_card_data[index_two[i]][y];
				}
				for(int y=0;y<5;y++){
					finial_three_card[init_type_count][y]=allt_type.type_card_data[index_three[i]][y];
				}
				is_find=true;
				finial_index[init_type_count]=i;
				break;
			}
		}
		
		for(int i=0;i<type_count;i++){
			if(finial_three_type[init_type_count] == allt_type.card_type[index_three[i]]){
				if(finial_two_type[finial_type_count] < allt_type.card_type[index_two[i]]){
					int one_count=3;
					int two_count=allt_type.card_count[index_two[i]];
					int three_count=allt_type.card_count[index_three[i]];
					finial_one_type[finial_type_count]=one_type[i];
					finial_two_type[finial_type_count]=allt_type.card_type[index_two[i]];
					finial_three_type[finial_type_count]=allt_type.card_type[index_three[i]];
					for(int y=0;y<one_count;y++){
						finial_one_card[finial_type_count][y]=one_card[i][y];
					}
					for(int y=0;y<two_count;y++){
						finial_two_card[finial_type_count][y]=allt_type.type_card_data[index_two[i]][y];
					}
					for(int y=0;y<three_count;y++){
						finial_three_card[finial_type_count][y]=allt_type.type_card_data[index_three[i]][y];
					}
					is_find=true;
					finial_index[finial_type_count]=i;
				}else if(finial_two_type[finial_type_count] == allt_type.card_type[index_two[i]]){
					if(finial_one_type[finial_type_count] < one_type[i]){
						int one_count=3;
						int two_count=allt_type.card_count[index_two[i]];
						int three_count=allt_type.card_count[index_three[i]];
						finial_one_type[finial_type_count]=one_type[i];
						finial_two_type[finial_type_count]=allt_type.card_type[index_two[i]];
						finial_three_type[finial_type_count]=allt_type.card_type[index_three[i]];
						for(int y=0;y<one_count;y++){
							finial_one_card[finial_type_count][y]=one_card[i][y];
						}
						for(int y=0;y<two_count;y++){
							finial_two_card[finial_type_count][y]=allt_type.type_card_data[index_two[i]][y];
						}
						for(int y=0;y<three_count;y++){
							finial_three_card[finial_type_count][y]=allt_type.type_card_data[index_three[i]][y];
						}
						is_find=true;
						finial_index[finial_type_count]=i;
					}else if(finial_one_type[finial_type_count] == one_type[i]){
						if(CompareCard(finial_one_card[finial_type_count], one_card[i], 3, 3, true, !has_rule(GameConstants.GAME_RULE_SSZ_BI_HUA_SE)) == 1){
							int one_count=3;
							int two_count=allt_type.card_count[index_two[i]];
							int three_count=allt_type.card_count[index_three[i]];
							finial_one_type[finial_type_count]=one_type[i];
							finial_two_type[finial_type_count]=allt_type.card_type[index_two[i]];
							finial_three_type[finial_type_count]=allt_type.card_type[index_three[i]];
							for(int y=0;y<one_count;y++){
								finial_one_card[finial_type_count][y]=one_card[i][y];
							}
							for(int y=0;y<two_count;y++){
								finial_two_card[finial_type_count][y]=allt_type.type_card_data[index_two[i]][y];
							}
							for(int y=0;y<three_count;y++){
								finial_three_card[finial_type_count][y]=allt_type.type_card_data[index_three[i]][y];
							}
							is_find=true;
							finial_index[finial_type_count]=i;
						}
					}
				}
			}
		}

		if(is_find){
			boolean is_same=false;
			for(int i=0;i<finial_type_count;i++){
				if(finial_one_type[i] == finial_one_type[finial_type_count]
				 &&finial_two_type[i] == finial_two_type[finial_type_count]
				 &&finial_three_type[i] == finial_three_type[finial_type_count]){
					is_same=true;
				}
			}
			if(!is_same){
				if(finial_two_type[finial_type_count] == GameConstants.SSZ_CT_ONE_DOUBLE){
					if(finial_three_type[finial_type_count] == GameConstants.SSZ_CT_FIVE_TWO_DOUBLE){
						for(int i=0;i<2;i++){
							for(int j=0;j<1;j++){
								if(this.GetCardLogicValue(finial_three_card[finial_type_count][i*2]) > this.GetCardLogicValue(finial_two_card[finial_type_count][j*2])){
									int temp_card_one=0;
									int temp_card_two=0;
									for(int x=0;x<5;x++){
										if(GetCardLogicValue(finial_three_card[finial_type_count][x]) ==  GetCardLogicValue(finial_three_card[finial_type_count][j*2])){
											temp_card_one=finial_three_card[finial_type_count][0];
											temp_card_two=finial_three_card[finial_type_count][1];
											finial_three_card[finial_type_count][x]=finial_two_card[finial_type_count][0];
											finial_three_card[finial_type_count][x+1]=finial_two_card[finial_type_count][1];
											break;
										}
									}
									for(int x=0;x<5;x++){
										if(GetCardLogicValue(finial_two_card[finial_type_count][x]) ==  GetCardLogicValue(finial_two_card[finial_type_count][i*2])){
											finial_two_card[finial_type_count][x]=temp_card_one;
											finial_two_card[finial_type_count][x+1]=temp_card_two;
											break;
										}
									}
								}
							}
						}
					}
				}
				if(finial_one_type[finial_type_count] == GameConstants.SSZ_CT_ONE_DOUBLE){
					if(finial_two_type[finial_type_count] == GameConstants.SSZ_CT_FIVE_TWO_DOUBLE){
						tagAnalyseData AnalyseData=new tagAnalyseData();
						this.AnalyseCard(finial_one_card[finial_type_count], 3, AnalyseData);
						for(int i=0;i<2;i++){
							for(int j=0;j<AnalyseData.bTwoCount;j++){
								if(this.GetCardLogicValue(finial_two_card[finial_type_count][i*2]) > this.GetCardLogicValue(finial_one_card[finial_type_count][AnalyseData.bTwoFirst[j]])){
									int temp_card_one=0;
									int temp_card_two=0;
									for(int x=0;x<3;x++){
										if(GetCardLogicValue(finial_one_card[finial_type_count][x]) ==  GetCardLogicValue(finial_one_card[finial_type_count][AnalyseData.bTwoFirst[j]])){
											temp_card_one=finial_one_card[finial_type_count][x];
											temp_card_two=finial_one_card[finial_type_count][x+1];
											finial_one_card[finial_type_count][x]=finial_two_card[finial_type_count][i*2];
											finial_one_card[finial_type_count][x+1]=finial_two_card[finial_type_count][i*2+1];
											break;
										}
									}
									for(int x=0;x<5;x++){
										if(GetCardLogicValue(finial_two_card[finial_type_count][x]) ==  GetCardLogicValue(finial_two_card[finial_type_count][i*2])){
											finial_two_card[finial_type_count][x]=temp_card_one;
											finial_two_card[finial_type_count][x+1]=temp_card_two;
											break;
										}
									}
								}
							}
						}
					}
				}
				finial_type_count++;
			}
		}
		if(finial_type_count<3){
			is_find=false;
			for(int i=0;i<type_count;i++){
				if(finial_three_type[init_type_count] == allt_type.card_type[index_three[i]]){
					if(finial_one_type[finial_type_count] < one_type[i]){
						int one_count=3;
						int two_count=allt_type.card_count[index_two[i]];
						int three_count=allt_type.card_count[index_three[i]];
						finial_one_type[finial_type_count]=one_type[i];
						finial_two_type[finial_type_count]=allt_type.card_type[index_two[i]];
						finial_three_type[finial_type_count]=allt_type.card_type[index_three[i]];
						for(int y=0;y<one_count;y++){
							finial_one_card[finial_type_count][y]=one_card[i][y];
						}
						for(int y=0;y<two_count;y++){
							finial_two_card[finial_type_count][y]=allt_type.type_card_data[index_two[i]][y];
						}
						for(int y=0;y<three_count;y++){
							finial_three_card[finial_type_count][y]=allt_type.type_card_data[index_three[i]][y];
						}
						is_find=true;
						finial_index[finial_type_count]=i;
					}else if(finial_one_type[finial_type_count] == one_type[i]){

						if(finial_two_type[finial_type_count] < allt_type.card_type[index_two[i]]){
							int one_count=3;
							int two_count=allt_type.card_count[index_two[i]];
							int three_count=allt_type.card_count[index_three[i]];
							finial_one_type[finial_type_count]=one_type[i];
							finial_two_type[finial_type_count]=allt_type.card_type[index_two[i]];
							finial_three_type[finial_type_count]=allt_type.card_type[index_three[i]];
							for(int y=0;y<one_count;y++){
								finial_one_card[finial_type_count][y]=one_card[i][y];
							}
							for(int y=0;y<two_count;y++){
								finial_two_card[finial_type_count][y]=allt_type.type_card_data[index_two[i]][y];
							}
							for(int y=0;y<three_count;y++){
								finial_three_card[finial_type_count][y]=allt_type.type_card_data[index_three[i]][y];
							}
							is_find=true;
							finial_index[finial_type_count]=i;
						}else if(finial_two_type[finial_type_count] == allt_type.card_type[index_two[i]]){
							if(CompareCard(finial_one_card[finial_type_count], one_card[i], 3, 3, true, !has_rule(GameConstants.GAME_RULE_SSZ_BI_HUA_SE)) == 1){
								int one_count=3;
								int two_count=allt_type.card_count[index_two[i]];
								int three_count=allt_type.card_count[index_three[i]];
								finial_one_type[finial_type_count]=one_type[i];
								finial_two_type[finial_type_count]=allt_type.card_type[index_two[i]];
								finial_three_type[finial_type_count]=allt_type.card_type[index_three[i]];
								for(int y=0;y<one_count;y++){
									finial_one_card[finial_type_count][y]=one_card[i][y];
								}
								for(int y=0;y<two_count;y++){
									finial_two_card[finial_type_count][y]=allt_type.type_card_data[index_two[i]][y];
								}
								for(int y=0;y<three_count;y++){
									finial_three_card[finial_type_count][y]=allt_type.type_card_data[index_three[i]][y];
								}
								is_find=true;
								finial_index[finial_type_count]=i;
							}
						}
					}
				}
			}

			if(is_find){
				boolean is_same=false;
				for(int i=0;i<finial_type_count;i++){
					if(finial_one_type[i] == finial_one_type[finial_type_count]
					 &&finial_two_type[i] == finial_two_type[finial_type_count]
					 &&finial_three_type[i] == finial_three_type[finial_type_count]){
						is_same=true;
					}
				}
				if(!is_same){
					if(finial_two_type[finial_type_count] == GameConstants.SSZ_CT_ONE_DOUBLE){
						if(finial_three_type[finial_type_count] == GameConstants.SSZ_CT_FIVE_TWO_DOUBLE){
							for(int i=0;i<2;i++){
								for(int j=0;j<1;j++){
									if(this.GetCardLogicValue(finial_three_card[finial_type_count][i*2]) > this.GetCardLogicValue(finial_two_card[finial_type_count][j*2])){
										int temp_card_one=0;
										int temp_card_two=0;
										for(int x=0;x<5;x++){
											if(GetCardLogicValue(finial_three_card[finial_type_count][x]) ==  GetCardLogicValue(finial_three_card[finial_type_count][j*2])){
												temp_card_one=finial_three_card[finial_type_count][0];
												temp_card_two=finial_three_card[finial_type_count][1];
												finial_three_card[finial_type_count][x]=finial_two_card[finial_type_count][0];
												finial_three_card[finial_type_count][x+1]=finial_two_card[finial_type_count][1];
												break;
											}
										}
										for(int x=0;x<5;x++){
											if(GetCardLogicValue(finial_two_card[finial_type_count][x]) ==  GetCardLogicValue(finial_two_card[finial_type_count][i*2])){
												finial_two_card[finial_type_count][x]=temp_card_one;
												finial_two_card[finial_type_count][x+1]=temp_card_two;
												break;
											}
										}
									}
								}
							}
						}
					}
					if(finial_one_type[finial_type_count] == GameConstants.SSZ_CT_ONE_DOUBLE){
						if(finial_two_type[finial_type_count] == GameConstants.SSZ_CT_FIVE_TWO_DOUBLE){
							tagAnalyseData AnalyseData=new tagAnalyseData();
							this.AnalyseCard(finial_one_card[finial_type_count], 3, AnalyseData);
							for(int i=0;i<2;i++){
								for(int j=0;j<AnalyseData.bTwoCount;j++){
									if(this.GetCardLogicValue(finial_two_card[finial_type_count][i*2]) > this.GetCardLogicValue(finial_one_card[finial_type_count][AnalyseData.bTwoFirst[j]])){
										int temp_card_one=0;
										int temp_card_two=0;
										for(int x=0;x<3;x++){
											if(GetCardLogicValue(finial_one_card[finial_type_count][x]) ==  GetCardLogicValue(finial_one_card[finial_type_count][AnalyseData.bTwoFirst[j]])){
												temp_card_one=finial_one_card[finial_type_count][x];
												temp_card_two=finial_one_card[finial_type_count][x+1];
												finial_one_card[finial_type_count][x]=finial_two_card[finial_type_count][i*2];
												finial_one_card[finial_type_count][x+1]=finial_two_card[finial_type_count][i*2+1];
												break;
											}
										}
										for(int x=0;x<5;x++){
											if(GetCardLogicValue(finial_two_card[finial_type_count][x]) ==  GetCardLogicValue(finial_two_card[finial_type_count][i*2])){
												finial_two_card[finial_type_count][x]=temp_card_one;
												finial_two_card[finial_type_count][x+1]=temp_card_two;
												break;
											}
										}
									}
								}
							}
						}
					}
					
					finial_type_count++;
				}
			}
		}
		
		

		return finial_type_count;
	}
	//搜索对子
	public boolean search_dui_zi(int bCardData[], int bCardCount,search_all_card_type all_type){
		boolean have=false;
		SortCardList(bCardData, bCardCount);
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(bCardData, bCardCount, AnalyseData);
		if(AnalyseData.bTwoCount == 0 && AnalyseData.bThreeCount == 0){
			return false;
		}
		for(int i=0;i<AnalyseData.bTwoCount;i++){
			for(int j=bCardCount-1;j>=0;j--){
				for(int x=j-1;x>=0;x--){
					if(get_card_value(bCardData[j]) == get_card_value(bCardData[x])){
						continue;
					}
					for(int y=x-1;y>=0;y--){
						if(get_card_value(bCardData[j])==get_card_value(bCardData[AnalyseData.bTwoFirst[i]])
							||get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bTwoFirst[i]])
							||get_card_value(bCardData[y])==get_card_value(bCardData[AnalyseData.bTwoFirst[i]])){
							continue;
						}
						if(get_card_value(bCardData[y]) == get_card_value(bCardData[x])){
							continue;
						}
						
						for(int index=0;index<2;index++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[i]+index];
						}
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[j];
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[y];
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_ONE_DOUBLE;
						have=true;
					}
				}
			}
		}
		for(int i=0;i<AnalyseData.bThreeCount;i++){
			for(int j=bCardCount-1;j>=0;j--){
				for(int x=j-1;x>=0;x--){
					if(get_card_value(bCardData[j]) == get_card_value(bCardData[x])){
						continue;
					}
					for(int y=x-1;y>=0;y--){
						if(get_card_value(bCardData[j])==get_card_value(bCardData[AnalyseData.bThreeFirst[i]])
							||get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bThreeFirst[i]])
							||get_card_value(bCardData[y])==get_card_value(bCardData[AnalyseData.bThreeFirst[i]])){
							continue;
						}
						if(get_card_value(bCardData[y]) == get_card_value(bCardData[x])){
							continue;
						}
						for(int three_index_one=0;three_index_one<2;three_index_one++){
							for(int three_index_two=three_index_one+1;three_index_two<3;three_index_two++){
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+three_index_one];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+three_index_two];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[j];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[y];
								all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_ONE_DOUBLE;
								have=true;
							}
						}
					}
				}
			}
		}

		return have;
	}
	//搜索两对
	public boolean search_liang_dui(int bCardData[], int bCardCount,search_all_card_type all_type){
		boolean have=false;
		SortCardList(bCardData, bCardCount);
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(bCardData, bCardCount, AnalyseData);
		if(AnalyseData.bTwoCount + AnalyseData.bThreeCount< 2){
			return false;
		}
		
		for(int i=0;i<AnalyseData.bTwoCount;i++){
			for(int j=AnalyseData.bTwoCount-1;j>=0;j--){
				if(i == j){
					continue;
				}
				for(int x=bCardCount-1;x>=0;x--){
					if(get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bTwoFirst[i]])
							|| get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bTwoFirst[j]])){
						continue;
					}
					for(int y=0;y<2;y++){
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[i]+y];
					}
					for(int y=0;y<2;y++){
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[j]+y];
					}
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
					all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_TWO_DOUBLE;
					have=true;
				}	
			}	
			for(int j=AnalyseData.bThreeCount-1;j>=0;j--){
				for(int x=AnalyseData.bOneCount-1;x>=0;x--){
					for(int three_index_one=0;three_index_one<2;three_index_one++){
						for(int three_index_two=three_index_one+1;three_index_two<3;three_index_two++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_one];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_two];
							for(int y=0;y<2;y++){
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[j]+y];
							}
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bOneFirst[x]];
							all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_TWO_DOUBLE;
							have=true;
						}
					}
				}	
				for(int x=bCardCount-1;x>=0;x--){
					if(get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bTwoFirst[i]])
							|| get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bThreeFirst[j]])){
						continue;
					}
					for(int three_index_one=0;three_index_one<2;three_index_one++){
						for(int three_index_two=three_index_one+1;three_index_two<3;three_index_two++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_one];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_two];
							for(int y=0;y<2;y++){
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[j]+y];
							}
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
							all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_TWO_DOUBLE;
							have=true;
						}
					}
				}	
			}
		}
		for(int i=0;i<AnalyseData.bThreeCount-1;i++){
			for(int j=AnalyseData.bThreeCount-1;j>=0;j--){
				if(i == j){
					continue;
				}
				for(int x=AnalyseData.bOneCount-1;x>=0;x--){
					for(int three_index_one=0;three_index_one<2;three_index_one++){
						for(int three_index_two=three_index_one+1;three_index_two<3;three_index_two++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+three_index_one];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+three_index_two];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_one];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_two];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bOneFirst[x]];
							all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_TWO_DOUBLE;
							have=true;
						}
					}
				}	
				for(int x=bCardCount-1;x>=0;x--){
					if(get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bThreeFirst[i]])
							|| get_card_value(bCardData[x])==get_card_value(bCardData[AnalyseData.bThreeFirst[j]])){
						continue;
					}
					for(int three_index_one=0;three_index_one<2;three_index_one++){
						for(int three_index_two=three_index_one+1;three_index_two<3;three_index_two++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+three_index_one];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+three_index_two];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_one];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+three_index_two];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
							all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_TWO_DOUBLE;
							have=true;
						}
					}
				}	
			}	
		}
		return have;
	}
	public boolean search_wu_long(int bCardData[], int bCardCount,search_all_card_type all_type,boolean is_tou_dao){
		for(int i=0;i<bCardCount;i++){
			for(int j=bCardCount-1;j>i;j--){
				if(get_card_value(bCardData[i])==get_card_value(bCardData[j])){
					continue;
				}
				for(int x=j-1;x>i;x--){
					if(get_card_value(bCardData[j])==get_card_value(bCardData[x])){
						continue;
					}
					for(int y=x-1;y>i;y--){
						if(get_card_value(bCardData[y])==get_card_value(bCardData[x])){
							continue;
						}
						for(int z=y-1;z>i;z--){
							if(get_card_value(bCardData[y])==get_card_value(bCardData[z])){
								continue;
							}
							int card_data_temp[]=new int[bCardCount];
							card_data_temp[0]=bCardData[i];
							card_data_temp[1]=bCardData[j];
							card_data_temp[2]=bCardData[x];
							card_data_temp[3]=bCardData[y];
							card_data_temp[4]=bCardData[z];
							if(this.GetCardType(card_data_temp, 5) != GameConstants.SSZ_CT_SINGLE){
								continue;
							}
							
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[i];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[j];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[y];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[z];
							all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_SINGLE;
						}
					}
				}
			}
		}
		return true;
	}
	//搜索三条
	public boolean search_san_tiao(int bCardData[], int bCardCount,search_all_card_type all_type,boolean is_tou_dao){
		boolean have=false;
		SortCardList(bCardData, bCardCount);
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(bCardData, bCardCount, AnalyseData);
		if(AnalyseData.bThreeCount == 0){
			return false;
		}
		
		if(is_tou_dao){
			for(int i=0;i<AnalyseData.bThreeCount;i++){
				for(int y=0;y<3;y++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+y];
				}
				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_THREE;
				have=true;
			}
		}else{
			for(int i=0;i<AnalyseData.bThreeCount;i++){
				for(int j=bCardCount-1;j>=0;j--){
					
					for(int x=j-1;x>=0;x--){
						if(get_card_value(bCardData[AnalyseData.bThreeFirst[i]]) == get_card_value(bCardData[j])
						||get_card_value(bCardData[AnalyseData.bThreeFirst[i]]) == get_card_value(bCardData[x])
						||get_card_value(bCardData[j]) == get_card_value(bCardData[x])){
							continue;
						}
						for(int y=0;y<3;y++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+y];
						}
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[j];
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_THREE;
						have=true;
					}	
				}
			}
		}

		return have;
	}
	//搜索顺子
	public boolean search_shun_zi(int bCardData[], int bCardCount,search_all_card_type all_type){
		boolean have=false;
		int first_index=0;
		int first_value=GetCardLogicValue(bCardData[first_index]);
		int prv_value=GetCardLogicValue(bCardData[first_index]);
		this.SortCardList(bCardData, bCardCount);
		
		for(int one_index=first_index;one_index<bCardCount;one_index++){
			for(int two_index=one_index+1;two_index<bCardCount;two_index++){
				if(GetCardLogicValue(bCardData[one_index]) > GetCardLogicValue(bCardData[two_index])+1){
					break;
				}
				if(GetCardLogicValue(bCardData[one_index]) != GetCardLogicValue(bCardData[two_index])+1){
					continue;
				}
				for(int three_index=two_index+1;three_index<bCardCount;three_index++){
					if(GetCardLogicValue(bCardData[two_index]) > GetCardLogicValue(bCardData[three_index])+1){
						break;
					}
					if(GetCardLogicValue(bCardData[two_index]) != GetCardLogicValue(bCardData[three_index])+1){
						continue;
					}
					for(int four_index=three_index+1;four_index<bCardCount;four_index++){
						if(GetCardLogicValue(bCardData[three_index]) > GetCardLogicValue(bCardData[four_index])+1){
							break;
						}
						if(GetCardLogicValue(bCardData[three_index]) != GetCardLogicValue(bCardData[four_index])+1){
							continue;
						}
						for(int five_index=four_index+1;five_index<bCardCount;five_index++){
							if(GetCardLogicValue(bCardData[four_index]) > GetCardLogicValue(bCardData[five_index])+1){
								break;
							}
							if(GetCardLogicValue(bCardData[four_index]) != GetCardLogicValue(bCardData[five_index])+1){
								continue;
							}
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[one_index];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[two_index];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[three_index];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[four_index];
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[five_index];
							if(get_card_value(bCardData[one_index]) == 1){
								all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_BACK_A;
							}else{
								all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_NO_A;
							}
						}
					}
				}
			}
		}
		
		if(this.get_card_value(bCardData[0]) == 1){
			//A最小
			SortCardList_A_Small(bCardData, bCardCount);
			for(int one_index=bCardCount-1;one_index>=0;one_index--){
				if(get_card_value(bCardData[one_index]) != 1){
					break;
				}
				for(int two_index=one_index-1;two_index>=0;two_index--){
					if(get_card_value(bCardData[one_index]) < get_card_value(bCardData[two_index])-1){
						break;
					}
					if(get_card_value(bCardData[one_index]) != get_card_value(bCardData[two_index])-1){
						continue;
					}
					for(int three_index=two_index-1;three_index>=0;three_index--){
						if(get_card_value(bCardData[two_index]) < get_card_value(bCardData[three_index])-1){
							break;
						}
						if(get_card_value(bCardData[two_index]) != get_card_value(bCardData[three_index])-1){
							continue;
						}
						for(int four_index=three_index-1;four_index>=0;four_index--){
							if(get_card_value(bCardData[three_index]) < get_card_value(bCardData[four_index])-1){
								break;
							}
							if(get_card_value(bCardData[three_index]) != get_card_value(bCardData[four_index])-1){
								continue;
							}
							for(int five_index=four_index+1;five_index>=0;five_index--){
								if(get_card_value(bCardData[four_index]) < get_card_value(bCardData[five_index])-1){
									break;
								}
								if(get_card_value(bCardData[four_index]) != get_card_value(bCardData[five_index])-1){
									continue;
								}
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[five_index];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[four_index];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[three_index];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[two_index];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[one_index];
								if(get_card_value(bCardData[one_index]) == 1){
									all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_FIRST_A;
								}else{
									all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_NO_A;
								}
							}
						}
					}
				}
			}
		}

		

		return have;
	}
	//搜索同花
	public boolean search_tong_hua(int bCardData[], int bCardCount,search_all_card_type all_type){
		boolean have=false;
		//按花色排序
		SortCardList_By_Color(bCardData, bCardCount);
		for(int i=bCardCount-1;i>=0;i--){
			for(int j=i-1;j>=0;j--){
				for(int x=j-1;x>=0;x--){
					for(int y=x-1;y>=0;y--){
						for(int z=y-1;z>=0;z--){
							if(get_card_color(bCardData[i]) == get_card_color(bCardData[j])
							   &&get_card_color(bCardData[i]) == get_card_color(bCardData[x])
							   &&get_card_color(bCardData[i]) == get_card_color(bCardData[y])
							   &&get_card_color(bCardData[i]) == get_card_color(bCardData[z])){
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[i];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[j];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[x];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[y];
								all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[z];
								this.SortCardList(all_type.type_card_data[all_type.card_type_count], 5);
								all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_FLUSH;
								have=true;
							}
						}
					}
					
				}
			}
		}
		return have;
	}
	//搜索同花顺
	public boolean search_tong_hua_shun(int bCardData[], int bCardCount,search_all_card_type all_type){
		boolean have=false;
		//按花色排序
		//A最大
		SortCardList_By_Color(bCardData, bCardCount);
		int first_index=0;
		int first_value=GetCardLogicValue(bCardData[first_index]);
		int prv_value=GetCardLogicValue(bCardData[first_index]);
		for(int i=1;i<bCardCount;i++){
			if(get_card_color(bCardData[first_index]) != get_card_color(bCardData[i])){
				if(first_value-prv_value==4){
					for(int j=0;j<5;j++){
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
					}
					all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
					have=true;
				}
				first_index=i;
			}else{
				if(GetCardLogicValue(bCardData[first_index])-(i-first_index) != GetCardLogicValue(bCardData[i])){
					if(first_value-prv_value==4){
						for(int j=0;j<5;j++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
						}
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
						have=true;
					}
					first_index=i;
				}else if(first_value-prv_value==4){
					for(int j=0;j<5;j++){
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
					}
					all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
					first_index=first_index+1;
					have=true;
				}
			}
			first_value=GetCardLogicValue(bCardData[first_index]);
			prv_value=GetCardLogicValue(bCardData[i]);
//			if(first_value-prv_value==4){
//				for(int j=0;j<5;j++){
//					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
//				}
//				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
//				first_index=first_index+1;
//				have=true;
//			}
		}
		
		//A最小
		SortCardList_By_Color_A_SMALL(bCardData, bCardCount);
		first_index=0;
		first_value=GetCardLogicValue(bCardData[first_index]);
		prv_value=GetCardLogicValue(bCardData[first_index]);
		for(int i=0;i<bCardCount;i++){
			if(get_card_color(bCardData[first_index]) != get_card_color(bCardData[i])){
				if(first_value-prv_value==4){
					for(int j=0;j<5;j++){
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
					}
					if(this.get_card_value(bCardData[first_index]) == 1){
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A;
					}else{
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
					}
					have=true;
				}
				first_index=i;
			}else{
				if(get_card_value(bCardData[first_index])-(i-first_index) != get_card_value(bCardData[i])){
					if(first_value-prv_value==4){
						for(int j=0;j<5;j++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
						}
						if(this.get_card_value(bCardData[first_index]) == 1){
							all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A;
						}else{
							all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
						}
						have=true;
					}
					first_index=i;
				}else if(first_value-prv_value==4){
					for(int j=0;j<5;j++){
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
					}
					if(this.get_card_value(bCardData[first_index]) == 1){
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A;
					}else{
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
					}
					first_index=first_index+1;
					have=true;
				}
			}
			first_value=GetCardLogicValue(bCardData[first_index]);
			prv_value=GetCardLogicValue(bCardData[i]);
			
//			if(first_value-prv_value==4){
//				for(int j=0;j<5;j++){
//					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[first_index+j];
//				}
//				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH;
//				first_index=first_index+1;
//				have=true;
//			}
		}
		return have;
	}
	//搜索铁枝
	public boolean search_tie_zhi(int bCardData[], int bCardCount,search_all_card_type all_type){
		boolean have=false;
		this.SortCardList(bCardData, bCardCount);
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(bCardData, bCardCount, AnalyseData);
		if(AnalyseData.bFourCount == 0){
			return false;
		}
		
		for(int i=0;i<AnalyseData.bFourCount;i++){
			for(int j=AnalyseData.bOneCount-1;j>=0;j--){
				for(int x=0;x<4;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bFourFirst[i]+x];
				}
				all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bOneFirst[j]];
				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_FOUR_ONE;
				have=true;
			}
			for(int j=AnalyseData.bTwoCount-1;j>=0;j--){
				for(int x=0;x<4;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bFourFirst[i]+x];
				}
				all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[j]];
				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_FOUR_ONE;
				have=true;
			}
			for(int j=AnalyseData.bThreeCount-1;j>=0;j--){
				for(int x=0;x<4;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bFourFirst[i]+x];
				}
				all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]];
				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_FOUR_ONE;
				have=true;
			}
			for(int j=AnalyseData.bFourCount-1;j>=0;j--){
				if(i == j){
					continue;
				}
				for(int x=0;x<4;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bFourFirst[i]+x];
				}
				all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bFourFirst[j]];
				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_FOUR_ONE;
				have=true;
			}
		}
		return have;
	}
	//搜索葫芦
	public boolean search_hulu(int bCardData[], int bCardCount,search_all_card_type all_type){
		boolean have=false;
		this.SortCardList(bCardData, bCardCount);
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(bCardData, bCardCount, AnalyseData);
		if(AnalyseData.bThreeCount + AnalyseData.bTwoCount <2){
			return false;
		}
		
		for(int i=0;i<AnalyseData.bThreeCount;i++){
			for(int j=AnalyseData.bTwoCount-1;j>=0;j--){
				for(int x=0;x<3;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+x];
				}
				for(int x=0;x<2;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[j]+x];
				}
				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE;
				have=true;
			}
			for(int j=AnalyseData.bThreeCount-1;j>=0;j--){
				if(i == j){
					continue;
				}
				for(int x=0;x<2;x++){
					for(int y=x+1;y<3;y++){
						if(y == x){
							continue;
						}
						for(int z=0;z<3;z++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[i]+z];
						}
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+x];
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+y];
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE;
						have=true;
					}
				}

			}
		}
		
		for(int i=0;i<AnalyseData.bFourCount;i++){
			for(int j=AnalyseData.bTwoCount-1;j>=0;j--){
				for(int x=0;x<3;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bFourFirst[i]+x];
				}
				for(int x=0;x<2;x++){
					all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bTwoFirst[j]+x];
				}
				all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE;
				have=true;
			}
			for(int j=AnalyseData.bThreeCount-1;j>=0;j--){
				if(i == j){
					continue;
				}
				for(int x=0;x<2;x++){
					for(int y=x+1;y<3;y++){
						if(y == x){
							continue;
						}
						for(int z=0;z<3;z++){
							all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bFourFirst[i]+z];
						}
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+x];
						all_type.type_card_data[all_type.card_type_count][all_type.card_count[all_type.card_type_count]++]=bCardData[AnalyseData.bThreeFirst[j]+y];
						all_type.card_type[all_type.card_type_count++]=GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE;
						have=true;
					}
				}

			}
		}
		return have;
	}
	
	/*特殊牌型
	 * 
	 * 
	 * */
	//全红
	public boolean is_quan_hong(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=0;i<13;i++){
			if(get_card_color(cb_card_data_temp[i])!=0 && get_card_color(cb_card_data_temp[i])!=2){
				return false;
			}
		}
		return true;
	}
	//全黑
	public boolean is_quan_hei(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=0;i<13;i++){
			if(get_card_color(cb_card_data_temp[i])!=1 && get_card_color(cb_card_data_temp[i])!=3){
				return false;
			}
		}
		return true;
	}	
	//全红一点黑
	public boolean is_quan_hong_yi_dian_hei(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		int count=0;
		for(int i=0;i<13;i++){
			if(get_card_color(cb_card_data_temp[i])!=0 && get_card_color(cb_card_data_temp[i])!=2){
				count++;
			}
			if(count>1){
				return false;
			}
		}
		if(count != 1){
			return false;
		}

		return true;
	}	
	//全黑一点红
	public boolean is_quan_hei_yi_dian_hong(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		int count=0;
		for(int i=0;i<13;i++){
			if(get_card_color(cb_card_data_temp[i])!=1 && get_card_color(cb_card_data_temp[i])!=3){
				count++;
			}
			if(count>1){
				return false;
			}
		}
		if(count != 1){
			return false;
		}

		return true;
	}
	/*几花
	 * @param:牌数据
	 * @param:牌数量
	 * @param:几花
	 * */
	public boolean is_ji_hua(int bCardData[], int bCardCount,int num_hua){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		int count=0;
		for(int i=0;i<13;i++){
			if(get_card_value(cb_card_data_temp[i])>10){
				count++;
			}
		}
		if(count != num_hua){
			return false;
		}
		return true;
	}
	/*几起
	 * @param:牌数据
	 * @param:牌数量
	 * @param:几起
	 * */
	public boolean is_ji_qi(int bCardData[], int bCardCount,int num_qi){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=0;i<13;i++){
			if(GetCardLogicValue(cb_card_data_temp[i])<num_qi){
				return false;
			}
		}
		return true;
	}

	//四对一顺
	public boolean is_si_dui_yi_shun(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bTwoCount != 4 || AnalyseData.bOneCount != 5){
			return false;
		}
		
		boolean is_shun_zi=true;
		//A在后
		for(int i=1;i<AnalyseData.bOneCount;i++){
			if(GetCardLogicValue(cb_card_data_temp[AnalyseData.bOneFirst[i]])+1 != GetCardLogicValue(cb_card_data_temp[AnalyseData.bOneFirst[0]])){
				is_shun_zi=false;
			}
		}
		
		//A在前
		if(!is_shun_zi){
			for(int i=1;i<AnalyseData.bOneCount;i++){
				if(get_card_value(cb_card_data_temp[AnalyseData.bOneFirst[i]])+1 != get_card_value(cb_card_data_temp[AnalyseData.bOneFirst[0]])){
					is_shun_zi=false;
				}
			}
		}
		return is_shun_zi;
	}
	//四对一同
	public boolean is_si_dui_yi_tong(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		int card_count_temp=bCardCount;
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bTwoCount+AnalyseData.bThreeCount+AnalyseData.bFourCount*2 != 4){
			return false;
		}
		
		for(int i=0;i<card_count_temp;i++){
			int color=this.get_card_color(cb_card_data_temp[i]);
			for(int j=i+1;j<card_count_temp;j++){
				if(this.get_card_color(cb_card_data_temp[j]) == color){
					for(int x=j+1;x<card_count_temp;x++){
						if(this.get_card_color(cb_card_data_temp[x]) == color){
							for(int y=x+1;y<card_count_temp;y++){
								if(this.get_card_color(cb_card_data_temp[y]) == color){
									for(int z=y+1;z<card_count_temp;z++){
										if(this.get_card_color(cb_card_data_temp[z]) == color){
											int remove_cards[]=new int[bCardCount];
											int remove_count=5;
											remove_cards[0]=cb_card_data_temp[i];
											remove_cards[1]=cb_card_data_temp[j];
											remove_cards[2]=cb_card_data_temp[x];
											remove_cards[3]=cb_card_data_temp[y];
											remove_cards[4]=cb_card_data_temp[z];
											for(int card_index=0;card_index<bCardCount;card_index++){
												cb_card_data_temp[card_index]=bCardData[card_index];
											}
											remove_cards_by_data(cb_card_data_temp, bCardCount, remove_cards, remove_count);
											card_count_temp-=5;
											this.AnalyseCard(cb_card_data_temp, card_count_temp, AnalyseData);
											if(AnalyseData.bTwoCount== 4){
												return true;
											}
										}
										
									}
								}
							}
						}
						
					}
				}
				
			}
		}
		return false;
	}
	//半小
	public boolean is_ban_xiao(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			if(this.get_card_value(bCardData[i])>10){
				return false;
			}
		}
		return true;
	}
	//独四
	public boolean is_du_si(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bFourCount != 1 || AnalyseData.bOneCount != 9){
			return false;
		}

		return true;
	}
	//独三
	public boolean is_du_san(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bThreeCount != 1 || AnalyseData.bOneCount != 10){
			return false;
		}

		return true;
	}
	//独对
	public boolean is_du_dui(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bTwoCount != 1 || AnalyseData.bOneCount != 11){
			return false;
		}

		return true;
	}
	//三顺子
	public boolean is_san_shun_zi(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		int cb_card_data_finish[]=new int[bCardCount];
		int cb_card_data_one[]=new int[bCardCount];
		int cb_card_data_two[]=new int[bCardCount];
		int cb_card_data_three[]=new int[bCardCount];
		int first_count=0;
		int second_count=0;
		int three_count=0;
		int pre_count=0;
		int bCardCount_temp=bCardCount;
		for(int i=0;i<bCardCount_temp;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		int shun_zi_count=1;
		int pre_value=this.GetCardLogicValue(cb_card_data_temp[0]);
		int remove_cards[]=new int[1];
		remove_cards[0]=cb_card_data_temp[0];
		cb_card_data_finish[0]=cb_card_data_temp[0];
		this.remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
		bCardCount_temp--;
		boolean is_shun_zi=true;
		for(int i=0;i<bCardCount_temp;){
			if(pre_value-this.GetCardLogicValue(cb_card_data_temp[i]) == 1){
				cb_card_data_finish[shun_zi_count]=cb_card_data_temp[i];
				shun_zi_count++;
				pre_value=this.GetCardLogicValue(cb_card_data_temp[i]);
				remove_cards[0]=cb_card_data_temp[i];
				remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
				bCardCount_temp--;
				if(bCardCount_temp ==0 && shun_zi_count == 13){
					if(first_count == 0){
						int card_index=0;
						for(int j=pre_count;j<shun_zi_count;j++){
							cb_card_data_one[card_index++]=cb_card_data_finish[j];
							first_count++;
						}
					}else{
						int card_index=0;
						for(int j=pre_count;j<shun_zi_count;j++){
							cb_card_data_two[card_index++]=cb_card_data_finish[j];
							second_count++;
						}
					}
					break;
				}
			}else{
				i++;
				if(i == bCardCount_temp){
					if(shun_zi_count != 3 && shun_zi_count!=5 && shun_zi_count !=8 && shun_zi_count!=10&& shun_zi_count!=13
							|| ((shun_zi_count == 5 || shun_zi_count == 10) && first_count != 0)){
						is_shun_zi=false;
						break;
					}
					if(shun_zi_count == 3 || shun_zi_count == 8 ||shun_zi_count == 13 ){
						if(first_count == 0){
							int card_index=0;
							for(int j=pre_count;j<shun_zi_count;j++){
								cb_card_data_one[card_index++]=cb_card_data_finish[j];
								first_count++;
							}
						}else{
							if(three_count<5){
								int card_index=0;
								for(int j=pre_count;j<shun_zi_count;j++){
									cb_card_data_three[card_index++]=cb_card_data_finish[j];
									three_count++;
								}
							}else{
								int card_index=0;
								for(int j=pre_count;j<shun_zi_count;j++){
									cb_card_data_two[card_index++]=cb_card_data_finish[j];
									second_count++;
								}
							}
						}
						
					}else{
						if(three_count<5){
							int card_index=0;
							for(int j=pre_count;j<shun_zi_count;j++){
								cb_card_data_three[card_index++]=cb_card_data_finish[j];
								three_count++;
							}
						}else{
							int card_index=0;
							for(int j=pre_count;j<shun_zi_count;j++){
								cb_card_data_two[card_index++]=cb_card_data_finish[j];
								second_count++;
							}
						}
					}
					
					if(shun_zi_count != 13){
						pre_count=shun_zi_count;
						cb_card_data_finish[shun_zi_count]=cb_card_data_temp[0];
						i=0;
						shun_zi_count++;
						pre_value=this.GetCardLogicValue(cb_card_data_temp[0]);
						remove_cards[0]=cb_card_data_temp[0];
						this.remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
						bCardCount_temp--;
					}
				}else if((shun_zi_count==5 || shun_zi_count==10) && first_count == 0){
					if(three_count<5){
						int card_index=0;
						for(int j=pre_count;j<shun_zi_count;j++){
							cb_card_data_three[card_index++]=cb_card_data_finish[j];
							three_count++;
						}
					}else{
						int card_index=0;
						for(int j=pre_count;j<shun_zi_count;j++){
							cb_card_data_two[card_index++]=cb_card_data_finish[j];
							second_count++;
						}
					}
					pre_count=shun_zi_count;
					cb_card_data_finish[shun_zi_count]=cb_card_data_temp[0];
					i=0;
					shun_zi_count++;
					pre_value=this.get_card_value(cb_card_data_temp[0]);
					remove_cards[0]=cb_card_data_temp[0];
					this.remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
					bCardCount_temp--;
				}
				
			}
		}
		
		
		//A在前
		if(!is_shun_zi && shun_zi_count != 13){
			bCardCount_temp=bCardCount;
			for(int i=0;i<bCardCount_temp;i++){
				cb_card_data_temp[i]=bCardData[i];
			}
			this.SortCardList_A_Small(cb_card_data_temp, bCardCount_temp);
			shun_zi_count=1;
			first_count=0;
			second_count=0;
			three_count=0;
			pre_count=0;
			cb_card_data_finish[0]=cb_card_data_temp[0];
			pre_value=this.get_card_value(cb_card_data_temp[0]);
			remove_cards[0]=cb_card_data_temp[0];
			this.remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
			bCardCount_temp--;
			is_shun_zi=true;
			for(int i=0;i<bCardCount_temp;){
				if(pre_value-this.get_card_value(cb_card_data_temp[i]) == 1){
					cb_card_data_finish[shun_zi_count]=cb_card_data_temp[i];
					shun_zi_count++;
					pre_value=this.get_card_value(cb_card_data_temp[i]);
					remove_cards[0]=cb_card_data_temp[i];
					remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
					bCardCount_temp--;
					if(bCardCount_temp ==0 && shun_zi_count == 13){
						if(first_count == 0){
							int card_index=0;
							for(int j=pre_count;j<shun_zi_count;j++){
								cb_card_data_one[card_index++]=cb_card_data_finish[j];
								first_count++;
							}
						}else{
							int card_index=0;
							for(int j=pre_count;j<shun_zi_count;j++){
								cb_card_data_two[card_index++]=cb_card_data_finish[j];
								second_count++;
							}
						}
						break;
					}
				}else{
					i++;
					if(i == bCardCount_temp){
						if(shun_zi_count != 3 && shun_zi_count!=5 && shun_zi_count !=8 && shun_zi_count!=10&& shun_zi_count!=13
						|| ((shun_zi_count == 5 || shun_zi_count == 10) && first_count != 0)){
							is_shun_zi=false;
							break;
						}else{
							if(shun_zi_count == 3 || shun_zi_count == 8 ||shun_zi_count == 13 ){
								if(first_count == 0){
									int card_index=0;
									for(int j=pre_count;j<shun_zi_count;j++){
										cb_card_data_one[card_index++]=cb_card_data_finish[j];
										first_count++;
									}
								}else{
									if(three_count<5){
										int card_index=0;
										for(int j=pre_count;j<shun_zi_count;j++){
											cb_card_data_three[card_index++]=cb_card_data_finish[j];
											three_count++;
										}
									}else{
										int card_index=0;
										for(int j=pre_count;j<shun_zi_count;j++){
											cb_card_data_two[card_index++]=cb_card_data_finish[j];
											second_count++;
										}
									}
								}
							}else{
								if(three_count<5){
									int card_index=0;
									for(int j=pre_count;j<shun_zi_count;j++){
										cb_card_data_three[card_index++]=cb_card_data_finish[j];
										three_count++;
									}
								}else{
									int card_index=0;
									for(int j=pre_count;j<shun_zi_count;j++){
										cb_card_data_two[card_index++]=cb_card_data_finish[j];
										second_count++;
									}
								}
							}
						}
						if(shun_zi_count != 13){
							pre_count=shun_zi_count;
							cb_card_data_finish[shun_zi_count]=cb_card_data_temp[0];
							i=0;
							shun_zi_count++;
							pre_value=this.get_card_value(cb_card_data_temp[0]);
							remove_cards[0]=cb_card_data_temp[0];
							this.remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
							bCardCount_temp--;
						}
					}else if((shun_zi_count==5 || shun_zi_count==10) && first_count == 0){
						if(three_count<5){
							int card_index=0;
							for(int j=pre_count;j<shun_zi_count;j++){
								cb_card_data_three[card_index++]=cb_card_data_finish[j];
								three_count++;
							}
						}else{
							int card_index=0;
							for(int j=pre_count;j<shun_zi_count;j++){
								cb_card_data_two[card_index++]=cb_card_data_finish[j];
								second_count++;
							}
						}
						pre_count=shun_zi_count;
						cb_card_data_finish[shun_zi_count]=cb_card_data_temp[0];
						i=0;
						shun_zi_count++;
						pre_value=this.get_card_value(cb_card_data_temp[0]);
						remove_cards[0]=cb_card_data_temp[0];
						this.remove_cards_by_data(cb_card_data_temp, bCardCount_temp, remove_cards, 1);
						bCardCount_temp--;
					}
					
				}
			}
		}		
		if(is_shun_zi && shun_zi_count == 13){
			int card_index=0;
			for(int i=0;i<first_count;i++){
				bCardData[card_index++]=cb_card_data_one[i];
			}
			for(int i=0;i<second_count;i++){
				bCardData[card_index++]=cb_card_data_two[i];
			}
			for(int i=0;i<three_count;i++){
				bCardData[card_index++]=cb_card_data_three[i];
			}
			return true;
		}
		return false;
	}
	//六对半
	public boolean is_liu_dui_ban(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bTwoCount != 6){
			return false;
		}

		return true;
	}
	//五对三条
	public boolean is_wu_dui_san_tiao(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bThreeCount != 1 || AnalyseData.bTwoCount != 5){
			return false;
		}

		return true;
	}
	//四套三条
	public boolean is_si_tao_san_tiao(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bThreeCount != 4){
			return false;
		}

		return true;
	}
	//凑一色
	public boolean is_cou_yi_se(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=1;i<13;i++){
			if(get_card_color(cb_card_data_temp[i])%2!=get_card_color(cb_card_data_temp[0])%2){
				return false;
			}
		}

		return true;
	}
	//全小
	public boolean is_quan_xiao_zz(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=0;i<13;i++){
			if(GetCardLogicValue(cb_card_data_temp[i])>10){
				return false;
			}
		}

		return true;
	}
	//全小
	public boolean is_quan_xiao_jd(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=0;i<13;i++){
			if(GetCardLogicValue(cb_card_data_temp[i])>8){
				return false;
			}
		}

		return true;
	}
	//全大
	public boolean is_quan_da(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=0;i<13;i++){
			if(GetCardLogicValue(cb_card_data_temp[i])<8){
				return false;
			}
		}

		return true;
	}
	//三分天下
	public boolean is_san_fen_tian_xia(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		tagAnalyseData AnalyseData=new tagAnalyseData();
		this.AnalyseCard(cb_card_data_temp, bCardCount, AnalyseData);
		if(AnalyseData.bFourCount != 3){
			return false;
		}

		return true;
	}
	//三同花顺
	public boolean is_san_tong_hua_shun(int bCardData[], int bCardCount){
		if(!is_san_tong_hua(bCardData,bCardCount)){
			return false;
		}
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		this.SortCardList_By_Color(cb_card_data_temp, bCardCount);
		
		
		//顺子判断
		int color_count=0;
		int shun_count=0;
		int first_index=0;
		int card_color=get_card_color(cb_card_data_temp[0]);
		boolean san_zhang=false;
		color_count=1;
		for(int i=1;i<bCardCount;i++){
			shun_count=0;
			if(get_card_color(cb_card_data_temp[i]) == card_color){
				color_count++;
			}else{
				boolean is_shunzi=true;
				for(int j=first_index;j<color_count;j++){
					if(GetCardLogicValue(cb_card_data_temp[first_index])-j == GetCardLogicValue(cb_card_data_temp[j])){
						shun_count++;
					}else{
						if(color_count != shun_count){
							return false;
						}
						if(san_zhang){
							if(shun_count!= 8 && shun_count != 13){
								is_shunzi=false;
								break;
							}
						}else{
							if(shun_count != 3 && shun_count!= 5 && shun_count!= 8 && shun_count != 13 && shun_count != 10){
								is_shunzi=false;
								break;
							}
							if(!san_zhang && (shun_count==3 && shun_count==8)){
								san_zhang=true;
							}
						}
						
					}
				}
				if(!is_shunzi){
					//A在前
					san_zhang=false;
					int cb_card_data_color[]=new int[color_count];
					for(int j=0;j<color_count;j++){
						cb_card_data_color[j]=cb_card_data_temp[i+j];
					}
					this.SortCardList_A_Small(cb_card_data_color, color_count);
					for(int j=first_index;j<color_count;j++){
						if(get_card_value(cb_card_data_color[first_index])-j == get_card_value(cb_card_data_color[j])){
							shun_count++;
						}else{
							if(color_count != shun_count){
								return false;
							}
							if(san_zhang){
								if(shun_count!= 8 && shun_count != 13){
									is_shunzi=false;
									break;
								}
							}else{
								if(shun_count != 3 && shun_count!= 5 && shun_count!= 8 && shun_count != 13 && shun_count != 10){
									is_shunzi=false;
									break;
								}
								if(!san_zhang && (shun_count==3 && shun_count==8)){
									san_zhang=true;
								}
							}
						}
					}
				}
				if(!is_shunzi){
					return false;
				}
				first_index=color_count;
				card_color=get_card_color(cb_card_data_temp[first_index]);
				color_count=0;
			}
		}

		int card_index=0;
		color_count=1;
		card_color=get_card_color(cb_card_data_temp[0]);
		int index=0;
		for(int i=1;i<bCardCount;i++){
			if(get_card_color(cb_card_data_temp[i]) == card_color){
				color_count++;
				if(i == bCardCount-1){
					if(color_count == 3){
						for(int x=index;x<=i;x++){
							bCardData[card_index++]=cb_card_data_temp[x];
						}
					}
				}
			}else{
				if(color_count == 3){
					for(int x=index;x<i;x++){
						bCardData[card_index++]=cb_card_data_temp[x];
					}
				}
				color_count=1;
				card_color=get_card_color(cb_card_data_temp[i]);
				index=i;
			}
		}
		index=0;
		color_count=1;
		card_color=get_card_color(cb_card_data_temp[0]);
		for(int i=1;i<bCardCount;i++){
			if(get_card_color(cb_card_data_temp[i]) == card_color){
				color_count++;
				if(i == bCardCount-1){
					if(color_count == 5){
						for(int x=index;x<=i;x++){
							bCardData[card_index++]=cb_card_data_temp[x];
						}
					}
				}
			}else{
				if(color_count == 5){
					for(int x=index;x<i;x++){
						bCardData[card_index++]=cb_card_data_temp[x];
					}
				}
				card_color=get_card_color(cb_card_data_temp[i]);
				color_count=1;
				index=i;
			}
		}
		
		return true;
	}
	//十二皇族
	public boolean is_shier_huang_zu(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		for(int i=0;i<13;i++){
			if(GetCardLogicValue(cb_card_data_temp[i])<11){
				return false;
			}
		}

		return true;
	}
	//一条龙
	public boolean is_yitiao_long(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		int value=this.GetCardLogicValue(cb_card_data_temp[0]);
		for(int i=1;i<13;i++){
			if(value-i!=GetCardLogicValue(cb_card_data_temp[i])){
				return false;
			}
		}

		return true;
	}
	//至尊青龙
	public boolean is_zhizun_qing_long(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		this.SortCardList_A_Small(cb_card_data_temp, bCardCount);
		int value=cb_card_data_temp[0];
		for(int i=1;i<13;i++){
			if(value-i!=cb_card_data_temp[i]){
				return false;
			}
		}

		return true;
	}	
	//三同花
	public boolean is_san_tong_hua(int bCardData[], int bCardCount){
		int cb_card_data_temp[]=new int[bCardCount];
		for(int i=0;i<bCardCount;i++){
			cb_card_data_temp[i]=bCardData[i];
		}
		this.SortCardList_By_Color(cb_card_data_temp, bCardCount);
		
		int color_count=0;
		int card_color=get_card_color(cb_card_data_temp[0]);
		boolean san_zhang=false;
		color_count=1;
		for(int i=1;i<13;i++){
			if(get_card_color(cb_card_data_temp[i]) == card_color){
				color_count++;
			}else{
				if(san_zhang){
					if(color_count!= 8 && color_count != 13){
						return false;
					}
				}else{
					if(color_count != 3 && color_count!= 5 && color_count!= 8 && color_count != 13 && color_count != 10){
						return false;
					}
					if(san_zhang == false && (color_count == 3 || color_count == 8)){
						san_zhang=true;
					}
				}
				
				
				card_color=get_card_color(cb_card_data_temp[i]);
				color_count++;
			}
			
		}
		
		//三同花排序
		int card_index=0;
		if(color_count == 13){
			color_count=1;
			card_color=get_card_color(cb_card_data_temp[0]);
			int index=0;
			for(int i=1;i<bCardCount;i++){
				if(get_card_color(cb_card_data_temp[i]) == card_color){
					color_count++;
					if(i == bCardCount-1){
						if(color_count == 3){
							for(int x=index;x<=i;x++){
								bCardData[card_index++]=cb_card_data_temp[x];
							}
						}
					}
				}else{
					if(color_count == 3){
						for(int x=index;x<i;x++){
							bCardData[card_index++]=cb_card_data_temp[x];
						}
					}
					color_count=1;
					card_color=get_card_color(cb_card_data_temp[i]);
					index=i;
				}
			}
			index=0;
			color_count=1;
			card_color=get_card_color(cb_card_data_temp[0]);
			for(int i=1;i<bCardCount;i++){
				if(get_card_color(cb_card_data_temp[i]) == card_color){
					color_count++;
					if(i == bCardCount-1){
						if(color_count == 5){
							for(int x=index;x<=i;x++){
								bCardData[card_index++]=cb_card_data_temp[x];
							}
						}
					}
				}else{
					if(color_count == 5){
						for(int x=index;x<i;x++){
							bCardData[card_index++]=cb_card_data_temp[x];
						}
					}
					card_color=get_card_color(cb_card_data_temp[i]);
					color_count=1;
					index=i;
				}
			}
			return true;
		}
		return false;
	}
	
	//分析扑克
	public void AnalyseCard(int bCardDataList[],int bCardCount , tagAnalyseData AnalyseData) 
	{
		//排列扑克
		int bCardData[]=new int[13] ;
		for(int i=0;i<bCardCount;i++){
			bCardData[i]=bCardDataList[i];
		}
		SortCardList(bCardData , bCardCount , enSortCardType.enDescend) ;

		//变量定义
		int bSameCount = 1 ,
			 bCardValueTemp=0,
			 bSameColorCount = 1 ,
			 bFirstCardIndex = 0 ;	//记录下标

		int bLogicValue=GetCardLogicValue(bCardData[0]);
		int bCardColor = get_card_color(bCardData[0]) ;





		//设置结果
		AnalyseData.Reset();

		//扑克分析
		for (int i=1;i<bCardCount;i++)
		{
			//获取扑克
			bCardValueTemp=GetCardLogicValue(bCardData[i]);
			if (bCardValueTemp==bLogicValue) bSameCount++;

			//保存结果
			if ((bCardValueTemp!=bLogicValue)||(i==(bCardCount-1)))
			{
				switch (bSameCount)
				{
				case 1:		//一张
					break; 
				case 2:		//两张
					{
						AnalyseData.bTwoFirst[AnalyseData.bTwoCount]	 = bFirstCardIndex ;
						AnalyseData.bTwoCount++ ;
						break;
					}
				case 3:		//三张
					{
						AnalyseData.bThreeFirst[AnalyseData.bThreeCount] = bFirstCardIndex ;
						AnalyseData.bThreeCount++ ;
						break;
					}
				case 4:		//四张
					{
						AnalyseData.bFourFirst[AnalyseData.bFourCount]   = bFirstCardIndex ;
						AnalyseData.bFourCount++ ;
						break;
					}
				default:			
					break;
				}
			}

			//设置变量
			if (bCardValueTemp!=bLogicValue)
			{
				if(bSameCount==1)
				{
					if(i!=bCardCount-1)
					{
						AnalyseData.bOneFirst[AnalyseData.bOneCount]	= bFirstCardIndex ;
						AnalyseData.bOneCount++ ;
					}
					else
					{
						AnalyseData.bOneFirst[AnalyseData.bOneCount]	= bFirstCardIndex ;
						AnalyseData.bOneCount++ ;
						AnalyseData.bOneFirst[AnalyseData.bOneCount]	= i ;
						AnalyseData.bOneCount++ ;				
					}
				}
				else
				{
					if(i==bCardCount-1)
					{
						AnalyseData.bOneFirst[AnalyseData.bOneCount]	= i ;
						AnalyseData.bOneCount++ ;
					}
				}
				bSameCount=1;
				bLogicValue=bCardValueTemp;
				bFirstCardIndex = i ;

			}
			if(get_card_color(bCardData[i])!=bCardColor) bSameColorCount = 1 ;
			else									   ++bSameColorCount ;
		}

		//是否同花
		AnalyseData.bStraight = (5==bSameColorCount) ? true : false ;

		return;
	}
	
	//对比扑克
	public int CompareCard(int bInFirstList[], int bInNextList[], int bFirstCount, int bNextCount , boolean bComperWithOther, boolean bComperWithcolor)
	{

		tagAnalyseData FirstAnalyseData=new tagAnalyseData();
		tagAnalyseData NextAnalyseData = new tagAnalyseData();

		for(int i=0;i<bFirstCount;i++){
			if(bInFirstList[i] == GameConstants.INVALID_CARD){
				bNextCount=0;
				break;
			}
		}
		for(int i=0;i<bNextCount;i++){
			if(bInNextList[i] == GameConstants.INVALID_CARD){
				bNextCount=0;
				break;
			}
		}

		//排列扑克
		int bFirstList[]=new int[13];
		int bNextList[]=new int[13] ;
		for(int i=0;i<bFirstCount;i++){
			bFirstList[i]=bInFirstList[i];
		}
		for(int i=0;i<bNextCount;i++){
			bNextList[i]=bInNextList[i];
		}

		SortCardList(bFirstList , bFirstCount , enSortCardType.enDescend) ;
		SortCardList(bNextList , bNextCount , enSortCardType.enDescend) ;



		AnalyseCard(bFirstList , bFirstCount , FirstAnalyseData) ;

		AnalyseCard(bNextList  , bNextCount  , NextAnalyseData) ;

		if(bFirstCount != 0 && bNextCount==0){
			return -1;
		}
		if(bNextCount != 0 && bFirstCount==0){
			return 1;
		}
		if(bFirstCount!=(FirstAnalyseData.bOneCount+FirstAnalyseData.bTwoCount*2+FirstAnalyseData.bThreeCount*3+FirstAnalyseData.bFourCount*4+FirstAnalyseData.bFiveCount*5))
		{
			return -1 ;
		}
		if(bNextCount != (NextAnalyseData.bOneCount + NextAnalyseData.bTwoCount*2 + NextAnalyseData.bThreeCount*3+NextAnalyseData.bFourCount*4+NextAnalyseData.bFiveCount*5))
		{
			return 1 ;
		}
		//数据验证
		if(!((bFirstCount==bNextCount) || (bFirstCount!=bNextCount && (3==bFirstCount && 5==bNextCount || 5==bFirstCount && 3==bNextCount)))) return -1 ;

		//获取类型
		int bNextType=GetCardType(bNextList,bNextCount);
		int bFirstType=GetCardType(bFirstList,bFirstCount);
		bFirstType=GetCardType(bFirstList,bFirstCount);


		if(GameConstants.SSZ_CT_INVALID==bFirstType || GameConstants.SSZ_CT_INVALID==bNextType) return -1 ;

		//头段比较
		if(true==bComperWithOther){
			if(3==bFirstCount){
				//开始对比
				if(bNextType==bFirstType) {
					switch(bFirstType) 
					{
					case GameConstants.SSZ_CT_SINGLE:				//单牌类型
						{
							//数据验证
							//数据验证
							//if(bNextList[0]==bFirstList[0]) return false ;

							boolean bAllSame=true ;

							for(int i=0 ; i<3 ; ++i){
								if(GetCardLogicValue(bNextList[i]) != GetCardLogicValue(bFirstList[i]))
								{
									bAllSame = false ;
									break; 
								}
							}
							if(true==bAllSame){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}else{
								for(int i=0 ; i<3 ; ++i){
									if(GetCardLogicValue(bNextList[i]) != GetCardLogicValue(bFirstList[i])){
										if(GetCardLogicValue(bNextList[i]) > GetCardLogicValue(bFirstList[i]) ){
											return 1;
										}else{
											return -1;
										}
									}
								}
								return -1;
							}

						}
					case GameConstants.SSZ_CT_ONE_DOUBLE:			//对带一张
						{
							//数据验证
							if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]])==GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]])){
								if(GetCardLogicValue(bNextList[NextAnalyseData.bOneFirst[0]]) != GetCardLogicValue(bFirstList[FirstAnalyseData.bOneFirst[0]]))
									if(GetCardLogicValue(bNextList[NextAnalyseData.bOneFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bOneFirst[0]])){
										return 1;
									}else{
										return -1;
									}
								else{
									if(bComperWithcolor){
										if(bNextList[NextAnalyseData.bTwoFirst[0]] > bFirstList[FirstAnalyseData.bTwoFirst[0]]){
											return 1;
										}else if(bNextList[NextAnalyseData.bTwoFirst[0]] == bFirstList[FirstAnalyseData.bTwoFirst[0]]){
											return 0;
										}else{
											return -1;
										}
									}else{
										return 0;
									}
								}
							}else {
								if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]])){
									return 1;
								}else{
									return -1;
								}
							} 
						}	

					case GameConstants.SSZ_CT_THREE:				//三张牌型
						{
							//数据验证

							//if(bNextList[NextAnalyseData.bThreeFirst[0]]==bFirstList[FirstAnalyseData.bThreeFirst[0]]) return false ;
							if(GetCardLogicValue(bNextList[NextAnalyseData.bThreeFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bThreeFirst[0]])){
								return 1;
							}else if(GetCardLogicValue(bNextList[NextAnalyseData.bThreeFirst[0]]) < GetCardLogicValue(bFirstList[FirstAnalyseData.bThreeFirst[0]])){
								return -1;
							}else{
								return 0;
							}
						}		

					}

				}else{
					if(bNextType>bFirstType){
						return 1;
					}else{
						return -1;
					}
				}
			}else{
				//开始对比
				if(bNextType==bFirstType) 
				{

					switch(bFirstType) 
					{
					case GameConstants.SSZ_CT_SINGLE:				//单牌类型
						{
							boolean bAllSame=true ;
							for(int i=0 ; i<5 ; ++i){
								if(GetCardLogicValue(bNextList[i]) != GetCardLogicValue(bFirstList[i]))
								{
									bAllSame = false ;
									break; 
								}
							}
							if(true==bAllSame){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}else{
								for(int i=0 ; i<5 ; ++i){
									if(GetCardLogicValue(bNextList[i]) != GetCardLogicValue(bFirstList[i])){
										if(GetCardLogicValue(bNextList[i]) > GetCardLogicValue(bFirstList[i])){
											return 1;
										}else{
											return -1;
										}
									}
								}
							return -1 ;
							}

						}
					case GameConstants.SSZ_CT_ONE_DOUBLE:			//对带一张
						{
							//数据验证
							if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]])==GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]])){
								//对比单张
								for(int i=0; i<3; ++i)
								{
									if(GetCardLogicValue(bNextList[NextAnalyseData.bOneFirst[i]])!=GetCardLogicValue(bFirstList[FirstAnalyseData.bOneFirst[i]])){
										if(GetCardLogicValue(bNextList[NextAnalyseData.bOneFirst[i]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bOneFirst[i]])){
											return 1;
										}else{
											return -1;
										}
									}
								}
								if(bComperWithcolor){
									if(bNextList[NextAnalyseData.bTwoFirst[0]] > bFirstList[FirstAnalyseData.bTwoFirst[0]]){
										return 1;
									}else if(bNextList[NextAnalyseData.bTwoFirst[0]] > bFirstList[FirstAnalyseData.bTwoFirst[0]]){
										return 0;
									}else{
										return -1;
									}		//比较花色
								}else{
									return 0;
								}
								
							}else{
								if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]])){
									return 1;
								}else{
									return -1;
								}
							}
						}
					case GameConstants.SSZ_CT_FIVE_TWO_DOUBLE:	//两对牌型
						{
							//数据验证
							if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]])==GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]]))
							{
								if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[1]])==GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[1]])){
									if(GetCardLogicValue(bNextList[NextAnalyseData.bOneFirst[0]])!=GetCardLogicValue(bFirstList[FirstAnalyseData.bOneFirst[0]])){
										if(GetCardLogicValue(bNextList[NextAnalyseData.bOneFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bOneFirst[0]])){
											return 1;
										}else{
											return -1;
										}
									}
									if(bComperWithcolor){
										if(bNextList[NextAnalyseData.bTwoFirst[0]] > bFirstList[FirstAnalyseData.bTwoFirst[0]]){
											return 1;
										}else if(bNextList[NextAnalyseData.bTwoFirst[0]] > bFirstList[FirstAnalyseData.bTwoFirst[0]]){
											return 0;
										}else{
											return -1;
										}
									}else{
										return 0;
									}
									
								}else{
									if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[1]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[1]])){
										return 1;
									}else{
										return -1;
									}
								}
							}							
							else{
								if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]])){
									return 1;
								}else{
									return -1;
								}
							}
						}

					case GameConstants.SSZ_CT_THREE:				//三张牌型
						{
							//数据验证
							if( GetCardLogicValue(bNextList[NextAnalyseData.bThreeFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bThreeFirst[0]])){
								return 1;
							}else if(GetCardLogicValue(bNextList[NextAnalyseData.bThreeFirst[0]]) == GetCardLogicValue(bFirstList[FirstAnalyseData.bThreeFirst[0]])){
								
								return 0;
							}else{
								return -1;
							}
						}

					case GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_FIRST_A:				//A在前顺子
						{
							//数据验证
							//if(bNextList[0]==bFirstList[0]) return false ;

							if(GetCardLogicValue(bNextList[0]) == GetCardLogicValue(bFirstList[0])){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}
							else{
								if(GetCardLogicValue(bNextList[0]) > GetCardLogicValue(bFirstList[0])){
									return 1;
								}else{
									return -1;
								}
							}

						}
					case GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_NO_A:			//没A杂顺
						{
							if(GetCardLogicValue(bNextList[0]) == GetCardLogicValue(bFirstList[0])){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}
							else{
								if(GetCardLogicValue(bNextList[0]) > GetCardLogicValue(bFirstList[0])){
									return 1;
								}else{
									return -1;
								}
							}
						}
					case GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_BACK_A:		//A在后顺子
						{
							if(GetCardLogicValue(bNextList[0]) == GetCardLogicValue(bFirstList[0])){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}
							else{
								if(GetCardLogicValue(bNextList[0]) > GetCardLogicValue(bFirstList[0])){
									return 1;
								}else{
									return -1;
								}
							}

						}

					case GameConstants.SSZ_CT_FIVE_FLUSH:				//同花五牌
						{
							//数据验证
							//if(bNextList[0]==bFirstList[0]) return false ;

							//比较数值
							for(int i=0; i<5; ++i){
								if(GetCardLogicValue(bNextList[i]) != GetCardLogicValue(bFirstList[i])){
									if(GetCardLogicValue(bNextList[i]) > GetCardLogicValue(bFirstList[i])){
										return 1;
									}else{
										return -1;
									}
								}
							}
							if(bComperWithcolor){
								//比较花色
								if(bNextList[0] > bFirstList[0]){
									return 1;
								}else if(bNextList[0] == bFirstList[0]){
									return 0;
								}else{
									return -1;
								}
							}else{
								return 0;
							}
						}

					case GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE:			//三条一对
						{
							//数据验证
							if(GetCardLogicValue(bNextList[NextAnalyseData.bThreeFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bThreeFirst[0]])){
								return 1;
							}else if(GetCardLogicValue(bNextList[NextAnalyseData.bThreeFirst[0]]) < GetCardLogicValue(bFirstList[FirstAnalyseData.bThreeFirst[0]])){
								return -1;
							}else{
								if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]])){
									return 1;
								}else if(GetCardLogicValue(bNextList[NextAnalyseData.bTwoFirst[0]]) < GetCardLogicValue(bFirstList[FirstAnalyseData.bTwoFirst[0]])){
									return -1;
								}else{
									return 0;
								}
							}
						}

					case GameConstants.SSZ_CT_FIVE_FOUR_ONE:			//四带一张
						{
							//数据验证
							//if(bNextList[NextAnalyseData.bFourFirst[0]]==bFirstList[FirstAnalyseData.bFourFirst[0]]) return false ;
							if(GetCardLogicValue(bNextList[NextAnalyseData.bFourFirst[0]]) > GetCardLogicValue(bFirstList[FirstAnalyseData.bFourFirst[0]])){
								return 1;
							}else{
								return -1;
							}
						}

					case GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A://A在前同花顺
						{
							if(GetCardLogicValue(bNextList[0]) == GetCardLogicValue(bFirstList[0])){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}
							else{
								if(GetCardLogicValue(bNextList[0]) > GetCardLogicValue(bFirstList[0])){
									return 1;
								}else{
									return -1;
								}
							}
						}
					case GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH:		//同花顺牌
						{
							if(GetCardLogicValue(bNextList[0]) == GetCardLogicValue(bFirstList[0])){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}
							else{
								if(GetCardLogicValue(bNextList[0]) > GetCardLogicValue(bFirstList[0])){
									return 1;
								}else{
									return -1;
								}
							}
						}

					default:
						return -1 ;
					}

				}
				else
				{
					//同花顺牌
					if( bNextType==GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A || bNextType==GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH)
					{
						if(GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A==bFirstType || GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH==bFirstType)
						{
							if(GetCardLogicValue(bNextList[0]) == GetCardLogicValue(bFirstList[0])){
								if(bComperWithcolor){
									if(bNextList[0] > bFirstList[0]){
										return 1;
									}else if(bNextList[0] == bFirstList[0]){
										return 0;
									}else{
										return -1;
									}
								}else{
									return 0;
								}
							}
							else{
								if(GetCardLogicValue(bNextList[0]) > GetCardLogicValue(bFirstList[0])){
									return 1;
								}else{
									return -1;
								}
							}
						}
					}
					if(bNextType>bFirstType ){
						return 1;
					}else{
						return -1;
					}
				}
			}
		}


		return -1;
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
	//排列扑克
	public void SortCardList(int bCardData[], int bCardCount,enSortCardType SortCardType)
	{
		if(bCardCount<1 || bCardCount>13) return ;

		//转换数值
		int bLogicVolue[]=new int[13];
		for (int i=0;i<bCardCount;i++)	bLogicVolue[i]=GetCardLogicValue(bCardData[i]);	

		if(enSortCardType.enDescend==SortCardType)
		{
			//排序操作
			boolean bSorted=true;
			int bTempData,bLast=bCardCount-1;
			int m_bCardCount=1;
			do
			{
				bSorted=true;
				for (int i=0;i<bLast;i++)
				{
					if ((bLogicVolue[i]<bLogicVolue[i+1])||
						((bLogicVolue[i]==bLogicVolue[i+1])&&(bCardData[i]<bCardData[i+1])))
					{
						//交换位置
						bTempData=bCardData[i];
						bCardData[i]=bCardData[i+1];
						bCardData[i+1]=bTempData;
						bTempData=bLogicVolue[i];
						bLogicVolue[i]=bLogicVolue[i+1];
						bLogicVolue[i+1]=bTempData;
						bSorted=false;
					}	
				}
				bLast--;
			} while(bSorted==false);
		}
		else if(enSortCardType.enAscend==SortCardType)
		{
			//排序操作
			boolean bSorted=true;
			int bTempData,bLast=bCardCount-1;
			int m_bCardCount=1;
			do
			{
				bSorted=true;
				for (int i=0;i<bLast;i++)
				{
					if ((bLogicVolue[i]>bLogicVolue[i+1])||
						((bLogicVolue[i]==bLogicVolue[i+1])&&(bCardData[i]>bCardData[i+1])))
					{
						//交换位置
						bTempData=bCardData[i];
						bCardData[i]=bCardData[i+1];
						bCardData[i+1]=bTempData;
						bTempData=bLogicVolue[i];
						bLogicVolue[i]=bLogicVolue[i+1];
						bLogicVolue[i+1]=bTempData;
						bSorted=false;
					}	
				}
				bLast--;
			} while(bSorted==false);
		}
		else if(enSortCardType.enColor==SortCardType)
		{
			//排序操作
			boolean bSorted=true;
			int bTempData,bLast=bCardCount-1;
			int m_bCardCount=1;
			int bColor[]=new int[13];
			for (int i=0;i<bCardCount;i++)	bColor[i]=get_card_color(bCardData[i]);	
			do
			{
				bSorted=true;
				for (int i=0;i<bLast;i++)
				{
					if ((bColor[i]<bColor[i+1])||
						((bColor[i]==bColor[i+1])&&(GetCardLogicValue(bCardData[i])<GetCardLogicValue(bCardData[i+1]))))
					{
						//交换位置
						bTempData=bCardData[i];
						bCardData[i]=bCardData[i+1];
						bCardData[i+1]=bTempData;
						bTempData=bColor[i];
						bColor[i]=bColor[i+1];
						bColor[i+1]=bTempData;
						bSorted=false;
					}	
				}
				bLast--;
			} while(bSorted==false);
		}
		return;
	}
	//排列扑克
	public void SortCardList(int bCardData[], int bCardCount)
	{
		enSortCardType SortCardType=enSortCardType.enDescend;
		if(bCardCount<1 || bCardCount>13) return ;

		//转换数值
		int bLogicVolue[]=new int[13];
		for (int i=0;i<bCardCount;i++)	bLogicVolue[i]=GetCardLogicValue(bCardData[i]);	

		if(enSortCardType.enDescend==SortCardType)
		{

			//排序操作
			boolean bSorted=true;
			int bTempData,bLast=bCardCount-1;
			int m_bCardCount=1;
			do
			{
				
				bSorted=true;
				for (int i=0;i<bLast;i++)
				{
					if ((bLogicVolue[i]<bLogicVolue[i+1])||
						((bLogicVolue[i]==bLogicVolue[i+1])&&(bCardData[i]<bCardData[i+1])))
					{
						//交换位置
						bTempData=bCardData[i];
						bCardData[i]=bCardData[i+1];
						bCardData[i+1]=bTempData;
						bTempData=bLogicVolue[i];
						bLogicVolue[i]=bLogicVolue[i+1];
						bLogicVolue[i+1]=bTempData;
						bSorted=false;
					}	
				}
				bLast--;
			} while(bSorted==false);
			int Type=GetCardType(bCardData,bCardCount);
			if(Type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A || Type == GameConstants.SSZ_CT_FIVE_MIXED_FLUSH_FIRST_A){
				for (int i=0;i<bCardCount;i++){
					if(bLogicVolue[i] == 14){
						bLogicVolue[i]=1;
					}
				}
				do
				{
					
					bSorted=true;
					bLast=bCardCount-1;
					for (int i=0;i<bLast;i++)
					{
						if ((bLogicVolue[i]<bLogicVolue[i+1])||
							((bLogicVolue[i]==bLogicVolue[i+1])&&(bCardData[i]<bCardData[i+1])))
						{
							//交换位置
							bTempData=bCardData[i];
							bCardData[i]=bCardData[i+1];
							bCardData[i+1]=bTempData;
							bTempData=bLogicVolue[i];
							bLogicVolue[i]=bLogicVolue[i+1];
							bLogicVolue[i+1]=bTempData;
							bSorted=false;
						}	
					}
					bLast--;
				} while(bSorted==false);
			}
			
		}
		else if(SortCardType.enAscend==SortCardType)
		{
			//排序操作
			boolean bSorted=true;
			int bTempData,bLast=bCardCount-1;
			int m_bCardCount=1;
			do
			{
				bSorted=true;
				for (int i=0;i<bLast;i++)
				{
					if ((bLogicVolue[i]>bLogicVolue[i+1])||
						((bLogicVolue[i]==bLogicVolue[i+1])&&(bCardData[i]>bCardData[i+1])))
					{
						//交换位置
						bTempData=bCardData[i];
						bCardData[i]=bCardData[i+1];
						bCardData[i+1]=bTempData;
						bTempData=bLogicVolue[i];
						bLogicVolue[i]=bLogicVolue[i+1];
						bLogicVolue[i+1]=bTempData;
						bSorted=false;
					}	
				}
				bLast--;
			} while(bSorted==false);
		}
		else if(enSortCardType.enColor==SortCardType)
		{
			//排序操作
			boolean bSorted=true;
			int bTempData,bLast=bCardCount-1;
			int m_bCardCount=1;
			int bColor[]=new int[13];
			for (int i=0;i<bCardCount;i++)	bColor[i]=get_card_color(bCardData[i]);	
			do
			{
				bSorted=true;
				for (int i=0;i<bLast;i++)
				{
					if ((bColor[i]<bColor[i+1])||
						((bColor[i]==bColor[i+1])&&(GetCardLogicValue(bCardData[i])<GetCardLogicValue(bCardData[i+1]))))
					{
						//交换位置
						bTempData=bCardData[i];
						bCardData[i]=bCardData[i+1];
						bCardData[i+1]=bTempData;
						bTempData=bColor[i];
						bColor[i]=bColor[i+1];
						bColor[i+1]=bTempData;
						bSorted=false;
					}	
				}
				bLast--;
			} while(bSorted==false);
		}
		return;
	}
	//排列扑克
	public void SortCardList_A_Small(int bCardData[], int bCardCount)
	{
		//转换数值
		int bLogicVolue[]=new int[13];
		for (int i=0;i<bCardCount;i++)	bLogicVolue[i]=get_card_value(bCardData[i]);	
		//排序操作
		boolean bSorted=true;
		int bTempData,bLast=bCardCount-1;
		int m_bCardCount=1;
		do
		{
			bSorted=true;
			for (int i=0;i<bLast;i++)
			{
				if ((bLogicVolue[i]<bLogicVolue[i+1])||
					((bLogicVolue[i]==bLogicVolue[i+1])&&(bCardData[i]<bCardData[i+1])))
				{
					//交换位置
					bTempData=bCardData[i];
					bCardData[i]=bCardData[i+1];
					bCardData[i+1]=bTempData;
					bTempData=bLogicVolue[i];
					bLogicVolue[i]=bLogicVolue[i+1];
					bLogicVolue[i+1]=bTempData;
					bSorted=false;
				}	
			}
			bLast--;
		} while(bSorted==false);
	}
	public void SortCardList_By_Color(int bCardData[], int bCardCount){
		//排序操作
		boolean bSorted=true;
		int bTempData,bLast=bCardCount-1;
		int bColor[]=new int[13];
		for (int i=0;i<bCardCount;i++)	bColor[i]=get_card_color(bCardData[i]);	
		int m_bCardCount=1;
		do
		{
			bSorted=true;
			for (int i=0;i<bLast;i++)
			{
				if ((bColor[i]<bColor[i+1])||
						((bColor[i]==bColor[i+1])&&(GetCardLogicValue(bCardData[i])<GetCardLogicValue(bCardData[i+1]))))
					{
						//交换位置
						bTempData=bCardData[i];
						bCardData[i]=bCardData[i+1];
						bCardData[i+1]=bTempData;
						bTempData=bColor[i];
						bColor[i]=bColor[i+1];
						bColor[i+1]=bTempData;
						bSorted=false;
					}
			}
			bLast--;
		} while(bSorted==false);
	}
	public void SortCardList_By_Color_A_SMALL(int bCardData[], int bCardCount){
		//排序操作
		boolean bSorted=true;
		int bTempData,bLast=bCardCount-1;
		int m_bCardCount=1;
		int bColor[]=new int[13];
		for (int i=0;i<bCardCount;i++)	bColor[i]=get_card_color(bCardData[i]);	
		do
		{
			bSorted=true;
			for (int i=0;i<bLast;i++)
			{
				if (bCardData[i]<bCardData[i+1])
				{
					//交换位置
					bTempData=bCardData[i];
					bCardData[i]=bCardData[i+1];
					bCardData[i+1]=bTempData;

					bSorted=false;
				}	
			}
			bLast--;
		} while(bSorted==false);
	}
	public int GetCardLogicValue(int CardData){
		//扑克属性
		int bCardColor=get_card_color(CardData);
		int bCardValue=get_card_value(CardData);

		//转换数值
		return (bCardValue==1)?(bCardValue+13):bCardValue;
	}

	// 获取数值
	public int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}
	public int pailie_zhengque(int cards[], int card_count){
		
		int bFirstList[]=new int[13];
		int bSecondList[]=new int[13] ;
		int bThreeList[]=new int[13] ;
		for(int i=0;i<3;i++){
			bFirstList[i]=cards[i];
		}
		for(int i=0;i<5;i++){
			bSecondList[i]=cards[3+i];
		}
		for(int i=0;i<5;i++){
			bThreeList[i]=cards[8+i];
		}
		if(CompareCard(bFirstList, bSecondList, 3, 5, true, has_rule(GameConstants.GAME_RULE_SSZ_BI_HUA_SE)) == -1){
			return 1;
		}
		if(CompareCard(bSecondList, bThreeList, 5, 5, true, has_rule(GameConstants.GAME_RULE_SSZ_BI_HUA_SE)) == -1){
			return 2;
		}
		return 0;
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
	
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}
}
