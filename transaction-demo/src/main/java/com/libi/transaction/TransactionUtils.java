package com.libi.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * @author libi
 * 自己写事务开始、回滚和提交的方法（编程式事务）
 * 使用的时候不能使用默认的单例模式，而是需要使用原型模式
 */
@Component
@Scope("prototype")
public class TransactionUtils {
    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    private TransactionStatus status;

    /** 开启事务*/
    public TransactionStatus begin() {
        //使用默认的传播级别
        TransactionStatus transaction = dataSourceTransactionManager.getTransaction(new DefaultTransactionAttribute());
        return transaction;
    }

    /** 提交事务 需要传入这个事务状态*/
    public void commit() {
        dataSourceTransactionManager.commit(status);
    }

    /**回滚事务 需要传入这个事务状态*/
    public void rollBack() {
        //获取当前事务，如果有，就回滚
        if (status != null) {
            dataSourceTransactionManager.rollback(status);
        }
    }
}
