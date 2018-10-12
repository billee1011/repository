package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明的一个Java类所对应的数据表名。
 * <p>
 * 前提是此类必需有@Entity注解
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
	/**
	 * name属性表示实体所对应表的名称，默认表名为实体的名称。
	 */
	String name() default "";

	/**
	 * comment表示注释，仅在创建表时起作用.
	 */
	String comment() default "";

	/**
	 * engine表示表结构的默认存储引擎，仅在创建表时起作用.
	 */
	TableEngine engine() default TableEngine.InnoDB;

	public static enum TableEngine {
		InnoDB, MyISAM
	}
}