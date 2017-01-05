package push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.util.PathUtil;

import java.io.InputStream;
import java.util.Properties;

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
    public void broadCast(String message){
        sps.broadCast(message);
    }
}
