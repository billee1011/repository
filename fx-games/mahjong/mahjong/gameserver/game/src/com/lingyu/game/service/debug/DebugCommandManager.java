package com.lingyu.game.service.debug;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.lingyu.game.GameServerContext;

/**
 * Debug命令管理类.
 * 
 * @author 小流氓<zhoumingkai@lingyuwangluo.com>
 */
@Service
public class DebugCommandManager {
	private static final Logger logger = LogManager.getLogger(DebugCommandManager.class);

	private static Set<String> cmds = new HashSet<String>();

	static {// 如果有两个词为Key的需将第一个词记录在册
		cmds.add("add");// 添加系列
		cmds.add("reset");// 重置系列
	}

	private boolean canUse(long roleId) {
		if (GameServerContext.getAppConfig().isDebug()) {
			return true;
		}
		return false;
	}

	public void handle(long roleId, String m) {
		logger.warn("GM command roleId={},cmd={}", roleId, m);
		if (!this.canUse(roleId)) {
			return;
		}
		String[] args = StringUtils.split(m, " ");
		boolean first = cmds.contains(args[1]);
		StringBuilder cmdName = new StringBuilder(26);
		if (first) {
			cmdName.append(args[0]).append(" ").append(args[1].toLowerCase()).append(" ").append(args[2].toLowerCase());
		} else {
			cmdName.append(args[0]).append(" ").append(args[1].toLowerCase());
		}
		String beanName = cmdName.toString();
		if (!GameServerContext.getAppContext().containsBeanDefinition(beanName)) {
			return;
		}
		Command cmd = GameServerContext.getAppContext().getBean(beanName, Command.class);
		cmd.setRoleIdAndStageId(roleId);
		if ((first && args.length > 3 && ("help".equals(args[3]) || "?".equals(args[3])))
				|| (args.length > 2 && ("help".equals(args[2]) || "?".equals(args[2])))) {
			cmd.send(cmd.help());
		} else {
			boolean analysis = true;
			// 解析参数
			try {
				cmd.analysis(args);
			} catch (Exception e) {
				analysis = false;
				cmd.send("参数解析错误：" + e.getMessage());
				cmd.send(cmd.help());
				logger.warn("analysis Exception-", e.getMessage());
			}
			// 执行命令逻辑
			try {
				// FIXME 当前gm命令需要指出跨服不咯
				if (analysis) {
						cmd.exec();
				}
			} catch (Exception e) {
				cmd.send("执行异常：" + cmd.help());
				logger.warn("Exec Exception-", e);
			}
		}
	}
}
