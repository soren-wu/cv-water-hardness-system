package com.example.titration.module.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解，标注在 Controller 方法上自动记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作类型，如「登录」「提交实验」「批阅」 */
    String value();

    /** 操作内容描述 */
    String content() default "";
}
