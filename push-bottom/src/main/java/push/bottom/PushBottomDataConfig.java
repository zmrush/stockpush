package push.bottom;


import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import push.bottom.dao.MessageDao;
import push.bottom.dao.UserDao;

import javax.sql.DataSource;

@Configuration
public class PushBottomDataConfig {
    private static Logger logger= LoggerFactory.getLogger(PushBottomDataConfig.class);

    @Bean(name = "primaryDatasource")
    @ConfigurationProperties(prefix="spring.datasource.push.bottom")
    @Primary
    public DataSource getDatasource(){
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "transactionManagerPrimary")
    @Primary
    DataSourceTransactionManager transactionManagerPrimary() {
        DataSourceTransactionManager dataSourceTransactionManager=new DataSourceTransactionManager(getDatasource());
        return  dataSourceTransactionManager;
    }
    @Bean
    UserDao getUserDao(){
        return new UserDao(getDatasource());
    }
    @Bean
    MessageDao getMessageDao(){
        return new MessageDao(getDatasource());
    }

}
