package com.cai.handler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.cai.coin.CoinService;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EGameType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GoodsModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.MatchType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.core.Global;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.game.AbstractRoom;
import com.cai.game.mj.handler.henansmx.MJTable_SMX;
import com.cai.net.core.ClientHandler;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;
import com.cai.util.MessageResponse;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.log4j.Logger;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.LocationInfor.Builder;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.extendCommon.ExtendsProto.PermitLessRep;
import protobuf.clazz.mj.Klds.KLDS_PAO_QIAN_EXT;
import protobuf.clazz.mj.KwxProto.KWXLiangCard;
import protobuf.clazz.mj.KwxProto.PlayerDZMessage;

/**
 * 房间处理
 *
 * @author run
 */
public class RoomHandler extends ClientHandler<RoomRequest> {

	private static Logger logger = Logger.getLogger(RoomHandler.class);

	/**
	 */
	private static final LoadingCache<Integer, Integer> lockRoom = CacheBuilder.newBuilder().maximumSize(2000).expireAfterAccess(2, TimeUnit.HOURS)
			.build(new CacheLoader<Integer, Integer>() {
				@Override
				public Integer load(Integer key) throws Exception {
					return null;
				}
			});

	@Override
	public void execute() throws Exception {
		final int roomId = request.getRoomId();
		if (roomId > 0) {
			AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if (null != room) {
				room.runInRoomLoop(() -> {
					try {
						doExecute();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		} else {
			logger.error("request roomhandler type="+request.getType());//基本上是12号协议
			Global.getRoomPoolBACK().execute(new Runnable() {
				
				@Override
				public void run() {
					try{
						doExecute();
					}catch(Exception e) {
						e.printStackTrace();
					}
				
				}
			});
		}
	}

	@Override
	public void onRequest() throws Exception {

		int type = request.getType();

		if (type == MsgConstants.REQUST_PLAYER_RELEASE_ROOM) {
			handler_release_room(topRequest.getProxyAccountId(), request);
			return;
		}

		if (type == MsgConstants.REQUES_OX_GAME_START) {
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(request.getRoomId());
			if (room == null) {
				logger.error("房主开始，房间不存在" + request.getRoomId());
				return;
			}
			ReentrantLock lock = room.getRoomLock();
			try {
				lock.lock();
				handler_ox_game_start(request.getRoomId(), topRequest.getProxyAccountId());
			} finally {
				lock.unlock();
			}
			return;
		}

		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(topRequest.getProxyAccountId());

		if (null == player) {
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(request.getRoomId());
			if (room == null) {
				logger.error("房主开始，房间不存在" + request.getRoomId());
				return;
			}
			player = room.get_player((topRequest.getProxyAccountId()));
			if (player == null) {
				player = room.observers().getPlayer(topRequest.getProxyAccountId());
				if (null == player) {
					player = room.godViewObservers().getPlayer(topRequest.getProxyAccountId());
				}
			}
		}
		if (player == null) {
			// logger.error(String.format("玩家不存在，玩家:%d,协议:%d",
			// topRequest.getProxyAccountId(), type));
			if (type != MsgConstants.REQUST_LOCATION_NEW && type != MsgConstants.REQUST_REFRESH_PLAYERS && type != MsgConstants.REQUST_GOODS
					&& type != MsgConstants.REQUST_ENTER_BACK) {
				logger.error(String.format("重大bug玩家不存在，玩家:%d,协议:%d", topRequest.getProxyAccountId(), type));
			}
			return;
		}

		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(player.getRoom_id());

		// 代理可以没房间
		if (room == null) {
			if (type != MsgConstants.REQUST_LOCATION_NEW && type != MsgConstants.REQUST_REFRESH_PLAYERS && type != MsgConstants.REQUST_GOODS
					&& type != MsgConstants.REQUST_ENTER_BACK&& type != MsgConstants.REQUST_PLAYER_BE_IN_ROOM) {
				logger.error(String.format("重大bug房间不存在，玩家:%d,房间号:%d,协议:%d", player.getAccount_id(), player.getRoom_id(), type));
			}
			return;
		}

		if (player.getRoom_id() != request.getRoomId() && request.getRoomId() > 0) {// 记录下不一致的情况---这个异常太特么多了过滤下
			if (type != MsgConstants.REQUST_LOCATION_NEW && type != MsgConstants.REQUST_REFRESH_PLAYERS && type != MsgConstants.REQUST_GOODS) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomIdNotEqual,
						"重大bug roomId不相等" + player.getRoom_id() + "request roomid=" + request.getRoomId() + "type=" + type, player.getAccount_id(),
						SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}

		player.setChannel(session.getChannel());
		
		if(!player.isOnline()){
			handler_online(room.getRoom_id(), player.getAccount_id(), true);//发现不在线，主动推一次
		}
		
		player.setOnline(true);

		// 刷新时间
		room.process_flush_time();
		if (type == MsgConstants.REQUST_AUDIO_CHAT) {
			handler_requst_audio_chat(room.getRoom_id(), player.getAccount_id(), request);
			return;
		} else if (type == MsgConstants.REQUST_EMJOY_CHAT) {
			handler_requst_emjoy_chat(room.getRoom_id(), player.getAccount_id(), request);
			return;
		} else if (type == MsgConstants.REQUST_LOCATION) {
			handler_requst_location(room.getRoom_id(), player.getAccount_id(), request);
			return;
		} else if (type == MsgConstants.REQUST_LOCATION_NEW) {
			handler_requst_location_new(room.getRoom_id(), player.getAccount_id(), request);
			return;
		} else if (type == MsgConstants.REQUST_GOODS) {
			handler_request_goods(room.getRoom_id(), player.getAccount_id(), request);
			return;
		} else if (type == MsgConstants.REQUEST_ROOM_CHAT) {
			handler_requst_chat(room.getRoom_id(), player.getAccount_id(), request, type);
			return;
		}
		ReentrantLock lock = room.getRoomLock();

		try {
			lock.lock();

			if (type == MsgConstants.REQUST_PLAYER_READY) {
				// 准备
				handler_player_ready(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_OUT_CARD) {
				handler_player_out_card(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_OPERATE) {
				handler_player_operate_card(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUES_OPERATE_HANDLER_OPERATE) {
				handler_player_handler_operate_card(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_RELEASE_ROOM) {
				handler_release_room(player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_BE_IN_ROOM) {
				handler_requst_player_be_in_room(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PAO_QIANG) {
				handler_requst_pao_qiang(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_NAO_ZHUANG) {
				handler_requst_nao_zhuang(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_BIAO_YAN) {
				handler_requst_biao_yan(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_OPEN_LESS) {
				handler_requst_open_less(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PROXY_RELEASE_ROOM) {
				handler_release_room(player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_IS_TRUSTEE) {
				handler_request_trustee(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_REFRESH_PLAYERS) {
				room.handler_refresh_player_data(player.get_seat_index());
			} else if (type == MsgConstants.REQUST_CALL_BANKER) {
				handler_call_banker(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_ADD_SCORE) {
				handler_add_jetton(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_OPEN_CARD) {
				handler_open_cards(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_OPERATE_BUTTON) {
				handler_operate_button(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_OUT_CARD_MUL) {
				handler_operate_out_card_mul(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUES_OX_GAME_START) {
				handler_ox_game_start(room.getRoom_id(), player.getAccount_id());
			} else if (type == MsgConstants.REQUES_ASK_PLAYER) {
				handler_ask_player(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUES_UPDATA_ONLINE) {
				handler_online(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUES_CPDZ_YANG_PAI) {
				handler_yang_operate_card(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUEST_XIA_BA) {
				handler_requst_xia_ba(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_OPEN_LESS_EXTENDS) { // 允许少人模式
				handler_requst_open_less_extends(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUET_LIANG_ZHANG) { // 亮牌
				handler_player_operate_liang(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUEST_SWITCH_CARDS) {
				handler_request_switch_cards(room.getRoom_id(), player.getAccount_id(), request, type);
			} else if (type == MsgConstants.RESPONSE_RECORD_INFO_OX_C) {
				handler_requst_record_info(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_CHU_ZI) {
				handler_player_operate_chu_zi(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUEST_SWITCH_TO_MAIN_SCREEN || type == MsgConstants.REQUEST_SWITCH_TO_GAME_SCREEN) {
				handler_switch_screen(room.getRoom_id(), player.getAccount_id(), type);
			}
			if (type > 1000) {
				handler_requst_message_deal(room.getRoom_id(), player.getAccount_id(), request, type);
			}

		} catch (Exception e) {
			logger.error("error,request:" + request, e);

			MongoDBServiceImpl.getInstance()
					.server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e), player.getAccount_id(),
							SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
		} finally {
			lock.unlock();
		}

	}

	private boolean handler_ask_player(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_ask_player(player.get_seat_index(), room_rq.getOpenCard());
	}

	private boolean handler_online(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_online(player.get_seat_index(), room_rq.getOpenCard());
	}
	
	
	private boolean handler_online(int room_id, long account_id, boolean isOnline) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_online(player.get_seat_index(), isOnline);
	}

	private boolean handler_yang_operate_card(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_yang_operate_card(player.get_seat_index(), room_rq.getOpenCard());
	}

	private boolean handler_switch_screen(int room_id, long account_id, int type) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		return table.handler_switch_screen(type, player.get_seat_index());
	}

	private boolean handler_requst_record_info(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = room.handler_requst_record_info(player.get_seat_index(), room_rq.getLuoCode());

		return true;
	}

	private boolean handler_ox_game_start(int room_id, long account_id) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		// 逻辑处理
		return table.handler_ox_game_start(room_id, account_id);
	}

	private boolean handler_operate_out_card_mul(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_operate_out_card_mul(player.get_seat_index(), room_rq.getOutCardsList(), room_rq.getOutCardCount(),
				room_rq.getBOutCardType(), room_rq.getRoomPw());
	}

	// private boolean handler_operate_out_card_mul(int room_id, long
	// account_id, RoomRequest room_rq) {
	// Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
	//
	// if (table == null) {
	// return false;
	// }
	//
	// Player player = table.get_player(account_id);
	// if (player == null) {
	// return false;
	// }
	// OutCardsReq req = PBUtil.toObject(room_rq,OutCardsReq.class);
	// // 逻辑处理
	// return table.handler_operate_out_card_mul(player.get_seat_index(),
	// req.getOutCardsList(), req.getOutCardCount(),
	// req.getBOutCardType());
	// }

	private boolean handler_requst_chat(int room_id, long account_id, RoomRequest room_rq, int type) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		return table.handler_requst_chat(player.get_seat_index(), room_rq, type);
	}

	private boolean handler_requst_message_deal(int room_id, long account_id, RoomRequest room_rq, int type) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}
		Player player = table.get_player(account_id);
		if (player == null) {
			player = table.observers().getPlayer(account_id);
			if (null != player) {
				return table.handler_requst_message_deal(player, player.get_seat_index(), room_rq, type);
			}
			return false;
		}
		return table.handler_requst_message_deal(player, player.get_seat_index(), room_rq, type);
	}

	private boolean handler_operate_button(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		return table.handler_operate_button(player.get_seat_index(), room_rq.getOperateCode());
	}

	public boolean handler_call_banker(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_call_banker(player.get_seat_index(), room_rq.getSelectCallBanker());
	}

	public boolean handler_add_jetton(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		return table.handler_add_jetton(player.get_seat_index(), room_rq.getAddJetton());
	}

	public boolean handler_open_cards(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_open_cards(player.get_seat_index(), room_rq.getOpenCard());

	}

	// 准备
	private boolean handler_player_ready(int room_id, long account_id, RoomRequest room_rq) {
		AbstractRoom table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		if (table.is_sys()) {// 金币场
			return table.handler_player_ready_in_gold(player.get_seat_index(), room_rq.getIsCancelReady());
		} else {

			boolean r = table.handler_player_ready(player.get_seat_index(), room_rq.getIsCancelReady());
			// 俱乐部房间,通知俱乐部服
			if (table.club_id > 0) {
				ClubMsgSender.playerStatusUpdate(ERoomStatus.PLAYER_READY, table, player);
			}
			return r;
		}

	}

	private boolean handler_requst_player_be_in_room(int room_id, long account_id, RoomRequest room_rq) {

		AbstractRoom table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			player = table.observers().getPlayer(account_id);
			if (null != player) {
				return table.handler_observer_be_in_room(player);
			}

			player = table.godViewObservers().getPlayer(account_id);
			if (null != player) {
				return table.handler_god_observer_be_in_room(player);
			}
			return false;
		}

		// 逻辑处理
		player.setMatchConnectStatus(MatchType.C_STATUS_COMMON);
		table.handler_player_be_in_room(player.get_seat_index());

		table.sendTrusteeResp(player.get_seat_index());

		if(request.hasClientVersion()){
			player.setClientVersion(request.getClientVersion());
		}

		// 进房间自动准备的子游戏再同步一下准备状态到俱乐部
		if (table.club_id > 0) {
			ClubMsgSender.playerStatusUpdate(ERoomStatus.PLAYER_READY, table, player);
		}

		return true;
	}

	private boolean handler_player_out_card(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		return table.handler_player_out_card(player.get_seat_index(), room_rq.getOperateCard());
	}

	private boolean handler_player_operate_card(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		if (table.getGame_id() == EGameType.PHUYX.getId()) {
			return handler_player_handler_operate_card(room_id, player.getAccount_id(), room_rq);
		}

		// 逻辑处理
		return table.handler_operate_card(player.get_seat_index(), room_rq.getOperateCode(), room_rq.getOperateCard(), room_rq.getLuoCode());
	}

	private boolean handler_player_handler_operate_card(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		int status = 0;
		if (room_rq.hasHandlerStatus()) {
			status = room_rq.getHandlerStatus();
		}
		// 逻辑处理
		return table.handler_status_operate_card(player.get_seat_index(), room_rq.getOperateCode(), room_rq.getOperateCard(), room_rq.getLuoCode(),
				status);
	}

	private boolean handler_player_operate_liang(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		KWXLiangCard kWXLiangCard = PBUtil.toObject(room_rq, KWXLiangCard.class);
		// 逻辑处理
		boolean r = room.handler_requst_liang_zhang(player.get_seat_index(), kWXLiangCard.getOperateCode(), kWXLiangCard.getOperateCard(),
				kWXLiangCard.getKouCardsList(), kWXLiangCard.getKouCardsCount());

		return true;
	}

	private boolean handler_player_operate_chu_zi(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		PlayerDZMessage playerDZMessage = PBUtil.toObject(room_rq, PlayerDZMessage.class);
		// 逻辑处理
		room.handler_requst_chu_zi(player.get_seat_index(), playerDZMessage.getCanCardList(), playerDZMessage.getType());

		return true;
	}

	private boolean handler_release_room(long account_id, RoomRequest room_rq) {
		int room_id = request.getRoomId();
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		ReentrantLock lock = table.getRoomLock();

		try {
			lock.lock();

			if (!PlayerServiceImpl.getInstance().getRoomMap().containsKey(room_id)) {
				return false;
			}

			// 1 围观者请求离开房间
			if (room_rq.getOperateCode() == GameConstants.Release_Room_Type_OBSERVER) {
				Player player = table.observers().getPlayer(account_id);
				if (null != player) {
					return table.handler_exit_room_observer(player);
				}
				return false;
			}

			// 2上帝视角微观请求离开
			if (room_rq.getOperateCode() == GameConstants.Release_Room_Type_GOD_OBSERVER) {
				Player player = table.godViewObservers().getPlayer(account_id);
				if (null != player) {
					return table.handler_god_view_observer_exit(player);
				}
				return false;
			}

			// 3房间内的玩家请求离开房间
			Player player = table.get_player(account_id);
			if ((player == null) && (account_id != table.getRoom_owner_account_id())) {
				return false;
			}

			// 逻辑处理
			boolean r = table.handler_release_room(player, room_rq.getOperateCode());
			if (r) {
				// ClubMsgSender.playerStatusUpdate(ERoomStatus.PLAYER_EXIT,
				// table, player);
			}

		} finally {
			lock.unlock();
		}

		// if(r==true){
		// PlayerServiceImpl.getInstance().quitRoomId(account_id);
		//
		// PlayerServiceImpl.getInstance().getRoomMap().remove(room_id);
		// }

		return true;
	}

	private boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = room.handler_audio_chat(player, room_rq.getAudioChat(), room_rq.getAudioSize(), room_rq.getAudioLen());

		return true;
	}

	private boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = room.handler_emjoy_chat(player, room_rq.getEmjoyId());

		return true;
	}

	private boolean handler_requst_pao_qiang(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = room.handler_requst_pao_qiang(player, room_rq.getPao(), room_rq.getQiang());

		return true;
	}

	private boolean handler_requst_xia_ba(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		KLDS_PAO_QIAN_EXT otherParam = PBUtil.toObject(room_rq, KLDS_PAO_QIAN_EXT.class);
		// 逻辑处理
		boolean r = room.handler_requst_xia_ba(player, room_rq.getPao(), otherParam.getZiBa(), otherParam.getDuanMen());

		return true;
	}

	private boolean handler_requst_nao_zhuang(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = room.handler_requst_nao_zhuang(player, room_rq.getNao());

		return true;
	}

	private boolean handler_request_switch_cards(int room_id, long account_id, RoomRequest room_rq, int type) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = room.handler_requst_message_deal(player, player.get_seat_index(), room_rq, type);

		return true;
	}

	private boolean handler_requst_biao_yan(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (room == null || !(room instanceof MJTable_SMX)) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = ((MJTable_SMX) room).handler_requst_biao_yan(player, room_rq.getBiaoyan());

		return true;
	}

	private boolean handler_requst_location_new(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		final double pos_x = room_rq.getLocationInfor().getPosX();
		final double pos_y = room_rq.getLocationInfor().getPosY();

		if (pos_x == 0 || pos_y == 0) {
			logger.error("定位位置为0 pos_x:" + pos_x + ",pos_y:" + pos_y);
			return false;
		}

		Builder locationInfor = LocationInfor.newBuilder();
		locationInfor.setAddress(room_rq.getLocationInfor().getAddress());
		locationInfor.setPosX(pos_x);
		locationInfor.setPosY(pos_y);
		locationInfor.setTargetAccountId(player.getAccount_id());
		player.locationInfor = locationInfor.build();
		player.setLocation_time(System.currentTimeMillis());// 刷新定位时间
		boolean r = room.handler_requst_location_new(player, player.locationInfor);

		return true;
	}

	private boolean handler_requst_location(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (room == null) {
			return false;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			return false;
		}

		final double pos_x = room_rq.getLocationInfor().getPosX();
		final double pos_y = room_rq.getLocationInfor().getPosY();

		if (pos_x == 0 || pos_y == 0) {
			return false;
		}

		// long sysTime = System.currentTimeMillis();

		// LocationInfor oldlocationInfor = player.locationInfor;

		// if (!Global.checkBaiDuThread()) {
		// if (oldlocationInfor != null) {
		// room.handler_requst_location(player, player.locationInfor);
		// }
		// logger.error("定位队列太多丢弃请求");
		// return false;
		// }

		Builder locationInfor = LocationInfor.newBuilder();
		locationInfor.setAddress("");
		locationInfor.setPosX(pos_x);
		locationInfor.setPosY(pos_y);
		locationInfor.setTargetAccountId(player.getAccount_id());
		player.locationInfor = locationInfor.build();
		player.setLocation_time(System.currentTimeMillis());// 刷新定位时间
		boolean r = room.handler_requst_location(player, player.locationInfor);

		// Global.executeWithPositionThread(new PositionRunnable(room_id, pos_x,
		// pos_y, account_id, sysTime));

		return true;
	}

	private boolean handler_requst_open_less(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		boolean r = table.handler_requst_open_less(player, room_rq.getOpenThree());

		return true;
	}

	/**
	 * 允许少人模式扩展,支持指定人数
	 *
	 * @param room_id
	 * @param account_id
	 * @param room_rq
	 * @return
	 */
	private boolean handler_requst_open_less_extends(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		PermitLessRep rep = PBUtil.toObject(room_rq, PermitLessRep.class);
		if (rep == null) {
			logger.error("没有扩展数据结构");
			return false;
		}
		boolean r = table.handler_requst_open_less(player, rep.getPlayerNumber());

		return true;
	}

	// 托管
	private boolean handler_request_trustee(int room_id, long account_id, RoomRequest room_rq) {
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_rq.getRoomId());
		if (room == null) {
			logger.error("handler_request_trustee->no find room accountId:" + account_id + "|roomId=" + room_rq.getRoomId());
			return false;
		}

		Player player = room.getPlayer(account_id);
		if (player == null) {
			logger.error("handler_request_trustee->no find player accountId:" + account_id);
			return false;
		}
		// 逻辑处理
		boolean tResult = room.handler_request_trustee(player.get_seat_index(), room_rq.getIsTrustee(), room_rq.getTrusteeType());
		if (room_rq.getIsTrustee()) {
			player.operationAi();
		}
		return tResult;
	}

	private void handler_request_goods(int room_id, long account_id, RoomRequest room_rq) {

		Global.getLogicThreadPool().execute(new Runnable() {

			@Override
			public void run() {
				AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

				if (room == null) {
					return;
				}

				Player player = room.get_player(account_id);
				if (player == null) {
					return;
				}
				// long targetID = room_rq.getTargetAccountId();
				if ((!room_rq.hasTargetAccountId()) || room.get_player(room_rq.getTargetAccountId()) == null) {
					session.send(encode(MessageResponse.getMsgAllResponse(-1, "发送失败,目标玩家已离开房间!").build()));
					return;
				}
				// 门槛判断
				SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(5006);
				// 判断金币是否足够使用道具
				// 扣金币
				if (room_rq.hasGoodsID()) {
					int goodsid = room_rq.getGoodsID();
					GoodsModel gmodel = GoodsDict.getInstance().getGoodsModelByGameIdAndGoodsId(room.getGame_id(), goodsid);
					if (gmodel == null) {
						session.send(encode(MessageResponse.getMsgAllResponse(-1, "道具id非法").build()));
						return;
					}

					//
					if (room.is_sys()) {
						long limit_money = sysParamModel.getVal5().longValue() + gmodel.getMoney();
						if (player.getMoney() < limit_money) {
							session.send(encode(MessageResponse.getMsgAllResponse(-1, "您的金币不足，是否打开商城获取金币？!", ESysMsgType.MONEY_ERROR).build()));
							return;
						}
					}

					if (player.getMoney() < gmodel.getMoney()) {
						session.send(encode(MessageResponse.getMsgAllResponse(-1, "您的金币不足，是否打开商城获取金币？!", ESysMsgType.MONEY_ERROR).build()));
						return;
					}

					int coinRoomLimit = CoinService.INTANCE().getRoomToolsLimit(room, room.id);
					if (coinRoomLimit > 0 && player.getMoney() < coinRoomLimit) {
						session.send(encode(MessageResponse.getMsgAllResponse("持有金币大于等于" + coinRoomLimit + "金币才能使用道具").build()));
						return;
					}

					// StringBuilder buf = new StringBuilder();
					// buf.append("使用道具ID:" + goodsid).append("game_id:" +
					// table.getGame_id());

					String desc = String
							.format("使用道具ID:%d,game_id:%d,玩家id:%d,金币[-%d],金币值变化[%d]-[%d]", goodsid, room.getGame_id(), player.getAccount_id(),
									gmodel.getMoney(), player.getMoney(), player.getMoney() - gmodel.getMoney());

					boolean isSuccess = false;
					if (gmodel.getMoney() > 0) {
						try {
							ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
							AddMoneyResultModel addGoldResultModel = centerRMIServer
									.addAccountMoney(player.getAccount_id(), 0 - gmodel.getMoney(), false, desc, EMoneyOperateType.USE_PROP, goodsid);
							isSuccess = addGoldResultModel.isSuccess();
						} catch (Exception e) {
							isSuccess = false;
						}
					} else {
						isSuccess = true;
					}

					if (isSuccess == false) {
						session.send(encode(MessageResponse.getMsgAllResponse(-1, "您的金币不足，是否打开商城获取金币？!", ESysMsgType.MONEY_ERROR).build()));
						return;
					} else {
						// 逻辑处理
						player.setMoney(player.getMoney() - gmodel.getMoney());
						room.handler_request_goods(player.get_seat_index(), room_rq);
					}
				} else {
					session.send(encode(MessageResponse.getMsgAllResponse(-1, "无道具id").build()));
					return;
				}

			}
		});

	}

	private Request encode(Response res) {
		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.PROXY);
		requestBuilder.setProxId(topRequest.getProxId());
		requestBuilder.setProxSeesionId(topRequest.getProxSeesionId());
		// requestBuilder.setProxId(value);
		requestBuilder.setExtension(Protocol.response, res);
		return requestBuilder.build();
	}
}
