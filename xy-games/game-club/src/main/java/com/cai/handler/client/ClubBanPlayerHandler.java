package com.cai.handler.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubSetBanPlayerProtoReq;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年5月28日 下午6:20:26
 */
@ICmd(code = C2SCmd.CLUB_BAN_PLAYER_SET, desc = "亲友圈禁止同桌")
public class ClubBanPlayerHandler extends IClientExHandler<ClubSetBanPlayerProtoReq> {

	@Override
	protected void execute(ClubSetBanPlayerProtoReq req, TransmitProto topReq, C2SSession session) throws Exception {
		long beOpeAccountId = req.getAccountId();
		List<Long> targetIds = req.getTargetAccountsList();
		if (targetIds == null) {
			return;
		}
		// 管理员可操作除自己、创建人之外的所有人
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator) {
				return;
			}
			if (!EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			final ClubMemberModel beOperator = club.members.get(beOpeAccountId);
			if (null == beOperator) {
				return;
			}
			// 先清掉旧的禁玩玩家
			Map<Long, Long> banPlayerMap = beOperator.getMemberBanPlayerMap();
			if (banPlayerMap != null) {
				List<Long> delList = new ArrayList<>();
				for (Long targetId : banPlayerMap.keySet()) {
					ClubMemberModel target = club.members.get(targetId);
					if (target != null) {
						target.removeBanPlayer(beOpeAccountId);
					}
					delList.add(targetId);
				}
				for (Long id : delList) {
					beOperator.removeBanPlayer(id);
				}
			}

			for (Long targetId : targetIds) {
				ClubMemberModel target = club.members.get(targetId);
				if (target == null) {
					continue;
				}
				beOperator.addBanPlayer(targetId);
				target.addBanPlayer(beOpeAccountId);
			}
			Utils.sendTip(topReq.getAccountId(), "设置成功！", ESysMsgType.INCLUDE_ERROR, session);
		});
	}

}
