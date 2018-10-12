package com.cai.util;

import com.cai.common.constant.GameConstants;

public class Test {

	public static void main(String[] args) {

		int t1 = 11;
//		int value = StringTempUtil.change34(t1);
//		
//		System.out.println((115 & GameConstants.LOGIC_MASK_COLOR) >> 4);
//		System.out.println(115 & GameConstants.LOGIC_MASK_VALUE);
//		
//		
//		
//		System.out.println((67 & GameConstants.LOGIC_MASK_COLOR) >> 4);
//		System.out.println(67 & GameConstants.LOGIC_MASK_VALUE);
//		
//		
//		
//		System.out.println(((9 / 3) << 4) | (9 % 3 + 1));
//		
//		System.out.println(((22 / 3) << 4) | (22 % 3 + 1));
//		
//		System.out.println(((4 / 3) << 4) | (4 % 3 + 1));
		
		
		for(int i =0;i<=2;i++) {
			System.out.println("next = "+(i+ 3 + 1)% 3);
		
		}
	
		for(int i =0;i<=3;i++) {
			System.out.println("next = "+(i+ 4 + 1)% 4);
		
		}
		
	}

}
