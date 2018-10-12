package com.lingyu.admin.privilege;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 资源
 * 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resource
{
	String name() default "";
	ResourceType type() default ResourceType.MENU;
	int module()  default 0;
}
