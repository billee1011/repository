/**
 *
 */
package com.cai.dictionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.Symbol;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.domain.coin.CoinGame;
import com.cai.common.domain.coin.CoinGameDetail;
import com.cai.common.domain.coin.CoinGameType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.common.util.StringUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.coin.CoinProtocol;
import protobuf.clazz.coin.CoinProtocol.ConfigRefreshResponse;
import protobuf.clazz.coin.CoinProtocol.GameDescResponse;
import protobuf.clazz.coin.CoinProtocol.GameDetailMsg;
import protobuf.clazz.coin.CoinProtocol.GameDetailResponse;
import protobuf.clazz.coin.CoinProtocol.GameListResponse;
import protobuf.clazz.coin.CoinProtocol.GameMsg;
import protobuf.clazz.coin.CoinProtocol.GameRuleMsg;
import protobuf.clazz.coin.CoinProtocol.GameTypeListResponse;
import protobuf.clazz.coin.CoinProtocol.GameTypeMsg;
import protobuf.clazz.coin.CoinProtocol.OneGameDetailResponse;
import protobuf.clazz.coin.CoinProtocol.OneGameRuleResponse;

/**
 * 扣豆描述
 *
 * @author tang
 */
public class CoinDict {

	private Logger logger = LoggerFactory.getLogger(CoinDict.class);
	private static CoinDict coinDict = new CoinDict();

	private CoinDict() {
	}

	private Response gameType;
	private Map<Integer, Response> games = new LinkedHashMap<>();
	private Map<Integer, Response> gameDetails = new LinkedHashMap<>();
	private Map<Integer, Response> gameDetail = new LinkedHashMap<>();
	private Map<Integer, Response> gameDetailRule = new LinkedHashMap<>();
	private Map<Integer, Response> gameDesc = new LinkedHashMap<>();

	private Map<Integer, List<CoinGameDetail>> gameTypeDetailMap = new LinkedHashMap<>();

	public static CoinDict getInstance() {
		return coinDict;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();

		RedisService redisService = SpringService.getBean(RedisService.class);
		List<CoinGameType> gameTypeList = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_COIN_GAME_TYPE, ArrayList.class);

		GameTypeListResponse.Builder gameTypeResp = GameTypeListResponse.newBuilder();
		GameTypeMsg.Builder gameTypeMsg = null;

