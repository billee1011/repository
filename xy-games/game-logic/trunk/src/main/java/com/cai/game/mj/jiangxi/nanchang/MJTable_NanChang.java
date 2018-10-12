package com.cai.game.mj.jiangxi.nanchang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.czbg.CZBGConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NANCHANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.game.RoomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;
import com.google.common.base.Strings;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Nanchang.ActionData_NC;
import protobuf.clazz.mj.Nanchang.ActionJingDatas;
import protobuf.clazz.mj.Nanchang.FaDianActionEnd;
import protobuf.clazz.mj.Nanchang.GameEndInfo;
import protobuf.clazz.mj.Nanchang.GameEndResponse_NC;

/**
 * 
 * 南昌麻将
 *
 * @author WalkerGeek date: 2018年8月23日 下午9:12:41 <br/>
 */
public class MJTable_NanChang extends AbstractMJTable {

	private static Logger logger = LoggerFactory.getLogger(MJTable_NanChang.class);

	private static final long serialVersionUID = -3740668572580145190L;

	public boolean isLast; // 是否最后一张牌
	public int isBian[]; // 是否胡的是边章
	public int canNotHu[]; // 过手胡
	public int canNotGang[]; // 每个玩家当前不能杠的牌[有杠不杠选择碰牌、要过一轮之后才能杠]
	public int outCardRound[]; // 补杠辅助判断
	public boolean isQiangGang; // 是否抢杠
	public boolean isDahu;
	public boolean isDianPao;
	public boolean isLiuJu;
	public ChiHuRight[] chr; // 胡牌类型
	public int jiePao[];
	public int dianPao[];
	public int jing[]; // 上精
	public int xiaJing[];
	public int xiaJingNumber;
	public int jingProvider[]; // 上精提供者

	public int jingScore[];
	public int huPaiScore[];
	public int jiangliScore[];
	public int mingGangScore[];
	public int anGangScore[];
	public int gangJingScore[];
	public int chaoZhuangScore[];
	public int maxFan[]; // 最大胡牌番数
	public ChiHuRight[] maxChr; // 最大胡牌番数时胡的牌型
	public int beginCardCount;
	public int chaoZhuangSeat;
	public int fisrtOut[];

	public int chongGuan[][];
	public boolean isBaWang[][];
	public int jingCount[][][];
	public int jingGetScore[][];
	public int piaoZhengScore[];
	public int piaoFuScore[];
	public int preBanker;
	public int[][] personJing; // 个人随机精
	public int[][] touZi; // 当局骰子信息
	public int[] touZicount; // 每个数字出现的次数
	public int faDianCount; // 当前发电次数
	public boolean[] tingPai; // 停牌
	public int[][] tingCard; // 每个玩家的停牌
	public int[] tingCount; // 每个玩家的停牌数量
	public GameLogicNanChang _logic; // 南昌麻将逻辑处理器
	public int[] shang_ju_jing; // 上局精数据
	public Map<Integer, HashMap<Integer, Integer>> xiaoJuScore; // 小局数据
	public Map<Integer, HashMap<Integer, Integer>> daJuScore; // 大局数据
	public int[] huScore;
	public int liangZhuangCount; // 连庄次数
	public int shangJuZhuang; // 上局庄
	private int[] dispatchcardNum; // 摸牌次数
	private boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCount; // 跟庄次数
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index; // 下个玩家
	private int[] tingPaiIndex; // 停牌玩家
	private int piaoZheng[]; // 飘正精
	private int piaoFu[]; // 飘正精
	private int provide_index;
	private int[] gang_jing_card; // 计算杠精的牌值，比如同一首歌
	private int gang_jing_count; // 杠精数量
	private int count_fa_dian; // 发电常量
	private int[] piaoJingScore; // 记录胡牌分数:存在飘精，大结算显示

	public MJTable_NanChang() {
		_logic = new GameLogicNanChang(MJType.DEFAULT);
	}

