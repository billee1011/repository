/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.game.mj.jiangxi.nanchang;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NANCHANG;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.MJGameLogic;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;

/**
 * 南昌麻将逻辑处理类
 * 
 * @author WalkerGeek 
 * date: 2018年9月8日 上午10:38:40 <br/>
 */
public class GameLogicNanChang extends MJGameLogic {
	
	/**
	 * @param mjType
	 */
	public GameLogicNanChang(MJType mjType) {
		super(mjType);
	}
	
	
	/**
	 * 副精计算
	 * 
	 * @param zhengJing
	 * @return
	 */
	public int getFuJing(int zhengJing) {
		int cardColor = get_card_color(zhengJing);
		int cardValue = get_card_value(zhengJing);

		if (cardColor == 3) {
			if (cardValue > 4) { // 中发白
				cardValue++;
				if (cardValue > 7) {
					cardValue = 5;
				}
			} else { // 东南西北
				cardValue++;
				if (cardValue > 4) {
					cardValue = 1;
				}
			}
		} else {
			cardValue++;
			if (cardValue > 9) {
				cardValue = 1;
			}
		}

		return (cardColor << 4) + cardValue;
	}

	
	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data(int cards_index[], int cards_data[],int jingTemp[]) {
		// 转换扑克
		int cbPosition = 0;

		for (int m = 0; m < jingTemp.length; m++) {
			if(jingTemp[m] == 0){
				continue;
			}
			for (int i = 0; i < cards_index[this.switch_to_card_index(jingTemp[m])]; i++) {
				cards_data[cbPosition++] = switch_to_card_data(this.switch_to_card_index(jingTemp[m]));
			}
		}

		boolean errorFlag = false;
		for (int i = 0; i < GameConstants.MAX_INDEX && errorFlag == false; i++) {
			if (this.switch_to_card_index(jingTemp[0]) == i || this.switch_to_card_index(jingTemp[1]) == i)
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i] && errorFlag == false; j++) {
					if (cbPosition == 14) {
						errorFlag = true;
						break;
					}
					cards_data[cbPosition++] = switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}
	
	/**
	 * 自摸类型判断
	 * 
	 * @param card_type
	 * @return
	 */
	public boolean isZiMo(int card_type) {
		if (card_type == Constants_MJ_NANCHANG.HU_CARD_TYPE_ZI_MO
				|| card_type == Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * 精钓检测 所有牌都能胡的就是精钓牌型
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param cur_card
	 * @return
	 */
	public boolean check_jing_diao(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card,
			int[] magic_cards_index, int magic_card_count,boolean needHuanYuan) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card > 0) {
			cbCardIndexTemp[switch_to_card_index(cur_card)]--;
		}
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 4) {
				count++;
				continue;
			}
			if (AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, i, magic_cards_index,
					magic_card_count,needHuanYuan)) {
				count++;
			}
		}
		if (count == GameConstants.MAX_ZI_FENG) {
			return true;
		}

		return false;
	}
	
	
	
}
