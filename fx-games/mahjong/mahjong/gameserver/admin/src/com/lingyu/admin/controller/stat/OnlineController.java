package com.lingyu.admin.controller.stat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.manager.ConcurrentManager;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.PlayerNumVO;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.StatOnlineNum;
import com.lingyu.common.entity.User;

/** 在线人数 */
@Controller
@RequestMapping(value = "/stat/online")
public class OnlineController {
	private ConcurrentManager concurrentManager;
	private GameAreaManager gameAreaManager;

	public void initialize() {
		concurrentManager = AdminServerContext.getBean(ConcurrentManager.class);
		gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
	}

	/** 在线统计UI */
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void indexUI(Model model) {
		User user = SessionUtil.getCurrentUser();
		Collection<GameArea> gameAreas = gameAreaManager.getGameAreaList(user.getLastPid());
		model.addAttribute("gameAreas", gameAreas);
		model.addAttribute("areaId", user.getLastAreaId());
	}
	
	/** 获取某个区的当前同时在线 */
	@Privilege(value = PrivilegeConstant.MENU_ONLINE_NUM)
	@RequestMapping(value = "/onlineNum.do", method = { RequestMethod.GET })
	public int getOnlineNum(@RequestParam("areaId") int areaId) {
		int ret = concurrentManager.getOnlineNum(areaId);
		return ret;
	}

	/** 获取某两天的在线数据 */
	@ResponseBody
	@Privilege(value = PrivilegeConstant.MENU_ONLINE_NUM)
	@RequestMapping(value = "/concurrentUser.do", method = { RequestMethod.GET })
	public void getConcurrentUserNum(Model model, @RequestParam("areaId") int areaId, @RequestParam("baseTime") Date baseTime,
			@RequestParam("compareTime") Date compareTime) {
		List<StatOnlineNum> baseList = concurrentManager.getConcurrentNumList(areaId, SessionUtil.getCurrentUser().getLastPid(), baseTime);
		List<PlayerNumVO> base = new ArrayList<>();
		for (StatOnlineNum e : baseList) {
			PlayerNumVO vo = e.toVO();
			base.add(vo);
		}
		model.addAttribute("baseList", base);
		List<StatOnlineNum> compareList = concurrentManager.getConcurrentNumList(areaId, SessionUtil.getCurrentUser().getLastPid(), compareTime);
		List<PlayerNumVO> compare = new ArrayList<>();
		for (StatOnlineNum e : compareList) {
			PlayerNumVO vo = e.toVO();
			compare.add(vo);
		}
		model.addAttribute("compareList", compare);
	}

	/** 获取某时段的在线数据 */
	@ResponseBody
	@Privilege(value = PrivilegeConstant.MENU_ONLINE_NUM)
	@RequestMapping(value = "/online4area.do", method = { RequestMethod.GET })
	public Object getPopulationInfo4Area(@RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
			@RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime, @RequestParam("areaId") int areaId) {
		User user = SessionUtil.getCurrentUser();
		List<StatOnlineNum> list = concurrentManager.getConcurrentNumList4Area(areaId, SessionUtil.getCurrentUser().getLastPid(), startTime, endTime);
		List<PlayerNumVO> ret = new ArrayList<>();
		for (StatOnlineNum e : list) {
			PlayerNumVO vo = e.toVO();
			ret.add(vo);
		}
		return ret;
	}

	/** 实时获取在线人数 */
	@ResponseBody
	@Privilege(value = PrivilegeConstant.MENU_ONLINE_NUM)
	@RequestMapping(value = "/timerOnlineNum.do", method = { RequestMethod.GET })
	public Object getRealTimeOnlineNum(@RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
			@RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
		Date today = new Date(System.currentTimeMillis());
		// 只有和服务器时间相同 才同步数据到客户端
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		List<StatOnlineNum> dataList = new ArrayList<>();
		if (startTime != null && sdf.format(startTime).equals(sdf.format(today))) {
			User user = SessionUtil.getCurrentUser();
			dataList = concurrentManager.getConcurrentNumListTimerArea(user.getLastAreaId(), SessionUtil.getCurrentUser().getLastPid(), startTime, endTime);
		}
		List<PlayerNumVO> ret = new ArrayList<>();
		for (StatOnlineNum e : dataList) {
			PlayerNumVO vo = e.toVO();
			ret.add(vo);
		}
		return ret;
	}
}