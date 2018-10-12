package com.lingyu.noark.data.kit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReflectKit {

	/**
	 * 利用反射，扫描出此类所有属性(包含父类中子类没有重写的属性)
	 * 
	 * @param klass 指定类.
	 * @param annotations 标识属性的注解
	 * @return 返回此类所有属性.
	 */
	public static Field[] scanAllField(final Class<?> klass, List<Class<?>> annotations) {
		// 为了返回是有序的添加过程，这里使用LinkedHashMap
		Map<String, Field> fieldMap = new LinkedHashMap<String, Field>();
		scanField(klass, fieldMap, annotations);
		return fieldMap.values().toArray(new Field[fieldMap.size()]);
	}

	/**
	 * 递归的方式拉取属性，这样父类的属性就在上面了...
	 * 
	 * @param klass 类
	 * @param fieldMap 所有属性集合
	 * @param annotations 标识属性的注解
	 */
	private static void scanField(final Class<?> klass, Map<String, Field> fieldMap, List<Class<?>> annotations) {
		Class<?> superClass = klass.getSuperclass();
		if (!Object.class.equals(superClass)) {
			scanField(superClass, fieldMap, annotations);
		}
		// 属性判定
		for (Field f : klass.getDeclaredFields()) {
			// Static和Final的不要
			if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
				continue;
			}
			// 子类已重写或内部类中的不要
			if (fieldMap.containsKey(f.getName()) || f.getName().startsWith("this$")) {
				continue;
			}
			// 没有指定的注解不要
			for (Annotation a : f.getAnnotations()) {
				if (annotations.contains(a.annotationType())) {
					fieldMap.put(f.getName(), f);
					break;
				}
			}
		}
	}
}