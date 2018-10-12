package com.lingyu.admin.privilege;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Privilege
{
	boolean login() default true;
    /** 
     * 功能ID，该功能ID，对应数据库中的功能ID 
     * @return 
     */  
    int value() default 0;  
}
