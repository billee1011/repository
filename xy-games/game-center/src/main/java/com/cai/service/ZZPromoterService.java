package com.cai.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cai.common.domain.Account;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.Event;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.zhuzhou.AccountPromoterReceiveModel;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.domain.zhuzhou.IndexModel;
import com.cai.common.domain.zhuzhou.RankModel;
import com.cai.common.domain.zhuzhou.RechargeRankModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.google.common.collect.Maps;

/**
 * 株洲协会专享推广
 * 
 * @author run
 *
 */
@Service
public class ZZPromoterService extends AbstractService {

	private Map<Long, Map<Long, AccountZZPromoterModel>> promoterMap = Maps.newConcurrentMap();
	private Map<Long, AccountPromoterReceiveModel> promoterReceiveMap = Maps.newConcurrentMap();
	private static ZZPromoterService instance = null;
	// private final Timer timer;

	public static ZZPromoterService getInstance() {
		if (null == instance) {
			instance = new ZZPromoterService();

		}
		return instance;
	}

	private ZZPromoterService() {
		// timer = new Timer("ZZPromoterService-Timer");
	}

	private static Logger logger = LoggerFactory.getLogger(ZZPromoterService.class);

	// 定时计算充值榜
	public void taskJob() {
		try {

		} catch (Exception e) {
			logger.error("taskJob error!", e);
		}
	}

