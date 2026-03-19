package com.towerdefense;

import java.awt.Color;

public class Monster {
    public double x, y;
    public int targetWaypointIndex;
    public int maxHp, hp;
    public String element;
    public Color color;
    public boolean isBoss;
    public double radius;
    public int facing;
    public boolean mirror;

    public Monster(String element, Color color, int hp, boolean isBoss, double startX, double startY) {
        this.element = element;
        this.color = color;
        this.maxHp = hp;
        this.hp = hp;
        this.isBoss = isBoss;
        this.radius = isBoss ? 18.0 : 10.0;
        this.x = startX;
        this.y = startY;
        this.targetWaypointIndex = 1;
        this.facing = GameConstants.FACING_FRONT;
        this.mirror = false;
    }
}
