package push.io;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * Created by mingzhu7 on 2017/11/7.
 */
public class UdpClient {
    private int port;
    public UdpClient(int port){
        this.port=port;
    }
    public void start() throws Exception{
        Bootstrap bootstrap=new Bootstrap();
        NioEventLoopGroup nioEventLoopGroup=new NioEventLoopGroup();
        bootstrap.group(nioEventLoopGroup).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST,true).handler(new ClientHandler());

        Channel channel =bootstrap.bind(0).sync().channel();
        channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("我来做个测试",CharsetUtil.UTF_8),new InetSocketAddress("10.10.104.255",port))).sync();
    }
    public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket>{

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            System.out.println("receive server:"+msg.content().toString(CharsetUtil.UTF_8));
        }
    }
    public static void main(String[] args)throws Exception{
        UdpClient udpClient=new UdpClient(8877);
        udpClient.start();
    }
}
