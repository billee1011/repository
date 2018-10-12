package com.lingyu.common.core;

import org.apache.logging.log4j.message.ParameterizedMessageFactory;

/**
 * 
 */
public class ServiceException extends RuntimeException {

	/**
     *
     */
	private static final long serialVersionUID = -3287463281746412649L;
	//private int errorCode;

	public ServiceException() {
		super();
	}
	public ServiceException(int code) {
		super(String.valueOf(code));
	}
	/**
	 * 
	 * @param message
	 */
	public ServiceException(String message) {
		super(message);
	}
	public ServiceException(String message,Object... params){
		super(ParameterizedMessageFactory.INSTANCE.newMessage(message, params).getFormattedMessage());
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceException(Throwable cause) {
		super(cause);
	}
}
