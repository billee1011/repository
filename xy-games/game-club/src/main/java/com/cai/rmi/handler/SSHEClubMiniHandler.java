package com.cai.rmi.handler;

import java.util.Collections;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubSSHEMiniVo;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.google.common.collect.Lists;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月9日 下午8:02:32 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_SSHE_MINI_LIST, desc = "俱乐部列表-mini数据")
public final class SSHEClubMiniHandler extends IRMIHandler<List<Integer>, List<ClubSSHEMiniVo>> {
	@Override
	protected List<ClubSSHEMiniVo> execute(List<Integer> clubIds) {

		if (null == clubIds || clubIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<ClubSSHEMiniVo> vos_ = Lists.newArrayList();
		clubIds.forEach((clubId) -> {
			Club club = ClubService.getInstance().getClub(clubId);
			if (null != club) {
				vos_.add(ClubSSHEMiniVo.newVO(clubId, club.getOwnerId(), club.getClubName()));
			}
		});
		return vos_;
	}
}
