package push.bottom.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.datasource.DaoUtil;
import push.datasource.XDataSource;
import push.model.dao.AbstractDao;
import push.model.message.GroupMessage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Created by lizheng on 2017/3/20.
 */
public class MessageDao extends AbstractDao {
    private static final Logger logger = LoggerFactory.getLogger(MessageDao.class);
    public MessageDao(XDataSource dataSource){

        super(dataSource);
    }

    private static final String QUERY_LASTMESSAGE_BYNODEID_SQL="select * from push_message where nodeid=? order by createTime desc limit 1" ;

    /**
     * 根据nodeid查询该节点最后一条消息
     * @param nodeid
     * @return
     * @throws Exception
     */
    public GroupMessage queryLastMessageByNodeid(final int nodeid) throws Exception{
        return DaoUtil.queryObject(dataSource, QUERY_LASTMESSAGE_BYNODEID_SQL, new DaoUtil.QueryCallback<GroupMessage>() {
            public GroupMessage map(ResultSet rs) throws Exception {
                GroupMessage target= new GroupMessage();
                target.setNodeid(rs.getInt(2));
                target.setMessage(rs.getString(3));
                return target;
            }

            public void before(PreparedStatement statement) throws Exception {
                statement.setInt(1,nodeid);
            }
        });

    }

}
