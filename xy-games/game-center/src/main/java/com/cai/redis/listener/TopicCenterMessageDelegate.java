package com.cai.redis.listener;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EWxHeadimgurlType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountSimple;
import com.cai.common.util.RedisToModelUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.WxUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicServiceImpl;

import protobuf.redis.ProtoRedis;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 中心主题监听
 * 
 * @author run
 *
 */
public class TopicCenterMessageDelegate implements MessageDelegate {

	private static Logger logger = LoggerFactory.getLogger(TopicCenterMessageDelegate.class);

	private AtomicLong mesCount = new AtomicLong();

	private final Converter<Object, byte[]> serializer;
	private final Converter<byte[], Object> deserializer;

	public TopicCenterMessageDelegate() {
		this.serializer = new SerializingConverter();
		this.deserializer = new DeserializingConverter();
	}

	@Override
	public void handleMessage(byte[] message) {

		mesCount.incrementAndGet();
		try {

			RedisResponse redisResponse = ProtoRedis.RedisResponse.parseFrom(message);
			int type = redisResponse.getRsResponseType().getNumber();
			switch (type) {

			// 更新账号信息
			case RsResponseType.ACCOUNT_UP_VALUE: {

				RsAccountResponse rsAccountResponse = redisResponse.getRsAccountResponse();
				this.handleRsAccountResponse(rsAccountResponse);
				break;
			}

			// TODO 废弃 logic直接删除房间 然后转发给代理
			// 房间
			// case RsResponseType.ROOM_VALUE: {
			// RsRoomResponse rsRoomResponse =
			// redisResponse.getRsRoomResponse();
			//
			// int type2 = rsRoomResponse.getType();
			// // 删除房间
			// if (type2 == 1) {
			// Integer room_id = rsRoomResponse.getRoomId();
			// // 找到所有玩家
			// RoomRedisModel roomRedisModel =
			// SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
			// room_id + "",
			// RoomRedisModel.class);
			// if (roomRedisModel == null)
			// break;
			//
			// // 删除缓存
			// SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM,
			// room_id.toString().getBytes());
			// if (SystemConfig.gameDebug == 1) {
			// logger.info("房间[{}]从redis中被删除,删除时房间内的玩家:!", room_id);
			// }
			// }

			// // 玩家退出房间
			// else if (type2 == 2) {
			// Long account_id = rsRoomResponse.getAccountId();
			// Account account =
			// PublicServiceImpl.getInstance().getAccount(account_id);
			// if (account == null) {
			// logger.error("玩家退出房间，玩家不存在account_id=" + account_id);
			// return;
			// }
			//
			// Integer room_id = rsRoomResponse.getRoomId();
			// // 找到玩家所在的房间
			// RoomRedisModel roomRedisModel =
			// SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
			// room_id.toString(),
			// RoomRedisModel.class);
			// if (roomRedisModel == null) {
			// logger.error("玩家退出房间，房间redis不存在" + room_id + "account_id=" +
			// account_id);
			// return;
			// }
			//
			// roomRedisModel.getPlayersIdSet().remove(account_id);
			// if (account.getAccountModel().getClient_ip() != null) {
			// roomRedisModel.getIpSet().remove(account.getAccountModel().getClient_ip());
			// }
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM,
			// room_id + "", roomRedisModel);
			//
			// // ========同步到中心========
			//
			// // ======================
			//
			// //
			// // 通知redis消息队列,通知开房间的代理
			// if (roomRedisModel.isProxy_room()) {
			// RedisResponse.Builder redisResponseBuilder =
			// RedisResponse.newBuilder();
			// redisResponseBuilder.setRsResponseType(RsResponseType.CMD);
			// RsCmdResponse.Builder rsCmdResponseBuilder =
			// RsCmdResponse.newBuilder();
			// rsCmdResponseBuilder.setType(3);
			// rsCmdResponseBuilder.setAccountId(roomRedisModel.getCreate_account_id());
			// redisResponseBuilder.setRsCmdResponse(rsCmdResponseBuilder);
			// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
			// ERedisTopicType.topicProxy);
			// }
			//
			// }
			//
			// break;
			// }

			default:
				logger.warn("多余的广播协议{}", type);
				break;
			}

		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	public AtomicLong getMesCount() {
		return mesCount;
	}

	private void handleRsAccountResponse(RsAccountResponse rsAccountResponse) {
		Account account = PublicServiceImpl.getInstance().getAccount(rsAccountResponse.getAccountId());
		if (account == null)
			return;

		// 属性copy加锁
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			// 属性copy
			RedisToModelUtil.rsAccountResponseToAccount(rsAccountResponse, account);
			// 刷新微信头像
			if (rsAccountResponse.hasRsAccountWeixinModelResponse()) {
				if (account.getAccountWeixinModel() != null) {
					//添加unionId对accountId缓存
					PublicServiceImpl.getInstance().putUnionIdAccountRelative(account.getAccountWeixinModel().getUnionid(),account.getAccountWeixinModel().getAccount_id());
					AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpleMap().get(account.getAccount_id());
					if (accountSimple != null) {
						String icon = WxUtil.changHeadimgurl(account.getAccountWeixinModel().getHeadimgurl(), EWxHeadimgurlType.S132);
						accountSimple.setIcon(icon);
						accountSimple.setNick_name(account.getAccountWeixinModel().getNickname());
					} else {
						accountSimple = new AccountSimple();// 对象为null，应该创建一个，否则还未入库，导致取不到微信对象
															// addby xie
						accountSimple.setAccount_id(account.getAccount_id());
						accountSimple.setIcon(WxUtil.changHeadimgurl(account.getAccountWeixinModel().getHeadimgurl(), EWxHeadimgurlType.S132));
						accountSimple.setNick_name(account.getAccountWeixinModel().getNickname());
						PublicServiceImpl.getInstance().getAccountSimpleMap().put(account.getAccount_id(), accountSimple);
					}
				}
			}
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}

//		// 是否刷新到redis
//		if (rsAccountResponse.hasFlushRedisCache() && rsAccountResponse.getFlushRedisCache()) {
//			SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT, account.getAccount_id() + "", account);
//		}

	}
}
