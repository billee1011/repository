package com.lingyu.common.util;

public class AngleUtil {

	/**
	 * 求一个角度的正弦值 eg:sin(30) = 0.5
	 * 
	 * @param angle
	 * @return
	 */
	public static float sin(float angle) {
		return (float) Math.sin(Math.PI / 180 * angle);
	}

	/**
	 * 求一个正弦值的角度 eg:asin(0.5) = 30
	 * 
	 * @param angle
	 * @return
	 */
	public static float asin(float sinVal) {
		return (float) (Math.asin(sinVal) * 180 / Math.PI);
	}

	/**
	 * 求一个角度的正切值
	 * 
	 * @param angle
	 * @return
	 */
	public static float tan(float angle) {
		return (float) Math.tan(Math.PI / 180 * angle);
	}

	/**
	 * 求一个正切值的角度
	 * 
	 * @param angle
	 * @return
	 */
	public static float atan(float angleVal) {
		float angle = (float) (Math.atan(angleVal) * 180 / Math.PI);
		return angle > 0 ? angle : angle + 180;
	}

	public static void main(String[] args) {
		System.out.println(tan(135f));
		System.out.println(tan(45));
		System.out.println(atan(-1));
	}
}
