package com.lingyu.admin.manager;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.dao.MailTemplateDao;
import com.lingyu.common.entity.MailTemplate;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class MailTemplateManager {
	private static final Logger logger = LogManager.getLogger(MailTemplateManager.class);
	@Autowired
	private MailTemplateDao mailTemplateDao;
	// 内存中的缓存
	private Map<Integer, MailTemplate> mailTemplateMap = new HashMap<Integer, MailTemplate>();

	public void init() {
		logger.info("邮件模板缓存化开始");
		mailTemplateMap.clear();
		List<MailTemplate> list = mailTemplateDao.queryAll();
		for (MailTemplate mailTemplate : list) {
			mailTemplateMap.put(mailTemplate.getId(), mailTemplate);
			logger.info("mailTemplate={}", mailTemplate.toString());
		}
		logger.info("邮件模板缓存化完毕");
	}

	/**
	 * 方法描述： 获取所有邮件模板
	 * 
	 * @return
	 */
	public Collection<MailTemplate> getMailTemplateList() {
		return mailTemplateMap.values();
	}

	/**
	 * 添加邮件模板
	 * 
	 * @param msg
	 * @return
	 */

	public String createMailTemplate(MailTemplate mailTemplate) {
		Date now=new Date();
		mailTemplate.setAddTime(now);
		mailTemplate.setModifyTime(now);
		logger.info("添加邮件模板成功 {}", mailTemplate.toString());
		String retCode = mailTemplateDao.add(mailTemplate);
		if (ErrorCode.EC_OK.equals(retCode)) {
			mailTemplateMap.put(mailTemplate.getId(), mailTemplate);
		}

		return retCode;
	}

	/**
	 * 修改邮件模板
	 * 
	 * @param msg
	 * @return
	 */
	public String updateMailTemplate(MailTemplate mailTemplate) {
		Date now=new Date();
		mailTemplate.setModifyTime(now);
		String retCode = mailTemplateDao.update(mailTemplate);
		if (ErrorCode.EC_OK.equals(retCode)) {
			mailTemplateMap.put(mailTemplate.getId(), mailTemplate);
		}
		return retCode;
	}

	/**
	 * 删除邮件模板
	 * 
	 * @param msg
	 * @return
	 */
	public String removeMailTemplate(int mailId) {
		MailTemplate mailTemplate = this.getMailTemplate(mailId);
		String retCode = ErrorCode.EC_OK;
		if (mailTemplate != null) {
			retCode = mailTemplateDao.delete(mailTemplate);
			if (ErrorCode.EC_OK.equals(retCode)) {
				mailTemplateMap.remove(mailTemplate.getId());
			}
		} else {
			retCode = ErrorCode.EC_FAILED;
		}

		return retCode;
	}

	public MailTemplate getMailTemplate(int mailId) {
		return mailTemplateMap.get(mailId);
	}

}
