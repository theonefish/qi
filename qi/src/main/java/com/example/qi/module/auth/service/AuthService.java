package com.example.qi.module.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.common.BusinessException;
import com.example.qi.module.auth.dto.LoginRequest;
import com.example.qi.module.auth.dto.LoginResponse;
import com.example.qi.module.auth.entity.AdminUser;
import com.example.qi.module.auth.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理员认证（ADM-01）：账号密码登录、退出、当前操作人获取。
 * 操作管理员从登录会话获取，发布记录不允许手工填写（7.14）。
 * 登录成功/失败与退出输出审计日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserMapper adminUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 发布记录等场景使用的操作人信息 */
    public record Operator(long id, String name) {
    }

    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, request.getUsername()));
        // 账号不存在与密码错误统一提示，避免暴露账号是否存在
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败：账号不存在或密码错误 username={}", request.getUsername());
            throw new BusinessException("账号或密码错误");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("adminId", user.getId());
        StpUtil.getSession().set("displayName", user.getDisplayName());
        log.info("管理员登录成功：adminId={} displayName={}", user.getId(), user.getDisplayName());

        LoginResponse resp = new LoginResponse();
        resp.setToken(StpUtil.getTokenValue());
        resp.setDisplayName(user.getDisplayName());
        return resp;
    }

    public void logout() {
        long adminId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("管理员退出登录：adminId={}", adminId);
    }

    public Operator currentOperator() {
        StpUtil.checkLogin();
        long id = StpUtil.getLoginIdAsLong();
        String name = (String) StpUtil.getSession().get("displayName");
        return new Operator(id, name == null ? "未知管理员" : name);
    }
}
