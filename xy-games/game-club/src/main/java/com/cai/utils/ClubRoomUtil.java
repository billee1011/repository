package com.cai.utils;

import java.util.Date;

import com.cai.common.define.EGoldOperateType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ClubRuleCostLogModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.constant.ClubRuleTable;
import com.cai.service.MongoDBServiceImpl;

/**
 * @author wu_hc date: 2017年12月5日 下午4:04:20 <br/>
 */
public final class ClubRoomUtil {

	public static AddGoldResultModel subGold(long account_id, int gold, boolean isExceed, String desc, EGoldOperateType eGoldOperateType) {
		gold = gold * -1;
		if (gold > 0) {
			AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
			addGoldResultModel.setSuccess(false);
			addGoldResultModel.setMsg("扣卡数量要大于0");
			return addGoldResultModel;
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account_id, gold, isExceed, desc, eGoldOperateType);

		// 同步状态到代理服
		//		if (addGoldResultModel.isSuccess()) {
		//
		//			RoomWealthProto.Builder builder = WealthUtil.newWealthBuilder(account_id, EWealthCategory.GOLD, EGoldOperateType.OPEN_ROOM.getId(), gold);
		//			int proxyIndex = SessionService.getInstance().getProxyByServerIndex(account_id);
		//			if (proxyIndex != -1) {
		//				SessionService sender = SessionService.getInstance();
		//				sender.sendMsg(EServerType.PROXY, proxyIndex, PBUtil.toS2SResponse(S2SCmd.WEALTH_UPDATE, builder));
		//			}
		//		}
		return addGoldResultModel;
	}

	public static AddGoldResultModel addGold(long account_id, int gold, boolean isExceed, String desc, EGoldOperateType eGoldOperateType) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account_id, gold, isExceed, desc, eGoldOperateType);

		// 同步状态到代理服
		//		if (addGoldResultModel.isSuccess() && WealthUtil.roomGoldType.contains(eGoldOperateType)) {
		//			RoomWealthProto.Builder builder = WealthUtil.newWealthBuilder(account_id, EWealthCategory.GOLD, eGoldOperateType.getId(), gold);
		//			int proxyIndex = SessionService.getInstance().getProxyByServerIndex(account_id);
		//			if (proxyIndex != -1) {
		//				SessionService sender = SessionService.getInstance();
		//				sender.sendMsg(EServerType.PROXY, proxyIndex, PBUtil.toS2SResponse(S2SCmd.WEALTH_UPDATE, builder));
		//			}
		//		}

		return addGoldResultModel;
	}

	/**
	 * 落地俱乐部包间消耗
	 */
	public static void saveRuleCostModel(final ClubRuleTable ruleTable, int clubMemberCount, Date date) {
		if (null == ruleTable) {
			return;
		}
		if (null == date)
			date = new Date();

		try {
			ClubRuleCostLogModel costLogModel = ruleTable.getCostLogModel();
			if (null == costLogModel) {
				return;
			}
			costLogModel.setCreate_time(date);
			costLogModel.setClubMemberCount(clubMemberCount);
			MongoDBServiceImpl.getInstance().getLogQueue().add(costLogModel);
			ruleTable.setNullCostLogModel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
