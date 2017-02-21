package push.bottom.dao;

import push.bottom.message.Registration;
import push.bottom.model.User;
import push.datasource.DaoUtil;
import push.datasource.DataSourceConfig;
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

    public static void main(String[] args) throws Exception{
        push.datasource.DataSourceConfig dataSourceConfig=new DataSourceConfig();
        dataSourceConfig.setType("HikariCP");
        dataSourceConfig.setDriver("com.mysql.jdbc.Driver");
        dataSourceConfig.setUrl("jdbc:mysql://10.100.141.39:3306/tm_dte");
        dataSourceConfig.setUser("tm_dte");
        dataSourceConfig.setPassword("tm_dte123");

        push.datasource.DataSourceFactory dataSourceFactory = new push.datasource.DataSourceFactory(dataSourceConfig);
        UserDao userDao= new UserDao(dataSourceFactory.build());
        String str = "lizheng";
        String password="123456";

        Registration user = new Registration();
        for(int i=1;i<=1000;i++){
            user.setUsername(str+String.valueOf(i));
            user.setPassword(password);
            userDao.createNewUser(user);
        }

    }
}
