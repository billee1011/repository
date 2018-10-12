package com.cai.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

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
import com.cai.game.AbstractRoom;
import com.cai.game.btz.BTZTable;
import com.cai.game.bullfight.newyy.YynOxTable;
import com.cai.game.chdphz.CHDPHZTable;
import com.cai.game.czbg.CZBGTable;
import com.cai.game.czwxox.CZWXOXTable;
import com.cai.game.dbd.AbstractDBDTable;
import com.cai.game.dbn.DBNTable;
import com.cai.game.ddz.DDZTable;
import com.cai.game.dzd.DZDTable;
import com.cai.game.fkn.FKNTable;
import com.cai.game.fls.FLSTable;
import com.cai.game.gdy.AbstractGDYTable;
import com.cai.game.gxzp.GXZPTable;
import com.cai.game.gzp.GZPTable;
import com.cai.game.hbzp.HBPHZTable;
import com.cai.game.hh.HHTable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hongershi.HongErShiTable;
import com.cai.game.jdb.JDBTable;
import com.cai.game.jxklox.JXKLOXTable;
import com.cai.game.klox.KLOXTable;
import com.cai.game.laopai.AbstractLPTable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.nn.NNTable;
import com.cai.game.paijiu.PJTable;
import com.cai.game.pdk.PDKTable;
import com.cai.game.phu.PHTable;
import com.cai.game.phz.PHZTable;
import com.cai.game.pshox.PSHOXTable;
import com.cai.game.schcp.SCHCPTable;
import com.cai.game.schcpdss.SCHCPDSSTable;
import com.cai.game.schcpdz.SCHCPDZTable;
import com.cai.game.scphz.SCPHZTable;
import com.cai.game.sdh.SDHTable;
import com.cai.game.sg.SGTable;
import com.cai.game.shidianban.SDBTable;
import com.cai.game.shisanzhang.SSZTable;
import com.cai.game.tdz.TDZTable;
import com.cai.game.wmq.WMQTable;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.xpbh.XPBHTable;
import com.cai.game.xykl.XYKLTable;
import com.cai.game.yyox.YYOXTable;
import com.cai.game.yyqf.YYQFTable;
import com.cai.game.zjh.ZJHTable;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.mongodb.WriteResult;

public class TestCMD {

	private static Logger logger = LoggerFactory.getLogger(TestCMD.class);

