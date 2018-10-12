/**
 * 
 */
package com.cai.future.runnable;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.domain.Account;
import com.cai.common.domain.AutoUpdateRecomLevelModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamServerDict;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RecommenderReceiveService;

import protobuf.redis.ProtoRedis.RsAccountModelResponse;

/**
 * @author tang
 *
 */
public class AutoUpdateRecommendLevelRunnble implements Runnable {

	private static Logger logger = Logger.getLogger(AutoUpdateRecommendLevelRunnble.class);

	private long account_id = 0;

	public AutoUpdateRecommendLevelRunnble(long account_id) {
		this.account_id = account_id;
	}

	@Override
	public void run() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			SysParamModel sysParamModel2252 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2252);
			if (sysParamModel2252 == null || sysParamModel2252.getVal4() == 0) {
				return;
			}

			// HallRecommendModel hallRecommendModel =
			// PublicServiceImpl.getInstance().getHallRecommendModel(account_id);
			Account account = PublicServiceImpl.getInstance().getAccount(account_id);
			if (account.getAccountModel().getProxy_level() == 0) {
				return;
			}
			HallRecommendModel hallRecommendModel = account.getHallRecommendModel();
			if (hallRecommendModel.getRecommend_level() == 1) {// 钻石推广员无需处理
				return;
			}
			if (hallRecommendModel.getRecommend_level() == 0) {
				Map<Long, HallRecommendModel> map = account.getHallRecommendModelMap();
				int i = 0;
				for (HallRecommendModel rmodel : map.values()) {
					if (rmodel.getProxy_level() == 1) {
						i++;
					}
				}
				if (i < sysParamModel2252.getVal5()) {// 未达到开通三个代理的基本条件
					return;
				}
				Date now = new Date();
				Date start = MyDateUtil.getNowMonthFirstDay();
				long count = MongoDBServiceImpl.getInstance().queryOwnerRechargeMoney(account_id, start, now);
				if (hallRecommendModel.getRecommend_level() == 0 && count >= sysParamModel2252.getVal3()) {
					// 升级白银
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setHallRecommentId(hallRecommendModel.getAccount_id());
					rsAccountModelResponse.setHallRecommentLevel(3);
					RecommenderReceiveService.getInstance().updateLevel(account_id, 3);
					SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
					log_update_record(account_id, 3, 0, "升级为白银推广员");
				}

			}
			int level = hallRecommendModel.getRecommend_level();
			long count = RecommenderReceiveService.getInstance().getRecommendReceiveModel(account_id).getReceive();
			if (level == 3) {
				if (count >= sysParamModel2252.getVal1()) {
					// 升级黄金
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setHallRecommentId(hallRecommendModel.getAccount_id());
					rsAccountModelResponse.setHallRecommentLevel(2);
					RecommenderReceiveService.getInstance().updateLevel(account_id, 2);
					SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
					log_update_record(account_id, 2, 3, "升级为黄金推广员");
				}
			} else if (level == 2) {
				if (count > sysParamModel2252.getVal2()) {
					// 升级钻石
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setHallRecommentId(hallRecommendModel.getAccount_id());
					rsAccountModelResponse.setHallRecommentLevel(1);
					RecommenderReceiveService.getInstance().updateLevel(account_id, 1);
					SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
					log_update_record(account_id, 1, 2, "升级为钻石推广员");
				}
			} else {
				return;
			}

		} catch (Exception e) {
			logger.error(account_id + " 推广员自动升级失败," + timer.getStr(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private void log_update_record(long account_id, int curLevel, int oldLevel, String desc) {
		AutoUpdateRecomLevelModel levelModel = new AutoUpdateRecomLevelModel();
		levelModel.setAccount_id(account_id);
		levelModel.setCreate_time(new Date());
		levelModel.setCurLevel(curLevel);
		levelModel.setOldLevel(oldLevel);
		levelModel.setDesc(desc);
		levelModel.setType(1);
		MongoDBServiceImpl.getInstance().getLogQueue().add(levelModel);
	}
}
