/**
 * 
 */
package com.cai.game.xpbh;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.hh.handler.yyzhz.GameConstants_YYZHZ;

//类型子项
class KindItem {
	public int cbWeaveKind;// 组合类型
	public int cbCenterCard;// 中心扑克
	public int cbCarddata[] = new int[3];// 扑克索引
	public int cbValidIndex[] = new int[3];
	public int cbShang;

	public KindItem() {

	}
}
class ShangWeaveItem{
	public int weavekind[] = new int[4]; 
	public int weaveCard[][]= new int[4][4];
	public int card_count[] = new int[4];
	public int count ;
	public int max_value;
	public int min_value;
	public ShangWeaveItem(){
		
	}
	
}
class CombineWeaveItem{
	public int combineItem[][] = new int [2][3];
	public int item_Count[] = new int [2];
	public int weave_kind[] = new int[2];
	public int combine_count;
	public int center_card;

	public CombineWeaveItem(){
		
	}
}

class LouWeaveItem {
	public int nWeaveKind; // 组合类型
	public int nLouWeaveKind[][] = new int[50][2];
	public int nCount;

	public LouWeaveItem() {

	}
}

// 吃牌信息
class ChiCardInfo {
	int cbChiKind; // 吃牌类型
	int cbCenterCard; // 中心扑克
	int cbResultCount; // 结果数目
	int cbCardData[][] = new int[3][3]; // 吃牌组合

	public ChiCardInfo() {

	}
};

// 分析结构
class tagAnalyseResult {

	int cbMulCount; // 多张数目总和
	int cbFourCount; // 四张数目
	int cbThreeCount; // 三张数目
	int cbDoubleCount; // 两张数目
	int cbSingleCount; // 单张数目
	int cbMulCardData[] = new int[GameConstants.XPBH_MAX_COUNT];
	int cbFourCardData[] = new int[GameConstants.XPBH_MAX_COUNT]; // 四张扑克
	int cbThreeCardData[] = new int[GameConstants.XPBH_MAX_COUNT]; // 三张扑克
	int cbDoubleCardData[] = new int[GameConstants.XPBH_MAX_COUNT]; // 两张扑克
	int cbSingleCardData[] = new int[GameConstants.XPBH_MAX_COUNT]; // 单张扑克

	public tagAnalyseResult() {
		cbFourCount = 0;
		cbThreeCount = 0;
		cbDoubleCount = 0;
		cbMulCount = 0;
		Arrays.fill(cbMulCardData, 0);
		Arrays.fill(cbFourCardData, 0);
		Arrays.fill(cbThreeCardData, 0);
		Arrays.fill(cbDoubleCardData, 0);

	}

	public void Reset() {

		cbFourCount = 0;
		cbThreeCount = 0;
		cbDoubleCount = 0;
		Arrays.fill(cbMulCardData, 0);
		Arrays.fill(cbFourCardData, 0);
		Arrays.fill(cbThreeCardData, 0);
		Arrays.fill(cbDoubleCardData, 0);
	}
};

//// 胡牌信息
// class HuCardInfo{
// public int card_eye;
// public int hu_xi_count;
// public int weave_count;
// public WeaveItem weave_item[] = new WeaveItem[10];
// }

public class XPBHGameLogic {

	private static Logger logger = Logger.getLogger(XPBHGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	private int _lai_gen;
	private int _ding_gui;

	private Object[] weave_item;

	public XPBHGameLogic() {
		_magic_card_count = 0;
		_magic_card_index = new int[GameConstants.XPBH_MAX_COUNT];
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
		return _magic_card_index[index];// MJGameConstants.XPBH_MAX_INDEX;
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
		int cbTempCardData[] = new int[GameConstants.XPBH_MAX_COUNT];

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
		int cbTempCardData[] = new int[GameConstants.XPBH_MAX_COUNT];

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
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}

	// 牌数数目岳阳捉红字
	public int get_card_count_by_index_yyzhz(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++)
			card_count += cards_index[i];

		return card_count;
	}

	public int get_card_count_by_index_yzchz(int cards_index[]) {
		// 数目统计
		int card_count = 0;
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++)
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
		if (card_index < 0 || card_index >= GameConstants.XPBH_MAX_INDEX) {
			return false;
		}

		if (cards_index[card_index] == 0) {
			return false;
		}

