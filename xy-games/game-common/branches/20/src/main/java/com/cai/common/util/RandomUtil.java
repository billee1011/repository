package com.cai.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.cai.common.domain.ProbModel;



public class RandomUtil {
	
	
	public static Random random = new Random();

	/**
	 * 是否在范围内 如(5/100)则有5%几率返回值为true
	 * 
	 * @param minPercent
	 *            分子
	 * @param maxPercent
	 *            分母
	 * @return 是否在范围内
	 */
	public static boolean isInTheLimits(Integer minPercent, Integer maxPercent) {
		if (random.nextInt(maxPercent) + 1 <= minPercent) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 生成随机数字数组
	 * 
	 * @param maxNum
	 *            最大数字范围(0~maxNum)
	 * @param length
	 *            长度
	 * @return int数组(按照从小到大自动排序)
	 */
	public static int[] generateRandomNumberArray(int maxNum, int length) {
		int[] randomArray = new int[length];
		Set<Integer> set = new TreeSet<Integer>();
		while (true) {
			set.add(random.nextInt(maxNum + 1));
			if (set.size() >= length) {
				break;
			}
		}
		Iterator<Integer> iterator = set.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			randomArray[i] = iterator.next();
			i++;
		}
		return randomArray;
	}
	
	/**
	 * 随机生成数字
	 * @param minNum 最小值
	 * @param maxNum 最大值
	 * @return
	 */
	public static int generateRandomNumber(int minNum, int maxNum){
		int offset = maxNum - minNum +1;
		return random.nextInt(offset) + minNum;
	}

