package com.lingyu.common.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @description 只读map
 * @author ShiJie Chi
 * @created 2010-4-9 下午03:43:50
 */
public class ReadyOnlyMap<K, V> implements Map<K, V>, ConcurrentMap<K, V>, Serializable {
	private static final long serialVersionUID = 299600537501969372L;
	private Map<K, V> map = new HashMap<K, V>();

	public ReadyOnlyMap(Map<K, V> map) {
		this.map = map;
	}

	public void clear() {
		// map.clear();
		throw new UnsupportedOperationException("read only map!");
	}

	public boolean containsKey(Object property) {
		return map.containsKey(property);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	public V get(Object property) {
		return map.get(property);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public V put(K property, V value) {
		throw new UnsupportedOperationException("read only map!");
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException("read only map!");
	}

	public V remove(Object property) {
		throw new UnsupportedOperationException("read only map!");
	}

	public int size() {
		return map.size();
	}

	public Collection<V> values() {
		return map.values();
	}

	@Override
	public V putIfAbsent(K key, V value) {
		throw new UnsupportedOperationException("read only map!");
	}

	@Override
	public boolean remove(Object key, Object value) {
		throw new UnsupportedOperationException("read only map!");
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		throw new UnsupportedOperationException("read only map!");
	}

	@Override
	public V replace(K key, V value) {
		throw new UnsupportedOperationException("read only map!");
	}

}
