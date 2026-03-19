package com.towerdefense;

import java.awt.Color;
import java.util.Iterator;

import static com.towerdefense.GameConstants.*;

public class MonsterManager {
    private final GameContext ctx;

    public MonsterManager(GameContext ctx) {
        this.ctx = ctx;
    }

    public void update() {
        if (ctx.monsters.size() >= MAX_FIELD_MONSTERS) ctx.life = 0;

        Iterator<Monster> mobIter = ctx.monsters.iterator();
        while (mobIter.hasNext()) {
            Monster m = mobIter.next();
            if (m.hp <= 0) {
                // 원본의 보스 보상(30G) 및 일반몹 보상(1G) 복구
                ctx.gold += m.isBoss ? 30 : 1;
                if (ctx.selectedMonster == m) ctx.selectedMonster = null;
                mobIter.remove();
            } else if (!moveMonster(m)) {
                if (ctx.selectedMonster == m) ctx.selectedMonster = null;
                mobIter.remove();
                // 원본의 보스 누치 생명력 감소(5) 및 일반몹(1) 복구
                ctx.life -= m.isBoss ? 5 : 1;
            }
        }
    }

    private Monster createMonster(String element, Color color, int hp, boolean isBoss) {
        return new Monster(element, color, hp, isBoss, ctx.waypoints[0][0], ctx.waypoints[0][1]);
    }

    public void spawnMonster() {
        int idx = (ctx.wave - 1) % ELEMENTS.length;
        // 원본의 HP 스케일링 공식: 150 + wave*80 + wave^2*3.5
        int maxHp = (int) (150 + (ctx.wave * 80) + (Math.pow(ctx.wave, 2) * 3.5));
        ctx.monsters.add(createMonster(ELEMENTS[idx], ELEMENT_COLORS[idx], maxHp, false));
    }

    public void spawnBoss() {
        // 원본의 보스 HP 공식: 일반몹의 15배
        int maxHp = (int) ((150 + (ctx.wave * 80) + (Math.pow(ctx.wave, 2) * 3.5)) * 15);
        ctx.monsters.add(createMonster("Boss", new Color(225, 90, 255), maxHp, true));
    }

    private boolean moveMonster(Monster m) {
        if (m.targetWaypointIndex >= ctx.waypoints.length) return false;

        int targetX = ctx.waypoints[m.targetWaypointIndex][0];
        int targetY = ctx.waypoints[m.targetWaypointIndex][1];
        double speed = m.isBoss ? 2.5 : 4.0;

        double dx = targetX - m.x;
        double dy = targetY - m.y;
        double distance = Math.hypot(dx, dy);
        updateMonsterFacing(m, dx, dy);

        if (distance <= speed) {
            m.x = targetX;
            m.y = targetY;
            m.targetWaypointIndex++;
        } else {
            m.x += (dx / distance) * speed;
            m.y += (dy / distance) * speed;
        }
        return true;
    }

    private void updateMonsterFacing(Monster m, double dx, double dy) {
        if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) return;

        if (Math.abs(dx) >= Math.abs(dy)) {
            m.facing = FACING_SIDE;
            m.mirror = dx > 0;
        } else {
            m.facing = dy > 0 ? FACING_FRONT : FACING_BACK;
            m.mirror = false;
        }
    }

    public Monster findClosestMonster(int tileX, int tileY) {
        int towerCenterX = tileX * TILE_SIZE + GRID_ORIGIN_X + 22;
        int towerCenterY = tileY * TILE_SIZE + GRID_ORIGIN_Y + 22;

        Tower tower = ctx.tiles[tileY][tileX].tower;
        if (tower == null) return null;

        double range;
        if (tower.tier == 4) range = 200.0 + (ctx.hiddenUpgradeLevel * 20.0);
        else range = 120.0 + (ctx.upgradeLevels[tower.typeIndex] * 15.0);

        Monster closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Monster m : ctx.monsters) {
            double dx = m.x - towerCenterX;
            double dy = m.y - towerCenterY;
            double dist = Math.hypot(dx, dy);
            if (dist <= range && dist < minDistance) {
                minDistance = dist;
                closest = m;
            }
        }
        return closest;
    }
}
