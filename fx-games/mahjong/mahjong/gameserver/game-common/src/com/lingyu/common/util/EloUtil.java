package com.lingyu.common.util;

//K取值范围：
//R<2000，K=32
//2000<R<2400，K=130-R/20    ... -0.06R+152
//R>2400，K=8
public class EloUtil {
	public static int K = 32;

	public static void main(String[] args) {
		double a = 1200;
		double b = 1200;
		System.out.println(getRate(a, b));
		System.out.println(getDelta(false, a, b));
	}

	// A对B的期望胜率： P(D)=1/[1+10^(-D/400)]（^表示次方）
	public static double getRate(double a, double b) {
		return 1d / (1 + Math.pow(10, (b - a) / 400));
	}

	// 得分 a的胜利率
	public static double getDelta(boolean flag, double a, double b) {
		return flag ? (1 - getRate(a, b)) * K : (0 - getRate(a, b)) * K;
	}
}