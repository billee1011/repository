package com.lingyu.common.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleCache {
	private static final Logger logger = LogManager.getLogger(RoleCache.class);

	private long roleId; // 角色唯一标示
	private String name; // 角色名
	private String headimgurl;
	private String ip; // 角色ip
	private int jifen = 0; // 角色总积分
	// 角色手里的牌 4杠一开花。最多18张
	private List<ChessEvery> roleChessList = new ArrayList<>(18);
	// 角色打出去的牌
	private List<ChessEvery> outChessList = new ArrayList<>();
	// 每一局的杠分
	private int oneGangfen;
	// 每一局的总分
	private int oneSumfen;
	// 每一局的番薯
	private int fan;
	// 每一局 暗杠次数
	private int oneAnGangCount;
	// 每一局 明杠次数
	private int oneMingGangCount;
	// 每一局 碰次数
	private int onePengCount;
	// 每一局 吃次数
	private int oneEatCount;
	// 每一局 红中杠次数
	private int oneHzGangCount;
	// 每一局 痞子杠次数
	private int oneRuffianGangCount;
	// 每一局 癞子杠次数
	private int oneLaiZiGangCount;
	// 每一局 胡牌还有癞子数
	private int oneHaveLzCount;
	// 是否在房间中
	private boolean isInRoom;

	private int ziMoCount; // 自摸次数
	private int jiePaoCount; // 接炮次数
	private int dianPaoCount; // 点炮次数
	private int anGangCount; // 暗杠次数
	private int mingGangCount; // 明杠次数

	// 是否有过路杠
	private boolean guolugang;

	// 是否弹了标签提示
	private boolean tan;

	// 当前局下的积分变动
	private int changeJifen;

	// 当前局占据的唯一编号
	private long curJuLogId;

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getOneHaveLzCount() {
		return oneHaveLzCount;
	}

	public void setOneHaveLzCount(int oneHaveLzCount) {
		this.oneHaveLzCount = oneHaveLzCount;
	}

	public int getJifen() {
		return jifen;
	}

	public String getHeadimgurl() {
		return headimgurl;
	}

	public void setHeadimgurl(String headimgurl) {
		this.headimgurl = headimgurl;
	}

	public void setJifen(int jifen) {
		this.jifen = jifen;
	}

	public int getZiMoCount() {
		return ziMoCount;
	}

	public boolean isInRoom() {
		return isInRoom;
	}

	public void setInRoom(boolean isInRoom) {
		this.isInRoom = isInRoom;
	}

	public void setZiMoCount(int ziMoCount) {
		this.ziMoCount = ziMoCount;
	}

	public int getJiePaoCount() {
		return jiePaoCount;
	}

	public void setJiePaoCount(int jiePaoCount) {
		this.jiePaoCount = jiePaoCount;
	}

	public int getDianPaoCount() {
		return dianPaoCount;
	}

	public void setDianPaoCount(int dianPaoCount) {
		this.dianPaoCount = dianPaoCount;
	}

	public int getAnGangCount() {
		return anGangCount;
	}

	public void setAnGangCount(int anGangCount) {
		this.anGangCount = anGangCount;
	}

	public int getMingGangCount() {
		return mingGangCount;
	}

	public void setMingGangCount(int mingGangCount) {
		this.mingGangCount = mingGangCount;
	}

	public boolean isGuolugang() {
		return guolugang;
	}

	public void setGuolugang(boolean guolugang) {
		this.guolugang = guolugang;
	}

	public boolean isTan() {
		return tan;
	}

	public void setTan(boolean tan) {
		this.tan = tan;
	}

	public int getChangeJifen() {
		return changeJifen;
	}

	public void setChangeJifen(int changeJifen) {
		this.changeJifen = changeJifen;
	}

	public long getCurJuLogId() {
		return curJuLogId;
	}

	public void setCurJuLogId(long curJuLogId) {
		this.curJuLogId = curJuLogId;
	}

	public int getFan() {
		return fan;
	}

	public void setFan(int fan) {
		this.fan = fan;
	}

	/** 初始化信息 */
	public void init() {
		this.roleChessList = new ArrayList<>();
		this.outChessList = new ArrayList<>();
		this.guolugang = false;
		this.tan = false;
		this.changeJifen = 0;
		this.oneGangfen = 0;
		this.oneSumfen = 0;
		this.oneHaveLzCount = 0;

		this.oneAnGangCount = 0;
		this.oneMingGangCount = 0;
		this.oneHzGangCount = 0;
		this.oneRuffianGangCount = 0;
		this.oneLaiZiGangCount = 0;
		this.onePengCount = 0;
		this.oneEatCount = 0;
	}

	/**
	 * 摸牌
	 *
	 * @param chessEvery
	 * @param needSort
	 *            是否需要排序
	 */
	public void addRoleChess(ChessEvery chessEvery, boolean needSort) {
		roleChessList.add(chessEvery);
		if (needSort) {
			// 因为要生成临时的一个roleCache,而且要排序。this.jifen == 0是为了看是真玩家还是临时的temp
			sortRoleChess(this.jifen == 0);
		}
	}

	public void copyRoleChess(List<ChessEvery> roleChessList) {
		this.roleChessList = roleChessList;
	}

	/** 删除我身上的牌 */
	public void removeRoleChess(ChessEvery chessEvery) {
		roleChessList.remove(chessEvery);
	}

	public List<ChessEvery> getRoleChessList() {
		return roleChessList;
	}

	/** 打出去的牌 */
	public void addOutChess(ChessEvery chessEvery) {
		outChessList.add(chessEvery);
	}

	public int getOneAnGangCount() {
		return oneAnGangCount;
	}

	public void setOneAnGangCount(int oneAnGangCount) {
		this.oneAnGangCount = oneAnGangCount;
	}

	public int getOneMingGangCount() {
		return oneMingGangCount;
	}

	public void setOneMingGangCount(int oneMingGangCount) {
		this.oneMingGangCount = oneMingGangCount;
	}

	public int getOnePengCount() {
		return onePengCount;
	}

	public void setOnePengCount(int onePengCount) {
		this.onePengCount = onePengCount;
	}

	public int getOneEatCount() {
		return oneEatCount;
	}

	public void setOneEatCount(int oneEatCount) {
		this.oneEatCount = oneEatCount;
	}

	public int getOneHzGangCount() {
		return oneHzGangCount;
	}

	public void setOneHzGangCount(int oneHzGangCount) {
		this.oneHzGangCount = oneHzGangCount;
	}

	public int getOneRuffianGangCount() {
		return oneRuffianGangCount;
	}

	public void setOneRuffianGangCount(int oneRuffianGangCount) {
		this.oneRuffianGangCount = oneRuffianGangCount;
	}

	public int getOneLaiZiGangCount() {
		return oneLaiZiGangCount;
	}

	public void setOneLaiZiGangCount(int oneLaiZiGangCount) {
		this.oneLaiZiGangCount = oneLaiZiGangCount;
	}

	/** 删除我打出去的牌 */
	public void removeOutChess(ChessEvery chessEvery) {
		outChessList.remove(chessEvery);
	}

	public List<ChessEvery> getOutChessList() {
		return outChessList;
	}

	public int getOneGangfen() {
		return oneGangfen;
	}

	public void setOneGangfen(int oneGangfen) {
		this.oneGangfen = oneGangfen;
	}

	public int getOneSumfen() {
		return oneSumfen;
	}

	public void setOneSumfen(int oneSumfen) {
		this.oneSumfen = oneSumfen;
	}

	public List<ChessEvery> getHeadChess() {
		List<ChessEvery> headChess = new ArrayList<>();
		for (ChessEvery ce : roleChessList) {
			if (!ce.isUsed()) {
				headChess.add(ce);
			}
		}
		return headChess;
	}

	/**
	 * 根据牌的唯一编号得到vo
	 *
	 * @param id
	 * @return
	 */
	public ChessEvery getChessEveryById(int id) {
		for (ChessEvery ce : roleChessList) {
			if (ce.getId() == id) {
				return ce;
			}
		}
		return null;
	}

	public void sortRoleChess(boolean temp) {
		List<ChessEvery> list = roleChessList;
		Collections.sort(list, new Comparator<ChessEvery>() {
			@Override
			public int compare(ChessEvery c1, ChessEvery c2) {
				if (c1.getColor() < c2.getColor()) {
					return -1;
				}
				if (c1.getColor() == c2.getColor() && c1.getNumber() < c2.getNumber()) {
					return -1;
				}
				return 0;
			}
		});

		logger.info("sortchesses,roleId={},isTemp={}, name={},chesses={}", this.getRoleId(), temp, this.getName(),
		        loggerInitOutput());
	}

	/** 输出角色初始化的牌 */
	private String loggerInitOutput() {
		List<ChessEvery> list = this.getRoleChessList();
		String strs = "";
		for (ChessEvery c : list) {
			String str = "";
			if (c.getColor() == 1) {
				str = "筒";
			} else if (c.getColor() == 2) {
				str = "条";
			} else if (c.getColor() == 3) {
				str = "万";
			}
			String aa = "";
			if (c.isUsed()) {
				aa = "t";
			} else {
				aa = "f";
			}
			strs += c.getNumber() + str + "[" + aa + c.getId() + "] ";
		}
		return strs;
	}
}
