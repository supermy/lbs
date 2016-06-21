/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.supermy.im.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


/**
 * Main Server
 *
 */
@Component
public class TCPServer {

    private volatile boolean closed = false;

    @Autowired
    @Qualifier("serverBootstrap")
    private ServerBootstrap serverBootstrap;

    @Autowired
    @Qualifier("tcpSocketAddress")
    private InetSocketAddress tcpPort;

    private Channel serverChannel;

    /**
     * closeFuture().sync() 阻塞
     *
     * @throws Exception
     */
    @PostConstruct
    public void start() throws Exception {
        closed = false;

        serverChannel =  serverBootstrap.bind(tcpPort).sync().channel().closeFuture().sync().channel();

        //doBind();  //心跳包

    }

    @PreDestroy
    public void stop() throws Exception {
        closed = true;

        serverChannel.close();
        serverChannel.parent().close();

    }

    public ServerBootstrap getServerBootstrap() {
        return serverBootstrap;
    }

    public void setServerBootstrap(ServerBootstrap serverBootstrap) {
        this.serverBootstrap = serverBootstrap;
    }

    public InetSocketAddress getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(InetSocketAddress tcpPort) {
        this.tcpPort = tcpPort;
    }


    /**
     * 3秒后自动重连
     */
    protected void doBind() {
        if (closed) {
            return;
        }

        serverBootstrap.bind(tcpPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()) {
                    System.out.println("Started Tcp Server: " + tcpPort);
                } else {
                    System.out.println("Started Tcp Server Failed: " + tcpPort);

                   // f.channel().eventLoop().schedule(() -> doBind(), 3, TimeUnit.SECONDS);

                    f.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doBind();
                        }
                    }, 3, TimeUnit.SECONDS);
                }
            }
        });
    }


}
