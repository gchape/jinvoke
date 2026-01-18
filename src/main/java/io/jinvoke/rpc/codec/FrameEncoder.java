package io.jinvoke.rpc.codec;

import com.alibaba.fastjson2.JSON;
import io.jinvoke.rpc.protocol.Frame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameEncoder extends MessageToByteEncoder<Frame> {
    private static final Logger logger = LoggerFactory.getLogger(FrameEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame frame, ByteBuf out) {
        try {
            byte[] payload = frame.hasPayload()
                    ? JSON.toJSONBytes(frame.payload())
                    : new byte[0];

            out.writeByte(frame.type().toOrdinal());
            out.writeInt(payload.length);

            if (payload.length > 0) {
                out.writeBytes(payload);
            }

            logger.debug("Encoded frame: type={}, payloadSize={}",
                    frame.type(), payload.length);
        } catch (Exception e) {
            logger.error("Failed to encode frame: {}", frame, e);
            ctx.fireExceptionCaught(e);
        }
    }
}
