package com.lingyu.noark.data.exception;

import com.lingyu.noark.data.annotation.Entity;

/**
 * 非法实体类异常.
 * <p>
 * 当实体类没有{@link Entity}注解时会抛出此异常.<br>
 * 当实体类没有可映射的属性时也会抛出此异常.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class NoEntityException extends RuntimeException {
	private static final long serialVersionUID = -6578547540200771950L;

	/**
	 * 构造一个非法实体类异常对象.
	 * 
	 * @param className 实体类全名
	 * @param message 异常产生原因描述
	 */
	public NoEntityException(String className, String message) {
		super("非法的实体类：" + className + "，原因:" + message);
	}
}