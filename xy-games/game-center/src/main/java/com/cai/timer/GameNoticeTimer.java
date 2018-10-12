package com.cai.timer;

import java.util.List;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.GameNoticeModel;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.RedisServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;

/**
 * 游戏公告
 * @author run
 *
 */
public class GameNoticeTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(GameNoticeTimer.class);
	
	
	@Override
	public void run() {
		
		try{
			long nowTime = System.currentTimeMillis();
			long nowMinuteTime = nowTime/(1000*60L);
			PublicService publicService = SpringService.getBean(PublicService.class);
			List<GameNoticeModel> gameNoticeModellist = publicService.getPublicDAO().getGameNoticeModelList();
			//推给redis消息队列
			for(GameNoticeModel model : gameNoticeModellist){
				
				if(model.getEnd_time()!=null&&model.getEnd_time().getTime()<nowTime) {
					logger.error("特么的，果然有问题。。。");
					continue;
				}
				//时间间隔控制
				if(model.getDelay()!=0 && nowMinuteTime % model.getDelay() == 0){
					
					RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
					redisResponseBuilder.setRsResponseType(RsResponseType.GAME_NOTICE);
					redisResponseBuilder.setRsGameNoticeModelResponse(ModelToRedisUtil.getRsGameNoticeModelResponse(model));
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
					//日志
					StringBuilder buf = new StringBuilder();
					buf.append("公告:id:").append(model.getId()).append(",content:"+model.getContent()).append(",game_type:"+model.getGame_type());
					MongoDBServiceImpl.getInstance().systemLog(ELogType.gameNotice, buf.toString(), null, null, ESysLogLevelType.NONE);
				}
			}
			
		}catch(Exception e){
			logger.error("error",e);
			MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
		}
		
	}

}
