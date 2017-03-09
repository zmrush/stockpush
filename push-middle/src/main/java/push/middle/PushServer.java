package push.middle;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.middle.dao.NodeDao;
import push.middle.dao.SaveMessageDao;
import push.middle.dao.UserDao;
import push.middle.pojo.NodeBean;
import push.middle.pojo.Registration;
import push.model.message.GroupMessage;
import push.io.ConnectionEvent;
import push.io.ConnectionListener;
import push.io.SecurePushServer;
import push.model.message.SendMessageEnum;
import push.registry.NetUtil;
import push.registry.URL;
import push.registry.util.PathUtil;
import push.registry.zookeeper.ZKRegistry;
import push.model.message.Entity;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mingzhu7 on 2017/1/5.
 */
public class PushServer {
    private static Logger logger= LoggerFactory.getLogger(PushServer.class);
    private SaveMessageDao saveMessageDao;
    private UserDao userDao;
    private NodeDao nodeDao;

    public final CopyOnWriteArraySet<ChannelHandlerContext> channels=new CopyOnWriteArraySet<ChannelHandlerContext>();
    private ConcurrentHashMap<String,ReentrantLock> lockMap=new ConcurrentHashMap<String, ReentrantLock>();
    private ConcurrentHashMap<String,Condition> conditionMap=new ConcurrentHashMap<String, Condition>();
    private ConcurrentHashMap<String,String> messageMap=new ConcurrentHashMap<String, String>();
    private  SecurePushServer sps;
    static {
    }
    public PushServer(){
    }
    public void start() throws Exception{
        try{
            InputStream is=PushClient.class.getClassLoader().getResourceAsStream("push.properties");
            Properties properties=new Properties();
            properties.load(is);
            String url=properties.getProperty("url");
            int port=Integer.valueOf(properties.getProperty("port"));
            String root=properties.getProperty("root");
            sps=new SecurePushServer(port);
            sps.start();
            sps.addListener(new DefaultConnectionListener());
            String ip = NetUtil.getLocalIP();
            if (ip == null) {
                throw new Exception("Fail to get ip of the server!");
            }
            String nodeName = ip + "_" + port;

            ZKRegistry zkRegistry = new ZKRegistry(URL.valueOf(url));
            zkRegistry.open();
            zkRegistry.createLive(PathUtil.makePath(root, "live", nodeName), null);
        }catch (Exception e){
            logger.error("push server start error",e);
            throw e;
        }
    }


    public class DefaultConnectionListener implements ConnectionListener {
        public void onEvent(ConnectionEvent event) {
            if(event.getCet()== ConnectionEvent.ConnectionEventType.CONNECTION_ADD){
                channels.add(event.getChc());
            }else if(event.getCet() == ConnectionEvent.ConnectionEventType.CONNECTION_REMOVE){
                channels.remove(event.getChc());
                event.getChc().close();
            }else if(event.getCet() == ConnectionEvent.ConnectionEventType.MESSAGE_TRANSFER){

                Entity.BaseEntity msg=event.getMessage();
                Entity.Message message=msg.getExtension(Entity.message);
                String to=message.getTo();
                String from=message.getFrom();
                String content=message.getMessage();
                String messageId=message.getMessageId();
                ChannelHandlerContext chc=event.getChc();
                //--------------------------------------------------------
                Entity.Message.Builder reply= Entity.Message.newBuilder();
                reply.setFrom("0");
                reply.setTo(to);
                reply.setMessageId(messageId);
                Entity.BaseEntity.Builder builder2 = Entity.BaseEntity.newBuilder();
                builder2.setType(Entity.Type.MESSAGE);
                //--------------------------------------------------------
                GroupMessage groupMessage=JSON.parseObject(content, GroupMessage.class);
                //2代表群发消息
                if(groupMessage.getType().equals(SendMessageEnum.SENDMESSAGE_TYPE.getType())){
                    if("0".equals(groupMessage.getMessageType())){
                        try {
                            int count = saveMessageDao.saveMessage(groupMessage);
                            if(count ==1){
                                reply.setMessage("save message sucess");
                                logger.info("save message sucess");
                            }else{
                                reply.setMessage("save message error...");
                            }
                            builder2.setExtension(Entity.message,reply.build());
                            chc.writeAndFlush(builder2.build());
                        } catch (Exception e) {
                            logger.error("save message error",e);
                            reply.setMessage("fail");
                            builder2.setExtension(Entity.message,reply.build());
                            chc.writeAndFlush(builder2.build());
                        }
                    }

                }



            }
        }
    }

