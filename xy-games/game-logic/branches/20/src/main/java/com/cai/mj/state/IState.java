package com.cai.mj.state;

/** 
 * 麻将的状态机接口 
 */  
public interface IState<T> {  
	boolean on_enter(T owner);
	boolean on_exe(T owner);
	boolean on_exit(T owner);
	boolean on_message(T owner,long account_id,com.google.protobuf.GeneratedMessage msg);
	boolean on_global(T owner,long account_id,com.google.protobuf.GeneratedMessage msg);
}  