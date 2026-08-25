package com.example.teammarker.config;

import java.util.ArrayList;
import java.util.List;

public class TeamMarkerConfig {

    public boolean enable = true;
    public boolean glowEnabled = true;
    public String prefixText = "[队友]";
    public String color = "e";
    public List<String> playerNameList = new ArrayList<>();
    public List<Team> teams = new ArrayList<>();

    public static class Team {
        public String name = "";
        public String prefix = "[T]";
        public String color = "e";
        public List<String> players = new ArrayList<>();

        public boolean containsPlayerIgnoreCase(String name) {
            if (name == null || name.isEmpty() || players == null) return false;
            for (String e : players) {
                if (e != null && e.equalsIgnoreCase(name)) return true;
            }
            return false;
        }

        public boolean removePlayer(String name) {
            if (name == null || players == null) return false;
            return players.removeIf(e -> e != null && e.equalsIgnoreCase(name));
        }
    }

    public boolean containsPlayerIgnoreCase(String name) {
        if (name == null || name.isEmpty() || playerNameList == null) return false;
        for (String e : playerNameList) {
            if (e != null && e.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public Team findTeamOfPlayer(String name) {
        if (name == null || name.isEmpty() || teams == null) return null;
        for (Team t : teams) {
            if (t != null && t.containsPlayerIgnoreCase(name)) return t;
        }
        return null;
    }

    public Team findTeamByName(String teamName) {
        if (teamName == null || teamName.isEmpty() || teams == null) return null;
        for (Team t : teams) {
            if (t != null && t.name != null && t.name.equalsIgnoreCase(teamName)) return t;
        }
        return null;
    }
}
