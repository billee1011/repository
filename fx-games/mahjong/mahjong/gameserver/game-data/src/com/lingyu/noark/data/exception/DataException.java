package com.lingyu.noark.data.exception;

/**
 * 数据异常类.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class DataException extends RuntimeException {

	private static final long serialVersionUID = 3959550765618198463L;

	public DataException(String message) {
		super(message);
	}

	public DataException(String message, Throwable e) {
		super(message, e);
	}
}
