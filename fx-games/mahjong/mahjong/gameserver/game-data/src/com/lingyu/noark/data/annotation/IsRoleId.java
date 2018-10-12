package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IsRoleId注解表示此属性是否为游戏中的角色ID字段.
 * <p>
 * 其作用为数据存储层提供哪个属性是角色Id，以便区分数据和访问存储策略<br>
 * 可联合Id或Column一起使用.
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsRoleId {
}
