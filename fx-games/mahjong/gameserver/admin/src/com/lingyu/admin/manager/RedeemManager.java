package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lingyu.admin.dao.RedeemMailRecordDao;
import com.lingyu.admin.dao.RedeemRecordDao;
import com.lingyu.admin.network.AsyncHttpClient;
import com.lingyu.admin.network.GameClient;
import com.lingyu.admin.network.GameClientManager;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.ItemVo;
import com.lingyu.admin.vo.RetCode;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.RedeemMailRecord;
import com.lingyu.common.entity.RedeemRecord;
import com.lingyu.common.entity.User;
import com.lingyu.msg.http.NewRedeemRoleDTO;
import com.lingyu.msg.http.RedeemItemDTO;
import com.lingyu.msg.http.Redeem_C2S_Msg;
import com.lingyu.msg.http.Redeem_S2C_Msg;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class RedeemManager {
	private static final Logger logger = LogManager.getLogger(RedeemManager.class);

	@Autowired
	private GameClientManager gameClientManager;
	@Autowired
	private GameAreaManager gameAreaManager;
	@Autowired
	private ItemDataTemplateManager dataTemplateManager;
	@Autowired
	private RedeemRecordDao redeemRecordDao;
	@Autowired
	private RedeemMailRecordDao redeemMailRecordDao;
	
	@Autowired
	private OptResultManager optResultManager;

	public void init() {
		logger.info("补偿记录缓存化开始");
		// List<RedeemRecord> list =
		// redeemRecordDao.getRecentRecords(REDEEM_RECORD_COUNT);
		logger.info("补偿记录缓存化完毕");
	}


	public Redeem_S2C_Msg redeem(String mailTitle, String mailContent, int selectRoleType, String roleArray,
			Integer money, Integer diamond, String itemArray) {
		Redeem_C2S_Msg msg = new Redeem_C2S_Msg();
		msg.setMailTitle(mailTitle);
		msg.setMailContent(mailContent);
		msg.setSelectRoleType(selectRoleType);
		if (StringUtils.isNotEmpty(roleArray)) {
			String[] ss = StringUtils.split(roleArray, ",");
			List<NewRedeemRoleDTO> list = new ArrayList<NewRedeemRoleDTO>();
			for (int i = 0; i < ss.length; i += 2) {
				NewRedeemRoleDTO roleDTO = new NewRedeemRoleDTO();
				roleDTO.setUserId(ss[i]);
				roleDTO.setName(ss[i + 1]);
				list.add(roleDTO);
			}
			msg.setRedeemRoles(list);
		}
		if (money != null) {
			msg.setMoney(money);
		}
		if (diamond != null) {
			msg.setDiamond(diamond);
		}
		if (StringUtils.isNotEmpty(itemArray)) {
			String[] ss = StringUtils.split(itemArray, ",");
			List<RedeemItemDTO> list = new ArrayList<RedeemItemDTO>();
			for (int i = 0; i < ss.length; i += 3) {
				RedeemItemDTO roleDTO = new RedeemItemDTO();
				roleDTO.setItemId(ss[i]);
				roleDTO.setItemName(ss[i + 1]);
				roleDTO.setCount(Integer.parseInt(ss[i + 2]));
				list.add(roleDTO);
			}
			msg.setRedeemItems(list);
		}

		// 保存补偿记录
		try {
			User user = SessionUtil.getCurrentUser();
			List<Integer> areaList = new ArrayList<>(1);
			areaList.add(user.getLastAreaId());
			RedeemRecord rr = createRedeemRecord(selectRoleType, itemArray, false, areaList, msg, user, true);
			addRedeemRecord(rr);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		GameClient gameClient = gameClientManager.getCurrentGameClient();

		return gameClient.redeem(msg);
	}

	public Object[] redeemMultiAreas(String mailTitle, String mailContent, int selectRoleType, String roleArray,
			Integer money, Integer diamond, String itemArray, boolean allArea, List<Integer> areaList,
			List<Integer> orignalAreaList, String redeemType) {
		Redeem_C2S_Msg msg = new Redeem_C2S_Msg();
		msg.setMailTitle(mailTitle);
		msg.setMailContent(mailContent);
		msg.setSelectRoleType(selectRoleType);
		int insertRecordId = 0;
		if (StringUtils.isNotEmpty(roleArray)) {
			String[] ss = StringUtils.split(roleArray, ",");
			List<NewRedeemRoleDTO> list = new ArrayList<NewRedeemRoleDTO>();
			for (int i = 0; i < ss.length; i += 2) {
				NewRedeemRoleDTO roleDTO = new NewRedeemRoleDTO();
				roleDTO.setUserId(ss[i]);
				roleDTO.setName(ss[i + 1]);
				list.add(roleDTO);
			}
			msg.setRedeemRoles(list);
		}
		if (money != null) {
			msg.setMoney(money);
		}
		if (diamond != null) {
			msg.setDiamond(diamond);
		}
		if (StringUtils.isNotEmpty(itemArray)) {
			String[] ss = StringUtils.split(itemArray, ",");
			List<RedeemItemDTO> list = new ArrayList<RedeemItemDTO>();
			for (int i = 0; i < ss.length; i += 5) {
				RedeemItemDTO roleDTO = new RedeemItemDTO();
				roleDTO.setItemId(ss[i]);
				roleDTO.setItemName(ss[i + 1]);
				roleDTO.setCount(Integer.parseInt(ss[i + 2]));
				roleDTO.setBind(Integer.parseInt(ss[i + 3]));
				String alink = ss[i + 4];
				Pattern p = Pattern.compile("title=\"(.*?)\"");
				Matcher m = p.matcher(alink);
				String extendShuXing = "";
				if (m.find()) {
					extendShuXing = m.group(1);
				}
				roleDTO.setExtendShuxing(extendShuXing);
				list.add(roleDTO);
			}
			msg.setRedeemItems(list);
		}

		User user = SessionUtil.getCurrentUser();
		int counter = 0;
		Collection<GameArea> list = null;
		boolean hasCheckPrivilege = false;
		GameArea onlyGameArea = null;
		// 如果是邮件补偿 不需要权限验证
		if (user.getPrivilegeList().contains(PrivilegeConstant.MENU_PLAY_RECOUP_CHECK) || redeemType.equals("mail")) { // 帐号有审核权限
			hasCheckPrivilege = true;
			list = gameAreaManager.getHandleGameAreaList(user.getLastPid(), allArea, areaList);
			for (GameArea area : list) {
				if (area.getFollowerId() == 0) {
					onlyGameArea = area;
					counter++;
					if (counter > 1) {
						break;
					}
				}
			}
		}
		if (null == redeemType || redeemType.trim().equals("")) {
			// 保存补偿记录
			try {
				RedeemRecord rr = createRedeemRecord(selectRoleType, itemArray, allArea, orignalAreaList, msg, user,
						hasCheckPrivilege);
				insertRecordId = addRedeemRecord(rr);
				logger.info(
						"redeem: admin={}, selectRoleType={}, roleArray={}, money={}, diamond={}, itemArray={}, allArea={}, areaList={}, orignalAreaList={}",
						user.getName(), selectRoleType, roleArray, money, diamond, itemArray, allArea,
						areaList != null ? Arrays.toString(areaList.toArray()) : "",
						orignalAreaList != null ? Arrays.toString(orignalAreaList.toArray()) : "");
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			// 邮件补偿
			RedeemMailRecord mailRecord = createRedeemMailRecord(selectRoleType, allArea, orignalAreaList, msg, user);
			insertRecordId = addRedeemMailRecord(mailRecord);
			logger.info(
					"redeem 邮件补偿: admin={}, selectRoleType={}, roleArray={}, money={}, diamond={}, allArea={}, areaList={}, orignalAreaList={}",
					user.getName(), selectRoleType, roleArray, money, diamond, allArea,
					areaList != null ? Arrays.toString(areaList.toArray()) : "",
					orignalAreaList != null ? Arrays.toString(orignalAreaList.toArray()) : "");
		}

		if (hasCheckPrivilege) { // 有审核权限 直接发放
			if (counter > 1) {
				msg.setSerialId(optResultManager.incrementAndGet());
				AsyncHttpClient.getInstance().send(list, msg);
				Redeem_S2C_Msg retMsg = new Redeem_S2C_Msg();
				retMsg.setSerialId(msg.getSerialId());
				retMsg.setRetCode(RetCode.ASYN_SUCCESS.getCode());
				return new Object[] { retMsg, insertRecordId };
			} else {
				if (onlyGameArea != null) {
					GameClient gameClient = gameClientManager.getGameClient(onlyGameArea.getWorldId());
					return new Object[] { gameClient.redeem(msg), insertRecordId };
				}
			}
		}
		Redeem_S2C_Msg ret = new Redeem_S2C_Msg();
		ret.setRetCode(ErrorCode.EC_OK);
		return new Object[] { ret, insertRecordId };
	}

	private RedeemRecord createRedeemRecord(int selectRoleType, String itemArray, boolean allArea,
			List<Integer> areaList, Redeem_C2S_Msg msg, User user, boolean hasCheckPrivilege) {
		RedeemRecord rr = new RedeemRecord();
		rr.setAdminId(user.getId());
		rr.setAdminName(user.getNickName());
		rr.setIp(user.getLastLoginIp());
		rr.setAll(selectRoleType == 1);
		rr.setCoin(msg.getMoney());
		rr.setDiamond(msg.getDiamond());
		rr.setItems(itemArray);
		rr.setAllArea(allArea);
		if (!hasCheckPrivilege) {
			rr.setRedeemMsg(JSON.toJSONString(msg));
		} else {
			rr.setStatus(RedeemRecord.STATUS_ACCEPTED);
			rr.setCheckTime(new Date());
			rr.setCheckAdminId(user.getId());
			rr.setCheckAdminName(user.getNickName());
		}
		StringBuilder areaSb = new StringBuilder();
		if (CollectionUtils.isNotEmpty(areaList)) {
			rr.setAreaIdList(JSON.toJSONString(areaList));
			areaSb.append("[");
			for (Integer areaId : areaList) {
				if (areaSb.length() > 1) {
					areaSb.append(",");
				}
				GameArea area = gameAreaManager.getGameAreaByAreaId(user.getLastPid(), areaId);
				if (area != null) {
					areaSb.append(area.getAreaName());
				} else {
					areaSb.append(areaId);
				}
			}
			areaSb.append("]");
		}
		rr.setAreas(areaSb.toString());
		// rr.setAreas(areaList != null?
		// Arrays.toString(areaList.toArray()):"");
		if (CollectionUtils.isNotEmpty(msg.getRedeemRoles())) {
			StringBuilder roleSb = new StringBuilder();
			for (NewRedeemRoleDTO role : msg.getRedeemRoles()) {
				if (roleSb.length() > 0) {
					roleSb.append(",");
				}
				roleSb.append(role.getName());
			}
			rr.setPlayers(roleSb.toString());
		} else {
			rr.setPlayers("");
		}
		rr.setAddTime(new Date());
		return rr;
	}

	private RedeemMailRecord createRedeemMailRecord(int selectRoleType, boolean allArea, List<Integer> areaList,
			Redeem_C2S_Msg msg, User user) {
		RedeemMailRecord rr = new RedeemMailRecord();
		rr.setAdminId(user.getId());
		rr.setAdminName(user.getNickName());
		rr.setIp(user.getLastLoginIp());
		rr.setAll(selectRoleType == 1);
		rr.setCoin(msg.getMoney());
		rr.setDiamond(msg.getDiamond());
		rr.setAllArea(allArea);
		rr.setRedeemMsg(msg.getMailContent());
		StringBuilder areaSb = new StringBuilder();
		if (CollectionUtils.isNotEmpty(areaList)) {
			rr.setAreaIdList(JSON.toJSONString(areaList));
			areaSb.append("[");
			for (Integer areaId : areaList) {
				if (areaSb.length() > 1) {
					areaSb.append(",");
				}
				GameArea area = gameAreaManager.getGameAreaByAreaId(user.getLastPid(), areaId);
				if (area != null) {
					areaSb.append(area.getAreaName());
				} else {
					areaSb.append(areaId);
				}
			}
			areaSb.append("]");
		}
		rr.setAreas(areaSb.toString());
		// rr.setAreas(areaList != null?
		// Arrays.toString(areaList.toArray()):"");
		if (CollectionUtils.isNotEmpty(msg.getRedeemRoles())) {
			StringBuilder roleSb = new StringBuilder();
			for (NewRedeemRoleDTO role : msg.getRedeemRoles()) {
				if (roleSb.length() > 0) {
					roleSb.append(",");
				}
				roleSb.append(role.getName());
			}
			rr.setPlayers(roleSb.toString());
		} else {
			rr.setPlayers("");
		}
		rr.setAddTime(new Date());
		return rr;
	}

	private int addRedeemRecord(RedeemRecord redeemRecord) {
		redeemRecordDao.add(redeemRecord);
		return redeemRecord.getId();
	}

	private int addRedeemMailRecord(RedeemMailRecord redeemMailRecord) {
		redeemMailRecordDao.add(redeemMailRecord);
		return redeemMailRecord.getId();
	}

	public List<RedeemRecord> getRedeemRecords(int page, int rows) {
		return redeemRecordDao.getRecords(page, rows);
	}

	public List<RedeemMailRecord> getMailRedeemRecords(int page, int rows) {
		return redeemMailRecordDao.getRecords(page, rows);
	}

	public int getRecordsCount() {
		return redeemRecordDao.size();
	}

	public int getMailRecordsCount() {
		return redeemMailRecordDao.size();
	}

	public Redeem_S2C_Msg checkRedeem(int id, boolean redeemAccept) {
		Redeem_S2C_Msg ret = new Redeem_S2C_Msg();
		List<String> messages = new ArrayList<String>(1);
		RedeemRecord redeemRecord = redeemRecordDao.getRedeemRecordById(id);
		if (redeemRecord == null) {
			messages.add("RedeemRecord not found!");
		} else if (redeemRecord.getStatus() != RedeemRecord.STATUS_APPLYING) {
			messages.add("RedeemRecord has checked!");
		} else {
			User user = SessionUtil.getCurrentUser();
			redeemRecord.setStatus(redeemAccept ? RedeemRecord.STATUS_ACCEPTED : RedeemRecord.STATUS_REJECTED);
			redeemRecord.setCheckAdminId(user.getId());
			redeemRecord.setCheckAdminName(user.getNickName());
			redeemRecord.setCheckTime(new Date());
			redeemRecordDao.update(redeemRecord);

			logger.info("checkRedeem: checkAdminId={}, checkAdminName={}, redeemId={}, accepted={}", user.getId(),
					user.getName(), id, redeemAccept);

			if (redeemAccept) {
				List<Integer> areaList = Collections.emptyList();
				if (StringUtils.isNotEmpty(redeemRecord.getAreaIdList())) {
					areaList = JSON.parseArray(redeemRecord.getAreaIdList(), Integer.class);
				}

				// 发奖励
				Collection<GameArea> list = gameAreaManager.getHandleGameAreaList(user.getLastPid(),
						redeemRecord.isAllArea(), areaList);
				int counter = 0;
				GameArea onlyGameArea = null;
				for (GameArea area : list) {
					if (area.getFollowerId() == 0) {
						counter++;
						onlyGameArea = area;
						if (counter > 1) {
							break;
						}
					}
				}

				Redeem_C2S_Msg msg = JSON.parseObject(redeemRecord.getRedeemMsg(), Redeem_C2S_Msg.class);

				if (counter > 1) {
					AsyncHttpClient.getInstance().send(list, msg);
					ret.setRetCode(RetCode.ASYN_SUCCESS.getCode());
				} else {
					if (onlyGameArea != null) {
						GameClient gameClient = gameClientManager.getGameClient(onlyGameArea.getWorldId());
						return gameClient.redeem(msg);
					}
				}
			} else {
				ret.setRetCode(ErrorCode.EC_OK);
			}
		}
		ret.setMessages(messages);
		return ret;
	}

	/**
	 * 没有角色的记录过滤
	 */
	public void updateRecord(String redeemType, int recordId, List<String> roleMsgs) {
		// 解析错误角色名字
		if (roleMsgs != null) {
			Set<String> errorRole = new HashSet<>();
			for (String str : roleMsgs) {
				Pattern p = Pattern.compile("rolename:(.*)的角色");
				Matcher m = p.matcher(str);
				while (m.find()) {
					errorRole.add(m.group(1));
				}
			}
			if (null == redeemType || redeemType.trim().equals("")) {
				// 补偿
				RedeemRecord record = redeemRecordDao.getRedeemRecordById(recordId);
				String plays = record.getPlayers();
				StringBuilder errorRoles = new StringBuilder();
				StringBuilder updateRoles = new StringBuilder();
				String[] playss = plays.split(",");
				if (null != playss && playss.length != 0) {
					for (int i = 0; i < playss.length; i++) {
						if (errorRole.contains(playss[i])) {
							errorRoles.append(playss[i] + ",");
						} else {
							updateRoles.append(playss[i] + ",");
						}
					}
				}
				if (updateRoles.length() != 0) {
					record.setPlayers(updateRoles.substring(0, updateRoles.length() - 1).toString());
					redeemRecordDao.update(record);
				}
			} else {
				// 邮件
				RedeemMailRecord record = redeemMailRecordDao.getRedeemRecordById(recordId);
				String plays = record.getPlayers();
				StringBuilder errorRoles = new StringBuilder();
				StringBuilder updateRoles = new StringBuilder();
				String[] playss = plays.split(",");
				if (null != playss && playss.length != 0) {
					for (int i = 0; i < playss.length; i++) {
						if (errorRole.contains(playss[i])) {
							errorRoles.append(playss[i] + ",");
						} else {
							updateRoles.append(playss[i] + ",");
						}
					}
				}
				if (errorRoles.length() != 0) {
					record.setPlayers(updateRoles.substring(0, updateRoles.length() - 1).toString());
					redeemMailRecordDao.update(record);
				}
			}
		}
	}
	
}
