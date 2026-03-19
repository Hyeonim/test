package com.towerdefense;

import java.awt.Color;

public class Tower {
    public String type;
    public Color color;
    public int tier;
    public int typeIndex; // GameConstants.ELEMENTS 내의 인덱스
    public int cooldown;

    public Tower(String type, Color color, int tier, int typeIndex) {
        this.type = type;
        this.color = color;
        this.tier = tier;
        this.typeIndex = typeIndex;
        this.cooldown = 0;
    }
}
