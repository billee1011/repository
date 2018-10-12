package com.lingyu.game.service.announce;

import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.Announce;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

/**
 * 公告Repository
 * @author Wang Shuguang
 */
@Repository
public class AnnounceRepository extends UniqueCacheRepository<Announce,Long>{
	
}
