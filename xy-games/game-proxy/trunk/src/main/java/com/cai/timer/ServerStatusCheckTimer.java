/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.IPGroupModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.core.SystemConfig;
import com.cai.dictionary.IPGroupDict;
import com.cai.domain.IpFirewallModel;
import com.cai.service.FirewallServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.TempSmsService;

import javolution.util.FastMap;

public class ServerStatusCheckTimer  extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(ServerStatusCheckTimer.class);
	
	private long ddosTime = 0L;
	
	@Override
	public void run() {
		PerformanceTimer timer = new PerformanceTimer();
		int blackSize = getBlackSize();
		
		if(blackSize>10) {
			FirewallServiceImpl.getInstance().isDebugInfo=false;//停止打印日志
			ddosTime = System.currentTimeMillis();
			
			modifyIpGroup(true);
			
			String msg = SystemConfig.localip+SystemConfig.proxy_index+"服务器疑遭受攻击,当前黑名单ip数量"+blackSize;
			sendMsg(msg);
			
			MongoDBServiceImpl.getInstance().systemLog(ELogType.blackIpNumber, msg,timer.get(), null,
					ESysLogLevelType.NONE);
			
		}
		
		if(ddosTime>0) {//解除黑名单
			boolean flag = System.currentTimeMillis() - ddosTime>600000?true:false;
			if(flag) {
				FirewallServiceImpl.getInstance().isDebugInfo=flag;
				ddosTime=0;
//				modifyIpGroup(false);还是手动调整的好
				
			}
		}
		
	}
	
	
	
	private void sendMsg(String msg) {
		Set<String> phoneSet = new HashSet<String>();
		phoneSet.add("13670139534");
		TempSmsService.batchSendMsg(phoneSet,  msg);
	}
	/**
	 * 根据是否攻击 自动调整线路--只改各自的内存
	 * @param ddos
	 */
	private void modifyIpGroup(boolean ddos) {
		FastMap<Integer, FastMap<Integer, IPGroupModel>> ipgroup =  IPGroupDict.getInstance().getIPGroupModelDictionary();
		for(FastMap<Integer, IPGroupModel> ipMap:ipgroup.values()) {
			for(IPGroupModel ipModel:ipMap.values()) {
				if(ipModel.getRemark()!=null && ipModel.getRemark().contains("云梯")) {
					if(ddos) {
						ipModel.setState(1);
					}else {
						ipModel.setState(0);
					}
				}else {
					if(ddos) {
						ipModel.setState(0);
					}else {
						ipModel.setState(1);
					}
				}
			}
		}
	}
	
	public int getBlackSize() {
		Map<String, IpFirewallModel> ipBlackWallMap = FirewallServiceImpl.getInstance().getIpFirewallModelMap();
		int blackSize = 0;
		for(Entry<String, IpFirewallModel> entry:ipBlackWallMap.entrySet()) {
			if(entry.getValue().isBlackIp()) {
				blackSize++;
			}
		}
		return blackSize;
	}

}
