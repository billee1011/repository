package com.cai.http.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGameType;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.LogicRoomInfo;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.RoomDetailJsonModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.http.FastJsonJsonView;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicServiceImpl;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Controller
@RequestMapping("/room")
public class RoomController {
	public static final int FAIL = -1;

	public static final int SUCCESS = 1;

	private static final RoomDetailJsonModel EMPTY_ROOM = new RoomDetailJsonModel();

	public static final LoadingCache<Integer, RoomDetailJsonModel> CACHE = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterAccess(5, TimeUnit.SECONDS).recordStats().build(new CacheLoader<Integer, RoomDetailJsonModel>() {
				@Override
				public RoomDetailJsonModel load(Integer key) {
					
					SysParamModel model =SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(20000);
					if(model!=null&& model.getVal1()==1) {
						LogicRoomInfo logicRoomInfo = SpringService.getBean(ICenterRMIServer.class).getLogicRoomInfo(key);
						if(logicRoomInfo == null) {
							return EMPTY_ROOM;
						}
						List<String> headPics = new ArrayList<String>();
						
						List<String> names = new ArrayList<String>();
						for (int accountId : logicRoomInfo.getPlayerIDs()) {
							if (accountId > 0) {
								AccountSimple accountsimple = PublicServiceImpl.getInstance().getAccountSimpe(accountId);
								if (accountsimple != null) {
									headPics.add(accountsimple.getIcon());
									names.add(accountsimple.getNick_name());
								}
									
							}
						}
						int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(logicRoomInfo.get_game_type_index());
						RoomDetailJsonModel roomDetail = new RoomDetailJsonModel();
						roomDetail.setCurRound(logicRoomInfo.getCurRound());
						roomDetail.setGame_type_index(logicRoomInfo.get_game_type_index());
						roomDetail.setGameDesc(logicRoomInfo.getGameDesc());
						roomDetail.setGameSubName(SysGameTypeDict.getInstance().getMJname(logicRoomInfo.get_game_type_index()));
						roomDetail.setGameName(EGameType.getEGameType(gameId).getName());
						roomDetail.setGame_id(gameId);
						roomDetail.setClub_id(logicRoomInfo.getClubId());
						roomDetail.setClub_name(logicRoomInfo.getClubName());
						roomDetail.setPlayers(names);
						roomDetail.setRoomStatus(logicRoomInfo.getRoomStatus());
						roomDetail.setHeadPics(headPics);
						AccountSimple createAccount = PublicServiceImpl.getInstance().getAccountSimpe(logicRoomInfo.getCreateID());
						if (createAccount != null) {
							roomDetail.setNickName(createAccount.getNick_name());
							roomDetail.setCreateHeadPic(createAccount.getIcon());
						}
						roomDetail.setCreateAccountId(logicRoomInfo.getCreateID());
						
						return  roomDetail;
					}
					
					
					
					RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, key + "",
							RoomRedisModel.class);
					if (roomRedisModel == null) {
						return EMPTY_ROOM;
					}
					List<String> headPics = new ArrayList<String>();
					for (Long accountId : roomRedisModel.getPlayersIdSet()) {
						if (accountId > 0) {
							AccountSimple accountsimple = PublicServiceImpl.getInstance().getAccountSimpe(accountId);
							if (accountsimple != null)
								headPics.add(accountsimple.getIcon());
						}
					}
					int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(roomRedisModel.getGame_type_index());
					RoomDetailJsonModel roomDetail = new RoomDetailJsonModel();
					roomDetail.setCurRound(roomRedisModel.getGame_round());
					roomDetail.setGame_type_index(roomRedisModel.getGame_type_index());
					roomDetail.setGameDesc(roomRedisModel.getGameRuleDes());
					roomDetail.setGameSubName(SysGameTypeDict.getInstance().getMJname(roomRedisModel.getGame_type_index()));
					roomDetail.setGameName(EGameType.getEGameType(gameId).getName());
					roomDetail.setGame_id(gameId);
					roomDetail.setClub_id(roomRedisModel.getClub_id());
					roomDetail.setClub_name(roomRedisModel.getClubName());
					roomDetail.setPlayers(roomRedisModel.getNames());
					roomDetail.setRoomStatus(roomRedisModel.isStart() ? 1 : 0);
					roomDetail.setHeadPics(headPics);
					AccountSimple createAccount = PublicServiceImpl.getInstance().getAccountSimpe(roomRedisModel.getCreate_account_id());
					if (createAccount != null) {
						roomDetail.setNickName(createAccount.getNick_name());
						roomDetail.setCreateHeadPic(createAccount.getIcon());
					}
					roomDetail.setCreateAccountId(roomRedisModel.getCreate_account_id());
					return roomDetail;
				}
			});

	@RequestMapping("/detail")
	public ModelAndView handle(HttpServletRequest request, @RequestParam(value = "roomId", defaultValue = "0") int roomId,
			HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");

		Map<String, Object> map = new HashMap<String, Object>();

		if (roomId <= 0) {
			map.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), map);
		}

		try {
			RoomDetailJsonModel room = CACHE.get(roomId);
			if (room == EMPTY_ROOM) {
				map.put("result", FAIL);
			} else {
				map.put("data", room);
				map.put("result", SUCCESS);
			}

		} catch (ExecutionException e) {
			e.printStackTrace();
			map.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), map);
		}

		return new ModelAndView(new FastJsonJsonView(), map);
	}

}
