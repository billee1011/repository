package com.cai.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.AccountConstant;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.DbOpType;
import com.cai.common.define.DbStoreType;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EPtType;
import com.cai.common.define.EWxHeadimgurlType;
import com.cai.common.define.IDType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountProxyModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.AgentRecommendModel;
import com.cai.common.domain.DBUpdateDto;
import com.cai.common.domain.Event;
import com.cai.common.domain.GlobalModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.RandomGameRoomModel;
import com.cai.common.domain.RecommendRelativeModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.WxUtil;
import com.cai.common.util.XYGameException;
import com.cai.core.DataThreadPool;
import com.cai.core.Global;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.AccountWxOfficialDict;
import com.cai.dictionary.ActivityDict;
import com.cai.dictionary.ActivityMissionDict;
import com.cai.dictionary.ActivityRedpacketPoolDict;
import com.cai.dictionary.AppItemDict;
import com.cai.dictionary.CardCategoryDict;
import com.cai.dictionary.CardSecretDict;
import com.cai.dictionary.ChannelModelDict;
import com.cai.dictionary.CityDict;
import com.cai.dictionary.ClientPlayerErWeiMaDict;
import com.cai.dictionary.CoinCornucopiaDict;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.CoinExciteDict;
import com.cai.dictionary.ContinueLoginDict;
import com.cai.dictionary.CustomerSerNoticeDict;
import com.cai.dictionary.GameDescDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.GameRecommendDict;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.HallGuideDict;
import com.cai.dictionary.IPGroupDict;
import com.cai.dictionary.InviteActiveDict;
import com.cai.dictionary.InviteFriendsActivityDict;
import com.cai.dictionary.ItemDict;
import com.cai.dictionary.ItemExchangeDict;
import com.cai.dictionary.LogicServerBalanceDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.dictionary.MainUiNoticeDict;
import com.cai.dictionary.MatchBroadDict;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.MoneyShopDict;
import com.cai.dictionary.NoticeDict;
import com.cai.dictionary.PushManagerDict;
import com.cai.dictionary.RecommendLimitDict;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.dictionary.SdkAppDict;
import com.cai.dictionary.SdkDiamondShopDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SpecialAccountDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.TurntableDict;
import com.cai.dictionary.WelfareExchangeDict;
import com.cai.dictionary.WelfareGoodsTypeDict;
import com.cai.domain.Session;
import com.cai.future.runnable.DbBatchRunnable;
import com.cai.redis.service.RedisService;
import com.cai.timer.CheckRedisRoomTimer;
import com.cai.timer.DataStatTimer;
import com.cai.timer.RMIStatTimer;
import com.cai.timer.RedisTopicStatisticsTimer;
import com.cai.timer.SubGameOnlineTimer;
import com.cai.timer.TvActivityOnlineTimer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;

public class PublicServiceImpl extends AbstractService {

	private static Logger logger = LoggerFactory.getLogger(PublicServiceImpl.class);

	private static PublicServiceImpl instance = null;

	private Timer timer;

	private Map<Long, Account> accountIdMap = Maps.newConcurrentMap();
	private Map<String, Long> accountNameIdMap = Maps.newConcurrentMap();
	private Map<String, Long> unionidAccountIdMap = Maps.newConcurrentMap();
	private Map<Long, AccountSimple> AccountSimpleMap = Maps.newConcurrentMap();

	/**
	 * 随机房间号 key=game_id
	 */
	private Map<Integer, RandomGameRoomModel> randomRoomIdMap;

	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private Map<Integer, Map<Integer, SysParamModel>> sysParamModelMap = Maps.newConcurrentMap();

	/**
	 * 记录每天每个子游戏的--在线人数的情况
	 */
	private Map<Integer, HashSet<Integer>> gameTypeNumber = new ConcurrentHashMap<>();

	/**
	 * 全局变量
	 */
	private GlobalModel globalModel;

	/**
	 * 单个玩家创建房间调用频率限制
	 */
	private static final LoadingCache<Integer, Integer> roomCache = CacheBuilder.newBuilder().maximumSize(100000L)
			.expireAfterWrite(24, TimeUnit.HOURS).build(new CacheLoader<Integer, Integer>() {
				@Override
				public Integer load(Integer key) throws Exception {
					return null;
				}
			});

