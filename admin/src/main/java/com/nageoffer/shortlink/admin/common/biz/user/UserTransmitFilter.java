package com.nageoffer.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {
    private final StringRedisTemplate stringRedisTemplate;
    private static final List<String> IGNORE_URI = Lists.newArrayList(
            "/api/short-link/usr/login",
            "/api/short-link/admin/v1/user/has-username"
    );
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();
        if (!IGNORE_URI.contains(requestURI)){
            String username = httpServletRequest.getHeader("username");
            String token = httpServletRequest.getHeader("token");
            if (StrUtil.isBlank(username) || StrUtil.isBlank(token)){
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            Object userInfoStr = stringRedisTemplate.opsForHash().get("login_" + username, token);
            if (userInfoStr != null){
                UserInfoDTO userInfoDTO = JSONUtil.toBean(userInfoStr.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);
            }
            try {
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                UserContext.removeUser();
            }
        }

    }
}
