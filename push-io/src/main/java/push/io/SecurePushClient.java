package push.io;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.message.Entity;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SecurePushClient {
    private static Logger logger= LoggerFactory.getLogger(SecurePushClient.class);
    public String host;
    public int port;
    public Channel ch;
    private String uid;
    private String password;
    EventLoopGroup group;
    Bootstrap b;
    ScheduledFuture sf;
    ScheduledExecutorService ses;
    SecurePushClientInitializer spci;
    public SecurePushClient(String host, int port, ScheduledExecutorService ses,String uid,String password){
        this.host=host;
        this.port=port;
        this.ses=ses;
        this.uid=uid;
        this.password=password;
    }
    public void addListener(MessageListener messageListener){
        spci.addListener(messageListener);
    }
    public void start(){
        try {
            final SslContext sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            spci = new SecurePushClientInitializer(sslCtx, this);
        }catch (Exception e){
            logger.error("start error",e);
            throw new RuntimeException("client start error",e);
        }
        sf=ses.scheduleAtFixedRate(new ConnectionTask(this),0,10000, TimeUnit.MILLISECONDS);
    }
    public class ConnectionTask implements Runnable{
        public SecurePushClient spc;
        public ConnectionTask(SecurePushClient spc){
            this.spc=spc;
        }
        public void run(){
            try{
                if(ch==null || ch.isActive()==false){
                    logger.info("start to connect");
                    connect();
                }
            }catch (InterruptedException ie){
                logger.error("connect task interrupt",ie);
                return;
            }
            catch (Exception e){
                logger.error("connect task error",e);
            }
        }
        public void connect() throws Exception{
            if(b==null) {

                group = new NioEventLoopGroup();
                b = new Bootstrap();
                b.group(group).channel(NioSocketChannel.class).handler(spci);
                // Start the connection attempt.
            }
            ch = b.connect(host, port).sync().channel();
            login();
        }
    }
    public void sendData(String from,String to,String message) throws Exception{
        Entity.BaseEntity.Builder builder2 = Entity.BaseEntity.newBuilder();
        builder2.setType(Entity.Type.MESSAGE);
        Entity.Message.Builder msgBuilder = Entity.Message.newBuilder();
        msgBuilder.setFrom(from);
        msgBuilder.setTo(to);
        msgBuilder.setMessage(message);
        builder2.setExtension(Entity.message,msgBuilder.build());
        ChannelFuture f = ch.writeAndFlush(builder2.build());
        if (f != null)
            f.sync();
    }
    public void login() throws Exception{
        Entity.Login.Builder login = Entity.Login.newBuilder();
        login.setUid(uid);
        login.setAuthToken(password);
        Entity.BaseEntity.Builder builder = Entity.BaseEntity.newBuilder();
        builder.setType(Entity.Type.LOGIN);
        builder.setExtension(Entity.login, login.build());
        ChannelFuture f = ch.writeAndFlush(builder.build());
        if (f != null)
            f.sync();
    }
    public void stop(){
        sf.cancel(true);
        if(group!=null)
            group.shutdownGracefully();
    }
    public static void main(String[] args) throws Exception {
//        final String HOST = System.getProperty("host", "127.0.0.1");
//        final int PORT = Integer.parseInt(System.getProperty("port", "8992"));
//        // Configure SSL.
//        final SslContext sslCtx = SslContextBuilder.forClient()
//            .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//
//        EventLoopGroup group = new NioEventLoopGroup();
//        try {
//            Bootstrap b = new Bootstrap();
//            b.group(group)
//             .channel(NioSocketChannel.class)
//             .handler(new SecurePushClientInitializer(sslCtx,));
//
//            // Start the connection attempt.
//            Channel ch = b.connect(HOST, PORT).sync().channel();
//            Entity.Login.Builder login=Entity.Login.newBuilder();
//            login.setUid(213);
//            Entity.BaseEntity.Builder builder=Entity.BaseEntity.newBuilder();
//            builder.setType(Entity.Type.LOGIN);
//            builder.setExtension(Entity.login,login.build());
//            ChannelFuture f=ch.writeAndFlush(builder.build());
//            try {
//                if (f != null)
//                    f.sync();
//            }catch (Throwable throwable){
//                throwable.printStackTrace();
//                throw new RuntimeException("login error",throwable);
//            }
//            // Read commands from the stdin.
//            ChannelFuture lastWriteFuture = null;
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            for (;;) {
//                String line = in.readLine();
//                if (line == null) {
//                    break;
//                }
//
//                String[] lines=line.split("<");
//                if(lines.length==2) {
//                    Entity.BaseEntity.Builder builder2 = Entity.BaseEntity.newBuilder();
//                    builder2.setType(Entity.Type.MESSAGE);
//                    Entity.Message.Builder msgBuilder = Entity.Message.newBuilder();
//                    msgBuilder.setFrom(213);
//                    msgBuilder.setTo(Long.valueOf(lines[0]));
//                    msgBuilder.setMessage(lines[1]);
//                    builder2.setExtension(Entity.message,msgBuilder.build());
//                    lastWriteFuture = ch.writeAndFlush(builder2.build());
//                }else{
//                    if ("bye".equals(line.toLowerCase())) {
//                        Entity.BaseEntity.Builder builder2=Entity.BaseEntity.newBuilder();
//                        builder2.setType(Entity.Type.LOGOUT);
//                        Entity.Logout.Builder msgBuilder=Entity.Logout.newBuilder();
//                        msgBuilder.setUid(213);
//                        builder2.setExtension(Entity.logout,msgBuilder.build());
//                        ch.writeAndFlush(builder2.build());
//                        ch.closeFuture().sync();
//                        break;
//                    }
//                }
//            }
//
//            // Wait until all messages are flushed before closing the channel.
//            if (lastWriteFuture != null) {
//                lastWriteFuture.sync();
//            }
//        } finally {
//            // The connection is closed automatically on shutdown.
//            group.shutdownGracefully();
//        }
    }
}
