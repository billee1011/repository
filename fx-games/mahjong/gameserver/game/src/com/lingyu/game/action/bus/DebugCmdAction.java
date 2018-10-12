package com.lingyu.game.action.bus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.message.GameAction;
import com.lingyu.common.message.GameMapping;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.debug.DebugCommandManager;

/**
 * GM 命令执行
 */
@Controller
@GameAction(module = ModuleConstant.MODULE_DEBUG, group = SystemConstant.GROUP_BUS_CACHE)
public class DebugCmdAction {

	@Autowired
	private DebugCommandManager debugCommandManager;
	
	/**
	 * 调试命令. 
	 */
	@GameMapping(value = MsgType.MAHJONG_GM_MSG)
	public void debugCommand(long roleId, Object[] data) {
		String m = (String) data[0];
		debugCommandManager.handle(roleId, m);
	}
}
