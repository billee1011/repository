package com.cai.game.hh.util;

public class Triple<First, Second, Third> {
	private First first;
	private Second second;
	private Third third;

	private Triple(First first, Second second, Third third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public static <T1, T2, T3> Triple<T1, T2, T3> of(T1 first, T2 second, T3 third) {
		return new Triple<>(first, second, third);
	}

	public First getFirst() {
		return first;
	}

	public Second getSecond() {
		return second;
	}

	public Third getThird() {
		return third;
	}

	@Override
	public String toString() {
		return "Triple [" + "HuXi:" + first.toString() + ", WeaveKind:" + second.toString() + ", MagicToRealCard:" + third.toString() + "] ";
	}
}
