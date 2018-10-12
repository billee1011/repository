/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.Optional;
import java.util.Set;

import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.ClubMsgProto.ClubAccountProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月13日 下午2:58:29 <br/>
 */
@ICmd(code = S2SCmd.CLUB_SYNC_MEMBER_INFO, desc = "成员信息变化")

public class ClubMemberInfoUpdateHandler extends IClientHandler<ClubAccountProto> {

	@Override
	protected void execute(ClubAccountProto proto, C2SSession session) throws Exception {
		final long accountId = proto.getAccountId();
		if (accountId <= 0L || (!proto.hasAvatar() && !proto.hasNickname())) {
			return;
		}

		Optional<Set<Integer>> opt = ClubCacheService.getInstance().optMemberClubs(accountId);
		if (opt.isPresent()) {
			opt.get().forEach((clubId) -> {
				Club club = ClubService.getInstance().getClub(clubId);
				if (null != club) {
					ClubMemberModel member = club.members.get(accountId);
					if (null != member) {
						if (proto.hasNickname()) {
							member.setNickname(proto.getNickname());
						}
						if (proto.hasAvatar()) {
							member.setAvatar(proto.getAvatar());

							// 如果俱乐是自己的俱乐部
							if (club.getOwnerId() == accountId) {
								club.clubModel.setAvatar(proto.getAvatar());
							}
						}

						logger.warn("俱乐部[{}],成员[{}]信息有刷新,avatar:[{}] ,nickName:[{}]", clubId, accountId, member.getAvatar(), member.getNickname());
					}
				}

			});
		}
	}
}
