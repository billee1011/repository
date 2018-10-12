package com.cai.test.xwy;

import java.util.Map;

import com.google.common.collect.Maps;

public final class MapTest {

	public static void main(String[] args) {
		Map<Integer, String> xxx = Maps.newHashMap();
		Map<Integer, Object> yyy = Maps.newConcurrentMap();
		System.out.println(xxx instanceof Map);
		System.out.println(yyy instanceof Map);
	}

}
