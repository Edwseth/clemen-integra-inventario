package com.willyes.clemenintegra.shared.performance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final long WARN_THRESHOLD_MS = 2500;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();

            if (elapsedMs >= WARN_THRESHOLD_MS) {
                log.warn("RequestTiming method={} uri={} status={} elapsedMs={}", method, uri, status, elapsedMs);
            } else {
                log.info("RequestTiming method={} uri={} status={} elapsedMs={}", method, uri, status, elapsedMs);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.startsWith("/webjars")
                || uri.startsWith("/favicon.ico");
    }
}
