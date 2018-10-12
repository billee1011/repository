package com.lingyu.game;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.codec.Protocol;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.io.Session;
import com.lingyu.common.io.SessionManager;
import com.lingyu.game.service.mahjong.MahjongConstant;

@Service
public class GameAdlProcessorImpl extends GameAdlProcessor {
	private static final Logger logger = LogManager.getLogger(GameAdlProcessorImpl.class);

	/**
	 * 19000 [moveSpeed,interval] 防加速外挂和心跳作用 直接踢用户下线 时间间隔小于服务器记录的间隔的0.8倍则踢线
	 * 如果当前时间戳 - 上次时间戳 小于时间间隔乘以容差值 now - lastTimer < interval * ( 1-
	 * allowance)，如果此错误次数达到错误次数上限次
	 */
	@Override
	public void keepAlive(Session session, JSONObject msg) {
		// logger.info("keepAlive: userId={},roleId={},sessionId={}",
		// session.getUserId(), session.getRoleId(),
		// session.getId());
		Protocol protocol = new Protocol();
		protocol.cmd = MsgType.CLIENT_HEART_BEAT_MSG;
		protocol.body.put(ErrorCode.RESULT, ErrorCode.OK);
		protocol.body.put(MahjongConstant.CLIENT_DATA, new Object[] {});
		session.sendMsg(protocol);
	}

	@Override
	public void getRoleList(Session session, JSONObject msg) {
		Object[] objects = (Object[]) msg.get(MahjongConstant.CLIENT_DATA);
		String userId = String.valueOf(objects[1]);
		session.setUserId(userId);

		SessionManager.getInstance().addSession4User(userId, session);
	}

	@Override
	public void creatRole(Session session, JSONObject msg) {
		Object[] objects = (Object[]) msg.get(MahjongConstant.CLIENT_DATA);
		String userId = String.valueOf(objects[0]);
		session.setUserId(userId);
		SessionManager.getInstance().addSession4User(userId, session);
	}

	@Override
	public void loginGame(Session session, JSONObject msg) {
		Object object = msg.get(MahjongConstant.CLIENT_DATA);
		JSONArray jsonArray = (JSONArray) object;
		Object[] objects = jsonArray.toArray();
		String userId = String.valueOf(objects[3]);
		if (StringUtils.isEmpty(userId)) {
			// 客户端没有userid缓存,临时把设备码当userid
			userId = String.valueOf(objects[3]);
		}
		session.setUserId(userId);

		// 同一个账号登陆。踢掉前一个登陆的。
		Session old = SessionManager.getInstance().getSession4User(userId);
		if (old != null) {
			logger.info("kick old session,userId={}", userId);
			old.sendMsg(new Object[] { MsgType.Disconnet_S2C_Msg, ErrorCode.USERID_RELOAD });
			old.close();
		}

		SessionManager.getInstance().addSession4User(userId, session);
	}

	@Override
	public void heartBeat(Session session, JSONObject msg) {
		logger.info("heartBeat: roleId={},userId={},sessionId={}", session.getRoleId(), session.getUserId(),
		        session.getId());
		// session.sendMsg(new Object[] { MsgType.HEART_BEAT_Msg, new Object[]
		// {} });
	}
}
