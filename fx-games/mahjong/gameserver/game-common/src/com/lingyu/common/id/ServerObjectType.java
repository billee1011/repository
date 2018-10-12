package com.lingyu.common.id;

/**
 * 最大值为15 除了普通类型外其他的都是场景中需要持久化的战斗单位需要特别申明类型
 * 
 * @author Allen Jiang
 */
public class ServerObjectType {
	/** 通用的,大部分对象都可以用于这个 */
	public final static byte COMMON = 0;
	/** 角色 */
	public final static byte ROLE = 1;
	/** 宠物 */
	public final static byte PET = 2;
	/** 坐骑 */
	public final static byte HORSE = 3;
	/** 道具 */
	public final static byte ITEM = 4;
	/** 投保日志 */
	public final static byte INSURE = 5;
	/** 此类型为匹配场景ID */
	public final static byte MATCH_STAGE = 6;
	/** 变身 */
	public final static byte SUPERMAN = 7;
}