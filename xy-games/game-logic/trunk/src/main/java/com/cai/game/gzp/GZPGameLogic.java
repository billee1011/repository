/**
 * 
 */
package com.cai.game.gzp;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;

//类型子项
class KindItem {
	public int cbWeaveKind;// 组合类型
	public int cbCenterCard;// 中心扑克
	public int cbCardIndex[] = new int[3];// 扑克索引
	public int cbValidIndex[] = new int[3];// 实际扑克索引

	public KindItem() {

	}
}
enum EGZPType {
	GZP_YI, GZP_ER, GZP_SAN, GZP_SI, GZP_WU, GZP_LIU, GZP_QI, GZP_BA, GZP_JIU, GZP_SHI, GZP_SHANG, GZP_DA,
	GZP_REN, GZP_KONG, GZP_JI, GZP_KE, GZP_ZHI, GZP_LI, GZP_HUA, GZP_QIAN, GZP_TU, GZP_ZI,
	GZP_YI_HUA,GZP_SAN_HUA,GZP_WU_HUA,GZP_QI_HUA,GZP_JIU_HUA
}
// 分析子项
class AnalyseItem {
	public int cbCardEye[] = new int[4];//// 牌眼扑克
	public int cbCardIndex[] = new int[4]; //牌眼扑克位置
	public boolean bMagicEye;// 牌眼是否是王霸
	public int cbWeaveKind[] = new int[6];// 组合类型
	public int cbCenterCard[] = new int[6];// 中心扑克
	public int cbCardData[][] = new int[6][5]; // 实际扑克

	public int cbPoint;// 组合牌的最佳点数;

	public boolean curCardEye;// 当前摸的牌是否是牌眼
	public boolean isShuangDui;// 牌眼 true双对--判断碰碰胡
	public int eyeKind;// 牌眼 组合类型
	public int eyeCenterCard;// 牌眼 中心扑克
	public int cbGzshu[] = new int[6];
}

// 分析子项
class AnalyseItemTwenty {
	public int cbCardEye[] = new int[2];//// 牌眼扑克
	public int cbWeaveKind[] = new int[6];// 组合类型
	public int cbCenterCard[] = new int[6];// 中心扑克
	public int cbCardData[][] = new int[6][4]; // 实际扑克
	public boolean eyeDui;//// 牌眼是对子

	public int totalScore;
	public int hong;
	
	public boolean isDoubleCount;

	ChiHuRight subChiHuRight;
}

public class GZPGameLogic {

	private static Logger logger = Logger.getLogger(GZPGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	public GZPGameLogic() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.MAX_FLS_COUNT];
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
	public boolean is_card_flower(int card) {
		if(get_card_color(card) == 2)
			return true;
		else 
			return false;
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
		return _magic_card_index[index];// MJGameConstants.GZP_MAX_INDEX;
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

	// 删除扑克 by data
	public boolean remove_card_by_data(int cards[], int card_data) {
		int card_count = cards.length;

		if (card_count == 0) {
			return false;
		}

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.GZP_MAX_COUNT];

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
		int cbTempCardData[] = new int[GameConstants.MAX_FLS_COUNT];

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
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++)
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
		if (card_index < 0 || card_index >= GameConstants.GZP_MAX_INDEX) {
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
	public boolean remove_cards_by_index(int cards_index[],int cards[], int card_count) {
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

    public boolean get_remove_cards(int cards_index[],int pickup_index[],int cards[],int card_count,int cur_card)
    {
    	int temp_index[] = new int[GameConstants.GZP_MAX_INDEX];
    	for(int i = 0 ; i< GameConstants.GZP_MAX_INDEX;i++)
    	{
    		temp_index[i] = cards_index[i] - pickup_index[i];
    	}
    	int cur_index = this.switch_to_card_index(cur_card);
    	int count = 0;
    	if(cur_index == EGZPType.GZP_YI.ordinal())
    	{
//    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
//			for(int i = 0; i < temp_count; i++ )
//			{
//				cards[count++] = this.switch_to_card_data(cur_index);
//			}
			
			for(int i = 0; i< temp_index[EGZPType.GZP_YI_HUA.ordinal()];i++)
			{
				cards[count++] = this.switch_to_card_data(EGZPType.GZP_YI_HUA.ordinal());
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_YI.ordinal());
				}
			}
			return true;
    	}
    	else if(cur_index == EGZPType.GZP_YI_HUA.ordinal())
    	{
    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
			for(int i = 0; i < temp_count; i++ )
			{
				cards[count++] = this.switch_to_card_data(cur_index);
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_YI.ordinal());
				}
			}  
			return true;
    	}
    	else if(cur_index == EGZPType.GZP_SAN.ordinal())
    	{
    		for(int i = 0; i< temp_index[EGZPType.GZP_SAN_HUA.ordinal()];i++)
			{
				cards[count++] = this.switch_to_card_data(EGZPType.GZP_SAN_HUA.ordinal());
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_SAN.ordinal());
				}
			}
//    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
//			for(int i = 0; i < temp_count; i++ )
//			{
//				cards[count++] = this.switch_to_card_data(cur_index);
//			}
//			int max = card_count - count ;
//			if(max!=0)
//			{
//				for(int i = 0; i<max;i++)
//				{
//					cards[count++] = this.switch_to_card_data(EGZPType.GZP_SAN_HUA.ordinal());
//				}
//			}
			return true;

    	}
    	else if(cur_index == EGZPType.GZP_SAN_HUA.ordinal())
    	{
    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
			for(int i = 0; i < temp_count; i++ )
			{
				cards[count++] = this.switch_to_card_data(cur_index);
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_SAN.ordinal());
				}
			}
			return true;
    	}
    	else if(cur_index == EGZPType.GZP_WU.ordinal())
    	{
       		for(int i = 0; i< temp_index[EGZPType.GZP_WU_HUA.ordinal()];i++)
			{
				cards[count++] = this.switch_to_card_data(EGZPType.GZP_WU_HUA.ordinal());
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_WU.ordinal());
				}
			}
//    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
//			for(int i = 0; i < temp_count; i++ )
//			{
//				cards[count++] = this.switch_to_card_data(cur_index);
//			}
//			int max = card_count - count ;
//			if(max!=0)
//			{
//				for(int i = 0; i<max;i++)
//				{
//					cards[count++] = this.switch_to_card_data(EGZPType.GZP_WU_HUA.ordinal());
//				}
//			}
			return true;
    	}
    	else if(cur_index == EGZPType.GZP_WU_HUA.ordinal())
    	{
    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
			for(int i = 0; i < temp_count; i++ )
			{
				cards[count++] = this.switch_to_card_data(cur_index);
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_WU.ordinal());
				}
			}
			return true;

    	}
    	else if(cur_index == EGZPType.GZP_QI.ordinal())
    	{
    		for(int i = 0; i< temp_index[EGZPType.GZP_QI_HUA.ordinal()];i++)
			{
				cards[count++] = this.switch_to_card_data(EGZPType.GZP_QI_HUA.ordinal());
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_QI.ordinal());
				}
			}
//    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
//			for(int i = 0; i < temp_count; i++ )
//			{
//				cards[count++] = this.switch_to_card_data(cur_index);
//			}
//			int max = card_count - count ;
//			if(max!=0)
//			{
//				for(int i = 0; i<max;i++)
//				{
//					cards[count++] = this.switch_to_card_data(EGZPType.GZP_QI_HUA.ordinal());
//				}
//			}
			return true;

    	}
    	else if(cur_index == EGZPType.GZP_QI_HUA.ordinal())
    	{
    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
			for(int i = 0; i < temp_count; i++ )
			{
				cards[count++] = this.switch_to_card_data(cur_index);
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_QI.ordinal());
				}
			}
			return true;
    	}
    	else if(cur_index == EGZPType.GZP_JIU.ordinal())
    	{
    		for(int i = 0; i< temp_index[EGZPType.GZP_JIU_HUA.ordinal()];i++)
			{
				cards[count++] = this.switch_to_card_data(EGZPType.GZP_JIU_HUA.ordinal());
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_JIU.ordinal());
				}
			}
