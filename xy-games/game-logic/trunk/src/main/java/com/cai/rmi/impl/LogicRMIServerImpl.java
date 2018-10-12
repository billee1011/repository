package com.cai.rmi.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.LogicRoomInfo;
import com.cai.common.domain.LogicStatusModel;
import com.cai.common.domain.RmiDTO;
import com.cai.common.domain.TestCardModel;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.util.DescParams;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.SystemConfig;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.game.AbstractRoom;
import com.cai.game.btz.BTZTable;
import com.cai.game.chdphz.CHDPHZTable;
import com.cai.game.dbn.DBNTable;
import com.cai.game.ddz.DDZTable;
import com.cai.game.fkn.FKNTable;
import com.cai.game.fls.FLSTable;
import com.cai.game.gxzp.GXZPTable;
import com.cai.game.hh.HHTable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.laopai.AbstractLPTable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.NewAbstractMjTable;
import com.cai.game.nn.NNTable;
import com.cai.game.pdk.PDKTable;
import com.cai.game.phu.PHTable;
import com.cai.game.qjqf.QJQFTable;
import com.cai.game.sdh.SDHTable;
import com.cai.game.sg.SGTable;
import com.cai.game.shisanzhang.SSZTable;
import com.cai.game.tdz.TDZTable;
import com.cai.game.wmq.WMQTable;
import com.cai.game.zjh.ZJHTable;
import com.cai.handler.LogicRoomHandler;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RMIHandlerServiceImp;
import com.cai.util.TestCardUti;

public class LogicRMIServerImpl implements ILogicRMIServer {

	@Override
	public String sayHello() {
		System.out.println("logic say hello");
		return "logic say hello";
	}

	@Override
	public Long getCurDate() {
		return System.currentTimeMillis();
	}

	public LogicStatusModel getLogicStatus() {
		LogicStatusModel model = new LogicStatusModel();
		model.setLogic_game_id(SystemConfig.logic_index);
		// int onlineCount = 0;
		// for (Map.Entry<Long, Player> entry :
		// PlayerServiceImpl.getInstance().getPlayerMap().entrySet()) {
		// if (entry.getValue().isOnline()) {
		// onlineCount++;
		// }
		// }
		model.setOnline_playe_num(PlayerServiceImpl.getInstance().getPlayerMap().size());
		model.setSocket_connect_num(1);
		model.setRoom_num(PlayerServiceImpl.getInstance().getRoomMap().size());

		// 消息处理情况
		model.setMsg_receive_count(RequestHandlerThreadPool.getInstance().getTpe().getTaskCount());
		model.setMsg_completed_count(RequestHandlerThreadPool.getInstance().getTpe().getCompletedTaskCount());
		model.setMsg_queue_count(RequestHandlerThreadPool.getInstance().getBlockQueue().size());

		return model;
	}

	/**
	 * 测试是否通
	 *
	 * @return
	 */
	public boolean test() {
		return true;
	}

