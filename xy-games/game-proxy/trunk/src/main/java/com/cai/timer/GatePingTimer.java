/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.TimerTask;

import com.cai.common.constant.S2SCmd;
import com.cai.common.util.PBUtil;
import com.cai.service.ClientServiceImpl;

import protobuf.clazz.Protocol;
import protobuf.clazz.s2s.S2SProto.Ping;

/**
 * ping club server
 *
 * @author wu_hc date: 2017年9月1日 下午12:28:52 <br/>
 */
public final class GatePingTimer extends TimerTask {

	static final Protocol.Request.Builder builder = PBUtil.toS2SRequet(S2SCmd.PING, Ping.newBuilder());
	@Override
	public void run() {
		ClientServiceImpl.getInstance().sendAllGate(builder.build());
	}
}
