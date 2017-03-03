package push.bottom;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.bottom.message.NodeBean;
import push.bottom.message.Registration;
import push.bottom.message.SubscribeBean;
import push.bottom.model.SendMessageEnum;
import push.io.MessageEvent;
import push.io.MessageListener;
import push.io.SecurePushClient;
import push.message.Entity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mingzhu7 on 2017/1/19.
 */
public class PushClient {

    private static Logger logger= LoggerFactory.getLogger(PushClient.class);


    private SecurePushClient securePushClient;
    private String uid;
    private ScheduledExecutorService scheduledExecutorService= Executors.newScheduledThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread thread=new Thread(r,"connect");
            thread.setDaemon(true);
            return thread;
        }
    });

    public  class BottomClientMessageListener implements MessageListener{
        public void onEvent(MessageEvent event) {
            Entity.Message message=event.getMessage().getExtension(Entity.message);
            logger.info(System.currentTimeMillis()+":"+message.getCreateAt()+":"+message.getMessageId()+":"+message.getMessage());
            //------------------------------------------------------
            String messageId=message.getMessageId();
            ReentrantLock writeLock=lockMap.get(messageId);
            if(writeLock!=null){
                messageMap.put(messageId,message.getMessage());
                try {
                    writeLock.lock();
                    conditionMap.get(messageId).signalAll();
                }finally {
                    writeLock.unlock();
                }
            }
        }
    }
    public PushClient(String host,int port,String uid,String password){
        this.uid=uid;
        securePushClient=new SecurePushClient(host,port,scheduledExecutorService,uid,password);
        securePushClient.start();
        securePushClient.addListener(new BottomClientMessageListener());
    }
    public void sendData(String message) throws Exception{
        securePushClient.sendData(uid,"0",message);
    }
    private ConcurrentHashMap<String,ReentrantLock> lockMap=new ConcurrentHashMap<String, ReentrantLock>();
    private ConcurrentHashMap<String,Condition> conditionMap=new ConcurrentHashMap<String, Condition>();
    private ConcurrentHashMap<String,String> messageMap=new ConcurrentHashMap<String, String>();
    //private ReentrantLock writeLock=new ReentrantLock();
    public void sendDataSync(String message,String messageId) throws Exception{
        ReentrantLock writeLock=new ReentrantLock();
        try {
            lockMap.put(messageId, writeLock);
            Condition condition=writeLock.newCondition();
            conditionMap.put(messageId,condition);
            writeLock.lock();
            securePushClient.sendDataSync(uid, "0", message, messageId);
            //------------------------------------------------------------
            condition.await(3, TimeUnit.SECONDS);
            if (messageMap.get(messageId) != null) {
                //收到回复的消息
                logger.info("收到返回消息--->" + messageMap.get(messageId));
            } else {
                //超时
                logger.info("本次操作超时");
            }
        }catch (Exception e){
            logger.error("",e);
        }finally {
            writeLock.unlock();
            lockMap.remove(messageId);
            conditionMap.remove(messageId);
            messageMap.remove(messageId);
        }
    }

    public boolean registUser(String uid,String password) throws Exception{
        boolean registFlag ;
        Registration registration = new Registration();
        registration.setUsername(uid);
        registration.setPassword(password);
        registration.setType(SendMessageEnum.REGIST_TYPE.getType());
        try {
            this.sendDataSync(JSON.toJSONString(registration),UUID.randomUUID().toString());
            registFlag =true;
        } catch (Exception e) {
            registFlag =false;
            logger.error("regist user error!");
        }
        return registFlag;
    }

    public boolean createNode(String nodeName,String description,String nodeType) throws Exception {
        boolean createFlag;
        NodeBean nodeBean =new NodeBean();
        nodeBean.setNodeName(nodeName);
        nodeBean.setDescription(description);
        nodeBean.setNodeType(nodeType);
        nodeBean.setType(SendMessageEnum.CREATENODE_TYPE.getType());//创建节点
        try {
            this.sendDataSync(JSON.toJSONString(nodeBean),UUID.randomUUID().toString());
            createFlag = true;
        } catch (Exception e) {
            logger.error("create node error", e);
            createFlag = false;
        }
        return createFlag;
    }

    public boolean deleteNode(String nodeName) throws Exception{
        boolean deleteFlag;
        NodeBean nodeBean =new NodeBean();
        nodeBean.setNodeName(nodeName);
        nodeBean.setType(SendMessageEnum.DELATENODE_TYPE.getType());//删除节点
        try {
            this.sendDataSync(JSON.toJSONString(nodeBean),UUID.randomUUID().toString());
            deleteFlag = true;
        } catch (Exception e) {
            logger.error("delete node error", e);
            deleteFlag = false;
        }
        return deleteFlag;
    }

    public boolean subscribeNode(String uid,String nodeId) throws Exception{
        boolean subscribeFlag;
        SubscribeBean subscribeBean =new SubscribeBean();
        subscribeBean.setUid(uid);
        subscribeBean.setNodeId(Integer.parseInt(nodeId));
        subscribeBean.setType(SendMessageEnum.SUBSCRIBE_TYPE.getType());
        try {
            this.sendDataSync(JSON.toJSONString(subscribeBean),UUID.randomUUID().toString());
            subscribeFlag =true;
        } catch (Exception e) {
            logger.error("subscribe node error");
            subscribeFlag =false;
        }
        return subscribeFlag;
    }

    public boolean unSubscribe(String uid,String nodeId) throws Exception{
        boolean unSubscribeFlag;
        SubscribeBean subscribeBean =new SubscribeBean();
        subscribeBean.setUid(uid);
        subscribeBean.setNodeId(Integer.parseInt(nodeId));
        subscribeBean.setType(SendMessageEnum.UNSUBSCRIBE_TYPE.getType());
        try {
            this.sendDataSync(JSON.toJSONString(subscribeBean),UUID.randomUUID().toString());
            unSubscribeFlag =true;
        } catch (Exception e) {
            logger.error("unSubscribe node error");
            unSubscribeFlag =false;
        }
        return unSubscribeFlag;
    }

    /**
     * 测试多用户登录
     * @param args
     * @throws Exception
     */
