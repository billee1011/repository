package com.cai.handler.client;

import com.cai.common.ClubTireLogType;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.log.ClubScoreMsgLogModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubModifyTireMsgProto;
import protobuf.clazz.ClubMsgProto.ClubModifyTireMsgRequestProto;
import protobuf.clazz.ClubMsgProto.ClubModifyTireMsgResponse;
import protobuf.clazz.ClubMsgProto.ClubModifyTireMsgResponse.Builder;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年4月26日 下午3:55:22
 */
@ICmd(code = C2SCmd.CLUB_MODIFY_TIRE_MSG_REQUEST, desc = "俱乐部修改疲劳值消息请求")
public class ClubRequstModifyTireMsgHandler extends IClientExHandler<ClubModifyTireMsgRequestProto> {

	@Override
	protected void execute(ClubModifyTireMsgRequestProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator) {
				return;
			}
			ClubModifyTireMsgResponse.Builder builder = ClubModifyTireMsgResponse.newBuilder();
			builder.setClubId(club.getClubId());
			builder.setType(1);
			if (EClubIdentity.isManager(operator.getIdentity())) {
				if (req.getAccountId() == 0) {
					club.getClubScoreMsgWrap().getMsgList().forEach((model) -> {
						if (model.getMsgType() != ClubTireLogType.TIRE_ACCU) {
							encodeScoreMsg(builder, model);
						}
					});
				} else {
					ClubMemberModel member = club.members.get(req.getAccountId());
					if (member != null) {
						club.getClubScoreMsgWrap().getMsgList().forEach((model) -> {
							if (model.getTargetAccountId() == req.getAccountId()) {
								encodeScoreMsg(builder, model);
							}
						});
					}
				}
			} else if (topReq.getAccountId() == req.getAccountId()) {
				ClubMemberModel member = club.members.get(req.getAccountId());
				if (member != null) {
					club.getClubScoreMsgWrap().getMsgList().forEach((model) -> {
						if (model.getTargetAccountId() == req.getAccountId()) {
							encodeScoreMsg(builder, model);
						}
					});
				}
			}
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MODIFY_TIRE_MSG, builder));
		});
	}

	private void encodeScoreMsg(Builder builder, ClubScoreMsgLogModel model) {
		ClubModifyTireMsgProto.Builder msgBuilder = ClubModifyTireMsgProto.newBuilder();
		msgBuilder.setOpeAccountId(model.getAccountId());
		msgBuilder.setOpeNickname(model.getAccountName());
		msgBuilder.setTargetAccountId(model.getTargetAccountId());
		msgBuilder.setTargetNickname(model.getTargetAccountName() == null ? "" : model.getTargetAccountName());
		msgBuilder.setOldValue(model.getOldValue());
		msgBuilder.setNewValue(model.getNewValue());
		msgBuilder.setTime(model.getCreate_time().getTime());
		msgBuilder.setRecordTime(model.getRecordTime());
		msgBuilder.setMsgType(model.getMsgType());
		msgBuilder.setSwitchStatus(model.getSwitchStatus());
		builder.addMsgs(msgBuilder);
	}
}
