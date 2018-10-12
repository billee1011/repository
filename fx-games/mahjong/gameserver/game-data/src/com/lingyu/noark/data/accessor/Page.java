package com.lingyu.noark.data.accessor;

import java.util.List;

public class Page<T> {
	private List<T> result;
	// 总记录数.
	private int totalSize;
	// 总页数
	private int totalPage;
	// 当前第几页.
	private int page;
	// 每页多少条.
	private int size;

	public final List<T> getResult() {
		return result;
	}

	public final void setResult(List<T> result) {
		this.result = result;
	}

	public final int getTotalSize() {
		return totalSize;
	}

	public final void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}

	public final int getTotalPage() {
		return totalPage;
	}

	public final void setTotalPage(int totalPage) {
		this.totalPage = totalPage;
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

	@Override
	public String toString() {
		return "Page [result=" + result + ", totalSize=" + totalSize + ", totalPage=" + totalPage + ", page=" + page + ", size=" + size + "]";
	}
}