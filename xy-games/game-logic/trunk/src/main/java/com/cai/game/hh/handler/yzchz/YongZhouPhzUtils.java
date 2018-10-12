package com.cai.game.hh.handler.yzchz;

import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;
import com.cai.game.hh.HHGameLogic.AnalyseItem;

public class YongZhouPhzUtils {
	/**
	 * 判断老的胡牌分析是否够胡息
	 * 
	 * @param analyseItemArray
	 * @param table
	 * @param qiHu
	 * @return
	 */
	public static boolean hasEnoughHuXi(List<AnalyseItem> analyseItemArray, Table_YongZhou table, int qiHu, int addedHuXi) {
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		for (int w = 0; w < analyseItemArray.size(); w++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(w);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += table._logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi + addedHuXi > max_hu_xi) {
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi < qiHu)
			return false;

		return true;
	}

	/**
	 * 打印手牌和落地牌的日志信息
	 * 
	 * @param table
	 * @param cardList
	 * @param weaveItems
	 * @param weaveCount
	 */
	public static String cardInfoString(List<Integer> cardList, WeaveItem[] weaveItems, int weaveCount) {
		StringBuilder sb = new StringBuilder();
		sb.append("HandCards: " + cardList.toString() + ", ");
		for (int w = 0; w < weaveCount; w++) {
			sb.append(weaveItems[w].toSimpleString());
		}
		return sb.toString();
	}
}
