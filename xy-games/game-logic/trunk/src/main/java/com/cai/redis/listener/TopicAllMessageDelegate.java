package com.cai.redis.listener;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.DictStringType;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.CardCategoryDict;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.CoinExciteDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysMatchBroadDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.TurntableDict;
import com.cai.game.AbstractRoom;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;
import com.google.common.base.Strings;

import protobuf.redis.ProtoRedis;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsCmdResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;
import protobuf.redis.ProtoRedis.RsRoomResponse;

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
		try {

			RedisResponse redisResponse = ProtoRedis.RedisResponse.parseFrom(message);
			int type = redisResponse.getRsResponseType().getNumber();
			switch (type) {

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
				case RsDictType.GOODS_VALUE: {
					logger.info("收到redis消息更新GoodsDict字典");
					GoodsDict.getInstance().load();// 道具
					break;
				}
				case RsDictType.SERVER_LOGIC_VALUE: {
					logger.info("收到redis消息更新SERVER_LOGIC_VALUE字典");
					ServerDict.getInstance().load();
					break;
				}
				case RsDictType.SYS_GAME_TYPE_VALUE: {
					logger.info("收到redis消息更新SysGameTypeDict字典");
					SysGameTypeDict.getInstance().load();
					break;
				}
				case RsDictType.RED_PACKAGE_RULE_VALUE: {
					logger.info("收到redis消息更新RedPackageRuleDict字典");
					RedPackageRuleDict.getInstance().load();// 逻辑服列表更新
					break;
				}
				case RsDictType.SYS_PARAM_SERVER_VALUE: {
					logger.info("收到redis消息更新SysParamServerDict字典");
					SysParamServerDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.MATCH_GROUND_VALUE: {
					logger.info("收到redis消息更新MatchDict字典");
					break;
				}
				case RsDictType.MATCH_BROAD_VALUE: {
					logger.info("收到redis消息更新SysMatchBroadDict字典");
					MatchDict.getInstance().load();// 服务端系统参数
					SysMatchBroadDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.GAME_GROUP_RULE_VALUE: {
					logger.info("收到redis消息更新GameGroupRuleDict字典");
					GameGroupRuleDict.getInstance().load();// 服务端系统参数
					break;
				}
				case RsDictType.TURNTABLE_VALUE: {
					logger.info("收到redis消息更新TURNTABLE_VALUE字典");
					TurntableDict.getInstance().load();
					break;
				}
				case RsDictType.COIN_CONFIG_VALUE: {
					logger.info("收到redis消息更新TURNTABLE_VALUE字典,更新金币场配置");
					CoinDict.getInstance().load();
					break;
				}
				case RsDictType.EXCITE_DICT_VALUE:{
					logger.info("收到redis消息更新EXCITE_DICT_VALUE字典,更新配置");
					CoinExciteDict.getInstance().load();
					break;
				}
				case RsDictType.CARD_CATEGORY_VALUE:{
					logger.info("收到redis消息更新CARD_CATEGORY_VALUE字典,更新配置");
					CardCategoryDict.getInstance().load();
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

			// 房间
			case RsResponseType.ROOM_VALUE: {
				RsRoomResponse rsRoomResponse = redisResponse.getRsRoomResponse();

				int type2 = rsRoomResponse.getType();
				// 删除房间
				if (type2 == 1) {
					Integer room_id = rsRoomResponse.getRoomId();
					// 找到所有玩家
					AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
					if (room != null) {
						try {
							room.getRoomLock().lock();
							if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room_id)) {
								if (room.matchId > 0) {
									// 比赛场不清
									return;
								}

								if (Strings.isNullOrEmpty(rsRoomResponse.getDesc())) {
									room.force_account();
								} else {
									room.force_account(rsRoomResponse.getDesc());
								}

							}
						} finally {
							room.getRoomLock().unlock();
						}

					} else {
						RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "",
								RoomRedisModel.class);

						if (roomRedisModel != null && roomRedisModel.getLogic_index() == SystemConfig.logic_index) {
							SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM, String.valueOf(room_id).getBytes());
						}
					}
				}

				break;
			}

			case RsResponseType.CMD_VALUE: {
				PerformanceTimer timer = new PerformanceTimer();
				RsCmdResponse rsCmdResponse = redisResponse.getRsCmdResponse();
				// 强制结算
				if (rsCmdResponse.getType() == 2) {
					for (Room room : PlayerServiceImpl.getInstance().getRoomMap().values()) {
						room.force_account("房间已被系统解散");
						logger.error("强制结算房间:room_id:" + room.getRoom_id());
					}

					logger.info("=========强制结算房间完成================" + timer.getStr());
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
