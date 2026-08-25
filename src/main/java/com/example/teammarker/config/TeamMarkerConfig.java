package com.example.teammarker.config;

import java.util.ArrayList;
import java.util.List;

public class TeamMarkerConfig {

    public boolean enable = true;

    public boolean glowEnabled = true;

    public String prefixText = "[队友]";

    public String color = "e";

    public List<String> playerNameList = new ArrayList<>();

    public boolean containsPlayerIgnoreCase(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (playerNameList == null) {
            return false;
        }
        for (String entry : playerNameList) {
            if (entry != null && entry.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
