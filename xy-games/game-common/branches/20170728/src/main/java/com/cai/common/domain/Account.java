package com.cai.common.domain;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.define.EPtType;
import com.cai.common.define.EWxHeadimgurlType;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.WxUtil;
import com.xianyi.framework.core.concurrent.WorkerLoop;

/**
 * 账号
 * 
 * @author run
 *
 */
public class Account implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5612008938216272367L;

	/**
	 * 账号
	 */
	private AccountModel accountModel;

	/**
	 * 微信相关
	 */
	private AccountWeixinModel accountWeixinModel;

	/**
	 * 账号参数列表
	 */
	private Map<Integer, AccountParamModel> accountParamModelMap;

	/**
	 * 邀请列表 key=对方玩家的id
	 */
	private Map<Long, AccountRecommendModel> accountRecommendModelMap;

	/**
	 * 下级代理列表 key=对方玩家的id
	 */
	private Map<Long, AccountProxyModel> accountProxyModelMap;

	private Map<String, AccountGroupModel> accountGroupModelMap;

	/**
	 * 缓存创建时间
	 */
	private long cacheCreateTime;

	/**
	 * 上次登录的游戏
	 */
	private int lastGameIndex;

	/**
	 * 上一次游戏状态
	 */
	private int lastGameStatus;

	/**
	 * 上一次链接的逻辑计算服
	 */
	private int lastLogicIndex;

	/**
	 * 上一次链接的代理服
	 */
	private int lastProxyIndex;

	/**
	 * 当前游戏id
	 */
	private int game_id;

	/**
	 * 当前房间号
	 */
	private int room_id;

	/**
	 * redis数据刷新锁
	 */
	private transient ReentrantLock redisLock;

	/**
	 * 执行器
	 */
	private transient WorkerLoop workerLoop;

	// /**
	// * 当前代理房间列表 key=room_id
	// */
	// private Map<Integer,PrxoyPlayerRoomModel> proxRoomMap =
	// Maps.newConcurrentMap();
	//
	public Account() {
		redisLock = new ReentrantLock();
	}

	public AccountModel getAccountModel() {
		return accountModel;
	}

	public void setAccountModel(AccountModel accountModel) {
		this.accountModel = accountModel;
	}

	public long getCacheCreateTime() {
		return cacheCreateTime;
	}

	public void setCacheCreateTime(long cacheCreateTime) {
		this.cacheCreateTime = cacheCreateTime;
	}

	public int getLastGameIndex() {
		return lastGameIndex;
	}

	public void setLastGameIndex(int lastGameIndex) {
		this.lastGameIndex = lastGameIndex;
	}

	public int getLastGameStatus() {
		return lastGameStatus;
	}

	public void setLastGameStatus(int lastGameStatus) {
		this.lastGameStatus = lastGameStatus;
	}

	public int getLastLogicIndex() {
		return lastLogicIndex;
	}

	public void setLastLogicIndex(int lastLogicIndex) {
		this.lastLogicIndex = lastLogicIndex;
	}

	public int getGame_id() {
		return game_id;
	}

	public void setGame_id(int game_id) {
		this.game_id = game_id;
	}

	public int getRoom_id() {
		return room_id;
	}

	public void setRoom_id(int room_id) {
		this.room_id = room_id;
	}

	public int getLastProxyIndex() {
		return lastProxyIndex;
	}

	public void setLastProxyIndex(int lastProxyIndex) {
		this.lastProxyIndex = lastProxyIndex;
	}

	public AccountWeixinModel getAccountWeixinModel() {
		return accountWeixinModel;
	}

	public void setAccountWeixinModel(AccountWeixinModel accountWeixinModel) {
		this.accountWeixinModel = accountWeixinModel;
	}

	public Map<Integer, AccountParamModel> getAccountParamModelMap() {
		return accountParamModelMap;
	}

	public void setAccountParamModelMap(Map<Integer, AccountParamModel> accountParamModelMap) {
		this.accountParamModelMap = accountParamModelMap;
	}

	public Map<String, AccountGroupModel> getAccountGroupModelMap() {
		return accountGroupModelMap;
	}

	public void setAccountGroupModelMap(Map<String, AccountGroupModel> accountGroupModelMap) {
		this.accountGroupModelMap = accountGroupModelMap;
	}

	public Map<Long, AccountProxyModel> getAccountProxyModelMap() {
		return accountProxyModelMap;
	}

	public void setAccountProxyModelMap(Map<Long, AccountProxyModel> accountProxyModelMap) {
		this.accountProxyModelMap = accountProxyModelMap;
	}

	// ==========
	public long getAccount_id() {
		return accountModel.getAccount_id();
	}

	public String getAccount_name() {
		return accountModel.getAccount_name();
	}

	public String getNickName() {

		if (accountModel.getPt().equals(EPtType.SELF.getId())) {
			String name = accountModel.getAccount_name();
			if (name.indexOf("SELF_") != -1) {
				String name2 = name.split("SELF_")[1];
				name2 = MyStringUtil.substringByLength(name2, 12);
				return name2;
			}
			// String name = "游客" + this.getAccount_id();
			// return name;
		} else if (accountModel.getPt().equals(EPtType.WX.getId())) {
			String nickname = accountWeixinModel.getNickname();
			return MyStringUtil.substringByLength(nickname, 12);
		}

		return accountModel.getAccount_name();

	}

	public String getIcon() {
		if (accountWeixinModel != null) {
			String icon = WxUtil.changHeadimgurl(accountWeixinModel.getHeadimgurl(), EWxHeadimgurlType.S46);
			return icon;
		}
		return "1.png";
	}

	public ReentrantLock getRedisLock() {
		if (redisLock == null) {
			redisLock = new ReentrantLock();
		}

		return redisLock;
	}

	public void setRedisLock(ReentrantLock redisLock) {
		this.redisLock = redisLock;
	}

	public Map<Long, AccountRecommendModel> getAccountRecommendModelMap() {
		return accountRecommendModelMap;
	}

	public void setAccountRecommendModelMap(Map<Long, AccountRecommendModel> accountRecommendModelMap) {
		this.accountRecommendModelMap = accountRecommendModelMap;
	}

	/**
	 * @return the workerLoop
	 */
	public WorkerLoop getWorkerLoop() {
		return workerLoop;
	}

	/**
	 * @param workerLoop
	 *            the workerLoop to set
	 */
	public void setWorkerLoop(WorkerLoop workerLoop) {
		this.workerLoop = workerLoop;
	}

	@Override
	public String toString() {
		return String.format("Account [accountid:%s,nickname:%s],pt:%s,gold:%d,money%d]", accountModel.getAccount_id(),
				null == accountWeixinModel ? accountModel.getAccount_name() : accountWeixinModel.getNickname(), accountModel.getPt(),
				accountModel.getGold(), accountModel.getMoney());
	}

	// public Map<Integer, PrxoyPlayerRoomModel> getProxRoomMap() {
	// return proxRoomMap;
	// }
	//
	// public void setProxRoomMap(Map<Integer, PrxoyPlayerRoomModel>
	// proxRoomMap) {
	// this.proxRoomMap = proxRoomMap;
	// }

}
