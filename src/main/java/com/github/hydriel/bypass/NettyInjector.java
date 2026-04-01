package com.github.hydriel.bypass;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class NettyInjector {

    private final BypassPlugin plugin;
    private List<Connection> connections;

    public NettyInjector(BypassPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void inject() {
        try {
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
            ServerConnectionListener listener = server.getConnection();
            
            // In 1.21.1, ServerConnection handles the pipeline
            // We can add a ChannelInitializer to the server's listener
            Field connectionsField = ServerConnectionListener.class.getDeclaredField("connections");
            connectionsField.setAccessible(true);
            this.connections = Collections.synchronizedList((List<Connection>) connectionsField.get(listener));

            // We need to inject into the pipeline of new connections.
            // The cleanest way is to use a ChannelInitializer on the server's acceptor channel.
            // But an easier way for a simple bypass is to periodically check the connections list 
            // OR use a Bukkit listener for login start if possible (unlikely for encryption bypass).
            
            // Actually, the most robust way is to inject a handler into the pipeline *now* 
            // but we need to do it for EVERY new connection.
            // We can hook into the channel initializer.
            
            injectServerChannel();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to inject Netty handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void injectServerChannel() throws Exception {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
        ServerConnectionListener listener = server.getConnection();
        
        // This is the list of future channels (listeners)
        Field channelsField = ServerConnectionListener.class.getDeclaredField("channels");
        channelsField.setAccessible(true);
        List<?> channels = (List<?>) channelsField.get(listener);

        for (Object channelFuture : channels) {
            // ChannelFuture has a channel() method
            Channel serverChannel = ((io.netty.channel.ChannelFuture) channelFuture).channel();
            serverChannel.pipeline().addFirst(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast("bypass_handler", new BypassHandler(plugin, ch));
                }
            });
        }
    }

    public void uninject() {
        // Cleaning up injected handlers is ideal but tricky for per-connection handlers.
        // Usually, removing the handler from the pipeline is enough, but here we add to new channels.
    }
}
