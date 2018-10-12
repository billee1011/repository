package com.cai.dictionary;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.cai.common.util.CSVUtil;
import com.cai.common.util.PerformanceTimer;
import com.google.common.collect.Maps;

public class DirtyWordDict {

	private Logger logger = LoggerFactory.getLogger(DirtyWordDict.class);

	private Map<String, Object> dirtyWordMap;
	/**
	 * 单例
	 */
	private static DirtyWordDict instance;

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static DirtyWordDict getInstance() {
		if (null == instance) {
			instance = new DirtyWordDict();
		}

		return instance;
	}

	private DirtyWordDict() {
		dirtyWordMap = new HashMap<>();
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			init(CSVUtil.readDirtyCSVFile("./dirty.txt"));
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典SysGameTypeDict" + timer.getStr());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void init(List<String> keyWordSet) {
		String key = null;
		Map nowMap = null;
		Map<String, String> newWorMap = null;
		// 迭代keyWordSet
		Iterator<String> iterator = keyWordSet.iterator();
		while (iterator.hasNext()) {
			key = iterator.next(); // 关键字
			nowMap = dirtyWordMap;
			for (int i = 0; i < key.length(); i++) {
				char keyChar = key.charAt(i); // 转换成char型
				Object wordMap = nowMap.get(keyChar); // 获取

				if (wordMap != null) { // 如果存在该key，直接赋值
					nowMap = (Map) wordMap;
				} else { // 不存在则，则构建一个map，同时将isEnd设置为0，因为他不是最后一个
					newWorMap = new HashMap<String, String>();
					newWorMap.put("isEnd", "0"); // 不是最后一个
					nowMap.put(keyChar, newWorMap);
					nowMap = newWorMap;
				}

				if (i == key.length() - 1) {
					nowMap.put("isEnd", "1"); // 最后一个
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public boolean checkDirtyWord(String txt) {
		boolean flag = false; // 敏感词结束标识位：用于敏感词只有1位的情况
		char word = 0;
		Map nowMap = dirtyWordMap;
		for (int i = 0; i < txt.length(); i++) {
			word = txt.charAt(i);
			nowMap = (Map) nowMap.get(word); // 获取指定key
			if (nowMap == null) { // 存在，则判断是否为最后一个
				nowMap = (Map) dirtyWordMap.get(word);
				if (nowMap == null) {
					nowMap = dirtyWordMap;
					continue;
				}

			}

			if ("1".equals(nowMap.get("isEnd"))) { // 如果为最后一个匹配规则,结束循环，返回匹配标识数
				flag = true; // 结束标志位为true
				break;
			}

		}
		return flag;
	}

	public static void main(String[] args) {
		DirtyWordDict.getInstance().load();
		// String chat =
		// "["{\"content\":\"哈哈哈\",\"accountId\":65647,\"type\":1,\"time\":1511770155,\"name\":\"兜兜優鼞111\",\"url\":\"http://wx.qlogo.cn/mmopen/vi_32/1ornnGqSaTy1jvjNh3EKDlOMvEjapVmibqz7KXnmO0LsxcCMdXibMUDF097B67O4xYFicWBHxiaFiaytITMCbGrr0cA/132\"}","{\"content\":\"反反复复反反复复\",\"accountId\":65647,\"type\":1,\"time\":1511770851,\"name\":\"兜兜優鼞111\",\"url\":\"http://wx.qlogo.cn/mmopen/vi_32/1ornnGqSaTy1jvjNh3EKDlOMvEjapVmibqz7KXnmO0LsxcCMdXibMUDF097B67O4xYFicWBHxiaFiaytITMCbGrr0cA/132\"}","{\"content\":\"的撒高峰时段\",\"accountId\":16226,\"type\":1,\"time\":1511771438,\"name\":\"wsy9911\",\"url\":\"\"}"\]"
		Map<String, Object> chats = Maps.newHashMap();
		chats.put("content", "哈哈哈哈");
		chats.put("accountId", 993454L);
		chats.put("name", "五环彩");
		chats.put("url", "http://wx.qlogo.cn/mmopen/vi_32/37XDk9XKIWiaibuBRI29kR2Ss4wL9GVs2MLbsicPzMVMHoRic49g0CkgRWGyI6jgU5bzE5yuc9bKVjGgLoiaTaXSG4g/132");
		chats.put("type", 1);
		String msg = JSON.toJSON(chats).toString();
		System.out.println(msg);
		System.out.println(DirtyWordDict.getInstance().checkDirtyWord("http://wx.qlogo.cn/mmopen/vi_32/37XDk9XKIWiaibuBRI29kR2Ss4wL9GVs2MLbsicPzMVMHoRic49g0CkgRWGyI6jgU5bzE5yuc9bKVjGgLoiaTaXSG4g/132"));

	}
}
