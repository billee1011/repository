package com.lingyu.noark.data.accessor.redis;

import java.util.Map;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ShardedJedisPool;

/**
 * 一个实现Redis基本操作的接口.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public interface IRediser {
	public Long del(String key);

	/**
	 * 设置key所对应的value值.
	 * <p>
	 * 时间复杂度为o(1)<br>
	 * 如果Key已存在了，它会被覆盖，而不管它是什么类型。<br>
	 * 这里干掉了返回值，因为SET不会失败，总返回OK
	 * 
	 * @param key KEY值
	 * @param value KEY对应的Value
	 */
	public void set(String key, String value);

	/**
	 * 获取key所对应的value值.
	 * <p>
	 * 时间复杂度为o(1)<br>
	 * 如果key不存在，返回null.<br>
	 * 如果key的value值不是String类型，就返回错误，因为Get只处理String类型的Values.
	 * 
	 * @param key Key值
	 * @return 如果Key存在，则返回对应的Value值.
	 */
	public String get(String key);

	/**
	 * 添加指定成员到Key对应的Set集合中.
	 * <p>
	 * 时间复杂度o(log(N)) N为Set集合中的元素个数<br>
	 * 每一个成员都有一个分数值，如果指定成员存在，那么其分数就会被更新成最新的。<br>
	 * 如果不存在，则会创建一个新的。
	 * 
	 * @param key KEY值
	 * @param score 分数值
	 * @param member 指定成员
	 * @return 返回添加到Set集合中的元素个数，不包括那种已存在只更新的分数的元素。
	 */
	public long zadd(String key, double score, String member);

	/**
	 * 删除指定Key对应的Set集合中指定的成员。
	 * <p>
	 * 
	 * @param key KEY值
	 * @param members 指定的成员
	 * @return 返回被删除的元素的个数
	 */
	public long zrem(String key, String... member);

	/**
	 * 增加指定元素的分数
	 * 
	 * @return 指定元素新的分数值
	 */
	public double zincrby(String key, double increment, String member);

	/**
	 * 获取key指定的Hash集中所有的字段和值。
	 * <p>
	 * 时间复杂度o(N),其中N是Hash集的大小。<br>
	 * 使用命令行时请注意：<br>
	 * 返回值中，每个字段名的下一个是它的值，所以返回值的长度是Hash集大小的两倍.
	 * 
	 * @param key Key值
	 * @return 如果key所对应的Hash集存在，则返回此集合，否则返回空列表.
	 */
	public Map<String, String> hgetAll(String key);

	/**
	 * Hash操作，设置Key指定的Hash集中指定字段的值.
	 */
	public Long hset(String key, String field, String value);

	/**
	 * Hash操作，设置Key指定的Hash集中指定字段的值.
	 * <p>
	 * 时间复杂度为o(N)，其中N是被设置的字段数量.<br>
	 * 该命令将重写所有在Hash集中存在字段，如果key指定的Hash集不存在，会创建一个新的Hash集并与key关联
	 * 
	 * @param key Key键
	 * @param hash Hash集
	 * @return 状态码
	 */
	public String hmset(String key, Map<String, String> hash);

	public ShardedJedisPool getShardedPool();
	
	public JedisPool getPool();
}