/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.TimerTask;

import com.cai.common.constant.S2SCmd;
import com.cai.common.util.PBUtil;
import com.cai.service.SessionServiceImpl;

import protobuf.clazz.s2s.S2SProto.Ping;

/**
 * ping foundation server
 * @author chansonyan
 * 2018年4月17日
 */
public final class FoundationPingTimer extends TimerTask {

	@Override
	public void run() {
		Ping.Builder builder = Ping.newBuilder();
		SessionServiceImpl.getInstance().sendFoundation(PBUtil.toS2SRequet(S2SCmd.PING, builder).build());
	}
}
