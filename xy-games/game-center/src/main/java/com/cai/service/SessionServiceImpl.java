package com.cai.service;

import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;

import com.cai.common.domain.Account;
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
	
	
	private Map<Integer,Session> sessionMap = Maps.newConcurrentMap();
	

	/**
	 * 在线的session key=session_id
	 */
	private Map<Integer,Session> onlineSessionMap = Maps.newConcurrentMap(); 
	
	/**
	 * 账号-sessionId
	 */
	private Map<Long,Integer> onlineAccountIdSessionIdMap = Maps.newConcurrentMap();
	
	
	/**
	 * 账号缓存
	 */
	private Map<String,Account> accountMap = Maps.newConcurrentMap();
	
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
		onlineSessionMap.put(session.getSessionId(), session);
		onlineAccountIdSessionIdMap.put(session.getAccountID(), session.getSessionId());
		
	}

	@Override
	public void sessionFree(Session session) {
		onlineSessionMap.remove(session.getSessionId());
		onlineAccountIdSessionIdMap.remove(session.getAccountID());
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
	public void fireSessionCreate(Session _session)
	{
		ServiceManager.getInstance().sessionCreate(_session);
	}
	
	public void fireSessionFree(int session_id)
	{
		
		SessionServiceImpl.getInstance().getSessionMap().remove(session_id);
		Session session = onlineSessionMap.get(session_id);
		if(session!=null){
			ServiceManager.getInstance().sessionFree(session);
			if(session.getChannel()!=null && session.getChannel().isActive()){
				session.getChannel().close();
			}
		}
		
		
		//信息
		StringBuilder buf = new StringBuilder();
		buf.append("当前sessionMap.szie="+SessionServiceImpl.getInstance().getSessionMap().size())
		.append(";onlineSessionMap.size="+onlineSessionMap.size())
		.append(";onlineAccountIdSessionIdMap.size="+onlineAccountIdSessionIdMap.size());
		System.out.println(buf.toString());
		
	}
	
	/**
	 * 查询sesson
	 * @param session_id
	 * @return
	 */
	public Session getSession(int session_id){
		return sessionMap.get(session_id);
	}

	public Map<Integer, Session> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Integer, Session> sessionMap) {
		this.sessionMap = sessionMap;
	}

	



	public Map<Integer, Session> getOnlineSessionMap() {
		return onlineSessionMap;
	}

	public void setOnlineSessionMap(Map<Integer, Session> onlineSessionMap) {
		this.onlineSessionMap = onlineSessionMap;
	}

	public Map<Long, Integer> getOnlineAccountIdSessionIdMap() {
		return onlineAccountIdSessionIdMap;
	}

	public void setOnlineAccountIdSessionIdMap(Map<Long, Integer> onlineAccountIdSessionIdMap) {
		this.onlineAccountIdSessionIdMap = onlineAccountIdSessionIdMap;
	}

	public Map<String, Account> getAccountMap() {
		return accountMap;
	}

	public void setAccountMap(Map<String, Account> accountMap) {
		this.accountMap = accountMap;
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
