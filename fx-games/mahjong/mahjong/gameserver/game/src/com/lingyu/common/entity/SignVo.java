package com.lingyu.common.entity;

import java.util.List;

/**
 * 玩家标签类
 * @author wangning
 * @date 2017年1月9日 上午10:28:55
 */
public class SignVo {
	private List<Integer> signList; // 玩家当前可操作的标签
	private int operateSign; // 已经操作的标签类型
	
	public SignVo(List<Integer> signList, int operateSign){
		this.signList = signList;
		this.operateSign = operateSign;
	}
	
	public List<Integer> getSignList() {
		return signList;
	}
	public void setSignList(List<Integer> signList) {
		this.signList = signList;
	}

	public int getOperateSign() {
		return operateSign;
	}

	public void setOperateSign(int operateSign) {
		this.operateSign = operateSign;
	}
}
