package com.lingyu.game.service.currency;

import java.io.Serializable;
import java.util.List;
import org.springframework.stereotype.Repository;
import com.lingyu.common.entity.MoneyFlowLog;
import com.lingyu.noark.data.repository.MultiCacheRepository;

@Repository
public class MoneyFlowLogRepository extends MultiCacheRepository<MoneyFlowLog, Long> {

	
	@Override
	public List<MoneyFlowLog> cacheLoadAll(Serializable roleId) {
		return super.cacheLoadAll(roleId);
	}
}
