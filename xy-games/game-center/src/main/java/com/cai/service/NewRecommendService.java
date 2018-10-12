package com.cai.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.domain.Event;
import com.cai.common.domain.HallRecommendModel;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

/**
 * 
 * 钻石代理
 *
 * @author tang date: 2017年8月21日 上午10:43:45 <br/>
 */
public class NewRecommendService extends AbstractService {

	private static Logger logger = LoggerFactory.getLogger(NewRecommendService.class);

	private static ConcurrentHashMap<String, HallRecommendModel> recommendMap;
	private static NewRecommendService instance;

	private NewRecommendService() {
	}

	public static NewRecommendService getInstance() {
		if (null == instance) {
			instance = new NewRecommendService();
			recommendMap = new ConcurrentHashMap<String, HallRecommendModel>();
		}
		return instance;
	}

	// 添加缓存
	public void pushRecommender(String key, HallRecommendModel model) {
		recommendMap.put(key, model);
	}

	// 入库的时候删除缓存
	public HallRecommendModel reduceRecommendMap(String key) {
		return recommendMap.remove(key);
	}

	// 查询缓存
	public boolean containsKey(String key) {
		return recommendMap.containsKey(key);
	}

	public static Map<Integer,Date> getDateByType(int type){
		Map<Integer,Date> map = new HashMap<>();
		Date first = null;
		Date end = null;
		if(type==1){
			Calendar c = Calendar.getInstance();    
			c.add(Calendar.MONTH, 0);
			c.set(Calendar.DAY_OF_MONTH,1);//设置为1号,当前日期既为本月第一天 
			first = c.getTime();
			end = new Date();
		}else if(type == 2){
			Calendar   cal_1=Calendar.getInstance();//获取当前日期 
			cal_1.add(Calendar.MONTH, -1);
			cal_1.set(Calendar.DAY_OF_MONTH,1);//设置为1号,当前日期既为本月第一天 
			//获取前月的最后一天
			Calendar cale = Calendar.getInstance();   
			cale.set(Calendar.DAY_OF_MONTH,0);//设置为1号,当前日期既为本月第一天 
			first = cal_1.getTime();
			end = cale.getTime();
		}else{
			Calendar currCal=Calendar.getInstance();    
		    int currentYear = currCal.get(Calendar.YEAR); 
		    currCal.clear();  
		    currCal.set(Calendar.YEAR,currentYear);  
			first = currCal.getTime();
			end = new Date();
		}
		map.put(1, first);
		map.put(2, end);
		return map;
	}
	@Override
	protected void startService() {

	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {

	}

}
