package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Column注解标记表示所持久化属性所映射表中的字段.
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	/**
	 * 属性对应数据库中列的名称.
	 */
	String name() default "";

	/**
	 * 属性表示该字段是否为唯一标识，默认为false。
	 */
	boolean unique() default false;

	/**
	 * nullable属性表示该字段是否可以为null值，默认为true。
	 */
	boolean nullable() default true;

	/**
	 * length属性表示字段的长度，当字段的类型为varchar时，该属性才有效，默认为255个字符。
	 */
	int length() default 255;

	/**
	 * precision属性和scale属性表示精度，当字段类型为double时，precision表示数值的总长度，scale表示小数点所占的位数。
	 */
	int precision() default 15;

	/**
	 * @see Column#precision
	 */
	int scale() default 5;

	/**
	 * comment表示注释，仅在创建表时起作用.
	 */
	String comment() default "";

	/**
	 * 建表时的默认值。
	 * 
	 * @return 如果有此属性，则在生成建表语句时添加此默认值
	 */
	String defaultValue() default "";

	/**
	 * 日志输出属性排序功能，对DB无效.
	 * 
	 * @return 主要用于日志输出
	 */
	int order() default 0;
}
