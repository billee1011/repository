package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.common.GameNoticeMsg;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.dictionary.NoticeDict;

/**
 * 验证是否绑定手机号码
 * @author chansonyan
 * 2018年5月7日
 */
@IRmi(cmd = RMICmd.SEND_GAME_NOTICE, desc = "发送游戏跑马灯")
public final class SendGameNoticeHandler extends IRMIHandler<GameNoticeMsg, Boolean> {

	@Override
	protected Boolean execute(GameNoticeMsg msg) {
		NoticeDict.INSTANCE().sendNotice(msg);
		return true;
	}
}
