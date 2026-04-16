package com.propertize.payment.aspect;

import com.propertize.commons.aspect.BaseLoggingAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect extends BaseLoggingAspect {

    @Around("execution(* com.propertize.payment.service..*(..)) || " +
            "execution(* com.propertize.payment.controller..*(..))")
    public Object log(ProceedingJoinPoint jp) throws Throwable {
        return logMethodExecution(jp);
    }
}

