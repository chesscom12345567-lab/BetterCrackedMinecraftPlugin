package com.github.hydriel.bypass;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import java.lang.reflect.Field;

public class BypassHandler extends ChannelDuplexHandler {

    private final BypassPlugin plugin;
    private final Channel channel;
    private static Field onlineModeField = null;

    public BypassHandler(BypassPlugin plugin, Channel channel) {
        this.plugin = plugin;
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof ServerboundHelloPacket packet) {
                String name = packet.name();
                if (name.equalsIgnoreCase(plugin.getTargetUsername())) {
                    plugin.getLogger().info("[GHOST] Identified login for " + name + ". Temporarily disabling online-mode check...");
                    
                    MinecraftServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
                    
                    // Find the onlineMode field if not already found
                    if (onlineModeField == null) {
                        for (Field f : MinecraftServer.class.getDeclaredFields()) {
                            if (f.getType() == boolean.class) {
                                // Mojang mapping: onlineMode
                                if (f.getName().equals("onlineMode") || f.getName().equals("usesAuthentication")) {
                                    onlineModeField = f;
                                    break;
                                }
                            }
                        }
                        
                        // Fallback: search for any boolean that is currently true (premium server)
                        if (onlineModeField == null) {
                             for (Field f : MinecraftServer.class.getDeclaredFields()) {
                                 if (f.getType() == boolean.class) {
                                     f.setAccessible(true);
                                     if (f.getBoolean(server)) {
                                         onlineModeField = f;
                                         break;
                                     }
                                 }
                             }
                        }
                    }

                    if (onlineModeField != null) {
                        onlineModeField.setAccessible(true);
                        boolean originalValue = onlineModeField.getBoolean(server);
                        
                        // Synchronize on server to prevent other connections from being affected
                        synchronized (server) {
                            onlineModeField.setBoolean(server, false);
                            try {
                                super.channelRead(ctx, msg);
                                return;
                            } finally {
                                onlineModeField.setBoolean(server, originalValue);
                                plugin.getLogger().info("[GHOST] Successfully restored onlineMode to " + originalValue);
                            }
                        }
                    } else {
                        plugin.getLogger().severe("[ERROR] Could not find the 'onlineMode' field via reflection.");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in Ghost Toggle channelRead: " + e.getMessage());
            e.printStackTrace();
        }
        super.channelRead(ctx, msg);
    }
}
