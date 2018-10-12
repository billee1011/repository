/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.module;

import java.util.List;

import com.cai.common.define.EPropertyType;
import com.cai.common.util.RuntimeOpt;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Common.CommonIL;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月18日 上午11:53:40 <br/>
 */
public final class ClubMsgSender {

	/**
	 * 俱乐部同步消息线程
	 */
	private static final DefaultWorkerLoopGroup clubWorker = DefaultWorkerLoopGroup.newGroup("club-work-thread", RuntimeOpt.availableProcessors());

	/**
	 * 同步俱乐部专属豆
	 * 
	 * @param session
	 * @param exclusiveGolds
	 */
	public static void notifyClubExclusiveGold(C2SSession session, final List<CommonIL> exclusiveGolds) {
		AccountPropertyListResponse.Builder builder = AccountPropertyListResponse.newBuilder();
		exclusiveGolds.forEach((exclusive) -> {
			// val1:gameId, vallong1:count
			AccountPropertyResponse.Builder propertyBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.CLUB_EXCLUSIVE_GOLD.getId(),
					exclusive.getK(), null, null, null, null, null, null, exclusive.getV());
			builder.addAccountProperty(propertyBuilder);
		});

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.PROPERTY);
		responseBuilder.setExtension(Protocol.accountPropertyListResponse, builder.build());
		session.send(responseBuilder.build());
	}

	/**
	 * 
	 * @param clubId
	 * @return
	 */
	public static final WorkerLoop worker(int clubId) {
		return clubWorker.next(clubId);
	}

	private ClubMsgSender() {
	}
}
