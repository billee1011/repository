/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.game.mj.jiangxi.nanchang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NANCHANG;

import protobuf.clazz.mj.Nanchang.ActionData_NC;
import protobuf.clazz.mj.Nanchang.ActionJingDatas;
import protobuf.clazz.mj.Nanchang.EveryJingInfo_NC;

/**
 * 
 * 动画精数据
 *
 * @author WalkerGeek date: 2018年9月6日 下午6:18:46 <br/>
 */
class ActionJinData {
	List<Integer> showCenterCards; // 中间显示的牌
	List<EveryJingInfo> jingInfos; // 每个玩家对应的显示牌数据 下标对应玩家index
	int count; // 发电显示次数
	boolean touZi; // 是否需要骰子动画
	int target; // 对应玩家
	int touZiOne; // 骰子1
	int touZiTwo; // 骰子2
	List<Integer> otherCards; // 用于显示左右上下精
	boolean[] tingPai; // 听牌

	public void addShowCenterCards(int cards) {
		showCenterCards.add(cards);
	}

	public void addJingInfos(EveryJingInfo info) {
		jingInfos.add(info);
	}

	public void addOtherCards(int cards) {
		otherCards.add(cards);
	}

	public boolean getTingPai() {
		if (tingPai == null) {
			return false;
		}
		for (int i = 0; i < tingPai.length; i++) {
			if (tingPai[i]) {
				return true;
			}
		}
		return false;
	}

	public boolean getTingPaiForIndex(int i) {
		return tingPai[i];
	}

	/**
	 * 
	 * @param playerCardData
	 * @param playerNum
	 * @param zheng
	 * @param fu
	 * @param faDianCount
	 * @param seat_index
	 * @param touZis
	 * @param other
	 * @param type
	 */
	public ActionJinData(int[][] playerCardData, int playerNum, int zheng, int fu, int faDianCount, int seat_index,
			List<Integer> touZis, List<Integer> other, int type) {
		showCenterCards = new ArrayList<Integer>();
		jingInfos = new ArrayList<EveryJingInfo>();
		otherCards = new ArrayList<Integer>();

		showCenterCards.add(zheng);
		showCenterCards.add(fu);
		for (int i = 0; i < playerNum; i++) {
			int cards[] = playerCardData[i];
			EveryJingInfo everyJingInfo = new EveryJingInfo(cards, zheng, fu);
			jingInfos.add(everyJingInfo);
		}
		int count = 0;
		int indexTemp = -1;
		for (int i = 0; i < jingInfos.size(); i++) {
			EveryJingInfo info = jingInfos.get(i);
			if ((info.getFuJingCount() + info.getZhengJingCount()) > 0) {
				count++;
				indexTemp = i;
			}
		}
		//发电计算原始值
		tingPai = new boolean[playerNum];
		Arrays.fill(tingPai, false);
		for (int i = 0; i < jingInfos.size(); i++) {
			EveryJingInfo info = jingInfos.get(i);
			if (info.getEveryJingScore() >= 3) {
				tingPai[i] = true;
			}
		}
		if (count == 1) {
			jingInfos.get(indexTemp).setBaWangJing(true);
			int s = jingInfos.get(indexTemp).getEveryJingScore();
			if (type == Constants_MJ_NANCHANG.BA_WANG_JING_TYPE_FANGBEI) {
				s *= 2;
			} else if (type == Constants_MJ_NANCHANG.BA_WANG_JING_TYPE_JIA10) {
				s += 10;
			}
			jingInfos.get(indexTemp).setEveryJingScore(s);
			jingInfos.get(indexTemp).setOtherDelScore(s);
		}

		for (int i = 0; i < playerNum; i++) {
			jingInfos.get(i).setEveryJingScore(0);
		}
		// 计算每个玩家的输赢精分
		for (int i = 0; i < playerNum; i++) {
			int score = 0;
			for (int u = 0; u < playerNum; u++) {
				if (u == i) {
					continue;
				}
				score += jingInfos.get(u).getOtherDelScore();
				jingInfos.get(u).setEveryJingScore(jingInfos.get(u).getEveryJingScore() + jingInfos.get(u).getOtherDelScore());
			}
			jingInfos.get(i).setEveryJingScore(jingInfos.get(i).getEveryJingScore() - score);
		}

		if (touZis != null) {
			touZi = true;
			touZiOne = touZis.get(0);
			touZiTwo = touZis.get(1);
		} else {
			touZi = false;
		}
		if (other != null) {
			otherCards = other;
		}
		if (seat_index != GameConstants.INVALID_SEAT) {
			target = seat_index;
		}
		this.count = faDianCount;
		

	}

