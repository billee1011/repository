package com.lingyu.common.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.game.service.config.ConfigConstant;
import com.lingyu.game.service.mahjong.MahjongConstant;

/**
 * 房间类
 *
 * @author wangning
 * @date 2016年12月6日 下午6:26:18
 */
public class MahjongRoom {

	private static final Logger logger = LogManager.getLogger(MahjongRoom.class);

	/** 房间编号 */
	private int roomNum;
	/** 创建房间的人id */
	private long leaderId;
	/** 局数 */
	private int jushu;
	/** 是否要红中赖子 */
	private boolean hz;
	/** 是否开始游戏 */
	private boolean isStart = false;
	/** key=index(可以理解为第几个进来的玩家) val=roleCache */
	private Map<Integer, RoleCache> roleCacheMap = new ConcurrentHashMap<>();
	/** 房间里的牌总和 */
	private ChessEvery roomChess[] = null;
	/** 痞牌 */
	private ChessEvery ruffian;
	/** 痞下一张为癞子，此处就是该癞子 */
	private ChessEvery randomLaizi;
	/** 正常摸牌的索引位 */
	private int paindex;
	/** 从后面摸的索引位 */
	private int lastIndex;
	/** 一共摸了多少张牌 */
	private int sumIndex;
	/** 摸牌或打牌的玩家索引位 */
	private int operateIndex;
	/** 当前打出去的牌 */
	private ChessEvery currChessEvery;
	/** 当前摸到的牌 */
	private ChessEvery currMoChessEvery;
	/** 房间角色Id缓存 */
	private long[] allRoleIdCache = null;
	/** 翻的倍数 只有胡和自摸的时候才会用到 */
	private int fan;
	/** 开始游戏的时候 角色是否都点了开始游戏 */
	private Map<Long, Boolean> startGameMap = new HashMap<>();
	/** 已经打了的局数 */
	private int alreadyJuShu = 1;
	/** 是否可以弹总分面板 1.局数全部打完可以弹 2.解散房间了以后可以弹 */
	private boolean sumScorePanel = false;
	/** 记录这把是谁第一个摸牌的，(谁是庄)，用于牌打完了。没人胡的情况下。下把开始的时候。他还是第一个摸牌的 */
	private int zhuang = 0;
	/** 申请解散房间的申请人id */
	private long disMissApplyRoleId = 0;
	/** 申请解散房间 玩家的操作 */
	private Map<Long, Boolean> disMissMap = new HashMap<>();
	/** 当前局是否结束 */
	private boolean curEnd;
	/** 房间的创建时间 */
	private Date createDate;
	/** 每局的开始时间 */
	private Date curJuStartDate;
	/** 玩法：单双豹 */
	private int playType;
	/** 起胡番薯 */
	private int minimumHu = 8;
	/** 封顶番薯 */
	private int fanLimit = 32;
	/** 回放map */
	private Map<Integer, PlayBackVo> playBackMap = new HashMap<>();
	/**
	 * 碰，明杠，胡的roleId缓存 key=roleId, val=signType 1.有一炮多响的情况
	 * 2.有碰(或明杠)和胡的情况，如果碰的玩家先点碰，胡牌的玩家点过，这时候要在过的那边处理碰的玩家的牌
	 */
	private Map<Long, SignVo> signMap = new HashMap<>();

	public int getRoomNum() {
		return roomNum;
	}

	public void setRoomNum(int roomNum) {
		this.roomNum = roomNum;
	}

	public long getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(long leaderId) {
		this.leaderId = leaderId;
	}

	public int getJushu() {
		return jushu;
	}

	public void setJushu(int jushu) {
		this.jushu = jushu;
	}

	public boolean isHz() {
		return hz;
	}

	public void setHz(boolean hz) {
		this.hz = hz;
	}

	public boolean isStart() {
		return isStart;
	}

	public void setStart(boolean isStart) {
		this.isStart = isStart;
	}

	public ChessEvery[] getRoomChess() {
		return roomChess;
	}

	public int getPaindex() {
		return paindex;
	}

	public int getPlayType() {
		return playType;
	}

	public void setPlayType(int playType) {
		this.playType = playType;
	}

	public int getMinimumHu() {
		return minimumHu;
	}

	public void setMinimumHu(int minimumHu) {
		this.minimumHu = minimumHu;
	}

	public int getFanLimit() {
		return fanLimit;
	}

	public void setFanLimit(int fanLimit) {
		this.fanLimit = fanLimit;
	}

	public void setPaindex(int paindex) {
		this.paindex = paindex;
	}

	public int getLastIndex() {
		return lastIndex;
	}

