package push.io;

import com.google.protobuf.ExtensionRegistry;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.registry.EventManager;
import push.model.message.Entity;

import javax.net.ssl.*;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class SecurePushServerInitializer extends ChannelInitializer<SocketChannel> {
    private static Logger logger= LoggerFactory.getLogger(SecurePushServerInitializer.class);
    private EventManager<ConnectionEvent> eventManager=new EventManager<ConnectionEvent>("server-initialize-event-manager");
    //private final SslContext sslCtx;
    private final SSLContext sslContext;
    private ExtensionRegistry registry;
    private SecurePushServerHandler securePushServerHandler;
    private ProtobufDecoder protobufDecoder;
    private ProtobufEncoder protobufEncoder;
    public SecurePushServerInitializer(SSLContext sslCtx) {
        this.sslContext = sslCtx;
        eventManager.start();
        registry = ExtensionRegistry.newInstance();
        protobufDecoder=new ProtobufDecoder(Entity.BaseEntity.getDefaultInstance(),registry);
        protobufEncoder=new ProtobufEncoder();
        Entity.registerAllExtensions(registry);
        securePushServerHandler=new SecurePushServerHandler(eventManager);
    }
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        logger.debug("initialize channel,remote address is:"+ch.remoteAddress());
        ChannelPipeline pipeline = ch.pipeline();
        SSLEngine sslEngine=sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.setWantClientAuth(true);
        pipeline.addLast(new SslHandler(sslEngine));
        //------------------------------------------------------------------------------
        //180秒的心跳检测，200秒之类必须受到回复,加上一定的随机性
        //int rd=(int)(10*Math.random());
        pipeline.addLast("timeout", new IdleStateHandler(200, 180, 180, TimeUnit.SECONDS));
        // On top of the SSL handler, add the text line codec.
        //放到外层的registry来进行共用
//        ExtensionRegistry registry = ExtensionRegistry.newInstance();
//        Entity.registerAllExtensions(registry);
        //pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
        pipeline.addLast(protobufDecoder);
        pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast(protobufEncoder);
        //pipeline.addLast(spsh);
        //pipeline.addLast(new SecurePushServerHandler(eventManager));
        //我们可以使用一个handler来处理所有的东西，当且晋档这个handelr有个@Sharable的注解
        pipeline.addLast(securePushServerHandler);
    }
    public void addListener(ConnectionListener connectionListner){
        eventManager.addListener(connectionListner);
    }
}
