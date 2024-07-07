package me.crazycranberry.headhunterplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.crazycranberry.headhunterplugin.HeadHunterPlugin.getPlugin;

public class BackwardsCompatibilityUtils {
    private static final Map<String, String> mobsToDefaultVariant = Map.of(
        "WOLF", "WOLF_PALE"
    );

    public static void update(YamlConfiguration kcOrHcLogConfig, String configName) {
        AtomicBoolean madeAChange = new AtomicBoolean(false);
        for (String key : kcOrHcLogConfig.getKeys(false)) {
            if (!kcOrHcLogConfig.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection playersStats = (ConfigurationSection) kcOrHcLogConfig.get(key);
            mobsToDefaultVariant.forEach((key1, value) -> {
                Integer count = (Integer) playersStats.get(key1);
                if (count != null) {
                    playersStats.set(value, count);
                    playersStats.set(key1, null);
                    madeAChange.set(true);
                }
            });
        }
        if (madeAChange.get()) {
            try {
                kcOrHcLogConfig.save(getPlugin().getDataFolder() + "" + File.separatorChar + configName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