	public static void cmd(String cmd) {
		System.out.println("输入命令:" + cmd);

		if (cmd != null)
			cmd = cmd.trim();

		if ("".equals(cmd)) {
			System.err.println("=========请输入指令=========");
		} else if ("0".equals(cmd)) {
			System.out.println("麻将关闭调试牌型模式");
			AbstractMJTable.DEBUG_CARDS_MODE = false;
			FLSTable.DEBUG_CARDS_MODE = false;
			HHTable.DEBUG_CARDS_MODE = false;
			HJKTable.DEBUG_CARDS_MODE = false;
			NNTable.DEBUG_CARDS_MODE = false;
			PDKTable.DEBUG_CARDS_MODE = false;
			DDZTable.DEBUG_CARDS_MODE = false;
			WMQTable.DEBUG_CARDS_MODE = false;
			PHTable.DEBUG_CARDS_MODE = false;
			PHZTable.DEBUG_CARDS_MODE = false;
			GXZPTable.DEBUG_CARDS_MODE = false;
			DBNTable.DEBUG_CARDS_MODE = false;
			FKNTable.DEBUG_CARDS_MODE = false;
			AbstractLPTable.DEBUG_CARDS_MODE = false;
			SSZTable.DEBUG_CARDS_MODE = false;
			SDHTable.DEBUG_CARDS_MODE = false;
			TDZTable.DEBUG_CARDS_MODE = false;
			CHDPHZTable.DEBUG_CARDS_MODE = false;
			ZJHTable.DEBUG_CARDS_MODE = false;
			DZDTable.DEBUG_CARDS_MODE = false;
			YYQFTable.DEBUG_CARDS_MODE = false;
			AbstractGDYTable.DEBUG_CARDS_MODE = false;
			SGTable.DEBUG_CARDS_MODE = false;
			JDBTable.DEBUG_CARDS_MODE = false;
			BTZTable.DEBUG_CARDS_MODE = false;
			XYKLTable.DEBUG_CARDS_MODE = false;
			CZWXOXTable.DEBUG_CARDS_MODE = false;
			KLOXTable.DEBUG_CARDS_MODE = false;
			YYOXTable.DEBUG_CARDS_MODE = false;
			GZPTable.DEBUG_CARDS_MODE = false;
			PJTable.DEBUG_CARDS_MODE = false;
			AbstractWSKTable.DEBUG_CARDS_MODE = false;
			AbstractDBDTable.DEBUG_CARDS_MODE = false;
			PSHOXTable.DEBUG_CARDS_MODE = false;
			CZBGTable.DEBUG_CARDS_MODE = false;
			SCHCPTable.DEBUG_CARDS_MODE = false;
			SCHCPDSSTable.DEBUG_CARDS_MODE = false;
			SCHCPDZTable.DEBUG_CARDS_MODE = false;
			JXKLOXTable.DEBUG_CARDS_MODE = false;
			SDBTable.DEBUG_CARDS_MODE = false;
			HBPHZTable.DEBUG_CARDS_MODE = false;
			HongErShiTable.DEBUG_CARDS_MODE = false;
			YynOxTable.DEBUG_CARDS_MODE = false;
			XPBHTable.DEBUG_CARDS_MODE = false;
			AbstractRoom.DEBUG_CARDS_MODE = false;
			SCPHZTable.DEBUG_CARDS_MODE = false;
		} else if ("1".equals(cmd)) {
			System.out.println("麻将开启调试牌型模式");
			AbstractMJTable.DEBUG_CARDS_MODE = true;
			FLSTable.DEBUG_CARDS_MODE = true;
			HHTable.DEBUG_CARDS_MODE = true;
			HJKTable.DEBUG_CARDS_MODE = true;
			NNTable.DEBUG_CARDS_MODE = true;
			PDKTable.DEBUG_CARDS_MODE = true;
			DDZTable.DEBUG_CARDS_MODE = true;
			WMQTable.DEBUG_CARDS_MODE = true;
			PHTable.DEBUG_CARDS_MODE = true;
			PHZTable.DEBUG_CARDS_MODE = true;
			GXZPTable.DEBUG_CARDS_MODE = true;
			DBNTable.DEBUG_CARDS_MODE = true;
			FKNTable.DEBUG_CARDS_MODE = true;
			AbstractLPTable.DEBUG_CARDS_MODE = true;
			SSZTable.DEBUG_CARDS_MODE = true;
			SDHTable.DEBUG_CARDS_MODE = true;
			TDZTable.DEBUG_CARDS_MODE = true;
			CHDPHZTable.DEBUG_CARDS_MODE = true;
			ZJHTable.DEBUG_CARDS_MODE = true;
			DZDTable.DEBUG_CARDS_MODE = true;
			YYQFTable.DEBUG_CARDS_MODE = true;
			AbstractGDYTable.DEBUG_CARDS_MODE = true;
			SGTable.DEBUG_CARDS_MODE = true;
			JDBTable.DEBUG_CARDS_MODE = true;
			BTZTable.DEBUG_CARDS_MODE = true;
			XYKLTable.DEBUG_CARDS_MODE = true;
			CZWXOXTable.DEBUG_CARDS_MODE = true;
			KLOXTable.DEBUG_CARDS_MODE = true;
			YYOXTable.DEBUG_CARDS_MODE = true;
			GZPTable.DEBUG_CARDS_MODE = true;
			PJTable.DEBUG_CARDS_MODE = true;
			AbstractWSKTable.DEBUG_CARDS_MODE = true;
			AbstractDBDTable.DEBUG_CARDS_MODE = true;
			PSHOXTable.DEBUG_CARDS_MODE = true;
			CZBGTable.DEBUG_CARDS_MODE = true;
			SCHCPTable.DEBUG_CARDS_MODE = true;
			SCHCPDSSTable.DEBUG_CARDS_MODE = true;
			SCHCPDZTable.DEBUG_CARDS_MODE = true;
			JXKLOXTable.DEBUG_CARDS_MODE = true;
			SDBTable.DEBUG_CARDS_MODE = true;
			HBPHZTable.DEBUG_CARDS_MODE = true;
			YynOxTable.DEBUG_CARDS_MODE = true;
			AbstractRoom.DEBUG_CARDS_MODE = true;
			HongErShiTable.DEBUG_CARDS_MODE = true;
			XPBHTable.DEBUG_CARDS_MODE = true;
			SCPHZTable.DEBUG_CARDS_MODE = true;
		} else if ("2".equals(cmd)) {
			long id = BrandIdDict.getInstance().getId();
			System.out.println(id);
		} else if ("3".equals(cmd)) {
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).get("ROOM_ID_582394",
					RoomRedisModel.class);
			System.out.println(roomRedisModel.getRoom_id());

		} else if ("10".equals(cmd)) {
		} else if ("11".equals(cmd)) {
			GameSchedule.put(new TestRunnable(), 2, TimeUnit.SECONDS);
			GameSchedule.put(new TestRunnable(), 3, TimeUnit.SECONDS);
			GameSchedule.put(new TestRunnable(), 4, TimeUnit.SECONDS);
		}

