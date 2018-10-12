package com.lingyu.admin.manager;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.admin.network.AsyncHttpClient;
import com.lingyu.admin.network.GameClient;
import com.lingyu.admin.network.GameClientManager;
import com.lingyu.common.entity.GameArea;
import com.lingyu.msg.http.GetServerInfo_C2S_Msg;
import com.lingyu.msg.http.GetServerInfo_S2C_Msg;
import com.lingyu.msg.http.KickOffPlayer_C2S_Msg;
import com.lingyu.msg.http.MaintainServer_C2S_Msg;

@Service
public class ProgramSupportManager {
	@Autowired
	private GameClientManager gameClientManager;
	@Autowired
	private GameAreaManager gameAreaManager;
	
	
	/** 获取服务器信息 */
	public GetServerInfo_S2C_Msg GetServerInfo(String pid, int areaId) {
		GetServerInfo_S2C_Msg msg = null;
		GameClient gameClient = gameClientManager.getGameClientByAreaId(pid, areaId);
		GameArea gameArea = gameAreaManager.getGameAreaByAreaId(pid, areaId);
		if (gameArea.isValid()) {
			msg = gameClient.getServerInfo(new GetServerInfo_C2S_Msg());

		}
		return msg;
	}
	
	/**
	 * 踢人
	 * @param areaList
	 * @param reason
	 */
	public void kickOff(List<GameArea> areaList, String reason) {
		KickOffPlayer_C2S_Msg msg = new KickOffPlayer_C2S_Msg();
		msg.setReason(reason);
		AsyncHttpClient.getInstance().send(areaList, msg);
	}
	
	/**
	 * 维护
	 * @param status
	 * @param reason
	 * @param areaList
	 * @param foreseeOpenTime
	 * @param maintainUrl
	 * @return
	 */
	public String maintain(int status, String reason, List<GameArea> areaList, Date foreseeOpenTime, String maintainUrl) {
		MaintainServer_C2S_Msg msg = new MaintainServer_C2S_Msg();
		msg.setStatus(status);
		msg.setReason(reason);
		AsyncHttpClient.getInstance().send(areaList, msg);
		return "";
	}

	
}