	/**
	 * 添加飘精牌
	 * 
	 * @param seat_index
	 * @param card
	 */
	public void addPiaoJing(int seat_index, int card) {
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_PIAOJING)) {
			if (jing[0] == card) {
				piaoZheng[seat_index]++;
			} else if (jing[1] == card) {
				piaoFu[seat_index]++;
			}
		}
	}

	/**
	 * 是否飘精
	 * 
	 * @param seat_indx
	 * @return
	 */
	public boolean isPiaoJing(int seat_index) {
		return piaoZheng[seat_index] + piaoFu[seat_index] > 0;
	}

	/**
	 * 跟庄操作
	 * 
	 * @param seat_index
	 * @param card
	 * @param isZhuang
	 */
	public void addGenZhuangCard(int seat_index, int card, boolean isZhuang) {
		if (isZhuang) {
			genZhuangCard = card;
			next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			if (!isDispatchcardNum(seat_index)) {
				setGenZhuangCount();
			}
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			}
		}
	}

	/**
	 * 跟庄状态
	 * 
	 * @return the isCanGenZhuang
	 */
	public boolean isCanGenZhuang() {
		return isCanGenZhuang;
	}

	/**
	 * 跟庄次数
	 * 
	 * @return the genZhuangCount
	 */
	public int getGenZhuangCount() {
		return genZhuangCount;
	}

	/**
	 * 跟庄次数累计
	 * 
	 * @param genZhuangCount
	 *            the genZhuangCount to set
	 */
	public void setGenZhuangCount() {
		this.genZhuangCount++;
	}

	/** 摸牌数累计 */
	public void addDispatchcardNum(int seat_index) {
		dispatchcardNum[seat_index]++;
	}

	/**
	 * 是否第一次摸牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isDispatchcardNum(int seat_index) {
		return dispatchcardNum[seat_index] == 1;
	}

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		} else if (getRuleValue(Constants_MJ_NANCHANG.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) {
			return 2;
		} else if (getRuleValue(Constants_MJ_NANCHANG.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) {
			return 3;
		}
		return 4;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_NanChang();
		_handler_dispath_card = new MJHandlerDispatchCard_NanChang();
		_handler_gang = new MJHandlerGang_NanChang();
		_handler_out_card_operate = new MJHandlerOutCardOperate_NanChang();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);
		isBian = new int[this.getTablePlayerNumber()];
		canNotHu = new int[this.getTablePlayerNumber()];
		canNotGang = new int[this.getTablePlayerNumber()];
		outCardRound = new int[this.getTablePlayerNumber()];
		jiePao = new int[this.getTablePlayerNumber()];
		dianPao = new int[this.getTablePlayerNumber()];
		jing = new int[2];
		xiaJing = new int[12];
		this.isLast = false;
		this.isQiangGang = false;
		this.isDahu = false;
		this.isDianPao = false;
		this.isLiuJu = false;
		this.preBanker = -1;
		touZi = new int[22][2];
		touZicount = new int[13];
		faDianCount = 0;
		tingPai = new boolean[this.getTablePlayerNumber()];
		tingCard = new int[this.getTablePlayerNumber()][22];
		tingCount = new int[this.getTablePlayerNumber()];

		Arrays.fill(tingPai, false);
		xiaJingNumber = 0;
		huScore = new int[this.getTablePlayerNumber()];
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_MAI_LEI)) {
			xiaJingNumber = 1;
		} else if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SXZZYY)) {
			xiaJingNumber = 5;
		}

		// WalkerGeek 根据人数确定开始张数
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_ALL_GAME_TWO_PLAYER)) {
			beginCardCount = Constants_MJ_NANCHANG.BEGIN_TWO;
		} else if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_ALL_GAME_THREE_PLAYER)) {
			beginCardCount = Constants_MJ_NANCHANG.BEGIN_THREE;
		} else {
			beginCardCount = Constants_MJ_NANCHANG.BEGIN_FOUR;
		}
		personJing = new int[getTablePlayerNumber()][2];
		this.jingScore = new int[this.getTablePlayerNumber()];
		this.huPaiScore = new int[this.getTablePlayerNumber()];
		this.jiangliScore = new int[this.getTablePlayerNumber()];
		this.mingGangScore = new int[this.getTablePlayerNumber()];
		this.anGangScore = new int[this.getTablePlayerNumber()];
		this.gangJingScore = new int[this.getTablePlayerNumber()];
		this.chaoZhuangScore = new int[this.getTablePlayerNumber()];
		this.jingProvider = new int[this.getTablePlayerNumber()];
		this.maxFan = new int[this.getTablePlayerNumber()];
		this.fisrtOut = new int[this.getTablePlayerNumber()];
		this.piaoZhengScore = new int[this.getTablePlayerNumber()];
		this.piaoFuScore = new int[this.getTablePlayerNumber()];

		chongGuan = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否冲关
		isBaWang = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否霸王精
		jingCount = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6][2];// 每一组精中正、副精的个数
		jingGetScore = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精中得到的分数

		chr = new ChiHuRight[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			chr[i] = new ChiHuRight();
		}
		maxChr = new ChiHuRight[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			maxChr[i] = new ChiHuRight();
		}

		this.setMinPlayerCount(this.getTablePlayerNumber());
	}

	@Override
	public boolean reset_init_data() {
		this.isLast = false;
		this.isQiangGang = false;
		this.isDahu = false;
		this.isDianPao = false;
		this.isLiuJu = false;
		this.chaoZhuangSeat = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			chr[i] = new ChiHuRight();
			maxChr[i] = new ChiHuRight();
		}
		Arrays.fill(isBian, 0);
		Arrays.fill(jiePao, 0);
		Arrays.fill(dianPao, 0);
		Arrays.fill(jing, 0);
		Arrays.fill(xiaJing, 0);
		Arrays.fill(jingScore, 0);
		Arrays.fill(huPaiScore, 0);
		Arrays.fill(jiangliScore, 0);
		Arrays.fill(mingGangScore, 0);
		Arrays.fill(anGangScore, 0);
		Arrays.fill(gangJingScore, 0);
		Arrays.fill(chaoZhuangScore, 0);
		Arrays.fill(jingProvider, -1);
		Arrays.fill(maxFan, 0);
		Arrays.fill(fisrtOut, 0);
		Arrays.fill(canNotHu, 0);
		Arrays.fill(piaoZhengScore, 0);
		Arrays.fill(piaoFuScore, 0);
		chongGuan = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否冲关
		isBaWang = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否霸王精
		jingCount = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6][2];// 每一组精中正、副精的个数
		jingGetScore = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精中得到的分数
		personJing = new int[getTablePlayerNumber()][2];

		_logic.clean_magic_cards();

		return super.reset_init_data();
	}

	@Override
	protected void init_shuffle() {
		int[] cards = Constants_MJ_NANCHANG.DEFAULT;
		_repertory_card = new int[cards.length];
		shuffle(_repertory_card, cards);
	}

	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = getOpenRoomIndex();// 创建者的玩家为专家

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
		genZhuangCount = 0;
		GRR._banker_player = _cur_banker;
		_current_player = _cur_banker;
		if (_cur_round == 1) {
			shangJuZhuang = _cur_banker;
		}
	}

	@Override
	protected boolean on_game_start() {
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		initPram();
		// 每局开始初始化剩余牌局数
		if (getTablePlayerNumber() == 2) {
			beginCardCount = Constants_MJ_NANCHANG.BEGIN_TWO;
		} else if (getTablePlayerNumber() == 3) {
			beginCardCount = Constants_MJ_NANCHANG.BEGIN_THREE;
		} else {
			beginCardCount = Constants_MJ_NANCHANG.BEGIN_FOUR;
		}

		this.chaoZhuangSeat = this._cur_banker;
		this.preBanker = this._cur_banker;

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i], getJingData(i));
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				GameStartAfter();
			}
		}, 1, TimeUnit.SECONDS);
	

		return true;
	}
	
	
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			room_player.setAccountId(rplayer.getAccount_id());
			// room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			// room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			if(GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status){
				room_player.setScore(_player_result.game_score[i]);
			}else{
				room_player.setScore(_player_result.game_score[i]+getGameScore(i));
			}
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			if (_game_status == GameConstants.GS_MJ_PAO) {
				if (null != _playerStatus[i]) {
					// 叫地主的字段值，用来标示玩家是否点了跑呛
					if (_playerStatus[i]._is_pao_qiang) {
						room_player.setJiaoDiZhu(1);
					} else {
						room_player.setJiaoDiZhu(0);
					}
				}
			}

			roomResponse.addPlayers(room_player);
		}
	}
	
	
	public void load_player_info_data_end(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			room_player.setAccountId(rplayer.getAccount_id());
			// room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			// room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			if (_game_status == GameConstants.GS_MJ_PAO) {
				if (null != _playerStatus[i]) {
					// 叫地主的字段值，用来标示玩家是否点了跑呛
					if (_playerStatus[i]._is_pao_qiang) {
						room_player.setJiaoDiZhu(1);
					} else {
						room_player.setJiaoDiZhu(0);
					}
				}
			}

			roomResponse.addPlayers(room_player);
		}
	}

	/**
	 * 游戏开始后流程
	 */
	public void GameStartAfter() {
		sendFirstCard();
		ActionDataModel actions = new ActionDataModel();
		// 回头一笑
		if (_cur_round > 1 && has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_HUI_TOU_XIAO)) {
			actions.addActionType(Constants_MJ_NANCHANG.ACTION_HUI_TOU_XIAO);
			actions.addActionJinData(new ActionJinData(getPlayerCard(), getTablePlayerNumber(), shang_ju_jing[0],
					shang_ju_jing[1], -1, GameConstants.INVALID_SEAT, null, null, getBaWangJingType()));
		}
		// 照镜子
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_ZHAO_JIN_ZI)) {
			buildZhaoJingZi(actions, Constants_MJ_NANCHANG.ACTION_ZHAO_JING_ZI);
		}

		calJing(actions);
	}

	/**
	 * 封装照镜子数据（晒月亮）
	 * 
	 * @param actions
	 */
	public void buildZhaoJingZi(ActionDataModel actions, int type) {
		// 所有玩家的牌值
		int[][] playerCards = getPlayerCardAll();
		int[][] playerCardsIndex = getPlayerCardIndex(playerCards);
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			// 检测玩家是否有大于5
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				int fuIndex = _logic.switch_to_card_index(_logic.getFuJing(_logic.switch_to_card_data(i)));
				int score = 2 * playerCardsIndex[p][i] + playerCardsIndex[p][fuIndex];

				if (score >= 5) {
					actions.addActionType(type);
					int zheng = _logic.switch_to_card_data(i);
					int fu = _logic.getFuJing(zheng);

					actions.addActionJinData(new ActionJinData(playerCards, getTablePlayerNumber(), zheng, fu, -1,
							GameConstants.INVALID_SEAT, null, null, getBaWangJingType()));
					break;
				}
			}
		}
	}

	/**
	 * 结算数据封装
	 * 
	 * @param actions
	 * @param type
	 * @param zheng
	 * @param fu
	 */
	public void buildGameEndData(ActionDataModel actions, int type, int zheng, int fu, List<Integer> other) {
		actions.addActionType(type);
		actions.addActionJinData(new ActionJinData(getPlayerCardAll(), getTablePlayerNumber(), zheng, fu, -1,
				GameConstants.INVALID_SEAT, null, other, getBaWangJingType()));
	}

	public void initPram() {
		touZi = new int[22][2];
		touZicount = new int[13];
		faDianCount = 0;
		provide_index = GameConstants.INVALID_SEAT;
		tingCard = new int[this.getTablePlayerNumber()][22];
		tingCount = new int[this.getTablePlayerNumber()];
		xiaoJuScore = new HashMap<>();
		huScore = new int[this.getTablePlayerNumber()];
		for (int i = 1; i <= getTablePlayerNumber(); i++) {
			xiaoJuScore.put(i, new HashMap<Integer, Integer>());
		}
		isCanGenZhuang = true; // 可以跟庄
		genZhuangCount = 0; // 跟庄次数
		genZhuangCard = 0; // 跟庄牌
		next_seat_index = -1; // 下个玩家、
		count_fa_dian = 0;
		piaoJingScore = new int[getTablePlayerNumber()];
		dispatchcardNum = new int[getTablePlayerNumber()];
		tingPaiIndex = new int[getTablePlayerNumber()];
		piaoZheng = new int[getTablePlayerNumber()];
		piaoFu = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._provider[i] = -1;
			_player_result.ziba[i] = 0;
		}
		gang_jing_card = new int[20]; // 初始化杠精其他牌值
		gang_jing_count = 0;
		if (_cur_round == 1) {
			shang_ju_jing = new int[2];
			daJuScore = new HashMap<>();
			for (int i = 1; i <= getTablePlayerNumber(); i++) {
				daJuScore.put(i, new HashMap<Integer, Integer>());
			}
		}
	}

	/**
	 * 类型映射
	 * 
	 * @param type
	 * @return
	 */
	public int getJieShuangType(int type) {
		switch (type) {
		case Constants_MJ_NANCHANG.ACTION_ZHAO_JING_ZI:
			return Constants_MJ_NANCHANG.ZHAOJINGZI;

		case Constants_MJ_NANCHANG.ACTION_MAI_DI_LEI:
			return Constants_MJ_NANCHANG.MAILEI;

		case Constants_MJ_NANCHANG.ACTION_TONG_YI_SHOU_GE:
			return Constants_MJ_NANCHANG.CHANGGE;

		case Constants_MJ_NANCHANG.ACTION_SHAI_YUE_LIANG:
			return Constants_MJ_NANCHANG.SHAIYUELIANG;

		case Constants_MJ_NANCHANG.ACTION_FA_DIAN:
			return Constants_MJ_NANCHANG.FADIAN;

		case Constants_MJ_NANCHANG.ACTION_SHANG_JING:
			return Constants_MJ_NANCHANG.SHANGJING;
		case Constants_MJ_NANCHANG.ACTION_SHANG_XIA_FANG:
			return Constants_MJ_NANCHANG.XIAJING;

		case Constants_MJ_NANCHANG.ACTION_XIA_JING:
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SXZZYY)) {
				return Constants_MJ_NANCHANG.XIAZOUYOU;
			}
			return Constants_MJ_NANCHANG.XIAJING;
		case Constants_MJ_NANCHANG.ACTION_ZUO_JING:
		case Constants_MJ_NANCHANG.ACTION_YOU_JING:
		case Constants_MJ_NANCHANG.ACTION_ZZ_JING:
		case Constants_MJ_NANCHANG.ACTION_YY_JING:
			return Constants_MJ_NANCHANG.XIAZOUYOU;
		case Constants_MJ_NANCHANG.ACTION_HUI_TOU_XIAO:
			return Constants_MJ_NANCHANG.HUITOU;
		default:
			break;
		}

		return GameConstants.INVALID_SEAT;
	}

	/**
	 * 记录算分
	 * 
	 * @param actions
	 */
	public void calScore(ActionDataModel actions) {

		for (int i = 1; i <= getTablePlayerNumber(); i++) {
			Map<Integer, Integer> map = xiaoJuScore.get(i);
			if (map == null) {
				map = new HashMap<Integer, Integer>();
			}
			for (int k = 0; k < actions.getActionType().size(); k++) {
				// 停牌不计算分
				if (actions.getActionType().get(k) == Constants_MJ_NANCHANG.ACTION_TING_PAI) {
					continue;
				}
				int type = getJieShuangType(actions.getActionType().get(k));
				if (type == GameConstants.INVALID_SEAT) {
					continue;
				}
				int newScore = actions.getActionJingDatas().get(k).getJingInfos().get((i - 1)).getEveryJingScore();
				int oldScore = map.get(type) == null ? 0 : map.get(type);
				map.put(type, newScore + oldScore);
			}
		}
	}

	
	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	public boolean operate_player_cards_with_ting_gzcg(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(changeCard(_playerStatus[seat_index]._hu_out_card_ting[i], seat_index) + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}
	
	/**
	 * 计算大结算的分
	 */
	@SuppressWarnings("unused")
	public void calDaJuScore() {
		for (int i = 1; i <= getTablePlayerNumber(); i++) {
			Map<Integer, Integer> map = xiaoJuScore.get(i);
			if (map == null) {
				continue;
			}
			Map<Integer, Integer> daMap = daJuScore.get(i);
			if (map == null) {
				daMap = new HashMap<Integer, Integer>();
			}
			for (Entry<Integer, Integer> entry : map.entrySet()) {
				// 大结算不显示爬楼及飘分
				int key = entry.getKey();
				if (key == Constants_MJ_NANCHANG.PALOU || key == Constants_MJ_NANCHANG.PIAOJING) {
					continue;
				}
				int bei = 1;
				if(map.get(Constants_MJ_NANCHANG.PALOU ) != null){
					bei = map.get(Constants_MJ_NANCHANG.PALOU ) ;
				}
				int score = 0;
				int s = 0;
				
				if(key ==  Constants_MJ_NANCHANG.HUSCORE ){
					score = piaoJingScore[i-1] * bei;
					s = daMap.get(entry.getKey()) == null ? 0 : daMap.get(key);
				}else{
					 score = entry.getValue() * bei;
					 s = daMap.get(key) == null ? 0 : daMap.get(key);
				}
				daMap.put(key,  score+ s);
			}
		}
	}

	/**
	 * 翻精（随精流程 发电流程也在这里）
	 */
	public void calJing(ActionDataModel actions) {
		calScore(actions);
		// 随精玩法处理
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			sendActionToClient(actions.bulidPbActionData());
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					sendSuiJin();
				}
			}, actions.actionType.size() * 2000, TimeUnit.MILLISECONDS);
		} else if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_FA_DIAN)) {
			sendActionToClient(actions.bulidPbActionData());
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					generateTouZi();
					sendFaDian();
				}
			}, actions.actionType.size() * 2000, TimeUnit.MILLISECONDS);

		} else {
			// 暂时写死墩数，有特殊需求再进行随机
			jing[0] = this._repertory_card[110];
			jing[1] = _logic.getFuJing(jing[0]);

			if (DEBUG_CARDS_MODE) {
				jing[0] = 0x25;
				jing[1] = 0x26;
			}
			if (this.BACK_DEBUG_CARDS_MODE || this.magic_card_decidor != 0) {
				jing[0] = this.magic_card_decidor;
				jing[1] = _logic.getFuJing(jing[0]);
			}
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHANGXIA_FANG_JING)) {
				this.xiaJing[0] = this._repertory_card[111];
				this.xiaJing[1] = _logic.getFuJing(this.xiaJing[0]);
			}
			for (int i = 0; i < 2; i++) {
				this._logic.add_magic_card_index(this._logic.switch_to_card_index(jing[i]));
			}

			fanJing(actions.bulidPbActionData(ActionData_NC.newBuilder()));
		}

	}

	/**
	 * 添加特殊值
	 * 
	 * @param zheng
	 * @param fu
	 */
	public void addEspecialCards(int zheng, int fu) {
		GRR._especial_show_cards[GRR._especial_card_count] = zheng;
		GRR._especial_card_count++;
		GRR._especial_show_cards[GRR._especial_card_count] = fu;
		GRR._especial_card_count++;
	}

	/**
	 * 动画发送
	 * 
	 * @param actions
	 */
	public void sendActionToClient(ActionData_NC.Builder actions) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(Constants_MJ_NANCHANG.RESPONSE_ACTION_ANIMATION);
		this.load_common_status(roomResponse);
		roomResponse.setCommResponse(PBUtil.toByteString(actions));
		RoomUtil.send_response_to_room(this, roomResponse);
		if (this.GRR != null) {
			this.GRR.add_room_response(roomResponse);
		}
	}

	/**
	 * 动画发送
	 * 
	 * @param actions
	 */
	public void sendActionToClient(ActionData_NC.Builder actions, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(Constants_MJ_NANCHANG.RESPONSE_ACTION_ANIMATION);
		this.load_common_status(roomResponse);
		roomResponse.setCommResponse(PBUtil.toByteString(actions));
		RoomUtil.send_response_to_player(this, seat_index, roomResponse);
	}

	/**
	 * 发电数据
	 */
	public void sendFaDian() {
		int zhengCard = getFaDainCards(faDianCount, false);
		int fuCard = _logic.getFuJing(zhengCard);
		faDianCount++;
		_game_status = Constants_MJ_NANCHANG.GS_MJ_FA_DIAN;
		operate_player_status();
		ActionDataModel dataModel = calFaDian(zhengCard, fuCard, Constants_MJ_NANCHANG.ACTION_FA_DIAN, true);
		ActionData_NC.Builder data = dataModel.bulidPbActionData();
		// 停牌判断
		ActionJinData actionJingData = dataModel.getActionJingDatas().get(0);
		calScore(dataModel);
		if (actionJingData.getTingPai()) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (getJingArray(zhengCard)) {
					data.getActionJingDatasBuilder(0).setIsTing(actionJingData.getTingPaiForIndex(i));
				}
				tingPai[i] = actionJingData.getTingPaiForIndex(i);
				sendActionToClient(data, i);
			}
		} else {
			sendActionToClient(data);
		}
	}

	/**
	 * 发电数据不停
	 */
	public void sendFaDianBuTing() {
		int zhengCard = getFaDainCards(faDianCount - 1, true);
		int fuCard = _logic.getFuJing(zhengCard);
		_game_status = Constants_MJ_NANCHANG.GS_MJ_FA_DIAN;
		operate_player_status();
		ActionDataModel dataModel = calFaDian(zhengCard, fuCard, Constants_MJ_NANCHANG.ACTION_FA_DIAN, false);
		calScore(dataModel);
		// 不停不需要骰子动画
		dataModel.getActionJingDatas().get(0).setTouZi(false);
		dataModel.getActionJingDatas().get(0).setTouZiOne(-1);
		dataModel.getActionJingDatas().get(0).setTouZiTwo(-1);
		sendActionToClient(dataModel.bulidPbActionData());
	}

	/**
	 * 指定玩家的发电数据
	 * 
	 * @param seat_index
	 * @param checkTing
	 */
	public void sendFaDian(int seat_index, boolean checkTing, int count) {
		int zhengCard = getFaDainCards(count, true);
		int fuCard = _logic.getFuJing(zhengCard);
		ActionDataModel dataModel = calFaDian(zhengCard, fuCard, Constants_MJ_NANCHANG.ACTION_FA_DIAN, true);
		ActionData_NC.Builder data = dataModel.bulidPbActionData();
		// 停牌判断
		ActionJinData actionJingData = dataModel.getActionJingDatas().get(0);
		if (actionJingData.getTingPai()) {
			// 已操作玩家不发送停牌按钮
			if (tingPai[seat_index]) {
				data.getActionJingDatasBuilder(0).setIsTing(actionJingData.getTingPaiForIndex(seat_index));
			}
		}
		sendActionToClient(data, seat_index);
	}
	
	public int getGameScore(int seat_index){
		int gameScore = 0;
		if(xiaoJuScore  == null ){
			return gameScore;
		}
		Map<Integer, Integer> map = xiaoJuScore.get(seat_index+1);
		for (Entry<Integer, Integer> entry : map.entrySet()) {

			if (entry.getKey() == Constants_MJ_NANCHANG.PALOU
					|| entry.getKey() == Constants_MJ_NANCHANG.PIAOJING
					|| entry.getKey() == Constants_MJ_NANCHANG.FAFENG 
					|| entry.getKey() == Constants_MJ_NANCHANG.CHAOZHUANG 
					|| entry.getKey() == Constants_MJ_NANCHANG.GANGSCORE) {
				continue;
			}
			// 胡分累加已在结算已完成：爬楼玩法在爬楼处理已完成
			if (entry.getKey() != Constants_MJ_NANCHANG.HUSCORE) {
				gameScore+= entry.getValue().intValue() ;
			}
		}
		return gameScore;
	}
	

	/**
	 * 发电禁牌判断
	 * 
	 * @param card
	 * @param seat_index
	 * @return
	 */
	public boolean getJingArray(int card) {
		for (int u = 0; u < getTablePlayerNumber(); u++) {
			for (int i = 0; i < tingCount[u]; i++) {
				if (card == tingCard[u][i]) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 封装发电数据
	 * 
	 * @param zheng
	 * @param fu
	 * @param type
	 * @return
	 */
	public ActionDataModel calFaDian(int zheng, int fu, int type, boolean checkTing) {
		List<Integer> touZis = new ArrayList<Integer>();
		touZis.add(touZi[faDianCount - 1][0]);
		touZis.add(touZi[faDianCount - 1][1]);
		ActionDataModel dataModel = new ActionDataModel();

		dataModel.addActionJinData(new ActionJinData(getPlayerCard(), getTablePlayerNumber(), zheng, fu, faDianCount,
				GameConstants.INVALID_SEAT, touZis, null, getBaWangJingType()));
		// 停牌检测
		if (checkTing) {
			if (dataModel.actionJingDatas.get(0).getTingPai()) {
				if (getJingArray(zheng)) {
					type = Constants_MJ_NANCHANG.ACTION_TING_PAI;
					_game_status = Constants_MJ_NANCHANG.GS_MJ_TING_PAI;
				}
			}
		}
		dataModel.addActionType(type);
		return dataModel;
	}

	/**
	 * 得到玩家手牌数据
	 * 
	 * @return
	 */
	public int[][] getPlayerCard() {
		int[][] playerCardData = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			_logic.switch_to_cards_data(GRR._cards_index[i], cards);
			playerCardData[i] = cards;
		}
		return playerCardData;
	}

	/**
	 * 根据牌值获取index
	 * 
	 * @param playerCards
	 * @return
	 */
	public int[][] getPlayerCardIndex(int[][] playerCards) {
		int[][] index = new int[getTablePlayerNumber()][GameConstants.MAX_INDEX];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int u = 0; u < playerCards[i].length; u++) {
				if (playerCards[i][u] > 0) {
					index[i][_logic.switch_to_card_index(playerCards[i][u])]++;
				}
			}
		}
		return index;
	}

	/**
	 * 计算分数
	 * 
	 * @param card
	 * @return
	 */
	public int calCardScore(int card) {
		if (card == jing[0]) {
			return 2;
		}
		if (card == jing[1]) {
			return 1;
		}
		return 0;
	}

	/**
	 * 冲关包赔玩家
	 * 
	 * @param seat_index
	 * @return
	 */
	public int checkChongGuangReason(int seat_index) {
		int ownFen = 0;
		int otherFen[] = new int[getTablePlayerNumber()]; // 别人提供
		int frist_index = GameConstants.INVALID_SEAT;
		// 吃碰组合 -->区分别人提供
		for (int w = 0; w < GRR._weave_items[seat_index].length; w++) {
			WeaveItem item = GRR._weave_items[seat_index][w];
			int kind = item.getWeave_kind();
			int card = item.center_card;
			int provideItem = item.provide_player;
			if (kind == GameConstants.WIK_GANG) {
				int score = calCardScore(card);
				provideItem = item.provide_player_before;
				if (score > 0) {
					if (provideItem != seat_index) {
						otherFen[provideItem] += score;
						ownFen += 3 * score;
					} else {
						ownFen += 4 * score;
					}
				}
			} else if (kind == GameConstants.WIK_PENG) {
				int score = calCardScore(card);
				if (score > 0) {
					if (provideItem != seat_index) {
						otherFen[provideItem] += score;
						ownFen += 2 * score;
					} else {
						ownFen += 3 * score;
					}
				}
			} else if (kind == GameConstants.WIK_CENTER) {
				int cbRemoveCard[] = new int[] { card, card - 1, card + 1 };
				if (3 == _logic.get_card_color(card)) { // 风牌
					cbRemoveCard[2] = 0x30 + getFive(card);
					cbRemoveCard[1] = 0x30 + getFour(card);
					cbRemoveCard[0] = 0x30 + _logic.get_card_value(card);
				}
				for (int o = 0; o < cbRemoveCard.length; o++) {
					int score = calCardScore(cbRemoveCard[o]);
					if (score > 0) {
						if (o == 0) {
							otherFen[provideItem] += score;
						} else {
							ownFen += score;
						}
					}
				}
			} else if (kind == GameConstants.WIK_RIGHT) {
				int cbRemoveCard[] = new int[] { card, card - 1, card - 2 };
				if (3 == _logic.get_card_color(card)) { // 风牌
					cbRemoveCard[0] = 0x30 + _logic.get_card_value(card);
					cbRemoveCard[1] = 0x30 + getFour(card);
					cbRemoveCard[2] = 0x30 + getFive(card);
				}
				for (int o = 0; o < cbRemoveCard.length; o++) {
					int score = calCardScore(cbRemoveCard[o]);
					if (score > 0) {
						if (o == 0) {
							otherFen[provideItem] += score;
						} else {
							ownFen += score;
						}
					}
				}
			} else if (kind == GameConstants.WIK_LEFT) {
				int cbRemoveCard[] = new int[] { card, card + 1, card + 2 };
				if (3 == _logic.get_card_color(card)) { // 风牌
					cbRemoveCard[0] = 0x30 + _logic.get_card_value(card);
					cbRemoveCard[1] = 0x30 + getFour(card);
					cbRemoveCard[2] = 0x30 + getFive(card);
				}
				for (int o = 0; o < cbRemoveCard.length; o++) {
					int score = calCardScore(cbRemoveCard[o]);
					if (score > 0) {
						if (o == 0) {
							otherFen[provideItem] += score;
						} else {
							ownFen += score;
						}
					}
				}
			}
			if (frist_index == GameConstants.INVALID_SEAT && otherFen[provideItem] > 0) {
				frist_index = provideItem;
			}
		}

		// 手牌-->区分胡牌
		int operateCard = GRR._chi_hu_card[seat_index][0];
		int[] playerCardIndex = Arrays.copyOf(GRR._cards_index[seat_index], GRR._cards_index[seat_index].length);
		if (operateCard != 0 && provide_index == seat_index) {
			if (calCardScore(operateCard) > 0) {
				otherFen[provide_index] += calCardScore(operateCard);
				playerCardIndex[_logic.switch_to_card_index(operateCard)]--;
			}

		}
		int cards[] = new int[GameConstants.MAX_COUNT];
		_logic.switch_to_cards_data(playerCardIndex, cards);
		for (int c = 0; c < cards.length; c++) {
			if (cards[c] > 0) {
				ownFen += calCardScore(cards[c]);
			}
		}

		// 废牌堆
		for (int j = 0; j < 55; j++) {
			int card = getRealCard(GRR._discard_cards[seat_index][j]);
			if (card > 0) {
				ownFen += calCardScore(card);
			}
		}

		int score = 0;
		for (int j = 0; j < getTablePlayerNumber(); j++) {
			score += otherFen[j];
		}
		if (ownFen < 5 && score + ownFen >= 5) {
			return frist_index;
		}else if(ownFen >= 5 && score > 0 ){
			return frist_index;
		}
		return GameConstants.INVALID_SEAT;
	}

	/**
	 * 得到玩家所有牌数据
	 * 
	 * @return
	 */
	public int[][] getPlayerCardAll() {
		int[][] playerCardData = new int[getTablePlayerNumber()][70];
		int playerCardCount[] = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			_logic.switch_to_cards_data(GRR._cards_index[i], cards);

			for (int c = 0; c < cards.length; c++) {
				if (cards[c] > 0) {
					playerCardData[i][playerCardCount[i]++] = cards[c];
				}
			}

			// 废牌堆
			for (int j = 0; j < 55; j++) {
				playerCardData[i][playerCardCount[i]++] = GRR._discard_cards[i][j];
			}

			// 吃碰组合
			for (int w = 0; w < GRR._weave_items[i].length; w++) {
				WeaveItem item = GRR._weave_items[i][w];
				int kind = item.getWeave_kind();
				int card = item.center_card;
				if (kind == GameConstants.WIK_GANG) {
					for (int o = 0; o < 4; o++) {
						playerCardData[i][playerCardCount[i]++] = card;
					}
				} else if (kind == GameConstants.WIK_PENG) {
					for (int o = 0; o < 3; o++) {
						playerCardData[i][playerCardCount[i]++] = card;
					}
				} else if (kind == GameConstants.WIK_CENTER) {
					int cbRemoveCard[] = new int[] { card, card - 1, card + 1 };
					if (3 == _logic.get_card_color(card)) { // 风牌
						cbRemoveCard[0] = 0x30 + getFive(card);
						cbRemoveCard[1] = 0x30 + getFour(card);
						cbRemoveCard[2] = 0x30 + _logic.get_card_value(card);
					}
					for (int o = 0; o < cbRemoveCard.length; o++) {
						playerCardData[i][playerCardCount[i]++] = cbRemoveCard[o];
					}
				} else if (kind == GameConstants.WIK_RIGHT) {
					int cbRemoveCard[] = new int[] { card, card - 1, card - 2 };
					if (3 == _logic.get_card_color(card)) { // 风牌
						cbRemoveCard[0] = 0x30 + getFive(card);
						cbRemoveCard[1] = 0x30 + getFour(card);
						cbRemoveCard[2] = 0x30 + _logic.get_card_value(card);
					}
					for (int o = 0; o < cbRemoveCard.length; o++) {
						playerCardData[i][playerCardCount[i]++] = cbRemoveCard[o];
					}
				} else if (kind == GameConstants.WIK_LEFT) {
					int cbRemoveCard[] = new int[] { card, card + 1, card + 2 };
					if (3 == _logic.get_card_color(card)) { // 风牌
						cbRemoveCard[0] = 0x30 + getFive(card);
						cbRemoveCard[1] = 0x30 + getFour(card);
						cbRemoveCard[2] = 0x30 + _logic.get_card_value(card);
					}
					for (int o = 0; o < cbRemoveCard.length; o++) {
						playerCardData[i][playerCardCount[i]++] = cbRemoveCard[o];
					}
				}
			}

		}
		return playerCardData;
	}

	/**
	 * 获取计算精分需要霸王精类型
	 * 
	 * @return
	 */
	public int getBaWangJingType() {
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_FANGBEI)) {
			return Constants_MJ_NANCHANG.BA_WANG_JING_TYPE_FANGBEI;
		}
		return Constants_MJ_NANCHANG.BA_WANG_JING_TYPE_JIA10;
	}

	/**
	 * 生成全部骰子
	 */
	public void generateTouZi() {
		int[] count = new int[13];
		for (int i = 0; i < 22;) {
			int tou1 = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
			int tou2 = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
			if (count[tou1 + tou2] < 2) {
				touZi[i][0] = tou1;
				touZi[i][1] = tou2;
				i++;
				count[tou1 + tou2]++;
			}
		}
	}

	/**
	 * 根据发电次数获取发电牌值
	 * 
	 * @param count
	 */
	public int getFaDainCards(int count, boolean buTing) {
		int[] touZiTemp = touZi[count];
		int touNum = touZiTemp[1] + touZiTemp[0];
		if (!buTing) {
			touZicount[touNum]++;
		}
		int cardIndex = touZicount[touNum] * touNum - 1;
		return _repertory_card[_all_card_len - cardIndex];
	}

	/**
	 * 随精流程
	 */
	public void sendSuiJin() {
		// 变更游戏状态及发送随精按钮
		_game_status = Constants_MJ_NANCHANG.GS_MJ_SUI_JING;
		operate_player_status();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 1;
			handler_refresh_player_data_room(i);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			operate_effect_action(i, GameConstants.Effect_Action_Other, 1,
					new long[] { Constants_MJ_NANCHANG.EFFECT_ACTION_XUAN_JING }, 1, i);
		}
	}

	public boolean handler_refresh_player_data_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 发送第一张牌
	 */
	public void sendFirstCard() {
		_send_card_count++;
		_send_card_data = _repertory_card[_all_card_len - GRR._left_card_count];
		if (DEBUG_CARDS_MODE) {
			_send_card_data = 0x23;
		}
		GRR._left_card_count--;
		_provide_player = GRR._banker_player;
		GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(_send_card_data)]++;
		operate_player_get_card(GRR._banker_player, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);
	}

	/**
	 * 封装翻精数据动画
	 */
	public void fanJing(ActionData_NC.Builder actions) {

		// 上下翻精
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHANGXIA_FANG_JING)) {
			actions.addActionType(Constants_MJ_NANCHANG.ACTION_SHANG_XIA_FANG);
			ActionJingDatas.Builder actionDatas = ActionJingDatas.newBuilder();
			actionDatas.addShowCenterCards(jing[0]);
			actionDatas.addShowCenterCards(xiaJing[0]);
			actions.addActionJingDatas(actionDatas);
			actions.addActionType(Constants_MJ_NANCHANG.ACTION_XIA_JING);
			actions.addActionJingDatas(new ActionJinData(getPlayerCard(), getTablePlayerNumber(), xiaJing[0],
					xiaJing[1], 0, GameConstants.INVALID_SEAT, null, null, getBaWangJingType()).bulidPbJingData());

			// 分数计算
			ActionDataModel dataModel = new ActionDataModel();
			dataModel.addActionType(Constants_MJ_NANCHANG.ACTION_XIA_JING);
			dataModel.addActionJinData(new ActionJinData(getPlayerCard(), getTablePlayerNumber(), xiaJing[0],
					xiaJing[1], 0, GameConstants.INVALID_SEAT, null, null, getBaWangJingType()));
			calScore(dataModel);
		} else {
			actions.addActionType(Constants_MJ_NANCHANG.ACTION_FAN_JING);
			ActionJingDatas.Builder actionDatas = ActionJingDatas.newBuilder();
			for (int i = 0; i < 2; i++) {
				actionDatas.addShowCenterCards(jing[i]);
			}
			actions.addActionJingDatas(actionDatas);
		}

		sendActionToClient(actions);
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				addEspecialCards(jing[0], jing[1]);
			}
		}, 3, TimeUnit.SECONDS);

		exe_dispatch_card_sui(GRR._banker_player, Constants_MJ_NANCHANG.DISPLAYER_TYPE_SUI);
	}

	public void changeCard(int cards[], int cardCount, int seat_index) {
		if (cards == null) {
			return;
		}
		int jingCards[] = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingCards = personJing[seat_index];
		}
		for (int j = 0; j < cards.length; j++) {
			if (cards[j] == 0) {
				continue;
			}
			if (cards[j] == jingCards[0] || cards[j] == jingCards[1]) {
				cards[j] |= Constants_MJ_NANCHANG.JING;
			} else {
				cards[j] |= Constants_MJ_NANCHANG.NORMAL;
			}
		}
	}

	/**
	 * 单个牌精牌改变
	 * 
	 * @param cards
	 * @param seat_index
	 * @return
	 */
	public int changeCard(int cards, int seat_index) {

		int jingCards[] = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingCards = personJing[seat_index];
		}
		if (cards == jingCards[0] || cards == jingCards[1]) {
			cards |= Constants_MJ_NANCHANG.JING;
		} else {
			cards |= Constants_MJ_NANCHANG.NORMAL;
		}
		return cards;
	}

	public boolean operate_auto_win_card(int seat_index, boolean isTurnOn) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SWITCH_AUTO_WIN_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsXiangGong(isTurnOn); // TODO false 表示隐藏 true 表示显示

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 获取杠分
	 * 
	 * @param card
	 * @return
	 */
	public int getGangJingScore(int card) {
		int score = 2;
		// 上精判断
		if (jing[0] == card || jing[1] == card) {
			score = 10;
		}
		// 其他下精牌判断
		for (int i = 0; i < xiaJingNumber; i++) {
			if (xiaJing[i] == card) {
				score = 10;
			}
		}
		// 其他精牌：同一首哥
		for (int i = 0; i < gang_jing_card.length; i++) {
			if (gang_jing_card[i] == card) {
				score = 10;
			}
		}

		return score;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;
		if(GRR._end_type != GameConstants.Game_End_RELEASE_PLAY || GRR._end_type != GameConstants.Game_End_RELEASE_RESULT){
			gameEndShowCards();
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		// 查牌数据
		this.setGameEndBasicPrama(game_end);
		// 小结算协议
		GameEndResponse_NC.Builder comm_end = GameEndResponse_NC.newBuilder();
		roomResponse.setLeftCardCount(0);
		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);

		if (GRR != null) {
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
				int index = -1;
				/*if (GRR._end_type != GameConstants.Game_End_DRAW
						&& GRR._end_type != GameConstants.Game_End_RELEASE_PLAY) {
					index = GRR._banker_player;
					//jing = personJing[index];
				}*/
				for(int i = 0; i < getTablePlayerNumber(); i++){
					if(GRR._win_order[i] == 1){
						index = i;
						break;
					}
				}
				if(index > -1){
					RoomResponse.Builder roomResponseJing = RoomResponse.newBuilder();
					roomResponseJing.setType(Constants_MJ_NANCHANG.RESPONSE_JING_DATA);
					roomResponseJing.clearCardsList();
					buildJingData(index, roomResponseJing);
					this.send_response_to_room(roomResponseJing);
				}
			}

			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}
			GRR._end_type = reason;

			for (int j = 0; j < 2; j++) {
				comm_end.addZhengJing(this.jing[j]);
			}
			// 结算处理剩余精牌
			for (int j = 0; j < this.xiaJingNumber; j++) {
				this.xiaJing[j * 2] = this._repertory_card[this._all_card_len - (this.GRR._left_card_count - j)];
				this.xiaJing[j * 2 + 1] = _logic.getFuJing(this.xiaJing[j * 2]);
			}
			GRR._end_type = reason;
			List<Integer> other = null;
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SXZZYY)) {
				other = new ArrayList<Integer>();
				other.add(jing[0]);
				for (int j = 0; j < this.xiaJingNumber; j++) {
					other.add(this.xiaJing[j * 2]);
				}
			}

			ActionDataModel actionModel = new ActionDataModel();
			// 上精 start
			actionModel.addActionType(Constants_MJ_NANCHANG.ACTION_SHANG_JING);
			// 打出去的癞子，如果被别人碰杠胡，导致冲关，需要一人承包冲关分数。
			ActionJinData jinData = new ActionJinData(getPlayerCardAll(), getTablePlayerNumber(), jing[0], jing[1], -1,
					seat_index, null, other, getBaWangJingType());
			for (int j = 0; j < jinData.jingInfos.size(); j++) {
				// 冲关玩家检测
				if (jinData.jingInfos.get(j).getChongGuanScore() > 0) {
					// 检测牌型是否有别人提供的导致冲关
					int index = checkChongGuangReason(j);
					if (index != GameConstants.INVALID_SEAT) {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == index || i == j) {
								continue;
							}
							
							int score = jinData.jingInfos.get(j).getOtherDelScore(); // 减分
							jinData.jingInfos.get(i)
									.setEveryJingScore(jinData.jingInfos.get(i).getEveryJingScore() + score);
							jinData.jingInfos.get(index)
									.setEveryJingScore(jinData.jingInfos.get(index).getEveryJingScore() - score);
						}
					}
				}
			}

			actionModel.addActionJinData(jinData);
			// 上精 end

			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SXZZYY)) {
				for (int j = 0; j < this.xiaJingNumber; j++) {
					int type = Constants_MJ_NANCHANG.ACTION_XIA_JING + j;
					buildGameEndData(actionModel, type, this.xiaJing[j * 2], this.xiaJing[j * 2 + 1], other);
				}
			}
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHAI_YUE_LIANG)) {
				buildZhaoJingZi(actionModel, Constants_MJ_NANCHANG.ACTION_SHAI_YUE_LIANG);
			}
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_MAI_LEI)) {
				this.xiaJing[0] = this._repertory_card[111];
				this.xiaJing[1] = _logic.getFuJing(this.xiaJing[0]);
				buildGameEndData(actionModel, Constants_MJ_NANCHANG.ACTION_MAI_DI_LEI, xiaJing[0], xiaJing[1], null);
			}

			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_TON_YI_SHOU_GE)) {
				int card = this._repertory_card[112];
				if (card >= 0x31 && card <= 0x34) {
					int zheng = card;
					for (int k = 0; k < 4; k++) {
						int fu = _logic.getFuJing(zheng);
						gang_jing_card[gang_jing_count++] = zheng;
						gang_jing_card[gang_jing_count++] = fu;
						buildGameEndData(actionModel, Constants_MJ_NANCHANG.ACTION_TONG_YI_SHOU_GE, zheng, fu, null);
						zheng = fu;
					}
				} else if (card > 0x34) {
					int zheng = card;
					for (int k = 0; k < 3; k++) {
						int fu = _logic.getFuJing(zheng);
						gang_jing_card[gang_jing_count++] = zheng;
						gang_jing_card[gang_jing_count++] = fu;
						buildGameEndData(actionModel, Constants_MJ_NANCHANG.ACTION_TONG_YI_SHOU_GE, zheng, fu, null);
						zheng = fu;
					}
				} else {
					int val = _logic.get_card_value(card);
					for (int k = 0; k < 27; k++) {
						if (_logic.get_card_value(_logic.switch_to_card_data(k)) != val) {
							continue;
						}
						int zheng = _logic.switch_to_card_data(k);
						int fu = _logic.getFuJing(zheng);
						gang_jing_card[gang_jing_count++] = zheng;
						gang_jing_card[gang_jing_count++] = fu;
						buildGameEndData(actionModel, Constants_MJ_NANCHANG.ACTION_TONG_YI_SHOU_GE, zheng, fu, null);
					}
				}
			}
			calScore(actionModel);

			float lGangScore[] = new float[this.getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (GRR._end_type != GameConstants.Game_End_DRAW
						&& GRR._end_type != GameConstants.Game_End_RELEASE_PLAY) { // 荒庄荒杠
					// 杠精分
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
							int score = getGangJingScore(GRR._weave_items[i][j].center_card);
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								if (k == i) {
									continue;
								}

								lGangScore[i] += score;
								lGangScore[k] -= score;
							}
						}
					}
				}
				if (GRR._end_type == GameConstants.Game_End_RELEASE_PLAY) {
					this.isLiuJu = true;
				}
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			// 房主确认
			int fangZhu = -1;
			for (Player p : get_players()) {
				if (p == null) {
					continue;
				}
				if (p.getAccount_id() == getRoom_owner_account_id()) {
					fangZhu = p.get_seat_index();
				}
			}
			game_end.setShowBirdEffect(fangZhu);

			// 计算 杠分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				xiaoJuScore.get(i + 1).put(Constants_MJ_NANCHANG.HUSCORE, huScore[i]);
				xiaoJuScore.get(i + 1).put(Constants_MJ_NANCHANG.GANGSCORE, (int) lGangScore[i]);
				if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_CHAOZHUANG) && genZhuangCount > 0) {
					if (i == GRR._banker_player) {
						xiaoJuScore.get(i + 1).put(Constants_MJ_NANCHANG.CHAOZHUANG, -(getTablePlayerNumber() - 1) * 5);
					} else {
						xiaoJuScore.get(i + 1).put(Constants_MJ_NANCHANG.CHAOZHUANG, 5);
					}
				}
			}
			// 惩罚分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_result.ziba[i] > 0 && GRR._win_order[i] != 1
						&& has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_FA_DIAN)) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						if (k == i) {
							//GRR._game_score[k] -= (getTablePlayerNumber() - 1) * 5;
							xiaoJuScore.get(k + 1).put(Constants_MJ_NANCHANG.FAFENG, -(getTablePlayerNumber() - 1) * 5);
						} else {
							//GRR._game_score[k] += 5;
							xiaoJuScore.get(k + 1).put(Constants_MJ_NANCHANG.FAFENG, 5);
						}
					}
				}
			}

			// 连庄算分
			int bei = 1;
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_KUAI_XIA)
					|| has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_MANG_XIA)) {
				if (liangZhuangCount >= bei) {
					bei = liangZhuangCount;
				}
			}
			// 爬楼飘精分
			int score = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (!has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_BU_PA)) {
					xiaoJuScore.get(i + 1).put(Constants_MJ_NANCHANG.PALOU, bei);
					// 爬楼玩法胡牌分数
					GRR._game_score[(i)] *= bei;
				}
				
				if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_PIAOJING)) {
					if(GRR._win_order[i] == 1){
						int zheng = (int) Math.pow(4, piaoZheng[i]);
						int fu = (int) Math.pow(2, piaoFu[i]);
						score = zheng * fu;
					}
				}
			}
			if(score > 0){
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					//为了边色显示其他人为负数
					//if(GRR._win_order[i] == 1){
						xiaoJuScore.get(i + 1).put(Constants_MJ_NANCHANG.PIAOJING, score);
					/*}else{
						xiaoJuScore.get(i + 1).put(Constants_MJ_NANCHANG.PIAOJING, -score);
					}*/
				}
			}
			

			for (int i = 1; i <= getTablePlayerNumber(); i++) {
				// 结算数据
				GameEndInfo.Builder info = GameEndInfo.newBuilder();
				Map<Integer, Integer> map = xiaoJuScore.get(i);
				for (Entry<Integer, Integer> entry : map.entrySet()) {

					info.addTitle(entry.getKey());
					info.addScore(entry.getValue());
					if (entry.getKey() == Constants_MJ_NANCHANG.PALOU
							|| entry.getKey() == Constants_MJ_NANCHANG.PIAOJING) {
						continue;
					}
					// 胡分累加已在结算已完成：爬楼玩法在爬楼处理已完成
					if (entry.getKey() != Constants_MJ_NANCHANG.HUSCORE) {
						GRR._game_score[(i - 1)] += entry.getValue().intValue() * bei;
					}
				}
				comm_end.addGameEndInfoXia(info);
			}

			calDaJuScore();

			comm_end.addActionDataNC(actionModel.bulidPbActionData());

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(this.get_di_fen());

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addGameScore(GRR._game_score[i]);
				_player_result.game_score[i] += GRR._game_score[i];
				game_end.addHuResult(GRR._hu_result[i]);
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}
				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}
				game_end.addHuCardArray(hc);
			}
			
			this.load_player_info_data_end(roomResponse);
			
			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			this.set_result_describe(seat_index);
			comm_end.setResultDesc(seat_index >= 0 ? GRR._result_des[seat_index] : "");
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i],
						getJingData(i));

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}
				game_end.addLostFanShu(lfs);
			}

			// 记录上局精
			for (int i = 0; i < 2; i++) {
				shang_ju_jing[i] = jing[i];
			}
			if (!has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_BU_PA)) {
				if (shangJuZhuang == _cur_banker) {
					liangZhuangCount++;
					_player_result.biaoyan[shangJuZhuang] = liangZhuangCount;

				} else {
					_player_result.biaoyan[GRR._banker_player] = 0;
					if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_KUAI_XIA)) {
						liangZhuangCount = 0;
					} else {
						if (liangZhuangCount > 0) {
							liangZhuangCount--;
							if (liangZhuangCount > 0) {
								_player_result.biaoyan[_cur_banker] = 0;
							}
						}
					}
				}
			}
			shangJuZhuang = GRR._banker_player;
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
				for (int i = 1; i <= getTablePlayerNumber(); i++) {
					// 结算数据
					GameEndInfo.Builder info = GameEndInfo.newBuilder();
					Map<Integer, Integer> map = daJuScore.get(i);
					for (Entry<Integer, Integer> entry : map.entrySet()) {
						info.addTitle(entry.getKey());
						info.addScore(entry.getValue());
					}
					comm_end.addGameEndInfoDa(info);
				}
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
			for (int i = 1; i <= getTablePlayerNumber(); i++) {
				// 结算数据
				GameEndInfo.Builder info = GameEndInfo.newBuilder();
				Map<Integer, Integer> map = daJuScore.get(i);
				for (Entry<Integer, Integer> entry : map.entrySet()) {
					info.addTitle(entry.getKey());
					info.addScore(entry.getValue());
				}
				comm_end.addGameEndInfoDa(info);
			}
		}
		// 停牌状态重置
