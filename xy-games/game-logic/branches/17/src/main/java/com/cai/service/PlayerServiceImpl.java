package com.cai.service;

import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.HuTypeModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;
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
	
	
	private Map<Long,Player> playerMap = Maps.newConcurrentMap();
	
	/**
	 * 房间缓存 key=房间号  room_id
	 */
	private Map<Integer,Room> roomMap = Maps.newConcurrentMap();
	
	
	
	private Timer timer;
	
	
	
	/**
	 * 牌型记录
	 */
	//缓存接口这里是LoadingCache，LoadingCache在缓存项不存在时可以自动加载缓存
	private LoadingCache<String,HuTypeModel> cardTypeCache
            //CacheBuilder的构造函数是私有的，只能通过其静态方法newBuilder()来获得CacheBuilder的实例
            = CacheBuilder.newBuilder()
            //设置并发级别为8，并发级别是指可以同时写缓存的线程数
            .concurrencyLevel(8)
            //设置写缓存后8秒钟过期
            .expireAfterWrite(24, TimeUnit.HOURS)
            //设置缓存容器的初始容量为10
            .initialCapacity(100000)
            //设置缓存最大容量为10000，超过10000之后就会按照LRU最近虽少使用算法来移除缓存项
            .maximumSize(10000000)
            //设置要统计缓存的命中率
            .recordStats()
//            //设置缓存的移除通知
//            .removalListener(new RemovalListener<Object, Object>() {
//                @Override
//                public void onRemoval(RemovalNotification<Object, Object> notification) {
//                    System.out.println(notification.getKey() + " was removed, cause is " + notification.getCause());
//                }
//            })
            //build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
            .build(
                    new CacheLoader<String, HuTypeModel>() {
                        @Override
                        public HuTypeModel load(String key) throws Exception {
                            System.out.println("load HuTypeModel key : " + key);
                            HuTypeModel huTypeModel = new HuTypeModel();
                            return huTypeModel;
                        }	
                    }
            ); 
	
	
	
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
		timer.schedule(new ProxySocketCheckTimer(), 10000L, 10000L);//代理服链接检测
		timer.schedule(new DataStatTimer(), 60000L, 60000L);//在线数据统计 
		timer.schedule(new RoomCheckTimer(), 600000L, 1800000L);//房间检测  每30分钟一次
		timer.schedule(new CardTypeCacheTimer(), 60000L, 60000L);//牌型缓存统计
		//缓存字典
		loadCache();

	}
	
	
	/**
	 * 远程加载缓存
	 */
	public void loadCache(){
		SysParamDict.getInstance().load();//系统参数
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
	
	public void send(Player player,Response response){
		
		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.PROXY);
		requestBuilder.setProxId(player.getProxy_index());
		requestBuilder.setProxSeesionId(player.getProxy_session_id());
		requestBuilder.setExtension(Protocol.response, response);
		if(SystemConfig.gameDebug==1){
			System.out.println("逻辑计算服2Encoder<========="+response);
		}
		
		
		ChannelFuture wf = player.getChannel().writeAndFlush(requestBuilder.build());
		wf.addListener(new ChannelFutureListener()
		{
			public void operationComplete(ChannelFuture future) throws Exception
			{
				if (!future.isSuccess())
				{
					logger.error("发送给指定玩家消息失败,"+response);
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
	 * @param logicRoomAccountItemRequest
	 * @param room_id
	 * @param session
	 * @return
	 */
	public Player createPlayer(LogicRoomAccountItemRequest logicRoomAccountItemRequest,int room_id,Session session){
		Player player = new Player();
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
		PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(), player);
		return player;
	}
	
	
	
	/**
	 * 房卡减操作
	 * @param account_id
	 * @param gold
	 * @param isExceed
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel subGold(long account_id,int gold,boolean isExceed ,String desc){
		gold = gold * -1;
		if(gold>0){
			AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
			addGoldResultModel.setSuccess(false);
			addGoldResultModel.setMsg("扣卡数量要大于0");
			return addGoldResultModel;
		}
		
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account_id, gold, isExceed, desc,EGoldOperateType.OPEN_ROOM);
		return addGoldResultModel;
	}
	
	/**
	 * 房卡操作
	 * @param account_id
	 * @param gold
	 * @param isExceed 是否可以超过库存
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel addGold(long account_id,int gold,boolean isExceed ,String desc,EGoldOperateType eGoldOperateType){
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account_id, gold, isExceed, desc,eGoldOperateType);
		return addGoldResultModel;
	}
	
	/**
	 * 解散房间
	 * @param room_id
	 */
	public void delRoomId(int room_id){
		
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if(room!=null){
			for(Player player : room.get_players()){
				//防止出错，只有房间是一样的才清除玩家缓存
				if(player!=null && player.getRoom_id()==room_id){
					PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
				}
			}
			PlayerServiceImpl.getInstance().getRoomMap().remove(room_id);
		}
		
		//========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ROOM);
		//
		RsRoomResponse.Builder rsRoomResponseBuilder = RsRoomResponse.newBuilder();
		rsRoomResponseBuilder.setType(1);
		rsRoomResponseBuilder.setRoomId(room_id);
		//
		redisResponseBuilder.setRsRoomResponse(rsRoomResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
		
	}
	
	/**
	 * 退出房间
	 * @param room_id
	 */
	public void quitRoomId(long account_id){
		
		//========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ROOM);
		//
		RsRoomResponse.Builder rsRoomResponseBuilder = RsRoomResponse.newBuilder();
		rsRoomResponseBuilder.setType(2);
		rsRoomResponseBuilder.setAccountId(account_id);
		//
		redisResponseBuilder.setRsRoomResponse(rsRoomResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
		
		//删除本地缓存
		PlayerServiceImpl.getInstance().getPlayerMap().remove(account_id);
		
	}

	public LoadingCache<String, HuTypeModel> getCardTypeCache() {
		return cardTypeCache;
	}
	
	

	
	

	

	

}
