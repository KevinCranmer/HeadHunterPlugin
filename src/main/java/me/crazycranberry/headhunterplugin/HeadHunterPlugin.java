package me.crazycranberry.headhunterplugin;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.papermc.lib.PaperLib;
import me.crazycranberry.headhunterplugin.util.HeadHunterConfig;
import me.crazycranberry.headhunterplugin.util.JsonDataType;
import me.crazycranberry.headhunterplugin.util.MobHeads;
import me.crazycranberry.headhunterplugin.util.ScoreboardWrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Camel;
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
import org.bukkit.entity.Item;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotResult;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static me.crazycranberry.headhunterplugin.util.HeadHunterConfig.updateOutOfDateConfig;

public final class HeadHunterPlugin extends JavaPlugin implements Listener {
    private static HeadHunterPlugin plugin;
    public final static Logger logger = Logger.getLogger("Minecraft");
    private final NamespacedKey NAME_KEY = new NamespacedKey(this, "head_name");
    private final NamespacedKey LORE_KEY_1 = new NamespacedKey(this, "head_lore_1");
    private final NamespacedKey LORE_KEY_2 = new NamespacedKey(this, "head_lore_2");
    private static Field fieldProfileItem;
    HeadHunterConfig headHunterConfig;
    YamlConfiguration chanceConfig;
    YamlConfiguration defaultChanceConfig;
    YamlConfiguration headLogConfig;
    YamlConfiguration kcLogConfig;
    YamlConfiguration mobNameTranslationConfig;
    YamlConfiguration defaultMobNameTranslationConfig;
    ScoreboardWrapper scoreboardWrapper;
    CommandManager commandManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        registerCommandManager();
        registerEvents();
        registerScoreboard();
    }

    @Override
    public void onDisable() {
        try {
            kcConfig().save(getDataFolder() + "" + File.separatorChar + "kc_log.yml");
            hcConfig().save(getDataFolder() + "" + File.separatorChar + "head_log.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        displayScoreboard(event);
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() != null) {
            String name = getTrueVictimName(event);
            double roll = Math.random();
            double dropRate = getDropRate(name, entity.getKiller());
            if (headHunterConfig().log_rolls()) {
                logger.info(String.format("%s killed %s and rolled %s for a %s drop rate.", entity.getKiller().getName(), translateMob(name), roll, dropRate));
            }
            if (roll < dropRate) {
                entity.getWorld().dropItem(entity.getLocation(), makeSkull(name, entity.getKiller()));
                getServer().broadcastMessage(headHunterConfig().head_drop_message(entity.getKiller().getName(), translateMob(name) + ChatColor.RESET));
                logKillOrDrop(entity.getKiller(), name.replace(".", "_"), hcConfig());
                updateScore(entity.getKiller(), hcConfig());
            }
            logKillOrDrop(entity.getKiller(), name.replace(".", "_"), kcConfig());
        }
    }
    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        ItemStack headItem = event.getItemInHand();
        ItemMeta meta = headItem.getItemMeta();
        if (headItem.getType() != Material.PLAYER_HEAD || meta == null) {
            return;
        }

        String name = meta.getPersistentDataContainer().get(NAME_KEY, PersistentDataType.STRING);
        List<String> lore = new ArrayList<>();
        Optional.ofNullable(meta.getPersistentDataContainer().get(LORE_KEY_1, PersistentDataType.STRING)).ifPresent(lore::add);
        Optional.ofNullable(meta.getPersistentDataContainer().get(LORE_KEY_2, PersistentDataType.STRING)).ifPresent(lore::add);
        Block block = event.getBlockPlaced();
        TileState skullState = (TileState) block.getState();
        PersistentDataContainer skullPDC = skullState.getPersistentDataContainer();
        skullPDC.set(NAME_KEY, PersistentDataType.STRING, name);
        if (!lore.isEmpty()) {
            skullPDC.set(LORE_KEY_1, PersistentDataType.STRING, lore.get(0));
            if (lore.size() > 1) {
                skullPDC.set(LORE_KEY_2, PersistentDataType.STRING, lore.get(1));
            }
        }
        skullState.update();
    }

    @EventHandler
    public void onBlockDropItemEvent(BlockDropItemEvent event) {
        BlockState blockState = event.getBlockState();
        if (blockState.getType() != Material.PLAYER_HEAD && blockState.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }
        TileState skullState = (TileState) blockState;
        PersistentDataContainer skullPDC = skullState.getPersistentDataContainer();
        String name = skullPDC.get(NAME_KEY, PersistentDataType.STRING);
        List<String> lore = new ArrayList<>();
        Optional.ofNullable(skullPDC.get(LORE_KEY_1, PersistentDataType.STRING)).ifPresent(lore::add);
        Optional.ofNullable(skullPDC.get(LORE_KEY_2, PersistentDataType.STRING)).ifPresent(lore::add);
        if (name == null) {
            return;
        }
        for (Item item: event.getItems()) {
            ItemStack itemstack = item.getItemStack();
            if (itemstack.getType() == Material.PLAYER_HEAD) {
                ItemMeta meta = itemstack.getItemMeta();
                if (meta == null) {
                    continue; // This shouldn't happen
                }
                meta.setDisplayName(name);
                meta.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, name);
                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                    meta.getPersistentDataContainer().set(LORE_KEY_1, PersistentDataType.STRING, lore.get(0));
                    if (lore.size() > 1) {
                        meta.getPersistentDataContainer().set(LORE_KEY_2, PersistentDataType.STRING, lore.get(1));
                    }
                }
                itemstack.setItemMeta(meta);
            }
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void registerScoreboard() {
        if (headHunterConfig().display_score()) {
            scoreboardWrapper = new ScoreboardWrapper("Unique Head Count");
        }
    }

    private void displayScoreboard(PlayerJoinEvent event) {
        if (headHunterConfig().display_score()) {
            event.getPlayer().setScoreboard(scoreboardWrapper.getScoreboard());
            scoreboardWrapper.updateScore(event.getPlayer(), hcConfig());
        }
    }

    private void updateScore(Player p, YamlConfiguration hcConfig) {
        if (headHunterConfig().display_score()) {
            scoreboardWrapper.updateScore(p, hcConfig);
        }
    }

    private void displayScoreboardForAll() {
        registerScoreboard();
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            p.setScoreboard(scoreboardWrapper.getScoreboard());
            scoreboardWrapper.updateScore(p, hcConfig());
        }
    }

    private void hideScoreboard() {
        for (String entry : scoreboardWrapper.getScoreboard().getEntries()) {
            scoreboardWrapper.getScoreboard().resetScores(entry);
        }
        scoreboardWrapper.getScoreboard().clearSlot(DisplaySlot.PLAYER_LIST);
        scoreboardWrapper = null;
    }

    private void registerCommandManager() {
        commandManager = new CommandManager(getServer(), chanceConfig(), kcConfig(), hcConfig(), headHunterConfig());
        setCommandManager("kc", commandManager);
        setCommandManager("hc", commandManager);
        setCommandManager("mobs", commandManager);
        setCommandManager("heads", commandManager);
        setCommandManager("headhunterrefresh", commandManager);
    }

    private void setCommandManager(String command, @NotNull CommandManager commandManager) {
        PluginCommand pc = getCommand(command);
        if (pc == null) {
            logger.info(String.format("[ ERROR ] - Error loading the %s command", command));
        } else {
            pc.setExecutor(commandManager);
        }
    }

    private YamlConfiguration loadConfig(String configName) throws InvalidConfigurationException {
        File configFile = new File(getDataFolder() + "" + File.separatorChar + configName);
        if(!configFile.exists()){
            saveResource(configName, true);
            logger.info(String.format("%s not found! copied %s to %s", configName, configName, getDataFolder()));
        }
        YamlConfiguration config = new YamlConfiguration();
        YamlConfiguration originalConfig;
        try {
            config.load(configFile);
            originalConfig = getOriginalConfig(configName);
            updateOutOfDateConfig(config, originalConfig, configName);
        } catch (InvalidConfigurationException | IOException e) {
            throw new InvalidConfigurationException("[ ERROR ] An error occured while trying to load " + configName);
        }
        return config;
    }

    private YamlConfiguration getOriginalConfig(String configName) throws IOException, InvalidConfigurationException {
        try {
            YamlConfiguration originalConfig = new YamlConfiguration();
            InputStream defaultChanceConfigStream = getPlugin().getResource(configName);
            assert defaultChanceConfigStream != null;
            InputStreamReader defaultChanceConfigReader = new InputStreamReader(defaultChanceConfigStream);
            originalConfig.load(defaultChanceConfigReader);
            return originalConfig;
        } catch (InvalidConfigurationException | IOException e) {
            logger.info("[ ERROR ] An error occured while trying to load the (original) " + configName + " file.");
            throw e;
        }
    }

    private YamlConfiguration chanceConfig() {
        if (chanceConfig != null) {
            return chanceConfig;
        }
        try {
            chanceConfig = loadConfig("chance_config.yml");
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return chanceConfig;
    }

    private YamlConfiguration defaultChanceConfig() {
        if (defaultChanceConfig != null) {
            return defaultChanceConfig;
        }
        defaultChanceConfig = new YamlConfiguration();
        try {
            InputStream defaultChanceConfigStream = getResource("chance_config.yml");
            assert defaultChanceConfigStream != null;
            InputStreamReader defaultChanceConfigReader = new InputStreamReader(defaultChanceConfigStream);
            defaultChanceConfig.load(defaultChanceConfigReader);
        } catch (InvalidConfigurationException | IOException e) {
            logger.info("[ ERROR ] An error occured while trying to load the (default) chance file.");
            e.printStackTrace();
        }
        return defaultChanceConfig;
    }

    private YamlConfiguration defaultMobNames() {
        if (defaultMobNameTranslationConfig != null) {
            return defaultMobNameTranslationConfig;
        }
        defaultMobNameTranslationConfig = new YamlConfiguration();
        try {
            InputStream defaultChanceConfigStream = getResource("mob_name_translations.yml");
            assert defaultChanceConfigStream != null;
            InputStreamReader defaultChanceConfigReader = new InputStreamReader(defaultChanceConfigStream);
            defaultMobNameTranslationConfig.load(defaultChanceConfigReader);
        } catch (InvalidConfigurationException | IOException e) {
            logger.info("[ ERROR ] An error occured while trying to load the (default) mob name file.");
            e.printStackTrace();
        }
        return defaultMobNameTranslationConfig;
    }

    private YamlConfiguration hcConfig() {
        if (headLogConfig != null) {
            return headLogConfig;
        }
        try {
            headLogConfig = loadConfig("head_log.yml");
            BackwardsCompatibilityUtils.update(headLogConfig, "head_log.yml");
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return headLogConfig;
    }

    private YamlConfiguration kcConfig() {
        if (kcLogConfig != null) {
            return kcLogConfig;
        }
        try {
            kcLogConfig = loadConfig("kc_log.yml");
            BackwardsCompatibilityUtils.update(kcLogConfig, "kc_log.yml");
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return kcLogConfig;
    }

    private YamlConfiguration mobNameTranslationConfig() {
        if (mobNameTranslationConfig != null) {
            return mobNameTranslationConfig;
        }
        try {
            mobNameTranslationConfig = loadConfig("mob_name_translations.yml");
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return mobNameTranslationConfig;
    }

    private HeadHunterConfig headHunterConfig() {
        if (headHunterConfig != null) {
            return headHunterConfig;
        }
        YamlConfiguration headHunterYamlConfig = null;
        try {
            headHunterYamlConfig = loadConfig("head_hunter_config.yml");
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        headHunterConfig = new HeadHunterConfig(headHunterYamlConfig);
        return headHunterConfig;
    }

    private double getDropRate(String mobName, Player killer) {
        String yamlMobName = "chance_percent." + mobName.toLowerCase();
        double dropRate = chanceConfig().getDouble(yamlMobName, defaultChanceConfig().getDouble(yamlMobName));
        if (headHunterConfig().looting_matters()) {
            int looting_level = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
            dropRate = dropRate * (1 + (looting_level * headHunterConfig().looting_multiplier()));
        }
        return dropRate;
    }

    private void logKillOrDrop(Player killer, String victim, YamlConfiguration config) {
        if (config.contains(String.format("%s.%s", killer.getName(), victim))) {
            config.set(String.format("%s.%s", killer.getName(), victim), config.getInt(String.format("%s.%s", killer.getName(), victim)) + 1);
        } else if (config.contains(killer.getName())) {
            ConfigurationSection cs = config.getConfigurationSection(killer.getName());
            if (cs == null) {
                logger.info(String.format("For some weird reason, %s has a null ConfigurationSection?", killer.getName()));
                return;
            }
            cs.set(victim, 1);
        } else {
            ConfigurationSection cs = config.createSection(killer.getName());
            cs.set(victim, 1);
        }
    }

    private String getTrueVictimName(EntityDeathEvent event) {
        String name = event.getEntityType().name().replaceAll(" ", "_");
        switch(name) {
            case "AXOLOTL":
                return "AXOLOTL." + ((Axolotl) event.getEntity()).getVariant();
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
            case "WOLF":
                // Idk why but just the wolf has a namespace tag when I try to get the variant
                return "WOLF." + ((Wolf) event.getEntity()).getVariant().toString().replace("minecraft:", "").toUpperCase();
            default:
                return name;
        }
    }

    public ItemStack makeSkull(String headName, Player killer) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        String textureCode =  MobHeads.valueOf(headName.replace(".", "_")).getTexture();
        if (textureCode == null) {
            return item;
        }
        PlayerProfile profile = Bukkit.createPlayerProfile("Skull");
        PlayerTextures textures = profile.getTextures();
        String jsonTexture = new String(Base64.getDecoder().decode(textureCode), StandardCharsets.UTF_8);
        String url = JsonParser.parseString(jsonTexture).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        try {
            textures.setSkin(new URL(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        profile.setTextures(textures);
        assert meta != null;
        meta.setOwnerProfile(profile);
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.WHITE + headHunterConfig().head_owner_statement(killer.getName(), translateMob(headName)) + ChatColor.RESET);
        lore.add(ChatColor.WHITE + headHunterConfig().head_secondary_statement() + ChatColor.RESET);

        meta.setLore(lore);

        String translatedName = translateMob(headName);
        meta.setDisplayName(translatedName);
        meta.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, translatedName);
        meta.getPersistentDataContainer().set(LORE_KEY_1, PersistentDataType.STRING, lore.get(0));
        meta.getPersistentDataContainer().set(LORE_KEY_2, PersistentDataType.STRING, lore.get(1));
        item.setItemMeta(meta);
        return item;
    }

    public String refreshYmlConfigurations() {
        boolean wasScoreboardDisplayed = headHunterConfig().display_score();
        try {
            chanceConfig = loadConfig("chance_config.yml");
            mobNameTranslationConfig = loadConfig("mob_name_translations.yml");
            headHunterConfig = new HeadHunterConfig(loadConfig("head_hunter_config.yml"));
            commandManager.reloadYmlConfigs(chanceConfig(), kcConfig(), hcConfig(), headHunterConfig());
            if (wasScoreboardDisplayed && !headHunterConfig().display_score()) {
                hideScoreboard();
            }
            if (!wasScoreboardDisplayed && headHunterConfig().display_score()) {
                displayScoreboardForAll();
            }
            return "Successfully loaded configs.";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static HeadHunterPlugin getPlugin() {
        return plugin;
    }

    public String translateMob(String mobNameEnglish) {
        String name = mobNameEnglish.toLowerCase();
        if (!mobNameTranslationConfig().isString(name)) {
            //maybe it's a mob with a type... Let's try finding it's prefix mob type
            for (String key : defaultMobNames().getKeys(false)) {
                if (defaultMobNames().isConfigurationSection(key) && name.startsWith(key)) {
                    name = name.replace(key + "_", key + ".");
                }
            }
        }
        String translatedName = mobNameTranslationConfig().getString(name, defaultMobNames().getString(name));
        if (translatedName == null) {
            return mobNameEnglish;
        }
        return translatedName.replaceAll("\\.", " ");
    }

    public String translateMobToEnglish(String renamedMob) {
        String name = renamedMob.toLowerCase();
        for (String key : mobNameTranslationConfig().getKeys(true)) {
            if (mobNameTranslationConfig().isString(key) && mobNameTranslationConfig().getString(key).equalsIgnoreCase(name)) {
                return key.toUpperCase().replace(".", "_");
            }
        }
        return renamedMob;
    }
}
