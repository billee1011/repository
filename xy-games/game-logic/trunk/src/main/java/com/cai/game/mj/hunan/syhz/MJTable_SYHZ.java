package com.cai.game.mj.hunan.syhz;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_MJ_JiYuan;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Basic.MJ_GAME_END_INFO_EXT;

public class MJTable_SYHZ extends AbstractMJTable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1551695042996439136L;
	/**
	 * 
	 */
	protected MJHandlerQiShouSYHZ _handler_qishou = new MJHandlerQiShouSYHZ();
	protected MJHandlerPiao_SYHZ _handler_piao = new MJHandlerPiao_SYHZ();
	
	public int hu_score[] = new int[getTablePlayerNumber()];// 红中麻将胡分统计
	public int niao_score[] = new int[getTablePlayerNumber()];// 红中麻将鸟分统计
	public int piao_score[] = new int[getTablePlayerNumber()];// 红中麻将飘分显示
	public int hu_type[] = new int[getTablePlayerNumber()]; // 红中麻将胡牌类型
	
	public MJTable_SYHZ() {
		super(MJType.GAME_TYPE_MJ_HONG_ZHONG_FEI);
	}
	
	@Override
	protected void init_shuffle() {
		int[] cards =  MJConstants.CARD_DATA_HNCZ;
		if (getRuleValue(GameConstants.GAME_RULE_HUNAN_DAIFENG) == 1) {
			cards = MJConstants.DEFAULT;   
		}
		_repertory_card = new int[cards.length];
		shuffle(_repertory_card, cards);
	};
	
	
	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants.GAME_RULE_HUNAN_THREE)) {
			return GameConstants.GAME_PLAYER - 1;
		} else {
			return GameConstants.GAME_PLAYER;
		}
	}
	
	
	
	




	@Override
	protected void onInitTable() {
		// 初始化基础牌局handler
		_handler_dispath_card = new MJHandlerDispatchCard_SYHZ();
		_handler_out_card_operate = new MJHandlerOutCardOperate_SYHZ();
		_handler_gang = new MJHandlerGang_SYHZ();
		_handler_chi_peng = new MJHandlerChiPeng_SYHZ();
		_handler_qishou = new MJHandlerQiShouSYHZ();
		_handler_piao = new MJHandlerPiao_SYHZ();
		_logic.add_magic_card_index(_logic.switch_to_card_index(0x35));
	}

	@Override
	protected boolean on_game_start() {

		// 3D红中麻将 不至0
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = -1;
		}

		// 红中麻将添加飘分
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			set_handler(_handler_piao);
			_handler_piao.exe(this);
			return true;
		}
		return on_game_start_hz_real();
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		return analyse_chi_hu_card_hz(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
	
		int wFanShu = 0;// 番数
		wFanShu = _logic.get_chi_hu_action_rank_hz(chr, getTablePlayerNumber()); // walkergeek
	
		countCardType(chr, seat_index);
	
		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;
	
		// 杠上炮,呼叫转移 如果开杠者掷骰子补张，补张的牌开杠者若不能胡而其他玩家可以胡属于杠上炮，若胡，则属于杠上开花
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO).is_empty())) {
			
		}
		// 抢杠杠分不算 玩家在明杠的时候，其他玩家可以胡被杠的此张牌，叫抢杠胡
		else if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU).is_empty())) {
			// GRR._gang_score[_provide_player].gang_count--;//这个杠就不算了
			// _player_result.ming_gang_count[_provide_player]--;
	
		}
	
		int real_provide_index = GameConstants.INVALID_SEAT;
		// 统计
		if (zimo) {
			// 自摸
			// _player_result.zi_mo_count[seat_index]++;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._hu_result[i] = MJGameConstants.HU_RESULT_NULL;
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
	
				if (real_provide_index == GameConstants.INVALID_SEAT) {
					GRR._lost_fan_shu[i][seat_index] = wFanShu;
				} else {
					// 全包
					GRR._lost_fan_shu[real_provide_index][seat_index] = wFanShu;
				}
	
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
	
			// GRR._hu_result[provide_index] =
			// MJGameConstants.HU_RESULT_FANGPAO;
			// GRR._hu_result[seat_index] = MJGameConstants.HU_RESULT_JIEPAO;
		}
	
		/////////////////////////////////////////////// 算分//////////////////////////
		int tmp_niao_count = GRR._count_pick_niao;
		GRR._count_pick_niao = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////////////////////////////////////// 转转麻将抓鸟//////////////////只要159就算
				GRR._count_pick_niao = tmp_niao_count;
				GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
				  
			}
		}
	
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			hu_type[seat_index] = GameConstants.ZI_MO;
	
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
	
				int s = lChiHuScore;
	
				// WalkerGeek 胡牌分记录
				hu_score[i] -= s;
				hu_score[seat_index] += s;
	
				int niao_fen = 2;
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_NIAO_FEN_1))
					niao_fen = 1;

				niao_score[i] -= GRR._count_pick_niao * niao_fen;
				niao_score[seat_index] += GRR._count_pick_niao * niao_fen;
				s += GRR._count_pick_niao * niao_fen;
	
				// WalkerGeek 湖南红中添加飘分选项
				if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
					int piao = (_player_result.pao[i] + _player_result.pao[seat_index]);
					s += piao;
					piao_score[i] -= piao;
					piao_score[seat_index] += piao;
				}
	
				// WalkerGeek 红中比赛场分等比增加
				if(is_match()){
					s *= getSettleBase(seat_index);
				}
				// 胡牌分
				if (real_provide_index == GameConstants.INVALID_SEAT) {
					GRR._game_score[i] -= s;
				} else {
					int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
					if (niao > 0) {
						s -= niao;// 鸟要最后处理,把上面加的鸟分减掉 ----先这样处理--年后拆分出来
					}
					if (i == getTablePlayerNumber() - 1) {// 循环到最后一次 才把鸟分加上
						niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[real_provide_index];
						if (niao > 0) {
							s += niao;
						}
					}
	
					// 全包
					GRR._game_score[real_provide_index] -= s;
				}
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			// WalkerGeek 胡牌分记录
			hu_score[provide_index] -= s;
			hu_score[seat_index] += s;
	
			if (this.is_zhuang_xian()) {
				if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					int zx = GRR._chi_hu_rights[seat_index].da_hu_count;// lChiHuScore/6;
					s += (zx == 0 ? 1 : zx);
				}
			}
	
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU).is_empty())) {
				hu_type[seat_index] = GameConstants.QIANG_GANG_HU;
				hu_type[provide_index] = GameConstants.QIANG_GANG_HU_ALL;

				// 这个玩家全包
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					int niao_fen = 2;
					if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_NIAO_FEN_1))
						niao_fen = 1;

					int niao = GRR._count_pick_niao * niao_fen;
					niao_score[provide_index] -= niao;
					niao_score[seat_index] += niao;
					s += niao;
					
				}
			} else {
				int niao_fen = 2;
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_NIAO_FEN_1))
					niao_fen = 1;

				int niao = GRR._count_pick_niao * niao_fen;
				niao_score[provide_index] -= niao;
				niao_score[seat_index] += niao;
				s += niao;
				
			}
			
	
			// WalkerGeek 湖南红中添加飘分选项
			if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
				int piao = (_player_result.pao[provide_index] + _player_result.pao[seat_index]);
				s += piao;
				piao_score[provide_index] -= piao;
				piao_score[seat_index] += piao;
			}
	
			// WalkerGeek 红中比赛场分等比增加
			if(is_match()){
				s *= getSettleBase(seat_index);
			}
		
			if (real_provide_index == GameConstants.INVALID_SEAT) {
				GRR._game_score[provide_index] -= s;
			} else {
				s *= 3;
				GRR._game_score[provide_index] -= s;
			}
			
	
			GRR._game_score[seat_index] += s;
	
			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
			
		}
	
		if (real_provide_index == GameConstants.INVALID_SEAT) {
			GRR._provider[seat_index] = provide_index;
		} else {
			GRR._provider[seat_index] = real_provide_index;
			GRR._hu_result[real_provide_index] = GameConstants.HU_RESULT_FANG_KAN_QUAN_BAO;
		}
		// 设置变量
	
		_status_gang = false;
		_status_gang_hou_pao = false;
	
		// _playerStatus[seat_index].clean_status();
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
		
	}
	
	protected boolean on_handler_game_finish(int seat_index, int reason) {

		// 比赛场:多次重连不发送结算数据
		if (is_match()) {
			return true;
		}

		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		MJ_GAME_END_INFO_EXT.Builder gameEndExtBuilder = MJ_GAME_END_INFO_EXT.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			int ming_gang_score[] = new int[getTablePlayerNumber()];
			int an_gang_score[] = new int[getTablePlayerNumber()];
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {

						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
						int type = GRR._weave_items[i][j].type;
						if (type == GameConstants.GANG_TYPE_JIE_GANG) {
							int score = GRR._weave_items[i][j].weave_score;

							ming_gang_score[GRR._weave_items[i][j].provide_player] -= score;
							ming_gang_score[i] += score;
						} else {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								int score = GRR._weave_items[i][j].weave_score;
								if (k == i) {
									continue;
								}
								if (type == GameConstants.GANG_TYPE_ADD_GANG) {
									ming_gang_score[k] -= score;
									ming_gang_score[i] += score;
								} else {
									an_gang_score[k] -= score;
									an_gang_score[i] += score;
								}
							}
						}
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				gameEndExtBuilder.addPiao(piao_score[i]);
				gameEndExtBuilder.addHuScore(hu_score[i]);
				gameEndExtBuilder.addHuType(hu_type[i]);
				gameEndExtBuilder.addNiaoScore(niao_score[i]);
				gameEndExtBuilder.addMingGangScore(ming_gang_score[i]);
				gameEndExtBuilder.addAnGangScore(an_gang_score[i]);
			}

			// WalkerGeek 红中比赛场杠分等比变更
			if(is_match()){
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					lGangScore[k] *= getSettleBase(k);// 杠牌分*比赛场倍数
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);
		game_end.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		roomResponse.setGameEnd(game_end);

		roomResponse.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		// 记录小结算发送状态
		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}
	
	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_HZ_QISHOU_HU).is_empty())) {
							des += " 起手自摸";
						} else {
							des += " 自摸";
						}

						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
		
	}
	
	public int get_hz_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (dai_feng) {
			l += GameConstants.CARD_FENG_COUNT;
			ql += (GameConstants.CARD_FENG_COUNT - 1);
		}
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			int max_hz = 4;
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_8_HZ))
				max_hz = 8;
			if (cards_index[this._logic.get_magic_card_index(0)] == (max_hz - 1)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else if (count > 0 && count < ql) {
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			count++;
		} else {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int analyse_chi_hu_card_hz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type) {
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int result = GameConstants.WIK_NULL;

		if (has_rule(GameConstants.GAME_RULE_HUNAN_QIDUI)) {
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				result = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
			}
		}

		int max_hz = 4;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_8_HZ))
			max_hz = 8;
		if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == max_hz)
				|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == (max_hz - 1))
						&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
			result = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}

		if (chiHuRight.is_empty() == false)
			return result;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		result = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return result;
	}
	
	public boolean on_game_start_hz_real() {
		hu_score = new int[getTablePlayerNumber()];// 红中麻将胡分统计
		niao_score = new int[getTablePlayerNumber()];// 红中麻将鸟分统计
		piao_score = new int[getTablePlayerNumber()];// 红中麻将飘分显示
		hu_type = new int[getTablePlayerNumber()]; // 红中麻将胡牌类型

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			
			//WalkerGeek 麻将金币场配牌调试测试红中金币场上线要删除
			/*if( isCoinRoom() && get_players()[i].getAccount_id() == 44002 ){
				int[] cards_of_player0 = new int[] { 0x35, 0x35, 0x35, 0x35, 0x12, 0x12, 0x33, 0x33, 0x33, 0x23, 0x23, 0x23, 0x31 };
				for (int j = 0; j < 13; j++) {
					GRR._cards_index[i][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				}
			}*/
			
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_hz_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		boolean is_qishou_hu = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 起手4个红中
			int max_hz = 4;
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_8_HZ))
				max_hz = 8;
			if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == max_hz) {

				_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
				_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);
				this.exe_qishou_hongzhong(i);

				is_qishou_hu = true;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l, this.getRoom_id());
				break;
			}
		}
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}
		// this.exe_dispatch_card(_current_player,true);

		return false;
	}

	public int get_hz_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng) {
		return get_hz_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, dai_feng);
	}
	
	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou);
		this._handler_qishou.reset_status(seat_index);
		this._handler_qishou.exe(this);

		return true;
	}
	
	/**
	 * 获取鸟的 数量
	 * 
	 * @param check
	 * @param add_niao
	 * @return
	 */
	public int get_niao_card_num(boolean check, int add_niao) {
		int nNum = GameConstants.ZHANIAO_0;

		// 湖南麻将的抓鸟
		if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
			nNum = GameConstants.ZHANIAO_1;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			nNum = GameConstants.ZHANIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			nNum = GameConstants.ZHANIAO_4;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			nNum = GameConstants.ZHANIAO_6;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO8)) {
			nNum = GameConstants.ZHANIAO_8;
		}else if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ) || is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
				&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			nNum = 0;
			return nNum;
		} else if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ) || is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
				&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
			nNum = 0;
			return nNum;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			nNum = 1;
			return nNum;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
			nNum = 1;
			return nNum;
		}


		nNum += add_niao;

		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
	}
	
	public void process_chi_hu_player_operate_all() {
		// 把其他玩家的牌也倒下来展示3秒
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			operate_player_cards(i, 0, null, 0, null);

			int[] cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}
	
	public void testChangeCard(){
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				int[] temps = new int[debug_my_cards.length];
				System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
				testSameCard(temps);
				debug_my_cards = null;
			}
		}
	}
	
	public void set_niao_card_hz(int seat_index, int card, boolean show, int add_niao, int hu_card) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;

		GRR._count_niao = get_niao_card_num(true, add_niao);

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
			if (hu_card == GameConstants.HZ_MAGIC_CARD) {
				GRR._count_niao = GRR._left_card_count > 10 ? 10 : GRR._left_card_count;
			} else {
				GRR._count_niao = _logic.get_card_value(hu_card);
			}
		}
		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._count_niao = 1;
		}
		if (GRR._count_niao > GRR._left_card_count) {
			GRR._count_niao = GRR._left_card_count;
		}
		if (GRR._count_niao == GameConstants.ZHANIAO_0)
			return;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

		_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);

		GRR._left_card_count -= GRR._count_niao;

		_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._count_pick_niao = _logic.get_pick_niao_count_new_hz(GRR._cards_data_niao, GRR._count_niao);
		} else {
			GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		}

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]++] = GRR._cards_data_niao[0];
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = get_zhong_seat_by_value_three(nValue, seat_index);
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

		int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
		int[][] player_niao_cards = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_NIAO_CARD];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if (seat_index == i) {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);//
					// 胡牌的鸟生效
				} else {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);//
					// 胡牌的鸟生效
				}
				player_niao_count[i]++;
			}
		}
		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;
	}
	
	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		// MAX_NIAO_CARD
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}
	
	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_hz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 红中不能胡
			if (card != _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
				// 可以胡的情况 判断
				if (_playerStatus[i].is_chi_hu_round()) {
					int magic_count = _logic.magic_count(GRR._cards_index[i]);
					if (magic_count == 0 || !GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_BU_JIE_PAO)) {
						// 吃胡判断
						ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
						chr.set_empty();
						int cbWeaveCount = GRR._weave_count[i];
						action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								GameConstants.HU_CARD_TYPE_PAOHU, i);

						// 结果判断
						if (action != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
							bAroseAction = true;
						}
					}
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}
	
	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05 };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x13, 0x13, 0x13, 0x14, 0x14, 0x14, 0x15 };
		int[] cards_of_player2 = new int[] { 0x21, 0x21, 0x21, 0x22, 0x22, 0x22, 0x23, 0x23, 0x23, 0x24, 0x24, 0x24, 0x25 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x11, 0x12, 0x13, 0x11 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (this.getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (this.getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}
		
		//this.BACK_DEBUG_CARDS_MODE=true;
		//debug_my_cards=new int[] {52, 53, 35, 55, 9, 1, 6, 51, 35, 2, 52, 24, 24, 35, 8, 1, 20, 9, 24, 49, 7, 49, 25, 4, 38, 53, 39, 40, 38, 50, 34, 41, 39, 18, 23, 22, 23, 39, 54, 22, 36, 1, 40, 37, 21, 7, 34, 33, 55, 52, 55, 9, 7, 55, 25, 41, 8, 2, 9, 50, 41, 20, 21, 8, 22, 3, 34, 18, 2, 4, 22, 33, 19, 51, 5, 17, 6, 6, 53, 49, 4, 21, 34, 18, 19, 50, 23, 19, 36, 37, 38, 3, 17, 18, 4, 23, 37, 51, 1, 54, 7, 39, 2, 35, 36, 20, 41, 53, 38, 52, 3, 54, 25, 17, 17, 25, 5, 33, 3, 37, 33, 54, 36, 5, 40, 49, 19, 50, 40, 5, 20, 21, 24, 8, 6, 51};

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
