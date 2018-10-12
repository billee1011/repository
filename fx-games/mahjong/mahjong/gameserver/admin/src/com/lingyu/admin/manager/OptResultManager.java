package com.lingyu.admin.manager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Service
public class OptResultManager {
	public static String WRAP_LINE = "<br/>";
	
	private AtomicInteger serialId = new AtomicInteger(0);
	/** 补偿结果 */
	private Cache<Integer, StringBuilder> resultCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
	
	public int incrementAndGet(){
		int id = serialId.incrementAndGet();
		StringBuilder sb = new StringBuilder(256);
		resultCache.put(id, sb);
		return id;
	}
	
	public StringBuilder getRedeemResult(Integer key) {
		return resultCache.getIfPresent(key);
	}

	public void append(Integer key, String value){
		StringBuilder sb = getRedeemResult(key);
		if(sb != null){
			sb.append(value + WRAP_LINE);
		}
	}
}
