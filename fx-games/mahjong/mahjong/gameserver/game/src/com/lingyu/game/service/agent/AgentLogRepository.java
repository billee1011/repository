package com.lingyu.game.service.agent;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.AgentLog;
import com.lingyu.game.service.mahjong.MahjongConstant;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

@Repository
public class AgentLogRepository extends UniqueCacheRepository<AgentLog, Long> {

	public List<AgentLog> getList4RoleIdAndToRoleId(final long roleId, final long toRoleId) {
		StringBuffer sb1 = new StringBuffer();
		sb1.append("select * from agent_log where role_id =? and to_role_id =? limit ?, ?");
		return queryForList(sb1.toString(), roleId, toRoleId, 0, MahjongConstant.ZHANJI_RESULT_PAGE_SIZE);
	}

	/**
	 * 获取对应的信息
	 * 
	 * @param ids
	 *            对应的id
	 * @param start
	 *            开始时间
	 * @param end
	 *            结束时间
	 * @param startNum
	 *            开始条数的索引位
	 * @param endNum
	 *            查多少个
	 * @return
	 */
	public List<AgentLog> getAllResultLog(Long roleId, Date start, Date end, int startNum, int endNum) {
		StringBuffer sb1 = new StringBuffer();
		sb1.append("select * from agent_log where role_id =? and add_time >=? and add_time <=? limit ?, ?");
		return queryForList(sb1.toString(), roleId, start, end, startNum, endNum);
	}
}