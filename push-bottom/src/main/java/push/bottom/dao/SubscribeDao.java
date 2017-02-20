package push.bottom.dao;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.bottom.message.SubscribeBean;
import push.datasource.DaoUtil;
import push.datasource.DataSourceConfig;
import push.datasource.XDataSource;

import java.sql.PreparedStatement;

/**
 * Created by lizheng on 2017/2/17.
 */
public class SubscribeDao extends AbstractDao{
    private static final Logger logger = LoggerFactory.getLogger(SubscribeDao.class);

    public SubscribeDao(XDataSource dataSource){
        super(dataSource);
    }

    public static final String SUBSCRIBE_SQL="insert into push_subscribe(nodeid,uid,subscribetime) values(?,?,now()) ";
    public static final String UNSUBSCRIBE_SQL="delete from push_subscribe where uid=? and nodeid=?";

    /**
     * 订阅节点（单个用户）
     * @param subscribeBean
     * @return
     * @throws Exception
     */
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
            logger.info("该用户"+subscribeBean.getUid()+"已经订阅过节点id:"+subscribeBean.getNodeid());
            return 1;
        }
    }

    /**
     * 反订阅节点
     * @param subscribeBean
     * @return
     * @throws Exception
     */
    public int unSubscribe(final SubscribeBean subscribeBean) throws Exception{

        int count = DaoUtil.delete(dataSource, subscribeBean, UNSUBSCRIBE_SQL, new DaoUtil.UpdateCallback<SubscribeBean>() {
            public void before(PreparedStatement statement, SubscribeBean target) throws Exception {
                statement.setString(1, subscribeBean.getUid());
                statement.setInt(2, subscribeBean.getNodeid());
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
        SubscribeDao subscribeDao= new SubscribeDao(dataSourceFactory.build());

        int nodeid=1;
        String uid ="hello";

        SubscribeBean subscribeBean =new SubscribeBean();
        subscribeBean.setUid(uid);
        subscribeBean.setNodeid(nodeid);

        int count = subscribeDao.subscribeNode(subscribeBean);
        System.out.println("订阅节点的个数-----》"+count);

        int number = subscribeDao.unSubscribe(subscribeBean);
        System.out.println("反订阅节点的个数----->"+number);
    }

}
