package com.snail.easyble.annotation

import java.lang.annotation.Inherited

/**
 * 标识方法期望被调用的线程
 * 
 * date: 2018/12/11 21:18
 * author: zengfansheng
 */
@kotlin.annotation.MustBeDocumented
@Inherited
@kotlin.annotation.Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class InvokeThread(val value: RunOn)
