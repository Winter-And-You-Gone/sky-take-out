package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor, ApplicationContextAware {

    @Autowired
    private JwtProperties jwtProperties;
    
    private static UserService userService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        JwtTokenUserInterceptor.userService = applicationContext.getBean(UserService.class);
    }

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userID = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            
            // 验证JWT令牌版本号
            Integer tokenVersion = claims.get(JwtClaimsConstant.TOKEN_VERSION, Integer.class);
            User user = userService.getById(userID);
            if (user != null && !user.getTokenVersion().equals(tokenVersion)) {
                // 令牌版本不匹配，说明令牌已被使失效
                log.info("JWT令牌版本不匹配，userID: {}, tokenVersion: {}, currentVersion: {}",
                         userID, tokenVersion, user.getTokenVersion());
                response.setStatus(401);
                return false;
            }
            
            log.info("当前用户id：{}", userID);
            //设置当前登录用户的id
            BaseContext.setCurrentId(userID);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
}