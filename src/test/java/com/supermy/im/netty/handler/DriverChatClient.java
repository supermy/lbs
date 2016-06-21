package com.supermy.im.netty.handler;

import com.supermy.im.netty.domain.Cmd;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * 司机客户端测试
 *
 * @author moyong
 */
public class DriverChatClient {


    @Value("${im.server.host}")
    private String imServerHost;

    @Value("${im.server.port}")
    private String imServerPort;


    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8090"));

    public static void main(String[] args) throws Exception {

        new DriverChatClient("127.0.0.1", 8090).run();

    }

    private volatile boolean closed = false;
    private volatile EventLoopGroup workerGroup;
    private volatile Bootstrap bootstrap;

    private ExecutorService executorService;
    private Channel channel = null;

    @Value("${im.server.host}")
    private  String host;

    @Value("${im.server.port}")
    private  int port;

    public DriverChatClient() {
    }

    public DriverChatClient(@Value("${im.server.host}") String host, @Value("${im.server.port}") int port) {
        this.host = host;
        this.port = port;
    }



    public void run() throws Exception {
        //自动构造数据交互 g::18610586586;1462870826752;106.72;26.57

        final SslContext sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);

        workerGroup = new NioEventLoopGroup();
        try {
            bootstrap = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new DriverChatClientInitializer(sslCtx));

            channel = bootstrap.connect(host, port).sync().channel();

            //1.司机出车绑定
            String json = String.format(SetSystemProperty.getKeyValue("im.msg"), Cmd.BIND_DRIVER,"司机出车",new Date().getTime(), "success",
                    "{'_id':'18610588588','name':'张司机','plateNumber':'京P98888'}");

            channel.writeAndFlush(json+"\n");

            //2.定时执行 发送经纬度信息
//            Channel ch = null; // Get reference to channel
            ScheduledFuture<?> future = channel.eventLoop().scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            String data = String.format(SetSystemProperty.getKeyValue("driver.geo"), "18610588588",new Date().getTime(), "1.2","1.1");

                            String json = String.format(SetSystemProperty.getKeyValue("im.msg"), Cmd.SEND_GEO,"司机位置信息",new Date().getTime(), "success",data);
                            channel.writeAndFlush(json + "\n");

                            System.out.println("Run every 3 seconds");

                        }
                    }, 2, 3, TimeUnit.SECONDS);

            //3.司机等待接单
            //channel.writeAndFlush("g::18610586586;1462870826752;106.72;26.57" + "\n");



//                    键盘输入交互
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                channel.writeAndFlush(in.readLine() + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }


    }



    private String getServerInfo() {
        return String.format("RemoteHost=%s RemotePort=%d",
                host,
                port);
    }


    public void close() {
        closed = true;
        workerGroup.shutdownGracefully();
        System.out.println("Stopped Tcp Client: " + getServerInfo());
    }

}
