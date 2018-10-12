package com.cai.service;

import java.util.Date;
import java.util.SortedMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.DbOpType;
import com.cai.common.define.DbStoreType;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountProxyModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AgentRecommendModel;
import com.cai.common.domain.DBUpdateDto;
import com.cai.common.domain.Event;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.WealthUtil;
import com.cai.core.DataThreadPool;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountProxyResponse;
import protobuf.redis.ProtoRedis.RsAccountRecommendResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsCmdResponse;
import protobuf.redis.ProtoRedis.RsHallRecommendResponse;

/**
 * 
 * 钻石代理
 *
 * @author Administrator date: 2017年8月21日 上午10:43:45 <br/>
 */
public class OssModifyService extends AbstractService {

	private static Logger logger = LoggerFactory.getLogger(OssModifyService.class);

	private static OssModifyService instance;

	private OssModifyService() {
	}

	public static OssModifyService getInstance() {
		if (null == instance) {
			instance = new OssModifyService();
		}
		return instance;
	}

	/**
	 * 增减玩家房卡
	 * 
	 * @param account_id
	 * @param gold
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel addAccountGold(long account_id, int gold, boolean isExceed, String desc, EGoldOperateType eGoldOperateType) {

		// logger.info("===========================" + desc + "\t" +
		// eGoldOperateType.getIdstr() + "\t" + account_id);
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			AccountWeixinModel wxModel = account.getAccountWeixinModel();
			addGoldResultModel.setAccountModel(accountModel);
			if (wxModel != null) {
				addGoldResultModel.setWxNickName(wxModel.getNickname());
			}
			long oldValue = accountModel.getGold();

			if (gold > 0) {
				accountModel.setGold(accountModel.getGold() + gold);
				accountModel.setHistory_pay_gold(accountModel.getHistory_pay_gold() + gold);
				addGoldResultModel.setSuccess(true);

				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_ADD_GOLD);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() + gold);
					accountParamModel.setNeedDB(true);
				}
				if (eGoldOperateType.getId() == EGoldOperateType.FAILED_ROOM.getId()) {
					accountModel.setConsum_total(accountModel.getConsum_total() - gold);// gold<0
				}
			} else {
				long k = accountModel.getGold() + gold;
				if (!isExceed) {
					if (k < 0) {
						addGoldResultModel.setMsg("库存不足");
						return addGoldResultModel;
					}
				}

				if (k < 0) {
					accountModel.setGold(0L);
				} else {
					accountModel.setGold(k);
				}
				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_CONSUM_GOLD);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() - gold);
					accountParamModel.setNeedDB(true);
				}
				if (eGoldOperateType.getId() == EGoldOperateType.PROXY_GIVE.getId()
						|| eGoldOperateType.getId() == EGoldOperateType.OPEN_ROOM.getId()) {
					accountModel.setConsum_total(accountModel.getConsum_total() - gold);// gold<0
				}
				addGoldResultModel.setSuccess(true);

			}

			if (EGoldOperateType.OPEN_ROOM.getId() == eGoldOperateType.getId()) {
				accountModel.setNeedDB(true);
			} else {
				// 房卡操作直接入库
				DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			}

			long change = accountModel.getGold() - oldValue;
			long newValue = accountModel.getGold();
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
			} else {
				buf.append("减少[" + change + "]");
				// MongoDBServiceImpl.getInstance().log(account_id,
				// ELogType.addGold, desc, change,
				// (long) EAccountParamType.TODAY_CONSUM_GOLD.getId(), null);
			}
			buf.append(",值变化:[").append(oldValue).append("]->[").append(newValue).append("]");
			desc = desc + buf.toString();
			MongoDBServiceImpl.getInstance().log(account_id, ELogType.addGold, desc, change, (long) eGoldOperateType.getId(), null, oldValue,
					newValue);
			// ========同步到中心========
			// 房间消耗的逻辑服直接同步了，不需要走redis广播
			if (!WealthUtil.roomGoldType.contains(eGoldOperateType)) {
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(account_id);
				//
				RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
				rsAccountModelResponseBuilder.setGold(accountModel.getGold());
				rsAccountModelResponseBuilder.setHistoryPayGold(accountModel.getHistory_pay_gold());
				rsAccountModelResponseBuilder.setConsumTotal(accountModel.getConsum_total());
				rsAccountModelResponseBuilder.setGoldChangeType(eGoldOperateType.getId());
				rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
						ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
			}

		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}
		return addGoldResultModel;
	}

	/**
	 * 新增邀请记录
	 * 
	 * @param accountRecommendModel
	 */
	public boolean addAccountRecommendModel(AccountRecommendModel accountRecommendModel) {
		try {
			long account_id = accountRecommendModel.getAccount_id();
			Account account = PublicServiceImpl.getInstance().getAccount(account_id);
			if (account == null)
				return false;
			account.getRecommendRelativeModel().incre();
			AccountRecommendModel model = account.getAccountRecommendModelMap().get(accountRecommendModel.getTarget_account_id());
			if (model != null) {
				logger.warn("玩家:{} 已经推荐过 玩家:{},不能重复推荐!!", account_id, accountRecommendModel.getTarget_account_id());
				return false;
			}

			if (accountRecommendModel.getTarget_account_id() == account_id) {
				logger.warn("玩家:{} ，不可以推荐自己!!", account_id);
				return false;
			}

			// 入库
			DataThreadPool.getInstance()
					.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.INSERT, "insertAccountRecommendModel", accountRecommendModel));
			account.getAccountRecommendModelMap().put(accountRecommendModel.getTarget_account_id(), accountRecommendModel);

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountRecommendResponse.Builder rsAccountRecommendResponse = RsAccountRecommendResponse.newBuilder();
			rsAccountRecommendResponse.setAccountId(accountRecommendModel.getAccount_id());
			rsAccountRecommendResponse.setTargetAccountId(accountRecommendModel.getTarget_account_id());
			rsAccountRecommendResponse.setCreateTime(accountRecommendModel.getCreate_time().getTime());
			rsAccountRecommendResponse.setGoldNum(accountRecommendModel.getGold_num());
			rsAccountRecommendResponse.setTargetName(accountRecommendModel.getTarget_name());
			rsAccountRecommendResponse.setTargetIcon(accountRecommendModel.getTarget_icon());
			rsAccountResponseBuilder.addRsAccountRecommendResponseList(rsAccountRecommendResponse);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
			return true;

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
	}

	public boolean addHallRecommendModel(HallRecommendModel hallRecommendModel) {
		try {

			long account_id = hallRecommendModel.getAccount_id();
			if (account_id != 0) {
				Account account = PublicServiceImpl.getInstance().getAccount(account_id);
				if (account == null)
					return false;
				if (account.getHallRecommendModelMap() != null) {
					HallRecommendModel model = account.getHallRecommendModelMap().get(hallRecommendModel.getTarget_account_id());
					if (model != null) {
						logger.warn("玩家:{} 已经推荐过 玩家:{},不能重复推荐!!", account_id, hallRecommendModel.getTarget_account_id());
						return false;
					}
				}
				account.getHallRecommendModelMap().put(hallRecommendModel.getTarget_account_id(), hallRecommendModel);
				account.getRecommendRelativeModel().incre();// 推荐人数+1
				// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
				// account_id + "", account);// 更新redis缓存
			}
			if (hallRecommendModel.getTarget_account_id() == account_id) {
				logger.warn("玩家:{} ，不可以推荐自己!!", account_id);
				return false;
			}
			// 入库
			DataThreadPool.getInstance()
					.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.INSERT, "insertHallRecommendModel", hallRecommendModel));
			Account targetAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getTarget_account_id());
			targetAccount.setHallRecommendModel(hallRecommendModel);
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// hallRecommendModel.getTarget_account_id() + "", targetAccount);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(hallRecommendModel.getTarget_account_id());
			RsHallRecommendResponse.Builder rsHallRecommendResponse = RsHallRecommendResponse.newBuilder();
			rsHallRecommendResponse.setAccountId(hallRecommendModel.getAccount_id());
			rsHallRecommendResponse.setTargetAccountId(hallRecommendModel.getTarget_account_id());
			// rsHallRecommendResponse.setCreateTime(hallRecommendModel.getCreate_time().getTime());
			rsHallRecommendResponse.setProxyLevel(hallRecommendModel.getProxy_level());
			rsAccountResponseBuilder.setRsHallRecommendResponse(rsHallRecommendResponse);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + targetAccount.getLastProxyIndex());
			return true;

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
	}

	public boolean updateHallRecommendModel(HallRecommendModel hallRecommendModel) {
		try {
			// 更新
			DataThreadPool.getInstance()
					.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateHallRecommendLevel", hallRecommendModel));
			Account targetAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getTarget_account_id());
			targetAccount.setHallRecommendModel(hallRecommendModel);
			// 更新上级缓存
			if (hallRecommendModel.getAccount_id() != 0) {
				Account upAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
				if (upAccount != null) {
					upAccount.getHallRecommendModelMap().put(hallRecommendModel.getTarget_account_id(), hallRecommendModel);
				}
			}
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// hallRecommendModel.getTarget_account_id() + "", targetAccount);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(hallRecommendModel.getTarget_account_id());
			RsHallRecommendResponse.Builder rsHallRecommendResponse = RsHallRecommendResponse.newBuilder();
			rsHallRecommendResponse.setAccountId(hallRecommendModel.getAccount_id());
			rsHallRecommendResponse.setTargetAccountId(hallRecommendModel.getTarget_account_id());
			rsHallRecommendResponse.setCreateTime(hallRecommendModel.getCreate_time().getTime());
			rsHallRecommendResponse.setProxyLevel(hallRecommendModel.getProxy_level());
			rsAccountResponseBuilder.setRsHallRecommendResponse(rsHallRecommendResponse);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + targetAccount.getLastProxyIndex());
			return true;

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
	}

	public void dealBanned(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (rsAccountModelResponse.hasBanned()) {
			AccountModel accountModel = account.getAccountModel();
			accountModel.setBanned(rsAccountModelResponse.getBanned());
			rsAccountModelResponseBuilder.setBanned(rsAccountModelResponse.getBanned());// 同步
			// 被封号了，如果玩家在线直接踢下线
			if (accountModel.getBanned() == 1) {
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.CMD);
				RsCmdResponse.Builder rsCmdResponseBuilder = RsCmdResponse.newBuilder();
				rsCmdResponseBuilder.setType(1);
				rsCmdResponseBuilder.setAccountId(account.getAccount_id());
				redisResponseBuilder.setRsCmdResponse(rsCmdResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
			}
		}
	}

	public void dealIsAgent(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (rsAccountModelResponse.hasIsAgent()) {
			AccountModel accountModel = account.getAccountModel();
			accountModel.setIs_agent(rsAccountModelResponse.getIsAgent());
			if (rsAccountModelResponse.getIsAgent() >= 1) {
				accountModel.setProxy_level(rsAccountModelResponse.getIsAgent());
				account.getHallRecommendModel().setProxy_level(rsAccountModelResponse.getIsAgent());
				if (account.getHallRecommendModel().getTarget_account_id() > 0) {
					if (rsAccountModelResponse.getOpenAgentSource() > 0) {
						account.getHallRecommendModel().setSource(1);
					}
					updateHallRecommendModel(account.getHallRecommendModel());
				}
			} else {
				accountModel.setProxy_level(0);
				if (account.getHallRecommendModel().getProxy_level() != 0) {
					account.getHallRecommendModel().setProxy_level(0);
					if (account.getHallRecommendModel().getTarget_account_id() > 0)
						updateHallRecommendModel(account.getHallRecommendModel());
				}
			}
			rsAccountModelResponseBuilder.setIsAgent(rsAccountModelResponse.getIsAgent());// 通知
		}
	}

	public void dealHallRecommentId(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (ZZPromoterService.getInstance().getAccountZZPromoterModel(account.getAccount_id()) != null) {
			return;// 株洲协会的推广对象不能设置推广员
		}
		if (rsAccountModelResponse.hasHallRecommentId()) {
			AccountModel accountModel = account.getAccountModel();
			HallRecommendModel hallRecommendModel = account.getHallRecommendModel();
			long account_id = rsAccountModelResponse.getAccountId();
			String update_time = MyDateUtil.getDateFormat(new Date(), "yyyy-MM-dd");
			// 取消推荐人
			if (rsAccountModelResponse.getHallRecommentId() == 0) {
				if (hallRecommendModel.getAccount_id() > 0) {
					Account upAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
					HallRecommendModel subHallRecommendModel = upAccount.getHallRecommendModelMap().remove(account_id);
					// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
					// upAccount.getAccount_id() + "", upAccount);// 更新redis
					subHallRecommendModel.setAccount_id(0);
					subHallRecommendModel.setUpdate_time(update_time);
					hallRecommendModel.setAccount_id(0);
					updateHallRecommendModel(subHallRecommendModel);
				}
			} else {
				int level = 0;
				Account upAccount = PublicServiceImpl.getInstance().getAccount(rsAccountModelResponse.getHallRecommentId());
				// 上级的身份必须是1,2,3级推广员
				if (upAccount != null && upAccount.getHallRecommendModel().getRecommend_level() > 0
						&& upAccount.getHallRecommendModel().getRecommend_level() < 4) {
					if (rsAccountModelResponse.getHallRecommentLevel() > 0) {
						level = (upAccount.getHallRecommendModel().getRecommend_level() + 1) % 4;
					}
					if (hallRecommendModel.getRecommend_level() > 0) {
						// 先取消旧关系，再设置新的关系
						if (hallRecommendModel.getAccount_id() > 0) {
							Account oldUpAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
							HallRecommendModel subHallRecommendModel = oldUpAccount.getHallRecommendModelMap().remove(account_id);
							SpringService.getBean(PublicService.class).getPublicDAO().deleteHallRecommendModel(subHallRecommendModel);
							HallRecommendModel nowHallRecommendModel = new HallRecommendModel();// 现在的推荐关系
							nowHallRecommendModel.setAccount_id(upAccount.getAccount_id());
							nowHallRecommendModel.setTarget_name(account.getNickName());
							nowHallRecommendModel.setTarget_icon(account.getIcon());
							nowHallRecommendModel.setRecommend_level(level);
							nowHallRecommendModel.setCreate_time(new Date());
							nowHallRecommendModel.setTarget_account_id(account.getAccount_id());
							nowHallRecommendModel.setUpdate_time(update_time);
							nowHallRecommendModel.setProxy_level(accountModel.getIs_agent());
							account.setHallRecommendModel(nowHallRecommendModel);
							addHallRecommendModel(nowHallRecommendModel);// 添加推荐关系入库
						} else {
							hallRecommendModel.setAccount_id(rsAccountModelResponse.getHallRecommentId());
							hallRecommendModel.setRecommend_level(level);
							hallRecommendModel.setUpdate_time(update_time);
							upAccount.getHallRecommendModelMap().put(account_id, hallRecommendModel);
							// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
							// upAccount.getAccount_id() + "", upAccount);//
							// 更新redis
							updateHallRecommendModel(hallRecommendModel);
						}
					} else {
						if (hallRecommendModel.getTarget_account_id() == 0) {
							HallRecommendModel nowHallRecommendModel = new HallRecommendModel();// 现在的推荐关系
							nowHallRecommendModel.setAccount_id(upAccount.getAccount_id());
							nowHallRecommendModel.setTarget_name(account.getNickName());
							nowHallRecommendModel.setTarget_icon(account.getIcon());
							nowHallRecommendModel.setRecommend_level(level);
							nowHallRecommendModel.setCreate_time(new Date());
							nowHallRecommendModel.setTarget_account_id(account.getAccount_id());
							nowHallRecommendModel.setUpdate_time(update_time);
							nowHallRecommendModel.setProxy_level(accountModel.getIs_agent());
							account.setHallRecommendModel(nowHallRecommendModel);
							addHallRecommendModel(nowHallRecommendModel);// 添加推荐关系入库
						} else {
							if (hallRecommendModel.getAccount_id() > 0) {
								Account oldUpAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
								oldUpAccount.getHallRecommendModelMap().remove(account_id);
							}
							hallRecommendModel.setAccount_id(rsAccountModelResponse.getHallRecommentId());
							hallRecommendModel.setUpdate_time(update_time);
							hallRecommendModel.setRecommend_level(level);
							upAccount.getHallRecommendModelMap().put(account_id, hallRecommendModel);
							// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
							// upAccount.getAccount_id() + "", upAccount);//
							// 更新redis
							updateHallRecommendModel(hallRecommendModel);
						}

					}
				}
			}

		}
	}

	public void dealHallRecommentLevel(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (ZZPromoterService.getInstance().getAccountZZPromoterModel(account.getAccount_id()) != null) {
			return;// 株洲协会的推广对象不能设置推广员
		}
		if (rsAccountModelResponse.hasHallRecommentLevel()) {
			String update_time = MyDateUtil.getDateFormat(new Date(), "yyyy-MM-dd");
			long account_id = rsAccountModelResponse.getAccountId();
			if (rsAccountModelResponse.getHallRecommentLevel() >= 0 && rsAccountModelResponse.getHallRecommentLevel() < 4) {
				HallRecommendModel hallRecommendModel = account.getHallRecommendModel();// 获取当前推广员的上下级关系
				AccountModel accountModel = account.getAccountModel();
				if (rsAccountModelResponse.getHallRecommentLevel() > 0) {
					if (rsAccountModelResponse.getHallRecommentId() == 0) {// 只需要处理无推荐人的情况
						if (hallRecommendModel.getRecommend_level() > 0) {// 旧推荐关系存在，则先删除
							if (hallRecommendModel.getAccount_id() != 0) {// 旧推荐人存在，则先删除
								Account oldUpAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
								HallRecommendModel subHallRecommendModel = oldUpAccount.getHallRecommendModelMap().remove(account_id);
								// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
								// oldUpAccount.getAccount_id() + "",
								// oldUpAccount);// 更新redis
								SpringService.getBean(PublicService.class).getPublicDAO().deleteHallRecommendModel(subHallRecommendModel);
								HallRecommendModel nowHallRecommendModel = new HallRecommendModel();// 现在的推荐关系
								nowHallRecommendModel.setAccount_id(0);
								nowHallRecommendModel.setTarget_name(account.getNickName());
								nowHallRecommendModel.setTarget_icon(account.getIcon());
								nowHallRecommendModel.setRecommend_level(rsAccountModelResponse.getHallRecommentLevel());
								nowHallRecommendModel.setCreate_time(new Date());
								nowHallRecommendModel.setTarget_account_id(account.getAccount_id());
								nowHallRecommendModel.setUpdate_time(update_time);
								nowHallRecommendModel.setProxy_level(accountModel.getIs_agent());
								account.setHallRecommendModel(nowHallRecommendModel);
								addHallRecommendModel(nowHallRecommendModel);// 添加推荐关系入库
							} else {
								hallRecommendModel.setAccount_id(0);
								updateHallRecommendModel(hallRecommendModel);
							}
						} else {
							HallRecommendModel nowHallRecommendModel = new HallRecommendModel();// 现在的推荐关系
							nowHallRecommendModel.setAccount_id(0);
							nowHallRecommendModel.setTarget_name(account.getNickName());
							nowHallRecommendModel.setTarget_icon(account.getIcon());
							nowHallRecommendModel.setRecommend_level(rsAccountModelResponse.getHallRecommentLevel());
							nowHallRecommendModel.setCreate_time(new Date());
							nowHallRecommendModel.setTarget_account_id(account.getAccount_id());
							nowHallRecommendModel.setUpdate_time(update_time);
							nowHallRecommendModel.setProxy_level(accountModel.getIs_agent());
							account.setHallRecommendModel(nowHallRecommendModel);
							addHallRecommendModel(nowHallRecommendModel);// 添加推荐关系入库
						}
					}
				} else {// 取消推广员身份
					if (hallRecommendModel.getRecommend_level() > 0) {
						if (hallRecommendModel.getAccount_id() != 0) {
							Account oldUpAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
							if (oldUpAccount != null && oldUpAccount.getHallRecommendModel().getRecommend_level() > 0) {
								HallRecommendModel subHallRecommendModel = oldUpAccount.getHallRecommendModelMap().remove(account_id);
								// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
								// oldUpAccount.getAccount_id() + "",
								// oldUpAccount);// 更新redis
								SpringService.getBean(PublicService.class).getPublicDAO().deleteHallRecommendModel(subHallRecommendModel);
							}
						} else {
							SpringService.getBean(PublicService.class).getPublicDAO().deleteHallRecommendModel(hallRecommendModel);
						}
						account.setHallRecommendModel(new HallRecommendModel());
						// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
						// account.getAccount_id() + "", account);// 更新redis
					}
				}
			}
		}

	}

	public void dealMobilePhone(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (rsAccountModelResponse.hasMobilePhone()) {
			long account_id = rsAccountModelResponse.getAccountId();
			if (StringUtils.isNotBlank(rsAccountModelResponse.getMobilePhone())) {
				if (rsAccountModelResponse.getMobilePhone().equals("0")) {
					account.getAccountModel().setMobile_phone("");
					rsAccountModelResponseBuilder.setMobilePhone("");
					PhoneService.getInstance().unBind(account_id);
				} else {
					Pattern p = Pattern.compile("^1[1|3|4|5|7|8]\\d{9}$");
					Matcher m = p.matcher(rsAccountModelResponse.getMobilePhone());
					if (m.matches()) {
						account.getAccountModel().setMobile_phone(rsAccountModelResponse.getMobilePhone());
						rsAccountModelResponseBuilder.setMobilePhone(rsAccountModelResponse.getMobilePhone());
					}
				}
			}
		}
	}

	public void dealRecommendId(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (rsAccountModelResponse.hasRecommendId()) {
			long recommend_id = rsAccountModelResponse.getRecommendId();
			AccountModel accountModel = account.getAccountModel();
			if (recommend_id > 0 && recommend_id != account.getAccount_id()) {
				Account targetAccount = PublicServiceImpl.getInstance().getAccount(recommend_id);
				if (targetAccount != null) {
					// 删除旧推荐人
					if (accountModel.getRecommend_id() != 0) {
						Account targetAccountd = PublicServiceImpl.getInstance().getAccount(accountModel.getRecommend_id());
						AccountRecommendModel accountRecommendModel = targetAccountd.getAccountRecommendModelMap()
								.remove(accountModel.getAccount_id());
						if (accountRecommendModel != null) {
							SpringService.getBean(PublicService.class).getPublicDAO().deleteAccountRecommendModel(accountRecommendModel);
						}
					}
					// 重置推荐代理人id
					accountModel.setRecommend_id(recommend_id);

					// ========同步到中心========
					RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
					redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
					//
					RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
					rsAccountResponseBuilder.setAccountId(account.getAccount_id());
					//
					rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
					rsAccountModelResponseBuilder.setRecommendId(recommend_id);
					// rsAccountModelResponseBuilder.setNeedDb(true);
					rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
					//
					redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
					// 活动相关
					SysParamModel sysParamModel2000 = SysParamDict.getInstance()
							.getSysParamModelDictionaryByGameId(account.getGame_id() == 0 ? 3 : account.getGame_id()).get(2000);
					if (sysParamModel2000 != null && sysParamModel2000.getVal1() == 1) {
						if (targetAccount != null) {
							// 活动相关
							SysParamModel sysParamModel2004 = SysParamDict.getInstance()
									.getSysParamModelDictionaryByGameId(account.getGame_id() == 0 ? 3 : account.getGame_id()).get(2004);
							int addGold = sysParamModel2004.getVal2();
							AccountRecommendModel accountRecommendModel = new AccountRecommendModel();
							accountRecommendModel.setAccount_id(targetAccount.getAccount_id());
							accountRecommendModel.setTarget_account_id(account.getAccount_id());
							accountRecommendModel.setCreate_time(new Date());
							accountRecommendModel.setGold_num(addGold);
							accountRecommendModel.setTarget_name(account.getNickName());
							accountRecommendModel.setTarget_icon(account.getIcon());
							accountRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
							boolean flag = addAccountRecommendModel(accountRecommendModel);
							if (flag) {
								// 给好友加金币
								this.addAccountGold(targetAccount.getAccount_id(), addGold, false, "分享好友下载,好友account_id:" + account.getAccount_id(),
										EGoldOperateType.FRIEND_DOWN);
								// 给自己加金币,登录成功后的地方给
							}
						}
					}
				}
			}
			// addRecommendPlayerIncome(account,recommend_id);

		}
	}

	public void dealRecommendLevel(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (rsAccountModelResponse.hasRecommendLevel()) {
			long account_id = rsAccountModelResponse.getAccountId();
			AccountModel accountModel = account.getAccountModel();
			// && accountModel.getIs_agent() == 0
			if (accountModel.getRecommend_id() != 0) {
				Account recommendAccount = PublicServiceImpl.getInstance().getAccount(accountModel.getRecommend_id());// 获取推荐人的信息并判断是否推广员
				int level = recommendAccount.getAccountModel().getRecommend_level();
				if (level > 0 && level < 3) {
					if (rsAccountModelResponse.getRecommendLevel() == 0) {// 取消
						AccountRecommendModel accountRecommendModel = recommendAccount.getAccountRecommendModelMap().get(account_id);
						accountRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
						accountRecommendModel.setRecommend_level(0);
						// 更新recommend表
						SpringService.getBean(PublicService.class).getPublicDAO().updateAccountRecommendModel(accountRecommendModel);

						accountModel.setRecommend_level(0);

						recommendAccount.getAccountRecommendModelMap().put(account_id, accountRecommendModel);
						rsAccountModelResponseBuilder.setRecommendLevel(accountModel.getRecommend_level());// 通知
					} else {
						int count = 0;
						if (account.getAccountRecommendModelMap().size() > 30) {
							for (AccountRecommendModel model : recommendAccount.getAccountRecommendModelMap().values()) {
								if (model.getRecommend_level() > 0) {
									count++;
								}
							}
						}
						if (count < 30) {
							accountModel.setRecommend_level(level + 1);
							// 修改推荐表,更新推荐人缓存
							AccountRecommendModel accountRecommendModel = recommendAccount.getAccountRecommendModelMap().get(account_id);
							System.out.println("获取到未修改的等级为：" + accountRecommendModel.getRecommend_level());
							accountRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
							accountRecommendModel.setRecommend_level(level + 1);
							SpringService.getBean(PublicService.class).getPublicDAO().updateAccountRecommendModel(accountRecommendModel);

							recommendAccount.getAccountRecommendModelMap().put(account_id, accountRecommendModel);

							System.out.println("获取到修改后的等级为：" + accountRecommendModel.getRecommend_level());
							rsAccountModelResponseBuilder.setRecommendLevel(accountModel.getRecommend_level());// 通知
						}
					}
				}
			}

		}
	}

	public void dealProxyLevel(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (rsAccountModelResponse.hasProxyLevel()) {
			AccountModel accountModel = account.getAccountModel();

			// if (accountModel.getRecommend_level() == 0) {
			accountModel.setProxy_level(rsAccountModelResponse.getProxyLevel());
			if (rsAccountModelResponse.getProxyLevel() >= 1) {
				accountModel.setIs_agent(rsAccountModelResponse.getProxyLevel());
				if (account.getHallRecommendModel().getTarget_account_id() > 0) {
					account.getHallRecommendModel().setProxy_level(rsAccountModelResponse.getProxyLevel());
					updateHallRecommendModel(account.getHallRecommendModel());
				}
			} else {
				accountModel.setIs_agent(0);
				// 取消代理，需要取消钻石推广员的上下级关系
				if (accountModel.getRecommend_agent_id() > 0) {
					Account agentAccount = PublicServiceImpl.getInstance().getAccount(accountModel.getRecommend_agent_id());
					AgentRecommendModel upRecommendModel = agentAccount.getAgentRecommendModelMap().remove(accountModel.getAccount_id());
					SpringService.getBean(PublicService.class).getPublicDAO().deleteAgentRecommendModel(upRecommendModel);
					rsAccountModelResponseBuilder.setAgentRecommentId(0);
					accountModel.setRecommend_agent_id(0);
				}
				if (account.getHallRecommendModel().getTarget_account_id() > 0) {
					if (account.getHallRecommendModel().getProxy_level() != 0) {
						account.getHallRecommendModel().setProxy_level(0);
						updateHallRecommendModel(account.getHallRecommendModel());
					}
				}
			}
			rsAccountModelResponseBuilder.setProxyLevel(rsAccountModelResponse.getProxyLevel());
			// }

		}
	}

	public void dealTargetProxyId(RsAccountModelResponse rsAccountModelResponse, Account account,
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder) {
		if (rsAccountModelResponse.hasTargetProxyId()) {
			long account_id = rsAccountModelResponse.getAccountId();
			AccountModel accountModel = account.getAccountModel();
			long targetProxyId = rsAccountModelResponse.getTargetProxyId();
			if (targetProxyId > 0) {
				Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetProxyId);
				if (targetAccount != null) {
					RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();

					RsAccountModelResponse.Builder rsTargetAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
					if (rsAccountModelResponse.hasIsDeleteTarget() && rsAccountModelResponse.getIsDeleteTarget()) {
						AccountProxyModel accountProxy = account.getAccountProxyModelMap().get(targetProxyId);
						if (accountProxy != null) {
							account.getAccountProxyModelMap().remove(targetProxyId);
							SpringService.getBean(PublicService.class).getPublicDAO().deleteAccountProxyModel(accountProxy);

							targetAccount.getAccountModel().setUp_proxy(0);
							SpringService.getBean(PublicService.class).updateObject("updateAccountModel", targetAccount.getAccountModel());

							rsTargetAccountModelResponseBuilder.setUpProxy(0);

							RsAccountProxyResponse.Builder rsAccountProxyResponse = RsAccountProxyResponse.newBuilder();
							rsAccountProxyResponse.setAccountId(account_id);
							rsAccountProxyResponse.setTargetAccountId(targetProxyId);
							rsAccountProxyResponse.setIsDeleteTarget(true);// 删除
							rsAccountResponseBuilder.addRsRsAccountProxyResponseList(rsAccountProxyResponse);
						}
					} else {
						AccountProxyModel accountProxy = account.getAccountProxyModelMap().get(targetProxyId);
						if (accountProxy != null)
							return;

						if (targetAccount.getAccountModel().getUp_proxy() != 0) {
							return;
						}

						AccountProxyModel targertProxy = new AccountProxyModel();
						targertProxy.setAccount_id(account_id);
						targertProxy.setApply(1);
						targertProxy.setCreate_time(new Date());
						targertProxy.setTarget_account_id(targetProxyId);
						targertProxy.setTarget_proxy_level(accountModel.getProxy_level() + 1);// 目标的代理等级
																								// 当前的
						account.getAccountProxyModelMap().put(targetProxyId, targertProxy);

						SpringService.getBean(PublicService.class).getPublicDAO().insertAccountProxyModel(targertProxy);

						targetAccount.getAccountModel().setIs_agent(accountModel.getProxy_level() + 1);
						targetAccount.getAccountModel().setProxy_level(accountModel.getProxy_level() + 1);
						targetAccount.getAccountModel().setUp_proxy(account_id);

						SpringService.getBean(PublicService.class).updateObject("updateAccountModel", targetAccount.getAccountModel());

						rsTargetAccountModelResponseBuilder.setUpProxy(account_id);
						rsTargetAccountModelResponseBuilder.setProxyLevel(accountModel.getProxy_level() + 1);
						rsTargetAccountModelResponseBuilder.setIsAgent(accountModel.getProxy_level() + 1);

						RsAccountProxyResponse.Builder rsAccountProxyResponse = RsAccountProxyResponse.newBuilder();
						rsAccountProxyResponse.setAccountId(targertProxy.getAccount_id());
						rsAccountProxyResponse.setTargetAccountId(targertProxy.getTarget_account_id());
						rsAccountProxyResponse.setCreateTime(targertProxy.getCreate_time().getTime());
						if (targetAccount.getAccountWeixinModel() != null) {
							rsAccountProxyResponse.setTargetName(targetAccount.getAccountWeixinModel().getNickname());
							rsAccountProxyResponse.setTargetIcon(targetAccount.getAccountWeixinModel().getHeadimgurl());
						}
						rsAccountResponseBuilder.addRsRsAccountProxyResponseList(rsAccountProxyResponse);
					}

					RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
					redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
					//
					rsAccountResponseBuilder.setAccountId(targetProxyId);
					//
					rsAccountResponseBuilder.setRsAccountModelResponse(rsTargetAccountModelResponseBuilder);
					//
					redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
				} else {
					return;
				}
			}

		}
	}

	public boolean ossModifyAccountModel(RsAccountModelResponse rsAccountModelResponse) {
		if (rsAccountModelResponse == null)
			return false;

		if (!rsAccountModelResponse.hasAccountId())
			return false;

		long account_id = rsAccountModelResponse.getAccountId();
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null)
			return false;

		AccountModel accountModel = account.getAccountModel();

		RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
		dealBanned(rsAccountModelResponse, account, rsAccountModelResponseBuilder);
		dealIsAgent(rsAccountModelResponse, account, rsAccountModelResponseBuilder);
		dealHallRecommentId(rsAccountModelResponse, account, rsAccountModelResponseBuilder);
		dealHallRecommentLevel(rsAccountModelResponse, account, rsAccountModelResponseBuilder);
		// 设置大厅的推广级别
		dealMobilePhone(rsAccountModelResponse, account, rsAccountModelResponseBuilder);
		if (rsAccountModelResponse.hasIsInner()) {
			accountModel.setIs_inner(rsAccountModelResponse.getIsInner());
			rsAccountModelResponseBuilder.setIsInner(rsAccountModelResponse.getIsInner());// 通知
		}
		if (rsAccountModelResponse.hasPassword()) {
			accountModel.setPassword(rsAccountModelResponse.getPassword());
			rsAccountModelResponseBuilder.setPassword(rsAccountModelResponse.getPassword());
		}
		dealProxyLevel(rsAccountModelResponse, account, rsAccountModelResponseBuilder);
		if (rsAccountModelResponse.hasPromoteLevel()) {
			// if (accountModel.getIs_agent() == 0) {
			accountModel.setPromote_level(rsAccountModelResponse.getPromoteLevel());
			accountModel.setRecommend_level(rsAccountModelResponse.getPromoteLevel());
			rsAccountModelResponseBuilder.setPromoteLevel(rsAccountModelResponse.getPromoteLevel());
			rsAccountModelResponseBuilder.setRecommendLevel(accountModel.getRecommend_level());// 通知
			// }

		}

		if (rsAccountModelResponse.hasIsRebate()) {
			accountModel.setIs_rebate(rsAccountModelResponse.getIsRebate());
			rsAccountModelResponseBuilder.setIsRebate(rsAccountModelResponse.getIsRebate());
		}
		dealTargetProxyId(rsAccountModelResponse, account, rsAccountModelResponseBuilder);
		// 立即入库
		SpringService.getBean(PublicService.class).updateObject("updateAccountModel", accountModel);

		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		//
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account_id);
		//
		rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
		//
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);

		return true;
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
