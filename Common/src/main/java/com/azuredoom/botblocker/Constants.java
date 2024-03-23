package com.azuredoom.botblocker;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Constants {

    public static final String MOD_ID = "botblocker";
    public static final String MOD_NAME = "Multi Loader Template";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
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

    public static final Logger LOGGER = LogUtils.getLogger();
}