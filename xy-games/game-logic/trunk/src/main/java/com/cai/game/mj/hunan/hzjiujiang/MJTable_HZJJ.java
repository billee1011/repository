package com.cai.game.mj.hunan.hzjiujiang;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NANCHANG;
import com.cai.common.constant.game.mj.GameConstants_HZJJ;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Basic.MJ_GAME_END_INFO_EXT;

public class MJTable_HZJJ extends AbstractMJTable {

	public int addNiao;

	/**
	 * 
	 */
	private static final long serialVersionUID = -2251147248324432276L;
	public MJHandlerQiShouHongZhong_HZJJ _handler_qishou_hongzhong;
	public MJHandlerPiao_HZ _handler_piao; // 红中飘分

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER - 1;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER - 2;
		}

		return GameConstants.GAME_PLAYER;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card),
				GameConstants.DELAY_JIAN_PAO_HU_NEW, TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	protected void init_shuffle() {
		if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_HONGZHONG8)) {
			_repertory_card = new int[GameConstants_HZJJ.CARD_DATA_MAX1.length];
			shuffle(_repertory_card, GameConstants_HZJJ.CARD_DATA_MAX1);
		} else {
			_repertory_card = new int[GameConstants_HZJJ.CARD_DATA_MAX.length];
			shuffle(_repertory_card, GameConstants_HZJJ.CARD_DATA_MAX);
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#onInitTable()
	 */
	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_HZJJ();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HZJJ();
		_handler_gang = new MJHandlerGang_HZJJ();
		_handler_chi_peng = new MJHandlerChiPeng_HZJJ();

		_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong_HZJJ();
		_handler_piao = new MJHandlerPiao_HZ();

		_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		if (_handler_piao != null) {
			return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		}
		return false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#on_game_start()
	 */
	@Override
	protected boolean on_game_start() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.biaoyan[i] = 0;
		}
		operate_player_info();

		// 红中麻将添加飘分
		if (has_rule(GameConstants_HZJJ.GAME_RULE_DING_PIAO1)) {
			// for(int i =0; i < getTablePlayerNumber();i++){
			// _playerStatus[i]._is_pao_qiang = true;
			// _player_result.pao[i] = 1;
			// operate_player_data();
			// }
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_DING_PIAO2)) {
			// for(int i =0; i < getTablePlayerNumber();i++){
			// _playerStatus[i]._is_pao_qiang = true;
			// _player_result.pao[i] = 2;
			// operate_player_data();
			// }
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_DING_PIAO3)) {
			// for(int i =0; i < getTablePlayerNumber();i++){
			// _playerStatus[i]._is_pao_qiang = true;
			// _player_result.pao[i] = 3;
			// operate_player_data();
			// }
		} else if (!has_rule(GameConstants_HZJJ.GAME_RULE_BU_PIAO)) {
			set_handler(_handler_piao);
			_handler_piao.exe(this);
			return true;
		}
		return on_game_start_hz_real();
	}

	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = getOpenRoomIndex();// 创建者的玩家为专家

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (_cur_round == 1 && _cur_banker == 0) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#analyse_chi_hu_card(int[],
	 * com.cai.common.domain.WeaveItem[], int, int,
	 * com.cai.common.domain.ChiHuRight, int, int)
	 */
	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		return analyse_chi_hu_card_hz_new(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
	}

	// hz麻将胡牌动作 优先级
	public int get_chi_hu_action_rank_hnhz(ChiHuRight chiHuRight) {
		int wFanShu = 2;

		// if
		// (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU)).is_empty())
		// {
		// // 抢杠胡
		// wFanShu = 6;// 没人两分,被抢杠的人全包
		// }
		return wFanShu;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#process_chi_hu_player_score(int,
	 * int, int, boolean)
	 */
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
		wFanShu = get_chi_hu_action_rank_hnhz(chr);
		countCardType(chr, seat_index);
		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				GRR._lost_fan_shu[i][seat_index] = wFanShu;

			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				// if(has_rule(GameConstants_HZJJ.GAME_RULE_WU_DI)){
				// s = 0;
				
				s += getJiangQuanMa(seat_index,zimo);
				
				s *= getDiFen();
				s += getJiangLiFen(seat_index);

				// 飘分计算
				s += getPiaoFne(seat_index, i);
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			// if(has_rule(GameConstants_HZJJ.GAME_RULE_WU_DI)){
			// s = 0;
			// }