	public void logicToRoom(int roomID, int logicIndex) {
		roomCache.put(roomID, logicIndex);
	}

	public int getLogicIndexByRoomId(int roomId) {
		Integer logicIndex = roomCache.getIfPresent(roomId);
		return logicIndex == null ? -1 : logicIndex;
	}

	private PublicServiceImpl() {
		timer = new Timer("Timer-PublicServiceImpl");
		randomRoomIdMap = Maps.newHashMap();
	}

	public void clearGameTypeNumber() {
		gameTypeNumber.clear();
	}

	public Map<Integer, HashSet<Integer>> getGameTypeNumberMap() {
		return gameTypeNumber;
	}

	public static PublicServiceImpl getInstance() {
		if (null == instance) {
			instance = new PublicServiceImpl();
		}
		return instance;
	}

	@Override
	protected void startService() {

		// 全局变最
		globalModel = new GlobalModel();

		// mysql入库
		// dbSyncTimer = new DBSyncTimer();
		// timer.schedule(dbSyncTimer, 1000, 30000);
		Global.getDbBatchService().scheduleAtFixedRate(new DbBatchRunnable(), 5, 10, TimeUnit.SECONDS);

		// timer.schedule(new GameNoticeTimer(), 60000L, 60000L);// 公告
		timer.schedule(new DataStatTimer(), 60000L, 60000L);// 数据统计
		timer.schedule(new RedisTopicStatisticsTimer(), 60000L, 60000L);// redis数据统计
		timer.schedule(new RMIStatTimer(), 60000L, 10000L);// RMI连接测试
		timer.schedule(new CheckRedisRoomTimer(), 60000L, 3600000L);// 释放过期的房间,每小时
		timer.schedule(new SubGameOnlineTimer(), 60000L, 300000L);// 5分钟统计一次
		timer.schedule(new TvActivityOnlineTimer(), 600000L, 600000L);
		// 排行榜测试 GAME-TODO
		// timer.schedule(new RankAndNoticeTimer(), 10000L, 3600000L);

		// roomIdGenerate();

		loadCache();

	}

	/**
	 * 房间id生成
	 */
	protected void roomIdGenerate() {

		final Random random = new Random();

		// 普通房间id
		// 金币场/比赛场房间

		// 生成随机房间号
		randomRoomIdMap.put(1, new RandomGameRoomModel(1, idGenerate(110000, 999999, random), idGenerate(1000000, 2000000, random)));
	}

	/**
	 * 
	 * @param min
	 * @param max
	 * @param random
	 * @return
	 */
	private int[] idGenerate(int min, int max, Random random) {

		if (min < 0 || max < 0 || min >= max) {
			throw new XYGameException(String.format("id生成参数不合理！min:%d ,max:%d", min, max));
		}
		int len = max - min + 1;
		int[] ids = new int[len];
		for (int i = 0, idx = min; i < len; i++) {
			ids[i] = idx++;
		}

		RandomUtil.shuffle(ids, null != random ? random : new Random());
		return ids;
	}

