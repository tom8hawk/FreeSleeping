package ru.baronessdev.personal.freesleeping;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

import static ru.baronessdev.personal.freesleeping.FreeSleeping.inst;

public final class Config {
    private static final YamlConfiguration configuration = new YamlConfiguration();

    private Config() {
        throw new IllegalStateException("Utility class");
    }

    public static void init() {
        String path = inst.getDataFolder() + File.separator + "config.yml";
        File file = new File(path);

        if (!file.exists())
            inst.saveResource("config.yml", true);

        try {
            configuration.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static String getMessage(String path) {
        String line = configuration.getString(path);

        if (line != null)
            return ChatColor.translateAlternateColorCodes('&', line);

        return "";
    }

    public static int getInt(String path) {
        return configuration.getInt(path);
    }

    public static double getDouble(String path) {
        return configuration.getDouble(path);
    }

    public static long getLong(String path) {
        return configuration.getLong(path);
    }

    public static boolean getBoolean(String path) {
        return configuration.getBoolean(path);
    }
}
