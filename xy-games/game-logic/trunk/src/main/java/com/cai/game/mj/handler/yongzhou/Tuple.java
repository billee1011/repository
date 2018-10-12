package com.cai.game.mj.handler.yongzhou;

public class Tuple<K, V> {
	private K left;
	private V right;

	public Tuple(K left, V right) {
		this.left = left;
		this.right = right;
	}

	public Tuple() {
	}

	public void setLeft(K left) {
		this.left = left;
	}

	public K getLeft() {
		return left;
	}

	public void setRight(V right) {
		this.right = right;
	}

	public V getRight() {
		return right;
	}

	public String toString() {
		return "left = " + left + " right = " + right;
	}
}
