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
import push.model.message.Entity;
import push.registry.EventManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class SecurePushClientInitializer extends ChannelInitializer<SocketChannel> {
    private static Logger logger= LoggerFactory.getLogger(SecurePushClientInitializer.class);
    //private final SslContext sslCtx;
    private final SSLContext sslContext;
    private SecurePushClient spc;
    private EventManager<MessageEvent> eventManager=new EventManager<MessageEvent>("client-initialize-event-manager");
    private ExtensionRegistry registry;
    public SecurePushClientInitializer(SSLContext sslCtx,SecurePushClient spc) {
        this.sslContext = sslCtx;
        this.spc=spc;
        //我们不希望事件合并（丢失），所以设置这个interval为0，事实上这个默认是0，所以不会丢失事件
        eventManager.setInterval(0);
        eventManager.start();
        //把这个放到这个上面，而不是channel initialize的时候再初始化，会比较慢
        registry=ExtensionRegistry.newInstance();
        Entity.registerAllExtensions(registry);
    }
    public void addListener(MessageListener messageListener){
        eventManager.addListener(messageListener);
    }


    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        int rd=(int)(30*Math.random());
        pipeline.addLast("timeout", new IdleStateHandler(200, 180, 150+rd, TimeUnit.SECONDS));
        //------------------------------------------------------------------------------
        //pipeline.addLast(sslCtx.newHandler(ch.alloc(), spc.host, spc.port));
        SSLEngine sslEngine=sslContext.createSSLEngine(spc.host,spc.port);
        sslEngine.setUseClientMode(true);
        //sslEngine.setEnabledCipherSuites(new String[]{"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"});
        pipeline.addLast(new SslHandler(sslEngine));
        //------------------------------------------------------------------------------
        // On top of the SSL handler, add the text line codec.
        //pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast(new ProtobufEncoder());
        pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
//        ExtensionRegistry registry=ExtensionRegistry.newInstance();
//        Entity.registerAllExtensions(registry);
        pipeline.addLast(new ProtobufDecoder(Entity.BaseEntity.getDefaultInstance(),registry));
        // and then business logic.
        pipeline.addLast(new SecurePushClientHandler(eventManager));
    }
}
