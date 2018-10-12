package com.lingyu.common.util;
/**
 * 按位操作的工具类
 * @author WangShuguang
 */
public class BitUtils {
	/**
	 * 设置第n位为1
	 * @param value
	 * @param n
	 * @return
	 */
	public static int setIntN(int value, int n){
		return (value | (1 << n));
	}
	/**
	 * 设置第n位为0
	 * @param value
	 * @param n
	 * @return
	 */
	public static int setIntNotN(int value, int n){
		return (value & ~(1 << n));
	}
	/**
	 * 检查value的第n为为1否
	 * @param value
	 * @param n
	 * @return
	 */
	public static boolean checkIntN(int value, int n){
		return ((value & (1 << n)) != 0);
	}
	
	/**
	 * 设置第n位为1
	 * @param value
	 * @param n
	 * @return
	 */
	public static long setLongN(long value, int n){
		return (value | (1L << n));
	}
	/**
	 * 设置第n位为0
	 * @param value
	 * @param n
	 * @return
	 */
	public static long setLongNotN(long value, int n){
		return (value & ~(1L << n));
	}
	/**
	 * 检查value的第n为为1否
	 * @param value
	 * @param n
	 * @return
	 */
	public static boolean checkLongN(long value, int n){
		return ((value & (1L << n)) != 0);
	}
	/**
	 * 遍历bit 回调callback int版本
	 * @param value
	 * @param callback
	 */
	public static void forEachBit(int value, BitCheckCallback callback){
		if(value >= 0){
			int bitCount = -1;
			for(int i = 1; i <= value; i = (i << 1)){
				bitCount++;
				if(((value & i) != 0) && !callback.callback(bitCount)){
					break;
				}
			}
		}else{ //最高位为 1
			for(int i = 0; i < 32; i++){
				if(checkIntN(value, i) && !callback.callback(i)){
					break;
				}
			}
		}
	}
	/**
	 * 遍历bit 回调callback long版本
	 * @param value
	 * @param callback
	 */
	public static void forEachBit(long value, BitCheckCallback callback){
		if(value >= 0){
			int bitCount = -1;
			for(long i = 1; i <= value; i = (i << 1)){
				bitCount++;
				if(((value & i) != 0) && !callback.callback(bitCount)){
					break;
				}
			}
		}else{ //最高位为 1
			for(int i = 0; i < 64; i++){
				if(checkLongN(value, i) && !callback.callback(i)){
					break;
				}
			}
		}
	}
	
	public static int toBitValue(Integer[] bitArray){
		int ret = 0;
		for(Integer bit : bitArray){
			ret = setIntN(ret, bit);
		}
		return ret;
	} 
	
	public static interface BitCheckCallback{
		public boolean callback(int bitN);
	}
	
	
	public static void main(String[] args) {
		int value = 11;
		System.out.println(BitUtils.setIntN(value, 1));
		System.out.println(BitUtils.setIntNotN(value, 2));
		System.out.println(BitUtils.checkIntN(value, 1));
		System.out.println(Integer.highestOneBit(value));
		System.out.println(Integer.lowestOneBit(value));
		
		forEachBit(value, new BitCheckCallback() {
			
			@Override
			public boolean callback(int bitN) {
				System.out.println(bitN);
				return true;
			}
		});
	}
}
