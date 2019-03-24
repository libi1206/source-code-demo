package com.libi.annotation;

import java.lang.annotation.*;

/**
 * @author libi
 * 自己实现的事务注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtTransaction {

}
