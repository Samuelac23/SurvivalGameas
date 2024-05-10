package com.example.survivalgame.object;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import com.example.survivalgame.R;
import com.example.survivalgame.Utils;

/**
 * A Crosshair is an object that is used to know where the bullets will be fired.
 * The Crosshair class is an extension of a Circle, which in turn inherits from the GameObject class.
 */
public class Crosshair extends Circle {
    public static final int ORBIT_RADIUS = 250;
    public static final int CROSSHAIR_RADIUS = 15;
    private double crosshairPositionX, crosshairPositionY;
    private final Player player;
    private final Joystick joystick;
    private Context context;
    Paint orbitPaint;

    public Crosshair(Context context, Player player, Joystick joystick, double positionX, double positionY) {
        super(context, ContextCompat.getColor(context, R.color.crosshair), positionX, positionY, CROSSHAIR_RADIUS);

        this.context = context;
        this.player = player;
        this.joystick = joystick;

        orbitPaint = new Paint();
        orbitPaint.setColor(ContextCompat.getColor(context, R.color.crosshair));
        orbitPaint.setStrokeWidth(2);
        orbitPaint.setStyle(Paint.Style.STROKE);
        orbitPaint.setPathEffect(new DashPathEffect(new float[]{(float) (10 * Math.PI), (float) (10 * Math.PI)}, (float)1.0));
    }

    @Override
    public void update() {
        // Update direction based on current joystick position
        crosshairPositionX = joystick.getActuatorX();
        crosshairPositionY = joystick.getActuatorY();


        // Normalize direction
        if (crosshairPositionX != 0 || crosshairPositionY != 0) {
            double distance = Utils.getDistanceBetweenPoints(0, 0, crosshairPositionX, crosshairPositionY);

            crosshairPositionX = crosshairPositionX / distance;
            crosshairPositionY = crosshairPositionY / distance;
        }

        positionX = player.getPositionX() + crosshairPositionX * ORBIT_RADIUS;
        positionY = player.getPositionY() + crosshairPositionY * ORBIT_RADIUS;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawCircle((float) player.getPositionX(), (float) player.getPositionY(), ORBIT_RADIUS, orbitPaint);
    }
}
