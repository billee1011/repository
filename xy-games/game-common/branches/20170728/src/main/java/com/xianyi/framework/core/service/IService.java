/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.core.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author wu_hc
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IService {
	/**
	 * 序号
	 * 
	 * @return
	 */
	int order();

	/**
	 * 
	 */
	String name() default "";

	/**
	 * 
	 * @return
	 */
	String desc() default "";
}
