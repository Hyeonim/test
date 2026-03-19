package com.towerdefense;

public class Tile {
    public boolean buildable;
    public boolean selected;
    public Tower tower;

    public Tile(boolean buildable) {
        this.buildable = buildable;
        this.selected = false;
        this.tower = null;
    }

    public boolean hasTower() {
        return tower != null;
    }
}