	public ActionJingDatas.Builder bulidPbJingData() {
		ActionJingDatas.Builder jingData = ActionJingDatas.newBuilder();
		showCenterCards.forEach((showCard) -> {
			jingData.addShowCenterCards(showCard);
		});
		jingInfos.forEach((data) -> {
			jingData.addJingInfo(data.bulidPbJingInfo());
		});
		jingData.setCount(count);
		jingData.setTouZi(touZi);
		jingData.setTarget(target);
		jingData.setTouZiOne(touZiOne);
		jingData.setTouZiTwo(touZiTwo);
		otherCards.forEach((data)->{
			jingData.addOtherCards(data.intValue());
		});
		return jingData;
	}

	/**
	 * @return the showCenterCards
	 */
	public List<Integer> getShowCenterCards() {
		return showCenterCards;
	}

	/**
	 * @param showCenterCards
	 *            the showCenterCards to set
	 */
	public void setShowCenterCards(List<Integer> showCenterCards) {
		this.showCenterCards = showCenterCards;
	}

	/**
	 * @return the jingInfos
	 */
	public List<EveryJingInfo> getJingInfos() {
		return jingInfos;
	}

	/**
	 * @param jingInfos
	 *            the jingInfos to set
	 */
	public void setJingInfos(List<EveryJingInfo> jingInfos) {
		this.jingInfos = jingInfos;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @param count
	 *            the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @return the touZi
	 */
	public boolean isTouZi() {
		return touZi;
	}

	/**
	 * @param touZi
	 *            the touZi to set
	 */
	public void setTouZi(boolean touZi) {
		this.touZi = touZi;
	}

	/**
	 * @return the target
	 */
	public int getTarget() {
		return target;
	}

	/**
	 * @param target
	 *            the target to set
	 */
	public void setTarget(int target) {
		this.target = target;
	}

	/**
	 * @return the touZiOne
	 */
	public int getTouZiOne() {
		return touZiOne;
	}

	/**
	 * @param touZiOne
	 *            the touZiOne to set
	 */
	public void setTouZiOne(int touZiOne) {
		this.touZiOne = touZiOne;
	}

	/**
	 * @return the touZiTwo
	 */
	public int getTouZiTwo() {
		return touZiTwo;
	}

	/**
	 * @param touZiTwo
	 *            the touZiTwo to set
	 */
	public void setTouZiTwo(int touZiTwo) {
		this.touZiTwo = touZiTwo;
	}

	/**
	 * @return the otherCards
	 */
	public List<Integer> getOtherCards() {
		return otherCards;
	}

	/**
	 * @param otherCards
	 *            the otherCards to set
	 */
	public void setOtherCards(List<Integer> otherCards) {
		this.otherCards = otherCards;
	}

}

/**
 * 每个玩家精的数据
 * 
 *
 * @author WalkerGeek date: 2018年9月6日 下午6:19:58 <br/>
 */
class EveryJingInfo {
	List<Integer> showCards; // 显示的牌
	int zhengJingCount; // 正精的个数
	int fuJingCount; // 副精的个数
	boolean isBaWangJing; // 是否霸王精 (true是 false否)
	int chongGuanScore; // 精的冲关(0表示没有冲关, >1 表示冲关情况)
	int everyJingScore; // 得分
	int otherDelScore; // 其他玩家减分

	public void addShowCards(int cards) {
		showCards.add(cards);
	}

	public EveryJingInfo_NC.Builder bulidPbJingInfo() {
		EveryJingInfo_NC.Builder jingInfo = EveryJingInfo_NC.newBuilder();
		showCards.forEach((card) -> {
			jingInfo.addShowCards(card);
		});
		jingInfo.setZhengJingCount(zhengJingCount);
		jingInfo.setFuJingCount(fuJingCount);
		jingInfo.setIsBaWangJing(isBaWangJing);
		jingInfo.setChongGuanScore(chongGuanScore);
		jingInfo.setEveryJingScore(everyJingScore);
		return jingInfo;
	}

	public EveryJingInfo() {
	}

	public EveryJingInfo(int cards[], int zheng, int fu) {
		showCards = new ArrayList<Integer>();
		for (int i = 0; i < cards.length; i++) {
			if (cards[i] == 0) {
				continue;
			}
			if (cards[i] == zheng) {
				zhengJingCount++;
				everyJingScore += 2;
				showCards.add(zheng);
			}
			if (cards[i] == fu) {
				fuJingCount++;
				everyJingScore++;
			}

		}

		for (int i = 0; i < cards.length; i++) {
			if (cards[i] == 0) {
				continue;
			}
			if (cards[i] == fu) {
				showCards.add(fu);
			}
		}
		if (everyJingScore >= 5) {
			chongGuanScore = everyJingScore = everyJingScore * (everyJingScore - 3);
		}
		otherDelScore = everyJingScore;
		isBaWangJing = false;
	}

