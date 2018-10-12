package com.lingyu.noark.cache.exception;

/**
 * Guava缓存异常.
 * <p>
 * 主要用来包裹GuavaCache的检查异常，并输出一句什么情况
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class GuavaCacheException extends RuntimeException {
	private static final long serialVersionUID = -4437724877684829381L;

	public GuavaCacheException(String message, Exception e) {
		super(message, e);
	}
}
