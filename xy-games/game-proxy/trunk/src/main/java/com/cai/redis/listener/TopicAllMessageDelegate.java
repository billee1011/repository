package com.cai.redis.listener;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.SysParamEnum;
import com.cai.common.define.DictStringType;
import com.cai.common.define.EGameType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.EPropertyType;
import com.cai.common.define.ERankType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.PrxoyPlayerRoomModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RedisToModelUtil;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.ActivityDict;
import com.cai.dictionary.AppItemDict;
import com.cai.dictionary.CardCategoryDict;
import com.cai.dictionary.ChannelModelDict;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.CoinExciteDict;
import com.cai.dictionary.ContinueLoginDict;
import com.cai.dictionary.CustomerSerNoticeDict;
import com.cai.dictionary.GameDescDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.GameRecommendDict;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.HallGuideDict;
import com.cai.dictionary.IPGroupDict;
import com.cai.dictionary.InviteActiveDict;
import com.cai.dictionary.ItemDict;
import com.cai.dictionary.ItemExchangeDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.dictionary.MainUiNoticeDict;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.MoneyShopDict;
import com.cai.dictionary.SdkAppDict;
import com.cai.dictionary.SdkDiamondShopDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.TurntableDict;
import com.cai.dictionary.WelfareExchangeDict;
import com.cai.dictionary.WelfareGoodsTypeDict;
import com.cai.module.RoomModule;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.cai.service.ClientServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RankService;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.GameNoticeAllResponse;
import protobuf.clazz.Protocol.MyTestResponse;
import protobuf.clazz.Protocol.ProxyRoomItemResponse;
import protobuf.clazz.Protocol.ProxyRoomViewResposne;
import protobuf.clazz.Protocol.RankInfoProto;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.c2s.C2SProto.RechargeForCoinResp;
import protobuf.clazz.coin.CoinServerProtocol.S2SCoinStoreStat;
import protobuf.redis.ProtoRedis;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountGroup;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsCmdResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;
import protobuf.redis.ProtoRedis.RsGameNoticeModelResponse;
import protobuf.redis.ProtoRedis.RsMyTestResponse;
import protobuf.redis.ProtoRedis.RsRankProto;
import protobuf.redis.ProtoRedis.RsRankResponse;
import protobuf.redis.ProtoRedis.StoreNoticeResponse;

/**
 * 通用主题监听
 *
 * @author run
 */
public class TopicAllMessageDelegate implements MessageDelegate {

	private static Logger logger = LoggerFactory.getLogger(TopicAllMessageDelegate.class);

	private AtomicLong mesCount = new AtomicLong();

	@SuppressWarnings("unused") private final Converter<Object, byte[]> serializer;
	@SuppressWarnings("unused") private final Converter<byte[], Object> deserializer;

	public TopicAllMessageDelegate() {
		this.serializer = new SerializingConverter();
		this.deserializer = new DeserializingConverter();
	}

