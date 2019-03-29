package com.libi.mybatis.mapper;

import com.libi.mybatis.annotation.ExtInsert;
import com.libi.mybatis.annotation.ExtParam;
import com.libi.mybatis.annotation.ExtSelect;
import com.libi.mybatis.entity.User;

/**
 * @author libi
 */
public interface UserMapper {
    @ExtInsert("insert into user(username,password) values (#{userName},#{password})")
    int insertUser(@ExtParam("userName") String userName, @ExtParam("password") String password);

    @ExtSelect("select * from user where username=#{userName} and password=#{password}")
    User selectUser(@ExtParam("userName") String userName, @ExtParam("password") String password);
}
