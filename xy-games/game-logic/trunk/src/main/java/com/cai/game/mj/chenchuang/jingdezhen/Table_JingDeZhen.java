package com.cai.game.mj.chenchuang.jingdezhen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_JING_DE_ZHEN;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 景德镇麻将
 */
public class Table_JingDeZhen extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public HandlerSelectMagic_JingDeZhen handler_select_magic;
	public HandlerPao_JingDeZhen _handler_pao;
	public HandlerNao_JingDeZhen _handler_nao;
	
	public int[] score_when_abandoned_jie_pao = new int[getTablePlayerNumber()]; // 可以胡没有胡记录可以胡的牌型翻数
	public int[] is_bao_ding = new int[getTablePlayerNumber()];//是否报定
	public int[] player_bai_count = new int[getTablePlayerNumber()];//摆牌的数量
	public long[] bai_pai_xing = new long[getTablePlayerNumber()];//玩家摆的牌型;
	public int[][] cpg_count = new int[getTablePlayerNumber()][getTablePlayerNumber()];//没有报听或摆牌前打给其他玩家吃碰杠次数
	public boolean[] is_bao_pai = new boolean[getTablePlayerNumber()];//该玩家是否包赔
	@SuppressWarnings("unchecked")
	public Set<Integer>[] can_bai_out_card = new HashSet[3];//打出牌能摆牌的数量
	{for (int i = 0; i < 3; i++)can_bai_out_card[i] = new HashSet<Integer>();}
	
	public int[] out_bao_count = new int[getTablePlayerNumber()]; //玩家打出宝牌的数量
	public int[] in_bao_count = new int[getTablePlayerNumber()];//玩家摸进宝牌的数量
	public int continueBankerCount;//连续坐庄次数
	public int lastBanker = -1;//上局庄家;
	
	public int[] zhi_gang_count = new int[getTablePlayerNumber()];
	public int[] wan_gang_count = new int[getTablePlayerNumber()];
	public int[] an_gang_count = new int[getTablePlayerNumber()];
	public int[] ming_gang_count = new int[getTablePlayerNumber()];
	
	public int[] bp_mo_count = new int[getTablePlayerNumber()];//摆牌后摸牌次数
	
	public Set<Integer> baiPaiPlayerHuCards = new HashSet<Integer>();
	
	public boolean _is_nao_zhuang;//防止庄家多次闹
	public boolean[] _is_pao_qiang = new boolean[getTablePlayerNumber()];//防止玩家多次飘
	
	public long[] bai_describe = new long[getTablePlayerNumber()];//摆牌描述
	public int[] xiao_tao_count = new int[getTablePlayerNumber()];//小讨个数
			
	
	public Table_JingDeZhen() {
		super(MJType.GAME_TYPE_MJ_JING_DE_ZHEN);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_JingDeZhen();
		_handler_dispath_card = new HandlerDispatchCard_JingDeZhen();
		_handler_gang = new HandlerGang_JingDeZhen();
		_handler_out_card_operate = new HandlerOutCardOperate_JingDeZhen();
		handler_select_magic = new HandlerSelectMagic_JingDeZhen();
		_handler_pao = new HandlerPao_JingDeZhen();
		_handler_nao = new HandlerNao_JingDeZhen();
	}

	public boolean exe_select_magic() {
		this.set_handler(this.handler_select_magic);
		handler_select_magic.reset_status(_cur_banker);
		_handler.exe(this);

		return true;
	}
	
	/**
	 * 发牌
	 * @return
	 */
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player) {
		boolean is_all = is_bao_ding[seat_index] == 2 || player_bai_count[seat_index] > 0;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(is_all ? 3 : 2);// get牌
		roomResponse.setCardCount(count);

		if (to_player == GameConstants.INVALID_SEAT) {
			// 实时存储牌桌上的数据，方便回放时，任意进度读取
			operate_player_cards_record(seat_index, 1);

			this.send_response_to_other(seat_index, roomResponse);

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}
			GRR.add_room_response(roomResponse);
			if(is_all)
				return this.send_response_to_room(roomResponse);
			else
				return this.send_response_to_player(seat_index, roomResponse);

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			}
			// GRR.add_room_response(roomResponse);
			return this.send_response_to_player(to_player, roomResponse);
		}

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
		boolean is_ming_pai = is_bao_ding[seat_index] == 2 || player_bai_count[seat_index] > 0;
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(is_ming_pai ? 3 : 1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		if(is_ming_pai){
			for (int j = 0; j < card_count; j++) {
				roomResponse.addCardData(cards[j]);
			}
		}
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
		if(!is_ming_pai){
			for (int j = 0; j < card_count; j++) {
				roomResponse.addCardData(cards[j]);
			}
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		if (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_MAI_PIAO))
			_handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		return false;
	}
	
	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		_handler_nao.handler_nao_zhuang(this, player.get_seat_index(), nao);
		return false;
	}
	
	

	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		time_for_tou_zi_animation = 2000;
		//初始化变量
		baiPaiPlayerHuCards = new HashSet<Integer>();
		score_when_abandoned_jie_pao = new int[getTablePlayerNumber()];
		is_bao_ding = new int[getTablePlayerNumber()];
		player_bai_count = new int[getTablePlayerNumber()];
		bai_pai_xing = new long[getTablePlayerNumber()];
		cpg_count = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		is_bao_pai = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < 3; i++)can_bai_out_card[i] = new HashSet<Integer>();
		
		out_bao_count = new int[getTablePlayerNumber()]; 
		in_bao_count = new int[getTablePlayerNumber()];
		
		zhi_gang_count = new int[getTablePlayerNumber()];
		wan_gang_count = new int[getTablePlayerNumber()];
		an_gang_count = new int[getTablePlayerNumber()];
		ming_gang_count = new int[getTablePlayerNumber()];
		
		bp_mo_count = new int[getTablePlayerNumber()];
		
		bai_describe = new long[getTablePlayerNumber()];
		xiao_tao_count = new int[getTablePlayerNumber()];//小讨个数
		
		_is_nao_zhuang = false;
		
		if(lastBanker == _cur_banker){
			continueBankerCount++;
		}else
			continueBankerCount = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.biaoyan[i] = 0;
			_player_result.ziba[i] = 0;
			_is_pao_qiang[i] = false;
			_player_result.qiang[i] = 0;
		}
		//庄家买码
		this.set_handler(this._handler_nao);
		this._handler_nao.exe(this);
		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_QIANG_GANG_HU);//抢杠胡
		} else if (card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_DIAN_PAO_HU);//接炮
		}else if (card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_GSKH) {
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_GANG_SHANG_KAI_HUA);//杠上开花
		}
		//清一色、碰碰胡、七对、豪华七对、双豪华七对、幺仁、全字清一色需要摆牌
		//基础胡牌牌型，3n+2、七对、19字一色、烂胡
		
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		
		//潇洒
		if(bp_mo_count[_seat_index] == 1 && card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_ZI_MO)
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_XIAO_SA);
		//门清
		boolean is_men_qing = (card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_ZI_MO || card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_GSKH) && is_men_qing(weaveItems, weave_count);
		if(is_men_qing)
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_MEN_PING);
		//七对
		int is_xiao_qi_dui = is_xiao_qi_dui(temp_cards_index, weave_count);
		
		//幺胡
		boolean is_yao_jiu = is_yao_jiu(temp_cards_index, weaveItems, weave_count);
		
		//判断十三烂 ，0不是， 1是，2七星十三烂
		int shiSanLan = isShiSanLan(temp_cards_index, weave_count);
		if(shiSanLan == 1)
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU);
		if(shiSanLan == 2){
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU);
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_QI_ZI);
			if(chiHuRight.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU).is_empty())
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU);
			if(cur_card >= 0x31){
				if(chiHuRight.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA).is_empty())
					chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA);
			}
		}
		//0字一色1清一色
		int se_count = get_se_count(temp_cards_index, weaveItems, weave_count);
		//是否胡牌牌型
		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, cur_card_index, null, 0);
		//if(player_bai_count[_seat_index] > 0){
			if(is_xiao_qi_dui == 2)
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI);
			if(is_xiao_qi_dui == 1)
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI);
			if(is_xiao_qi_dui == 0 && analyse_win_by_cards_index)
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI);
			else if(is_xiao_qi_dui == 0)
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI);
			if(is_xiao_qi_dui != -1){
				if(chiHuRight.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA).is_empty())
					chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA);
				if(chiHuRight.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU).is_empty())
					chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU);
			}
			if(se_count == 0)
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE);
			if(se_count == 1)
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE);
			if((analyse_win_by_cards_index || is_xiao_qi_dui != -1) && is_yao_jiu){
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN);
				if(chiHuRight.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU).is_empty())
					chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU);
			}else if(is_yao_jiu)
				chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_HU);
			
		//}
		if(!(analyse_win_by_cards_index || is_yao_jiu || shiSanLan > 0 || is_xiao_qi_dui != -1)){
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		//碰碰胡
		boolean is_peng_hu = is_no_chi(weaveItems, weave_count) && AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, null, 0);
		if(is_peng_hu/* && player_bai_count[_seat_index] > 0*/){
			chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU);
		}
		//判断全求人
		int tmp_card_count = _logic.get_card_count_by_index(temp_cards_index);
	    if (tmp_card_count == 2 && analyse_win_by_cards_index) {
	    	chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_QUAN_QIU_REN);
	    }
	    //死掐
	    boolean is_si_qia = is_jia_zi(cards_index, cur_card) || is_hu_dui(temp_cards_index, cur_card_index) || is_dan_diao(temp_cards_index, cur_card_index) || (cur_card >= 0x35 && shiSanLan == 0 && chiHuRight.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_HU).is_empty());
	    if(is_si_qia){
	    	if(chiHuRight.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA).is_empty())
	    		chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA);
	    }
	    //一条龙
	    boolean is_yi_tiao_long = is_yi_tiao_long(temp_cards_index, weaveItems, weave_count);
	    if(is_yi_tiao_long)
	    	chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_YI_TIAO_LONG);
	    
	    if(GRR._left_card_count == _player_result.qiang[_cur_banker] && (card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_ZI_MO || card_type == Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_GSKH)){
	    	chiHuRight.opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_HAI_DI_LAO_YUE);
	    }
	    
		return GameConstants.WIK_CHI_HU;
	}

	

	/**
	 * 判断抢杠胡
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
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			// 吃胡判断
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_QIANG_GANG, i);
			// 可以胡的情况 判断 ,强杠胡不用判断是否过圈
			if (playerStatus.is_chi_hu_round()/* || getPaiXingScore(chr, seat_index) > score_when_abandoned_jie_pao[seat_index]*/) {

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		if(seat_index == GRR._banker_player && continueBankerCount >= 5){
			boolean is_fan_bei = (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN) || has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN));
			//“长毛×5开始（包括长毛×5），庄家每多胡一局或流局，向其他玩家多讨赏10分，直到换庄
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if(i == seat_index)
					continue;
				GRR._game_score[i] -= is_fan_bei ? 20 : 10;
				GRR._game_score[seat_index] += is_fan_bei ? 20 : 10;
			}
		}
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		int score = getHuPaiScore(chr, seat_index);
		int niao = GRR._player_niao_count[seat_index];
		int pao = _player_result.pao[seat_index];
		int outBao = player_bai_count[seat_index] > 1 ? out_bao_count[seat_index] * player_bai_count[seat_index] : out_bao_count[seat_index];
		int chang_mao_score = player_bai_count[seat_index] > 1 ? player_bai_count[seat_index] * continueBankerCount : continueBankerCount;
		////////////////////////////////////////////////////// 自摸 算分
		boolean is_bp = false;
		int bp = 0;
		for (int p = 0; p < getTablePlayerNumber() && player_bai_count[seat_index] > 0; p++) {
			if(cpg_count[seat_index][p] >= 3){
				is_bao_pai[p] = true;
				is_bp = true;
				bp = p;
			}
		}
		if (zimo) {//自摸
			
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int lChiHuScore = score + chang_mao_score + outBao;//长毛、飞宝
				boolean is_zhaung = (seat_index == GRR._banker_player || i == GRR._banker_player);
				lChiHuScore = is_zhaung ? lChiHuScore + (player_bai_count[seat_index] > 1 ? player_bai_count[seat_index] : 1) : lChiHuScore;
				if(is_zhaung && (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN) || has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN))){
					lChiHuScore *= 2;
				}
				lChiHuScore = lChiHuScore + niao + pao + _player_result.pao[i];//中鸟、飘
				
				if(is_bp)
					GRR._game_score[bp] -= lChiHuScore;
				else
					GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		}else {//点炮
			int lChiHuScore = score + chang_mao_score + outBao;
			boolean is_zhaung = (seat_index == GRR._banker_player || provide_index == GRR._banker_player);
			lChiHuScore = is_zhaung ? lChiHuScore + (player_bai_count[seat_index] > 1 ? player_bai_count[seat_index] : 1) : lChiHuScore;
			if(is_zhaung && (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN) || has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN))){
				lChiHuScore *= 2;
			}
			lChiHuScore = lChiHuScore + niao + pao + _player_result.pao[provide_index];
			GRR._game_score[provide_index] -= lChiHuScore;
			GRR._game_score[seat_index] += lChiHuScore;
			
			if(player_bai_count[seat_index] > 0){
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index || i == provide_index) {
						continue;
					}
					int cms = chang_mao_score;
					boolean is_z = (seat_index == GRR._banker_player || i == GRR._banker_player);
					if(is_z && (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN) || has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN))){
						cms *= 2;
					}
					int s = pao + _player_result.pao[i] + cms;
					if(is_bp)
						GRR._game_score[bp] -= s;
					else
						GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
				if(is_bp){
					int cms = chang_mao_score;
					if(is_zhaung && (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN) || has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN))){
						cms *= 2;
					}
					GRR._game_score[bp] -= pao + _player_result.pao[provide_index] + cms;
					GRR._game_score[provide_index] += pao + _player_result.pao[provide_index] + cms;
				}
			}
		}
			
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private int getHuPaiScore(ChiHuRight chr,int seat_index) {
		int score = 2;//胡牌分
		//清一色、碰碰胡、七对、豪华七对、双豪华七对、幺仁、全字清一色需要摆牌
		int bpCount = player_bai_count[seat_index];
		long px = bai_pai_xing[seat_index];
		boolean lanhu = false;
		if(bpCount > 0){
			score += (bpCount - 1) * 2;//胡牌分
			int pxCount = 0;
			for(int i = 0; i < 1; i++){
				if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI).is_empty()){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI;
					score += 20;
					score += 1;
					lanhu = true;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
				if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI).is_empty()){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI;
					score += 14;
					score += 1;
					lanhu = true;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
				if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI).is_empty()){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI;
					score += 14;
					score += 1;
					lanhu = true;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
				if((px & Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE) != 0){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE;
					score += 8;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
				if((px & Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE) != 0){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE;
					score += 8;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
				if((px & Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI) != 0){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI;
					score += 8;
					score += 1;
					lanhu = true;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
				if((px & Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN) != 0){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN;
					score += 8;
					score += 1;
					lanhu = true;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
				if((px & Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU) != 0){
					bai_describe[seat_index] |= Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU;
					score += 6;
					pxCount++;
				}
				if(pxCount == bpCount)
					break;
			}
		}
		
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_MEN_PING).is_empty())
			score += bpCount > 1 ? bpCount : 1;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_HU).is_empty() ||
				(player_bai_count[seat_index] == 0 && !chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN).is_empty()))
			score++;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU).is_empty() && !lanhu)
			if(has_lan_describe(seat_index))
				score++;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA).is_empty()){
			if(bpCount > 1){//七对摆牌只有幺仁不算死掐
				if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI).is_empty() ||
						!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI).is_empty() ||
						!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI).is_empty()){
					//int cur_card_index = _logic.switch_to_card_index(GRR._chi_hu_card[seat_index][0]);
					if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN).is_empty()){
						score += 2 * (bpCount - 1);
					}else{
						score += 2 * bpCount;
					}
				}else{
					score += 2 * bpCount;
				}
			}else{
				score += 2;
			}
		}
		if(zhi_gang_count[seat_index] > 0)
			score += bpCount > 1 ? zhi_gang_count[seat_index] * bpCount : zhi_gang_count[seat_index];
		if(wan_gang_count[seat_index] > 0)
			score += bpCount > 1 ? wan_gang_count[seat_index] * bpCount : wan_gang_count[seat_index];
		if(an_gang_count[seat_index] > 0)
			score += bpCount > 1 ? an_gang_count[seat_index] * 2 * bpCount : an_gang_count[seat_index] * 2;
		if(ming_gang_count[seat_index] > 0)
			score += bpCount > 1 ? ming_gang_count[seat_index] * 6 * bpCount : ming_gang_count[seat_index] * 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QIANG_GANG_HU).is_empty())
			score += bpCount > 1 ? 6 * bpCount: 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_GANG_SHANG_KAI_HUA).is_empty())
			score += bpCount > 1 ? 6 * bpCount: 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QI_ZI).is_empty())
			score += bpCount > 1 ? 6 * bpCount: 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YI_TIAO_LONG).is_empty())
			score += bpCount > 1 ? 6 * bpCount: 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QUAN_QIU_REN).is_empty())
			score += bpCount > 1 ? 6 * bpCount: 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_XIAO_SA).is_empty())
			score += bpCount > 1 ? 6 * bpCount: 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_HAI_DI_LAO_YUE).is_empty())
			score += bpCount > 1 ? 6 * bpCount: 6;
		
		return score;
	}
	
	public int get_real_card(int card) {
		if (card > 8200) {
			card -= 8200;
		} else if (card > 5000) {
			card -= 5000;
		} else if (card > 3200)
			card -= 3200;

		return card;
	}

	@Override
	protected void set_result_describe() {
		//清一色、碰碰胡、七对、豪华七对、双豪华七对、幺仁、全字清一色需要摆牌
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_QIANG_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_DIAN_PAO_HU) {
						result.append(" 点炮胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_GANG_SHANG_KAI_HUA) {
						result.append(" 杠上开花");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_MEN_PING) {
						result.append(" 门清");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_YAO_HU) {
						result.append(" 幺胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU) {
						if(has_lan_describe(player))
							result.append(" 烂胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA) {
						result.append(" 死掐");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU && (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU) != 0) {
						result.append(" 碰碰胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_QI_ZI) {
						result.append(" 七字");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI && (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI) != 0) {
						result.append(" 七对");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI && (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI) != 0) {
						result.append(" 豪华七对");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI && (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI) != 0) {
						result.append(" 双豪华七对");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI && (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI) != 0) {
						result.append(" 仁胡七对");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE && (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE) != 0) {
						result.append(" 清一色");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN) {
						if((bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN) != 0)
							result.append(" 幺仁");
						else
							result.append(" 幺胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_YI_TIAO_LONG) {
						result.append(" 一条龙");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_QUAN_QIU_REN) {
						result.append(" 全求人");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_XIAO_SA) {
						result.append(" 潇洒");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_HAI_DI_LAO_YUE) {
						result.append(" 海底捞月");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE && (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE) != 0) {
						result.append(" 全字清一色");
					}
				} else{
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_FANG_PAO) {
						result.append(" 放炮");
					}
					if (type == Constants_MJ_JING_DE_ZHEN.CHR_BEI_QIANG_GANG) {
						result.append(" 被抢杠");
					}
				}
			}

			if(GRR._chi_hu_rights[player].is_valid()){
				if(continueBankerCount > 0)
					result.append(" 长毛x" + continueBankerCount);
				if(out_bao_count[player] > 0)
					result.append(" 飞宝x" + out_bao_count[player]);
				if(zhi_gang_count[player] > 0)
					result.append(" 直杠x" + zhi_gang_count[player]);
				if(wan_gang_count[player] > 0)
					result.append(" 弯杠x" + wan_gang_count[player]);
				if(ming_gang_count[player] > 0)
					result.append(" 明杠x" + ming_gang_count[player]);
				if(an_gang_count[player] > 0)
					result.append(" 暗杠x" + an_gang_count[player]);
				if(GRR._player_niao_count[player] > 0)
					result.append(" 中码x" + GRR._player_niao_count[player]);
			}
			
			if(in_bao_count[player] == 3)
				result.append(" 三宝");
			if(in_bao_count[player] == 4)
				result.append(" 四宝");
			if(xiao_tao_count[player] > 0)
				result.append(" 小讨x" + xiao_tao_count[player]);
			if(is_bao_ding[player] == 2)
				result.append(" 报定");
			if(player_bai_count[player] > 0)
				result.append(" 摆牌x" + player_bai_count[player]);
			if(is_bao_pai[player]){
				String b = "";
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if(player_bai_count[j] > 0){
						if(cpg_count[j][player] >= 3){
							String n = get_players()[j].getNick_name();
							b += n.substring(0,n.length() > 6 ? 6 : n.length()) + ",";
						}
					}
				}
				result.append(" 三包(" + b.substring(0, b.length() - 1) + ")");
			}
			if(!GRR._chi_hu_rights[player].is_valid() && player_bai_count[player] > 0){
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE) != 0)
					result.append(" 清一色");
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU) != 0)
					result.append(" 碰碰胡");
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI) != 0)
					result.append(" 七对");
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI) != 0)
					result.append(" 豪华七对");
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI) != 0)
					result.append(" 双豪华七对");
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI) != 0)
					result.append(" 仁胡七对");
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN) != 0)
					result.append(" 幺仁");
				if((bai_pai_xing[player] & Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE) != 0)
					result.append(" 全字清一色");
				if(continueBankerCount > 0)
					result.append(" 长毛x" + continueBankerCount);
			}

			GRR._result_des[player] = result.toString();
		}
	}
	
	public boolean has_lan_describe(int player){
		boolean flag = true;
		if(!GRR._chi_hu_rights[player].opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI).is_empty()
				&& (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI) == 0)
			flag = false;
		if(!GRR._chi_hu_rights[player].opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI).is_empty()
				&& (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI) == 0)
			flag = false;
		if(!GRR._chi_hu_rights[player].opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI).is_empty()
				&& (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI) == 0)
			flag = false;
		if(!GRR._chi_hu_rights[player].opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI).is_empty()
				&& (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI) == 0)
			flag = false;
		if(!GRR._chi_hu_rights[player].opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN).is_empty()
				&& (bai_describe[player] & Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN) == 0)
			flag = false;
		return flag;
	}





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
			if(GRR._left_card_count > _player_result.qiang[_cur_banker] && is_bao_ding[i] != 2 && player_bai_count[i] == 0){
				if (i == get_banker_next_seat(seat_index)) {
					action = check_chi(GRR._cards_index[i], card);
	
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CENTER);
						_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_LEFT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_LEFT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}
					if ((action & Constants_MJ_JING_DE_ZHEN.WIK_LL_CHI) != 0) {
						_playerStatus[i].add_action(Constants_MJ_JING_DE_ZHEN.WIK_LL_CHI);
						_playerStatus[i].add_chi(card, Constants_MJ_JING_DE_ZHEN.WIK_LL_CHI, seat_index);
					}
					if ((action & Constants_MJ_JING_DE_ZHEN.WIK_RR_CHI) != 0) {
						_playerStatus[i].add_action(Constants_MJ_JING_DE_ZHEN.WIK_RR_CHI);
						_playerStatus[i].add_chi(card, Constants_MJ_JING_DE_ZHEN.WIK_RR_CHI, seat_index);
					}
	
					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}
	
				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng = false;
						break;
					}
				}
				if (can_peng) {
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(Constants_MJ_JING_DE_ZHEN.WIK_ZHI_GANG);
					add_action_weave(playerStatus, card, seat_index, 1, Constants_MJ_JING_DE_ZHEN.WIK_ZHI_GANG);
					bAroseAction = true;
				}
			}

			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			int card_type = Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_DIAN_PAO;
			action = analyse_chi_hu_card(GRR._cards_index[i],
					GRR._weave_items[i], cbWeaveCount, card, chr,
					card_type, i);
			if (playerStatus.is_chi_hu_round()/*  || getPaiXingScore(chr, seat_index) > score_when_abandoned_jie_pao[i]*/) {
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
		} else {
			return false;
		}

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int data) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				cards[count++] = cbCurrentCard;
				if(is_bao_ding[seat_index] != 2 && player_bai_count[seat_index] == 0) {
					long canBaiPaiXing = setCanBaiPaiXing(chr,seat_index);
					int numberOf1 = numberOf1(canBaiPaiXing);
					if(numberOf1 >= 1)
						can_bai_out_card[0].add(data);
					if(numberOf1 >= 2)
						can_bai_out_card[1].add(data);
					if(numberOf1 > 2)
						can_bai_out_card[2].add(data);
				}
			}
		}
		
		//全听
		if (count == GameConstants.MAX_ZI_FENG) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}
	
	public void set_ting_card_bai(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		_playerStatus[seat_index]._hu_card_count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				long canBaiPaiXing = setCanBaiPaiXing(chr,seat_index);
				int numberOf1 = numberOf1(canBaiPaiXing);
				if(player_bai_count[seat_index] >= 0){
					if(numberOf1 >= player_bai_count[seat_index]){
						bai_pai_xing[seat_index] |= canBaiPaiXing;
						baiPaiPlayerHuCards.add(cbCurrentCard);
						_playerStatus[seat_index]._hu_cards[_playerStatus[seat_index]._hu_card_count++] = cbCurrentCard;
					}
				}
			}
		}
		//刷新听的牌
		int ting_cards[] = _playerStatus[seat_index]._hu_cards;
		int ting_count = _playerStatus[seat_index]._hu_card_count;
		operate_chi_hu_cards(seat_index, ting_count, ting_cards);
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	public int numberOf1(long n){
        int count = 0;
        while(n!=0){
            n = n&(n-1);
            count++;
        }
        return count;
    }
	
	public long setCanBaiPaiXing(ChiHuRight chr, int seat_index){
		long paixing = 0;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE).is_empty()){
			paixing |= Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE;
		}
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU).is_empty()){
			paixing |= Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU;
		}
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI).is_empty()){
			paixing |= Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI;
		}
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI).is_empty()){
			paixing |= Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI;
		}
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI).is_empty()){
			boolean flag = false;
			for (int i = 0; i < 34; i++) {
				if(GRR._cards_index[seat_index][i] == 4){
					flag = true;
					break;
				}
			}
			if(flag)
				paixing |= Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI;
			else
				paixing |= Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI;
		}
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI).is_empty()){
			int hh = 0;
			for (int i = 0; i < 34; i++) {
				if(GRR._cards_index[seat_index][i] == 4){
					hh++;
				}
			}
			if(hh >= 2)
				paixing |= Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI;
			else
				paixing |= Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI;
		}	
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN).is_empty()){
			paixing |= Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN;
		}
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE).is_empty()){
			paixing |= Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE;
		}
		return paixing;
	}
	
	public int getPaiXingScore(ChiHuRight chr, int provide_index){
		//清一色、碰碰胡、七对、豪华七对、双豪华七对、幺仁、全字清一色不算
		boolean is_zhuang = provide_index == _cur_banker;
		int score = 2;
		if(is_zhuang)
			score += 1;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_HU).is_empty())
			score += 1;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_LAN_HU).is_empty())
			score += 1;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_SI_QIA).is_empty())
			score += 2;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QIANG_GANG_HU).is_empty())
			score += 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QI_ZI).is_empty())
			score += 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YI_TIAO_LONG).is_empty())
			score += 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_QUAN_QIU_REN).is_empty())
			score += 6;
		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_XIAO_SA).is_empty())
			score += 6;
		if((has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN)||has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN)) && is_zhuang)
			score = (score - 1) * 2;
		return score;
	}
	
	
	
    
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, boolean check_weave, int cards_abandoned_gang[]) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;
        
		if (GRR._left_card_count > _player_result.qiang[GRR._banker_player]) {
	        // 手上杠牌
	        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
	            if (cards_index[i] == 4) {
	                cbActionMask |= GameConstants.WIK_GANG;
	                int index = gangCardResult.cbCardCount++;
	                gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
	                gangCardResult.isPublic[index] = 0;// 暗刚
	                gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
	            }
	        }
	
	        if (check_weave) {
	            // 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
	            for (int i = 0; i < cbWeaveCount; i++) {
	                if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
	                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
	                        if (cards_index[j] != 1) { //  || cards_abandoned_gang[j] != 0
	                            continue;
	                        } else {
	                            if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
	                                cbActionMask |= GameConstants.WIK_GANG;
	                                int index = gangCardResult.cbCardCount++;
	                                gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
	                                gangCardResult.isPublic[index] = 1;// 明刚
	                                gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
	                                break;
	                            }
	                        }
	                    }
	                }
	            }
	        }
		}
        return cbActionMask;
    }
    
    /**
     * 处理牌桌结束逻辑
     * 
     * @param seat_index
     * @param reason
     */
    protected boolean on_handler_game_finish(int seat_index, int reason) {
        int real_reason = reason;

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            _player_ready[i] = 0;
            _player_result.biaoyan[i] = 0;
			_player_result.ziba[i] = 0;
        }
        operate_player_info();
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
        GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
        // 查牌数据
        this.setGameEndBasicPrama(game_end);

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
        if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
                          // == MJGameConstants.Game_End_DRAW ||
            // (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
        	lastBanker = GRR._banker_player;
    		for (int i = 0; i < getTablePlayerNumber(); i++) {
    			if(in_bao_count[i] == 4 || (i == GRR._banker_player && (in_bao_count[i] == 3 || player_bai_count[i] > 0))){
    				_cur_banker = GRR._banker_player;
    				break;
    			}
    		}
            game_end.setRoundOverType(1);
            game_end.setStartTime(GRR._start_time);

            game_end.setGameTypeIndex(GRR._game_type_index);
            roomResponse.setLeftCardCount(GRR._left_card_count);

            // 特别显示的牌
            if (this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN)
                    || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT)) {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(
                            GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
                }
            } else {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
                }
            }

            GRR._end_type = reason;

            // 杠牌，每个人的分数
            float lGangScore[] = new float[getTablePlayerNumber()];
            boolean liu_ju = reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY;
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                // 记录
                for (int j = 0; j < getTablePlayerNumber(); j++) {
                    _player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
                }
                
                for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
            		for (int k = 0; k < getTablePlayerNumber(); k++) {
            			lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
            		}
            	}

            }
            setLiuJuScore(liu_ju);

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
            // 设置中鸟数据
            for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
                game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
            }
            game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GRR._count_niao; j++) {
                    pnc.addItem(GRR._player_niao_cards[i][j]);
                }
                for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
                    pnc.addItem(GRR._player_niao_cards_fei[i][j]);
                }
                game_end.addPlayerNiaoCards(pnc);
                game_end.addHuResult(GRR._hu_result[i]);

                // 胡的牌
                Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][h])) {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]);
					}
				}

                game_end.addHuCardArray(hc);
            }

            // 现在权值只有一位
            long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

            // 设置胡牌描述
            this.set_result_describe();

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._card_count[i] = switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

                Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GRR._card_count[i]; j++) {
                	int data = GRR._cards_data[i][j] > 3000 ? GRR._cards_data[i][j] - 3000 : GRR._cards_data[i][j];
					if (_logic.is_magic_card(data)) {
						cs.addItem(GRR._cards_data[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
                game_end.addCardsData(cs);// 牌

                // 组合
                WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
                for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                    WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                    weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
                    if(GRR._weave_items[i][j].weave_kind == GameConstants.WIK_PENG ||
                    		GRR._weave_items[i][j].weave_kind == Constants_MJ_JING_DE_ZHEN.WIK_ZHI_GANG ||
                    		GRR._weave_items[i][j].weave_kind == Constants_MJ_JING_DE_ZHEN.WIK_WAN_GANG){
                    	if(getTablePlayerNumber() == 2){
                    		weaveItem_item.setProvidePlayer(2);
                    	}
                    	if(getTablePlayerNumber() == 3){
                    		if((i + 1) % 3 == GRR._weave_items[i][j].provide_player)
                    			weaveItem_item.setProvidePlayer(3);
                    		else
                    			weaveItem_item.setProvidePlayer(1);
                    	}
                    	if(getTablePlayerNumber() == 4){
                    		if((i + 1) % 4 == GRR._weave_items[i][j].provide_player)
                    			weaveItem_item.setProvidePlayer(3);
                    		else if((i + 2) % 4 == GRR._weave_items[i][j].provide_player)
                    			weaveItem_item.setProvidePlayer(2);
                    		else
                    			weaveItem_item.setProvidePlayer(1);
                    	}
                    }else
                    	weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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
                game_end.addPao(_player_result.pao[i]);

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

        ////////////////////////////////////////////////////////////////////// 得分总的
        roomResponse.setGameEnd(game_end);

        this.send_response_to_room(roomResponse);

        record_game_round(game_end);

        // 结束后刷新玩家
        // RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
        // roomResponse2.setGameStatus(_game_status);
        // roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
        // this.load_player_info_data(roomResponse2);
        // this.send_response_to_room(roomResponse2);

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
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            _player_result.qiang[i] = -1;
            _player_result.pao[i] = -1;
        }
        // 错误断言
        return false;
    }
    
    
    
	
	/**
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(_logic.is_magic_card(cards[i])?cards[i]+GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI:cards[i]);
			// roomResponse.addOutCardTing(this.GRR._cards_index[seat_index][this._logic.switch_to_card_index(cards[i])]);

		}

		this.send_response_to_player(seat_index, roomResponse);
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
	
	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					int data = _logic.switch_to_card_data(i);
					cards_data[cbPosition++] = baiPaiPlayerHuCards.contains(data) ? data + 3000 : data;
				}
			}
		}
		return cbPosition;
	}
	
	public void add_action_weave(PlayerStatus curPlayerStatus, int card, int provider, int bai_count, int wik) {
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].public_card = bai_count;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].center_card = card;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].weave_kind = wik;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].provide_player = provider;
		curPlayerStatus._weave_count++;
	}
	
	// 吃牌判断
	public int check_chi(int cards_index[], int cur_card) {
		// 变量定义
		int excursion[] = new int[] { 0, 1, 2 };
		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT
				, Constants_MJ_JING_DE_ZHEN.WIK_LL_CHI, Constants_MJ_JING_DE_ZHEN.WIK_RR_CHI};

		// 吃牌判断
		int eat_type = 0, first_index = 0;
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		if(cur_card_index > 26 && cur_card_index < 31){
			if(cur_card_index == 27){
				if(cards_index[cur_card_index + 1] != 0 && cards_index[cur_card_index + 2] != 0)
					eat_type |= eat_type_check[0];
				if(cards_index[cur_card_index + 1] != 0 && cards_index[cur_card_index + 3] != 0)
					eat_type |= eat_type_check[3];
				if(cards_index[cur_card_index + 2] != 0 && cards_index[cur_card_index + 3] != 0)
					eat_type |= eat_type_check[4];
			}
			if(cur_card_index == 28){
				if(cards_index[cur_card_index + 1] != 0 && cards_index[cur_card_index + 2] != 0)
					eat_type |= eat_type_check[0];
				if(cards_index[cur_card_index - 1] != 0 && cards_index[cur_card_index + 1] != 0)
					eat_type |= eat_type_check[1];
				if(cards_index[cur_card_index - 1] != 0 && cards_index[cur_card_index + 2] != 0)
					eat_type |= eat_type_check[3];
			}
			if(cur_card_index == 29){
				if(cards_index[cur_card_index - 1] != 0 && cards_index[cur_card_index + 1] != 0)
					eat_type |= eat_type_check[1];
				if(cards_index[cur_card_index - 1] != 0 && cards_index[cur_card_index - 2] != 0)
					eat_type |= eat_type_check[2];
				if(cards_index[cur_card_index - 2] != 0 && cards_index[cur_card_index + 1] != 0)
					eat_type |= eat_type_check[4];
			}
			if(cur_card_index == 30){
				if(cards_index[cur_card_index - 1] != 0 && cards_index[cur_card_index - 2] != 0)
					eat_type |= eat_type_check[2];
				if(cards_index[cur_card_index - 2] != 0 && cards_index[cur_card_index - 3] != 0)
					eat_type |= eat_type_check[3];
				if(cards_index[cur_card_index - 1] != 0 && cards_index[cur_card_index - 3] != 0)
					eat_type |= eat_type_check[4];
			}
			
		}else if(cur_card_index > 30){
			if(cur_card_index == 31 && cards_index[cur_card_index + 1] != 0 && cards_index[cur_card_index + 2] != 0)
				eat_type |= eat_type_check[0];
			if(cur_card_index == 32 && cards_index[cur_card_index + 1] != 0 && cards_index[cur_card_index - 1] != 0)
				eat_type |= eat_type_check[1];
			if(cur_card_index == 33 && cards_index[cur_card_index - 1] != 0 && cards_index[cur_card_index - 2] != 0)
				eat_type |= eat_type_check[2];
		}else{
			for (int i = 0; i < 3; i++) {
				int value_index = cur_card_index % 9;
				if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= 6)) {
					// 吃牌判断
					first_index = cur_card_index - excursion[i];
					
					if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
						continue;
					if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
						continue;
					if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
						continue;
					// 设置类型
					eat_type |= eat_type_check[i];
				}
			}
		}

		return eat_type;
	}
	
	public int is_xiao_qi_dui(int cards_index[], int cbWeaveCount) {

 		// 组合判断
 		if (cbWeaveCount != 0)
 			return -1;

 		// 单牌数目
 		int cbReplaceCount = 0;
 		int nGenCount = 0;

 		// 计算单牌
 		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
 			int cbCardCount = cards_index[i];
 				// 单牌统计
 				if (cbCardCount == 1 || cbCardCount == 3)
 					cbReplaceCount++;

 				if (cbCardCount == 4) {
 					nGenCount++;
 				}
 		}

		if (cbReplaceCount > 0)
			return -1;

 		if (nGenCount == 1)
 			return 1;
 		if (nGenCount >= 2)
 			return 2;
 		return 0;
 	}
	
	public boolean is_yao_jiu(int[] hand_indexs, WeaveItem weaveItem[], int weaveCount) {
    	// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI; i ++) {
			if(hand_indexs[i] == 0)
				continue;
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value != 1 && value != 9)
				return false;
		}
    	
		// 落地牌都是19
        for (int i = 0; i < weaveCount; i++) {
        	if(_logic.switch_to_card_index(weaveItem[i].center_card) >= GameConstants.MAX_ZI)
        		continue;
        	if(weaveItem[i].weave_kind == GameConstants.WIK_LEFT ||
        			weaveItem[i].weave_kind == GameConstants.WIK_CENTER ||
        			weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)
        		return false;
            int value = _logic.get_card_value(weaveItem[i].center_card);
            if (value != 1 && value != 9)
				return false;
        }
        return true;
    }
	
	//判断十三烂 ，0不是， 1是，2七星十三烂
    public int isShiSanLan(int[] cards_index,int weaveCount) {
		if (weaveCount != 0) {
			return 0;
		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cards_index[j] == 0) {
					continue;
				}
				if (cards_index[j] > 1) {
					return 0;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cards_index[j + 1] != 0) {
					return 0;
				}
				if (j + 2 < limitIndex && cards_index[j + 2] != 0) {
					return 0;
				}
			}
		}

		int count = 0;
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] > 1) {
				return 0;
			}
			if(cards_index[i] == 1)
				count++;
		}
		if(count == 7)
			return 2;
		return 1;
	}
    
    public int get_se_count(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		boolean has_feng = false;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}
			int card = _logic.switch_to_card_data(i);
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = _logic.get_card_color(card);
			if (cbCardColor > 2){
				has_feng = true;
				continue;
			}
			cbQueYiMenColor[cbCardColor] = 0;
		}
		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = _logic.get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cbCardColor > 2){
				has_feng = true;
				continue;
			}
			cbQueYiMenColor[cbCardColor] = 0;
		}
		int count = 0;
		for (int i = 0; i < 3; i++) {
			if (cbQueYiMenColor[i] == 0) {
				count += 1;
			}
		}
		if(count != 0 && has_feng)
			count++;
		return count;
	}
    
    public boolean is_men_qing(WeaveItem weaveItem[], int weaveCount) {
    	if(weaveCount == 0)
    		return true;
    	
    	// 都是明杠
        for (int i = 0; i < weaveCount; i++) {
            if(weaveItem[i].weave_kind != Constants_MJ_JING_DE_ZHEN.WIK_MING_GANG)
            	return false;
        }
        return true;
    }
    
    public boolean is_no_chi(WeaveItem weaveItem[], int weaveCount) {
    	if(weaveCount == 0)
    		return true;
    	
    	// 都是暗杠
        for (int i = 0; i < weaveCount; i++) {
            if(weaveItem[i].weave_kind == GameConstants.WIK_LEFT ||
            		weaveItem[i].weave_kind == GameConstants.WIK_CENTER ||
            		weaveItem[i].weave_kind == GameConstants.WIK_RIGHT ||
            		weaveItem[i].weave_kind == Constants_MJ_JING_DE_ZHEN.WIK_LL_CHI ||
            		weaveItem[i].weave_kind == Constants_MJ_JING_DE_ZHEN.WIK_RR_CHI)
            	return false;
        }
        return true;
    }
    
    public boolean is_jia_zi(int cards_index[], int card) {
		int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
		if(_logic.get_card_color(card) == 3)
			return false;
		int value = _logic.get_card_value(card);
		if(value == 1 || value == 9)
			return false;
		if(value == 3){
			if(copyOf[_logic.switch_to_card_index(card - 1)] != 0 &&
					copyOf[_logic.switch_to_card_index(card - 2)] != 0){
				copyOf[_logic.switch_to_card_index(card - 1)] -= 1;
				copyOf[_logic.switch_to_card_index(card - 2)] -= 1;
				boolean hu;
					hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
				copyOf[_logic.switch_to_card_index(card - 1)] += 1;
				copyOf[_logic.switch_to_card_index(card - 2)] += 1;
			}
		}else if(value == 7){
			if(copyOf[_logic.switch_to_card_index(card + 1)] != 0 &&
					copyOf[_logic.switch_to_card_index(card + 2)] != 0){
				copyOf[_logic.switch_to_card_index(card + 1)] -= 1;
				copyOf[_logic.switch_to_card_index(card + 2)] -= 1;
				boolean hu;
					hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
				copyOf[_logic.switch_to_card_index(card + 1)] += 1;
				copyOf[_logic.switch_to_card_index(card + 2)] += 1;
			}
		}

		if(copyOf[_logic.switch_to_card_index(card - 1)] == 0 ||
				copyOf[_logic.switch_to_card_index(card + 1)] == 0)
			return false;
		copyOf[_logic.switch_to_card_index(card - 1)] -= 1;
		copyOf[_logic.switch_to_card_index(card + 1)] -= 1;
		
		boolean hu;
			hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
		
		return hu;
	}
    
    public boolean is_hu_dui(int[] cards_index, int cur_card_index) {
		if(cards_index[cur_card_index] < 3)
			return false;
		int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
		copyOf[cur_card_index] -= 3;
		if(!AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0))
			return false;
		return true;
	}
    
    public boolean is_dan_diao(int[] temp_cards_index, int cur_card_index) {
		if(temp_cards_index[cur_card_index] < 2)
			return false;
		int[] copyOf = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		copyOf[cur_card_index] -= 2;
		boolean hu = true;
		for (int i = 0; i < 9; i++) {
			if(copyOf[i] >= 3)
				continue;
			copyOf[i]++;
			hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, i, null, 0);
			copyOf[i]--;
			if(!hu)
				return false;
		}
		return hu;
	}
    
    public boolean is_yi_tiao_long(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		Set<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(cards_index[i] == 0){
				set.add(_logic.get_card_color(_logic.switch_to_card_data(i)));
			}
 		}
		//手上有同花色1-9时检查能不能胡
		if(set.size() != 3){
			int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
			if(!set.contains(0)){
				for(int i = 0; i < 9; i++)
					copyOf[i]--;
			}else if(!set.contains(1)){
				for(int i = 9; i < 18; i++)
					copyOf[i]--;
			}else if(!set.contains(2)){
				for(int i = 18; i < 27; i++)
					copyOf[i]--;
			}
			boolean hu;
				hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
			if(hu)
				return hu;
		}
		
		// 检查有吃情况下，一条龙判断
		if(weaveCount == 0)
			return false;
		int[][] chi3 = new int[3][3]; 
		for (int i = 0; i < weaveCount; i++) {
			if(weaveItem[i].center_card < 0x31){
				int value = _logic.get_card_value(weaveItem[i].center_card);
				int color = _logic.get_card_color(weaveItem[i].center_card);
				boolean is123 = (value == 1 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 2 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 3 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT);
				boolean is456 = (value == 4 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 5 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 6 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT);
				boolean is789 = (value == 7 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 8 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 9 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT);
				//一个吃的情况
				if(is123){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 3; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if(is456){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 3; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					for(int c = color * 9 + 6; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if(is789){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 6; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				
				if(is123)
					chi3[color][0] = 1;
				if(is456)
					chi3[color][1] = 1;
				if(is789)
					chi3[color][2] = 1;
			}
		}
			
		for(int c = 0; c < 3; c++){
			//三个吃的情况
			if(chi3[c][0] == 1 && chi3[c][1] == 1 && chi3[c][2] == 1)
				return true;
			
			//两个吃的情况
			if(chi3[c][0] == 1 && chi3[c][1] == 1){
				int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
				boolean flag = true;
				for(int cd = c * 9 + 6; cd < c * 9 + 9; cd++){
					if(copyOf[cd] == 0){
						flag = false;
						break;
					}
					copyOf[cd]--;
				}
				boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
			}
			if(chi3[c][0] == 1 && chi3[c][2] == 1){
				int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
				boolean flag = true;
				for(int cd = c * 9 + 3; cd < c * 9 + 6; cd++){
					if(copyOf[cd] == 0){
						flag = false;
						break;
					}
					copyOf[cd]--;
				}
				boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
			}
			if(chi3[c][1] == 1 && chi3[c][2] == 1){
				int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
				boolean flag = true;
				for(int cd = c * 9 + 0; cd < c * 9 + 3; cd++){
					if(copyOf[cd] == 0){
						flag = false;
						break;
					}
					copyOf[cd]--;
				}
				boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
			}
		}
		
		return false;
	}
    
    
    /**
     * @param seat_index 抓鸟的玩
     * @param t是否通炮
     */
    public void set_niao_card(int seat_index, boolean t){

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;
		GRR._count_niao = _player_result.qiang[GRR._banker_player];
		if(GRR._count_niao > 0){
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			
			set_pick_niao_cards(GRR._cards_data_niao, GRR._count_niao, seat_index, t);
		}
	}
	
	private static final int[][] birdVaule = new int[][]{{1,5,9,0x31},{2,6,0x32,0x35},{3,7,0x33,0x36},{4,8,0x34,0x37}};
	public void set_pick_niao_cards(int cards_data[], int card_num, int seat_index, boolean t) {
		int seat = (seat_index - GRR._banker_player + getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < card_num; i++) {
			boolean flag = false;
			int data = cards_data[i];
			int nValue = data < 0x31 ? _logic.get_card_value(cards_data[i]) : data;
			for(int v = 0; v < birdVaule[seat].length; v++){
				if(nValue == birdVaule[seat][v]){
					flag = true;
					break;
				}
			}
			if(t){
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					if(GRR._chi_hu_rights[p].is_valid()){
						if (flag) {
							GRR._player_niao_cards[p][i] = data + GameConstants.DING_NIAO_VALID;
							GRR._player_niao_count[p]++;
						}else{
							GRR._player_niao_cards[p][i] = data;
						}
					}
				}
			}else{
				if (flag) {
					GRR._player_niao_cards[seat_index][i] = data + GameConstants.DING_NIAO_VALID;
					GRR._player_niao_count[seat_index]++;
				}else{
					GRR._player_niao_cards[seat_index][i] = data;
				}
			}

		}
	}
	
	public int getBaiPaiCount(){
		if(!can_bai_out_card[2].isEmpty())
			return 3;
		if(!can_bai_out_card[1].isEmpty())
			return 2;
		if(!can_bai_out_card[0].isEmpty())
			return 1;
		return 0;
	}
	
	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			if(curPlayerStatus._action_weaves[i].weave_kind == GameConstants.WIK_BAO_TING || 
					curPlayerStatus._action_weaves[i].weave_kind == Constants_MJ_JING_DE_ZHEN.WIK_BAI_PAI){
				if(!can_bai_out_card[0].isEmpty()){
					for(int card : can_bai_out_card[0]){
						roomResponse.addOutCardTingCount(card);
					}
				}
				if(!can_bai_out_card[1].isEmpty()){
					for(int card : can_bai_out_card[1]){
						roomResponse.addOutCardTing(card);
					}
				}
				if(!can_bai_out_card[2].isEmpty()){
					for(int card : can_bai_out_card[2]){
						roomResponse.addCardsList(card);
					}
				}
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}
	
	public void setLiuJuScore(boolean liu_ju){
		/*if(liu_ju){
			//飘分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int pao = _player_result.pao[i];
				if(pao > 0){
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						GRR._game_score[j] -= pao;
						GRR._game_score[i] += pao;
					}
				}
			}
		}*/
		
		//宝牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(in_bao_count[i] > 2){
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					int bScore = in_bao_count[i] == 4 ? 12 : 6;
					if(j == i)
						continue;
					if((i == GRR._banker_player || j == GRR._banker_player) && has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN)){
						bScore *= 2;
					}
					GRR._game_score[j] -= bScore;
					GRR._game_score[i] += bScore;
				}
			}
		}
			
		//报定分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(is_bao_ding[i] == 2){
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if(j == i)
						continue;
					int bds = 6;
					if((i == GRR._banker_player || j == GRR._banker_player) && (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN) || has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN))){
						bds = 12;
					}
					GRR._game_score[j] -= bds;
					GRR._game_score[i] += bds;
				}
			}
		}
		
		//摆牌分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(player_bai_count[i] > 0){
				ChiHuRight chr = GRR._chi_hu_rights[i];
				long px = chr.is_valid() ? bai_pai_xing[i] & setCanBaiPaiXing(chr, i) : bai_pai_xing[i];
				int baiPaiScore = getBaiPaiScore(px,player_bai_count[i]);
				int baiPaiScore2 = !chr.is_valid() ? baiPaiScore + (player_bai_count[i] > 1 ? player_bai_count[i] * continueBankerCount : continueBankerCount) : baiPaiScore;//长毛分
				boolean is_bp = false;
				int bp = 0;
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					if(cpg_count[i][p] >= 3){
						is_bao_pai[p] = true;
						is_bp = true;
						bp = p;
					}
				}
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if(j == i)
						continue;
					int baiPaiScore1 = baiPaiScore2;
					if((i == GRR._banker_player || j == GRR._banker_player) && (has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN) || has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN_BU_FAN))){
						baiPaiScore1 *= 2;
						//baiPaiScore1 -= baiPaiScore;
					}
					if(!chr.is_valid())//飘分
						baiPaiScore1 += _player_result.pao[j] +_player_result.pao[i];
					if(is_bp)
						GRR._game_score[bp] -= baiPaiScore1;
					else
						GRR._game_score[j] -= baiPaiScore1;
					GRR._game_score[i] += baiPaiScore1;
				}
			}
		}
		
		//小讨分
        if(has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_XIAO_TAO)){
        	for (int i = 0; i < getTablePlayerNumber(); i++) {
        		ChiHuRight chr = GRR._chi_hu_rights[i];
        		int index = _logic.switch_to_card_index(_provide_card);
        		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_ZI_MO).is_empty() ||
        				!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_GANG_SHANG_KAI_HUA).is_empty()){
        			GRR._cards_index[i][index]++;
        		}
        		for (int j = 0; j < GameConstants.MAX_ZI_FENG; j++) {
        			if(GRR._cards_index[i][j] == 4){
        				xiao_tao_count[i]++;
        			}
				}
        		if(!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_ZI_MO).is_empty() ||
        				!chr.opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_GANG_SHANG_KAI_HUA).is_empty()){
        			GRR._cards_index[i][index]--;
        		}
        		xiao_tao_count[i] += ming_gang_count[i];
        		xiao_tao_count[i] += an_gang_count[i];
			}
        	
        	for (int i = 0; i < getTablePlayerNumber(); i++) {
        		if(xiao_tao_count[i] > 0){
        			for (int j = 0; j < getTablePlayerNumber(); j++) {
        				if(i == j)
        					continue;
        				int xts = xiao_tao_count[i] * 2;
        				if((i == GRR._banker_player || j == GRR._banker_player) && has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_DAI_BEN)){
        					xts *= 2;
        				}
        				GRR._game_score[i] += xts;
        				GRR._game_score[j] -= xts;
        			}
        		}
        	}
        }
	}
	
	public int getBaiPaiScore(long px, int bpCount){
		int pxScore = 0;
		int pxCount = 0;
		for(int i = 0; i < 1; i++){
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_2HH_QI_DUI) != 0){
				pxScore += 18;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_RN_QI_DUI) != 0){
				pxScore += 14;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_HH_QI_DUI) != 0){
				pxScore += 12;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_QING_YI_SE) != 0){
				pxScore += 8;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_QZ_QING_YI_SE) != 0){
				pxScore += 8;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_QI_DUI) != 0){
				pxScore += 8;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN) != 0){
				pxScore += 8;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
			if((px & Constants_MJ_JING_DE_ZHEN.CHR_PENG_PENG_HU) != 0){
				pxScore += 6;
				pxCount++;
			}
			if(pxCount == bpCount)
				break;
		}
		return pxScore;
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

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}
		// 杠牌等级
		if (player_action == Constants_MJ_JING_DE_ZHEN.WIK_MING_GANG) {
			return 30;
		}
		// 杠牌等级
		if (player_action == Constants_MJ_JING_DE_ZHEN.WIK_WAN_GANG) {
			return 30;
		}
		// 杠牌等级
		if (player_action == Constants_MJ_JING_DE_ZHEN.WIK_AN_GANG) {
			return 30;
		}
		// 杠牌等级
		if (player_action == Constants_MJ_JING_DE_ZHEN.WIK_ZHI_GANG) {
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
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT
				 || player_action == Constants_MJ_JING_DE_ZHEN.WIK_LL_CHI || player_action == Constants_MJ_JING_DE_ZHEN.WIK_RR_CHI) {
			
			return 10;
		}

		return 0;
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
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// filtrate_right(seat_index, chr);

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																																	// 2017/7/10
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
		} else {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);
		}

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
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
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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
			/*if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {*/
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			//}

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
	
    
    @Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01,0x01,0x02,0x02,0x03,0x03,0x04,0x04,0x05,0x05,0x36,0x36,0x04 };
		int[] cards_of_player1 = new int[] { 0x01,0x01,0x02,0x02,0x03,0x03,0x04,0x04,0x05,0x05,0x36,0x36,0x04 };
		int[] cards_of_player2 = new int[] { 0x12,0x12,0x12,0x11,0x11,0x11,0x31,0x32,0x33,0x34,0x35,0x36,0x37 };
		int[] cards_of_player3 = new int[] { 0x12,0x12,0x12,0x11,0x11,0x11,0x31,0x32,0x33,0x34,0x35,0x36,0x37 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic
						.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
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
