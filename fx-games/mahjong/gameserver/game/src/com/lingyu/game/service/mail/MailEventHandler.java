package com.lingyu.game.service.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.RoleRedeemInfo;
import com.lingyu.game.service.event.IEventHandler;
import com.lingyu.game.service.event.LoginGameEvent;
import com.lingyu.game.service.role.RoleManager;

@Service
public class MailEventHandler implements IEventHandler {
	@Autowired
	private MailManager mailManager;
	@Autowired
	private SystemMailManager systemMailManager;
	@Autowired
	private RoleManager roleManager;
	
	public void handle(LoginGameEvent event) {
		// 删除多余的邮件
		mailManager.simplifyMailList(event.getRoleId());

		Role role = roleManager.getRole(event.getRoleId());
		RoleRedeemInfo roleRedeemInfo = mailManager.getRedeemInfoTryUpdate(event.getRoleId(), null);
		// 系统邮件发送 检测他之前有没有收到系统邮件。没有的话。上线了。要给他发~
		systemMailManager.checkSystemMail(role, roleRedeemInfo);
	}

	@Override
	public String getModule() {
		return ModuleConstant.MODULE_MAIL;
	}
}
