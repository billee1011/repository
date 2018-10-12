/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.rmi.handler;

import java.util.Map;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;

/**
 * @author wu_hc date: 2018年8月7日 上午11:58:53 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_TRANSFER, desc = "俱乐部转让")
public final class ClubTransiferRMIHandler extends IRMIHandler<Map<String, Object>, String> {

	@Override
	public String execute(Map<String, Object> arg) {
		logger.warn("来自后台设置亲友圈创始人,参数:{}", arg);

		Integer clubId = get(arg, "clubId", Integer.class);
		if (null == clubId || (clubId.intValue() < 1000000 || clubId.intValue() > 9999999)) {
			return "俱乐部id参数异常!";
		}

		Long newOwnerId = get(arg, "newOwnerId", Long.class);
		if (null == newOwnerId) {
			return "没有设置新创始人Id,newOwnerId is null!";
		}

		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return "俱乐部不存在!";
		}

		ClubMemberModel newOwner = club.members.get(newOwnerId);
		if (null == newOwner) {
			return String.format("玩家:%s 不是俱乐部成员，操作失败!", newOwnerId);
		}

		Byte identity = get(arg, "identity", Byte.class);
		EClubIdentity eClubIdentity;
		if (null != identity && null != (eClubIdentity = EClubIdentity.of(identity.byteValue())) && eClubIdentity != EClubIdentity.CREATOR) {
			club.runInReqLoop(() -> {
				newOwner.setIdentity(identity.byteValue());
				club.runInDBLoop(() -> SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountIdentity(newOwner));
			});
		} else {
			club.runInReqLoop(() -> club.transferOwner(newOwnerId));
		}

		return "操作成功！";
	}
}
