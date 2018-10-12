package com.lingyu.game.service.role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.game.service.event.IEventHandler;
import com.lingyu.game.service.event.LoginGameEvent;

@Service
public class RoleEventHandler implements IEventHandler {
	@Autowired
	private RoleManager roleManager;
	
	public void handle(LoginGameEvent event) {
		roleManager.loginGame(event.getRoleId(), event.getIp());
	}

	@Override
	public String getModule() {
		return ModuleConstant.MODULE_ROLE;
	}
}
