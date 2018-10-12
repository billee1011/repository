package com.cai.handler.client.clubwelfare;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.log.ClubMemberWelfareChangeLogModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import static protobuf.clazz.ClubMsgProto.ClubCommon;
import static protobuf.clazz.ClubMsgProto.ClubMemberWelfareChangeLogResponse;
import static protobuf.clazz.ClubMsgProto.WelfareChangeProto;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/11 15:02
 */
@ICmd(code = C2SCmd.CLUB_MEMBER_WELFARE_CHANGE_MSG_REQ, desc = "玩家亲友圈福卡变动记录请求")
public class ClubMemberWelfareChangeMsgReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			long operatorId = topReq.getAccountId();
			ClubMemberModel operator = club.members.get(operatorId);
			if (operator == null) {
				return;
			}
			ClubMemberWelfareChangeLogResponse.Builder b = ClubMemberWelfareChangeLogResponse.newBuilder();
			b.setClubId(club.getClubId());
			b.setAccountId(operatorId);
			List<ClubMemberWelfareChangeLogModel> list = MongoDBServiceImpl.getInstance().getClubMemberWelfareChangeLog(club.getClubId(), operatorId);
			if (list != null) {
				for (ClubMemberWelfareChangeLogModel model : list) {
					WelfareChangeProto.Builder changeBuilder = WelfareChangeProto.newBuilder();
					changeBuilder.setType(model.getType());
					changeBuilder.setCostNum(model.getCostNum());
					changeBuilder.setSubName(model.getSubName() == null ? "" : model.getSubName());
					changeBuilder.setOperatorId(model.getOperatorId());
					changeBuilder.setOperatorName(model.getOperatorName() == null ? "" : model.getOperatorName());
					changeBuilder.setOldValue(model.getOldValue());
					changeBuilder.setNewValue(model.getNewValue());
					changeBuilder.setRecordTime((int) (model.getCreate_time().getTime() / 1000));
					b.addLog(changeBuilder);
				}
			}
			session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_MEMBER_WELFARE_CHANGE_MSG_RSP, b));
		});
	}
}
