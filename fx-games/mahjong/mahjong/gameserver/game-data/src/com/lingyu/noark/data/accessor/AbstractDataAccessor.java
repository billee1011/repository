package com.lingyu.noark.data.accessor;

import java.util.List;
import java.util.Map;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.accessor.mysql.RowMapper;
import com.lingyu.noark.data.exception.UnrealizedException;

public abstract class AbstractDataAccessor implements DataAccessor {

	@Override
	public <T> void checkupEntityFieldsWithDatabase(EntityMapping<T> em) {
	}

	@Override
	public <T> T queryForObject(EntityMapping<T> em, String sql, Object... args) {
		throw new UnrealizedException("未实现的queryForObject");
	}

	@Override
	public <T> List<T> queryForList(EntityMapping<T> em, String sql, Object... args) {
		throw new UnrealizedException("未实现的queryForList");
	}

	@Override
	public <T> int queryForInt(EntityMapping<T> em, String sql, Object... args) {
		throw new UnrealizedException("未实现的queryForInt");
	}

	@Override
	public <T> long queryForLong(EntityMapping<T> em, String sql, Object... args) {
		throw new UnrealizedException("未实现的queryForLong");
	}

	@Override
	public <T> Map<String, Object> queryForMap(EntityMapping<T> em, String sql, Object... args) {
		throw new UnrealizedException("未实现的queryForMap");
	}

	@Override
	public <E> List<E> queryForList(EntityMapping<E> em, String sql, RowMapper<E> mapper, Object... args) {
		throw new UnrealizedException("未实现的queryForList");
	}

	@Override
	public <E> int execute(String sql, Object... args) {
		throw new UnrealizedException("未实现的execute");
	}
}