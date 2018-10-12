package com.cai.rmi.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.IReqRedisRoomType;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.RoomReqRMIVo;
import com.cai.common.util.SpringService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * 
 * P：： ROOM_ID取值复杂度O(1),其他方式取值复杂度均为O(n),还有序列化与传输成本，慎用！！！！！！！！！
 *
 * @author wu_hc date: 2018年1月11日 下午2:15:25 <br/>
 */
@IRmi(cmd = RMICmd.REDIS_ROOM_INFO, desc = "获取redis房间")
public final class RedisRoomInfoRMIHandler extends IRMIHandler<RoomReqRMIVo, List<RoomRedisModel>> {

	private final AtomicInteger counter = new AtomicInteger(0);

	static Map<Integer, Function<RoomReqRMIVo, List<RoomRedisModel>>> func = Maps.newHashMap();
	static {
		func.put(IReqRedisRoomType.ALL, 					RedisRoomInfoRMIHandler::byNone);
		func.put(IReqRedisRoomType.CLUB_ID, 				RedisRoomInfoRMIHandler::byClubId);
		func.put(IReqRedisRoomType.ROOM_ID, 				RedisRoomInfoRMIHandler::byRoomId);
		func.put(IReqRedisRoomType.LOGIC_SERVER_INDEX, 		RedisRoomInfoRMIHandler::byLogicServerIndex);
		func.put(IReqRedisRoomType.GAME_ID, 				RedisRoomInfoRMIHandler::byGameId);
		func.put(IReqRedisRoomType.GAME_TYPE_INDEX, 		RedisRoomInfoRMIHandler::byGameTypeIndex);
		func.put(IReqRedisRoomType.CREATE_ACCOUNT_ID, 		RedisRoomInfoRMIHandler::byCreateAccountId);
		func.put(IReqRedisRoomType.CLUB_RULE_ID, 			RedisRoomInfoRMIHandler::byClubRuleId);
		func.put(IReqRedisRoomType.MONEY_ROOM, 				RedisRoomInfoRMIHandler::byMoneyRoom);
		func.put(IReqRedisRoomType.PARTAKE_ACCOUNT_ID, 		RedisRoomInfoRMIHandler::byInAccountId);
	}

	@Override
	public List<RoomRedisModel> execute(RoomReqRMIVo vo) {

		logger.warn("RMICmd.REDIS_ROOM_INFO 参数:{} , 累计请求次数:{}!", vo, counter.incrementAndGet());
		Function<RoomReqRMIVo, List<RoomRedisModel>> f = func.get(vo.getType());
		if (null != f) {
			return f.apply(vo);
		} else {
			logger.error("不存在请求类型,msg:{}", vo);
		}
		return Collections.emptyList();
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byNone(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		return centerRMIServer.getAllRoomRedisModelList();
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byClubId(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Lists.newArrayList();
		}
		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.getClub_id() == vo.getValue()) {
				r.add(m);
			}
		});
		return r;
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byRoomId(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		RoomRedisModel model = centerRMIServer.getRoomById((int) vo.getValue());
		if (null != model) {
			return Arrays.asList(model);
		}
		return Collections.emptyList();
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byLogicServerIndex(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Collections.emptyList();
		}
		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.getLogic_index() == vo.getValue()) {
				r.add(m);
			}
		});
		return r;

	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byGameId(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Collections.emptyList();
		}
		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.getGame_id() == vo.getValue()) {
				r.add(m);
			}
		});
		return r;
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byGameTypeIndex(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Collections.emptyList();
		}

		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.getGame_type_index() == vo.getValue()) {
				r.add(m);
			}
		});
		return r;
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byCreateAccountId(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Collections.emptyList();
		}

		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.getCreate_account_id() == vo.getValue()) {
				r.add(m);
			}
		});
		return r;
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byClubRuleId(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Collections.emptyList();
		}

		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.getRule_id() == vo.getValue()) {
				r.add(m);
			}
		});
		return r;
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byMoneyRoom(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Collections.emptyList();
		}

		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.isMoneyRoom()) {
				r.add(m);
			}
		});
		return r;
	}

	/**
	 * 
	 * @param vo
	 * @return
	 */
	private static List<RoomRedisModel> byInAccountId(RoomReqRMIVo vo) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> redisModels = centerRMIServer.getAllRoomRedisModelList();
		if (null == redisModels || redisModels.isEmpty()) {
			return Collections.emptyList();
		}

		List<RoomRedisModel> r = Lists.newArrayList();
		redisModels.forEach((m) -> {
			if (null != m && m.getPlayersIdSet().contains(vo.getValue())) {
				r.add(m);
			}
		});
		return r;
	}
}
