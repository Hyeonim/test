package com.towerdefense;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.towerdefense.GameConstants.*;

public class GameContext {
    public final Tile[][] tiles = new Tile[GRID_SIZE][GRID_SIZE];
    public final List<Monster> monsters = new ArrayList<>();
    public final List<Laser> lasers = new ArrayList<>();
    public final List<Quest> questList = new ArrayList<>();
    public final Random random = new Random();

    public int life = 20;
    public int gold = 80;
    public int wave = 1;
    public int waveTimer = 600;
    public int spawnCooldown = 20;
    public int spawnedThisWave = 0;
    public int currentSpeed = 1;
    public int tick = 0;
    public boolean gameWon = false;

    public final int[] upgradeLevels = {0, 0, 0};
    public int hiddenUpgradeLevel = 0;

    public Job selectedJob = null;
    public int selectedTileX = -1;
    public int selectedTileY = -1;
    public Monster selectedMonster = null;

    public final int[][] mapGrid = {
            {0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1},
            {1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0}
    };

    public final int[][] gridWaypoints = {
            {0, 0}, {0, 4}, {11, 4}, {11, 0}, {6, 0}, {6, 11},
            {11, 11}, {11, 7}, {0, 7}, {0, 11}, {3, 11}, {3, 0}
    };
    public final int[][] waypoints = new int[12][2];

    public GameContext() {
        for (int i = 0; i < waypoints.length; i++) {
            waypoints[i][0] = gridWaypoints[i][0] * TILE_SIZE + (TILE_SIZE / 2) + GRID_ORIGIN_X;
            waypoints[i][1] = gridWaypoints[i][1] * TILE_SIZE + (TILE_SIZE / 2) + GRID_ORIGIN_Y;
        }
    }

    public void reset() {
        life = 20;
        gold = 80;
        wave = 1;
        waveTimer = 600;
        spawnCooldown = 20;
        spawnedThisWave = 0;
        tick = 0;
        gameWon = false;
        for (int i = 0; i < 3; i++) upgradeLevels[i] = 0;
        hiddenUpgradeLevel = 0;
        monsters.clear();
        lasers.clear();
        selectedMonster = null;
        selectedTileX = -1;
        selectedTileY = -1;
    }
}
