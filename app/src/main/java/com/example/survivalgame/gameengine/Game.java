package com.example.survivalgame.gameengine;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.survivalgame.R;
import com.example.survivalgame.WebSocketServer;
import com.example.survivalgame.object.Bullet;
import com.example.survivalgame.object.Circle;
import com.example.survivalgame.object.Crosshair;
import com.example.survivalgame.object.Enemy;
import com.example.survivalgame.object.Joystick;
import com.example.survivalgame.object.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


//        Actua como si fueses un experto en servidores usando webSockets para videojuegos real-time.
//
//        Estoy desarrollando un videojuego realtime en el que se conectan 2 jugadores a una partida y deben sobrevivir
//        juntos a enemigos que van apareciendo usando proyectiles, tengo implementada la logica de los webSockets,
//        pero quiero hacer que se conecten los 2 jugadores a una misma sala, osea, que cuando un jugador inicia sesion
//        le da la opcion de crear una nueva sala o conectarse a una ya existente con 1 solo jugador en ella, y que el
//        juego no inicie hasta que haya 2 jugadores en esa sala, me podrías ayudar a hacer esta actividad intermedia?
//
//        necesito el codigo xml y los cambios que debo hacer, te paso todos mis archivos .java para poder hacer los cambios
//        Debes hacer que la LoginActivity lanze la nueva actividad que me tienes que proporcionar en vez de lanzar el
//        juego, también deberás crear o modificar algún archivo .java para crear esas salas en la nueva actividad y que se
//        puedan conectar los jugadores a las salas ya creadas, cuando un jugador cree una sala, al resto de jugadores le
//        debe aparecer esa nueva sala creada en un listView con todas las salzas creadas con 1 solo jugador, las salas en las
//        que se conecte 1 segundo jugador desaparecerán de este lisView



//        Antes de hacer una actividad, hacer que solo exista 1 sala y solo puedan jugar 2 jugadores a la vez, para probar
//        el multijugador
/**
 * The Game class will manage all objects in the game, and will be responsible
 * for updating all states and rendering all objects to the screen.
 */
public class Game extends SurfaceView implements SurfaceHolder.Callback {
    private final Joystick playerJoystick;
    private final Joystick aimJoystick;
    private final Player player;
    private final Crosshair crosshair;
    private List<Enemy> enemyList = new ArrayList<>();
    private List<Bullet> bulletList = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private GameLoop gameLoop;
    private int playerJoystickPointerId = -1;
    private int aimJoystickPointerId = -1;
    private long gameTime;

    int screenHeight, screenWidth;
    private boolean bulletReady = false;
    private boolean showPlayerJoystick = false;
    private boolean showAimJoystick = false;
    private double bulletAimDirectionX, bulletAimDirectionY;

    private WebSocketServer webSocketServer;
    private GameEngine gameEngine;

    public Game(Context context) {
        super(context);


        // Get screen sizes on runtime
        screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

        // Get surface holder and add callback
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        // Create a new game loop
        gameLoop = new GameLoop(this, surfaceHolder);

        // Create the joysticks (they're set on approximate positions by default, but will move dynamically when used)
        playerJoystick = new Joystick((int) screenWidth / 4, (int) screenHeight / 2, 100, 60);
        aimJoystick = new Joystick((int) 3 * screenWidth / 4, (int) screenHeight / 2, 100, 60);

        // Create a new player
        player = new Player(getContext(), playerJoystick, 2 * 500, 500, 60);

        // Create a new crosshair
        crosshair = new Crosshair(getContext(), player, aimJoystick, 2 * 500, 600);

        this.webSocketServer = new WebSocketServer(this, context, playerJoystick);
        this.gameEngine = new GameEngine(context);

        setFocusable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Handle different touch event actions
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        int pointerIdIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerId = event.getPointerId(pointerIdIndex);

        float eventX = event.getX(pointerIdIndex);
        float eventY = event.getY(pointerIdIndex);

        int pointerCount = event.getPointerCount();

        switch (actionCode) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (((float) screenWidth / 2 - screenWidth * 0.1) > eventX && !showPlayerJoystick) {
                    // left side of the screen was pressed -> playerJoystick position will be moved to event position
                    playerJoystick.setPositionX(eventX);
                    playerJoystick.setPositionY(eventY);

                    playerJoystickPointerId = pointerId;
                    playerJoystick.setIsPressed(true);

                    showPlayerJoystick = true;

                }

                if (((float) screenWidth / 2 + screenWidth * 0.1) < eventX && !showAimJoystick) {
                    // right side of the screen was pressed -> aimJoystick position will be moved to event position
                    aimJoystick.setPositionX(eventX);
                    aimJoystick.setPositionY(eventY);

                    aimJoystickPointerId = pointerId;
                    aimJoystick.setIsPressed(true);

                    showAimJoystick = true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                // A joystick was pressed right before this event, and is now moved
                if (playerJoystickPointerId == pointerId) {
                    // joystick pointer was let go off -> setIsPressed(false) and resetActuator()
                    playerJoystick.setIsPressed(false);
                    playerJoystick.resetActuator();

                    playerJoystickPointerId = -1;

                    showPlayerJoystick = false;

                } else if (aimJoystickPointerId == pointerId) {
                    // Since this joystick is the crosshair, prepare a bullet
                    bulletReady = true;

                    bulletAimDirectionX = aimJoystick.getActuatorX();
                    bulletAimDirectionY = aimJoystick.getActuatorY();

                    // joystick pointer was let go off -> setIsPressed(false) and resetActuator()
                    aimJoystick.setIsPressed(false);
                    aimJoystick.resetActuator();

                    aimJoystickPointerId = -1;

                    showAimJoystick = false;
                }
                break;
        }

        for (int iPointerIndex = 0; iPointerIndex < pointerCount; iPointerIndex++) {
            if (playerJoystick.getIsPressed() && event.getPointerId(iPointerIndex) == playerJoystickPointerId) {
                // Joystick was pressed previously and is now moved
                playerJoystick.setActuator(event.getX(iPointerIndex), event.getY(iPointerIndex));

            } else if (aimJoystick.getIsPressed() && event.getPointerId(iPointerIndex) == aimJoystickPointerId) {
                // Joystick was pressed previously and is now moved
                aimJoystick.setActuator(event.getX(iPointerIndex), event.getY(iPointerIndex));
            }
        }
        return true;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        gameLoop.startLoop();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        drawUPS(canvas);
        drawFPS(canvas);

        crosshair.draw(canvas);
        player.draw(canvas);

        if (showPlayerJoystick) {
            playerJoystick.draw(canvas);
        }
        if (showAimJoystick) {
            aimJoystick.draw(canvas);
        }

        for (Enemy enemy : enemyList) {
            enemy.draw(canvas);
        }

        for (Bullet bullet : bulletList) {
            bullet.draw(canvas);
        }
    }

