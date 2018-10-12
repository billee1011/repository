/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EPlayerStatus;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RuntimeOpt;
import com.cai.common.util.SessionUtil;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.module.RoomModule;
import com.cai.tasks.OfflineTask;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.concurrent.WorkerLoopGroup;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.s2s.S2SProto.PlayerStatusProto;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 *
 * @author wu_hc
 */
public class C2SSessionService extends AbstractService {

	/**
	 * 处理请求的线程池，线程安全
	 */
	private static final WorkerLoopGroup workGroup = DefaultWorkerLoopGroup.newGroup("player-work-thread", RuntimeOpt.availableProcessors() << 1);

	/**
	 * 玩家上下线
	 */
	// private static final Executor e = new
	// DefaultWorkerLoop("player-status-thread");

	/**
	 * 保存已经登陆的session, [accountID,session]
	 */
	private final ConcurrentMap<Long, C2SSession> accountSessions = Maps.newConcurrentMap();

	/**
	 * 保存所有已经建立的session,[seessionId,session]
	 */
	private final ConcurrentMap<Long, C2SSession> allSessions = Maps.newConcurrentMap();

	/**
	 * 
	 */
	private final static C2SSessionService service = new C2SSessionService();

	private C2SSessionService() {
	}

	public static C2SSessionService getInstance() {
		return service;
	}

	public C2SSession getSession(Long accountID) {
		return accountSessions.get(accountID);
	}

	/**
	 * 会话/玩家下线
	 * 
	 * @param sessionId
	 */
	public void offline(C2SSession session) {
		Account account = session.getAccount();
		if (null == account) {
			logger.error("offline,会话未进行登录操作~~{}", session);
			return;
		}
		C2SSession s = accountSessions.remove(account.getAccount_id());
		if (null == s) {
			return;
		}
		// notifyToAll(session);
		GlobalExecutor.execute(new OfflineTask(session));

		// 1通知俱乐部服
		notifyClub(session, EPlayerStatus.OFFLINE);
		// 2通知网关服
		notifyGate(session, EPlayerStatus.OFFLINE);

		debugMessage(session, 0);
	}

	/**
	 * 会话/玩家上线
	 * 
	 * @param session
	 */
	public void online(final C2SSession session, final Account account) {
		if (null == session || null == account) {
			logger.error("online,会话未进行登录操作~~{}", session);
			return;
		}

		// 1如果是重复登陆，不需要重新分配业务线程
		if (null == account.getWorkerLoop()) {
			account.setWorkerLoop(workGroup.next());
		}

		// 2如果是一条连接,两个帐号的情况,先打日志观察一阵子
		if (0L != session.getAccountID() && account.getAccount_id() != session.getAccountID()) {
			logger.warn("玩家1:[{}]与玩家2:[{}]共用同一条tcp连接了,sessionid:{},channel:{}", account, session.getAccount(), session.getSessionId(),
					session.channel());
			accountSessions.remove(session.getAccountID());
		}
		session.setAccount(account);
		C2SSession oldSession = accountSessions.put(session.getAccountID(), session);
		if (null != oldSession && oldSession.getSessionId() != session.getSessionId()) {
			logger.info("玩家:[{}]重复发起登陆,关闭旧连接,sessionId:{}!", account, oldSession.getSessionId());
			oldSession.setAccount(null);
			SessionUtil.shutdown(oldSession); // 关闭旧连接
		}
		debugMessage(session, 1);
		notifyClub(session, EPlayerStatus.ONLINE);
		notifyGate(session, EPlayerStatus.ONLINE);
	}

