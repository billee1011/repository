package com.cai.coin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.Symbol;
import com.cai.common.define.ETriggerType;
import com.cai.common.domain.CoinPlayerRedis;
import com.cai.common.domain.Player;
import com.cai.common.domain.coin.CoinGameDetail;
import com.cai.common.thread.HandleMessageExecutorPool;
import com.cai.common.type.CoinType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.StringUtil;
import com.cai.dictionary.CoinDict;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.SessionServiceImpl;
import com.cai.tasks.CheckCoinGameTask;
import com.cai.tasks.CoinGameOverTask;
import com.cai.util.RedisRoomUtil;
import com.cai.util.SystemRoomUtil;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.BaseS2S.SendToClientsProto2;
import protobuf.clazz.coin.CoinProtocol.GameConnectResponse;
import protobuf.clazz.coin.CoinProtocol.GameEnterResponse;
import protobuf.clazz.coin.CoinProtocol.GameExitResponse;
import protobuf.clazz.coin.CoinProtocol.GameStartFailResponse;
import protobuf.clazz.coin.CoinProtocol.MessageTip;
import protobuf.clazz.coin.CoinServerProtocol;
import protobuf.clazz.coin.CoinServerProtocol.S2SMatchSuccessRequest;

public class CoinService {
	private static final Logger logger = LoggerFactory.getLogger(CoinService.class);
	private static CoinService service = new CoinService();

	private CoinService() {
	}

	private static volatile boolean isStart = false;

	private Map<Integer, CoinTable> coinMap = new ConcurrentHashMap<>();
	private Map<Integer, Future<?>> futureMap = new ConcurrentHashMap<>();
	private HandleMessageExecutorPool executor = new HandleMessageExecutorPool("coin-executor");
	private HandleMessageExecutorPool check_executor = new HandleMessageExecutorPool("coin-check_executor", 1);

	public static CoinService INTANCE() {
		return service;
	}

	public void gameExit(long accountId, Session session) {
		CoinPlayerRedis playerRedis = getPlayerRedis(accountId);
		if (playerRedis == null) {
			sendExitRsp(session, accountId, CoinType.OP_FAIL, "游戏未开始,退出失败");
			return;
		}
		int coinIndex = playerRedis.getCoinIndex();
		CoinTable table = coinMap.get(coinIndex);
		if (table == null) {
			removePlayerRedis(accountId);
			sendEnterRsp(session, accountId, CoinType.OP_FAIL, "游戏已结束,退出失败");
			return;
		}

		CoinPlayer player = table.getCoinPlayer(accountId);
		if (player == null) {
			removePlayerRedis(accountId);
			sendEnterRsp(session, accountId, CoinType.OP_FAIL, "未找到玩家数据,退出失败");
			return;
		}

		removePlayerRedis(accountId);
		player.setChannel(null);
		player.setEnter(false);
		player.setOnline(false);

		AbstractRoom room = table.getGameRoom();
		if (room != null) {
			if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
				RedisRoomUtil.clearRoom(player.getAccount_id(), 0);
			}
		}