    //公告类推送
    public void publish(String nodeId,String message,String messageType) throws Exception{
        GroupMessage groupMessage=new GroupMessage();
        groupMessage.setNodeid(Integer.parseInt(nodeId));
        groupMessage.setMessage(message);
        groupMessage.setMessageType(messageType);
        groupMessage.setType(SendMessageEnum.SENDMESSAGE_TYPE.getType());
        try {
            int count = saveMessageDao.saveMessage(groupMessage);
            if(count ==1){
                logger.info("消息保存成功,正在推送中...");
                broadCast(JSON.toJSONString(groupMessage));
            }
        } catch (Exception e) {
            logger.error("broadCast error!",e);
            throw e;
        }
    }

    //注册用户
    public boolean registUser(String uid,String password) throws Exception{
        Registration registration = new Registration();
        registration.setUsername(uid);
        registration.setPassword(password);

        try {
            int count = userDao.createNewUser(registration);
            if (count ==1){
                logger.info("regist User success! uid-->"+uid);
                return true;
            }else{
                logger.error("regist User error。uid-->"+uid);
                return false;
            }
        } catch (Exception e) {
            logger.error("网络异常,注册用户失败",e);
            return false;
        }
    }

    //创建节点
    public boolean createNode(String nodeName,String description,String nodeType) throws Exception {
        NodeBean nodeBean =new NodeBean();
        nodeBean.setNodeName(nodeName);
        nodeBean.setDescription(description);
        nodeBean.setNodeType(nodeType);

        try {
            int count = nodeDao.createNode(nodeBean);
            if (count ==1){
                logger.info("create node success! nodename---->"+nodeName);
                return true;
            }else{
                logger.error("create node error! nodename---->"+nodeName);
                return false;
            }
        } catch (Exception e) {
            logger.error("网络异常,创建节点失败",e);
            return false;
        }
    }

    //删除节点
    public boolean deleteNode(String nodeName) throws Exception{
        NodeBean nodeBean =new NodeBean();
        nodeBean.setNodeName(nodeName);
        try {
            int count = nodeDao.deleteNodeByName(nodeBean);
            if(count==1){
                logger.info("delete node success! nodeName-->"+nodeName);
                return true;
            }else{
                logger.error("delete node error! nodename---->"+nodeName);
                return false;
            }
        } catch (Exception e) {
            logger.error("网络异常,删除节点失败",e);
            return false;
        }

    }


    public void  broadCast(String message) throws Exception{
        Entity.Message.Builder msgBuilder=Entity.Message.newBuilder();
        msgBuilder.setMessage(message);
        msgBuilder.setFrom("0");
        msgBuilder.setTo("0");
        msgBuilder.setCreateAt(System.currentTimeMillis());
        Entity.BaseEntity.Builder builder=Entity.BaseEntity.newBuilder();
        builder.setType(Entity.Type.MESSAGE);
        builder.setExtension(Entity.message,msgBuilder.build());
        Entity.BaseEntity baseEntity=builder.build();
        Iterator<ChannelHandlerContext> iter=channels.iterator();
        while(iter.hasNext()){
            try {
                ChannelHandlerContext chc = iter.next();
                chc.writeAndFlush(baseEntity);
            }catch (Exception e){
                logger.error("broad cast error",e);
                throw new RuntimeException("broad cast error",e);
            }
        }
    }

    public SaveMessageDao getSaveMessageDao() {
        return saveMessageDao;
    }

    public void setSaveMessageDao(SaveMessageDao saveMessageDao) {
        this.saveMessageDao = saveMessageDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public NodeDao getNodeDao() {
        return nodeDao;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }


    public static void main(String[] args) throws Exception{
        PushServer pushServer=new PushServer();
        pushServer.start();
        Thread.currentThread().sleep(5000);
        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setNodeid(50);
        groupMessage.setMessage("netty测试发送公告");
        groupMessage.setMessageType("0");
        pushServer.broadCast(JSON.toJSONString(groupMessage));
    }
}
