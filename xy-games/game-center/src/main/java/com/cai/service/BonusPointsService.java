/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
/**
 * 
 * 
 * @author wu_hc date: 2017年11月30日 下午5:02:34 <br/>
 */
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cai.common.define.EBonusPointsType;
import com.cai.common.domain.Event;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.bonuspoints.AccountBonusPointsModel;
import com.cai.common.domain.bonuspoints.BonusPointsActivity;
import com.cai.common.domain.bonuspoints.BonusPointsExchangeLog;
import com.cai.common.domain.bonuspoints.BonusPointsGoods;
import com.cai.common.domain.bonuspoints.BonusPointsGoodsType;
import com.cai.common.domain.bonuspoints.ExchangeRankModel;
import com.cai.common.domain.bonuspoints.PlayerAddressModel;
import com.cai.common.util.IDGeneratorOrder;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.dao.PublicDAO;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.google.common.collect.Maps;

/**
 * 
 * 积分商城服务
 *
 * @author tang date: 2018年07月03日 上午12:05:26 <br/>
 */
public class BonusPointsService extends AbstractService {
	private static Logger logger = LoggerFactory.getLogger(BonusPointsService.class);
	private static BonusPointsService instance = new BonusPointsService();
	// 积分
	private ConcurrentMap<Long, AccountBonusPointsModel> accountBonusPointsMap = Maps.newConcurrentMap();
	// 地址
	private ConcurrentMap<Long, PlayerAddressModel> accountAddressMap = Maps.newConcurrentMap();
	// 商品类型
	private Map<Integer, BonusPointsGoodsType> goodsTypeMap = new HashMap<>();
	// 商品列表
	private Map<Integer, BonusPointsGoods> goodsMap = new HashMap<>();

	private Map<Integer, BonusPointsGoods> allGoodsMap = new HashMap<>();

	private Map<Integer, List<BonusPointsGoods>> typeGoodsMap = new HashMap<>();

	private List<BonusPointsGoods> hotGoodsList = new ArrayList<>();
	private List<ExchangeRankModel> rankList = new ArrayList<>();

	private BonusPointsActivity activityModel = null;

	private final Timer timer;

	private BonusPointsService() {
		timer = new Timer("bonusPointsService-Timer");
	}

	public static BonusPointsService getInstance() {
		return instance;
	}

	public AccountBonusPointsModel getAccountBonusPointsModel(long accountId) {
		return accountBonusPointsMap.get(accountId);
	}

	public long getScore(long accountId) {
		if (accountBonusPointsMap.containsKey(accountId)) {
			return accountBonusPointsMap.get(accountId).getScore();
		} else {
			return 0;
		}
	}

	public List<ExchangeRankModel> List = new ArrayList<>();

	public ConcurrentMap<Long, PlayerAddressModel> getAccountAddressMap() {
		return accountAddressMap;
	}

	public Map<Integer, BonusPointsGoodsType> getGoodsTypeMap() {
		return goodsTypeMap;
	}

	public Map<Integer, BonusPointsGoods> getGoodsMap() {
		return goodsMap;
	}

	public List<BonusPointsGoods> getGoodsListByType(int type) {
		return typeGoodsMap.get(type);
	}