//			if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1) || has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
//				if (getAddNiao(seat_index) == 0) {
//					s += GRR._count_pick_niao - addNiao;
//				} else {
//					s += GRR._count_pick_niao;
//				}
//			} else {
//				if (getAddNiao(seat_index) == 0) {
//					s += (GRR._count_pick_niao - addNiao) * 2;
//				} else {
//					s += GRR._count_pick_niao * 2;
//				}
//			}
			s += getJiangQuanMa(seat_index,zimo);

			// 如果是抢杠胡
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU).is_empty())) {
				s *= getTablePlayerNumber() - 1;
			}
			s *= getDiFen();
			// 飘分计算

			s += getPiaoFne(seat_index, provide_index);
			s += getJiangLiFen(seat_index);
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		// _playerStatus[seat_index].clean_status();
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;

	}

	/**
	 * 飘分计算
	 * 
	 * @param seat_idnex
	 * @param provide_index
	 * @return
	 */
	public int getPiaoFne(int seat_index, int provide_index) {
		int feng = 0;
		int flag = 0;
		if (has_rule(GameConstants_HZJJ.GAME_RULE_DING_PIAO1)) {
			flag = 1;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_DING_PIAO2)) {
			flag = 2;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_DING_PIAO3)) {
			flag = 3;
		}
		if (flag > 0) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_playerStatus[i]._is_pao_qiang = true;
				_player_result.pao[i] = flag;
			}
		}

		if (!has_rule(GameConstants_HZJJ.GAME_RULE_BU_PIAO)) {
			if (getRuleValue(GameConstants_HZJJ.GAME_RULE_DUI_PIAO) == 1) {
				feng = (_player_result.pao[seat_index] == -1 ? 0 : _player_result.pao[seat_index])
						+ (_player_result.pao[provide_index] == -1 ? 0 : _player_result.pao[provide_index]);
			} else if (getRuleValue(GameConstants_HZJJ.GAME_RULE_BUDUI_PIAO) == 1) {
				if (_player_result.pao[seat_index] >= _player_result.pao[provide_index]) {
					feng = _player_result.pao[seat_index] == -1 ? 0 : _player_result.pao[seat_index];
				} else {
					feng = _player_result.pao[provide_index] == -1 ? 0 : _player_result.pao[provide_index];
				}
			}
		}
		return feng;
	}

	public void changeCard(int cards[]) {
		if (cards == null) {
			return;
		}
		for (int j = 0; j < cards.length; j++) {
			if (cards[j] == 0) {
				continue;
			}
			if (cards[j] == 0x35) {
				cards[j] += GameConstants_HZJJ.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
	}

	/**
	 * 获取4红中奖励分
	 * 
	 * @return
	 */
	public int getJiangLiFen(int seat_index) {
		int feng = 0;
		if (GRR._cards_index[seat_index][_logic.switch_to_card_index(0x35)] != 4) {
			if ((GRR._cards_index[seat_index][_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
					&& (GRR._chi_hu_card[seat_index][0] == GameConstants.HZ_MAGIC_CARD)) {
			} else {
				return feng;
			}
		}

		if (has_rule(GameConstants_HZJJ.GAME_RULE_HONGZHONG_JIANG_FENG)) {
			if (has_rule(GameConstants_HZJJ.GAME_RULE_HONGZHONG_JIANG2)) {
				feng = 2;
			} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HONGZHONG_JIANG5)) {
				feng = 5;
			} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HONGZHONG_JIANG10)) {
				feng = 10;
			}
		}
		return feng;
	}

	public int getJiangQuanMa(int seat_index,boolean zimo) {
		int feng = 0;

		if(zimo){
			// }
			if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1)
					|| has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
				feng += GRR._count_pick_niao;
			} else {
				feng += GRR._count_pick_niao * 2;
			}
		}else{
			
			if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1) || has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
				if (getAddNiao(seat_index) == 0) {
					feng += GRR._count_pick_niao - addNiao;
				} else {
					feng += GRR._count_pick_niao;
				}
			} else {
				if (getAddNiao(seat_index) == 0) {
					feng += (GRR._count_pick_niao - addNiao) * 2;
				} else {
					feng += GRR._count_pick_niao * 2;
				}
			}
		}

		if (GRR._cards_index[seat_index][_logic.switch_to_card_index(0x35)] != 4) {
			if ((GRR._cards_index[seat_index][_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
					&& (GRR._chi_hu_card[seat_index][0] == GameConstants.HZ_MAGIC_CARD)) {
			} else {
				return feng;
			}
		}

		if (has_rule(GameConstants_HZJJ.GAME_RULE_HONGZHONG_JIANG_QUANMA)) {
			if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA2)) {
				feng = 2 * 2;
			} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA3)) {
				feng = 3 * 2;
			} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA4)) {
				feng = 4 * 2;
			} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA6)) {
				feng = 6 * 2;
			} else if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1)
					|| has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
				feng = 10;
			}
		}
		return feng;
	}

	public int getDiFen() {
		int feng = 1;
		if (has_rule(GameConstants_HZJJ.GAME_RULE_DI1)) {
			feng = 1;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_DI2)) {
			feng = 2;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_DI5)) {
			feng = 5;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_DI3)) {
			feng = 3;
		}
		return feng;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#set_result_describe()
	 */
	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			boolean siFlag = true;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";

						if (GRR._count_pick_niao > 0) {
							if (getAddNiao(i) == 0) {
								des += " 中码X" + (GRR._count_pick_niao - addNiao);
							} else {
								des += " 中码X" + GRR._count_pick_niao;
							}
						}
					}
					if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_HONGZHONG4)) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_HZ_QISHOU_HU).is_empty())
								&& siFlag) {
							des += " 四红中";
							siFlag = false;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							if (getAddNiao(i) == 0) {
								des += " 中码X" + (GRR._count_pick_niao - addNiao);
							} else {
								des += " 中码X" + GRR._count_pick_niao;
							}
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠开";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七对";
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

			String strD = "";
			// 飘分
			if (!has_rule(GameConstants_HZJJ.GAME_RULE_BU_PIAO)) {
				int score = _player_result.pao[i] == -1 ? 0 : _player_result.pao[i];
				strD = " 飘分" + score;
			}
			des += strD;
			GRR._result_des[i] = des;
		}
	}

	public void set_niao_card_hz(int seat_index, int card, boolean show, int add_niao, int hu_card, boolean is_tong) {
		if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1) || has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
			add_niao = 0;
		}

		// 初始化
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
		if (GRR._count_niao == GameConstants.ZHANIAO_0) {
			return;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
				cbCardIndexTemp);

		GRR._left_card_count -= GRR._count_niao;
		_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

		if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1) || has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
			int val = _logic.get_pick_niao_count_new_hz(GRR._cards_data_niao, GRR._count_niao);
			// 新一码全中1为9
			if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
				if (val == 1) {
					val = 9;
				}
			}
			GRR._count_pick_niao = val;
		} else {
			GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao, is_tong, add_niao);
		}

		if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1)) {
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
				if (seat_index == i || has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1)
						|| has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
							true);//
					// 胡牌的鸟生效
				} else {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
							false);//
					// 胡牌的鸟生效
				}
				player_niao_count[i]++;
			}
		}

		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;
		// 记录中码个数
		_player_result.piao_lai_count[seat_index] += GRR._count_pick_niao;
	}

	public int getAddNiao(int seat_index) {
		int addNiao = 1;
		if (GRR._cards_index[seat_index][_logic.get_magic_card_index(0)] != 0) {
			// 手上没有红中
			addNiao = 0;

		}
		for (int i = 0; i < GRR._weave_count[seat_index]; i++) {
			if (GRR._weave_items[seat_index][i].center_card == 0x35) {
				addNiao = 0;
			}
		}

		return addNiao;
	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num, boolean is_tong, int add_niao) {
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
			if (is_tong && add_niao > 0 && i == (card_num - 1)) {
				if (nValue == 1 || nValue == 5 || nValue == 9) {
					addNiao = 1;
				}
			}

		}
		return cbPickNum;
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

		if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA2)) {
			nNum = GameConstants.ZHANIAO_2;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA3)) {
			nNum = GameConstants.ZHANIAO_3;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA4)) {
			nNum = GameConstants.ZHANIAO_4;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_MA6)) {
			nNum = GameConstants.ZHANIAO_6;
		} else if (has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1)
				|| has_rule(GameConstants_HZJJ.GAME_RULE_ALL_MA1_NEW)) {
			nNum = GameConstants.ZHANIAO_1;
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

	/**
	 * 河南红中麻将
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_hnhz(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
				effect_indexs[i] = GameConstants.CHR_HU;
			} else {
				effect_indexs[i] = chr.type_list[i];
			}

		}
		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
				GameConstants.INVALID_SEAT);
		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

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
			int card_index_temp[] = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
			card_index_temp[_logic.switch_to_card_index(card)] -= 2;
			boolean flag = false;
			for (int k = 0; k < card_index_temp.length; k++) {
				if (_logic.switch_to_card_index(0x35) != k && card_index_temp[k] > 0) {
					flag = true;
					break;
				}
			}
			if (action != 0 && flag) {
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

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				if (has_rule(GameConstants_HZJJ.GAME_RULE_KE_DIAN_PAO)) {
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

	public int get_hz_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {
		return get_hz_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, dai_feng);
	}

	public int get_hz_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {
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
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			int max_hz = 4;
			if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_HONGZHONG4)) {
				if (cards_index[this._logic.get_magic_card_index(0)] == (max_hz - 1)) {
					cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
					count++;
				}
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

	public int analyse_chi_hu_card_hz_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if (!has_rule(GameConstants_HZJJ.GAME_RULE_KE_DIAN_PAO) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {
			return GameConstants.WIK_NULL;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (cbCardIndexTemp[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] > 0) {
			// 有红中只自摸
			if (has_rule(GameConstants_HZJJ.GAME_RULE_HONGZHONG_ONLY_ZIMO)) {
				if (card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
					return GameConstants.WIK_NULL;
				}
			}
			// 有红中可以点炮，抢杠胡
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int result = GameConstants.WIK_NULL;

		if (has_rule(GameConstants_HZJJ.GAME_RULE_KE_HU_QI_DUI)) {
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				result = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
			}
		}

		int max_hz = 4;
		if (has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_HONGZHONG4)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == max_hz)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == (max_hz - 1))
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				result = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);
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

		boolean can_win = false;
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU && cur_card == GameConstants.HZ_MAGIC_CARD) {
			can_win = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		} else {
			can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
		}

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

	public void initParm() {
		addNiao = 0;
	}

	public boolean on_game_start_hz_real() {

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			// WalkerGeek 麻将金币场配牌调试测试红中金币场上线要删除
			/*
			 * if( isCoinRoom() && get_players()[i].getAccount_id() == 44002 ){
			 * int[] cards_of_player0 = new int[] { 0x35, 0x35, 0x35, 0x35,
			 * 0x12, 0x12, 0x33, 0x33, 0x33, 0x23, 0x23, 0x23, 0x31 }; for (int
			 * j = 0; j < 13; j++) {
			 * GRR._cards_index[i][_logic.switch_to_card_index(cards_of_player0[
			 * j])] += 1; } }
			 */

			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			changeCard(hand_cards[i]);
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
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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
			_playerStatus[i]._hu_card_count = this.get_hz_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		boolean is_qishou_hu = false;
		/*
		 * if(has_rule(GameConstants_HZJJ.GAME_RULE_HZJJ_HONGZHONG4)){ for (int
		 * i = 0; i < getTablePlayerNumber(); i++) { // 起手4个红中 int max_hz = 4;
		 * if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == max_hz) {
		 * _playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
		 * _playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.
		 * get_magic_card_index(0)), i);
		 * GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
		 * GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);
		 * this.exe_qishou_hongzhong(i);
		 * 
		 * is_qishou_hu = true;
		 * MongoDBServiceImpl.getInstance().card_log(this.get_players()[i],
		 * ECardType.hongZhong4, "", 0, 0l, this.getRoom_id()); break; } } }
		 */
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(GRR._banker_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}
		// this.exe_dispatch_card(_current_player,true);

		return false;
	}

	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou_hongzhong);
		this._handler_qishou_hongzhong.reset_status(seat_index);
		this._handler_qishou_hongzhong.exe(this);

		return true;
	}

	/***
	 * 红中麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 *//*
		 * public int analyse_chi_hu_card_hz(int cards_index[], WeaveItem
		 * weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
		 * int card_type) {
		 * 
		 * if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) { if
		 * (has_rule(GameConstants_HZJJ.GAME_RULE_KE_DIAN_PAO)) { int
		 * magic_count = _logic.magic_count(cards_index); if (magic_count > 0)
		 * return GameConstants.WIK_NULL; } else { return
		 * GameConstants.WIK_NULL; } }
		 * 
		 * // cbCurrentCard一定不为0 !!!!!!!!! if (cur_card == 0) { return
		 * GameConstants.WIK_NULL; }
		 * 
		 * // 变量定义 int cbChiHuKind = GameConstants.WIK_NULL;
		 * 
		 * if (has_rule(GameConstants_HZJJ.GAME_RULE_KE_HU_QI_DUI)) { // 7.8 //
		 * 七小对牌 豪华七小对 long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems,
		 * weaveCount, cur_card); if (qxd != GameConstants.WIK_NULL) {
		 * cbChiHuKind = GameConstants.WIK_CHI_HU;
		 * chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI); if (card_type
		 * == GameConstants.HU_CARD_TYPE_ZIMO) {
		 * chiHuRight.opr_or(GameConstants.CHR_ZI_MO); } else {
		 * chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); } } }
		 * 
		 * if
		 * ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD
		 * )] == 4) ||
		 * ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD
		 * )] == 3) && (cur_card == GameConstants.HZ_MAGIC_CARD))) {
		 * 
		 * cbChiHuKind = GameConstants.WIK_CHI_HU;
		 * chiHuRight.opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU); if
		 * (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
		 * chiHuRight.opr_or(GameConstants.CHR_ZI_MO); } else { // 这个没必要。一定是自摸
		 * chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); } }
		 * 
		 * if (chiHuRight.is_empty() == false) {
		 * 
		 * return cbChiHuKind; }
		 * 
		 * // 构造扑克 int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX]; for
		 * (int i = 0; i < GameConstants.MAX_INDEX; i++) { cbCardIndexTemp[i] =
		 * cards_index[i]; }
		 * 
		 * // 插入扑克 if (cur_card != GameConstants.INVALID_VALUE) {
		 * cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++; }
		 * 
		 * List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>(); //
		 * 分析扑克 boolean bValue = _logic.analyse_card(cbCardIndexTemp,
		 * weaveItems, weaveCount, analyseItemArray, true); if (!bValue) {
		 * 
		 * chiHuRight.set_empty(); return GameConstants.WIK_NULL; }
		 * 
		 * cbChiHuKind = GameConstants.WIK_CHI_HU;
		 * 
		 * if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // cbChiHuKind =
		 * MJGameConstants.WIK_CHI_HU;
		 * 
		 * chiHuRight.opr_or(GameConstants.CHR_ZI_MO); } else {
		 * chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		 * 
		 * }
		 * 
		 * return cbChiHuKind; }
		 */

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		return handler_game_finish_hunan_hz(seat_index, reason);
	}

	/**
	 * 湖南红中麻将的游戏结束
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean handler_game_finish_hunan_hz(int seat_index, int reason) {
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
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {

						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}
				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._game_score[i] += lGangScore[i];
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);
		game_end.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		roomResponse.setGameEnd(game_end);

		roomResponse.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

	protected void test_cards() {

		// 黑摸
		int cards[] = new int[] { 0x11, 0x35, 0x13, 0x35, 0x22, 0x01, 0x07, 0x35, 0x29, 0x14, 0x35, 0x15, 0x25 };
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
		// int[] realyCards = new int[] { 34, 19, 25, 8, 9, 6, 4, 3, 24, 41, 20,
		// 35, 2, 7, 1, 18, 17, 7, 34, 8, 9, 6, 41,
		// 35, 21, 2, 9, 21, 1, 2, 38, 34, 5, 39, 40, 21, 39, 33, 18, 38, 23,
		// 38, 37, 3, 33, 19, 24, 20, 22, 4, 39,
		// 3, 7, 5, 8, 18, 35, 36, 22, 2, 1, 22, 24, 23, 40, 35, 17, 4, 25, 36,
		// 19, 8, 5, 41, 6, 33, 20, 24, 40,
		// 38, 40, 25, 17, 6, 17, 34, 4, 20, 37, 37, 7, 37, 36, 25, 3, 41, 23,
		// 33, 39, 36, 21, 18, 9, 1, 22, 23, 5,
		// 19 };
		// testRealyCard(realyCards);

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

}
