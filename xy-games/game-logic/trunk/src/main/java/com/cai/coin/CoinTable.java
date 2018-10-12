package com.cai.coin;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cai.coin.excite.condition.ExciteConditionGroup;
import com.cai.coin.excite.condition.IExciteCondition;
import com.cai.common.constant.ActivityMissionTypeEnum;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ETriggerType;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.domain.coin.CoinGameDetail;
import com.cai.common.domain.json.MatchBaseScoreJsonModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.thread.HandleMessageExecutorPool;
import com.cai.common.util.DescParams;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.StringUtil;
import com.cai.core.SystemConfig;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.CoinExciteDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.game.AbstractRoom;
import com.cai.handler.LogicRoomHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.FoundationService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RobotService;
import com.cai.service.RobotService.RobotRandom;
import com.cai.service.SessionServiceImpl;
import com.cai.tasks.CoinSenderEnsureTask;
import com.cai.tasks.CoinGameStartTask;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.BaseS2S.SendToClientsProto2;
import protobuf.clazz.Common.CommonLI;
import protobuf.clazz.coin.CoinProtocol;
import protobuf.clazz.coin.CoinProtocol.GameOverResponse;
import protobuf.clazz.coin.CoinProtocol.GamePlayerMsg;
import protobuf.clazz.coin.CoinProtocol.GameStartResponse;
import protobuf.clazz.coin.CoinServerProtocol.CoinPlayerMsg;
import protobuf.clazz.coin.CoinServerProtocol.CoinPlayerProto;
import protobuf.clazz.coin.CoinServerProtocol.CornucopiaRecycleCoinProto;
import protobuf.clazz.coin.CoinServerProtocol.S2SCoinMsgStat;

import static protobuf.clazz.coin.CoinServerProtocol.CornucopiaAwardProto;

public class CoinTable implements Consumer<DataWrap> {

	private static final Logger logger = LoggerFactory.getLogger(CoinTable.class);
	public static final int START_TIME = 1;
	public static final int COST = 1; //扣除费用
	public static final int BACK = 2; //返回费用
	private final int gameId;
	private int gameTypeIndex;
	private final int detailId;
	private int coinIndex;
	private boolean isStart;
	private List<Long> accountIds;
	private List<CoinPlayer> players = new ArrayList<>(4);
	private HandleMessageExecutorPool executor;
	private RobotRandom robotRandom;
	private AbstractRoom gameRoom;
	private int coinRoomId;
	private long gameStartTime;

	//结算
	private Map<Long, CoinPlayer> winMap = new LinkedHashMap<>(4);
	private Map<Long, CoinPlayer> loseMap = new LinkedHashMap<>(4);
	private int sumCoin = 0;

	//table相关配置
	private final CoinGameDetail gameDetail;
	//刺激场条件
	private final List<Integer> exciteCdtIds;
	//聚宝盆条件
	private final List<Integer> cornucopiaCdtIds;

	/**
	 * @param coinIndex 牌桌内存索引
	 * @param executor  执行器
	 */
	public CoinTable(int coinIndex, HandleMessageExecutorPool executor, CoinGameDetail gameDetail) {
		this.gameTypeIndex = gameDetail.getGame_type_index();
		this.coinIndex = coinIndex;
		this.executor = executor;
		this.gameDetail = gameDetail;
		this.detailId = gameDetail.getId();
		this.gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(this.gameTypeIndex);

		//add by wu_hc 2018-08-13,金币场-刺激场相关
		this.exciteCdtIds = StringUtil.toIntList(gameDetail.getExcite_condition_id(), Symbol.COMMA);
		this.cornucopiaCdtIds = StringUtil.toIntList(gameDetail.getCornucopia_condition_id(), Symbol.COMMA);

	}

	public void gameUpdate() {
		if (gameRoom == null) {
			return;
		}

	}

