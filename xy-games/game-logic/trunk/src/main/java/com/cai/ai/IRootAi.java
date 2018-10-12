package com.cai.ai;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IRootAi {  
	
	/**
	 * 游戏类型  1麻将，2扑克，3字牌
	 * @return
	 */
	int gameType() default -1;
	
	/**
	 * 游戏Id 默认为0 如果指定游戏 这个ai只能被某个游戏使用
	 * @return
	 */
	int[] gameIds();
	
	/**
	 * 排除的游戏Id 默认为0 如果指定游戏 这个ai不能被某个游戏使用
	 * @return
	 */
	int[] exceptGameIds() default 0;
	
	/**
	 * ai描述
	 * @return
	 */
	String desc();
	
	
	int[] msgIds();
	
	/**
	 * 房间类型
	 * 0:通用, 4:比赛场, 5:自建赛 , 7:金币场
	 */
	int roomType() default 0;
}
