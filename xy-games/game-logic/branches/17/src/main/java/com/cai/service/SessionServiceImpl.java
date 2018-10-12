package com.cai.service;

import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;

import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.google.common.collect.Maps;

/**
 * 会话相关
 * @author run
 *
 */
public class SessionServiceImpl extends AbstractService{
	
	private static SessionServiceImpl instance = null;
	
	
	private Map<Long,Session> sessionMap = Maps.newConcurrentMap();
	
	private Timer timer;
	
	
	
	
	private SessionServiceImpl()
	{
		timer = new Timer("SessionServiceImpl-Timer");
	}
	
	public static SessionServiceImpl getInstance()
	{
		if (null == instance)
		{
			instance = new SessionServiceImpl();
		}
		return instance;
	}

	@Override
	protected void startService() {
		//4s扫描一次
		//timer.scheduleAtFixedRate(new HeartTask(), 0L, 4000L); 
		//timer.scheduleAtFixedRate(new SessinTimeOut(), 0L, 4000L); 
		
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
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * 查询sesson
	 * @param session_id
	 * @return
	 */
	public Session getSession(long session_id){
		return sessionMap.get(session_id);
	}

	public Map<Long, Session> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Long, Session> sessionMap) {
		this.sessionMap = sessionMap;
	}

	



	private class HeartTask extends TimerTask {
		private int logTimes = 0;

		public void run() {
			
			long time = System.currentTimeMillis();
			System.out.println("jkdsjflsjdlkf");

		}
	}
	
	private class SessinTimeOut extends TimerTask {

		public void run() {
//			long nowTime = System.currentTimeMillis();
//			for(Session m : sessionMap.values()){
//				long lastackTime = m.getLast_client_heart_ack();
//				if(nowTime - lastackTime > 40000L){
//					m.getChannel().close();
//				}
//			}

		}
	}
	
	
}
