package com.lingyu.common.util;

import java.math.BigDecimal;

/**
 * 
 * @description 类型转换工具
 * 
 * @author LiuJuan
 * @created 2011-12-12 上午09:35:57
 */
public class ConvertObjectUtil {

	/**
	 * 属性值转换(obj -> Integer)
	 * 
	 * @param val
	 * @return
	 */
	public static Integer object2Integer(Object val) {
		//
		if (null != val && !"".equals(val.toString().trim())) {
			String tmp = val.toString();
			if (tmp.equals("true")) {
				return 1;
			} else if (tmp.equals("false")) {
				return 0;
			}

			if (val instanceof Double) {
				return ((Double) val).intValue();
			} else if (val instanceof Integer) {
				return (Integer) val;
			} else if (val instanceof Long) {
				return ((Long) val).intValue();
			} else if (val instanceof Float) {
				return ((Float) val).intValue();
			} else {
				int index = tmp.indexOf(".");
				if (index != -1) {
					tmp = tmp.substring(0, index);
					return Integer.parseInt(tmp);
				} else {
					return Integer.parseInt(tmp);
				}
			}
		}
		return null;
	}

	public static Integer object2int(Object val) {
		if (null != val && !"".equals(val.toString().trim())) {
			String tmp = val.toString();
			if (tmp.equals("true")) {
				return 1;
			} else if (tmp.equals("false")) {
				return 0;
			}

			if (val instanceof Double) {
				return ((Double) val).intValue();
			} else if (val instanceof Integer) {
				return (Integer) val;
			} else if (val instanceof Long) {
				return ((Long) val).intValue();
			} else if (val instanceof Float) {
				return ((Float) val).intValue();
			} else {
				try {
					int index = tmp.indexOf(".");
					if (index != -1) {
						tmp = tmp.substring(0, index);
						return Integer.parseInt(tmp);
					} else {
						return Integer.parseInt(tmp);
					}
				} catch (Exception e) {
					return 0;
				}
			}
		}
		return 0;
	}

	/**
	 * Object转成String,如果为null就返回""
	 * 
	 * @param value
	 * @return String
	 */
	public static String object2String(Object value) {
		if (value == null)
			return "";
		return value.toString();
	}

	/**
	 * Object转换成Str,如果为空就返回Null
	 * 
	 * @param obj
	 * @return
	 */
	public static String obj2StrOrNull(Object obj) {
		if (obj == null)
			return null;
		String str = obj.toString();
		if (str.trim().isEmpty())
			return null;
		return str;
	}