	@Override
	public void handleMessage(byte[] message) {

		mesCount.incrementAndGet();
		// logger.info("接收到redis消息队列==>" + mesCount.get());
		// 临时关闭，本地测试
		try {

			RedisResponse redisResponse = ProtoRedis.RedisResponse.parseFrom(message);
			int type = redisResponse.getRsResponseType().getNumber();
			// System.out.println("redis主题 ====>" + redisResponse);
			switch (type) {

			// 更新账号信息
			case RsResponseType.ACCOUNT_UP_VALUE: {

				RsAccountResponse rsAccountResponse = redisResponse.getRsAccountResponse();

				// 是否在session中存在
				// C2SSession session =
				// SessionServiceImpl.getInstance().getSessionByAccountId(rsAccountResponse.getAccountId());
				C2SSession session = C2SSessionService.getInstance().getSession(rsAccountResponse.getAccountId());
				if (session == null)
					return;

				if (rsAccountResponse.hasLastProxyIndex()) {
					if (rsAccountResponse.getLastProxyIndex() != SystemConfig.proxy_index) {
						// 下线操作
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("你已在其它地方登录!").build());
						session.shutdownGracefully();
						return;
					}
				}

				Account account = session.getAccount();
				if (account == null)
					return;

				long old_gold = account.getAccountModel().getGold();
				int old_agent = account.getAccountModel().getIs_agent();
				String old_password = account.getAccountModel().getPassword();
				long old_money = account.getAccountModel().getMoney();
				String old_sig = account.getAccountModel().getSignature();
				long old_welfare = account.getAccountModel().getWelfare();
				int old_diamond = account.getAccountModel().getDiamond();

				// 属性copy加锁
				ReentrantLock lock = account.getRedisLock();
				lock.lock();
				try {
					// 属性copy
					RedisToModelUtil.rsAccountResponseToAccount(rsAccountResponse, account);

					// 新值
					long new_gold = account.getAccountModel().getGold();// 锁内保证取到是当前最准确的

					if (new_gold != old_gold) {
						AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse.newBuilder();

						int changType = 0;
						if (rsAccountResponse.hasRsAccountModelResponse()) {
							changType = rsAccountResponse.getRsAccountModelResponse().getGoldChangeType();
						}
						AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
								.getAccountPropertyResponse(EPropertyType.GOLD.getId(), changType, null, null, null, null, null, null, new_gold);
						accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
						RoomModule.notifyWealthUpdate(account.getAccount_id());

						if (accountPropertyListResponseBuilder.getAccountPropertyBuilderList().size() > 0) {
							Response.Builder responseBuilder = Response.newBuilder();
							responseBuilder.setResponseType(ResponseType.PROPERTY);
							responseBuilder.setExtension(Protocol.accountPropertyListResponse, accountPropertyListResponseBuilder.build());
							PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
						}
					}
					
					int new_diamond = account.getAccountModel().getDiamond();
					if(old_diamond != new_diamond) {
						//通知钻石更新
						AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse.newBuilder();

						int changType = 0;
						if (rsAccountResponse.hasRsAccountModelResponse()) {
							changType = rsAccountResponse.getRsAccountModelResponse().getGoldChangeType();
						}
						AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
								.getAccountPropertyResponse(EPropertyType.DIAMOND.getId(), changType, null, null, null, null, null, null, new_diamond+0L);
						accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
//						RoomModule.notifyWealthUpdate(account.getAccount_id());

						if (accountPropertyListResponseBuilder.getAccountPropertyBuilderList().size() > 0) {
							Response.Builder responseBuilder = Response.newBuilder();
							responseBuilder.setResponseType(ResponseType.PROPERTY);
							responseBuilder.setExtension(Protocol.accountPropertyListResponse, accountPropertyListResponseBuilder.build());
							PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
						}
					}

				} catch (Exception e) {
					logger.error("error", e);
				} finally {
					lock.unlock();
				}

				// ==========关键值监听===============

				int new_agent = account.getAccountModel().getIs_agent();
				@SuppressWarnings("unused") int new_inner = account.getAccountModel().getIs_inner();
				String new_password = account.getAccountModel().getPassword();
				long new_money = account.getAccountModel().getMoney();
				final String new_sig = account.getAccountModel().getSignature();
				long new_welfare = account.getAccountModel().getWelfare();

				AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse.newBuilder();

				if (new_welfare != old_welfare) {
					int changType = 0;
					if (rsAccountResponse.hasRsAccountModelResponse()) {
						changType = rsAccountResponse.getRsAccountModelResponse().getWelfareChangeType();
					}
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
							.getAccountPropertyResponse(EPropertyType.WELFARE_CARD.getId(), changType, null, null, null, null, null, null,
									new_welfare);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
					RoomModule.notifyWealthUpdate(account.getAccount_id());
				}

				if (old_agent != new_agent) {
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
							.getAccountPropertyResponse(EPropertyType.VIP.getId(), new_agent, null, null, null, null, null, null, null);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				}
				if (new_password != null && !new_password.equals(old_password)) {
					int is_null_agent_pw = 1;// 1=是空密码 0=不是空密码
					if (!new_password.equals("")) {
						is_null_agent_pw = 0;
					}
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
							.getAccountPropertyResponse(EPropertyType.IS_NULL_AGENT_PW.getId(), is_null_agent_pw, null, null, null, null, null, null,
									null);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				}
				if (old_money != new_money) {

					int changType = 0;
					String strTip = null;
					if (rsAccountResponse.hasRsAccountModelResponse()) {
						changType = rsAccountResponse.getRsAccountModelResponse().getMoneyChangeType();
						strTip = rsAccountResponse.getRsAccountModelResponse().getStrTip();
					}
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
							.getAccountPropertyResponse(EPropertyType.MONEY.getId(), changType, null, null, null, null, strTip, null, new_money);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
					RoomModule.notifyWealthUpdate(account.getAccount_id());
					if (changType == EMoneyOperateType.RECHARGE_EXCHANGE_COIN.getId()) {
						RechargeForCoinResp.Builder builder = RechargeForCoinResp.newBuilder();
						builder.setIsSuccess(true);
						session.send(PBUtil.toS2CCommonRsp(S2CCmd.RECHARGE_FOR_COIN, builder));
					}
				}
				
				// 签名
				if (null != new_sig && !new_sig.equals(old_sig)) {
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
							.getAccountPropertyResponse(EPropertyType.SIGNTURE.getId(), null, null, null, null, null, new_sig, null, null);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				}
				if (accountPropertyListResponseBuilder.getAccountPropertyBuilderList().size() > 0) {
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.PROPERTY);
					responseBuilder.setExtension(Protocol.accountPropertyListResponse, accountPropertyListResponseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}
				// =================================================

				break;
			}

			// 游戏公告
			case RsResponseType.GAME_NOTICE_VALUE: {
				RsGameNoticeModelResponse rsGameNoticeModelResponse = redisResponse.getRsGameNoticeModelResponse();

				// 公告内容
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.GAME_NOTICE);
				GameNoticeAllResponse.Builder gameNoticeAllResponseBuilder = GameNoticeAllResponse.newBuilder();
				gameNoticeAllResponseBuilder.setType(rsGameNoticeModelResponse.getPayType());
				gameNoticeAllResponseBuilder.setMsg(rsGameNoticeModelResponse.getContent());
				gameNoticeAllResponseBuilder.setGameId(rsGameNoticeModelResponse.getGameId());
				gameNoticeAllResponseBuilder.setAppId(rsGameNoticeModelResponse.getGameType());
				responseBuilder.setExtension(Protocol.gameNoticeAllResponse, gameNoticeAllResponseBuilder.build());
				Response response = responseBuilder.build();
				// 缓存到本地用于玩家第一次登录显示最后一条,玩家登录先找game_id相等的，如果没有找key=0的(全局)
				int gameType = rsGameNoticeModelResponse.getGameType();
				PublicServiceImpl.getInstance().getLastNoticeCache().put(gameType, response);
				if (gameType == 0) {
					// 所有的
					for (Integer key : PublicServiceImpl.getInstance().getLastNoticeCache().keySet()) {
						PublicServiceImpl.getInstance().getLastNoticeCache().put(key, response);
					}
				}

				// 在线所有玩家
				// for (C2SSession session :
				// SessionServiceImpl.getInstance().getOnlineSessionMap().values())
				// {

				Global.getLogicService().execute(new Runnable() {

					@Override
					public void run() {
						for (C2SSession session : C2SSessionService.getInstance().getAllOnlieSession()) {
							if (session == null)
								continue;

							Account account = session.getAccount();
							if (account == null)
								continue;

							if (rsGameNoticeModelResponse.getGameType() != 0) {
								if (account.getGame_id() != EGameType.JS.getId() && account.getGame_id() != EGameType.DT.getId()
										&& rsGameNoticeModelResponse.getGameType() != account.getGame_id()) {
									continue;
								}
							}

							PlayerServiceImpl.getInstance().sendAccountMsg(session, response);
						}
					}

				});

				break;
			}

			// 字典更新
			case RsResponseType.DICT_UPDATE_VALUE: {
				RsDictUpdateResponse rsDictUpdateResponse = redisResponse.getRsDictUpdateResponse();
				RsDictType rsDictType = rsDictUpdateResponse.getRsDictType();
				
				switch (rsDictType.getNumber()) {
				// 系统参数
				case RsDictType.SYS_PARAM_VALUE: {
					logger.info("收到redis消息更新SysParamDict字典");
					SysParamDict.getInstance().load();// 系统参数
					break;
				}

				case RsDictType.SYS_NOTICE_VALUE: {
					logger.info("收到redis消息更新SysNoticeDict字典");
					SysNoticeDict.getInstance().load();// 系统公告
					break;
				}

				case RsDictType.GAME_DESC_VALUE: {
					logger.info("收到redis消息更新GameDescDict字典");
					GameDescDict.getInstance().load();// 游戏玩法说明
					break;
				}

				case RsDictType.SHOP_VALUE: {
					logger.info("收到redis消息更新ShopDict字典");
					ShopDict.getInstance().load();// 商店
					break;
				}

				case RsDictType.MAIN_UI_NOTICE_VALUE: {
					logger.info("收到redis消息更新MainUiNoticeDict字典");
					MainUiNoticeDict.getInstance().load();// 主界面公告
					break;
				}

				case RsDictType.LOGIN_NOTICE_VALUE: {
					logger.info("收到redis消息更新LoginNoticeDict字典");
					LoginNoticeDict.getInstance().load();// 登录公告
					break;
				}

				case RsDictType.MONEY_SHOP_VALUE: {
					logger.info("收到redis消息更新MoneyShopDict字典");
					MoneyShopDict.getInstance().load();// 金币商城
					break;
				}
				case RsDictType.ACTIVITY_VALUE: {
					logger.info("收到redis消息更新ActivityDict字典");
					ActivityDict.getInstance().load();// 活动
					break;
				}
				case RsDictType.CONTINUE_LOGIN_VALUE: {
					logger.info("收到redis消息更新ActivityDict字典");
					ContinueLoginDict.getInstance().load();// 活动
					break;
				}
				case RsDictType.GOODS_VALUE: {
					logger.info("收到redis消息更新GoodsDict字典");
					GoodsDict.getInstance().load();// 道具
					break;
				}
				case RsDictType.IP_LIST_VALUE: {
					logger.info("收到redis消息更新IPGroupDict字典");
					IPGroupDict.getInstance().load();// ip
					break;
				}
				case RsDictType.APPITEM_VALUE: {
					logger.info("收到redis消息更新AppItemDict字典");
					AppItemDict.getInstance().load();// ip
					break;
				}
				case RsDictType.SERVER_LOGIC_VALUE: {
					logger.info("收到redis消息更新ServerDict字典");
					ServerDict.getInstance().load();// 逻辑服列表更新
					ClientServiceImpl.getInstance().reloadConnector(); // 重载和逻辑服的连接
					break;
				}
				case RsDictType.GAME_RECOMMEND_VALUE: {
					logger.info("收到redis消息更新GameRecommendDict字典");
					GameRecommendDict.getInstance().load();// 逻辑服列表更新
					break;
				}
				case RsDictType.SYS_GAME_TYPE_VALUE: {
					logger.info("收到redis消息更新SysGameTypeDict字典");
					SysGameTypeDict.getInstance().load();// 逻辑服列表更新
					break;
				}
				case RsDictType.GAME_GROUP_RULE_VALUE: {
					logger.info("收到redis消息更新GameGroupRuleDict字典");
					GameGroupRuleDict.getInstance().load();// 逻辑服列表更新
					break;
				}
				case RsDictType.TURNTABLE_VALUE: {
					TurntableDict.getInstance().load();
					logger.info("收到redis消息更新turntableDict字典");
					break;
				}
				case RsDictType.SYS_PARAM_SERVER_VALUE: {
					logger.info("收到redis消息更新SysParamServerDict字典");
					SysParamServerDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.CUSTOMER_SER_NOTICE_VALUE: {
					logger.info("收到redis消息更新CustomerSerNoticeDict字典");
					CustomerSerNoticeDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.MATCH_GROUND_VALUE: {
					logger.info("收到redis消息更新MatchDict字典");
					MatchDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.MATCH_BROAD_VALUE: {
					logger.info("收到redis消息更新MatchDict字典");
					MatchDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.INVITE_REDPACKEY_VALUE: {
					logger.info("收到redis消息更新InviteActiveDict字典");
					InviteActiveDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.COIN_CONFIG_VALUE: {
					logger.info("收到redis消息更新InviteActiveDict字典,更新金币场配置");
					CoinDict.getInstance().load();
					break;
				}
				case RsDictType.ITEM_EXCHANGE_VALUE: {
					logger.info("收到redis消息更新ItemExchangeDict字典");
					ItemExchangeDict.getInstance().load();
					break;
				}
				case RsDictType.PACKAGE_ITEM_VALUE: {
					logger.info("收到redis消息更新ItemDict字典");
					ItemDict.getInstance().load();
					break;
				}
				case RsDictType.WELFARE_EXCHANGE_VALUE: {
					logger.info("收到redis消息更新WelfareExchangeDict字典");
					WelfareExchangeDict.getInstance().load();
					break;
				}
				case RsDictType.WELFARE_GOODS_TYPE_VALUE: {
					logger.info("收到redis消息更新WelfareGoodsTypeDict字典");
					WelfareGoodsTypeDict.getInstance().load();
					break;
				}
				case RsDictType.CHANNEL_DICT_VALUE: {
					logger.info("收到redis消息更新CHANNEL_DICT_VALUE字典");
					ChannelModelDict.getInstance().load();
				}
				case RsDictType.HALL_GUIDE_DICT_VALUE: {
					logger.info("收到redis消息更新HALL_GUIDE_DICT_VALUE字典");
					HallGuideDict.getInstance().load();
				}
				case RsDictType.GAME_RESOURCE_DICT_VALUE: {
					logger.info("收到redis消息更新GAME_RESOURCE_DICT_VALUE字典");
					HallGuideDict.getInstance().loadResource();
					break;
				}
				case RsDictType.EXCITE_DICT_VALUE: {
					logger.info("收到redis消息更新EXCITE_DICT_VALUE字典,更新金币场");
					CoinExciteDict.getInstance().load();
					break;
				}
				case RsDictType.CARD_CATEGORY_VALUE: {
					logger.info("收到redis消息更新CARD_CATEGORY_VALUE字典,更新金币场");
					CardCategoryDict.getInstance().load();
					break;
				}
				case RsDictType.HALL_MAIN_VIEW_BACK_VALUE: {
					logger.info("收到redis消息更新HALL_MAIN_VIEW_BACK_VALUE字典");
					HallGuideDict.getInstance().loadMainViewBack();
					break;
				}
				case RsDictType.SDK_APP_VALUE: {
					logger.info("收到redis消息更新SDK_APP_VALUE字典");
					SdkAppDict.getInstance().load();
					break;
				}
				case RsDictType.SDK_DIAMOND_SHOP_VALUE: {
					logger.info("收到redis消息更新SDK_DIAMOND_SHOP_VALUE字典");
					SdkDiamondShopDict.getInstance().load();
					break;
				}
				case RsDictType.COMMON_DICT_VALUE: {//通用的字典加载,根据字符串标记，决定是否加载某个字典;
					logger.info("收到redis消息更新COMMON_DICT_VALUE字典");
					String dictTypeStr = rsDictUpdateResponse.getDictTypeStr();
					switch(dictTypeStr) {
					case DictStringType.TESTDICT:
//						load();--执行具体的加载
						break;
					}
					break;
				}
				}
				break;
			}

			// 压力测试
			case RsResponseType.MY_TEST_VALUE: {
				RsMyTestResponse rsMyTestResponse = redisResponse.getRsMyTestResponse();
				int rtype = rsMyTestResponse.getType();
				if (rtype == 1) {
					int num = rsMyTestResponse.getNum();
					if (num > 2000)
						num = 2000;

					MyTestResponse.Builder myTestResponsebuilder = MyTestResponse.newBuilder();
					myTestResponsebuilder.setType(100);
					myTestResponsebuilder.setNum(num);
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.MY_TEST);
					responseBuilder.setExtension(Protocol.myTestResponse, myTestResponsebuilder.build());
					// for (C2SSession session :
					// SessionServiceImpl.getInstance().getOnlineSessionMap().values())
					// {
					for (C2SSession session : C2SSessionService.getInstance().getAllOnlieSession()) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
					}
				}

				break;
			}

			case RsResponseType.CMD_VALUE: {
				RsCmdResponse rsCmdResponse = redisResponse.getRsCmdResponse();
				// 踢下线
				if (rsCmdResponse.getType() == 1) {
					if (rsCmdResponse.hasAccountId()) {
						long account_id = rsCmdResponse.getAccountId();
						// C2SSession session =
						// SessionServiceImpl.getInstance().getSessionByAccountId(account_id);
						C2SSession session = C2SSessionService.getInstance().getSession(account_id);
						if (session != null) {
							session.shutdownGracefully();
							MongoDBServiceImpl.getInstance()
									.systemLog(ELogType.kickOnlineAccount, "踢玩家下线:account_id=" + account_id, account_id, null, ESysLogLevelType.NONE);
						}
					}
				}

				// 强制结算
				else if (rsCmdResponse.getType() == 2) {
					Response.Builder responseBuilder = MessageResponse.getMsgAllResponse("服务器即将停机维护，牌局结算中...");
					for (C2SSession session : C2SSessionService.getInstance().getAllOnlieSession()) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
					}
				}
				// 代理开房变化了
				else if (rsCmdResponse.getType() == 3) {
					long account_id = rsCmdResponse.getAccountId();
					// 是否在session中存在
					C2SSession session = C2SSessionService.getInstance().getSession(account_id);
					if (session == null)
						return;
					Account account = session.getAccount();
					if (account == null)
						return;

					AccountRedis accountRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "", AccountRedis.class);
					if(accountRedis == null) return;
					Map<Integer, PrxoyPlayerRoomModel> proxyRoomMap = accountRedis.getProxRoomMap();
					List<PrxoyPlayerRoomModel> list = Lists.newArrayList(proxyRoomMap.values());
					// 排序一下
					Collections.sort(list, new Comparator<PrxoyPlayerRoomModel>() {
						public int compare(PrxoyPlayerRoomModel p1, PrxoyPlayerRoomModel p2) {
							return ((Long) p2.getCreate_time()).compareTo((Long) p1.getCreate_time());// id
							// 从大到小
						}
					});

					RedisService redisService = SpringService.getBean(RedisService.class);
					List<ProxyRoomItemResponse> proxyRoomItemResponseList = Lists.newArrayList();
					for (PrxoyPlayerRoomModel model : list) {
						RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, model.getRoom_id() + "", RoomRedisModel.class);
						if (roomRedisModel != null) {
							ProxyRoomItemResponse.Builder proxyRoomItemResponseBuilder = MessageResponse
									.getProxyRoomItemResponse(model, roomRedisModel);
							proxyRoomItemResponseList.add(proxyRoomItemResponseBuilder.build());
						}
					}

					ProxyRoomViewResposne.Builder proxyRoomViewResposneBuilder = ProxyRoomViewResposne.newBuilder();
					proxyRoomViewResposneBuilder.addAllProxyRoomItemResponseList(proxyRoomItemResponseList);

					SysParamModel sysParamModel = null;

					FastMap<Integer, SysParamModel> paramMap = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6);
					if (paramMap != null) {
						sysParamModel = paramMap.get(SysParamEnum.ID_1107.getId());
					}
					int count = 50;
					if (sysParamModel != null) {
						if (account.getAccountModel().getIs_agent() < 1) {
							count = sysParamModel.getVal2();
						} else {
							count = sysParamModel.getVal1();
						}
					}
					proxyRoomViewResposneBuilder.setCanMaxRoom(count);

					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					roomResponse.setType(MsgConstants.RESPONSE_MY_ROOMS);
					roomResponse.setProxyRoomViewResposne(proxyRoomViewResposneBuilder);

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

				break;
			}
			// 排行榜
			case RsResponseType.RANK_VALUE: {
				RsRankResponse rankRsp = redisResponse.getRsRankRsp();

				int rankType = rankRsp.getType();
				List<RsRankProto> rankProtos = rankRsp.getRanksList();

				// 组装成RankInfoProto
				if (null != ERankType.of(rankType) && null != rankProtos) {

					List<RankInfoProto> rankInfos = Lists.newArrayList();
					// rankProtos.forEach((data) -> {
					// ;
					// });

					for (final RsRankProto data : rankProtos) {
						RankInfoProto.Builder b = RankInfoProto.newBuilder();
						b.setAccountId(data.getAccountId());
						b.setRank(data.getRank());
						b.setHead(data.getHead());
						b.setNickName(data.getNickName());
						b.setSignature(data.getSignature());
						b.setValue(data.getValue());
						rankInfos.add(b.build());
					}
					RankService.getInstance().addOrUpdate(ERankType.of(rankType), rankInfos);
				} else {
					logger.error("Center ---> Proxy 同步排行榜数据出错！！！");
				}
				break;
			}
			case RsResponseType.ACCOUNT_GROUP_VALUE: {
				List<RsAccountGroup> list = redisResponse.getRsAccountGroupListList();
				if (list != null && list.size() > 0) {
					return;
				}
				Date date = new Date();
				list.forEach((accountGroup) -> {
					C2SSession session = C2SSessionService.getInstance().getSession(accountGroup.getAccountId());
					if (session == null)
						return;
					Account account = session.getAccount();
					if (account == null)
						return;
					AccountGroupModel model = new AccountGroupModel();
					model.setAccount_id(accountGroup.getAccountId());
					model.setDate(date);
					model.setGroupId(accountGroup.getGroupId());
					account.getAccountGroupModelMap().put(accountGroup.getGroupId(), model);
				});
			}
			case RsResponseType.STORE_NOTICE_VALUE: {
				StoreNoticeResponse storeResponse = redisResponse.getStoreResponse();
				S2SCoinStoreStat.Builder request = S2SCoinStoreStat.newBuilder();

				request.setAccountId(storeResponse.getAccountId());
				request.setOpType(storeResponse.getOpType());
				request.setGold(storeResponse.getGold());
				request.setCoin(storeResponse.getCoin());
				request.setUiType(storeResponse.getUiType());
				request.setSubUiType(storeResponse.getUiSubType());
				request.setOpId(storeResponse.getOpId());

				ClientServiceImpl.getInstance().sendToCoin(SystemConfig.connectCoin, PBUtil.toS2SRequet(C2SCmd.COIN_PAY_MSG_STAT, request).build());
			}
			default:
				break;
			}

		} catch (Exception e) {
			logger.error("error", e);
		}
	}

}
