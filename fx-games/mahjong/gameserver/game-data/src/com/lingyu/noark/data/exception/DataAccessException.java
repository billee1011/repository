package com.lingyu.noark.data.exception;

/**
 * 数据存储异常.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class DataAccessException extends RuntimeException {

	private static final long serialVersionUID = -5151735581480584254L;

	public DataAccessException(Exception e) {
		super(e);
	}
}
