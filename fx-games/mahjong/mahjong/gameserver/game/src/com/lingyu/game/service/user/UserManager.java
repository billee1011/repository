package com.lingyu.game.service.user;

import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.ServerInfo;
import com.lingyu.common.entity.User;
import com.lingyu.common.http.HttpManager;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.io.Session;
import com.lingyu.common.io.SessionManager;
import com.lingyu.common.util.TimeUtil;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.event.LoginGameEvent;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.id.IdMaxConstant;
import com.lingyu.game.service.id.TableNameConstant;
import com.lingyu.game.service.mahjong.MahjongConstant;
import com.lingyu.game.service.role.RoleManager;

@Service
public class UserManager {
	private static final Logger logger = LogManager.getLogger(UserManager.class);

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private IdManager idManager;
	@Autowired
	private RouteManager routeManager;

	public void init() {
		userRepository.loadAll();
	}

	public User getUser(String pid, String userId) {
		return userRepository.getUser(pid, userId);
	}

	/**
	 * 验证角色信息
	 *
	 * @param pid
	 * @param userId
	 * @return 不为错误码的时候，返回服务器信息
	 */
	public Object[] validateUser(String pid, String userId) {
		User user = getUser(pid, userId);
		if (user == null) {
			// return ErrorCode.USER_NOT_EXIST;
		}

		List<Role> roleList = roleManager.getRoleListByUserId(pid, userId);
		int i = 0;
		Object[] result = new Object[roleList.size()];
		for (Role e : roleList) {
			result[i++] = roleManager.toRoleVo(e);
		}

		long nowTime = System.currentTimeMillis();
		return new Object[] { ErrorCode.EC_OK, nowTime, TimeZone.getDefault().getOffset(nowTime), result };
	}

