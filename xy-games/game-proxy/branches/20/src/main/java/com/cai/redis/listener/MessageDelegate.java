package com.cai.redis.listener;

import java.io.Serializable;
import java.util.Map;

public interface MessageDelegate {
//	void handleMessage(String message);
//	void handleMessage(Map message);
//	void handleMessage(Serializable message);
//	void handleMessage(Serializable message, String channel);
	
	
	void handleMessage(byte[] message);
}
