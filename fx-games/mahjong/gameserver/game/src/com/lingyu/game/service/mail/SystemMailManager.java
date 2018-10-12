package com.lingyu.game.service.mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.constant.OperateConstant.OperateType;
import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.RoleRedeemInfo;
import com.lingyu.common.entity.SystemMail;
import com.lingyu.common.util.TimeUtil;

@Service
public class SystemMailManager {
	private static final Logger logger = LogManager.getLogger(SystemMailManager.class);
	@Autowired
	private SystemMailRepository systemMailRepository;
	@Autowired
	private MailManager mailManager;

	private List<SystemMail> list;

	public void init() {
		// 把系统邮件加载进内存
		list = systemMailRepository.loadAllBySystem();
		filterExpireSystemMail();
	}

	/**
	 * 过滤过期的系统邮件
	 */
	private void filterExpireSystemMail() {
		try {
			Iterator<SystemMail> it = list.iterator();
			long now = System.currentTimeMillis();
			while (it.hasNext()) {
				SystemMail mail = it.next();
				if (mail.getAddTime().getTime() + MailConstant.SYSTEM_MAIL_VALID_MILLIS < now) {
					systemMailRepository.delete(mail);
					it.remove();
					logger.info("deleteExpiredSysMail: mailId={}, mailAddTime={}", mail.getId(), mail.getAddTime());
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/** 创建系统邮件 */
	public void createSystemMail(SystemMail mail) {
		if (mail.getDiamond() > 0) {
			mail.setSerialId(System.currentTimeMillis());
		}
		list.add(mail);
		systemMailRepository.insert(mail);
		logger.info("创建系统邮件");
	}

	/** 创建系统邮件 */
	public SystemMail createSystemEmail(String title, String content, int coin, int diamond, String attachment, Date addTime, OperateType operateType) {
		SystemMail mail = new SystemMail();
		mail.setAttachment(attachment);
		mail.setCoin(coin);
		mail.setTitle(title);
		mail.setContent(content);
		mail.setDiamond(diamond);
		mail.setAddTime(addTime);
		mail.setModifyTime(addTime);
		mail.setCurrencyOptType(operateType.getId());
		this.createSystemMail(mail);
		return mail;
	}

	public void removeSystemMail(long id) {
		SystemMail mail = null;
		for (SystemMail e : list) {
			if (e.getId() == id) {
				mail = e;
				break;
			}
		}
		if (mail != null) {
			list.remove(mail);
			systemMailRepository.delete(mail);
			logger.info("removeSystemMaill: id={}", id);
		}
	}

	public void checkSystemMail(Role role, RoleRedeemInfo roleRedeemInfo) {
		Date offineTime = roleRedeemInfo.getRedeemTime();
		if (offineTime == null || TimeConstant.DATE_LONG_AGO.equals(offineTime)) {// 兼容以前的东东
			offineTime = role.getLastLogoutTime();
		}
		logger.info("systemMaillCheck: step=1, roleId={}, offlineTime={}", role.getId(), offineTime);
		Date createTime = role.getAddTime();
		Date recentRedeemTime = null;
		List<SystemMail> pendingTransferMailList = new ArrayList<>(list.size());
		for (SystemMail e : list) {
			// 获取下线之后起系统发送的所有邮件,并这些邮件是在角色创建后生成的，防止刷小号拿补偿邮件奖励
			if (e.getAddTime().after(createTime) && e.getAddTime().after(offineTime)) {
				pendingTransferMailList.add(e);
				// 计算最新的补偿时间
				if (recentRedeemTime == null || recentRedeemTime.before(e.getAddTime())) {
					recentRedeemTime = e.getAddTime();
				}
			}
		}

		Date updateRedeemTime = new Date();
		if (recentRedeemTime != null && updateRedeemTime.before(recentRedeemTime)) { // 曾经被调过时间有可能进这儿
			logger.warn("systemMaillCheck: step=warn, roleId={}, updateRedeemTime={}, recentRedeemTime={}", role.getId(), updateRedeemTime, recentRedeemTime);
			updateRedeemTime = recentRedeemTime;
		}
		updateRedeemTime = new Date(updateRedeemTime.getTime() + TimeUtil.SECOND);

		mailManager.getRedeemInfoTryUpdate(role.getId(), updateRedeemTime);
		logger.info("systemMaillCheck: step=2, roleId={}, recentRedeemTime={}, updateRedeemTime={}", role.getId(), recentRedeemTime, updateRedeemTime);

		for (SystemMail e : pendingTransferMailList) {
			mailManager.sendMail(e, role.getId(), OperateType.getOperateType(e.getCurrencyOptType()));
			logger.info("systemMailTransfer: roleId={}, roleAddTime={}, offlineTime={}, sysMailId={}, sysMailTime={}, recentRedeemTime={}", role.getId(),
					createTime, offineTime, e.getId(), e.getAddTime(), recentRedeemTime);
		}
	}
}