	/**
	 * 加载缓存
	 */
	public void loadCache() {
		CityDict.getInstance().load();
		SysGameTypeDict.getInstance().load();// 游戏类型对应收费索引 游戏类型 描述
		SysParamDict.getInstance().load();// 系统参数
		ServerDict.getInstance().load();// 服务器
//		ServerDict.getInstance().loadGate();// 服务器
		SysNoticeDict.getInstance().load();// 系统公告
		ShopDict.getInstance().load();// 商品列表
		GameDescDict.getInstance().load();// 游戏玩法说明
		MainUiNoticeDict.getInstance().load();// 主界面公告
		LoginNoticeDict.getInstance().load();// 登录公告
		MoneyShopDict.getInstance().load();// 金币商品列表
		ActivityDict.getInstance().load();// 活动
		ActivityMissionDict.getInstance().load();// 活动任务
		ContinueLoginDict.getInstance().load();// 连续登录
		GoodsDict.getInstance().load();
		IPGroupDict.getInstance().load();
		AppItemDict.getInstance().load();
		GameRecommendDict.getInstance().load();
		GameGroupRuleDict.getInstance().load();
		LogicServerBalanceDict.getInstance().load(); // 指定子游戏运行服务器
		RedPackageRuleDict.getInstance().load();
		TurntableDict.getInstance().load();
		SysParamServerDict.getInstance().load();// 服务器后端系统参数
		CustomerSerNoticeDict.getInstance().load();// 客服公告
		ItemDict.getInstance().load();
		MatchBroadDict.getInstance().load();// 比赛场公告
		MatchDict.getInstance().load();// 加载比赛场
		AccountPushServiceImp.getInstance().loadPushMap();// 加载推送信息
		SpecialAccountDict.getInstance().load();
		InviteActiveDict.getInstance().load();// 加载邀请活动
		CoinDict.getInstance().load(); // 加载金币场配置
		ItemExchangeDict.getInstance().load(); // 加载实物兑换商品列表
		WelfareExchangeDict.getInstance().load(); // 福卡兑换商品列表
		WelfareGoodsTypeDict.getInstance().load(); // 福卡商城商品分类
		CardSecretDict.getInstance().load(); // 卡密库
		RecommendLimitDict.getInstance().load();
		AccountWxOfficialDict.getInstance().load();
		ActivityRedpacketPoolDict.getInstance().load();
		ChannelModelDict.getInstance().load();
		PushManagerDict.getInstance().load();
		NoticeDict.INSTANCE().load();
		HallGuideDict.getInstance().load();
		HallGuideDict.getInstance().loadResource();
		CardCategoryDict.getInstance().load();
		CoinExciteDict.getInstance().load();
		ClientPlayerErWeiMaDict.getInstance().load();
		InviteFriendsActivityDict.getInstance().load();
		HallGuideDict.getInstance().loadMainViewBack();
		SdkAppDict.getInstance().load();
		SdkDiamondShopDict.getInstance().load();
		CoinCornucopiaDict.getInstance().load();
	}

	public boolean hasAccount(long account_id) {
		return accountIdMap.containsKey(account_id);
	}

