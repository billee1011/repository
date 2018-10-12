/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.time.DateUtils;

import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.RuntimeOpt;
import com.cai.core.SystemConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.concurrent.WorkerLoopGroup;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;
import com.xianyi.framework.core.service.Service;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 *
 * @author wu_hc
 */
@IService(name = "SessionService", order = 1)
public class C2SSessionService extends AbstractService implements Service {

	/**
	 * 处理请求的线程池，线程安全
	 */
	private static final WorkerLoopGroup workGroup = DefaultWorkerLoopGroup.newGroup("player-work-thread", RuntimeOpt.availableProcessors() << 1);

	/**
	 * [accountID,session]
	 */
	private final ConcurrentMap<Long, C2SSession> accountSessions = Maps.newConcurrentMap();

	/**
	 * 
	 */
	private final static C2SSessionService service = new C2SSessionService();

	public static C2SSessionService getInstance() {
		return service;
	}

	@Override
	public void start() throws Exception {

	}

	@Override
	public void stop() throws Exception {

	}

	private void addSession(final C2SSession c2s) {
		C2SSession s = accountSessions.put(c2s.getAccountID(), c2s);
		if (null != s) {
			s.shutdownGracefully();
		}
	}

	public C2SSession getSession(Long accountID) {
		return accountSessions.get(accountID);
	}

	/**
	 * 移出会话
	 * 
	 * @param sessionId
	 * @param disConnect
	 * @return
	 */
	private C2SSession removeSession(long accountID, boolean disConnect) {
		C2SSession c2s = accountSessions.remove(accountID);
		if (disConnect)
			c2s.shutdownGracefully();
		return c2s;
	}

	/**
	 * 踢玩家下线
	 * 
	 * @param accountID
	 */
	public void kick(long accountID) {
		offline(accountID);
	}

	/**
	 * 会话/玩家下线
	 * 
	 * @param sessionId
	 */
	public synchronized void offline(long accountID) {
		C2SSession session = accountSessions.get(accountID);
		if (null == session) {
			return;
		}
		session.getAccount().getWorkerLoop().unRegister(null);
		notifyToAll(session);
		removeSession(accountID, true);
		debugMessage(session, 0);
	}

	/**
	 * 会话/玩家下线
	 * 
	 * @param sessionId
	 */
	public synchronized void offline(C2SSession session) {
		Account account = session.getAccount();
		if (null == account) {
			return;
		}
		account.getWorkerLoop().unRegister(null);
		notifyToAll(session);
		removeSession(account.getAccount_id(), true);
		debugMessage(session, 0);
	}

	/**
	 * 会话/玩家上线
	 * 
	 * @param session
	 */
	public synchronized void online(final C2SSession session, final Account account) {
		if (null == session || null == account) {
			logger.error("会话未进行登录操作~~{}", session);
			return;
		}
		session.setAccount(account);
		WorkerLoop loop = workGroup.next();
		account.setWorkerLoop(loop);
		loop.register(null);
		addSession(session);
		debugMessage(session, 1);
	}

	/**
	 * 
	 * @param session
	 */
	public void notifyToAll(final C2SSession session) {
		// 1如果玩家跟某个逻辑服有关系，通知逻辑服玩家下线
		notifyLogic(session);
		// 2通知中心刷新缓存
		notifyCenter(session);
	}

	/**
	 * 通知代理服
	 * 
	 * @param session
	 */
	private void notifyLogic(final C2SSession session) {
		Integer logicIndex = session.attr(C2SSession.SESSION_LOGIC_ID).get();
		if (null == logicIndex || logicIndex.intValue() <= 0) {
			return;
		}

//		Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
//		LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
//		logicRoomRequestBuilder.setType(4);
//		logicRoomRequestBuilder.setAccountId(session.getAccount().getAccount_id());
//		requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
//		ClientServiceImpl.getInstance().sendMsg(logicIndex, requestBuider.build());
	}

	/**
	 * 通知中心服
	 * 
	 * @param session
	 */
	private void notifyCenter(final C2SSession session) {
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);

		// 计算在线时长
		Account account = session.getAccount();

		Date now = new Date();
		AccountModel accountModel = account.getAccountModel();
		// 历史在线时长
		int second = (int) ((System.currentTimeMillis() - session.getCreateTime()) / 1000L);
		accountModel.setHistory_online(accountModel.getHistory_online() + second);
		// 今日在线时长,判断最后登录时间是否是今天,如果不是今天的从今天零点开始算
		long startTime = 0;
		if (DateUtils.isSameDay(now, accountModel.getLast_login_time())) {
			startTime = session.getCreateTime();
		} else {
			startTime = MyDateUtil.getZeroDate(now).getTime();
		}
		second = (int) ((System.currentTimeMillis() - startTime) / 1000L);
		accountModel.setToday_online(accountModel.getToday_online() + second);

		// ==
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		rsAccountResponseBuilder.setFlushRedisCache(true);

		RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
		rsAccountModelResponseBuilder.setTodayOnline(accountModel.getToday_online());
		rsAccountModelResponseBuilder.setHistoryOnline(accountModel.getHistory_online());
		rsAccountModelResponseBuilder.setNeedDb(true);
		rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
	}

	/**
	 * 
	 * @param c2s
	 * @param type
	 */
	private void debugMessage(final C2SSession c2s, int type) {
		if (SystemConfig.gameDebug == 0) {
			logger.info("玩家[{}]{},当前在线人数:{}", c2s.getAccount(), type == 1 ? "上线" : "下线", accountSessions.size());
		}
	}

	/**
	 * 
	 * @return
	 */
	public WorkerLoopGroup getWorkerGroup() {
		return workGroup;
	}

	/**
	 * 在线玩家
	 * 
	 * @return
	 */
	public int getOnlineCount() {
		return accountSessions.size();
	}

	public List<C2SSession> getAllOnlieSession() {
		return Collections.unmodifiableList(Lists.newArrayList(accountSessions.values()));
	}
}
