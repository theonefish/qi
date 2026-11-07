package com.example.qi.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis-Plus 手动装配。
 *
 * Spring Boot 4 下 MP 自动配置在 MybatisPlusProperties.applyTo 阶段
 * 调用不存在的方法导致启动失败，参考社区方案排除自动配置后手动装配。
 * 原 application.yml 中 mybatis-plus 的配置项（驼峰映射、SQL 日志、
 * 逻辑删除、时间填充）全部移到此处，行为与原配置一致。
 */
@Configuration
@MapperScan(basePackages = "com.example.qi", annotationClass = Mapper.class)
public class MybatisPlusDataSourceConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               MybatisPlusInterceptor mybatisPlusInterceptor,
                                               ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider) throws Exception {
        // 必须用 MP 的 MybatisSqlSessionFactoryBean，原生 SqlSessionFactoryBean 会丢失 MP 增强
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPlugins(mybatisPlusInterceptor);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(StdOutImpl.class);   // 开发期打印 SQL，交付前删除
        factoryBean.setConfiguration(configuration);

        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setLogicDeleteField("deleted");      // 5.1 / 7.10 软删除
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);

        // 时间自动填充（MybatisPlusConfig.FillTimeHandler）
        MetaObjectHandler fillHandler = metaObjectHandlerProvider.getIfAvailable();
        if (fillHandler != null) {
            globalConfig.setMetaObjectHandler(fillHandler);
        }
        factoryBean.setGlobalConfig(globalConfig);

        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
