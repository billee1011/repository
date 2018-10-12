package com.lingyu.admin.controller.mm;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.manager.PrivilegeManager;
import com.lingyu.admin.manager.RoleManager;
import com.lingyu.admin.manager.UserManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.vo.MenuVO;
import com.lingyu.admin.vo.ModuleVO;
import com.lingyu.admin.vo.RoleVO;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.User;

@Controller
@RequestMapping(value = "/mm/role")
public class RoleController {
	private static final Logger logger = LogManager.getLogger(RoleController.class);
	private RoleManager roleManager;
	private UserManager userManager;
	private PrivilegeManager privilegeManager;

	public void initialize() {
		roleManager = AdminServerContext.getBean(RoleManager.class);
		userManager = AdminServerContext.getBean(UserManager.class);
		privilegeManager = AdminServerContext.getBean(PrivilegeManager.class);

	}

	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/list.do", method = { RequestMethod.GET })
	public List<RoleVO> getList() {
		List<RoleVO> list = roleManager.getRoleVOList();
		return list;
	}

	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/getPrivilege.do", method = { RequestMethod.GET })
	public void getPrivilege(Model model, @RequestParam("roleId") int roleId) {
		Role role = roleManager.getRole(roleId);
		Collection<ModuleVO> list = privilegeManager.getModuleList();
		for (ModuleVO e : list) {
			List<MenuVO> menuList = e.getMenuDTOList();
			for (MenuVO menu : menuList) {
				menu.setAccess(role.isAuthorize(menu.getCode()));
			}
		}
		model.addAttribute("moduleList", list);
		model.addAttribute("roleVO", role.toVO());
	}

	/** 获取创建游戏区UI */
	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/create.do", method = { RequestMethod.GET })
	public void toCreate(Model model) {
		Role role = new Role();
		model.addAttribute("role", role);
	}

	@RequestMapping(value = "/create.do", method = { RequestMethod.POST })
	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	public String create(@RequestParam("name") String name, @RequestParam("description") String description,
			@RequestParam(value="privilegeList", required=false) List<Integer> privilegeList) {
		String retCode = roleManager.create(name, description, privilegeList);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/role/index.do";
		} else {
			return retCode;
		}
	}

	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/delete.do", method = { RequestMethod.GET })
	public void toDelete(Model model, @RequestParam("id") int id) {
		Role role = roleManager.getRole(id);
		model.addAttribute("role", role);
	}

	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/delete.do", method = { RequestMethod.POST })
	public String delete(@RequestParam("id") int id) {
		roleManager.removeRole(id);
		return "redirect:/mm/role/index.do";
	}

	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/update.do", method = { RequestMethod.GET })
	public void toUpdate(Model model, @RequestParam("id") int id) {
		Role role = roleManager.getRole(id);
		model.addAttribute("role", role.toVO());
		model.addAttribute("privilegeList", role.getPrivilegeList());

	}

	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/update.do", method = { RequestMethod.POST })
	public String update(@RequestParam("id") int id, @RequestParam("name") String name, @RequestParam("description") String description,
			@RequestParam(value = "privilegeList", required=false) List<Integer> list) {
		String retCode = roleManager.update(id, name, description, list);
		if (retCode == ErrorCode.EC_OK) {
			return "redirect:/mm/role/index.do";
		} else {
			return retCode;
		}
	}

	/** 目前只赋予一个用户一个身份权限，以后又需要再扩展 */
	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/select.do", method = { RequestMethod.POST })
	public String select(Model model, @RequestParam("userId") int userId, @RequestParam("privilegeList") List<Integer> privilegeList) {
		logger.info("给用户更新角色权限 userId={}", userId);
		String ret = ErrorCode.EC_OK;
		User user = userManager.getUser(userId);
		if (user == null) {
			ret = ErrorCode.NO_USER;
			return ret;
		}
		user.setPrivilegeList(privilegeList);
		return "redirect:/mm/role/index.do";
	}

	@RequestMapping(value = "/success.do", method = { RequestMethod.GET })
	public void success() {

	}

	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/index.do", method = { RequestMethod.GET })
	public String index() {
		return "redirect:/mm/role/list.do";
	}

}
