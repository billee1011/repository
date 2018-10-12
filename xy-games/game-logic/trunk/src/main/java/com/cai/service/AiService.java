package com.cai.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.ai.AbstractAi;
import com.cai.ai.AiMsg;
import com.cai.ai.AiRunnable;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.base.BaseTask;
import com.cai.common.domain.Event;
import com.cai.common.thread.HandleMessageExecutorPool;
import com.cai.common.util.LoadPackageClasses;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.RoomResponse;

public class AiService extends AbstractService{
	private static Logger logger = LoggerFactory.getLogger(AiService.class);
	private static final int DEFAULT_ROOM_TYPE = 0;
	
	private final static AiService instance = new AiService();
	
	private final static HandleMessageExecutorPool AI_EXECUTOR = new HandleMessageExecutorPool("AI-executor");
	
	private Map<Integer,Map<Integer, Map<Integer, AbstractAi<?>>>> aiMaps = new HashMap<>(); //key:roomType value:key:gameId value:key:msgId value:ai
	
	private final FieldDescriptor fieldDescriptor;
	
	public AiService(){
		ExtensionRegistry registry = ExtensionRegistry.newInstance();
		Protocol.registerAllExtensions(registry);
		fieldDescriptor = registry.findExtensionByName("roomResponse").descriptor;
	}
	
	public static AiService getInstance() {
		return instance;
	}

	@Override
	protected void startService() {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.game" }, IRootAi.class);
		try {
			List<AiMsg> aiMsgList = new ArrayList<>();
			AiMsg aiMsg = null;
			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				IRootAi cmdAnnotation = cls.getAnnotation(IRootAi.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

				AbstractAi<?> ai = (AbstractAi<?>) cls.newInstance();
				
				int gameType = cmdAnnotation.gameType();
				int[] gameIds = cmdAnnotation.gameIds();
				
				if(gameType < 0 && gameIds.length == 0){
					logger.warn("牌桌ai没配置游戏Id {}",cls.getClass().getName() );
					continue;
				}
				
				int[] msgIds = cmdAnnotation.msgIds();
				if(msgIds.length == 0){
					logger.warn("牌桌ai没配置游戏协议Id {}",cls.getClass().getName() );
					continue;
				}
				int[] exceptGameIds = cmdAnnotation.exceptGameIds();
				
				int roomType = cmdAnnotation.roomType();
				aiMsg = new AiMsg(ai, gameType, gameIds, exceptGameIds, msgIds, roomType);
				aiMsgList.add(aiMsg);
			}
			
			initGameIds(aiMsgList);
			initGameType(aiMsgList);
		}catch (Exception e) {
			logger.error("ai初始化失败", e);
		}
	}
	
	private void initGameType(List<AiMsg> aiMsgList){
//		int[] exceptGameIds = null;
		Set<Integer> gameIdSet = null;
		for(AiMsg msg : aiMsgList){
			gameIdSet = SysGameTypeDict.getInstance().getGameTypeIndexSet(msg.getGameType());
			
			for (int gameId : gameIdSet) {
				initGameId(gameId, msg);
			}
		}
	}
	
	private void initGameIds(List<AiMsg> aiMsgList){
		int gameIds[] = null;
		for(AiMsg msg : aiMsgList){
			gameIds = msg.getGameIds();
			for(int gameId : gameIds){
				initGameId(gameId, msg);
			}
		}
	}
	
	private void initGameId(int gameId, AiMsg msg){
		if(gameId <= 0){
			return;
		}
		
		Map<Integer, AbstractAi<?>> aiMap = getMapByRoomTypeAndGameId(msg.getRoomType(), gameId); 
		
		int msgIds[] = msg.getMsgIds();
		for (int msgId : msgIds) {
			if(aiMap.containsKey(msgId)){
				logger.warn("initGameId->had exist gameId:{} msgId:{} !!",gameId,msgId);
				continue;
			}
			aiMap.put(msgId, msg.getAi());
		}
	}
	
	private Map<Integer, AbstractAi<?>> getMapByRoomTypeAndGameId(int roomType, int gameId){
		Map<Integer, Map<Integer, AbstractAi<?>>> roomTypeMap = aiMaps.get(roomType);
		if(roomTypeMap == null){
			roomTypeMap = new HashMap<>();
			aiMaps.put(roomType, roomTypeMap);
		}
		Map<Integer, AbstractAi<?>> gameIdMap = roomTypeMap.get(gameId);
		if(gameIdMap == null){
			gameIdMap = new HashMap<>();
			roomTypeMap.put(gameId, gameIdMap);
		}
		return gameIdMap;
	}
	
	public Future<?> schedule(long index,BaseTask task,long delay){
		return AI_EXECUTOR.scheduleMSecond(index, task, delay);
	}
	
	public <T extends AbstractRoom> void schedule(long index,AbstractAi<T> handler, RobotPlayer player,
			RoomResponse rsp, T t, int aiFlag, AiWrap aiWrap){
		BaseTask task = new AiRunnable<>(handler, player, rsp, t, aiFlag, aiWrap);
		Future<?> future = schedule(index,task,aiWrap.getDelayTime());
		player.future = future;
		player.task = task;
		player.setPlay_card_time(System.currentTimeMillis());
	}
	
	public void execute(long index,BaseTask task){
		AI_EXECUTOR.execute(index, task);
	}
	
	/**
	 * 优先游戏类型
	 */
	public Map<Integer, AbstractAi<?>> getAiByGameId(int roomType, int gameId){
		Map<Integer, AbstractAi<?>> gameAiMap = getAiByGameId0(roomType, gameId);
		if(gameAiMap == null){
			gameAiMap = getAiByGameId0(DEFAULT_ROOM_TYPE, gameId);
		}
		return gameAiMap;
	}
	
	private Map<Integer, AbstractAi<?>> getAiByGameId0(int roomType, int gameId){
		Map<Integer, AbstractAi<?>> gameAiMap = null;
		Map<Integer, Map<Integer, AbstractAi<?>>> roomTypeMap = aiMaps.get(roomType);
		if(roomTypeMap != null){
			gameAiMap = roomTypeMap.get(gameId);
		}
		return gameAiMap;
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		
	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void sessionFree(Session session) {
		
	}
	
	public RoomResponse get(Response resp){
		return (RoomResponse) resp.getField(fieldDescriptor);
	}

	@Override
	public void dbUpdate(int _userID) {
		
	}
}
