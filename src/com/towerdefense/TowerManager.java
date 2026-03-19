package com.towerdefense;

import java.awt.Color;
import static com.towerdefense.GameConstants.*;

public class TowerManager {
    private final GameContext ctx;
    private final QuestManager questManager;

    public TowerManager(GameContext ctx, QuestManager questManager) {
        this.ctx = ctx;
        this.questManager = questManager;
    }

    public void buildTower(int x, int y) {
        Tile tile = ctx.tiles[y][x];
        if (tile.buildable && !tile.hasTower() && ctx.gold >= TOWER_COST) {
            ctx.gold -= TOWER_COST;
            int typeIndex = ctx.random.nextInt(ELEMENTS.length);
            tile.tower = new Tower(ELEMENTS[typeIndex], ELEMENT_COLORS[typeIndex], 1, typeIndex);
            questManager.checkQuests();
        }
    }

    public void mergeTowers(int x1, int y1, int x2, int y2) {
        Tile t1 = ctx.tiles[y1][x1];
        Tile t2 = ctx.tiles[y2][x2];

        if (t1.hasTower() && t2.hasTower() && t1.tower.tier == t2.tower.tier) {
            if (t1.tower.tier < 3 && t1.tower.type.equals(t2.tower.type)) {
                int newTier = t1.tower.tier + 1;
                int rIndex = ctx.random.nextInt(ELEMENTS.length);
                t2.tower = new Tower(ELEMENTS[rIndex], ELEMENT_COLORS[rIndex], newTier, rIndex);
                t1.tower = null;
                questManager.checkQuests();
            } else if (t1.tower.tier == 3) {
                if (ctx.random.nextInt(100) < 30) {
                    int rIndex = ctx.random.nextInt(HIDDEN_ELEMENTS.length);
                    t2.tower = new Tower(HIDDEN_ELEMENTS[rIndex], HIDDEN_COLORS[rIndex], 4, -1);
                    ctx.showToast("✨ 히든 강림 성공!!", 60);
                } else {
                    ctx.showToast("💥 합성 실패! 재료 타워가 파괴되었습니다.", 60);
                }
                t1.tower = null;
                questManager.checkQuests();
            }
        }
    }

    public void sellTower(int x, int y) {
        Tile tile = ctx.tiles[y][x];
        if (tile.hasTower()) {
            ctx.gold += tile.tower.tier * 5;
            tile.tower = null;
        }
    }

    public void upgradeNormal(int typeIndex) {
        int cost = getUpgCost(typeIndex);
        if (ctx.gold >= cost) {
            ctx.gold -= cost;
            ctx.upgradeLevels[typeIndex]++;
        }
    }

    public void upgradeHidden() {
        int cost = getHiddenUpgCost();
        if (ctx.gold >= cost) {
            ctx.gold -= cost;
            ctx.hiddenUpgradeLevel++;
        }
    }

    public int getUpgCost(int typeIndex) {
        int baseCost = 20 + (ctx.upgradeLevels[typeIndex] * 10);
        if (ctx.selectedJob == Job.KNIGHT_COMMANDER) {
            return Math.max(1, (int) Math.ceil(baseCost * (1.0 - KNIGHT_UPGRADE_DISCOUNT_RATE)));
        }
        return baseCost;
    }

    public int getHiddenUpgCost() {
        int baseCost = 50 + (ctx.hiddenUpgradeLevel * 20);
        if (ctx.selectedJob == Job.KNIGHT_COMMANDER) {
            return Math.max(1, (int) Math.ceil(baseCost * (1.0 - KNIGHT_UPGRADE_DISCOUNT_RATE)));
        }
        return baseCost;
    }

    public double calculateDamage(Tower tower, Monster target) {
        double damage;
        double multiplier = 1.0;

        if (tower.tier == 4) {
            damage = 800;
            multiplier = 1.0 + (ctx.hiddenUpgradeLevel * 0.4);
        } else {
            damage = tower.tier == 1 ? 25 : (tower.tier == 2 ? 60 : 150);
            multiplier = 1.0 + (ctx.upgradeLevels[tower.typeIndex] * 0.3);
            multiplier *= calcElementMultiplier(tower.type, target.element);
        }

        if (ctx.selectedJob == Job.MAGITECH_ENGINEER) {
            multiplier *= (1.0 + ENGINEER_DAMAGE_BONUS_RATE);
        }
        return damage * multiplier;
    }

    private double calcElementMultiplier(String towerElement, String targetElement) {
        if ("Boss".equals(targetElement)) return 1.0;
        if ("Fire".equals(towerElement) && "Nature".equals(targetElement)) return 1.5;
        if ("Fire".equals(towerElement) && "Water".equals(targetElement)) return 0.5;
        if ("Water".equals(towerElement) && "Fire".equals(targetElement)) return 1.5;
        if ("Water".equals(towerElement) && "Nature".equals(targetElement)) return 0.5;
        if ("Nature".equals(towerElement) && "Water".equals(targetElement)) return 1.5;
        if ("Nature".equals(towerElement) && "Fire".equals(targetElement)) return 0.5;
        return 1.0;
    }
}
