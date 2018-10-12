/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.test.xwy;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 
 *
 * @author XXY date: 2017年7月29日 下午11:10:40 <br/>
 */
public final class InputTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.println("enter:");
			String cmd = sc.nextLine();

			System.out.println(cmd);
		}*/
	
		while (true) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("enter:");
				String cmd = br.readLine();
				System.out.println(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	
	
	
}
