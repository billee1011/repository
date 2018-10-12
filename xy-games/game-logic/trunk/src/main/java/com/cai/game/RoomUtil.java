/**
 * 
 */
package com.cai.game;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.cai.activity.redpackets.RedPackManager;
import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ClubExclusiveGoldLogModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.common.util.ClubRangeCostUtil;
import com.cai.common.util.LocationUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * @author xwy
 *
 */
public class RoomUtil {

	private static Logger logger = Logger.getLogger(RoomUtil.class);

	/**
	 * 记录机器人开房
	 * 
	 * @param room
	 */
	public static void realkou_dou(AbstractRoom room) {
		if (room.is_sys()) {
			return;
		}

		// 比赛场直接返回true
		if (room.id > 0) {
			// logger.warn("realkou_dou room.id "+room.id
			// +"roomID="+room.getRoom_id());
			return;
		}

		if (room.clubInfo.matchId > 0) {
			room._recordRoomRecord.setRealKouDou(true);
			return;
		}

		// int[] roundGoldArray =
		// SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(room._game_type_index);
		// if (roundGoldArray == null) {
		// logger.warn("realkou_dou roundGoldArray is null _game_type_index" +
		// room._game_type_index + "roomID="
		// + room.getRoom_id());
		// return;
		// }
		//
		// SysParamModel findParam = null;
		// for (int index : roundGoldArray) {
		// SysParamModel tempParam =
		// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id())
		// .get(index);// 扑克类30
		// if (tempParam == null) {
		// continue;
		// }
		// if (tempParam.getVal1() == room._game_round) {
		// findParam = tempParam;
		// break;
		// }
		// }
		// if (findParam == null) {
		// logger.warn("realkou_dou findParam is null _game_type_index" +
		// room._game_type_index + "roomID="
		// + room.getRoom_id() + "room.getGame_id()=" + room.getGame_id());
		// return;
		// }
		int check_gold = room.config_cost_dou;
		// 确定扣款数量,第四个参数用于控制俱乐部开房扣卡政策
		// int check_gold = (room.clubInfo.clubId > 0 && 1 ==
		// findParam.getVal4()) ? findParam.getVal5().intValue() :
		// findParam.getVal2().intValue();

		// 设置为已经处理真实扣逗了。
		room._recordRoomRecord.setRealKouDou(true);
		room._recordRoomRecord.setExclusiveGold(room.clubInfo.exclusive);
		int goldType = 0;
		if (room.clubInfo.exclusive) {
			goldType = 1;
		}
		Date curDate = new Date();
		if (null != room._recordRoomRecord) {
			room._recordRoomRecord.setReal_cost_time(curDate);
		}
		room.setRealCostTime(curDate.getTime() / 1000L);
		// 收费
		if (check_gold != 0) {
			StringBuilder buf = new StringBuilder();
			buf.append("真实扣豆 创建房间:" + room.getRoom_id()).append("game_id:" + room.getGame_id()).append(",game_type_index:" + room._game_type_index)
					.append(",game_round:" + room._game_round);
			// PlayerServiceImpl.getInstance().subRealGold(room.getRoom_owner_account_id(),
			// check_gold, false, buf.toString());

			// 专属豆和房卡真实扣豆日至分开统计,和tangbin确定过 by:wu
			if (/* room.club_id > 0 && */room.clubInfo.exclusive) { // 专属豆扣豆

				ClubExclusiveGoldLogModel logModel = new ClubExclusiveGoldLogModel();
				logModel.setAccount_id(room.getRoom_owner_account_id());
				logModel.setAppId(room.getGame_id());
				logModel.setCreate_time(new Date());
				logModel.setMsg(buf.toString());
				logModel.setV1(0L); // 旧值
				logModel.setV2(0L); // 新值
				logModel.setV3(check_gold); // 真实扣豆的数量
				logModel.setGameTypeIndex(room.getGameTypeIndex());
				logModel.setClubId(room.clubInfo.clubId);
				logModel.setTargetAccountId(0L);
				logModel.setOperateType(EGoldOperateType.REAL_OPEN_ROOM.getId());
				logModel.setOpenRoomWay(room.getCreate_type());
				MongoDBServiceImpl.getInstance().getLogQueue().add(logModel);

			} else { // 其他扣豆
				PlayerServiceImpl.getInstance().subRealGold(room, check_gold, false, buf.toString());
			}

			if (room.club_id > 0) {

				StringBuilder playerMsg = new StringBuilder();
				for (Player player : room.get_players()) {
					if (player == null) {
						continue;
					}
					playerMsg.append(player.getNick_name()).append("(").append(player.getAccount_id()).append(")").append(";");
				}
				if (playerMsg.length() > 0) {
					playerMsg.deleteCharAt(playerMsg.length() - 1);
				}

				PlayerServiceImpl.getInstance().subClubGold(room.getRoom_owner_account_id(), check_gold, false, buf.toString(), room.club_id,
						room._game_type_index, room._game_round, room._game_rule_index, room.getRoom_id(), playerMsg.toString(),
						room.clubInfo.exclusive);
			} else if (StringUtils.isNotEmpty(room.groupID)) {
				StringBuffer nickNames = new StringBuffer();
				for (Player player : room.get_players()) {
					// 容错！！！！
					if (null == player) {
						continue;
					}
					nickNames.append(player.getNick_name()).append(" ");
				}
				PlayerServiceImpl.getInstance().subRobotGold(room.getRoom_owner_account_id(), check_gold, false, buf.toString(), room.groupID,
						room.groupName, room._game_type_index, room._game_round, room._game_rule_index, room.getRoom_id(), nickNames.toString());
			}

		}
		proxyConsumeStatistics(room.getRoom_owner_account_id(), room._game_type_index, check_gold, goldType);
		// 扣豆成功
		RedPackManager.getInstance().checkReadPackReward(room);
	}

