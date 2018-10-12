package com.cai.service;

import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Event;
import com.cai.common.util.ThreadUtil;
import com.cai.core.Global;
import com.cai.core.MonitorEvent;
import com.cai.domain.IpFirewallModel;
import com.cai.future.AutoDefenseRunnable;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import io.netty.channel.Channel;

/**
 * 防火墙
 * @author run
 *
 */
public class FirewallServiceImpl extends AbstractService{
	
	private static Logger logger = LoggerFactory.getLogger(FirewallServiceImpl.class);
	
	private static FirewallServiceImpl instance = null;
	
	private Channel channel;
	
	private Timer timer;
	
	
	/**
	 * ip统计
	 */
	private Map<String,IpFirewallModel> ipFirewallModelMap = Maps.newConcurrentMap();
	
	
	/**
	 * 每个ip最大的链接数
	 */
	public static final int IP_MAX_LINK = 1500;

	/**
	 * ip的链接频率(每N秒最高链接次数)
	 */
	private static final int IP_LINK_HZ = 300;
	
	/**
	 * 检测频率(毫秒)
	 */
	private static final long HZ_MS = 3000L;
	
	/**
	 * 拉黑时间 60s
	 */
	private static final long BLACK_MS = 60000L;
	
	
	public volatile boolean isDebugInfo = true;
	
	private final ReentrantLock mainLock = new ReentrantLock();
	
	public static final long sendTime = System.currentTimeMillis();
	
	public IpFirewallModel addNewLink(String ip){
		
		mainLock.lock();
		try{
			if(ip ==null || "".equals(ip))
				return null;
			
			IpFirewallModel ipFirewallModel = ipFirewallModelMap.get(ip);
			if(ipFirewallModel==null){
				ipFirewallModel = new IpFirewallModel(ip);
				ipFirewallModelMap.put(ip,ipFirewallModel);
			}
			ipFirewallModel.setLinkCount(ipFirewallModel.getLinkCount()+1);
			long nowTime = System.currentTimeMillis();
			if(nowTime - ipFirewallModel.getLastHzFlushTime() > HZ_MS){
				ipFirewallModel.setLastHzFlushTime(nowTime);
				ipFirewallModel.setHzLinkTimes(1);
			}else{
				ipFirewallModel.setHzLinkTimes(ipFirewallModel.getHzLinkTimes()+1);
			}
			
			//是否加黑名单
			if(ipFirewallModel.getBlackExpirationTime() < nowTime &&  ipFirewallModel.getHzLinkTimes()>IP_LINK_HZ){
				ipFirewallModel.setBlackExpirationTime(nowTime+BLACK_MS);
				ipFirewallModel.setBlackIp(true);
				logger.warn("新加入IP黑名单:"+ip);
				if((System.currentTimeMillis()-sendTime)>1000*60*10) {
					TempSmsService.send("13670139534", "新加入IP黑名单:"+ip);
				}
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.unkownError, "特别请注意攻击来了",
						(long) ipFirewallModelMap.size(),"新加入IP黑名单:"+ip );
			}else {
				ipFirewallModel.setBlackIp(false);
			}
			
			
			return ipFirewallModel;
			
		}catch(Exception e){
			logger.error("error",e);
		}finally{
			mainLock.unlock();
		}
		
		return null;
	}
	
	public void delLink(String ip){
		mainLock.lock();
		try{
			if(ip ==null || "".equals(ip))
				return;
			
			IpFirewallModel ipFirewallModel = ipFirewallModelMap.get(ip);
			if(ipFirewallModel==null)
				return;
			
			ipFirewallModel.setLinkCount(ipFirewallModel.getLinkCount()-1);
		}catch(Exception e){
			logger.error("error",e);
		}finally{
			mainLock.unlock();
		}
		
	}
	
	
	
	
	
	private FirewallServiceImpl() {
		timer = new Timer("Timer-FirewallServiceImpl Timer");
	}

	public static FirewallServiceImpl getInstance() {
		if (null == instance) {
			instance = new FirewallServiceImpl();
		}
		return instance;
	}

	@Override
	protected void startService() {
		//logger.info("防火墙模块启动....");
		//timer.schedule(new LogicLinkCheckTimer(), 6000L, 10000L);//链接检测
		
		
		
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionCreate(C2SSession session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(C2SSession session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}

	public Map<String, IpFirewallModel> getIpFirewallModelMap() {
		return ipFirewallModelMap;
	}
	
	
}
