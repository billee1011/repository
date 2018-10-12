package com.cai.dictionary;

/**
 * 比赛场字典
 * 
 * @author run
 *
 */
public class MatchDict {

	/**
	 * 单例
	 */
	private static MatchDict instance;

	/**
	 * 私有构造
	 */
	private MatchDict() {
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static MatchDict getInstance() {
		if (null == instance) {
			instance = new MatchDict();
		}
		return instance;
	}

	public void load() {
	}
}
