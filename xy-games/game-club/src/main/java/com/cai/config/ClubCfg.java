/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cai.service.ClubService;
import com.google.common.collect.Sets;

/**
 * @author wu_hc date: 2017年11月7日 下午2:34:03 <br/>
 */
public final class ClubCfg {

	private static final ClubCfg cfg = new ClubCfg();

	public static ClubCfg get() {
		return cfg;
	}

	/**
	 * 个人俱乐部上限
	 */
	private volatile int ownerClubMax;

	/**
	 * 单个俱乐部玩法上限
	 */
	private volatile int clubRuleMax;

	/**
	 * 单个玩法桌子上限
	 */
	private volatile int ruleTableMax;

	/**
	 * 俱乐部人数上限
	 */
	private volatile int clubMemberMax;

	/**
	 * 关闭开房/进入房间，考虑用于临时更新俱乐部服时用
	 */
	private volatile boolean open;

	/**
	 * 管理员上限str1[x:]
	 */
	private volatile int managerMax = 2;

	/**
	 * 是否理解更新创建者的房卡更新str1[*:x]
	 */
	private volatile boolean syncGoldUpdateImmediate = false;

	/**
	 * 是否检测桌子位置str1[*:*:x]
	 */
	private volatile boolean checkSeat = true;

	/**
	 * 活动是否开启
	 */
	private volatile boolean activityOpen = true;

	/**
	 * 俱乐部活动最长时间(单位小时)
	 */
	private volatile int activityMaxTime = 7 * 24;

	/**
	 * 俱乐部活动最短时间(单位小时)
	 */
	private volatile int activityMinTime = 1;

	/**
	 * 历史纪录
	 */
	private volatile int showHistoryTime = 7 * 24;

	/**
	 * 进行中的俱乐部活动数量上限
	 */
	private volatile int activityLimit = 50;

	/**
	 * 隐藏桌子设置时，展示多少个桌子给客户端
	 */
	private volatile int tableCountWhenHideSetting = 8;

	/**
	 * 游戏玩家有修改，需要从逻辑服同步最新消息的子游戏id
	 */
	private volatile Set<Integer> ruleUpdateSubGameIds = Sets.newHashSet();

	/**
	 * 使用新获取俱乐部信息方式-@see {@link ClubService#getMyClub(long)} &
	 * {@link ClubService#getMyClubNew(long)}
	 */
	private volatile boolean useNewGetClubWay = true;

	/**
	 * 俱乐部事件日志是否落地
	 */
	private volatile boolean saveClubEventDB = true;

	/**
	 * 是否独立线程同步房间状态
	 */
	private volatile boolean useOwnThreadSyncRoomStatus = false;

	/**
	 * 忽略邀请时常(单位：ms)
	 */
	private volatile int ignoreInviteTime = 5 * 60 * 1000;

	/**
	 * 找不到roomredismodel时清除ClubRoomRedisModel redis
	 */
	private volatile boolean delRedisCache = false;

	/**
	 * 房间状态同步时是否需要校验房间id是否相同
	 */
	private boolean checkRoomId = true;

	/**
	 * 使用新获取俱乐部成员记录方式
	 */
	private boolean useNewGetClubMemRecordWay = false;

	/**
	 * 是否能删除已经开局的房间
	 */
	private boolean canDelStartedRoom = true;

	/**
	 * 题诗语
	 */
	private String tip = null;

	/**
	 *
	 */
	private String delRedisCacheTip = "牌桌清理中，请稍候再试!";

	/**
	 * 是否使用备用线程
	 */
	private boolean useReserveWorker = false;

	/**
	 * 频率限制
	 */
	private boolean accessLimit = true;

	/**
	 * 下线游戏
	 */
	private Set<Integer> offlineGames = Sets.newHashSet();

	/**
	 *
	 */
	private String offlineGameTip = "该游戏已经下线，详情请咨询客服!";

	private boolean useOldRecordSaveWay = false;

	private boolean useOldTireWay = false;

	private boolean useOldRecordInsertWay = false;

	private boolean isBanChat = false;

	private boolean isBanBulletin = false;

	private boolean isBanMarquee = false;

	private boolean defendCheating = false;

	/**
	 * 俱乐部最小设置开赛时间(分钟)
	 */
	private int clubMatchMinStartMinute = 3;

	/**
	 * 俱乐部开赛前通知时间(分钟)
	 */
	private int clubMatchWillStartMinute = 10;

	/**
	 * 自建赛自主报名时间限制(分钟)
	 */
	private int clubMatchEnrollTimeLimit = 10;

	/**
	 * 自建赛管理员设置参赛时间限制(分钟)
	 */
	private int clubMatchSetEnrollTimeLimit = 20;

	private boolean useNewClubRuleRecordGetWay = false;

