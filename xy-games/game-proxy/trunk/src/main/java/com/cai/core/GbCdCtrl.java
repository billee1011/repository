package com.cai.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ESysMsgType;
import com.cai.common.util.SessionUtil;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import io.netty.util.AttributeKey;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

public class GbCdCtrl implements Serializable{
	
	private static final Logger log = LoggerFactory.getLogger(GbCdCtrl.class);
	
	private static final long serialVersionUID = 1L;
	
	
	private Map<Opt, Long> m = new HashMap<Opt, Long>();	

	public static AttributeKey<Map<Opt, Long>> SESSION_CD_CTRL = AttributeKey.valueOf("SESSION_CD_CTRL~~");
	
	
	/**
	 * 针对玩家对象来用
	 * @param opt
	 * @return
	 */
	public boolean canHandle(Opt opt,String playerId){
		long time =  System.currentTimeMillis();
		if(!m.containsKey(opt) || null == m.get(opt)||(time - m.get(opt)) >= opt.getValue() || (time - m.get(opt)) < 0) {
			m.put(opt,time);
			return true;
		}
//		PhzMsgHelper.getInstance().sendCustomTips(playerId, "提示", "操作过于频繁！");
		return false;
	}
	
	
	public static boolean canHandle(C2SSession session, Opt opt){
//		Map<Opt, Long> m = SessionUtil.getAttr(session,SESSION_CD_CTRL);
//		if(null == m){
//			m = new HashMap<Opt, Long>();
//			SessionUtil.setAttr(session,SESSION_CD_CTRL, m);
//		}
//		long time =  System.currentTimeMillis();
//		if(!m.containsKey(opt) || null == m.get(opt)||(time - m.get(opt)) >= opt.getValue() || (time - m.get(opt)) < 0) {
//			m.put(opt,time);
//			return true;
//		}
//		session.send(MessageResponse.getMsgAllResponse(-1, "操作过于频繁!请稍后再试").build());
//		log.error("opt is ==="+opt);
		return true;
	}
	
	/**
	 * 必须限制频率的
	 * @param session
	 * @param opt
	 * @return
	 */
	public static boolean canHandleMust(C2SSession session, Opt opt){
		Map<Opt, Long> m = SessionUtil.getAttr(session,SESSION_CD_CTRL);
		if(null == m){
			m = new HashMap<Opt, Long>();
			SessionUtil.setAttr(session,SESSION_CD_CTRL, m);
		}
		long time =  System.currentTimeMillis();
		if(!m.containsKey(opt) || null == m.get(opt)||(time - m.get(opt)) >= opt.getValue() || (time - m.get(opt)) < 0) {
			m.put(opt,time);
			return true;
		}
		session.send(MessageResponse.getMsgAllResponse(-1, "操作过于频繁!请稍后再试").build());
		log.error("操作过于频繁!请稍后再试"+opt);
		return false;
	}
	
	
	public static enum Opt{
		BUY_GOODS(50),
		PAY_IOS(50),
		SIGNATURE(50),
		REQUEST_GAME_ROOM_RECORD(200),
		PROXY_ROOM_RECORD(100),
		REQUEST_GAME_ROUND_RECORD(200),
		ROUND_RECORD_VIDEO(100),
		PARENT_ROUND_RECORD_VIDEO(150),
		ROUND_RECORD_VIDEO_BY_NUM(150),
		PROXY_ROOM_MAIN_VIEW(50),
		PROXY_ROOM_CREATE(50),
		
		JOIN_GOLD_ROOM(100),
		PROXY_ROOM_APP_LIST(20),
		GIVE_CARD_LOG(150),
		BUY_GOLD(100),
		
		UPDATE_RECOMMEND_ID(50),
		RECOMMEND_AGENT_ID(50),
		CLUB_REQ_RECORD(100),
		
		CLUB_HONOUR_RECORD(100),
		
		RANK_DATA(100),
		RED_PACK_GET_DATA(100),
		RED_PACK_RECEIVE_DATA(100),
		
		
		PROXY_ROOM_COUNT_REQ(50),
		GAME_RULE(100),
		
		
		C2S_MATCH_ENTER_MATCH(50),
		C2S_MATCH_LEAVE(50),
		C2S_MATCH_OEVER_DATA(50),
	
		NEW_GAME_RECORDS(100),
		
		DB_RECORD_REPLY(200),
		GOLDCARD_TRANS(200),
		CLUB_EXCLUSIVE(200),
		CLIENT_UP(1000),
		CLUB_RULE_RECORD(200);
		private long time;
		private Opt(long time){this.time = time;}
		
		public long getValue(){return time;}
	}
}
