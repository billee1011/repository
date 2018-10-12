package com.lingyu.game.service.role;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.db.GameRepository;
import com.lingyu.common.entity.Role;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.id.TableNameConstant;
import com.lingyu.game.service.mahjong.MahjongManager;
import com.lingyu.game.service.mahjong.StateType;

@Service
public class RoleManager {
	private static final Logger logger = LogManager.getLogger(RoleManager.class);

	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private MahjongManager mahjongManager;
	@Autowired
	private RouteManager routeManager;
	@Autowired
	private IdManager idManager;

	private int registerNum;
	/** 已注册玩家名 */
	private Map<String, Long> roleNameStore = new HashMap<>();
	private Map<Long, String> roleIdStore = new HashMap<>();

	// 个人锁
	private Map<Long, Byte[]> lockerStore = new ConcurrentHashMap<>();

	public Role getRole(long roleId) {
		return roleRepository.cacheLoad(roleId);
	}

	/**
	 * 获取角色，默认获取第一个把
	 * 
	 * @param pid
	 * @param userId
	 * @return
	 */
	public Role getRole(String pid, String userId) {
		return getRoleListByUserId(pid, userId).get(0);
	}

	public List<Role> getRoleListByUserId(String pid, String userId) {
		return roleRepository.getRoleListByUserId(pid, userId);
	}

	public void init() {
		GameRepository repository = GameServerContext.getGameRepository();
		registerNum = repository.getAllRegistRoleNum();
		logger.info("预加载已注册玩家信息开始 num={}", registerNum);
		roleNameStore = repository.getAllNameMap();
		roleIdStore = MapUtils.invertMap(roleNameStore);
		logger.info("预加载已注册玩家信息完毕 num={}", registerNum);
	}

	/**
	 * 登陆游戏
	 * 
	 * @param roleId
	 * @return
	 */
	public void loginGame(long roleId, String ip) {
		Role role = getRole(roleId);
		Date now = new Date();
		role.setLastLoginTime(now);
		if (!DateUtils.isSameDay(role.getLastLogoutTime(), now)) {
			role.setTotalLoginDays(role.getTotalLoginDays() + 1);
		}
		role.setIp(ip);
		roleRepository.cacheUpdate(role);

		// 断线重连等业务
		int roomNum = role.getRoomNum();
		if (roomNum != 0 && role.getState() != 0) {
			switch (StateType.getStateType(role.getState())) {
			case WAIT:
				mahjongManager.refreshJoinRoom(roleId, roomNum);
				break;
			case STARTGAME:
				mahjongManager.reLogin(roleId);
				break;
			}
		}
	}

	/**
	 * 登出游戏
	 * 
	 * @param roleId
	 */
	public void logoutGame(long roleId) {
		Role role = getRole(roleId);
		if (role == null) {
			// 这里是正常的。玩家没有登陆，断开socket就会出现这种情况
			logger.info("玩家登出，role is null。roleId={}", roleId);
			return;
		}
		Date now = new Date();
		role.setLastLogoutTime(now);
		long onlineMs = (role.getLastLoginTime().getTime() - role.getLastLogoutTime().getTime()) / 1000;
		role.setOnlineMillis(onlineMs);
		roleRepository.cacheUpdate(role);
		logger.info("玩家登出,roleId={}, name={}", roleId, role.getName());
	}

	/**
	 * 创建角色
	 * 
	 * @param pid
	 * @param userId
	 * @param name
	 * @param gender
	 * @param ip
	 * @return
	 */
	public Role createRole(String pid, String userId, String name, int gender, String province, String city,
	        String country, String headimg, String ip) {
		Role role = new Role();
		role.setId(idManager.newId(TableNameConstant.ROLE));
		role.setPid(pid);
		role.setUserId(userId);
		role.setName(name);
		role.setGender(gender);
		role.setProvince(province);
		role.setCity(city);
		role.setCountry(country);
		role.setHeadimgurl(headimg);
		role.setIp(ip);
		role.setAddTime(new Date());
		role.setModifyTime(role.getAddTime());
		role.setLastLoginTime(role.getAddTime());
		role.setLastLogoutTime(TimeConstant.DATE_LONG_AGO);
		roleRepository.cacheInsert(role);

		this.add2Store(role.getId(), name);

		return role;
	}

	public void updateRole(Role role) {
		roleRepository.cacheUpdate(role);
	}

	public void add2Store(long roleId, String name) {
		registerNum++;
		roleNameStore.put(name, roleId);
		roleIdStore.put(roleId, name);
	}

	/** 通过角色ID获取角色名 */
	public String getRoleName(long roleId) {
		return roleIdStore.get(roleId);
	}

	public Object[] toRoleVo(Role role) {
		return new Object[] { role.getId(), role.getName(), role.getGender(), role.getDiamond(), role.getHeadimgurl() };
	}

	/** 通过角色名获取角色ID */
	public long getRoleId(String name) {
		long ret = 0;
		Long value = roleNameStore.get(name);
		if (value != null) {
			ret = value.longValue();
		}
		return ret;
	}

	/** 获取个人锁，不用回收，反正很小 */
	public Object getLock(long roleId) {
		Byte[] ret = lockerStore.get(roleId);
		if (ret == null) {
			ret = new Byte[] {};
			lockerStore.put(roleId, ret);
		}
		return ret;
	}
}