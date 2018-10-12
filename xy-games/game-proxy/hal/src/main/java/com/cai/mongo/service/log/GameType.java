package com.cai.mongo.service.log;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GameType {

	PHZ(0, "phz", "跑胡子"), DDZ(1, "ddz", "斗地主"), CSMJ(2, "csmj", "长沙麻将"), PDK(3, "pdk", "跑得快");

	private int id;
	private String engName;
	private String name; // 中文注释

	private GameType(int id, String engName, String name) {
		this.id = id;
		this.engName = engName;
		this.name = name;
	}

	private static final Logger log = LoggerFactory.getLogger(GameType.class);

	public int getId() {
		return id;
	}

	public String getEngName() {
		return engName;
	}

	public String getName() {
		return name;
	}

	/**
	 * ID--》中文注释
	 */
	private static final Map<Integer, String> idTableMap = new HashMap<Integer, String>();

	/**
	 * ID--》英文
	 */
	private static final Map<Integer, String> idEngMap = new HashMap<Integer, String>();

	/**
	 * 中文名--》ID
	 */
	private static final Map<String, Integer> nameToIDMap = new HashMap<String, Integer>();

	static {
		for (GameType e : GameType.values()) {
			if (idTableMap.get(e.getId()) != null) {
				log.error("LogType重复的类型" + e.getId());
				System.exit(-1);
			}
			if (nameToIDMap.get(e.getName()) != null) {
				log.error("LogType重复的类型" + e.getName());
				System.exit(-1);
			}
			idTableMap.put(e.getId(), e.getName());
			nameToIDMap.put(e.getName(), e.getId());
			idEngMap.put(e.getId(), e.getEngName());
		}
	}

	/**
	 * 中文名--》ID
	 */
	public static Map<String, Integer> getNameToIDMap() {
		return nameToIDMap;
	}

	public static String getNameByID(int id) {
		return idTableMap.get(id);
	}

}
