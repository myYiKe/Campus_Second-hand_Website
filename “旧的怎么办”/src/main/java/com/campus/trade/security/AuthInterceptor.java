package com.campus.trade.security;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtTokenProvider jwtTokenProvider, UserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (isPublicRequest(request)) {
            return true;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }
        String token = authorization.substring(7);
        try {
            LoginUser loginUser = jwtTokenProvider.parseToken(token);
            Map<String, Object> user = userMapper.findById(loginUser.userId());
            if (user == null) {
                writeUnauthorized(response, "用户不存在");
                return false;
            }
            String status = String.valueOf(user.get("status"));
            if ("BANNED".equalsIgnoreCase(status)) {
                writeForbidden(response, "账号已被封禁");
                return false;
            }
            UserContext.set(new LoginUser(
                    loginUser.userId(),
                    String.valueOf(user.get("openid")),
                    String.valueOf(user.get("nickname"))
            ));
            return true;
        } catch (Exception e) {
            writeUnauthorized(response, "登录凭证无效");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return "/api/items".equals(uri) || "/api/wanted".equals(uri);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail("UNAUTHORIZED", message)));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail("FORBIDDEN", message)));
    }
}
