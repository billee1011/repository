package com.cai.service;

import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.HuTypeModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;
import com.cai.redis.service.RedisService;
import com.cai.timer.CardTypeCacheTimer;
import com.cai.timer.DataStatTimer;
import com.cai.timer.ProxySocketCheckTimer;
import com.cai.timer.RoomCheckTimer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsRoomResponse;

public class PlayerServiceImpl extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);
	private static PlayerServiceImpl instance = null;

	private Map<Long, Player> playerMap = Maps.newConcurrentMap();

	/**
	 * 房间缓存 key=房间号 room_id
	 */
	private Map<Integer, Room> roomMap = Maps.newConcurrentMap();

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
		// 缓存字典
		loadCache();

	}

	/**
	 * 远程加载缓存
	 */
	public void loadCache() {
		SysParamDict.getInstance().load();// 系统参数
		GoodsDict.getInstance().load();
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

	public void send(Player player, Response response) {
		if (player == null) {
			logger.error("player is null" + ThreadUtil.getStack());
			return;
		}
		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.PROXY);
		requestBuilder.setProxId(player.getProxy_index());
		requestBuilder.setProxSeesionId(player.getProxy_session_id());
		requestBuilder.setExtension(Protocol.response, response);
		if (SystemConfig.gameDebug == 1) {
			System.out.println("逻辑计算服2Encoder<=========" + response);
			System.out.println("逻辑服务器写入消息sessionID=" + player.getProxy_session_id() + "playerID==" + player.getAccount_id() + " player.getChannel()=="
					+ player.getChannel());
		}
		ChannelFuture wf = player.getChannel().writeAndFlush(requestBuilder.build());
		wf.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					logger.error("发送给指定玩家消息失败" + response.getResponseType());
				}
			}
		});

	}

	public Map<Integer, Room> getRoomMap() {
		return roomMap;
	}

	public void setRoomMap(Map<Integer, Room> roomMap) {
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
		if (room_id != 0) {
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(logicRoomAccountItemRequest.getAccountId());
			if (player == null) {
				player = new Player();
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
			player.setOnline(true);
			PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(), player);
			return player;
		} else {
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(logicRoomAccountItemRequest.getAccountId());
			if (player == null) {
				player = new Player();
			}
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
			PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(), player);
			return player;
		}

	}

	public Player createPlayer(long accountId) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account targetAccount = centerRMIServer.getAccount(accountId);

		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(accountId);
		if (player == null) {
			player = new Player();
		}
		player.setAccount_id(accountId);
		player.setGold(targetAccount.getAccountModel().getGold());
		player.setProxy_index(1);
		player.setProxy_session_id(1);
		player.setAccount_icon(targetAccount.getAccountWeixinModel().getHeadimgurl());
		// player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
		// player.setAccount_ip_addr(logicRoomAccountItemRequest.getIpAddr());
		player.setNick_name(targetAccount.getNickName());
		// player.setRoom_id(room_id);
		// player.setSex(targetAccount.getAccountWeixinModel().getSex());
		// player.setChannel(session.getChannel());
		PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(), player);
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
	public void subRealGold(long account_id, int gold, boolean isExceed, String desc) {
		MongoDBServiceImpl.getInstance().log(account_id, ELogType.addGold, desc, (long) gold, (long) EGoldOperateType.REAL_OPEN_ROOM.getId(), null);
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
			int _game_round, int _game_rule_index, int roomID) {
		MongoDBServiceImpl.getInstance().robot_log(account_id, ELogType.addGold, msg, (long) gold, groupID, groupName, _game_type_index, _game_round,
				_game_rule_index, roomID);
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

	/**
	 * 解散房间
	 * 
	 * @param room_id
	 */
	public void delRoomId(int room_id) {

		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (room != null && room.getCreate_type() == GameConstants.CREATE_ROOM_NORMAL) {
			for (Player player : room.get_players()) {
				// 防止出错，只有房间是一样的才清除玩家缓存
				if (player != null && player.getRoom_id() == room_id) {
					PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
				}
			}

			for (Player player : room.observers().observerCollection()) {
				if (player != null && player.getRoom_id() == room_id) {
					PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
				}
			}
			room.observers().clear();
		}

		PlayerServiceImpl.getInstance().getRoomMap().remove(room_id);

		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ROOM);
		//
		RsRoomResponse.Builder rsRoomResponseBuilder = RsRoomResponse.newBuilder();
		rsRoomResponseBuilder.setType(1);// 删除房间
		rsRoomResponseBuilder.setRoomId(room_id);
		//
		redisResponseBuilder.setRsRoomResponse(rsRoomResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

		// 如果是代理房间 --在玩家的代理房间中 移除这个房间
		if (room != null && room.getCreate_type() == GameConstants.CREATE_ROOM_PROXY) {

			for (Player player : room.get_players()) {// 5.10
				// 防止出错，只有房间是一样的才清除玩家缓存
				if (player != null && player.getRoom_id() == room_id) {
					player.setRoom_id(0);
					player.set_seat_index(GameConstants.INVALID_SEAT);
				}
			}

			// 更新redis
			AccountRedis accountRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT_REDIS,
					room.getRoom_owner_account_id() + "", AccountRedis.class);
			if (accountRedis != null) {
				accountRedis.getProxRoomMap().remove(room.getRoom_id());
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT_REDIS, room.getRoom_owner_account_id() + "", accountRedis);
			}

			// 通知代理
			room.refresh_room_redis_data(GameConstants.PROXY_ROOM_RELEASE, true);
		}

	}

	/**
	 * 退出房间
	 * 
	 * @param room_id
	 */
	public void quitRoomId(int room_id, long account_id) {

		// ========同步到中心========
		RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);
		if (roomRedisModel != null) {
			roomRedisModel.getPlayersIdSet().remove(new Long(account_id));
			// 写入redis
			SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);
		}

		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ROOM);
		//
		RsRoomResponse.Builder rsRoomResponseBuilder = RsRoomResponse.newBuilder();
		rsRoomResponseBuilder.setType(2);
		rsRoomResponseBuilder.setAccountId(account_id);
		rsRoomResponseBuilder.setRoomId(room_id);
		//
		redisResponseBuilder.setRsRoomResponse(rsRoomResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);

		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (room != null && room.getCreate_type() == GameConstants.CREATE_ROOM_PROXY && account_id == room.getRoom_owner_account_id()) {

		} else {
			// 删除本地缓存
			// PlayerServiceImpl.getInstance().getPlayerMap().remove(account_id);
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

}
