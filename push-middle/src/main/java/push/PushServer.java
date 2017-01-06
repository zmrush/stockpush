package push;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.message.Entity;
import push.util.PathUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mingzhu7 on 2017/1/5.
 */
public class PushServer {
    private static Logger logger= LoggerFactory.getLogger(PushServer.class);
    private static SecurePushServer sps;
    static {
        try {
            InputStream is=PushClient.class.getClassLoader().getResourceAsStream("push.properties");
            Properties properties=new Properties();
            properties.load(is);
            String url=properties.getProperty("url");
            int port=Integer.valueOf(properties.getProperty("port"));
            sps=new SecurePushServer(port);
            sps.start();
            String ip = NetUtil.getLocalIP();
            if (ip == null) {
                throw new Exception("Fail to get ip of the server!");
            }
            String nodeName = ip + "_" + port;

            ZKRegistry zkRegistry = new ZKRegistry(URL.valueOf(url));
            zkRegistry.open();
            zkRegistry.createLive(PathUtil.makePath("/push/test", "live", nodeName), null);
        }catch (Exception e){
            logger.error("push server initialize error",e);
        }
    }

    public static void  broadCast(String message){
        Entity.Message.Builder msgBuilder=Entity.Message.newBuilder();
        msgBuilder.setMessage(message);
        msgBuilder.setFrom(0);
        msgBuilder.setTo(0);
        Entity.BaseEntity.Builder builder=Entity.BaseEntity.newBuilder();
        builder.setType(Entity.Type.MESSAGE);
        builder.setExtension(Entity.message,msgBuilder.build());
        Iterator<Map.Entry<Long,ChannelHandlerContext>> iter=channels.entrySet().iterator();
        while(iter.hasNext()){
            try {
                ChannelHandlerContext chc = iter.next().getValue();
                chc.writeAndFlush(builder.build());
            }catch (Exception e){
                logger.error("broad cast error",e);
            }
        }
    }
    public static final Map<Long,ChannelHandlerContext> channels=new ConcurrentHashMap<Long, ChannelHandlerContext>();
    public static class DefaultConnectionListener implements ConnectionListener{

        public void onEvent(ConnectionEvent event) {
            if(event.getCet()== ConnectionEvent.ConnectionEventType.CONNECTION_ADD){
                channels.put(event.getUid(),event.getChc());
            }else if(event.getCet() == ConnectionEvent.ConnectionEventType.CONNECTION_REMOVE){
                if(event.getUid()!=null) {
                    channels.remove(event.getUid());
                }else{
                    Iterator<Map.Entry<Long,ChannelHandlerContext>> iter=channels.entrySet().iterator();
                    while(iter.hasNext()){
                        Map.Entry<Long,ChannelHandlerContext> tmp=iter.next();
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
                long to=message.getTo();
                ChannelHandlerContext toContext=channels.get(to);
                if(toContext!=null)
                    toContext.writeAndFlush(msg);
            }
        }
    }
    public static void main(String[] args) throws Exception{
        Thread.currentThread().sleep(10000);
        for(;;) {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String ss=br.readLine();
            broadCast(ss);
        }
    }
}
