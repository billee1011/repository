package com.cai.dao;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.DbOpType;
import com.cai.common.define.DbStoreType;
import com.cai.common.domain.DBUpdateDto;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;

public final class DbInvoker implements Runnable {
	private Logger logger = LoggerFactory.getLogger(DbInvoker.class);

	private DBUpdateDto dbUpdateDto;

	public DbInvoker(DBUpdateDto dbUpdateDto) {
		this.dbUpdateDto = dbUpdateDto;
	}

	@SuppressWarnings("rawtypes")
	public void run() {

		if (dbUpdateDto.getDbStoreType() == DbStoreType.PUBLIC) {
			
			PerformanceTimer timer = new PerformanceTimer();
			
			if(dbUpdateDto.getDbOpType() == DbOpType.INSERT){
				SpringService.getBean(ClubDaoService.class).insertObject(dbUpdateDto.getSqlStr(), dbUpdateDto.getObject());
			}else if(dbUpdateDto.getDbOpType() == DbOpType.DELETE){
				SpringService.getBean(ClubDaoService.class).deleteObject(dbUpdateDto.getSqlStr(), dbUpdateDto.getObject());
			}else if(dbUpdateDto.getDbOpType() == DbOpType.UPDATE){
				SpringService.getBean(ClubDaoService.class).updateObject(dbUpdateDto.getSqlStr(), dbUpdateDto.getObject());
			}else if(dbUpdateDto.getDbOpType() == DbOpType.BATCH_INSERT){
				SpringService.getBean(ClubDaoService.class).batchInsert(dbUpdateDto.getSqlStr(), (List)dbUpdateDto.getObject());
			}else if(dbUpdateDto.getDbOpType() == DbOpType.BATCH_DELETE){
				SpringService.getBean(ClubDaoService.class).batchDelete(dbUpdateDto.getSqlStr(), (List)dbUpdateDto.getObject());
			}else if(dbUpdateDto.getDbOpType() == DbOpType.BATCH_UPDATE){
				SpringService.getBean(ClubDaoService.class).batchUpdate(dbUpdateDto.getSqlStr(), (List)dbUpdateDto.getObject());
			}
			
			if(timer.get()>5000L){
				int count = 1;
				if(dbUpdateDto.getObject()!=null && dbUpdateDto.getObject() instanceof List){
					List _list = (List)dbUpdateDto.getObject();
					count = _list.size();
				}
				String msg = "入库时间过长:数量:"+count+",sql:"+dbUpdateDto.getSqlStr();
				
				logger.error(msg);
			}
			
			if(!StringUtils.isEmpty(dbUpdateDto.getLogMsg())){
				logger.error(dbUpdateDto.getLogMsg()+" 导入成功");
			}
			
		}

		else if (dbUpdateDto.getDbStoreType() == DbStoreType.PUBLIC_DATA) {
			//基础库一般不操作
		}

		else if (dbUpdateDto.getDbStoreType() == DbStoreType.GAME) {
			//目前还没有
		}

	}

	public DBUpdateDto getDbUpdateDto() {
		return dbUpdateDto;
	}

	public void setDbUpdateDto(DBUpdateDto dbUpdateDto) {
		this.dbUpdateDto = dbUpdateDto;
	}

}
