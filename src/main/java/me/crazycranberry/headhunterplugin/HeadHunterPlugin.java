package me.crazycranberry.headhunterplugin;

import me.crazycranberry.headhunterplugin.util.MobHeads;
import me.crazycranberry.headhunterplugin.util.ScoreboardWrapper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Llama;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Horse;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Cat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public final class HeadHunterPlugin extends JavaPlugin implements Listener {
    public final static Logger logger = Logger.getLogger("Minecraft");
    List<String> validMobs;
    private static Field fieldProfileItem;
    YamlConfiguration chanceConfig;
    YamlConfiguration defaultChanceConfig;
    YamlConfiguration headLogConfig;
    YamlConfiguration kcLogConfig;
    ScoreboardWrapper scoreboardWrapper;

    @Override
    public void onEnable() {
        // Plugin startup logic
        loadChanceConfigs();
        loadLogConfig();
        loadKcConfig();
        registerEvents();
        scoreboardWrapper = new ScoreboardWrapper("Head Count");
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadChanceConfigs() {
        File chanceFile = new File(getDataFolder() + "" + File.separatorChar + "chance_config.yml");
        if(!chanceFile.exists()){
            saveResource("chance_config.yml", true);
            logger.info("chance_config.yml not found! copied chance_config.yml to " + getDataFolder());
        }
        chanceConfig = new YamlConfiguration();
        defaultChanceConfig = new YamlConfiguration();
        try {
            InputStream defaultChanceConfigStream = getResource("chance_config.yml");
            assert defaultChanceConfigStream != null;
            InputStreamReader defaultChanceConfigReader = new InputStreamReader(defaultChanceConfigStream);
            chanceConfig.load(chanceFile);
            defaultChanceConfig.load(defaultChanceConfigReader);
        } catch (InvalidConfigurationException | IOException e) {
            logger.info("[ ERROR ] An error occured while trying to load the chance file.");
            e.printStackTrace();
        }
    }

    private void loadLogConfig() {
        File logFile = new File(getDataFolder() + "" + File.separatorChar + "head_log.yml");
        if(!logFile.exists()){
            saveResource("head_log.yml", true);
            logger.info("head_log.yml not found! copied head_log.yml to " + getDataFolder() + "");
        }
        headLogConfig = new YamlConfiguration();
        try {
            headLogConfig.load(logFile);
        } catch (InvalidConfigurationException | IOException e) {
            logger.info("[ ERROR ] An error occured while trying to load the log file.");
            e.printStackTrace();
        }
    }

    private void loadKcConfig() {
        File logFile = new File(getDataFolder() + "" + File.separatorChar + "kc_log.yml");
        if(!logFile.exists()){
            saveResource("kc_log.yml", true);
            logger.info("kc_log.yml not found! copied kc_log.yml to " + getDataFolder() + "");
        }
        kcLogConfig = new YamlConfiguration();
        try {
            kcLogConfig.load(logFile);
        } catch (InvalidConfigurationException | IOException e) {
            logger.info("[ ERROR ] An error occured while trying to load the log file.");
            e.printStackTrace();
        }
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

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() != null) {
            String name = getTrueVictimName(event);
            double roll = Math.random();
            double dropRate = chanceConfig.getDouble("chance_percent." + name.toLowerCase(), defaultChanceConfig.getDouble(name));
            System.out.println("Rolled " + roll + " for a " + dropRate + " drop rate.");
            if (roll < dropRate) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), makeSkull(name.replace(".", "_"), entity.getKiller()));
                getServer().broadcastMessage(String.format("%s%s%s just got a %s%s%s head%s", ChatColor.LIGHT_PURPLE, entity.getKiller().getDisplayName(), ChatColor.GRAY, ChatColor.LIGHT_PURPLE, name.replaceAll("\\.", "_"),  ChatColor.GRAY, ChatColor.RESET));
                logKillOrDrop(entity.getKiller(), name.replace(".", "_"), headLogConfig);
                scoreboardWrapper.updateScore(entity.getKiller(), headLogConfig);
            }
            logKillOrDrop(entity.getKiller(), name.replace(".", "_"), kcLogConfig);
        }
    }

    private void logKillOrDrop(Player killer, String victim, YamlConfiguration config) {
        if (config.contains(String.format("%s.%s", killer.getDisplayName(), victim))) {
            config.set(String.format("%s.%s", killer.getDisplayName(), victim), config.getInt(String.format("%s.%s", killer.getDisplayName(), victim)) + 1);
        } else if (config.contains(killer.getDisplayName())) {
            ConfigurationSection cs = config.getConfigurationSection(killer.getDisplayName());
            if (cs == null) {
                System.out.printf("For some weird reason, %s has a null ConfigurationSection?", killer.getDisplayName());
                return;
            }
            cs.set(victim, 1);
        } else {
            ConfigurationSection cs = config.createSection(killer.getDisplayName());
            cs.set(victim, 1);
        }
    }

    private String getTrueVictimName(EntityDeathEvent event) {
        String name = event.getEntityType().name().replaceAll(" ", "_");
        switch(name) {
            case "CAT":
                return "CAT." + ((Cat) event.getEntity()).getCatType();
            case "FOX":
                return "FOX." + ((Fox) event.getEntity()).getFoxType();
            case "SHEEP":
                return "SHEEP." + ((Sheep) event.getEntity()).getColor();
            case "TRADER_LLAMA":
                return "TRADER_LLAMA." + ((TraderLlama) event.getEntity()).getColor();
            case "HORSE":
                return "HORSE." + ((Horse) event.getEntity()).getColor();
            case "LLAMA":
                return "LLAMA." + ((Llama) event.getEntity()).getColor();
            case "MUSHROOM_COW":
                return "MUSHROOM_COW." + ((MushroomCow) event.getEntity()).getVariant();
            case "PANDA":
                return "PANDA." + ((Panda) event.getEntity()).getMainGene();
            case "PARROT":
                return "PARROT." + ((Parrot) event.getEntity()).getVariant();
            case "RABBIT":
                return "RABBIT." + ((Rabbit) event.getEntity()).getRabbitType();
            case "FROG":
                return "FROG." + ((Frog) event.getEntity()).getVariant();
            default:
                return name;
        }
    }

    public ItemStack makeSkull(String headName, Player killer) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        String textureCode =  MobHeads.valueOf(headName).getTexture();
        if (textureCode == null) {
            return item;
        }

        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(textureCode.getBytes()), textureCode);
        profile.getProperties().put("textures", new Property("textures",textureCode));
        profile.getProperties().put("display", new Property("Name", headName + " head"));
        assert meta != null;
        setGameProfile(meta, profile);
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.RESET + "Killed by " + ChatColor.RESET + ChatColor.YELLOW + killer.getName());
        lore.add(ChatColor.GRAY + "A mod by CrazyCranberry" + ChatColor.RESET);

        meta.setLore(lore);

        meta.setDisplayName(headName);
        item.setItemMeta(meta);
        return item;
    }

    public static void setGameProfile(SkullMeta meta, GameProfile profile){
        try{
            if(fieldProfileItem == null) {
                fieldProfileItem = meta.getClass().getDeclaredField("profile");
            }
            fieldProfileItem.setAccessible(true);
            fieldProfileItem.set(meta, profile);
        }
        catch(NoSuchFieldException | IllegalArgumentException | SecurityException | IllegalAccessException e){
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(scoreboardWrapper.getScoreboard());
        scoreboardWrapper.updateScore(event.getPlayer(), headLogConfig);
    }

    @Override
    public void onDisable() {
        try {
            kcLogConfig.save(getDataFolder() + "" + File.separatorChar + "kc_log.yml");
            headLogConfig.save(getDataFolder() + "" + File.separatorChar + "head_log.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