	public int gameOver(AbstractRoom room) {
		List<CoinPlayer> playerList = GameRoundOver();
		GameOverSettle(playerList);

		CoinPlayer player = null;
		for (long accountId : accountIds) {
			player = getCoinPlayer(accountId);
			if (player != null && !player.isRobot()) {
				player.reset();
				PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			}
		}
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hDelListByLong(RedisConstant.COIN_PLAYER_INFO, accountIds);

		SessionServiceImpl.getInstance()
				.sendToCoin(SystemConfig.connectCoin, PBUtil.toS2SRequet(C2SCmd.COIN_MSG_STAT, getMsgStat(getDetail(), playerList)).build());
		gameRoom = null;
		return coinIndex;
	}

	private S2SCoinMsgStat getMsgStat(CoinGameDetail detail, List<CoinPlayer> playerList) {
		S2SCoinMsgStat.Builder resp = S2SCoinMsgStat.newBuilder();
		resp.setDetailId(detail.getId());
		resp.setGameId(detail.getGame_type_index());
		resp.setPlayTime((int) ((System.currentTimeMillis() - gameStartTime) / 1000));
		Date nowDate = new Date();
		CoinPlayerMsg.Builder msg = null;
		for (CoinPlayer player : playerList) {
			msg = CoinPlayerMsg.newBuilder();
			msg.setAccountId(player.getAccount_id());
			msg.setIsRobot(player.isRobot());
			msg.setWinCoin(player.getResultWinCoin2() - player.getRecycleCoin()); //剔出回收部分金币
			msg.setIsBankrupt(player.isCoinBankruptcy());
			msg.setBaseCoin(player.getBaseCoin());
			msg.setPlayTime(player.getCoinPlayTime());
			msg.setCreateTime(player.getCreateTime());
			msg.setNowTime(nowDate.getTime());
			msg.setCornucopia(player.isCornucopia());
			msg.setRecycleCoin(player.getRecycleCoin());
			if(player.isCornucopia()){
				msg.setCornucopiaCost(gameDetail.getCornucopia_cost());
			}
			msg.addAllCardTypeValue(player.getConditionGroup().toCardCategoryValueList());
			resp.addMsgs(msg.build());
		}
		return resp.build();
	}

	private List<CoinPlayer> GameRoundOver() {
		List<CoinPlayer> settleList = new ArrayList<>();
		int winCoin = 0;
		CoinPlayer coinPlayer = null;
		long accountId = 0;
		int sumWinCoin = 0;
		int sumLoseCoin = 0;
		int baseAnte = getDetail().getBase_antes();

		int length = gameRoom._player_result.game_score.length;
		for (int i = 0; i < length; i++) {
			Player player = gameRoom.get_players()[i];
			if (player == null) {
				continue;
			}

			accountId = player.getAccount_id();

			coinPlayer = (CoinPlayer) player;
			settleList.add(coinPlayer);
			//int exciteOutput = coinPlayer.getExciteMultiple();
			//logger.error("coin table[{}],excite date,player:{} output:{}", getCoinIndex(), accountId, exciteOutput);

			//GAME-TODO 需要算上特殊玩法带来的额外产出 [ 结算举例：最终分=结算分*底分*倍率 ] by wu_hc
			winCoin = (int) gameRoom._player_result.game_score[i];//* baseAnte * (exciteOutput == 0 ? 1 : exciteOutput); //new

			winCoin = coinPlayer.setWinCoin(winCoin);

			if (winCoin > 0) {
				winMap.put(accountId, coinPlayer);
				sumWinCoin += winCoin;
			} else if (winCoin < 0) {
				loseMap.put(accountId, coinPlayer);
				sumLoseCoin += winCoin;
			}
		}

		int aSumLoseCoin = Math.abs(sumLoseCoin);
		sumCoin = aSumLoseCoin > sumWinCoin ? sumWinCoin : aSumLoseCoin;

		handleResultWinCoin();

		//聚宝盆
		for (int i = 0; i < length; i++) {
			Player player = gameRoom.get_players()[i];
			if (player == null) {
				continue;
			}

			accountId = player.getAccount_id();

			coinPlayer = (CoinPlayer) player;
			winCoin = coinPlayer.getResultWinCoin2();

			//赢的，场次有聚宝盆玩法, PS:产品要求抽水,这里不需要区分是否机器人
			if (winCoin > 0 && StringUtils.isNotEmpty(gameDetail.getCornucopia_condition_id())) {
				int recycleCoin = (int) (winCoin * (gameDetail.getCornucopia_recycle_ratio() / 100.0f));
				coinPlayer.setRecycleCoin(recycleCoin);
				logger.error("coin [{}],Cornucopia,game:{} player:{} winCoin:{} recycleCoin:{}", getCoinIndex(), gameDetail.getGame_type_index(),
						accountId, winCoin, recycleCoin);
			}
		}
		return settleList;
	}

