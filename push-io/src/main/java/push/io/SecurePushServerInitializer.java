package push.io;

import com.google.protobuf.ExtensionRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.registry.EventManager;
import push.message.Entity;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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
    public SecurePushServerInitializer(SSLContext sslCtx) {
        this.sslContext = sslCtx;
        eventManager.start();
        registry = ExtensionRegistry.newInstance();
        Entity.registerAllExtensions(registry);
    }
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        logger.debug("initialize channel,remote address is:"+ch.remoteAddress());
        ChannelPipeline pipeline = ch.pipeline();
        // Add SSL handler first to encrypt and decrypt everything.
        // In this example, we use a bogus certificate in the server side
        // and accept any invalid certificates in the client side.
        // You will need something more complicated to identify both
        // and server in the real world.

        //------------------------------------------------------------------------------
        //pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        SSLEngine sslEngine=sslContext.createSSLEngine();
        //------------------------------------------------------
//        System.out.println("lslsljs;lgj");
//        String[] suits2=sslEngine.getSupportedCipherSuites();
//        for(String suit:suits2){
//            System.out.println(suit);
//        }
//        sslEngine.setEnabledCipherSuites(new String[]{"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"});
//        String[] suits=sslEngine.getEnabledCipherSuites();
//        for(String suit:suits){
//            System.out.println(suit);
//        }
        //------------------------------------------------------
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
        pipeline.addLast(new ProtobufDecoder(Entity.BaseEntity.getDefaultInstance(),registry));
        pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast(new ProtobufEncoder());

        // 不同的socketchannel不能共用一个securepushserverhandler，因此下面的写法不对，只能适用new的方式来生成新的handler
        //pipeline.addLast(spsh);
        pipeline.addLast(new SecurePushServerHandler(eventManager));
    }
    public void addListener(ConnectionListener connectionListner){
        eventManager.addListener(connectionListner);
    }
}
