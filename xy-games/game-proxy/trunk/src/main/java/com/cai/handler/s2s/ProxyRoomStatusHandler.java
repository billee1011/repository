/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.SysParamEnum;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.PrxoyPlayerRoomModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.cai.util.MessageResponse;
import com.cai.util.RoomUtil;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.ProxyRoomItemResponse;
import protobuf.clazz.Protocol.ProxyRoomViewResposne;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.s2s.S2SProto.ProxyRoomUpdateProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年11月8日 下午3:12:41 <br/>
 */
@IServerCmd(code = S2SCmd.PROXY_ROOM_STATUS, desc = "代理房间状态变化")
public class ProxyRoomStatusHandler extends IServerHandler<ProxyRoomUpdateProto> {

	@Override
	public void execute(ProxyRoomUpdateProto resp, S2SSession session) throws Exception {
		C2SSession clientS = C2SSessionService.getInstance().getSession(resp.getAccountId());
		if (null == clientS) {
			return;
		}

		Account account = clientS.getAccount();
		if (account == null)
			return;

		if (resp.hasChangeType() && resp.getChangeType() != GameConstants.PROXY_ROOM_RELEASE) {
			long now = System.currentTimeMillis();
			if (now - account.lastRefreshRoom < 5000) {// 控制五秒钟一次的刷新频率，客户端用的地方太多。。
				return;
			}
			account.lastRefreshRoom = now;
		}

		SysParamModel model2228 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2228);
		int value = 0;
		if (model2228 != null) {
			value = model2228.getVal5();
		}

		if (value != 1) {
			int source_room_id = RoomUtil.getRoomId(account.getAccount_id());
			if (source_room_id != 0) {
				return;
			}
		}

		AccountRedis accountRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "",
				AccountRedis.class);
		if (null == accountRedis) {
			return;
		}
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
				ProxyRoomItemResponse.Builder proxyRoomItemResponseBuilder = MessageResponse.getProxyRoomItemResponse(model, roomRedisModel);
				proxyRoomItemResponseList.add(proxyRoomItemResponseBuilder.build());
			}
		}

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
		ProxyRoomViewResposne.Builder proxyRoomViewResposneBuilder = ProxyRoomViewResposne.newBuilder();
		proxyRoomViewResposneBuilder.addAllProxyRoomItemResponseList(proxyRoomItemResponseList);
		proxyRoomViewResposneBuilder.setCanMaxRoom(count);// TODO 临时

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_MY_ROOMS);
		roomResponse.setProxyRoomViewResposne(proxyRoomViewResposneBuilder);

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		clientS.send(responseBuilder.build());
	}
}
