package com.libi.aop;

import com.libi.transaction.TransactionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * @author libi
 * 使用AOP封装
 */
@Component
@Aspect
public class AopTransaction {
    @Autowired
    private TransactionUtils transactionUtils;

//    @Around("execution(* com.libi.service.UserService.add(..))")
    public void around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        System.out.println("开启事务");
        TransactionStatus status = transactionUtils.begin();
        proceedingJoinPoint.proceed();
        System.out.println("提交事务");
        transactionUtils.commit();
    }

//    @AfterThrowing("execution(* com.libi.service.UserService.add(..))")
    public void afterThrowing() {
        System.out.println("回滚事务");
        //获取当前事务，直接回滚
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

}
