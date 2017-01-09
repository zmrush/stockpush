package push;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SecurePushServer {
    private static Logger logger= LoggerFactory.getLogger(SecurePushServer.class);
    static int port;
    public EventLoopGroup bossGroup;
    public EventLoopGroup workerGroup;
    SecurePushServerInitializer spsi;

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
            spsi=new SecurePushServerInitializer(sslCtx);
            b.group(bossGroup, workerGroup)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(spsi);

            //b.bind(port).sync().channel().closeFuture().sync();
            b.bind(port).sync();
            //this.addListener(new DefaultConnectionListener());
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
    public void addListener(ConnectionListener connectionListener){
        spsi.addListener(connectionListener);
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
