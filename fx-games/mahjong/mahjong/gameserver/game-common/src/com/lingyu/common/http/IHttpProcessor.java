package com.lingyu.common.http;

import com.lingyu.msg.http.AnnounceDelete_C2S_Msg;
import com.lingyu.msg.http.AnnounceDelete_S2C_Msg;
import com.lingyu.msg.http.Announce_C2S_Msg;
import com.lingyu.msg.http.Announce_S2C_Msg;
import com.lingyu.msg.http.GetServerInfo_C2S_Msg;
import com.lingyu.msg.http.GetServerInfo_S2C_Msg;
import com.lingyu.msg.http.KickOffPlayer_C2S_Msg;
import com.lingyu.msg.http.KickOffPlayer_S2C_Msg;
import com.lingyu.msg.http.MaintainServer_C2S_Msg;
import com.lingyu.msg.http.MaintainServer_S2C_Msg;
import com.lingyu.msg.http.Redeem_C2S_Msg;
import com.lingyu.msg.http.Redeem_S2C_Msg;
import com.lingyu.msg.http.VersionNotic_C2S_Msg;
import com.lingyu.msg.http.VersionNotic_S2C_Msg;

public interface IHttpProcessor {
	
	/** 公告 */
	Announce_S2C_Msg announce(Announce_C2S_Msg msg);
	
	/** 删除公告 */
	AnnounceDelete_S2C_Msg announceDelete(AnnounceDelete_C2S_Msg msg);
	
	/** 发送补偿，就是邮件功能*/
	Redeem_S2C_Msg redeem(Redeem_C2S_Msg msg);
	
	/** 官方公告 版本公告*/
	VersionNotic_S2C_Msg versionNotic(VersionNotic_C2S_Msg msg);
	
	/** 获取服务器信息 */
	GetServerInfo_S2C_Msg getServerInfo(GetServerInfo_C2S_Msg msg);
	
	/** 服务器要维护啦。开始踢人*/
	KickOffPlayer_S2C_Msg kickOffPlayer(KickOffPlayer_C2S_Msg msg);
	
	/** 服务器维护*/
	MaintainServer_S2C_Msg maintainServer(MaintainServer_C2S_Msg msg);
}