
package com.lingyu.game.service.currency;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.constant.CurrencyConstant;
import com.lingyu.common.constant.CurrencyConstant.CurrencyCostType;
import com.lingyu.common.constant.CurrencyConstant.CurrencyType;
import com.lingyu.common.constant.OperateConstant.OperateType;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.entity.MoneyFlowLog;
import com.lingyu.common.entity.Role;
import com.lingyu.common.manager.GameThreadFactory;
import com.lingyu.common.util.MapUtil;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.service.currency.MoneyResponse.MoneyChange;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.id.TableNameConstant;
import com.lingyu.game.service.role.RoleManager;

/**
 * 货币管理
 *
 * @author WSG
 *
 */
@Service
public class MoneyManager {
    @Autowired
    private RoleManager roleManager;
    @Autowired
    private List<IMoneyOperation> moneyOperationList;
    @Autowired
    private IdManager idManager;
    @Autowired
    private MoneyFlowLogRepository moneyFlowLogRepository;

    private Map<Integer, IMoneyOperation> operationMap;

    private ExecutorService pool = Executors.newCachedThreadPool(new GameThreadFactory("async-money-flow"));

    private ServerConfig serverConfig = GameServerContext.getAppConfig();

    public void initialize() {
        operationMap = new HashMap<>();
        for (IMoneyOperation operation : moneyOperationList) {
            CurrencyType currencyType = operation.getMoneyType();
            if (currencyType != null) {
                operationMap.put(operation.getMoneyType().getId(), operation);
            }
        }
    }

    private IMoneyOperation getOperation(CurrencyType moneyType) {
        return operationMap.get(moneyType.getId());
    }

    // ------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------
    /**
     * 增加货币
     *
     * @param roleId
     *            角色id
     * @param moneyEntity
     * @param operateType
     *            操作原因
     * @return
     */
    public MoneyResponse incr(long roleId, MoneyEntity moneyEntity, OperateType operateType) {
        return incr(roleId, moneyEntity.getCurrencyType(), moneyEntity.getAmount(), operateType);
    }

    /**
     * 增加货币
     *
     * @param roleId
     *            角色id
     * @param currencyTypeId
     *            货币的类型id
     * @param amount
     *            增加的数量
     * @param operateType
     *            操作原因
     * @return
     */
    public MoneyResponse incr(long roleId, int currencyTypeId, long amount, OperateType operateType) {
        CurrencyType currencyType = CurrencyType.getInitCurrencyType(currencyTypeId);
        if (currencyType == null) {
            throw new ServiceException(MoneyConstant.UNKNOWN_MONEY_TYPE, currencyTypeId);
        }

        return incr(roleId, currencyType, amount, operateType);
    }

    /**
     * 增加货币
     *
     * @param roleId
     *            角色id
     * @param ammount
     *            增加的数量
     * @param currencyType
     *            货币的类型
     * @param operateType
     *            操作原因
     * @return
     */
    public MoneyResponse incr(long roleId, long ammount, CurrencyType currencyType, OperateType operateType) {
        return incr(roleId, currencyType.getId(), ammount, operateType);
    }

    /**
     * 增加货币
     *
     * @param roleId
     *            角色id
     * @param currencyType
     *            货币的类型
     * @param amount
     *            增加的数量
     * @param operateType
     *            操作原因
     * @return
     */
    public MoneyResponse incr(long roleId, CurrencyType currencyType, long amount, OperateType operateType) {
        if (amount < 0) {
            return new MoneyResponse(false, currencyType, 0, 0, 0);
        }
        if (amount == 0) {
            return new MoneyResponse(true, currencyType, 0, 0, 0);
        }
        IMoneyOperation operation = getOperation(currencyType);
        if (operation == null) {
            throw new ServiceException(MoneyConstant.UNKNOWN_MONEY_TYPE, currencyType.getId());
        }

        boolean needAntiAddiction = GameServerContext.getAppConfig().isAntiAddiction();
        MoneyResponse moneyResponse = incr(roleId, currencyType, amount, true, needAntiAddiction, operateType);

        // @Event
        if (moneyResponse.isSuccess()) {
            for (MoneyChange moneyChange : moneyResponse.getMoneyChangeInfo().values()) {
                createMoneyFlowLog(roleId, currencyType.getId(), CurrencyConstant.INCR_TYPE,
                                moneyChange.getBeforeChangedAmount(), moneyChange.getChangedAmount(),
                                moneyChange.getAfterChangedAmount(), operateType.getId());
            }
        }
        return moneyResponse;
    }

