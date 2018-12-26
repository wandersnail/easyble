package com.snail.easyble.annotation

import java.lang.annotation.Inherited

/**
 * Markup the function will be called on which thread.
 * 
 * date: 2018/12/11 21:18
 * author: zengfansheng
 */
@kotlin.annotation.MustBeDocumented
@Inherited
@kotlin.annotation.Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class InvokeThread(val value: RunOn = RunOn.POSTING)
