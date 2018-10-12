package com.cai.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.cai.ai.Gamer;
import com.cai.clubmatch.ClubMatchPlayer;
import com.cai.coin.CoinService;
import com.cai.common.constant.ActivityMissionTypeEnum;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.EWealthCategory;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.HuTypeModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomLogModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.MatchType;
import com.cai.common.type.PlayerRoomStatus;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RMIUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.common.util.WealthUtil;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.TurntableDict;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.cai.manager.MessageManager;
import com.cai.match.MatchPlayer;
import com.cai.redis.service.RedisService;
import com.cai.tasks.ClubRecordEnsureTask;
import com.cai.timer.CardTypeCacheTimer;
import com.cai.timer.DataStatTimer;
import com.cai.timer.L2CRMIPingTimer;
import com.cai.timer.ProxySocketCheckTimer;
import com.cai.timer.RoomCheckTimer;
import com.cai.util.ClubMsgSender;
import com.cai.util.ProxyMsgSender;
import com.cai.util.RedisRoomUtil;
import com.cai.util.SystemRoomUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.activity.ActivityTurntableServerProto.TurntableGameOverReq;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse.Builder;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubGameOverProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubGameOverProto.GamePlayerProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubKouDouProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.RecordProtoType;
import protobuf.clazz.s2s.S2SProto.DelRoomNotifyProto;
import protobuf.clazz.s2s.S2SProto.ProxyRoomUpdateProto;
import protobuf.clazz.s2s.S2SProto.RoomWealthProto;
import protobuf.clazz.s2s.S2SProto.S2STransmitProto;

