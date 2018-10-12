package com.cai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class MongoDBService{

	@Autowired
	private MongoTemplate mongoTemplate;

	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}
	

//	public <T> List<T>  test(Query query, Class<T> entityClass){
//		return mongoTemplate.find(query, entityClass);
//	}
	
	
//	@Override
//	public List<Object> findListByParams(Map<String, Object> params) {
//		
//	}
//
//	@Override
//	public List<Object> findListByQuery(Query query) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Object findOne(Map<String, Object> params) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Object findOne(Query query) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public void insert(Object object) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void save(Object object) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void insertAll(List<Object> object) {
//		// TODO Auto-generated method stub
//		
//	}

	
	
	
	
	
}
