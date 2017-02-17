package push.bottom;

import com.alibaba.fastjson.JSON;
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
import push.bottom.dao.UserDao;
import push.bottom.message.Registration;
import push.bottom.model.User;
import push.io.*;
import push.message.AbstractMessage;
import push.message.Entity;
import push.middle.*;
import push.middle.PushClient;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by mingzhu7 on 2017/1/19.
 */
public class PushServer {
    private static Logger logger= LoggerFactory.getLogger(PushServer.class);

    push.middle.PushClient pushClient;
    //-----------------------------------------------------------------------
    //账户数据库操作
    private UserDao userDao;
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
            if(event.getCet()== ConnectionEvent.ConnectionEventType.CONNECTION_ADD){
                Entity.Login login=event.getMessage().getExtension(Entity.login);
                String username=login.getUid();
                String password=login.getAuthToken();
                try {
                    clients.put(event.getUid(),event.getChc());
//                    User user=userDao.findByUserInfo(username,password);
//                    if(user!=null)
//                        clients.put(event.getUid(),event.getChc());
//                    else
//                        event.getChc().close();
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
                String content=message.getMessage();
                AbstractMessage abstractMessage=JSON.parseObject(content, AbstractMessage.class);
                if(abstractMessage.getType().equals("1")){
                    Registration registration= JSON.parseObject(content, Registration.class);
                    try {
                        userDao.createNewUser(registration);
                    }catch (Exception e){
                        logger.error("create user error",e);
                    }
                }
//                ChannelHandlerContext toContext=clients.get(to);
//                if(toContext!=null)
//                    toContext.writeAndFlush(msg);
            }
        }
    }
    //---------------------------------------------------------------------------------------

    //负责监听服务端传送过来的数据
    public class BottomMessageLitener implements MessageListener{

        public void onEvent(MessageEvent event) {
            messageEventProducer.onData(event);
        }
    }
    public class MiddleEventHandler implements EventHandler<MessageEvent> {
        public void onEvent(MessageEvent messageEvent, long l, boolean b) throws Exception {
            //Entity.Message message=event.getMessage().getExtension(Entity.message);
            //System.out.println(message.getMessage());
            Iterator<ConcurrentHashMap.Entry<String,ChannelHandlerContext>> iter=clients.entrySet().iterator();
            while(iter.hasNext()){
                iter.next().getValue().writeAndFlush(messageEvent.getMessage());
            }
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

    public PushServer(int port){
        this.port=port;
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

//    public static void main(String[] args) throws Exception{
//        PushServer pushServer=new PushServer(9988);
//        pushServer.start();
//        Thread.currentThread().sleep(10000000);
//    }
    public static void main(String[] args){
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring-*.xml");
        ctx.registerShutdownHook();
        logger.info("Push Server started");
        logger.info("java.library.path=" + System.getProperty("java.library.path"));
    }

}
