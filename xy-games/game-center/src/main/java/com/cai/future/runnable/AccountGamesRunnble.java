/**
 * 
 */
package com.cai.future.runnable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;

import com.cai.common.domain.Account;
import com.cai.common.domain.AccountGamesModel;
import com.cai.common.domain.GamesAccountModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.IFoundationRMIServer;
import com.cai.common.rmi.vo.InviteMissionDataVo;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PublicServiceImpl;

/**
 * @author tang
 *
 */
public class AccountGamesRunnble implements Runnable {

	private static Logger logger = Logger.getLogger(AccountGamesRunnble.class);

	private Map<String, String> map = null;

	public AccountGamesRunnble(Map<String, String> map) {
		this.map = map;
	}

	@Override
	public void run() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			String accountIds = map.get("account_ids");
			String gameId = map.get("game_type_index");
			String createType = map.get("createType");
			String all_round = map.get("all_round");
			boolean isAllRound = StringUtils.isEmpty(all_round)? false : true;
			if (StringUtils.isBlank(accountIds) || StringUtils.isBlank(gameId)) {
				return;
			}
			SysParamModel sysParamModel2232 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2232);
			if (sysParamModel2232 == null) {
				return;
			}
			String[] ids = accountIds.split(",");
			int game_type_index = Integer.parseInt(gameId);
			Date date = new Date();
			int notes_date = Integer.parseInt(DateFormatUtils.format(date, "yyyyMMdd"));
			int type = 0;
			if (StringUtils.isNotBlank(createType)) {
				type = Integer.parseInt(createType);
			}
			//每个玩家的推荐任务
			List<InviteMissionDataVo> missionBuilderList = new ArrayList<>(10);
			InviteMissionDataVo missionData = null;
			int gameID = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
			for (String accountId : ids) {
				long account_id = Long.parseLong(accountId);
				Account account = PublicServiceImpl.getInstance().getAccount(account_id);
				
				if (account == null || account.getAccountModel() == null || account.getAccountModel().getCreate_time() == null) {
					continue;
				}
				//upsertDailyBrand(account_id, type, notes_date, account.getAccountModel().getCreate_time());
				addDailyBrandRecord(account_id, type, notes_date, account.getAccountModel().getCreate_time());
				// 只针对某个时间点之后的新用户进行处理
				if ((account.getAccountModel().getCreate_time().getTime() / 1000) < sysParamModel2232.getVal3()) {
					continue;
				}
				AccountGamesModel accountGamesModel = PlayerServiceImpl.getInstance().getAccountGamesModelByAccountId(account_id);
				if (accountGamesModel == null) {
					accountGamesModel = MongoDBServiceImpl.getInstance().getAccountGamesModelByAccountId(account_id);
					PlayerServiceImpl.getInstance().addOrUpdateAccountGames(account_id, accountGamesModel);
				}
				
				if(isAllRound && account.getAccountModel().getRecommend_id() > 0) {
					missionData = new InviteMissionDataVo();
					//通知给推荐人任务刷新
					missionData.setAccountId(account.getAccountModel().getRecommend_id());
					missionData.setBeRecommendId(account_id);
					missionData.setCount(1);
					missionData.setGame(gameID);
					if(null == account.getAccountModel().getCreate_time()) {
						missionData.setCreateTime(0);
					} else {
						missionData.setCreateTime(account.getAccountModel().getCreate_time().getTime());
					}
					missionBuilderList.add(missionData);
				}
				if (accountGamesModel.getGames() == null || accountGamesModel.getGames().isEmpty()) {
					accountGamesModel.setAccountId(account_id);
					Set<Integer> set = new HashSet<>();
					set.add(game_type_index);
					accountGamesModel.setGames(set);
					GamesAccountModel model = new GamesAccountModel();
					model.setAccountId(accountGamesModel.getAccountId());
					model.setCreate_time(date);
					model.setGame_type_index(game_type_index);
					MongoDBServiceImpl.getInstance().addOrUpdateAccountGamesModel(accountGamesModel);
					MongoDBServiceImpl.getInstance().addGamsAccount(accountGamesModel, game_type_index);
					continue;
				}
				if (!accountGamesModel.getGames().contains(game_type_index)) {
					accountGamesModel.getGames().add(game_type_index);
					GamesAccountModel model = new GamesAccountModel();
					model.setAccountId(accountGamesModel.getAccountId());
					model.setCreate_time(date);
					model.setGame_type_index(game_type_index);
					MongoDBServiceImpl.getInstance().addOrUpdateAccountGamesModel(accountGamesModel);
					MongoDBServiceImpl.getInstance().addGamsAccount(accountGamesModel, game_type_index);
				}
			}
			if(missionBuilderList.size() > 0) {
				try {
					IFoundationRMIServer foundationRMIServer = SpringService.getBean(IFoundationRMIServer.class);
					foundationRMIServer.recommendMissionNotify(missionBuilderList);
				} catch(Exception e) {
					logger.error("邀请下载任务通知失败", e);
				}
			}
		} catch (Exception e) {
			logger.error("玩家游戏列表入库失败," + timer.getStr(), e);
		}
	}
	
	private void addDailyBrandRecord(long accountId, int createType, int notes_date, Date registerTime) {
		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		if(null != account) {
			account.addAccoundBrand(accountId, createType, notes_date, registerTime);
		}
	}

//	private void upsertDailyBrand(long accountId, int createType, int notes_date, Date registerTime) {
//		try {
//			Query query = new Query();
//			query.addCriteria(Criteria.where("account_id").is(accountId)).addCriteria(Criteria.where("date").is(notes_date))
//					.addCriteria(Criteria.where("type").is(createType));
//			// 牌局次数由Mongodb自增1
//			Update update = new Update();
//			update.inc("count", 1);
//			update.set("registerTime", registerTime);
//			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
//			mongoDBService.getMongoTemplate().upsert(query, update, AccountDailyBrandStatistics.class);
//		} catch (Exception e) {
//			logger.error("MongoDBService taskJob error", e);
//		}
//	}
}