	/**
	 * @return the showCards
	 */
	public List<Integer> getShowCards() {
		return showCards;
	}

	/**
	 * @param showCards
	 *            the showCards to set
	 */
	public void setShowCards(List<Integer> showCards) {
		this.showCards = showCards;
	}

	/**
	 * @return the zhengJingCount
	 */
	public int getZhengJingCount() {
		return zhengJingCount;
	}

	/**
	 * @param zhengJingCount
	 *            the zhengJingCount to set
	 */
	public void setZhengJingCount(int zhengJingCount) {
		this.zhengJingCount = zhengJingCount;
	}

	/**
	 * @return the fuJingCount
	 */
	public int getFuJingCount() {
		return fuJingCount;
	}

	/**
	 * @param fuJingCount
	 *            the fuJingCount to set
	 */
	public void setFuJingCount(int fuJingCount) {
		this.fuJingCount = fuJingCount;
	}

	/**
	 * @return the isBaWangJing
	 */
	public boolean isBaWangJing() {
		return isBaWangJing;
	}

	/**
	 * @param isBaWangJing
	 *            the isBaWangJing to set
	 */
	public void setBaWangJing(boolean isBaWangJing) {
		this.isBaWangJing = isBaWangJing;
	}

	/**
	 * @return the chongGuanScore
	 */
	public int getChongGuanScore() {
		return chongGuanScore;
	}

	/**
	 * @param chongGuanScore
	 *            the chongGuanScore to set
	 */
	public void setChongGuanScore(int chongGuanScore) {
		this.chongGuanScore = chongGuanScore;
	}

	/**
	 * @return the everyJingScore
	 */
	public int getEveryJingScore() {
		return everyJingScore;
	}

	/**
	 * @param everyJingScore
	 *            the everyJingScore to set
	 */
	public void setEveryJingScore(int everyJingScore) {
		this.everyJingScore = everyJingScore;
	}

	/**
	 * @return the otherDelScore
	 */
	public int getOtherDelScore() {
		return otherDelScore;
	}

	/**
	 * @param otherDelScore
	 *            the otherDelScore to set
	 */
	public void setOtherDelScore(int otherDelScore) {
		this.otherDelScore = otherDelScore;
	}

}

/**
 * 
 * 动画基类
 * 
 * @author WalkerGeek date: 2018年9月6日 下午4:07:16 <br/>
 */
// 动画数据
public class ActionDataModel {
	List<Integer> actionType; // 动画类型数组
	List<ActionJinData> actionJingDatas; // 动作的精牌数据(actionTypes下标)

	public ActionDataModel() {
		actionType = new ArrayList<Integer>();
		actionJingDatas = new ArrayList<ActionJinData>();
	}

	public void addActionType(int type) {
		actionType.add(type);
	}

	public void addActionJinData(ActionJinData actionJinData) {
		actionJingDatas.add(actionJinData);
	}

	public ActionData_NC.Builder bulidPbActionData() {
		ActionData_NC.Builder actionData = ActionData_NC.newBuilder();
		actionType.forEach((type) -> {
			actionData.addActionType(type);
		});
		actionJingDatas.forEach((data) -> {
			actionData.addActionJingDatas(data.bulidPbJingData());
		});
		return actionData;
	}
	
	public ActionData_NC.Builder bulidPbActionData(ActionData_NC.Builder builder) {
		actionType.forEach((type) -> {
			builder.addActionType(type);
		});
		actionJingDatas.forEach((data) -> {
			builder.addActionJingDatas(data.bulidPbJingData());
		});
		return builder;
	}

	/**
	 * @return the actionType
	 */
	public List<Integer> getActionType() {
		return actionType;
	}

	/**
	 * @param actionType
	 *            the actionType to set
	 */
	public void setActionType(List<Integer> actionType) {
		this.actionType = actionType;
	}

	/**
	 * @return the actionJingDatas
	 */
	public List<ActionJinData> getActionJingDatas() {
		return actionJingDatas;
	}

	/**
	 * @param actionJingDatas
	 *            the actionJingDatas to set
	 */
	public void setActionJingDatas(List<ActionJinData> actionJingDatas) {
		this.actionJingDatas = actionJingDatas;
	}

}
