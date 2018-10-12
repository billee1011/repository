package com.lingyu.game;

import java.util.Collection;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.ServerInfo;
import com.lingyu.common.http.IHttpProcessor;
import com.lingyu.common.io.Session;
import com.lingyu.common.io.SessionManager;
import com.lingyu.game.service.announce.AnnounceManager;
import com.lingyu.game.service.mail.MailManager;
import com.lingyu.game.service.system.SystemManager;
import com.lingyu.game.service.versionnotice.VersionNoticeManager;
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

@Service
public class GameHttpProcessorImpl implements IHttpProcessor {
	private static final Logger logger = LogManager.getLogger(GameHttpProcessorImpl.class);
	
	@Autowired
	private AnnounceManager announceManager;
	@Autowired
	private MailManager mailManager;
	@Autowired
	private VersionNoticeManager versionNoticeManager;
	@Autowired
	private SystemManager systemManager;
	
	
	/**
	 * 添加公告
	 */
	@Override
	public Announce_S2C_Msg announce(Announce_C2S_Msg msg) {
		int id = msg.getId();
		String content = msg.getContent();
		Date beginTime = msg.getBeginTime();
		Date endTime = msg.getEndTime();
		int interval = msg.getInterval();
		announceManager.addAnnounce(id, content, beginTime, endTime, interval);
		Announce_S2C_Msg ret = new Announce_S2C_Msg();
		ret.setRetCode(ErrorCode.EC_OK);
		ret.setAnnounceId(id);
		return ret;
	}

	/**
	 * 删除公告
	 */
	@Override
	public AnnounceDelete_S2C_Msg announceDelete(AnnounceDelete_C2S_Msg msg) {
		int id = msg.getId();
		announceManager.deleteAnnounceByAnnounceId(id);

		AnnounceDelete_S2C_Msg ret = new AnnounceDelete_S2C_Msg();
		ret.setRetCode(ErrorCode.EC_OK);
		ret.setAnnounceId(id);
		return ret;
	}
	
	/**
	 * 补偿(就是邮件功能)
	 */
	@Override
	public Redeem_S2C_Msg redeem(Redeem_C2S_Msg msg) {
		return mailManager.redeem(msg);
	}
	
	/**
	 * 添加版本公告
	 */
	@Override
	public VersionNotic_S2C_Msg versionNotic(VersionNotic_C2S_Msg msg) {
		VersionNotic_S2C_Msg ret = new VersionNotic_S2C_Msg();
		int type = msg.getType();
		String content = msg.getContent();
		Date time = msg.getTime();
		versionNoticeManager.addVersionNotice(type, content, time, msg.getVersion());
		return ret;
	}
	
	/** 获取服务器信息 */
	@Override
	public GetServerInfo_S2C_Msg getServerInfo(GetServerInfo_C2S_Msg msg) {
		GetServerInfo_S2C_Msg ret = new GetServerInfo_S2C_Msg();
		ServerInfo serverInfo = GameServerContext.getServerInfo();
		ret.setStatus(serverInfo.getStatus());
		ret.setTimes(serverInfo.getTimes());
		ret.setStartTime(serverInfo.getStartTime());
		ret.setMaintainTime(serverInfo.getMaintainTime());
		ret.setCombineTime(serverInfo.getCombineTime());
		ret.setGray(0);
		ret.setMaxConcurrentUser(GameServerContext.getAppConfig().getMaxConcurrentUser());
		return ret;
	}
	
	/***
	 * 服务器要维护啦。开始踢人
	 */
	@Override
	public KickOffPlayer_S2C_Msg kickOffPlayer(KickOffPlayer_C2S_Msg msg) {
		logger.info("踢玩家开始");
		KickOffPlayer_S2C_Msg ret = new KickOffPlayer_S2C_Msg();
		Collection<Session> list = SessionManager.getInstance().getOnlineRoleList();
		for (Session e : list) {
			e.close();
		}
		// GameServerContext.getDataManager().flushAll();
		ret.setRetCode(ErrorCode.EC_OK);
		logger.info("踢玩家完毕");
		return ret;
	}
	
	/**
	 * 服务器维护
	 **/
	@Override
	public MaintainServer_S2C_Msg maintainServer(MaintainServer_C2S_Msg msg) {
		MaintainServer_S2C_Msg ret = new MaintainServer_S2C_Msg();
		int status = msg.getStatus();
		ServerInfo serverInfo = GameServerContext.getServerInfo();
		if (status == SystemConstant.SERVER_STATUS_MAINTAIN) {
			serverInfo.setMaintainTime(new Date());
		} else {
			serverInfo.setOpenTime(new Date());
		}
		serverInfo.setStatus(status);
		ServerConfig appConfig = GameServerContext.getAppConfig();
		GameServerContext.getBackServerManager().stopServer(status,appConfig.getServerList());
		systemManager.update(serverInfo);
		ret.setRetCode(ErrorCode.EC_OK);
		ret.setStatus(status);
		return ret;
	}
}