package com.lingyu.common.message;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.game.GameServerService;

/**
 * @description 请求可执行任务池，缓存{@link Runnable}对象供下次使用
 * @author hehj
 * @author Allen Jiang
 * @created 2010-2-8 下午04:46:29
 * @param <T>
 */
public class ExecutorRunnablePool {
    private static final Logger logger = LogManager.getLogger(GameServerService.class);

    private MessageMediator defaultManager;

    public ExecutorRunnablePool(MessageMediator defaultManager) {
        this.defaultManager = defaultManager;
    }

    public IRunnable getRunnable(int command, long roleId, JSONObject message) {
        RunnableImpl runnable = new RunnableImpl();
        runnable.init(command, roleId, message);
        return runnable;

    }

    public IRunnable getRunnable(int command, String userId, JSONObject message) {
        RunnableImpl runnable = new RunnableImpl();
        runnable.init(command, userId, message);
        return runnable;

    }

    /**
     * @description 可执行对象
     *              <p>
     *              将请求提交给{@link IMesaageExecutor}处理，并将处理结果输出
     * @author hehj
     * @author Allen Jiang
     * @created 2009-9-5 下午01:59:28
     */
    public class RunnableImpl implements IRunnable {

        private JSONObject message;
        private long roleId;
        private String userId;
        private int command;

        public RunnableImpl() {
        }

        private void init(int command, long roleId, JSONObject message) {
            this.message = message;
            this.roleId = roleId;
            this.command = command;
        }

        private void init(int command, String userId, JSONObject message) {
            this.message = message;
            this.userId = userId;
            this.command = command;
        }

        @Override
        public int getCommand() {
            return command;
        }

        @Override
        public long getRoleId() {
            return roleId;
        }

        @Override
        public void run() {
            try {
                if (StringUtils.isEmpty(userId)) {
                    defaultManager.execute(command, roleId, message);
                } else {
                    defaultManager.execute(command, userId, message);
                }
            } catch (Exception e) {
                logger.error(message, e);
            }

        }

    }

}
