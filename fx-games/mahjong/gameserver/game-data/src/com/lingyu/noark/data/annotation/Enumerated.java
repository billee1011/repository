package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enumerated注解用来标注枚举类型的属性.
 * <p>
 * 可联合Column一起使用.
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Enumerated {

	/**
	 * 入库时有两种模式，一是以下标，二是以字符串，默认为下标
	 */
	EnumType value() default EnumType.ORDINAL;

	public enum EnumType {
		/**
		 * 下标形式
		 */
		ORDINAL,
		/**
		 * 字符串形式
		 */
		STRING
	}
}
