/**
 * 
 */
package com.cai.common.util;

/**
 * @author xwy
 *
 */
public class ThreadUtil {
	/**
	 * 获取当前线程的堆栈信息
	 * 
	 * @return
	 */
	public static String getStack() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StringBuffer buf = new StringBuffer();
		for (StackTraceElement item : stackTrace) {
			buf.append(item.getClassName()).append(".");
			buf.append(item.getMethodName()).append("()#");
			buf.append(item.getLineNumber()).append("\n");
		}
		return buf.toString();
	}
}
