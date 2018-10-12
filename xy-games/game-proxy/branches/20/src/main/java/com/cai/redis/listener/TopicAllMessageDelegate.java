package com.cai.redis.listener;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;

import com.cai.common.define.ELogType;
import com.cai.common.define.EPropertyType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.Account;
import com.cai.common.util.RedisToModelUtil;
import com.cai.core.SystemConfig;
import com.cai.dictionary.ActivityDict;
import com.cai.dictionary.ContinueLoginDict;
import com.cai.dictionary.GameDescDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.dictionary.MainUiNoticeDict;
import com.cai.dictionary.MoneyShopDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.SessionServiceImpl;
import com.cai.util.MessageResponse;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.GameNoticeAllResponse;
import protobuf.clazz.Protocol.MyTestResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.redis.ProtoRedis;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsCmdResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;
import protobuf.redis.ProtoRedis.RsGameNoticeModelResponse;
import protobuf.redis.ProtoRedis.RsMyTestResponse;

/**
 * 通用主题监听
 *
 * @author run
 *
 */
public class TopicAllMessageDelegate implements MessageDelegate {

	private static Logger logger = LoggerFactory.getLogger(TopicAllMessageDelegate.class);

	private AtomicLong mesCount = new AtomicLong();

	private final Converter<Object, byte[]> serializer;
	private final Converter<byte[], Object> deserializer;

	public TopicAllMessageDelegate() {
		this.serializer = new SerializingConverter();
		this.deserializer = new DeserializingConverter();
	}

	@Override
	public void handleMessage(byte[] message) {

		mesCount.incrementAndGet();
		//logger.info("接收到redis消息队列==>" + mesCount.get());
		// 临时关闭，本地测试
		if (false)
			return;
		try {

			RedisResponse redisResponse = ProtoRedis.RedisResponse.parseFrom(message);
			int type = redisResponse.getRsResponseType().getNumber();
			// System.out.println("redis主题 ====>" + redisResponse);
			switch (type) {

			// 更新账号信息
			case RsResponseType.ACCOUNT_UP_VALUE: {

				RsAccountResponse rsAccountResponse = redisResponse.getRsAccountResponse();

				// 是否在session中存在
				Session session = SessionServiceImpl.getInstance().getSessionByAccountId(rsAccountResponse.getAccountId());
				if (session == null)
					return;

				if (rsAccountResponse.hasLastProxyIndex()) {
					if (rsAccountResponse.getLastProxyIndex() != SystemConfig.proxy_index) {
						// 下线操作
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("你已在其它地方登录!").build());
						session.getChannel().close();
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
				//属性copy加锁
				ReentrantLock lock = account.getRedisLock();
				lock.lock();
				try {
					// 属性copy
					RedisToModelUtil.rsAccountResponseToAccount(rsAccountResponse, account);
				} catch (Exception e) {
					logger.error("error",e);
				}finally{
					lock.unlock();
				}



				//==========关键值监听===============
				// 新值
				long new_gold = account.getAccountModel().getGold();
				int new_agent = account.getAccountModel().getIs_agent();
				String new_password = account.getAccountModel().getPassword();
				long new_money = account.getAccountModel().getMoney();
				AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse.newBuilder();
				if (new_gold != old_gold) {
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.GOLD.getId(), null, null,null,null, null, null, null, new_gold);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				}
				if(old_agent != new_agent){
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.VIP.getId(), new_agent, null, null,null,null, null, null, null);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				}
				if(new_password!=null && !new_password.equals(old_password)){
					int is_null_agent_pw = 1;//1=是空密码  0=不是空密码
					if(!new_password.equals("")){
						is_null_agent_pw = 0;
					}
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.IS_NULL_AGENT_PW.getId(), is_null_agent_pw, null,null,null, null, null, null, null);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				}
				if(old_money != new_money){
					AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.MONEY.getId(), null, null,null,null, null, null, null, new_money);
					accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				}

				if (accountPropertyListResponseBuilder.getAccountPropertyBuilderList().size() > 0) {
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.PROPERTY);
					responseBuilder.setExtension(Protocol.accountPropertyListResponse, accountPropertyListResponseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}
				//=================================================
				

				break;
			}

