package com.lingyu.admin.controller.ps;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.core.Constant;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.manager.ProgramSupportManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.User;
import com.lingyu.msg.http.GetServerInfo_S2C_Msg;

/**
 * 运维管理系统Controller
 * 
 */
@Controller
@RequestMapping(value = "/ps")
public class PSController {
	private static final Logger logger = LogManager.getLogger(PSController.class);

	private ProgramSupportManager programSupportManager;
	private GameAreaManager gameAreaManager;

	public void initialize() {
		programSupportManager = AdminServerContext.getBean(ProgramSupportManager.class);
		gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
	}

	/** 运维管理系统主页面UI */
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void indexUI() {
		logger.debug("运维管理系统主页面UI");
	}
	
	/** 获取服务器信息 */
	@Privilege(value = PrivilegeConstant.MENU_SERVER_MANAGER)
	@RequestMapping(value = "/serverManager.do", method = RequestMethod.GET)
	public void serverInfo(Model model, @RequestParam(value = "info", required = false) String info) {
		User user = SessionUtil.getCurrentUser();
		GetServerInfo_S2C_Msg msg = programSupportManager.GetServerInfo(user.getLastPid(), user.getLastAreaId());
		if (msg != null) {
			model.addAttribute("serverInfo", JSON.toJSONString(msg, SerializerFeature.UseSingleQuotes, SerializerFeature.WriteDateUseDateFormat));
		}
		if (!StringUtils.isEmpty(info)) {
			if (info.equals("no area")) {
				model.addAttribute("info", "没有选择游戏区");
			} else {
				model.addAttribute("info", "成功进行操作");
			}
		}

		model.addAttribute("openMillis", 0);
		model.addAttribute("maintainUrl", "");
	}
	
	/** 踢玩家下线 */
	@Privilege(value = PrivilegeConstant.MENU_SERVER_MANAGER)
	@RequestMapping(value = "/kickoff.do", method = RequestMethod.POST)
	public String kickOff(@RequestParam(value = "all", required = false) boolean all,
			@RequestParam(value = "areaList", required = false) List<Integer> areaList, @RequestParam("reason") String reason, Model model) {
		if (!(!CollectionUtils.isEmpty(areaList) || all)) {
			model.addAttribute("info", "no area");
		}
		String pid = SessionUtil.getCurrentUser().getLastPid();
		List<GameArea> list = gameAreaManager.getHandleGameAreaList(pid, all, areaList);
		// 在线清除
		/*if (all) {
			concurrentManager.clearAll(pid);
		} else {
			for (GameArea e : list) {
				concurrentManager.clear(pid, e.getWorldId());
			}
		}*/
		programSupportManager.kickOff(list, reason);
		model.addAttribute("info", "踢玩家下线成功");
		logger.info("ps operate/kickoff : running by {}", SessionUtil.getCurrentUser().getName());
		return "redirect:serverManager.do";
	}
	
	/** 维护 */
	@Privilege(value = PrivilegeConstant.MENU_SERVER_MANAGER)
	@RequestMapping(value = "/maintain.do", method = { RequestMethod.POST })
	public String maintain(@RequestParam("status") int status, @RequestParam("reason") String reason,
			@RequestParam(value = "foreseeOpenTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date foreseeOpenTime,
			@RequestParam(value = "maintainUrl", required = false) String maintainUrl, @RequestParam(value = "all", required = false) boolean all,
			@RequestParam(value = "areaList", required = false) List<Integer> areaList, Model model) {
		if (!(!areaList.isEmpty() || all)) {
			model.addAttribute("info", "no area");
		} else {
			List<GameArea> list = gameAreaManager.getHandleGameAreaList(SessionUtil.getCurrentUser().getLastPid(), all, areaList);
			String retcode = programSupportManager.maintain(status, reason, list, foreseeOpenTime, maintainUrl);
			model.addAttribute("maintainCode", retcode);
			model.addAttribute("info", "成功进行操作");
			logger.info("ps operate/maintain : running by {}", SessionUtil.getCurrentUser().getName());
		}
		return "redirect:serverManager.do";
	}
}
