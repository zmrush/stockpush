package push.datasource;


/**
 * 数据源工厂类
 */
public class DataSourceFactory {

    private DataSourceConfig config;

    public DataSourceFactory(DataSourceConfig config) {
        this.config = config;
    }

    /**
     * 构建数据源
     *
     * @param config 数据源配置
     * @return 数据源
     */
    public static XDataSource build(DataSourceConfig config) {
        if (config == null) {
            return null;
        }
        DataSourceFactory factory = new DataSourceFactory(config);
        return factory.build();
    }

    /**
     * 构建数据源
     *
     * @return 数据源
     */
    public XDataSource build() {
        DataSourceBuilder builder = new HikariDataSourceBuilder();
        if (builder == null) {
            return null;
        }
        return builder.build(config);
    }

}
