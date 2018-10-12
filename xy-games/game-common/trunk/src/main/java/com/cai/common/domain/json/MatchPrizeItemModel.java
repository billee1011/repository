package com.cai.common.domain.json;

import java.io.Serializable;

import protobuf.clazz.match.MatchClientRsp.MatchRankItemProto;
import protobuf.clazz.match.MatchClientRsp.MatchRankPrizeConfigProto.Builder;

public class MatchPrizeItemModel  implements Serializable{

	/**
	 */
	private static final long serialVersionUID = 1L;
	
	private int prizeId;
	
	private String prizeName;
	
	private int count;
	private String icon;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getPrizeId() {
		return prizeId;
	}

	public void setPrizeId(int prizeId) {
		this.prizeId = prizeId;
	}

	public void encodeClient(Builder prizeBuilder) {
		prizeBuilder.addItems(encodeClient());
	}
	
	public MatchRankItemProto.Builder encodeClient(){
		MatchRankItemProto.Builder b = MatchRankItemProto.newBuilder();
		b.setCount(count);
		b.setItemId(prizeId);
		b.setName(prizeName);
		b.setIcon(icon != null ? icon : "");
		return b;
	}

	public String getPrizeName() {
		return prizeName;
	}

	public void setPrizeName(String prizeName) {
		this.prizeName = prizeName;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
	

}
