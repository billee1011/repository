package com.cai.handler;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GoodsModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.SysParamDict;
import com.cai.net.core.ClientHandler;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.util.MessageResponse;

import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.LocationInfor.Builder;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.RoomRequest;

/**
 * 房间处理
 * 
 * @author run
 *
 */
public class RoomHandler extends ClientHandler<RoomRequest> {

	private static Logger logger = Logger.getLogger(RoomHandler.class);

	@Override
	public void onRequest() throws Exception {

		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(topRequest.getProxyAccountId());
		if (player == null)
			return;
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(player.getRoom_id());
		int type = request.getType();

		if (type == MsgConstants.REQUST_PLAYER_RELEASE_ROOM) {
			handler_release_room(player.getAccount_id(), request);
			return;
		}

		// 代理可以没房间
		if (room == null) {
			return;
		}

		ReentrantLock lock = room.getRoomLock();

		try {
			lock.lock();
			// 刷新时间
			room.process_flush_time();

			// MJTable table =
			// (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room.getRoom_id());

			// table.get_state_machine().on_message(player.getAccount_id(),
			// request);
			if (type == MsgConstants.REQUST_PLAYER_READY) {
				// 准备
				handler_player_ready(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_OUT_CARD) {
				handler_player_out_card(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_OPERATE) {
				handler_player_operate_card(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_RELEASE_ROOM) {
				handler_release_room(player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PLAYER_BE_IN_ROOM) {
				handler_requst_player_be_in_room(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_AUDIO_CHAT) {
				handler_requst_audio_chat(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_EMJOY_CHAT) {
				handler_requst_emjoy_chat(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PAO_QIANG) {
				handler_requst_pao_qiang(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_LOCATION) {
				handler_requst_location(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_NAO_ZHUANG) {
				handler_requst_nao_zhuang(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_OPEN_LESS) {
				handler_requst_open_less(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_PROXY_RELEASE_ROOM) {
				handler_release_room(player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_IS_TRUSTEE) {
				handler_request_trustee(room.getRoom_id(), player.getAccount_id(), request);
			} else if (type == MsgConstants.REQUST_GOODS) {
				handler_request_goods(room.getRoom_id(), player.getAccount_id(), request);
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
			} else if (type == MsgConstants.REQUST_CALL_QIANG_ZHUANG) {
				handler_requst_call_qiang_zhuang(room.getRoom_id(), player.getAccount_id(), request);
			}

		} finally {
			lock.unlock();
		}

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
				room_rq.getBOutCardType());
	}

	// 叫地主抢地主
	private boolean handler_requst_call_qiang_zhuang(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}
		// 逻辑处理
		return table.handler_requst_call_qiang_zhuang(player.get_seat_index(), room_rq.getSelectCallBanker(), room_rq.getSelectQiangBanker());
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
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

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
			return table.handler_player_ready(player.get_seat_index(), room_rq.getIsCancelReady());
		}

	}

	private boolean handler_requst_player_be_in_room(int room_id, long account_id, RoomRequest room_rq) {

		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			player = table.observers().getPlayer(account_id);
			if (null != player) {
				return table.handler_observer_be_in_room(player);
			}
			return false;
		}

		// 逻辑处理
		return table.handler_player_be_in_room(player.get_seat_index());
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

		// 逻辑处理
		return table.handler_operate_card(player.get_seat_index(), room_rq.getOperateCode(), room_rq.getOperateCard(), room_rq.getLuoCode());
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

			// 1 围观者请求离开房间
			if (room_rq.getOperateCode() == GameConstants.Release_Room_Type_OBSERVER) {
				Player player = table.observers().getPlayer(account_id);
				if (null != player) {
					return table.handler_exit_room_observer(player);
				}
				return false;
			}

			// 2房间内的玩家请求离开房间
			Player player = table.get_player(account_id);
			if ((player == null) && (account_id != table.getRoom_owner_account_id())) {
				return false;
			}

			// 逻辑处理
			boolean r = table.handler_release_room(player, room_rq.getOperateCode());
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
		Global.getService("").execute(new Runnable() {

			@Override
			public void run() {

				// LocationInfor oldlocationInfor = player.locationInfor;
				// if (oldlocationInfor != null) {
				// if (oldlocationInfor.getPosX() !=
				// room_rq.getLocationInfor().getPosX()) {
				// boolean r = room.handler_requst_location(player,
				// player.locationInfor);
				// return;
				// }
				// }
				if (pos_x == 0 || pos_y == 0) {
					return;
				}

				LocationInfor oldlocationInfor = player.locationInfor;
				if (oldlocationInfor != null) {
					if (Math.abs(oldlocationInfor.getPosX() - pos_x) < 0.001) {
						room.handler_requst_location(player, player.locationInfor);
						return;
					}
				}

				int game_id = room == null ? GameConstants.GAME_ID_FLS_LX : room.getGame_id();
				game_id = game_id == 0 ? GameConstants.GAME_ID_FLS_LX : game_id;

				String position = "";
				int random = RandomUtil.generateRandomNumber(0, 100);
				SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5005);

				if (random > 50 || org.apache.commons.lang.StringUtils.isEmpty(sysParamModel.getStr2()) || sysParamModel.getStr2().equals("0")) {
					position = PtAPIServiceImpl.getInstance().getbaiduPosition(game_id, pos_x, pos_y);
				} else {
					position = PtAPIServiceImpl.getInstance().getTengXunPosition(game_id, pos_x, pos_y);
				}

				Builder locationInfor = LocationInfor.newBuilder();
				locationInfor.setAddress(position);
				locationInfor.setPosX(pos_x);
				locationInfor.setPosY(pos_y);
				locationInfor.setTargetAccountId(player.getAccount_id());

				player.locationInfor = locationInfor.build();

				if (room != null) {
					boolean r = room.handler_requst_location(player, player.locationInfor);
				}

			}
		});

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

	// 托管
	private boolean handler_request_trustee(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		// 逻辑处理
		return table.handler_request_trustee(player.get_seat_index(), room_rq.getIsTrustee());
	}

	private boolean handler_request_goods(int room_id, long account_id, RoomRequest room_rq) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		Player player = table.get_player(account_id);
		if (player == null) {
			return false;
		}

		// long targetID = room_rq.getTargetAccountId();
		if ((!room_rq.hasTargetAccountId()) || table.get_player(room_rq.getTargetAccountId()) == null) {
			send(MessageResponse.getMsgAllResponse(-1, "发送失败,目标玩家已离开房间!").build());
			return false;
		}
		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id()).get(5006);
		// 判断金币是否足够使用道具
		// 扣金币
		if (room_rq.hasGoodsID()) {
			int goodsid = room_rq.getGoodsID();
			GoodsModel gmodel = GoodsDict.getInstance().getGoodsModelByGameIdAndGoodsId(table.getGame_id(), goodsid);
			if (gmodel == null) {
				send(MessageResponse.getMsgAllResponse(-1, "道具id非法").build());
				return false;
			}

			//
			long limit_money = sysParamModel.getVal5().longValue() + gmodel.getMoney();
			if (player.getMoney() < limit_money) {
				send(MessageResponse.getMsgAllResponse(-1, "金币必须大于" + limit_money + "才能够使用该道具!").build());
				return false;
			}

			// StringBuilder buf = new StringBuilder();
			// buf.append("使用道具ID:" + goodsid).append("game_id:" +
			// table.getGame_id());

			String desc = String.format("使用道具ID:%d,game_id:%d,玩家id:%d,金币[-%d],金币值变化[%d]-[%d]", goodsid, table.getGame_id(), player.getAccount_id(),
					gmodel.getMoney(), player.getMoney(), player.getMoney() - gmodel.getMoney());

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(player.getAccount_id(), 0 - gmodel.getMoney(), false, desc,
					EMoneyOperateType.USE_PROP);
			if (addGoldResultModel.isSuccess() == false) {
				send(MessageResponse.getMsgAllResponse(-1, "金币不足!").build());
				return false;
			} else {
				// 逻辑处理
				player.setMoney(player.getMoney() - gmodel.getMoney());
				return table.handler_request_goods(player.get_seat_index(), room_rq);
			}
		} else {
			send(MessageResponse.getMsgAllResponse(-1, "无道具id").build());
			return false;
		}

	}
}
