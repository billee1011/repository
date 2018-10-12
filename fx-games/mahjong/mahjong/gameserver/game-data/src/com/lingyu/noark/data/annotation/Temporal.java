package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Temporal注解用来标注日期类型的属性.
 * <p>
 * 可配合Column一起使用.<br>
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Temporal {

	/**
	 * 对应数据库中存储格式.
	 * <p>
	 * 有三种模式，DATE、TIME或TIMESTAMP，默认为TIMESTAMP
	 */
	TemporalType value() default TemporalType.TIMESTAMP;

	public enum TemporalType {
		/**
		 * 日期 格式：yyyy-MM-dd
		 */
		DATE,
		/**
		 * 时间 格式：HH:mm:ss
		 */
		TIME,
		/**
		 * 日期和时间 格式：yyyy-MM-dd HH:mm:ss
		 */
		TIMESTAMP
	}
}