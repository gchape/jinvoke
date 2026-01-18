package io.jinvoke.rpc.handlers;

import com.alibaba.fastjson2.JSON;
import io.jinvoke.rpc.model.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Packet> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        byte type = (byte) msg.type().ordinal();

        byte[] payload = switch (msg.type()) {
            case REGISTER, HEART_BEAT -> new byte[0];
            case CALL, FORWARD, RESPONSE -> JSON.toJSONBytes(msg.payload());
        };

        out.writeByte(type);
        out.writeInt(payload.length);
        out.writeBytes(payload);
    }
}
