package com.gourmet.common.interceptor;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.model.LoginUser;
import com.gourmet.common.result.ResultCode;
import com.gourmet.common.util.JwtProperties;
import com.gourmet.common.util.JwtUtil;
import com.gourmet.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * JWT 登录认证与角色鉴权拦截器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 线程池复用时应先清空上下文，避免 preHandle 抛异常导致 afterCompletion 未执行而串号
        UserContext.clear();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        boolean permitAll = isPermitAll(request);
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                LoginUser loginUser = jwtUtil.parseToken(token);
                UserContext.set(loginUser);
            } catch (BusinessException e) {
                // 公开接口携带过期令牌时按匿名处理，其余接口直接返回 401
                if (!permitAll) {
                    throw e;
                }
                UserContext.clear();
            }
        }

        LoginUser current = UserContext.get();
        RequireRole requireRole = resolveRequireRole(handler);

        // 已登录用户：校验角色（公开接口上也生效，避免越权访问）
        if (requireRole != null && current != null) {
            if (!hasRole(current, requireRole.value())) {
                log.warn("角色校验失败：当前角色={}，需要角色={}",
                        current.getRole(), Arrays.toString(requireRole.value()));
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            return true;
        }

        // 未登录：公开接口放行，其余接口一律 401
        if (current == null && !permitAll) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private RequireRole resolveRequireRole(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        return requireRole;
    }

    private boolean hasRole(LoginUser loginUser, String[] roles) {
        return Arrays.stream(roles).anyMatch(role -> role.equalsIgnoreCase(loginUser.getRole()));
    }

    private boolean isPermitAll(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        final String target = path;
        return securityProperties.getPermitAll().stream()
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> pathMatcher.match(pattern, target));
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String prefix = jwtProperties.getPrefix();
        if (StringUtils.hasText(prefix) && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return header.trim();
    }
}
