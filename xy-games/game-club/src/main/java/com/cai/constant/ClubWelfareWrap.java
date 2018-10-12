package com.cai.constant;

import java.util.Date;
import java.util.List;

import com.cai.common.ClubMemWelfareLotteryInfo;
import com.cai.common.ClubWelfareCode;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.ClubWelfareRewardModel;
import com.cai.common.domain.ClubWelfareSwitchModel;
import com.cai.common.domain.log.ClubMemberWelfareChangeLogModel;
import com.cai.common.domain.log.ClubWelfareLotteryMsgLogModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.dao.ClubDao;
import com.cai.dictionary.ClubWelfareSwitchModelDict;
import com.cai.dictionary.ItemDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubDaoService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.SessionService;
import com.cai.utils.Utils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.s2s.S2SProto;

import static protobuf.clazz.ClubMsgProto.ClubEventProto;
import static protobuf.clazz.ClubMsgProto.ClubWelfareLotteryNotify;
import static protobuf.clazz.ClubMsgProto.ClubWelfareLotteryResponse;
import static protobuf.clazz.ClubMsgProto.ClubWelfareRewardProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/20 14:37
 */
public class ClubWelfareWrap {

	private static final Logger logger = LoggerFactory.getLogger(ClubWelfareWrap.class);

	private Club club;

	private ClubWelfareLotteryLogWrap clubWelfareLotteryLogWrap;

	ClubWelfareWrap(Club club) {
		this.club = club;
	}

	public long getTotalClubWelfare() {
		return club.clubModel.getTotalClubWelfare();
	}

	public void setTotalClubWelfare(long totalClubWelfare) {
		club.clubModel.setTotalClubWelfare(totalClubWelfare);
	}

