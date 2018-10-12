package com.cai.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Event;
import com.cai.common.domain.WeiXinProxyConsumeModel;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.cai.future.GameSchedule;

/**
 * 统计代理的消耗(供微信公众号使用)
 * 
 * @author wuhaoran
 */
public class WeiXinProxyConsumeService extends AbstractService {

	private static Logger logger = LoggerFactory.getLogger(WeiXinProxyConsumeService.class);

	private static ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> proxyConsumeMap = new ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>>();

	private static ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> backupMap = new ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>>();

	private static WeiXinProxyConsumeService instance;

	private static int type = 0; // 用于0点切换map

	public WeiXinProxyConsumeService() {

	}

	public static WeiXinProxyConsumeService getInstance() {
		if (null == instance) {
			instance = new WeiXinProxyConsumeService();
		}
		return instance;
	}

	// 0点切换
	public void zeroTask() {
		if (type == 0) {
			saveProxyConsumeDataInDB(0);
			type = 1;
		} else if (type == 1) {
			saveProxyConsumeDataInDB(1);
			type = 0;
		}
	}

	/**
	 * 记录代理的消耗
	 * 
	 * @param accountId
	 * @param gameTypeIndex
	 * @param brand
	 * @param gold
	 */
	public void addProxyConsumeStatistics(long accountId, int gameTypeIndex, int brand, int gold, int exclusiveGold) {
		ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> dataMap = null;
		ConcurrentHashMap<Integer, WeiXinProxyConsumeModel> currentChildMap = null;
		try {
			if (type == 0) {
				dataMap = proxyConsumeMap;
			} else {
				dataMap = backupMap;
			}
			if (dataMap.containsKey(accountId)) {
				currentChildMap = dataMap.get(accountId);
				WeiXinProxyConsumeModel currentModel = null;
				if (currentChildMap.containsKey(gameTypeIndex)) {
					currentModel = currentChildMap.get(gameTypeIndex);
					currentModel.setBrand(currentModel.getBrand() + brand);
					currentModel.setGold_count(currentModel.getGold_count() + gold);
					currentModel.setExclusive_gold(currentModel.getExclusive_gold() + exclusiveGold);
				} else {
					currentModel = new WeiXinProxyConsumeModel();
					currentModel.setAccount_id(accountId);
					currentModel.setGame_type_index(gameTypeIndex);
					currentModel.setBrand(brand);
					currentModel.setGold_count(gold);
					currentModel.setExclusive_gold(exclusiveGold);
					currentChildMap.put(gameTypeIndex, currentModel);
				}
			} else {
				currentChildMap = new ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>();
				WeiXinProxyConsumeModel addModel = new WeiXinProxyConsumeModel();
				addModel.setAccount_id(accountId);
				addModel.setGame_type_index(gameTypeIndex);
				addModel.setBrand(brand);
				addModel.setGold_count(gold);
				addModel.setExclusive_gold(exclusiveGold);
				currentChildMap.put(gameTypeIndex, addModel);
				dataMap.put(accountId, currentChildMap);
			}
		} catch (Exception e) {
			logger.error("proxyConsumeStatistics error", e);
		}
	}

	// 统计数据入库
	public void saveProxyConsumeDataInDB(int type) {
		try {
			GameSchedule.put(new Runnable() {
				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					if (type == 0) {
						MongoDBServiceImpl.getInstance().addProxyConsumeInMongo(proxyConsumeMap);
						proxyConsumeMap.clear();
					} else {
						MongoDBServiceImpl.getInstance().addProxyConsumeInMongo(backupMap);
						backupMap.clear();
					}
				}
			}, 3 * 60 * 60 * 1000, TimeUnit.MILLISECONDS); // 凌晨3点执行
		} catch (Exception e) {
			logger.error("saveProxyCounsume into mongo error", e);
		}
	}

	// 获取当天0点的时间
	public int getZeroDate() {
		int notesDate = 0;
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			calendar.set(Calendar.HOUR_OF_DAY, -1);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			Date zero = calendar.getTime();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			notesDate = Integer.valueOf(sdf.format(zero));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return notesDate;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void startService() {
		proxyConsumeMap = MongoDBServiceImpl.getInstance().getTempProxyConsume();
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

	@Override
	protected void stopService() {
		dumpModel();
	}

	@SuppressWarnings("unchecked")
	public void dumpModel() {
		try {
			if (proxyConsumeMap.size() > 0) {
				MongoDBServiceImpl.getInstance().addTempProxyConsumeInMongo(proxyConsumeMap);
				proxyConsumeMap.clear();
			} else {
				MongoDBServiceImpl.getInstance().addTempProxyConsumeInMongo(backupMap);
				backupMap.clear();
			}
		} catch (Exception e) {

		}
	}
}
