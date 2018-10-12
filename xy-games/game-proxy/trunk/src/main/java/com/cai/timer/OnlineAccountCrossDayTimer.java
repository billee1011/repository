/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.TimerTask;

/**
 * 在线玩家跨天记录当天在线时长timer
 * 存在一定时间误差
 * @author chansonyan
 * 2018年6月29日
 */
public final class OnlineAccountCrossDayTimer extends TimerTask {

	@Override
	public void run() {
		//C2SSessionService.getInstance().calOnlineTimeByCrossDay();
	}
}
