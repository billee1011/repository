package com.lingyu.common.entity;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.time.DateUtils;

import com.lingyu.common.id.ServerObject;
import com.lingyu.game.service.mail.MailConstant;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.annotation.Table;

@Entity
@Table(name = "mail")
public class Mail extends ServerObject {
	@IsRoleId
	@Column(name = "role_id")
	private long roleId;
	
	@Column(name = "title", length = 128, comment = "标题")
	private String title;
	
	@Column(name = "content", length = 1024, comment = "内容")
	private String content;
	
	@Column(name = "sender_id", comment = "发件人ID")
	private long senderId;
	
	@Column(name = "sender_name", length = 36, comment = "发件人名称")
	private String senderName;
	
	// 如果有附件的，领取了附件才算已读，没有附件的，点开看了就算
	@Column(name = "status", nullable = false, defaultValue = "0", comment = "邮件状态")
	private int status;
	
	@Column(name = "attachment", length = 65535, defaultValue = "{}", comment = "附件")
	private String attachment;
	
	@Column(name = "diamond", nullable = false, defaultValue = "0", comment = "钻石数")
	private int diamond;
	
	@Column(name = "add_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "创建时间")
	private Date addTime;
	
	@Column(name = "modify_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "修改时间")
	private Date modifyTime;
	
	@Column(name = "mail_type", nullable = false, defaultValue = MailConstant.TYPE_SYSTEM + "", comment = "邮件类型")
	private int mailType;
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getSenderId() {
		return senderId;
	}

	public void setSenderId(long senderId) {
		this.senderId = senderId;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}

	public int getDiamond() {
		return diamond;
	}

	public void setDiamond(int diamond) {
		this.diamond = diamond;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public Date getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}

	public int getMailType() {
		return mailType;
	}

	public void setMailType(int mailType) {
		this.mailType = mailType;
	}

	public Date getEndTime() {
		int interval = MailConstant.MAX_DAY_NOATTACHMENT;

		if (this.getMailType() == MailConstant.TYPE_FRIEND_GIFT) { // 优先判断类型
			interval = MailConstant.MAX_DAY_FRIEND_GIFT;
		} else if (this.hasAttachment(this)) {
			interval = MailConstant.MAX_DAY_HASATTACHMENT;
		}
		Date ret = DateUtils.addDays(addTime, interval);
		return ret;
	}

	private boolean hasAttachment(Mail mail) {
		if (mail.getDiamond() > 0) {
			return true;
		}
		if (StringUtils.isNotEmpty(mail.getAttachment())) {
			return true;
		}
		return false;
	}

	/**
	 * 是否有附件
	 * 
	 * @return
	 */
	public boolean hasAttachment() {
		if (this.diamond > 0) {
			return true;
		}
		if (StringUtils.isNotEmpty(this.attachment)) {
			return true;
		}
		return false;
	}

	/**
	 * 邮件附件状态<br>
	 * [0.没有附件; 1.有附件; 2.附件已经领取]
	 * 
	 * @return
	 */
	public int attachmentStatus() {
		int isNull = 0; // 没有附件
		int isHave = 1; // 有附件
		int hadReceive = 2; // 附件已经领取
		if (this.diamond > 0) {
			return isHave;
		}
		if (StringUtils.isNotEmpty(this.attachment)) {
			return isHave;
		}
		if (this.attachment == null) {
			return isNull;
		}
		return hadReceive;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
