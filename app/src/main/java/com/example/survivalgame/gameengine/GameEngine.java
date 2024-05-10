package com.example.survivalgame.gameengine;

import android.content.Context;

import com.example.survivalgame.object.Bullet;
import com.example.survivalgame.object.Circle;
import com.example.survivalgame.object.Enemy;
import com.example.survivalgame.object.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameEngine {
    private final List<Player> players;
    private final List<Enemy> enemies;
    private final List<Bullet> bullets;
    private final Context context;

    public GameEngine(Context context) {
        this.players = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.context = context;
    }

    public void updateGameState(Map<String, Object> gameState) {
        players.clear();
        players.addAll((List<Player>) gameState.get("players"));

        enemies.clear();
        enemies.addAll((List<Enemy>) gameState.get("enemies"));

        bullets.clear();
        bullets.addAll((List<Bullet>) gameState.get("bullets"));
    }

    public void updateGameObjects(long gameTime) {
        for (Player player : players) {
            player.update(gameTime);
        }

        for (Enemy enemy : enemies) {
            enemy.update(gameTime);
        }

        for (Bullet bullet : bullets) {
            bullet.update(gameTime);
        }

        // Handle collisions
        handleCollisions();
    }

    public void handleCollisions() {
        // Iterate through enemies and remove those that collide with the player or a bullet
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();

            if (Circle.isColliding(enemy, players.get(0))) {
                enemyIterator.remove();
                continue;
            }

            Iterator<Bullet> bulletIterator = bullets.iterator();
            while (bulletIterator.hasNext()) {
                Bullet bullet = bulletIterator.next();

                if (Circle.isColliding(bullet, enemy)) {
                    bulletIterator.remove();
                    enemyIterator.remove();
                    break;
                }
            }
        }
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void removeEnemy(Enemy enemy) {
        enemies.remove(enemy);
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void removeBullet(Bullet bullet) {
        bullets.remove(bullet);
    }
    public List<Enemy> getEnemies() {
        return new ArrayList<>(enemies);
    }
    public List<Bullet> getBullets() {
        return new ArrayList<>(bullets);
    }
    public void updateEnemies(List<Enemy> newEnemies) {
        enemies.clear();
        enemies.addAll(newEnemies);
    }
    public void updateBullets(List<Bullet> newBullets) {
        bullets.clear();
        bullets.addAll(newBullets);
    }
}
