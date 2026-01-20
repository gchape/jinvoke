package io.jinvoke.rpc.codec;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.jinvoke.rpc.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.jinvoke.rpc.protocol.Protocol.MessageType.HEARTBEAT;

/**
 * Combined Frame encoder/decoder for Netty pipeline.
 * Format: [type:1][length:4][payload:n]
 */
public class FrameCodec extends ByteToMessageCodec<Frame> {
    private static final Logger log = LoggerFactory.getLogger(FrameCodec.class);
    private static final int HEADER_SIZE = 5; // 1 byte type + 4 bytes length

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame frame, ByteBuf out) {
        try {
            byte[] payload = frame.hasPayload()
                    ? JSON.toJSONBytes(frame.payload())
                    : new byte[0];

            out.writeByte(frame.type().toByte());
            out.writeInt(payload.length);
            if (payload.length > 0) {
                out.writeBytes(payload);
            }

            log.debug("Encoded {}: {} bytes", frame.type(), payload.length);
        } catch (Exception e) {
            log.error("Encode failed: {}", frame, e);
            throw e;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_SIZE) {
            return; // Need more data
        }

        in.markReaderIndex();

        byte typeByte = in.readByte();
        int length = in.readInt();

        if (length < 0 || length > 10_000_000) { // 10MB sanity check
            log.error("Invalid payload length: {}", length);
            ctx.close();
            return;
        }

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return; // Need more data
        }

        try {
            Protocol.MessageType type = Protocol.MessageType.fromByte(typeByte);
            Frame frame = decodeFrame(type, in, length);
            out.add(frame);
            log.debug("Decoded {}: {} bytes", type, length);
        } catch (Exception e) {
            log.error("Decode failed", e);
            ctx.fireExceptionCaught(e);
        }
    }

    private Frame decodeFrame(Protocol.MessageType type, ByteBuf in, int length) {
        if (length == 0) {
            if (type == HEARTBEAT) {
                return Frame.heartbeat();
            } else {
                throw new IllegalArgumentException(
                        "Empty payload not allowed for " + type);
            }
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        JSONObject json = JSON.parseObject(bytes);

        return switch (type) {
            case INVOKE, FORWARD -> {
                InvocationRequest req = json.to(InvocationRequest.class);
                yield new Frame(req.requestId(), type, req);
            }
            case RESULT -> {
                InvocationResult res = json.to(InvocationResult.class);
                yield new Frame(res.requestId(), type, res);
            }
            case REGISTER -> {
                Registration reg = json.to(Registration.class);
                yield Frame.register(reg.clientId());
            }
            case HEARTBEAT -> Frame.heartbeat();
        };
    }
}
