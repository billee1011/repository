package com.cai.constant;

import com.cai.common.domain.ClubMatchModel;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/6 17:58
 */
public class ClubMatchFactory {

	public static ClubMatchWrap createClubMatchWrap(int openMatchType, ClubMatchModel model, Club club) {
		switch (openMatchType) {
		case ClubMatchOpenType.TIME_MODE:
			return new ClubMatchTimeWrap(model, club);
		case ClubMatchOpenType.COUNT_MODE:
			return new ClubMatchCountWrap(model, club);
		default:
			return new ClubMatchWrap(model, club);
		}
	}
}
