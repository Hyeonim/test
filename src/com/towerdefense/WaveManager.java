package com.towerdefense;

import static com.towerdefense.GameConstants.*;

public class WaveManager {
    private final GameContext ctx;
    private final MonsterManager monsterManager;

    public WaveManager(GameContext ctx, MonsterManager monsterManager) {
        this.ctx = ctx;
        this.monsterManager = monsterManager;
    }

    public void update() {
        ctx.waveTimer--;

        if (ctx.waveTimer <= 0) {
            int bonus = BASE_WAVE_CLEAR_GOLD;
            if (ctx.selectedJob == Job.NOBLE) {
                bonus += (int) Math.floor(BASE_WAVE_CLEAR_GOLD * NOBLE_WAVE_BONUS_RATE);
            }
            ctx.gold += bonus;
            // Toast 알림은 UI 계층에서 처리하도록 나중에 이벤트를 발생시키거나 ctx에 메시지를 담을 수 있음
            // 여기서는 일단 ctx 상태만 변경

            ctx.wave++;
            if (ctx.wave > MAX_WAVE) {
                ctx.gameWon = true;
                return;
            }

            ctx.waveTimer = 600;
            ctx.spawnedThisWave = 0;
            ctx.spawnCooldown = 20;
        }

        boolean isBossWave = (ctx.wave % 10 == 0);
        if (ctx.waveTimer > 200) {
            ctx.spawnCooldown--;
            if (ctx.spawnCooldown <= 0) {
                if (isBossWave) {
                    if (ctx.spawnedThisWave == 0) {
                        monsterManager.spawnBoss();
                        ctx.spawnedThisWave++;
                    }
                } else {
                    monsterManager.spawnMonster();
                    ctx.spawnedThisWave++;
                    ctx.spawnCooldown = 20;
                }
            }
        }
    }
}
