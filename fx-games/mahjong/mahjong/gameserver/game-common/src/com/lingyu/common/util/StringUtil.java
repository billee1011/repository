package com.lingyu.common.util;

import org.apache.commons.lang3.StringUtils;

public class StringUtil {
	
	public static boolean isEmpty(String... strs) {
		for (String str : strs) {
			if (StringUtils.isEmpty(str)) {
				return true;
			}
		}
		return false;
	}
}
