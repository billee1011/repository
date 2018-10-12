package com.cai.future.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.game.mj.MJTable;

public class TestRunnable implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(TestRunnable.class);

//	private MJTable _table;
//	public TestRunnable(MJTable table, int type){
//		
//		_table = table;
//	}
	@Override
	public void run() {
		
		logger.info("this is future task................");
	}
	
	

}