	/**
	 * 亲友圈自建赛逻辑服列表
	 */
	private String clubMatchLogicIndexs;

	private List<Integer> clubMatchLogicList = new ArrayList<>();

	/**
	 * 多玩法模式下，多长时间内玩家无准备则踢出桌子[单位S]
	 */
	private int autoKickoutPlayerTime = 60;

	/**
	 * 多玩法模式下，被自动踢出的玩家多久时间内禁止再进入桌子[单位S]
	 */
	private int playerEnterTableBanTime = 15;

	/**
	 * 亲友圈福卡抽奖自动抽取的时间[单位S]
	 */
	private int clubWelfareAotoLotteryTime = 10;

	/**
	 * 亲友圈福卡每日限制领取次数
	 */
	private int clubWelfareDailyGetCount = 3;

	public int getOwnerClubMax() {
		return ownerClubMax;
	}

	public ClubCfg setOwnerClubMax(int ownerClubMax) {
		this.ownerClubMax = ownerClubMax;
		return this;
	}

	public int getClubRuleMax() {
		return clubRuleMax;
	}

	public ClubCfg setClubRuleMax(int clubRuleMax) {
		this.clubRuleMax = clubRuleMax;
		return this;
	}

	public int getRuleTableMax() {
		return ruleTableMax;
	}

	public ClubCfg setRuleTableMax(int ruleTableMax) {
		this.ruleTableMax = ruleTableMax;
		return this;
	}

	public int getClubMemberMax() {
		return clubMemberMax;
	}

