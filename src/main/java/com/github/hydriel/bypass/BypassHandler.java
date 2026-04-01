package com.github.hydriel.bypass;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.network.Connection;
import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;

public class BypassHandler extends ChannelDuplexHandler {

    private final BypassPlugin plugin;
    private final Channel channel;
    private boolean bypassing = false;

    public BypassHandler(BypassPlugin plugin, Channel channel) {
        this.plugin = plugin;
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServerboundHelloPacket packet) {
            String name = packet.name();
            if (name.equalsIgnoreCase(plugin.getTargetUsername())) {
                plugin.getLogger().info("Intercepted login for " + name + ". Bypassing authentication...");
                this.bypassing = true;
                
                // We let the packet go through to register the player in the server's listener
                // BUT we need to handle the server's response (Encryption Request)
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
        if (this.bypassing && msg instanceof ClientboundHelloPacket) {
            plugin.getLogger().info("Blocking encryption request for bypass user.");
            // We block the encryption request to the client
            // This keeps the client in 'offline mode' (plain text)
            
            // Now we must manually trigger the server to advance to the next state
            // The server is currently in handleHello() waiting for the client to respond to the encryption request
            // We need to bypass that and call the finish method.
            
            ctx.channel().eventLoop().execute(() -> {
                try {
                    advanceLogin(ctx);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to advance login state: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // Don't call super.write() to block the packet
            return;
        }
        super.write(ctx, msg, promise);
    }

    private void advanceLogin(ChannelHandlerContext ctx) throws Exception {
        Object handler = ctx.pipeline().get("packet_handler");
        ServerLoginPacketListenerImpl loginListener = null;

        if (handler instanceof ServerLoginPacketListenerImpl) {
            loginListener = (ServerLoginPacketListenerImpl) handler;
        } else if (handler instanceof Connection connection) {
            if (connection.getPacketListener() instanceof ServerLoginPacketListenerImpl) {
                loginListener = (ServerLoginPacketListenerImpl) connection.getPacketListener();
            }
        }

        if (loginListener == null) {
            plugin.getLogger().warning("Could not find ServerLoginPacketListenerImpl in pipeline.");
            return;
        }
        
        String username = plugin.getTargetUsername();
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
        GameProfile profile = new GameProfile(offlineUuid, username);
        
        // Find startClientVerification(GameProfile)
        Method startMethod = null;
        for (Method m : ServerLoginPacketListenerImpl.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(GameProfile.class)) {
                startMethod = m;
                break;
            }
        }
        
        if (startMethod != null) {
            startMethod.setAccessible(true);
            startMethod.invoke(loginListener, profile);
            plugin.getLogger().info("Successfully forced offline login sequence for " + username);
        } else {
            plugin.getLogger().severe("Could not find startClientVerification method! Paper mappings might have changed.");
        }
    }
}
