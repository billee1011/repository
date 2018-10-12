package com.lingyu.game.service.stat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.http.HttpManager;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.service.back.BackServerConstant;

@Service
public class StatManager {
	private ServerConfig config = GameServerContext.getAppConfig();
	
	
	/** 统计充值，在线，注册 
	 *  目前只有在线人数
	 * */
	public void statRealTime(int ccu) {
		Map<String, Object> map = new HashMap<>();
		map.put(BackServerConstant.WORLD_ID, config.getLeaderServer().getWorldId());
		map.put(BackServerConstant.AREA_ID, config.getLeaderServer().getId());
		map.put(BackServerConstant.PID, config.getPlatformId());
		map.put(BackServerConstant.CCU, ccu);
		map.put(BackServerConstant.ADD_TIME, new Date());
		
		String url = HttpManager.getParamString(config.getBackUrl() + BackServerConstant.STATREALTIME, map);
		HttpManager.getNoReturn(url);
	}
}
