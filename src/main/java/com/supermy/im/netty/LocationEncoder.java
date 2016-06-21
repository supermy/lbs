package com.supermy.im.netty;

import com.supermy.im.netty.domain.Location;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by moyong on 16/5/10.
 */
@Deprecated
public class LocationEncoder extends MessageToByteEncoder<Location> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Location msg, ByteBuf out) {
        out.writeBytes(msg.toString().getBytes());
    }
}

