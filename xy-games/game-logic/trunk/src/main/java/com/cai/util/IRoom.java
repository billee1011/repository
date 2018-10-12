/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cai.common.define.EGameType;

/**
 * 用于注解请求处理器
 * 
 * @author wu_hc
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IRoom {

	/**
	 * 协议号
	 * 
	 * @return
	 */
	EGameType gameType();

	/**
	 * 
	 * @return
	 */
	String desc() default "";

}
