package com.cai.rmi.handler;

import java.util.Map;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.RedisServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;

/**
 * @author tang date: 2017年11月27日 下午3:41:24 <br/>
 */
@IRmi(cmd = RMICmd.NEW_DICT_RELOAD, desc = "通用通知加载字典")
public final class NewReloadDictRMIHandler extends IRMIHandler<Map<String, String>, Integer> {

	@Override
	public Integer execute(Map<String, String> map) {
		int result = 0;
		RsResponseType rt = RsResponseType.DICT_UPDATE;
		String types = map.get("type");
		if (types != null) {
			int type = Integer.parseInt(types);
			for (RsResponseType c : RsResponseType.values()) {
				if (c.getNumber() == type) {
					rt = c;
					break;
				}
			}
		}
		String dictTypeStr = map.get("dictTypeStr");// 字典对象
		String eMsgType = map.get("eMsgType");// 需要通知的服务器
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(rt);
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.COMMON_DICT);
		rsDictUpdateResponseBuilder.setDictTypeStr(dictTypeStr);
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.getEMsgType(eMsgType));
		return result;
	}

}