		if (gameTypeList != null) {
			for (CoinGameType gameType : gameTypeList) {
				if (gameType.getState() == 2) {
					continue;
				}
				gameTypeMsg = GameTypeMsg.newBuilder();

				gameTypeMsg.setGameTypeId(gameType.getGame_big_type_id());
				gameTypeMsg.setStatus(gameType.getState());
				gameTypeMsg.setSortIndex(gameType.getGame_big_type_sort());

				gameTypeResp.addMsgs(gameTypeMsg.build());
			}
		}
		gameType = PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_TYPE_LIST, gameTypeResp).build();

		Map<Integer, List<CoinGame>> gameTypeIndexMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_COIN_GAMES, LinkedHashMap.class);
		if (gameTypeIndexMap != null) {
			Map<Integer, Response> tempGameDesc = new LinkedHashMap<>();
			Map<Integer, Response> tempGameTypeIndexs = new LinkedHashMap<>();
			GameListResponse.Builder gamesResp = null;
			GameMsg.Builder gameMsg = null;
			GameDescResponse.Builder gameDescResp = null;
			for (int gameType : gameTypeIndexMap.keySet()) {
				gamesResp = GameListResponse.newBuilder();
				List<CoinGame> gameList = gameTypeIndexMap.get(gameType);
				for (CoinGame gameTypeIndex : gameList) {
					if (gameTypeIndex.getState() == 2) {
						continue;
					}
					gameMsg = GameMsg.newBuilder();
					gameDescResp = GameDescResponse.newBuilder();

					gameDescResp.setGameId(gameTypeIndex.getGame_type_index());
					gameDescResp.setGameDesc(gameTypeIndex.getGame_desc());

					gameMsg.setGameTypeId(gameTypeIndex.getGame_big_type_id());
					gameMsg.setGameId(gameTypeIndex.getGame_type_index());
					gameMsg.setName(gameTypeIndex.getGame_name());
					gameMsg.setStatus(gameTypeIndex.getState());
					gameMsg.setSortIndex(gameTypeIndex.getGame_sort());
					gameMsg.setAppId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(gameTypeIndex.getGame_type_index()));

					gamesResp.addMsgs(gameMsg.build());
					tempGameDesc.put(gameTypeIndex.getGame_type_index(), PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_DESC, gameDescResp).build());
				}
				tempGameTypeIndexs.put(gameType, PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_LIST, gamesResp).build());
			}
			games = tempGameTypeIndexs;
			gameDesc = tempGameDesc;
		}

		Map<Integer, List<CoinGameDetail>> tempGameDetailMap = redisService
				.hGet(RedisConstant.DICT, RedisConstant.DICT_COIN_GAME_DETAILS, LinkedHashMap.class);
		if (tempGameDetailMap != null) {
			Map<Integer, Response> tempGameTypeDetails = new LinkedHashMap<>();
			Map<Integer, Response> tempGameTypeDetail = new LinkedHashMap<>();
			Map<Integer, Response> tempGameDetailRule = new LinkedHashMap<>();
			GameDetailResponse.Builder detailResp = null;
			GameDetailMsg.Builder detailMsg = null;
			OneGameDetailResponse.Builder oneDetailResp = null;
			OneGameRuleResponse.Builder ruleResp = null;
			for (int gameId : tempGameDetailMap.keySet()) {
				detailResp = GameDetailResponse.newBuilder();
				List<CoinGameDetail> detailList = tempGameDetailMap.get(gameId);
				for (CoinGameDetail detail : detailList) {
					if (detail.getState() == 2) {
						continue;
					}
					oneDetailResp = OneGameDetailResponse.newBuilder();
					detailMsg = GameDetailMsg.newBuilder();
					ruleResp = OneGameRuleResponse.newBuilder();
					detailMsg.setDetailId(detail.getId());
					detailMsg.setGameId(detail.getGame_type_index());
					detailMsg.setName(detail.getRound_level_name());
					detailMsg.setLevel(detail.getRound_level());
					detailMsg.setSortIndex(detail.getRound_sort());
					detailMsg.setBaseAntes(detail.getBase_antes());
					detailMsg.setBaseCoin(detail.getBase_coin());
					detailMsg.setMinCoin(detail.getMin_coin());
					detailMsg.setMaxCoin(detail.getMax_coin());
					detailMsg.setMinGameCoin(detail.getLower_limit());
					detailMsg.setMaxGameCoin(detail.getUpper_limit());
					detailMsg.setPayCoin(detail.getGold_price());
					detailMsg.setStatus(detail.getState());
					detailMsg.setIcon(detail.getRound_icon());
					detailMsg.setConvertCoin(detail.getGold_price());
					detailMsg.setConvertMoney(detail.getGold_price());
					detailMsg.setMatchCountDown(detail.getCount_down_time());
					detailMsg.setCornucopiaCost(detail.getCornucopia_cost());
					detailMsg.setHasCornucopia(StringUtils.isNotEmpty(detail.getCornucopia_condition_id()));

					//by wu 刺激场
					List<Integer> cdtIds = StringUtil.toIntList(detail.getExcite_condition_id(), Symbol.COMMA);
					if (null != cdtIds && !cdtIds.isEmpty()) {
						detailMsg.setExciteProto(toExciteProto(cdtIds));
					}
					if (StringUtils.isNotEmpty(detail.getCorner_icon())) {
						detailMsg.setSuperscriptIcon(detail.getCorner_icon());
					}
					detailResp.addMsgs(detailMsg.build());
					oneDetailResp.setMsg(detailMsg.build());
					ruleResp.addAllRuleMsgs(getRuleMsgs(detail));

					tempGameTypeDetail.put(detail.getId(), PBUtil.toS2CCommonRsp(C2SCmd.COIN_ONE_GAME_DETAIL, oneDetailResp).build());
					tempGameDetailRule.put(detail.getId(), PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_RULE, ruleResp).build());
				}
				tempGameTypeDetails.put(gameId, PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_DETAIL, detailResp).build());
			}
			gameDetails = tempGameTypeDetails;
			gameDetail = tempGameTypeDetail;
			gameTypeDetailMap = tempGameDetailMap;
			gameDetailRule = tempGameDetailRule;
		}

		// 通知玩家重新加载
		ConfigRefreshResponse.Builder refreshResp = ConfigRefreshResponse.newBuilder();
		Response refreshResponse = PBUtil.toS2CCommonRsp(C2SCmd.COIN_CONFIG_REFRESH, refreshResp).build();
		C2SSessionService.getInstance().getAllOnlieSession().forEach((session) -> {
			session.send(refreshResponse);
		});

		logger.info("load-> coin cache load success time:{} !!", timer.getStr());
	}

	private List<GameRuleMsg> getRuleMsgs(CoinGameDetail detail) {
		List<GameRuleMsg> list = new ArrayList<>();
		Map<Integer, Integer> rMap = detail.getRuleParam().getMap();
		GameRuleMsg.Builder msg = null;
		for (int rId : rMap.keySet()) {
			msg = GameRuleMsg.newBuilder();
			msg.setRuleId(rId);
			msg.setValue(rMap.get(rId));

			list.add(msg.build());
		}
		return list;
	}

	public Response getGameTypeResp() {
		return gameType;
	}

	public Response getGameListResp(int gameType) {
		Response response = games.get(gameType);
		if (response == null) {
			GameListResponse.Builder gamesResp = GameListResponse.newBuilder();
			response = PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_LIST, gamesResp).build();
		}
		return response;
	}

	public Response getGameDetailResp(int gameId) {
		Response response = gameDetails.get(gameId);
		if (response == null) {
			logger.error("getGameDetailResp-> no find game detail list response gameId:{} !!", gameId);
		}
		return response;
	}

	public Response getGameRuleResp(int detailId) {
		Response response = gameDetailRule.get(detailId);
		if (response == null) {
			logger.error("getGameRuleResp-> no find game rule response detailId:{} !!", detailId);
		}
		return response;
	}

	public Response getGameDetailRespById(int detailId) {
		Response response = gameDetail.get(detailId);
		if (response == null) {
			logger.error("getGameDetailRespById-> no find game detail response detailId:{} !!", detailId);
		}
		return response;
	}

	public Response getGameDescRespById(int gameId) {
		Response response = gameDesc.get(gameId);
		if (response == null) {
			logger.error("getGameDetailRespById-> no find game desc response gameId:{} !!", gameId);
		}
		return response;
	}

	public CoinGameDetail getGameDetail(int gameId, int id) {
		List<CoinGameDetail> list = getGameDetails(gameId);
		if (list == null) {
			return null;
		}
		for (CoinGameDetail detail : list) {
			if (detail.getId() == id) {
				return detail;
			}
		}
		logger.error("getGameDetail-> no find gameTypeDetail gameId:{} id:{} !!", gameId, id);
		return null;
	}

	public List<CoinGameDetail> getGameDetails(int gameId) {
		List<CoinGameDetail> list = gameTypeDetailMap.get(gameId);
		if (list == null) {
			logger.error("getGameDetails-> no find game details gameId:{} !!", gameId);
		}
		return list;
	}

	/**
	 * @param cdtIds 条件id列表
	 * @return
	 */
	public final CoinProtocol.ExciteConditionGroupProto.Builder toExciteProto(List<Integer> cdtIds) {
		List<CoinProtocol.ExciteConditionProto> list = Lists.newArrayList();
		cdtIds.forEach(id -> {
			CoinExciteModel model = CoinExciteDict.getInstance().getExciteModel(id);
			if (null != model) {
				CoinProtocol.ExciteConditionProto.Builder b = CoinProtocol.ExciteConditionProto.newBuilder();
				b.setId(id);
				b.setOutput(model.getOutput());
				b.setType(model.getId() == 1 ? 1 : 2); //ps，此处是标记为余牌类型或者牌型类型，并不是起手类型
				b.setVar1(model.getVar1());
				b.setVar2(model.getVar2());
				list.add(b.build());
			} else {
				logger.error("toExciteProto exciteModel is null , id:{}", id);
			}
		});

		CoinProtocol.ExciteConditionGroupProto.Builder builder = CoinProtocol.ExciteConditionGroupProto.newBuilder();
		builder.addAllCdtGroup(list);
		return builder;
	}

}
