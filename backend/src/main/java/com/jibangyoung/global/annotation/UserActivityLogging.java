package com.jibangyoung.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🎯 사용자 활동 로깅 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserActivityLogging {
    String actionType();

    int scoreDelta() default 0;

    Priority priority() default Priority.NORMAL;

    String description() default "";

    boolean logOnFailure() default true;

    enum Priority {
        NORMAL, HIGH, CRITICAL
    }
}