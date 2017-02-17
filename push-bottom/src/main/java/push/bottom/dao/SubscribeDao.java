package push.bottom.dao;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import push.bottom.message.SubscribeBean;
import push.datasource.DaoUtil;
import push.datasource.DataSourceConfig;
import push.datasource.XDataSource;

import java.sql.PreparedStatement;

/**
 * Created by lizheng on 2017/2/17.
 */
public class SubscribeDao extends AbstractDao{
    public SubscribeDao(XDataSource dataSource){
        super(dataSource);
    }

    public static final String SUBSCRIBE_SQL="insert into push_subscribe(nodeid,uid,subscribetime) values(?,?,now()) ";

    public int subscribeNode(final SubscribeBean subscribeBean) throws Exception{
        try {
            int count = DaoUtil.insert(dataSource, subscribeBean, SUBSCRIBE_SQL, new DaoUtil.UpdateCallback<SubscribeBean>() {
                public void before(PreparedStatement statement, SubscribeBean target) throws Exception {
                    statement.setInt(1, subscribeBean.getNodeid());
                    statement.setString(2, subscribeBean.getUid());
                }
            });
            return count;
        }catch (MySQLIntegrityConstraintViolationException e){
            return 1;
        }
    }
    public static void main(String[] args) throws Exception{
        push.datasource.DataSourceConfig dataSourceConfig=new DataSourceConfig();
        dataSourceConfig.setType("HikariCP");
        dataSourceConfig.setDriver("com.mysql.jdbc.Driver");
        dataSourceConfig.setUrl("jdbc:mysql://10.100.141.39:3306/tm_dte");
        dataSourceConfig.setUser("tm_dte");
        dataSourceConfig.setPassword("tm_dte123");

        push.datasource.DataSourceFactory dataSourceFactory = new push.datasource.DataSourceFactory(dataSourceConfig);
        SubscribeDao subscribeDao= new SubscribeDao(dataSourceFactory.build());

        String uid ="hello";
        int nodeid=1;

        SubscribeBean subscribeBean =new SubscribeBean();
        subscribeBean.setUid(uid);
        subscribeBean.setNodeid(nodeid);

//        SubscribeDao subscribe = new SubscribeDao();
        int count = subscribeDao.subscribeNode(subscribeBean);
        System.out.println("订阅节点的个数-----》"+count);
    }

}
