package com.cai.game.mj.henan.wuzhi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_WuZhi;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.hh.util.Triple;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Wuzhi.AllWinDetails;
import protobuf.clazz.mj.Wuzhi.IntegerArray;
import protobuf.clazz.mj.Wuzhi.LsdyCards;
import protobuf.clazz.mj.Wuzhi.WinDetail;

@ThreeDimension
public class Table_WuZhi extends AbstractMJTable {

	private static final long serialVersionUID = 1L;

	public HandlerOutCardBaoTing_WuZhi _handler_out_card_bao_ting;
	public HandlerHun_WuZhi _handler_hun;

	/**
	 * 从什么地方走的分析胡牌算分，1表示从获取听牌数据的地方，2表示自摸或接炮的时候正常分析胡牌
	 */
	public int analyse_state;
	public static final int FROM_TING = 1;
	public static final int NORMAL = 2;

	/**
	 * 玩法是否已经报听
	 */
	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];
	/**
	 * 玩家本小局自摸次数
	 */
	public int[] ziMoCount = new int[getTablePlayerNumber()];
	/**
	 * 玩家本小局接炮次数
	 */
	public int[] jiePaoCount = new int[getTablePlayerNumber()];
	/**
	 * 玩家本小局放炮次数
	 */
	public int[] fangPaoCount = new int[getTablePlayerNumber()];
	/**
	 * 玩家本小局杠开次数
	 */
	public int[] gangKaiCount = new int[getTablePlayerNumber()];
	/**
	 * 本小局是否有人自摸
	 */
	public boolean hasZiMo = false;
	/**
	 * 本小局是否有人胡牌
	 */
	public boolean hasWin = false;

	public int da_dian_card;
	public int magic_card_index;
	/**
	 * 是否有亮四打一玩法
	 */
	public boolean hasLsdy = false;

	public int[][] lsdyCardsIndex = new int[getTablePlayerNumber()][GameConstants.MAX_INDEX];

	@SuppressWarnings("unchecked")
	public List<Integer>[] ziMoCardsData = new ArrayList[getTablePlayerNumber()];

	@SuppressWarnings("unchecked")
	public Map<Integer, List<Integer>>[] winCardsData = new HashMap[getTablePlayerNumber()];

	public WinDetailsModel[] winDetails = new WinDetailsModel[getTablePlayerNumber()];

	/**
	 * Triple.first：牌；Triple.second：提供者；Triple.third：分
	 */
	@SuppressWarnings("unchecked")
	public List<Triple<Integer, Integer, Integer>>[] jiePaoFangPao = new ArrayList[getTablePlayerNumber()];

	/**
	 * Triple.first：牌；Triple.second：提供者；Triple.third：分
	 */
	@SuppressWarnings("unchecked")
	public List<Triple<Integer, Integer, Integer>>[] ziMoTriple = new ArrayList[getTablePlayerNumber()];

	public int[] player_gang_score = new int[getTablePlayerNumber()];

	@SuppressWarnings("unchecked")
	public Set<Integer>[] alreadyWinCardsSet = new HashSet[getTablePlayerNumber()];

	public int[] totalZiMo = new int[getTablePlayerNumber()];
	public int[] totalJiePao = new int[getTablePlayerNumber()];
	public int[] totalFangPao = new int[getTablePlayerNumber()];

	public Table_WuZhi() {
		super(MJType.GAME_TYPE_MJ_HUO_JIA);
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUEST_HNWZ_DISPLAY_WIN_CARDS) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_HNWZ_DISPLAY_WIN_CARDS);

			LsdyCards.Builder lsdyCardsBuilder = LsdyCards.newBuilder();

			if (ziMoCardsData != null) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					IntegerArray.Builder iaBuilder = IntegerArray.newBuilder();
					for (int card : ziMoCardsData[i]) {
						if (i == seat_index) {
							iaBuilder.addCard(card);
						} else {
							iaBuilder.addCard(GameConstants.BLACK_CARD);
						}
					}
					lsdyCardsBuilder.addZiMoCards(iaBuilder);
				}
			}

			if (winCardsData != null) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (winCardsData[i] != null) {
						List<Integer> shangJia = winCardsData[i].get(0);
						List<Integer> duiJia = winCardsData[i].get(1);
						List<Integer> xiaJia = winCardsData[i].get(2);

						if (shangJia != null) {
							IntegerArray.Builder iaBuilder = IntegerArray.newBuilder();
							for (int card : shangJia) {
								iaBuilder.addCard(card);
							}
							lsdyCardsBuilder.addShangJiaProvidedCards(iaBuilder);
						}
						if (duiJia != null) {
							IntegerArray.Builder iaBuilder = IntegerArray.newBuilder();
							for (int card : duiJia) {
								iaBuilder.addCard(card);
							}
							lsdyCardsBuilder.addDuiJiaProvidedCards(iaBuilder);
						}
						if (xiaJia != null) {
							IntegerArray.Builder iaBuilder = IntegerArray.newBuilder();
							for (int card : xiaJia) {
								iaBuilder.addCard(card);
							}
							lsdyCardsBuilder.addXiaJiaProviedCards(iaBuilder);
						}
					}
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(lsdyCardsBuilder));

			send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(-1);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		roomResponse.clearWeaveItems();

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

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(ziMoCount[i] + gangKaiCount[i]); // 自摸次数
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);

			// 少人模式
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			// 处理玩家是否已经报听
			if (null != is_bao_ting) {
				room_player.setBiaoyan(is_bao_ting[i] ? 1 : 0);
			} else {
				room_player.setBiaoyan(0);
			}

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	public void process_next_banker() {
		if (GRR != null) {
			int pCount = getTablePlayerNumber();
			if (hasZiMo) {
				int maxZiMoCount = 0;
				for (int i = 0; i < pCount; i++) {
					int tmpCount = ziMoCount[i] + gangKaiCount[i];
					if (tmpCount > maxZiMoCount)
						maxZiMoCount = tmpCount;
				}

				int sameZiMoCount = 0;
				boolean bankerHasMostCount = false;
				for (int i = 0; i < pCount; i++) {
					int tmpCount = ziMoCount[i] + gangKaiCount[i];
					if (tmpCount == maxZiMoCount) {
						sameZiMoCount++;
						if (i == GRR._banker_player)
							bankerHasMostCount = true;
					}
				}

				if (sameZiMoCount == 1) {
					// 自摸次数最多的人成为下一小局的庄家
					for (int i = 0; i < pCount; i++) {
						int tmpCount = ziMoCount[i] + gangKaiCount[i];
						if (tmpCount == maxZiMoCount) {
							_cur_banker = i;
							break;
						}
					}
				} else {
					if (bankerHasMostCount) {
						_cur_banker = GRR._banker_player;
					} else {
						for (int i = 0; i < pCount; i++) {
							int realSeat = (i + GRR._banker_player) % pCount;
							if (ziMoCount[realSeat] + gangKaiCount[realSeat] == maxZiMoCount) {
								_cur_banker = realSeat;
								break;
							}
						}
					}
				}
			} else {
				if (jiePaoCount[GRR._banker_player] > 0) {
					_cur_banker = GRR._banker_player;
				} else {
					for (int x = 0; x < pCount; x++) {
						int i = (x + GRR._banker_player) % pCount;
						if (i == GRR._banker_player)
							continue;
						if (jiePaoCount[i] > 0) {
							_cur_banker = i;
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int lsdy_cards_index[]) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (lsdy_cards_index != null) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				cbCardIndexTemp[i] += lsdy_cards_index[i];
			}
		}

		ChiHuRight chr = new ChiHuRight();

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_WuZhi.HU_CARD_TYPE_ZI_MO, seat_index)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;

		if (this.getClass().getAnnotation(ThreeDimension.class) != null) {
			show_tou_zi(GRR._banker_player);
		}

		int count = getTablePlayerNumber();

		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			if (hasLsdy) {
				_logic.switch_to_cards_index_lsdy(repertory_card, have_send_count, send_count, GRR._cards_index[i], lsdyCardsIndex[i]);
			} else {
				_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			}

			have_send_count += send_count;
		}

		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		set_handler(_handler_out_card_bao_ting);
		_handler_out_card_bao_ting.reset_status(seat_index, card, type);
		_handler.exe(this);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, boolean has_feng,
			int lsdy_cards_index[]) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (lsdy_cards_index != null) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				cbCardIndexTemp[i] += lsdy_cards_index[i];
			}
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;
		if (has_feng) {
			max_ting_count = GameConstants.MAX_ZI_FENG;
		}

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_WuZhi.HU_CARD_TYPE_ZI_MO, seat_index)) {
				if (_logic.is_magic_card(cbCurrentCard))
					cbCurrentCard += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= 25) {
			if (hasLsdy) {
				int magicCardColor = _logic.get_card_color(da_dian_card);

				int paiSeCount = _logic.get_se_count(cbCardIndexTemp, weaveItem, cbWeaveCount);

				if (paiSeCount == 1 && count == max_ting_count) {
					// 手里有一色牌时 能全听 不能让玩家报听
					count = 0;
				} else if (paiSeCount == 2) {
					int queSeColor = get_que_se_color(cbCardIndexTemp, weaveItem, cbWeaveCount);
					if (queSeColor == magicCardColor) {
						if (count + 8 == max_ting_count)
							count = 0;
					} else {
						if (count + 9 == max_ting_count) {
							count = 0;
						}
					}
				}
			} else {
				if (count + 9 == max_ting_count) {
					// 没赖子时 如果除去缺的那门牌 能全听牌 不能让玩家报听
					count = 0;
				}
			}
		}

		return count;
	}

	public int get_que_se_color(int[] cards_index, WeaveItem[] weaveItems, int cbWeaveCount) {
		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		for (int i = 0; i < cbWeaveCount; i++) {
			int card_index = _logic.switch_to_card_index(weaveItems[i].center_card);
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				tmp_cards_index[card_index] += 4;
			} else {
				tmp_cards_index[card_index] += 3;
			}
		}
		boolean founded = true;
		for (int i = 0; i < 9; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (tmp_cards_index[i] > 0) {
				founded = false;
				break;
			}
		}
		if (founded)
			return 0;
		founded = true;
		for (int i = 9; i < 18; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (tmp_cards_index[i] > 0) {
				founded = false;
				break;
			}
		}
		if (founded)
			return 1;
		founded = true;
		for (int i = 9; i < 18; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (tmp_cards_index[i] > 0) {
				founded = false;
				break;
			}
		}
		if (founded)
			return 2;
		return 3;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_WuZhi();
		_handler_dispath_card = new HandlerDispatchCard_WuZhi();
		_handler_gang = new HandlerGang_WuZhi();
		_handler_out_card_operate = new HandlerOutCardOperate_WuZhi();
		_handler_out_card_bao_ting = new HandlerOutCardBaoTing_WuZhi();
		_handler_hun = new HandlerHun_WuZhi();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			lsdyCardsIndex[i] = new int[GameConstants.MAX_INDEX];
			ziMoCardsData[i] = new ArrayList<Integer>();
			winCardsData[i] = new HashMap<Integer, List<Integer>>();

			for (int j = 0; j < getTablePlayerNumber() - 1; j++)
				winCardsData[i].put(j, new ArrayList<Integer>());

			winDetails[i] = new WinDetailsModel();

			jiePaoFangPao[i] = new ArrayList<Triple<Integer, Integer, Integer>>();
			ziMoTriple[i] = new ArrayList<Triple<Integer, Integer, Integer>>();

			alreadyWinCardsSet[i] = new HashSet<Integer>();
		}

		ziMoCount = new int[getTablePlayerNumber()];
		jiePaoCount = new int[getTablePlayerNumber()];
		fangPaoCount = new int[getTablePlayerNumber()];
		gangKaiCount = new int[getTablePlayerNumber()];

		Arrays.fill(totalZiMo, 0);
		Arrays.fill(totalJiePao, 0);
		Arrays.fill(totalFangPao, 0);

		hasLsdy = has_rule(Constants_WuZhi.GAME_RULE_LIANG_SI_DA_YI);
	}

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			lsdyCardsIndex[i] = new int[GameConstants.MAX_INDEX];
			ziMoCardsData[i] = new ArrayList<Integer>();
			winCardsData[i] = new HashMap<Integer, List<Integer>>();

			for (int j = 0; j < getTablePlayerNumber() - 1; j++)
				winCardsData[i].put(j, new ArrayList<Integer>());

			winDetails[i] = new WinDetailsModel();

			jiePaoFangPao[i] = new ArrayList<Triple<Integer, Integer, Integer>>();
			ziMoTriple[i] = new ArrayList<Triple<Integer, Integer, Integer>>();

			alreadyWinCardsSet[i] = new HashSet<Integer>();
		}

		ziMoCount = new int[getTablePlayerNumber()];
		jiePaoCount = new int[getTablePlayerNumber()];
		fangPaoCount = new int[getTablePlayerNumber()];
		gangKaiCount = new int[getTablePlayerNumber()];

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;

		init_shuffle();

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE || debug_lsdy)
			test_cards();

		getLocationTip();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	// 获取庄家上家的座位
	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (getTablePlayerNumber() + seat - 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	// 获取庄家下家的座位
	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected boolean on_game_start() {
		is_bao_ting = new boolean[getTablePlayerNumber()];
		hasZiMo = false;
		hasWin = false;
		player_gang_score = new int[getTablePlayerNumber()];

		_game_status = GameConstants.GS_MJ_PLAY;
		_logic.clean_magic_cards();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		int lsdy_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		LsdyCards.Builder lsdyCardsBuilder = LsdyCards.newBuilder();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);

			if (hasLsdy) {
				int tmpCount = _logic.switch_to_cards_data(lsdyCardsIndex[i], lsdy_cards[i]);
				lsdyCardsBuilder.addCardsCount(tmpCount);

				IntegerArray.Builder cards = IntegerArray.newBuilder();
				for (int x = 0; x < tmpCount; x++) {
					cards.addCard(lsdy_cards[i][x]);
				}

				lsdyCardsBuilder.addCardsData(cards);
			}

			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			// 重新装载玩家信息
			load_player_info_data(roomResponse);

			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			if (hasLsdy) {
				roomResponse.setCommResponse(PBUtil.toByteString(lsdyCardsBuilder));
			}

			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);

		if (hasLsdy) {
			roomResponse.setCommResponse(PBUtil.toByteString(lsdyCardsBuilder));
		}

		GRR.add_room_response(roomResponse);

		if (hasLsdy) {
			exe_hun(_current_player);
		} else {
			exe_dispatch_card(_cur_banker, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	public boolean exe_hun(int seat_index) {
		set_handler(_handler_hun);
		_handler_hun.reset_status(seat_index);
		_handler_hun.exe(this);
		return true;
	}

	@Override
	public PlayerResultResponse.Builder process_player_result(int result) {
		huan_dou(result);

		int pCount = getTablePlayerNumber();

		for (int i = 0; i < pCount; i++) {
			_player_result.win_order[i] = -1;
		}

		int win_idx = 0;
		float max_score = 0;

		for (int i = 0; i < pCount; i++) {
			int winner = -1;
			float s = -999999;

			for (int j = 0; j < pCount; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (_player_result.game_score[j] > s) {
					s = _player_result.game_score[j];
					winner = j;
				}
			}

			if (s >= max_score) {
				max_score = s;
			} else {
				win_idx++;
			}
			if (winner != -1) {
				_player_result.win_order[winner] = win_idx;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < pCount; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < pCount; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addZiMoCount(totalZiMo[i]);
			player_result.addJiePaoCount(totalJiePao[i]);
			player_result.addDianPaoCount(totalFangPao[i]);

			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);

			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);
			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
		}

		player_result.setRoomId(getRoom_id());
		player_result.setRoomOwnerAccountId(getRoom_owner_account_id());
		player_result.setRoomOwnerName(getRoom_owner_name());
		player_result.setCreateTime(getCreate_time());
		player_result.setRecordId(get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(getCreate_player().getAccount_icon());
		room_player.setIp(getCreate_player().getAccount_ip());
		room_player.setUserName(getCreate_player().getNick_name());
		room_player.setSeatIndex(getCreate_player().get_seat_index());
		room_player.setOnline(getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(getCreate_player().getAccount_ip_addr());
		room_player.setSex(getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);

		if (getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(getCreate_player().locationInfor);
		}

		player_result.setCreatePlayer(room_player);

		return player_result;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);

		setGameEndBasicPrama(game_end);

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i] + player_gang_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int tmpCount = 0;
				int[] tmpCardsData = new int[4];
				if (hasLsdy) {
					tmpCount = _logic.switch_to_cards_data(lsdyCardsIndex[i], tmpCardsData);
				}
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < tmpCount; j++) {
					cs.addItem(tmpCardsData[j]);
				}
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
				game_end.addGameScore(GRR._game_score[i] + player_gang_score[i]); // 总分
				game_end.addGangScore(player_gang_score[i]); // 杠分
				game_end.addStartHuScore((int) GRR._game_score[i]); // 胡分
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		LsdyCards.Builder lsdyCardsBuilder = LsdyCards.newBuilder();
		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			AllWinDetails.Builder allWinDetailBuilder = AllWinDetails.newBuilder();
			for (WinDetailModel detailModel : winDetails[i].winDetailList) {
				WinDetail.Builder winDetailBuilder = WinDetail.newBuilder();
				winDetailBuilder.setType(detailModel.type);
				winDetailBuilder.setCard(detailModel.card);
				winDetailBuilder.setProvider(detailModel.provider);
				winDetailBuilder.setScore(detailModel.score);
				allWinDetailBuilder.addWinDetails(winDetailBuilder);
			}
			lsdyCardsBuilder.addWinDetailList(allWinDetailBuilder);
		}
		game_end.setCommResponse(PBUtil.toByteString(lsdyCardsBuilder));

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);

		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		if (effect_type == GameConstants.EFFECT_ACTION_TYPE_HU && effect_indexs[0] == GameConstants.WIK_ZI_MO)
			roomResponse.setOperateLen(ziMoCount[seat_index] + gangKaiCount[seat_index]);

		if (to_player == GameConstants.INVALID_SEAT) {
			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(to_player, roomResponse);
		}

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	public void process_chi_hu_player_operate() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (hasLsdy) {
				operate_player_cards_lsdy(i, 0, null, 0, null);
			} else {
				operate_player_cards(i, 0, null, 0, null);
			}

			int[] cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_ezhou(GRR._cards_index[i], cards);

			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(cards[j]))
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (is_bao_ting[_seat_index] == false && analyse_state == NORMAL) {
			return 0;
		}

		if (!_logic.is_valid_card(cur_card)) {
			return 0;
		}

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		if (hasLsdy && analyse_state == NORMAL) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				tmp_cards_index[i] = cards_index[i] + lsdyCardsIndex[_seat_index][i];
			}
		} else {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				tmp_cards_index[i] = cards_index[i];
			}
		}
		tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		if (!_logic.is_que_yi_se(tmp_cards_index, weaveItems, weave_count)) {
			return 0;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(tmp_cards_index, -1, magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return cbChiHuKind;
		}

		if (analyse_state == NORMAL && has_rule(Constants_WuZhi.GAME_RULE_YING_DUAN_MEN)) {
			boolean founded = false;
			for (int i = 0; i <= 3; i++) {
				boolean has_enough_card = true;
				for (int j = i; j <= i + 5; j++) {
					if (tmp_cards_index[j] == 0) {
						has_enough_card = false;
						break;
					}
				}
				if (has_enough_card) {
					int[] new_cards_index = Arrays.copyOf(tmp_cards_index, tmp_cards_index.length);
					for (int j = i; j <= i + 5; j++) {
						new_cards_index[j]--;
					}

					boolean can_lian_liu = AnalyseCardUtil.analyse_win_by_cards_index(new_cards_index, -1, magic_cards_index, magic_card_count);
					if (can_lian_liu) {
						founded = true;
						chiHuRight.opr_or(Constants_WuZhi.CHR_LIAN_LIU);
						break;
					}
				}
			}

			if (!founded) {
				for (int i = 9; i <= 12; i++) {
					boolean has_enough_card = true;
					for (int j = i; j <= i + 5; j++) {
						if (tmp_cards_index[j] == 0) {
							has_enough_card = false;
							break;
						}
					}
					if (has_enough_card) {
						int[] new_cards_index = Arrays.copyOf(tmp_cards_index, tmp_cards_index.length);
						for (int j = i; j <= i + 5; j++) {
							new_cards_index[j]--;
						}

						boolean can_lian_liu = AnalyseCardUtil.analyse_win_by_cards_index(new_cards_index, -1, magic_cards_index, magic_card_count);
						if (can_lian_liu) {
							founded = true;
							chiHuRight.opr_or(Constants_WuZhi.CHR_LIAN_LIU);
							break;
						}
					}
				}
			}

			if (!founded) {
				for (int i = 18; i <= 21; i++) {
					boolean has_enough_card = true;
					for (int j = i; j <= i + 5; j++) {
						if (tmp_cards_index[j] == 0) {
							has_enough_card = false;
							break;
						}
					}
					if (has_enough_card) {
						int[] new_cards_index = Arrays.copyOf(tmp_cards_index, tmp_cards_index.length);
						for (int j = i; j <= i + 5; j++) {
							new_cards_index[j]--;
						}

						boolean can_lian_liu = AnalyseCardUtil.analyse_win_by_cards_index(new_cards_index, -1, magic_cards_index, magic_card_count);
						if (can_lian_liu) {
							founded = true;
							chiHuRight.opr_or(Constants_WuZhi.CHR_LIAN_LIU);
							break;
						}
					}
				}
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_WuZhi.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_WuZhi.CHR_ZI_MO);
		} else if (card_type == Constants_WuZhi.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_WuZhi.CHR_JIE_PAO);
		} else if (card_type == Constants_WuZhi.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_WuZhi.CHR_JIE_PAO);
		} else if (card_type == Constants_WuZhi.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_WuZhi.CHR_ZI_MO);
			if (has_rule(Constants_WuZhi.GAME_RULE_YING_DUAN_MEN)) {
				chiHuRight.opr_or(Constants_WuZhi.CHR_GANG_KAI);
			}
		}

		return cbChiHuKind;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_WuZhi.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	public boolean estimate_player_out_card_respond_lsdy(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				action = _logic.check_peng_lsdy(GRR._cards_index[i], lsdyCardsIndex[i], card);
				if (action != 0 && !is_bao_ting[i]) {
					int cbRemoveCard[] = new int[] { card, card };
					int[] tmpCardsIndex = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
					int[] tmpLsdyCardsIndex = Arrays.copyOf(lsdyCardsIndex[i], lsdyCardsIndex[i].length);
					if (_logic.remove_cards_by_index_lsdy(tmpCardsIndex, tmpLsdyCardsIndex, cbRemoveCard, 2)) {
						int tmpCount = _logic.get_card_count_by_index(tmpCardsIndex);

						if (tmpCount == 0) {
							// 如果碰牌之后 没牌能出 不能让玩家碰
							int count = 0;
							int card_type_count = GameConstants.MAX_ZI_FENG;

							for (int j = 0; j < card_type_count; j++) {
								count = tmpLsdyCardsIndex[j];
								if (count > 0) {
									tmpLsdyCardsIndex[j]--;
									int[] tmpHuCards = new int[card_type_count];
									int tingCount = get_ting_card(tmpHuCards, tmpCardsIndex, GRR._weave_items[i], GRR._weave_count[i], i, true,
											tmpLsdyCardsIndex);
									if (tingCount > 0) {
										playerStatus.add_action(action);
										playerStatus.add_peng(card, seat_index);
										bAroseAction = true;

										break;
									}
								}
							}
						} else {
							playerStatus.add_action(action);
							playerStatus.add_peng(card, seat_index);
							bAroseAction = true;
						}
					}
				}

				action = _logic.estimate_gang_card_out_card_lsdy(GRR._cards_index[i], lsdyCardsIndex[i], card);
				if (action != 0) {
					if (is_bao_ting[i]) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = _logic.switch_to_card_index(card);
						int tmp_card_count = GRR._cards_index[i][tmp_card_index];
						int tmp_lsdy_card_count = lsdyCardsIndex[i][tmp_card_index];
						int tmp_weave_count = GRR._weave_count[i];

						// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
						GRR._cards_index[i][tmp_card_index] = 0;
						lsdyCardsIndex[i][tmp_card_index] = 0;
						GRR._weave_items[i][tmp_weave_count].public_card = 1;
						GRR._weave_items[i][tmp_weave_count].center_card = card;
						GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
						++GRR._weave_count[i];

						boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i,
								lsdyCardsIndex[i]);

						boolean can_gang = false;
						if (is_ting_state_after_gang) {
							can_gang = true;

							ChiHuRight chr = new ChiHuRight();
							for (int tmpCard : alreadyWinCardsSet[i]) {
								chr.set_empty();
								int action_hu = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], tmpCard, chr,
										Constants_WuZhi.HU_CARD_TYPE_ZI_MO, i);
								if (action_hu == GameConstants.WIK_NULL) {
									can_gang = false;
									break;
								}
							}
						}

						// 还原手牌数据和落地牌数据
						GRR._cards_index[i][tmp_card_index] = tmp_card_count;
						lsdyCardsIndex[i][tmp_card_index] = tmp_lsdy_card_count;
						GRR._weave_count[i] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang && can_gang) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					} else {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_WuZhi.HU_CARD_TYPE_JIE_PAO, i);

				if (action != 0) {
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
		}

		return bAroseAction;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && !is_bao_ting[i]) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (is_bao_ting[i]) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = _logic.switch_to_card_index(card);
						int tmp_card_count = GRR._cards_index[i][tmp_card_index];
						int tmp_weave_count = GRR._weave_count[i];

						// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
						GRR._cards_index[i][tmp_card_index] = 0;
						GRR._weave_items[i][tmp_weave_count].public_card = 1;
						GRR._weave_items[i][tmp_weave_count].center_card = card;
						GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
						++GRR._weave_count[i];

						boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i, null);

						// 还原手牌数据和落地牌数据
						GRR._cards_index[i][tmp_card_index] = tmp_card_count;
						GRR._weave_count[i] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					} else {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_WuZhi.HU_CARD_TYPE_JIE_PAO, i);

				if (action != 0) {
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
		}

		return bAroseAction;
	}

	public void process_an_gang_ming_gang() {
		if (GRR == null)
			return;

		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			int wCount = GRR._weave_count[i];
			for (int j = 0; j < wCount; j++) {
				int wKind = GRR._weave_items[i][j].weave_kind;
				int card = GRR._weave_items[i][j].center_card;
				int pSeat = GRR._weave_items[i][j].provide_player;
				if (wKind == GameConstants.WIK_GANG) {
					int score = 1;
					if (ziMoCount[i] <= 0 && gangKaiCount[i] <= 0)
						score = 0;

					int p = GRR._weave_items[i][j].public_card;
					if (p == 0) {
						WinDetailModel detail = new WinDetailModel();
						detail.type = 7;
						detail.card = card;
						detail.provider = 0;
						detail.score += (pCount - 1) * score;
						player_gang_score[i] += (pCount - 1) * score;
						winDetails[i].winDetailList.add(detail);

						for (int k = 0; k < pCount; k++) {
							if (i == k)
								continue;

							detail = new WinDetailModel();
							detail.type = 8;
							detail.card = card;
							if (i == get_banker_pre_seat(k)) {
								detail.provider = 1;
							} else if (i == get_banker_next_seat(k)) {
								detail.provider = 3;
							} else {
								detail.provider = 2;
							}
							detail.score -= score;
							player_gang_score[k] -= score;
							winDetails[k].winDetailList.add(detail);
						}
					} else {
						WinDetailModel detail = new WinDetailModel();
						detail.type = 5;
						detail.card = card;
						if (pSeat == get_banker_pre_seat(i)) {
							detail.provider = 1;
						} else if (pSeat == get_banker_next_seat(i)) {
							detail.provider = 3;
						} else {
							detail.provider = 2;
						}
						detail.score += score;
						player_gang_score[i] += score;
						winDetails[i].winDetailList.add(detail);

						detail = new WinDetailModel();
						detail.type = 6;
						detail.card = card;
						if (i == get_banker_pre_seat(pSeat)) {
							detail.provider = 1;
						} else if (i == get_banker_next_seat(pSeat)) {
							detail.provider = 3;
						} else {
							detail.provider = 2;
						}
						detail.score -= score;
						player_gang_score[pSeat] -= score;
						winDetails[pSeat].winDetailList.add(detail);
					}
				}
			}
		}
	}

	public void process_zi_mo_triple() {
		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			int ziMoCount = ziMoTriple[i].size();
			if (ziMoCount > 0) {
				for (Triple<Integer, Integer, Integer> triple : ziMoTriple[i]) {
					int card = triple.getFirst();
					int score = triple.getThird();

					WinDetailModel detail = new WinDetailModel();
					detail.type = 1;
					detail.card = card;
					detail.provider = 0;
					detail.score += score * (pCount - 1);
					winDetails[i].winDetailList.add(detail);

					for (int j = 0; j < pCount; j++) {
						if (i == j)
							continue;

						detail = new WinDetailModel();
						detail.type = 2;
						detail.card = card;
						if (i == get_banker_pre_seat(j)) {
							detail.provider = 1;
						} else if (i == get_banker_next_seat(j)) {
							detail.provider = 3;
						} else {
							detail.provider = 2;
						}
						detail.score -= score;
						winDetails[j].winDetailList.add(detail);

						GRR._game_score[j] -= score;
						GRR._game_score[i] += score;
					}
				}
			}
		}
	}

	public void process_jie_pao_fang_pao() {
		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			int jiePaoCount = jiePaoFangPao[i].size();
			if (jiePaoCount > 0) {
				for (Triple<Integer, Integer, Integer> triple : jiePaoFangPao[i]) {
					int card = triple.getFirst();
					int provider = triple.getSecond();
					int score = triple.getThird();
					if (ziMoCount[i] <= 0 && gangKaiCount[i] <= 0) {
						score = 0;
					}

					if (GRR != null) {
						GRR._game_score[i] += score;
						GRR._game_score[provider] -= score;
					}

					WinDetailModel detail = new WinDetailModel();
					detail.type = 3;
					detail.card = card;
					if (provider == get_banker_pre_seat(i)) {
						detail.provider = 1;
					} else if (provider == get_banker_next_seat(i)) {
						detail.provider = 3;
					} else {
						detail.provider = 2;
					}
					detail.score += score;
					winDetails[i].winDetailList.add(detail);

					detail = new WinDetailModel();
					detail.type = 4;
					detail.card = card;
					if (i == get_banker_pre_seat(provider)) {
						detail.provider = 1;
					} else if (i == get_banker_next_seat(provider)) {
						detail.provider = 3;
					} else {
						detail.provider = 2;
					}
					detail.score -= score;
					winDetails[provider].winDetailList.add(detail);
				}
			}
		}
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		int lChiHuScore = GameConstants.CELL_SCORE;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = lChiHuScore;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = lChiHuScore;
		}

		if (zimo) {
			int s = lChiHuScore;

			if (has_rule(Constants_WuZhi.GAME_RULE_YING_DUAN_MEN)) {
				if (seat_index == _cur_banker)
					s *= 2;

				if (!chr.opr_and(Constants_WuZhi.CHR_GANG_KAI).is_empty())
					s *= 2;

				if (!chr.opr_and(Constants_WuZhi.CHR_LIAN_LIU).is_empty())
					s *= 2;
			}

			// 自摸的分处理 正常结束的时候再来处理 防止中途解散
			Triple<Integer, Integer, Integer> triple = Triple.of(operate_card, seat_index, s);
			ziMoTriple[seat_index].add(triple);
		} else {
			int s = lChiHuScore;

			if (!chr.opr_and(Constants_WuZhi.CHR_LIAN_LIU).is_empty())
				s *= 2;

			// 接炮人有自摸才计算点炮胡的分
			Triple<Integer, Integer, Integer> triple = Triple.of(operate_card, provide_index, s);
			jiePaoFangPao[seat_index].add(triple);
		}
	}

	public boolean operate_player_cards_with_ting_lsdy(int seat_index, int card_count, int cards[], int tmpCount, int lsdy_cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_HNWZ_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(-1);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		//
		IntegerArray.Builder cardsBuilder = IntegerArray.newBuilder();
		for (int x = 0; x < tmpCount; x++) {
			cardsBuilder.addCard(lsdy_cards[x]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(cardsBuilder));
		//

		send_response_to_other(seat_index, roomResponse);

		roomResponse.clearWeaveItems();

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

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(-1);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		roomResponse.clearWeaveItems();

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

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	public boolean operate_player_cards_lsdy(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_HNWZ_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(-1);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		//
		int lsdy_cards[] = new int[GameConstants.MAX_COUNT];
		int tmpCount = _logic.switch_to_cards_data(lsdyCardsIndex[seat_index], lsdy_cards);

		IntegerArray.Builder cardsBuilder = IntegerArray.newBuilder();
		for (int x = 0; x < tmpCount; x++) {
			cardsBuilder.addCard(lsdy_cards[x]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(cardsBuilder));
		//

		send_response_to_other(seat_index, roomResponse);

		roomResponse.clearWeaveItems();

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

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	protected void set_result_describe() {
		int[] jie_gang = new int[getTablePlayerNumber()];
		int[] fang_gang = new int[getTablePlayerNumber()];
		int[] an_gang = new int[getTablePlayerNumber()];
		// 荒庄荒杠
		if (GRR._end_type != GameConstants.Game_End_DRAW) {
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				if (GRR != null) {
					for (int w = 0; w < GRR._weave_count[player]; w++) {
						if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}

						if (GRR._weave_items[player][w].public_card == 0) {
							an_gang[player]++;
						} else {
							jie_gang[player]++;
							fang_gang[GRR._weave_items[player][w].provide_player]++;
						}
					}
				}
			}
		}

		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			if (ziMoCount[player] > 0) {
				result.append("自摸x" + ziMoCount[player] + " ");
			}
			if (gangKaiCount[player] > 0) {
				result.append("杠开x" + gangKaiCount[player] + " ");
			}
			if (jiePaoCount[player] > 0) {
				result.append("接炮x" + jiePaoCount[player] + " ");
			}
			if (fangPaoCount[player] > 0) {
				result.append("点炮x" + fangPaoCount[player] + " ");
			}

			// 荒庄荒杠
			if (GRR._end_type != GameConstants.Game_End_DRAW) {
				if (an_gang[player] > 0) {
					result.append("暗杠x" + an_gang[player] + " ");
				}
				if (fang_gang[player] > 0) {
					result.append("放杠x" + fang_gang[player] + " ");
				}
				if (jie_gang[player] > 0) {
					result.append("接杠x" + jie_gang[player] + " ");
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[], List<Integer> ziMoCards) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		for (int card : ziMoCards) {
			roomResponse.addCardData(card);
		}

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public void test_cards() {
		if (debug_lsdy && hasLsdy && lsdy_debug_cards != null) {
			if (lsdy_debug_cards.length == 13) {
				testSameLsdyCard(lsdy_debug_cards);
			} else {
				testRealyLsdyCard(lsdy_debug_cards);
			}
			lsdy_debug_cards = null;
			debug_lsdy = false;
		} else {
			int[] cards_of_player0 = new int[] { 0x12, 0x12, 0x12, 0x12, 0x15, 0x15, 0x15, 0x15, 0x22, 0x27, 0x27, 0x27, 0x27 };
			int[] cards_of_player1 = new int[] { 0x14, 0x18, 0x35, 0x31, 0x14, 0x31, 0x01, 0x28, 0x28, 0x27, 0x27, 0x02, 0x03 };
			int[] cards_of_player2 = new int[] { 0x14, 0x18, 0x35, 0x31, 0x14, 0x31, 0x01, 0x28, 0x28, 0x27, 0x27, 0x02, 0x03 };
			int[] cards_of_player3 = new int[] { 0x14, 0x18, 0x35, 0x31, 0x14, 0x31, 0x01, 0x28, 0x28, 0x27, 0x27, 0x02, 0x03 };

			if (hasLsdy) {
				testSameLsdyCard(cards_of_player0);
				return;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
					GRR._cards_index[i][j] = 0;
				}
			}

			for (int j = 0; j < 13; j++) {
				if (getTablePlayerNumber() == 4) {
					GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
					GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
					GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
					GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
				} else if (getTablePlayerNumber() == 3) {
					GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
					GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
					GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				}
			}

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
	}

	public void testRealyLsdyCard(int[] realyCards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
				lsdyCardsIndex[i][j] = 0;
			}
		}

		int send_count = (GameConstants.MAX_COUNT - 1);
		int have_send_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_logic.switch_to_cards_index_lsdy(realyCards, have_send_count, send_count, GRR._cards_index[i], lsdyCardsIndex[i]);
			have_send_count += send_count;
		}
	}

	public void testSameLsdyCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
				lsdyCardsIndex[i][j] = 0;
			}
		}
		int send_count = (GameConstants.MAX_COUNT - 1);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_logic.switch_to_cards_index_lsdy(cards, 0, send_count, GRR._cards_index[i], lsdyCardsIndex[i]);
		}
	}
}
