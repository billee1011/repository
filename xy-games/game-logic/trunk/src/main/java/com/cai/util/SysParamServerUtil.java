package com.cai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamServerDict;

import javolution.util.FastMap;

public class SysParamServerUtil {

    private static Logger logger = LoggerFactory.getLogger(SysParamServerUtil.class);

    public static final int SYS_PARAM_SERVER_3000 = 3000; // 控制新胡牌算法参数
    
	/**配置表参数1*/
	public static final int VAL1=1;    
	/**配置表参数2*/
	public static final int VAL2 =2;
	/**配置表参数3*/
	public static final int VAL3 =3;
	/**配置表参数4*/
	public static final int VAL4 =4;
	/**配置表参数5*/
	public static final int VAL5 =5;
	/**默认1200毫秒*/
	public static final int KEY_1200 =1200;
	/**默认2000毫秒*/
	public static final int DEFAULT_2000 =5000;
    
    
    /**
     * 麻将自动出牌时间
     */
    public static int auto_out_card_time_mj() {
        // 判断房卡
        try {
            FastMap<Integer, FastMap<Integer, SysParamModel>> map = SysParamServerDict.getInstance()
                    .getSysParamModelDictionary();
            if (map != null) {
                FastMap<Integer, SysParamModel> gameMap = map.get(3);
                if (gameMap != null) {
                    SysParamModel sysparam = gameMap.get(5000);
                    if (sysparam != null) {
                        return sysparam.getVal1();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("param error", e);
        }
        return 500;
    }

    /**
     * 麻将自动出牌时间
     */
    public static int auto_out_card_time_mj_260() {
        // 判断房卡
        try {
            FastMap<Integer, FastMap<Integer, SysParamModel>> map = SysParamServerDict.getInstance()
                    .getSysParamModelDictionary();
            if (map != null) {
                FastMap<Integer, SysParamModel> gameMap = map.get(3);
                if (gameMap != null) {
                    SysParamModel sysparam = gameMap.get(5000);
                    if (sysparam != null) {
                        return sysparam.getVal1();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("param error", e);
        }
        return 260;
    }

    /**
     * 根据game_id、id和特定的val值，从sys_param_server数据表里取值，并判断特定的val值是否为非0值，来判断是否启用新的胡牌算法
     * 
     * @param game_id
     * @param id
     * @param which_val
     * @return
     */
    public static boolean is_new_algorithm(int game_id, int id, int which_val) {
        try {
            FastMap<Integer, SysParamModel> map = SysParamServerDict.getInstance()
                    .getSysParamModelDictionaryByGameId(game_id);
            if (map != null) {
                SysParamModel sysParamModel = map.get(id);
                if (sysParamModel != null) {
                    int value = 0;
                    
                    switch (which_val) {
                    case 1:
                        value = sysParamModel.getVal1();
                        break;
                    case 2:
                        value = sysParamModel.getVal2();
                        break;
                    case 3:
                        value = sysParamModel.getVal3();
                        break;
                    case 4:
                        value = sysParamModel.getVal4();
                        break;
                    case 5:
                        value = sysParamModel.getVal5();
                        break;
                    default:
                        break;
                    }
                    
                    if (value != 0)
                        return true;
                }
            }
        } catch (Exception e) {
            logger.error("param error", e);
        }

        return false;
    }
    
    
    
	/**
	 * 获取配置表参数时间
	 * @param gameId
	 * 		      子游戏Id(table.getGame_id())
	 * @param default_time
	 * 		      默认值 (当参数表没有值时默认值)
	 * @param param_val
	 * 		     参数名称 
	 * @param sys_id
	 * 		     参数ID
	 * @return
	 */
	public static int getSysParamValueServer(int gameId, int default_time ,int param_val, int sys_id	){
		int time = 0;
		FastMap<Integer, SysParamModel> map = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(gameId);
		if(map == null){
			return default_time;
		}
		SysParamModel sysParamModel =  map.get(sys_id);
		
		if(sysParamModel == null){
			return default_time;
		}
		switch (param_val) {
		case VAL1:
			time = sysParamModel.getVal1();
			break;
		case VAL2:
			time = sysParamModel.getVal2();
			break;
		case VAL3:
			time = sysParamModel.getVal3();
			break;
		case VAL4:
			time = sysParamModel.getVal4();
			break;
		case VAL5:
			time = sysParamModel.getVal5();
			break;
		}
		
		if ( time > 0 && time < 10000) {
			return time;
		}
		return default_time;
	}
}
