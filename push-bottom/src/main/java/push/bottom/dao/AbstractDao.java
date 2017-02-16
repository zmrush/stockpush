package push.bottom.dao;

import javax.sql.DataSource;

/**
 * Created by mingzhu7 on 2017/2/14.
 */
public abstract class AbstractDao {
    protected DataSource dataSource;

    protected AbstractDao() {
    }

    protected AbstractDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
