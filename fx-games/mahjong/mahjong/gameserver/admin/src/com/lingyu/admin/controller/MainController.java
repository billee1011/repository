package com.lingyu.admin.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.WebUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.core.Code;
import com.lingyu.admin.core.Constant;
import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.manager.ConcurrentManager;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.manager.PlatformManager;
import com.lingyu.admin.manager.UserManager;
import com.lingyu.admin.manager.UserPlatformManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.util.MD5Encrypt;
import com.lingyu.admin.util.PageUtils;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.GameAreaVo;
import com.lingyu.admin.vo.LoginCheckResultVo;
import com.lingyu.admin.vo.SimpleGameAreaVo;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.User;
import com.lingyu.common.verifycode.Captcha;
import com.lingyu.common.verifycode.GifCaptcha;

/**
 * 刚进管理平台登录页面控制器.
 * <p>
 */
@Controller
public class MainController {
	private static final Logger logger = LogManager.getLogger(MainController.class);
	
	private UserManager userManager;
	private GameAreaManager gameAreaManager;
	private PlatformManager platformManager;
	private UserPlatformManager userPlatformManager;
	private ConcurrentManager concurrentManager;
	private Captcha captcha = new GifCaptcha(150, 55, 5);// gif格式动画验证码
	
	private GameAreaComparator gameAreaComparator = new GameAreaComparator();
	
	public void initialize() {
		userManager = AdminServerContext.getBean(UserManager.class);
		gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
		platformManager = AdminServerContext.getBean(PlatformManager.class);
		userPlatformManager = AdminServerContext.getBean(UserPlatformManager.class);
		concurrentManager = AdminServerContext.getBean(ConcurrentManager.class);
	}
	
	
	/**
	 * 登录页面UI.
	 */
	@RequestMapping("/index.do")
	public void indexUI() {
		System.out.println("indexUI");
	}
	
	/** 总览主页面UI */
	@RequestMapping(value = "/summary.do", method = RequestMethod.GET)
	public void summary(Model model) {
		model.addAttribute("serverTime", System.currentTimeMillis());
		logger.debug("总览页面UI");
	}
	@RequestMapping("/authorize.do")
	public void authorize() {
		System.out.println("authorize");
	}
	
	/**
	 * AJAX检查输入的账号今天错误多少次了，要不要验证码?还能不能再登录啦.
	 */
	@ResponseBody
	@RequestMapping(value = "/check.do", method = RequestMethod.POST)
	public LoginCheckResultVo check(HttpServletRequest request, @RequestParam("username") String username) {
		LoginCheckResultVo vo = new LoginCheckResultVo();
		User user = userManager.getUserByName(username);
		if (user == null) {
			vo.setErrcode(ErrorCode.LOGIN_USERNAME_ERROR);
		} else {
			vo.setLoginFailed(user.getLoginFailed());
			// 连错十次就不让他登录了，提示1小时后再来登录...
			if (vo.getLoginFailed() >= Constant.MAX_LOGIN_FAILED_NO_LOGIN) {
				vo.setErrcode(ErrorCode.LOGIN_PASSWORD_ERROR_10);
			}
			// 连错3次就让他输入验证码...
			else if (vo.getLoginFailed() >= Constant.MAX_LOGIN_FAILED_VERIFY_CODE) {
				vo.setErrcode(ErrorCode.LOGIN_PASSWORD_ERROR_3);
			}
			// 正常登录，把验证码发给他，自动写上去登录
			else {
				Object verifycode = WebUtils.getSessionAttribute(request, Constant.SESSION_KEY_VERIFY_CODE);
				vo.setVerifycode(verifycode == null ? null : verifycode.toString());
			}
		}
		return vo;
	}
	
	/**
	 * 验证码.
	 */
	@RequestMapping("/verifycode.do")
	public void verifycode(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try (OutputStream out = response.getOutputStream()) {
			WebUtils.setSessionAttribute(request, Constant.SESSION_KEY_VERIFY_CODE, captcha.out(out).toLowerCase());
		}
	}
	
