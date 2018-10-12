package com.cai.util;

import com.cai.common.constant.GameConstants;

public class Test {
	
	public static void main(String[] args) {
		
		int t1 = 11;
		int value = StringTempUtil.change34(t1);
		
		System.out.println((49 & GameConstants.LOGIC_MASK_COLOR) >> 4);
		System.out.println(49 & GameConstants.LOGIC_MASK_VALUE);
		
		
		
		System.out.println((114 & GameConstants.LOGIC_MASK_COLOR) >> 4);
		System.out.println(114 & GameConstants.LOGIC_MASK_VALUE);
		
		
		
		System.out.println(((9 / 3) << 4) | (9 % 3 + 1));
		
		System.out.println(((22 / 3) << 4) | (22 % 3 + 1));
		
		System.out.println(((4 / 3) << 4) | (4 % 3 + 1));
		
	}

}
