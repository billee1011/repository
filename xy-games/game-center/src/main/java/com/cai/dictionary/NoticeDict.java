package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.Account;
import com.cai.common.domain.GameNoticeModel;
import com.cai.common.domain.common.GameNoticeMsg;
import com.cai.common.domain.json.NoticeDetailJsonModel;
import com.cai.common.thread.HandleMessageExecutorPool;
import com.cai.common.type.NoticeType;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.future.runnable.GameNoticeTask;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RedisServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;

public class NoticeDict {
	private Logger logger = LoggerFactory.getLogger(NoticeDict.class);
	private static NoticeDict dict = new NoticeDict();
	private NoticeDict(){}
	
	public static NoticeDict INSTANCE(){
		return dict;
	}
	
	private HandleMessageExecutorPool executor = new HandleMessageExecutorPool("send-notice-executor",1);
	private Map<Integer, GameNoticeModel> gameNoticeMap = new HashMap<>();
	private Map<Integer, Future<?>> futureMap = new HashMap<>();
	
	public void load(){
		long now = System.currentTimeMillis();
		cancelFutures();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<GameNoticeModel> list = publicService.getPublicDAO().getGameNoticeModelList();
		Map<Integer, GameNoticeModel> tempMap = new HashMap<>();
		for(GameNoticeModel model : list){
			if(model.getEnd_time() == null ||now > model.getEnd_time().getTime()){
				continue;
			}
			model.init();
			tempMap.put(model.getId(), model);
		}
		gameNoticeMap = tempMap;
		startFutures();
		
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.GAME_NOTICE_DICT, gameNoticeMap);
		
		logger.info("load notice success num:{} vaildNum:{} costTime:{}s !!",
				list.size(),tempMap.size(),(System.currentTimeMillis() - now) / 1000);
	}
	
	private void startFutures(){
		Map<Integer, Future<?>> newFutureMap = new HashMap<>();
		gameNoticeMap.keySet().forEach((id) -> {
			startFutures(id,newFutureMap);
		});
		futureMap = newFutureMap;
	}
	
	private void startFutures(int noticeId,Map<Integer, Future<?>> newFutureMap){
		GameNoticeModel model = getModel(noticeId);
		if(model == null || model.isClose()){
			return;
		}
		int type = model.getTrigger_type();
		if(type != NoticeType.TYPE_CLIENT){
			return;
		}
		int delay = model.getDelay();
		GameNoticeTask task = new GameNoticeTask(noticeId);
		if(delay == 0){
			executor.schedule(task, delay * 60 * 1000);
		}else if(delay > 0){
			Future<?> future = executor.scheduleAtFixedMinute(task, delay, delay);
			newFutureMap.put(noticeId, future);
		}
	}
	
	private void cancelFutures(){
		List<Integer> idList = new ArrayList<>();
		idList.addAll(futureMap.keySet());
		for(int noId : idList){
			cancelFuture(noId);
		}
		futureMap.clear();
	}
	
	public void cancelFuture(int id){
		Future<?> future = futureMap.get(id);
		if(future != null){
			future.cancel(true);
		}
	}
	
	public GameNoticeModel getModel(int noticeId){
		GameNoticeModel model = gameNoticeMap.get(noticeId);
		if(model == null){
			logger.error("no find game notice model id={} !",noticeId);
		}
		return model;
	}
	
	public boolean isClose(int noticeId){
		GameNoticeModel model = getModel(noticeId);
		if(model == null){
			return true;
		}
		return model.isClose();
	}
	
	public void sendNotice(GameNoticeMsg msg){
		try{
			int type = msg.getType();
			int typeId = msg.getTypeId();
			List<GameNoticeModel> modelList = getModelByType(type);
			if(modelList .size() <= 0){
				return;
			}
	
			NoticeDetailJsonModel detailMode = null;
			GameNoticeModel newModel = null;
			String content = null;
			for(GameNoticeModel model : modelList){
				if(type == NoticeType.TYPE_MATCH_START){
					detailMode = model.getDetailMode();
					if(detailMode == null || !detailMode.isSame(typeId)){
						continue;
					}
				}
				newModel = model.copy();
				content = newModel.getContent();
				if(msg.getAccountId() > 0){
					Account account = PublicServiceImpl.getInstance().getAccount(msg.getAccountId());
					if(account == null){
						continue;
					}
					content = content.replaceAll(NoticeType.A_NAME, account.getNickName());
				}
				content = content.replaceAll(NoticeType.N_NAME, msg.getName());
				newModel.setContent(content);
				
				sendNotice(newModel);
			}
			
		}catch (Exception e) {
			logger.error("sendNotice content error !!",e);
		}
	}
	
	public void sendNotice(GameNoticeModel model){
		if(model == null || model.isClose()){
			return;
		}
		
		try{
			//时间间隔控制
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.GAME_NOTICE);
			redisResponseBuilder.setRsGameNoticeModelResponse(ModelToRedisUtil.getRsGameNoticeModelResponse(model));
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
			//日志
			StringBuilder buf = new StringBuilder();
			buf.append("公告:id:").append(model.getId()).append(",content:"+model.getContent()).append(",game_type:"+model.getGame_type());
			MongoDBServiceImpl.getInstance().systemLog(ELogType.gameNotice, buf.toString(), null, null, ESysLogLevelType.NONE);
			
		}catch(Exception e){
			logger.error("sendNotice error !! ",e);
			MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
		}
	}
	
	private List<GameNoticeModel> getModelByType(int type){
		List<GameNoticeModel> list = new ArrayList<>();
		gameNoticeMap.values().forEach((model) -> {
			if(!model.isClose() && model.getTrigger_type() == type){
				list.add(model);
			}
		});
		return list;
	}

}
