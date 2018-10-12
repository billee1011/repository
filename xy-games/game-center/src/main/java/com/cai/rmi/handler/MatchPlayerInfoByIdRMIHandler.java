package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PublicServiceImpl;
import com.cai.util.AccountUtil;


@IRmi(cmd = RMICmd.MATCH_PLAYER_INFO_ID, desc = "单个比赛玩家数据")
public class MatchPlayerInfoByIdRMIHandler extends IRMIHandler<Long, PlayerViewVO>{

	@Override
	protected PlayerViewVO execute(Long accountId) {
		PlayerViewVO vo = null;
		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		if (account == null) {
			return vo;
		}
		AccountModel accountModel = account.getAccountModel();
		if (null == accountModel) {
			return vo;
		}

		vo = AccountUtil.getVo(account);
		return vo;
	}

}