	public void load() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<AccountPromoterReceiveModel> prList = publicService.getPublicDAO().getAccountPromoterReceiveModel();
		for (AccountPromoterReceiveModel model : prList) {
			promoterReceiveMap.put(model.getAccount_id(), model);
		}
		List<AccountZZPromoterModel> plist = publicService.getPublicDAO().getAccountZZPromoterModel();
		for (AccountZZPromoterModel model : plist) {
			if (promoterMap.containsKey(model.getAccount_id())) {
				Map<Long, AccountZZPromoterModel> map = promoterMap.get(model.getAccount_id());
				map.put(model.getTarget_id(), model);
			} else {
				Map<Long, AccountZZPromoterModel> map = Maps.newConcurrentMap();
				map.put(model.getTarget_id(), model);
				promoterMap.put(model.getAccount_id(), map);
			}
		}
		logger.info("load ZZPromoterService success!");
	}

	Map<String, Integer> addMap = Maps.newConcurrentMap();

	public boolean addPromoterObject(long accountId, long targetId) {
		if (accountId == targetId) {
			return false;
		}
		Map<Long, AccountZZPromoterModel> map = promoterMap.get(accountId);
		if (map != null && map.get(targetId) != null) {
			return false;
		}
		SysParamModel sysParamModel2274 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2274);
		if (sysParamModel2274 == null || sysParamModel2274.getVal1() == 0 || StringUtils.isBlank(sysParamModel2274.getStr1())) {
			return false;
		} else {
			String ids = sysParamModel2274.getStr1();
			if (ids.equals("0")) {
				return false;
			}
			String[] idArray = ids.split(",");
			boolean add = false;
			for (String id : idArray) {
				if (Long.parseLong(id) == accountId) {
					add = true;
					break;
				}
			}
			if (!add) {
				return false;
			}
		}

		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetId);
		if (targetAccount == null) {
			return false;
		}
		if (targetAccount.getHallRecommendModel().getRecommend_level() > 0 || targetAccount.getHallRecommendModel().getAccount_id() > 0) {
			return false;// 已经填写了闲逸助手推广员，就不能再填写麻将协会的推广员
		}

		try {
			AccountSimple as = PublicServiceImpl.getInstance().getAccountSimpe(targetId);
			AccountZZPromoterModel model = new AccountZZPromoterModel();
			model.setAccount_id(accountId);
			model.setTarget_id(targetId);
			model.setLevel(targetAccount.getAccountModel().getIs_agent());
			model.setCreate_time(new Date());
			model.setIcon(as.getIcon());
			model.setNick(as.getNick_name());
			SpringService.getBean(PublicService.class).getPublicDAO().insertAccountZZPromoterModel(model);
			if (map != null) {
				map.put(model.getTarget_id(), model);
			} else {
				map = Maps.newConcurrentMap();
				map.put(model.getTarget_id(), model);
				promoterMap.put(model.getAccount_id(), map);
			}
		} catch (Exception e) {
			logger.error("addPromoterObject error", e);
			e.printStackTrace();
		}

		return true;
	}

	public void removePromoterObject(long accountId, long targetId) {
		Map<Long, AccountZZPromoterModel> map = promoterMap.get(accountId);
		if (map == null) {
			return;
		}
		AccountZZPromoterModel model = map.remove(targetId);
		if (model == null) {
			return;
		}
		SpringService.getBean(PublicService.class).getPublicDAO().deleteAccountZZPromoterModel(model);
	}

	public void recharge(int rechargeType, int rechargeMoney, long accountId, long targetId, String orderId) {
		if (addMap.containsKey(orderId)) {
			return;// 当前订单已经返利过了
		}
		Map<Long, AccountZZPromoterModel> map = promoterMap.get(accountId);
		if (map == null) {
			return;
		}
		AccountZZPromoterModel pmodel = map.get(targetId);
		if (pmodel == null || pmodel.getAccount_id() != accountId) {// 推广关系不存在
			return;
		}
		SysParamModel sysParamModel2274 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2274);
		if (sysParamModel2274 == null || sysParamModel2274.getVal1() == 0) {
			return;// 返利开关关闭
		}
		int percent = 0;
		if (rechargeType == 1) {// 1游戏商城微信，2游戏商城ios，3公众号
			percent = sysParamModel2274.getVal2();
		} else if (rechargeType == 2) {
			percent = sysParamModel2274.getVal3();
		} else if (rechargeType == 3) {
			percent = sysParamModel2274.getVal4();
		} else {
			percent = sysParamModel2274.getVal5();
		}
		int receive = rechargeMoney * percent / 100;
		AccountPromoterReceiveModel model = promoterReceiveMap.get(accountId);
		if (model == null) {
			model = new AccountPromoterReceiveModel(accountId);
			model.setNewAddValue(true);
			promoterReceiveMap.put(accountId, model);
		}
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetId);
		if (targetAccount == null) {
			return;
		}
		ReentrantLock lock = model.getRedisLock();
		lock.lock();
		try {
			addMap.put(orderId, 1);
			model.setHistory_money(model.getHistory_money() + receive);
			model.setRemain_money(model.getRemain_money() + receive);
			if (model.isNewAddValue()) {
				model.setNewAddValue(false);
				model.setCreate_time(new Date());
				SpringService.getBean(PublicService.class).getPublicDAO().insertAccountPromoterReceiveModel(model);
			} else {
				SpringService.getBean(PublicService.class).getPublicDAO().updateAccountPromoterReceiveModel(model);
			}
			MongoDBServiceImpl.getInstance().saveRechargeRecord(accountId, targetId, rechargeType, receive, rechargeMoney, 0, orderId, percent, "充值");
		} catch (Exception e) {
			logger.error(accountId + " add promoter receive money error", e);
		} finally {
			lock.unlock();
		}
	}

	public long getRemainMoney(long accountId) {
		AccountPromoterReceiveModel model = promoterReceiveMap.get(accountId);
		if (model == null) {
			return 0;// 余额不足
		} else {
			return model.getRemain_money();
		}
	}

	// 提现
	public int drawCash(long accountId, int money, String desc) {
		AccountPromoterReceiveModel model = promoterReceiveMap.get(accountId);
		if (model == null || model.getRemain_money() < money) {
			return -1;// 余额不足
		}
		if (money < 0) {
			money = -money;
		}
		ReentrantLock lock = model.getRedisLock();
		lock.lock();
		try {
			if (model.getRemain_money() < money) {
				return -1;// 余额不足
			}
			model.setRemain_money(model.getRemain_money() - money);
			model.setDraw_money(model.getDraw_money() + money);
			SpringService.getBean(PublicService.class).getPublicDAO().updateAccountPromoterReceiveModel(model);
			MongoDBServiceImpl.getInstance().saveRechargeRecord(accountId, 0, 0, -money, 0, 1, "", 0, StringUtils.isBlank(desc) ? "提现" : desc);
			return 1;// 扣款成功
		} catch (Exception e) {
			logger.error(accountId + " drawCash  error " + money, e);
		} finally {
			lock.unlock();
		}
		return -1;
	}

	public int rollBackOrder(long accountId, int money, String orderId) {
		AccountPromoterReceiveModel model = promoterReceiveMap.get(accountId);
		if (model == null) {
			return -1;// 余额不足
		}
		ReentrantLock lock = model.getRedisLock();
		lock.lock();
		try {
			model.setRemain_money(model.getRemain_money() - money);
			model.setDraw_money(model.getDraw_money() + money);
			SpringService.getBean(PublicService.class).getPublicDAO().updateAccountPromoterReceiveModel(model);
			MongoDBServiceImpl.getInstance().saveRechargeRecord(accountId, 0, 0, -money, 0, 2, "", 0, "退单");
			return 1;// 扣款成功
		} catch (Exception e) {
			logger.error(accountId + " drawCash  error " + money, e);
		} finally {
			lock.unlock();
		}
		return -1;
	}

	private Map<Long, IndexModel> indexMap = Maps.newConcurrentMap();

	public IndexModel getIndex(long accountId) {
		SysParamModel sysParamModel2274 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2274);
		if (sysParamModel2274 == null || sysParamModel2274.getVal1() == 0 || StringUtils.isBlank(sysParamModel2274.getStr1())) {
			return null;
		} else {
			String ids = sysParamModel2274.getStr1();
			if (ids.equals("0")) {
				return null;
			}
			String[] idArray = ids.split(",");
			boolean add = false;
			for (String id : idArray) {
				if (Long.parseLong(id) == accountId) {
					add = true;
					break;
				}
			}
			if (!add) {
				return null;
			}
		}

		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		if (account == null) {
			return null;
		}
		IndexModel index = indexMap.get(accountId);
		if (index == null) {
			index = new IndexModel();
			indexMap.put(accountId, index);
			AccountSimple as = PublicServiceImpl.getInstance().getAccountSimpe(accountId);
			index.setAccountId(accountId);
			index.setIcon(as.getIcon());
			index.setNick(as.getNick_name());
		}
		AccountPromoterReceiveModel model = promoterReceiveMap.get(accountId);
		if (model == null) {
			index.setHistoryIncome(0);
			index.setRemainIncome(0);
			index.setHistoryDrawCash(0);
		} else {
			index.setHistoryIncome(model.getHistory_money());
			index.setRemainIncome(model.getRemain_money());
			index.setHistoryDrawCash(model.getDraw_money());
		}
		if (index.getUpdateTime() == null || !MyDateUtil.isSameDay(index.getUpdateTime())) {
			Date date = new Date();
			Date start = MyDateUtil.getYesterdayZeroDate(date.getTime());
			Date end = MyDateUtil.getZeroDate(date);
			Integer num = SpringService.getBean(PublicService.class).getPublicDAO().getZZPromoterNum(start, end, accountId);
			index.setYesterdayBind(num != null ? num : 0);
			MongoDBServiceImpl.getInstance().getPromoterData(index, start, end);
		}
		Map<Long, AccountZZPromoterModel> map = promoterMap.get(accountId);
		if (map == null) {
			index.setTotalBind(0);
		} else {
			index.setTotalBind(map.size());
		}

		return index;
	}

	private Map<Long, Map<Integer, RankModel>> rankMap = Maps.newConcurrentMap();

	public Set<RechargeRankModel> getRankByType(long accountId, int type) {
		Map<Integer, RankModel> rank = rankMap.get(accountId);
		if (rank == null) {
			rank = new HashMap<>();
			rankMap.put(accountId, rank);
		}
		if (type == 1) {
			RankModel model = rank.get(1);
			if (model == null || !MyDateUtil.isSameDay(model.getUpdateDate())) {
				model = new RankModel();
				Date now = new Date();
				TreeSet<RechargeRankModel> rankSet = initRank(accountId, null, now);
				model.setUpdateDate(now);
				model.setRankSet(rankSet);
				rank.put(1, model);
			}
		} else if (type == 2) {
			RankModel model = rank.get(2);
			if (model == null || !MyDateUtil.isSameDay(model.getUpdateDate())) {
				model = new RankModel();
				Date now = new Date();
				Date start = MyDateUtil.getNowMonthFirstDay();
				TreeSet<RechargeRankModel> rankSet = initRank(accountId, start, now);
				model.setUpdateDate(now);
				model.setRankSet(rankSet);
				rank.put(2, model);
			}
		} else if (type == 3) {
			RankModel model = rank.get(3);
			if (model == null || !MyDateUtil.isSameDay(model.getUpdateDate())) {
				model = new RankModel();
				Date now = new Date();
				Date start = MyDateUtil.getYesterdayZeroDate(new Date().getTime());
				TreeSet<RechargeRankModel> rankSet = initRank(accountId, start, MyDateUtil.getZeroDate(now));
				model.setUpdateDate(now);
				model.setRankSet(rankSet);
				rank.put(3, model);
			}
		}
		return rank.get(type).getRankSet();
	}

	public TreeSet<RechargeRankModel> initRank(long accountId, Date start, Date now) {
		TreeSet<RechargeRankModel> rankSet = MongoDBServiceImpl.getInstance().getPromoteRechargeRank(accountId, start, now);
		return rankSet;
	}

	public Map<Long, AccountZZPromoterModel> getPromoterMap(long accountId) {
		return promoterMap.get(accountId);
	}

	public AccountZZPromoterModel getAccountZZPromoterModel(long targetId) {
		for (Map<Long, AccountZZPromoterModel> c : promoterMap.values()) {
			if (c.containsKey(targetId)) {
				return c.get(targetId);
			}
		}
		return null;
	}

	@Override
	protected void startService() {
		try {
			load();
		} catch (Exception e) {
			logger.error("ZZPromoterService startService error", e);
			e.printStackTrace();
		}
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
}
