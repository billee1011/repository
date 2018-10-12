package com.cai.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class BeanUtil {

	public static boolean isEmpty(final String value) {
		return value == null || value.trim().length() == 0
				|| "null".endsWith(value);
	}
	
	/**
	 * 获取指定包名下的所有子目录名称集合set
	 * @param pack
	 * @return
	 * @Added by 500
	 */
	public static Set<String> getPackageDir(String pack) { 
		Set<String> set = new HashSet<String>();
		// 获取包的名字 并进行替换
		String packageName = pack;
		String packageDirName = packageName.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader()
					.getResources(packageDirName);
			// 循环迭代下去
			while (dirs.hasMoreElements()) {
				// 获取下一个元素
				URL url = dirs.nextElement();
				// 得到协议的名称
				String protocol = url.getProtocol();
				// 如果是以文件的形式保存在服务器上
				if ("file".equals(protocol)) {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					File f = new File(filePath);
					if ( f.isDirectory()){
						File[] files = f.listFiles();
						for(File ff:files) {
							if ( ff.isDirectory() ) set.add(ff.getName());
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return set;
	}
	

	public static Object copyPropertie(Object source, Object target) {
		try {
			BeanInfo targetbean = Introspector.getBeanInfo(target.getClass());
			PropertyDescriptor[] propertyDescriptors = targetbean
					.getPropertyDescriptors();
			for (int i = 0; i < propertyDescriptors.length; i++) {
				PropertyDescriptor pro = propertyDescriptors[i];
				Method wm = pro.getWriteMethod();
				if (wm != null) { 
					BeanInfo sourceBean = Introspector.getBeanInfo(source
							.getClass());
					PropertyDescriptor[] sourcepds = sourceBean
							.getPropertyDescriptors();
					for (int j = 0; j < sourcepds.length; j++) {
						if (sourcepds[j].getName().equals(pro.getName())) { 
							Method rm = sourcepds[j].getReadMethod();
							if (!Modifier.isPublic(rm.getDeclaringClass()
									.getModifiers())) {
								rm.setAccessible(true);
							}
							Object value = rm.invoke(source, new Object[0]);
							if (!Modifier.isPublic(wm.getDeclaringClass()
									.getModifiers())) {
								wm.setAccessible(true);
							}
							wm.invoke((Object) target, new Object[] { value });
							break;
						}
					}
				}
			}
		} catch (IntrospectionException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return target;
	}
	
	public static void main(String[] args){
		for(String s:getPackageDir("com.jcwx.game.module") ) {
			System.out.println(s);
		}
	}
}
