package com.libi.mybatis.annotation;

import java.lang.annotation.*;

/**
 * @author libi
 * 表示参数
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ExtParam {
    String value();
}
