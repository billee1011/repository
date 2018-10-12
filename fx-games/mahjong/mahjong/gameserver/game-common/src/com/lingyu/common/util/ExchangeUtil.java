package com.lingyu.common.util;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class ExchangeUtil {
	/** 则算成平台币 */
	public static float getPoint(int gamePoint, float exchangeRate) {
		return (float) (gamePoint / exchangeRate);
	}

	public static int getGamePoint(int point, float exchangeRate) {
		return (int) (point * exchangeRate);
	}

	/** 以下用于交易序列号生成 */
	private static AtomicLong serialNum = new AtomicLong(0);
	private final static String PATTERN_SERIAL = "00000000";

	public static String createOrderId(int serverId) {
		String orderId = String.valueOf(serverId) + format(PATTERN_SERIAL, Long.toHexString(serialNum.incrementAndGet()));
		return orderId;
	}
	
	public static String createOrderId() {
		String orderId = Long.toHexString(System.currentTimeMillis() / 1000) + format(PATTERN_SERIAL, Long.toHexString(serialNum.incrementAndGet()));
		return orderId;
	}

	/** 用于商城流水号生成 */
	private static AtomicLong serialNum4Hall = new AtomicLong(0);

	/** 用于商城流水号生成 */
	public static String createOrderId4Shop() {
		String orderId = TimeUtil.format(new Date(), TimeUtil.PATTERN_yyyyMMddHHmmss)
				+ format(PATTERN_SERIAL, Long.toHexString(serialNum4Hall.incrementAndGet()));
		return orderId;
	}

	/** 用于家园商铺流水号生成 */
	private static AtomicLong serialNum4HallHomeShop = new AtomicLong(0);

	/** 用于家园商铺流水号生成 */
	public static String createOrderId4HomeShop() {
		String orderId = TimeUtil.format(new Date(), TimeUtil.PATTERN_yyyyMMddHHmmss)
				+ format(PATTERN_SERIAL, Long.toHexString(serialNum4HallHomeShop.incrementAndGet()));
		return orderId;
	}

	/** 用于珍宝商人流水号生成 */
	private static AtomicLong serialNum4HallTreasureHouse = new AtomicLong(0);

	/** 用于珍宝商人流水号生成 */
	public static String createOrderId4TreasureHouse() {
		String orderId = TimeUtil.format(new Date(), TimeUtil.PATTERN_yyyyMMddHHmmss)
				+ format(PATTERN_SERIAL, Long.toHexString(serialNum4HallTreasureHouse.incrementAndGet()));
		return orderId;
	}

	/** 用于国家商城 流水号生成 */
	private static AtomicLong serialNum4HallCountryShop = new AtomicLong(0);

	/** 用于国家商城 流水号生成 */
	public static String createOrderId4CountryShop() {
		String orderId = TimeUtil.format(new Date(), TimeUtil.PATTERN_yyyyMMddHHmmss)
				+ format(PATTERN_SERIAL, Long.toHexString(serialNum4HallCountryShop.incrementAndGet()));
		return orderId;
	}

	/**
	 * @param pattern -"000000"
	 * @param input - the input String
	 */
	public static String format(String pattern, String input) {
		String str = pattern + input;
		return str.substring(str.length() - pattern.length());
	}

	/**
	 * @param pattern -"000000"
	 * @param input - the input String
	 */
	public static String format(String pattern, int input) {
		String str = pattern + input;
		return str.substring(str.length() - pattern.length());
	}

	public static void main(String[] args) {
		System.out.println(ExchangeUtil.createOrderId4Shop());
	}
}
