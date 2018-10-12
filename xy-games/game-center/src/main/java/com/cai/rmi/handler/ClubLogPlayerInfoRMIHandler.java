package com.cai.rmi.handler;

import java.util.ArrayList;
import java.util.Date;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PublicServiceImpl;

/**
 * 
 *
 * @author zhanglong date: 2018年6月15日 下午5:52:48
 */
@IRmi(cmd = RMICmd.CLUB_LOG_PLAYER_INFO, desc = "俱乐部玩家数据(仅供俱乐部数据统计时使用)")
public class ClubLogPlayerInfoRMIHandler extends IRMIHandler<ArrayList<Long>, PlayerViewVO> {

	@Override
	protected PlayerViewVO execute(ArrayList<Long> list) {
		long accountId = list.get(0);
		boolean isJoinClub = list.get(1) == 1;
		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		if (account == null) {
			return null;
		}
		AccountModel accountModel = account.getAccountModel();
		if (null == accountModel) {
			return null;
		}

		PlayerViewVO vo = new PlayerViewVO();
		vo.setAccountId(accountId);
		vo.setCreate_time(accountModel.getCreate_time());

		if (isJoinClub && accountModel.getFirstJoinClubTime() == null) {
			accountModel.setFirstJoinClubTime(new Date());
			accountModel.setNeedDB(true);
			vo.setFirstJoinClub(true);
		}
		vo.setFirstJoinClubTime(accountModel.getFirstJoinClubTime());

		return vo;
	}

}
