package com.cai.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.Account;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChannelModel;
import com.cai.common.domain.RankModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.Signature;
import com.cai.common.util.SpringService;
import com.cai.common.util.XMLParser;
import com.cai.core.Global;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.dictionary.ChannelModelDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.IpFirewallModel;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.cai.service.FirewallServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.TaskService;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MyTestResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

public class TestCMD {

	private static Logger logger = LoggerFactory.getLogger(TestCMD.class);

	public static void cmd(String cmd) {
		System.out.println("输入命令:" + cmd);

		if (cmd != null)
			cmd = cmd.trim();

		if ("".equals(cmd)) {
			System.err.println("=========请输入指令=========");
		}

		if ("0".equals(cmd)) {
			System.exit(0);
		}

		else if ("1".equals(cmd)) {
			System.out.println("测试ok");
			SpringService.getBean(TaskService.class).taskZero();

		} else if ("2".equals(cmd)) {
			System.out.println("当前业务处理排队情况:" + RequestHandlerThreadPool.getInstance().getBlockQueue().size());

		} else if ("3".equals(cmd)) {
			SpringService.getBean(RedisService.class).test();
		}

		else if ("4".equals(cmd)) {

			PerformanceTimer timer = new PerformanceTimer();
			for (int i = 0; i < 1; i++) {

				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				// centerRMIServer.sayHello();
				System.out.println(centerRMIServer.randomRoomId(1));

			}
			System.out.println(timer.getStr());
		}

		else if ("5".equals(cmd)) {
			PerformanceTimer timer = new PerformanceTimer();

			for (int i = 0; i < 1000; i++) {
				logger.info("mongodb test");
			}

			System.out.println("=======================" + timer.getStr());
		}

		else if ("6".equals(cmd)) {
			System.out.println("当前内存状态:");
			int k = C2SSessionService.getInstance().getAllSessionCount();
			// int k2 =
			// SessionServiceImpl.getInstance().getOnlineSessionMap().size();
			int k2 = C2SSessionService.getInstance().getOnlineCount();
			int k3 = k2;

			System.out.println("session数量:" + k + " ,在线玩家:" + k2 + " ,在线玩家2:" + k3);

		}

		else if ("7".equals(cmd)) {

			Response.Builder build = MessageResponse.getMsgAllResponse("消息测试55555");
			RedisTemplate redisTemplate = SpringService.getBean("redisTemplate", RedisTemplate.class);
			redisTemplate.convertAndSend("java2", build.build().toByteArray());

		}

		else if ("8".equals(cmd)) {

			// JSONObject jsonObject =
			// PtAPIServiceImpl.getInstance().wxGetAccessTokenByCode("xxsdfsdfsdf",
			// 1);
			// System.out.println(jsonObject.get("errcode"));

		}

		else if ("9".equals(cmd)) {

			// RoleLogService roleLogService =
			// SpringService.getBean(RoleLogService.class);
			//
			// PerformanceTimer timer = new PerformanceTimer();
			// List<RoleLogBase> list = Lists.newArrayList();
			// for(int i=0;i<10000;i++){
			// RoleLogBase roleLogBase = new RoleLogBase();
			// roleLogBase.setRoleId(111L);
			// roleLogBase.setMsgCode(1);
			// //roleLogService.insert(roleLogBase);
			// list.add(roleLogBase);
			// }
			// roleLogService.insertAll(list);;
			// System.out.println(timer.getStr());
			// timer.reset();

			for (int i = 0; i < 1000000; i++) {
				MongoDBServiceImpl.getInstance().player_log(1, ELogType.login, "登录测试", null, 1L, null);
				MongoDBServiceImpl.getInstance().systemLog(ELogType.login, "测试2", 100L, 200L, ESysLogLevelType.NONE);

			}

		}

		else if ("11".equals(cmd)) {

			// 创建远程代理
			RmiProxyFactoryBean rpfb = new RmiProxyFactoryBean();
			rpfb.setRefreshStubOnConnectFailure(true);
			// 可以根据需要动态设定rmi的ip地址和端口
			rpfb.setServiceUrl("rmi://127.0.0.1:9008/centerRMIServer");
			// 设置访问接口
			rpfb.setServiceInterface(ICenterRMIServer.class);
			// 设置结束，让rmi开始链接远程的服务
			rpfb.afterPropertiesSet();
			// 获取链接后的返回结果
			Object o = rpfb.getObject();
			// 将结果转换成我们希望的类型
			ICenterRMIServer centerRMIServer = (ICenterRMIServer) o;

			PerformanceTimer timer = new PerformanceTimer();
			for (int i = 0; i < 1; i++) {
				// 调用结果执行远程调用
				Account account = centerRMIServer.getAccount(101428L);
				System.out.println(account);
				System.out.println(timer.getStr());
				timer.reset();
			}

		}

		else if ("12".equals(cmd)) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAndCreateAccount(EPtType.WX.getId(), "t2", "127.0.0.1", "0", "1.0.0", -1);
		}

