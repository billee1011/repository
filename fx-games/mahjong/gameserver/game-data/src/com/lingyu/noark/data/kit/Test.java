package com.lingyu.noark.data.kit;

import java.lang.reflect.Field;

import com.esotericsoftware.reflectasm.MethodAccess;

public class Test {

	public static void main(String[] args) {
		Class classes = Person.class;
		MethodAccess methodAccess = MethodAccess.get(classes);
		Field[] fields = classes.getDeclaredFields();
		for (Field field : fields) {
		}
	}
}

class Person {
	private String name;
	private int age;
	public static String call;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getCall() {
		return call;
	}

	public void setCall(String call) {
		this.call = call;
	}

	public static void say() {
		System.out.println("hello word");
	}
}