	private void handleResultWinCoin() {
		int sumWinCoin = getSumWinCoin(winMap);
		int sumLoseCoin = getSumWinCoin(loseMap);

		int sumValue = setResultWinCoin(winMap, sumCoin, sumWinCoin);
		int leftValue = sumCoin - sumValue;
		if (leftValue > 0) {
			addLeftResultWinCoin(winMap, leftValue);
		}
		setResultWinCoin(loseMap, -sumCoin, sumLoseCoin);
	}

	private void GameOverSettle(List<CoinPlayer> playerList) {
		ICenterRMIServer centerRmiServer = SpringService.getBean(ICenterRMIServer.class);
		GameOverResponse.Builder resp = GameOverResponse.newBuilder();
		resp.setGameId(gameTypeIndex);
		resp.setDetailId(getDetail().getId());

		String des = "";
		EMoneyOperateType type = null;
		GamePlayerMsg.Builder playerMsg = null;
		boolean isRobot = false;
		int resultWinCoin = 0;
		int winOrder = 0;
		long resultOwnResult = 0;

		CornucopiaRecycleCoinProto.Builder recycleCoinBuilder = CornucopiaRecycleCoinProto.newBuilder();
		recycleCoinBuilder.setGameTypeIndex(gameDetail.getGame_type_index());

		for (CoinPlayer player : playerList) {
			playerMsg = GamePlayerMsg.newBuilder();
			playerMsg.setAccountId(player.getAccount_id());
			playerMsg.setNickname(player.getNick_name());

			//真实赢的-回收
			resultWinCoin = player.getResultWinCoin2();// ;

			//0为赢  1为输 2为平
			winOrder = resultWinCoin == 0 ? 2 : (resultWinCoin > 0 ? 0 : 1);
			resultOwnResult = player.getResultOwnCoin();

			playerMsg.setRoundScore(resultWinCoin);
			playerMsg.setSeatId(player.get_seat_index());
			playerMsg.setWinOrder(winOrder);
			playerMsg.setIsBankruptcy(resultOwnResult <= 0 ? true : false);
			playerMsg.setOwnCoin(resultOwnResult);

			//特殊玩法相关  by wu_hc 2018-08-15
			ExciteConditionGroup conditionGroup = player.getConditionGroup();
			if (null != conditionGroup) {
				playerMsg.setExciteMultiple(player.getExciteMultiple());
				List<CoinProtocol.CardCategoryProto> pb = conditionGroup.toCardCategoryPBBuilder(this.gameId);
				if (!pb.isEmpty()) {
					playerMsg.addAllCardCategorys(pb);
				}

				Optional<CoinProtocol.CardArrayProto> cardPB = conditionGroup.toCardProto(ETriggerType.START);
				if (cardPB.isPresent()) {
					playerMsg.setStartCards(cardPB.get());
				}
				cardPB = conditionGroup.toCardProto(ETriggerType.OVER);
				if (cardPB.isPresent()) {
					playerMsg.setOverCards(cardPB.get());
				}

			}

			playerMsg.setRecycleCoin(player.getRecycleCoin());

			//利息，场次聚宝盆玩法，而且玩家赢币，与是否参与无关
			if (!player.isRobot() && player.getRecycleCoin() > 0) {
				recycleCoinBuilder.addAccrual(CommonLI.newBuilder().setK(player.getAccount_id()).setV(player.getRecycleCoin()));
			}

			//参赛费，需要并入奖池[报名才会收费]
			if (!player.isRobot() && player.isCornucopia()) {
				recycleCoinBuilder.addExpenses(CommonLI.newBuilder().setK(player.getAccount_id()).setV(gameDetail.getCornucopia_cost()));
			}

			resp.addPlayerMsg(playerMsg.build());
			player.setCoinBankruptcy(isBankruptcy(resultOwnResult));
			isRobot = player.isRobot();
			//			logger.info("GameOverSettle->coin settlement roomId:{} gameTypeIndex:{} level:{} num:{} accountId:{} isRobot:{} winOrder:{}"
			//					+ " winCoin:{} resultWinCoin1:{} resultWinCoin2:{} ownCoin:{}!!",
			//					gameRoom.getRoom_id(),getDetail().getGame_type_index(),getDetail().getRound_level(),playerList.size(),player.getAccount_id(),isRobot,winOrder,
			//					player.getWinCoin(),player.getResultWinCoin1(),player.getResultWinCoin2(),resultOwnResult);

			//

			//发奖的时候，需要减去被回收的----PS
			resultWinCoin -= player.getRecycleCoin();

			if (isRobot || resultWinCoin == 0) {
				continue;
			}

			des = "金币场结算奖励金币";
			type = EMoneyOperateType.COIN_SETTLE_REWARD;
			if (resultWinCoin < 0) {
				des = "金币场结算扣除金币";
				type = EMoneyOperateType.COIN_SETTLE_COST;
			}

			if (player.getRecycleCoin() > 0) {
				des += (",聚宝盆回收金币:" + player.getRecycleCoin());
			}
			try {
				AddGoldResultModel model = centerRmiServer
						.addAccountGoldAndMoney(player.getAccount_id(), 0, false, des, EGoldOperateType.MATCH_APPLY, resultWinCoin, type);
				if (null != model && model.isSuccess()) {
					AccountModel accountModel = model.getAccountModel();
					if (null != accountModel) {
						player.setMoney(accountModel.getMoney());
					}
				}
			} catch (Exception e) {
				logger.error("error addmoney",e);
			}
		}

		for (CoinPlayer player : playerList) {
			gameRoom.handler_refresh_player_data(player.get_seat_index());
		}

		//金币场回收
		//SessionServiceImpl.getInstance().sendToCoin(PBUtil.toS2SRequet(S2SCmd.COIN__RECYCLE_UPDATE, recycleCoinBuilder).build());
		GlobalExecutor.execute(new CoinSenderEnsureTask(PBUtil.toS2SRequet(S2SCmd.COIN__RECYCLE_UPDATE, recycleCoinBuilder).build()));

		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountIds);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_OVER, resp)).build();

		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
		logger.info("coin over <===============gameTypeIndex:{},roomId:{},detailId:{},coinIndex:{},accountIds:{} costTime:{}s !!", gameTypeIndex,
				coinRoomId, getDetail().getId(), coinIndex, accountIds, (System.currentTimeMillis() - gameStartTime) / 1000);

	}

	private boolean isBankruptcy(long ownCoin) {
		CoinGameDetail detail = getDetail();
		int bankruptcyValue = detail.getMin_coin();
		if (ownCoin < bankruptcyValue) {
			return true;
		}
		return false;
	}

	private int getSumWinCoin(Map<Long, CoinPlayer> map) {
		int sum = 0;
		if (map == null || map.size() <= 0) {
			return sum;
		}
		for (CoinPlayer player : map.values()) {
			sum += player.getResultWinCoin1();
		}
		return sum;
	}

	private int setResultWinCoin(Map<Long, CoinPlayer> map, int endSumCoin, int curSumCoin) {
		double dEndSumCoin = endSumCoin;
		double dCurSumCoin = curSumCoin;
		double rate = dEndSumCoin / dCurSumCoin;
		CoinPlayer player = null;
		int sum = 0;
		int result = 0;
		for (long accountId : map.keySet()) {
			player = map.get(accountId);
			result = (int) (player.getResultWinCoin1() * rate);
			sum += result;
			player.setResultWinCoin2(result);
		}
		return sum;
	}

	private void addLeftResultWinCoin(Map<Long, CoinPlayer> map, int leftValue) {
		CoinPlayer player = null;
		CoinPlayer maxWinPlayer = null;
		int maxValue = -1;
		int result = 0;
		for (long accountId : map.keySet()) {
			player = map.get(accountId);
			result = player.getResultWinCoin2();
			if (result > maxValue) {
				maxValue = result;
				maxWinPlayer = player;
			}
		}
		if (maxWinPlayer != null) {
			result = maxWinPlayer.getResultWinCoin2() + leftValue;
			maxWinPlayer.setResultWinCoin2(result);
		}

		return;
	}

	/**
	 * 游戏开始
	 */
	public void gameStart() {
		DescParams params = getDetail().getRuleParam();
		int playerCount = RoomComonUtil.getMaxNumber(params);
		int mode = players.size() % playerCount; // 轮空的

		int minLimit = getDetail().getLower_limit();
		int maxLimit = getDetail().getUpper_limit();
		// 补齐玩家人数
		if (mode > 0) {
			int count = playerCount - mode;
			List<CoinPlayer> robotList = getRandom().getRandomCoinPlayers(count, coinIndex, minLimit, maxLimit);
			//add by wu_hc,给机器人也加倍数
			robotList.forEach(p -> initExciteCondition(p));

			players.addAll(robotList);
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int roomId = centerRMIServer.moneyRandomRoomId(1);
		coinRoomId = roomId;
		int coin = getDetail().getBase_coin();
		try {
			handleBaseCoin(-coin, "金币场扣除台费!", false);
			gameStart0(roomId, params);
		} catch (Exception e) {
			handleBaseCoin(coin, "金币场返还台费!", true);
			logger.error("gameStart-> error !!", e);
		}
	}

	private void gameStart0(int roomId, DescParams params) {
		int game_type_index = params._game_type_index;

		MatchBaseScoreJsonModel model = new MatchBaseScoreJsonModel();
		model.setBase(1);
		model.setBaseScore(getDetail().getBase_antes());
		model.setTimes(1);
		// 测试牌局
		AbstractRoom table = LogicRoomHandler.createRoom(game_type_index, params._game_rule_index);
		table.enableRobot();
		table.setCreate_type(GameConstants.CREATE_ROOM_NEW_COIN);
		table.setRoom_id(roomId);
		table.setCreate_time(System.currentTimeMillis() / 1000L);
		table.setRoom_owner_account_id(players.get(0).getAccount_id());

		table.setRoom_owner_name(players.get(0).getNick_name());

		table.initRoomRule(coinIndex, 0, params);
		table.setPlayOutTime(getDetail().getPlay_card_time());
		table.matchBase = model;
		table.init_table(game_type_index, params._game_rule_index, 1);
		PlayerServiceImpl.getInstance().getRoomMap().put(roomId, table);
		table.setGame_id(SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index));

		int maxNumber = RoomComonUtil.getMaxNumber(table, table.getDescParams());
		table.handler_create_coin_room(players, GameConstants.CREATE_ROOM_NEW_COIN, maxNumber, model);
		toStart();

		this.gameRoom = table;

		try {
			int gameID = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
			for (Player player : table.get_players()) {
				//参与金币场人员都完成一次任务
				if (null != player && !player.isRobot()) {
					//完成金币模式牌局XX次
					FoundationService.getInstance()
							.sendActivityMissionProcess(player.getAccount_id(), ActivityMissionTypeEnum.COIN_BOARD_SUMMARY, 1, 1);
					//完成金币模式XX游戏牌局XX次
					FoundationService.getInstance()
							.sendActivityMissionProcess(player.getAccount_id(), ActivityMissionTypeEnum.COIN_TARGET_GAME_BOARD, gameID, 1);
				}
			}
		} catch (Exception e) {
			logger.error("任务执行出错", e);
		}
	}

	/**
	 * 游戏开始准备阶段
	 */
	public void gameReady(List<Long> accountIds, List<CoinPlayerProto> playerProtos) {
		this.accountIds = accountIds;

		gameStartTime = System.currentTimeMillis();

		Map<Long, CoinPlayerProto> protoMap = playerProtos.stream().collect(Collectors.toMap(CoinPlayerProto::getAccountId, Function.identity()));
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<PlayerViewVO> playerList = centerRMIServer.rmiInvoke(RMICmd.MATCH_PLAYER_INFO, accountIds);
		playerList.forEach((account) -> {
			CoinPlayer player = createPlayer(account);

			CoinPlayerProto playerProto = protoMap.get(account.getAccountId());
			player.setCornucopia(null != playerProto && playerProto.getCornucopia());

			players.add(player);
		});

		GameStartResponse.Builder b = GameStartResponse.newBuilder();
		b.setGameId(gameTypeIndex);
		b.setDetailId(getDetail().getId());

		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountIds);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_MATCH_SUC, b)).build();

		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));

		logger.info("coin start ===============>gameTypeIndex:{},roomId:{} detailId:{},coinIndex:{},accountIds:{},costTime:{}s !!", gameTypeIndex,
				coinRoomId, getDetail().getId(), coinIndex, accountIds, (System.currentTimeMillis() - gameStartTime) / 1000);

		CoinGameStartTask task = new CoinGameStartTask(this);
		executor.scheduleSecond(coinIndex, task, START_TIME);
	}

	private CoinPlayer createPlayer(PlayerViewVO account) {
		CoinPlayer player = new CoinPlayer();

		player.setAccount_id(account.getAccountId());
		player.setProxy_session_id(account.getAccountId());
		player.setGold(account.getGold());
		player.setAccount_icon(account.getHead());
		player.setAccount_ip("");
		player.setAccount_ip_addr("");
		player.setNick_name(account.getNickName());
		player.setSex(account.getSex());
		player.set_seat_index(GameConstants.INVALID_SEAT);
		player.setRoom_id(0);
		player.setMoney(account.getMoney());
		player.setCoinPlayTime(account.getCoinPlayTime().getTime());
		player.setCreateTime(account.getCreate_time().getTime());

		//add by wu_hc
		initExciteCondition(player);

		return player;
	}

	private void handleBaseCoin(int coin, String msg, boolean isBack) {
		long ownMoney = 0;
		for (CoinPlayer player : players) {
			int baseCoin = coin;

			//考虑聚宝盆
			if (player.isCornucopia() && StringUtils.isNotEmpty(gameDetail.getCornucopia_condition_id())) {
				if (isBack) {
					baseCoin += gameDetail.getCornucopia_cost();
				} else {
					baseCoin -= gameDetail.getCornucopia_cost();
				}

				logger.error("coin[{}],game:{},player:{} 聚宝盆{}豆:{}", getCoinIndex(), gameDetail.getGame_type_index(), player.getAccount_id(),
						isBack ? "还" : "扣", gameDetail.getCornucopia_cost());
			}

			if (isBack) {
				baseCoin = Math.abs(player.getBaseCoin());
			}
			ownMoney = player.getMoney() + baseCoin;
			if (ownMoney < 0) {
				baseCoin = (int) player.getMoney();
				ownMoney = 0;
			}
			player.setMoney(ownMoney);
			player.setBaseCoin(baseCoin);
			if (player.isRobot()) {
				continue;
			}

			try {
				ICenterRMIServer centerRmiServer = SpringService.getBean(ICenterRMIServer.class);
				centerRmiServer.addAccountGoldAndMoney(player.getAccount_id(), 0, false, msg, EGoldOperateType.MATCH_APPLY, baseCoin,
						EMoneyOperateType.COIN_BASE_COST);
			} catch (Exception e) {
				logger.error("error",e);
			}
		}
	}

	public CoinPlayer getCoinPlayer(long accountId) {
		for (CoinPlayer player : players) {
			if (player.getAccount_id() == accountId) {
				return player;
			}
		}
		return null;
	}

	public AbstractRoom getGameRoom() {
		return gameRoom;
	}

	private RobotRandom getRandom() {
		if (robotRandom == null) {
			robotRandom = RobotService.getInstance().getRobotRandom();
		}
		return robotRandom;
	}

	public int getGameTypeIndex() {
		return gameTypeIndex;
	}

	public int getCoinIndex() {
		return coinIndex;
	}

	public CoinGameDetail getDetail() {
		return CoinDict.getInstance().getGameDetail(gameTypeIndex, detailId);
	}

	public boolean isStart() {
		return isStart;
	}

	public void toStart() {
		this.isStart = true;
	}

	/**
	 * @param player
	 */
	private void initExciteCondition(CoinPlayer player) {
		if (null != exciteCdtIds && !exciteCdtIds.isEmpty()) {
			List<CoinExciteModel> conditions = buildConditionModels(exciteCdtIds);
			player.initConditionIfHas(DataWrap.Type.EXCITE, conditions, this);
		}

		//非机器人才会参与聚宝盆
		if (!player.isRobot() && null != cornucopiaCdtIds && !cornucopiaCdtIds.isEmpty()) {
			List<CoinExciteModel> conditions = buildConditionModels(cornucopiaCdtIds);
			player.initConditionIfHas(DataWrap.Type.CORNUCOPIA, conditions, this);
		}
	}

	/**
	 * @param cdtIds
	 * @return
	 */
	static final List<CoinExciteModel> buildConditionModels(final List<Integer> cdtIds) {
		List<CoinExciteModel> conditions = Lists.newArrayListWithCapacity(cdtIds.size());
		cdtIds.forEach(cdtId -> {
			CoinExciteModel model = CoinExciteDict.getInstance().getExciteModel(cdtId);
			if (null != model) {
				conditions.add(model);
			} else {
				logger.error("金币场-刺激玩法找不到条件id:{}", cdtId);
			}
		});
		return conditions;
	}

	@Override
	public void accept(DataWrap wrap) {

		if (null == wrap.cdts || wrap.cdts.isEmpty()) {
			return;
		}

		if (wrap.type == DataWrap.Type.CORNUCOPIA) {
			cornucopiaEvent(wrap);

		} else if (wrap.type == DataWrap.Type.EXCITE) {
			//do anything
		}
	}

	/**
	 * 聚宝盆事件
	 *
	 * @param wrap
	 */
	private void cornucopiaEvent(DataWrap wrap) {
		CornucopiaAwardProto.Builder builder = CornucopiaAwardProto.newBuilder();
		builder.setAccountId(wrap.accountId);
		builder.setGameTypeIndex(gameDetail.getGame_type_index());
		builder.setRoundLevel(gameDetail.getRound_level());
		builder.setRoomId(coinRoomId);
		builder.setDetailId(gameDetail.getId());
		int optimal = 0;
		for (IExciteCondition cdt : wrap.cdts) {
			builder.addExciteIds(cdt.id());
			if (cdt.model().getOutput() > optimal) {
				optimal = cdt.model().getOutput();
				builder.setOptimalCategoryId(cdt.cardTypeValue());
				if (StringUtils.isNotEmpty(cdt.cardCategoryModel().getDescription())) {
					builder.setCategoryName(cdt.cardCategoryModel().getDescription());
				}
			}
		}
		builder.setOptimalOutput(optimal);

		GlobalExecutor.execute(new CoinSenderEnsureTask(PBUtil.toS2SRequet(S2SCmd.COIN_EXCITE_MONEY_UPDATE, builder).build()));
		//SessionServiceImpl.getInstance().sendToCoin(PBUtil.toS2SRequet(S2SCmd.COIN_EXCITE_MONEY_UPDATE, builder).build());
	}
}
