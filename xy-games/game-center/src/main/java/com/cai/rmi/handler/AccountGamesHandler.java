package com.cai.rmi.handler;

import java.util.Map;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.core.TaskThreadPool;
import com.cai.future.runnable.AccountGamesRunnble;

/**
 * 
 * 
 *
 * @author tang date: 2018年01月24日 上午10:11:20 <br/>
 */
@IRmi(cmd = RMICmd.ACCOUNT_PLAY_GAME_LIST, desc = "添加玩家玩过的游戏列表")
public final class AccountGamesHandler extends IRMIHandler<Map<String, String>,Integer> {
	@Override
	public Integer execute(Map<String, String> map) {
		AccountGamesRunnble runnable = new AccountGamesRunnble(map);
		TaskThreadPool.getInstance().addTask(runnable);
		return 1;
	}
}