public class PlayerServiceImpl extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);
	private static PlayerServiceImpl instance = null;

	private Map<Long, Player> playerMap = Maps.newConcurrentMap();

	/**
	 * 房间缓存 key=房间号 room_id
	 */
	private Map<Integer, AbstractRoom> roomMap = Maps.newConcurrentMap();

	/**
	 * 金币场房间缓存。
	 */
	public final Map<Integer, AbstractRoom> goldRooms = Maps.newConcurrentMap();

	private Timer timer;

	/**
	 * 牌型记录
	 */
	// 缓存接口这里是LoadingCache，LoadingCache在缓存项不存在时可以自动加载缓存
	private LoadingCache<String, HuTypeModel> cardTypeCache
	// CacheBuilder的构造函数是私有的，只能通过其静态方法newBuilder()来获得CacheBuilder的实例
			= CacheBuilder.newBuilder()
					// 设置并发级别为8，并发级别是指可以同时写缓存的线程数
					.concurrencyLevel(8)
					// 设置写缓存后8秒钟过期
					.expireAfterWrite(24, TimeUnit.HOURS)
					// 设置缓存容器的初始容量为10
					.initialCapacity(100000)
					// 设置缓存最大容量为10000，超过10000之后就会按照LRU最近虽少使用算法来移除缓存项
					.maximumSize(10000000)
					// 设置要统计缓存的命中率
					.recordStats()
					// //设置缓存的移除通知
					// .removalListener(new RemovalListener<Object, Object>() {
					// @Override
					// public void onRemoval(RemovalNotification<Object, Object>
					// notification) {
					// System.out.println(notification.getKey() + " was removed,
					// cause is " + notification.getCause());
					// }
					// })
					// build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
					.build(new CacheLoader<String, HuTypeModel>() {
						@Override
						public HuTypeModel load(String key) throws Exception {
							System.out.println("load HuTypeModel key : " + key);
							HuTypeModel huTypeModel = new HuTypeModel();
							return huTypeModel;
						}
					});

	private PlayerServiceImpl() {
		timer = new Timer("Timer-PlayerServiceImpl Timer");
	}

	public static PlayerServiceImpl getInstance() {
		if (null == instance) {
			instance = new PlayerServiceImpl();
		}
		return instance;
	}

	@Override
	protected void startService() {
		timer.schedule(new ProxySocketCheckTimer(), 10000L, 10000L);// 代理服链接检测
		timer.schedule(new DataStatTimer(), 60000L, 60000L);// 在线数据统计
		timer.schedule(new RoomCheckTimer(), 600000L, 1800000L);// 房间检测 每30分钟一次
		timer.schedule(new CardTypeCacheTimer(), 60000L, 60000L);// 牌型缓存统
		timer.schedule(new L2CRMIPingTimer(), RMIUtil.RMI_PING_DELAY, RMIUtil.RMI_PING_INTERVAL);// 和中心服连接ping
		// 缓存字典
		loadCache();
	}

	/**
	 * 远程加载缓存
	 */
	public void loadCache() {

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
	}

	public void sendMatchRsp(Player player, MatchClientResponse.Builder response) {
		if (player == null) {
			logger.error("player is null" + ThreadUtil.getStack());
			return;
		}

		Channel channel = player.getChannel();
		if (channel == null) {
			// logger.warn("player.getChannel is null" + ThreadUtil.getStack());
			return;
		}

		ChannelFuture wf = channel.writeAndFlush(PBUtil.toS_S2CRequet(player.getAccount_id(), S2CCmd.MATCH, response));
		wf.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					logger.error("比赛场发送给指定消息失败" + response.getCmd() + player.getProxy_session_id() + "playerID==" + player.getAccount_id()
							+ " player.getChannel()==" + player.getChannel());
				}
			}
		});
	}

	public void send(Player player, Response response) {
		if (player == null) {
			logger.error("player is null" + ThreadUtil.getStack());
			return;
		}

		player.useAi(response);

		if (player.isMatch()) { // 过滤比赛场
			MatchPlayer matchPlayer = (MatchPlayer) player;
			if (matchPlayer.isNoSend()) {
				return;
			}
			sendToMatch(matchPlayer, response);
		} else {
			send0(player, response);
		}
	}

	private void sendToMatch(Player player, Response response) {
		boolean isSend = false;
		int matchStatus = player.getMatchConnectStatus();
		switch (matchStatus) {
		case MatchType.C_STATUS_COMMON:
			isSend = true;
			break;
		case MatchType.C_STATUS_START:
			player.setMatchConnectStatus(MatchType.C_STATUS_END);
			isSend = true;
			break;
		}
		if (isSend) {
			send0(player, response);
		}
	}

	private void send0(Player player, Response response) {
		Channel channel = player.getChannel();
		if (channel == null) {
			return;
		}

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.PROXY);
		requestBuilder.setProxId(player.getProxy_index());
		requestBuilder.setProxSeesionId(player.getProxy_session_id());
		requestBuilder.setExtension(Protocol.response, response);
		if (SystemConfig.gameDebug == 1) {
			// System.out.println("逻辑计算服2Encoder<=========" + response);
			// System.out.println("逻辑服务器写入消息sessionID=" +
			// player.getProxy_session_id() + "playerID==" +
			// player.getAccount_id() + " player.getChannel()=="
			// + player.getChannel());
		}
		ChannelFuture wf = channel.writeAndFlush(requestBuilder.build());
		wf.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					logger.error("逻辑服发送给指定消息失败" + response.getResponseType() + player.getProxy_session_id() + "playerID==" + player.getAccount_id()
							+ " player.getChannel()==" + player.getChannel());
				}
			}
		});
	}

	/**
	 * 新协议，拓展[]
	 * 
	 * @param player
	 * @param response
	 */
	public void sendExMsg(Player player, int cmd, GeneratedMessage.Builder<?> builder) {
		if (player == null) {
			logger.error("player is null" + ThreadUtil.getStack());
			return;
		}

		if (!player.getChannel().isActive()) {
			logger.warn("玩家:[{}]持有的代理服连接已经失效,proxyIndex:{},channel:{}", player, player.getProxy_index(), player.getChannel());
			return;
		}
		Request.Builder reqBuilder = PBUtil.toS_S2CRequet(player.getAccount_id(), cmd, builder);
		player.getChannel().writeAndFlush(reqBuilder);
	}

	public Map<Integer, AbstractRoom> getRoomMap() {
		return roomMap;
	}

	public void setRoomMap(Map<Integer, AbstractRoom> roomMap) {
		this.roomMap = roomMap;
	}

	public Map<Long, Player> getPlayerMap() {
		return playerMap;
	}

	public void setPlayerMap(Map<Long, Player> playerMap) {
		this.playerMap = playerMap;
	}

	/**
	 * 初始化player,同时放入缓存
	 * 
	 * @param logicRoomAccountItemRequest
	 * @param room_id
	 *            代理开房这值为0
	 * @param session
	 * @return
	 */
	public Player createPlayer(LogicRoomAccountItemRequest logicRoomAccountItemRequest, int room_id, Session session) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(logicRoomAccountItemRequest.getCreateTime());
		if (room_id != 0) {
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(logicRoomAccountItemRequest.getAccountId());
			if (player == null) {
				player = new Gamer();
			}
			if (player.getRoom_id() != 0 && player.getRoom_id() != room_id) {
				MongoDBServiceImpl.getInstance().server_error_log(room_id, ELogType.roomIdCreateError, "之前的房间号没被清理" + player.getRoom_id(),
						player.getAccount_id(), player.toString(), 0);

			}

			player.setAccount_id(logicRoomAccountItemRequest.getAccountId());
			player.setGold(logicRoomAccountItemRequest.getGold());
			player.setProxy_index(logicRoomAccountItemRequest.getProxyIndex());
			player.setProxy_session_id(logicRoomAccountItemRequest.getProxySessionId());
			player.setAccount_icon(logicRoomAccountItemRequest.getAccountIcon());
			player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
			player.setAccount_ip_addr(logicRoomAccountItemRequest.getIpAddr());
			player.setNick_name(logicRoomAccountItemRequest.getNickName());
			player.setRoom_id(room_id);
			player.setSex(logicRoomAccountItemRequest.getSex());
			player.setChannel(session.getChannel());
			player.setMoney(logicRoomAccountItemRequest.getMoney());
			player.setLocation_time(System.currentTimeMillis());
			player.setOnline(true);
			player.setAccount_icon(logicRoomAccountItemRequest.getAccountIcon());
			player.setRegisterTime(calendar.getTime());
			LocationInfor locationInfo = logicRoomAccountItemRequest.getLocationInfor();
			if (null != locationInfo) {
				player.locationInfor = locationInfo;
			}

			PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(), player);
			return player;
		} else {

			// 代理开房，这个不能放缓存 会影响代理的房间数据
			Player player = new Gamer();

			player.setAccount_id(logicRoomAccountItemRequest.getAccountId());
			player.setGold(logicRoomAccountItemRequest.getGold());
			player.setProxy_index(logicRoomAccountItemRequest.getProxyIndex());
			player.setProxy_session_id(logicRoomAccountItemRequest.getProxySessionId());
			player.setAccount_icon(logicRoomAccountItemRequest.getAccountIcon());
			player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
			player.setAccount_ip_addr(logicRoomAccountItemRequest.getIpAddr());
			player.setNick_name(logicRoomAccountItemRequest.getNickName());
			// player.setRoom_id(room_id);
			player.setSex(logicRoomAccountItemRequest.getSex());
			player.set_seat_index(GameConstants.INVALID_SEAT);
			player.setRoom_id(0);
			player.setChannel(session.getChannel());
			player.setMoney(logicRoomAccountItemRequest.getMoney());
			player.setOnline(true);
			player.setRegisterTime(calendar.getTime());
			LocationInfor locationInfo = logicRoomAccountItemRequest.getLocationInfor();
			if (null != locationInfo) {
				player.locationInfor = locationInfo;
			}
			// PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(),
			// player);//千万不要放进去--只是一个临时数据不共享
			return player;
		}

	}

	public ClubMatchPlayer createClubMatchPlayer(LogicRoomAccountItemRequest logicRoomAccountItemRequest, int room_id, Session session) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(logicRoomAccountItemRequest.getCreateTime());
		ClubMatchPlayer player = new ClubMatchPlayer();
		player.setAccount_id(logicRoomAccountItemRequest.getAccountId());
		player.setGold(logicRoomAccountItemRequest.getGold());
		player.setProxy_index(logicRoomAccountItemRequest.getProxyIndex());
		player.setProxy_session_id(logicRoomAccountItemRequest.getProxySessionId());
		player.setAccount_icon(logicRoomAccountItemRequest.getAccountIcon());
		player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
		player.setAccount_ip_addr(logicRoomAccountItemRequest.getIpAddr());
		player.setNick_name(logicRoomAccountItemRequest.getNickName());
		player.setRoom_id(room_id);
		player.setSex(logicRoomAccountItemRequest.getSex());
		player.setOnline(false);
		if (session != null) {
			player.setChannel(session.getChannel());
			player.setOnline(true);
		}
		player.setMoney(logicRoomAccountItemRequest.getMoney());
		player.setLocation_time(System.currentTimeMillis());
		
		player.setAccount_icon(logicRoomAccountItemRequest.getAccountIcon());
		player.setRegisterTime(calendar.getTime());
		LocationInfor locationInfo = logicRoomAccountItemRequest.getLocationInfor();
		if (null != locationInfo) {
			player.locationInfor = locationInfo;
		}

		return player;
	}

	/**
	 * 不放入map中
	 */
	public Player getClubCreatePlayer(LogicRoomAccountItemRequest logicRoomAccountItemRequest, Session session) {
		Player player = new Gamer();
		player.setAccount_id(logicRoomAccountItemRequest.getAccountId());
		player.setGold(logicRoomAccountItemRequest.getGold());
		player.setProxy_index(logicRoomAccountItemRequest.getProxyIndex());
		player.setProxy_session_id(logicRoomAccountItemRequest.getProxySessionId());
		player.setAccount_icon(logicRoomAccountItemRequest.getAccountIcon());
		player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
		player.setAccount_ip_addr(logicRoomAccountItemRequest.getIpAddr());
		player.setNick_name(logicRoomAccountItemRequest.getNickName());
		// player.setRoom_id(room_id);
		player.setSex(logicRoomAccountItemRequest.getSex());
		player.set_seat_index(GameConstants.INVALID_SEAT);
		player.setRoom_id(0);
		if (session != null) {
			player.setChannel(session.getChannel());
		}

		player.setMoney(logicRoomAccountItemRequest.getMoney());
		return player;
	}

	public Player createPlayer(long accountId) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AccountSimple targetAccount = centerRMIServer.getSimpleAccount(accountId);

		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(accountId);
		if (player == null) {
			player = new Gamer();
		}
		player.setAccount_id(accountId);
		player.setAccount_icon(targetAccount.getIcon());
		player.setNick_name(targetAccount.getNick_name());
		return player;
	}

	/**
	 * 房卡减操作
	 * 
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel subGold(long account_id, int gold, boolean isExceed, String desc) {
		gold = gold * -1;
		if (gold > 0) {
			AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
			addGoldResultModel.setSuccess(false);
			addGoldResultModel.setMsg("扣卡数量要大于0");
			return addGoldResultModel;
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account_id, gold, isExceed, desc, EGoldOperateType.OPEN_ROOM);

		// 同步状态到代理服
		if (addGoldResultModel.isSuccess() && WealthUtil.roomGoldType.contains(EGoldOperateType.OPEN_ROOM)) {

			RoomWealthProto.Builder builder = WealthUtil.newWealthBuilder(account_id, EWealthCategory.GOLD, EGoldOperateType.OPEN_ROOM.getId(), gold);
			ProxyMsgSender.sendToProxyWithAccountId(account_id, S2SCmd.WEALTH_UPDATE, builder);
		}
		return addGoldResultModel;
	}

	/**
	 * 金币减操作
	 * 
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 * @param desc
	 * @return
	 */
	public AddMoneyResultModel addMoney(long account_id, int gold, boolean isExceed, String desc, EMoneyOperateType eMoneyOperateType) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(account_id, gold, isExceed, desc, eMoneyOperateType);
		return addGoldResultModel;
	}

	/**
	 * 房卡减操作--真实扣房卡记录
	 * 
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 * @param desc
	 * @return
	 */
	public void subRealGold(Room room, int gold, boolean isExceed, String desc) {
		MongoDBServiceImpl.getInstance().log(room, ELogType.addGold, desc, (long) gold, (long) EGoldOperateType.REAL_OPEN_ROOM.getId(), null);
	}

	/**
	 * 记录开房流水
	 * 
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 * @param desc
	 * @return
	 */
	public void roomLogInfo(Room room, int logicIndex, String msg) {
		RoomLogModel roomLogModel = new RoomLogModel();
		roomLogModel.setAccount_id(room.getRoom_owner_account_id());
		roomLogModel.setAppId(room.getGame_id());
		roomLogModel.setClubId(room.club_id);
		roomLogModel.setCreate_time(new Date());
		roomLogModel.setGameTypeIndex(room.getGameTypeIndex());
		roomLogModel.setLogic_id(logicIndex);
		roomLogModel.setMsg(msg);
		roomLogModel.setRoomId(room.getRoom_id());
		MongoDBServiceImpl.getInstance().insert_Model(roomLogModel);
	}

	/**
	 * 房卡减操作--真实扣房卡记录
	 * 
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 * @param desc
	 * @return
	 */
	public void subRobotGold(long account_id, int gold, boolean isExceed, String msg, String groupID, String groupName, int _game_type_index,
			int _game_round, int _game_rule_index, int roomID, String nickNames) {
		MongoDBServiceImpl.getInstance().robot_log(account_id, ELogType.addGold, msg, (long) gold, groupID, groupName, _game_type_index, _game_round,
				_game_rule_index, roomID, nickNames);
	}

	/**
	 * 房卡减操作--真实扣房卡记录
	 * 
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 * @param playerMsg
	 * @param desc
	 * @return
	 */
	public void subClubGold(long account_id, int gold, boolean isExceed, String msg, int clubId, int _game_type_index, int _game_round,
			int _game_rule_index, int roomID, String playerMsg, boolean isExclusiveGold) {
		MongoDBServiceImpl.getInstance().club_log(account_id, ELogType.addGold, msg, gold, clubId, _game_type_index, _game_round, _game_rule_index,
				roomID, playerMsg, isExclusiveGold);
	}

	/**
	 * 房卡操作
	 * 
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 *            是否可以超过库存
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel addGold(long account_id, int gold, boolean isExceed, String desc, EGoldOperateType eGoldOperateType) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account_id, gold, isExceed, desc, eGoldOperateType);

		// 同步状态到代理服
		if (addGoldResultModel.isSuccess() && WealthUtil.roomGoldType.contains(eGoldOperateType)) {
			RoomWealthProto.Builder builder = WealthUtil.newWealthBuilder(account_id, EWealthCategory.GOLD, eGoldOperateType.getId(), gold);
			ProxyMsgSender.sendToProxyWithAccountId(account_id, S2SCmd.WEALTH_UPDATE, builder);
		}

		return addGoldResultModel;
	}

	/**
	 * 因为金币场房间重复利用
	 */
	public int getUnionRoomID() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int room_id = centerRMIServer.moneyRandomRoomId(1);// 随机房间号
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		int index = 0;
		if (room != null) {
			while (true) {
				room_id = centerRMIServer.moneyRandomRoomId(1);// 随机房间号
				room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
				if (room == null) {
					break;
				}
				index++;
				if (index >= 100) {
					logger.error("index>100!!!!!" + index);
					break;
				}
			}
		}
		return room_id;

	}

	public void updateRoomInfo(int room_id) {
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (room == null) {
			return;
		}
		int createType = room.getCreate_type();
		switch (createType) {
		case GameConstants.CREATE_ROOM_MATCH:
			MatchTableService.getInstance().updateRoomInfo(room.id, room);
			break;
		}
	}

	/**
	 * 解散房间
	 * 
	 * @param room_id
	 */
	public void delRoomId(int room_id) {
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return;
		}

		List<Player> allPlayers = room.getAllPlayers();
		allPlayers.forEach(p -> {
			p.setCurRoom(null);
		});
		// RoomRedisModel roomRedisModel =
		// SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
		// room_id + "", RoomRedisModel.class);
		RoomRedisModel roomRedisModel = room.roomRedisModel;
		if (roomRedisModel != null && roomRedisModel.getLogic_index() == SystemConfig.logic_index) {
			// 删除缓存
			SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM, String.valueOf(room_id).getBytes());
		} else {
			MongoDBServiceImpl.getInstance().server_error_log(room_id, ELogType.unkownError, "", 0L,
					roomRedisModel == null ? "roomRedisModel is null"
							: "roomRedisModel.getLogic_index()=" + roomRedisModel.getLogic_index() + "sysconfig=" + SystemConfig.logic_index,
					room.getGame_id());
		}
		Object cache = PlayerServiceImpl.getInstance().getRoomMap().remove(room_id);
		goldRooms.remove(room_id);

		if (null != cache) {
			try {
				SessionServiceImpl.getInstance().sendAllProxy(S2SCmd.RM_ROOM_CACHE,
						DelRoomNotifyProto.newBuilder().setRoomId(room_id).setServerIndex(SystemConfig.logic_index));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (room._recordRoomRecord != null) {
			try {
				room._recordRoomRecord.setGame_end_time(new Date());
				room._recordRoomRecord.setMsg(room._gameRoomRecord.to_json());
				for (int j = 0; j < room.get_players().length; j++) {
					Player player = room.get_players()[j];
					if (player != null) {
						room._recordRoomRecord.getAccountIds().add(player.getAccount_id());
					}
				}
				if (!room.is_sys()) {
					MongoDBServiceImpl.getInstance().updateParenBrand(room._recordRoomRecord);
				}
			} catch (Exception e) {
				logger.error("牌局记录", e);
			}
		}
		try {
			// 新加每人打的总局数记录 --- 有BUG---GAME-TODO
			if (room._recordRoomRecord != null && room.matchId <= 0 && room._recordRoomRecord.isRealKouDou() && room._cur_round >= room._game_round) {
				for (int j = 0; j < room.get_players().length; j++) {
					Player player = room.get_players()[j];
					// 防止出错，只有房间是一样的才清除玩家缓存
					if (player != null && player.getRoom_id() == room_id) {
						long accountId = player.getAccount_id();
						ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
						centerRMIServer.addDayPlayRound(accountId);
					}
				}

			}

			MessageManager.INSTATNCE().sendSettlementMsg(room);
		} catch (Exception e1) {
			logger.error("有BUG---GAME", e1);
		}
		try {
			if (room._recordRoomRecord != null && room._cur_round >= 1 && room.matchId <= 0) {
				SysParamModel sysParamModel2232 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2232);
				if (sysParamModel2232 != null && sysParamModel2232.getVal3() > 0) {
					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					HashMap<String, String> map = new HashMap<>();
					StringBuffer ids = new StringBuffer();
					for (int j = 0; j < room.get_players().length; j++) {
						Player player = room.get_players()[j];
						if (player != null && player.getAccount_id() > 0) {
							if (StringUtils.isNotBlank(ids.toString())) {
								ids.append(",");
							}
							ids.append(player.getAccount_id());
						}
					}
					map.put("account_ids", ids.toString());
					map.put("game_type_index", room.getGameTypeIndex() + "");
					map.put("createType", room.getCreate_type() + "");
					if (room._cur_round >= room._game_round) {
						// 打满
						map.put("all_round", "true");
					}
					centerRMIServer.rmiInvoke(RMICmd.ACCOUNT_PLAY_GAME_LIST, map);
				}
			}
		} catch (Exception e1) {
			logger.error("新增玩家游戏列表", e1);
		}
		if (room.matchId <= 0) {
			try {
				if (room._cur_round > 0) {
					if (StringUtils.isNotBlank(room.groupID)) {
						String winner = "";
						List<HashMap<String, String>> mapList = new ArrayList<HashMap<String, String>>();
						long winnerId = 0;
						for (int i = 0; i < room.get_players().length; i++) {
							Player player = room.get_players()[i];
							if (player != null) {
								// 大赢家
								if (room._player_result.getWin_order()[i] == 0) {
									winnerId = player.getAccount_id();
									winner = player.getNick_name();
								}
								HashMap<String, String> map = new HashMap<String, String>();
								map.put("nickName", player.getNick_name());
								map.put("userId", player.getAccount_id() + "");
								map.put("score", (int) room._player_result.getGame_score()[i] + "分");
								mapList.add(map);
							}
						}
						MongoDBServiceImpl.getInstance().accountBrandResult(room._game_type_index,
								SysGameTypeDict.getInstance().getMJname(room._game_type_index), room.getRoom_owner_account_id(), room.club_id,
								room.groupID, JSON.toJSONString(mapList), room.cost_dou, room.getCreate_type(), room.getRoom_id(), winner,
								room._game_round, winnerId);
					}
				}
			} catch (Exception e) {
				logger.error("大赢家记录", e);
			}
		}

		ClubGameOverProto.Builder clubOverBuilder = null;
		if (room != null) {
			for (int j = 0; j < room.get_players().length; j++) {
				Player player = room.get_players()[j];
				if (player == null)
					continue;

				if (player instanceof MatchPlayer) {
					MatchPlayer matchPlayer = (MatchPlayer) player;
					if (matchPlayer.isLeave() || !matchPlayer.isEnter()) {
						continue;
					}
				}

				// 防止出错，只有房间是一样的才清除玩家缓存
				if (player != null && player.getRoom_id() == room_id) {
					if (room.isClubMatch()) {
						int roomId = SystemRoomUtil.getRoomId(player.getAccount_id());
						if (roomId == room_id) {
							RedisRoomUtil.clearRoom(player.getAccount_id(), room_id);
						}
					} else {
						RedisRoomUtil.clearRoom(player.getAccount_id(), room_id);
					}
				} else {
					MongoDBServiceImpl.getInstance().server_error_log(room_id, ELogType.unkownError, "删除的时候房间对不上", 0L, player.toString(),
							room.getGame_id());
				}
			}
		}
		if (room != null && (room.getCreate_type() == GameConstants.CREATE_ROOM_NORMAL || room.getCreate_type() == GameConstants.CREATE_ROOM_CLUB)) {

			if (room.club_id > 0 && room.isStart) {
				clubOverBuilder = ClubGameOverProto.newBuilder();
				clubOverBuilder.setClubId(room.club_id);
				clubOverBuilder.setRuleId(room.clubInfo.ruleId);
				clubOverBuilder.setRoomId(room.getRoom_id());
				clubOverBuilder.setClubMatchId(room.clubInfo.matchId);
				// 俱乐部数据统计需要以下三个数据
				clubOverBuilder.setCurRound(room._cur_round);
				clubOverBuilder.setGameRound(room._game_round);
				clubOverBuilder.setGameId(room.getGame_id());
			}

			for (int j = 0; j < room.get_players().length; j++) {
				Player player = room.get_players()[j];
				// 防止出错，只有房间是一样的才清除玩家缓存
				if (player != null && player.getRoom_id() == room_id) {

					player.setStatus(PlayerRoomStatus.INVALID);
					if (!room.isClubMatch()) {
						PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
					}

					if (clubOverBuilder != null) {
						GamePlayerProto.Builder b = GamePlayerProto.newBuilder();
						b.setAccountId(player.getAccount_id());
						if (room._player_result != null) {
							b.setScore((int) room._player_result.game_score[player.get_seat_index()]);
						} else {
							logger.error("牌桌的 _player_result 为空 " + room.getClass().getName());
						}

						clubOverBuilder.addWinOrder(room._player_result.win_order[j]);
						clubOverBuilder.addPlayers(b);
					}
				}
			}

			// for (Player player : room.observers().observerCollection())
			// {//---观战者不放入 公共缓存，所以这里就不需要移除，否则有问题
			// if (player != null && player.getRoom_id() == room_id) {
			// PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			// }
			// }

			if (room.club_id > 0 && room._recordRoomRecord != null && room._recordRoomRecord.isRealKouDou()) {
				ClubGameRecordProto.Builder club = ClubGameRecordProto.newBuilder();
				club.setType(RecordProtoType.CLUB_KOU_DOU);
				club.setGameOver(clubOverBuilder);
				ClubKouDouProto.Builder kouDou = ClubKouDouProto.newBuilder();
				kouDou.setClubId(room.club_id);
				kouDou.setGold(room.config_cost_dou);
				// 扣豆分开统计
				if (room.clubInfo.exclusive) {
					kouDou.setWealthCategory(EWealthCategory.EXCLUSIVE_GOLD.category());
				} else {
					kouDou.setWealthCategory(EWealthCategory.GOLD.category());
				}
				// 以开局时间节点来算 by wu_hc 20180418
				kouDou.setCreateTime(room.getStartGameTime());
				club.setKouDou(kouDou);

				GlobalExecutor.asyn_db_execute(new ClubRecordEnsureTask(club));

				// SessionServiceImpl.getInstance().sendClub(1,
				// PBUtil.toS2SRequet(S2SCmd.CLUB_GAME_RECORD_REQ,
				// club).build());
			}

			room.observers().clear();
			room.cost_dou = 0;
		}

		// 如果是代理房间 --在玩家的代理房间中 移除这个房间
		if (room != null && room.getCreate_type() == GameConstants.CREATE_ROOM_PROXY) {

			for (Player player : room.get_players()) {// 5.10
				// 防止出错，只有房间是一样的才清除玩家缓存
				if (player != null && player.getRoom_id() == room_id) {
					player.setRoom_id(0);
					player.set_seat_index(GameConstants.INVALID_SEAT);
				}
			}

			if (room.clubInfo.clubId <= 0) {// 代开房 才用到下面这个东西，没啥鸟用
				// 更新redis
				AccountRedis accountRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT_REDIS,
						room.getRoom_owner_account_id() + "", AccountRedis.class);
				if (accountRedis != null) {
					accountRedis.getProxRoomMap().remove(room.getRoom_id());
					SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT_REDIS, room.getRoom_owner_account_id() + "", accountRedis);
				}
			}

			// 通知代理
			room.refresh_room_redis_data(GameConstants.PROXY_ROOM_RELEASE, true);
		}

		if (clubOverBuilder != null && clubOverBuilder.getPlayersCount() > 0 && room._recordRoomRecord.isRealKouDou()) {
			ClubGameRecordProto.Builder club = null;
			club = ClubGameRecordProto.newBuilder();
			club.setType(RecordProtoType.CLUB_GAME_OVER);
			club.setGameOver(clubOverBuilder);
			ClubKouDouProto.Builder kouDou = ClubKouDouProto.newBuilder();
			kouDou.setCreateTime(room.getStartGameTime());
			kouDou.setClubId(room.club_id);
			kouDou.setGold(room.config_cost_dou);
			club.setKouDou(kouDou);

			GlobalExecutor.asyn_execute(new ClubRecordEnsureTask(club));
			// SessionServiceImpl.getInstance().sendClub(1,
			// PBUtil.toS2SRequet(S2SCmd.CLUB_GAME_RECORD_REQ, club).build());
		}

		if (room.clubInfo.clubId > 0) {
			if (room.isStart) {
				ClubMsgSender.gameOverSnapshotNotify(room);
			}
			ClubMsgSender.roomStatusUpdate(ERoomStatus.END, room);
		}

		if (room.matchId > 0) {
			MatchTableService.getInstance().matchOver(room.id, room);
		}

		if (room.getCreate_type() == GameConstants.CREATE_ROOM_NEW_COIN) {
			CoinService.INTANCE().gameOver(room, room.id);
		}

		try {
			if (room != null && (room.getCreate_type() == GameConstants.CREATE_ROOM_PROXY || room.getCreate_type() == GameConstants.CREATE_ROOM_NORMAL
					|| room.getCreate_type() == GameConstants.CREATE_ROOM_CLUB)) {

				if (room._cur_round >= room._game_round && TurntableDict.getInstance().checkHasActiveModel()) {
					// 全部打完了。没有中途结束，才算入活动中
					List<Long> ids = new ArrayList<>();
					for (int j = 0; j < room.get_players().length; j++) {
						Player player = room.get_players()[j];
						// 防止出错，只有房间是一样的才清除玩家缓存
						if (player != null) {
							ids.add(player.getAccount_id());
						}
					}
					SessionServiceImpl.getInstance()
							.sendMatch(PBUtil.toS2SRequet(S2SCmd.GAME_OVER, TurntableGameOverReq.newBuilder().addAllAccountId(ids)).build());
				}

			}
		} catch (Exception e) {
			logger.error("S2SCmd.GAME_OVER error", e);
		}

		/** 
		 * 改为不通知到俱乐部服了
		try {
			RoomGameOverProto.Builder builder = RoomGameOverProto.newBuilder();
			builder.setRoomId(room.getRoom_id());
			Player[] players = room.get_players();
			for (int i = 0; i < players.length; i++) {
				Player player = players[i];
				if (player != null) {
					builder.addAccountId(player.getAccount_id());
				}
			}
			SessionServiceImpl.getInstance().sendClub(1, PBUtil.toS2SRequet(S2SCmd.ROOM_GAME_OVER_NOTIFY, builder).build());
		} catch (Exception e) {
			logger.error("send roomGameOver to club error", e);
		}
		*/

		handlerRoomActivityMission(room);
		// addAccountBrand(room);
		room.cleanPlayers();
	}

	// private void addAccountBrand(AbstractRoom room) {
	// try {
	// // 非比赛房间个人参与牌局数入库
	// if (room._recordRoomRecord != null && room._cur_round > 0 &&
	// (room.getCreate_type() != GameConstants.CREATE_ROOM_MATCH)) {
	// MongoDBService mongoDBService =
	// SpringService.getBean(MongoDBService.class);
	// int notes_date = Integer.parseInt(DateFormatUtils.format(new Date(),
	// "yyyyMMdd"));
	// for (int j = 0; j < room.get_players().length; j++) {
	// Player player = room.get_players()[j];
	// // 防止出错，只有房间是一样的才清除玩家缓存
	// if (player != null) {
	// mongoDBService.saveAccountDailyBrandStatitistic(player.getAccount_id(),
	// room.getCreate_type(), notes_date,
	// player.getRegisterTime());
	// }
	// }
	// }
	// } catch (Exception e) {
	// logger.error("addAccountBrand error", e);
	// }
	// }

	/**
	 * 处理房间活动任务 所有房间类的相关
	 * 
	 * @param room
	 */
	private void handlerRoomActivityMission(AbstractRoom room) {
		if (room._cur_round < room._game_round) {
			return;
		}
		try {
			// 代表俱乐部开房
			if (room.club_id > 0) {
				// 俱乐部大赢家
				float maxScore = 0;
				Player player = null;
				int playerArrayLength = room.get_players().length;
				for (int i = 0; i < room._player_result.game_score.length; i++) {
					if (maxScore < room._player_result.game_score[i]) {
						maxScore = room._player_result.game_score[i];
					}
					// 处理数组越界问题
					if (i >= playerArrayLength) {
						continue;
					}
					player = room.get_players()[i];
					if (null != player) {
						// 完成俱乐部牌局XX次
						FoundationService.getInstance().sendActivityMissionProcess(player.getAccount_id(), ActivityMissionTypeEnum.CLUB_BOARD_SUMMARY,
								room.getGame_id(), 1);
						// 完成俱乐部XX游戏牌局XX次
						FoundationService.getInstance().sendActivityMissionProcess(player.getAccount_id(),
								ActivityMissionTypeEnum.CLUB_TARGET_GAME_BOARD, room.getGame_id(), 1);
					}
				}
				if (maxScore > 0) {
					for (int i = 0; i < room._player_result.game_score.length; i++) {
						//
						// 大赢家可能存在重复，需要再处理
						if (room._player_result.game_score[i] == maxScore) {
							// 俱乐部大赢家
							FoundationService.getInstance().sendActivityMissionProcess(room.get_players()[i].getAccount_id(),
									ActivityMissionTypeEnum.CLUB_BIG_WINNER, 1, 1);
							// 俱乐部玩XX游戏获得XX次大赢家
							FoundationService.getInstance().sendActivityMissionProcess(room.get_players()[i].getAccount_id(),
									ActivityMissionTypeEnum.CLUB_GAME_BIG_WINNER, room.getGame_id(), 1);
						}
					}
				}

			}
		} catch (Exception e) {
			logger.error("======handlerRoomActivityMission error=====", e);
		}
		try {
			// 完成开房模式牌局达到XX次
			// 目前不结算积分的为普通或者俱乐部模式的开房
			if (!room.isNeedScoreSettle()) {
				// 新功能，需要判断任务是否支持免费游戏
				int gameConsumeType = 0;
				if (room.cost_dou > 0) {
					// 收费游戏
					gameConsumeType = 1;
				}
				// 房间内每个玩家都能完成任务XX次
				Player player = null;
				for (int i = 0; i < room.get_players().length; i++) {
					player = room.get_players()[i];
					// 排除某些位置没人
					if (null != player) {
						FoundationService.getInstance().sendActivityMissionProcess(player.getAccount_id(),
								ActivityMissionTypeEnum.ROOM_BOARD_SUMMARY, gameConsumeType, 1);

						FoundationService.getInstance().sendActivityMissionProcess(player.getAccount_id(),
								ActivityMissionTypeEnum.ROOM_TARGET_GAME_BOARD, room.getGame_id(), 1);
					}
				}
			}
		} catch (Exception e) {
			logger.error("======handlerRoomActivityMission error=====", e);
		}
	}

	/**
	 * 退出房间
	 * 
	 * @param room_id
	 */
	public void quitRoomId(int room_id, long account_id) {
		RedisRoomUtil.clearRoom(account_id, room_id);

		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);// 12.5号修改
		if (room == null) {
			return;
		}
		// ========同步到中心========
		RoomRedisModel roomRedisModel = room.roomRedisModel;
		if (roomRedisModel != null) {
			roomRedisModel.getPlayersIdSet().remove(new Long(account_id));

			Player player = playerMap.get(account_id);
			if (player != null) {

				ClubMsgSender.playerStatusUpdate(ERoomStatus.PLAYER_EXIT, room, player);
				player.setRoom_id(0);// 既然退出了，房间号就应该清理掉--add 6.5号
				player.set_seat_index(GameConstants.INVALID_SEAT);
				roomRedisModel.getNames().remove(player.getNick_name());
				if (player.getAccount_ip() != null) {
					roomRedisModel.getIpSet().remove(player.getAccount_ip());
				}
			} else {
				logger.error("======玩家[{}]离开房间[{}]，但找不到玩家对象！=====", account_id, room_id);
			}
			// 写入redis
			SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

			// 通知redis消息队列,通知开房间的代理
			if (roomRedisModel.isProxy_room()) {
				SessionServiceImpl.getInstance()
						.sendGate(1, PBUtil
								.toS2SRequet(S2SCmd.S_G_S,
										S2STransmitProto.newBuilder().setAccountId(roomRedisModel.getCreate_account_id())
												.setRequest(PBUtil.toS2SResponse(S2SCmd.PROXY_ROOM_STATUS,
														ProxyRoomUpdateProto.newBuilder().setAccountId(roomRedisModel.getCreate_account_id()))))
								.build());
			}
			boolean isRemove = playerMap.remove(account_id, player);
			if (!isRemove) {
				MongoDBServiceImpl.getInstance().server_error_log(room_id, ELogType.playerIdRemove, "玩家对象移除失败", account_id, player.toString(),
						room.getGame_id());
			}
		}

	}

	public LoadingCache<String, HuTypeModel> getCardTypeCache() {
		return cardTypeCache;
	}

	/**
	 * 记录每个玩家的小局次数
	 */
	public void addHistorySmallBrandTimes() {
		// try{
		// for (Player player : this.get_players()) {
		// if(player==null)
		// continue;
		// ICenterRMIServer centerRMIServer =
		// SpringService.getBean(ICenterRMIServer.class);
		// centerRMIServer.addHistorySamllBrandTimes(player.getAccount_id(), 1);
		// }
		// }catch(Exception e){
		// logger.error("error",e);
		// }
	}

	/**
	 * 记录每个玩家的大局次数
	 */
	public void addHistoryBigBrandTimes() {
		// try{
		// for (Player player : this.get_players()) {
		// if(player==null)
		// continue;
		// ICenterRMIServer centerRMIServer =
		// SpringService.getBean(ICenterRMIServer.class);
		// centerRMIServer.addHistoryBigBrandTimes(player.getAccount_id(), 1);
		// }
		// }catch(Exception e){
		// logger.error("error",e);
		// }
	}

	public void sendMatchRspToAllProxy(MatchPlayer player, Builder matchResponse) {

	}

}
