package com.lingyu.game.service.debug.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.lingyu.game.service.debug.Command;
import com.lingyu.game.service.mahjong.MahjongManager;

/**
 * 改变最后一张牌属性，用于杠开
 */
@Scope("prototype")
@Component("@ lastchess")
public class ChangeLastChessCommand extends Command {
	@Autowired
	private MahjongManager mahjongManager;

	private int c;
	private int n;
	
	@Override
	public void analysis(String... args) {
		String strs[] = args[2].split(",");
		this.c = Integer.valueOf(strs[0]);
		this.n = Integer.valueOf(strs[1]);
	}

	@Override
	public void exec() {
		String str = mahjongManager.GMChangeLastChess(roleId, c, n);
		if(str != null){
			this.send(str);
		}
	}

	@Override
	public String help() {
		return "@ lastchess";
	}
}