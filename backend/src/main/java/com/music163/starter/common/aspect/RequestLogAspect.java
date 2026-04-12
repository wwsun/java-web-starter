package com.music163.starter.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * API 请求审计日志切面
 * <p>
 * 拦截所有 RestController 方法，记录请求摘要和耗时。
 * 不记录请求参数，避免密码等敏感字段写入日志。
 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logRequest(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = currentRequest();
        String method = request != null ? request.getMethod() : "-";
        String uri    = request != null ? request.getRequestURI() : "-";
        String user   = currentUsername();

        log.info("[REQ]  {} {} user={}", method, uri, user);

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("[RESP] {} {} user={} cost={}ms", method, uri, user, elapsed(start));
            return result;
        } catch (Exception e) {
            log.warn("[RESP] {} {} user={} cost={}ms error={}",
                    method, uri, user, elapsed(start), e.getMessage());
            throw e;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