//    public static void main(String[] args) throws Exception{
//        String uid ="lizheng";
//        for(int i=1;i<=10;i++) {
//            PushClient pushClient = new PushClient("10.10.104.84", 9988,uid+String.valueOf(i), "123456");
//            //PushClient pushClient = new PushClient("10.100.138.174", 9988, String.valueOf(i), "123456");
//        }
//
//        Thread.currentThread().sleep(10000000);
//    }

    public static void main(String[] args) throws Exception{
        //1:注册用户。2:群发消息。3:创建节点。4:删除节点。5：订阅节点。6：反订阅节点
        PushClient pushClient = new PushClient("10.10.104.84", 9988,"lizheng1", "123456");

        pushClient.createNode("lizhengTest","测试","0");
        /*for(;;) {
            BufferedReader bufferedInputStream=new BufferedReader(new InputStreamReader(System.in));
            String line = bufferedInputStream.readLine();
            String[] strList=line.split(" ");

            NodeBean nodeBean =new NodeBean();

//            create node Test
            nodeBean.setNodeName(strList[0]);
            nodeBean.setDescription(strList[1]);
            nodeBean.setNodeType(strList[2]);
            nodeBean.setType(strList[3]);

            //delete node Test
//            nodeBean.setNodeName(strList[0]);
//            nodeBean.setType(strList[1]);
//            pushClient.sendDataSync(JSON.toJSONString(nodeBean),UUID.randomUUID().toString());


            //subscribe node Test
//            SubscribeBean subscribeBean =new SubscribeBean();
//            subscribeBean.setUid(strList[0]);
//            subscribeBean.setNodeid(Integer.valueOf(strList[1]));
//            subscribeBean.setType(strList[2]);
//            pushClient.sendData(JSON.toJSONString(subscribeBean));
        }*/
    }


//    public static void main(String[] args) throws Exception{
//        PushClient pushClient=new PushClient("10.10.104.103", 9988, String.valueOf(0), "123456");
//        BufferedReader bufferedInputStream=new BufferedReader(new InputStreamReader(System.in));
//        String line;
//        while((line=bufferedInputStream.readLine())!=null){
//            Registration registration = new Registration();
//            registration.setType("1");
//            String[] strings=line.split(" ");
//            registration.setUsername(strings[0]);
//            registration.setPassword(strings[1]);
//            pushClient.sendData(JSON.toJSONString(registration));
//        }
//    }

}
