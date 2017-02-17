package push.bottom.dao;

import push.bottom.message.NodeBean;
import push.datasource.DaoUtil;
import push.datasource.XDataSource;

import java.sql.PreparedStatement;

/**
 * Created by lizheng on 2017/2/17.
 */
public class NodeDao extends AbstractDao{
    public NodeDao(XDataSource dataSource){
        super(dataSource);
    }

    public static final String CREATE_NODE_SQL = "insert into push_node(nodename,host,description,type,createtime) values (?,?,?,?,now())";
    public static final String DELETE_NODE_BY_NODENAME_SQL ="delete from push_node where nodeid= ? ";

    public int createNode(final NodeBean nodeBean) throws Exception{
        int count=DaoUtil.insert(dataSource, nodeBean, CREATE_NODE_SQL, new DaoUtil.UpdateCallback<NodeBean>() {
            public void before(PreparedStatement statement, NodeBean target) throws Exception {
                statement.setString(1,nodeBean.getNodename());
                statement.setString(2,nodeBean.getHost());
                statement.setString(3,nodeBean.getDescription());
                statement.setString(4,nodeBean.getType());
            }
        });
        return count;
    }

    public int deleteNode(final NodeBean nodeBean) throws Exception{
        int count= DaoUtil.delete(dataSource,nodeBean,DELETE_NODE_BY_NODENAME_SQL,new DaoUtil.UpdateCallback<NodeBean>(){
            public void before(PreparedStatement statement,NodeBean target) throws Exception{
                statement.setInt(1,nodeBean.getNodeid());
            }
        });
        return count;
    }

}
