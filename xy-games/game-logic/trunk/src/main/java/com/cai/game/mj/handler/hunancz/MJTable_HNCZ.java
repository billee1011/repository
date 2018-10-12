package com.cai.game.mj.handler.hunancz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.game.mj.handler.sg.MJHandlerChiPeng_SG;
import com.cai.game.mj.handler.sg.MJHandlerDispatchCard_SG;
import com.cai.game.mj.handler.sg.MJHandlerGang_SG;
import com.cai.service.MongoDBServiceImpl;
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

public class MJTable_HNCZ extends AbstractMJTable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;	
	private  MJHandlerPiao_HNCZ  _handler_piao_cz;
	private  MJHandlerHun_HNCZ  _handler_hun;
	
	private BrandLogModel _recordRoomRecord;
	private GameRoomRecord _gameRoomRecord;

	public MJTable_HNCZ() {
		super(MJType.GAME_TYPE_HUNAN_CHEN_ZHOU);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_HNCZ();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HNCZ();
		_handler_gang = new MJHandlerGang_HNCZ();
		_handler_chi_peng = new MJHandlerChiPeng_HNCZ();
		_handler_piao_cz=new MJHandlerPiao_HNCZ();
		_handler_hun=new MJHandlerHun_HNCZ();
	}
	
	@Override
	protected boolean on_game_start() {
		
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			this.set_handler(this._handler_piao_cz);
			this._handler_piao_cz.exe(this);
			return true;
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}
		
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
	
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);

		if (this._cur_round == 1) {
			// shuffle_players();
			this.load_player_info_data(roomResponse);
		}
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
		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
		
	}
	
	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay,int cardCount,boolean isGang) {
		this._handler_dispath_card.reset_card_count(cardCount,isGang);
		return super.exe_dispatch_card(seat_index, type, delay);
	}
	

	@Override
	//胡牌分析
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		// card_type 1 zimo 2 paohu 3qiangganghu	
		//如果勾选的规则是自摸胡，但是你传过来的胡牌类型又是点炮胡，这种是不能胡的 
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))){
			return GameConstants.WIK_NULL;		
		}
		
		//如果规则是点炮胡（即可自摸也就可以点炮）
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU) && cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] >= 1
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))){
			//如果手中有红中的牌，则只能自摸才能胡，没有红中的玩家可以接炮也可以自摸
			//如果勾选的规则是点炮胡，并且手中有红中，这个时候只能自摸，但是你传过来的的胡牌类型是点炮胡，所以是不能胡的
				return GameConstants.WIK_NULL;		
		}
		
		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		//可胡七对
		if (has_rule(GameConstants.GAME_RULE_HUNAN_QIDUI)) {
			// 七小对牌 豪华七小对
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
			}

		}
		
