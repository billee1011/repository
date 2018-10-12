package com.lingyu.admin.controller.pss;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.manager.AnnounceManager;
import com.lingyu.admin.manager.AnnounceTemplateManager;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.AnnounceVo;
import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.AnnoucenceTemplate;
import com.lingyu.common.entity.Announce;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.User;

/**
 * 客服系统公告管理
 * 
 * @author Wang Shuguang
 */
@Controller
@RequestMapping(value = "/pss/announce")
public class AnnounceController {
	private static final Logger logger = LogManager.getLogger(AnnounceController.class);

	private AnnounceManager announceManager;
	private GameAreaManager gameAreaManager;
	private AnnounceTemplateManager announceTemplateManager;

	public void initialize() {
		announceManager = AdminServerContext.getBean(AnnounceManager.class);
		gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
		announceTemplateManager = AdminServerContext.getBean(AnnounceTemplateManager.class);
	}

	/** 客服系统User管理主页面UI */
	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void toIndex(Model model) {
		List<AnnoucenceTemplate> templateList = announceTemplateManager.queryAll();
		model.addAttribute("announceTemplate", templateList);
	}

	@ResponseBody
	@RequestMapping(value = "/create.do", method = RequestMethod.POST)
	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	public int create(@RequestParam("content") String content, @RequestParam("interval") int interval,
			@RequestParam(value = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
			@RequestParam(value = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
			@RequestParam(value = "all", required = false) boolean all, @RequestParam(value = "areaList", required = false) List<Integer> areaList) {

		if (startTime == null) {
			startTime = TimeConstant.DATE_LONG_AGO;
		}
		if (endTime == null) {
			endTime = TimeConstant.DATE_LONG_AGO;
		}

		if (!all) {
			areaList = gameAreaManager.filterGameAreaIds(SessionUtil.getCurrentUser().getLastPid(), areaList);
		}

		Announce announce = announceManager.create(content, interval, startTime, endTime, all, areaList);
		if (announce == null||announce.getId()==0) {
			return ErrorCode.EC_FAILED;
		}

		return ErrorCode.EC_OK;
	}

	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/announcingList.do", method = RequestMethod.GET)
	public void list(Model model) {
		List<AnnounceVo> list = announceManager.getAnnouncingList();
		model.addAttribute("announcingList", list);
	}

	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/announceHistory.do", method = RequestMethod.GET)
	public void announceHistory(Model model) {
		List<AnnounceVo> list = announceManager.getAnnounceHistory();
		model.addAttribute("announceHistory", list);
	}

	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/deleteAnnounce.do", method = RequestMethod.GET)
	public void toDeleteAnnounce(Model model, @RequestParam("id") int id) {
		Announce announce = announceManager.getAnnounce(id);
		if (announce != null) {
			model.addAttribute("announce", announce);
		}
	}

	@ResponseBody
	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/deleteAnnounce.do", method = RequestMethod.POST)
	public int deleteAnnounce(@RequestParam("id") int id) {
		boolean ret = announceManager.deleteAnnounce(id);
		return ret ? id : -1;
	}

	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/deleteTemplate.do", method = RequestMethod.GET)
	public String deleteTemplate(@RequestParam("id") int id) {
		announceTemplateManager.delete(id);
		return "redirect:/pss/announce/index.do";
	}

	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/updateTemplate.do", method = RequestMethod.GET)
	public String updateTemplate(@RequestParam("id") int id, @RequestParam("title") String title, @RequestParam("content") String content) {
		announceTemplateManager.update(id, title, content);
		return "redirect:/pss/announce/index.do";
	}

	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/insertTemplate.do", method = RequestMethod.GET)
	public String insertTemplate(@RequestParam("title") String title, @RequestParam("content") String content) {
		announceTemplateManager.insert(title, content);
		return "redirect:/pss/announce/index.do";
	}
	
	/** 获取更新公告 */
	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/notice.do", method = RequestMethod.GET)
	public void toNoticeIndex(Model model) {

//		model.addAttribute("notice", announceTemplateManager.getNotice());
	}

	/** 更新公告 */
	@ResponseBody
	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/updateNotice.do", method = RequestMethod.POST)
	public void updateNotice(@RequestParam("type") int type, @RequestParam("content") String content, @RequestParam("version") String version) {
		announceManager.addNotice(type, content, version);
	}	

	/**
	 * 服务器维护
	 */
	@Privilege(value = PrivilegeConstant.MENU_ANNOUNCEMENT)
	@RequestMapping(value = "/serverNotify.do", method = RequestMethod.GET)
	public void serverNotify(Model model) {
		User user = SessionUtil.getCurrentUser();
		Collection<GameArea> gameAreas = gameAreaManager.getGameAreaList(user.getLastPid());
		model.addAttribute("gameAreas", gameAreas);
		model.addAttribute("areaId", user.getLastAreaId());
	}

}
