package push;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.message.Entity;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SecurePushServer {
    private static Logger logger= LoggerFactory.getLogger(SecurePushServer.class);
    static int port;
    public EventLoopGroup bossGroup;
    public EventLoopGroup workerGroup;
    public static final Map<Long,ChannelHandlerContext> channels=new ConcurrentHashMap<Long, ChannelHandlerContext>();

    public SecurePushServer(int port){
        this.port=port;
    }
    public void start() throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .build();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new SecurePushServerInitializer(sslCtx));

            b.bind(port).sync().channel().closeFuture().sync();
        }catch (Exception e){
            logger.error("secure push server start error",e);
        }
    }
    public void stop(){
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }catch (Exception e){

        }
    }
    public void broadCast(String message){

        Entity.Message.Builder msgBuilder=Entity.Message.newBuilder();
        msgBuilder.setMessage(message);
        msgBuilder.setFrom(0);
        msgBuilder.setTo(0);
        Entity.BaseEntity.Builder builder=Entity.BaseEntity.newBuilder();
        builder.setType(Entity.Type.MESSAGE);
        builder.setExtension(Entity.message,msgBuilder.build());
        Iterator<Map.Entry<Long,ChannelHandlerContext>> iter=channels.entrySet().iterator();
        while(iter.hasNext()){
            ChannelHandlerContext chc=iter.next().getValue();
            chc.writeAndFlush(msgBuilder.build());
        }
    }
    public static void main(String[] args) throws Exception {
//        SelfSignedCertificate ssc = new SelfSignedCertificate();
//        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
//            .build();
//
//        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//        try {
//            ServerBootstrap b = new ServerBootstrap();
//            b.group(bossGroup, workerGroup)
//             .channel(NioServerSocketChannel.class)
//             .handler(new LoggingHandler(LogLevel.INFO))
//             .childHandler(new SecurePushServerInitializer(sslCtx));
//
//            b.bind(PORT).sync().channel().closeFuture().sync();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
    }
}
