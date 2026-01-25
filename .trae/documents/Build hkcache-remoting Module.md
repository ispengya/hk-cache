I have successfully built the `hkcache-remoting` module based on the design documents.

The module structure is as follows:

### 1. Protocol (`com.ispengya.hkcache.remoting.protocol`)
- **Command**: Defines the communication protocol packet structure (Type + Payload).
- **CommandType**: Enum for command types (ACCESS_REPORT, HOT_KEY_QUERY, etc.).
- **Serializer**: Interface for object serialization.

### 2. Message (`com.ispengya.hkcache.remoting.message`)
- **AccessReportMessage**: Message for reporting key access statistics.
- **HotKeyViewMessage**: Message for sending hot key definitions to clients.
- **HotKeyQueryRequest**: Request object for querying hot keys.

### 3. Codec (`com.ispengya.hkcache.remoting.codec`)
- **CommandEncoder**: Encodes `Command` objects into ByteBuf.
- **CommandDecoder**: Decodes ByteBuf into `Command` objects.

### 4. Server (`com.ispengya.hkcache.remoting.server`)
- **NettyServer**: Manages the Netty server lifecycle (bind, start, stop).
- **NettyServerConfig**: Configuration for server (port, threads, timeouts).
- **ServerHandler**: Inbound handler that dispatches requests.
- **ServerRequestDispatcher**: Interface for dispatching logic.
- **ServerChannelManager**: Manages active server channels.

### 5. Client (`com.ispengya.hkcache.remoting.client`)
- **NettyClient**: Manages Netty client bootstrap and connection creation.
- **NettyClientConfig**: Configuration for client (server addresses, timeouts).
- **ClientRequestSender**: Handles sending requests (OneWay and Sync) and managing responses using `CompletableFuture`.
- **ClientInboundHandler**: Handles inbound responses and completes futures.

### Verification
- **Compilation**: Successfully compiled using `mvn clean compile`.
- **Dependencies**: Added `netty-all` and `slf4j-api` to `pom.xml`.

This completes the construction of the communication module as specified in `remoting-design.md` and `project.md`.
