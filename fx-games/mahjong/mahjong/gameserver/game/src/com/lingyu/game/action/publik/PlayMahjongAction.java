/**
 *
 */
package com.lingyu.game.action.publik;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.message.GameAction;
import com.lingyu.common.message.GameMapping;
import com.lingyu.common.util.ConvertObjectUtil;
import com.lingyu.common.util.ObjectUtil;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.mahjong.MahjongConstant;
import com.lingyu.game.service.mahjong.MahjongManager;

@Controller
@GameAction(module = ModuleConstant.MODULE_PLAY_MAHJONG, group = SystemConstant.GROUP_PUBLIC)
public class PlayMahjongAction {

	@Autowired
	private RouteManager routeManager;
	@Autowired
	private MahjongManager mahjongManager;

	@GameMapping(value = MsgType.CREATE_MAHJONG_ROOM)
	public void createMahjongRoom(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] data = jsonArray.toArray();
		int jushu = ObjectUtil.obj2int(data[0]);
		int playType = ObjectUtil.obj2int(data[1]);
		int minimumHu = ObjectUtil.obj2int(data[2]);
		int fanLimit = ObjectUtil.obj2int(data[3]);
		int roomId = 0;

		JSONObject result = mahjongManager.createRoom(roleId, jushu, playType, roomId);
		routeManager.relayMsg(roleId, MsgType.CREATE_MAHJONG_ROOM, result);
	}

	@GameMapping(value = MsgType.JOIN_MAHJONG_MSG)
	public void joinMahjongRoom(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] data = jsonArray.toArray();
		int roomNum = ObjectUtil.obj2int(data[0]);

		JSONObject result = mahjongManager.joinRoom(roleId, roomNum);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.JOIN_MAHJONG_MSG, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_PLAY_MSG)
	public void play(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		int paiId = ObjectUtil.obj2int(objects[0]);
		JSONObject result = mahjongManager.play(roleId, paiId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_PLAY_MSG, result);
		}
	}

	@GameMapping(value = MsgType.DISSOLVED_ROOM_MSG)
	public void dissolvedRoom(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		JSONObject result = mahjongManager.dissolvedRoom(roleId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.DISSOLVED_ROOM_MSG, result);
		}
	}

	@GameMapping(value = MsgType.QUIT_ROOM_MSG)
	public void quitRoom(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		JSONObject result = mahjongManager.quitRoom(roleId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.QUIT_ROOM_MSG, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_CHESS_SIGN_MSG)
	public void sign(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		int signType = ObjectUtil.obj2int(objects[0]);
		JSONObject result = mahjongManager.sign(roleId, signType);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_CHESS_SIGN_MSG, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_START_GAME_MSG)
	public void startGame(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		JSONObject result = mahjongManager.startGame(roleId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_START_GAME_MSG, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_DISMISS_MSG)
	public void disMiss(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		JSONObject result = mahjongManager.disMiss(roleId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_DISMISS_MSG, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_DISMISS_OPERATE_MSG)
	public void disMissOperate(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		int type = ObjectUtil.obj2int(objects[0]);
		JSONObject result = mahjongManager.disMissOperate(roleId, type);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_DISMISS_OPERATE_MSG, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_EAT)
	public void eat(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		int firstPaiId = ObjectUtil.obj2int(objects[0]);
		int nextPaiId = ObjectUtil.obj2int(objects[1]);
		JSONObject result = mahjongManager.eat(roleId, firstPaiId, nextPaiId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_EAT, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_RETURN_HALL)
	public void returnHall(long roleId, JSONObject msg) {
		JSONObject result = mahjongManager.returnHall(roleId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_RETURN_HALL, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_RETURN_ROOM)
	public void returnRoom(long roleId, JSONObject msg) {
		JSONObject result = mahjongManager.returnRoom(roleId);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_RETURN_ROOM, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_ZHANJI_INFO)
	public void getZhanJiInfo(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		// int pageNum = ObjectUtil.obj2int(objects[0]);
		int pageNum = 1;
		JSONObject result = mahjongManager.getZhanJiInfo(roleId, pageNum);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_ZHANJI_INFO, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_ZHANJI_DETAILS_INFO)
	public void getZhanJiDetailsInfo(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		long id = ConvertObjectUtil.obj2long(msg.get("id"));
		JSONObject result = mahjongManager.getZhanJiDetailsInfo(roleId, id);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_ZHANJI_DETAILS_INFO, result);
		}
	}

	@GameMapping(value = MsgType.MAHJONG_ZHANJI_PLAYBACK)
	public void playBack(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		long id = ConvertObjectUtil.obj2long(msg.get("id"));
		int jushu = ConvertObjectUtil.object2int(msg.get("jushu"));
		JSONObject result = mahjongManager.playBack(roleId, id, jushu);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.MAHJONG_ZHANJI_PLAYBACK, result);
		}
	}
}