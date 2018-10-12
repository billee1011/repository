package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Readonly注解用来标注不需要更新的属性.
 * <p>
 * 可配合Column一起使用. <br>
 * Readonly只会在Inster语句时插入一次，Update语句中会忽略此属性 <br>
 * 常用于addTime属性上,在不了解此注解功能时，请不要使用这个功能
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Readonly {

}
