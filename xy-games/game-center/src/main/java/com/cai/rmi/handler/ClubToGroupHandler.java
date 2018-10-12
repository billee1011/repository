package com.cai.rmi.handler;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.GroupClubMemberService;

/**
 * 
 * 
 *
 * @author tang date: 2017年11月27日 下午3:41:24 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_TO＿GROUP, desc = "俱乐部成员同步到群中")
public final class ClubToGroupHandler extends IRMIHandler<HashMap<String,String>, Integer> {

	@Override
	public Integer execute(HashMap<String,String> paramsMap) {
		String type = paramsMap.get("type");
		int result = -1;
		if(StringUtils.isBlank(type)){
			String groupId = paramsMap.get("groupId");
			String clubId = paramsMap.get("clubId");
			if(StringUtils.isBlank(clubId)||StringUtils.isBlank(groupId)){
				return -1;//参数有误
			}
			result = GroupClubMemberService.getInstance().ClubToGroup(Integer.parseInt(clubId), groupId);
		}else{
			String groupId = paramsMap.get("groupId");
			String accountId = paramsMap.get("accountId");
			result = GroupClubMemberService.getInstance().kickGroup(Long.parseLong(accountId), groupId);
		}
		return result;
	}

}