    public void drawUPS(Canvas canvas) {
        double avgUPS = gameLoop.getAverageUPS();

        int color = ContextCompat.getColor(getContext(), R.color.green);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(50);

        canvas.drawText(getContext().getString(R.string.canvas_text_ups, avgUPS), 100, 100, paint);
    }

    public void drawFPS(Canvas canvas) {
        double avgFPS = gameLoop.getAverageFPS();

        int color = ContextCompat.getColor(getContext(), R.color.green);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(50);

        canvas.drawText(getContext().getString(R.string.canvas_text_fps, avgFPS), 100, 200, paint);
    }

    public void update() {

//        Map<String, Object> gameState = new HashMap<>();
//        gameState.put("players", players);
//        gameState.put("enemies", enemies);
//        gameState.put("bullets", bullets);
//        gameState.put("gameTime", gameTime);
//
//        Room room = webSocketServer.getRoomForPlayer(player);
//        room.updateGameState(gameState);

        for (Player player : players) {
            player.update(gameTime);
        }

        for (Enemy enemy : enemies) {
            enemy.update(gameTime);
        }

        for (Bullet bullet : bullets) {
            bullet.update(gameTime);
        }
        // Update state of each object in the game
        playerJoystick.update();
        aimJoystick.update();

        player.update();
        crosshair.update();

        // Enemies are created dynamically here
        if (Enemy.readyToSpawn()) {
            enemyList.add(new Enemy(getContext(), player));
        }

        // Update state of each enemy
        for (Enemy enemy : enemyList) {
            enemy.update();
        }

        // Create bullet if ready
        if (bulletReady) {
            bulletList.add(new Bullet(getContext(), player, bulletAimDirectionX, bulletAimDirectionY));
            bulletReady = false;
        }

        // Update state of each bullet
        for (Bullet bullet : bulletList) {
            bullet.update();
        }

        // Remove the current enemy if it is colliding with the player or a bullet
        // by iterating through all enemies and bullets
        enemyList.removeIf(enemy -> Circle.isColliding(enemy, player));

        Iterator<Enemy> enemyIterator = enemyList.iterator();
        while (enemyIterator.hasNext()) {
            Circle enemy = enemyIterator.next();

            if (Circle.isColliding(enemy, player)) {
                enemyIterator.remove();

                // Skip bullets collision detection with current enemy since it collided with the player
                continue;
            }

            Iterator<Bullet> bulletIterator = bulletList.iterator();
            while (bulletIterator.hasNext()) {
                Circle bullet = bulletIterator.next();

                // Remove current bullet if it collides with an enemy
                if (Circle.isColliding(bullet, enemy)) {
                    bulletIterator.remove();
                    enemyIterator.remove();

//                     Stop checking collision of current enemy with the rest of the bullets since it collided with curent bullet
                    break;
                }
            }
        }
        Room room = webSocketServer.getRoomForPlayer(player);
        if (room != null) {
            Map<String, Object> gameState = room.getGameState();

            // Actualiza el estado del juego en el GameEngine
            gameEngine.updateGameState(gameState);


            // Actualiza los objetos del juego (jugadores, enemigos, balas, etc.)
            gameEngine.updateGameObjects(gameTime);

            // Actualiza la posición y estado del jugador local
            player.update(gameTime);
            crosshair.update();

            // Crea nuevos enemigos y balas si es necesario
            if (Enemy.readyToSpawn()) {
                enemyList.add(new Enemy(getContext(), player));
            }

            if (bulletReady) {
                bulletList.add(new Bullet(getContext(), player, bulletAimDirectionX, bulletAimDirectionY));
                bulletReady = false;
            }

            // Actualiza el estado de los enemigos y balas
            for (Enemy enemy : enemyList) {
                enemy.update();
            }

            for (Bullet bullet : bulletList) {
                bullet.update();
            }

            // Maneja las colisiones
            gameEngine.handleCollisions();
        } else {
            // Handle the case where the room is null
            System.err.println("Room is null for player: " + player.toString());
        }
    }
//    public void update() {
//        // Obten el estado de la sala actual para este jugador
//        Room room = webSocketServer.getRoomForPlayer(player);
//        if (room != null) {
//            Map<String, Object> gameState = room.getGameState();
//
//            // Actualiza el estado del juego en el GameEngine
//            gameEngine.updateGameState(gameState);
//
//
//            // Actualiza los objetos del juego (jugadores, enemigos, balas, etc.)
//            gameEngine.updateGameObjects(gameTime);
//
//            // Actualiza la posición y estado del jugador local
//            player.update(gameTime);
//            crosshair.update();
//
//            // Crea nuevos enemigos y balas si es necesario
//            if (Enemy.readyToSpawn()) {
//                enemyList.add(new Enemy(getContext(), player));
//            }
//
//            if (bulletReady) {
//                bulletList.add(new Bullet(getContext(), player, bulletAimDirectionX, bulletAimDirectionY));
//                bulletReady = false;
//            }
//
//            // Actualiza el estado de los enemigos y balas
//            for (Enemy enemy : enemyList) {
//                enemy.update();
//            }
//
//            for (Bullet bullet : bulletList) {
//                bullet.update();
//            }
//
//            // Maneja las colisiones
//            gameEngine.handleCollisions();
//        } else {
//            // Handle the case where the room is null
//            System.err.println("Room is null for player: " + player.toString());
//        }
//    }
//    public void update() {
//        // Obtén el estado de la sala actual para este jugador
//        Room room = webSocketServer.getRoomForPlayer(player);
//        if (room != null) {
//            Map<String, Object> gameState = room.getGameState();
//
//            // Actualiza el estado del juego en el GameEngine
//            gameEngine.updateGameState(gameState);
//
//            // Actualiza los objetos del juego (jugadores, enemigos, balas, etc.)
//            gameEngine.updateGameObjects(gameTime);
//
//            // Actualiza la posición y estado del jugador local
//            player.update(gameTime);
//            crosshair.update();
//
//            // Crea nuevos enemigos y balas si es necesario
//            if (Enemy.readyToSpawn()) {
//                enemyList.add(new Enemy(getContext(), player));
//            }
//
//            if (bulletReady) {
//                bulletList.add(new Bullet(getContext(), player, bulletAimDirectionX, bulletAimDirectionY));
//                bulletReady = false;
//            }
//
//            // Actualiza el estado de los enemigos y balas
//            for (Enemy enemy : enemyList) {
//                enemy.update();
//            }
//
//            for (Bullet bullet : bulletList) {
//                bullet.update();
//            }
//
//            // Maneja las colisiones
//            gameEngine.handleCollisions();
//        } else {
//            // Handle the case where the room is null
//            System.err.println("Room is null for player: " + player.toString());
//        }
//    }




    public void updateGameState(Map<String, Object> gameState) {
        players.clear();
        players.addAll((List<Player>) gameState.get("players"));

        enemies.clear();
        enemies.addAll((List<Enemy>) gameState.get("enemies"));

        bullets.clear();
        bullets.addAll((List<Bullet>) gameState.get("bullets"));

        gameTime = (long) gameState.get("gameTime");
    }
    public GameEngine getGameEngine() {
        return gameEngine;
    }
    public List<Enemy> getEnemyList() {
        return new ArrayList<>(enemyList);
    }

    public List<Bullet> getBulletList() {
        return new ArrayList<>(bulletList);
    }

    public void setEnemyList(List<Enemy> enemyList) {
        this.enemyList = new ArrayList<>(enemyList);
    }

    public void setBulletList(List<Bullet> bulletList) {
        this.bulletList = new ArrayList<>(bulletList);
    }

    public void addEnemy(Enemy enemy) {
        enemyList.add(enemy);
    }

    public void addBullet(Bullet bullet) {
        bulletList.add(bullet);
    }

    public void removeEnemy(Enemy enemy) {
        enemyList.remove(enemy);
    }

    public void removeBullet(Bullet bullet) {
        bulletList.remove(bullet);
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }
}