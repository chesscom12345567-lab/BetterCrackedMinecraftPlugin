package com.github.hydriel.bypass;

import org.bukkit.plugin.java.JavaPlugin;

public class BypassPlugin extends JavaPlugin {

    private NettyInjector injector;
    private String targetUsername;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        targetUsername = getConfig().getString("target-username", "hydriel");
        
        getLogger().info("BypassOnlineMode enabled for user: " + targetUsername);

        injector = new NettyInjector(this);
        injector.inject();
    }

    @Override
    public void onDisable() {
        if (injector != null) {
            injector.uninject();
        }
    }

    public String getTargetUsername() {
        return targetUsername;
    }
}
