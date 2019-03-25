package com.libi.annotation;

import javax.annotation.Resource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * @author libi
 */
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtResource {
}
