package com.example.qi.module.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.module.auth.entity.AdminUser;
import com.example.qi.module.auth.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动初始化：admin_user 为空时创建默认管理员，密码 BCrypt 哈希入库（7.1）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AdminUserMapper adminUserMapper;

    @Override
    public void run(ApplicationArguments args) {
        Long count = adminUserMapper.selectCount(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, "admin"));
        if (count != null && count > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
        admin.setDisplayName("管理员");
        adminUserMapper.insert(admin);
        log.info("已初始化默认管理员账号 admin / admin123，正式使用前请修改密码");
    }
}
