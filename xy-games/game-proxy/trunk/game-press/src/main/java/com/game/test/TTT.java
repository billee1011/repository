/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class param<T1, T2> {

	class A {
	}

	class B extends A {
	}

	private Class<T1> entityClass;

	public param() {
		Type type = getClass().getGenericSuperclass();
		System.out.println("getClass() == " + getClass());
		System.out.println("type = " + type);
		Type trueType = ((ParameterizedType) type).getActualTypeArguments()[0];
		System.out.println("trueType1 = " + trueType);
		trueType = ((ParameterizedType) type).getActualTypeArguments()[1];
		System.out.println("trueType2 = " + trueType);
		this.entityClass = (Class<T1>) trueType;
		System.out.println("entityClass = " + entityClass);

		B t = new B();
		type = t.getClass().getGenericSuperclass();

		System.out.println("B is A's super class :" + ((ParameterizedType) type).getActualTypeArguments().length);
	}

}

public class TTT extends param<MyClass, MyInvoke> {
	public static void main(String[] args) {
		TTT classDemo = new TTT();
	}
}

class MyClass {

}

class MyInvoke {

}