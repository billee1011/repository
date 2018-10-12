package com.cai.test.xwy;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.Mongo;

public class MongoApp {

	private static final Log log = LogFactory.getLog(MongoApp.class);

	public static void main(String[] args) throws Exception {

//		MongoOperations mongoOps = new MongoTemplate(new Mongo(), "database");
	
		
		MongoOperations mongoOps = new MongoTemplate(new SimpleMongoDbFactory(new Mongo(), "database"));

	    
	    mongoOps.dropCollection(Person.class);

		mongoOps.insert(new Person("Joe", 34));

		log.info(mongoOps.findOne(new Query(where("name").is("Joe")), Person.class));

		mongoOps.dropCollection("person");

		

		mongoOps.insert(new Person("Joe", 35));

		log.info(mongoOps.findOne(new Query(where("name").is("Joe")), Person.class));

		mongoOps.dropCollection("person");
		
		MongoOperations mongoTemplate = new MongoTemplate(new SimpleMongoDbFactory(new Mongo(), "database"));
		
		mongoTemplate.insert(new Person("Tom", 21));
		mongoTemplate.insert(new Person("Dick", 22));
		mongoTemplate.insert(new Person("Harry", 23));

		
		Update update = new Update().inc("age", 1);

		Query query = new Query(Criteria.where("firstName").is("Harry"));

	}
}