	/**
	 * 
	 * @param session
	 */
	public void notifyToAll(final C2SSession session) {

		if (null == session.getAccount()) {
			return;
		}
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
		if (null == session.getAccount()) {
			return;
		}
		RoomRedisModel roomRedisModel = RoomModule.getRoomRedisModelIfExsit(session.getAccount(), session);
		int logicIndex = 0;
		if (null != roomRedisModel) {
			logicIndex = roomRedisModel.getLogic_index();
		} else {
			logicIndex = SessionUtil.getLastAccessLogicSvrId(session);
		}

		if (logicIndex > 0) {
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(4);
			logicRoomRequestBuilder.setAccountId(session.getAccount().getAccount_id());
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			ClientServiceImpl.getInstance().sendMsg(logicIndex, requestBuider.build());
		}
	}

	/**
	 * 通知中心服
	 * 
	 * @param session
	 */
	private void notifyCenter(final C2SSession session) {
		// 计算在线时长
		Account account = session.getAccount();
		if (null == account) {
			return;
		}
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);

		Date now = new Date();
		AccountModel accountModel = account.getAccountModel();
		// 历史在线时长
		int second = (int) ((System.currentTimeMillis() - session.getCreateTime()) / 1000L);
		accountModel.setHistory_online(accountModel.getHistory_online() + second);
		// 今日在线时长,判断最后登录时间是否是今天,如果不是今天的从今天零点开始算
		long startTime = 0;
		int onlineTime = 0;
		if (DateUtils.isSameDay(now, accountModel.getLast_login_time())) {
			startTime = session.getCreateTime();
			second = (int) ((System.currentTimeMillis() - startTime) / 1000L);
			onlineTime = accountModel.getToday_online() + second;
		} else {
			//将当天在线时长清为0
			accountModel.setToday_online(0);
			startTime = MyDateUtil.getZeroDate(now).getTime();
			second = (int) ((System.currentTimeMillis() - startTime) / 1000L);
			onlineTime = second;
		}
		// second = (int) ((System.currentTimeMillis() - startTime) / 1000L);
		accountModel.setToday_online(onlineTime);

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
	 * @param session
	 * @param type
	 *            1上线，2下线
	 */
	public void notifyClub(final C2SSession session, EPlayerStatus status) {
		Account account = session.getAccount();
		if (null == account) {
			return;
		}

		PlayerStatusProto.Builder builder = PlayerStatusProto.newBuilder();
		builder.setAccountId(account.getAccount_id());
		builder.setStatus(status.status());
		builder.setLoginTime(account.getAccountModel().getLogin_times());
		ClientServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.PLAYER_STATUS, builder).build());

		ClientServiceImpl.getInstance().sendMatch(PBUtil.toS2SRequet(S2SCmd.PLAYER_STATUS, builder).build());

		ClientServiceImpl.getInstance().sendToCoin(PBUtil.toS2SRequet(S2SCmd.PLAYER_STATUS, builder).build());
	}

	/**
	 * 
	 * @param session
	 * @param type
	 *            1上线，2下线
	 */
	public void notifyGate(final C2SSession session, EPlayerStatus status) {
		Account account = session.getAccount();
		if (null == account) {
			return;
		}

		PlayerStatusProto.Builder builder = PlayerStatusProto.newBuilder();
		builder.setAccountId(account.getAccount_id());
		builder.setStatus(status.status());
		builder.setLoginTime(account.getAccountModel().getLogin_times());
		ClientServiceImpl.getInstance().sendAllGate(PBUtil.toS2SRequet(S2SCmd.PLAYER_STATUS, builder).build());
	}

	/**
	 * 防止俱乐部服中途重启
	 */
	public void notifyClubAllAccountOnline() {
		for (final C2SSession session : getAllOnlieSession()) {
			notifyClub(session, EPlayerStatus.ONLINE);
		}
	}

	/**
	 * 防止俱乐部服中途重启
	 */
	public void notifyGateAllAccountOnline() {
		for (final C2SSession session : getAllOnlieSession()) {
			notifyGate(session, EPlayerStatus.ONLINE);
		}
	}

	/**
	 * 
	 * @param c2s
	 * @param type
	 */
	private void debugMessage(final C2SSession c2s, int type) {
		if (SystemConfig.gameDebug == 1) {
			logger.info("玩家[{}]{},sessionid:{},在线人数:{},总会话:{}", c2s.getAccount(), type == 1 ? "上线" : "下线", c2s.getSessionId(), accountSessions.size(),
					allSessions.size());
		} else {
			// logger.info("玩家[{}]{},sessionid:{}", c2s.getAccount(), type == 1
			// ? "上线" : "下线", c2s.getSessionId());
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
		return Lists.newArrayList(accountSessions.values());
	}

	@Override
	protected void startService() {
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
	}

	@Override
	public void sessionCreate(C2SSession session) {
		allSessions.put(session.getSessionId(), session);
	}

	@Override
	public void sessionFree(C2SSession session) {
		allSessions.remove(session.getSessionId());
		if (session.getAccountID() > 0) {
			offline(session);
		}
		session.shutdownGracefully();
	}

	public List<C2SSession> getAllSession() {
		return Lists.newArrayList(allSessions.values());
	}

	public int getAllSessionCount() {
		return allSessions.size();
	}

	public Map<Long, C2SSession> getAllSessionMap() {
		return Collections.unmodifiableMap(allSessions);
	}

	public Map<Long, C2SSession> getAccountSessionMap() {
		return Collections.unmodifiableMap(accountSessions);
	}

	/**
	 * 
	 */
	public void allOffline() {
		List<C2SSession> allSession = getAllSession();
		allSession.forEach((session) -> {
			// 1通知俱乐部服
			notifyClub(session, EPlayerStatus.OFFLINE);
			// 2通知网关服
			notifyGate(session, EPlayerStatus.OFFLINE);
		});
	}

	@Override
	public void dbUpdate(int _userID) {
	}

	/**
	 * 发送给所有在线玩家
	 * 
	 * @param cmd
	 * @param builder
	 */
	public void sendAllOLPlayers(int cmd, GeneratedMessage.Builder<?> builder) {
		getAllOnlieSession().forEach(session -> {
			session.send(PBUtil.toS2CCommonRsp(cmd, builder));
		});
	}

	/**
	 * 发送给所有在线玩家
	 * 
	 * @param builder
	 */
	public void sendAllOLPlayers(GeneratedMessage.Builder<?> builder) {
		getAllOnlieSession().forEach(session -> {
			session.send(builder);
		});
	}

	/**
	 * 发送给所有在线玩家
	 * 
	 * @param builder
	 */
	public void sendAllOLPlayers(GeneratedMessage message) {
		getAllOnlieSession().forEach(session -> {
			session.send(message);
		});
	}

	/**
	 * 将当前日志session写入文件
	 * 
	 * @param async
	 *            是否异步
	 */
	public void writeToFile(boolean async) {
		final Runnable task = () -> {
			try {
				int onlineCount = getOnlineCount();
				int allSession = getAllSessionCount();
				final StringBuilder sb = new StringBuilder();
				sb.append("在线玩家:").append(onlineCount).append("\n");
				sb.append("总连接:").append(allSession).append("\n");
				sb.append("--------- account ------------\n");
				accountSessions.forEach((K, V) -> {
					sb.append(String.format("玩家[%s],会话[%s],sessionid:%d", V.getAccount(), V.channel(), V.getSessionId())).append("\n");
				});

				sb.append("--------- session ------------\n");
				allSessions.forEach((K, V) -> {
					sb.append(String.format("会话[%s],sessionid:%d", V.channel(), V.getSessionId())).append("\n");
				});
				String fileName = String.format("%d-session-log-%s.log", SystemConfig.proxy_index, LocalDate.now().toString());
				FileUtils.writeStringToFile(new File(fileName), sb.toString(), Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
			}
		};

		if (async) {
			new Thread(task).start();
		} else {
			task.run();
		}

	}
}
