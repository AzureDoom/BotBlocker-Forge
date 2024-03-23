package com.azuredoom.botblocker;

import java.util.UUID;

public class CommonClass {
    public static void init() {
    }

    public static boolean isPlayerExempt(UUID playerId) {
        return Constants.players.containsKey(playerId.toString()) && (boolean) Constants.players.get(
                playerId.toString());
    }
}