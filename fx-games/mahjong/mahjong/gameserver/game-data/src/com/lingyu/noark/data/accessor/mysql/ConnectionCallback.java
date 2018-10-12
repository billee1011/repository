package com.lingyu.noark.data.accessor.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import com.lingyu.noark.data.exception.DataAccessException;

public interface ConnectionCallback<T> {
	T doInConnection(Connection con) throws SQLException, DataAccessException;
}
