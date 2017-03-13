package push.io;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.registry.EventManager;
import push.message.Entity;

public class SecurePushServerHandler extends SimpleChannelInboundHandler<Entity.BaseEntity> {
    private static Logger logger= LoggerFactory.getLogger(SecurePushServerHandler.class);
    private  final EventManager<ConnectionEvent> eventManager;
    public SecurePushServerHandler(EventManager eventManager){
        this.eventManager=eventManager;
    }
//    @Override
//    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
//        ctx.channel().close();
//    }
    //static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Entity.BaseEntity welcomeEntity;
    private static final Entity.BaseEntity pingReplyEntity;
    static {
            String msg = "Welcome to  secure chat service!\n";
            Entity.Message.Builder msgBuilder = Entity.Message.newBuilder();
            msgBuilder.setMessage(msg);
            msgBuilder.setFrom("0");
            msgBuilder.setTo("0");
            Entity.BaseEntity.Builder builder = Entity.BaseEntity.newBuilder();
            builder.setType(Entity.Type.MESSAGE);
            builder.setExtension(Entity.message, msgBuilder.build());
            welcomeEntity = builder.build();


            Entity.Ping.Builder ping = Entity.Ping.newBuilder();
            ping.setMessage("ok");
            Entity.BaseEntity.Builder base = Entity.BaseEntity.newBuilder();
            base.setType(Entity.Type.PING);
            base.setExtension(Entity.ping, ping.build());
            pingReplyEntity = base.build();
    }
    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        logger.debug("channle is active now,address is:"+ctx.channel().remoteAddress());
        ConnectionEvent ce=new ConnectionEvent();
        ce.setChc(ctx);
        ce.setCet(ConnectionEvent.ConnectionEventType.CONNECTION_TRANSIENT);
        eventManager.add(ce);
        // Once session is secured, send a greeting and register the channel to the global channel
        // list so the channel received the messages from others.
//        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
//                new GenericFutureListener<Future<Channel>>() {
//                    @Override
//                    public void operationComplete(Future<Channel> future) throws Exception {
//                        ctx.writeAndFlush(welcomeEntity).sync();
//                    }
//        });
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Entity.BaseEntity msg) throws Exception {
        if(msg.getType()==Entity.Type.PING){
            //这段代码只是为了提前结束判断
            ctx.writeAndFlush(pingReplyEntity);
        }else if(msg.getType()==Entity.Type.MESSAGE){
            Entity.Message message=msg.getExtension(Entity.message);
//            long to=message.getTo();
//            ChannelHandlerContext toContext=sps.channels.get(to);
//            if(toContext!=null)
//                toContext.writeAndFlush(msg);
            ConnectionEvent ce=new ConnectionEvent();
            ce.setCet(ConnectionEvent.ConnectionEventType.MESSAGE_TRANSFER);
            ce.setChc(ctx);
            ce.setMessage(msg);
            eventManager.add(ce);
        }
        else if(msg.getType()==Entity.Type.LOGIN){
            Entity.Login login=msg.getExtension(Entity.login);
            ConnectionEvent ce=new ConnectionEvent();
            ce.setCet(ConnectionEvent.ConnectionEventType.CONNECTION_ADD);
            ce.setChc(ctx);
            ce.setUid(login.getUid());
            ce.setMessage(msg);
            eventManager.add(ce);
            //sps.channels.put(login.getUid(),ctx);
        }else if(msg.getType()==Entity.Type.LOGOUT){
            Entity.Logout logout=msg.getExtension(Entity.logout);
            ConnectionEvent ce=new ConnectionEvent();
            ce.setCet(ConnectionEvent.ConnectionEventType.CONNECTION_REMOVE);
            ce.setChc(ctx);
            ce.setUid(logout.getUid());
            eventManager.add(ce);
            //sps.channels.remove(logout.getUid());
            //ctx.close();
        }else {
            logger.error("receive message that cannot be parsed:"+msg.toString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        ctx.close();
//        ctx.channel().close();
        //因为关闭一个通道还会进行其他相关的操作，因此把这个作为一个操作进行处理。
        ConnectionEvent ce=new ConnectionEvent();
        ce.setCet(ConnectionEvent.ConnectionEventType.CONNECTION_REMOVE);
        ce.setChc(ctx);
        eventManager.add(ce);
        logger.error("remote client error",cause);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                logger.error("READER_IDLE");
                //ctx.close();
                //交给上层去删除
                ConnectionEvent ce=new ConnectionEvent();
                ce.setCet(ConnectionEvent.ConnectionEventType.CONNECTION_REMOVE);
                ce.setChc(ctx);
                eventManager.add(ce);
            } else if (event.state().equals(IdleState.WRITER_IDLE)) {
                //logger.error("WRITER_IDLE");
            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                //logger.error("ALL_IDLE");
                // 发送心跳
                //这段代码可以提出去啊
//                Entity.Ping.Builder ping=Entity.Ping.newBuilder();
//                ping.setMessage("ping");
//                Entity.BaseEntity.Builder base=Entity.BaseEntity.newBuilder();
//                base.setType(Entity.Type.PING);
//                base.setExtension(Entity.ping,ping.build());
//                //ctx.channel().write(base.build());
//                ctx.writeAndFlush(base.build());
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
