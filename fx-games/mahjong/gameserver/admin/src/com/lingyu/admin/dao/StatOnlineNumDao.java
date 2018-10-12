package com.lingyu.admin.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lingyu.common.entity.StatOnlineNum;
import com.lingyu.common.orm.SimpleHibernateTemplate;

@Service
public class StatOnlineNumDao {
	private SimpleHibernateTemplate<StatOnlineNum, Integer> template;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new SimpleHibernateTemplate<StatOnlineNum, Integer>(sessionFactory, StatOnlineNum.class);
	}

	public void saveOnlineNum(StatOnlineNum stat) {
		template.save(stat);
	}

	public void saveOnlineNum(String pid, int areaId, int num, Date date) {
		StatOnlineNum stat = new StatOnlineNum();
		stat.setPid(pid);
		stat.setAreaId(areaId);
		stat.setNum(num);
		stat.setAddTime(date);
		// stat.setAddTime(DateUtils.round(date, Calendar.MINUTE));
		template.save(stat);
	}

	public int getConcurrentNum(int areaId) {
		int ret = 0;
		int id = jdbcTemplate.queryForObject("select ifnull(max(id),0)from stat_online_num where area_id=?", new Object[] { areaId }, Integer.class);
		if (id == 0) {
			return ret;
		}
		StatOnlineNum stat = template.get(id);
		return ret = stat.getNum();
		// return jdbcTemplate
		// .queryForObject(
		// "select ifnull(num,0) from stat_online_num where area_id=? order by id desc limit 1",
		// new Object[] { areaId }, Integer.class);

	}

	@Cacheable(value = "statOnlineNum", key = "#areaId+'area'+'start'+#startTime.time+'end'+#endTime.time")
	public List<StatOnlineNum> getOnlineNumList4Area(int areaId,String pid, Date startTime, Date endTime) {
		String sql = "from StatOnlineNum where areaId=?0 and pid=?1 and addTime>=?2 and addTime<?3 order by id";
		List<StatOnlineNum> list = template.findT(sql, areaId, pid, startTime, endTime);
		return list;
	}
	
	public List<StatOnlineNum> getConcurrentNumListTimerArea(int areaId,String pid,Date startTime, Date endTime){
//		List<OnlineDataVo> list = jdbcTemplate.queryForList("select add_time, num from stat_online_num where area_id = ? and pid = ? and add_time > ? and add_time <= ?",
//				new Object[]{areaId,pid,startTime,endTime},OnlineDataVo.class);
		List<StatOnlineNum> list = template.findT("from StatOnlineNum where areaId=?0 and pid=?1 and addTime>=?2 and addTime<?3", areaId, pid, startTime, endTime);
		return list;
	}
	
	public int getAvgNum(int areaId,String pid, Date startTime, Date endTime){
		return jdbcTemplate.queryForObject("select IFNULL(AVG(num),0) from stat_online_num where pid = ? and area_id = ? and num>0  and add_time BETWEEN ? and ?",
				new Object[]{pid,areaId,startTime,endTime}, Integer.class);
	}
	
	public int getMaxNum(int areaId,String pid, Date startTime, Date endTime){
		return jdbcTemplate.queryForObject("select IFNULL(MAX(num),0) from stat_online_num where pid = ? and area_id = ? and add_time BETWEEN ? and ?", 
				new Object[]{pid,areaId,startTime,endTime}, Integer.class);
	}

	
	
	/** 选取某天的在线人数 */
	@Cacheable(value = "statOnlineNum", key = "#areaId+#day")
	public List<StatOnlineNum> getOnlineNumList(int areaId, String pid, Date day) {
		List<StatOnlineNum> list = template.findT("from StatOnlineNum where areaId=? and pid=? and datediff(?,addTime)=0", areaId, pid, day);
		return list;
	}

//	public int getPCU4Day(int areaId) {
//		return jdbcTemplate.queryForObject("select ifnull(max(num),0) from stat_online_num where area_id=? and datediff(now(),add_time)=1",
//				new Object[] { areaId }, Integer.class);
//	}
//
//	public int getACU4Day(int areaId) {
//		return jdbcTemplate.queryForObject("select ifnull(avg(num),0) from stat_online_num where area_id=? and datediff(now(),add_time)=1",
//				new Object[] { areaId }, Integer.class);
//	}

//	/** 获取某个月的最高同时在线 */
//	public int getPCU(int areaId) {
//		return jdbcTemplate
//				.queryForObject(
//						"select ifnull(max(num),0) from stat_online_num where area_id=? and month(add_time) = month(date_sub(now(), INTERVAL 1 MONTH)) AND year(add_time) = year(now())",
//						new Object[] { areaId }, Integer.class);
//	}

//	/** 获取某个月的平均同时在线 */
//	public int getACU(int areaId) {
//		return jdbcTemplate
//				.queryForObject(
//						"select ifnull(avg(num),0) from stat_online_num where area_id=? and num>0 and month(add_time) = month(date_sub(now(), INTERVAL 1 MONTH)) AND year(add_time) = year(now())",
//						new Object[] { areaId }, Integer.class);
//	}

	public List<StatOnlineNum> getConcurrentNum(int areaId, Date startTime, Date endTime) {
		return template.findT("from StatOnlineNum where areaId=?0 and (addTime between ?1 and ?2)", areaId, startTime, endTime);
	}
}