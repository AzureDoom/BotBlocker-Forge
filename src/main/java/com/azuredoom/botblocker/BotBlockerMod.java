package com.azuredoom.botblocker;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(BotBlockerMod.MODID)
public class BotBlockerMod {
    public static final String MODID = "botblocker";
    public static final String COMMAND_ENABLE = "enable";
    public static final String COMMAND_DISABLE = "disable";
    public static final String COMMAND_SET_TIME_LIMIT = "setTimeLimit";
    public static final String MESSAGE_DISCONNECT = "Bot detected. If you are a legitimate user, please contact the admin.";
    public static final String MESSAGE_DISCONNECT_CONSOLE = "Player %s was banned for disconnecting within %d seconds of joining for the first time - suspected bot.";
    public static final String MESSAGE_ENABLED = "BotBlocker enabled.";
    public static final String MESSAGE_DISABLED = "BotBlocker disabled.";
    public static final String MESSAGE_TIME_LIMIT = "Time limit set to %d seconds.";
    public static final String MESSAGE_MOD_LOADED = "BotBlocker mod has loaded with 'time-limit: %d'!";

    public static final String PATH_CONFIG = "mods/BotBlocker/config.yml";
    public static final String PATH_PLAYERS = "mods/BotBlocker/players.yml";

    public static boolean pluginEnabled = true;
    public static int timeLimit; // In seconds
    public static HashMap<UUID, Long> joinTimes = new HashMap<>();
    public static Map<String, Object> config;
    public static Map<String, Object> players;
    public static Path configPath;
    public static Path playersPath;
    public static Yaml yaml;

    private static final Logger LOGGER = LogUtils.getLogger();

    public BotBlockerMod() {
        var options = new DumperOptions();

        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
        configPath = new File(PATH_CONFIG).toPath();
        playersPath = new File(PATH_PLAYERS).toPath();
        loadConfig();
        loadPlayers();
        timeLimit = 20;
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    public static boolean isPlayerExempt(UUID playerId) {
        return players.containsKey(playerId.toString()) && (boolean) players.get(playerId.toString());
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
        players = loadConfigs(playersPath);
    }

    public static void savePlayers() {
        saveConfigs(players, playersPath);
    }

    public static void loadConfig() {
        config = loadConfigs(configPath);
        if (config == null) {
            config = new HashMap<>();
            config.putIfAbsent("enabled", true);
            config.putIfAbsent("time-limit", 20);
            saveConfig();
        }
    }

    public static void saveConfig() {
        saveConfigs(config, configPath);
    }
}
