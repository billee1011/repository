package com.lingyu.game.service.mail;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.CurrencyConstant.CurrencyType;
import com.lingyu.common.constant.OperateConstant.OperateType;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.Mail;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.RoleRedeemInfo;
import com.lingyu.common.entity.SystemMail;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.io.Session;
import com.lingyu.common.io.SessionManager;
import com.lingyu.common.util.TimeUtil;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.currency.MoneyManager;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.id.TableNameConstant;
import com.lingyu.game.service.role.RoleManager;
import com.lingyu.msg.http.NewRedeemRoleDTO;
import com.lingyu.msg.http.Redeem_C2S_Msg;
import com.lingyu.msg.http.Redeem_S2C_Msg;

@Service
public class MailManager {
	private static final Logger logger = LogManager.getLogger(MailManager.class);
	@Autowired
	private MailRepository mailRepository;
	@Autowired
	private SystemMailManager systemMailManager;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private RouteManager routeManager;
	@Autowired
	private RoleRedeemInfoRepository roleRedeemInfoRepository;
	@Autowired
	private IdManager idManager;
	@Autowired
	private MoneyManager moneyManager;

	/***
	 * 后台发送邮件 diamond不等于0，可以收取附件的
	 *
	 * @param msg
	 * @return
	 */
	public Redeem_S2C_Msg redeem(Redeem_C2S_Msg msg) {
		Redeem_S2C_Msg ret = new Redeem_S2C_Msg();
		ret.setRetCode(ErrorCode.EC_OK);
		List<NewRedeemRoleDTO> redeemRoles = msg.getRedeemRoles();
		List<Long> roleIds = new LinkedList<Long>();
		boolean redeemAll = false;
		List<String> messages = new ArrayList<String>();
		if (msg.getSelectRoleType() == MailConstant.REDEEM_ALL) {
			redeemAll = true;
		} else if (CollectionUtils.isEmpty(redeemRoles)) {
			ret.setRetCode(ErrorCode.EC_FAILED);
		} else {
			// 剔除不符合条件的角色
			Iterator<NewRedeemRoleDTO> it = redeemRoles.iterator();
			while (it.hasNext()) {
				NewRedeemRoleDTO rrd = it.next();
				long roleId = roleManager.getRoleId(rrd.getName());
				if (roleId == 0L) {
					it.remove();
					messages.add(
					        MessageFormat.format("找不到帐号:{0}且rolename:{1}的角色", rrd.getUserId() + "", rrd.getName()));
				} else {
					Role role = roleManager.getRole(roleId);
					if (role == null || !role.getUserId().equals(rrd.getUserId())) {
						it.remove();
						messages.add(
						        MessageFormat.format("找不到帐号:{0}且rolename:{1}的角色", rrd.getUserId() + "", rrd.getName()));
					} else {
						roleIds.add(role.getId());
					}
				}
			}
		}

		try {
			/*
			 * List<RedeemItemDTO> redeemItems = msg.getRedeemItems(); String
			 * attachment = null; if (CollectionUtils.isNotEmpty(redeemItems)) {
			 * List<ItemSerializeVo> listSerializeVo = new ArrayList<>();
			 * attachment = JSONArray.toJSONString(listSerializeVo); }
			 */
			String attachment = null;
			if (redeemAll) {
				sendRedeemMail(roleIds, msg.getMailTitle(), msg.getMailContent(), msg.getMoney(), msg.getDiamond(),
				        attachment);
			} else if (CollectionUtils.isNotEmpty(roleIds)) {
				sendRedeemMail(roleIds, msg.getMailTitle(), msg.getMailContent(), msg.getMoney(), msg.getDiamond(),
				        attachment);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		ret.setMessages(messages);

		return ret;
	}

	/***
	 * 后台发送给玩家补偿或者活动邮件 如果角色列表为空，则发在线玩家，离线玩家在登录的时候发,列表非空，则在线玩家直接发到缓存，离线玩家直接插库
	 *
	 * @param roleIds
	 * @param title
	 * @param content
	 * @param coin
	 * @param diamond
	 * @param attachment
	 */
	public void sendRedeemMail(List<Long> roleIds, String title, String content, int coin, int diamond,
	        String attachment) {
		logger.info("批量发邮件开始");
		// 批量部分玩家发送邮件
		if (CollectionUtils.isNotEmpty(roleIds)) {
			Date now = new Date();
			for (long e : roleIds) {
				try {
					this.sendMail(e, title, content, now, coin, 0, attachment, 0, OperateType.REDEEM);
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
			}
		} else {
			// 给所有在线玩家发送
			Date redeemTime = new Date();
			Collection<Session> list = SessionManager.getInstance().getOnlineRoleList();
			List<Long> pendingRedeemSessionList = new ArrayList<>(list.size());
			for (Session session : list) {
				long roleId = session.getRoleId();
				if (roleId != 0) {
					pendingRedeemSessionList.add(roleId);
					getRedeemInfoTryUpdate(roleId, redeemTime);
				}
			}

			SystemMail mail = systemMailManager.createSystemEmail(title, content, coin, diamond, attachment, redeemTime,
			        OperateType.REDEEM);
			for (Long roleId : pendingRedeemSessionList) {
				try {
					this.sendMail(mail, roleId, OperateType.REDEEM);
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
			}
		}
		logger.info("批量发邮件完毕");
	}

	/**
	 * 用于游戏后台发邮件
	 *
	 * @param systemEmail
	 * @param roleId
	 */
	public void sendMail(SystemMail systemMail, long roleId, OperateType operateType) {
		this.sendMail(roleId, systemMail.getTitle(), systemMail.getContent(), systemMail.getAddTime(),
		        systemMail.getCoin(), systemMail.getDiamond(), systemMail.getAttachment(), systemMail.getSerialId(),
		        operateType);
	}

	/**
	 * 发送系统邮件给角色
	 *
	 * @param roleId
	 * @param title
	 * @param content
	 * @param addTime
	 * @param coin
	 * @param diamond
	 * @param attachment
	 *            附件||null: 没有附件; 空字符串: 附件领取;
	 * @return
	 */
	public void sendMail(long roleId, String title, String content, Date addTime, int coin, int diamond,
	        String attachment, long serialId, OperateType operateType) {
		sendMail(roleId, 0, title, content, addTime, coin, diamond, attachment, MailConstant.TYPE_SYSTEM, serialId,
		        operateType);
	}

	/**
	 * 发送系统邮件给角色
	 *
	 * @param roleId
	 * @param senderId
	 * @param title
	 * @param content
	 * @param addTime
	 * @param coin
	 * @param diamond
	 * @param attachment
	 *            附件||null: 没有附件; 空字符串: 附件已领取;
	 * @param mailType
	 */
	public void sendMail(long roleId, long senderId, String title, String content, Date addTime, int coin, int diamond,
	        String attachment, int mailType, long serialId, OperateType operateType) {
		Mail mail = new Mail();
		mail.setSenderId(senderId);
		if ("".equals(attachment)) {
			// 附件||null: 没有附件; 空字符串: 附件已领取;
			// 传入附件值为[空字符串]时: 没有附件, 将attachment置为null
			attachment = null;
		}
		mail.setId(idManager.newId(TableNameConstant.MAIL));
		mail.setAttachment(attachment);
		mail.setTitle(title);
		mail.setContent(content);
		mail.setAddTime(addTime);
		mail.setModifyTime(addTime);
		mail.setDiamond(diamond);
		mail.setMailType(mailType);
		mail.setStatus(MailConstant.READ_NO);
		mail.setRoleId(roleId);
		boolean online = SessionManager.getInstance().isOnline(roleId);
		if (online) {
			simplifyMailList(roleId);
			mailRepository.cacheInsert(mail);
			// 通知客户端有新邮件
			JSONObject result = new JSONObject();
			result.put("mail_new", 1);
			routeManager.relayMsg(roleId, MsgType.MAIL_NEW, result);
		} else {
			mailRepository.cacheInsert(mail);
		}
		logger.info("创建邮件 roleId={},id={}", mail.getRoleId(), mail.getId());
	}

	/**
	 * 精简邮件列表, 已读过的删除3天前的邮件
	 */
	public void simplifyMailList(long roleId) {
		List<Mail> list = mailRepository.cacheLoadAll(roleId);
		long now = System.currentTimeMillis();
		List<Mail> removedList = new ArrayList<Mail>();
		for (Mail mail : list) {
			if (mail.getStatus() == MailConstant.READ) {
				Date addTime = mail.getAddTime();
				long end = TimeUtil.getTimeStart(addTime, MailConstant.DELETE_THREE_DAY);
				if (now > end) {
					removedList.add(mail);
				}
			}
		}
		for (Mail e : removedList) {
			// 删除过期邮件
			boolean flag = list.remove(e);
			if (flag) {
				remove(e, MailConstant.MAIL_DEL_SYSTEM_TYPE_EXPIRE);
			}
		}
	}

	/**
	 * 删除邮件
	 *
	 * @param roleId
	 * @param emailIds
	 * @return
	 */
	public void remove(Mail mail, int deleteType) {
		logger.info("删除邮件 roleId={},mailId={}, del mail type={}", mail.getRoleId(), mail.getId(), deleteType);
		mailRepository.cacheDelete(mail);
	}

	/**
	 * 获取邮件列表
	 */
	public JSONObject getMailList(long roleId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);
		JSONArray array = new JSONArray();
		List<Mail> list = mailRepository.cacheLoadAll(roleId);
		for (Mail mail : list) {
			if (mail.getStatus() == MailConstant.DELETE) {
				continue;
			}
			JSONObject object = new JSONObject();
			object.put("id", mail.getId());
			object.put("senderId", mail.getSenderId());
			object.put("senderName", mail.getSenderName());
			object.put("status", mail.getStatus());
			array.add(object);
		}
		result.put("data", array);
		return result;
	}

	/** 获取邮件 */
	public Mail getMail(long roleId, long mailId) {
		return mailRepository.cacheLoad(roleId, mailId);
	}

	/** 打开邮件 */
	public JSONObject openMail(long roleId, long mailId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);
		Mail mail = this.getMail(roleId, mailId);
		if (mail == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.NOT_FIND_MAIL_INFO);
			return result;
		}
		// 更改状态为已读
		if (mail.getDiamond() == 0 && mail.getStatus() == MailConstant.READ_NO) {
			mail.setStatus(MailConstant.READ);
			mailRepository.cacheUpdate(mail);
		}
		result.put("title", mail.getTitle());
		result.put("senderId", mail.getSenderId());
		result.put("senderName", mail.getSenderName());
		result.put("content", mail.getContent());
		result.put("addTime", mail.getAddTime());
		result.put("diamond", mail.getDiamond());
		return result;
	}

	/**
	 * 删除邮件
	 *
	 * @param mail
	 * @param deleteType
	 */
	public Object[] remove(long roleId, long[] mailIds) {
		List<Long> ret = new ArrayList<>();
		for (long id : mailIds) {
			Mail mail = mailRepository.cacheLoad(roleId, id);
			if (mail == null) {
				continue;
			}
			mailRepository.cacheDelete(mail);
			ret.add(id);
		}
		logger.info("删除邮件 roleId={},ids={}, del mail type={}", roleId, mailIds, MailConstant.MAIL_DEL_ROLE_TYPE);
		return new Object[] { ErrorCode.EC_OK, ret.toArray() };
	}

	/**
	 * 领取钻石
	 *
	 * @param roleId
	 * @param mailId
	 * @return
	 */
	public JSONObject gainDiamond(long roleId, long mailId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);

		Mail mail = this.getMail(roleId, mailId);

		if (mail == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.NOT_FIND_MAIL_INFO);
			return result;
		}
		if (mail.getDiamond() == 0) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.DIAMOND_ZERO);
			return result;
		}

		moneyManager.incr(roleId, CurrencyType.DIAMOND_NEW, mail.getDiamond(), OperateType.MAIL_GET);

		mail.setStatus(MailConstant.READ);
		mailRepository.cacheUpdate(mail);
		logger.info("mail lingqu diamond success, roleId={}, gainDiamond={}", roleId, mail.getDiamond());
		return result;
	}

	/**
	 * 获取玩家补偿信息 为空的话创建
	 *
	 * @param roleId
	 * @return
	 */
	public RoleRedeemInfo getRedeemInfoTryUpdate(long roleId, Date redeemTime) {
		RoleRedeemInfo ret = roleRedeemInfoRepository.cacheLoad(roleId);
		if (ret == null) {
			ret = new RoleRedeemInfo();
			ret.setRoleId(roleId);
			if (redeemTime != null) {
				ret.setRedeemTime(redeemTime);
			}
			roleRedeemInfoRepository.cacheInsert(ret);
		} else if (redeemTime != null) {
			ret.setRedeemTime(redeemTime);
			roleRedeemInfoRepository.cacheUpdate(ret);
		}
		return ret;
	}
}
