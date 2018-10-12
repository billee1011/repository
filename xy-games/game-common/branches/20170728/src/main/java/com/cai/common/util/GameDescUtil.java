/**
 * 
 */
package com.cai.common.util;

import com.cai.common.constant.GameConstants;

/**
 * @author xwy
 *
 */
public class GameDescUtil {

	public static String get_henanxc_game_des(int _game_type_index, int _game_rule_index, int baseScore,
			int max_times) {
		return get_game_des_henan_xc(_game_type_index, _game_rule_index, baseScore, max_times);
	}

	public static String getGameDesc(int _game_type_index, int _game_rule_index) {
		String des = "";

		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_XTHH)) {
			return get_game_des_xthh(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_ZHUZHOU)) {
			return get_game_des_zhuzhou(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_AY)) {
			return get_game_des_ay(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_XY)) {
			return get_game_des_xy(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_LZ)) {
			return get_game_des_lz(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_KF)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_XX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_PDS)) {
			return get_game_des_henan(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_HZ)) {
			return get_game_des_hnhz(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_CS)) {
			return get_game_des_cs(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_LH)) {
			return get_game_des_henan_lh(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_FLS_LX_CG)) {
			return get_game_des_lxcg(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_FLS_CS_LX)) {
			return get_game_des_cs_lx(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_FLS_LX) || is_mj_type(_game_type_index, GameConstants.GAME_TYPE_FLS_LX_DP)) {
			return get_game_des_fls(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			return get_game_des_fls_twenty(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HH_YX)) {
			return get_game_des_hh(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_PHZ_YX)) {
			return get_game_des_hh(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_PHZ_CHD)) {
			return get_game_des_phz_chd(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_LHQ_HD)) {
			return get_game_des_lhq(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_THK_HY)) {
			return get_game_des_lhq(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_WMQ_AX)) {
			return get_game_des_lhq(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_PHZ_XT)) {
			return get_game_des_phz_xt(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_FPHZ_YX)) {
			return get_game_des_fphz(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SEVER_OX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_LZOX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SZOX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_ZYQOX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MSZOX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MFZOX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_TBOX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SEVER_OX_LX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_LZOX_LX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SZOX_LX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_ZYQOX_LX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MSZOX_LX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MFZOX_LX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_TBOX_LX)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_BTZ_YY)) {
			return get_game_des_ox(_game_type_index, _game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HJK)) {
			return get_game_des_hjk(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HJK)) {
			return get_game_des_hjk(_game_rule_index);
		}else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_PDK_JD)){
			return get_game_des_pdk(_game_rule_index,_game_rule_index);
		}else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_PDK_FP)){
			return get_game_des_pdk(_game_rule_index,_game_rule_index);
		}else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_PDK_LZ)){
			return get_game_des_pdk(_game_rule_index,_game_rule_index);
		}else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_PDK_SW)){
			return get_game_des_pdk(_game_rule_index,_game_rule_index);
		}else if(is_mj_type(_game_type_index, GameConstants.GAME_TYPE_DDZ_JD)){
			return get_game_des_ddz_jd(_game_rule_index);
		} else if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_CHENZHOU)) {
			return get_game_des_hn_cz(_game_rule_index);
		}

		if ((_game_type_index == GameConstants.GAME_TYPE_ZZ) || is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)) { // liuyan
																								// 2017/7/8
																								// 河南转转
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZIMOHU)) {

				des += "自摸胡";
			} else {
				if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_HZ)) {
					des += "可炮胡";
				} else {
					des += "可炮胡(可抢杠胡)";
				}
			}
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_THREE)) {
				des += "三人场";
			}
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_QIDUI)) {

				des += "\n" + "可胡七对";
			}

			// if (is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
			// des += "\n" + "红中癞子";
			// }

			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_JIANPAOHU)) {
				des += "\n" + "强制胡牌";
			}
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_GANG_HU)) {
				des += "\n" + "抢杠胡";
			}
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)) {
				des += "\n" + "庄闲";
			}
		} else if (_game_type_index == GameConstants.GAME_TYPE_CS) {

		} else if (_game_type_index == GameConstants.GAME_TYPE_ZHUZHOU) {
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_SCORE_ADD)) {
				des += "\n" + "加法记分";
			} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_SCORE_MUTIP)) {
				des += "\n" + "乘法记分";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:1个";
			} else {
				des += "抓鸟:1个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:2个";
			} else {
				des += "抓鸟:2个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:4个";
			} else {
				des += "抓鸟:4个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:6个";
			} else {
				des += "抓鸟:6个";
			}
		} else {
			if (des.length() > 3) {
				des += "\n" + "不抓鸟";
			} else {
				des += "不抓鸟";
			}
		}
		return des;
	}

	/**
	 * 洛阳杠次des
	 * 
	 * @param _game_rule_index
	 * @return
	 */
	public static String get_game_des_lygc(int _game_rule_index, Integer gang_di_feng, Integer ci_di_feng,
			Integer zimo_di_feng) {
		String des = "";
		boolean has_first = false;
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_THREE)) {
			des += "三人场";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			if (has_first) {
				des += " 点炮胡";
			} else {
				des += "点炮胡";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			if (has_first) {
				des += " 带风";
			} else {
				des += "带风";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_LCI)) {
			if (has_first) {
				des += " 软次";
			} else {
				des += "软次";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_YCI)) {
			if (has_first) {
				des += " 硬次";
			} else {
				des += "硬次";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_PICI)) {
			if (has_first) {
				des += " 皮次";
			} else {
				des += "皮次";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_BAOCI)) {
			if (has_first) {
				des += " 包次";
			} else {
				des += "包次";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_JIA_DI)) {
			if (has_first) {
				des += " 庄家翻倍";
			} else {
				des += "庄家翻倍";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_BUCIFENG)) {
			if (has_first) {
				des += " 不次风";
			} else {
				des += "不次风";
			}
			has_first = true;
		}

		if (gang_di_feng != 0) {
			des += " 杠底:" + gang_di_feng;
		}

		if (ci_di_feng != 0) {
			des += " 次底:" + ci_di_feng;
		}

		if (zimo_di_feng != 0) {
			des += " 自摸底:" + zimo_di_feng;
		}

		return des;
	}

	private static String get_game_des_hjk(int _game_rule_index) {
		String des = "";
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_EVERYONE_ROBOT_BANKER)) {
			des += "每局抢庄" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_WUXILONG_HUAN_BANKER)) {
			des += "五小龙换庄" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LUN_BNAKER)) {
			des += "轮庄" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DING_BNAKER)) {
			des += "固定庄" + "\n";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_FIRST_ROBOT_BANKER)) {
			des += "首局抢庄后五小龙换庄" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_NO_LEI_ZHU)) {
			des += "不能垒注" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_MAX_TWENTY_FOUR)) {
			des += "24分封顶" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_MAX_FOURTY_EIGHT)) {
			des += "48分封顶" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_MID_JOIN)) {
			des += "中途可进" + "\n";
		}

		return des;
	}

	private static String get_game_des_pdk(int _game_rule_index, int game_rule_index) {

		String des = "";
		if (is_mj_type(game_rule_index, GameConstants.GAME_TYPE_PDK_JD)) {
			des += "经典玩法";
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
				des += "扎鸟";
			}
			des += " ";
		}
		if (is_mj_type(game_rule_index, GameConstants.GAME_TYPE_PDK_LZ)) {
			des += "癞子玩法";
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
				des += "扎鸟";
			}
			des += " ";
		}
		if (is_mj_type(game_rule_index, GameConstants.GAME_TYPE_PDK_FP)) {
			des += "四人玩法";
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
				des += "扎鸟";
			}
			des += " ";
		}
		if(is_mj_type(game_rule_index, GameConstants.GAME_TYPE_PDK_SW)){
			des +=  "十五张玩法" ;
			if(has_rule(_game_rule_index, GameConstants.GAME_RULE_HONGTAO10_ZANIAO)){
				des +=  "扎鸟" ;
			}
			des += " ";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_THREE_PLAY)) {
			des += "3人玩" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_TWO_PLAY)) {
			des += "2人玩" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_FOUR_PLAY)) {
			des += "4人玩" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DISPLAY_CARD)) {
			des += "显示牌数" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_NO_DISPLAY_CARD)) {
			des += "不显示牌数" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_BI_XU_GUAN)) {
			des += "必须管" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_KE_BU_YAO)) {
			des += "可不要" + " ";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_FANG_ZOU_BAOPEI)) {
			des += "放走包赔" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
			des += "可四带三" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_KE_FAN_DE)) {
			des += "可反的" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_BOOM)) {
			des += "有炸弹" + " ";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_FIFTEEN_COUNT)) {
			des += "15张" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_SIXTEEN_COUNT)) {
			des += "16张" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_YIFU_COUNT)) {
			des += "一副牌" + " ";
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_TWO_PLAY)) {
				des += "首局最小先出牌" + " ";
			}else{
				if (has_rule(_game_rule_index, GameConstants.GAME_RULE_SHOU_JU_HEITAO_SAN)) {
					des += "首局先出黑桃3(必出)" + " ";
				} else {
					des += "首局黑桃3先出" + " ";
				}
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIANGFU_COUNT)) {
			des += "两副牌" + " ";
		}

		return des;
	}

	private static String get_game_des_ox(int _game_type_index, int _game_rule_index) {
		String des = "";
		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_TBOX_LX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_TBOX)) {
			des += "通比牛牛" + "\n";
		}
		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SEVER_OX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SEVER_OX_LX)) {
			des += "房主上庄" + "\n";
		}

		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_LZOX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_LZOX_LX)) {
			des += "轮流上庄" + "\n";
		}
		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SZOX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_SZOX_LX)) {
			des += "牛牛上庄" + "\n";
		}
		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_ZYQOX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_ZYQOX_LX)) {
			des += "自由抢庄" + "\n";
		}
		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MFZOX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MFZOX_LX)) {
			des += "看四张抢庄" + "\n";
		}
		if (is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MSZOX)
				|| is_mj_type(_game_type_index, GameConstants.GAME_TYPE_MSZOX_LX)) {
			des += "明三张抢庄" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_OX_WUHUANIU)) {
			des += "五花牛" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_OX_BOOM)) {
			des += "炸弹" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_OX_WUXIAONIU)) {
			des += "五小牛" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE)) {
			des += "牛牛3倍、牛九2倍、牛八2倍、 其余1倍" + "\n";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO)) {
			des += "牛牛4倍、牛九3倍、牛八2倍、牛七2倍 、 其余1倍" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_PlAYER_TUI_ZHU)) {
			des += "闲家可推注" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_START_FORBID_JOIN)) {
			des += "游戏开始后禁止加入" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUA_SE_COMPARE)) {
			des += "按花色比牌" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_EQUAL_PING)) {
			des += "打和算平" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_ZHUANG_WIN)) {
			des += "庄家算赢" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_MAX_ONE_TIMES)) {
			des += "最大抢庄：1倍" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_MAX_TWO_TIMES)) {
			des += "最大抢庄：2倍" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_MAX_THREE_TIMES)) {
			des += "最大抢庄：3倍" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_MAX_FOUR_TIMES)) {
			des += "最大抢庄：4倍" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_ONE_TWO)) {
			des += "下注1/2" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_TWO_FOUR)) {
			des += "下注2/4" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_FOUR_EIGHT)) {
			des += "下注4/8" + "\n";
		}
		return des;
	}

	private static String get_game_des_phz_chd(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_SHUA_HOU)) {
			des += "耍猴" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_HUANG_FAN)) {
			des += "黄番" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_TUAN_YUAN)) {
			des += "团圆" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_HANG_HANG_XI)) {
			des += "行行息" + " ";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
			des += "亮牌" + "\n";
		} else {
			des += "不亮牌" + "\n";
		}

		return des;
	}

	private static String get_game_des_lhq(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)) {
			des += "吃：一五十" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_ONE)) {
			des += "底分：一分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_TWO)) {
			des += "底分：二分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_THREE)) {
			des += "底分：三分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_AN_WEI)) {
			des += "暗偎" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_MING_WEI)) {
			des += "明偎" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
			des += "一囤一分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
			des += "一息一分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_QIANG_ZHI_HU_PAI)) {
			des += "强制胡牌" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HONG_HEI_DIAN)) {
			des += "红黑点" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HAI_DI_LAO_YUE)) {
			des += "海底捞月" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_NO_XING)) {
			des += "不带醒" + "\n";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_FAN_XING)) {
			des += "翻醒" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_SUI_XING)) {
			des += "随醒" + "\n";
		}

		return des;
	}

	private static String get_game_des_phz_xt(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)) {
			des += "吃：一五十" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)) {
			des += "胡息 18硬息" + "\n";
		} else {
			des += "胡息 15胡息 " + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_ONE)) {
			des += "底分：一分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_TWO)) {
			des += "底分：二分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_THREE)) {
			des += "底分：三分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_AN_WEI)) {
			des += "暗偎" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_MING_WEI)) {
			des += "明偎" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_NO_MING_TANG)) {
			des += "无名堂" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_JI_BENMING_TANG)) {
			des += "基本名堂" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_ZI_MO_ADD_THREE)) {
			des += "自摸加3胡息" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_ALL_MING_TANG)) {
			des += "全名堂" + "\n";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_SI_QI_ZHANG_HONG_HU)) {
			des += "四、七张红胡" + "\n";
		}

		return des;
	}

	private static String get_game_des_hh(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)) {
			des += "吃：一五十" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)) {
			des += "胡息 18硬息" + "\n";
		} else {
			des += "胡息 15胡息 " + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_ONE)) {
			des += "底分：一分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_TWO)) {
			des += "底分：二分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_THREE)) {
			des += "底分：三分" + " ";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
			des += "亮牌" + "\n";
		} else {
			des += "不亮牌" + "\n";
		}

		return des;
	}

	private static String get_game_des_fphz(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)) {
			des += "吃：一五十" + "\n";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_ONE)) {
			des += "底分：一分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_TWO)) {
			des += "底分：二分" + " ";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_FEN_SELECT_THREE)) {
			des += "底分：三分" + " ";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
			des += "亮牌" + "\n";
		} else {
			des += "不亮牌" + "\n";
		}

		return des;
	}

	private static String get_game_des_lxcg(int _game_rule_index) {
		String des = "";
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {
			des += "轮庄";
		} else {
			des += "胡牌者庄";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			des += " 飘分";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
			if (des.length() == 0) {
				des += "允许相同ip进入";
			} else {
				des += "\n" + "允许相同ip进入";
			}
		} else {
			des += "\n" + "不允许相同ip进入";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_JIANPAOHU)) {
			if (des.length() == 0) {
				des += "强制胡牌";
			} else {
				des += "\n" + "强制胡牌";
			}
		}

		return des;
	}

	private static String get_game_des_fls(int _game_rule_index) {
		String des = "";
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {
			des += "轮庄";
		} else {
			des += "胡牌者庄";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			des += " 飘分";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
			if (des.length() == 0) {
				des += "允许相同ip进入";
			} else {
				des += "\n" + "允许相同ip进入";
			}
		} else {
			des += "\n" + "不允许相同ip进入";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_JIANPAOHU)) {
			if (des.length() == 0) {
				des += "强制胡牌";
			} else {
				des += "\n" + "强制胡牌";
			}
		}

		return des;
	}

	private static String get_game_des_fls_twenty(int _game_rule_index) {
		String des = "";
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {
			des += "轮庄";
		} else {
			des += "胡牌者庄";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_IS_TWO)) {
			des += " 2底分";
		} else {
			des += " 1底分";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			des += " 飘分";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
			if (des.length() == 0) {
				des += "允许相同ip进入";
			} else {
				des += "\n" + "允许相同ip进入";
			}
		} else {
			des += "\n" + "不允许相同ip进入";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_JIANPAOHU)) {
			if (des.length() == 0) {
				des += "强制胡牌";
			} else {
				des += "\n" + "强制胡牌";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIXIANG_FLS_TONGPAO)) {
			if (des.length() == 0) {
				des += "通炮";
			} else {
				des += "通炮";
			}
		}

		return des;
	}

	private static String get_game_des_xthh(int _game_rule_index) {
		String des = "";
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HEBEI_DI_FEN_05)) {

			des += "底分:0.5";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HEBEI_DI_FEN_10)) {
			des += "底分:1";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HEBEI_DI_FEN_20)) {
			des += "底分:2";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HEBEI_PIAO_LAI_YOU_JIANG)) {
			des += " 飘赖有奖";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HEBEI_GAN_DENG_YAN)) {

			des += "\n" + "干瞪眼";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {
			des += "\n" + "一赖到底";
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HEBEI_TU_HAO_BI_GANG)) {
				des += " " + "土豪必杠";
			}
		}

		return des;
	}

	private static String get_game_des_ay(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			des += "带风";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_XIAPAO)) {
			if (des.length() == 0) {
				des += "庄家可下跑";
			} else {
				des += "\n" + "庄家可下跑";
			}
		}
		return des;
	}

	private static String get_game_des_xy(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_THREE)) {
			des += "三人场";
		}
		if (!has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			if (des.length() == 0) {
				des += "自摸胡";
			} else {
				des += "\n" + "自摸胡";
			}
		} else {
			if (des.length() == 0) {
				des += "点炮胡";
			} else {
				des += "\n" + "点炮胡";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			if (des.length() == 0) {
				des += "带跑";
			} else {
				des += "\n" + "带跑";
			}
		} else {
			if (des.length() == 0) {
				des += "不带跑";
			} else {
				des += "\n" + "不带跑";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
			if (des.length() == 0) {
				des += "推倒胡";
			} else {
				des += "\n" + "推倒胡";
			}
		} else {
			if (des.length() == 0) {
				des += "七公嘴";
			} else {
				des += "\n" + "七公嘴";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
			if (des.length() == 0) {
				des += " 杠上开花加倍";
			} else {
				des += "杠上开花加倍";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
			if (des.length() == 0) {
				des += " 七对加倍";
			} else {
				des += "七对加倍";
			}
		}
		return des;
	}

	private static String get_game_des_smx(int _game_rule_index) {
		String des = "";

		if (!has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			if (des.length() == 0) {
				des += "自摸胡";
			} else {
				des += "\n" + "自摸胡";
			}
		} else {
			if (des.length() == 0) {
				des += "点炮胡";
			} else {
				des += "\n" + "点炮胡";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			if (des.length() == 0) {
				des += "带跑";
			} else {
				des += "\n" + "带跑";
			}
		} else {
			if (des.length() == 0) {
				des += "不带跑";
			} else {
				des += "\n" + "不带跑";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
			if (des.length() == 0) {
				des += "推倒胡";
			} else {
				des += "\n" + "推倒胡";
			}
		} else {
			if (des.length() == 0) {
				des += "七公嘴";
			} else {
				des += "\n" + "七公嘴";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
			if (des.length() == 0) {
				des += " 杠上开花加倍";
			} else {
				des += "杠上开花加倍";
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
			if (des.length() == 0) {
				des += " 七对加倍";
			} else {
				des += "七对加倍";
			}
		}
		return des;
	}

	private static String get_game_des_lz(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			des += "带风";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			if (des.length() == 0) {
				des += "带混";
			} else {
				des += "\n" + "带混";
			}
		}
		return des;
	}

	private static String get_game_des_hnhz(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			des += "带风";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			des += " 带跑";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_ZIMOHU)) {

			des += "自摸胡";
		} else {
			des += "可炮胡";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_QIANG_GANG_HU)) {
			if (des.length() > 3) {
				des += "\n" + "抢杠胡";
			} else {
				des += "抢杠胡";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_ZHANIAO2)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:2个";
			} else {
				des += "抓鸟:2个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_ZHANIAO4)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:4个";
			} else {
				des += "抓鸟:4个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_ZHANIAO6)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:6个";
			} else {
				des += "抓鸟:6个";
			}
		} else {
			if (des.length() > 3) {
				des += "\n" + "不抓鸟";
			} else {
				des += "不抓鸟";
			}
		}
		return des;
	}

	private static String get_game_des_henan(int game_type_index, int _game_rule_index) {
		String des = "";
		boolean has_first = false;
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_THREE)) {
			des += "三人场";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_JIA_DI)) {
			des += "庄家加底";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
			if (has_first) {
				des += " 杠上开花加倍";
			} else {
				des += "杠上开花加倍";
			}
			has_first = true;
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
			if (has_first) {
				des += " 七对加倍";
			} else {
				des += "七对加倍";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAI_HUN)) {

			if ((is_mj_type(game_type_index, GameConstants.GAME_TYPE_HENAN_NY))) {
				if (has_first) {
					des += " 带金";
				} else {
					des += "带金";
				}
				has_first = true;
			} else {
				if (has_first) {
					des += " 带混";
				} else {
					des += "带混";
				}
				has_first = true;
			}

		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			if (has_first) {
				des += " 带风";
			} else {
				des += "带风";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			if (has_first) {
				des += " 点炮胡";
			} else {
				des += "点炮胡";
			}
			has_first = true;
		} else {
			if (has_first) {
				des += " 自摸胡";
			} else {
				des += "自摸胡";
			}
			has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			// 南阳麻将信息特殊处理
			if ((is_mj_type(game_type_index, GameConstants.GAME_TYPE_HENAN_NY))) {
				if (has_first) {
					des += " 带飘";
				} else {
					des += "带飘";
				}
			} else {
				if (has_first) {
					des += " 带跑";
				} else {
					des += "带跑";
				}
			}

			has_first = true;
		} else {
			// if (has_first) {
			// des += " 不带跑";
			// } else {
			// des += "不带跑";
			// }
			// has_first = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_GANG_PAO)) {

			if ((is_mj_type(game_type_index, GameConstants.GAME_TYPE_HENAN_NY))) {
				if (has_first) {
					des += " 杠飘";
				} else {
					des += "杠飘";
				}
				has_first = true;
			} else {
				if (has_first) {
					des += " 杠跑";
				} else {
					des += "杠跑";
				}
			}
		}

		if ((is_mj_type(game_type_index, GameConstants.GAME_TYPE_HENAN_NY))) {
			if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HZBHG)) {
				if (has_first) {
					des += " 荒庄不荒杠";
				} else {
					des += "荒庄不荒杠";
				}
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_258)) {
			if (has_first) {
				des += " 258做将";
			} else {
				des += "258做将";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (has_first) {
				des += " 缺门";
			} else {
				des += "缺门";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HAOQI)) {
			if (has_first) {
				des += " 豪七四倍";
			} else {
				des += "豪七四倍";
			}
		}

		return des;
	}

	private static String get_game_des_henan_xc(int game_type_index, int _game_rule_index, int baseScore,
			int max_times) {
		StringBuffer des = new StringBuffer();
		des.append("封顶").append(max_times).append("番 ").append(" 底分").append(baseScore).append("分");
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_ZIMOHU)) {
			des.append(" 自摸胡");
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			des.append(" 可炮胡");
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HTG)) {
			des.append(" 回头杠");
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			des.append(" 带混");
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_SHUAIHUN)) {
			des.append(" 甩混");
		}

		return des.toString();
	}

	public static String get_game_des_zhuzhou(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_QIANGGANGHU)) {
			des += "可炮胡";
		} else {
			des += "自摸胡";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_JIANPAOHU)) {
			des += "\n" + "强制胡牌";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_SCORE_ADD)) {
			des += "\n" + "加法记分";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_SCORE_MUTIP)) {
			des += "\n" + "乘法记分";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:1个";
			} else {
				des += "抓鸟:1个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:2个";
			} else {
				des += "抓鸟:2个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:4个";
			} else {
				des += "抓鸟:4个";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			if (des.length() > 3) {
				des += "\n" + "抓鸟:6个";
			} else {
				des += "抓鸟:6个";
			}
		} else {
			if (des.length() > 3) {
				des += "\n" + "不抓鸟";
			} else {
				des += "不抓鸟";
			}
		}
		return des;
	}

	/**
	 * 河南周口麻将 -- 根据游戏规则获取游戏玩法描述
	 * 
	 * @param _game_rule_index
	 * @return
	 */
	public static String get_game_des_he_nan_zhou_kou(int[] _game_rule_index) {
		// for (int tmpInt : _game_rule_index) {
		// char[] chs = new char[Integer.SIZE];
		// for (int i = 0; i < Integer.SIZE; i++)
		// {
		// chs[Integer.SIZE - 1 - i] = (char) (((tmpInt >> i) & 1) + '0');
		// System.out.println(i+"："+chs[Integer.SIZE - 1 - i]);
		// }
		// System.out.println(new String(chs));
		// }

		StringBuilder gameDesc = new StringBuilder("");
		boolean hasFirst = false;
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			gameDesc.append("点炮胡");
			hasFirst = true;
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_ZIMOHU)) {
			if (hasFirst) {
				gameDesc.append("\n自摸胡");
			} else {
				gameDesc.append("自摸胡");
				hasFirst = true;
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (hasFirst) {
				gameDesc.append("\n断门");
			} else {
				gameDesc.append("断门");
				hasFirst = true;
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_258)) {
			if (hasFirst) {
				gameDesc.append("\n258将");
			} else {
				gameDesc.append("258将");
				hasFirst = true;
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if (hasFirst) {
				gameDesc.append("\n红中癞子");
			} else {
				gameDesc.append("红中癞子");
				hasFirst = true;
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			if (hasFirst) {
				gameDesc.append("\n带风牌");
			} else {
				gameDesc.append("带风牌");
				hasFirst = true;
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) {
			if (hasFirst) {
				gameDesc.append("\n出风报听");
			} else {
				gameDesc.append("出风报听");
				hasFirst = true;
			}
		}
		return gameDesc.toString();
	}

	/**
	 * 河南漯河麻将 -- 根据游戏规则获取游戏玩法描述
	 * 
	 * @param _game_rule_index
	 * @return
	 */
	public static String get_game_des_henan_lh(int _game_rule_index) {
		// for (int tmpInt : _game_rule_index) {
		// char[] chs = new char[Integer.SIZE];
		// for (int i = 0; i < Integer.SIZE; i++)
		// {
		// chs[Integer.SIZE - 1 - i] = (char) (((tmpInt >> i) & 1) + '0');
		// System.out.println(i+"："+chs[Integer.SIZE - 1 - i]);
		// }
		// System.out.println(new String(chs));
		// }

		StringBuilder gameDesc = new StringBuilder("");
		boolean hasFirst = false;
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			gameDesc.append("点炮胡");
			hasFirst = true;
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_ZIMOHU)) {
			if (hasFirst) {
				gameDesc.append("\n自摸胡");
			} else {
				gameDesc.append("自摸胡");
				hasFirst = true;
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if (hasFirst) {
				gameDesc.append("\n红中癞子");
			} else {
				gameDesc.append("红中癞子");
				hasFirst = true;
			}
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			if (hasFirst) {
				gameDesc.append("\n带风牌");
			} else {
				gameDesc.append("带风牌");
				hasFirst = true;
			}
		}
		return gameDesc.toString();
	}

	public static String get_game_des_cs_lx(int _game_rule_index) {
		String des = "";

		// if (has_rule(_game_rule_index,
		// GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO1)) {
		// des += "坐飘1分";
		// } else if (has_rule(_game_rule_index,
		// GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO2)) {
		// des += "坐飘2分";
		// } else if (has_rule(_game_rule_index,
		// GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO3)) {
		// des += "坐飘3分";
		// }
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_THREE)) {
			des += "三人场";
		} else {
			des += "四人场";
		}

		boolean hasNiao = false;
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			if (des.length() > 0) {
				des += "\n定鸟加法2只";
			} else {
				des += "定鸟加法2只";
			}
			hasNiao = true;
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			if (des.length() > 0) {
				des += "\n定鸟加法4只";
			} else {
				des += "定鸟加法4只";
			}
			hasNiao = true;
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			if (des.length() > 0) {
				des += "\n定鸟加法6只";
			} else {
				des += "定鸟加法6只";
			}
			hasNiao = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1)) {
			if (des.length() > 0) {
				des += "\n" + "定鸟乘法1只";
			} else {
				des += "定鸟乘法1只";
			}
			hasNiao = true;
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2)) {
			if (des.length() > 0) {
				des += "\n" + "定鸟乘法2只";
			} else {
				des += "定鸟乘法2只";
			}
			hasNiao = true;
		}

		if (!has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_FLS_IP)) {
			if (des.length() == 0) {
				des += "允许相同ip进入";
			} else {
				des += "\n" + "允许相同ip进入";
			}
		} else {
			des += "\n" + "不允许相同ip进入";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ONE_GANG)) {
			if (des.length() == 0) {
				des += "一杠到底";
			} else {
				des += "\n" + "一杠到底";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_SCORE_TOP)) {
			if (des.length() == 0) {
				des += "60分封顶   ";
			} else {
				des += "\n" + "60分封顶   ";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ALL_OPEN)) {
			if (des.length() == 0) {
				des += "全开放 ";
			} else {
				des += "\n" + "全开放 ";
			}
		}

		// if (has_rule(_game_rule_index,
		// GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
		// if (des.length() > 0) {
		// des += "\n" + "飞鸟2只(每只2分)";
		// } else {
		// des += "飞鸟2只(每只2分)";
		// }
		//
		// } else if (has_rule(_game_rule_index,
		// GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO4)) {
		// if (des.length() > 0) {
		// des += "\n" + "飞鸟4只(每只2分)";
		// } else {
		// des += "飞鸟4只(每只2分)";
		// }
		// }

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			des += "飘分";
		}
		return des;
	}

	public static String get_game_des_cs(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO1)) {
			des += "坐飘1分";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO2)) {
			des += "坐飘2分";
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO3)) {
			des += "坐飘3分";
		}

		boolean hasNiao = false;
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			if (des.length() > 0) {
				des += " 定鸟加法2只";
			} else {
				des += "定鸟加法2只";
			}
			hasNiao = true;
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			if (des.length() > 0) {
				des += " 定鸟加法4只";
			} else {
				des += "定鸟加法4只";
			}
			hasNiao = true;
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			if (des.length() > 0) {
				des += " 定鸟加法6只";
			} else {
				des += "定鸟加法6只";
			}
			hasNiao = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1)) {
			if (des.length() > 0) {
				des += "\n" + "定鸟乘法1只";
			} else {
				des += "定鸟乘法1只";
			}
			hasNiao = true;
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2)) {
			if (des.length() > 0) {
				des += "\n" + "定鸟乘法2只";
			} else {
				des += "定鸟乘法2只";
			}
			hasNiao = true;
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
			if (des.length() > 0) {
				des += "\n" + "飞鸟2只(每只2分)";
			} else {
				des += "飞鸟2只(每只2分)";
			}

		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO4)) {
			if (des.length() > 0) {
				des += "\n" + "飞鸟4只(每只2分)";
			} else {
				des += "飞鸟4只(每只2分)";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			des += " 飘分";
		}
		return des;
	}

	/**
	 * 经典斗地主 -- 根据游戏规则获取游戏玩法描述
	 * 
	 * @param _game_rule_index
	 * @return
	 */
	public static String get_game_des_ddz_jd(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_THREE_PLAY_DDZ)) {
			des += "三人场";
		} else {
			des += "二人场";
		}
		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_JIAO_DI_ZHU)) {
			if (des.length() > 0) {
				des += "\n" + "叫地主";
			} else {
				des += "叫地主";
			}
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_JIAO_FEN)) {
			if (des.length() > 0) {
				des += "\n" + "叫分";
			} else {
				des += "叫分";
			}
		} else {
			if (des.length() > 0) {
				des += "\n" + "抢地主";
			} else {
				des += "抢地主";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIMIT_THREE_BOMB)) {
			if (des.length() > 0) {
				des += "\n" + "3炸";
			} else {
				des += "3炸";
			}
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_LIMIT_FOUR_BOMB)) {
			if (des.length() > 0) {
				des += "\n" + "4炸";
			} else {
				des += "4炸";
			}
		} else {
			if (des.length() > 0) {
				des += "\n" + "5炸";
			} else {
				des += "5炸";
			}
		}
		return des;
	}

	/**
	 * 湖南郴州麻将玩法描述
	 * 
	 * @param _game_rule_index
	 * @return
	 */
	public static String get_game_des_hn_cz(int _game_rule_index) {
		String des = "";

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZIMOHU)) {

			des += "\n" + "自摸胡";
		} else {
			des += "\n" + "可炮胡(可抢杠胡)";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_QIDUI)) {

			des += "\n" + "可胡七对";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			des += "\n" + "红中";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			des += "\n" + "飘";
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			if (des.length() > 0) {
				des += "\n" + "抓鸟:2个";
			} else {
				des += "抓鸟:2个";
			}
		} else if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			if (des.length() > 0) {
				des += "\n" + "抓鸟:4个";
			} else {
				des += "抓鸟:4个";
			}

		} else {
			if (des.length() > 0) {
				des += "\n" + "抓鸟:6个";
			} else {
				des += "抓鸟:6个";
			}
		}

		if (has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_JINNIAO)) {
			des += "\n" + "金鸟";
		}

		return des;
	}

	public static boolean is_mj_type(int _game_type_index, int type) {
		return _game_type_index == type;
	}

	// 支持 0~31
	public static boolean has_rule(int _game_rule_index, int cbRule) {
		assert (cbRule < 32);
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	// 支持32位之后的
	public static boolean has_rule(int[] rules, int cbRule) {
		if (rules == null)// error
			return false;

		if (cbRule < 32) {
			return has_rule(rules[0], cbRule);
		}

		int mod = cbRule % 32;
		int index = (cbRule - mod) / 32;
		if (index >= rules.length)// error
			return false;
		return has_rule(rules[index], mod);
	}
}
