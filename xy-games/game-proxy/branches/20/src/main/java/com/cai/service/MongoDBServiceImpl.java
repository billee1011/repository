package com.cai.service;

import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.SystemLogModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.timer.MogoDBTimer;
import com.google.common.collect.Lists;
import com.xianyi.framework.server.AbstractService;

/**
 * mogodb服务类
 * @author run
 *
 */
public class MongoDBServiceImpl extends AbstractService{
	
	/**
	 * 日志队列
	 */
	private LinkedBlockingQueue logQueue = new LinkedBlockingQueue();
	
	
	private Timer timer;
	
	
	
	private static MongoDBServiceImpl instance = null;
	
	
	//引用
	private MogoDBTimer mogoDBTimer;

	private MongoDBServiceImpl() {
		timer = new Timer("Timer-MongoDBServiceImpl Timer");
	}

	public static MongoDBServiceImpl getInstance() {
		if (null == instance) {
			instance = new MongoDBServiceImpl();
		}
		return instance;
	}
	
	
	/**
	 * 玩家日志
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void log(long account_id,ELogType eLogType,String msg,Long v1,Long v2,String account_ip){
		GameLogModel gameLogModel = new GameLogModel();
		gameLogModel.setCreate_time(new Date());
		gameLogModel.setAccount_id(account_id);
		gameLogModel.setProxy_id(SystemConfig.proxy_index);
		gameLogModel.setLog_type(eLogType.getId());
		gameLogModel.setMsg(msg);
		gameLogModel.setV1(v1);
		gameLogModel.setV2(v2);
		gameLogModel.setLocal_ip(SystemConfig.localip);
		gameLogModel.setAccount_ip(account_ip);
		logQueue.add(gameLogModel);
	}
	
	/**
	 * 系统日志
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void systemLog(ELogType eLogType,String msg,Long v1,Long v2,ESysLogLevelType eSysLogLevelType){
		SystemLogModel systemLogModel = new SystemLogModel();
		systemLogModel.setCreate_time(new Date());
		systemLogModel.setProxy_id(SystemConfig.proxy_index);
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLocal_ip(SystemConfig.localip);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}
	
	
	/**
	 * 玩家的最后10条大牌局记录(2天内的记录)   TODO 优化
	 * 
	 * @param account_id
	 */
	public List<BrandLogModel> getParentBrandListByAccountId(long account_id,int game_id) {
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("game_id").is(game_id));
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -2)));
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));
		query.with(new Sort(Direction.DESC,"create_time"));
		query.limit(15);//15局
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		
		List<Long> brandIdList = Lists.newArrayList();
		for(BrandLogModel model : brandLogModelList){
			if(model.getBrand_id()==0L)
				continue;
			brandIdList.add(model.getBrand_id());
		}
		
		//转成大局记录
		query = new Query();
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		
		return brandLogModelList;
	}
	
	/**
	 * @param brand_parent_id
	 */
	public BrandLogModel getParentBrandByParentId(long brand_parent_id,int game_id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("game_id").is(game_id));
		query.addCriteria(Criteria.where("brand_id").is(brand_parent_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		BrandLogModel brandLogModel = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);
		return brandLogModel;
	}
	
	/**
	 * 根据大牌局获取所有子牌局(60天内的记录)  TODO 优化
	 * @param brand_parent_id
	 * @return
	 */
	public List<BrandLogModel> getChildBrandList(long brand_parent_id,int game_id){
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("game_id").is(game_id));
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -60)));
		query.addCriteria(Criteria.where("brand_parent_id").is(brand_parent_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.childBrand.getId()));
		query.with(new Sort(Direction.ASC,"create_time"));
		query.limit(20);
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		return brandLogModelList;
	}
	
	/**
	 * 查询指定的子牌局
	 * @param brand_id
	 * @param game_id
	 * @return
	 */
	public BrandLogModel getChildBrandByBrandId(long brand_id,int game_id){
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("game_id").is(game_id));
		query.addCriteria(Criteria.where("brand_id").is(brand_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.childBrand.getId()));
		BrandLogModel brandLogModel = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);
		return brandLogModel;
	}
	
	
	/**
	 * 代理转卡记录
	 * @param account_id
	 * @param target_account_id
	 * @param give_num
	 * @param account_ip
	 * @param code
	 */
	public void proxyGoldLog(long account_id,long target_account_id,String target_nick_name,long give_num,String account_ip,int code,int target_proxy_account){
		ProxyGoldLogModel model = new ProxyGoldLogModel();
		model.setCreate_time(MyDateUtil.getNow());
		model.setAccount_id(account_id);
		model.setTarget_account_id(target_account_id);
		model.setTarget_nick_name(target_nick_name);
		model.setGive_num(give_num);
		model.setAccount_ip(account_ip);
		model.setCode(code);
		model.setTarget_proxy_account(target_proxy_account);
		logQueue.add(model);
	}

	/**
	 * 代理转卡历史记录
	 * @param page
	 * @param account_id
	 * @return
	 */
	public List<ProxyGoldLogModel> getProxyGoldLogModelList(Page page,long account_id,Long target_account_id) {
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -2)));
		if(target_account_id!=null){
			query.addCriteria(Criteria.where("target_account_id").is(target_account_id));
		}
		query.with(new Sort(Direction.DESC,"create_time"));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<ProxyGoldLogModel> proxyGoldLogModelList = mongoDBService.getMongoTemplate().find(query, ProxyGoldLogModel.class);
		return proxyGoldLogModelList;
	}
	/**
	 * 代理转卡历史记录数
	 * @param account_id
	 * @return
	 */
	public int getProxyGoldLogModelCount(long account_id,Long target_account_id){
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -2)));
		if(target_account_id!=null){
			query.addCriteria(Criteria.where("target_account_id").is(target_account_id));
		}
		query.with(new Sort(Direction.DESC,"create_time"));
		long count = mongoDBService.getMongoTemplate().count(query, ProxyGoldLogModel.class);
		return (int)count;
	}
	

	@Override
	protected void startService() {
		mogoDBTimer = new MogoDBTimer();
		timer.schedule(mogoDBTimer, 1000, 1000);
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
	}

	public LinkedBlockingQueue getLogQueue() {
		return logQueue;
	}

	public MogoDBTimer getMogoDBTimer() {
		return mogoDBTimer;
	}

}
