package com.cai.service;

import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ERedpacketPoolType;
import com.cai.common.domain.AccountRedpacketPoolModel;
import com.cai.common.domain.Event;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

/**
 * 
 * 钻石代理
 *
 * @author Administrator date: 2017年8月21日 上午10:43:45 <br/>
 */
public class AccountRedpacketPoolService extends AbstractService {

	private static Logger logger = LoggerFactory.getLogger(AccountRedpacketPoolService.class);

	private static ConcurrentHashMap<Long, AccountRedpacketPoolModel> accountRedpacketPoolMap;
	
	private static AccountRedpacketPoolService instance;
	private final Timer timer;

	private AccountRedpacketPoolService() {
		timer = new Timer("accountRedpacketPoolService-Timer");
	}

	public static AccountRedpacketPoolService getInstance() {
		if (null == instance) {
			instance = new AccountRedpacketPoolService();
			accountRedpacketPoolMap = new ConcurrentHashMap<>();
		}
		return instance;
	}

	private void load() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<AccountRedpacketPoolModel> accountRedpacketPoolModelList = publicService.getPublicDAO().getAccountRedpacketPoolModelList();
		for(AccountRedpacketPoolModel model:accountRedpacketPoolModelList){
			accountRedpacketPoolMap.put(model.getAccount_id(), model);
		}
		logger.info("load AccountRedpacketPoolService success!");
	}
	//红包池领取红包是否成功
	public boolean takeMoney(long account_id, int money,int log_type,String msg){
		AccountRedpacketPoolModel model = accountRedpacketPoolMap.get(account_id);
		if (model == null||money<=0) {
			return false;
		}else{
			ReentrantLock reentrantLock = model.getRedisLock();
			reentrantLock.lock();
			try{
				if(model.getMoney()-money<0){
					return false;
				}
				model.setMoney(model.getMoney() - money);
				model.setNeedDB(true);
				MongoDBServiceImpl.getInstance().addRedpacketPoolLogModel(account_id, log_type, money, model.getMoney(),msg);
				return true;
			}catch (Exception e) {
				logger.error("add money error!", e);
			}finally{
				reentrantLock.unlock();	
			}
		}
		return false;
	}
	public int getAccountRedpacketPoolModel(long account_id){
		AccountRedpacketPoolModel model = accountRedpacketPoolMap.get(account_id);
		return model==null?0:model.getMoney();
	}
	//红包池添加红包金额
	public boolean addMoney(long account_id, int money) {
		try {
			AccountRedpacketPoolModel model = accountRedpacketPoolMap.get(account_id);
			if (model == null) {
				model = new AccountRedpacketPoolModel();
				accountRedpacketPoolMap.put(account_id, model);
//				AccountRedpacketPoolModel temp = accountRedpacketPoolMap.putIfAbsent(account_id, model);
				Date date = new Date();
				model.setAccount_id(account_id);
				model.setCreate_time(date);
				model.setMoney(money);
				model.setOperate_time(date);
				model.setNewAddValue(true);
			} else {
				ReentrantLock reentrantLock = model.getRedisLock();
				reentrantLock.lock();
				try{
					model.setMoney(model.getMoney() + money);
					model.setNeedDB(true);
				}catch (Exception e) {
					logger.error("add money error!", e);
				}finally{
					reentrantLock.unlock();	
				}
			}
			MongoDBServiceImpl.getInstance().addRedpacketPoolLogModel(account_id, ERedpacketPoolType.PUT.getId(),
					money, model.getMoney(),"发放红包到红包池");
		} catch (Exception e) {
			logger.error("add money error!", e);
		}
		return true;
	}
	//定时入库保存红包池
	private void dumpRecommenReceiveModel() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		for (AccountRedpacketPoolModel model : accountRedpacketPoolMap.values()) {
			if (model.isNewAddValue()) {
				publicService.getPublicDAO().insertAccountRedpacketPoolModel(model);
				model.setNewAddValue(false);
			} else if (model.isNeedDB()) {
				publicService.getPublicDAO().updateAccountRedpacketPoolModel(model);
				model.setNeedDB(false);
			}
		}
	}
	
	public void clearRedpacketPool(){
		logger.info("clearRedpacketPool begin!");
		for(AccountRedpacketPoolModel model:accountRedpacketPoolMap.values()){
			ReentrantLock reentrantLock = model.getRedisLock();
			reentrantLock.lock();
			try{
				model.setMoney(0);
				model.setNeedDB(true);
			}catch (Exception e) {
				logger.error("add money error!", e);
			}finally{
				reentrantLock.unlock();	
			}
		}
		logger.info("clearRedpacketPool ok!");
	}

	@Override
	protected void startService() {
		load();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					dumpRecommenReceiveModel();
				} catch (Exception e) {
					logger.error("", e);
				}
			}
		}, 300 * 1000L, 300 * 1000L);//红包池数据5分钟入库一次

	}
	
	@Override
	protected void stopService() {
		dumpRecommenReceiveModel();
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
