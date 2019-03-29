package com.libi.mybatis.annotation;

import java.lang.annotation.*;

/**
 * @author libi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExtSelect {
    String value();
}
