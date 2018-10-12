/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.PBUtil;
import com.cai.core.Global;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.c2s.C2SProto.ClubUpvoteProto;

/**
 * 由点赞
 *
 * @author wu_hc date: 2017年8月29日 下午5:21:00 <br/>
 */
@IServerCmd(code = S2SCmd.CLUB_RECORD_UPVOTE, desc = "点赞")
public class ClubUpvoteRspHandler extends IServerHandler<ClubUpvoteProto> {

	@Override
	public void execute(ClubUpvoteProto resp, S2SSession session) throws Exception {

		Global.getService(Global.SERVER_LOGIC).execute(() -> {
			C2SSession client = C2SSessionService.getInstance().getSession(resp.getAccountId());
			if (null == client) {
				return;
			}
			protobuf.clazz.Common.CommonSII vote = resp.getVote();
			BrandLogModel model = MongoDBServiceImpl.getInstance().searchBrandLogModel(resp.getClubId(), Long.parseLong(vote.getK()));
			if (null == model) {

				client.send(MessageResponse.getMsgAllResponse("战绩不存在!").build());
				return;
			}

			if (model.getUpvote() == vote.getV1()) {
				client.send(MessageResponse.getMsgAllResponse(String.format("已经是%s点赞状态", model.getUpvote() == 0 ? "未" : "")).build());
				return;
			}
			model.setUpvote(vote.getV1());
			MongoDBServiceImpl.getInstance().updateBrandLogModel(model);

			client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_RECORD_UPVOTE, resp.toBuilder()));
		});
	}
}
