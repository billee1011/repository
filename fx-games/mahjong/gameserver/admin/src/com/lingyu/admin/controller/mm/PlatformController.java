package com.lingyu.admin.controller.mm;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.manager.PlatformManager;
import com.lingyu.admin.manager.UserManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.User;

@Controller
@RequestMapping(value = "/mm/platform")
public class PlatformController {
	private static final Logger logger = LogManager.getLogger(PlatformController.class);
	private PlatformManager platformManager;
	private UserManager userManager;

	public void initialize() {
		platformManager = AdminServerContext.getBean(PlatformManager.class);
		userManager = AdminServerContext.getBean(UserManager.class);
	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/list.do", method = { RequestMethod.GET })
	public Collection<Platform> getList() {
		Collection<Platform> list = platformManager.getPlatformList();
		return list;
	}

	/** 获取创建游戏区UI */
	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/create.do", method = { RequestMethod.GET })
	public void toCreate(Model model) {
		Platform platform = new Platform();
//		int id = platformManager.getNextPlatformId();
//		platform.setId(id);
		model.addAttribute("platform", platform);
	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/create.do", method = { RequestMethod.POST })
	public String create(Model model, @ModelAttribute("platform") Platform platform) {
		String retCode = platformManager.create(platform);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/platform/list.do";
		} else {
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/delete.do", method = { RequestMethod.GET })
	public void toDelete(Model model, @RequestParam("id") String id) {
		Platform platform = platformManager.getPlatform(id);
		model.addAttribute("platform", platform);
	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/delete.do", method = { RequestMethod.POST })
	public String delete(@RequestParam("id") String id) {
		String retCode = platformManager.removePlatform(id);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/platform/list.do";
		} else {
			return retCode;
		}

	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/update.do", method = { RequestMethod.GET })
	public void toUpdate(Model model, @RequestParam("id") String id) {
		Platform platform = platformManager.getPlatform(id);
		model.addAttribute("platform", platform);

	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/update.do", method = { RequestMethod.POST })
	public String update(Model model, @ModelAttribute("platform") Platform platform) {
		String retCode = platformManager.update(platform);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/platform/list.do";
		} else {
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/select.do", method = { RequestMethod.POST })
	public String select(Model model, @RequestParam("id") String id) {
		logger.info("选择平台 areaId={}", id);
		String retCode = platformManager.selectPlatform(id);
		if (retCode == ErrorCode.EC_OK) {
			User user = SessionUtil.getCurrentUser();
			userManager.updatePlatformId(user, id);
			return "redirect:/mm/platform/success.do";
		} else {
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/success.do", method = { RequestMethod.GET })
	public void success() {

	}

	@Privilege(value = PrivilegeConstant.MENU_PLATFORM)
	@RequestMapping(value = "/index.do", method = { RequestMethod.GET })
	public String index() {
		return "redirect:/mm/platform/list.do";
	}

}
