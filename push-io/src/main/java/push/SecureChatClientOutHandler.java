package push;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import push.message.BaseEntity;

/**
 * Created by 201601180154 on 2016/6/13.
 */
public class SecureChatClientOutHandler extends ChannelOutboundHandlerAdapter{
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof BaseEntity)
            ctx.write(, promise);
    }
}
