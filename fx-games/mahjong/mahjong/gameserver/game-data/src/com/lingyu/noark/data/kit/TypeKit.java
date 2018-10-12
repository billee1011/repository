package com.lingyu.noark.data.kit;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

public class TypeKit {

	public static boolean isInt(Type type) {
		return type == int.class || type == Integer.class;
	}

	public static boolean isString(Type type) {
		return type == String.class;
	}

	public static boolean isLong(Type type) {
		return type == long.class || type == Long.class;
	}

	public static boolean isAtomicInteger(Type type) {
		return type == AtomicInteger.class;
	}
}