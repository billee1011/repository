package com.lingyu.admin.controller.ps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.manager.PlatformManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.DisplayGameAreaEntryVo;
import com.lingyu.admin.vo.DisplayGameAreaListVo;
import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.User;
import com.lingyu.common.util.TimeUtil;

@Controller
@RequestMapping(value = "/ps/area")
public class AreaController {
	private static final Logger logger = LogManager.getLogger(AreaController.class);

	private GameAreaManager gameAreaManager;
	private PlatformManager platformManager;
	
	private static final String SEARCH_TYPE_ID = "0";
	private static final String SEARCH_TYPE_IP = "1";
	private static final String SEARCH_TYPE_AREAID = "2";

	public void initialize() {
		gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
		platformManager = AdminServerContext.getBean(PlatformManager.class);
	}

	@Privilege(value = PrivilegeConstant.MENU_GAME_AREA_LIST)
	@RequestMapping(value = "/gamelist.do", method = { RequestMethod.GET })
	public void getList(Model model) {
		String pid = SessionUtil.getCurrentUser().getLastPid();
		model.addAttribute("platformId", pid);
		Collection<GameArea> areas = gameAreaManager.getAllGameAreaList();
		model.addAttribute("areaListSize", areas.size());
	}
	
	@ResponseBody
	@Privilege(value = PrivilegeConstant.MENU_GAME_AREA_LIST)
	@RequestMapping(value = "/arealist.do", method = { RequestMethod.POST })
	public DisplayGameAreaListVo<DisplayGameAreaEntryVo> getJsonList(@RequestParam("page") int page, @RequestParam("rows") int rows,
			@RequestParam(value = "searchvalue", required = false) String searchValue, @RequestParam(value = "searchtype", required = false) String searchType) {
		try {
			String pid = SessionUtil.getCurrentUser().getLastPid();
			Collection<GameArea> areas = gameAreaManager.getAllGameAreaList();
			List<GameArea> ret = new ArrayList<GameArea>(areas.size());
			String[] searchValues = null;
			if (StringUtils.isNotEmpty(searchType) && StringUtils.isNotEmpty(searchValue)) {
				searchValues = StringUtils.split(searchValue, ",");
			}
			for (GameArea area : areas) {
				if (searchValues == null) {
					ret.add(area);
				} else {
					for (String s : searchValues) {
						if (SEARCH_TYPE_IP.equals(searchType) && area.getIp().indexOf(s) >= 0) {
							ret.add(area);
							break;
						} else if (SEARCH_TYPE_ID.equals(searchType) && s.matches("\\d+") && area.getWorldId() == Integer.parseInt(s)) {
							ret.add(area);
							break;
						} else if (SEARCH_TYPE_AREAID.equals(searchType) && area.getAreaId() == Integer.parseInt(s)) {
							ret.add(area);
							break;
						}
					}
				}
			}
			Collections.sort(ret, new Comparator<GameArea>() {

				@Override
				public int compare(GameArea area0, GameArea area1) {
					return area0.getWorldId() - area1.getWorldId();
				}
			});
			int start = (page - 1) * rows;
			int end = page * rows;
			if (end > ret.size()) {
				end = ret.size();
			}
			
			List<GameArea> list = ret.subList(start, end);
			DisplayGameAreaListVo<DisplayGameAreaEntryVo> result = new DisplayGameAreaListVo<DisplayGameAreaEntryVo>();
			result.setTotal(ret.size());
			List<DisplayGameAreaEntryVo> entries = new ArrayList<>();
			for (GameArea area : list) {
				DisplayGameAreaEntryVo vo = getDisplayGameAreaEntryVo(area);
				if(vo == null)continue;
				entries.add(vo);
			}
			result.setRows(entries);
			return result;
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			throw e;
		}
	}
	
	public DisplayGameAreaEntryVo getDisplayGameAreaEntryVo(GameArea area) {
		DisplayGameAreaEntryVo entry = new DisplayGameAreaEntryVo();
		Platform platform=platformManager.getPlatform(area.getPid());
		if(platform == null){
			logger.info("no platformId,pid={}",area.getPid());
			return null;
		}
		entry.setPlatformName(platform.getName());
		entry.setWorldId(area.getWorldId());
		entry.setWorldName(area.getWorldName());
		entry.setAreaId(area.getAreaId());
		entry.setAreaName(area.getAreaName());
		entry.setFollowerId(area.getFollowerId() != 0 ? area.getFollowerId() + "" : "");
		entry.setAdminAddress(area.getIp() + ":" + area.getPort());
		entry.setGameAddress(area.getExternalIp() + ":" + area.getTcpPort());
		entry.setOpenTime(DateFormatUtils.format(area.getAddTime(), TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss));
		Date combineTime = area.getCombineTime();
		if(combineTime == null){
			combineTime = TimeConstant.DATE_LONG_AGO;
			logger.info("areaCombineTimeNull: worldId={}, worldName={}", area.getWorldId(), area.getWorldName());
		}
		entry.setCombineTime(DateFormatUtils.format(combineTime, TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss));
		entry.setRestartTime(DateFormatUtils.format(area.getRestartTime(), TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss));
		entry.setStatus(area.getStatus());
		entry.setServerVersion("");
		entry.setDataVersion("");
		return entry;
	}
	
	@Privilege(value = PrivilegeConstant.MENU_GAME_AREA_LIST)
	@RequestMapping(value = "/index.do", method = { RequestMethod.GET })
	public String index() {
		User user = SessionUtil.getCurrentUser();
		return "redirect:/mm/area/gamelist.do";
	}
}
