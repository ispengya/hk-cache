package com.ispengya.hkcache.remoting.protocol;

/**
 * Command 表示一次 remoting 请求或响应的通用命令模型。
 *
 * <p>命令由 {@link CommandType} 和二进制负载 payload 组成，负载由上层
 * 通过 {@code Serializer} 进行序列化和反序列化。</p>
 */
public final class Command {

    /**
     * 命令类型，用于标识本次交互的业务语义。
     */
    private final CommandType type;

    /**
     * 命令负载的二进制数据，由具体业务对象序列化而来。
     */
    private final byte[] payload;

    /**
     * 构造命令对象。
     *
     * @param type    命令类型
     * @param payload 序列化后的负载字节数组
     */
    public Command(CommandType type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    /**
     * 获取命令类型。
     *
     * @return 命令类型
     */
    public CommandType getType() {
        return type;
    }

    /**
     * 获取命令负载的二进制数据。
     *
     * @return 负载字节数组
     */
    public byte[] getPayload() {
        return payload;
    }
}
