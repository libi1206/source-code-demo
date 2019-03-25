package com.libi.ioc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * @author libi
 */
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtResource {
}
