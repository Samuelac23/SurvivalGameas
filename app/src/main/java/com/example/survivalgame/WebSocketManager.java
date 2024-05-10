package com.example.survivalgame;

import android.content.Context;

import com.example.survivalgame.gameengine.Game;
import com.example.survivalgame.gameengine.GameEngine;
import com.example.survivalgame.gameengine.Room;
import com.example.survivalgame.object.Player;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;

@ServerEndpoint("/game")
public class WebSocketManager {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private final Map<String, Player> playersBySessionId = new HashMap<>();
    private final Map<String, Room> roomsBySessionId = new HashMap<>();
    private final Game game;
    private final Gson gson;
    private final Context context;

    public WebSocketManager(Game game, Context context) {
        this.game = game;
        this.gson = new Gson();
        this.context = context;
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            sessions.add(session);

            // Create a new player
            Player player = new Player(context, null, 100.0, 100.0, 60.0);
            playersBySessionId.put(session.getId(), player);

            // Find or create a new room
            Room room = Room.getRoomForPlayer(player, game);
            roomsBySessionId.put(session.getId(), room);
            room.addPlayer(player);

            // Broadcast the initial room state to all clients
            broadcastRoomState(room);

            // Check if the room is now full (2 players)
            if (room.getPlayers().size() == 2) {
                // Room is now full, start the game
                room.startGame();
            }
        } catch (Exception e) {
            System.err.println("Error in onOpen: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @OnClose
    public void onClose(Session session) {
        try {
            sessions.remove(session);

            // Remove the player from the room and update the state
            Player player = playersBySessionId.remove(session.getId());
            Room room = roomsBySessionId.remove(session.getId());
            room.removePlayer(player);

            // Check if the room is now empty
            if (room.getPlayers().isEmpty()) {
                // Room is now empty, stop the game
                room.stopGame();
            } else {
                // Broadcast the updated room state to all clients
                broadcastRoomState(room);
            }
        } catch (Exception e) {
            System.err.println("Error in onClose: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable);
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            // Process the message received from the client
            Map<String, Object> gameUpdate = gson.fromJson(message, Map.class);
            Room room = roomsBySessionId.get(session.getId());

            // Verify that the room is not null before updating the game state
            if (room != null) {
                // Update the game state for the room
                room.updateGameState(gameUpdate);

                // Broadcast the updated room state to all clients
                broadcastRoomState(room);
            } else {
                // Handle the case where the room is null
                System.err.println("Room is null for session: " + session.getId());
            }
        } catch (Exception e) {
            System.err.println("Error in onMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void broadcastRoomState(Room room) {
        Map<String, Object> gameState = room.getGameState();
        String gameStateJson = gson.toJson(gameState);

        for (Session session : sessions) {
            if (roomsBySessionId.get(session.getId()) == room) {
                try {
                    session.getBasicRemote().sendText(gameStateJson);
                } catch (IOException e) {
                    System.err.println("WebSocket broadcast error: " + e);
                }
            }
        }
    }

    public Room getRoomForPlayer(Player player) {
        for (Room room : roomsBySessionId.values()) {
            if (room.containsPlayer(player)) {
                return room;
            }
        }
        return null;
    }

    public Player getPlayerBySessionId(String sessionId) {
        return playersBySessionId.get(sessionId);
    }
}
