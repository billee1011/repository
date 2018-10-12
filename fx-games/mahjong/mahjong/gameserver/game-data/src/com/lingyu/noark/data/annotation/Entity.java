package com.lingyu.noark.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lingyu.noark.data.exception.NoEntityException;
import com.lingyu.noark.data.repository.AbstractCacheRepository;

/**
 * Entity注解是用来标注一个Java类为实体类.
 * <p>
 * 当一个Java类没有Entity注解时，就认为他不是一个实体对象. <br>
 * 当实体类没有此注解时会抛出 NoEntityException异常.
 * 
 * @see NoEntityException
 * @author 小流氓<176543888@qq.com>
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {

	/**
	 * 返回当前实体类的抓取策略.
	 * 
	 * @return 返回配置的抓取策略，默认值为什么用，什么时候初始化.
	 */
	FeatchType fetch() default FeatchType.USE;

	/**
	 * 延迟插入，只对 {@link AbstractCacheRepository#cacheInsert(Object)}生效
	 * <p>
	 * 如果实体类上标识为false,就算使用cacheInsert也是先插入数据库再添加到缓存中.
	 * 
	 * @return 返回是否需要延迟插入，默认值是延迟
	 */
	boolean delayInsert() default true;

	/**
	 * 抓取策略.
	 * <p>
	 * 1.启动服务器的时候，初始化当前实体数据.<br>
	 * 2.登录游戏的时候，初始化当前实体数据.<br>
	 * 3.什么时候用，什么时候初始化当前实体数据.<br>
	 */
	public enum FeatchType {
		/**
		 * 启动服务器的时候，初始化当前实体数据.
		 */
		START,
		/**
		 * 和服前,启动服务器的时候，初始化当前实体数据.
		 */
		LOGIN,
		/**
		 * 什么时候用，什么时候初始化当前实体数据.
		 */
		USE;
		
	}
}
