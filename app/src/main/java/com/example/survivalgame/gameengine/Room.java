package com.example.survivalgame.gameengine;

import com.example.survivalgame.object.Bullet;
import com.example.survivalgame.object.Enemy;
import com.example.survivalgame.object.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Room {
    private static final Map<String, Room> rooms = new HashMap<>();

    private String id;
    private Set<Player> players;
    private Game game;
    private GameEngine gameEngine;
    private boolean isActive;

    private Room(String id, Game game) {
        this.id = id;
        this.players = new HashSet<>();
        this.game = game;
        this.isActive = false;
    }

    public static Room getRoom(Game game) {
        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, game);
        rooms.put(roomId, room);
        return room;
    }

    public static Room getRoomForPlayer(Player player, Game game) {
        for (Room room : rooms.values()) {
            if (room.players.size() < 2) {
                return room;
            }
        }
        return getRoom(game);
    }

    public String getId() {
        return id;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Game getGame() {
        return game;
    }

    public boolean isActive() {
        return isActive;
    }

    public void addPlayer(Player player) {
        players.add(player);
        gameEngine.addPlayer(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        gameEngine.removePlayer(player);
    }

    public void startGame() {
        isActive = true;
        // Iniciar la lógica de la partida
    }

    public void stopGame() {
        isActive = false;
        // Detener la lógica de la partida
    }
    public boolean containsPlayer(Player player) {
        return players.contains(player);
    }
    public Map<String, Object> getGameState() {
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("players", players);
        gameState.put("enemies", gameEngine.getEnemies());
        gameState.put("bullets", gameEngine.getBullets());
        return gameState;
    }
    public void updateGameState(Map<String, Object> gameState) {
        players = (Set<Player>) gameState.get("players");
        gameEngine.updateEnemies((List<Enemy>) gameState.get("enemies"));
        gameEngine.updateBullets((List<Bullet>) gameState.get("bullets"));
    }
}
