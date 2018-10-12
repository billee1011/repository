package com.lingyu.noark.data.accessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.accessor.mysql.SqlExpert;
import com.lingyu.noark.data.accessor.mysql.SqlGener;

public class AccessorEntityMapping<T> extends EntityMapping<T> implements SqlGener {
	private static final Logger logger = LogManager.getLogger(AccessorEntityMapping.class);
	private String insterSql = null;
	private String updateSql = null;
	private String deleteSql = null;
	private String seleteSql = null;
	private String seleteAllSql = null;
	private String seleteByRoleIdSql = null;

	private String seleteByCount = null;
	private String seleteByRoleIdAndCount = null;
	private String seleteByGroupSql = null;

	private String seleteByPageSql = null;
	private String seleteByAndSortPageSql = null;

	private String seleteByRoleIdAndPageSql = null;
	private String seleteByRoleIdAndSortAndPageSql = null;

	public AccessorEntityMapping(Class<T> klass) {
		super(klass);
	}

	@Override
	public String getInsterSql(SqlExpert expert) {
		if (insterSql == null) {
			insterSql = expert.genInsertSql(this);
		}
		logger.debug(insterSql);
		return insterSql;
	}

	@Override
	public String getUpdateSql(SqlExpert expert) {
		if (updateSql == null) {
			updateSql = expert.genUpdateSql(this);
		}
		logger.debug(updateSql);
		return updateSql;
	}

	@Override
	public String getDeleteSql(SqlExpert expert) {
		if (deleteSql == null) {
			deleteSql = expert.genDeleteSql(this);
		}
		logger.debug(deleteSql);
		return deleteSql;
	}

	@Override
	public String getSeleteSql(SqlExpert expert) {
		if (seleteSql == null) {
			seleteSql = expert.genSeleteSql(this);
		}
		logger.debug(seleteSql);
		return seleteSql;
	}

	@Override
	public String getSeleteAllSql(SqlExpert expert) {
		if (seleteAllSql == null) {
			seleteAllSql = expert.genSeleteAllSql(this);
		}
		logger.debug(seleteAllSql);
		return seleteAllSql;
	}

	@Override
	public String getSeleteByRoleId(SqlExpert expert) {
		if (seleteByRoleIdSql == null) {
			seleteByRoleIdSql = expert.genSeleteByRoleId(this);
		}
		logger.debug(seleteByRoleIdSql);
		return seleteByRoleIdSql;
	}

	@Override
	public String getSeleteByGroupBy(SqlExpert expert) {
		if (seleteByGroupSql == null) {
			seleteByGroupSql = expert.genSeleteByGroup(this);
		}
		logger.debug(seleteByGroupSql);
		return seleteByGroupSql;
	}

	@Override
	public String getSeleteByRoleIdAndPage(SqlExpert expert, Sort sort) {
		if (sort == null) {
			if (seleteByRoleIdAndPageSql == null) {
				seleteByRoleIdAndPageSql = expert.genSeleteByRoleIdAndPage(this);
			}
			logger.debug(seleteByRoleIdAndPageSql);
			return seleteByRoleIdAndPageSql;
		} else {
			if (seleteByRoleIdAndSortAndPageSql == null) {
				seleteByRoleIdAndSortAndPageSql = expert.genSeleteByRoleIdAndSortAndPageSql(this, sort);
			}
			logger.debug(seleteByRoleIdAndSortAndPageSql);
			return seleteByRoleIdAndSortAndPageSql;
		}
	}

	@Override
	public String getSeleteByCount(SqlExpert expert) {
		if (seleteByCount == null) {
			seleteByCount = expert.genSeleteByCount(this);
		}
		logger.debug(seleteByCount);
		return seleteByCount;
	}

	@Override
	public String getSeleteByRoleIdAndCount(SqlExpert expert) {
		if (seleteByRoleIdAndCount == null) {
			seleteByRoleIdAndCount = expert.genSeleteByRoleIdAndCount(this);
		}
		logger.debug(seleteByRoleIdAndCount);
		return seleteByRoleIdAndCount;
	}

	@Override
	public String getSeleteByPage(SqlExpert expert, Sort sort) {
		if (sort == null) {
			if (seleteByPageSql == null) {
				seleteByPageSql = expert.genSeleteByPageSql(this);
			}
			logger.debug(seleteByPageSql);
			return seleteByPageSql;
		} else {
			if (seleteByAndSortPageSql == null) {
				seleteByAndSortPageSql = expert.genSeleteByAndSortPageSql(this, sort);
			}
			logger.debug(seleteByAndSortPageSql);
			return seleteByAndSortPageSql;
		}
	}
}