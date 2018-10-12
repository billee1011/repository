/**
 * 
 */
package com.cai.future.runnable;

import java.util.Date;

import org.apache.log4j.Logger;

import com.cai.common.domain.AutoUpdateRecomLevelModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.SysParamModel;
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
public class AutoDownRecommendLevelRunnble implements Runnable {

	private static Logger logger = Logger.getLogger(AutoDownRecommendLevelRunnble.class);

	private long account_id = 0;
	private int receive = 0;

	public AutoDownRecommendLevelRunnble(long account_id, int receive) {
		this.account_id = account_id;
		this.receive = receive;
	}

	@Override
	public void run() {
		try {
			SysParamModel sysParamModel2253 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2253);
			if (sysParamModel2253 == null || sysParamModel2253.getVal4() == 0) {
				return;
			}
			HallRecommendModel hallRecommendModel = PublicServiceImpl.getInstance().getHallRecommendModel(account_id);
			if (hallRecommendModel == null || hallRecommendModel.getProxy_level() == 0) {
				// 未开启，不处理
				return;
			}
			int level = hallRecommendModel.getRecommend_level();
			long count = receive;
			if (level == 3) {
				if (count < sysParamModel2253.getVal1()) {
					// 白银降级
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setHallRecommentId(hallRecommendModel.getAccount_id());
					rsAccountModelResponse.setHallRecommentLevel(0);
					SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
					RecommenderReceiveService.getInstance().updateLevel(account_id, 0);
					log_down_record(account_id, 0, 3, "上月返利" + count / 100 + "低于" + sysParamModel2253.getVal1() / 100 + ",白银降级为见习推广员");
				}
			} else if (level == 2) {
				if (count < sysParamModel2253.getVal2()) {
					// 黄金降级
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setHallRecommentId(hallRecommendModel.getAccount_id());
					rsAccountModelResponse.setHallRecommentLevel(3);
					SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
					RecommenderReceiveService.getInstance().updateLevel(account_id, 3);
					log_down_record(account_id, 3, 2, "上月返利" + count / 100 + "低于" + sysParamModel2253.getVal2() / 100 + ",黄金降级为白银推广员");
				}
			} else if (level == 1) {
				if (count < sysParamModel2253.getVal3()) {
					// 钻石降级
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setHallRecommentId(hallRecommendModel.getAccount_id());
					rsAccountModelResponse.setHallRecommentLevel(2);
					SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
					RecommenderReceiveService.getInstance().updateLevel(account_id, 2);
					log_down_record(account_id, 2, 1, "上月返利" + count / 100 + "低于" + sysParamModel2253.getVal3() / 100 + ",钻石降级为黄金推广员");
				}
			} else {
				return;
			}

		} catch (Exception e) {
			logger.error(account_id + " 推广员自动降级失败", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void log_down_record(long account_id, int curLevel, int oldLevel, String desc) {
		AutoUpdateRecomLevelModel levelModel = new AutoUpdateRecomLevelModel();
		levelModel.setAccount_id(account_id);
		levelModel.setCreate_time(new Date());
		levelModel.setCurLevel(curLevel);
		levelModel.setOldLevel(oldLevel);
		levelModel.setDesc(desc);
		levelModel.setType(2);
		MongoDBServiceImpl.getInstance().getLogQueue().add(levelModel);
	}

}
