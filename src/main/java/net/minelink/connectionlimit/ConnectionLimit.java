package net.minelink.connectionlimit;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;

public class ConnectionLimit extends Plugin implements Listener {
    private Configuration config;
    private final TObjectIntMap<InetAddress> addresses = new TObjectIntHashMap<>();

    @Override
    public void onEnable() {
        try {
            loadConfig();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getProxy().getPluginManager().registerListener(this, this);
    }

    private void loadConfig() throws IOException {
        File configFile = new File(getDataFolder(), "config.yml");

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            }
        }

        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }

    private boolean isExempt(InetAddress address) {
        return address.isSiteLocalAddress() || address.isLoopbackAddress();
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        InetAddress address = event.getConnection().getAddress().getAddress();

        if (!isExempt(address) && addresses.get(address) >= config.getInt("limit")) {
            event.setCancelReason("Too many connections from this IP address.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        addresses.adjustOrPutValue(event.getPlayer().getAddress().getAddress(), 1, 1);
    }

    @EventHandler
    public void disconnect(PlayerDisconnectEvent event) {
        InetAddress address = event.getPlayer().getAddress().getAddress();

        addresses.adjustValue(address, -1);

        if (addresses.get(address) <= 0) {
            addresses.remove(address);
        }
    }
}
