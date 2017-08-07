package push.middle.dao;


import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.datasource.DaoUtil;
import push.datasource.XDataSource;
import push.model.dao.AbstractDao;
import push.model.message.GroupMessage;

import java.sql.PreparedStatement;

/**
 * Created by lizheng on 2017/3/7.
 */
public class SaveMessageDao extends AbstractDao {

    private static final Logger logger = LoggerFactory.getLogger(SaveMessageDao.class);
    public SaveMessageDao(XDataSource dataSource){
        super(dataSource);
    }

    public static final String CREATE_MESSAGE_SQL = "insert into push_message(nodeId,payload,createtime) values (?,?,now())";


    public int saveMessage(final GroupMessage groupMessage) throws Exception{
        try{
            int count= DaoUtil.insert(dataSource, groupMessage, CREATE_MESSAGE_SQL, new DaoUtil.UpdateCallback<GroupMessage>() {
                public void before(PreparedStatement statement, GroupMessage target) throws Exception {
                    statement.setInt(1,groupMessage.getNodeid());
                    statement.setString(2,groupMessage.getMessage());
                }
            });
            return count;
        }catch(Exception e){
            logger.error("网络异常,创建消息异常");
            return 0;
        }
    }
}
