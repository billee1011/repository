package com.lingyu.common.message;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.esotericsoftware.reflectasm.MethodAccess;

public class MessageResolver {
    private static final Logger logger = LogManager.getLogger(MessageResolver.class);

    private Method method;
    private Object target;
    private final MethodAccess access;
    private final int methodIndex;// 方法索引

    public MessageResolver(Method method, Object target, MethodAccess access, int methodIndex) {
        this.method = method;
        this.target = target;
        this.access = access;
        this.methodIndex = methodIndex;
    }

    public void execute(long roleId, JSONObject message) {
        try {
            access.invoke(target, methodIndex, roleId, message);
            // method.invoke(target,roleId, message);
        } catch (Exception e) {
            logger.error("method={},roleId={},args={}", method.getName(), roleId, message);
            logger.error(e.getMessage(), e);
        }

    }

    public void execute(String userId, JSONObject message) {
        try {
            access.invoke(target, methodIndex, userId, message);
            // method.invoke(target,userId, message);
        } catch (Exception e) {
            logger.error("method={},userId={},args={}", method.getName(), userId, message);
            logger.error(e.getMessage(), e);
        }

    }
}
