package camel_transport;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.component.mybatis.MyBatisComponent;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MybatisDataSourceProvider implements CamelContextAware {
    private static final Logger logger = LoggerFactory.getLogger(MybatisDataSourceProvider.class);

    private CamelContext camelContext;
    private DataSource dataSource;

        
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }
    
    public void setDataSource() throws Exception {
        MyBatisComponent component = camelContext.getComponent("mybatis", MyBatisComponent.class);
        Environment environment = new Environment("metadata", new JdbcTransactionFactory()  , dataSource);
        component.start();
        component.getSqlSessionFactory().getConfiguration().setEnvironment(environment);
    }


}
