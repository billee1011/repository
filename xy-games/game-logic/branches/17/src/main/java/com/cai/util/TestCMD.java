package com.cai.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializable;
import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.SpringService;
import com.cai.dictionary.BrandIdDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.TestRunnable;
import com.cai.mongo.service.imp.RoleLogService;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.mongodb.WriteResult;

public class TestCMD {

	private static Logger logger = LoggerFactory.getLogger(TestCMD.class);

	
	public static void cmd(String cmd) {
		System.out.println("输入命令:" + cmd);

		if (cmd != null)
			cmd = cmd.trim();

		if ("".equals(cmd)) {
			System.err.println("=========请输入指令=========");
		} else if ("1".equals(cmd)) {
			System.out.println("测试ok");

		} else if ("2".equals(cmd)) {
			long id = BrandIdDict.getInstance().getId();
			System.out.println(id);
		} else if ("3".equals(cmd)) {
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).get("ROOM_ID_582394",
					RoomRedisModel.class);
			System.out.println(roomRedisModel.getRoom_id());

		} else if ("10".equals(cmd)) {
			SpringService.getBean(RoleLogService.class).test();
			MDC.put("level", "1");
			logger.info("启动了。。。。。", MDC.get("level"));
		} else if ("11".equals(cmd)) {
			GameSchedule.put(new TestRunnable(), 2, TimeUnit.SECONDS);
			GameSchedule.put(new TestRunnable(), 3, TimeUnit.SECONDS);
			GameSchedule.put(new TestRunnable(), 4, TimeUnit.SECONDS);
		}

		else if ("12".equals(cmd)) {
				
			long id0 = BrandIdDict.getInstance().getId();
			
//			long id1 = BrandIdDict.getInstance().getId();
//			MongoDBServiceImpl.getInstance().childBrand(1, id1, id0, "test", null, null);
//			long id2 = BrandIdDict.getInstance().getId();
//			MongoDBServiceImpl.getInstance().childBrand(1, id2, id0, "test", null, null);
//			long id3 = BrandIdDict.getInstance().getId();
//			MongoDBServiceImpl.getInstance().childBrand(1, id3, id0, "test", null, null);
//			long id4 = BrandIdDict.getInstance().getId();
//			MongoDBServiceImpl.getInstance().childBrand(1, id4, id0, "test", null, null);
//			StringBuffer buf = new StringBuffer();
//			buf.append(id1).append("|").append(id2).append("|").append(id3).append("|").append(id4);
//			MongoDBServiceImpl.getInstance().parentBrand(1, id0, buf.toString(), "test", null, null);
		}
		
		else if ("13".equals(cmd)) {
			//位
			long num = 100;
			char[] chs = new char[Long.SIZE];
			for (int i = 0; i < Long.SIZE; i++) {
				chs[Long.SIZE - 1 - i] = (char) (((num >> i) & 1) + '0');

//				if (i == 31) {
//					String ss = new String(chs);
//					System.out.println(ss);
//				}

			}

			System.out.println(new String(chs));
		}
		
		else if("14".equals(cmd)){
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query(Criteria.where("game_id").is(1));
			List list = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
			System.out.println(list.size());
			
			//
			Query query2 = new Query(Criteria.where("brand_id").is(1610131023570010002L));
			Update update = new Update().update("msg", "更新后的值");
			mongoDBService.getMongoTemplate().updateFirst(query, update, BrandLogModel.class);
			
			//mongoDBService.getMongoTemplate().findAndModify(query2, update, entityClass);
			
			
		}
		
		else if("15".equals(cmd)){
			
			
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query();
			query.addCriteria(Criteria.where("brand_id").is(1610201754140010001L));
			query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
			BrandLogModel brandLogModel = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);
			
			System.out.println("sss");
			
			query = new Query();
			query.addCriteria(Criteria.where("brand_id").is(brandLogModel.getBrand_id()));
			query.addCriteria(Criteria.where("log_type").is("parentBrand"));
			Update update = new Update();
			update.set("local_ip", "0.0.0.3");
			update.set("logic_id", "1");
			WriteResult result =  mongoDBService.getMongoTemplate().updateFirst(query, update, BrandLogModel.class);
			
			System.out.println(result);
			
			
			
		}
		
		else if("16".equals(cmd)){
			 long now = System.currentTimeMillis();
			 Map<Integer, Room> roomMap = PlayerServiceImpl.getInstance().getRoomMap();
			 for(Room m : roomMap.values()){
				 long k = now - m.getLast_flush_time();
				 //1小时
				 if(k>0){			 
					 //日志
					 StringBuffer buf = new StringBuffer();
					 buf.append("系统释放房间,房间id:").append(m.getRoom_id()).append(",玩家列表:");
					 int j = 0;
					 for(Player player : m.get_players()){
						 if(player!=null){
							 j++;
							 if(j>1){
								 buf.append("|");
							 }
							 buf.append(player.getAccount_id());
						 }
					 }
					 MongoDBServiceImpl.getInstance().systemLog(ELogType.sysFreeRoom, "", (long)m.getRoom_id(), null, ESysLogLevelType.NONE);
					//TODO 释放房间

					 m.process_release_room();
				 }
			 }
			 
			
		}
		
		else if("17".equals(cmd)){
			
		}
		

	}

}
