package com.cai.util;

public class StringTempUtil {

	public static int change34(int t1){
		String s1 = t1+"";
		s1 = s1.replaceAll("3", "a");
		s1 = s1.replaceAll("4", "b");
		s1 = s1.replaceAll("a", "4");
		s1 = s1.replaceAll("b", "3");
		
		int value = Integer.valueOf(s1);
		return value;
	}
	
}
