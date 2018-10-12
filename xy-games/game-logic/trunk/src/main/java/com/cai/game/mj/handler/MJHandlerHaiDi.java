package com.cai.game.mj.handler;

import com.cai.common.constant.GameConstants;
import com.cai.game.mj.AbstractMJTable;

public abstract class MJHandlerHaiDi<T extends AbstractMJTable> extends AbstractMJHandler<T> {

    protected int _start_index;
    protected int _seat_index;

    public void reset_status(int start_index, int seat_index) {
        _start_index = start_index;
        _seat_index = seat_index;
    }

    public MJHandlerHaiDi() {

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
    public boolean handler_operate_card(T table, int seat_index, int operate_code, int operate_card) {
        if (seat_index != _seat_index) {
            // logger.error("[海底],操作失败,"+seat_index+"不是当前操作玩家");
            return false;
        }

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            // 不要海底
            _seat_index = (_seat_index + 1) % GameConstants.GAME_PLAYER;
            if (_seat_index == _start_index) {
                table._cur_banker = _start_index;

                // 流局
                table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

                return true;
            }

            table.exe_hai_di(_start_index, _seat_index);
        } else {
            table.exe_yao_hai_di(_seat_index);
        }

        return true;
    }
}
