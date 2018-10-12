package com.lingyu.common.message;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.util.SpringContainer;
import com.lingyu.game.RouteManager;

public class MessageMediator {
    private static final Logger logger = LogManager.getLogger(MessageMediator.class);
    private Map<Integer, MessageResolver> resolvers = new HashMap<Integer, MessageResolver>();

    public MessageMediator(String groupName) {
        init(groupName);
    }

    public void init(String groupName) {
        logger.info("消息分发系统初始化开始 group={}", groupName);
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        Set<BeanDefinition> candidates = provider.findCandidateComponents(groupName);
        for (BeanDefinition candidate : candidates) {
            try {
                String clazzName = candidate.getBeanClassName();
                Class<?> clazz = Class.forName(clazzName);
                Object instance = SpringContainer.getBean(clazz);
                GameAction action = clazz.getAnnotation(GameAction.class);
                if (null != action) {
                    MethodAccess access = MethodAccess.get(instance.getClass());
                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method method : methods) {
                        GameMapping mapping = method.getAnnotation(GameMapping.class);
                        if (null != mapping) {
                            int methodIndex = access.getIndex(method.getName(), method.getParameterTypes());
                            resolvers.put(mapping.value(), new MessageResolver(method, instance, access, methodIndex));
                            RouteManager.register(new GameCommand(action.group(), action.module(), mapping.value(),
                                            mapping.relay(), mapping.print()));
                        }
                    }
                }

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ServiceException(e);
            }
        }
        logger.info("消息分发系统初始化完毕 group={}", groupName);
    }

    public MessageResolver getResolver(int command) {
        MessageResolver resolver = resolvers.get(command);
        return resolver;
    }

    public void execute(int command, long roleId, JSONObject message) {
        MessageResolver resolver = this.getResolver(command);
        try {
            resolver.execute(roleId, message);

        } catch (Exception e) {
            logger.warn("--->异常情况:command={},roleId={} message={}", command, roleId, message);
            logger.warn(e.getMessage(), e);
        }

    }

    public void execute(int command, String userId, JSONObject message) {
        MessageResolver resolver = this.getResolver(command);
        resolver.execute(userId, message);

    }

}
