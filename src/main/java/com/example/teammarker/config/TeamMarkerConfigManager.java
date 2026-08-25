package com.example.teammarker.config;

import com.example.teammarker.TeamMarkerClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TeamMarkerConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("teammarker.json");

    private static TeamMarkerConfig CONFIG = new TeamMarkerConfig();

    public static TeamMarkerConfig getConfig() {
        return CONFIG;
    }

    public static TeamMarkerConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    TeamMarkerConfig loaded = GSON.fromJson(reader, TeamMarkerConfig.class);
                    if (loaded != null) {
                        CONFIG = sanitize(loaded);
                    } else {
                        CONFIG = new TeamMarkerConfig();
                    }
                }
            } else {
                CONFIG = new TeamMarkerConfig();
                save();
            }
        } catch (Exception e) {
            TeamMarkerClient.LOGGER.error("[TeamMarker] 读取配置失败，使用默认配置", e);
            CONFIG = new TeamMarkerConfig();
        }
        return CONFIG;
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(CONFIG, writer);
            }
        } catch (IOException e) {
            TeamMarkerClient.LOGGER.error("[TeamMarker] 保存配置失败", e);
        }
    }

    private static TeamMarkerConfig sanitize(TeamMarkerConfig c) {
        if (c == null) {
            return new TeamMarkerConfig();
        }
        if (c.prefixText == null) c.prefixText = "[队友]";
        if (c.color == null) c.color = "e";
        if (c.playerNameList == null) c.playerNameList = new ArrayList<>();
        c.playerNameList.removeIf(item -> item == null || item.isEmpty());
        return c;
    }

    public static boolean isEnabled() {
        return CONFIG.enable;
    }

    public static boolean isGlowEnabled() {
        return CONFIG.glowEnabled;
    }

    public static void toggleGlow() {
        CONFIG.glowEnabled = !CONFIG.glowEnabled;
        save();
    }

    public static void toggle() {
        CONFIG.enable = !CONFIG.enable;
        save();
    }

    public static void setPrefix(String newPrefix) {
        CONFIG.prefixText = (newPrefix == null) ? "" : newPrefix;
        save();
    }

    public static String getPrefix() {
        return CONFIG.prefixText == null ? "" : CONFIG.prefixText;
    }

    public static String getColorCode() {
        return CONFIG.color == null ? "e" : CONFIG.color;
    }

    public static boolean addPlayer(String name) {
        if (name == null || name.isBlank()) return false;
        if (CONFIG.playerNameList == null) {
            CONFIG.playerNameList = new ArrayList<>();
        }
        for (String entry : CONFIG.playerNameList) {
            if (entry != null && entry.equalsIgnoreCase(name)) {
                return false;
            }
        }
        CONFIG.playerNameList.add(name);
        save();
        return true;
    }

    public static boolean removePlayer(String name) {
        if (name == null || name.isBlank()) return false;
        if (CONFIG.playerNameList == null) return false;
        boolean removed = CONFIG.playerNameList.removeIf(
                entry -> entry != null && entry.equalsIgnoreCase(name)
        );
        if (removed) save();
        return removed;
    }

    public static List<String> getPlayerList() {
        if (CONFIG.playerNameList == null) return new ArrayList<>();
        return new ArrayList<>(CONFIG.playerNameList);
    }

    public static boolean containsPlayer(String name) {
        return CONFIG.containsPlayerIgnoreCase(name);
    }
}
