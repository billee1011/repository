/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.utils;

import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.ClubRangeCostUtil;
import com.cai.common.util.Pair;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;

/**
 * 俱乐部比赛场最终扣豆
 *
 * @author wu_hc date: 2018年6月26日 下午4:36:17 <br/>
 */
public final class ClubMatchCostUtil {

	/**
	 * 俱乐部比赛场最终扣豆
	 * 
	 * @param game_type_index
	 * @param game_round
	 * @param clubMemberSize
	 * @param ruleMap
	 * @return
	 */
	public static int finalCost(int game_type_index, int game_round, int clubMemberSize, Map<Integer, Integer> ruleMap) {

		// 是否免费的
		int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
		int game_index = SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index);
		
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(game_index);
		if (null == sysParamModel) {
			return -1;
		}

		// 免费
		if (sysParamModel.getVal2() == 0) {
			return 0;
		}

		int[] roundGoldArray = SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(game_type_index);
		if (roundGoldArray == null) {
			return -1;
		}

		SysParamModel findParam = null;
		for (int index : roundGoldArray) {
			SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(index);// 扑克类30
			if (tempParam == null) {
				continue;
			}
			if (tempParam.getVal1() == game_round) {
				findParam = tempParam;
				break;
			}
		}
		if (findParam == null) {
			return -1;
		}

		// 是否俱乐部特殊扣费
		final boolean isClubSpecialCost = 1 == findParam.getVal4();

		// 确定扣款数量,第三个参数用于控制俱乐部开房扣卡政策
		int check_gold = isClubSpecialCost ? findParam.getVal5().intValue() : findParam.getVal2().intValue();

		// add 20180205,临时需求-俱乐部人数区间扣豆
		if (ClubRangeCostUtil.INSTANCE.isActive()) {
			long value = ClubRangeCostUtil.INSTANCE.getValue(Pair.of(game_type_index, findParam.getVal1()), clubMemberSize);
			if (value >= 0 && value != Long.MIN_VALUE) {
				check_gold = (int) value;
			}
		}
		// 免费
		if (check_gold == 0) {
			return 0;
		}

		// 语音房附加扣豆
		Integer ruleValue = ruleMap.get(GameConstants.GAME_RULE_VOICE_ROOM);
		if (null != ruleValue && ruleValue.intValue() > 0) {
			check_gold += findParam.getVal3();
		}

		return check_gold;
	}

	private ClubMatchCostUtil() {
	}
}
