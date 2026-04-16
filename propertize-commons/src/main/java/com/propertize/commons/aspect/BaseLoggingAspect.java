package com.propertize.commons.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Arrays;
import java.util.UUID;

/**
 * Reusable logging logic for service/controller method execution.
 * <p>
 * Each consuming service creates a concrete {@code @Aspect @Component}
 * subclass and wires the {@code @Around} pointcut to its own packages:
 * <pre>{@code
 * @Aspect @Component
 * public class ServiceLoggingAspect extends BaseLoggingAspect {
 *     @Around("execution(* com.propertize.payment.service..*(..)) || " +
 *             "execution(* com.propertize.payment.controller..*(..))")
 *     public Object log(ProceedingJoinPoint jp) throws Throwable {
 *         return logMethodExecution(jp);
 *     }
 * }
 * }</pre>
 */
@Slf4j
public abstract class BaseLoggingAspect {

    protected Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String className = sig.getDeclaringType().getSimpleName();
        String methodName = sig.getName();
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        long start = System.currentTimeMillis();

        log.debug("[{}] Entering {}.{} args={}", correlationId, className, methodName,
                Arrays.toString(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            long ms = System.currentTimeMillis() - start;
            log.debug("[{}] Exiting {}.{} ({}ms)", correlationId, className, methodName, ms);
            return result;
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            log.error("[{}] Exception in {}.{} after {}ms: {}", correlationId,
                    className, methodName, ms, e.getMessage(), e);
            throw e;
        }
    }
}

