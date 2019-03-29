package com.libi.mybatis;

import com.libi.mybatis.entity.User;
import com.libi.mybatis.mapper.UserMapper;
import com.libi.mybatis.sql.SqlSession;

/**
 * @author libi
 */
public class Cluster {
    public static void main(String[] args) {
        UserMapper userMapper = SqlSession.getMapper(UserMapper.class);
        User user = userMapper.selectUser("name", "123");
        System.out.println(user.getUserName());
    }
}
