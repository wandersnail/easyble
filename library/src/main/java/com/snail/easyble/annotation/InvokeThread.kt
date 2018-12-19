package com.snail.easyble.annotation

import java.lang.annotation.Inherited

/**
 * 描述: 调用线程
 * 时间: 2018/12/11 21:18
 * 作者: zengfansheng
 */
@Inherited
@kotlin.annotation.Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class InvokeThread(val value: RunOn = RunOn.POSTING)
