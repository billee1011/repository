/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EPhoneIdentifyCodeType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.define.ICommonCode;
import com.cai.common.define.IPhoneOperateType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.UserPhoneRMIVo;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.google.common.primitives.Longs;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubAssignmentProto;
import protobuf.clazz.c2s.C2SProto.CommonCodeRsp;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年3月2日 上午11:15:07 <br/>
 */
@ICmd(code = C2SCmd.CLUB_ASSIGNMENT, desc = "俱乐部转让,要求玩家必须邦定手机")
public class ClubAssignmentHandler extends IClientExHandler<ClubAssignmentProto> {

	@Override
	protected void execute(ClubAssignmentProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club || !req.hasTargetAccountId()) {
			return;
		}

		club.runInReqLoop(() -> {

			// 1权限验证
			ClubMemberModel self = club.members.get(topReq.getAccountId());

			if (null == self || self.getAccount_id() != club.getOwnerId()) {
				Utils.sendTip(topReq.getAccountId(), "不是亲友圈成员或者权限不足!", ESysMsgType.NONE, session);
				return;
			}

			ClubMemberModel target = club.members.get(req.getTargetAccountId());
			if (null == target) {
				Utils.sendTip(topReq.getAccountId(), "不可以转让亲友圈给非群成员!", ESysMsgType.NONE, session);
				return;
			}

			// 2机验证码 GAME-TODO
			if (club.getMemberCount() >= 10) {
				UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND_INFO, 0L, req.getPhoneInfo().getMobile());
				Pair<Integer, String> r = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);

				long accountId = Longs.tryParse(r.getSecond());
				if (r.getFirst().intValue() == 0 || accountId != topReq.getAccountId()) {
					Utils.sendTip(topReq.getAccountId(), "你的帐号没有邦定手机，不能转让!", ESysMsgType.NONE, session);
					return;
				}

				if (!Utils.identifyCodeVaild(req.getPhoneInfo().getMobile(), req.getPhoneInfo().getIdentifyCode(),
						EPhoneIdentifyCodeType.PHONE_CLUB_ASSIGNMENT)) {
					Utils.sendTip(topReq.getAccountId(), "验证码不正确，请重新输入!", ESysMsgType.NONE, session);
					return;
				}
			}

			// 3设置，落地
			club.transferOwner(req.getTargetAccountId());

			// 4推送成功信息
			List<Long> notifyIds = club.getManagerIds();
			notifyIds.add(self.getAccount_id());

			Utils.notityIdentityUpdate(notifyIds, self.getAccount_id(), club.getClubId(), self.getIdentity());
			Utils.notityIdentityUpdate(notifyIds, target.getAccount_id(), club.getClubId(), target.getIdentity());
			Utils.sendTip(topReq.getAccountId(), "亲友圈转让成功!", ESysMsgType.NONE, session);
			session.send(
					PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.COMMON_CODE, CommonCodeRsp.newBuilder().setCode(ICommonCode.CLUB_ASSIGNMENT)));

			logger.warn("玩家[{}] 转让亲友圈[{}]给玩家[{}]!", self.getAccount_id(), club.getClubId(), target.getAccount_id());
		});
	}
}
