package com.example.survivalgame;

import android.content.Context;

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

import com.example.survivalgame.gameengine.Room;
import com.example.survivalgame.object.Joystick;
import com.example.survivalgame.object.Player;
import com.google.gson.Gson;
import com.example.survivalgame.gameengine.Game;


@ServerEndpoint("/game")
public class WebSocketServer {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private Map<String, Player> playersBySessionId = new HashMap<>();
    private Map<String, Room> roomsBySessionId = new HashMap<>();
    private final Game game;
    private final Gson gson;
    private final Context context;
    private final Joystick joystick;

    public WebSocketServer(Game game, Context context, Joystick playerJoystick) {
        this.game = game;
        this.gson = new Gson();
        this.context = context;
        this.joystick = playerJoystick;
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            sessions.add(session);

            // Crear un nuevo jugador
            Player player = new Player(context, joystick, 100.0, 100.0, 60.0);
            playersBySessionId.put(session.getId(), player);

            // Buscar o crear una nueva sala
            Room room = Room.getRoomForPlayer(player, game);
            roomsBySessionId.put(session.getId(), room);
            room.addPlayer(player);

            // Enviar el estado inicial de la sala a todos los clientes
            broadcastRoomState(room);
        } catch (Exception e) {
            System.err.println("Error en onOpen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            sessions.remove(session);

            // Eliminar al jugador de la sala y actualizar el estado
            Player player = playersBySessionId.remove(session.getId());
            Room room = roomsBySessionId.remove(session.getId());
            room.removePlayer(player);
            broadcastRoomState(room);
        } catch (Exception e) {
            System.err.println("Error en onClose: " + e.getMessage());
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
        // Procesar el mensaje recibido del cliente
        Map<String, Object> gameUpdate = gson.fromJson(message, Map.class);
        Room room = roomsBySessionId.get(session.getId());
        room.updateGameState(gameUpdate);

        // Enviar el estado actualizado de la sala a todos los clientes
        broadcastRoomState(room);
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
