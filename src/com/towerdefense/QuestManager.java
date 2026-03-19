package com.towerdefense;

import static com.towerdefense.GameConstants.*;

public class QuestManager {
    private final GameContext ctx;

    public QuestManager(GameContext ctx) {
        this.ctx = ctx;
    }

    public void initQuests() {
        ctx.questList.clear();
        ctx.questList.add(new Quest("나무가 자란다!", "자연(풀) 타워 6개 모으기", 150));
        ctx.questList.add(new Quest("불바다", "화염(불) 타워 6개 모으기", 150));
        ctx.questList.add(new Quest("대홍수", "물 타워 6개 모으기", 150));
        ctx.questList.add(new Quest("전설의 시작", "스페셜(히든) 타워 1개 제작", 300));
        ctx.questList.add(new Quest("타워 콜렉터", "맵에 타워 15개 이상 건설", 200));
    }

    public void checkQuests() {
        int natureCount = 0, fireCount = 0, waterCount = 0, hiddenCount = 0, totalTowers = 0;

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                Tile tile = ctx.tiles[y][x];
                if (tile.hasTower()) {
                    totalTowers++;
                    Tower tower = tile.tower;
                    if (tower.tier == 4) hiddenCount++;
                    else {
                        String type = tower.type;
                        if ("Nature".equals(type)) natureCount++;
                        else if ("Fire".equals(type)) fireCount++;
                        else if ("Water".equals(type)) waterCount++;
                    }
                }
            }
        }

        if (!ctx.questList.get(0).completed && natureCount >= 6) completeQuest(ctx.questList.get(0));
        if (!ctx.questList.get(1).completed && fireCount >= 6) completeQuest(ctx.questList.get(1));
        if (!ctx.questList.get(2).completed && waterCount >= 6) completeQuest(ctx.questList.get(2));
        if (!ctx.questList.get(3).completed && hiddenCount >= 1) completeQuest(ctx.questList.get(3));
        if (!ctx.questList.get(4).completed && totalTowers >= 15) completeQuest(ctx.questList.get(4));
    }

    private void completeQuest(Quest q) {
        q.completed = true;
        ctx.gold += q.reward;
        // 알림 로직은 나중에 처리
    }
}
