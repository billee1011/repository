package com.lingyu.game.service.config;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.core.ServiceException;
import com.lingyu.common.template.ConfigTemplate;

public class ConstantUtil {
	private static final Logger logger = LogManager.getLogger(ConstantUtil.class);
	public static void build(Map<Integer, ConfigTemplate> map) {
		Set<Integer> set = new HashSet<Integer>();
		for (Field f : ConfigConstant.class.getDeclaredFields()) {
			if (f.isAnnotationPresent(Constant.class)) {
				Constant c = f.getAnnotation(Constant.class);
				int index = c.value();
				if (0 == index) {
					throw new ServiceException("全局配置ID不能为 0");
				} else if (set.contains(index)) {
					throw new ServiceException("全局配置重复：" + index);
				} else if (!map.containsKey(index)) {
					logger.info("没有对应ID的全局配置：" + index);
					continue;
				} else {
					boolean accessAble = f.isAccessible();
					f.setAccessible(true);
					try {
						Class<?> type = f.getType();
						String value = map.get(index).getValue();
						// f.set(f, value);
						if (type == Byte.TYPE || type == Byte.class) {
							f.setByte(ConfigConstant.class, Byte.valueOf(value));
						} else if (type == Short.TYPE || type == Short.class) {
							f.setShort(ConfigConstant.class, Short.valueOf(value));
						} else if (type == Integer.TYPE || type == Integer.class) {
							f.setInt(ConfigConstant.class, Integer.valueOf(value));
						} else if (type == Long.TYPE || type == Long.class) {
							f.setLong(ConfigConstant.class, Long.valueOf(value));
						} else if (type == Float.TYPE || type == Float.class) {
							f.setFloat(ConfigConstant.class, Float.valueOf(value));
						} else if (type == Double.TYPE || type == Double.class) {
							f.setDouble(ConfigConstant.class, Double.valueOf(value));
						} else if (type == Boolean.TYPE || type == Boolean.class) { //赋值boolean 兼容int（1: true  0:false）
							if("1".equals(value)){
								f.setBoolean(ConfigConstant.class, true);
							}else if("0".equals(value)){
								f.setBoolean(ConfigConstant.class, false);
							}else{
								f.setBoolean(ConfigConstant.class, Boolean.valueOf(value));
							}
						} else if (type == String.class) {
							f.set(ConfigConstant.class, String.valueOf(value));
						} else if (type == Date.class) {
							if (value.indexOf("-") < 0) {
								value = "2000-01-01 " + value;
							}
							try {
								SimpleDateFormat dformater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								Date date = dformater.parse(value);
								f.set(ConfigConstant.class, date);
							} catch (ParseException e) {
								throw new ServiceException("全局表日期格式不对：" + value, e);
							}
						} else if (type == List.class) {
							// 只支持Integer数组
							List<Integer> list = new ArrayList<Integer>();
							for (String s : value.split(";")) {
								list.add(Integer.valueOf(s));
							}
							f.set(ConfigConstant.class, list);
						} else {
							throw new UnsupportedDataTypeException("不支持的类型：" + type.getSimpleName());
						}
						f.setAccessible(accessAble);
						set.add(index);
					} catch (Exception e) {
						throw new ServiceException(e);
					}
				}
			}
		}
	}
}
