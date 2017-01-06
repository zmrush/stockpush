package push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.listener.ChildrenEvent;
import push.listener.ChildrenListener;
import push.message.Entity;
import push.util.PathUtil;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mingzhu7 on 2017/1/4.
 */
public class PushClient {
    private static Logger logger= LoggerFactory.getLogger(PushClient.class);
    private static Long uid;
    static{
        try {
            InputStream is=PushClient.class.getClassLoader().getResourceAsStream("push.properties");
            Properties properties=new Properties();
            properties.load(is);
            String url=properties.getProperty("url");
            uid=Long.valueOf(properties.getProperty("uid"));
            String root=properties.getProperty("root");
            ZKRegistry zkRegistry = new ZKRegistry(URL.valueOf(url));
            zkRegistry.open();
            zkRegistry.addListener(PathUtil.makePath(root, "live"),new ExecutorLiveListener());
        }catch (Exception e){
            logger.error("push client initialize error",e);
        }
    }
    public static class ClientIndex{
        private String host;
        private int port;
        public ClientIndex(String host,int port){
            this.host=host;
            this.port=port;
        }
        @Override
        public int hashCode(){
            return this.host.hashCode();
        }
        @Override
        public boolean equals(Object spc){
            if(spc instanceof ClientIndex){
                ClientIndex tmp=(ClientIndex)spc;
                return tmp.host.equals(this.host) && tmp.port==this.port;
            }
            return false;
        }
    }
    private static Map<ClientIndex,SecurePushClient> connectedClients=new HashMap<ClientIndex,SecurePushClient>();
    private static ReentrantLock mutex=new ReentrantLock();
    private static Set<String> lives=new HashSet<String>();
    public static ScheduledExecutorService ses=new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread thread=new Thread(r,"Connect");
            thread.setDaemon(true);
            return thread;
        }
    });
    public static class ExecutorLiveListener implements ChildrenListener {
        @Override
        public void onEvent(ChildrenEvent event) {
            // 当新开启了服务器或者服务器宕机了重新分配
            mutex.lock();
            try {
                String node = PathUtil.getNodeFromPath(event.getPath());
                if (event.getType() == ChildrenEvent.ChildrenEventType.CHILD_REMOVED) {
                    logger.error("remove server "+node);
                    String[] s=node.split("_");
                    ClientIndex ci=new ClientIndex(s[0],Integer.valueOf(s[1]));
                    if(connectedClients.get(ci)!=null){
                        connectedClients.get(ci).stop();
                        connectedClients.remove(ci);
                    }
                } else if (event.getType() == ChildrenEvent.ChildrenEventType.CHILD_CREATED) {
                    //lives.add(node);
                    logger.error("add server "+node);
                    String[] s=node.split("_");
                    ClientIndex ci=new ClientIndex(s[0],Integer.valueOf(s[1]));
                    if(connectedClients.get(ci)==null){
                        SecurePushClient spc=new SecurePushClient(s[0],Integer.valueOf(s[1]),ses,uid);
                        spc.addListener(new DefaultMessageListener());
                        connectedClients.put(ci,spc);
                        spc.start();

                    }
                }
            } finally {
                mutex.unlock();
            }
        }
    }
    public static class DefaultMessageListener implements MessageListener{

        public void onEvent(MessageEvent event) {
            if(event.getMessageEventType()== MessageEvent.MessageEventType.MESSAGE_RECEIVE){
                Entity.Message message=event.getMessage().getExtension(Entity.message);
                System.out.println(message.getFrom()+">"+message.getMessage());
            }
        }
    }
    public static void main(String[] args) throws Exception{
        Thread.currentThread().sleep(10000);
        System.out.println("start");
        Thread.currentThread().sleep(2000000000);

    }
}
