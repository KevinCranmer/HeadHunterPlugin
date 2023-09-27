package me.crazycranberry.headhunterplugin;

import me.crazycranberry.headhunterplugin.util.HeadHunterConfig;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.bukkit.Bukkit.getServer;

public class CommandManager implements CommandExecutor, TabCompleter {
    Server server;
    List<String> validMobs;
    YamlConfiguration chanceConfig;
    YamlConfiguration kcLogConfig;
    YamlConfiguration headLogConfig;
    HeadHunterConfig headHunterConfig;

    public CommandManager(@NotNull Server server, @NotNull YamlConfiguration chanceConfig, @NotNull YamlConfiguration kcLogConfig, @NotNull YamlConfiguration headLogConfig, @NotNull HeadHunterConfig headHunterConfig) {
        this.server = server;
        this.chanceConfig = chanceConfig;
        this.kcLogConfig = kcLogConfig;
        this.headLogConfig = headLogConfig;
        this.headHunterConfig = headHunterConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("kc") || command.getName().equalsIgnoreCase("hc")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args.length < 1) {
                    p.sendMessage(headHunterConfig.missing_mob_name_message());
                    return false;
                }
                String mob = args[0].toUpperCase();
                boolean isKcCommand = command.getName().equalsIgnoreCase("kc");
                YamlConfiguration config = isKcCommand ? kcLogConfig : headLogConfig;
                if (!config.contains(p.getDisplayName()) || !Objects.requireNonNull(config.getConfigurationSection(p.getDisplayName())).contains(mob)) {
                    if (getValidMobsList().contains(mob)) {
                        if (isKcCommand) {
                            printKcMessage(p, 0, mob);
                        } else {
                            printHcMessage(p, 0, mob);
                        }
                    } else {
                        p.sendMessage(headHunterConfig.invalid_mob_name_message());
                    }
                    return true;
                }
                int count = Objects.requireNonNull(config.getConfigurationSection(p.getDisplayName())).getInt(mob);
                if (isKcCommand) {
                    printKcMessage(p, count, mob);
                } else {
                    printHcMessage(p, count, mob);
                }
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("mobs")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.sendMessage(getValidMobsList().toString());
            }
        } else if (command.getName().equalsIgnoreCase("heads")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                printHeadsMessage(p);
            }
        }
        return true;
    }

    private void printKcMessage(Player p, int kc, String mob) {
        String kcMessage = headHunterConfig.kill_count_message()
                .replace("{PLAYER_NAME}", String.format("%s%s%s",  ChatColor.AQUA, p.getDisplayName(), ChatColor.GRAY))
                .replace("{NUMBER}", String.valueOf(kc))
                .replace("{MOB_NAME}", String.format("%s%s%s",  ChatColor.AQUA, mob, ChatColor.GRAY));
        getServer().broadcastMessage(ChatColor.GRAY + kcMessage + ChatColor.RESET);
    }

    private void printHcMessage(Player p, int hc, String mob) {
        String hcMessage = headHunterConfig.head_count_message()
                .replace("{PLAYER_NAME}", String.format("%s%s%s",  ChatColor.AQUA, p.getDisplayName(), ChatColor.GRAY))
                .replace("{NUMBER}", String.valueOf(hc))
                .replace("{MOB_NAME}", String.format("%s%s%s",  ChatColor.AQUA, mob, ChatColor.GRAY));
        getServer().broadcastMessage(ChatColor.GRAY + hcMessage + ChatColor.RESET);
    }

    private void printHeadsMessage(Player p) {
        ConfigurationSection cs = headLogConfig.getConfigurationSection(p.getDisplayName());
        String headsMessage = headHunterConfig.heads_message()
                .replace("{PLAYER_NAME}", String.format("%s%s%s",  ChatColor.AQUA, p.getDisplayName(), ChatColor.GRAY))
                .replace("{NUMBER}", cs == null ? "0" : String.valueOf(cs.getKeys(false).size()))
                .replace("{TOTAL}", String.valueOf(getValidMobsList().size()))
                .replace("{HEAD_LIST}", String.format("%s%s%s",  ChatColor.AQUA, cs == null ? "[]" : cs.getKeys(false).toString(), ChatColor.GRAY));
        getServer().broadcastMessage(ChatColor.GRAY + headsMessage + ChatColor.RESET);
    }

    /** Get a valid list of mobs based on the chance_config.yml. */
    private List<String> getValidMobsList() {
        if (validMobs != null) {
            return validMobs;
        }
        List<String> l = new ArrayList<>();
        ConfigurationSection cs = chanceConfig.getConfigurationSection("chance_percent");
        if (cs == null) {
            return l;
        }
        Set<String> keys = cs.getKeys(false);
        for (String key : keys) {
            if (cs.isDouble(key)) {
                l.add(key.toUpperCase());
            } else if (cs.isConfigurationSection(key)) {
                ConfigurationSection innerCs = cs.getConfigurationSection(key);
                if (innerCs == null) {
                    continue;
                }
                for (String innerKey : innerCs.getKeys(false)) {
                    if (innerCs.isDouble(innerKey)) {
                        l.add(String.format("%s_%s", key.toUpperCase(), innerKey.toUpperCase()));
                    }
                }
            }
        }
        validMobs = l;
        return validMobs;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player && (command.getName().equalsIgnoreCase("kc") || command.getName().equalsIgnoreCase("hc")) && args.length == 1) {
            return getValidMobsList().stream().filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return null;
    }
}
