package com.lingyu.common.message;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.SystemConstant;

/**
 * @description 客户端接入消息分发器
 * @author hehj 2011-11-4 下午3:04:30
 */
public class PublicMsgDispatcher {

    private ThreadLocal<ExecutorRunnablePool> runnableLocal = new ThreadLocal<ExecutorRunnablePool>();

    private BalanceBusinessExecutor businessExexutor;

    private MessageMediator defaultManager;

    public PublicMsgDispatcher(BalanceBusinessExecutor businessExexutor, MessageMediator defaultManager) {
        this.businessExexutor = businessExexutor;
        this.defaultManager = defaultManager;
    }

    public void invoke(int command, long roleId, String moduleName, JSONObject message) {
        IRunnable runnable = this.getRunnablePool().getRunnable(command, roleId, message);
        businessExexutor.execute(runnable, SystemConstant.GROUP_PUBLIC, moduleName);
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
