package com.lingyu.noark.data.accessor.mysql;

import java.sql.SQLException;
import java.sql.Statement;

public interface StatementCallback<T> {
	T doInStatement(Statement stmt) throws SQLException;
}
