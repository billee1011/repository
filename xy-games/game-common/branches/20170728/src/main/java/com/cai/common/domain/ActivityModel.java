package com.cai.common.domain;

import java.io.Serializable;

/**
 * 活动
 *
 */
public class ActivityModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8700833815705971737L;

	private Integer id;

	/**
	 * 游戏ID
	 */
	private int game_id;
	/**
	 * 类型 1=文字 2=图片
	 */
	private int type;
	/**
	 * 内容
	 */
	private String content;

	private String name;
	/**
	 * 链接
	 */
	private String href;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getGame_id() {
		return game_id;
	}

	public void setGame_id(int game_id) {
		this.game_id = game_id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
}
