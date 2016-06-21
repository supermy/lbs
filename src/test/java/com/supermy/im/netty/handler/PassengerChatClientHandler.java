package com.supermy.im.netty.handler;

import com.supermy.im.netty.domain.Cmd;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Date;

/**
 * 客户端 channel
 * 
 * @author moyong
 */
public class PassengerChatClientHandler extends SimpleChannelInboundHandler<String> {
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
		Channel incoming = ctx.channel();

		//todo 处理服务器回复的消息
		System.out.println(s);
		org.bson.Document msg = org.bson.Document.parse(s);

		boolean a=msg.getString("cmd").equals(Cmd.WAIT_APPOINTMENT);

		if(a){

			System.out.println("等待抢单消息,可以取消订单.....");

			String json = String.format(SetSystemProperty.getKeyValue("im.msg"),
					Cmd.CANCEL_APPOINTMENT,"取消订单...",new Date().getTime(), "success",
					msg.get("data",org.bson.Document.class).toJson());

			incoming.writeAndFlush(json+"\n");

		}

	}
}