	public Account getAccount(long account_id) {
		// 内存中找,缓存中找,数据库中找
		Account account = accountIdMap.get(account_id);
		if (account != null)
			return account;

		// try {
		// account =
		// SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT,
		// account_id + "", Account.class);
		// } catch (Exception e) {
		// logger.error("error", e);
		// }

		// if (account != null) {
		// accountIdMap.put(account_id, account);
		// accountNameIdMap.put(account.getAccount_name(), account_id);
		// AccountWeixinModel accountWeixinModel =
		// account.getAccountWeixinModel();
		// if (accountWeixinModel != null && accountWeixinModel.getUnionid() !=
		// null) {
		// unionidAccountIdMap.put(account.getAccountWeixinModel().getUnionid(),
		// account_id);
		// }
		// return account;
		// }

		PublicService publicService = SpringService.getBean(PublicService.class);
		AccountModel accountModel = publicService.getPublicDAO().getAccountById(account_id);
		if (accountModel != null) {
			account = new Account();
			account.setCacheCreateTime(System.currentTimeMillis());
			account.setLastProxyIndex(0);
			// account.setGame_id(0);傻掉
			account.setAccountModel(accountModel);

			// 参数列表
			Map<Integer, AccountParamModel> accountParamModelMap = Maps.newConcurrentMap();
			account.setAccountParamModelMap(accountParamModelMap);
			List<AccountParamModel> accountParamModelList = publicService.getPublicDAO().getAccountParamModelByAccountId(account_id);
			for (AccountParamModel m : accountParamModelList) {
				accountParamModelMap.put(m.getType(), m);
			}

			// 微信
			if (accountModel.getPt().equals(EPtType.WX.getId())) {
				AccountWeixinModel accountWeixinModel = publicService.getPublicDAO().getAccountWeixinModelByAccountId(account.getAccount_id());
				if (accountWeixinModel == null) {
					accountWeixinModel = new AccountWeixinModel();
					accountWeixinModel.setAccount_id(account.getAccount_id());
					publicService.getPublicDAO().insertAccountWeixinModel(accountWeixinModel);
				}
				account.setAccountWeixinModel(accountWeixinModel);
			}
			// 设置推广员通用属性
			account.setRecommendRelativeModel(new RecommendRelativeModel());
			// 钻石黄金推广员系统中，如果是代理，需要新加代理的关系
			Map<Long, AgentRecommendModel> agentRecommendModelMap = Maps.newConcurrentMap();
			account.setAgentRecommendModelMap(agentRecommendModelMap);
			if (accountModel.getIs_agent() > 0 && accountModel.getProxy_level() < 3) {
				// 代理列表
				List<AgentRecommendModel> agentRecommendModelList = publicService.getPublicDAO().getAgentRecommendModelListByAccountId(account_id);
				if (agentRecommendModelList != null && agentRecommendModelList.size() > 0) {
					for (AgentRecommendModel m : agentRecommendModelList) {
						// 填充扩展值
						AccountSimple accountSimple = getAccountSimpe(m.getTarget_account_id());
						if (accountSimple != null) {
							m.setTarget_name(accountSimple.getNick_name());
							m.setTarget_icon(accountSimple.getIcon());
							agentRecommendModelMap.put(m.getTarget_account_id(), m);
						} else {
							// 防止错误
							m.setTarget_name("--");
							m.setTarget_icon("1.png");
							agentRecommendModelMap.put(m.getTarget_account_id(), m);
						}
					}
					account.setAgentRecommendModelMap(agentRecommendModelMap);
				}
			}
			// 设置红包相关对象
			account.setRedActivityModel(publicService.getPublicDAO().getRedActivityModelByAccountId(account_id));
			// 设置他的推荐关系
			account.setHallRecommendModel(publicService.getPublicDAO().getHallRecommendModelByTargetAccountId(account_id));
			Map<Long, HallRecommendModel> hallRecommendModelMap = Maps.newConcurrentMap();
			account.setHallRecommendModelMap(hallRecommendModelMap);
			List<HallRecommendModel> hallRecommendModelList = publicService.getPublicDAO().getHallRecommendModelListByAccountId(account_id);
			for (HallRecommendModel hallRecommendModel : hallRecommendModelList) {
				AccountSimple accountSimple = getAccountSimpe(hallRecommendModel.getTarget_account_id());
				if (accountSimple != null) {
					hallRecommendModel.setTarget_name(accountSimple.getNick_name());
					hallRecommendModel.setTarget_icon(accountSimple.getIcon());
				} else {
					// 防止错误
					hallRecommendModel.setTarget_name("--");
					hallRecommendModel.setTarget_icon("http://img.51yeyou.cc/luahall/xiaobenzi.png");
				}
				hallRecommendModelMap.put(hallRecommendModel.getTarget_account_id(), hallRecommendModel);
			}
			// 邀请列表
			Map<Long, AccountRecommendModel> accountRecommendModelMap = Maps.newConcurrentMap();
			// 这是是查推荐人列表
			List<AccountRecommendModel> accountRecommendModelList = publicService.getPublicDAO().getAccountRecommendModelListByAccountId(account_id);
			for (AccountRecommendModel m : accountRecommendModelList) {

				// 填充扩展值
				AccountSimple accountSimple = getAccountSimpe(m.getTarget_account_id());
				if (accountSimple != null) {
					m.setTarget_name(accountSimple.getNick_name());
					m.setTarget_icon(accountSimple.getIcon());
					accountRecommendModelMap.put(m.getTarget_account_id(), m);
				} else {
					// 防止错误
					m.setTarget_name("--");
					m.setTarget_icon("1.png");
					accountRecommendModelMap.put(m.getTarget_account_id(), m);
				}
			}
			account.setAccountRecommendModelMap(accountRecommendModelMap);

			// 邀请列表
			Map<Long, AccountProxyModel> accountProxyModelMap = Maps.newConcurrentMap();
			List<AccountProxyModel> accountProxyModelList = publicService.getPublicDAO().getAccountProxyModelListByAccountId(account_id);
			for (AccountProxyModel m : accountProxyModelList) {
				// 填充扩展值
				AccountSimple accountSimple = getAccountSimpe(m.getTarget_account_id());
				if (accountSimple != null) {
					m.setTarget_name(accountSimple.getNick_name());
					m.setTarget_icon(accountSimple.getIcon());
					accountProxyModelMap.put(m.getTarget_account_id(), m);
				} else {
					// 防止错误
					m.setTarget_name("--");
					m.setTarget_icon("1.png");
					accountProxyModelMap.put(m.getTarget_account_id(), m);
				}
			}
			account.setAccountProxyModelMap(accountProxyModelMap);

			Map<String, AccountGroupModel> accountGroupModelMap = Maps.newConcurrentMap();
			account.setAccountGroupModelMap(accountGroupModelMap);
			List<AccountGroupModel> accountGroupModelList = publicService.getPublicDAO().getAccountGroupModelListByAccountId(account_id);
			for (AccountGroupModel m : accountGroupModelList) {
				accountGroupModelMap.put(m.getGroupId(), m);
			}

			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// account_id + "", account);

			
//			AccountRedis accountRedis = SpringService.getBean(RedisService.class)
//					.hGet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "", AccountRedis.class);
//			if (null == accountRedis) {//删除这些需要的时候再创建
//				// 辅助对象
//				accountRedis = new AccountRedis();
//				accountRedis.setAccount_id(account_id);
//				SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT_REDIS, account_id + "", accountRedis);
//
//			}
			
			accountIdMap.put(account_id, account);
			accountNameIdMap.put(account.getAccount_name(), account_id);
			AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
			if (accountWeixinModel != null && accountWeixinModel.getUnionid() != null) {
				unionidAccountIdMap.put(accountWeixinModel.getUnionid(), account_id);
			}

			// WalkerGeek 初始化红包雨未入库变量
			account.setDay_all_round(new AtomicInteger(0));
			account.setRED_PACK_COUNT(HashMultiset.create());

			return account;
		}

		return null;
	}

