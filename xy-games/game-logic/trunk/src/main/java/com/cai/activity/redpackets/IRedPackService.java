package com.cai.activity.redpackets;

import com.cai.common.domain.Room;

public interface IRedPackService {

	/**
	 * 是否在活动期内
	 * 
	 * @return
	 */
	public boolean isStart();

	/**
	 * 获取红包雨类型
	 * 
	 * @param type
	 * @return
	 */
	public int getRedPackageActivitType();

	/**
	 * 验证发红包的资格
	 * 
	 * @param room
	 */
	public boolean checkReadPackReward(Room room);
}
