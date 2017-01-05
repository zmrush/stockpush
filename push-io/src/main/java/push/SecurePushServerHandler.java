package push;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.message.Entity;

import java.net.InetAddress;

public class SecurePushServerHandler extends SimpleChannelInboundHandler<Entity.BaseEntity> {
    private Logger logger= LoggerFactory.getLogger(SecurePushServerHandler.class);
    private SecurePushServer sps;
    public SecurePushServerHandler(SecurePushServer sps){
        this.sps=sps;
    }
    //static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Once session is secured, send a greeting and register the channel to the global channel
        // list so the channel received the messages from others.
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(Future<Channel> future) throws Exception {
                        String msg="Welcome to " + InetAddress.getLocalHost().getHostName() + " secure chat service!\n";
                        msg+="Your session is protected by " +
                                        ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() +
                                        " cipher suite.\n";
                        Entity.Message.Builder msgBuilder=Entity.Message.newBuilder();
                        msgBuilder.setMessage(msg);
                        msgBuilder.setFrom(0);
                        msgBuilder.setTo(0);
                        Entity.BaseEntity.Builder builder=Entity.BaseEntity.newBuilder();
                        builder.setType(Entity.Type.MESSAGE);
                        builder.setExtension(Entity.message,msgBuilder.build());
                        ctx.writeAndFlush(builder.build()).sync();
                    }
        });
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Entity.BaseEntity msg) throws Exception {
        if(msg.getType()==Entity.Type.LOGIN){
            Entity.Login login=msg.getExtension(Entity.login);
            sps.channels.put(login.getUid(),ctx);
        }else if(msg.getType()==Entity.Type.LOGOUT){
            Entity.Logout logout=msg.getExtension(Entity.logout);
            sps.channels.remove(logout.getUid());
            ctx.close();
        }else{
            Entity.Message message=msg.getExtension(Entity.message);
            long to=message.getTo();
            ChannelHandlerContext toContext=sps.channels.get(to);
            if(toContext!=null)
                toContext.writeAndFlush(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("remote client error",cause);
        ctx.close();
    }
}
