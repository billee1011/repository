package com.cai.rmi.handler;

import java.util.Collections;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubMiniVo;
import com.cai.constant.Club;
import com.cai.service.ClubService;

import protobuf.clazz.ClubMsgProto.ClubSimple;

/**
 * 
 *
 * @author wu_hc date: 2017年8月02日 上午16:11:00 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_LIST, desc = "俱乐部列表-mini数据")
public final class RmiClubHandler extends IRMIHandler<List<ClubMiniVo>, List<ClubMiniVo>> {
	@Override
	protected List<ClubMiniVo> execute(List<ClubMiniVo> vos) {

		if (null == vos || vos.isEmpty()) {
			return Collections.emptyList();
		}

		vos.forEach((vo) -> {

			if (ClubMiniVo.Type.ACCOUNT_ID == vo.getType()) {
				ClubSimple.Builder builder = ClubService.getInstance().encodeSimpleClubs(vo.getAccountId(), vo.isMyCreate());
				vo.getClubs().add(builder.build());

			} else if (ClubMiniVo.Type.CLUB_ID == vo.getType()) {

				Club club = ClubService.getInstance().getClub(vo.getClubId());
				if (null != club) {
					vo.getClubs().add(ClubSimple.newBuilder().addClubs(club.encodeMini()).build());
				}
			} else {
				logger.error("后台rmi请求俱乐部列表，但消息类型[{}]不存在!", vo.getType());
			}
		});
		return vos;
	}
}
