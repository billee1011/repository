package com.lingyu.admin.dao;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.Announce;
@Repository
public class AnnounceDao extends GeneralDao<Announce, Integer>{

	@Override
	public List<Announce> queryAll() {
		List<Announce> ret = super.queryAll();
		if(CollectionUtils.isNotEmpty(ret)){
			for(Announce announce : ret){
				announce.derialize();
			}
		}
		return ret;
	}
}
