/**
 * 湘ICP备15020076 copyright@2015-2016湖南旗胜网络科技有限公司
 */
package com.cai.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.InviteActiveModel;
import com.cai.common.domain.InviteRedPacketModel;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.InviteActiveDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 排行榜，排行榜数据会从center server当前服务器
 *
 * @author wu_hc
 */
public class RecommendService {

	/**
	 * 
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static RecommendService INSTANCE = null;
	// 湖南麻将推广缓存
	// private static ConcurrentHashMap<Long, HallRecommendModel> recommendHNMap
	// = null;

	// private static ConcurrentHashMap<Long, HallRecommendModel> recommendDTMap
	// = null;

	private RecommendService() {
	}

	public static RecommendService getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RecommendService();
			// recommendHNMap = new ConcurrentHashMap<Long,
			// HallRecommendModel>();
			// recommendDTMap = new ConcurrentHashMap<Long,
			// HallRecommendModel>();
		}
		return INSTANCE;
	}

	// 给满足条件的人送豆，返利
	public boolean hallSendGold(Long accountId) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(accountId);
		if (account == null) {
			return false;
		}
		// 没有填写推广员
		if (account.getHallRecommendModel().getAccount_id() == 0) {
			return false;
		}
		// 没有推广员或已经领取过填写推广员奖励
		if (!account.getAccountParamModelMap().containsKey(EAccountParamType.ADD_HALL_GOLD.getId())
				|| account.getAccountParamModelMap().get(EAccountParamType.ADD_HALL_GOLD.getId()).getVal1() == 2) {
			return false;
		}
		AccountParamModel paramModel15 = account.getAccountParamModelMap().get(EAccountParamType.TOTAL_ROUND.getId());
		SysParamModel sysParamModel2227 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2227);
		int minRoundCount = 6;// 最少局数
		int day = 1;// 最少天数
		// int maxRecommendCount = 300;//最大推荐人数
		if (sysParamModel2227 != null) {
			minRoundCount = sysParamModel2227.getVal1();// 最少局数
			day = sysParamModel2227.getVal2();// 最少天数
			// maxRecommendCount = sysParamModel2226.getVal3();//最大推荐人数
		}
		// 未满最低局数不送豆，不返利
		if (paramModel15.getVal1() < minRoundCount) {
			return false;
		}
		HallRecommendModel hallRecommendModel = account.getHallRecommendModel();
		// 未满day*24小时不送豆
		if ((hallRecommendModel.getCreate_time().getTime() + day * 24 * 60 * 60000) > System.currentTimeMillis()) {
			return false;
		}
		SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
		int addGold = 50;
		if (sysParamModel2004 != null) {
			addGold = sysParamModel2004.getVal4();
		}
		centerRMIServer.addAccountGold(account.getAccount_id(), addGold, false,
				"填写推广员送豆，推广员account_id:" + account.getHallRecommendModel().getAccount_id(), EGoldOperateType.PADDING_RECOMMEND_ID);
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
		rsAccountParamModelResponse.setAccountId(account.getAccount_id());
		rsAccountParamModelResponse.setType(EAccountParamType.ADD_HALL_GOLD.getId());
		rsAccountParamModelResponse.setVal1(2);// 大厅填写推广员获豆
		rsAccountParamModelResponse.setNeedDb(true);
		rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
		return true;
	}

	// 推荐送豆，被推荐人得豆
	public boolean SendGold(Long accountId) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(accountId);
		if (account == null) {
			return false;
		}
		// 江苏棋牌推广玩家返利
		recommendPlayerReceive(account);
		if (account.getAccountModel().getRecommend_id() == 0) {
			return false;
		}
		// 登录后处理邀请送红包活动
		AccountParamModel paramModel16 = account.getAccountParamModelMap().get(EAccountParamType.ADD_RECOMMEND_GOLD.getId());
		if (paramModel16 != null && StringUtils.isBlank(paramModel16.getStr1())) {
			inviteRedpacket(accountId);
		}
		// 没有推荐人或已经领取过推荐奖励
		if (!account.getAccountParamModelMap().containsKey(EAccountParamType.ADD_RECOMMEND_GOLD.getId())
				|| account.getAccountParamModelMap().get(EAccountParamType.ADD_RECOMMEND_GOLD.getId()).getVal1() == 2) {
			return false;
		}
		AccountParamModel paramModel15 = account.getAccountParamModelMap().get(EAccountParamType.TOTAL_ROUND.getId());
		if (paramModel15 == null) {
			return false;
		}

		SysParamModel sysParamModel2226 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2226);
		int minRoundCount = 3;// 最少局数
		int day = 1;// 最少天数
		// int maxRecommendCount = 300;//最大推荐人数
		if (sysParamModel2226 != null) {
			minRoundCount = sysParamModel2226.getVal1();// 最少局数
			day = sysParamModel2226.getVal2();// 最少天数
			// maxRecommendCount = sysParamModel2226.getVal3();//最大推荐人数
		}
		// 未满最低局数不送豆，不返利
		if (paramModel15.getVal1() == null || paramModel15.getVal1() < minRoundCount) {
			return false;
		}
		Account upAccount = centerRMIServer.getAccount(account.getAccountModel().getRecommend_id());
		AccountRecommendModel recommendModel = upAccount.getAccountRecommendModelMap().get(account.getAccount_id());
		if (recommendModel == null) {
			return false;// 绑定关系已经解除
		} else {
			// 未满day*24小时不送豆
			if ((recommendModel.getCreate_time().getTime() + day * 24 * 60 * 60000) > System.currentTimeMillis()) {
				return false;
			}
			SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2000);
			if (sysParamModel2000 == null || sysParamModel2000.getVal1() != 1) {
				return false;
			}
			SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
			int addGold = sysParamModel2004.getVal2();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
			rsAccountParamModelResponse.setAccountId(account.getAccount_id());
			rsAccountParamModelResponse.setType(EAccountParamType.ADD_RECOMMEND_GOLD.getId());
			rsAccountParamModelResponse.setVal1(2);// 推荐人已经享受推荐获豆
			rsAccountParamModelResponse.setNeedDb(true);
			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
			RsAccountParamModelResponse.Builder rsAccountParamModelResponse2 = RsAccountParamModelResponse.newBuilder();
			rsAccountParamModelResponse2.setAccountId(account.getAccount_id());
			rsAccountParamModelResponse2.setType(EAccountParamType.DRAW_SHARE_DOWN.getId());
			rsAccountParamModelResponse2.setVal1(1);
			rsAccountParamModelResponse2.setNeedDb(true);
			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse2);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
			// 给好友/推荐人加金币
			centerRMIServer.addAccountGold(upAccount.getAccount_id(), addGold, false, "分享好友下载,好友account_id:" + account.getAccount_id(),
					EGoldOperateType.FRIEND_DOWN);
			// 推荐送豆，自己得豆
			centerRMIServer.addAccountGold(account.getAccount_id(), sysParamModel2004.getVal3(), false,
					"通过分享下载,分享人account_id:" + upAccount.getAccount_id(), EGoldOperateType.SHARE_DOWN);
			// 计算返利
			if (account.getGame_id() == EGameType.MJ.getId()) {
				addRecommendPlayerIncome(account, upAccount.getAccount_id());
			}
			recommendModel.setGold_num(addGold);
			centerRMIServer.rmiInvoke(RMICmd.UPDATE_MODEL, recommendModel);
			return true;
		}
	}

	// 推荐送豆，自己得豆
	public boolean recommendSendGold(Long accountId) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(accountId);
		if (account == null) {
			return false;
		}
		if (account.getAccountModel().getRecommend_id() == 0) {
			return false;
		}
		// 没有推荐人或已经领取过推荐奖励
		if (!account.getAccountParamModelMap().containsKey(EAccountParamType.ADD_RECOMMEND_GOLD.getId())
				|| account.getAccountParamModelMap().get(EAccountParamType.ADD_RECOMMEND_GOLD.getId()).getVal1() == 2) {
			return false;
		}
		AccountParamModel paramModel15 = account.getAccountParamModelMap().get(EAccountParamType.TOTAL_ROUND.getId());
		SysParamModel sysParamModel2226 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2226);
		int minRoundCount = 3;// 最少局数
		int day = 1;// 最少天数
		// int maxRecommendCount = 300;//最大推荐人数
		if (sysParamModel2226 != null) {
			minRoundCount = sysParamModel2226.getVal1();// 最少局数
			day = sysParamModel2226.getVal2();// 最少天数
			// maxRecommendCount = sysParamModel2226.getVal3();//最大推荐人数
		}
		// 未满最低局数不送豆，不返利
		if (paramModel15.getVal1() < minRoundCount) {
			return false;
		}
		Account upAccount = centerRMIServer.getAccount(account.getAccountModel().getRecommend_id());
		AccountRecommendModel recommendModel = upAccount.getAccountRecommendModelMap().get(account.getAccount_id());
		if (recommendModel == null) {
			return false;// 绑定关系已经解除
		} else {
			// 未满day*24小时不送豆
			if ((recommendModel.getCreate_time().getTime() + day * 24 * 60 * 60000) > System.currentTimeMillis()) {
				return false;
			}
			SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
			int addGold = sysParamModel2004.getVal2();
			// 给好友/推荐人加金币
			centerRMIServer.addAccountGold(upAccount.getAccount_id(), addGold, false, "分享好友下载,好友account_id:" + account.getAccount_id(),
					EGoldOperateType.FRIEND_DOWN);
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
			rsAccountParamModelResponse.setAccountId(account.getAccount_id());
			rsAccountParamModelResponse.setType(EAccountParamType.ADD_RECOMMEND_GOLD.getId());
			rsAccountParamModelResponse.setVal1(2);// 已经享受推荐获豆
			rsAccountParamModelResponse.setNeedDb(true);
			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
			return true;
		}
	}

	// 江苏棋牌推广玩家返利
	private void recommendPlayerReceive(Account account) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		// 只有江苏棋牌有推荐返利
		SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
		long gameId = sysParamModel5000.getVal1();
		if (gameId != EGameType.JS.getId()) {
			return;
		}
		SysParamModel sysParamModel2225 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(7).get(2225);
		int money = 150;
		if (sysParamModel2225 != null) {
			if (sysParamModel2225.getVal4() == 0) {// 返利开关
				return;
			}
			money = sysParamModel2225.getVal1();// val1返利总额
		} else {
			return;
		}
		AccountParamModel accountParamModel18 = PublicServiceImpl.getInstance().getAccountParamModel(account,
				EAccountParamType.RECOMMEND_PLAYER_RECEIVE);
		// val1=1获得返利资格，2表示已经返利
		if (accountParamModel18 == null || accountParamModel18.getVal1() == null || accountParamModel18.getVal1() != 1) {
			return;
		}
		int minRoundCount = 3;// 最少局数
		int day = 1;// 最少天数
		SysParamModel sysParamModel2226 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(7).get(2226);
		// int maxRecommendCount = 300;//最大推荐人数
		if (sysParamModel2226 != null) {
			minRoundCount = sysParamModel2226.getVal4();// 最少局数
			day = sysParamModel2226.getVal5();// 最少天数
			// maxRecommendCount = sysParamModel2226.getVal3();//最大推荐人数
		}
		if ((account.getAccountModel().getCreate_time().getTime() + day * 24 * 60 * 60000) > System.currentTimeMillis()) {
			return;// 创建账号未到设定返利时间
		}
		AccountParamModel paramModel15 = account.getAccountParamModelMap().get(EAccountParamType.TOTAL_ROUND.getId());

		if (paramModel15 == null || paramModel15.getVal1() == null || paramModel15.getVal1() < minRoundCount) {// 未达指定局数
			return;
		}
		centerRMIServer.doRecommendPlayerReceive(account, money);
		accountParamModel18.setVal1(2);
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
		rsAccountParamModelResponse.setAccountId(account.getAccount_id());
		rsAccountParamModelResponse.setType(EAccountParamType.RECOMMEND_PLAYER_RECEIVE.getId());
		rsAccountParamModelResponse.setVal1(accountParamModel18.getVal1());
		rsAccountParamModelResponse.setNeedDb(true);
		rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

	}

	// 返利
	private void addRecommendPlayerIncome(Account account, long recommend_id) {
		try {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			// 只有湖南麻将有推荐返利
			if (account.getGame_id() != EGameType.MJ.getId()) {
				return;
			}
			AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account,
					EAccountParamType.UP_RECOMMEND_PLAYER_INCOME);
			if (accountParamModel.getVal1() != null && accountParamModel.getVal1() == 1) {
				return;
			}
			// 活动相关
			if (recommend_id == 0) {
				return;// 无推荐人
			}
			Account recommendAccount = centerRMIServer.getAccount(recommend_id);
			int level = recommendAccount.getAccountModel().getRecommend_level();
			if (recommendAccount == null || level == 0) {
				return;// 推荐人不存在或推荐人不是推广员
			}
			SysParamModel sysParamModel6001 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(6001);
			if (sysParamModel6001 == null) {
				return;
			}
			if (level == 1) {
				logger.info(recommendAccount.getAccount_id() + " 一级推广员推荐新玩家返利:" + sysParamModel6001.getVal1() / 10.0);
				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal1() / 10.0, 0l, "推荐新玩家",
						EGoldOperateType.RECOMMEND_PLAYER, account.getAccount_id());
				return;
			} else if (level == 2) {
				logger.info(recommendAccount.getAccount_id() + " 二级推广员推荐新玩家返利:" + sysParamModel6001.getVal2() / 10.0);
				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal2() / 10.0, 0l, "推荐新玩家",
						EGoldOperateType.RECOMMEND_PLAYER, account.getAccount_id());
				if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
					Account recommendAccountUp = centerRMIServer.getAccount(recommendAccount.getAccountModel().getRecommend_id());
					if (recommendAccountUp.getAccountModel().getRecommend_level() == 1) {
						logger.info(recommendAccount.getAccount_id() + " 二级推广员推荐新玩家返利:" + sysParamModel6001.getVal4() / 10.0);
						centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(), sysParamModel6001.getVal4() / 10.0, 2l, "下级推广员推荐新玩家",
								EGoldOperateType.RECOMMEND_PLAYER, account.getAccount_id());
						return;
					}
				}

			} else if (level == 3) {
				logger.info(recommendAccount.getAccount_id() + " 三级推广员推荐新玩家返利:" + sysParamModel6001.getVal3() / 10.0);
				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal3() / 10.0, 0l, "推荐新玩家",
						EGoldOperateType.RECOMMEND_PLAYER, account.getAccount_id());
				if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
					Account recommendAccountUp = centerRMIServer.getAccount(recommendAccount.getAccountModel().getRecommend_id());
					if (recommendAccountUp.getAccountModel().getRecommend_level() == 2) {
						logger.info(recommendAccount.getAccount_id() + " 三级推广员推荐新玩家返利:" + sysParamModel6001.getVal5() / 10.0);
						centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(), sysParamModel6001.getVal5() / 10.0, 3l, "下级推广员推荐新玩家",
								EGoldOperateType.RECOMMEND_PLAYER, account.getAccount_id());
						if (recommendAccountUp.getAccountModel().getRecommend_id() != 0) {
							Account recommendAccountUpUp = centerRMIServer.getAccount(recommendAccountUp.getAccountModel().getRecommend_id());
							if (recommendAccountUpUp.getAccountModel().getRecommend_level() == 1) {
								logger.info(recommendAccount.getAccount_id() + " 三级推广员推荐新玩家返利:" + sysParamModel6001.getVal4() / 10.0);
								centerRMIServer.doRecommendIncome(recommendAccountUpUp.getAccount_id(), sysParamModel6001.getVal4() / 10.0, 3l,
										"下级推广员推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER, account.getAccount_id());
							}
						}
					}
				}
			}
			accountParamModel.setVal1(1);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
			rsAccountParamModelResponse.setAccountId(account.getAccount_id());
			rsAccountParamModelResponse.setType(EAccountParamType.UP_RECOMMEND_PLAYER_INCOME.getId());
			rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
			rsAccountParamModelResponse.setNeedDb(true);
			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
		} catch (Exception e) {
		}
	}

	// 登录获取邀请红包处理
	public void inviteRedpacket(long accountId) {
		try {
			InviteActiveModel inviteActiveModel = InviteActiveDict.getInstance().getInviteActiveModel();
			if (inviteActiveModel == null || inviteActiveModel.getId() == 0) {
				return;
			}
			long nowTime = System.currentTimeMillis();
			long startTime = inviteActiveModel.getBegin_time().getTime();
			long endTime = inviteActiveModel.getEnd_time().getTime();
			// 活动未开始
			if (nowTime < startTime || nowTime > endTime) {
				return;
			}
			Map<String, String> map = new HashMap<>();
			map.put("account_id", accountId + "");
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.rmiInvoke(RMICmd.INVITE_REDPACKET_HANDLER, map);
		} catch (Exception e) {
			logger.error("INVITE_REDPACKET_HANDLER error !", e);
		}
	}

	// 邀请用户预处理
	public void invite(long accountId, long target_account_id,String headimgurl) {
		InviteActiveModel inviteActiveModel = InviteActiveDict.getInstance().getInviteActiveModel();
		if (inviteActiveModel == null || inviteActiveModel.getId() == 0) {
			return;
		}
		long nowTime = System.currentTimeMillis();
		long startTime = inviteActiveModel.getBegin_time().getTime();
		long endTime = inviteActiveModel.getEnd_time().getTime();
		// 活动未开始
		if (nowTime < startTime || nowTime > endTime) {
			return;
		}
		try {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			PlayerViewVO accountModel = centerRMIServer.getPlayerViewVo(target_account_id);
			// 注册时间要在活动范围内
			long registerTime = accountModel.getCreate_time().getTime();
			if (registerTime < startTime || registerTime > endTime) {
				return;
			}
			InviteRedPacketModel inviteRedPacketModel = new InviteRedPacketModel(accountId, target_account_id, 0, inviteActiveModel.getInvite_pay(),
					inviteActiveModel.getId(), StringUtils.isBlank(accountModel.getHead())?headimgurl:accountModel.getHead());
			MongoDBServiceImpl.getInstance().getLogQueue().add(inviteRedPacketModel);
		} catch (Exception e) {
			logger.error("invite error! ", e);
		}

	}
}
