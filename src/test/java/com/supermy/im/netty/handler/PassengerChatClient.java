package com.supermy.im.netty.handler;

import com.supermy.im.netty.domain.Cmd;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.ExecutorService;


/**
 * 简单聊天服务器-客户端
 *
 * @author moyong
 */
public class PassengerChatClient {



    @Value("${im.server.host}")
    private String imServerHost;

    @Value("${im.server.port}")
    private String imServerPort;


    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8090"));

    public static void main(String[] args) throws Exception {
//        new DriverChatClient("127.0.0.1", 8090).runtest();
//       new DriverChatClient("127.0.0.1", 8090).run("g::18610586586;1462870826752;106.72;26.57");


        new PassengerChatClient("127.0.0.1", 8090).run();

//        Thread.sleep(600000);

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

    public PassengerChatClient() {
    }

    public PassengerChatClient(@Value("${im.server.host}") String host, @Value("${im.server.port}") int port) {
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
                    .handler(new PassengerChatClientInitializer(sslCtx));

            channel = bootstrap.connect(host, port).sync().channel();

            //1.乘客登陆绑定
            channel.writeAndFlush(String.format(SetSystemProperty.getKeyValue("im.msg"), Cmd.BIND_PASSENGER,"司机出车",
                    new Date().getTime(), "success",
                    "{'_id':'15510325588','name':'莫先生'}") +"\n");

            //2.乘客约车 生成流水号(id代替流水号)  用户重复发起后台进行处理
            channel.writeAndFlush(String.format(SetSystemProperty.getKeyValue("im.msg"),Cmd.POST_APPOINTMENT,"乘客约车", new Date().getTime(), "success",
                    String.format(SetSystemProperty.getKeyValue("passenger.tasks"),
                            "15510325588",new Date().getTime(), "0.0","0.0","0.7")) +"\n");

            //3.取消订单;接收到开始安排订单
//            channel.writeAndFlush(String.format(SetSystemProperty.getKeyValue("im.msg"),Cmd.CANCEL_APPOINTMENT,"取消订单",new Date().getTime(), "success",
//                    String.format(SetSystemProperty.getKeyValue("passenger.tasks"),
//                            "15510325588",new Date().getTime(), "0.0","0.0","0.7")) +"\n");

            //4.再次约车
            channel.writeAndFlush(String.format(SetSystemProperty.getKeyValue("im.msg"),Cmd.POST_APPOINTMENT,"乘客约车", new Date().getTime(), "success",
                    String.format(SetSystemProperty.getKeyValue("passenger.tasks"),
                            "15510325588",new Date().getTime(), "0.0","0.0","0.7")) +"\n");



//                    键盘输入交互
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                channel.writeAndFlush(in.readLine() + "\r\n");
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
