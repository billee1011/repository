package com.cai.redis.listener;

import java.util.concurrent.atomic.AtomicLong;

import com.cai.common.define.DictStringType;
import com.cai.dictionary.ClubWelfareRewardDict;
import com.cai.dictionary.ClubWelfareSwitchModelDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.ItemDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import protobuf.redis.ProtoRedis;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;

/**
 * 通用主题监听
 * 
 * @author run
 *
 */
public class TopicAllMessageDelegate implements MessageDelegate {

	private static Logger logger = LoggerFactory.getLogger(TopicAllMessageDelegate.class);

	private AtomicLong mesCount = new AtomicLong();

	@SuppressWarnings("unused")
	private final Converter<Object, byte[]> serializer;
	@SuppressWarnings("unused")
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
				case RsDictType.SYS_GAME_TYPE_VALUE: {
					logger.info("收到redis消息更新SysGameTypeDict字典");
					SysGameTypeDict.getInstance().load();// 逻辑服列表更新
					break;
				}
				case RsDictType.SERVER_LOGIC_VALUE: {
					logger.info("收到redis消息更新SERVER_LOGIC_VALUE字典");
					ServerDict.getInstance().load();
					break;
				}
				case RsDictType.PACKAGE_ITEM_VALUE: {
					logger.info("收到redis消息更新SERVER_LOGIC_VALUE字典");
					ItemDict.getInstance().load();
					break;
				}
				case RsDictType.SYS_PARAM_SERVER_VALUE: {
					logger.info("收到redis消息更新SYS_PARAM_SERVER_VALUE字典");
					SysParamServerDict.getInstance().load();
					break;
				}
				case RsDictType.GAME_GROUP_RULE_VALUE: {
					logger.info("收到redis消息更新GAME_GROUP_RULE_VALUE字典");
					GameGroupRuleDict.getInstance().load();
					break;
				}
				case RsDictType.SYS_PARAM_VALUE: {
					logger.info("收到redis消息更新SYS_PARAM_VALUE字典");
					SysParamDict.getInstance().load();
				}
				case RsDictType.COMMON_DICT_VALUE: {//通用的字典加载,根据字符串标记，决定是否加载某个字典;
					logger.info("收到redis消息更新COMMON_DICT_VALUE字典");
					String dictTypeStr = rsDictUpdateResponse.getDictTypeStr();
					switch(dictTypeStr) {
					case DictStringType.TESTDICT:
//						load();--执行具体的加载
						break;
					case DictStringType.LOAD_CLUB_WELFARE_SWITCH_MODEL:
						ClubWelfareSwitchModelDict.getInstance().load(true);
						break;
					case DictStringType.LOAD_CLUB_WELFARE_LOTTERY_REWARD_MODEL:
						ClubWelfareRewardDict.getInstance().load();
						break;
					}
					break;
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