    /**
     * 创建货币流日志
     *
     * @param roleId
     * @param currencyType
     * @param useType
     * @param beforeVal
     * @param val
     * @param afterVal
     * @param operateType
     */
    private void createMoneyFlowLog(long roleId, int currencyType, int useType, long beforeVal, long val, long afterVal,
                    int operateType) {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                MoneyFlowLog log = new MoneyFlowLog();
                Role role = roleManager.getRole(roleId);
                log.setId(idManager.newId(TableNameConstant.MONEY_FOLW_LOG));
                log.setPid(serverConfig.getPlatformId());
                log.setWorldId(serverConfig.getWorldId());
                log.setAreaId(serverConfig.getServerId());
                log.setUserId(role.getUserId());
                log.setRoleId(roleId);
                log.setCurrencyType(currencyType);
                log.setUseType(useType);
                log.setBeforeValue(beforeVal);
                log.setValue(val);
                log.setAfterValue(afterVal);
                log.setOperateType(operateType);
                log.setAddTime(new Date());
                moneyFlowLogRepository.cacheInsert(log);
            }
        });
    }

    /**
     * 增加货币
     *
     * @param roleId
     *            角色id
     * @param amount
     *            增加的数量
     * @param notify
     *            是否通知客户端
     * @param moneyType
     *            货币类型
     * @param needAntiAddiction
     *            是否需要防沉迷
     * @param reason
     *            操作原因
     *
     * @return
     */
    private MoneyResponse incr(long roleId, CurrencyType moneyType, long amount, boolean notify,
                    boolean needAntiAddiction, OperateType reason) {
        IMoneyOperation operation = getOperation(moneyType);
        if (operation == null) {
            throw new ServiceException(MoneyConstant.UNKNOWN_MONEY_TYPE, moneyType);
        }
        synchronized (roleManager.getLock(roleId)) {
            return operation.incr(roleId, amount, notify, needAntiAddiction, reason);
        }
    }

    // ------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------
    /**
     * 同时扣除多种货币<br>
     * 任何一项不够 return false
     *
     * @param roleId
     *            角色id
     * @param moneyEntity
     * @param operateType
     *            操作原因
     * @return true: 货币足够
     */
    public boolean decr(long roleId, List<MoneyEntity> moneyEntityList, OperateType operateType) {
        if (CollectionUtils.isEmpty(moneyEntityList)) {
            return true;
        } else if (moneyEntityList.size() == 1) {
            MoneyResponse response = decr(roleId, moneyEntityList.get(0), operateType);
            return response.isSuccess();
        } else {
            if (!checkDecr(roleId, moneyEntityList)) {
                return false;
            }

            for (MoneyEntity moneyEntity : moneyEntityList) {
                decr(roleId, moneyEntity.getCurrencyType(), moneyEntity.getAmount(), operateType,
                                moneyEntity.getCostType());
            }
            return true;
        }
    }

    /**
     * 扣除货币
     *
     * @param roleId
     *            角色id
     * @param moneyEntity
     * @param operateType
     *            操作原因
     * @return
     */
    public MoneyResponse decr(long roleId, MoneyEntity moneyEntity, OperateType operateType) {
        return decr(roleId, moneyEntity.getCurrencyType(), moneyEntity.getAmount(), operateType,
                        moneyEntity.getCostType());
    }

    /**
     * 扣除货币
     *
     * @param roleId
     *            角色id
     * @param currencyTypeId
     *            货币类型id
     * @param amount
     *            扣除的货币数量
     * @param operateType
     *            操作原因
     *
     * @return
     */
    public MoneyResponse decr(long roleId, int currencyTypeId, long amount, OperateType operateType,
                    CurrencyCostType costType) {
        CurrencyType currencyType = CurrencyType.getInitCurrencyType(currencyTypeId);
        if (currencyType == null) {
            throw new ServiceException(MoneyConstant.UNKNOWN_MONEY_TYPE, currencyTypeId);
        }

        return decr(roleId, currencyType, amount, operateType, costType);
    }

    /**
     * 扣除货币
     *
     * @param roleId
     *            角色id
     * @param currencyType
     *            货币类型
     * @param amount
     *            扣除的货币数量
     * @param operateType
     *            操作原因
     *
     * @return
     */
    public MoneyResponse decr(long roleId, CurrencyType currencyType, long amount, OperateType operateType,
                    CurrencyCostType costType) {
        // 消耗值小于零 中断流程 这个必需有的逻辑
        if (amount < 0) {
            return new MoneyResponse(false, currencyType, amount, 0, 0);
        }
        if (amount == 0) {
            return new MoneyResponse(true, currencyType, 0, 0, 0);
        }

        MoneyResponse moneyResponse = decr(roleId, currencyType, amount, true, operateType, costType);

        // @Event
        if (moneyResponse.isSuccess()) {
            for (MoneyChange moneyChange : moneyResponse.getMoneyChangeInfo().values()) {
                createMoneyFlowLog(roleId, currencyType.getId(), CurrencyConstant.DECR_TYPE,
                                moneyChange.getBeforeChangedAmount(), moneyChange.getChangedAmount(),
                                moneyChange.getAfterChangedAmount(), operateType.getId());
            }
        }

        return moneyResponse;
    }

    /**
     * 扣除货币
     *
     * @param roleId
     *            角色id
     * @param moneyType
     *            货币类型
     * @param amount
     *            扣除的数量
     * @param notify
     *            是否通知客户端
     * @param operateType
     *            操作原因
     * @param costType
     *            货币消耗类型
     *
     * @return
     */
    private MoneyResponse decr(long roleId, CurrencyType currencyType, long amount, boolean notify,
                    OperateType operateType, CurrencyCostType costType) {
        IMoneyOperation operation = getOperation(currencyType);
        if (operation == null) {
            throw new ServiceException(MoneyConstant.UNKNOWN_MONEY_TYPE, currencyType.getId());
        }

        synchronized (roleManager.getLock(roleId)) {
            if (currencyType.getId() < 100) {
                return operation.decr(roleId, amount, notify, operateType, costType);
            } else {
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------
    /**
     * 验证同时扣除多种货币
     *
     * @param roleId
     *            角色id
     * @param moneyEntity
     *            货币变化类
     * @return
     */
    public boolean checkDecr(long roleId, List<MoneyEntity> moneyEntityList) {
        Map<CurrencyCostType, Map<CurrencyType, Long>> checkSpeMoney = new HashMap<>();
        for (MoneyEntity moneyEntity : moneyEntityList) {
            // 货币类型
            CurrencyType currencyType = moneyEntity.getCurrencyType();
            // 消耗货币数量
            long changeNum = moneyEntity.getAmount();
            // 货币消耗类型
            CurrencyCostType costType = moneyEntity.getCostType();

            // 对有对应绑定关系的货币处理
            if (currencyType.getId() == CurrencyType.DIAMOND_NEW.getId()) {
                Map<CurrencyType, Long> sameCostType = checkSpeMoney.get(costType);
                if (sameCostType == null) {
                    sameCostType = new HashMap<>();
                    checkSpeMoney.put(costType, sameCostType);
                }

                if (CurrencyCostType.ONLY.getId() == costType.getId()) {
                    MapUtil.addMapValue(sameCostType, currencyType, changeNum);
                } else {
                    MapUtil.addMapValue(sameCostType, CurrencyType.DIAMOND_NEW, changeNum);
                }
            } else {
                Map<CurrencyType, Long> sameCostType = checkSpeMoney.get(CurrencyCostType.ONLY);
                if (sameCostType == null) {
                    sameCostType = new HashMap<>();
                    checkSpeMoney.put(costType, sameCostType);
                }
                MapUtil.addMapValue(sameCostType, currencyType, changeNum);
            }
        }

        Map<CurrencyType, Long> olnyMap = checkSpeMoney.get(CurrencyCostType.ONLY);
        if (olnyMap != null) {
            for (Map.Entry<CurrencyType, Long> entry : olnyMap.entrySet()) {
                CurrencyType currencyType = entry.getKey();
                // 扣除货币金额
                long decrNum = entry.getValue();

                boolean beEnough = checkDecr(roleId, currencyType, decrNum, CurrencyCostType.ONLY);
                if (!beEnough) {
                    return false;
                }
            }
        }

        Map<CurrencyType, Long> currentFirstMap = checkSpeMoney.get(CurrencyCostType.CURRENT_FIRST);
        if (currentFirstMap != null) {
            // 玩家货币信息
            Role role = roleManager.getRole(roleId);
            long diamond = role.getDiamond();

            Long currentFirstDiamond = currentFirstMap.get(CurrencyType.DIAMOND_NEW);
            if (currentFirstDiamond != null && (diamond < currentFirstDiamond)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 验证扣除单一货币
     *
     * @param roleId
     *            角色id
     * @param moneyEntity
     *            货币变化类
     * @return
     */
    public boolean checkDecr(long roleId, MoneyEntity moneyEntity) {
        return checkDecr(roleId, moneyEntity.getCurrencyType(), moneyEntity.getAmount(), moneyEntity.getCostType());
    }

    /**
     * 验证扣除单一货币
     *
     * @param roleId
     *            角色id
     * @param amount
     *            扣除的数量
     * @param currencyTypeId
     *            货币类型id
     * @param costType
     *            货币消耗类型
     * @return
     */
    public boolean checkDecr(long roleId, int currencyTypeId, long amount, CurrencyCostType costType) {
        CurrencyType currencyType = CurrencyType.getInitCurrencyType(currencyTypeId);
        if (currencyType == null) {
            throw new ServiceException(MoneyConstant.UNKNOWN_MONEY_TYPE, currencyTypeId);
        }

        return checkDecr(roleId, currencyType, amount, costType);
    }

    /**
     * 验证扣除单一货币
     *
     * @param roleId
     *            角色id
     * @param amount
     *            扣除的数量
     * @param currencyType
     *            货币类型
     * @param costType
     *            货币消耗类型
     * @return
     */
    public boolean checkDecr(long roleId, CurrencyType currencyType, long amount, CurrencyCostType costType) {
        // 消耗值小于零 中断流程 这个必需有的逻辑
        if (amount < 0) {
            return false;
        }
        if (amount == 0) {
            return true;
        }

        IMoneyOperation operation = getOperation(currencyType);
        if (operation == null) {
            throw new ServiceException(MoneyConstant.UNKNOWN_MONEY_TYPE, currencyType.getId());
        }

        synchronized (roleManager.getLock(roleId)) {
            if (currencyType.getId() < 100) {
                return operation.checkDecr(roleId, amount, costType);
            } else {
                return false;
            }
        }
    }

    // ---------------------------------------------------------------------
    // ---------------------------------------------------------------------
    /**
     * 货币不足错误码
     *
     * @param currencyTypeId
     *
     * @return
     */
    public JSONObject getMoneyNotEnoughErrorCode(int currencyTypeId) {
        CurrencyType currencyType = CurrencyType.getInitCurrencyType(currencyTypeId);
        JSONObject result = new JSONObject();
        if (currencyType == null) {
            result.put(ErrorCode.RESULT, ErrorCode.FAILED);
            result.put(ErrorCode.CODE, ErrorCode.ACCOUNT_TYPE_ERROR);
            return result;
        }
        return getMoneyNotEnoughErrorCode(currencyType);
    }

    /**
     * 货币不足错误码
     *
     * @param currencyType
     *
     * @return
     */
    public JSONObject getMoneyNotEnoughErrorCode(CurrencyType currencyType) {
        IMoneyOperation moneyOperation = getOperation(currencyType);
        JSONObject result = new JSONObject();
        if (moneyOperation == null) {
            result.put(ErrorCode.RESULT, ErrorCode.FAILED);
            result.put(ErrorCode.CODE, ErrorCode.ACCOUNT_TYPE_ERROR);
            return result;
        }

        if (currencyType.getId() < 100) {
            return moneyOperation.getMoneyNotEnoughErrorCode();
        } else {
            return null;
        }
    }

    // ---------------------------------------------------------------------
}
