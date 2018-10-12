package com.lingyu.game.service.config;

public class ConfigConstant {
	/** 房间人数 */
	@Constant(1)
	public static int ROOM_NUM;
	
	/** 消耗房卡数量 */
	@Constant(2)
	public static String COST_CARS;
	
	/** 解散房间超过多少分钟默认全部同意 */
	@Constant(3)
	public static int DISMISS_MINUTES = 5;
	
	/** 初始化房间的总数量 */
	@Constant(4)
	public static int ROOM_NUM_COUNT = 20000;
	
	/** 获取战绩几天前的数据，包含当天 */
	@Constant(5)
	public static int ZHANJI_BEFORE_DAYS = 3;
	
}
