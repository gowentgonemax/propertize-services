package com.propertize.platform.employecraft.aspect;

import com.propertize.commons.aspect.BaseLoggingAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect extends BaseLoggingAspect {

    @Around("execution(* com.propertize.platform.employecraft.service..*(..)) || " +
            "execution(* com.propertize.platform.employecraft.controller..*(..))")
    public Object log(ProceedingJoinPoint jp) throws Throwable {
        return logMethodExecution(jp);
    }
}

