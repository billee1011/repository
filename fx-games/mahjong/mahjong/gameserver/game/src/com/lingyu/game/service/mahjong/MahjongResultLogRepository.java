package com.lingyu.game.service.mahjong;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;
import com.lingyu.common.entity.MahjongResultLog;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

@Repository
public class MahjongResultLogRepository extends UniqueCacheRepository<MahjongResultLog, Long> {

	@Override
	public void cacheUpdate(MahjongResultLog entity) {
		entity.setModifyTime(new Date());
		super.cacheUpdate(entity);
	}
	
	/**
	 * 获取对应的信息
	 * @param ids  对应的id
	 * @param start 开始时间
	 * @param end 结束时间
	 * @param startNum 开始条数的索引位
	 * @param endNum 查多少个
	 * @return
	 */
	public List<MahjongResultLog> getAllResultLog(List<Long> ids, Date start, Date end, int startNum, int endNum){
		if(CollectionUtils.isNotEmpty(ids)){
			StringBuffer sb = new StringBuffer();
			sb.append("select * from mahjong_result_log where id in(");
			for(int i = 0; i< ids.size(); i++){
				sb.append(ids.get(i));
				sb.append(",");
			}
			String sql = sb.substring(0, sb.length() -1);
			StringBuffer sb1 = new StringBuffer();
			sb1.append(sql);
			sb1.append(") and add_time >=? and add_time <? limit ?, ?");
			return queryForList(sb1.toString(), start, end, startNum, endNum);
		}
		return null;
	}
}