	@Override
	public String testCard(String cards) {
		try {
			String[] bodys = StringUtils.split(cards, "#");
			if (bodys.length <= 0) {
				return "参数非法";
			}
			int roomID = Integer.parseInt(bodys[1]);
			int head = Integer.parseInt(bodys[2]);
			String cardList = bodys[3];
			String[] arrayStr = StringUtils.split(cardList, ",");
			int[] arrays = new int[arrayStr.length];
			for (int i = 0; i < arrays.length; i++) {
				arrays[i] = Integer.decode(arrayStr[i]);
			}
			int gameTypeIndex = 0;
			if (bodys.length >= 5) {
				gameTypeIndex = Integer.parseInt(bodys[4]);
			}

			if (bodys[0].equalsIgnoreCase("COIN")) { //金币场配牌

				if ((roomID == 1 || roomID == -1) && gameTypeIndex > 0) {
					//添加金币场配牌
					if (roomID == 1) {
						TestCardUti.testCard(gameTypeIndex, arrays);
					} else {
						TestCardUti.testCard(gameTypeIndex, null);
					}
					return "金币场配牌操作成功，新局生效," + cards;
				}
				return "金币场配牌操作失败," + cards;

			} else if (bodys[0].equalsIgnoreCase("CARD")) {
				AbstractRoom mjTable = PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				TestCardModel cardModel = new TestCardModel();
				cardModel.setGame_id(mjTable.getGame_id());
				cardModel.set_game_rule_index(mjTable.getGameRuleIndex());
				cardModel.set_game_type_index(mjTable.getGameTypeIndex());
				cardModel.setCards_param(cards);
				cardModel.setCreate_time(new Date());
				cardModel.setName(SysGameTypeDict.getInstance().getSysGameType(mjTable.getGameTypeIndex()).getDesc());
				cardModel.setDebug_my_cards(cardList);
				if (bodys.length > 0) {
					cardModel.setDesc(bodys[bodys.length - 1]);
				}
				MongoDBServiceImpl.getInstance().insert_Model(cardModel);
				return "操作成功" + cardList;

			} else if (bodys[0].equalsIgnoreCase("MJ")) {
				AbstractMJTable mjTable = (AbstractMJTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					if (mjTable._game_type_index == GameConstants.GAME_TYPE_MJ_XTDGK
							|| mjTable._game_type_index == GameConstants.GAME_TYPE_MJ_YING_JING) { // 幺筒断勾卡麻将
						if (arrayStr.length != 10) {
							return "牌型数量不对";
						}
					} else if (mjTable._game_type_index == GameConstants.GAME_TYPE_MJ_QUAN_ZHOU) { // 幺筒断勾卡麻将
						if (arrayStr.length != 16) {
							return "牌型数量不对";
						}
					} else if (is_fourteen_init_type(mjTable)) {
						if (arrayStr.length != 14) {
							return "牌型数量不对";
						}
					} else {
						if (arrayStr.length != 13) {
							return "牌型数量不对";
						}
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = arrays[0];
					break;
				case 5:
					if (arrays.length == 0) {
						return "牌堆数量不够";
					}
					mjTable.DEBUG_MAGIC_CARD = true;
					mjTable.magic_card_decidor = arrays[0];
					break;
				case 6:
					if (arrays.length == 0) {
						return "牌堆数量不够";
					}
					if (arrays.length != mjTable.getTablePlayerNumber() * 13) {
						return "牌堆数目不对，必须每人13张牌";
					}
					mjTable.debug_lsdy = true;
					mjTable.lsdy_debug_cards = arrays;
					break;
				case 7:
					if (arrays.length == 0) {
						return "牌堆数量不够";
					}
					if (arrays.length != 13) {
						return "牌堆数目不对，必须一人13张牌";
					}
					mjTable.debug_lsdy = true;
					mjTable.lsdy_debug_cards = arrays;
					break;
				case 8:
					if (arrays.length == 0) {
						return "牌堆数量不够";
					}
					mjTable.DEBUG_SPECIAL_CARD = true;
					mjTable.special_card_decidor = arrays[0];
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("FLS")) {
				FLSTable mjTable = (FLSTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = arrays[0];
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("FLST")) {
				FLSTable mjTable = (FLSTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				mjTable._game_type_index = GameConstants.GAME_TYPE_FLS_LX_TWENTY;
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = arrays[0];
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("HJK")) {
				HJKTable mjTable = (HJKTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("NN")) {
				NNTable mjTable = (NNTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 0;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("FKN")) {
				FKNTable mjTable = (FKNTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 0;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("DBN")) {
				DBNTable mjTable = (DBNTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 0;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("SG")) {
				SGTable mjTable = (SGTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 0;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("HH")) {
				HHTable mjTable = (HHTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("WMQ")) {
				WMQTable mjTable = (WMQTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("CHDPHZ")) {
				CHDPHZTable mjTable = (CHDPHZTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("GXZP")) {
				GXZPTable mjTable = (GXZPTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("PH")) {
				PHTable mjTable = (PHTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length <= 40) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count = 4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("PDK")) {

				PDKTable mjTable = (PDKTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}

				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("LAOPAI")) {
				AbstractLPTable mjTable = (AbstractLPTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					if (arrays.length > (mjTable._all_card_len - mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for (int i = 0; i < arrays.length; i++) {
						mjTable._repertory_card[mjTable._all_card_len - mjTable.GRR._left_card_count + i] = arrays[i];
					}
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("ZJH")) {
				ZJHTable mjTable = (ZJHTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("SSZ")) {
				SSZTable mjTable = (SSZTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("DDZ")) {
				DDZTable mjTable = (DDZTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				String di_cardList = "";
				if (bodys.length > 4) {
					di_cardList = bodys[4];
				}

				String[] di_arrayStr = StringUtils.split(di_cardList, ",");
				int[] di_arrays = new int[3];
				for (int i = 0; i < di_arrayStr.length; i++) {
					di_arrays[i] = Integer.decode(di_arrayStr[i]);
				}
				if (di_arrayStr.length == 0) {
					di_arrays = null;
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;

					break;
				case 3:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("QF")) {
				QJQFTable mjTable = (QJQFTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("BTZ")) {
				BTZTable mjTable = (BTZTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("TDZ")) {
				TDZTable mjTable = (TDZTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("SDH")) {
				SDHTable mjTable = (SDHTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 3:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				case 4:
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			} else if (bodys[0].equalsIgnoreCase("CHANGECARD")) {
				AbstractMJTable mjTable = (AbstractMJTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if (mjTable == null) {
					return "房间不存在";
				}
				switch (head) {
				case 1:
					if (arrayStr.length != 13) {
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE = true;
					mjTable.debug_my_cards = arrays;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功" + cardList;
			}

		} catch (Exception e) {
			String msg = e.getMessage();
			return msg;
		}
		return "非知指令";
	}

	@Override
	public boolean createRobotRoom(long accountID, int roomID, int game_type_index, int game_rule_index, int game_round, String nickName,
			String groupID, String groupName, int isInner) {
		return LogicRoomHandler
				.createRoomByBobot(accountID, roomID, game_type_index, game_rule_index, game_round, nickName, groupID, groupName, isInner);
	}

	@Override
	public boolean createRoomByBobotExtend(long accountID, int roomID, int game_type_index, int game_rule_index, int game_round, String nickName,
			String groupID, String groupName, int isInner, int exRule, int fanshu, int baseScore, int gangScore, int ciScore, int WcTimes) {
		return LogicRoomHandler
				.createRoomByBobotExtend(accountID, roomID, game_type_index, game_rule_index, game_round, nickName, groupID, groupName, isInner,
						exRule, fanshu, baseScore, gangScore, ciScore, WcTimes);
	}

	@Override
	public boolean createRoomByBobotExtend(long accountID, int roomID, int game_type_index, int game_rule_index, int game_round, String nickName,
			String groupID, String groupName, int isInner, int exRule, int fanshu, int baseScore, int gangScore, int ciScore) {
		return LogicRoomHandler
				.createRoomByBobotExtend(accountID, roomID, game_type_index, game_rule_index, game_round, nickName, groupID, groupName, isInner,
						exRule, fanshu, baseScore, gangScore, ciScore);
	}

	@Override
	public String getGameDesc(DescParams descParams) {
		if (descParams.game_rules.length == 1) {

			int[] new_rules = new int[2];
			new_rules[0] = descParams._game_rule_index;
			new_rules[1] = descParams.game_rules[0];
			descParams.game_rules = new_rules;
		}

		descParams.groupConfig = GameGroupRuleDict.getInstance().get(descParams._game_type_index);
		return GameDescUtil.getGameDesc(descParams);
	}

	@Override
	public RmiDTO getGameDescAndPeopleNumber(DescParams descParams) {
		RmiDTO dto = new RmiDTO();
		dto.setDesc(getGameDesc(descParams));
		dto.setValue(RoomComonUtil.getMaxNumber(descParams));
		return dto;
	}

	@Override
	public <T, R> R rmiInvoke(int cmd, T message) {
		Function<T, R> handler = RMIHandlerServiceImp.getInstance().getHandler(cmd);
		if (null != handler) {
			return handler.apply(message);
		}
		return null;
	}

	@Override
	public boolean createRoomByBobotExtend(long accountID, int roomID, int game_type_index, int game_round, String nickName, String groupID,
			String groupName, int isInner, Map<Integer, Integer> map) {
		return LogicRoomHandler.createRoomByBobotExtend(accountID, roomID, game_type_index, game_round, nickName, groupID, groupName, isInner, map);
	}

	/**
	 * 庄家起手14张的子麻将类型，而不是庄家起手13张。
	 *
	 * @param table
	 * @return
	 */
	private boolean is_fourteen_init_type(AbstractMJTable table) {
		if (table instanceof NewAbstractMjTable) {
			return true;
		}
		return false;
	}

	@Override
	public List<LogicRoomInfo> getLogicRoomInfos() {
		List<LogicRoomInfo> infos = new ArrayList<>(PlayerServiceImpl.getInstance().getRoomMap().size());
		for (AbstractRoom room : PlayerServiceImpl.getInstance().getRoomMap().values()) {
			LogicRoomInfo roomInfo = room.getLogicRoomInfo(false);
			if (roomInfo != null) {
				infos.add(roomInfo);
			}
		}
		return infos;
	}

	@Override
	public LogicRoomInfo getLogicRoomInfo(int roomID) {
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
		return room == null ? null : room.getLogicRoomInfo(true);
	}
}