	public ClubCfg setClubMemberMax(int clubMemberMax) {
		this.clubMemberMax = clubMemberMax;
		return this;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public int getManagerMax() {
		return managerMax;
	}

	public ClubCfg setManagerMax(int managerMax) {
		this.managerMax = managerMax;
		return this;
	}

	public boolean isSyncGoldUpdateImmediate() {
		return syncGoldUpdateImmediate;
	}

	public ClubCfg setSyncGoldUpdateImmediate(boolean syncGoldUpdateImmediate) {
		this.syncGoldUpdateImmediate = syncGoldUpdateImmediate;
		return this;
	}

	public boolean isCheckSeat() {
		return checkSeat;
	}

	public ClubCfg setCheckSeat(boolean checkSeat) {
		this.checkSeat = checkSeat;
		return this;
	}

	public boolean isActivityOpen() {
		return activityOpen;
	}

	public ClubCfg setActivityOpen(boolean activityOpen) {
		this.activityOpen = activityOpen;
		return this;
	}

	public int getActivityMaxTime() {
		return activityMaxTime;
	}

	public ClubCfg setActivityMaxTime(int activityMaxTime) {
		this.activityMaxTime = activityMaxTime;
		return this;
	}

	public int getActivityMinTime() {
		return activityMinTime;
	}

	public ClubCfg setActivityMinTime(int activityMinTime) {
		this.activityMinTime = activityMinTime;
		return this;
	}

	public int getActivityLimit() {
		return activityLimit;
	}

	public ClubCfg setActivityLimit(int activityLimit) {
		this.activityLimit = activityLimit;
		return this;
	}

	public int getTableCountWhenHideSetting() {
		return tableCountWhenHideSetting;
	}

	public ClubCfg setTableCountWhenHideSetting(int tableCountWhenHideSetting) {
		this.tableCountWhenHideSetting = tableCountWhenHideSetting;
		return this;
	}

	public Set<Integer> getRuleUpdateSubGameIds() {
		return ruleUpdateSubGameIds;
	}

	public ClubCfg setRuleUpdateSubGameIds(Set<Integer> ruleUpdateSubGameIds) {
		this.ruleUpdateSubGameIds = ruleUpdateSubGameIds;
		return this;
	}

	public int getShowHistoryTime() {
		return showHistoryTime;
	}

	public void setShowHistoryTime(int showHistoryTime) {
		this.showHistoryTime = showHistoryTime;
	}

	public boolean isUseNewGetClubWay() {
		return useNewGetClubWay;
	}

	public void setUseNewGetClubWay(boolean useNewGetClubWay) {
		this.useNewGetClubWay = useNewGetClubWay;
	}

	public boolean isSaveClubEventDB() {
		return saveClubEventDB;
	}

	public void setSaveClubEventDB(boolean saveClubEventDB) {
		this.saveClubEventDB = saveClubEventDB;
	}

	public boolean isUseOwnThreadSyncRoomStatus() {
		return useOwnThreadSyncRoomStatus;
	}

	public void setUseOwnThreadSyncRoomStatus(boolean useOwnThreadSyncRoomStatus) {
		this.useOwnThreadSyncRoomStatus = useOwnThreadSyncRoomStatus;
	}

	public int getIgnoreInviteTime() {
		return ignoreInviteTime;
	}

	public void setIgnoreInviteTime(int ignoreInviteTime) {
		this.ignoreInviteTime = ignoreInviteTime;
	}

	public boolean isDelRedisCache() {
		return delRedisCache;
	}

	public void setDelRedisCache(boolean delRedisCache) {
		this.delRedisCache = delRedisCache;
	}

	public String getDelRedisCacheTip() {
		return delRedisCacheTip;
	}

	public void setDelRedisCacheTip(String delRedisCacheTip) {
		this.delRedisCacheTip = delRedisCacheTip;
	}

	public boolean isCheckRoomId() {
		return checkRoomId;
	}

	public void setCheckRoomId(boolean checkRoomId) {
		this.checkRoomId = checkRoomId;
	}

	public boolean isUseNewGetClubMemRecordWay() {
		return useNewGetClubMemRecordWay;
	}

	public void setUseNewGetClubMemRecordWay(boolean useNewGetClubMemRecordWay) {
		this.useNewGetClubMemRecordWay = useNewGetClubMemRecordWay;
	}

	public boolean isCanDelStartedRoom() {
		return canDelStartedRoom;
	}

	public void setCanDelStartedRoom(boolean canDelStartedRoom) {
		this.canDelStartedRoom = canDelStartedRoom;
	}

	public String getTip() {
		return tip;
	}

	public void setTip(String tip) {
		this.tip = tip;
	}

	public boolean isUseReserveWorker() {
		return useReserveWorker;
	}

	public void setUseReserveWorker(boolean useReserveWorker) {
		this.useReserveWorker = useReserveWorker;
	}

	public boolean isAccessLimit() {
		return accessLimit;
	}

	public void setAccessLimit(boolean accessLimit) {
		this.accessLimit = accessLimit;
	}

	public Set<Integer> getOfflineGames() {
		return offlineGames;
	}

	public void setOfflineGames(Set<Integer> offlineGames) {
		this.offlineGames = offlineGames;
	}

	public String getOfflineGameTip() {
		return offlineGameTip;
	}

	public void setOfflineGameTip(String offlineGameTip) {
		this.offlineGameTip = offlineGameTip;
	}

	public boolean isUseOldRecordSaveWay() {
		return useOldRecordSaveWay;
	}

	public void setUseOldRecordSaveWay(boolean useOldRecordSaveWay) {
		this.useOldRecordSaveWay = useOldRecordSaveWay;
	}

	public boolean isUseOldTireWay() {
		return useOldTireWay;
	}

	public void setUseOldTireWay(boolean useOldTireWay) {
		this.useOldTireWay = useOldTireWay;
	}

	public boolean isUseOldRecordInsertWay() {
		return useOldRecordInsertWay;
	}

	public void setUseOldRecordInsertWay(boolean useOldRecordInsertWay) {
		this.useOldRecordInsertWay = useOldRecordInsertWay;
	}

	public boolean isBanChat() {
		return isBanChat;
	}

	public void setBanChat(boolean isBanChat) {
		this.isBanChat = isBanChat;
	}

	public boolean isBanBulletin() {
		return isBanBulletin;
	}

	public void setBanBulletin(boolean isBanBulletin) {
		this.isBanBulletin = isBanBulletin;
	}

	public boolean isBanMarquee() {
		return isBanMarquee;
	}

	public void setBanMarquee(boolean isBanMarquee) {
		this.isBanMarquee = isBanMarquee;
	}

	public int getClubMatchMinStartMinute() {
		return clubMatchMinStartMinute;
	}

	public void setClubMatchMinStartMinute(int clubMatchMinStartMinute) {
		this.clubMatchMinStartMinute = clubMatchMinStartMinute;
	}

	public int getClubMatchWillStartMinute() {
		return clubMatchWillStartMinute;
	}

	public void setClubMatchWillStartMinute(int clubMatchWillStartMinute) {
		this.clubMatchWillStartMinute = clubMatchWillStartMinute;
	}

	public boolean isUseNewClubRuleRecordGetWay() {
		return useNewClubRuleRecordGetWay;
	}

	public void setUseNewClubRuleRecordGetWay(boolean useNewClubRuleRecordGetWay) {
		this.useNewClubRuleRecordGetWay = useNewClubRuleRecordGetWay;
	}

	public void setClubMatchLogicIndexs(String clubMatchLogicIndexs) {
		this.clubMatchLogicIndexs = clubMatchLogicIndexs;
	}

	public List<Integer> getClubMatchLogicList() {
		return clubMatchLogicList;
	}

	public void setClubMatchLogicList(List<Integer> clubMatchLogicList) {
		this.clubMatchLogicList = clubMatchLogicList;
	}

	public boolean isDefendCheating() {
		return defendCheating;
	}

	public void setDefendCheating(boolean defendCheating) {
		this.defendCheating = defendCheating;
	}

	public int getClubMatchEnrollTimeLimit() {
		return clubMatchEnrollTimeLimit;
	}

	public void setClubMatchEnrollTimeLimit(int clubMatchEnrollTimeLimit) {
		this.clubMatchEnrollTimeLimit = clubMatchEnrollTimeLimit;
	}

	public int getClubMatchSetEnrollTimeLimit() {
		return clubMatchSetEnrollTimeLimit;
	}

	public void setClubMatchSetEnrollTimeLimit(int clubMatchSetEnrollTimeLimit) {
		this.clubMatchSetEnrollTimeLimit = clubMatchSetEnrollTimeLimit;
	}

	public int getAutoKickoutPlayerTime() {
		return autoKickoutPlayerTime;
	}

	public void setAutoKickoutPlayerTime(int autoKickoutPlayerTime) {
		this.autoKickoutPlayerTime = autoKickoutPlayerTime;
	}

	public int getPlayerEnterTableBanTime() {
		return playerEnterTableBanTime;
	}

	public void setPlayerEnterTableBanTime(int playerEnterTableBanTime) {
		this.playerEnterTableBanTime = playerEnterTableBanTime;
	}

	public int getClubWelfareAotoLotteryTime() {
		return clubWelfareAotoLotteryTime;
	}

	public void setClubWelfareAotoLotteryTime(int clubWelfareAotoLotteryTime) {
		this.clubWelfareAotoLotteryTime = clubWelfareAotoLotteryTime;
	}

	public int getClubWelfareDailyGetCount() {
		return clubWelfareDailyGetCount;
	}

	public void setClubWelfareDailyGetCount(int clubWelfareDailyGetCount) {
		this.clubWelfareDailyGetCount = clubWelfareDailyGetCount;
	}

	@Override
	public String toString() {
		return "ClubCfg [ownerClubMax=" + ownerClubMax + ", clubRuleMax=" + clubRuleMax + ", ruleTableMax=" + ruleTableMax + ", clubMemberMax="
				+ clubMemberMax + ", open=" + open + ", managerMax=" + managerMax + ", syncGoldUpdateImmediate=" + syncGoldUpdateImmediate
				+ ", checkSeat=" + checkSeat + ", activityOpen=" + activityOpen + ", activityMaxTime=" + activityMaxTime + ", activityMinTime="
				+ activityMinTime + ", showHistoryTime=" + showHistoryTime + ", activityLimit=" + activityLimit + ", tableCountWhenHideSetting="
				+ tableCountWhenHideSetting + ", ruleUpdateSubGameIds=" + ruleUpdateSubGameIds + ", useNewGetClubWay=" + useNewGetClubWay
				+ ", saveClubEventDB=" + saveClubEventDB + ", useOwnThreadSyncRoomStatus=" + useOwnThreadSyncRoomStatus + ", ignoreInviteTime="
				+ ignoreInviteTime + ", delRedisCache=" + delRedisCache + ", checkRoomId=" + checkRoomId + ", useNewGetClubMemRecordWay="
				+ useNewGetClubMemRecordWay + ", canDelStartedRoom=" + canDelStartedRoom + ", tip=" + tip + ", delRedisCacheTip=" + delRedisCacheTip
				+ ", useReserveWorker=" + useReserveWorker + ", accessLimit=" + accessLimit + ", offlineGames=" + offlineGames + ", offlineGameTip="
				+ offlineGameTip + ", useOldRecordSaveWay=" + useOldRecordSaveWay + ", useOldTireWay=" + useOldTireWay + ", useOldRecordInsertWay="
				+ useOldRecordInsertWay + ", isBanChat=" + isBanChat + ", isBanBulletin=" + isBanBulletin + ", isBanMarquee=" + isBanMarquee
				+ ", clubMatchMinStartMinute=" + clubMatchMinStartMinute + ", clubMatchWillStartMinute=" + clubMatchWillStartMinute
				+ ", useNewClubRuleRecordGetWay=" + useNewClubRuleRecordGetWay + ", clubMatchLogicIndexs=" + clubMatchLogicIndexs
				+ ", defendCheating=" + defendCheating + ", clubMatchEnrollTimeLimit=" + clubMatchEnrollTimeLimit + ", clubMatchSetEnrollTimeLimit="
				+ clubMatchSetEnrollTimeLimit + ", autoKickoutPlayerTime=" + autoKickoutPlayerTime + ", playerEnterTableBanTime="
				+ playerEnterTableBanTime + ", clubWelfareAotoLotteryTime=" + clubWelfareAotoLotteryTime + ", clubWelfareDailyGetCount="
				+ clubWelfareDailyGetCount + "]";
	}
}
