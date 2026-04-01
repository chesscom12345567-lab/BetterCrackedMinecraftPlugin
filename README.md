# BypassOnlineMode Plugin for Paper 1.21.11

This plugin allows a specific username (default `hydriel`) to join the server without Mojang authentication (cracked), even if `online-mode` is set to `true` in `server.properties`.

## Features
- **Online-Mode Bypass**: Only the target user bypasses authentication. Everyone else still needs a premium account.
- **Selective**: Configure which user gets to bypass.
- **NMS Powered**: Uses Netty channel injection for low-level protocol manipulation.

## How it Works
1. When a player tries to join, the plugin intercepts the "Hello" packet.
2. If the username matches `target-username`, it flags the connection.
3. When the server tries to send an **Encryption Request**, the plugin blocks it and manually advances the login state to "Success" using an offline UUID.
4. The server accepts the player as if they were in offline mode, while others are still processed via Mojang.

## How to Build
To compile the plugin into a JAR file, follow these steps:

1.  Make sure you have **Java 21** installed.
2.  Run the Gradle build command:
    ```bash
    ./gradlew build
    ```
    (Note: If you don't have the wrapper, you can run `gradle build` if installed).
3.  The resulting JAR will be in `build/libs/BypassOnlineMode-1.0-SNAPSHOT.jar`.

## Configuration
Edit `plugins/BypassOnlineMode/config.yml`:
```yaml
target-username: "hydriel"
```

## Security Warning
> [!WARNING]
> By allowing `hydriel` to join without authentication, **anyone** with a cracked client can join as `hydriel`. It is highly recommended to use a secondary authentication plugin (like AuthMe) or use an IP whitelist if possible.
