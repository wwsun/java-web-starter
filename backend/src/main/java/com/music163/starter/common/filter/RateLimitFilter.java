package com.music163.starter.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.common.result.Result;
import com.music163.starter.common.result.ResultCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 级别速率限制过滤器
 * <p>
 * 防止登录/注册接口遭受暴力破解或垃圾注册攻击。
 * 超出阈值时返回 HTTP 429，并附带 Retry-After 响应头。
 * <p>
 * 限额（可按需调整）：
 * - /api/auth/login    ：5 次 / 分钟 / IP
 * - /api/auth/register ：3 次 / 分钟 / IP
 * <p>
 * ⚠️ 当前使用 JVM 内存存储 Bucket，仅适合单实例部署。
 * 生产多实例场景请替换为 Bucket4j + Redis（bucket4j-redis 模块）。
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE + 1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    /** 登录接口：每分钟最多 5 次 */
    private static final int LOGIN_CAPACITY = 5;

    /** 注册接口：每分钟最多 3 次 */
    private static final int REGISTER_CAPACITY = 3;

    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    /** 按 "ip:uri" 为键，每个 IP + 接口独立计数 */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        int capacity = resolveCapacity(uri);

        // 非限流接口直接放行
        if (capacity <= 0) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);
        String bucketKey = ip + ":" + uri;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> buildBucket(capacity));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("[限流] IP={} URI={} 触发速率限制", ip, uri);
            writeTooManyRequestsResponse(response);
        }
    }

    /**
    * 根据 URI 返回对应容量，-1 表示不限流。
    * 这里使用 endsWith 匹配，兼容显式 /api 前缀的控制器映射。
     */
    private int resolveCapacity(String uri) {
        if (uri.endsWith("/auth/login")) return LOGIN_CAPACITY;
        if (uri.endsWith("/auth/register")) return REGISTER_CAPACITY;
        return -1;
    }

    /**
     * 构建令牌桶：容量为 capacity，每分钟补充 capacity 个令牌（整批补充）。
     */
    private static Bucket buildBucket(int capacity) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, REFILL_DURATION)
                        .build())
                .build();
    }

    /**
     * 优先读取反向代理设置的 X-Forwarded-For 头，取第一个 IP；
     * 否则使用直连 RemoteAddr。
     */
    private static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 写入 HTTP 429 响应，body 沿用项目统一的 Result 格式。
     */
    private void writeTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(REFILL_DURATION.getSeconds()));
        String body = objectMapper.writeValueAsString(Result.fail(ResultCode.TOO_MANY_REQUESTS));
        response.getWriter().write(body);
    }
}
