package com.cai.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountPushModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ClubExclusiveGoldLogModel;
import com.cai.common.domain.ClubExclusiveGoldTjResultModel;
import com.cai.common.domain.DayCardResult;
import com.cai.common.domain.ExclusiveGoldLogModel;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.InviteTjModel;
import com.cai.common.domain.LogicStatusModel;
import com.cai.common.domain.ProxyAccountReplaceRoomModel;
import com.cai.common.domain.ProxyAccountStatModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.AccountGroupModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.IClubRMIServer;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.common.rmi.vo.UpdateClubIdVo;
import com.cai.common.util.HttpClientUtils;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.AccountPushServiceImp;
import com.cai.service.MongoDBService;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RMIServiceImpl;
import com.cai.service.RecommendService;
import com.cai.service.TaskService;
import com.google.common.collect.Lists;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

public class TestCMD {

	private static Logger logger = LoggerFactory.getLogger(TestCMD.class);

	public static void cmd(String cmd) {
		System.out.println("输入命令:" + cmd);

		if (cmd != null)
			cmd = cmd.trim();

		if ("".equals(cmd)) {
			System.err.println("=========请输入指令=========");
		} else if ("0".equals(cmd)) {
			System.exit(0);
		}

		else if ("1".equals(cmd)) {
			System.out.println("测试ok");
			PublicService publicService = SpringService.getBean(PublicService.class);
			publicService.getPublicDAO().updateAppItemZeroFlag(9999999);
			SpringService.getBean(ICenterRMIServer.class).reLoadAppItemDictionary();

		} else if ("2".equals(cmd)) {
			Date targetDate = new Date();
			Aggregation aggregation = Aggregation.newAggregation(
					Aggregation.match(Criteria.where("log_type").is(ELogType.parentBrand.getId()).and("create_time")
							.gte(MyDateUtil.getZeroDate(targetDate)).lte(MyDateUtil.getTomorrowZeroDate(targetDate)).and("match_id").is(0)),
					Aggregation.group("v1", "v2").count().as("total").last("v1").as("v1").last("v2").as("v2"));
			AggregationResults<DayCardResult> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
					BrandLogModel.class, DayCardResult.class);
			List<DayCardResult> list = models.getMappedResults();
			System.out.println(JSON.toJSONString(list));
		} else if ("3".equals(cmd)) {
			AggregationOperation match = Aggregation
					.match(Criteria.where("account_id").is(10060).and("log_type").is(ELogType.agentIncome.getId()).
							and("create_time").gte(MyDateUtil.getZeroDate(DateUtils.addDays(new Date(), -3))).lt(MyDateUtil.getTomorrowZeroDate(new Date())));
			AggregationOperation group = Aggregation.group("target_id").sum("recharge_money").as("count").sum("v1").as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend", GiveCardModel.class);
			List<GiveCardModel> sumLlist = result.getMappedResults();
			HashMap<Long, Long> map = new HashMap<Long, Long>();
			if (sumLlist != null && sumLlist.size() > 0) {
				for (GiveCardModel giveCardModel : sumLlist) {
					map.put(giveCardModel.get_id(), giveCardModel.getCount());
				}
			}
		} else if ("10".equals(cmd)) {

			ICenterRMIServer centerRMIServer = (ICenterRMIServer) SpringService.getBean("centerRMIServerImpl");
			centerRMIServer.updateAccountId(18827, 1);

		} else if ("11".equals(cmd)) {

			ILogicRMIServer logicRMIServer = RMIServiceImpl.getInstance().getLogicRMIServerMap().get(1);
			System.out.println(logicRMIServer.sayHello());

			IProxyRMIServer proxyRMIServer = RMIServiceImpl.getInstance().getProxyRMIServerMap().get(1);
			System.out.println(proxyRMIServer.sayHello());

		} else if ("12".equals(cmd)) {

			for (ILogicRMIServer logicRMIServer : RMIServiceImpl.getInstance().getLogicRMIServerMap().values()) {
				try {
					System.out.println(logicRMIServer.sayHello());
				} catch (Exception e) {
					logger.error("error", e);
				}
			}

		} else if ("13".equals(cmd)) {
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);

			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(222L);
			rsAccountResponseBuilder.setGameId(10);

			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder.build());

			System.out.println(redisResponseBuilder.build());
			byte[] bytes = redisResponseBuilder.build().toByteArray();

			RedisTemplate redisTemplate = SpringService.getBean("redisTemplate", RedisTemplate.class);
			for (int i = 0; i < 1000; i++)
				redisTemplate.convertAndSend("java3", bytes);

		} else if ("14".equals(cmd)) {
			// GameLogModel gameLogModel = new GameLogModel();
			// gameLogModel.setAccount_id(555L);
			// gameLogModel.setMsg("this is test2");
			// MongoDBService mongoDBService =
			// SpringService.getBean(MongoDBService.class);
			// mongoDBService.getMongoTemplate().insert(gameLogModel);

			// MongoDBService mongoDBService =
			// SpringService.getBean(MongoDBService.class);
			// Query query = new Query();
			// query.addCriteria(Criteria.where("_id").is("5813633bd8208ba3f801161e"));
			// List<GameLogModel> list =
			// mongoDBService.getMongoTemplate().find(query,
			// GameLogModel.class);

			PublicService publicService = SpringService.getBean(PublicService.class);
			int num = publicService.getPublicDAO().getAccountNum();
			System.out.println("总注册人数：" + num);
			Date endDate = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(System.currentTimeMillis() - 24 * 60 * 60 * 1000L * 2);
			Date beginDate = calendar.getTime();
			int numCreate = publicService.getPublicDAO().getAccountCreateNumByTime(beginDate, endDate);
			System.out.println("指定时间注册人数：" + numCreate);

			int numActive = publicService.getPublicDAO().getAccountActiveOnlineNum(beginDate, endDate);
			System.out.println("指定时间活跃账号数量：" + numCreate);

			TaskService tastService = SpringService.getBean(TaskService.class);
			tastService.taskZero();

		} else if ("15".equals(cmd)) {

			SpringService.getBean(TaskService.class).everyDayAccount(-5);
			SpringService.getBean(TaskService.class).keepRate();

		} else if ("16".equals(cmd)) {
			// CEService.doSystemWork1();
			// try {
			// SmsService.getInstance().sendSms("13026640938",
			// "{\"code\":\"123456\"}", "SMS_115265235", "");
			// } catch (ClientException e) {
			// e.printStackTrace();
			// }
			AccountPushModel model = new AccountPushModel();
			model.setAccount_id(9300l);
			model.setPlat(1);
			model.setEquipment_id("fadfalfjlasdjflkajflkdajfeh");
			AccountPushServiceImp.getInstance().addAccountPushModel(model);
		} else if ("17".equals(cmd)) {
			// SpringService.getBean(TaskService.class).resetTodayAccountParam();
			long accountId = 150669;
			// List<WxGroups> list = new ArrayList<>();
			SysParamModel sysParamModel2233 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2233);
			String path = "";
			int hallId = 0;
			if (sysParamModel2233 == null) {
				path = "http://39.108.11.126:8018/web/group/list";
				hallId = 80008;
			} else {
				hallId = sysParamModel2233.getVal1();
				path = sysParamModel2233.getStr1();
			}
			path = path + "?hallId=" + hallId + "&userId=" + accountId;
			try {
				logger.info(path);
				String res = HttpClientUtils.get(path);
				JSONObject json = JSONObject.parseObject(res);
				// json.getString("data")
				List<AccountGroupModel> list = JSON.parseArray(json.getString("data"), AccountGroupModel.class);
				System.out.println(list);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if ("18".equals(cmd)) {

			Date targetDate = new Date();
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				targetDate = sdf.parse("2017-03-20");
			} catch (Exception e) {
				e.printStackTrace();
			}

			List<ProxyAccountStatModel> proxyAccountStatModelList = Lists.newArrayList();
			Aggregation aggregation = Aggregation.newAggregation(
					Aggregation.match(Criteria.where("target_proxy_account").is(0).and("create_time").gte(MyDateUtil.getZeroDate(targetDate))
							.lte(MyDateUtil.getTomorrowZeroDate(targetDate))),
					Aggregation.group("account_id").sum("give_num").as("count").last("account_id").as("account_id"));
			AggregationResults<Map> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation, "proxy_gold_log",
					Map.class);
			for (Map obj : models) {
				ProxyAccountStatModel pstatModel = new ProxyAccountStatModel();
				pstatModel.setAccount_id((long) obj.get("account_id"));
				pstatModel.setNick_name("--");
				pstatModel.setGive_gold_num((long) obj.get("count"));
				pstatModel.setInvite_num(1);
				proxyAccountStatModelList.add(pstatModel);
			}
		} else if ("19".equals(cmd)) {

			try {
				PerformanceTimer timer = new PerformanceTimer();
				// 日期
				Date now = new Date();
				Date targetDate = DateUtils.addDays(now, -1);
				int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
				// System.out.println(targetDateInt);
				Account account = PublicServiceImpl.getInstance().getAccount(2044);

				ProxyAccountReplaceRoomModel pmodel = new ProxyAccountReplaceRoomModel();
				pmodel.setAccount_id(2438l);
				pmodel.setNotes_date(targetDateInt);
				pmodel.setToday_consume(100);
				SpringService.getBean(MongoDBService.class).getMongoTemplate().insert(pmodel);

				// SpringService.getBean(TaskService.class).proxAccountStat(-1);

				// SpringService.getBean(TaskService.class).everyDayRobotModel(-1);
				//
				// EveryDayRobotModel robotModel = new EveryDayRobotModel();
				// robotModel.setNotes_date(20170504);
				// robotModel.setAccount_id(1916l);
				// robotModel.setMsg("");
				// robotModel.setGroupId("asda1");
				// robotModel.setGoldTotal(2);
				// SpringService.getBean(MongoDBService.class).getMongoTemplate().insert(robotModel);

			} catch (Exception e) {
				logger.error("error", e);
			}
		} else if ("20".equals(cmd)) {
			String user_ID = "9289";
			String target_ID = "9300";
			String opType = "1";// 0取消推广员身份，1设置为下级推广员
			// 验证必填参数格式
			long userID;
			Long targetID = null;
			int type = 0;
			try {
				userID = Long.parseLong(user_ID);
				targetID = Long.parseLong(target_ID);
				type = Integer.parseInt(opType);
			} catch (Exception e) {
				return;
			}
			Account account = PublicServiceImpl.getInstance().getAccount(userID);
			if (account == null) {
				return;
			}
			Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
			if (targetAccount == null) {
				return;
			}
			if (targetAccount.getAccountModel().getRecommend_id() != userID) {
				return;
			}
			RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
			rsAccountModelResponse.setAccountId(targetID);
			rsAccountModelResponse.setRecommendLevel(type);
			SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
		} else if ("21".equals(cmd)) {
			// ICenterRMIServer centerRMIServer = (ICenterRMIServer)
			// SpringService.getBean("centerRMIServerImpl");
			// Map<String, Object> map =
			// centerRMIServer.queryDownRecommendByMonth(9288l, "20170501");
			// System.out.println(JSONObject.toJSON(map));
			String account_id = "9289";
			try {
				Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryRecommendAll(Long.parseLong(account_id));
				System.out.println(JSONObject.toJSON(detailMap));
			} catch (Exception e) {
				logger.error("doRecommendAll插入日志异常" + e);
			}

		} else if ("22".equals(cmd)) {
			Date startDate = null;
			Date endDate = null;
			try {
				String targetDateStr = "20170501";
				startDate = MyDateUtil.getMinMonthDate(targetDateStr);
				endDate = MyDateUtil.getMaxMonthDate(targetDateStr);
			} catch (Exception e) {
				return;
			}
			String account_id = "9289";
			Query query = new Query();
			query.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
			query.addCriteria(Criteria.where("log_type").is(ELogType.recommendIncome));
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(MyDateUtil.getTomorrowZeroDate(endDate)));
			BasicDBList values = new BasicDBList();
			values.add(3);
			values.add(4);
			query.addCriteria(Criteria.where("v2").in(values));
			query.with(new Sort(Direction.DESC, "create_time"));
			List<GameLogModel> recommendIncomeList = SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query, GameLogModel.class);
			System.out.println(JSONObject.toJSON(recommendIncomeList));
		} else if ("23".equals(cmd)) {
			// MongoDBServiceImpl.getInstance().getTotalRobotGold(1916L);
			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, -1);
			// Aggregation aggregation = Aggregation.newAggregation(
			// Aggregation.match(Criteria.where("account_id").is(13235l).and("log_type")
			// .is("addGold").and("create_times").gte(MyDateUtil.getZeroDate(targetDate))
			// .lte(MyDateUtil.getTomorrowZeroDate(targetDate)).and("v2")
			// .is((long) EGoldOperateType.REAL_OPEN_ROOM.getId())),
			// Aggregation.group("account_id").sum("v1").as("total").last("account_id").as("account_id"));
			AggregationOperation match = Aggregation.match(
					Criteria.where("account_id").is(13284L).and("log_type").is("addGold").and("create_time").gte(MyDateUtil.getZeroDate(targetDate))
							.lte(MyDateUtil.getTomorrowZeroDate(targetDate)).and("v2").is((long) EGoldOperateType.REAL_OPEN_ROOM.getId()));
			AggregationOperation group = Aggregation.group().sum("v1").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			AggregationResults<GiveCardModel> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
					"game_log", GiveCardModel.class);
			List<GiveCardModel> list = models.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				System.out.println(JSONObject.toJSON(giveCardModel));
			}
		} else if ("24".equals(cmd)) {

		} else if ("25".equals(cmd)) {
			String user_ID = "9301";// 自己的id
			String target_ID = "9300";// 代理推荐人id
			// 验证必填参数格式
			long userID;
			Long targetID = null;
			try {
				userID = Long.parseLong(user_ID);
				targetID = Long.parseLong(target_ID);
			} catch (Exception e) {
				return;
			}
			Account account = PublicServiceImpl.getInstance().getAccount(userID);
			if (account == null || account.getAccountModel().getProxy_level() == 0) {
				return;
			}
			int level = account.getAccountModel().getProxy_level();
			if (level == 1) {
				return;
			}
			Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
			if (targetAccount == null || targetAccount.getAccountModel().getProxy_level() == 0) {
				return;
			}
			if (level == targetAccount.getAccountModel().getProxy_level()) {
				return;
			}
			if (level - 1 != targetAccount.getAccountModel().getProxy_level()) {
				return;
			}

			RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
			rsAccountModelResponse.setAccountId(userID);
			rsAccountModelResponse.setAgentRecommentId(targetID);
			SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
		} else if ("26".equals(cmd)) {
			
		} else if ("27".equals(cmd)) {
			BasicDBList values = new BasicDBList();
			values.add(9288);
			values.add(9300);
			values.add(9301);
			values.add(9309);
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").in(values).and("log_type").is(ELogType.agentIncome.getId()));
			AggregationOperation group = Aggregation.group("account_id").sum("recharge_money").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "hall_recommend",
					GiveCardModel.class);
			List<GiveCardModel> sumLlist = result.getMappedResults();
			long totalBalance = 0;// 累计充值金额
			if (sumLlist != null && sumLlist.size() > 0) {
				GiveCardModel giveCardModel = sumLlist.get(0);
				totalBalance = giveCardModel.getCount();
			}
		} else if ("28".equals(cmd)) {
			DBObject groupFields = new BasicDBObject("_id", "$account_id");
			groupFields.put("today_consume", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			// sort
			DBObject sort = new BasicDBObject("$sort", new BasicDBObject("today_consume", -1));
			// limit
			DBObject limit = new BasicDBObject("$limit", 10);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationOutput output = mongoDBService.getMongoTemplate().getCollection("proxy_account_replace_room").aggregate(group, sort, limit);
			Iterable<DBObject> list = output.results();
			HashMap<Long, Long> map = new HashMap<Long, Long>();
			for (DBObject dbObject : list) {
				map.put(Long.parseLong(String.valueOf(dbObject.get("_id"))), Long.parseLong((String.valueOf(dbObject.get("today_consume")))));
			}
		} else if ("29".equals(cmd)) {
			RedPackageRuleDict.getInstance().load();
		} else if ("30".equals(cmd)) {
			UpdateClubIdVo vo = SpringService.getBean(IClubRMIServer.class).rmiInvoke(RMICmd.UPDATE_CLUB_ID, UpdateClubIdVo.newVo(9999999, 9999991));
			System.err.println(vo);
		} else if ("31".equals(cmd)) {
			ICenterRMIServer centerRMIServer = (ICenterRMIServer) SpringService.getBean("centerRMIServerImpl");
			centerRMIServer.addAccountGold(13273L, 10, true, "转厕所过户", EGoldOperateType.TURNTABLE);
		} else if ("32".equals(cmd)) {
			List<LogicStatusModel> logs = SpringService.getBean(ICenterRMIServer.class).getLogicStatusList();
			System.out.println(logs);
		}else if("33".equals(cmd)){
			SpringService.getBean(ICenterRMIServer.class).reLoadSysParamModelDictionary();
		}else if("34".equals(cmd)){
			Fields fields = Fields.fields("clubId","appId");
			Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("operateType")
					.is(EGoldOperateType.REAL_OPEN_ROOM.getId())),Aggregation.group(fields)
					.count().as("openRoomCount").sum("v3").as("cost").last("clubId").as("clubId").last("appId").as("appId"));
			AggregationResults<ClubExclusiveGoldTjResultModel> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
					"club_exclusive_gold_log", ClubExclusiveGoldTjResultModel.class);
			List<ClubExclusiveGoldTjResultModel> list = models.getMappedResults();
			System.out.println(JSON.toJSONString(list));
		}else  if("35".equalsIgnoreCase(cmd)){
			ICenterRMIServer centerRMIServer = (ICenterRMIServer) SpringService.getBean("centerRMIServerImpl");
			centerRMIServer.testCard("coin#1#1#0x01,0x11,0x21,0x31,0x13,0x03,0x23,0x33,0x15,0x05,0x37,0x28,0x38,0x1D,0x2D,0x3D#11001");
		}

	}

}
