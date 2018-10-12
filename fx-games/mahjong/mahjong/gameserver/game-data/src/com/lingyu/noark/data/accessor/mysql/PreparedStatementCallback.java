package com.lingyu.noark.data.accessor.mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementCallback<T> {
	T doInPreparedStatement(PreparedStatement pstmt) throws SQLException, IllegalArgumentException, IllegalAccessException;
}
