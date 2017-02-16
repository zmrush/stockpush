package push.datasource;


import push.registry.URL;

/**
 * HikariCP实现
 */
public class HikariDataSourceBuilder implements DataSourceBuilder {
    @Override
    public XDataSource build(DataSourceConfig config) {
        HikariXDataSource ds = new HikariXDataSource();
        ds.setJdbcUrl(config.getUrl());
        ds.setUsername(config.getUser());
        ds.setPassword(config.getPassword());
        ds.setDriverClassName(config.getDriver());
        ds.setConnectionTimeout(config.getConnectionTimeout());
        ds.setIdleTimeout(config.getIdleTimeout());
        ds.setMaxLifetime(config.getMaxLifetime());
        ds.setMaximumPoolSize(config.getMaxPoolSize());
        ds.setMinimumIdle(config.getMinIdle());
        ds.setConnectionTestQuery(config.getValidationQuery());
        ds.setAutoCommit(config.isAutoCommit());
        ds.setTransactionIsolation(config.getTransactionIsolation());
        ds.setReadOnly(config.isReadOnly());
        ds.setCatalog(config.getCatalog());
        System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", String.valueOf(config.getCleanupInterval()));

        if (config.getConnectionProperties() != null) {
            String[] properties = config.getConnectionProperties().split(";");
            int pos;
            String name, value;
            for (String property : properties) {
                pos = property.indexOf('=');
                if (pos > 0 && pos < property.length() - 1) {
                        name = property.substring(0, pos);
                        value = property.substring(pos + 1);
                        ds.addDataSourceProperty(name, value);
                }
            }
        }
        return ds;
    }

    public String getType() {
        return "HikariCP";
    }

    public void setUrl(URL url) {

    }
}
