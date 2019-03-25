package com.libi.transaction.aop;

import com.libi.transaction.annotation.ExtTransaction;
import com.libi.transaction.utils.TransactionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import java.lang.reflect.Method;

/**
 * @author libi
 * 使用AOP封装注解的扫包操作
 */
@Component
@Aspect
public class AopAnnotationTransaction {
    @Autowired
    private TransactionUtils transactionUtils;
    /**这边规定扫描service下的所有方法*/
    @Around("execution(* com.libi.transaction.service.*.*.*(..))")
    public void around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        //获取方法上的注解,这里把获取注解的方法单独提出来了
        ExtTransaction extTransaction = getExtTransaction(proceedingJoinPoint);

        TransactionStatus status = null;
        if (extTransaction != null) {
            //若果有事务，开启事务
            System.out.println("开启事务");
            status = transactionUtils.begin();
        }
        //调用代理目标方法
        proceedingJoinPoint.proceed();
        if (status != null) {
            //提交事务
            System.out.println("提交事务");
            transactionUtils.commit();
        }
    }

    /**事务的异常通知*/
    @AfterThrowing("execution(* com.libi.transaction.service.*.*.*(..))")
    public void afterThrowing() {
        System.out.println("回滚事务");
       transactionUtils.rollBack();
    }

    /**获取方法上的注解*/
    private ExtTransaction getExtTransaction(ProceedingJoinPoint proceedingJoinPoint) throws NoSuchMethodException {
        //获取代理对象的方法
        String methodName = proceedingJoinPoint.getSignature().getName();
        Class<?> targetClass = proceedingJoinPoint.getTarget().getClass();
        Class[] parameterTypes = ((MethodSignature) (proceedingJoinPoint.getSignature())).getParameterTypes();
        Method targetMethod = targetClass.getMethod(methodName, parameterTypes);
        //获取方法上的注解
        return targetMethod.getAnnotation(ExtTransaction.class);
    }



}
