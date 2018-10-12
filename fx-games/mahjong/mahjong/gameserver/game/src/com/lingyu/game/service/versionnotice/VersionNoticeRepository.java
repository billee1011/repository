package com.lingyu.game.service.versionnotice;

import java.util.Date;

import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.VersionNotice;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

/**
 * 版本公告
 * @author wangning
 * @date 2017年3月3日 下午9:40:38
 */
@Repository
public class VersionNoticeRepository extends UniqueCacheRepository<VersionNotice,Integer>{
	
	@Override
	public void cacheUpdate(VersionNotice entity) {
		entity.setModifyTime(new Date());
		super.cacheUpdate(entity);
	}
	
}
