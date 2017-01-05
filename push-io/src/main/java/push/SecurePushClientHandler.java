package push;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import push.message.Entity;

/**
 * Handles a client-side channel.
 */
public class SecurePushClientHandler extends SimpleChannelInboundHandler<Entity.BaseEntity> {
    private static Logger logger= LoggerFactory.getLogger(SecurePushClientHandler.class);
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Entity.BaseEntity msg) throws Exception {
        if(msg.getType()==Entity.Type.MESSAGE){
            Entity.Message message=msg.getExtension(Entity.message);
            System.out.println(message.getFrom()+">"+message.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("remote socket exception caught",cause);
        ctx.close();
    }
}
