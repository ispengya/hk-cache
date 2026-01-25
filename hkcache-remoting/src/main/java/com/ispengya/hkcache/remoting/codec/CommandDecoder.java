package com.ispengya.hkcache.remoting.codec;

import com.ispengya.hkcache.remoting.protocol.Command;
import com.ispengya.hkcache.remoting.protocol.CommandType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * CommandDecoder 基于 length-field 协议，将字节流解码为 {@link Command} 对象。
 *
 * <p>解码规则与 {@link CommandEncoder} 对应：第一段为 length 字段，
 * 之后是 type 和 payload。</p>
 */
public class CommandDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * 创建解码器。
     *
     * @param maxFrameLength 单帧允许的最大长度，避免异常请求导致内存占用过大
     */
    public CommandDecoder(int maxFrameLength) {
        // lengthFieldOffset=0, lengthFieldLength=4, lengthAdjustment=0, initialBytesToStrip=4
        super(maxFrameLength, 0, 4, 0, 4);
    }

    /**
     * 从底层 ByteBuf 中解析出一条完整的 Command。
     *
     * @param ctx Netty 上下文
     * @param in  输入缓冲区
     * @return 解析出的 Command；如果帧不完整则返回 null
     * @throws Exception 解码异常
     */
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            int typeOrdinal = frame.readInt();
            CommandType type = CommandType.values()[typeOrdinal];

            int payloadLength = frame.readableBytes();
            byte[] payload = new byte[payloadLength];
            frame.readBytes(payload);

            return new Command(type, payload);
        } finally {
            frame.release();
        }
    }
}