		// 删除扑克
		cards_index[card_index]--;
		return true;
	}

	/***
	 * 删除扑克 索引(岳阳捉红字)
	 * 
	 * @param cards_index
	 * @param card
	 * @return
	 */
	public boolean remove_card_by_index_yyzhz(int cards_index[], int card) {
		// 效验扑克
		int card_index = switch_to_card_index(card);
		if (card_index < 0 || card_index >= GameConstants_YYZHZ.MAX_YYZHZ_INDEX) {
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
					cards_index[switch_to_card_index(cards[j])]++;
				}
				return false;
			}
		}

		return true;
	}

	public boolean get_card(int cur_card, int cards[], int cards_count) {
		for (int i = 0; i < cards_count; i++) {
			if (cur_card == cards[i])
				return true;
		}
		return false;
	}

	public int get_card_action(int weave_card[], int weave_count, int hu_card) {
		int card = 0;
		int hu_card_index = 0;
		int action = 0;
		for (int i = 0; i < weave_count - 1; i++) {
			for (int j = i + 1; j < weave_count; j++) {
				if (weave_card[i] > weave_card[j]) {
					card = weave_card[i];
					weave_card[i] = weave_card[j];
					weave_card[j] = card;
				}

			}
		}
		if (hu_card != 0) {
			for (int i = 0; i < weave_count; i++) {
				if (weave_card[i] == hu_card)
					hu_card_index++;
			}
			if (hu_card_index == weave_count && hu_card_index == 3) {
				return GameConstants.WIK_BH_PENG;
			} else if (hu_card_index == 3) {
				action = GameConstants.WIK_BH_PENG;
			}
		}
		int yao_card_count = 0;
		for (int i = 0; i < weave_count; i++) {
			if (this.is_yao_card(weave_card[i]))
				yao_card_count++;
		}
		if (yao_card_count == weave_count) {
			return GameConstants.WIK_BH_LUO_YAO;
		}
		if (weave_card[weave_count - 1] - weave_card[0] + 1 == weave_count) {
			card = weave_card[0];
			if (this.get_card_value(weave_card[0]) == 10)
				return GameConstants.WIK_BH_NULL;
			for (int i = 1; i < weave_count; i++) {
				if (card + 1 != weave_card[i])
					return GameConstants.WIK_BH_NULL;
				if (this.get_card_value(weave_card[i]) == 10)
					return GameConstants.WIK_BH_NULL;
				card = weave_card[i];
			}
			return action | GameConstants.WIK_BH_CHI_L;
		}
		if (weave_count != 3)
			return GameConstants.WIK_BH_NULL;
		int weave_kind[] = new int[6];
		this.special_kind(weave_kind);
		for (int i = 0; i < 6; i++) {
			int cards[] = new int[3];
			get_special_card(weave_kind[i], cards);
			int j = 0;
			for (; j < weave_count; j++)
				if (cards[j] != weave_card[j])
					break;
			if (j == weave_count)
				return action | weave_kind[i];
		}
		int value = this.get_card_value(card);
		int index = 0;
		for (; index < weave_count; index++) {
			if (value != this.get_card_value(weave_card[index]) && this.get_card_color(weave_card[index]) != index)
				break;
		}
		if (index == weave_count)
			return action | GameConstants.WIK_BH_CHI_H;
		return GameConstants.WIK_BH_NULL;
	}

	public int get_special_kind(int cards[], int count, int you_card) {
		int weave_card[] = new int[3];
		int weave_kind[] = new int[6];
		int weave_kind_count = special_kind(weave_kind);
		for (int i = 0; i < weave_kind_count; i++) {
			int weave_count = get_special_card(weave_kind[i], weave_card);
			boolean is_flag = false;
			int is_special_card = 0;
			for (int j = 0; j < weave_count; j++) {

				for (int k = 0; k < count; k++) {
					if (cards[k] == weave_card[j] && you_card != weave_card[j]) {
						is_special_card++;
						break;
					}
				}
				if (you_card == weave_card[j])
					is_flag = true;

			}
			if (is_special_card >= 2 && is_flag == true)
				return weave_kind[i];
			else if (is_flag == true) {
				return 0;
			}

		}
		boolean is_flag = false;
		int is_special_card = 0;
		int value = this.get_card_value(you_card);
		for (int i = 0; i < 3; i++) {
			if (value == 1 || value == 10)
				return 0;
			weave_kind[i] = value + i * 10;
		}
		for (int j = 0; j < 3; j++) {

			for (int k = 0; k < count; k++) {
				if (cards[k] == weave_card[j]) {
					is_special_card++;
					break;
				}
			}
			if (you_card == weave_card[j])
				is_flag = true;
		}
		if (is_special_card >= 2 && is_flag == true)
			return GameConstants.WIK_BH_CHI_H;
		else if (is_flag == true) {
			return 0;
		}

		return -1;
	}

	public int get_must_kind_card(int kind, int card_first, int card_second,int cbRemoveCard[]) {
		int count = 0;
		int cards[] = new int[3];
		int temp_card[] = new int[3];
		Arrays.fill(temp_card, 0);
		if (card_first < card_second) {
			temp_card[0] = card_first;
			temp_card[1] = card_second;
		} else {
			temp_card[0] = card_second;
			temp_card[1] = card_first;
		}	//位置不能修改
		switch (kind) {
		case GameConstants.WIK_BH_CHI_A99: {
			cards[count++] = 0x09;
			cards[count++] = 0x0a;
			cards[count++] = 0x19;
			break;

		}
		case GameConstants.WIK_BH_CHI_A98: {
			cards[count++] = 0x09;
			cards[count++] = 0x18;
			cards[count++] = 0x2a;

			break;
		}
		case GameConstants.WIK_BH_CHI_AA9: {
			cards[count++] = 0x0a;
			cards[count++] = 0x19;
			cards[count++] = 0x1a;
			break;

		}
		case GameConstants.WIK_BH_CHI_119: {
			cards[count++] = 0x01;
			cards[count++] = 0x11;
			cards[count++] = 0x29;
			break;

		}
		case GameConstants.WIK_BH_CHI_337: {
			cards[count++] = 0x03;
			cards[count++] = 0x13;
			cards[count++] = 0x27;
			break;

		}
		case GameConstants.WIK_BH_CHI_228: {
			cards[count++] = 0x02;
			cards[count++] = 0x12;
			cards[count++] = 0x28;
			break;

		}
		case GameConstants.WIK_BH_CHI_H: {
			cards[count++] = get_card_value(card_first);
			cards[count++] = get_card_value(card_first) | 0x10;
			cards[count++] = get_card_value(card_first) | 0x20;
			break;
		}
		}
		int remove_count = 0;
		for(int i = 0; i< count; i++)
		{
			boolean flag = false;
			for(int j = 0; j<2;j++)
			{
				if(cards[i] == temp_card[j]){
					flag = true;
					break;
				}
			}
			
			if(flag == false)
				cbRemoveCard[remove_count++] = cards[i];
	
			
		}
		return remove_count;
	}

	public int get_special_card(int kind, int cards[]) {
		int count = 0;
		switch (kind) {
		case GameConstants.WIK_BH_CHI_A98: {
			cards[count++] = 0x09;
			cards[count++] = 0x18;
			cards[count++] = 0x2a;
			break;
		}
		case GameConstants.WIK_BH_CHI_A99: {
			cards[count++] = 0x09;
			cards[count++] = 0x0a;
			cards[count++] = 0x19;
			break;

		}
		case GameConstants.WIK_BH_CHI_AA9: {
			cards[count++] = 0x0a;
			cards[count++] = 0x19;
			cards[count++] = 0x1a;
			break;

		}
		case GameConstants.WIK_BH_CHI_119: {
			cards[count++] = 0x01;
			cards[count++] = 0x11;
			cards[count++] = 0x29;
			break;

		}
		case GameConstants.WIK_BH_CHI_337: {
			cards[count++] = 0x03;
			cards[count++] = 0x13;
			cards[count++] = 0x27;
			break;

		}
		case GameConstants.WIK_BH_CHI_228: {
			cards[count++] = 0x02;
			cards[count++] = 0x12;
			cards[count++] = 0x28;
			break;

		}
		}
		return count;
	}

	public int special_kind(int weave_kind[]) {
		int count = 0;
		weave_kind[count++] = GameConstants.WIK_BH_CHI_A98;
		weave_kind[count++] = GameConstants.WIK_BH_CHI_A99;
		weave_kind[count++] = GameConstants.WIK_BH_CHI_AA9;
		weave_kind[count++] = GameConstants.WIK_BH_CHI_119;
		weave_kind[count++] = GameConstants.WIK_BH_CHI_337;
		weave_kind[count++] = GameConstants.WIK_BH_CHI_228;
		return count;
	}

	public int check_shang(int cards_index[], WeaveItem weave_items[], int weave_count, int cur_card, int type_count[], int type_eat_count[]) {
		int eat_type = 0;
		int count = 0;
		int chi_type = GameConstants.WIK_BH_CHI_L | GameConstants.WIK_BH_CHI_C | GameConstants.WIK_BH_CHI_R;
		int special_chi = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9 | GameConstants.WIK_BH_CHI_119
				| GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_337;
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		int value_index = cur_card_index % 10;
		int chi_kind = GameConstants.WIK_BH_NULL;
		for (int j = 0; j < weave_count; j++) {
			int min_card = 0xFF;
			int max_card = 0;
			int bb_count = 0;
			chi_kind = weave_items[j].weave_kind & chi_type;
			if (chi_kind != 0) {
				for (int k = 0; k < weave_items[j].weave_card_count; k++) {
					if (weave_items[j].weave_card[k] > max_card)
						max_card = weave_items[j].weave_card[k];
					if (weave_items[j].weave_card[k] < min_card)
						min_card = weave_items[j].weave_card[k];

				}

				if(cur_card + 1 == min_card){
					eat_type |= GameConstants.WIK_BH_XIA; 
					type_count[count++]  = GameConstants.WIK_BH_XIA + j+1;
				}
				if(cur_card - 1 == max_card)
				{
					eat_type |= GameConstants.WIK_BH_SHANG; 
					type_count[count++]  = GameConstants.WIK_BH_SHANG+ j+1;
					
				}
				if(is_yao_card(cur_card) == false){
					if(cur_card+2 == min_card&&cards_index[cur_card_index+1] >=1){
						eat_type |= GameConstants.WIK_BH_XIA_TWO; 
						type_count[count++]  = GameConstants.WIK_BH_XIA_TWO + j+1;
					}
					if(cur_card-2 == max_card&&cards_index[cur_card_index-1] >=1){
						eat_type |= GameConstants.WIK_BH_SHANG_TWO; 
						type_count[count++]  = GameConstants.WIK_BH_SHANG_TWO + j+1;
					}
				}
			}
			int excursion[] = { 0, 10, 20 };
			chi_kind = GameConstants.WIK_BH_CHI_H & weave_items[j].weave_kind;
			if (chi_kind != 0) {
				bb_count = 0;
				for (int i = 0; i < weave_items[j].weave_card_count; i++) {
					int index = this.switch_to_card_index(weave_items[j].weave_card[i] & 0xff);
					if (index == cur_card_index) {
						bb_count++;
						break;
					}
				}
				if(bb_count == 1)
				{
					eat_type |= GameConstants.WIK_BH_SHANG;
					type_count[count++]  = GameConstants.WIK_BH_SHANG+ j+1;
				}			

			}
			chi_kind = weave_items[j].weave_kind & special_chi;
			if (chi_kind != 0) {
				bb_count = 0;
				for (int i = 0; i < weave_items[j].weave_card_count; i++) {
					int index = this.switch_to_card_index(weave_items[j].weave_card[i] & 0xff);
					if (index == cur_card_index) {
						bb_count++;
						break;
					}
				}
				if(bb_count == 1)
				{
					eat_type |= GameConstants.WIK_BH_SHANG;
					type_count[count++]  = GameConstants.WIK_BH_SHANG+ j+1;
				}				

			}

		}

		type_eat_count[0] = count;
		return eat_type;
	}

	public int check_chi(int cards_index[], WeaveItem weave_items[], int weave_count, int cur_card, int type_count[],int weave_card[], int type_eat_count[]) {
		int eat_type = 0;
		int count = 0;
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		int value_index = cur_card_index % 10;
		// 三牌判断
		if (cards_index[cur_card_index] >= 3)
			return eat_type;
		// 顺子吃

		int excursion[] = { 0, 1, 2 };

		
		int bb_count = 0;
		excursion[0] = 0;
		excursion[1] = 10;
		excursion[2] = 20;
		boolean flag = false;
		int same_count = 0;
		int weave_card_index = -1;
		int same_type = GameConstants.WIK_BH_PENG | GameConstants.WIK_BH_SHE | GameConstants.WIK_BH_KAIZ | GameConstants.WIK_BH_DAGUN
				| GameConstants.WIK_BH_ZHUA_LONG;
		int temp_weave_card = 0;
		for (int i = 0; i < excursion.length; i++) {
			flag = false;
			if (value_index + excursion[i] == 10)
				break;
			if (value_index + excursion[i] == 9)
				break;

			if (cards_index[value_index + excursion[i]] < 0)
				continue;
			
			
			for (int k = 0; k < weave_count; k++) {
				if (weave_items[k].weave_kind - (weave_items[k].weave_kind & same_type) != 0 )
					continue;
				if (switch_to_card_index(weave_items[k].center_card) == excursion[i] + value_index) {
					flag = true;
					if(weave_card_index != -1)
						weave_card_index = -1;
					else
						weave_card_index = k;
					same_count ++;
				}
			}
			if(value_index + excursion[i] == cur_card_index &&flag == true )
				temp_weave_card= cur_card;
			if(value_index + excursion[i] == cur_card_index&&flag == false)
			{
				flag = true;
				continue;
			}
			if (cards_index[value_index + excursion[i]] > 0||flag == true)
				flag = true;
			else{
				flag = false;
				break;
			}
				

		}
	    if( flag == true) {
	   
	    	weave_card[count] = temp_weave_card;
	    	if(weave_card_index != -1&&same_count==1)
	    	{
	    		eat_type |= (GameConstants.WIK_BH_CHI_H+(weave_card_index+1)); 
				type_count[count++]  = (GameConstants.WIK_BH_CHI_H+(weave_card_index+1));
	    	}
	    	else{
	    		eat_type |= GameConstants.WIK_BH_CHI_H; 
				type_count[count++]  = GameConstants.WIK_BH_CHI_H;
	    	}
			
		}
		int weave_kind[] = new int[6];
		int special_kind_count = this.special_kind(weave_kind);
		boolean is_kind = false;
		for (int i = 0; i < special_kind_count; i++) {
			same_count = 0;
			int weave_cards[] = new int[3];
			int weave_cards_count = this.get_special_card(weave_kind[i], weave_cards);
			for (int j = 0; j < weave_cards_count; j++) {
				excursion[j] = this.switch_to_card_index(weave_cards[j]);
			}
			weave_card_index = -1;
			flag = false;
			is_kind = false;
			temp_weave_card = 0;
			for (int j = 0; j < excursion.length; j++) {
				flag = false;			
				if (cards_index[excursion[j]] < 0)
					continue;
				for (int k = 0; k < weave_count; k++) {
					if (weave_items[k].weave_kind - (weave_items[k].weave_kind & same_type) != 0 )
						continue;
					if (switch_to_card_index(weave_items[k].center_card) == excursion[j]) {
						flag = true;
						if(weave_card_index != -1)
							weave_card_index = -1;
						else
							weave_card_index = k;
						same_count ++;
					}
				}
				if(excursion[j]  == cur_card_index)
				{
					
					is_kind = true;
				}
				if(excursion[j] == cur_card_index &&flag == true )
					temp_weave_card = cur_card;
				if(excursion[j] == cur_card_index&&flag == false)
				{
					flag = true;
					continue;
				}
				if (cards_index[excursion[j]] > 0||flag == true)
				{	
					flag = true;
				}
				else {
					flag = false;
					break;
				}

			}
		 if( flag == true&&is_kind == true) {
			 weave_card[count] = temp_weave_card;
			 	if(weave_card_index != -1&&same_count == 1)
			 	{
			 		eat_type |= (weave_kind[i]+(weave_card_index+1)); 
					type_count[count++]  = (weave_kind[i]+(weave_card_index+1));
			 	}
			 	else{
					eat_type |= weave_kind[i]; 
					type_count[count++]  = weave_kind[i];
			 	}
			}
		}
		excursion[0] = 0;
		excursion[1] = 1;
		excursion[2] = 2;
		for (int i = 0; i < excursion.length; i++) {

			if ((value_index >= excursion[i]) && (value_index - excursion[i] < 7)) {
				int first_index = cur_card_index - excursion[i];
				if ((cur_card_index != first_index)
						&& ((cards_index[first_index] == 0) || (cards_index[first_index] == 3) || (cards_index[first_index] == 4)))
					continue;
				if ((cur_card_index != first_index + 1)
						&& ((cards_index[first_index + 1] == 0) || (cards_index[first_index + 1] == 3) || (cards_index[first_index + 1] == 4)))
					continue;
				if ((cur_card_index != first_index + 2)
						&& ((cards_index[first_index + 2] == 0) || (cards_index[first_index + 2] == 3) || (cards_index[first_index + 2] == 4)))
					continue;

				int chi_kind[] = { GameConstants.WIK_BH_CHI_L, GameConstants.WIK_BH_CHI_C, GameConstants.WIK_BH_CHI_R };
				eat_type |= chi_kind[i];
				type_count[count++] = chi_kind[i];
			}

		}

		type_eat_count[0] = count;
		return eat_type;

	}

	public int get_kind_card(int kind, int cur_card, int cards[]) {
		int count = 0;
		switch (kind) {
		case GameConstants.WIK_BH_CHI_L: {
			cards[count++] = cur_card;
			cards[count++] = cur_card + 1;
			cards[count++] = cur_card + 2;
			break;
		}
		case GameConstants.WIK_BH_CHI_C: {
			cards[count++] = cur_card - 1;
			cards[count++] = cur_card;
			cards[count++] = cur_card + 1;
			break;
		}
		case GameConstants.WIK_BH_CHI_R: {
			cards[count++] = cur_card - 2;
			cards[count++] = cur_card - 1;
			cards[count++] = cur_card;
			break;
		}
		case GameConstants.WIK_BH_CHI_H: {
			int value = this.get_card_value(cur_card);
			cards[count++] = value | 0x00;
			cards[count++] = value | 0x10;
			cards[count++] = value | 0x20;
			break;
		}
		case GameConstants.WIK_BH_CHI_A98: {
			cards[count++] = 0x2a;
			cards[count++] = 0x09;
			cards[count++] = 0x18;
			break;
		}
		case GameConstants.WIK_BH_CHI_A99: {
			cards[count++] = 0x0a;
			cards[count++] = 0x09;
			cards[count++] = 0x19;
			break;
		}
		case GameConstants.WIK_BH_CHI_AA9: {
			cards[count++] = 0x0a;
			cards[count++] = 0x1a;
			cards[count++] = 0x19;
			break;
		}
		case GameConstants.WIK_BH_CHI_119: {
			cards[count++] = 0x01;
			cards[count++] = 0x11;
			cards[count++] = 0x29;
			break;
		}
		case GameConstants.WIK_BH_CHI_337: {
			cards[count++] = 0x03;
			cards[count++] = 0x13;
			cards[count++] = 0x27;
			break;
		}
		case GameConstants.WIK_BH_CHI_228: {
			cards[count++] = 0x02;
			cards[count++] = 0x12;
			cards[count++] = 0x28;
			break;
		}
		case GameConstants.WIK_BH_PENG: {
			cards[count++] = cur_card;
			cards[count++] = cur_card;
			cards[count++] = cur_card;
			break;
		}
		case GameConstants.WIK_BH_SHE: {
			cards[count++] = cur_card;
			cards[count++] = cur_card;
			cards[count++] = cur_card;
			break;
		}
		case GameConstants.WIK_BH_KAIZ:
		case GameConstants.WIK_BH_ZHUA_LONG:
		case GameConstants.WIK_BH_DAGUN: {
			cards[count++] = cur_card;
			cards[count++] = cur_card;
			cards[count++] = cur_card;
			cards[count++] = cur_card;
			break;
		}
		}
		return count;
	}

	public boolean check_lou_weave(int cards_index[], WeaveItem weave_items[], int weave_count, int cur_card, int action,
			LouWeaveItem lou_weave_item[], int sub_lou_index,int chi_type) {
		boolean bAction = false;
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return bAction;
		}
		int cur_card_index = switch_to_card_index(cur_card);// 当前牌索引
		int temp_cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			temp_cards_index[i] = cards_index[i];
		}
		lou_weave_item[sub_lou_index].nWeaveKind = action;

		int lou_index = 0;
		int temp_type_count[] = new int[10];
		int weave_card[] = new int[10];
		int temp_eat_count[] = new int[1];
		int shang_type_count[] = new int[10];
		int shang_eat_count[] = new int[1];
		check_chi(temp_cards_index, weave_items, weave_count, cur_card, temp_type_count,weave_card, temp_eat_count);
		check_shang(temp_cards_index, weave_items, weave_count, cur_card, shang_type_count, shang_eat_count);
		int type = GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|
				GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119|
				GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228|
				GameConstants.WIK_BH_CHI_H;
		for (int i = 0; i < temp_eat_count[0]; i++) {
			if((type&temp_type_count[i])!=0&&temp_type_count[i] == chi_type )
			{
				continue;
			}
			lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = temp_type_count[i];
			bAction = true;
		}
		for (int i = 0; i < shang_eat_count[0]; i++) {
			lou_weave_item[sub_lou_index].nLouWeaveKind[lou_index++][0] = shang_type_count[i];
			bAction = true;
		}

		lou_weave_item[sub_lou_index].nCount = lou_index;
		return bAction;
	}

	// 碰牌判断
	public int check_peng(int card_index[], WeaveItem weave_items[], int weave_count, int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		int type = GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|
				GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119|
				GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228|
				GameConstants.WIK_BH_CHI_H;
		if(card_index[switch_to_card_index(cur_card)] == 2)
			return  GameConstants.WIK_BH_PENG;
		if(card_index[switch_to_card_index(cur_card)] == 1)
		{
			for(int i = 0; i< weave_count;i++){
				if((weave_items[i].weave_kind & type)!= 0){
					for(int j = 0 ;j < weave_items[i].weave_card.length;j++ ){
						if(weave_items[i].weave_card[j] == cur_card){
							return GameConstants.WIK_BH_PENG+i+1;
						}
					}
				}
			}
		}
		// 碰牌判断
		return GameConstants.WIK_NULL;
	}

	public boolean is_yao_card(int card) {
		switch (card) {
		case 0x01:
		case 0x11:
		case 0x21:
		case 0x0a:
		case 0x1a:
		case 0x2a:
			return true;
		}
		return false;
	}

	// 碰牌判断
	public int check_she(int card_index[], WeaveItem weave_items[], int weave_count, int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		int type = GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|
				GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119|
				GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228|
				GameConstants.WIK_BH_CHI_H;
		if(card_index[switch_to_card_index(cur_card)] == 2)
			return  GameConstants.WIK_BH_SHE;
		if(card_index[switch_to_card_index(cur_card)] == 1)
		{
			for(int i = 0; i< weave_count;i++){
				if((weave_items[i].weave_kind & type)!= 0){
					for(int j = 0 ;j < weave_items[i].weave_card_count;j++ ){
						if(weave_items[i].weave_card[j] == cur_card){
							return  GameConstants.WIK_BH_SHE+i+1;

						}
					}
				}
			}
		}
		// 碰牌判断
		return GameConstants.WIK_NULL;
	}

	// 碰牌判断
	public int check_peng_wmq(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
	}

	// 跑牌判断
	public int check_pao(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 3) ? GameConstants.WIK_PAO : GameConstants.WIK_NULL;
	}

	public int check_sao(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] == 2) ? GameConstants.WIK_SAO : GameConstants.WIK_NULL;
	}

	public int check_wei_wmq(int card_index[], int cur_card) {
		// 参数效验
		if (is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_WEI : GameConstants.WIK_NULL;
	}

	public boolean yao_card(int card) {
		switch (card) {
		case 0x01:
		case 0x11:
		case 0x21:
		case 0x0a:
		case 0x1a:
		case 0x2a:
			return true;
		}
		return false;
	}

	public int get_shang_count(int weave_kind, boolean is_yao) {
		switch (weave_kind) {
		case GameConstants.WIK_BH_PENG: {
			if (is_yao == true)
				return 4;
			else
				return 1;
		}
		case GameConstants.WIK_BH_SHE: {
			if (is_yao == true)
				return 5;
			else
				return 2;
		}
		case GameConstants.WIK_BH_DAGUN: {
			if (is_yao == true)
				return 13;
			else
				return 9;
		}
		case GameConstants.WIK_BH_KAIZ: {
			if (is_yao == true)
				return 14;
			else
				return 10;
		}
		case GameConstants.WIK_BH_ZHUA_LONG: {
			if (is_yao == true)
				return 16;
			else
				return 12;
		}
		}
		return 0;
	}

	public int get_weave_hu_xi(WeaveItem weave_item[], int cur_weaveindex, int weave_count) {
		int card_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(card_index, 0);
		int type = GameConstants.WIK_BH_PENG | GameConstants.WIK_BH_SHE | GameConstants.WIK_BH_ZHUA_LONG | GameConstants.WIK_BH_KAIZ
				| GameConstants.WIK_BH_DAGUN;
		int special_type = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9 | GameConstants.WIK_BH_CHI_119
				| GameConstants.WIK_BH_CHI_337 | GameConstants.WIK_BH_CHI_228;
		int eat_type = GameConstants.WIK_BH_CHI_L | GameConstants.WIK_BH_CHI_C | GameConstants.WIK_BH_CHI_R;
		int count = 0;
		int yao_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		for(int i = 0; i< weave_count;i++)
		{
			if ((weave_item[i].weave_kind & (type)) != 0&&(weave_item[i].weave_kind & (special_type|GameConstants.WIK_BH_CHI_H)) == 0) {
				for (int j = 0; j < weave_item[i].weave_card_count; j++) {
					if ((weave_item[i].weave_card[j] & (type)) != 0) {
						if (yao_index[switch_to_card_index(weave_item[i].weave_card[j] & 0xFF)] == 0) {
							yao_index[switch_to_card_index(weave_item[i].weave_card[j] & 0xFF)] = 1;
						}
					}

				}
			}
			
		}
		if(weave_item[cur_weaveindex].weave_kind == GameConstants.WIK_BH_LUO_YAO)
		{
			weave_item[cur_weaveindex].hu_xi = weave_item[cur_weaveindex].weave_card_count;
			return weave_item[cur_weaveindex].hu_xi;
		}
		for (int i = 0; i < weave_count; i++) {
			if(weave_item[i].weave_kind == GameConstants.WIK_BH_LUO_YAO)
				continue;
			boolean is_yao_card = false;
			int hu_xi = 0;
			if ((weave_item[i].weave_kind & special_type) != 0) {
				is_yao_card = true;
			}
			int weave_card_index[] = new int[GameConstants.XPBH_MAX_INDEX];
			Arrays.fill(weave_card_index, 0);
			for (int j = 0; j < weave_item[i].weave_card_count; j++) {
				if ((weave_item[i].weave_card[j] & type) != 0) {
					if (card_index[switch_to_card_index(weave_item[i].weave_card[j] & 0xFF)] == 0) {
						card_index[switch_to_card_index(weave_item[i].weave_card[j] & 0xFF)] = 1;
						count++;
						if (is_yao_card == false)
							is_yao_card = yao_card(weave_item[i].weave_card[j] & 0xFF);
						if(is_yao_card == false){
							int weave_kind[] = new int[3];
							int weave_kind_count = get_card_to_kind(weave_item[i].weave_card[j]&0xff,weave_kind);
							for(int kind_index = 0; kind_index< weave_kind_count;kind_index++){
								if(weave_kind[kind_index] == GameConstants.WIK_BH_CHI_H)
									continue;
								int weave_card[] = new int[3];
								int weave_card_count = this.get_special_card(weave_kind[kind_index], weave_card);
								int yao_count = 0;
								for(yao_count = 0;yao_count<weave_card_count;yao_count++){
									if(yao_index[this.switch_to_card_index(weave_card[yao_count])]==0)
										break;
								}
								if(yao_count == weave_card_count){
									is_yao_card = true;
									break;
								}
							}
						}
						
						hu_xi += get_shang_count(weave_item[i].weave_card[j] - (weave_item[i].weave_card[j] & 0xFF), is_yao_card);
						if((weave_item[i].weave_kind&special_type)!=0){
							hu_xi -=1;
						}
						if (count >= 4)
							hu_xi += 7;
					}
				} else if (is_yao_card == true) {
					hu_xi++;
					if (weave_card_index[switch_to_card_index(weave_item[i].weave_card[j] & 0xFF)] == 0)
						weave_card_index[switch_to_card_index(weave_item[i].weave_card[j] & 0xFF)]++;
				}
			}
			weave_item[i].hu_xi = hu_xi;
		}
		
		int ben_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(ben_index, 0);
		int first_index= -1;
		int second_index = -1;
		for(int i = 0; i< weave_count;i++)
		{
			
			if((weave_item[i].weave_kind& eat_type)!=0&&this.get_card_color(weave_item[i].weave_card[0])==2){
				for(int j = 0; j< weave_item[i].weave_card_count;j++){
					ben_index[this.switch_to_card_index(weave_item[i].weave_card[j])]++;
					if(weave_item[i].weave_card[j] == 0x21&&first_index != -1)
						second_index = i;
					else if(weave_item[i].weave_card[j] == 0x21)
						first_index = i;
				}
			}
			
		}
		int first_rote = 19;
		int next_rote = 19;
		for(int i = 20; i< GameConstants.XPBH_MAX_INDEX;i++){
			if(ben_index[i] == 0)
			{
				break;
			}
			if(ben_index[i] == 2&&next_rote+1 == i)
				next_rote = i;
			if(ben_index[i] == 1||next_rote == i)
				first_rote = i;
			else if(ben_index[i] == 2){
				int ben_count = 0;
				for(int j = i; j<GameConstants.XPBH_MAX_INDEX;j++)
				{
					if(ben_index[j] == 2)
						ben_count++;
					else 
						break;
				}
						
				if(ben_count < 3)
				{
					i += ben_count-1;
					first_rote = i;
					break;
				}
				else  
				{
					i += ben_count-1;
					first_rote = i;
				}
			}
		}
		if(first_rote > 21&&first_index!=-1)
			weave_item[first_index].hu_xi = first_rote-19;
		if(next_rote > 21&&second_index!=-1)
			weave_item[second_index].hu_xi = next_rote-19;

		return 0;
	}

	public void ming_index_temp(int cbMingIndexTemp[], WeaveItem weaveItems[], int weaveCount, boolean zimo, int cur_card) {
		if (zimo == false) {
			if (cur_card != 0)
				cbMingIndexTemp[switch_to_card_index(cur_card)] = 1;
		}
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_PENG:
				cbMingIndexTemp[switch_to_card_index(weaveItems[i].center_card)] = 1;
				break;
			case GameConstants.WIK_LEFT: {
				int card_index = this.switch_to_card_index(weaveItems[i].center_card);
				for (int j = card_index; j <= card_index + 2; j++) {
					cbMingIndexTemp[j]++;
				}
				break;
			}
			case GameConstants.WIK_CENTER: {
				int card_index = this.switch_to_card_index(weaveItems[i].center_card);
				for (int j = card_index - 1; j <= card_index + 1; j++) {
					cbMingIndexTemp[j]++;
				}
				break;
			}
			case GameConstants.WIK_RIGHT: {
				int card_index = this.switch_to_card_index(weaveItems[i].center_card);
				for (int j = card_index - 2; j <= card_index; j++) {
					cbMingIndexTemp[j]++;
				}
				break;
			}

			}
		}
		return;
	}

	public int calculate_dui_zi_hu_count(AnalyseItem analyseItem) {
		int dui_zi_count = 0;
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch (analyseItem.cbWeaveKind[j]) {
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_PENG: {
				dui_zi_count++;
				break;
			}
			}
		}
		if (analyseItem.cbCardEye != 0) {
			dui_zi_count++;
		}
		return dui_zi_count;
	}

	public void analyse_item_to_card(AnalyseItem analyseItem, int cbAnalyseIndexTemp[]) {
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			switch (analyseItem.cbWeaveKind[j]) {
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_PENG: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				cbAnalyseIndexTemp[card_index] += 3;
				break;
			}

			case GameConstants.WIK_LEFT: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index; i <= card_index + 2; i++) {
					cbAnalyseIndexTemp[i]++;
				}
				break;
			}
			case GameConstants.WIK_CENTER: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index - 1; i <= card_index + 1; i++) {
					cbAnalyseIndexTemp[i]++;
				}
				break;
			}
			case GameConstants.WIK_RIGHT: {
				int card_index = this.switch_to_card_index(analyseItem.cbCenterCard[j]);
				for (int i = card_index - 2; i <= card_index; i++) {
					cbAnalyseIndexTemp[i]++;
				}
				break;
			}
			}
		}
		if (analyseItem.cbCardEye != 0) {
			int card_index = this.switch_to_card_index(analyseItem.cbCardEye);
			cbAnalyseIndexTemp[card_index] += 2;
		}
		return;
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
	public int analyse_first_pao_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
	public int analyse_pao_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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

	// 提牌判断
	public int get_action_ti_Card(int cards_index[], int ti_cards_index[]) {
		int ti_card_count = 0;
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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

	public int analyse_gang_card_hh(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave) {
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
	public int analyse_gang_by_card_hand_card(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
	public int analyse_gang_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
	public int analyse_pao_by_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
			} else if (WeaveItem[i].weave_kind == GameConstants.WIK_SAO) {
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
	public boolean is_dan_diao(WeaveItem weaveItem[], int weave_count, int cards_index[], int cur_card) {

		// 四碰判断
		if (weave_count != 4) {
			return false;
		}
		for (int i = 0; i < weave_count; i++) {
			if (weaveItem[i].weave_kind != GameConstants_YYZHZ.WIK_PENG) {
				return false;
			}
		}

		// 单吊判读
		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int nTaltal = 0;
		boolean bDuizi = false;
		boolean has_magic = false;
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			if (cbCardCount == 0) {
				continue;
			}
			if (is_magic_index(i)) {
				has_magic = true;
			}
			if (cbCardCount == 2) {
				bDuizi = true;
			}
			nTaltal += cbCardCount;
		}

		if (bDuizi && nTaltal == 2) {
			return true;
		} else if (has_magic && nTaltal == 2) {
			return true;
		}
		return false;

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
			if ((weaveItems[i].weave_kind != GameConstants.WIK_PENG && weaveItems[i].weave_kind != GameConstants.WIK_GANG
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
			if ((weaveItems[i].weave_kind != GameConstants.WIK_GANG && weaveItems[i].weave_kind != GameConstants.WIK_ZHAO)) {
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

		int cbMagicCardIndex[] = new int[GameConstants.XPBH_MAX_INDEX];
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			int num = cbMagicCardIndex[i] = cards_index[i];
			if (num > 0) {
				for (int j = 0; j < num; j++) {
					// for (int k = 0; k < 4; k++) {
					// if (analyseItem.cbCardEye[k] == 0) {
					// analyseItem.cbCardEye[k] = switch_to_card_data(i);// 复制牌眼
					// break;
					// }
					// }
				}
			}
		}

		int mj_count = GameConstants.XPBH_MAX_INDEX;
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
			if ((i < (GameConstants.XPBH_MAX_INDEX - 2)) && ((i % 3) == 0)) {
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

	public boolean isInCardEye(AnalyseItem analyseItem, int cur_card) {
		boolean isEyes = false;
		// for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
		// if (analyseItem.cbCardEye[i] == cur_card) {
		// isEyes = true;
		// break;
		// }
		// }
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
		// for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
		// if (analyseItem.cbCardEye[i] == cur_card) {
		// isEyes = true;
		// break;
		// }
		// }

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

	protected int get_magic_count(int[] cards_index) {
		// int count = 0;
		//
		// int[] hand_cards = new int[Constants_YongZhou.MAX_CARD_INDEX];
		// int hand_card_count = this.switch_to_cards_data(cards_index,
		// hand_cards);
		//
		// for (int i = 0; i < hand_card_count; i++) {
		// if (hand_cards[i] == 0x21)
		// count++;
		// }
		//
		// return count;

		return cards_index[Constants_YongZhou.MAX_CARD_INDEX - 1];
	}

	public boolean has_weave_kind(WeaveItem weaveItem[], int cbWeaveCount, int card, int weave_kind) {
		int type = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9 | GameConstants.WIK_BH_CHI_119
				| GameConstants.WIK_BH_CHI_337 | GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_H;
		if ((type & weave_kind) == 0)
			return false;
		for (int i = 0; i < cbWeaveCount; i++) {
			if ((weaveItem[i].weave_kind & weave_kind) != 0) {
				if (weave_kind != GameConstants.WIK_BH_CHI_H) {
					return true;
				}
				for (int j = 0; j < weaveItem[i].weave_card_count; j++) {
					if (weaveItem[i].weave_card[j] == card)
						return true;
				}
			}
		}
		return false;
	}
	public int get_card_to_card(int card, int weave_kind,int cards[])
	{
		int count = 0;
		int temp_card[]  = new int[3];
		int temp_card_count = this.get_special_card(weave_kind, temp_card);
		for(int i = 0; i< temp_card_count;i++){
			if(temp_card[i] != card)
				cards[count++] = temp_card[i];
		}
		if(weave_kind == GameConstants.WIK_BH_CHI_H){
			int value = get_card_value(card);
			temp_card_count = 3;
			for(int i = 0; i<temp_card_count;i++)
			{
				if(get_card_color(card) != i)
					cards[count++] = i*16+value;
			}
		}
		return count;
	}
	public int get_two_card_kind(int kind[],int card[],int count){
		int kind_count = 0;
		int weave_kind[][] = new int[2][3];
		int weave_kind_count[] = new int[2];
		for(int i = 0;i<count;i++)
		{
			 weave_kind[i] = new int[3];
			weave_kind_count[i] = get_card_to_kind(card[i],weave_kind[i]);
		}
		for(int i = 0; i<weave_kind_count[0];i++)
		{
			for(int j = 0; j< weave_kind_count[1];j++){
				if(weave_kind[0][i] != weave_kind[1][j])
					continue;
				if(weave_kind[0][i] == GameConstants.WIK_BH_CHI_H)
				{
					if(get_card_value(card[0]) != get_card_value(card[1]))
						continue;
				}
				 kind[kind_count++] = weave_kind[0][i];
			}
		}
		
		return kind_count;
	}
	public int get_card_to_kind(int card,int weave_kind[]){
		int count = 0;
		switch(card){
		case 0x2a:
		case 0x18:
			weave_kind[count++] = GameConstants.WIK_BH_CHI_A98;
			break;
		case 0x09:
			weave_kind[count++] = GameConstants.WIK_BH_CHI_A98;
			weave_kind[count++] = GameConstants.WIK_BH_CHI_A99;
			break;
		case 0x0a:
		case 0x19:
			weave_kind[count++] = GameConstants.WIK_BH_CHI_A99;
			weave_kind[count++] = GameConstants.WIK_BH_CHI_AA9;
			break;
		case 0x1a:
			weave_kind[count++] = GameConstants.WIK_BH_CHI_AA9;
			break;
		case 0x01:
		case 0x11:
		case 0x29:
			weave_kind[count++] = GameConstants.WIK_BH_CHI_119;
			break;
		case 0x02:
		case 0x12:
		case 0x28:
			weave_kind[count++] = GameConstants.WIK_BH_CHI_228;
			break;
		case 0x03:
		case 0x13:
		case 0x27:
			weave_kind[count++] = GameConstants.WIK_BH_CHI_337;
			break;	
		}
		if(this.is_yao_card(card) == false)
			weave_kind[count++] = GameConstants.WIK_BH_CHI_H;
		return count;
	}
	public int delete_arrays_to_arrays(int cards_data[],int cards_count,int delete_data[],int delete_count){
		int count = 0;
		for(int i = 0; i < cards_count ;i++ )
		{
			boolean flag = false;
			for(int j = 0; j < delete_count ;j ++){
				if(cards_data[i] == delete_data[j])
				{
					flag = true;
					break;
				}
			}
			if(flag == false)
				cards_data[count++] = cards_data[i];
		}
		return count;
	}
	public int get_same_to_arrays(int cards_data[],int cards_count,int same_data[],int same_count,int cards[]){
		int count = 0;
		for(int i = 0; i < cards_count ;i++ )
		{
			for(int j = 0; j < same_count ;j ++){
				if(cards_data[i] == same_data[j])
				{
					cards[count++] = cards_data[i];
					break;
				}
			}
		}
		return count;
	}
	public int  check_hu(WeaveItem weaveItem[],WeaveItem hu_weaveItem[],int temp_cards_index[] ,ShangWeaveItem shang_weave_item[],
			 int cards_index[],int weave_count,int cur_card,boolean is_peng){
		int special_chi = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9
				| GameConstants.WIK_BH_CHI_119 | GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_337
				| GameConstants.WIK_BH_CHI_H;
		int eat_type = GameConstants.WIK_BH_CHI_L | GameConstants.WIK_BH_CHI_R | GameConstants.WIK_BH_CHI_C;
		int same_type = GameConstants.WIK_BH_DAGUN | GameConstants.WIK_BH_PENG | GameConstants.WIK_BH_ZHUA_LONG | GameConstants.WIK_BH_SHE
				| GameConstants.WIK_BH_KAIZ;
		int shang_type = GameConstants.WIK_BH_SHANG| GameConstants.WIK_BH_XIA;
		int shang_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		
		for(int i = 0; i < weave_count ;i++)
		{
			int index = temp_cards_index[i];
			if(index == shang_weave_item[i].count-1)
				continue;
			if(shang_weave_item[i].card_count[index] >=2 && (weaveItem[i].weave_kind&special_chi)==0&& (weaveItem[i].weave_kind&same_type)!=0)
				weaveItem[i].weave_card[weaveItem[i].weave_card_count-1] &= 0xff; 
			for(int j = 0; j < shang_weave_item[i].card_count[index];j++)
			{
				int weave_index  = this.switch_to_card_index(shang_weave_item[i].weaveCard[index][j]);
				shang_count[weave_index]++;
				if(cards_index[weave_index] == 0)
					return 0;
				weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[index][j]; 
				hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[index][j];
				if((weaveItem[i].weave_kind & eat_type)!=0)
				{
					if(shang_count[weave_index] > 0)
						shang_count[weave_index] --;
				}
				cards_index[weave_index] --;
			}
			if(shang_weave_item[i].card_count[index] > 0)
			{
				if((weaveItem[i].weave_kind & eat_type)!=0 && shang_weave_item[i].weavekind[index] ==  GameConstants.WIK_BH_SHANG)
					shang_weave_item[i].max_value += shang_weave_item[i].card_count[index];
				else if((weaveItem[i].weave_kind & eat_type)!=0)
					shang_weave_item[i].min_value -= shang_weave_item[i].card_count[index];
				weaveItem[i].weave_kind |= shang_weave_item[i].weavekind[index];
				
			}
		}
		int same_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(same_index, -1);
		int same_card[] = new int[GameConstants.XPBH_MAX_COUNT];
		int same_card_count = 0;

		for(int i = 0; i< weave_count;i++)
		{
			if ((weaveItem[i].weave_kind & same_type) != 0) {
				if ((weaveItem[i].weave_kind & same_type) != 0&&(weaveItem[i].weave_kind & special_chi) == 0) {
					for (int j = 0; j < weaveItem[i].weave_card_count; j++) {
						if (same_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] == -1) {
							same_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] = i;
							same_card[same_card_count++] = weaveItem[i].weave_card[j] & 0xFF;

						}
					}

				}
			}
			
		}
		int weave_kind_count = 0;
		int weave_kind_item[] = new int[weave_count];
		int weave_center_card[] = new int[weave_count];
		int weave_same_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		int must_kind_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		int same_kind_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(weave_same_index, 0);
		Arrays.fill(same_kind_index,0);
		Arrays.fill(must_kind_index,0);
		int com_kind_item[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(com_kind_item, 0);
		int cards[] = new int[3];
		int  is_two = 0;
		int one_two_count = 0;

		int cbLessKindItem = same_card_count;
		int cbKindItemCount = same_card_count;
		int cbIndex[] = new int[same_card_count];
		int same_kind[] = new int[GameConstants.XPBH_MAX_INDEX];
		for(int i = 0;  i < same_card_count ;i++){
			cbIndex[i] = i;
		}
		do{
			is_two = 0;
			weave_kind_count = 0;
			Arrays.fill(weave_same_index, 0);
			Arrays.fill(same_kind_index,0);
			for(int i = 0; i< cbLessKindItem-1;i++)
			{
				cards[0] = same_card[cbIndex[i]];
				for(int j = i+1;j<cbLessKindItem;j++){
					cards[1] = same_card[cbIndex[j]];
					int kind[] = new int [2];				
					int kind_count = this.get_two_card_kind(kind,cards, 2);
					int must_k = -1;
					for( int k = 0; k<kind_count ;k++)
					{
						int chi_card[] = new int[3];
						this.get_must_kind_card(kind[k],same_card[cbIndex[i]],same_card[cbIndex[j]],chi_card);
						int index = this.switch_to_card_index(chi_card[0]);
						if(shang_count[index] == 0&& cards_index[index]>0&& same_index[index] == -1&&this.is_yao_card(chi_card[0])==false)
							 must_k = k;
						
					}
					for(int  k = 0; k < kind_count; k++){
						if(must_k != -1)
						{
							if(must_k != k)
								continue;
						}
							
						if(kind[k] != 0){
							int chi_card[] = new int[3];
							this.get_must_kind_card(kind[k],same_card[cbIndex[i]],same_card[cbIndex[j]],chi_card);
							boolean flag = false;
							int index = this.switch_to_card_index(chi_card[0]);
							for(int kind_index = 0; kind_index < weave_kind_count ; kind_index++){
								if(kind[k] == weave_kind_item[kind_index]&&kind[k] != GameConstants.WIK_BH_CHI_H)
								{
									flag = true;
									break;		
								}
								if(kind[k] == GameConstants.WIK_BH_CHI_H && cards_index[index] == 0)
								{
									flag = true;
									break;	
								}
								if(kind[k] == GameConstants.WIK_BH_CHI_H && this.get_card_value(chi_card[0]) == weave_center_card[kind_index])
								{
									flag = true;
									break;	
								}
								if(kind[k] == GameConstants.WIK_BH_CHI_H&&com_kind_item[index]!=0){
									flag = true;
									break;
								}
							}
							if(flag == true)
								continue;

							if(shang_count[index] != 0 && (cards_index[index] > 0 )&&kind[k] == GameConstants.WIK_BH_CHI_H)
								continue;
							if(cards_index[index] == 0 && kind[k] == GameConstants.WIK_BH_CHI_H)
								continue;
							if(shang_count[index] == 0&&this.is_yao_card(switch_to_card_data(index))!=true)
							{
								must_kind_index[index] = kind[k];
							}
							same_kind_index[index] = kind[k];
							int d_index = 0;
							for( ;d_index <cbLessKindItem; d_index ++ )
							{
								if(chi_card[0] == same_card[cbIndex[d_index]])
									break;
							}
							if(!(cards_index[index] != 0 || d_index != cbLessKindItem))
								continue;
							if(cbLessKindItem ==same_card_count&& cards_index[index]>0 && same_index[index] == -1 )
								same_kind[index]++;
							weave_center_card[weave_kind_count] = chi_card[0];
							weave_kind_item[weave_kind_count++] = kind[k];
							com_kind_item[index] = kind[k];
							if(weave_same_index[this.switch_to_card_index(same_card[cbIndex[i]])] >0)
								is_two ++;
							weave_same_index[this.switch_to_card_index(same_card[cbIndex[i]])]++;
							if(weave_same_index[this.switch_to_card_index(same_card[cbIndex[j]])] >0)
								is_two ++;
							weave_same_index[this.switch_to_card_index(same_card[cbIndex[j]])]++;
						}
					}
					
				}
			}
		
			boolean must_flag = true;
			if(is_two <=  1)
			{
				int weave_same_count = 0;
				for(int i = 0; i < GameConstants.XPBH_MAX_INDEX;i++){
					if(same_kind[i] > 0 && same_index[i] != -1 &&weave_same_index[i] == 0)
					{
						is_two = 2;
						break;
					}
					if(weave_same_index[i] >= 2)
					{
						is_two = 2;
						break;
					}
					weave_same_count += weave_same_index[i] ;
					if(must_kind_index[i]!= 0)
					{
						if(same_kind_index[i] == 0)
							must_flag = false;
					}
				}
				if(is_two <=  1&&weave_same_count!=0&&must_flag == true)
					break;
			}
			if(cbLessKindItem ==same_card_count)
			{
				one_two_count = is_two;
//				cbLessKindItem = same_card_count-1;
			}
			// 设置索引
			if(cbLessKindItem<2)
				break;
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
				{
					if(one_two_count>1)
					{
						cbLessKindItem --;
						one_two_count--;
					}
					else
						break;
				}
			} else
				cbIndex[cbLessKindItem - 1]++;
		}while(true);
		CombineWeaveItem combine_weaveItem[] = new CombineWeaveItem [GameConstants.XPBH_MAX_INDEX];
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX; i++ )
		{
			combine_weaveItem[i] = new CombineWeaveItem();
			for(int j = 0; j<combine_weaveItem[i].item_Count.length;j++ )
			{
				Arrays.fill(combine_weaveItem[i].combineItem[j], -1);
			}
		}
		if(is_two <= 1){
			for(int i = 0; i<weave_kind_count; i++)
			{
				int weave_card[] = new int[3];
				int weave_card_count = this.get_card_to_card(weave_center_card[i],weave_kind_item[i] , weave_card);
				int weave_index = this.switch_to_card_index(weave_center_card[i]);
				int weave_same_card[] = new int[3];
				int chi_same_count = this.get_same_to_arrays(same_card, same_card_count, weave_card, weave_card_count, weave_same_card);
				int count = combine_weaveItem[weave_index].combine_count;
				combine_weaveItem[weave_index].weave_kind[count] = weave_kind_item[i];	
				
				for(int j = 0; j<chi_same_count ;j++){
					if( weave_same_index[weave_index]>1&&chi_same_count == 3 && this.switch_to_card_index(weave_same_card[j])==weave_index)
						continue;
					combine_weaveItem[weave_index].combineItem[count][j] = same_index[this.switch_to_card_index(weave_same_card[j])] ;
					combine_weaveItem[weave_index].item_Count[count]++;
				}
				combine_weaveItem[weave_index].combine_count++;
			}
		}
		
		int chi_type = special_chi&(~GameConstants.WIK_BH_CHI_H);
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind&chi_type)!=0){
				for(int j = 0; j< shang_weave_item[i].count-1;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if((shang_weave_item[i].weavekind[j]&weaveItem[i].weave_kind)==0)
							continue;
						while(cards_index[weave_index]>0&&combine_weaveItem[weave_index].combine_count == 0)
						{
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							cards_index[weave_index]--;
						}
					}
				}
			}
		}
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind&(GameConstants.WIK_BH_CHI_H))!=0){
				for(int j = 0; j< shang_weave_item[i].count-1;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if((shang_weave_item[i].weavekind[j]&weaveItem[i].weave_kind)==0)
							continue;
						if(this.is_yao_card(shang_weave_item[i].weavekind[j]) == true)
							continue;
						while(cards_index[weave_index]>0&&combine_weaveItem[weave_index].combine_count == 0)
						{
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							cards_index[weave_index]--;
						}
					}
				}
			}
		}
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind&eat_type)!=0){
				
				for(int j = 0; j< shang_weave_item[i].count-1;j++){
					
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if(this.is_yao_card(shang_weave_item[i].weavekind[j]) == true)
							continue;
						if(!((shang_weave_item[i].min_value -1==shang_weave_item[i].weaveCard[j][k])||
						(shang_weave_item[i].weaveCard[j][k] ==shang_weave_item[i].max_value + 1)))
							continue;
						if(cards_index[weave_index]>0&&combine_weaveItem[weave_index].combine_count == 0)
						{
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							cards_index[weave_index]--;
							if(shang_weave_item[i].weavekind[j] ==  GameConstants.WIK_BH_SHANG)
								shang_weave_item[i].max_value += 1;
							else 
								shang_weave_item[i].min_value -= 1;
						}
					}
				}
			}
		}
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind&eat_type)!=0){
				if(get_card_color(shang_weave_item[i].min_value) == 2 && get_card_value(shang_weave_item[i].min_value) == 2&&cards_index[20]>0){
					weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = 0x21;
					hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = 0x21 ;
					cards_index[20]--;	 
					shang_weave_item[i].min_value -= 1;
				}
				
			}
		}
		
		int pai_count = 0;
		boolean is_yao = false;
		int item_index[][] = new int[4][weave_count];
		int item_count  = 0;
		for(int i = 0; i<3;i++){
			Arrays.fill(item_index[i],0);
		}
		for(int i = 0; i< weave_count;i++){
			item_index[0][i] = i;
		}
		for(int i = 0 ; i <GameConstants.XPBH_MAX_INDEX;i++){
			if(combine_weaveItem[i].combine_count == 0)
				continue;
			
	
			if(combine_weaveItem[i].combine_count>1){
				if(combine_weaveItem[i].weave_kind[0] == GameConstants.WIK_BH_CHI_H)
				{
					
					combine_weaveItem[i].weave_kind[0] = combine_weaveItem[i].weave_kind[1];
					combine_weaveItem[i].weave_kind[0] = combine_weaveItem[i].weave_kind[1];
					combine_weaveItem[i].item_Count[0] = combine_weaveItem[i].item_Count[1];
					for(int j = 0; j<combine_weaveItem[i].item_Count[0];j++ )
						combine_weaveItem[i].combineItem[0][j] = combine_weaveItem[i].combineItem[1][j]; 
					combine_weaveItem[i].combine_count --;
				}
				else if(combine_weaveItem[i].weave_kind[1] == GameConstants.WIK_BH_CHI_H)
					combine_weaveItem[i].combine_count --;
				if((combine_weaveItem[i].weave_kind[0] == GameConstants.WIK_BH_CHI_AA9||combine_weaveItem[i].weave_kind[0] == GameConstants.WIK_BH_CHI_A99)&&
						(combine_weaveItem[i].weave_kind[1] == GameConstants.WIK_BH_CHI_A99||combine_weaveItem[i].weave_kind[1] == GameConstants.WIK_BH_CHI_AA9)){
					if(combine_weaveItem[i].weave_kind[0] == GameConstants.WIK_BH_CHI_A99)
						combine_weaveItem[i].combine_count --;
					else if(combine_weaveItem[i].weave_kind[1] == GameConstants.WIK_BH_CHI_A99)
					{
							
							combine_weaveItem[i].weave_kind[0] = combine_weaveItem[i].weave_kind[1];
							combine_weaveItem[i].item_Count[0] = combine_weaveItem[i].item_Count[1];
							for(int j = 0; j<combine_weaveItem[i].item_Count[0];j++ )
								combine_weaveItem[i].combineItem[0][j] = combine_weaveItem[i].combineItem[1][j]; 
							combine_weaveItem[i].combine_count --;
						
						
					}
				}
				if((combine_weaveItem[i].weave_kind[0] == GameConstants.WIK_BH_CHI_A98||combine_weaveItem[i].weave_kind[0] == GameConstants.WIK_BH_CHI_A99)&&
						(combine_weaveItem[i].weave_kind[1] == GameConstants.WIK_BH_CHI_A99||combine_weaveItem[i].weave_kind[1] == GameConstants.WIK_BH_CHI_A98)){
					if(combine_weaveItem[i].weave_kind[0] == GameConstants.WIK_BH_CHI_A99)
						combine_weaveItem[i].combine_count --;
					else if(combine_weaveItem[i].weave_kind[1] == GameConstants.WIK_BH_CHI_A99)
					{
						combine_weaveItem[i].weave_kind[0] = combine_weaveItem[i].weave_kind[1];
						combine_weaveItem[i].item_Count[0] = combine_weaveItem[i].item_Count[1];
						for(int j = 0; j<combine_weaveItem[i].item_Count[0];j++ )
							combine_weaveItem[i].combineItem[0][j] = combine_weaveItem[i].combineItem[1][j]; 
						combine_weaveItem[i].combine_count --;
					}
				}
					
			}
			for(int j = 0; j<combine_weaveItem[i].item_Count[0];j++)
			{
				combine_weaveItem[i].combineItem[0][j] = item_index[item_count][combine_weaveItem[i].combineItem[0][j]];
			}
			for(int index = 0; index< combine_weaveItem[i].combine_count;index++)
			{
				int temp_weave_count = 0;
				int remove_card[] = new int[3];
				int remove_count = 0;
				int card_action[] = new int[3];
				int card_count[] = new int[3];
				int hu_card_count[] = new int[3];
				int hu_remove_card[] = new int[3];
				int hu_remove_count = 0;
				int hu_card_action[] = new int[3];
				int cur_index  = -1;
				for(int j = 0; j<weave_count;j++){
					if(!(j== combine_weaveItem[i].combineItem[index][0]|| j == combine_weaveItem[i].combineItem[index][1]||j== combine_weaveItem[i].combineItem[index][2]|| cur_index == j) ){
						if(j!= temp_weave_count){
							item_index[item_count+1][j] = item_index[item_count][temp_weave_count];
							weaveItem[temp_weave_count].center_card = weaveItem[j].center_card;
							weaveItem[temp_weave_count].weave_card_count = weaveItem[j].weave_card_count;
							for (int k = 0; k < weaveItem[temp_weave_count].weave_card_count; k++) {
								weaveItem[temp_weave_count].weave_card[k] = weaveItem[j].weave_card[k];
							}
							
							hu_weaveItem[temp_weave_count].weave_card_count = hu_weaveItem[j].weave_card_count;
							for (int k = 0; k < hu_weaveItem[temp_weave_count].weave_card_count; k++) {
								hu_weaveItem[temp_weave_count].weave_card[k] = hu_weaveItem[j].weave_card[k];
							}
							hu_weaveItem[temp_weave_count].weave_kind  = hu_weaveItem[j].weave_kind;
							weaveItem[temp_weave_count++].weave_kind = weaveItem[j].weave_kind;
							hu_weaveItem[j].weave_kind = 0;
							hu_weaveItem[j].weave_card_count = 0;
							hu_weaveItem[j].hu_xi = 0;
							weaveItem[j].weave_kind = 0;
							weaveItem[j].weave_card_count = 0;
							weaveItem[j].hu_xi = 0;
							continue;
						}
						temp_weave_count++;
						continue;
					}
					remove_card[remove_count] = weaveItem[j].weave_card[0];
					card_action[remove_count] = weaveItem[j].weave_kind;
					card_count[remove_count++] = weaveItem[j].weave_card_count-1;
					if(hu_weaveItem[j].weave_card[0] != 0){
						hu_card_count[hu_remove_count] =  hu_weaveItem[j].weave_card_count;
						hu_card_action[hu_remove_count] = hu_weaveItem[j].weave_kind;
						hu_remove_card[hu_remove_count++] = hu_weaveItem[j].weave_card[0];				
						hu_weaveItem[j].weave_card_count = 0;
						hu_weaveItem[j].hu_xi = 0;
					}
					
					weaveItem[j].weave_card_count = 0;
					weaveItem[j].hu_xi = 0;	
					
				
				}
				if(remove_count == 0)
					break;
			
				weave_count -= remove_count;
				int wIndex = weave_count++;
				weaveItem[wIndex].public_card = 1;
				weaveItem[wIndex].center_card = this.switch_to_card_data(i);
				weaveItem[wIndex].weave_kind = combine_weaveItem[i].weave_kind[index]|card_action[0]|card_action[1];
				weaveItem[wIndex].weave_card_count = 0;
				if(combine_weaveItem[i].combine_count > 1)
				{
					if(cards_index[i]>0){
						weaveItem[wIndex].weave_card[weaveItem[wIndex].weave_card_count ++] = this.switch_to_card_data(i);
						hu_weaveItem[wIndex].weave_card[hu_weaveItem[wIndex].weave_card_count++] = this.switch_to_card_data(i);
						cards_index[i] --;
					}
				}
				else{
					while(cards_index[i]>0){
						weaveItem[wIndex].weave_card[weaveItem[wIndex].weave_card_count ++] = this.switch_to_card_data(i);
						hu_weaveItem[wIndex].weave_card[hu_weaveItem[wIndex].weave_card_count++] = this.switch_to_card_data(i);
						cards_index[i] --;
					}
				}
				
				for(int k = 0; k< hu_remove_count;k++){
					
					for(int j = hu_weaveItem[wIndex].weave_card_count; j<hu_weaveItem[wIndex].weave_card_count+hu_card_count[k];j++){
						hu_weaveItem[wIndex].weave_card[j] = hu_remove_card[k];
					}
					hu_weaveItem[wIndex].weave_card_count += hu_card_count[k];
					hu_weaveItem[wIndex].weave_kind |= hu_card_action[k];
				}
				for(int k = 0; k< remove_count;k++){
					int remove_index = this.switch_to_card_index(remove_card[k]&0x0ff);
					while(remove_index<30&&cards_index[remove_index]>0){
						weaveItem[wIndex].weave_card[weaveItem[wIndex].weave_card_count ++] = this.switch_to_card_data(remove_index);
						hu_weaveItem[wIndex].weave_card[hu_weaveItem[wIndex].weave_card_count++] = this.switch_to_card_data(remove_index);
						cards_index[remove_index] --;
					}
					weaveItem[wIndex].weave_card[weaveItem[wIndex].weave_card_count ++] = remove_card[k]&0xff;
					for(int j = weaveItem[wIndex].weave_card_count; j<weaveItem[wIndex].weave_card_count+card_count[k];j++){
						weaveItem[wIndex].weave_card[j] = remove_card[k];
					}
					weaveItem[wIndex].weave_card_count += card_count[k];
				}
				item_count++;
		
			}
		}
		
		for(int i = 0; i< GameConstants.XPBH_MAX_INDEX;i++)
		{
			if(cards_index[i] == 0)
				continue;
			if(this.is_yao_card(this.switch_to_card_data(i))){
				while(cards_index[i]>0){
					weaveItem[weave_count].weave_card[weaveItem[weave_count].weave_card_count++] = this.switch_to_card_data(i);
					hu_weaveItem[weave_count].weave_card[hu_weaveItem[weave_count].weave_card_count++] = this.switch_to_card_data(i);
					cards_index[i]--;
				}
					
				is_yao = true;
			}
			else
				pai_count++;
		}
		if(pai_count != 0 )
			return 0;
	
		if(is_yao == true)
			weaveItem[weave_count++].weave_kind = GameConstants.WIK_BH_LUO_YAO;
		int all_hu_xi = 0;
		for(int i = 0; i< weave_count;i++)
		{
			if(is_peng == true)
			{
				int cur_count = 0;
				if((weaveItem[i].weave_kind&special_chi)!=0){
					for(int j = 0; j<weaveItem[i].weave_card_count;j++){
						if(weaveItem[i].weave_card[j] == cur_card){
							cur_count ++;
						}
					}
					if(cur_count == 3)
					{
						for(int j = 0; j<weaveItem[i].weave_card_count;j++){
							if(weaveItem[i].weave_card[j] == cur_card && --cur_count!=2){
								weaveItem[i].weave_card[j]|=GameConstants.WIK_BH_PENG;
								hu_weaveItem[i].weave_kind = GameConstants.WIK_BH_PENG;
								is_peng = false;
							}
						}
					}
				}
			}
			this.get_weave_hu_xi(weaveItem, i, i+1);
			
		}
		for(int i = 0; i< weave_count;i++)
			all_hu_xi += weaveItem[i].hu_xi;
		if(all_hu_xi >=20)
			return weave_count;
		
		return 0;
	}
	public int analyse_pai_shang(WeaveItem weaveItem[],int weave_count,int cards_index[],int cur_card,WeaveItem hu_weaveItem[]){
		boolean is_peng = false;
		if(cur_card != 0 && cards_index[this.switch_to_card_index(cur_card)] >= 2&& this.is_yao_card(cur_card) == false)
			is_peng = true;
		
		ShangWeaveItem shang_weave_item[] = new ShangWeaveItem[weave_count];
		CombineWeaveItem combine_weave_item[]  = new CombineWeaveItem[weave_count];
		for(int i = 0; i<weave_count;i++)
		{
			shang_weave_item[i] = new ShangWeaveItem();
			combine_weave_item[i] = new CombineWeaveItem();
		}
		int combine_count = 0;
		int shang_index[][] = new int[GameConstants.XPBH_MAX_INDEX][GameConstants.XPBH_MAX_WEAVE];
		int shang_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		int only_one_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(shang_count, 0);
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++){
			shang_index[i] = new int[GameConstants.XPBH_MAX_WEAVE];
			Arrays.fill(shang_index[i], 0);
		}
		int special_chi = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9
				| GameConstants.WIK_BH_CHI_119 | GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_337
				| GameConstants.WIK_BH_CHI_H;
		int eat_type = GameConstants.WIK_BH_CHI_L | GameConstants.WIK_BH_CHI_R | GameConstants.WIK_BH_CHI_C;
		int same_type = GameConstants.WIK_BH_DAGUN | GameConstants.WIK_BH_PENG | GameConstants.WIK_BH_ZHUA_LONG | GameConstants.WIK_BH_SHE
				| GameConstants.WIK_BH_KAIZ;
		int shang_type = GameConstants.WIK_BH_SHANG| GameConstants.WIK_BH_XIA;
		int same_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		int same_card[] = new int[GameConstants.XPBH_MAX_COUNT];
		int same_card_count = 0;
		int chi_same_index[][] = new int[GameConstants.XPBH_MAX_INDEX][GameConstants.XPBH_MAX_WEAVE];
		int chi_same_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		int chi_same_kind[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(chi_same_kind, 0);
		Arrays.fill(same_index, -1);
		Arrays.fill(chi_same_count, 0);
		for(int i = 0; i< weave_count;i++)
		{
			if ((weaveItem[i].weave_kind & same_type) != 0) {
				if ((weaveItem[i].weave_kind & same_type) != 0&&(weaveItem[i].weave_kind & special_chi) == 0) {
					for (int j = 0; j < weaveItem[i].weave_card_count; j++) {
						if (same_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] == -1) {
							same_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] = i;
							same_card[same_card_count++] = weaveItem[i].weave_card[j] & 0xFF;
						}
					}

				}
			}
			
		}
		
		
		for(int i = 0;i<weave_count;i++)
		{
	
			if((weaveItem[i].weave_kind& eat_type)!=0){
				int min = 0xff;
				int max = 0;
				for(int j = 0; j< weaveItem[i].weave_card_count;j++)
				{
					if(weaveItem[i].weave_card[j]>max)
						max = weaveItem[i].weave_card[j];
					if(weaveItem[i].weave_card[j] < min)
						min = weaveItem[i].weave_card[j];
				}
				shang_weave_item[i].count = 0;
				int min_index = this.switch_to_card_index(min);
				shang_weave_item[i].min_value  = min;
				shang_weave_item[i].max_value  = max;
				if(get_card_value(min)>3)
				{	
					
					if(cards_index[min_index-1]>0 && cards_index[min_index-2]>0)
					{
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = min-1;
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][1] = min-2;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 2;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_XIA;
						shang_weave_item[i].min_value  = min;
						shang_index[min_index-1][shang_count[min_index-1]] = i;
						shang_count[min_index-1]++;
						shang_index[min_index-2][shang_count[min_index-2]] = i;
						shang_count[min_index-2]++;
					}
					else if(cards_index[min_index-1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = min-1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_XIA;
						shang_index[min_index-1][shang_count[min_index-1]] = i;
						shang_count[min_index-1]++;
						shang_weave_item[i].min_value  = min;
					}
				}
				else if(get_card_value(min)>2){
					if(cards_index[min_index-1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = min-1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_XIA;
						shang_index[min_index-1][shang_count[min_index-1]] = i;
						shang_count[min_index-1]++;
						shang_weave_item[i].min_value  = min;
					}
				}
				int max_index = this.switch_to_card_index(max);
				if(get_card_value(max)<8)
				{	
					
					if(cards_index[max_index+1]>0 && cards_index[max_index+2]>0)
					{
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = max+1;
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][1] = max+2;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 2;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_SHANG;
						shang_index[max_index+1][shang_count[max_index+1]] = i;
						shang_count[max_index+1]++;
						shang_index[max_index+2][shang_count[max_index+2]] = i;
						shang_count[max_index+2]++;
						shang_weave_item[i].max_value = max;
					}
					else if(cards_index[max_index+1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = max+1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_SHANG;
						shang_index[max_index+1][shang_count[max_index+1]] = i;
						shang_count[max_index+1]++;
						shang_weave_item[i].max_value = max;
					}
				}
				else if(get_card_value(max)<9){
					if(cards_index[max_index+1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = max+1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_SHANG;
						shang_index[max_index+1][shang_count[max_index+1]] = i;
						shang_count[max_index+1]++;
						shang_weave_item[i].max_value = max;
					}
				}
				if(shang_weave_item[i].card_count[0]+shang_weave_item[i].card_count[1] == 4)
				{
					shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = max+1;
					shang_weave_item[i].weaveCard[shang_weave_item[i].count][1] = max+2;
					shang_weave_item[i].weaveCard[shang_weave_item[i].count][2] = min-1;
					shang_weave_item[i].weaveCard[shang_weave_item[i].count][3] = min-2;
					shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_SHANG|GameConstants.WIK_BH_XIA;
				}
			}
			else if((weaveItem[i].weave_kind&same_type)!=0&&(weaveItem[i].weave_kind&special_chi)==0){
				int weave_kind[] = new int[3];
				int weave_kind_count = get_card_to_kind(weaveItem[i].center_card,weave_kind);
				shang_weave_item[i].count = 0;
				int same_kind = weaveItem[i].weave_kind&same_type;
				for(int j = 0; j<weave_kind_count;j++){
					int weave_card[] = new int[2];
					get_card_to_card(weaveItem[i].center_card,weave_kind[j],weave_card);
					if(cards_index[this.switch_to_card_index(weave_card[0])]>0&&cards_index[this.switch_to_card_index(weave_card[1])]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = weave_card[0];
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][1] = weave_card[1];
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 2;
						shang_weave_item[i].weavekind[shang_weave_item[i].count] = weave_kind[j];
						shang_index[switch_to_card_index(weave_card[0])][shang_count[switch_to_card_index(weave_card[0])]] = i;
						shang_count[switch_to_card_index(weave_card[0])]++;
						shang_index[switch_to_card_index(weave_card[1])][shang_count[switch_to_card_index(weave_card[1])]] = i;
						shang_count[switch_to_card_index(weave_card[1])]++;
						if(cards_index[this.switch_to_card_index(weaveItem[i].center_card)]>0)
						{
							shang_weave_item[i].weaveCard[shang_weave_item[i].count][2] = weaveItem[i].center_card;
							shang_index[switch_to_card_index(weaveItem[i].center_card)][shang_count[switch_to_card_index(weaveItem[i].center_card)]] = i;
							shang_count[switch_to_card_index(weaveItem[i].center_card)]++;
							shang_weave_item[i].card_count[shang_weave_item[i].count]++;
						}
						shang_weave_item[i].count++;
						
					}
				}
			}
			else if((weaveItem[i].weave_kind&special_chi)!=0){
				int weave_kind = weaveItem[i].weave_kind&special_chi;
				int weave_card[] = new int[3];
				int weave_card_count = 0;
				if(weave_kind == GameConstants.WIK_BH_CHI_H){
					weave_card_count = 3;
					for(int j = 0; j<weave_card_count;j++){
						weave_card[j] = this.get_card_value(weaveItem[i].center_card)+j*16;
					}
				}
				else 
					weave_card_count = this.get_special_card(weave_kind,weave_card);
				shang_weave_item[i].count = 0;
				for(int j = 0; j <weave_card_count;j++){
					int weave_card_index = this.switch_to_card_index(weave_card[j]);
					if(cards_index[weave_card_index]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][shang_weave_item[i].card_count[shang_weave_item[i].count]++] =weave_card[j];
						shang_weave_item[i].weavekind[shang_weave_item[i].count] = weave_kind;
						shang_index[weave_card_index][shang_count[weave_card_index]] = i;
						shang_count[weave_card_index]++;
						
					}
				}
				if(shang_weave_item[i].card_count[shang_weave_item[i].count]>0)
					
					shang_weave_item[i].count++;	
				
			}	
			shang_weave_item[i].count++;  //都扩充一个 位，不能加数据的
		}
	
		int cards[] = new int[2];

		for(int i = 0; i< same_card_count-1;i++)
		{
			cards[0] = same_card[i];
			for(int j = i+1;j<same_card_count;j++){
				cards[1] = same_card[j];
				int kind[] = new int [2];
				
				int kind_count = this.get_two_card_kind(kind,cards, 2);
				for(int k = 0; k < kind_count; k++){
					if(kind[k] != 0){
						int chi_card[] = new int[3];
						int chi_card_count = this.get_must_kind_card(kind[k],same_card[i],same_card[j],chi_card);
						int index = this.switch_to_card_index(chi_card[0]);
						if(cards_index[index] > 0 )
						{	
							chi_same_kind[index] = kind[k];
							chi_same_index[index][0] = same_index[this.switch_to_card_index(same_card[i])];
							chi_same_index[index][1] = same_index[this.switch_to_card_index(same_card[j])];

						}
					}
				}
				
			}
		}
	
		if(is_all_shang(shang_count,cards_index,weaveItem,shang_index,chi_same_kind) == false)
			return 0;
		
		int temp_weave_index[] = new int[weave_count + 1];
		for(int i = 0; i< weave_count+1; i++)
		{
			temp_weave_index[i] = 0 ;
		}
		int first_count = 0;
		int second_count = 0;
		int temp_cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		WeaveItem weave_item[] = new WeaveItem[weave_count+1];
		WeaveItem temp_hu_item[] = new WeaveItem[weave_count+1];
		ShangWeaveItem temp_shang_weave_item[] = new ShangWeaveItem[weave_count+1];

		int length = 1;
		for(int i = 0 ;i < weave_count;i++)
		{
			length *= shang_weave_item[i].count;
		}
	
		int loop = 0;
		int counterIndex = weave_count -1;
		int hu_pai = 0;
		do{
			loop++;
			boolean  is_hu_pai = true;
			for(int i = 0; i < weave_count && loop > 1 ;i ++)
			{
				if((weaveItem[i].weave_kind & special_chi) != 0&& shang_weave_item[i].count-1 != temp_weave_index[i] )
				{
					is_hu_pai = false;
					break;
				}
				if((weaveItem[i].weave_kind & shang_type)!= 0 && temp_weave_index[i]<3&&shang_weave_item[i].card_count[temp_weave_index[i]] == 1)
				{
					is_hu_pai = false;
					break;
				}
				
			}
			if(is_hu_pai == true){
				for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++)
				{
					temp_cards_index[i] = cards_index[i];
				}
				for(int i = 0; i< weave_count+1; i++)
				{
					weave_item[i] = new WeaveItem();
					temp_hu_item[i] = new WeaveItem();
					temp_shang_weave_item[i] = new ShangWeaveItem();
				}

				copy_weave_item(weaveItem,weave_item,weave_count);
				copy_weave_item(hu_weaveItem,temp_hu_item,weave_count);
				copy_shang_weave_item(temp_shang_weave_item,shang_weave_item,weave_count);
				
				hu_pai = check_hu( weave_item, temp_hu_item, temp_weave_index,temp_shang_weave_item,
						   temp_cards_index, weave_count, cur_card, is_peng) ;
				if(hu_pai != 0)
				{
					
					break;
				}
			}		
			int flag = 1;
			do{
				temp_weave_index[counterIndex]++;
				if(flag == 1)
					flag = 0;
				if(temp_weave_index[counterIndex] >= shang_weave_item[counterIndex].count){
					temp_weave_index[counterIndex] = 0;
					counterIndex -- ;
					if(counterIndex >= 0){
						flag = 2;
						continue;
					}
					counterIndex = weave_count -1;
				}
				if(flag == 2)
				{
					counterIndex = weave_count -1;
					flag = 0;
				}
			}while(flag != 0);
