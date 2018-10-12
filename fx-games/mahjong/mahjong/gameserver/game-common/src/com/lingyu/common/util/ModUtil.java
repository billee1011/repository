package com.lingyu.common.util;

/**
 * @description 两数相除求余数和整除数
 * @author ShiJie Chi
 * @date 2013-2-25 下午3:49:40
 */
public class ModUtil {

	/**
	 * 求余
	 * 
	 * @param
	 * @return {@link ModResult}
	 */
	public static ModResult mod(long value, long mod) {

		ModResult result = new ModResult();

		if (mod > 0) {

			long modedCount = value / mod;
			long rest = value % mod;

			result.setModedCount(modedCount);
			result.setRest(rest);
		} else {
			result.setModedCount(0);
			result.setRest(value);
		}

		return result;

	}

	public static class ModResult {

		/**
		 * 被模整除次数
		 */
		private long modedCount;

		/**
		 * 余数
		 */
		private long rest;

		public long getModedCount() {
			return modedCount;
		}

		public void setModedCount(long modedCount) {
			this.modedCount = modedCount;
		}

		public long getRest() {
			return rest;
		}

		public void setRest(long rest) {
			this.rest = rest;
		}

	}

}
