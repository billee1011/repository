package com.cai.common.domain.match;

import java.io.Serializable;

/**
 * match_choice的冲榜类型信息
 * @author wuhaoran
 */
public class MatchChoiceRankTypeInfo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String rankType;			//榜单类型(逗号分隔: 1日榜2周榜3月榜)
	private String rankGameBgImage;		//冲榜赛子游戏背景图
	private String prizeImage;			//冲榜赛奖励图
	private String prizeDesc;			//冲榜赛奖励描述
	
	public String getRankType() {
		return rankType;
	}
	public void setRankType(String rankType) {
		this.rankType = rankType;
	}
	public String getRankGameBgImage() {
		return rankGameBgImage;
	}
	public void setRankGameBgImage(String rankGameBgImage) {
		this.rankGameBgImage = rankGameBgImage;
	}
	public String getPrizeImage() {
		return prizeImage;
	}
	public void setPrizeImage(String prizeImage) {
		this.prizeImage = prizeImage;
	}
	public String getPrizeDesc() {
		return prizeDesc;
	}
	public void setPrizeDesc(String prizeDesc) {
		this.prizeDesc = prizeDesc;
	}
	
}