		else if ("12".equals(cmd)) {

			long id0 = BrandIdDict.getInstance().getId();

			// long id1 = BrandIdDict.getInstance().getId();
			// MongoDBServiceImpl.getInstance().childBrand(1, id1, id0, "test",
			// null, null);
			// long id2 = BrandIdDict.getInstance().getId();
			// MongoDBServiceImpl.getInstance().childBrand(1, id2, id0, "test",
			// null, null);
			// long id3 = BrandIdDict.getInstance().getId();
			// MongoDBServiceImpl.getInstance().childBrand(1, id3, id0, "test",
			// null, null);
			// long id4 = BrandIdDict.getInstance().getId();
			// MongoDBServiceImpl.getInstance().childBrand(1, id4, id0, "test",
			// null, null);
			// StringBuffer buf = new StringBuffer();
			// buf.append(id1).append("|").append(id2).append("|").append(id3).append("|").append(id4);
			// MongoDBServiceImpl.getInstance().parentBrand(1, id0,
			// buf.toString(), "test", null, null);
		}

		else if ("13".equals(cmd)) {
			// 位
			long num = 100;
			char[] chs = new char[Long.SIZE];
			for (int i = 0; i < Long.SIZE; i++) {
				chs[Long.SIZE - 1 - i] = (char) (((num >> i) & 1) + '0');

				// if (i == 31) {
				// String ss = new String(chs);
				// System.out.println(ss);
				// }

			}

			System.out.println(new String(chs));
		}

		else if ("14".equals(cmd)) {
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query(Criteria.where("game_id").is(1));
			List list = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
			System.out.println(list.size());

			//
			Query query2 = new Query(Criteria.where("brand_id").is(1610131023570010002L));
			Update update = new Update().update("msg", "更新后的值");
			mongoDBService.getMongoTemplate().updateFirst(query, update, BrandLogModel.class);

			// mongoDBService.getMongoTemplate().findAndModify(query2, update,
			// entityClass);

		}

		else if ("15".equals(cmd)) {

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
			WriteResult result = mongoDBService.getMongoTemplate().updateFirst(query, update, BrandLogModel.class);

			System.out.println(result);

		}

		else if ("16".equals(cmd)) {
			long now = System.currentTimeMillis();
			Map<Integer, AbstractRoom> roomMap = PlayerServiceImpl.getInstance().getRoomMap();
			for (Room m : roomMap.values()) {
				long k = now - m.getLast_flush_time();
				// 1小时
				if (k > 0) {
					// 日志
					StringBuffer buf = new StringBuffer();
					buf.append("系统释放房间,房间id:").append(m.getRoom_id()).append(",玩家列表:");
					int j = 0;
					for (Player player : m.get_players()) {
						if (player != null) {
							j++;
							if (j > 1) {
								buf.append("|");
							}
							buf.append(player.getAccount_id());
						}
					}
					MongoDBServiceImpl.getInstance().systemLog(ELogType.sysFreeRoom, "", (long) m.getRoom_id(), null,
							ESysLogLevelType.NONE);
					// TODO 释放房间

					m.force_account();
				}
			}

		}

		else if ("17".equals(cmd)) {
			// CEService.doSystemWork1();
			System.out.println(PtAPIServiceImpl.getInstance().getTengXunPosition(5, 39.983424, 116.322987));

			System.out.println(PtAPIServiceImpl.getInstance().getbaiduPosition(5, 39.983424, 116.322987));
		} else if ("18".equals(cmd)) {
		}

	}

}
