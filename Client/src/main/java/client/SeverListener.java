package client;

import common.filetransfer.FileLoader;
import common.messages.FileHeader;
import common.messages.Message;
import common.callback.Callback;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.util.UUID;

public class SeverListener implements Runnable  {
    private static final Logger LOGGER = LogManager.getLogger(SeverListener.class.getName());

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8189"));
    private static SeverListener severListener;

    private MessageClientHandler messageClientHandler;
    private EventLoopGroup group;
    private Channel ch;
    private boolean isConnect = false;
    private UUID token;

    static SeverListener getInstance() {
        if(severListener == null) {
            severListener = new SeverListener();
        }
        return severListener;
    }

    private SeverListener(){

    }
    public void setToken(UUID token) {
        this.token = token;
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void setCallback(Callback callback) {
        messageClientHandler.setCallback(callback);
    }

    public void registerFileLoader(FileLoader fileLoader) {
        messageClientHandler.registerFileLoader(fileLoader);
    }

    public void unRegisterFileLoader(FileHeader fileHeader) {
        messageClientHandler.unRegisterFileLoader(fileHeader);
    }

    public void sendMessage(Message message) {
        message.setToken(token);
        ch.writeAndFlush(message);
    }

    public void disconnect() {
        group.shutdownGracefully();
    }

    @Override
    public void run() {
        // Configure SSL.
        SslContext sslCtx = null;
        if (SSL) {
            try {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        final SslContext sslCtx1 = sslCtx;
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)  {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx1 != null) {
                                p.addLast(sslCtx1.newHandler(ch.alloc(), HOST, PORT));
                            }
                            p.addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ChunkedWriteHandler(),
                                    new MessageClientHandler());
                        }
                    });

            // Start the connection attempt.
            ch = b.connect(HOST, PORT).sync().channel();
            messageClientHandler = ch.pipeline().get(MessageClientHandler.class);
            isConnect = true;
            ch.closeFuture().sync();

        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } finally {
            group.shutdownGracefully();
        }
    }

    public void stop() {
        if(group != null) {
            group.shutdownGracefully();
        }
    }
}