	public Account getAccount(String account_name) {
		Long account_id = accountNameIdMap.get(account_name);
		if (account_id == null) {
			PublicService publicService = SpringService.getBean(PublicService.class);
			AccountModel accountModel = publicService.getPublicDAO().getAccountByAccountName(account_name);
			if (accountModel == null)
				return null;
			accountNameIdMap.put(accountModel.getAccount_name(), accountModel.getAccount_id());
			account_id = accountModel.getAccount_id();
		}
		return getAccount(account_id);
	}

	public void putUnionIdAccountRelative(String unionid, long accountId) {
		if (StringUtils.isNotBlank(unionid) && accountId > 0) {
			unionidAccountIdMap.put(unionid, accountId);
		}
	}

	public Account getAccountByWxUnionid(String unionid) {
		Long account_id = unionidAccountIdMap.get(unionid);
		if (account_id == null) {
			PublicService publicService = SpringService.getBean(PublicService.class);
			AccountWeixinModel accountWeixinModel = publicService.getPublicDAO().getAccountWeixinModelByUnionid(unionid);
			if (accountWeixinModel == null)
				return null;
			unionidAccountIdMap.put(accountWeixinModel.getUnionid(), accountWeixinModel.getAccount_id());
			account_id = accountWeixinModel.getAccount_id();
		}
		return getAccount(account_id);
	}

	/**
	 * 不同游戏房间号生成
	 * 
	 * @param game_id
	 * @return -1表示失败
	 */
	public int randomRoomId(int game_id) {
		// RandomGameRoomModel randomGameRoomModel =
		// randomRoomIdMap.get(game_id);
		// return randomGameRoomModel.randomRoomId();

		return IDServiceImpl.getInstance().nextInt(IDType.NORMAL_ROOM);
	}

	/**
	 * 不同游戏房间号生成
	 * 
	 * @param game_id
	 * @return -1表示失败
	 */
	public int moneyRandomRoomId(int game_id) {
		// RandomGameRoomModel randomGameRoomModel =
		// randomRoomIdMap.get(game_id);
		// return randomGameRoomModel.moneyRandomRoomId();

		return IDServiceImpl.getInstance().nextInt(IDType.COIN_ROOM);
	}

	/**
	 * 获取玩家属性值，查不到时自动初始化入库
	 * 
	 * @param account_id
	 * @param eAccountParamType
	 * @return
	 */
	public AccountParamModel getAccountParamModel(long account_id, EAccountParamType eAccountParamType) {
		Account account = this.getAccount(account_id);
		if (account == null)
			return null;
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(eAccountParamType.getId());
		if (accountParamModel != null) {
			return accountParamModel;
		} else {
			accountParamModel = new AccountParamModel();
			accountParamModel.setAccount_id(account_id);
			accountParamModel.setType(eAccountParamType.getId());
			accountParamModel.setVal1(0);
			accountParamModel.setStr1("");
			accountParamModel.setLong1(0L);
			accountParamModel.setDate1(null);
			accountParamModel.setNewAddValue(true);
			// 加入缓存
			account.getAccountParamModelMap().put(eAccountParamType.getId(), accountParamModel);
			return accountParamModel;
		}
	}

