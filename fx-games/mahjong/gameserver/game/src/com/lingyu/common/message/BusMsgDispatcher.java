package com.lingyu.common.message;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

/**
 * @description 客户端接入消息分发器
 * @author Allen Jiang 2014-02-10 上午8:30:00
 */
public class BusMsgDispatcher {
    private ThreadLocal<ExecutorRunnablePool> runnableLocal = new ThreadLocal<ExecutorRunnablePool>();
    private BalanceBusinessExecutor businessExexutor;
    private MessageMediator defaultManager;

    public BusMsgDispatcher(BalanceBusinessExecutor businessExexutor, MessageMediator defaultManager) {
        this.businessExexutor = businessExexutor;
        this.defaultManager = defaultManager;
    }

    public void invoke(int command, long roleId, byte group, JSONObject message) {
        IRunnable runnable = this.getRunnablePool().getRunnable(command, roleId, message);
        businessExexutor.execute(runnable, group, String.valueOf(roleId));
    }

    public void invoke(int command, String userId, byte group, JSONObject message) {
        IRunnable runnable = this.getRunnablePool().getRunnable(command, userId, message);
        businessExexutor.execute(runnable, group, userId);
    }

    private ExecutorRunnablePool getRunnablePool() {
        ExecutorRunnablePool ret = runnableLocal.get();
        if (null == ret) {
            ret = new ExecutorRunnablePool(defaultManager);
            runnableLocal.set(ret);
        }
        return ret;
    }
}
