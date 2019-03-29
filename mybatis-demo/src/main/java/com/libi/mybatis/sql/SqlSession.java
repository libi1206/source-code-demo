package com.libi.mybatis.sql;

import com.libi.mybatis.proxy.InvocationHandlerMybatis;

import java.lang.reflect.Proxy;

/**
 * @author libi
 */
public class SqlSession {
    /**加载Mapper接口*/
    public static <T> T getMapper(Class clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandlerMybatis());
    }
}
