package com.example.survivalgame.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.survivalgame.Utils;

public class Joystick {
    private Paint innerCirclePaint, outerCirclePaint;
    private int outerCircleCenterPositionX, outerCircleCenterPositionY;
    private int innerCircleCenterPositionX, innerCircleCenterPositionY;
    private int outerCircleRadius, innerCircleRadius;
    private double joystickCenterToTouchDistance;
    private boolean isPressed = false;
    private double actuatorX, actuatorY;

    public Joystick(int centerPositionX, int centerPositionY, int outerCircleRadius, int innerCircleRadius) {
        // initialize the outer and inner circle that make up the joystick
        outerCircleCenterPositionX = centerPositionX;
        outerCircleCenterPositionY = centerPositionY;

        innerCircleCenterPositionX = centerPositionX;
        innerCircleCenterPositionY = centerPositionY;

        // Radii of the circles
        this.outerCircleRadius = outerCircleRadius;
        this.innerCircleRadius = innerCircleRadius;

        // Paint the circles
        outerCirclePaint = new Paint();
        outerCirclePaint.setColor(Color.GRAY);
        outerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        innerCirclePaint = new Paint();
        innerCirclePaint.setColor(Color.DKGRAY);
        innerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);

    }

    public void draw(Canvas canvas) {
        // Draw outer circle
        canvas.drawCircle(
                outerCircleCenterPositionX,
                outerCircleCenterPositionY,
                outerCircleRadius,
                outerCirclePaint
        );

        // Draw inner circle (the handle)
        canvas.drawCircle(
                innerCircleCenterPositionX,
                innerCircleCenterPositionY,
                innerCircleRadius,
                innerCirclePaint
        );


    }

    public void update() {
        updateInnerCirclePosition();
    }

    private void updateInnerCirclePosition() {
        innerCircleCenterPositionX = (int) (outerCircleCenterPositionX + actuatorX * outerCircleRadius);
        innerCircleCenterPositionY = (int) (outerCircleCenterPositionY + actuatorY * outerCircleRadius);
    }

    public boolean isPressed(double touchPositionX, double touchPositionY) {
        joystickCenterToTouchDistance = Utils.getDistanceBetweenPoints(
                outerCircleCenterPositionX,
                outerCircleCenterPositionY,
                touchPositionX,
                touchPositionY
        );

        return joystickCenterToTouchDistance < outerCircleRadius;
    }

    public boolean getIsPressed() {
        return isPressed;
    }

    public void setIsPressed(boolean isPressed) {
        this.isPressed = isPressed;
    }

    public double getActuatorX() {
        return actuatorX;
    }

    public double getActuatorY() {
        return actuatorY;
    }

    public void setActuator(double touchPositionX, double touchPositionY) {
        double deltaX = touchPositionX - outerCircleCenterPositionX;
        double deltaY = touchPositionY - outerCircleCenterPositionY;

        double deltaDistance = Utils.getDistanceBetweenPoints(0, 0, deltaX, deltaY);

        // Actuator X and Y will always be a value between 0 and 1
        if (deltaDistance < outerCircleRadius) {
            actuatorX = deltaX / outerCircleRadius;
            actuatorY = deltaY / outerCircleRadius;
        } else {
            actuatorX = deltaX / deltaDistance;
            actuatorY = deltaY / deltaDistance;
        }
    }

    public void resetActuator() {
        actuatorX = 0;
        actuatorY = 0;
    }

    public void setPositionX(float eventX) {
        outerCircleCenterPositionX = (int) eventX;
        innerCircleCenterPositionX = (int) eventX;
    }

    public void setPositionY(float eventY) {
        outerCircleCenterPositionY = (int) eventY;
        innerCircleCenterPositionY = (int) eventY;
    }
}
