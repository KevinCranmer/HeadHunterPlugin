package me.crazycranberry.headhunterplugin;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.bukkit.Bukkit.getServer;

public class CommandManager implements CommandExecutor {
    Server server;
    List<String> validMobs;
    YamlConfiguration chanceConfig;
    YamlConfiguration kcLogConfig;
    YamlConfiguration headLogConfig;

    public CommandManager(@NotNull Server server, @NotNull YamlConfiguration chanceConfig, @NotNull YamlConfiguration kcLogConfig, @NotNull YamlConfiguration headLogConfig) {
        this.server = server;
        this.chanceConfig = chanceConfig;
        this.kcLogConfig = kcLogConfig;
        this.headLogConfig = headLogConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("kc") || command.getName().equalsIgnoreCase("hc")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args.length < 1) {
                    p.sendMessage("You must provide a mob name (example: FROG_COLD)");
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
                        p.sendMessage("That mob does not exist. Try '/mobs' to view a list of all mobs.");
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
        getServer().broadcastMessage(String.format("%s%s%s has killed %s %s%s%s's%s", ChatColor.AQUA, p.getDisplayName(), ChatColor.GRAY, kc, ChatColor.AQUA, mob, ChatColor.GRAY, ChatColor.RESET));
    }

    private void printHcMessage(Player p, int hc, String mob) {
        getServer().broadcastMessage(String.format("%s%s%s has received %s %s%s%s heads%s", ChatColor.AQUA, p.getDisplayName(), ChatColor.GRAY, hc, ChatColor.AQUA, mob, ChatColor.GRAY, ChatColor.RESET));
    }

    private void printHeadsMessage(Player p) {
        ConfigurationSection cs = headLogConfig.getConfigurationSection(p.getDisplayName());
        if (cs == null) {
            getServer().broadcastMessage(String.format("%s%s%s ain't get no head%s", ChatColor.AQUA, p.getDisplayName(), ChatColor.GRAY, ChatColor.RESET));
            return;
        }
        getServer().broadcastMessage(String.format("%s%s%s has received the following heads: %s%s%s ", ChatColor.AQUA, p.getDisplayName(), ChatColor.GRAY, ChatColor.AQUA, cs.getKeys(false), ChatColor.RESET));
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
}
