package io.jinvoke.rpc.handlers;

import com.alibaba.fastjson2.JSON;
import io.jinvoke.rpc.model.Message;
import io.jinvoke.rpc.model.Packet;
import io.jinvoke.rpc.model.Request;
import io.jinvoke.rpc.model.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) return;

        in.markReaderIndex();

        byte ordinal = in.readByte();
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte[] payload = new byte[length];
        in.readBytes(payload);

        Message.Type type = Message.Type.fromOrdinal(ordinal);

        Message.Payload decodedPayload = switch (type) {
            case REGISTER, HEART_BEAT -> null;
            case CALL, FORWARD -> JSON.parseObject(payload, Request.class);
            case RESPONSE -> JSON.parseObject(payload, Response.class);
        };

        out.add(new Packet(null, type, decodedPayload));
    }
}
