package com.cai.service;

import java.util.Date;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.SystemLogModel;
import com.cai.core.MongoDBUpdateThreadPool;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.timer.MogoDBTimer;

/**
 * mogodb服务类
 * 
 * @author run
 *
 */
public class MongoDBServiceImpl extends AbstractService {
	
	private static final Logger logger = LoggerFactory.getLogger(MongoDBServiceImpl.class);

	/**
	 * 日志队列,仅用于批量插入
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
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void log(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip) {
		GameLogModel gameLogModel = new GameLogModel();
		gameLogModel.setCreate_time(new Date());
		gameLogModel.setAccount_id(account_id);
		gameLogModel.setLogic_id(SystemConfig.logic_index);
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
	 * 
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void systemLog(ELogType eLogType, String msg, Long v1, Long v2, ESysLogLevelType eSysLogLevelType) {
		SystemLogModel systemLogModel = new SystemLogModel();
		systemLogModel.setCreate_time(new Date());
		systemLogModel.setLogic_id(SystemConfig.logic_index);
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLocal_ip(SystemConfig.localip);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}

	/**
	 * 牌局记录
	 * 
	 * @param game_id
	 *            游戏id
	 * @param brand_id
	 *            牌局id
	 * @param brand_child_id
	 *            牌局子id列表
	 * @param msg
	 *            详细信息
	 * @param v1
	 *            扩展值
	 * @param v2
	 *            扩展值
	 */
	public BrandLogModel parentBrand(int game_id, Long brand_id, String brand_child_id, String msg, Long v1, Long v2,String room_id) {
		BrandLogModel brandLogModel = new BrandLogModel();
		brandLogModel.setCreate_time(new Date());
		brandLogModel.setGame_id(game_id);
		brandLogModel.setLogic_id(SystemConfig.logic_index);
		brandLogModel.setLog_type(ELogType.parentBrand.getId());
		brandLogModel.setBrand_id(brand_id);
		brandLogModel.setBrand_child_id(brand_child_id);
		brandLogModel.setMsg(msg);
		brandLogModel.setV1(v1);
		brandLogModel.setV2(v2);
		brandLogModel.setV3(room_id);//房间号
		brandLogModel.setLocal_ip(SystemConfig.localip);
		logQueue.add(brandLogModel);
		return brandLogModel;
	}
	
	/**
	 * 更新大牌局
	 * @param brandLogModel
	 */
	public void updateParenBrand(BrandLogModel brandLogModel){
		MongoDBUpdateThreadPool.getInstance().addTask(brandLogModel);
	}

	/**
	 * 牌局记录
	 * 
	 * @param game_id
	 *            游戏id
	 * @param brand_id
	 *            牌局id
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public BrandLogModel childBrand(int game_id, Long brand_id, Long brand_parent_id, String msg, Long v1, Long v2,byte video_record[],String room_id) {
		BrandLogModel brandLogModel = new BrandLogModel();
		brandLogModel.setCreate_time(new Date());
		brandLogModel.setGame_id(game_id);
		brandLogModel.setLogic_id(SystemConfig.logic_index);
		brandLogModel.setLog_type(ELogType.childBrand.getId());
		brandLogModel.setBrand_id(brand_id);
		brandLogModel.setBrand_parent_id(brand_parent_id);
		brandLogModel.setMsg(msg);
		brandLogModel.setV1(v1);
		brandLogModel.setV2(v2);
		brandLogModel.setV3(room_id);
		brandLogModel.setLocal_ip(SystemConfig.localip);
		brandLogModel.setVideo_record(video_record);
		logQueue.add(brandLogModel);
		return brandLogModel;
	}

	/**
	 * 玩家牌局索引
	 */
	public BrandLogModel accountBrand(int game_id, long account_id, Long brand_id) {
		BrandLogModel brandLogModel = new BrandLogModel();
		brandLogModel.setCreate_time(new Date());
		brandLogModel.setAccount_id(account_id);
		brandLogModel.setGame_id(game_id);
		brandLogModel.setLogic_id(SystemConfig.logic_index);
		brandLogModel.setLog_type(ELogType.accountBrand.getId());
		brandLogModel.setBrand_id(brand_id);
		brandLogModel.setLocal_ip(SystemConfig.localip);
		logQueue.add(brandLogModel);
		return brandLogModel;
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
