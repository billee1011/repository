package com.lingyu.common.rpc;


public interface Callback<T> {
	public void handle(T msg) throws Exception;
}