	/**
	 * 获取account简单对象
	 * 
	 * @param account_id
	 * @return
	 */
	public AccountSimple getAccountSimpe(long account_id) {

		try {
			AccountSimple accountSimple = AccountSimpleMap.get(account_id);
			if (accountSimple != null)
				return accountSimple;

			// 直接查数据库
			PublicService publicService = SpringService.getBean(PublicService.class);
			AccountModel accountModel = publicService.getPublicDAO().getAccountById(account_id);
			if (accountModel != null) {
				if (EPtType.SELF.getId().equals(accountModel.getPt())) {
					String name1 = accountModel.getAccount_name();
					if (name1.indexOf("SELF_") != -1) {
						String name2 = name1.split("SELF_")[1];
						name2 = MyStringUtil.substringByLength(name2, AccountConstant.NICK_NAME_LEN);

						accountSimple = new AccountSimple();
						accountSimple.setAccount_id(account_id);
						accountSimple.setNick_name(name2);
						accountSimple.setIcon("http://img.51yeyou.cc/luahall/xiaobenzi.png");
						AccountSimpleMap.put(account_id, accountSimple);
					}

				} else if (EPtType.WX.getId().equals(accountModel.getPt())) {
					AccountWeixinModel accountWeixinModel = publicService.getPublicDAO().getAccountWeixinModelByAccountId(account_id);
					if (accountWeixinModel != null) {

						accountSimple = new AccountSimple();
						accountSimple.setAccount_id(account_id);
						String nickname = accountWeixinModel.getNickname();
						accountSimple.setNick_name(MyStringUtil.substringByLength(nickname, 12));
						String icon = WxUtil.changHeadimgurl(accountWeixinModel.getHeadimgurl(), EWxHeadimgurlType.S132);
						accountSimple.setIcon(icon);
						AccountSimpleMap.put(account_id, accountSimple);
					}
				}
			}

			return accountSimple;

		} catch (Exception e) {
			logger.error("error", e);
			return null;
		}
	}

	public void clearAccount(Account account) {
		if (account == null)
			return;
		// 本地内存
		PublicServiceImpl.getInstance().getAccountNameIdMap().remove(account.getAccount_name());
		PublicServiceImpl.getInstance().getAccountIdMap().remove(account.getAccount_id());
		PublicServiceImpl.getInstance().getAccountSimpleMap().remove(account.getAccount_id());
	}

