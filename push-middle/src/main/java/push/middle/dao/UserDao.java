package push.middle.dao;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.datasource.DaoUtil;
import push.datasource.DataSourceConfig;
import push.datasource.DataSourceFactory;
import push.datasource.XDataSource;
import push.middle.pojo.Registration;
import push.model.dao.AbstractDao;

import java.sql.PreparedStatement;

/**
 * Created by mingzhu7 on 2017/2/14.
 */
public class UserDao extends AbstractDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    public UserDao(XDataSource dataSource){
        super(dataSource);
    }
    public static final String FIND_BY_ID_SQL =
            "SELECT username,password,type" +
                    "" + " FROM push_user where username=? and password=?";
    public static final String INSERTUSER =
            "insert into push_user(username,password,create_date,update_date) values(?,?,now(),now())";


    public int createNewUser(final Registration registration) throws Exception{
        try {
            int count = DaoUtil.insert(dataSource, registration, INSERTUSER, new DaoUtil.UpdateCallback<Registration>() {
                public void before(PreparedStatement statement, Registration target) throws Exception {
                    statement.setString(1,registration.getUsername());
                    statement.setString(2,registration.getPassword());
                }
            });
            return count;
        } catch (MySQLIntegrityConstraintViolationException e) {
            logger.error("该用户已经存在,不能重复创建");
            return 0;
        }
    }

    public static void main(String[] args) throws Exception{
        DataSourceConfig dataSourceConfig=new DataSourceConfig();
        dataSourceConfig.setType("HikariCP");
        dataSourceConfig.setDriver("com.mysql.jdbc.Driver");
        dataSourceConfig.setUrl("jdbc:mysql://10.100.141.39:3306/tm_dte");
        dataSourceConfig.setUser("tm_dte");
        dataSourceConfig.setPassword("tm_dte123");

        DataSourceFactory dataSourceFactory = new DataSourceFactory(dataSourceConfig);
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
