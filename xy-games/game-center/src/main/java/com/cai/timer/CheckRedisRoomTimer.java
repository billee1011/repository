package com.cai.timer;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SerializeUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.google.common.collect.Lists;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsRoomResponse;

/**
 * 检测redis上的房间缓存
 * 
 * @author run
 *
 */
public class CheckRedisRoomTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(CheckRedisRoomTimer.class);

	@Override
	public void run() {

		try {

			PerformanceTimer timer = new PerformanceTimer();

			RedisService redisService = SpringService.getBean(RedisService.class);
			Map<byte[], byte[]> map = redisService.hGetAll(RedisConstant.ROOM);
			if (map == null)
				return;

			String redisGetTime = timer.duration();
			timer.reset();
			
			long now = System.currentTimeMillis();
			List<RoomRedisModel> list = Lists.newArrayList();
			Calendar ca = Calendar.getInstance();
			int hour = ca.get(Calendar.HOUR_OF_DAY);// 小时

			StringBuilder buf0 = new StringBuilder();
			buf0.append("释放的房间号:");
			for (byte[] key : map.keySet()) {
				String skey = new String(key);
				byte[] values = map.get(key);
				RoomRedisModel roomRedisModel = (RoomRedisModel) SerializeUtil.unserialize(values);
				if (roomRedisModel == null) {
					logger.error("房间是空的");
					continue;
				}

				// 距创建时间
				long diff_create_time = now - roomRedisModel.getCreate_time();
				// 距最后刷新时间
				long diff_flush_time = now - roomRedisModel.getLast_flush_time();

				if (roomRedisModel.isMoneyRoom()) {
					if (hour == 4) {
						redisService.hDel(RedisConstant.ROOM, key);
						delRoom(roomRedisModel.getRoom_id(), roomRedisModel.getLogic_index());
						list.add(roomRedisModel);
						buf0.append(roomRedisModel.getRoom_id() + ",");
					}
					continue;
				}

				if (roomRedisModel.getCur_player_num() < roomRedisModel.getPlayer_max()) {
					// 没满-大于2小时的释放
					if (diff_create_time > 1000L * 60 * 60 * 2 && diff_flush_time > 1000L * 60 * 60) {
						redisService.hDel(RedisConstant.ROOM, key);
						delRoom(roomRedisModel.getRoom_id(), roomRedisModel.getLogic_index());
						list.add(roomRedisModel);
						buf0.append(roomRedisModel.getRoom_id() + ",");
					}
				} else {
					// 满4人，大于10小时释放
					if (diff_create_time > 1000L * 60 * 60 * 10 && diff_flush_time > 1000L * 60 * 60) {
						redisService.hDel(RedisConstant.ROOM, key);
						delRoom(roomRedisModel.getRoom_id(), roomRedisModel.getLogic_index());
						list.add(roomRedisModel);
						buf0.append(roomRedisModel.getRoom_id() + ",");
					}
				}
			}

			StringBuilder buf = new StringBuilder();
			buf.append("释放过期的房间:获取房间数量:").append(map.size()).append(",释放房间数量:" + list.size()).append(buf0.toString()).append(",序列化耗时" + timer.getStr()+",redis获取房间耗时"+redisGetTime);

			long num = list.size();
			MongoDBServiceImpl.getInstance().systemLog(ELogType.releaseRedisRoom, buf.toString(), num, null, ESysLogLevelType.NONE);
		} catch (Exception e) {
			logger.error("error", e);
			MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
		}
//		MongoDBServiceImpl.getInstance().everyDayRobotModel(0, false);
	}

	private void delRoom(int room_id, int logicIndex) {
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ROOM);
		//
		RsRoomResponse.Builder rsRoomResponseBuilder = RsRoomResponse.newBuilder();
		rsRoomResponseBuilder.setType(1);// 删除房间
		rsRoomResponseBuilder.setRoomId(room_id);
		//
		redisResponseBuilder.setRsRoomResponse(rsRoomResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
				ERedisTopicType.topicLogic.getId() + String.valueOf(logicIndex));

	}

}
