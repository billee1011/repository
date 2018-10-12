package com.cai.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.TurntableCmd;
import com.cai.common.domain.TurntableActiveModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;

import protobuf.clazz.Protocol.Response;
import protobuf.clazz.activity.ActivityTurntableProto.TurntableEmpty;
import protobuf.clazz.activity.ActivityTurntableProto.TurntableRsp;

/**
 * 转盘字典
 */
public class TurntableDict {

	private Logger logger = LoggerFactory.getLogger(TurntableDict.class);

	private Map<Integer, Response> rsps;

	private static TurntableDict instance = new TurntableDict();;

	public static TurntableDict getInstance() {
		return instance;
	}

	private TurntableDict() {
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		RedisService redisService = SpringService.getBean(RedisService.class);
		@SuppressWarnings("unchecked")
		List<TurntableActiveModel> activitys = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_TURUNTABLE_AWARD_POOL, ArrayList.class);
		if(activitys!=null) {
			rsps = activitys.stream().collect(Collectors.toMap(TurntableActiveModel::getId, TurntableActiveModel::encode));
			logger.info("加载缓存数据 turntableDict timer" + timer.getStr());
			
		    TurntableRsp.Builder b = TurntableRsp.newBuilder();
			b.setCmd(TurntableCmd.S2C_CONFIG_CHANGE);
			b.setProtos(TurntableEmpty.getDefaultInstance().toByteString());
			Response rsp =  PBUtil.toS2CCommonRsp(S2CCmd.TURNTABLE, b).build();
			C2SSessionService.getInstance().getAllOnlieSession().forEach((session) -> {
				session.send(rsp);
			});
		}
	
	}

	public Response get(int id) {
		return rsps.get(id);
	}
}
