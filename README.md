# JInvoke RPC Framework

A lightweight, type-safe RPC framework for Java built on Netty with modern Java 17+ features.

![svgviewer-output](https://github.com/user-attachments/assets/4c844d90-8562-4bb5-b8fc-8ac928127a2d)

## Key Features

- **Hub-and-Spoke Architecture**: Central server routes invocations between distributed clients
- **Type Safety**: Sealed interfaces and records with exhaustive pattern matching
- **Async by Default**: CompletableFuture-based invocation tracking
- **Reflection-Based**: Automatic method discovery via `@Rpc` annotation scanning
- **Binary Protocol**: Efficient frame-based encoding with FastJSON serialization

## Architecture

![mermaid-diagram-2026-01-20T08-46-42](https://github.com/user-attachments/assets/be6d4d64-6bd3-42ec-b245-73822ee64743)

The framework uses a three-tier architecture:
1. **Protocol Layer**: Immutable value objects (Frame, InvocationRequest, InvocationResult)
2. **Transport Layer**: Unified codec for bidirectional frame serialization
3. **Application Layer**: Client/Server handlers with session management

## Quick Start

**Define an RPC service:**
```java
public class MathService {
    @Rpc
    public int add(int a, int b) {
        return a + b;
    }
}
```

**Enable RPC client:**
```java
@SpringBootApplication
@EnableRpc(clientId = "client-a", basePackages = "com.example.services")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Start the server:**
```java
new RpcServer(8888).start();
```

## Protocol

**Message Types:**
- `REGISTER`: Client handshake with server
- `INVOKE`: Request method execution on remote client
- `FORWARD`: Server routes request to target client
- `RESULT`: Return value or exception from execution
- `HEARTBEAT`: Keep-alive mechanism

**Frame Format:**
```
[MessageType:1][PayloadLength:4][Payload:N]
```

## License

MIT
