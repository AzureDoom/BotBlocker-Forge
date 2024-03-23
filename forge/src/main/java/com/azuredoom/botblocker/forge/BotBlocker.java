package com.azuredoom.botblocker.forge;

import com.azuredoom.botblocker.CommonClass;
import com.azuredoom.botblocker.Constants;
import net.minecraftforge.fml.common.Mod;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Mod(Constants.MOD_ID)
public class BotBlocker {
    public static Yaml yaml;

    public BotBlocker() {
        CommonClass.init();
        var options = new DumperOptions();

        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
        Constants.configPath = new File(Constants.PATH_CONFIG).toPath();
        Constants.playersPath = new File(Constants.PATH_PLAYERS).toPath();
        loadConfig();
        loadPlayers();
        Constants.timeLimit = 20;
    }

    public static Map<String, Object> loadConfigs(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new HashMap<>();
        }

        try (InputStream input = new FileInputStream(path.toFile())) {
            return yaml.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static void saveConfigs(Map<String, Object> data, Path path) {
        try (Writer writer = new FileWriter(path.toFile())) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadPlayers() {
        Constants.players = loadConfigs(Constants.playersPath);
    }

    public static void savePlayers() {
        saveConfigs(Constants.players, Constants.playersPath);
    }

    public static void loadConfig() {
        Constants.config = loadConfigs(Constants.configPath);
        if (Constants.config == null) {
            Constants.config = new HashMap<>();
            Constants.config.putIfAbsent("enabled", true);
            Constants.config.putIfAbsent("time-limit", 20);
            saveConfig();
        }
    }

    public static void saveConfig() {
        saveConfigs(Constants.config, Constants.configPath);
    }
}