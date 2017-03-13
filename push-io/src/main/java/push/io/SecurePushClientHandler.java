package push.io;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.registry.EventManager;
import push.model.message.Entity;

/**
 * Handles a client-side channel.
 */
public class SecurePushClientHandler extends SimpleChannelInboundHandler<Entity.BaseEntity> {
    private static Logger logger= LoggerFactory.getLogger(SecurePushClientHandler.class);
    private EventManager<MessageEvent> eventManager;
    public SecurePushClientHandler(EventManager eventManager){
        this.eventManager=eventManager;
    }
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Entity.BaseEntity msg) throws Exception {
        if(msg.getType()==Entity.Type.PING){
            //回复心跳
//            Entity.Ping.Builder ping=Entity.Ping.newBuilder();
//            ping.setMessage("ok");
//            Entity.BaseEntity.Builder base=Entity.BaseEntity.newBuilder();
//            base.setType(Entity.Type.PING);
//            base.setExtension(Entity.ping,ping.build());
//            ctx.writeAndFlush(base.build());
//            logger.error("receive ping");
        }
        else if(msg.getType()==Entity.Type.MESSAGE){
            //Entity.Message message=msg.getExtension(Entity.message);
            //System.out.println(message.getFrom()+">"+message.getMessage());
            MessageEvent messageEvent=new MessageEvent();
            messageEvent.setMessageEventType(MessageEvent.MessageEventType.MESSAGE_RECEIVE);
            messageEvent.setMessage(msg);
            eventManager.add(messageEvent);
        }
    }
    static final Entity.BaseEntity pingEntity;
    static {
        Entity.Ping.Builder ping=Entity.Ping.newBuilder();
        ping.setMessage("ping");
        Entity.BaseEntity.Builder base=Entity.BaseEntity.newBuilder();
        base.setType(Entity.Type.PING);
        base.setExtension(Entity.ping,ping.build());
        pingEntity=base.build();
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                logger.debug("READ_IDLE");
                ctx.close();
            } else if (event.state().equals(IdleState.WRITER_IDLE)) {
                logger.debug("WRITER_IDLE");
            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                logger.debug("ALL_IDLE");
                // 发送心跳
                ctx.writeAndFlush(pingEntity);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("remote socket exception caught",cause);
        ctx.close();
    }
}
