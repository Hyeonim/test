package com.towerdefense;

import java.awt.Color;

public class GameConstants {
    public static final int GRID_SIZE = 12;
    public static final int TILE_SIZE = 45;

    public static final int GRID_ORIGIN_X = 0;
    public static final int GRID_ORIGIN_Y = 70;
    public static final int TOP_PANEL_H = 58;
    public static final int BOTTOM_PANEL_Y = 620;
    public static final int PANEL_W = GRID_SIZE * TILE_SIZE;
    public static final int PANEL_H = 760;

    public static final int TOWER_COST = 10;
    public static final int MAX_WAVE = 100;
    public static final int MAX_FIELD_MONSTERS = 100;
    public static final int TOWER_SPRITE_SIZE = 33;
    
    public static final double MONSTER_SCALE_NORMAL = 2.15;
    public static final double MONSTER_SCALE_BOSS = 1.85;
    
    public static final int FACING_FRONT = 0;
    public static final int FACING_BACK = 1;
    public static final int FACING_SIDE = 2;

    public static final String[] ELEMENTS = {"Fire", "Water", "Nature"};
    public static final String[] ELEMENT_LABELS = {"화염", "물", "자연"};
    public static final Color[] ELEMENT_COLORS = {
            new Color(255, 114, 90),
            new Color(91, 160, 255),
            new Color(98, 216, 121)
    };

    public static final String[] HIDDEN_ELEMENTS = {"Arcane", "Shadow", "Chaos"};
    public static final Color[] HIDDEN_COLORS = {
            new Color(255, 232, 130),
            new Color(115, 95, 210),
            new Color(255, 94, 193)
    };

    public static final int BASE_WAVE_CLEAR_GOLD = 60;
    public static final double NOBLE_WAVE_BONUS_RATE = 0.35;
    public static final double KNIGHT_UPGRADE_DISCOUNT_RATE = 0.20;
    public static final double ENGINEER_DAMAGE_BONUS_RATE = 0.20;
}
