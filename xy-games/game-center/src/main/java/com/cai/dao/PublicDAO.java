package com.cai.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.EAccountParamType;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.AccountMobileModel;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountProxyModel;
import com.cai.common.domain.AccountPushModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountRedpacketPoolModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.ActivityMissionModel;
import com.cai.common.domain.ActivityModel;
import com.cai.common.domain.ActivityRedpacketPoolModel;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AgentRecommendModel;
import com.cai.common.domain.AppItem;
import com.cai.common.domain.AppShopModel;
import com.cai.common.domain.CardCategoryModel;
import com.cai.common.domain.ChannelModel;
import com.cai.common.domain.ClientUploadErWeiMaModel;
import com.cai.common.domain.ClubAccountModel;
import com.cai.common.domain.ClubExclusiveActivityModel;
import com.cai.common.domain.ClubExclusiveGoldModel;
import com.cai.common.domain.ClubGroupModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubModel;
import com.cai.common.domain.ClubRoomOverModel;
import com.cai.common.domain.ClubRoomOverModel.ClubRoomPlayerModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.CoinCornucopiaModel;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.domain.CoinGameServerModel;
import com.cai.common.domain.ContinueLoginModel;
import com.cai.common.domain.CustomerSerNoticeModel;
import com.cai.common.domain.FoundationGameServerModel;
import com.cai.common.domain.GameDescModel;
import com.cai.common.domain.GameGroupModel;
import com.cai.common.domain.GameGroupRuleModel;
import com.cai.common.domain.GameGroupSetModel;
import com.cai.common.domain.GameNoticeModel;
import com.cai.common.domain.GameRecommendIndexModel;
import com.cai.common.domain.GameResourceModel;
import com.cai.common.domain.GameTypeDBModel;
import com.cai.common.domain.GaoDeModel;
import com.cai.common.domain.GateServerModel;
import com.cai.common.domain.GoodsModel;
import com.cai.common.domain.HallGuideModel;
import com.cai.common.domain.HallMainViewBackModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.IPGroupModel;
import com.cai.common.domain.InviteActiveModel;
import com.cai.common.domain.InviteFriendsActivityModel;
import com.cai.common.domain.ItemExchangeModel;
import com.cai.common.domain.ItemModel;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.domain.LogicServerBalanceModule;
import com.cai.common.domain.LoginNoticeModel;
import com.cai.common.domain.MainUiNoticeModel;
import com.cai.common.domain.MatchGameServerModel;
import com.cai.common.domain.MoneyShopModel;
import com.cai.common.domain.OldUserModel;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.domain.PushInfoModel;
import com.cai.common.domain.RechargeModel;
import com.cai.common.domain.RecommendLimitModel;
import com.cai.common.domain.RecommendReceiveModel;
import com.cai.common.domain.RedActivityModel;
import com.cai.common.domain.RedPackageActivityModel;
import com.cai.common.domain.RoomGeneratorModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SpecialAccountModel;
import com.cai.common.domain.SysNoticeModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.TurntableActiveModel;
import com.cai.common.domain.TurntablePrizeModel;
import com.cai.common.domain.WelfareExchangeModel;
import com.cai.common.domain.WelfareGoodsTypeModel;
import com.cai.common.domain.activity.ActivityDaysMission;
import com.cai.common.domain.activity.ActivityMissionGroupModel;
import com.cai.common.domain.activity.NewActivityPrizeModel;
import com.cai.common.domain.bonuspoints.AccountBonusPointsModel;
import com.cai.common.domain.bonuspoints.BonusPointsActivity;
import com.cai.common.domain.bonuspoints.BonusPointsGoods;
import com.cai.common.domain.bonuspoints.BonusPointsGoodsType;
import com.cai.common.domain.bonuspoints.PlayerAddressModel;
import com.cai.common.domain.coin.CoinGame;
import com.cai.common.domain.coin.CoinGameDetail;
import com.cai.common.domain.coin.CoinGameType;
import com.cai.common.domain.coin.CoinRelief;
import com.cai.common.domain.info.CardSecretInfo;
import com.cai.common.domain.sdk.SdkApp;
import com.cai.common.domain.sdk.SdkDiamondShopModel;
import com.cai.common.domain.sdk.SdkShop;
import com.cai.common.domain.statistics.DailyCoinExchangeStatistics;
import com.cai.common.domain.zhuzhou.AccountPromoterReceiveModel;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.util.MyDateUtil;
import com.google.common.collect.Lists;
import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * public数据库dao
 * 
 * @author run
 *
 */
@Repository
@SuppressWarnings("rawtypes")
public class PublicDAO extends CoreDao {

	private final ThreadLocal<Map<String, Object>> local = new ThreadLocal<Map<String, Object>>() {
		@Override
		protected Map<String, Object> initialValue() {
			return new HashMap<>();
		}
	};

