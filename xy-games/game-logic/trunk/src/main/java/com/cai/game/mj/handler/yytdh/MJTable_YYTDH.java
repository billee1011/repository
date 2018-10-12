package com.cai.game.mj.handler.yytdh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamServerUtil;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_YYTDH extends AbstractMJTable {

    private static final long serialVersionUID = 1L;

    protected MJHandlerGang_YYTDH_DispatchCard _handler_gang_yytdh;
    protected MJHandlerHaiDi_YYTDH _handler_hai_di_yytdh;
    protected MJHandlerYaoHaiDi_YYTDH _handler_yao_hai_di_yytdh;
    
    public int[] chi_color;

    public MJTable_YYTDH(MJType mjType) {
        super(mjType);
    }

    /**
     * 海底
     * 
     * @param seat_index
     * @return
     */
    public boolean exe_hai_di(int start_index, int seat_index) {
        this.set_handler(this._handler_hai_di_yytdh);
        this._handler_hai_di_yytdh.reset_status(start_index, seat_index);
        this._handler_hai_di_yytdh.exe(this);
        return true;
    }

    /**
     * 要海底
     * 
     * @param seat_index
     * @return
     */
    public boolean exe_yao_hai_di(int seat_index) {
        this.set_handler(this._handler_yao_hai_di_yytdh);
        this._handler_yao_hai_di_yytdh.reset_status(seat_index);
        this._handler_yao_hai_di_yytdh.exe(this);
        return true;
    }

    /**
     * 推倒胡麻将杠牌处理
     * 
     * @param seat_index
     * @param d
     * @return
     */
    public boolean exe_gang_yytdh(int seat_index, boolean d) {
        this.set_handler(this._handler_gang_yytdh);
        this._handler_gang_yytdh.reset_status(seat_index, d);
        this._handler_gang_yytdh.exe(this);
        return true;
    }

    // 是否听牌
    public boolean is_yytdh_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
        int handcount = _logic.get_card_count_by_index(cards_index);
        if (handcount == 1) {
            // 全求人
            return true;
        }

        // 复制数据
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        ChiHuRight chr = new ChiHuRight();
        for (int i = 0; i < GameConstants.MAX_ZI; i++) {
            chr.set_empty();
            int cbCurrentCard = _logic.switch_to_card_data(i);
            if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
                    chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index, true))
                return true;
        }
        return false;
    }

    // 推倒胡特殊算分类型,
    public ChiHuRight is_yytdh_ting_card_sroce(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
            int seat_index) {

        ChiHuRight chr_rt = new ChiHuRight();
        // 复制数据
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        ChiHuRight chr = new ChiHuRight();
        for (int i = 0; i < GameConstants.MAX_ZI; i++) {
            chr.set_empty();
            int cbCurrentCard = _logic.switch_to_card_data(i);
            if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
                    chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index, false)) {

                if (chr_rt.type_count < chr.type_count) {
                    if (chr_rt.type_count == 0) {
                        chr_rt = chr;
                    } else if (chr.da_hu_count > chr_rt.da_hu_count) {
                        chr_rt = chr;
                    }
                }
            }
        }
        return chr_rt;
    }

    /**
     * 岳阳推到胡
     * 
     * @param seat_index
     * @param operate_card
     * @param rm
     */
    public void process_chi_hu_player_operate_yytdh(int seat_index, int operate_card[], int card_count, boolean rm) {
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

    /**
     * 
     * @param seat_index
     * @param card
     *            固定的鸟
     * @param show
     * @param add_niao
     *            额外奖鸟
     * @param isTongPao
     *            --通炮不抓飞鸟
     */
    public void set_niao_card_yytdh(int seat_index, int card, boolean show, int add_niao, boolean isTongPao) {
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
        if (card == GameConstants.INVALID_VALUE) {
            GRR._count_niao = get_ding_niao_card_num_yytdh(true);
        } else {
            GRR._count_niao = get_ding_niao_card_num_yytdh(false);
        }

        if (GRR._count_niao > GameConstants.ZHANIAO_0) {
            if (card == GameConstants.INVALID_VALUE) {
                int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
                _logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
                        cbCardIndexTemp);
                GRR._left_card_count -= GRR._count_niao;
                _logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
            } else {
                for (int i = 0; i < GRR._count_niao; i++) {
                    GRR._cards_data_niao[i] = card;
                }
            }
        }
        // 中鸟个数
        GRR._count_pick_niao = _logic.get_pick_niao_count_yytdh(GRR._cards_data_niao, GRR._count_niao);

        for (int i = 0; i < GRR._count_niao; i++) {
            int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
            int seat = get_zhong_seat_by_value_yytdh(nValue, seat_index);
            GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
            GRR._player_niao_count[seat]++;
        }
    }

    private int get_zhong_seat_by_value_yytdh(int nValue, int banker_seat) {
        int seat = 0;
        if (getTablePlayerNumber() == 4) {
            seat = (banker_seat + (nValue - 1) % 4) % 4;
        } else {// 3人场//这里这么特殊处理是因为 算鸟是根据玩家逻辑位置,只有庄家上家,庄家下家,不存在对家
            int v = (nValue - 1) % 4;
            switch (v) {
            case 0:// 本家//159
                seat = banker_seat;
                break;
            case 1:// 26//下家
                seat = get_banker_next_seat(banker_seat);
                break;
            case 2:// 37//对家
                seat = get_null_seat();
                break;
            default:// 48//上家
                seat = get_banker_pre_seat(banker_seat);
                break;
            }
        }
        return seat;
    }

    /**
     * 获取长沙能抓的定鸟的 数量
     * 
     * @return
     */
    public int get_ding_niao_card_num_yytdh(boolean check) {
        int nNum = getyytdhDingNiaoNum();
        if (check == false) {
            return nNum;
        }
        if (nNum > GRR._left_card_count) {
            nNum = GRR._left_card_count;
        }
        return nNum;
    }

    /**
     * 定鸟 个数
     * 
     * @return
     */
    public int getyytdhDingNiaoNum() {
        int nNum = GameConstants.ZHANIAO_0;

        if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
            nNum = GameConstants.ZHANIAO_1;
        }
        return nNum;
    }

    /**
     * 乘法 定鸟 个数
     * 
     * @return
     */
    public int getMutlpDingNiaoNum() {
        int num = 0;
        if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1)) {
            num = GameConstants.ZHANIAO_1;
        } else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2)) {
            num = GameConstants.ZHANIAO_2;
        }
        return num;
    }

    /**
     * 是否乘法 定鸟
     * 
     * @return
     */
    public boolean isMutlpDingNiao() {
        boolean isMutlp = has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1)
                || has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2);
        return isMutlp;
    }

    // 检查长沙麻将,杠牌
    public boolean estimate_gang_yytdh_respond(int seat_index, int provider, int card, boolean d) {
        // 变量定义
        boolean bAroseAction = false;// 出现(是否)有

        PlayerStatus playerStatus = null;

        int action = GameConstants.WIK_NULL;
        if(!isChi(seat_index, card)){
        	return false;
        }
        
        playerStatus = _playerStatus[seat_index];
        // playerStatus.clean_action();

        if (playerStatus.lock_huan_zhang() == false) {
            //// 碰牌判断
            action = _logic.check_peng(GRR._cards_index[seat_index], card);
            if (action != 0) {
                playerStatus.add_action(action);
                playerStatus.add_peng(card, seat_index);

                bAroseAction = true;
            }

        }

        // 杠牌判断 如果剩余牌大于1，是否有杠
        if (GRR._left_card_count > 1) {
            if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
                playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
                playerStatus.add_bu_zhang(card, provider, 1);// 加上补涨

                if (GRR._left_card_count > 2) {
                    boolean is_ting = false;
                    boolean can_gang = false;
                    if (is_ting == true) {
                        can_gang = true;
                    } else {
                        // 把可以杠的这张牌去掉。看是不是听牌
                        int bu_index = _logic.switch_to_card_index(card);
                        int save_count = GRR._cards_index[seat_index][bu_index];
                        GRR._cards_index[seat_index][bu_index] = 0;

                        int cbWeaveIndex = GRR._weave_count[seat_index];

                        GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
                        GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
                        GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
                        GRR._weave_items[seat_index][cbWeaveIndex].provide_player = provider;
                        GRR._weave_count[seat_index]++;

                        can_gang = this.is_yytdh_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
                                GRR._weave_count[seat_index], seat_index);

                        GRR._weave_count[seat_index] = cbWeaveIndex;
                        GRR._cards_index[seat_index][bu_index] = save_count;
                    }

                    if (can_gang == true) {

                        playerStatus.add_action(GameConstants.WIK_GANG);
                        playerStatus.add_gang(card, provider, 1);// 加上杠

                    }
                }

                bAroseAction = true;
            }

        }

        // 吃胡判断
        ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
        // chr.set_empty();
        int cbWeaveCount = GRR._weave_count[seat_index];
        action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
                chr, GameConstants.HU_CARD_TYPE_GANG_PAO, seat_index, false);

        // 结果判断
        if (action != 0) {
            chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
            if (_playerStatus[seat_index].has_chi_hu() == false) {
                _playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
                _playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
            }

            bAroseAction = true;
        }

        // 推倒胡清一色可吃
        boolean flag = false;
        //_logic.is_qing_yi_se(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card)
        if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_QING_YI_SE_CHI) && checkChi(card, GRR._cards_index[seat_index],seat_index )) {
            flag = true;
        }

        if (flag) {
            int chi_seat_index = (provider + 1) % getTablePlayerNumber();
            if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
                // 长沙麻将吃操作 转转麻将不能吃
                action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
                if ((action & GameConstants.WIK_LEFT) != 0) {
                    _playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
                    _playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
                }
                if ((action & GameConstants.WIK_CENTER) != 0) {
                    _playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
                    _playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
                }
                if ((action & GameConstants.WIK_RIGHT) != 0) {
                    _playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
                    _playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
                }
                // 结果判断
                if (_playerStatus[chi_seat_index].has_action()) {
                    bAroseAction = true;
                }
            }
        }

        return bAroseAction;
    }

    /**
     * 获取牌型是否能吃
     * @param card
     * @param card_index
     * @return
     */
    public boolean checkChi(int card,int[] card_index,int seat_index){
    	int color = _logic.get_card_color(card);
    	int count = 0;
    	//要是碰了筒子牌 条、万牌就不能吃了：不与出牌一个花色
    	if(GRR._weave_count[seat_index] > 0){
    		for(int i = 0; i < GRR._weave_count[seat_index]; i++){
    			if(color != _logic.get_card_color(GRR._weave_items[seat_index][i].center_card)){
    				return false;
    			}else{
    				if(GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_GANG || GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_BU_ZHNAG){
    					if(GRR._weave_items[seat_index][i].type == GameConstants.GANG_TYPE_ADD_GANG){
    						count += 4;
    					}else{
    						count += 3;
    					}
    				}else{
    					count += 2;
    				}
    			}
    		}
    	}
    	
    	for(int i = 0; i < card_index.length; i++){
    		if(card_index[i]> 0){
    			int color2 = _logic.get_card_color(_logic.switch_to_card_data(i));
    			if(color == color2){
    				count+=card_index[i];
    			}
    		}
    	}
    	return count >= 6;
    }
    
    
    // 检查杠牌,有没有胡的
    public boolean estimate_gang_respond_yytdh(int seat_index, int card, int _action) {
        // 变量定义
        // boolean isGang = _action == GameConstants.WIK_GANG ? true : false;// 补张 不算抢杠胡
        boolean isGang = true;// 补张 不算抢杠胡

        boolean bAroseAction = false;// 出现(是否)有

        // PlayerStatus playerStatus = null;

        int action = GameConstants.WIK_NULL;

        // 动作判断
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            // 用户过滤
            if (seat_index == i)
                continue;

            // playerStatus = _playerStatus[i];
            // 可以胡的情况 判断
            // if(playerStatus.is_chi_hu_round()){
            // 吃胡判断
            ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
            chr.set_empty();
            int cbWeaveCount = GRR._weave_count[i];
            action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
                    GameConstants.HU_CARD_TYPE_QIANGGANG, i, false);

            // 结果判断
            if (action != 0) {
                if (isGang) {
                    chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
                }
                _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
                _playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
                bAroseAction = true;
            }
            // }
        }

        if (bAroseAction == true) {
            _provide_player = seat_index;// 谁打的牌
            _provide_card = card;// 打的什么牌
            _resume_player = _current_player;// 保存当前轮到的玩家
            _current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
        }

        return bAroseAction;
    }

    // 玩家出牌的动作检测
    public boolean estimate_player_out_card_respond_yytdh(int seat_index, int card) {
        // 变量定义
        boolean bAroseAction = false;// 出现(是否)有

        // 用户状态
        for (int i = 0; i < getTablePlayerNumber(); i++) {

            _playerStatus[i].clean_action();
            _playerStatus[i].clean_weave();
        }

        PlayerStatus playerStatus = null;

        int action = GameConstants.WIK_NULL;

        // 动作判断
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            // 用户过滤
            if (seat_index == i)
                continue;
            if(!isChi(i, card)){
            	continue;
            }
            playerStatus = _playerStatus[i];

            if (_playerStatus[i].lock_huan_zhang() == false) {
                //// 碰牌判断
                action = _logic.check_peng(GRR._cards_index[i], card);
                if (action != 0) {
                    playerStatus.add_action(action);
                    playerStatus.add_peng(card, seat_index);
                    bAroseAction = true;
                }
            }
            // 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
            if (GRR._left_card_count > 1) {
                action = _logic.estimate_gang_card_out_card_hy(GRR._cards_index[i], card);
                if (action != 0) {
                    if (_playerStatus[i].lock_huan_zhang() == false) {
                        playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
                        playerStatus.add_bu_zhang(card, seat_index, 1);// 加上补张
                    }

                    // 剩一张为海底
                    if (GRR._left_card_count > 2) {
                        // 把可以杠的这张牌去掉。看是不是听牌
                        // int bu_index = _logic.switch_to_card_index(card);
                        // int save_count = GRR._cards_index[i][bu_index];
                        // GRR._cards_index[i][bu_index]=0;

                        // boolean is_ting =
                        // is_cs_ting_card(GRR._cards_index[i],
                        // GRR._weave_items[i], GRR._weave_count[i]);

                        boolean is_ting = false;
                        // 把牌加回来
                        // GRR._cards_index[i][bu_index]=save_count;
                        boolean can_gang = false;
                        if (is_ting == true) {
                            can_gang = true;
                        } else {
                            // 把可以杠的这张牌去掉。看是不是听牌
                            int bu_index = _logic.switch_to_card_index(card);
                            int save_count = GRR._cards_index[i][bu_index];
                            GRR._cards_index[i][bu_index] = 0;

                            int cbWeaveIndex = GRR._weave_count[i];

                            GRR._weave_items[i][cbWeaveIndex].public_card = 0;
                            GRR._weave_items[i][cbWeaveIndex].center_card = card;
                            GRR._weave_items[i][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
                            GRR._weave_items[i][cbWeaveIndex].provide_player = seat_index;
                            GRR._weave_count[i]++;

                            can_gang = this.is_yytdh_ting_card(GRR._cards_index[i], GRR._weave_items[i],
                                    GRR._weave_count[i], i);

                            // 把牌加回来
                            GRR._cards_index[i][bu_index] = save_count;
                            GRR._weave_count[i] = cbWeaveIndex;

                        }

                        if (can_gang == true) {
                            playerStatus.add_action(GameConstants.WIK_GANG);
                            playerStatus.add_gang(card, seat_index, 1);// 加上杠
                            bAroseAction = true;
                        }
                    }
                }
            }
            // 如果是自摸胡
            // 可以胡的情况 判断
            if (_playerStatus[i].is_chi_hu_round()) {
                // 吃胡判断
                ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
                chr.set_empty();
                int cbWeaveCount = GRR._weave_count[i];
                action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
                        GameConstants.HU_CARD_TYPE_PAOHU, i, false);

                // 结果判断
                if (action != 0) {
                    _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
                    _playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
                    bAroseAction = true;
                }
            }
        }

        if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_QING_YI_SE_CHI)) {
            int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();
            // 推倒胡清一色可吃
            boolean can_chi = true;
            if(chi_color[chi_seat_index] > GameConstants.INVALID_SEAT){
            	if(_logic.get_card_color(card) != chi_color[chi_seat_index]){
            		can_chi = false;
            	}
            }
            if(can_chi){
            	if ( checkChi(card, GRR._cards_index[chi_seat_index],chi_seat_index) ) {
            		if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
            			// 这里可能有问题 应该是 |=
            			action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
            			if ((action & GameConstants.WIK_LEFT) != 0) {
            				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
            				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
            			}
            			if ((action & GameConstants.WIK_CENTER) != 0) {
            				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
            				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
            			}
            			if ((action & GameConstants.WIK_RIGHT) != 0) {
            				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
            				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
            			}
            			
            			// 结果判断
            			if (_playerStatus[chi_seat_index].has_action()) {
            				bAroseAction = true;
            			}
            		}
            	}
            }
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

    @Override
    protected void onInitTable() {
        _handler_dispath_card = new MJHandlerDispatchCard_YYTDH();
        _handler_out_card_operate = new MJHandlerOutCardOperate_YYTDH();
        _handler_gang = new MJHandlerGang_YYTDH();
        _handler_chi_peng = new MJHandlerChiPeng_YYTDH();

        // 岳陽推到胡handler
        _handler_gang_yytdh = new MJHandlerGang_YYTDH_DispatchCard();
        _handler_hai_di_yytdh = new MJHandlerHaiDi_YYTDH();
        _handler_yao_hai_di_yytdh = new MJHandlerYaoHaiDi_YYTDH();
    }

    @Override
    protected boolean on_game_start() {
    	chi_color = new int[getTablePlayerNumber()];
    	Arrays.fill(chi_color, -1);
    	
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
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

            // 只发自己的牌
            gameStartResponse.clearCardData();
            for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
                gameStartResponse.addCardData(hand_cards[i][j]);

                cards.addItem(hand_cards[i][j]);
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

            roomResponse.setGameStart(gameStartResponse);
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
        
        // 检测听牌
        for (int i = 0; i < this.getTablePlayerNumber(); i++) {
            _playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
                    GRR._weave_items[i], GRR._weave_count[i], true,i);
            if (_playerStatus[i]._hu_card_count > 0) {
                this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
            }
        }
        
        this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

        return true;
    }

    public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
            ChiHuRight chiHuRight, int card_type, int _seat_index, boolean dis_patch){
    	
    	// Table里面的get_ting_card方法
    	if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
    		return analyse_chi_hu_card_new(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index, dis_patch);
    	} else {
    		return analyse_chi_hu_card_old(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index, dis_patch);
    	}
    	
    }
    
    public int analyse_chi_hu_card_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
            ChiHuRight chiHuRight, int card_type, int _seat_index, boolean dis_patch) {
        // 变量定义
        // cbCurrentCard一定不为0 !!!!!!!!!
        if (cur_card == 0)
            return GameConstants.WIK_NULL;

        int[] luo_hu_card = this._playerStatus[_seat_index].get_cards_abandoned_hu();
        for (int i = 0; i < luo_hu_card.length; i++) {
            if (luo_hu_card[i] == cur_card) {
                return GameConstants.WIK_NULL;
            }
        }

        // 设置变量
        List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
        // chiHuRight.set_empty();可以重复

        // 构造扑克
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        // 插入扑克
        cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

        boolean hu = false;// 是否胡的标记--//七小对 将将胡 可能不是能胡牌的牌型 优先判断
        long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
        if (qxd != GameConstants.WIK_NULL) {
            chiHuRight.opr_or(qxd);
            hu = true;
        }

        // 将将胡只能自摸
        if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_JIANGJIANGHU) && dis_patch) {
            // 将将胡
            if (_logic.is_jiangjiang_hu(cards_index, weaveItems, weave_count, cur_card)) {
                chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
                hu = true;
            }
        }

        int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();

        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }

        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }
        
        // 分析扑克--通用的判断胡牌方法
        boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
                magic_cards_index, magic_card_count);

        // 胡牌分析
        if (bValue == false) {
            if (hu == false) {
                return GameConstants.WIK_NULL;
            }
        }

        // 全求人
        if (_logic.is_dan_diao(cards_index, cur_card) && GRR.hasAnGang(_seat_index) == 0) {// weaveCount == 4&&
            chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
            hu = true;
        }

        // 清一色牌
        if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
            chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
            hu = true;
        }

        
        boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
                _logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

        if (is_peng_hu) {
            boolean exist_eat = exist_eat(weaveItems, weave_count);
            if (!exist_eat) {
            	chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
                hu = true;
            }
        }
        // 碰碰和
       /* if (AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
                magic_cards_index, magic_card_count)) {
            chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
            hu = true;
        }
*/		
        
        boolean flag = true;
        if(!isChi(_seat_index, cur_card)){
        	flag = false;
        }
        if(!flag){
        	 return GameConstants.WIK_NULL;
        }
        if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_QING_YI_SE_CHI)){
	        if(chi_color[_seat_index] > GameConstants.INVALID_SEAT){
	        	if((chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE).is_empty())){
	        		return GameConstants.WIK_NULL;
	        	}
	        }
        }
        if (hu == true) {
            if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
                chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
            } else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
                return GameConstants.WIK_CHI_HU;
            } else {
                chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
            }
            // 有大胡
            return GameConstants.WIK_CHI_HU;
        } else if (bValue) {
            if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
                chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
                return GameConstants.WIK_CHI_HU;
            } else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
                return GameConstants.WIK_CHI_HU;
            } else if (GRR._left_card_count == 0) {
                return GameConstants.WIK_CHI_HU;
            } else if(card_type == GameConstants.HU_CARD_TYPE_GANG_PAO){
            	return GameConstants.WIK_CHI_HU;
            } else {
                return GameConstants.WIK_NULL;
            }
        }

        return GameConstants.WIK_NULL;
    }
    
    
    public int analyse_chi_hu_card_old(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
            ChiHuRight chiHuRight, int card_type, int _seat_index, boolean dis_patch) {
        // 变量定义
        // cbCurrentCard一定不为0 !!!!!!!!!
        if (cur_card == 0)
            return GameConstants.WIK_NULL;

        int[] luo_hu_card = this._playerStatus[_seat_index].get_cards_abandoned_hu();
        for (int i = 0; i < luo_hu_card.length; i++) {
            if (luo_hu_card[i] == cur_card) {
                return GameConstants.WIK_NULL;
            }
        }

        // 设置变量
        List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

        // 构造扑克
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        // 插入扑克
        cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

        boolean hu = false;// 是否胡的标记--//七小对 将将胡 可能不是能胡牌的牌型 优先判断
        long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
        if (qxd != GameConstants.WIK_NULL) {
            chiHuRight.opr_or(qxd);
            hu = true;
        }

        // 将将胡只能自摸
        if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_JIANGJIANGHU) && dis_patch) {
            // 将将胡
            if (_logic.is_jiangjiang_hu(cards_index, weaveItems, weave_count, cur_card)) {
                chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
                hu = true;
            }
        }

        // 分析扑克--通用的判断胡牌方法
        boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);

        // 胡牌分析
        if (bValue == false) {
            // 不能胡的情况,有可能是七小对
            // 七小对牌 豪华七小对
            if (hu == false) {
                // chiHuRight.set_empty();
                return GameConstants.WIK_NULL;
            }
        }

        // 全求人
        if (_logic.is_dan_diao(cards_index, cur_card) && GRR.hasAnGang(_seat_index) == 0) {// weaveCount == 4&&
            chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
            hu = true;
        }

        // 清一色牌
        if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
            chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
            hu = true;
        }

        // 牌型分析
        for (int i = 0; i < analyseItemArray.size(); i++) {
            // 变量定义
            AnalyseItem analyseItem = analyseItemArray.get(i);
            // 碰碰和
            if (_logic.is_pengpeng_hu(analyseItem)) {
                chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
                hu = true;
                break;
            }

        }

        boolean flag = true;
        if(!isChi(_seat_index, cur_card)){
        	flag = false;
        }
        if(!flag){
        	 return GameConstants.WIK_NULL;
        }
        if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_QING_YI_SE_CHI)){
	        if(chi_color[_seat_index] > GameConstants.INVALID_SEAT){
	        	if((chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE).is_empty())){
	        		return GameConstants.WIK_NULL;
	        	}
	        }
        }
        
        if (hu == true) {
            if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
                chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
            } else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
                return GameConstants.WIK_CHI_HU;
            } else {
                chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
            }
            // 有大胡
            return GameConstants.WIK_CHI_HU;
        } else if (bValue) {
            if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
                chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
                return GameConstants.WIK_CHI_HU;
            } else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
                return GameConstants.WIK_CHI_HU;
            } else if (GRR._left_card_count == 0) {
                return GameConstants.WIK_CHI_HU;
            } else {
                return GameConstants.WIK_NULL;
            }
        }

        return GameConstants.WIK_NULL;
    }
    
    /**
     * 检测是否吃了牌
     * @param seat_index
     * @param card
     * @return
     */
    public boolean isChi(int seat_index, int card ){
    	 if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_QING_YI_SE_CHI)){
    		 if(chi_color[seat_index] > GameConstants.INVALID_SEAT){
    			 if(_logic.get_card_color( card) != chi_color[seat_index]){
    				 return false;
    			 }
    		 }
    	 }
    	 return true;
    }
    
    public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
        for (int i = 0; i < cbWeaveCount; i++) {
            if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
                    || weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
                return true;
        }

        return false;
    }

    public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo,
            int jie_pao_count) {
        GRR._win_order[seat_index] = 1;
        // 引用权位
        ChiHuRight chr = GRR._chi_hu_rights[seat_index];

        int di_feng = GameConstants.CELL_SCORE;
        int wFanShu = 0;// 番数
        if (!zimo && !(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
            // 特殊处理 海底炮 杠上炮的番数 算开杠玩家分数
            ChiHuRight chr_t = this.is_yytdh_ting_card_sroce(GRR._cards_index[provide_index],
                    GRR._weave_items[provide_index], GRR._weave_count[provide_index], provide_index);
            if (chr_t.da_hu_count > 0) {
                di_feng = 5;
                wFanShu = chr.da_hu_count;
            } else {
                di_feng = 5;
                wFanShu = 0;
            }

        } else {
            if (chr.da_hu_count > 0) {
                di_feng = 5;
                wFanShu = chr.da_hu_count;
            }
        }

        int lChiHuScore = 0;// wFanShu*m_pGameServiceOption->lCellScore;
        if (wFanShu > 0) {
            lChiHuScore = (int) (5 * Math.pow(2, (wFanShu - 1)));
        } else {
            lChiHuScore = di_feng;
        }

        // 记录牌型日志
        countCardType(chr, seat_index);

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

        /////////////////////////////////////////////// 算分//////////////////////////
        GRR._count_pick_niao = 0;
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            for (int j = 0; j < GRR._player_niao_count[i]; j++) {
                if (zimo) {
                    GRR._count_pick_niao++;
                    GRR._player_niao_cards[i][j] = super.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效

                } else {
                    /*
                     * if(is_tong_bao){ if(provide_index == i){ GRR._count_pick_niao++;
                     * GRR._player_niao_cards[i][j] =
                     * this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效 }
                     * }else{
                     */
                    if (seat_index == i || provide_index == i) {// 自己还有放炮的人有效
                        GRR._count_pick_niao++;
                        GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
                    } else {
                        GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
                    }
                    // }

                }

            }
        }

        ////////////////////////////////////////////////////// 自摸 算分
        if (zimo) {
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                if (i == seat_index) {
                    continue;
                }

                int s = lChiHuScore;

                GRR._player_niao_invalid[i] = 1;// 庄家鸟生效

                int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
                if (niao > 0 && GRR._count_pick_niao > 0) {
                    s *= Math.pow(2, niao);
                }
                // 胡牌分
                GRR._game_score[i] -= s;
                GRR._game_score[seat_index] += s;
            }
        }
        ////////////////////////////////////////////////////// 点炮 算分
        else {
            int s = lChiHuScore;
            int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
            if (niao > 0 && GRR._count_pick_niao > 0) {
                s *= Math.pow(2, niao);
            }

            GRR._game_score[provide_index] -= s;
            GRR._game_score[seat_index] += s;

            GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

        }

        GRR._provider[seat_index] = provide_index;
        // 设置变量

        _status_gang = false;
        _status_gang_hou_pao = false;

        _playerStatus[seat_index].clean_status();

        return;

    }

    @Override
    protected void set_result_describe() {
        int l;
        long type = 0;
        // 有可能是通炮
        boolean has_da_hu = false;
        // 大胡
        boolean dahu[] = { false, false, false, false };
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
                dahu[i] = true;
                has_da_hu = true;
            }
        }
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            ChiHuRight chr = GRR._chi_hu_rights[i];
            String des = "";

            l = chr.type_count;
            for (int j = 0; j < l; j++) {
                type = chr.type_list[j];
                if (chr.is_valid()) {
                    if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
                        des += " 碰碰胡";
                    }
                    if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
                        des += " 将将胡";
                    }
                    if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
                        des += " 清一色";
                    }
                    if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
                        des += " 海底捞";
                    }
                    if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
                        des += " 海底炮";
                    }
                    if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
                        des += " 七小对";
                    }
                    if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
                        des += " 豪华七小对";
                    }
                    if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
                        des += " 双豪华七小对";
                    }
                    if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
                        des += " 杠上开花";
                    }
                    if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
                        des += " 双杠上开花";
                    }
                    if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
                        des += " 抢杠胡";
                    }
                    if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
                        des += " 杠上炮";
                    }

                    if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO) {
                        des += " 双杠上炮";
                    }

                    if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {
                        des += " 全求人";
                    }

                    if (type == GameConstants.CHR_ZI_MO) {
                        if (dahu[i] == true) {
                            des += " 大胡自摸";
                        } else {
                            des += " 小胡自摸";
                        }
                    }
                    if (type == GameConstants.CHR_SHU_FAN) {
                        if (dahu[i] == true) {
                            des += " 大胡接炮";
                        } else {
                            des += " 小胡接炮";
                        }
                    }
                    if (type == GameConstants.CHR_FANG_PAO) {
                        des += " 放炮";
                    }
                    if (type == GameConstants.CHR_TONG_PAO) {
                        des += " 通炮";
                    }
                } else {
                    if (type == GameConstants.CHR_FANG_PAO) {
                        if (has_da_hu == true) {
                            des += " 大胡放炮";
                        } else {
                            des += " 小胡放炮";
                        }
                    }
                }

            }

            int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
            if (GRR != null) {
                for (int p = 0; p < getTablePlayerNumber(); p++) {
                    for (int w = 0; w < GRR._weave_count[p]; w++) {
                        if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG
                                && GRR._weave_items[p][w].weave_kind != GameConstants.WIK_BU_ZHNAG) {
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

            if (GRR._player_niao_count[i] > 0) {
                des += " 鸟X" + GRR._player_niao_count[i] + "(乘法)";
            }
            GRR._result_des[i] = des;
        }

    }

    @Override
    public int getTablePlayerNumber() {
        if (has_rule(GameConstants.GAME_RULE_HUNAN_THREE)) {
            return GameConstants.GAME_PLAYER - 1;
        }
        return GameConstants.GAME_PLAYER;
    }

    /**
     * 第一局随机庄
     */
    @Override
    protected void initBanker() {
        if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
            int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER - 1);
            this._cur_banker = banker;
        } else {
            int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER);
            this._cur_banker = banker;
        }
    }

    protected boolean on_handler_game_finish(int seat_index, int reason) {
        int real_reason = reason;

        for (int i = 0; i < getTablePlayerNumber(); i++) {
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
        game_end.setGamePlayerNumber(getTablePlayerNumber());
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            game_end.addPao(_player_result.pao[i]);
        }
        
        setGameEndBasicPrama(game_end);

        if (GRR != null) {
            // reason == MJGameConstants.Game_End_NORMAL || reason
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
            // 重新算小胡分数
            /*
             * if (this.is_mj_type(GameConstants.GAME_TYPE_CS)) {
             * 
             * for (int p = 0; p < getTablePlayerNumber(); p++) { if
             * (GRR._start_hu_right[p].is_valid()) { int lStartHuScore = 0;
             * 
             * int wFanShu = _logic.get_chi_hu_action_rank_yytdh(GRR._start_hu_right[p]);
             * 
             * lStartHuScore = wFanShu * GameConstants.CELL_SCORE;
             * 
             * for (int i = 0; i < getTablePlayerNumber(); i++) { if (i == p) continue; int
             * s = lStartHuScore; // 庄闲 if (is_zhuang_xian()) { if ((GRR._banker_player ==
             * p) || (GRR._banker_player == i)) { s += s; } } int niao =
             * GRR._player_niao_count[p] + GRR._player_niao_count[i]; if (niao > 0) { s *=
             * Math.pow(2, niao); } GRR._start_hu_score[i] -= s;// 输的番薯
             * GRR._start_hu_score[p] += s; } } }
             * 
             * }
             */
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
                GRR._game_score[i] += lGangScore[i];
                GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

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

            for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
                game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
            }
            game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

            // 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
            for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
                Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
                // 定鸟
                for (int j = 0; j < GRR._player_niao_count[i]; j++) {
                    int card = GRR._player_niao_cards[i][j];
                    if (card < GameConstants.DING_NIAO_INVALID) {
                        card += GameConstants.DING_NIAO_INVALID;
                    }
                    pnc.addItem(card);
                }

                game_end.addPlayerNiaoCards(pnc);
            }

            for (int i = 0; i < getTablePlayerNumber(); i++) {
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
                    if (j < GRR._weave_count[i]) {
                        weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
                    } else {
                        weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
                    }

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
            if (_cur_round >= _game_round) {// 局数到了
                end = true;
                game_end.setRoomOverType(1);
                game_end.setPlayerResult(this.process_player_result(reason));
            } else {
                // 确定下局庄家
                // 以后谁胡牌，下局谁做庄。
                // 流局,庄家下一个
                // 通炮,放炮的玩家

            }
        } else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
                || reason == GameConstants.Game_End_RELEASE_RESULT
                || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
            end = true;
            real_reason = GameConstants.Game_End_DRAW;
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
            for (int j = 0; j < getTablePlayerNumber(); j++) {
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

        // 错误断言
        return false;
    }

    public boolean operate_shai_zi_effect(int num1, int num2, int anim_time, int delay_time, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
        roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
        roomResponse.setTarget(seat_index);
        roomResponse.setEffectCount(2);
        roomResponse.addEffectsIndex(num1);
        roomResponse.addEffectsIndex(num2);
        roomResponse.setEffectTime(anim_time);// anim time//摇骰子动画的时间
        roomResponse.setStandTime(delay_time); // delay time//停留时间
        this.send_response_to_room(roomResponse);
        GRR.add_room_response(roomResponse);
        return true;
    }

    // 计算中骰子的玩家
    public int get_target_shai_zi_player(int num1, int num2) {
        return get_zhong_seat_by_value_yytdh(num1 + num2, GRR._banker_player);// GameConstants.INVALID_SEAT;
    }

    @Override
    public void test_cards() {
        int[] cards_of_player0 = new int[] { 0x01,0x01,0x02,0x03,0x4,0x05,0x05,0x05,0x11,0x12,0x13,0x21,0x22 };
        int[] cards_of_player1 = new int[] {0x01,0x01,0x02,0x03,0x4,0x05,0x05,0x05,0x11,0x12,0x13,0x21,0x22};
        int[] cards_of_player3 = new int[] { 0x01,0x01,0x02,0x03,0x4,0x05,0x05,0x05,0x11,0x12,0x13,0x21,0x22 };
        int[] cards_of_player2 = new int[] { 0x01,0x01,0x02,0x03,0x4,0x05,0x05,0x05,0x11,0x12,0x13,0x21,0x22};

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

    @Override
    public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
        // WalkerGeek Auto-generated method stub
        return false;
    }

    @Override
    public boolean handler_requst_nao_zhuang(Player player, int nao) {
        // WalkerGeek Auto-generated method stub
        return false;
    }

    @Override
    public boolean handler_requst_message_deal(Player player,int seat_index, RoomRequest room_rq, int type) {
        return true;
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
        // WalkerGeek Auto-generated method stub
        return 0;
    }

    @Override
    public boolean trustee_timer(int operate_id, int seat_index) {
        // TODO Auto-generated method stub
        return false;
    }

    
    
    public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dis_patch,int seat_index) {
	    if (SysParamServerUtil.is_new_algorithm(SysParamServerUtil.SYS_PARAM_SERVER_3000, SysParamServerUtil.SYS_PARAM_SERVER_3000, 2)) {
	        return get_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, dis_patch,seat_index);
	    } else {
	        return 0;
	    }
	}
	
	public int get_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
	        boolean dis_patch,int seat_index) {
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
	    /*if (dai_feng) {
	        l += GameConstants.CARD_FENG_COUNT;
	        ql += (GameConstants.CARD_FENG_COUNT - 1);
	    }*/
	    for (int i = 0; i < l; i++) {
	        if (this._logic.is_magic_index(i))
	            continue;
	        cbCurrentCard = _logic.switch_to_card_data(i);
	        chr.set_empty();
	        if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount,
	                cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO,seat_index,dis_patch)) {
	            cards[count] = cbCurrentCard;
	            count++;
	        }
	    }
	
	    l -= 1;
	    
	    if (count == 0) {
			
		} else if (count > 0 && count < ql) {
		
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}
	
	    if (timer.get() > 50) {
	        logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
	    }
	
	    return count;
	}
    
    /*
     * (non-Javadoc)
     * 
     * @see com.cai.game.mj.AbstractMJTable#process_chi_hu_player_score(int, int,
     * int, boolean)
     */
    @Override
    public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
        // WalkerGeek Auto-generated method stub

    }

}
