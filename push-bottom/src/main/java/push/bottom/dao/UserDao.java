package push.bottom.dao;

import push.bottom.message.Registration;
import push.bottom.model.User;
import push.datasource.DaoUtil;
import push.datasource.XDataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Created by mingzhu7 on 2017/2/14.
 */
public class UserDao extends AbstractDao{
    public UserDao(XDataSource dataSource){
        super(dataSource);
    }
    public static final String FIND_BY_ID_SQL =
            "SELECT username,password" +
                    "" + " FROM push_user where username=? and password=?";
    public static final String INSERTUSER =
            "insert into push_user(username,password,create_date,update_date) values(?,?,now(),now())";
    public User findByUserInfo(final String username, final String password) throws Exception{
        return DaoUtil.queryObject(dataSource, FIND_BY_ID_SQL, new DaoUtil.QueryCallback<User>() {
                    @Override
                    public User map(final ResultSet rs) throws Exception {
                        User target = new User();
                        target.setUsername(rs.getString(1));
                        target.setPassword(rs.getString(2));
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
    public int createNewUser(final Registration registration) throws Exception{
        int count=DaoUtil.insert(dataSource, registration, INSERTUSER, new DaoUtil.UpdateCallback<Registration>() {
            public void before(PreparedStatement statement, Registration target) throws Exception {
                statement.setString(1,registration.getUsername());
                statement.setString(2,registration.getPassword());
            }
        });
        return count;
    }
}
