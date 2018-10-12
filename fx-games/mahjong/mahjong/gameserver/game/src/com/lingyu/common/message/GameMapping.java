package com.lingyu.common.message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GameMapping {
	int value() default 0;
	boolean relay() default true;//是否转发
	boolean print() default true;//是否要打印
}
