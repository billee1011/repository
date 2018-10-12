/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import com.cai.common.constant.S2SCmd;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PBUtil;
import com.cai.service.SessionServiceImpl;

import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto;

/**
 * 
 * 用于确保提交成功
 *
 * @author wu_hc date: 2018年5月18日 下午12:16:13 <br/>
 */
public final class ClubRecordEnsureTask implements Runnable {

	/**
	 * 最大重试次数
	 */
	private static final int MAX_TRY_TIME = 30;

	/**
	 * 尝试提交次数
	 */
	private int tryTime = 0;

	private final ClubGameRecordProto.Builder recordBuidler;

	/**
	 * @param recordBuidler
	 */
	public ClubRecordEnsureTask(ClubGameRecordProto.Builder recordBuidler) {
		this.recordBuidler = recordBuidler;
	}

	@Override
	public void run() {
		if (tryTime++ > MAX_TRY_TIME) {
			return;
		}

		boolean ret = SessionServiceImpl.getInstance().sendClub(1, PBUtil.toS2SRequet(S2SCmd.CLUB_GAME_RECORD_REQ, recordBuidler).build());
		if (!ret) {
			GlobalExecutor.schedule(this, 5000L * tryTime);
		}
	}
}
