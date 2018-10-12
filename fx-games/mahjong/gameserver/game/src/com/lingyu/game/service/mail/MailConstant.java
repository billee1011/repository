package com.lingyu.game.service.mail;

import com.lingyu.common.util.TimeUtil;

public class MailConstant {
	//玩家补偿
	/** 部分补偿 */
	public static final int REDEEM_PART = 0;
	/** 全部补偿*/
	public static final int REDEEM_ALL = 1; 
	
	/** 邮件状态-已读取*/
	public static int READ = 1;
	/** 邮件状态-未读取*/
	public static int READ_NO = 0;
	/** 邮件状态-已删除 */
	public static int DELETE = 2;
	
	/** 删除几天前已读的邮件*/
	public static int DELETE_THREE_DAY = 3;
	
	/** 系统删除邮件-邮件过期 */
	public static int MAIL_DEL_SYSTEM_TYPE_EXPIRE = 1;
	
	/** 玩家手动删除邮件 */
	public static int MAIL_DEL_ROLE_TYPE = 2;
	
	/**
	 * 系统邮件有效毫秒数
	 */
	public static final long SYSTEM_MAIL_VALID_MILLIS = DELETE_THREE_DAY * TimeUtil.DAY; 
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/** 系统业务[邮件上限时]删除邮件-数量上限 */
	public static int MAIL_DEL_SYSTEM_TYPE_MAX = 1;
	

	/**
	 * 最大邮件数量
	 */
	public static final int MAX_COUNT = 100;

	/**
	 * 最大保留时间（没有附件）
	 */
	public static final int MAX_DAY_NOATTACHMENT = 15;

	/**
	 * 最大保留时间（有附件）
	 */
	public static final int MAX_DAY_HASATTACHMENT = 30;
	
	/**
	 * 最大保留时间 （好友礼物）
	 */
	public static final int MAX_DAY_FRIEND_GIFT = 3;

	/**
	 * 最大附件数量
	 */
	public static final int MAX_ATT_COUNT = 6;
	
	
	
	
	//-----------------------邮件类型------------------------------
	/** 系统邮件 */
	public static final int TYPE_SYSTEM = 0;
	/** 好友送礼 */
	public static final int TYPE_FRIEND_GIFT = 1;
	/** 拍卖行购买获得 */
	public static final int TYPE_BUY_AUCTION_ITEMS = 2;
	/** 拍卖行寄售物品到期退回 */
	public static final int TYPE_AUCTION_EXPIRE_BACK = 3;
	/** 系统遣返拍卖行中寄售的物品 */
	public static final int TYPE_SYSTEM_REPATRIATE_AUCTION_ITEM = 4;
	//-----------------------邮件类型------------------------------
	
	
	
	/** 邮件最大的bitCount */
	public static final int MAIL_BIT_MAX_COUNT = 63; 
	
	
	/**
	 * 验证当前邮件产生的类型操作类型重要性
	 * @return true.很重要,不能由系统发起删除邮件操作
	 */
	public static boolean isImportantMailType(int mailType) {
		return TYPE_FRIEND_GIFT == mailType || TYPE_BUY_AUCTION_ITEMS == mailType || TYPE_AUCTION_EXPIRE_BACK == mailType
				|| TYPE_SYSTEM_REPATRIATE_AUCTION_ITEM == mailType;
	}
}
