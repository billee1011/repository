package com.cai.service;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.Event;
import com.cai.common.util.MyDateUtil;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.util.MessageResponse;
import com.google.common.collect.Maps;
import com.xianyi.framework.server.AbstractService;
import com.xianyi.framework.server.ServiceManager;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 会话相关
 * @author run
 *
 */
public class SessionServiceImpl extends AbstractService{
	
	private static Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);
	
	private static SessionServiceImpl instance = null;
	
	
	private Map<Long,Session> sessionMap = Maps.newConcurrentMap();
	

	/**
	 * 在线的session key=session_id
	 */
	private Map<Long,Session> onlineSessionMap = Maps.newConcurrentMap(); 
	
	/**
	 * 账号-sessionId
	 */
	private Map<Long,Long> onlineAccountIdSessionIdMap = Maps.newConcurrentMap();
	
	
	/**
	 * 账号缓存
	 */
	private Map<String,Account> accountMap = Maps.newConcurrentMap();
	
	private Timer timer;
	
	private ReentrantLock sessionLock =  new ReentrantLock();
	
	
	
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
		
		//处理多重连接
		sessionLock.lock();
		try{
			long account_id = session.getAccountID();
			Long old_session_id = onlineAccountIdSessionIdMap.get(session.getAccountID());
			if(old_session_id!=null){
				Session oldSession = onlineSessionMap.get(old_session_id);
				onlineAccountIdSessionIdMap.remove(account_id);
				onlineSessionMap.remove(oldSession.getSessionId());
				oldSession.getChannel().close();
			}
			onlineSessionMap.put(session.getSessionId(), session);
			onlineAccountIdSessionIdMap.put(session.getAccountID(), session.getSessionId());
		}catch(Exception e){
			logger.error("error",e);
		}finally{
			sessionLock.unlock();
		}
		
	}

	@Override
	public void sessionFree(Session session) {
		
		sessionLock.lock();
		try{
			onlineSessionMap.remove(session.getSessionId());
			onlineAccountIdSessionIdMap.remove(session.getAccountID());
		}catch(Exception e){
			logger.error("error",e);
		}finally{
			sessionLock.unlock();
		}
		
		
		//通知逻辑服,TODO 后面再写多逻辑服的控制
		Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
		LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
		logicRoomRequestBuilder.setType(4);
		logicRoomRequestBuilder.setAccountId(session.getAccount().getAccount_id());
		requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
		ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
		
		Account account = session.getAccount();

		//通知中心刷新缓存
		//========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		
		
		//计算在线时长
		Date now = new Date();
		AccountModel accountModel = account.getAccountModel();
		//历史在线时长
		int second = (int)((System.currentTimeMillis() - session.getCreateTime())/1000L);
		accountModel.setHistory_online(accountModel.getHistory_online() + second);
		//今日在线时长,判断最后登录时间是否是今天,如果不是今天的从今天零点开始算
		long startTime = 0;
		if(DateUtils.isSameDay(now, accountModel.getLast_login_time())){
			startTime = session.getCreateTime();
		}else{
			startTime = MyDateUtil.getZeroDate(now).getTime();
		}
		second = (int)((System.currentTimeMillis() - startTime)/1000L);
		accountModel.setToday_online(accountModel.getToday_online() + second);
		
		
		//==
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		rsAccountResponseBuilder.setFlushRedisCache(true);
		//
		RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
		rsAccountModelResponseBuilder.setTodayOnline(accountModel.getToday_online());
		rsAccountModelResponseBuilder.setHistoryOnline(accountModel.getHistory_online());
		rsAccountModelResponseBuilder.setNeedDb(true);
		rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		//
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
		//=================================
		
		
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
	public void fireSessionCreate(Session _session)
	{
		ServiceManager.getInstance().sessionCreate(_session);
		
	}
	
	public void fireSessionFree(long session_id)
	{
		
		SessionServiceImpl.getInstance().getSessionMap().remove(session_id);
		Session session = onlineSessionMap.get(session_id);
		if(session!=null){
			ServiceManager.getInstance().sessionFree(session);
//			if(session.getChannel()!=null && session.getChannel().isActive()){
//				session.getChannel().close();
//			}
		}
		onlineSessionMap.remove(session_id);
		
		
		//信息
		if(SystemConfig.gameDebug == 1){
			StringBuilder buf = new StringBuilder();
			buf.append("当前sessionMap.szie="+SessionServiceImpl.getInstance().getSessionMap().size())
			.append(";onlineSessionMap.size="+onlineSessionMap.size())
			.append(";onlineAccountIdSessionIdMap.size="+onlineAccountIdSessionIdMap.size());
			System.out.println(buf.toString());
		}
		
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

	



	public Map<Long, Session> getOnlineSessionMap() {
		return onlineSessionMap;
	}

	public void setOnlineSessionMap(Map<Long, Session> onlineSessionMap) {
		this.onlineSessionMap = onlineSessionMap;
	}

	public Map<Long, Long> getOnlineAccountIdSessionIdMap() {
		return onlineAccountIdSessionIdMap;
	}

	public void setOnlineAccountIdSessionIdMap(Map<Long, Long> onlineAccountIdSessionIdMap) {
		this.onlineAccountIdSessionIdMap = onlineAccountIdSessionIdMap;
	}

	public Map<String, Account> getAccountMap() {
		return accountMap;
	}

	public void setAccountMap(Map<String, Account> accountMap) {
		this.accountMap = accountMap;
	}

	/**
	 * 根据玩家账号获取session
	 * @return
	 */
	public Session getSessionByAccountId(long account_id){
		Long session_id = onlineAccountIdSessionIdMap.get(account_id);
		if(session_id==null)
			return null;
		
		Session session = onlineSessionMap.get(session_id);
		return session;
		
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