	@ResponseBody
	@RequestMapping(value = "/login.do", method = { RequestMethod.POST })
	public Code login(HttpServletRequest request, @RequestParam("userName") String name, @RequestParam("password") String password,
			@RequestParam("verifycode") String verifycode) {
		logger.info("管理员登陆 name={}", name);
		// Object vcode = WebUtils.getSessionAttribute(request,
		// Constant.SESSION_KEY_VERIFY_CODE);
		String retCode = ErrorCode.EC_OK;
		// if (StringUtils.isNotBlank(verifycode) &&
		// verifycode.toLowerCase().equals(vcode)) {
		User user = userManager.getUserByName(name);
		if (user == null) {
			retCode = ErrorCode.LOGIN_USERNAME_ERROR;
		} else {
			if (!user.getPassword().equals(MD5Encrypt.encrypt(password))) {
				user.setLoginFailed(user.getLoginFailed() + 1);
				userManager.save(user);
				if (user.getLoginFailed() >= Constant.MAX_LOGIN_FAILED_VERIFY_CODE) {
					retCode = ErrorCode.LOGIN_PASSWORD_ERROR_3;
				} else {
					retCode = ErrorCode.LOGIN_PASSWORD_ERROR;
				}
			} else {
				String ip = request.getHeader("x-forwarded-for");
				if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getHeader("Proxy-Client-IP");
				}
				if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getHeader("WL-Proxy-Client-IP");
				}
				if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getRemoteAddr();
				}
				ip = ip.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ip;
				user.setLastLoginIp(ip);
				user.setLoginFailed(0);
				userManager.save(user);
				userManager.login(user);
				int areaId = user.getLastAreaId();
				retCode = gameAreaManager.selectGameArea(user.getLastPid(), areaId);
			}
		}
		// } else {
		// retCode = ErrorCode.LOGIN_VERIFYCODE_ERROR;
		// }
		return new Code(retCode);
	}
	
	/**
	 * 添加游戏服务器
	 * @param resp
	 * @param world_id
	 * @param area_id
	 * @param world_name
	 * @param area_name
	 * @param area_type
	 * @param external_ip
	 * @param tcp_port
	 * @param ip
	 * @param prot
	 * @param pid
	 * @param status
	 * @param follower_id
	 * @throws Exception
	 */
	@RequestMapping(value = "/addAreaSys.do", method = { RequestMethod.GET })
	public void addAreaSys(HttpServletResponse resp, @RequestParam("world_id") int world_id,@RequestParam("area_id") int area_id
			, @RequestParam("world_name") String world_name, @RequestParam("area_name") String area_name, @RequestParam("area_type") int area_type, @RequestParam("external_ip") String external_ip,
			@RequestParam("tcp_port") int tcp_port, @RequestParam("ip") String ip,@RequestParam("port") int port,
			@RequestParam("pid") String pid,@RequestParam("status") int status,@RequestParam("follower_id") int follower_id) throws Exception {
		// URLEncoder 乱码的问题参考
		// http://blog.csdn.net/snow_crazy/article/details/38016411
		GameArea ret = new GameArea();
		ret.setWorldId(world_id);
		ret.setWorldName(world_name);
		ret.setAreaId(area_id);
		ret.setAreaName(area_name);
		ret.setIp(ip);
		ret.setPort(port);
		ret.setExternalIp(external_ip);
		ret.setTcpPort(tcp_port);
		ret.setStatus(status);
		ret.setPid(pid);
		ret.setType(area_type);
		ret.setFollowerId(follower_id);
		ret.setAddTime(new Date());
		
		GameArea area = gameAreaManager.getGameAreaByAreaId(ret.getPid(), ret.getAreaId());
		if(area == null){
			gameAreaManager.addGameArea(ret);
		}else{
			gameAreaManager.updateGameArea(ret);
		}
		
		JSONArray areaArrayJson = new JSONArray();
		JSONObject areaJson = new JSONObject();
		areaJson.put("errorCode", "1");
		areaArrayJson.add(areaJson);
		ServletOutputStream out = resp.getOutputStream();
		out.write(areaJson.toString().getBytes("UTF-8"));
		out.flush();
		out.close();
		logger.info("添加游戏区成功,worldId={},areaId={}", world_id, area_id);
	}
	
	/**
	 * 在线人数统计
	 * @param world_id
	 * @param area_id
	 * @param pid
	 * @param add_time
	 * @param ccu  在线人数
	 * @throws Exception
	 */
	@RequestMapping(value = "/statrealtime.do", method = { RequestMethod.GET })
	public void addAreaSys(@RequestParam("world_id") int world_id,@RequestParam("area_id") int area_id
			, @RequestParam("pid") String pid, @RequestParam("add_time") Date add_time, @RequestParam("ccu") int ccu) throws Exception {
		concurrentManager.createStatOnlineNum(pid, world_id, area_id, ccu, add_time);
	}
	
	@Privilege
	@RequestMapping(value = "/logout.do", method = RequestMethod.GET)
	public String logout() {
		SessionUtil.removeCurrentSession();
		return "redirect:/index.do";
	}
	
	@Privilege
	// @ResponseBody
	@RequestMapping(value = "/changeplatform.do", method = { RequestMethod.POST })
	public String changePlatform(@RequestParam("id") String id) {
		User user = SessionUtil.getCurrentUser();
		String retCode = platformManager.selectPlatform(id);
		if (ErrorCode.EC_OK.equals(retCode)) {
			// 平台下面有服务器才让选择平台
			userManager.updatePlatformId(user, id);
			GameArea gameArea = gameAreaManager.getFirstGameArea(id);
			userManager.updateAreaId(user, gameArea.getAreaId());
			SessionUtil.setSessionValue(SessionUtil.AREA_ID_KEY, gameArea.getAreaId());
			return "redirect:/center.do";
		} else {
			return retCode;

		}
	}
	
	@Privilege
	@RequestMapping(value = "/center.do", method = { RequestMethod.GET })
	public void index(Model model) {
		// TODO 综合信息
		// TODO 数据趋势相关
		// TODO 兑换情况
		// 选区列表
		User user = SessionUtil.getCurrentUser();
		int areaId = user.getLastAreaId();
		GameArea gameArea = gameAreaManager.getGameAreaByAreaId(user.getLastPid(), areaId);
		model.addAttribute("area", gameArea);
		Platform platform = platformManager.getPlatform(user.getLastPid());
		model.addAttribute("platform", platform);
		List<String> idList = userPlatformManager.getPlatformIdListByUserId(user.getId());
		List<Platform> platformList = platformManager.getPlatformListByIds(idList);
		model.addAttribute("platformList", platformList);

		// int userId = SessionUtil.getCurrentUser().getId();
		//
		// List<GameArea> areaList = new ArrayList<>();
		// List<UserArea> userAreaList =
		// userAreaManager.getUserAreaListByUserId(userId);
		// for (UserArea e : userAreaList) {
		// GameArea gameArea = gameAreaManager.getGameArea(e.getAreaId());
		// areaList.add(gameArea);
		//
		// }
		// model.addAttribute("areaList", areaList);

		Date now = new Date();
		model.addAttribute("serverTime", now);
		SessionUtil.setSessionValue("timezonerawoffset", TimeZone.getDefault().getOffset(now.getTime()));
		SessionUtil.setSessionValue("serverTime", System.currentTimeMillis());
		model.addAttribute("timezone", TimeZone.getDefault().getDisplayName());
		int num = 111;
		model.addAttribute("onlineNum", num);
		String version = AdminServerContext.getVersion();
		model.addAttribute("version", version);

		// int areaId = SessionUtil.getCurrentAreaId();
		// model.addAttribute("areaId", areaId);
	}
	
	@Privilege
	@RequestMapping(value = "/multiarea.do", method = { RequestMethod.GET })
	public void mutiArea(Model model, @RequestParam("curpage") int curPage, @RequestParam("countpp") int countPerPage,
			@RequestParam(value = "normalOrder", required = false) String normalOrder,
			@RequestParam(value = "allavailablepf", required = false) String allavailablepf) {
		User user = SessionUtil.getCurrentUser();
		String platformId = user.getLastPid();

		GameArea lastArea = gameAreaManager.getGameAreaByAreaId(platformId, user.getLastAreaId());
		List<GameArea> gameAreas = null;
		Set<String> set = new HashSet<>();
		if ("1".equals(allavailablepf)) {
			List<String> platformIdList = userPlatformManager.getPlatformIdListByUserId(user.getId());
			set.addAll(platformIdList);
		}
		set.add(platformId);
		for (String pid : set) {
			Collection<GameArea> gameAreaCollection = gameAreaManager.getGameAreaList(pid);
			if (gameAreas == null) {
				gameAreas = new ArrayList<>(gameAreaCollection);
			} else {
				gameAreas.addAll(gameAreaCollection);
			}
		}
		Collections.sort(gameAreas, gameAreaComparator);
		List<GameArea> list = new ArrayList<>();
		for (GameArea e : gameAreas) {
			if (e.getFollowerId() == 0) {
				list.add(e);
				for (GameArea child : e.getChildAreas()) {
					list.add(child);
				}
			}
		}
		// 修正countPerPage
		if (countPerPage <= 0) {
			countPerPage = 100;
		}

		// 计算
		int totalPage = 1;
		int size = list.size();
		totalPage = size / countPerPage;
		if (size % countPerPage != 0) {
			totalPage++;
		}

		// 修正发的参数
		if (curPage < 1) {
			curPage = 1;
		} else if (curPage > totalPage) {
			curPage = totalPage;
		}

		int start = countPerPage * (curPage - 1);
		int end = Math.min(countPerPage * curPage, list.size());

		// 移动start
		for (int i = start; i < list.size(); i++) {
			GameArea area = list.get(i);
			if (area.getFollowerId() == 0) {
				break;
			}
			start++;
		}

		for (int i = end; i < list.size(); i++) {
			GameArea area = list.get(i);
			if (area.getFollowerId() == 0) {
				break;
			}
			end++;
		}

		List<GameArea> pagedGameAreas = list.subList(start, Math.min(end, list.size()));
		if (!"1".equals(normalOrder)) { // 不是正常次序
			if (lastArea != null && CollectionUtils.isNotEmpty(pagedGameAreas)) {
				Iterator<GameArea> it = pagedGameAreas.iterator();
				while (it.hasNext()) {
					GameArea ga = it.next();
					if (ga.getWorldId() == lastArea.getWorldId()) {
						it.remove();
						break;
					}
				}
				pagedGameAreas.add(0, lastArea);
			}
		}

		List<GameAreaVo> retAreaVoList = gameAreaManager.transferToGameAreaVo(pagedGameAreas);

		model.addAttribute("lastarea", lastArea);
		model.addAttribute("gameAreas", retAreaVoList);
		model.addAttribute("curPage", curPage);
		model.addAttribute("totalPage", totalPage);
		List<Integer> displayPages = PageUtils.calPages(curPage, totalPage, 10);
		model.addAttribute("displayPages", displayPages);
	}
	
	@Privilege
	@RequestMapping(value = "/changearea.do", method = { RequestMethod.GET })
	public void toChangeArea(Model model, @RequestParam("curpage") int curPage, @RequestParam("countpp") int countPerPage) {
		User user = SessionUtil.getCurrentUser();
		String platformId = user.getLastPid();
		GameArea lastArea = gameAreaManager.getGameAreaByAreaId(platformId, user.getLastAreaId());
		Collection<GameArea> gameAreaCollection = gameAreaManager.getGameAreaList(platformId);
		List<GameArea> gameAreas = new ArrayList<>(gameAreaCollection);
		Collections.sort(gameAreas, gameAreaComparator);
		List<GameArea> list = new ArrayList<>();
		for (GameArea e : gameAreas) {
			list.add(e);
		}
		// List<GameArea> list = new ArrayList<GameArea>(1024);
		// for(int i = 0; i < 1024; i ++){
		// list.addAll(gameAreas);
		// }
		// gameAreas = list;

		// 修正countPerPage
		if (countPerPage <= 0) {
			countPerPage = 100;
		}

		// 计算
		int totalPage = 1;
		int size = list.size();
		totalPage = size / countPerPage;
		if (size % countPerPage != 0) {
			totalPage++;
		}

		// 修正发的参数
		if (curPage < 1) {
			curPage = 1;
		} else if (curPage > totalPage) {
			curPage = totalPage;
		}

		List<GameArea> pagedGameAreas = list.subList(countPerPage * (curPage - 1), Math.min(countPerPage * curPage, list.size()));

		model.addAttribute("lastarea", lastArea);
		model.addAttribute("gameAreas", pagedGameAreas);
		model.addAttribute("curPage", curPage);
		model.addAttribute("totalPage", totalPage);
		List<Integer> displayPages = PageUtils.calPages(curPage, totalPage, 10);
		model.addAttribute("displayPages", displayPages);
	}
	
	@Privilege
	@ResponseBody
	@RequestMapping(value = "/changearea.do", method = { RequestMethod.POST })
	public SimpleGameAreaVo changeArea(@RequestParam("id") int areaId) {
		User user = SessionUtil.getCurrentUser();
		int lastAreaId = user.getLastAreaId();
		GameArea gameArea = gameAreaManager.getGameAreaByAreaId(user.getLastPid(), lastAreaId);
		if (lastAreaId != areaId) {
			String retCode = gameAreaManager.selectGameArea(user.getLastPid(), areaId);
			if (ErrorCode.EC_OK.equals(retCode)) {
				userManager.updateAreaId(user, areaId);
			} else {
				SimpleGameAreaVo ret = new SimpleGameAreaVo();
				ret.setSuccess(-2);
				return ret;
			}
			gameArea = gameAreaManager.getGameAreaByAreaId(user.getLastPid(), areaId);
		}
		SimpleGameAreaVo ret = null;
		if (gameArea != null) {
			ret = new SimpleGameAreaVo(gameArea.getAreaId(), gameArea.getAreaName());
		} else {
			ret = new SimpleGameAreaVo();
			ret.setSuccess(-1);
		}
		return ret;
	}
	
	class GameAreaComparator implements Comparator<GameArea> {

		@Override
		public int compare(GameArea o1, GameArea o2) {
			return o1.getAreaId() - o2.getAreaId();
		}

	}
	
	
}
