package push;

import com.google.protobuf.ExtensionRegistry;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import push.message.Entity;

import javax.sql.ConnectionEventListener;

public class SecurePushClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private SecurePushClient spc;
    private  EventManager<MessageEvent> eventManager=new EventManager<MessageEvent>();
    public SecurePushClientInitializer(SslContext sslCtx,SecurePushClient spc) {
        this.sslCtx = sslCtx;
        this.spc=spc;
        eventManager.start();
    }
    public void addListener(MessageListener messageListener){
        eventManager.addListener(messageListener);
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(sslCtx.newHandler(ch.alloc(), spc.host, spc.port));

        // On top of the SSL handler, add the text line codec.
        //pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast(new ProtobufEncoder());
        pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
        ExtensionRegistry registry=ExtensionRegistry.newInstance();
        Entity.registerAllExtensions(registry);
        pipeline.addLast(new ProtobufDecoder(Entity.BaseEntity.getDefaultInstance(),registry));
        // and then business logic.
        pipeline.addLast(new SecurePushClientHandler(eventManager));
    }
}
