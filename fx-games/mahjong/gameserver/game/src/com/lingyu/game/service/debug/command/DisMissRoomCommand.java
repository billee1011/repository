package com.lingyu.game.service.debug.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.lingyu.game.service.debug.Command;
import com.lingyu.game.service.mahjong.MahjongManager;

/**
 * 解散房间
 */
@Scope("prototype")
@Component("@ dismissroom")
public class DisMissRoomCommand extends Command {
	@Autowired
	private MahjongManager mahjongManager;

	
	@Override
	public void analysis(String... args) {
	}

	@Override
	public void exec() {
		mahjongManager.GMDisMissRoom(roleId);
	}

	@Override
	public String help() {
		return "@ disMissRoom ";
	}
}