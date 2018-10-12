package com.cai.game.mj.handler.henansmx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_SMX extends AbstractMJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;

	BrandLogModel _recordRoomRecord;

	public MJHandlerPao_HeNan_smx _handler_pao_henna_smx;// 河南跑
	public MJHandlerHun_Hennan_smx _handler_hun_henna_smx;// 河南混
	public MJHandlerBiaoYan_HeNan_smx _handler_biaoyan_henna_smx;

	public MJTable_SMX() {
		super(MJType.GAME_TYPE_HENNAN_SMX);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_HeNan_smx();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HeNan_smx();
		_handler_gang = new MJHandlerGang_HeNan_smx();
		_handler_chi_peng = new MJHandlerChiPeng_HeNan_smx();
		_handler_pao_henna_smx = new MJHandlerPao_HeNan_smx();
		_handler_hun_henna_smx = new MJHandlerHun_Hennan_smx();
		_handler_biaoyan_henna_smx = new MJHandlerBiaoYan_HeNan_smx();
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		// 跑分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;// 清掉 默认是-1
		}
		// 闹庄
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
		}

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJGameConstants.GAME_PLAYER;//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) { // 112张
			_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_SMX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_SMX);
		} else { // 108张
			_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_SMX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_SMX);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_gang_mo_posion = 0;
		for (int i = 0; i < 2; i++) {
			_gang_mo_cards[i] = GameConstants.INVALID_CARD;
		}

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
					}
				}
				_biaoyan_count[i] = 0;
			}
		} catch (Exception e) {

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;

		int count = has_rule(GameConstants.GAME_RULE_HUNAN_THREE) ? GameConstants.GAME_PLAYER - 1 : GameConstants.GAME_PLAYER;
		// 分发扑克
		for (int i = 0; i < count; i++) {
			// if(GRR._banker_player == i){
			// send_count = MJGameConstants.MAX_COUNT;
			// }else{
			//
			// send_count = (MJGameConstants.MAX_COUNT - 1);
			// }
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;

		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	protected void test_cards() {

		// 黑摸
		// int cards[] = new int[] { 0x06, 0x07, 0x08, 0x14, 0x15,0x16, 0x16,
		// 0x24, 0x25, 0x26, 0x27, 0x28, 0x29 };

		// 晃晃不能胡
		// int cards[] = new int[] { 0x12, 0x13, 0x16, 0x17, 0x18,0x22, 0x22,
		// 0x24, 0x25, 0x26, 0x28, 0x28, 0x28 };

		// 晃晃没显示听癞子
		// int cards[] = new int[] { 0x02, 0x04, 0x06, 0x06, 0x06,0x14, 0x15,
		// 0x16, 0x21, 0x21, 0x21, 0x22, 0x22 };

		// 晃晃没显示听癞子
		// int cards[] = new int[] { 0x22, 0x06, 0x06, 0x06, 0x06, 0x14, 0x15,
		// 0x16, 0x21, 0x21, 0x21, 0x21, 0x22 };

		// 双鬼没显示听鬼
		// int cards[] = new int[] { 0x01, 0x02, 0x15, 0x15, 0x16,0x17, 0x18,
		// 0x26, 0x26, 0x26, 0x27, 0x28, 0x29 };

		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };

		// 红中显示听多牌
		// int cards[] = new int[]
		// {0x02,0x03,0x04,0x07,0x08,0x09,0x11,0x11,0x11,0x18,0x18,0x25,0x26};
		// 杠5饼出错
		// int cards[] = new int[]
		// {0x02,0x02,0x06,0x07,0x22,0x22,0x22,0x25,0x25,0x02,0x27,0x28,0x29};

		// 五鬼不能胡
		// int cards[] = new int[]
		// {0x01,0x01,0x03,0x03,0x06,0x06,0x07,0x15,0x16,0x18,0x18,0x25,0x26};

		// 起手四红中

		// int cards[] = new int[]
		// {0x11,0x11,0x12,0x12,0x13,0x13,0x14,0x14,0x19,0x19,0x19,0x19,0x16};

		// 红中不能胡1
		// int cards[] = new int[]
		// {0x11,0x12,0x12,0x13,0x14,0x16,0x16,0x17,0x18,0x21,0x22,0x23,0x35};
		// 红中不能胡2
		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x17,0x17,0x21,0x22,0x22,0x23,0x24,0x26,0x28,0x35};
		// 杠牌重复/
		// int cards[] = new int[] { 0x09, 0x09, 0x18, 0x18, 0x19, 0x19, 0x19,
		// 0x26, 0x27, 0x28, 0x29, 0x29, 0x29 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x21, 0x22, 0x23, 0x12,
		// 0x13, 0x14, 0x15, 0x16, 0x17, 0x18 };

		// 花牌
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};

		// int cards[] = new int[]
		// {0x05,0x05,0x07,0x07,0x08,0x09,0x09,0x17,0x17,0x18,0x18,0x19,0x19};
		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x04,0x05,0x06,0x16,0x16,0x21,0x21,0x21,0x15,0x15};

		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x04,0x05,0x06,0x16,0x16,0x21,0x21,0x21,0x15,0x15};
		// int cards[] = new int[]
		// {0x03,0x03,0x05,0x05,0x06,0x07,0x08,0x08,0x08,0x12,0x12,0x12,0x22};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// //int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};

		// 单吊
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};

		// 全球人
		// int cards[] = new int[] { 0x13, 0x14, 0x15, 0x16, 0x17, 0x17, 0x17,
		// 0x18, 0x18, 0x18, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x23, 0x24, 0x25, 0x16, 0x17, 0x17, 0x17,
		// 0x08, 0x08, 0x08, 0x19, 0x19, 0x19 };
		// 河南麻将七小对
		// int cards[] = new int[]
		// {0x02,0x02,0x05,0x05,0x09,0x09,0x12,0x12,0x14,0x14,0x18,0x27,0x27};

		// int cards[] = new int[] { 0x21, 0x21, 0x21, 0x03, 0x03, 0x03, 0x06,
		// 0x06, 0x06, 0x07, 0x07, 0x07, 0x09 };

		// 吃
		// int cards[] = new int[]
		// {0x01,0x02,0x05,0x05,0x21,0x23,0x24,0x26,0x26,0x27,0x28,0x29,0x29};

		// 板板胡
		// int cards[] = new int[]
		// {0x03,0x14,0x16,0x16,0x17,0x17,0x17,0x21,0x21,0x23,0x23,0x19,0x19};
		// 红中中2鸟算分不对
		// int cards[] = new int[]
		// {0x07,0x09,0x12,0x12,0x15,0x17,0x21,0x21,0x21,0x23,0x23,0x23,0x35};
		// 杠牌胡两张

		// int cards[] = new int[] { 0x01, 0x01, 0x1, 0x4, 0x2, 0x3, 0x13, 0x14,
		// 0x12, 0x21, 0x23, 0x23, 0x23 };
		// int cards[] = new int[] { 0x01, 0x06, 0x06, 0x06, 0x06, 0x04, 0x05,
		// 0x08, 0x08, 0x08, 0x09, 0x09, 0x09 };
		// int cards[] = new int[]
		// {0x01,0x06,0x06,0x06,0x03,0x04,0x05,0x08,0x08,0x08,0x09,0x09,0x09};
		// int cards[] = new int[]
		// {0x01,0x06,0x06,0x06,0x03,0x04,0x05,0x08,0x08,0x08,0x09,0x09,0x09};

		// int cards[] = new int[]
		// {0x01,0x02,0x04,0x04,0x05,0x07,0x07,0x08,0x14,0x19,0x21,0x23,0x29};
		// 四喜

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x16,
		// 0x16, 0x17, 0x17, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19,
		//
		// 0x19, 0x19 };
		// int cards[] = new int[] { 0x15, 0x19, 0x13, 0x02, 0x04, 0x03, 0x15,
		// 0x15, 0x17, 0x17, 0x19,0x17, 0x19 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };
		// int cards[] = new int[] { 0x15, 0x19, 0x13, 0x02, 0x04, 0x03, 0x15,
		// 0x15, 0x17, 0x17, 0x19,0x17, 0x19 };
		// //碰碰胡
		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x03, 0x03,0x11, 0x11,
		// 0x11, 0x15, 0x15, 0x21, 0x21, 0x21 };
		// 清一色碰碰胡
		// int cards[] = new int[] { 0x06, 0x06, 0x06, 0x06, 0x17, 0x17, 0x17,
		// 0x23, 0x33, 0x33, 0x33, 0x23, 0x23 };

		// 将将胡
		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x05, 0x05, 0x05, 0x08,
		// 0x08, 0x08, 0x12, 0x12, 0x12, 0x15 };

		// 十三幺
		// int[] cards = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		// 十三烂
		// int[] cards = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x16 };
		// 起手胡
		// int[] cards = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x16, 0x16, 0x16, 0x16 };
		// 七对
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x04 };
		// 王闯王
		int cards[] = new int[] { 0x19, 0x19, 0x19, 0x13, 0x14, 0x15, 0x13, 0x04, 0x17, 0x17, 0x16, 0x16, 0x04 };

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

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

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
	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		_logic.clean_magic_cards();

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna_smx);
			this._handler_pao_henna_smx.exe(this);
			return true;
		} else if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_SMX_BIAO_YAN_FEN)) {
			exe_biaoyan();
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
				_player_result.biaoyan[i] = 0;
			}

		}

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
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
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		for (int i = 0; i < 2; i++) {
			gameStartResponse.addOtherCards(_repertory_card[_gang_mo_posion]);
			_gang_mo_cards[i] = _repertory_card[_gang_mo_posion++];
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			exe_hun(this.GRR._banker_player);
			return true;
		}

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_henan_smx_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;

	}

	public void exe_hun(int seat_index) {
		// 出牌
		this.set_handler(this._handler_hun_henna_smx);
		this._handler_hun_henna_smx.reset_status(seat_index);
		this._handler_hun_henna_smx.exe(this);
	}

	public void exe_biaoyan() {
		// 出牌
		this.set_handler(this._handler_biaoyan_henna_smx);
		this._handler_biaoyan_henna_smx.exe(this);
	}

	// 三门峡麻将进入表演判断
	public boolean isonbiaoyan(int seat_index, int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card) {
		// 表演判断
		int cbCardIndexTemp[] = new int[GameConstants.MAX_COUNT];
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[seat_index][i];
		}
		int igc_count = _logic.magic_count(cbCardIndexTemp);
		if (igc_count == 0) {
			return false;
		}
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		if (bValue) {
			for (int i = 0; i < analyseItemArray.size(); i++) {
				AnalyseItem analyseitem = analyseItemArray.get(i);
				if (analyseitem.bMagicEye) {
					if (cur_card == analyseitem.cbCardEye || cur_card == _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	// 三门峡麻将表演2,3,4判断
	public boolean isbiaoyancontinue(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card) {
		// 表演判断
		int cbCardIndexTemp[] = new int[GameConstants.MAX_COUNT];
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[_out_card_player][i];
		}
		int igc_count = _logic.magic_count(cbCardIndexTemp);
		if (igc_count < 1) {
			return false;
		}
		if (cur_card != _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
			return false;
		}
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		if (bValue) {
			for (int i = 0; i < analyseItemArray.size(); i++) {
				AnalyseItem analyseitem = analyseItemArray.get(i);
				if (analyseitem.bMagicEye) {
					if (cur_card == analyseitem.cbCardEye && analyseitem.cbCardEye == _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public int analyse_chi_hu_card_henan_smx(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type) {
		SysParamModel sysParamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(0).get(997364);

		if (sysParamModel != null && sysParamModel.getVal1() == 997364) {
			return analyse_chi_hu_card_henan_smx_new(cards_index, weaveItems, weaveCount, cur_card, chiHuRight, card_type);
		} else {
			return analyse_chi_hu_card_henan_smx_old(cards_index, weaveItems, weaveCount, cur_card, chiHuRight, card_type);
		}
	}

	public int analyse_chi_hu_card_henan_smx_old(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type) {
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU)// 如果胡牌类型是炮胡
															// 但没这个规则
															// return
		{
			return GameConstants.WIK_NULL;

		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int igc_count = _logic.magic_count(cbCardIndexTemp);

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_henan_smx_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type) {
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU)
			return GameConstants.WIK_NULL;

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
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

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return cbChiHuKind;
	}

	/**
	 * 三门峡麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_henan_smx_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		PerformanceTimer timer = new PerformanceTimer();

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		boolean isDaiFeng = has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG);
		int mj_count = GameConstants.MAX_ZI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		} else {
			mj_count = GameConstants.MAX_ZI;
		}

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_smx(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {

				cards[count] = cbCurrentCard;
				if (this._logic.is_magic_index(i)) {
					if (chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡
		if (count > 0) {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// cards[count] =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
			// + GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
		} else {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// // 看看鬼牌能不能胡
			// cbCurrentCard =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// chr.set_empty();
			// if (GameConstants.WIK_CHI_HU ==
			// analyse_chi_hu_card_henan(cbCardIndexTemp, weaveItem,
			// cbWeaveCount,
			// cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
			// cards[count] = cbCurrentCard +
			// GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
			// }
		}

		int number = isDaiFeng ? 34 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 500) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_henan(int seat_index, int card) {
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
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan_smx(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

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

	// 三门峡麻将
	public boolean estimate_player_out_card_respond_henan_smx(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		// int llcard = get_niao_card_num(true, 0);
		int llcard = 0;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {// 带混玩法 剩余14张 结算
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		if (_logic.is_magic_card(card)) {
			return false;
		}
		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能碰
				action = 0;
			}
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能杠
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			// if (_playerStatus[i].is_chi_hu_round()) {
			// // 吃胡判断
			// ChiHuRight chr = GRR._chi_hu_rights[i];//
			// playerStatus._chiHuRight;
			// chr.set_empty();
			// int cbWeaveCount = GRR._weave_count[i];
			// action = analyse_chi_hu_card_henan_smx(GRR._cards_index[i],
			// GRR._weave_items[i], cbWeaveCount, card,
			// chr, GameConstants.HU_CARD_TYPE_PAOHU);
			//
			// // 结果判断
			// if (action != 0) {
			// _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
			// _playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
			// bAroseAction = true;
			// }
			// }
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

	protected boolean on_handler_game_finish(int seat_index, int reason) {
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

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

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
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(0);// 杠牌得分
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

	/**
	 * 三门峡麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_henan_smx(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_henan(chr);// 番数
		countCardType(chr, seat_index);
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		// 庄家不翻倍
		boolean jiabei = GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_SMX_ZHUANG_NO_DOUBLE);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				s *= Math.pow(2, _biaoyan_count[seat_index]);
				// 跑
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);
				s += (_player_result.biaoyan[i] + _player_result.biaoyan[seat_index]);
				if (!jiabei) {// 庄家翻倍
					if (zhuang_hu) {
						s *= 2;// 庄家hu 别人多输一分
					} else if (GRR._banker_player == i) {// 别人hu 庄家多输一分
						s *= 2;
					}
				}

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			s *= Math.pow(2, _biaoyan_count[seat_index]);
			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);
			s += (_player_result.biaoyan[provide_index] + _player_result.biaoyan[seat_index]);

			if (!jiabei) {// 庄家翻倍
				if (GRR._banker_player == provide_index) {// 别人hu 庄家多输一分
					s *= 2;
				}
			}
			// 跑和呛
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}
		// 杠牌，每个人的分数
		float lGangScore[] = new float[GameConstants.GAME_PLAYER];
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_SMX_GAGN_SUI_HU_ZOU)) {
			for (int j = 0; j < GRR._gang_score[seat_index].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[seat_index].scores[j][k];// 杠牌，每个人的分数
				}
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}
			}
		}
		//
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._game_score[i] += lGangScore[i];
			_player_result.game_score[i] += GRR._game_score[i];
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	public boolean handler_requst_biao_yan(Player player, int biaoyan) {
		if (_handler_biaoyan_henna_smx != null) {
			_handler_biaoyan_henna_smx.handler_biaoyan(this, player.get_seat_index(), biaoyan);
		}

		return false;

	}

	private float getScore(ChiHuRight chr, float lChiHuScore) {
		float s = lChiHuScore;

		if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
			chr.da_hu_count--;
		}
		if (chr.da_hu_count > 0) {
			int count = 0;
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QISHOU_BAO_TING)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
				count++;
			}

			s = 3 * (1 << (count - 1));
		} else {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
				s = 2;
			} else {
				s = 1;
			}
		}
		return s;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;
			if (_biaoyan_count[player] != 0) {
				gameDesc.append("表演x " + _biaoyan_count[player]);
			}
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < GameConstants.GAME_PLAYER; tmpPlayer++) {
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
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		// TODO Auto-generated method stub
		if (_handler_pao_henna_smx != null) {
			return _handler_pao_henna_smx.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		}
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			if (_handler_dispath_last_card != null) {
				this.set_handler(this._handler_dispath_last_card);
				this._handler_dispath_last_card.reset_status(seat_index, type);
				this._handler.exe(this);
			}
		}

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			this._playerStatus[i]._hu_card_count = this.get_henan_smx_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		//
		// this.exe_dispatch_card(seat_index,MJGameConstants.WIK_NULL, 0);
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;

		if (_handler_dispath_last_card != null) {
			// 发牌
			this.set_handler(this._handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
			this._handler.exe(this);
		}

		return true;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
