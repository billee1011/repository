/**
 * 
 */
package com.cai.future.runnable;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.service.MongoDBService;

/**
 * @author xwy
 *
 */
public class FiveCleanRunnble implements Runnable {

	private static Logger logger = Logger.getLogger(FiveCleanRunnble.class);

	private static int fail_Times = 0;
	
	private Class<?> entityClass;
	
	private int day;
	
	public FiveCleanRunnble(int day,Class<?> entityClass) {
		this.day = day;
		this.entityClass= entityClass;
	}
	
	@Override
	public void run() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			fail_Times++;
			Date d2 = MyDateUtil.getZeroDate(MyDateUtil.getNow());
			Date d1 = DateUtils.addDays(d2, day);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

			// 删除过期数据
			Query query = new Query();
			query.addCriteria(Criteria.where("create_time").lt(d1));
			mongoDBService.getMongoTemplate().remove(query, entityClass);

			logger.info("FiveCleanRunnble 5点删除过期数据," + entityClass.getName() +"," + timer.getStr());
		} catch (Exception e) {
			logger.error("error" + entityClass.getName() +timer.getStr()+ fail_Times, e);
			if(fail_Times<5) {
				GameSchedule.put(new FiveCleanRunnble(day-5,entityClass), 25, TimeUnit.MINUTES);
			}
		}
	}

}
