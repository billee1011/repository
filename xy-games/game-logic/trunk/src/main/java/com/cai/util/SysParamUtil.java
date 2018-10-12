package com.cai.util;

import java.util.Date;

import com.cai.common.domain.SysParamModel;
import com.cai.common.util.MyDateUtil;
import com.cai.dictionary.SysParamDict;

public class SysParamUtil {
	private static final int SYS_PARAM_INDEX_1107 = 1107; // 代开房
	private static final int SYS_PARAM_INDEX_1108 = 1108; // 托管

	private static final int SYS_PARAM_INDEX_1000 = 1000; // 托管

	/**
	 * 是否托管
	 */
	public static boolean is_auto(int game_id) {
		// 判断房卡
		SysParamModel sysparam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
				.get(SYS_PARAM_INDEX_1108);
		if (sysparam != null && sysparam.getVal1() == 1) {
			return true;
		}
		return false;
	}

	/**
	 * 代理最大开房数
	 * 
	 * @param game_id
	 */
	public static int proxy_max_create_room(int game_id) {
		SysParamModel sysparam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
				.get(SYS_PARAM_INDEX_1107);
		if (sysparam != null) {
			return sysparam.getVal1();
		}
		return 0;
	}

	/**
	 * 玩家最大开房数
	 * 
	 * @param game_id
	 */
	public static int player_max_create_room(int game_id) {
		SysParamModel sysparam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
				.get(SYS_PARAM_INDEX_1107);
		if (sysparam != null) {
			return sysparam.getVal2();
		}
		return 0;
	}

	/**
	 * 是否审核
	 */
	public static boolean is_shen_he(Date createTime) {
		// 判断房卡
		SysParamModel sysparam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
				.get(SYS_PARAM_INDEX_1000);
		if (sysparam != null && sysparam.getVal4() == 1 && !sysparam.getStr1().isEmpty()) {
			try {
				Date date = MyDateUtil.getZeroDate(sysparam.getStr1());
				if (createTime.getTime() < date.getTime())
					return false;
			} catch (Exception e) {

			}
			return true;
		}
		return false;
	}
}