	public void setLastIndex(int lastIndex) {
		this.lastIndex = lastIndex;
	}

	public int getSumIndex() {
		return sumIndex;
	}

	public void setSumIndex(int sumIndex) {
		this.sumIndex = sumIndex;
	}

	public Map<Integer, RoleCache> getRoleCacheMap() {
		return roleCacheMap;
	}

	public void setRoleCacheMap(Map<Integer, RoleCache> roleCacheMap) {
		this.roleCacheMap = roleCacheMap;
	}

	public ChessEvery getRuffian() {
		return ruffian;
	}

	public void setRuffian(ChessEvery ruffian) {
		this.ruffian = ruffian;
	}

	public int getOperateIndex() {
		return operateIndex;
	}

	public void setOperateIndex(int operateIndex) {
		this.operateIndex = operateIndex;
	}

	public ChessEvery getCurrChessEvery() {
		return currChessEvery;
	}

	public void setCurrChessEvery(ChessEvery currChessEvery) {
		this.currChessEvery = currChessEvery;
	}

	public ChessEvery getCurrMoChessEvery() {
		return currMoChessEvery;
	}

	public void setCurrMoChessEvery(ChessEvery currMoChessEvery) {
		this.currMoChessEvery = currMoChessEvery;
	}

	public int getFan() {
		return fan;
	}

	public void setFan(int fan) {
		this.fan = fan;
	}

	public Map<Long, Boolean> getStartGameMap() {
		return startGameMap;
	}

	public Map<Long, SignVo> getSignMap() {
		return signMap;
	}

	public void addSignMap(long roleId, SignVo vo) {
		signMap.put(roleId, vo);
	}

	public int getAlreadyJuShu() {
		return alreadyJuShu;
	}

	public void addAlreadyJuShu() {
		this.alreadyJuShu = alreadyJuShu + 1;
	}

	public int getZhuang() {
		return zhuang;
	}

	public void setZhuang(int zhuang) {
		this.zhuang = zhuang;
	}

	public long getDisMissApplyRoleId() {
		return disMissApplyRoleId;
	}

	public void setDisMissApplyRoleId(long disMissApplyRoleId) {
		this.disMissApplyRoleId = disMissApplyRoleId;
	}

	public Map<Long, Boolean> getDisMissMap() {
		return disMissMap;
	}

	public void addDisMissMap(long roleId, boolean flag) {
		this.disMissMap.put(roleId, flag);
	}

	public boolean isSumScorePanel() {
		return sumScorePanel;
	}

	public void setSumScorePanel(boolean sumScorePanel) {
		this.sumScorePanel = sumScorePanel;
	}

	public boolean isCurEnd() {
		return curEnd;
	}