//		for (int i = 0; i < getTablePlayerNumber(); i++) {
//			_player_result.ziba[i] = 0;
//			handler_refresh_player_data_end(i);
//		}

		game_end.setEndType(real_reason);
		comm_end.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(comm_end));
		game_end.setCommResponse(PBUtil.toByteString(comm_end));
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);
		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		this.magic_card_decidor = 0;
		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		} else {
			clear_score_in_gold_room();
		}

		return false;
	}
	
	
	public boolean handler_refresh_player_data_end(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data_end(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		// this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index) {
		return 0;
	}
	
	
	/**
	 * 在玩家的前面显示出的牌 --- 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	//
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);
		
		
		if (to_player == GameConstants.INVALID_SEAT) {
			// 实时存储牌桌上的数据，方便回放时，任意进度读取
			operate_player_cards_record(seat_index, 2);
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				for (int i = 0; i < count; i++) {
					roomResponse.clearCardData();
					roomResponse.addCardData(changeCard(cards[i], p));
				}
				this.send_response_to_player(p, roomResponse);
			}
			GRR.add_room_response(roomResponse);
			return true;
			//return this.send_response_to_player(roomResponse);
		} else {
			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(changeCard(cards[i], to_player));
			}
			return this.send_response_to_player(to_player, roomResponse);
		}
		
//		if (seat_index == GameConstants.INVALID_SEAT) {
//			return false;
//		}
//		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
//		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
//		this.load_common_status(roomResponse);
//		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
//		roomResponse.setTarget(seat_index);
//		roomResponse.setCardType(type);// 出牌
//		roomResponse.setCardCount(count);
//		int flashTime = 60;
//		int standTime = 60;
//		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
//		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
//		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
//			flashTime = sysParamModel105.getVal3();
//			standTime = sysParamModel105.getVal4();
//		}
//		roomResponse.setFlashTime(flashTime);
//		roomResponse.setStandTime(standTime);
//
//		for (int i = 0; i < count; i++) {
//
//			roomResponse.addCardData(cards[i]);
//		}
//
//		if (to_player == GameConstants.INVALID_SEAT) {
//			// 实时存储牌桌上的数据，方便回放时，任意进度读取
//			operate_player_cards_record(seat_index, 2);
//
//			GRR.add_room_response(roomResponse);
//			return this.send_response_to_room(roomResponse);
//		} else {
//			return this.send_response_to_player(to_player, roomResponse);
//		}


	}

	/**
	 * 获取精牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public int[] getJingData(int seat_index) {
		int[] jingTemp = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingTemp = personJing[seat_index];
		}
		return jingTemp;
	}

	public int analyse_chi_hu_card_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index, int provide_index) {
		maxFan[seat_index] = 0;
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 替换精牌及计算精牌数量
		int[] jingTemp = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingTemp = personJing[seat_index];
		}

		int jingCount = 0;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
			if(cards_index[i] > 0){
				int card = _logic.switch_to_card_data(i);
				if (jingTemp[0] == card || jingTemp[1] == card) {
					jingCount++;
				}
			}
		}
		int cbChiHuKind = GameConstants.WIK_NULL;
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
			if (jingTemp[0] == cur_card || jingTemp[1] == cur_card) {
				jingCount++;
			}
		}
		// 精牌组装
		int[] magic_cards_index = bulidMagicArray(seat_index);
		int magic_card_count = 0;
		for (int i = 0; i < magic_cards_index.length; i++) {
			if (magic_cards_index[i] == GameConstants.INVALID_SEAT) {
				continue;
			}
			magic_card_count++;
		}
		// 飘精：必飘一精判断
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_BI_PIAO_YI_JING)) {
			if (!isPiaoJing(seat_index) && jingCount > 0) {
				return GameConstants.WIK_NULL;
			}
		}

		// 飘精：飘一精手上精牌才能为癞子
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_PIAOJING)) {
			if (!isPiaoJing(seat_index) && jingCount > 0) {
				magic_cards_index = new int[2];
				magic_card_count = 0;
				jingTemp = new int[2];
			}
		}

		// 判断是否需要还原胡牌
		boolean needHuanYuan = false;
		if (card_type == Constants_MJ_NANCHANG.HU_CARD_TYPE_JIE_PAO) {
			for (int k = 0; k < jingTemp.length; k++) {
				if (jingTemp[k] == cur_card) {
					needHuanYuan = true;
				}
			}
		}

		// 精钓
		boolean jingDiao = _logic.check_jing_diao(cbCardIndexTemp, weaveItems, weave_count, cur_card, magic_cards_index,
				magic_card_count, false);

		// 精吊不接炮
		if (jingDiao && !_logic.isZiMo(card_type) && isTianDiHu(card_type, chiHuRight, seat_index) == 0) {
			return GameConstants.WIK_NULL;
		}
		// 大七对 即碰胡
		int daQiDui = is_da_qi_dui(cbCardIndexTemp, weaveItems, weave_count, cur_card, chiHuRight, magic_cards_index,
				magic_card_count, card_type, seat_index, needHuanYuan);
		if (daQiDui > 0) {
			setMaxFan(seat_index, daQiDui, chiHuRight);
		}

		// 小七对
		int xiaoQiDui = is_qi_xiao_dui(cbCardIndexTemp, weaveItems, weave_count, cur_card, chiHuRight, seat_index,
				card_type, jingTemp, needHuanYuan);
		setMaxFan(seat_index, xiaoQiDui, chiHuRight);

		// WalkerGeek 十三烂 需要重写
		int shiSanLan = 0;
		if (!jingDiao) {
			shiSanLan = this.is_shi_san_lan(cbCardIndexTemp, weaveItems, weave_count, cur_card, chiHuRight, jingTemp,
					seat_index, card_type, needHuanYuan);
			setMaxFan(seat_index, shiSanLan, chiHuRight);
		}

		int pingHu = is_ping_hu(cards_index, cur_card, chiHuRight, magic_cards_index, magic_card_count, jingDiao,
				card_type, jingTemp, seat_index, needHuanYuan);
		// 平胡
		setMaxFan(seat_index, pingHu, chiHuRight);

		chiHuRight.copy(this.maxChr[seat_index]);

		if (card_type == Constants_MJ_NANCHANG.HU_CARD_TYPE_ZI_MO && seat_index == provide_index) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_ZI_MO);
		} else if (card_type == Constants_MJ_NANCHANG.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_JIE_PAO);
		} else if (card_type == Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA);
		}

		if (this.maxFan[seat_index] == 0) { // 没胡牌
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		return cbChiHuKind;
	}

	/**
	 * 组装精牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public int[] bulidMagicArray(int seat_index) {

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		Arrays.fill(magic_cards_index, 0);
		int jingTemp[] = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingTemp = personJing[seat_index];
		}
		for (int i = 0; i < 2; i++) {
			if (jingTemp[i] == 0) {
				continue;
			}
			magic_cards_index[i] = _logic.switch_to_card_index(jingTemp[i]);
		}
		return magic_cards_index;
	}

	public void setMaxFan(int seat_index, int fan, ChiHuRight chiHuRight) {
		if (fan > maxFan[seat_index]) {
			maxFan[seat_index] = fan;
			maxChr[seat_index].copy(chiHuRight);
		}
	}

	/**
	 * 判断七小对精钓
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param cur_card
	 * @return
	 */
	public boolean check_jing_diao_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card,
			int[] jingTemp, boolean needHuanYuan) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card > 0) {
			cbCardIndexTemp[this._logic.switch_to_card_index(cur_card)]--;
		}
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (is_qi_dui(cbCardIndexTemp, weaveItem, cbWeaveCount, _logic.switch_to_card_data(i), jingTemp,
					needHuanYuan)) {
				count++;
			}
		}
		if (count == GameConstants.MAX_ZI_FENG) {
			return true;
		}

		return false;
	}

	public boolean is_qi_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, int[] jingTemp,
			boolean needHuanYuan) {
		if (cbWeaveCount != 0) {
			return false;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		if (cur_card > 0) {
			cbCardIndexTemp[cbCurrentIndex]++;
		}

		int jingCount = 0, danCount = 0, duiCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			int card = this._logic.switch_to_card_data(i);

			if (card == jingTemp[0] || jingTemp[1] == card) {
				// WalkerGeek 对子部分是否需要还原部分
				if (needHuanYuan && cbCardCount > 0) {
					if (jingTemp[0] == cur_card || jingTemp[1] == cur_card) {
						jingCount = (cbCardCount - 1);
						danCount += 1;
					} else {
						jingCount += cbCardCount;
					}
				} else {
					jingCount += cbCardCount;
				}
			} else {
				danCount += cbCardCount % 2;
			}
			duiCount += cbCardCount / 2;
		}

		if (duiCount < 7) {
			if (jingCount < danCount) {
				return false;
			}
		}
		if (jingCount > 0) {
			return true;
		}

		return false;
	}

	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card,
			ChiHuRight chiHuRight, int seat_index, int card_type, int[] jingTemp, boolean needHuanYuan) {
		chiHuRight.set_empty();
		if (cbWeaveCount != 0) {
			return 0;
		}
		boolean isJingDiao = check_jing_diao_qi_xiao_dui(cards_index, weaveItem, cbWeaveCount, cur_card, jingTemp,
				false);
		// 精吊不接炮
		if (isJingDiao && !_logic.isZiMo(card_type) && isTianDiHu(card_type, chiHuRight, seat_index) == 0) {
			return 0;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int jingCount = 0, danCount = 0, duiCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			int card = this._logic.switch_to_card_data(i);

			if (card == jingTemp[0] || jingTemp[1] == card) {
				// WalkerGeek 对子部分是否需要还原部分
				if (needHuanYuan && cbCardCount > 0) {
					if (jingTemp[0] == cur_card || jingTemp[1] == cur_card) {
						jingCount = (cbCardCount - 1);
						danCount += 1;
					} else {
						jingCount += cbCardCount;
					}
				} else {
					jingCount += cbCardCount;
				}
			} else {
				danCount += cbCardCount % 2;
			}
			duiCount += cbCardCount / 2;
		}

		if (duiCount < 7) {
			if (jingCount < danCount) {
				return 0;
			}
		}
		int fan = Constants_MJ_NANCHANG.FAN_XIAO_QI_DUI;
		// 天胡
		int tianHuType = isTianDiHu(card_type, chiHuRight, seat_index);
		if (tianHuType > 0) {
			fan = Constants_MJ_NANCHANG.FAN_TIAN_HU;
		}

		chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_XIAO_QI_DUI);
		if (duiCount == 7) { // 德国七对
			if (isDeZhongDe(seat_index)) {
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE);
				fan *= Constants_MJ_NANCHANG.FAN_DE_ZHON_DE;
			} else {
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_GUO);
				fan *= Constants_MJ_NANCHANG.FAN_DE_GUO;
			}
		}
		if (isJingDiao) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_JING_DIAO);
			fan *= Constants_MJ_NANCHANG.FAN_JING_DIAO;
		}
		// 天胡确定
		if (fan == 20) {
			fan = 1;
		}
		fan = getAddFan(chiHuRight, fan, tianHuType);
		return fan;
	}

	public int is_ping_hu(int[] cards_index, int cur_card, ChiHuRight chiHuRight, int[] magic_cards_index,
			int magic_card_count, boolean isJingDiao, int card_type, int[] jingTemp, int seat_index,
			boolean needHuanYuan) {
		chiHuRight.set_empty();
		boolean can_win_with_out_magic = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, 0);
		int fan = 1;

		int tianHuType = isTianDiHu(card_type, chiHuRight, seat_index);
		if (tianHuType > 0) {
			fan = Constants_MJ_NANCHANG.FAN_TIAN_HU;
		}
		if (isJingDiao && (_logic.isZiMo(card_type) || tianHuType > 0)) {
			fan *= Constants_MJ_NANCHANG.FAN_JING_DIAO;
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_JING_DIAO);
		}
		if (!can_win_with_out_magic) {
			if (!has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_WU_PING_HU) || isJingDiao) {
				if (AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						magic_cards_index, magic_card_count, needHuanYuan)) {
					chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_PING_HU);
					fan *= Constants_MJ_NANCHANG.FAN_PING_HU;
					fan = getAddFan(chiHuRight, fan, tianHuType);
					return fan;
				}
			}
		} else {
			if (isDeZhongDe(seat_index)) {
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE);
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_PING_HU);
				fan *= Constants_MJ_NANCHANG.FAN_DE_ZHON_DE * Constants_MJ_NANCHANG.FAN_PING_HU;
				fan = getAddFan(chiHuRight, fan, tianHuType);
				return fan;
			} else {
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_GUO);
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_PING_HU);
				fan *= Constants_MJ_NANCHANG.FAN_DE_GUO * Constants_MJ_NANCHANG.FAN_PING_HU;
				fan = getAddFan(chiHuRight, fan, tianHuType);
				return fan;
			}
		}
		return 0;
	}

	/**
	 * 德中德判断
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isDeZhongDe(int seat_index) {
		int[] jingTemp = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingTemp = personJing[seat_index];
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			if (jingTemp[0] == 0 || jingTemp[1] == 0) {
				continue;
			}
			int card_index = _logic.switch_to_card_index(jingTemp[0]);
			int card_index1 = _logic.switch_to_card_index(jingTemp[1]);

			if (GRR._cards_index[i][card_index] > 0 || GRR._cards_index[i][card_index1] > 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 南昌麻将判断碰碰胡(大七对)
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param cbWeaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param magic_cards_index
	 * @param magic_card_count
	 * @param card_type
	 * @param seat_index
	 * @param needHuanYuan
	 * @return
	 */
	public int is_da_qi_dui(int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int cur_card,
			ChiHuRight chiHuRight, int[] magic_cards_index, int magic_card_count, int card_type, int seat_index,
			boolean needHuanYuan) {
		chiHuRight.set_empty();
		if (exist_eat(weaveItems, cbWeaveCount)) { // 有吃不可能是碰胡
			return 0;
		}
		int tempCardsIndex[] = Arrays.copyOf(cards_index, cards_index.length);
		if (cur_card > 0) {
			tempCardsIndex[_logic.switch_to_card_index(cur_card)]--;
		}
		boolean isJingDiao = false;
		// 精钓判断
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (AnalyseCardUtil.analyse_peng_hu_by_cards_index_nc(tempCardsIndex, i, magic_cards_index,
					magic_card_count, false)) {
				count++;
			}
		}
		if (count == GameConstants.MAX_ZI_FENG) {
			isJingDiao = true;
		}
		// 精吊不接炮
		if (isJingDiao && !_logic.isZiMo(card_type) && isTianDiHu(card_type, chiHuRight, seat_index) == 0) {
			return 0;
		}

		int fan = 1;
		int tianHuType = isTianDiHu(card_type, chiHuRight, seat_index);
		if (tianHuType > 0) {
			fan = Constants_MJ_NANCHANG.FAN_TIAN_HU;
		}

		// 德国大七对
		boolean pengHu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(tempCardsIndex,
				_logic.switch_to_card_index(cur_card), magic_cards_index, 0);
		if (isJingDiao) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_JING_DIAO);
			fan *= Constants_MJ_NANCHANG.FAN_JING_DIAO;
		}
		if (!pengHu) {
			if (AnalyseCardUtil.analyse_peng_hu_by_cards_index_nc(tempCardsIndex, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count, needHuanYuan)) {
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_PENG_HU);
				if (tianHuType == 0) {
					fan *= Constants_MJ_NANCHANG.FAN_DA_QI_DUI; // 大七对
				}
			}
		} else {
			if (isDeZhongDe(seat_index)) {
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_PENG_HU);
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE);
				fan *= Constants_MJ_NANCHANG.FAN_DA_QI_DUI * Constants_MJ_NANCHANG.FAN_DE_ZHON_DE; // 大七对*德国
			} else {
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_PENG_HU);
				chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_GUO);
				fan *= Constants_MJ_NANCHANG.FAN_DA_QI_DUI * Constants_MJ_NANCHANG.FAN_DE_GUO; // 大七对*德国
			}
		}

		// 天胡确定
		if (fan == 20) {
			fan = 1;
		}
		fan = getAddFan(chiHuRight, fan, tianHuType);
		return fan == 1 ? 0 : fan;
	}

	/**
	 * 天胡判断
	 * 
	 * @param card_type
	 * @param chiHuRight
	 * @param seat_index
	 * @return
	 */
	public int isTianDiHu(int card_type, ChiHuRight chiHuRight, int seat_index) {
		// 天胡
		if (_out_card_count == 0 && seat_index == _cur_banker) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_TIAN_HU);
			return 1;
		}

		// 地胡
		if (_out_card_count == 1 && seat_index != _cur_banker
				&& card_type == Constants_MJ_NANCHANG.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DI_HU);
			return 2;
		}
		return 0;
	}

	/**
	 * 通知客户端本人精牌数据
	 */
	public void sendJingData() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(Constants_MJ_NANCHANG.RESPONSE_JING_DATA);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.clearCardsList();
			buildJingData(i, roomResponse);
			this.send_response_to_player(i, roomResponse);
		}
	}

	/**
	 * 组装精数据
	 * 
	 * @param i
	 */
	public void buildJingData(int i, RoomResponse.Builder roomResponse) {
		int[] jingTemp = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingTemp = personJing[i];
		}
		for (int k = 0; k < 2; k++) {
			if (jingTemp[k] == 0) {
				roomResponse.addCardsList(-1);
			} else {
				roomResponse.addCardsList(jingTemp[k]);
			}
		}
	}

	/**
	 * 判断是否对精钓、德国、德中德进行加分
	 * 
	 * @param chiHuRight
	 * @param fan
	 * @return
	 */
	public int getAddFan(ChiHuRight chiHuRight, int fan, int isTianDiHu) {
		int count = 0;
		if (!(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_DE_GUO).is_empty())) {
			count++;
		}
		if (!(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_JING_DIAO).is_empty())) {
			count++;
		}
		if (!(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE).is_empty())) {
			count++;
		}
		if (count > 0 && fan > 1 && isTianDiHu == 0) {
			fan += count;
		}
		return fan;
	}

	/**
	 * 判断是否对精钓、德国、德中德进行加分
	 * 
	 * @param chiHuRight
	 * @param fan
	 * @return
	 */
	public int getAddFan(ChiHuRight chiHuRight) {
		int count = 0;
		if (!(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_DE_GUO).is_empty())) {
			count += 5;
		}
		if (!(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_JING_DIAO).is_empty())) {
			count += 5;
		}
		if (!(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE).is_empty())) {
			count += 5;
		}
		if (!(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_DI_HU).is_empty())
				|| !(chiHuRight.opr_and(Constants_MJ_NANCHANG.CHR_TIAN_HU).is_empty())) {
			count = 0;
		}

		return count;
	}

	/**
	 * 精钓时候只能炮胡精牌本身
	 * 
	 * @param isZimo
	 * @param isJingDiao
	 * @param cards_index
	 * @param cur_card
	 * @return
	 */
	public boolean jingDiaoCanPaoHu(boolean isZimo, boolean isJingDiao, int cards_index[], int cur_card) {
		if (!isZimo && isJingDiao) {
			int temp = cur_card & 0x000FF;
			if ((temp == this.jing[0] || temp == this.jing[1])) {
				if ((cards_index[this._logic.switch_to_card_index(temp)] > 0)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public int is_shi_san_lan(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card,
			ChiHuRight chiHuRight, int[] jingTemp, int seat_index, int card_type, boolean needHuanYuan) {
		chiHuRight.set_empty();
		if (cbWeaveCount > 0) {
			return 0;
		}

		int cardCount = 0, cardCountWithJing = 0;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX]; // 所有不带精牌的卡牌索引
		int cbCardIndexTempWithJing[] = new int[GameConstants.MAX_INDEX]; // 所有不带精牌的卡牌索引
		int cbIndexs[] = new int[GameConstants.MAX_COUNT]; // 所有带精牌的卡牌索引
		int cbReIndexs[] = new int[GameConstants.MAX_COUNT];
		boolean isDeGuo = true;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				int card = this._logic.switch_to_card_data(i);
				if (card == jingTemp[0] || jingTemp[1] == card) {
					if (cards_index[i] > 1) { // 精牌有两张以上相同的则肯定不是德国
						isDeGuo = false;
					}
				} else if (cards_index[i] > 1) {
					// 非精牌相同值的牌数量大于1 则肯定不是十三烂
					return 0;
				} else {
					cbCardIndexTemp[i] = cards_index[i];
					cbReIndexs[cardCount++] = card;
				}
				cbIndexs[cardCountWithJing++] = card;
				cbCardIndexTempWithJing[i] = cards_index[i];
			}
		}

		int preCard = cbReIndexs[0]; // 判断非精牌两个卡牌之间的间隔
		for (int i = 1; i < cardCount; i++) {
			if (this._logic.get_card_color(cbReIndexs[i]) == 3) { // 算到风牌的时候停止
				break;
			} else if (cbReIndexs[i] - preCard > 2) {
				preCard = cbReIndexs[i];
			} else {
				return 0;
			}
		}

		int fan = 1;
		int tianHuType = isTianDiHu(card_type, chiHuRight, seat_index);
		if (tianHuType > 0) {
			fan = Constants_MJ_NANCHANG.FAN_TIAN_HU;
		}
		if (isDeGuo) {
			// 有精牌的单牌数小于14 则肯定不是德国牌
			if (cardCountWithJing < GameConstants.MAX_COUNT) {
				isDeGuo = false;
			} else {
				preCard = cbIndexs[0];
				for (int i = 1; i < cardCountWithJing; i++) {
					if (this._logic.get_card_color(cbIndexs[i]) == 3) { // 算到风牌的时候停止
						break;
					} else if (cbIndexs[i] - preCard > 2) {
						preCard = cbIndexs[i];
					} else {
						isDeGuo = false;
						break;
					}
				}
			}
			if (isDeGuo) {
				if (isDeZhongDe(seat_index)) {
					chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE);
					fan *= Constants_MJ_NANCHANG.FAN_DE_ZHON_DE;
				} else {
					chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_DE_GUO);
					fan *= Constants_MJ_NANCHANG.FAN_DE_GUO;
				}
			}
		}

		boolean isQiXing = false;
		int begin = this._logic.switch_to_card_index(0x31);
		int end = this._logic.switch_to_card_index(0x37);
		int count = 0;
		for (int i = begin; i <= end; i++) {
			if (cbCardIndexTempWithJing[i] > 0) {
				count++;
			}
		}
		if (count >= 7) {
			isQiXing = true;
		}

		if (isQiXing) {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_QI_XING_SHI_SAN_LAN);
			fan *= Constants_MJ_NANCHANG.FAN_QI_XING_SHI_SAN_LAN;
		} else {
			chiHuRight.opr_or(Constants_MJ_NANCHANG.CHR_SHI_SAN_LAN);
			fan *= Constants_MJ_NANCHANG.FAN_SHI_SAN_LAN;
		}
		// 天胡确定
		if (fan == 20) {
			fan = 1;
		}
		fan = getAddFan(chiHuRight, fan, tianHuType);
		return fan == 1 ? 0 : fan;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		this.provide_index = provide_index;
		System.err.println("provide_index=="+provide_index);
		System.err.println("seat_index=="+seat_index);
		// 随精精牌数据
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jing = personJing[seat_index];
		}

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int di_fen = this.get_di_fen();
		int huFan = getFenShu(chr, seat_index);
		if (zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}
		boolean dianpao3 = false;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_DIANPAO3)) {
			dianpao3 = true;
		}
		boolean hasTian = hasTianHu(chr);
		if (dianpao3 && !zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				int tmpScore = 2;
				if (hasTian) {
					tmpScore = 1;
				}
				if (i == seat_index) {
					continue;
				}
				if (i != provide_index) {
					tmpScore = 1;
				}
				int score = tmpScore * huFan * di_fen;
				// 坐庄
				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					if (!hasTian) {
						score *= 2;
					}
				}
				int huScorePerson = score;
				if (i == provide_index) {
					huScorePerson += getAddFan(chr);
				}
				// 胡牌分
				huScore[i] -= huScorePerson;
				huScore[seat_index] += huScorePerson;
				// 计算飘精
				int piaoJing = getPiaoJing(seat_index);
				huScorePerson *= piaoJing;
				if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_FENGDING300)) {
					if (huScorePerson > 300) {
						huScorePerson = 300;
					}
				}
				GRR._game_score[i] -= huScorePerson;
				GRR._game_score[seat_index] += huScorePerson;
				piaoJingScore[i] -=  huScorePerson;
				piaoJingScore[seat_index] +=  huScorePerson;
			}
			this.operate_effect_action(provide_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[]{Constants_MJ_NANCHANG.CHR_FANG_PAO}, 1,
					GameConstants.INVALID_SEAT);
			GRR._provider[provide_index] = 1;
		} else if (zimo) {
			int tmpScore = 2;
			if (hasTian) {
				tmpScore = 1;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				int score = tmpScore * huFan * di_fen;
				if (i == seat_index) {
					continue;
				}
				// 坐庄
				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					if (!hasTian) {
						score *= 2;
					}
				}
				int huScorePerson = (score + getAddFan(chr));
				// 胡牌分
				huScore[i] -= huScorePerson;
				huScore[seat_index] += huScorePerson;
				int piaoJing = getPiaoJing(seat_index);
				huScorePerson *= piaoJing;
				if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_FENGDING300)) {
					if (huScorePerson > 300) {
						huScorePerson = 300;
					}
				}
				GRR._game_score[i] -= huScorePerson;
				GRR._game_score[seat_index] += huScorePerson;
				piaoJingScore[i] -=  huScorePerson;
				piaoJingScore[seat_index] +=  huScorePerson;
			}

		} else {
			int tmpScore = 2;
			if (hasTian) {
				tmpScore = 1;
			}
			int score = tmpScore * di_fen * huFan;
			if (provide_index == GRR._banker_player || seat_index == GRR._banker_player) {
				if (!hasTian) {
					score *= 2;
				}
			}

			// 胡牌分
			int huScorePerson = (score + getAddFan(chr));
			huScore[provide_index] -= huScorePerson;
			huScore[seat_index] += huScorePerson;

			int piaoJing = getPiaoJing(seat_index);
			huScorePerson *= piaoJing;
			if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_FENGDING300)) {
				if (huScorePerson > 300) {
					huScorePerson = 300;
				}
			}
			GRR._game_score[provide_index] -= huScorePerson;
			GRR._game_score[seat_index] += huScorePerson;
			piaoJingScore[provide_index] -=  huScorePerson;
			piaoJingScore[seat_index] +=  huScorePerson;
			GRR._provider[provide_index] = 1;
			
			this.operate_effect_action(provide_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[]{Constants_MJ_NANCHANG.CHR_FANG_PAO}, 1,
					GameConstants.INVALID_SEAT);
		}

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	/**
	 * 有没有天胡
	 * 
	 * @param chr
	 * @return
	 */
	public boolean hasTianHu(ChiHuRight chr) {
		if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_DI_HU).is_empty())
				|| !(chr.opr_and(Constants_MJ_NANCHANG.CHR_TIAN_HU).is_empty())) {
			return true;
		}
		return false;
	}

	protected void set_result_describe() {

	}

	public int getFenShu(ChiHuRight chr, int seat_index) {
		int fen = 1;

		boolean isTian = false;
		boolean needDouble = false;
		if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_TIAN_HU).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_TIAN_HU;
			isTian = true;
		} else if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_DI_HU).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_TIAN_HU;
			isTian = true;
		} else if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_QI_XING_SHI_SAN_LAN).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_QI_XING_SHI_SAN_LAN;
		} else if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_SHI_SAN_LAN).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_SHI_SAN_LAN;
		} else if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_XIAO_QI_DUI).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_DA_QI_DUI;
			needDouble = true;
		} else if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_PENG_HU).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_DA_QI_DUI;
			needDouble = true;
		}

		if (!isTian) {
			if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA).is_empty())) {
				fen *= Constants_MJ_NANCHANG.FAN_GANG_KAI;
			}
			if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_QIANG_GANG_HU).is_empty())) {
				fen *= Constants_MJ_NANCHANG.FAN_QIANG_GANG;
			}
		}

		if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_JING_DIAO).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_JING_DIAO;
		}
		if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE).is_empty())) {
			if (isTian) {
				fen *= Constants_MJ_NANCHANG.FAN_DE_GUO;
			} else {
				fen *= Constants_MJ_NANCHANG.FAN_DE_ZHON_DE;
			}

			if (needDouble) {
				fen *= 2;
			}
		}
		if (!(chr.opr_and(Constants_MJ_NANCHANG.CHR_DE_GUO).is_empty())) {
			fen *= Constants_MJ_NANCHANG.FAN_DE_GUO;
			if (needDouble) {
				fen *= 2;
			}
		}

		/*
		 * // 飘精算倍数 if
		 * (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_PIAOJING)) { fen *=
		 * Math.pow(4, piaoZheng[seat_index]); fen *= Math.pow(2,
		 * piaoFu[seat_index]); }
		 */
		return fen;
	}

	public int getPiaoJing(int seat_index) {
		int fen = 1;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_PIAOJING)) {
			fen *= Math.pow(4, piaoZheng[seat_index]);
			fen *= Math.pow(2, piaoFu[seat_index]);
		}
		return fen;
	}

	protected void set_result_describe(int seatIndex) {
		if (seatIndex < 0) {
			return;
		}
		int i = seatIndex;
		chr[i] = this.GRR._chi_hu_rights[i];
		StringBuffer des = new StringBuffer();

		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA).is_empty())) {
			des.append(",杠上开花");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_FANG_PAO).is_empty())) {
			des.append(",点炮");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_TIAN_HU).is_empty())) {
			des.append(",天胡");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_DI_HU).is_empty())) {
			des.append(",地胡");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_QIANG_GANG_HU).is_empty())) {
			des.append(",抢杠胡");
		} else if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_ZI_MO).is_empty())) {
			des.append(",自摸");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_JING_DIAO).is_empty())) {
			des.append(",精吊");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_DE_ZHON_DE).is_empty())) {
			des.append(",德中德");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_DE_GUO).is_empty())) {
			des.append(",德国");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_QI_XING_SHI_SAN_LAN).is_empty())) {
			des.append(",七星十三烂");
		} else if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_SHI_SAN_LAN).is_empty())) {
			des.append(",十三烂");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_XIAO_QI_DUI).is_empty())) {
			des.append(",小七对");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_PENG_HU).is_empty())) {
			des.append(",大七对");
		}
		if (!(chr[i].opr_and(Constants_MJ_NANCHANG.CHR_PING_HU).is_empty())) {
			des.append(",平胡");
		}

		Arrays.fill(GRR._result_des, Strings.isNullOrEmpty(des.toString()) ? "" : des.substring(1, des.length()));
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	/**
	 * 其他玩家对当前出牌信息的响应
	 * 
	 * @param seat_index
	 * @param card
	 * @param type
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;
		int action = GameConstants.WIK_NULL;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			playerStatus = _playerStatus[i];

			if (!this.isLast) {
				if (i == get_banker_next_seat(seat_index)) {
					action = _logic.check_chi_gzcg(GRR._cards_index[i], card);
					int number = 0;
					int cardChanged = card;
					int cardValue = _logic.get_card_value(card);
					if (3 == _logic.get_card_color(card)) {
						if (cardValue > 4) {
							switch (cardValue) {
							case 5:
								cardChanged |= 0x67000;
								break;
							case 6:
								cardChanged |= 0x57000;
								break;
							case 7:
								cardChanged |= 0x56000;
								break;
							}
						} else {
							int begin = _logic.switch_to_card_index(0x31);
							int c = _logic.switch_to_card_index(card);
							for (int k = begin; k < begin + 4; k++) {
								if (k == c) {
									continue;
								}
								if (GRR._cards_index[i][k] > 0) {
									number++;
								}
							}
							if (number == 2) {
								int value = 16 * 16 * 16;
								for (int k = begin; k < begin + 4; k++) {
									if (k == c) {
										continue;
									}
									if (GRR._cards_index[i][k] > 0) {
										if (number == 1) {
											value *= 16;
										}
										cardChanged += value * _logic.get_card_value(_logic.switch_to_card_data(k));
										number--;
									}
								}
							}
						}
					}
					if (number == 3) {
						int begin = _logic.switch_to_card_index(0x31);
						int c = _logic.switch_to_card_index(cardChanged);
						List<Integer> arr = new ArrayList<Integer>();

						for (int u = begin; u < begin + 4; u++) {
							int value = 16 * 16 * 16;
							if (u == c) {
								continue;
							}
							if (GRR._cards_index[i][u] == 0) {
								continue;
							}
							int cardChanged2 = cardChanged;

							cardChanged2 += value * _logic.get_card_value(_logic.switch_to_card_data(u));
							value *= 16;
							for (int k = u + 1; k < begin + 4; k++) {
								if (k == c) {
									continue;
								}
								int cardChanged1 = cardChanged2;
								if (GRR._cards_index[i][k] > 0) {
									arr.clear();
									arr.add(u);
									arr.add(k);
									cardChanged1 += value * _logic.get_card_value(_logic.switch_to_card_data(k));
									int types = getWIKAction(arr, c);
									_playerStatus[i].add_action(types);
									_playerStatus[i].add_chi_gzcg(cardChanged1, types, seat_index);
								}
							}
						}
					} else {
						if ((action & GameConstants.WIK_LEFT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi(cardChanged, GameConstants.WIK_LEFT, seat_index);
						}
						if ((action & GameConstants.WIK_CENTER) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi(cardChanged, GameConstants.WIK_CENTER, seat_index);
						}
						if ((action & GameConstants.WIK_RIGHT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi(cardChanged, GameConstants.WIK_RIGHT, seat_index);
						}
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				action = _logic.check_peng(GRR._cards_index[i], card);
				// 过碰
				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng = false;
						break;
					}
				}
				if (action != 0 && can_peng) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > Constants_MJ_NANCHANG.END) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}
			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_MJ_NANCHANG.HU_CARD_TYPE_JIE_PAO;
				action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						card_type, i, seat_index);
				if (action > 0) {
					// 接炮时，牌型分有变动，才能接炮胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param cardsIndex
	 * @param centerCardIndex
	 * @return
	 */
	public int getWIKAction(List<Integer> cardsIndex, int centerCardIndex) {
		int max = 0;
		int min = 0;

		for (Integer integer : cardsIndex) {
			if (integer > centerCardIndex) {
				max++;
			}
			if (integer < centerCardIndex) {
				min++;
			}
		}

		int action = GameConstants.WIK_NULL;
		if (max == 1 && min == 1) {
			action = GameConstants.WIK_CENTER;
		} else if (max == 2) {
			action = GameConstants.WIK_RIGHT;
		} else if (min == 2) {
			action = GameConstants.WIK_LEFT;
		}
		return action;
	}

	
	
	
	/**
	 * 胡牌类型映射,用于排序
	 * 
	 * @param type
	 * @return
	 */
	public int getHuActionType(int type) {
		switch (type) {
		//德国：德中德
		case Constants_MJ_NANCHANG.CHR_TIAN_HU:
		case Constants_MJ_NANCHANG.CHR_DI_HU:
			return 1;
		case Constants_MJ_NANCHANG.CHR_SHI_SAN_LAN:
		case Constants_MJ_NANCHANG.CHR_XIAO_QI_DUI:
		case Constants_MJ_NANCHANG.CHR_QI_XING_SHI_SAN_LAN:
		case Constants_MJ_NANCHANG.CHR_PENG_HU:
			return 2;
		case Constants_MJ_NANCHANG.CHR_DE_ZHON_DE:
			return 3;
		case Constants_MJ_NANCHANG.CHR_DE_GUO:
			return 4;
		case Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA:
			return 5;
		case Constants_MJ_NANCHANG.CHR_QIANG_GANG_HU:
			return 6;
		case Constants_MJ_NANCHANG.CHR_ZI_MO:
			return 7;
		case Constants_MJ_NANCHANG.CHR_JIE_PAO:
			return 7;
		}
		return 10;
	}
	
	/**
	 * 处理后发给客户端
	 * 
	 * @param type_list
	 * @param type_count
	 * @return
	 */
	public int sendClentType(long[] type_list, int type_count) {
		long type = 999;
		Map<Integer,Long> map = new HashMap<Integer,Long>();
		
		
		for(int i = 0; i < type_count; i++ ){
			map.put(getHuActionType((int)type_list[i]), type_list[i]);
		}
		for(Entry<Integer, Long> entry  : map.entrySet()){
			if(entry.getKey() < type){
				type = entry.getKey();
			}
		}
		if(type != 999L){
			Arrays.fill(type_list, 0);
			type_list[0] = map.get((int)type);
			type_count = 1;
		}
		
		return type_count;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int type_count = chr.type_count;
		long[] type_list = Arrays.copyOf(chr.type_list, chr.type_list.length);
		type_count = sendClentType(type_list, type_count);
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, type_count, type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);
		
		
		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards, getJingData(i));
			operate_player_cards(i, 0, null, 0, null);

			operate_player_cards(i, 0, new int[] {}, GRR._weave_count[i], GRR._weave_items[i]);

			// 显示胡牌
			int[] temp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
			cards = new int[GameConstants.MAX_COUNT];
			hand_card_count = _logic.switch_to_cards_data(temp_cards_index, cards, getJingData(i));
			if (i == seat_index) {
				cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
				hand_card_count++;
			}
			changeCard(cards, hand_card_count, seat_index);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
		// GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]++;

		return;
	}

	/**
	 * 流局显示胡牌
	 */
	public void gameEndShowCards(){
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards, getJingData(i));
			operate_player_cards(i, 0, null, 0, null);

			operate_player_cards(i, 0, new int[] {}, GRR._weave_count[i], GRR._weave_items[i]);

			// 显示胡牌
			int[] temp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
			cards = new int[GameConstants.MAX_COUNT];
			hand_card_count = _logic.switch_to_cards_data(temp_cards_index, cards, getJingData(i));
			
			changeCard(cards, hand_card_count, i);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
	}
	
	
	protected int get_di_fen() {
		int score = 1;
		return score;
	}

	protected int getGangScore() {
		return 2;
	}

	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int seat = banker_seat;
		do {
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null);
		return seat;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int seat = banker_seat;
		do {
			seat = (seat + 1) % this.getTablePlayerNumber();
		} while (get_players()[seat] == null);
		return seat;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card != GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
		}
		if (card > 0x100 && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= 0x100;
		}
		return card;
	}

	protected int get_seat(int nValue, int seat_index) {
		return (seat_index + (nValue - 1) % 4) % 4;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int max_ting_count = GameConstants.MAX_ZI_FENG;
		for (int i = 0; i < max_ting_count; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_NULL != analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, Constants_MJ_NANCHANG.HU_CARD_TYPE_ZI_MO, seat_index,
					(seat_index + 1) % this.getTablePlayerNumber())) {
				cards[count++] = cbCurrentCard;
			}
		}

		int[] magic_cards_index = bulidMagicArray(seat_index);
		int magic_card_count = 0;
		for (int i = 0; i < magic_cards_index.length; i++) {
			if (magic_cards_index[i] == GameConstants.INVALID_SEAT) {
				continue;
			}
			magic_card_count++;
		}
		int[] jingTemp = jing;
		if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_SHUIJING)) {
			jingTemp = personJing[seat_index];
		}
		if (count > 27
				|| _logic.check_jing_diao(cbCardIndexTemp, weaveItem, cbWeaveCount, 0, magic_cards_index,
						magic_card_count, false)
				|| check_jing_diao_qi_xiao_dui(cbCardIndexTemp, weaveItem, cbWeaveCount, 0, jingTemp, false)) {
			//必飘一精精钓情况下要做一个检测
			if(count >27){
				count = 1;
				cards[0] = -1;
			}else{
				// 飘精：必飘一精判断
				if (has_rule(Constants_MJ_NANCHANG.GAME_RULE_MJ_NC_BI_PIAO_YI_JING)) {
					if (!isPiaoJing(seat_index)) {
						return 0;
					}else{
						count = 1;
						cards[0] = -1;
					}
				}else{
					count = 1;
					cards[0] = -1;
				}
			}
			
		}

		return count;
	}

	public int getCardType(int seatIndex, int card, int[] cardIndex) {
		return 0;
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		this.changeCard(cards, card_count, seat_index);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player >= 1000 ? weaveitems[j].provide_player
						: weaveitems[j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_cards_toPlayer(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		this.changeCard(cards, card_count, seat_index);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player >= 1000 ? weaveitems[j].provide_player
						: weaveitems[j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		// this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean isJing(int card) {
		card = card & 0x000FF;
		if (this.jing[0] == card || card == this.jing[1]) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) {
				continue;
			}

			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					Constants_MJ_NANCHANG.CHR_QIANG_GANG_HU, i, seat_index);
			// 结果判断
			if (action != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
				chr.opr_or(Constants_MJ_NANCHANG.CHR_QIANG_GANG_HU); // 抢杠胡
				bAroseAction = true;
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	@Override
	protected void test_cards() {

//		 int cards[] = new int[] {
//		 0x11,0x11,0x12,0x12,0x14,0x14,0x15,0x15,0x16,0x16,0x13,0x13,0x17 };
		int cards[] = new int[] { 0x22, 0x23, 0x23, 0x23 };
//		 int cards[] = new int[] {
//		 0x11,0x11,0x12,0x12,0x13,0x13,0x14,0x14,0x15,0x15,0x16,0x16,0x17 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < cards.length; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps);
					debug_my_cards = null;
				}
			}

		}
	}

	private int getFive(int card) {
		return (card & 0xF0000) >> 16;
	}

	private int getFour(int card) {
		return (card & 0xF000) >> 12;
	}

	/**
	 * 取消精牌特殊值
	 * 
	 * @param card
	 * @return
	 */
	public int getRealCard(int card) {
		if (card > 256 && card < 500) {
			return card & 0xFF;
		} else {
			return card;
		}
	}

	/**
	 * 处理子游戏新定义的交互
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_NCMJ_SUIJING) {
			// 放弃选精
			if (room_rq.getOutCardsCount() == 0) {
				log_error("随精数据长度为空");
				return false;
			} else {
				int card = room_rq.getOutCardsList().get(0);
				if (!_logic.is_valid_card(card)) {
					log_error("牌型错误");
					return false;
				}
				int fuCard = _logic.getFuJing(card);
				personJing[seat_index][0] = card;
				personJing[seat_index][1] = fuCard;
			}
			// 变更已选择
			_player_result.nao[seat_index] = 2;

			handler_refresh_player_data_room(seat_index);
			// 手牌刷新
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards,
					getJingData(seat_index));

			if (seat_index == GRR._banker_player) {
				_logic.remove_card_by_data(cards, _send_card_data);
			}
			changeCard(cards, hand_card_count, seat_index);
			operate_player_cards_toPlayer(seat_index, hand_card_count, cards, 0, null);
			if (seat_index == GRR._banker_player) {
				_logic.remove_card_by_data(cards, _send_card_data);
				operate_player_get_card(seat_index, 1, new int[] { changeCard(_send_card_data, seat_index) }, seat_index);
			}

			boolean flag = true;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_result.nao[i] == 1) {
					flag = false;
				}
			}
			if (flag) {
				_game_status = Constants_MJ_NANCHANG.GS_MJ_PLAY;
				operate_player_status();
				exe_dispatch_card_sui(GRR._banker_player, Constants_MJ_NANCHANG.DISPLAYER_TYPE_SUI);
			}

		} else if (type == MsgConstants.REQUST_NCMJ_TINGPAI) {
			FaDianActionEnd cbr = PBUtil.toObject(room_rq, FaDianActionEnd.class);
			int daDainType = cbr.getTyep();
			int count = cbr.getCount();

			if (daDainType == 1) {
				// 记录停牌信息
				if (count != faDianCount) {
					return false;
				}
				if (count == 22) {
					// count_fa_dian 参数控制不要多次触发结算
					if (count_fa_dian == 0) {
						handler_game_finish(_cur_banker, GameConstants.Game_End_DRAW);
						count_fa_dian++;
					}
				} else {
					sendFaDian();
				}
			} else if (daDainType == 2) {
				tingPai[seat_index] = false;
				tingPaiIndex[seat_index] = 1;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (tingPai[i]) {
						return false;
					}
				}
				int target = GameConstants.INVALID_SEAT;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					int index = (_cur_banker + i + getTablePlayerNumber()) % getTablePlayerNumber();
					if (tingPaiIndex[index] == 1) {
						target = index;
						break;
					}
				}
				_player_result.ziba[target] = 1;
				handler_refresh_player_data_room(target);

				// 停牌
				_game_status = Constants_MJ_NANCHANG.GS_MJ_PLAY;
				int zheng = getFaDainCards(faDianCount - 1, true);
				int fu = _logic.getFuJing(zheng);
				addEspecialCards(zheng, fu);
				jing[0] = zheng;
				jing[1] = fu;

				this._logic.add_magic_card_index(this._logic.switch_to_card_index(zheng));
				this._logic.add_magic_card_index(this._logic.switch_to_card_index(fu));

				operate_player_status();
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(Constants_MJ_NANCHANG.RESPONSE_ACTION_TIP);
				roomResponse.setTarget(target);
				this.load_common_status(roomResponse);
				RoomUtil.send_response_to_room(this, roomResponse);

				exe_dispatch_card_sui(GRR._banker_player, Constants_MJ_NANCHANG.DISPLAYER_TYPE_SUI);
			} else if (daDainType == 3) {
				// 记录停牌信息
				tingPai[seat_index] = false;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (tingPai[i]) {
						return false;
					}
				}
				tingCard[seat_index][tingCount[seat_index]++] = getFaDainCards(faDianCount - 1, true);
				// 不停
				sendFaDianBuTing();
			}
		} else {
			logger.error("南昌麻将收到未定义协议号Type{},房间号{}", type, getRoom_id());
			return false;
		}

		return true;
	}

	/**
	 * 随精操作切换Handler
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card_sui(int seat_index, int type) {
		// 发牌
		this.set_handler(this._handler_dispath_card);
		this._handler_dispath_card.reset_status_send(seat_index, type, _send_card_data);
		this._handler.exe(this);
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {

			if (_game_status == Constants_MJ_NANCHANG.GS_MJ_SUI_JING
					|| _game_status == Constants_MJ_NANCHANG.GS_MJ_FA_DIAN
					|| _game_status == Constants_MJ_NANCHANG.GS_MJ_TING_PAI) {
				handler_player_be_in_room_table(seat_index);
			} else if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}
			handler_refresh_player_data(seat_index);
			// 重连后的托管状态
			be_in_room_trustee_match(seat_index);
		}

		if (GameConstants.GS_MJ_WAIT == _game_status) {
			RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
			tmp_roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_WHEN_GAME_FINISH);

			send_response_to_room(tmp_roomResponse);

			log_warn(" 小局结束的时候，断线重连了！！");
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;

		return true;
	}

	/**
	 * 随精发电状态重连
	 * 
	 * @param saet_index
	 */
	public boolean handler_player_be_in_room_table(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);
		load_common_status(roomResponse);

		tableResponse.setBankerPlayer(GRR._banker_player);
		tableResponse.setCurrentPlayer(_current_player);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item
						.setProvidePlayer(GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT); // 客户端说这里要+1000
																													// 不知道是什么瞎操作
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (i == _current_player) {
				tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));
			}
		}
		for (int i = 0; i < 2; i++) {
			tableResponse.addWinnerOrder(jing[i]);
		}
		tableResponse.setActionCard(xiaJingNumber);

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards,
				getJingData(seat_index));

		changeCard(hand_cards, GRR._card_count[seat_index], seat_index);

		if (seat_index == _current_player) {
			_logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = _playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _current_player)) {
			for (int j = 0; j < hand_card_count; j++) {
				int card = getRealCard(hand_cards[j]);
				for (int k = 0; k < out_ting_count; k++) {
					if (card == _playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(
					_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = _playerStatus[seat_index]._hu_cards;
		int ting_count = _playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		int real_card = _send_card_data;

		operate_player_get_card(_current_player, 1, new int[] { real_card }, seat_index);
		if (_game_status == Constants_MJ_NANCHANG.GS_MJ_SUI_JING) {
			if (_player_result.nao[seat_index] == 1) {
				operate_effect_action(seat_index, GameConstants.Effect_Action_Other, 1,
						new long[] { Constants_MJ_NANCHANG.EFFECT_ACTION_XUAN_JING }, 1, seat_index);
			}
		} else if (_game_status == Constants_MJ_NANCHANG.GS_MJ_FA_DIAN
				|| _game_status == Constants_MJ_NANCHANG.GS_MJ_TING_PAI) {
			sendFaDian(seat_index, true, faDianCount - 1);
		}
		return true;
	}

}
