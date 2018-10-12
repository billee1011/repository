package com.cai.http;

public class OssException extends RuntimeException{
	
	
	public int code = -1;
	
	public OssException(String msg){
		super(msg);
	}
	
	public OssException(int code,String msg){
		super(msg);
		this.code = code;
	}
	
	

}
