I have successfully implemented the hot key detection reporting and pulling mechanism in the CLI side (`hotkey-spring-boot-starter`) and integrated it with the communication module (`hkcache-remoting`).

### Completed Tasks

1.  **hkcache-remoting Module Updates**:
    -   Added `HotKeyRemotingClient`: A high-level client wrapper for sending `AccessReportMessage` (OneWay) and querying `HotKeyViewMessage` (Sync).
    -   Added `JdkSerializer`: A default `Serializer` implementation using Java Serialization.
    -   Installed the module to the local Maven repository to make it available for the starter.

2.  **hotkey-spring-boot-starter Module Updates**:
    -   **Dependencies**: Added dependency on `hkcache-remoting`.
    -   **HotKeySet**: Updated to support versioning (`AtomicLong version`) and an atomic update method (`update(Iterable<String> keys, long newVersion)`).
    -   **HotKeyDetector**: Implemented the core logic:
        -   **Data Collection**: Uses a `ConcurrentHashMap<String, LongAdder>` buffer to accumulate access counts efficiently.
        -   **Reporting**: A scheduled task (every 5s) aggregates the buffer into an `AccessReportMessage` and sends it to the server.
        -   **Polling**: A scheduled task (every 1s) polls the server for hot keys using `HotKeyRemotingClient`. It compares the returned version with the local version and updates `HotKeySet` if newer.
    -   **HotKeyAutoConfiguration**: Updated Spring configuration to:
        -   Create `NettyClient`, `ClientRequestSender`, `JdkSerializer`, and `HotKeyRemotingClient` beans.
        -   Inject these dependencies into `HotKeyDetector`.

### Verification
-   Ran `mvn install` on `hkcache-remoting` successfully.
-   Ran `mvn clean compile` on `hotkey-spring-boot-starter` successfully.

The CLI is now ready to report access data and receive hot key updates from the server.