//    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
//			for(int i = 0; i < temp_count; i++ )
//			{
//				cards[count++] = this.switch_to_card_data(cur_index);
//			}
//			int max = card_count - count ;
//			if(max!=0)
//			{
//				for(int i = 0; i<max;i++)
//				{
//					cards[count++] = this.switch_to_card_data(EGZPType.GZP_JIU_HUA.ordinal());
//				}
//			}
			return true;

    	}
    	else if(cur_index == EGZPType.GZP_JIU_HUA.ordinal())
    	{
    		int temp_count = temp_index[cur_index]>card_count?card_count:temp_index[cur_index];
			for(int i = 0; i < temp_count; i++ )
			{
				cards[count++] = this.switch_to_card_data(cur_index);
			}
			int max = card_count - count ;
			if(max!=0)
			{
				for(int i = 0; i<max;i++)
				{
					cards[count++] = this.switch_to_card_data(EGZPType.GZP_JIU.ordinal());
				}
			}
			return true;
    	}
    	else{
    		for(int i = 0; i < card_count ; i++ )
    		{
    			cards[count++] =cur_card;
    		}
    	}

    	return true;
    }
	// 碰牌判断
	public int check_peng(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
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
	public int estimate_gang_card_out_card(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
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
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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

	public int analyse_gang_card_fls(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上有3张
		if (cards_index[this.switch_to_card_index(card)] == 3) {
			cbActionMask |= GameConstants.WIK_GANG;
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
						cbActionMask |= GameConstants.WIK_GANG;

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
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
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
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
			// if( i == get_magic_card_index() ) continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
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
					gangCardResult.isPublic[index] = 1;// 明刚
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
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
		int cbCardIndexTemp[] = new int[GameConstants.GZP_MAX_INDEX];
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
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
	 * 手牌是否满足
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_pengpeng_hu(AnalyseItem analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		boolean same = true;
		int lastCard = 0;
		int num = 0;
		
		int cbMagicCardIndex[] = new int[GameConstants.GZP_MAX_INDEX];
		
		for (int eye : analyseItem.cbCardEye) {
			if (eye == 0)
				continue;
			if (lastCard == 0) {
				lastCard = eye;
			}
			if (eye != lastCard) {
				same = false;
			}
			num++;
			
			cbMagicCardIndex[switch_to_card_index(eye)]++;
		}
		boolean flag =  analyseItem.isShuangDui || (same && num == 4) || analyseItem.eyeKind == GameConstants.WIK_PENG;// 2对子
		if(!flag) {
			for(int i:cbMagicCardIndex) {
				if(i>=3) {
					return true;
				}
			}
		}
		return flag;
	}

	/**
	 * 手牌是否满足
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_pengpeng_hu(AnalyseItemTwenty analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		return analyseItem.eyeDui;
	}

	/**
	 * 清胡 上大人 福禄寿不能成句
	 * 
	 * @param analyseItem
	 * @param weaveCount
	 * @param weaveItems
	 * @return
	 */
	public boolean is_qing_hu(AnalyseItemTwenty analyseItem, WeaveItem[] weaveItems, int weaveCount, int cur_card) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			for (int j = 0; j < 3; j++) {
				int cardValue = analyseItem.cbCardData[i][j];
				if (cardValue == 0x01 || cardValue == 0x71) {//
					return false;
				}
			}
		}
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0) {
				int cardValue = weaveItems[i].center_card;
				if (cardValue > 0) {
					int cbCardcolor = get_card_color(cardValue);
					if (cbCardcolor == 0 || cbCardcolor == 7) {//
						return false;
					}
				}
				
			}
			
			if (weaveItems[i].weave_kind == GameConstants.WIK_PENG
					|| weaveItems[i].weave_kind == GameConstants.WIK_ZHAO || weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				int cardValue = weaveItems[i].center_card;
				if (cardValue == 0x01 || cardValue == 0x71) {//
					return false;
				}
			}
		}

		if (!analyseItem.eyeDui) {
			for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
				int eyecolor = get_card_color(analyseItem.cbCardEye[i]);
				if (eyecolor == 0 || eyecolor == 7) {//
					return false;
				}
			}
		}else {
			if (cur_card == 0x01 || cur_card == 0x71) {//
				return false;
			}
			for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
				if (analyseItem.cbCardEye[i] == 0x01 || analyseItem.cbCardEye[i] == 0x71) {//
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 枯胡
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_ku_hu(AnalyseItemTwenty analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		return analyseItem.eyeDui;
	}

	/**
	 * 重胡
	 * 
	 * @param analyseItem
	 * @return
	 */
	public boolean is_zhong_hu(AnalyseItemTwenty analyseItem) {
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0)
				return false;
		}
		return analyseItem.eyeDui;
	}

	/**
	 * hong胡
	 * 
	 * @param analyseItem
	 * @param cur_card
	 * @param weaveCount
	 * @param weaveItems
	 * @return
	 */
	public boolean is_hong_hu(AnalyseItemTwenty analyseItem, WeaveItem[] weaveItems, int weaveCount, int cur_card) {
		int hong = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if (analyseItem.cbWeaveKind[i] == GameConstants.WIK_PENG
					|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_ZHAO)
				return false;
			if ((analyseItem.cbWeaveKind[i]
					& (GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT)) != 0) {
				for (int j = 0; j < 3; j++) {
					int cardValue = analyseItem.cbCardData[i][j];
					if (cardValue > 0) {
						int cbCardcolor = get_card_color(cardValue);
						if (cbCardcolor == 0 || cbCardcolor == 7) {//
							hong++;
							break;
						}
					}
				}
			}
		}
		for (int i = 0; i < weaveCount; i++) {
			for (int j = 0; j < 3; j++) {
				int cardValue = weaveItems[i].center_card;
				if (cardValue > 0) {
					int cbCardcolor = get_card_color(cardValue);
					if (cbCardcolor == 0 || cbCardcolor == 7) {//
						hong++;
						break;
					}
				}
			}
			if (weaveItems[i].weave_kind == GameConstants.WIK_PENG
					|| weaveItems[i].weave_kind == GameConstants.WIK_ZHAO) {
				return false;
			}
		}

		if (!analyseItem.eyeDui) {
			for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
				if (analyseItem.cbCardEye[i] == 0x01 || analyseItem.cbCardEye[i] == 0x71) {//
					hong++;
					break;
				}
			}

		}
		analyseItem.hong = hong;
		return !analyseItem.eyeDui && hong >= 3;
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
	public int get_gzp_count(int card)
	{
		switch(card)
		{
		case 0x21:
		case 0x29:
		case 0x03:
		case 0x07:
			return 1;
		case 0x05:
		case 0x23:
		case 0x27:
			return 2;
		case 0x25:
			return 4;
		}
		return 0;
	}
	public int  get_weave_items_gzp(WeaveItem weave_items[],int weave_count)
	{
		int hei_peng_count = 0;
		int count = 0;
		for(int i =0 ; i< weave_count; i++)
		{
			if(weave_items[i].weave_kind == GameConstants.GZP_WIK_PENG &&  is_hong(weave_items[i].center_card)==false)
			{
				hei_peng_count ++;
			}
		
		}
		for(int j = 0; j<weave_items[weave_count-1].weave_card.length;j++)
		{
			count += get_gzp_count(weave_items[weave_count-1].weave_card[j]);
		}
			
		count += get_weave_hu_xi(weave_items[weave_count-1]);
		if(weave_items[weave_count-1].weave_kind == GameConstants.GZP_WIK_PENG && hei_peng_count%3==0&&is_hong(weave_items[weave_count-1].center_card)==false)
		{
			count += hei_peng_count/3;
		}
		return count;
	}
    public void get_logic_count(int real_cards_index[],int cards_index[])
    {
    	for(int i = 0; i< GameConstants.GZP_MAX_LOGIC_INDEX;i++)
    	{
	    	switch(i)
	    	{
	    	case 0:
	    		real_cards_index[EGZPType.GZP_YI.ordinal()] =  cards_index[EGZPType.GZP_YI.ordinal()] + cards_index[EGZPType.GZP_YI_HUA.ordinal()] ;
	    		break;
	    	case 2:
	    		real_cards_index[EGZPType.GZP_SAN.ordinal()] = cards_index[EGZPType.GZP_SAN.ordinal()] + cards_index[EGZPType.GZP_SAN_HUA.ordinal()] ;
	    		break;
	    	case 4:
	    		real_cards_index[EGZPType.GZP_WU.ordinal()] = cards_index[EGZPType.GZP_WU.ordinal()] + cards_index[EGZPType.GZP_WU_HUA.ordinal()] ;
	    		break;
	    	case 6:
	    		real_cards_index[EGZPType.GZP_QI.ordinal()] = cards_index[EGZPType.GZP_QI.ordinal()] + cards_index[EGZPType.GZP_QI_HUA.ordinal()] ;
	    		break;
	    	case 8:
	    		real_cards_index[EGZPType.GZP_JIU.ordinal()] = cards_index[EGZPType.GZP_JIU.ordinal()] + cards_index[EGZPType.GZP_JIU_HUA.ordinal()] ;
	    		break;
	    	default:
	    		real_cards_index[i] = cards_index[i];
	    	}
    	}
    }
	public boolean is_hu(int cards_index[],int pickup_index[],AnalyseItem analyseItem)
	{
		boolean is_hu = false;
		if(cards_index[EGZPType.GZP_YI.ordinal()] +cards_index[EGZPType.GZP_KONG.ordinal()]+cards_index[EGZPType.GZP_JI.ordinal()] ==2)
		{
			analyseItem.curCardEye = true;
			int count = 0;
			if(cards_index[EGZPType.GZP_KONG.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_KONG.ordinal();
			if(cards_index[EGZPType.GZP_YI.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_YI.ordinal();
			if(cards_index[EGZPType.GZP_JI.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_JI.ordinal();
			if(count == 2)
				is_hu = true;
		}
		if(cards_index[EGZPType.GZP_SAN.ordinal()] +cards_index[EGZPType.GZP_HUA.ordinal()]+cards_index[EGZPType.GZP_QIAN.ordinal()] ==2)
		{
			analyseItem.curCardEye = true;
			int count = 0;
			if(cards_index[EGZPType.GZP_SAN.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_SAN.ordinal();
			if(cards_index[EGZPType.GZP_HUA.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_HUA.ordinal();
			if(cards_index[EGZPType.GZP_QIAN.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_QIAN.ordinal();
			if(count == 2)
				return true;
		}
		if(cards_index[EGZPType.GZP_QI.ordinal()] +cards_index[EGZPType.GZP_SHI.ordinal()]+cards_index[EGZPType.GZP_TU.ordinal()] ==2)
		{
			analyseItem.curCardEye = true;
			int count = 0;
			if(cards_index[EGZPType.GZP_QI.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_QI.ordinal();
			if(cards_index[EGZPType.GZP_SHI.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] =EGZPType.GZP_SHI.ordinal();
			if(cards_index[EGZPType.GZP_TU.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_TU.ordinal();
			if(count == 2)
				return true;
		}
		if(cards_index[EGZPType.GZP_BA.ordinal()] +cards_index[EGZPType.GZP_JIU.ordinal()]+cards_index[EGZPType.GZP_ZI.ordinal()] ==2)
		{
			analyseItem.curCardEye = true;
			int count = 0;
			if(cards_index[EGZPType.GZP_BA.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_BA.ordinal();
			if(cards_index[EGZPType.GZP_JIU.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_JIU.ordinal();
			if(cards_index[EGZPType.GZP_ZI.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_ZI.ordinal();
			if(count == 2)
				return true;
		}
		if(cards_index[EGZPType.GZP_SHANG.ordinal()] +cards_index[EGZPType.GZP_DA.ordinal()]+cards_index[EGZPType.GZP_REN.ordinal()] ==2)
		{
			analyseItem.curCardEye = true;
			int count = 0;
			if(cards_index[EGZPType.GZP_SHANG.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_SHANG.ordinal();
			if(cards_index[EGZPType.GZP_DA.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_DA.ordinal();
			if(cards_index[EGZPType.GZP_REN.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_REN.ordinal();
			if(count == 2)
				return true;
		}
		if(cards_index[EGZPType.GZP_KE.ordinal()] +cards_index[EGZPType.GZP_ZHI.ordinal()]+cards_index[EGZPType.GZP_LI.ordinal()] ==2)
		{
			analyseItem.curCardEye = true;
			int count = 0;
			if(cards_index[EGZPType.GZP_KE.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_KE.ordinal();
			if(cards_index[EGZPType.GZP_ZHI.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_ZHI.ordinal();
			if(cards_index[EGZPType.GZP_LI.ordinal()] == 1)
				analyseItem.cbCardIndex[count++] = EGZPType.GZP_LI.ordinal();
			if(count == 2)
				return true;
			
		}
		for(int i = 0; i<GameConstants.GZP_MAX_LOGIC_INDEX&&is_hu!=true;i++)
		{
			if(cards_index[i] == 2&&pickup_index[i] == 0)
			{
				analyseItem.curCardEye = true;
				analyseItem.cbCardIndex[0] = i;
				analyseItem.cbCardIndex[1] = i;
				return true;
			}
			else{
				
				// 顺子判断
				if (get_card_color(switch_to_card_data(i)) == 0  &&  (i <= 7)) {
					if(cards_index[i] +cards_index[i+1]+cards_index[i+2] == 2&&(!(cards_index[i] == 2||cards_index[i+1] == 2||cards_index[i+2]==2)))
					{
						analyseItem.curCardEye = true;
						int count  = 0;
						if(cards_index[i] >0)
							analyseItem.cbCardIndex[count++] = i;
						if(cards_index[i+1]>0)
							analyseItem.cbCardIndex[count++] = i+1;
						if(cards_index[i+2]>0)
							analyseItem.cbCardIndex[count++] = i+2;
						if(count == 2)
							return  true;
						break;
					}
				}
				
			}
		}
		return is_hu;
	}

	
	/**
	 * 只处理牌眼 4张的情况
	 * 
	 * @param cards_index
	 * @param cardEyes
	 * @return
	 */
	public boolean isYankou(int cards_index[],int pickup_index[], AnalyseItem analyseItem,int cur_card) {
		
		if(is_hu(cards_index,pickup_index,analyseItem) == true)
		{
			
			return true;
		}		
		// 变量定义
		return false;
	}
//	public void minus_cards_index(int cards_index[],int pickup_index[], int card_index,int count)
//	{
//		switch(card_index)
//    	{
//    	case 0:
//    		if(pickup_index[EGZPType.GZP_YI_HUA.ordinal()] == 1)
//    		{
//    			cards_index[EGZPType.GZP_YI.ordinal()] -= (4- cards_index[EGZPType.GZP_YI_HUA.ordinal()+1]);
//    			cards_index[EGZPType.GZP_YI_HUA.ordinal()] = 1;
//    		}
//    		else
//    		{
//    			cards_index[EGZPType.GZP_YI.ordinal()] -= (4- cards_index[EGZPType.GZP_YI_HUA.ordinal()]);
//    			cards_index[EGZPType.GZP_YI_HUA.ordinal()] = 0;
//    		}
//    		break;
//    	case 2:
//    		if(pickup_index[EGZPType.GZP_YI_HUA.ordinal()] == 1)
//    		{
//    			cards_index[EGZPType.GZP_SAN.ordinal()] -= (4- cards_index[EGZPType.GZP_SAN_HUA.ordinal()+1]);
//        		cards_index[EGZPType.GZP_SAN_HUA.ordinal()] = 1;
//    		}
//    		else
//    		{
//    			cards_index[EGZPType.GZP_SAN.ordinal()] -= (4- cards_index[EGZPType.GZP_SAN_HUA.ordinal()]);
//        		cards_index[EGZPType.GZP_SAN_HUA.ordinal()] = 0;
//    		}
//    		break;
//    	case 4:
//    		if(pickup_index[EGZPType.GZP_YI_HUA.ordinal()] == 1)
//    		{
//        		cards_index[EGZPType.GZP_WU.ordinal()] -= (4- cards_index[EGZPType.GZP_WU_HUA.ordinal()+1]);
//        		cards_index[EGZPType.GZP_WU_HUA.ordinal()] = 1;
//    		}
//    		else
//    		{
//        		cards_index[EGZPType.GZP_WU.ordinal()] -= (4- cards_index[EGZPType.GZP_WU_HUA.ordinal()]);
//        		cards_index[EGZPType.GZP_WU_HUA.ordinal()] = 0;
//    		}
//
//    		break;
//    	case 6:
//    		if(pickup_index[EGZPType.GZP_YI_HUA.ordinal()] == 1)
//    		{
//        		cards_index[EGZPType.GZP_QI.ordinal()] -= (4- cards_index[EGZPType.GZP_QI_HUA.ordinal()+1]);
//        		cards_index[EGZPType.GZP_QI_HUA.ordinal()] = 0;
//    		}
//    		else
//    		{
//        		cards_index[EGZPType.GZP_QI.ordinal()] -= (4- cards_index[EGZPType.GZP_QI_HUA.ordinal()]);
//        		cards_index[EGZPType.GZP_QI_HUA.ordinal()] = 1;
//    		}
//
//    		break;
//    	case 8:
//    		if(pickup_index[EGZPType.GZP_YI_HUA.ordinal()] == 1)
//    		{
//    			cards_index[EGZPType.GZP_JIU.ordinal()] -= (4- cards_index[EGZPType.GZP_JIU_HUA.ordinal()+1]);
//        		cards_index[EGZPType.GZP_JIU_HUA.ordinal()] = 1;
//    		}
//    		else
//    		{
//    			cards_index[EGZPType.GZP_JIU.ordinal()] -= (4- cards_index[EGZPType.GZP_JIU_HUA.ordinal()]);
//        		cards_index[EGZPType.GZP_JIU_HUA.ordinal()] = 0;
//    		}
//    		
//    		break;
//    	default:
//    		cards_index[card_index]-=4;
//    	}
//	}
	public boolean is_hong(int card)
	{
		int cur_index = this.switch_to_card_index(card);
		if(cur_index == EGZPType.GZP_SHANG.ordinal()||cur_index == EGZPType.GZP_DA.ordinal()
				||cur_index == EGZPType.GZP_REN.ordinal()||cur_index == EGZPType.GZP_KE.ordinal()
				||cur_index == EGZPType.GZP_ZHI.ordinal()||cur_index == EGZPType.GZP_LI.ordinal()
				||cur_index == EGZPType.GZP_SAN.ordinal()||cur_index == EGZPType.GZP_WU.ordinal()
				||cur_index == EGZPType.GZP_QI.ordinal()||cur_index == EGZPType.GZP_SAN_HUA.ordinal()
				||cur_index == EGZPType.GZP_WU_HUA.ordinal()||cur_index == EGZPType.GZP_QI_HUA.ordinal())
			return true;
		return false;
	}
	public int get_weave_hu_xi(WeaveItem weave_item) {
	
		switch (weave_item.weave_kind) {
		case GameConstants.GZP_WIK_HUA:
			return (is_hong(weave_item.center_card)) ? 5 : 3;
		case GameConstants.GZP_WIK_GUAN:
		case GameConstants.GZP_WIK_ZHAO:
			return (is_hong(weave_item.center_card)) ? 4 : 2;
		case GameConstants.GZP_WIK_KAN:
			return (is_hong(weave_item.center_card)) ? 2 : 1;
		case GameConstants.GZP_WIK_PENG:
			return (is_hong(weave_item.center_card)) ? 1 :0;
		case GameConstants.GZP_WIK_YI_JU_HUA:
		case GameConstants.GZP_WIK_LEFT:
		case GameConstants.GZP_WIK_RIGHT:
		case GameConstants.GZP_WIK_CETNER:{
			if(this.switch_to_card_index(weave_item.center_card) == EGZPType.GZP_SHANG.ordinal()
					||this.switch_to_card_index(weave_item.center_card) == EGZPType.GZP_KE.ordinal())
				return 1;
			}
			return 0;
		}
		return 0;
	}
	//分析 十对
	public int  analyse_card_ten_dui(int card_index[] ,WeaveItem weaveItem[],int real_guan_index[],int real_pickup_index[])
	{
		int count = 0;
		int temp_cards_index[] = new int[GameConstants.GZP_MAX_INDEX];
		for(int i = 0; i<GameConstants.GZP_MAX_INDEX ;i++ )
		{
			if(real_guan_index[i] != 0)
				return count;
			if(real_pickup_index[i] != 0)
				return count;
			temp_cards_index[i] = card_index[i];
		}

		for(int i = 0; i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
		{
			int flower_index = switch_to_card_flower_index(this.switch_to_card_data(i));
			if(flower_index != -1)
			{
				int pai_count = temp_cards_index[flower_index] + temp_cards_index[i];
				if(pai_count > 0&& pai_count%2==0)
				{
					int k = 0;
					for(int j = 0;j <pai_count;j++)
					{
						if(j == 2)
						{
							weaveItem[count++].weave_kind = GameConstants.GZP_WIK_DUI_ZI;
							k = 0;
						}
						weaveItem[count].weave_card[k++] = this.get_card(temp_cards_index, real_pickup_index, i);
						
					}
					weaveItem[count++].weave_kind = GameConstants.GZP_WIK_DUI_ZI;
				}
				else if(pai_count%2!=0)
					return count;
			}
			if(temp_cards_index[i]%2==0&&temp_cards_index[i]>0)
			{
				int pai_count = temp_cards_index[i];
				int k = 0;
				for(int j = 0;j <pai_count;j++)
				{
					if(j == 2)
					{
						weaveItem[count++].weave_kind = GameConstants.GZP_WIK_DUI_ZI;
						k = 0;
					}
					weaveItem[count].weave_card[k++] = this.get_card(temp_cards_index, real_pickup_index, i);
				}
				weaveItem[count++].weave_kind = GameConstants.GZP_WIK_DUI_ZI;
			}
			else if(temp_cards_index[i]%2!=0)
				return count;
			
		}

		return count;
	}
	public int get_magic_index(int card_index)
	{
		switch(card_index)
		{
		case 0:
			return EGZPType.GZP_YI_HUA.ordinal();
		case 2:
			return EGZPType.GZP_SAN_HUA.ordinal();
		case 4:
			return EGZPType.GZP_WU_HUA.ordinal();
		case 6:
			return EGZPType.GZP_QI_HUA.ordinal();
		case 8:
			return EGZPType.GZP_JIU_HUA.ordinal();
		
		}
		
		return -1;
	}
	// 分析扑克
	public boolean analyse_card_gzp(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,int guan_index[],int pickup_index[],
			List<AnalyseItem> analyseItemArray, boolean has_feng, int cur_card, int seat_index,int provider) {
		// 计算数目
	
		
		
		for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
		{
			if(guan_index[i] == 1)
			{
				int flower_index = get_magic_index(i);
				if(flower_index != -1)
				{
					
				}
				
				
				cards_index[i] -= 4;
				
			}
		}
		int cbCardCount = 0;
		// 需求判断
		int cbLessKindItem = (cbCardCount ) / 3;

		// 单吊判断
		if (cbLessKindItem <= 1) {
		
			boolean flag = false;
			
			for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				AnalyseItem analyseItem = new AnalyseItem();
				int card = this.switch_to_card_data(i);
				if(is_hu(cards_index,pickup_index,analyseItem) == true)
				{
					flag = true;
					break;
				}
		
			}
				
			// 变量定义
			if(flag == true)
			{
				AnalyseItem analyseItem = new AnalyseItem();
				analyseItem.curCardEye = true;
				int count = 0;	

				count = 0;
				// 设置结果
				for (int j = 0; j < cbWeaveCount; j++) {
					analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
					analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
					analyseItem.cbGzshu[j] = weaveItem[j].hu_xi;
					for(int i = 0; i<weaveItem[j].weave_card.length;i++)
						analyseItem.cbCardData[j][i] = weaveItem[j].weave_card[i];
					count++;
				}
				for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
				{
					if(guan_index[i] == 1)
					{
						analyseItem.cbWeaveKind[count] = GameConstants.GZP_WIK_GUAN;
						analyseItem.cbCenterCard[count] = this.switch_to_card_data(i);
						count++;
					}
				}
//				switch_hu_pai(analyseItem,cbWeaveCount,real_cards_index,real_pickup_index);
				analyseItemArray.add(analyseItem);
				return (analyseItemArray.size() > 0 ? true : false);
			}
			
		} // 单吊判断 end
		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[75];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		for (int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			if (cards_index[i]-pickup_index[i] >= 3) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				if(seat_index == provider||cards_index[i]-pickup_index[i]>=3)
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_KAN;
				else 
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_PENG;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
		
			if (cards_index[EGZPType.GZP_YI.ordinal()] > 0 && (i == EGZPType.GZP_YI.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_KONG.ordinal()]>=j)&&(cards_index[EGZPType.GZP_JI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_KONG.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_YI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_JI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_SAN.ordinal()] > 0 && (i == EGZPType.GZP_SAN.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_HUA.ordinal()]>=j)&&(cards_index[EGZPType.GZP_QIAN.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_HUA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_SAN.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_QIAN.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_QI.ordinal()] > 0 && (i == EGZPType.GZP_QI.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_SHI.ordinal()]>=j)&&(cards_index[EGZPType.GZP_TU.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_QI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_SHI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_TU.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_BA.ordinal()] > 0 && (i == EGZPType.GZP_BA.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_JIU.ordinal()]>=j)&&(cards_index[EGZPType.GZP_ZI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_BA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_JIU.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_ZI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_SHANG.ordinal()] > 0 && (i == EGZPType.GZP_SHANG.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_DA.ordinal()]>=j)&&(cards_index[EGZPType.GZP_REN.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_SHANG.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_DA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_REN.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_KE.ordinal()] > 0 && (i == EGZPType.GZP_KE.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_ZHI.ordinal()]>=j)&&(cards_index[EGZPType.GZP_LI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_KE.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_ZHI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_LI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			// 顺子判断
			if (get_card_color(switch_to_card_data(i)) == 0  && (cards_index[i] > 0) && ((i % 10) <= 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if ( cards_index[i + 1] >= j && cards_index[i + 2] >= j ) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析

	
		
		// 组合分析
//		cbLessKindItem = cbLessKindItem - 1;
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.GZP_MAX_INDEX];

			// 变量定义
			// int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5 };
			int cbIndex[] = new int[] { 0, 1, 2, 3, 4,5,6};
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.GZP_MAX_LOGIC_INDEX;
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbCardIndex[i % 3];
					if (cbCardIndexTemp[cbCardIndex] == 0) {
						if (this.magic_count(cbCardIndexTemp) > 0) {
							for (int m = 0; m < this._magic_card_count; m++) {
								if (cbCardIndexTemp[this._magic_card_index[m]] > 0) {
									pKindItem[i / 3].cbValidIndex[i % 3] = this._magic_card_index[m];
									cbCardIndexTemp[this._magic_card_index[m]]--;
									break;
								}
							}
						} else {
							bEnoughCard = false;
							break;
						}

					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {
					// 牌眼判断
					AnalyseItem analyseItem = new AnalyseItem();
					if (isYankou(cbCardIndexTemp,pickup_index, analyseItem,cur_card)) {

						int count = 0;
						// 设置结果
						for (int j = 0; j < cbWeaveCount; j++) {
							analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
							analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
							analyseItem.cbGzshu[j] = weaveItem[j].hu_xi;
							for(int i = 0; i<weaveItem[j].weave_card.length;i++)
								analyseItem.cbCardData[j][i] = weaveItem[j].weave_card[i];
							count++;
						}
						
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							count ++;
						}
						for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
						{
							if(guan_index[i] == 1)
							{
								analyseItem.cbWeaveKind[count] = GameConstants.GZP_WIK_GUAN;
								analyseItem.cbCenterCard[count] = this.switch_to_card_data(i);
								count++;
							}
						}
//						switch_hu_pai(analyseItem,cbWeaveCount,real_cards_index,real_pickup_index);
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
	public boolean analyse_card(int real_cards_index[], WeaveItem weaveItem[], int cbWeaveCount,int real_guan_index[],int real_pickup_index[],
			List<AnalyseItem> analyseItemArray, boolean has_feng, int cur_card, int seat_index,int provider) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(real_cards_index);
		int cur_index = switch_to_card_common_index(cur_card);
		if(cur_index == -1)
			cur_index = switch_to_card_index(cur_card);
		int cards_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		int guan_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		int pickup_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		for(int i = 0; i<GameConstants.GZP_MAX_LOGIC_INDEX;i++ )
		{
			switch(i)
			{
			case 0:
				cards_index[EGZPType.GZP_YI.ordinal()] = real_cards_index[EGZPType.GZP_YI.ordinal()] + real_cards_index[EGZPType.GZP_YI_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_YI.ordinal()]+real_guan_index[EGZPType.GZP_YI_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_YI.ordinal()] = real_pickup_index[EGZPType.GZP_YI.ordinal()]+real_pickup_index[EGZPType.GZP_YI_HUA.ordinal()];
				break;
			case 2:
				cards_index[EGZPType.GZP_SAN.ordinal()] = real_cards_index[EGZPType.GZP_SAN.ordinal()] + real_cards_index[EGZPType.GZP_SAN_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_SAN.ordinal()]+real_guan_index[EGZPType.GZP_SAN_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_SAN.ordinal()] = real_pickup_index[EGZPType.GZP_SAN.ordinal()]+real_pickup_index[EGZPType.GZP_SAN_HUA.ordinal()];
				break;
			case 4:
				cards_index[EGZPType.GZP_WU.ordinal()] = real_cards_index[EGZPType.GZP_WU.ordinal()] + real_cards_index[EGZPType.GZP_WU_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_WU.ordinal()]+real_guan_index[EGZPType.GZP_WU_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_WU.ordinal()] = real_pickup_index[EGZPType.GZP_WU.ordinal()]+real_pickup_index[EGZPType.GZP_WU_HUA.ordinal()];
				break;
			case 6:
				cards_index[EGZPType.GZP_QI.ordinal()] = real_cards_index[EGZPType.GZP_QI.ordinal()] + real_cards_index[EGZPType.GZP_QI_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_QI.ordinal()]+real_guan_index[EGZPType.GZP_QI_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_QI.ordinal()] = real_pickup_index[EGZPType.GZP_QI.ordinal()]+real_pickup_index[EGZPType.GZP_QI_HUA.ordinal()];
				break;
			case 8:
				cards_index[EGZPType.GZP_JIU.ordinal()] = real_cards_index[EGZPType.GZP_JIU.ordinal()] + real_cards_index[EGZPType.GZP_JIU_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_JIU.ordinal()]+real_guan_index[EGZPType.GZP_JIU_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_JIU.ordinal()] = real_pickup_index[EGZPType.GZP_JIU.ordinal()]+real_pickup_index[EGZPType.GZP_JIU_HUA.ordinal()];
				break;
			default:
				cards_index[i] =  real_cards_index[i];
				guan_index[i] = real_guan_index[i];
				pickup_index[i] = real_pickup_index[i];
			}
		}
		if(seat_index != provider&&cur_index != GameConstants.GZP_MAX_INDEX)
		{
			pickup_index[cur_index]++;
		}
		for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
		{
			if(guan_index[i] == 1)
			{
				cards_index[i] -= 4;
				cbCardCount -= 4;
			}
		}
	
		// 需求判断
		int cbLessKindItem = (cbCardCount ) / 3;

		// 单吊判断
		if (cbLessKindItem < 1) {
		
			boolean flag = false;
			AnalyseItem analyseItem = new AnalyseItem();
			for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				
				int card = this.switch_to_card_data(i);
				if(is_hu(cards_index,pickup_index,analyseItem) == true)
				{
					flag = true;
					break;
				}
		
			}
				
			// 变量定义
			if(flag == true)
			{
				analyseItem.curCardEye = true;
				int count = 0;	

				count = 0;
				// 设置结果
				for (int j = 0; j < cbWeaveCount; j++) {
					analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
					analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
					analyseItem.cbGzshu[j] = weaveItem[j].hu_xi;
					for(int i = 0; i<weaveItem[j].weave_card.length;i++)
						analyseItem.cbCardData[j][i] = weaveItem[j].weave_card[i];
					count++;
				}
				for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
				{
					if(guan_index[i] == 1)
					{
						analyseItem.cbWeaveKind[count] = GameConstants.GZP_WIK_GUAN;
						analyseItem.cbCenterCard[count] = this.switch_to_card_data(i);
						count++;
					}
				}
				switch_hu_pai(analyseItem,cbWeaveCount,real_cards_index,real_pickup_index);
				analyseItemArray.add(analyseItem);
				
			}
			return (analyseItemArray.size() > 0 ? true : false);
			
		} // 单吊判断 end
		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[75];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		for (int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			if ((cards_index[i]-pickup_index[i] >= 3)||(cards_index[i]-pickup_index[i] >= 2&& (seat_index != provider && cur_index == i))) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				if(cards_index[i]-pickup_index[i]>=3)
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_KAN;
				else if(cards_index[i]-pickup_index[i]>=2 && (seat_index != provider && cur_index == i))
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_PENG;		
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
		
			if (cards_index[EGZPType.GZP_YI.ordinal()] > 0 && (i == EGZPType.GZP_YI.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_KONG.ordinal()]>=j)&&(cards_index[EGZPType.GZP_JI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_KONG.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_YI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_JI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_SAN.ordinal()] > 0 && (i == EGZPType.GZP_SAN.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_HUA.ordinal()]>=j)&&(cards_index[EGZPType.GZP_QIAN.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_HUA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_SAN.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_QIAN.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_QI.ordinal()] > 0 && (i == EGZPType.GZP_QI.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_SHI.ordinal()]>=j)&&(cards_index[EGZPType.GZP_TU.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_QI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_SHI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_TU.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_BA.ordinal()] > 0 && (i == EGZPType.GZP_BA.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_JIU.ordinal()]>=j)&&(cards_index[EGZPType.GZP_ZI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_BA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_JIU.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_ZI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_SHANG.ordinal()] > 0 && (i == EGZPType.GZP_SHANG.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_DA.ordinal()]>=j)&&(cards_index[EGZPType.GZP_REN.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_SHANG.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_DA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_REN.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_KE.ordinal()] > 0 && (i == EGZPType.GZP_KE.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_ZHI.ordinal()]>=j)&&(cards_index[EGZPType.GZP_LI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_KE.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_ZHI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_LI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			// 顺子判断
			if (get_card_color(switch_to_card_data(i)) == 0  && (cards_index[i] > 0) && ((i % 10) <= 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if ( cards_index[i + 1] >= j && cards_index[i + 2] >= j ) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_LEFT;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析

	
		
		// 组合分析
//		cbLessKindItem = cbLessKindItem - 1;
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.GZP_MAX_INDEX];

			// 变量定义
			// int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5 };
			int cbIndex[] = new int[] { 0, 1, 2, 3, 4,5,6};
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.GZP_MAX_LOGIC_INDEX;
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
				int temp_pickup_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
				Arrays.fill(temp_pickup_index, 0);
				for(int i = 0; i<GameConstants.GZP_MAX_LOGIC_INDEX;i++ )
					temp_pickup_index[i] = pickup_index[i];
				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbCardIndex[i % 3];
					if (cbCardIndexTemp[cbCardIndex] == 0) {
						if (this.magic_count(cbCardIndexTemp) > 0) {
							for (int m = 0; m < this._magic_card_count; m++) {
								if (cbCardIndexTemp[this._magic_card_index[m]] > 0) {
									pKindItem[i / 3].cbValidIndex[i % 3] = this._magic_card_index[m];
									cbCardIndexTemp[this._magic_card_index[m]]--;
									if(temp_pickup_index[this._magic_card_index[m]]>0)
										temp_pickup_index[this._magic_card_index[m]]--;
									break;
								}
							}
						} else {
							bEnoughCard = false;
							break;
						}

					} else
					{
						cbCardIndexTemp[cbCardIndex]--;
						if(pKindItem[i / 3].cbWeaveKind  == GameConstants.GZP_WIK_PENG&&i%3==0)
						{
							if(temp_pickup_index[cbCardIndex]>0)
								temp_pickup_index[cbCardIndex]--;
						}
						if(pKindItem[i / 3].cbWeaveKind  == GameConstants.GZP_WIK_KAN
						||pKindItem[i / 3].cbWeaveKind  == GameConstants.GZP_WIK_PENG)
							continue;
						{
							if(temp_pickup_index[cbCardIndex]>0)
								temp_pickup_index[cbCardIndex]--;
						}
							
					}
					
				}

				// 胡牌判断
				if (bEnoughCard == true) {
					// 牌眼判断
					AnalyseItem analyseItem = new AnalyseItem();
					if (isYankou(cbCardIndexTemp,temp_pickup_index, analyseItem,cur_card)) {

						int count = 0;
						// 设置结果
						for (int j = 0; j < cbWeaveCount; j++) {
							analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
							analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
							analyseItem.cbGzshu[j] = weaveItem[j].hu_xi;
							for(int i = 0; i<weaveItem[j].weave_card.length;i++)
								analyseItem.cbCardData[j][i] = weaveItem[j].weave_card[i];
							count++;
						}
						
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							count ++;
						}
						for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
						{
							if(guan_index[i] == 1)
							{
								analyseItem.cbWeaveKind[count] = GameConstants.GZP_WIK_GUAN;
								analyseItem.cbCenterCard[count] = this.switch_to_card_data(i);
								count++;
							}
						}
						switch_hu_pai(analyseItem,cbWeaveCount,real_cards_index,real_pickup_index);
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
	public boolean analyse_card_ddwf(int real_cards_index[], WeaveItem weaveItem[], int cbWeaveCount,int real_guan_index[],int real_pickup_index[],
			List<AnalyseItem> analyseItemArray, boolean has_feng, int cur_card, int seat_index,int provider) {
		// 计算数目
		int cbCardCount = get_card_count_by_index(real_cards_index);
		int cur_index = switch_to_card_common_index(cur_card);
		if(cur_index == -1)
			cur_index = switch_to_card_index(cur_card);
		int cards_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		int guan_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		int pickup_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		for(int i = 0; i<GameConstants.GZP_MAX_LOGIC_INDEX;i++ )
		{
			switch(i)
			{
			case 0:
				cards_index[EGZPType.GZP_YI.ordinal()] = real_cards_index[EGZPType.GZP_YI.ordinal()] + real_cards_index[EGZPType.GZP_YI_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_YI.ordinal()]+real_guan_index[EGZPType.GZP_YI_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_YI.ordinal()] = real_pickup_index[EGZPType.GZP_YI.ordinal()]+real_pickup_index[EGZPType.GZP_YI_HUA.ordinal()];
				break;
			case 2:
				cards_index[EGZPType.GZP_SAN.ordinal()] = real_cards_index[EGZPType.GZP_SAN.ordinal()] + real_cards_index[EGZPType.GZP_SAN_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_SAN.ordinal()]+real_guan_index[EGZPType.GZP_SAN_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_SAN.ordinal()] = real_pickup_index[EGZPType.GZP_SAN.ordinal()]+real_pickup_index[EGZPType.GZP_SAN_HUA.ordinal()];
				break;
			case 4:
				cards_index[EGZPType.GZP_WU.ordinal()] = real_cards_index[EGZPType.GZP_WU.ordinal()] + real_cards_index[EGZPType.GZP_WU_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_WU.ordinal()]+real_guan_index[EGZPType.GZP_WU_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_WU.ordinal()] = real_pickup_index[EGZPType.GZP_WU.ordinal()]+real_pickup_index[EGZPType.GZP_WU_HUA.ordinal()];
				break;
			case 6:
				cards_index[EGZPType.GZP_QI.ordinal()] = real_cards_index[EGZPType.GZP_QI.ordinal()] + real_cards_index[EGZPType.GZP_QI_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_QI.ordinal()]+real_guan_index[EGZPType.GZP_QI_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_QI.ordinal()] = real_pickup_index[EGZPType.GZP_QI.ordinal()]+real_pickup_index[EGZPType.GZP_QI_HUA.ordinal()];
				break;
			case 8:
				cards_index[EGZPType.GZP_JIU.ordinal()] = real_cards_index[EGZPType.GZP_JIU.ordinal()] + real_cards_index[EGZPType.GZP_JIU_HUA.ordinal()];
				if(real_guan_index[EGZPType.GZP_JIU.ordinal()]+real_guan_index[EGZPType.GZP_JIU_HUA.ordinal()]>=1)
				{
					guan_index[i] = 1;
				}
				pickup_index[EGZPType.GZP_JIU.ordinal()] = real_pickup_index[EGZPType.GZP_JIU.ordinal()]+real_pickup_index[EGZPType.GZP_JIU_HUA.ordinal()];
				break;
			default:
				cards_index[i] =  real_cards_index[i];
				guan_index[i] = real_guan_index[i];
				pickup_index[i] = real_pickup_index[i];
			}
		}
		if(seat_index != provider&&cur_index != GameConstants.GZP_MAX_INDEX)
		{
			pickup_index[cur_index]++;
		}
		for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
		{
			if(guan_index[i] == 1)
			{
				cards_index[i] -= 4;
				cbCardCount -= 4;
			}
		}
	
		// 需求判断
		int cbLessKindItem = (cbCardCount ) / 3;

		// 单吊判断
		if (cbLessKindItem < 1) {
		
			boolean flag = false;
			AnalyseItem analyseItem = new AnalyseItem();
			for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				
				int card = this.switch_to_card_data(i);
				if(is_hu(cards_index,pickup_index,analyseItem) == true)
				{
					flag = true;
					break;
				}
		
			}
				
			// 变量定义
			if(flag == true)
			{
				analyseItem.curCardEye = true;
				int count = 0;	

				count = 0;
				// 设置结果
				for (int j = 0; j < cbWeaveCount; j++) {
					analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
					analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
					analyseItem.cbGzshu[j] = weaveItem[j].hu_xi;
					for(int i = 0; i<weaveItem[j].weave_card.length;i++)
						analyseItem.cbCardData[j][i] = weaveItem[j].weave_card[i];
					count++;
				}
				for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
				{
					if(guan_index[i] == 1)
					{
						analyseItem.cbWeaveKind[count] = GameConstants.GZP_WIK_GUAN;
						analyseItem.cbCenterCard[count] = this.switch_to_card_data(i);
						count++;
					}
				}
				switch_hu_pai(analyseItem,cbWeaveCount,real_cards_index,real_pickup_index);
				analyseItemArray.add(analyseItem);
				
			}
			return (analyseItemArray.size() > 0 ? true : false);
			
		} // 单吊判断 end
		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[75];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		for (int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			if ((cards_index[i]-pickup_index[i] >= 3&&pickup_index[i] == 0)||(cards_index[i]-pickup_index[i] >= 2&&pickup_index[i] == 1&& (seat_index != provider && cur_index == i))) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				if(cards_index[i]>=3&&pickup_index[i] == 0)
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_KAN;
				else if(cards_index[i]>=2 &&pickup_index[i] == 1 && (seat_index != provider && cur_index == i))
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_PENG;		
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				cbKindItemCount++;
			}
		
			if (cards_index[EGZPType.GZP_YI.ordinal()] > 0 && (i == EGZPType.GZP_YI.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_KONG.ordinal()]>=j)&&(cards_index[EGZPType.GZP_JI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_KONG.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_YI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_JI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_SAN.ordinal()] > 0 && (i == EGZPType.GZP_SAN.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_HUA.ordinal()]>=j)&&(cards_index[EGZPType.GZP_QIAN.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_HUA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_SAN.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_QIAN.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_QI.ordinal()] > 0 && (i == EGZPType.GZP_QI.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_SHI.ordinal()]>=j)&&(cards_index[EGZPType.GZP_TU.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_QI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_SHI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_TU.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_BA.ordinal()] > 0 && (i == EGZPType.GZP_BA.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_JIU.ordinal()]>=j)&&(cards_index[EGZPType.GZP_ZI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_BA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_JIU.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_ZI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_SHANG.ordinal()] > 0 && (i == EGZPType.GZP_SHANG.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_DA.ordinal()]>=j)&&(cards_index[EGZPType.GZP_REN.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_SHANG.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_DA.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_REN.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (cards_index[EGZPType.GZP_KE.ordinal()] > 0 && (i == EGZPType.GZP_KE.ordinal())) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if((cards_index[EGZPType.GZP_ZHI.ordinal()]>=j)&&(cards_index[EGZPType.GZP_LI.ordinal()]>=j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = EGZPType.GZP_KE.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[1] = EGZPType.GZP_ZHI.ordinal();
						kindItem[cbKindItemCount].cbCardIndex[2] = EGZPType.GZP_LI.ordinal();
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_YI_JU_HUA;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			// 顺子判断
			if (get_card_color(switch_to_card_data(i)) == 0  && (cards_index[i] > 0) && ((i % 10) <= 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if ( cards_index[i + 1] >= j && cards_index[i + 2] >= j ) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.GZP_WIK_LEFT;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析

	
		
		// 组合分析
//		cbLessKindItem = cbLessKindItem - 1;
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.GZP_MAX_INDEX];

			// 变量定义
			// int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5 };
			int cbIndex[] = new int[] { 0, 1, 2, 3, 4,5,6};
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}

			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int mj_count = GameConstants.GZP_MAX_LOGIC_INDEX;
				// 设置变量
				for (int i = 0; i < mj_count; i++) {
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
				int temp_pickup_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
				Arrays.fill(temp_pickup_index, 0);
				for(int i = 0; i<GameConstants.GZP_MAX_LOGIC_INDEX;i++ )
					temp_pickup_index[i] = pickup_index[i];
				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbCardIndex[i % 3];
					if (cbCardIndexTemp[cbCardIndex] == 0) {
						if (this.magic_count(cbCardIndexTemp) > 0) {
							for (int m = 0; m < this._magic_card_count; m++) {
								if (cbCardIndexTemp[this._magic_card_index[m]] > 0) {
									pKindItem[i / 3].cbValidIndex[i % 3] = this._magic_card_index[m];
									cbCardIndexTemp[this._magic_card_index[m]]--;
//									if(temp_pickup_index[this._magic_card_index[m]]>0)
//										temp_pickup_index[this._magic_card_index[m]]--;
									break;
								}
							}
						} else {
							bEnoughCard = false;
							break;
						}

					} else
					{
						cbCardIndexTemp[cbCardIndex]--;
//						if(temp_pickup_index[cbCardIndex]>0)
//							temp_pickup_index[cbCardIndex]--;
					}
				}

				// 胡牌判断
				if (bEnoughCard == true) {
					// 牌眼判断
					AnalyseItem analyseItem = new AnalyseItem();
					if (isYankou(cbCardIndexTemp,temp_pickup_index, analyseItem,cur_card)) {

						int count = 0;
						// 设置结果
						for (int j = 0; j < cbWeaveCount; j++) {
							analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
							analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
							analyseItem.cbGzshu[j] = weaveItem[j].hu_xi;
							for(int i = 0; i<weaveItem[j].weave_card.length;i++)
								analyseItem.cbCardData[j][i] = weaveItem[j].weave_card[i];
							count++;
						}
						
						// 设置牌型
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							count ++;
						}
						for(int i = 0; i < GameConstants.GZP_MAX_LOGIC_INDEX;i++)
						{
							if(guan_index[i] == 1)
							{
								analyseItem.cbWeaveKind[count] = GameConstants.GZP_WIK_GUAN;
								analyseItem.cbCenterCard[count] = this.switch_to_card_data(i);
								count++;
							}
						}
						switch_hu_pai(analyseItem,cbWeaveCount,real_cards_index,real_pickup_index);
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
	public int get_card(int real_cards_index[], int real_pickup_index[],int index)
	{
		int cur_index = index;
		int flower_index = this.switch_to_card_flower_index(this.switch_to_card_data(index));
		if(flower_index!= -1)
		{
			if(real_pickup_index[flower_index] != 0)
			{
				real_pickup_index[flower_index]--;
				real_cards_index[flower_index]--;
				return this.switch_to_card_data(flower_index);
			}
			if(real_cards_index[flower_index]>0)
			{
				real_cards_index[flower_index]--;
				return this.switch_to_card_data(flower_index);
			}
		}
		if(real_pickup_index[cur_index] != 0)
		{		
			real_pickup_index[cur_index]--;
			real_cards_index[cur_index]--;
			return  this.switch_to_card_data(cur_index);
		}
		if(real_cards_index[cur_index]>0)
		{
			real_cards_index[cur_index]--;
			return this.switch_to_card_data(cur_index);
		}
		return 0;
	}
	public void switch_hu_pai(AnalyseItem analyseItem,int cbWeaveCount,int real_cards_index[],int real_pickup_index[])
	{
		int temp_cards_index[] = new int[GameConstants.GZP_MAX_INDEX];
		int temp_pickup_index[] = new int[GameConstants.GZP_MAX_INDEX];
		for(int i = 0; i<GameConstants.GZP_MAX_INDEX ;i++ )
		{
			temp_cards_index[i] = real_cards_index[i];
			temp_pickup_index[i] = real_pickup_index[i];
		}
		for (int i = cbWeaveCount; i < 6; i++) {
			// 存在判断
			switch(analyseItem.cbWeaveKind[i])
			{
			case GameConstants.GZP_WIK_LEFT:
			{
				for(int index = 0; index<3;index++)
				{
					analyseItem.cbCardData[i][index] = get_card(temp_cards_index,temp_pickup_index,
							this.switch_to_card_index(analyseItem.cbCenterCard[i])+index);
				}
				break;
			}
			case GameConstants.GZP_WIK_YI_JU_HUA:
			{
				int count = 0;
				switch(analyseItem.cbCenterCard[i])
				{
				case 0x01:
				{
					temp_cards_index[EGZPType.GZP_KONG.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_KONG.ordinal());
					
					analyseItem.cbCardData[i][count++] = get_card(temp_cards_index,temp_pickup_index,
							EGZPType.GZP_YI.ordinal());
					temp_cards_index[EGZPType.GZP_JI.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_JI.ordinal());
					break;
				}
				case 0x03:
				{
					temp_cards_index[EGZPType.GZP_HUA.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_HUA.ordinal());
					analyseItem.cbCardData[i][count++] = get_card(temp_cards_index,temp_pickup_index,
							EGZPType.GZP_SAN.ordinal());
					temp_cards_index[EGZPType.GZP_QIAN.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_QIAN.ordinal());
					break;
				}	
				case 0x07:
				{
					analyseItem.cbCardData[i][count++] = get_card(temp_cards_index,temp_pickup_index,
							EGZPType.GZP_QI.ordinal());
					temp_cards_index[EGZPType.GZP_SHI.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_SHI.ordinal());
					
					temp_cards_index[EGZPType.GZP_TU.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_TU.ordinal());
					break;
				}
				case 0x08:
				{
					temp_cards_index[EGZPType.GZP_BA.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_BA.ordinal());
					analyseItem.cbCardData[i][count++] = get_card(temp_cards_index,temp_pickup_index,
							EGZPType.GZP_JIU.ordinal());
					temp_cards_index[EGZPType.GZP_ZI.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_ZI.ordinal());
					break;
				}
				case 0x11:
				{
					temp_cards_index[EGZPType.GZP_SHANG.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_SHANG.ordinal());
					temp_cards_index[EGZPType.GZP_DA.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_DA.ordinal());
					temp_cards_index[EGZPType.GZP_REN.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_REN.ordinal());
					break;
				}
				case 0x16:
				{
					temp_cards_index[EGZPType.GZP_KE.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_KE.ordinal());
					temp_cards_index[EGZPType.GZP_ZHI.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_ZHI.ordinal());
					temp_cards_index[EGZPType.GZP_LI.ordinal()]--;
					analyseItem.cbCardData[i][count++]= this.switch_to_card_data(EGZPType.GZP_LI.ordinal());
					break;
				}
				}
				break;
			}
			case GameConstants.GZP_WIK_PENG:
			case GameConstants.GZP_WIK_KAN:
			{
				for(int index = 0; index<3;index++)
				{
					analyseItem.cbCardData[i][index] = get_card(temp_cards_index,temp_pickup_index,
							this.switch_to_card_index(analyseItem.cbCenterCard[i]));
				}
				break;
			}
			case GameConstants.GZP_WIK_GUAN:
			{
				for(int index = 0; index<4;index++)
				{
					analyseItem.cbCardData[i][index] = get_card(temp_cards_index,temp_pickup_index,
							this.switch_to_card_index(analyseItem.cbCenterCard[i]));
				}
				break;
			}
			}
		}
		for(int i = 0; i< 2; i++)
			analyseItem.cbCardEye[i] =  get_card(temp_cards_index,temp_pickup_index,analyseItem.cbCardIndex[i]);
	}
	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.GZP_WIK_ZI_MO) {
			return 60;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.GZP_WIK_CHI_HU) {
			return 50;
		}

		// 招牌等级
		if (player_action == GameConstants.GZP_WIK_ZHAO) {
			return 30;
		}

		// 观生等级
		if (player_action == GameConstants.GZP_WIK_GUAN) {
			return 30;
		}

		// 滑等级
		if (player_action == GameConstants.GZP_WIK_HUA) {
			return 40;
		}

		// 碰牌等级
		if (player_action == GameConstants.GZP_WIK_PENG) {
			return 20;
		}

		// 上牌等级
		if (player_action == GameConstants.GZP_WIK_PICKUP) {
			return 10;
		}

		return 0;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int MAX_FLS_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (MAX_FLS_INDEX < index) {
				MAX_FLS_INDEX = index;
			}

		}

		return MAX_FLS_INDEX;
	}
	public int calculate_flower_count(WeaveItem weave_item[],int weave_count)
	{
		int count = 0;
		for(int i = 0; i< weave_count; i++)
		{
			for(int j = 0; j<weave_item[i].weave_card.length;j++)
			{
				count += this.get_flower_count(weave_item[i].weave_card[j]);
			}
			
				
		}
		return count;
	}
	
	public int calculate_quan_quan_count(WeaveItem weave_item[],int weave_count)
	{
		int count = 0;
		for(int i = 0; i< weave_count; i++)
		{
			for(int j = 0; j<weave_item[i].weave_card.length;j++)
			{
				count += this.get_quan_quan_count(weave_item[i].weave_card[j]);
			}
			
				
		}
		return count;
	}
	public int get_quan_quan_count(int card)
	{
		int count = 0;
		switch(card)
		{
		case 0x12:
		case 0x18:
		case 0x0a:
		case 0x21:
		case 0x29:
	    case 0x23:
		case 0x27:
		case 0x25:
			return 1;
		}
		return count;
	}
	public int get_flower_count(int card)
	{
		switch(card)
		{
		case 0x21:
		case 0x29:
			return 1;
	    case 0x23:
		case 0x27:
			return 1;
		case 0x25:
			return 1;
		}
		return 0;
	}
	

	public int get_chi_hu_action_rank_fls_twenty(ChiHuRight chiHuRight) {
		int wFanShu = 0;
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_QING_HU)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_KA_HU)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_TAI_HU)).is_empty()) {
			wFanShu += 1;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_KU_HU)).is_empty()) {
			wFanShu += 5;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_ZHONG_HU)).is_empty()) {
			wFanShu += 6;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HEI_HU)).is_empty()) {
			wFanShu += 4;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_SHI_DUI)).is_empty()) {
			wFanShu += 10;
		}

		boolean hasHong = false;
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU7)).is_empty()) {
			wFanShu += 7;
			hasHong=true;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU6)).is_empty()) {
			wFanShu += 6;
			hasHong=true;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU5)).is_empty()) {
			wFanShu += 5;
			hasHong=true;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU4)).is_empty()) {
			wFanShu += 4;
			hasHong=true;
		}else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU)).is_empty()) {
			if(!hasHong) {
				wFanShu += 3;
			}
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu += 1;
		}
		return wFanShu;
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
		case GameConstants.WIK_PENG: // 碰牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		case GameConstants.WIK_ZHAO: // 招操作
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
		case GameConstants.WIK_XIAO_CHAO_TIAN: // 杠牌操作
		case GameConstants.WIK_DA_CHAO_TIAN: {
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		default: {
			// logger.error("get_weave_card:invalid cbWeaveKind" + cbWeaveKind);
		}
		}

		return 0;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 12) && (cbColor <= 2);
	}
	/***
	 * 扑克转换--将实际数据 转换为 逻辑索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_logic_index(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants.GZP_MAX_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = 0;
		if(color <= 1)
			index = color * 10 + value - 1;
		else 
			index = value-1;
		return index;
	}
	public int switch_to_card_flower_index(int card)
	{
		switch(this.switch_to_card_index(card))
		{
		case 0:
			return EGZPType.GZP_YI_HUA.ordinal();
		case 2:
			return EGZPType.GZP_SAN_HUA.ordinal();
		case 4:
			return EGZPType.GZP_WU_HUA.ordinal();
		case 6:
			return EGZPType.GZP_QI_HUA.ordinal();
		case 8:
			return EGZPType.GZP_JIU_HUA.ordinal();
		}
		return -1;
	}
	public int switch_to_card_common_index(int card)
	{
		switch(card)
		{
		case 0x21:
			return EGZPType.GZP_YI.ordinal();
		case 0x23:
			return EGZPType.GZP_SAN.ordinal();
		case 0x25:
			return EGZPType.GZP_WU.ordinal();
		case 0x27:
			return EGZPType.GZP_QI.ordinal();
		case 0x29:
			return EGZPType.GZP_JIU.ordinal();
		}
		return -1;
	}
	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants.GZP_MAX_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = 0;
		if(color <= 1)
			index = color * 10 + value - 1;
		else 
			index = 22+(value-1)/2;
		return index;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data(int card_index) {
		if (card_index >= GameConstants.GZP_MAX_INDEX) {
			return GameConstants.GZP_MAX_INDEX;
		}
		if(card_index<10)
			return  (card_index % 10 + 1);
		else if(card_index < 22)
			return 0x10 | ((card_index-10) % 12 + 1);
		else 
			return 0x20|(2*(card_index-22)+1);
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
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
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
		if (cbCardCount == 0 || cbCardCount > GameConstants.MAX_FLS_COUNT)
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

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_ten_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int nGenCount = 0;

		// 计算单牌
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];
			// 王牌过滤

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				return GameConstants.WIK_NULL;

			if (cbCardCount == 4) {
				nGenCount++;
			}

		}
		return GameConstants.CHR_FLS_SHI_DUI;
	}

	public static void main(String[] args) {
		// 插入扑克
		GZPGameLogic logic = new GZPGameLogic();
		int index = logic.switch_to_card_index(24);

		for (int i = 0; i < GameConstants.CARD_DATA_FLS_LX.length; i++)
			System.out.println(GameConstants.CARD_DATA_FLS_LX[i]);
	}

}