		else if ("13".equals(cmd)) {
			// 当前防火墙状态
			StringBuilder buf = new StringBuilder();
			buf.append("当前处理ip数量:" + FirewallServiceImpl.getInstance().getIpFirewallModelMap().size());
			for (IpFirewallModel model : FirewallServiceImpl.getInstance().getIpFirewallModelMap().values()) {
				boolean pass = true;
				if (model.getBlackExpirationTime() > System.currentTimeMillis()) {
					pass = false;
				}
				buf.append("\n").append("ip:" + model.getIp() + "  \tlinkCount:" + model.getLinkCount()).append(" \tpass:" + pass);
			}
			System.out.println(buf.toString());

		}

		else if ("14".equals(cmd)) {
			List<BrandLogModel> list = MongoDBServiceImpl.getInstance().getParentBrandListByAccountId(101495L, 1, 0, 0);
			System.out.println(list.size());

		}

		else if ("15".equals(cmd)) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAccount(101670L);
			System.out.println(account);

		}

		else if ("16".equals(cmd)) {
			MyTestResponse.Builder myTestResponsebuilder = MyTestResponse.newBuilder();
			myTestResponsebuilder.setType(100);
			myTestResponsebuilder.setNum(10000);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MY_TEST);
			responseBuilder.setExtension(Protocol.myTestResponse, myTestResponsebuilder.build());

			for (C2SSession session : C2SSessionService.getInstance().getAllOnlieSession()) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
			}

		}

		else if ("17".equals(cmd)) {
			ThreadPoolTaskScheduler tt = new ThreadPoolTaskScheduler();

		} else if ("18".equals(cmd)) {

			for (int i = 0; i < 10; i++) {
				Global.getWxPayService().execute(new Runnable() {

					@Override
					public void run() {
						PerformanceTimer performanceTimer = new PerformanceTimer();
						FastMap<Integer, ShopModel> shopMap = ShopDict.getInstance().getShopMap();
						int shopID = 10;
						ShopModel shopModel = shopMap.get(shopID);
						ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
						String gameOrderID = centerRMIServer.getGameOrderID();

						PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
						String prepayID = ptAPIServiceImpl.getPrepayId(gameOrderID, shopModel.getName(), shopModel.getPrice(), "192.168.1.29", shopID,
								1, 0);
						System.out.println(prepayID);

						if (StringUtils.isEmpty(prepayID)) {
							logger.error("prepayId 获取失败!!!");
							return;
						}
						SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);// 微信登录相关,str1=appid,str2=应用密钥
						SysParamModel sysParamModel5001 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5001);// 微信支付str1=商户号str2=key

						ChannelModel channelModel = ChannelModelDict.getInstance().getChannelModel(0);
						String appid = channelModel.getChannelAppId(); // sysParamModel5000.getStr1()
						String mch_id = channelModel.getChannelPaySecret(); // sysParamModel5001.getStr1()
						String desc = channelModel.getChannelPayDesc(); // sysParamModel5002.getStr2()
						String out_trade_no = channelModel.getChannelAppCode(); // sysParamModel5002.getStr1()
						String notify_url = channelModel.getChannelPayCBUrl(); // sysParamModel5001.getStr2()
						System.out.println(channelModel);
						// 签名生成 临时订单
						Map<String, Object> signMap = new HashMap<String, Object>();
						signMap.put("appid", appid);
						signMap.put("partnerid", mch_id);
						signMap.put("prepayid", prepayID);
						signMap.put("package", "Sign=WXPay");
						signMap.put("noncestr", XMLParser.getRandomStringByLength(32));
						signMap.put("timestamp", XMLParser.getTimeStamp());
						signMap.put("sign", Signature.getSign(signMap, notify_url));
						logger.info("发送给微信的订单参数" + JSONObject.toJSONString(signMap));

					}
				});
			}

		} else if ("19".equals(cmd)) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			List<RankModel> models = centerRMIServer.queryRank(1);
			System.out.println(models);
		} else if ("20".equals(cmd)) {
		} else if ("21".equals(cmd)) {

		}

	}

}
