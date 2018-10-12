package com.cai.game.mj.shanxi.tuidaohu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_SXTDH extends AbstractMJTable {

	private static final long serialVersionUID = 6611671011530711969L;

	protected MJHandlerOutCardBaoTing_SXTDH _handler_bao_ting;

	// 本圈是否可以抢杠
	public boolean[] can_qiang_gang;

	// 未报听玩家点炮
	public boolean[] no_bao_ting_dian_pao;

	// 报听玩家点炮
	public boolean[] bao_ting_dian_pao;

	public void NoBaoTingDianPaoVaild(int seat_index) {
		no_bao_ting_dian_pao[seat_index] = true;
	}

	public void BaoTingDianPaoVaild(int seat_index) {
		bao_ting_dian_pao[seat_index] = true;
	}

	public MJTable_SXTDH() {
		super(MJType.GAME_TYPE_SX_TUIDAOHU);
	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_bao_ting);
		this._handler_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}
		// 如果不是自摸胡又勾选了只能自摸胡
		if (has_rule(Constants_SXTuiDaoHu.GAME_RULE_ZI_MO_HU) && card_type != Constants_SXTuiDaoHu.HU_CARD_TYPE_ZI_MO) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean is_xiao_hu = true; // 是否为小胡（平胡）
		boolean can_win = false; // 是否能胡牌

		int xiao_qi_dui = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (xiao_qi_dui != Constants_SXTuiDaoHu.WIK_NULL) {// 七小对，不需要258将，能直接胡牌
			is_xiao_hu = false;
			can_win = true;
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			// 判断是否豪华七小对
			if (xiao_qi_dui == Constants_SXTuiDaoHu.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
				chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_HAO_HUA_QI_XIAO_DUI);
			} else {
				chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QI_XIAO_DUI);
			}
		}

		// 十三幺判断之前需要加上发的那张牌
		int[] cbCardIndexTemp_SSY = Arrays.copyOf(cards_index, cards_index.length);
		cbCardIndexTemp_SSY[_logic.switch_to_card_index(cur_card)]++;
		boolean shi_san_yao = _logic.isShiSanYao(cbCardIndexTemp_SSY, weaveItems, weave_count);

		if (shi_san_yao) { // 十三幺，不需要258将
			is_xiao_hu = false;
			can_win = true;
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_SHI_SAN_YAO);
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

		// 分析扑克
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card_(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);
		if (bValue) {
			// 清一色一条龙需要检测基础牌型，当基础牌型胡牌满足才开始判断
			boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
			boolean is_yi_tiao_long = false;
			if (_logic.is_yi_tiao_long(cbCardIndexTemp, weave_count)) {
				is_yi_tiao_long = true;
			}
			if (is_yi_tiao_long && is_qing_yi_se) { // 清一色一条龙，不需要258将
				if (has_rule(Constants_SXTuiDaoHu.GAME_RULE_QING_YI_SE_AND_LONG_JIA_FAN)) {
					is_xiao_hu = false;
					can_win = true;
					cbChiHuKind = GameConstants.WIK_CHI_HU;
					chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QYS_YTL);
				} else {
					is_xiao_hu = false;
					can_win = true;
					cbChiHuKind = GameConstants.WIK_CHI_HU;
					chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QING_YI_SE);
				}
			} else if (is_qing_yi_se) { // 清一色，不需要258将
				is_xiao_hu = false;
				can_win = true;
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QING_YI_SE);
			} else if (is_yi_tiao_long) { // 一条龙，不需要258将
				is_xiao_hu = false;
				can_win = true;
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_YI_TIAO_LONG);
			} else if ((xiao_qi_dui == Constants_SXTuiDaoHu.WIK_NULL) && !shi_san_yao) {
				is_xiao_hu = true;
				can_win = true;
			}

		}

		if (can_win == false) { // 如果不能胡牌
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (is_xiao_hu) { // 平胡
			chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_PI_HU);
		}

		if (card_type == Constants_SXTuiDaoHu.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_ZI_MO); // 自摸
		} else if (card_type == Constants_SXTuiDaoHu.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_FANG_PAO); // 点炮
		} else if (card_type == Constants_SXTuiDaoHu.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QIANG_GANG); // 抢杠胡
		}

		return cbChiHuKind;
	}

	/**
	 * 检查杠牌后是否换章 返回true表示已经换张了
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	// 不能换章，需要检测是否改变了听牌
	public boolean check_gang_huan_zhang(int seat_index, int card) {
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		// 假如杠了
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		hu_card_count = this.get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);
		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;
		// table._playerStatus[_seat_index]._hu_out_cards[ting_count]
		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != get_real_card(hu_cards[j])) {
					return true;
				}
			}
		}

		return false;
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

			// 过手胡判断，如果可炮胡不胡，那么抢杠胡也不能胡，过圈才能胡
			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				// 抢杠胡时的胡牌检测
				// action = analyse_card_from_ting(GRR._cards_index[i],
				// GRR._weave_items[i], cbWeaveCount, card,
				// chr,Constants_SXTuiDaoHu.HU_CARD_TYPE_QIANG_GANG, i);
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_SXTuiDaoHu.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
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

	// 抢杠胡时的胡牌检测
	public int analyse_card_from_ting(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		if (card_type == Constants_SXTuiDaoHu.CHR_QIANG_GANG) {
			chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QIANG_GANG);
		}

		int check_qi_xiao_dui = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		// 七对
		boolean xiao_qi_dui = check_qi_xiao_dui != GameConstants.WIK_NULL;
		// 清一色
		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		// 一条龙
		boolean is_yi_tiao_long = _logic.is_yi_tiao_long(cards_index, weave_count);
		// 十三幺
		boolean shi_san_yao = _logic.isShiSanYao(cards_index, weaveItems, weave_count);

		if (xiao_qi_dui || is_qing_yi_se || is_yi_tiao_long || shi_san_yao) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;

			if (xiao_qi_dui) {
				if (check_qi_xiao_dui == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
				}
			}
			if (is_qing_yi_se && is_yi_tiao_long) {
				chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QYS_YTL);
			} else {
				if (is_qing_yi_se) {
					chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_QING_YI_SE);
				}
				if (is_yi_tiao_long) {
					chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_YI_TIAO_LONG);
				}
			}
			if (shi_san_yao) {
				chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_SHI_SAN_YAO);
			}

		} else {
			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
			boolean bValue = _logic.analyse_card_(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);
			if (bValue) {
				chiHuRight.opr_or(Constants_SXTuiDaoHu.CHR_PI_HU);
				cbChiHuKind = GameConstants.WIK_CHI_HU;
			}
		}

		return cbChiHuKind;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;

		// 用户状态
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤，不判断出牌的人
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			// 牌堆还有牌才能碰和杠，不然流局算庄会出错
			if (GRR._left_card_count > 0) {
				if (this.has_rule(Constants_SXTuiDaoHu.GAME_RULE_BAO_TING)) {

					// 如果已经报听了，打出来的牌就好根据玩法判断了
					if (this._playerStatus[i].is_bao_ting()) {
						// 如果选择了改变听口不能杠玩法，那么杠完之后 不能改变所听的那几张牌
						if (this.has_rule(Constants_SXTuiDaoHu.GAME_RULE_GAI_TING_KOU_NO_GANG)) {
							// 如果杠完之后所听的牌没有发生改变，就把杠的动作加上去
							if (!this.check_gang_huan_zhang(i, card)) {
								action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
								if (action != 0 && action == GameConstants.WIK_GANG) {
									playerStatus.add_action(GameConstants.WIK_GANG);
									playerStatus.add_gang(card, seat_index, 1);
									bAroseAction = true;
								}
							}
						}
						// 一定是要报听之后，才会有吃胡的显示
						// 胡要过圈
						if (_playerStatus[i].is_chi_hu_round()) {
							boolean can_hu_this_card = true;
							int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
							for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
								if (tmp_cards_data[x] == card) {
									can_hu_this_card = false;
									break;
								}
							}
							if (can_hu_this_card) {
								// if (GRR._weave_count[i] == 0) {
								// 吃胡判断
								ChiHuRight chr = GRR._chi_hu_rights[i];
								chr.set_empty();
								int cbWeaveCount = GRR._weave_count[i];
								action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
										Constants_SXTuiDaoHu.HU_CARD_TYPE_JIE_PAO, i);
								// 结果判断
								if (action != 0 && action == GameConstants.WIK_CHI_HU) {
									_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
									_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
									bAroseAction = true;
								}
								// }
							}
						}
					} else {// 在没有听牌的情况下，碰和杠牌做简单的处理
							// 碰要过圈
						boolean can_peng = true;
						int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
						for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
							if (tmp_cards_data[x] == card) {
								can_peng = false;
								break;
							}
						}

						// 碰牌判断
						action = _logic.check_peng(GRR._cards_index[i], card);
						if (action != 0 && can_peng) {
							playerStatus.add_action(action);
							playerStatus.add_peng(card, seat_index);
							bAroseAction = true;
						}

						// 杠牌判断
						action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
						if (action != 0) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}

						int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
						for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
							cbCardIndexTemp[j] = GRR._cards_index[i][j];
						}
						cbCardIndexTemp[_logic.switch_to_card_index(card)]++;

					}

				} else {
					// 一定是要报听之后，才会有吃胡的显示
					// 碰要过圈
					boolean can_peng = true;
					int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
					for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
						if (tmp_cards_data[x] == card) {
							can_peng = false;
							break;
						}
					}

					// 碰牌判断
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0 && can_peng) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}

					// 杠牌判断
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}

					int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						cbCardIndexTemp[j] = GRR._cards_index[i][j];
					}
					cbCardIndexTemp[_logic.switch_to_card_index(card)]++;
					// 胡要过圈
					if (_playerStatus[i].is_chi_hu_round()) {
						boolean can_hu_this_card = true;
						tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
						for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
							if (tmp_cards_data[x] == card) {
								can_hu_this_card = false;
								break;
							}
						}
						if (can_hu_this_card) {
							// if (GRR._weave_count[i] == 0) {
							// 吃胡判断
							ChiHuRight chr = GRR._chi_hu_rights[i];
							chr.set_empty();
							int cbWeaveCount = GRR._weave_count[i];

							action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
									Constants_SXTuiDaoHu.HU_CARD_TYPE_JIE_PAO, i);
							// 结果判断
							if (action != 0 && action == GameConstants.WIK_CHI_HU) {
								_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
								_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
								bAroseAction = true;
							}
							// }
						}
					}
				}
			}

		}

		if (bAroseAction) {
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT; // 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	/**
	 * 封装落地牌组合
	 * 
	 * @param weave_count
	 * @param weaveitems
	 * @param roomResponse
	 */
	public void builderWeaveItemResponse(int weave_count, WeaveItem weaveitems[], RoomResponse.Builder roomResponse) {
		for (int j = 0; j < weave_count; j++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
			weaveItem_item.setPublicCard(weaveitems[j].public_card);
			weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
			weaveItem_item.setCenterCard(weaveitems[j].center_card);

			// 客户端特殊处理的牌值
			for (int i = 0; i < weaveitems[j].client_special_count; i++) {
				weaveItem_item.addClientSpecialCard(weaveitems[j].client_special_card[i]);
			}

			roomResponse.addWeaveItems(weaveItem_item);
		}
	}

	// 听牌时的判断
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;
		int max_ting_count = 0;
		// 根据不同规则更换不同索引
		if (has_rule(Constants_SXTuiDaoHu.GAME_RULE_DAI_FENG)) {
			max_ting_count = GameConstants.MAX_ZI_FENG;
		} else {
			max_ting_count = GameConstants.MAX_ZI;
		}

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SXTuiDaoHu.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	// 是否听牌
	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SXTuiDaoHu.HU_CARD_TYPE_ZI_MO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected void init_shuffle() {
		super.init_shuffle();
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_SXTDH();
		_handler_dispath_card = new MJHandlerDispatchCard_SXTDH();
		_handler_gang = new MJHandlerGang_SXTDH();
		_handler_out_card_operate = new MJHandlerOutCardOperate_SXTDH();
		_handler_bao_ting = new MJHandlerOutCardBaoTing_SXTDH();
	}

	@Override
	protected boolean on_game_start() {
		no_bao_ting_dian_pao = new boolean[] { false, false, false, false };
		bao_ting_dian_pao = new boolean[] { false, false, false, false };
		_game_status = GameConstants.GS_MJ_PLAY; // 设置状态

		_logic.clean_magic_cards();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_COUNT];

		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

		// 回放
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

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
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
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉，结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		// 把麻将推倒，提示其他玩家胡牌
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void runnable_remove_middle_cards(int seat_index) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		/*
		 * for (int i = 0; i < GameConstants.GAME_PLAYER; i++) { boolean
		 * has_lai_zi = false; for (int j = 0; j < GameConstants.MAX_INDEX; j++)
		 * { if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
		 * has_lai_zi = true; break; } } if (has_lai_zi) { // 刷新自己手牌 int cards[]
		 * = new int[GameConstants.MAX_COUNT]; int hand_card_count =
		 * _logic.switch_to_cards_data(GRR._cards_index[i], cards); for (int j =
		 * 0; j < hand_card_count; j++) { if (_logic.is_magic_card(cards[j])) {
		 * cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI; } }
		 * this.operate_player_cards(i, hand_card_count, cards, 0, null); } }
		 */

		// 重载的方法里，不直接发牌，交给起手混去处理
		// this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	/**
	 * 根据胡牌的牌类型和胡牌的牌型种类来进行叠加算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param hu_card_type
	 */
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int hu_card_type, boolean is_zi_mo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		countCardType(chr, seat_index);

		wFanShu = this.get_fan_shu(hu_card_type, chr); // 根据牌型和吃胡类型获取番数

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		// 算基础分
		if (is_zi_mo) { // 自摸
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else { // 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		// 算分
		if (is_zi_mo) { // 自摸
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else { // 点炮
			/*
			 * int s = lChiHuScore;
			 * 
			 * GRR._game_score[provide_index] -= s; GRR._game_score[seat_index]
			 * += s;
			 * 
			 * GRR._chi_hu_rights[provide_index]
			 * .opr_or(Constants_SXTuiDaoHu.CHR_FANG_PAO);
			 */
			int real_player = provide_index;

			/// 这个时候就要判断出牌人是否已经报停了 ，如果A报听了点炮给D了，ABC三个人都要给钱，没报听点炮了 A就要替BC两家买单了
			if (this._playerStatus[real_player].is_bao_ting()) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;
					GRR._chi_hu_rights[real_player].opr_or(Constants_SXTuiDaoHu.CHR_FANG_PAO);
					float s = 0;
					s = wFanShu;

					// 胡牌分
					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				GRR._chi_hu_rights[real_player].opr_or(Constants_SXTuiDaoHu.CHR_FANG_PAO);
				float s = 0;
				s = wFanShu;

				// 胡牌分，一个人出三家的钱
				GRR._game_score[real_player] -= 3 * s;
				GRR._game_score[seat_index] += 3 * s;
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	public void test_cards() {

		int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x12, 0x12, 0x13, 0x13, 0x14, 0x14, 0x15, 0x15, 0x16, 0x16, 0x09 };
		int[] cards_of_player2 = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x09, 0x09 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x09, 0x09 };
		// int cards[] = new int[] { 0x34, 0x34, 0x34, 0x17, 0x18, 0x19, 0x07,
		// 0x08, 0x09, 0x14, 0x15, 0x16, 0x17 };
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

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			if (GRR._chi_hu_rights[player].is_valid()) {
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];

					if (type == Constants_SXTuiDaoHu.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == Constants_SXTuiDaoHu.CHR_FANG_PAO) {
						gameDesc.append(" 接炮");
					}
					if (type == Constants_SXTuiDaoHu.CHR_QIANG_GANG) {
						gameDesc.append(" 抢杠胡");
					}
					if (type == Constants_SXTuiDaoHu.CHR_PI_HU) {
						gameDesc.append(" 平胡");
					}
					if (type == Constants_SXTuiDaoHu.CHR_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == Constants_SXTuiDaoHu.CHR_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == Constants_SXTuiDaoHu.CHR_YI_TIAO_LONG) {
						gameDesc.append(" 一条龙");
					}
					if (type == Constants_SXTuiDaoHu.CHR_QYS_YTL) {
						gameDesc.append(" 清一色一条龙");
					}
					if (type == Constants_SXTuiDaoHu.CHR_HAO_HUA_QI_XIAO_DUI) {
						gameDesc.append(" 豪华七小对");
					}
					if (type == Constants_SXTuiDaoHu.CHR_SHI_SAN_YAO) {
						gameDesc.append(" 十三幺");
					}
				}

			} else if (!GRR._chi_hu_rights[player].opr_and(Constants_SXTuiDaoHu.CHR_FANG_PAO).is_empty()) {
				gameDesc.append(" 放炮");
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	// 获取庄家上家的座位
	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
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
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			if (!(chiHuRight.opr_and(Constants_SXTuiDaoHu.CHR_QIANG_GANG)).is_empty()) { // 抢杠胡
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_qiangganghu, "", _game_type_index, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_SXTuiDaoHu.CHR_QING_YI_SE)).is_empty()) { // 清一色
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_qingyise, "", _game_type_index, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_SXTuiDaoHu.CHR_YI_TIAO_LONG)).is_empty()) { // 一条龙
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_yitiaolong, "", _game_type_index, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_SXTuiDaoHu.CHR_QYS_YTL)).is_empty()) { // 清一色一条龙
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_qysytl, "", _game_type_index, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_SXTuiDaoHu.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_qixiaodui, "", _game_type_index, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_SXTuiDaoHu.CHR_HAO_HUA_QI_XIAO_DUI)).is_empty()) { // 豪华七小对
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_haohuaqixiaodui, "", _game_type_index, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_SXTuiDaoHu.CHR_SHI_SAN_YAO)).is_empty()) { // 十三幺
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_shisanyao, "", _game_type_index, 0l,
						this.getRoom_id());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int get_fan_shu(int hu_card_type, ChiHuRight chr) {
		int wFanShu = 1;

		if (hu_card_type == Constants_SXTuiDaoHu.HU_CARD_TYPE_QIANG_GANG || hu_card_type == Constants_SXTuiDaoHu.HU_CARD_TYPE_JIE_PAO) { // 抢杠胡和点炮胡
			// 如果勾选了平胡 则所有胡都按平胡算
			if (has_rule(Constants_SXTuiDaoHu.GAME_RULE_ONLY_PI_HU)) {
				wFanShu = 1;
			} else {
				wFanShu = 1;
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_QING_YI_SE)).is_empty()) { // 清一色
					wFanShu = 9;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_YI_TIAO_LONG)).is_empty()) { // 一条龙
					wFanShu = 9;
				}
				// 勾选了清一色一条龙就是18
				if (has_rule(Constants_SXTuiDaoHu.GAME_RULE_QING_YI_SE_AND_LONG_JIA_FAN)
						&& !(chr.opr_and(Constants_SXTuiDaoHu.CHR_QING_YI_SE)).is_empty()
						&& !(chr.opr_and(Constants_SXTuiDaoHu.CHR_YI_TIAO_LONG)).is_empty()) {
					wFanShu = 18;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
					wFanShu = 9;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_HAO_HUA_QI_XIAO_DUI)).is_empty()) { // 豪华七小对
					wFanShu = 18;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_QYS_YTL)).is_empty()) { // 清一色一条龙
					wFanShu = 18;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_SHI_SAN_YAO)).is_empty()) { // 十三幺
					wFanShu = 27;
				}
			}
		} else if (hu_card_type == Constants_SXTuiDaoHu.HU_CARD_TYPE_ZI_MO) { // 自摸胡
			// 如果勾选了平胡，则所有胡只按平胡算
			if (has_rule(Constants_SXTuiDaoHu.GAME_RULE_ONLY_PI_HU)) {
				wFanShu = 2;
			} else {
				wFanShu = 2;
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_QING_YI_SE)).is_empty()) { // 清一色
					wFanShu = 9;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
					wFanShu = 9;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_YI_TIAO_LONG)).is_empty()) { // 一条龙
					wFanShu = 9;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_HAO_HUA_QI_XIAO_DUI)).is_empty()) { // 豪华七小对
					wFanShu = 18;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_QYS_YTL)).is_empty()) { // 清一色一条龙
					wFanShu = 18;
				}
				if (!(chr.opr_and(Constants_SXTuiDaoHu.CHR_SHI_SAN_YAO)).is_empty()) { // 十三幺
					wFanShu = 27;
				}
			}
		}

		return wFanShu;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		// 查牌数据
		this.setGameEndBasicPrama(game_end);

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
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;
			// 是否有人杠，流局时有人杠则庄家下家庄，没人则庄家继续庄
			boolean hasgang = false;
			// ***********************************将杠分拿到游戏结束的时候来算**************************
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
						int gang_fen = 1;
						int cbGangIndex = this.GRR._gang_score[i].gang_count++;

						if (no_bao_ting_dian_pao[i]) {// **********如果未报听情况下点炮，点炮者的杠做废*************
							this.GRR._gang_score[i].scores[cbGangIndex][i] = 0;
						} else if (bao_ting_dian_pao[i]) { // 如果报听情况下点炮。点炮者的杠分三家给（直杠、补杠、暗杠）,正常情况补杠和暗杆都是三家给的，这里就只考虑直杠的
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {// 直杠
								hasgang = true; // ,这种情况下直杠就是三家给杠分了
								// 放杠1分
								// 如果荒庄的话 杠分都作废
								if (GameConstants.Game_End_DRAW != reason) {
									for (int k = 0; k < this.getTablePlayerNumber(); k++) {
										if (i == k)
											continue;
										this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * GameConstants.CELL_SCORE;
										// this._player_result.game_score[k] -=
										// gang_fen * GameConstants.CELL_SCORE;
										// this._player_result.game_score[i] +=
										// gang_fen * GameConstants.CELL_SCORE;

									}
								}
								this._player_result.ming_gang_count[i]++;

							}
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_AN_GANG) {// 暗杠
								hasgang = true;
								if (GameConstants.Game_End_DRAW != reason) {
									for (int k = 0; k < this.getTablePlayerNumber(); k++) {
										if (i == k)
											continue;

										// 暗杠每人2分
										this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * 2 * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * 2 * GameConstants.CELL_SCORE;

										// this._player_result.game_score[k] -=
										// gang_fen * 2 *
										// GameConstants.CELL_SCORE;
										// this._player_result.game_score[i] +=
										// gang_fen * 2 *
										// GameConstants.CELL_SCORE;

									}
								}
								this._player_result.an_gang_count[i]++;
							}
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_ADD_GANG) {// 补杠
								hasgang = true;
								if (GameConstants.Game_End_DRAW != reason) {
									for (int k = 0; k < this.getTablePlayerNumber(); k++) {
										if (i == k)
											continue;

										// 组合牌杠每人1分
										this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * GameConstants.CELL_SCORE;
										// this._player_result.game_score[k] -=
										// gang_fen * GameConstants.CELL_SCORE;
										// this._player_result.game_score[i] +=
										// gang_fen * GameConstants.CELL_SCORE;

									}
								}
								this._player_result.ming_gang_count[i]++;
							}
						} else {// **********************正常情况下的杠分***********************
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
								hasgang = true;
								// 放杠1分
								// 判断当前玩家杠这个牌时，提供牌的玩家是不是报听状态
								if (GameConstants.Game_End_DRAW != reason) {
									// 如果勾选了点杠包杠玩法
									if (GRR._weave_items[i][j].is_lao_gang) { // 报听点杠三家扣分;
										for (int k = 0; k < this.getTablePlayerNumber(); k++) {
											if (i == k)
												continue;
											this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * GameConstants.CELL_SCORE;
											this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * GameConstants.CELL_SCORE;

											// this._player_result.game_score[k]
											// -= gang_fen *
											// GameConstants.CELL_SCORE;
											// this._player_result.game_score[i]
											// += gang_fen *
											// GameConstants.CELL_SCORE;
										}
									} else {// 不报听点杠包杠（包三家的杠）
										this.GRR._gang_score[i].scores[cbGangIndex][i] += 3 * gang_fen * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][GRR._weave_items[i][j].provide_player] -= 3 * gang_fen
												* GameConstants.CELL_SCORE;

										// this._player_result.game_score[GRR._weave_items[i][j].provide_player]
										// -= 3
										// * gang_fen *
										// GameConstants.CELL_SCORE;
										// this._player_result.game_score[i] +=
										// 3 * gang_fen *
										// GameConstants.CELL_SCORE;
									}

								}
								this._player_result.ming_gang_count[i]++;

							}
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_AN_GANG) {
								hasgang = true;
								if (GameConstants.Game_End_DRAW != reason) {
									for (int k = 0; k < this.getTablePlayerNumber(); k++) {
										if (i == k)
											continue;

										// 暗杠每人2分
										this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * 2 * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * 2 * GameConstants.CELL_SCORE;

										// this._player_result.game_score[k] -=
										// gang_fen * 2 *
										// GameConstants.CELL_SCORE;
										// this._player_result.game_score[i] +=
										// gang_fen * 2 *
										// GameConstants.CELL_SCORE;
									}
								}
								this._player_result.an_gang_count[i]++;
							}
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_ADD_GANG) {
								hasgang = true;
								if (GameConstants.Game_End_DRAW != reason) {
									for (int k = 0; k < this.getTablePlayerNumber(); k++) {
										if (i == k)
											continue;

										// 组合牌杠每人1分
										this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * GameConstants.CELL_SCORE;

										// this._player_result.game_score[k] -=
										// gang_fen * GameConstants.CELL_SCORE;
										// this._player_result.game_score[i] +=
										// gang_fen * GameConstants.CELL_SCORE;
									}
								}

								this._player_result.ming_gang_count[i]++;
							}
						}

					}
				}
			}
			if (GameConstants.Game_End_DRAW == reason) {
				if (hasgang) {
					// 流局时有人杠则庄家下家庄
					this._cur_banker = get_banker_next_seat(this._cur_banker);
				}

			}

			// ***********************************将杠分拿到游戏结束的时候来算**************************

			// // 杠牌，每个人的分数
			float lGangScore[] = new float[this.getTablePlayerNumber()];

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				// 记录
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 每个小局的分数=胡分+杠分
				GRR._game_score[i] += lGangScore[i];
				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			// 这个是设立结算时的庄家用户还是下局的庄家？答：只是结算时的
			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

			this.set_result_describe();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

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

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		/*
		 * if (!end) { GameSchedule.put(new
		 * LuHeReadyRunnable(this.getRoom_id()), 10, TimeUnit.SECONDS); }
		 */

		return false;
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee_not_gold(this, get_seat_index);
		}
		return true;

	}

	public boolean handler_request_trustee_action(int get_seat_index, boolean isTrustee) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		return true;
	}

	/**
	 * 重连发送托管状态
	 * 
	 * @param get_seat_index
	 */
	// public void sendIsTruetee(int get_seat_index) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
	// roomResponse.setOperatePlayer(get_seat_index);
	// roomResponse.setIstrustee(istrustee[get_seat_index]);
	// this.send_response_to_room(roomResponse);
	//
	// if (GRR != null) {
	// GRR.add_room_response(roomResponse);
	// }
	// }

	@Override
	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER];
		}

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态

		if (is_sys() && (st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD)) {
			_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index), GameConstants.TRUSTEE_TIME_OUT_SECONDS,
					TimeUnit.SECONDS);
		}
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		handler_request_trustee(_seat_index, true, 0);
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (max_index < index) {
				max_index = index;
			}

		}

		return max_index;
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 地胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}

		// 补张牌等级
		if (player_action == GameConstants.WIK_BU_ZHNAG) {
			return 30;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 15;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT) {
			return 10;
		}

		return 0;
	}
}
