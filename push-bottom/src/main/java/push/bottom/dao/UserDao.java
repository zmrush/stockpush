package push.bottom.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.bottom.message.User;
import push.datasource.DaoUtil;
import push.datasource.XDataSource;
import push.model.dao.AbstractDao;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Created by mingzhu7 on 2017/2/14.
 */
public class UserDao extends AbstractDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    public UserDao(DataSource dataSource){
        super(dataSource);
    }

    public static final String FIND_BY_ID_SQL ="SELECT username,password,type FROM push_user where username=? and password=?";

    public User findByUserInfo(final String username, final String password) throws Exception{
        return DaoUtil.queryObject(dataSource, FIND_BY_ID_SQL, new DaoUtil.QueryCallback<User>() {
                    @Override
                    public User map(final ResultSet rs) throws Exception {
                        User target = new User();
                        target.setUsername(rs.getString(1));
                        target.setPassword(rs.getString(2));
                        target.setType(rs.getInt(3));
                        return target;
                    }

                    @Override
                    public void before(final PreparedStatement statement) throws Exception {
                        statement.setString(1, username);
                        statement.setString(2, password);
                    }
                }
        );
    }

}
