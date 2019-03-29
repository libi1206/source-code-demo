package com.libi.mybatis.proxy;

import com.libi.mybatis.annotation.ExtInsert;
import com.libi.mybatis.annotation.ExtParam;
import com.libi.mybatis.annotation.ExtSelect;
import com.libi.mybatis.utils.JDBCUtils;
import com.libi.mybatis.utils.SQLUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author libi
 * 用于动态代理，获取方法的参数并且给返回值
 */
public class InvocationHandlerMybatis implements InvocationHandler {
    /**
     * @param proxy     代理对象
     * @param method    拦截的方法
     * @param args      方法上的参数
     * @return          方法的返回值
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("开始代理");
        //判断方法上是否存在Insert注解
        ExtInsert extInsert = method.getDeclaredAnnotation(ExtInsert.class);
        if (extInsert != null) {
            //执行插入的操作，返回影响行数
            return doInsert(method, args, extInsert);
        }
        //判断方法上是否有Select注解
        ExtSelect extSelect = method.getDeclaredAnnotation(ExtSelect.class);
        if (extSelect != null) {
            //执行查询的操作，返回实际实体类或者List
            return doSelect(method, args, extSelect);
        }
        return null;
    }

    /**
     * 执行插入的操作
     * @param method
     * @param args
     * @param extInsert
     * @return 影响行数
     */
    private int doInsert(Method method, Object[] args, ExtInsert extInsert) {
        //获取Sql语句
        String sql = extInsert.value();
        System.out.println("insert sql:" + sql);
        //获取方法参数和Sql语句进行匹配
        ConcurrentHashMap<String, Object> paramMap = getParamMap(method, args);

        //怕Sql参数顺序和@Param参数顺序不一致而把sql语句的参数放在一个有序的数组里
        List<Object> sqlParamValue = new ArrayList<>();
        String[] sqlInsertParameter = SQLUtils.sqlInsertParameter(sql);
        for (String paramName : sqlInsertParameter) {
            Object paramValue = paramMap.get(paramName);
            sqlParamValue.add(paramValue);
        }
        //把参数替换成？
        sql = SQLUtils.parameQuestion(sql, sqlInsertParameter);
        System.out.println("new sql:"+sql);
        //执行JDBC
        return JDBCUtils.insert(sql, false, sqlParamValue);
    }

    /**
     * 执行查询的操作
     * @param method
     * @param args
     * @param extSelect
     * @return 查询结果，可能是实体类对象，List或者基础类型
     */
    private Object doSelect(Method method, Object[] args, ExtSelect extSelect) throws SQLException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        //获取Sql语句
        String sql = extSelect.value();
        System.out.println("select sql:" + sql);
        //获取方法参数和Sql语句进行匹配
        ConcurrentHashMap<String, Object> paramMap = getParamMap(method, args);

        //怕Sql参数顺序和@Param参数顺序不一致而把sql语句的参数放在一个有序的数组里
        List<Object> sqlParamValue = new ArrayList<>();
        List<String> sqlSelectParameter = SQLUtils.sqlSelectParameter(sql);
        for (String paramName : sqlSelectParameter) {
            Object paramValue = paramMap.get(paramName);
            sqlParamValue.add(paramValue);
        }
        //把参数替换成？
        sql = SQLUtils.parameQuestion(sql, sqlSelectParameter);
        System.out.println("new sql:"+sql);
        //执行JDBC
        ResultSet resultSet = JDBCUtils.query(sql, sqlParamValue);
        //判断是否有结果集
        if (!resultSet.next()) {
            return null;
        }
        resultSet.previous();
        //使用反射获取方法类型
        Class<?> returnType = method.getReturnType();
        //使用反射机制实例化对象
        Object result = returnType.newInstance();
        //遍历这个结果集
        while (resultSet.next()) {
            for (String paramName : sqlSelectParameter) {
                //获取参数的值
                Object resultValue = resultSet.getObject(paramName);
                //使用反射机制赋值
                Field field = returnType.getDeclaredField(paramName);
                field.setAccessible(true);
                field.set(result, resultValue);
            }
        }
        return result;
    }

    /**
     * 建立方法上的参数和值@Param参数名的映射
     * @param method
     * @param args
     * @return
     */
    private ConcurrentHashMap<String, Object> getParamMap(Method method, Object[] args) {
        //定义一个Map，Key是参数名，Value是参数值
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        //获取方法上的参数
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            //获取参数名称和参数的值
            ExtParam param = parameters[i].getDeclaredAnnotation(ExtParam.class);
            if (param != null) {
                String name = param.value();
                Object value = args[i];
                System.out.println("paramName:"+name+",paramValue:"+value);
                map.put(name, value);
            }
        }
        return map;
    }
}
