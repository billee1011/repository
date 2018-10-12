package com.lingyu.admin.controller.mm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.manager.PlatformManager;
import com.lingyu.admin.manager.PrivilegeManager;
import com.lingyu.admin.manager.RoleManager;
import com.lingyu.admin.manager.UserManager;
import com.lingyu.admin.manager.UserPlatformManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.ItemVo;
import com.lingyu.admin.vo.MenuVO;
import com.lingyu.admin.vo.ModuleVO;
import com.lingyu.admin.vo.PlatformVo;
import com.lingyu.admin.vo.RoleVO;
import com.lingyu.admin.vo.UserVo;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.User;
import com.lingyu.common.entity.UserPlatform;

@Controller
@RequestMapping(value = "/mm/user")
public class UserController {
	private static final Logger logger = LogManager.getLogger(UserController.class);
	private UserManager userManager;
	private UserPlatformManager userPlatformManager;
	private PlatformManager platformManager;
	private PrivilegeManager privilegeManager;
	private RoleManager roleManager;
	private GameAreaManager gameAreaManager;

	public void initialize() {
		userManager = AdminServerContext.getBean(UserManager.class);
		userPlatformManager = AdminServerContext.getBean(UserPlatformManager.class);
		privilegeManager = AdminServerContext.getBean(PrivilegeManager.class);
		roleManager = AdminServerContext.getBean(RoleManager.class);
		gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
		platformManager = AdminServerContext.getBean(PlatformManager.class);
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/list.do", method = { RequestMethod.GET })
	public List<UserVo> getList() {
		Collection<User> list = userManager.getUserList();
		List<UserVo> ret = new ArrayList<UserVo>();
		for (User user : list) {
			String lastAreaName = null;
			GameArea gameArea = gameAreaManager.getGameAreaByAreaId(user.getLastPid(), user.getLastAreaId());
			if (gameArea != null) {
				lastAreaName = gameArea.getAreaName();
			} else {
				lastAreaName = String.valueOf(user.getLastAreaId());
			}
			String roleName = null;
			Role role = roleManager.getRole(user.getRoleId());
			if (role != null) {
				roleName = role.getName();
			} else {
				roleName = String.valueOf(user.getRoleId());
			}
			ret.add(user.toUserVo(lastAreaName, roleName));
		}

		// 放入平台ID
		for (UserVo userVo : ret) {
			List<String> adminUserPids = userPlatformManager.getPlatformIdListByUserId(userVo.getId());
			userVo.setPlatformIdList(adminUserPids);
		}
		return ret;
	}

	/** 获取创建游戏区UI */
	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/create.do", method = { RequestMethod.GET })
	public void toCreate(Model model) {
		List<RoleVO> list = roleManager.getRoleVOList();
		model.addAttribute("roleList", list);
		User user = new User();
		if (CollectionUtils.isNotEmpty(list)) {
			user.setRoleId(list.get(0).getId());
		}
		User adminUser = SessionUtil.getCurrentUser();
		List<String> adminUserPids = userPlatformManager.getPlatformIdListByUserId(adminUser.getId());
		user.setPlatformIdList(adminUserPids);
		model.addAttribute("user", user);

		List<ItemVo> platformList = new ArrayList<>();
		for (Platform platform : platformManager.getPlatformList()) {
			if (platform != null) {
				ItemVo itemVo = new ItemVo();
				itemVo.setId(platform.getId());
				itemVo.setName(platform.getName());
				platformList.add(itemVo);
			}
		}
		model.addAttribute("platformList", platformList);
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/create.do", method = { RequestMethod.POST })
	public String create(@RequestParam("name") String name, @RequestParam("nickName") String nickName, @RequestParam("password") String password,
			@RequestParam("email") String email, @RequestParam("roleId") int roleId, @RequestParam("platformIdList") List<String> platformIdList) {
		String retCode = userManager.create(name, nickName, password, email, roleId, platformIdList);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/user/index.do";
		} else {
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/delete.do", method = { RequestMethod.GET })
	public void toDelete(Model model, @RequestParam("id") int id) {
		User user = userManager.getUser(id);
		model.addAttribute("user", user);
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/delete.do", method = { RequestMethod.POST })
	public String delete(@RequestParam("id") int id) {
		userManager.removeUser(id);
		userPlatformManager.removeUserPlatform(id);
		return "redirect:/mm/user/index.do";

	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/update.do", method = { RequestMethod.GET })
	public void toUpdate(Model model, @RequestParam("id") int id) {
		User user = userManager.getUser(id);
		model.addAttribute("user", user);
		Map<String, UserPlatform> store = userPlatformManager.getUserPlatformStore(id);
		List<PlatformVo> platformList = new ArrayList<>();
		for (Platform platform : platformManager.getPlatformList()) {
			if (platform != null) {
				PlatformVo vo = new PlatformVo();
				vo.setId(platform.getId());
				vo.setName(platform.getName());
				if (store.containsKey(platform.getId())) {
					vo.setSuccess(1);
				}
				platformList.add(vo);
			}
		}
		model.addAttribute("platformList", platformList);

		{
			Role role = roleManager.getRole(user.getRoleId());
			model.addAttribute("rolename", role.getName());
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/update.do", method = { RequestMethod.POST })
	public String update(@RequestParam("id") int id, @RequestParam("nickName") String nickName, @RequestParam("email") String email,@RequestParam("platformIdList") List<String> platformIdList) {
		String retCode = userManager.update(id, nickName, email,platformIdList);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/user/index.do";
		} else {	
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/updatePassword.do", method = { RequestMethod.GET })
	public void toUpdatePwd(Model model, @RequestParam("id") int id) {
		User user = userManager.getUser(id);
		user.setPassword("");
		model.addAttribute("user", user);
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/updatePassword.do", method = { RequestMethod.POST })
	public String updatePwd(@RequestParam("id") int id, @RequestParam("password") String password) {
		String retCode = userManager.updatePassword(id, password);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/user/index.do";
		} else {
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/updatePrivilege.do", method = { RequestMethod.POST })
	public String updatePrivilege(@RequestParam("id") int id, @RequestParam("privilegeList") List<Integer> privilegeList) {
		String retCode = userManager.updatePrivilege(id, privilegeList);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/user/index.do";
		} else {
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/getPrivilege.do", method = { RequestMethod.GET })
	public void getPrivilege(Model model, @RequestParam("id") int id) {
		User user = userManager.getUser(id);
		Collection<ModuleVO> list = privilegeManager.getModuleList();
		for (ModuleVO e : list) {
			List<MenuVO> menuList = e.getMenuDTOList();
			for (MenuVO menu : menuList) {
				menu.setAccess(user.isAuthorize(menu.getCode()));
			}
		}
		model.addAttribute("moduleList", list);
		model.addAttribute("userid", id);
		model.addAttribute("username", user.getName());
	}

	@RequestMapping(value = "/success.do", method = { RequestMethod.GET })
	public void success() {

	}

	@Privilege(value = PrivilegeConstant.MENU_USER)
	@RequestMapping(value = "/index.do", method = { RequestMethod.GET })
	public String index() {
		return "redirect:/mm/user/list.do";
	}
}
