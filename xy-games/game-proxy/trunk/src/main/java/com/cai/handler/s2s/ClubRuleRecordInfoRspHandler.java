package com.cai.handler.s2s;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.handler.IServerHandler;
import com.cai.common.type.ClubRecordDayType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.TimeUtil;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.core.Global;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.ClubMsgProto.ClubRuleRecordProto;
import protobuf.clazz.ClubMsgProto.ClubRuleRecordResponse;

/**
 * 
 *
 * @author zhanglong date: 2018年7月16日 上午11:37:25
 */
@IServerCmd(code = S2SCmd.CLUB_RULE_RECORD_INFO, desc = "俱乐部包间记录数据 ")
public class ClubRuleRecordInfoRspHandler extends IServerHandler<ClubRuleRecordResponse> {

	@Override
	public void execute(ClubRuleRecordResponse resp, S2SSession session) throws Exception {
		C2SSession client = C2SSessionService.getInstance().getSession(resp.getAccountId());
		if (null == client) {
			return;
		}
		if (!GbCdCtrl.canHandleMust(client, Opt.CLUB_RULE_RECORD))
			return;
		Global.getService(Global.SERVER_LOGIC).execute(() -> {
			int clubId = resp.getClubId();
			int requestType = resp.getRequestType();
			long beginTime = 0;
			long endTime = 0;
			Date now = new Date();
			if (requestType == ClubRecordDayType.TODAY) {// 今天
				beginTime = TimeUtil.getTimeStart(now, 0);
				endTime = now.getTime();
			} else if (requestType == ClubRecordDayType.YESTERDAY) {// 昨天
				beginTime = TimeUtil.getTimeStart(now, -1);
				endTime = TimeUtil.getTimeEnd(now, -1);
			} else if (requestType == ClubRecordDayType.BEFORE_YESTERDAY) {// 前天
				beginTime = TimeUtil.getTimeStart(now, -2);
				endTime = TimeUtil.getTimeEnd(now, -2);
			} else if (requestType == ClubRecordDayType.EIGHT) {// 八天
				beginTime = TimeUtil.getTimeStart(now, -7);
				endTime = now.getTime();
			} else if (requestType == ClubRecordDayType.ALL) {// 全部
				beginTime = TimeUtil.getTimeStart(now, -7);
				endTime = now.getTime();
			} 
			if (beginTime == 0 || endTime == 0) {
				client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_RULE_RECORD_RSP, resp.toBuilder()));
				return;
			}
			ClubRuleRecordResponse.Builder builder = resp.toBuilder();
			List<ClubRuleRecordProto.Builder> recordList = builder.getRuleRecordBuilderList();
			for (ClubRuleRecordProto.Builder record : recordList) {
				List<BrandLogModel> brandLogModels = null;
				Map<String, Object> param = Maps.newHashMapWithExpectedSize(1);
				param.put("ruleId", record.getRuleId());
				brandLogModels = MongoDBServiceImpl.getInstance().getClubParentBrandList(null, clubId, beginTime, endTime, param);
				if (brandLogModels == null) {
					continue;
				}
				int tmpGameCount = 0;
				int tmpExclusiveGold = 0;
				int tmpGold = 0;
				tmpGameCount = brandLogModels.size();
				for (BrandLogModel branModel : brandLogModels) {
					if (branModel.isExclusiveGold()) {
						tmpExclusiveGold += branModel.getGold_count();
					} else {
						tmpGold += branModel.getGold_count();
					}
				}
				
				record.setGameCount(tmpGameCount);
				record.setExclusiveGold(tmpExclusiveGold);
				record.setGold(tmpGold);
			}

			client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_RULE_RECORD_RSP, builder));
		});
	}
}