//			for(int i = 0; i<weave_count;i++ )
//			{
//				logger.error(i+"="+temp_weave_index[i]+"\t");
//			}
		}while (loop<length); //从每个数组中，选一个值，进行遍历

		if(hu_pai != 0)
		{
			for(int i = 0; i< weave_count; i++)
			{
				weaveItem[i] = new WeaveItem();
				hu_weaveItem[i] = new WeaveItem();
			}
			copy_weave_item(weave_item,weaveItem,hu_pai);
			copy_weave_item(temp_hu_item,hu_weaveItem,hu_pai);
			
			return hu_pai;
		}
		return 0;
	}

	public int analyse_pai_shang_next(WeaveItem weaveItem[],int weave_count,int cards_index[],int cur_card,WeaveItem hu_weaveItem[]){
		boolean is_peng = false;
		if(cur_card != 0 && cards_index[this.switch_to_card_index(cur_card)] >= 2&& this.is_yao_card(cur_card) == false)
			is_peng = true;
		ShangWeaveItem shang_weave_item[] = new ShangWeaveItem[weave_count];
		for(int i = 0; i<weave_count;i++)
		{
			shang_weave_item[i] = new ShangWeaveItem();
		}
		int shang_index[][] = new int[GameConstants.XPBH_MAX_INDEX][GameConstants.XPBH_MAX_WEAVE];
		int shang_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		int only_one_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(shang_count, 0);
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++){
			shang_index[i] = new int[GameConstants.XPBH_MAX_WEAVE];
			Arrays.fill(shang_index[i], 0);
		}
		int special_chi = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9
				| GameConstants.WIK_BH_CHI_119 | GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_337
				| GameConstants.WIK_BH_CHI_H;
		int eat_type = GameConstants.WIK_BH_CHI_L | GameConstants.WIK_BH_CHI_R | GameConstants.WIK_BH_CHI_C;
		int same_type = GameConstants.WIK_BH_DAGUN | GameConstants.WIK_BH_PENG | GameConstants.WIK_BH_ZHUA_LONG | GameConstants.WIK_BH_SHE
				| GameConstants.WIK_BH_KAIZ;
		int shang_type = GameConstants.WIK_BH_SHANG| GameConstants.WIK_BH_XIA;
		int same_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		int same_card[] = new int[GameConstants.XPBH_MAX_COUNT];
		int same_card_count = 0;
		int chi_same_index[][] = new int[GameConstants.XPBH_MAX_INDEX][GameConstants.XPBH_MAX_WEAVE];
		int chi_same_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		int chi_same_kind[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(chi_same_kind, 0);
		Arrays.fill(same_index, -1);
		Arrays.fill(chi_same_count, 0);
		for(int i = 0; i< weave_count;i++)
		{
			if ((weaveItem[i].weave_kind & same_type) != 0) {
				if ((weaveItem[i].weave_kind & same_type) != 0&&(weaveItem[i].weave_kind & special_chi) == 0) {
					for (int j = 0; j < weaveItem[i].weave_card_count; j++) {
						if (same_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] == -1) {
							same_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] = i;
							same_card[same_card_count++] = weaveItem[i].weave_card[j] & 0xFF;
						}
					}

				}
			}
			
		}
		
		
		for(int i = 0;i<weave_count;i++)
		{
	
			if((weaveItem[i].weave_kind& eat_type)!=0){
				int min = 0xff;
				int max = 0;
				for(int j = 0; j< weaveItem[i].weave_card_count;j++)
				{
					if(weaveItem[i].weave_card[j]>max)
						max = weaveItem[i].weave_card[j];
					if(weaveItem[i].weave_card[j] < min)
						min = weaveItem[i].weave_card[j];
				}
				shang_weave_item[i].count = 0;
				int min_index = this.switch_to_card_index(min);
				if(get_card_value(min)>3)
				{	
					
					if(cards_index[min_index-1]>0 && cards_index[min_index-2]>0)
					{
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = min-1;
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][1] = min-2;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 2;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_XIA;
						shang_index[min_index-1][shang_count[min_index-1]] = i;
						shang_count[min_index-1]++;
						shang_index[min_index-2][shang_count[min_index-2]] = i;
						shang_count[min_index-2]++;
					}
					else if(cards_index[min_index-1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = min-1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_XIA;
						shang_index[min_index-1][shang_count[min_index-1]] = i;
						shang_count[min_index-1]++;
					}
				}
				else if(get_card_value(min)>2){
					if(cards_index[min_index-1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = min-1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_XIA;
						shang_index[min_index-1][shang_count[min_index-1]] = i;
						shang_count[min_index-1]++;
					}
				}
				int max_index = this.switch_to_card_index(max);
				if(get_card_value(max)<8)
				{	
					
					if(cards_index[max_index+1]>0 && cards_index[max_index+2]>0)
					{
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = max+1;
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][1] = max+2;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 2;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_SHANG;
						shang_index[max_index+1][shang_count[max_index+1]] = i;
						shang_count[max_index+1]++;
						shang_index[max_index+2][shang_count[max_index+2]] = i;
						shang_count[max_index+2]++;
					}
					else if(cards_index[max_index+1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = max+1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_SHANG;
						shang_index[max_index+1][shang_count[max_index+1]] = i;
						shang_count[max_index+1]++;
					}
				}
				else if(get_card_value(max)<9){
					if(cards_index[max_index+1]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = max+1;
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 1;
						shang_weave_item[i].weavekind[shang_weave_item[i].count++] = GameConstants.WIK_BH_SHANG;
						shang_index[max_index+1][shang_count[max_index+1]] = i;
						shang_count[max_index+1]++;
					}
				}
			}
			else if((weaveItem[i].weave_kind&same_type)!=0&&(weaveItem[i].weave_kind&special_chi)==0){
				int weave_kind[] = new int[3];
				int weave_kind_count = get_card_to_kind(weaveItem[i].center_card,weave_kind);
				shang_weave_item[i].count = 0;
				int same_kind = weaveItem[i].weave_kind&same_type;
				for(int j = 0; j<weave_kind_count;j++){
					int weave_card[] = new int[2];
					get_card_to_card(weaveItem[i].center_card,weave_kind[j],weave_card);
					if(cards_index[this.switch_to_card_index(weave_card[0])]>0&&cards_index[this.switch_to_card_index(weave_card[1])]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][0] = weave_card[0];
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][1] = weave_card[1];
						shang_weave_item[i].card_count[shang_weave_item[i].count] = 2;
						shang_weave_item[i].weavekind[shang_weave_item[i].count] = weave_kind[j];
						shang_index[switch_to_card_index(weave_card[0])][shang_count[switch_to_card_index(weave_card[0])]] = i;
						shang_count[switch_to_card_index(weave_card[0])]++;
						shang_index[switch_to_card_index(weave_card[1])][shang_count[switch_to_card_index(weave_card[1])]] = i;
						shang_count[switch_to_card_index(weave_card[1])]++;
						if(cards_index[this.switch_to_card_index(weaveItem[i].center_card)]>0)
						{
							shang_weave_item[i].weaveCard[shang_weave_item[i].count][2] = weaveItem[i].center_card;
							shang_index[switch_to_card_index(weaveItem[i].center_card)][shang_count[switch_to_card_index(weaveItem[i].center_card)]] = i;
							shang_count[switch_to_card_index(weaveItem[i].center_card)]++;
							shang_weave_item[i].card_count[shang_weave_item[i].count]++;
						}
						shang_weave_item[i].count++;
						
					}
				}
			}
			else if((weaveItem[i].weave_kind&special_chi)!=0){
				int weave_kind = weaveItem[i].weave_kind&special_chi;
				int weave_card[] = new int[3];
				int weave_card_count = 0;
				if(weave_kind == GameConstants.WIK_BH_CHI_H){
					weave_card_count = 3;
					for(int j = 0; j<weave_card_count;j++){
						weave_card[j] = this.get_card_value(weaveItem[i].center_card)+j*16;
					}
				}
				else 
					weave_card_count = this.get_special_card(weave_kind,weave_card);
				shang_weave_item[i].count = 0;
				for(int j = 0; j <weave_card_count;j++){
					int weave_card_index = this.switch_to_card_index(weave_card[j]);
					if(cards_index[weave_card_index]>0){
						shang_weave_item[i].weaveCard[shang_weave_item[i].count][shang_weave_item[i].card_count[shang_weave_item[i].count]++] =weave_card[j];
						shang_weave_item[i].weavekind[shang_weave_item[i].count] = weave_kind;
						shang_index[weave_card_index][shang_count[weave_card_index]] = i;
						shang_count[weave_card_index]++;
						
					}
				}
				if(shang_weave_item[i].card_count[shang_weave_item[i].count]>0)
					
					shang_weave_item[i].count++;	
				
			}

			
		}
		int cards[] = new int[2];
		int must_same_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(must_same_index, 0);
		int must_same_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(must_same_count, 0);
		int add_shang[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(must_same_count, 0);
		int made_index_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(made_index_count, 0);
		int made_weave_count[] = new int[GameConstants.XPBH_MAX_WEAVE];
		Arrays.fill(made_weave_count, 0);
		for(int i = 0; i< same_card_count-1;i++)
		{
			cards[0] = same_card[i];
			for(int j = i+1;j<same_card_count;j++){
				cards[1] = same_card[j];
				int kind[] = new int [2];
				
				int kind_count = this.get_two_card_kind(kind,cards, 2);
				int last_card_index = -1;
				for(int k = 0; k < kind_count; k++){
					if(kind[k] != 0){
						int chi_card[] = new int[3];
						int chi_card_count = this.get_must_kind_card(kind[k],same_card[i],same_card[j],chi_card);
						int index = this.switch_to_card_index(chi_card[0]);
						int weave_card[] = new int[3];
						int weave_card_count = this.get_kind_card(kind[k], chi_card[0], weave_card);
						boolean flag = false;
						for(int l = 0; l< weave_card_count ;l++){
							if(chi_same_kind[switch_to_card_index(weave_card[l])]!=0&&kind[k] == chi_same_kind[switch_to_card_index(weave_card[l])]){
								flag = true;
							}
						}
						if(flag == true)
							continue;
						if(cards_index[index] > 0 )
						{	
							if(kind[k] == GameConstants.WIK_BH_CHI_H &&shang_count[index] != 0 ){
								continue;
							}
						    flag = false;
							for(int l = 0; l<same_card_count;l++){
								if(chi_card[0] != same_card[l])
									continue;
								if(kind[k] == GameConstants.WIK_BH_CHI_H &&shang_weave_item[chi_same_index[index][0]].count != 0 ){
									continue;
								}
								flag = true;
								chi_same_kind[index] = kind[k];
								chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[i])];
								must_same_count[this.switch_to_card_index(same_card[i])]++;
								chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[j])];
								must_same_count[this.switch_to_card_index(same_card[j])]++;
								chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[l])];
								must_same_count[this.switch_to_card_index(same_card[l])]++;
								if(kind[k] != GameConstants.WIK_BH_CHI_H){
									if(is_yao_card(same_card[i]) == false)
										add_shang[index] += 3;
									if(is_yao_card(same_card[j]) == false)
										add_shang[index] += 3;
									if(is_yao_card(same_card[l]) == false)
										add_shang[index] += 3;
									add_shang[index] += cards_index[index];
								}
								last_card_index = index;
							}
							if(flag == true)
								continue;
							chi_same_kind[index] = kind[k];
							chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[i])];
							must_same_count[this.switch_to_card_index(same_card[i])]++;
							chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[j])];
							must_same_count[this.switch_to_card_index(same_card[j])]++;
							if(kind[k] != GameConstants.WIK_BH_CHI_H){
								if(is_yao_card(same_card[i]) == false)
									add_shang[index] += 3;
								if(is_yao_card(same_card[j]) == false)
									add_shang[index] += 3;
								add_shang[index] += cards_index[index];
							}
							if(shang_count[index] == 0 && this.is_yao_card(chi_card[0]) ==false){
								if(last_card_index != -1)
									chi_same_kind[last_card_index] = 0;
								shang_weave_item[chi_same_index[index][0]].count = 0;
								shang_weave_item[chi_same_index[index][1]].count = 0;
								must_same_index[index] = kind[k];
								break;
							}
							last_card_index = index;
						}
						else {
							
							for(int l = 0; l<same_card_count;l++){
								if(chi_card[0] != same_card[l])
									continue;
								if(kind[k] == GameConstants.WIK_BH_CHI_H &&shang_weave_item[chi_same_index[index][0]].count != 0 ){
									continue;
								}
								chi_same_kind[index] = kind[k];
								chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[i])];
								must_same_count[this.switch_to_card_index(same_card[i])]++;
								chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[j])];
								must_same_count[this.switch_to_card_index(same_card[j])]++;
								chi_same_index[index][chi_same_count[index]++] = same_index[this.switch_to_card_index(same_card[l])];
								must_same_count[this.switch_to_card_index(same_card[l])]++;
								if(kind[k] != GameConstants.WIK_BH_CHI_H){
									if(is_yao_card(same_card[i]) == false)
										add_shang[index] += 3;
									if(is_yao_card(same_card[j]) == false)
										add_shang[index] += 3;
									if(is_yao_card(same_card[l]) == false)
										add_shang[index] += 3;
								}
								last_card_index = index;
							}
						}
					}
				}
				
			}
		}
		//删除必须操作 控制
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++){
			if(must_same_index[i]  == 0)
				continue;
			for(int j = 0; j<chi_same_count[i] ;j++){
				if(must_same_count[switch_to_card_index(weaveItem[chi_same_index[i][j]].center_card)]==0){
					chi_same_kind[i] = 0;
					break;
				}
				else{
					must_same_count[switch_to_card_index(weaveItem[chi_same_index[i][j]].center_card)] = 0;
				}
			}	
		}
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++){
			if(chi_same_kind[i]  == 0)
				continue;
			for(int j = 0; j<chi_same_count[i] ;j++){
				if(must_same_count[switch_to_card_index(weaveItem[chi_same_index[i][j]].center_card)] !=0){
					chi_same_kind[i] = 0;
					break;
				}
				else if(must_same_count[switch_to_card_index(weaveItem[chi_same_index[i][j]].center_card)]>1){
					for(int k = 0; k < GameConstants.XPBH_MAX_INDEX; k++)
					{
						if(chi_same_kind[k]  == 0 || k == i)
							continue;
						for(int l = 0; l<chi_same_count[k] ;l++){
							if(must_same_count[switch_to_card_index(weaveItem[chi_same_index[k][l]].center_card)]==must_same_count[switch_to_card_index(weaveItem[chi_same_index[i][j]].center_card)]){
								if(add_shang[i] >= add_shang[k])
									chi_same_kind[k] = 0;
								else
									chi_same_kind[i] = 0;
								must_same_count[switch_to_card_index(weaveItem[chi_same_index[k][l]].center_card)] = 1;
								break;
							}
						}
						if(chi_same_kind[k] == 0 || chi_same_count[i] == 0)
							break;
					}
				}
			}	
		}
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++){
			if(chi_same_kind[i]  == 0)
				continue;
			for(int l = 0; l<chi_same_count[i] ;l++){
				for(int j = 0; j< shang_weave_item[chi_same_index[i][l]].count;j++){
					for(int k = 0; k< shang_weave_item[chi_same_index[i][l]].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[chi_same_index[i][l]].weaveCard[j][k]);
					
						if(shang_count[weave_index] == 1&& i != weave_index && 
								is_yao_card(shang_weave_item[chi_same_index[i][l]].weaveCard[j][k]) == false && cards_index[weave_index]>0)
						{
							chi_same_kind[i] = 0;
							break;
						}
					}
					if(chi_same_kind[i] == 0)
						break;
				}
				if(chi_same_kind[i] == 0)
					break;
			}
			if(chi_same_kind[i] == 0)
				continue;
			for(int l = 0; l<chi_same_count[i] ;l++){
				made_weave_count[chi_same_index[i][l]]  = 1;
				shang_weave_item[chi_same_index[i][l]].count = 0;
			}
		}

		for(int i = 0; i< same_card_count-1;i++)
		{
			cards[0] = same_card[i];
			for(int j = i+1;j<same_card_count;j++){
				cards[1] = same_card[j];
				int kind[] = new int [2];
				
				int kind_count = this.get_two_card_kind(kind,cards, 2);
				for(int k = 0; k < kind_count; k++){
					if(kind[k] != 0){
						int chi_card[] = new int[3];
						int chi_card_count = this.get_must_kind_card(kind[k],same_card[i],same_card[j],chi_card);
						int index = this.switch_to_card_index(chi_card[0]);
						if(cards_index[index] > 0 )
						{	
							if(shang_count[index] == 0){
//								is_one_card(cards_index,shang_weave_item[i],shang_index,shang_count);
							}
							chi_same_kind[index] = kind[k];
							chi_same_index[index][0] = same_index[this.switch_to_card_index(same_card[i])];
							chi_same_index[index][1] = same_index[this.switch_to_card_index(same_card[j])];

						}
					}
				}
				
			}
		}
		if(is_all_shang(shang_count,cards_index,weaveItem,shang_index,chi_same_kind) == false)
			return 0;
		
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind& eat_type)!=0){
				int weave_card[] = new int[4];
				int weave_card_count = 0;
				for(int j = 0; j< shang_weave_item[i].count;j++){
					
					if(shang_weave_item[i].weavekind[j] == GameConstants.WIK_BH_XIA){
						boolean flag = false;
						boolean flag_last = false;
						for(int k = shang_weave_item[i].card_count[j]-1;k>=0;k--)
						{
							int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
							if(k == 0 && cards_index[weave_index] == 0&&flag_last == true)
								return 0;
							else  if(k == 0){
								if(cards_index[weave_index]>0)
								{
									if(flag_last == true)
										flag = true;
									if((chi_same_kind[weave_index]&(special_chi&(~GameConstants.WIK_BH_CHI_H)))!=0){
										continue;
									}
									if(shang_count[weave_index] == 1)
										flag = true;
									if(shang_count[weave_index] != 1&&flag == false){
										int l = 0;
										for( ;l< shang_count[weave_index];l++)
										{
											if((weaveItem[shang_index[weave_index][l]].weave_kind&eat_type)==0)
												break;
										}
										if(l == shang_count[weave_index])
											flag = true;
									}
									if(flag == true)
									{
										weave_card[weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
										cards_index[weave_index]--;
									}
									break;
								}
								else{
									weave_card_count = 0;
								}
							}
							if((chi_same_kind[weave_index]&(special_chi&(~GameConstants.WIK_BH_CHI_H)))!=0){
								continue;
							}	
							if(cards_index[weave_index]>0){
								if(shang_count[weave_index] == 1)
									flag_last = true;
								else if(shang_count[weave_index] > 1){
									int l = 0;
									for( ;l< shang_count[weave_index];l++)
									{
										if((weaveItem[shang_index[weave_index][l]].weave_kind & eat_type)==0)
											break;
									}
									if(l == shang_count[weave_index])
										flag_last = true;
								}
							}
							
							if(flag_last == true){
								weave_card[weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
								cards_index[weave_index]--;
							}
						}
					}

					else if(shang_weave_item[i].weavekind[j] == GameConstants.WIK_BH_SHANG){
						boolean flag = false;
						boolean flag_last = false;
						for(int k = shang_weave_item[i].card_count[j]-1;k>=0;k--)
						{
							int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
							if(k == 0 && cards_index[weave_index] == 0&&flag_last == true)
								return 0;
							else  if(k == 0){
								if(cards_index[weave_index]>0){
									if(flag_last == true)
										flag = true;
									if((chi_same_kind[weave_index]&(special_chi&(~GameConstants.WIK_BH_CHI_H)))!=0){
										continue;
									}
									if(shang_count[weave_index] == 1)
										flag = true;
									if(shang_count[weave_index] != 1&&flag == false){
										int l = 0;
										for( ;l< shang_count[weave_index];l++)
										{
											if(shang_index[weave_index][l] == i)
												continue;
											if((weaveItem[shang_index[weave_index][l]].weave_kind&eat_type)==0)
												break;
										}
										if(l == shang_count[weave_index])
											flag = true;
									}
									if(flag == true)
									{
										weave_card[weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
										cards_index[weave_index]--;
									}
									break;
								}
								else{
									weave_card_count = 0;
								}
								
							}
							if((chi_same_kind[weave_index]&(special_chi&(~GameConstants.WIK_BH_CHI_H)))!=0){
								continue;
							}
							if(cards_index[weave_index]>0){
								if(shang_count[weave_index] == 1)
									flag_last = true;
								else if(shang_count[weave_index] > 1){
									int l = 0;
									for( ;l< shang_count[weave_index];l++)
									{
										if((weaveItem[shang_index[weave_index][l]].weave_kind&eat_type)==0)
											break;
									}
									if(l == shang_count[weave_index])
										flag_last = true;
								}
							}
							
							if(flag_last == true){
								weave_card[weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
								cards_index[weave_index]--;
							}
						}
					}
					
				}
				for(int k = 0; k< weave_card_count; k++){
					weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = weave_card[k];
					hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = weave_card[k]&0xff;
					int weave_index = this.switch_to_card_index(weave_card[k]);
					int index = 0;
					for(int l = 0; l < shang_count[weave_index];l++)
					{
						if(shang_index[weave_index][l] != i)
						{
							shang_index[weave_index][index++] = shang_index[weave_index][l];
						}
					}
					shang_count[weave_index]--;
				}
				if(weave_card_count > 0)
				{
					weaveItem[i].weave_kind |= GameConstants.WIK_BH_SHANG;
				}
				
			}
		}
		for(int i = 0; i<weave_count;i++){
			if(made_weave_count[i]  == 0)
				continue;
			boolean is_one[] = new boolean[shang_weave_item[i].count];
			Arrays.fill(is_one, false);
			boolean is_made[] = new boolean[shang_weave_item[i].count];
			boolean is_yao[] = new boolean [shang_weave_item[i].count];
			Arrays.fill(is_made, false);
			Arrays.fill(is_yao, false);
			int is_shang[] = new int[shang_weave_item[i].count];
			Arrays.fill(is_shang, -1);
			int temp_shang_index[] = new int[GameConstants.XPBH_MAX_INDEX];
			for(int j = 0; j< shang_weave_item[i].count;j++){
				for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
					int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
					temp_shang_index[weave_index]++;
				}
			}
			for(int j = 0; j< shang_weave_item[i].count;j++){
				for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
					int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
					if(is_yao_card(shang_weave_item[i].weaveCard[j][k])||temp_shang_index[weave_index]>=2){
						if(is_yao[j] == false && k == 1)
							is_yao[j] = false;
						if(k == 0)
							is_yao[j] = true;
					}
					if(shang_count[weave_index] == 1 && cards_index[weave_index]>0&&is_yao[j] == false){
						is_one[j] = true;
						is_made[j] = true;
						
					}
				}
				if(is_yao[j] == false && is_one[j] == true)
				{
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						only_one_count[weave_index] ++;

					}
				}
			}
		}
		for(int i = 0; i<weave_count;i++){
			if((weaveItem[i].weave_kind&same_type)!=0&&(weaveItem[i].weave_kind&GameConstants.WIK_BH_CHI_H)==0&&(weaveItem[i].center_card == 0x29
					||weaveItem[i].center_card == 0x09||weaveItem[i].center_card == 0x19)){
				boolean is_one[] = new boolean[shang_weave_item[i].count];
				Arrays.fill(is_one, false);
				boolean is_made[] = new boolean[shang_weave_item[i].count];
				boolean is_yao[] = new boolean [shang_weave_item[i].count];
				boolean is_only[] = new boolean [shang_weave_item[i].count];
				Arrays.fill(is_made, false);
				Arrays.fill(is_yao, false);
				int is_one_count = 0;
				int is_made_count = 0;
				int is_shang[] = new int[shang_weave_item[i].count];
				Arrays.fill(is_shang, -1);
				int temp_shang_index[] = new int[GameConstants.XPBH_MAX_INDEX];
				for(int j = 0; j< shang_weave_item[i].count;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						temp_shang_index[weave_index]++;
					}
				}
				for(int j = 0; j< shang_weave_item[i].count;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if(is_yao_card(shang_weave_item[i].weaveCard[j][k])||temp_shang_index[weave_index]>=2){
							if(is_yao[j] == false && k == 1)
								is_yao[j] = false;
							if(k == 0)
								is_yao[j] = true;
						}
						else{
							is_yao[j] = false;
						}
						if(shang_count[weave_index] == 1 && cards_index[weave_index]>0&&(is_yao_card(shang_weave_item[i].weaveCard[j][k])==false||temp_shang_index[weave_index]>=2)){
							is_one[j] = true;
							is_made[j] = true;
							
						}
						else if(cards_index[weave_index] == 0)
						{
							is_made[j] = false;
							break;
						}
						else 
							is_made[j] = true;
						if(only_one_count[weave_index] != 0)
						{
							is_only[j] = true;
						}
							
					}
					if(shang_weave_item[i].weavekind[j] != GameConstants.WIK_BH_CHI_H)
						is_shang[j] = 1;
					if(is_one[j] == true)
						is_one_count++;
					if(is_made[j] == true && ((is_only[j] == is_one[j]&&is_only[j] == true)||(is_only[j] == false)))
					{	
						is_made_count++;
					}
					else is_made[j] = false;
				}
				if(is_made_count >1){
					for(int j = 0; j< shang_weave_item[i].count;j++){
						if(is_made[j] == true&&is_yao[j]){
							is_made_count--;
							is_one_count--;
							is_made[j] = false;
							is_shang[j] = -1;
						}
						if(is_made_count == 1)
							break;
					}
				}
				if((shang_weave_item[i].count >= 2&& is_one_count >= 2)){
					return 0;
				}
				
				for(int j = 0; j< shang_weave_item[i].count;j++){
					if((is_one[j] == true||(is_shang[j] == 0 && is_one_count!=1))&&is_made[j] == true)
					{
						int weave_card_data = weaveItem[i].weave_card[0];
						int weave_card_data_count = weaveItem[i].weave_card_count-1;
						weaveItem[i].weave_card_count = 0;
						weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = weave_card_data&0xff;
						for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
							int index = 0;
							for(int l = 0; l < shang_count[weave_index];l++)
							{
								if(shang_index[weave_index][l] != i)
								{
									shang_index[weave_index][index++] = shang_index[weave_index][l];
								}
							}
							shang_count[weave_index]--;
							if(cards_index[weave_index]>0)
								cards_index[weave_index]--;
						}
						for(int k = 0; k<weave_card_data_count;k++){
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = weave_card_data;
							
						}
						weaveItem[i].weave_kind |= shang_weave_item[i].weavekind[j];
					}
				}
			}
		}
		for(int i = 0; i<weave_count;i++){
			if((weaveItem[i].weave_kind&same_type)!=0&&(weaveItem[i].weave_kind&special_chi)==0){
				boolean is_one[] = new boolean[shang_weave_item[i].count];
				Arrays.fill(is_one, false);
				boolean is_made[] = new boolean[shang_weave_item[i].count];
				boolean is_yao[] = new boolean [shang_weave_item[i].count];
				boolean is_only[] = new boolean [shang_weave_item[i].count];
				Arrays.fill(is_made, false);
				Arrays.fill(is_yao, false);
				int is_one_count = 0;
				int is_made_count = 0;
				int is_shang[] = new int[shang_weave_item[i].count];
				Arrays.fill(is_shang, -1);
				int temp_shang_index[] = new int[GameConstants.XPBH_MAX_INDEX];
				for(int j = 0; j< shang_weave_item[i].count;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						temp_shang_index[weave_index]++;
					}
				}
				for(int j = 0; j< shang_weave_item[i].count;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if(is_yao_card(shang_weave_item[i].weaveCard[j][k])||temp_shang_index[weave_index]>=2){
							if(is_yao[j] == false && k == 1)
								is_yao[j] = false;
							if(k == 0)
								is_yao[j] = true;
						}
						else{
							is_yao[j] = false;
						}
						if(shang_count[weave_index] == 1 && cards_index[weave_index]>0&&is_yao_card(shang_weave_item[i].weaveCard[j][k]) == false){
							is_one[j] = true;
							is_made[j] = true;
							
						}
						else if(cards_index[weave_index] == 0)
						{
							is_made[j] = false;
							break;
						}
						else 
							is_made[j] = true;
						if(only_one_count[weave_index] != 0)
						{
							is_only[j] = true;
						}
							
					}
					if((shang_weave_item[i].weavekind[j] &GameConstants.WIK_BH_CHI_H) == 0)
						is_shang[j] = 1;
					if(is_one[j] == true)
						is_one_count++;
					if(is_made[j] == true && ((is_only[j] == is_one[j]&&is_only[j] == true)||(is_only[j] == false)))
						is_made_count++;
					else
						is_made[j] = false;
				}
				if(is_made_count >1){
					for(int j = 0; j< shang_weave_item[i].count;j++){
						if(is_made[j] == true&&is_yao[j]){
							is_made_count--;
							is_one_count--;
							is_made[j] = false;
							is_shang[j] = -1;
						}
						if(is_made_count == 1)
							break;
					}
				}
				if((shang_weave_item[i].count >= 2&& is_one_count >= 2)){
					return 0;
				}
				
				for(int j = 0; j< shang_weave_item[i].count;j++){
					if((is_one[j] == true||(is_shang[j] == 1 && is_one_count!=1))&&is_made[j] == true)
					{
						int weave_card_data = weaveItem[i].weave_card[0];
						int weave_card_data_count = weaveItem[i].weave_card_count-1;
						weaveItem[i].weave_card_count = 0;
						weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = weave_card_data&0xff;
						for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
							int index = 0;
							for(int l = 0; l < shang_count[weave_index];l++)
							{
								if(shang_index[weave_index][l] != i)
								{
									shang_index[weave_index][index++] = shang_index[weave_index][l];
								}
							}
							shang_count[weave_index]--;
							if(cards_index[weave_index]>0)
								cards_index[weave_index]--;
						}
						for(int k = 0; k<weave_card_data_count;k++){
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = weave_card_data;
							
						}
						weaveItem[i].weave_kind |= shang_weave_item[i].weavekind[j];
					}
				}
			}
		}
		int chi_type = special_chi&(~GameConstants.WIK_BH_CHI_H);
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind&chi_type)!=0){
				for(int j = 0; j< shang_weave_item[i].count;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if((shang_weave_item[i].weavekind[j]&weaveItem[i].weave_kind)==0)
							continue;
						while(cards_index[weave_index]>0&&chi_same_kind[weave_index] == 0)
						{
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							cards_index[weave_index]--;
						}
					}
				}
			}
		}
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind&(GameConstants.WIK_BH_CHI_H))!=0){
				for(int j = 0; j< shang_weave_item[i].count;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if((shang_weave_item[i].weavekind[j]&weaveItem[i].weave_kind)==0)
							continue;
						if(this.is_yao_card(shang_weave_item[i].weavekind[j]) == true)
							continue;
						while(cards_index[weave_index]>0&&chi_same_kind[weave_index] == 0)
						{
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							cards_index[weave_index]--;
						}
					}
				}
			}
		}
		for(int i = 0; i< weave_count;i++){
			if((weaveItem[i].weave_kind&eat_type)!=0){
				for(int j = 0; j< shang_weave_item[i].count;j++){
					for(int k = 0; k< shang_weave_item[i].card_count[j];k++){
						int weave_index = this.switch_to_card_index(shang_weave_item[i].weaveCard[j][k]);
						if(this.is_yao_card(shang_weave_item[i].weavekind[j]) == true)
							continue;
						if(cards_index[weave_index]>0&&chi_same_kind[weave_index] == 0)
						{
							weaveItem[i].weave_card[weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							hu_weaveItem[i].weave_card[hu_weaveItem[i].weave_card_count++] = shang_weave_item[i].weaveCard[j][k];
							cards_index[weave_index]--;
						}
					}
				}
			}
		}
		int ben_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(ben_index, 0);
		int one_ben_index[] = new int[2];
		for(int i = 0; i< weave_count;i++)
		{
			
			if((weaveItem[i].weave_kind& eat_type)!=0&&this.get_card_color(weaveItem[i].weave_card[0])==2){
				int min=0xff;
				for(int j = 0; j< weaveItem[i].weave_card_count;j++){
					ben_index[this.switch_to_card_index(weaveItem[i].weave_card[j])]++;
					if(min>weaveItem[i].weave_card[j])
						min = weaveItem[i].weave_card[j];
				}
				
				if(min == 0x22&&cards_index[20]>0)
				{
					shang_index[20][shang_count[20]++] = i;
					
				}
			}
	
			
		}
		int yi_ben = ben_index[20];
		if(cards_index[20] > 0)
		{
			ben_index[20]+= cards_index[20];
		}
		int first_rote = 19;
		int next_rote = 19;
		for(int i = 20; i< GameConstants.XPBH_MAX_INDEX;i++){
			if(ben_index[i] == 0)
			{
				break;
			}
			if(ben_index[i] == 2&&next_rote+1 == i)
				next_rote = i;
			if(ben_index[i] == 1||next_rote == i)
				first_rote = i;
			else if(ben_index[i] == 2){
				int ben_count = 0;
				for(int j = i; j<GameConstants.XPBH_MAX_INDEX;j++)
				{
					if(ben_index[j] == 2)
						ben_count++;
					else 
						break;
				}
						
				if(ben_count < 3)
				{
					i += ben_count-1;
					first_rote = i;
					break;
				}
				else  
				{
					i += ben_count-1;
					first_rote = i;
				}
			}
		}
		if(first_rote >= 23)
		{
			if(yi_ben+cards_index[20] < 1)
				first_rote  = 0; 
			else if(yi_ben >= 1)
			{
				
				for(int i = shang_count[20]-1; i>=0;i --){
					weaveItem[shang_index[20][i]].weave_card[weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					hu_weaveItem[shang_index[20][i]].weave_card[hu_weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					
				}
				yi_ben -- ;
			}
			else if(cards_index[20]>=1){
				for(int i = shang_count[20]-1; i>=0;i --){
					weaveItem[shang_index[20][i]].weave_card[weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					hu_weaveItem[shang_index[20][i]].weave_card[hu_weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					
				}
				cards_index[20]--;
			}
		}
		
		if(next_rote >= 23)
		{
			if(yi_ben + cards_index[20] < 2)
				next_rote = 0;
			else if(yi_ben >= 1)
			{
				
				for(int i = shang_count[20]-1; i>0;i --){
					weaveItem[shang_index[20][i]].weave_card[weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					hu_weaveItem[shang_index[20][i]].weave_card[hu_weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					
				}
				
			}	
			else if(cards_index[20]>=1){
				for(int i = shang_count[20]-1; i>0;i --){
					weaveItem[shang_index[20][i]].weave_card[weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					hu_weaveItem[shang_index[20][i]].weave_card[hu_weaveItem[shang_index[20][i]].weave_card_count++] = 0x21;
					
				}
				cards_index[20]--;
			}
		}
		
		for(int i = 23; i<29;i++){
			for(int j = 0; j<shang_count[i];j++){
				if((next_rote+1==i && cards_index[i]>0)||(first_rote+1 == i && cards_index[i] >0)){
					int index = shang_index[i][j];
					if((weaveItem[index].weave_kind&eat_type)!=0&&cards_index[i]>0)
					{
						weaveItem[index].weave_card[weaveItem[index].weave_card_count++] = this.switch_to_card_data(i);
						hu_weaveItem[index].weave_card[hu_weaveItem[index].weave_card_count++] = this.switch_to_card_data(i);
						cards_index[i]--;
					}
				}
					
			}
			
		}
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++){
			if(chi_same_kind[i] != 0&&cards_index[i] != 0){
				if((weaveItem[chi_same_index[i][0]].weave_kind&special_chi)!=0||(weaveItem[chi_same_index[i][1]].weave_kind&special_chi)!=0){
					chi_same_kind[i] = 0;
				}
			}
		}
		
		int pai_count = 0;
		boolean is_yao = false;
		int item_index[][] = new int[4][weave_count];
		int item_count  = 0;
		for(int i = 0; i<3;i++){
			Arrays.fill(item_index[i],0);
		}
		for(int i = 0; i< weave_count;i++){
			item_index[0][i] = i;
		}
		for(int i = 0 ; i <GameConstants.XPBH_MAX_INDEX;i++){
			if(cards_index[i] == 0&&chi_same_count[i] != 3)
				continue;
			if(chi_same_kind[i] == 0)
				continue;
	
			int temp_weave_count = 0;
			int remove_card[] = new int[3];
			int remove_count = 0;
			int card_action[] = new int[3];
			int card_count[] = new int[3];
			int hu_card_count[] = new int[3];
			int hu_remove_card[] = new int[3];
			int hu_remove_count = 0;
			int hu_card_action[] = new int[3];
			int cur_index  = -1;
			if(same_index[i] != -1){
				if((weaveItem[same_index[i]].weave_kind & same_type) != 0&&(weaveItem[same_index[i]].weave_kind & special_chi) == 0)
					cur_index = same_index[i];
			}
			for(int j = 0; j<chi_same_count[i];j++)
			{
				chi_same_index[i][j] = item_index[item_count][chi_same_index[i][j]];
			}
			for(int j = 0; j<weave_count;j++){
				if(!(j== chi_same_index[i][0]|| j == chi_same_index[i][1]||j==chi_same_index[i][2]|| cur_index == j) ){
					if(j!= temp_weave_count){
						item_index[item_count+1][j] = item_index[item_count][temp_weave_count];
						weaveItem[temp_weave_count].center_card = weaveItem[j].center_card;
						weaveItem[temp_weave_count].weave_card_count = weaveItem[j].weave_card_count;
						for (int k = 0; k < weaveItem[temp_weave_count].weave_card_count; k++) {
							weaveItem[temp_weave_count].weave_card[k] = weaveItem[j].weave_card[k];
						}
						
						hu_weaveItem[temp_weave_count].weave_card_count = hu_weaveItem[j].weave_card_count;
						for (int k = 0; k < hu_weaveItem[temp_weave_count].weave_card_count; k++) {
							hu_weaveItem[temp_weave_count].weave_card[k] = hu_weaveItem[j].weave_card[k];
						}
						hu_weaveItem[temp_weave_count].weave_kind  = hu_weaveItem[j].weave_kind;
						weaveItem[temp_weave_count++].weave_kind = weaveItem[j].weave_kind;
						hu_weaveItem[j].weave_kind = 0;
						hu_weaveItem[j].weave_card_count = 0;
						hu_weaveItem[j].hu_xi = 0;
						weaveItem[j].weave_kind = 0;
						weaveItem[j].weave_card_count = 0;
						weaveItem[j].hu_xi = 0;
						continue;
					}
					temp_weave_count++;
					continue;
				}
				remove_card[remove_count] = weaveItem[j].weave_card[0];
				card_action[remove_count] = weaveItem[j].weave_kind;
				card_count[remove_count++] = weaveItem[j].weave_card_count-1;
				if(hu_weaveItem[j].weave_card[0] != 0){
					hu_card_count[hu_remove_count] =  hu_weaveItem[j].weave_card_count;
					hu_card_action[hu_remove_count] = hu_weaveItem[j].weave_kind;
					hu_remove_card[hu_remove_count++] = hu_weaveItem[j].weave_card[0];				
					hu_weaveItem[j].weave_card_count = 0;
					hu_weaveItem[j].hu_xi = 0;
				}
				weaveItem[j].weave_card_count = 0;
				weaveItem[j].hu_xi = 0;	
				
			
			}
			
			weave_count -= remove_count;
			int wIndex = weave_count++;
			weaveItem[wIndex].public_card = 1;
			weaveItem[wIndex].center_card = this.switch_to_card_data(i);
			weaveItem[wIndex].weave_kind = chi_same_kind[i]|card_action[0]|card_action[1];
			weaveItem[wIndex].weave_card_count = 0;
			while(cards_index[i]>0){
				weaveItem[wIndex].weave_card[weaveItem[wIndex].weave_card_count ++] = this.switch_to_card_data(i);
				hu_weaveItem[wIndex].weave_card[hu_weaveItem[wIndex].weave_card_count++] = this.switch_to_card_data(i);
				cards_index[i] --;
			}
			for(int k = 0; k< hu_remove_count;k++){
				for(int j = hu_weaveItem[wIndex].weave_card_count; j<hu_weaveItem[wIndex].weave_card_count+hu_card_count[k];j++){
					hu_weaveItem[wIndex].weave_card[j] = hu_remove_card[k];
				}
				hu_weaveItem[wIndex].weave_card_count += hu_card_count[k];
				hu_weaveItem[wIndex].weave_kind |= hu_card_action[k];
			}
			for(int k = 0; k< remove_count;k++){
				weaveItem[wIndex].weave_card[weaveItem[wIndex].weave_card_count ++] = remove_card[k]&0xff;
				for(int j = weaveItem[wIndex].weave_card_count; j<weaveItem[wIndex].weave_card_count+card_count[k];j++){
					weaveItem[wIndex].weave_card[j] = remove_card[k];
				}
				weaveItem[wIndex].weave_card_count += card_count[k];
			}
			item_count++;
		}
		
		for(int i = 0; i< GameConstants.XPBH_MAX_INDEX;i++)
		{
			if(cards_index[i] == 0)
				continue;
			if(this.is_yao_card(this.switch_to_card_data(i))){
				while(cards_index[i]>0){
					weaveItem[weave_count].weave_card[weaveItem[weave_count].weave_card_count++] = this.switch_to_card_data(i);
					hu_weaveItem[weave_count].weave_card[hu_weaveItem[weave_count].weave_card_count++] = this.switch_to_card_data(i);
					cards_index[i]--;
				}
					
				is_yao = true;
			}
			else
				pai_count++;
		}
		if(pai_count != 0 )
			return 0;
	
		if(is_yao == true)
			weaveItem[weave_count++].weave_kind = GameConstants.WIK_BH_LUO_YAO;
		int all_hu_xi = 0;
		for(int i = 0; i< weave_count;i++)
		{
			if(is_peng == true)
			{
				int cur_count = 0;
				if((weaveItem[i].weave_kind&special_chi)!=0){
					for(int j = 0; j<weaveItem[i].weave_card_count;j++){
						if(weaveItem[i].weave_card[j] == cur_card){
							cur_count ++;
						}
					}
					if(cur_count == 3)
					{
						for(int j = 0; j<weaveItem[i].weave_card_count;j++){
							if(weaveItem[i].weave_card[j] == cur_card && --cur_count!=2){
								weaveItem[i].weave_card[j]|=GameConstants.WIK_BH_PENG;
								hu_weaveItem[i].weave_kind = GameConstants.WIK_BH_PENG;
								is_peng = false;
							}
						}
					}
				}
			}
			this.get_weave_hu_xi(weaveItem, i, i+1);
			
		}
		for(int i = 0; i< weave_count;i++)
			all_hu_xi += weaveItem[i].hu_xi;
		if(all_hu_xi >=20)
			return weave_count;
		return 0;
	}

	
	
	public boolean is_one_card(int cards_index[], ShangWeaveItem shang_weave_item,WeaveItem weave_item[],int shang_index[][],int shang_count[],int cur_index){
		int chi_type = GameConstants.WIK_BH_CHI_L|GameConstants.WIK_BH_CHI_C|GameConstants.WIK_BH_CHI_R;
		for(int i = 0; i< shang_weave_item.count;i++){
			for(int j = 0; j< shang_weave_item.card_count[i];j++){
				int weave_index = this.switch_to_card_index(shang_weave_item.weaveCard[i][j]);
				for(int k = 0; k < shang_count[weave_index]; k++)
				{
					if(shang_count[weave_index] == 1&&weave_index != cur_index)
						return false;
					if(shang_count[weave_index]>1&&weave_index != cur_index){
						int l = 0;
						for( ;l<shang_count[weave_index];l++){
							if((weave_item[shang_index[weave_index][l]].weave_kind&chi_type)!=0)
								break;
						}
						if(l == shang_count[weave_index]){
							
						}
						else{
							if(cards_index[weave_index] == 2)
								return true;
						}
					}
				}
			}
		}
	
		return true;
	}
	public void copy_weave_item(WeaveItem weaveItem[],WeaveItem weave_item[],int count){
		for(int i = 0; i < count ;i++){
			weave_item[i].center_card = weaveItem[i].center_card;
			weave_item[i].weave_kind = weaveItem[i].weave_kind;
			weave_item[i].weave_card_count = weaveItem[i].weave_card_count;
			for(int j = 0; j < weave_item[i].weave_card_count;j++){
				weave_item[i].weave_card[j] = weaveItem[i].weave_card[j];
			}
			weave_item[i].hu_xi = weaveItem[i].hu_xi;
		}
	}
	public void copy_shang_weave_item(ShangWeaveItem shang_weaveItem[] ,ShangWeaveItem shang_weave_item[] ,int count){
		for(int i = 0; i < count ;i++){
			shang_weaveItem[i].count = shang_weave_item[i].count;
			for(int j = 0; j< shang_weaveItem[i].count-1; j++)
			{
				shang_weaveItem[i].card_count[j] = shang_weave_item[i].card_count[j];
				for(int k = 0; k<shang_weaveItem[i].card_count[j];k++)
				{
					shang_weaveItem[i].weaveCard[j][k] = shang_weave_item[i].weaveCard[j][k];
				}
				shang_weaveItem[i].weavekind[j] = shang_weave_item[i].weavekind[j];
			}
			
			shang_weaveItem[i].min_value = shang_weave_item[i].min_value;
			shang_weaveItem[i].max_value = shang_weave_item[i].max_value;
		}
	}
	
	public boolean is_all_shang(int shang_count[],int cards_index[],WeaveItem weave_item[],int shang_index[][],int chi_same_kind[]){
		int chi_type = GameConstants.WIK_BH_CHI_L|GameConstants.WIK_BH_CHI_C|GameConstants.WIK_BH_CHI_R;
		for(int i = 0; i< GameConstants.XPBH_MAX_INDEX;i++)
		{
			if(cards_index[i] == 0)
				continue;
			if(cards_index[i] > 0 && shang_count[i] > 0)
				continue;
			if(this.is_yao_card(this.switch_to_card_data(i)))
				continue;
			if(chi_same_kind[i] != 0)
				continue;
			if(cards_index[i] > shang_count[i]){
				int count = 0;
				for(int j = 0; j<shang_count[i];j++ ){
					if(shang_index[i][j]>-1&&(weave_item[shang_index[i][j]].weave_kind&chi_type)!=0)
						count ++;
				}
				if(count == shang_count[i])
					return false;
			}
			if(this.is_yao_card(this.switch_to_card_data(i))==false)
				return false;
				
		}
		return true;
	}

	public boolean analyse_same_type(WeaveItem weaveItem[], int cbWeaveCount,int cards,int card_index[]){
		return false;
		
	}
	public boolean is_need_calculate( int cards_index[],WeaveItem weaveItem[], int  cbWeaveCount,KindItem kindItem[],int cbKindItemCount,int cur_card)
	{
		int special_chi = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9
				| GameConstants.WIK_BH_CHI_119 | GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_337;
		int eat_type = GameConstants.WIK_BH_CHI_L | GameConstants.WIK_BH_CHI_R | GameConstants.WIK_BH_CHI_C;
		int same_type = GameConstants.WIK_BH_DAGUN | GameConstants.WIK_BH_PENG | GameConstants.WIK_BH_ZHUA_LONG | GameConstants.WIK_BH_SHE
				| GameConstants.WIK_BH_KAIZ;
		int shang_type = GameConstants.WIK_BH_SHANG| GameConstants.WIK_BH_XIA;
		int temp_cards_index[]  = new int[GameConstants.XPBH_MAX_INDEX];
		int special_chi_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		int same_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		int same_card[] = new int[GameConstants.XPBH_MAX_COUNT];
		int same_card_count = 0;
		int chi_same_index[][] = new int[GameConstants.XPBH_MAX_INDEX][GameConstants.XPBH_MAX_WEAVE];
		int chi_same_count[] = new int[GameConstants.XPBH_MAX_INDEX];
		int chi_same_kind[] = new int[GameConstants.XPBH_MAX_INDEX];
		int peng_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(peng_index,0);
		int peng_count = 0;
		Arrays.fill(chi_same_kind, 0);
		Arrays.fill(same_index, -1);
		Arrays.fill(chi_same_count, 0);
		int all_hu_xi = 0;
		int same_count = 0;
		for(int i = 0; i< cbWeaveCount;i++)
		{
			this.get_weave_hu_xi(weaveItem, i, i+1);
			if ((weaveItem[i].weave_kind & same_type) != 0) {
				if ((weaveItem[i].weave_kind & same_type) != 0&&(weaveItem[i].weave_kind & special_chi) == 0) {
					for (int j = 0; j < weaveItem[i].weave_card_count; j++) {
						if (same_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] == -1) {
							same_card[same_card_count++] = weaveItem[i].weave_card[j] & 0xFF;
							same_index[this.switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] = i;
						}
					}

				}
			}
			if((weaveItem[i].weave_kind & same_type) != 0)
				same_count++;
			
		}

		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++){
			temp_cards_index[i] = cards_index[i];
			special_chi_index[i] = 0;
		}
		for(int i = 0; i<cbWeaveCount;i++ )
		{
			if((special_chi&weaveItem[i].weave_kind)!=0)
			{
				for(int j = 0; j< weaveItem[i].weave_card_count;j++ ){
					int weave_index = this.switch_to_card_index(weaveItem[i].weave_card[j]&0xff);
					special_chi_index[weave_index]++;
				}
			}
			for (int j = 0; j < weaveItem[i].weave_card_count; j++) {
				if ((weaveItem[i].weave_card[j] & same_type) != 0) {
					if (peng_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] == 0) {
						peng_index[switch_to_card_index(weaveItem[i].weave_card[j] & 0xFF)] = 1;
						peng_count++;
					}
				}
			}
			
		}
	
		boolean  kindItem_peng =  false ;
		for(int i = 0; i<cbKindItemCount;i++ )
		{
			if((special_chi&kindItem[i].cbWeaveKind)!=0)
			{
				for(int j = 0; j< 3;j++ ){
					int weave_index = this.switch_to_card_index(kindItem[i].cbCarddata[j] &0xff);
					special_chi_index[weave_index]++;
				}
			}
			if(kindItem[i].cbWeaveKind == GameConstants.WIK_BH_PENG)
			{	
				same_card[same_card_count++] = kindItem[i].cbCarddata[0] &0xff;
				kindItem_peng = true;
				if(peng_count>=3)
					all_hu_xi+=7;
			}
		}
		int cards[] = new int[2];
	
		for(int i = 0; i< same_card_count-1;i++)
		{
			cards[0] = same_card[i];
			for(int j = i+1;j<same_card_count;j++){
				cards[1] = same_card[j];
				int kind[] = new int [2];
				
				int kind_count = this.get_two_card_kind(kind,cards, 2);
				for(int k = 0; k < kind_count; k++){
					if(kind[k] != 0){
						int chi_card[] = new int[3];
						int chi_card_count = this.get_must_kind_card(kind[k],same_card[i],same_card[j],chi_card);
						int index = this.switch_to_card_index(chi_card[0]);
						if((cards_index[index] > 0 || same_index[index] !=-1)&&(kind[k]&special_chi)!=0)
						{	
							int weave_card[] = new int[3];
							int weave_card_count = this.get_special_card(kind[k], weave_card);
							for(int weave_card_index = 0; weave_card_index < weave_card_count;weave_card_index++){
								int weave_index = this.switch_to_card_index(weave_card[weave_card_index]);
								special_chi_index[weave_index]++;
							}
						}
					}
				}
				
			}
		}
		for(int i = 0; i<same_card_count;i++){
			int weave_kind[] = new int[3];
			int weave_kind_count = get_card_to_kind(same_card[i],weave_kind);
			for(int j = 0; j<weave_kind_count;j++){
				if(weave_kind[j] == GameConstants.WIK_BH_CHI_H)
					continue;
				int weave_card[] = new int[2];
				get_card_to_card(same_card[i],weave_kind[j],weave_card);
				if(cards_index[this.switch_to_card_index(weave_card[0])]>0&&cards_index[this.switch_to_card_index(weave_card[1])]>0){
					int temp_weave_card[] = new int[3];
					int weave_card_count = this.get_special_card(weave_kind[j], temp_weave_card);
					for(int k = 0; k<weave_card_count;k++)
						special_chi_index[this.switch_to_card_index(temp_weave_card[k])]++;
				}
			}
		}
		if(kindItem_peng == true){
			int index = this.switch_to_card_index(same_card[same_card_count-1]);
			if(special_chi_index[index] != 0)
				all_hu_xi += 4;
			else 
				all_hu_xi += 1;
		}
		int ben_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		Arrays.fill(ben_index, 0);

		for(int i = 0; i<cbWeaveCount;i++){
			if((weaveItem[i].weave_kind& eat_type)!=0&&this.get_card_color(weaveItem[i].weave_card[0])==2){
				for(int j = 0; j< weaveItem[i].weave_card_count;j++){
					ben_index[this.switch_to_card_index(weaveItem[i].weave_card[j])]++;
				}
			}
			if((same_type&weaveItem[i].weave_kind)!=0&&(special_chi&weaveItem[i].weave_kind)==0)
			{
				if(special_chi_index[this.switch_to_card_index(weaveItem[i].center_card)] != 0&&this.is_yao_card(weaveItem[i].center_card) == false)
					all_hu_xi += weaveItem[i].hu_xi+4;
				else all_hu_xi +=weaveItem[i].hu_xi;
			}
			else  if((weaveItem[i].weave_kind& eat_type)==0)
				all_hu_xi += weaveItem[i].hu_xi;
			
		}
		for(int i = 0 ; i<GameConstants.XPBH_MAX_INDEX;i++)
		{
			if(this.yao_card(this.switch_to_card_data(i))){
				all_hu_xi += temp_cards_index[i];
			}
			else if(special_chi_index[i] != 0)
			{
				if(temp_cards_index[i] == 2 && i == this.switch_to_card_index(cur_card))
				{
					if(peng_count>=3)
						all_hu_xi += 7;
					all_hu_xi+=3;
				}
				else{
					all_hu_xi += temp_cards_index[i];
				}
					
			}
			else if(temp_cards_index[i] == 2 && i == this.switch_to_card_index(cur_card)){
				if(peng_count>=3)
					all_hu_xi += 7;
				all_hu_xi+=3;
			}
		    if(i >= 20)
			{
				ben_index[i] += temp_cards_index[i];
			}
		}
		int count = ben_index[20];
		for(int i = 20 ; i<GameConstants.XPBH_MAX_INDEX;i++){
			while((ben_index[i]<count&&i>21&&count>0)||(i==29))
			{
				all_hu_xi+=i-20;
				count--;
				if(i == 29&&count==0)
					break;
			}
			if(ben_index[i] == 0||count==0)
				break;
				
		}
	
		if(all_hu_xi>=20)
			return true;
		else
			return false;
	}
	// 分析扑克
	public boolean analyse_card_phz(int temp_card_index[], WeaveItem weaveItem_lou, WeaveItem temp_weaveItem[], int cbWeaveCount, int seat_index,
			int provider_index, int cur_card, List<AnalyseItem> analyseItemArray, boolean has_feng, int hu_xi[],boolean dispatch,WeaveItem hu_weaveItem[]) {

		// 跑胡判断
		int cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			cards_index[i] = temp_card_index[i];
		}
	
		WeaveItem weaveItem[] = new WeaveItem[GameConstants.XPBH_MAX_WEAVE];
		for(int i = 0; i< GameConstants.XPBH_MAX_WEAVE;i ++ )
		{
			weaveItem[i] = new WeaveItem();
		}
		WeaveItem hu_pai_item = new WeaveItem();
		int hu_pai_index = -1;
		for(int i = 0 ;i< cbWeaveCount; i++)
		{
			weaveItem[i].center_card = temp_weaveItem[i].center_card;
			weaveItem[i].weave_card_count = temp_weaveItem[i].weave_card_count;
			for (int j = 0; j < weaveItem[i].weave_card_count; j++) {
				weaveItem[i].weave_card[j]= temp_weaveItem[i].weave_card[j];
			}
			weaveItem[i].weave_kind = temp_weaveItem[i].weave_kind;
			weaveItem[i].hu_xi = temp_weaveItem[i].hu_xi;
		}
		if(cur_card != 0)
		{
			boolean is_add = true;
		    int type = GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|
					GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119|
					GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228|
					GameConstants.WIK_BH_CHI_H;
			for (int weave_index = 0; weave_index < cbWeaveCount; weave_index++) {
				int weave_kind = weaveItem[weave_index].weave_kind;
				// 转换判断
				if((weave_kind & type) != 0 && seat_index != provider_index &&cards_index[this.switch_to_card_index(cur_card)] == 1&&this.is_yao_card(cur_card)){
					
					for(int j = 0 ;j < weaveItem[weave_index].weave_card_count;j++ ){
						if(weaveItem[weave_index].weave_card[j] == cur_card){
							for(int i = 0; i< 2;i++)
							{
								weaveItem[weave_index].weave_card[weaveItem[weave_index].weave_card_count++] = GameConstants.WIK_BH_PENG|cur_card;
								hu_pai_item.weave_card[hu_pai_item.weave_card_count++] = cur_card;
							}
							hu_pai_index = cbWeaveCount;
							weaveItem[weave_index].weave_kind |= GameConstants.WIK_BH_PENG;
							this.get_weave_hu_xi(weaveItem, weave_index, weave_index+1);
							hu_pai_item.weave_kind = GameConstants.WIK_BH_PENG;
							is_add = false;
							cards_index[this.switch_to_card_index(cur_card)] = 0;
							break;
						}
						if(is_add == false)
							break;
					}	
					if(is_add == false)
						break;
				}
				if((weave_kind & type) != 0 && seat_index == provider_index  && cards_index[this.switch_to_card_index(cur_card)] == 1){
					
					for(int j = 0 ;j < weaveItem[weave_index].weave_card_count;j++ ){
						if(weaveItem[weave_index].weave_card[j] == cur_card){
							for(int i = 0; i< 2;i++)
							{
								weaveItem[weave_index].weave_card[weaveItem[weave_index].weave_card_count++] = GameConstants.WIK_BH_SHE|cur_card;
								hu_pai_item.weave_card[hu_pai_item.weave_card_count++] = cur_card;
							}
							hu_pai_index = cbWeaveCount;
							weaveItem[weave_index].weave_kind |= GameConstants.WIK_BH_SHE;
							this.get_weave_hu_xi(weaveItem, weave_index, weave_index+1);
							hu_pai_item.weave_kind = GameConstants.WIK_BH_SHE;
							is_add = false;
							cards_index[this.switch_to_card_index(cur_card)] = 0;
							break;
						}
						if(is_add == false)
							break;
					}	
					if(is_add == false)
						break;
				}
				if ((weave_kind & GameConstants.WIK_BH_PENG) != 0 && dispatch == true )
				{
					int weave_card_count = 0;
					int is_da_gun = 0;
					int peng_count = 0;
					for (int i = 0; i < weaveItem[weave_index].weave_card_count; i++) {
						if (weaveItem[weave_index].weave_card[i] == (cur_card | GameConstants.WIK_BH_PENG)) {
							is_da_gun = 1;
							break;				
						}
					}
					if(is_da_gun == 0 )
						continue;
					for (int i = 0; i < weaveItem[weave_index].weave_card_count; i++) {
						if (weaveItem[weave_index].weave_card[i] != (cur_card | GameConstants.WIK_BH_PENG)) {
							weaveItem[weave_index].weave_card[weave_card_count++] = weaveItem[weave_index].weave_card[i];				
						}
						else{
							peng_count ++;
						}
					}
					weaveItem[weave_index].weave_card_count -= peng_count ;
					for(int i = weaveItem[weave_index].weave_card_count; i < weaveItem[weave_index].weave_card_count+peng_count+1;i++){
						weaveItem[weave_index].weave_card[weave_card_count++] = cur_card|GameConstants.WIK_BH_DAGUN;
					}
					weaveItem[weave_index].weave_card_count += peng_count+1;
					weaveItem[weave_index].weave_kind |= GameConstants.WIK_BH_DAGUN;
					this.get_weave_hu_xi(weaveItem, weave_index, weave_index+1);
					hu_pai_item.weave_kind = GameConstants.WIK_BH_DAGUN;
					hu_pai_item.weave_card[hu_pai_item.weave_card_count++] = cur_card;
					hu_pai_index = weave_index;
					is_add = false;
					break;
				}
				if ((weave_kind & GameConstants.WIK_BH_SHE) != 0 && seat_index != provider_index)
				{
					int is_kai_zhai = 0;
					int she_count = 0;
					for (int i = 0; i < weaveItem[weave_index].weave_card_count; i++) {
						if (weaveItem[weave_index].weave_card[i] == (cur_card | GameConstants.WIK_BH_SHE)) {
							is_kai_zhai = 1;
							break;				
						}
					}
					if(is_kai_zhai == 0 )
						continue;
					int weave_card_count = 0;
					for (int i = 0; i < weaveItem[weave_index].weave_card_count; i++) {
						if (weaveItem[weave_index].weave_card[i] != (cur_card | GameConstants.WIK_BH_SHE)) {
							weaveItem[weave_index].weave_card[weave_card_count++] = weaveItem[weave_index].weave_card[i];				
						}
						else 
							she_count++;
					}
					weaveItem[weave_index].weave_card_count -= she_count ;
					for(int i = weaveItem[weave_index].weave_card_count; i < weaveItem[weave_index].weave_card_count+she_count+1;i++){
						weaveItem[weave_index].weave_card[weave_card_count++] = cur_card|GameConstants.WIK_BH_KAIZ;
					}
					weaveItem[weave_index].weave_card_count += she_count+1;
					weaveItem[weave_index].weave_kind |= GameConstants.WIK_BH_KAIZ;
					this.get_weave_hu_xi(weaveItem, weave_index, weave_index+1);
					hu_pai_item.weave_kind = GameConstants.WIK_BH_KAIZ;
					hu_pai_item.weave_card[hu_pai_item.weave_card_count++] = cur_card;
					hu_pai_index = weave_index;
					is_add = false;
					break;
				}
				if ((weave_kind & GameConstants.WIK_BH_SHE) != 0 && seat_index == provider_index)
				{
					int is_zhua_long = 0;
					int she_count = 0;
					for (int i = 0; i < weaveItem[weave_index].weave_card_count; i++) {
						if (weaveItem[weave_index].weave_card[i] == (cur_card | GameConstants.WIK_BH_SHE)) {
							is_zhua_long = 1;
							break;				
						}
					}
					if(is_zhua_long == 0 )
						continue;
					int weave_card_count = 0;
					for (int i = 0; i < weaveItem[weave_index].weave_card_count; i++) {
						if (weaveItem[weave_index].weave_card[i] != (cur_card | GameConstants.WIK_BH_SHE)) {
							weaveItem[weave_index].weave_card[weave_card_count++] = weaveItem[weave_index].weave_card[i];				
						}
						else 
							she_count++;
					}
					weaveItem[weave_index].weave_card_count -= she_count ;
					for(int i = weaveItem[weave_index].weave_card_count; i < weaveItem[weave_index].weave_card_count+she_count+1;i++){
						weaveItem[weave_index].weave_card[weave_card_count++] = cur_card|GameConstants.WIK_BH_ZHUA_LONG;
					}
					weaveItem[weave_index].weave_card_count += she_count+1;
					weaveItem[weave_index].weave_kind |= GameConstants.WIK_BH_ZHUA_LONG;
					this.get_weave_hu_xi(weaveItem, weave_index, weave_index+1);
					hu_pai_item.weave_kind = GameConstants.WIK_BH_ZHUA_LONG;
					hu_pai_item.weave_card[hu_pai_item.weave_card_count++] = cur_card;
					hu_pai_index = weave_index;
					is_add = false;
					break;
				}
				if((weave_kind & type)!= 0 && seat_index == provider_index){
					for(int j = 0 ;j < weaveItem[weave_index].weave_card_count;j++ ){
						if(weaveItem[weave_index].weave_card[j] == cur_card&&cards_index[this.switch_to_card_index(cur_card)]==1){
							
							weaveItem[weave_index].weave_card[weaveItem[weave_index].weave_card_count++] = cur_card|GameConstants.WIK_BH_SHE;
							weaveItem[weave_index].weave_card[weaveItem[weave_index].weave_card_count++] = cur_card|GameConstants.WIK_BH_SHE;
							cards_index[this.switch_to_card_index(cur_card)] = 0;
							is_add = false;
							break;
						}
					}
					if(is_add == false)
						break;
				}
				
			}
			if(cards_index[this.switch_to_card_index(cur_card)]==2  && seat_index == provider_index){
				weaveItem[cbWeaveCount].center_card = cur_card;
				weaveItem[cbWeaveCount].weave_card_count = 3;
				for(int i = 0; i< 3;i++)
				{
					weaveItem[cbWeaveCount].weave_card[i] = GameConstants.WIK_BH_SHE|cur_card;
					hu_pai_item.weave_card[hu_pai_item.weave_card_count++] = cur_card;
				}
				hu_pai_index = cbWeaveCount;
				weaveItem[cbWeaveCount++].weave_kind = GameConstants.WIK_BH_SHE;
				this.get_weave_hu_xi(weaveItem, cbWeaveCount-1, cbWeaveCount);
				hu_pai_item.weave_kind = GameConstants.WIK_BH_SHE;
				is_add = false;
				cards_index[this.switch_to_card_index(cur_card)] = 0;
			}
			if(this.is_yao_card(cur_card)){
				if(cards_index[this.switch_to_card_index(cur_card)]==2 && seat_index != provider_index)
				{
					
					weaveItem[cbWeaveCount].center_card = cur_card;
					weaveItem[cbWeaveCount].weave_card_count = 3;
					for(int i = 0; i< 3;i++)
					{
						weaveItem[cbWeaveCount].weave_card[i] = GameConstants.WIK_BH_PENG|cur_card;
						hu_pai_item.weave_card[hu_pai_item.weave_card_count++] = cur_card;
					}
					hu_pai_index = cbWeaveCount;
					weaveItem[cbWeaveCount++].weave_kind = GameConstants.WIK_BH_PENG;
					this.get_weave_hu_xi(weaveItem, cbWeaveCount-1, cbWeaveCount);
					hu_pai_item.weave_kind = GameConstants.WIK_BH_PENG;
					is_add = false;
					cards_index[this.switch_to_card_index(cur_card)] = 0;
				}
			}
		
			if(is_add == true){
				cards_index[this.switch_to_card_index(cur_card)]++;
			}
			else{
				cur_card = 0;
			}
		}
		for (int i = 0; i < weaveItem_lou.weave_card_count; i++) {
			cards_index[this.switch_to_card_index(weaveItem_lou.weave_card[i])]++;
		}
		// 计算数目
		int cbCardCount = get_card_count_by_index(cards_index);
		hu_xi[0] = 0;
		// 需求判断
		if (cbCardCount == 0)
			return false;
		int cbLessKindItem = (cbCardCount) / 3;
		boolean bNeedCardEye = ((cbCardCount + 1) % 3 == 0);
		if (cbLessKindItem > 8)
			cbLessKindItem = 8;
		int cbLessKindCount = 1;

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[76];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		// 拆分分析
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			int card_date = switch_to_card_data(i);
			if (cards_index[i] == 3 && this.yao_card(card_date) == false) {
				kindItem[cbKindItemCount].cbCarddata[0] = card_date|GameConstants.WIK_BH_PENG;
				kindItem[cbKindItemCount].cbCarddata[1] = card_date|GameConstants.WIK_BH_PENG;
				kindItem[cbKindItemCount].cbCarddata[2] = card_date|GameConstants.WIK_BH_PENG;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = i;
				kindItem[cbKindItemCount].cbValidIndex[2] = i;
				if ((cur_card == switch_to_card_data(i)) && (seat_index != provider_index))
					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_PENG;
				kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
				kindItem[cbKindItemCount].cbShang = 1;
				cbKindItemCount++;
			}

			if (card_date == 0x09 && has_weave_kind(weaveItem, cbWeaveCount, card_date, GameConstants.WIK_BH_CHI_A98)==false) {
				int excursion[] = { 8, 17, 29 };
				for (int j = 1; j <= cards_index[i]; j++)  {
					if (cards_index[excursion[1]] >= j && cards_index[excursion[2]] >= j) {
							for (int k = 0; k < excursion.length; k++) {
								kindItem[cbKindItemCount].cbCarddata[k] = switch_to_card_data(excursion[k]);
								kindItem[cbKindItemCount].cbValidIndex[k] = excursion[k];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_A98;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbShang = 3;
							cbKindItemCount++;
						}
					}
			}
			if (card_date == 0x09 && has_weave_kind(weaveItem, cbWeaveCount, card_date, GameConstants.WIK_BH_CHI_A99)==false) {
				int excursion[] = { 8, 9, 18 };
				for (int j = 1; j <= cards_index[i]; j++)  {
					if (cards_index[excursion[1]] >= j && cards_index[excursion[2]] >= j) {
							for (int k = 0; k < excursion.length; k++) {
								kindItem[cbKindItemCount].cbCarddata[k] = switch_to_card_data(excursion[k]);
								kindItem[cbKindItemCount].cbValidIndex[k] = excursion[k];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_A99;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbShang = 3;
							cbKindItemCount++;
						}
					}
			}
			if (card_date == 0x0a && has_weave_kind(weaveItem, cbWeaveCount, card_date, GameConstants.WIK_BH_CHI_AA9)==false) {
				int excursion[] = { 9, 19, 18 };
				for (int j = 1; j <= cards_index[i]; j++)  {
					if (cards_index[excursion[1]] >= j && cards_index[excursion[2]] >= j) {
							for (int k = 0; k < excursion.length; k++) {
								kindItem[cbKindItemCount].cbCarddata[k] = switch_to_card_data(excursion[k]);
								kindItem[cbKindItemCount].cbValidIndex[k] = excursion[k];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_AA9;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbShang = 3;
							cbKindItemCount++;
						}
					}
			}
			if (card_date == 0x01 && has_weave_kind(weaveItem, cbWeaveCount, card_date, GameConstants.WIK_BH_CHI_119)==false) {
				int excursion[] = { 0, 10, 28 };
				for (int j = 1; j <= cards_index[i]; j++)  {
					if (cards_index[excursion[1]] >= j && cards_index[excursion[2]] >= j) {
							for (int k = 0; k < excursion.length; k++) {
								kindItem[cbKindItemCount].cbCarddata[k] = switch_to_card_data(excursion[k]);
								kindItem[cbKindItemCount].cbValidIndex[k] = excursion[k];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_119;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbShang = 3;
							cbKindItemCount++;
						}
					}
			}
			if (card_date == 0x03 && has_weave_kind(weaveItem, cbWeaveCount, card_date, GameConstants.WIK_BH_CHI_337)==false) {
				int excursion[] = { 2, 12, 26 };
				for (int j = 1; j <= cards_index[i]; j++)  {
					if (cards_index[excursion[1]] >= j && cards_index[excursion[2]] >= j) {
							for (int k = 0; k < excursion.length; k++) {
								kindItem[cbKindItemCount].cbCarddata[k] = switch_to_card_data(excursion[k]);
								kindItem[cbKindItemCount].cbValidIndex[k] = excursion[k];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_337;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbShang = 3;
							cbKindItemCount++;
						}
					}
			}
			if (card_date == 0x02 && has_weave_kind(weaveItem, cbWeaveCount, card_date, GameConstants.WIK_BH_CHI_228)==false) {
				int excursion[] = { 1, 11, 27 };
				for (int j = 1; j <= cards_index[i]; j++)  {
					if (cards_index[excursion[1]] >= j && cards_index[excursion[2]] >= j) {
							for (int k = 0; k < excursion.length; k++) {
								kindItem[cbKindItemCount].cbCarddata[k] = switch_to_card_data(excursion[k]);
								kindItem[cbKindItemCount].cbValidIndex[k] = excursion[k];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_228;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							kindItem[cbKindItemCount].cbShang = 3;
							cbKindItemCount++;
						}
					}
			}
			if (i < 9 && i> 0&& has_weave_kind(weaveItem, cbWeaveCount, card_date, GameConstants.WIK_BH_CHI_H)==false) {
				int excursion[] = { i, 10 + i, 20 + i };
					for (int j = 1; j <= cards_index[i]; j++)  {
						if (cards_index[excursion[1]] >= j && cards_index[excursion[2]] >= j) {
							for (int k = 0; k < excursion.length; k++) {
								kindItem[cbKindItemCount].cbCarddata[k] = switch_to_card_data(excursion[k]);
								kindItem[cbKindItemCount].cbValidIndex[k] = excursion[k];
							}
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_H;
							kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
							cbKindItemCount++;
						}
					}
			}

			// 顺子判断
			if ((i < (GameConstants.XPBH_MAX_INDEX - 2)) && (cards_index[i] > 0) && ((i % 10) < 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if ( cards_index[i + 1] >= j && cards_index[i + 2] >= j ) {
						kindItem[cbKindItemCount].cbCarddata[0] = switch_to_card_data(i);
						kindItem[cbKindItemCount].cbCarddata[1] = switch_to_card_data(i + 1);
						kindItem[cbKindItemCount].cbCarddata[2] = switch_to_card_data(i + 2);
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 2;
						if (card_date == 0x21)
							kindItem[cbKindItemCount].cbShang = 3;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_BH_CHI_L;
						kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析
		if(is_need_calculate( cards_index, weaveItem,  cbWeaveCount,kindItem,cbKindItemCount,cur_card)==false)
			return false;
		if(cbLessKindItem>cbKindItemCount)
			cbLessKindItem = cbKindItemCount;
		if (cbKindItemCount >= 0) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.XPBH_MAX_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6,7};
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}
			
			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {

				// 设置变量
				for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
					cbCardIndexTemp[i] = cards_index[i];
					}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCarddata[j] = kindItem[cbIndex[i]].cbCarddata[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				// 胡牌判断
				if (bEnoughCard == true) {
					for(int i = 0; i< GameConstants.XPBH_MAX_WEAVE;i++)
					{
						hu_weaveItem[i]  = new  WeaveItem() ;
					}
					WeaveItem weave_item[] = new WeaveItem[GameConstants.XPBH_MAX_WEAVE];
					for(int i = 0; i< GameConstants.XPBH_MAX_WEAVE;i ++ )
					{
						weave_item[i] = new WeaveItem();
					}
					int weave_count = 0;
					for(int i = 0; i< cbWeaveCount+cbLessKindItem ; i++){
						if(i<cbWeaveCount){
							weave_item[i].center_card = weaveItem[i].center_card;
							weave_item[i].weave_card_count = weaveItem[i].weave_card_count;
							for(int j = 0; j < weave_item[i].weave_card_count;j++){
								weave_item[i].weave_card[j] =weaveItem[i].weave_card[j]; 
							}
							weave_item[weave_count++].weave_kind = weaveItem[i].weave_kind;
						}
						else {
							weave_item[i].center_card = pKindItem[i-cbWeaveCount].cbCenterCard;
							hu_weaveItem[i].weave_card_count = 3;
							if(pKindItem[i-cbWeaveCount].cbWeaveKind == GameConstants.WIK_BH_PENG)
								hu_weaveItem[i].weave_kind = pKindItem[i-cbWeaveCount].cbWeaveKind ;
							weave_item[i].weave_card_count = 3;
							for (int j = 0; j < 3; j++) {
								weave_item[i].weave_card[j]= pKindItem[i-cbWeaveCount].cbCarddata[j];
								hu_weaveItem[i].weave_card[j] = weave_item[i].weave_card[j]&0xff;
							}
							weave_item[weave_count++].weave_kind = pKindItem[i-cbWeaveCount].cbWeaveKind;
						}
					}	
					if(hu_pai_index != -1)
					{
						hu_weaveItem[hu_pai_index].weave_card_count = hu_pai_item.weave_card_count;
						for(int i = 0; i<hu_pai_item.weave_card_count;i++){
							hu_weaveItem[hu_pai_index].weave_card[i] = hu_pai_item.weave_card[i];
						}
						hu_weaveItem[hu_pai_index].weave_kind = hu_pai_item.weave_kind;
					}
					hu_xi[0] = analyse_pai_shang( weave_item, weave_count, cbCardIndexTemp,cur_card,hu_weaveItem);
					if(hu_xi[0]!= 0)
						return true;
				}
				// 设置索引
				if(cbLessKindItem == 0)
					break;
				if(cbIndex[cbLessKindItem - 1]>cbKindItemCount)
					break;
				if (cbIndex[cbLessKindItem - cbLessKindCount] == (cbKindItemCount - cbLessKindCount)) {
					int i = cbLessKindItem - cbLessKindCount;
					for (; i > 0; i--) {
						if ((cbIndex[i - cbLessKindCount] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - cbLessKindCount];
							for (int j = (i - cbLessKindCount); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0) {
						cbLessKindItem--;
						for(int j = 0 ; j< 8;j++){
							cbIndex[j] = j;
						}
						if(cbWeaveCount+cbLessKindItem <3)
							break;
					}
				} else 
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}

	
	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(WeaveItem weave_items[], int weave_count, int player_action) {
		// 自摸牌等级
		player_action = player_action&(~0xf);
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 60;
		}
		if (player_action == GameConstants.WIK_BH_CHI_HU) {
			return 60;
		}
		// 跑等级
		if (player_action == GameConstants.WIK_BH_KAIZ) {
			return 50;
		}
		if (player_action == GameConstants.WIK_BH_DAGUN)
			return 40;
		if(player_action==GameConstants.WIK_BH_PENG)
				return 30;
		else if (player_action == GameConstants.WIK_BH_NULL)
			return 0;
		else 
			return 10;

	}

	// 获取动作序列最高等级
	public int get_action_list_rank(WeaveItem weave_items[], int weave_count, int action_count, int action[]) {
		int XPBH_MAX_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(weave_items, weave_count, action[i]);
			if (XPBH_MAX_INDEX < index) {
				XPBH_MAX_INDEX = index;
			}

		}

		return XPBH_MAX_INDEX;
	}

	public int get_chi_hu_action_rank_hh(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu = 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu = 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu = 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HEI)).is_empty()) {
			wFanShu = 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu = 5;
		}

		return wFanShu;
	}

	public int get_chi_hu_ying_xi_dzb_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 8;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_dzb_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 30 + 30 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()) {
			hu_xi += ying_hu_count * 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()) {
			hu_xi += chun_ying_count * 150;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			hu_xi += 200;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()) {
			hu_xi += 40;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()) {
			hu_xi += 80;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 120;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()) {
			hu_xi += 450;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()) {
			hu_xi += 300;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()) {
			hu_xi += 200;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}

		return hu_xi;
	}

	public int get_chi_hu_ying_xi_xzb_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 8;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_xzb_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 30 + 30 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()) {
			hu_xi += ying_hu_count * 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()) {
			hu_xi += chun_ying_count * 150;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			hu_xi += 200;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()) {
			hu_xi += 40;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()) {
			hu_xi += 80;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 120;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()) {
			hu_xi += 450;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()) {
			hu_xi += 300;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()) {
			hu_xi += 200;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LDH_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		return hu_xi;
	}

	public int get_chi_hu_ying_xi_qmt_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 8;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_qmt_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 30 + 30 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()) {
			hu_xi += ying_hu_count * 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()) {
			hu_xi += chun_ying_count * 150;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			hu_xi += 200;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()) {
			hu_xi += 40;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()) {
			hu_xi += 80;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 120;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()) {
			hu_xi += 450;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()) {
			hu_xi += 300;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()) {
			hu_xi += 300;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()) {
			hu_xi += 200;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LDH_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIU_DUI_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SBD_TIAN_HU)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BIAN_KAN_HU)).is_empty()) {
			hu_xi += 30;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BKB_WMQ)).is_empty()) {
			hu_xi += 100;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_HU_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHA_DAN_WMQ)).is_empty()) {
			hu_xi += 150;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_FBW_WMQ)).is_empty()) {
			hu_xi += 50;
		}

		return hu_xi;
	}

	public int get_chi_hu_ying_xi_lmt_wmq(int seat_index, ChiHuRight chiHuRight) {
		int fanshu = 0;
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			fanshu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			fanshu += 6;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			fanshu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			fanshu += 3;
		}
		if (fanshu == 0)
			fanshu = 1;
		return fanshu;
	}

	public int get_chi_hu_action_rank_lmt_wmq(int seat_index, int da_pai_count, int xiao_pai_count, int ying_hu_count, int chun_ying_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int hu_xi = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			hu_xi += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			hu_xi += 10 + 10 * (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			hu_xi += 50;
		}
		return hu_xi;
	}

	public int get_chi_hu_action_rank_dhd_chd(int seat_index, int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 3 + (hong_pai_count - 10);
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_dhd_two_chd(int seat_index, int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 2 + (hong_pai_count - 10);
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 5;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_phz_chd(int seat_index, int da_pai_count, int xiao_pai_count, int tuan_yuan_count, int huang_zhang_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 3 + (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_THIRTEEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 8;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TING_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_HU_CHD)).is_empty()) {
			wFanShu += 8 + (da_pai_count - 18);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_HU_CHD)).is_empty()) {
			wFanShu += 10 + (xiao_pai_count - 16);
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHUA_HOU_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TUAN_CHD)).is_empty()) {
			wFanShu += 8 * tuan_yuan_count;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_lIU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_CHD)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TUAN_YUAN_CHD)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MTH_DA_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MTH_XIAO_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_FAN_TIAN_CHD)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_DENG_CHD)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_GAI_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_QI_CHD)).is_empty()) {
			wFanShu += 3;
		}

		if (wFanShu == 0)
			wFanShu = 1;

		return wFanShu;
	}

	public int get_chi_hu_action_rank_phz_lba(int seat_index, int da_pai_count, int xiao_pai_count, int tuan_yuan_count, int huang_zhang_count,
			int hong_pai_count, ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
			wFanShu += 3 + (hong_pai_count - 10);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
			wFanShu += 5;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_HU_CHD)).is_empty()) {
			wFanShu += 6 + (da_pai_count - 18);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_HU_CHD)).is_empty()) {
			wFanShu += 8 + (xiao_pai_count - 16);
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TUAN_CHD)).is_empty()) {
			wFanShu += 8 * tuan_yuan_count;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_HU_CHD)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HANG_HANG_XI_lIU_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_GAI_CHD)).is_empty()) {
			wFanShu += 4;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_CHD)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_QI_CHD)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_PIAO_CHD)).is_empty()) {
			wFanShu += 3;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHUANG_PIAO_CHD)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_CHD)).is_empty()) {
			wFanShu += 2;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHUN_CHD)).is_empty()) {
			wFanShu += 8;
		}

		if (wFanShu == 0)
			wFanShu = 1;

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
		case GameConstants.WIK_XXD:// 吃小
		{
			// 设置变量
			if (cbCenterCard > 16)
				cbCenterCard = cbCenterCard - 16;
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard + 16;

			return 3;
		}
		case GameConstants.WIK_DDX:// 吃大
		{
			// 设置变量
			if (cbCenterCard < 16)
				cbCenterCard = cbCenterCard + 16;
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard - 16;

			return 3;
		}
		case GameConstants.WIK_EQS:// 吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch (cur_card_value) {
			case 2: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard + 5;
				cbCardBuffer[2] = cbCenterCard + 8;
				break;
			}
			case 7: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 5;
				cbCardBuffer[2] = cbCenterCard + 3;
				break;
			}
			case 10: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 8;
				cbCardBuffer[2] = cbCenterCard - 3;
				break;
			}

			}
			return 3;
		}
		case GameConstants.WIK_YWS:// 吃小
		{
			// 设置变量
			int cur_card_value = get_card_value(cbCenterCard);
			switch (cur_card_value) {
			case 1: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard + 4;
				cbCardBuffer[2] = cbCenterCard + 9;
				break;
			}
			case 5: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 4;
				cbCardBuffer[2] = cbCenterCard + 5;
				break;
			}
			case 10: {
				cbCardBuffer[0] = cbCenterCard;
				cbCardBuffer[1] = cbCenterCard - 9;
				cbCardBuffer[2] = cbCenterCard - 5;
				break;
			}

			}
			return 3;
		}
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_SAO: // 扫牌操作
		case GameConstants.WIK_PENG: // 碰牌操作
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI: {
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
			// logger.error("get_weave_card:invalid cbWeaveKind" + cbWeaveKind);
		}
		}

		return 0;
	}


	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);
		int cbColor = get_card_color(card);
		return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 2);
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants.XPBH_MAX_INDEX;
		}
		int color = get_card_color(card);
		int value = get_card_value(card);
		int index = color * 10 + value - 1;
		return index;
	}

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public int switch_to_card_index_yyzhz(int card) {
		if (is_valid_card(card) == false) {
			return GameConstants_YYZHZ.MAX_YYZHZ_INDEX;
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
		if (card_index >= GameConstants.XPBH_MAX_INDEX) {
			return GameConstants.XPBH_MAX_INDEX;
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
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
		if (cbCardCount == 0 || cbCardCount > GameConstants.XPBH_MAX_COUNT)
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
		XPBHGameLogic logic = new XPBHGameLogic();
		int index = logic.switch_to_card_index(24);

		for (int i = 0; i < GameConstants.CARD_DATA_FLS_LX.length; i++)
			System.out.println(GameConstants.CARD_DATA_FLS_LX[i]);
	}

	public static class AnalyseItem {
		public int cbCardEye;//// 牌眼扑克
		public boolean bMagicEye;// 牌眼是否是王霸
		public int cbWeaveKind[] = new int[GameConstants.XPBH_MAX_WEAVE];// 组合类型
		public int cbCenterCard[] = new int[GameConstants.XPBH_MAX_WEAVE];// 中心扑克
		public int cbCardData[][] = new int[GameConstants.XPBH_MAX_WEAVE][16]; // 实际扑克
		public int hu_xi[] = new int[GameConstants.XPBH_MAX_WEAVE];// 计算胡息

		public int cbPoint;// 组合牌的最佳点数;

		public boolean curCardEye;// 当前摸的牌是否是牌眼
		public boolean isShuangDui;// 牌眼 true双对--判断碰碰胡
		public int eyeKind;// 牌眼 组合类型
		public int eyeCenterCard;// 牌眼 中心扑克
		public int cbHuXiCount; // 胡息
	}

	
	public int is_qi_xiao_dui_yyzhz(int cards_index[]) {
		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 计算单牌
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (this._magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == get_magic_card_index(m))
						continue;

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (this._magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cards_index[get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		return GameConstants.CHR_HENAN_QI_XIAO_DUI;

	}

	/**
	 * 一挂匾判断
	 * 
	 * @param card_index
	 * @return
	 */
	public boolean check_yi_gua_bian(int card_index[]) {
		boolean flag = false;
		int card_type[] = new int[] { 0, 0, 0 };
		int card_count = 0;
		int mac_card = 0;
		for (int i = 0; i < card_index.length; i++) {
			if (card_index[i] == 0) {
				continue;
			}
			if (is_magic_card(switch_to_card_data(i))) {
				mac_card++;
				continue;
			}
			card_count += card_index[i];

			if (card_count > 2) {
				return false;
			}
			// mac_card[car_count] =
		}

		return flag;
	}

}
