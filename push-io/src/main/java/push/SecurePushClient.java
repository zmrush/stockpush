package push;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import push.message.Entity;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class SecurePushClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8992"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new SecurePushClientInitializer(sslCtx));

            // Start the connection attempt.
            Channel ch = b.connect(HOST, PORT).sync().channel();
            Entity.Login.Builder login=Entity.Login.newBuilder();
            login.setUid(213);
            Entity.BaseEntity.Builder builder=Entity.BaseEntity.newBuilder();
            builder.setType(Entity.Type.LOGIN);
            builder.setExtension(Entity.login,login.build());
            ChannelFuture f=ch.writeAndFlush(builder.build());
            try {
                if (f != null)
                    f.sync();
            }catch (Throwable throwable){
                throwable.printStackTrace();
                throw new RuntimeException("login error",throwable);
            }
            // Read commands from the stdin.
            ChannelFuture lastWriteFuture = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            for (;;) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                String[] lines=line.split("<");
                if(lines.length==2) {
                    Entity.BaseEntity.Builder builder2 = Entity.BaseEntity.newBuilder();
                    builder2.setType(Entity.Type.MESSAGE);
                    Entity.Message.Builder msgBuilder = Entity.Message.newBuilder();
                    msgBuilder.setFrom(213);
                    msgBuilder.setTo(Long.valueOf(lines[0]));
                    msgBuilder.setMessage(lines[1]);
                    builder2.setExtension(Entity.message,msgBuilder.build());
                    lastWriteFuture = ch.writeAndFlush(builder2.build());
                }else{
                    if ("bye".equals(line.toLowerCase())) {
                        Entity.BaseEntity.Builder builder2=Entity.BaseEntity.newBuilder();
                        builder2.setType(Entity.Type.LOGOUT);
                        Entity.Logout.Builder msgBuilder=Entity.Logout.newBuilder();
                        msgBuilder.setUid(213);
                        builder2.setExtension(Entity.logout,msgBuilder.build());
                        ch.writeAndFlush(builder2.build());
                        ch.closeFuture().sync();
                        break;
                    }
                }
            }

            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        } finally {
            // The connection is closed automatically on shutdown.
            group.shutdownGracefully();
        }
    }
}