	/**
	 * Object转成boolean,如果为null就返回flase(旧方法，不建议用)
	 * 
	 * @param value
	 * @return boolean
	 */
	public static boolean object2Boolean(Object value) {
		if (value != null && !"".equals(value.toString().trim())) {
			String str = value.toString().trim();
			if (!str.equals("0") && !str.equals("false")) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * Object 转换为 boolean(新方法)
	 * 
	 * @param obj
	 * @return
	 */
	public static boolean object2boolean(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof Boolean) {
			return (Boolean) obj;
		} else if (obj instanceof String) {
			String str = ((String) obj);
			if (str.equals("true") || str.equals("1"))
				return true;
			else
				return false;
		} else if (obj instanceof Integer) {
			Integer it = (Integer) obj;
			if (it == 1)
				return true;
			else
				return false;
		} else if (obj instanceof Long) {
			Long it = (Long) obj;
			if (it == 1)
				return true;
			else
				return false;
		} else if (obj instanceof Float) {
			Float it = (Float) obj;
			if (it.intValue() == 1)
				return true;
			else
				return false;
		} else if (obj instanceof Double) {
			Double it = (Double) obj;
			if (it.intValue() == 1)
				return true;
			else
				return false;
		}
		return false;
	}

	/**
	 * Object转成Float,如果为null就返回0f
	 * 
	 * @param value
	 * @return
	 */
	public static Float object2Float(Object value) {
		if (value != null && !"".equals(value.toString().trim())) {
			String str = value.toString().trim();
			if (!str.equals("")) {
				return Float.parseFloat(str);
			}
		}
		return 0f;
	}

	/**
	 * object转换为float,为空或转换失败，返回0
	 * 
	 * @param obj
	 * @return
	 */
	public static float obj2float(Object obj) {
		if (obj2StrOrNull(obj) == null) {
			return 0;
		}

		if (obj instanceof Float) {
			return (Float) obj;
		} else if (obj instanceof Integer) {
			return ((Integer) obj).floatValue();
		} else if (obj instanceof Short) {
			return ((Short) obj).floatValue();
		} else if (obj instanceof Double) {
			return ((Double) obj).floatValue();
		} else if (obj instanceof Long) {
			return ((Long) obj).floatValue();
		} else {
			String objStr = obj.toString().trim();
			try {
				return Float.parseFloat(objStr);
			} catch (Exception e) {
				return 0;
			}
		}
	}

	/**
	 * 属性值转换(obj -> Long)
	 */
	public static Long object2Long(Object val) {
		if (null != val && !"".equals(val.toString().trim())) {
			String tmp = val.toString();
			if (tmp.equals("true")) {
				return 1l;
			} else if (tmp.equals("false")) {
				return 0l;
			}

			if (val instanceof Double) {
				return ((Double) val).longValue();
			} else if (val instanceof Integer) {
				return ((Integer) val).longValue();
			} else if (val instanceof Long) {
				return (Long) val;
			} else if (val instanceof Float) {
				return ((Float) val).longValue();
			} else {
				int index = tmp.indexOf(".");
				if (index != -1) {
					tmp = tmp.substring(0, index);
					return Long.parseLong(tmp);
				} else {
					return Long.parseLong(tmp);
				}
			}
		}
		return null;
	}

	/**
	 * message处使用的从arg转换成long类型
	 * 
	 * @param arg
	 * @return
	 */
	public static long arg2long(Object arg) {
		if (arg instanceof Long) {
			return (Long) arg;
		} else if (arg instanceof Double) {
			return ((Double) arg).longValue();
		} else if (arg instanceof BigDecimal) {
			return ((BigDecimal) arg).longValue();
		} else if (arg instanceof Integer) {
			return ((Integer) arg).longValue();
		}else if(arg instanceof String){
			return Long.parseLong((String)arg);
		}
		return 0L;
	}

	public static long[] obj2longarray(Object obj){
		if (obj2StrOrNull(obj) == null) {
			return null;
		}
		if(obj instanceof Object[]){
			Object[] ret = (Object[])obj;
			long[] retValue = new long[ret.length];
			int index = 0;
			for(Object objt : ret){
				retValue[index++] = obj2long(objt);
			}
			return retValue;
		}
		return null;
	}
	
	/**
	 * object 转换为long,若为空的时候返回0
	 * 
	 * @param obj
	 * @return
	 */
	public static long obj2long(Object obj) {
		if (obj2StrOrNull(obj) == null) {
			return 0l;
		}

		if (obj instanceof Long) {
			return (Long) obj;
		} else if (obj instanceof Integer) {
			return ((Integer) obj).longValue();
		} else if (obj instanceof Short) {
			return ((Short) obj).longValue();
		} else if (obj instanceof Double) {
			return ((Double) obj).longValue();
		} else if (obj instanceof Float) {
			return ((Float) obj).longValue();
		} else {
			String objStr = obj.toString().trim();
			int index = objStr.indexOf(".");
			try {
				if (index > -1) {
					objStr = objStr.substring(0, index);
				}
				return Long.parseLong(objStr);
			} catch (Exception e) {
				return 0l;
			}
		}

	}

	/**
	 * 是否为空
	 * 
	 * @param val
	 * @return true:为空null 或 ""
	 */
	public static boolean isEmpty(String val) {

		if (null == val || val.trim().isEmpty()) {
			return true;
		}

		return false;
	}

}