	@Override
	protected void startService() {
		try {
			loadScore();
			loadAddress();
			loadGoods();
		} catch (Exception e) {
			logger.error("BonusPointsService startService error", e);
		}
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					dumpBonusPoints();
					dumpGoodsStock();
				} catch (Exception e) {
					logger.error("scheduleAtFixedRate", e);
				}
			}
		}, 60 * 1000L, 60 * 1000L);// 积分与商品库存1分钟入库一次
	}

	public void loadAddress() {
		PublicDAO dao = SpringService.getBean(PublicService.class).getPublicDAO();
		List<PlayerAddressModel> list = dao.getPlayerAddressModelList();
		for (PlayerAddressModel model : list) {
			accountAddressMap.put(model.getAccount_id(), model);
		}
	}

	public void loadScore() {
		PublicDAO dao = SpringService.getBean(PublicService.class).getPublicDAO();
		List<AccountBonusPointsModel> list = dao.getAccountBonusPointsModelList();
		for (AccountBonusPointsModel model : list) {
			accountBonusPointsMap.put(model.getAccount_id(), model);
		}
	}

	public void loadGoods() {
		Map<Integer, BonusPointsGoodsType> goodsTypeMap = new HashMap<>();
		Map<Integer, List<BonusPointsGoods>> typeGoodsMap = new HashMap<>();
		List<BonusPointsGoods> hotGoodsList = new ArrayList<>();
		PublicDAO dao = SpringService.getBean(PublicService.class).getPublicDAO();
		List<BonusPointsActivity> activityList = dao.getBonusPointsActivityList();
		if (activityList != null && activityList.size() > 0) {
			activityModel = activityList.get(0);
		}
		List<BonusPointsGoodsType> goodsTypelist = dao.getBonusPointsGoodsTypeList();
		for (BonusPointsGoodsType typeModel : goodsTypelist) {
			goodsTypeMap.put(typeModel.getId(), typeModel);
			typeGoodsMap.put(typeModel.getId(), new ArrayList<>());
		}
		this.goodsTypeMap = goodsTypeMap;
		Map<Integer, BonusPointsGoods> goodsMap = new HashMap<>();
		// List<BonusPointsGoods> goodslist = dao.getBonusPointsGoodsList();
		List<BonusPointsGoods> goodslist = dao.getAllBonusPointsGoodsList();
		for (BonusPointsGoods model : goodslist) {
			allGoodsMap.put(model.getId(), model);
			if (model.getState() != 1) {
				continue;
			}
			if (typeGoodsMap.containsKey(model.getGoods_type())) {
				typeGoodsMap.get(model.getGoods_type()).add(model);
			}
			if (model.getFlag() > 0) {
				hotGoodsList.add(model);
			}
			goodsMap.put(model.getId(), model);
		}
		this.typeGoodsMap = typeGoodsMap;
		this.goodsMap = goodsMap;
		this.hotGoodsList = hotGoodsList;
	}

	public int exchangeGoods(int goodsId, long accountId, long score, int count, String goodsFormat, StringBuffer sb) {
		score = score > 0 ? score : -score;
		AccountBonusPointsModel accountScore = accountBonusPointsMap.get(accountId);
		if (accountScore == null || accountScore.getScore() < score) {
			sb.append("您的积分不足，暂时无法兑换");
			return -1;// 积分不够，兑换失败
		}

		BonusPointsGoods goods = goodsMap.get(goodsId);
		if (goods == null) {
			sb.append("当前商品已下架，请您重新选择商品兑换吧");
			return -2;// 下单失败，商品不存在或已经下架
		}
		if (goods.getScore() * count != score) {
			sb.append("扣除的积分与兑换商品需要的积分不一致，兑换失败");
			return -3;// 兑换扣除的积分有误
		}
		if (goods.getItemId() == 0) {// 实物，道具id默认0
			PlayerAddressModel playerAddressModel = accountAddressMap.get(accountId);
			if (playerAddressModel == null) {
				sb.append("请先填写收货地址再来兑换");
				return -5;// 请先填写收货地址
			}
		}
		ReentrantLock lock = goods.getRedisLock();
		lock.lock();
		try {
			if (!hasStock(goods, count)) {
				sb.append("该商品库存不足，请您重新选择商品兑换吧");
				return -4;// 库存不足
			}
			// 减去库存
			decStock(goods, count);
		} catch (Exception e) {
			logger.error("dec goods stock error", e);
		} finally {
			lock.unlock();
		}
		lock = accountScore.getRedisLock();
		lock.lock();
		try {
			if (accountScore.getScore() < score) {
				sb.append("您的积分不足，暂时无法兑换");
				return -1;
			}
			accountScore.setScore(accountScore.getScore() - score);
			accountScore.setNeedDB(true);
		} catch (Exception e) {
			logger.error("dec score  error", e);
		} finally {
			lock.unlock();
		}
		// 添加订单记录
		addOrder(accountId, score, accountScore.getScore(), goodsId, count, goodsFormat, goods.getGoods_type(), goods.getItemId());
		return 0;
	}

	/**
	 * 后台sshe增减积分
	 * 
	 * @param accountId
	 * @param score
	 * @param etype
	 * @return
	 */
	public int operateBonusPoints(String accountIds, long score, EBonusPointsType etype, String remark) {
		SysParamModel sysParamModel2266 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2266);
		if (sysParamModel2266 == null || sysParamModel2266.getVal4() != 1) {
			return 0;
		}

		if (StringUtils.isBlank(accountIds)) {
			return 0;
		}
		try {
			String[] accountIdArray = accountIds.split(",");
			if (etype == EBonusPointsType.BACKUP_ADD) {
				score = score > 0 ? score : -score;
				for (String accountId : accountIdArray) {
					long account_id = Long.parseLong(accountId);
					if (PublicServiceImpl.getInstance().getAccount(account_id) == null) {
						continue;
					}
					addBonusPoints(account_id, score);
					addBonusPointsStreamLog(account_id, score, accountBonusPointsMap.get(account_id).getScore(), remark, etype, 0);
				}
			} else if (etype == EBonusPointsType.BACKUP_DEC) {
				score = score > 0 ? -score : score;
				for (String accountId : accountIdArray) {
					long account_id = Long.parseLong(accountId);
					if (PublicServiceImpl.getInstance().getAccount(account_id) == null) {
						continue;
					}
					addBonusPoints(account_id, score);
					addBonusPointsStreamLog(account_id, score, accountBonusPointsMap.get(account_id).getScore(), remark, etype, 0);
				}
			}
		} catch (Exception e) {
			logger.error("operateBonusPoints error", e);
		}

		return 0;
	}

	public void updateAccountId(long oldAccountId, long newAccountId) {
		AccountBonusPointsModel oldModel = accountBonusPointsMap.get(oldAccountId);
		AccountBonusPointsModel newModel = accountBonusPointsMap.get(newAccountId);
		if (oldModel == null && newModel == null) {
			return;
		}
		if (oldModel == null) {
			addBonusPoints(oldAccountId, newModel.getScore());
			addBonusPoints(newAccountId, -newModel.getScore());
			return;
		}
		if (newModel == null) {
			addBonusPoints(oldAccountId, -oldModel.getScore());
			addBonusPoints(newAccountId, oldModel.getScore());
			return;
		}
		long temp = newModel.getScore();
		newModel.setScore(oldModel.getScore());
		newModel.setNeedDB(true);
		oldModel.setScore(temp);
		oldModel.setNeedDB(true);
	}

	// 获取积分倍率
	private int getBonusPointsPercentByAccountId(long accountId) {
		if (activityModel == null || activityModel.getId() == 0) {
			return 100;
		}
		long curTime = System.currentTimeMillis();
		if (activityModel.getBegin_time().getTime() <= curTime && activityModel.getEnd_time().getTime() >= curTime) {
			HallRecommendModel hallRecommendModel = PublicServiceImpl.getInstance().getHallRecommendModel(accountId);
			if (hallRecommendModel.getRecommend_level() == 0) {
				return activityModel.getAgent_rate();
			} else if (hallRecommendModel.getRecommend_level() == 3) {
				return activityModel.getSilver_rate();
			} else if (hallRecommendModel.getRecommend_level() == 2) {
				return activityModel.getGold_rate();
			} else if (hallRecommendModel.getRecommend_level() == 1) {
				return activityModel.getDiamond_rate();
			}
		}
		return 100;
	}

	/**
	 * 积分定时入库
	 */
	private void dumpBonusPoints() {
		try {
			PublicService publicService = SpringService.getBean(PublicService.class);
			for (AccountBonusPointsModel model : accountBonusPointsMap.values()) {
				if (model.isNewAddValue()) {
					publicService.getPublicDAO().insertAccountBonusPointsModel(model);
					model.setNewAddValue(false);
				} else if (model.isNeedDB()) {
					publicService.getPublicDAO().updateAccountBonusPointsModel(model);
					model.setNeedDB(false);
				}
			}
		} catch (Exception e) {
			logger.error("dumpBonusPoints error", e);
		}

	}

	/**
	 * 商品库存定时入库
	 */
	private void dumpGoodsStock() {
		try {
			PublicService publicService = SpringService.getBean(PublicService.class);
			for (BonusPointsGoods model : goodsMap.values()) {
				if (model.isNeedDB()) {
					publicService.getPublicDAO().updateupdateBonusPointsGoodsModel(model);
					model.setNeedDB(false);
				}
			}
		} catch (Exception e) {
			logger.error("dumpGoodsStock error", e);
		}

	}

	// 获取额外赠送的积分
	public int getSendBonusPoints(EBonusPointsType etype) {
		SysParamModel sysParamModel2266 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2266);
		if (sysParamModel2266 == null) {
			return 0;
		}
		if (etype.getId() == EBonusPointsType.FIRST_RECHARGE_BP.getId()) {
			return sysParamModel2266.getVal1();
		} else if (etype.getId() == EBonusPointsType.PROMOTER_OPENAGENT_BP.getId()) {
			return sysParamModel2266.getVal2();
		} else if (etype.getId() == EBonusPointsType.SELF_OPENAGENT_BP.getId()) {
			return sysParamModel2266.getVal3();
		}
		return 0;
	}

	// 充值赠送积分
	public void rechargeSendBonusPoints(long accountId, int money, EBonusPointsType etype) {
		SysParamModel sysParamModel2266 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2266);
		if (sysParamModel2266 == null || sysParamModel2266.getVal4() != 1) {
			return;
		}
		int percent = getBonusPointsPercentByAccountId(accountId);
		long score = money * percent / 10000;
		addBonusPoints(accountId, score);
		addBonusPointsStreamLog(accountId, score, accountBonusPointsMap.get(accountId).getScore(), etype.getName(), etype, money);
		int exattrScore = getSendBonusPoints(etype);
		if (exattrScore > 0) {
			addBonusPoints(accountId, exattrScore);
			addBonusPointsStreamLog(accountId, exattrScore, accountBonusPointsMap.get(accountId).getScore(), "额外赠送积分", etype, 0);
		}
	}

	// 退单返还积分
	public void payBackDecreaseBonusPoints(long accountId, int money) {
		SysParamModel sysParamModel2266 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2266);
		if (sysParamModel2266 == null || sysParamModel2266.getVal4() != 1) {
			return;
		}
		int percent = getBonusPointsPercentByAccountId(accountId);
		long score = money * percent / 10000;
		if (score > 0) {
			score = -score;
		}
		addBonusPoints(accountId, score);
		addBonusPointsStreamLog(accountId, score, accountBonusPointsMap.get(accountId).getScore(), "后台退单扣还积分", EBonusPointsType.PAYBACK_DEC, money);
	}

	// 添加流水日志
	public void addBonusPointsStreamLog(long accountId, long score, long remainScore, String remark, EBonusPointsType etype, int money) {
		try {
			MongoDBServiceImpl.getInstance().addBonusPointsStream(accountId, score, remainScore, remark, etype, money);
		} catch (Exception e) {
			logger.error("addBonusPointsStream error", e);
		}
	}

	// 增减积分
	private void addBonusPoints(long accountId, long score) {
		try {
			AccountBonusPointsModel model = accountBonusPointsMap.get(accountId);
			if (model == null) {
				model = new AccountBonusPointsModel();
				accountBonusPointsMap.put(accountId, model);
				model.setAccount_id(accountId);
				model.setScore(score);
				model.setHistory_score(score);
				model.setNewAddValue(true);
			} else {
				ReentrantLock reentrantLock = model.getRedisLock();
				reentrantLock.lock();
				try {
					model.setScore(model.getScore() + score);
					if (score > 0) {
						model.setHistory_score(model.getHistory_score() + score);
					}
					model.setNeedDB(true);
				} catch (Exception e) {
					logger.error(accountId + "add BonusPoints error!", e);
				} finally {
					reentrantLock.unlock();
				}
			}
		} catch (Exception e) {
			logger.error("add BonusPoints error!", e);
		}
	}

	public List<ExchangeRankModel> getRankList() {
		if (rankTime == 0) {
			rankTask();
		} else {
			if (System.currentTimeMillis() - 60 * 60 * 1000 > rankTime) {
				rankTask();
			}
		}
		return rankList;
	}

	// 发货
	public int updateOrder(String id, String logisticsChannel, String logisticsOrderId, String operator) {
		if (StringUtils.isBlank(logisticsChannel) || StringUtils.isBlank(logisticsChannel)) {
			return -1;
		}
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(id));
		Update update = new Update();
		update.set("logisticsChannel", logisticsChannel);
		update.set("logisticsOrderId", logisticsOrderId);
		update.set("operator", operator);
		update.set("orderState", 1);
		mongoDBService.getMongoTemplate().updateFirst(query, update, BonusPointsExchangeLog.class);
		return 0;
	}

	// 添加订单记录
	private void addOrder(long accountId, long score, long remainScore, int goodsId, int count, String goodsFormat, int goodsType, int itemId) {
		try {
			BonusPointsGoods goods = goodsMap.get(goodsId);
			BonusPointsExchangeLog model = new BonusPointsExchangeLog();
			model.setAccountId(accountId);
			model.setCount(count);
			model.setCreate_time(new Date());
			if (itemId == 0) {
				PlayerAddressModel playerAddressModel = accountAddressMap.get(accountId);
				model.setDeliverAddress(playerAddressModel.getAddress());
				model.setDeliverName(playerAddressModel.getName());
				model.setMobile(playerAddressModel.getPhone() + "");
				model.setOrderState(0);
			} else {
				model.setOrderState(1);
			}
			model.setGoodsFormat(goodsFormat);
			model.setGoodsId(goodsId);
			model.setGoodsName(goods.getGoods_name());
			model.setOrderId(IDGeneratorOrder.getInstance().getUseRedPacketUniqueID());
			model.setRemainScore(remainScore);
			model.setScore(score);
			model.setGoodsType(goodsType);
			MongoDBServiceImpl.getInstance().addExchangeOrder(model);
			addBonusPointsStreamLog(accountId, score, remainScore, "兑换" + goods.getGoods_name() + "*" + count, EBonusPointsType.EXCHANGE_CONSUME_BP,
					0);
		} catch (Exception e) {
			logger.error("addorder error", e);
		}
	}

	private long rankTime = 0;

	public synchronized void rankTask() {
		List<ExchangeRankModel> rankList = new ArrayList<>();
		@SuppressWarnings("unchecked")
		List<Entry<Long, Long>> top10 = MongoDBServiceImpl.getInstance().getExchangeGoodsRank();
		rankTime = System.currentTimeMillis();
		// 排行是空的
		if (top10 == null || top10.size() == 0) {
			rankList.addAll(new ArrayList<ExchangeRankModel>());
		} else {
			int i = 1;
			for (Entry<Long, Long> e : top10) {
				BonusPointsGoods goods = allGoodsMap.get((int) e.getKey().longValue());
				if (goods != null) {
					ExchangeRankModel model = new ExchangeRankModel();
					model.setRank(i);
					model.setExchangeCount(e.getValue());
					model.setGoods(goods);
					rankList.add(model);
					i++;
				}
			}
			this.rankList = rankList;
		}
	}

	// 判断是否还有库存
	private boolean hasStock(BonusPointsGoods goods, int count) {
		if (goods.getStock_type() == 0) {
			if (goods.getStock() < count) {
				return false;
			}
		}
		return true;
	}

	// 下单成功减库存
	private void decStock(BonusPointsGoods goods, int count) {
		if (goods.getStock_type() == 0) {
			goods.setStock(goods.getStock() - count);
			goods.setNeedDB(true);
		}
	}

	public List<BonusPointsGoods> getHotGoodsList() {
		return hotGoodsList;
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	protected void stopService() {
		try {
			dumpBonusPoints();
			dumpGoodsStock();
		} catch (Exception e) {
			logger.error("stopService", e);
		}

	}

	public BonusPointsGoods getGoodsById(int goodsId) {
		return allGoodsMap.get(goodsId);
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