	/**
	 * 是否开启了亲友圈福卡
	 */
	public boolean isOpenClubWelfare() {
		return club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_WELFARE_SWITCH);
	}

	/**
	 * 检查是否可领取福卡
	 */
	public boolean canGetWelfare() {
		ClubWelfareSwitchModel switchModel = ClubWelfareSwitchModelDict.getInstance().getClubWelfareSwitchModel();
		if (switchModel != null) {
			return getTotalClubWelfare() < switchModel.getCanGetCond();
		}
		return false;
	}

	/**
	 * 关闭亲友圈福卡功能
	 */
	public void closeClubWelfare() {
		club.setsModel.statusDel(EClubSettingStatus.CLUB_WELFARE_SWITCH);
		club.clubModel.setSettingStatus(club.setsModel.getStatus());
		// 福卡功能关闭时清除相关数据
		//清除亲友圈福卡
		setTotalClubWelfare(0);
		//清除玩家福卡
		List<ClubMemberModel> list = Lists.newArrayList();
		club.members.forEach((id, member) -> {
			if (member.getClubWelfare() > 0) {
				list.add(member);
				member.setClubWelfare(0);
			}
			ClubCacheService.getInstance().removeWelfareLotteryMember(member.getAccount_id());
		});
		ClubDao clubDao = SpringService.getBean(ClubDaoService.class).getDao();
		club.runInDBLoop(() -> clubDao.batchUpdate("updateClubAccountWelfare", list));
		//清除包间福卡设置
		club.ruleTables.forEach((ruleId, ruleTable) -> {
			ClubRuleModel ruleModel = club.clubModel.getRule(ruleId);
			if (ruleModel != null) {
				if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.CLUB_WELFARE_SWITCH)) {
					ruleModel.getSetsModel().statusDel(ERuleSettingStatus.CLUB_WELFARE_SWITCH);
					ruleModel.setLimitWelfare(0);
					ruleModel.setLotteryCost(0);
					club.runInDBLoop(() -> clubDao.updateClubRule(ruleModel));
				}
			}
		});
	}

	/**
	 * 开启亲友圈福卡功能
	 */
	public void openClubWelfare() {
		club.setsModel.statusAdd(EClubSettingStatus.CLUB_WELFARE_SWITCH);
		club.clubModel.setSettingStatus(club.setsModel.getStatus());
		club.clubModel.setClubWelfareGetCount(0);
		// 福卡功能开启时赠送配置的福卡
		ClubWelfareSwitchModel switchModel = ClubWelfareSwitchModelDict.getInstance().getClubWelfareSwitchModel();
		if (switchModel != null) {
			setTotalClubWelfare(switchModel.getFirstSendNum());
			// 通知所有管理员亲友圈福卡数量变化
			ClubEventProto.Builder eventBuilder = ClubEventProto.newBuilder();
			eventBuilder.setClubId(club.getClubId());
			eventBuilder.setEventCode(ClubEventCode.WELFARE_CHANGE);
			Utils.sendClient(club.getManagerIds(), S2CCmd.CLUB_EVENT_RSP, eventBuilder);
		}
	}

	/**
	 * 离开亲友圈
	 */
	public void outClub(ClubMemberModel memberModel) {
		if (isOpenClubWelfare()) {
			setTotalClubWelfare(getTotalClubWelfare() + memberModel.getClubWelfare());
		}
		ClubCacheService.getInstance().removeWelfareLotteryMember(memberModel.getAccount_id());
	}

	public void lotteryReward(ClubMemberModel memberModel, ClubMemWelfareLotteryInfo lotteryInfo, int index) {
		ClubCacheService.getInstance().removeWelfareLotteryMember(memberModel.getAccount_id());

		if (index < 0 || index > 4) {
			logger.error("clubwelfare lottery index={} error,accountId={}", index, memberModel.getAccount_id());
			Utils.sendTip(memberModel.getAccount_id(), "抽奖异常", ESysMsgType.INCLUDE_ERROR);
		}

		int realCost = (memberModel.getClubWelfare() > lotteryInfo.getCost()) ? lotteryInfo.getCost() : (int) memberModel.getClubWelfare();
		//扣除福卡
		memberModel.setClubWelfare(Math.max(memberModel.getClubWelfare() - lotteryInfo.getCost(), 0));
		club.runInDBLoop(() -> SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountWelfare(memberModel));

		//展示奖励
		List<ClubWelfareRewardModel> showRewardList = ClubWelfareLotteryUtil.randomShowReward(4);
		if (showRewardList.size() < 4) {
			Utils.sendTip(memberModel.getAccount_id(), "抽奖异常！", ESysMsgType.INCLUDE_ERROR);
			return;
		}
		//实际奖励
		List<ClubWelfareRewardModel> realRewardList = ClubWelfareLotteryUtil.randomRealReward(1);
		if (realRewardList.size() < 1) {
			Utils.sendTip(memberModel.getAccount_id(), "抽奖异常！！！", ESysMsgType.INCLUDE_ERROR);
			return;
		}
		//发放奖励
		S2SProto.ClubWelfareLotteryRewardProto.Builder rewardBuilder = S2SProto.ClubWelfareLotteryRewardProto.newBuilder();
		rewardBuilder.setAccountId(memberModel.getAccount_id());
		for (ClubWelfareRewardModel model : realRewardList) {
			S2SProto.LotteryReward.Builder reward = S2SProto.LotteryReward.newBuilder();
			reward.setItemId(model.getAwardId());
			reward.setItemNum(model.getAwardNum());
			rewardBuilder.addRewards(reward);
		}
		SessionService.getInstance().sendGate(1, PBUtil.toS2SRequet(S2SCmd.S_2_M, S2SProto.S2STransmitProto.newBuilder().setAccountId(0)
				.setRequest(PBUtil.toS2SResponse(S2SCmd.CLUB_WELFARE_LOTTERY_REWARD_TO_MATCH, rewardBuilder))).build());
		//通知客户端
		ClubWelfareLotteryResponse.Builder b = ClubWelfareLotteryResponse.newBuilder();
		b.setClubId(club.getClubId());
		List<ClubWelfareRewardProto> rewardList = Lists.newArrayList();
		ClubWelfareRewardModel realReward = null;
		for (int i = 0; i < 5; i++) {
			if (i == index) {
				realReward = realRewardList.remove(0);
				rewardList.add(toRewardBuilder(true, realReward));
			} else {
				rewardList.add(toRewardBuilder(false, showRewardList.remove(0)));
			}
		}
		b.addAllRewards(rewardList);
		Utils.sendClient(memberModel.getAccount_id(), S2CCmd.CLUB_WELFARE_LOTTERY_RSP, b);

		//		club.sendHaveNewMsg(ERedHeartCategory.CLUB_WELFARE_LOTTERY);

		// 抽奖记录
		recordLotteryMsgLog(memberModel, realReward, lotteryInfo, realCost);
	}

	public void initLotteryLogMsg(List<ClubWelfareLotteryMsgLogModel> clubWelfareLotteryMsgLogModels) {
		clubWelfareLotteryLogWrap = new ClubWelfareLotteryLogWrap();
		clubWelfareLotteryLogWrap.initData(clubWelfareLotteryMsgLogModels);
	}

	private void recordLotteryMsgLog(ClubMemberModel memberModel, ClubWelfareRewardModel realReward, ClubMemWelfareLotteryInfo lotteryInfo,
			int realCost) {
		Date now = new Date();
		// 福卡抽奖日志
		ClubWelfareLotteryMsgLogModel lotteryLogModel = new ClubWelfareLotteryMsgLogModel();
		lotteryLogModel.setCreate_time(now);
		lotteryLogModel.setClubId(this.club.getClubId());
		lotteryLogModel.setAccountId(memberModel.getAccount_id());
		lotteryLogModel.setNickname(memberModel.getNickname());
		if (realReward != null) {
			lotteryLogModel.setAwardId(realReward.getAwardId());
			lotteryLogModel.setAwardNum(realReward.getAwardNum());
		}

		lotteryLogModel.setCostNum(realCost);
		lotteryLogModel.setGameTypeIndex(lotteryInfo.getGameTypeIndex());
		String subGameName = SysGameTypeDict.getInstance().getMJname(lotteryInfo.getGameTypeIndex());
		if (!Strings.isNullOrEmpty(subGameName)) {
			lotteryLogModel.setSubName(subGameName);
		}

		MongoDBServiceImpl.getInstance().getLogQueue().add(lotteryLogModel);

		clubWelfareLotteryLogWrap.addLotteryLog(lotteryLogModel);

		// 成员福卡变动日志
		ClubMemberWelfareChangeLogModel changeLogModel = new ClubMemberWelfareChangeLogModel();
		changeLogModel.setCreate_time(now);
		changeLogModel.setClubId(this.club.getClubId());
		changeLogModel.setAccountId(memberModel.getAccount_id());
		changeLogModel.setType(ClubWelfareCode.MEMBER_WELFARE_CHANGE_LOTTERY);
		if (!Strings.isNullOrEmpty(subGameName)) {
			changeLogModel.setSubName(subGameName);
		}
		changeLogModel.setCostNum(-realCost);
		MongoDBServiceImpl.getInstance().getLogQueue().add(changeLogModel);
	}

	public ClubWelfareLotteryLogWrap getClubWelfareLotteryLogWrap() {
		return clubWelfareLotteryLogWrap;
	}

	/**
	 * 通知客户端抽奖
	 */
	public void notifyLottery(ClubMemberModel member, int lotteryCost) {
		ClubWelfareLotteryNotify.Builder b = ClubWelfareLotteryNotify.newBuilder();
		b.setAccountId(member.getAccount_id());
		b.setPlayerClubWelfare(member.getClubWelfare());
		b.setCostClubWelfare(lotteryCost);
		Utils.sendClient(member.getAccount_id(), S2CCmd.CLUB_WELFARE_LOTTERY_NOTIFY, b);
	}

	public ClubWelfareRewardProto toRewardBuilder(boolean isRealReward, ClubWelfareRewardModel model) {
		ClubWelfareRewardProto.Builder b = ClubWelfareRewardProto.newBuilder();
		b.setId(model.getId());
		b.setRewardNum(model.getAwardNum());
		b.setItemId(model.getAwardId());
		b.setItemName(ItemDict.getInstance().getNameByItemId(model.getAwardId()));
		if (!Strings.isNullOrEmpty(model.getAwardIcon())) {
			b.setRewardIcon(model.getAwardIcon());
		}
		if (!Strings.isNullOrEmpty(model.getAwardDesc())) {
			b.setRewardDesc(model.getAwardDesc());
		}
		b.setIsRealReward(isRealReward);
		return b.build();
	}

	/**
	 * 检查是否可开启亲友圈福卡功能
	 */
	public void checkOpenClubWelfare() {
		if (isOpenClubWelfare()) {
			return;
		}

		ClubWelfareSwitchModel clubWelfareSwitchModel = ClubWelfareSwitchModelDict.getInstance().getClubWelfareSwitchModel();
		if (clubWelfareSwitchModel == null) {
			return;
		}
		boolean totalSwitch = clubWelfareSwitchModel.getTotalSwitch() == 1;
		boolean isConditionOpen = clubWelfareSwitchModel.getIsConditionOpen() == 1;
		boolean hasMemCountCond = clubWelfareSwitchModel.getHasMemCountCond() == 1;
		int memCount = clubWelfareSwitchModel.getMemCount();
		boolean hasGameCountCond = clubWelfareSwitchModel.getHasGameCountCond() == 1;
		int gameCount = clubWelfareSwitchModel.getGameCount();
		if (!totalSwitch) {
			return;
		}
		if (isConditionOpen) {
			if (hasMemCountCond) {
				if (club.getMemberCount() <= memCount) {
					return;
				}
			}
			if (hasGameCountCond) {
				if (club.clubModel.getGameCount() <= gameCount) {
					return;
				}
			}
			openClubWelfare();
		}
	}

	/**
	 * 亲友圈福卡每日领取次数重置
	 */
	public void clubWelfareDailyReset() {
		if (isOpenClubWelfare()) {
			club.clubModel.setClubWelfareGetCount(0);
		}
	}
}
