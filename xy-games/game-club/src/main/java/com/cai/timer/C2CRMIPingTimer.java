/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.TimerTask;

import com.cai.common.define.EServerType;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.config.SystemConfig;

/**
 * ClubServer ping中心服，保持通讯用
 *
 * @author wu_hc date: 2017年8月7日 下午2:32:12 <br/>
 */
public final class C2CRMIPingTimer extends TimerTask {

	@Override
	public void run() {
		try {
			SpringService.getBean(ICenterRMIServer.class).serverPing(EServerType.CLUB, SystemConfig.club_index);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
