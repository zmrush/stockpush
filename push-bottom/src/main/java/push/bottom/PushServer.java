package push.bottom;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import push.bottom.dao.SubscribeDao;
import push.bottom.dao.UserDao;
import push.bottom.message.SubscribeBean;
import push.bottom.message.User;
import push.io.*;
import push.middle.PushClient;
import push.middle.dao.NodeDao;
import push.middle.pojo.NodeBean;
import push.model.message.AbstractMessage;
import push.model.message.Entity;
import push.model.message.GroupMessage;
import push.model.message.SendMessageEnum;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by mingzhu7 on 2017/1/19.
 */
public class PushServer {
    private static Logger logger= LoggerFactory.getLogger(PushServer.class);

    push.middle.PushClient pushClient;
    //-----------------------------------------------------------------------
    //账户数据库操作
    private UserDao userDao;
    private SubscribeDao subscribeDao;
    private NodeDao nodeDao;

    public NodeDao getNodeDao() {
        return nodeDao;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

    public SubscribeDao getSubscribeDao() {
        return subscribeDao;
    }

    public void setSubscribeDao(SubscribeDao subscribeDao) {
        this.subscribeDao = subscribeDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
    //------------------------------------------------------------------------

    int bufferSize = 1024;
    public class ConnectionEventFactory implements EventFactory<ConnectionEvent> {
        public ConnectionEvent newInstance() {
            return new ConnectionEvent();
        }
    }
    public class ConnectionEventProducer {
        private final RingBuffer<ConnectionEvent> ringBuffer;

        public ConnectionEventProducer(RingBuffer<ConnectionEvent> ringBuffer) {
            this.ringBuffer = ringBuffer;
        }

        public void onData(ConnectionEvent connectionEvent) {
            long sequence = ringBuffer.next();  // 获取下一个序列号
            try {
                ConnectionEvent event = ringBuffer.get(sequence); // 根据序列号获取预分配的数据槽
                event.setMessage(connectionEvent.getMessage());
                event.setCet(connectionEvent.getCet());
                event.setChc(connectionEvent.getChc());
                event.setUid(connectionEvent.getUid());
            } finally {
                ringBuffer.publish(sequence);
            }
        }
    }
    ConnectionEventFactory factory=new ConnectionEventFactory();
    Disruptor<ConnectionEvent> clientDisruptor;
    private ConnectionEventProducer connectionEventProducer;
    //------------------------------------------------------------------------
//    int bufferSize = 1024;
    public class MessageEventFactory implements EventFactory<MessageEvent> {

        public MessageEvent newInstance() {
            return new MessageEvent();
        }
    }

    public class MessageEventProducer {
        private final RingBuffer<MessageEvent> ringBuffer;

        public MessageEventProducer(RingBuffer<MessageEvent> ringBuffer) {
            this.ringBuffer = ringBuffer;
        }

        public void onData(MessageEvent messageEvent) {
            long sequence = ringBuffer.next();  // 获取下一个序列号
            try {
                MessageEvent event = ringBuffer.get(sequence); // 根据序列号获取预分配的数据槽
                event.setMessage(messageEvent.getMessage());
                event.setMessageEventType(messageEvent.getMessageEventType());
            } finally {
                ringBuffer.publish(sequence);
            }
        }
    }
    MessageEventFactory messageFactory=new MessageEventFactory();
    Disruptor<MessageEvent> middleDisruptor;
    private MessageEventProducer messageEventProducer;
    //------------------------------------------------------------------------
    //管理员账号
    private Set<String> administrators=new HashSet<String>();
    //没有登录的账号，我们也会定期清理的
    private Map<ChannelHandlerContext,Long> inactiveChannel=new ConcurrentHashMap<ChannelHandlerContext, Long>();
    private ScheduledExecutorService cleanSchedule=Executors.newScheduledThreadPool(1);

    //连接的客户端
    private ConcurrentHashMap<String,ChannelHandlerContext> clients=new ConcurrentHashMap<String, ChannelHandlerContext>();

    //按照客户端股票排到不同的通道里面去进行推送
    private ConcurrentHashMap<String,Set<ChannelHandlerContext>> codeClients=new ConcurrentHashMap<String, Set<ChannelHandlerContext>>();
    //-------------------------------------------------------------------------

    //负责监听客户端的连接
    public class BottomConnectionListener implements ConnectionListener {
        public void onEvent(ConnectionEvent event) {
            connectionEventProducer.onData(event);
        }
    }
    public class BottomEventHandler implements EventHandler<ConnectionEvent> {

        public void onEvent(ConnectionEvent event, long l, boolean b) throws Exception {
            if(event.getCet()== ConnectionEvent.ConnectionEventType.CONNECTION_TRANSIENT){
                logger.info("put inactive channel");
                inactiveChannel.put(event.getChc(),System.currentTimeMillis());
            }else if(event.getCet()== ConnectionEvent.ConnectionEventType.CONNECTION_ADD){
                logger.info("remove inavtive channel size:"+inactiveChannel.size());
                inactiveChannel.remove(event.getChc());
                Entity.Login login=event.getMessage().getExtension(Entity.login);
                String username=login.getUid();
                String password=login.getAuthToken();
                try {
                    User user=userDao.findByUserInfo(username,password);
                    if(user == null){
                        logger.info("user login fail,username:{},password:{}",username,password);
                        event.getChc().close();
                        return;
                    }
                    clients.put(event.getUid(),event.getChc());
                    List<NodeBean> nodeList = subscribeDao.querySubscribeNodeListByUid(username);
                    for(int i=0;i<nodeList.size();i++){
                        Set<ChannelHandlerContext> sets = codeClients.get(String.valueOf(nodeList.get(i).getNodeId()));
                        if(sets==null){
                            //如果该节点的set不存在，则新建一个。
                            CopyOnWriteArraySet<ChannelHandlerContext> codeSet=new CopyOnWriteArraySet<ChannelHandlerContext>();
                            codeSet.add(event.getChc());
                            Object isPut=codeClients.putIfAbsent(String.valueOf(nodeList.get(i).getNodeId()),codeSet);
                            if(isPut!=null){
                                codeClients.get(nodeList.get(i).toString()).add(event.getChc());
                            }
                        }else{
                            //如果该节点的set存在，那么把这个用户添加进去
                            sets.add(event.getChc());
                        }
                    }
                }catch (Exception e){
                    event.getChc().close();
                }
            }else if(event.getCet() == ConnectionEvent.ConnectionEventType.CONNECTION_REMOVE){
                if(event.getUid()!=null) {
                    clients.remove(event.getUid());
                }else{
                    Iterator<Map.Entry<String,ChannelHandlerContext>> iter=clients.entrySet().iterator();
                    while(iter.hasNext()){
                        Map.Entry<String,ChannelHandlerContext> tmp=iter.next();
                        if(tmp.getValue()==event.getChc()) {
                            iter.remove();
                            break;
                        }
                    }
                }
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
                AbstractMessage abstractMessage=JSON.parseObject(content, AbstractMessage.class);
                //没有登录或者不是本人发的消息不能够进行处理
                if(clients.get(from)!=event.getChc())
                    return;
                //1:注册用户。2:群发消息。3:创建节点。4:删除节点。5：订阅节点。6：反订阅节点
                if(abstractMessage.getType().equals("2")){
                    GroupMessage groupMessage=JSONObject.parseObject(content,GroupMessage.class);
                    Set<ChannelHandlerContext> clients=codeClients.get(groupMessage.getNodeid());
                    for(ChannelHandlerContext client:clients){
                        client.writeAndFlush(groupMessage.getMessage());
                    }
                    return;
                }else if(abstractMessage.getType().equals(SendMessageEnum.SUBSCRIBE_TYPE.getType())){
                    //只有管理员或者用户登录才能订阅节点
                    SubscribeBean subscribeBean =JSON.parseObject(content,SubscribeBean.class);
                    if(!subscribeBean.getUid().equals(from) && !administrators.contains(from))
                        return;
                    try {
                        int count = subscribeDao.subscribeNode(subscribeBean);
                        if(count==1){
                            //把该用户添加到codeClients里面
                            Set<ChannelHandlerContext> sets = codeClients.get(subscribeBean.getNodeId());
                            if(sets==null){
                                //如果该节点的set不存在，则新建一个。
                                CopyOnWriteArraySet<ChannelHandlerContext> codeSet=new CopyOnWriteArraySet<ChannelHandlerContext>();
                                codeSet.add(event.getChc());
                                Object isPut=codeClients.putIfAbsent(String.valueOf(subscribeBean.getNodeId()),codeSet);
                                if(isPut!=null){
                                    codeClients.get(String.valueOf(subscribeBean.getNodeId())).add(event.getChc());
                                }
                            }else{
                                //如果该节点的set存在，那么把这个用户添加进去
                                sets.add(event.getChc());
                            }

                            reply.setMessage("subscribe node sucess");
                            logger.info("subscribe node sucess");
                            builder2.setExtension(Entity.message,reply.build());
                            chc.writeAndFlush(builder2.build());

                        }
                    } catch (Exception e) {
                        logger.error("subscribe node error",e);
                    }
                }else if(abstractMessage.getType().equals(SendMessageEnum.UNSUBSCRIBE_TYPE.getType())){
                    //只有管理员或者用户登录才能反订阅节点
                    SubscribeBean subscribeBean =JSON.parseObject(content,SubscribeBean.class);
                    if(!subscribeBean.getUid().equals(from) && !administrators.contains(from)) {
                        return;
                    }
                    try {
                        int count = subscribeDao.unSubscribe(subscribeBean);
                        if(count==1){
                            Set<ChannelHandlerContext> sets = codeClients.get(subscribeBean.getNodeId());
                            sets.remove(event.getChc());
                            reply.setMessage("unSubscribe node sucess");
                            logger.info("unSubscribe node sucess");
                            builder2.setExtension(Entity.message,reply.build());
                            chc.writeAndFlush(builder2.build());
                        }
                    } catch (Exception e) {
                        logger.error("unSubscribe node error",e);
                    }
                }else if(abstractMessage.getType().equals(SendMessageEnum.SENDMESSAGE_TYPE.getType())){
                    //群发消息
                    if(!administrators.contains(from)){
                        return;
                    }

                    GroupMessage groupMessage=JSON.parseObject(content, GroupMessage.class);
                    reply.setMessage(groupMessage.getMessage());
                    builder2.setExtension(Entity.message,reply.build());
                    Set<ChannelHandlerContext> sets = codeClients.get(groupMessage.getNodeid());
                    Iterator<ChannelHandlerContext> iter=sets.iterator();
                    while (iter.hasNext()){
                        try {
                            ChannelHandlerContext singleChannel = iter.next();
                            singleChannel.writeAndFlush(builder2.build());
                        }catch (Exception e){
                            logger.error("push-bottom broad cast error",e);
                            throw new RuntimeException("push-bottom broad cast error",e);
                        }
                    }

                }


            }
        }
    }
    //---------------------------------------------------------------------------------------

    //负责监听服务端传送过来的数据
    public class BottomMessageLitener implements MessageListener {

        public void onEvent(MessageEvent event) {
            messageEventProducer.onData(event);
        }
    }
    public class MiddleEventHandler implements EventHandler<MessageEvent> {
        public void onEvent(MessageEvent messageEvent, long l, boolean b) throws Exception {
            Entity.Message message=messageEvent.getMessage().getExtension(Entity.message);
            if(JSON.parseObject(message.getMessage(),AbstractMessage.class).getType().equals("2")){
                GroupMessage groupMessage=JSON.parseObject(message.getMessage(),GroupMessage.class);
                String nodeId=String.valueOf(groupMessage.getNodeid());
                Set<ChannelHandlerContext> clients=codeClients.get(nodeId);

                Iterator<ChannelHandlerContext> iter=clients.iterator();
                while(iter.hasNext()){
                    iter.next().writeAndFlush(messageEvent.getMessage());

                }
            }
//            Iterator<ConcurrentHashMap.Entry<String,ChannelHandlerContext>> iter=clients.entrySet().iterator();
//            while(iter.hasNext()){
//                iter.next().getValue().writeAndFlush(messageEvent.getMessage());
//            }
        }
    }
    //----------------------------------------------------------------------------
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    public class CleanRunnable implements Runnable{

        @Override
        public void run() {
            Long now=System.currentTimeMillis();
            Iterator<Map.Entry<ChannelHandlerContext,Long>> iter=inactiveChannel.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry<ChannelHandlerContext,Long> tmp=iter.next();
                ChannelHandlerContext chc=tmp.getKey();
                Long start=tmp.getValue();
                //超过三分钟没有登录清理掉连接
                if(now>(start+3*60*1000)){
                    chc.close();
                    iter.remove();
                    logger.info("remove long-no-login channel");
                }
            }

        }
    }
    public PushServer(int port){
        this.port=port;
        //------------------------------------------------------------
        String[] adm={"lizheng1"};
        administrators.addAll(Arrays.asList(adm));
        //------------------------------------------------------------
        cleanSchedule.scheduleAtFixedRate(new CleanRunnable(),0,5*60, TimeUnit.SECONDS);
        //------------------------------------------------------------
        //启动客户端消息的队列
        clientDisruptor = new Disruptor<ConnectionEvent>(factory,
                bufferSize, Executors.newFixedThreadPool(2),
                ProducerType.SINGLE,
                new YieldingWaitStrategy());
        clientDisruptor.handleEventsWith(new BottomEventHandler());
        clientDisruptor.start();
        RingBuffer<ConnectionEvent> ringBuffer = clientDisruptor.getRingBuffer();
        connectionEventProducer = new ConnectionEventProducer(ringBuffer);
        //--------------------------------------------------------------
        middleDisruptor= new Disruptor<MessageEvent>(messageFactory,
                bufferSize, Executors.newFixedThreadPool(2),
                ProducerType.SINGLE,
                new YieldingWaitStrategy());
        middleDisruptor.handleEventsWith(new MiddleEventHandler());
        middleDisruptor.start();
        RingBuffer<MessageEvent> middleRingbuffer=middleDisruptor.getRingBuffer();
        messageEventProducer=new MessageEventProducer(middleRingbuffer);
        //--------------------------------------------------------------
        pushClient=new PushClient();


    }
    public void start() throws Exception{
        try {
            //监听客户端
            SecurePushServer securePushServer = new SecurePushServer(port);
            securePushServer.start();
            securePushServer.addListener(new BottomConnectionListener());
            //监听上层middle发过来的消息
            pushClient.start();
            pushClient.addMesageListener(new BottomMessageLitener());
        }catch (Exception e){
            logger.error("push server start error",e);
            throw new RuntimeException("push server start error",e);
        }
    }

    public static void main(String[] args){
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring-*.xml");
        ctx.registerShutdownHook();
        logger.info("Push Server started");
        logger.info("java.library.path=" + System.getProperty("java.library.path"));
    }

}
