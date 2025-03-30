package me.crazycranberry.headhunterplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.crazycranberry.headhunterplugin.HeadHunterPlugin.getPlugin;

public class BackwardsCompatibilityUtils {
    private static final Map<String, List<String>> mobsToDefaultVariant = Map.of(
        "WOLF", List.of("PALE", "SPOTTED", "SNOWY", "BLACK", "ASHEN", "RUSTY", "WOODS", "CHESTNUT", "STRIPED"),
        "COW", List.of("TEMPERATE", "WARM", "COLD"),
        "PIG", List.of("TEMPERATE", "WARM", "COLD"),
        "CHICKEN", List.of("TEMPERATE", "WARM", "COLD")
    );

    public static void updateLogConfig(YamlConfiguration kcOrHcLogConfig, String configFileName) {
        AtomicBoolean madeAChange = new AtomicBoolean(false);
        for (String key : kcOrHcLogConfig.getKeys(false)) {
            if (!kcOrHcLogConfig.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection playersStats = (ConfigurationSection) kcOrHcLogConfig.get(key);
            mobsToDefaultVariant.forEach((key1, variants) -> {
                Integer count = (Integer) playersStats.get(key1);
                if (count != null) {
                    playersStats.set(key1 + "_" + variants.get(0), count);
                    playersStats.set(key1, null);
                    madeAChange.set(true);
                }
            });
        }
        if (madeAChange.get()) {
            try {
                kcOrHcLogConfig.save(getPlugin().getDataFolder() + "" + File.separatorChar + configFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateChanceConfig(YamlConfiguration chanceConfig, String configFileName) {
        AtomicBoolean madeAChange = new AtomicBoolean(false);
        ConfigurationSection chancePercent = chanceConfig.getConfigurationSection("chance_percent");
        mobsToDefaultVariant.forEach((key1, variants) -> {
            String preVariantMobName = key1.toLowerCase();
            if (!chancePercent.isDouble(preVariantMobName)) {
                return;
            }
            Double dropRate = chancePercent.getDouble(preVariantMobName);
            ConfigurationSection variantDropRates = new YamlConfiguration();
            for (String variant : variants) {
                variantDropRates.set(variant.toLowerCase(), dropRate);
            }
            chancePercent.set(preVariantMobName, variantDropRates);
            madeAChange.set(true);
        });
        if (madeAChange.get()) {
            chanceConfig.set("chance_percent", chancePercent);
            try {
                chanceConfig.save(getPlugin().getDataFolder() + "" + File.separatorChar + configFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateMobNameConfig(YamlConfiguration mobNameConfig, String configFileName) {
        AtomicBoolean madeAChange = new AtomicBoolean(false);
        mobsToDefaultVariant.forEach((key1, variants) -> {
            String preVariantMobName = key1.toLowerCase();
            if (!mobNameConfig.isString(preVariantMobName)) {
                return;
            }
            String translatedMobName = mobNameConfig.getString(preVariantMobName);
            ConfigurationSection variantNames = new YamlConfiguration();
            for (String variant : variants) {
                variantNames.set(variant.toLowerCase(), String.format("%s_%s", translatedMobName, variant.toLowerCase()));
            }
            mobNameConfig.set(preVariantMobName, variantNames);
            madeAChange.set(true);
        });
        if (madeAChange.get()) {
            try {
                mobNameConfig.save(getPlugin().getDataFolder() + "" + File.separatorChar + configFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
