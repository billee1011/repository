package com.cai.rmi.handler;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.DbOpType;
import com.cai.common.define.DbStoreType;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.DBUpdateDto;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.NewHallRecommend;
import com.cai.common.domain.RecommendLimitModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.SpringService;
import com.cai.core.DataThreadPool;
import com.cai.core.TaskThreadPool;
import com.cai.dictionary.RecommendLimitDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.runnable.AutoUpdateRecommendLevelRunnble;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RecommenderReceiveService;

/**
 * 
 * 新版闲逸助手返利相关操作
 *
 * @author tang date: 2018年03月23日 上午10:43:45 <br/>
 */
@IRmi(cmd = RMICmd.RECHARGE_RECEIVE, desc = "充值推广员返利相关操作")
public final class RecommendHandler extends IRMIHandler<HashMap<String, String>, Integer> {
	private static Logger logger = LoggerFactory.getLogger(RecommendHandler.class);

	@Override
	protected Integer execute(HashMap<String, String> map) {
		GlobalExecutor.asyn_execute(new Runnable() {
			@Override
			public void run() {
				String accountId = map.get("accountId");
				String moneyStr = map.get("money");
				String orderSeq = map.get("orderSeq");
				String type = map.get("type");
				try {
					long account_id = Long.parseLong(accountId);
					int money = Integer.parseInt(moneyStr);
					if (StringUtils.isNotBlank(type) && type.equals("1")) {
						doPaybackReceived(account_id, money, orderSeq);// 退单
					} else {
						SysParamModel sysParamModel2224 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2224);
						int limitMoney = 30000;
						if (sysParamModel2224 != null && sysParamModel2224.getVal4() > 0) {
							limitMoney = sysParamModel2224.getVal4();
						}
						if (money >= limitMoney) {
							doHallRecommendReceived(account_id, money, orderSeq);
						}
					}
				} catch (Exception e) {
					logger.error(accountId + "返利/退单出现故障 ", e);
				}
			}
		});
		return 1;
	}

	public void doHallRecommendReceived(long account_id, int money, String orderSeq) {
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null || account.getAccountModel().getIs_agent() == 0) {
			return;
		}
		HallRecommendModel hallRecommendModel = account.getHallRecommendModel();
		if (hallRecommendModel.getAccount_id() == 0) {// 没有推荐人
			// 没有推荐人则只需要将充值记录记录下来，给自己返利也要加上
			if (hallRecommendModel.getRecommend_level() > 0) {
				SysParamModel sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
				if (sysParamModel2251.getVal3() == 1) {
					int percent = getSubAgentPercent(hallRecommendModel.getRecommend_level(), hallRecommendModel.getTarget_account_id());
					double receiveMoney = percent * money / 10000.0;
					doHallRecommendIncome(hallRecommendModel.getTarget_account_id(), receiveMoney, 4, "自己返利",
							EGoldOperateType.AGENT_RECHARGE_RECEIVER, hallRecommendModel.getTarget_account_id(), money,
							hallRecommendModel.getTarget_account_id(), hallRecommendModel.getRecommend_level(),
							hallRecommendModel.getRecommend_level(), percent, orderSeq);
				}
			} else {
				MongoDBServiceImpl.getInstance().log_new_hall_recommend(0, ELogType.agentIncome, "未有上级推广员的代理充值", 0l, 5, null, account_id, money,
						account_id, 0, 0, 0, orderSeq);
			}
			autoUpdateLevel(account_id);
			return;
		}
		// 充值返利
		SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
		long gameId = sysParamModel5000.getVal1();
		SysParamModel sysParamModel2248 = null;
		SysParamModel sysParamModel2249 = null;
		SysParamModel sysParamModel2250 = null;
		SysParamModel sysParamModel2251 = null;
		if (gameId == 6) {
			// 旧时推广系统的返利比
			// 新钻石推广返利比
			sysParamModel2248 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2248);
			// 新黄金推广返利比
			sysParamModel2249 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2249);
			// 新白银推广返利比
			sysParamModel2250 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2250);
			// 被推广的人比推广员比自己的等级高返利比
			sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
			if (sysParamModel2248 == null || sysParamModel2249 == null || sysParamModel2250 == null || sysParamModel2251 == null) {
				return;
			}
		} else {
			return;
		}
		try {
			doReceiveByLevel(hallRecommendModel, money, orderSeq);
			autoUpdateLevel(account_id);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public int getSubAgentPercent(int level, long accountId) {
		RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(accountId);
		if (model != null) {
			return model.getAgent_receive_per();
		}
		SysParamModel sysParamModel2223 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2223);
		if (sysParamModel2223 == null) {
			if (level == 1) {
				return 30;
			} else if (level == 2) {
				return 20;
			} else if (level == 3) {
				return 15;
			} else {
				return 0;
			}
		} else {
			if (level == 1) {
				return sysParamModel2223.getVal1();
			} else if (level == 2) {
				return sysParamModel2223.getVal2();
			} else if (level == 3) {
				return sysParamModel2223.getVal3();
			} else {
				return 0;
			}
		}

	}

	public void doReceiveByLevel(HallRecommendModel hallRecommendModel, int money, String orderSeq) {
		// 上挖两代
		HallRecommendModel level1UpModel = PublicServiceImpl.getInstance().getHallRecommendModel(hallRecommendModel.getAccount_id());
		if (level1UpModel.getRecommend_level() == 0) {
			MongoDBServiceImpl.getInstance().log_new_hall_recommend(0, ELogType.agentIncome, "未有上级返利的代理充值", 0l, 5, null,
					hallRecommendModel.getTarget_account_id(), money, hallRecommendModel.getTarget_account_id(), 0, 0, 0, orderSeq);
			return;
		}
		HallRecommendModel level2UpModel = null;
		if (hallRecommendModel.getRecommend_level() == 0) {// 直属代理、见习推广员
			// 直属代理上挖两代
			if (level1UpModel.getRecommend_level() > 0) {
				int percent = getSubAgentPercent(level1UpModel.getRecommend_level(), level1UpModel.getTarget_account_id());
				double receiveMoney = percent * money / 10000.0;
				doHallRecommendIncome(level1UpModel.getTarget_account_id(), receiveMoney, 0, "直属代理", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						hallRecommendModel.getTarget_account_id(), money, hallRecommendModel.getTarget_account_id(),
						level1UpModel.getRecommend_level(), 0, percent, orderSeq);
				if (SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251).getVal4() == 1
						&& level1UpModel.getRecommend_level() != 1) {
					autoUpdateLevel(level1UpModel.getTarget_account_id());
				}
			} else {
				return;
			}
			if (level1UpModel.getAccount_id() > 0) {
				level2UpModel = PublicServiceImpl.getInstance().getHallRecommendModel(level1UpModel.getAccount_id());
			} else {
				return;
			}
			if (level2UpModel != null && level2UpModel.getRecommend_level() > 0) {
				int percent = getReceivePercentBySubUpLevel(level1UpModel.getRecommend_level(), level2UpModel.getRecommend_level(), 2,
						level2UpModel.getTarget_account_id(), true);
				if (percent == 0) {
					return;
				}
				double receiveMoney = percent * money / 10000.0;
				doHallRecommendIncome(level2UpModel.getTarget_account_id(), receiveMoney, 2, "下级-直属", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						hallRecommendModel.getTarget_account_id(), money, level1UpModel.getTarget_account_id(), level2UpModel.getRecommend_level(), 0,
						percent, orderSeq);
				if (SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251).getVal4() == 1
						&& level2UpModel.getRecommend_level() != 1) {
					autoUpdateLevel(level2UpModel.getTarget_account_id());
				}
			} else {
				return;
			}
		} else if (hallRecommendModel.getRecommend_level() == 1) {// 钻石推广员
			SysParamModel sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
			if (sysParamModel2251.getVal3() == 1) {
				int percent = getSubAgentPercent(hallRecommendModel.getRecommend_level(), hallRecommendModel.getTarget_account_id());
				double receiveMoney = percent * money / 10000.0;
				doHallRecommendIncome(hallRecommendModel.getTarget_account_id(), receiveMoney, 4, "自己返利", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						hallRecommendModel.getTarget_account_id(), money, hallRecommendModel.getTarget_account_id(),
						hallRecommendModel.getRecommend_level(), hallRecommendModel.getRecommend_level(), percent, orderSeq);
			}
			// todo receive {}推广员本身充值给自己返利
			if (level1UpModel.getRecommend_level() > 0) {
				int percent = getReceivePercentBySubUpLevel(1, level1UpModel.getRecommend_level(), 1, level1UpModel.getTarget_account_id(), false);
				if (percent == 0) {
					return;
				}
				double receiveMoney = percent * money / 10000.0;
				doHallRecommendIncome(level1UpModel.getTarget_account_id(), receiveMoney, 1, "下级推广员", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						hallRecommendModel.getTarget_account_id(), money, hallRecommendModel.getTarget_account_id(),
						hallRecommendModel.getRecommend_level(), hallRecommendModel.getRecommend_level(), percent, orderSeq);
				if (SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251).getVal4() == 1
						&& level1UpModel.getRecommend_level() != 1) {
					autoUpdateLevel(level1UpModel.getTarget_account_id());
				}
			}
		} else if (hallRecommendModel.getRecommend_level() == 2 || hallRecommendModel.getRecommend_level() == 3) {// 黄金推广员
			// 推广员本身充值给自己返利
			SysParamModel sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
			if (sysParamModel2251.getVal3() == 1) {
				int percent = getSubAgentPercent(hallRecommendModel.getRecommend_level(), hallRecommendModel.getTarget_account_id());
				double receiveMoney = percent * money / 10000.0;
				doHallRecommendIncome(hallRecommendModel.getTarget_account_id(), receiveMoney, 4, "自己返利", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						hallRecommendModel.getTarget_account_id(), money, hallRecommendModel.getTarget_account_id(),
						hallRecommendModel.getRecommend_level(), hallRecommendModel.getRecommend_level(), percent, orderSeq);
				if (SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251).getVal4() == 1) {
					autoUpdateLevel(hallRecommendModel.getTarget_account_id());
				}
			}
			// 特殊情況特殊处理，当下级的级别比推广员的级别要高
			// todo
			if (level1UpModel.getRecommend_level() > 0) {
				int percent = getReceivePercentBySubUpLevel(hallRecommendModel.getRecommend_level(), level1UpModel.getRecommend_level(), 1,
						level1UpModel.getTarget_account_id(), false);
				if (percent == 0) {
					return;
				}
				double receiveMoney = percent * money / 10000.0;
				doHallRecommendIncome(level1UpModel.getTarget_account_id(), receiveMoney, 1, "下级推广员", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						hallRecommendModel.getTarget_account_id(), money, hallRecommendModel.getTarget_account_id(),
						level1UpModel.getRecommend_level(), hallRecommendModel.getRecommend_level(), percent, orderSeq);
				if (SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251).getVal4() == 1
						&& level1UpModel.getRecommend_level() != 1) {
					autoUpdateLevel(level1UpModel.getTarget_account_id());
				}
			} else {
				return;
			}
			if (level1UpModel.getAccount_id() > 0) {
				level2UpModel = PublicServiceImpl.getInstance().getHallRecommendModel(level1UpModel.getAccount_id());
			} else {
				return;
			}
			if (level2UpModel != null && level2UpModel.getRecommend_level() > 0) {
				int percent = 0;
				if (level2UpModel.getRecommend_level() >= level1UpModel.getRecommend_level()) {
					percent = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251).getVal2();
				} else {
					percent = getReceivePercentBySubUpLevel(hallRecommendModel.getRecommend_level(), level2UpModel.getRecommend_level(), 2,
							level2UpModel.getTarget_account_id(), false);
				}
				if (percent == 0) {
					return;
				}
				double receiveMoney = percent * money / 10000.0;
				doHallRecommendIncome(level2UpModel.getTarget_account_id(), receiveMoney, 3, "下级-下级", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						hallRecommendModel.getTarget_account_id(), money, level1UpModel.getTarget_account_id(), level2UpModel.getRecommend_level(),
						hallRecommendModel.getRecommend_level(), percent, orderSeq);
				if (SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251).getVal4() == 1
						&& level2UpModel.getRecommend_level() != 1) {
					autoUpdateLevel(level2UpModel.getTarget_account_id());
				}
			}
		}
	}

	// 根据推广员的上下级返回返利比,gen隔几代
	public int getReceivePercentBySubUpLevel(int sub, int up, int gen, long accountId, boolean isProxy) {
		RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(accountId);
		if (model != null) {
			if (gen == 1) {
				if (isProxy) {
					return model.getTwo_gen_agent_receive_per();
				}
				return model.getRecom_receive_per();
			} else if (gen == 2) {
				if (isProxy) {
					return model.getTwo_gen_agent_receive_per();
				} else {
					return model.getTwo_gen_recom_receive_per();
				}
			}
		}
		SysParamModel sysParamModel2248 = null;
		SysParamModel sysParamModel2249 = null;
		SysParamModel sysParamModel2250 = null;
		SysParamModel sysParamModel2251 = null;
		// 新钻石推广返利比
		sysParamModel2248 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2248);
		// 新黄金推广返利比
		sysParamModel2249 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2249);
		// 新白银推广返利比
		sysParamModel2250 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2250);
		// 被推广的人比推广员比自己的等级高返利比
		sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
		if (sysParamModel2248 == null || sysParamModel2249 == null || sysParamModel2250 == null || sysParamModel2251 == null) {
			return 0;
		}
		if (gen == 1) {
			if (sub > up) {
				switch (up) {
				case 1:
					return sysParamModel2248.getVal1();
				case 2:
					return sysParamModel2249.getVal1();
				case 3:
					return sysParamModel2250.getVal1();
				default:
					return 0;
				}
			} else {
				return sysParamModel2251.getVal1();// 当下级的级别大于或者等于自己的时候，那么下级和下下级的返利按
			}
		} else if (gen == 2) {
			if (sub == 1) {
				return 0;// 隔了两代，下下级为钻石，不给返利
			}
			if (sub > up) {
				switch (up) {
				case 1:
					return sysParamModel2248.getVal2();
				case 2:
					return sysParamModel2249.getVal2();
				case 3:
					return sysParamModel2250.getVal2();
				default:
					return 0;
				}
			} else {
				return sysParamModel2251.getVal2();
			}
		} else {
			return 0;
		}
	}

	public AccountParamModel getAccountParamModel(Account account, EAccountParamType eAccountParamType) {
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(eAccountParamType.getId());
		if (accountParamModel == null) {
			accountParamModel = new AccountParamModel();
			accountParamModel.setAccount_id(account.getAccount_id());
			accountParamModel.setType(eAccountParamType.getId());
			account.getAccountParamModelMap().put(eAccountParamType.getId(), accountParamModel);
		}
		return accountParamModel;
	}

	/**
	 * 钻石推广员收益操作
	 * 
	 * @param account_id
	 * @param income
	 * @param level
	 * @param desc
	 * @param eGoldOperateType
	 * @param targetId
	 * @return
	 */
	public AddGoldResultModel doHallRecommendIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType,
			long targetId, long rechargeMoney, long sourceId, int recommend_level, int my_level, int receive_percent, String orderSeq) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			addGoldResultModel.setAccountModel(accountModel);
			double oldValue = accountModel.getRecommend_remain_income();
			if (income > 0) {
				// 返利单独记录用来作为晋升跟降级的依据
				RecommenderReceiveService.getInstance().addReceive(account, income);
				accountModel.setRecommend_remain_income(accountModel.getRecommend_remain_income() + income);
				accountModel.setRecommend_history_income(accountModel.getRecommend_history_income() + income);
				addGoldResultModel.setSuccess(true);
			} else {
				if (income < 0) {
					if (accountModel.getIs_rebate() != 1) {
						addGoldResultModel.setMsg("您未开通提现功能");
						return addGoldResultModel;
					}
				}
				double k = accountModel.getRecommend_remain_income() + income;
				if (k < 0) {
					addGoldResultModel.setMsg("提现的金额不能大于余额");
					return addGoldResultModel;
				} else {
					accountModel.setRecommend_remain_income(k);
				}
				accountModel.setRecommend_receive_income(accountModel.getRecommend_receive_income() - income);
				addGoldResultModel.setSuccess(true);
			}

			// 现金操作操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			double change = (accountModel.getRecommend_remain_income() - oldValue) * 100;// 元换成分
			double newValue = accountModel.getRecommend_remain_income() * 100;
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_new_hall_recommend(account_id, ELogType.agentIncome, desc, (long) change, level, null, targetId,
						rechargeMoney, sourceId, recommend_level, my_level, receive_percent, orderSeq);
			} else {
				buf.append("减少[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_new_hall_recommend(account_id, ELogType.agentOutcome, desc, (long) change, level, null, targetId,
						rechargeMoney, sourceId, recommend_level, my_level, receive_percent, orderSeq);
			}
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}
		return addGoldResultModel;
	}

	// 返单退利
	@SuppressWarnings("unchecked")
	private void doPaybackReceived(long account_id, int money, String orderSeq) {
		if (StringUtils.isBlank(orderSeq)) {
			logger.error(account_id + "退单,没带订单号过来，无法退返利" + money);
			return;
		}

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null || account.getAccountModel().getIs_agent() == 0) {
			return;
		}
		try {
			List<NewHallRecommend> list = MongoDBServiceImpl.getInstance().queryNewHallRecommendListByOrderSeq(orderSeq);
			for (NewHallRecommend model : list) {
				// 是否已经退单了
				if (model.getLog_type().equals(ELogType.agentPayback.getId())) {
					continue;
				}
				model.setLog_type(ELogType.agentPayback.getId());
				model.setMsg(account_id + "退单" + money);
				model.setV1(model.getV1() * -1);
				doHallRecommendRollback(model.getAccount_id(), model.getV1());
			}
			MongoDBServiceImpl.getInstance().updateNewHallRecommendListByRollback(list);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void doHallRecommendRollback(long account_id, long money) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(account_id);
		if (account == null) {
			logger.error(account_id + "要退" + money + "分,但账号已经不存在");
			return;
		}
		if (money > 0) {
			money = -money;
		}
		double outCome = money / 100.00;
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			double k = accountModel.getRecommend_remain_income() + outCome;
			accountModel.setRecommend_remain_income(k);
			accountModel.setRecommend_receive_income(accountModel.getRecommend_receive_income() - outCome);
			// 现金操作操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
		} catch (Exception e) {
			logger.error("doHallRecommendRollback error", e);
		} finally {
			lock.unlock();
		}
	}

	public static final FastDateFormat ISO_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

	public static Map<Integer, String> getTimeByType(int type) {
		Map<Integer, String> map = new HashMap<>();
		String first = null;
		String end = null;
		if (type == 1) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MONTH, 0);
			c.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			first = ISO_DATE_FORMAT.format(c.getTime());
			end = ISO_DATE_FORMAT.format(new Date());
		} else if (type == 2) {
			Calendar cal_1 = Calendar.getInstance();// 获取当前日期
			cal_1.add(Calendar.MONTH, -1);
			cal_1.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			// 获取前月的最后一天
			Calendar cale = Calendar.getInstance();
			cale.set(Calendar.DAY_OF_MONTH, 0);// 设置为1号,当前日期既为本月第一天
			first = ISO_DATE_FORMAT.format(cal_1.getTime());
			end = ISO_DATE_FORMAT.format(cale.getTime());
		} else {
			Calendar currCal = Calendar.getInstance();
			int currentYear = currCal.get(Calendar.YEAR);
			currCal.clear();
			currCal.set(Calendar.YEAR, currentYear);
			first = ISO_DATE_FORMAT.format(currCal.getTime());
			end = ISO_DATE_FORMAT.format(new Date());
		}
		map.put(1, first);
		map.put(2, end);
		return map;
	}

	public static Map<Integer, Date> getDateByType(int type) {
		Map<Integer, Date> map = new HashMap<>();
		Date first = null;
		Date end = null;
		if (type == 1) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MONTH, 0);
			c.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			first = c.getTime();
			end = new Date();
		} else if (type == 2) {
			Calendar cal_1 = Calendar.getInstance();// 获取当前日期
			cal_1.add(Calendar.MONTH, -1);
			cal_1.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			// 获取前月的最后一天
			Calendar cale = Calendar.getInstance();
			cale.set(Calendar.DAY_OF_MONTH, 0);// 设置为1号,当前日期既为本月第一天
			first = cal_1.getTime();
			end = cale.getTime();
		} else {
			Calendar currCal = Calendar.getInstance();
			int currentYear = currCal.get(Calendar.YEAR);
			currCal.clear();
			currCal.set(Calendar.YEAR, currentYear);
			first = currCal.getTime();
			end = new Date();
		}
		map.put(1, first);
		map.put(2, end);
		return map;
	}

	public void autoUpdateLevel(long account_id) {

		AutoUpdateRecommendLevelRunnble runnlble = new AutoUpdateRecommendLevelRunnble(account_id);
		TaskThreadPool.getInstance().addTask(runnlble);

	}
}
