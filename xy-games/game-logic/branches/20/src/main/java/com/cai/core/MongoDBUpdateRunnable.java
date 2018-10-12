package com.cai.core;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cai.common.domain.BrandLogModel;
import com.cai.common.util.SpringService;
import com.cai.service.MongoDBService;

public final class MongoDBUpdateRunnable implements Runnable{
	
	private BrandLogModel brandLogModel;
	
	public MongoDBUpdateRunnable(BrandLogModel brandLogModel){
		this.brandLogModel = brandLogModel; 
	}

	
	@Override
	public void run() {
		//更新操作
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("brand_id").is(brandLogModel.getBrand_id()));
		query.addCriteria(Criteria.where("log_type").is("parentBrand"));
		Update update = new Update();
		update.set("msg", brandLogModel.getMsg());
		mongoDBService.getMongoTemplate().updateFirst(query, update, BrandLogModel.class);
	}

}
