package push.bottom;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.bottom.dao.NodeDao;
import push.bottom.dao.UserDao;
import push.bottom.message.NodeBean;
import push.bottom.message.Registration;
import push.bottom.message.SubscribeBean;
import push.io.MessageEvent;
import push.io.MessageListener;
import push.io.SecurePushClient;
import push.message.Entity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

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
    public static class BottomClientMessageListener implements MessageListener{
        public void onEvent(MessageEvent event) {
            Entity.Message message=event.getMessage().getExtension(Entity.message);
            logger.info(System.currentTimeMillis()+":"+message.getCreateAt()+":"+message.getMessage());
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
        PushClient pushClient = new PushClient("10.10.104.84", 9988,"lizheng2", "123456");
        for(;;) {
            BufferedReader bufferedInputStream=new BufferedReader(new InputStreamReader(System.in));
            String line = bufferedInputStream.readLine();
            String[] strList=line.split(" ");

//            NodeBean nodeBean =new NodeBean();

            //create node Test
//            nodeBean.setNodename(strList[0]);
//            nodeBean.setDescription(strList[1]);
//            nodeBean.setNodetype(strList[2]);
//            nodeBean.setType(strList[3]);

            //delete node Test
//            nodeBean.setNodename(strList[0]);
//            nodeBean.setType(strList[1]);
//            pushClient.sendData(JSON.toJSONString(nodeBean));

            //subscribe node Test
            SubscribeBean subscribeBean =new SubscribeBean();
            subscribeBean.setUid(strList[0]);
            subscribeBean.setNodeid(Integer.valueOf(strList[1]));
            subscribeBean.setType(strList[2]);
            pushClient.sendData(JSON.toJSONString(subscribeBean));
        }
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
