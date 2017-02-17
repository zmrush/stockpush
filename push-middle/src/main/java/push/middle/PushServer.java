package push.middle;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.io.ConnectionEvent;
import push.io.ConnectionListener;
import push.io.SecurePushServer;
import push.registry.NetUtil;
import push.registry.URL;
import push.registry.util.PathUtil;
import push.registry.zookeeper.ZKRegistry;
import push.message.Entity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by mingzhu7 on 2017/1/5.
 */
public class PushServer {
    private static Logger logger= LoggerFactory.getLogger(PushServer.class);
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
    public final CopyOnWriteArraySet<ChannelHandlerContext> channels=new CopyOnWriteArraySet<ChannelHandlerContext>();
    public class DefaultConnectionListener implements ConnectionListener {
        public void onEvent(ConnectionEvent event) {
            if(event.getCet()== ConnectionEvent.ConnectionEventType.CONNECTION_ADD){
                channels.add(event.getChc());
            }else if(event.getCet() == ConnectionEvent.ConnectionEventType.CONNECTION_REMOVE){
                channels.remove(event.getChc());
                event.getChc().close();
            }else if(event.getCet() == ConnectionEvent.ConnectionEventType.MESSAGE_TRANSFER){
            }
        }
    }
    public static void main(String[] args) throws Exception{
        PushServer pushServer=new PushServer();
        pushServer.start();
        Thread.currentThread().sleep(10000);
        for(;;) {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String ss=br.readLine();
            pushServer.broadCast(ss);
        }
    }
}
