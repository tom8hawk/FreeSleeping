package ru.baronessdev.personal.freesleeping;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
        return translateColors(configuration.getString(path));
    }

    public static List<String> getStringList(String path) {
        return configuration.getStringList(path).stream()
                .map(Config::translateColors).collect(Collectors.toList());
    }

    public static int getInt(String path) {
        return configuration.getInt(path);
    }

    public static double getDouble(String path) {
        return configuration.getDouble(path);
    }

    public static boolean getBoolean(String path) {
        return configuration.getBoolean(path);
    }

    public static ConfigurationSection getSection(String path) {
        return configuration.getConfigurationSection(path);
    }

    private static String translateColors(String line) {
        if (line != null)
            return ChatColor.translateAlternateColorCodes('&', line);

        return "";
    }
}
