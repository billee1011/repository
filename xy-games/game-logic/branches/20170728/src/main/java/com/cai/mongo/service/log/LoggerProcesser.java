package com.cai.mongo.service.log;

import com.cai.mongo.service.log.bean.RoleLogBase;

/**
 * 
 * 
 * @author xwy
 *
 */
public class LoggerProcesser {
	/**
	 * 操作日志 操作
	 */
	public static void addOperateLog(LogType logtype, long roleId, int msgCode, GameType gameType, String content) {
		RoleLogBase roleLog = new RoleLogBase(roleId, gameType, msgCode, logtype, content);
		LogService.getInstance().addLog(logtype.getId(), roleLog);
	}

	/**
	 * 操作日志 数值
	 */
	public static void addNumLog(LogType logtype, long roleId, int code, GameType gameType, int beforeNum,
			int changeNum, int afterNum) {
		RoleLogBase playerNumberLog = new RoleLogBase(roleId, gameType, code, logtype, beforeNum, changeNum, afterNum);
		LogService.getInstance().addLog(logtype.getId(), playerNumberLog);
	}
	
	//{"timestamp":"%X{timestamp}","level":"%p","className":"%c{1}","message":"%m","pid":"%V","ip":"%I","user_id":"%X{userID}","accountID":"%X{accountID}","responseType":"%X{responseType}","requestType":"%X{requestType}","server_id":"%X{server_id}","publisher":"%X{publisher}","client_version":"%X{client_version}","logType":"%X{logType}"} - %m%n

	// account_id,proxy_id,logic_id,val1 val2 ,client_version responseType
	//	MDC.put("userID", String.valueOf(user_id));
}
