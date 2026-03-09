package br.com.evolui.portalevolui.web.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfiguration {

    @Autowired
    private DataSource dataSource;

    @Bean
    @DependsOn(value = "entityManagerFactory")
    public CustomSpringLiquibase liquibase() {
        LiquibaseProperties liquibaseProperties = new LiquibaseProperties();
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog(liquibaseProperties.getChangeLog());
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDataSource(this.dataSource);
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        liquibase.setShouldRun(true);
        //liquibase.setLabels(liquibaseProperties.getLabels());
        liquibase.setChangeLogParameters(liquibaseProperties.getParameters());
        return new CustomSpringLiquibase(liquibase);
    }

    public class CustomSpringLiquibase implements InitializingBean, BeanNameAware, ResourceLoaderAware {

        private SpringLiquibase springLiquibase;

        public CustomSpringLiquibase(SpringLiquibase springLiquibase) {
            this.springLiquibase = springLiquibase;
        }


        @Override
        public void afterPropertiesSet() throws Exception {

            springLiquibase.afterPropertiesSet();
        }

        @Override
        public void setBeanName(String s) {
            springLiquibase.setBeanName(s);
        }

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            springLiquibase.setResourceLoader(resourceLoader);
        }
    }

}
