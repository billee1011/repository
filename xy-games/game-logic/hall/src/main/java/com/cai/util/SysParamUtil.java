package com.cai.util;

import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;

public class SysParamUtil {
	private static final int SYS_PARAM_INDEX_1107 = 1107; // 代开房
	private static final int SYS_PARAM_INDEX_1108 = 1108; // 托管
	/**
	 * 是否托管
	 */
	public static boolean is_auto(int game_id){
		// 判断房卡
		SysParamModel sysparam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(SYS_PARAM_INDEX_1108);
		if(sysparam!=null && sysparam.getVal1() == 1){
			return true;
		}
		return false;
	}
	
	/**
	 * 代理最大开房数
	 * @param game_id
	 */
	public static int proxy_max_create_room(int game_id){
		SysParamModel sysparam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(SYS_PARAM_INDEX_1107);
		if(sysparam!=null){
			return sysparam.getVal1();
		}
		return 0;
	}
	/**
	 * 玩家最大开房数
	 * @param game_id
	 */
	public static int player_max_create_room(int game_id){
		SysParamModel sysparam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(SYS_PARAM_INDEX_1107);
		if(sysparam!=null){
			return sysparam.getVal2();
		}
		return 0;
	}
}
