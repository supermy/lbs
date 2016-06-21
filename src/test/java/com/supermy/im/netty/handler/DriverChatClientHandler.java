package com.supermy.im.netty.handler;

import com.supermy.im.netty.domain.Cmd;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.swing.text.Document;
import java.util.Date;

/**
 * 客户端 channel
 * 
 * @author moyong
 */
public class DriverChatClientHandler extends SimpleChannelInboundHandler<String> {
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
		Channel incoming = ctx.channel();

		//todo 处理服务器回复的消息
		System.out.println(s);
		//如果是预约单,则接单
		org.bson.Document msg = org.bson.Document.parse(s);
        boolean a=msg.getString("cmd").equals(Cmd.RECEIVE_APPOINTMENT);
        if(a){

            System.out.println("接收订单信息,开始抢单.....");

            //Thread.sleep(1000);  //测试能否取消订单

            incoming.writeAndFlush(String.format(SetSystemProperty.getKeyValue("im.msg"),
                    Cmd.SEND_ORDER,"抢单...", new Date().getTime(), "success",
                    msg.get("data",org.bson.Document.class).toJson()) +"\n");

        }


	}
}
