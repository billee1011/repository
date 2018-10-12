package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Json注解用来标注需要把对象转成Json格式的属性.
 * <p>
 * 可配合Column一起使用. <br>
 * Json转化所用的Jar包为FastJson
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Json {
	/**
	 * 是否写入类名.
	 * <p>
	 * 默认不写入 <br>
	 * <b>如果需要写入类名，注意重构时包目录和类名</b>
	 * 
	 * @return 如果需要写入类名返回true,否则返回false.
	 */
	JsonStyle style() default JsonStyle.DefaultStyle;

	public enum JsonStyle {
		/**
		 * 这个没有任何意义，只是个默认值.
		 */
		DefaultStyle,
		/**
		 * 写入Class类名
		 */
		WriteClassName;
	}
}