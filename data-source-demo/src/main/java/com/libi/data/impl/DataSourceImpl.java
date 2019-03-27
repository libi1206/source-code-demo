package com.libi.data.impl;

import com.libi.data.LibiDataSource;
import com.libi.data.properties.DbProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author libi
 */
public class DataSourceImpl implements LibiDataSource {
    /**空闲线程的容器*/
    private List<Connection> freeConnection = new Vector<Connection>();
    /**活动线程的容器*/
    private List<Connection> activeConnection = new Vector<Connection>();
    /**参数*/
    private DbProperties properties;
    /**原子类计数*/
    private AtomicInteger count;

    public DataSourceImpl(DbProperties properties) throws SQLException, ClassNotFoundException {
        this.properties = properties;
        count = new AtomicInteger(0);
        init();
    }

    /**初始化空闲连接*/
    private void init() throws SQLException, ClassNotFoundException {
        //获取初始化连接数，创建连接，存在空闲连接池里
        for (int i = 0; i < properties.getInitConnections(); i++) {
            Connection connection = newConnection();
            if (connection != null) {
                freeConnection.add(connection);
            }
        }
    }

    /**
     * 创建新的连接
     */
    private Connection newConnection() throws ClassNotFoundException, SQLException {
        Class.forName(properties.getDriverName());
        Connection connection = DriverManager.getConnection(properties.getUrl(), properties.getUserName(), properties.getPassword());
        count.addAndGet(1);
        return connection;

    }


    @Override
    public Connection getConnection() throws SQLException, ClassNotFoundException, InterruptedException {
        Connection connection = null;
        if (count.get() < properties.getMaxActiveConnections()) {
            //小于最大连接数，判断空闲连接是否有数据,存在活动连接里
            if (freeConnection.size() > 0) {
                //有空闲数据,从空闲池里取出连接
                connection = freeConnection.remove(0);
            } else {
                //创建新的连接
                connection = newConnection();
            }
            //判断连接是否可用
            if (isAvailable(connection)) {
                activeConnection.add(connection);
            } else {
                count.addAndGet(-1);
                connection = getConnection();
            }

        } else {
            //大于最大活动连接数，经行等待
            Thread.sleep(properties.getConnTimeOut());
            connection = getConnection();
        }
        return connection;
    }

    @Override
    public void releaseConnection(Connection connection) throws SQLException {
        //判断连接是否可用
        if (isAvailable(connection)) {
            //判断空闲连接是否大于最大连接
            if (freeConnection.size() < properties.getMaxConnections()) {
                //空闲线程未满
                freeConnection.add(connection);
            } else {
                //空闲线程已满
                connection.close();
            }
            activeConnection.remove(connection);
            count.addAndGet(-1);
        }

    }


    /**判断连接是否可用*/
    private boolean isAvailable(Connection connection) throws SQLException {
        if (connection == null || connection.isClosed()) {
            return false;
        }
        return true;
    }

}
