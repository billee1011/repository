package com.cai.rmi.handler;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PublicServiceImpl;
import com.cai.util.AccountUtil;


@IRmi(cmd = RMICmd.MATCH_PLAYER_INFO, desc = "比赛玩家数据")
public class MatchPlayerInfoRMIHandler extends IRMIHandler<List<Long>, List<PlayerViewVO>>{

	@Override
	protected List<PlayerViewVO> execute(List<Long> req) {
		List<PlayerViewVO> list = new ArrayList<>();
		
		req.forEach((accountId)->{
			Account account = PublicServiceImpl.getInstance().getAccount(accountId);
			if (account == null) {
				return;
			}
			AccountModel accountModel = account.getAccountModel();
			if (null == accountModel) {
				return;
			}

			PlayerViewVO vo = AccountUtil.getVo(account);
			list.add(vo);
		});
		return list;
	}

}