	/**
	 * 角色登陆 没有角色就直接创建
	 *
	 * @param loginType
	 * @param pid
	 * @param userId
	 * @param machingId
	 * @param code
	 * @return
	 */
	public JSONObject loginGame(int loginType, String pid, String userId, String machingId, String code) {
		logger.info("login game, type={}, pid={},userId={},machingId={},code={}", loginType, pid, userId, machingId,
		        code);
		if (loginType == SystemConstant.LOGIN_TYPE_WEIXIN) {
			return loginWeiXin(pid, userId, machingId, code);
		} else if (loginType == SystemConstant.LOGIN_TYPE_YOUKE) {
			return loginYouKe(pid, userId, machingId, code);
		} else {
			JSONObject result = new JSONObject();
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.LOGIN_TYPE_ERROR);
			return result;
		}
	}

	/**
	 * 微信登陆
	 *
	 * @param pid
	 * @param userId
	 * @param machingId
	 * @param code
	 * @return
	 */
	private JSONObject loginWeiXin(String pid, String userId, String machingId, String code) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Session session = null;
		if (StringUtils.isEmpty(userId)) {
			session = SessionManager.getInstance().getSession4User(machingId);
		} else {
			session = SessionManager.getInstance().getSession4User(userId);
		}
		if (session == null) {
			// 根据设备码找不到session
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.NOT_FIND_SESSION);
			return result;
		}

		User user = null;
		Role role = null;

		if (StringUtils.isEmpty(userId) && StringUtils.isNotEmpty(code)) {
			// 要注册新号啊,1。根据code获取token
			Map<String, String> sendParams = new HashMap<>();
			sendParams.put(WeiXinConstant.APPID, SystemConstant.APPID_VAL);
			sendParams.put(WeiXinConstant.SECRET, SystemConstant.SECRET_VAL);
			sendParams.put(WeiXinConstant.CODE, code);
			sendParams.put(WeiXinConstant.GRANT_TYPE, "authorization_code");
			String url = "https://api.weixin.qq.com/sns/oauth2/access_token";
			JSONObject js = HttpManager.get4https(getParamString(url, sendParams));

			String access_token = js.getString(WeiXinConstant.ACCESS_TOKEN);
			int expires_in = js.getIntValue(WeiXinConstant.EXPIRES_IN);
			String refresh_token = js.getString(WeiXinConstant.REFRESH_TOKEN);
			String openid = js.getString(WeiXinConstant.OPENID);
			logger.info("拉取微信的token,code={}, token={},openid={}", code, access_token, openid);
			if (StringUtils.isEmpty(openid)) {
				// 微信拉取的时候。openid为null
				result.put(ErrorCode.RESULT, ErrorCode.FAILED);
				result.put(ErrorCode.CODE, ErrorCode.WEIXIN_OPENID_NULL);
				return result;
			}

			// 2.获取token以后再获取用户信息
			Map<String, String> infoParams = new HashMap<>();
			infoParams.put(WeiXinConstant.ACCESS_TOKEN, access_token);
			infoParams.put(WeiXinConstant.OPENID, openid);
			// 获取用户信息的url
			String infoUrl = "https://api.weixin.qq.com/sns/userinfo";
			JSONObject infoJs = HttpManager.get4https(getParamString(infoUrl, infoParams));

			String nickname = infoJs.getString(WeiXinConstant.NICKNAME); // 名字
			int sex = infoJs.getIntValue(WeiXinConstant.SEX); // 性别
			String province = infoJs.getString(WeiXinConstant.PROVINCE); // 省份
			String city = infoJs.getString(WeiXinConstant.CITY); // 城市
			String country = infoJs.getString(WeiXinConstant.COUNTRY); // 国家
			String headimgurl = infoJs.getString(WeiXinConstant.HEADIMGURL); // 头像url

			logger.info("创建新的用户，从微信那里拉取用户信息， userId={},name={},sex={},maching={}", openid, nickname, sex, machingId);

			// 首先检测一下，当前的openid是否我已经注册过了。我退出或者删了客户端。重新授权，但是我的账号是存在的
			user = getUser(pid, openid);
			if (user == null) {
				user = createUser(pid, openid, access_token, refresh_token, expires_in, machingId,
				        SystemConstant.LOGIN_TYPE_WEIXIN);

				// String imgName = getImgName(pid, user.getUserId(),
				// user.getId());
				role = roleManager.createRole(pid, user.getUserId(), nickname, sex, province, city, country, headimgurl,
				        session.getClientIp());
				// 把头像下载到本地
				// FileUtil.downImg(headimgurl,
				// GameServerContext.getAppConfig().getImgLocal(), headimgurl);
			} else {
				List<Role> roleList = roleManager.getRoleListByUserId(user.getPid(), user.getUserId());
				if (roleList.size() == 0) {
					role = roleManager.createRole(pid, user.getUserId(), nickname, sex, province, city, country,
					        headimgurl, session.getClientIp());
				} else {
					role = roleList.get(0);
				}
				role.setHeadimgurl(headimgurl);
				role.setName(nickname);
				roleManager.updateRole(role);
			}
			// 替换之前用机器码临时保存的session
			session.setUserId(user.getUserId());
			SessionManager.getInstance().replaceSession4User(machingId, user.getUserId());
		} else {
			user = getUser(pid, userId);
		}

		JSONObject check = checkValidate(user);
		if (check != null) {
			return check;
		}

		role = roleManager.getRole(user.getPid(), user.getUserId());

		session.setRoleId(role.getId());
		session.setRoleName(role.getName());
		SessionManager.getInstance().addSession4Role(session, role.getId());

		// 检测token过期
		// checkRefreshToken(user);

		long nowTime = System.currentTimeMillis();

		Object[] data = new Object[] { nowTime, TimeZone.getDefault().getOffset(nowTime), roleManager.toRoleVo(role),
		        user.getUserId(), session.getClientIp() };
		result.put(MahjongConstant.CLIENT_DATA, data);
		routeManager.relayMsg(user.getUserId(), MsgType.LoginGame_C2S_Msg, result);

		LoginGameEvent.publish(role.getId(), session.getClientIp());
		logger.info("login game end, type={}, pid={},userId={},machingId={},roleId={}, name={}", 1, pid,
		        user.getUserId(), machingId, role.getId(), role.getName());
		return null;
	}

	/**
	 * 获取图片名字
	 *
	 * @param roleId
	 * @param userId
	 * @param id
	 * @return
	 */
	private String getImgName(String pid, String userId, long id) {
		return new StringBuffer().append(pid).append(userId).append(id % IdMaxConstant.USER).append(".jpg").toString();
	}

	/**
	 * 检查token是否过期了 没有任何意义啊
	 *
	 * @param user
	 */
	private void checkRefreshToken(User user) {
		Date now = new Date();
		Date tokenEndTime = user.getTokenEndTime();
		// 当前时间 大于 过期时间。直接刷新
		if (now.getTime() >= tokenEndTime.getTime()) {
			refreshToken(user);
		} else {
			// 过期时间和当前时间相差2天，我也刷
			int sub = TimeUtil.subDateToDay(now, tokenEndTime);
			if (sub <= 2) {
				refreshToken(user);
			}
		}
	}

	/**
	 * 刷新token
	 *
	 * @param user
	 */
	private void refreshToken(User user) {
		String refreshUlr = "https://api.weixin.qq.com/sns/oauth2/refresh_token";
		Map<String, String> refreshParams = new HashMap<>();
		refreshParams.put(WeiXinConstant.APPID, SystemConstant.APPID_VAL);
		refreshParams.put(WeiXinConstant.GRANT_TYPE, "refresh_token");
		refreshParams.put(WeiXinConstant.REFRESH_TOKEN, user.getRefreshToken());
		JSONObject infoJs = HttpManager.get4https(getParamString(refreshUlr, refreshParams));
		String access_token = infoJs.getString(WeiXinConstant.ACCESS_TOKEN);
		int expires_in = infoJs.getIntValue(WeiXinConstant.EXPIRES_IN);
		Date now = new Date();
		Date tokenEndTime = DateUtils.addMilliseconds(now, expires_in);
		user.setTokenEndTime(tokenEndTime);
		user.setAccessToken(access_token);
		userRepository.cacheUpdate(user);
	}

	/**
	 * 拼接链接
	 *
	 * @param url
	 * @param sendParams
	 * @return
	 */
	public String getParamString(String url, Map<String, String> sendParams) {
		StringBuilder sb = new StringBuilder();
		sb.append(url);

		int i = 0;
		for (String key : sendParams.keySet()) {
			if (i == 0) {
				sb.append("?");
			} else {
				sb.append("&");
			}
			i++;
			try {
				sb.append(key).append("=").append(URLEncoder.encode(sendParams.get(key), "UTF-8"));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return sb.toString();
	}

	/**
	 * 游客登陆 ，一个设备，只能有一个用户
	 *
	 * @param pid
	 * @param userId
	 * @param machingId
	 * @param code
	 * @return
	 */
	private JSONObject loginYouKe(String pid, String userId, String machingId, String code) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Session session = null;
		if (StringUtils.isEmpty(userId)) {
			session = SessionManager.getInstance().getSession4User(machingId);
		} else {
			session = SessionManager.getInstance().getSession4User(userId);
		}
		if (session == null) {
			// 根据设备码找不到session
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.NOT_FIND_SESSION);
			return result;
		}

		User user = null;
		boolean add = false; // 是否要创建玩家
		// 通过机器码找到userid
		String u = userRepository.getUserIdByMachingId(machingId);
		if (StringUtils.isEmpty(u)) {
			// 找不到userId，那就创建一个
			add = true;
		} else {
			user = getUser(pid, u);
		}

		if (add) {
			// 创建user
			user = createUser(pid, userId, "", "", 0, machingId, SystemConstant.LOGIN_TYPE_YOUKE);

			String name = SystemConstant.YOUKE_NAME + user.getId() % IdMaxConstant.USER;
			roleManager.createRole(pid, user.getUserId(), name, 2, "", "", "", "", session.getClientIp());
		}

		// 保险起见，放到这里把。不要相信客户端啊
		session.setUserId(user.getUserId());
		if (StringUtils.isEmpty(userId)) {
			SessionManager.getInstance().replaceSession4User(machingId, user.getUserId());
		} else {
			SessionManager.getInstance().replaceSession4User(userId, user.getUserId());
		}

		JSONObject check = checkValidate(user);
		if (check != null) {
			return check;
		}

		Role role = roleManager.getRole(user.getPid(), user.getUserId());

		session.setRoleId(role.getId());
		session.setRoleName(role.getName());
		SessionManager.getInstance().addSession4Role(session, role.getId());

		long nowTime = System.currentTimeMillis();
		// result.put("nowTime", nowTime);
		// result.put("offset", TimeZone.getDefault().getOffset(nowTime));
		// result.put("roleId", role.getId());
		// result.put("name", role.getName());
		// result.put("gender", role.getGender());
		// result.put("diamond", role.getDiamond());
		// result.put("headImgUrl", role.getHeadimgurl());
		// result.put("userId", user.getUserId());
		// result.put("ip", session.getClientIp());
		Object[] data = new Object[] { nowTime, TimeZone.getDefault().getOffset(nowTime), roleManager.toRoleVo(role),
		        user.getUserId(), session.getClientIp() };
		result.put(MahjongConstant.CLIENT_DATA, data);
		routeManager.relayMsg(user.getUserId(), MsgType.LoginGame_C2S_Msg, result);

		LoginGameEvent.publish(role.getId(), role.getIp());
		logger.info("login game end, type={}, pid={},userId={},machingId={},roleId={}, name={}", 2, pid,
		        user.getUserId(), machingId, role.getId(), role.getName());
		return null;
	}

	/**
	 * 是否有效的验证
	 *
	 * @param user
	 * @return
	 */
	private JSONObject checkValidate(User user) {
		// 是否为内部玩家,服务器在维护状态下。不让普通玩家进入
		ServerInfo serverInfo = GameServerContext.getServerInfo();
		int status = serverInfo.getStatus();
		if (status == SystemConstant.SERVER_STATUS_MAINTAIN) {
			if (user.getType() == UserConstant.TYPE_PLAYER) {
				JSONObject result = new JSONObject();
				result.put(ErrorCode.RESULT, ErrorCode.FAILED);
				result.put(ErrorCode.CODE, ErrorCode.SERVER_MAINTAINING);
				return result;
			}
		}
		return null;
	}

	/**
	 * 创建角色
	 *
	 * @param pid
	 * @param userId
	 * @return
	 */
	public User createUser(String pid, String userId, String accessToken, String refreshToken, int expires_in,
	        String machingId, int createType) {
		User user = new User();
		user.setId(idManager.newId(TableNameConstant.USER));
		user.setPid(pid);
		if (createType == SystemConstant.LOGIN_TYPE_YOUKE) {
			userId = SystemConstant.YOUKE + user.getId() % IdMaxConstant.USER;
		}
		user.setUserId(userId);
		user.setAccessToken(accessToken);
		user.setRefreshToken(refreshToken);
		user.setMachingId(machingId);
		user.setType(UserConstant.TYPE_PLAYER);
		Date now = new Date();
		Date tokenEndTime = DateUtils.addMilliseconds(now, expires_in);
		user.setTokenEndTime(tokenEndTime);
		user.setAddTime(now);
		user.setModifyTime(user.getAddTime());
		userRepository.cacheInsert(user);

		return user;
	}
}