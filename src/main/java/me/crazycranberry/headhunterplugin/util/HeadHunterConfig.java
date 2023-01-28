package me.crazycranberry.headhunterplugin.util;

import org.bukkit.configuration.file.YamlConfiguration;

public class HeadHunterConfig {
    boolean display_score_in_player_list = true;
    boolean looting_enchantment_affects_drop_rate = false;
    double looting_enchantment_drop_rate_multiplier = 0.1;

    public HeadHunterConfig(YamlConfiguration config) {
        loadConfig(config);
    }

    public void loadConfig(YamlConfiguration config) {
        display_score_in_player_list = config.getBoolean("display_score_in_player_list", true);
        looting_enchantment_affects_drop_rate = config.getBoolean("looting_enchantment.affects_drop_rate", false);
        looting_enchantment_drop_rate_multiplier = config.getDouble("looting_enchantment.drop_rate_multiplier", 0.1);
    }

    public boolean display_score() {
        return display_score_in_player_list;
    }

    public boolean looting_matters() {
        return looting_enchantment_affects_drop_rate;
    }

    public double looting_multiplier() {
        return looting_enchantment_drop_rate_multiplier;
    }
}
