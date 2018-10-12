package com.cai.common.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 游戏公告
 * @author run
 *
 */
public class GameNoticeModel implements Serializable{

	/**
	 * 公告id
	 */
	private int id;
	
	/**
	 * 内容
	 */
	private String content;
	
	/**
	 * 游戏类型
	 */
	private int game_type;
	
	/**
	 * 播放间隔
	 */
	private int delay;
	
	/**
	 * 创建时间
	 */
	private Date create_time;
	
	/**
	 * 公告结束时间
	 */
	private Date end_time;
	
	/**
	 * 播放类型 1=普通 2=全局
	 */
	private int play_type;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getGame_type() {
		return game_type;
	}

	public void setGame_type(int game_type) {
		this.game_type = game_type;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public Date getCreate_time() {
		return create_time;
	}

	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}

	public Date getEnd_time() {
		return end_time;
	}

	public void setEnd_time(Date end_time) {
		this.end_time = end_time;
	}

	public int getPlay_type() {
		return play_type;
	}

	public void setPlay_type(int play_type) {
		this.play_type = play_type;
	}
	
	
	
	
}
