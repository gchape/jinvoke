package io.jinvoke.rpc.codec;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.jinvoke.rpc.protocol.Frame;
import io.jinvoke.rpc.protocol.InvocationRequest;
import io.jinvoke.rpc.protocol.InvocationResult;
import io.jinvoke.rpc.protocol.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(FrameDecoder.class);
    private static final int HEADER_SIZE = 5;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();

        byte messageTypeByte = in.readByte();
        int payloadLength = in.readInt();

        if (payloadLength < 0) {
            logger.error("Invalid payload length: {}", payloadLength);
            ctx.close();
            return;
        }

        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex();
            return;
        }

        try {
            Protocol.MessageType type = Protocol.MessageType.fromByte(messageTypeByte);

            byte[] payloadBytes = new byte[payloadLength];
            if (payloadLength > 0) {
                in.readBytes(payloadBytes);
            }

            Frame frame = decodeFrame(type, payloadBytes);
            out.add(frame);

            logger.debug("Decoded frame: type={}, payloadSize={}", type, payloadLength);
        } catch (Exception e) {
            logger.error("Failed to decode frame", e);
            ctx.fireExceptionCaught(e);
        }
    }

    private Frame decodeFrame(Protocol.MessageType type, byte[] payloadBytes) {
        if (payloadBytes.length == 0) {
            return createControlFrame(type);
        }

        JSONObject json = JSON.parseObject(payloadBytes);

        Protocol.Payload payload = switch (type) {
            case INVOKE, FORWARD -> json.to(InvocationRequest.class);
            case RESULT -> json.to(InvocationResult.class);
            default -> throw new IllegalArgumentException(
                    "Message type " + type + " should not have payload"
            );
        };

        String messageId = json.getString("requestId");
        return new Frame(messageId, type, payload);
    }

    private Frame createControlFrame(Protocol.MessageType type) {
        return switch (type) {
            case REGISTER -> Frame.register(generateMessageId());
            case HEARTBEAT -> Frame.heartbeat();
            default -> throw new IllegalArgumentException(
                    "Control frame type expected, got: " + type
            );
        };
    }

    private String generateMessageId() {
        return "msg-" + System.nanoTime();
    }
}
