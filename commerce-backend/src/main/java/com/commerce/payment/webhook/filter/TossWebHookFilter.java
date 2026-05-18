package com.commerce.payment.webhook.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class TossWebHookFilter extends OncePerRequestFilter {

    private static final String TOSS_WEBHOOK_PATH_PREFIX = "/v1/payment/toss/webhook/";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(TOSS_WEBHOOK_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            log.warn("토스 웹훅 요청의 Content-Type 이 application/json 이 아닙니다. uri={}, contentType={}",
                    request.getRequestURI(), contentType);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        log.info("토스 웹훅 수신. uri={}, remote={}", request.getRequestURI(), request.getRemoteAddr());
        filterChain.doFilter(request, response);
    }
}
