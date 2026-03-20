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
            ctx.showToast("🌊 웨이브 클리어! 보너스 +" + bonus + "G", 40);

            ctx.wave++;
            if (ctx.wave > MAX_WAVE) {
                ctx.gameWon = true;
                return;
            }

            ctx.waveTimer = 600;
            ctx.spawnedThisWave = 0;
            ctx.spawnCooldown = 20;
            ctx.bossWarningIssued = false;
            ctx.bossPreviewHp = 0;
        }

        boolean isBossWave = (ctx.wave % 10 == 0);

        if (isBossWave && ctx.spawnedThisWave == 0 && !ctx.bossWarningIssued) {
            ctx.triggerBossWarning(calculateBossHp());
            ctx.showToast("WARNING: Boss wave " + ctx.wave, 50);
        }

        if (ctx.waveTimer > 200) {
            if (isBossWave && ctx.spawnedThisWave == 0 && ctx.bossWarningTimer > 0) return;

            ctx.spawnCooldown--;
            if (ctx.spawnCooldown <= 0) {
                if (isBossWave) {
                    if (ctx.spawnedThisWave == 0) {
                        monsterManager.spawnBoss();
                        ctx.spawnedThisWave++;
                        ctx.triggerBossArrivalImpact();
                        ctx.showToast("BOSS INCOMING", 40);
                    }
                } else {
                    monsterManager.spawnMonster();
                    ctx.spawnedThisWave++;
                    ctx.spawnCooldown = 20;
                }
            }
        }
    }

    private int calculateBossHp() {
        return (int) ((150 + (ctx.wave * 80) + (Math.pow(ctx.wave, 2) * 3.5)) * 15);
    }
}
