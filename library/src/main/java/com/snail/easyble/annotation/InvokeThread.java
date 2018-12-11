package com.snail.easyble.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述: 调用线程
 * 时间: 2018/12/11 21:18
 * 作者: zengfansheng
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface InvokeThread {
    RunOn value() default RunOn.POSTING; 
}