	public void setCurEnd(boolean curEnd) {
		this.curEnd = curEnd;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getCurJuStartDate() {
		return curJuStartDate;
	}

	public void setCurJuStartDate(Date curJuStartDate) {
		this.curJuStartDate = curJuStartDate;
	}

	public Map<Integer, PlayBackVo> getPlayBackMap() {
		return playBackMap;
	}

	public ChessEvery getRandomLaizi() {
		return randomLaizi;
	}

	public void setRandomLaizi(ChessEvery randomLaizi) {
		this.randomLaizi = randomLaizi;
	}

	public void init() {
		this.paindex = 0;
		this.lastIndex = 0;
		this.sumIndex = 0;
		this.startGameMap.clear();
		this.currChessEvery = null;
		this.currMoChessEvery = null;
		this.fan = 0;
		this.signMap.clear();
		this.sumScorePanel = false;
		this.disMissApplyRoleId = 0l;
		this.disMissMap.clear();
		this.curEnd = false;
		this.playBackMap = new HashMap<>();
		this.addAlreadyJuShu();
	}

	/**
	 * 获取房间里的所有角色id
	 *
	 * @return
	 */
	public long[] getAllRoleId() {
		if (allRoleIdCache == null) {
			long[] roleIds = new long[roleCacheMap.size()];
			int i = 0;
			for (Entry<Integer, RoleCache> e : roleCacheMap.entrySet()) {
				RoleCache cache = e.getValue();
				roleIds[i++] = cache.getRoleId();
			}
			this.allRoleIdCache = roleIds;
		}
		return allRoleIdCache;
	}

	/**
	 * 获取房间里的除了RoleId以外的角色id
	 *
	 * @param allRoleIds
	 * @param roleId
	 * @return
	 */
	public long[] getOtherRoleId(long roleId) {
		long allROleIds[] = getAllRoleId();
		long all[] = new long[allROleIds.length - 1];
		int i = 0;
		for (long id : allROleIds) {
			if (id == roleId) {
				continue;
			}
			all[i++] = id;
		}
		return all;
	}

	/**
	 * 添加房间用户
	 *
	 * @param cache
	 */
	public void addMember(RoleCache cache) {
		this.allRoleIdCache = null;
		int index = getMaxIndex() + 1;
		roleCacheMap.put(index, cache);
	}

	/**
	 * 房间玩家退出
	 *
	 * @param index
	 */
	public void removeMember(int index) {
		this.allRoleIdCache = null;
		roleCacheMap.remove(index);

		Map<Integer, RoleCache> roleCacheMapTemp = new ConcurrentHashMap<>();
		Integer keys[] = roleCacheMap.keySet().toArray(new Integer[roleCacheMap.size()]);
		Arrays.sort(keys);

		int i = 1;
		for (Integer key : keys) {
			RoleCache cache = roleCacheMap.get(key);
			roleCacheMapTemp.put(i++, cache);
		}
		this.roleCacheMap.clear();
		this.roleCacheMap = roleCacheMapTemp;
	}

	/**
	 * 找到角色最大索引位
	 *
	 * @return
	 */
	public int getMaxIndex() {
		int max = 0;
		for (Integer index : roleCacheMap.keySet()) {
			if (index > max) {
				max = index;
			}
		}
		return max;
	}

	/***
	 * 根据角色id获取索引位
	 *
	 * @param roleId
	 * @return
	 */
	public int getIndexByRoleId(long roleId) {
		for (Entry<Integer, RoleCache> e : roleCacheMap.entrySet()) {
			if (roleId == e.getValue().getRoleId()) {
				return e.getKey();
			}
		}
		return 0;
	}

	/***
	 * 根据索引id得到roleId
	 *
	 * @param index
	 * @return
	 */
	public long getRoleIdByIndex(int index) {
		for (Entry<Integer, RoleCache> e : roleCacheMap.entrySet()) {
			if (index == e.getKey()) {
				return e.getValue().getRoleId();
			}
		}
		return 0;
	}

	/***
	 * 根据索引id得到roleId
	 *
	 * @param index
	 * @return
	 */
	public RoleCache getRoleCacheByIndex(int index) {
		for (Entry<Integer, RoleCache> e : roleCacheMap.entrySet()) {
			if (index == e.getKey()) {
				return e.getValue();
			}
		}
		return null;
	}

	/**
	 * 房间内所有人的积分
	 * 
	 * @param room
	 * @return
	 */
	public Object[] getAllRoleJifen() {
		List res = new ArrayList<>();
		for (RoleCache cache : roleCacheMap.values()) {
			res.add(new Object[] { cache.getRoleId(), cache.getJifen() });
		}
		return res.toArray();
	}

	/**
	 * 通过角色id得到roleCache
	 *
	 * @param roleId
	 * @return
	 */
	public RoleCache getRoleCacheByRoleId(long roleId) {
		for (Entry<Integer, RoleCache> e : roleCacheMap.entrySet()) {
			if (roleId == e.getValue().getRoleId()) {
				return e.getValue();
			}
		}
		return null;
	}

	/** 重置索引位 */
	public void resetOperateIndex(int operateIndex) {
		int index = 0;
		if (operateIndex == 0) {
			index = this.operateIndex + 1;
		} else {
			index = operateIndex;
		}
		if (index > ConfigConstant.ROOM_NUM) {
			index = 1;
		}
		this.operateIndex = index;
	}

	/** 生成痞牌，癞子牌 */
	public void initRuffian() {
		while (true) {
			int randomIndex = RandomUtils.nextInt(paindex, roomChess.length - 1);
			ruffian = roomChess[randomIndex];
			if (ruffian.getColor() != MahjongConstant.MAHJONG_FENG) {
				break;
			}
		}
		laiziChess();
	}

	/** 初始化癞子 */
	private void laiziChess() {
		randomLaizi = new ChessEvery();
		if (ruffian.getNumber() <= 9) {
			randomLaizi.setNumber((ruffian.getNumber() % 9) + 1);
		} else if (ruffian.getNumber() <= MahjongConstant.FENG_TYPE_BEI) {
			int temp = ruffian.getNumber() % MahjongConstant.FENG_TYPE_BEI;
			if (temp == 0) {
				randomLaizi.setNumber(MahjongConstant.FENG_TYPE_DONG);
			} else {
				randomLaizi.setNumber(temp + 1);
			}
		} else if (ruffian.getNumber() <= MahjongConstant.FENG_TYPE_BAI) {
			int temp = ruffian.getNumber() % MahjongConstant.FENG_TYPE_BAI;
			if (temp == 0) {
				randomLaizi.setNumber(MahjongConstant.FENG_TYPE_ZHONG);
			} else {
				randomLaizi.setNumber(temp + 1);
			}
		}
		randomLaizi.setColor(ruffian.getColor());
	}

	/** 正常摸到的牌 */
	public ChessEvery getMo() {
		ChessEvery moChess = roomChess[paindex];
		paindex = paindex + 1;
		sumIndex = sumIndex + 1;
		return moChess;
	}

	/** 从后面摸到的牌 */
	public ChessEvery getLastMo() {
		ChessEvery moChess = roomChess[lastIndex];
		lastIndex = lastIndex - 1;
		sumIndex = sumIndex + 1;
		return moChess;
	}

	/**
	 * 牌是否打完了
	 *
	 * @return
	 */
	public boolean allPlayFinish() {
		return this.sumIndex >= roomChess.length;
	}

	/**
	 * 还剩多少张牌
	 *
	 * @return
	 */
	public int getLeftChess() {
		return roomChess.length - this.sumIndex;
	}

	/**
	 * 房间开始了以后。初始化房间牌列表
	 */
	public void startInitRoomChess() {
		int sum = MahjongConstant.MAHJONG_PAI_SUM;
		roomChess = new ChessEvery[sum];

		int b = 1;
		int feng_type = MahjongConstant.FENG_TYPE_DONG;

		for (int i = 0; i < sum; i++) {
			ChessEvery ce = new ChessEvery();
			ce.setId(i + 1);
			// 给每个牌赋花色和数值
			if (i < MahjongConstant.MAHJONG_PAI_NO_FENG_SUM) {
				int a = i + 1;
				ce.setNumber(a % 9 + 1);
				if (i <= 35) {
					ce.setColor(MahjongConstant.MAHJONG_TONG);
				} else if (i >= 36 && i <= 71) {
					ce.setColor(MahjongConstant.MAHJONG_TIAO);
				} else if (i >= 72 && i <= 107) {
					ce.setColor(MahjongConstant.MAHJONG_WAN);
				}
			} else {
				if (b > 4) {
					b = 1;
					feng_type++;
				}
				ce.setNumber(feng_type);
				ce.setColor(MahjongConstant.MAHJONG_FENG);
				b++;
			}
			roomChess[i] = ce;
		}
	}

	/**
	 * 洗牌
	 */
	public void xipai() {
		int size = roomChess.length;
		for (int i = 0; i < size; i++) {
			int j = ((int) (Math.random() * 10000)) % (size - i) + i;
			ChessEvery temp = roomChess[i];
			roomChess[i] = roomChess[j];
			roomChess[j] = temp;
		}
	}

	/**
	 * 给房间里的每个人初始化13张牌
	 *
	 * @param roles
	 */
	public void initThirteenth() {
		// 房间里的牌最大索引。用于杠类操作
		setLastIndex(roomChess.length - 1);

		int initSum = MahjongConstant.MAHJONG_INIT_THIRTEENTH;
		// 目前先这样处理把。第一个角色身上的牌房间的牌列表种拿13张，第二个角色的牌拿第14-26张，以此类推
		for (RoleCache cache : roleCacheMap.values()) {
			cache.init();
			for (int i = 0; i < initSum; i++) {
				ChessEvery c = roomChess[paindex];
				cache.addRoleChess(c, false);
				this.paindex = paindex + 1;
				this.sumIndex = sumIndex + 1;
			}
		}
	}

	/**
	 * 添加回放索引
	 *
	 * @param vo
	 */
	public void addPlayBackMap(PlayBackVo vo) {
		int maxIndex = playBackMaxIndex(true);
		this.playBackMap.put(maxIndex, vo);
	}

	/**
	 * 获取最大的回放索引
	 *
	 * @return
	 */
	public int getPlayBackMaxIndex() {
		return playBackMaxIndex(false);
	}

	/**
	 * 获取最大的回放记录索引
	 *
	 * @param add
	 *            是否是新的记录
	 * @return
	 */
	private int playBackMaxIndex(boolean add) {
		int i = 0;
		for (Integer index : this.playBackMap.keySet()) {
			if (index > i) {
				i = index;
			}
		}
		if (add) {
			return i + 1;
		} else {
			return i;
		}
	}

	/**
	 * 给房间里每个人的牌排序
	 *
	 * @param roles
	 */
	public void sort() {
		for (RoleCache roleCache : roleCacheMap.values()) {
			roleCache.sortRoleChess(false);
		}
	}
}
