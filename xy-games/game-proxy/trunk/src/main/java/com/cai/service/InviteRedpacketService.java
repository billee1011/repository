/**
 * 湘ICP备15020076 copyright@2015-2016湖南旗胜网络科技有限公司
 */
package com.cai.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.InviteActiveModel;
import com.cai.common.domain.InviteRedPacketModel;
import com.cai.common.domain.InviteResultModel;
import com.cai.common.domain.json.PrizeJsonModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.InviteActiveDict;

import protobuf.clazz.activity.InviteRedpacketProto.ActivityRuleResp;
import protobuf.clazz.activity.InviteRedpacketProto.GetInviteRedpacketResp;
import protobuf.clazz.activity.InviteRedpacketProto.GetRedPackData;
import protobuf.clazz.activity.InviteRedpacketProto.InviteRecordData;
import protobuf.clazz.activity.InviteRedpacketProto.InviteRecordResp;
import protobuf.clazz.activity.InviteRedpacketProto.RankData;
import protobuf.clazz.activity.InviteRedpacketProto.RankListInviteRedpacketResp;

/**
 * 邀请新用户送红包活动
 *
 * @author tang
 */
public final class InviteRedpacketService {

	/**
	 * 
	 */
	private static final InviteRedpacketService INSTANCE = new InviteRedpacketService();

	private long rankTime = 0;
	/**
	 * 
	 */
	private List<RankData> redPackageRankList = new ArrayList<RankData>();

	private InviteRedpacketService() {
	}

	private Map<Long,Integer> rankMap = new HashMap<>();
	/**
	 * 
	 * @return
	 */
	public static InviteRedpacketService getInstance() {
		return InviteRedpacketService.INSTANCE;
	}

	// 获取排行榜数据 key为计算排行榜的时间戳,只需取values就
	public List<RankData> getRedPackageRankList() {
		if (rankTime == 0) {
			initRedPackageRankList();
		} else {
			long nowTime = System.currentTimeMillis();
			if (nowTime > rankTime + 5 * 60 * 1000) {
				initRedPackageRankList();
			}
		}
		return redPackageRankList;
	}

	private void initRedPackageRankList() {
		List<Entry<Long, Long>> top10 = MongoDBServiceImpl.getInstance().getInviteRedPacketRank();
		rankTime = System.currentTimeMillis();
		// 排行是空的
		if (top10 == null || top10.size() == 0) {
			redPackageRankList.addAll(new ArrayList<RankData>());
		} else {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			List<RankData> rankList = new ArrayList<RankData>();
			int i = 1;
			for (Entry<Long, Long> e : top10) {
				AccountSimple targetAccount = centerRMIServer.getSimpleAccount(e.getKey());
				RankData.Builder model = RankData.newBuilder();
				if (targetAccount != null) {
					model.setNickName(targetAccount.getNick_name());
				} else {
					model.setNickName("-");
				}
				model.setRanking(i);
				model.setInvitePersons(e.getValue().intValue());
				rankList.add(model.build());
				rankMap.put(e.getKey(), i);
				if(i==10){
					break;
				}
				i++;
			}
			redPackageRankList = rankList;
		}
	}
	//我的红包
	public GetInviteRedpacketResp processGetInviteRedpacketResp(long accountId) {
		MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
		InviteRedPacketModel inviteRedPacketModel = mongoDBServiceImpl.getLastInviteAccountId(accountId);
		GetInviteRedpacketResp.Builder builder = GetInviteRedpacketResp.newBuilder();
		// 用户状态,1未成功邀请绑定任何人,2已成功邀请绑定但绑定人未完成局数时,3已成功邀请绑定且已获得奖励时状态,4已成功绑定但未完成时状态
		// 表示没有邀请任何人
		if (inviteRedPacketModel == null) {
			builder.setState(1);
			return builder.build();
		}
		// 获取红包金额总数
		InviteResultModel redPacketModel = mongoDBServiceImpl.getInviteRedpacketReceive(accountId);
		// 获取邀请人总数
		long inviteCount = mongoDBServiceImpl.getInvitePersonsCount(accountId);
		if (inviteRedPacketModel.getState() == 0) {
			if (redPacketModel == null) {
				builder.setState(4);
				
			} else {
				builder.setState(2);
			}
		} else {
			builder.setState(3);
		}
		GetRedPackData.Builder getData = GetRedPackData.newBuilder();
		// 没有获得任何红包
		if (redPacketModel == null) {
			getData.setReceiveMoney(0);
		} else {
			getData.setReceiveMoney(redPacketModel.getCount().intValue());
		}
		// 邀请总人数
		getData.setInvitePersons((int)inviteCount);
		long effectiveCount = mongoDBServiceImpl.getEffectiveInvitePersonsCount(accountId);
		getData.setEffectiveCount((int)effectiveCount);
		getData.setRank(rankMap.get(accountId)==null?0:rankMap.get(accountId));
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AccountSimple sim = centerRMIServer.getSimpleAccount(inviteRedPacketModel.getTarget_account_id());
		getData.setLastInviteHeadPic(sim.getIcon());
		getData.setLastInviteNick(sim.getNick_name());
		builder.setGetData(getData);
		return builder.build();
	}
	//排行榜
	public RankListInviteRedpacketResp processRankListInviteRedpacketResp(long accountId) {
		RankListInviteRedpacketResp.Builder builder = RankListInviteRedpacketResp.newBuilder();
		builder.addAllRankDataList(getRedPackageRankList());
		return builder.build();
	}
	//邀请记录
	public InviteRecordResp processInviteRecordResp(long accountId,int curPage,int pageSize) {
		MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
		List<InviteRedPacketModel> list = mongoDBServiceImpl.getInviteAccountList(accountId, curPage, pageSize);
		InviteRecordResp.Builder builder = InviteRecordResp.newBuilder();
		if(list.size()==0){
			return builder.build();
		}else{
			long inviteCount = mongoDBServiceImpl.getInvitePersonsCount(accountId);
			builder.setTotalInvite((int)inviteCount);
			for(InviteRedPacketModel model:list){
				InviteRecordData.Builder iBuilder = InviteRecordData.newBuilder();
				iBuilder.setHeadPic(model.getIcon()==null?"":model.getIcon());
				iBuilder.setState(model.getState());
				builder.addInviteRecordDataList(iBuilder);
			}
		}
		builder.setCurPage(curPage);
		return builder.build();
	}

	public ActivityRuleResp processGetActivityRuleResp(long accountId) {
		ActivityRuleResp.Builder builder = ActivityRuleResp.newBuilder();
		InviteActiveModel model = InviteActiveDict.getInstance().getInviteActiveModel();
		if(model==null||model.getId()==0){
			return builder.build();
		}
		builder.setActiveDesc(model.getActive_desc());
		builder.setActiveName(model.getActive_name());
		long sTime = model.getBegin_time().getTime()/1000;
		long eTime = model.getEnd_time().getTime()/1000;
		builder.setBeginTime((int)sTime);
		builder.setEndTime((int)eTime);
		builder.setInvitePay(model.getInvite_pay());
		builder.setOpenTime(model.getOpenTime());
		builder.addAllPrizeDataList(InviteActiveDict.getInstance().prizeList.stream().map(PrizeJsonModel::encode).collect(Collectors.toList()));
		builder.setActivePic(model.getActive_desc_bg_img());
		return builder.build();
	}

}
