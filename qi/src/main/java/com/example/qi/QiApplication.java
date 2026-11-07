package com.example.qi;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 排除 MyBatis-Plus 自动配置：Boot 4 下其属性绑定阶段存在兼容问题，
 * 核心 Bean 改由 config/MybatisPlusDataSourceConfig 手动装配。
 */
@SpringBootApplication(exclude = MybatisPlusAutoConfiguration.class)
public class QiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiApplication.class, args);
    }

}
