package com.lingyu.game.service.currency;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.CurrencyConstant.CurrencyCostType;
import com.lingyu.common.constant.CurrencyConstant.CurrencyType;
import com.lingyu.common.constant.OperateConstant.OperateType;

public interface IMoneyOperation {

    /**
     * 货币增加逻辑
     * 
     * @param roleId
     * @param moneyType
     *            这个是给道具那块用的 正常货币不必理会
     * @param ammount
     *            增加货币数量
     * @param notify
     *            是否通知客户端
     * @param needAntiAddiction
     *            是否需要防沉迷
     * @param reason
     *            货币编号途径
     * @return
     */
    public MoneyResponse incr(long roleId, long ammount, boolean notify, boolean needAntiAddiction, OperateType reason);

    /**
     * 货币扣除逻辑
     * 
     * @param roleId
     * @param moneyType
     *            这个是给道具那块用的 正常货币不必理会
     * @param ammount
     * @param notify
     * @return
     */
    public MoneyResponse decr(long roleId, long ammount, boolean notify, OperateType reason, CurrencyCostType costType);

    /**
     * 检查货币是否足够
     * 
     * @param roleId
     * @param moneyType
     *            这个是给道具那块用的 正常货币不必理会
     * @param ammount
     * @return
     */
    public boolean checkDecr(long roleId, long ammount, CurrencyCostType costType);

    /**
     * 货币不足时给出的错误码
     * 
     * @param moneyType
     * @return
     */
    public JSONObject getMoneyNotEnoughErrorCode();

    /**
     * 货币类型
     * 
     * @return
     */
    public CurrencyType getMoneyType();

}
