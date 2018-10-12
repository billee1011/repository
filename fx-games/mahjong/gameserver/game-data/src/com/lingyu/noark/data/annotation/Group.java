package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GroupBy注解标识以此属性为分组Id切割游戏数据.
 * <p>
 * 常用于公会成员实体类的公会Id属性上，可联合Column一起使用.
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Group {
	/**
	 * 目标实体类.
	 * 
	 * @return 返回目标实体类的Class
	 */
	// Class<?> value() default null;
}
