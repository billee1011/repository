package com.cai.game.util.phz;

public class SetManager {
	private static final SetManager manager = new SetManager();
	
	private SetWrapper setNormal;
	
	private SetManager() {
		setNormal  = new SetWrapper();
	}
	
	public static SetManager getInstance() {
		return manager;
	}
	
	protected boolean contains(String key) {
		return setNormal.contains(key);
	}
	
	protected void add(String key) {
		setNormal.add(key);
	}
	
	protected void dumpNormal() {
		String path1 = "tbl_phz/table.tbl";
		setNormal.dump(path1);
	}
	
	public void load() {
		String path1 = "tbl_phz/table.tbl";
		setNormal.load(path1);
	}
}
