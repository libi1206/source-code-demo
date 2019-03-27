package com.libi.data;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author libi
 */

public interface LibiDataSource {
    /**获取连接*/
    public Connection getConnection() throws SQLException, ClassNotFoundException, InterruptedException;

    /**释放连接*/
    public void releaseConnection(Connection connection) throws SQLException;
}
