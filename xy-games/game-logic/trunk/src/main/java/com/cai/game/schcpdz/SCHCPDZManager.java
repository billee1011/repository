/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.game.schcpdz;

import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;

/**
 * 
 * 跑胡子通用方法管理类
 * 		(为减少冗余代码，会把部分代码写成通用代码放在本类调用)
 * @author WalkerGeek 
 * date: 2017年8月29日 下午3:08:11 <br/>
 */
public class SCHCPDZManager {
	
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
	/**默认2000毫秒*/
	public static final int DEFAULT_2000 =2000;
	/**默认3000毫秒*/
	public static final int DEFAULT_3000 =3000;

	/**参数ID1104 */
	public static final int PARAM_ID1104 = 1104;
	/**参数ID1105 */
	public static final int PARAM_ID1105 = 1105;
	
	/**参数ID1199 红包雨配置 */
	public static final int PARAM_ID1199 = 1199;
	
	
	
	
	private static class HHManagerHolder {  
	      private static SCHCPDZManager INSTANCE = new SCHCPDZManager();
	 } 
	
	private SCHCPDZManager(){}
	
	public static final SCHCPDZManager getInstance(){
		return HHManagerHolder.INSTANCE;
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
	public int getSysParamValue(int gameId, int default_time ,int param_val, int sys_id	){
		int time = 0;
		
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(sys_id);
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
