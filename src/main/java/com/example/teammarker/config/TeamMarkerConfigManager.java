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
        if (c == null) return new TeamMarkerConfig();
        if (c.prefixText == null) c.prefixText = "[队友]";
        if (c.color == null) c.color = "e";
        if (c.playerNameList == null) c.playerNameList = new ArrayList<>();
        c.playerNameList.removeIf(item -> item == null || item.isEmpty());
        if (c.teams == null) c.teams = new ArrayList<>();
        for (TeamMarkerConfig.Team t : c.teams) {
            if (t == null) continue;
            if (t.name == null) t.name = "";
            if (t.prefix == null) t.prefix = "[T]";
            if (t.color == null) t.color = "e";
            if (t.players == null) t.players = new ArrayList<>();
            t.players.removeIf(item -> item == null || item.isEmpty());
        }
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
        if (CONFIG.playerNameList == null) CONFIG.playerNameList = new ArrayList<>();
        for (String entry : CONFIG.playerNameList) {
            if (entry != null && entry.equalsIgnoreCase(name)) return false;
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
        boolean removedFromTeams = removeFromAllTeams(name);
        if (removed || removedFromTeams) {
            save();
            return true;
        }
        return false;
    }

    public static List<String> getPlayerList() {
        if (CONFIG.playerNameList == null) return new ArrayList<>();
        return new ArrayList<>(CONFIG.playerNameList);
    }

    public static boolean containsPlayer(String name) {
        return CONFIG.containsPlayerIgnoreCase(name);
    }

    public static List<TeamMarkerConfig.Team> getTeams() {
        if (CONFIG.teams == null) {
            CONFIG.teams = new ArrayList<>();
        }
        return CONFIG.teams;
    }

    public static boolean createTeam(String teamName, String prefix, String color) {
        if (teamName == null || teamName.isBlank()) return false;
        if (CONFIG.teams == null) CONFIG.teams = new ArrayList<>();
        for (TeamMarkerConfig.Team t : CONFIG.teams) {
            if (t != null && t.name != null && t.name.equalsIgnoreCase(teamName)) return false;
        }
        TeamMarkerConfig.Team t = new TeamMarkerConfig.Team();
        t.name = teamName;
        t.prefix = (prefix == null || prefix.isEmpty()) ? "[T]" : prefix;
        t.color = (color == null || color.isEmpty()) ? "e" : color.substring(0, 1);
        t.players = new ArrayList<>();
        CONFIG.teams.add(t);
        save();
        return true;
    }

    public static boolean deleteTeam(String teamName) {
        if (teamName == null || teamName.isBlank() || CONFIG.teams == null) return false;
        boolean removed = CONFIG.teams.removeIf(
                t -> t != null && t.name != null && t.name.equalsIgnoreCase(teamName)
        );
        if (removed) save();
        return removed;
    }

    public static TeamMarkerConfig.Team findTeamOfPlayer(String playerName) {
        return CONFIG.findTeamOfPlayer(playerName);
    }

    public static TeamMarkerConfig.Team findTeamByName(String teamName) {
        return CONFIG.findTeamByName(teamName);
    }

    public static boolean addPlayerToTeam(String playerName, String teamName) {
        if (playerName == null || playerName.isBlank()) return false;
        if (teamName == null || teamName.isBlank()) return false;
        if (CONFIG.teams == null) CONFIG.teams = new ArrayList<>();
        TeamMarkerConfig.Team target = findTeamByName(teamName);
        if (target == null) return false;
        removeFromAllTeams(playerName);
        if (target.players == null) target.players = new ArrayList<>();
        for (String e : target.players) {
            if (e != null && e.equalsIgnoreCase(playerName)) return false;
        }
        target.players.add(playerName);
        if (CONFIG.playerNameList != null) {
            CONFIG.playerNameList.removeIf(e -> e != null && e.equalsIgnoreCase(playerName));
        }
        save();
        return true;
    }

    public static boolean removeFromAllTeams(String playerName) {
        if (playerName == null || playerName.isBlank() || CONFIG.teams == null) return false;
        boolean any = false;
        for (TeamMarkerConfig.Team t : CONFIG.teams) {
            if (t == null) continue;
            if (t.removePlayer(playerName)) any = true;
        }
        return any;
    }

    public static String getPlayerPrefix(String playerName) {
        TeamMarkerConfig.Team t = findTeamOfPlayer(playerName);
        if (t != null) return t.prefix == null ? "[T]" : t.prefix;
        if (containsPlayer(playerName)) return getPrefix();
        return "";
    }

    public static String getPlayerColorCode(String playerName) {
        TeamMarkerConfig.Team t = findTeamOfPlayer(playerName);
        if (t != null) return t.color == null ? "e" : t.color;
        if (containsPlayer(playerName)) return getColorCode();
        return "";
    }

    public static boolean isPlayerMarked(String playerName) {
        if (findTeamOfPlayer(playerName) != null) return true;
        return containsPlayer(playerName);
    }
}