	/**
	 * 账号id换靓号辅助方法，修改上下级推荐关系等等，不修改旧账号的id
	 * 
	 * @return
	 */
	public boolean updateAccountId(long accountId, long newAccountId) {
		Account account = this.getAccount(accountId);
		if (account == null) {
			return false;
		}
		HallRecommendModel model = account.getHallRecommendModel();
		HallRecommendModel hallRecommendModel = new HallRecommendModel(accountId, newAccountId);
		DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "replaceAccountId", hallRecommendModel));
		// 湖南推广员身份需要更新推广员的id
		if (account.getAccountModel().getRecommend_level() > 0) {
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "replaceRecommendId", hallRecommendModel));
			Map<Long, AccountRecommendModel> accountRecommendModelMap = account.getAccountRecommendModelMap();
			if (accountRecommendModelMap.size() > 0) {
				for (long id : accountRecommendModelMap.keySet()) {
					Account subAccount = this.getAccount(id);
					subAccount.getAccountModel().setRecommend_id(newAccountId);
				}
			}
		}
		deleteAccountNameIdMapByName(account.getAccount_name());
		if (account.getAccountWeixinModel() != null)
			deleteUnionidAccountIdMapByName(account.getAccountWeixinModel().getUnionid());
		// RedisService redisService =
		// SpringService.getBean(RedisService.class);
		// redisService.hDel(RedisConstant.ACCOUNT, (account.getAccount_id() +
		// "").getBytes());
		// 有大厅新推广员关系
		if (model.getTarget_account_id() > 0) {
			if (model.getRecommend_level() > 0) {// 一般需要修改代理关系及推广员关系
				// 替换自己
				DataThreadPool.getInstance()
						.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateHallTargetAccountId", hallRecommendModel));
				// 替换下级关联
				DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateHallAccountId", hallRecommendModel));
				Map<Long, HallRecommendModel> hallRecommendModelMap = account.getHallRecommendModelMap();
				// 替换下级玩家的缓存
				if (hallRecommendModelMap.size() > 0) {
					for (long id : hallRecommendModelMap.keySet()) {
						Account subAccount = this.getAccount(id);
						subAccount.getHallRecommendModel().setAccount_id(newAccountId);
					}
				}
			} else {
				// 替换自己
				DataThreadPool.getInstance()
						.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateHallTargetAccountId", hallRecommendModel));
			}
		}
		BonusPointsService.getInstance().updateAccountId(accountId, newAccountId);
		clearAccount(account);
		return true;
		// 由于操作这个步骤之后会将用户踢下线，所以不用对自己的缓存进行操作
	}

	public void deleteAccountNameIdMapByName(String key) {
		try {
			accountNameIdMap.remove(key);
		} catch (Exception e) {
		}
	}

	public void deleteUnionidAccountIdMapByName(String key) {
		try {
			unionidAccountIdMap.remove(key);
		} catch (Exception e) {
		}
	}

	public void switchUnionId(AccountWeixinModel acountWx, AccountWeixinModel oldAcountWx) {
		unionidAccountIdMap.put(acountWx.getUnionid(), acountWx.getAccount_id());
		unionidAccountIdMap.put(oldAcountWx.getUnionid(), oldAcountWx.getAccount_id());
		String accounName = EPtType.WX.getId() + "_" + acountWx.getUnionid();
		String oldAccounName = EPtType.WX.getId() + "_" + oldAcountWx.getUnionid();
		accountNameIdMap.put(accounName, acountWx.getAccount_id());
		accountNameIdMap.put(oldAccounName, oldAcountWx.getAccount_id());
	}

	public void switchAccountNameId(AccountWeixinModel acountWx, AccountWeixinModel oldAcountWx) {
		unionidAccountIdMap.put(acountWx.getUnionid(), acountWx.getAccount_id());
		unionidAccountIdMap.put(oldAcountWx.getUnionid(), oldAcountWx.getAccount_id());
	}

	/**
	 * 
	 * @param oldUnid
	 * @param oldAccountName
	 * @param account
	 * @return
	 */
	public synchronized Account switchWX(String oldUnid, String oldAccountName, Account account) {

		final AccountWeixinModel wxModel = account.getAccountWeixinModel();
		if (null == wxModel) {
			logger.error("########玩家[]切换微信，但wxmodel为空!##########", account);
			return account;
		}

		logger.warn("玩家[{}]切换微信操作,{}->{}", account, oldUnid, wxModel.getUnionid());

		unionidAccountIdMap.remove(oldUnid);
		unionidAccountIdMap.put(account.getAccountWeixinModel().getUnionid(), account.getAccount_id());

		accountNameIdMap.remove(oldAccountName);
		accountNameIdMap.put(account.getAccount_name(), account.getAccount_id());
		// try {
		// // 更新redis缓存
		// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
		// Long.toString(account.getAccount_id()), account);
		// } catch (Exception e) {
		// e.printStackTrace();
		// logger.error("error", e);
		// }
		AccountSimple accountSimple = AccountSimpleMap.get(account.getAccount_id());
		if (null != accountSimple) {
			accountSimple.setNick_name(account.getAccountWeixinModel().getNickname());
			accountSimple.setIcon(WxUtil.changHeadimgurl(account.getAccountWeixinModel().getHeadimgurl(), EWxHeadimgurlType.S132));
		}
		return account;
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

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub

	}

	public Map<Long, Account> getAccountIdMap() {
		return accountIdMap;
	}

	public Map<String, Long> getAccountNameIdMap() {
		return accountNameIdMap;
	}

	public GlobalModel getGlobalModel() {
		return globalModel;
	}

	public Map<Long, AccountSimple> getAccountSimpleMap() {
		return AccountSimpleMap;
	}

	public void setAccountSimpleMap(Map<Long, AccountSimple> accountSimpleMap) {
		AccountSimpleMap = accountSimpleMap;
	}

	public HallRecommendModel getHallRecommendModel(long account_id) {
		Account account = this.getAccount(account_id);
		if (account == null) {
			return new HallRecommendModel();
		}
		return account.getHallRecommendModel();
	}
}
