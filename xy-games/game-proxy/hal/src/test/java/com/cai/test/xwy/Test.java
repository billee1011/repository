/**
 * 
 */
package com.cai.test.xwy;

import java.math.BigDecimal;

/**
 * @author xwy
 *
 */
public class Test {
	public static void main(String[] args) {
		BigDecimal b = new BigDecimal(10.222 - 1.23).setScale(2, BigDecimal.ROUND_HALF_UP);
		double change =b.doubleValue();
		
		BigDecimal b2 = new BigDecimal(10.220).setScale(2, BigDecimal.ROUND_HALF_UP);
		double newValue = b2.doubleValue()*10;

		// 保留2位小数
	
		System.out.println("change" + change);

		System.out.println("newValue" + newValue);
	}
}
