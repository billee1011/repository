package com.cai.dao;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.alibaba.fastjson.JSON;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.CityCodeModel;
import com.cai.common.domain.ClubAccountModel;
import com.cai.common.domain.ClubActivityModel;
import com.cai.common.domain.ClubBanPlayerModel;
import com.cai.common.domain.ClubBulletinModel;
import com.cai.common.domain.ClubDataModel;
import com.cai.common.domain.ClubGroupModel;
import com.cai.common.domain.ClubMatchModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.ClubModel;
import com.cai.common.domain.ClubRoomOverModel;
import com.cai.common.domain.ClubRoomOverModel.ClubRoomPlayerModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.ClubRuleRecordModel;
import com.cai.common.domain.ClubWelfareLotteryInfo;
import com.cai.common.domain.ClubWelfareRewardModel;
import com.cai.common.domain.ClubWelfareSwitchModel;
import com.cai.common.domain.MatchSignUpModel;
import com.cai.common.domain.OldUserModel;
import com.cai.common.domain.PlayerEmailModel;
import com.cai.common.domain.SysEmailModel;
import com.cai.common.domain.SysPalyerEmailModel;
import com.cai.dictionary.GameGroupRuleDict;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ibatis.sqlmap.client.SqlMapClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * @author demon date: 2017年9月1日 下午5:03:34 <br/>
 */
@Repository

