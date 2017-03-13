package push.io;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public final class SecurePushServer {
    private static Logger logger= LoggerFactory.getLogger(SecurePushServer.class);
    static int port;
    public EventLoopGroup bossGroup;
    public EventLoopGroup workerGroup;
    SecurePushServerInitializer spsi;
  //---------------------------------------------------------------------
    static X509Certificate[] toX509Certificates(File file) throws CertificateException {
        if (file == null) {
            return null;
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteBuf[] certs = MyPermReader.readCertificates(file);
        X509Certificate[] x509Certs = new X509Certificate[certs.length];

        try {
            for (int i = 0; i < certs.length; i++) {
                x509Certs[i] = (X509Certificate) cf.generateCertificate(new ByteBufInputStream(certs[i]));
            }
        } finally {
            for (ByteBuf buf: certs) {
                buf.release();
            }
        }
        return x509Certs;
    }
    protected static PKCS8EncodedKeySpec generateKeySpec(char[] password, byte[] key)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidKeyException, InvalidAlgorithmParameterException {

        if (password == null || password.length == 0) {
            return new PKCS8EncodedKeySpec(key);
        }

        EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);

        Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());

        return encryptedPrivateKeyInfo.getKeySpec(cipher);
    }
    static PrivateKey toPrivateKey(File keyFile, String keyPassword) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException,
            KeyException, IOException {
        if (keyFile == null) {
            return null;
        }
        ByteBuf encodedKeyBuf = MyPermReader.readPrivateKey(keyFile);
        byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
        encodedKeyBuf.readBytes(encodedKey).release();

        PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPassword == null ? null : keyPassword.toCharArray(),
                encodedKey);

        PrivateKey key;
        try {
            key = KeyFactory.getInstance("RSA").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException ignore) {
            try {
                key = KeyFactory.getInstance("DSA").generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore2) {
                try {
                    key = KeyFactory.getInstance("EC").generatePrivate(encodedKeySpec);
                } catch (InvalidKeySpecException e) {
                    throw new InvalidKeySpecException("Neither RSA, DSA nor EC worked", e);
                }
            }
        }
        return key;
    }
    //-------------------------------------------------------------------
    public SecurePushServer(int port){
        this.port=port;
    }
    public void start() throws Exception {
//        SelfSignedCertificate ssc = new SelfSignedCertificate();
//        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
//                .build();
        //-----------------------------------------------------------------------------
        SSLContext sslContext = SSLContext.getInstance("TLSv1.1");
        TrustManager tm = new SimpleTrustManager();
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        File keyCertChainFile=ssc.certificate();
        File keyFile=ssc.privateKey();
        X509Certificate[] keyCertChain=toX509Certificates(keyCertChainFile);
        PrivateKey key=toPrivateKey(keyFile,null);
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("key", key, new char[0], keyCertChain);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, new char[0]);
        sslContext.init(kmf.getKeyManagers(), new TrustManager[] { tm }, null);
        //-----------------------------------------------------------------------------

        SslContext sslCtx =null;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            spsi=new SecurePushServerInitializer(sslContext);
            b.group(bossGroup, workerGroup)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .option(ChannelOption.SO_TIMEOUT,10000)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,10000)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(spsi);

            //b.bind(port).sync().channel().closeFuture().sync();
            b.bind(port).sync();
            //this.addListener(new DefaultConnectionListener());
        }catch (Exception e){
            logger.error("secure push server start error",e);
            throw new RuntimeException("secure push server start error",e);
        }
    }
    public void stop(){
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }catch (Exception e){
            logger.error("stop secure server error",e);
        }
    }
    public void addListener(ConnectionListener connectionListener){
        spsi.addListener(connectionListener);
    }
    public static void main(String[] args) throws Exception {
//        SelfSignedCertificate ssc = new SelfSignedCertificate();
//        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
//            .build();
//
//        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//        try {
//            ServerBootstrap b = new ServerBootstrap();
//            b.group(bossGroup, workerGroup)
//             .channel(NioServerSocketChannel.class)
//             .handler(new LoggingHandler(LogLevel.INFO))
//             .childHandler(new SecurePushServerInitializer(sslCtx));
//
//            b.bind(PORT).sync().channel().closeFuture().sync();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
    }
}
