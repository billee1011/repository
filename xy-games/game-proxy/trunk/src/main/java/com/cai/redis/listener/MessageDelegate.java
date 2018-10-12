package com.cai.redis.listener;

public interface MessageDelegate {
//	void handleMessage(String message);
//	void handleMessage(Map message);
//	void handleMessage(Serializable message);
//	void handleMessage(Serializable message, String channel);
	
	
	void handleMessage(byte[] message);
}