		sendEnterRsp(session, accountId, CoinType.OP_SUCCESS, "退出成功");
	}

	public void gameEnter(long accountId, int proxyId, Session session) {

		CoinPlayerRedis playerRedis = getPlayerRedis(accountId);
		if (playerRedis == null) {
			sendEnterRsp(session, accountId, CoinType.OP_FAIL, "游戏未开始,进入失败");
			return;
		}
		int coinIndex = playerRedis.getCoinIndex();
		CoinTable table = coinMap.get(coinIndex);
		if (table == null) {
			removePlayerRedis(accountId);
			sendEnterRsp(session, accountId, CoinType.OP_FAIL, "游戏已结束,进入失败");
			return;
		}

		CoinPlayer player = table.getCoinPlayer(accountId);
		if (player == null) {
			removePlayerRedis(accountId);
			sendEnterRsp(session, accountId, CoinType.OP_FAIL, "未找到玩家数据,进入失败");
			return;
		}

		int roomId = SystemRoomUtil.getRoomId(player.getAccount_id());
		if (!table.isStart() && roomId > 0) {
			sendEnterRsp(session, accountId, CoinType.OP_FAIL, "您有未完成的游戏,进入失败");
			return;
		}

		player.setChannel(session.getChannel());
		player.setOnline(true);
		player.setProxy_index(proxyId);

		if (!player.isEnter()) {
			PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(), player);
			player.setEnter(true);
		}
		AbstractRoom room = table.getGameRoom();
		if (room != null && table.isStart()) {
			ReentrantLock lock = room.getRoomLock();
			try {
				lock.lock();
				if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
					room.onPlayerEnterUpdateRedis(player.getAccount_id());
					room.handler_reconnect_room(player);
				}
			} finally {
				lock.unlock();
			}
		}

		sendEnterRsp(session, accountId, CoinType.OP_SUCCESS, "进入成功");
		reconnect(accountId, room, session);
	}

	// 重连
	private void reconnect(long accountId, AbstractRoom room, Session session) {
		GameConnectResponse.Builder resp = GameConnectResponse.newBuilder();
		int base = 1;
		int baseScore = 1;
		int times = 1;
		if (room != null) {
			base = room.matchBase.getBase();
			baseScore = room.matchBase.getBaseScore();
			int roomNum = room.getPlayerCount();
			Player player = null;
			for (int i = 0; i < roomNum; i++) {
				player = room.getPlayerBySeatId(i);
				if (player != null && player.getAccount_id() == accountId) {
					times = room.getTimes(i);
				}
			}
		}
		resp.setBase(base);
		resp.setBaseScore(baseScore);
		resp.setTimes(times);

		session.send(PBUtil.toS_S2CRequet(accountId, C2SCmd.COIN_GAME_CONNECT, resp).build());
	}

	public int getRoomToolsLimit(AbstractRoom room, int coinIndex) {
		int limit = -1;
		CoinTable table = coinMap.get(coinIndex);
		if (table != null) {
			limit = table.getDetail().getTools_limit();
		}
		return limit;
	}

	public void gameOver(AbstractRoom room, int coinIndex) {
		try {
			CoinTable table = coinMap.remove(coinIndex);
			CoinGameOverTask task = new CoinGameOverTask(room, table);
			executor.execute(coinIndex, task);
		} catch (Exception e) {
			logger.error("coin gameOver error " + e.getMessage(), e);
		}
	}

	public void gameStart(S2SMatchSuccessRequest request) {
		int detailId = request.getDetailId();
		int gameId = request.getGameId();
		List<CoinServerProtocol.CoinPlayerProto> playerProtos = request.getAccountsList();
		List<Long> accountIds = Lists.newArrayListWithCapacity(playerProtos.size());
		playerProtos.forEach(p -> accountIds.add(p.getAccountId()));

		CoinGameDetail gDetail = CoinDict.getInstance().getGameDetail(gameId, detailId);
		if (gDetail == null) {
			sendFailMsg(accountIds);
			return;
		}

		try {
			int coinIndex = request.getCoinIndex();
			CoinTable table = new CoinTable(coinIndex, executor, gDetail);
			table.gameReady(accountIds, playerProtos);
			coinMap.put(coinIndex, table);
			addStateTask(coinIndex);
		} catch (Exception e) {
			sendFailMsg(accountIds);
			logger.error("gameStart-> error !!", e);
		}
	}

	public void checkGameState(int coinIndex) {
		CoinTable table = coinMap.get(coinIndex);
		if (table == null) {
			removeStateFutrue(coinIndex);
			return;
		}
		AbstractRoom room = table.getGameRoom();
		if (room == null) {
			removeStateFutrue(coinIndex);
			return;
		}
		if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
			room.force_account();
		}
		removeStateFutrue(coinIndex);
	}

	/**
	 * 清除玩家金币场缓存
	 */
	public void removePlayerRedis(long accountId) {
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hDel(RedisConstant.COIN_PLAYER_INFO, accountId + "");
	}

	/**
	 * 获取玩家金币场缓存
	 */
	public CoinPlayerRedis getPlayerRedis(long accountId) {
		RedisService redisService = SpringService.getBean(RedisService.class);
		CoinPlayerRedis playerRedis = redisService.hGet(RedisConstant.COIN_PLAYER_INFO, accountId + "", CoinPlayerRedis.class);
		return playerRedis;
	}

	private void sendExitRsp(Session session, long accountId, int status, String msg) {

		GameExitResponse.Builder response = GameExitResponse.newBuilder();
		MessageTip.Builder tipMsg = MessageTip.newBuilder();
		tipMsg.setValue(status);
		tipMsg.setTip(msg);
		response.setTip(tipMsg.build());

		session.send(PBUtil.toS_S2CRequet(accountId, C2SCmd.COIN_GAME_EXIT, response).build());
	}

	private void sendEnterRsp(Session session, long accountId, int status, String msg) {

		GameEnterResponse.Builder response = GameEnterResponse.newBuilder();
		MessageTip.Builder tipMsg = MessageTip.newBuilder();
		tipMsg.setValue(status);
		tipMsg.setTip(msg);
		response.setTip(tipMsg.build());

		session.send(PBUtil.toS_S2CRequet(accountId, C2SCmd.COIN_GAME_ENTER, response).build());
	}

	private void sendFailMsg(List<Long> accountIds) {
		GameStartFailResponse.Builder resp = GameStartFailResponse.newBuilder();
		MessageTip.Builder msg = MessageTip.newBuilder();
		msg.setValue(-1);
		msg.setTip("金币场开始失败,请重新匹配!");
		resp.setTip(msg.build());

		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountIds);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_FAIL, resp)).build();
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
	}

	private void addStateTask(int coinIndex) {
		CheckCoinGameTask task = new CheckCoinGameTask(coinIndex);
		long delayTime = 10 * 60 * 1000;
		Future<?> future = check_executor.schedule(task, delayTime);
		futureMap.put(coinIndex, future);
	}

	public void cancelStateTask(int coinIndex) {
		Future<?> future = futureMap.get(coinIndex);
		if (future != null) {
			future.cancel(true);
		}
		removeStateFutrue(coinIndex);
	}

	private void removeStateFutrue(int coinIndex) {
		if (futureMap.containsKey(coinIndex)) {
			futureMap.remove(coinIndex);
		}
	}
}
