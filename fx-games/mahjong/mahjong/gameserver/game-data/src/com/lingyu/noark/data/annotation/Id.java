package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Id标记表示此属性为表的主键字段.
 * <p>
 * 其他配置可联合Column一起使用.<br>
 * 注意：当有Id注解时Column的unique和nullable两个属性失效
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {

}