	@Autowired
	@Qualifier("dataSourcePublic")
	public void setDataSource2(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	@Autowired
	@Qualifier("sqlMapClient")
	public void setSqlMapClient2(SqlMapClient sqlMapClient) {
		super.setSqlMapClient(sqlMapClient);
		;
	}
	// ===============================================================================

	public List<ProxyGameServerModel> getProxyGameServerModelList() {
		return queryForList("getProxyGameServerModelList");
	}

	public List<LogicGameServerModel> getLogicGameServerModelList() {
		return queryForList("getLogicGameServerModelList");
	}

	public List<GateServerModel> getGateServerModelList() {
		return queryForList("getGateServerModelList");
	}

	public List<MatchGameServerModel> getMatchServerModelList() {
		return queryForList("getMatchServerModelList");
	}

	public List<CoinGameServerModel> getCoinServerModelList() {
		return queryForList("getCoinServerModelList");
	}

	public List<FoundationGameServerModel> getFoundationServerModelList() {
		return queryForList("getFoundationServerModelList");
	}

	public void insertAccountModel(AccountModel accoutModel) {
		insertObject("insertAccountModel", accoutModel);
	}

	public void updateAccountModel(AccountModel accoutModel) {
		updateObject("updateAccountModel", accoutModel);
	}

	public List<AccountModel> getAccountList() {
		return queryForList("getAccountModelList");
	}

	public AccountModel getAccountById(long account_id) {
		return (AccountModel) queryForObject("getAccountById", account_id);
	}

	public AccountModel getAccountByAccountName(String account_name) {
		return (AccountModel) queryForObject("getAccountByAccountName", account_name);
	}

	// 排行榜相关
	public List<AccountModel> getAccountByMoneyDesc(int offset, int count) {
		return queryForList("getAccountByMoneyDesc");
	}

	public List<AccountModel> getAccountByGoldDesc(int count) {
		return queryForList("getAccountByGoldDesc", count);
	}

	/**
	 * 零点重置调度,账号
	 */
	public void resetAccountZero() {
		updateObject("resetAccountZero");
	}

	/**
	 * 零点重置调度,账号属性列表
	 */
	public void resetTodayAccountParam() {

		List<Integer> list = Lists.newArrayList();

		for (EAccountParamType c : EAccountParamType.values()) {
			if (c.getType() == 1) {
				list.add(c.getId());
			}
		}
		if (list.size() > 0) {
			updateObject("resetTodayAccountParam", list);
		}
	}

	/** 查询指定时间段的注册账号数量 */
	public Integer getAccountCreateNumByTime(Date beginTime, Date endTime) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("beginTime", beginTime);
		params.put("endTime", endTime);
		return (Integer) queryForObject("getAccountCreateNumByTime", params);
	}

	/** 查询用户总数 */
	public Integer getAccountNum() {
		return (Integer) queryForObject("getAccountNum");
	}

