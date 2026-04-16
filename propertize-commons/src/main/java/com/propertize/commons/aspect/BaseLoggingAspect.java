package com.propertize.commons.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base logging aspect providing method execution tracing.
 * Subclasses define the pointcut and delegate to {@link #logMethodExecution}.
 */
public abstract class BaseLoggingAspect {

    protected Object logMethodExecution(ProceedingJoinPoint jp) throws Throwable {
        Logger log = LoggerFactory.getLogger(jp.getTarget().getClass());
        MethodSignature sig = (MethodSignature) jp.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        long start = System.currentTimeMillis();
        try {
            Object result = jp.proceed();
            log.debug("[OK] {} ({}ms)", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.warn("[FAIL] {} threw {} ({}ms): {}", method,
                    ex.getClass().getSimpleName(), System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
