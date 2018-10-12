package com.lingyu.admin.network;

import com.alibaba.fastjson.JSON;
import com.lingyu.msg.http.GetServerInfo_C2S_Msg;
import com.lingyu.msg.http.GetServerInfo_S2C_Msg;
import com.lingyu.msg.http.Redeem_C2S_Msg;
import com.lingyu.msg.http.Redeem_S2C_Msg;
import com.lingyu.msg.http.VersionNotic_C2S_Msg;
import com.lingyu.msg.http.VersionNotic_S2C_Msg;

public class GameClient extends HttpClient {
	/**
	 * @param timeout
	 *            超时 单位:毫秒
	 */
	public GameClient(String ip, int port, int timeout) {
		super(ip, port, timeout);
	}
	
	public Redeem_S2C_Msg redeem(Redeem_C2S_Msg msg) {
		Redeem_S2C_Msg ret = null;
		String content = this.sendWithReturn(msg);
		if (content != null) {
			ret = JSON.parseObject(content, Redeem_S2C_Msg.class);
		}
		return ret;
	}
	
	/**
	 * 更新公告
	 * @param msg
	 * @return
	 */
	public VersionNotic_S2C_Msg versionNotic(VersionNotic_C2S_Msg msg){
		VersionNotic_S2C_Msg ret = null;
		String content = this.sendWithReturn(msg);
		if (content != null) {
			ret = JSON.parseObject(content, VersionNotic_S2C_Msg.class);
		}
		return ret;
	}
	
	/** 获取服务器信息 */
	public GetServerInfo_S2C_Msg getServerInfo(GetServerInfo_C2S_Msg msg) {
		GetServerInfo_S2C_Msg ret = null;
		String content = this.sendWithReturn(msg);
		if (content != null) {
			ret = JSON.parseObject(content, GetServerInfo_S2C_Msg.class);
		}
		return ret;
	}
}