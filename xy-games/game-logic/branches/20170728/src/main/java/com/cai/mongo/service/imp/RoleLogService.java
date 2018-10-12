package com.cai.mongo.service.imp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.cai.common.util.PerformanceTimer;
import com.cai.mongo.service.IRoleLogService;
import com.cai.mongo.service.log.bean.RoleLogBase;

@Service
public class RoleLogService implements IRoleLogService {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public List<RoleLogBase> findListByParams(Map<String, Object> params) {
		return mongoTemplate.find(new Query(), RoleLogBase.class);
	}

	@Override
	public List<RoleLogBase> findListByQuery(Query query) {
		return mongoTemplate.find(query, RoleLogBase.class);
	}

	@Override
	public RoleLogBase findOne(Query query) {
		// TODO Auto-generated method stub
		return mongoTemplate.findOne(query, RoleLogBase.class);
	}

	@Override
	public RoleLogBase findOne(Map<String, Object> params) {
		return mongoTemplate.findOne(new Query(), RoleLogBase.class);
	}

	@Override
	public void insert(RoleLogBase object) {
		mongoTemplate.insert(object);
	}

	@Override
	public void save(RoleLogBase object) {
		mongoTemplate.save(object);
	}

	@Override
	public void insertAll(List<RoleLogBase> object) {
		mongoTemplate.insertAll(object);
	}

	public void test() {

		// mongoTemplate.dropCollection(RoleLogBase.class);

		RoleLogBase roleLogBase = null;
		ArrayList<RoleLogBase> list = new ArrayList<>();
		long time = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			roleLogBase = new RoleLogBase();
			roleLogBase.setRoleId(i);
			roleLogBase.setAfterNum(i  );
			roleLogBase.setChangeNum(i );
			roleLogBase.setTime(time);
			roleLogBase.setRoleName(i + "phz");
			roleLogBase.setMsgCode(i);
			list.add(roleLogBase);

		}
		PerformanceTimer timer = new PerformanceTimer();
		insertAll(list);

		// Criteria criteria =
		// Criteria.where("_id").is("57e8c4f82402a8caafc41f7e");
		// Criteria criteria =
		// Criteria.where("msgCode").is(Integer.valueOf(30));

		// Criteria criteria = Criteria.where("roleName").is("1phz");
		// Criteria criteria =
		// Criteria.where("msgCode").is(Integer.valueOf(30)).and("roleId").is(Integer.valueOf(30));
		// Query query = new Query();
		//
		// List<RoleLogBase> roleLogBases = findListByQuery(query);
		// System.out.println(roleLogBases.size());

		// Set<String> collectionNames = mongoTemplate.getCollectionNames();
		//
		// // 打印出test中的集合
		// for (String name : collectionNames) {
		// System.out.println("collectionName===" + name);
		// }
	}

}
