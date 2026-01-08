package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.context.BaseContext;
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
public class JwtTokenAdminInterceptor implements HandlerInterceptor, ApplicationContextAware {

    @Autowired
    private JwtProperties jwtProperties;
    
    private static EmployeeService employeeService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        JwtTokenAdminInterceptor.employeeService = applicationContext.getBean(EmployeeService.class);
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
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            
            // 验证JWT令牌版本号
            Integer tokenVersion = claims.get(JwtClaimsConstant.TOKEN_VERSION, Integer.class);
            Employee employee = employeeService.getById(empId);
            if (employee != null && !employee.getTokenVersion().equals(tokenVersion)) {
                // 令牌版本不匹配，说明令牌已被使失效
                log.info("JWT令牌版本不匹配，empId: {}, tokenVersion: {}, currentVersion: {}", 
                         empId, tokenVersion, employee.getTokenVersion());
                response.setStatus(401);
                return false;
            }
            
            log.info("当前员工id：{}", empId);
            //设置当前登录用户的id
            BaseContext.setCurrentId(empId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
}