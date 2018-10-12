package com.lingyu.common.message;

public interface IRunnable extends Runnable {
	public abstract int getCommand();
	
	public abstract long getRoleId();
}