//		//如果勾选了红中麻将的玩法，才有作为万能牌
//		if(has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)){
//			
//		}
	
		if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
				|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
						&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {

//			cbChiHuKind = GameConstants.WIK_CHI_HU;
			cbChiHuKind = GameConstants.WIK_ZI_MO;		//判断手中是不是有四个红中，有就直接算自摸胡
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				// 这个没必要。一定是自摸
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}
		

		//可 抢杠胡
		if (_current_player == GameConstants.INVALID_SEAT && _status_gang
				&& (cbChiHuKind == GameConstants.WIK_CHI_HU)) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU))// 是否选择了抢杠胡
			{
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}

		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
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

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}


		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;

	}
	
	/**
	 * 吃胡操作
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate(int seat_index, int operate_card[], int card_count, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		return;
	}
	
	/***
	 * 玩家出牌的动作检测--玩家出牌 响应判断,是否有吃碰杠补胡
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);//出牌玩家鸟的个数
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			if(has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)){
				action=_logic.check_peng_hncz(GRR._cards_index[i], card);		//如果勾选了红中麻将的玩法，则红中不能碰不能杠
			}else{
				action = _logic.check_peng(GRR._cards_index[i], card);
			}
					
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			//判断剩下来的牌不能小于需要抓鸟的牌的个数
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				if(has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)){
					action=_logic.estimate_gang_card_out_card_hncz(GRR._cards_index[i], card);		//如果勾选了红中麻将的玩法，则红中不能碰不能杠
				}else{
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				}
				
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
	
	/***
	 * 湖南麻将杠牌检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
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
	
	
	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {
		// 牌局未开 或者等待状态 调度不需要执行
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card);
		}

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
		if (is_mj(GameConstants.GAME_ID_HUNAN) ) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
				nNum = GameConstants.ZHANIAO_1;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
				nNum = GameConstants.ZHANIAO_2;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
				nNum = GameConstants.ZHANIAO_4;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
				nNum = GameConstants.ZHANIAO_6;
			}

		} else if (is_mj(GameConstants.GAME_ID_HENAN)) {
			// 河南麻将的抓鸟
			if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO2)) {
				nNum = GameConstants.ZHANIAO_2;
			} else if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO4)) {
				nNum = GameConstants.ZHANIAO_4;
			} else if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO6)) {
				nNum = GameConstants.ZHANIAO_6;
			}
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
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_niao_card_num(true, add_niao);
		} else {
			GRR._count_niao = get_niao_card_num(false, add_niao);
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
						cbCardIndexTemp);
				// cbCardIndexTemp[0] = 3;
				// cbCardIndexTemp[1] = 0x25;
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		// 中鸟个数
		if (has_rule(GameConstants.GAME_RULE_HUNAN_JINNIAO)) {//判断是否勾选金鸟
			GRR._count_pick_niao =_logic.get_pick_jin_niao(GRR._cards_data_niao, GRR._count_niao);
		}else{
			GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		}
		
		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int seat = 0;
			if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
					|| is_mj_type(GameConstants.GAME_TYPE_CHENZHOU)) { 
				seat = (seat_index + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_CS == _game_type_index
					|| GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index) {
				seat = (GRR._banker_player + (nValue - 1) % 4) % 4;
			}
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}
	}
	
	public int set_ding_niao_valid(int card_data, boolean val) {
		// 先把值还原
		if (val) {
			if (card_data > GameConstants.DING_NIAO_INVALID && card_data < GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_INVALID;
			} else if (card_data > GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.DING_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID : card_data);
		}
	}
	
	@Override
	/**
	 * 胡牌算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
		wFanShu = _logic.get_chi_hu_action_rank_hncz(chr);

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;
		
		int real_provide_index = GameConstants.INVALID_SEAT;

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
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
		}

		/////////////////////////////////////////////// 算分//////////////////////////
		GRR._count_pick_niao = 0;//中鸟的个数
		int  no_pick_niao =0; //没有中鸟的个数
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////转转麻将抓鸟//////////////////只要159就算
					int nValue = GRR._player_niao_cards[i][j];
					nValue = nValue > 1000 ? (nValue - 1000) : nValue;
					int v = _logic.get_card_value(nValue);
					if (v == 1 || v == 5 || v == 9) {
						GRR._count_pick_niao++;
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
					} else {
						no_pick_niao++;
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟失效
					}
				
			}						
			//如果一个都没中的话，则为全中  为金鸟
			if(no_pick_niao==GRR._player_niao_count[i]){
				GRR._count_pick_niao=GRR._player_niao_count[i];
			}
			
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int s = lChiHuScore;
					//////////////////////////////////////////////// 转转麻将自摸算分//////////////////
				s += GRR._count_pick_niao;// 只算自己的
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);//如果选择飘，要算飘分

//				// 胡牌分
//				if (real_provide_index == GameConstants.INVALID_SEAT) {
//					GRR._game_score[i] -= s;
//				} else {
//					int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
//					if (niao > 0) {
//						s -= niao;// 鸟要最后处理,把上面加的鸟分减掉 ----先这样处理--年后拆分出来
//					}
//					if (i == getTablePlayerNumber() - 1) {// 循环到最后一次 才把鸟分加上
//						niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[real_provide_index];
//						if (niao > 0) {
//							s += niao;
//						}
//					}
//
//					// 全包
//					GRR._game_score[real_provide_index] -= s;
//				}
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;	
				if( (GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					s += 1;
				}
			
				s += GRR._count_pick_niao;// 算鸟分   只算自己的
				s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);;//如果选择飘，要算飘分
				
				if (real_provide_index == GameConstants.INVALID_SEAT) {
					GRR._game_score[provide_index] -= s;
				} else {
					s *= 3;
					GRR._game_score[provide_index] -= s;
				}

			GRR._game_score[seat_index] += s;
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

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
	}

	/**
	 * 	郴州麻将结果描述
	 */
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
						if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN) && i == GRR._banker_player) {
							des += " 庄家加底";
						}
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
						if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN) && i == GRR._banker_player) {
							des += " 庄家加底";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
						if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN) && i == GRR._banker_player) {
							des += " 庄家加底";
						}
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
	
	//湖南郴州检测听牌的操作
	public int get_hncz_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,boolean dai_feng) {

		// 复制数据
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
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount,cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO,i)) {					
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		
		
		for (int i = 0; i < l; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO,i)) {
				
				cards[count] = cbCurrentCard;
				if (this._logic.is_magic_index(i)) {
					if (chr.opr_and(GameConstants.GAME_RULE_HUNAN_QIDUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				count++;
			}
		}
		

		l -= 1;
//		if (count == 0) {
//			// 红中能不能胡
//			// cbCurrentCard =
//			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
//			// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
//			// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,MJGameConstants.HU_CARD_TYPE_ZIMO
//			// ) ){
//			// cards[count] = cbCurrentCard;
//			// count++;
//			// }
//		} else if (count > 0 && count < ql) {
//			// 有胡的牌。红中肯定能胡
//			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
//			count++;
//		} else {
//			// 全听
//			count = 1;
//			cards[0] = -1;
//		}

		return count;
	}
	
	
	/**
	 * 游戏完成的相关的处理
	 * @param seat_index
	 * @param reason
	 * @return
	 */
	public boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setRunPlayerId(_run_player_id);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
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
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if ((!is_sys()) && _cur_round >= _game_round) {// 局数到了
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

		if (end)// 删除
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
	
	/**
	 * 处理玩家的结果
	 * @param result
	 * @return
	 */
	@Override
	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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
				// win_idx++;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
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
			// }
		}

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}
	
	

	/**
	 * 游戏的局数的记录
	 * @param game_end
	 */
	@Override
	public void record_game_round(GameEndResponse.Builder game_end) {

		if (GRR != null) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());

			if (!is_sys()) {
				// 记录 to mangodb
				MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "",
						null, null, gzipByte, this.getRoom_id() + "",
						_recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(),
						this.getRoom_owner_account_id());
			}

		}

	}
	
	
	@Override
	public void test_cards() {
		int cards[] = new int[] { 0x09, 0x09, 0x18, 0x18, 0x19, 0x19, 0x19, 0x26, 0x27, 0x28, 0x29, 0x29, 0x29 };

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
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
	
	/**
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_hun(int seat_index) {
		// 出牌
		this.set_handler(this._handler_hun);
		this._handler_hun.reset_status(seat_index);
		this._handler_hun.exe(this);

		return true;
	}
	
	
	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		
		if (_handler_piao_cz != null) {
			return ((MJHandlerPiao_HNCZ) _handler_piao_cz).handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} 
		return false;
		
	}


	
	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public  boolean handler_requst_message_deal(Player player,int seat_index, RoomRequest room_rq,int type){
		return true;
	}


	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
