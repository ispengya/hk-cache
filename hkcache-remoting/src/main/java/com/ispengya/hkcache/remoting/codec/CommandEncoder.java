package com.ispengya.hkcache.remoting.codec;

import com.ispengya.hkcache.remoting.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * CommandEncoder 负责将 {@link Command} 对象编码为字节流。
 *
 * <p>编码格式为：<code>length(4 bytes) + type(4 bytes) + payload</code>，
 * 其中 length 表示后续 type 和 payload 的总字节长度。</p>
 */
public class CommandEncoder extends MessageToByteEncoder<Command> {

    /**
     * 将 Command 对象写入 ByteBuf。
     *
     * @param ctx Netty 上下文
     * @param msg 待编码的命令
     * @param out 目标缓冲区
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Command msg, ByteBuf out) {
        byte[] payload = msg.getPayload();
        int payloadLength = payload == null ? 0 : payload.length;

        // length 字段：type(4 字节) + payload 长度
        out.writeInt(4 + payloadLength);

        // 写入命令类型
        out.writeInt(msg.getType().ordinal());

        // 写入负载数据
        if (payload != null) {
            out.writeBytes(payload);
        }
    }
}
