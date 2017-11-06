package push.io;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

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
        EventLoopGroup bossGroup =new NioEventLoopGroup(1);
        EventLoopGroup workerGroup=new NioEventLoopGroup();
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.group(bossGroup).group(workerGroup).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST,true).handler();
    }
}