	private static void proxyConsumeStatistics(long accountId, int gameTypeIndex, int cost, int goldType) {
		HashMap<String, String> map = new HashMap<>();
		map.put("accountId", accountId + "");
		map.put("reduceType", goldType + "");
		map.put("gameTypeIndex", gameTypeIndex + "");
		map.put("gold", cost + "");
		map.put("exclusiveGold", cost + "");
		try {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.rmiInvoke(RMICmd.WEIXIN_PROXY_CONSUME, map);
		} catch (Exception e) {
			logger.error("WEIXIN_PROXY_CONSUME rmi error", e);
		}

	}

	static boolean kou_dou(AbstractRoom room) {
		int game_type_index = room._game_type_index;
		// 比赛场直接返回true

		if (SysGameTypeDict.getInstance().getSysGameType(game_type_index) == null) {
			logger.warn("getSysGameType is null !!!" + room.getRoom_id());
			return false;
		}
		// 收费索引
		room.game_index = SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index);
		room.setGame_id(SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index));

		if (room.id > 0) {
			// logger.warn("room.id "+room.id +"roomID="+room.getRoom_id());
			return true;
		}

		if (room.clubInfo.matchId > 0) {
			return true;
		}

		if (room.is_sys()) {
			return true;
		}

		int[] roundGoldArray = SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(game_type_index);
		if (roundGoldArray == null) {
			logger.warn("roundGoldArray is null !!!" + room.getRoom_id() + "game_type_index=" + game_type_index);
			return false;
		}

		SysParamModel findParam = null;
		for (int index : roundGoldArray) {
			SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(index);// 扑克类30
			if (tempParam == null) {
				continue;
			}
			if (tempParam.getVal1() == room._game_round) {
				findParam = tempParam;
				break;
			}
		}
		if (findParam == null) {
			return false;
		}

		// 是否俱乐部特殊扣费
		final boolean isClubSpecialCost = (room.clubInfo.clubId > 0) && (1 == findParam.getVal4());

		// 确定扣款数量,第三个参数用于控制俱乐部开房扣卡政策
		int check_gold = isClubSpecialCost ? findParam.getVal5().intValue() : findParam.getVal2().intValue();

		// add 20180205,临时需求-俱乐部人数区间扣豆
		if (ClubRangeCostUtil.INSTANCE.isActive() && room.clubInfo.clubId > 0) {
			long value = ClubRangeCostUtil.INSTANCE.getValue(Pair.of(game_type_index, findParam.getVal1()), room.clubInfo.clubMemberSize);
			if (value >= 0 && value != Long.MIN_VALUE) {
				check_gold = (int) value;
			}
		}

		boolean isVoiceSpecialCost = ClubRangeCostUtil.INSTANCE.isIncludeVoice();
		// 1语音房附加扣豆，和2互斥
		if (isVoiceSpecialCost) {
			if (room.getRuleValue(GameConstants.GAME_RULE_VOICE_ROOM) > 0) {
				check_gold += findParam.getVal3();
			}
		}
		if (check_gold == 0) {
			logger.warn("check_gold is 0 !!!" + room.getRoom_id() + "game_type_index=" + game_type_index);
			PlayerServiceImpl.getInstance().roomLogInfo(room, SystemConfig.logic_index, "" + check_gold);
			return true;
		}

		// 2语音房附加扣豆，和1互斥
		if (!isVoiceSpecialCost) {
			if (room.getRuleValue(GameConstants.GAME_RULE_VOICE_ROOM) > 0) {
				check_gold += findParam.getVal3();
			}
		}

		// 是否免费的
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(room.game_index);

		if (sysParamModel != null && sysParamModel.getVal2() == 1) {
			// 收费

			StringBuilder buf = new StringBuilder();
			buf.append("创建房间:" + room.getRoom_id()).append("game_id:" + room.getGame_id()).append(",game_type_index:" + game_type_index)
					.append(",game_round:" + room._game_round);

			if (room.clubInfo.clubId > 0) {
				buf.append(",当前亲友圈[").append(room.clubInfo.clubId).append("]人数:").append(room.clubInfo.clubMemberSize);
			}
			// 1 如果是俱乐部开的房间,优先判断俱乐部专属豆
			if (/* room.clubInfo.clubId > 0 && */ExclusiveGoldCfg.get().isUseExclusiveGold()
					|| (ExclusiveGoldCfg.get().isRobotCreateRoomCostExclusiveGold() && StringUtils.isNotEmpty(room.groupID))) {

				long createAccountId = room.getRoom_owner_account_id();
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

				ClubExclusiveRMIVo vo = ClubExclusiveRMIVo.newVo(createAccountId, room.getGame_id(), check_gold, EGoldOperateType.OPEN_ROOM)
						.setDesc(buf.toString()).setGameTypeIndex(room.getGameTypeIndex()).setClubId(room.clubInfo.clubId);

				AddGoldResultModel result = centerRMIServer.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_COST, vo);
				if (null != result && result.isSuccess()) {
					room.config_cost_dou = room.cost_dou = check_gold;
					room.clubInfo.exclusive = true;

					Object attament = result.getAttament();
					if (null != attament && attament instanceof CommonILI) {
						ClubMsgSender.sendExclusiveGoldUpdate(createAccountId, Arrays.asList((CommonILI) attament));
					}

					PlayerServiceImpl.getInstance().roomLogInfo(room, SystemConfig.logic_index, "" + check_gold);
					return true;
				} else { // 专属豆不足的情况下，如果这款游戏配置必须使用专属豆，则创建房间失败
					// GAME-TODO,强制专属豆扣费
					if (ExclusiveGoldCfg.get().isMustCostExclusive(room.getGame_id())) {
						return false;
					}
				}
			}

			// 2俱乐部专属豆不够，用房卡支付
			AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(room.getRoom_owner_account_id(), check_gold, false, buf.toString());
			if (result.isSuccess() == false) {
				return false;
			} else {
				// 扣豆成功
				room.config_cost_dou = room.cost_dou = check_gold;
			}

		}
		PlayerServiceImpl.getInstance().roomLogInfo(room, SystemConfig.logic_index, "" + check_gold);
		return true;
	}

	public static boolean send_response_to_player(Room room, int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(room.getRoom_id());// 日志用的
		roomResponse.setAppId(room.getGame_id());
		if (room.get_players()[seat_index] == null) {
			return false;
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		PlayerServiceImpl.getInstance().send(room.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public static boolean send_error_notify(Room room, int seat_index, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(room.get_players()[seat_index], responseBuilder.build());

		return false;
	}

	public static boolean send_error_notify(Player player, int type, String msg, int time) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		e.setTime(time);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

		return false;
	}

	public static boolean send_response_to_other(Room room, int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(room.getRoom_id());// 日志用的
		roomResponse.setAppId(room.getGame_id());
		Player player = null;
		for (int i = 0; i < room.get_players().length; i++) {
			player = room.get_players()[i];
			if (player == null)
				continue;
			if (i == seat_index)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			// this.log_error("nickname =
			// "+this.get_players()[seat_index].getNick_name() + "nickname =
			// "+this.get_players()[i].getNick_name()
			// +roomResponse.getAudioLen()+ "time=" +
			// System.currentTimeMillis());
			PlayerServiceImpl.getInstance().send(room.get_players()[i], responseBuilder.build());
		}
		room.observers().sendAll(roomResponse);
		return true;
	}

	/**
	 * 只给围观者发
	 * 
	 * @param roomResponse
	 * @return
	 */
	public static boolean send_response_to_observer(Room room, RoomResponse.Builder roomResponse) {
		room.observers().sendAll(roomResponse);
		return true;
	}

	public static boolean handler_audio_chat(Room room, Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_AUDIO_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setAudioChat(chat);
		roomResponse.setAudioSize(l);
		roomResponse.setAudioLen(audio_len);
		send_response_to_other(room, player.get_seat_index(), roomResponse);
		return true;
	}

	public static boolean send_response_to_room(Room room, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(room.getRoom_id());// 日志用的
		roomResponse.setAppId(room.getGame_id());
		Player player = null;
		for (int i = 0; i < room.get_players().length; i++) {
			player = room.get_players()[i];
			if (player == null)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			PlayerServiceImpl.getInstance().send(room.get_players()[i], responseBuilder.build());
		}
		room.observers().sendAll(roomResponse);
		return true;
	}

	public static boolean handler_emjoy_chat(Room room, Player player, int id) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EMJOY_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setEmjoyId(id);
		send_response_to_room(room, roomResponse);

		return true;
	}

	public static void getLocationTip(Room room) {
		try {

			if (room._game_round > 1)
				return;
			String tipMsg = "";

			StringBuffer buf = new StringBuffer();
			HashSet<String> names = new HashSet<String>();
			for (int i = 0; i < room.get_players().length; i++) {
				if (room.get_players()[i] == null)
					continue;
				Player player = room.get_players()[i];
				if (player.locationInfor == null || player.locationInfor.getPosX() == 0 || player.locationInfor.getPosX() == 0) {
					continue;
				}
				for (int j = i; j < room.get_players().length; j++) {
					if (room.get_players()[j] == null)
						continue;
					Player targetPlayer = room.get_players()[j];
					if (targetPlayer.locationInfor == null || targetPlayer.locationInfor.getPosX() == 0
							|| targetPlayer.locationInfor.getPosX() == 0) {
						continue;
					}
					if (targetPlayer.getAccount_id() == player.getAccount_id())
						continue;
					double distance = LocationUtil.LantitudeLongitudeDist(player.locationInfor.getPosX(), player.locationInfor.getPosY(),
							targetPlayer.locationInfor.getPosX(), targetPlayer.locationInfor.getPosY());

					int tipDistance = 1000;
					SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5005);
					if (sysParamModel != null && sysParamModel.getVal5() > 0) {
						tipDistance = sysParamModel.getVal5();
					}
					if (distance < tipDistance) {
						names.add(player.getNick_name());
						names.add(targetPlayer.getNick_name());
					}
				}
			}
			for (String name : names) {
				buf.append(name).append(" ");
			}
			if (names.size() > 1) {
				tipMsg = buf.append("距离过近").toString();
				for (int i = 0; i < room.get_players().length; i++) {
					Player player = room.get_players()[i];
					if (player == null)
						continue;
					// WalkerGeek 比赛场房间不发送距离过近飘字
					if (!room.is_match()) {
						send_error_notify(player, 2, tipMsg, 5);
					}
				}
			}

		} catch (Exception e) {
			logger.error("定位提示异常", e);
		}
	}

	/**
	 * 玩家座位重新随机
	 * 
	 * @param room
	 *//*
		 * public static void random_player_index(Room room){ Player[] players =
		 * room.get_players();
		 * 
		 * //座位收集 List<Integer> indexArr = new ArrayList<Integer>(); for (Player
		 * player : players) { if(player == null){ continue; }
		 * indexArr.add(player.get_seat_index()); }
		 * 
		 * //重新打乱 for (Player player : players) { if(player == null){ continue;
		 * } //随机位置 int randomNum = RandomUtil.getRandomNumber(indexArr.size());
		 * System.out.println(randomNum);
		 * player.set_seat_index(indexArr.get(randomNum));
		 * indexArr.remove(randomNum); } System.out.println();
		 * 
		 * //玩家数据 RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		 * roomResponse.setType(MsgConstants.RESPONSE_PLAYER_INFO); Player
		 * rplayer; for (int i = 0; i < players.length; i++) { rplayer =
		 * players[i]; if (rplayer == null) continue; RoomPlayerResponse.Builder
		 * room_player = RoomPlayerResponse.newBuilder();
		 * room_player.setAccountId(rplayer.getAccount_id());
		 * room_player.setHeadImgUrl(rplayer.getAccount_icon());
		 * room_player.setIp(rplayer.getAccount_ip());
		 * room_player.setUserName(rplayer.getNick_name());
		 * room_player.setSeatIndex(rplayer.get_seat_index());
		 * room_player.setOnline(rplayer.isOnline() ? 1 : 0);
		 * room_player.setIpAddr(rplayer.getAccount_ip_addr());
		 * room_player.setSex(rplayer.getSex());
		 * room_player.setScore(room._player_result.game_score[i]);
		 * room_player.setReady(1);//已准备
		 * room_player.setPao(room._player_result.pao[i] < 0 ? 0 :
		 * room._player_result.pao[i]);
		 * room_player.setQiang(room._player_result.qiang[i]);
		 * //room_player.setOpenThree(room._player_open_less[i] == 0 ? false :
		 * true); room_player.setMoney(rplayer.getMoney());
		 * room_player.setGold(rplayer.getGold()); if (rplayer.locationInfor !=
		 * null) { room_player.setLocationInfor(rplayer.locationInfor); }
		 * roomResponse.addPlayers(room_player); } send_response_to_room(room,
		 * roomResponse); }
		 */
}
