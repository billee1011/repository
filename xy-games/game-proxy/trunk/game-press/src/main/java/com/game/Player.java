/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.cai.common.constant.MsgConstants;
import com.cai.common.util.RuntimeOpt;
import com.game.common.Cfg;
import com.game.common.Constant;
import com.game.common.util.PressGlobalExecutor;
import com.game.manager.PlayerMananger;
import com.game.network.PressConnector;
import com.game.network.RspExecutor;
import com.game.network.tasks.EmjChatTask;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.concurrent.IEventListener;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.concurrent.WorkerLoopGroup;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEvent.Event;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.HeartRequest;
import protobuf.clazz.Protocol.LoginRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.RoomRequest;

/**
 * 
 *
 * @author wu_hc date: 2017年10月11日 下午4:17:30 <br/>
 */
public final class Player implements IOEventListener<S2SSession>, IEventListener {

	/**
	 * 
	 */
	private static final WorkerLoopGroup workGroup = DefaultWorkerLoopGroup.newGroup("player-work-thread", 12);

	/**
	 * 
	 */
	private final AtomicInteger SEQ = new AtomicInteger(0);

	private long accountId;

	private String accountName;

	private PressConnector connector;

	private WorkerLoop worker;

	private boolean ready;

	/**
	 * @param accountName
	 */
	public Player(String accountName) {
		this.accountName = accountName;
	}

	/**
	 * 
	 * @param request
	 */
	public void send(final Object request) {
		if (connector.isActive()) {
			connector.send(request);
		}
	}

	public void connect() {
		PressConnector connector = new PressConnector(Cfg.targetHost);
		connector.setListener(this);
		connector.setConnectedCallback((cntor) -> {
			PressGlobalExecutor.schedule(() -> {
				sendLogin();
			}, ThreadLocalRandom.current().nextLong(0, 20000));
		});
		connector.doInit();
		this.connector = connector;
		connector.connect();
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public PressConnector getConnector() {
		return connector;
	}

	public void setConnector(PressConnector connector) {
		this.connector = connector;
	}

	/**
	 * 发起登陆
	 */
	public void sendLogin() {
		LoginRequest.Builder builder = LoginRequest.newBuilder();
		builder.setImei(accountName);
		builder.setType(3);
		builder.setGameIndex(6);
		builder.setClientIp(RuntimeOpt.getHostAddress());
		builder.setClientFlag(3);

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.LOING);
		requestBuilder.setExtension(Protocol.loginRequest, builder.build());
		connector.send(requestBuilder.build());

	}

	/**
	 * 发起心跳
	 */
	public void sendHeart() {
		HeartRequest.Builder builder = HeartRequest.newBuilder();
		builder.setSeqNum(SEQ.incrementAndGet());
		builder.setTime(System.currentTimeMillis());

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.HEAR);
		requestBuilder.setExtension(Protocol.heartRequest, builder.build());
		connector.send(requestBuilder);
	}

	/**
	 * 网络事件
	 */
	@Override
	public void onEvent(IOEvent<S2SSession> ioEvent) {

		if (ioEvent.event() == Event.READ) {
			Response response = (Response) ioEvent.attachment();
			// System.err.println(response);
			WorkerLoop worker = ioEvent.session().attr(Constant.worker_key).get();
			worker.runInLoop(new RspExecutor(response, ioEvent.session()));
			return;
		}

		if (ioEvent.event() == Event.UNREGISTERED && accountId > 0) {
			PlayerMananger.getInstance().removePlayer(accountId);
			WorkerLoop worker = ioEvent.session().attr(Constant.worker_key).get();
			worker.unRegister(this);
			return;
		}

		if (ioEvent.event() == Event.REGISTERED) {
			ioEvent.session().attr(Constant.player_key).set(this);
			ioEvent.session().attr(Constant.worker_key).set(worker = workGroup.next());
			worker.register(this);
			return;
		}
	}

	/**
	 * 
	 * @param roomId
	 */
	public void sendTrustee(int roomId) {
		RoomRequest.Builder builder = RoomRequest.newBuilder();
		builder.setType(MsgConstants.REQUST_IS_TRUSTEE);
		builder.setAppId(9);
		builder.setRoomId(roomId);
		builder.setIsTrustee(true);
		builder.setTrusteeType(40962);

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.ROOM);
		requestBuilder.setExtension(Protocol.roomRequest, builder.build());
		send(requestBuilder.build());
	}

	/**
	 * 
	 * @param targetAccountId
	 * @param emjId
	 */
	public void sendChat(long targetAccountId, int emjId) {
		Runnable task = new EmjChatTask(this, targetAccountId, emjId);
		this.worker.runInLoop(task);
	}

	public WorkerLoop getWorker() {
		return worker;
	}

	@Override
	public void onEvent(Object event) {

	}

	/**
	 * @param b
	 */
	public void setReady(boolean b) {
		this.ready = b;
	}

	public boolean getReady() {
		return this.ready;
	}

	@Override
	public String toString() {
		return "Player [accountId=" + accountId + ", accountName=" + accountName + "]";
	}

}