			// 游戏公告
			case RsResponseType.GAME_NOTICE_VALUE: {
				RsGameNoticeModelResponse rsGameNoticeModelResponse = redisResponse.getRsGameNoticeModelResponse();

				//公告内容
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.GAME_NOTICE);
				GameNoticeAllResponse.Builder gameNoticeAllResponseBuilder = GameNoticeAllResponse.newBuilder();
				gameNoticeAllResponseBuilder.setType(rsGameNoticeModelResponse.getPayType());
				gameNoticeAllResponseBuilder.setMsg(rsGameNoticeModelResponse.getContent());
				responseBuilder.setExtension(Protocol.gameNoticeAllResponse, gameNoticeAllResponseBuilder.build());
				Response response = responseBuilder.build();
				//缓存到本地用于玩家第一次登录显示最后一条,玩家登录先找game_id相等的，如果没有找key=0的(全局)
				int gameType = rsGameNoticeModelResponse.getGameType();
				PublicServiceImpl.getInstance().getLastNoticeCache().put(gameType, response);
				if(gameType==0){
					//所有的
					for(Integer key : PublicServiceImpl.getInstance().getLastNoticeCache().keySet()){
						PublicServiceImpl.getInstance().getLastNoticeCache().put(key, response);
					}
				}


				// 在线所有玩家
				for (Session session : SessionServiceImpl.getInstance().getOnlineSessionMap().values()) {
					if (session.getChannel() == null)
						continue;

					Account account = session.getAccount();
					if (account == null)
						continue;

					if (rsGameNoticeModelResponse.getGameType() != 0) {
						if (rsGameNoticeModelResponse.getGameType() != account.getGame_id()) {
							continue;
						}
					}

					PlayerServiceImpl.getInstance().sendAccountMsg(session, response);
				}

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
					GameDescDict.getInstance().load();//游戏玩法说明
					break;
				}

				case RsDictType.SHOP_VALUE: {
					logger.info("收到redis消息更新ShopDict字典");
					ShopDict.getInstance().load();//商店
					break;
				}

				case RsDictType.MAIN_UI_NOTICE_VALUE: {
					logger.info("收到redis消息更新MainUiNoticeDict字典");
					MainUiNoticeDict.getInstance().load();//主界面公告
					break;
				}

				case RsDictType.LOGIN_NOTICE_VALUE: {
					logger.info("收到redis消息更新LoginNoticeDict字典");
					LoginNoticeDict.getInstance().load();//登录公告
					break;
				}

				case RsDictType.MONEY_SHOP_VALUE: {
                    logger.info("收到redis消息更新MoneyShopDict字典");
                    MoneyShopDict.getInstance().load();//金币商城
                    break;
				}
				case RsDictType.ACTIVITY_VALUE: {
                    logger.info("收到redis消息更新ActivityDict字典");
                    ActivityDict.getInstance().load();//活动
                    break;
				}
				case RsDictType.CONTINUE_LOGIN_VALUE: {
                    logger.info("收到redis消息更新ActivityDict字典");
                    ContinueLoginDict.getInstance().load();//活动
                    break;
				}

				}
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
					for (Session session : SessionServiceImpl.getInstance().getOnlineSessionMap().values()) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
					}
				}

				break;
			}

			case RsResponseType.CMD_VALUE: {
				RsCmdResponse rsCmdResponse = redisResponse.getRsCmdResponse();
				//踢下线
				if(rsCmdResponse.getType()==1){
					if(rsCmdResponse.hasAccountId()){
						long account_id = rsCmdResponse.getAccountId();
						Session session = SessionServiceImpl.getInstance().getSessionByAccountId(account_id);
						if(session!=null){
							session.getChannel().close();
							MongoDBServiceImpl.getInstance().systemLog(ELogType.kickOnlineAccount, "踢玩家下线:account_id="+account_id, account_id,null, ESysLogLevelType.NONE);
						}
					}
				}

				//强制结算
				else if(rsCmdResponse.getType()==2){
					Response.Builder responseBuilder = MessageResponse.getMsgAllResponse("服务器即将停机维护，牌局结算中...");
					for (Session session : SessionServiceImpl.getInstance().getOnlineSessionMap().values()) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
					}
				}

				break;
			}

			default:
				break;
			}

		} catch (Exception e) {
			logger.error("error", e);
		}
	}

}
