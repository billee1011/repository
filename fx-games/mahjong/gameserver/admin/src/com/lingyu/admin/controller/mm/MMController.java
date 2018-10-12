package com.lingyu.admin.controller.mm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.controller.os.OSController;
import com.lingyu.admin.manager.PlatformManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.User;

/**
 * 后台管理Controller
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Controller
@RequestMapping(value = "/mm")
public class MMController {
	private static final Logger logger = LogManager.getLogger(OSController.class);
	private PlatformManager platformManager;

	public void initialize() {
		platformManager=AdminServerContext.getBean(PlatformManager.class);
	}
	/** 后台管理主页面UI */
	@Privilege
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void indexUI(Model model) {
		logger.debug("后台管理主页面UI");
		User user = SessionUtil.getCurrentUser();
		
		Platform platform=platformManager.getPlatform(user.getLastPid());
		model.addAttribute("platform", platform);
	}
}