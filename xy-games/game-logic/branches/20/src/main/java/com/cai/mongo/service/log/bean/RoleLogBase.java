package com.cai.mongo.service.log.bean;

import java.io.Serializable;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;

import com.cai.mongo.service.log.GameType;
import com.cai.mongo.service.log.LogType;

@Document(collection="logBase")
@CompoundIndexes({
    @CompoundIndex(name = "log_idx", def = "{'roleId': 0, 'gameType': 1}")
})
public class RoleLogBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3521707659527761393L;
	@Indexed
	private long roleId; // 角色id
	private String roleName; // 角色名
	private int msgCode; // 消息号
	@Indexed
	public LogType logType;// 日志类型 --后面改为final
	@Indexed
	public GameType gameType;// 游戏类型 --后面改为final

	private int beforeNum; // 变化前数值
	private int changeNum; // 受影响的数值(负数是减少，正数是添加)
	private int afterNum; // 变化后数值

	private int version; // 日志版本号
	private String content; // 操作日志内容，格式自定

	private long time; // 日志时间

	public RoleLogBase() {
	}

	public RoleLogBase(long roleId, GameType gameType, int msgCode, LogType logType, String content) {
		this.roleId = roleId;
		this.msgCode = msgCode;
		this.logType = logType;
		this.content = content;
		this.gameType = gameType;
	}

	public RoleLogBase(long roleId, GameType gameType, int msgCode, LogType logType, int beforeNum, int changeNum,
			int afterNum) {
		this.roleId = roleId;
		this.msgCode = msgCode;
		this.logType = logType;
		this.gameType = gameType;
	}

	public RoleLogBase setTime(long time) {
		this.time = time;
		return this;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public int getMsgCode() {
		return msgCode;
	}

	public void setMsgCode(int msgCode) {
		this.msgCode = msgCode;
	}

	public long getTime() {
		return time;
	}

	public int getBeforeNum() {
		return beforeNum;
	}

	public void setBeforeNum(int beforeNum) {
		this.beforeNum = beforeNum;
	}

	public int getChangeNum() {
		return changeNum;
	}

	public void setChangeNum(int changeNum) {
		this.changeNum = changeNum;
	}

	public int getAfterNum() {
		return afterNum;
	}

	public void setAfterNum(int afterNum) {
		this.afterNum = afterNum;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
