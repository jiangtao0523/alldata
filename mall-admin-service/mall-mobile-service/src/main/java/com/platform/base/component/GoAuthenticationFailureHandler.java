package com.platform.base.component;

import cn.hutool.json.JSONUtil;
import com.platform.common.api.CommonResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wulinhao on 2019/8/6.
 */
public class GoAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        response.getWriter().print(JSONUtil.parse(CommonResult.failed("登录失败：" + exception.getMessage())));
        response.getWriter().flush();
    }
}