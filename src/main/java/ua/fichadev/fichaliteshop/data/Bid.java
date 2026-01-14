package ua.fichadev.fichaliteshop.data;

import java.util.UUID;

public class Bid {
    private UUID playerId;
    private String playerName;
    private int amount;
    private long timestamp;

    public Bid(UUID playerId, String playerName, int amount) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public int getAmount() {
        return this.amount;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}