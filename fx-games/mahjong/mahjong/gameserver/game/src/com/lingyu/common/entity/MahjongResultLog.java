package com.lingyu.common.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.id.ServerObject;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Json;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;

/**
 * 麻将对战战绩日志表
 * @author wangning
 * @date 2017年2月12日 下午2:19:11
 */
@Entity
@Table(name = "mahjong_result_log")
public class MahjongResultLog extends ServerObject{
	
	@Column(name = "room_num", nullable = false, defaultValue = "0", comment = "房间号")
	private int roomNum;
	
	@Json
	@Column(name = "all_info", length = 65535, defaultValue = "{}", comment = "对战数据记录")
	private Map<Integer, MahjongResultData> allInfo = new HashMap<>();
	
	@Temporal
	@Column(name = "add_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "创建时间")
	private Date addTime = TimeConstant.DATE_LONG_AGO;

	@Temporal
	@Column(name = "modify_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "修改时间")
	private Date modifyTime = TimeConstant.DATE_LONG_AGO;

	public int getRoomNum() {
		return roomNum;
	}

	public void setRoomNum(int roomNum) {
		this.roomNum = roomNum;
	}

	public Map<Integer, MahjongResultData> getAllInfo() {
		return allInfo;
	}

	public void setAllInfo(Map<Integer, MahjongResultData> allInfo) {
		this.allInfo = allInfo;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public Date getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}
}