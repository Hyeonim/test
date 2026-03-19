package com.towerdefense;

import java.awt.Color;

public class Laser {
    public int startX, startY;
    public int endX, endY;
    public int frames;
    public Color color;
    public boolean heavy;

    public Laser(int startX, int startY, int endX, int endY, int frames, Color color, boolean heavy) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.frames = frames;
        this.color = color;
        this.heavy = heavy;
    }
}
