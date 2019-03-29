package com.libi.mybatis.annotation;

import java.lang.annotation.*;

/**
 * @author libi
 * 插入的注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExtInsert {
    String value();
}
