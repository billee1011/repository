/**
 * 
 */
package com.cai.test.xwy;

import java.util.Date;

/**
 * @author xwy
 *
 */
public class Test {
	public static void main(String[] args) {
//		BigDecimal b = new BigDecimal(10.222 - 1.23).setScale(2, BigDecimal.ROUND_HALF_UP);
//		double change =b.doubleValue();
//		
//		BigDecimal b2 = new BigDecimal(10.220).setScale(2, BigDecimal.ROUND_HALF_UP);
//		double newValue = b2.doubleValue()*10;
//
//		// 保留2位小数
//	
//		System.out.println("change" + change);
//
//		System.out.println("newValue" + newValue);
//		
		Date date = new Date(1521388800000L);
		
		Date endDate = new Date(1521475200000L);
		
		long day = ((1521475200000L-1521388800000L)/1000/24/60/60L);
		System.out.println("pass=="+day);
		
		System.out.println(date);
		System.out.println(endDate);
		
	}
}
