package com.supermy.im.netty;

/**
 * Created by moyong on 2017/10/17.
 */

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    /**
     * 创建ServerBootstrap对象来启动服务器，然后配置这个对象的相关选项，如端口、线程模式、事件循环，并且添加逻辑处理程序用来处理
     业务逻辑
     * @throws Exception
     */
    public void start() throws Exception {
        //创建NioEventLoopGroup对象来处理事件，如接受新连接、接收数据、写数据等等
        EventLoopGroup group = new NioEventLoopGroup();
        try {
//create ServerBootstrap instance
            //创建ServerBootstrap实例来引导绑定和启动服务器
            ServerBootstrap b = new ServerBootstrap();
//Specifies NIO transport, local socket address
//Adds handler to channel pipeline
            /**
             * // 启动服务器应先创建一个ServerBootstrap对象，因为使用NIO，所以指定NioEventLoopGroup来接受和 处理新连接，
             // 指定通道类型为NioServerSocketChannel，设置InetSocketAddress让服务器监听某个端口已等待客户端连接。
             *
             */
            b.group(group).channel(NioServerSocketChannel.class).localAddress(port)
                    .childHandler(new ChannelInitializer<Channel>() {
                        /**
                         * 调用childHandler放来指定连接后调用的ChannelHandler，这个方法传ChannelInitializer类型的参数，
                         * ChannelInitializer是个抽象类，所 以需要实现initChannel方法，这个方法就是用来设置ChannelHandler。
                         * @param ch
                         * @throws Exception
                         */
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });
//Binds server, waits for server to close, and releases resources
            //最后绑定服务器等待直到绑定完成，调用sync()方法会阻塞直到服务器完成绑定，然后服务器等待通道关闭，因为使用sync()，所以关闭操作也 会被阻塞。
            ChannelFuture f = b.bind().sync();
            System.out.println(EchoServer.class.getName() + "started and listen on " + f.channel().localAddress());
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args) throws Exception {
//        if (args.length != 1) {
//            System.err.println(
//                    "Usage: " + EchoServer.class.getSimpleName() +
//                            " <port>");
//            return;
//        }
//        int port = Integer.parseInt(args[0]);


        new EchoServer(19740).start();
    }
}
