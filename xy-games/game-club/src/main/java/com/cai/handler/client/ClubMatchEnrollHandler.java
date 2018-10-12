/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchCode;
import com.cai.constant.ClubMatchOpenType;
import com.cai.constant.ClubMatchType;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchEnrollOrExitResultResponse;
import protobuf.clazz.ClubMsgProto.ClubMatchEnrollProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * @author wu_hc date: 2018年6月21日 下午8:54:34 <br/>
 */
@ICmd(code = C2SCmd.CLUB_MATCH_ENROLL_OR_EXIT, desc = "报名/取消报名,亲友圈赛事")
public final class ClubMatchEnrollHandler extends IClientExHandler<ClubMatchEnrollProto> {

	@Override
	protected void execute(ClubMatchEnrollProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMatchEnrollOrExitResultResponse.Builder resultBuilder = ClubMatchEnrollOrExitResultResponse.newBuilder();
			resultBuilder.setClubId(req.getClubId());
			resultBuilder.setCategory(req.getCategory());
			resultBuilder.setMatchId(req.getMatchId());
			ClubMatchWrap wrap = club.matchs.get(req.getMatchId());
			if (null == wrap) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("比赛不存在 ！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
				return;
			}
			if (wrap.getModel().getStatus() != ClubMatchWrap.ClubMatchStatus.PRE.status()) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("比赛已不在报名阶段，不能操作！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
				return;
			}
			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (member == null) {
				return;
			}
			if (req.getCategory() == 1 && club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_FREEZE)) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("亲友圈已被冻结，无法报名！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
				return;
			}
			if (req.getCategory() == 1 && club.getIdentify(topReq.getAccountId()) == EClubIdentity.DEFRIEND) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("您在该亲友圈暂停娱乐列表中，无法报名！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
				return;
			}

			if (req.getCategory() == 1 && wrap.isBanEnroll()) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("管理员已关闭报名入口！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
				return;
			}

			if (req.getCategory() == 1 && wrap.getBanPlayerIds().contains(topReq.getAccountId())) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("您已被管理员禁止参与该场比赛");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
				return;
			}

			if (req.getCategory() == 1 && wrap.getModel().getMatchType() == ClubMatchType.MANAGER_SET) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("请联系管理员设置参赛！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
				return;
			}
			if (req.getCategory() == 1 && wrap.getModel().getOpenType() == ClubMatchOpenType.TIME_MODE) {
				int limitTime = ClubCfg.get().getClubMatchEnrollTimeLimit();
				if (wrap.getModel().getStartDate().getTime() - System.currentTimeMillis() > limitTime * TimeUtil.MINUTE) {
					resultBuilder.setIsSuccess(false);
					resultBuilder.setMsg("比赛开始前" + limitTime + "分钟内开放报名！");
					session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
					return;
				}
			}
			if (req.getCategory() == 1 && wrap.getModel().getAttendCondition() == 1) {
				ClubMemberRecordModel memberRecordModel = club.getMemberRecordModelByDay(1, member);
				if (club.getMemberRealUseTire(memberRecordModel) < wrap.getModel().getConditionValue()) {
					resultBuilder.setIsSuccess(false);
					resultBuilder.setMsg("疲劳值不足，无法报名！");
					session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
					return;
				}
			}

			if (req.getCategory() == 1 ? wrap.enroll(topReq.getAccountId()) : wrap.exitMatch(topReq.getAccountId())) { // 成功
				resultBuilder.setIsSuccess(true);
				resultBuilder.setMsg(req.getCategory() == 1 ? "报名成功！" : "退赛成功！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));

				// send to other client
				Utils.notifyClubMatchEvent(topReq.getAccountId(), club, wrap.id(),
						req.getCategory() == 1 ? ClubMatchCode.ATTEND : ClubMatchCode.EXIT);
			} else { // 没有报名记录，退赛无效
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg(req.getCategory() == 1 ? "该比赛人数已满，请尝试报名别的比赛！" : "你没有报名该该赛，退赛操作无效！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_ENROLL_OR_EXIT_RESULT_RSP, resultBuilder));
			}
		});
	}
}
