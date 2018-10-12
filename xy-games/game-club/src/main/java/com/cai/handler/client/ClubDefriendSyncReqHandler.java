package com.cai.handler.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.tasks.db.ClubMemberUpdateIdentityDBTask;
import com.cai.utils.Utils;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/9 10:25
 */
@ICmd(code = C2SCmd.CLUB_DEFRIEND_SYNC_REQ, desc = "同步亲友圈黑名单")
public class ClubDefriendSyncReqHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club sourceClub = ClubService.getInstance().getClub(req.getClubId());
		if (null == sourceClub) {
			return;
		}
		long operatorId = topReq.getAccountId();
		sourceClub.runInReqLoop(() -> {
			if (sourceClub.getIdentify(operatorId) != EClubIdentity.CREATOR) {
				Utils.sendTip(topReq.getAccountId(), "无此权限！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}
			Collection<Club> clubs = ClubService.getInstance().getMyCreateClub(operatorId);
			if (clubs.size() <= 1) {
				Utils.sendTip(topReq.getAccountId(), "没有其它创建的亲友圈！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}
			List<Long> deFriendsList = new ArrayList<>();
			sourceClub.members.forEach((id, memberModel) -> {
				if (sourceClub.getIdentify(id) == EClubIdentity.DEFRIEND) {
					deFriendsList.add(id);
				}
			});
			if (deFriendsList.isEmpty()) {
				Utils.sendTip(topReq.getAccountId(), "亲友圈没有暂停娱乐玩家！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}

			for (Club destClub : clubs) {
				if (destClub.getClubId() == sourceClub.getClubId()) {
					continue;
				}
				destClub.runInReqLoop(() -> {
					ClubMemberModel operator = destClub.members.get(operatorId);
					List<ClubMemberModel> targetList = new ArrayList<>();
					List<ClubJoinQuitMsgProto> msgList = new ArrayList<>();
					Map<Long, Integer> targetMap = new HashMap<>();
					for (Long targetId : deFriendsList) {
						ClubMemberModel targetMember = destClub.members.get(targetId);
						if (null == targetMember) {
							continue;
						}
						if (destClub.getIdentify(targetId) == EClubIdentity.DEFRIEND) {
							continue;
						}

						targetMember.setIdentity(EClubIdentity.DEFRIEND.identify());
						targetList.add(targetMember);
						Set<Long> tmpSets = Sets.newHashSet();
						tmpSets.add(targetId);
						Utils.notityIdentityUpdate(tmpSets, targetId, destClub.getClubId(), targetMember.getIdentity());
						targetMap.put(targetId, (int) targetMember.getIdentity());

						// 事件
						ClubJoinQuitMsgProto.Builder eventMsg = Utils
								.newEventMsg(destClub.getClubId(), ClubJoinQuitMsgProto.MsgType.SET_DEFRIEND, operator, targetMember);
						destClub.joinQuitMsgQueueProto.offer(eventMsg.build());
						msgList.add(eventMsg.build());
					}
					if (targetMap.size() > 0) {
						Set<Long> notifyIds = Sets.newHashSet(destClub.getManagerIds());
						Utils.notityIdentityUpdateBatch(notifyIds, destClub.getClubId(), targetMap);
					}

					if (!targetList.isEmpty()) {
						destClub.runInDBLoop(new ClubMemberUpdateIdentityDBTask(targetList));
					}
					if (!msgList.isEmpty()) {
						Utils.sendClubEventMsgBatch(destClub.getManagerIds(), destClub.getClubId(), msgList);
					}
				});
			}
			Utils.sendTip(topReq.getAccountId(), "同步暂停娱乐列表成功！", ESysMsgType.INCLUDE_ERROR, session);
		});
	}
}
