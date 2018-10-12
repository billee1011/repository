package com.lingyu.game.service.back;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.entity.Server;
import com.lingyu.common.http.HttpManager;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.service.job.ScheduleManager;

@Service
public class BackServerManager {
	private static final Logger logger = LogManager.getLogger(BackServerManager.class);
	@Autowired
	private ScheduleManager scheduleManager;
	
	// 所有定时存根
	private List<ScheduledFuture<?>> futures = new ArrayList<>();
	
	
	public void init(){
		long a = System.currentTimeMillis();
		logger.info("向后台注册服务器信息开始");
		Server server = GameServerContext.getAppConfig().getLeaderServer();
		registerServer(server);
		logger.info("向后台注册服务器信息结束 {}ms",System.currentTimeMillis() - a);
	}
	
	/**
	 * 注册服务器信息
	 * @param server
	 * @return
	 */
	public void registerServer(Server server){
		cancelScheduledFuture();
		Map<String, Object> map = new HashMap<>();
		map.put(BackServerConstant.WORLD_ID, server.getWorldId());
		map.put(BackServerConstant.AREA_ID, server.getId());
		map.put(BackServerConstant.WORLD_NAME, server.getWorldName());
		map.put(BackServerConstant.AREA_NAME, server.getName());
		map.put(BackServerConstant.AREA_TYPE, server.getType());
		map.put(BackServerConstant.EXTERNAL_IP, server.getExternalIp());
		map.put(BackServerConstant.TCP_PORT, server.getTcpPort());
		map.put(BackServerConstant.IP, server.getInnerIp());
		map.put(BackServerConstant.PORT, server.getWebPort());
		map.put(BackServerConstant.PID, server.getPid());
		map.put(BackServerConstant.STATUS, server.getStatus());
		map.put(BackServerConstant.FOLLOWER_ID, server.getFollowerId());
		
		String backServerUrl = GameServerContext.getAppConfig().getBackUrl() + BackServerConstant.ADDAREASYS;
		String url = HttpManager.getParamString(backServerUrl, map);
		
		JSONObject js = HttpManager.get(url);
		if(!js.get("errorCode").equals("1")){
			futures.add(scheduleManager.schedule(this, "registerServer", new Object[] { server }, BackServerConstant.CRON));
		}else{
			cancelScheduledFuture();
		}
	}
	
	/**
	 * 注销服务器
	 * @param list
	 */
	public void stopServer(int status, Collection<Server> list) {
		for (Server e : list) {
			e.setStatus(status);
			this.registerServer(e);
		}
	}
	
	/**
	 * 添加定时存根
	 * 
	 * @param future
	 */
	public void addScheduledFuture(ScheduledFuture<?> future) {
		this.futures.add(future);
	}

	/**
	 * 取消定时存根
	 */
	public void cancelScheduledFuture() {
		for (ScheduledFuture<?> future : futures) {
			future.cancel(false);
		}
	}
}