public class ClubDao extends CoreDao {

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
	}
	// ===============================================================================

	public List<ClubModel> getClubList() {
		return queryForList("getClubList");
	}

	public void insertClub(ClubModel clubModel) {
		insertObject("insertClub", clubModel);
	}

	public void insertClubDataModel(ClubDataModel clubDataModel) {
		insertObject("insertClubDataModel", clubDataModel);
	}

	public void deleteClub(int club_id) {
		deleteObject("deleteClub", club_id);
	}

	public void updateClub(ClubModel clubModel) {
		updateObject("updateClub", clubModel);
	}

	public int updateClubLuckyId(int new_id, int club_id) {

		Map<String, Object> map = local.get();
		map.clear();
		map.put("new_id", new_id);
		map.put("club_id", club_id);
		return updateObject("updateClubLuckyId", map);
	}

	public int updateClubAccountId(long new_id, long account_id) {
		// TODO Auto-generated method stub
		Map<String, Object> map = local.get();
		map.clear();
		map.put("new_id", new_id);
		map.put("account_id", account_id);
		return updateObject("updateClubAccountId", map);
	}

	public int updateClubAccountId2(long new_id, long account_id) {
		// TODO Auto-generated method stub
		Map<String, Object> map = local.get();
		map.clear();
		map.put("new_id", new_id);
		map.put("account_id", account_id);
		return updateObject("updateClubAccountId2", map);
	}

	/**
	 * 成员备注
	 *
	 * @param remark
	 * @param remarkExt
	 * @param account_id
	 * @param club_id
	 * @return
	 */
	public int updateClubAccountRemark(String remark, String remarkExt, long account_id, int club_id) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("club_id", club_id);
		map.put("remark", remark);
		map.put("remark_ext", remarkExt);
		map.put("account_id", account_id);
		return updateObject("updateClubAccountRemark", map);
	}

	public int updateClubAccountIdentity(ClubMemberModel memberModel) {
		return updateObject("updateClubAccountIdentity", memberModel);
	}

	public int updateClubAccountPartner(ClubMemberModel memberModel) {
		return updateObject("updateClubAccountPartner", memberModel);
	}

	public int updateClubAccountWelfare(ClubMemberModel memberModel) {
		return updateObject("updateClubAccountWelfare", memberModel);
	}

	public int getSignSeq() {
		try {
			return (Integer) queryForObject("getSignSeq");
		} catch (Exception e) {
		}
		return 0;
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

	public List<ClubGroupModel> getClubGroupList() {
		return queryForList("getClubGroupList");
	}

	public List<ClubGroupModel> getClubGroup(int club_id) {
		return queryForList("getGroupByClubId", club_id);
	}

	public List<ClubGroupModel> getClubGroupByUserId(long userId) {
		return queryForList("getGroupByUserId", userId);
	}

	public void insertClubGroup(ClubGroupModel model) {
		insertObject("insertClubGroup", model);
	}

	public int deleteClubGroup(int club_id, String group_id) {

		Map<String, Object> map = local.get();
		map.clear();

		map.put("club_id", club_id);
		map.put("group_id", group_id);
		return deleteObject("deleteClubGroup", map);
	}

	public void deleteClubAllGroup(int club_id) {
		deleteObject("deleteClubAllGroup", club_id);
	}

	public List<ClubRuleModel> getClubRule(int club_id) throws InvalidProtocolBufferException {
		List<ClubRuleModel> list = queryForList("getRuleByClubId", club_id);
		for (ClubRuleModel clubRuleModel : list) {
			clubRuleModel.decodeRule();
			GameGroupRuleDict.getInstance().checkClubRule(clubRuleModel);
		}
		return list;
	}

	public List<ClubRuleModel> getAllClubRule() throws InvalidProtocolBufferException {
		List<ClubRuleModel> list = queryForList("getAllClubRule");
		for (ClubRuleModel clubRuleModel : list) {
			clubRuleModel.decodeRule();
			GameGroupRuleDict.getInstance().checkClubRule(clubRuleModel);
		}
		return list;
	}

	public void updateClubRule(ClubRuleModel clubRule) {
		clubRule.encodeRule();
		updateObject("updateClubRule", clubRule);
	}

	public void insertClubRule(Collection<ClubRuleModel> clubRule) {
		for (ClubRuleModel clubRuleModel : clubRule) {
			clubRuleModel.encodeRule();
			int id = (int) insertObject("insertClubRule", clubRuleModel);
			clubRuleModel.setId(id);
		}
	}

	public void deleteClubRule(int club_id) {
		deleteObject("deleteClubRule", club_id);
	}

	public void deleteClubRuleWithRuleId(int id) {
		deleteObject("deleteClubRuleWithRuleId", id);
	}

	public List<ClubMemberModel> getClubMembers(int club_id) {
		return queryForList("getClubMembers", club_id);
	}

	public List<ClubMemberModel> getAllClubMembers() {
		List<ClubMemberModel> list = queryForList("getAllClubMembers");
		for (ClubMemberModel model : list) {
			model.initLimitRoundData();
		}
		return list;
	}

	public List<OldUserModel> getWmqUserModel() {
		return queryForList("getWmqUser");
	}

	public List<OldUserModel> getHhUserModel() {
		return queryForList("getHuNanUser");
	}

	public MatchSignUpModel getMatchSignUp(long accountId, Date useTime) {
		Map<String, Object> params = local.get();
		params.put("accountId", accountId);
		params.put("useTime", useTime);

		return (MatchSignUpModel) queryForObject("getMatchSignUp", params);
	}

	// @SuppressWarnings("unchecked")
	// public List<MatchModel> getMatchModelList() {
	// return queryForList("matchModelList");
	//
	// }
	//
	// public MatchModel getMatchModel(int match_id) {
	// return (MatchModel) queryForObject("matchModel", match_id);
	//
	// }
	//
	// @SuppressWarnings("unchecked")
	// public List<MatchUnionModel> getMatchUnionModelList() {
	// return queryForList("matchUnionModelList");
	// }

	public List<SysEmailModel> getSysEmailModelList() {
		return queryForList("sysEmailModelList");
	}

	public SysEmailModel loadSysEmailModel(int id) {
		return (SysEmailModel) queryForObject("loadSysEmailModel", id);
	}

	@SuppressWarnings("rawtypes")
	public byte[] getData(String key) {
		Object object = queryForObject("getDataByKey", key);
		if (object == null) {
			return new byte[0];
		}
		return (byte[]) ((HashMap) object).get("data");
	}

	public void saveData(String key, byte[] data) {
		Map<String, Object> param = local.get();
		param.clear();
		param.put("key", key);
		param.put("data", data);

		updateObject("updateOrInsertData", param);
	}

	public void updatePlayerSysEmailModel(long accountId, Collection<SysPalyerEmailModel> emails) {
		Map<String, Object> param = local.get();
		param.put("accountId", accountId);
		param.put("sysEmail", JSON.toJSON(emails).toString());

		updateObject("updatePlayerSysEmailModel", param);
	}

	public List<SysPalyerEmailModel> creatPlayerSysEmailModel(long accountId) {
		Map<String, Object> param = Maps.newHashMap();
		param.put("accountId", accountId);
		param.put("sysEmail", JSON.toJSON(Lists.newArrayList().toString()));

		insertObject("creatPlayerSysEmailModel", param);

		return Lists.newArrayList();
	}

	public List<SysPalyerEmailModel> getPlayerSysEmailModel(long accountId) {
		List<SysPalyerEmailModel> result = Lists.newArrayList();

		Object o = queryForObject("sysEmailPlayerModelList", accountId);
		if (o == null)
			return null;

		String json = String.valueOf(o);
		result = JSON.parseArray(json, SysPalyerEmailModel.class);
		return result;
	}

	public void updateSysEmailState(int id, int sendState) {
		Map<String, Integer> param = Maps.newHashMap();
		param.put("sendState", sendState);
		param.put("id", id);
		updateObject("updateSysEmailState", param);
	}

	public List<PlayerEmailModel> getPlayerEmailModelList(long accountId) {
		return queryForList("getPlayerEmailModelList", accountId);
	}

	public void updatePlayerEmailState(int isOpen, int extraType, int mailId) {
		Map<String, Integer> param = Maps.newHashMap();
		param.put("isOpen", isOpen);
		param.put("extraType", extraType);
		param.put("mailId", mailId);
		updateObject("updatePlayerEmailState", param);
	}

	public int creatPlayerEmailModel(PlayerEmailModel model) {
		/*
		 * Map param = Maps.newHashMap(); param.put("isOpen", isOpen);
		 * param.put("extraType", extraType); param.put("mailId", mailId);
		 */
		return (int) insertObject("creatPlayerEmailModel", model);
	}

	@SuppressWarnings("rawtypes")
	public List getGroupAccountIdNotInClub(String groupId, int clubId) {
		Map<String, Object> param = local.get();
		param.put("groupId", groupId);
		param.put("clubId", clubId);
		return queryForList("getGroupAccountIdNotInClub", param);

	}

	@SuppressWarnings("rawtypes")
	public List getClubAccountIdNotInGroup(String groupId, int clubId) {
		Map<String, Object> param = Maps.newHashMap();
		param.put("groupId", groupId);
		param.put("clubId", clubId);
		return queryForList("getClubAccountIdNotInGroup", param);

	}

	public List<AccountGroupModel> getAccountGroupModelListByGroupId(String groupID) {
		return queryForList("getAccountGroupModelListByGroupId", groupID);
	}

	/**
	 * 修改俱乐部id
	 *
	 * @param new_club_id
	 * @param old_club_id
	 * @param account_id
	 * @return
	 */
	public int updateClubAccountClubId(int new_club_id, int old_club_id, long account_id) {

		Map<String, Object> map = local.get();
		map.clear();
		map.put("new_club_id", new_club_id);
		map.put("old_club_id", old_club_id);
		map.put("account_id", account_id);
		return updateObject("updateClubAccountClubId", map);
	}

	/**
	 * @param new_club_id
	 * @param old_club_id
	 * @return
	 */
	public int updateClubId(int new_club_id, int old_club_id) {

		Map<String, Object> map = local.get();
		map.clear();
		map.put("new_club_id", new_club_id);
		map.put("old_club_id", old_club_id);
		return updateObject("updateClubId", map);
	}

	/**
	 * 俱乐部活动列表
	 *
	 * @return
	 */
	public List<ClubActivityModel> getClubActivityModelList() {
		return queryForList("getClubActivityModelList");
	}

	/**
	 * 删除活动
	 *
	 * @param id
	 * @param club_id
	 */
	public int deleteClubActivity(long id, int clubId) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("id", id);
		return deleteObject("deleteClubActivity", map);
	}

	/**
	 * 删除活动
	 *
	 * @param id
	 * @param club_id
	 */
	public int deleteAllClubActivity(int clubId) {
		return deleteObject("deleteAllClubActivity", clubId);
	}

	/**
	 * 创建或者更新
	 *
	 * @param model
	 * @return
	 */
	public long updateOrInsertClubActivity(ClubActivityModel model) {
		long id = (long) insertObject("updateOrInsertClubActivity", model);
		model.setId(id);
		return id;
	}

	public List<CityCodeModel> getCityCodeModelList() {
		return queryForList("getCityCodeModelList");
	}

	// ------------------- 俱乐部公告 --------------------------------------------

	/**
	 * 俱乐部公告列表
	 *
	 * @return
	 */
	public List<ClubBulletinModel> getClubBulletinModelList() {
		return queryForList("getClubBulletinModelList");
	}

	/**
	 * 创建
	 *
	 * @param model
	 * @return
	 */
	public long insertClubBulletin(ClubBulletinModel model) {
		long id = (long) insertObject("insertClubBulletin", model);
		model.setId(id);
		return id;
	}

	/**
	 * 更新
	 *
	 * @param model
	 * @return
	 */
	public long updateClubBulletin(ClubBulletinModel model) {
		long id = (long) updateObject("updateClubBulletin", model);
		return id;
	}

	public List<ClubMemberRecordModel> getAllClubMemberRecord() {
		return queryForList("getAllClubMemberRecord");
	}

	public Object insertClubMemberRecordModel(ClubMemberRecordModel clubMemberRecordModel) {
		return insertObject("insertClubMemberRecord", clubMemberRecordModel);
	}

	public Object updateClubMemberRecordModel(ClubMemberRecordModel clubMemberRecordModel) {
		return updateObject("updateClubMemberRecord", clubMemberRecordModel);
	}

	public void deleteClubAllMemberRecord(int club_id) {
		deleteObject("deleteClubAllMemberRecord", club_id);
	}

	public void deleteClubMemberRecord(int clubId, long accountId) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("accountId", accountId);
		deleteObject("deleteClubMemberRecord", map);
	}

	public List<ClubBanPlayerModel> getAllClubMemberBanPlayer() {
		return queryForList("getAllClubMemberBanPlayer");
	}

	public void deleteClubAllMemberBanPlayer(int club_id) {
		deleteObject("deleteClubAllMemberBanPlayer", club_id);
	}

	public void deleteClubMemberBanPlayer(int clubId, long accountId) {
		Map<String, Object> map = local.get();
		map.clear();
		map.put("clubId", clubId);
		map.put("accountId", accountId);
		deleteObject("deleteClubMemberBanPlayer", map);
	}

	public List<ClubMatchModel> getAllClubMatchs() {
		return queryForList("getAllClubMatch");
	}

	public Object insertClubMatchModel(ClubMatchModel model) {
		return insertObject("insertClubMatchModel", model);
	}

	public Object updateClubMatchModel(ClubMatchModel model) {
		return updateObject("updateClubMatchModel", model);
	}

	public void deleteClubMatchModel(ClubMatchModel model) {
		deleteObject("deleteClubMatchModel", model);
	}

	public List<ClubDataModel> getClubDataList() {
		return queryForList("getClubDataList");
	}

	public void deleteClubAllMatch(int club_id) {
		deleteObject("deleteClubAllMatch", club_id);
	}

	public void deleteClubDataModel(int club_id) {
		deleteObject("deleteClubDataModel", club_id);
	}

	public List<ClubRuleRecordModel> getAllClubRuleRecord() {
		return queryForList("getClubRuleRecordList");
	}

	public void deleteClubAllRuleRecordModel(int club_id) {
		deleteObject("deleteClubAllRuleRecordModel", club_id);
	}

	public List<ClubWelfareSwitchModel> getClubWelfareSwitchModel() {
		return queryForList("getClubWelfareSwitchModel");
	}

	public List<ClubWelfareRewardModel> getClubWelfareRewardList() {
		return queryForList("getAllClubWelfareRewardModel");
	}

	public List<ClubWelfareLotteryInfo> getClubWelfareLotteryInfoList() {
		return queryForList("getAllClubWelfareLotteryInfo");
	}

	public void updateClubWelfareLotteryInfo(ClubWelfareLotteryInfo info) {
		updateObject("updateClubWelfareLotteryInfo", info);
	}

	public void insertClubWelfareLotteryInfo(ClubWelfareLotteryInfo info) {
		insertObject("insertClubWelfareLotteryInfo", info);
	}

	public void updateClubWelfareLastLotteryTime(ClubWelfareSwitchModel model) {
		updateObject("updateClubWelfareLastLotteryTime", model);
	}
}
