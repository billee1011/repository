/**
 * 
 Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler;

import java.util.List;

import com.cai.common.define.ERankType;
import com.cai.common.domain.RankModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.net.core.ClientHandler;
import com.cai.service.RankService;
import com.google.common.collect.Lists;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.RankInfoProto;
import protobuf.clazz.Protocol.RankRequest;
import protobuf.clazz.Protocol.RankResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 处理客户端的排行榜数据请求
 *
 * @author wu_hc
 */
public class TestRankHandler extends ClientHandler<RankRequest> {

	@Override
	public void onRequest() throws Exception {

		ERankType rankType = ERankType.of(request.getType());
		if (ERankType.NONE == rankType) {
			logger.error("玩家:{},请求排行榜类型错误，type:{}", session.getAccount(), request.getType());
			return;
		}

		List<RankInfoProto> rankInfos = RankService.getInstance().getRankByType(rankType);
		if (rankInfos.isEmpty()) {
			logger.error("排行数据为空!!!");

			// GAME-TODO(待测试) 需要到center server取排行榜数据
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			List<RankModel> models = centerRMIServer.queryRank(request.getType());
			rankInfos = addRankToCache(rankType, models);
			if (rankInfos.isEmpty()) {
				return;
			}
		}

		RankResponse.Builder rankRspBuilder = RankResponse.newBuilder();
		rankRspBuilder.setType(request.getType());
		// rankInfos.forEach((info) -> {
		//
		// });
		for (final RankInfoProto info : rankInfos) {
			rankRspBuilder.addRanks(info);
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.RANK);
		responseBuilder.setExtension(Protocol.rankRsponse, rankRspBuilder.build());

		send(responseBuilder.build());
	}

	/**
	 * 缓存数据
	 * 
	 * @param rankType
	 * @param models
	 */
	private List<RankInfoProto> addRankToCache(ERankType rankType, List<RankModel> models) {
		List<RankInfoProto> rankInfos = Lists.newArrayList();
		// models.forEach((data) -> {
		//
		// });

		for (final RankModel data : models) {
			RankInfoProto.Builder builder = RankInfoProto.newBuilder();
			builder.setAccountId(data.getAccountId());
			builder.setValue(data.getValue());
			builder.setRank(data.getRank());
			builder.setHead(data.getHead());
			builder.setNickName(data.getNickName());
			builder.setSignature(data.getSignature());
			rankInfos.add(builder.build());
		}
		RankService.getInstance().addOrUpdate(rankType, rankInfos);
		return rankInfos;
	}
}