	/** 查询指定时间段的活跃账号数量 */
	public Integer getAccountActiveOnlineNum(Date beginTime, Date endTime) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("beginTime", beginTime);
		params.put("endTime", endTime);
		return (Integer) queryForObject("getAccountActiveOnlineNum", params);
	}

	/**
	 * 指定某天有登录的
	 * 
	 * @param beginTime
	 * @param endTime
	 * @return
	 */
	public int getAccountActivieByCreateTime(Date beginTime, Date endTime, Date loginTime) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("beginTime", beginTime);
		params.put("endTime", endTime);
		params.put("loginTime", loginTime);
		return (Integer) queryForObject("getAccountActivieByCreateTime", params);
	}

	public void insertAccountParamModel(AccountParamModel accountParamModel) {
		logger.info(JSONObject.toJSON(accountParamModel));
		insertObject("insertAccountParamModel", accountParamModel);
	}

	public void updateAccountParamModel(AccountParamModel accountParamModel) {
		updateObject("updateAccountParamModel", accountParamModel);
	}

	public List<AccountParamModel> getAccountParamModelByAccountId(long account_id) {
		return queryForList("getAccountParamModelByAccountId", account_id);
	}

	/**
	 * 查看他的上下级关系
	 * 
	 * @param target_account_id
	 * @return
	 */
	public HallRecommendModel getHallRecommendModelByTargetAccountId(long target_account_id) {
		List<HallRecommendModel> list = queryForList("getHallRecommendModelListByTargetAccountId", target_account_id);
		return list.size() > 0 ? list.get(0) : new HallRecommendModel();
	}

	public void insertSysParamModel(SysParamModel sysParamModel) {
		insertObject("insertSysParamModel", sysParamModel);
	}

	public void updateSysParamModel(SysParamModel sysParamModel) {
		updateObject("updateSysParamModel", sysParamModel);
	}

	public List<SysParamModel> getSysParamModelList() {
		return queryForList("getSysParamModelList");
	}

	public List<SysParamModel> getSysParamServerModelList() {
		return queryForList("getSysParamServerModelList");
	}

	public AccountWeixinModel getAccountWeixinModelByAccountId(long account_id) {
		return (AccountWeixinModel) queryForObject("getaccountWeixinModelByAccountId", account_id);
	}

	public void insertAccountWeixinModel(AccountWeixinModel model) {
		insertObject("insertAccountWeixinModel", model);
	}

	public int updateAccountWeixinModel(AccountWeixinModel model) {
		return updateObject("updateAccountWeixinModel", model);
	}

	public AccountWeixinModel getAccountWeixinModelByUnionid(String unionid) {
		return (AccountWeixinModel) queryForObject("getAccountWeixinModelByUnionid", unionid);
	}

	public List<GameNoticeModel> getGameNoticeModelList() {
		return queryForList("getGameNoticeModelList", new Date());
	}

	public List<SysNoticeModel> getSysNoticeModelList() {
		return queryForList("getSysNoticeModelList");
	}

	public List<ShopModel> getValidShopModelList() {
		return queryForList("getValidShopModelList");
	}

	public List<AppShopModel> getValidAppShopModelList() {
		return queryForList("getValidAppShopModelList");
	}

	public List<GameDescModel> getGameDescModelList() {
		return queryForList("getGameDescModelList");
	}

	public List<MainUiNoticeModel> getMainUiNoticeModelList() {
		return queryForList("getMainUiNoticeModelList");
	}

	public List<CustomerSerNoticeModel> getCustomerSerNoticeModelList() {
		return queryForList("getCustomerSerNoticeModelList");
	}

	public List<LoginNoticeModel> getLoginNoticeModelList() {
		return queryForList("getLoginNoticeModelList");
	}

	public List<MoneyShopModel> getValidMoneyShopModelList() {
		return queryForList("getValidMoneyShopModelList");
	}

	public List<TurntableActiveModel> getTurntableActiveModelList() {
		return queryForList("turntableActiveList");
	}

	public List<TurntablePrizeModel> getTurntablePrizeModelList() {
		return queryForList("turntablePrizeList");
	}

	public List<ItemModel> getItemModelList() {
		return queryForList("getItemModelList");
	}

	public List<ActivityModel> getActivityModelList() {
		return queryForList("getActivityModelList");
	}

	public List<NewActivityPrizeModel> getActivityPrizeModelList() {
		return queryForList("getActivityPrizeModelList");
	}

	public List<NewActivityPrizeModel> getActivityMissionGroupPrizeModelList(int activityId) {
		return queryForList("getActivityMissionGroupPrizeModelList", activityId);
	}

	public List<RedPackageActivityModel> getRedPackageModelList() {
		return queryForList("getRedPackageModelList");
	}

	public List<GoodsModel> getValidGoodsModelList() {
		return queryForList("getValidGoodsModelList");
	}

	public List<IPGroupModel> getValidIPGroupModelList() {
		return queryForList("getValidIPGroupModelList");
	}

	public List<ContinueLoginModel> getContinueLoginModelList() {
		return queryForList("getContinueLoginModelList");
	}

	public List<AccountRecommendModel> getAccountRecommendModelListByAccountId(long account_id) {
		return queryForList("getAccountRecommendModelListByAccountId", account_id);
	}

	public List<HallRecommendModel> getHallRecommendModelListByAccountId(long account_id) {
		return queryForList("getHallRecommendModelListByAccountId", account_id);
	}

	public void insertAccountRecommendModel(AccountRecommendModel accountRecommendModel) {
		insertObject("insertAccountRecommendModel", accountRecommendModel);
	}

	public void insertAccountRecommendModel(HallRecommendModel hallRecommendModel) {
		insertObject("insertHallRecommendModel", hallRecommendModel);
	}

	public List<AccountGroupModel> getAccountGroupModelListByAccountId(long account_id) {
		return queryForList("getAccountGroupModelListByAccountId", account_id);
	}

	public void insertAccountGroupModel(AccountGroupModel accountGroupModel) {
		insertObject("insertAccountGroupModel", accountGroupModel);
	}

	public void deleteAccountGroupModel(AccountGroupModel accountGroupModel) {
		deleteObject("deleteAccountGroupModel", accountGroupModel);
	}

	public List<AccountGroupModel> getAccountGroupModelListByGroupId(String groupID) {
		return queryForList("getAccountGroupModelListByGroupId", groupID);
	}

	public List<AccountProxyModel> getAccountProxyModelListByAccountId(long account_id) {
		return queryForList("getAccountProxyModelListByAccountId", account_id);
	}

	public void insertAccountProxyModel(AccountProxyModel accountProxyModel) {
		insertObject("insertAccountProxyModel", accountProxyModel);
	}

	public void insertSysGameTypeDBModel(GameTypeDBModel gameTypeDBModel) {
		insertObject("insertGameTypeDBModel", gameTypeDBModel);
	}

	public List<GameTypeDBModel> getSysGameTypeDBModelList() {
		return queryForList("getGameTypeDBModelList");
	}

	public void deleteAccountProxyModel(AccountProxyModel accountProxyModel) {
		deleteObject("deleteAccountProxyModel", accountProxyModel);
	}

	public void updateAccountProxyModel(AccountProxyModel accountProxyModel) {
		updateObject("updateAccountProxyModel", accountProxyModel);
	}

	public void deleteAccountRecommendModel(AccountRecommendModel accountRecommendModel) {
		deleteObject("deleteAccountRecommendModel", accountRecommendModel);
	}

	public void deleteHallRecommendModel(HallRecommendModel hallRecommendModel) {
		deleteObject("deleteHallRecommendModel", hallRecommendModel);
	}

	public void deleteAgentRecommendModel(AgentRecommendModel agentRecommendModel) {
		deleteObject("deleteAgentRecommendModel", agentRecommendModel);
	}

	public List<HashMap> getProxyAccountByProxy() {
		return queryForList("getProxyAccountByProxy");
	}

	public List<Long> getProxyAccountByProxyInfo(Long account_id) {
		return queryForList("getProxyAccountByProxyInfo", account_id);
	}

	public List<AccountModel> getProxyAccountList() {
		return queryForList("getProxyAccountList");
	}

	public void updateAccountRecommendModel(AccountRecommendModel accountRecommendModel) {
		updateObject("updateAccountRecommendLevel", accountRecommendModel);
	}

	public void updateHallRecommendModel(HallRecommendModel hallRecommendModel) {
		updateObject("updateHallRecommendLevel", hallRecommendModel);
	}

	public void updateHallAccountId(HallRecommendModel hallRecommendModel) {
		updateObject("updateHallAccountId", hallRecommendModel);
	}

	public void updateHallTargetAccountId(HallRecommendModel hallRecommendModel) {
		updateObject("updateHallTargetAccountId", hallRecommendModel);
	}

	public List<Long> getRecommendAccountIdByDate(Date date) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("startTime", MyDateUtil.getZeroDate(date));
		params.put("endTime", MyDateUtil.getTomorrowZeroDate(date));
		return queryForList("getRecommendAccountIdListByCreateTime", params);
	}

	public List<AppItem> getAllAppItemList() {
		return queryForList("getAppItemList");
	}

	public List<AppItem> getAllAppItem() {
		return queryForList("getAllAppItem");
	}

	public List<AppItem> getAppItemListByAppId(int appId) {
		return queryForList("getAppItemListByAppId", appId);
	}

	public void insertAppItem(AppItem appItem) {
		insertObject("insertAppItem", appItem);
	}

	public void updateAppItem(AppItem appItem) {
		updateObject("updateAppItem", appItem);
	}

	public List<ClubModel> getClubList() {
		return queryForList("getClubList");
	}

	public int insertClub(ClubModel clubModel) {
		return (int) insertObject("insertClub", clubModel);
	}

	public void deleteClub(int club_id) {
		deleteObject("deleteClub", club_id);
	}

	public void updateClub(ClubModel clubModel) {
		updateObject("updateClub", clubModel);
	}

	public ClubAccountModel getClubAccount(ClubAccountModel clubModel) {
		return (ClubAccountModel) queryForObject("getClubAccount", clubModel);
	}

	public void insertClubAccount(ClubAccountModel clubAccountModel) {
		insertObject("insertClubAccount", clubAccountModel);
	}

	public boolean deleteClubAccount(ClubAccountModel clubAccountModel) {
		return deleteObject("deleteClubAccount", clubAccountModel) > 0;
	}

	public void deleteClubAllAccount(int club_id) {
		deleteObject("deleteClubAllAccount", club_id);
	}

	/**
	 * 包含未审核的
	 * 
	 * @param club_id
	 * @return
	 */
	public int getClubMemberCount(int club_id) {
		return (int) queryForObject("getClubAccountCount", club_id);
	}

	public int getClubMemberCountByStatus(int club_id, int status) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", club_id);
		map.put("status", status);

		return (int) queryForObject("getClubAccountCountByStatus", map);
	}

	public void updateClubAccount(ClubAccountModel clubAccountModel) {
		updateObject("updateClubAccount", clubAccountModel);
	}

	public void replaceAccountId(HallRecommendModel hallRecommendModel) {
		updateObject("replaceAccountId", hallRecommendModel);
	}

	public void replaceRecommendId(HallRecommendModel hallRecommendModel) {
		updateObject("replaceRecommendId", hallRecommendModel);
	}

	public void updateClubMember(ClubRoomOverModel roomOverModel) {
		if (roomOverModel.getPlayers().isEmpty()) {
			return;
		}
		StringBuilder buidler = new StringBuilder();

		for (ClubRoomPlayerModel player : roomOverModel.getPlayers()) {
			buidler.append(player.getAccountId()).append(",");
		}
		buidler.deleteCharAt(buidler.length() - 1);

		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", roomOverModel.getClubId());
		map.put("accountIds", buidler.toString());
		updateObject("updateClubAccountGameCount", map);
	}

	public int agreeEnterClubAccountBatch(int clubId, String accountIds) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("accountIds", accountIds);
		return updateObject("agreeEnterClubAccountBatch", map);
	}

	public int deleteClubAllAccountBatch(int clubId, String accountIds) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("accountIds", accountIds);
		int count = deleteObject("deleteClubAllAccountBatch", map);
		return count;
	}

	public List<ClubMemberModel> getClubMemberByPage(int start, int end, int clubId) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("start", start);
		map.put("end", end);

		return queryForList("getClubMemberByPage", map);
	}

	public List<ClubMemberModel> getClubMemberByPageAndStatus(int start, int end, int clubId, int status) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("start", start);
		map.put("end", end);
		map.put("status", status);

		return queryForList("getClubMemberByPageAndStatus", map);
	}

	public List<ClubMemberModel> searchClubMember(int clubId, long accountId) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("accountId", accountId);
		return queryForList("searchClubMember", map);
	}

	public ClubGroupModel getClubGroup(String group_id) {
		return (ClubGroupModel) queryForObject("getClubGroup", group_id);
	}

	public List<String> getClubGroup(int club_id) {
		return queryForList("getGroupByClubId", club_id);
	}

	public void insertClubGroup(ClubGroupModel model) {
		insertObject("insertClubGroup", model);
	}

	public void deleteClubGroup(ClubGroupModel model) {
		deleteObject("deleteClubGroup", model);
	}

	public void deleteClubAllGroup(int club_id) {
		deleteObject("deleteClubAllGroup", club_id);
	}

	public List<ClubRuleModel> getClubRule(int club_id) {
		List<ClubRuleModel> list = queryForList("getRuleByClubId", club_id);

		return list;
	}

	public ClubModel getClub(int club_id) {
		return (ClubModel) queryForObject("getClub", club_id);
	}

	public void updateClubRule(ClubRuleModel clubRule) {
		// clubRule.encodeRule();
		// updateObject("updateClubRule", clubRule);
	}

	public void insertClubRule(List<ClubRuleModel> clubRule) {
		// for (ClubRuleModel clubRuleModel : clubRule) {
		// clubRuleModel.encodeRule();
		// int id= (int) insertObject("insertClubRule", clubRuleModel);
		// clubRuleModel.setId(id);
		// }
	}

	public void deleteClubRule(int club_id) {
		deleteObject("deleteClubRule", club_id);
	}

	public List<AgentRecommendModel> getAgentRecommendModelListByAccountId(long account_id) {
		return queryForList("getAgentRecommendModelListByAccountId", account_id);
	}

	public void insertAgentRecommendModel(AgentRecommendModel agentRecommendModel) {
		insertObject("insertAgentRecommendModel", agentRecommendModel);
	}

	public void updateAccountRecommendModel(AgentRecommendModel agentRecommendModel) {
		updateObject("updateAgentRecommendLevel", agentRecommendModel);
	}

	/// ------------ 城市推荐位相关 --------------------
	public List<GameRecommendIndexModel> getGameRecommendModelList() {
		return queryForList("getGameRecommendModelList");
	}

	/// -------------指定app运行逻辑服相关 --------------------
	public List<LogicServerBalanceModule> getServerBalanceModelList() {
		return queryForList("getServerBalanceModelList");
	}

	/**
	 * 跑得快旧数据
	 * 
	 * @param club_id
	 * @return
	 */
	public List<OldUserModel> getPkdUserModel() {
		return queryForList("getPDKOldData");
	}

	public List<ClubMemberModel> getClubMembers(int club_id) {
		return queryForList("getClubMembers", club_id);
	}

	public void savePdkUser(OldUserModel m) {
		// TODO Auto-generated method stub
		insertObject("insertPDKOldData", m);
	}

	public List<GameGroupModel> getGameGroup() {
		return queryForList("getGameGroupModelList");
	}

	public List<GameGroupRuleModel> getGameGroupRule(int group_id) {
		return queryForList("getGameGroupRuleModelList", group_id);
	}

	public RedActivityModel getRedActivityModelByAccountId(long account_id) {
		List<RedActivityModel> list = queryForList("getRedActivityModelList", account_id);
		return list.size() > 0 ? list.get(0) : new RedActivityModel();
	}

	public void insertRedActivityModel(RedActivityModel redActivityModel) {
		insertObject("insertRedActivityModel", redActivityModel);
	}

	public void updateRedActivityModel(RedActivityModel redActivityModel) {
		updateObject("updateRedActivityModel", redActivityModel);
	}

	public List<GameGroupSetModel> getGameGroupSetList() {
		return queryForList("getGameGroupSet");
	}

	// 获取线下参赛群列表
	public List<ItemModel> getOfflineItemModelList() {
		return queryForList("getOfflineItemModelList");
	}

	public List<HashMap<String, Integer>> getMatchMaxSeq() {
		return queryForList("getMaxSeqMatchSignUpList");
	}

	/**
	 * 帐号手机绑定关系
	 * 
	 * @param model
	 */
	public List<AccountMobileModel> getAccountMobileList() {
		return queryForList("getAccountMobileList");
	}

	public int getNewClubPersonNum(Date date) {
		long count = 0;
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("startTime", MyDateUtil.getZeroDate(date));
			params.put("endTime", MyDateUtil.getTomorrowZeroDate(date));
			count = (long) queryForObject("getNewClubPersonNum", params);
			return (int) count;
		} catch (Exception e) {
			return 0;
		}
	}

	public int getNewClubNum(Date date) {
		long count = 0;
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("startTime", MyDateUtil.getZeroDate(date));
			params.put("endTime", MyDateUtil.getTomorrowZeroDate(date));
			count = (long) queryForObject("getNewClubNum", params);
			return (int) count;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public List getClubAccountIdNotInGroup(String groupId, int clubId) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("groupId", groupId);
		map.put("clubId", clubId);
		return queryForList("getClubAccountIdNotInGroup", map);

	}

	public List getMatchBroadModelList(Date date) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("endTime", date);
		return queryForList("getMatchBroadModelList", map);

	}

	public List<ClubExclusiveGoldModel> getClubExclusiveGoldModelList() {
		return queryForList("getExclusiveGoldModelList");
	}

	public void insertExclusiveGoldModel(ClubExclusiveGoldModel model) {
		insertObject("insertExclusiveGoldModel", model);
	}

	public List<ClubExclusiveActivityModel> getClubExclusiveActivityModelList() {
		return queryForList("getClubExclusiveActivityModelList");
	}

	public Object insertAccountPushModel(AccountPushModel accountPushModel) {
		return insertObject("insertAccountPushModel", accountPushModel);
	}

	public Object updateAccountPushModel(AccountPushModel accountPushModel) {
		return updateObject("updateAccountPushModel", accountPushModel);
	}

	public List<AccountPushModel> getAccountPushModelList() {
		return queryForList("getAccountPushModelList");
	}

	// 调整收益，一次性提现
	public void updateAccountIncome(AccountModel accountModel) {
		updateObject("updateAccountIncome", accountModel);
	}

	public List<SpecialAccountModel> getSpecialAccountModelList() {
		return queryForList("getSpecialAccountModelList");
	}

	public List<InviteActiveModel> getInviteActiveModelList() {
		return queryForList("getInviteActiveModelList");
	}

	public List<HashMap> getGoldAndMoneyRemain() {
		return queryForList("getGoldAndMoneyRemain");
	}

	public void updateAppItemZeroFlag(int appId) {
		updateObject("updateAppItemZeroFlag", appId);
	}

	public void updateAppItemOnline() {
		updateObject("updateAppItemOnline");
	}

	public void updateReloadControlModel(int state) {
		updateObject("updateReloadControlModel", state);
	}

	public List<HashMap> getOpenAgentRankByDate(long account_id, String begin, String end) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("begin", begin);
		map.put("end", end);
		map.put("account_id", account_id);
		return queryForList("getOpenAgentRankByDate", map);
	}

	public List<RecommendLimitModel> getRecommendLimitModelList() {
		return queryForList("getRecommendLimitModelList");
	}

	public List<CoinGameType> getCoinGameTypeList() {
		return queryForList("coinGameTypeList");
	}

	public List<CoinGame> getCoinGameTypeIndexList() {
		return queryForList("coinGameTypeIndexList");
	}

	public List<CoinGameDetail> getCoinGameTypeDetailList() {
		return queryForList("coinGameTypeDetailList");
	}

	public List<CoinRelief> getCoinReliefList() {
		return queryForList("coinReliefList");
	}

	public List<ItemExchangeModel> getItemExchangeModelList() {
		return queryForList("getItemExchangeModelList");
	}

	public List<WelfareExchangeModel> getWelfareExchangeModelList() {
		return queryForList("getWelfareExchangeModelList");
	}

	public List<CardSecretInfo> getCardSecretInfoList() {
		return queryForList("getCardSecretInfoList");
	}

	public List<GaoDeModel> getGaoDeCityCodeModelList() {
		return queryForList("getGaoDeCityCodeModelList");
	}

	public List<ActivityMissionModel> getActivityMissionModelList() {
		return queryForList("getActivityMissionModelList");
	}

	public List<ActivityMissionGroupModel> getActivityMissionGroupModelList() {
		return queryForList("getActivityMissionGroupModelList");
	}

	public List<ChannelModel> getChannelDictModelList() {
		return queryForList("getChannelDictModelList");
	}

	public List<RecommendReceiveModel> getRecommenderReceiveList() {
		return queryForList("getRecommenderReceiveList");
	}

	public void updateRecommendReceiveModel(RecommendReceiveModel model) {
		insertObject("updateRecommendReceiveModel", model);
	}

	public void insertRecommendReceiveModel(RecommendReceiveModel model) {
		updateObject("insertRecommendReceiveModel", model);
	}

	public List<AccountRedpacketPoolModel> getAccountRedpacketPoolModelList() {
		return queryForList("getAccountRedpacketPoolModelList");
	}

	public void updateAccountRedpacketPoolModel(AccountRedpacketPoolModel model) {
		insertObject("updateAccountRedpacketPoolModel", model);
	}

	public void insertAccountRedpacketPoolModel(AccountRedpacketPoolModel model) {
		updateObject("insertAccountRedpacketPoolModel", model);
	}

	public List<ActivityRedpacketPoolModel> getActivityRedpacketPoolModelList() {
		return queryForList("getActivityRedpacketPoolModelList");
	}

	public List<PushInfoModel> getPushInfoModelList() {
		return queryForList("getPushInfoModelList");
	}

	public List<WelfareGoodsTypeModel> getWelfareGoodsTypeModelList() {
		return queryForList("getWelfareGoodsTypeList");
	}

	/**
	 * 查询指定时间段指定id的留存数量
	 * 
	 * @param beginTime
	 * @param endTime
	 * @return
	 */
	public Integer getTVActiveAccountNum(Date beginTime, Date endTime, Date loginTime, String ids) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("beginTime", beginTime);
		params.put("endTime", endTime);
		params.put("loginTime", loginTime);
		params.put("ids", ids);
		return (Integer) queryForObject("getTVActiveAccountNum", params);
	}

	public void insertDailyCoinExchangeStatistics(DailyCoinExchangeStatistics model) {
		this.insertObject("insertDailyCoinExchangeStatistics", model);
	}

	/**
	 * 调用存储过程，统计昨日在线时长 返回平均值，存储过程已经对每个时段的在线时长做了统计并插入到statistis_daily_online表中
	 * 
	 * @param beginTime
	 * @return
	 */
	public Long callProcedureDailyOnlineTime(Date beginTime) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("loginTime", beginTime);
		return (Long) queryForObject("callProcedureDailyOnlineTime", params);
	}

	/**
	 * 调用存储过程，统计金币和闲逸豆区间 已经插入到统计表中
	 */
	public void callProcedureDailyCurrency() {
		queryForObject("proc_currency_stats");
	}

	/**
	 * 查询指定id的今日活跃账号数量
	 * 
	 * @param loginTime
	 * @param ids
	 * @return
	 */
	public Integer getTVTodayActiveAccountNum(Date loginTime, String ids) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("loginTime", loginTime);
		params.put("ids", ids);
		return (Integer) queryForObject("getTVTodayActiveAccountNum", params);
	}

	public List<HallGuideModel> getHallGuideModelList() {
		return queryForList("getHallGuideModelList");
	}

	public void updateAccountBonusPointsModel(AccountBonusPointsModel model) {
		updateObject("updateAccountBonusPointsModel", model);
	}

	public void insertAccountBonusPointsModel(AccountBonusPointsModel model) {
		insertObject("insertAccountBonusPointsModel", model);
	}

	public void updateupdateBonusPointsGoodsModel(BonusPointsGoods model) {
		updateObject("updateBonusPointsGoodsModel", model);
	}

	public void insertPlayerAddressModel(PlayerAddressModel model) {
		insertObject("insertPlayerAddressModel", model);
	}

	public void updatePlayerAddressModel(PlayerAddressModel model) {
		updateObject("updatePlayerAddressModel", model);
	}

	public List<BonusPointsGoodsType> getBonusPointsGoodsTypeList() {
		return queryForList("getBonusPointsGoodsTypeList");
	}

	public List<PlayerAddressModel> getPlayerAddressModelList() {
		return queryForList("getPlayerAddressModelList");
	}

	public List<BonusPointsActivity> getBonusPointsActivityList() {
		return queryForList("getBonusPointsActivityList");
	}

	public List<BonusPointsGoods> getBonusPointsGoodsList() {
		return queryForList("getBonusPointsGoodsList");
	}

	public List<BonusPointsGoods> getAllBonusPointsGoodsList() {
		return queryForList("getAllBonusPointsGoodsList");
	}

	public List<AccountBonusPointsModel> getAccountBonusPointsModelList() {
		return queryForList("getAccountBonusPointsModelList");
	}

	public void deleteStatistisRechargeTemp() {
		this.deleteObject("deleteStatistisRechargeTemp");
	}

	public void batchInsertAddCard(List<AddCardLog> addCardLogList) {
		this.insertObject("insertAddcardLogBatch", addCardLogList);
	}

	public List<CoinExciteModel> getCoinExciteModelList() {
		return queryForList("getCoinExciteModelList");
	}

	public List<CardCategoryModel> getCardCategoryModelList() {
		return queryForList("getCardCategoryModelList");
	}

	public void insertAddCard(AddCardLog addCardLog) {
		RechargeModel rechargeModel = new RechargeModel();
		rechargeModel.setAccount_id(addCardLog.getAccountId());
		rechargeModel.setCreate_time(addCardLog.getCreate_time());
		rechargeModel.setFinish_date(addCardLog.getFinishDate());
		rechargeModel.setNick_name(StringUtils.isBlank(addCardLog.getNickname()) ? "" : addCardLog.getNickname());
		rechargeModel.setOrder_id(addCardLog.getOrderID());
		rechargeModel.setRecharge_gold(addCardLog.getCardNum());
		rechargeModel.setRmb(addCardLog.getRmb());
		rechargeModel.setSell_type(addCardLog.getSellType());
		rechargeModel.setSend_gold(addCardLog.getSendNum());
		rechargeModel.setShop_id(addCardLog.getShopId());
		this.insertObject("insertAddcardLog", rechargeModel);
	}

	public void callProcedureDailyRechargeStats() {
		queryForObject("callProcedureDailyRechargeStats");
	}

	public List<GameResourceModel> getGameResourceModelList() {
		return queryForList("getGameResourceModelList");
	}

	public void insertClientUploadErWeiMaModel(ClientUploadErWeiMaModel model) {
		insertObject("insertClientUploadErWeiMaModel", model);
	}

	public void updateClientUploadErWeiMaModel(ClientUploadErWeiMaModel model) {
		updateObject("updateClientUploadErWeiMaModel", model);
	}

	public List<ClientUploadErWeiMaModel> getClientUploadErWeiMaModelList() {
		return queryForList("getClientUploadErWeiMaModelList");
	}

	public List<ActivityDaysMission> getActivityDaysMissionList() {
		return queryForList("getActivityDaysMissionList");
	}

	public List<NewActivityPrizeModel> getEveryDayMissionPrizeModelList(int activityId) {
		return queryForList("getEveryDayMissionPrizeModelList", activityId);
	}

	public List<InviteFriendsActivityModel> getInviteFriendsActivityModelList() {
		return queryForList("getInviteFriendsActivityModelList");
	}

	public List<HallMainViewBackModel> getHallMainViewBackModelList() {
		return queryForList("getHallMainViewBackModelList");
	}

	public List<SdkApp> getAllSdkAppList() {
		return queryForList("getAllSdkApp");
	}

	public List<SdkShop> getAllSdkAppShopList() {
		return queryForList("getAllSdkAppShop");
	}

	public List<SdkDiamondShopModel> getAllSdkDiamondShopList() {
		return queryForList("getAllSdkDiamondShop");
	}

	public List<CoinCornucopiaModel> getCoinCornucopiaModel() {
		return queryForList("getCoinCornucopiaModel");
	}

	public List<AccountZZPromoterModel> getAccountZZPromoterModel() {
		return queryForList("getAccountZZPromoterModel");
	}

	public List<AccountZZPromoterModel> getAccountZZPromoterModelList(long account_id, Integer pageNumber,
			Integer pageSize) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("account_id", account_id);
		params.put("pageNumber", pageNumber);
		params.put("pageSize", pageSize);
		return queryForList("getAccountZZPromoterModelList", params);
	}

	public List<AccountPromoterReceiveModel> getAccountPromoterReceiveModel() {
		return queryForList("getAccountPromoterReceiveModel");
	}

	public void insertAccountPromoterReceiveModel(AccountPromoterReceiveModel model) {
		insertObject("insertAccountPromoterReceiveModel", model);
	}

	public void updateAccountPromoterReceiveModel(AccountPromoterReceiveModel model) {
		updateObject("updateAccountPromoterReceiveModel", model);
	}

	public void insertAccountZZPromoterModel(AccountZZPromoterModel model) {
		insertObject("insertAccountZZPromoterModel", model);
	}

	public void deleteAccountZZPromoterModel(AccountZZPromoterModel model) {
		deleteObject("deleteAccountZZPromoterModel", model);
	}

	public Integer getZZPromoterNum(Date start, Date end, long accountId) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("start", start);
		params.put("end", end);
		params.put("account_id", accountId);
		return (Integer) queryForObject("getZZPromoterNum", params);
	}

	/**
	 * @param model
	 */
	public List<RoomGeneratorModel> getRoomGeneratorList() {
		return queryForList("getRoomGeneratorList");
	}

	public void insertRoomGeneratorModel(RoomGeneratorModel model) {
		insertObject("insertRoomGeneratorModel", model);
	}

	public void updateRoomGeneratorModel(RoomGeneratorModel model) {
		insertObject("updateRoomGeneratorModel", model);
	}

}
