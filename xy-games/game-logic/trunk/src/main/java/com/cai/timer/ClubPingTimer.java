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
 * ping club server
 *
 * @author wu_hc date: 2017年9月1日 下午12:28:52 <br/>
 */
public final class ClubPingTimer extends TimerTask {

	@Override
	public void run() {
		Ping.Builder builder = Ping.newBuilder();
		SessionServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.PING, builder).build());
	}
}
