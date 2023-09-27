package me.crazycranberry.headhunterplugin.util;

import org.bukkit.configuration.file.YamlConfiguration;

public class HeadHunterConfig {
    private boolean display_score_in_player_list = true;
    private boolean looting_enchantment_affects_drop_rate = false;
    private double looting_enchantment_drop_rate_multiplier = 0.1;
    private String head_drop = "{PLAYER_NAME} just got a {MOB_NAME} head";
    private String kill_count = "{PLAYER_NAME} has killed {NUMBER} {MOB_NAME}'s";
    private String head_count = "{PLAYER_NAME} has received {NUMBER} {MOB_NAME} heads";
    private String heads = "{PLAYER_NAME} has received {NUMBER}/{TOTAL} heads: {HEAD_LIST}";
    private String missing_mob_name = "You must provide a mob name (example: FROG_COLD)";
    private String invalid_mob_name = "That mob does not exist. Try '/mobs' to view a list of all mobs.";

    public HeadHunterConfig(YamlConfiguration config) {
        loadConfig(config);
    }

    public void loadConfig(YamlConfiguration config) {
        display_score_in_player_list = config.getBoolean("display_score_in_player_list", true);
        looting_enchantment_affects_drop_rate = config.getBoolean("looting_enchantment.affects_drop_rate", false);
        looting_enchantment_drop_rate_multiplier = config.getDouble("looting_enchantment.drop_rate_multiplier", 0.1);
        String head_drop_maybe = config.getString("messages.head_drop", head_drop);
        if (head_drop_maybe.contains("{PLAYER_NAME}") && head_drop_maybe.contains("{MOB_NAME}")) {
            head_drop = head_drop_maybe;
        }
        String kill_count_maybe = config.getString("messages.kill_count", kill_count);
        if (kill_count_maybe.contains("{PLAYER_NAME}") && kill_count_maybe.contains("{NUMBER}") && kill_count_maybe.contains("{MOB_NAME}")) {
            kill_count = kill_count_maybe;
        }
        String head_count_maybe = config.getString("messages.head_count", head_count);
        if (head_count_maybe.contains("{PLAYER_NAME}") && head_count_maybe.contains("{NUMBER}") && head_count_maybe.contains("{MOB_NAME}")) {
            head_count = head_count_maybe;
        }
        String heads_maybe = config.getString("messages.heads", heads);
        if (heads_maybe.contains("{PLAYER_NAME}") && heads_maybe.contains("{NUMBER}") && heads_maybe.contains("{TOTAL}") && heads_maybe.contains("{HEAD_LIST}")) {
            heads = heads_maybe;
        }
        missing_mob_name = config.getString("messages.command_errors.missing_mob_name", missing_mob_name);
        invalid_mob_name = config.getString("messages.command_errors.invalid_mob_name", invalid_mob_name);
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

    public String head_drop_message() {
        return head_drop;
    }

    public String kill_count_message() {
        return kill_count;
    }

    public String head_count_message() {
        return head_count;
    }

    public String heads_message() {
        return heads;
    }

    public String missing_mob_name_message() {
        return missing_mob_name;
    }

    public String invalid_mob_name_message() {
        return invalid_mob_name;
    }
}
