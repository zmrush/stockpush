package push.bottom;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.bottom.message.SubscribeBean;
import push.io.MessageEvent;
import push.io.MessageListener;
import push.io.SecurePushClient;
import push.model.message.Entity;
import push.model.message.SendMessageEnum;

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
            logger.error("error",e);
            throw new Exception(e.toString());
        }finally {
            writeLock.unlock();
            lockMap.remove(messageId);
            conditionMap.remove(messageId);
            messageMap.remove(messageId);
        }
    }

    public boolean subscribeNode(String uid,String nodeId) throws Exception{
        SubscribeBean subscribeBean =new SubscribeBean();
        subscribeBean.setUid(uid);
        subscribeBean.setNodeId(Integer.parseInt(nodeId));
        subscribeBean.setType(SendMessageEnum.SUBSCRIBE_TYPE.getType());
        try {
            this.sendDataSync(JSON.toJSONString(subscribeBean),UUID.randomUUID().toString());
        } catch (Exception e) {
            logger.error("subscribe node error");
            return false;
        }
        return true;
    }

    public boolean unSubscribe(String uid,String nodeId) throws Exception{
        SubscribeBean subscribeBean =new SubscribeBean();
        subscribeBean.setUid(uid);
        subscribeBean.setNodeId(Integer.parseInt(nodeId));
        subscribeBean.setType(SendMessageEnum.UNSUBSCRIBE_TYPE.getType());
        try {
            this.sendDataSync(JSON.toJSONString(subscribeBean),UUID.randomUUID().toString());
        } catch (Exception e) {
            logger.error("unSubscribe node error");
            return false;
        }
        return true;
    }

    /**
     * 测试多用户登录
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        String uid ="lizheng";
        for(int i=1;i<=1;i++) {
            PushClient pushClient = new PushClient("10.10.104.84", 9988,uid+String.valueOf(i), "123456");
            //PushClient pushClient = new PushClient("10.100.138.174", 9988, String.valueOf(i), "123456");
        }

        Thread.currentThread().sleep(10000000);
    }

}
