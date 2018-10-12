package com.cai.core;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cai.common.domain.BrandLogModel;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.service.MongoDBService;

public final class MongoDBUpdateRunnable implements Runnable {

	private BrandLogModel brandLogModel;

	public MongoDBUpdateRunnable(BrandLogModel brandLogModel) {
		this.brandLogModel = brandLogModel;
	}

	@Override
	public void run() {
		// 更新操作
		try {
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query();
			query.addCriteria(Criteria.where("brand_id").is(brandLogModel.getBrand_id()));
			query.addCriteria(Criteria.where("log_type").is("parentBrand"));
			Update update = new Update();
			update.set("msg", brandLogModel.getMsg());
			update.set("isRealKouDou", brandLogModel.isRealKouDou());
			update.set("accountIds", brandLogModel.getAccountIds());

			if (null != brandLogModel.getReal_cost_time()) {
				update.set("real_cost_time", brandLogModel.getReal_cost_time());
			}
			if (null != brandLogModel.getGame_end_time()) {
				update.set("game_end_time", brandLogModel.getGame_end_time());
			}
			boolean inFisrt = RandomUtil.random.nextBoolean();
			int random = RandomUtil.generateRandomNumber(10, 99);
			String randomNum = inFisrt ? random + brandLogModel.getV3() : brandLogModel.getV3() + random;
			// String randomNum = SystemConfig.logic_index +
			// brandLogModel.getV3();

			update.set("randomNum", Integer.parseInt(randomNum));
			mongoDBService.getMongoTemplate().updateFirst(query, update, BrandLogModel.class);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

}
