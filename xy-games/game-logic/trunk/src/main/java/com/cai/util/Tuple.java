package com.cai.util;

public class Tuple<K, V> {
	private K left;
	private V right;

	public Tuple() {
	}

	public Tuple(K left, V rigth) {
		this.left = left;
		this.right = rigth;
	}

	public K getLeft() {
		return left;
	}

	public void setLeft(K left) {
		this.left = left;
	}

	public V getRight() {
		return right;
	}

	public void setRight(V right) {
		this.right = right;
	}
}