	/**
	 * 生成随机数字数组(大于等于minNum且小于等于maxNum)
	 * 
	 * @param minNum
	 * @param maxNum
	 * @param length
	 * @return int数组(按照从小到大自动排序)
	 */
	public static int[] generateRandomNumberArray(int minNum, int maxNum, int length) {
		int[] randomArray = new int[length];
		Set<Integer> set = new TreeSet<Integer>();
		int offset = maxNum - minNum;
		while (true) {
			set.add(random.nextInt(offset) + minNum + 1);
			if (set.size() >= length) {
				break;
			}
		}
		Iterator<Integer> iterator = set.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			randomArray[i] = iterator.next();
			i++;
		}
		return randomArray;
	}

	/**
	 * 生成随机数字数组(大于等于minNum且小于等于maxNum)
	 * 
	 * @param minNum
	 * @param maxNum
	 * @param length 不能大于10000000
	 * @return int数组(按照从小到大自动排序)
	 */
	public static Integer[] generateNumberArray(int minNum, int maxNum, int length) {
		
		if(length > maxNum-minNum+1 ){
			return null;
		}
		
		Integer[] randomArray = new Integer[length];
		Set<Integer> set = new TreeSet<Integer>();
		int min = minNum < maxNum ? minNum : maxNum;
		int offset = Math.abs(maxNum - minNum)+1;
		int i = 0;
		while (true) {
			int tmp = offset == 0 ? min : random.nextInt(offset) + min;
			if (set.add(tmp)) {
				randomArray[i++] = tmp;
			}
			if (set.size() >= length) {
				break;
			}
			
			if(i>10000000)
				return null;
		}
		return randomArray;
	}

	/**
	 * 在已存在数据中随机选取数字
	 * 
	 * @param existingArray
	 *            已存在数组
	 * @param num
	 *            数量
	 * @return
	 */
	public static int[] generateRandomNumberArrayFromExistingArray(Integer[] existingArray, int num) {
		int[] randomArray = new int[num];
		Set<Integer> set = new TreeSet<Integer>();
		while (true) {
			set.add(existingArray[random.nextInt(existingArray.length)]);
			if (set.size() >= num) {
				break;
			}
		}
		Iterator<Integer> iterator = set.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			randomArray[i] = iterator.next();
			i++;
		}
		return randomArray;
	}

	public static int getRandomNumber(int len) {
		return random.nextInt(len);
	}

	public static boolean isInArray(int[] array, int e) {
		for (int i : array) {
			if (i == e) {
				return true;
			}
		}
		return false;
	}

	public static <T> T getOneRandomElement(Collection<T> coll) {
		int length = coll.size();
		int sel = getRandomNumber(length);
		int i = 0;
		for (T one : coll) {
			if (sel == i)
				return one;
			// sel++;
			i++;
		}
		return null;
	}

	/**
	 * 概率选择
	 * 
	 * @param keyChanceMap
	 *            key为唯一标识，value为该标识的概率，是去掉%的数字
	 * @return 被选中的key。未选中返回null
	 */
	public static Object chanceSelect(Map<Object, Integer> keyChanceMap) {
		if (keyChanceMap == null || keyChanceMap.size() == 0)
			return null;

		Integer sum = 0;
		for (Integer value : keyChanceMap.values()) {
			sum += value;
		}
		// 从1开始
		Integer rand = new Random().nextInt(sum) + 1;

		for (Map.Entry<Object, Integer> entry : keyChanceMap.entrySet()) {
			rand -= entry.getValue();
			// 选中
			if (rand <= 0) {
				return entry.getKey();
			}
		}

		return null;
	}
	
	/**
	 * 权重
	 * @param probList
	 * @return
	 */
	public static <T> ProbModel<T> randomProb(List<ProbModel<T>> probList){
		
		if(probList==null)
			return null;
		if(probList.size()==0)
			return null;
		
		//计算总和
		int count = 0;
		for(ProbModel m : probList){
			count += m.getProb();
		}
		
		//随机数
		int rNum = random.nextInt(count)+1;
		int sum = 0;
		ProbModel locate = probList.get(0);
		for(int i=0;i<probList.size();i++){
			ProbModel m = probList.get(i);
			sum += m.getProb();
			if(rNum<=sum){
				locate = m;
				break;
			}
		}		
		return locate;
	}
	
	/**
	 * 移除重新random
	 * @param list
	 * @param len
	 * @return
	 */
	public static <T> List<T> randomFixedLength(List<T> list,int len) {
		if (list == null || len > list.size()) {
			throw new UnsupportedOperationException("len is too low");
		}
		List<T> targetList = new ArrayList<T>();
		for ( ; ;) {
			int random_index = RandomUtil.getRandomNumber(list.size());
			targetList.add(list.remove(random_index));
			len --;
			if (len <= 0) {
				break;
			}
		}
		return targetList;
	}
	

	/**
	 * 随机生成字符串
	 * 
	 * @param length
	 * @return
	 */
	public static String getRandomString(int length) {
		StringBuffer buffer = new StringBuffer("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
		StringBuffer sb = new StringBuffer();
		Random r = new Random();
		int range = buffer.length();
		for (int i = 0; i < length; i++) {
			sb.append(buffer.charAt(r.nextInt(range)));
		}
		return sb.toString();
	}
	
	public static int getRandomNumBetweenProb(int start,int end){
		int random = start + new Random().nextInt(end - start +1);
		return random;
	}
	
	
	/**
	 * 随机一个ip
	 * @return
	 */
	public static String randomIp(){
		String[] a = {"183.57.249.136","218.76.166.102","183.28.170.150","119.133.83.87","175.4.128.200","220.202.153.9","113.80.98.149",
				"113.222.237.169","117.136.88.108","218.18.38.247","113.222.221.134","14.215.43.155","113.222.236.163","121.14.142.131",
				"113.113.121.115","110.52.33.128","175.4.134.117","113.222.66.77","14.215.41.229","113.222.131.77"};
		
		int index = getRandomNumber(a.length);
		return a[index];
	}

	public static void main(String[] args) {
	
		for(int i=0;i<100;i++){
			
			System.out.println(randomIp());
		}

	}

}
