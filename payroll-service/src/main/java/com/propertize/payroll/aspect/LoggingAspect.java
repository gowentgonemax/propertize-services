package com.propertize.payroll.aspect;

import com.propertize.payroll.config.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect for logging method execution with correlation ID
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.propertize.payroll.service..*(..)) || " +
            "execution(* com.propertize.payroll.controller..*(..))")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String correlationId = CorrelationIdUtil.getCorrelationId();

        long startTime = System.currentTimeMillis();

        log.debug("[{}] Entering {}.{} with args: {}",
                correlationId,
                className,
                methodName,
                Arrays.toString(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            log.debug("[{}] Exiting {}.{} - Execution time: {}ms",
                    correlationId,
                    className,
                    methodName,
                    executionTime);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("[{}] Exception in {}.{} after {}ms: {}",
                    correlationId,
                    className,
                    methodName,
                    executionTime,
                    e.getMessage(),
                    e);

            throw e;
        }
    }
}

