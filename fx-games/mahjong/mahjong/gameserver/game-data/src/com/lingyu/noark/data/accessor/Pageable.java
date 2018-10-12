package com.lingyu.noark.data.accessor;

public class Pageable {
	private int page = 1;
	private int size = 10;
	private Sort sort;

	public Pageable() {

	}

	public Pageable(int page, int size) {
		this.page = page;
		this.size = size;
	}

	public Pageable(int page, int size, Sort sort) {
		this.page = page;
		this.size = size;
		this.sort = sort;
	}

	public final int getPage() {
		return page;
	}

	public final void setPage(int page) {
		this.page = page;
	}

	public final int getSize() {
		return size;
	}

	public final void setSize(int size) {
		this.size = size;
	}

	public final Sort getSort() {
		return sort;
	}

	public final void setSort(Sort sort) {
		this.sort = sort;
	}
}