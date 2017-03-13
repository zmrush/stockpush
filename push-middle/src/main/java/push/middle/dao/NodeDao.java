package push.middle.dao;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.datasource.DaoUtil;
import push.datasource.DataSourceConfig;
import push.datasource.DataSourceFactory;
import push.datasource.XDataSource;
import push.middle.pojo.NodeBean;
import push.model.dao.AbstractDao;

import java.sql.PreparedStatement;

/**
 * Created by lizheng on 2017/2/17.
 */
public class NodeDao extends AbstractDao {
    private static final Logger logger= LoggerFactory.getLogger(NodeDao.class);

    public NodeDao(XDataSource dataSource){
        super(dataSource);
    }

    public static final String CREATE_NODE_SQL = "insert into push_node(nodename,description,type,createtime) values (?,?,?,now())";
    public static final String DELETE_NODE_BY_NODEID_SQL ="delete from push_node where nodeid= ? ";
    public static final String DELETE_NODE_BY_NODENAME_SQL ="delete from push_node where nodename= ? ";

    public int createNode(final NodeBean nodeBean) throws Exception{
        try{
            int count=DaoUtil.insert(dataSource, nodeBean, CREATE_NODE_SQL, new DaoUtil.UpdateCallback<NodeBean>() {
                public void before(PreparedStatement statement, NodeBean target) throws Exception {
                    statement.setString(1,nodeBean.getNodeName());
                    statement.setString(2,nodeBean.getDescription());
                    statement.setInt(3,Integer.parseInt(nodeBean.getNodeType()));
                }
            });
            return count;
        }catch(MySQLIntegrityConstraintViolationException e){
            logger.error("该节点已经存在,不能重复创建");
            return 0;
        }
    }

    /**
     * 根据nodeid删除节点
     * @param nodeBean
     * @return
     * @throws Exception
     */
    public int deleteNodeById(final NodeBean nodeBean) throws Exception{
        int count= DaoUtil.delete(dataSource,nodeBean,DELETE_NODE_BY_NODEID_SQL,new DaoUtil.UpdateCallback<NodeBean>(){
            public void before(PreparedStatement statement,NodeBean target) throws Exception{
                statement.setInt(1,nodeBean.getNodeId());
            }
        });
        return count;
    }

    public
    /**
     * 根据nodename删除节点
     * @param nodeBean
     * @return
     * @throws Exception
     */
    public int deleteNodeByName(final NodeBean nodeBean) throws Exception{
        int count= DaoUtil.delete(dataSource,nodeBean,DELETE_NODE_BY_NODENAME_SQL,new DaoUtil.UpdateCallback<NodeBean>(){
            public void before(PreparedStatement statement,NodeBean target) throws Exception{
                statement.setString(1,nodeBean.getNodeName());
            }
        });
        return count;
    }

    /**
     * Test测试
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        DataSourceConfig dataSourceConfig=new DataSourceConfig();
        dataSourceConfig.setType("HikariCP");
        dataSourceConfig.setDriver("com.mysql.jdbc.Driver");
        dataSourceConfig.setUrl("jdbc:mysql://10.100.141.39:3306/tm_dte");
        dataSourceConfig.setUser("tm_dte");
        dataSourceConfig.setPassword("tm_dte123");

        DataSourceFactory dataSourceFactory = new DataSourceFactory(dataSourceConfig);
        NodeDao nodeDao= new NodeDao(dataSourceFactory.build());

        String nodename ="pushAllTest5";
        String description ="全体公告5";
        String nodetype="0";

        NodeBean nodeBean =new NodeBean();
        nodeBean.setNodeName(nodename);
        nodeBean.setDescription(description);
        nodeBean.setNodeType(nodetype);
        int count = nodeDao.createNode(nodeBean);
        if(count==1){
            logger.info("创建节点："+nodename+"成功。");
        }

    }

}
