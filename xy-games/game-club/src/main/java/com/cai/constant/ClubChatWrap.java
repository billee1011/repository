/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.Arrays;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.cai.common.util.LimitQueue;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import protobuf.clazz.Common.ChatMsgReq;
import protobuf.clazz.Common.ChatMsgRsp;

/**
 * 
 * 
 * 俱乐部聊天
 * 
 * @author wu_hc date: 2017年11月22日 上午11:15:45 <br/>
 */
public final class ClubChatWrap {

	/**
	 * 俱乐部聊天
	 */
	private final LimitQueue<String> chatMsgQueue = new LimitQueue<>(50);

	/**
	 * 俱乐部id
	 */
	private final int clubId;

	/**
	 * @param clubId
	 * @param source
	 *            落地数据
	 */
	public ClubChatWrap(int clubId, String source) {
		this.clubId = clubId;
		this.unSerializeFromDB(source);
	}

	/**
	 * 追加聊天数据
	 * 
	 * @param req
	 */
	public void appendChat(final ChatMsgReq req) {
		chatMsgQueue.offer(req.getChatMsg());
	}

	/**
	 * 
	 * @return
	 */
	public List<String> getChatMsgList() {
		return Arrays.asList(chatMsgQueue.toArray(new String[chatMsgQueue.size()]));
	}

	/**
	 * 
	 * @return
	 */
	public List<ChatMsgRsp> serializeToBuilder(long uniqueId) {
		List<ChatMsgRsp> msg = Lists.newArrayList();
		for (String chat : getChatMsgList()) {
			try {
				if (uniqueId != 0) {
					ClubChatMsg chatMsg = JSON.parseObject(chat, ClubChatMsg.class);
					if (chatMsg.getUniqueId() <= uniqueId) {
						continue;
					}
				}
				msg.add(ChatMsgRsp.newBuilder().setChatMsg(chat).build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return msg;
	}

	public int getClubId() {
		return clubId;
	}

	/**
	 * 序列化到落地
	 * 
	 * @return
	 */
	public String serializeToDB() {
		try {
			return JSON.toJSON(getChatMsgList()).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 反序列化
	 * 
	 * @param source
	 */
	public void unSerializeFromDB(final String source) {
		if (Strings.isNullOrEmpty(source))
			return;

		try {
			chatMsgQueue.clear();
			// 容错，如果前面已经超过一百条，截取最新100条
			List<String> chats = JSON.parseArray(source, String.class);
			chats.forEach((chat) -> {
				chatMsgQueue.offer(chat);
			});
			// chatMsgQueue.addAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ClubChatWrap w = new ClubChatWrap(1, "");
		for (int i = 0; i < 100; i++) {
			w.appendChat(ChatMsgReq.newBuilder().setChatMsg("xxxxxxxxxxxxxxxxxxxxxxxxx" + i).build());
		}

		String xxx = w.serializeToDB();
		System.out.println(xxx);
		List<String> ss = JSON.parseArray(xxx, String.class);
		System.out.println(ss);
	}
}
