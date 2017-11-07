package push.io;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

/**
 * Created by mingzhu7 on 2017/11/6.
 */
public class UdpServer {
    //http://blog.csdn.net/mffandxx/article/details/53264172
    private int port;
    public UdpServer(int port){
        this.port=port;
    }
    public void start() throws Exception{
        EventLoopGroup workerGroup=new NioEventLoopGroup();
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.group(workerGroup).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST,true).handler(new ChineseProverServerHandler()).bind(port).sync().channel().closeFuture().await();
    }
    public class ChineseProverServerHandler extends SimpleChannelInboundHandler<DatagramPacket>{

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            String req=msg.content().toString(CharsetUtil.UTF_8);
            System.out.println("receive client:"+msg.sender().getAddress()+" "+msg.sender().getPort()+req);
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("haha",CharsetUtil.UTF_8),msg.sender()));
        }
    }
    public static void main(String[] args) throws Exception{
        UdpServer udpServer=new UdpServer(8877);
        udpServer.start();
    }
}
